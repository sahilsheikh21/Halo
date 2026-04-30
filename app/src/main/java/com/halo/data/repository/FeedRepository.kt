package com.halo.data.repository

import com.halo.data.local.dao.PostDao
import com.halo.data.matrix.MatrixClientManager
import com.halo.data.matrix.events.HaloPost
import com.halo.data.matrix.events.PostLocation
import com.halo.data.matrix.events.PostMedia
import com.halo.domain.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.matrix.rustcomponents.sdk.messageEventContentFromMarkdown
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val postDao: PostDao,
    private val matrixClientManager: MatrixClientManager
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    fun getFeedPosts(limit: Int = 20, offset: Int = 0): Flow<List<Post>> {
        return postDao.getFeedPosts(limit, offset).map { items ->
            items.map { item ->
                val entity = item.post
                val author = item.author
                Post(
                    eventId = entity.eventId,
                    authorId = entity.authorId,
                    authorName = author?.displayName ?: entity.authorId,
                    authorAvatarUrl = matrixClientManager.resolveMxc(author?.avatarMxc),
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
        return postDao.getPostsByAuthor(authorId).map { items ->
            items.map { item ->
                val entity = item.post
                val author = item.author
                Post(
                    eventId = entity.eventId,
                    authorId = entity.authorId,
                    authorName = author?.displayName ?: entity.authorId,
                    authorAvatarUrl = matrixClientManager.resolveMxc(author?.avatarMxc),
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
                    url = matrixClientManager.resolveMxc(it.mxcUri) ?: it.mxcUri,
                    mxcUri = it.mxcUri,
                    mimeType = it.mimeType,
                    width = it.width,
                    height = it.height,
                    thumbnailUrl = matrixClientManager.resolveMxc(it.thumbnailMxc),
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
        // Minimal safe refresh: prune old cache and rely on timeline processors to insert fresh events.
        val cacheWindowMs = 7L * 24 * 60 * 60 * 1000
        val threshold = System.currentTimeMillis() - cacheWindowMs
        postDao.deleteOldCache(threshold)
    }
    suspend fun publishPost(
        caption: String,
        mediaMxc: String,
        mimeType: String,
        location: String?
    ): Result<Unit> {
        val userId = matrixClientManager.getCurrentSession()?.userId ?: "@me:localhost"
        val now = System.currentTimeMillis()
        val haloPost = HaloPost(
            caption = caption,
            media = listOf(
                PostMedia(
                    mxcUri = mediaMxc,
                    mimeType = mimeType
                )
            ),
            location = location?.takeIf { it.isNotBlank() }?.let { PostLocation(name = it) },
            tags = emptyList(),
            createdAt = now
        )
        val client = matrixClientManager.getClient() ?: return Result.failure(Exception("Not authenticated"))
        val broadcastRoom = client.rooms().firstOrNull { room ->
            runCatching { !room.isDirect() && !room.isSpace() }.getOrDefault(false)
        } ?: return Result.failure(Exception("No feed-capable room available"))
        val typedJson = json.encodeToString(haloPost)
        val legacyPayload = "HALO_POST:$typedJson"

        runCatching {
            // Preferred path: typed custom event.
            broadcastRoom.sendRaw(HaloPost.EVENT_TYPE, typedJson)
            // Temporary compatibility path for legacy listeners.
            broadcastRoom.timeline().send(messageEventContentFromMarkdown(legacyPayload))
        }.getOrElse { return Result.failure(it) }

        val entity = com.halo.data.local.entity.PostEntity(
            eventId = "local_$now",
            feedRoomId = broadcastRoom.id(),
            authorId = userId,
            caption = caption,
            mediaJson = kotlinx.serialization.json.Json.encodeToString(
                listOf(
                    PostMedia(
                        mxcUri = mediaMxc,
                        mimeType = mimeType
                    )
                )
            ),
            locationJson = location?.takeIf { it.isNotBlank() }?.let {
                kotlinx.serialization.json.Json.encodeToString(
                    PostLocation(name = it)
                )
            },
            tagsJson = "[]",
            likeCount = 0,
            commentCount = 0,
            isLikedByMe = false,
            createdAt = now,
            cachedAt = now
        )
        postDao.insertPosts(listOf(entity))
        return Result.success(Unit)
    }
}
