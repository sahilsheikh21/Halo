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
    private val chatRepository: ChatRepository,
    private val messageDao: MessageDao,
    private val postDao: PostDao,
    private val storyDao: StoryDao,
    private val userDao: UserDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val activeListeners = mutableSetOf<String>()

    /**
     * Start the sync event processing pipeline.
     */
    fun startProcessing(scope: CoroutineScope) {
        scope.launch(ioDispatcher) {
            slidingSyncManager.syncState.collect { state ->
                if (state is SyncState.Syncing) {
                    attachTimelineListeners(scope)
                }
            }
        }
    }

    private suspend fun attachTimelineListeners(scope: CoroutineScope) {
        val client = slidingSyncManager.getSyncService()?.roomListService()?.allRooms() ?: return
        // In a real app we'd observe the room list. For remediation, we'll pull once and attach.
        val entries = client.entries()
        // entries might be filtered/limited. 
        // Better to use client.rooms() if available on the main Client.
        val sdkClient = slidingSyncManager.matrixClientManager.getClient() ?: return
        sdkClient.rooms().forEach { room ->
            val roomId = room.id()
            if (activeListeners.contains(roomId)) return@forEach
            activeListeners.add(roomId)

            scope.launch(ioDispatcher) {
                try {
                    val timeline = room.timeline()
                    timeline.addListener(HaloTimelineListener(roomId, this@SyncEventProcessor, scope))
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
            val append = d.append() ?: return@forEach
            append.forEach { item ->
                val event = item.asEvent() ?: return@forEach
                val msg = event.content().asRoomMessage() ?: run {
                    // Check for Halo custom events
                    val type = event.eventType()
                    val sender = event.sender()
                    val eventId = event.eventId() ?: "remote_${System.currentTimeMillis()}"
                    val json = event.content().toString() // Raw JSON

                    scope.launch {
                        when (type) {
                            "com.halo.post" -> processor.processHaloPost(eventId, sender, roomId, json)
                            "com.halo.story" -> processor.processHaloStory(eventId, sender, roomId, json)
                        }
                    }
                    return@forEach
                }

                // Process standard message
                val body = msg.body()
                val senderId = event.sender()
                val eventId = event.eventId() ?: "remote_${System.currentTimeMillis()}"
                val timestamp = event.timestamp().toLong()

                scope.launch {
                    processor.processRoomMessage(roomId, eventId, senderId, body, timestamp)
                }
            }
        }
    }
}
