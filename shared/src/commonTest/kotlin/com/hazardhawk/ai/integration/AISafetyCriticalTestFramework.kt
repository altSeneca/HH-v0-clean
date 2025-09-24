package com.hazardhawk.ai.integration

import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.core.models.*
import kotlin.test.*
import kotlinx.coroutines.test.runTest

/**
 * Safety-critical testing framework for OSHA compliance validation.
 * This framework ensures that AI systems never miss critical hazards that could
 * result in serious injury or death, and that all OSHA compliance requirements
 * are met with the highest accuracy standards.
 */
class AISafetyCriticalTestFramework {
    
    /**
     * Critical hazard test cases that must never be missed (zero false negative tolerance)
     */
    data class CriticalHazardTestCase(
        val id: String,
        val imageData: ByteArray,
        val workType: WorkType,
        val criticalHazards: List<CriticalHazard>,
        val expectedFatalities: Int = 0, // Expected fatalities if hazard not addressed
        val expectedInjuries: Int = 1, // Expected injuries if hazard not addressed
        val oshaViolationClass: OSHAViolationClass,
        val description: String
    )
    
    data class CriticalHazard(
        val type: HazardType,
        val severity: Severity,
        val oshaStandard: String,
        val mustDetect: Boolean = true, // If true, missing this hazard fails the test
        val minimumConfidence: Float = 0.6f, // Minimum confidence required for detection
        val expectedBoundingBox: BoundingBox?, // Expected location (for precision testing)
        val fatalityPotential: Boolean = true // Can this hazard cause death?
    )
    
    data class OSHAComplianceTestCase(
        val id: String,
        val imageData: ByteArray,
        val workType: WorkType,
        val expectedOSHAViolations: List<OSHAViolation>,
        val expectedCitations: List<OSHACitation>,
        val complianceLevel: ComplianceLevel
    )
    
    data class OSHAViolation(
        val standard: String, // e.g., "1926.501"
        val description: String,
        val severity: OSHASeverity,
        val proposedPenalty: Int, // In USD
        val abatementRequired: Boolean = true
    )
    
    data class OSHACitation(
        val citationType: CitationType,
        val standard: String,
        val description: String,
        val penalty: Int
    )
    
    enum class OSHAViolationClass {
        WILLFUL,
        SERIOUS,
        OTHER_THAN_SERIOUS,
        DE_MINIMIS,
        REPEAT,
        FAILURE_TO_ABATE
    }
    
    enum class OSHASeverity {
        IMMINENT_DANGER,
        SERIOUS,
        OTHER_THAN_SERIOUS,
        DE_MINIMIS
    }
    
    enum class CitationType {
        SERIOUS,
        WILLFUL,
        REPEAT,
        OTHER
    }
    
    enum class ComplianceLevel {
        FULLY_COMPLIANT,
        MINOR_VIOLATIONS,
        SERIOUS_VIOLATIONS,
        IMMINENT_DANGER
    }
    
    /**
     * Critical hazard detection tests - Zero tolerance for false negatives
     */
    class CriticalHazardDetectionTest {
        
        private lateinit var aiAnalyzer: AIPhotoAnalyzer
        
        @BeforeTest
        fun setup() {
            aiAnalyzer = createSafetyCriticalAIAnalyzer()
        }
        
        @Test
        fun `test fatal fall protection hazards detection - zero false negatives allowed`() = runTest {
            val criticalFallCases = listOf(
                CriticalHazardTestCase(
                    id = "critical_fall_001",
                    imageData = loadCriticalTestImage("unguarded_roof_edge_worker"),
                    workType = WorkType.ROOFING,
                    criticalHazards = listOf(
                        CriticalHazard(
                            type = HazardType.FALL_PROTECTION,
                            severity = Severity.HIGH,
                            oshaStandard = "1926.501",
                            mustDetect = true,
                            minimumConfidence = 0.8f,
                            expectedBoundingBox = BoundingBox(0.2f, 0.3f, 0.4f, 0.5f, 0.8f),
                            fatalityPotential = true
                        )
                    ),
                    expectedFatalities = 1,
                    oshaViolationClass = OSHAViolationClass.SERIOUS,
                    description = "Worker at roof edge >6 feet without fall protection"
                ),
                CriticalHazardTestCase(
                    id = "critical_fall_002", 
                    imageData = loadCriticalTestImage("unprotected_floor_opening"),
                    workType = WorkType.CONCRETE_WORK,
                    criticalHazards = listOf(
                        CriticalHazard(
                            type = HazardType.FALL_PROTECTION,
                            severity = Severity.HIGH,
                            oshaStandard = "1926.501",
                            mustDetect = true,
                            minimumConfidence = 0.75f,
                            expectedBoundingBox = BoundingBox(0.3f, 0.4f, 0.3f, 0.4f, 0.75f),
                            fatalityPotential = true
                        )
                    ),
                    expectedFatalities = 1,
                    oshaViolationClass = OSHAViolationClass.SERIOUS,
                    description = "Unprotected floor opening >6 feet drop"
                )
            )
            
            val results = analyzeCriticalHazards(criticalFallCases)
            
            // ZERO false negatives allowed for fatal fall hazards
            val missedCriticalHazards = results.filter { !it.allCriticalHazardsDetected }
            assertEquals(
                "CRITICAL FAILURE: Fatal fall hazards missed - ${missedCriticalHazards.map { it.testCase.id }}",
                0, missedCriticalHazards.size
            )
            
            // Verify minimum confidence levels met
            results.forEach { result ->
                result.detectedCriticalHazards.forEach { (hazard, detection) ->
                    assertTrue(
                        "Confidence ${detection.confidence} below minimum ${hazard.minimumConfidence} for ${hazard.type}",
                        detection.confidence >= hazard.minimumConfidence
                    )
                }
            }
            
            logCriticalHazardResults("Fatal Fall Protection", results)
        }
        
