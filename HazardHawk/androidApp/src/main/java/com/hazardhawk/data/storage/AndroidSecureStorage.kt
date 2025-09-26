package com.hazardhawk.data.storage

import android.content.Context
import com.hazardhawk.data.repositories.SecureStorage
import com.hazardhawk.security.SecureKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of SecureStorage using SecureKeyManager
 * Provides encrypted storage for camera settings and other sensitive data
 */
class AndroidSecureStorage(
    private val context: Context,
    private val secureKeyManager: SecureKeyManager = SecureKeyManager.getInstance(context)
) : SecureStorage {
    
    override suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        try {
            // Use the existing SecureKeyManager but extend it for general settings
            // For now, we'll store in a prefixed way to avoid conflicts
            secureKeyManager.storeGenericData(key, value)
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails
            val prefs = context.getSharedPreferences("hazardhawk_settings_fallback", Context.MODE_PRIVATE)
            prefs.edit().putString(key, value).apply()
        }
    }
    
    override suspend fun getString(key: String): String? = withContext(Dispatchers.IO) {
        try {
            secureKeyManager.getGenericData(key)
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences
            val prefs = context.getSharedPreferences("hazardhawk_settings_fallback", Context.MODE_PRIVATE)
            prefs.getString(key, null)
        }
    }
    
    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        try {
            secureKeyManager.removeGenericData(key)
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences
            val prefs = context.getSharedPreferences("hazardhawk_settings_fallback", Context.MODE_PRIVATE)
            prefs.edit().remove(key).apply()
        }
    }
    
    override suspend fun clear() = withContext(Dispatchers.IO) {
        try {
            secureKeyManager.clearAllGenericData()
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences
            val prefs = context.getSharedPreferences("hazardhawk_settings_fallback", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        }
    }
}