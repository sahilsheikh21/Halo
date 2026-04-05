package com.halo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.halo.data.local.dao.PostDao
import com.halo.data.local.dao.StoryDao
import com.halo.data.local.dao.UserDao
import com.halo.data.local.entity.PostEntity
import com.halo.data.local.entity.StoryEntity
import com.halo.data.local.entity.UserEntity

@Database(
    entities = [
        PostEntity::class,
        StoryEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HaloDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun userDao(): UserDao
}
