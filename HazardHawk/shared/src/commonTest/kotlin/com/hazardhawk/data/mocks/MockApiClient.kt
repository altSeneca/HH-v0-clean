package com.hazardhawk.data.mocks

import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.random.Random

/**
 * Configurable mock API client for testing
 * 
 * Features:
 * - Simulate network delays
 * - Configure success/failure scenarios
 * - Track API call history
 * - Response validation
 */
class MockApiClient(
    private val config: MockApiConfig = MockApiConfig()
) {
    private val callHistory = mutableListOf<ApiCall>()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    
    /**
     * Record of an API call for verification in tests
     */
    data class ApiCall(
        val method: String,
        val path: String,
        val body: Any?,
        val headers: Map<String, String>,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Configuration for mock behavior
     */
    data class MockApiConfig(
        val networkDelayMs: LongRange = 50L..200L,
        val failureRate: Double = 0.0, // 0.0 to 1.0
        val shouldReturnErrors: Boolean = false,
        val customResponses: Map<String, Any> = emptyMap(),
        val simulateTimeout: Boolean = false,
        val timeoutAfterMs: Long = 30000L
    )
    
    /**
     * GET request
     */
    suspend inline fun <reified T> get(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): Result<T> {
        return executeRequest("GET", path, null, headers)
    }
    
    /**
     * POST request
     */
    suspend inline fun <reified T> post(
        path: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): Result<T> {
        return executeRequest("POST", path, body, headers)
    }
    
    /**
     * PUT request
     */
    suspend inline fun <reified T> put(
        path: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): Result<T> {
        return executeRequest("PUT", path, body, headers)
    }
    
    /**
     * DELETE request
     */
    suspend inline fun <reified T> delete(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): Result<T> {
        return executeRequest("DELETE", path, null, headers)
    }
    
    /**
     * Execute HTTP request with mock behavior
     */
    suspend inline fun <reified T> executeRequest(
        method: String,
        path: String,
        body: Any?,
        headers: Map<String, String>
    ): Result<T> {
        // Record the call
        callHistory.add(ApiCall(method, path, body, headers))
        
        // Simulate network delay
        val delay = Random.nextLong(config.networkDelayMs.first, config.networkDelayMs.last)
        delay(delay)
        
        // Simulate timeout
        if (config.simulateTimeout && delay > config.timeoutAfterMs) {
            return Result.failure(MockTimeoutException("Request timed out after ${config.timeoutAfterMs}ms"))
        }
        
        // Simulate random failures
        if (config.failureRate > 0 && Random.nextDouble() < config.failureRate) {
            return Result.failure(MockNetworkException("Simulated network failure"))
        }
        
        // Check for custom response
        val customResponse = config.customResponses[path]
        if (customResponse != null) {
            @Suppress("UNCHECKED_CAST")
            return Result.success(customResponse as T)
        }
        
        // Return configured error
        if (config.shouldReturnErrors) {
            return Result.failure(MockApiException("Configured to return errors"))
        }
        
        // Default: return empty response
        return Result.success(getDefaultResponse<T>())
    }
    
    /**
     * Get default response for type T
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> getDefaultResponse(): T {
        return when (T::class.simpleName) {
            "Unit" -> Unit as T
            "String" -> "" as T
            "Int" -> 0 as T
            "Long" -> 0L as T
            "Boolean" -> false as T
            "List" -> emptyList<Any>() as T
            else -> throw IllegalStateException("No default response for type ${T::class.simpleName}")
        }
    }
    
    /**
     * Get call history for verification
     */
    fun getCallHistory(): List<ApiCall> = callHistory.toList()
    
    /**
     * Clear call history
     */
    fun clearHistory() {
        callHistory.clear()
    }
    
    /**
     * Count calls to a specific path
     */
    fun countCalls(path: String): Int {
        return callHistory.count { it.path == path }
    }
    
    /**
     * Get last call to a specific path
     */
    fun getLastCall(path: String): ApiCall? {
        return callHistory.findLast { it.path == path }
    }
    
    /**
     * Verify that a call was made
     */
    fun verifyCalled(method: String, path: String): Boolean {
        return callHistory.any { it.method == method && it.path == path }
    }
}

/**
 * Exception types for mock failures
 */
class MockNetworkException(message: String) : Exception(message)
class MockApiException(message: String) : Exception(message)
class MockTimeoutException(message: String) : Exception(message)
