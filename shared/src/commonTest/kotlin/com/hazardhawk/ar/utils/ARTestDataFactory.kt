package com.hazardhawk.ar.utils

import com.hazardhawk.core.models.*
import com.hazardhawk.core.models.SafetyAnalysis
import kotlin.random.Random

/**
 * Factory for creating test data for AR safety monitoring tests.
 * Provides realistic construction scenarios and hazard data for comprehensive testing.
 */
object ARTestDataFactory {

    /**
     * Create a comprehensive construction scene with multiple hazards
     */
    fun createConstructionScene(
        hazardCount: Int = 5,
        workType: WorkType = WorkType.GENERAL_CONSTRUCTION,
        riskLevel: RiskLevel = RiskLevel.MEDIUM
    ): ConstructionScenario {
        val hazards = generateHazardsForScene(hazardCount, workType, riskLevel)
        val ppeStatus = generatePPEStatusForScene(workType, riskLevel)
        val analysis = createSafetyAnalysis(hazards, ppeStatus, workType)
        
        return ConstructionScenario(
            id = "construction_scene_${System.nanoTime()}",
            workType = workType,
            hazards = hazards,
            analysis = analysis,
            environmentalConditions = generateEnvironmentalConditions(),
            workerCount = Random.nextInt(3, 12),
            metadata = mapOf(
                "scene_complexity" to getSceneComplexity(hazardCount),
                "risk_level" to riskLevel.name
            )
        )
    }

    /**
     * Create fall protection test scenarios
     */
    fun createFallProtectionScenarios(): List<FallProtectionScenario> {
        return listOf(
            // Scaffolding work without fall protection
            FallProtectionScenario(
                id = "scaffolding_no_protection",
                height = 12f,
                hasProtection = false,
                workSurface = "scaffolding",
                hazard = createFallProtectionHazard(
                    height = 12f,
                    severity = Severity.CRITICAL,
                    description = "Worker on 12ft scaffolding without fall protection"
                ),
                oshaViolation = "1926.501(b)(1)"
            ),
            
            // Roof work with inadequate protection
            FallProtectionScenario(
                id = "roof_inadequate_protection",
                height = 15f,
                hasProtection = true,
                workSurface = "roof",
                hazard = createFallProtectionHazard(
                    height = 15f,
                    severity = Severity.HIGH,
                    description = "Inadequate fall protection on roof work"
                ),
                oshaViolation = "1926.501(b)(10)"
            ),
            
            // Ladder work at height
            FallProtectionScenario(
                id = "ladder_height_work",
                height = 8f,
                hasProtection = false,
                workSurface = "ladder",
                hazard = createFallProtectionHazard(
                    height = 8f,
                    severity = Severity.HIGH,
                    description = "Extended ladder work without fall protection"
                ),
                oshaViolation = "1926.1053(a)(3)(i)"
            )
        )
    }

    /**
     * Create electrical work test scenarios
     */
    fun createElectricalWorkScenarios(): List<ElectricalScenario> {
        return listOf(
            // Live wire exposure
            ElectricalScenario(
                id = "live_wire_exposure",
                voltage = "480V",
                hazardType = "exposed_wiring",
                isEnergized = true,
                hazard = createElectricalHazard(
                    voltage = "480V",
                    type = "exposed_wiring",
                    severity = Severity.CRITICAL
                ),
                requiredPPE = listOf("insulated_gloves", "arc_flash_suit", "safety_glasses")
            ),
            
            // Missing lockout/tagout
            ElectricalScenario(
                id = "missing_lockout",
                voltage = "277V",
                hazardType = "missing_lockout",
                isEnergized = true,
                hazard = createElectricalHazard(
                    voltage = "277V",
                    type = "missing_lockout",
                    severity = Severity.CRITICAL
                ),
                requiredPPE = listOf("insulated_gloves", "hard_hat", "safety_glasses")
            ),
            
            // Improper grounding
            ElectricalScenario(
                id = "improper_grounding",
                voltage = "120V",
                hazardType = "improper_grounding",
                isEnergized = true,
                hazard = createElectricalHazard(
                    voltage = "120V",
                    type = "improper_grounding",
                    severity = Severity.HIGH
                ),
                requiredPPE = listOf("insulated_gloves", "hard_hat")
            )
        )
    }

