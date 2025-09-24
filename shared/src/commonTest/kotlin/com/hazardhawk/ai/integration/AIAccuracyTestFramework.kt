package com.hazardhawk.ai.integration

import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.core.models.*
import kotlin.test.*
import kotlinx.coroutines.test.runTest

/**
 * Comprehensive AI accuracy testing framework for hazard detection validation.
 * This framework provides structured testing of AI model accuracy against
 * ground truth data with specific focus on construction safety scenarios.
 */
class AIAccuracyTestFramework {
    
    /**
     * Test data representing known hazards with expected detection results
     */
    data class GroundTruthTestCase(
        val id: String,
        val imageData: ByteArray,
        val workType: WorkType,
        val filename: String,
        val category: String,
        val expectedHazards: List<ExpectedHazard>,
        val expectedPPE: Map<PPEType, Boolean>,
        val expectedOSHACodes: List<String>,
        val overallRiskLevel: RiskLevel,
        val description: String
    )
    
    data class ExpectedHazard(
        val type: HazardType,
        val severity: Severity,
        val confidence: Float,
        val boundingBox: BoundingBox?,
        val oshaCode: String?,
        val mustDetect: Boolean = false // Critical hazards that must never be missed
    )
    
    data class AccuracyMetrics(
        val totalTests: Int,
        val correctDetections: Int,
        val falsePositives: Int,
        val falseNegatives: Int,
        val accuracy: Float,
        val precision: Float,
        val recall: Float,
        val f1Score: Float,
        val criticalMissRate: Float // Rate of missed critical hazards
    )
    
    data class HazardTypeAccuracy(
        val hazardType: HazardType,
        val totalCases: Int,
        val correctDetections: Int,
        val falsePositives: Int,
        val falseNegatives: Int,
        val accuracy: Float
    )
    
    /**
     * Comprehensive accuracy test suite for all hazard types
     */
    class AIHazardDetectionAccuracyTest {
        
        private val testDataLoader = AITestDataLoader()
        private lateinit var aiAnalyzer: AIPhotoAnalyzer
        
        @BeforeTest
        fun setup() {
            aiAnalyzer = createTestAIAnalyzer()
        }
        
        @Test
        fun `test fall protection hazard detection accuracy`() = runTest {
            val fallProtectionCases = testDataLoader.loadGroundTruthData("fall-protection")
            val results = analyzeTestCases(fallProtectionCases)
            val metrics = calculateAccuracyMetrics(results)
            
            // Fall protection is critical - require high accuracy
            assertTrue(
                "Fall protection detection accuracy ${metrics.accuracy} below required 0.85",
                metrics.accuracy >= 0.85f
            )
            
            // No critical fall hazards should be missed
            assertEquals(
                "Critical fall hazards were missed",
                0f, metrics.criticalMissRate
            )
            
            logAccuracyResults("Fall Protection", metrics)
        }
        
        @Test
        fun `test PPE detection accuracy across different scenarios`() = runTest {
            val ppeTestCases = testDataLoader.loadGroundTruthData("ppe-detection")
            val results = analyzePPETestCases(ppeTestCases)
            
            // Test individual PPE types
            val hardHatAccuracy = calculatePPEAccuracy(results, PPEType.HARD_HAT)
            val safetyVestAccuracy = calculatePPEAccuracy(results, PPEType.SAFETY_VEST)
            val eyeProtectionAccuracy = calculatePPEAccuracy(results, PPEType.EYE_PROTECTION)
            
            assertTrue("Hard hat detection accuracy too low", hardHatAccuracy >= 0.80f)
            assertTrue("Safety vest detection accuracy too low", safetyVestAccuracy >= 0.75f)
            assertTrue("Eye protection detection accuracy too low", eyeProtectionAccuracy >= 0.70f)
            
            logPPEAccuracyResults(results)
        }
        
