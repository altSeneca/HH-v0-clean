package com.hazardhawk

import com.hazardhawk.ai.models.*
import kotlinx.uuid.uuid4

/**
 * Factory for creating test data objects used across all test suites.
 * Provides realistic construction safety scenarios for comprehensive testing.
 */
object TestDataFactory {
    
    /**
     * Creates a sample SafetyAnalysis for testing AI orchestrator functionality.
     */
    fun createSampleSafetyAnalysis(
        analysisType: AnalysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
        hazardCount: Int = 3,
        includeCriticalHazards: Boolean = true,
        confidence: Float = 0.87f,
        processingTime: Long = 2340L
    ): SafetyAnalysis {
        
        val hazards = mutableListOf<Hazard>()
        
        // Add critical fall protection hazard if requested
        if (includeCriticalHazards) {
            hazards.add(createFallProtectionHazard())
        }
        
        // Add additional hazards to reach target count
        val remainingCount = hazardCount - hazards.size
        repeat(remainingCount) { index ->
            when (index % 4) {
                0 -> hazards.add(createPPEViolationHazard())
                1 -> hazards.add(createElectricalHazard())
                2 -> hazards.add(createHousekeepingHazard())
                3 -> hazards.add(createMechanicalHazard())
            }
        }
        
        return SafetyAnalysis(
            id = "test-analysis-${uuid4()}",
            timestamp = System.currentTimeMillis(),
            analysisType = analysisType,
            workType = WorkType.GENERAL_CONSTRUCTION,
            hazards = hazards.take(hazardCount),
            ppeStatus = createSamplePPEStatus(),
            recommendations = createSampleRecommendations(hazards),
            overallRiskLevel = determineOverallRisk(hazards),
            confidence = confidence,
            processingTimeMs = processingTime,
            oshaViolations = createSampleOSHAViolations(hazards),
            metadata = createSampleMetadata()
        )
    }
    
    /**
     * Creates a critical fall protection hazard.
     */
    fun createFallProtectionHazard(): Hazard {
        return Hazard(
            id = "hazard-fall-${uuid4()}",
            type = HazardType.FALL_PROTECTION,
            severity = Severity.CRITICAL,
            description = "Worker at height without fall protection system",
            oshaCode = "1926.501(b)(1)",
            boundingBox = BoundingBox(0.2f, 0.1f, 0.3f, 0.4f),
            confidence = 0.89f,
            recommendations = listOf(
                "Install proper fall protection system immediately",
                "Ensure worker is wearing proper fall arrest harness",
                "Provide safety training on fall protection"
            ),
            immediateAction = "STOP WORK - Install fall protection before continuing"
        )
    }
    
    /**
     * Creates a PPE violation hazard.
     */
    fun createPPEViolationHazard(): Hazard {
        return Hazard(
            id = "hazard-ppe-${uuid4()}",
            type = HazardType.PPE_VIOLATION,
            severity = Severity.HIGH,
            description = "Hard hat not worn by worker",
            oshaCode = "1926.95(a)",
            boundingBox = BoundingBox(0.15f, 0.05f, 0.2f, 0.25f),
            confidence = 0.94f,
            recommendations = listOf(
                "Require hard hat use in all construction areas",
                "Conduct PPE inspection before work begins"
            ),
            immediateAction = "Worker must don hard hat before continuing work"
        )
    }
    
    /**
     * Creates an electrical hazard.
     */
    fun createElectricalHazard(): Hazard {
        return Hazard(
            id = "hazard-electrical-${uuid4()}",
            type = HazardType.ELECTRICAL_HAZARD,
            severity = Severity.MEDIUM,
            description = "Exposed electrical wiring in work area",
            oshaCode = "1926.416(a)(1)",
            boundingBox = BoundingBox(0.6f, 0.7f, 0.2f, 0.15f),
            confidence = 0.76f,
            recommendations = listOf(
                "Cover or secure exposed wiring",
                "Verify electrical systems are de-energized",
                "Follow lockout/tagout procedures"
            )
        )
    }
    
    /**
     * Creates a housekeeping hazard.
     */
    fun createHousekeepingHazard(): Hazard {
        return Hazard(
            id = "hazard-housekeeping-${uuid4()}",
            type = HazardType.HOUSEKEEPING,
            severity = Severity.LOW,
            description = "Tools and materials scattered in walkway",
            oshaCode = "1926.25(a)",
            boundingBox = BoundingBox(0.3f, 0.8f, 0.4f, 0.2f),
            confidence = 0.68f,
            recommendations = listOf(
                "Clear walkways of tools and materials",
                "Designate specific storage areas for tools",
                "Implement daily housekeeping inspections"
            )
        )
    }
    
    /**
     * Creates a mechanical hazard.
     */
    fun createMechanicalHazard(): Hazard {
        return Hazard(
            id = "hazard-mechanical-${uuid4()}",
            type = HazardType.MECHANICAL_HAZARD,
            severity = Severity.HIGH,
            description = "Unguarded rotating machinery",
            oshaCode = "1926.300(b)(2)",
            boundingBox = BoundingBox(0.4f, 0.3f, 0.35f, 0.25f),
            confidence = 0.82f,
            recommendations = listOf(
                "Install proper machine guarding",
                "Lockout/tagout procedures before maintenance",
                "Train operators on safe machine operation"
            )
        )
    }
    
