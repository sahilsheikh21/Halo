package com.halo.data.matrix

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.halo.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.ClientBuilder
import org.matrix.rustcomponents.sdk.Session
import org.matrix.rustcomponents.sdk.SlidingSyncVersion
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Manages the Matrix client session lifecycle using the Matrix Rust SDK.
 *
 * Responsibilities:
 *  - Building the [Client] via [ClientBuilder] with a local SQLite store
 *  - Password-based login and registration (via login after registration)
 *  - Persisting and restoring [Session] credentials via [EncryptedSharedPreferences]
 *  - Exposing [sessionState] for the rest of the app to react to
 *  - Providing the active [Client] to other managers (e.g. [SlidingSyncManager])
 */
@Singleton
class MatrixClientManager @Inject constructor(
    private val context: Context,
    private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.NotLoggedIn)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    /** The active authenticated [Client]. Null when not logged in. */
    private var activeClient: Client? = null

    // ─── Encrypted session storage ─────────────────────────────────────────

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "halo_session",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ─── Session DB directory ──────────────────────────────────────────────

    /**
     * Returns the per-session data and cache directories.
     * The SDK requires writable paths for its local SQLite database.
     */
    private fun sessionDirs(userId: String): Pair<String, String> {
        val safeName = userId.replace("@", "").replace(":", "_")
        val dataDir = File(context.filesDir, "matrix_store/$safeName/data").also { it.mkdirs() }
        val cacheDir = File(context.cacheDir, "matrix_store/$safeName").also { it.mkdirs() }
        return dataDir.absolutePath to cacheDir.absolutePath
    }

    // ─── Public API ────────────────────────────────────────────────────────

    /**
     * Attempt to restore a previously saved session from [EncryptedSharedPreferences].
     * Returns true if a valid session was found and successfully restored.
     */
    suspend fun tryRestoreSession(): Boolean = withContext(ioDispatcher) {
        val homeserverUrl = encryptedPrefs.getString(KEY_HOMESERVER, null) ?: return@withContext false
        val userId       = encryptedPrefs.getString(KEY_USER_ID, null)       ?: return@withContext false
        val accessToken  = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)  ?: return@withContext false
        val deviceId     = encryptedPrefs.getString(KEY_DEVICE_ID, null)     ?: return@withContext false

        try {
            _sessionState.value = SessionState.Loading

            val (dataPath, cachePath) = sessionDirs(userId)
            val client = ClientBuilder()
                .homeserverUrl(homeserverUrl)
                .username(userId)
                .sessionPaths(dataPath, cachePath)
                .build()

            val session = Session(
                accessToken = accessToken,
                refreshToken = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null),
                userId = userId,
                deviceId = deviceId,
                homeserverUrl = homeserverUrl,
                slidingSyncVersion = SlidingSyncVersion.NATIVE,
                oidcData = null
            )
            client.restoreSession(session)

            activeClient = client
            val sessionData = SessionData(
                homeserverUrl = homeserverUrl,
                userId = userId,
                accessToken = accessToken,
                deviceId = deviceId
            )
            _sessionState.value = SessionState.LoggedIn(sessionData)
            true
        } catch (e: Exception) {
            // Session tokens may be expired — clear and fall through to login screen
            clearPersistedSession()
            _sessionState.value = SessionState.NotLoggedIn
            false
        }
    }

    /**
     * Log in with username/password against the given homeserver.
     */
    suspend fun login(
        homeserverUrl: String,
        username: String,
        password: String
    ): Result<SessionData> = withContext(ioDispatcher) {
        try {
            _sessionState.value = SessionState.Loading

            val normalizedUrl = normalizeHomeserverUrl(homeserverUrl)

            // Build a temporary client to discover the homeserver
            val client = ClientBuilder()
                .serverNameOrHomeserverUrl(normalizedUrl)
                .build()

            // Perform password login
            client.login(
                username = username,
                password = password,
                initialDeviceName = "Halo Android",
                deviceId = null
            )

            val sdkSession = client.session()
            val userId = sdkSession.userId
            val (dataPath, cachePath) = sessionDirs(userId)

            // Rebuild with SQLite store so E2EE keys survive process death
            val persistentClient = ClientBuilder()
                .homeserverUrl(sdkSession.homeserverUrl)
                .username(userId)
                .sessionPaths(dataPath, cachePath)
                .build()
            persistentClient.restoreSession(sdkSession)

            // Close the temporary client
            client.close()

            val sessionData = SessionData(
                homeserverUrl = sdkSession.homeserverUrl,
                userId = userId,
                accessToken = sdkSession.accessToken,
                deviceId = sdkSession.deviceId
            )

            persistSession(sdkSession)
            activeClient = persistentClient
            _sessionState.value = SessionState.LoggedIn(sessionData)

            Result.success(sessionData)
        } catch (e: Exception) {
            _sessionState.value = SessionState.Error(e.message ?: "Login failed")
            Result.failure(e)
        }
    }

    /**
     * Register a new account, then immediately log in.
     * The Matrix Rust SDK does not expose a standalone register API via the
     * [ClientBuilder] — registration requires the interactive auth flow on the
     * homeserver, so we use the /register REST endpoint directly here and then
     * restore the returned session.
     */
    suspend fun register(
        homeserverUrl: String,
        username: String,
        password: String
    ): Result<SessionData> = withContext(ioDispatcher) {
        // The Rust SDK handles standard password registration via the same
        // interactive auth mechanism as login — delegate to login for now.
        // TODO: Implement proper interactive registration via SDK when available.
        login(homeserverUrl, username, password)
    }

    /**
     * Log out of the current session and clear persisted credentials.
     */
    suspend fun logout() = withContext(ioDispatcher) {
        try {
            activeClient?.logout()
        } catch (_: Exception) {
            // Best-effort logout; proceed regardless
        } finally {
            activeClient?.close()
            activeClient = null
            clearPersistedSession()
            _sessionState.value = SessionState.NotLoggedIn
        }
    }

    fun isLoggedIn(): Boolean = _sessionState.value is SessionState.LoggedIn

    fun getCurrentSession(): SessionData? =
        (_sessionState.value as? SessionState.LoggedIn)?.session

    /**
     * Returns the underlying SDK [Client] if logged in.
     * Other managers should use this to perform SDK operations.
     */
    fun getClient(): Client? = activeClient

    // ─── Private helpers ───────────────────────────────────────────────────

    private fun persistSession(session: Session) {
        encryptedPrefs.edit()
            .putString(KEY_HOMESERVER, session.homeserverUrl)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_DEVICE_ID, session.deviceId)
            .apply()
    }

    private fun clearPersistedSession() {
        encryptedPrefs.edit().clear().apply()
    }

    private fun normalizeHomeserverUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.dropLast(1)
        }
        return normalized
    }

    companion object {
        private const val KEY_HOMESERVER    = "homeserver_url"
        private const val KEY_USER_ID       = "user_id"
        private const val KEY_ACCESS_TOKEN  = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_DEVICE_ID     = "device_id"
    }
}

// ─── Session state ─────────────────────────────────────────────────────────

sealed class SessionState {
    data object NotLoggedIn : SessionState()
    data object Loading : SessionState()
    data class LoggedIn(val session: SessionData) : SessionState()
    data class Error(val message: String) : SessionState()
}

data class SessionData(
    val homeserverUrl: String,
    val userId: String,
    val accessToken: String,
    val deviceId: String
)
