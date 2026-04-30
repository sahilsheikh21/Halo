package com.halo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "chat_room_members",
    primaryKeys = ["room_id", "user_id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["room_id"])
    ]
)
data class ChatRoomMemberEntity(
    @ColumnInfo(name = "room_id")
    val roomId: String,
    @ColumnInfo(name = "user_id")
    val userId: String
)