        @Test
        fun `test electrical hazard detection reliability`() = runTest {
            val electricalCases = testDataLoader.loadGroundTruthData("electrical-hazards")
            val results = analyzeTestCases(electricalCases)
            val metrics = calculateAccuracyMetrics(results)
            
            // Electrical hazards are life-threatening - require very high accuracy
            assertTrue(
                "Electrical hazard detection accuracy ${metrics.accuracy} below required 0.90",
                metrics.accuracy >= 0.90f
            )
            
            // Verify OSHA code assignment accuracy
            val oshaCodeAccuracy = validateOSHACodeAssignment(results, "1926.95")
            assertTrue("OSHA electrical code assignment accuracy too low", oshaCodeAccuracy >= 0.95f)
        }
        
        @Test
        fun `test struck-by hazard detection in equipment areas`() = runTest {
            val struckByHazards = testDataLoader.loadGroundTruthData("struck-by")
            val results = analyzeTestCases(struckByHazards)
            val metrics = calculateAccuracyMetrics(results)
            
            assertTrue(
                "Struck-by hazard detection accuracy ${metrics.accuracy} below required 0.80",
                metrics.accuracy >= 0.80f
            )
            
            // Verify detection of heavy machinery and moving equipment
            val machineryDetection = results.filter { result ->
                result.expectedHazards.any { it.type == HazardType.STRUCK_BY }
            }
            val machineryAccuracy = machineryDetection.count { it.wasDetectedCorrectly } / 
                                  machineryDetection.size.toFloat()
            assertTrue("Heavy machinery hazard detection too low", machineryAccuracy >= 0.85f)
        }
        
        @Test
        fun `test caught-in-between hazard detection`() = runTest {
            val caughtInCases = testDataLoader.loadGroundTruthData("caught-in-between")
            val results = analyzeTestCases(caughtInCases)
            val metrics = calculateAccuracyMetrics(results)
            
            assertTrue(
                "Caught-in-between hazard detection accuracy ${metrics.accuracy} below required 0.75",
                metrics.accuracy >= 0.75f
            )
        }
        
        @Test
        fun `test confidence threshold optimization`() = runTest {
            val testCases = testDataLoader.loadGroundTruthData("mixed-confidence")
            val thresholds = listOf(0.3f, 0.5f, 0.7f, 0.8f, 0.9f)
            
            val optimizationResults = thresholds.map { threshold ->
                val filteredResults = analyzeTestCasesWithThreshold(testCases, threshold)
                val metrics = calculateAccuracyMetrics(filteredResults)
                threshold to metrics
            }
            
            // Find optimal threshold that balances precision and recall
            val optimalThreshold = optimizationResults.maxByOrNull { it.second.f1Score }?.first
            assertNotNull("Could not determine optimal confidence threshold", optimalThreshold)
            assertTrue("Optimal threshold should be reasonable", optimalThreshold!! in 0.5f..0.8f)
            
            logThresholdOptimizationResults(optimizationResults)
        }
        
        @Test
        fun `test multi-hazard scenario accuracy`() = runTest {
            val multiHazardCases = testDataLoader.loadGroundTruthData("multi-hazard")
            val results = analyzeTestCases(multiHazardCases)
            
            // Verify all hazards in multi-hazard scenarios are detected
            val multiHazardAccuracy = results.filter { it.expectedHazards.size > 1 }
                .map { result ->
                    val detectedTypes = result.actualHazards.map { it.type }.toSet()
                    val expectedTypes = result.expectedHazards.map { it.type }.toSet()
                    expectedTypes.intersect(detectedTypes).size.toFloat() / expectedTypes.size
                }.average().toFloat()
            
            assertTrue(
                "Multi-hazard detection accuracy ${multiHazardAccuracy} below required 0.70",
                multiHazardAccuracy >= 0.70f
            )
        }
        
