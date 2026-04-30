package com.halo.data.matrix

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.halo.di.ApplicationScope
import org.matrix.rustcomponents.sdk.SyncService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages SyncService connections for real-time updates.
 */
@Singleton
class SlidingSyncManager @Inject constructor(
    private val matrixClientManager: MatrixClientManager,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var activeSyncService: SyncService? = null

    /**
     * Start the sync connection.
     * This begins streaming room updates from the homeserver.
     */
    suspend fun startSync() {
        if (_syncState.value == SyncState.Syncing) return

        try {
            _syncState.value = SyncState.Starting

            val client = matrixClientManager.getClient()
                ?: throw IllegalStateException("Matrix Client not initialized")

            val syncService = activeSyncService ?: client.syncService()
                .withSharePos(true)
                .withOfflineMode()
                .finish()

            activeSyncService = syncService
            syncService.start()

            _syncState.value = SyncState.Syncing
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
        }
    }

    /**
     * Stop the sync connection.
     */
    suspend fun stopSync() {
        try {
            activeSyncService?.stop()
        } catch (_: Exception) {
        } finally {
            activeSyncService = null
            _syncState.value = SyncState.Idle
        }
    }

    /**
     * Get the SyncService instance to access the RoomListService or timelines.
     */
    fun getSyncService(): SyncService? = activeSyncService

    /**
     * Subscribe to a specific room for real-time updates.
     */
    suspend fun subscribeToRoom(roomId: String) {
        // TODO: Subscribe to room timeline via RoomListService
    }

    /**
     * Unsubscribe from a room's real-time updates.
     */
    suspend fun unsubscribeFromRoom(roomId: String) {
        // TODO: Remove room subscription
    }
}

sealed class SyncState {
    data object Idle : SyncState()
    data object Starting : SyncState()
    data object Syncing : SyncState()
    data class Error(val message: String) : SyncState()
}