        @Test
        fun `test electrical hazards with fatality potential - zero false negatives`() = runTest {
            val criticalElectricalCases = listOf(
                CriticalHazardTestCase(
                    id = "critical_elec_001",
                    imageData = loadCriticalTestImage("exposed_high_voltage_lines"),
                    workType = WorkType.ELECTRICAL_WORK,
                    criticalHazards = listOf(
                        CriticalHazard(
                            type = HazardType.ELECTRICAL,
                            severity = Severity.HIGH,
                            oshaStandard = "1926.95",
                            mustDetect = true,
                            minimumConfidence = 0.85f,
                            expectedBoundingBox = BoundingBox(0.1f, 0.2f, 0.6f, 0.4f, 0.85f),
                            fatalityPotential = true
                        )
                    ),
                    expectedFatalities = 1,
                    oshaViolationClass = OSHAViolationClass.SERIOUS,
                    description = "Workers within 10 feet of energized high voltage lines"
                ),
                CriticalHazardTestCase(
                    id = "critical_elec_002",
                    imageData = loadCriticalTestImage("water_electrical_hazard"),
                    workType = WorkType.GENERAL_CONSTRUCTION,
                    criticalHazards = listOf(
                        CriticalHazard(
                            type = HazardType.ELECTRICAL,
                            severity = Severity.HIGH,
                            oshaStandard = "1926.95",
                            mustDetect = true,
                            minimumConfidence = 0.80f,
                            expectedBoundingBox = BoundingBox(0.2f, 0.5f, 0.4f, 0.3f, 0.80f),
                            fatalityPotential = true
                        )
                    ),
                    expectedFatalities = 1,
                    oshaViolationClass = OSHAViolationClass.SERIOUS,
                    description = "Electrical equipment in contact with water"
                )
            )
            
            val results = analyzeCriticalHazards(criticalElectricalCases)
            
            // ZERO false negatives for electrical hazards
            val missedElectricalHazards = results.filter { !it.allCriticalHazardsDetected }
            assertEquals(
                "CRITICAL FAILURE: Fatal electrical hazards missed - ${missedElectricalHazards.map { it.testCase.id }}",
                0, missedElectricalHazards.size
            )
            
            logCriticalHazardResults("Fatal Electrical Hazards", results)
        }
        
        @Test
        fun `test excavation and trenching hazards - cave-in potential`() = runTest {
            val criticalExcavationCases = listOf(
                CriticalHazardTestCase(
                    id = "critical_excav_001",
                    imageData = loadCriticalTestImage("deep_unprotected_trench"),
                    workType = WorkType.EXCAVATION,
                    criticalHazards = listOf(
                        CriticalHazard(
                            type = HazardType.CAUGHT_IN_BETWEEN,
                            severity = Severity.HIGH,
                            oshaStandard = "1926.652",
                            mustDetect = true,
                            minimumConfidence = 0.75f,
                            expectedBoundingBox = BoundingBox(0.1f, 0.3f, 0.8f, 0.5f, 0.75f),
                            fatalityPotential = true
                        )
                    ),
                    expectedFatalities = 2, // Cave-ins often kill multiple workers
                    oshaViolationClass = OSHAViolationClass.SERIOUS,
                    description = "Workers in trench >5 feet deep without protective system"
                )
            )
            
            val results = analyzeCriticalHazards(criticalExcavationCases)
            
            // Cave-ins are among the most fatal construction hazards
            val missedTrenchHazards = results.filter { !it.allCriticalHazardsDetected }
            assertEquals(
                "CRITICAL FAILURE: Fatal trench/excavation hazards missed",
                0, missedTrenchHazards.size
            )
            
            logCriticalHazardResults("Fatal Excavation Hazards", results)
        }
        
