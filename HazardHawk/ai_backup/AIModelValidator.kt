package com.hazardhawk.ai

import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * Validates AI model files and ensures they're ready for production use
 */
expect class AIModelValidator {
    /**
     * Validates that the AI model file exists and is accessible
     */
    suspend fun validateModelExists(modelPath: String): ValidationResult
    
    /**
     * Performs a test inference to ensure the model is functional
     */
    suspend fun validateModelInference(modelPath: String): ValidationResult
    
    /**
     * Checks model file integrity and format
     */
    suspend fun validateModelIntegrity(modelPath: String): ValidationResult
    
    /**
     * Comprehensive model validation including all checks
     */
    suspend fun validateModel(modelPath: String): ValidationResult
}

/**
 * Result of model validation with detailed information
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val details: Map<String, Any> = emptyMap()
) {
    companion object {
        fun success(details: Map<String, Any> = emptyMap()) = ValidationResult(
            isValid = true,
            details = details
        )
        
        fun failure(error: String, details: Map<String, Any> = emptyMap()) = ValidationResult(
            isValid = false,
            errorMessage = error,
            details = details
        )
    }
}

/**
 * Common model validation logic that works across platforms
 */
class CommonModelValidator {
    companion object {
        /**
         * Validates model with timeout protection
         */
        suspend fun validateWithTimeout(
            modelPath: String,
            validator: AIModelValidator,
            timeoutSeconds: Int = 10
        ): ValidationResult {
            return try {
                withTimeout(timeoutSeconds.seconds) {
                    validator.validateModel(modelPath)
                }
            } catch (e: Exception) {
                ValidationResult.failure(
                    "Model validation timed out after ${timeoutSeconds}s: ${e.message}",
                    mapOf("timeout" to timeoutSeconds, "exception" to e.javaClass.simpleName)
                )
            }
        }
        
        /**
         * Gets model file size information
         */
        fun getModelSizeInfo(sizeBytes: Long): Map<String, Any> {
            val sizeMB = sizeBytes / (1024.0 * 1024.0)
            return mapOf(
                "sizeBytes" to sizeBytes,
                "sizeMB" to String.format("%.2f", sizeMB),
                "category" to when {
                    sizeMB < 5.0 -> "Small"
                    sizeMB < 20.0 -> "Medium" 
                    sizeMB < 50.0 -> "Large"
                    else -> "Very Large"
                }
            )
        }
    }
}