    /**
     * Create PPE violation test scenarios
     */
    fun createPPEViolationScenarios(): List<PPEScenario> {
        return listOf(
            // Missing hard hat
            PPEScenario(
                id = "missing_hard_hat",
                missingPPE = listOf("hard_hat"),
                presentPPE = listOf("safety_vest", "safety_boots"),
                workType = WorkType.GENERAL_CONSTRUCTION,
                hazard = createPPEViolationHazard("hard_hat", Severity.HIGH),
                complianceScore = 0.6f
            ),
            
            // Multiple PPE violations
            PPEScenario(
                id = "multiple_ppe_violations",
                missingPPE = listOf("hard_hat", "safety_glasses", "fall_protection"),
                presentPPE = listOf("safety_vest"),
                workType = WorkType.FALL_PROTECTION,
                hazard = createPPEViolationHazard("multiple", Severity.CRITICAL),
                complianceScore = 0.2f
            ),
            
            // Electrical work PPE missing
            PPEScenario(
                id = "electrical_ppe_missing",
                missingPPE = listOf("insulated_gloves", "arc_flash_suit"),
                presentPPE = listOf("hard_hat", "safety_glasses"),
                workType = WorkType.ELECTRICAL_WORK,
                hazard = createPPEViolationHazard("electrical_ppe", Severity.CRITICAL),
                complianceScore = 0.3f
            )
        )
    }

    /**
     * Create performance test data sets
     */
    fun createPerformanceTestData(): PerformanceTestData {
        return PerformanceTestData(
            lightLoadScenario = createConstructionScene(hazardCount = 2, riskLevel = RiskLevel.LOW),
            mediumLoadScenario = createConstructionScene(hazardCount = 8, riskLevel = RiskLevel.MEDIUM),
            heavyLoadScenario = createConstructionScene(hazardCount = 25, riskLevel = RiskLevel.HIGH),
            stressTestScenario = createConstructionScene(hazardCount = 50, riskLevel = RiskLevel.HIGH),
            frameSequence = generateFrameSequence(30), // 1 second at 30 FPS
            targetMetrics = PerformanceTargets(
                maxFrameTime = 16.67f, // 60 FPS
                maxAnalysisTime = 2000L, // 2 seconds
                maxMemoryUsage = 500L * 1024 * 1024 // 500MB
            )
        )
    }

    /**
     * Create mock camera frame data
     */
    fun createMockCameraFrames(
        count: Int = 10,
        resolution: CameraResolution = CameraResolution.HD_1080,
        frameRate: Float = 30f
    ): List<CameraFrame> {
        val frameDuration = (1000f / frameRate).toLong()
        
        return (0 until count).map { index ->
            CameraFrame(
                timestamp = System.nanoTime() + index * frameDuration * 1_000_000L,
                data = generateMockImageData(resolution),
                width = resolution.width,
                height = resolution.height,
                format = ImageFormat.YUV_420_888,
                metadata = CameraFrameMetadata(
                    exposure = Random.nextFloat() * 100f,
                    iso = Random.nextInt(100, 1600),
                    focusDistance = Random.nextFloat() * 10f,
                    lightingLevel = Random.nextFloat()
                )
            )
        }
    }

    /**
     * Create realistic bounding boxes for hazards
     */
    fun createRealisticBoundingBoxes(hazardType: HazardType, count: Int = 1): List<BoundingBox> {
        return (0 until count).map {
            when (hazardType) {
                HazardType.FALL_PROTECTION -> BoundingBox(
                    left = Random.nextFloat() * 0.6f,
                    top = Random.nextFloat() * 0.4f,
                    width = 0.15f + Random.nextFloat() * 0.25f,
                    height = 0.3f + Random.nextFloat() * 0.4f
                )
                HazardType.PPE_VIOLATION -> BoundingBox(
                    left = 0.3f + Random.nextFloat() * 0.4f,
                    top = Random.nextFloat() * 0.3f,
                    width = 0.1f + Random.nextFloat() * 0.2f,
                    height = 0.15f + Random.nextFloat() * 0.25f
                )
                HazardType.ELECTRICAL_HAZARD -> BoundingBox(
                    left = Random.nextFloat() * 0.7f,
                    top = 0.3f + Random.nextFloat() * 0.4f,
                    width = 0.1f + Random.nextFloat() * 0.2f,
                    height = 0.08f + Random.nextFloat() * 0.15f
                )
                else -> BoundingBox(
                    left = Random.nextFloat() * 0.8f,
                    top = Random.nextFloat() * 0.8f,
                    width = 0.1f + Random.nextFloat() * 0.2f,
                    height = 0.1f + Random.nextFloat() * 0.2f
                )
            }
        }
    }

