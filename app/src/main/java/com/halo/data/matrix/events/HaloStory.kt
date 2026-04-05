package com.halo.data.matrix.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Custom Matrix state event: com.halo.story
 *
 * Sent as a state event in the user's Feed Room.
 * state_key = "{user_id}_{story_index}" for multi-story support.
 *
 * 24-hour TTL is enforced client-side (not by the homeserver).
 */
@Serializable
data class HaloStory(
    @SerialName("media_mxc")
    val mediaMxc: String,

    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @SerialName("story_type")
    val storyType: StoryType = StoryType.IMAGE,

    @SerialName("duration_ms")
    val durationMs: Long = 5000,

    @SerialName("caption")
    val caption: String? = null,

    @SerialName("thumbnail_mxc")
    val thumbnailMxc: String? = null,

    @SerialName("blurhash")
    val blurhash: String? = null
) {
    companion object {
        const val EVENT_TYPE = "com.halo.story"
        const val TTL_MS = 24 * 60 * 60 * 1000L // 24 hours

        fun isExpired(createdAt: Long): Boolean {
            return System.currentTimeMillis() - createdAt > TTL_MS
        }
    }
}

@Serializable
enum class StoryType {
    @SerialName("image")
    IMAGE,

    @SerialName("video")
    VIDEO
}
