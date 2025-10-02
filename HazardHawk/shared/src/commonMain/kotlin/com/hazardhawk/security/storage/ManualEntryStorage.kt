package com.hazardhawk.security.storage

import com.hazardhawk.platform.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Emergency storage that requires manual user input
 * Used when all other storage methods fail
 */
class ManualEntryStorage : SecureStorage {
    
    companion object {
        private const val HEALTH_CHECK_KEY = "_manual_health_check"
        private const val HEALTH_CHECK_VALUE = "healthy_manual"
    }
    
    override val securityLevel = StorageSecurityLevel.MANUAL_EMERGENCY
    override val isAvailable = true // Always available as last resort
    
    private val mutex = Mutex()
    private val temporaryStorage = mutableMapOf<String, String>()
    
    // State for UI to know when manual entry is required
    private val _manualEntryRequired = MutableStateFlow<ManualEntryRequest?>(null)
    val manualEntryRequired: StateFlow<ManualEntryRequest?> = _manualEntryRequired.asStateFlow()
    
    private val _pendingRequests = MutableStateFlow<List<ManualEntryRequest>>(emptyList())
    val pendingRequests: StateFlow<List<ManualEntryRequest>> = _pendingRequests.asStateFlow()
    
    override suspend fun getString(key: String): String? = mutex.withLock {
        // First check if we have the value in temporary storage
        val existingValue = temporaryStorage[key]
        if (existingValue != null) {
            return existingValue
        }
        
        // If not found, request manual entry
        val request = ManualEntryRequest(
            id = "get_${key}_${currentTimeMillis()}",
            key = key,
            operation = ManualEntryOperation.GET,
            description = getKeyDescription(key),
            isRequired = isKeyRequired(key)
        )
        
        addPendingRequest(request)
        
        // Return null for now - UI will handle the manual entry
        return null
    }
    
    override suspend fun setString(key: String, value: String): Boolean = mutex.withLock {
        temporaryStorage[key] = value
        return true
    }
    
    override suspend fun remove(key: String): Boolean = mutex.withLock {
        temporaryStorage.remove(key) != null
    }
    
    override suspend fun clear(): Boolean = mutex.withLock {
        temporaryStorage.clear()
        clearAllRequests()
        true
    }
    
    override suspend fun contains(key: String): Boolean = mutex.withLock {
        temporaryStorage.containsKey(key)
    }
    
    override suspend fun healthCheck(): Boolean {
        // Manual entry storage is always "healthy" in the sense that it's available
        return true
    }
    
    /**
     * Complete a manual entry request with user-provided value
     */
    suspend fun completeManualEntry(requestId: String, value: String): Boolean = mutex.withLock {
        val currentRequests = _pendingRequests.value.toMutableList()
        val request = currentRequests.find { it.id == requestId }
        
        if (request != null) {
            // Store the value
            temporaryStorage[request.key] = value
            
            // Remove from pending requests
            currentRequests.removeAll { it.id == requestId }
            _pendingRequests.value = currentRequests
            
            // Clear current request if it matches
            if (_manualEntryRequired.value?.id == requestId) {
                _manualEntryRequired.value = currentRequests.firstOrNull()
            }
            
            return true
        }
        
        return false
    }
    
    /**
     * Cancel a manual entry request
     */
    suspend fun cancelManualEntry(requestId: String): Boolean = mutex.withLock {
        val currentRequests = _pendingRequests.value.toMutableList()
        val wasRemoved = currentRequests.removeAll { it.id == requestId }
        
        if (wasRemoved) {
            _pendingRequests.value = currentRequests
            
            // Clear current request if it matches
            if (_manualEntryRequired.value?.id == requestId) {
                _manualEntryRequired.value = currentRequests.firstOrNull()
            }
        }
        
        return wasRemoved
    }
    
    /**
     * Skip a manual entry request (mark as not required)
     */
    suspend fun skipManualEntry(requestId: String): Boolean = mutex.withLock {
        val currentRequests = _pendingRequests.value.toMutableList()
        val request = currentRequests.find { it.id == requestId }
        
        if (request != null && !request.isRequired) {
            currentRequests.removeAll { it.id == requestId }
            _pendingRequests.value = currentRequests
            
            // Clear current request if it matches
            if (_manualEntryRequired.value?.id == requestId) {
                _manualEntryRequired.value = currentRequests.firstOrNull()
            }
            
            return true
        }
        
        return false
    }
    
    /**
     * Get all stored keys (for debugging)
     */
    suspend fun getStoredKeys(): List<String> = mutex.withLock {
        temporaryStorage.keys.toList()
    }
    
    /**
     * Check if there are any pending manual entry requests
     */
    suspend fun hasPendingRequests(): Boolean = mutex.withLock {
        _pendingRequests.value.isNotEmpty()
    }
    
    private suspend fun addPendingRequest(request: ManualEntryRequest) {
        val currentRequests = _pendingRequests.value.toMutableList()
        
        // Don't add duplicate requests for the same key
        if (currentRequests.none { it.key == request.key && it.operation == request.operation }) {
            currentRequests.add(request)
            _pendingRequests.value = currentRequests
            
            // Set as current request if none exists
            if (_manualEntryRequired.value == null) {
                _manualEntryRequired.value = request
            }
        }
    }
    
    private fun clearAllRequests() {
        _pendingRequests.value = emptyList()
        _manualEntryRequired.value = null
    }
    
    private fun getKeyDescription(key: String): String {
        return when {
            key.contains("api", ignoreCase = true) && key.contains("key", ignoreCase = true) -> 
                "API Key for AI analysis services"
            key.contains("gemini", ignoreCase = true) -> 
                "Google Gemini API Key for photo analysis"
            key.contains("auth", ignoreCase = true) -> 
                "Authentication token"
            key.contains("token", ignoreCase = true) -> 
                "Access token"
            key.contains("secret", ignoreCase = true) -> 
                "Secret key"
            else -> "Configuration value for $key"
        }
    }
    
    private fun isKeyRequired(key: String): Boolean {
        return when {
            key.contains("api", ignoreCase = true) && key.contains("key", ignoreCase = true) -> true
            key.contains("gemini", ignoreCase = true) -> true
            key.contains("auth", ignoreCase = true) -> true
            else -> false
        }
    }
}

/**
 * Manual entry operation types
 */
enum class ManualEntryOperation {
    GET, SET, UPDATE
}

/**
 * Request for manual user input
 */
data class ManualEntryRequest(
    val id: String,
    val key: String,
    val operation: ManualEntryOperation,
    val description: String,
    val isRequired: Boolean,
    val timestamp: Long = currentTimeMillis()
)