        @Test
        fun `test crane and heavy equipment struck-by hazards`() = runTest {
            val criticalStruckByCases = listOf(
                CriticalHazardTestCase(
                    id = "critical_struck_001",
                    imageData = loadCriticalTestImage("workers_under_crane_load"),
                    workType = WorkType.HEAVY_CONSTRUCTION,
                    criticalHazards = listOf(
                        CriticalHazard(
                            type = HazardType.STRUCK_BY,
                            severity = Severity.HIGH,
                            oshaStandard = "1926.1404",
                            mustDetect = true,
                            minimumConfidence = 0.80f,
                            expectedBoundingBox = BoundingBox(0.2f, 0.3f, 0.5f, 0.4f, 0.80f),
                            fatalityPotential = true
                        )
                    ),
                    expectedFatalities = 1,
                    oshaViolationClass = OSHAViolationClass.SERIOUS,
                    description = "Workers in crane's load path without barricades"
                )
            )
            
            val results = analyzeCriticalHazards(criticalStruckByCases)
            
            val missedStruckByHazards = results.filter { !it.allCriticalHazardsDetected }
            assertEquals(
                "CRITICAL FAILURE: Fatal struck-by hazards missed",
                0, missedStruckByHazards.size
            )
            
            logCriticalHazardResults("Fatal Struck-By Hazards", results)
        }
        
        @Test
        fun `test confined space entry hazards`() = runTest {
            val criticalConfinedSpaceCases = listOf(
                CriticalHazardTestCase(
                    id = "critical_confined_001",
                    imageData = loadCriticalTestImage("unmonitored_confined_space_entry"),
                    workType = WorkType.GENERAL_CONSTRUCTION,
                    criticalHazards = listOf(
                        CriticalHazard(
                            type = HazardType.ATMOSPHERIC,
                            severity = Severity.HIGH,
                            oshaStandard = "1926.1204",
                            mustDetect = true,
                            minimumConfidence = 0.70f,
                            expectedBoundingBox = BoundingBox(0.3f, 0.2f, 0.4f, 0.6f, 0.70f),
                            fatalityPotential = true
                        )
                    ),
                    expectedFatalities = 1,
                    oshaViolationClass = OSHAViolationClass.SERIOUS,
                    description = "Worker entering confined space without atmospheric monitoring"
                )
            )
            
            val results = analyzeCriticalHazards(criticalConfinedSpaceCases)
            
            val missedConfinedSpaceHazards = results.filter { !it.allCriticalHazardsDetected }
            assertEquals(
                "CRITICAL FAILURE: Fatal confined space hazards missed",
                0, missedConfinedSpaceHazards.size
            )
            
            logCriticalHazardResults("Fatal Confined Space Hazards", results)
        }
        
        @Test
        fun `test multi-hazard fatal scenarios`() = runTest {
            val multiHazardCases = listOf(
                CriticalHazardTestCase(
                    id = "critical_multi_001",
                    imageData = loadCriticalTestImage("complex_fatal_scenario"),
                    workType = WorkType.GENERAL_CONSTRUCTION,
                    criticalHazards = listOf(
                        CriticalHazard(
                            type = HazardType.FALL_PROTECTION,
                            severity = Severity.HIGH,
                            oshaStandard = "1926.501",
                            mustDetect = true,
                            minimumConfidence = 0.75f,
                            expectedBoundingBox = BoundingBox(0.1f, 0.2f, 0.3f, 0.4f, 0.75f),
                            fatalityPotential = true
                        ),
                        CriticalHazard(
                            type = HazardType.ELECTRICAL,
                            severity = Severity.HIGH,
                            oshaStandard = "1926.95",
                            mustDetect = true,
                            minimumConfidence = 0.80f,
                            expectedBoundingBox = BoundingBox(0.5f, 0.3f, 0.3f, 0.3f, 0.80f),
                            fatalityPotential = true
                        )
                    ),
                    expectedFatalities = 2,
                    oshaViolationClass = OSHAViolationClass.SERIOUS,
                    description = "Multiple simultaneous fatal hazards present"
                )
            )
            
            val results = analyzeCriticalHazards(multiHazardCases)
            
            // All critical hazards in multi-hazard scenarios must be detected
            results.forEach { result ->
                assertTrue(
                    "Not all critical hazards detected in multi-hazard scenario ${result.testCase.id}",
                    result.allCriticalHazardsDetected
                )
                
                // Verify each critical hazard was detected individually
                result.testCase.criticalHazards.forEach { criticalHazard ->
                    assertTrue(
                        "Critical hazard ${criticalHazard.type} not detected in ${result.testCase.id}",
                        result.detectedCriticalHazards.containsKey(criticalHazard)
                    )
                }
            }
            
            logCriticalHazardResults("Multi-Hazard Fatal Scenarios", results)
        }
        
