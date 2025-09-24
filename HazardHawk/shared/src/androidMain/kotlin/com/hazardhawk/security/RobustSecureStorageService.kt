package com.hazardhawk.security

import android.content.Context
import com.hazardhawk.security.storage.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Robust implementation of SecureStorageService with comprehensive fallback system
 * Ensures the app never crashes due to storage failures
 */
class RobustSecureStorageService(private val context: Context) : SecureStorageService {
    
    private val mutex = Mutex()
    private var storageManager: StorageManager? = null
    private var initializationError: Throwable? = null
    
    private val _currentSecurityLevel = MutableStateFlow(StorageSecurityLevel.NONE)
    val currentSecurityLevel: StateFlow<StorageSecurityLevel> = _currentSecurityLevel.asStateFlow()
    
    private val _healthStatus = MutableStateFlow(
        StorageHealthStatus(
            isHealthy = false,
            currentLevel = StorageSecurityLevel.NONE,
            lastSuccessfulOperation = 0L,
            failureCount = 0,
            errorMessage = "Not initialized"
        )
    )
    val healthStatus: StateFlow<StorageHealthStatus> = _healthStatus.asStateFlow()
    
    private val _manualEntryRequired = MutableStateFlow<ManualEntryRequest?>(null)
    val manualEntryRequired: StateFlow<ManualEntryRequest?> = _manualEntryRequired.asStateFlow()
    
    private var isInitialized = false
    
    /**
     * Initialize the robust storage system
     */
    suspend fun initialize(): Result<Unit> = mutex.withLock {
        if (isInitialized) {
            return Result.success(Unit)
        }
        
        return try {
            // Create storage manager with all fallback options
            storageManager = AndroidStorageFactory.createStorageManager(context)
            
            // Initialize the storage manager
            val initResult = storageManager?.initialize()
            
            when (initResult) {
                is StorageResult.Success -> {
                    _currentSecurityLevel.value = initResult.securityLevel
                    isInitialized = true
                    initializationError = null
                    
                    // Subscribe to health status updates
                    subscribeToHealthUpdates()
                    
                    // Subscribe to manual entry requests
                    subscribeToManualEntryRequests()
                    
                    Result.success(Unit)
                }
                is StorageResult.Failure -> {
                    initializationError = initResult.error
                    Result.failure(initResult.error)
                }
                null -> {
                    val error = IllegalStateException("Failed to create storage manager")
                    initializationError = error
                    Result.failure(error)
                }
            }
        } catch (e: Exception) {
            initializationError = e
            Result.failure(e)
        }
    }
    
    override suspend fun getString(key: String): String? {
        return try {
            ensureInitialized()
            
            val result = storageManager?.getString(key)
            
            when (result) {
                is StorageResult.Success -> {
                    _currentSecurityLevel.value = result.securityLevel
                    result.data
                }
                is StorageResult.Failure -> {
                    handleStorageFailure("getString", key, result.error)
                    null
                }
                null -> null
            }
        } catch (e: Exception) {
            handleStorageFailure("getString", key, e)
            null
        }
    }
    
    override suspend fun setString(key: String, value: String): Boolean {
        return try {
            ensureInitialized()
            
            val result = storageManager?.setString(key, value)
            
            when (result) {
                is StorageResult.Success -> {
                    _currentSecurityLevel.value = result.securityLevel
                    result.data
                }
                is StorageResult.Failure -> {
                    handleStorageFailure("setString", key, result.error)
                    false
                }
                null -> false
            }
        } catch (e: Exception) {
            handleStorageFailure("setString", key, e)
            false
        }
    }
    
    /**
     * Remove a key from storage
     */
    suspend fun remove(key: String): Boolean {
        return try {
            ensureInitialized()
            
            val result = storageManager?.remove(key)
            
            when (result) {
                is StorageResult.Success -> {
                    _currentSecurityLevel.value = result.securityLevel
                    result.data
                }
                is StorageResult.Failure -> {
                    handleStorageFailure("remove", key, result.error)
                    false
                }
                null -> false
            }
        } catch (e: Exception) {
            handleStorageFailure("remove", key, e)
            false
        }
    }
    
