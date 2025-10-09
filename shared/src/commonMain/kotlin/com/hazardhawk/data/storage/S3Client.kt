package com.hazardhawk.data.storage

/**
 * Interface for S3-compatible cloud storage operations.
 * Platform-specific implementations should handle the actual AWS SDK integration.
 */
interface S3Client {
    /**
     * Uploads a file to an S3 bucket.
     * Uses chunked upload for large files and provides progress tracking.
     *
     * @param bucket The S3 bucket name
     * @param key The object key (path) in the bucket
     * @param data The file data as a ByteArray
     * @param contentType The MIME type of the file
     * @param onProgress Callback for upload progress (0.0 to 1.0)
     * @return Result containing the CDN URL of the uploaded file, or an error
     */
    suspend fun uploadFile(
        bucket: String,
        key: String,
        data: ByteArray,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String>

    /**
     * Generates a presigned URL for direct upload from client.
     * Useful for large files to avoid proxying through the app server.
     *
     * @param bucket The S3 bucket name
     * @param key The object key (path) in the bucket
     * @param contentType The MIME type of the file
     * @param expirationSeconds How long the presigned URL should be valid (default: 3600s = 1 hour)
     * @return Result containing the presigned upload URL, or an error
     */
    suspend fun generatePresignedUploadUrl(
        bucket: String,
        key: String,
        contentType: String,
        expirationSeconds: Int = 3600
    ): Result<String>

    /**
     * Deletes a file from S3 storage.
     *
     * @param bucket The S3 bucket name
     * @param key The object key (path) in the bucket
     * @return Result indicating success or failure
     */
    suspend fun deleteFile(
        bucket: String,
        key: String
    ): Result<Unit>

    /**
     * Checks if a file exists in S3 storage.
     *
     * @param bucket The S3 bucket name
     * @param key The object key (path) in the bucket
     * @return Result containing true if the file exists, false otherwise
     */
    suspend fun fileExists(
        bucket: String,
        key: String
    ): Result<Boolean>
}
