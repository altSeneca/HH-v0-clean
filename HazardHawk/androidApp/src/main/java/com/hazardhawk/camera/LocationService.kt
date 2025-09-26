package com.hazardhawk.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val altitude: Double = 0.0,
    val address: String = "Unknown Location",
    val timestamp: Long = System.currentTimeMillis(),
    val isAvailable: Boolean = false
)

class LocationService(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val geocoder = Geocoder(context, Locale.getDefault())
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    private val _locationData = MutableStateFlow(LocationData())
    val locationData: StateFlow<LocationData> = _locationData.asStateFlow()
    
    private var currentLocationListener: LocationListener? = null
    
    companion object {
        private const val TAG = "LocationService"
        private const val MIN_TIME_MS = 30000L // 30 seconds (battery optimized)
        private const val MIN_DISTANCE_M = 50f // 50 meters (battery optimized)
        private const val LOCATION_TIMEOUT_MS = 10000L // 10 seconds
        private const val GEOCODING_TIMEOUT_MS = 5000L // 5 seconds
    }
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Start location updates
     */
    fun startLocationUpdates() {
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Location permissions not granted")
            return
        }
        
        try {
            stopLocationUpdates() // Stop any existing updates
            
            val provider = getBestProvider()
            if (provider == null) {
                Log.w(TAG, "No location provider available")
                return
            }
            
            currentLocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
                    updateLocation(location)
                }
                
                override fun onProviderEnabled(provider: String) {
                    Log.d(TAG, "Provider enabled: $provider")
                }
                
                override fun onProviderDisabled(provider: String) {
                    Log.d(TAG, "Provider disabled: $provider")
                }
                
                @Deprecated("Deprecated in API level 29")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
                    // Handle status changes if needed
                }
            }
            
            locationManager.requestLocationUpdates(
                provider,
                MIN_TIME_MS,
                MIN_DISTANCE_M,
                currentLocationListener!!
            )
            
            // Try to get last known location immediately
            val lastLocation = locationManager.getLastKnownLocation(provider)
            lastLocation?.let { updateLocation(it) }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when requesting location updates", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates", e)
        }
    }
    
    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        currentLocationListener?.let { listener ->
            try {
                locationManager.removeUpdates(listener)
                currentLocationListener = null
                Log.d(TAG, "Location updates stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping location updates", e)
            }
        }
    }
    
    /**
     * Get current location as a one-time request
     */
    suspend fun getCurrentLocation(): LocationData? {
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Location permissions not granted")
            return null
        }
        
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            try {
                val provider = getBestProvider() ?: return@withTimeoutOrNull null
                
                // Try to get last known location first
                val lastLocation = locationManager.getLastKnownLocation(provider)
                if (lastLocation != null && isLocationRecent(lastLocation)) {
                    return@withTimeoutOrNull createLocationData(lastLocation)
                }
                
                // Request fresh location
                suspendCancellableCoroutine<LocationData?> { continuation ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            locationManager.removeUpdates(this)
                            coroutineScope.launch {
                                val locationData = createLocationData(location)
                                continuation.resume(locationData)
                            }
                        }
                        
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {
                            locationManager.removeUpdates(this)
                            continuation.resume(null)
                        }
                        
                        @Deprecated("Deprecated in API level 29")
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                    }
                    
                    try {
                        locationManager.requestLocationUpdates(
                            provider,
                            MIN_TIME_MS,
                            MIN_DISTANCE_M,
                            listener
                        )
                        
                        continuation.invokeOnCancellation {
                            locationManager.removeUpdates(listener)
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Security exception in getCurrentLocation", e)
                        continuation.resume(null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting current location", e)
                null
            }
        }
    }
    
    /**
     * Get the best available location provider
     */
    private fun getBestProvider(): String? {
        return when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> {
                LocationManager.GPS_PROVIDER
            }
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> {
                LocationManager.NETWORK_PROVIDER
            }
            locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER) -> {
                LocationManager.PASSIVE_PROVIDER
            }
            else -> null
        }
    }
    
    /**
     * Check if location is recent (within 5 minutes)
     */
    private fun isLocationRecent(location: Location): Boolean {
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
        return location.time > fiveMinutesAgo
    }
    
    /**
     * Update location data and notify observers
     */
    private fun updateLocation(location: Location) {
        coroutineScope.launch {
            val locationData = createLocationData(location)
            _locationData.value = locationData
        }
    }
    
    /**
     * Create LocationData from Android Location with geocoding
     */
    private suspend fun createLocationData(location: Location): LocationData {
        val address = getAddressFromLocation(location.latitude, location.longitude)
        
        return LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            altitude = if (location.hasAltitude()) location.altitude else 0.0,
            address = address,
            timestamp = location.time,
            isAvailable = true
        )
    }
    
    /**
     * Reverse geocode coordinates to get human-readable address
     */
    private suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                withTimeoutOrNull(GEOCODING_TIMEOUT_MS) {
                    if (Geocoder.isPresent()) {
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            buildString {
                                address.thoroughfare?.let { append("$it, ") }
                                address.locality?.let { append("$it, ") }
                                address.adminArea?.let { append("$it ") }
                                address.postalCode?.let { append(it) }
                            }.trim().removeSuffix(",")
                        } else {
                            "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
                        }
                    } else {
                        "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
                    }
                } ?: "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
            } catch (e: Exception) {
                Log.w(TAG, "Geocoding failed, using coordinates", e)
                "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
            }
        }
    }
    
    /**
     * Format location data for display
     */
    fun formatLocationForDisplay(locationData: LocationData): String {
        return if (locationData.isAvailable) {
            "${locationData.address}\n${String.format("%.6f", locationData.latitude)}, ${String.format("%.6f", locationData.longitude)}"
        } else {
            "📍 Location unavailable"
        }
    }
    
    /**
     * Format coordinates for EXIF embedding
     */
    fun formatCoordinatesForExif(locationData: LocationData): Pair<String, String>? {
        if (!locationData.isAvailable) return null
        
        val lat = locationData.latitude
        val lng = locationData.longitude
        
        val latRef = if (lat >= 0) "N" else "S"
        val lngRef = if (lng >= 0) "E" else "W"
        
        return Pair(
            "${Math.abs(lat)},$latRef",
            "${Math.abs(lng)},$lngRef"
        )
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopLocationUpdates()
    }
}