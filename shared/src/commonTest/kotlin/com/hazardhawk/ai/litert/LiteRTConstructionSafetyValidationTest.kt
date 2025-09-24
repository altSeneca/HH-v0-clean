package com.hazardhawk.ai.litert

import com.hazardhawk.TestUtils
import com.hazardhawk.core.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Comprehensive construction safety validation tests for LiteRT AI analysis.
 * Validates OSHA compliance detection, accuracy of safety assessments,
 * and construction-specific hazard identification across work types.
 */
class LiteRTConstructionSafetyValidationTest {
    
    private lateinit var mockEngine: MockLiteRTModelEngine
    private lateinit var testDataFactory: LiteRTTestDataFactory
    private lateinit var oshaValidator: OSHAComplianceValidator
    private lateinit var safetyMetrics: SafetyValidationMetrics
    
    @BeforeTest
    fun setup() {
        mockEngine = MockLiteRTModelEngine()
        testDataFactory = LiteRTTestDataFactory()
        oshaValidator = OSHAComplianceValidator()
        safetyMetrics = SafetyValidationMetrics()
        
        // Initialize with high-end configuration for accurate testing
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 8f,
            supportedBackends = setOf(
                LiteRTBackend.CPU,
                LiteRTBackend.GPU_OPENCL,
                LiteRTBackend.NPU_QTI_HTP
            )
        )
    }
    
    @AfterTest
    fun tearDown() {
        mockEngine.cleanup()
        printSafetyValidationSummary()
    }
    
    // =====================================================
    // PPE COMPLIANCE VALIDATION TESTS
    // =====================================================
    
    @Test
    fun `test hard hat detection accuracy across scenarios`() = runTest {
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val hardHatScenarios = listOf(
            PPEScenario(
                name = "Worker with proper hard hat",
                imageData = testDataFactory.createPPECompliantImage(),
                expectedDetection = PPEDetection(detected = true, confidence = 0.9f),
                ppeType = PPEType.HARD_HAT,
                workContext = WorkType.GENERAL_CONSTRUCTION
            ),
            PPEScenario(
                name = "Worker without hard hat - clear violation",
                imageData = testDataFactory.createMissingHardHatImage(),
                expectedDetection = PPEDetection(detected = false, confidence = 0.95f),
                ppeType = PPEType.HARD_HAT,
                workContext = WorkType.GENERAL_CONSTRUCTION,
                expectedOSHAViolation = "1926.95"
            ),
            PPEScenario(
                name = "Hard hat worn incorrectly (backwards/loose)",
                imageData = testDataFactory.createImproperPPEUsageImage(PPEType.HARD_HAT),
                expectedDetection = PPEDetection(detected = true, confidence = 0.7f),
                ppeType = PPEType.HARD_HAT,
                workContext = WorkType.ELECTRICAL,
                expectedWarning = "Improper PPE usage detected"
            ),
            PPEScenario(
                name = "Multiple workers - mixed compliance",
                imageData = testDataFactory.createMixedPPEComplianceImage(),
                expectedDetection = PPEDetection(detected = true, confidence = 0.8f),
                ppeType = PPEType.HARD_HAT,
                workContext = WorkType.GENERAL_CONSTRUCTION,
                expectedHazardCount = 1 // One non-compliant worker
            )
        )
        
        testPPEScenarios(hardHatScenarios, "Hard Hat Detection")
    }
    
    @Test
    fun `test high-visibility vest detection accuracy`() = runTest {
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val vestScenarios = listOf(
            PPEScenario(
                name = "Proper high-vis vest in daylight",
                imageData = testDataFactory.createHighVisVestImage(lighting = LightingCondition.DAYLIGHT),
                expectedDetection = PPEDetection(detected = true, confidence = 0.92f),
                ppeType = PPEType.SAFETY_VEST,
                workContext = WorkType.ROADWORK
            ),
            PPEScenario(
                name = "High-vis vest in low light conditions",
                imageData = testDataFactory.createHighVisVestImage(lighting = LightingCondition.LOW_LIGHT),
                expectedDetection = PPEDetection(detected = true, confidence = 0.85f),
                ppeType = PPEType.SAFETY_VEST,
                workContext = WorkType.ROADWORK
            ),
            PPEScenario(
                name = "Worker without high-vis vest near traffic",
                imageData = testDataFactory.createTrafficAreaWithoutVestImage(),
                expectedDetection = PPEDetection(detected = false, confidence = 0.9f),
                ppeType = PPEType.SAFETY_VEST,
                workContext = WorkType.ROADWORK,
                expectedOSHAViolation = "1926.95",
                expectedSeverity = Severity.HIGH // High risk near traffic
            ),
            PPEScenario(
                name = "Faded/dirty high-vis vest - reduced visibility",
                imageData = testDataFactory.createFadedVestImage(),
                expectedDetection = PPEDetection(detected = true, confidence = 0.6f),
                ppeType = PPEType.SAFETY_VEST,
                workContext = WorkType.ROADWORK,
                expectedWarning = "PPE condition may be inadequate"
            )
        )
        
        testPPEScenarios(vestScenarios, "High-Visibility Vest Detection")
    }
    
    @Test
    fun `test safety harness detection for fall protection`() = runTest {
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val harnessScenarios = listOf(
            PPEScenario(
                name = "Proper full-body harness with lanyard",
                imageData = testDataFactory.createProperHarnessImage(),
                expectedDetection = PPEDetection(detected = true, confidence = 0.88f),
                ppeType = PPEType.SAFETY_HARNESS,
                workContext = WorkType.ROOFING
            ),
            PPEScenario(
                name = "Worker at height without fall protection",
                imageData = testDataFactory.createHeightWorkWithoutHarnessImage(),
                expectedDetection = PPEDetection(detected = false, confidence = 0.92f),
                ppeType = PPEType.SAFETY_HARNESS,
                workContext = WorkType.ROOFING,
                expectedOSHAViolation = "1926.501",
                expectedSeverity = Severity.HIGH
            ),
            PPEScenario(
                name = "Harness worn but not connected",
                imageData = testDataFactory.createUnconnectedHarnessImage(),
                expectedDetection = PPEDetection(detected = true, confidence = 0.75f),
                ppeType = PPEType.SAFETY_HARNESS,
                workContext = WorkType.SCAFFOLDING,
                expectedWarning = "Fall protection not properly connected"
            )
        )
        
        testPPEScenarios(harnessScenarios, "Safety Harness Detection")
    }
    
    // =====================================================
    // HAZARD DETECTION VALIDATION TESTS
    // =====================================================
    
    @Test
    fun `test fall hazard detection accuracy`() = runTest {
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val fallHazardScenarios = listOf(
            HazardScenario(
                name = "Unguarded roof edge above 6 feet",
                imageData = testDataFactory.createUnguardedRoofEdgeImage(),
                expectedHazardType = HazardType.FALL,
                expectedSeverity = Severity.HIGH,
                expectedConfidence = 0.9f,
                expectedOSHACodes = listOf("1926.501"),
                workContext = WorkType.ROOFING,
                expectedRecommendations = listOf("Install guardrail systems", "Use personal fall arrest systems")
            ),
            HazardScenario(
                name = "Unprotected floor opening",
                imageData = testDataFactory.createFloorOpeningImage(),
                expectedHazardType = HazardType.FALL,
                expectedSeverity = Severity.HIGH,
                expectedConfidence = 0.85f,
                expectedOSHACodes = listOf("1926.501"),
                workContext = WorkType.GENERAL_CONSTRUCTION,
                expectedRecommendations = listOf("Cover or guard floor openings", "Install warning signage")
            ),
            HazardScenario(
                name = "Damaged scaffolding platform",
                imageData = testDataFactory.createDamagedScaffoldingImage(),
                expectedHazardType = HazardType.FALL,
                expectedSeverity = Severity.HIGH,
                expectedConfidence = 0.88f,
                expectedOSHACodes = listOf("1926.451"),
                workContext = WorkType.SCAFFOLDING,
                expectedRecommendations = listOf("Repair or replace damaged components", "Restrict access until repaired")
            ),
            HazardScenario(
                name = "Ladder positioned unsafely",
                imageData = testDataFactory.createUnsafeLadderImage(),
                expectedHazardType = HazardType.FALL,
                expectedSeverity = Severity.MEDIUM,
                expectedConfidence = 0.82f,
                expectedOSHACodes = listOf("1926.1053"),
                workContext = WorkType.GENERAL_CONSTRUCTION,
                expectedRecommendations = listOf("Follow 4:1 rule for ladder angle", "Secure ladder at top and bottom")
            )
        )
        
        testHazardScenarios(fallHazardScenarios, "Fall Hazard Detection")
    }
    
    @Test
    fun `test electrical hazard detection accuracy`() = runTest {
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val electricalHazardScenarios = listOf(
            HazardScenario(
                name = "Exposed live electrical panel",
                imageData = testDataFactory.createExposedElectricalPanelImage(),
                expectedHazardType = HazardType.ELECTRICAL,
                expectedSeverity = Severity.HIGH,
                expectedConfidence = 0.93f,
                expectedOSHACodes = listOf("1926.416"),
                workContext = WorkType.ELECTRICAL,
                expectedRecommendations = listOf("De-energize before work", "Install proper panel covers")
            ),
            HazardScenario(
                name = "Power lines within crane operating radius",
                imageData = testDataFactory.createPowerLineProximityImage(),
                expectedHazardType = HazardType.ELECTRICAL,
                expectedSeverity = Severity.HIGH,
                expectedConfidence = 0.88f,
                expectedOSHACodes = listOf("1926.1408"),
                workContext = WorkType.CRANE_OPERATION,
                expectedRecommendations = listOf("Maintain 20-foot clearance", "Coordinate with utility company")
            ),
            HazardScenario(
                name = "Damaged extension cord in wet conditions",
                imageData = testDataFactory.createWetElectricalHazardImage(),
                expectedHazardType = HazardType.ELECTRICAL,
                expectedSeverity = Severity.HIGH,
                expectedConfidence = 0.85f,
                expectedOSHACodes = listOf("1926.405"),
                workContext = WorkType.GENERAL_CONSTRUCTION,
                expectedRecommendations = listOf("Use GFCI protection", "Replace damaged equipment")
            )
        )
        
        testHazardScenarios(electricalHazardScenarios, "Electrical Hazard Detection")
    }
    
    @Test
    fun `test struck-by hazard detection accuracy`() = runTest {
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val struckByScenarios = listOf(
            HazardScenario(
                name = "Heavy equipment operating near workers",
                imageData = testDataFactory.createEquipmentProximityImage(),
                expectedHazardType = HazardType.STRUCK_BY,
                expectedSeverity = Severity.MEDIUM,
                expectedConfidence = 0.82f,
                expectedOSHACodes = listOf("1926.95"),
                workContext = WorkType.EXCAVATION,
                expectedRecommendations = listOf("Establish exclusion zones", "Use spotters")
            ),
            HazardScenario(
                name = "Unsecured materials at height",
                imageData = testDataFactory.createFallingObjectHazardImage(),
                expectedHazardType = HazardType.STRUCK_BY,
                expectedSeverity = Severity.HIGH,
                expectedConfidence = 0.88f,
                expectedOSHACodes = listOf("1926.95"),
                workContext = WorkType.HIGH_RISE,
                expectedRecommendations = listOf("Secure all materials", "Install toe boards and screens")
            ),
            HazardScenario(
                name = "Crane load swing path through work area",
                imageData = testDataFactory.createCraneSwingPathImage(),
                expectedHazardType = HazardType.STRUCK_BY,
                expectedSeverity = Severity.HIGH,
                expectedConfidence = 0.9f,
                expectedOSHACodes = listOf("1926.1424"),
                workContext = WorkType.CRANE_OPERATION,
                expectedRecommendations = listOf("Clear swing path", "Use taglines for load control")
            )
        )
        
        testHazardScenarios(struckByScenarios, "Struck-By Hazard Detection")
    }
    
    @Test
    fun `test caught-in/between hazard detection accuracy`() = runTest {
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val caughtInScenarios = listOf(
            HazardScenario(
                name = "Excavation without proper sloping/shoring",
                imageData = testDataFactory.createUnsafeExcavationImage(),
                expectedHazardType = HazardType.CAUGHT_IN,
                expectedSeverity = Severity.HIGH,
                expectedConfidence = 0.9f,
                expectedOSHACodes = listOf("1926.651"),
                workContext = WorkType.EXCAVATION,
                expectedRecommendations = listOf("Install protective systems", "Classify soil type properly")
            ),
            HazardScenario(
                name = "Unguarded machinery with rotating parts",
                imageData = testDataFactory.createUnguardedMachineryImage(),
                expectedHazardType = HazardType.CAUGHT_IN,
                expectedSeverity = Severity.HIGH,
                expectedConfidence = 0.85f,
                expectedOSHACodes = listOf("1926.300"),
                workContext = WorkType.MANUFACTURING,
                expectedRecommendations = listOf("Install machine guards", "Implement LOTO procedures")
            ),
            HazardScenario(
                name = "Trench collapse risk - workers in proximity",
                imageData = testDataFactory.createTrenchCollapseRiskImage(),
                expectedHazardType = HazardType.CAUGHT_IN,
                expectedSeverity = Severity.HIGH,
                expectedConfidence = 0.92f,
                expectedOSHACodes = listOf("1926.651"),
                workContext = WorkType.EXCAVATION,
                expectedRecommendations = listOf("Exit trenches immediately", "Install proper protective systems")
            )
        )
        
        testHazardScenarios(caughtInScenarios, "Caught-In/Between Hazard Detection")
    }
    
    // =====================================================
    // OSHA COMPLIANCE VALIDATION TESTS
    // =====================================================
    
    @Test
    fun `test OSHA 1926 Subpart M fall protection compliance`() = runTest {
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val fallProtectionCompliance = listOf(
            OSHAComplianceScenario(
                regulation = "1926.501(b)(1)",
                description = "Fall protection required at 6 feet",
                testImage = testDataFactory.createSixFootHeightWorkImage(),
                workType = WorkType.GENERAL_CONSTRUCTION,
                expectedCompliance = false,
                expectedViolationSeverity = Severity.HIGH,
                expectedCitations = listOf("Failure to provide fall protection at 6+ feet")
            ),
            OSHAComplianceScenario(
                regulation = "1926.502(d)",
                description = "Personal fall arrest system requirements",
                testImage = testDataFactory.createFallArrestSystemImage(),
                workType = WorkType.ROOFING,
                expectedCompliance = true,
                expectedViolationSeverity = null,
                expectedCitations = emptyList()
            ),
            OSHAComplianceScenario(
                regulation = "1926.501(b)(4)",
                description = "Hole covering requirements",
                testImage = testDataFactory.createUncoveredHoleImage(),
                workType = WorkType.GENERAL_CONSTRUCTION,
                expectedCompliance = false,
                expectedViolationSeverity = Severity.HIGH,
                expectedCitations = listOf("Unguarded or uncovered holes")
            )
        )
        
        testOSHACompliance(fallProtectionCompliance, "Fall Protection (1926 Subpart M)")
    }
    
    @Test
    fun `test OSHA 1926 Subpart K electrical safety compliance`() = runTest {
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val electricalSafetyCompliance = listOf(
            OSHAComplianceScenario(
                regulation = "1926.416(a)(1)",
                description = "De-energization requirements",
                testImage = testDataFactory.createLiveElectricalWorkImage(),
                workType = WorkType.ELECTRICAL,
                expectedCompliance = false,
                expectedViolationSeverity = Severity.HIGH,
                expectedCitations = listOf("Work on energized equipment without proper procedures")
            ),
            OSHAComplianceScenario(
                regulation = "1926.405(g)(2)",
                description = "GFCI protection in wet locations",
                testImage = testDataFactory.createWetLocationWithoutGFCIImage(),
                workType = WorkType.GENERAL_CONSTRUCTION,
                expectedCompliance = false,
                expectedViolationSeverity = Severity.HIGH,
                expectedCitations = listOf("Missing GFCI protection in wet location")
            ),
            OSHAComplianceScenario(
                regulation = "1926.408(c)",
                description = "Electrical equipment grounding",
                testImage = testDataFactory.createImproperGroundingImage(),
                workType = WorkType.ELECTRICAL,
                expectedCompliance = false,
                expectedViolationSeverity = Severity.MEDIUM,
                expectedCitations = listOf("Improper or missing equipment grounding")
            )
        )
        
        testOSHACompliance(electricalSafetyCompliance, "Electrical Safety (1926 Subpart K)")
    }
    
    @Test
    fun `test OSHA 1926 Subpart P excavation compliance`() = runTest {
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val excavationCompliance = listOf(
            OSHAComplianceScenario(
                regulation = "1926.651(c)(2)",
                description = "Protective systems for 5+ foot excavations",
                testImage = testDataFactory.createDeepExcavationWithoutProtectionImage(),
                workType = WorkType.EXCAVATION,
                expectedCompliance = false,
                expectedViolationSeverity = Severity.HIGH,
                expectedCitations = listOf("Excavation 5+ feet without protective systems")
            ),
            OSHAComplianceScenario(
                regulation = "1926.651(j)(2)",
                description = "Safe means of egress",
                testImage = testDataFactory.createExcavationWithoutEgressImage(),
                workType = WorkType.EXCAVATION,
                expectedCompliance = false,
                expectedViolationSeverity = Severity.HIGH,
                expectedCitations = listOf("No safe means of egress within 25 feet")
            ),
            OSHAComplianceScenario(
                regulation = "1926.651(k)(1)",
                description = "Water accumulation control",
                testImage = testDataFactory.createWaterFilledExcavationImage(),
                workType = WorkType.EXCAVATION,
                expectedCompliance = false,
                expectedViolationSeverity = Severity.MEDIUM,
                expectedCitations = listOf("Inadequate water control in excavation")
            )
        )
        
        testOSHACompliance(excavationCompliance, "Excavation Safety (1926 Subpart P)")
    }
    
    // =====================================================
    // CONSTRUCTION-SPECIFIC ACCURACY TESTS
    // =====================================================
    
    @Test
    fun `test work type specific hazard prioritization`() = runTest {
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val workTypeScenarios = listOf(
            WorkTypeScenario(
                workType = WorkType.ROOFING,
                testImage = testDataFactory.createRoofingScenarioImage(),
                expectedPrimaryHazards = listOf(HazardType.FALL, HazardType.STRUCK_BY),
                expectedOSHAPriority = listOf("1926.501", "1926.95"),
                minimumConfidenceThreshold = 0.85f
            ),
            WorkTypeScenario(
                workType = WorkType.ELECTRICAL,
                testImage = testDataFactory.createElectricalScenarioImage(),
                expectedPrimaryHazards = listOf(HazardType.ELECTRICAL, HazardType.FALL),
                expectedOSHAPriority = listOf("1926.416", "1926.501"),
                minimumConfidenceThreshold = 0.88f
            ),
            WorkTypeScenario(
                workType = WorkType.EXCAVATION,
                testImage = testDataFactory.createExcavationScenarioImage(),
                expectedPrimaryHazards = listOf(HazardType.CAUGHT_IN, HazardType.STRUCK_BY),
                expectedOSHAPriority = listOf("1926.651", "1926.95"),
                minimumConfidenceThreshold = 0.82f
            ),
            WorkTypeScenario(
                workType = WorkType.SCAFFOLDING,
                testImage = testDataFactory.createScaffoldingScenarioImage(),
                expectedPrimaryHazards = listOf(HazardType.FALL, HazardType.STRUCK_BY),
                expectedOSHAPriority = listOf("1926.451", "1926.95"),
                minimumConfidenceThreshold = 0.85f
            )
        )
        
        testWorkTypeSpecificScenarios(workTypeScenarios)
    }
    
    @Test
    fun `test construction site context awareness`() = runTest {
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val contextualScenarios = listOf(
            ContextualScenario(
                name = "High-rise construction site",
                imageData = testDataFactory.createHighRiseConstructionImage(),
                workType = WorkType.HIGH_RISE,
                expectedContextualFactors = listOf("Height exposure", "Weather exposure", "Material hoisting"),
                expectedAdditionalPPE = listOf(PPEType.SAFETY_HARNESS, PPEType.HARD_HAT, PPEType.SAFETY_BOOTS),
                expectedEnvironmentalConsiderations = listOf("Wind conditions", "Weather protection")
            ),
            ContextualScenario(
                name = "Confined space construction",
                imageData = testDataFactory.createConfinedSpaceImage(),
                workType = WorkType.CONFINED_SPACE,
                expectedContextualFactors = listOf("Atmospheric hazards", "Ventilation", "Entry/exit procedures"),
                expectedAdditionalPPE = listOf(PPEType.RESPIRATORY_PROTECTION, PPEType.GAS_MONITOR),
                expectedEnvironmentalConsiderations = listOf("Air quality monitoring", "Emergency rescue procedures")
            ),
            ContextualScenario(
                name = "Road construction work zone",
                imageData = testDataFactory.createRoadConstructionImage(),
                workType = WorkType.ROADWORK,
                expectedContextualFactors = listOf("Traffic proximity", "Visibility", "Vehicle barriers"),
                expectedAdditionalPPE = listOf(PPEType.SAFETY_VEST, PPEType.HARD_HAT),
                expectedEnvironmentalConsiderations = listOf("Traffic control", "Flagging operations")
            )
        )
        
        testContextualAwareness(contextualScenarios)
    }
    
    // =====================================================
    // HELPER TEST METHODS
    // =====================================================
    
    private suspend fun testPPEScenarios(scenarios: List<PPEScenario>, categoryName: String) {
        println("\n=== Testing $categoryName ===")
        
        scenarios.forEach { scenario ->
            val result = mockEngine.generateSafetyAnalysis(
                imageData = scenario.imageData,
                workType = scenario.workContext,
                includeOSHACodes = true,
                confidenceThreshold = 0.6f
            )
            
            assertTrue(result.isSuccess, "${scenario.name}: Analysis should succeed")
            
            val analysis = result.getOrNull()!!
            val actualDetection = analysis.ppeStatus[scenario.ppeType]
            
            assertNotNull(actualDetection, "${scenario.name}: Should detect ${scenario.ppeType}")
            
            assertEquals(
                scenario.expectedDetection.detected,
                actualDetection.detected,
                "${scenario.name}: PPE detection status should match expectation"
            )
            
            assertTrue(
                actualDetection.confidence >= (scenario.expectedDetection.confidence - 0.15f),
                "${scenario.name}: Confidence ${actualDetection.confidence} should be within range of ${scenario.expectedDetection.confidence}"
            )
            
            // Validate OSHA violations if expected
            scenario.expectedOSHAViolation?.let { expectedCode ->
                val hasExpectedViolation = analysis.oshaViolations.any { 
                    it.regulationCode.contains(expectedCode) 
                }
                assertTrue(
                    hasExpectedViolation,
                    "${scenario.name}: Should detect OSHA violation $expectedCode"
                )
            }
            
            safetyMetrics.recordPPETest(scenario, actualDetection, analysis)
        }
    }
    
    private suspend fun testHazardScenarios(scenarios: List<HazardScenario>, categoryName: String) {
        println("\n=== Testing $categoryName ===")
        
        scenarios.forEach { scenario ->
            val result = mockEngine.generateSafetyAnalysis(
                imageData = scenario.imageData,
                workType = scenario.workContext,
                includeOSHACodes = true,
                confidenceThreshold = 0.6f
            )
            
            assertTrue(result.isSuccess, "${scenario.name}: Analysis should succeed")
            
            val analysis = result.getOrNull()!!
            
            // Find hazards of expected type
            val matchingHazards = analysis.hazards.filter { it.type == scenario.expectedHazardType }
            assertTrue(
                matchingHazards.isNotEmpty(),
                "${scenario.name}: Should detect ${scenario.expectedHazardType} hazard"
            )
            
            val bestHazard = matchingHazards.maxByOrNull { it.confidence }!!
            
            assertEquals(
                scenario.expectedSeverity,
                bestHazard.severity,
                "${scenario.name}: Hazard severity should match expectation"
            )
            
            assertTrue(
                bestHazard.confidence >= scenario.expectedConfidence - 0.1f,
                "${scenario.name}: Hazard confidence ${bestHazard.confidence} should be >= ${scenario.expectedConfidence - 0.1f}"
            )
            
            // Validate OSHA codes
            scenario.expectedOSHACodes.forEach { expectedCode ->
                val hasCode = analysis.oshaViolations.any { 
                    it.regulationCode.contains(expectedCode) 
                }
                assertTrue(
                    hasCode,
                    "${scenario.name}: Should include OSHA code $expectedCode"
                )
            }
            
            // Validate recommendations
            scenario.expectedRecommendations.forEach { expectedRec ->
                val hasRecommendation = bestHazard.recommendations.any { 
                    it.contains(expectedRec, ignoreCase = true) 
                } || analysis.recommendations.any { 
                    it.contains(expectedRec, ignoreCase = true) 
                }
                assertTrue(
                    hasRecommendation,
                    "${scenario.name}: Should include recommendation containing '$expectedRec'"
                )
            }
            
            safetyMetrics.recordHazardTest(scenario, bestHazard, analysis)
        }
    }
    
    private suspend fun testOSHACompliance(scenarios: List<OSHAComplianceScenario>, categoryName: String) {
        println("\n=== Testing $categoryName ===")
        
        scenarios.forEach { scenario ->
            val result = mockEngine.generateSafetyAnalysis(
                imageData = scenario.testImage,
                workType = scenario.workType,
                includeOSHACodes = true,
                confidenceThreshold = 0.6f
            )
            
            assertTrue(result.isSuccess, "${scenario.regulation}: Analysis should succeed")
            
            val analysis = result.getOrNull()!!
            val complianceResult = oshaValidator.validateCompliance(scenario.regulation, analysis)
            
            assertEquals(
                scenario.expectedCompliance,
                complianceResult.isCompliant,
                "${scenario.regulation}: Compliance status should match expectation"
            )
            
            if (!scenario.expectedCompliance) {
                assertNotNull(
                    scenario.expectedViolationSeverity,
                    "${scenario.regulation}: Should specify expected violation severity"
                )
                
                val relevantViolation = analysis.oshaViolations.find { 
                    it.regulationCode.contains(scenario.regulation) 
                }
                assertNotNull(
                    relevantViolation,
                    "${scenario.regulation}: Should detect specific OSHA violation"
                )
                
                assertEquals(
                    scenario.expectedViolationSeverity,
                    relevantViolation.severity,
                    "${scenario.regulation}: Violation severity should match expectation"
                )
            }
            
            safetyMetrics.recordOSHATest(scenario, complianceResult, analysis)
        }
    }
    
    private suspend fun testWorkTypeSpecificScenarios(scenarios: List<WorkTypeScenario>) {
        scenarios.forEach { scenario ->
            val result = mockEngine.generateSafetyAnalysis(
                imageData = scenario.testImage,
                workType = scenario.workType,
                includeOSHACodes = true,
                confidenceThreshold = 0.6f
            )
            
            assertTrue(result.isSuccess, "${scenario.workType}: Analysis should succeed")
            
            val analysis = result.getOrNull()!!
            
            // Validate primary hazards are detected
            scenario.expectedPrimaryHazards.forEach { expectedHazard ->
                val hasHazard = analysis.hazards.any { 
                    it.type == expectedHazard && it.confidence >= scenario.minimumConfidenceThreshold 
                }
                assertTrue(
                    hasHazard,
                    "${scenario.workType}: Should detect $expectedHazard with confidence >= ${scenario.minimumConfidenceThreshold}"
                )
            }
            
            // Validate OSHA priority codes
            scenario.expectedOSHAPriority.forEach { expectedCode ->
                val hasCode = analysis.oshaViolations.any { 
                    it.regulationCode.contains(expectedCode) 
                }
                assertTrue(
                    hasCode,
                    "${scenario.workType}: Should prioritize OSHA code $expectedCode"
                )
            }
            
            safetyMetrics.recordWorkTypeTest(scenario, analysis)
        }
    }
    
    private suspend fun testContextualAwareness(scenarios: List<ContextualScenario>) {
        scenarios.forEach { scenario ->
            val result = mockEngine.generateSafetyAnalysis(
                imageData = scenario.imageData,
                workType = scenario.workType,
                includeOSHACodes = true,
                confidenceThreshold = 0.6f
            )
            
            assertTrue(result.isSuccess, "${scenario.name}: Analysis should succeed")
            
            val analysis = result.getOrNull()!!
            
            // Validate contextual factors are considered
            scenario.expectedContextualFactors.forEach { factor ->
                val hasContextualAwareness = analysis.recommendations.any { 
                    it.contains(factor, ignoreCase = true) 
                } || analysis.hazards.any { hazard ->
                    hazard.description.contains(factor, ignoreCase = true) || 
                    hazard.recommendations.any { it.contains(factor, ignoreCase = true) }
                }
                assertTrue(
                    hasContextualAwareness,
                    "${scenario.name}: Should show awareness of contextual factor '$factor'"
                )
            }
            
            // Validate additional PPE requirements
            scenario.expectedAdditionalPPE.forEach { ppeType ->
                val ppeStatus = analysis.ppeStatus[ppeType]
                assertTrue(
                    ppeStatus != null,
                    "${scenario.name}: Should evaluate $ppeType for this context"
                )
            }
            
            safetyMetrics.recordContextualTest(scenario, analysis)
        }
    }
    
    private fun printSafetyValidationSummary() {
        println("\n" + "=".repeat(50))
        println("CONSTRUCTION SAFETY VALIDATION SUMMARY")
        println("=".repeat(50))
        
        val summary = safetyMetrics.generateSummary()
        
        println("PPE Detection Tests: ${summary.ppeTestCount}")
        println("  - Accuracy: ${summary.ppeAccuracy}%")
        println("  - False Positives: ${summary.ppeFalsePositives}")
        println("  - False Negatives: ${summary.ppeFalseNegatives}")
        
        println("\nHazard Detection Tests: ${summary.hazardTestCount}")
        println("  - Accuracy: ${summary.hazardAccuracy}%")
        println("  - Average Confidence: ${summary.averageHazardConfidence}")
        
        println("\nOSHA Compliance Tests: ${summary.oshaTestCount}")
        println("  - Compliance Detection Rate: ${summary.oshaComplianceDetectionRate}%")
        println("  - Regulatory Accuracy: ${summary.oshaRegulatoryAccuracy}%")
        
        println("\nWork Type Specialization: ${summary.workTypeTestCount}")
        println("  - Context Awareness Score: ${summary.contextAwarenessScore}%")
        
        println("\nOverall Safety Validation Score: ${summary.overallScore}%")
        
        if (summary.overallScore >= 90) {
            println("✅ PRODUCTION READY - Excellent safety validation performance")
        } else if (summary.overallScore >= 80) {
            println("⚠️ NEEDS IMPROVEMENT - Good safety validation with areas for enhancement")
        } else {
            println("❌ NOT PRODUCTION READY - Significant safety validation issues detected")
        }
        
        println("=".repeat(50))
    }
}

