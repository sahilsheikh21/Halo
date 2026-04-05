package com.halo.data.matrix.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Custom Matrix state event: com.halo.profile
 *
 * Sent as a state event (state_key: "") in the user's Feed Room.
 * Represents the user's public profile information.
 */
@Serializable
data class HaloProfile(
    @SerialName("display_name")
    val displayName: String,

    @SerialName("avatar_mxc")
    val avatarMxc: String? = null,

    @SerialName("bio")
    val bio: String = "",

    @SerialName("links")
    val links: List<String> = emptyList(),

    @SerialName("pronouns")
    val pronouns: String? = null,

    @SerialName("verified")
    val isVerified: Boolean = false
) {
    companion object {
        const val EVENT_TYPE = "com.halo.profile"
        const val STATE_KEY = "" // Empty state key = canonical profile
    }
}
