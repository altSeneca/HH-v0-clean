package com.hazardhawk.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Configuration management for Gemma 3N E2B construction safety model.
 * 
 * Provides centralized configuration loading, validation, and management
 * for the multimodal AI model deployment across different platforms.
 */
@Serializable
data class GemmaModelConfiguration(
    val modelName: String,
    val modelVersion: String,
    val modelType: String,
    val description: String,
    val architecture: ModelArchitecture,
    val performance: PerformanceSpecifications,
    val capabilities: ModelCapabilities,
    val confidenceThresholds: ConfidenceThresholds,
    val constructionClasses: List<String>,
    val oshaRegulations: Map<String, List<String>>,
    val promptTemplates: Map<String, String>,
    val deviceCompatibility: DeviceCompatibility,
    val modelFiles: Map<String, ModelFileInfo>,
    val deployment: DeploymentInfo,
    val usageGuidelines: UsageGuidelines
) {
    companion object {
        /**
         * Default configuration for development and fallback scenarios.
         */
        fun default() = GemmaModelConfiguration(
            modelName = "gemma-3n-e2b-construction-safety",
            modelVersion = "1.0.0",
            modelType = "multimodal_vision_language",
            description = "Gemma 3N E2B optimized for construction safety analysis",
            architecture = ModelArchitecture.default(),
            performance = PerformanceSpecifications.default(),
            capabilities = ModelCapabilities.default(),
            confidenceThresholds = ConfidenceThresholds.default(),
            constructionClasses = listOf(
                "hard_hat_required", "safety_vest_required", "fall_protection_required",
                "electrical_hazard", "heavy_machinery_operation", "general_construction"
            ),
            oshaRegulations = mapOf(
                "subpart_e_ppe" to listOf("1926.95", "1926.96"),
                "subpart_m_fall_protection" to listOf("1926.501", "1926.502"),
                "subpart_k_electrical" to listOf("1926.416")
            ),
            promptTemplates = mapOf(
                "general_safety" to GemmaVisionAnalyzer.DEFAULT_CONSTRUCTION_SAFETY_PROMPT
            ),
            deviceCompatibility = DeviceCompatibility.default(),
            modelFiles = mapOf(
                "vision_encoder.onnx" to ModelFileInfo(150, "checksum_vision", true),
                "decoder_model_merged_q4.onnx" to ModelFileInfo(1898, "checksum_decoder", true)
            ),
            deployment = DeploymentInfo.default(),
            usageGuidelines = UsageGuidelines.default()
        )
        
        /**
         * Load configuration from JSON string.
         */
        fun fromJson(jsonString: String): GemmaModelConfiguration {
            return Json.decodeFromString(serializer(), jsonString)
        }
    }
    
    /**
     * Convert configuration to JSON string.
     */
    fun toJson(): String {
        return Json.encodeToString(serializer(), this)
    }
    
    /**
     * Validate configuration for deployment readiness.
     */
    fun validate(): ConfigurationValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate model files
        if (!modelFiles.containsKey("vision_encoder.onnx")) {
            errors.add("Missing vision encoder model file configuration")
        }
        
        if (!modelFiles.containsKey("decoder_model_merged_q4.onnx")) {
            errors.add("Missing decoder model file configuration")
        }
        
        // Validate performance specifications
        if (performance.memoryFootprintMb > performance.minRamRequirementMb) {
            warnings.add("Memory footprint exceeds minimum RAM requirement")
        }
        
        if (performance.targetInferenceTimeMs > 5000) {
            warnings.add("Target inference time is quite high (>5 seconds)")
        }
        
        // Validate thresholds
        if (confidenceThresholds.default < 0.3 || confidenceThresholds.default > 0.9) {
            warnings.add("Default confidence threshold may be too extreme")
        }
        
        return ConfigurationValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Get optimal configuration for specific device capabilities.
     */
    fun optimizeForDevice(deviceInfo: DeviceInfo): GemmaModelConfiguration {
        val optimizedPerformance = when {
            deviceInfo.availableMemoryMb < 6144 -> performance.copy(
                memoryFootprintMb = minOf(performance.memoryFootprintMb, deviceInfo.availableMemoryMb / 2),
                targetInferenceTimeMs = performance.targetInferenceTimeMs + 1000
            )
            deviceInfo.cpuCores < 6 -> performance.copy(
                targetInferenceTimeMs = performance.targetInferenceTimeMs + 500
            )
            else -> performance
        }
        
        val optimizedThresholds = when {
            deviceInfo.gpuSupport -> confidenceThresholds
            else -> confidenceThresholds.copy(
                default = minOf(confidenceThresholds.default + 0.05f, 0.8f)
            )
        }
        
        return this.copy(
            performance = optimizedPerformance,
            confidenceThresholds = optimizedThresholds
        )
    }
}

@Serializable
data class ModelArchitecture(
    val visionEncoder: VisionEncoderConfig,
    val textDecoder: TextDecoderConfig
) {
    companion object {
        fun default() = ModelArchitecture(
            visionEncoder = VisionEncoderConfig.default(),
            textDecoder = TextDecoderConfig.default()
        )
    }
}

@Serializable
data class VisionEncoderConfig(
    val inputSize: List<Int>,
    val modelFile: String,
    val preprocessing: PreprocessingConfig
) {
    companion object {
        fun default() = VisionEncoderConfig(
            inputSize = listOf(224, 224, 3),
            modelFile = "vision_encoder.onnx",
            preprocessing = PreprocessingConfig.default()
        )
    }
}