    /**
     * Create OSHA compliance test scenarios
     */
    fun createOSHAComplianceScenarios(): List<OSHAComplianceScenario> {
        return listOf(
            OSHAComplianceScenario(
                id = "compliant_construction_site",
                workType = WorkType.GENERAL_CONSTRUCTION,
                isCompliant = true,
                violations = emptyList(),
                complianceScore = 1.0,
                requiredActions = emptyList()
            ),
            
            OSHAComplianceScenario(
                id = "fall_protection_violations",
                workType = WorkType.FALL_PROTECTION,
                isCompliant = false,
                violations = listOf(
                    OSHAViolation(
                        code = "1926.501(b)(1)",
                        description = "Fall protection required at 6 feet or higher",
                        severity = ViolationSeverity.CRITICAL,
                        category = ViolationCategory.FALL_PROTECTION
                    )
                ),
                complianceScore = 0.3,
                requiredActions = listOf("Install fall protection system")
            ),
            
            OSHAComplianceScenario(
                id = "electrical_safety_violations",
                workType = WorkType.ELECTRICAL_WORK,
                isCompliant = false,
                violations = listOf(
                    OSHAViolation(
                        code = "1926.416(a)(1)",
                        description = "Live parts must be protected",
                        severity = ViolationSeverity.CRITICAL,
                        category = ViolationCategory.ELECTRICAL
                    ),
                    OSHAViolation(
                        code = "1926.417(a)",
                        description = "Lockout/tagout required",
                        severity = ViolationSeverity.CRITICAL,
                        category = ViolationCategory.ELECTRICAL
                    )
                ),
                complianceScore = 0.1,
                requiredActions = listOf("Implement lockout/tagout", "Cover live parts")
            )
        )
    }

    // Private helper methods
    private fun generateHazardsForScene(
        count: Int,
        workType: WorkType,
        riskLevel: RiskLevel
    ): List<Hazard> {
        val hazardTypes = getHazardTypesForWorkType(workType)
        val severityDistribution = getSeverityDistribution(riskLevel)
        
        return (0 until count).map { index ->
            val hazardType = hazardTypes[index % hazardTypes.size]
            val severity = selectSeverityFromDistribution(severityDistribution)
            
            when (hazardType) {
                HazardType.FALL_PROTECTION -> createFallProtectionHazard(
                    height = 6f + Random.nextFloat() * 20f,
                    severity = severity
                )
                HazardType.ELECTRICAL_HAZARD -> createElectricalHazard(
                    voltage = listOf("120V", "277V", "480V").random(),
                    severity = severity
                )
                HazardType.PPE_VIOLATION -> createPPEViolationHazard(
                    missingItem = listOf("hard_hat", "safety_glasses", "gloves").random(),
                    severity = severity
                )
                else -> createGenericHazard(hazardType, severity)
            }
        }
    }

    private fun generatePPEStatusForScene(workType: WorkType, riskLevel: RiskLevel): PPEStatus {
        val complianceLevel = when (riskLevel) {
            RiskLevel.LOW -> 0.9f
            RiskLevel.MEDIUM -> 0.6f
            RiskLevel.HIGH -> 0.3f
            else -> 1.0f
        }
        
        return PPEStatus(
            hardHat = PPEItem(
                if (Random.nextFloat() < complianceLevel) PPEItemStatus.PRESENT else PPEItemStatus.MISSING,
                Random.nextFloat() * 0.3f + 0.7f,
                null,
                true
            ),
            safetyVest = PPEItem(
                if (Random.nextFloat() < complianceLevel) PPEItemStatus.PRESENT else PPEItemStatus.MISSING,
                Random.nextFloat() * 0.2f + 0.8f,
                null,
                true
            ),
            safetyBoots = PPEItem(
                PPEItemStatus.PRESENT,
                Random.nextFloat() * 0.1f + 0.9f,
                null,
                true
            ),
            safetyGlasses = PPEItem(
                if (workType == WorkType.ELECTRICAL_WORK && Random.nextFloat() < complianceLevel) 
                    PPEItemStatus.PRESENT else PPEItemStatus.UNKNOWN,
                Random.nextFloat() * 0.4f + 0.6f,
                null,
                workType == WorkType.ELECTRICAL_WORK
            ),
            fallProtection = PPEItem(
                if (workType == WorkType.FALL_PROTECTION && Random.nextFloat() < complianceLevel)
                    PPEItemStatus.PRESENT else PPEItemStatus.MISSING,
                if (workType == WorkType.FALL_PROTECTION) Random.nextFloat() * 0.3f + 0.7f else 0f,
                null,
                workType == WorkType.FALL_PROTECTION
            ),
            respirator = PPEItem(
                PPEItemStatus.UNKNOWN,
                Random.nextFloat() * 0.5f,
                null,
                false
            ),
            overallCompliance = complianceLevel
        )
    }

