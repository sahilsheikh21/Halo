package com.halo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.halo.ui.screens.chat.ChatMessage

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["room_id"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "room_id")
    val roomId: String,

    @ColumnInfo(name = "sender_id")
    val senderId: String,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "is_me")
    val isMe: Boolean,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)

fun MessageEntity.toDomain() = ChatMessage(
    id = id,
    body = body,
    isMe = isMe,
    timestamp = timestamp
)
