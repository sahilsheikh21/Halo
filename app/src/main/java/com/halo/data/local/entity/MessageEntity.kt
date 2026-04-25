package com.halo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.halo.domain.model.ChatMessage
import com.halo.domain.model.MessageStatus

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
    val timestamp: Long,

    /**
     * Delivery status for outbound messages.
     * Inbound messages (received via sync) are always stored as SENT.
     * Values: [MessageStatus.SENDING], [MessageStatus.SENT], [MessageStatus.FAILED]
     */
    @ColumnInfo(name = "status", defaultValue = MessageStatus.SENT)
    val status: String = MessageStatus.SENT
)

fun MessageEntity.toDomain() = ChatMessage(
    id        = id,
    senderId  = senderId,
    body      = body,
    isMe      = isMe,
    timestamp = timestamp,
    status    = status
)
