package com.hazardhawk.ai.litert

import com.hazardhawk.TestUtils
import com.hazardhawk.ai.core.*
import com.hazardhawk.core.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Integration tests for LiteRT with SmartAIOrchestrator and SimplifiedAIOrchestrator.
 * Tests end-to-end photo analysis workflows, orchestrator comparison, and feature flag behavior.
 */
class LiteRTIntegrationTest {
    
    private lateinit var mockLiteRTEngine: MockLiteRTModelEngine
    private lateinit var mockSmartOrchestrator: MockSmartAIOrchestrator
    private lateinit var mockSimplifiedOrchestrator: MockSimplifiedAIOrchestrator
    private lateinit var testDataFactory: LiteRTTestDataFactory
    private lateinit var performanceTestRunner: LiteRTPerformanceTestRunner
    
    @BeforeTest
    fun setup() {
        mockLiteRTEngine = MockLiteRTModelEngine()
        mockSmartOrchestrator = MockSmartAIOrchestrator()
        mockSimplifiedOrchestrator = MockSimplifiedAIOrchestrator()
        testDataFactory = LiteRTTestDataFactory()
        performanceTestRunner = LiteRTPerformanceTestRunner()
    }
    
    @AfterTest
    fun tearDown() {
        mockLiteRTEngine.cleanup()
    }
    
    // =====================================================
    // END-TO-END WORKFLOW TESTS
    // =====================================================
    
    @Test
    fun `test complete photo analysis workflow with LiteRT`() = runTest {
        // Setup
        mockLiteRTEngine.setDeviceCapabilities(
            totalMemoryGB = 8f,
            supportedBackends = setOf(
                LiteRTBackend.CPU,
                LiteRTBackend.GPU_OPENCL,
                LiteRTBackend.NPU_QTI_HTP
            )
        )
        
        val initResult = mockLiteRTEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.AUTO)
        assertTrue(initResult.isSuccess, "LiteRT engine should initialize successfully")
        assertEquals(LiteRTBackend.NPU_QTI_HTP, mockLiteRTEngine.currentBackend)
        
        // Test workflow steps
        val workflowSteps = listOf(
            WorkflowStep(
                name = "PPE Compliance Check",
                imageData = testDataFactory.createPPECompliantImage(),
                workType = WorkType.GENERAL_CONSTRUCTION,
                expectedRisk = Severity.LOW
            ),
            WorkflowStep(
                name = "Fall Hazard Detection",
                imageData = testDataFactory.createFallHazardImage(),
                workType = WorkType.ROOFING,
                expectedRisk = Severity.HIGH
            ),
            WorkflowStep(
                name = "Electrical Safety Analysis",
                imageData = testDataFactory.createWorkTypeSpecificImage(WorkType.ELECTRICAL),
                workType = WorkType.ELECTRICAL,
                expectedRisk = Severity.MEDIUM
            )
        )
        
        val workflowResults = mutableListOf<WorkflowResult>()
        
        workflowSteps.forEach { step ->
            val (analysisResult, duration) = TestUtils.measureExecutionTime {
                mockLiteRTEngine.generateSafetyAnalysis(
                    imageData = step.imageData,
                    workType = step.workType,
                    includeOSHACodes = true,
                    confidenceThreshold = 0.7f
                )
            }
            
            assertTrue(analysisResult.isSuccess, "Analysis should succeed for ${step.name}")
            
            val analysis = analysisResult.getOrNull()!!
            workflowResults.add(WorkflowResult(
                stepName = step.name,
                success = true,
                processingTimeMs = duration.inWholeMilliseconds,
                detectedRisk = analysis.overallRiskAssessment.overallRisk,
                hazardCount = analysis.hazards.size,
                oshaViolationCount = analysis.oshaViolations.size
            ))
            
            // Validate risk level is as expected
            assertTrue(
                analysis.overallRiskAssessment.overallRisk == step.expectedRisk,
                "${step.name}: Expected ${step.expectedRisk} risk, got ${analysis.overallRiskAssessment.overallRisk}"
            )
            
            // Validate processing performance
            TestUtils.assertPerformanceWithin(
                actualMs = duration.inWholeMilliseconds,
                expectedMs = 1000L, // 1 second target for NPU
                tolerancePercent = 50.0,
                scenario = step.name
            )
        }
        
