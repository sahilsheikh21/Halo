package com.halo.ui.screens.messages

import androidx.lifecycle.ViewModel
import com.halo.data.mock.MockData
import com.halo.data.repository.ChatRepository
import com.halo.domain.model.ChatRoom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(MockData.chatRooms)
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()
}
