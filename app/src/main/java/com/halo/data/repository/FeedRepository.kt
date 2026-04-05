package com.halo.data.repository

import com.halo.data.local.dao.PostDao
import com.halo.data.matrix.MatrixClientManager
import com.halo.domain.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val postDao: PostDao,
    private val matrixClientManager: MatrixClientManager
) {
    fun getFeedPosts(limit: Int = 20, offset: Int = 0): Flow<List<Post>> {
        return postDao.getFeedPosts(limit, offset).map { entities ->
            entities.map { entity ->
                Post(
                    eventId = entity.eventId,
                    authorId = entity.authorId,
                    caption = entity.caption,
                    likeCount = entity.likeCount,
                    commentCount = entity.commentCount,
                    isLikedByMe = entity.isLikedByMe,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    fun getPostsByAuthor(authorId: String): Flow<List<Post>> {
        return postDao.getPostsByAuthor(authorId).map { entities ->
            entities.map { entity ->
                Post(
                    eventId = entity.eventId,
                    authorId = entity.authorId,
                    caption = entity.caption,
                    likeCount = entity.likeCount,
                    commentCount = entity.commentCount,
                    isLikedByMe = entity.isLikedByMe,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    suspend fun likePost(eventId: String) {
        // TODO: Send com.halo.reaction event via Matrix SDK
        val post = postDao.getPostById(eventId) ?: return
        postDao.updateLikeState(eventId, post.likeCount + 1, true)
    }

    suspend fun unlikePost(eventId: String) {
        // TODO: Redact com.halo.reaction event via Matrix SDK
        val post = postDao.getPostById(eventId) ?: return
        postDao.updateLikeState(eventId, maxOf(0, post.likeCount - 1), false)
    }

    suspend fun refreshFeed() {
        // TODO: Sync feed rooms via Sliding Sync, parse com.halo.post events, cache locally
    }
}
