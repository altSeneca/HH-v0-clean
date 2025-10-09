package com.hazardhawk.data.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Android-specific image compression utilities using native Bitmap APIs.
 * Provides efficient compression while preserving image quality and EXIF data.
 */
object ImageCompression {

    private const val DEFAULT_QUALITY = 85
    private const val MIN_QUALITY = 50
    private const val MAX_QUALITY = 95

    /**
     * Compresses an image to target file size while preserving EXIF orientation.
     * Uses iterative quality reduction if necessary to meet size target.
     *
     * @param data Original image data
     * @param maxSizeKB Target maximum size in kilobytes (default 500KB)
     * @param format Compression format (JPEG or PNG)
     * @return Compressed image data
     */
    fun compressImage(
        data: ByteArray,
        maxSizeKB: Int = 500,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): ByteArray {
        return try {
            // Decode original image
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)

            // Calculate sample size for initial decode
            val sampleSize = calculateSampleSize(options, maxSizeKB)

            // Decode with sampling
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }
            var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions)
                ?: return data

            // Apply EXIF orientation correction
            bitmap = preserveExifOrientation(bitmap, data)

            // Compress with quality iteration
            val maxSizeBytes = maxSizeKB * 1024
            var quality = DEFAULT_QUALITY
            var compressed: ByteArray

            do {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(format, quality, outputStream)
                compressed = outputStream.toByteArray()

                if (compressed.size <= maxSizeBytes || quality <= MIN_QUALITY) {
                    break
                }

                // Reduce quality for next iteration
                quality = max(MIN_QUALITY, quality - 5)
            } while (compressed.size > maxSizeBytes)

            // Clean up
            bitmap.recycle()

            compressed
        } catch (e: Exception) {
            // Return original data if compression fails
            data
        }
    }

    /**
     * Generates a square thumbnail from image data.
     * Crops to center square before resizing for consistent dimensions.
     *
     * @param data Original image data
     * @param targetSize Target dimension for square thumbnail (default 300px)
     * @param targetSizeKB Target file size in KB (default 100KB)
     * @return Compressed thumbnail data
     */
    fun generateThumbnail(
        data: ByteArray,
        targetSize: Int = 300,
        targetSizeKB: Int = 100
    ): ByteArray {
        return try {
            // Decode with appropriate sample size
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)

            // Calculate sample size for efficient memory usage
            val sampleSize = calculateThumbnailSampleSize(options, targetSize)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }
            var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions)
                ?: return data

            // Apply EXIF orientation
            bitmap = preserveExifOrientation(bitmap, data)

            // Create center-cropped square
            val dimension = min(bitmap.width, bitmap.height)
            val x = (bitmap.width - dimension) / 2
            val y = (bitmap.height - dimension) / 2

            val cropped = Bitmap.createBitmap(bitmap, x, y, dimension, dimension)
            bitmap.recycle()

            // Scale to target size
            val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
            cropped.recycle()

            // Compress to target file size
            val maxSizeBytes = targetSizeKB * 1024
            var quality = DEFAULT_QUALITY
            var compressed: ByteArray

            do {
                val outputStream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                compressed = outputStream.toByteArray()

                if (compressed.size <= maxSizeBytes || quality <= MIN_QUALITY) {
                    break
                }

                quality = max(MIN_QUALITY, quality - 5)
            } while (compressed.size > maxSizeBytes)

            scaled.recycle()

            compressed
        } catch (e: Exception) {
            // Fallback: compress original without resizing
            compressImage(data, targetSizeKB)
        }
    }

    /**
     * Applies EXIF orientation transformation to bitmap.
     * Preserves correct image orientation from camera metadata.
     *
     * @param bitmap Original bitmap
     * @param data Original image data containing EXIF
     * @return Oriented bitmap
     */
    fun preserveExifOrientation(bitmap: Bitmap, data: ByteArray): Bitmap {
        return try {
            val exif = ExifInterface(ByteArrayInputStream(data))
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                else -> return bitmap
            }

            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            if (rotated != bitmap) {
                bitmap.recycle()
            }

            rotated
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * Calculates optimal sample size for image decode based on target file size.
     * Reduces memory usage while maintaining acceptable quality.
     *
     * @param options BitmapFactory options with image dimensions
     * @param maxSizeKB Target file size in KB
     * @return Sample size (power of 2)
     */
    private fun calculateSampleSize(options: BitmapFactory.Options, maxSizeKB: Int): Int {
        val height = options.outHeight
        val width = options.outWidth

        // Estimate bytes per pixel (assuming ARGB_8888)
        val bytesPerPixel = 4
        val currentSizeBytes = height * width * bytesPerPixel
        val targetSizeBytes = maxSizeKB * 1024

        // Calculate scale factor
        val scaleFactor = sqrt((currentSizeBytes.toDouble() / targetSizeBytes)).toInt()

        // Sample size must be power of 2
        var sampleSize = 1
        while (sampleSize * 2 <= scaleFactor) {
            sampleSize *= 2
        }

        return max(1, sampleSize)
    }

    /**
     * Calculates optimal sample size for thumbnail generation.
     *
     * @param options BitmapFactory options with image dimensions
     * @param targetSize Target thumbnail dimension
     * @return Sample size (power of 2)
     */
    private fun calculateThumbnailSampleSize(
        options: BitmapFactory.Options,
        targetSize: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        val minDimension = min(height, width)

        var sampleSize = 1
        while (minDimension / (sampleSize * 2) >= targetSize) {
            sampleSize *= 2
        }

        return sampleSize
    }
}
