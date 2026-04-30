package com.halo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halo.data.local.entity.ProcessedEventEntity

@Dao
interface ProcessedEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(event: ProcessedEventEntity): Long

    @Query("DELETE FROM processed_events WHERE processed_at < :cutoff")
    suspend fun pruneOlderThan(cutoff: Long)
}
