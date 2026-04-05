package com.halo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stories",
    indices = [
        Index(value = ["author_id"]),
        Index(value = ["created_at"])
    ]
)
data class StoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,

    @ColumnInfo(name = "feed_room_id")
    val feedRoomId: String,

    @ColumnInfo(name = "author_id")
    val authorId: String,

    @ColumnInfo(name = "media_mxc")
    val mediaMxc: String,

    @ColumnInfo(name = "story_type")
    val storyType: String = "image", // "image" | "video"

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 5000,

    @ColumnInfo(name = "caption")
    val caption: String? = null,

    @ColumnInfo(name = "thumbnail_mxc")
    val thumbnailMxc: String? = null,

    @ColumnInfo(name = "blurhash")
    val blurhash: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_seen")
    val isSeen: Boolean = false,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)
