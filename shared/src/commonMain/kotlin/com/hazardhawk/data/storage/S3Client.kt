package com.hazardhawk.data.storage

/**
 * Client interface for interacting with S3-compatible cloud storage.
 *
 * Implements presigned URL strategy for secure uploads without embedding AWS credentials.
 * Supports progress tracking and automatic retry logic.
 */
interface S3Client {
    /**
     * Uploads a file to S3 using presigned URL strategy.
     *
     * Flow:
     * 1. Request presigned URL from backend API
     * 2. Upload file directly to S3 using HTTP PUT
     * 3. Return CDN URL for accessing the file
     *
     * @param bucket S3 bucket name
     * @param key Object key (path) within the bucket
     * @param data File data as byte array
     * @param contentType MIME type of the file
     * @param onProgress Callback receiving upload progress (0.0 to 1.0)
     * @return Result containing CDN URL or error
     */
    suspend fun uploadFile(
        bucket: String,
        key: String,
        data: ByteArray,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String>

    /**
     * Requests a presigned URL from the backend for uploading.
     *
     * @param bucket S3 bucket name
     * @param key Object key (path) within the bucket
     * @param contentType MIME type of the file
     * @param expirationSeconds URL expiration time in seconds (default: 300)
     * @return Result containing presigned upload URL or error
     */
    suspend fun getPresignedUploadUrl(
        bucket: String,
        key: String,
        contentType: String,
        expirationSeconds: Int = 300
    ): Result<PresignedUrlResponse>

    /**
     * Uploads file data directly to S3 using a presigned URL.
     *
     * @param presignedUrl The presigned URL obtained from backend
     * @param data File data as byte array
     * @param contentType MIME type of the file
     * @param onProgress Callback receiving upload progress (0.0 to 1.0)
     * @return Result indicating success or error
     */
    suspend fun uploadToPresignedUrl(
        presignedUrl: String,
        data: ByteArray,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit>

    /**
     * Deletes a file from S3.
     *
     * @param bucket S3 bucket name
     * @param key Object key (path) to delete
     * @return Result indicating success or error
     */
    suspend fun deleteFile(
        bucket: String,
        key: String
    ): Result<Unit>
}

/**
 * Response containing presigned URL information.
 *
 * @property uploadUrl Presigned URL for uploading
 * @property cdnUrl Public CDN URL for accessing the file after upload
 * @property expiresAt Timestamp when the presigned URL expires
 * @property fields Additional fields required for the upload (for multipart form uploads)
 */
data class PresignedUrlResponse(
    val uploadUrl: String,
    val cdnUrl: String,
    val expiresAt: Long,
    val fields: Map<String, String> = emptyMap()
)

/**
 * Configuration for S3 client.
 *
 * @property apiBaseUrl Base URL of the backend API for requesting presigned URLs
 * @property cdnBaseUrl Base URL of the CDN for accessing uploaded files
 * @property defaultBucket Default bucket name for uploads
 * @property maxRetries Maximum number of retry attempts (default: 3)
 * @property retryDelayMs Initial delay between retries in milliseconds (default: 1000)
 */
data class S3ClientConfig(
    val apiBaseUrl: String,
    val cdnBaseUrl: String,
    val defaultBucket: String,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000
)

/**
 * Errors that can occur during S3 operations.
 */
sealed class S3Error : Exception() {
    data class PresignedUrlRequestFailed(val reason: String) : S3Error() {
        override val message: String = "Failed to obtain presigned URL: $reason"
    }

    data class UploadFailed(val statusCode: Int, val reason: String) : S3Error() {
        override val message: String = "S3 upload failed (HTTP $statusCode): $reason"
    }

    data class NetworkError(override val cause: Throwable) : S3Error() {
        override val message: String = "Network error during S3 operation: ${cause.message}"
    }

    data class AuthenticationError(val reason: String) : S3Error() {
        override val message: String = "S3 authentication failed: $reason"
    }

    data class InvalidBucket(val bucket: String) : S3Error() {
        override val message: String = "Invalid S3 bucket: $bucket"
    }

    data class InvalidKey(val key: String) : S3Error() {
        override val message: String = "Invalid S3 key: $key"
    }

    data class RetryExhausted(val attempts: Int, val lastError: Throwable) : S3Error() {
        override val message: String = "S3 operation failed after $attempts retry attempts: ${lastError.message}"
    }
}
