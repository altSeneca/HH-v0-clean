package com.hazardhawk.ai.services

import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.ai.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.uuid.uuid4
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Google Vertex AI Gemini Vision Pro 2.5 service for cloud-based AI analysis.
 * This serves as a fallback when local Gemma 3N E2B is unavailable.
 */
expect class VertexAIClient() {
    suspend fun configure(apiKey: String): Result<Unit>
    suspend fun analyzePhoto(imageData: ByteArray, workType: WorkType): Result<SafetyAnalysis>
}

class VertexAIGeminiService : AIPhotoAnalyzer {
    
    private var apiKey: String? = null
    private var isConfigured = false
    private val client = VertexAIClient()
    
    override val analyzerName = "Vertex AI Gemini Vision Pro 2.5"
    override val priority = 75 // Lower priority than Gemma (local-first approach)
    
    override val analysisCapabilities = setOf(
        AnalysisCapability.MULTIMODAL_VISION,
        AnalysisCapability.PPE_DETECTION,
        AnalysisCapability.HAZARD_IDENTIFICATION,
        AnalysisCapability.OSHA_COMPLIANCE,
        AnalysisCapability.DOCUMENT_GENERATION
    )
    
    override val isAvailable: Boolean
        get() = isConfigured && apiKey != null
    
    override suspend fun configure(apiKey: String?): Result<Unit> {
        return try {
            if (apiKey.isNullOrBlank()) {
                Result.failure(Exception("Vertex AI requires a valid API key"))
            } else {
                // Validate API key format
                val validationResult = validateVertexAIKey(apiKey)
                if (!validationResult.isValid) {
                    Result.failure(Exception("Invalid API key: ${validationResult.errorMessage}"))
                } else {
                    // Configure the platform-specific client
                    val configResult = client.configure(apiKey)
                    if (configResult.isSuccess) {
                        this.apiKey = apiKey
                        this.isConfigured = true
                        Result.success(Unit)
                    } else {
                        Result.failure(configResult.exceptionOrNull() ?: Exception("Client configuration failed"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Vertex AI configuration failed: ${e.message}", e))
        }
    }
    
    private fun validateVertexAIKey(apiKey: String): ValidationResult {
        return when {
            apiKey.startsWith("AIzaSy") -> {
                // Gemini API key format
                if (apiKey.length in 35..45) {
                    ValidationResult(true)
                } else {
                    ValidationResult(false, "Gemini API key length should be 35-45 characters")
                }
            }
            apiKey.startsWith("AQ.") -> {
                // Vertex AI API key format
                if (apiKey.length >= 20 && apiKey.contains('.')) {
                    ValidationResult(true)
                } else {
                    ValidationResult(false, "Invalid Vertex AI API key format")
                }
            }
            else -> ValidationResult(false, "API key should start with 'AIzaSy' (Gemini) or 'AQ.' (Vertex AI)")
        }
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )
    
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType
    ): Result<SafetyAnalysis> {
        
        if (!isAvailable) {
            return Result.failure(Exception("Vertex AI service not configured"))
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Use real Vertex AI integration through platform-specific client
            return client.analyzePhoto(imageData, workType)
            
        } catch (e: Exception) {
            Result.failure(Exception("Vertex AI analysis failed: ${e.message}", e))
        }
    }
}