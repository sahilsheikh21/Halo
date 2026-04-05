package com.halo.data.matrix.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Custom Matrix timeline event: com.halo.comment
 *
 * Sent as a timeline event in a Feed Room to comment on a post.
 * Uses m.relates_to for linking to the parent post event.
 */
@Serializable
data class HaloComment(
    @SerialName("relates_to")
    val relatesTo: EventRelation,

    @SerialName("body")
    val body: String,

    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val EVENT_TYPE = "com.halo.comment"
    }
}
