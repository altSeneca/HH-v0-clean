package com.hazardhawk.security

import android.content.Context
import com.hazardhawk.security.SecureStorageService

/**
 * Android implementation of SecureStorageService using SecureKeyManager
 */
class AndroidSecureStorageService(private val context: Context) : SecureStorageService {
    
    private val secureKeyManager = SecureKeyManager.getInstance(context)
    
    override suspend fun getString(key: String): String? {
        return when (key) {
            "gemini_api_key" -> secureKeyManager.getGeminiApiKey()
            else -> null
        }
    }
    
    override suspend fun setString(key: String, value: String): Boolean {
        return try {
            when (key) {
                "gemini_api_key" -> {
                    secureKeyManager.storeGeminiApiKey(value)
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
}