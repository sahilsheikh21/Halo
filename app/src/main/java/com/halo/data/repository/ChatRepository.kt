package com.halo.data.repository

import com.halo.data.local.dao.ChatRoomDao
import com.halo.data.local.dao.MessageDao
import com.halo.data.local.entity.MessageEntity
import com.halo.data.local.entity.toDomain
import com.halo.data.matrix.MatrixClientManager
import com.halo.domain.model.ChatRoom
import com.halo.ui.screens.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val matrixClientManager: MatrixClientManager,
    private val chatRoomDao: ChatRoomDao,
    private val messageDao: MessageDao
) {
    // ─── Room list ────────────────────────────────────────────────────────────

    val chatRooms: Flow<List<ChatRoom>> = chatRoomDao.getAllChatRooms().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun refreshChatRooms() {
        // TODO: Sync chat rooms from Matrix SDK and update local DB
    }

    // ─── Timeline ────────────────────────────────────────────────────────────

    /**
     * Returns a reactive [Flow] of [ChatMessage] for the given room, backed by Room DB.
     */
    fun getRoomTimeline(roomId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForRoom(roomId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Append a message received from the SDK timeline listener into the DB.
     */
    suspend fun appendIncomingMessage(
        roomId: String,
        eventId: String,
        senderId: String,
        body: String,
        timestamp: Long
    ) {
        val session = matrixClientManager.getCurrentSession()
        val isMe = senderId == session?.userId

        messageDao.insertMessage(
            MessageEntity(
                id = eventId,
                roomId = roomId,
                senderId = senderId,
                body = body,
                isMe = isMe,
                timestamp = timestamp
            )
        )

        // Update last message in room list
        chatRoomDao.getChatRoom(roomId)?.let { room ->
            chatRoomDao.insertChatRoom(
                room.copy(
                    lastMessage = body,
                    lastMessageAt = timestamp
                )
            )
        }
    }

    // ─── Send ─────────────────────────────────────────────────────────────────

    /**
     * Send a text message to a Matrix room.
     */
    suspend fun sendMessage(roomId: String, body: String) {
        val session = matrixClientManager.getCurrentSession() ?: return
        val eventId = "local_${System.currentTimeMillis()}"

        // 1. Persist locally for immediate UI update
        appendIncomingMessage(
            roomId = roomId,
            eventId = eventId,
            senderId = session.userId,
            body = body,
            timestamp = System.currentTimeMillis()
        )

        // 2. Send via SDK
        val client = matrixClientManager.getClient() ?: return
        try {
            // SDK: val room = client.getRoom(roomId) ?: return
            // SDK: room.timeline().sendMessage(RoomMessageEventContentWithoutRelation.text(body))
            // TODO: Uncomment when SDK API confirmed
        } catch (e: Exception) {
            // Logic to mark message as failed can be added here
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
}
