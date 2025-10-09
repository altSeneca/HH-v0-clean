package com.hazardhawk.domain.services

import kotlinx.serialization.Serializable

/**
 * Service for handling file uploads to cloud storage (S3).
 * Provides image compression, progress tracking, and automatic retry logic.
 */
interface FileUploadService {
    /**
     * Uploads a file to cloud storage with progress tracking.
     * Automatically retries up to 3 times on failure.
     *
     * @param file The file data as a ByteArray
     * @param fileName The name of the file to upload
     * @param contentType The MIME type of the file (e.g., "image/jpeg")
     * @param onProgress Callback for upload progress (0.0 to 1.0)
     * @return Result containing the upload result with URLs, or an error
     */
    suspend fun uploadFile(
        file: ByteArray,
        fileName: String,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<UploadResult>

    /**
     * Compresses an image to reduce file size.
     * Targets a maximum size while maintaining acceptable quality.
     *
     * @param imageData The original image data as a ByteArray
     * @param maxSizeKB Maximum target size in kilobytes (default: 500KB)
     * @return Result containing compressed image data, or an error
     */
    suspend fun compressImage(
        imageData: ByteArray,
        maxSizeKB: Int = 500
    ): Result<ByteArray>
}

/**
 * Result of a successful file upload.
 *
 * @property url The public URL to access the uploaded file
 * @property thumbnailUrl Optional URL to a thumbnail version (for images)
 * @property sizeBytes The final size of the uploaded file in bytes
 */
@Serializable
data class UploadResult(
    val url: String,
    val thumbnailUrl: String? = null,
    val sizeBytes: Long
)
