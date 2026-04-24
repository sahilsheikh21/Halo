package com.halo.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.data.repository.FeedRepository
import com.halo.data.repository.StoryRepository
import com.halo.domain.model.Post
import com.halo.domain.model.StoryGroup
import com.halo.ui.common.ConnectivityObserver
import com.halo.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val storyRepository: StoryRepository,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Observe online/offline status for offline banner */
    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val feedState: StateFlow<UiState<List<Post>>> = feedRepository.getFeedPosts()
        .map<List<Post>, UiState<List<Post>>> { posts ->
            if (posts.isEmpty()) UiState.Empty else UiState.Success(posts)
        }
        .catch { e -> emit(UiState.Error(e.message ?: "Failed to load feed", e)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )

    // Convenience for screens that just need the list
    val feedPosts: StateFlow<List<Post>> = feedRepository.getFeedPosts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val storyGroups: StateFlow<List<StoryGroup>> = storyRepository.getActiveStoryGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
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
                joinAll(
                    launch { feedRepository.refreshFeed() },
                    launch { storyRepository.refreshStories() }
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

