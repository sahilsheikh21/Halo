package com.halo.data.repository

import com.halo.data.local.dao.ChatRoomDao
import com.halo.data.local.entity.toDomain
import com.halo.data.matrix.MatrixClientManager
import com.halo.domain.model.ChatRoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val matrixClientManager: MatrixClientManager,
    private val chatRoomDao: ChatRoomDao
) {
    val chatRooms: Flow<List<ChatRoom>> = chatRoomDao.getAllChatRooms().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun refreshChatRooms() {
        // TODO: Query DM rooms via Sliding Sync
        
        // Temporarily seed MockData if DB is empty so UI works during transition
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

    suspend fun sendMessage(roomId: String, body: String) {
        // TODO: Send m.room.message via Matrix SDK
    }

    suspend fun createDirectMessage(userId: String): Result<String> {
        // TODO: Create DM room via Matrix SDK
        return Result.success("placeholder_room_id")
    }
}