// =====================================================
// DATA CLASSES FOR SAFETY TESTING
// =====================================================

data class PPEScenario(
    val name: String,
    val imageData: ByteArray,
    val expectedDetection: PPEDetection,
    val ppeType: PPEType,
    val workContext: WorkType,
    val expectedOSHAViolation: String? = null,
    val expectedSeverity: Severity? = null,
    val expectedWarning: String? = null,
    val expectedHazardCount: Int? = null
)

data class HazardScenario(
    val name: String,
    val imageData: ByteArray,
    val expectedHazardType: HazardType,
    val expectedSeverity: Severity,
    val expectedConfidence: Float,
    val expectedOSHACodes: List<String>,
    val workContext: WorkType,
    val expectedRecommendations: List<String>
)

data class OSHAComplianceScenario(
    val regulation: String,
    val description: String,
    val testImage: ByteArray,
    val workType: WorkType,
    val expectedCompliance: Boolean,
    val expectedViolationSeverity: Severity?,
    val expectedCitations: List<String>
)

data class WorkTypeScenario(
    val workType: WorkType,
    val testImage: ByteArray,
    val expectedPrimaryHazards: List<HazardType>,
    val expectedOSHAPriority: List<String>,
    val minimumConfidenceThreshold: Float
)

data class ContextualScenario(
    val name: String,
    val imageData: ByteArray,
    val workType: WorkType,
    val expectedContextualFactors: List<String>,
    val expectedAdditionalPPE: List<PPEType>,
    val expectedEnvironmentalConsiderations: List<String>
)

