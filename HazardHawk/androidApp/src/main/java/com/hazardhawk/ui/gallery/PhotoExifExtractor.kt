package com.hazardhawk.ui.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.hazardhawk.camera.PhotoOrientationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * PhotoExifExtractor - Advanced EXIF processing utilities for construction safety photos
 * 
 * Features:
 * - Memory-efficient bitmap operations
 * - Orientation correction for proper display
 * - Thumbnail generation with EXIF preservation
 * - Batch processing capabilities
 * - Privacy-compliant data handling
 */

class PhotoExifExtractor(
    private val context: Context
) {
    
    /**
     * Load bitmap with proper orientation correction
     */
    suspend fun loadOrientedBitmap(
        file: File, 
        targetWidth: Int? = null,
        targetHeight: Int? = null
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            // Calculate sample size for memory efficiency
            val options = BitmapFactory.Options().apply {
                if (targetWidth != null || targetHeight != null) {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeFile(file.absolutePath, this)
                    
                    val sampleSize = calculateInSampleSize(
                        this, 
                        targetWidth ?: outWidth, 
                        targetHeight ?: outHeight
                    )
                    
                    inSampleSize = sampleSize
                    inJustDecodeBounds = false
                }
            }
            
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            bitmap?.let { correctOrientation(it, orientation) }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculate optimal sample size for bitmap loading
     * Now uses centralized PhotoOrientationManager for consistency
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        return PhotoOrientationManager.getInstance()
            .calculateInSampleSize(options, reqWidth, reqHeight)
    }
    
    /**
     * Correct bitmap orientation based on EXIF data
     * Now uses centralized PhotoOrientationManager for consistency
     */
    private fun correctOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val photoOrientation = PhotoOrientationManager.PhotoOrientation.fromExifValue(orientation)
        return PhotoOrientationManager.getInstance()
            .applyOrientationToBitmap(bitmap, photoOrientation)
    }
    
    /**
     * Generate thumbnail with EXIF data preservation
     */
    suspend fun generateThumbnail(
        sourceFile: File,
        thumbnailFile: File,
        maxWidth: Int = 512,
        maxHeight: Int = 512,
        quality: Int = 85
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadOrientedBitmap(sourceFile, maxWidth, maxHeight)
            bitmap?.let { bmp ->
                val outputStream = FileOutputStream(thumbnailFile)
                val compressed = bmp.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                outputStream.close()
                bmp.recycle()
                
                // Copy important EXIF data to thumbnail
                if (compressed) {
                    copyEssentialExifData(sourceFile, thumbnailFile)
                }
                
                compressed
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Copy essential EXIF data between files
     */
    private suspend fun copyEssentialExifData(
        sourceFile: File, 
        destinationFile: File
    ) = withContext(Dispatchers.IO) {
        try {
            val sourceExif = ExifInterface(sourceFile.absolutePath)
            val destExif = ExifInterface(destinationFile.absolutePath)
            
            // Copy essential tags (excluding GPS for privacy in thumbnails)
            val essentialTags = listOf(
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.TAG_SOFTWARE,
                ExifInterface.TAG_ARTIST,
                ExifInterface.TAG_COPYRIGHT
                // Note: Explicitly excluding GPS tags for thumbnail privacy
            )
            
            essentialTags.forEach { tag ->
                sourceExif.getAttribute(tag)?.let { value ->
                    destExif.setAttribute(tag, value)
                }
            }
            
            destExif.saveAttributes()
        } catch (e: IOException) {
            // Ignore EXIF copy failures - thumbnail is still valid
        }
    }
    
    /**
     * Batch process photos for thumbnail generation
     */
    suspend fun batchGenerateThumbnails(
        sourceFiles: List<File>,
        thumbnailDirectory: File,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onError: (File, Exception) -> Unit = { _, _ -> }
    ): List<File> = withContext(Dispatchers.IO) {
        val thumbnails = mutableListOf<File>()
        
        sourceFiles.forEachIndexed { index, sourceFile ->
            try {
                val thumbnailName = "thumb_${sourceFile.nameWithoutExtension}.jpg"
                val thumbnailFile = File(thumbnailDirectory, thumbnailName)
                
                if (generateThumbnail(sourceFile, thumbnailFile)) {
                    thumbnails.add(thumbnailFile)
                }
                
                onProgress(index + 1, sourceFiles.size)
            } catch (e: Exception) {
                onError(sourceFile, e)
            }
        }
        
        thumbnails
    }
    
    /**
     * Extract technical photography data for analysis
     */
    suspend fun extractTechnicalData(file: File): TechnicalPhotoData = withContext(Dispatchers.IO) {
        try {
            val exif = ExifInterface(file.absolutePath)
            
            TechnicalPhotoData(
                fileSize = file.length(),
                dimensions = extractDimensions(exif),
                colorSpace = exif.getAttribute(ExifInterface.TAG_COLOR_SPACE),
                compression = exif.getAttribute(ExifInterface.TAG_COMPRESSION),
                exposureData = ExposureData(
                    aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER),
                    shutterSpeed = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
                    iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS),
                    exposureMode = exif.getAttribute(ExifInterface.TAG_EXPOSURE_MODE),
                    meteringMode = exif.getAttribute(ExifInterface.TAG_METERING_MODE),
                    whiteBalance = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)
                ),
                lensData = LensData(
                    focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH),
                    focalLengthIn35mm = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM),
                    digitalZoomRatio = exif.getAttribute(ExifInterface.TAG_DIGITAL_ZOOM_RATIO)
                ),
                flashData = FlashData(
                    flash = exif.getAttribute(ExifInterface.TAG_FLASH),
                    flashEnergy = exif.getAttribute(ExifInterface.TAG_FLASH_ENERGY)
                )
            )
        } catch (e: Exception) {
            TechnicalPhotoData() // Return empty data if extraction fails
        }
    }
    
    private fun extractDimensions(exif: ExifInterface): Pair<Int, Int>? {
        val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
        val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
        
        return if (width > 0 && height > 0) {
            Pair(width, height)
        } else null
    }
    
    /**
     * Remove EXIF data from file (for privacy compliance)
     */
    suspend fun stripExifData(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val exif = ExifInterface(file.absolutePath)
            
            // Remove all location-related tags
            val locationTags = listOf(
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_GPS_SPEED,
                ExifInterface.TAG_GPS_SPEED_REF,
                ExifInterface.TAG_GPS_TRACK,
                ExifInterface.TAG_GPS_TRACK_REF
            )
            
            locationTags.forEach { tag ->
                exif.setAttribute(tag, null)
            }
            
            // Optionally remove camera identifying information
            val identifyingTags = listOf(
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_SOFTWARE,
                ExifInterface.TAG_ARTIST,
                ExifInterface.TAG_COPYRIGHT
            )
            
            identifyingTags.forEach { tag ->
                exif.setAttribute(tag, null)
            }
            
            exif.saveAttributes()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validate photo integrity and EXIF data
     */
    suspend fun validatePhotoIntegrity(file: File): PhotoValidationResult = withContext(Dispatchers.IO) {
        val issues = mutableListOf<ValidationIssue>()
        
        try {
            // Check file accessibility
            if (!file.exists()) {
                issues.add(ValidationIssue.FILE_NOT_FOUND)
            }
            
            if (!file.canRead()) {
                issues.add(ValidationIssue.FILE_NOT_READABLE)
            }
            
            // Check file size
            if (file.length() == 0L) {
                issues.add(ValidationIssue.EMPTY_FILE)
            }
            
            // Check if it's a valid image
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                issues.add(ValidationIssue.INVALID_IMAGE_FORMAT)
            }
            
            // Check EXIF data
            val exif = ExifInterface(file.absolutePath)
            
            // Validate orientation
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            if (orientation < ExifInterface.ORIENTATION_NORMAL || 
                orientation > ExifInterface.ORIENTATION_TRANSVERSE) {
                issues.add(ValidationIssue.INVALID_ORIENTATION)
            }
            
        } catch (e: Exception) {
            issues.add(ValidationIssue.PROCESSING_ERROR)
        }
        
        PhotoValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            fileSize = if (file.exists()) file.length() else 0L
        )
    }
}

