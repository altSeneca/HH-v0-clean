package com.hazardhawk.ai

import com.hazardhawk.BuildConfig

/**
 * AI Configuration helper for initializing AI services with API keys.
 * Uses BuildConfig to securely access API keys from local.properties.
 */
object AIConfig {

    /**
     * Get the Gemini API key from BuildConfig.
     * The key is loaded from local.properties during build.
     */
    fun getGeminiApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    /**
     * Check if Gemini API key is configured.
     */
    fun isGeminiConfigured(): Boolean {
        return getGeminiApiKey().isNotBlank() &&
               !getGeminiApiKey().contains("YOUR_API_KEY_HERE")
    }

    /**
     * Get user-friendly error message for missing API key.
     */
    fun getApiKeyErrorMessage(): String {
        return """
            Gemini API key not configured.

            To fix this:
            1. Open local.properties in project root
            2. Add your key: GEMINI_API_KEY=your_actual_key_here
            3. Get a key from: https://aistudio.google.com/app/apikey
            4. Rebuild the app
        """.trimIndent()
    }
}
