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
    }
    suspend fun publishPost(
        caption: String,
        mediaMxc: String,
        mimeType: String,
        location: String?
    ) {
        // TODO: Send com.halo.post event via Matrix SDK

        val entity = com.halo.data.local.entity.PostEntity(
            eventId = "local_${System.currentTimeMillis()}",
            feedRoomId = "local_feed",
            authorId = "@me:localhost", // Placeholder until auth is wired
            caption = caption,
            mediaJson = kotlinx.serialization.json.Json.encodeToString(
                listOf(
                    com.halo.data.matrix.events.PostMedia(
                        mxcUri = mediaMxc,
                        mimeType = mimeType
                    )
                )
            ),
            locationJson = location?.takeIf { it.isNotBlank() }?.let {
                kotlinx.serialization.json.Json.encodeToString(
                    com.halo.data.matrix.events.PostLocation(name = it)
                )
            },
            tagsJson = "[]",
            likeCount = 0,
            commentCount = 0,
            isLikedByMe = false,
            createdAt = System.currentTimeMillis(),
            cachedAt = System.currentTimeMillis()
        )
        postDao.insertPosts(listOf(entity))
    }
}
