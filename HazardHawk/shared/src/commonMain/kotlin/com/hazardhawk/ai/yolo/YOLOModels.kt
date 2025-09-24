package com.hazardhawk.ai.yolo

import kotlinx.serialization.Serializable
import com.hazardhawk.domain.entities.HazardType
import com.hazardhawk.models.Severity
import com.hazardhawk.domain.entities.WorkType

/**
 * Core data models and type definitions for YOLO11 integration
 * These models provide the foundation for AI-powered object detection
 * and construction safety hazard identification.
 */

/**
 * Represents a bounding box for detected objects
 * Coordinates are normalized (0.0 to 1.0) relative to image dimensions
 */
@Serializable
data class YOLOBoundingBox(
    val x: Float, // Center X coordinate (normalized)
    val y: Float, // Center Y coordinate (normalized) 
    val width: Float, // Width (normalized)
    val height: Float, // Height (normalized)
    val confidence: Float, // Detection confidence (0.0 to 1.0)
    val classId: Int, // YOLO class identifier
    val className: String // Human-readable class name
) {
    /**
     * Convert to absolute coordinates for rendering
     */
    fun toAbsolute(imageWidth: Int, imageHeight: Int): AbsoluteBoundingBox {
        val centerX = x * imageWidth
        val centerY = y * imageHeight
        val w = width * imageWidth
        val h = height * imageHeight
        
        return AbsoluteBoundingBox(
            left = (centerX - w / 2).toInt(),
            top = (centerY - h / 2).toInt(),
            right = (centerX + w / 2).toInt(),
            bottom = (centerY + h / 2).toInt(),
            confidence = confidence,
            classId = classId,
            className = className
        )
    }
    
    /**
     * Check if bounding box is valid
     */
    fun isValid(): Boolean {
        return x in 0.0f..1.0f && y in 0.0f..1.0f && 
               width > 0.0f && height > 0.0f &&
               confidence in 0.0f..1.0f
    }
}

/**
 * Absolute bounding box coordinates for rendering
 */
@Serializable
data class AbsoluteBoundingBox(
    val left: Int,
    val top: Int, 
    val right: Int,
    val bottom: Int,
    val confidence: Float,
    val classId: Int,
    val className: String
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2
    val area: Int get() = width * height
}

/**
 * Construction-specific hazard types that can be detected by YOLO11
 */
@Serializable
enum class ConstructionHazardType {
    // Personal Protective Equipment
    MISSING_HARD_HAT,
    MISSING_SAFETY_VEST,
    MISSING_SAFETY_GLASSES,
    MISSING_GLOVES,
    MISSING_HEARING_PROTECTION,
    
    // Fall Protection
    UNGUARDED_EDGE,
    MISSING_GUARDRAILS,
    LADDER_UNSAFE_POSITION,
    SCAFFOLD_VIOLATION,
    WORKING_AT_HEIGHT_WITHOUT_PROTECTION,
    
    // Equipment Safety
    UNSAFE_EQUIPMENT_OPERATION,
    EQUIPMENT_WITHOUT_GUARDS,
    DAMAGED_TOOLS,
    IMPROPER_LIFTING_TECHNIQUE,
    
    // Housekeeping
    CLUTTERED_WORKSPACE,
    TRIP_HAZARDS,
    IMPROPER_STORAGE,
    DEBRIS_ACCUMULATION,
    
    // Electrical
    EXPOSED_WIRING,
    ELECTRICAL_HAZARD,
    IMPROPER_ELECTRICAL_CONNECTIONS,
    
    // Fire Safety
    FIRE_HAZARD,
    IMPROPER_STORAGE_FLAMMABLES,
    BLOCKED_FIRE_EXIT,
    
    // General
    UNSAFE_WORK_PRACTICE,
    UNKNOWN_HAZARD
}

/**
 * Maps construction hazards to existing HazardType enum
 */
