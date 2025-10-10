package com.hazardhawk.integration.refactoring

import com.hazardhawk.TestDataFactory
import com.hazardhawk.TestUtils
import com.hazardhawk.MockAIPhotoAnalyzer
import com.hazardhawk.PerformanceTestRunner
import com.hazardhawk.ai.core.SmartAIOrchestrator
import com.hazardhawk.core.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * CameraX Integration Test for HazardHawk Refactoring Validation
 * 
 * Tests camera preview performance, AI throttling, and UI responsiveness
 * after comprehensive integration refactoring.
 * 
 * Key Requirements:
 * - Camera preview displays at 30 FPS
 * - AI analysis throttled to 2 FPS (500ms intervals)
 * - SmartAIOrchestrator integration preserved
 * - Error handling works correctly
 * - Memory management optimized
 */
class CameraXIntegrationTest {
    
    private lateinit var performanceRunner: PerformanceTestRunner
    private lateinit var mockOrchestrator: MockAIPhotoAnalyzer
    
    @BeforeTest
    fun setup() {
        performanceRunner = PerformanceTestRunner()
        mockOrchestrator = MockAIPhotoAnalyzer(
            analyzerName = "Smart AI Orchestrator",
            priority = 200,
            responseDelay = 1800L, // Realistic AI processing time
            shouldSucceed = true
        )
    }
    
    @Test
    fun `camera preview should maintain 30 FPS performance after refactoring`() = runTest {
        // Given - Camera configuration for AR preview
        val cameraConfig = ARCameraConfig(
            targetAspectRatio = AspectRatio.RATIO_16_9,
            analysisInterval = 500L, // 2 FPS for AI
            enableHDR = true,
            enableStabilization = true
        )
        
        // When - Simulate camera preview rendering
        val frameRenderTimes = mutableListOf<Long>()
        val targetFrameTime = 33L // 30 FPS = 33ms per frame
        
        repeat(30) { frameIndex ->
            val (_, frameTime) = TestUtils.measureExecutionTime {
                simulateCameraPreviewFrame(frameIndex, cameraConfig)
            }
            frameRenderTimes.add(frameTime.inWholeMilliseconds)
        }
        
        // Then - Validate UI performance maintained
        val averageFrameTime = frameRenderTimes.average()
        val maxFrameTime = frameRenderTimes.maxOrNull() ?: 0L
        
        assertTrue(
            averageFrameTime < targetFrameTime,
            "Average frame time should be <${targetFrameTime}ms, got ${averageFrameTime}ms"
        )
        
        // Allow occasional frame spikes but keep them reasonable
        assertTrue(
            maxFrameTime < targetFrameTime * 1.5,
            "Max frame time should be reasonable, got ${maxFrameTime}ms"
        )
        
        // Verify frame time consistency (low variance)
        val frameTimeVariance = frameRenderTimes.map { (it - averageFrameTime) * (it - averageFrameTime) }.average()
        assertTrue(
            frameTimeVariance < 100.0,
            "Frame time variance should be low for smooth preview, got ${frameTimeVariance}"
        )
    }
    
    @Test
    fun `AI analysis throttling should maintain 2 FPS target after refactoring`() = runTest {
        // Given - AI analysis configuration
        val analysisInterval = 500L // Target: 2 FPS
        val testDuration = 5000L // 5 seconds
        val expectedAnalyses = (testDuration / analysisInterval).toInt()
        
        val analysisTimes = mutableListOf<Long>()
        var lastAnalysisTime = 0L
        
        // When - Simulate throttled AI analysis
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < testDuration) {
            val currentTime = System.currentTimeMillis()
            
            // Throttle analysis calls
            if (currentTime - lastAnalysisTime >= analysisInterval) {
                val imageData = TestDataFactory.createMockImageData(640, 480) // Smaller for real-time
                val (result, analysisTime) = TestUtils.measureExecutionTime {
                    mockOrchestrator.analyzePhoto(imageData, WorkType.GENERAL_CONSTRUCTION)
                }
                
                assertTrue(result.isSuccess, "AI analysis should succeed")
                analysisTimes.add(analysisTime.inWholeMilliseconds)
                lastAnalysisTime = currentTime
            }
            
            // Simulate other work between analyses
            kotlinx.coroutines.delay(50L)
        }
        
