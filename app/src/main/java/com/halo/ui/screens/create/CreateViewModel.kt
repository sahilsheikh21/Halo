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

import com.halo.data.matrix.MediaManager
import com.halo.data.repository.FeedRepository
import com.halo.data.repository.StoryRepository

@HiltViewModel
class CreateViewModel @Inject constructor(
    private val mediaManager: MediaManager,
    private val feedRepository: FeedRepository,
    private val storyRepository: StoryRepository
) : ViewModel() {

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
        val uri = _selectedMediaUri.value ?: return
        
        viewModelScope.launch {
            _isPosting.value = true
            
            // 1. Upload Media
            val result = mediaManager.uploadMedia(uri, "image/jpeg")
            val mxcUri = result.getOrNull()
            
            if (mxcUri != null) {
                // 2. Publish Post
                feedRepository.publishPost(
                    caption = _caption.value.trim(),
                    mediaMxc = mxcUri,
                    mimeType = "image/jpeg",
                    location = _location.value.trim()
                )
            }
            
            _isPosting.value = false
            _caption.value = ""
            _selectedMediaUri.value = null
            _location.value = ""
            onComplete()
        }
    }

    fun createStory(onComplete: () -> Unit = {}) {
        val uri = _selectedMediaUri.value ?: return
        
        viewModelScope.launch {
            _isPosting.value = true
            
            // 1. Upload Media
            val result = mediaManager.uploadMedia(uri, "image/jpeg")
            val mxcUri = result.getOrNull()
            
            if (mxcUri != null) {
                // 2. Publish Story
                storyRepository.publishStory(mediaMxc = mxcUri)
            }
            
            _isPosting.value = false
            _selectedMediaUri.value = null
            onComplete()
        }
    }
}
