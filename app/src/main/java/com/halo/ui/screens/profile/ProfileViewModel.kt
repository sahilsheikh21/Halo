package com.halo.ui.screens.profile

import androidx.lifecycle.ViewModel
import com.halo.data.mock.MockData
import com.halo.data.repository.UserRepository
import com.halo.domain.model.Post
import com.halo.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    fun observeUser(userId: String): Flow<UserProfile?> {
        // Serve from mock data for now
        val user = MockData.users.find { it.userId == userId }
        return flowOf(user)
    }

    fun getPostsForUser(userId: String): List<Post> {
        return MockData.posts.filter { it.authorId == userId }
    }

    fun toggleFollow() {
        _isFollowing.value = !_isFollowing.value
    }
}