    private fun createFallProtectionHazard(
        height: Float = 8f,
        severity: Severity = Severity.HIGH,
        description: String = "Worker at ${height}ft height without fall protection"
    ): Hazard {
        return Hazard(
            id = "fall_protection_${System.nanoTime()}",
            type = HazardType.FALL_PROTECTION,
            severity = severity,
            description = description,
            oshaCode = "1926.501(b)(1)",
            boundingBox = createRealisticBoundingBoxes(HazardType.FALL_PROTECTION, 1).first(),
            confidence = 0.8f + Random.nextFloat() * 0.2f,
            recommendations = listOf(
                "Install fall protection system",
                "Use safety harness and lanyard",
                "Implement fall protection plan"
            ),
            immediateAction = if (severity == Severity.CRITICAL) "STOP WORK - Install fall protection" else null,
            metadata = mapOf("height" to height, "work_surface" to "elevation")
        )
    }

    private fun createElectricalHazard(
        voltage: String = "120V",
        type: String = "exposed_wiring",
        severity: Severity = Severity.HIGH
    ): Hazard {
        val oshaCode = when (type) {
            "missing_lockout" -> "1926.417(a)"
            "exposed_wiring" -> "1926.416(a)(1)"
            "improper_grounding" -> "1926.404(f)(6)"
            else -> "1926.405(a)"
        }
        
        return Hazard(
            id = "electrical_${type}_${System.nanoTime()}",
            type = HazardType.ELECTRICAL_HAZARD,
            severity = severity,
            description = "Electrical hazard: $type at $voltage",
            oshaCode = oshaCode,
            boundingBox = createRealisticBoundingBoxes(HazardType.ELECTRICAL_HAZARD, 1).first(),
            confidence = 0.75f + Random.nextFloat() * 0.25f,
            recommendations = when (type) {
                "missing_lockout" -> listOf("Implement lockout/tagout procedure", "Train workers on LOTO")
                "exposed_wiring" -> listOf("Cover exposed conductors", "Install proper electrical panels")
                else -> listOf("Address electrical safety concern", "Consult qualified electrician")
            },
            immediateAction = if (severity == Severity.CRITICAL) "STOP WORK - Address electrical hazard" else null,
            metadata = mapOf("voltage" to voltage, "hazard_subtype" to type)
        )
    }

    private fun createPPEViolationHazard(
        missingItem: String,
        severity: Severity = Severity.MEDIUM
    ): Hazard {
        return Hazard(
            id = "ppe_violation_${missingItem}_${System.nanoTime()}",
            type = HazardType.PPE_VIOLATION,
            severity = severity,
            description = "Missing required PPE: $missingItem",
            oshaCode = "1926.95(a)",
            boundingBox = createRealisticBoundingBoxes(HazardType.PPE_VIOLATION, 1).first(),
            confidence = 0.85f + Random.nextFloat() * 0.15f,
            recommendations = listOf(
                "Don required PPE: $missingItem",
                "Ensure PPE compliance training",
                "Implement PPE inspection checklist"
            ),
            metadata = mapOf("missing_ppe" to missingItem)
        )
    }

