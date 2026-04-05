package com.halo.ui.screens.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateViewModel @Inject constructor() : ViewModel() {

    private val _caption = MutableStateFlow("")
    val caption: StateFlow<String> = _caption.asStateFlow()

    private val _selectedMediaUri = MutableStateFlow<Uri?>(null)
    val selectedMediaUri: StateFlow<Uri?> = _selectedMediaUri.asStateFlow()

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    fun updateCaption(text: String) {
        _caption.value = text
    }

    fun updateLocation(text: String) {
        _location.value = text
    }

    fun onMediaSelected(uri: Uri) {
        _selectedMediaUri.value = uri
    }

    // Legacy compat
    fun selectMedia(uri: String) {
        _selectedMediaUri.value = Uri.parse(uri)
    }

    fun createPost(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isPosting.value = true
            // TODO: Upload media to Matrix, send com.halo.post event
            delay(1000) // simulate upload
            _isPosting.value = false
            _caption.value = ""
            _selectedMediaUri.value = null
            _location.value = ""
            onComplete()
        }
    }

    fun createStory() {
        viewModelScope.launch {
            _isPosting.value = true
            // TODO: Upload media, send com.halo.story state event (24h TTL enforced client-side)
            delay(800)
            _isPosting.value = false
        }
    }
}
