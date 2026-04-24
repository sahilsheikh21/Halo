package com.halo.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.data.repository.ChatRepository
import com.halo.domain.model.ChatRoom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    fun getRoomTimeline(roomId: String): Flow<List<ChatMessage>> {
        return chatRepository.getRoomTimeline(roomId)
    }

    fun getRoomDetails(roomId: String): Flow<ChatRoom?> {
        return chatRepository.chatRooms.map { rooms ->
            rooms.find { it.roomId == roomId }
        }
    }

    fun sendMessage(roomId: String, text: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(roomId, text)
        }
    }
}
