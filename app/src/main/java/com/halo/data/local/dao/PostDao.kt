package com.halo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.halo.data.local.entity.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Query("SELECT * FROM posts WHERE feed_room_id = :roomId ORDER BY created_at DESC")
    fun getPostsByRoom(roomId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun getFeedPosts(limit: Int = 20, offset: Int = 0): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE author_id = :authorId ORDER BY created_at DESC")
    fun getPostsByAuthor(authorId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE event_id = :eventId")
    suspend fun getPostById(eventId: String): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("UPDATE posts SET like_count = :likeCount, is_liked_by_me = :isLiked WHERE event_id = :eventId")
    suspend fun updateLikeState(eventId: String, likeCount: Int, isLiked: Boolean)

    @Query("UPDATE posts SET comment_count = :commentCount WHERE event_id = :eventId")
    suspend fun updateCommentCount(eventId: String, commentCount: Int)

    @Query("DELETE FROM posts WHERE cached_at < :threshold")
    suspend fun deleteOldCache(threshold: Long)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()
}
