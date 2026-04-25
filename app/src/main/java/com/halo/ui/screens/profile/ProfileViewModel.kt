package com.halo.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.data.repository.FeedRepository
import com.halo.data.repository.UserRepository
import com.halo.domain.model.Post
import com.halo.domain.model.UserProfile
import com.halo.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val feedRepository: FeedRepository,
    private val chatRepository: com.halo.data.repository.ChatRepository
) : ViewModel() {

    private val _followState = MutableStateFlow<UiState<Unit>?>(null)
    val followState: StateFlow<UiState<Unit>?> = _followState.asStateFlow()

    fun loadUser(userId: String) {
        viewModelScope.launch {
            // Check if user exists in DB first (observeUser will handle the UI update)
            val existing = userRepository.getUserProfile(userId)
            if (existing == null) {
                userRepository.fetchAndCacheUser(userId)
            }
        }
    }

    fun observeUser(userId: String): Flow<UserProfile?> {
        return userRepository.observeUser(userId)
    }

    fun getPostsForUser(userId: String): Flow<List<Post>> {
        return feedRepository.getPostsByAuthor(userId)
    }

    fun toggleFollow(userId: String, isCurrentlyFollowing: Boolean) {
        viewModelScope.launch {
            _followState.value = UiState.Loading
            try {
                if (isCurrentlyFollowing) {
                    userRepository.unfollowUser(userId)
                } else {
                    userRepository.followUser(userId)
                }
                _followState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _followState.value = UiState.Error(e.message ?: "Follow action failed", e)
            }
        }
    }

    fun startDM(userId: String, onRoomReady: (String) -> Unit) {
        viewModelScope.launch {
            chatRepository.createDirectMessage(userId).onSuccess { roomId ->
                onRoomReady(roomId)
            }
        }
    }

    fun clearFollowState() {
        _followState.value = null
    }
}

