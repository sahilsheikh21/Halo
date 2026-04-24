package com.halo.ui.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ActivityType { LIKE, COMMENT, FOLLOW, MENTION }

data class ActivityItem(
    val id: String,
    val type: ActivityType,
    val actorName: String,
    val actorAvatarUrl: String?,
    val actorId: String,
    val postImageUrl: String? = null,
    val text: String,
    val timestampMs: Long
)

@HiltViewModel
class ActivityViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ActivityItem>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ActivityItem>>> = _uiState.asStateFlow()

    // Convenience accessor for screens that collect the list directly
    val activities: StateFlow<List<ActivityItem>> = MutableStateFlow(emptyList<ActivityItem>()).also {
        viewModelScope.launch {
            // Propagate from uiState into flat list
            uiState.collect { state ->
                if (state is UiState.Success) it.value = state.data
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // TODO: Fetch real activity from Matrix (reactions + comments directed at current user)
                delay(300) // simulate network
                _uiState.value = UiState.Empty
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load activity", e)
            }
        }
    }
}
