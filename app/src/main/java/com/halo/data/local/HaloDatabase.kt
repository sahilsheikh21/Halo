package com.halo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.halo.data.local.dao.ChatRoomDao
import com.halo.data.local.dao.MessageDao
import com.halo.data.local.dao.PostDao
import com.halo.data.local.dao.StoryDao
import com.halo.data.local.dao.UserDao
import com.halo.data.local.entity.ChatRoomEntity
import com.halo.data.local.entity.MessageEntity
import com.halo.data.local.entity.PostEntity
import com.halo.data.local.entity.StoryEntity
import com.halo.data.local.entity.UserEntity

@Database(
    entities = [
        PostEntity::class,
        StoryEntity::class,
        UserEntity::class,
        ChatRoomEntity::class,
        MessageEntity::class
    ],
    version = 4, // bumped from 3: MessageEntity gained a "status" column (B3)
    exportSchema = false
)
abstract class HaloDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun userDao(): UserDao
    abstract fun chatRoomDao(): ChatRoomDao
    abstract fun messageDao(): MessageDao
}
