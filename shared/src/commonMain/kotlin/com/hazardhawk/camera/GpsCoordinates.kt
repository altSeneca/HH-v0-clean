package com.hazardhawk.camera

import kotlinx.serialization.Serializable

/**
 * GPS coordinates data class for location metadata
 * Used across the multiplatform project for consistent location handling
 */
@Serializable
data class GpsCoordinates(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val bearing: Float? = null,
    val speed: Float? = null,
    val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
) {
    /**
     * Format coordinates as human-readable string
     */
    fun toDisplayString(): String {
        return "${latitude.format(6)}, ${longitude.format(6)}"
    }
    
    /**
     * Check if coordinates are valid (not null island)
     */
    fun isValid(): Boolean {
        return latitude != 0.0 || longitude != 0.0
    }
    
    /**
     * Calculate distance to another coordinate in meters
     */
    fun distanceTo(other: GpsCoordinates): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        
        val lat1Rad = kotlin.math.PI * latitude / 180.0
        val lat2Rad = kotlin.math.PI * other.latitude / 180.0
        val deltaLatRad = kotlin.math.PI * (other.latitude - latitude) / 180.0
        val deltaLonRad = kotlin.math.PI * (other.longitude - longitude) / 180.0
        
        val a = kotlin.math.sin(deltaLatRad / 2) * kotlin.math.sin(deltaLatRad / 2) +
                kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
                kotlin.math.sin(deltaLonRad / 2) * kotlin.math.sin(deltaLonRad / 2)
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    companion object {
        /**
         * Create GpsCoordinates from latitude and longitude strings
         */
        fun fromStrings(lat: String, lon: String): GpsCoordinates? {
            return try {
                GpsCoordinates(
                    latitude = lat.toDouble(),
                    longitude = lon.toDouble()
                )
            } catch (e: NumberFormatException) {
                null
            }
        }
    }
}

/**
 * Extension function to format Double with specified decimal places
 */
private fun Double.format(digits: Int): String {
    // Simple formatting for KMP compatibility
    return toString().let { str ->
        val dotIndex = str.indexOf('.')
        if (dotIndex == -1) str else str.substring(0, minOf(str.length, dotIndex + digits + 1))
    }
}