        @Test
        fun `test OSHA compliance validation accuracy`() = runTest {
            val oshaTestCases = testDataLoader.loadGroundTruthData("osha-compliance")
            val results = analyzeTestCases(oshaTestCases)
            
            // Verify OSHA code assignment for each hazard type
            val oshaAccuracyByType = HazardType.values().associate { hazardType ->
                val relevantResults = results.filter { result ->
                    result.expectedHazards.any { it.type == hazardType }
                }
                
                if (relevantResults.isEmpty()) {
                    hazardType to 1.0f // No test cases for this type
                } else {
                    val correctOSHACodes = relevantResults.count { result ->
                        val expectedCodes = result.expectedHazards
                            .filter { it.type == hazardType }
                            .mapNotNull { it.oshaCode }
                            .toSet()
                        val actualCodes = result.actualHazards
                            .filter { it.type == hazardType }
                            .mapNotNull { it.oshaCode }
                            .toSet()
                        expectedCodes.intersect(actualCodes).isNotEmpty()
                    }
                    hazardType to (correctOSHACodes.toFloat() / relevantResults.size)
                }
            }
            
            // All OSHA code assignments should be >= 90% accurate
            oshaAccuracyByType.forEach { (hazardType, accuracy) ->
                assertTrue(
                    "OSHA code accuracy for $hazardType is ${accuracy}, below required 0.90",
                    accuracy >= 0.90f
                )
            }
        }
        
        private suspend fun analyzeTestCases(testCases: List<GroundTruthTestCase>): List<TestResult> {
            return testCases.map { testCase ->
                val analysisResult = aiAnalyzer.analyzePhoto(testCase.imageData, testCase.workType)
                val analysis = analysisResult.getOrNull()
                
                TestResult(
                    testCase = testCase,
                    actualHazards = analysis?.hazards ?: emptyList(),
                    actualPPE = analysis?.ppeStatus ?: emptyMap(),
                    actualOSHACodes = analysis?.hazards?.mapNotNull { it.oshaCode } ?: emptyList(),
                    actualRiskLevel = analysis?.overallRisk ?: RiskLevel.UNKNOWN,
                    confidence = analysis?.confidence ?: 0f,
                    processingTime = analysis?.processingTimeMs ?: 0L,
                    wasSuccessful = analysisResult.isSuccess
                )
            }
        }
        
        private suspend fun analyzePPETestCases(testCases: List<GroundTruthTestCase>): List<PPETestResult> {
            return testCases.map { testCase ->
                val analysisResult = aiAnalyzer.analyzePhoto(testCase.imageData, testCase.workType)
                val analysis = analysisResult.getOrNull()
                
                PPETestResult(
                    testCase = testCase,
                    detectedPPE = analysis?.ppeStatus ?: emptyMap(),
                    expectedPPE = testCase.expectedPPE
                )
            }
        }
        
        private suspend fun analyzeTestCasesWithThreshold(
            testCases: List<GroundTruthTestCase>,
            confidenceThreshold: Float
        ): List<TestResult> {
            return testCases.map { testCase ->
                val analysisResult = aiAnalyzer.analyzePhoto(
                    testCase.imageData, 
                    testCase.workType,
                    AnalysisOptions(confidenceThreshold = confidenceThreshold)
                )
                val analysis = analysisResult.getOrNull()
                
                TestResult(
                    testCase = testCase,
                    actualHazards = analysis?.hazards?.filter { it.confidence >= confidenceThreshold } ?: emptyList(),
                    actualPPE = analysis?.ppeStatus ?: emptyMap(),
                    actualOSHACodes = analysis?.hazards?.mapNotNull { it.oshaCode } ?: emptyList(),
                    actualRiskLevel = analysis?.overallRisk ?: RiskLevel.UNKNOWN,
                    confidence = analysis?.confidence ?: 0f,
                    processingTime = analysis?.processingTimeMs ?: 0L,
                    wasSuccessful = analysisResult.isSuccess
                )
            }
        }
        
