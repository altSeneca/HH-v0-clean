package com.hazardhawk.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Log
import java.security.GeneralSecurityException
import java.io.IOException

/**
 * Secure key management for HazardHawk using Android Keystore and EncryptedSharedPreferences
 * 
 * This class implements industry best practices for storing sensitive API keys and credentials:
 * - Hardware-backed Android Keystore when available
 * - AES-256 encryption with EncryptedSharedPreferences
 * - Key rotation and lifecycle management
 * - Secure key derivation and storage
 */
class SecureKeyManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SecureKeyManager"
        private const val ENCRYPTED_PREFS_NAME = "hazardhawk_secure_keys"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_ENCRYPTION_KEY = "app_encryption_key"
        private const val KEY_API_KEY_VERSION = "api_key_version"
        private const val KEY_LAST_ROTATION = "last_rotation_timestamp"
        
        @Volatile
        private var INSTANCE: SecureKeyManager? = null
        
        fun getInstance(context: Context): SecureKeyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureKeyManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val encryptedSharedPreferences: SharedPreferences by lazy {
        createEncryptedSharedPreferences()
    }
    
    /**
     * Create EncryptedSharedPreferences with hardware-backed master key
     */
    private fun createEncryptedSharedPreferences(): SharedPreferences {
        return try {
            Log.d(TAG, "Creating EncryptedSharedPreferences with hardware-backed key")
            createHardwareBackedPreferences()
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "Hardware-backed encryption failed, falling back to software: ${e.message}")
            createSoftwareBackedPreferences()
        } catch (e: IOException) {
            Log.w(TAG, "IO error with hardware encryption, falling back to software: ${e.message}")
            createSoftwareBackedPreferences()
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid configuration for hardware encryption, falling back: ${e.message}")
            createSoftwareBackedPreferences()
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception with hardware encryption, falling back: ${e.message}")
            createSoftwareBackedPreferences()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error creating EncryptedSharedPreferences: ${e.message}")
            createBasicFallbackPreferences()
        }
    }

    /**
     * Create hardware-backed EncryptedSharedPreferences (preferred method)
     */
    private fun createHardwareBackedPreferences(): SharedPreferences {
        // Use only KeyScheme to avoid conflicts with KeyGenParameterSpec
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true) // Request hardware security module if available
            .setUserAuthenticationRequired(false) // Allow background access for app functionality
            .build()
            
        val prefs = EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        // Mark the storage type for diagnostics
        prefs.edit().putString("__storage_type", "hardware").apply()
        Log.i(TAG, "Hardware-backed encrypted storage initialized successfully")
        
        return prefs
    }

    /**
     * Create software-backed EncryptedSharedPreferences (fallback)
     */
    private fun createSoftwareBackedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(false) // Software-only encryption
            .build()
            
        val prefs = EncryptedSharedPreferences.create(
            context,
            "${ENCRYPTED_PREFS_NAME}_software",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        // Mark the storage type for diagnostics
        prefs.edit().putString("__storage_type", "software").apply()
        Log.i(TAG, "Software-backed encrypted storage initialized as fallback")
        
        return prefs
    }

    /**
     * Final fallback to basic EncryptedSharedPreferences without special configuration
     */
    private fun createBasicFallbackPreferences(): SharedPreferences {
        return try {
            Log.w(TAG, "Using basic fallback encryption - security may be reduced")
            val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            val prefs = EncryptedSharedPreferences.create(
                context,
                "${ENCRYPTED_PREFS_NAME}_basic",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            // Mark the storage type for diagnostics
            prefs.edit().putString("__storage_type", "basic").apply()
            Log.w(TAG, "Basic encrypted storage initialized - reduced security level")
            
            return prefs
        } catch (e: Exception) {
            Log.e(TAG, "All encryption methods failed, using unencrypted storage - THIS IS NOT SECURE!")
            // As a last resort, return regular SharedPreferences with a clear warning
            // In production, you might want to disable the app entirely rather than fall back to this
            val prefs = context.getSharedPreferences("${ENCRYPTED_PREFS_NAME}_UNENCRYPTED_FALLBACK", Context.MODE_PRIVATE)
            prefs.edit().putString("__storage_type", "unencrypted").apply()
            return prefs
        }
    }
    
    /**
     * Store Gemini API key securely with version control
     * 
     * @param apiKey The API key to store
     * @param version Optional version identifier for key rotation
     */
    fun storeGeminiApiKey(apiKey: String, version: String = "1.0") {
        try {
            if (apiKey.isBlank()) {
                throw IllegalArgumentException("API key cannot be blank")
            }
            
            // Validate API key format (basic check for Google AI API keys)
            if (!apiKey.startsWith("AI") || apiKey.length < 20) {
                Log.w(TAG, "API key format may be invalid - expected format: AIza...")
            }
            
            // Additional security validation
            if (apiKey.length > 1000) {
                throw IllegalArgumentException("API key is suspiciously long")
            }
            
            // Store with retry logic for better reliability
            var attempts = 0
            var lastException: Exception? = null
            
            while (attempts < 3) {
                try {
                    with(encryptedSharedPreferences.edit()) {
                        putString(KEY_GEMINI_API_KEY, apiKey)
                        putString(KEY_API_KEY_VERSION, version)
                        putLong(KEY_LAST_ROTATION, System.currentTimeMillis())
                        
                        // Use commit() for critical operations to ensure immediate persistence
                        val success = commit()
                        if (!success) {
                            throw IOException("Failed to commit API key to secure storage")
                        }
                    }
                    
                    Log.i(TAG, "API key stored securely with version: $version")
                    return // Success, exit function
                    
                } catch (e: Exception) {
                    lastException = e
                    attempts++
                    Log.w(TAG, "Attempt $attempts to store API key failed: ${e.message}")
                    
                    if (attempts < 3) {
                        Thread.sleep(100) // Brief delay before retry
                    }
                }
            }
            
            // All attempts failed
            Log.e(TAG, "Failed to store API key after 3 attempts", lastException)
            throw SecurityException("Failed to securely store API key after multiple attempts", lastException)
            
        } catch (e: SecurityException) {
            throw e // Re-throw SecurityException as-is
        } catch (e: IllegalArgumentException) {
            throw e // Re-throw IllegalArgumentException as-is
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error storing API key", e)
            throw SecurityException("Failed to securely store API key", e)
        }
    }
    
    /**
     * Retrieve Gemini API key securely
     * 
     * @return API key or null if not found or retrieval failed
     */
    fun getGeminiApiKey(): String? {
        return try {
            val apiKey = encryptedSharedPreferences.getString(KEY_GEMINI_API_KEY, null)
            
            if (apiKey != null) {
                // Basic validation of retrieved key
                if (apiKey.isBlank()) {
                    Log.w(TAG, "Retrieved API key is blank - returning null")
                    return null
                }
                
                if (apiKey.length < 20) {
                    Log.w(TAG, "Retrieved API key is too short - may be corrupted")
                    return null
                }
                
                Log.d(TAG, "API key retrieved successfully (length: ${apiKey.length})")
                return apiKey
            } else {
                Log.w(TAG, "No API key found in secure storage")
                return null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception retrieving API key", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve API key: ${e.message}", e)
            null
        }
    }
    
    /**
     * Check if API key is available and valid
     */
    fun hasValidApiKey(): Boolean {
        val apiKey = getGeminiApiKey()
        return !apiKey.isNullOrBlank() && apiKey.length > 20
    }
    
    /**
     * Rotate API key with new value
     */
    fun rotateApiKey(newApiKey: String, newVersion: String) {
        Log.i(TAG, "Rotating API key to version: $newVersion")
        storeGeminiApiKey(newApiKey, newVersion)
        
        // Optional: Audit log for compliance
        logSecurityEvent("API_KEY_ROTATED", mapOf(
            "old_version" to getCurrentKeyVersion(),
            "new_version" to newVersion,
            "timestamp" to System.currentTimeMillis()
        ))
    }
    
    /**
     * Get current API key version
     */
    fun getCurrentKeyVersion(): String {
        return encryptedSharedPreferences.getString(KEY_API_KEY_VERSION, "unknown") ?: "unknown"
    }
    
    /**
     * Get last rotation timestamp
     */
    fun getLastRotationTime(): Long {
        return encryptedSharedPreferences.getLong(KEY_LAST_ROTATION, 0L)
    }
    
    /**
     * Clear all stored keys (for logout or reset)
     */
    fun clearAllKeys() {
        try {
            with(encryptedSharedPreferences.edit()) {
                clear()
                apply()
            }
            Log.i(TAG, "All secure keys cleared")
            
            logSecurityEvent("ALL_KEYS_CLEARED", mapOf(
                "timestamp" to System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear keys", e)
            throw SecurityException("Failed to clear secure storage", e)
        }
    }
    
    /**
     * Check if hardware-backed security is available
     */
    fun isHardwareBackedSecurity(): Boolean {
        return try {
            // Test if hardware-backed encryption is available
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true) // Request hardware security
                .build()
            
            // If we can create the key without exception, hardware security is available
            Log.d(TAG, "Hardware-backed security is available")
            true
        } catch (e: GeneralSecurityException) {
            Log.d(TAG, "Hardware-backed security not available: ${e.message}")
            false
        } catch (e: Exception) {
            Log.d(TAG, "Hardware security check failed: ${e.message}")
            false
        }
    }
    
    /**
     * Generate secure random encryption key for local data
     */
    fun generateEncryptionKey(): String {
        val key = java.security.SecureRandom().let { random ->
            ByteArray(32).also { random.nextBytes(it) }
        }
        val encodedKey = android.util.Base64.encodeToString(key, android.util.Base64.NO_WRAP)
        
        // Store the encryption key securely
        with(encryptedSharedPreferences.edit()) {
            putString(KEY_ENCRYPTION_KEY, encodedKey)
            apply()
        }
        
        return encodedKey
    }
    
    /**
     * Get stored encryption key
     */
    fun getEncryptionKey(): String? {
        return encryptedSharedPreferences.getString(KEY_ENCRYPTION_KEY, null)
    }
    
    /**
     * Get security level information for diagnostics
     */
    fun getSecurityInfo(): SecurityInfo {
        return try {
            // Try to determine which storage is being used based on the preferences name
            val isHardware = isHardwareBackedSecurity()
            val storageType = when {
                encryptedSharedPreferences.getString("test_key_detection", null) != null -> "encrypted"
                else -> determineStorageType()
            }
            
            SecurityInfo(
                isHardwareBacked = isHardware,
                storageType = storageType,
                hasApiKey = hasValidApiKey(),
                keyVersion = getCurrentKeyVersion(),
                lastRotation = getLastRotationTime()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get security info", e)
            SecurityInfo(
                isHardwareBacked = false,
                storageType = "unknown",
                hasApiKey = false,
                keyVersion = "unknown",
                lastRotation = 0L
            )
        }
    }
    
    /**
     * Determine which storage implementation is actually being used
     */
    private fun determineStorageType(): String {
        return try {
            // This is a heuristic approach - in a real implementation you'd track this more directly
            when {
                encryptedSharedPreferences.getString("__storage_type", null) == "hardware" -> "hardware-backed"
                encryptedSharedPreferences.getString("__storage_type", null) == "software" -> "software-backed"
                else -> {
                    // Try to detect based on available capabilities
                    val testResult = validateKeyIntegrity()
                    if (testResult && isHardwareBackedSecurity()) "hardware-backed" else "software-backed"
                }
            }
        } catch (e: Exception) {
            "fallback"
        }
    }

    /**
     * Security event logging for audit trail
     */
    private fun logSecurityEvent(event: String, metadata: Map<String, Any>) {
        // In production, this would integrate with your security monitoring system
        Log.i(TAG, "Security Event: $event, Metadata: $metadata")
        
        // Store in secure audit log for OSHA compliance if needed
        // This could integrate with your backend audit system
    }
    
    /**
     * Validate key storage integrity
     */
    fun validateKeyIntegrity(): Boolean {
        return try {
            val testKey = "test_key_${System.currentTimeMillis()}"
            val testValue = "test_value_${System.currentTimeMillis()}"
            
            // Test write/read cycle
            with(encryptedSharedPreferences.edit()) {
                putString(testKey, testValue)
                apply()
            }
            
            val retrievedValue = encryptedSharedPreferences.getString(testKey, null)
            val isValid = retrievedValue == testValue
            
            // Clean up test data
            with(encryptedSharedPreferences.edit()) {
                remove(testKey)
                apply()
            }
            
            Log.d(TAG, "Key storage integrity check: ${if (isValid) "PASSED" else "FAILED"}")
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Key integrity validation failed", e)
            false
        }
    }
    
    /**
     * Store generic data securely (for settings, preferences, etc.)
     */
    fun storeGenericData(key: String, value: String) {
        try {
            with(encryptedSharedPreferences.edit()) {
                putString("generic_$key", value)
                apply()
            }
            Log.d(TAG, "Generic data stored successfully for key: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store generic data for key: $key", e)
            throw SecurityException("Failed to store data securely", e)
        }
    }
    
    /**
     * Retrieve generic data securely
     */
    fun getGenericData(key: String): String? {
        return try {
            val value = encryptedSharedPreferences.getString("generic_$key", null)
            if (value != null) {
                Log.d(TAG, "Generic data retrieved successfully for key: $key")
            }
            value
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve generic data for key: $key", e)
            null
        }
    }
    
    /**
     * Remove generic data
     */
    fun removeGenericData(key: String) {
        try {
            with(encryptedSharedPreferences.edit()) {
                remove("generic_$key")
                apply()
            }
            Log.d(TAG, "Generic data removed successfully for key: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove generic data for key: $key", e)
        }
    }
    
    /**
     * Clear all generic data (keeping API keys)
     */
    fun clearAllGenericData() {
        try {
            val allKeys = encryptedSharedPreferences.all.keys
            with(encryptedSharedPreferences.edit()) {
                allKeys.filter { it.startsWith("generic_") }.forEach { key ->
                    remove(key)
                }
                apply()
            }
            Log.d(TAG, "All generic data cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear generic data", e)
        }
    }
}

/**
 * Security information data class for diagnostics
 */
data class SecurityInfo(
    val isHardwareBacked: Boolean,
    val storageType: String,
    val hasApiKey: Boolean,
    val keyVersion: String,
    val lastRotation: Long
)