package com.hazardhawk.data.network

import com.hazardhawk.FeatureFlags
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * HTTP API client for backend communication
 *
 * Features:
 * - Automatic JSON serialization/deserialization
 * - Error handling with typed exceptions
 * - Request/response logging
 * - Authentication token management
 * - Timeout configuration
 */
class ApiClient(
    private val baseUrl: String = FeatureFlags.API_BASE_URL,
    private val authTokenProvider: () -> String? = { null }
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = FeatureFlags.API_LOGGING_ENABLED
    }

    @PublishedApi
    internal val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }

        if (FeatureFlags.API_LOGGING_ENABLED) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = FeatureFlags.API_TIMEOUT_MS
            connectTimeoutMillis = FeatureFlags.API_TIMEOUT_MS
            socketTimeoutMillis = FeatureFlags.API_TIMEOUT_MS
        }

        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }

    /**
     * Perform GET request
     */
    suspend inline fun <reified T> get(
        path: String,
        parameters: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Result<T> {
        return executeRequest {
            httpClient.get(path) {
                applyAuth()
                applyHeaders(headers)
                parameters.forEach { (key, value) ->
                    parameter(key, value)
                }
            }
        }
    }

    /**
     * Perform POST request
     */
    suspend inline fun <reified T> post(
        path: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): Result<T> {
        return executeRequest {
            httpClient.post(path) {
                applyAuth()
                applyHeaders(headers)
                setBody(body)
            }
        }
    }

    /**
     * Perform PATCH request
     */
    suspend inline fun <reified T> patch(
        path: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): Result<T> {
        return executeRequest {
            httpClient.patch(path) {
                applyAuth()
                applyHeaders(headers)
                setBody(body)
            }
        }
    }

    /**
     * Perform DELETE request
     */
    suspend inline fun <reified T> delete(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): Result<T> {
        return executeRequest {
            httpClient.delete(path) {
                applyAuth()
                applyHeaders(headers)
            }
        }
    }

    /**
     * Upload file data directly to a presigned URL.
     * Used for S3 uploads and external file uploads.
     *
     * @param url Full URL including presigned parameters
     * @param data Binary file data to upload
     * @param contentType MIME type of the file
     * @return Result indicating success or failure of upload
     */
    suspend fun uploadFile(
        url: String,
        data: ByteArray,
        contentType: String
    ): Result<Unit> {
        return try {
            val response = httpClient.put(url) {
                setBody(data)
                header(HttpHeaders.ContentType, contentType)
                // Don't send auth header for presigned URLs
                headers.remove(HttpHeaders.Authorization)
            }

            when (response.status) {
                HttpStatusCode.OK,
                HttpStatusCode.Created,
                HttpStatusCode.NoContent -> Result.success(Unit)

                else -> Result.failure(
                    ApiException.NetworkError("Upload failed: ${response.status}")
                )
            }
        } catch (e: Exception) {
            Result.failure(
                ApiException.NetworkError("Upload error: ${e.message}")
            )
        }
    }

    /**
     * Execute request with error handling
     */
    suspend inline fun <reified T> executeRequest(
        crossinline block: suspend () -> HttpResponse
    ): Result<T> {
        return try {
            val response = block()

            when (response.status) {
                HttpStatusCode.OK,
                HttpStatusCode.Created,
                HttpStatusCode.Accepted -> {
                    val body = response.body<T>()
                    Result.success(body)
                }
                HttpStatusCode.NoContent -> {
                    @Suppress("UNCHECKED_CAST")
                    Result.success(Unit as T)
                }
                HttpStatusCode.BadRequest -> {
                    val errorBody = response.bodyAsText()
                    Result.failure(ApiException.BadRequest(errorBody))
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(ApiException.Unauthorized("Authentication required"))
                }
                HttpStatusCode.Forbidden -> {
                    Result.failure(ApiException.Forbidden("Access denied"))
                }
                HttpStatusCode.NotFound -> {
                    Result.failure(ApiException.NotFound("Resource not found"))
                }
                HttpStatusCode.Conflict -> {
                    val errorBody = response.bodyAsText()
                    Result.failure(ApiException.Conflict(errorBody))
                }
                HttpStatusCode.UnprocessableEntity -> {
                    val errorBody = response.bodyAsText()
                    Result.failure(ApiException.ValidationError(errorBody))
                }
                else -> {
                    val errorBody = response.bodyAsText()
                    Result.failure(ApiException.ServerError("Server error: ${response.status.value} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            when (e) {
                is ApiException -> Result.failure(e)
                is kotlinx.coroutines.TimeoutCancellationException -> {
                    Result.failure(ApiException.NetworkError("Request timed out"))
                }
                else -> {
                    Result.failure(ApiException.NetworkError(e.message ?: "Network error occurred"))
                }
            }
        }
    }

    /**
     * Apply authentication token to request
     */
    @PublishedApi
    internal fun HttpRequestBuilder.applyAuth() {
        authTokenProvider()?.let { token ->
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    /**
     * Apply custom headers to request
     */
    @PublishedApi
    internal fun HttpRequestBuilder.applyHeaders(headers: Map<String, String>) {
        headers.forEach { (key, value) ->
            header(key, value)
        }
    }

    /**
     * Close the HTTP client
     */
    fun close() {
        httpClient.close()
    }
}

/**
 * API exception hierarchy for typed error handling
 */
sealed class ApiException(message: String) : Exception(message) {
    class BadRequest(message: String) : ApiException(message)
    class Unauthorized(message: String) : ApiException(message)
    class Forbidden(message: String) : ApiException(message)
    class NotFound(message: String) : ApiException(message)
    class Conflict(message: String) : ApiException(message)
    class ValidationError(message: String) : ApiException(message)
    class ServerError(message: String) : ApiException(message)
    class NetworkError(message: String) : ApiException(message)
}