        private fun calculateAccuracyMetrics(results: List<TestResult>): AccuracyMetrics {
            val totalTests = results.size
            var correctDetections = 0
            var falsePositives = 0
            var falseNegatives = 0
            var criticalMisses = 0
            
            results.forEach { result ->
                val expectedTypes = result.expectedHazards.map { it.type }.toSet()
                val actualTypes = result.actualHazards.map { it.type }.toSet()
                val criticalTypes = result.expectedHazards.filter { it.mustDetect }.map { it.type }.toSet()
                
                // Count correct detections (intersection)
                correctDetections += expectedTypes.intersect(actualTypes).size
                
                // Count false positives (detected but not expected)
                falsePositives += actualTypes.subtract(expectedTypes).size
                
                // Count false negatives (expected but not detected)
                falseNegatives += expectedTypes.subtract(actualTypes).size
                
                // Count critical misses
                criticalMisses += criticalTypes.subtract(actualTypes).size
            }
            
            val totalExpected = results.sumOf { it.expectedHazards.size }
            val totalDetected = results.sumOf { it.actualHazards.size }
            val totalCritical = results.sumOf { it.expectedHazards.count { it.mustDetect } }
            
            val accuracy = if (totalExpected > 0) correctDetections.toFloat() / totalExpected else 0f
            val precision = if (totalDetected > 0) correctDetections.toFloat() / totalDetected else 0f
            val recall = if (totalExpected > 0) correctDetections.toFloat() / totalExpected else 0f
            val f1Score = if (precision + recall > 0) 2 * (precision * recall) / (precision + recall) else 0f
            val criticalMissRate = if (totalCritical > 0) criticalMisses.toFloat() / totalCritical else 0f
            
            return AccuracyMetrics(
                totalTests = totalTests,
                correctDetections = correctDetections,
                falsePositives = falsePositives,
                falseNegatives = falseNegatives,
                accuracy = accuracy,
                precision = precision,
                recall = recall,
                f1Score = f1Score,
                criticalMissRate = criticalMissRate
            )
        }
        
        private fun calculatePPEAccuracy(results: List<PPETestResult>, ppeType: PPEType): Float {
            val relevantResults = results.filter { it.expectedPPE.containsKey(ppeType) }
            if (relevantResults.isEmpty()) return 1.0f
            
            val correctDetections = relevantResults.count { result ->
                val expected = result.expectedPPE[ppeType] ?: false
                val detected = result.detectedPPE[ppeType]?.isPresent ?: false
                expected == detected
            }
            
            return correctDetections.toFloat() / relevantResults.size
        }
        
        private fun validateOSHACodeAssignment(results: List<TestResult>, expectedCode: String): Float {
            val relevantResults = results.filter { 
                it.expectedOSHACodes.contains(expectedCode) 
            }
            if (relevantResults.isEmpty()) return 1.0f
            
            val correctAssignments = relevantResults.count { result ->
                result.actualOSHACodes.contains(expectedCode)
            }
            
            return correctAssignments.toFloat() / relevantResults.size
        }
        
        private fun logAccuracyResults(category: String, metrics: AccuracyMetrics) {
            println("=== $category Accuracy Results ===")
            println("Total Tests: ${metrics.totalTests}")
            println("Accuracy: ${String.format("%.2f%%", metrics.accuracy * 100)}")
            println("Precision: ${String.format("%.2f%%", metrics.precision * 100)}")
            println("Recall: ${String.format("%.2f%%", metrics.recall * 100)}")
            println("F1 Score: ${String.format("%.3f", metrics.f1Score)}")
            println("Critical Miss Rate: ${String.format("%.2f%%", metrics.criticalMissRate * 100)}")
            println("False Positives: ${metrics.falsePositives}")
            println("False Negatives: ${metrics.falseNegatives}")
            println("=====================================")
        }
        
        private fun logPPEAccuracyResults(results: List<PPETestResult>) {
            println("=== PPE Detection Accuracy Results ===")
            PPEType.values().forEach { ppeType ->
                val accuracy = calculatePPEAccuracy(results, ppeType)
                println("$ppeType: ${String.format("%.2f%%", accuracy * 100)}")
            }
            println("======================================")
        }
        
        private fun logThresholdOptimizationResults(results: List<Pair<Float, AccuracyMetrics>>) {
            println("=== Confidence Threshold Optimization ===")
            results.forEach { (threshold, metrics) ->
                println("Threshold $threshold: F1=${String.format("%.3f", metrics.f1Score)}, " +
                       "Accuracy=${String.format("%.2f%%", metrics.accuracy * 100)}")
            }
            println("==========================================")
        }
    }
    
