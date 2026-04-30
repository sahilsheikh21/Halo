package com.halo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halo.data.local.entity.ChatRoomEntity
import com.halo.data.local.entity.ChatRoomMemberEntity

@Dao
interface ChatRoomMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<ChatRoomMemberEntity>)

    @Query("DELETE FROM chat_room_members WHERE room_id = :roomId")
    suspend fun deleteMembersForRoom(roomId: String)

    @Query(
        """
        SELECT cr.* FROM chat_rooms cr
        INNER JOIN chat_room_members crm ON crm.room_id = cr.roomId
        WHERE cr.isDm = 1 AND crm.user_id = :userId
        LIMIT 1
        """
    )
    suspend fun findDmRoomByMember(userId: String): ChatRoomEntity?
}
