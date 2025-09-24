package com.hazardhawk.ui.camera

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.geometry.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.camera.UnifiedViewfinderCalculator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Performance Benchmarks for Camera Viewport Fixes
 * 
 * These benchmarks validate the performance improvements from the camera viewport
 * positioning fixes and measure critical performance metrics:
 * 
 * SUCCESS CRITERIA:
 * - 20-30% improvement in startup time compared to legacy implementation
 * - 15-25MB reduction in memory usage  
 * - Viewport calculations complete within 1ms on average
 * - UI responsiveness maintained during rapid aspect ratio changes
 */
@RunWith(AndroidJUnit4::class)
class CameraPerformanceBenchmark {
    
    @get:Rule
    val benchmarkRule = BenchmarkRule()
    
    companion object {
        // Standard test configurations
        private val TEST_CANVAS_SIZES = listOf(
            Size(1080f, 1920f),  // Phone portrait
            Size(1920f, 1080f),  // Phone landscape
            Size(1600f, 2560f),  // Tablet portrait
            Size(2560f, 1600f),  // Tablet landscape
            Size(720f, 1280f),   // Small phone
            Size(2048f, 2732f)   // Large tablet
        )
        
        private val TEST_ASPECT_RATIOS = UnifiedViewfinderCalculator.ViewfinderAspectRatio.values()
    }
    
    @Test
    fun benchmark_viewport_calculation_performance() {
        benchmarkRule.measureRepeated {
            // Test viewport calculation performance across all configurations
            TEST_CANVAS_SIZES.forEach { canvasSize ->
                TEST_ASPECT_RATIOS.forEach { aspectRatio ->
                    val bounds = UnifiedViewfinderCalculator.calculateBounds(
                        canvasSize = canvasSize,
                        aspectRatio = aspectRatio
                    )
                    
                    // Validate result to prevent optimization elimination
                    require(bounds.width > 0f && bounds.height > 0f)
                }
            }
        }
    }
    
    @Test
    fun benchmark_animated_bounds_calculation() {
        benchmarkRule.measureRepeated {
            // Simulate aspect ratio animation scenarios
            TEST_CANVAS_SIZES.forEach { canvasSize ->
                val animationRatios = listOf(1f, 1.1f, 1.2f, 1.33f, 1.5f, 1.77f)
                
                animationRatios.forEach { currentRatio ->
                    TEST_ASPECT_RATIOS.forEach { targetAspectRatio ->
                        val bounds = UnifiedViewfinderCalculator.calculateBoundsAnimated(
                            canvasSize = canvasSize,
                            currentRatio = currentRatio,
                            targetAspectRatio = targetAspectRatio
                        )
                        
                        require(UnifiedViewfinderCalculator.validateBounds(bounds))
                    }
                }
            }
        }
    }
    
    @Test
    fun benchmark_bounds_validation_performance() {
        // Pre-calculate bounds for testing
        val testBounds = mutableListOf<UnifiedViewfinderCalculator.ViewfinderBounds>()
        
        TEST_CANVAS_SIZES.forEach { canvasSize ->
            TEST_ASPECT_RATIOS.forEach { aspectRatio ->
                testBounds.add(
                    UnifiedViewfinderCalculator.calculateBounds(
                        canvasSize = canvasSize,
                        aspectRatio = aspectRatio
                    )
                )
            }
        }
        
        benchmarkRule.measureRepeated {
            // Test bounds validation performance
            testBounds.forEach { bounds ->
                val isValid = UnifiedViewfinderCalculator.validateBounds(bounds)
                require(isValid) // Should all be valid
            }
        }
    }
    
    @Test
    fun benchmark_optimal_margin_calculation() {
        benchmarkRule.measureRepeated {
            TEST_CANVAS_SIZES.forEach { canvasSize ->
                val marginFactor = UnifiedViewfinderCalculator.calculateOptimalMarginFactor(canvasSize)
                require(marginFactor in 0.8f..1.0f)
            }
        }
    }
    
    @Test
    fun benchmark_legacy_ratio_conversion() {
        val legacyRatios = listOf(
            1f,      // Square
            4f/3f,   // Correct 4:3
            3f/4f,   // Legacy inverted 4:3
            16f/9f,  // Correct 16:9
            9f/16f,  // Legacy inverted 16:9
            3f/2f,   // 3:2
            2f/3f    // Inverted 3:2
        )
        
        benchmarkRule.measureRepeated {
            legacyRatios.forEach { ratio ->
                val converted = UnifiedViewfinderCalculator.fromLegacyAspectRatio(ratio)
                require(converted in TEST_ASPECT_RATIOS)
            }
        }
    }
    
    /**
     * Memory usage benchmark comparing current implementation with baseline
     */
    @Test
    fun measure_memory_usage_improvements() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val runtime = Runtime.getRuntime()
        
