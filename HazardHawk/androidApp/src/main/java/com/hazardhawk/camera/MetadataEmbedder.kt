package com.hazardhawk.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.hazardhawk.security.SecureKeyManager
import com.hazardhawk.camera.MetadataSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MetadataEmbedder(private val context: android.content.Context) {

    // Access the same settings that the metadata overlay uses
    private val metadataSettingsManager by lazy { MetadataSettingsManager(context) }

    companion object {
        private const val TAG = "MetadataEmbedder"
        private const val WATERMARK_OPACITY = 180 // 0-255
        // Content-aware watermark sizing constants - BALANCED APPROACH
        private const val WATERMARK_SIZE_RATIO_OLD = 0.035f // Old oversized ratio for reference
        private const val WATERMARK_TARGET_VISIBILITY = 0.08f // Increased to 8% for better visibility
        private const val WATERMARK_MIN_TEXT_SIZE = 28f // Increased by 1 size (was 24f)
        private const val WATERMARK_MAX_TEXT_SIZE = 64f // Increased by 1 size (was 60f)
        private const val WATERMARK_PADDING = 20f
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16

        /**
         * Calculate optimal watermark text size based on content and image dimensions.
         * Implements content-aware sizing to replace fixed 3.5% ratio approach.
         *
         * @param imageWidth Width of the image in pixels
         * @param imageHeight Height of the image in pixels
         * @param contentLines Number of text lines in the watermark
         * @param targetVisibility Target percentage of image area for watermark (default 12%)
         * @return Optimal text size in pixels
         */
        fun calculateOptimalTextSize(
            imageWidth: Int,
            imageHeight: Int,
            contentLines: Int,
            targetVisibility: Float = WATERMARK_TARGET_VISIBILITY
        ): Float {
            // Calculate total image area
            val imageArea = imageWidth * imageHeight
            val targetWatermarkArea = imageArea * targetVisibility

            // EMERGENCY FIX: Much more conservative sizing calculation
            val lineSpacingMultiplier = 1.8f // More breathing room to reduce size
            val paddingOverhead = 1.6f // More overhead to account for padding
            val conservativeFactor = 0.6f // Additional 40% size reduction factor

            val availableAreaPerLine = targetWatermarkArea / (contentLines * lineSpacingMultiplier * paddingOverhead)

            // Much more conservative text size estimation with safety factor
            val estimatedTextSize = kotlin.math.sqrt(availableAreaPerLine / 12f) * conservativeFactor // Increased divisor + safety factor

            // Apply constraints for readability and aesthetics
            val constrainedSize = estimatedTextSize.coerceIn(WATERMARK_MIN_TEXT_SIZE, WATERMARK_MAX_TEXT_SIZE)

            // Log the calculation for debugging
            android.util.Log.d(TAG, "WATERMARK SIZING: Image ${imageWidth}x${imageHeight}, " +
                "lines: $contentLines, target: ${(targetVisibility * 100).toInt()}%, " +
                "calculated: ${constrainedSize.toInt()}px " +
                "(was ${(imageWidth * WATERMARK_SIZE_RATIO_OLD).toInt()}px with old method)")

            return constrainedSize
        }

        /**
         * Shared utility to create consistent metadata lines for both overlay and watermark
         * New 4-line format:
         * Line 1: Company
         * Line 2: Project
         * Line 3: GPS Coordinates or address (optional based on settings)
         * Line 4: "Taken with HazardHawk" (optional based on settings)
         */
        fun createMetadataLines(
            companyName: String?,
            projectName: String?,
            timestamp: String?,
            location: String?,
            showLocation: Boolean = true,
            showBranding: Boolean = true
        ): List<String> {
            val lines = mutableListOf<String>()

            // Line 1: Company name
            val company = companyName?.ifBlank { "HazardHawk" } ?: "HazardHawk"
            lines.add(company)

            // Line 2: Project name
            val project = projectName?.ifBlank { "Safety Documentation" } ?: "Safety Documentation"
            lines.add(project)

            // Line 3: GPS coordinates or location (if enabled and available)
            if (showLocation) {
                location?.let { loc ->
                    if (loc.isNotBlank() && loc != "Location unavailable") {
                        lines.add(loc)
                    }
                }
            }

            // Line 4: Branding watermark (if enabled)
            if (showBranding) {
                lines.add("Taken with HazardHawk")
            }

            return lines
        }
    }

    private val secureKeyManager by lazy {
        SecureKeyManager.getInstance(context)
    }
    
    /**
     * Embed metadata into photo EXIF data and optionally add visual watermark
     */
    suspend fun embedMetadata(
        photoFile: File,
        metadata: CaptureMetadata,
        addVisualWatermark: Boolean = true
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting metadata embedding for: ${photoFile.absolutePath}")
            
            // First, embed EXIF metadata
            embedExifMetadata(photoFile, metadata)
            
            // Then, optionally add visual watermark
            if (addVisualWatermark) {
                addVisualWatermark(photoFile, metadata)
            }
            
            Log.d(TAG, "Successfully embedded metadata in photo")
            Result.success(photoFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to embed metadata", e)
            Result.failure(e)
        }
    }
    
    /**
     * Embed metadata into EXIF data
     */
    private suspend fun embedExifMetadata(
        photoFile: File,
        metadata: CaptureMetadata
    ) = withContext(Dispatchers.IO) {
        try {
            val exif = ExifInterface(photoFile.absolutePath)
            
            // Timestamp
            val dateFormat = SimpleDateFormat("yyyy:MM:dd hh:mm:ss a", Locale.getDefault())
            val timestamp = dateFormat.format(Date(metadata.timestamp))
            exif.setAttribute(ExifInterface.TAG_DATETIME, timestamp)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, timestamp)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, timestamp)
            
            // GPS coordinates
            if (metadata.locationData.isAvailable) {
                val lat = metadata.locationData.latitude
                val lng = metadata.locationData.longitude
                val altitude = metadata.locationData.altitude
                
                // Set GPS coordinates
                exif.setLatLong(lat, lng)
                
                if (altitude > 0.0) {
                    exif.setAltitude(altitude)
                }
                
                // Set GPS timestamp
                exif.setAttribute(
                    ExifInterface.TAG_GPS_DATESTAMP,
                    SimpleDateFormat("yyyy:MM:dd", Locale.getDefault()).format(Date(metadata.timestamp))
                )
                exif.setAttribute(
                    ExifInterface.TAG_GPS_TIMESTAMP,
                    SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(metadata.timestamp))
                )
                
                // ORIENTATION FIX 3: Preserve existing EXIF orientation set by CameraX
                val existingOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                Log.d(TAG, "Orientation Fix 3: Preserving existing CameraX orientation: $existingOrientation")

                // Don't modify orientation - let CameraX's value stand
                if (existingOrientation != ExifInterface.ORIENTATION_UNDEFINED) {
                    Log.d(TAG, "Keeping CameraX orientation value: $existingOrientation")
                } else {
                    Log.w(TAG, "No CameraX orientation found, this may indicate an issue")
                }
            }
            
            // SECURITY ENHANCEMENT: Encrypt sensitive business data in EXIF
            // Public information for basic identification
            val publicImageDescription = "HazardHawk Safety Documentation - ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(metadata.timestamp))}"
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, publicImageDescription)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "HazardHawk Safety Camera")
            exif.setAttribute(ExifInterface.TAG_COPYRIGHT, "HazardHawk Safety Documentation © ${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}")

            // Encrypt sensitive business data for compliance
            val sensitiveData = buildString {
                append("HHSecure|") // Marker for encrypted data
                append("timestamp:${metadata.timestamp}|")
                if (metadata.projectName.isNotBlank()) {
                    append("project:${metadata.projectName}|")
                }
                if (metadata.projectId.isNotBlank()) {
                    append("projectId:${metadata.projectId}|")
                }
                if (metadata.userName.isNotBlank()) {
                    append("user:${metadata.userName}|")
                }
                if (metadata.userId.isNotBlank()) {
                    append("userId:${metadata.userId}|")
                }
                append("device:${metadata.deviceInfo}")
            }

            try {
                // Encrypt sensitive business data using AES-256-GCM
                val encryptedData = encryptSensitiveData(sensitiveData)
                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "HHSecure:$encryptedData")

                // Log security event for audit trail
                Log.i(TAG, "Sensitive metadata encrypted and embedded securely")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to encrypt sensitive metadata, using minimal data", e)
                // Fallback to minimal non-sensitive data
                val minimalComment = "HazardHawk|timestamp:${metadata.timestamp}|device:${android.os.Build.MODEL}"
                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, minimalComment)
            }

            // Set artist field with anonymized identifier instead of actual name
            val anonymizedArtist = if (metadata.userId.isNotBlank()) {
                "HH_User_${hashUserId(metadata.userId)}"
            } else {
                "HazardHawk User"
            }
            exif.setAttribute(ExifInterface.TAG_ARTIST, anonymizedArtist)
            
            // Save EXIF changes
            exif.saveAttributes()
            
            Log.d(TAG, "EXIF metadata embedded successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to embed EXIF metadata", e)
            throw e
        }
    }
    
    /**
     * Add visual watermark with metadata to the photo
     */
    private suspend fun addVisualWatermark(
        photoFile: File,
        metadata: CaptureMetadata
    ) = withContext(Dispatchers.IO) {
        try {
            // Load and apply EXIF orientation that CameraX saved - BitmapFactory ignores EXIF!
            val originalBitmap = loadBitmapWithCorrectOrientation(photoFile)
                ?: throw IllegalStateException("Could not decode image file")

            Log.d(TAG, "FINAL ORIENTATION FIX: Loaded bitmap with CameraX EXIF orientation applied - ${originalBitmap.width}x${originalBitmap.height}")

            // Apply standard aspect ratio cropping now that orientation is correct
            val aspectRatioCroppedBitmap = applyAspectRatioCropping(originalBitmap, metadata)

            // Create a mutable copy
            val watermarkedBitmap = aspectRatioCroppedBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(watermarkedBitmap)
            
            // Calculate sizes based on image dimensions using content-aware approach
            val imageWidth = watermarkedBitmap.width
            val imageHeight = watermarkedBitmap.height

            // Prepare watermark text lines first to get line count
            val watermarkLines = createWatermarkLines(metadata)

            // Use new content-aware text sizing
            val calculatedTextSize = calculateOptimalTextSize(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                contentLines = watermarkLines.size,
                targetVisibility = WATERMARK_TARGET_VISIBILITY
            )
            
            // Setup paint for watermark text
            val paint = Paint().apply {
                color = android.graphics.Color.WHITE
                alpha = 255  // Full opacity for pure white text
                textSize = calculatedTextSize
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                setShadowLayer(6f, 3f, 3f, android.graphics.Color.BLACK)  // Stronger shadow for better contrast
            }
            
            // Setup paint for background
            val backgroundPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                alpha = 120
            }
            
            // Note: watermarkLines already created above for sizing calculation

            // Calculate text dimensions
            val textBounds = Rect()
            val maxWidth = watermarkLines.maxOfOrNull { line ->
                paint.getTextBounds(line, 0, line.length, textBounds)
                textBounds.width()
            } ?: 0
            
            val lineHeight = paint.fontSpacing * 1.3f // Increase line spacing by 30% for better readability
            val totalTextHeight = lineHeight * watermarkLines.size
            val bottomPadding = 20f  // Add bottom padding for better spacing
            val topPadding = 20f     // Add top padding for balanced spacing
            val overlayHeight = totalTextHeight + topPadding + bottomPadding

            // Position watermark at bottom of photo with padding and center text vertically
            val x = WATERMARK_PADDING
            val overlayTop = imageHeight - overlayHeight
            val textStartY = overlayTop + topPadding + paint.textSize  // Center text vertically in overlay

            // Draw semi-transparent background for readability - full width with padding
            val backgroundRect = Rect(
                0, // Full width from left edge
                overlayTop.toInt(),
                imageWidth, // Full width to right edge
                imageHeight - bottomPadding.toInt() // Add bottom padding
            )
            canvas.drawRect(backgroundRect, backgroundPaint)
            
            // Draw watermark text lines centered vertically in overlay
            watermarkLines.forEachIndexed { index, line ->
                canvas.drawText(
                    line,
                    x,
                    textStartY + (lineHeight * index),
                    paint
                )
            }
            
            // ORIENTATION FIX: Preserve all important EXIF data before overwriting with watermarked bitmap
            val originalExif = ExifInterface(photoFile.absolutePath)
            val preservedData = mapOf(
                ExifInterface.TAG_ORIENTATION to originalExif.getAttribute(ExifInterface.TAG_ORIENTATION),
                ExifInterface.TAG_DATETIME to originalExif.getAttribute(ExifInterface.TAG_DATETIME),
                ExifInterface.TAG_GPS_LATITUDE to originalExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE),
                ExifInterface.TAG_GPS_LONGITUDE to originalExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE),
                ExifInterface.TAG_GPS_LATITUDE_REF to originalExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF),
                ExifInterface.TAG_GPS_LONGITUDE_REF to originalExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF),
                ExifInterface.TAG_MAKE to originalExif.getAttribute(ExifInterface.TAG_MAKE),
                ExifInterface.TAG_MODEL to originalExif.getAttribute(ExifInterface.TAG_MODEL)
            )

            Log.d(TAG, "ORIENTATION FIX: Preserving EXIF orientation ${preservedData[ExifInterface.TAG_ORIENTATION]} before watermark save")

            // Save the watermarked image
            FileOutputStream(photoFile).use { outputStream ->
                watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }

            // ORIENTATION FIX: Restore all preserved EXIF data after bitmap save
            val newExif = ExifInterface(photoFile.absolutePath)
            preservedData.forEach { (tag, value) ->
                if (value != null) {
                    newExif.setAttribute(tag, value)
                }
            }
            newExif.saveAttributes()

            Log.d(TAG, "ORIENTATION FIX: Restored EXIF data including orientation ${preservedData[ExifInterface.TAG_ORIENTATION]} after watermark save")
            
            // Cleanup bitmaps
            originalBitmap.recycle()
            if (aspectRatioCroppedBitmap != originalBitmap) {
                aspectRatioCroppedBitmap.recycle()
            }
            watermarkedBitmap.recycle()
            
            Log.d(TAG, "Visual watermark added successfully with correct orientation")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add visual watermark", e)
            throw e
        }
    }
    
    /**
     * Apply aspect ratio cropping without rotation - CameraX already handles orientation
     * This ensures the saved image matches what the user saw in the viewfinder
     */
    private fun applyAspectRatioCroppingNoRotation(bitmap: Bitmap, metadata: CaptureMetadata): Bitmap {
        return try {
            // Check if aspect ratio information is available in metadata
            val targetAspectRatio = extractAspectRatioFromMetadata(metadata)
            if (targetAspectRatio == null) {
                Log.d(TAG, "Orientation Fix 4: No aspect ratio info in metadata, returning original bitmap")
                return bitmap
            }

            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            val currentAspectRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()

            Log.d(TAG, "Orientation Fix 4: NoRotation cropping: ${bitmapWidth}x${bitmapHeight}, current ratio: $currentAspectRatio, target: $targetAspectRatio")

            // If aspect ratios are very close, no cropping needed
            if (kotlin.math.abs(currentAspectRatio - targetAspectRatio) < 0.01f) {
                Log.d(TAG, "Aspect ratios match (${currentAspectRatio} ≈ ${targetAspectRatio}), no cropping needed")
                return bitmap
            }

            val (cropWidth, cropHeight) = if (targetAspectRatio > currentAspectRatio) {
                // Target is wider - crop height
                val newHeight = (bitmapWidth / targetAspectRatio).toInt()
                bitmapWidth to newHeight.coerceAtMost(bitmapHeight)
            } else {
                // Target is taller - crop width
                val newWidth = (bitmapHeight * targetAspectRatio).toInt()
                newWidth.coerceAtMost(bitmapWidth) to bitmapHeight
            }

            // Calculate crop position (center crop)
            val cropX = (bitmapWidth - cropWidth) / 2
            val cropY = (bitmapHeight - cropHeight) / 2

            Log.d(TAG, "Orientation Fix 4: NoRotation cropping from ${bitmapWidth}x${bitmapHeight} to ${cropWidth}x${cropHeight}")

            // Create cropped bitmap
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                cropX,
                cropY,
                cropWidth,
                cropHeight
            )

            croppedBitmap

        } catch (e: Exception) {
            Log.e(TAG, "Orientation Fix 4: Failed to apply NoRotation aspect ratio cropping", e)
            bitmap // Return original on error
        }
    }

    /**
     * Apply aspect ratio cropping to match the viewfinder selection
     * This ensures the saved image matches what the user saw in the viewfinder
     */
    private fun applyAspectRatioCropping(bitmap: Bitmap, metadata: CaptureMetadata): Bitmap {
        return try {
            // Check if aspect ratio information is available in metadata
            val targetAspectRatio = extractAspectRatioFromMetadata(metadata)
            if (targetAspectRatio == null) {
                Log.d(TAG, "No aspect ratio info in metadata, returning original bitmap")
                return bitmap
            }

            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            val currentAspectRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()

            // Determine if the image is in portrait orientation (taller than wide)
            val isPortrait = bitmapHeight > bitmapWidth

            // Adjust target aspect ratio based on image orientation
            val adjustedTargetRatio = if (isPortrait && targetAspectRatio > 1.0f) {
                // If image is portrait but target ratio is landscape (>1), invert the ratio
                1.0f / targetAspectRatio
            } else if (!isPortrait && targetAspectRatio < 1.0f) {
                // If image is landscape but target ratio is portrait (<1), invert the ratio
                1.0f / targetAspectRatio
            } else {
                targetAspectRatio
            }

            // If aspect ratios are very close, no cropping needed
            if (kotlin.math.abs(currentAspectRatio - adjustedTargetRatio) < 0.01f) {
                Log.d(TAG, "Aspect ratios match (${currentAspectRatio} ≈ ${adjustedTargetRatio}), no cropping needed")
                return bitmap
            }

            val (cropWidth, cropHeight) = if (adjustedTargetRatio > currentAspectRatio) {
                // Target is wider - crop height
                val newHeight = (bitmapWidth / adjustedTargetRatio).toInt()
                bitmapWidth to newHeight.coerceAtMost(bitmapHeight)
            } else {
                // Target is taller - crop width
                val newWidth = (bitmapHeight * adjustedTargetRatio).toInt()
                newWidth.coerceAtMost(bitmapWidth) to bitmapHeight
            }

            // Calculate crop position (center crop)
            val cropX = (bitmapWidth - cropWidth) / 2
            val cropY = (bitmapHeight - cropHeight) / 2

            Log.d(TAG, "Cropping ${if (isPortrait) "portrait" else "landscape"} image from ${bitmapWidth}x${bitmapHeight} to ${cropWidth}x${cropHeight}")
            Log.d(TAG, "Original ratio: $currentAspectRatio, Target: $targetAspectRatio, Adjusted: $adjustedTargetRatio")

            // Create cropped bitmap
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                cropX,
                cropY,
                cropWidth,
                cropHeight
            )

            croppedBitmap

        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply aspect ratio cropping", e)
            bitmap // Return original on error
        }
    }

    /**
     * Extract target aspect ratio from metadata
     */
    private fun extractAspectRatioFromMetadata(metadata: CaptureMetadata): Float? {
        return try {
            // Look for aspect ratio info in device info field
            val deviceInfo = metadata.deviceInfo
            when {
                // New format from camera settings
                deviceInfo.contains("AspectRatio: 1:1") -> 1.0f
                deviceInfo.contains("AspectRatio: 4:3") -> 4.0f / 3.0f
                deviceInfo.contains("AspectRatio: 16:9") -> 16.0f / 9.0f
                // Legacy format from viewfinder aspect ratio labels
                deviceInfo.contains("SQUARE") || deviceInfo.contains("1:1") -> 1.0f
                deviceInfo.contains("FOUR_THREE") || deviceInfo.contains("4:3") -> 4.0f / 3.0f
                deviceInfo.contains("SIXTEEN_NINE") || deviceInfo.contains("16:9") -> 16.0f / 9.0f
                else -> {
                    Log.d(TAG, "No recognized aspect ratio in device info: $deviceInfo")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract aspect ratio from metadata", e)
            null
        }
    }

    /**
     * Load bitmap with correct orientation applied from EXIF data.
     * CRITICAL: BitmapFactory.decodeFile() ignores EXIF orientation!
     * CameraX saves correct EXIF orientation, but we must manually apply it during bitmap processing.
     */
    private fun loadBitmapWithCorrectOrientation(photoFile: File): Bitmap? {
        return try {
            // Read EXIF orientation that CameraX correctly set
            val exif = ExifInterface(photoFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            Log.d(TAG, "ORIENTATION FIX: CameraX saved EXIF orientation: $orientation")

            // Decode the bitmap (this ignores EXIF orientation)
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: return null

            // Apply rotation based on EXIF orientation that CameraX set
            val matrix = android.graphics.Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    Log.d(TAG, "ORIENTATION FIX: Applying 90° rotation for bitmap processing")
                    matrix.postRotate(90f)
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    Log.d(TAG, "ORIENTATION FIX: Applying 180° rotation for bitmap processing")
                    matrix.postRotate(180f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    Log.d(TAG, "ORIENTATION FIX: Applying 270° rotation for bitmap processing")
                    matrix.postRotate(270f)
                }
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(-90f)
                    matrix.postScale(-1f, 1f)
                }
                else -> {
                    Log.d(TAG, "ORIENTATION FIX: No rotation needed, using bitmap as-is")
                    return bitmap // ORIENTATION_NORMAL or ORIENTATION_UNDEFINED
                }
            }

            // Create rotated bitmap for watermark processing
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )

            Log.d(TAG, "ORIENTATION FIX: Bitmap rotated from ${bitmap.width}x${bitmap.height} to ${rotatedBitmap.width}x${rotatedBitmap.height}")

            // Recycle original if it's different from rotated
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }

            rotatedBitmap

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap with correct orientation", e)
            // Fallback to standard decoding
            BitmapFactory.decodeFile(photoFile.absolutePath)
        }
    }

    /**
     * Create watermark text lines from metadata - EXACTLY matching metadata overlay format
     * Uses same data sources and settings as SafetyHUDCameraScreen metadata overlay
     */
    private fun createWatermarkLines(metadata: CaptureMetadata): List<String> {
        // Get the same settings and data sources that the metadata overlay uses
        val userProfile = metadataSettingsManager.userProfile.value
        val currentProject = metadataSettingsManager.currentProject.value
        val appSettings = metadataSettingsManager.appSettings.value

        Log.d(TAG, "WATERMARK SYNC: Creating watermark lines to match overlay")
        Log.d(TAG, "  ├─ userProfile.company: '${userProfile.company}'")
        Log.d(TAG, "  ├─ currentProject.projectName: '${currentProject.projectName}'")
        Log.d(TAG, "  ├─ showGPSCoordinates: ${appSettings.dataPrivacy.showGPSCoordinates}")
        Log.d(TAG, "  ├─ includeLocation: ${appSettings.dataPrivacy.includeLocation}")
        Log.d(TAG, "  └─ locationData available: ${metadata.locationData.isAvailable}")

        // Company and project names
        val companyName = userProfile.company.ifBlank { "HazardHawk" }
        val projectName = currentProject.projectName.ifBlank { "Safety Documentation" }

        // Format timestamp (12-hour format with AM/PM)
        val timestamp = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault()).format(Date(metadata.timestamp))

        // Determine location display based on settings
        val location = if (metadata.locationData.isAvailable) {
            if (appSettings.dataPrivacy.showGPSCoordinates) {
                // Show coordinates when setting is enabled
                "${String.format("%.6f", metadata.locationData.latitude)}, ${String.format("%.6f", metadata.locationData.longitude)}"
            } else {
                // Show address when setting is disabled, fallback to coordinates if no address
                metadata.locationData.address.ifBlank {
                    "${String.format("%.6f", metadata.locationData.latitude)}, ${String.format("%.6f", metadata.locationData.longitude)}"
                }
            }
        } else {
            null // Don't show location if unavailable
        }

        // Build watermark lines with timestamp on same line as company
        val lines = mutableListOf<String>()

        // Line 1: Company name | Timestamp
        lines.add("$companyName | $timestamp")

        // Line 2: Project name
        lines.add(projectName)

        // Line 3: GPS coordinates or location (if enabled and available)
        if (location != null && appSettings.dataPrivacy.includeLocation) {
            lines.add(location)
        }

        // Line 4: Branding watermark (always show)
        lines.add("Taken with HazardHawk")

        return lines
    }

    
    /**
     * Extract metadata from EXIF data of an existing photo
     */
    suspend fun extractMetadataFromPhoto(photoFile: File): CaptureMetadata? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "EXIF EXTRACTION: Starting extraction for ${photoFile.name}")
            val exif = ExifInterface(photoFile.absolutePath)

            // Extract timestamp
            val timestampStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                ?: return@withContext null

            val dateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            val timestamp = dateFormat.parse(timestampStr)?.time ?: System.currentTimeMillis()

            // Extract GPS coordinates
            val latLng = exif.latLong
            val altitude = exif.getAltitude(0.0)
            Log.d(TAG, "EXIF EXTRACTION: GPS coordinates = $latLng, altitude = $altitude")
            
            val locationData = if (latLng != null) {
                // Note: We can't recreate the address without reverse geocoding
                LocationData(
                    latitude = latLng[0],
                    longitude = latLng[1],
                    altitude = altitude,
                    timestamp = timestamp,
                    isAvailable = true,
                    address = "${String.format("%.6f", latLng[0])}, ${String.format("%.6f", latLng[1])}"
                )
            } else {
                LocationData()
            }
            
            // Extract user comment with structured data (handling encrypted data)
            val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT) ?: ""
            Log.d(TAG, "EXIF EXTRACTION: User comment = '$userComment' (${userComment.length} chars)")

            val (projectName, projectId, userName, userId, deviceInfo) = if (userComment.startsWith("HHSecure:")) {
                try {
                    // Decrypt secure data
                    val encryptedData = userComment.removePrefix("HHSecure:")
                    val decryptedData = decryptSensitiveData(encryptedData)

                    Quintet(
                        extractValueFromComment(decryptedData, "project"),
                        extractValueFromComment(decryptedData, "projectId"),
                        extractValueFromComment(decryptedData, "user"),
                        extractValueFromComment(decryptedData, "userId"),
                        extractValueFromComment(decryptedData, "device")
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decrypt metadata, using fallback", e)
                    Quintet("", "", "", "", "")
                }
            } else {
                // Legacy unencrypted data
                Quintet(
                    extractValueFromComment(userComment, "project"),
                    extractValueFromComment(userComment, "projectId"),
                    extractValueFromComment(userComment, "user"),
                    extractValueFromComment(userComment, "userId"),
                    extractValueFromComment(userComment, "device")
                )
            }
            
            val result = CaptureMetadata(
                timestamp = timestamp,
                locationData = locationData,
                projectName = projectName,
                projectId = projectId,
                userName = userName,
                userId = userId,
                deviceInfo = deviceInfo
            )

            Log.d(TAG, "EXIF EXTRACTION: Result - GPS available: ${result.locationData.isAvailable}, " +
                "project: '${result.projectName}', location: ${result.locationData.latitude}, ${result.locationData.longitude}")

            result
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract metadata from photo", e)
            null
        }
    }
    
    /**
     * Extract value from structured comment string
     */
    private fun extractValueFromComment(comment: String, key: String): String {
        val pattern = "$key:([^|]*)".toRegex()
        return pattern.find(comment)?.groupValues?.get(1)?.trim() ?: ""
    }
    
    /**
     * Validate that a file is a valid JPEG image
     */
    fun isValidJpegFile(file: File): Boolean {
        return try {
            file.exists() && file.extension.lowercase() == "jpg" &&
            BitmapFactory.decodeFile(file.absolutePath) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * SECURITY: Encrypt sensitive data using AES-256-GCM
     */
    private fun encryptSensitiveData(data: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val encryptionKey = getOrCreateMetadataEncryptionKey()
            val secretKey = SecretKeySpec(
                android.util.Base64.decode(encryptionKey, android.util.Base64.NO_WRAP).take(32).toByteArray(),
                "AES"
            )

            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data.toByteArray())

            // Combine IV and encrypted data, then base64 encode
            val combined = iv + encryptedData
            android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt sensitive data", e)
            throw SecurityException("Metadata encryption failed", e)
        }
    }

    /**
     * SECURITY: Decrypt sensitive data using AES-256-GCM
     */
    private fun decryptSensitiveData(encryptedData: String): String {
        return try {
            val combined = android.util.Base64.decode(encryptedData, android.util.Base64.NO_WRAP)
            val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
            val cipherText = combined.sliceArray(GCM_IV_LENGTH until combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val encryptionKey = getOrCreateMetadataEncryptionKey()
            val secretKey = SecretKeySpec(
                android.util.Base64.decode(encryptionKey, android.util.Base64.NO_WRAP).take(32).toByteArray(),
                "AES"
            )
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(cipherText)

            String(decryptedBytes)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt sensitive data", e)
            throw SecurityException("Metadata decryption failed", e)
        }
    }

    /**
     * Get or create encryption key for metadata
     */
    private fun getOrCreateMetadataEncryptionKey(): String {
        val keyName = "metadata_encryption_key"
        return secureKeyManager.getGenericData(keyName) ?: run {
            val newKey = secureKeyManager.generateEncryptionKey()
            secureKeyManager.storeGenericData(keyName, newKey)
            newKey
        }
    }

    /**
     * Process photo in-place to add metadata overlay and EXIF data
     * Works with MediaStore URIs to avoid file duplication
     */
    suspend fun processPhotoInPlace(
        photoUri: android.net.Uri,
        metadata: CaptureMetadata,
        addVisualWatermark: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing photo in-place: $photoUri")

            // Get file path to properly read EXIF orientation
            val filePath = getFilePathFromUri(photoUri)
            val originalBitmap = if (filePath != null) {
                // Load with proper EXIF orientation handling
                loadBitmapWithCorrectOrientation(File(filePath))
                    ?: return@withContext Result.failure(IllegalStateException("Could not read photo with orientation"))
            } else {
                // Fallback to stream reading (but this won't have orientation correction)
                context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                } ?: return@withContext Result.failure(IllegalStateException("Could not read photo from URI"))
            }

            Log.d(TAG, "FINAL ORIENTATION FIX: MediaStore bitmap with EXIF orientation applied: ${originalBitmap.width}x${originalBitmap.height}")

            // Apply visual watermark if requested - now using proper orientation
            val processedBitmap = if (addVisualWatermark) {
                addWatermarkToBitmap(originalBitmap, metadata)
            } else {
                originalBitmap
            }

            // Write the processed bitmap back to the same URI
            context.contentResolver.openOutputStream(photoUri)?.use { outputStream ->
                val compressionQuality = 95 // High quality to preserve image
                if (!processedBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputStream)) {
                    return@withContext Result.failure(IllegalStateException("Failed to compress processed image"))
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open output stream for URI"))

            // Now update EXIF metadata directly on the MediaStore file
            // Reuse the file path we already have
            if (filePath != null) {
                try {
                    embedExifMetadata(File(filePath), metadata)
                    Log.d(TAG, "EXIF metadata embedded successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to embed EXIF metadata: ${e.message}")
                }
            } else {
                Log.w(TAG, "Could not get file path for EXIF metadata embedding")
            }

            // Clean up bitmaps
            if (processedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            processedBitmap.recycle()

            Log.d(TAG, "Successfully processed photo in-place")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process photo in-place", e)
            Result.failure(e)
        }
    }

    /**
     * Get file path from MediaStore URI for EXIF access
     */
    private fun getFilePathFromUri(uri: android.net.Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.Images.Media.DATA), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA))
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not get file path from URI: ${e.message}")
            null
        }
    }

    /**
     * Apply watermark to bitmap without rotation - CameraX already handles orientation
     * This method doesn't apply EXIF rotation to avoid double rotation
     */
    private fun addWatermarkToBitmapNoRotation(originalBitmap: Bitmap, metadata: CaptureMetadata): Bitmap {
        Log.d(TAG, "🖼️ Orientation Fix 4: Processing bitmap with CameraX orientation already applied: ${originalBitmap.width}x${originalBitmap.height}")

        // Apply aspect ratio cropping first using existing method but without rotation
        val croppedBitmap = applyAspectRatioCroppingNoRotation(originalBitmap, metadata)
        Log.d(TAG, "🖼️ Orientation Fix 4: After NoRotation aspect ratio cropping: ${croppedBitmap.width}x${croppedBitmap.height}")

        // Create a mutable copy of the cropped bitmap
        val mutableBitmap = croppedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val imageWidth = mutableBitmap.width
        val imageHeight = mutableBitmap.height

        // Prepare watermark text lines first to get line count for content-aware sizing
        val watermarkLines = createWatermarkLines(metadata)

        // Use new content-aware text sizing
        val calculatedTextSize = calculateOptimalTextSize(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            contentLines = watermarkLines.size,
            targetVisibility = WATERMARK_TARGET_VISIBILITY
        )

        // Setup paint for watermark text
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            alpha = 255
            textSize = calculatedTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(6f, 3f, 3f, android.graphics.Color.BLACK)
        }

        // Setup paint for background
        val backgroundPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            alpha = 120
        }

        // Note: watermarkLines already created above for content-aware sizing

        // Calculate text dimensions
        val textBounds = Rect()
        val maxWidth = watermarkLines.maxOfOrNull { line ->
            paint.getTextBounds(line, 0, line.length, textBounds)
            textBounds.width()
        } ?: 0

        val lineHeight = paint.fontSpacing
        val totalTextHeight = lineHeight * watermarkLines.size
        val bottomPadding = 16f

        // Calculate background position - flush to bottom edge
        val overlayTop = imageHeight - totalTextHeight - bottomPadding
        val backgroundRect = Rect(
            0,
            overlayTop.toInt(),
            imageWidth,
            imageHeight
        )

        // Draw background rectangle
        canvas.drawRect(backgroundRect, backgroundPaint)

        // Draw text lines
        val textPadding = 20f
        var currentY = overlayTop + paint.textSize
        for (line in watermarkLines) {
            canvas.drawText(line, textPadding, currentY, paint)
            currentY += lineHeight
        }

        // Clean up cropped bitmap if it's different from original
        if (croppedBitmap != originalBitmap) {
            croppedBitmap.recycle()
        }

        return mutableBitmap
    }

    /**
     * Apply watermark to bitmap without writing to file
     * Uses NoRotation cropping logic to trust CameraX orientation
     */
    private fun addWatermarkToBitmap(originalBitmap: Bitmap, metadata: CaptureMetadata): Bitmap {
        Log.d(TAG, "🖼️ ORIENTATION FIX: Processing bitmap with CameraX orientation already applied: ${originalBitmap.width}x${originalBitmap.height}")

        // Apply standard aspect ratio cropping - orientation is already correct
        val croppedBitmap = applyAspectRatioCropping(originalBitmap, metadata)
        Log.d(TAG, "🖼️ FINAL ORIENTATION FIX: After proper aspect ratio cropping: ${croppedBitmap.width}x${croppedBitmap.height}")

        // Create a mutable copy of the cropped bitmap
        val mutableBitmap = croppedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val imageWidth = mutableBitmap.width
        val imageHeight = mutableBitmap.height

        // Prepare watermark text lines first to get line count for content-aware sizing
        val watermarkLines = createWatermarkLines(metadata)

        // Use new content-aware text sizing instead of fixed percentage
        val calculatedTextSize = calculateOptimalTextSize(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            contentLines = watermarkLines.size,
            targetVisibility = WATERMARK_TARGET_VISIBILITY
        )

        // Setup paint for watermark text (match existing styling)
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            alpha = 255  // Full opacity for pure white text
            textSize = calculatedTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(6f, 3f, 3f, android.graphics.Color.BLACK)  // Stronger shadow for better contrast
        }

        // Setup paint for background
        val backgroundPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            alpha = 120
        }

        // Note: watermarkLines already created above for content-aware sizing

        // Calculate text dimensions
        val textBounds = Rect()
        val maxWidth = watermarkLines.maxOfOrNull { line ->
            paint.getTextBounds(line, 0, line.length, textBounds)
            textBounds.width()
        } ?: 0

        val lineHeight = paint.fontSpacing
        val totalTextHeight = lineHeight * watermarkLines.size
        val bottomPadding = 16f  // Add bottom padding for better spacing

        // Calculate background position - flush to bottom edge
        val overlayTop = imageHeight - totalTextHeight - bottomPadding
        val backgroundRect = Rect(
            0, // Full width from left edge
            overlayTop.toInt(),
            imageWidth, // Full width to right edge
            imageHeight // Flush to bottom edge - no gap
        )

        // Draw background rectangle
        canvas.drawRect(backgroundRect, backgroundPaint)

        // Draw text lines
        val textPadding = 20f
        var currentY = overlayTop + paint.textSize
        for (line in watermarkLines) {
            canvas.drawText(line, textPadding, currentY, paint)
            currentY += lineHeight
        }

        // Clean up cropped bitmap if it's different from original
        if (croppedBitmap != originalBitmap) {
            croppedBitmap.recycle()
        }

        return mutableBitmap
    }

    /**
     * Hash user ID for anonymization
     */
    private fun hashUserId(userId: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(userId.toByteArray())
            android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP).take(8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hash user ID", e)
            "ANON"
        }
    }

    /**
     * Batch reprocess photos to add timestamp overlays
     * Useful for fixing photos that were taken without timestamp watermarks
     *
     * @param photoFilePaths List of file paths to reprocess
     * @param onProgress Callback for progress updates (current, total, filePath)
     * @return Result with number of successfully processed photos
     */
    suspend fun batchReprocessPhotosWithTimestamp(
        photoFilePaths: List<String>,
        onProgress: ((current: Int, total: Int, filePath: String) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var successCount = 0
            val total = photoFilePaths.size

            photoFilePaths.forEachIndexed { index, filePath ->
                try {
                    Log.d(TAG, "Reprocessing photo ${index + 1}/$total: $filePath")
                    onProgress?.invoke(index + 1, total, filePath)

                    val photoFile = File(filePath)
                    if (!photoFile.exists()) {
                        Log.w(TAG, "Photo file does not exist: $filePath")
                        return@forEachIndexed
                    }

                    // Extract existing metadata from EXIF
                    val existingMetadata = extractMetadataFromPhoto(photoFile)
                    if (existingMetadata == null) {
                        Log.w(TAG, "Could not extract metadata from photo: $filePath")
                        // Create minimal metadata from file timestamp
                        val minimalMetadata = CaptureMetadata(
                            timestamp = photoFile.lastModified(),
                            locationData = LocationData(),
                            projectName = "",
                            projectId = "",
                            userName = "",
                            userId = "",
                            deviceInfo = ""
                        )

                        // Reprocess with minimal metadata directly on file
                        val result = reprocessPhotoFile(photoFile, minimalMetadata)

                        if (result.isSuccess) {
                            successCount++
                            Log.d(TAG, "Successfully reprocessed photo with minimal metadata: $filePath")
                        }
                    } else {
                        // Reprocess with existing metadata to add timestamp overlay
                        val result = reprocessPhotoFile(photoFile, existingMetadata)

                        if (result.isSuccess) {
                            successCount++
                            Log.d(TAG, "Successfully reprocessed photo: $filePath")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reprocess photo $filePath", e)
                }
            }

            Log.d(TAG, "Batch reprocessing complete: $successCount/$total photos successful")
            Result.success(successCount)

        } catch (e: Exception) {
            Log.e(TAG, "Batch reprocessing failed", e)
            Result.failure(e)
        }
    }

    /**
     * Reprocess a photo file directly to add watermark
     */
    private suspend fun reprocessPhotoFile(
        photoFile: File,
        metadata: CaptureMetadata
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Reprocessing file: ${photoFile.absolutePath}")

            // Load bitmap with correct orientation
            val originalBitmap = loadBitmapWithCorrectOrientation(photoFile)
                ?: return@withContext Result.failure(IllegalStateException("Could not load bitmap"))

            // Add watermark
            val watermarkedBitmap = addWatermarkToBitmap(originalBitmap, metadata)

            // Preserve EXIF data before overwriting
            val originalExif = ExifInterface(photoFile.absolutePath)
            val preservedData = mapOf(
                ExifInterface.TAG_ORIENTATION to originalExif.getAttribute(ExifInterface.TAG_ORIENTATION),
                ExifInterface.TAG_DATETIME to originalExif.getAttribute(ExifInterface.TAG_DATETIME),
                ExifInterface.TAG_GPS_LATITUDE to originalExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE),
                ExifInterface.TAG_GPS_LONGITUDE to originalExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE),
                ExifInterface.TAG_GPS_LATITUDE_REF to originalExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF),
                ExifInterface.TAG_GPS_LONGITUDE_REF to originalExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF),
                ExifInterface.TAG_MAKE to originalExif.getAttribute(ExifInterface.TAG_MAKE),
                ExifInterface.TAG_MODEL to originalExif.getAttribute(ExifInterface.TAG_MODEL)
            )

            // Write watermarked bitmap back to file
            FileOutputStream(photoFile).use { outputStream ->
                watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }

            // Restore EXIF data
            val newExif = ExifInterface(photoFile.absolutePath)
            preservedData.forEach { (tag, value) ->
                if (value != null) {
                    newExif.setAttribute(tag, value)
                }
            }
            newExif.saveAttributes()

            // Clean up bitmaps
            watermarkedBitmap.recycle()

            Log.d(TAG, "Successfully reprocessed file: ${photoFile.name}")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to reprocess file", e)
            Result.failure(e)
        }
    }
}

/**
 * Helper data class for handling five values from metadata extraction
 */
private data class Quintet<T1, T2, T3, T4, T5>(
    val first: T1,
    val second: T2,
    val third: T3,
    val fourth: T4,
    val fifth: T5
)