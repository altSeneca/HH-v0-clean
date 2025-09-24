package com.hazardhawk.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlin.random.Random

/**
 * Centralized photo orientation management for HazardHawk.
 *
 * Consolidates scattered orientation logic from MetadataEmbedder and PhotoExifExtractor
 * into a single, reliable service with fallback detection and legal compliance features.
 *
 * Features:
 * - EXIF-based orientation correction
 * - Fallback orientation detection when EXIF is corrupted/missing
 * - Photo integrity verification for legal documentation
 * - Memory-efficient bitmap processing
 * - Comprehensive error handling
 */
class PhotoOrientationManager {

    companion object {
        private const val TAG = "PhotoOrientationManager"

        // Singleton instance
        @Volatile
        private var INSTANCE: PhotoOrientationManager? = null

        fun getInstance(): PhotoOrientationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PhotoOrientationManager().also { INSTANCE = it }
            }
        }
    }

    /**
     * Photo orientation enumeration with all possible EXIF values
     */
    enum class PhotoOrientation(val exifValue: Int, val degrees: Float, val flipHorizontal: Boolean = false, val flipVertical: Boolean = false) {
        NORMAL(ExifInterface.ORIENTATION_NORMAL, 0f),
        FLIP_HORIZONTAL(ExifInterface.ORIENTATION_FLIP_HORIZONTAL, 0f, flipHorizontal = true),
        ROTATE_180(ExifInterface.ORIENTATION_ROTATE_180, 180f),
        FLIP_VERTICAL(ExifInterface.ORIENTATION_FLIP_VERTICAL, 180f, flipHorizontal = true),
        TRANSPOSE(ExifInterface.ORIENTATION_TRANSPOSE, 90f, flipHorizontal = true),
        ROTATE_90(ExifInterface.ORIENTATION_ROTATE_90, 90f),
        TRANSVERSE(ExifInterface.ORIENTATION_TRANSVERSE, -90f, flipHorizontal = true),
        ROTATE_270(ExifInterface.ORIENTATION_ROTATE_270, -90f);

        companion object {
            fun fromExifValue(exifValue: Int): PhotoOrientation {
                return values().find { it.exifValue == exifValue } ?: NORMAL
            }
        }
    }

    /**
     * Orientation source enumeration for tracking how orientation was determined
     */
    enum class OrientationSource {
        EXIF,              // Read from EXIF metadata
        PIXEL_ANALYSIS,    // Detected from image content
        MANUAL_CORRECTION, // User-corrected
        FALLBACK          // Default when detection fails
    }

    /**
     * Result of orientation analysis
     */
    data class OrientationResult(
        val orientation: PhotoOrientation,
        val source: OrientationSource,
        val confidence: Float = 1.0f,
        val validated: Boolean = false,
        val integrityHash: String? = null,
        val errorMessage: String? = null
    )

    /**
     * Load bitmap with correct orientation applied from EXIF data.
     * CRITICAL FIX: BitmapFactory.decodeFile() ignores EXIF orientation!
     * CameraX saves correct EXIF but we must manually apply it for bitmap processing.
     *
     * @param photoFile The photo file to load
     * @param preserveOriginal If true, keep original file integrity for legal compliance
     * @return Bitmap with CameraX EXIF orientation properly applied
     */
    fun loadBitmapWithCorrectOrientation(
        photoFile: File,
        preserveOriginal: Boolean = true
    ): Bitmap? {
        return try {
            Log.d(TAG, "ORIENTATION FIX: Reading CameraX EXIF orientation and applying to bitmap")

            val orientationResult = analyzeOrientation(photoFile)
            Log.d(TAG, "ORIENTATION FIX: CameraX saved orientation ${orientationResult.orientation}")

            // Decode the bitmap (this ignores EXIF orientation)
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: return null

            // Apply CameraX orientation correction manually for bitmap processing
            applyOrientationToBitmap(bitmap, orientationResult.orientation)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap with correct orientation", e)
            // Fallback to standard decoding
            BitmapFactory.decodeFile(photoFile.absolutePath)
        }
    }

    /**
     * Load bitmap from InputStream - trust CameraX EXIF handling.
     * Used for content:// URIs and other stream sources.
     * No manual rotation needed as CameraX already handled orientation.
     */
    fun loadBitmapWithCorrectOrientation(
        inputStream: InputStream,
        exifOrientation: Int = ExifInterface.ORIENTATION_NORMAL
    ): Bitmap? {
        return try {
            Log.d(TAG, "ORIENTATION FIX: Loading bitmap from stream without manual rotation - trusting CameraX EXIF orientation")

            // Simply decode the bitmap - CameraX already handled orientation via EXIF
            val bitmap = BitmapFactory.decodeStream(inputStream)

            if (bitmap != null) {
                Log.d(TAG, "ORIENTATION FIX: Stream bitmap loaded successfully: ${bitmap.width}x${bitmap.height}")
            }

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from stream", e)
            null
        }
    }

    /**
     * Comprehensive orientation analysis with fallback detection.
     *
     * @param photoFile The photo file to analyze
     * @return OrientationResult with confidence and source information
     */
    fun analyzeOrientation(photoFile: File): OrientationResult {
        return try {
            // First, try to read EXIF orientation
            val exifResult = readExifOrientation(photoFile)

            if (exifResult.source == OrientationSource.EXIF && exifResult.confidence > 0.8f) {
                return exifResult
            }

            // If EXIF is unreliable, try fallback detection
            val fallbackResult = detectOrientationFromPixels(photoFile)

            // Return the most confident result
            if (fallbackResult.confidence > exifResult.confidence) {
                fallbackResult
            } else {
                exifResult
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze orientation", e)
            OrientationResult(
                orientation = PhotoOrientation.NORMAL,
                source = OrientationSource.FALLBACK,
                confidence = 0.1f,
                errorMessage = e.message
            )
        }
    }

    /**
     * Read orientation from EXIF metadata
     */
    private fun readExifOrientation(photoFile: File): OrientationResult {
        return try {
            val exif = ExifInterface(photoFile.absolutePath)
            val orientationValue = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val orientation = PhotoOrientation.fromExifValue(orientationValue)
            val confidence = if (orientationValue == ExifInterface.ORIENTATION_UNDEFINED) 0.1f else 0.9f

            OrientationResult(
                orientation = orientation,
                source = OrientationSource.EXIF,
                confidence = confidence,
                validated = true
            )

        } catch (e: Exception) {
            Log.w(TAG, "Failed to read EXIF orientation", e)
            OrientationResult(
                orientation = PhotoOrientation.NORMAL,
                source = OrientationSource.FALLBACK,
                confidence = 0.1f,
                errorMessage = e.message
            )
        }
    }

    /**
     * Fallback orientation detection using pixel analysis.
     * This is a simplified implementation - could be enhanced with ML-based detection.
     */
    private fun detectOrientationFromPixels(photoFile: File): OrientationResult {
        return try {
            // For now, analyze aspect ratio and make educated guess
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(photoFile.absolutePath, options)

            val width = options.outWidth
            val height = options.outHeight

            // Simple heuristic: if width > height significantly, might be rotated portrait
            val aspectRatio = width.toFloat() / height.toFloat()

            val orientation = when {
                aspectRatio > 1.5f -> PhotoOrientation.ROTATE_90 // Likely rotated portrait
                aspectRatio < 0.7f -> PhotoOrientation.NORMAL    // Likely normal portrait
                else -> PhotoOrientation.NORMAL                   // Square or normal landscape
            }

            OrientationResult(
                orientation = orientation,
                source = OrientationSource.PIXEL_ANALYSIS,
                confidence = 0.6f, // Medium confidence for heuristic
                validated = false
            )

        } catch (e: Exception) {
            Log.w(TAG, "Failed to detect orientation from pixels", e)
            OrientationResult(
                orientation = PhotoOrientation.NORMAL,
                source = OrientationSource.FALLBACK,
                confidence = 0.1f,
                errorMessage = e.message
            )
        }
    }

    /**
     * Apply orientation transformation to a bitmap.
     * Uses GPU-accelerated Matrix operations for performance.
     *
     * @param bitmap The source bitmap
     * @param orientation The orientation to apply
     * @return Transformed bitmap (may be the same as input for NORMAL orientation)
     */
    fun applyOrientationToBitmap(bitmap: Bitmap, orientation: PhotoOrientation): Bitmap {
        if (orientation == PhotoOrientation.NORMAL) {
            return bitmap
        }

        return try {
            val matrix = Matrix()

            // Apply rotation if needed
            if (orientation.degrees != 0f) {
                matrix.postRotate(orientation.degrees)
            }

            // Apply horizontal flip if needed
            if (orientation.flipHorizontal) {
                matrix.postScale(-1f, 1f)
            }

            // Apply vertical flip if needed
            if (orientation.flipVertical) {
                matrix.postScale(1f, -1f)
            }

            // Create transformed bitmap
            val transformedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )

            // Clean up original bitmap if it's different from transformed
            if (transformedBitmap != bitmap) {
                bitmap.recycle()
            }

            transformedBitmap

        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during orientation transformation", e)
            bitmap // Return original if out of memory
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply orientation transformation", e)
            bitmap // Return original on any other error
        }
    }

    /**
     * Generate integrity hash for legal compliance.
     * Creates SHA-256 hash of original photo file for tamper detection.
     */
    fun generateIntegrityHash(photoFile: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val fileBytes = photoFile.readBytes()
            val hashBytes = digest.digest(fileBytes)
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate integrity hash", e)
            null
        }
    }

    /**
     * Validate photo integrity using stored hash.
     * Used for legal compliance and tamper detection.
     */
    fun validateIntegrity(photoFile: File, expectedHash: String): Boolean {
        val currentHash = generateIntegrityHash(photoFile)
        return currentHash == expectedHash
    }

    /**
     * Create a rotation matrix for the given orientation.
     * Useful for applying to other graphics operations.
     */
    fun createOrientationMatrix(orientation: PhotoOrientation): Matrix {
        val matrix = Matrix()

        if (orientation.degrees != 0f) {
            matrix.postRotate(orientation.degrees)
        }

        if (orientation.flipHorizontal) {
            matrix.postScale(-1f, 1f)
        }

        if (orientation.flipVertical) {
            matrix.postScale(1f, -1f)
        }

        return matrix
    }

    /**
     * Calculate inSampleSize for memory-efficient loading.
     * Extracted from PhotoExifExtractor for consistency.
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}