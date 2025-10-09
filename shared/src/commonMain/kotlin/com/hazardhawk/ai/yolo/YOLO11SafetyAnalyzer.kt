package com.hazardhawk.ai.yolo

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.datetime.Clock
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.domain.entities.HazardType
import com.hazardhawk.models.SafetyAnalysis
import com.hazardhawk.models.AnalysisType
import com.hazardhawk.models.Severity
import com.hazardhawk.models.AnalysisOptions
import com.hazardhawk.models.Hazard
import com.hazardhawk.models.OSHACode

/**
 * YOLO11 Safety Analyzer - Main orchestrator for HazardHawk construction safety analysis
 * 
 * This is the core integration class that ties together all YOLO11 components to provide
 * comprehensive AI-powered safety analysis for construction sites. It handles the complete
 * pipeline from image input to safety recommendations with OSHA compliance.
 * 
 * Key Features:
 * - Security-first approach with comprehensive validation
 * - Device-adaptive performance optimization  
 * - Construction-specific hazard detection and mapping
 * - Real-time performance monitoring and optimization
 * - Seamless integration with existing SafetyAnalysis system
 * - Hybrid AI capabilities (prepares for Gemini Vision integration)
 * 
 * Architecture:
 * Security Validation → Object Detection → Hazard Mapping → Safety Analysis
 * 
 * @author HazardHawk AI Team
 * @since YOLO11 Integration Phase 2
 * @version 1.0.0
 */
class YOLO11SafetyAnalyzer {
    
    companion object {
        private const val TAG = "YOLO11SafetyAnalyzer"
        private const val MAX_CONCURRENT_ANALYSES = 3
        private const val ANALYSIS_TIMEOUT_MS = 30000L
        private const val BATCH_SIZE_LIMIT = 10
        
        // Performance thresholds
        private const val MIN_CONFIDENCE_THRESHOLD = 0.3f
        private const val MAX_ANALYSIS_TIME_MS = 5000L
        
        // Analysis quality levels
        private val QUALITY_PRESETS = mapOf(
            AnalysisQuality.FAST to AnalysisPreset(
                confidenceThreshold = 0.5f,
                enableContextualAnalysis = false,
                maxProcessingTimeMs = 2000L
            ),
            AnalysisQuality.BALANCED to AnalysisPreset(
                confidenceThreshold = 0.6f,
                enableContextualAnalysis = true,
                maxProcessingTimeMs = 4000L
            ),
            AnalysisQuality.ACCURATE to AnalysisPreset(
                confidenceThreshold = 0.7f,
                enableContextualAnalysis = true,
                maxProcessingTimeMs = 8000L
            )
        )
    }
    
    // Core components
    private val securityManager = YOLOSecurityManager()
    private val performanceOptimizer = YOLOPerformanceOptimizer()
    private val hazardMapper = ConstructionHazardMapper()
    private var objectDetector: YOLOObjectDetector? = null
    
    // State management
    private var isInitialized = false
    private var currentConfiguration: YOLOOptimizationConfig? = null
    private var currentDeviceCapability: DeviceCapability? = null
    
    // Coroutine management
    private val analyzerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val analysisJobLimiter = Semaphore(MAX_CONCURRENT_ANALYSES)
    
    // Performance monitoring
    private val _analysisResults = MutableSharedFlow<AnalysisResult>()
    private val _performanceUpdates = MutableSharedFlow<PerformanceUpdate>()
    
    val analysisResults: SharedFlow<AnalysisResult> = _analysisResults.asSharedFlow()
    val performanceUpdates: SharedFlow<PerformanceUpdate> = _performanceUpdates.asSharedFlow()
    