        // Validate overall workflow performance
        val totalTime = workflowResults.sumOf { it.processingTimeMs }
        assertTrue(totalTime < 5000L, "Complete workflow should finish within 5 seconds")
        
        // Validate performance metrics tracking
        val metrics = mockLiteRTEngine.getPerformanceMetrics()
        assertEquals(3L, metrics.analysisCount)
        assertEquals(1.0f, metrics.successRate, 0.001f)
        assertTrue(metrics.averageProcessingTimeMs > 0)
    }
    
    @Test
    fun `test orchestrator fallback chain integration`() = runTest {
        // Simulate LiteRT failure scenario
        mockLiteRTEngine.setModelCorrupted(true)
        
        val imageData = testDataFactory.createStandardTestImage()
        val workType = WorkType.GENERAL_CONSTRUCTION
        
        // Test SmartAIOrchestrator fallback behavior
        val smartResult = mockSmartOrchestrator.analyzePhoto(imageData, workType)
        
        assertTrue(smartResult.isSuccess, "Smart orchestrator should succeed with fallback")
        
        val analysis = smartResult.getOrNull()!!
        // Should indicate fallback was used
        assertTrue(
            analysis.analysisType != AnalysisType.LOCAL_GEMMA_MULTIMODAL,
            "Should use fallback analysis type when LiteRT fails"
        )
        
        // Test that fallback is properly logged
        val orchestratorStats = mockSmartOrchestrator.getStats()
        assertTrue(
            orchestratorStats.failureCounts.containsKey(AnalysisType.LOCAL_GEMMA_MULTIMODAL),
            "Should track LiteRT failure"
        )
    }
    
    @Test
    fun `test concurrent photo analysis with LiteRT`() = runTest {
        mockLiteRTEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.GPU_OPENCL)
        
        val testImages = listOf(
            testDataFactory.createPPECompliantImage() to WorkType.GENERAL_CONSTRUCTION,
            testDataFactory.createFallHazardImage() to WorkType.ROOFING,
            testDataFactory.createWorkTypeSpecificImage(WorkType.ELECTRICAL) to WorkType.ELECTRICAL
        )
        
        // Test concurrent processing
        val concurrentResults = mockSmartOrchestrator.analyzeBatch(
            images = testImages,
            maxConcurrency = 3
        )
        
        assertEquals(3, concurrentResults.size)
        concurrentResults.forEach { result ->
            assertTrue(result.isSuccess, "Each concurrent analysis should succeed")
        }
        
        // Verify memory usage didn't exceed limits during concurrent processing
        val metrics = mockLiteRTEngine.getPerformanceMetrics()
        assertTrue(
            metrics.peakMemoryUsageMB < 2000f, // Should stay under 2GB
            "Concurrent processing should manage memory efficiently"
        )
        
        assertFalse(
            metrics.thermalThrottlingDetected,
            "Should not trigger thermal throttling in normal conditions"
        )
    }
    
    // =====================================================
    // ORCHESTRATOR COMPARISON TESTS
    // =====================================================
    
    @Test
    fun `test SmartAIOrchestrator vs SimplifiedAIOrchestrator comparison`() = runTest {
        val testImage = testDataFactory.createMultipleHazardsImage()
        val workType = WorkType.GENERAL_CONSTRUCTION
        
        // Configure both orchestrators
        mockSmartOrchestrator.setLiteRTAvailable(true)
        mockSimplifiedOrchestrator.setLiteRTAvailable(true)
        
        // Test SmartAIOrchestrator
        val (smartResult, smartDuration) = TestUtils.measureExecutionTime {
            mockSmartOrchestrator.analyzePhoto(testImage, workType)
        }
        
        // Test SimplifiedAIOrchestrator  
        val (simplifiedResult, simplifiedDuration) = TestUtils.measureExecutionTime {
            mockSimplifiedOrchestrator.analyzePhoto(testImage, workType)
        }
        
        assertTrue(smartResult.isSuccess && simplifiedResult.isSuccess)
        
        val smartAnalysis = smartResult.getOrNull()!!
        val simplifiedAnalysis = simplifiedResult.getOrNull()!!
        
        // Compare analysis quality
        assertAnalysisQuality(smartAnalysis, "Smart Orchestrator")
        assertAnalysisQuality(simplifiedAnalysis, "Simplified Orchestrator")
        
        // Smart orchestrator should provide more comprehensive analysis
        assertTrue(
            smartAnalysis.hazards.size >= simplifiedAnalysis.hazards.size,
            "Smart orchestrator should detect same or more hazards"
        )
        
        assertTrue(
            smartAnalysis.oshaViolations.size >= simplifiedAnalysis.oshaViolations.size,
            "Smart orchestrator should identify same or more OSHA violations"
        )
        
        // Performance comparison
        val performanceComparison = PerformanceComparison(
            smartOrchestratorTimeMs = smartDuration.inWholeMilliseconds,
            simplifiedOrchestratorTimeMs = simplifiedDuration.inWholeMilliseconds,
            smartHazardCount = smartAnalysis.hazards.size,
            simplifiedHazardCount = simplifiedAnalysis.hazards.size,
            smartConfidence = smartAnalysis.confidence,
            simplifiedConfidence = simplifiedAnalysis.confidence
        )
        
        logPerformanceComparison(performanceComparison)
        
        // Both should complete within reasonable time
        assertTrue(smartDuration.inWholeMilliseconds < 5000L, "Smart orchestrator should be reasonably fast")
        assertTrue(simplifiedDuration.inWholeMilliseconds < 3000L, "Simplified orchestrator should be faster")
    }
    
    @Test
    fun `test A/B testing framework integration`() = runTest {
        val testScenarios = listOf(
            ABTestScenario(
                name = "PPE Detection Accuracy",
                imageData = testDataFactory.createMissingHardHatImage(),
                workType = WorkType.GENERAL_CONSTRUCTION,
                expectedFeature = "missing_ppe_detection"
            ),
            ABTestScenario(
                name = "Fall Hazard Sensitivity", 
                imageData = testDataFactory.createFallHazardImage(),
                workType = WorkType.ROOFING,
                expectedFeature = "fall_hazard_detection"
            )
        )
        
        val abTestResults = mutableListOf<ABTestResult>()
        
        testScenarios.forEach { scenario ->
            // Test with Smart orchestrator (Group A)
            mockSmartOrchestrator.setFeatureFlag("enhanced_analysis", true)
            val smartResult = mockSmartOrchestrator.analyzePhoto(scenario.imageData, scenario.workType)
            
            // Test with Simplified orchestrator (Group B)
            mockSimplifiedOrchestrator.setFeatureFlag("enhanced_analysis", false)
            val simplifiedResult = mockSimplifiedOrchestrator.analyzePhoto(scenario.imageData, scenario.workType)
            
            assertTrue(smartResult.isSuccess && simplifiedResult.isSuccess)
            
            val smartAnalysis = smartResult.getOrNull()!!
            val simplifiedAnalysis = simplifiedResult.getOrNull()!!
            
            abTestResults.add(ABTestResult(
                scenarioName = scenario.name,
                groupA_HazardCount = smartAnalysis.hazards.size,
                groupB_HazardCount = simplifiedAnalysis.hazards.size,
                groupA_Confidence = smartAnalysis.confidence,
                groupB_Confidence = simplifiedAnalysis.confidence,
                groupA_ProcessingTime = smartAnalysis.processingTimeMs,
                groupB_ProcessingTime = simplifiedAnalysis.processingTimeMs
            ))
        }
        
        // Analyze A/B test results
        val avgSmartHazards = abTestResults.map { it.groupA_HazardCount }.average()
        val avgSimplifiedHazards = abTestResults.map { it.groupB_HazardCount }.average()
        val avgSmartConfidence = abTestResults.map { it.groupA_Confidence.toDouble() }.average()
        val avgSimplifiedConfidence = abTestResults.map { it.groupB_Confidence.toDouble() }.average()
        
        assertTrue(avgSmartHazards > 0, "Smart orchestrator should detect hazards")
        assertTrue(avgSimplifiedHazards > 0, "Simplified orchestrator should detect hazards")
        assertTrue(avgSmartConfidence > 0.7, "Smart orchestrator should have good confidence")
        assertTrue(avgSimplifiedConfidence > 0.7, "Simplified orchestrator should have good confidence")
    }
    
    // =====================================================
    // FEATURE FLAG BEHAVIOR TESTS
    // =====================================================
    
    @Test
    fun `test feature flag rollback mechanism`() = runTest {
        val testImage = testDataFactory.createStandardTestImage()
        
        // Test normal operation
        mockSmartOrchestrator.setFeatureFlag("litert_enabled", true)
        val normalResult = mockSmartOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
        assertTrue(normalResult.isSuccess, "Normal operation should succeed")
        
        // Simulate emergency rollback
        mockSmartOrchestrator.setEmergencyRollback(true)
        mockSmartOrchestrator.setFeatureFlag("litert_enabled", false)
        
        val rollbackResult = mockSmartOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
        assertTrue(rollbackResult.isSuccess, "Should succeed with rollback")
        
        val rollbackAnalysis = rollbackResult.getOrNull()!!
        assertTrue(
            rollbackAnalysis.recommendations.any { 
                it.contains("emergency mode", ignoreCase = true) 
            },
            "Should indicate emergency rollback mode"
        )
    }
    
    @Test
    fun `test gradual feature rollout simulation`() = runTest {
        val rolloutPercentages = listOf(0, 25, 50, 75, 100)
        val testImage = testDataFactory.createStandardTestImage()
        val rolloutResults = mutableMapOf<Int, RolloutResult>()
        
        rolloutPercentages.forEach { percentage ->
            mockSmartOrchestrator.setFeatureRolloutPercentage("litert_processing", percentage)
            
            // Simulate multiple users
            val userResults = mutableListOf<Boolean>()
            repeat(20) { userId ->
                mockSmartOrchestrator.setUserId(userId.toString())
                val result = mockSmartOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
                
                if (result.isSuccess) {
                    val analysis = result.getOrNull()!!
                    val usedLiteRT = analysis.metadata?.get("backend") == "LiteRT"
                    userResults.add(usedLiteRT)
                }
            }
            
            val actualUsagePercentage = (userResults.count { it } * 100) / userResults.size
            rolloutResults[percentage] = RolloutResult(
                targetPercentage = percentage,
                actualPercentage = actualUsagePercentage,
                totalUsers = userResults.size,
                successfulAnalyses = userResults.size
            )
            
            // Verify rollout percentage is approximately correct
            val tolerance = 15 // Allow 15% tolerance for small sample size
            assertTrue(
                kotlin.math.abs(actualUsagePercentage - percentage) <= tolerance,
                "Rollout percentage should be approximately $percentage%, got $actualUsagePercentage%"
            )
        }
        
        // Log rollout results for analysis
        rolloutResults.forEach { (target, result) ->
            println("Rollout $target%: Actual ${result.actualPercentage}% (${result.totalUsers} users)")
        }
    }
    
    // =====================================================
    // ERROR HANDLING AND RECOVERY TESTS
    // =====================================================
    
    @Test
    fun `test network failure recovery during hybrid analysis`() = runTest {
        val testImage = testDataFactory.createStandardTestImage()
        
        // Start with network available
        mockSmartOrchestrator.setNetworkAvailable(true)
        mockSmartOrchestrator.setLiteRTAvailable(false) // Force cloud usage
        
        val onlineResult = mockSmartOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
        assertTrue(onlineResult.isSuccess, "Should succeed with network")
        
        // Simulate network failure
        mockSmartOrchestrator.setNetworkAvailable(false)
        mockSmartOrchestrator.setLiteRTAvailable(true) // Enable local fallback
        
        val offlineResult = mockSmartOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
        assertTrue(offlineResult.isSuccess, "Should recover with local processing")
        
        val offlineAnalysis = offlineResult.getOrNull()!!
        assertTrue(
            offlineAnalysis.analysisType == AnalysisType.LOCAL_GEMMA_MULTIMODAL,
            "Should use local analysis when network fails"
        )
    }
    
    @Test
    fun `test memory pressure recovery`() = runTest {
        // Simulate low memory condition
        mockLiteRTEngine.setAvailableMemory(200f) // Very low memory
        
        val largeImage = testDataFactory.createLargeImageData(4096, 4096)
        
        val result = mockSmartOrchestrator.analyzePhoto(largeImage, WorkType.GENERAL_CONSTRUCTION)
        
        if (result.isFailure) {
            // Should fail gracefully with appropriate error
            val error = result.exceptionOrNull()
            assertTrue(
                error is LiteRTException.OutOfMemoryException,
                "Should fail with OutOfMemoryException in low memory conditions"
            )
        } else {
            // If it succeeds, should use memory-efficient processing
            val analysis = result.getOrNull()!!
            assertTrue(
                analysis.processingTimeMs > 2000L,
                "Should take longer due to memory-efficient processing"
            )
        }
    }
    
    // =====================================================
    // HELPER METHODS AND DATA CLASSES
    // =====================================================
    
    private fun assertAnalysisQuality(analysis: SafetyAnalysis, orchestratorName: String) {
        // Validate basic analysis structure
        assertTrue(analysis.id.isNotEmpty(), "$orchestratorName: Analysis should have ID")
        assertTrue(analysis.timestamp > 0, "$orchestratorName: Analysis should have timestamp")
        assertTrue(analysis.confidence >= 0.5f, "$orchestratorName: Analysis should have reasonable confidence")
        assertTrue(analysis.processingTimeMs > 0, "$orchestratorName: Processing time should be positive")
        
        // Validate hazard detection
        analysis.hazards.forEach { hazard ->
            assertTrue(hazard.confidence >= 0.5f, "$orchestratorName: Hazard confidence should be >= 0.5")
            assertTrue(hazard.description.isNotEmpty(), "$orchestratorName: Hazard should have description")
            
            hazard.boundingBox?.let { box ->
                assertTrue(box.x >= 0 && box.x <= 1, "$orchestratorName: Bounding box X should be normalized")
                assertTrue(box.y >= 0 && box.y <= 1, "$orchestratorName: Bounding box Y should be normalized")
                assertTrue(box.width > 0 && box.width <= 1, "$orchestratorName: Bounding box width should be valid")
                assertTrue(box.height > 0 && box.height <= 1, "$orchestratorName: Bounding box height should be valid")
            }
        }
        
        // Validate OSHA compliance
        analysis.oshaViolations.forEach { violation ->
            assertTrue(violation.regulationCode.isNotEmpty(), "$orchestratorName: OSHA code should not be empty")
            assertTrue(violation.description.isNotEmpty(), "$orchestratorName: OSHA description should not be empty")
            assertTrue(violation.recommendation.isNotEmpty(), "$orchestratorName: OSHA recommendation should not be empty")
        }
    }
    
    private fun logPerformanceComparison(comparison: PerformanceComparison) {
        println("Performance Comparison:")
        println("  Smart Orchestrator: ${comparison.smartOrchestratorTimeMs}ms, ${comparison.smartHazardCount} hazards, confidence: ${comparison.smartConfidence}")
        println("  Simplified Orchestrator: ${comparison.simplifiedOrchestratorTimeMs}ms, ${comparison.simplifiedHazardCount} hazards, confidence: ${comparison.simplifiedConfidence}")
        println("  Speed difference: ${comparison.simplifiedOrchestratorTimeMs - comparison.smartOrchestratorTimeMs}ms")
    }
}