/**
 * Technical photography data extracted from EXIF
 */
data class TechnicalPhotoData(
    val fileSize: Long = 0L,
    val dimensions: Pair<Int, Int>? = null,
    val colorSpace: String? = null,
    val compression: String? = null,
    val exposureData: ExposureData = ExposureData(),
    val lensData: LensData = LensData(),
    val flashData: FlashData = FlashData()
)

data class ExposureData(
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val iso: String? = null,
    val exposureMode: String? = null,
    val meteringMode: String? = null,
    val whiteBalance: String? = null
)

data class LensData(
    val focalLength: String? = null,
    val focalLengthIn35mm: String? = null,
    val digitalZoomRatio: String? = null
)

data class FlashData(
    val flash: String? = null,
    val flashEnergy: String? = null
)

/**
 * Photo validation result
 */
data class PhotoValidationResult(
    val isValid: Boolean,
    val issues: List<ValidationIssue>,
    val fileSize: Long
)

/**
 * Validation issues that can occur
 */
enum class ValidationIssue {
    FILE_NOT_FOUND,
    FILE_NOT_READABLE,
    EMPTY_FILE,
    INVALID_IMAGE_FORMAT,
    INVALID_ORIENTATION,
    PROCESSING_ERROR
}

/**
 * Extension functions for technical data formatting
 */
fun TechnicalPhotoData.getFormattedFileSize(): String {
    val kb = 1024
    val mb = kb * 1024
    val gb = mb * 1024
    
    return when {
        fileSize >= gb -> "${String.format("%.2f", fileSize.toDouble() / gb)} GB"
        fileSize >= mb -> "${String.format("%.2f", fileSize.toDouble() / mb)} MB"
        fileSize >= kb -> "${String.format("%.2f", fileSize.toDouble() / kb)} KB"
        else -> "$fileSize B"
    }
}

fun TechnicalPhotoData.getDimensionsString(): String? {
    return dimensions?.let { (width, height) -> "${width} × ${height} px" }
}

fun ExposureData.getFormattedAperture(): String? {
    return aperture?.let { "f/$it" }
}

fun ExposureData.getFormattedShutterSpeed(): String? {
    return shutterSpeed?.toDoubleOrNull()?.let { speed ->
        when {
            speed >= 1.0 -> "${speed.toInt()}s"
            else -> "1/${(1.0 / speed).toInt()}s"
        }
    }
}

fun ExposureData.getFormattedISO(): String? {
    return iso?.let { "ISO $it" }
}

fun LensData.getFormattedFocalLength(): String? {
    return focalLength?.let { "${it}mm" }
}