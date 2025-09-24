package com.hazardhawk.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.hazardhawk.camera.GpsCoordinates
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service for acquiring live GPS location data for photo metadata
 * Addresses the issue where all photos show the same GPS coordinates
 */
class LiveLocationService(private val context: Context) {
    
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    companion object {
        private const val GPS_TIMEOUT_MS = 10000L // 10 seconds
        private const val LOCATION_ACCURACY_THRESHOLD = 50f // 50 meters
        private const val LOCATION_FRESHNESS_MS = 30000L // 30 seconds
    }
    
    /**
     * Get current live GPS location with timeout and accuracy requirements
     * This replaces the problematic getLastKnownLocation() approach
     */
    suspend fun getCurrentLiveLocation(): GpsCoordinates? {
        if (!hasLocationPermission()) {
            return null
        }
        
        if (!isLocationEnabled()) {
            return null
        }
        
        // Try GPS first for highest accuracy
        val gpsLocation = withTimeoutOrNull(GPS_TIMEOUT_MS) {
            requestLocationUpdate(LocationManager.GPS_PROVIDER)
        }
        
        if (gpsLocation != null && isLocationAccurate(gpsLocation)) {
            return gpsLocation.toGpsCoordinates()
        }
        
        // Fallback to network provider
        val networkLocation = withTimeoutOrNull(GPS_TIMEOUT_MS / 2) {
            requestLocationUpdate(LocationManager.NETWORK_PROVIDER)
        }
        
        return networkLocation?.toGpsCoordinates()
    }
    
    /**
     * Get the best available location (live or cached) with freshness validation
     */
    suspend fun getBestAvailableLocation(): GpsCoordinates? {
        // First try live location
        getCurrentLiveLocation()?.let { return it }
        
        // If live location fails, check if last known is fresh enough
        if (hasLocationPermission()) {
            try {
                val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                
                val bestLast = when {
                    lastGps != null && lastNetwork != null -> {
                        if (lastGps.time > lastNetwork.time) lastGps else lastNetwork
                    }
                    lastGps != null -> lastGps
                    lastNetwork != null -> lastNetwork
                    else -> null
                }
                
                bestLast?.let { location ->
                    if (isLocationFresh(location)) {
                        return location.toGpsCoordinates()
                    }
                }
            } catch (e: SecurityException) {
                return null
            }
        }
        
        return null
    }
    
    private suspend fun requestLocationUpdate(provider: String): Location? = 
        suspendCancellableCoroutine { continuation ->
            
            if (!locationManager.isProviderEnabled(provider)) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    continuation.resume(location)
                }
                
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    locationManager.removeUpdates(this)
                    continuation.resume(null)
                }
                
                @Suppress("DEPRECATION")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    1000, // 1 second intervals
                    0f,   // 0 meter intervals
                    listener,
                    Looper.getMainLooper()
                )
                
                continuation.invokeOnCancellation {
                    locationManager.removeUpdates(listener)
                }
                
            } catch (e: SecurityException) {
                continuation.resumeWithException(e)
            }
        }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || 
        ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    private fun isLocationAccurate(location: Location): Boolean {
        return !location.hasAccuracy() || location.accuracy <= LOCATION_ACCURACY_THRESHOLD
    }
    
    private fun isLocationFresh(location: Location): Boolean {
        return (System.currentTimeMillis() - location.time) <= LOCATION_FRESHNESS_MS
    }
    
    private fun Location.toGpsCoordinates(): GpsCoordinates {
        return GpsCoordinates(
            latitude = latitude,
            longitude = longitude,
            altitude = if (hasAltitude()) altitude else null,
            accuracy = if (hasAccuracy()) accuracy else null
        )
    }
}
