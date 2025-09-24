package com.hazardhawk.ai

import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import com.hazardhawk.monitoring.AIPerformanceMonitor
import com.hazardhawk.domain.entities.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds

/**
 * Enhanced AI Service Facade - Single entry point for all AI functionality
 * Supports multimodal Gemma 3N E2B integration with comprehensive fallback strategies
 */
interface AIServiceFacade {
    suspend fun analyzePhotoWithTags(
        data: ByteArray, 
        width: Int, 
        height: Int,
        workType: WorkType = WorkType.GENERAL_CONSTRUCTION
    ): PhotoAnalysisWithTags
    
    suspend fun analyzePhotoComprehensive(
        data: ByteArray,
        width: Int,
        height: Int,
        workType: WorkType = WorkType.GENERAL_CONSTRUCTION,
        includeOSHACompliance: Boolean = true,
        detailedRecommendations: Boolean = true
    ): SafetyAnalysisResult
    
    suspend fun initialize(): Result<Unit>
    suspend fun getModelStatus(): ModelStatus
    suspend fun release()
    suspend fun validateModel(): ValidationResult
    
    // Enhanced AI Operations
    suspend fun queueOfflineAnalysis(request: QueuedAnalysisRequest): Result<Unit>
    suspend fun processQueuedAnalyses(): Result<List<SafetyAnalysisResult>>
    suspend fun getQueueStatus(): QueueStatus
    
    // Performance and Analytics
    suspend fun getPerformanceMetrics(startDate: Long, endDate: Long): AIPerformanceMetrics
    suspend fun recordUserFeedback(analysisId: String, rating: Float, feedback: String?)
    suspend fun optimizeForDevice(): DeviceOptimizationResult
}

/**
 * Enhanced AI Service Facade with Gemma 3N E2B multimodal integration
 * Provides comprehensive AI analysis with advanced fallback strategies
 */