    /**
     * Initialize the YOLO11 safety analyzer with device-adaptive configuration
     * 
     * @param enableGPU Whether to enable GPU acceleration if available
     * @param optimizeForBattery Whether to prioritize battery life over performance
     * @return Result indicating initialization success or failure
     */
    suspend fun initialize(
        enableGPU: Boolean = true,
        optimizeForBattery: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            logAnalysis("INITIALIZATION_STARTED", mapOf(
                "enable_gpu" to enableGPU.toString(),
                "optimize_battery" to optimizeForBattery.toString()
            ))
            
            // Step 1: Create and assess device capability
            objectDetector = YOLODetectorFactory.createDetector(
                enableGPU = enableGPU,
                optimizeForBattery = optimizeForBattery
            )
            
            currentDeviceCapability = objectDetector?.getDeviceCapability()
                ?: return@withContext Result.failure(
                    YOLO11AnalysisException.InitializationFailed("Failed to assess device capability")
                )
            
            // Step 2: Get optimal configuration for device
            currentConfiguration = performanceOptimizer.getOptimalConfiguration(currentDeviceCapability!!)
            
            // Step 3: Create secure runtime context
            val securityContext = securityManager.createSecureRuntimeContext().getOrElse {
                return@withContext Result.failure(
                    YOLO11AnalysisException.SecurityValidationFailed("Failed to create secure runtime context")
                )
            }
            
            // Step 4: Initialize object detector with validated configuration
            val modelConfig = YOLOModelConfiguration(
                modelName = currentConfiguration!!.modelVariant.name.lowercase(),
                modelPath = "models/${currentConfiguration!!.modelVariant.name.lowercase()}.onnx",
                inputSize = currentConfiguration!!.inputResolution,
                numClasses = 80, // Standard YOLO classes + construction-specific
                confidenceThreshold = currentConfiguration!!.confidenceThreshold,
                iouThreshold = currentConfiguration!!.iouThreshold
            )
            
            objectDetector?.initialize(modelConfig)?.getOrElse {
                return@withContext Result.failure(
                    YOLO11AnalysisException.ModelInitializationFailed("YOLO model initialization failed: ${it.message}")
                )
            }
            
            // Step 5: Start performance monitoring
            startPerformanceMonitoring()
            
            isInitialized = true
            
            logAnalysis("INITIALIZATION_COMPLETED", mapOf(
                "model_variant" to currentConfiguration!!.modelVariant.name,
                "input_resolution" to "${currentConfiguration!!.inputResolution.width}x${currentConfiguration!!.inputResolution.height}",
                "confidence_threshold" to currentConfiguration!!.confidenceThreshold.toString()
            ))
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            logAnalysis("INITIALIZATION_FAILED", mapOf("error" to e.message.orEmpty()))
            Result.failure(YOLO11AnalysisException.InitializationFailed("Initialization failed: ${e.message}", e))
        }
    }
    
    /**
     * Perform comprehensive safety analysis on a single image
     * 
     * @param imageData Raw image bytes (JPEG, PNG, etc.)
     * @param workType Type of construction work being performed
     * @param photoId Unique identifier for the photo
     * @param quality Analysis quality level (FAST, BALANCED, ACCURATE)
     * @return Comprehensive safety analysis result
     */
    suspend fun analyzeSafety(
        imageData: ByteArray,
        workType: WorkType = WorkType.GENERAL_CONSTRUCTION,
        photoId: String = "photo-${Clock.System.now().toEpochMilliseconds()}",
        quality: AnalysisQuality = AnalysisQuality.BALANCED
    ): Result<SafetyAnalysis> {
        return analysisJobLimiter.withPermit {
            withContext(Dispatchers.Default) {
            val startTime = Clock.System.now().toEpochMilliseconds()
            val analysisId = "analysis-$startTime"
            
            try {
                if (!isInitialized) {
                    return@withContext Result.failure(
                        YOLO11AnalysisException.NotInitialized("Analyzer not initialized. Call initialize() first.")
                    )
                }
                
                logAnalysis("SAFETY_ANALYSIS_STARTED", mapOf(
                    "analysis_id" to analysisId,
                    "photo_id" to photoId,
                    "work_type" to workType.name,
                    "quality" to quality.name,
                    "image_size_bytes" to imageData.size.toString()
                ))
                
                // Step 1: Security validation
                val imageValidation = validateImageSecurity(imageData)
                if (imageValidation.isFailure) {
                    return@withContext imageValidation
                }
                
                // Step 2: Apply quality preset
                val preset = QUALITY_PRESETS[quality]!!
                applyQualityPreset(preset)
                
                // Step 3: Object detection with timeout
                val detectionResult = withTimeout(preset.maxProcessingTimeMs) {
                    objectDetector?.detectObjects(imageData, workType)
                        ?: Result.failure(YOLO11AnalysisException.DetectionFailed("Object detector not available"))
                }
                
                val yoloResult = detectionResult.getOrElse {
                    return@withContext Result.failure(
                        YOLO11AnalysisException.DetectionFailed("YOLO detection failed: ${it.message}", it)
                    )
                }
                
                // Step 4: Construction hazard mapping
                val safetyAnalysis = hazardMapper.mapToSafetyAnalysis(
                    yoloResult = yoloResult,
                    workType = workType,
                    photoId = photoId
                )
                
                // Step 5: Performance monitoring and optimization
                val processingTime = Clock.System.now().toEpochMilliseconds() - startTime
                val performanceAnalysis = performanceOptimizer.monitorPerformance(
                    detection = yoloResult,
                    deviceInfo = currentDeviceCapability!!
                )
                
                // Step 6: Emit results for monitoring
                val analysisResult = AnalysisResult(
                    analysisId = analysisId,
                    safetyAnalysis = safetyAnalysis,
                    processingTimeMs = processingTime,
                    detectionCount = yoloResult.detections.size,
                    performanceScore = performanceAnalysis.performanceScore,
                    quality = quality
                )
                
                analyzerScope.launch {
                    _analysisResults.emit(analysisResult)
                    _performanceUpdates.emit(
                        PerformanceUpdate(
                            timestamp = Clock.System.now().toEpochMilliseconds(),
                            analysis = performanceAnalysis,
                            recommendations = generatePerformanceRecommendations(performanceAnalysis)
                        )
                    )
                }
                
                logAnalysis("SAFETY_ANALYSIS_COMPLETED", mapOf(
                    "analysis_id" to analysisId,
                    "processing_time_ms" to processingTime.toString(),
                    "detections_count" to yoloResult.detections.size.toString(),
                    "hazards_count" to safetyAnalysis.hazards.size.toString(),
                    "overall_severity" to safetyAnalysis.severity.name,
                    "ai_confidence" to safetyAnalysis.aiConfidence.toString()
                ))
                
                Result.success(safetyAnalysis)
                
            } catch (e: TimeoutCancellationException) {
                val preset = QUALITY_PRESETS[quality]!!
                logAnalysis("SAFETY_ANALYSIS_TIMEOUT", mapOf(
                    "analysis_id" to analysisId,
                    "timeout_ms" to preset.maxProcessingTimeMs.toString()
                ))
                Result.failure(YOLO11AnalysisException.AnalysisTimeout("Analysis timed out after ${preset.maxProcessingTimeMs}ms"))
                
            } catch (e: Exception) {
                logAnalysis("SAFETY_ANALYSIS_FAILED", mapOf(
                    "analysis_id" to analysisId,
                    "error" to e.message.orEmpty()
                ))
                Result.failure(YOLO11AnalysisException.AnalysisFailed("Safety analysis failed: ${e.message}", e))
            }
        }
        }
    }
    
    /**
     * Perform batch safety analysis on multiple images
     * 
     * @param imageBatch List of image data with metadata
     * @param workType Type of construction work being performed
     * @param quality Analysis quality level
     * @return Flow of analysis results
     */
    fun analyzeSafetyBatch(
        imageBatch: List<ImageData>,
        workType: WorkType = WorkType.GENERAL_CONSTRUCTION,
        quality: AnalysisQuality = AnalysisQuality.BALANCED
    ): Flow<Result<SafetyAnalysis>> = flow {
        if (!isInitialized) {
            emit(Result.failure(
                YOLO11AnalysisException.NotInitialized("Analyzer not initialized. Call initialize() first.")
            ))
            return@flow
        }
        
        if (imageBatch.size > BATCH_SIZE_LIMIT) {
            emit(Result.failure(
                YOLO11AnalysisException.BatchSizeExceeded("Batch size ${imageBatch.size} exceeds limit of $BATCH_SIZE_LIMIT")
            ))
            return@flow
        }
        
        logAnalysis("BATCH_ANALYSIS_STARTED", mapOf(
            "batch_size" to imageBatch.size.toString(),
            "work_type" to workType.name,
            "quality" to quality.name
        ))
        
        imageBatch.forEach { imageData ->
            val result = analyzeSafety(
                imageData = imageData.data,
                workType = workType,
                photoId = imageData.id,
                quality = quality
            )
            emit(result)
            
            // Add small delay between analyses to prevent overwhelming the system
            delay(100)
        }
        
        logAnalysis("BATCH_ANALYSIS_COMPLETED", mapOf(
            "batch_size" to imageBatch.size.toString()
        ))
    }.flowOn(Dispatchers.Default)
    
    /**
     * Generate comprehensive benchmark report
     * 
     * @return Detailed performance and accuracy metrics
     */
    suspend fun generateBenchmarkReport(): Result<YOLO11BenchmarkReport> = withContext(Dispatchers.Default) {
        try {
            if (!isInitialized) {
                return@withContext Result.failure(
                    YOLO11AnalysisException.NotInitialized("Analyzer not initialized")
                )
            }
            
            val yoloBenchmark = performanceOptimizer.generateBenchmarkReport()
            val memoryOptimization = performanceOptimizer.optimizeMemoryUsage()
            
            val report = YOLO11BenchmarkReport(
                reportId = "yolo11-benchmark-${Clock.System.now().toEpochMilliseconds()}",
                generatedAt = Clock.System.now().toEpochMilliseconds(),
                systemInfo = SystemInfo(
                    deviceCapability = currentDeviceCapability!!,
                    modelConfiguration = currentConfiguration!!
                ),
                performanceMetrics = yoloBenchmark,
                memoryOptimization = memoryOptimization,
                recommendations = generateSystemRecommendations()
            )
            
            Result.success(report)
            
        } catch (e: Exception) {
            Result.failure(YOLO11AnalysisException.BenchmarkFailed("Benchmark generation failed: ${e.message}", e))
        }
    }
    
    /**
     * Update analysis parameters at runtime
     * 
     * @param newQuality New quality preset to apply
     * @param confidenceOverride Optional confidence threshold override
     */
    suspend fun updateAnalysisParameters(
        newQuality: AnalysisQuality,
        confidenceOverride: Float? = null
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val preset = QUALITY_PRESETS[newQuality]!!
            val finalConfidence = confidenceOverride ?: preset.confidenceThreshold
            
            // Validate confidence threshold
            if (finalConfidence !in MIN_CONFIDENCE_THRESHOLD..1.0f) {
                return@withContext Result.failure(
                    YOLO11AnalysisException.InvalidParameters("Confidence threshold must be between $MIN_CONFIDENCE_THRESHOLD and 1.0")
                )
            }
            
            objectDetector?.updateDetectionParameters(
                confidenceThreshold = finalConfidence,
                iouThreshold = currentConfiguration?.iouThreshold ?: 0.45f
            )
            
            logAnalysis("PARAMETERS_UPDATED", mapOf(
                "quality" to newQuality.name,
                "confidence_threshold" to finalConfidence.toString()
            ))
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(YOLO11AnalysisException.ParameterUpdateFailed("Parameter update failed: ${e.message}", e))
        }
    }
    
    /**
     * Get current analyzer status and health
     */
    fun getAnalyzerStatus(): YOLO11AnalyzerStatus {
        return YOLO11AnalyzerStatus(
            isInitialized = isInitialized,
            detectorStatus = objectDetector?.detectorStatus?.value ?: YOLODetectorStatus.UNINITIALIZED,
            currentConfiguration = currentConfiguration,
            deviceCapability = currentDeviceCapability,
            lastPerformanceMetrics = objectDetector?.getLastPerformanceMetrics()
        )
    }
    
    /**
     * Cleanup resources and shutdown analyzer
     */
    suspend fun cleanup() {
        try {
            logAnalysis("CLEANUP_STARTED", emptyMap())
            
            objectDetector?.cleanup()
            analyzerScope.cancel()
            
            isInitialized = false
            currentConfiguration = null
            currentDeviceCapability = null
            objectDetector = null
            
            logAnalysis("CLEANUP_COMPLETED", emptyMap())
            
        } catch (e: Exception) {
            logAnalysis("CLEANUP_FAILED", mapOf("error" to e.message.orEmpty()))
        }
    }
    
    // Private helper methods
    
    private suspend fun validateImageSecurity(imageData: ByteArray): Result<SafetyAnalysis> {
        // Extract basic image dimensions (this would be more sophisticated in practice)
        val width = 640 // Mock value - would extract from image headers
        val height = 480 // Mock value - would extract from image headers
        
        val validationResult = securityManager.validateInferenceInput(
            imageData = imageData,
            width = width,
            height = height
        )
        
        return if (validationResult.isSuccess) {
            Result.success(SafetyAnalysis(
                id = "temp",
                photoId = "temp",
                analyzedAt = Clock.System.now(),
                analysisType = AnalysisType.ON_DEVICE
            ))
        } else {
            Result.failure(
                YOLO11AnalysisException.SecurityValidationFailed(
                    "Image security validation failed: ${validationResult.exceptionOrNull()?.message}"
                )
            )
        }
    }
    
    private fun applyQualityPreset(preset: AnalysisPreset) {
        objectDetector?.updateDetectionParameters(
            confidenceThreshold = preset.confidenceThreshold,
            iouThreshold = currentConfiguration?.iouThreshold ?: 0.45f
        )
    }
    
    private fun startPerformanceMonitoring() {
        analyzerScope.launch {
            // Monitor performance updates from optimizer
            performanceOptimizer.optimizationUpdates.collect { update ->
                // Apply dynamic optimizations
                currentConfiguration = update.newConfiguration
                
                logAnalysis("PERFORMANCE_OPTIMIZATION_APPLIED", mapOf(
                    "optimizations" to update.appliedOptimizations.joinToString(", ")
                ))
            }
        }
    }
    
    private fun generatePerformanceRecommendations(
        analysis: PerformanceAnalysis
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (analysis.averageProcessingTime > MAX_ANALYSIS_TIME_MS) {
            recommendations.add("Consider using a lighter model variant for faster processing")
        }
        
        if (analysis.memoryPressure > 0.8) {
            recommendations.add("Enable aggressive memory optimization to reduce memory usage")
        }
        
        if (analysis.performanceScore < 0.7) {
            recommendations.add("Performance below optimal - consider adjusting quality settings")
        }
        
        return recommendations
    }
    
    private fun generateSystemRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        currentDeviceCapability?.let { capability ->
            when (capability.performanceLevel) {
                PerformanceLevel.LOW -> {
                    recommendations.add("Consider upgrading device for better analysis performance")
                    recommendations.add("Use FAST quality setting for optimal battery life")
                }
                PerformanceLevel.MEDIUM -> {
                    recommendations.add("Device well-suited for balanced analysis quality")
                    recommendations.add("Consider enabling GPU acceleration if available")
                }
                PerformanceLevel.HIGH -> {
                    recommendations.add("Device capable of highest quality analysis")
                    recommendations.add("Consider using ACCURATE quality for critical safety analysis")
                }
            }
        }
        
        return recommendations
    }
    
    private fun logAnalysis(event: String, details: Map<String, String>) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        println("[$TAG] $timestamp - $event: ${details.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
    }
}

