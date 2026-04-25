package com.halo.data.matrix

import android.util.Log
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncEventProcessor"

/**
 * How long (ms) to wait before re-attempting timeline listener attachment.
 *
 * B11: On first login the SDK may return an empty room list even after the sync
 * state transitions to Syncing. A single delayed retry covers the common case
 * where rooms are available a few seconds later.
 */
private const val LISTENER_RETRY_DELAY_MS = 3_000L

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

    /** One TaskHandle per room — lets us cancel individual listeners cleanly. */
    private val activeListeners =
        ConcurrentHashMap<String, org.matrix.rustcomponents.sdk.TaskHandle>()

    /**
     * B2: Tracks every Matrix event ID we have already processed.
     *
     * The SDK replays historical timeline items on every sync restart (app relaunch,
     * network reconnect). Without this guard those items would be re-inserted into
     * the DB on each restart, causing flicker and potential duplicates.
     *
     * The set lives in-memory for the lifetime of the singleton; it is never
     * persisted to disk (Room's OnConflictStrategy.REPLACE is the safety net for
     * anything that slips through after a process kill).
     */
    private val seenEventIds: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap())

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun startProcessing(scope: CoroutineScope) {
        scope.launch(ioDispatcher) {
            slidingSyncManager.syncState.collect { state ->
                when (state) {
                    is SyncState.Syncing -> {
                        attachTimelineListeners(scope)

                        // B11: rooms() may return an empty list on the very first sync
                        // because the SDK hasn't finished populating its room store yet.
                        // Scheduling a second pass a few seconds later catches all rooms
                        // that were missed in the first pass without re-adding duplicates
                        // (attachTimelineListeners is idempotent for rooms already tracked).
                        scope.launch(ioDispatcher) {
                            delay(LISTENER_RETRY_DELAY_MS)
                            attachTimelineListeners(scope)
                        }
                    }
                    is SyncState.Idle, is SyncState.Error -> clearListeners()
                    else -> Unit
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun clearListeners() {
        activeListeners.values.forEach { it.cancel() }
        activeListeners.clear()
    }

    /**
     * Attaches a [HaloTimelineListener] to every room that doesn't already have one.
     * Safe to call multiple times — existing listeners are never replaced.
     */
    private suspend fun attachTimelineListeners(scope: CoroutineScope) {
        val sdkClient = matrixClientManager.getClient() ?: return
        val rooms = sdkClient.rooms()

        if (rooms.isEmpty()) {
            Log.d(TAG, "attachTimelineListeners: room list is empty — will retry")
            return
        }

        rooms.forEach { room ->
            val roomId = room.id()
            if (activeListeners.containsKey(roomId)) return@forEach

            scope.launch(ioDispatcher) {
                try {
                    val timeline = room.timeline()
                    val handle = timeline.addListener(
                        HaloTimelineListener(roomId, this@SyncEventProcessor, scope)
                    )
                    activeListeners[roomId] = handle
                    Log.d(TAG, "Attached timeline listener for room $roomId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to attach listener for room $roomId", e)
                    activeListeners.remove(roomId)
                }
            }
        }
    }

    /**
     * B2: Records an event ID as processed.
     * @return `true` if the event is new and should be processed,
     *         `false` if it was already seen and must be skipped.
     */
    fun markEventSeen(eventId: String): Boolean = seenEventIds.add(eventId)

    // ─────────────────────────────────────────────────────────────────────────
    // Event processors (called from HaloTimelineListener)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Persists a plain chat message received from the timeline.
     *
     * Delegates to [ChatRepository.appendIncomingMessage] which handles B1
     * (own-message deduplication) internally.
     */
    suspend fun processRoomMessage(
        roomId: String,
        eventId: String,
        senderId: String,
        body: String,
        timestamp: Long
    ) {
        chatRepository.appendIncomingMessage(roomId, eventId, senderId, body, timestamp)
    }

    suspend fun processHaloPost(
        eventId: String,
        sender: String,
        roomId: String,
        eventJson: String
    ) {
        try {
            val haloPost   = json.decodeFromString<HaloPost>(eventJson)
            val mediaJson  = json.encodeToString(haloPost.media)
            val locationJson = haloPost.location?.let { json.encodeToString(it) }
            val tagsJson   = json.encodeToString(haloPost.tags)
            val entity = PostEntity(
                eventId      = eventId,
                feedRoomId   = roomId,
                authorId     = sender,
                caption      = haloPost.caption,
                mediaJson    = mediaJson,
                locationJson = locationJson,
                tagsJson     = tagsJson,
                likeCount    = 0,
                commentCount = 0,
                isLikedByMe  = false,
                createdAt    = haloPost.createdAt,
                cachedAt     = System.currentTimeMillis()
            )
            postDao.insertPosts(listOf(entity))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode HaloPost (eventId=$eventId)", e)
        }
    }

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
                eventId   = eventId,
                feedRoomId = roomId,
                authorId  = sender,
                mediaMxc  = haloStory.mediaMxc,
                storyType = haloStory.storyType.name.lowercase(),
                durationMs = haloStory.durationMs,
                caption   = haloStory.caption,
                createdAt = haloStory.createdAt,
                isSeen    = false
            )
            storyDao.insertStories(listOf(entity))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode HaloStory (eventId=$eventId)", e)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Timeline listener
// ─────────────────────────────────────────────────────────────────────────────

private class HaloTimelineListener(
    private val roomId: String,
    private val processor: SyncEventProcessor,
    private val scope: CoroutineScope
) : org.matrix.rustcomponents.sdk.TimelineListener {

    override fun onUpdate(diff: List<org.matrix.rustcomponents.sdk.TimelineDiff>) {
        diff.forEach { d ->
            val items = when (d) {
                is org.matrix.rustcomponents.sdk.TimelineDiff.Append    -> d.values
                is org.matrix.rustcomponents.sdk.TimelineDiff.PushBack  -> listOf(d.value)
                is org.matrix.rustcomponents.sdk.TimelineDiff.PushFront -> listOf(d.value)
                is org.matrix.rustcomponents.sdk.TimelineDiff.Insert    -> listOf(d.value)
                is org.matrix.rustcomponents.sdk.TimelineDiff.Set       -> listOf(d.value)
                else -> emptyList()
            }

            items.forEach { item ->
                val event    = item.asEvent() ?: return@forEach

                // B2: Use the SDK's internal unique ID as the stable event identifier.
                // EventTimelineItem does not expose the raw Matrix event ID directly in
                // SDK v26.03.31; item.uniqueId().id is the closest stable handle we have
                // within a single sync session. The in-memory seenEventIds set below
                // prevents replay duplicates within a process lifetime; Room's
                // OnConflictStrategy.REPLACE is the safety net across process restarts.
                val eventId  = item.uniqueId().id

                // B2: Skip events we have already written to the DB.
                // This prevents duplicate rows on sync restarts / timeline replays.
                if (!processor.markEventSeen(eventId)) return@forEach

                val senderId = event.sender

                // B4: Use the origin server timestamp so that historical messages
                // sort correctly and time labels ("3:42 PM") are accurate.
                // Fall back to device clock only if the SDK returns zero or null.
                val timestamp = event.timestamp.toLong().takeIf { it > 0L }
                    ?: System.currentTimeMillis()

                when (val content = event.content) {
                    is org.matrix.rustcomponents.sdk.TimelineItemContent.MsgLike -> {
                        val kind = content.content.kind
                        if (kind is org.matrix.rustcomponents.sdk.MsgLikeKind.Message) {
                            scope.launch {
                                processor.processRoomMessage(
                                    roomId    = roomId,
                                    eventId   = eventId,
                                    senderId  = senderId,
                                    body      = kind.content.body,
                                    timestamp = timestamp
                                )
                            }
                        }
                    }
                    else -> {
                        // B9: content.toString() produces a Kotlin debug string like
                        // "MembershipChange(change=JOINED)" — it does NOT contain the
                        // Matrix event type string "com.halo.story" / "com.halo.post".
                        //
                        // Custom Halo events (posts, stories) cannot be reliably routed
                        // through the standard timeline listener in SDK v26.03.31 because
                        // the raw event type is not exposed on non-message timeline items.
                        //
                        // TODO: Migrate to the Room state API or a raw event endpoint for
                        //       custom com.halo.* events in a future SDK upgrade.
                        //
                        // For now we log the content class name so we can observe what
                        // comes through during testing instead of silently discarding it.
                        Log.d(
                            "SyncEventProcessor",
                            "Unhandled timeline content type in room $roomId: " +
                                "${content::class.simpleName} (eventId=$eventId)"
                        )
                    }
                }
            }
        }
    }
}
