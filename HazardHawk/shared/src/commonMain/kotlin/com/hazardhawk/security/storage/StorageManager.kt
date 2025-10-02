package com.hazardhawk.security.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Storage security levels indicating the type and quality of storage being used
 */
enum class StorageSecurityLevel(val displayName: String, val isSecure: Boolean) {
    ENCRYPTED_SECURE("Encrypted Storage", true),
    OBFUSCATED_MEDIUM("Obfuscated Storage", false), 
    MEMORY_LOW("Memory Storage", false),
    MANUAL_EMERGENCY("Manual Entry Required", false),
    NONE("No Storage Available", false)
}

/**
 * Storage health status for monitoring and diagnostics
 */
data class StorageHealthStatus(
    val isHealthy: Boolean,
    val currentLevel: StorageSecurityLevel,
    val lastSuccessfulOperation: Long,
    val failureCount: Int,
    val errorMessage: String?
)

/**
 * Storage operation result with detailed information
 */
sealed class StorageResult<out T> {
    data class Success<T>(val data: T, val securityLevel: StorageSecurityLevel) : StorageResult<T>()
    data class Failure(val error: Throwable, val securityLevel: StorageSecurityLevel) : StorageResult<Nothing>()
}

/**
 * Core interface for all storage implementations
 */
interface SecureStorage {
    val securityLevel: StorageSecurityLevel
    val isAvailable: Boolean
    
    suspend fun getString(key: String): String?
    suspend fun setString(key: String, value: String): Boolean
    suspend fun remove(key: String): Boolean
    suspend fun clear(): Boolean
    suspend fun contains(key: String): Boolean
    
    /**
     * Health check to verify storage is working correctly
     */
    suspend fun healthCheck(): Boolean
}

/**
 * Comprehensive storage manager with robust fallback system
 * Ensures app never crashes due to storage failures
 */
