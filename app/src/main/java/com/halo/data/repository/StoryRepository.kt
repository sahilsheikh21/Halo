package com.halo.data.repository

import com.halo.data.local.dao.StoryDao
import com.halo.data.matrix.MatrixClientManager
import com.halo.domain.model.Story
import com.halo.domain.model.StoryGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepository @Inject constructor(
    private val storyDao: StoryDao,
    private val matrixClientManager: MatrixClientManager
) {
    /**
     * Get active (non-expired) stories, grouped by author.
     */
    fun getActiveStoryGroups(): Flow<List<StoryGroup>> {
        val cutoff = System.currentTimeMillis() - Story.TTL_MS
        return storyDao.getActiveStories(cutoff).map { entities ->
            entities
                .groupBy { it.authorId }
                .map { (authorId, stories) ->
                    StoryGroup(
                        authorId = authorId,
                        authorName = stories.first().authorId, // TODO: resolve display name
                        authorAvatarUrl = null, // TODO: resolve avatar
                        stories = stories.map { entity ->
                            Story(
                                eventId = entity.eventId,
                                authorId = entity.authorId,
                                mediaUrl = entity.mediaMxc, // TODO: resolve mxc to http
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

    suspend fun markStorySeen(eventId: String) {
        storyDao.markAsSeen(eventId)
    }

    suspend fun cleanExpiredStories() {
        val cutoff = System.currentTimeMillis() - Story.TTL_MS
        storyDao.deleteExpiredStories(cutoff)
    }

    suspend fun refreshStories() {
        // TODO: Sync com.halo.story state events from followed rooms
    }
    suspend fun publishStory(mediaMxc: String) {
        // TODO: Send com.halo.story state event via Matrix SDK
        
        val entity = com.halo.data.local.entity.StoryEntity(
            eventId = "local_story_${System.currentTimeMillis()}",
            feedRoomId = "local_feed",
            authorId = "@me:localhost", // Placeholder
            mediaMxc = mediaMxc,
            storyType = "image",
            durationMs = 5000,
            caption = null,
            createdAt = System.currentTimeMillis(),
            isSeen = true
        )
        storyDao.insertStories(listOf(entity))
    }
}
