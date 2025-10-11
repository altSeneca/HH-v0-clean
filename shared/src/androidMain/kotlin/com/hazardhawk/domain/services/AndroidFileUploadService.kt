package com.hazardhawk.domain.services

import com.hazardhawk.data.storage.ImageCompression
import com.hazardhawk.data.storage.S3Client

/**
 * Android-specific implementation of FileUploadService.
 * Uses Android's native image processing capabilities for compression.
 *
 * @property s3Client The S3 client for cloud storage operations
 * @property config Configuration for file upload service
 */
class AndroidFileUploadService(
    s3Client: S3Client,
    config: FileUploadConfig = FileUploadConfig()
) : FileUploadServiceImpl(s3Client, config) {

    /**
     * Android-specific image compression using native bitmap processing.
     * Overrides the base implementation to use platform-specific optimizations.
     */
    override suspend fun compressImage(
        imageData: ByteArray,
        maxSizeKB: Int
    ): Result<ByteArray> {
        return try {
            val compressedData = ImageCompression.compressImage(
                imageData = imageData,
                maxSizeKB = maxSizeKB
            )

            Result.success(compressedData)

        } catch (e: Exception) {
            Result.failure(
                FileUploadException("Failed to compress image on Android: ${e.message}", e)
            )
        }
    }
}

/**
 * Exception for file upload related errors
 */
class FileUploadException(message: String, cause: Throwable? = null) : Exception(message, cause)
