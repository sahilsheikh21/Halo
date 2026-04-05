package com.halo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String, // @username:server.com

    @ColumnInfo(name = "display_name")
    val displayName: String = "",

    @ColumnInfo(name = "avatar_mxc")
    val avatarMxc: String? = null,

    @ColumnInfo(name = "bio")
    val bio: String = "",

    @ColumnInfo(name = "feed_room_id")
    val feedRoomId: String? = null,

    @ColumnInfo(name = "is_following")
    val isFollowing: Boolean = false,

    @ColumnInfo(name = "follower_count")
    val followerCount: Int = 0,

    @ColumnInfo(name = "following_count")
    val followingCount: Int = 0,

    @ColumnInfo(name = "post_count")
    val postCount: Int = 0,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)
