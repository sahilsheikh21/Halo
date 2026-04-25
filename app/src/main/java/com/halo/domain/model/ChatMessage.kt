package com.halo.domain.model

data class ChatMessage(
    val id: String,
    val body: String,
    val isMe: Boolean,
    val timestamp: Long
)
