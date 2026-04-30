package com.halo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.halo.data.local.dao.ChatRoomDao
import com.halo.data.local.dao.ChatRoomMemberDao
import com.halo.data.local.dao.MessageDao
import com.halo.data.local.dao.PostDao
import com.halo.data.local.dao.ProcessedEventDao
import com.halo.data.local.dao.StoryDao
import com.halo.data.local.dao.UserDao
import com.halo.data.local.entity.ChatRoomEntity
import com.halo.data.local.entity.ChatRoomMemberEntity
import com.halo.data.local.entity.MessageEntity
import com.halo.data.local.entity.PostEntity
import com.halo.data.local.entity.ProcessedEventEntity
import com.halo.data.local.entity.StoryEntity
import com.halo.data.local.entity.UserEntity

@Database(
    entities = [
        PostEntity::class,
        StoryEntity::class,
        UserEntity::class,
        ChatRoomEntity::class,
        MessageEntity::class,
        ChatRoomMemberEntity::class,
        ProcessedEventEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class HaloDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun userDao(): UserDao
    abstract fun chatRoomDao(): ChatRoomDao
    abstract fun messageDao(): MessageDao
    abstract fun chatRoomMemberDao(): ChatRoomMemberDao
    abstract fun processedEventDao(): ProcessedEventDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_room_members` (
                        `room_id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        PRIMARY KEY(`room_id`, `user_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_room_members_user_id` ON `chat_room_members` (`user_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_room_members_room_id` ON `chat_room_members` (`room_id`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `processed_events` (
                        `event_key` TEXT NOT NULL,
                        `processed_at` INTEGER NOT NULL,
                        PRIMARY KEY(`event_key`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
