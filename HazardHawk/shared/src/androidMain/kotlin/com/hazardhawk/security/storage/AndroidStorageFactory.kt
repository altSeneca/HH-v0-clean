package com.hazardhawk.security.storage

import android.content.Context
import com.hazardhawk.security.storage.*

/**
 * Factory for creating the complete Android storage fallback system
 * Sets up the cascading fallback chain: Encrypted -> Obfuscated -> Memory -> Manual
 */
object AndroidStorageFactory {
    
    /**
     * Create a complete storage manager with all fallback options
     */
    fun createStorageManager(context: Context): StorageManager {
        val providers = mutableListOf<SecureStorage>()
        
        // Primary: EncryptedSharedPreferences (most secure)
        try {
            val encryptedStorage = EncryptedSecureStorage(context)
            providers.add(encryptedStorage)
        } catch (e: Exception) {
            println("Failed to initialize EncryptedSecureStorage: ${e.message}")
        }
        
        // Fallback 1: Obfuscated SharedPreferences (medium security)
        try {
            val obfuscatedStorage = ObfuscatedSecureStorage(context)
            providers.add(obfuscatedStorage)
        } catch (e: Exception) {
            println("Failed to initialize ObfuscatedSecureStorage: ${e.message}")
        }
        
        // Fallback 2: In-memory storage (low security, session only)
        try {
            val memoryStorage = InMemorySecureStorage()
            providers.add(memoryStorage)
        } catch (e: Exception) {
            println("Failed to initialize InMemorySecureStorage: ${e.message}")
        }
        
        // Emergency: Manual entry (always available)
        try {
            val manualStorage = ManualEntryStorage()
            providers.add(manualStorage)
        } catch (e: Exception) {
            println("Failed to initialize ManualEntryStorage: ${e.message}")
            // This should never happen, but if it does, we have a serious problem
        }
        
        if (providers.isEmpty()) {
            // This should never happen, but provide a final fallback
            providers.add(InMemorySecureStorage())
        }
        
        return StorageManager(providers)
    }
    
    /**
     * Create a storage manager with specific providers (for testing)
     */
    fun createStorageManager(
        context: Context,
        includeEncrypted: Boolean = true,
        includeObfuscated: Boolean = true,
        includeMemory: Boolean = true,
        includeManual: Boolean = true
    ): StorageManager {
        val providers = mutableListOf<SecureStorage>()
        
        if (includeEncrypted) {
            try {
                providers.add(EncryptedSecureStorage(context))
            } catch (e: Exception) {
                println("EncryptedSecureStorage not available: ${e.message}")
            }
        }
        
        if (includeObfuscated) {
            try {
                providers.add(ObfuscatedSecureStorage(context))
            } catch (e: Exception) {
                println("ObfuscatedSecureStorage not available: ${e.message}")
            }
        }
        
        if (includeMemory) {
            try {
                providers.add(InMemorySecureStorage())
            } catch (e: Exception) {
                println("InMemorySecureStorage not available: ${e.message}")
            }
        }
        
        if (includeManual) {
            try {
                providers.add(ManualEntryStorage())
            } catch (e: Exception) {
                println("ManualEntryStorage not available: ${e.message}")
            }
        }
        
        if (providers.isEmpty()) {
            // Final emergency fallback
            providers.add(InMemorySecureStorage())
        }
        
        return StorageManager(providers)
    }
    
    /**
     * Get information about available storage providers
     */
    suspend fun getAvailableStorageInfo(context: Context): List<StorageProviderInfo> {
        val info = mutableListOf<StorageProviderInfo>()
        
        // Test EncryptedSharedPreferences
        try {
            val encrypted = EncryptedSecureStorage(context)
            info.add(StorageProviderInfo(
                level = StorageSecurityLevel.ENCRYPTED_SECURE,
                isAvailable = encrypted.isAvailable,
                isHealthy = if (encrypted.isAvailable) encrypted.healthCheck() else false,
                description = "Android EncryptedSharedPreferences with AES-256 encryption"
            ))
        } catch (e: Exception) {
            info.add(StorageProviderInfo(
                level = StorageSecurityLevel.ENCRYPTED_SECURE,
                isAvailable = false,
                isHealthy = false,
                description = "EncryptedSharedPreferences failed: ${e.message}"
            ))
        }
        
        // Test Obfuscated SharedPreferences
        try {
            val obfuscated = ObfuscatedSecureStorage(context)
            info.add(StorageProviderInfo(
                level = StorageSecurityLevel.OBFUSCATED_MEDIUM,
                isAvailable = obfuscated.isAvailable,
                isHealthy = if (obfuscated.isAvailable) obfuscated.healthCheck() else false,
                description = "Standard SharedPreferences with Base64 obfuscation"
            ))
        } catch (e: Exception) {
            info.add(StorageProviderInfo(
                level = StorageSecurityLevel.OBFUSCATED_MEDIUM,
                isAvailable = false,
                isHealthy = false,
                description = "ObfuscatedStorage failed: ${e.message}"
            ))
        }
        
        // Test Memory Storage
        try {
            val memory = InMemorySecureStorage()
            info.add(StorageProviderInfo(
                level = StorageSecurityLevel.MEMORY_LOW,
                isAvailable = memory.isAvailable,
                isHealthy = memory.healthCheck(),
                description = "In-memory storage with session persistence"
            ))
        } catch (e: Exception) {
            info.add(StorageProviderInfo(
                level = StorageSecurityLevel.MEMORY_LOW,
                isAvailable = false,
                isHealthy = false,
                description = "MemoryStorage failed: ${e.message}"
            ))
        }
        
        // Manual Entry is always available
        try {
            val manual = ManualEntryStorage()
            info.add(StorageProviderInfo(
                level = StorageSecurityLevel.MANUAL_EMERGENCY,
                isAvailable = manual.isAvailable,
                isHealthy = manual.healthCheck(),
                description = "Manual user entry for emergency scenarios"
            ))
        } catch (e: Exception) {
            info.add(StorageProviderInfo(
                level = StorageSecurityLevel.MANUAL_EMERGENCY,
                isAvailable = false,
                isHealthy = false,
                description = "ManualStorage failed: ${e.message}"
            ))
        }
        
        return info
    }
}

/**
 * Information about a storage provider
 */
data class StorageProviderInfo(
    val level: StorageSecurityLevel,
    val isAvailable: Boolean,
    val isHealthy: Boolean,
    val description: String
)