@Serializable
data class TextDecoderConfig(
    val modelFile: String,
    val maxSequenceLength: Int,
    val vocabularySize: Int,
    val quantization: String
) {
    companion object {
        fun default() = TextDecoderConfig(
            modelFile = "decoder_model_merged_q4.onnx",
            maxSequenceLength = 2048,
            vocabularySize = 32000,
            quantization = "int4"
        )
    }
}

@Serializable
data class PreprocessingConfig(
    val normalization: NormalizationConfig,
    val resizeMethod: String
) {
    companion object {
        fun default() = PreprocessingConfig(
            normalization = NormalizationConfig.default(),
            resizeMethod = "bilinear"
        )
    }
}

@Serializable
data class NormalizationConfig(
    val mean: List<Float>,
    val std: List<Float>
) {
    companion object {
        fun default() = NormalizationConfig(
            mean = listOf(0.485f, 0.456f, 0.406f),
            std = listOf(0.229f, 0.224f, 0.225f)
        )
    }
}

@Serializable
data class PerformanceSpecifications(
    val memoryFootprintMb: Int,
    val minRamRequirementMb: Int,
    val targetInferenceTimeMs: Int,
    val supportsGpuAcceleration: Boolean,
    val supportsNnapi: Boolean
) {
    companion object {
        fun default() = PerformanceSpecifications(
            memoryFootprintMb = 2048,
            minRamRequirementMb = 4096,
            targetInferenceTimeMs = 3000,
            supportsGpuAcceleration = true,
            supportsNnapi = true
        )
    }
}

@Serializable
data class ModelCapabilities(
    val supportsMultimodal: Boolean,
    val constructionSafetyOptimized: Boolean,
    val oshaComplianceAware: Boolean,
    val ppeDetection: Boolean,
    val hazardIdentification: Boolean,
    val riskAssessment: Boolean
) {
    companion object {
        fun default() = ModelCapabilities(
            supportsMultimodal = true,
            constructionSafetyOptimized = true,
            oshaComplianceAware = true,
            ppeDetection = true,
            hazardIdentification = true,
            riskAssessment = true
        )
    }
}

@Serializable
data class ConfidenceThresholds(
    val default: Float,
    val ppeDetection: Float,
    val hazardIdentification: Float,
    val oshaViolations: Float
) {
    companion object {
        fun default() = ConfidenceThresholds(
            default = 0.6f,
            ppeDetection = 0.7f,
            hazardIdentification = 0.65f,
            oshaViolations = 0.8f
        )
    }
}

@Serializable
data class DeviceCompatibility(
    val minAndroidVersion: Int,
    val recommendedAndroidVersion: Int,
    val cpuRequirements: String,
    val gpuAcceleration: Map<String, Boolean>
) {
    companion object {
        fun default() = DeviceCompatibility(
            minAndroidVersion = 26,
            recommendedAndroidVersion = 30,
            cpuRequirements = "ARM64 or x86_64",
            gpuAcceleration = mapOf(
                "qualcomm_adreno" to true,
                "arm_mali" to true,
                "imagination_powervr" to false
            )
        )
    }
}

@Serializable
data class ModelFileInfo(
    val sizeMb: Int,
    val checksum: String,
    val required: Boolean
)

@Serializable
data class DeploymentInfo(
    val createdAt: String,
    val optimizedFor: String,
    val trainingDataVersion: String,
    val modelFormat: String,
    val precision: String
) {
    companion object {
        fun default() = DeploymentInfo(
            createdAt = "2025-01-03T00:00:00Z",
            optimizedFor = "construction_safety",
            trainingDataVersion = "v2.1",
            modelFormat = "ONNX",
            precision = "mixed_int4_fp16"
        )
    }
}

@Serializable
data class UsageGuidelines(
    val recommendedImageResolution: String,
    val lightingConditions: String,
    val distanceFromSubjects: String,
    val cameraAngle: String,
    val imageQualityRequirements: String
) {
    companion object {
        fun default() = UsageGuidelines(
            recommendedImageResolution = "1080x1080 or higher",
            lightingConditions = "Good outdoor lighting preferred",
            distanceFromSubjects = "10-50 feet optimal",
            cameraAngle = "Eye level or slightly elevated",
            imageQualityRequirements = "Clear, non-blurry images with visible safety equipment"
        )
    }
}

/**
 * Device information for model optimization.
 */
data class DeviceInfo(
    val availableMemoryMb: Int,
    val cpuCores: Int,
    val gpuSupport: Boolean,
    val nnApiSupport: Boolean,
    val androidVersion: Int
)

/**
 * Result of configuration validation.
 */
data class ConfigurationValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Configuration loader with platform-specific implementations.
 */
expect class GemmaConfigurationLoader {
    /**
     * Load configuration from assets or resources.
     */
    suspend fun loadConfiguration(configPath: String): GemmaModelConfiguration
    
    /**
     * Get current device information for optimization.
     */
    fun getDeviceInfo(): DeviceInfo
    
    /**
     * Validate model files exist and are accessible.
     */
    suspend fun validateModelFiles(config: GemmaModelConfiguration): ValidationResult
}

/**
 * Model file validation result.
 */
data class ValidationResult(
    val isValid: Boolean,
    val missingFiles: List<String>,
    val invalidChecksums: List<String>
)
