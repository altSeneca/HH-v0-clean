package com.hazardhawk.ai

import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.StateFlow

/**
 * Gemma 3N E2B multimodal AI analyzer for construction safety analysis.
 * 
 * Provides multimodal construction safety analysis using Google's Gemma 3N E2B model
 * with enhanced vision-language understanding for hazard identification and safety
 * assessment in construction environments.
 * 
 * This expect/actual pattern allows platform-specific implementations using:
 * - Android: ONNX Runtime Mobile with GPU acceleration
 * - iOS: Core ML or ONNX Runtime
 * - Desktop: ONNX Runtime with hardware optimization
 * - Web: TensorFlow.js or ONNX.js
 */
expect class GemmaVisionAnalyzer {
    /**
     * Initialize the Gemma vision model with configuration.
     * 
     * @param modelPath Path to the Gemma model files (vision_encoder.onnx, decoder.onnx)
     * @param confidenceThreshold Minimum confidence for analysis results (0.0-1.0)
     * @return True if initialization successful
     */
    suspend fun initialize(
        modelPath: String,
        confidenceThreshold: Float = 0.6f
    ): Boolean
    
    /**
     * Analyze construction safety using multimodal AI.
     * 
     * Combines vision and language understanding to provide comprehensive safety analysis
     * including hazard identification, OSHA compliance assessment, and safety recommendations.
     * 
     * @param imageData Raw image bytes
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @param analysisPrompt Optional specific analysis prompt for targeted assessment
     * @return Comprehensive safety analysis result
     */
    suspend fun analyzeConstructionSafety(
        imageData: ByteArray,
        width: Int,
        height: Int,
        analysisPrompt: String = DEFAULT_CONSTRUCTION_SAFETY_PROMPT
    ): SafetyAnalysisResult
    
    /**
     * Check if model is loaded and ready.
     */
    fun isModelLoaded(): Boolean
    
    /**
     * Get model information and capabilities.
     */
    fun getModelInfo(): GemmaModelInfo
    
    /**
     * Get current performance metrics.
     */
    fun getPerformanceMetrics(): AIPerformanceMetrics
    
    /**
     * Get device compatibility information.
     */
    fun getDeviceCompatibility(): DeviceCompatibilityInfo
    
    /**
     * Monitor performance metrics in real-time.
     */
    val performanceFlow: StateFlow<AIPerformanceMetrics>
    
    /**
     * Release model resources.
     */
    suspend fun release()
    
    companion object {
        const val DEFAULT_CONSTRUCTION_SAFETY_PROMPT = """
            Analyze this construction site image for safety hazards and OSHA compliance:
            
            1. Identify visible safety hazards and risks
            2. Assess PPE compliance (hard hats, safety vests, fall protection)
            3. Evaluate equipment safety and proper usage
            4. Check for OSHA violations and compliance issues
            5. Provide specific safety recommendations
            
            Focus on: fall protection, electrical safety, PPE requirements, equipment operation,
            housekeeping, and structural hazards. Provide specific OSHA regulation references
            where applicable.
        """.trimIndent()
    }
}

/**
 * Comprehensive safety analysis result from Gemma multimodal analysis.
 */
@Serializable
data class SafetyAnalysisResult(
    val hazardIdentifications: List<IdentifiedHazard>,
    val ppeCompliance: PPEComplianceAssessment,
    val oshaCompliance: OSHAComplianceAssessment,
    val safetyRecommendations: List<SafetyRecommendation>,
    val overallRiskLevel: RiskLevel,
    val confidence: Float,
    val processingTimeMs: Long,
    val analysisMetadata: AnalysisMetadata
) {
    companion object {
        fun empty() = SafetyAnalysisResult(
            hazardIdentifications = emptyList(),
            ppeCompliance = PPEComplianceAssessment.unknown(),
            oshaCompliance = OSHAComplianceAssessment.unknown(),
            safetyRecommendations = emptyList(),
            overallRiskLevel = RiskLevel.UNKNOWN,
            confidence = 0.0f,
            processingTimeMs = 0L,
            analysisMetadata = AnalysisMetadata.default()
        )
        
        fun error(message: String) = SafetyAnalysisResult(
            hazardIdentifications = emptyList(),
            ppeCompliance = PPEComplianceAssessment.unknown(),
            oshaCompliance = OSHAComplianceAssessment.unknown(),
            safetyRecommendations = listOf(
                SafetyRecommendation(
                    category = "error",
                    description = "Analysis failed: $message",
                    priority = RecommendationPriority.HIGH,
                    oshaReference = null
                )
            ),
            overallRiskLevel = RiskLevel.UNKNOWN,
            confidence = 0.0f,
            processingTimeMs = 0L,
            analysisMetadata = AnalysisMetadata.default()
        )
    }
}

/**
 * Individual hazard identified by multimodal analysis.
 */
@Serializable
data class IdentifiedHazard(
    val hazardType: String,
    val description: String,
    val severity: HazardSeverity,
    val location: HazardLocation?,
    val confidence: Float,
    val oshaViolation: OSHAViolation?,
    val immediateAction: String?
)

