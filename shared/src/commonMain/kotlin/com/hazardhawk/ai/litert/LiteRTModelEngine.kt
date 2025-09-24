package com.hazardhawk.ai.litert

import com.hazardhawk.core.models.SafetyAnalysis
import com.hazardhawk.core.models.WorkType
import com.hazardhawk.core.models.HazardType
import com.hazardhawk.core.models.Severity
import com.hazardhawk.core.models.PPEType
import com.hazardhawk.core.models.PPEDetection
import com.hazardhawk.core.models.OSHAViolation
import com.hazardhawk.core.models.RiskAssessment

/**
 * Cross-platform LiteRT model engine with hardware acceleration.
 * Provides unified interface for GPU/NPU processing across Android platforms.
 * 
 * Key Features:
 * - Hardware acceleration (CPU, GPU, NPU)
 * - Construction safety focused analysis
 * - OSHA compliance integration
 * - Memory-efficient processing
 * - Device capability detection
 */
expect class LiteRTModelEngine() {
    
    /**
     * Indicates if LiteRT is available on this platform.
     */
    val isAvailable: Boolean
    
    /**
     * Set of backends supported on this device.
     */
    val supportedBackends: Set<LiteRTBackend>
    
    /**
     * Currently active backend for processing.
     */
    val currentBackend: LiteRTBackend?
    
    /**
     * Initialize the LiteRT engine with specified model and backend.
     * 
     * @param modelPath Path to the .litertmlm model file
     * @param backend Processing backend to use (AUTO for automatic selection)
     * @return Success or failure with error details
     */
    suspend fun initialize(
        modelPath: String,
        backend: LiteRTBackend = LiteRTBackend.AUTO
    ): Result<Unit>
    
    /**
     * Generate comprehensive safety analysis using LiteRT model.
     * 
     * @param imageData Photo data for analysis
     * @param workType Construction work context
     * @param includeOSHACodes Include OSHA regulation references
     * @param confidenceThreshold Minimum confidence for hazard detection
     * @param progressCallback Optional callback for progress updates
     * @return Analysis result with hazards, PPE status, and recommendations
     */
    suspend fun generateSafetyAnalysis(
        imageData: ByteArray,
        workType: WorkType,
        includeOSHACodes: Boolean = true,
        confidenceThreshold: Float = 0.6f,
        progressCallback: ((LiteRTProgressUpdate) -> Unit)? = null
    ): Result<LiteRTAnalysisResult>
    
    /**
     * Get current performance metrics for monitoring.
     */
    fun getPerformanceMetrics(): LiteRTPerformanceMetrics
    
    /**
     * Clean up resources and release model memory.
     */
    fun cleanup()
}

/**
 * Available processing backends for LiteRT with performance characteristics.
 */
enum class LiteRTBackend(
    val displayName: String,
    val expectedTokensPerSecond: Float,
    val powerEfficiency: PowerEfficiency,
    val memoryRequirement: MemoryRequirement
) {
    /**
     * Automatic backend selection based on device capabilities.
     */
    AUTO(
        displayName = "Auto-Select",
        expectedTokensPerSecond = 0f, // Variable
        powerEfficiency = PowerEfficiency.OPTIMAL,
        memoryRequirement = MemoryRequirement.ADAPTIVE
    ),
    
    /**
     * CPU processing - baseline compatibility.
     */
    CPU(
        displayName = "CPU Processing",
        expectedTokensPerSecond = 243f,
        powerEfficiency = PowerEfficiency.HIGH,
        memoryRequirement = MemoryRequirement.LOW
    ),
    
    /**
     * GPU acceleration via OpenCL.
     */
    GPU_OPENCL(
        displayName = "GPU (OpenCL)",
        expectedTokensPerSecond = 1876f,
        powerEfficiency = PowerEfficiency.MEDIUM,
        memoryRequirement = MemoryRequirement.MEDIUM
    ),
    
    /**
     * GPU acceleration via OpenGL ES.
     */
    GPU_OPENGL(
        displayName = "GPU (OpenGL ES)",
        expectedTokensPerSecond = 1876f,
        powerEfficiency = PowerEfficiency.MEDIUM,
        memoryRequirement = MemoryRequirement.MEDIUM
    ),
    
    /**
     * Neural Processing Unit via Android NNAPI.
     */
    NPU_NNAPI(
        displayName = "NPU (NNAPI)",
        expectedTokensPerSecond = 5836f,
        powerEfficiency = PowerEfficiency.OPTIMAL,
        memoryRequirement = MemoryRequirement.LOW
    ),
    
    /**
     * Qualcomm Hexagon Tensor Processor.
     */
    NPU_QTI_HTP(
        displayName = "NPU (Qualcomm HTP)",
        expectedTokensPerSecond = 5836f,
        powerEfficiency = PowerEfficiency.OPTIMAL,
        memoryRequirement = MemoryRequirement.LOW
    )
}

