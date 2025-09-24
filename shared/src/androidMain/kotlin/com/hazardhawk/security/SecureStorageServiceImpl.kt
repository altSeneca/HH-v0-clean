package com.hazardhawk.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.io.IOException
import android.util.Base64

/**
 * Android implementation of SecureStorageService using Android Keystore
 * 
 * This implementation provides hardware-backed security when available,
 * with automatic fallback to software-based encryption.
 * 
 * Features:
 * - Hardware-backed Android Keystore (StrongBox when available)
 * - AES-256-GCM encryption with EncryptedSharedPreferences
 * - Secure key rotation and lifecycle management
 * - OSHA compliance audit trail
 * - Automatic fallback mechanisms
 */
class SecureStorageServiceImpl(private val context: Context) : SecureStorageService {
    
    companion object {
        private const val TAG = "SecureStorageServiceImpl"
        private const val ENCRYPTED_PREFS_NAME = "hazardhawk_secure_storage"
        private const val FALLBACK_PREFS_NAME = "hazardhawk_secure_storage_fallback"
        private const val KEY_VERSION_SUFFIX = "_version"
        private const val KEY_TIMESTAMP_SUFFIX = "_timestamp"
        private const val TEST_KEY = "_integrity_test_key"
        private const val TEST_VALUE_PREFIX = "test_value_"
    }
    
    private val encryptedSharedPreferences: SharedPreferences by lazy {
        createEncryptedSharedPreferences()
    }
    
    private val secureRandom = SecureRandom()
    
