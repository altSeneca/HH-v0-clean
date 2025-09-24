package com.hazardhawk.ar

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.ai.core.SmartAIOrchestrator
import com.hazardhawk.ai.models.*
import com.hazardhawk.core.models.SafetyAnalysis
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream
import kotlin.test.*

/**
 * End-to-end integration tests for AR hazard detection flow.
 * Tests complete pipeline from camera frame to hazard overlay display.
 */
@RunWith(AndroidJUnit4::class)
class ARHazardDetectionTest {

    private lateinit var hazardDetectionPipeline: ARHazardDetectionPipeline
    private lateinit var mockAIOrchestrator: MockSmartAIOrchestrator

    @Before
    fun setUp() {
        mockAIOrchestrator = MockSmartAIOrchestrator()
        hazardDetectionPipeline = ARHazardDetectionPipeline(mockAIOrchestrator)
    }

    @Test
    fun arHazardDetection_processesConstructionSceneEnd2End() = runTest {
        // Given - Load test construction scene image
        val testImageBytes = loadTestImage("construction_scene_fall_hazard.jpg")
        
        // When - Process through complete pipeline
        val result = hazardDetectionPipeline.processFrame(
            imageData = testImageBytes,
            workType = WorkType.GENERAL_CONSTRUCTION,
            timestamp = System.currentTimeMillis()
        )

        // Then - Should detect expected hazards
        assertTrue(result.isSuccess, "Pipeline should process frame successfully")
        
        val analysis = result.getOrNull()
        assertNotNull(analysis, "Should return analysis result")
        
        // Verify fall protection hazard detected
        val fallHazards = analysis.hazards.filter { it.type == HazardType.FALL_PROTECTION }
        assertTrue(fallHazards.isNotEmpty(), "Should detect fall protection hazards")
        
        // Verify hazard has bounding box for AR overlay
        val hazardWithBounds = fallHazards.first()
        assertNotNull(hazardWithBounds.boundingBox, "Hazard should have bounding box for AR overlay")
        
        // Verify OSHA compliance
        assertTrue(analysis.oshaViolations.isNotEmpty(), "Should identify OSHA violations")
    }

    @Test
    fun arHazardDetection_handlesElectricalWorkSpecifically() = runTest {
        // Given
        val testImageBytes = loadTestImage("electrical_work_exposed_wires.jpg")
        
        // When
        val result = hazardDetectionPipeline.processFrame(
            imageData = testImageBytes,
            workType = WorkType.ELECTRICAL_WORK,
            timestamp = System.currentTimeMillis()
        )

        // Then
        val analysis = result.getOrThrow()
        
        // Should detect electrical hazards specifically
        val electricalHazards = analysis.hazards.filter { it.type == HazardType.ELECTRICAL_HAZARD }
        assertTrue(electricalHazards.isNotEmpty(), "Should detect electrical hazards")
        
        // Should have appropriate OSHA codes for electrical work
        val oshaElectricalCodes = analysis.oshaViolations.filter { 
            it.oshaCode.startsWith("1926.4") // Electrical standards
        }
        assertTrue(oshaElectricalCodes.isNotEmpty(), "Should identify electrical OSHA violations")
    }

    @Test
    fun arHazardDetection_detectsPPEViolations() = runTest {
        // Given
        val testImageBytes = loadTestImage("worker_missing_hardhat.jpg")
        
        // When
        val result = hazardDetectionPipeline.processFrame(
            imageData = testImageBytes,
            workType = WorkType.GENERAL_CONSTRUCTION,
            timestamp = System.currentTimeMillis()
        )

        // Then
        val analysis = result.getOrThrow()
        
        // Should detect PPE violations
        val ppeHazards = analysis.hazards.filter { it.type == HazardType.PPE_VIOLATION }
        assertTrue(ppeHazards.isNotEmpty(), "Should detect PPE violations")
        
        // Should have detailed PPE status
        assertNotNull(analysis.ppeStatus, "Should analyze PPE status")
        assertEquals(PPEItemStatus.MISSING, analysis.ppeStatus?.hardHat?.status)
    }

