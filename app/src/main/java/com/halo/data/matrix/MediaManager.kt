package com.halo.data.matrix

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages media upload/download through Matrix Content Repository (mxc:// URIs).
 *
 * Handles:
 * - Uploading images/videos to homeserver → returns mxc:// URI
 * - Converting mxc:// URIs to downloadable HTTPS URLs
 * - Thumbnail generation and upload
 */
@Singleton
class MediaManager @Inject constructor(
    private val matrixClientManager: MatrixClientManager,
    @ApplicationContext private val context: Context
) {
    /**
     * Upload a local file to the homeserver's content repository.
     *
     * @param localUri Local content:// or file:// URI
     * @param mimeType MIME type of the file
     * @param fileName Optional filename
     * @return mxc:// URI of the uploaded content
     */
    suspend fun uploadMedia(
        localUri: Uri,
        mimeType: String,
        fileName: String? = null
    ): Result<String> {
        return try {
            // TODO: Implement with Matrix SDK
            // val inputStream = context.contentResolver.openInputStream(localUri)
            // val bytes = inputStream?.readBytes() ?: throw Exception("Cannot read file")
            // val mxcUri = client.uploadMedia(bytes, mimeType, fileName)
            // Result.success(mxcUri)

            // Placeholder
            Result.success("mxc://placeholder/${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert an mxc:// URI to an HTTPS download URL.
     *
     * @param mxcUri The mxc:// URI from the homeserver
     * @param width Optional thumbnail width
     * @param height Optional thumbnail height
     * @return HTTPS URL for downloading/displaying the media
     */
    fun mxcToHttpUrl(
        mxcUri: String,
        width: Int? = null,
        height: Int? = null
    ): String? {
        val session = matrixClientManager.getCurrentSession() ?: return null

        // mxc://server/media_id → https://server/_matrix/media/v3/download/server/media_id
        val parts = mxcUri.removePrefix("mxc://").split("/", limit = 2)
        if (parts.size != 2) return null

        val (serverName, mediaId) = parts
        val baseUrl = session.homeserverUrl

        return if (width != null && height != null) {
            "$baseUrl/_matrix/media/v3/thumbnail/$serverName/$mediaId?width=$width&height=$height&method=scale"
        } else {
            "$baseUrl/_matrix/media/v3/download/$serverName/$mediaId"
        }
    }

    /**
     * Upload a thumbnail version of the media.
     */
    suspend fun uploadThumbnail(
        localUri: Uri,
        maxWidth: Int = 800,
        maxHeight: Int = 600
    ): Result<String> {
        // TODO: Resize image locally, then upload
        return uploadMedia(localUri, "image/jpeg")
    }
}
