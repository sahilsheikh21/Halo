package com.halo.data.matrix

import android.util.Log
import com.halo.data.local.dao.ProcessedEventDao
import com.halo.data.local.dao.MessageDao
import com.halo.data.local.dao.PostDao
import com.halo.data.local.dao.StoryDao
import com.halo.data.local.dao.UserDao
import com.halo.data.local.entity.PostEntity
import com.halo.data.local.entity.ProcessedEventEntity
import com.halo.data.local.entity.StoryEntity
import com.halo.data.matrix.events.HaloPost
import com.halo.data.matrix.events.HaloStory
import com.halo.data.repository.ChatRepository
import com.halo.di.ApplicationScope
import com.halo.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
private const val INVITE_RECONCILE_INTERVAL_MS = 5_000L

@Singleton
class SyncEventProcessor @Inject constructor(
    private val slidingSyncManager: SlidingSyncManager,
    private val matrixClientManager: MatrixClientManager,
    private val chatRepository: ChatRepository,
    private val messageDao: MessageDao,
    private val postDao: PostDao,
    private val storyDao: StoryDao,
    private val userDao: UserDao,
    private val processedEventDao: ProcessedEventDao,
    @ApplicationScope private val appScope: CoroutineScope,
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
    private var inviteReconcileJob: Job? = null
    private val attachInProgress = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    @Volatile private var started = false

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun startProcessing() {
        if (started) return
        started = true
        appScope.launch(ioDispatcher) {
            slidingSyncManager.syncState.collect { state ->
                when (state) {
                    is SyncState.Syncing -> {
                        attachTimelineListeners()

                        // BUG-2 FIX: Accept pending room invites so DMs from
                        // Element/other clients appear in the Halo chat list.
                        reconcileInvitesAndListeners()
                        ensureInviteReconcileLoop()

                        // B11: rooms() may return an empty list on the very first sync
                        // because the SDK hasn't finished populating its room store yet.
                        // Scheduling a second pass a few seconds later catches all rooms
                        // that were missed in the first pass without re-adding duplicates
                        // (attachTimelineListeners is idempotent for rooms already tracked).
                        appScope.launch(ioDispatcher) {
                            delay(LISTENER_RETRY_DELAY_MS)
                            attachTimelineListeners()
                            reconcileInvitesAndListeners()
                        }
                    }
                    is SyncState.Idle, is SyncState.Error -> {
                        inviteReconcileJob?.cancel()
                        inviteReconcileJob = null
                        clearListeners()
                    }
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

    private fun ensureInviteReconcileLoop() {
        if (inviteReconcileJob?.isActive == true) return
        inviteReconcileJob = appScope.launch(ioDispatcher) {
            while (isActive) {
                reconcileInvitesAndListeners()
                delay(INVITE_RECONCILE_INTERVAL_MS)
            }
        }
    }

    private suspend fun reconcileInvitesAndListeners() {
        try {
            val joinedRooms = chatRepository.acceptPendingInvites()
            if (joinedRooms.isNotEmpty()) {
                Log.d(TAG, "Auto-accepted ${joinedRooms.size} DM invite(s)")
                chatRepository.refreshChatRooms()
                attachTimelineListeners()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process pending invites", e)
        }
    }

    /**
     * Attaches a [HaloTimelineListener] to every room that doesn't already have one.
     * Safe to call multiple times — existing listeners are never replaced.
     */
    private suspend fun attachTimelineListeners() {
        val sdkClient = matrixClientManager.getClient() ?: return
        val rooms = sdkClient.rooms()

        if (rooms.isEmpty()) {
            Log.d(TAG, "attachTimelineListeners: room list is empty — will retry")
            return
        }

        rooms.forEach { room ->
            val roomId = room.id()
            if (activeListeners.containsKey(roomId) || !attachInProgress.add(roomId)) return@forEach

            appScope.launch(ioDispatcher) {
                try {
                    val timeline = room.timeline()
                    val handle = timeline.addListener(
                        HaloTimelineListener(roomId, this@SyncEventProcessor)
                    )
                    activeListeners[roomId] = handle
                    Log.d(TAG, "Attached timeline listener for room $roomId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to attach listener for room $roomId", e)
                    activeListeners.remove(roomId)
                } finally {
                    attachInProgress.remove(roomId)
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
    private suspend fun markEventSeenPersisted(eventKey: String): Boolean {
        if (!seenEventIds.add(eventKey)) return false
        val inserted = processedEventDao.insertIfAbsent(
            ProcessedEventEntity(eventKey = eventKey, processedAt = System.currentTimeMillis())
        )
        return inserted != -1L
    }

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
    ): Boolean {
        chatRepository.appendIncomingMessage(roomId, eventId, senderId, body, timestamp)
        return true
    }

    suspend fun processHaloPost(
        eventId: String,
        sender: String,
        roomId: String,
        eventJson: String
    ): Boolean {
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
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode HaloPost (eventId=$eventId)", e)
            return false
        }
    }

    suspend fun processHaloStory(
        eventId: String,
        sender: String,
        roomId: String,
        eventJson: String
    ): Boolean {
        try {
            val haloStory = json.decodeFromString<HaloStory>(eventJson)
            if (HaloStory.isExpired(haloStory.createdAt)) return false
            val entity = StoryEntity(
                eventId   = eventId,
                feedRoomId = roomId,
                authorId  = sender,
                mediaMxc  = haloStory.mediaMxc,
                storyType = haloStory.storyType.name.lowercase(),
                durationMs = haloStory.durationMs,
                caption   = haloStory.caption,
                thumbnailMxc = haloStory.thumbnailMxc,
                blurhash = haloStory.blurhash,
                createdAt = haloStory.createdAt,
                isSeen    = false
            )
            storyDao.insertStories(listOf(entity))
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode HaloStory (eventId=$eventId)", e)
            return false
        }
    }

    suspend fun processTimelineItem(roomId: String, item: org.matrix.rustcomponents.sdk.TimelineItem) {
        val event = item.asEvent() ?: return
        val senderId = event.sender
        val timestamp = event.timestamp.toLong().takeIf { it > 0L } ?: System.currentTimeMillis()
        val eventKey = buildDeterministicEventKey(roomId, senderId, timestamp, event.content.hashCode())
        if (!markEventSeenPersisted(eventKey)) return

        val processed = when (val content = event.content) {
            is org.matrix.rustcomponents.sdk.TimelineItemContent.MsgLike -> {
                val kind = content.content.kind
                if (kind is org.matrix.rustcomponents.sdk.MsgLikeKind.Message) {
                    val body = kind.content.body
                    when {
                        body.startsWith("HALO_POST:") -> processHaloPost(
                            eventId = eventKey, sender = senderId, roomId = roomId, eventJson = body.removePrefix("HALO_POST:")
                        )
                        body.startsWith("HALO_STORY:") -> processHaloStory(
                            eventId = eventKey, sender = senderId, roomId = roomId, eventJson = body.removePrefix("HALO_STORY:")
                        )
                        else -> processRoomMessage(roomId, eventKey, senderId, body, timestamp)
                    }
                } else false
            }
            else -> false
        }
        if (!processed) {
            seenEventIds.remove(eventKey)
        }
    }

    fun launchTimelineProcessing(roomId: String, item: org.matrix.rustcomponents.sdk.TimelineItem) {
        appScope.launch(ioDispatcher) {
            processTimelineItem(roomId, item)
        }
    }

    companion object {
        internal fun buildDeterministicEventKey(
            roomId: String,
            senderId: String,
            timestamp: Long,
            contentHash: Int
        ): String = "$roomId|$senderId|$timestamp|$contentHash"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Timeline listener
// ─────────────────────────────────────────────────────────────────────────────

private class HaloTimelineListener(
    private val roomId: String,
    private val processor: SyncEventProcessor
) : org.matrix.rustcomponents.sdk.TimelineListener {

    override fun onUpdate(diff: List<org.matrix.rustcomponents.sdk.TimelineDiff>) {
        diff.forEach diffLoop@{ d ->
            val items = when (d) {
                is org.matrix.rustcomponents.sdk.TimelineDiff.Append    -> d.values
                is org.matrix.rustcomponents.sdk.TimelineDiff.PushBack  -> listOf(d.value)
                is org.matrix.rustcomponents.sdk.TimelineDiff.PushFront -> listOf(d.value)
                is org.matrix.rustcomponents.sdk.TimelineDiff.Insert    -> listOf(d.value)
                is org.matrix.rustcomponents.sdk.TimelineDiff.Set       -> listOf(d.value)
                else -> emptyList()
            }

            items.forEach itemLoop@{ item ->
                processor.launchTimelineProcessing(roomId, item)
            }
        }
    }
}
