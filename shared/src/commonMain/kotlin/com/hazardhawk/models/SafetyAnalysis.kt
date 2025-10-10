package com.hazardhawk.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import com.hazardhawk.core.models.WorkType
import com.hazardhawk.core.models.HazardType
import com.hazardhawk.core.models.Severity
import com.hazardhawk.core.models.AnalysisType
import com.hazardhawk.core.models.BoundingBox

@Serializable
data class AnalysisOptions(
    val workType: WorkType,
    val includeOSHACompliance: Boolean = true,
    val confidenceThreshold: Float = 0.7f
)

@Serializable
data class SafetyAnalysis(
    val id: String,
    val photoId: String,
    val hazards: List<Hazard> = emptyList(),
    val oshaCodes: List<OSHACode> = emptyList(),
    val severity: Severity = Severity.LOW,
    val recommendations: List<String> = emptyList(),
    val aiConfidence: Float = 0.0f,
    val analyzedAt: Instant,
    val analysisType: AnalysisType
) {
    companion object
}

@Serializable
data class Hazard(
    val id: String,
    val type: HazardType,
    val description: String,
    val severity: Severity,
    val confidence: Float,
    val boundingBox: BoundingBox? = null,
    val oshaReference: String? = null
)

@Serializable
data class OSHACode(
    val code: String,
    val title: String,
    val description: String,
    val url: String? = null,
    val applicability: Float = 1.0f
)

@Serializable
enum class HazardCategory {
    PPE,
    FALL_PROTECTION,
    ELECTRICAL,
    CHEMICAL,
    FIRE,
    MACHINERY,
    HOUSEKEEPING,
    GENERAL
}

// Extension to map HazardType to HazardCategory
fun HazardType.toCategory(): HazardCategory {
    return when (this) {
        HazardType.PPE_VIOLATION -> HazardCategory.PPE
        HazardType.FALL_PROTECTION -> HazardCategory.FALL_PROTECTION
        HazardType.ELECTRICAL, HazardType.ELECTRICAL_HAZARD, HazardType.ELECTRICAL_SAFETY -> HazardCategory.ELECTRICAL
        HazardType.CHEMICAL, HazardType.CHEMICAL_HAZARD -> HazardCategory.CHEMICAL
        HazardType.FIRE, HazardType.FIRE_HAZARD -> HazardCategory.FIRE
        HazardType.EQUIPMENT_SAFETY, HazardType.CRANE_LIFT, HazardType.CRANE_LIFTING -> HazardCategory.MACHINERY
        HazardType.HOUSEKEEPING -> HazardCategory.HOUSEKEEPING
        else -> HazardCategory.GENERAL
    }
}

// Extension function for SafetyAnalysis
suspend fun SafetyAnalysis.Companion.fromPhotoPath(
    photoPath: String,
    apiKey: String,
    analysisOptions: AnalysisOptions
): SafetyAnalysis {
    // Simulate some processing time
    kotlinx.coroutines.delay(100)
    
    return SafetyAnalysis(
        id = "analysis-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
        photoId = "photo-${photoPath.hashCode()}",
        hazards = listOf(
            Hazard(
                id = "hazard-1",
                type = HazardType.FALL_PROTECTION,
                description = "Potential fall hazard detected",
                severity = Severity.MEDIUM,
                confidence = 0.75f,
                oshaReference = "1926.95"
            )
        ),
        oshaCodes = listOf(
            OSHACode(
                code = "1926.95",
                title = "Personal Protective Equipment",
                description = "Fall protection equipment required"
            )
        ),
        severity = Severity.MEDIUM,
        aiConfidence = 0.75f,
        analyzedAt = kotlinx.datetime.Clock.System.now(),
        analysisType = AnalysisType.ON_DEVICE,
        recommendations = listOf(
            "Ensure proper fall protection equipment is used",
            "Conduct safety briefing on fall protection requirements"
        )
    )
}