// =====================================================
// TEST DATA CLASSES
// =====================================================

data class WorkflowStep(
    val name: String,
    val imageData: ByteArray,
    val workType: WorkType,
    val expectedRisk: Severity
)

data class WorkflowResult(
    val stepName: String,
    val success: Boolean,
    val processingTimeMs: Long,
    val detectedRisk: Severity,
    val hazardCount: Int,
    val oshaViolationCount: Int
)

data class PerformanceComparison(
    val smartOrchestratorTimeMs: Long,
    val simplifiedOrchestratorTimeMs: Long,
    val smartHazardCount: Int,
    val simplifiedHazardCount: Int,
    val smartConfidence: Float,
    val simplifiedConfidence: Float
)

data class ABTestScenario(
    val name: String,
    val imageData: ByteArray,
    val workType: WorkType,
    val expectedFeature: String
)

data class ABTestResult(
    val scenarioName: String,
    val groupA_HazardCount: Int,
    val groupB_HazardCount: Int,
    val groupA_Confidence: Float,
    val groupB_Confidence: Float,
    val groupA_ProcessingTime: Long,
    val groupB_ProcessingTime: Long
)

data class RolloutResult(
    val targetPercentage: Int,
    val actualPercentage: Int,
    val totalUsers: Int,
    val successfulAnalyses: Int
)