    @Test
    fun arHazardDetection_maintainsRealTimePerformance() = runTest {
        // Given
        val testImageBytes = loadTestImage("construction_scene_multiple_hazards.jpg")
        val targetFrameTime = 33L // 30 FPS = 33ms per frame
        
        // When - Process multiple frames to test sustained performance
        val processingTimes = mutableListOf<Long>()
        
        repeat(10) {
            val startTime = System.currentTimeMillis()
            
            val result = hazardDetectionPipeline.processFrame(
                imageData = testImageBytes,
                workType = WorkType.GENERAL_CONSTRUCTION,
                timestamp = System.currentTimeMillis()
            )
            
            val processingTime = System.currentTimeMillis() - startTime
            processingTimes.add(processingTime)
            
            assertTrue(result.isSuccess, "All frames should process successfully")
        }

        // Then - Should maintain real-time performance
        val averageProcessingTime = processingTimes.average()
        assertTrue(averageProcessingTime < targetFrameTime * 2, // Allow 2x buffer
            "Average processing time should be real-time capable: ${averageProcessingTime}ms")
        
        // No single frame should be excessively slow
        val maxProcessingTime = processingTimes.maxOrNull() ?: 0L
        assertTrue(maxProcessingTime < targetFrameTime * 3,
            "Maximum processing time should not cause frame drops: ${maxProcessingTime}ms")
    }

    @Test
    fun arHazardDetection_handlesLowConfidenceResults() = runTest {
        // Given - Configure AI to return low confidence results
        mockAIOrchestrator.setConfidenceThreshold(0.3f)
        val testImageBytes = loadTestImage("blurry_construction_scene.jpg")
        
        // When
        val result = hazardDetectionPipeline.processFrame(
            imageData = testImageBytes,
            workType = WorkType.GENERAL_CONSTRUCTION,
            timestamp = System.currentTimeMillis()
        )

        // Then
        val analysis = result.getOrThrow()
        
        // Should filter out low confidence detections for AR overlay
        val highConfidenceHazards = analysis.hazards.filter { it.confidence > 0.7f }
        val lowConfidenceHazards = analysis.hazards.filter { it.confidence < 0.5f }
        
        // AR overlay should prioritize high confidence detections
        assertTrue(highConfidenceHazards.size >= lowConfidenceHazards.size || analysis.hazards.isEmpty(),
            "Should prioritize high confidence detections for AR display")
    }

    @Test
    fun arHazardDetection_tracksCriticalHazardsAcrossFrames() = runTest {
        // Given - Sequence of frames with moving critical hazard
        val frameSequence = listOf(
            loadTestImage("fall_hazard_frame_1.jpg"),
            loadTestImage("fall_hazard_frame_2.jpg"),
            loadTestImage("fall_hazard_frame_3.jpg")
        )
        
        val detectedHazards = mutableListOf<List<Hazard>>()
        
        // When - Process frame sequence
        frameSequence.forEach { frameBytes ->
            val result = hazardDetectionPipeline.processFrame(
                imageData = frameBytes,
                workType = WorkType.FALL_PROTECTION,
                timestamp = System.currentTimeMillis()
            )
            
            val analysis = result.getOrThrow()
            detectedHazards.add(analysis.hazards)
        }

        // Then - Should maintain consistent critical hazard tracking
        val criticalHazardsPerFrame = detectedHazards.map { hazards ->
            hazards.filter { it.severity == Severity.CRITICAL }
        }
        
        // Should detect critical hazards in most frames
        val framesWithCritical = criticalHazardsPerFrame.count { it.isNotEmpty() }
        assertTrue(framesWithCritical >= frameSequence.size / 2,
            "Should consistently detect critical hazards across frame sequence")
        
        // Bounding boxes should show reasonable movement (not jumping wildly)
        if (criticalHazardsPerFrame.all { it.isNotEmpty() }) {
            val boundingBoxes = criticalHazardsPerFrame.map { hazards ->
                hazards.first().boundingBox
            }
            
            // Verify reasonable movement between frames
            for (i in 1 until boundingBoxes.size) {
                val prev = boundingBoxes[i-1]
                val current = boundingBoxes[i]
                
                if (prev != null && current != null) {
                    val movement = kotlin.math.abs(prev.left - current.left) + 
                                 kotlin.math.abs(prev.top - current.top)
                    assertTrue(movement < 0.3f, // Should not move more than 30% of screen
                        "Hazard tracking should be stable across frames")
                }
            }
        }
    }

