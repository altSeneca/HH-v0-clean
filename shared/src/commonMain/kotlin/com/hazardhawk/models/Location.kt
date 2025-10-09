package com.hazardhawk.models

import kotlinx.serialization.Serializable

/**
 * Location data class for backward compatibility with tests
 * Maps to GpsCoordinates from camera package for actual GPS handling
 */
@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val accuracy: Float? = null
) {
    /**
     * Convert to GpsCoordinates for camera/GPS operations
     */
    fun toGpsCoordinates(): com.hazardhawk.camera.GpsCoordinates {
        return com.hazardhawk.camera.GpsCoordinates(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        )
    }
    
    companion object {
        /**
         * Create Location from GpsCoordinates
         */
        fun fromGpsCoordinates(gps: com.hazardhawk.camera.GpsCoordinates): Location {
            return Location(
                latitude = gps.latitude,
                longitude = gps.longitude,
                accuracy = gps.accuracy
            )
        }
    }
}