enum class LightingCondition {
    DAYLIGHT, LOW_LIGHT, ARTIFICIAL, MIXED
}

// =====================================================
// SAFETY VALIDATION UTILITIES
// =====================================================

class OSHAComplianceValidator {
    fun validateCompliance(regulation: String, analysis: SafetyAnalysis): ComplianceResult {
        val isCompliant = !analysis.oshaViolations.any { 
            it.regulationCode.contains(regulation) 
        }
        
        return ComplianceResult(
            regulation = regulation,
            isCompliant = isCompliant,
            violations = analysis.oshaViolations.filter { it.regulationCode.contains(regulation) },
            recommendations = if (!isCompliant) getComplianceRecommendations(regulation) else emptyList()
        )
    }
    
    private fun getComplianceRecommendations(regulation: String): List<String> {
        return when {
            regulation.contains("1926.501") -> listOf(
                "Install fall protection systems",
                "Provide personal fall arrest equipment",
                "Implement fall protection training"
            )
            regulation.contains("1926.416") -> listOf(
                "De-energize equipment before work",
                "Implement lockout/tagout procedures", 
                "Use proper electrical PPE"
            )
            regulation.contains("1926.651") -> listOf(
                "Install protective systems",
                "Provide safe means of egress",
                "Classify soil conditions properly"
            )
            else -> listOf("Follow applicable OSHA regulations")
        }
    }
}

