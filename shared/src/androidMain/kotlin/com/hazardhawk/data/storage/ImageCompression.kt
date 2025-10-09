package com.hazardhawk.data.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Android-specific image compression utilities.
 * Handles EXIF orientation correction and quality-based compression.
 */
object ImageCompression {

    /**
     * Compresses an image to meet a target file size.
     * Automatically handles EXIF orientation and maintains aspect ratio.
     *
     * @param imageData Original image data
     * @param maxSizeKB Target maximum size in kilobytes
     * @return Compressed image data, or original if already under target size
     */
    fun compressImage(
        imageData: ByteArray,
        maxSizeKB: Int = 500
    ): ByteArray {
        try {
            val currentSizeKB = imageData.size / 1024

            // If already under target size, return as-is
            if (currentSizeKB <= maxSizeKB) {
                return imageData
            }

            // Decode the image
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
            }

            var bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
                ?: return imageData

            // Fix orientation based on EXIF data
            bitmap = fixImageOrientation(imageData, bitmap)

            // Calculate initial quality based on size ratio
            val sizeRatio = maxSizeKB.toFloat() / currentSizeKB.toFloat()
            var quality = (sizeRatio * 100).toInt().coerceIn(10, 100)

            var compressedData = compressBitmapToByteArray(bitmap, quality)
            var attempts = 0
            val maxAttempts = 5

            // Iteratively reduce quality until target size is met
            while (compressedData.size / 1024 > maxSizeKB && attempts < maxAttempts) {
                quality = (quality * 0.8).toInt().coerceAtLeast(10)
                compressedData = compressBitmapToByteArray(bitmap, quality)
                attempts++
            }

            // If still too large after quality reduction, scale down the bitmap
            if (compressedData.size / 1024 > maxSizeKB) {
                val scaleFactor = kotlin.math.sqrt(maxSizeKB.toFloat() / (compressedData.size / 1024))
                val scaledBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scaleFactor).toInt(),
                    (bitmap.height * scaleFactor).toInt(),
                    true
                )
                compressedData = compressBitmapToByteArray(scaledBitmap, quality)

                if (scaledBitmap != bitmap) {
                    scaledBitmap.recycle()
                }
            }

            bitmap.recycle()
            return compressedData

        } catch (e: Exception) {
            // If compression fails, return original
            return imageData
        }
    }

    /**
     * Compresses a bitmap to JPEG byte array.
     */
    private fun compressBitmapToByteArray(bitmap: Bitmap, quality: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Fixes image orientation based on EXIF data.
     * Many cameras save images in one orientation but record the intended
     * orientation in EXIF metadata.
     */
    private fun fixImageOrientation(imageData: ByteArray, bitmap: Bitmap): Bitmap {
        try {
            val inputStream = ByteArrayInputStream(imageData)
            val exif = ExifInterface(inputStream)
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
                else -> return bitmap // No rotation needed
            }

            return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )

        } catch (e: Exception) {
            // If EXIF processing fails, return original bitmap
            return bitmap
        }
    }

    /**
     * Creates a thumbnail from an image with fixed dimensions.
     *
     * @param imageData Original image data
     * @param maxWidth Maximum width of thumbnail (default: 300px)
     * @param maxHeight Maximum height of thumbnail (default: 300px)
     * @return Thumbnail image data
     */
    fun createThumbnail(
        imageData: ByteArray,
        maxWidth: Int = 300,
        maxHeight: Int = 300
    ): ByteArray {
        try {
            var bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                ?: return imageData

            // Fix orientation
            bitmap = fixImageOrientation(imageData, bitmap)

            // Calculate scale factor maintaining aspect ratio
            val scaleFactor = minOf(
                maxWidth.toFloat() / bitmap.width,
                maxHeight.toFloat() / bitmap.height
            ).coerceAtMost(1f) // Don't scale up

            val scaledWidth = (bitmap.width * scaleFactor).toInt()
            val scaledHeight = (bitmap.height * scaleFactor).toInt()

            val thumbnailBitmap = Bitmap.createScaledBitmap(
                bitmap,
                scaledWidth,
                scaledHeight,
                true
            )

            val thumbnailData = compressBitmapToByteArray(thumbnailBitmap, 80)

            bitmap.recycle()
            if (thumbnailBitmap != bitmap) {
                thumbnailBitmap.recycle()
            }

            return thumbnailData

        } catch (e: Exception) {
            // If thumbnail creation fails, return original
            return imageData
        }
    }
}
