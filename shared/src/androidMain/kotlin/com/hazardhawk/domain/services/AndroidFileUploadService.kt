package com.hazardhawk.domain.services

import com.hazardhawk.data.storage.ImageCompression
import com.hazardhawk.data.storage.S3Client

/**
 * Android-specific implementation of FileUploadService.
 * Uses Android's native image processing capabilities for compression.
 *
 * @property s3Client The S3 client for cloud storage operations
 * @property bucket The default S3 bucket name for uploads
 * @property cdnBaseUrl The base URL for the CDN (if different from S3 direct URL)
 */
class AndroidFileUploadService(
    s3Client: S3Client,
    bucket: String,
    cdnBaseUrl: String? = null
) : FileUploadServiceImpl(s3Client, bucket, cdnBaseUrl) {

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