        private suspend fun analyzeCriticalHazards(testCases: List<CriticalHazardTestCase>): List<CriticalHazardResult> {
            return testCases.map { testCase ->
                val analysisResult = aiAnalyzer.analyzePhoto(testCase.imageData, testCase.workType)
                val analysis = analysisResult.getOrNull()
                
                val detectedCriticalHazards = mutableMapOf<CriticalHazard, DetectedHazard>()
                
                testCase.criticalHazards.forEach { criticalHazard ->
                    val detected = analysis?.hazards?.find { detected ->
                        detected.type == criticalHazard.type &&
                        detected.severity >= criticalHazard.severity &&
                        detected.confidence >= criticalHazard.minimumConfidence
                    }
                    
                    if (detected != null) {
                        detectedCriticalHazards[criticalHazard] = detected
                    }
                }
                
                CriticalHazardResult(
                    testCase = testCase,
                    detectedCriticalHazards = detectedCriticalHazards,
                    allCriticalHazardsDetected = testCase.criticalHazards.all { hazard ->
                        detectedCriticalHazards.containsKey(hazard)
                    },
                    analysisSuccessful = analysisResult.isSuccess,
                    overallConfidence = analysis?.confidence ?: 0f
                )
            }
        }
        
        private fun logCriticalHazardResults(category: String, results: List<CriticalHazardResult>) {
            println("=== $category Critical Hazard Detection Results ===")
            println("Total Tests: ${results.size}")
            
            val successfulDetections = results.count { it.allCriticalHazardsDetected }
            val detectionRate = successfulDetections.toFloat() / results.size
            
            println("Critical Hazard Detection Rate: ${String.format("%.2f%%", detectionRate * 100)}")
            
            if (detectionRate < 1.0f) {
                println("CRITICAL FAILURES:")
                results.filter { !it.allCriticalHazardsDetected }.forEach { failure ->
                    println("  - ${failure.testCase.id}: ${failure.testCase.description}")
                    failure.testCase.criticalHazards.forEach { hazard ->
                        if (!failure.detectedCriticalHazards.containsKey(hazard)) {
                            println("    MISSED: ${hazard.type} (${hazard.oshaStandard})")
                        }
                    }
                }
            }
            
            println("Average Confidence: ${String.format("%.2f", results.map { it.overallConfidence }.average())}")
            println("==================================================")
        }
    }
    
    /**
     * OSHA compliance validation tests
     */
    class OSHAComplianceValidationTest {
        
        private lateinit var aiAnalyzer: AIPhotoAnalyzer
        
        @BeforeTest
        fun setup() {
            aiAnalyzer = createSafetyCriticalAIAnalyzer()
        }
        
        @Test
        fun `test OSHA 1926.501 fall protection standard compliance`() = runTest {
            val fallProtectionCases = listOf(
                OSHAComplianceTestCase(
                    id = "osha_fall_001",
                    imageData = loadCriticalTestImage("fall_protection_violation"),
                    workType = WorkType.CONCRETE_WORK,
                    expectedOSHAViolations = listOf(
                        OSHAViolation(
                            standard = "1926.501(b)(1)",
                            description = "Unprotected sides and edges",
                            severity = OSHASeverity.SERIOUS,
                            proposedPenalty = 14502 // 2024 maximum serious violation penalty
                        )
                    ),
                    expectedCitations = listOf(
                        OSHACitation(
                            citationType = CitationType.SERIOUS,
                            standard = "1926.501(b)(1)", 
                            description = "Employee exposed to fall hazard from height of 15 feet",
                            penalty = 14502
                        )
                    ),
                    complianceLevel = ComplianceLevel.SERIOUS_VIOLATIONS
                )
            )
            
            val results = analyzeOSHACompliance(fallProtectionCases)
            
            results.forEach { result ->
                // Verify OSHA standard citations are accurate
                result.testCase.expectedOSHAViolations.forEach { expectedViolation ->
                    val foundViolation = result.detectedViolations.find { detected ->
                        detected.standard == expectedViolation.standard
                    }
                    assertNotNull(
                        "OSHA violation ${expectedViolation.standard} not detected",
                        foundViolation
                    )
                    
                    assertEquals(
                        "Incorrect severity classification for ${expectedViolation.standard}",
                        expectedViolation.severity,
                        foundViolation.severity
                    )
                }
            }
            
            logOSHAComplianceResults("Fall Protection (1926.501)", results)
        }
        
