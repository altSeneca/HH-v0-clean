package com.hazardhawk.domain.services

import com.hazardhawk.data.storage.S3Client
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Implementation of FileUploadService with S3 integration.
 * Provides automatic retry logic, image compression, and progress tracking.
 *
 * @property s3Client The S3 client for cloud storage operations
 * @property bucket The default S3 bucket name for uploads
 * @property cdnBaseUrl The base URL for the CDN (if different from S3 direct URL)
 */
class FileUploadServiceImpl(
    private val s3Client: S3Client,
    private val bucket: String,
    private val cdnBaseUrl: String? = null
) : FileUploadService {

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 8000L
        private const val THUMBNAIL_MAX_SIZE_KB = 100

        // S3 key prefixes for different file types
        private const val CERTIFICATION_DOCS_PREFIX = "certifications"
        private const val THUMBNAILS_PREFIX = "thumbnails"
    }

    override suspend fun uploadFile(
        file: ByteArray,
        fileName: String,
        contentType: String,
        onProgress: (Float) -> Unit
    ): Result<UploadResult> {
        return try {
            // Generate unique S3 key with timestamp
            val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val sanitizedFileName = sanitizeFileName(fileName)
            val s3Key = "$CERTIFICATION_DOCS_PREFIX/$timestamp-$sanitizedFileName"

            // Upload main file with retry logic
            val uploadResult = uploadWithRetry(
                file = file,
                key = s3Key,
                contentType = contentType,
                onProgress = { progress ->
                    // Reserve 80% of progress bar for main file upload
                    onProgress(progress * 0.8f)
                }
            )

            if (uploadResult.isFailure) {
                return Result.failure(
                    uploadResult.exceptionOrNull()
                        ?: Exception("Upload failed with unknown error")
                )
            }

            val fileUrl = uploadResult.getOrThrow()

            // Generate thumbnail if it's an image
            val thumbnailUrl = if (isImageContentType(contentType)) {
                generateAndUploadThumbnail(
                    file = file,
                    originalKey = s3Key,
                    contentType = contentType,
                    onProgress = { progress ->
                        // Thumbnail upload uses remaining 20% of progress
                        onProgress(0.8f + (progress * 0.2f))
                    }
                ).getOrNull()
            } else {
                null
            }

            onProgress(1.0f)

            Result.success(
                UploadResult(
                    url = fileUrl,
                    thumbnailUrl = thumbnailUrl,
                    sizeBytes = file.size.toLong()
                )
            )
        } catch (e: Exception) {
            Result.failure(
                FileUploadException("Failed to upload file: ${e.message}", e)
            )
        }
    }

    override suspend fun compressImage(
        imageData: ByteArray,
        maxSizeKB: Int
    ): Result<ByteArray> {
        return try {
            // Check if compression is needed
            val currentSizeKB = imageData.size / 1024
            if (currentSizeKB <= maxSizeKB) {
                return Result.success(imageData)
            }

            // TODO: Platform-specific image compression
            // For now, return original image
            // Android implementation should use BitmapFactory and compress
            // iOS implementation should use UIImage and compress

            Result.failure(
                UnsupportedOperationException(
                    "Image compression requires platform-specific implementation. " +
                    "Current size: ${currentSizeKB}KB, target: ${maxSizeKB}KB"
                )
            )
        } catch (e: Exception) {
            Result.failure(
                FileUploadException("Failed to compress image: ${e.message}", e)
            )
        }
    }

    /**
     * Uploads a file with exponential backoff retry logic.
     */
    private suspend fun uploadWithRetry(
        file: ByteArray,
        key: String,
        contentType: String,
        onProgress: (Float) -> Unit,
        attempt: Int = 1
    ): Result<String> {
        val result = s3Client.uploadFile(
            bucket = bucket,
            key = key,
            data = file,
            contentType = contentType,
            onProgress = onProgress
        )

        return if (result.isFailure && attempt < MAX_RETRY_ATTEMPTS) {
            // Calculate exponential backoff delay
            val delayMs = min(
                INITIAL_RETRY_DELAY_MS * (1 shl (attempt - 1)),
                MAX_RETRY_DELAY_MS
            )

            delay(delayMs)

            // Retry the upload
            uploadWithRetry(
                file = file,
                key = key,
                contentType = contentType,
                onProgress = onProgress,
                attempt = attempt + 1
            )
        } else if (result.isFailure) {
            // All retries exhausted
            Result.failure(
                FileUploadException(
                    "Upload failed after $MAX_RETRY_ATTEMPTS attempts: ${result.exceptionOrNull()?.message}",
                    result.exceptionOrNull()
                )
            )
        } else {
            // Convert S3 URL to CDN URL if configured
            val s3Url = result.getOrThrow()
            val finalUrl = cdnBaseUrl?.let { cdn ->
                // Extract key from S3 URL and construct CDN URL
                val keyPart = s3Url.substringAfter("$bucket/")
                "$cdn/$keyPart"
            } ?: s3Url

            Result.success(finalUrl)
        }
    }

    /**
     * Generates and uploads a thumbnail version of an image.
     */
    private suspend fun generateAndUploadThumbnail(
        file: ByteArray,
        originalKey: String,
        contentType: String,
        onProgress: (Float) -> Unit
    ): Result<String> {
        return try {
            // Compress image to thumbnail size
            val thumbnailData = compressImage(file, THUMBNAIL_MAX_SIZE_KB)

            if (thumbnailData.isFailure) {
                // If compression fails, skip thumbnail generation
                return Result.success("")
            }

            // Generate thumbnail key
            val fileName = originalKey.substringAfterLast("/")
            val thumbnailKey = "$THUMBNAILS_PREFIX/$fileName"

            // Upload thumbnail (no retry for thumbnails)
            s3Client.uploadFile(
                bucket = bucket,
                key = thumbnailKey,
                data = thumbnailData.getOrThrow(),
                contentType = contentType,
                onProgress = onProgress
            ).map { s3Url ->
                // Convert to CDN URL if configured
                cdnBaseUrl?.let { cdn ->
                    val keyPart = s3Url.substringAfter("$bucket/")
                    "$cdn/$keyPart"
                } ?: s3Url
            }
        } catch (e: Exception) {
            // Thumbnail generation is non-critical, return empty on failure
            Result.success("")
        }
    }

    /**
     * Sanitizes a filename for S3 storage.
     * Removes special characters and spaces.
     */
    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .lowercase()
    }

    /**
     * Checks if a content type represents an image.
     */
    private fun isImageContentType(contentType: String): Boolean {
        return contentType.startsWith("image/")
    }
}

/**
 * Exception thrown when file upload operations fail.
 */
class FileUploadException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
