package com.halo.domain.model

/**
 * Domain model for a single chat message.
 *
 * [senderId] is the Matrix user ID of the message author (e.g. "@alice:matrix.org").
 * It is preserved through the data pipeline so the UI can display sender names in
 * group chats — not just the binary "is this me?" flag.
 */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val body: String,
    val isMe: Boolean,
    val timestamp: Long,
    /** SENDING | SENT | FAILED */
    val status: String = MessageStatus.SENT
)

object MessageStatus {
    const val SENDING = "SENDING"
    const val SENT    = "SENT"
    const val FAILED  = "FAILED"
}
