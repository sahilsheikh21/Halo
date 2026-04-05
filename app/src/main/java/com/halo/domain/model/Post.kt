package com.halo.domain.model

/**
 * Domain model representing a post in the Halo feed.
 */
data class Post(
    val eventId: String,
    val authorId: String,
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val caption: String = "",
    val mediaUrls: List<MediaItem> = emptyList(),
    val locationName: String? = null,
    val tags: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val createdAt: Long = 0
)

data class MediaItem(
    val url: String,
    val mxcUri: String,
    val mimeType: String,
    val width: Int = 0,
    val height: Int = 0,
    val thumbnailUrl: String? = null,
    val blurhash: String? = null
)
