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
 * Legacy PhotoStorageManager - DEPRECATED
 * Use PhotoStorageManagerCompat or new KMP PhotoStorageInterface instead.
 * 
 * This object is maintained for backwards compatibility only.
 * New code should use the factory pattern:
 * val photoStorage = PhotoStorage.getInstance(context)
 */
@Deprecated("Use PhotoStorageManagerCompat or PhotoStorage.getInstance(context)")
object PhotoStorageManager {
    
    private const val TAG = "PhotoStorageManager"
    private const val PHOTO_DIR_NAME = "HazardHawk/Photos"
    private const val THUMBNAILS_DIR_NAME = "HazardHawk/Thumbnails"
    
    /**
     * Get the standardized photos directory - used by both camera and gallery
     */
    @Deprecated("Use PhotoStorageManagerCompat.getPhotosDirectory(context)")
    fun getPhotosDirectory(context: Context): File {
        return PhotoStorageManagerCompat.getPhotosDirectory(context)
    }
    
    /**
     * Get the thumbnails directory
     */
    @Deprecated("Use PhotoStorageManagerCompat.getThumbnailsDirectory(context)")
    fun getThumbnailsDirectory(context: Context): File {
        return PhotoStorageManagerCompat.getThumbnailsDirectory(context)
    }
    
    /**
     * Create a new photo file with timestamp
     */
    @Deprecated("Use PhotoStorageManagerCompat.createPhotoFile(context)")
    fun createPhotoFile(context: Context): File {
        return PhotoStorageManagerCompat.createPhotoFile(context)
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
     * Save photo with detailed result reporting - FIXED: Better error handling
     */
    @Deprecated("Use PhotoStorageManagerCompat.savePhotoWithResult(context, photoFile)")
    fun savePhotoWithResult(context: Context, photoFile: File): Result<SaveResult> {
        return PhotoStorageManagerCompat.savePhotoWithResult(context, photoFile).map { compatResult ->
            SaveResult(
                localFile = compatResult.localFile,
                mediaStoreUri = compatResult.mediaStoreUri,
                fileSizeBytes = compatResult.fileSizeBytes
            )
        }
    }
    
    /**
     * Get all photos from the standardized photos directory
     */
    @Deprecated("Use PhotoStorageManagerCompat.getAllPhotos(context)")
    fun getAllPhotos(context: Context): List<File> {
        return PhotoStorageManagerCompat.getAllPhotos(context)
    }
    
    /**
     * Get FileProvider URI for sharing
     */
    @Deprecated("Use PhotoStorageManagerCompat.getFileProviderUri(context, file)")
    fun getFileProviderUri(context: Context, file: File): Uri? {
        return PhotoStorageManagerCompat.getFileProviderUri(context, file)
    }
    
    /**
     * Clean up old photos if storage is getting full
     */
    @Deprecated("Use PhotoStorageManagerCompat.cleanupOldPhotos(context, keepCount)")
    fun cleanupOldPhotos(context: Context, keepCount: Int = 100) {
        PhotoStorageManagerCompat.cleanupOldPhotos(context, keepCount)
    }
    
    /**
     * Check if storage directory is accessible
     */
    @Deprecated("Use PhotoStorageManagerCompat.isStorageAccessible(context)")
    fun isStorageAccessible(context: Context): Boolean {
        return PhotoStorageManagerCompat.isStorageAccessible(context)
    }
    
    /**
     * Get storage statistics
     */
    @Deprecated("Use PhotoStorageManagerCompat.getStorageStats(context)")
    fun getStorageStats(context: Context): StorageStats {
        val compatResult = PhotoStorageManagerCompat.getStorageStats(context)
        return StorageStats(
            photoCount = compatResult.photoCount,
            totalSizeBytes = compatResult.totalSizeBytes,
            availableSpaceBytes = compatResult.availableSpaceBytes,
            storageDirectory = compatResult.storageDirectory
        )
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