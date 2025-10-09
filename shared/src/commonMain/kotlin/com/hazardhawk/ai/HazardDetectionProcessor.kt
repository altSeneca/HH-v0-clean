package com.hazardhawk.ai

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.models.Severity
import com.hazardhawk.domain.entities.ComplianceStatus

/**
 * Hazard Detection Result Processor with OSHA Compliance
 * Processes AI analysis results and ensures regulatory compliance
 */
class HazardDetectionProcessor {

    /**
     * Process AI analysis results and apply OSHA compliance rules
     */
    suspend fun processAnalysisResults(
        aiResults: PhotoAnalysisWithTags,
        workType: WorkType,
        photoId: String
    ): HazardDetectionResult {
        
        val timestamp = Clock.System.now().toEpochMilliseconds()
        
        // Extract hazards from AI results
        val detectedHazards = extractHazards(aiResults, workType)
        
        // Apply OSHA compliance analysis
        val complianceAnalysis = analyzeOSHACompliance(detectedHazards, workType)
        
        // Generate recommendations
        val recommendations = generateRecommendations(detectedHazards, complianceAnalysis)
        
        // Calculate overall risk level
        val overallRisk = calculateOverallRisk(detectedHazards)
        
        return HazardDetectionResult(
            id = "hazard-detection-$timestamp",
            photoId = photoId,
            workType = workType,
            timestamp = timestamp,
            detectedHazards = detectedHazards,
            complianceAnalysis = complianceAnalysis,
            recommendations = recommendations,
            overallRiskLevel = overallRisk,
            requiresImmediateAction = overallRisk in listOf(Severity.HIGH, Severity.CRITICAL),
            oshaDocumentationRequired = complianceAnalysis.hasViolations,
            processingTimeMs = aiResults.processingTimeMs
        )
    }

    private fun extractHazards(aiResults: PhotoAnalysisWithTags, workType: WorkType): List<DetectedHazard> {
        val hazards = mutableListOf<DetectedHazard>()
        
        // Parse recommended tags and categorize into hazards
        aiResults.recommendedTags.forEach { tag ->
            val hazard = when {
                tag.contains("fall") -> DetectedHazard(
                    type = HazardType.FALL_PROTECTION,
                    severity = Severity.HIGH,
                    description = "Fall protection hazard detected",
                    oshaStandard = "1926.501",
                    location = "Construction site elevation",
                    tags = listOf(tag)
                )
                tag.contains("ppe") -> DetectedHazard(
                    type = HazardType.PPE_VIOLATION,
                    severity = Severity.MEDIUM,
                    description = "Personal protective equipment violation",
                    oshaStandard = "1926.95",
                    location = "Worker area",
                    tags = listOf(tag)
                )
                tag.contains("electrical") -> DetectedHazard(
                    type = HazardType.ELECTRICAL,
                    severity = Severity.HIGH,
                    description = "Electrical safety hazard",
                    oshaStandard = "1926.416",
                    location = "Electrical work area",
                    tags = listOf(tag)
                )
                tag.contains("equipment") -> DetectedHazard(
                    type = HazardType.EQUIPMENT_SAFETY,
                    severity = Severity.MEDIUM,
                    description = "Equipment safety issue",
                    oshaStandard = "1926.300",
                    location = "Equipment operation area",
                    tags = listOf(tag)
                )
                else -> null
            }
            
            hazard?.let { hazards.add(it) }
        }
        
        // Add work-type specific default hazards if none detected
        if (hazards.isEmpty()) {
            hazards.addAll(getDefaultHazardsForWorkType(workType))
        }
        
        return hazards
    }

    private fun analyzeOSHACompliance(hazards: List<DetectedHazard>, workType: WorkType): OSHAComplianceAnalysis {
        val violations = mutableListOf<OSHAViolation>()
        val requiredActions = mutableListOf<String>()
        
        hazards.forEach { hazard ->
            when (hazard.type) {
                HazardType.FALL_PROTECTION -> {
                    if (hazard.severity in listOf(Severity.HIGH, Severity.CRITICAL)) {
                        violations.add(OSHAViolation(
                            standard = "1926.501(b)(1)",
                            description = "Unprotected sides and edges",
                            severity = hazard.severity,
                            fineRange = "$7,000 - $15,625",
                            correctionDeadline = "Immediate"
                        ))
                        requiredActions.add("Install guardrail systems or personal fall arrest systems")
                    }
                }
                HazardType.PPE_VIOLATION -> {
                    violations.add(OSHAViolation(
                        standard = "1926.95(a)",
                        description = "Personal protective equipment not provided or used",
                        severity = hazard.severity,
                        fineRange = "$1,000 - $7,000",
                        correctionDeadline = "Within 24 hours"
                    ))
                    requiredActions.add("Provide and ensure use of required PPE")
                }
                HazardType.ELECTRICAL -> {
                    violations.add(OSHAViolation(
                        standard = "1926.416(a)(1)",
                        description = "Electrical safety requirements not met",
                        severity = hazard.severity,
                        fineRange = "$7,000 - $15,625",
                        correctionDeadline = "Immediate"
                    ))
                    requiredActions.add("Implement electrical safety procedures and lockout/tagout")
                }
                else -> {
                    // Handle other hazard types
                }
            }
        }

        val overallComplianceStatus = if (violations.any { it.severity == Severity.CRITICAL }) {
            ComplianceStatus.NonCompliant
        } else if (violations.isNotEmpty()) {
            ComplianceStatus.ReviewRequired
        } else {
            ComplianceStatus.Compliant
        }

        return OSHAComplianceAnalysis(
            status = overallComplianceStatus,
            violations = violations,
            requiredActions = requiredActions,
            hasViolations = violations.isNotEmpty(),
            criticalViolations = violations.count { it.severity == Severity.CRITICAL },
            estimatedFineRange = calculateEstimatedFines(violations),
            documentationRequired = violations.isNotEmpty(),
            retentionPeriodYears = 5
        )
    }