    /**
     * Check if a key exists in storage
     */
    suspend fun contains(key: String): Boolean {
        return try {
            ensureInitialized()
            
            val result = storageManager?.contains(key)
            
            when (result) {
                is StorageResult.Success -> {
                    _currentSecurityLevel.value = result.securityLevel
                    result.data
                }
                is StorageResult.Failure -> {
                    handleStorageFailure("contains", key, result.error)
                    false
                }
                null -> false
            }
        } catch (e: Exception) {
            handleStorageFailure("contains", key, e)
            false
        }
    }
    
    /**
     * Perform manual health check and failover if needed
     */
    suspend fun performHealthCheck(): StorageHealthStatus {
        return try {
            ensureInitialized()
            storageManager?.performHealthCheck() ?: _healthStatus.value
        } catch (e: Exception) {
            handleStorageFailure("healthCheck", "system", e)
            _healthStatus.value
        }
    }
    
    /**
     * Force failover to next available storage
     */
    suspend fun forceFailover(): StorageResult<StorageSecurityLevel> {
        return try {
            ensureInitialized()
            storageManager?.forceFailover() ?: StorageResult.Failure(
                IllegalStateException("Storage manager not available"),
                StorageSecurityLevel.NONE
            )
        } catch (e: Exception) {
            StorageResult.Failure(e, _currentSecurityLevel.value)
        }
    }
    
    /**
     * Get information about all storage providers
     */
    suspend fun getStorageProviderInfo(): List<StorageProviderInfo> {
        return try {
            AndroidStorageFactory.getAvailableStorageInfo(context)
        } catch (e: Exception) {
            println("Failed to get storage provider info: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Complete a manual entry request
     */
    suspend fun completeManualEntry(requestId: String, value: String): Boolean {
        return try {
            val manualStorage = getManualEntryStorage()
            manualStorage?.completeManualEntry(requestId, value) ?: false
        } catch (e: Exception) {
            println("Failed to complete manual entry: ${e.message}")
            false
        }
    }
    
    /**
     * Skip a manual entry request
     */
    suspend fun skipManualEntry(requestId: String): Boolean {
        return try {
            val manualStorage = getManualEntryStorage()
            manualStorage?.skipManualEntry(requestId) ?: false
        } catch (e: Exception) {
            println("Failed to skip manual entry: ${e.message}")
            false
        }
    }
    
    private suspend fun ensureInitialized() {
        if (!isInitialized) {
            val result = initialize()
            if (result.isFailure) {
                throw initializationError ?: IllegalStateException("Storage not initialized")
            }
        }
    }
    
    private suspend fun subscribeToHealthUpdates() {
        storageManager?.let { manager ->
            // In a real implementation, we would collect from the StateFlow
            // For now, we just update our status periodically
        }
    }
    
    private suspend fun subscribeToManualEntryRequests() {
        val manualStorage = getManualEntryStorage()
        manualStorage?.let { storage ->
            // In a real implementation, we would collect from the StateFlow
            // For now, we handle requests as they come
        }
    }
    
    private suspend fun getManualEntryStorage(): ManualEntryStorage? {
        return try {
            // In a real implementation, we'd have a reference to the ManualEntryStorage
            // from the StorageManager. For now, create a temporary one.
            ManualEntryStorage()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun handleStorageFailure(operation: String, key: String, error: Throwable) {
        println("RobustSecureStorageService.$operation failed for key '$key': ${error.message}")
        
        // Update health status
        val currentStatus = _healthStatus.value
        _healthStatus.value = currentStatus.copy(
            isHealthy = false,
            failureCount = currentStatus.failureCount + 1,
            errorMessage = error.message
        )
    }
}