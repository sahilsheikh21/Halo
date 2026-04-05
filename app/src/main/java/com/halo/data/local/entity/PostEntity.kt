package com.halo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posts",
    indices = [
        Index(value = ["feed_room_id"]),
        Index(value = ["author_id"]),
        Index(value = ["created_at"])
    ]
)
data class PostEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,

    @ColumnInfo(name = "feed_room_id")
    val feedRoomId: String,

    @ColumnInfo(name = "author_id")
    val authorId: String,

    @ColumnInfo(name = "caption")
    val caption: String = "",

    @ColumnInfo(name = "media_json")
    val mediaJson: String = "[]", // Serialized List<PostMedia>

    @ColumnInfo(name = "location_json")
    val locationJson: String? = null, // Serialized PostLocation

    @ColumnInfo(name = "tags_json")
    val tagsJson: String = "[]", // Serialized List<String>

    @ColumnInfo(name = "like_count")
    val likeCount: Int = 0,

    @ColumnInfo(name = "comment_count")
    val commentCount: Int = 0,

    @ColumnInfo(name = "is_liked_by_me")
    val isLikedByMe: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)
