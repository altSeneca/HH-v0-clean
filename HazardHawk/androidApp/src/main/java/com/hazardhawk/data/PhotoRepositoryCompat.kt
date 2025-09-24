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
import java.io.File
import android.content.Context
import com.hazardhawk.camera.MetadataEmbedder

/**
 * Compatibility PhotoRepository that works with the existing File-based photo system
 * This bridges the gap between the new gallery architecture and existing file storage
 * 
 * Currently commented out due to missing interface definition in shared module
 */
class PhotoRepositoryCompat(
    private val context: Context
) : PhotoRepository {
    
    private fun getAllPhotoFiles(): List<File> {
        return PhotoStorageManagerCompat.getAllPhotos(context)
    }
    
    private fun File.toPhoto(): Photo {
        // Extract metadata from EXIF data if available
        val metadataEmbedder = MetadataEmbedder(context)
        val extractedMetadata = try {
            android.util.Log.d("PhotoRepositoryCompat", "Extracting metadata from: ${this.name}")
            kotlinx.coroutines.runBlocking {
                val metadata = metadataEmbedder.extractMetadataFromPhoto(this@toPhoto)
                android.util.Log.d("PhotoRepositoryCompat", "Extracted metadata: projectName='${metadata?.projectName}', projectId='${metadata?.projectId}'")
                metadata
            }
        } catch (e: Exception) {
            android.util.Log.e("PhotoRepositoryCompat", "Failed to extract metadata from ${this.name}", e)
            null
        }

        return Photo(
            id = this.absolutePath, // Use file path as ID for simplicity
            fileName = this.name,
            filePath = this.absolutePath,
            capturedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(this.lastModified()),
            timestamp = this.lastModified(),
            location = extractedMetadata?.locationData?.let { loc ->
                if (loc.isAvailable) {
                    com.hazardhawk.domain.entities.GpsCoordinates(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        timestamp = loc.timestamp
                    )
                } else null
            },
            projectId = extractedMetadata?.projectId?.takeIf { it.isNotBlank() },
            userId = extractedMetadata?.userId?.takeIf { it.isNotBlank() },
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
    
    override suspend fun savePhoto(photo: Photo): Result<Photo> {
        return Result.success(photo)
    }
    
    suspend fun savePhoto(filePath: String, metadata: PhotoMetadata): Photo {
        val file = File(filePath)
        return file.toPhoto()
    }
    
    override suspend fun getPhoto(photoId: String): Photo? {
        val file = File(photoId)
        return if (file.exists()) file.toPhoto() else null
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
        val file = File(photoId)
        return if (file.exists() && file.delete()) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to delete photo: $photoId"))
        }
    }
    
    suspend fun deletePhotoCompat(id: String): Boolean {
        val file = File(id)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
    
    suspend fun deleteMultiplePhotos(ids: List<String>): Int {
        var deletedCount = 0
        ids.forEach { id ->
            if (deletePhotoCompat(id)) {
                deletedCount++
            }
        }
        return deletedCount
    }
    
    fun getRecentPhotos(limit: Int): Flow<List<Photo>> {
        return flow {
            val photos = getAllPhotoFiles()
                .sortedByDescending { it.lastModified() }
                .take(limit)
                .map { it.toPhoto() }
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
            val photos = getAllPhotos()
            emit(photos)
        }
    }
    
    override suspend fun getAllPhotos(): List<Photo> {
        return getAllPhotoFiles()
            .sortedByDescending { it.lastModified() }
            .map { it.toPhoto() }
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
                .map { it.toPhoto() }
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
            .map { it.toPhoto() }
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
            .map { it.toPhoto() }
    }
    
    suspend fun getPhotosFilteredByDate(startDate: Long, endDate: Long, offset: Int, limit: Int): List<Photo> {
        return getAllPhotoFiles()
            .filter { it.lastModified() in startDate..endDate }
            .sortedByDescending { it.lastModified() }
            .drop(offset)
            .take(limit)
            .map { it.toPhoto() }
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
            .map { it.toPhoto() }
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