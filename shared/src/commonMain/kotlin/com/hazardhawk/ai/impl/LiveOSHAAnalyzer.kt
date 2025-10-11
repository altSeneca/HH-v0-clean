package com.hazardhawk.ai.impl

import com.hazardhawk.ai.core.OSHAPhotoAnalyzer
import com.hazardhawk.ai.GeminiVisionAnalyzer
import com.hazardhawk.data.repositories.OSHAAnalysisRepository
import com.hazardhawk.core.models.*
import com.hazardhawk.core.models.WorkType
import com.hazardhawk.ai.PhotoAnalysisWithTags
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

/**
 * Live OSHA analyzer that uses Gemini Vision AI for real photo analysis
 * and persists results using OSHAAnalysisRepository.
 *
 * This analyzer:
 * 1. Uses Gemini Vision AI to analyze photos for OSHA compliance
 * 2. Saves analysis results to secure storage via OSHAAnalysisRepository
 * 3. Provides real-time OSHA violation detection
 * 4. Integrates with report generation system
 */
class LiveOSHAAnalyzer(
    private val geminiAnalyzer: GeminiVisionAnalyzer,
    private val oshaAnalysisRepository: OSHAAnalysisRepository
) : OSHAPhotoAnalyzer {

    private var configured = false

    override suspend fun analyzeForOSHACompliance(
        imageData: ByteArray,
        workType: WorkType
    ): Result<OSHAAnalysisResult> {
        return try {
            if (!configured) {
                return Result.failure(IllegalStateException("LiveOSHAAnalyzer not configured. Call configure() first."))
            }

            // Use Gemini Vision AI to analyze the photo for OSHA compliance
            val aiPrompt = buildOSHAAnalysisPrompt(workType)

            val geminiResult = geminiAnalyzer.analyzePhotoWithTags(
                data = imageData,
                width = 1920, // Default width
                height = 1080, // Default height
                workType = workType
            )

            // Parse AI response and convert to OSHA analysis
            val oshaResult = parseGeminiResponseToOSHAAnalysis(geminiResult, workType)
            Result.success(oshaResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun configure(apiKey: String?): Result<Unit> {
        return try {
            // Configure the underlying Gemini analyzer
            geminiAnalyzer.initialize()
            configured = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override val isAvailable: Boolean
        get() = configured && geminiAnalyzer.isServiceAvailable

    /**
     * Save OSHA analysis result to persistent storage
     */
    suspend fun saveAnalysis(photoId: String, analysis: OSHAAnalysisResult): Result<Unit> {
        return oshaAnalysisRepository.saveAnalysis(photoId, analysis)
    }

    /**
     * Get saved OSHA analysis for a photo
     */
    suspend fun getSavedAnalysis(photoId: String): Result<OSHAAnalysisResult?> {
        return oshaAnalysisRepository.getAnalysis(photoId)
    }

    /**
     * Delete OSHA analysis when photo is deleted
     */
    suspend fun deleteAnalysis(photoId: String): Result<Unit> {
        return oshaAnalysisRepository.deleteAnalysis(photoId)
    }

    /**
     * Check if analysis exists for a photo
     */
    suspend fun hasAnalysis(photoId: String): Boolean {
        return oshaAnalysisRepository.hasAnalysis(photoId)
    }

    private fun buildOSHAAnalysisPrompt(workType: WorkType): String {
        val workTypeContext = when (workType) {
            WorkType.ELECTRICAL -> "electrical work including wiring, panels, conduits, and electrical equipment"
            WorkType.CONCRETE -> "concrete work including pouring, finishing, and reinforcement"
            WorkType.ROOFING -> "roofing work including installation, repair, and maintenance"
            WorkType.EXCAVATION -> "excavation and earthwork including trenching and grading"
            WorkType.GENERAL_CONSTRUCTION -> "general construction work"
            WorkType.FALL_PROTECTION -> "fall protection and working at heights"
            WorkType.CRANE_OPERATIONS -> "crane operations and heavy lifting"
            else -> "construction work"
        }

        return """
            You are an OSHA compliance expert analyzing a construction site photo for safety violations and hazards.

            WORK TYPE CONTEXT: This photo shows $workTypeContext.

            Please analyze this image thoroughly and identify:

            1. SAFETY HAZARDS:
            - Personal Protective Equipment (PPE) violations
            - Fall protection issues
            - Electrical safety concerns
            - Equipment safety problems
            - Environmental hazards
            - Housekeeping violations

            2. OSHA COMPLIANCE:
            - Specific OSHA standard violations (provide exact CFR citations like "29 CFR 1926.95")
            - Severity level (Serious, Other-Than-Serious, De Minimis)
            - Required corrective actions
            - Potential penalties

            3. RECOMMENDATIONS:
            - Immediate actions needed
            - Training requirements
            - Equipment needed
            - Best practices to implement

            Focus specifically on hazards relevant to $workTypeContext while also noting general safety issues.

            Provide detailed, actionable findings with specific OSHA regulation references.
            Be thorough but practical in your assessment.

            If no significant hazards are visible, note this but provide preventive recommendations.
        """.trimIndent()
    }

    private fun parseGeminiResponseToOSHAAnalysis(
        aiAnalysis: PhotoAnalysisWithTags,
        workType: WorkType
    ): OSHAAnalysisResult {
        // Parse AI analysis and convert to OSHA format
        val hazards = mutableListOf<OSHAHazard>()
        val violations = mutableListOf<OSHADetailedViolation>()
        val recommendations = mutableListOf<OSHARecommendation>()

        // Convert AI detected hazards to OSHA hazards
        aiAnalysis.hazardDetections.forEachIndexed { index, hazard ->
            val oshaHazard = OSHAHazard(
                id = "live_hazard_${index + 1}",
                hazardType = mapHazardTypeToOSHA(hazard.hazardType.toString()),
                title = hazard.hazardType.toString(),
                description = hazard.description,
                severity = mapSeverityToOSHA(hazard.severity.toString()),
                oshaStandard = determineOSHAStandard(hazard.hazardType.toString(), workType),
                oshaCode = determineOSHACode(hazard.hazardType.toString(), workType),
                violationDetails = hazard.description,
                requiredAction = "Address ${hazard.hazardType.toString().lowercase()} safety concern",
                confidence = hazard.boundingBox.confidence,
                boundingBox = null // boundingBox would need conversion
            )
            hazards.add(oshaHazard)

            // Create corresponding violation
            val violation = OSHADetailedViolation(
                violationId = "live_violation_${index + 1}",
                oshaStandard = oshaHazard.oshaStandard,
                standardTitle = getStandardTitle(oshaHazard.oshaStandard),
                violationType = when (oshaHazard.severity) {
                    OSHASeverity.SERIOUS -> OSHAViolationType.SERIOUS
                    OSHASeverity.OTHER_THAN_SERIOUS -> OSHAViolationType.OTHER_THAN_SERIOUS
                    else -> OSHAViolationType.OTHER_THAN_SERIOUS
                },
                description = oshaHazard.violationDetails,
                potentialPenalty = calculatePenalty(oshaHazard.severity),
                correctiveAction = oshaHazard.requiredAction,
                timeframe = getRequiredTimeframe(oshaHazard.severity)
            )
            violations.add(violation)
        }

        // Generate OSHA-specific recommendations
        recommendations.addAll(generateOSHARecommendations(hazards, workType))

        // Calculate compliance score
        val complianceScore = calculateComplianceScore(hazards)
        val overallCompliance = determineComplianceStatus(complianceScore, hazards)

        return OSHAAnalysisResult(
            analysisId = "live_osha_analysis_${Clock.System.now().toEpochMilliseconds()}",
            overallCompliance = overallCompliance,
            safetyHazards = hazards,
            oshaViolations = violations,
            complianceScore = complianceScore,
            confidenceLevel = 0.8f, // Default confidence for live analysis
            detailedAnalysis = generateDetailedAnalysis(hazards, workType),
            recommendations = recommendations,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }

    private fun generateEnhancedFallbackAnalysis(
        workType: WorkType,
        errorMessage: String?
    ): OSHAAnalysisResult {
        // Enhanced fallback that still provides useful OSHA guidance
        val fallbackHazards = listOf(
            OSHAHazard(
                id = "fallback_general",
                hazardType = OSHAHazardType.GENERAL_SAFETY,
                title = "General Safety Assessment Required",
                description = "AI analysis unavailable. Manual OSHA compliance review recommended for this work area.",
                severity = OSHASeverity.OTHER_THAN_SERIOUS,
                oshaStandard = "29 CFR 1926.20",
                oshaCode = "1926.20(b)(2)",
                violationDetails = "Unable to complete automated safety analysis. Manual inspection required.",
                requiredAction = "Conduct thorough manual safety inspection using appropriate OSHA standards.",
                confidence = 0.5f
            )
        )

        val fallbackViolations = listOf(
            OSHADetailedViolation(
                violationId = "fallback_violation_001",
                oshaStandard = "29 CFR 1926.20",
                standardTitle = "General Safety and Health Provisions",
                violationType = OSHAViolationType.OTHER_THAN_SERIOUS,
                description = "Automated compliance analysis not available",
                potentialPenalty = "Manual assessment required",
                correctiveAction = "Perform manual OSHA compliance inspection",
                timeframe = "As soon as possible"
            )
        )

        val fallbackRecommendations = listOf(
            OSHARecommendation(
                id = "fallback_rec_001",
                priority = OSHAPriority.HIGH,
                category = OSHARecommendationCategory.SAFETY_PROCEDURES,
                title = "Manual Safety Inspection",
                description = "Conduct comprehensive manual safety inspection using relevant OSHA standards.",
                actionSteps = listOf(
                    "Identify specific OSHA standards applicable to this work type",
                    "Perform detailed safety inspection using OSHA checklists",
                    "Document all findings and corrective actions",
                    "Implement required safety measures before work proceeds"
                ),
                oshaReference = "29 CFR 1926.20",
                estimatedCost = "Inspection time only",
                timeToImplement = "Immediate"
            )
        )

        return OSHAAnalysisResult(
            analysisId = "fallback_osha_analysis_${Clock.System.now().toEpochMilliseconds()}",
            overallCompliance = ComplianceStatus.REQUIRES_REVIEW,
            safetyHazards = fallbackHazards,
            oshaViolations = fallbackViolations,
            complianceScore = 50.0f,
            confidenceLevel = 0.5f,
            detailedAnalysis = "Automated OSHA analysis unavailable. Error: ${errorMessage ?: "Unknown"}. Manual inspection required.",
            recommendations = fallbackRecommendations,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }

    private fun mapHazardTypeToOSHA(hazardType: String): OSHAHazardType {
        return when {
            hazardType.contains("PPE", ignoreCase = true) -> OSHAHazardType.PPE_VIOLATION
            hazardType.contains("fall", ignoreCase = true) -> OSHAHazardType.FALL_PROTECTION
            hazardType.contains("electrical", ignoreCase = true) -> OSHAHazardType.ELECTRICAL_SAFETY
            hazardType.contains("machinery", ignoreCase = true) -> OSHAHazardType.MACHINERY_SAFETY
            hazardType.contains("excavation", ignoreCase = true) -> OSHAHazardType.EXCAVATION_SAFETY
            hazardType.contains("scaffold", ignoreCase = true) -> OSHAHazardType.SCAFFOLDING
            hazardType.contains("crane", ignoreCase = true) -> OSHAHazardType.CRANE_OPERATIONS
            hazardType.contains("confined", ignoreCase = true) -> OSHAHazardType.CONFINED_SPACE
            hazardType.contains("chemical", ignoreCase = true) -> OSHAHazardType.CHEMICAL_EXPOSURE
            hazardType.contains("noise", ignoreCase = true) -> OSHAHazardType.NOISE_EXPOSURE
            else -> OSHAHazardType.GENERAL_SAFETY
        }
    }

    private fun mapSeverityToOSHA(severity: String): OSHASeverity {
        return when {
            severity.contains("high", ignoreCase = true) || severity.contains("critical", ignoreCase = true) -> OSHASeverity.SERIOUS
            severity.contains("medium", ignoreCase = true) -> OSHASeverity.OTHER_THAN_SERIOUS
            severity.contains("low", ignoreCase = true) -> OSHASeverity.DE_MINIMIS
            else -> OSHASeverity.OTHER_THAN_SERIOUS
        }
    }

    private fun determineOSHAStandard(hazardType: String, workType: WorkType): String {
        return when {
            hazardType.contains("PPE", ignoreCase = true) -> "29 CFR 1926.95"
            hazardType.contains("fall", ignoreCase = true) -> "29 CFR 1926.501"
            hazardType.contains("electrical", ignoreCase = true) -> "29 CFR 1926.137"
            hazardType.contains("scaffold", ignoreCase = true) -> "29 CFR 1926.451"
            hazardType.contains("excavation", ignoreCase = true) -> "29 CFR 1926.651"
            hazardType.contains("crane", ignoreCase = true) -> "29 CFR 1926.1400"
            else -> "29 CFR 1926.20"
        }
    }

    private fun determineOSHACode(hazardType: String, workType: WorkType): String {
        return when {
            hazardType.contains("PPE", ignoreCase = true) -> "1926.95(a)"
            hazardType.contains("fall", ignoreCase = true) -> "1926.501(b)(1)"
            hazardType.contains("electrical", ignoreCase = true) -> "1926.137(a)"
            hazardType.contains("scaffold", ignoreCase = true) -> "1926.451(b)"
            hazardType.contains("excavation", ignoreCase = true) -> "1926.651(c)"
            hazardType.contains("crane", ignoreCase = true) -> "1926.1400(a)"
            else -> "1926.20(b)(2)"
        }
    }

    private fun getStandardTitle(oshaStandard: String): String {
        return when {
            oshaStandard.contains("1926.95") -> "Personal Protective Equipment"
            oshaStandard.contains("1926.137") -> "Electrical Protective Equipment"
            oshaStandard.contains("1926.501") -> "Fall Protection"
            oshaStandard.contains("1926.451") -> "Scaffolding"
            oshaStandard.contains("1926.651") -> "Excavations"
            oshaStandard.contains("1926.1400") -> "Cranes and Derricks"
            oshaStandard.contains("1926.20") -> "General Safety and Health Provisions"
            else -> "Construction Safety Standards"
        }
    }

    private fun calculatePenalty(severity: OSHASeverity): String {
        return when (severity) {
            OSHASeverity.SERIOUS -> "Up to \$15,625 per violation"
            OSHASeverity.OTHER_THAN_SERIOUS -> "Up to \$15,625 per violation"
            OSHASeverity.DE_MINIMIS -> "No monetary penalty"
            OSHASeverity.WILLFUL -> "Up to \$156,259 per violation"
            OSHASeverity.REPEAT -> "Up to \$156,259 per violation"
            OSHASeverity.IMMINENT_DANGER -> "Work stoppage required"
        }
    }

    private fun getRequiredTimeframe(severity: OSHASeverity): String {
        return when (severity) {
            OSHASeverity.SERIOUS -> "Immediate correction required"
            OSHASeverity.IMMINENT_DANGER -> "Immediate work stoppage"
            OSHASeverity.WILLFUL -> "Immediate correction required"
            OSHASeverity.REPEAT -> "Immediate correction required"
            else -> "Correction required within 30 days"
        }
    }

    private fun calculateComplianceScore(hazards: List<OSHAHazard>): Float {
        if (hazards.isEmpty()) return 95.0f

        val seriousCount = hazards.count { it.severity == OSHASeverity.SERIOUS }
        val otherCount = hazards.count { it.severity == OSHASeverity.OTHER_THAN_SERIOUS }
        val deMinimisCount = hazards.count { it.severity == OSHASeverity.DE_MINIMIS }

        // Calculate score based on severity and count
        val baseScore = 100.0f
        val seriousDeduction = seriousCount * 25.0f
        val otherDeduction = otherCount * 10.0f
        val deMinimisDeduction = deMinimisCount * 5.0f

        return (baseScore - seriousDeduction - otherDeduction - deMinimisDeduction).coerceAtLeast(0.0f)
    }

    private fun determineComplianceStatus(score: Float, hazards: List<OSHAHazard>): ComplianceStatus {
        val hasSerious = hazards.any { it.severity == OSHASeverity.SERIOUS }
        val hasOther = hazards.any { it.severity == OSHASeverity.OTHER_THAN_SERIOUS }

        return when {
            hasSerious -> ComplianceStatus.SERIOUS_VIOLATIONS
            hasOther -> ComplianceStatus.MINOR_VIOLATIONS
            score >= 90 -> ComplianceStatus.COMPLIANT
            score >= 70 -> ComplianceStatus.MINOR_VIOLATIONS
            else -> ComplianceStatus.NON_COMPLIANT
        }
    }

    private fun generateOSHARecommendations(hazards: List<OSHAHazard>, workType: WorkType): List<OSHARecommendation> {
        val recommendations = mutableListOf<OSHARecommendation>()

        // Add specific recommendations based on hazards found
        if (hazards.any { it.hazardType == OSHAHazardType.PPE_VIOLATION }) {
            recommendations.add(
                OSHARecommendation(
                    id = "live_rec_ppe",
                    priority = OSHAPriority.HIGH,
                    category = OSHARecommendationCategory.PERSONAL_PROTECTIVE_EQUIPMENT,
                    title = "Improve PPE Compliance",
                    description = "Address personal protective equipment violations identified in analysis.",
                    actionSteps = listOf(
                        "Conduct PPE training for all workers",
                        "Ensure proper PPE is available and accessible",
                        "Implement daily PPE inspections",
                        "Document PPE compliance in safety reports"
                    ),
                    oshaReference = "29 CFR 1926.95",
                    estimatedCost = "\$200-500 per worker",
                    timeToImplement = "1 week"
                )
            )
        }

        if (hazards.any { it.hazardType == OSHAHazardType.FALL_PROTECTION }) {
            recommendations.add(
                OSHARecommendation(
                    id = "live_rec_fall",
                    priority = OSHAPriority.IMMEDIATE,
                    category = OSHARecommendationCategory.SAFETY_PROCEDURES,
                    title = "Implement Fall Protection",
                    description = "Address fall protection hazards identified in the work area.",
                    actionSteps = listOf(
                        "Install guardrail systems where applicable",
                        "Provide personal fall arrest equipment",
                        "Train workers on fall protection systems",
                        "Conduct daily fall protection inspections"
                    ),
                    oshaReference = "29 CFR 1926.501",
                    estimatedCost = "\$1,000-5,000",
                    timeToImplement = "Immediate"
                )
            )
        }

        // Always add general safety recommendation
        recommendations.add(
            OSHARecommendation(
                id = "live_rec_general",
                priority = OSHAPriority.MEDIUM,
                category = OSHARecommendationCategory.TRAINING_AND_EDUCATION,
                title = "Enhance Safety Training",
                description = "Improve overall safety awareness and compliance through targeted training.",
                actionSteps = listOf(
                    "Review OSHA standards specific to ${workType.name.lowercase()} work",
                    "Conduct regular safety meetings",
                    "Implement competent person training",
                    "Establish safety performance metrics"
                ),
                oshaReference = "29 CFR 1926.20",
                estimatedCost = "\$500-1,500",
                timeToImplement = "2 weeks"
            )
        )

        return recommendations
    }

    private fun generateDetailedAnalysis(
        hazards: List<OSHAHazard>,
        workType: WorkType
    ): String {
        val seriousCount = hazards.count { it.severity == OSHASeverity.SERIOUS }
        val otherCount = hazards.count { it.severity == OSHASeverity.OTHER_THAN_SERIOUS }

        return buildString {
            appendLine("## Live OSHA Compliance Analysis")
            appendLine()
            appendLine("**Analysis Type:** AI-Powered Real-Time Assessment")
            appendLine("**Work Type:** ${workType.name}")
            appendLine("**Analysis Timestamp:** ${Clock.System.now()}")
            appendLine("**Total Hazards Identified:** ${hazards.size}")
            appendLine("**Serious Violations:** $seriousCount")
            appendLine("**Other-Than-Serious Violations:** $otherCount")
            appendLine("**AI Confidence:** ${80}%")
            appendLine()

            if (seriousCount > 0) {
                appendLine("🚨 **IMMEDIATE ACTION REQUIRED**")
                appendLine("Serious OSHA violations detected that pose significant risk to worker safety.")
                appendLine("These violations must be corrected immediately before work continues.")
                appendLine()
            }

            if (hazards.isNotEmpty()) {
                appendLine("### Detailed Findings:")
                hazards.forEach { hazard ->
                    appendLine("#### ${hazard.title}")
                    appendLine("- **Severity:** ${hazard.severity.name}")
                    appendLine("- **OSHA Standard:** ${hazard.oshaCode}")
                    appendLine("- **Description:** ${hazard.description}")
                    appendLine("- **Required Action:** ${hazard.requiredAction}")
                    appendLine("- **AI Confidence:** ${(hazard.confidence * 100).toInt()}%")
                    appendLine()
                }
            } else {
                appendLine("### No Major Safety Violations Detected")
                appendLine("The AI analysis did not identify significant OSHA violations in this image.")
                appendLine("However, continue to follow all applicable safety procedures.")
                appendLine()
            }

            appendLine("### AI Analysis Summary:")
            appendLine("**Original AI Assessment:** Live OSHA analysis using Gemini Vision AI")
            appendLine()
            appendLine("### Compliance Notes:")
            appendLine("- This analysis was generated using AI technology")
            appendLine("- Results should be verified by qualified safety personnel")
            appendLine("- All OSHA standards and regulations remain in effect")
            appendLine("- Regular safety inspections are still required")
        }
    }
}