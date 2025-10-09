package com.hazardhawk.data.storage

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Platform-agnostic S3 client implementation using Ktor HTTP client.
 * Uses presigned URLs from backend API rather than direct AWS SDK integration.
 * This approach is more secure as credentials stay on the server.
 *
 * @property httpClient The Ktor HTTP client for making requests
 * @property backendApiUrl The backend API URL that generates presigned URLs
 */
class HttpS3Client(
    private val httpClient: HttpClient,
    private val backendApiUrl: String
) : S3Client {

    companion object {
        private const val CHUNK_SIZE = 1024 * 1024 // 1MB chunks for progress tracking
    }

    override suspend fun uploadFile(
        bucket: String,
        key: String,
        data: ByteArray,
        contentType: String,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.Default) {
        try {
            // Step 1: Request presigned URL from backend
            val presignedUrl = generatePresignedUploadUrl(
                bucket = bucket,
                key = key,
                contentType = contentType
            ).getOrElse { error ->
                return@withContext Result.failure(error)
            }

            // Step 2: Upload file directly to S3 using presigned URL
            val response = httpClient.put(presignedUrl) {
                contentType(ContentType.parse(contentType))
                setBody(data)

                // Track upload progress
                onUpload { bytesSentTotal, contentLength ->
                    if (contentLength != null && contentLength > 0) {
                        val progress = bytesSentTotal.toFloat() / contentLength.toFloat()
                        onProgress(progress)
                    }
                }
            }

            if (response.status.isSuccess()) {
                // Step 3: Extract and return the public URL
                val publicUrl = extractPublicUrl(presignedUrl)
                onProgress(1.0f)
                Result.success(publicUrl)
            } else {
                Result.failure(
                    Exception("Upload failed with status: ${response.status.value}")
                )
            }

        } catch (e: Exception) {
            Result.failure(
                Exception("S3 upload via HTTP failed: ${e.message}", e)
            )
        }
    }

    override suspend fun generatePresignedUploadUrl(
        bucket: String,
        key: String,
        contentType: String,
        expirationSeconds: Int
    ): Result<String> = withContext(Dispatchers.Default) {
        try {
            // Request presigned URL from backend API
            val response = httpClient.post("$backendApiUrl/api/storage/presigned-url") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "bucket" to bucket,
                    "key" to key,
                    "contentType" to contentType,
                    "expirationSeconds" to expirationSeconds,
                    "operation" to "PUT"
                ))
            }

            if (response.status.isSuccess()) {
                val presignedUrl = response.bodyAsText()
                Result.success(presignedUrl)
            } else {
                Result.failure(
                    Exception("Failed to generate presigned URL: ${response.status.value}")
                )
            }

        } catch (e: Exception) {
            Result.failure(
                Exception("Failed to request presigned URL: ${e.message}", e)
            )
        }
    }

    override suspend fun deleteFile(
        bucket: String,
        key: String
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // Request deletion via backend API (more secure than direct S3 access)
            val response = httpClient.delete("$backendApiUrl/api/storage/files") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "bucket" to bucket,
                    "key" to key
                ))
            }

            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception("Delete failed with status: ${response.status.value}")
                )
            }

        } catch (e: Exception) {
            Result.failure(
                Exception("Failed to delete file: ${e.message}", e)
            )
        }
    }

    override suspend fun fileExists(
        bucket: String,
        key: String
    ): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            // Check file existence via backend API
            val response = httpClient.head("$backendApiUrl/api/storage/files") {
                parameter("bucket", bucket)
                parameter("key", key)
            }

            Result.success(response.status.isSuccess())

        } catch (e: Exception) {
            Result.failure(
                Exception("Failed to check file existence: ${e.message}", e)
            )
        }
    }

    /**
     * Extracts the public URL from a presigned URL by removing query parameters.
     */
    private fun extractPublicUrl(presignedUrl: String): String {
        return presignedUrl.substringBefore("?")
    }
}