    /**
     * Creates sample PPE status with realistic detection results.
     */
    fun createSamplePPEStatus(): PPEStatus {
        return PPEStatus(
            hardHat = PPEItem(PPEItemStatus.MISSING, 0.94f, null, true),
            safetyVest = PPEItem(PPEItemStatus.PRESENT, 0.87f, 
                BoundingBox(0.1f, 0.25f, 0.4f, 0.35f), true),
            safetyBoots = PPEItem(PPEItemStatus.PRESENT, 0.82f,
                BoundingBox(0.15f, 0.8f, 0.3f, 0.2f), true),
            safetyGlasses = PPEItem(PPEItemStatus.UNKNOWN, 0.45f, null, false),
            fallProtection = PPEItem(PPEItemStatus.MISSING, 0.92f, null, true),
            respirator = PPEItem(PPEItemStatus.UNKNOWN, 0.2f, null, false),
            overallCompliance = 0.4f
        )
    }
    
    /**
     * Creates sample recommendations based on identified hazards.
     */
    fun createSampleRecommendations(hazards: List<Hazard>): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (hazards.any { it.type == HazardType.FALL_PROTECTION }) {
            recommendations.add("Immediately implement fall protection measures for workers at height")
        }
        if (hazards.any { it.type == HazardType.PPE_VIOLATION }) {
            recommendations.add("Ensure all workers wear required PPE")
        }
        if (hazards.any { it.type == HazardType.ELECTRICAL_HAZARD }) {
            recommendations.add("Address exposed electrical hazards before continuing work")
        }
        
        recommendations.addAll(listOf(
            "Conduct comprehensive safety training for all personnel",
            "Establish proper safety inspection procedures"
        ))
        
        return recommendations
    }
    
    /**
     * Creates sample OSHA violations based on hazards.
     */
    fun createSampleOSHAViolations(hazards: List<Hazard>): List<OSHAViolation> {
        return hazards.mapNotNull { hazard ->
            hazard.oshaCode?.let { code ->
                when (hazard.type) {
                    HazardType.FALL_PROTECTION -> OSHAViolation(
                        code = code,
                        title = "Fall Protection - Unprotected Sides and Edges",
                        description = "Each employee on a walking/working surface with an unprotected side or edge which is 6 feet or more above a lower level shall be protected from falling",
                        severity = hazard.severity,
                        fineRange = "$7,000 - $70,000",
                        correctiveAction = "Install guardrail systems, safety net systems, or personal fall arrest systems"
                    )
                    HazardType.PPE_VIOLATION -> OSHAViolation(
                        code = code,
                        title = "Personal Protective Equipment - Head Protection",
                        description = "Employees working in areas where there is a possible danger of head injury from impact shall be protected by protective helmets",
                        severity = hazard.severity,
                        fineRange = "$2,500 - $25,000",
                        correctiveAction = "Provide and require use of ANSI-approved hard hats"
                    )
                    else -> null
                }
            }
        }
    }
    
    /**
     * Creates sample analysis metadata.
     */
    fun createSampleMetadata(): AnalysisMetadata {
        return AnalysisMetadata(
            imageWidth = 1920,
            imageHeight = 1080,
            location = Location(
                latitude = 40.7128,
                longitude = -74.0060,
                accuracy = 5.0f,
                address = "Construction Site, New York, NY"
            ),
            weather = WeatherConditions(
                temperature = 22.5f,
                humidity = 65.0f,
                windSpeed = 12.0f,
                conditions = "Partly cloudy"
            ),
            timeOfDay = "Morning (09:45 AM)"
        )
    }
    
    /**
     * Determines overall risk level based on hazards.
     */
    private fun determineOverallRisk(hazards: List<Hazard>): RiskLevel {
        return when {
            hazards.any { it.severity == Severity.CRITICAL } -> RiskLevel.SEVERE
            hazards.any { it.severity == Severity.HIGH } -> RiskLevel.HIGH
            hazards.any { it.severity == Severity.MEDIUM } -> RiskLevel.MODERATE
            hazards.any { it.severity == Severity.LOW } -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
    }
    
    /**
     * Creates mock image data for testing.
     */
    fun createMockImageData(width: Int = 1920, height: Int = 1080): ByteArray {
        // Create a simple mock image data (in a real app this would be actual image bytes)
        return ByteArray(width * height * 3) { index ->
            // Create a simple pattern for testing
            (index % 256).toByte()
        }
    }
    
    /**
     * Creates performance test scenarios with different complexity levels.
     */
    data class PerformanceTestScenario(
        val name: String,
        val imageSize: Pair<Int, Int>,
        val hazardCount: Int,
        val expectedProcessingTime: Long,
        val targetFPS: Int
    )
    
    fun createPerformanceScenarios(): List<PerformanceTestScenario> {
        return listOf(
            PerformanceTestScenario("Light Load", 640 to 480, 1, 500L, 30),
            PerformanceTestScenario("Medium Load", 1280 to 720, 3, 1500L, 15),
            PerformanceTestScenario("Heavy Load", 1920 to 1080, 5, 3000L, 5),
            PerformanceTestScenario("Stress Test", 3840 to 2160, 10, 8000L, 2)
        )
    }
}
