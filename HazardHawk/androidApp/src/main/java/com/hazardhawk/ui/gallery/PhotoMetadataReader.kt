package com.hazardhawk.ui.gallery

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * PhotoMetadataReader - EXIF data extraction utility for construction safety photos
 * 
 * Features:
 * - Privacy-compliant GPS data handling
 * - Construction-relevant metadata extraction
 * - Memory-efficient processing
 * - GDPR compliance with data sanitization
 */

data class PhotoMetadata(
    val fileName: String,
    val filePath: String,
    val dateTime: String?,
    val gpsLatitude: Double?,
    val gpsLongitude: Double?,
    val cameraSettings: CameraSettings?,
    val orientation: Int,
    val fileSize: Long,
    val dimensions: Pair<Int, Int>?,
    val locationAddress: String? = null,
    val sanitizedForPrivacy: Boolean = false
)

data class CameraSettings(
    val make: String?,
    val model: String?,
    val fNumber: String?,
    val exposureTime: String?,
    val iso: String?,
    val focalLength: String?,
    val flash: String?
)

data class GPSCoordinates(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?
)

class PhotoMetadataReader(
    private val context: Context,
    private val privacySettings: PrivacySettings = PrivacySettings()
) {
    
    /**
     * Extract comprehensive metadata from photo file
     */
    suspend fun extractMetadata(file: File): PhotoMetadata = withContext(Dispatchers.IO) {
        try {
            val exif = ExifInterface(file.absolutePath)
            val gpsCoordinates = extractGPSCoordinates(exif)
            val locationAddress = gpsCoordinates?.let { coords ->
                if (privacySettings.includeLocationData) {
                    getAddressFromCoordinates(coords.latitude, coords.longitude)
                } else null
            }
            
            PhotoMetadata(
                fileName = file.name,
                filePath = file.absolutePath,
                dateTime = extractDateTime(exif),
                gpsLatitude = if (privacySettings.includeLocationData) gpsCoordinates?.latitude else null,
                gpsLongitude = if (privacySettings.includeLocationData) gpsCoordinates?.longitude else null,
                cameraSettings = extractCameraSettings(exif),
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL),
                fileSize = file.length(),
                dimensions = extractDimensions(exif),
                locationAddress = locationAddress,
                sanitizedForPrivacy = !privacySettings.includeLocationData
            )
        } catch (e: Exception) {
            // Return basic metadata if EXIF reading fails
            PhotoMetadata(
                fileName = file.name,
                filePath = file.absolutePath,
                dateTime = formatDate(Date(file.lastModified())),
                gpsLatitude = null,
                gpsLongitude = null,
                cameraSettings = null,
                orientation = ExifInterface.ORIENTATION_NORMAL,
                fileSize = file.length(),
                dimensions = null,
                sanitizedForPrivacy = true
            )
        }
    }
    
    /**
     * Extract GPS coordinates with privacy compliance
     */
    private fun extractGPSCoordinates(exif: ExifInterface): GPSCoordinates? {
        if (!privacySettings.includeLocationData) return null
        
        return try {
            val latLng = exif.latLong
            val altitude = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)?.toDoubleOrNull()
            
            latLng?.let { coords ->
                GPSCoordinates(
                    latitude = coords[0],
                    longitude = coords[1],
                    altitude = altitude
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract camera settings for technical analysis
     */
    private fun extractCameraSettings(exif: ExifInterface): CameraSettings {
        return CameraSettings(
            make = exif.getAttribute(ExifInterface.TAG_MAKE),
            model = exif.getAttribute(ExifInterface.TAG_MODEL),
            fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER),
            exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
            iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS),
            focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH),
            flash = exif.getAttribute(ExifInterface.TAG_FLASH)?.let { flashValue ->
                when (flashValue.toIntOrNull()) {
                    0x0 -> "No Flash"
                    0x1 -> "Flash Fired"
                    0x5 -> "Flash Fired, Return not detected"
                    0x7 -> "Flash Fired, Return detected"
                    0x9 -> "Flash Fired, Compulsory"
                    0xD -> "Flash Fired, Compulsory, Return not detected"
                    0xF -> "Flash Fired, Compulsory, Return detected"
                    0x10 -> "No Flash, Compulsory"
                    0x18 -> "No Flash, Auto"
                    0x19 -> "Flash Fired, Auto"
                    0x1D -> "Flash Fired, Auto, Return not detected"
                    0x1F -> "Flash Fired, Auto, Return detected"
                    0x20 -> "No flash function"
                    0x41 -> "Flash Fired, Red-eye reduction"
                    0x45 -> "Flash Fired, Red-eye reduction, Return not detected"
                    0x47 -> "Flash Fired, Red-eye reduction, Return detected"
                    0x49 -> "Flash Fired, Compulsory, Red-eye reduction"
                    0x4D -> "Flash Fired, Compulsory, Red-eye reduction, Return not detected"
                    0x4F -> "Flash Fired, Compulsory, Red-eye reduction, Return detected"
                    0x59 -> "Flash Fired, Auto, Red-eye reduction"
                    0x5D -> "Flash Fired, Auto, Red-eye reduction, Return not detected"
                    0x5F -> "Flash Fired, Auto, Red-eye reduction, Return detected"
                    else -> "Unknown ($flashValue)"
                }
            }
        )
    }
    
    /**
     * Extract photo dimensions
     */
    private fun extractDimensions(exif: ExifInterface): Pair<Int, Int>? {
        val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
        val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
        
        return if (width > 0 && height > 0) {
            Pair(width, height)
        } else null
    }
    
    /**
     * Extract and format date/time
     */
    private fun extractDateTime(exif: ExifInterface): String? {
        return exif.getAttribute(ExifInterface.TAG_DATETIME)?.let { dateTimeString ->
            try {
                val exifFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                val displayFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                val date = exifFormat.parse(dateTimeString)
                date?.let { displayFormat.format(it) }
            } catch (e: Exception) {
                dateTimeString // Return original if parsing fails
            }
        }
    }
    
    /**
     * Format date for display
     */
    private fun formatDate(date: Date): String {
        val format = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
        return format.format(date)
    }
    
    /**
     * Convert GPS coordinates to address (with privacy controls)
     */
    @SuppressLint("MissingPermission")
    private suspend fun getAddressFromCoordinates(
        latitude: Double, 
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        if (!privacySettings.includeLocationData || !privacySettings.includeAddressLookup) {
            return@withContext null
        }
        
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.let { address ->
                buildString {
                    address.thoroughfare?.let { append(it) }
                    address.locality?.let { 
                        if (isNotEmpty()) append(", ")
                        append(it)
                    }
                    address.adminArea?.let {
                        if (isNotEmpty()) append(", ")
                        append(it)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get file size in human-readable format
     */
    fun getFormattedFileSize(sizeInBytes: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024
        
        return when {
            sizeInBytes >= gb -> "${String.format("%.2f", sizeInBytes.toDouble() / gb)} GB"
            sizeInBytes >= mb -> "${String.format("%.2f", sizeInBytes.toDouble() / mb)} MB"
            sizeInBytes >= kb -> "${String.format("%.2f", sizeInBytes.toDouble() / kb)} KB"
            else -> "$sizeInBytes B"
        }
    }
    
    /**
     * Get orientation description
     */
    fun getOrientationDescription(orientation: Int): String {
        return when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> "Normal"
            ExifInterface.ORIENTATION_ROTATE_90 -> "Rotated 90° CW"
            ExifInterface.ORIENTATION_ROTATE_180 -> "Rotated 180°"
            ExifInterface.ORIENTATION_ROTATE_270 -> "Rotated 270° CW"
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Flipped Horizontally"
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Flipped Vertically"
            ExifInterface.ORIENTATION_TRANSPOSE -> "Transposed"
            ExifInterface.ORIENTATION_TRANSVERSE -> "Transverse"
            else -> "Unknown"
        }
    }
    
    /**
     * Sanitize metadata for sharing (removes sensitive data)
     */
    fun sanitizeForSharing(metadata: PhotoMetadata): PhotoMetadata {
        return metadata.copy(
            filePath = "[REDACTED]", // Remove full file path
            gpsLatitude = null,       // Remove GPS coordinates
            gpsLongitude = null,
            locationAddress = null,   // Remove address
            sanitizedForPrivacy = true
        )
    }
}

/**
 * Privacy settings for EXIF data extraction
 */
data class PrivacySettings(
    val includeLocationData: Boolean = true,
    val includeAddressLookup: Boolean = true,
    val includeCameraSettings: Boolean = true,
    val logSensitiveData: Boolean = false
)

/**
 * Extension functions for PhotoMetadata
 */
fun PhotoMetadata.hasGPSData(): Boolean {
    return gpsLatitude != null && gpsLongitude != null
}

fun PhotoMetadata.getCoordinatesString(): String? {
    return if (hasGPSData()) {
        "${String.format("%.6f", gpsLatitude!!)}, ${String.format("%.6f", gpsLongitude!!)}"
    } else null
}

fun PhotoMetadata.getDimensionsString(): String? {
    return dimensions?.let { (width, height) -> "${width} × ${height} px" }
}

fun PhotoMetadata.isLandscape(): Boolean {
    return dimensions?.let { (width, height) -> width > height } ?: false
}

fun PhotoMetadata.getAspectRatio(): Float? {
    return dimensions?.let { (width, height) -> 
        if (height > 0) width.toFloat() / height.toFloat() else null 
    }
}