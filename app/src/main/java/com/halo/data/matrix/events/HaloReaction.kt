package com.halo.data.matrix.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Custom Matrix timeline event: com.halo.reaction
 *
 * Sent as a timeline event in a Feed Room to react to a post.
 * Uses m.relates_to pattern for event relationship.
 */
@Serializable
data class HaloReaction(
    @SerialName("relates_to")
    val relatesTo: EventRelation,

    @SerialName("reaction")
    val reaction: String = "❤️" // Default: heart
) {
    companion object {
        const val EVENT_TYPE = "com.halo.reaction"
    }
}

@Serializable
data class EventRelation(
    @SerialName("event_id")
    val eventId: String,

    @SerialName("rel_type")
    val relationType: String = "m.annotation"
)