    private fun createGenericHazard(hazardType: HazardType, severity: Severity): Hazard {
        return Hazard(
            id = "${hazardType.name.lowercase()}_${System.nanoTime()}",
            type = hazardType,
            severity = severity,
            description = "Safety concern: ${hazardType.name.lowercase().replace('_', ' ')}",
            oshaCode = getOSHACodeForHazardType(hazardType),
            boundingBox = createRealisticBoundingBoxes(hazardType, 1).first(),
            confidence = 0.7f + Random.nextFloat() * 0.3f,
            recommendations = listOf("Address ${hazardType.name.lowercase()} safety concern")
        )
    }

    private fun createSafetyAnalysis(
        hazards: List<Hazard>,
        ppeStatus: PPEStatus,
        workType: WorkType
    ): SafetyAnalysis {
        val overallRisk = when {
            hazards.any { it.severity == Severity.CRITICAL } -> RiskLevel.HIGH
            hazards.any { it.severity == Severity.HIGH } -> RiskLevel.MEDIUM
            hazards.isNotEmpty() -> RiskLevel.LOW
            else -> RiskLevel.NONE
        }
        
        return SafetyAnalysis(
            id = "analysis_${System.nanoTime()}",
            timestamp = System.currentTimeMillis(),
            analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
            workType = workType,
            hazards = hazards,
            ppeStatus = ppeStatus,
            recommendations = generateRecommendations(hazards),
            overallRiskLevel = overallRisk,
            confidence = if (hazards.isNotEmpty()) hazards.map { it.confidence }.average().toFloat() else 1.0f,
            processingTimeMs = Random.nextLong(800, 2500),
            oshaViolations = generateOSHAViolations(hazards),
            metadata = mapOf(
                "scene_analysis" to true,
                "hazard_count" to hazards.size,
                "risk_level" to overallRisk.name
            )
        )
    }

    private fun generateEnvironmentalConditions(): EnvironmentalConditions {
        return EnvironmentalConditions(
            lightingLevel = Random.nextFloat(),
            weatherCondition = listOf("sunny", "cloudy", "overcast", "light_rain").random(),
            temperature = Random.nextInt(-10, 40),
            windSpeed = Random.nextFloat() * 20f,
            visibility = Random.nextFloat() * 0.3f + 0.7f
        )
    }

    private fun generateFrameSequence(frameCount: Int): List<CameraFrame> {
        return createMockCameraFrames(frameCount, CameraResolution.HD_1080, 30f)
    }

    private fun generateMockImageData(resolution: CameraResolution): ByteArray {
        return ByteArray(resolution.width * resolution.height * 3) { 
            (Random.nextInt(256)).toByte() 
        }
    }

    private fun getHazardTypesForWorkType(workType: WorkType): List<HazardType> {
        return when (workType) {
            WorkType.ELECTRICAL_WORK -> listOf(
                HazardType.ELECTRICAL_HAZARD, HazardType.PPE_VIOLATION, HazardType.HOUSEKEEPING
            )
            WorkType.FALL_PROTECTION -> listOf(
                HazardType.FALL_PROTECTION, HazardType.PPE_VIOLATION, HazardType.STRUCTURAL_SAFETY
            )
            else -> listOf(
                HazardType.FALL_PROTECTION, HazardType.PPE_VIOLATION, 
                HazardType.ELECTRICAL_HAZARD, HazardType.HOUSEKEEPING
            )
        }
    }

    private fun getSeverityDistribution(riskLevel: RiskLevel): Map<Severity, Float> {
        return when (riskLevel) {
            RiskLevel.HIGH -> mapOf(
                Severity.CRITICAL to 0.3f,
                Severity.HIGH to 0.4f,
                Severity.MEDIUM to 0.2f,
                Severity.LOW to 0.1f
            )
            RiskLevel.MEDIUM -> mapOf(
                Severity.CRITICAL to 0.1f,
                Severity.HIGH to 0.3f,
                Severity.MEDIUM to 0.4f,
                Severity.LOW to 0.2f
            )
            RiskLevel.LOW -> mapOf(
                Severity.CRITICAL to 0.05f,
                Severity.HIGH to 0.15f,
                Severity.MEDIUM to 0.3f,
                Severity.LOW to 0.5f
            )
            else -> mapOf(
                Severity.LOW to 1.0f
            )
        }
    }

    private fun selectSeverityFromDistribution(distribution: Map<Severity, Float>): Severity {
        val random = Random.nextFloat()
        var cumulative = 0f
        
        for ((severity, probability) in distribution) {
            cumulative += probability
            if (random <= cumulative) {
                return severity
            }
        }
        
        return Severity.LOW // Fallback
    }