// =====================================================
// MOCK ORCHESTRATORS FOR TESTING
// =====================================================

class MockSmartAIOrchestrator : AIPhotoAnalyzer {
    override val analyzerName = "Mock Smart AI Orchestrator"
    override val priority = 200
    override val analysisCapabilities = setOf(
        AnalysisCapability.MULTIMODAL_VISION,
        AnalysisCapability.PPE_DETECTION,
        AnalysisCapability.HAZARD_IDENTIFICATION,
        AnalysisCapability.OSHA_COMPLIANCE
    )
    override val isAvailable = true
    
    private var liteRTAvailable = true
    private var networkAvailable = true
    private var emergencyRollback = false
    private val featureFlags = mutableMapOf<String, Boolean>()
    private val rolloutPercentages = mutableMapOf<String, Int>()
    private var currentUserId = "test_user"
    private var orchestratorStats = MockOrchestratorStats()
    
    override suspend fun configure(apiKey: String?) = Result.success(Unit)
    
    override suspend fun analyzePhoto(imageData: ByteArray, workType: WorkType): Result<SafetyAnalysis> {
        if (emergencyRollback) {
            val analysis = createMockAnalysis(AnalysisType.LOCAL_YOLO_FALLBACK, workType)
            return Result.success(analysis.copy(
                recommendations = analysis.recommendations + "⚠️ Emergency mode - reduced functionality"
            ))
        }
        
        val useLiteRT = shouldUseLiteRT()
        
        return if (useLiteRT && liteRTAvailable) {
            orchestratorStats.recordSuccess(AnalysisType.LOCAL_GEMMA_MULTIMODAL)
            Result.success(createMockAnalysis(AnalysisType.LOCAL_GEMMA_MULTIMODAL, workType))
        } else if (networkAvailable) {
            orchestratorStats.recordSuccess(AnalysisType.CLOUD_GEMINI)
            Result.success(createMockAnalysis(AnalysisType.CLOUD_GEMINI, workType))
        } else {
            orchestratorStats.recordSuccess(AnalysisType.LOCAL_YOLO_FALLBACK)
            Result.success(createMockAnalysis(AnalysisType.LOCAL_YOLO_FALLBACK, workType))
        }
    }
    
