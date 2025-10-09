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
     * Get default response for type T based on the current request context
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> getDefaultResponse(): T {
        val lastCall = callHistory.lastOrNull()
        val path = lastCall?.path ?: ""

        // Generate smart responses based on endpoint
        return when {
            // Expiration reminder endpoints
            path.contains("/send-expiration-reminder") && !path.contains("bulk") -> {
                generateExpirationReminderResponse() as T
            }
            path.contains("/send-bulk-expiration-reminders") -> {
                generateBulkReminderResponse() as T
            }
            // CSV import endpoint
            path.contains("/bulk-import") -> {
                generateCSVImportResponse() as T
            }
            // Search endpoint
            path.contains("/search") -> {
                generateSearchResponse() as T
            }
            // Certification response
            path.contains("/certifications") && lastCall?.method == "POST" -> {
                generateCertificationResponse() as T
            }
            // Default fallbacks
            T::class.simpleName == "Unit" -> Unit as T
            T::class.simpleName == "String" -> "" as T
            T::class.simpleName == "Int" -> 0 as T
            T::class.simpleName == "Long" -> 0L as T
            T::class.simpleName == "Boolean" -> false as T
            T::class.simpleName == "List" -> emptyList<Any>() as T
            else -> throw IllegalStateException("No default response for type ${T::class.simpleName} at path $path")
        }
    }

    /**
     * Generate mock expiration reminder response
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateExpirationReminderResponse(): Any {
        return mapOf(
            "certificationId" to "cert_${Random.nextInt(1000)}",
            "workerName" to "John Doe",
            "certificationType" to "OSHA 10",
            "expirationDate" to "2025-12-31",
            "sentChannels" to listOf("EMAIL"),
            "failedChannels" to emptyList<String>(),
            "sentAt" to "2025-10-09T10:00:00Z"
        )
    }

    /**
     * Generate mock bulk reminder response
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateBulkReminderResponse(): Any {
        val count = 3
        val results = (1..count).map {
            mapOf(
                "certificationId" to "cert_$it",
                "workerName" to "Worker $it",
                "certificationType" to "OSHA ${if (it % 2 == 0) "10" else "30"}",
                "expirationDate" to "2025-12-31",
                "sentChannels" to listOf("EMAIL"),
                "failedChannels" to emptyList<String>(),
                "sentAt" to "2025-10-09T10:00:00Z"
            )
        }
        return mapOf(
            "totalRequested" to count,
            "successCount" to count,
            "failureCount" to 0,
            "results" to results
        )
    }

    /**
     * Generate mock CSV import response
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateCSVImportResponse(): Any {
        val certs = listOf(
            mapOf(
                "id" to "cert_001",
                "workerProfileId" to "worker_1",
                "companyId" to "company_123",
                "certificationTypeId" to "type_osha_10",
                "certificationNumber" to "OSHA10-001",
                "issueDate" to "2025-01-15",
                "expirationDate" to "2030-01-15",
                "issuingAuthority" to "OSHA Training Provider",
                "documentUrl" to "https://cdn.example.com/cert_001.pdf",
                "thumbnailUrl" to null,
                "status" to "VERIFIED",
                "verifiedBy" to null,
                "verifiedAt" to null,
                "rejectionReason" to null,
                "ocrConfidence" to null,
                "createdAt" to "2025-10-09T10:00:00Z",
                "updatedAt" to "2025-10-09T10:00:00Z",
                "certificationType" to null
            ),
            mapOf(
                "id" to "cert_002",
                "workerProfileId" to "worker_2",
                "companyId" to "company_123",
                "certificationTypeId" to "type_osha_30",
                "certificationNumber" to "OSHA30-002",
                "issueDate" to "2025-02-20",
                "expirationDate" to "2030-02-20",
                "issuingAuthority" to "OSHA Training Provider",
                "documentUrl" to "https://cdn.example.com/cert_002.pdf",
                "thumbnailUrl" to null,
                "status" to "VERIFIED",
                "verifiedBy" to null,
                "verifiedAt" to null,
                "rejectionReason" to null,
                "ocrConfidence" to null,
                "createdAt" to "2025-10-09T10:00:00Z",
                "updatedAt" to "2025-10-09T10:00:00Z",
                "certificationType" to null
            )
        )
        return mapOf(
            "totalRows" to certs.size,
            "successCount" to certs.size,
            "errorCount" to 0,
            "errors" to emptyList<Map<String, Any>>(),
            "createdCertifications" to certs
        )
    }

    /**
     * Generate mock search response
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateSearchResponse(): Any {
        val certs = listOf(
            mapOf(
                "id" to "cert_search_1",
                "workerProfileId" to "worker_1",
                "companyId" to "company_123",
                "certificationTypeId" to "type_osha_10",
                "certificationNumber" to "OSHA10-001",
                "issueDate" to "2025-01-15",
                "expirationDate" to "2025-06-15",
                "issuingAuthority" to "OSHA Training Provider",
                "documentUrl" to "https://cdn.example.com/cert_001.pdf",
                "thumbnailUrl" to null,
                "status" to "VERIFIED",
                "verifiedBy" to "admin_1",
                "verifiedAt" to "2025-01-16T10:00:00Z",
                "rejectionReason" to null,
                "ocrConfidence" to 0.95,
                "createdAt" to "2025-01-15T10:00:00Z",
                "updatedAt" to "2025-01-16T10:00:00Z",
                "certificationType" to null
            ),
            mapOf(
                "id" to "cert_search_2",
                "workerProfileId" to "worker_2",
                "companyId" to "company_123",
                "certificationTypeId" to "type_osha_30",
                "certificationNumber" to "OSHA30-002",
                "issueDate" to "2025-02-20",
                "expirationDate" to "2025-08-20",
                "issuingAuthority" to "OSHA Training Provider",
                "documentUrl" to "https://cdn.example.com/cert_002.pdf",
                "thumbnailUrl" to null,
                "status" to "VERIFIED",
                "verifiedBy" to "admin_1",
                "verifiedAt" to "2025-02-21T10:00:00Z",
                "rejectionReason" to null,
                "ocrConfidence" to 0.92,
                "createdAt" to "2025-02-20T10:00:00Z",
                "updatedAt" to "2025-02-21T10:00:00Z",
                "certificationType" to null
            )
        )
        return mapOf(
            "data" to certs,
            "pagination" to mapOf(
                "nextCursor" to null,
                "hasMore" to false,
                "totalCount" to certs.size
            )
        )
    }

    /**
     * Generate mock certification response
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateCertificationResponse(): Any {
        return mapOf(
            "id" to "cert_${Random.nextInt(1000)}",
            "workerProfileId" to "worker_123",
            "companyId" to "company_456",
            "certificationTypeId" to "type_osha_10",
            "certificationNumber" to "OSHA10-2025-123456",
            "issueDate" to "2025-01-15",
            "expirationDate" to "2030-01-15",
            "issuingAuthority" to "OSHA Training Provider",
            "documentUrl" to "https://cdn.example.com/cert.pdf",
            "thumbnailUrl" to null,
            "status" to "PENDING_VERIFICATION",
            "verifiedBy" to null,
            "verifiedAt" to null,
            "rejectionReason" to null,
            "ocrConfidence" to null,
            "createdAt" to "2025-10-09T10:00:00Z",
            "updatedAt" to "2025-10-09T10:00:00Z",
            "certificationType" to null
        )
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
