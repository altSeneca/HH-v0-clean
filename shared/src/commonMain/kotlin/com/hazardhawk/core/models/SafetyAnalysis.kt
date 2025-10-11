package com.hazardhawk.core.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

/**
 * Unified safety analysis model - single source of truth
 * Consolidates all previous SafetyAnalysis implementations
 * 
 * Migration Notes:
 * - Replaces com.hazardhawk.ai.models.SafetyAnalysis
 * - Maintains backward compatibility with legacy enums
 * - Supports all AI services (Gemma, Vertex AI, YOLO11)
 */
@Serializable
data class SafetyAnalysis(
    val id: String,
    val photoId: String,
    val timestamp: Instant,
    val analysisType: AnalysisType,
    val workType: WorkType,
    
    // Core analysis results
    val hazards: List<Hazard> = emptyList(),
    val ppeStatus: PPEStatus? = null,
    val oshaViolations: List<OSHAViolation> = emptyList(),
    val recommendations: List<String> = emptyList(),
    
    // Risk assessment
    val overallRiskLevel: RiskLevel,
    val severity: Severity, // Backward compatibility
    val aiConfidence: Float,
    val processingTimeMs: Long,
    
    // Metadata
    val metadata: AnalysisMetadata? = null
) {
    // Backward compatibility properties for AI services
    val oshaCodes: List<OSHACode>
        get() = oshaViolations.map { violation ->
            OSHACode(
                code = violation.code,
                title = violation.title,
                description = violation.description,
                applicability = 1.0f
            )
        }
    
    val analyzedAt: Instant get() = timestamp
    val confidence: Float get() = aiConfidence
    
    // Legacy compatibility for AI models package
    @Deprecated("Use overallRiskLevel instead", ReplaceWith("overallRiskLevel"))
    val riskLevel: RiskLevel get() = overallRiskLevel
    
    @Deprecated("Use processingTimeMs instead", ReplaceWith("processingTimeMs"))
    val processingTime: Long get() = processingTimeMs
}

/**
 * Unified hazard representation
 */
@Serializable
data class Hazard(
    val id: String,
    val type: HazardType,
    val severity: Severity,
    val description: String,
    val confidence: Float,
    val oshaCode: String? = null,
    val boundingBox: BoundingBox? = null,
    val recommendations: List<String> = emptyList(),
    val immediateAction: String? = null
) {
    // Backward compatibility
    val oshaReference: String? get() = oshaCode
}

/**
 * Unified bounding box implementation
 */
@Serializable
data class BoundingBox(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    // Alternative accessors for different conventions
    val x: Float get() = left
    val y: Float get() = top
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

/**
 * PPE detection status
 */
@Serializable
data class PPEStatus(
    val hardHat: PPEItem,
    val safetyVest: PPEItem,
    val safetyBoots: PPEItem,
    val safetyGlasses: PPEItem,
    val fallProtection: PPEItem,
    val respirator: PPEItem,
    val overallCompliance: Float
)

@Serializable
data class PPEItem(
    val status: PPEItemStatus,
    val confidence: Float,
    val boundingBox: BoundingBox? = null,
    val required: Boolean = false
)

/**
 * OSHA violation details
 */
@Serializable
data class OSHAViolation(
    val code: String,
    val title: String,
    val description: String,
    val severity: Severity,
    val fineRange: String? = null,
    val correctiveAction: String
)

/**
 * Backward compatibility OSHA code
 */
@Serializable
data class OSHACode(
    val code: String,
    val title: String,
    val description: String,
    val url: String? = null,
    val applicability: Float = 1.0f
)

/**
 * Analysis metadata
 */
@Serializable
data class AnalysisMetadata(
    val imageWidth: Int,
    val imageHeight: Int,
    val location: Location? = null,
    val weather: WeatherConditions? = null,
    val timeOfDay: String? = null,
    val cameraInfo: CameraInfo? = null
)

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val address: String? = null
)

@Serializable
data class WeatherConditions(
    val temperature: Float? = null,
    val humidity: Float? = null,
    val windSpeed: Float? = null,
    val conditions: String? = null
)

@Serializable
data class CameraInfo(
    val make: String? = null,
    val model: String? = null,
    val orientation: Int? = null,
    val iso: Int? = null,
    val fNumber: String? = null,
    val exposureTime: String? = null
)

// Consolidated enums
@Serializable
enum class AnalysisType {
    ON_DEVICE,
    LOCAL_GEMMA_MULTIMODAL,
    LOCAL_LITERT_VISION,        // LiteRT with hardware acceleration
    CLOUD_GEMINI,
    LOCAL_YOLO_FALLBACK,
    HYBRID_ANALYSIS,
    COMBINED,
    BATCH_OPERATION
}

@Serializable
enum class HazardType {
    FALL_PROTECTION,
    PPE_VIOLATION,
    ELECTRICAL,
    ELECTRICAL_HAZARD,
    ELECTRICAL_SAFETY,      // Added: Phase 1.1
    MECHANICAL_HAZARD,
    CHEMICAL,
    CHEMICAL_HAZARD,
    FIRE,
    FIRE_HAZARD,
    EQUIPMENT_SAFETY,
    CRANE_LIFT,
    CRANE_LIFTING,          // Added: Phase 1.1
    HOUSEKEEPING,
    STRUCK_BY_OBJECT,
    CAUGHT_IN_EQUIPMENT,
    ERGONOMIC_HAZARD,
    ENVIRONMENTAL_HAZARD,
    LOCKOUT_TAGOUT,
    CONFINED_SPACE,
    SCAFFOLDING_UNSAFE,
    EQUIPMENT_DEFECT,
    STEEL_WORK,             // Added: Phase 1.1
    OTHER,                  // Added: Phase 1.1
    UNKNOWN                 // Added: Phase 1.1
}

