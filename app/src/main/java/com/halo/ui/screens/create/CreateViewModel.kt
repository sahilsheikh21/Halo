package com.halo.ui.screens.create

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val mediaManager: MediaManager,
    private val feedRepository: FeedRepository,
    private val storyRepository: StoryRepository
) : ViewModel() {

    private val _caption          = MutableStateFlow("")
    val caption: StateFlow<String> = _caption.asStateFlow()

    private val _selectedMediaUri  = MutableStateFlow<Uri?>(null)
    val selectedMediaUri: StateFlow<Uri?> = _selectedMediaUri.asStateFlow()

    private val _location          = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _isPosting         = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    /**
     * B7: Exposes upload / publish errors to the UI.
     * Non-null while an error is pending; the UI should clear it after showing
     * the Snackbar (call [clearError]).
     */
    private val _error             = MutableStateFlow<String?>(null)
    val error: StateFlow<String?>  = _error.asStateFlow()

    // ─── Input handlers ───────────────────────────────────────────────────────

    fun updateCaption(text: String)  { _caption.value = text }
    fun updateLocation(text: String) { _location.value = text }
    fun onMediaSelected(uri: Uri)    { _selectedMediaUri.value = uri }
    fun selectMedia(uri: String)     { _selectedMediaUri.value = Uri.parse(uri) } // legacy compat
    fun clearError()                 { _error.value = null }

    // ─── Actions ──────────────────────────────────────────────────────────────

    fun createPost(onComplete: () -> Unit = {}) {
        val uri = _selectedMediaUri.value ?: return
        viewModelScope.launch {
            _isPosting.value = true
            _error.value     = null

            // B6: Resolve the actual MIME type from the content URI instead of
            // always sending "image/jpeg". Picks up PNG, GIF, HEIC, MP4, etc.
            val mimeType = resolveMimeType(uri)

            val result = mediaManager.uploadMedia(uri, mimeType)
            val mxcUri = result.getOrNull()

            if (mxcUri != null) {
                feedRepository.publishPost(
                    caption  = _caption.value.trim(),
                    mediaMxc = mxcUri,
                    mimeType = mimeType,
                    location = _location.value.trim().ifBlank { null }
                )
                // Clear form and navigate away only on success
                _caption.value          = ""
                _selectedMediaUri.value = null
                _location.value         = ""
                _isPosting.value        = false
                onComplete()
            } else {
                // B7: Surface the failure — do NOT navigate away or clear the form
                // so the user still has their content and can retry.
                val cause = result.exceptionOrNull()?.message ?: "Unknown error"
                _error.value     = "Upload failed: $cause"
                _isPosting.value = false
            }
        }
    }

    fun createStory(onComplete: () -> Unit = {}) {
        val uri = _selectedMediaUri.value ?: return
        viewModelScope.launch {
            _isPosting.value = true
            _error.value     = null

            // B6: Same MIME-type fix for stories
            val mimeType = resolveMimeType(uri)

            val result = mediaManager.uploadMedia(uri, mimeType)
            val mxcUri = result.getOrNull()

            if (mxcUri != null) {
                storyRepository.publishStory(mediaMxc = mxcUri)
                _selectedMediaUri.value = null
                _isPosting.value        = false
                onComplete()
            } else {
                val cause = result.exceptionOrNull()?.message ?: "Unknown error"
                _error.value     = "Story upload failed: $cause"
                _isPosting.value = false
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * B6: Resolves the actual MIME type for [uri] using the system ContentResolver.
     * Falls back to "image/jpeg" only when the resolver returns null (e.g. for
     * file:// URIs that the resolver can't inspect).
     */
    private fun resolveMimeType(uri: Uri): String =
        context.contentResolver.getType(uri) ?: "image/jpeg"
}