data class ComplianceResult(
    val regulation: String,
    val isCompliant: Boolean,
    val violations: List<OSHAViolation>,
    val recommendations: List<String>
)

class SafetyValidationMetrics {
    private val ppeTests = mutableListOf<PPETestResult>()
    private val hazardTests = mutableListOf<HazardTestResult>()
    private val oshaTests = mutableListOf<OSHATestResult>()
    private val workTypeTests = mutableListOf<WorkTypeTestResult>()
    private val contextualTests = mutableListOf<ContextualTestResult>()
    
    fun recordPPETest(scenario: PPEScenario, actualDetection: PPEDetection, analysis: SafetyAnalysis) {
        ppeTests.add(PPETestResult(
            scenario = scenario,
            actualDetection = actualDetection,
            correct = scenario.expectedDetection.detected == actualDetection.detected,
            confidenceMatch = kotlin.math.abs(scenario.expectedDetection.confidence - actualDetection.confidence) <= 0.15f
        ))
    }
    
    fun recordHazardTest(scenario: HazardScenario, detectedHazard: DetectedHazard, analysis: SafetyAnalysis) {
        hazardTests.add(HazardTestResult(
            scenario = scenario,
            detectedHazard = detectedHazard,
            correct = detectedHazard.type == scenario.expectedHazardType && 
                     detectedHazard.severity == scenario.expectedSeverity,
            confidenceMatch = detectedHazard.confidence >= scenario.expectedConfidence - 0.1f
        ))
    }
    