fun ConstructionHazardType.toHazardType(): HazardType {
    return when (this) {
        ConstructionHazardType.MISSING_HARD_HAT,
        ConstructionHazardType.MISSING_SAFETY_VEST,
        ConstructionHazardType.MISSING_SAFETY_GLASSES,
        ConstructionHazardType.MISSING_GLOVES,
        ConstructionHazardType.MISSING_HEARING_PROTECTION -> HazardType.PPE_VIOLATION
        
        ConstructionHazardType.UNGUARDED_EDGE,
        ConstructionHazardType.MISSING_GUARDRAILS,
        ConstructionHazardType.LADDER_UNSAFE_POSITION,
        ConstructionHazardType.SCAFFOLD_VIOLATION,
        ConstructionHazardType.WORKING_AT_HEIGHT_WITHOUT_PROTECTION -> HazardType.FALL_PROTECTION
        
        ConstructionHazardType.UNSAFE_EQUIPMENT_OPERATION,
        ConstructionHazardType.EQUIPMENT_WITHOUT_GUARDS,
        ConstructionHazardType.DAMAGED_TOOLS,
        ConstructionHazardType.IMPROPER_LIFTING_TECHNIQUE -> HazardType.EQUIPMENT_SAFETY
        
        ConstructionHazardType.CLUTTERED_WORKSPACE,
        ConstructionHazardType.TRIP_HAZARDS,
        ConstructionHazardType.IMPROPER_STORAGE,
        ConstructionHazardType.DEBRIS_ACCUMULATION -> HazardType.HOUSEKEEPING
        
        ConstructionHazardType.EXPOSED_WIRING,
        ConstructionHazardType.ELECTRICAL_HAZARD,
        ConstructionHazardType.IMPROPER_ELECTRICAL_CONNECTIONS -> HazardType.ELECTRICAL
        
        ConstructionHazardType.FIRE_HAZARD,
        ConstructionHazardType.IMPROPER_STORAGE_FLAMMABLES,
        ConstructionHazardType.BLOCKED_FIRE_EXIT -> HazardType.FIRE
        
        ConstructionHazardType.UNSAFE_WORK_PRACTICE,
        ConstructionHazardType.UNKNOWN_HAZARD -> HazardType.OTHER
    }
}

/**
 * YOLO detection result containing all detected objects
 */
@Serializable
data class YOLODetectionResult(
    val detections: List<YOLOBoundingBox>,
    val imageWidth: Int,
    val imageHeight: Int,
    val processingTimeMs: Long,
    val modelVersion: String,
    val deviceInfo: String? = null,
    val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
) {
    /**
     * Filter detections by confidence threshold
     */
    fun filterByConfidence(threshold: Float): YOLODetectionResult {
        return copy(
            detections = detections.filter { it.confidence >= threshold }
        )
    }
    
    /**
     * Get construction hazards from detections
     */
    fun getConstructionHazards(): List<ConstructionHazardDetection> {
        return detections.mapNotNull { detection ->
            YOLOClassMapper.mapToConstructionHazard(detection.classId, detection.className)?.let { hazardType ->
                ConstructionHazardDetection(
                    hazardType = hazardType,
                    boundingBox = detection,
                    severity = YOLOClassMapper.getSeverity(hazardType),
                    oshaReference = YOLOClassMapper.getOSHAReference(hazardType)
                )
            }
        }
    }
}

/**
 * Construction hazard detected by YOLO
 */
@Serializable
data class ConstructionHazardDetection(
    val hazardType: ConstructionHazardType,
    val boundingBox: YOLOBoundingBox,
    val severity: Severity,
    val oshaReference: String? = null,
    val description: String = hazardType.name.replace("_", " ").lowercase()
        .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
)

/**
 * YOLO model configuration for device-adaptive selection
 */
