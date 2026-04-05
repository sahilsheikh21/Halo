package com.halo.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.data.mock.MockData
import com.halo.data.repository.FeedRepository
import com.halo.data.repository.StoryRepository
import com.halo.domain.model.Post
import com.halo.domain.model.StoryGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val storyRepository: StoryRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Use mock data until Matrix SDK is wired up
    private val _feedPosts = MutableStateFlow<List<Post>>(MockData.posts)
    val feedPosts: StateFlow<List<Post>> = _feedPosts.asStateFlow()

    private val _storyGroups = MutableStateFlow<List<StoryGroup>>(MockData.storyGroups)
    val storyGroups: StateFlow<List<StoryGroup>> = _storyGroups.asStateFlow()

    fun toggleLike(eventId: String) {
        viewModelScope.launch {
            val current = _feedPosts.value.toMutableList()
            val idx = current.indexOfFirst { it.eventId == eventId }
            if (idx >= 0) {
                val post = current[idx]
                val nowLiked = !post.isLikedByMe
                current[idx] = post.copy(
                    isLikedByMe = nowLiked,
                    likeCount = post.likeCount + if (nowLiked) 1 else -1
                )
                _feedPosts.value = current
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // TODO: call feedRepository.refreshFeed() once Matrix SDK integrated
            kotlinx.coroutines.delay(800)
            _isRefreshing.value = false
        }
    }
}