class StorageManager(
    private val storageProviders: List<SecureStorage>
) {
    
    companion object {
        private const val HEALTH_CHECK_KEY = "_storage_health_test"
        private const val HEALTH_CHECK_VALUE = "healthy"
        private const val MAX_FAILURES_BEFORE_FALLBACK = 3
        private const val HEALTH_CHECK_INTERVAL_MS = 30000L // 30 seconds
    }
    
    private val mutex = Mutex()
    private val failureCounts = mutableMapOf<StorageSecurityLevel, Int>()
    private var currentStorageIndex = 0
    private var lastHealthCheck = 0L
    
    private val _currentSecurityLevel = MutableStateFlow(StorageSecurityLevel.NONE)
    val currentSecurityLevel: StateFlow<StorageSecurityLevel> = _currentSecurityLevel.asStateFlow()
    
    private val _healthStatus = MutableStateFlow(
        StorageHealthStatus(
            isHealthy = false,
            currentLevel = StorageSecurityLevel.NONE,
            lastSuccessfulOperation = 0L,
            failureCount = 0,
            errorMessage = null
        )
    )
    val healthStatus: StateFlow<StorageHealthStatus> = _healthStatus.asStateFlow()
    
    /**
     * Initialize the storage manager and find the best available storage
     */
    suspend fun initialize(): StorageResult<Unit> = mutex.withLock {
        if (storageProviders.isEmpty()) {
            updateHealthStatus(false, StorageSecurityLevel.NONE, "No storage providers available")
            return StorageResult.Failure(
                IllegalStateException("No storage providers configured"),
                StorageSecurityLevel.NONE
            )
        }
        
        // Try to find the best working storage
        for ((index, storage) in storageProviders.withIndex()) {
            if (storage.isAvailable && testStorage(storage)) {
                currentStorageIndex = index
                _currentSecurityLevel.value = storage.securityLevel
                updateHealthStatus(true, storage.securityLevel, null)
                return StorageResult.Success(Unit, storage.securityLevel)
            }
        }
        
        // No storage is working - emergency state
        updateHealthStatus(false, StorageSecurityLevel.NONE, "All storage providers failed")
        return StorageResult.Failure(
            IllegalStateException("No working storage provider found"),
            StorageSecurityLevel.NONE
        )
    }
    
    /**
     * Get a string value with automatic fallback handling
     */
    suspend fun getString(key: String): StorageResult<String?> {
        return executeWithFallback(key) { storage ->
            storage.getString(key)
        }
    }
    
    /**
     * Set a string value with automatic fallback handling
     */
    suspend fun setString(key: String, value: String): StorageResult<Boolean> {
        return executeWithFallback(key) { storage ->
            storage.setString(key, value)
        }
    }
    
    /**
     * Remove a key with automatic fallback handling
     */
    suspend fun remove(key: String): StorageResult<Boolean> {
        return executeWithFallback(key) { storage ->
            storage.remove(key)
        }
    }
    
    /**
     * Check if a key exists with automatic fallback handling
     */
    suspend fun contains(key: String): StorageResult<Boolean> {
        return executeWithFallback(key) { storage ->
            storage.contains(key)
        }
    }
    
    /**
     * Perform health check on current storage and failover if needed
     */
    suspend fun performHealthCheck(): StorageHealthStatus = mutex.withLock {
        val now = Clock.System.now().toEpochMilliseconds()
        
        // Skip if recently checked
        if (now - lastHealthCheck < HEALTH_CHECK_INTERVAL_MS) {
            return _healthStatus.value
        }
        
        lastHealthCheck = now
        
        // Check current storage
        val currentStorage = getCurrentStorage()
        if (currentStorage != null && testStorage(currentStorage)) {
            updateHealthStatus(true, currentStorage.securityLevel, null)
        } else {
            // Current storage failed, try to find alternative
            findWorkingStorage()
        }
        
        return _healthStatus.value
    }
    
    /**
     * Force failover to the next available storage option
     */
    suspend fun forceFailover(): StorageResult<StorageSecurityLevel> = mutex.withLock {
        val originalIndex = currentStorageIndex
        findWorkingStorage()
        
        val newStorage = getCurrentStorage()
        return if (newStorage != null && currentStorageIndex != originalIndex) {
            StorageResult.Success(newStorage.securityLevel, newStorage.securityLevel)
        } else {
            StorageResult.Failure(
                IllegalStateException("No alternative storage available"),
                _currentSecurityLevel.value
            )
        }
    }
    
    /**
     * Get detailed information about all storage providers
     */
    suspend fun getStorageProviderInfo(): List<Pair<StorageSecurityLevel, Boolean>> {
        return storageProviders.map { storage ->
            storage.securityLevel to (storage.isAvailable && testStorage(storage))
        }
    }
    
    private suspend fun <T> executeWithFallback(
        key: String,
        operation: suspend (SecureStorage) -> T
    ): StorageResult<T> = mutex.withLock {
        
        var lastError: Throwable? = null
        val startingIndex = currentStorageIndex
        
        // Try all storage providers starting from current one
        for (attempt in 0 until storageProviders.size) {
            val storage = storageProviders[currentStorageIndex]
            
            if (!storage.isAvailable) {
                // Move to next storage if current is not available
                currentStorageIndex = (currentStorageIndex + 1) % storageProviders.size
                continue
            }
            
            try {
                val result = operation(storage)
                
                // Success - update health status and reset failure count
                _currentSecurityLevel.value = storage.securityLevel
                failureCounts[storage.securityLevel] = 0
                updateHealthStatus(
                    true,
                    storage.securityLevel,
                    null
                )
                
                return StorageResult.Success(result, storage.securityLevel)
                
            } catch (e: Exception) {
                lastError = e
                
                // Increment failure count
                val currentFailures = (failureCounts[storage.securityLevel] ?: 0) + 1
                failureCounts[storage.securityLevel] = currentFailures

                // If too many failures, move to next storage
                if (currentFailures >= MAX_FAILURES_BEFORE_FALLBACK) {
                    currentStorageIndex = (currentStorageIndex + 1) % storageProviders.size
                }
                
                // If we've tried all storages, break
                if (currentStorageIndex == startingIndex && attempt > 0) {
                    break
                }
            }
            
            // Move to next storage for next attempt
            if (attempt < storageProviders.size - 1) {
                currentStorageIndex = (currentStorageIndex + 1) % storageProviders.size
            }
        }
        
        // All storage providers failed
        val errorMessage = "All storage providers failed: ${lastError?.message}"
        updateHealthStatus(false, StorageSecurityLevel.NONE, errorMessage)
        
        return StorageResult.Failure(
            lastError ?: IllegalStateException(errorMessage),
            StorageSecurityLevel.NONE
        )
    }
    
    private suspend fun testStorage(storage: SecureStorage): Boolean {
        return try {
            storage.healthCheck()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getCurrentStorage(): SecureStorage? {
        return if (currentStorageIndex < storageProviders.size) {
            storageProviders[currentStorageIndex]
        } else {
            null
        }
    }
    
    private suspend fun findWorkingStorage() {
        for ((index, storage) in storageProviders.withIndex()) {
            if (storage.isAvailable && testStorage(storage)) {
                currentStorageIndex = index
                _currentSecurityLevel.value = storage.securityLevel
                updateHealthStatus(true, storage.securityLevel, null)
                return
            }
        }
        
        // No working storage found
        updateHealthStatus(false, StorageSecurityLevel.NONE, "No working storage found")
    }
    
    private fun updateHealthStatus(
        isHealthy: Boolean,
        level: StorageSecurityLevel,
        errorMessage: String?
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val currentStatus = _healthStatus.value
        
        _healthStatus.value = StorageHealthStatus(
            isHealthy = isHealthy,
            currentLevel = level,
            lastSuccessfulOperation = if (isHealthy) now else currentStatus.lastSuccessfulOperation,
            failureCount = if (isHealthy) 0 else currentStatus.failureCount + 1,
            errorMessage = errorMessage
        )
    }
}