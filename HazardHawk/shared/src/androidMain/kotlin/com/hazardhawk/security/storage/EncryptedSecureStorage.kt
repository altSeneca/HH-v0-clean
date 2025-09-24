package com.hazardhawk.security.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.hazardhawk.security.storage.SecureStorage
import com.hazardhawk.security.storage.StorageSecurityLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Primary secure storage implementation using Android's EncryptedSharedPreferences
 * Provides the highest level of security for sensitive data like API keys
 */
class EncryptedSecureStorage(private val context: Context) : SecureStorage {
    
    companion object {
        private const val PREFS_NAME = "hazardhawk_secure_prefs"
        private const val HEALTH_CHECK_KEY = "_encrypted_health_check"
        private const val HEALTH_CHECK_VALUE = "healthy_encrypted"
    }
    
    override val securityLevel = StorageSecurityLevel.ENCRYPTED_SECURE
    
    private var encryptedPrefs: android.content.SharedPreferences? = null
    private var initializationError: Throwable? = null
    
    override val isAvailable: Boolean
        get() = encryptedPrefs != null && initializationError == null
    
    init {
        try {
            initializeEncryptedPreferences()
        } catch (e: Exception) {
            initializationError = e
            // Log error but don't crash - fallback storage will be used
            println("EncryptedSecureStorage initialization failed: ${e.message}")
        }
    }
    
    private fun initializeEncryptedPreferences() {
        try {
            // Generate or retrieve master key for encryption
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            
            encryptedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            // Clear any initialization error
            initializationError = null
            
        } catch (e: Exception) {
            initializationError = e
            encryptedPrefs = null
            throw e
        }
    }
    
    override suspend fun getString(key: String): String? = withContext(Dispatchers.IO) {
        try {
            checkAvailability()
            encryptedPrefs?.getString(key, null)
        } catch (e: Exception) {
            handleStorageError("getString", key, e)
            null
        }
    }
    
    override suspend fun setString(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        try {
            checkAvailability()
            encryptedPrefs?.edit()?.putString(key, value)?.apply() ?: false
            true
        } catch (e: Exception) {
            handleStorageError("setString", key, e)
            false
        }
    }
    
    override suspend fun remove(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            checkAvailability()
            encryptedPrefs?.edit()?.remove(key)?.apply() ?: false
            true
        } catch (e: Exception) {
            handleStorageError("remove", key, e)
            false
        }
    }
    
    override suspend fun clear(): Boolean = withContext(Dispatchers.IO) {
        try {
            checkAvailability()
            encryptedPrefs?.edit()?.clear()?.apply() ?: false
            true
        } catch (e: Exception) {
            handleStorageError("clear", "all", e)
            false
        }
    }
    
    override suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            checkAvailability()
            encryptedPrefs?.contains(key) ?: false
        } catch (e: Exception) {
            handleStorageError("contains", key, e)
            false
        }
    }
    
    override suspend fun healthCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            checkAvailability()
            
            // Perform read/write test
            val testResult = encryptedPrefs?.edit()
                ?.putString(HEALTH_CHECK_KEY, HEALTH_CHECK_VALUE)
                ?.apply()
            
            if (testResult != null) {
                val readValue = encryptedPrefs?.getString(HEALTH_CHECK_KEY, null)
                val isHealthy = readValue == HEALTH_CHECK_VALUE
                
                // Clean up test data
                encryptedPrefs?.edit()?.remove(HEALTH_CHECK_KEY)?.apply()
                
                return@withContext isHealthy
            }
            
            false
            
        } catch (e: Exception) {
            handleStorageError("healthCheck", HEALTH_CHECK_KEY, e)
            
            // Try to reinitialize if health check fails
            try {
                initializeEncryptedPreferences()
                return@withContext isAvailable
            } catch (reinitError: Exception) {
                println("EncryptedSecureStorage reinitialization failed: ${reinitError.message}")
                false
            }
        }
    }
    
    private fun checkAvailability() {
        if (!isAvailable) {
            throw IllegalStateException(
                "EncryptedSharedPreferences not available: ${initializationError?.message ?: "Unknown error"}"
            )
        }
    }
    
    private fun handleStorageError(operation: String, key: String, error: Throwable) {
        println("EncryptedSecureStorage.$operation failed for key '$key': ${error.message}")
        
        // If we encounter a security/crypto error, mark as unavailable
        if (error.message?.contains("crypto", ignoreCase = true) == true ||
            error.message?.contains("key", ignoreCase = true) == true ||
            error.message?.contains("decrypt", ignoreCase = true) == true) {
            
            initializationError = error
            encryptedPrefs = null
        }
    }
}