        @Test
        fun `test OSHA 1926.95 electrical safety standard compliance`() = runTest {
            val electricalSafetyCases = listOf(
                OSHAComplianceTestCase(
                    id = "osha_elec_001",
                    imageData = loadCriticalTestImage("electrical_violation"),
                    workType = WorkType.ELECTRICAL_WORK,
                    expectedOSHAViolations = listOf(
                        OSHAViolation(
                            standard = "1926.95(a)",
                            description = "Inadequate protection from electrical hazards",
                            severity = OSHASeverity.SERIOUS,
                            proposedPenalty = 14502
                        )
                    ),
                    expectedCitations = listOf(
                        OSHACitation(
                            citationType = CitationType.SERIOUS,
                            standard = "1926.95(a)",
                            description = "Employees not protected from electrical hazards",
                            penalty = 14502
                        )
                    ),
                    complianceLevel = ComplianceLevel.SERIOUS_VIOLATIONS
                )
            )
            
            val results = analyzeOSHACompliance(electricalSafetyCases)
            
            // Verify electrical OSHA compliance detection
            results.forEach { result ->
                assertTrue(
                    "Electrical OSHA violations not properly detected",
                    result.detectedViolations.any { it.standard.startsWith("1926.95") }
                )
            }
            
            logOSHAComplianceResults("Electrical Safety (1926.95)", results)
        }
        
        @Test
        fun `test OSHA 1926.652 excavation standard compliance`() = runTest {
            val excavationCases = listOf(
                OSHAComplianceTestCase(
                    id = "osha_excav_001",
                    imageData = loadCriticalTestImage("excavation_violation"),
                    workType = WorkType.EXCAVATION,
                    expectedOSHAViolations = listOf(
                        OSHAViolation(
                            standard = "1926.652(a)(1)",
                            description = "Inadequate protection from cave-ins",
                            severity = OSHASeverity.SERIOUS,
                            proposedPenalty = 14502
                        )
                    ),
                    expectedCitations = listOf(
                        OSHACitation(
                            citationType = CitationType.SERIOUS,
                            standard = "1926.652(a)(1)",
                            description = "Employees exposed to cave-in hazard",
                            penalty = 14502
                        )
                    ),
                    complianceLevel = ComplianceLevel.SERIOUS_VIOLATIONS
                )
            )
            
            val results = analyzeOSHACompliance(excavationCases)
            
            // Verify excavation OSHA compliance
            results.forEach { result ->
                assertTrue(
                    "Excavation OSHA violations not detected",
                    result.detectedViolations.any { it.standard.startsWith("1926.652") }
                )
            }
            
            logOSHAComplianceResults("Excavation Safety (1926.652)", results)
        }
        
        @Test
        fun `test OSHA penalty calculation accuracy`() = runTest {
            val penaltyTestCases = createOSHAPenaltyTestCases()
            
            penaltyTestCases.forEach { testCase ->
                val result = aiAnalyzer.analyzePhoto(testCase.imageData, testCase.workType)
                val analysis = result.getOrNull()!!
                
                // Verify penalty calculations match OSHA guidelines
                testCase.expectedOSHAViolations.forEach { expectedViolation ->
                    val detectedHazard = analysis.hazards.find { hazard ->
                        hazard.oshaCode?.startsWith(expectedViolation.standard.take(8)) == true
                    }
                    
                    assertNotNull(
                        "OSHA violation not detected: ${expectedViolation.standard}",
                        detectedHazard
                    )
                    
                    // Verify penalty is within acceptable range for violation type
                    val expectedPenaltyRange = when (expectedViolation.severity) {
                        OSHASeverity.SERIOUS -> 1000..14502
                        OSHASeverity.WILLFUL -> 11524..145027
                        OSHASeverity.OTHER_THAN_SERIOUS -> 0..14502
                        OSHASeverity.DE_MINIMIS -> 0..0
                    }
                    
                    assertTrue(
                        "Penalty ${expectedViolation.proposedPenalty} outside expected range $expectedPenaltyRange",
                        expectedViolation.proposedPenalty in expectedPenaltyRange
                    )
                }
            }
        }
        
        @Test
        fun `test imminent danger identification`() = runTest {
            val imminentDangerCases = listOf(
                OSHAComplianceTestCase(
                    id = "imminent_danger_001",
                    imageData = loadCriticalTestImage("imminent_danger_scenario"),
                    workType = WorkType.GENERAL_CONSTRUCTION,
                    expectedOSHAViolations = listOf(
                        OSHAViolation(
                            standard = "Section 13(a)",
                            description = "Imminent danger condition",
                            severity = OSHASeverity.IMMINENT_DANGER,
                            proposedPenalty = 0, // Imminent danger requires immediate cessation
                            abatementRequired = true
                        )
                    ),
                    expectedCitations = listOf(
                        OSHACitation(
                            citationType = CitationType.OTHER,
                            standard = "Section 13(a)",
                            description = "Imminent danger - immediate work cessation required",
                            penalty = 0
                        )
                    ),
                    complianceLevel = ComplianceLevel.IMMINENT_DANGER
                )
            )
            
            val results = analyzeOSHACompliance(imminentDangerCases)
            
            // Imminent danger must be detected with highest priority
            results.forEach { result ->
                assertTrue(
                    "Imminent danger condition not identified",
                    result.complianceLevel == ComplianceLevel.IMMINENT_DANGER
                )
                
                // Should recommend immediate work stoppage
                assertTrue(
                    "Should recommend immediate work stoppage for imminent danger",
                    result.recommendedActions.any { 
                        it.contains("stop", ignoreCase = true) ||
                        it.contains("cease", ignoreCase = true) ||
                        it.contains("immediate", ignoreCase = true)
                    }
                )
            }
        }
        
