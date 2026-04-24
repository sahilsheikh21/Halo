package com.halo.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.data.repository.FeedRepository
import com.halo.data.repository.UserRepository
import com.halo.domain.model.Post
import com.halo.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val feedRepository: FeedRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            userRepository.refreshUsers()
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
            if (isCurrentlyFollowing) {
                userRepository.unfollowUser(userId)
            } else {
                userRepository.followUser(userId)
            }
        }
    }
}