    fun recordOSHATest(scenario: OSHAComplianceScenario, result: ComplianceResult, analysis: SafetyAnalysis) {
        oshaTests.add(OSHATestResult(
            scenario = scenario,
            result = result,
            correct = result.isCompliant == scenario.expectedCompliance
        ))
    }
    
    fun recordWorkTypeTest(scenario: WorkTypeScenario, analysis: SafetyAnalysis) {
        val hazardDetectionScore = scenario.expectedPrimaryHazards.count { expectedHazard ->
            analysis.hazards.any { it.type == expectedHazard && it.confidence >= scenario.minimumConfidenceThreshold }
        }.toFloat() / scenario.expectedPrimaryHazards.size
        
        workTypeTests.add(WorkTypeTestResult(
            scenario = scenario,
            analysis = analysis,
            hazardDetectionScore = hazardDetectionScore
        ))
    }
    
    fun recordContextualTest(scenario: ContextualScenario, analysis: SafetyAnalysis) {
        val contextualAwarenessScore = scenario.expectedContextualFactors.count { factor ->
            analysis.recommendations.any { it.contains(factor, ignoreCase = true) } ||
            analysis.hazards.any { hazard ->
                hazard.description.contains(factor, ignoreCase = true) ||
                hazard.recommendations.any { it.contains(factor, ignoreCase = true) }
            }
        }.toFloat() / scenario.expectedContextualFactors.size
        
        contextualTests.add(ContextualTestResult(
            scenario = scenario,
            analysis = analysis,
            contextualAwarenessScore = contextualAwarenessScore
        ))
    }
    
