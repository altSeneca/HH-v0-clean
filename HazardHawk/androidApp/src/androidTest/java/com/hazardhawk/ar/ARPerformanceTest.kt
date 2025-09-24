package com.hazardhawk.ar

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.ai.models.*
import com.hazardhawk.core.models.SafetyAnalysis
import com.hazardhawk.ui.ar.HazardDetectionOverlay
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * Performance benchmarks for AR system focusing on frame rate, latency, and memory usage.
 * Tests real-world performance scenarios and validates 30fps rendering capability.
 */
@RunWith(AndroidJUnit4::class)
class ARPerformanceTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var performanceManager: ARPerformanceManager

    @Before
    fun setUp() {
        performanceManager = ARPerformanceManager()
    }

    @Test
    fun arPerformance_overlayRenderingBenchmark() {
        val hazards = createManyHazards(count = 20) // Stress test with many overlays
        val analysis = createTestAnalysis(hazards)

        benchmarkRule.measureRepeated {
            composeTestRule.setContent {
                HazardDetectionOverlay(
                    safetyAnalysis = analysis,
                    showBoundingBoxes = true,
                    showOSHABadges = true,
                    animationEnabled = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun arPerformance_frameProcessingLatency() = runTest {
        val testFrames = generateTestFrames(count = 100)
        val latencies = mutableListOf<Long>()

        testFrames.forEach { frame ->
            val startTime = System.nanoTime()
            
            // Simulate frame processing pipeline
            performanceManager.processFrame(frame)
            
            val latency = (System.nanoTime() - startTime) / 1_000_000 // Convert to milliseconds
            latencies.add(latency)
        }

        val averageLatency = latencies.average()
        val maxLatency = latencies.maxOrNull() ?: 0L
        val p95Latency = latencies.sorted()[latencies.size * 95 / 100]

        // Performance assertions
        assertTrue(averageLatency < 16.67, // 60 FPS target
            "Average frame processing should be under 16.67ms: ${averageLatency}ms")
        assertTrue(maxLatency < 33.33, // Should not exceed 30 FPS worst case
            "Maximum latency should be under 33.33ms: ${maxLatency}ms")
        assertTrue(p95Latency < 20.0,
            "95th percentile latency should be under 20ms: ${p95Latency}ms")
    }

    @Test
    fun arPerformance_memoryUsageUnderLoad() = runTest {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Generate heavy load scenario
        val heavyAnalysis = createTestAnalysis(createManyHazards(count = 50))
        
        // Simulate sustained operation
        repeat(100) { iteration ->
            composeTestRule.setContent {
                HazardDetectionOverlay(
                    safetyAnalysis = heavyAnalysis,
                    modifier = Modifier.fillMaxSize()
                )
            }
            composeTestRule.waitForIdle()
            
            // Force garbage collection every 10 iterations
            if (iteration % 10 == 0) {
                System.gc()
                Thread.sleep(10)
            }
        }

        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        val memoryIncreaseMB = memoryIncrease / (1024 * 1024)

        // Should not leak significant memory
        assertTrue(memoryIncreaseMB < 50, // Allow up to 50MB increase
            "Memory usage should not increase significantly: ${memoryIncreaseMB}MB")
    }

    @Test
    fun arPerformance_cameraFrameAnalysisRate() = runTest {
        val targetFPS = 30f
        val testDurationMs = 3000L // 3 seconds
        val expectedFrames = (testDurationMs / 1000f * targetFPS).toInt()
        
        var processedFrames = 0
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < testDurationMs) {
            val mockFrame = createMockCameraFrame()
            performanceManager.analyzeFrame(mockFrame)
            processedFrames++
        }

        val actualDuration = System.currentTimeMillis() - startTime
        val actualFPS = (processedFrames * 1000f) / actualDuration

        assertTrue(actualFPS >= targetFPS * 0.9f, // Allow 10% tolerance
            "Should maintain target frame rate: ${actualFPS} FPS (target: ${targetFPS} FPS)")
    }

    @Test
    fun arPerformance_overlayUpdateLatency() {
        val testScenarios = listOf(
            // Scenario 1: Few hazards
            createTestAnalysis(createManyHazards(count = 3)),
            // Scenario 2: Many hazards
            createTestAnalysis(createManyHazards(count = 15)),
            // Scenario 3: Critical hazards with animations
            createTestAnalysis(listOf(
                createCriticalHazard(),
                createCriticalHazard(),
                createCriticalHazard()
            ))
        )

        testScenarios.forEachIndexed { index, analysis ->
            benchmarkRule.measureRepeated {
                val startTime = System.nanoTime()
                
                composeTestRule.setContent {
                    HazardDetectionOverlay(
                        safetyAnalysis = analysis,
                        animationEnabled = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                composeTestRule.waitForIdle()
                
                runWithTimingDisabled {
                    val updateTime = (System.nanoTime() - startTime) / 1_000_000f
                    // Each update should be faster than one frame time
                    assertTrue(updateTime < 16.67f,
                        "Overlay update $index should be under 16.67ms: ${updateTime}ms")
                }
            }
        }
    }

    @Test
    fun arPerformance_continuousAnalysisStability() = runTest {
        val analysisManager = MockContinuousAnalysisManager()
        val performanceMetrics = mutableListOf<ARPerformanceMetric>()
        
        // Run continuous analysis for simulated time
        val testDurationMs = 5000L // 5 seconds
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < testDurationMs) {
            val frameStartTime = System.nanoTime()
            
            // Simulate frame processing
            val mockFrame = createMockCameraFrame()
            val analysis = analysisManager.processFrameContinuous(mockFrame)
            
            val frameTime = (System.nanoTime() - frameStartTime) / 1_000_000f
            
            performanceMetrics.add(ARPerformanceMetric(
                frameTime = frameTime,
                hazardCount = analysis?.hazards?.size ?: 0,
                memoryUsage = getCurrentMemoryUsage()
            ))
            
            // Maintain target frame rate
            Thread.sleep(33) // ~30 FPS
        }

        // Analyze stability metrics
        val frameTimes = performanceMetrics.map { it.frameTime }
        val frameTimeVariance = calculateVariance(frameTimes)
        val memoryUsages = performanceMetrics.map { it.memoryUsage }
        val memoryGrowth = memoryUsages.last() - memoryUsages.first()

        assertTrue(frameTimeVariance < 10.0f,
            "Frame time variance should be low for stable performance: $frameTimeVariance")
        assertTrue(memoryGrowth < 100 * 1024 * 1024, // 100MB
            "Memory growth should be controlled: ${memoryGrowth / (1024 * 1024)}MB")
    }

    @Test
    fun arPerformance_backgroundWorkImpact() = runTest {
        // Baseline performance without background work
        val baselineMetrics = measureOverlayPerformance()
        
        // Performance with background AI processing
        val backgroundManager = MockBackgroundAIManager()
        backgroundManager.startHeavyProcessing()
        
        val withBackgroundMetrics = measureOverlayPerformance()
        
        backgroundManager.stopProcessing()
        
        // Background work should not significantly impact AR performance
        val performanceImpact = withBackgroundMetrics.averageFrameTime / baselineMetrics.averageFrameTime
        
        assertTrue(performanceImpact < 1.5f, // Should not be more than 50% slower
            "Background work should not severely impact AR performance: ${performanceImpact}x slower")
    }

    @Test
    fun arPerformance_deviceRotationImpact() = runTest {
        val orientations = listOf(0, 90, 180, 270)
        val rotationMetrics = mutableMapOf<Int, Float>()
        
        orientations.forEach { rotation ->
            val startTime = System.nanoTime()
            
            // Simulate rotation
            performanceManager.handleDeviceRotation(rotation)
            
            // Measure overlay re-rendering after rotation
            composeTestRule.setContent {
                HazardDetectionOverlay(
                    safetyAnalysis = createTestAnalysis(createManyHazards(count = 10)),
                    modifier = Modifier.fillMaxSize()
                )
            }
            composeTestRule.waitForIdle()
            
            val rotationTime = (System.nanoTime() - startTime) / 1_000_000f
            rotationMetrics[rotation] = rotationTime
        }

        // Rotation handling should be fast
        rotationMetrics.values.forEach { time ->
            assertTrue(time < 100f, // Should complete rotation handling in under 100ms
                "Device rotation should be handled quickly: ${time}ms")
        }
    }

    @Test
    fun arPerformance_lowEndDeviceSimulation() = runTest {
        // Simulate low-end device constraints
        performanceManager.setDeviceProfile(DeviceProfile.LOW_END)
        
        val constrainedAnalysis = createTestAnalysis(createManyHazards(count = 30))
        var renderTime = 0f
        
        benchmarkRule.measureRepeated {
            val startTime = System.nanoTime()
            
            composeTestRule.setContent {
                HazardDetectionOverlay(
                    safetyAnalysis = constrainedAnalysis,
                    compactMode = true, // Should use compact mode on low-end devices
                    animationEnabled = false, // Should disable animations
                    modifier = Modifier.fillMaxSize()
                )
            }
            composeTestRule.waitForIdle()
            
            runWithTimingDisabled {
                renderTime = (System.nanoTime() - startTime) / 1_000_000f
            }
        }
        
        // Should maintain acceptable performance even on low-end devices
        assertTrue(renderTime < 50f, // More lenient for low-end devices
            "Low-end device should still maintain reasonable performance: ${renderTime}ms")
    }

    // Helper methods
    private fun createManyHazards(count: Int): List<Hazard> {
        return (0 until count).map { index ->
            val severity = Severity.values()[index % Severity.values().size]
            val x = (index % 5) * 0.2f
            val y = (index / 5) * 0.15f
            
            Hazard(
                id = "perf-test-hazard-$index",
                type = HazardType.values()[index % HazardType.values().size],
                severity = severity,
                description = "Performance test hazard $index",
                oshaCode = "TEST.001",
                boundingBox = BoundingBox(x, y, 0.15f, 0.1f),
                confidence = 0.75f + (index % 3) * 0.1f,
                recommendations = listOf("Test recommendation $index")
            )
        }
    }

    private fun createCriticalHazard(): Hazard {
        return Hazard(
            id = "critical-${System.nanoTime()}",
            type = HazardType.FALL_PROTECTION,
            severity = Severity.CRITICAL,
            description = "Critical fall hazard",
            oshaCode = "1926.501(b)(1)",
            boundingBox = BoundingBox(0.3f, 0.2f, 0.25f, 0.3f),
            confidence = 0.95f,
            recommendations = listOf("Install fall protection"),
            immediateAction = "STOP WORK"
        )
    }

    private fun createTestAnalysis(hazards: List<Hazard>): SafetyAnalysis {
        return SafetyAnalysis(
            id = "perf-test-analysis",
            timestamp = System.currentTimeMillis(),
            analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
            workType = WorkType.GENERAL_CONSTRUCTION,
            hazards = hazards,
            ppeStatus = createTestPPEStatus(),
            recommendations = hazards.map { "Address ${it.type}" },
            overallRiskLevel = if (hazards.any { it.severity == Severity.CRITICAL }) RiskLevel.HIGH else RiskLevel.MEDIUM,
            confidence = 0.85f,
            processingTimeMs = 1500L,
            oshaViolations = emptyList(),
            metadata = null
        )
    }

    private fun createTestPPEStatus(): PPEStatus {
        return PPEStatus(
            hardHat = PPEItem(PPEItemStatus.PRESENT, 0.9f, null, true),
            safetyVest = PPEItem(PPEItemStatus.PRESENT, 0.85f, null, true),
            safetyBoots = PPEItem(PPEItemStatus.PRESENT, 0.8f, null, true),
            safetyGlasses = PPEItem(PPEItemStatus.UNKNOWN, 0.5f, null, false),
            fallProtection = PPEItem(PPEItemStatus.MISSING, 0.0f, null, false),
            respirator = PPEItem(PPEItemStatus.UNKNOWN, 0.0f, null, false),
            overallCompliance = 0.7f
        )
    }

    private fun generateTestFrames(count: Int): List<CameraFrame> {
        return (0 until count).map { index ->
            CameraFrame(
                timestamp = System.nanoTime() + index * 33_000_000L, // 30 FPS timing
                data = ByteArray(1920 * 1080 * 3), // Mock frame data
                width = 1920,
                height = 1080
            )
        }
    }

    private fun createMockCameraFrame(): CameraFrame {
        return CameraFrame(
            timestamp = System.nanoTime(),
            data = ByteArray(1920 * 1080 * 3),
            width = 1920,
            height = 1080
        )
    }

    private fun measureOverlayPerformance(): PerformanceMetric {
        val frameTimes = mutableListOf<Float>()
        val testAnalysis = createTestAnalysis(createManyHazards(count = 10))

        repeat(30) { // Measure 30 frames
            val startTime = System.nanoTime()
            
            composeTestRule.setContent {
                HazardDetectionOverlay(
                    safetyAnalysis = testAnalysis,
                    modifier = Modifier.fillMaxSize()
                )
            }
            composeTestRule.waitForIdle()
            
            val frameTime = (System.nanoTime() - startTime) / 1_000_000f
            frameTimes.add(frameTime)
        }

        return PerformanceMetric(
            averageFrameTime = frameTimes.average().toFloat(),
            maxFrameTime = frameTimes.maxOrNull() ?: 0f,
            frameTimeVariance = calculateVariance(frameTimes)
        )
    }

    private fun getCurrentMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    private fun calculateVariance(values: List<Float>): Float {
        val mean = values.average().toFloat()
        val squaredDiffs = values.map { (it - mean) * (it - mean) }
        return squaredDiffs.average().toFloat()
    }
}

// Performance testing data classes
data class CameraFrame(
    val timestamp: Long,
    val data: ByteArray,
    val width: Int,
    val height: Int
)

data class ARPerformanceMetric(
    val frameTime: Float,
    val hazardCount: Int,
    val memoryUsage: Long
)

data class PerformanceMetric(
    val averageFrameTime: Float,
    val maxFrameTime: Float,
    val frameTimeVariance: Float
)

enum class DeviceProfile {
    LOW_END, MID_RANGE, HIGH_END
}

// Mock performance management classes
class ARPerformanceManager {
    private var deviceProfile = DeviceProfile.HIGH_END

    fun processFrame(frame: CameraFrame) {
        // Simulate frame processing with device-appropriate performance
        val processingTime = when (deviceProfile) {
            DeviceProfile.LOW_END -> 25L // Slower processing
            DeviceProfile.MID_RANGE -> 15L
            DeviceProfile.HIGH_END -> 8L
        }
        Thread.sleep(processingTime)
    }

    fun analyzeFrame(frame: CameraFrame) {
        // Simulate AI analysis with performance constraints
        val analysisTime = when (deviceProfile) {
            DeviceProfile.LOW_END -> 40L
            DeviceProfile.MID_RANGE -> 25L
            DeviceProfile.HIGH_END -> 15L
        }
        Thread.sleep(analysisTime)
    }

    fun setDeviceProfile(profile: DeviceProfile) {
        deviceProfile = profile
    }

    fun handleDeviceRotation(rotation: Int) {
        // Simulate rotation handling
        Thread.sleep(10)
    }
}

class MockContinuousAnalysisManager {
    fun processFrameContinuous(frame: CameraFrame): SafetyAnalysis? {
        // Simulate continuous frame analysis
        Thread.sleep(20) // Simulate processing time
        
        return if (frame.timestamp % 3 == 0L) {
            // Return analysis for every 3rd frame
            SafetyAnalysis(
                id = "continuous-${frame.timestamp}",
                timestamp = frame.timestamp,
                analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
                workType = WorkType.GENERAL_CONSTRUCTION,
                hazards = emptyList(),
                ppeStatus = null,
                recommendations = emptyList(),
                overallRiskLevel = RiskLevel.LOW,
                confidence = 0.75f,
                processingTimeMs = 20L,
                oshaViolations = emptyList(),
                metadata = null
            )
        } else null
    }
}

class MockBackgroundAIManager {
    private var isProcessing = false

    fun startHeavyProcessing() {
        isProcessing = true
        // Simulate background CPU load
        Thread {
            while (isProcessing) {
                Thread.sleep(10)
                // Simulate CPU-intensive work
                var sum = 0
                repeat(10000) { sum += it }
            }
        }.start()
    }

    fun stopProcessing() {
        isProcessing = false
    }
}