    private fun getSceneComplexity(hazardCount: Int): String {
        return when {
            hazardCount <= 2 -> "simple"
            hazardCount <= 5 -> "moderate"
            hazardCount <= 10 -> "complex"
            else -> "highly_complex"
        }
    }

    private fun getOSHACodeForHazardType(hazardType: HazardType): String {
        return when (hazardType) {
            HazardType.FALL_PROTECTION -> "1926.501(b)(1)"
            HazardType.ELECTRICAL_HAZARD -> "1926.416(a)(1)"
            HazardType.PPE_VIOLATION -> "1926.95(a)"
            HazardType.MACHINERY_HAZARD -> "1926.1053(a)"
            HazardType.STRUCTURAL_SAFETY -> "1926.451(f)(3)"
            HazardType.HOUSEKEEPING -> "1926.25(a)"
        }
    }

    private fun generateRecommendations(hazards: List<Hazard>): List<String> {
        return hazards.flatMap { it.recommendations }.distinct()
    }

    private fun generateOSHAViolations(hazards: List<Hazard>): List<com.hazardhawk.models.OSHAViolation> {
        return hazards.map { hazard ->
            com.hazardhawk.models.OSHAViolation(
                oshaCode = hazard.oshaCode ?: "1926.001",
                description = hazard.description,
                severity = hazard.severity.name,
                recommendations = hazard.recommendations
            )
        }
    }
}

// Data classes for test scenarios
data class ConstructionScenario(
    val id: String,
    val workType: WorkType,
    val hazards: List<Hazard>,
    val analysis: SafetyAnalysis,
    val environmentalConditions: EnvironmentalConditions,
    val workerCount: Int,
    val metadata: Map<String, Any>
)

data class FallProtectionScenario(
    val id: String,
    val height: Float,
    val hasProtection: Boolean,
    val workSurface: String,
    val hazard: Hazard,
    val oshaViolation: String
)

data class ElectricalScenario(
    val id: String,
    val voltage: String,
    val hazardType: String,
    val isEnergized: Boolean,
    val hazard: Hazard,
    val requiredPPE: List<String>
)

data class PPEScenario(
    val id: String,
    val missingPPE: List<String>,
    val presentPPE: List<String>,
    val workType: WorkType,
    val hazard: Hazard,
    val complianceScore: Float
)

data class OSHAComplianceScenario(
    val id: String,
    val workType: WorkType,
    val isCompliant: Boolean,
    val violations: List<OSHAViolation>,
    val complianceScore: Double,
    val requiredActions: List<String>
)

data class PerformanceTestData(
    val lightLoadScenario: ConstructionScenario,
    val mediumLoadScenario: ConstructionScenario,
    val heavyLoadScenario: ConstructionScenario,
    val stressTestScenario: ConstructionScenario,
    val frameSequence: List<CameraFrame>,
    val targetMetrics: PerformanceTargets
)

data class PerformanceTargets(
    val maxFrameTime: Float,
    val maxAnalysisTime: Long,
    val maxMemoryUsage: Long
)

data class CameraFrame(
    val timestamp: Long,
    val data: ByteArray,
    val width: Int,
    val height: Int,
    val format: ImageFormat,
    val metadata: CameraFrameMetadata
)

data class CameraFrameMetadata(
    val exposure: Float,
    val iso: Int,
    val focusDistance: Float,
    val lightingLevel: Float
)

data class EnvironmentalConditions(
    val lightingLevel: Float,
    val weatherCondition: String,
    val temperature: Int,
    val windSpeed: Float,
    val visibility: Float
)

data class OSHAViolation(
    val code: String,
    val description: String,
    val severity: ViolationSeverity,
    val category: ViolationCategory
)

enum class CameraResolution(val width: Int, val height: Int) {
    HD_720(1280, 720),
    HD_1080(1920, 1080),
    UHD_4K(3840, 2160)
}

enum class ImageFormat {
    YUV_420_888,
    JPEG,
    RGB_888
}

enum class ViolationSeverity { NONE, MINOR, MAJOR, CRITICAL }
enum class ViolationCategory { FALL_PROTECTION, PPE, ELECTRICAL, STRUCTURAL_SAFETY, MACHINERY, GENERAL }
