package com.hazardhawk.security.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Emergency fallback storage that keeps data in memory only
 * Provides session-based persistence but data is lost on app restart
 */
class InMemorySecureStorage : SecureStorage {
    
    companion object {
        private const val HEALTH_CHECK_KEY = "_memory_health_check"
        private const val HEALTH_CHECK_VALUE = "healthy_memory"
        private const val MAX_STORAGE_SIZE = 1024 * 1024 // 1MB limit for memory storage
    }
    
    override val securityLevel = StorageSecurityLevel.MEMORY_LOW
    override val isAvailable = true // Memory storage is always available
    
    private val storage = mutableMapOf<String, String>()
    private val mutex = Mutex()
    private var creationTime = Clock.System.now().toEpochMilliseconds()
    private var lastAccessTime = Clock.System.now().toEpochMilliseconds()
    
    // Session persistence data - can be serialized/deserialized if needed
    private val sessionData = mutableMapOf<String, String>()
    private var isSessionPersistenceEnabled = true
    
    override suspend fun getString(key: String): String? = mutex.withLock {
        updateAccessTime()
        
        // Check in-memory storage first
        var value = storage[key]
        
        // If not found and session persistence enabled, check session data
        if (value == null && isSessionPersistenceEnabled) {
            value = sessionData[key]
            // If found in session, restore to active storage
            if (value != null) {
                storage[key] = value
            }
        }
        
        return value
    }
    
    override suspend fun setString(key: String, value: String): Boolean = mutex.withLock {
        updateAccessTime()
        
        // Check memory limit
        val currentSize = calculateStorageSize()
        val valueSize = key.length + value.length
        
        if (currentSize + valueSize > MAX_STORAGE_SIZE) {
            // Try to free some space by removing oldest entries
            if (!freeMemorySpace(valueSize)) {
                println("InMemorySecureStorage: Memory limit exceeded, cannot store key '$key'")
                return false
            }
        }
        
        storage[key] = value
        
        // Also store in session data for persistence
        if (isSessionPersistenceEnabled) {
            sessionData[key] = value
        }
        
        return true
    }
    
    override suspend fun remove(key: String): Boolean = mutex.withLock {
        updateAccessTime()
        
        val wasRemoved = storage.remove(key) != null
        
        // Also remove from session data
        if (isSessionPersistenceEnabled) {
            sessionData.remove(key)
        }
        
        return wasRemoved
    }
    
    override suspend fun clear(): Boolean = mutex.withLock {
        updateAccessTime()
        
        storage.clear()
        
        if (isSessionPersistenceEnabled) {
            sessionData.clear()
        }
        
        return true
    }
    
    override suspend fun contains(key: String): Boolean = mutex.withLock {
        updateAccessTime()
        
        return storage.containsKey(key) || 
               (isSessionPersistenceEnabled && sessionData.containsKey(key))
    }
    
    override suspend fun healthCheck(): Boolean = mutex.withLock {
        return try {
            // Memory storage is always healthy if we can perform basic operations
            val testKey = HEALTH_CHECK_KEY
            val testValue = "${HEALTH_CHECK_VALUE}_${Clock.System.now().toEpochMilliseconds()}"
            
            // Test write
            storage[testKey] = testValue
            
            // Test read
            val readValue = storage[testKey]
            val isHealthy = readValue == testValue
            
            // Clean up
            storage.remove(testKey)
            
            isHealthy
        } catch (e: Exception) {
            println("InMemorySecureStorage health check failed: ${e.message}")
            false
        }
    }
    
    /**
     * Get session information for diagnostics
     */
    suspend fun getSessionInfo(): SessionInfo = mutex.withLock {
        return SessionInfo(
            creationTime = creationTime,
            lastAccessTime = lastAccessTime,
            storageSize = calculateStorageSize(),
            keyCount = storage.size,
            sessionDataCount = if (isSessionPersistenceEnabled) sessionData.size else 0,
            memoryUsageBytes = calculateStorageSize()
        )
    }
    
    /**
     * Enable or disable session persistence
     */
    suspend fun setSessionPersistenceEnabled(enabled: Boolean) = mutex.withLock {
        isSessionPersistenceEnabled = enabled
        if (!enabled) {
            sessionData.clear()
        }
    }
    
    /**
     * Manually save current storage to session data
     */
    suspend fun saveToSession() = mutex.withLock {
        if (isSessionPersistenceEnabled) {
            sessionData.clear()
            sessionData.putAll(storage)
        }
    }
    
    /**
     * Manually restore from session data
     */
    suspend fun restoreFromSession() = mutex.withLock {
        if (isSessionPersistenceEnabled && sessionData.isNotEmpty()) {
            storage.putAll(sessionData)
        }
    }
    
    /**
     * Get all stored keys for debugging
     */
    suspend fun getAllKeys(): List<String> = mutex.withLock {
        return (storage.keys + sessionData.keys).distinct()
    }
    
    private fun updateAccessTime() {
        lastAccessTime = Clock.System.now().toEpochMilliseconds()
    }
    
    private fun calculateStorageSize(): Int {
        return storage.entries.sumOf { (key, value) ->
            key.length + value.length
        }
    }
    
    private fun freeMemorySpace(requiredSpace: Int): Boolean {
        // Simple LRU-like approach - this is basic since we don't track individual access times
        // In a more sophisticated implementation, we'd track access times per key
        
        val entries = storage.entries.toList()
        var freedSpace = 0
        
        // Remove entries until we have enough space (simple approach)
        for ((key, value) in entries) {
            if (freedSpace >= requiredSpace) {
                break
            }
            
            storage.remove(key)
            sessionData.remove(key) // Also remove from session data
            freedSpace += key.length + value.length
        }
        
        return freedSpace >= requiredSpace
    }
}

/**
 * Session information for diagnostics and monitoring
 */
data class SessionInfo(
    val creationTime: Long,
    val lastAccessTime: Long,
    val storageSize: Int,
    val keyCount: Int,
    val sessionDataCount: Int,
    val memoryUsageBytes: Int
)