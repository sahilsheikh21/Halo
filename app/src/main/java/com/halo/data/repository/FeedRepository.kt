package com.halo.data.repository

import com.halo.data.local.dao.PostDao
import com.halo.data.matrix.MatrixClientManager
import com.halo.domain.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
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
                    mediaUrls = parseMedia(entity.mediaJson),
                    locationName = parseLocation(entity.locationJson),
                    tags = parseTags(entity.tagsJson),
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
                    mediaUrls = parseMedia(entity.mediaJson),
                    locationName = parseLocation(entity.locationJson),
                    tags = parseTags(entity.tagsJson),
                    likeCount = entity.likeCount,
                    commentCount = entity.commentCount,
                    isLikedByMe = entity.isLikedByMe,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    private fun parseMedia(json: String): List<com.halo.domain.model.MediaItem> {
        return try {
            val list = kotlinx.serialization.json.Json.decodeFromString<List<com.halo.data.matrix.events.PostMedia>>(json)
            list.map {
                com.halo.domain.model.MediaItem(
                    url = it.mxcUri, // TODO: resolve mxc to http url
                    mxcUri = it.mxcUri,
                    mimeType = it.mimeType,
                    width = it.width,
                    height = it.height,
                    thumbnailUrl = it.thumbnailMxc,
                    blurhash = it.blurhash
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseLocation(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return try {
            val loc = kotlinx.serialization.json.Json.decodeFromString<com.halo.data.matrix.events.PostLocation>(json)
            loc.name
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTags(json: String): List<String> {
        return try {
            kotlinx.serialization.json.Json.decodeFromString<List<String>>(json)
        } catch (e: Exception) {
            emptyList()
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
        // TODO: Sync feed rooms via Sliding Sync, parse com.halo.post events
        
        // Temporarily seed MockData if DB is empty so UI works during transition
        val currentPosts = postDao.getFeedPosts(1, 0).first()
        if (currentPosts.isEmpty()) {
            val mockEntities = com.halo.data.mock.MockData.posts.map { post ->
                com.halo.data.local.entity.PostEntity(
                    eventId = post.eventId,
                    feedRoomId = "mock_feed_room",
                    authorId = post.authorId,
                    caption = post.caption,
                    mediaJson = kotlinx.serialization.json.Json.encodeToString(
                        post.mediaUrls.map {
                            com.halo.data.matrix.events.PostMedia(
                                mxcUri = it.mxcUri,
                                mimeType = it.mimeType,
                                width = it.width,
                                height = it.height,
                                thumbnailMxc = it.thumbnailUrl,
                                blurhash = it.blurhash
                            )
                        }
                    ),
                    locationJson = post.locationName?.let {
                        kotlinx.serialization.json.Json.encodeToString(
                            com.halo.data.matrix.events.PostLocation(name = it)
                        )
                    },
                    tagsJson = kotlinx.serialization.json.Json.encodeToString(post.tags),
                    likeCount = post.likeCount,
                    commentCount = post.commentCount,
                    isLikedByMe = post.isLikedByMe,
                    createdAt = post.createdAt,
                    cachedAt = System.currentTimeMillis()
                )
            }
            postDao.insertPosts(mockEntities)
        }
    }
}
