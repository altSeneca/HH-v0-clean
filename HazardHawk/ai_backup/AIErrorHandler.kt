package com.hazardhawk.ai

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import com.hazardhawk.domain.entities.*
import com.hazardhawk.monitoring.AIPerformanceMonitor
import kotlin.math.pow
import kotlin.math.min

/**
 * Enhanced AI Error Handler with comprehensive fallback strategies
 * Supports Gemma multimodal integration, retry mechanisms, and business continuity
 * Provides robust error handling, graceful fallbacks, and user-friendly error messages
 * for both YOLO hazard detection and Gemma multimodal analysis operations.
 */
class AIErrorHandler(
    private val performanceMonitor: AIPerformanceMonitor? = null
) {
    
    /**
     * Enhanced error handling with retry logic and comprehensive fallback strategies
     */
    suspend fun handleAnalysisError(
        error: Throwable,
        context: AIAnalysisContext,
        fallbackProvider: AIFallbackProvider
    ): AIAnalysisResult {
        val errorType = classifyError(error)
        val fallbackStrategy = determineFallbackStrategy(errorType, context)
        
        performanceMonitor?.recordError("AI Error: ${errorType.name} - ${error.message}")
        
        return when (fallbackStrategy) {
            FallbackStrategy.RETRY_WITH_TIMEOUT -> {
                executeWithRetry(context, fallbackProvider, errorType)
            }
            
            FallbackStrategy.USE_ALTERNATIVE_MODEL -> {
                try {
                    fallbackProvider.useAlternativeModel(context)
                } catch (fallbackError: Exception) {
                    performanceMonitor?.recordError("Alternative model failed: ${fallbackError.message}")
                    generateEnhancedFallback(errorType, context)
                }
            }
            
            FallbackStrategy.OFFLINE_MODE -> {
                try {
                    fallbackProvider.generateOfflineAnalysis(context)
                } catch (offlineError: Exception) {
                    performanceMonitor?.recordError("Offline analysis failed: ${offlineError.message}")
                    generateEnhancedFallback(errorType, context)
                }
            }
            
            FallbackStrategy.BASIC_TAGS_ONLY -> {
                generateEnhancedFallback(errorType, context)
            }
        }
    }
    
    /**
     * Execute analysis with exponential backoff retry mechanism
     */
    private suspend fun executeWithRetry(
        context: AIAnalysisContext,
        fallbackProvider: AIFallbackProvider,
        originalErrorType: AIErrorType,
        maxRetries: Int = 3
    ): AIAnalysisResult {
        var lastError: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val backoffDelay = calculateBackoffDelay(attempt)
                if (attempt > 0) {
                    delay(backoffDelay)
                }
                
                val retryContext = context.copy(
                    attemptCount = context.attemptCount + attempt + 1,
                    analysisTimeout = context.analysisTimeout + (1000L * attempt) // Increase timeout
                )
                
                return fallbackProvider.retryWithReducedTimeout(retryContext)
                
            } catch (retryError: Exception) {
                lastError = retryError
                performanceMonitor?.recordError("Retry attempt ${attempt + 1} failed: ${retryError.message}")
                
                // If this is the last attempt, try alternative model
                if (attempt == maxRetries - 1) {
                    try {
                        return fallbackProvider.useAlternativeModel(context)
                    } catch (altError: Exception) {
                        performanceMonitor?.recordError("Alternative model failed: ${altError.message}")
                    }
                }
            }
        }
        
        // All retries failed, return enhanced fallback
        return generateEnhancedFallback(originalErrorType, context, lastError)
    }
    
    private fun calculateBackoffDelay(attempt: Int): Long {
        val baseDelay = 1000L // 1 second
        val maxDelay = 8000L // 8 seconds max
        val exponentialDelay = (baseDelay * 2.0.pow(attempt.toDouble())).toLong()
        return min(exponentialDelay, maxDelay)
    }
    
    /**
     * Classify error type for appropriate handling.
     */
    private fun classifyError(error: Throwable): AIErrorType {
        return when (error) {
            is TimeoutCancellationException -> AIErrorType.TIMEOUT
            is OutOfMemoryError -> AIErrorType.INSUFFICIENT_MEMORY
            is SecurityException -> AIErrorType.PERMISSION_DENIED
            is IllegalArgumentException -> AIErrorType.INVALID_INPUT
            is IllegalStateException -> AIErrorType.MODEL_NOT_LOADED
            else -> when {
                error.message?.contains("network", ignoreCase = true) == true -> AIErrorType.NETWORK_ERROR
                error.message?.contains("model", ignoreCase = true) == true -> AIErrorType.MODEL_ERROR
                error.message?.contains("memory", ignoreCase = true) == true -> AIErrorType.INSUFFICIENT_MEMORY
                else -> AIErrorType.UNKNOWN
            }
        }
    }
    
    /**
     * Determine the best fallback strategy based on error type and context.
     */
    private fun determineFallbackStrategy(
        errorType: AIErrorType,
        context: AIAnalysisContext
    ): FallbackStrategy {
        return when (errorType) {
            AIErrorType.TIMEOUT -> {
                if (context.attemptCount < 2) {
                    FallbackStrategy.RETRY_WITH_TIMEOUT
                } else {
                    FallbackStrategy.USE_ALTERNATIVE_MODEL
                }
            }
            
            AIErrorType.INSUFFICIENT_MEMORY -> {
                FallbackStrategy.USE_ALTERNATIVE_MODEL
            }
            
            AIErrorType.MODEL_ERROR, AIErrorType.MODEL_NOT_LOADED -> {
                FallbackStrategy.USE_ALTERNATIVE_MODEL
            }
            
            AIErrorType.NETWORK_ERROR -> {
                FallbackStrategy.OFFLINE_MODE
            }
            
            AIErrorType.PERMISSION_DENIED -> {
                FallbackStrategy.BASIC_TAGS_ONLY
            }
            
            AIErrorType.INVALID_INPUT -> {
                FallbackStrategy.BASIC_TAGS_ONLY
            }
            
            AIErrorType.UNKNOWN -> {
                if (context.hasNetworkAccess) {
                    FallbackStrategy.USE_ALTERNATIVE_MODEL
                } else {
                    FallbackStrategy.OFFLINE_MODE
                }
            }
        }
    }
    
    /**
     * Generate enhanced fallback analysis with comprehensive safety recommendations
     */
    private fun generateEnhancedFallback(
        errorType: AIErrorType,
        context: AIAnalysisContext,
        lastError: Exception? = null
    ): AIAnalysisResult {
        val workTypeBasedTags = generateWorkTypeBasedRecommendations(context.workType)
        val errorSpecificMessage = getErrorMessage(errorType)
        val enhancedRecommendations = generateEnhancedRecommendations(context.workType, errorType)
        
        // Create comprehensive fallback result
        val safetyAnalysisResult = SafetyAnalysisResult(
            id = "fallback-${System.currentTimeMillis()}",
            photoId = "photo-fallback-${System.currentTimeMillis()}",
            detailedAssessment = buildFallbackAssessment(context.workType, errorType, errorSpecificMessage),
            oshaViolations = generateFallbackOSHAViolations(context.workType),
            recommendations = enhancedRecommendations,
            overallConfidence = determineFallbackConfidence(errorType, context),
            processingTimeMs = 100L, // Quick fallback processing
            analysisSource = AnalysisSource.MANUAL,
            workType = context.workType,
            timestamp = System.currentTimeMillis()
        )
        
        return AIAnalysisResult(
            success = false,
            analysisType = AnalysisType.FALLBACK,
            recommendations = workTypeBasedTags,
            errorMessage = errorSpecificMessage,
            confidence = safetyAnalysisResult.overallConfidence,
            processingTimeMs = safetyAnalysisResult.processingTimeMs,
            fallbackUsed = true,
            fallbackReason = "AI analysis failed: $errorSpecificMessage. ${lastError?.message ?: ""}",
            modelUsed = "Enhanced-Fallback-v1.0",
            enhancedResult = safetyAnalysisResult
        )
    }
    
    private fun buildFallbackAssessment(workType: WorkType, errorType: AIErrorType, errorMessage: String): String {
        return buildString {
            appendLine("FALLBACK SAFETY ASSESSMENT")
            appendLine("==========================")
            appendLine("Work Type: ${workType.name.replace('_', ' ')}")
            appendLine("Analysis Status: AI systems unavailable ($errorMessage)")
            appendLine("")
            appendLine("MANUAL SAFETY REVIEW REQUIRED:")
            appendLine("This assessment is based on general safety guidelines for ${workType.name.replace('_', ' ').lowercase()} work.")
            appendLine("A qualified safety professional should conduct a thorough on-site inspection.")
            appendLine("")
            appendLine("IMMEDIATE ACTIONS:")
            appendLine("1. Contact site safety supervisor")
            appendLine("2. Verify all workers have proper PPE")
            appendLine("3. Conduct pre-work safety briefing")
            appendLine("4. Document safety conditions with additional photos")
            appendLine("5. Do not proceed with high-risk activities until manual inspection complete")
        }
    }
    
    private fun generateFallbackOSHAViolations(workType: WorkType): List<OSHAViolation> {
        return when (workType) {
            WorkType.ELECTRICAL_WORK -> listOf(
                OSHAViolation(
                    regulation = "1926.416(a)(1)",
                    title = "Electrical Safety Requirements",
                    description = "Manual verification required - AI analysis unavailable",
                    severity = ViolationSeverity.WARNING,
                    recommendedAction = "Conduct manual electrical safety inspection with qualified electrician"
                )
            )
            WorkType.ROOFING -> listOf(
                OSHAViolation(
                    regulation = "1926.501(b)(10)",
                    title = "Fall Protection Systems",
                    description = "Manual verification required - AI analysis unavailable",
                    severity = ViolationSeverity.WARNING,
                    recommendedAction = "Verify fall protection systems with competent person"
                )
            )
            WorkType.EXCAVATION -> listOf(
                OSHAViolation(
                    regulation = "1926.651(c)(1)",
                    title = "Excavation Safety Requirements",
                    description = "Manual verification required - AI analysis unavailable",
                    severity = ViolationSeverity.WARNING,
                    recommendedAction = "Conduct excavation safety inspection with competent person"
                )
            )
            else -> listOf(
                OSHAViolation(
                    regulation = "1926.95(a)",
                    title = "General Safety Requirements",
                    description = "Manual safety assessment required due to AI system unavailability",
                    severity = ViolationSeverity.NOTICE,
                    recommendedAction = "Conduct comprehensive safety assessment"
                )
            )
        }
    }
    
    private fun generateEnhancedRecommendations(workType: WorkType, errorType: AIErrorType): List<SafetyRecommendation> {
        val baseRecommendations = mutableListOf<SafetyRecommendation>()
        
        // Add error-specific recommendation
        baseRecommendations.add(
            SafetyRecommendation(
                id = "fallback-error-${errorType.name.lowercase()}",
                priority = RecommendationPriority.IMMEDIATE,
                category = RecommendationCategory.PROCEDURE,
                description = "Address AI system failure before proceeding",
                actionSteps = getErrorSpecificActions(errorType),
                estimatedTimeToImplement = "10-15 minutes",
                riskReduction = RiskReductionLevel.HIGH
            )
        )
        
        // Add work-type specific recommendations
        baseRecommendations.addAll(getWorkTypeRecommendations(workType))
        
        return baseRecommendations
    }
    
    private fun getErrorSpecificActions(errorType: AIErrorType): List<String> {
        return when (errorType) {
            AIErrorType.TIMEOUT -> listOf(
                "Check device performance and close unnecessary apps",
                "Try analysis again with simplified settings",
                "Contact IT support if problem persists"
            )
            AIErrorType.INSUFFICIENT_MEMORY -> listOf(
                "Close other applications to free memory",
                "Restart the app if necessary",
                "Consider using a device with more RAM for AI analysis"
            )
            AIErrorType.MODEL_ERROR, AIErrorType.MODEL_NOT_LOADED -> listOf(
                "Restart the application",
                "Check for app updates",
                "Contact technical support if issue persists",
                "Use manual safety assessment procedures"
            )
            AIErrorType.NETWORK_ERROR -> listOf(
                "Check internet connection",
                "Try again when connection is stable",
                "Use offline safety protocols"
            )
            else -> listOf(
                "Document the error for technical support",
                "Use manual safety assessment procedures",
                "Contact supervisor for guidance"
            )
        }
    }
    
    private fun getWorkTypeRecommendations(workType: WorkType): List<SafetyRecommendation> {
        return when (workType) {
            WorkType.ELECTRICAL_WORK -> listOf(
                SafetyRecommendation(
                    id = "electrical-manual-check",
                    priority = RecommendationPriority.CRITICAL,
                    category = RecommendationCategory.TRAINING,
                    description = "Perform manual electrical safety verification",
                    actionSteps = listOf(
                        "Verify lockout/tagout procedures",
                        "Check electrical PPE requirements",
                        "Test electrical safety equipment",
                        "Confirm qualified electrician is present"
                    ),
                    estimatedTimeToImplement = "20-30 minutes",
                    relatedOSHACode = "1926.416",
                    riskReduction = RiskReductionLevel.CRITICAL
                )
            )
            WorkType.ROOFING -> listOf(
                SafetyRecommendation(
                    id = "roofing-manual-check",
                    priority = RecommendationPriority.CRITICAL,
                    category = RecommendationCategory.EQUIPMENT,
                    description = "Perform manual fall protection verification",
                    actionSteps = listOf(
                        "Inspect all fall protection equipment",
                        "Verify anchor points and safety lines",
                        "Check weather conditions",
                        "Ensure competent person is supervising"
                    ),
                    estimatedTimeToImplement = "15-25 minutes",
                    relatedOSHACode = "1926.501",
                    riskReduction = RiskReductionLevel.CRITICAL
                )
            )
            else -> listOf(
                SafetyRecommendation(
                    id = "general-manual-check",
                    priority = RecommendationPriority.HIGH,
                    category = RecommendationCategory.PROCEDURE,
                    description = "Perform comprehensive manual safety assessment",
                    actionSteps = listOf(
                        "Conduct visual inspection of work area",
                        "Verify PPE compliance",
                        "Check equipment safety status",
                        "Review work procedures with team"
                    ),
                    estimatedTimeToImplement = "15-20 minutes",
                    riskReduction = RiskReductionLevel.MODERATE
                )
            )
        }
    }
    
    private fun determineFallbackConfidence(errorType: AIErrorType, context: AIAnalysisContext): Float {
        return when (errorType) {
            AIErrorType.TIMEOUT -> 0.4f // Moderate confidence - might work with retry
            AIErrorType.INSUFFICIENT_MEMORY -> 0.3f // Lower confidence - device limitations
            AIErrorType.MODEL_ERROR -> 0.2f // Low confidence - fundamental issue
            AIErrorType.NETWORK_ERROR -> 0.5f // Higher confidence - temporary issue
            AIErrorType.PERMISSION_DENIED -> 0.1f // Very low confidence - access issue
            else -> 0.25f // Default low confidence
        }
    }
    
    /**
     * Generate work-type specific safety recommendations as fallback.
     */
    private fun generateWorkTypeBasedRecommendations(workType: WorkType): List<UITagRecommendation> {
        return when (workType) {
            WorkType.GENERAL_CONSTRUCTION -> listOf(
                UITagRecommendation(
                    tagId = "general-safety-inspection",
                    displayName = "General Safety Inspection",
                    confidence = 0.5f,
                    reason = "Standard safety review for general construction work",
                    priority = TagPriority.MEDIUM
                ),
                UITagRecommendation(
                    tagId = "ppe-compliance-check",
                    displayName = "PPE Compliance Check",
                    confidence = 0.6f,
                    reason = "Verify all workers have required personal protective equipment",
                    priority = TagPriority.HIGH,
                    oshaReference = "1926.95"
                )
            )
            
            WorkType.ELECTRICAL_WORK -> listOf(
                UITagRecommendation(
                    tagId = "electrical-safety-inspection",
                    displayName = "Electrical Safety Inspection",
                    confidence = 0.7f,
                    reason = "Critical safety review for electrical work operations",
                    priority = TagPriority.CRITICAL,
                    oshaReference = "1926.416"
                ),
                UITagRecommendation(
                    tagId = "lockout-tagout-verification",
                    displayName = "Lockout/Tagout Verification",
                    confidence = 0.8f,
                    reason = "Ensure proper energy isolation procedures are followed",
                    priority = TagPriority.CRITICAL,
                    oshaReference = "1926.417"
                )
            )
            
            WorkType.ROOFING -> listOf(
                UITagRecommendation(
                    tagId = "fall-protection-inspection",
                    displayName = "Fall Protection Inspection",
                    confidence = 0.9f,
                    reason = "Critical fall protection review for roofing operations",
                    priority = TagPriority.CRITICAL,
                    oshaReference = "1926.501"
                ),
                UITagRecommendation(
                    tagId = "weather-conditions-assessment",
                    displayName = "Weather Conditions Assessment",
                    confidence = 0.6f,
                    reason = "Evaluate weather suitability for roofing work",
                    priority = TagPriority.HIGH
                )
            )
            
            WorkType.EXCAVATION -> listOf(
                UITagRecommendation(
                    tagId = "excavation-safety-inspection",
                    displayName = "Excavation Safety Inspection",
                    confidence = 0.8f,
                    reason = "Comprehensive excavation safety review",
                    priority = TagPriority.CRITICAL,
                    oshaReference = "1926.651"
                ),
                UITagRecommendation(
                    tagId = "competent-person-verification",
                    displayName = "Competent Person Verification",
                    confidence = 0.7f,
                    reason = "Ensure competent person is present for excavation work",
                    priority = TagPriority.HIGH,
                    oshaReference = "1926.651"
                )
            )
            
            else -> listOf(
                UITagRecommendation(
                    tagId = "manual-safety-review",
                    displayName = "Manual Safety Review Required",
                    confidence = 0.4f,
                    reason = "AI analysis unavailable - manual safety assessment needed",
                    priority = TagPriority.MEDIUM
                )
            )
        }
    }
    
    /**
     * Get user-friendly error messages for different error types.
     */
    private fun getErrorMessage(errorType: AIErrorType): String {
        return when (errorType) {
            AIErrorType.TIMEOUT -> "Analysis took too long and was cancelled. Using basic safety recommendations."
            AIErrorType.INSUFFICIENT_MEMORY -> "Device memory is low. Using simplified analysis."
            AIErrorType.MODEL_ERROR -> "AI model encountered an error. Using standard safety guidelines."
            AIErrorType.MODEL_NOT_LOADED -> "AI model is not ready. Using manual safety assessment."
            AIErrorType.NETWORK_ERROR -> "Network connection issue. Using offline safety guidelines."
            AIErrorType.PERMISSION_DENIED -> "Required permissions not granted. Using basic recommendations."
            AIErrorType.INVALID_INPUT -> "Image could not be processed. Using general safety guidelines."
            AIErrorType.UNKNOWN -> "An unexpected error occurred. Using standard safety procedures."
        }
    }
}

