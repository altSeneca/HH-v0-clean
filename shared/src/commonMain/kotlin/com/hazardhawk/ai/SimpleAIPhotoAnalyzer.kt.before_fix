package com.hazardhawk.ai

import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.core.models.*
import com.hazardhawk.core.models.HazardType as CoreHazardType
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Simple AI photo analyzer implementation for testing and demonstration.
 * Provides mock analysis results to verify the AI integration pipeline.
 */
@OptIn(ExperimentalUuidApi::class)
class SimpleAIPhotoAnalyzer : AIPhotoAnalyzer {

    private var isInitialized = false

    override val analyzerName: String = "Simple Mock Analyzer"
    override val priority: Int = 10  // Low priority - for testing only
    override val analysisCapabilities: Set<AnalysisCapability> = setOf(
        AnalysisCapability.HAZARD_IDENTIFICATION,
        AnalysisCapability.PPE_DETECTION
    )

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
            val startTime = Clock.System.now()

            // Generate mock analysis results based on work type
            val hazards = generateMockHazards(workType)
            val ppeStatus = generateMockPPEStatus()
            val oshaViolations = hazards.mapNotNull { hazard ->
                hazard.oshaCode?.let { code ->
                    OSHAViolation(
                        code = code,
                        title = getOSHATitle(code),
                        description = hazard.description,
                        severity = hazard.severity,
                        fineRange = getFineRange(hazard.severity),
                        correctiveAction = hazard.recommendations.firstOrNull() ?: "Address safety concern"
                    )
                }
            }

            val processingTime = (Clock.System.now() - startTime).inWholeMilliseconds

            val analysis = SafetyAnalysis(
                id = Uuid.random().toString(),
                photoId = "photo-${Uuid.random()}",
                timestamp = Clock.System.now(),
                analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
                workType = workType,
                hazards = hazards,
                ppeStatus = ppeStatus,
                oshaViolations = oshaViolations,
                recommendations = generateMockRecommendations(workType, hazards),
                overallRiskLevel = calculateRiskLevel(hazards),
                severity = calculateMaxSeverity(hazards),
                aiConfidence = 0.85f + Random.nextFloat() * 0.15f,
                processingTimeMs = processingTime,
                metadata = AnalysisMetadata(
                    imageWidth = 1920,
                    imageHeight = 1080
                )
            )

            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateMockHazards(workType: WorkType): List<Hazard> {
        val hazards = mutableListOf<Hazard>()

        // Generate 0-3 random hazards based on work type
        val hazardCount = Random.nextInt(0, 4)

        repeat(hazardCount) { index ->
            val hazardType = when (workType) {
                WorkType.ELECTRICAL ->
                    listOf(CoreHazardType.ELECTRICAL_HAZARD, CoreHazardType.PPE_VIOLATION).random()
                WorkType.ROOFING ->
                    listOf(CoreHazardType.FALL_PROTECTION, CoreHazardType.PPE_VIOLATION).random()
                WorkType.CRANE_OPERATIONS ->
                    listOf(CoreHazardType.MECHANICAL_HAZARD, CoreHazardType.PPE_VIOLATION).random()
                WorkType.EXCAVATION ->
                    listOf(CoreHazardType.MECHANICAL_HAZARD, CoreHazardType.CONFINED_SPACE).random()
                else ->
                    listOf(CoreHazardType.PPE_VIOLATION, CoreHazardType.HOUSEKEEPING).random()
            }

            hazards.add(
                Hazard(
                    id = Uuid.random().toString(),
                    type = hazardType,
                    description = getHazardDescription(hazardType),
                    severity = getHazardSeverity(hazardType),
                    confidence = 0.7f + Random.nextFloat() * 0.3f,
                    oshaCode = getOSHACode(hazardType),
                    recommendations = getHazardRecommendations(hazardType),
                    boundingBox = if (Random.nextBoolean()) {
                        BoundingBox(
                            left = Random.nextFloat() * 0.5f,
                            top = Random.nextFloat() * 0.5f,
                            width = 0.2f + Random.nextFloat() * 0.3f,
                            height = 0.2f + Random.nextFloat() * 0.3f
                        )
                    } else null
                )
            )
        }

        return hazards
    }

