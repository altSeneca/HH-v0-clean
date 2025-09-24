package com.hazardhawk.ai

import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.ai.core.SafetyAnalysis
import com.hazardhawk.ai.core.SafetyHazard
import com.hazardhawk.ai.core.HazardType
import com.hazardhawk.ai.core.HazardSeverity
import com.hazardhawk.domain.entities.WorkType
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Simple AI photo analyzer implementation for testing and demonstration.
 * Provides mock analysis results to verify the AI integration pipeline.
 */
class SimpleAIPhotoAnalyzer : AIPhotoAnalyzer {

    private var isInitialized = false

    override val isAvailable: Boolean
        get() = isInitialized

    /**
     * Configure the analyzer
     */
    override suspend fun configure(apiKey: String?): Result<Unit> {
        return try {
            // Simulate initialization delay
            delay(500)
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Analyze photo using mock AI analysis for testing
     */
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType
    ): Result<SafetyAnalysis> {
        return try {
            if (!isInitialized) {
                configure()
            }

            // Simulate processing time
            delay(1000)

            // Generate mock analysis results based on work type
            val hazards = generateMockHazards(workType)

            val analysis = SafetyAnalysis(
                analysisId = "mock_${System.currentTimeMillis()}",
                hazards = hazards,
                complianceScore = Random.nextFloat() * 100f,
                confidence = 0.85f + Random.nextFloat() * 0.15f,
                recommendations = generateMockRecommendations(workType, hazards),
                timestamp = System.currentTimeMillis()
            )

            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateMockHazards(workType: WorkType): List<SafetyHazard> {
        val hazards = mutableListOf<SafetyHazard>()

        // Generate 0-3 random hazards based on work type
        val hazardCount = Random.nextInt(0, 4)

        repeat(hazardCount) { index ->
            val hazardType = when (workType) {
                WorkType.ELECTRICAL, WorkType.ELECTRICAL_SAFETY ->
                    listOf(HazardType.ELECTRICAL_HAZARD, HazardType.PERSON_NO_HARD_HAT).random()
                WorkType.ROOFING, WorkType.FALL_PROTECTION ->
                    listOf(HazardType.FALL_HAZARD, HazardType.UNPROTECTED_EDGE, HazardType.PERSON_NO_SAFETY_VEST).random()
                WorkType.CRANE_LIFTING ->
                    listOf(HazardType.CRANE, HazardType.HEAVY_MACHINERY).random()
                WorkType.EXCAVATION ->
                    listOf(HazardType.EXCAVATOR, HazardType.HEAVY_MACHINERY).random()
                else ->
                    listOf(HazardType.PERSON_NO_HARD_HAT, HazardType.PERSON_NO_SAFETY_VEST, HazardType.GENERAL_SAFETY_ISSUE).random()
            }

            hazards.add(
                SafetyHazard(
                    id = "hazard_${index}_${System.currentTimeMillis()}",
                    type = hazardType,
                    description = getHazardDescription(hazardType),
                    severity = getHazardSeverity(hazardType),
                    confidence = 0.7f + Random.nextFloat() * 0.3f,
                    oshaViolation = getOSHAViolation(hazardType),
                    recommendations = getHazardRecommendations(hazardType)
                )
            )
        }

        return hazards
    }

    private fun getHazardDescription(type: HazardType): String = when (type) {
        HazardType.PERSON_NO_HARD_HAT -> "Worker without required hard hat protection"
        HazardType.PERSON_NO_SAFETY_VEST -> "Worker not wearing high-visibility safety vest"
        HazardType.FALL_HAZARD -> "Potential fall hazard detected - unprotected height"
        HazardType.UNPROTECTED_EDGE -> "Unprotected edge without guardrail system"
        HazardType.ELECTRICAL_HAZARD -> "Electrical hazard - exposed wiring or equipment"
        HazardType.HEAVY_MACHINERY -> "Heavy machinery operating in area"
        HazardType.CRANE -> "Crane operation detected - verify safety protocols"
        HazardType.EXCAVATOR -> "Excavation equipment in operation"
        HazardType.TRUCK -> "Vehicle operation in construction zone"
        HazardType.GENERAL_SAFETY_ISSUE -> "General safety concern identified"
    }

    private fun getHazardSeverity(type: HazardType): HazardSeverity = when (type) {
        HazardType.FALL_HAZARD, HazardType.ELECTRICAL_HAZARD -> HazardSeverity.CRITICAL
        HazardType.UNPROTECTED_EDGE, HazardType.HEAVY_MACHINERY, HazardType.CRANE -> HazardSeverity.HIGH
        HazardType.PERSON_NO_HARD_HAT, HazardType.EXCAVATOR -> HazardSeverity.MEDIUM
        HazardType.PERSON_NO_SAFETY_VEST, HazardType.TRUCK -> HazardSeverity.LOW
        HazardType.GENERAL_SAFETY_ISSUE -> HazardSeverity.INFO
    }

    private fun getOSHAViolation(type: HazardType): String? = when (type) {
        HazardType.PERSON_NO_HARD_HAT -> "29 CFR 1926.95 - Head Protection"
        HazardType.PERSON_NO_SAFETY_VEST -> "29 CFR 1926.95 - High-Visibility Clothing"
        HazardType.FALL_HAZARD -> "29 CFR 1926.501 - Fall Protection"
        HazardType.UNPROTECTED_EDGE -> "29 CFR 1926.501(b)(1) - Unprotected Sides and Edges"
        HazardType.ELECTRICAL_HAZARD -> "29 CFR 1926.416 - Electrical Safety"
        HazardType.CRANE -> "29 CFR 1926.1400 - Crane Safety"
        HazardType.EXCAVATOR -> "29 CFR 1926.651 - Excavation Safety"
        else -> null
    }

    private fun getHazardRecommendations(type: HazardType): List<String> = when (type) {
        HazardType.PERSON_NO_HARD_HAT -> listOf(
            "Ensure all workers wear ANSI-approved hard hats",
            "Conduct toolbox talk on head protection"
        )
        HazardType.PERSON_NO_SAFETY_VEST -> listOf(
            "Provide high-visibility safety vests to all workers",
            "Ensure vests meet ANSI Class 2 or 3 requirements"
        )
        HazardType.FALL_HAZARD -> listOf(
            "Install guardrail systems for heights over 6 feet",
            "Provide personal fall arrest systems",
            "Conduct fall protection training"
        )
        HazardType.ELECTRICAL_HAZARD -> listOf(
            "Implement lockout/tagout procedures",
            "Use ground fault circuit interrupters (GFCI)",
            "Maintain safe clearances from electrical equipment"
        )
        else -> listOf("Address safety concern immediately", "Review safety protocols")
    }

    private fun generateMockRecommendations(workType: WorkType, hazards: List<SafetyHazard>): List<String> {
        val recommendations = mutableListOf<String>()

        if (hazards.isNotEmpty()) {
            recommendations.add("Address ${hazards.size} safety hazard(s) immediately")

            if (hazards.any { it.severity == HazardSeverity.CRITICAL }) {
                recommendations.add("CRITICAL: Stop work until critical hazards are resolved")
            }
        } else {
            recommendations.add("No immediate safety hazards detected")
            recommendations.add("Continue following safety protocols")
        }

        // Add work-type specific recommendations
        when (workType) {
            WorkType.ELECTRICAL, WorkType.ELECTRICAL_SAFETY -> {
                recommendations.add("Verify electrical lockout/tagout procedures")
                recommendations.add("Ensure proper electrical PPE is worn")
            }
            WorkType.FALL_PROTECTION -> {
                recommendations.add("Inspect fall protection equipment daily")
                recommendations.add("Verify anchor points are rated for load")
            }
            WorkType.CRANE_LIFTING -> {
                recommendations.add("Conduct pre-lift safety meeting")
                recommendations.add("Verify crane load chart compliance")
            }
            else -> {
                recommendations.add("Maintain situational awareness")
                recommendations.add("Follow established safety procedures")
            }
        }

        return recommendations
    }
}