/**
 * Context information for AI analysis operations.
 */
@Serializable
data class AIAnalysisContext(
    val workType: WorkType,
    val imageWidth: Int,
    val imageHeight: Int,
    val imageSizeBytes: Int,
    val attemptCount: Int = 0,
    val hasNetworkAccess: Boolean = true,
    val deviceMemoryMB: Int = 0,
    val analysisTimeout: Long = 5000L,
    val preferredModel: AIModelType = AIModelType.GEMMA_MULTIMODAL
)

/**
 * Enhanced AI analysis result with comprehensive error handling information
 */
@Serializable
data class AIAnalysisResult(
    val success: Boolean,
    val analysisType: AnalysisType,
    val recommendations: List<UITagRecommendation>,
    val errorMessage: String? = null,
    val confidence: Float,
    val processingTimeMs: Long,
    val fallbackUsed: Boolean = false,
    val fallbackReason: String? = null,
    val modelUsed: String? = null,
    val retryCount: Int = 0,
    val enhancedResult: SafetyAnalysisResult? = null,
    val businessImpact: FallbackBusinessImpact? = null
)

@Serializable
data class FallbackBusinessImpact(
    val estimatedDelayMinutes: Int,
    val requiresManualIntervention: Boolean,
    val riskLevel: String,
    val complianceImpact: String
)