    /**
     * Test result data classes
     */
    data class TestResult(
        val testCase: GroundTruthTestCase,
        val actualHazards: List<DetectedHazard>,
        val actualPPE: Map<PPEType, PPEDetection>,
        val actualOSHACodes: List<String>,
        val actualRiskLevel: RiskLevel,
        val confidence: Float,
        val processingTime: Long,
        val wasSuccessful: Boolean
    ) {
        val expectedHazards: List<ExpectedHazard> = testCase.expectedHazards
        val expectedOSHACodes: List<String> = testCase.expectedOSHACodes
        
        val wasDetectedCorrectly: Boolean
            get() {
                val expectedTypes = expectedHazards.map { it.type }.toSet()
                val actualTypes = actualHazards.map { it.type }.toSet()
                return expectedTypes == actualTypes
            }
    }
    
    data class PPETestResult(
        val testCase: GroundTruthTestCase,
        val detectedPPE: Map<PPEType, PPEDetection>,
        val expectedPPE: Map<PPEType, Boolean>
    )
    
    /**
     * Test data loader for managing ground truth datasets
     */
    class AITestDataLoader {
        fun loadGroundTruthData(category: String): List<GroundTruthTestCase> {
            // Implementation would load test data from resources
            // For now, return mock data for testing framework
            return when (category) {
                "fall-protection" -> createFallProtectionTestCases()
                "ppe-detection" -> createPPETestCases()
                "electrical-hazards" -> createElectricalTestCases()
                "struck-by" -> createStruckByTestCases()
                "caught-in-between" -> createCaughtInBetweenTestCases()
                "multi-hazard" -> createMultiHazardTestCases()
                "osha-compliance" -> createOSHAComplianceTestCases()
                "mixed-confidence" -> createMixedConfidenceTestCases()
                else -> emptyList()
            }
        }
        
        private fun createFallProtectionTestCases(): List<GroundTruthTestCase> {
            return listOf(
                GroundTruthTestCase(
                    id = "fall_001",
                    imageData = createMockImageData("worker_near_unguarded_edge"),
                    workType = WorkType.CONCRETE_WORK,
                    filename = "worker_near_unguarded_edge.jpg",
                    category = "fall-protection",
                    expectedHazards = listOf(
                        ExpectedHazard(
                            type = HazardType.FALL_PROTECTION,
                            severity = Severity.HIGH,
                            confidence = 0.85f,
                            boundingBox = BoundingBox(0.2f, 0.3f, 0.4f, 0.5f, 0.85f),
                            oshaCode = "1926.501",
                            mustDetect = true // Critical hazard
                        )
                    ),
                    expectedPPE = mapOf(
                        PPEType.HARD_HAT to true,
                        PPEType.SAFETY_HARNESS to false
                    ),
                    expectedOSHACodes = listOf("1926.501"),
                    overallRiskLevel = RiskLevel.HIGH,
                    description = "Worker operating near unguarded roof edge without fall protection"
                )
                // Add more fall protection test cases...
            )
        }
        
        private fun createPPETestCases(): List<GroundTruthTestCase> {
            return listOf(
                GroundTruthTestCase(
                    id = "ppe_001",
                    imageData = createMockImageData("missing_hard_hat"),
                    workType = WorkType.GENERAL_CONSTRUCTION,
                    filename = "missing_hard_hat.jpg",
                    category = "ppe-detection",
                    expectedHazards = listOf(
                        ExpectedHazard(
                            type = HazardType.PPE_VIOLATION,
                            severity = Severity.MEDIUM,
                            confidence = 0.80f,
                            boundingBox = BoundingBox(0.3f, 0.1f, 0.2f, 0.3f, 0.80f),
                            oshaCode = "1926.95",
                            mustDetect = false
                        )
                    ),
                    expectedPPE = mapOf(
                        PPEType.HARD_HAT to false,
                        PPEType.SAFETY_VEST to true
                    ),
                    expectedOSHACodes = listOf("1926.95"),
                    overallRiskLevel = RiskLevel.MEDIUM,
                    description = "Construction worker without required hard hat"
                )
            )
        }
        
