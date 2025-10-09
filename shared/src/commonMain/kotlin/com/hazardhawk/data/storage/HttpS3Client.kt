package com.hazardhawk.data.storage

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * HTTP-based implementation of S3Client using presigned URLs.
 *
 * Security-first approach:
 * - No AWS credentials stored in client
 * - Uses backend API to generate presigned URLs
 * - Direct upload to S3 using HTTP PUT
 *
 * Features:
 * - Automatic retry logic (3 attempts, exponential backoff)
 * - Progress tracking
 * - Error handling with descriptive Result types
 */
class HttpS3Client(
    private val httpClient: HttpClient,
    private val config: S3ClientConfig,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : S3Client {

    companion object {
        private const val PRESIGNED_URL_ENDPOINT = "/api/v1/storage/presigned-url"
        private const val DELETE_FILE_ENDPOINT = "/api/v1/storage/delete"
    }

    override suspend fun uploadFile(
        bucket: String,
        key: String,
        data: ByteArray,
        contentType: String,
        onProgress: (Float) -> Unit
    ): Result<String> {
        return retryWithExponentialBackoff { attemptNumber ->
            try {
                // Step 1: Request presigned URL from backend
                onProgress(0.1f)
                val presignedResult = getPresignedUploadUrl(bucket, key, contentType)
                if (presignedResult.isFailure) {
                    return@retryWithExponentialBackoff presignedResult.exceptionOrNull()?.let { Result.failure<String>(it) }
                        ?: Result.failure(S3Error.PresignedUrlRequestFailed("Unknown error"))
                }

                val presignedResponse = presignedResult.getOrThrow()

                // Step 2: Upload to S3 using presigned URL
                onProgress(0.2f)
                val uploadResult = uploadToPresignedUrl(
                    presignedResponse.uploadUrl,
                    data,
                    contentType
                ) { progress ->
                    // Map upload progress to overall progress (20% - 90%)
                    onProgress(0.2f + (progress * 0.7f))
                }

                if (uploadResult.isFailure) {
                    return@retryWithExponentialBackoff uploadResult.exceptionOrNull()?.let { Result.failure<String>(it) }
                        ?: Result.failure(S3Error.UploadFailed(0, "Unknown error"))
                }

                // Step 3: Return CDN URL
                onProgress(1.0f)
                Result.success(presignedResponse.cdnUrl)
            } catch (e: Exception) {
                Result.failure(S3Error.NetworkError(e))
            }
        }
    }

    override suspend fun getPresignedUploadUrl(
        bucket: String,
        key: String,
        contentType: String,
        expirationSeconds: Int
    ): Result<PresignedUrlResponse> {
        return try {
            val response = httpClient.post("${config.apiBaseUrl}$PRESIGNED_URL_ENDPOINT") {
                contentType(ContentType.Application.Json)
                setBody(
                    json.encodeToString(
                        PresignedUrlRequest.serializer(),
                        PresignedUrlRequest(
                            bucket = bucket,
                            key = key,
                            contentType = contentType,
                            expirationSeconds = expirationSeconds
                        )
                    )
                )
            }

            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                val presignedResponse = json.decodeFromString(
                    PresignedUrlResponseDto.serializer(),
                    responseBody
                )
                Result.success(
                    PresignedUrlResponse(
                        uploadUrl = presignedResponse.uploadUrl,
                        cdnUrl = presignedResponse.cdnUrl,
                        expiresAt = presignedResponse.expiresAt,
                        fields = presignedResponse.fields
                    )
                )
            } else {
                Result.failure(
                    S3Error.PresignedUrlRequestFailed(
                        "HTTP ${response.status.value}: ${response.bodyAsText()}"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(S3Error.NetworkError(e))
        }
    }

    override suspend fun uploadToPresignedUrl(
        presignedUrl: String,
        data: ByteArray,
        contentType: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> {
        return try {
            val response = httpClient.put(presignedUrl) {
                contentType(ContentType.parse(contentType))
                setBody(data)

                // Note: Ktor doesn't provide built-in upload progress tracking
                // For production, consider implementing custom channel-based progress tracking
                onProgress(0.5f) // Midpoint approximation
            }

            if (response.status.isSuccess() || response.status == HttpStatusCode.OK) {
                onProgress(1.0f)
                Result.success(Unit)
            } else {
                Result.failure(
                    S3Error.UploadFailed(
                        response.status.value,
                        response.bodyAsText()
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(S3Error.NetworkError(e))
        }
    }

    override suspend fun deleteFile(
        bucket: String,
        key: String
    ): Result<Unit> {
        return retryWithExponentialBackoff { _ ->
            try {
                val response = httpClient.delete("${config.apiBaseUrl}$DELETE_FILE_ENDPOINT") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        json.encodeToString(
                            DeleteFileRequest.serializer(),
                            DeleteFileRequest(bucket = bucket, key = key)
                        )
                    )
                }

                if (response.status.isSuccess()) {
                    Result.success(Unit)
                } else {
                    Result.failure(
                        S3Error.UploadFailed(
                            response.status.value,
                            "Delete failed: ${response.bodyAsText()}"
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(S3Error.NetworkError(e))
            }
        }
    }

    /**
     * Retries an operation with exponential backoff.
     *
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelayMs Initial delay between retries in milliseconds
     * @param operation The operation to retry
     * @return Result of the operation
     */
    private suspend fun <T> retryWithExponentialBackoff(
        maxRetries: Int = config.maxRetries,
        initialDelayMs: Long = config.retryDelayMs,
        operation: suspend (attemptNumber: Int) -> Result<T>
    ): Result<T> {
        var currentDelay = initialDelayMs
        var lastError: Throwable? = null

        repeat(maxRetries) { attempt ->
            val result = operation(attempt + 1)
            if (result.isSuccess) {
                return result
            }

            lastError = result.exceptionOrNull()

            // Don't retry on authentication errors
            if (lastError is S3Error.AuthenticationError) {
                return result
            }

            // Wait before retrying (exponential backoff)
            if (attempt < maxRetries - 1) {
                delay(currentDelay)
                currentDelay *= 2 // Exponential backoff
            }
        }

        return Result.failure(
            S3Error.RetryExhausted(
                maxRetries,
                lastError ?: Exception("Unknown error")
            )
        )
    }
}

/**
 * Internal DTOs for API communication
 */
@Serializable
private data class PresignedUrlRequest(
    val bucket: String,
    val key: String,
    val contentType: String,
    val expirationSeconds: Int
)

@Serializable
private data class PresignedUrlResponseDto(
    val uploadUrl: String,
    val cdnUrl: String,
    val expiresAt: Long,
    val fields: Map<String, String> = emptyMap()
)

@Serializable
private data class DeleteFileRequest(
    val bucket: String,
    val key: String
)