/**
 * Location information for identified hazards.
 */
@Serializable
data class HazardLocation(
    val boundingBox: BoundingBox?,
    val description: String
)

/**
 * PPE compliance assessment from multimodal analysis.
 */
@Serializable
data class PPEComplianceAssessment(
    val hardHatCompliance: ComplianceStatus,
    val safetyVestCompliance: ComplianceStatus,
    val eyeProtectionCompliance: ComplianceStatus,
    val fallProtectionCompliance: ComplianceStatus,
    val overallPPEScore: Float,
    val violations: List<PPEViolation>
) {
    companion object {
        fun unknown() = PPEComplianceAssessment(
            hardHatCompliance = ComplianceStatus.UNKNOWN,
            safetyVestCompliance = ComplianceStatus.UNKNOWN,
            eyeProtectionCompliance = ComplianceStatus.UNKNOWN,
            fallProtectionCompliance = ComplianceStatus.UNKNOWN,
            overallPPEScore = 0.0f,
            violations = emptyList()
        )
    }
}

/**
 * OSHA compliance assessment from multimodal analysis.
 */
@Serializable
data class OSHAComplianceAssessment(
    val complianceLevel: ComplianceLevel,
    val violations: List<OSHAViolation>,
    val criticalViolations: Int,
    val overallComplianceScore: Float
) {
    companion object {
        fun unknown() = OSHAComplianceAssessment(
            complianceLevel = ComplianceLevel.UNKNOWN,
            violations = emptyList(),
            criticalViolations = 0,
            overallComplianceScore = 0.0f
        )
    }
}

/**
 * Safety recommendation from multimodal analysis.
 */
@Serializable
data class SafetyRecommendation(
    val category: String,
    val description: String,
    val priority: RecommendationPriority,
    val oshaReference: String?
)

/**
 * PPE violation details.
 */
@Serializable
data class PPEViolation(
    val ppeType: String,
    val violation: String,
    val personLocation: BoundingBox?,
    val oshaReference: String
)

/**
 * OSHA violation details.
 */
@Serializable
data class OSHAViolation(
    val regulation: String,
    val description: String,
    val severity: ViolationSeverity,
    val recommendedAction: String
)

/**
 * Analysis metadata for transparency and debugging.
 */
@Serializable
data class AnalysisMetadata(
    val modelVersion: String,
    val analysisTimestamp: Long,
    val imageProcessingTime: Long,
    val inferenceTime: Long,
    val postProcessingTime: Long,
    val deviceInfo: String
) {
    companion object {
        fun default() = AnalysisMetadata(
            modelVersion = "gemma-3n-e2b-1.0",
            analysisTimestamp = System.currentTimeMillis(),
            imageProcessingTime = 0L,
            inferenceTime = 0L,
            postProcessingTime = 0L,
            deviceInfo = "unknown"
        )
    }
}

/**
 * Gemma model information and capabilities.
 */
@Serializable
data class GemmaModelInfo(
    val modelName: String,
    val version: String,
    val inputImageSize: ImageSize,
    val maxSequenceLength: Int,
    val modelFormat: ModelFormat,
    val modelSizeMB: Float,
    val supportsMultimodal: Boolean,
    val memoryFootprintMB: Int
)

/**
 * Overall risk level assessment.
 */
@Serializable
enum class RiskLevel(val displayName: String, val color: String) {
    LOW("Low Risk", "#4CAF50"),
    MEDIUM("Medium Risk", "#FF9800"),
    HIGH("High Risk", "#F44336"),
    CRITICAL("Critical Risk", "#B71C1C"),
    UNKNOWN("Unknown Risk", "#9E9E9E")
}

/**
 * Compliance status for various safety requirements.
 */
@Serializable
enum class ComplianceStatus {
    COMPLIANT,
    NON_COMPLIANT,
    PARTIAL_COMPLIANCE,
    NOT_APPLICABLE,
    UNKNOWN
}

/**
 * Safety recommendation priority levels.
 */
@Serializable
enum class RecommendationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * OSHA violation severity levels.
 */
@Serializable
enum class ViolationSeverity {
    MINOR,
    SERIOUS,
    WILLFUL,
    IMMINENT_DANGER
}

/**
 * AI Performance metrics for monitoring and optimization.
 */
