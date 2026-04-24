package com.halo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.halo.data.local.dao.ChatRoomDao
import com.halo.data.local.dao.PostDao
import com.halo.data.local.dao.StoryDao
import com.halo.data.local.dao.UserDao
import com.halo.data.local.entity.ChatRoomEntity
import com.halo.data.local.entity.PostEntity
import com.halo.data.local.entity.StoryEntity
import com.halo.data.local.entity.UserEntity

@Database(
    entities = [
        PostEntity::class,
        StoryEntity::class,
        UserEntity::class,
        ChatRoomEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HaloDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun userDao(): UserDao
    abstract fun chatRoomDao(): ChatRoomDao
}