@Serializable
data class YOLOModelConfiguration(
    val modelName: String,
    val modelPath: String,
    val inputSize: ImageSize,
    val numClasses: Int,
    val confidenceThreshold: Float = 0.5f,
    val iouThreshold: Float = 0.45f,
    val supportedDevices: List<DeviceType> = emptyList(),
    val estimatedMemoryMB: Int = 0,
    val estimatedInferenceTimeMs: Int = 0
) {
    companion object {
        /**
         * Get optimal model configuration for device capabilities
         */
        fun getOptimalConfiguration(
            deviceCapability: DeviceCapability
        ): YOLOModelConfiguration {
            return when (deviceCapability.performanceLevel) {
                PerformanceLevel.HIGH -> YOLOModelConfiguration(
                    modelName = "yolo11l",
                    modelPath = "models/yolo11l.onnx",
                    inputSize = ImageSize(640, 640),
                    numClasses = 80,
                    confidenceThreshold = 0.6f,
                    estimatedMemoryMB = 512,
                    estimatedInferenceTimeMs = 150
                )
                PerformanceLevel.MEDIUM -> YOLOModelConfiguration(
                    modelName = "yolo11m",
                    modelPath = "models/yolo11m.onnx",
                    inputSize = ImageSize(640, 640),
                    numClasses = 80,
                    confidenceThreshold = 0.55f,
                    estimatedMemoryMB = 256,
                    estimatedInferenceTimeMs = 100
                )
                PerformanceLevel.LOW -> YOLOModelConfiguration(
                    modelName = "yolo11n",
                    modelPath = "models/yolo11n.onnx",
                    inputSize = ImageSize(416, 416),
                    numClasses = 80,
                    confidenceThreshold = 0.5f,
                    estimatedMemoryMB = 128,
                    estimatedInferenceTimeMs = 50
                )
            }
        }
    }
}

/**
 * Image size specification
 */
@Serializable
data class ImageSize(
    val width: Int,
    val height: Int
) {
    val aspectRatio: Float get() = width.toFloat() / height.toFloat()
    val totalPixels: Int get() = width * height
}

/**
 * Device capability assessment
 */
@Serializable
data class DeviceCapability(
    val deviceType: DeviceType,
    val availableMemoryMB: Long,
    val cpuCores: Int,
    val hasGPU: Boolean = false,
    val performanceLevel: PerformanceLevel
)

/**
 * Device type enumeration
 */
@Serializable
enum class DeviceType {
    MOBILE_PHONE,
    TABLET,
    DESKTOP,
    TV,
    EMBEDDED
}

/**
 * Performance level for model selection
 */
@Serializable
enum class PerformanceLevel {
    LOW, MEDIUM, HIGH
}

/**
 * YOLO class mapping helper object
 */
object YOLOClassMapper {
    /**
     * Map YOLO class ID to construction hazard type
     */
    fun mapToConstructionHazard(classId: Int, className: String): ConstructionHazardType? {
        // This will be implemented in ConstructionHazardMapper.kt
        return when (className.lowercase()) {
            "person" -> if (classId == 0) ConstructionHazardType.MISSING_HARD_HAT else null
            else -> null
        }
    }
    
    /**
     * Get severity for construction hazard type
     */
    fun getSeverity(hazardType: ConstructionHazardType): Severity {
        return when (hazardType) {
            ConstructionHazardType.WORKING_AT_HEIGHT_WITHOUT_PROTECTION,
            ConstructionHazardType.ELECTRICAL_HAZARD,
            ConstructionHazardType.FIRE_HAZARD -> Severity.CRITICAL
            
            ConstructionHazardType.MISSING_HARD_HAT,
            ConstructionHazardType.UNGUARDED_EDGE,
            ConstructionHazardType.UNSAFE_EQUIPMENT_OPERATION -> Severity.HIGH
            
            ConstructionHazardType.MISSING_SAFETY_VEST,
            ConstructionHazardType.TRIP_HAZARDS,
            ConstructionHazardType.CLUTTERED_WORKSPACE -> Severity.MEDIUM
            
            else -> Severity.LOW
        }
    }
    
    /**
     * Get OSHA reference for construction hazard type
     */
    fun getOSHAReference(hazardType: ConstructionHazardType): String? {
        return when (hazardType) {
            ConstructionHazardType.MISSING_HARD_HAT -> "29 CFR 1926.95"
            ConstructionHazardType.MISSING_SAFETY_VEST -> "29 CFR 1926.95"
            ConstructionHazardType.WORKING_AT_HEIGHT_WITHOUT_PROTECTION -> "29 CFR 1926.501"
            ConstructionHazardType.UNGUARDED_EDGE -> "29 CFR 1926.501"
            ConstructionHazardType.ELECTRICAL_HAZARD -> "29 CFR 1926.95"
            ConstructionHazardType.FIRE_HAZARD -> "29 CFR 1926.150"
            else -> null
        }
    }
}
