package com.halo.data.repository

import com.halo.data.local.dao.StoryDao
import com.halo.data.matrix.MatrixClientManager
import com.halo.data.matrix.events.HaloStory
import com.halo.data.matrix.events.StoryType
import com.halo.domain.model.Story
import com.halo.domain.model.StoryGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.matrix.rustcomponents.sdk.messageEventContentFromMarkdown
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepository @Inject constructor(
    private val storyDao: StoryDao,
    private val userDao: com.halo.data.local.dao.UserDao,
    private val matrixClientManager: MatrixClientManager
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000) // 1 minute
        }
    }
    /**
     * Get active (non-expired) stories, grouped by author.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getActiveStoryGroups(): Flow<List<StoryGroup>> {
        return tickerFlow.flatMapLatest { now ->
            val cutoff = now - Story.TTL_MS
            storyDao.getActiveStories(cutoff).map { entities ->
                val authorIds = entities.map { it.authorId }.distinct()
                
                // Let's get the authors from DB
                val authors = authorIds.map { id ->
                    id to userDao.getUserById(id)
                }.toMap()

                entities
                    .groupBy { it.authorId }
                    .map { (authorId, stories) ->
                        val authorEntity = authors[authorId]
                        StoryGroup(
                            authorId = authorId,
                            authorName = authorEntity?.displayName?.takeIf { it.isNotBlank() } ?: authorId,
                            authorAvatarUrl = matrixClientManager.resolveMxc(authorEntity?.avatarMxc),
                            stories = stories.map { entity ->
                                Story(
                                    eventId = entity.eventId,
                                    authorId = entity.authorId,
                                    mediaUrl = matrixClientManager.resolveMxc(entity.mediaMxc) ?: entity.mediaMxc,
                                    mxcUri = entity.mediaMxc,
                                    storyType = entity.storyType,
                                    durationMs = entity.durationMs,
                                    caption = entity.caption,
                                    createdAt = entity.createdAt,
                                    isSeen = entity.isSeen
                                )
                            },
                            hasUnseenStories = stories.any { !it.isSeen }
                        )
                    }
            }
        }
    }

    suspend fun markStorySeen(eventId: String) {
        storyDao.markAsSeen(eventId)
    }

    suspend fun cleanExpiredStories() {
        val cutoff = System.currentTimeMillis() - Story.TTL_MS
        storyDao.deleteExpiredStories(cutoff)
    }

    suspend fun refreshStories() {
        cleanExpiredStories()
    }
    suspend fun publishStory(mediaMxc: String): Result<Unit> {
        val userId = matrixClientManager.getCurrentSession()?.userId ?: "@me:localhost"
        val now = System.currentTimeMillis()
        val haloStory = HaloStory(
            mediaMxc = mediaMxc,
            createdAt = now,
            storyType = StoryType.IMAGE
        )
        val typedJson = json.encodeToString(haloStory)
        val payload = "HALO_STORY:$typedJson"
        val client = matrixClientManager.getClient() ?: return Result.failure(Exception("Not authenticated"))
        val broadcastRoom = client.rooms().firstOrNull { room ->
            runCatching { !room.isDirect() && !room.isSpace() }.getOrDefault(false)
        } ?: return Result.failure(Exception("No story-capable room available"))

        runCatching {
            broadcastRoom.sendRaw(HaloStory.EVENT_TYPE, typedJson)
            broadcastRoom.timeline().send(messageEventContentFromMarkdown(payload))
        }.getOrElse { return Result.failure(it) }

        val entity = com.halo.data.local.entity.StoryEntity(
            eventId = "local_story_$now",
            feedRoomId = broadcastRoom.id(),
            authorId = userId,
            mediaMxc = mediaMxc,
            storyType = "image",
            durationMs = 5000,
            caption = null,
            createdAt = now,
            isSeen = true
        )
        storyDao.insertStories(listOf(entity))
        return Result.success(Unit)
    }
}
