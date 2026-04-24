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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
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
    val feedPosts: StateFlow<List<Post>> = feedRepository.getFeedPosts()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val storyGroups: StateFlow<List<StoryGroup>> = storyRepository.getActiveStoryGroups()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleLike(eventId: String) {
        viewModelScope.launch {
            val post = feedPosts.value.find { it.eventId == eventId } ?: return@launch
            if (post.isLikedByMe) {
                feedRepository.unlikePost(eventId)
            } else {
                feedRepository.likePost(eventId)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                kotlinx.coroutines.joinAll(
                    launch { feedRepository.refreshFeed() },
                    launch { storyRepository.refreshStories() }
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
