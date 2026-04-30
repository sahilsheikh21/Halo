package com.halo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halo.data.local.entity.ChatRoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatRoomDao {
    @Query("SELECT * FROM chat_rooms ORDER BY lastMessageAt DESC")
    fun getAllChatRooms(): Flow<List<ChatRoomEntity>>

    @Query("SELECT * FROM chat_rooms WHERE roomId = :roomId LIMIT 1")
    suspend fun getChatRoom(roomId: String): ChatRoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRoom(chatRoom: ChatRoomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRooms(chatRooms: List<ChatRoomEntity>)

    /**
     * Inserts rooms without overwriting existing records.
     * Used by [refreshChatRooms] so that lastMessage / unreadCount
     * set by incoming sync events are never erased.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChatRoomsIgnore(chatRooms: List<ChatRoomEntity>)

    @Query("DELETE FROM chat_rooms WHERE roomId = :roomId")
    suspend fun deleteChatRoom(roomId: String)

    /**
     * Find an existing DM room with a specific user.
     * Checks the membersJoined column for a match.
     */
    @Query("SELECT * FROM chat_rooms WHERE isDm = 1 AND membersJoined LIKE '%' || :userId || '%' LIMIT 1")
    suspend fun findDmWithUser(userId: String): ChatRoomEntity?

    @Query("SELECT * FROM chat_rooms WHERE roomId = :roomId LIMIT 1")
    suspend fun findByRoomId(roomId: String): ChatRoomEntity?

    /**
     * Non-flow snapshot of all chat rooms — used for one-shot lookups.
     */
    @Query("SELECT * FROM chat_rooms ORDER BY lastMessageAt DESC")
    suspend fun getAllChatRoomsOnce(): List<ChatRoomEntity>

    @Query("DELETE FROM chat_rooms")
    suspend fun deleteAll()
}
