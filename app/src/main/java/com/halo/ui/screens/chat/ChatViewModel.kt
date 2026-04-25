package com.halo.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.data.repository.ChatRepository
import com.halo.domain.model.ChatMessage
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

    /**
     * B3: Retries a message that previously failed to send.
     *
     * Delegates to [ChatRepository.retryMessage] which deletes the FAILED
     * placeholder row and re-submits the body as a fresh [sendMessage] call,
     * giving the user a new SENDING → SENT / FAILED cycle.
     */
    fun retryMessage(roomId: String, messageId: String, body: String) {
        viewModelScope.launch {
            chatRepository.retryMessage(roomId, messageId, body)
        }
    }
}