enum class PowerEfficiency {
    LOW,     // High power consumption
    MEDIUM,  // Moderate power usage
    HIGH,    // Low power consumption
    OPTIMAL  // Best power efficiency
}

enum class MemoryRequirement {
    LOW,      // < 512MB
    MEDIUM,   // 512MB - 1GB
    HIGH,     // 1GB - 2GB
    ADAPTIVE  // Adjusts based on device
}

/**
 * Result from LiteRT model inference with construction safety focus.
 */
data class LiteRTAnalysisResult(
    /**
     * Detected safety hazards with OSHA classifications.
     */
    val hazards: List<DetectedHazard>,
    
    /**
     * Personal Protective Equipment detection results.
     */
    val ppeStatus: Map<PPEType, PPEDetection>,
    
    /**
     * OSHA regulation violations identified.
     */
    val oshaViolations: List<OSHAViolation>,
    
    /**
     * Overall risk assessment for the work area.
     */
    val overallRiskAssessment: RiskAssessment,
    
    /**
     * Model confidence in the analysis (0.0 - 1.0).
     */
    val confidence: Float,
    
    /**
     * Processing time for this analysis.
     */
    val processingTimeMs: Long,
    
    /**
     * Backend used for processing.
     */
    val backendUsed: LiteRTBackend,
    
    /**
     * Raw model outputs for debugging.
     */
    val debugInfo: LiteRTDebugInfo? = null
)

/**
 * Detected hazard with construction-specific details.
 */
data class DetectedHazard(
    val type: HazardType,
    val description: String,
    val severity: Severity,
    val confidence: Float,
    val boundingBox: BoundingBox?,
    val oshaCode: String? = null,
    val recommendations: List<String> = emptyList()
)

/**
 * Bounding box coordinates for hazard location.
 */
data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val confidence: Float
)

/**
 * Performance metrics from LiteRT processing.
 */
data class LiteRTPerformanceMetrics(
    val analysisCount: Long,
    val averageProcessingTimeMs: Long,
    val tokensPerSecond: Float,
    val peakMemoryUsageMB: Float,
    val averageMemoryUsageMB: Float,
    val successRate: Float,
    val preferredBackend: LiteRTBackend?,
    val thermalThrottlingDetected: Boolean
)

/**
 * Debug information for model analysis troubleshooting.
 */
data class LiteRTDebugInfo(
    val modelVersion: String,
    val inputPreprocessingTime: Long,
    val inferenceTime: Long,
    val postProcessingTime: Long,
    val rawOutputTensors: Map<String, FloatArray>? = null,
    val memoryPeakMB: Float,
    val deviceTemperature: Float? = null
)

/**
 * Progress update for local AI analysis with detailed status information.
 */
data class LiteRTProgressUpdate(
    val stage: LiteRTProcessingStage,
    val progress: Float, // 0.0 to 1.0
    val message: String,
    val backendUsed: LiteRTBackend? = null,
    val elapsedTimeMs: Long = 0L,
    val estimatedRemainingMs: Long? = null,
    val memoryUsageMB: Float? = null
)

/**
 * Stages of LiteRT processing for granular progress tracking.
 */
enum class LiteRTProcessingStage(val displayName: String, val expectedProgress: Float) {
    INITIALIZING("Initializing AI model", 0.1f),
    PREPROCESSING("Preprocessing image", 0.2f),
    INFERENCE("Analyzing safety hazards", 0.7f),
    POSTPROCESSING("Processing results", 0.9f),
    FINALIZING("Generating recommendations", 1.0f)
}

/**
 * Exception types specific to LiteRT operations.
 */
sealed class LiteRTException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    class InitializationException(message: String, cause: Throwable? = null) : 
        LiteRTException("LiteRT initialization failed: $message", cause)
    
    class ModelLoadException(message: String, cause: Throwable? = null) : 
        LiteRTException("Model loading failed: $message", cause)
    
    class InferenceException(message: String, cause: Throwable? = null) : 
        LiteRTException("Inference failed: $message", cause)
    
    class UnsupportedBackendException(backend: LiteRTBackend, cause: Throwable? = null) : 
        LiteRTException("Backend $backend not supported on this device", cause)
    
    class OutOfMemoryException(requiredMB: Float, availableMB: Float) : 
        LiteRTException("Insufficient memory: required ${requiredMB}MB, available ${availableMB}MB")
    
    class ThermalThrottlingException(temperature: Float) : 
        LiteRTException("Processing stopped due to thermal throttling: ${temperature}°C")
}