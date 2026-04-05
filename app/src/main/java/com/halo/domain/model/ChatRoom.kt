package com.halo.domain.model

/**
 * Domain model representing a chat/DM room.
 */
data class ChatRoom(
    val roomId: String,
    val name: String = "",
    val avatarUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageAt: Long = 0,
    val unreadCount: Int = 0,
    val isDm: Boolean = true,
    val members: List<String> = emptyList()
) {
    /** Backwards-compat alias */
    val lastMessageTimestamp: Long get() = lastMessageAt
    val isDirect: Boolean get() = isDm
}