    fun generateSummary(): SafetyValidationSummary {
        val ppeAccuracy = if (ppeTests.isNotEmpty()) {
            (ppeTests.count { it.correct }.toFloat() / ppeTests.size * 100)
        } else 0f
        
        val hazardAccuracy = if (hazardTests.isNotEmpty()) {
            (hazardTests.count { it.correct }.toFloat() / hazardTests.size * 100)
        } else 0f
        
        val oshaComplianceRate = if (oshaTests.isNotEmpty()) {
            (oshaTests.count { it.correct }.toFloat() / oshaTests.size * 100)
        } else 0f
        
        val contextAwarenessScore = if (contextualTests.isNotEmpty()) {
            (contextualTests.map { it.contextualAwarenessScore }.average().toFloat() * 100)
        } else 0f
        
        val overallScore = listOf(ppeAccuracy, hazardAccuracy, oshaComplianceRate, contextAwarenessScore)
            .filter { it > 0 }
            .average()
            .toFloat()
        
        return SafetyValidationSummary(
            ppeTestCount = ppeTests.size,
            ppeAccuracy = ppeAccuracy,
            ppeFalsePositives = ppeTests.count { !it.correct && it.actualDetection.detected },
            ppeFalseNegatives = ppeTests.count { !it.correct && !it.actualDetection.detected },
            hazardTestCount = hazardTests.size,
            hazardAccuracy = hazardAccuracy,
            averageHazardConfidence = if (hazardTests.isNotEmpty()) {
                hazardTests.map { it.detectedHazard.confidence }.average().toFloat()
            } else 0f,
            oshaTestCount = oshaTests.size,
            oshaComplianceDetectionRate = oshaComplianceRate,
            oshaRegulatoryAccuracy = oshaComplianceRate,
            workTypeTestCount = workTypeTests.size,
            contextAwarenessScore = contextAwarenessScore,
            overallScore = overallScore
        )
    }
}