    private fun generateMockPPEStatus(): PPEStatus {
        return PPEStatus(
            hardHat = PPEItem(
                status = if (Random.nextBoolean()) PPEItemStatus.PRESENT else PPEItemStatus.MISSING,
                confidence = 0.8f + Random.nextFloat() * 0.2f,
                required = true
            ),
            safetyVest = PPEItem(
                status = if (Random.nextBoolean()) PPEItemStatus.PRESENT else PPEItemStatus.MISSING,
                confidence = 0.8f + Random.nextFloat() * 0.2f,
                required = true
            ),
            safetyBoots = PPEItem(
                status = if (Random.nextBoolean()) PPEItemStatus.PRESENT else PPEItemStatus.UNKNOWN,
                confidence = 0.6f + Random.nextFloat() * 0.3f,
                required = true
            ),
            safetyGlasses = PPEItem(
                status = if (Random.nextBoolean()) PPEItemStatus.PRESENT else PPEItemStatus.MISSING,
                confidence = 0.7f + Random.nextFloat() * 0.3f,
                required = false
            ),
            fallProtection = PPEItem(
                status = PPEItemStatus.UNKNOWN,
                confidence = 0.5f,
                required = false
            ),
            respirator = PPEItem(
                status = PPEItemStatus.UNKNOWN,
                confidence = 0.5f,
                required = false
            ),
            overallCompliance = Random.nextFloat() * 0.3f + 0.7f
        )
    }

    private fun getHazardDescription(type: CoreHazardType): String = when (type) {
        CoreHazardType.PPE_VIOLATION -> "Worker without required PPE"
        CoreHazardType.FALL_PROTECTION -> "Potential fall hazard - unprotected height"
        CoreHazardType.ELECTRICAL_HAZARD, CoreHazardType.ELECTRICAL -> "Electrical hazard - exposed wiring or equipment"
        CoreHazardType.MECHANICAL_HAZARD -> "Mechanical equipment hazard"
        CoreHazardType.CHEMICAL_HAZARD, CoreHazardType.CHEMICAL -> "Chemical exposure hazard"
        CoreHazardType.FIRE_HAZARD, CoreHazardType.FIRE -> "Fire hazard detected"
        CoreHazardType.STRUCK_BY_OBJECT -> "Struck-by hazard"
        CoreHazardType.CAUGHT_IN_EQUIPMENT -> "Caught-in/between hazard"
        CoreHazardType.ERGONOMIC_HAZARD -> "Ergonomic concern"
        CoreHazardType.ENVIRONMENTAL_HAZARD -> "Environmental hazard"
        CoreHazardType.HOUSEKEEPING -> "Housekeeping issue - trip/slip hazard"
        CoreHazardType.LOCKOUT_TAGOUT -> "LOTO procedure not followed"
        CoreHazardType.CONFINED_SPACE -> "Confined space entry hazard"
        CoreHazardType.SCAFFOLDING_UNSAFE -> "Unsafe scaffolding condition"
        CoreHazardType.EQUIPMENT_DEFECT -> "Equipment defect detected"
        CoreHazardType.EQUIPMENT_SAFETY -> "Equipment safety concern"
        CoreHazardType.CRANE_LIFT -> "Crane lifting hazard"
    }

    private fun getHazardSeverity(type: CoreHazardType): Severity = when (type) {
        CoreHazardType.FALL_PROTECTION, CoreHazardType.ELECTRICAL_HAZARD,
        CoreHazardType.ELECTRICAL, CoreHazardType.CONFINED_SPACE -> Severity.CRITICAL
        CoreHazardType.MECHANICAL_HAZARD, CoreHazardType.SCAFFOLDING_UNSAFE,
        CoreHazardType.LOCKOUT_TAGOUT, CoreHazardType.CRANE_LIFT -> Severity.HIGH
        CoreHazardType.PPE_VIOLATION, CoreHazardType.CHEMICAL_HAZARD,
        CoreHazardType.CHEMICAL, CoreHazardType.EQUIPMENT_DEFECT,
        CoreHazardType.EQUIPMENT_SAFETY -> Severity.MEDIUM
        else -> Severity.LOW
    }

