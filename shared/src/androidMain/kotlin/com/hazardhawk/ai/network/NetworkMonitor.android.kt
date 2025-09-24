package com.hazardhawk.ai.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.hazardhawk.ai.core.NetworkConnectivityService
import com.hazardhawk.ai.core.ConnectionQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Android-specific network connectivity monitor.
 */
class AndroidNetworkMonitor(private val context: Context) : NetworkConnectivityService {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isConnected = MutableStateFlow(false)
    private val _connectionQuality = MutableStateFlow(ConnectionQuality.POOR)
    
    override val isConnected: Boolean
        get() = _isConnected.value
    
    override val connectionQuality: ConnectionQuality
        get() = _connectionQuality.value
    
    val isConnectedFlow: StateFlow<Boolean> = _isConnected
    val connectionQualityFlow: StateFlow<ConnectionQuality> = _connectionQuality
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            _isConnected.value = true
            updateConnectionQuality()
        }
        
        override fun onLost(network: Network) {
            super.onLost(network)
            _isConnected.value = false
            _connectionQuality.value = ConnectionQuality.POOR
        }
        
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            updateConnectionQuality()
        }
    }
    
    fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        updateConnectionStatus()
    }
    
    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore unregister errors
        }
    }
    
    private fun updateConnectionStatus() {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        _isConnected.value = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        updateConnectionQuality()
    }
    
    private fun updateConnectionQuality() {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        if (networkCapabilities == null) {
            _connectionQuality.value = ConnectionQuality.POOR
            return
        }
        
        val quality = when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val signalStrength = networkCapabilities.signalStrength
                when {
                    signalStrength > -50 -> ConnectionQuality.EXCELLENT
                    signalStrength > -60 -> ConnectionQuality.GOOD
                    signalStrength > -70 -> ConnectionQuality.FAIR
                    else -> ConnectionQuality.POOR
                }
            }
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                // Estimate based on network type
                when {
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> 
                        ConnectionQuality.GOOD
                    networkCapabilities.linkDownstreamBandwidthKbps > 10000 -> 
                        ConnectionQuality.GOOD
                    networkCapabilities.linkDownstreamBandwidthKbps > 5000 -> 
                        ConnectionQuality.FAIR
                    else -> ConnectionQuality.POOR
                }
            }
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 
                ConnectionQuality.EXCELLENT
            else -> ConnectionQuality.POOR
        }
        
        _connectionQuality.value = quality
    }
    
    /**
     * Test network latency to assess connection quality for AI services.
     */
    suspend fun testLatency(): Long {
        // TODO: Implement ping test to AI service endpoints
        return 100L // Mock latency
    }
    
    /**
     * Estimate bandwidth for large model downloads or cloud inference.
     */
    suspend fun estimateBandwidth(): Long {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        return networkCapabilities?.linkDownstreamBandwidthKbps?.toLong() ?: 1000L
    }
}