package com.halo.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.data.repository.FeedRepository
import com.halo.data.repository.UserRepository
import com.halo.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

sealed interface ExploreSearchUiState {
    data object Idle : ExploreSearchUiState
    data object Loading : ExploreSearchUiState
    data class Success(val users: List<UserProfile>) : ExploreSearchUiState
    data class Error(val message: String) : ExploreSearchUiState
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val feedRepository: FeedRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val exploreItems: StateFlow<List<Pair<String, String>>> = feedRepository.getFeedPosts(50, 0)
        .map { posts ->
            posts.mapNotNull { post ->
                val imageUrl = post.mediaUrls.firstOrNull()?.url
                if (imageUrl != null) imageUrl to post.authorId else null
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchUiState: StateFlow<ExploreSearchUiState> = _searchQuery
        .debounce(500)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf<ExploreSearchUiState>(ExploreSearchUiState.Idle)
            } else {
                flow<ExploreSearchUiState> {
                    emit(ExploreSearchUiState.Loading)
                    val result = userRepository.searchUsersReal(query)
                    emit(
                        result.fold(
                            onSuccess = { ExploreSearchUiState.Success(it) },
                            onFailure = {
                                ExploreSearchUiState.Error(
                                    it.message ?: "Search failed. Check your connection and session."
                                )
                            }
                        )
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExploreSearchUiState.Idle
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