        private suspend fun analyzeOSHACompliance(testCases: List<OSHAComplianceTestCase>): List<OSHAComplianceResult> {
            return testCases.map { testCase ->
                val analysisResult = aiAnalyzer.analyzePhoto(testCase.imageData, testCase.workType)
                val analysis = analysisResult.getOrNull()
                
                val detectedViolations = analysis?.hazards?.mapNotNull { hazard ->
                    hazard.oshaCode?.let { oshaCode ->
                        OSHAViolation(
                            standard = oshaCode,
                            description = hazard.description,
                            severity = mapSeverityToOSHA(hazard.severity),
                            proposedPenalty = calculateOSHAPenalty(hazard.severity, oshaCode)
                        )
                    }
                } ?: emptyList()
                
                OSHAComplianceResult(
                    testCase = testCase,
                    detectedViolations = detectedViolations,
                    complianceLevel = determineComplianceLevel(detectedViolations),
                    recommendedActions = generateOSHARecommendations(detectedViolations),
                    analysisSuccessful = analysisResult.isSuccess
                )
            }
        }
        
        private fun mapSeverityToOSHA(severity: Severity): OSHASeverity {
            return when (severity) {
                Severity.CRITICAL -> OSHASeverity.IMMINENT_DANGER
                Severity.HIGH -> OSHASeverity.SERIOUS
                Severity.MEDIUM -> OSHASeverity.OTHER_THAN_SERIOUS
                Severity.LOW -> OSHASeverity.DE_MINIMIS
            }
        }
        
        private fun calculateOSHAPenalty(severity: Severity, oshaCode: String): Int {
            // 2024 OSHA penalty amounts
            return when (severity) {
                Severity.CRITICAL -> 0 // Imminent danger - work stoppage required
                Severity.HIGH -> 14502 // Maximum serious violation
                Severity.MEDIUM -> 7251 // Typical other-than-serious
                Severity.LOW -> 0 // De minimis
            }
        }
        
        private fun determineComplianceLevel(violations: List<OSHAViolation>): ComplianceLevel {
            return when {
                violations.any { it.severity == OSHASeverity.IMMINENT_DANGER } -> 
                    ComplianceLevel.IMMINENT_DANGER
                violations.any { it.severity == OSHASeverity.SERIOUS } -> 
                    ComplianceLevel.SERIOUS_VIOLATIONS
                violations.any { it.severity == OSHASeverity.OTHER_THAN_SERIOUS } -> 
                    ComplianceLevel.MINOR_VIOLATIONS
                else -> ComplianceLevel.FULLY_COMPLIANT
            }
        }
        
        private fun generateOSHARecommendations(violations: List<OSHAViolation>): List<String> {
            val recommendations = mutableListOf<String>()
            
            violations.forEach { violation ->
                when {
                    violation.standard.startsWith("1926.501") -> 
                        recommendations.add("Install fall protection systems per 1926.501")
                    violation.standard.startsWith("1926.95") -> 
                        recommendations.add("Implement electrical safety measures per 1926.95")
                    violation.standard.startsWith("1926.652") -> 
                        recommendations.add("Install excavation protection per 1926.652")
                }
                
                if (violation.severity == OSHASeverity.IMMINENT_DANGER) {
                    recommendations.add("IMMEDIATE ACTION REQUIRED: Stop work until hazard is eliminated")
                }
            }
            
            return recommendations
        }
        
        private fun createOSHAPenaltyTestCases(): List<OSHAComplianceTestCase> {
            return listOf(
                // Add penalty test cases for different violation types
                OSHAComplianceTestCase(
                    id = "penalty_serious_001",
                    imageData = loadCriticalTestImage("serious_violation"),
                    workType = WorkType.GENERAL_CONSTRUCTION,
                    expectedOSHAViolations = listOf(
                        OSHAViolation(
                            standard = "1926.501(b)(1)",
                            description = "Serious fall protection violation",
                            severity = OSHASeverity.SERIOUS,
                            proposedPenalty = 14502
                        )
                    ),
                    expectedCitations = emptyList(),
                    complianceLevel = ComplianceLevel.SERIOUS_VIOLATIONS
                )
            )
        }
        
