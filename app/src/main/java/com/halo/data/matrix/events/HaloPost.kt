package com.halo.data.matrix.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Custom Matrix timeline event: com.halo.post
 *
 * Sent as a timeline event in the user's Feed Room.
 * Represents a single post with media, caption, and metadata.
 */
@Serializable
data class HaloPost(
    @SerialName("caption")
    val caption: String = "",

    @SerialName("media")
    val media: List<PostMedia> = emptyList(),

    @SerialName("location")
    val location: PostLocation? = null,

    @SerialName("tags")
    val tags: List<String> = emptyList(),

    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val EVENT_TYPE = "com.halo.post"
    }
}

@Serializable
data class PostMedia(
    @SerialName("mxc")
    val mxcUri: String,

    @SerialName("type")
    val mimeType: String, // "image/jpeg", "video/mp4"

    @SerialName("width")
    val width: Int = 0,

    @SerialName("height")
    val height: Int = 0,

    @SerialName("thumbnail_mxc")
    val thumbnailMxc: String? = null,

    @SerialName("blurhash")
    val blurhash: String? = null,

    @SerialName("size_bytes")
    val sizeBytes: Long = 0
)

@Serializable
data class PostLocation(
    @SerialName("name")
    val name: String,

    @SerialName("latitude")
    val latitude: Double? = null,

    @SerialName("longitude")
    val longitude: Double? = null
)