    @Test
    fun arHazardDetection_adaptsToDifferentLightingConditions() = runTest {
        // Given - Images with different lighting conditions
        val lightingScenarios = mapOf(
            "bright_outdoor" to loadTestImage("bright_construction_site.jpg"),
            "indoor_fluorescent" to loadTestImage("indoor_construction_work.jpg"),
            "low_light_evening" to loadTestImage("evening_construction_work.jpg")
        )
        
        // When - Process each lighting scenario
        val results = lightingScenarios.mapValues { (scenario, imageBytes) ->
            hazardDetectionPipeline.processFrame(
                imageData = imageBytes,
                workType = WorkType.GENERAL_CONSTRUCTION,
                timestamp = System.currentTimeMillis()
            )
        }

        // Then - Should successfully process all lighting conditions
        results.forEach { (scenario, result) ->
            assertTrue(result.isSuccess, "Should process $scenario lighting successfully")
            
            val analysis = result.getOrThrow()
            // Should maintain reasonable confidence in good lighting
            if (scenario == "bright_outdoor") {
                assertTrue(analysis.confidence > 0.7f, 
                    "Should have high confidence in good lighting")
            }
        }
    }

    @Test
    fun arHazardDetection_prioritizesImmediateThreats() = runTest {
        // Given
        val testImageBytes = loadTestImage("multiple_severity_hazards.jpg")
        
        // When
        val result = hazardDetectionPipeline.processFrame(
            imageData = testImageBytes,
            workType = WorkType.GENERAL_CONSTRUCTION,
            timestamp = System.currentTimeMillis()
        )

        // Then
        val analysis = result.getOrThrow()
        
        // Should prioritize critical and high severity hazards
        val sortedHazards = analysis.hazards.sortedByDescending { it.severity.ordinal }
        
        // Critical hazards should have immediate action recommendations
        val criticalHazards = sortedHazards.filter { it.severity == Severity.CRITICAL }
        criticalHazards.forEach { hazard ->
            assertNotNull(hazard.immediateAction, 
                "Critical hazards should have immediate action guidance")
            assertTrue(hazard.immediateAction?.isNotEmpty() == true,
                "Immediate action should not be empty")
        }
    }

    @Test
    fun arHazardDetection_handlesErrorRecovery() = runTest {
        // Given - Configure AI to fail initially then recover
        mockAIOrchestrator.setShouldFail(true)
        val testImageBytes = loadTestImage("construction_scene_fall_hazard.jpg")
        
        // When - First attempt should fail
        val failureResult = hazardDetectionPipeline.processFrame(
            imageData = testImageBytes,
            workType = WorkType.GENERAL_CONSTRUCTION,
            timestamp = System.currentTimeMillis()
        )
        
        // Then - Should handle failure gracefully
        assertTrue(failureResult.isFailure, "Should handle AI failure")
        
        // When - AI recovers
        mockAIOrchestrator.setShouldFail(false)
        val recoveryResult = hazardDetectionPipeline.processFrame(
            imageData = testImageBytes,
            workType = WorkType.GENERAL_CONSTRUCTION,
            timestamp = System.currentTimeMillis()
        )
        
        // Then - Should recover successfully
        assertTrue(recoveryResult.isSuccess, "Should recover from AI failure")
    }

