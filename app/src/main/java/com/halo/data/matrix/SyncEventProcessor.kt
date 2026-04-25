package com.halo.data.matrix

import com.halo.data.local.dao.MessageDao
import com.halo.data.local.dao.PostDao
import com.halo.data.local.dao.StoryDao
import com.halo.data.local.dao.UserDao
import com.halo.data.local.entity.PostEntity
import com.halo.data.local.entity.StoryEntity
import com.halo.data.matrix.events.HaloPost
import com.halo.data.matrix.events.HaloStory
import com.halo.data.repository.ChatRepository
import org.matrix.rustcomponents.sdk.EventTimelineItem
import org.matrix.rustcomponents.sdk.EventOrTransactionId
import org.matrix.rustcomponents.sdk.TimelineListener
import org.matrix.rustcomponents.sdk.TimelineDiff
import org.matrix.rustcomponents.sdk.TimelineItem
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
    private val chatRepository: ChatRepository,
    private val messageDao: MessageDao,
    private val postDao: PostDao,
    private val storyDao: StoryDao,
    private val userDao: UserDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val activeListeners = java.util.concurrent.ConcurrentHashMap<String, org.matrix.rustcomponents.sdk.TaskHandle>()

    /**
     * Start the sync event processing pipeline.
     */
    fun startProcessing(scope: CoroutineScope) {
        scope.launch(ioDispatcher) {
            slidingSyncManager.syncState.collect { state ->
                if (state is SyncState.Syncing) {
                    attachTimelineListeners(scope)
                } else if (state is SyncState.Idle || state is SyncState.Error) {
                    clearListeners()
                }
            }
        }
    }

    private fun clearListeners() {
        activeListeners.values.forEach { it.cancel() }
        activeListeners.clear()
    }

    private suspend fun attachTimelineListeners(scope: CoroutineScope) {
        // Better to use rooms() on the main Client.
        val sdkClient = matrixClientManager.getClient() ?: return
        sdkClient.rooms().forEach { room ->
            val roomId = room.id()
            if (activeListeners.containsKey(roomId)) return@forEach

            scope.launch(ioDispatcher) {
                try {
                    val timeline = room.timeline()
                    val handle = timeline.addListener(HaloTimelineListener(roomId, this@SyncEventProcessor, scope))
                    activeListeners[roomId] = handle
                } catch (e: Exception) {
                    activeListeners.remove(roomId)
                }
            }
        }
    }

    /**
     * Process a standard Matrix m.room.message event.
     */
    suspend fun processRoomMessage(
        roomId: String,
        eventId: String,
        senderId: String,
        body: String,
        timestamp: Long
    ) {
        chatRepository.appendIncomingMessage(
            roomId = roomId,
            eventId = eventId,
            senderId = senderId,
            body = body,
            timestamp = timestamp
        )
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

private class HaloTimelineListener(
    private val roomId: String,
    private val processor: SyncEventProcessor,
    private val scope: CoroutineScope
) : org.matrix.rustcomponents.sdk.TimelineListener {
    override fun onUpdate(diff: List<org.matrix.rustcomponents.sdk.TimelineDiff>) {
        diff.forEach { d ->
            val items = when (d) {
                is org.matrix.rustcomponents.sdk.TimelineDiff.Append -> d.values
                is org.matrix.rustcomponents.sdk.TimelineDiff.PushBack -> listOf(d.value)
                is org.matrix.rustcomponents.sdk.TimelineDiff.PushFront -> listOf(d.value)
                is org.matrix.rustcomponents.sdk.TimelineDiff.Insert -> listOf(d.value)
                is org.matrix.rustcomponents.sdk.TimelineDiff.Set -> listOf(d.value)
                else -> emptyList()
            }

            items.forEach { item ->
                val event = item.asEvent() ?: return@forEach
                val eventId = item.uniqueId().id
                val senderId = event.sender
                val timestamp = System.currentTimeMillis() // Fallback until timestamp mapping is fixed

                val content = event.content
                when (content) {
                    is org.matrix.rustcomponents.sdk.TimelineItemContent.MsgLike -> {
                        val kind = content.content.kind
                        if (kind is org.matrix.rustcomponents.sdk.MsgLikeKind.Message) {
                            val body = kind.content.body
                            scope.launch {
                                processor.processRoomMessage(roomId, eventId, senderId, body, timestamp)
                            }
                        }
                    }
                    else -> {
                        // Handle other types or custom Halo events via raw string if needed
                        val json = content.toString()
                        scope.launch {
                            processor.processHaloPost(eventId, senderId, roomId, json)
                        }
                    }
                }
            }
        }
    }
}
