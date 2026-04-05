package com.halo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halo.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {

    @Query("SELECT * FROM stories WHERE created_at > :cutoffTime ORDER BY created_at DESC")
    fun getActiveStories(cutoffTime: Long): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE author_id = :authorId AND created_at > :cutoffTime ORDER BY created_at ASC")
    fun getStoriesByAuthor(authorId: String, cutoffTime: Long): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Query("UPDATE stories SET is_seen = 1 WHERE event_id = :eventId")
    suspend fun markAsSeen(eventId: String)

    @Query("DELETE FROM stories WHERE created_at < :cutoffTime")
    suspend fun deleteExpiredStories(cutoffTime: Long)

    @Query("DELETE FROM stories")
    suspend fun deleteAll()
}