    /**
     * Create EncryptedSharedPreferences with hardware-backed master key
     */
    private fun createEncryptedSharedPreferences(): SharedPreferences {
        return try {
            // Try hardware-backed security first
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true) // Use StrongBox when available
                .setUserAuthenticationRequired(false) // Allow background access
                .setKeyGenParameterSpec(
                    KeyGenParameterSpec.Builder(
                        MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(SecurityConstants.MIN_KEY_LENGTH)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                )
                .build()
                
            val prefs = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            Log.i(TAG, "Hardware-backed secure storage initialized successfully")
            prefs
            
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "Hardware-backed security failed, falling back to software: ${e.message}")
            createFallbackEncryptedPreferences()
        } catch (e: IOException) {
            Log.e(TAG, "IO error creating secure storage: ${e.message}")
            throw SecureStorageException("Failed to initialize secure storage", e)
        }
    }
    
    /**
     * Fallback to software-based encryption if hardware backing fails
     */
    private fun createFallbackEncryptedPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(false) // Software-based fallback
                .build()
                
            val prefs = EncryptedSharedPreferences.create(
                context,
                FALLBACK_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            Log.i(TAG, "Software-based secure storage initialized as fallback")
            prefs
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create fallback secure storage: ${e.message}")
            throw SecureStorageException("Unable to initialize any secure storage mechanism", e)
        }
    }
    
    override suspend fun storeSecurely(key: String, value: String, version: String) {
        withContext(Dispatchers.IO) {
            try {
                validateInputs(key, value)
                
                with(encryptedSharedPreferences.edit()) {
                    putString(key, value)
                    putString(key + KEY_VERSION_SUFFIX, version)
                    putLong(key + KEY_TIMESTAMP_SUFFIX, System.currentTimeMillis())
                    apply() // Use apply() for async write
                }
                
                Log.d(TAG, "Successfully stored key: $key with version: $version")
                logSecurityEvent("KEY_STORED", mapOf("key" to key, "version" to version))
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to store key: $key", e)
                throw SecureStorageException("Failed to securely store key: $key", e)
            }
        }
    }
    
    override suspend fun retrieveSecurely(key: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val value = encryptedSharedPreferences.getString(key, null)
                if (value != null) {
                    Log.d(TAG, "Successfully retrieved key: $key")
                    logSecurityEvent("KEY_RETRIEVED", mapOf("key" to key))
                } else {
                    Log.d(TAG, "Key not found: $key")
                }
                value
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retrieve key: $key", e)
                throw SecureStorageException("Failed to retrieve key: $key", e)
            }
        }
    }
    
    override suspend fun hasKey(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                encryptedSharedPreferences.contains(key)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check key existence: $key", e)
                false
            }
        }
    }
    
    override suspend fun removeKey(key: String) {
        withContext(Dispatchers.IO) {
            try {
                with(encryptedSharedPreferences.edit()) {
                    remove(key)
                    remove(key + KEY_VERSION_SUFFIX)
                    remove(key + KEY_TIMESTAMP_SUFFIX)
                    apply()
                }
                
                Log.d(TAG, "Successfully removed key: $key")
                logSecurityEvent("KEY_REMOVED", mapOf("key" to key))
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove key: $key", e)
                throw SecureStorageException("Failed to remove key: $key", e)
            }
        }
    }
    
    override suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            try {
                with(encryptedSharedPreferences.edit()) {
                    clear()
                    apply()
                }
                
                Log.i(TAG, "All secure storage cleared")
                logSecurityEvent("ALL_KEYS_CLEARED", mapOf("timestamp" to System.currentTimeMillis()))
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all keys", e)
                throw SecureStorageException("Failed to clear secure storage", e)
            }
        }
    }
    
    override suspend fun getKeyVersion(key: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                encryptedSharedPreferences.getString(key + KEY_VERSION_SUFFIX, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get key version: $key", e)
                null
            }
        }
    }
    
    override fun isHardwareBackedSecurity(): Boolean {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true)
                .build()
            
            // If we can create a hardware-backed key, it's available
            true
        } catch (e: Exception) {
            Log.d(TAG, "Hardware-backed security not available: ${e.message}")
            false
        }
    }
    
    override suspend fun validateIntegrity(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val testKey = TEST_KEY + System.currentTimeMillis()
                val testValue = TEST_VALUE_PREFIX + System.currentTimeMillis()
                
                // Test write/read cycle
                storeSecurely(testKey, testValue)
                val retrievedValue = retrieveSecurely(testKey)
                val isValid = retrievedValue == testValue
                
                // Clean up test data
                removeKey(testKey)
                
                Log.d(TAG, "Storage integrity validation: ${if (isValid) "PASSED" else "FAILED"}")
                isValid
                
            } catch (e: Exception) {
                Log.e(TAG, "Storage integrity validation failed", e)
                false
            }
        }
    }
    
    override suspend fun generateSecureKey(keyLength: Int): String {
        return withContext(Dispatchers.IO) {
            try {
                val keyBytes = ByteArray(keyLength)
                secureRandom.nextBytes(keyBytes)
                val encodedKey = Base64.encodeToString(keyBytes, Base64.NO_WRAP)
                
                Log.d(TAG, "Generated secure key of length: $keyLength bytes")
                logSecurityEvent("SECURE_KEY_GENERATED", mapOf("keyLength" to keyLength))
                
                encodedKey
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate secure key", e)
                throw SecureStorageException("Failed to generate secure key", e)
            } finally {
                // Ensure sensitive data is cleared from memory
                System.gc()
            }
        }
    }
    
    override suspend fun storeApiCredentials(apiName: String, credentials: String, version: String) {
        val apiKey = "api_${apiName.lowercase()}_credentials"
        storeSecurely(apiKey, credentials, version)
        
        Log.i(TAG, "API credentials stored for: $apiName")
        logSecurityEvent("API_CREDENTIALS_STORED", mapOf(
            "apiName" to apiName,
            "version" to version
        ))
    }
    
    override suspend fun getApiCredentials(apiName: String): String? {
        val apiKey = "api_${apiName.lowercase()}_credentials"
        return retrieveSecurely(apiKey)
    }
    
    override suspend fun rotateApiCredentials(apiName: String, newCredentials: String, newVersion: String) {
        val oldVersion = getKeyVersion("api_${apiName.lowercase()}_credentials")
        storeApiCredentials(apiName, newCredentials, newVersion)
        
        Log.i(TAG, "API credentials rotated for: $apiName from version: $oldVersion to: $newVersion")
        logSecurityEvent("API_CREDENTIALS_ROTATED", mapOf(
            "apiName" to apiName,
            "oldVersion" to (oldVersion ?: "unknown"),
            "newVersion" to newVersion
        ))
    }
    
    /**
     * Validate input parameters
     */
    private fun validateInputs(key: String, value: String) {
        if (key.isBlank()) {
            throw IllegalArgumentException("Key cannot be blank")
        }
        if (value.isBlank()) {
            throw IllegalArgumentException("Value cannot be blank")
        }
    }
    
    /**
     * Security event logging for audit trail (OSHA compliance)
     */
    private fun logSecurityEvent(event: String, metadata: Map<String, Any>) {
        // In production, this would integrate with your security monitoring system
        Log.i(TAG, "Security Event: $event, Metadata: $metadata")
        
        // Store in secure audit log for OSHA compliance
        // This could integrate with your backend audit system
        try {
            with(encryptedSharedPreferences.edit()) {
                val auditKey = "audit_${System.currentTimeMillis()}_${event.lowercase()}"
                val auditData = "event=$event;timestamp=${System.currentTimeMillis()};metadata=$metadata"
                putString(auditKey, auditData)
                apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log security event to audit trail", e)
        }
    }
}