    private fun generateRecommendations(
        hazards: List<DetectedHazard>,
        compliance: OSHAComplianceAnalysis
    ): List<SafetyRecommendation> {
        val recommendations = mutableListOf<SafetyRecommendation>()
        
        // High-priority recommendations for critical hazards
        hazards.filter { it.severity == Severity.CRITICAL }.forEach { hazard ->
            recommendations.add(SafetyRecommendation(
                priority = RecommendationPriority.CRITICAL,
                category = hazard.type.name,
                title = "IMMEDIATE ACTION REQUIRED",
                description = "Critical safety hazard requires immediate correction",
                oshaStandard = hazard.oshaStandard,
                estimatedCorrectionTime = "Immediate",
                estimatedCost = "Varies"
            ))
        }
        
        // Standard recommendations based on compliance analysis
        compliance.requiredActions.forEach { action ->
            recommendations.add(SafetyRecommendation(
                priority = RecommendationPriority.HIGH,
                category = "COMPLIANCE",
                title = "OSHA Compliance Required",
                description = action,
                oshaStandard = null,
                estimatedCorrectionTime = "24-48 hours",
                estimatedCost = "$500 - $2,000"
            ))
        }
        
        // Preventive recommendations
        recommendations.add(SafetyRecommendation(
            priority = RecommendationPriority.MEDIUM,
            category = "PREVENTION",
            title = "Regular Safety Training",
            description = "Conduct regular safety briefings and training sessions",
            oshaStandard = "1926.95(a)",
            estimatedCorrectionTime = "Ongoing",
            estimatedCost = "$200 - $500/month"
        ))
        
        return recommendations.sortedBy { it.priority.ordinal }
    }

    private fun calculateOverallRisk(hazards: List<DetectedHazard>): Severity {
        return when {
            hazards.any { it.severity == Severity.CRITICAL } -> Severity.CRITICAL
            hazards.any { it.severity == Severity.HIGH } -> Severity.HIGH
            hazards.any { it.severity == Severity.MEDIUM } -> Severity.MEDIUM
            else -> Severity.LOW
        }
    }

    private fun calculateEstimatedFines(violations: List<OSHAViolation>): String {
        val minFine = violations.sumOf { 1000L } // Simplified calculation
        val maxFine = violations.sumOf { 15625L }
        return "$${minFine} - $${maxFine}"
    }

    private fun getDefaultHazardsForWorkType(workType: WorkType): List<DetectedHazard> {
        return when (workType) {
            WorkType.GENERAL_CONSTRUCTION -> listOf(
                DetectedHazard(
                    type = HazardType.PPE_VIOLATION,
                    severity = Severity.MEDIUM,
                    description = "General PPE requirements",
                    oshaStandard = "1926.95",
                    location = "Work area",
                    tags = listOf("ppe-required")
                )
            )
            WorkType.FALL_PROTECTION -> listOf(
                DetectedHazard(
                    type = HazardType.FALL_PROTECTION,
                    severity = Severity.HIGH,
                    description = "Fall protection assessment required",
                    oshaStandard = "1926.501",
                    location = "Elevated work area",
                    tags = listOf("fall-protection")
                )
            )
            else -> emptyList()
        }
    }
}

/**
 * Hazard detection result data class
 */
@Serializable
data class HazardDetectionResult(
    val id: String,
    val photoId: String,
    val workType: WorkType,
    val timestamp: Long,
    val detectedHazards: List<DetectedHazard>,
    val complianceAnalysis: OSHAComplianceAnalysis,
    val recommendations: List<SafetyRecommendation>,
    val overallRiskLevel: Severity,
    val requiresImmediateAction: Boolean,
    val oshaDocumentationRequired: Boolean,
    val processingTimeMs: Long
)

/**
 * Detected hazard data class
 */
@Serializable
data class DetectedHazard(
    val type: HazardType,
    val severity: Severity,
    val description: String,
    val oshaStandard: String,
    val location: String,
    val tags: List<String> = emptyList()
)

/**
 * OSHA compliance analysis
 */
@Serializable
data class OSHAComplianceAnalysis(
    val status: ComplianceStatus,
    val violations: List<OSHAViolation>,
    val requiredActions: List<String>,
    val hasViolations: Boolean,
    val criticalViolations: Int,
    val estimatedFineRange: String,
    val documentationRequired: Boolean,
    val retentionPeriodYears: Int
)

/**
 * OSHA violation details
 */
@Serializable
data class OSHAViolation(
    val standard: String,
    val description: String,
    val severity: Severity,
    val fineRange: String,
    val correctionDeadline: String
)

/**
 * Safety recommendation
 */
@Serializable
data class SafetyRecommendation(
    val priority: RecommendationPriority,
    val category: String,
    val title: String,
    val description: String,
    val oshaStandard: String?,
    val estimatedCorrectionTime: String,
    val estimatedCost: String
)

/**
 * Hazard types enumeration
 */
@Serializable
enum class HazardType {
    FALL_PROTECTION,
    PPE_VIOLATION,
    ELECTRICAL,
    EQUIPMENT_SAFETY,
    CHEMICAL,
    FIRE,
    CONFINED_SPACE,
    HOT_WORK,
    CRANE_LIFTING,
    EXCAVATION,
    STRUCTURAL,
    ENVIRONMENTAL,
    BIOLOGICAL,
    ERGONOMIC,
    HOUSEKEEPING,
    OTHER
}

/**
 * Recommendation priority levels
 */
@Serializable
enum class RecommendationPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}