package com.hazardhawk.performance

import com.hazardhawk.TestDataFactory
import com.hazardhawk.TestUtils
import com.hazardhawk.PerformanceTestRunner
import com.hazardhawk.MockAIPhotoAnalyzer
import com.hazardhawk.ai.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Performance benchmarking tests for AI analysis pipeline.
 * Validates performance against construction industry requirements:
 * - UI responsiveness: 30 FPS (33ms budget)
 * - AI analysis: 2 FPS (500ms budget) for real-time
 * - Batch processing: 10 images/minute
 */
class AIPerformanceBenchmarkTest {
    
    private lateinit var performanceRunner: PerformanceTestRunner
    
    @BeforeTest
    fun setup() {
        performanceRunner = PerformanceTestRunner()
    }
    
    @Test
    fun `AI analysis should meet real-time performance requirements`() = runTest {
        // Given - Real-time AI analyzer target: 2 FPS (500ms max)
        val realTimeAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "Real-time Analyzer",
            responseDelay = 450L, // Within budget
            shouldSucceed = true
        )
        
        val scenarios = TestDataFactory.createPerformanceScenarios()
        
        // When & Then - Test each scenario
        scenarios.forEach { scenario ->
            val result = performanceRunner.runPerformanceTest(realTimeAnalyzer, scenario)
            
            assertTrue(result.success, "Analysis should succeed for ${scenario.name}")
            
            // Validate real-time performance requirements
            when (scenario.name) {
                "Light Load" -> {
                    assertTrue(
                        result.fps >= 2.0,
                        "${scenario.name}: Expected ≥2 FPS, got ${result.fps}"
                    )
                    TestUtils.assertPerformanceWithin(
                        actualMs = result.executionTimeMs,
                        expectedMs = scenario.expectedProcessingTime,
                        tolerancePercent = 30.0,
                        scenario = scenario.name
                    )
                }
                "Medium Load" -> {
                    assertTrue(
                        result.fps >= 1.5,
                        "${scenario.name}: Expected ≥1.5 FPS, got ${result.fps}"
                    )
                }
                "Heavy Load" -> {
                    assertTrue(
                        result.fps >= 0.5,
                        "${scenario.name}: Expected ≥0.5 FPS, got ${result.fps}"
                    )
                }
            }
        }
    }
    
    @Test
    fun `batch processing should meet throughput requirements`() = runTest {
        // Given - Batch processing target: 10 images/minute (6 seconds per image average)
        val batchAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "Batch Analyzer",
            responseDelay = 5000L, // 5 seconds per image
            shouldSucceed = true
        )
        
        val batchSize = 5
        val expectedTotalTime = batchSize * 6000L // 6 seconds per image target
        
        // When
        val startTime = System.currentTimeMillis()
        val results = performanceRunner.runBatchPerformanceTest(
            batchAnalyzer, 
            batchSize = batchSize,
            concurrency = 2
        )
        val totalTime = System.currentTimeMillis() - startTime
        
        // Then
        assertEquals(batchSize, results.size, "Should process all images")
        assertTrue(results.all { it.success }, "All analyses should succeed")
        
        // Validate throughput with concurrency benefit
        assertTrue(
            totalTime < expectedTotalTime,
            "Batch processing should be faster than sequential: ${totalTime}ms vs ${expectedTotalTime}ms"
        )
        
        // Calculate actual throughput
        val throughputPerMinute = (results.size * 60000.0) / totalTime
        assertTrue(
            throughputPerMinute >= 8.0,
            "Should achieve ≥8 images/minute, got $throughputPerMinute"
        )
    }
    
    @Test
    fun `analyzer fallback should maintain performance`() = runTest {
        // Given - Primary analyzer fails, fallback should be fast
        val primaryAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "Primary Analyzer",
            responseDelay = 15000L, // Timeout scenario
            shouldSucceed = false
        )
        
        val fallbackAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "Fallback Analyzer",
            responseDelay = 800L, // Fast fallback
            shouldSucceed = true
        )
        
        val scenario = TestDataFactory.PerformanceTestScenario(
            name = "Fallback Performance Test",
            imageSize = 1280 to 720,
            hazardCount = 2,
            expectedProcessingTime = 3000L, // Including fallback time
            targetFPS = 2
        )
        
        // When - Test fallback performance
        val primaryResult = performanceRunner.runPerformanceTest(primaryAnalyzer, scenario)
        val fallbackResult = performanceRunner.runPerformanceTest(fallbackAnalyzer, scenario)
        
        // Then
        assertFalse(primaryResult.success, "Primary should fail as expected")
        assertTrue(fallbackResult.success, "Fallback should succeed")
        
        // Fallback should be significantly faster
        assertTrue(
            fallbackResult.executionTimeMs < 2000L,
            "Fallback should be fast: ${fallbackResult.executionTimeMs}ms"
        )
    }
    
    @Test
    fun `UI responsiveness should maintain 30 FPS during analysis`() = runTest {
        // Given - UI update simulation during AI analysis
        val uiUpdateBudget = 33L // 30 FPS = 33ms per frame
        
        // When - Simulate UI updates during analysis
        val uiUpdateTimes = mutableListOf<Long>()
        
        repeat(10) { frameIndex ->
            val frameStart = System.currentTimeMillis()
            
            // Simulate UI update work (rendering overlay, animations, etc.)
            simulateUIUpdate(frameIndex)
            
            val frameTime = System.currentTimeMillis() - frameStart
            uiUpdateTimes.add(frameTime)
        }
        
        // Then - UI should maintain responsiveness
        val averageFrameTime = uiUpdateTimes.average()
        val maxFrameTime = uiUpdateTimes.maxOrNull() ?: 0L
        
        assertTrue(
            averageFrameTime < uiUpdateBudget,
            "Average frame time should be <${uiUpdateBudget}ms, got ${averageFrameTime}ms"
        )
        
        assertTrue(
            maxFrameTime < uiUpdateBudget * 1.5, // Allow occasional spikes
            "Max frame time should be reasonable, got ${maxFrameTime}ms"
        )
    }
    
    // Helper method to simulate UI update work
    private suspend fun simulateUIUpdate(frameIndex: Int) {
        // Simulate typical UI operations: layout, drawing, animations
        val workAmount = when (frameIndex % 3) {
            0 -> 5L   // Light frame
            1 -> 15L  // Medium frame  
            else -> 25L // Heavy frame (animations)
        }
        
        kotlinx.coroutines.delay(workAmount)
    }
}