        private fun createElectricalTestCases(): List<GroundTruthTestCase> {
            return listOf(
                GroundTruthTestCase(
                    id = "elec_001",
                    imageData = createMockImageData("exposed_wiring"),
                    workType = WorkType.ELECTRICAL_WORK,
                    filename = "exposed_wiring.jpg",
                    category = "electrical-hazards",
                    expectedHazards = listOf(
                        ExpectedHazard(
                            type = HazardType.ELECTRICAL,
                            severity = Severity.HIGH,
                            confidence = 0.90f,
                            boundingBox = BoundingBox(0.4f, 0.5f, 0.3f, 0.2f, 0.90f),
                            oshaCode = "1926.95",
                            mustDetect = true // Electrical hazards are critical
                        )
                    ),
                    expectedPPE = mapOf(
                        PPEType.HARD_HAT to true,
                        PPEType.ELECTRICAL_GLOVES to false
                    ),
                    expectedOSHACodes = listOf("1926.95"),
                    overallRiskLevel = RiskLevel.HIGH,
                    description = "Exposed electrical wiring in work area"
                )
            )
        }
        
        private fun createStruckByTestCases(): List<GroundTruthTestCase> {
            return listOf(
                GroundTruthTestCase(
                    id = "struck_001",
                    imageData = createMockImageData("heavy_machinery_operation"),
                    workType = WorkType.EXCAVATION,
                    filename = "heavy_machinery_operation.jpg",
                    category = "struck-by",
                    expectedHazards = listOf(
                        ExpectedHazard(
                            type = HazardType.STRUCK_BY,
                            severity = Severity.HIGH,
                            confidence = 0.85f,
                            boundingBox = BoundingBox(0.1f, 0.2f, 0.6f, 0.5f, 0.85f),
                            oshaCode = "1926.602",
                            mustDetect = true
                        )
                    ),
                    expectedPPE = mapOf(
                        PPEType.HARD_HAT to true,
                        PPEType.SAFETY_VEST to true
                    ),
                    expectedOSHACodes = listOf("1926.602"),
                    overallRiskLevel = RiskLevel.HIGH,
                    description = "Workers in proximity to operating heavy machinery"
                )
            )
        }
        
        private fun createCaughtInBetweenTestCases(): List<GroundTruthTestCase> {
            return listOf(
                GroundTruthTestCase(
                    id = "caught_001",
                    imageData = createMockImageData("trench_work"),
                    workType = WorkType.EXCAVATION,
                    filename = "trench_work.jpg",
                    category = "caught-in-between",
                    expectedHazards = listOf(
                        ExpectedHazard(
                            type = HazardType.CAUGHT_IN_BETWEEN,
                            severity = Severity.HIGH,
                            confidence = 0.75f,
                            boundingBox = BoundingBox(0.2f, 0.4f, 0.5f, 0.4f, 0.75f),
                            oshaCode = "1926.652",
                            mustDetect = true
                        )
                    ),
                    expectedPPE = mapOf(
                        PPEType.HARD_HAT to true,
                        PPEType.SAFETY_VEST to true
                    ),
                    expectedOSHACodes = listOf("1926.652"),
                    overallRiskLevel = RiskLevel.HIGH,
                    description = "Unprotected trench work with cave-in potential"
                )
            )
        }
        
        private fun createMultiHazardTestCases(): List<GroundTruthTestCase> {
            return listOf(
                GroundTruthTestCase(
                    id = "multi_001",
                    imageData = createMockImageData("complex_construction_site"),
                    workType = WorkType.GENERAL_CONSTRUCTION,
                    filename = "complex_construction_site.jpg",
                    category = "multi-hazard",
                    expectedHazards = listOf(
                        ExpectedHazard(
                            type = HazardType.FALL_PROTECTION,
                            severity = Severity.HIGH,
                            confidence = 0.80f,
                            boundingBox = BoundingBox(0.1f, 0.1f, 0.3f, 0.4f, 0.80f),
                            oshaCode = "1926.501",
                            mustDetect = true
                        ),
                        ExpectedHazard(
                            type = HazardType.ELECTRICAL,
                            severity = Severity.HIGH,
                            confidence = 0.85f,
                            boundingBox = BoundingBox(0.6f, 0.3f, 0.2f, 0.3f, 0.85f),
                            oshaCode = "1926.95",
                            mustDetect = true
                        ),
                        ExpectedHazard(
                            type = HazardType.PPE_VIOLATION,
                            severity = Severity.MEDIUM,
                            confidence = 0.70f,
                            boundingBox = BoundingBox(0.4f, 0.2f, 0.15f, 0.25f, 0.70f),
                            oshaCode = "1926.95",
                            mustDetect = false
                        )
                    ),
                    expectedPPE = mapOf(
                        PPEType.HARD_HAT to false,
                        PPEType.SAFETY_VEST to true,
                        PPEType.EYE_PROTECTION to false
                    ),
                    expectedOSHACodes = listOf("1926.501", "1926.95"),
                    overallRiskLevel = RiskLevel.HIGH,
                    description = "Complex site with multiple simultaneous hazards"
                )
            )
        }
        
