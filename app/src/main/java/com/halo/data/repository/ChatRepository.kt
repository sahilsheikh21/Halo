package com.halo.data.repository

import com.halo.data.local.dao.ChatRoomDao
import com.halo.data.local.entity.toDomain
import com.halo.data.matrix.MatrixClientManager
import com.halo.domain.model.ChatRoom
import com.halo.ui.screens.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for DM/chat rooms and message timelines.
 *
 * Architecture:
 * - Room list: backed by [ChatRoomDao] (Room DB), seeded from mock until Sliding Sync is wired
 * - Message timeline: in-memory [MutableStateFlow] with optimistic updates;
 *   real SDK timeline subscription is registered per-room via [subscribeRoomTimeline]
 * - Send: delegates to Matrix Rust SDK room.timeline().sendMessage() when authenticated;
 *   falls back to local optimistic append when offline or unauthenticated
 */
@Singleton
class ChatRepository @Inject constructor(
    private val matrixClientManager: MatrixClientManager,
    private val chatRoomDao: ChatRoomDao
) {
    // ─── Room list ────────────────────────────────────────────────────────────

    val chatRooms: Flow<List<ChatRoom>> = chatRoomDao.getAllChatRooms().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun refreshChatRooms() {
        // TODO: When SlidingSync RoomListService is available, query DM rooms from SDK.
        // For now, seed mock data so the Messages screen shows content.
        val currentRooms = chatRoomDao.getAllChatRooms().first()
        if (currentRooms.isEmpty()) {
            val mockEntities = com.halo.data.mock.MockData.chatRooms.map { room ->
                com.halo.data.local.entity.ChatRoomEntity(
                    roomId = room.roomId,
                    name = room.name,
                    avatarUrl = room.avatarUrl,
                    lastMessage = room.lastMessage,
                    lastMessageAt = room.lastMessageAt,
                    unreadCount = room.unreadCount,
                    isDm = room.isDm,
                    membersJoined = room.members.joinToString(",")
                )
            }
            chatRoomDao.insertChatRooms(mockEntities)
        }
    }

    // ─── Timeline ────────────────────────────────────────────────────────────

    /**
     * In-memory timeline cache.
     * Real SDK rooms will have their timelines subscribed via [subscribeRoomTimeline].
     */
    private val _timelines = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()

    /**
     * Returns a reactive [Flow] of [ChatMessage] for the given room.
     *
     * If the Matrix SDK has an active session and the room exists, a timeline
     * subscription should be registered via [subscribeRoomTimeline] first.
     * Otherwise the in-memory mock is returned.
     */
    fun getRoomTimeline(roomId: String): Flow<List<ChatMessage>> {
        return getOrCreateTimeline(roomId)
    }

    /**
     * Wire a real Matrix SDK room timeline into this repository.
     *
     * Call this after [SlidingSyncManager.startSync] and room subscription is available.
     * The SDK's TimelineListener callbacks should call [appendIncomingMessage].
     *
     * TODO: Implement when SDK timeline listener API surface is confirmed.
     */
    suspend fun subscribeRoomTimeline(roomId: String) {
        val client = matrixClientManager.getClient() ?: return
        // SDK: val room = client.getRoom(roomId) ?: return
        // SDK: val timeline = room.timeline()
        // SDK: timeline.addListener(object : TimelineListener { ... })
    }

    /**
     * Append a message received from the SDK timeline listener into the cache.
     */
    fun appendIncomingMessage(roomId: String, message: ChatMessage) {
        val flow = getOrCreateTimeline(roomId)
        flow.value = flow.value + message
    }

    // ─── Send ─────────────────────────────────────────────────────────────────

    /**
     * Send a text message to a Matrix room.
     *
     * Strategy:
     * 1. Optimistic update: immediately append to the local timeline flow
     * 2. If SDK client is available, send via Matrix (m.room.message event)
     * 3. On failure, the optimistic message stays visible (offline-first UX)
     *
     * SDK call (when API confirmed):
     *   val content = RoomMessageEventContentWithoutRelation.text(body)
     *   client.getRoom(roomId)?.timeline()?.sendMessage(content)
     */
    suspend fun sendMessage(roomId: String, body: String) {
        // 1. Optimistic local append
        val flow = getOrCreateTimeline(roomId)
        flow.value = flow.value + ChatMessage(
            id = "local_${System.currentTimeMillis()}",
            body = body,
            isMe = true,
            timestamp = System.currentTimeMillis()
        )

        // 2. Send via SDK (if authenticated)
        val client = matrixClientManager.getClient() ?: return
        try {
            // SDK: val content = RoomMessageEventContentWithoutRelation.text(body)
            // SDK: client.getRoom(roomId)?.timeline()?.sendMessage(content)
            // TODO: Uncomment when SDK timeline send API is confirmed for version 26.03.31
        } catch (e: Exception) {
            // Message stays in optimistic state — retry logic can be added
        }
    }

    // ─── Direct Messages ──────────────────────────────────────────────────────

    /**
     * Create a Direct Message room with the given Matrix user ID.
     * Returns the room ID on success.
     *
     * SDK call:
     *   client.createRoom(CreateRoomParameters(isDirect = true, invite = listOf(userId)))
     */
    suspend fun createDirectMessage(userId: String): Result<String> {
        val client = matrixClientManager.getClient()
            ?: return Result.success("dm_${userId.replace("@", "").replace(":", "_")}")

        return try {
            // TODO: Replace with actual SDK call when CreateRoomParameters API is confirmed:
            // val roomId = client.createRoom(CreateRoomParameters(
            //     name = null, isEncrypted = true, isDirect = true, visibility = ...,
            //     preset = RoomPreset.PRIVATE_CHAT, invite = listOf(userId)
            // ))
            // For now return a placeholder that lets navigation work
            Result.success("dm_${userId.replace("@", "").replace(":", "_")}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun getOrCreateTimeline(roomId: String): MutableStateFlow<List<ChatMessage>> {
        return _timelines.getOrPut(roomId) {
            MutableStateFlow(
                listOf(
                    ChatMessage("m1", "Hey! How's it going? 👋", false, System.currentTimeMillis() - 86_400_000),
                    ChatMessage("m2", "All good! Just finished editing some shots from the trip", true, System.currentTimeMillis() - 85_800_000),
                    ChatMessage("m3", "Ooh nice, can't wait to see them 🔥", false, System.currentTimeMillis() - 82_000_000),
                    ChatMessage("m4", "I'll post them later today. The lighting in Greece was unreal", true, System.currentTimeMillis() - 80_000_000),
                    ChatMessage("m5", "Let's catch up soon!", false, System.currentTimeMillis() - 900_000)
                )
            )
        }
    }
}