@Serializable
enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
enum class RiskLevel {
    MINIMAL,
    LOW,
    MODERATE,
    HIGH,
    SEVERE
}

@Serializable
enum class PPEItemStatus {
    PRESENT,
    MISSING,
    INCORRECT,
    UNKNOWN
}

/**
 * Analysis options for backward compatibility
 */
@Serializable
data class AnalysisOptions(
    val workType: WorkType,
    val includeOSHACompliance: Boolean = true,
    val confidenceThreshold: Float = 0.7f
)

/**
 * Missing enums for full compatibility
 */
@Serializable
enum class AnalysisCapability {
    MULTIMODAL_VISION,
    PPE_DETECTION,
    HAZARD_IDENTIFICATION,
    OSHA_COMPLIANCE,
    OFFLINE_ANALYSIS,
    REAL_TIME_PROCESSING,
    DOCUMENT_GENERATION,
    HARDWARE_ACCELERATION
}

/**
 * PPE (Personal Protective Equipment) types for LiteRT detection
 */
@Serializable
enum class PPEType {
    HARD_HAT,
    SAFETY_VEST,
    SAFETY_BOOTS,
    SAFETY_GLASSES,
    FALL_PROTECTION,
    RESPIRATOR,
    HEARING_PROTECTION,
    HAND_PROTECTION
}

/**
 * PPE detection result from AI analysis
 */
@Serializable
data class PPEDetection(
    val isPresent: Boolean,
    val isRequired: Boolean,
    val confidence: Float,
    val boundingBox: BoundingBox? = null
)

/**
 * Risk assessment result for construction safety analysis
 */
@Serializable
data class RiskAssessment(
    val overallLevel: RiskLevel,
    val likelihood: Float = 0.5f,
    val impact: Float = 0.5f,
    val controlMeasuresPresent: Boolean = false,
    val requiresImmediateAction: Boolean = false
)

/**
 * Migration utilities for model consolidation
 */
object SafetyAnalysisModelMigration {
    
    /**
     * Migrate from AI models package SafetyAnalysis
     */
    @Deprecated("Use unified model directly")
    fun fromAiModel(
        id: String,
        timestamp: Long,
        analysisType: String, // String for flexibility
        workType: String,
        hazards: List<Any>,
        ppeStatus: Any?,
        recommendations: List<String>,
        overallRiskLevel: String,
        confidence: Float,
        processingTimeMs: Long,
        oshaViolations: List<Any> = emptyList(),
        metadata: Any? = null
    ): SafetyAnalysis {
        return SafetyAnalysis(
            id = id,
            photoId = "photo-$id",
            timestamp = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp),
            analysisType = AnalysisType.valueOf(analysisType.uppercase().replace(" ", "_")),
            workType = WorkType.valueOf(workType.uppercase().replace(" ", "_")),
            hazards = emptyList(), // Simplified for migration
            overallRiskLevel = RiskLevel.valueOf(overallRiskLevel.uppercase()),
            severity = Severity.MEDIUM, // Default
            aiConfidence = confidence,
            processingTimeMs = processingTimeMs,
            recommendations = recommendations
        )
    }
    
    /**
     * Migrate from simple models package SafetyAnalysis
     */
    @Deprecated("Use unified model directly")
    fun fromSimpleModel(
        id: String,
        photoId: String,
        hazards: List<Any> = emptyList(),
        oshaCodes: List<Any> = emptyList(),
        severity: String,
        recommendations: List<String> = emptyList(),
        aiConfidence: Float = 0.0f,
        analyzedAt: kotlinx.datetime.Instant,
        analysisType: String
    ): SafetyAnalysis {
        return SafetyAnalysis(
            id = id,
            photoId = photoId,
            timestamp = analyzedAt,
            analysisType = when (analysisType.uppercase()) {
                "ON_DEVICE" -> AnalysisType.ON_DEVICE
                "CLOUD_GEMINI" -> AnalysisType.CLOUD_GEMINI
                "COMBINED" -> AnalysisType.COMBINED
                "BATCH_OPERATION" -> AnalysisType.BATCH_OPERATION
                else -> AnalysisType.ON_DEVICE
            },
            workType = WorkType.GENERAL_CONSTRUCTION,
            hazards = emptyList(), // Simplified for migration
            overallRiskLevel = RiskLevel.LOW,
            severity = Severity.valueOf(severity.uppercase()),
            aiConfidence = aiConfidence,
            processingTimeMs = 1000L, // Default
            recommendations = recommendations
        )
    }
}

/**
 * Alert types for production monitoring and safety systems
 */
@Serializable
enum class AlertType {
    // Safety alerts
    CRITICAL_HAZARD_DETECTED,
    OSHA_VIOLATION,
    PPE_VIOLATION,
    FALL_RISK,
    CONFINED_SPACE,
    
    // Performance alerts
    PERFORMANCE_DEGRADATION,
    HIGH_ERROR_RATE,
    SLOW_RESPONSE_TIME,
    
    // System alerts
    COMPONENT_FAILURE,
    RESOURCE_EXHAUSTION,
    SERVICE_UNAVAILABLE,
    DATABASE_ERROR,
    NETWORK_ERROR,
    
    // Incident management
    INCIDENT_ESCALATION,
    INCIDENT_CRITICAL,
    
    // General alerts
    WARNING,
    INFO,
    SYSTEM_ALERT
}
