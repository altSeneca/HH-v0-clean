package com.hazardhawk.data.mocks

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * Mock S3 client for testing file upload/download operations
 * 
 * Features:
 * - Simulates presigned URL generation
 * - Tracks upload progress
 * - Simulates upload failures and retries
 * - In-memory file storage
 */
class MockS3Client(
    private val config: MockS3Config = MockS3Config()
) {
    private val storage = mutableMapOf<String, ByteArray>()
    private val uploadHistory = mutableListOf<UploadRecord>()
    var attemptCount = 0
        private set
    
    data class MockS3Config(
        val failureCount: Int = 0, // Number of times to fail before succeeding
        val uploadDelayMs: Long = 100L,
        val progressUpdateIntervalMs: Long = 10L,
        val simulateSlowUpload: Boolean = false,
        val maxFileSizeBytes: Long = 10 * 1024 * 1024, // 10MB default
        val baseUrl: String = "https://cdn.hazardhawk.com"
    )
    
    data class PresignedUrl(
        val uploadUrl: String,
        val cdnUrl: String,
        val expiresIn: Long = 900 // 15 minutes
    )
    
    data class UploadRecord(
        val key: String,
        val sizeBytes: Long,
        val contentType: String,
        val timestamp: Long = System.currentTimeMillis(),
        val success: Boolean
    )
    
    /**
     * Generate presigned URL for upload
     */
    suspend fun getPresignedUploadUrl(
        bucket: String,
        key: String,
        contentType: String
    ): Result<PresignedUrl> {
        delay(50) // Simulate API latency
        
        val presignedUrl = PresignedUrl(
            uploadUrl = "${config.baseUrl}/$bucket/$key?presigned=true",
            cdnUrl = "${config.baseUrl}/$bucket/$key",
            expiresIn = 900
        )
        
        return Result.success(presignedUrl)
    }
    
    /**
     * Upload file to presigned URL with progress tracking
     */
    suspend fun uploadToPresignedUrl(
        presignedUrl: String,
        data: ByteArray,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        attemptCount++
        
        // Validate file size
        if (data.size > config.maxFileSizeBytes) {
            return Result.failure(
                MockS3Exception("File size ${data.size} exceeds maximum ${config.maxFileSizeBytes}")
            )
        }
        
        // Simulate configured failures
        if (attemptCount <= config.failureCount) {
            uploadHistory.add(
                UploadRecord(
                    key = extractKeyFromUrl(presignedUrl),
                    sizeBytes = data.size.toLong(),
                    contentType = contentType,
                    success = false
                )
            )
            return Result.failure(MockS3Exception("Simulated upload failure (attempt $attemptCount)"))
        }
        
        // Simulate upload with progress
        val totalChunks = if (config.simulateSlowUpload) 20 else 5
        for (i in 1..totalChunks) {
            delay(config.uploadDelayMs / totalChunks)
            val progress = i.toFloat() / totalChunks
            onProgress(progress)
        }
        
        // Store file in memory
        val key = extractKeyFromUrl(presignedUrl)
        storage[key] = data
        
        // Record successful upload
        uploadHistory.add(
            UploadRecord(
                key = key,
                sizeBytes = data.size.toLong(),
                contentType = contentType,
                success = true
            )
        )
        
        // Return CDN URL
        val cdnUrl = presignedUrl.substringBefore("?presigned=true")
        return Result.success(cdnUrl)
    }
    
    /**
     * Upload file with automatic retry logic
     */
    suspend fun uploadWithRetry(
        bucket: String,
        key: String,
        data: ByteArray,
        contentType: String,
        maxRetries: Int = 3,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        var lastError: Exception? = null
        
        repeat(maxRetries) { attempt ->
            val presignedResult = getPresignedUploadUrl(bucket, key, contentType)
            if (presignedResult.isFailure) {
                lastError = presignedResult.exceptionOrNull() as? Exception
                return@repeat
            }
            
            val presignedUrl = presignedResult.getOrNull()!!
            val uploadResult = uploadToPresignedUrl(
                presignedUrl.uploadUrl,
                data,
                contentType,
                onProgress
            )
            
            if (uploadResult.isSuccess) {
                return uploadResult
            }
            
            lastError = uploadResult.exceptionOrNull() as? Exception
            
            // Exponential backoff
            if (attempt < maxRetries - 1) {
                val backoffMs = (100L * (1 shl attempt))
                delay(backoffMs)
            }
        }
        
        return Result.failure(
            lastError ?: MockS3Exception("Upload failed after $maxRetries retries")
        )
    }
    
    /**
     * Download file from S3
     */
    suspend fun download(url: String): Result<ByteArray> {
        delay(50) // Simulate network latency
        
        val key = extractKeyFromUrl(url)
        val data = storage[key]
        
        return if (data != null) {
            Result.success(data)
        } else {
            Result.failure(MockS3Exception("File not found: $key"))
        }
    }
    
    /**
     * Delete file from S3
     */
    suspend fun delete(url: String): Result<Unit> {
        delay(50)
        
        val key = extractKeyFromUrl(url)
        storage.remove(key)
        
        return Result.success(Unit)
    }
    
    /**
     * Check if file exists
     */
    suspend fun exists(url: String): Boolean {
        val key = extractKeyFromUrl(url)
        return storage.containsKey(key)
    }
    
    /**
     * Get file size
     */
    suspend fun getFileSize(url: String): Result<Long> {
        val key = extractKeyFromUrl(url)
        val data = storage[key]
        
        return if (data != null) {
            Result.success(data.size.toLong())
        } else {
            Result.failure(MockS3Exception("File not found: $key"))
        }
    }
    
    /**
     * Get upload history for verification
     */
    fun getUploadHistory(): List<UploadRecord> = uploadHistory.toList()
    
    /**
     * Clear storage and history
     */
    fun clear() {
        storage.clear()
        uploadHistory.clear()
        attemptCount = 0
    }
    
    /**
     * Get stored file count
     */
    fun getStoredFileCount(): Int = storage.size
    
    /**
     * Extract key from URL
     */
    private fun extractKeyFromUrl(url: String): String {
        return url
            .substringAfter("${config.baseUrl}/")
            .substringBefore("?")
    }
}

/**
 * Mock S3 exception
 */
class MockS3Exception(message: String) : Exception(message)