/**
 * Types of AI errors that can occur.
 */
enum class AIErrorType {
    TIMEOUT,
    INSUFFICIENT_MEMORY,
    MODEL_ERROR,
    MODEL_NOT_LOADED,
    NETWORK_ERROR,
    PERMISSION_DENIED,
    INVALID_INPUT,
    UNKNOWN
}

/**
 * Available fallback strategies with priority ordering
 */
enum class FallbackStrategy(val priority: Int, val description: String) {
    RETRY_WITH_TIMEOUT(1, "Retry analysis with adjusted parameters"),
    USE_ALTERNATIVE_MODEL(2, "Switch to backup AI model"),
    OFFLINE_MODE(3, "Use cached/offline analysis"),
    BASIC_TAGS_ONLY(4, "Generate basic safety recommendations")
}

/**
 * Types of analysis performed.
 */
enum class AnalysisType {
    GEMMA_MULTIMODAL,
    YOLO_DETECTION,
    HYBRID,
    FALLBACK,
    OFFLINE
}

/**
 * Available AI model types.
 */
enum class AIModelType {
    GEMMA_MULTIMODAL,
    YOLO_HAZARD_DETECTION,
    BASIC_RULES_ENGINE
}

/**
 * Interface for providing fallback analysis capabilities.
 */
interface AIFallbackProvider {
    suspend fun retryWithReducedTimeout(context: AIAnalysisContext): AIAnalysisResult
    suspend fun useAlternativeModel(context: AIAnalysisContext): AIAnalysisResult
    suspend fun generateOfflineAnalysis(context: AIAnalysisContext): AIAnalysisResult
}
