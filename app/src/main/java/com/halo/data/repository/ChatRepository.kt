package com.halo.data.repository

import android.util.Log
import com.halo.data.local.dao.ChatRoomDao
import com.halo.data.local.dao.MessageDao
import com.halo.data.local.entity.ChatRoomEntity
import com.halo.data.local.entity.MessageEntity
import com.halo.data.local.entity.toDomain
import com.halo.data.matrix.MatrixClientManager
import com.halo.domain.model.ChatMessage
import com.halo.domain.model.ChatRoom
import com.halo.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.matrix.rustcomponents.sdk.CreateRoomParameters
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
    private val messageDao: MessageDao
) {

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
     */
    suspend fun refreshChatRooms() {
        val client = matrixClientManager.getClient() ?: return
        val rooms = client.rooms()
        val entities = rooms.map { room ->
            val roomId = room.id()
            // Room display name resolution is a future improvement; for now store the
            // raw ID as a placeholder — it will not overwrite an existing richer record.
            ChatRoomEntity(
                roomId        = roomId,
                name          = roomId,
                avatarUrl     = null,
                lastMessage   = null,
                lastMessageAt = 0L,
                unreadCount   = 0,
                isDm          = false,
                membersJoined = ""
            )
        }
        // B8 fix: IGNORE strategy preserves existing metadata
        chatRoomDao.insertChatRoomsIgnore(entities)
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

    suspend fun createDirectMessage(userId: String): Result<String> {
        val client = matrixClientManager.getClient()
            ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val params = CreateRoomParameters(
                name                      = null,
                topic                     = null,
                isDirect                  = true,
                isEncrypted               = true,
                visibility                = RoomVisibility.Private,
                preset                    = RoomPreset.PRIVATE_CHAT,
                invite                    = listOf(userId),
                avatar                    = null,
                powerLevelContentOverride = null,
                joinRuleOverride          = null,
                historyVisibilityOverride = null,
                canonicalAlias            = null,
                isSpace                   = false
            )
            val roomId = client.createRoom(params)
            chatRoomDao.insertChatRoom(
                ChatRoomEntity(
                    roomId        = roomId,
                    name          = userId,
                    avatarUrl     = null,
                    lastMessage   = null,
                    lastMessageAt = 0L,
                    unreadCount   = 0,
                    isDm          = true,
                    membersJoined = userId
                )
            )
            Result.success(roomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