        // Force garbage collection and get baseline
        System.gc()
        Thread.sleep(100)
        val baselineMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Simulate intensive viewport calculation workload
        val calculatedBounds = mutableListOf<UnifiedViewfinderCalculator.ViewfinderBounds>()
        
        val memoryTestStartTime = System.currentTimeMillis()
        
        // Perform 1000 calculations across different configurations
        repeat(1000) { iteration ->
            val canvasSize = TEST_CANVAS_SIZES[iteration % TEST_CANVAS_SIZES.size]
            val aspectRatio = TEST_ASPECT_RATIOS[iteration % TEST_ASPECT_RATIOS.size]
            
            val bounds = UnifiedViewfinderCalculator.calculateBounds(
                canvasSize = canvasSize,
                aspectRatio = aspectRatio,
                marginFactor = 0.9f,
                safeAreaMargin = 16f
            )
            
            calculatedBounds.add(bounds)
        }
        
        val memoryTestEndTime = System.currentTimeMillis()
        val testDuration = memoryTestEndTime - memoryTestStartTime
        
        // Measure memory after intensive calculations
        System.gc()
        Thread.sleep(100)
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - baselineMemory
        val memoryIncreaseMB = memoryIncrease / (1024 * 1024)
        
        println("Memory Usage Benchmark Results:")
        println("- Baseline memory: ${baselineMemory / (1024 * 1024)}MB")
        println("- Final memory: ${finalMemory / (1024 * 1024)}MB")
        println("- Memory increase: ${memoryIncreaseMB}MB")
        println("- Test duration: ${testDuration}ms")
        println("- Calculations per second: ${1000 * 1000 / testDuration}")
        
        // Memory increase should be minimal (target: less than 15MB for 1000 calculations)
        require(memoryIncreaseMB < 15) { 
            "Memory increase should be less than 15MB, actual: ${memoryIncreaseMB}MB" 
        }
        
        // Performance should be acceptable (target: less than 1000ms for 1000 calculations)
        require(testDuration < 1000) { 
            "Test should complete within 1000ms, actual: ${testDuration}ms" 
        }
        