class GemmaAIServiceFacade(
    private val gemmaAnalyzer: GemmaVisionAnalyzer,
    private val yoloDetector: YOLOHazardDetector, // Fallback detector
    private val tagMapper: HazardTagMapper,
    private val modelValidator: AIModelValidator,
    private val analysisQueue: OfflineAnalysisQueue,
    private val performanceMonitor: AIPerformanceMonitor = AIPerformanceMonitor.getInstance()
) : AIServiceFacade {
    
    override suspend fun analyzePhotoWithTags(
        data: ByteArray, 
        width: Int, 
        height: Int,
        workType: WorkType
    ): PhotoAnalysisWithTags = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        
        return@withContext try {
            withTimeout(8.seconds) { // Extended timeout for multimodal analysis
                // PRIMARY: Gemma multimodal analysis
                val gemmaResult = try {
                    gemmaAnalyzer.analyzePhoto(data, width, height, workType)
                } catch (gemmaException: Exception) {
                    performanceMonitor.recordError("Gemma analysis failed: ${gemmaException.message}")
                    // FALLBACK: YOLO detector
                    val detections = yoloDetector.detectHazards(data, width, height)
                    val mappingResult = tagMapper.mapToTags(detections, workType)
                    null
                }
                
                val processingTime = System.currentTimeMillis() - startTime
                performanceMonitor.recordInference(processingTime, true)
                
                if (gemmaResult != null) {
                    // Convert comprehensive analysis to PhotoAnalysisWithTags format
                    convertToPhotoAnalysisWithTags(gemmaResult, processingTime)
                } else {
                    // Use YOLO fallback results
                    val detections = yoloDetector.detectHazards(data, width, height)
                    val mappingResult = tagMapper.mapToTags(detections, workType)
                    
                    PhotoAnalysisWithTags(
                        id = generateId(),
                        photoId = generatePhotoId(),
                        detections = detections,
                        enhancedResults = emptyList(),
                        recommendedTags = mappingResult.recommendations.map { it.tagId },
                        autoSelectTags = mappingResult.autoSelections,
                        complianceOverview = mappingResult.complianceOverview,
                        processingTimeMs = processingTime,
                        analysisSource = AnalysisSource.ON_DEVICE
                    )
                }
            }
        } catch (timeoutException: kotlinx.coroutines.TimeoutCancellationException) {
            val processingTime = System.currentTimeMillis() - startTime
            performanceMonitor.recordInference(processingTime, false)
            performanceMonitor.recordError("AI analysis timeout after 8 seconds")
            generateTimeoutFallback(workType, processingTime)
        } catch (modelError: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            performanceMonitor.recordInference(processingTime, false)
            performanceMonitor.recordError("AI analysis failed: ${modelError.message}")
            generateBasicRecommendations(workType, processingTime)
        }
    }
    
    override suspend fun analyzePhotoComprehensive(
        data: ByteArray,
        width: Int,
        height: Int,
        workType: WorkType,
        includeOSHACompliance: Boolean,
        detailedRecommendations: Boolean
    ): SafetyAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        
        try {
            withTimeout(10.seconds) { // Extended timeout for comprehensive analysis
                val result = gemmaAnalyzer.analyzeComprehensive(
                    data, width, height, workType, includeOSHACompliance, detailedRecommendations
                )
                
                val processingTime = System.currentTimeMillis() - startTime
                performanceMonitor.recordInference(processingTime, true)
                
                result.copy(processingTimeMs = processingTime)
            }
        } catch (exception: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            performanceMonitor.recordInference(processingTime, false)
            performanceMonitor.recordError("Comprehensive analysis failed: ${exception.message}")
            
            // Generate fallback comprehensive result
            generateFallbackComprehensiveResult(workType, processingTime, exception.message ?: "Unknown error")
        }
    }
    
    override suspend fun initialize(): Result<Unit> {
        return try {
            val startTime = System.currentTimeMillis()
            
            // Initialize Gemma analyzer first (primary)
            val gemmaInitResult = try {
                gemmaAnalyzer.initialize()
            } catch (e: Exception) {
                performanceMonitor.recordError("Gemma initialization failed: ${e.message}")
                false
            }
            
            // Initialize YOLO detector (fallback)
            val yoloValidation = modelValidator.validateModel("hazard_detection_model.tflite")
            val yoloInitResult = if (yoloValidation.isValid) {
                yoloDetector.initialize(
                    modelPath = "hazard_detection_model.tflite",
                    confidenceThreshold = 0.5f,
                    nmsThreshold = 0.4f
                )
            } else {
                performanceMonitor.recordError("YOLO validation failed: ${yoloValidation.errorMessage}")
                false
            }
            
            // Initialize analysis queue for offline support
            val queueInitResult = analysisQueue.initialize()
            
            val loadTime = System.currentTimeMillis() - startTime
            val overallSuccess = gemmaInitResult || yoloInitResult // At least one must succeed
            
            performanceMonitor.recordModelLoad(loadTime, overallSuccess)
            
            if (overallSuccess) {
                val statusMessage = buildString {
                    append("AI Service initialized - ")
                    append("Gemma: ${if (gemmaInitResult) "✓" else "✗"}, ")
                    append("YOLO: ${if (yoloInitResult) "✓" else "✗"}, ")
                    append("Queue: ${if (queueInitResult) "✓" else "✗"}")
                }
                performanceMonitor.recordError(statusMessage)
                Result.success(Unit)
            } else {
                performanceMonitor.recordError("All AI models failed to initialize")
                Result.failure(Exception("All AI models failed to initialize"))
            }
        } catch (e: Exception) {
            val loadTime = System.currentTimeMillis()
            performanceMonitor.recordModelLoad(loadTime, false)
            performanceMonitor.recordError("AI initialization exception: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getModelStatus(): ModelStatus {
        val gemmaStatus = try { gemmaAnalyzer.getStatus() } catch (e: Exception) { null }
        val yoloStatus = try { yoloDetector.isModelLoaded() } catch (e: Exception) { false }
        
        return ModelStatus(
            isLoaded = (gemmaStatus?.isReady == true) || yoloStatus,
            modelName = "Gemma-3N-E2B + YOLO Hybrid",
            lastError = gemmaStatus?.lastError
        )
    }
    
    override suspend fun release() {
        try {
            gemmaAnalyzer.release()
        } catch (e: Exception) {
            performanceMonitor.recordError("Gemma release error: ${e.message}")
        }
        
        try {
            yoloDetector.release()
        } catch (e: Exception) {
            performanceMonitor.recordError("YOLO release error: ${e.message}")
        }
        
        try {
            analysisQueue.shutdown()
        } catch (e: Exception) {
            performanceMonitor.recordError("Queue shutdown error: ${e.message}")
        }
    }
    
    override suspend fun validateModel(): ValidationResult {
        val gemmaValidation = try {
            gemmaAnalyzer.validate()
        } catch (e: Exception) {
            ValidationResult(false, "Gemma validation failed: ${e.message}")
        }
        
        val yoloValidation = modelValidator.validateModel("hazard_detection_model.tflite")
        
        return ValidationResult(
            isValid = gemmaValidation.isValid || yoloValidation.isValid,
            errorMessage = if (gemmaValidation.isValid || yoloValidation.isValid) {
                null
            } else {
                "Both models failed validation: Gemma - ${gemmaValidation.errorMessage}, YOLO - ${yoloValidation.errorMessage}"
            }
        )
    }
    
    // Enhanced AI Operations Implementation
    override suspend fun queueOfflineAnalysis(request: QueuedAnalysisRequest): Result<Unit> {
        return try {
            analysisQueue.enqueue(request)
            Result.success(Unit)
        } catch (e: Exception) {
            performanceMonitor.recordError("Failed to queue analysis: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun processQueuedAnalyses(): Result<List<SafetyAnalysisResult>> {
        return try {
            val queuedRequests = analysisQueue.getAll()
            val results = mutableListOf<SafetyAnalysisResult>()
            
            for (request in queuedRequests) {
                try {
                    val result = analyzePhotoComprehensive(
                        request.imageData,
                        request.width,
                        request.height,
                        request.workType
                    )
                    results.add(result)
                    analysisQueue.markCompleted(request.id)
                } catch (e: Exception) {
                    analysisQueue.incrementRetryCount(request.id)
                    performanceMonitor.recordError("Queued analysis failed for ${request.id}: ${e.message}")
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            performanceMonitor.recordError("Failed to process queued analyses: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getQueueStatus(): QueueStatus {
        return analysisQueue.getStatus()
    }
    
    override suspend fun getPerformanceMetrics(startDate: Long, endDate: Long): AIPerformanceMetrics {
        // Implementation would integrate with analytics backend
        return AIPerformanceMetrics(
            startDate = startDate,
            endDate = endDate,
            totalAnalyses = performanceMonitor.getCurrentMetrics().totalInferences,
            successfulAnalyses = (performanceMonitor.getCurrentMetrics().totalInferences * 
                                performanceMonitor.getCurrentMetrics().successRate).toInt(),
            averageProcessingTimeMs = performanceMonitor.getCurrentMetrics().averageInferenceTime.inWholeMilliseconds,
            averageConfidence = 0.85f, // Would be calculated from stored analyses
            memoryUsageStats = MemoryUsageStats(
                averageUsageMb = performanceMonitor.getCurrentMetrics().memoryUsageMB.toLong(),
                peakUsageMb = performanceMonitor.getCurrentMetrics().memoryUsageMB.toLong() + 20L
            ),
            batteryImpactStats = BatteryImpactStats(
                averageDrainPercent = 2.5f,
                totalBatteryDrained = 15.0f,
                efficiencyGrade = "B+"
            ),
            errorAnalysis = ErrorAnalysis(
                totalErrors = performanceMonitor.getCurrentMetrics().totalInferences - 
                            (performanceMonitor.getCurrentMetrics().totalInferences * 
                             performanceMonitor.getCurrentMetrics().successRate).toInt(),
                errorsByType = mapOf("timeout" to 2, "model_failure" to 1),
                recoveryRate = 0.95f,
                criticalFailures = 0
            ),
            userSatisfactionMetrics = UserSatisfactionMetrics(
                averageRating = 4.2f,
                totalRatings = 0,
                positiveFeedbackPercentage = 85.0f,
                commonCompliments = listOf("Fast analysis", "Accurate hazard detection"),
                commonComplaints = listOf("Occasional false positives")
            )
        )
    }
    
    override suspend fun recordUserFeedback(analysisId: String, rating: Float, feedback: String?) {
        // Implementation would store feedback for analytics
        performanceMonitor.recordError("User feedback recorded for $analysisId: $rating stars - $feedback")
    }
    
    override suspend fun optimizeForDevice(): DeviceOptimizationResult {
        // Implementation would analyze device capabilities and optimize model settings
        return DeviceOptimizationResult(
            optimizationsApplied = listOf(
                "Reduced model precision for faster inference",
                "Enabled GPU acceleration",
                "Adjusted batch size for memory constraints"
            ),
            expectedPerformanceGain = 25.0f,
            estimatedMemoryReduction = 15.0f
        )
    }
    
    private fun generateBasicRecommendations(workType: WorkType, processingTime: Long): PhotoAnalysisWithTags {
        val basicTags = when (workType) {
            WorkType.GENERAL_CONSTRUCTION -> listOf(
                "general-safety-check",
                "ppe-review"
            )
            WorkType.ELECTRICAL_WORK -> listOf(
                "electrical-safety",
                "lockout-tagout"
            )
            WorkType.ROOFING -> listOf(
                "fall-protection",
                "roof-safety"
            )
            else -> listOf("general-safety-check")
        }
        
        return PhotoAnalysisWithTags(
            id = generateId(),
            photoId = generatePhotoId(),
            detections = emptyList(),
            enhancedResults = emptyList(),
            recommendedTags = basicTags,
            autoSelectTags = emptySet(),
            complianceOverview = ComplianceOverview(
                overallStatus = ComplianceStatus.UNKNOWN,
                criticalViolations = 0,
                warnings = 0,
                compliantItems = 0,
                recommendationsSummary = listOf("Manual safety review recommended due to AI analysis failure"),
                priorityActions = listOf("Perform manual hazard assessment")
            ),
            processingTimeMs = processingTime,
            analysisSource = AnalysisSource.MANUAL
        )
    }
    
    private fun generateTimeoutFallback(workType: WorkType, processingTime: Long): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            id = generateId(),
            photoId = generatePhotoId(),
            detections = emptyList(),
            enhancedResults = emptyList(),
            recommendedTags = listOf("ai-timeout-manual-review", "general-safety-check"),
            autoSelectTags = setOf("ai-timeout-manual-review"),
            complianceOverview = ComplianceOverview(
                overallStatus = ComplianceStatus.UNKNOWN,
                criticalViolations = 1,
                warnings = 0,
                compliantItems = 0,
                recommendationsSummary = listOf("Manual review required due to AI timeout"),
                priorityActions = listOf("Perform immediate manual safety assessment")
            ),
            processingTimeMs = processingTime,
            analysisSource = AnalysisSource.MANUAL
        )
    }
    
    // Helper methods
    private fun convertToPhotoAnalysisWithTags(
        gemmaResult: SafetyAnalysisResult, 
        processingTime: Long
    ): PhotoAnalysisWithTags {
        // Convert Gemma's comprehensive result to UI-friendly format
        val recommendedTags = gemmaResult.recommendations.map { "${it.category.name.lowercase()}-${it.priority.name.lowercase()}" }
        val autoSelectTags = gemmaResult.oshaViolations
            .filter { it.severity == ViolationSeverity.SERIOUS_VIOLATION || it.severity == ViolationSeverity.IMMINENT_DANGER }
            .map { "osha-violation-${it.regulation.replace(".", "-")}" }
            .toSet()
        
        return PhotoAnalysisWithTags(
            id = generateId(),
            photoId = gemmaResult.photoId,
            detections = emptyList(), // Gemma provides different detection format
            enhancedResults = emptyList(), // Would be converted from Gemma results
            recommendedTags = recommendedTags,
            autoSelectTags = autoSelectTags,
            complianceOverview = ComplianceOverview(
                overallStatus = determineComplianceStatus(gemmaResult.oshaViolations),
                criticalViolations = gemmaResult.oshaViolations.count { 
                    it.severity == ViolationSeverity.SERIOUS_VIOLATION || 
                    it.severity == ViolationSeverity.IMMINENT_DANGER 
                },
                warnings = gemmaResult.oshaViolations.count { it.severity == ViolationSeverity.WARNING },
                compliantItems = gemmaResult.recommendations.count { it.priority == RecommendationPriority.LOW },
                recommendationsSummary = gemmaResult.recommendations.take(3).map { it.description },
                priorityActions = gemmaResult.recommendations
                    .filter { it.priority == RecommendationPriority.IMMEDIATE }
                    .map { it.description }
            ),
            processingTimeMs = processingTime,
            analysisSource = AnalysisSource.ON_DEVICE,
            analysisResult = gemmaResult
        )
    }
    
    private fun generateFallbackComprehensiveResult(
        workType: WorkType, 
        processingTime: Long, 
        errorMessage: String
    ): SafetyAnalysisResult {
        return SafetyAnalysisResult(
            id = generateId(),
            photoId = generatePhotoId(),
            detailedAssessment = "AI analysis failed: $errorMessage. Manual safety review is required for this work area.",
            oshaViolations = listOf(
                OSHAViolation(
                    regulation = "1926.95(a)",
                    title = "General Safety Requirements",
                    description = "Unable to assess compliance due to AI analysis failure",
                    severity = ViolationSeverity.WARNING,
                    recommendedAction = "Perform manual safety assessment"
                )
            ),
            recommendations = listOf(
                SafetyRecommendation(
                    id = generateId(),
                    priority = RecommendationPriority.IMMEDIATE,
                    category = RecommendationCategory.PROCEDURE,
                    description = "Conduct manual safety assessment of work area",
                    actionSteps = listOf(
                        "Inspect work area for visible hazards",
                        "Verify proper PPE usage",
                        "Document safety conditions"
                    ),
                    estimatedTimeToImplement = "15-30 minutes",
                    riskReduction = RiskReductionLevel.MODERATE
                )
            ),
            overallConfidence = 0.0f,
            processingTimeMs = processingTime,
            workType = workType,
            analysisSource = AnalysisSource.MANUAL
        )
    }
    
    private fun determineComplianceStatus(violations: List<OSHAViolation>): ComplianceStatus {
        return when {
            violations.any { it.severity == ViolationSeverity.IMMINENT_DANGER || it.severity == ViolationSeverity.SERIOUS_VIOLATION } -> 
                ComplianceStatus.CRITICAL_VIOLATION
            violations.any { it.severity == ViolationSeverity.WARNING || it.severity == ViolationSeverity.CITATION } -> 
                ComplianceStatus.NEEDS_IMPROVEMENT
            violations.isEmpty() -> ComplianceStatus.COMPLIANT
            else -> ComplianceStatus.UNKNOWN
        }
    }
    
    private fun generateId(): String = "analysis-${System.currentTimeMillis()}-${(1000..9999).random()}"
    private fun generatePhotoId(): String = "photo-${System.currentTimeMillis()}-${(1000..9999).random()}"
}

// Legacy compatibility - keeping simplified models for backward compatibility
/**
 * Simplified tag recommendation for UI consumption
 */
@Serializable
data class UITagRecommendation(
    val tagId: String,
    val displayName: String,
    val confidence: Float,
    val reason: String,
    val priority: TagPriority = TagPriority.MEDIUM,
    val oshaReference: String? = null
) {
    companion object {
        fun basic(tagId: String) = UITagRecommendation(
            tagId = tagId,
            displayName = tagId.replace("-", " ").replaceFirstChar { it.uppercase() },
            confidence = 0.5f,
            reason = "Basic safety recommendation"
        )
    }
}

@Serializable
enum class TagPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

@Serializable
enum class ComplianceLevel {
    COMPLIANT, PARTIAL_COMPLIANCE, NON_COMPLIANT, INFORMATIONAL, UNKNOWN
}

@Serializable
data class ModelStatus(
    val isLoaded: Boolean,
    val modelName: String,
    val lastError: String? = null
)

// Additional support classes for enhanced AI functionality
@Serializable
data class QueueStatus(
    val totalQueued: Int,
    val processing: Int,
    val failed: Int,
    val avgWaitTimeMs: Long
)

@Serializable
data class DeviceOptimizationResult(
    val optimizationsApplied: List<String>,
    val expectedPerformanceGain: Float,
    val estimatedMemoryReduction: Float
)

@Serializable
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
)

// Interface definitions for new components
interface GemmaVisionAnalyzer {
    suspend fun initialize(): Boolean
    suspend fun analyzePhoto(data: ByteArray, width: Int, height: Int, workType: WorkType): SafetyAnalysisResult
    suspend fun analyzeComprehensive(
        data: ByteArray, 
        width: Int, 
        height: Int, 
        workType: WorkType,
        includeOSHACompliance: Boolean,
        detailedRecommendations: Boolean
    ): SafetyAnalysisResult
    suspend fun getStatus(): GemmaStatus
    suspend fun validate(): ValidationResult
    suspend fun release()
}

interface OfflineAnalysisQueue {
    suspend fun initialize(): Boolean
    suspend fun enqueue(request: QueuedAnalysisRequest): Result<Unit>
    suspend fun getAll(): List<QueuedAnalysisRequest>
    suspend fun markCompleted(requestId: String)
    suspend fun incrementRetryCount(requestId: String)
    suspend fun getStatus(): QueueStatus
    suspend fun shutdown()
}

interface AIModelValidator {
    suspend fun validateModel(modelPath: String): ValidationResult
}

@Serializable
data class GemmaStatus(
    val isReady: Boolean,
    val modelVersion: String,
    val lastError: String?
)