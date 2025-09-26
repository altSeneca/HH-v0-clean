package com.hazardhawk.data

import com.hazardhawk.domain.entities.Photo
import com.hazardhawk.domain.entities.ComplianceStatus
import com.hazardhawk.domain.entities.SyncStatus
import com.hazardhawk.domain.entities.ExifData
import com.hazardhawk.domain.entities.PhotoMetadata
import com.hazardhawk.domain.entities.PhotoStorageInfo
import com.hazardhawk.domain.entities.DuplicatePhotoGroup
import com.hazardhawk.domain.entities.StorageStats
import com.hazardhawk.domain.repositories.PhotoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import android.content.Context
import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import android.net.Uri
import android.os.Build
import com.hazardhawk.camera.MetadataEmbedder
import java.util.concurrent.ConcurrentHashMap

/**
 * Compatibility PhotoRepository that works with the existing File-based photo system
 * This bridges the gap between the new gallery architecture and existing file storage
 * 
 * Currently commented out due to missing interface definition in shared module
 */
class PhotoRepositoryCompat(
    private val context: Context
) : PhotoRepository {

    // Metadata cache to avoid re-processing
    private val metadataCache = ConcurrentHashMap<String, com.hazardhawk.camera.CaptureMetadata?>()
    private val metadataEmbedder = MetadataEmbedder(context)
    
    private fun getAllPhotoFiles(): List<File> {
        return PhotoStorageManagerCompat.getAllPhotos(context)
    }
    
    /**
     * PERFORMANCE OPTIMIZED: Fast photo creation without metadata extraction
     * Used for gallery listing - metadata loaded lazily when needed
     */
    private fun File.toPhotoFast(): Photo {
        android.util.Log.v("PhotoRepositoryCompat", "🚀 Fast loading photo: ${this.name}")
        return Photo(
            id = this.absolutePath, // Use file path as ID for simplicity
            fileName = this.name,
            filePath = this.absolutePath,
            capturedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(this.lastModified()),
            timestamp = this.lastModified(),
            location = null, // Loaded lazily
            projectId = null, // Loaded lazily
            userId = null, // Loaded lazily
            complianceStatus = com.hazardhawk.domain.entities.ComplianceStatus.Unknown,
            syncStatus = SyncStatus.Pending,
            s3Url = null,
            fileSize = this.length(),
            width = null, // Could be extracted from image metadata
            height = null, // Could be extracted from image metadata
            md5Hash = null,
            thumbnailPath = null,
            exifData = null, // Could be extracted from EXIF data
            tags = emptyList(),
            createdAt = this.lastModified(),
            updatedAt = this.lastModified()
        )
    }

    /**
     * Extract metadata in background with caching
     */
    private suspend fun extractMetadataAsync(file: File): com.hazardhawk.camera.CaptureMetadata? = withContext(Dispatchers.IO) {
        val cacheKey = "${file.absolutePath}:${file.lastModified()}"

        // Check cache first
        metadataCache[cacheKey]?.let { cached ->
            android.util.Log.v("PhotoRepositoryCompat", "📋 Cache hit for metadata: ${file.name}")
            return@withContext cached
        }

        // Extract metadata in background
        try {
            android.util.Log.d("PhotoRepositoryCompat", "🔍 Extracting metadata async: ${file.name}")
            val metadata = metadataEmbedder.extractMetadataFromPhoto(file)
            android.util.Log.d("PhotoRepositoryCompat", "✅ Metadata extracted: projectName='${metadata?.projectName}', projectId='${metadata?.projectId}'")

            // Cache the result
            metadataCache[cacheKey] = metadata
            metadata
        } catch (e: Exception) {
            android.util.Log.e("PhotoRepositoryCompat", "❌ Failed to extract metadata from ${file.name}", e)
            // Cache the failure to avoid retrying
            metadataCache[cacheKey] = null
            null
        }
    }

    /**
     * Get detailed photo with metadata - used when viewing individual photos
     */
    suspend fun getPhotoWithMetadata(photoId: String): Photo? {
        val file = File(photoId)
        if (!file.exists()) return null

        val basePhoto = file.toPhotoFast()
        val metadata = extractMetadataAsync(file)

        return basePhoto.copy(
            location = metadata?.locationData?.let { loc ->
                if (loc.isAvailable) {
                    com.hazardhawk.domain.entities.GpsCoordinates(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        timestamp = loc.timestamp
                    )
                } else null
            },
            projectId = metadata?.projectId?.takeIf { it.isNotBlank() },
            userId = metadata?.userId?.takeIf { it.isNotBlank() }
        )
    }
    
    override suspend fun savePhoto(photo: Photo): Result<Photo> {
        return Result.success(photo)
    }
    
    suspend fun savePhoto(filePath: String, metadata: com.hazardhawk.camera.CaptureMetadata): Photo {
        val file = File(filePath)
        return file.toPhotoFast()
    }
    
    override suspend fun getPhoto(photoId: String): Photo? {
        val file = File(photoId)
        return if (file.exists()) file.toPhotoFast() else null
    }
    
    suspend fun getPhotoById(id: String): Photo? {
        return getPhoto(id)
    }
    
    suspend fun updateSyncStatus(id: String, status: SyncStatus, s3Url: String?) {
        // No-op for file-based system
    }
    
    suspend fun updateComplianceStatus(id: String, status: com.hazardhawk.domain.entities.ComplianceStatus) {
        // No-op for file-based system
    }
    
    override suspend fun deletePhoto(photoId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("PhotoRepositoryCompat", "🗑️ Attempting to delete photo: $photoId")

                val success = deletePhotoFromSystem(photoId)

                if (success) {
                    // Clear metadata cache for deleted photo
                    clearMetadataCacheForPhoto(photoId)
                    android.util.Log.d("PhotoRepositoryCompat", "✅ Successfully deleted photo: $photoId")
                    Result.success(Unit)
                } else {
                    android.util.Log.e("PhotoRepositoryCompat", "❌ Failed to delete photo: $photoId")
                    Result.failure(Exception("Failed to delete photo: $photoId"))
                }
            } catch (e: Exception) {
                android.util.Log.e("PhotoRepositoryCompat", "❌ Exception deleting photo: $photoId", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Enhanced photo deletion that handles both file system and MediaStore
     */
    private suspend fun deletePhotoFromSystem(photoPath: String): Boolean {
        val file = File(photoPath)

        // Method 1: Try MediaStore deletion first (for scoped storage)
        val mediaStoreDeleted = deleteFromMediaStore(file)
        if (mediaStoreDeleted) {
            android.util.Log.d("PhotoRepositoryCompat", "📱 Deleted from MediaStore: ${file.name}")
            return true
        }

        // Method 2: Direct file deletion (fallback)
        val fileDeleted = try {
            if (file.exists()) {
                val deleted = file.delete()
                android.util.Log.d("PhotoRepositoryCompat", "📁 Direct file deletion result: $deleted for ${file.name}")
                deleted
            } else {
                android.util.Log.w("PhotoRepositoryCompat", "⚠️ File doesn't exist: $photoPath")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("PhotoRepositoryCompat", "❌ Direct file deletion failed for ${file.name}", e)
            false
        }

        return fileDeleted
    }

    /**
     * Delete photo from Android MediaStore using content resolver
     */
    private suspend fun deleteFromMediaStore(file: File): Boolean {
        return try {
            android.util.Log.d("PhotoRepositoryCompat", "🗑️ Starting MediaStore deletion for: ${file.absolutePath}")

            val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME)
            val selection = "${MediaStore.Images.Media.DATA} = ?"
            val selectionArgs = arrayOf(file.absolutePath)

            android.util.Log.d("PhotoRepositoryCompat", "🔍 Querying MediaStore with path: ${file.absolutePath}")

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                android.util.Log.d("PhotoRepositoryCompat", "📊 MediaStore query returned ${cursor.count} results")

                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    android.util.Log.d("PhotoRepositoryCompat", "📱 Found in MediaStore: ID=$id, Name=$displayName, URI=$uri")

                    val deletedRows = context.contentResolver.delete(uri, null, null)
                    android.util.Log.d("PhotoRepositoryCompat", "🗑️ MediaStore deletion: $deletedRows rows affected for ${file.name}")

                    if (deletedRows > 0) {
                        android.util.Log.d("PhotoRepositoryCompat", "✅ Successfully deleted from MediaStore: ${file.name}")
                        true
                    } else {
                        android.util.Log.w("PhotoRepositoryCompat", "⚠️ MediaStore deletion returned 0 rows for: ${file.name}")
                        false
                    }
                } else {
                    android.util.Log.w("PhotoRepositoryCompat", "📱 Photo not found in MediaStore query: ${file.name}")

                    // Try alternative search by display name
                    val alternativeSelection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
                    val alternativeArgs = arrayOf(file.name)

                    android.util.Log.d("PhotoRepositoryCompat", "🔍 Trying alternative query by display name: ${file.name}")

                    context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        alternativeSelection,
                        alternativeArgs,
                        null
                    )?.use { altCursor ->
                        android.util.Log.d("PhotoRepositoryCompat", "📊 Alternative query returned ${altCursor.count} results")

                        if (altCursor.moveToFirst()) {
                            val idColumn = altCursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                            val id = altCursor.getLong(idColumn)
                            val storedPath = altCursor.getString(altCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                            android.util.Log.d("PhotoRepositoryCompat", "📱 Found via display name: ID=$id, StoredPath=$storedPath, URI=$uri")

                            val deletedRows = context.contentResolver.delete(uri, null, null)
                            android.util.Log.d("PhotoRepositoryCompat", "🗑️ Alternative MediaStore deletion: $deletedRows rows affected")

                            if (deletedRows > 0) {
                                android.util.Log.d("PhotoRepositoryCompat", "✅ Successfully deleted via alternative query: ${file.name}")
                                true
                            } else {
                                android.util.Log.w("PhotoRepositoryCompat", "⚠️ Alternative deletion also returned 0 rows")
                                false
                            }
                        } else {
                            android.util.Log.w("PhotoRepositoryCompat", "❌ Photo not found in either MediaStore query")
                            false
                        }
                    } ?: false
                }
            } ?: false
        } catch (e: Exception) {
            android.util.Log.e("PhotoRepositoryCompat", "❌ MediaStore deletion failed for ${file.name}", e)
            false
        }
    }

    /**
     * Clear metadata cache entries for a deleted photo
     */
    private fun clearMetadataCacheForPhoto(photoPath: String) {
        val file = File(photoPath)
        val keysToRemove = metadataCache.keys.filter { key ->
            key.startsWith("${file.absolutePath}:")
        }
        keysToRemove.forEach { key ->
            metadataCache.remove(key)
            android.util.Log.v("PhotoRepositoryCompat", "🧹 Cleared cache for key: $key")
        }
    }

    suspend fun deletePhotoCompat(id: String): Boolean {
        return deletePhoto(id).isSuccess
    }

    suspend fun deleteMultiplePhotos(ids: List<String>): Int {
        android.util.Log.d("PhotoRepositoryCompat", "🗑️ Batch deleting ${ids.size} photos")
        var deletedCount = 0

        ids.forEach { id ->
            if (deletePhotoCompat(id)) {
                deletedCount++
            }
        }

        android.util.Log.d("PhotoRepositoryCompat", "✅ Batch deletion completed: $deletedCount/${ids.size} photos deleted")
        return deletedCount
    }
    
    fun getRecentPhotos(limit: Int): Flow<List<Photo>> {
        return flow {
            val photos = getAllPhotoFiles()
                .sortedByDescending { it.lastModified() }
                .take(limit)
                .map { it.toPhotoFast() }
            emit(photos)
        }
    }
    
    fun getPendingPhotos(): Flow<List<Photo>> {
        return getAllPhotosFlow() // All photos are considered pending in file-based system
    }
    
    fun getPhotosByProject(projectId: String, limit: Int): Flow<List<Photo>> {
        return getAllPhotosFlow() // No project filtering in file-based system
    }
    
    override suspend fun getPhotos(): Flow<List<Photo>> {
        return flow {
            android.util.Log.d("PhotoRepositoryCompat", "🚀 PERFORMANCE OPTIMIZED: Starting fast photo loading...")
            val startTime = System.currentTimeMillis()

            val photos = getAllPhotosOptimized()

            val loadTime = System.currentTimeMillis() - startTime
            android.util.Log.d("PhotoRepositoryCompat", "⚡ Fast loading completed: ${photos.size} photos in ${loadTime}ms")

            emit(photos)
        }
    }

    override suspend fun getAllPhotos(): List<Photo> {
        return getAllPhotosOptimized()
    }

    /**
     * PERFORMANCE OPTIMIZED: Fast photo loading without metadata extraction
     * Uses concurrent processing for large galleries
     */
    private suspend fun getAllPhotosOptimized(): List<Photo> = withContext(Dispatchers.IO) {
        val photoFiles = getAllPhotoFiles()
            .sortedByDescending { it.lastModified() }

        android.util.Log.d("PhotoRepositoryCompat", "🔄 Processing ${photoFiles.size} photo files concurrently...")

        // Process photos concurrently in batches to avoid overwhelming the system
        val batchSize = 20 // Process 20 photos at a time
        val photos = mutableListOf<Photo>()

        photoFiles.chunked(batchSize).forEach { batch ->
            val batchPhotos = batch.map { file ->
                async(Dispatchers.IO) {
                    file.toPhotoFast()
                }
            }.awaitAll()

            photos.addAll(batchPhotos)
            android.util.Log.v("PhotoRepositoryCompat", "✅ Processed batch of ${batchPhotos.size} photos")
        }

        android.util.Log.d("PhotoRepositoryCompat", "🎯 Total photos loaded: ${photos.size}")

        // Preload metadata for recently taken photos in background (non-blocking)
        launchMetadataPreloading(photos.take(10))

        photos
    }

    /**
     * Background metadata preloading for better user experience
     * Loads metadata for recently taken photos without blocking gallery display
     */
    private fun launchMetadataPreloading(recentPhotos: List<Photo>) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("PhotoRepositoryCompat", "🔄 Background metadata preloading started for ${recentPhotos.size} recent photos")

                recentPhotos.forEach { photo ->
                    try {
                        val file = File(photo.filePath)
                        if (file.exists()) {
                            extractMetadataAsync(file) // This will cache the metadata
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("PhotoRepositoryCompat", "Failed to preload metadata for ${photo.fileName}", e)
                    }
                }

                android.util.Log.d("PhotoRepositoryCompat", "✅ Background metadata preloading completed")
            } catch (e: Exception) {
                android.util.Log.e("PhotoRepositoryCompat", "Background metadata preloading failed", e)
            }
        }
    }
    
    fun getAllPhotosFlow(): Flow<List<Photo>> {
        return flow {
            val photos = getAllPhotos()
            emit(photos)
        }
    }
    
    fun getPhotosByDateRange(startTime: Long, endTime: Long): Flow<List<Photo>> {
        return flow {
            val photos = getAllPhotoFiles()
                .filter { it.lastModified() in startTime..endTime }
                .sortedByDescending { it.lastModified() }
                .map { it.toPhotoFast() }
            emit(photos)
        }
    }
    
    fun getPhotosWithoutThumbnails(limit: Int): Flow<List<Photo>> {
        return getAllPhotosFlow() // All photos considered without thumbnails
    }
    
    // Gallery optimization methods
    suspend fun getPhotosPaged(offset: Int, limit: Int, projectId: String?): List<Photo> {
        return getAllPhotoFiles()
            .sortedByDescending { it.lastModified() }
            .drop(offset)
            .take(limit)
            .map { it.toPhotoFast() }
    }
    
    suspend fun getPhotoThumbnails(photoIds: List<String>): Map<String, String> {
        // Return original file paths as thumbnails
        return photoIds.associateWith { it }
    }
    
    suspend fun getPhotoCount(projectId: String?): Int {
        return getAllPhotoFiles().size
    }
    
    suspend fun searchPhotos(query: String, offset: Int, limit: Int): List<Photo> {
        return getAllPhotoFiles()
            .filter { it.name.contains(query, ignoreCase = true) }
            .sortedByDescending { it.lastModified() }
            .drop(offset)
            .take(limit)
            .map { it.toPhotoFast() }
    }
    
    suspend fun getPhotosFilteredByDate(startDate: Long, endDate: Long, offset: Int, limit: Int): List<Photo> {
        return getAllPhotoFiles()
            .filter { it.lastModified() in startDate..endDate }
            .sortedByDescending { it.lastModified() }
            .drop(offset)
            .take(limit)
            .map { it.toPhotoFast() }
    }
    
    suspend fun getPhotosFilteredByCompliance(status: com.hazardhawk.domain.entities.ComplianceStatus, offset: Int, limit: Int): List<Photo> {
        return emptyList() // No compliance filtering in file-based system
    }
    
    suspend fun prefetchNextBatch(currentOffset: Int, batchSize: Int, projectId: String?) {
        // No-op for file-based system
    }
    
    suspend fun generateThumbnail(photoId: String, filePath: String): String? {
        return filePath // Return original path
    }
    
    suspend fun cleanupOldThumbnails(olderThanDays: Int) {
        // No-op for file-based system
    }
    
    // Statistics and analytics (simplified)
    suspend fun getTotalPhotoCount(): Int = getAllPhotoFiles().size
    
    suspend fun getTotalStorageSize(): Long {
        return getAllPhotoFiles().sumOf { it.length() }
    }
    
    suspend fun getPhotoStorageInfo(): PhotoStorageInfo {
        val files = getAllPhotoFiles()
        val totalSize = files.sumOf { it.length() }
        
        return PhotoStorageInfo(
            totalPhotos = files.size,
            totalStorageBytes = totalSize,
            thumbnailStorageBytes = 0L,
            duplicatePhotos = 0,
            oldestPhotoTimestamp = files.minOfOrNull { it.lastModified() },
            newestPhotoTimestamp = files.maxOfOrNull { it.lastModified() },
            averageFileSize = if (files.isNotEmpty()) totalSize / files.size else 0L,
            storageByMonth = emptyMap()
        )
    }
    
    suspend fun getDuplicatePhotos(): List<DuplicatePhotoGroup> = emptyList()
    
    suspend fun getStorageStatisticsByMonth(): Map<String, StorageStats> = emptyMap()
    
    // Metadata operations (no-op)
    suspend fun updatePhotoMetadata(id: String, fileSize: Long?, width: Int?, height: Int?, md5Hash: String?) {}
    suspend fun updateThumbnailPath(id: String, thumbnailPath: String) {}
    suspend fun updateExifData(id: String, exifData: ExifData) {}
    suspend fun findPhotoByMd5Hash(md5Hash: String): Photo? = null
    
    // Batch operations
    suspend fun batchUpdateSyncStatus(photoIds: List<String>, status: SyncStatus) {}
    suspend fun getPhotosByIds(ids: List<String>): List<Photo> {
        return ids.mapNotNull { getPhotoById(it) }
    }
    
    // Cleanup operations
    suspend fun getPhotosOlderThan(timestampMillis: Long, limit: Int): List<Photo> {
        return getAllPhotoFiles()
            .filter { it.lastModified() < timestampMillis }
            .take(limit)
            .map { it.toPhotoFast() }
    }
    
    suspend fun cleanupOldPhotos(olderThanDays: Int): Int {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        val oldPhotos = getAllPhotoFiles().filter { it.lastModified() < cutoffTime }
        return deleteMultiplePhotos(oldPhotos.map { it.absolutePath })
    }
    
    // Location-based queries (no location data in file-based system)
    suspend fun getPhotosNearLocation(latitude: Double, longitude: Double, radiusMeters: Double, limit: Int): List<Photo> = emptyList()
    suspend fun getPhotosWithLocation(limit: Int): List<Photo> = emptyList()
    
    // Tag operations
    override suspend fun updatePhotoTags(photoId: String, tags: List<String>): Result<Photo> {
        return try {
            // For file-based system, we can't persist tags to the file system easily
            // So we'll just return the photo with updated tags in memory
            val existingPhoto = getPhoto(photoId)
            if (existingPhoto != null) {
                val updatedPhoto = existingPhoto.copy(tags = tags)
                Result.success(updatedPhoto)
            } else {
                Result.failure(Exception("Photo with ID $photoId not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}