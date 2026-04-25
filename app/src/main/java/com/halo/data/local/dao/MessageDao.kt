package com.halo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halo.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE room_id = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoom(roomId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE room_id = :roomId")
    suspend fun deleteMessagesForRoom(roomId: String)

    // ─── B3: Delivery status ──────────────────────────────────────────────────

    /**
     * Update the delivery status of a single message.
     * Called after SDK send succeeds (→ SENT) or throws (→ FAILED).
     */
    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    // ─── B1: Deduplication — replace local echo with server ID ───────────────

    /**
     * Look for a locally-inserted copy of a message that we sent ourselves.
     * We match on [roomId], [senderId], and [body] within the past [sinceTimestamp]
     * milliseconds, and only consider rows whose id starts with "local_".
     *
     * If such a row exists the caller should delete it and re-insert using the
     * real Matrix event ID so the message doesn't appear twice in the UI.
     */
    @Query(
        """
        SELECT * FROM messages
        WHERE room_id    = :roomId
          AND sender_id  = :senderId
          AND body       = :body
          AND id LIKE 'local_%'
          AND timestamp  >= :sinceTimestamp
        LIMIT 1
        """
    )
    suspend fun findRecentLocalMessage(
        roomId: String,
        senderId: String,
        body: String,
        sinceTimestamp: Long
    ): MessageEntity?

    /**
     * Delete a single message by its primary key.
     * Used to remove the local-echo row before inserting the server-confirmed row.
     */
    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)
}
