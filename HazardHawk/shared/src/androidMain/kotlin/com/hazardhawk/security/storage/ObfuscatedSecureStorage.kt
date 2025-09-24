package com.hazardhawk.security.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.hazardhawk.security.storage.SecureStorage
import com.hazardhawk.security.storage.StorageSecurityLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Fallback storage using standard SharedPreferences with basic obfuscation
 * Provides medium security when EncryptedSharedPreferences is not available
 */
class ObfuscatedSecureStorage(private val context: Context) : SecureStorage {
    
    companion object {
        private const val PREFS_NAME = "hazardhawk_obfuscated_prefs"
        private const val HEALTH_CHECK_KEY = "_obfuscated_health_check"
        private const val HEALTH_CHECK_VALUE = "healthy_obfuscated"
        private const val OBFUSCATION_KEY = "HazardHawk2024SecureKey"
    }
    
    override val securityLevel = StorageSecurityLevel.OBFUSCATED_MEDIUM
    
    private var sharedPrefs: SharedPreferences? = null
    private var initializationError: Throwable? = null
    
    override val isAvailable: Boolean
        get() = sharedPrefs != null && initializationError == null
    
    init {
        try {
            sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            initializationError = null
        } catch (e: Exception) {
            initializationError = e
            sharedPrefs = null
            println("ObfuscatedSecureStorage initialization failed: ${e.message}")
        }
    }
    
    override suspend fun getString(key: String): String? = withContext(Dispatchers.IO) {
        try {
            checkAvailability()
            val obfuscatedKey = obfuscateKey(key)
            val obfuscatedValue = sharedPrefs?.getString(obfuscatedKey, null)
            obfuscatedValue?.let { deobfuscateValue(it) }
        } catch (e: Exception) {
            handleStorageError("getString", key, e)
            null
        }
    }
    
    override suspend fun setString(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        try {
            checkAvailability()
            val obfuscatedKey = obfuscateKey(key)
            val obfuscatedValue = obfuscateValue(value)
            sharedPrefs?.edit()?.putString(obfuscatedKey, obfuscatedValue)?.apply()
            true
        } catch (e: Exception) {
            handleStorageError("setString", key, e)
            false
        }
    }
    
    override suspend fun remove(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            checkAvailability()
            val obfuscatedKey = obfuscateKey(key)
            sharedPrefs?.edit()?.remove(obfuscatedKey)?.apply()
            true
        } catch (e: Exception) {
            handleStorageError("remove", key, e)
            false
        }
    }
    
    override suspend fun clear(): Boolean = withContext(Dispatchers.IO) {
        try {
            checkAvailability()
            sharedPrefs?.edit()?.clear()?.apply()
            true
        } catch (e: Exception) {
            handleStorageError("clear", "all", e)
            false
        }
    }
    
    override suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            checkAvailability()
            val obfuscatedKey = obfuscateKey(key)
            sharedPrefs?.contains(obfuscatedKey) ?: false
        } catch (e: Exception) {
            handleStorageError("contains", key, e)
            false
        }
    }
    
    override suspend fun healthCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            checkAvailability()
            
            // Perform read/write test
            val testValue = "test_${Random.nextInt()}"
            val success = setString(HEALTH_CHECK_KEY, testValue) &&
                         getString(HEALTH_CHECK_KEY) == testValue
            
            // Clean up test data
            remove(HEALTH_CHECK_KEY)
            
            success
            
        } catch (e: Exception) {
            handleStorageError("healthCheck", HEALTH_CHECK_KEY, e)
            false
        }
    }
    
    /**
     * Simple obfuscation of keys using Base64 encoding with salt
     * Note: This is obfuscation, not encryption - provides limited security
     */
    private fun obfuscateKey(key: String): String {
        return try {
            val saltedKey = OBFUSCATION_KEY + key + OBFUSCATION_KEY.reversed()
            Base64.encodeToString(saltedKey.toByteArray(), Base64.DEFAULT)
                .replace("\n", "")
                .replace("=", "_")
        } catch (e: Exception) {
            // Fallback to original key if obfuscation fails
            "fallback_$key"
        }
    }
    
    /**
     * Simple obfuscation of values using Base64 encoding with XOR
     * Note: This is obfuscation, not encryption - provides limited security
     */
    private fun obfuscateValue(value: String): String {
        return try {
            // Simple XOR with key bytes
            val keyBytes = OBFUSCATION_KEY.toByteArray()
            val valueBytes = value.toByteArray()
            val obfuscatedBytes = ByteArray(valueBytes.size)
            
            for (i in valueBytes.indices) {
                obfuscatedBytes[i] = (valueBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            
            Base64.encodeToString(obfuscatedBytes, Base64.DEFAULT).replace("\n", "")
            
        } catch (e: Exception) {
            // Fallback to Base64 encoding only
            Base64.encodeToString(value.toByteArray(), Base64.DEFAULT).replace("\n", "")
        }
    }
    
    /**
     * Reverse the obfuscation process for values
     */
    private fun deobfuscateValue(obfuscatedValue: String): String {
        return try {
            val obfuscatedBytes = Base64.decode(obfuscatedValue, Base64.DEFAULT)
            val keyBytes = OBFUSCATION_KEY.toByteArray()
            val deobfuscatedBytes = ByteArray(obfuscatedBytes.size)
            
            for (i in obfuscatedBytes.indices) {
                deobfuscatedBytes[i] = (obfuscatedBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            
            String(deobfuscatedBytes)
            
        } catch (e: Exception) {
            // Fallback to Base64 decoding only
            try {
                String(Base64.decode(obfuscatedValue, Base64.DEFAULT))
            } catch (fallbackError: Exception) {
                // Return the original value as last resort
                obfuscatedValue
            }
        }
    }
    
    private fun checkAvailability() {
        if (!isAvailable) {
            throw IllegalStateException(
                "ObfuscatedSecureStorage not available: ${initializationError?.message ?: "Unknown error"}"
            )
        }
    }
    
    private fun handleStorageError(operation: String, key: String, error: Throwable) {
        println("ObfuscatedSecureStorage.$operation failed for key '$key': ${error.message}")
        
        // Mark as unavailable if we get consistent errors
        if (error is SecurityException || error is IllegalStateException) {
            initializationError = error
        }
    }
}