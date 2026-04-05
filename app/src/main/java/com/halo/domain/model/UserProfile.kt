package com.halo.domain.model

/**
 * Domain model representing a user's public profile.
 */
data class UserProfile(
    val userId: String, // @username:server.com
    val displayName: String = "",
    val avatarUrl: String? = null,
    val bio: String = "",
    val links: List<String> = emptyList(),
    val feedRoomId: String? = null,
    val isFollowing: Boolean = false,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val postCount: Int = 0,
    val isCurrentUser: Boolean = false
) {
    val username: String
        get() = userId.substringAfter("@").substringBefore(":")

    val serverName: String
        get() = userId.substringAfter(":")
}