data class PPETestResult(
    val scenario: PPEScenario,
    val actualDetection: PPEDetection,
    val correct: Boolean,
    val confidenceMatch: Boolean
)

data class HazardTestResult(
    val scenario: HazardScenario,
    val detectedHazard: DetectedHazard,
    val correct: Boolean,
    val confidenceMatch: Boolean
)

data class OSHATestResult(
    val scenario: OSHAComplianceScenario,
    val result: ComplianceResult,
    val correct: Boolean
)

data class WorkTypeTestResult(
    val scenario: WorkTypeScenario,
    val analysis: SafetyAnalysis,
    val hazardDetectionScore: Float
)

data class ContextualTestResult(
    val scenario: ContextualScenario,
    val analysis: SafetyAnalysis,
    val contextualAwarenessScore: Float
)

data class SafetyValidationSummary(
    val ppeTestCount: Int,
    val ppeAccuracy: Float,
    val ppeFalsePositives: Int,
    val ppeFalseNegatives: Int,
    val hazardTestCount: Int,
    val hazardAccuracy: Float,
    val averageHazardConfidence: Float,
    val oshaTestCount: Int,
    val oshaComplianceDetectionRate: Float,
    val oshaRegulatoryAccuracy: Float,
    val workTypeTestCount: Int,
    val contextAwarenessScore: Float,
    val overallScore: Float
)