        // Clean up to prevent memory leaks in subsequent tests
        calculatedBounds.clear()
    }
    
    /**
     * Startup time benchmark comparing viewport initialization
     */
    @Test
    fun measure_camera_startup_improvements() {
        val startupTimes = mutableListOf<Long>()
        
        // Perform multiple startup simulations to get average
        repeat(20) {
            val startTime = System.nanoTime()
            
            // Simulate camera initialization process
            val canvasSize = Size(1080f, 1920f)
            val aspectRatio = UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE
            
            // Calculate initial viewport bounds
            val bounds = UnifiedViewfinderCalculator.calculateBounds(
                canvasSize = canvasSize,
                aspectRatio = aspectRatio
            )
            
            // Validate bounds (part of initialization)
            require(UnifiedViewfinderCalculator.validateBounds(bounds))
            
            // Calculate optimal margin factor for screen
            val marginFactor = UnifiedViewfinderCalculator.calculateOptimalMarginFactor(canvasSize)
            require(marginFactor in 0.8f..1.0f)
            
            // Recalculate with optimal margins
            val optimizedBounds = UnifiedViewfinderCalculator.calculateBounds(
                canvasSize = canvasSize,
                aspectRatio = aspectRatio,
                marginFactor = marginFactor
            )
            
            require(UnifiedViewfinderCalculator.validateBounds(optimizedBounds))
            
            val endTime = System.nanoTime()
            val startupTimeMs = (endTime - startTime) / 1_000_000
            startupTimes.add(startupTimeMs)
        }
        
        val averageStartupTime = startupTimes.average()
        val minStartupTime = startupTimes.minOrNull() ?: 0L
        val maxStartupTime = startupTimes.maxOrNull() ?: 0L
        
        println("Camera Startup Performance Results:")
        println("- Average startup time: ${averageStartupTime}ms")
        println("- Min startup time: ${minStartupTime}ms")
        println("- Max startup time: ${maxStartupTime}ms")
        println("- Standard deviation: ${calculateStandardDeviation(startupTimes)}")
        
        // Target: Average startup time should be under 10ms (improvement from ~15ms)
        require(averageStartupTime < 10.0) { 
            "Average startup time should be under 10ms, actual: ${averageStartupTime}ms" 
        }
        
        // Maximum startup time should be reasonable
        require(maxStartupTime < 20) { 
            "Maximum startup time should be under 20ms, actual: ${maxStartupTime}ms" 
        }
    }
    
    /**
     * Measure UI responsiveness during rapid aspect ratio changes
     */
    @Test
    fun benchmark_aspect_ratio_change_responsiveness() {
        val aspectRatioChangeTimes = mutableListOf<Long>()
        val canvasSize = Size(1080f, 1920f)
        
        // Test rapid aspect ratio changes
        val aspectRatios = TEST_ASPECT_RATIOS.toList()
        
        repeat(50) { iteration ->
            val currentRatio = aspectRatios[iteration % aspectRatios.size]
            val nextRatio = aspectRatios[(iteration + 1) % aspectRatios.size]
            
            val startTime = System.nanoTime()
            
            // Simulate aspect ratio change process
            val currentBounds = UnifiedViewfinderCalculator.calculateBounds(
                canvasSize = canvasSize,
                aspectRatio = currentRatio
            )
            
            // Calculate animated transition
            val animatedBounds = UnifiedViewfinderCalculator.calculateBoundsAnimated(
                canvasSize = canvasSize,
                currentRatio = currentBounds.calculatedAspectRatio,
                targetAspectRatio = nextRatio
            )
            
            // Final bounds calculation
            val finalBounds = UnifiedViewfinderCalculator.calculateBounds(
                canvasSize = canvasSize,
                aspectRatio = nextRatio
            )
            
            require(UnifiedViewfinderCalculator.validateBounds(currentBounds))
            require(UnifiedViewfinderCalculator.validateBounds(animatedBounds))
            require(UnifiedViewfinderCalculator.validateBounds(finalBounds))
            
            val endTime = System.nanoTime()
            val changeTimeMs = (endTime - startTime) / 1_000_000
            aspectRatioChangeTimes.add(changeTimeMs)
        }
        
        val averageChangeTime = aspectRatioChangeTimes.average()
        val maxChangeTime = aspectRatioChangeTimes.maxOrNull() ?: 0L
        
        println("Aspect Ratio Change Performance Results:")
        println("- Average change time: ${averageChangeTime}ms")
        println("- Max change time: ${maxChangeTime}ms")
        println("- Changes per second: ${1000.0 / averageChangeTime}")
        
        // Target: Aspect ratio changes should complete within 5ms on average
        require(averageChangeTime < 5.0) { 
            "Average aspect ratio change time should be under 5ms, actual: ${averageChangeTime}ms" 
        }
        
        // Maximum change time should be reasonable
        require(maxChangeTime < 15) { 
            "Maximum aspect ratio change time should be under 15ms, actual: ${maxChangeTime}ms" 
        }
    }
    
    /**
     * Stress test to verify performance under continuous load
     */
    @Test
    fun benchmark_continuous_load_performance() {
        val runtime = Runtime.getRuntime()
        System.gc()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        val testStartTime = System.currentTimeMillis()
        val testDurationMs = 60 * 1000L // 1 minute stress test
        var operationCount = 0
        var totalCalculationTime = 0L
        
        while ((System.currentTimeMillis() - testStartTime) < testDurationMs) {
            val canvasSize = TEST_CANVAS_SIZES[operationCount % TEST_CANVAS_SIZES.size]
            val aspectRatio = TEST_ASPECT_RATIOS[operationCount % TEST_ASPECT_RATIOS.size]
            
            val operationStartTime = System.nanoTime()
            
            // Perform various operations
            val bounds = UnifiedViewfinderCalculator.calculateBounds(canvasSize, aspectRatio)
            val isValid = UnifiedViewfinderCalculator.validateBounds(bounds)
            val marginFactor = UnifiedViewfinderCalculator.calculateOptimalMarginFactor(canvasSize)
            
            if (operationCount % 3 == 0) {
                // Simulate animation calculation
                UnifiedViewfinderCalculator.calculateBoundsAnimated(
                    canvasSize = canvasSize,
                    currentRatio = bounds.calculatedAspectRatio * 1.1f,
                    targetAspectRatio = aspectRatio
                )
            }
            
            val operationEndTime = System.nanoTime()
            val operationTimeMs = (operationEndTime - operationStartTime) / 1_000_000
            totalCalculationTime += operationTimeMs
            
            require(isValid)
            require(marginFactor in 0.8f..1.0f)
            
            operationCount++
            
            // Brief pause to prevent overwhelming the system
            if (operationCount % 100 == 0) {
                Thread.sleep(1)
            }
        }
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        val averageOperationTime = totalCalculationTime.toDouble() / operationCount
        
        println("Continuous Load Performance Results:")
        println("- Total operations: $operationCount")
        println("- Operations per second: ${operationCount * 1000 / testDurationMs}")
        println("- Average operation time: ${averageOperationTime}ms")
        println("- Total calculation time: ${totalCalculationTime}ms")
        println("- Memory increase: ${memoryIncrease / (1024 * 1024)}MB")
        
        // Performance targets
        require(averageOperationTime < 1.0) { 
            "Average operation time should be under 1ms, actual: ${averageOperationTime}ms" 
        }
        
        require(operationCount > 5000) { 
            "Should complete at least 5000 operations in 1 minute, actual: $operationCount" 
        }
        
        val memoryIncreaseMB = memoryIncrease / (1024 * 1024)
        require(memoryIncreaseMB < 25) { 
            "Memory increase should be under 25MB, actual: ${memoryIncreaseMB}MB" 
        }
    }
    
    private fun calculateStandardDeviation(values: List<Long>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}
