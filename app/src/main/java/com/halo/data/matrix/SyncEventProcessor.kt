package com.halo.data.matrix

import com.halo.data.local.dao.PostDao
import com.halo.data.local.dao.StoryDao
import com.halo.data.local.dao.UserDao
import com.halo.data.local.entity.PostEntity
import com.halo.data.local.entity.StoryEntity
import com.halo.data.matrix.events.HaloPost
import com.halo.data.matrix.events.HaloStory
import com.halo.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens to the Matrix SyncService and processes incoming timeline events,
 * writing them to the local Room database so all UI flows update reactively.
 *
 * Events handled:
 *  - com.halo.post  → PostEntity  → feed & profile grids
 *  - com.halo.story → StoryEntity → story bar
 *
 * Implementation note: The Matrix Rust SDK exposes timeline events via callback
 * listeners on the SyncService. The actual event parsing is SDK-version-specific
 * and is handled via the TimelineListener interface.
 *
 * This class is structured as the integration point — the SDK callback registration
 * is performed in [startProcessing] and event dispatch routes to [processHaloPost]
 * and [processHaloStory].
 */
@Singleton
class SyncEventProcessor @Inject constructor(
    private val slidingSyncManager: SlidingSyncManager,
    private val matrixClientManager: MatrixClientManager,
    private val postDao: PostDao,
    private val storyDao: StoryDao,
    private val userDao: UserDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Start the sync event processing pipeline.
     * Observes [SlidingSyncManager.syncState] and starts room subscriptions
     * when the sync service transitions to Syncing state.
     */
    fun startProcessing(scope: CoroutineScope) {
        scope.launch(ioDispatcher) {
            slidingSyncManager.syncState.collect { state ->
                if (state is SyncState.Syncing) {
                    // SDK timeline listeners would be registered here once
                    // the RoomListService API surface is confirmed for this version.
                    // For now, Room DB is populated via:
                    //  1. Mock seeding (refreshFeed / refreshStories)
                    //  2. Direct writes from CreateViewModel (publishPost / publishStory)
                    // TODO: Register timeline listeners via SlidingSyncManager.getSyncService()
                }
            }
        }
    }

    /**
     * Process a raw com.halo.post event JSON string received from the Matrix timeline.
     * Call this from a timeline listener callback.
     */
    suspend fun processHaloPost(
        eventId: String,
        sender: String,
        roomId: String,
        eventJson: String
    ) {
        try {
            val haloPost = json.decodeFromString<HaloPost>(eventJson)
            val mediaJson = json.encodeToString(haloPost.media)
            val locationJson = haloPost.location?.let { json.encodeToString(it) }
            val tagsJson = json.encodeToString(haloPost.tags)
            val entity = PostEntity(
                eventId = eventId,
                feedRoomId = roomId,
                authorId = sender,
                caption = haloPost.caption,
                mediaJson = mediaJson,
                locationJson = locationJson,
                tagsJson = tagsJson,
                likeCount = 0,
                commentCount = 0,
                isLikedByMe = false,
                createdAt = haloPost.createdAt,
                cachedAt = System.currentTimeMillis()
            )
            postDao.insertPosts(listOf(entity))
        } catch (e: Exception) {
            // Malformed event — skip
        }
    }

    /**
     * Process a raw com.halo.story event JSON string received from the Matrix timeline.
     * Call this from a timeline listener callback.
     */
    suspend fun processHaloStory(
        eventId: String,
        sender: String,
        roomId: String,
        eventJson: String
    ) {
        try {
            val haloStory = json.decodeFromString<HaloStory>(eventJson)
            if (HaloStory.isExpired(haloStory.createdAt)) return

            val entity = StoryEntity(
                eventId = eventId,
                feedRoomId = roomId,
                authorId = sender,
                mediaMxc = haloStory.mediaMxc,
                storyType = haloStory.storyType.name.lowercase(),
                durationMs = haloStory.durationMs,
                caption = haloStory.caption,
                createdAt = haloStory.createdAt,
                isSeen = false
            )
            storyDao.insertStories(listOf(entity))
        } catch (e: Exception) {
            // Malformed event — skip
        }
    }
}
