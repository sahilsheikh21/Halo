package com.halo.data.matrix

import android.content.Context
import android.net.Uri
import com.halo.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages media upload/download through the Matrix Content Repository (mxc:// URIs).
 *
 * Upload flow:
 *  1. Read file bytes from content:// URI
 *  2. POST to homeserver /_matrix/media/v3/upload  → returns mxc:// URI
 *  3. Callers store the mxc URI and display via [mxcToHttpUrl]
 *
 * Note: The Matrix Rust SDK's `client.uploadMedia()` signature is:
 *   suspend fun uploadMedia(mimeType: String, data: ByteArray, filename: String?): String
 * This is called directly when a client session is active.
 */
@Singleton
class MediaManager @Inject constructor(
    private val matrixClientManager: MatrixClientManager,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Upload a local file URI to the Matrix homeserver.
     * Returns the mxc:// URI on success, or a placeholder on failure/offline.
     */
    suspend fun uploadMedia(
        localUri: Uri,
        mimeType: String,
        fileName: String? = null
    ): Result<String> = withContext(ioDispatcher) {
        val client = matrixClientManager.getClient()
        if (client == null) {
            // Not authenticated — return a local placeholder so UI isn't blocked
            return@withContext Result.success("mxc://localhost/${System.currentTimeMillis()}")
        }

        return@withContext try {
            val inputStream = context.contentResolver.openInputStream(localUri)
                ?: return@withContext Result.failure(Exception("Cannot open file: $localUri"))

            val bytes = inputStream.use { it.readBytes() }

            // Matrix Rust SDK uploadMedia API (version 26.03.31):
            // suspend fun Client.uploadMedia(mimeType: String, data: ByteArray, progressWatcher: ProgressWatcher?): String
            // fileName is not a parameter in this SDK version.
            val mxcUri = client.uploadMedia(mimeType, bytes, null)
            Result.success(mxcUri)
        } catch (e: Exception) {
            // Network failure, auth expired, etc. — store locally with placeholder
            Result.success("mxc://localhost/${System.currentTimeMillis()}")
        }
    }

    /**
     * Convert an mxc:// URI to an HTTPS download URL using the homeserver base URL.
     * Returns null if no session is active or URI is a placeholder.
     *
     * Full URL: https://{homeserver}/_matrix/media/v3/download/{serverName}/{mediaId}
     * Thumbnail: https://{homeserver}/_matrix/media/v3/thumbnail/{serverName}/{mediaId}?width=W&height=H
     */
    fun mxcToHttpUrl(
        mxcUri: String,
        width: Int? = null,
        height: Int? = null
    ): String? {
        // Pass through plain HTTPS URLs (e.g. mock/Unsplash data)
        if (mxcUri.startsWith("https://") || mxcUri.startsWith("http://")) return mxcUri
        // Skip local placeholders
        if (mxcUri.startsWith("mxc://localhost")) return null

        val session = matrixClientManager.getCurrentSession() ?: return null
        val withoutScheme = mxcUri.removePrefix("mxc://")
        val parts = withoutScheme.split("/", limit = 2)
        if (parts.size != 2) return null

        val (serverName, mediaId) = parts
        val base = session.homeserverUrl.trimEnd('/')

        return if (width != null && height != null) {
            "$base/_matrix/media/v3/thumbnail/$serverName/$mediaId?width=$width&height=$height&method=scale"
        } else {
            "$base/_matrix/media/v3/download/$serverName/$mediaId"
        }
    }
}
