package com.halo.ui.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes network connectivity state as a reactive Flow.
 * Used to suppress sync errors and show offline banners.
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Emit initial state
        trySend(manager.isCurrentlyConnected())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(manager.isCurrentlyConnected()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        manager.registerNetworkCallback(request, callback)

        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun ConnectivityManager.isCurrentlyConnected(): Boolean {
        val network = activeNetwork ?: return false
        val caps = getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