    private fun getOSHACode(type: CoreHazardType): String? = when (type) {
        CoreHazardType.PPE_VIOLATION -> "1926.95"
        CoreHazardType.FALL_PROTECTION -> "1926.501"
        CoreHazardType.ELECTRICAL_HAZARD, CoreHazardType.ELECTRICAL -> "1926.416"
        CoreHazardType.SCAFFOLDING_UNSAFE -> "1926.451"
        CoreHazardType.LOCKOUT_TAGOUT -> "1910.147"
        CoreHazardType.CONFINED_SPACE -> "1926.1200"
        else -> null
    }

    private fun getOSHATitle(code: String): String = when (code) {
        "1926.95" -> "Personal Protective Equipment"
        "1926.501" -> "Fall Protection"
        "1926.416" -> "Electrical Safety"
        "1926.451" -> "Scaffolding Standards"
        "1926.651" -> "Excavation Safety"
        "1910.147" -> "Lockout/Tagout"
        "1926.1200" -> "Confined Spaces"
        else -> "OSHA Regulation"
    }

    private fun getFineRange(severity: Severity): String = when (severity) {
        Severity.CRITICAL -> "$10,000 - $136,532"
        Severity.HIGH -> "$5,000 - $15,625"
        Severity.MEDIUM -> "$1,000 - $15,625"
        Severity.LOW -> "$0 - $15,625"
    }

    private fun getHazardRecommendations(type: CoreHazardType): List<String> = when (type) {
        CoreHazardType.PPE_VIOLATION -> listOf(
            "Ensure all workers wear required PPE",
            "Conduct toolbox talk on PPE requirements"
        )
        CoreHazardType.FALL_PROTECTION -> listOf(
            "Install guardrail systems for heights over 6 feet",
            "Provide personal fall arrest systems",
            "Conduct fall protection training"
        )
        CoreHazardType.ELECTRICAL_HAZARD, CoreHazardType.ELECTRICAL -> listOf(
            "Implement lockout/tagout procedures",
            "Use ground fault circuit interrupters (GFCI)",
            "Maintain safe clearances from electrical equipment"
        )
        CoreHazardType.SCAFFOLDING_UNSAFE -> listOf(
            "Inspect scaffolding daily",
            "Ensure proper bracing and tie-offs",
            "Verify load capacity compliance"
        )
        else -> listOf("Address safety concern immediately", "Review safety protocols")
    }

    private fun calculateRiskLevel(hazards: List<Hazard>): RiskLevel {
        if (hazards.isEmpty()) return RiskLevel.MINIMAL

        val maxSeverity = hazards.maxOf { it.severity }
        return when (maxSeverity) {
            Severity.CRITICAL -> RiskLevel.SEVERE
            Severity.HIGH -> RiskLevel.HIGH
            Severity.MEDIUM -> RiskLevel.MODERATE
            Severity.LOW -> RiskLevel.LOW
        }
    }

    private fun calculateMaxSeverity(hazards: List<Hazard>): Severity {
        if (hazards.isEmpty()) return Severity.LOW
        return hazards.maxOf { it.severity }
    }

    private fun generateMockRecommendations(workType: WorkType, hazards: List<Hazard>): List<String> {
        val recommendations = mutableListOf<String>()

        if (hazards.isNotEmpty()) {
            recommendations.add("Address ${hazards.size} safety hazard(s) immediately")

            if (hazards.any { it.severity == Severity.CRITICAL }) {
                recommendations.add("CRITICAL: Stop work until critical hazards are resolved")
            }
        } else {
            recommendations.add("No immediate safety hazards detected")
            recommendations.add("Continue following safety protocols")
        }

        // Add work-type specific recommendations
        when (workType) {
            WorkType.ELECTRICAL -> {
                recommendations.add("Verify electrical lockout/tagout procedures")
                recommendations.add("Ensure proper electrical PPE is worn")
            }
            WorkType.ROOFING, WorkType.FALL_PROTECTION -> {
                recommendations.add("Inspect fall protection equipment daily")
                recommendations.add("Verify anchor points are rated for load")
            }
            WorkType.CRANE_OPERATIONS -> {
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