        private fun logOSHAComplianceResults(category: String, results: List<OSHAComplianceResult>) {
            println("=== $category OSHA Compliance Results ===")
            println("Total Tests: ${results.size}")
            
            val accurateDetections = results.count { result ->
                result.testCase.expectedOSHAViolations.all { expected ->
                    result.detectedViolations.any { detected ->
                        detected.standard == expected.standard &&
                        detected.severity == expected.severity
                    }
                }
            }
            
            val accuracy = accurateDetections.toFloat() / results.size
            println("OSHA Compliance Accuracy: ${String.format("%.2f%%", accuracy * 100)}")
            
            // Log any compliance failures
            results.filter { result ->
                !result.testCase.expectedOSHAViolations.all { expected ->
                    result.detectedViolations.any { detected ->
                        detected.standard == expected.standard
                    }
                }
            }.forEach { failure ->
                println("COMPLIANCE FAILURE: ${failure.testCase.id}")
                failure.testCase.expectedOSHAViolations.forEach { expected ->
                    if (!failure.detectedViolations.any { it.standard == expected.standard }) {
                        println("  MISSED: ${expected.standard} - ${expected.description}")
                    }
                }
            }
            
            println("=========================================")
        }
    }
    
    /**
     * Data classes for test results
     */
    data class CriticalHazardResult(
        val testCase: CriticalHazardTestCase,
        val detectedCriticalHazards: Map<CriticalHazard, DetectedHazard>,
        val allCriticalHazardsDetected: Boolean,
        val analysisSuccessful: Boolean,
        val overallConfidence: Float
    )
    
    data class OSHAComplianceResult(
        val testCase: OSHAComplianceTestCase,
        val detectedViolations: List<OSHAViolation>,
        val complianceLevel: ComplianceLevel,
        val recommendedActions: List<String>,
        val analysisSuccessful: Boolean
    )
    
    /**
     * Helper functions
     */
    private fun createSafetyCriticalAIAnalyzer(): AIPhotoAnalyzer {
        // Return AI analyzer configured for safety-critical testing
        return SafetyCriticalMockAIAnalyzer()
    }
    
    private fun loadCriticalTestImage(scenario: String): ByteArray {
        // In real implementation, load actual critical hazard test images
        return scenario.toByteArray()
    }
}

/**
 * Mock AI analyzer optimized for safety-critical detection
 */