        private fun createOSHAComplianceTestCases(): List<GroundTruthTestCase> {
            return createFallProtectionTestCases() + createElectricalTestCases() + createStruckByTestCases()
        }
        
        private fun createMixedConfidenceTestCases(): List<GroundTruthTestCase> {
            return createFallProtectionTestCases().map { testCase ->
                testCase.copy(
                    expectedHazards = testCase.expectedHazards.mapIndexed { index, hazard ->
                        hazard.copy(confidence = 0.3f + (index * 0.15f)) // Varying confidence levels
                    }
                )
            }
        }
        
        private fun createMockImageData(scenario: String): ByteArray {
            // In real implementation, this would load actual test images
            return scenario.toByteArray()
        }
    }
    
    /**
     * Helper function to create test AI analyzer
     */
    private fun createTestAIAnalyzer(): AIPhotoAnalyzer {
        // Would return configured AI analyzer for testing
        // Could be real analyzer with test API keys or enhanced mock
        return MockEnhancedAIAnalyzer()
    }
}

/**
 * Enhanced mock AI analyzer that provides realistic responses based on test scenarios
 */
class MockEnhancedAIAnalyzer : AIPhotoAnalyzer {
    override val analyzerName = "Enhanced Mock AI Analyzer"
    override val analysisCapabilities = setOf(
        AnalysisCapability.HAZARD_DETECTION,
        AnalysisCapability.PPE_DETECTION,
        AnalysisCapability.OSHA_COMPLIANCE
    )
    
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType,
        analysisOptions: AnalysisOptions
    ): Result<SafetyAnalysis> {
        
        // Simulate realistic analysis based on "image content" (encoded in byte array for testing)
        val scenario = String(imageData)
        val hazards = generateRealisticHazards(scenario, analysisOptions.confidenceThreshold)
        val ppeStatus = generateRealisticPPEStatus(scenario)
        
        return Result.success(
            SafetyAnalysis(
                id = "test-${System.currentTimeMillis()}",
                hazards = hazards,
                ppeStatus = ppeStatus,
                overallRisk = determineOverallRisk(hazards),
                confidence = hazards.map { it.confidence }.average().toFloat().takeIf { !it.isNaN() } ?: 0.8f,
                processingTimeMs = (100..300).random().toLong(),
                aiProvider = analyzerName,
                recommendations = generateRecommendations(hazards),
                analysisType = AnalysisType.MOCK_TESTING
            )
        )
    }
    
    private fun generateRealisticHazards(scenario: String, confidenceThreshold: Float): List<DetectedHazard> {
        val hazards = mutableListOf<DetectedHazard>()
        
        when {
            scenario.contains("unguarded_edge") || scenario.contains("fall") -> {
                hazards.add(
                    DetectedHazard(
                        type = HazardType.FALL_PROTECTION,
                        description = "Worker near unguarded edge",
                        severity = Severity.HIGH,
                        confidence = 0.87f,
                        boundingBox = BoundingBox(0.2f, 0.3f, 0.4f, 0.5f, 0.87f),
                        oshaCode = "1926.501",
                        recommendations = listOf("Install guardrails", "Use safety harness")
                    )
                )
            }
            scenario.contains("exposed_wiring") || scenario.contains("electrical") -> {
                hazards.add(
                    DetectedHazard(
                        type = HazardType.ELECTRICAL,
                        description = "Exposed electrical wiring",
                        severity = Severity.HIGH,
                        confidence = 0.91f,
                        boundingBox = BoundingBox(0.4f, 0.5f, 0.3f, 0.2f, 0.91f),
                        oshaCode = "1926.95",
                        recommendations = listOf("Turn off power", "Use electrical PPE")
                    )
                )
            }
            scenario.contains("hard_hat") || scenario.contains("ppe") -> {
                hazards.add(
                    DetectedHazard(
                        type = HazardType.PPE_VIOLATION,
                        description = "Worker without required hard hat",
                        severity = Severity.MEDIUM,
                        confidence = 0.83f,
                        boundingBox = BoundingBox(0.3f, 0.1f, 0.2f, 0.3f, 0.83f),
                        oshaCode = "1926.95",
                        recommendations = listOf("Ensure all workers wear hard hats")
                    )
                )
            }
            scenario.contains("machinery") || scenario.contains("struck") -> {
                hazards.add(
                    DetectedHazard(
                        type = HazardType.STRUCK_BY,
                        description = "Workers in proximity to operating machinery",
                        severity = Severity.HIGH,
                        confidence = 0.85f,
                        boundingBox = BoundingBox(0.1f, 0.2f, 0.6f, 0.5f, 0.85f),
                        oshaCode = "1926.602",
                        recommendations = listOf("Maintain safe distance", "Use spotters")
                    )
                )
            }
            scenario.contains("trench") || scenario.contains("caught") -> {
                hazards.add(
                    DetectedHazard(
                        type = HazardType.CAUGHT_IN_BETWEEN,
                        description = "Unprotected trench work",
                        severity = Severity.HIGH,
                        confidence = 0.78f,
                        boundingBox = BoundingBox(0.2f, 0.4f, 0.5f, 0.4f, 0.78f),
                        oshaCode = "1926.652",
                        recommendations = listOf("Install trench protection", "Use proper shoring")
                    )
                )
            }
        }
        
        // Filter by confidence threshold
        return hazards.filter { it.confidence >= confidenceThreshold }
    }
    
    private fun generateRealisticPPEStatus(scenario: String): Map<PPEType, PPEDetection> {
        val ppeStatus = mutableMapOf<PPEType, PPEDetection>()
        
        // Generate realistic PPE detection based on scenario
        when {
            scenario.contains("hard_hat") -> {
                ppeStatus[PPEType.HARD_HAT] = PPEDetection(
                    isPresent = !scenario.contains("missing"),
                    confidence = 0.92f,
                    boundingBox = BoundingBox(0.45f, 0.1f, 0.1f, 0.15f, 0.92f)
                )
            }
            scenario.contains("electrical") -> {
                ppeStatus[PPEType.ELECTRICAL_GLOVES] = PPEDetection(
                    isPresent = false,
                    confidence = 0.88f,
                    boundingBox = null
                )
            }
        }
        
        // Always include common PPE types with realistic detection
        ppeStatus.putIfAbsent(PPEType.SAFETY_VEST, PPEDetection(
            isPresent = true,
            confidence = 0.89f,
            boundingBox = BoundingBox(0.35f, 0.3f, 0.3f, 0.4f, 0.89f)
        ))
        
        return ppeStatus
    }
    
    private fun determineOverallRisk(hazards: List<DetectedHazard>): RiskLevel {
        return when {
            hazards.any { it.severity == Severity.HIGH } -> RiskLevel.HIGH
            hazards.any { it.severity == Severity.MEDIUM } -> RiskLevel.MEDIUM
            hazards.isNotEmpty() -> RiskLevel.LOW
            else -> RiskLevel.LOW
        }
    }
    
    private fun generateRecommendations(hazards: List<DetectedHazard>): List<String> {
        val recommendations = mutableListOf<String>()
        
        hazards.forEach { hazard ->
            recommendations.addAll(hazard.recommendations)
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Continue following safety protocols")
        }
        
        return recommendations.distinct()
    }
    
    override fun getPerformanceMetrics(): AnalyzerPerformanceMetrics {
        return AnalyzerPerformanceMetrics(
            analysisCount = 100L,
            averageProcessingTime = 200L,
            successRate = 0.98f,
            averageConfidence = 0.85f
        )
    }
    
    override fun cleanup() {
        // Mock cleanup
    }
}