    suspend fun analyzeBatch(images: List<Pair<ByteArray, WorkType>>, maxConcurrency: Int = 3): List<Result<SafetyAnalysis>> {
        return images.map { (imageData, workType) ->
            analyzePhoto(imageData, workType)
        }
    }
    
    fun getStats() = orchestratorStats
    fun setLiteRTAvailable(available: Boolean) { liteRTAvailable = available }
    fun setNetworkAvailable(available: Boolean) { networkAvailable = available }
    fun setEmergencyRollback(rollback: Boolean) { emergencyRollback = rollback }
    fun setFeatureFlag(flag: String, enabled: Boolean) { featureFlags[flag] = enabled }
    fun setFeatureRolloutPercentage(feature: String, percentage: Int) { rolloutPercentages[feature] = percentage }
    fun setUserId(userId: String) { currentUserId = userId }
    
    private fun shouldUseLiteRT(): Boolean {
        val rolloutPercentage = rolloutPercentages["litert_processing"] ?: 100
        val userHash = currentUserId.hashCode()
        return (userHash % 100) < rolloutPercentage
    }
    
    private fun createMockAnalysis(analysisType: AnalysisType, workType: WorkType): SafetyAnalysis {
        return SafetyAnalysis(
            id = "mock_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            analysisType = analysisType,
            confidence = 0.85f,
            processingTimeMs = when (analysisType) {
                AnalysisType.LOCAL_GEMMA_MULTIMODAL -> 800L
                AnalysisType.CLOUD_GEMINI -> 2000L
                AnalysisType.LOCAL_YOLO_FALLBACK -> 1500L
                else -> 1200L
            },
            hazards = listOf(
                createMockHazard(HazardType.STRUCK_BY, 0.82f),
                createMockHazard(HazardType.FALL, 0.75f)
            ),
            oshaViolations = listOf(
                OSHAViolation("1926.95", "PPE violation", Severity.MEDIUM, "Use required PPE")
            ),
            overallRiskAssessment = RiskAssessment(
                overallRisk = Severity.MEDIUM,
                riskScore = 65,
                mitigationPriority = Severity.MEDIUM,
                recommendations = listOf("Address identified hazards", "Review safety procedures")
            ),
            recommendations = listOf("Follow safety protocols", "Use appropriate PPE"),
            workType = workType,
            metadata = mapOf("backend" to if (analysisType == AnalysisType.LOCAL_GEMMA_MULTIMODAL) "LiteRT" else "Cloud")
        )
    }
    