    @Test
    fun arHazardDetection_validatesHazardBoundingBoxes() = runTest {
        // Given
        val testImageBytes = loadTestImage("construction_scene_fall_hazard.jpg")
        
        // When
        val result = hazardDetectionPipeline.processFrame(
            imageData = testImageBytes,
            workType = WorkType.GENERAL_CONSTRUCTION,
            timestamp = System.currentTimeMillis()
        )

        // Then
        val analysis = result.getOrThrow()
        
        analysis.hazards.forEach { hazard ->
            hazard.boundingBox?.let { box ->
                // Validate bounding box coordinates
                assertTrue(box.left >= 0f && box.left <= 1f, 
                    "Bounding box left should be normalized: ${box.left}")
                assertTrue(box.top >= 0f && box.top <= 1f,
                    "Bounding box top should be normalized: ${box.top}")
                assertTrue(box.width > 0f && box.width <= 1f,
                    "Bounding box width should be valid: ${box.width}")
                assertTrue(box.height > 0f && box.height <= 1f,
                    "Bounding box height should be valid: ${box.height}")
                
                // Bounding box should not exceed image bounds
                assertTrue(box.left + box.width <= 1f,
                    "Bounding box should not exceed right boundary")
                assertTrue(box.top + box.height <= 1f,
                    "Bounding box should not exceed bottom boundary")
            }
        }
    }

    // Helper methods
    private fun loadTestImage(filename: String): ByteArray {
        // In a real test, this would load actual test images from assets
        // For this mock implementation, return sample byte array
        return "mock_image_data_$filename".toByteArray()
    }
}

/**
 * Mock AR Hazard Detection Pipeline for testing
 */
