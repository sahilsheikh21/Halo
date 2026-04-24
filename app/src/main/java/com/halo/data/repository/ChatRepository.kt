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

    // Temporary in-memory mock timeline until Matrix SDK timeline is wired
    private val _mockTimelines = mutableMapOf<String, kotlinx.coroutines.flow.MutableStateFlow<List<com.halo.ui.screens.chat.ChatMessage>>>()

    fun getRoomTimeline(roomId: String): Flow<List<com.halo.ui.screens.chat.ChatMessage>> {
        // TODO: Return actual room timeline flow from Matrix SDK Room.timeline()
        if (!_mockTimelines.containsKey(roomId)) {
            val initialMessages = listOf(
                com.halo.ui.screens.chat.ChatMessage("m1", "Hey! How's it going? 👋", false, System.currentTimeMillis() - 86_400_000),
                com.halo.ui.screens.chat.ChatMessage("m2", "All good! Just finished editing some shots from the trip", true, System.currentTimeMillis() - 85_800_000),
                com.halo.ui.screens.chat.ChatMessage("m3", "Ooh nice, can't wait to see them 🔥", false, System.currentTimeMillis() - 82_000_000),
                com.halo.ui.screens.chat.ChatMessage("m4", "I'll post them later today. The lighting in Greece was unreal", true, System.currentTimeMillis() - 80_000_000),
                com.halo.ui.screens.chat.ChatMessage("m5", "Let's catch up soon!", false, System.currentTimeMillis() - 900_000)
            )
            _mockTimelines[roomId] = kotlinx.coroutines.flow.MutableStateFlow(initialMessages)
        }
        return _mockTimelines[roomId]!!
    }

    suspend fun sendMessage(roomId: String, body: String) {
        // TODO: Send m.room.message via Matrix SDK
        val timelineFlow = _mockTimelines[roomId] ?: return
        val currentList = timelineFlow.value.toMutableList()
        currentList.add(
            com.halo.ui.screens.chat.ChatMessage(
                id = "m_${System.currentTimeMillis()}",
                body = body,
                isMe = true,
                timestamp = System.currentTimeMillis()
            )
        )
        timelineFlow.value = currentList
    }

    suspend fun createDirectMessage(userId: String): Result<String> {
        // TODO: Create DM room via Matrix SDK
        return Result.success("placeholder_room_id")
    }
}