// Supporting data classes and enums

@Serializable
data class ImageData(
    val id: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        
        other as ImageData
        
        if (id != other.id) return false
        if (!data.contentEquals(other.data)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

@Serializable
enum class AnalysisQuality {
    FAST, BALANCED, ACCURATE
}

@Serializable
data class AnalysisPreset(
    val confidenceThreshold: Float,
    val enableContextualAnalysis: Boolean,
    val maxProcessingTimeMs: Long
)

@Serializable
data class AnalysisResult(
    val analysisId: String,
    val safetyAnalysis: SafetyAnalysis,
    val processingTimeMs: Long,
    val detectionCount: Int,
    val performanceScore: Double,
    val quality: AnalysisQuality
)

@Serializable
data class PerformanceUpdate(
    val timestamp: Long,
    val analysis: PerformanceAnalysis,
    val recommendations: List<String>
)

@Serializable
data class YOLO11AnalyzerStatus(
    val isInitialized: Boolean,
    val detectorStatus: YOLODetectorStatus,
    val currentConfiguration: YOLOOptimizationConfig?,
    val deviceCapability: DeviceCapability?,
    @Contextual val lastPerformanceMetrics: YOLOPerformanceMetrics?
)

@Serializable
data class SystemInfo(
    val deviceCapability: DeviceCapability,
    val modelConfiguration: YOLOOptimizationConfig
)

@Serializable
data class YOLO11BenchmarkReport(
    val reportId: String,
    val generatedAt: Long,
    val systemInfo: SystemInfo,
    val performanceMetrics: YOLOBenchmarkReport,
    val memoryOptimization: MemoryOptimizationResult,
    val recommendations: List<String>
)

/**
 * Exception types for YOLO11 analysis errors
 */
sealed class YOLO11AnalysisException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotInitialized(message: String) : YOLO11AnalysisException(message)
    class InitializationFailed(message: String, cause: Throwable? = null) : YOLO11AnalysisException(message, cause)
    class SecurityValidationFailed(message: String) : YOLO11AnalysisException(message)
    class ModelInitializationFailed(message: String) : YOLO11AnalysisException(message)
    class DetectionFailed(message: String, cause: Throwable? = null) : YOLO11AnalysisException(message, cause)
    class AnalysisTimeout(message: String) : YOLO11AnalysisException(message)
    class AnalysisFailed(message: String, cause: Throwable? = null) : YOLO11AnalysisException(message, cause)
    class BatchSizeExceeded(message: String) : YOLO11AnalysisException(message)
    class BenchmarkFailed(message: String, cause: Throwable? = null) : YOLO11AnalysisException(message, cause)
    class InvalidParameters(message: String) : YOLO11AnalysisException(message)
    class ParameterUpdateFailed(message: String, cause: Throwable? = null) : YOLO11AnalysisException(message, cause)
}
