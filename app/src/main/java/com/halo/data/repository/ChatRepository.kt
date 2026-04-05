package com.halo.data.repository

import com.halo.data.matrix.MatrixClientManager
import com.halo.domain.model.ChatRoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val matrixClientManager: MatrixClientManager
) {
    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: Flow<List<ChatRoom>> = _chatRooms

    suspend fun refreshChatRooms() {
        // TODO: Query DM rooms via Sliding Sync
    }

    suspend fun sendMessage(roomId: String, body: String) {
        // TODO: Send m.room.message via Matrix SDK
    }

    suspend fun createDirectMessage(userId: String): Result<String> {
        // TODO: Create DM room via Matrix SDK
        return Result.success("placeholder_room_id")
    }
}