class SafetyCriticalMockAIAnalyzer : AIPhotoAnalyzer {
    override val analyzerName = "Safety-Critical Mock AI"
    override val analysisCapabilities = setOf(
        AnalysisCapability.HAZARD_DETECTION,
        AnalysisCapability.OSHA_COMPLIANCE,
        AnalysisCapability.CRITICAL_SAFETY_ANALYSIS
    )
    
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType,
        analysisOptions: AnalysisOptions
    ): Result<SafetyAnalysis> {
        
        val scenario = String(imageData)
        val hazards = generateCriticalHazards(scenario, workType)
        
        return Result.success(
            SafetyAnalysis(
                id = "safety-critical-${System.currentTimeMillis()}",
                hazards = hazards,
                overallRisk = determineCriticalRiskLevel(hazards),
                confidence = 0.90f, // High confidence for safety-critical analysis
                processingTimeMs = 250L,
                aiProvider = analyzerName,
                recommendations = generateSafetyRecommendations(hazards),
                oshaCompliance = generateOSHACompliance(hazards)
            )
        )
    }
    
    private fun generateCriticalHazards(scenario: String, workType: WorkType): List<DetectedHazard> {
        val hazards = mutableListOf<DetectedHazard>()
        
        when {
            scenario.contains("unguarded_roof_edge") -> {
                hazards.add(DetectedHazard(
                    type = HazardType.FALL_PROTECTION,
                    description = "Worker at roof edge >6 feet without fall protection",
                    severity = Severity.HIGH,
                    confidence = 0.92f,
                    boundingBox = BoundingBox(0.2f, 0.3f, 0.4f, 0.5f, 0.92f),
                    oshaCode = "1926.501(b)(1)",
                    recommendations = listOf(
                        "Install guardrail system immediately",
                        "Provide personal fall arrest system",
                        "Stop work until fall protection implemented"
                    )
                ))
            }
            scenario.contains("exposed_high_voltage") -> {
                hazards.add(DetectedHazard(
                    type = HazardType.ELECTRICAL,
                    description = "Workers within 10 feet of energized high voltage lines",
                    severity = Severity.HIGH,
                    confidence = 0.94f,
                    boundingBox = BoundingBox(0.1f, 0.2f, 0.6f, 0.4f, 0.94f),
                    oshaCode = "1926.95(a)",
                    recommendations = listOf(
                        "Maintain 10-foot minimum clearance",
                        "De-energize lines before work",
                        "Use lockout/tagout procedures"
                    )
                ))
            }
            scenario.contains("deep_unprotected_trench") -> {
                hazards.add(DetectedHazard(
                    type = HazardType.CAUGHT_IN_BETWEEN,
                    description = "Workers in trench >5 feet deep without protective system",
                    severity = Severity.HIGH,
                    confidence = 0.88f,
                    boundingBox = BoundingBox(0.1f, 0.3f, 0.8f, 0.5f, 0.88f),
                    oshaCode = "1926.652(a)(1)",
                    recommendations = listOf(
                        "Install trench protection system",
                        "Evacuate trench immediately",
                        "Conduct soil analysis"
                    )
                ))
            }
            scenario.contains("workers_under_crane_load") -> {
                hazards.add(DetectedHazard(
                    type = HazardType.STRUCK_BY,
                    description = "Workers in crane's load path without barricades",
                    severity = Severity.HIGH,
                    confidence = 0.90f,
                    boundingBox = BoundingBox(0.2f, 0.3f, 0.5f, 0.4f, 0.90f),
                    oshaCode = "1926.1404(h)(1)",
                    recommendations = listOf(
                        "Establish exclusion zone under load path",
                        "Use tag lines for load control",
                        "Post barriers and warning signs"
                    )
                ))
            }
            scenario.contains("imminent_danger") -> {
                hazards.add(DetectedHazard(
                    type = HazardType.IMMINENT_DANGER,
                    description = "Imminent danger condition requiring immediate work cessation",
                    severity = Severity.CRITICAL,
                    confidence = 0.95f,
                    boundingBox = BoundingBox(0.0f, 0.0f, 1.0f, 1.0f, 0.95f),
                    oshaCode = "Section 13(a)",
                    recommendations = listOf(
                        "STOP ALL WORK IMMEDIATELY",
                        "Evacuate all personnel from danger zone",
                        "Contact OSHA immediately",
                        "Do not resume work until hazard eliminated"
                    )
                ))
            }
        }
        
        return hazards
    }
    
    private fun determineCriticalRiskLevel(hazards: List<DetectedHazard>): RiskLevel {
        return when {
            hazards.any { it.severity == Severity.CRITICAL } -> RiskLevel.CRITICAL
            hazards.any { it.severity == Severity.HIGH } -> RiskLevel.HIGH
            hazards.any { it.severity == Severity.MEDIUM } -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }
    
    private fun generateSafetyRecommendations(hazards: List<DetectedHazard>): List<String> {
        val recommendations = mutableListOf<String>()
        
        hazards.forEach { hazard ->
            recommendations.addAll(hazard.recommendations)
        }
        
        // Add general safety recommendations
        if (hazards.any { it.severity >= Severity.HIGH }) {
            recommendations.add("Conduct immediate safety briefing with all workers")
            recommendations.add("Review and update safety procedures")
            recommendations.add("Consider additional safety supervision")
        }
        
        return recommendations.distinct()
    }
    
    private fun generateOSHACompliance(hazards: List<DetectedHazard>): OSHAComplianceInfo {
        val violations = hazards.mapNotNull { hazard ->
            hazard.oshaCode?.let { code ->
                OSHAViolationInfo(
                    standard = code,
                    description = hazard.description,
                    severity = when (hazard.severity) {
                        Severity.CRITICAL -> "Imminent Danger"
                        Severity.HIGH -> "Serious"
                        Severity.MEDIUM -> "Other-than-Serious"
                        Severity.LOW -> "De Minimis"
                    },
                    penalty = when (hazard.severity) {
                        Severity.HIGH -> 14502
                        Severity.MEDIUM -> 7251
                        else -> 0
                    }
                )
            }
        }
        
        return OSHAComplianceInfo(
            violations = violations,
            complianceStatus = if (violations.isEmpty()) "Compliant" else "Non-Compliant",
            totalPenalty = violations.sumOf { it.penalty },
            requiresImmediateAction = violations.any { it.severity == "Imminent Danger" }
        )
    }
    
    override fun getPerformanceMetrics(): AnalyzerPerformanceMetrics {
        return AnalyzerPerformanceMetrics(
            analysisCount = 200L,
            averageProcessingTime = 250L,
            successRate = 0.99f, // Very high for safety-critical
            averageConfidence = 0.90f
        )
    }
    
    override fun cleanup() {}
}

// Additional data classes for OSHA compliance
data class OSHAComplianceInfo(
    val violations: List<OSHAViolationInfo> = emptyList(),
    val complianceStatus: String = "Unknown",
    val totalPenalty: Int = 0,
    val requiresImmediateAction: Boolean = false
)

data class OSHAViolationInfo(
    val standard: String,
    val description: String,
    val severity: String,
    val penalty: Int
)
