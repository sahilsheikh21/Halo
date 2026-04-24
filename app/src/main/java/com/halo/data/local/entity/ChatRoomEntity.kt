package com.halo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.halo.domain.model.ChatRoom

@Entity(tableName = "chat_rooms")
data class ChatRoomEntity(
    @PrimaryKey val roomId: String,
    val name: String,
    val avatarUrl: String?,
    val lastMessage: String?,
    val lastMessageAt: Long,
    val unreadCount: Int,
    val isDm: Boolean,
    val membersJoined: String // Stored as comma-separated string for simplicity
)

fun ChatRoomEntity.toDomain() = ChatRoom(
    roomId = roomId,
    name = name,
    avatarUrl = avatarUrl,
    lastMessage = lastMessage,
    lastMessageAt = lastMessageAt,
    unreadCount = unreadCount,
    isDm = isDm,
    members = if (membersJoined.isEmpty()) emptyList() else membersJoined.split(",")
)

fun ChatRoom.toEntity() = ChatRoomEntity(
    roomId = roomId,
    name = name,
    avatarUrl = avatarUrl,
    lastMessage = lastMessage,
    lastMessageAt = lastMessageAt,
    unreadCount = unreadCount,
    isDm = isDm,
    membersJoined = members.joinToString(",")
)
