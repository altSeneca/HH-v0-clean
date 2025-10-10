package com.hazardhawk.core.models

import kotlinx.serialization.Serializable

/**
 * OSHA-focused safety analysis result with detailed compliance information
 */
@Serializable
data class OSHAAnalysisResult(
    val analysisId: String,
    val overallCompliance: ComplianceStatus,
    val safetyHazards: List<OSHAHazard> = emptyList(),
    val oshaViolations: List<OSHADetailedViolation> = emptyList(),
    val complianceScore: Float = 0.0f, // 0-100
    val confidenceLevel: Float = 0.0f, // 0-1
    val detailedAnalysis: String = "",
    val recommendations: List<OSHARecommendation> = emptyList(),
    val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
    val aiDisclaimer: AIAnalysisDisclaimer = AIAnalysisDisclaimer.createDefault(),
    val oshaRegulationSource: OSHARegulationSource = OSHARegulationSource.createDefault()
)

/**
 * Individual OSHA hazard detected in photo with specific compliance details
 */
@Serializable
data class OSHAHazard(
    val id: String,
    val hazardType: OSHAHazardType,
    val title: String,
    val description: String,
    val severity: OSHASeverity,
    val oshaStandard: String, // e.g., "29 CFR 1926.95"
    val oshaCode: String, // e.g., "1926.95(a)"
    val violationDetails: String,
    val requiredAction: String,
    val confidence: Float, // 0-1
    val boundingBox: BoundingBox? = null
)

/**
 * Specific OSHA violation with citation information
 */
@Serializable
data class OSHADetailedViolation(
    val violationId: String,
    val oshaStandard: String, // Full citation like "29 CFR 1926.95(a)"
    val standardTitle: String, // e.g., "Personal Protective Equipment"
    val violationType: OSHAViolationType,
    val description: String,
    val potentialPenalty: String?, // e.g., "Up to $15,625 per violation"
    val correctiveAction: String,
    val timeframe: String? = null // e.g., "Immediate", "30 days"
)

/**
 * OSHA compliance recommendation with actionable steps
 */
@Serializable
data class OSHARecommendation(
    val id: String,
    val priority: OSHAPriority,
    val category: OSHARecommendationCategory,
    val title: String,
    val description: String,
    val actionSteps: List<String>,
    val oshaReference: String?, // Related OSHA standard
    val estimatedCost: String? = null,
    val timeToImplement: String? = null
)

/**
 * Types of OSHA hazards
 */
@Serializable
enum class OSHAHazardType {
    PPE_VIOLATION,
    FALL_PROTECTION,
    ELECTRICAL_SAFETY,
    MACHINERY_SAFETY,
    EXCAVATION_SAFETY,
    SCAFFOLDING,
    CRANE_OPERATIONS,
    CONFINED_SPACE,
    CHEMICAL_EXPOSURE,
    NOISE_EXPOSURE,
    GENERAL_SAFETY
}

/**
 * OSHA severity levels
 */
@Serializable
enum class OSHASeverity {
    IMMINENT_DANGER,    // Life-threatening
    SERIOUS,            // Could cause serious injury
    OTHER_THAN_SERIOUS, // Unlikely to cause serious injury
    DE_MINIMIS,         // No direct relationship to safety/health
    REPEAT,             // Previously cited violation
    WILLFUL             // Intentional disregard for safety
}

/**
 * Types of OSHA violations
 */
@Serializable
enum class OSHAViolationType {
    SERIOUS,
    OTHER_THAN_SERIOUS,
    DE_MINIMIS,
    WILLFUL,
    REPEAT,
    FAILURE_TO_ABATE
}

/**
 * Priority levels for recommendations
 */
@Serializable
enum class OSHAPriority {
    IMMEDIATE,    // Must be addressed immediately
    HIGH,         // Address within 24-48 hours
    MEDIUM,       // Address within 1 week
    LOW,          // Address within 30 days
    PREVENTIVE    // Ongoing prevention measure
}

/**
 * Categories of OSHA recommendations
 */
@Serializable
enum class OSHARecommendationCategory {
    PERSONAL_PROTECTIVE_EQUIPMENT,
    TRAINING_AND_EDUCATION,
    EQUIPMENT_MAINTENANCE,
    SAFETY_PROCEDURES,
    WORKPLACE_CONDITIONS,
    SIGNAGE_AND_WARNINGS,
    EMERGENCY_PROCEDURES,
    REGULATORY_COMPLIANCE
}

/**
 * Overall compliance status for OSHA analysis
 */
@Serializable
enum class ComplianceStatus {
    COMPLIANT,
    NON_COMPLIANT,
    MINOR_VIOLATIONS,
    SERIOUS_VIOLATIONS,
    REQUIRES_REVIEW
}

// BoundingBox is imported from SafetyAnalysis.kt

/**
 * AI analysis disclaimer for reports
 */
@Serializable
data class AIAnalysisDisclaimer(
    val title: String,
    val message: String,
    val accuracyNotice: String,
    val verificationNote: String
) {
    companion object {
        fun createDefault(): AIAnalysisDisclaimer {
            return AIAnalysisDisclaimer(
                title = "AI-Generated Analysis",
                message = "This analysis was generated using artificial intelligence technology and is provided for informational purposes only.",
                accuracyNotice = "AI can make mistakes. This analysis should not be relied upon as the sole basis for safety decisions or compliance determinations.",
                verificationNote = "All findings should be verified by qualified safety professionals and inspected according to applicable regulations and industry standards."
            )
        }
    }
}

/**
 * OSHA regulation source attribution
 */
@Serializable
data class OSHARegulationSource(
    val sourceTitle: String,
    val sourceUrl: String,
    val retrievalDate: String,
    val disclaimer: String
) {
    companion object {
        fun createDefault(): OSHARegulationSource {
            val currentDate = kotlinx.datetime.Clock.System.now().toString().substring(0, 10) // YYYY-MM-DD format
            return OSHARegulationSource(
                sourceTitle = "Electronic Code of Federal Regulations (eCFR) - Title 29 Labor",
                sourceUrl = "https://www.ecfr.gov/api/versioner/v1/structure/$currentDate/title-29.json",
                retrievalDate = currentDate,
                disclaimer = "OSHA regulations and codes referenced in this analysis were retrieved from the Electronic Code of Federal Regulations (eCFR). Regulations may have been updated since retrieval. Always verify current regulations at ecfr.gov."
            )
        }
    }
}