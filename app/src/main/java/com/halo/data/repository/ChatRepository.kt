package com.halo.data.repository

import android.util.Log
import com.halo.data.local.dao.ChatRoomDao
import com.halo.data.local.dao.ChatRoomMemberDao
import com.halo.data.local.dao.MessageDao
import com.halo.data.local.entity.ChatRoomEntity
import com.halo.data.local.entity.ChatRoomMemberEntity
import com.halo.data.local.entity.MessageEntity
import com.halo.data.local.entity.toDomain
import com.halo.data.matrix.MatrixClientManager
import com.halo.domain.model.ChatMessage
import com.halo.domain.model.ChatRoom
import com.halo.domain.model.MessageStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.rustcomponents.sdk.CreateRoomParameters
import org.matrix.rustcomponents.sdk.Membership
import org.matrix.rustcomponents.sdk.RoomPreset
import org.matrix.rustcomponents.sdk.RoomVisibility
import org.matrix.rustcomponents.sdk.messageEventContentFromMarkdown
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ChatRepository"

/** How far back (ms) we look for a matching local echo when deduplicating our own messages. */
private const val LOCAL_ECHO_WINDOW_MS = 10_000L

@Singleton
class ChatRepository @Inject constructor(
    private val matrixClientManager: MatrixClientManager,
    private val chatRoomDao: ChatRoomDao,
    private val chatRoomMemberDao: ChatRoomMemberDao,
    private val messageDao: MessageDao
) {
    private val dmCreationLocks = mutableMapOf<String, Mutex>()


    // ─── Room list ────────────────────────────────────────────────────────────

    val chatRooms: Flow<List<ChatRoom>> = chatRoomDao.getAllChatRooms().map { entities ->
        entities.map { it.toDomain(matrixClientManager) }
    }

    /**
     * B8: Sync known Matrix rooms into the local DB.
     *
     * Uses INSERT-OR-IGNORE so that any lastMessage / unreadCount fields that were
     * written by the real-time sync listener are **never** overwritten by this
     * refresh pass (which doesn't have that information).
     *
     * Only brand-new rooms (not yet in the DB) will be inserted.
     *
     * FIX: Now resolves display names instead of storing raw room IDs,
     * and detects DM rooms properly.
     */
    suspend fun refreshChatRooms() {
        val client = matrixClientManager.getClient() ?: return
        val rooms = client.rooms()
        val currentUserId = matrixClientManager.getCurrentSession()?.userId

        val entities = rooms.map { room ->
            val roomId = room.id()

            // Resolve a human-readable name for the room
            val resolvedName = try {
                room.displayName() ?: roomId
            } catch (_: Exception) {
                roomId
            }

            // Detect DMs
            val isDm = try {
                room.isDirect()
            } catch (_: Exception) {
                false
            }

            // Cache DM counterpart IDs so local reuse checks keep working after sync.
            val membersStr = if (isDm) {
                resolveDmMemberIds(roomId = roomId, fallbackRoom = room, currentUserId = currentUserId)
            } else {
                ""
            }

            ChatRoomEntity(
                roomId        = roomId,
                name          = resolvedName,
                avatarUrl     = try { room.avatarUrl() } catch (_: Exception) { null },
                lastMessage   = null,
                lastMessageAt = 0L,
                unreadCount   = 0,
                isDm          = isDm,
                membersJoined = membersStr
            )
        }
        chatRoomDao.insertChatRooms(entities)
        entities.filter { it.isDm }.forEach { room ->
            upsertRoomMembers(room.roomId, room.membersJoined)
        }
    }

    // ─── Invited rooms (BUG-2 fix) ───────────────────────────────────────────

    /**
     * Detects rooms where we have been invited but haven't joined yet,
     * and auto-joins invites so incoming messages become visible immediately.
     */
    suspend fun acceptPendingInvites(): List<String> {
        val client = matrixClientManager.getClient() ?: return emptyList()
        val currentUserId = matrixClientManager.getCurrentSession()?.userId ?: return emptyList()
        val joinedRoomIds = mutableListOf<String>()

        try {
            val rooms = client.rooms()
            for (room in rooms) {
                try {
                    // Check if this room is in invited state
                    val membership = room.membership()
                    if (membership == Membership.INVITED) {
                        val isSpace = try { room.isSpace() } catch (_: Exception) { false }
                        if (isSpace) {
                            continue
                        }
                        val roomId = room.id()
                        Log.d(TAG, "Auto-accepting invite for room $roomId")

                        // Join invite and verify that membership transitions to joined.
                        room.join()
                        var joined = false
                        repeat(3) { attempt ->
                            val postJoinMembership = runCatching { room.membership() }.getOrNull()
                            if (postJoinMembership == Membership.JOINED) {
                                joined = true
                                return@repeat
                            }
                            Log.w(TAG, "Invite join pending for $roomId (attempt=${attempt + 1})")
                            delay(400L)
                        }
                        if (!joined) {
                            Log.w(TAG, "Invite join not confirmed for room $roomId")
                        }
                        joinedRoomIds.add(roomId)

                        // Resolve name and add to local DB
                        val resolvedName = try {
                            room.displayName() ?: roomId
                        } catch (_: Exception) { roomId }

                        val isDm = try { room.isDirect() } catch (_: Exception) { false }
                        val membersStr = resolveDmMemberIds(
                            roomId = roomId,
                            fallbackRoom = room,
                            currentUserId = currentUserId
                        )
                        upsertRoomMembers(roomId, membersStr)

                        chatRoomDao.insertChatRoom(
                            ChatRoomEntity(
                                roomId        = roomId,
                                name          = resolvedName,
                                avatarUrl     = try { room.avatarUrl() } catch (_: Exception) { null },
                                lastMessage   = null,
                                lastMessageAt = System.currentTimeMillis(),
                                unreadCount   = 1,
                                isDm          = isDm,
                                membersJoined = membersStr
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process room invite: ${room.id()}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for pending invites", e)
        }

        return joinedRoomIds
    }

    // ─── Timeline ─────────────────────────────────────────────────────────────

    fun getRoomTimeline(roomId: String): Flow<List<ChatMessage>> =
        messageDao.getMessagesForRoom(roomId).map { it.map(MessageEntity::toDomain) }

    /**
     * Persists an incoming timeline event.
     *
     * B1 — Own-message deduplication:
     * When the Matrix homeserver echoes back a message we just sent, the sender ID
     * will equal our own user ID and there will already be a local_ placeholder row
     * in the DB. In that case we delete the placeholder and write the server-confirmed
     * entity (with the real Matrix event ID) so the message only appears once in the UI.
     */
    suspend fun appendIncomingMessage(
        roomId: String,
        eventId: String,
        senderId: String,
        body: String,
        timestamp: Long
    ) {
        val session    = matrixClientManager.getCurrentSession()
        val currentUserId = session?.userId
        val isMe       = senderId == currentUserId

        if (isMe && currentUserId != null) {
            // B1: look for a local echo we inserted in sendMessage()
            val sinceTimestamp = System.currentTimeMillis() - LOCAL_ECHO_WINDOW_MS
            val localEcho = messageDao.findRecentLocalMessage(
                roomId         = roomId,
                senderId       = currentUserId,
                body           = body,
                sinceTimestamp = sinceTimestamp
            )
            if (localEcho != null) {
                // Remove the local placeholder and replace it with the server-confirmed row
                messageDao.deleteMessage(localEcho.id)
                Log.d(TAG, "Deduped local echo ${localEcho.id} → $eventId")
            }
        }

        messageDao.insertMessage(
            MessageEntity(
                id        = eventId,
                roomId    = roomId,
                senderId  = senderId,
                body      = body,
                isMe      = isMe,
                timestamp = timestamp,
                status    = MessageStatus.SENT
            )
        )

        // Keep the room's last-message preview up to date
        chatRoomDao.getChatRoom(roomId)?.let { room ->
            chatRoomDao.insertChatRoom(room.copy(lastMessage = body, lastMessageAt = timestamp))
        }
    }

    // ─── Send ─────────────────────────────────────────────────────────────────

    /**
     * Sends a chat message.
     *
     * B3 — Delivery status:
     * 1. Optimistically inserts a local placeholder with status=SENDING so the
     *    message appears immediately in the UI.
     * 2. Hands the content to the Matrix SDK for network delivery.
     * 3a. If the SDK call succeeds the placeholder is marked SENT. The homeserver
     *     will echo the event back; [appendIncomingMessage] will then replace the
     *     local_ row with the real server event ID (B1 dedup).
     * 3b. If the SDK call throws, the placeholder is marked FAILED so the UI can
     *     show an error indicator and offer a retry option.
     */
    suspend fun sendMessage(roomId: String, body: String) {
        val session = matrixClientManager.getCurrentSession() ?: return
        val localId = "local_${System.currentTimeMillis()}"
        val now     = System.currentTimeMillis()

        // Step 1: optimistic local insert with SENDING status
        messageDao.insertMessage(
            MessageEntity(
                id        = localId,
                roomId    = roomId,
                senderId  = session.userId,
                body      = body,
                isMe      = true,
                timestamp = now,
                status    = MessageStatus.SENDING
            )
        )
        chatRoomDao.getChatRoom(roomId)?.let { room ->
            chatRoomDao.insertChatRoom(room.copy(lastMessage = body, lastMessageAt = now))
        }

        // Step 2: send via SDK
        val client = matrixClientManager.getClient() ?: run {
            messageDao.updateStatus(localId, MessageStatus.FAILED)
            return
        }
        try {
            val room    = client.getRoom(roomId) ?: throw IllegalStateException("Room $roomId not found")
            val content = messageEventContentFromMarkdown(body)
            room.timeline().send(content)

            // Step 3a: network delivery handed off — mark SENT
            // (The server echo handled in appendIncomingMessage will later replace
            //  this local_ row with the real event ID.)
            messageDao.updateStatus(localId, MessageStatus.SENT)
        } catch (e: Exception) {
            // Step 3b: mark FAILED so the UI can show the error and offer retry
            Log.e(TAG, "Failed to send message in room $roomId", e)
            messageDao.updateStatus(localId, MessageStatus.FAILED)
        }
    }

    /**
     * B3 — Retries a previously failed message.
     * Deletes the FAILED placeholder and re-submits the message body as a fresh send.
     */
    suspend fun retryMessage(roomId: String, failedMessageId: String, body: String) {
        messageDao.deleteMessage(failedMessageId)
        sendMessage(roomId, body)
    }

    // ─── Direct Messages ──────────────────────────────────────────────────────

    /**
     * BUG-1 FIX: Check for existing DM rooms before creating a new one.
     * This prevents duplicate rooms every time the user taps "Message".
     */
    suspend fun createDirectMessage(userId: String): Result<String> {
        val currentUserId = matrixClientManager.getCurrentSession()?.userId ?: return Result.failure(Exception("Not authenticated"))
        val lockKey = listOf(currentUserId, userId).sorted().joinToString("|")
        val mutex = synchronized(dmCreationLocks) { dmCreationLocks.getOrPut(lockKey) { Mutex() } }
        return mutex.withLock {
            createDirectMessageLocked(userId, currentUserId)
        }
    }

    private suspend fun createDirectMessageLocked(userId: String, currentUserId: String): Result<String> {
        val client = matrixClientManager.getClient() ?: return Result.failure(Exception("Not authenticated"))
        val existingLocal = chatRoomMemberDao.findDmRoomByMember(userId) ?: chatRoomDao.findDmWithUser(userId)
        if (existingLocal != null) {
            Log.d(TAG, "Reusing existing local DM room: ${existingLocal.roomId}")
            return Result.success(existingLocal.roomId)
        }

        val existingServerRoomId = findExistingDirectRoomIdWithUser(userId)
        if (existingServerRoomId != null) {
            upsertDmSnapshotFromRoom(existingServerRoomId, userId, currentUserId)
            Log.d(TAG, "Reusing existing server DM room: $existingServerRoomId")
            return Result.success(existingServerRoomId)
        }

        return try {
            val params = CreateRoomParameters(
                name = null,
                topic = null,
                isDirect = true,
                isEncrypted = true,
                visibility = RoomVisibility.Private,
                preset = RoomPreset.PRIVATE_CHAT,
                invite = listOf(userId),
                avatar = null,
                powerLevelContentOverride = null,
                joinRuleOverride = null,
                historyVisibilityOverride = null,
                canonicalAlias = null,
                isSpace = false
            )
            val roomId = client.createRoom(params)
            val displayName = try { client.getProfile(userId).displayName ?: userId } catch (_: Exception) { userId }
            val avatarUrl = try { client.getProfile(userId).avatarUrl } catch (_: Exception) { null }
            chatRoomDao.insertChatRoom(
                ChatRoomEntity(
                    roomId = roomId,
                    name = displayName,
                    avatarUrl = avatarUrl,
                    lastMessage = null,
                    lastMessageAt = 0L,
                    unreadCount = 0,
                    isDm = true,
                    membersJoined = userId
                )
            )
            upsertRoomMembers(roomId, userId)
            Result.success(roomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun findExistingDirectRoomIdWithUser(userId: String): String? {
        val client = matrixClientManager.getClient() ?: return null
        val rooms = client.rooms()
        for (room in rooms) {
            val isDm = try { room.isDirect() } catch (_: Exception) { false }
            if (!isDm) continue

            val isTargetMember = try {
                room.member(userId)
                true
            } catch (_: Exception) {
                false
            }
            if (isTargetMember) {
                return room.id()
            }
        }
        return null
    }

    private suspend fun upsertDmSnapshotFromRoom(
        roomId: String,
        targetUserId: String,
        currentUserId: String?
    ) {
        val client = matrixClientManager.getClient() ?: return
        val room = client.getRoom(roomId) ?: return
        val existing = chatRoomDao.findByRoomId(roomId)
        val name = try { room.displayName() ?: existing?.name ?: targetUserId } catch (_: Exception) { existing?.name ?: targetUserId }
        val avatarUrl = try { room.avatarUrl() } catch (_: Exception) { existing?.avatarUrl }
        val membersJoined = resolveDmMemberIds(roomId, room, currentUserId).ifBlank { targetUserId }

        chatRoomDao.insertChatRoom(
            ChatRoomEntity(
                roomId = roomId,
                name = name,
                avatarUrl = avatarUrl,
                lastMessage = existing?.lastMessage,
                lastMessageAt = existing?.lastMessageAt ?: 0L,
                unreadCount = existing?.unreadCount ?: 0,
                isDm = true,
                membersJoined = membersJoined
            )
        )
        upsertRoomMembers(roomId, membersJoined)
    }

    private suspend fun resolveDmMemberIds(
        roomId: String,
        fallbackRoom: org.matrix.rustcomponents.sdk.Room? = null,
        currentUserId: String?
    ): String {
        val room = fallbackRoom ?: matrixClientManager.getClient()?.getRoom(roomId) ?: return ""
        val candidateIds = linkedSetOf<String>()
        val heroes = try { room.heroes() } catch (_: Exception) { emptyList() }
        heroes.mapNotNullTo(candidateIds) { it.userId }

        val filtered = candidateIds.filter { it != currentUserId }
        return filtered.joinToString(",")
    }

    private suspend fun upsertRoomMembers(roomId: String, membersJoined: String) {
        val members = membersJoined.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (members.isEmpty()) return
        chatRoomMemberDao.deleteMembersForRoom(roomId)
        chatRoomMemberDao.insertMembers(members.map { ChatRoomMemberEntity(roomId = roomId, userId = it) })
    }
}
