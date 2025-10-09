package com.hazardhawk.ai.models

import kotlinx.serialization.Serializable

/**
 * Complete safety analysis results from AI photo analysis.
 */
@Serializable
data class SafetyAnalysis(
    val id: String,
    val timestamp: Long,
    val analysisType: AnalysisType,
    val workType: WorkType,
    val hazards: List<Hazard>,
    val ppeStatus: PPEStatus,
    val recommendations: List<String>,
    val overallRiskLevel: RiskLevel,
    val confidence: Float,
    val processingTimeMs: Long,
    val oshaViolations: List<OSHAViolation> = emptyList(),
    val metadata: AnalysisMetadata? = null
)

/**
 * Individual hazard detected in the construction photo.
 */
@Serializable
data class Hazard(
    val id: String,
    val type: HazardType,
    val severity: Severity,
    val description: String,
    val oshaCode: String? = null,
    val boundingBox: BoundingBox? = null,
    val confidence: Float,
    val recommendations: List<String> = emptyList(),
    val immediateAction: String? = null
)

/**
 * PPE (Personal Protective Equipment) detection status.
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

/**
 * Individual PPE item status.
 */
@Serializable
data class PPEItem(
    val status: PPEItemStatus,
    val confidence: Float,
    val boundingBox: BoundingBox? = null,
    val required: Boolean = false
)

/**
 * OSHA violation details.
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
 * Bounding box for detected objects/hazards.
 */
@Serializable
data class BoundingBox(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

/**
 * Additional analysis metadata.
 */
@Serializable
data class AnalysisMetadata(
    val imageWidth: Int,
    val imageHeight: Int,
    val location: Location? = null,
    val weather: WeatherConditions? = null,
    val timeOfDay: String? = null,
    val processingTimeMs: Long? = null,      // Processing time in milliseconds
    val modelVersion: String? = null         // AI model version identifier
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

/**
 * Type of construction work being performed.
 */
@Serializable
enum class WorkType {
    GENERAL_CONSTRUCTION,
    ELECTRICAL,
    PLUMBING,
    ROOFING,
    SCAFFOLDING,
    EXCAVATION,
    CONCRETE,
    WELDING,
    PAINTING,
    DEMOLITION,
    FALL_PROTECTION,
    CRANE_OPERATIONS
}

/**
 * Type of AI analysis performed.
 */
@Serializable
enum class AnalysisType {
    LOCAL_LITERT_VISION,    // LiteRT with hardware acceleration
    LOCAL_GEMMA_MULTIMODAL, // Gemma 3N E2B multimodal
    CLOUD_GEMINI,           // Vertex AI cloud service
    LOCAL_YOLO_FALLBACK,    // YOLO11 basic detection
    HYBRID_ANALYSIS,        // Combined analysis
    CACHED_RESULT           // Previously cached analysis
}

/**
 * Hazard type categories.
 */
@Serializable
enum class HazardType {
    FALL_PROTECTION,
    PPE_VIOLATION,
    ELECTRICAL_HAZARD,
    MECHANICAL_HAZARD,
    CHEMICAL_HAZARD,
    FIRE_HAZARD,
    STRUCK_BY_OBJECT,
    CAUGHT_IN_EQUIPMENT,
    ERGONOMIC_HAZARD,
    ENVIRONMENTAL_HAZARD,
    HOUSEKEEPING,
    LOCKOUT_TAGOUT,
    CONFINED_SPACE,
    SCAFFOLDING_UNSAFE,
    EQUIPMENT_DEFECT
}

/**
 * Risk/severity levels.
 */
@Serializable
enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Overall risk assessment.
 */
@Serializable
enum class RiskLevel {
    MINIMAL,
    LOW,
    MODERATE,
    HIGH,
    SEVERE
}

/**
 * PPE item detection status.
 */
@Serializable
enum class PPEItemStatus {
    PRESENT,
    MISSING,
    INCORRECT,
    UNKNOWN
}

/**
 * AI analysis capabilities.
 */
enum class AnalysisCapability {
    MULTIMODAL_VISION,
    PPE_DETECTION,
    HAZARD_IDENTIFICATION,
    OSHA_COMPLIANCE,
    OFFLINE_ANALYSIS,
    REAL_TIME_PROCESSING,
    DOCUMENT_GENERATION,
    HARDWARE_ACCELERATION  // GPU/NPU/TPU acceleration support
}