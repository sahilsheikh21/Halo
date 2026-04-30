package com.halo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_events")
data class ProcessedEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_key")
    val eventKey: String,
    @ColumnInfo(name = "processed_at")
    val processedAt: Long = System.currentTimeMillis()
)
