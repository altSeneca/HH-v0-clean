package com.hazardhawk.domain.services

/**
 * Service for handling file uploads to cloud storage with compression and progress tracking.
 *
 * Supports file types: Images (PNG, JPG, JPEG), PDFs, Documents
 * Implements automatic retry logic and progress callbacks
 */
interface FileUploadService {
    /**
     * Uploads a file to cloud storage with progress tracking.
     *
     * @param file File data as byte array
     * @param fileName Name of the file including extension
     * @param contentType MIME type (e.g., "image/jpeg", "application/pdf")
     * @param onProgress Callback receiving progress updates (0.0 to 1.0)
     * @return Result containing upload metadata or error
     */
    suspend fun uploadFile(
        file: ByteArray,
        fileName: String,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<UploadResult>

    /**
     * Compresses an image to reduce file size while maintaining quality.
     *
     * @param imageData Original image data as byte array
     * @param maxSizeKB Maximum size in kilobytes (default: 500KB)
     * @return Result containing compressed image data or error
     */
    suspend fun compressImage(
        imageData: ByteArray,
        maxSizeKB: Int = 500
    ): Result<ByteArray>

    /**
     * Uploads multiple files in parallel with progress tracking.
     *
     * @param files List of file data to upload
     * @param onProgress Callback receiving overall progress (0.0 to 1.0)
     * @return List of results for each file (maintains order)
     */
    suspend fun uploadFiles(
        files: List<FileUpload>,
        onProgress: (Float) -> Unit = {}
    ): List<Result<UploadResult>>
}

/**
 * Represents a file to be uploaded.
 *
 * @property data File data as byte array
 * @property fileName Name of the file including extension
 * @property contentType MIME type of the file
 */
data class FileUpload(
    val data: ByteArray,
    val fileName: String,
    val contentType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FileUpload

        if (!data.contentEquals(other.data)) return false
        if (fileName != other.fileName) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}

/**
 * Result of a successful file upload.
 *
 * @property url CDN/public URL of the uploaded file
 * @property thumbnailUrl Optional URL of a thumbnail (for images)
 * @property sizeBytes Final size of the uploaded file in bytes
 * @property key Storage key/path for the file
 */
data class UploadResult(
    val url: String,
    val thumbnailUrl: String? = null,
    val sizeBytes: Long,
    val key: String
)

/**
 * Errors that can occur during file upload.
 */
sealed class FileUploadError : Exception() {
    data class FileTooLarge(val sizeBytes: Long, val maxSizeBytes: Long) : FileUploadError() {
        override val message: String = "File size $sizeBytes bytes exceeds maximum $maxSizeBytes bytes"
    }

    data class InvalidFileType(val contentType: String) : FileUploadError() {
        override val message: String = "Invalid file type: $contentType"
    }

    data class CompressionFailed(val reason: String) : FileUploadError() {
        override val message: String = "Image compression failed: $reason"
    }

    data class UploadFailed(val reason: String, override val cause: Throwable? = null) : FileUploadError() {
        override val message: String = "Upload failed: $reason ${cause?.message ?: ""}"
    }

    data class NetworkError(override val cause: Throwable) : FileUploadError() {
        override val message: String = "Network error during upload: ${cause.message}"
    }

    data class AuthenticationError(val reason: String) : FileUploadError() {
        override val message: String = "Authentication failed: $reason"
    }

    data class RetryExhausted(val attempts: Int) : FileUploadError() {
        override val message: String = "Upload failed after $attempts retry attempts"
    }
}
