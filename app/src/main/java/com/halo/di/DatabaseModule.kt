package com.halo.di

import android.content.Context
import androidx.room.Room
import com.halo.data.local.HaloDatabase
import com.halo.data.local.dao.PostDao
import com.halo.data.local.dao.StoryDao
import com.halo.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HaloDatabase {
        return Room.databaseBuilder(
            context,
            HaloDatabase::class.java,
            "halo_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePostDao(database: HaloDatabase): PostDao = database.postDao()

    @Provides
    fun provideStoryDao(database: HaloDatabase): StoryDao = database.storyDao()

    @Provides
    fun provideUserDao(database: HaloDatabase): UserDao = database.userDao()

    @Provides
    fun provideChatRoomDao(database: HaloDatabase): com.halo.data.local.dao.ChatRoomDao = database.chatRoomDao()
}
