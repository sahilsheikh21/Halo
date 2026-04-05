package com.halo.domain.model

/**
 * Domain model representing a story in Halo.
 * Stories expire after 24 hours (enforced client-side).
 */
data class Story(
    val eventId: String,
    val authorId: String,
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val mediaUrl: String,
    val mxcUri: String,
    val storyType: String = "image", // "image" | "video"
    val durationMs: Long = 5000,
    val caption: String? = null,
    val createdAt: Long = 0,
    val isSeen: Boolean = false,
    val isExpired: Boolean = false
) {
    val remainingTimeMs: Long
        get() {
            val elapsed = System.currentTimeMillis() - createdAt
            val remaining = TTL_MS - elapsed
            return if (remaining > 0) remaining else 0
        }

    companion object {
        const val TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
}

/**
 * A group of stories from a single user, for the story bar display.
 */
data class StoryGroup(
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val stories: List<Story>,
    val hasUnseenStories: Boolean
)