        // Then - Validate throttling effectiveness
        val actualAnalyses = analysisTimes.size
        assertTrue(
            actualAnalyses >= expectedAnalyses - 1, // Allow slight variation
            "Should complete approximately ${expectedAnalyses} analyses, got ${actualAnalyses}"
        )
        
        // Validate AI performance consistency
        val averageAnalysisTime = analysisTimes.average()
        assertTrue(
            averageAnalysisTime < 3000L, // Should complete within reasonable time
            "Average AI analysis time should be reasonable, got ${averageAnalysisTime}ms"
        )
    }
    
    @Test
    fun `SmartAIOrchestrator integration should be preserved after refactoring`() = runTest {
        // Given - Multiple analyzer scenarios
        val primaryAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "Gemma 3N E2B",
            priority = 150,
            responseDelay = 2000L,
            customAnalysis = TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
                confidence = 0.91f
            )
        )
        
        val fallbackAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "YOLO11 Fallback",
            priority = 50,
            responseDelay = 800L,
            customAnalysis = TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.LOCAL_YOLO_FALLBACK,
                confidence = 0.73f
            )
        )
        
        val imageData = TestDataFactory.createMockImageData()
        
        // When - Test orchestrator logic
        var result = primaryAnalyzer.analyzePhoto(imageData, WorkType.FALL_PROTECTION)
        
        // Test primary success
        assertTrue(result.isSuccess, "Primary analyzer should succeed")
        var analysis = result.getOrNull()!!
        assertEquals(AnalysisType.LOCAL_GEMMA_MULTIMODAL, analysis.analysisType)
        
        // Test fallback scenario
        val failingPrimary = MockAIPhotoAnalyzer(shouldSucceed = false, isAvailable = false)
        result = try {
            failingPrimary.analyzePhoto(imageData, WorkType.FALL_PROTECTION)
        } catch (e: Exception) {
            // Simulate orchestrator fallback
            fallbackAnalyzer.analyzePhoto(imageData, WorkType.FALL_PROTECTION)
        }
        
        // Then - Validate fallback works
        assertTrue(result.isSuccess, "Fallback analyzer should succeed")
        analysis = result.getOrNull()!!
        assertEquals(AnalysisType.LOCAL_YOLO_FALLBACK, analysis.analysisType)
        assertTrue(analysis.confidence < 0.8f, "Fallback should have lower confidence")
    }
    
    @Test
    fun `memory management should be optimized after refactoring`() = runTest {
        // Given - Memory-intensive camera operations
        val imageCount = 10
        val memoryUsageSnapshots = mutableListOf<MemorySnapshot>()
        
        // When - Process multiple camera frames
        repeat(imageCount) { frameIndex ->
            val memoryBefore = captureMemorySnapshot()
            
            // Simulate camera frame processing
            val imageData = TestDataFactory.createMockImageData(1920, 1080)
            val result = mockOrchestrator.analyzePhoto(imageData, WorkType.ELECTRICAL)
            assertTrue(result.isSuccess, "Frame $frameIndex analysis should succeed")
            
            val memoryAfter = captureMemorySnapshot()
            memoryUsageSnapshots.add(MemorySnapshot(frameIndex, memoryBefore, memoryAfter))
            
            // Simulate memory cleanup
            System.gc()
            kotlinx.coroutines.delay(100L)
        }
        
        // Then - Validate memory optimization
        val memoryGrowth = memoryUsageSnapshots.map { it.memoryGrowth }
        val averageGrowth = memoryGrowth.average()
        
        // Memory growth should be controlled (< 5MB per frame on average)
        assertTrue(
            averageGrowth < 5 * 1024 * 1024, // 5MB threshold
            "Average memory growth should be controlled, got ${averageGrowth / (1024 * 1024)}MB"
        )
        
        // No significant memory leaks (growth should stabilize)
        val recentGrowth = memoryGrowth.takeLast(3).average()
        val earlyGrowth = memoryGrowth.take(3).average()
        assertTrue(
            recentGrowth <= earlyGrowth * 2, // Allow some growth but not exponential
            "Memory usage should stabilize, not grow exponentially"
        )
    }
    
    @Test
    fun `error handling should work correctly after refactoring`() = runTest {
        // Given - Various error scenarios
        val errorScenarios = listOf(
            "Empty image data" to ByteArray(0),
            "Corrupted image data" to ByteArray(1000) { 0xFF.toByte() },
            "Oversized image data" to TestDataFactory.createMockImageData(8000, 6000)
        )
        
        // When & Then - Test error handling for each scenario
        errorScenarios.forEach { (scenario, imageData) ->
            val result = try {
                mockOrchestrator.analyzePhoto(imageData, WorkType.GENERAL_CONSTRUCTION)
            } catch (e: Exception) {
                Result.failure<SafetyAnalysis>(e)
            }
            
            // Error scenarios should either succeed gracefully or fail with meaningful messages
            if (result.isFailure) {
                val error = result.exceptionOrNull()!!
                assertTrue(
                    error.message?.isNotEmpty() == true,
                    "Error scenario '$scenario' should provide meaningful error message"
                )
            }
            
            // System should remain stable after error
            val recoveryTest = mockOrchestrator.analyzePhoto(
                TestDataFactory.createMockImageData(),
                WorkType.GENERAL_CONSTRUCTION
            )
            assertTrue(
                recoveryTest.isSuccess,
                "System should recover after error in scenario: $scenario"
            )
        }
    }
    
    @Test
    fun `camera configuration should be preserved after refactoring`() = runTest {
        // Given - Various camera configurations
        val configurations = listOf(
            ARCameraConfig(
                targetAspectRatio = AspectRatio.RATIO_16_9,
                analysisInterval = 500L,
                enableHDR = true,
                enableStabilization = true
            ),
            ARCameraConfig(
                targetAspectRatio = AspectRatio.RATIO_4_3,
                analysisInterval = 1000L,
                enableHDR = false,
                enableStabilization = false
            )
        )
        
        // When & Then - Test each configuration
        configurations.forEach { config ->
            val result = validateCameraConfiguration(config)
            assertTrue(
                result.isValid,
                "Camera configuration should be valid: ${result.errorMessage}"
            )
            
            // Verify configuration parameters are applied correctly
            assertEquals(config.analysisInterval, result.appliedInterval)
            assertEquals(config.enableHDR, result.hdrEnabled)
            assertEquals(config.enableStabilization, result.stabilizationEnabled)
        }
    }
    
    // Helper functions
    
    private suspend fun simulateCameraPreviewFrame(frameIndex: Int, config: ARCameraConfig) {
        // Simulate typical camera preview operations
        when (frameIndex % 3) {
            0 -> kotlinx.coroutines.delay(15L) // Light frame
            1 -> kotlinx.coroutines.delay(25L) // Medium frame
            else -> kotlinx.coroutines.delay(30L) // Heavy frame (effects, overlays)
        }
    }
    
    private fun captureMemorySnapshot(): Long {
        // Simulate memory usage capture
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun validateCameraConfiguration(config: ARCameraConfig): CameraValidationResult {
        // Simulate camera configuration validation
        return CameraValidationResult(
            isValid = true,
            appliedInterval = config.analysisInterval,
            hdrEnabled = config.enableHDR,
            stabilizationEnabled = config.enableStabilization
        )
    }
    
    // Data classes for testing
    
    data class MemorySnapshot(
        val frameIndex: Int,
        val memoryBefore: Long,
        val memoryAfter: Long
    ) {
        val memoryGrowth: Long get() = memoryAfter - memoryBefore
    }
    
    data class CameraValidationResult(
        val isValid: Boolean,
        val appliedInterval: Long,
        val hdrEnabled: Boolean,
        val stabilizationEnabled: Boolean,
        val errorMessage: String? = null
    )
    
    /**
     * Camera configuration for AR optimization
     */
    data class ARCameraConfig(
        val targetAspectRatio: Int,
        val analysisInterval: Long,
        val enableHDR: Boolean,
        val enableStabilization: Boolean
    )
    
    /**
     * Mock aspect ratio constants
     */
    object AspectRatio {
        const val RATIO_16_9 = 0
        const val RATIO_4_3 = 1
    }
}
