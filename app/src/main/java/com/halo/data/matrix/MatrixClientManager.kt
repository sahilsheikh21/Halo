package com.halo.data.matrix

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Matrix client session lifecycle.
 */
@Singleton
class MatrixClientManager @Inject constructor(
    private val context: Context,
    private val applicationScope: CoroutineScope
) {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.NotLoggedIn)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

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

    suspend fun tryRestoreSession(): Boolean {
        return false // mock
    }

    suspend fun login(
        homeserverUrl: String,
        username: String,
        password: String
    ): Result<SessionData> {
        return try {
            _sessionState.value = SessionState.Loading

            val normalizedUrl = normalizeHomeserverUrl(homeserverUrl)
            
            // TODO: Use real Matrix SDK
            // val client = ClientBuilder().homeserverUrl(normalizedUrl).build()

            val sessionData = SessionData(
                homeserverUrl = normalizedUrl,
                userId = "@$username:matrix.org",
                accessToken = "fake_token",
                deviceId = "dev"
            )

            persistSession(sessionData)
            _sessionState.value = SessionState.LoggedIn(sessionData)

            Result.success(sessionData)
        } catch (e: Exception) {
            _sessionState.value = SessionState.Error(e.message ?: "Login failed")
            Result.failure(e)
        }
    }

    suspend fun register(
        homeserverUrl: String,
        username: String,
        password: String
    ): Result<SessionData> {
        return login(homeserverUrl, username, password) // mock
    }

    suspend fun logout() {
        encryptedPrefs.edit().clear().apply()
        _sessionState.value = SessionState.NotLoggedIn
    }

    fun isLoggedIn(): Boolean = _sessionState.value is SessionState.LoggedIn

    fun getCurrentSession(): SessionData? {
        return (_sessionState.value as? SessionState.LoggedIn)?.session
    }

    private fun persistSession(session: SessionData) {
        encryptedPrefs.edit()
            .putString(KEY_HOMESERVER, session.homeserverUrl)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_DEVICE_ID, session.deviceId)
            .apply()
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
        private const val KEY_HOMESERVER = "homeserver_url"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_DEVICE_ID = "device_id"
    }
}

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
