package com.hazardhawk.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Compatibility layer for PhotoStorageManager
 * Bridges old object-based API with new KMP factory pattern
 * Maintains backwards compatibility with existing code
 */
object PhotoStorageManagerCompat {
    
    private const val TAG = "PhotoStorageManagerCompat"
    private const val PHOTO_DIR_NAME = "HazardHawk/Photos"
    private const val THUMBNAILS_DIR_NAME = "HazardHawk/Thumbnails"
    
    /**
     * Get the standardized photos directory - used by both camera and gallery
     * Now reads from public Pictures/HazardHawk folder where we save photos
     */
    fun getPhotosDirectory(context: Context): File {
        // Use public Pictures/HazardHawk folder to match where we save photos
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val photosDir = File(picturesDir, "HazardHawk")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
            Log.d(TAG, "Created photos directory: ${photosDir.absolutePath}")
        }
        return photosDir
    }
    
    /**
     * Get the thumbnails directory
     */
    fun getThumbnailsDirectory(context: Context): File {
        val thumbnailsDir = File(context.getExternalFilesDir(null), THUMBNAILS_DIR_NAME)
        if (!thumbnailsDir.exists()) {
            thumbnailsDir.mkdirs()
            Log.d(TAG, "Created thumbnails directory: ${thumbnailsDir.absolutePath}")
        }
        return thumbnailsDir
    }
    
    /**
     * Create a new photo file with timestamp
     */
    fun createPhotoFile(context: Context): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val photosDir = getPhotosDirectory(context)
        return File.createTempFile(
            "HH_${timestamp}_",
            ".jpg",
            photosDir
        )
    }
    
    /**
     * Save photo to both app storage and system MediaStore for gallery visibility
     * DEPRECATED: Use savePhotoWithResult for better error handling
     */
    @Deprecated("Use savePhotoWithResult for detailed error information")
    fun savePhotoWithMediaStoreIntegration(context: Context, photoFile: File): Boolean {
        val result = savePhotoWithResult(context, photoFile)
        return result.isSuccess
    }
    
    /**
     * Save photo with detailed result reporting - OPTIMIZED: Public Pictures only
     * Saves only to public Pictures/HazardHawk folder to reduce storage usage
     */
    fun savePhotoWithResult(context: Context, photoFile: File): Result<SaveResult> {
        return try {
            if (!photoFile.exists()) {
                return Result.failure(IllegalArgumentException("Photo file does not exist: ${photoFile.absolutePath}"))
            }

            if (photoFile.length() == 0L) {
                return Result.failure(IllegalArgumentException("Photo file is empty: ${photoFile.name}"))
            }

            // Save directly to MediaStore (public Pictures/HazardHawk folder)
            // This eliminates the need for dual storage (private + public)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, photoFile.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/HazardHawk")
                }
            }

            val mediaStoreUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return Result.failure(IllegalStateException("Failed to create MediaStore entry"))

            // Copy photo data directly to public Pictures folder via MediaStore
            context.contentResolver.openOutputStream(mediaStoreUri)?.use { outputStream ->
                photoFile.inputStream().use { inputStream ->
                    val bytesCopied = inputStream.copyTo(outputStream)
                    if (bytesCopied != photoFile.length()) {
                        return Result.failure(IllegalStateException("Incomplete copy to MediaStore: $bytesCopied/${photoFile.length()} bytes"))
                    }
                }
            } ?: return Result.failure(IllegalStateException("Failed to open MediaStore output stream"))

            // Clean up temporary file if it was in private storage
            if (photoFile.absolutePath.contains("Android/data")) {
                try {
                    photoFile.delete()
                    Log.d(TAG, "Cleaned up temporary file: ${photoFile.absolutePath}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clean up temporary file (non-critical): ${e.message}")
                }
            }

            // Get the actual file path for return value
            val actualFile = getFileFromMediaStoreUri(context, mediaStoreUri)
                ?: return Result.failure(IllegalStateException("Could not retrieve saved file path"))

            Log.d(TAG, "Photo saved to public Pictures only: ${actualFile.absolutePath}")
            Log.d(TAG, "Storage optimization: Using single location instead of dual storage")

            Result.success(SaveResult(
                localFile = actualFile,
                mediaStoreUri = mediaStoreUri,
                fileSizeBytes = actualFile.length()
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save photo to public Pictures", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all photos from the standardized photos directory
     */
    fun getAllPhotos(context: Context): List<File> {
        val photosDir = getPhotosDirectory(context)
        Log.d(TAG, "📁 Gallery reading from directory: ${photosDir.absolutePath}")
        Log.d(TAG, "📁 Directory exists: ${photosDir.exists()}")
        Log.d(TAG, "📁 Directory can read: ${photosDir.canRead()}")

        val allFiles = photosDir.listFiles()
        Log.d(TAG, "📁 Total files in directory: ${allFiles?.size ?: 0}")

        val photoFiles = photosDir.listFiles { file ->
            file.extension.lowercase() in listOf("jpg", "jpeg", "png")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        Log.d(TAG, "📸 Photo files found: ${photoFiles.size}")
        photoFiles.take(5).forEach { photo ->
            Log.d(TAG, "📸 Photo: ${photo.name}, size: ${photo.length()} bytes, modified: ${photo.lastModified()}")
        }

        return photoFiles
    }
    
    /**
     * Get FileProvider URI for sharing
     */
    fun getFileProviderUri(context: Context, file: File): Uri? {
        return try {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FileProvider URI", e)
            null
        }
    }
    
    /**
     * Clean up old photos if storage is getting full
     */
    fun cleanupOldPhotos(context: Context, keepCount: Int = 100) {
        try {
            val photos = getAllPhotos(context)
            if (photos.size > keepCount) {
                val photosToDelete = photos.drop(keepCount)
                photosToDelete.forEach { photo ->
                    if (photo.delete()) {
                        Log.d(TAG, "Cleaned up old photo: ${photo.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during photo cleanup", e)
        }
    }
    
    /**
     * Check if storage directory is accessible
     */
    fun isStorageAccessible(context: Context): Boolean {
        return try {
            val photosDir = getPhotosDirectory(context)
            photosDir.exists() && photosDir.canWrite()
        } catch (e: Exception) {
            Log.e(TAG, "Storage accessibility check failed", e)
            false
        }
    }
    
    /**
     * Get storage statistics
     */
    fun getStorageStats(context: Context): StorageStats {
        return try {
            val photosDir = getPhotosDirectory(context)
            val photos = getAllPhotos(context)
            val totalSize = photos.sumOf { it.length() }
            
            StorageStats(
                photoCount = photos.size,
                totalSizeBytes = totalSize,
                availableSpaceBytes = photosDir.freeSpace,
                storageDirectory = photosDir.absolutePath
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating storage stats", e)
            StorageStats(0, 0, 0, "Error")
        }
    }

    /**
     * Get File object from MediaStore URI - helper for single storage approach
     */
    private fun getFileFromMediaStoreUri(context: Context, uri: Uri): File? {
        return try {
            context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val filePath = cursor.getString(columnIndex)
                    if (filePath != null) File(filePath) else null
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not get file from MediaStore URI: ${e.message}")
            // Fallback: create a File object with estimated path
            val fileName = context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                } else null
            }
            fileName?.let { File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "HazardHawk/$it") }
        }
    }

    data class StorageStats(
        val photoCount: Int,
        val totalSizeBytes: Long,
        val availableSpaceBytes: Long,
        val storageDirectory: String
    )
    
    data class SaveResult(
        val localFile: File,
        val mediaStoreUri: Uri,
        val fileSizeBytes: Long
    )
}