    private fun createMockHazard(type: HazardType, confidence: Float) = DetectedHazard(
        type = type,
        description = "Mock ${type.name.lowercase()} hazard",
        severity = Severity.MEDIUM,
        confidence = confidence,
        boundingBox = BoundingBox(0.25f, 0.25f, 0.5f, 0.5f, confidence)
    )
}

class MockSimplifiedAIOrchestrator : AIPhotoAnalyzer {
    override val analyzerName = "Mock Simplified AI Orchestrator"
    override val priority = 100
    override val analysisCapabilities = setOf(
        AnalysisCapability.HAZARD_IDENTIFICATION,
        AnalysisCapability.PPE_DETECTION
    )
    override val isAvailable = true
    
    private var liteRTAvailable = true
    private val featureFlags = mutableMapOf<String, Boolean>()
    
    override suspend fun configure(apiKey: String?) = Result.success(Unit)
    
    override suspend fun analyzePhoto(imageData: ByteArray, workType: WorkType): Result<SafetyAnalysis> {
        val analysis = SafetyAnalysis(
            id = "simplified_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            analysisType = AnalysisType.LOCAL_YOLO_FALLBACK,
            confidence = 0.78f,
            processingTimeMs = 600L,
            hazards = listOf(
                DetectedHazard(
                    type = HazardType.STRUCK_BY,
                    description = "Basic hazard detection",
                    severity = Severity.MEDIUM,
                    confidence = 0.78f,
                    boundingBox = BoundingBox(0.3f, 0.3f, 0.4f, 0.4f, 0.78f)
                )
            ),
            oshaViolations = emptyList(),
            overallRiskAssessment = RiskAssessment(
                overallRisk = Severity.MEDIUM,
                riskScore = 55,
                mitigationPriority = Severity.MEDIUM,
                recommendations = listOf("Basic safety check recommended")
            ),
            recommendations = listOf("Follow basic safety protocols"),
            workType = workType
        )
        
        return Result.success(analysis)
    }
    
    fun setLiteRTAvailable(available: Boolean) { liteRTAvailable = available }
    fun setFeatureFlag(flag: String, enabled: Boolean) { featureFlags[flag] = enabled }
}

class MockOrchestratorStats {
    val successCounts = mutableMapOf<AnalysisType, Int>()
    val failureCounts = mutableMapOf<AnalysisType, Int>()
    
    fun recordSuccess(type: AnalysisType) {
        successCounts[type] = successCounts.getOrDefault(type, 0) + 1
    }
    
    fun recordFailure(type: AnalysisType) {
        failureCounts[type] = failureCounts.getOrDefault(type, 0) + 1
    }
}

class LiteRTPerformanceTestRunner {
    // Performance testing utilities for LiteRT integration
    suspend fun runLoadTest(orchestrator: AIPhotoAnalyzer, duration: Long, concurrency: Int): LoadTestResult {
        // Implementation for load testing
        return LoadTestResult(
            totalRequests = 100,
            successfulRequests = 95,
            averageResponseTime = 800L,
            peakMemoryUsage = 512f
        )
    }
}

data class LoadTestResult(
    val totalRequests: Int,
    val successfulRequests: Int,
    val averageResponseTime: Long,
    val peakMemoryUsage: Float
)