@Serializable
data class AIPerformanceMetrics(
    val analysisTimeMs: Long = 0L,
    val memoryUsageMB: Int = 0,
    val peakMemoryMB: Int = 0,
    val successRate: Float = 0f,
    val errorRate: Float = 0f,
    val batteryImpactPercent: Float = 0f,
    val modelLoadTimeMs: Long = 0L,
    val imageProcessingTimeMs: Long = 0L,
    val inferenceTimeMs: Long = 0L,
    val postProcessingTimeMs: Long = 0L,
    val totalAnalysisCount: Int = 0,
    val failedAnalysisCount: Int = 0,
    val averageAnalysisTime: Float = 0f,
    val deviceTemperatureC: Float = 0f,
    val gpuUtilizationPercent: Float = 0f,
    val cpuUtilizationPercent: Float = 0f,
    val networkLatencyMs: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        fun initial() = AIPerformanceMetrics()
    }
    
    /**
     * Calculate if performance meets target requirements.
     */
    fun meetsPerformanceTargets(): PerformanceStatus {
        val analysisTimeTarget = analysisTimeMs <= 3000L
        val memoryTarget = memoryUsageMB <= 2048
        val batteryTarget = batteryImpactPercent <= 3f
        val successRateTarget = successRate >= 0.95f
        
        return when {
            analysisTimeTarget && memoryTarget && batteryTarget && successRateTarget -> 
                PerformanceStatus.OPTIMAL
            successRateTarget && analysisTimeMs <= 5000L && memoryUsageMB <= 3072 -> 
                PerformanceStatus.ACCEPTABLE
            successRate >= 0.80f -> 
                PerformanceStatus.DEGRADED
            else -> 
                PerformanceStatus.CRITICAL
        }
    }
}

/**
 * Device compatibility information for AI processing.
 */
@Serializable
data class DeviceCompatibilityInfo(
    val deviceModel: String,
    val osVersion: String,
    val totalMemoryMB: Int,
    val availableMemoryMB: Int,
    val cpuCores: Int,
    val hasDedicatedGPU: Boolean,
    val hasNNAPI: Boolean,
    val supportsGemmaAnalysis: Boolean,
    val recommendedSettings: RecommendedAISettings,
    val compatibilityScore: Float,
    val estimatedPerformance: EstimatedPerformance,
    val limitations: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    companion object {
        fun unknown() = DeviceCompatibilityInfo(
            deviceModel = "Unknown",
            osVersion = "Unknown",
            totalMemoryMB = 0,
            availableMemoryMB = 0,
            cpuCores = 1,
            hasDedicatedGPU = false,
            hasNNAPI = false,
            supportsGemmaAnalysis = false,
            recommendedSettings = RecommendedAISettings.minimal(),
            compatibilityScore = 0f,
            estimatedPerformance = EstimatedPerformance.unknown()
        )
    }
}

/**
 * Recommended AI settings based on device capabilities.
 */
@Serializable
data class RecommendedAISettings(
    val enableGPUAcceleration: Boolean,
    val enableNNAPI: Boolean,
    val maxConcurrentAnalyses: Int,
    val recommendedInputSize: Int,
    val enableModelQuantization: Boolean,
    val enableMemoryMapping: Boolean,
    val backgroundProcessingLimit: Int,
    val maxModelCacheSize: Int,
    val enableThermalThrottling: Boolean
) {
    companion object {
        fun minimal() = RecommendedAISettings(
            enableGPUAcceleration = false,
            enableNNAPI = false,
            maxConcurrentAnalyses = 1,
            recommendedInputSize = 224,
            enableModelQuantization = true,
            enableMemoryMapping = false,
            backgroundProcessingLimit = 1,
            maxModelCacheSize = 512,
            enableThermalThrottling = true
        )
        
        fun optimal() = RecommendedAISettings(
            enableGPUAcceleration = true,
            enableNNAPI = true,
            maxConcurrentAnalyses = 2,
            recommendedInputSize = 224,
            enableModelQuantization = false,
            enableMemoryMapping = true,
            backgroundProcessingLimit = 3,
            maxModelCacheSize = 2048,
            enableThermalThrottling = false
        )
    }
}

/**
 * Estimated performance characteristics.
 */
@Serializable
data class EstimatedPerformance(
    val expectedAnalysisTimeMs: Long,
    val expectedMemoryUsageMB: Int,
    val expectedSuccessRate: Float,
    val expectedBatteryImpactPercent: Float,
    val thermalImpact: ThermalImpact,
    val reliabilityScore: Float
) {
    companion object {
        fun unknown() = EstimatedPerformance(
            expectedAnalysisTimeMs = 5000L,
            expectedMemoryUsageMB = 1024,
            expectedSuccessRate = 0.5f,
            expectedBatteryImpactPercent = 5f,
            thermalImpact = ThermalImpact.MODERATE,
            reliabilityScore = 0.5f
        )
    }
}

/**
 * Performance status categories.
 */
@Serializable
enum class PerformanceStatus(val displayName: String, val color: String) {
    OPTIMAL("Optimal Performance", "#4CAF50"),
    ACCEPTABLE("Acceptable Performance", "#FF9800"),
    DEGRADED("Degraded Performance", "#F44336"),
    CRITICAL("Critical Issues", "#B71C1C")
}

/**
 * Thermal impact levels.
 */
@Serializable
enum class ThermalImpact {
    MINIMAL,
    LOW,
    MODERATE,
    HIGH,
    CRITICAL
}

/**
 * Memory management strategies.
 */
@Serializable
enum class MemoryStrategy {
    CONSERVATIVE,
    BALANCED,
    AGGRESSIVE,
    MAXIMUM_PERFORMANCE
}
