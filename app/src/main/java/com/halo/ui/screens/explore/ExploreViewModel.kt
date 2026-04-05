package com.halo.ui.screens.explore

import androidx.lifecycle.ViewModel
import com.halo.data.mock.MockData
import com.halo.data.repository.UserRepository
import com.halo.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Mock explore grid — image URL + author ID pairs
    private val _exploreItems = MutableStateFlow(MockData.exploreImages)
    val exploreItems: StateFlow<List<Pair<String, String>>> = _exploreItems.asStateFlow()

    private val _allUsers = MutableStateFlow(MockData.users)

    val searchResults: StateFlow<List<UserProfile>> = MutableStateFlow(emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getFilteredUsers(): List<UserProfile> {
        val q = _searchQuery.value.trim().lowercase()
        if (q.isBlank()) return emptyList()
        return MockData.users.filter {
            it.displayName.lowercase().contains(q) ||
            it.username.lowercase().contains(q) ||
            it.bio.lowercase().contains(q)
        }
    }
}