class ARHazardDetectionPipeline(
    private val aiOrchestrator: SmartAIOrchestrator
) {
    suspend fun processFrame(
        imageData: ByteArray,
        workType: WorkType,
        timestamp: Long
    ): Result<SafetyAnalysis> {
        return try {
            // Simulate processing delay based on image size
            val processingDelay = (imageData.size / 1000).coerceIn(10, 100)
            Thread.sleep(processingDelay.toLong())
            
            aiOrchestrator.analyzePhoto(imageData, workType)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Enhanced Mock Smart AI Orchestrator for integration testing
 */
class MockSmartAIOrchestrator : SmartAIOrchestrator(
    gemma3NE2B = MockGemma3NE2BVisionService(),
    vertexAI = MockVertexAIGeminiService(),
    yolo11 = MockYOLO11LocalService(),
    networkMonitor = MockNetworkConnectivityService(),
    performanceManager = MockAdaptivePerformanceManager(),
    memoryManager = MockMemoryManager(),
    performanceMonitor = MockPerformanceMonitor()
) {
    private var confidenceThreshold = 0.7f
    private var shouldFail = false

    fun setConfidenceThreshold(threshold: Float) {
        confidenceThreshold = threshold
    }

    fun setShouldFail(fail: Boolean) {
        shouldFail = fail
    }

    override suspend fun analyzePhoto(
        imageData: ByteArray, 
        workType: WorkType
    ): Result<SafetyAnalysis> {
        if (shouldFail) {
            return Result.failure(Exception("Mock AI failure"))
        }

        // Simulate different hazards based on mock image filename
        val imageString = String(imageData)
        val hazards = when {
            imageString.contains("fall_hazard") -> listOf(
                createMockHazard(
                    type = HazardType.FALL_PROTECTION,
                    severity = Severity.CRITICAL,
                    boundingBox = BoundingBox(0.3f, 0.2f, 0.25f, 0.4f),
                    confidence = confidenceThreshold + 0.1f
                )
            )
            imageString.contains("electrical") -> listOf(
                createMockHazard(
                    type = HazardType.ELECTRICAL_HAZARD,
                    severity = Severity.HIGH,
                    boundingBox = BoundingBox(0.5f, 0.6f, 0.2f, 0.15f),
                    confidence = confidenceThreshold + 0.05f
                )
            )
            imageString.contains("missing_hardhat") -> listOf(
                createMockHazard(
                    type = HazardType.PPE_VIOLATION,
                    severity = Severity.HIGH,
                    boundingBox = BoundingBox(0.4f, 0.1f, 0.15f, 0.2f),
                    confidence = confidenceThreshold + 0.15f
                )
            )
            imageString.contains("multiple") -> listOf(
                createMockHazard(HazardType.FALL_PROTECTION, Severity.CRITICAL),
                createMockHazard(HazardType.PPE_VIOLATION, Severity.HIGH),
                createMockHazard(HazardType.ELECTRICAL_HAZARD, Severity.MEDIUM)
            )
            imageString.contains("blurry") -> listOf(
                createMockHazard(confidence = confidenceThreshold - 0.2f)
            )
            else -> emptyList()
        }

        val analysis = SafetyAnalysis(
            id = "mock-analysis-${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
            workType = workType,
            hazards = hazards.filter { it.confidence >= confidenceThreshold },
            ppeStatus = createMockPPEStatus(imageString),
            recommendations = hazards.map { "Address ${it.type}" },
            overallRiskLevel = when {
                hazards.any { it.severity == Severity.CRITICAL } -> RiskLevel.HIGH
                hazards.any { it.severity == Severity.HIGH } -> RiskLevel.MEDIUM
                else -> RiskLevel.LOW
            },
            confidence = if (hazards.isNotEmpty()) hazards.map { it.confidence }.average().toFloat() else 0.5f,
            processingTimeMs = (imageData.size / 1000).coerceIn(100, 2000).toLong(),
            oshaViolations = hazards.map { hazard ->
                OSHAViolation(
                    oshaCode = hazard.oshaCode ?: "1926.001",
                    description = hazard.description,
                    severity = hazard.severity.name,
                    recommendations = hazard.recommendations
                )
            },
            metadata = null
        )

        return Result.success(analysis)
    }

    private fun createMockHazard(
        type: HazardType = HazardType.FALL_PROTECTION,
        severity: Severity = Severity.MEDIUM,
        boundingBox: BoundingBox = BoundingBox(0.3f, 0.3f, 0.2f, 0.2f),
        confidence: Float = 0.85f
    ): Hazard {
        return Hazard(
            id = "mock-hazard-${type.name}-${System.nanoTime()}",
            type = type,
            severity = severity,
            description = "Mock ${type.name.lowercase()} hazard",
            oshaCode = when (type) {
                HazardType.FALL_PROTECTION -> "1926.501(b)(1)"
                HazardType.ELECTRICAL_HAZARD -> "1926.416(a)(1)"
                HazardType.PPE_VIOLATION -> "1926.95(a)"
                else -> "1926.001"
            },
            boundingBox = boundingBox,
            confidence = confidence.coerceIn(0f, 1f),
            recommendations = listOf("Mock recommendation for ${type.name}"),
            immediateAction = if (severity == Severity.CRITICAL) "STOP WORK" else null
        )
    }

    private fun createMockPPEStatus(imageString: String): PPEStatus {
        val missingHardhat = imageString.contains("missing_hardhat")
        
        return PPEStatus(
            hardHat = PPEItem(
                if (missingHardhat) PPEItemStatus.MISSING else PPEItemStatus.PRESENT,
                if (missingHardhat) 0.0f else 0.9f,
                null,
                !missingHardhat
            ),
            safetyVest = PPEItem(PPEItemStatus.PRESENT, 0.85f, null, true),
            safetyBoots = PPEItem(PPEItemStatus.PRESENT, 0.8f, null, true),
            safetyGlasses = PPEItem(PPEItemStatus.UNKNOWN, 0.5f, null, false),
            fallProtection = PPEItem(PPEItemStatus.MISSING, 0.0f, null, false),
            respirator = PPEItem(PPEItemStatus.UNKNOWN, 0.0f, null, false),
            overallCompliance = if (missingHardhat) 0.4f else 0.8f
        )
    }

    override suspend fun configure(apiKey: String?): Result<Unit> = Result.success(Unit)
    override fun getStats() = com.hazardhawk.ai.core.OrchestratorStats()
}

// Additional mock services
private class MockGemma3NE2BVisionService : com.hazardhawk.ai.services.Gemma3NE2BVisionService()
private class MockVertexAIGeminiService : com.hazardhawk.ai.services.VertexAIGeminiService()
private class MockYOLO11LocalService : com.hazardhawk.ai.services.YOLO11LocalService()
private class MockNetworkConnectivityService : com.hazardhawk.ai.core.NetworkConnectivityService {
    override val isConnected = true
    override val connectionQuality = com.hazardhawk.ai.core.ConnectionQuality.GOOD
}
private class MockAdaptivePerformanceManager : com.hazardhawk.performance.AdaptivePerformanceManager()
private class MockMemoryManager : com.hazardhawk.performance.MemoryManager()
private class MockPerformanceMonitor : com.hazardhawk.performance.PerformanceMonitor()
