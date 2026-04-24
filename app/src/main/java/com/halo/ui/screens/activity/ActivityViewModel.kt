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

    private val mockActivities = listOf(
        ActivityItem(
            id = "act_1", type = ActivityType.LIKE, actorName = "Aurora Sky",
            actorAvatarUrl = "https://i.pravatar.cc/150?img=47", actorId = "@aurora:matrix.org",
            postImageUrl = "https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?w=100",
            text = "liked your photo.", timestampMs = System.currentTimeMillis() - 600_000
        ),
        ActivityItem(
            id = "act_2", type = ActivityType.COMMENT, actorName = "Neon Drift",
            actorAvatarUrl = "https://i.pravatar.cc/150?img=12", actorId = "@neon:matrix.org",
            postImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=100",
            text = "commented: \"absolute fire 🔥\"", timestampMs = System.currentTimeMillis() - 3_600_000
        ),
        ActivityItem(
            id = "act_3", type = ActivityType.FOLLOW, actorName = "Prism Studio",
            actorAvatarUrl = "https://i.pravatar.cc/150?img=5", actorId = "@prism:matrix.org",
            text = "started following you.", timestampMs = System.currentTimeMillis() - 7_200_000
        ),
        ActivityItem(
            id = "act_4", type = ActivityType.LIKE, actorName = "Solaris",
            actorAvatarUrl = "https://i.pravatar.cc/150?img=32", actorId = "@solaris:matrix.org",
            postImageUrl = "https://images.unsplash.com/photo-1502680390469-be75c86b636f?w=100",
            text = "liked your photo.", timestampMs = System.currentTimeMillis() - 18_000_000
        ),
        ActivityItem(
            id = "act_5", type = ActivityType.MENTION, actorName = "Wave Rider",
            actorAvatarUrl = "https://i.pravatar.cc/150?img=68", actorId = "@wave:matrix.org",
            postImageUrl = "https://images.unsplash.com/photo-1502680390469-be75c86b636f?w=100",
            text = "mentioned you in a comment.", timestampMs = System.currentTimeMillis() - 86_400_000
        ),
        ActivityItem(
            id = "act_6", type = ActivityType.FOLLOW, actorName = "Ember Rose",
            actorAvatarUrl = "https://i.pravatar.cc/150?img=25", actorId = "@ember:matrix.org",
            text = "started following you.", timestampMs = System.currentTimeMillis() - 172_800_000
        )
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // TODO: Fetch real activity from Matrix (reactions + comments directed at current user)
                delay(300) // simulate network
                _uiState.value = if (mockActivities.isEmpty()) {
                    UiState.Empty
                } else {
                    UiState.Success(mockActivities)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load activity", e)
            }
        }
    }
}
