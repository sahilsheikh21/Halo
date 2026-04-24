package com.halo.ui.common

/**
 * Generic UI state wrapper used across ViewModels.
 * Replaces ad-hoc loading/error booleans with a unified sealed class.
 */
sealed class UiState<out T> {
    /** Initial state before any data has loaded */
    object Loading : UiState<Nothing>()

    /** Data successfully loaded */
    data class Success<T>(val data: T) : UiState<T>()

    /** Operation failed with an error */
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>()

    /** Screen with no content to show (after a successful load) */
    object Empty : UiState<Nothing>()
}

/** Convenience: true while in Loading state */
val UiState<*>.isLoading get() = this is UiState.Loading

/** Convenience: unwrap data or null */
fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data
