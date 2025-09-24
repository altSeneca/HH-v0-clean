package com.hazardhawk.ai.litert

import com.hazardhawk.TestUtils
import com.hazardhawk.core.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.math.abs
import kotlin.math.max

/**
 * Comprehensive performance benchmark suite for LiteRT integration.
 * Tests performance targets, regression detection, memory efficiency,
 * and scalability across different device tiers and workloads.
 */
class LiteRTPerformanceBenchmarkTest {
    
    private lateinit var performanceBaseline: PerformanceBaseline
    private lateinit var regressionDetector: RegressionDetector
    private lateinit var benchmarkRunner: BenchmarkRunner
    private lateinit var testDataFactory: LiteRTTestDataFactory
    private val benchmarkResults = mutableListOf<BenchmarkResult>()
    
    companion object {
        // Performance targets for different backends (milliseconds)
        private val PERFORMANCE_TARGETS = mapOf(
            LiteRTBackend.NPU_QTI_HTP to 600L,
            LiteRTBackend.NPU_NNAPI to 800L,
            LiteRTBackend.GPU_OPENCL to 1500L,
            LiteRTBackend.GPU_OPENGL to 1800L,
            LiteRTBackend.CPU to 3000L
        )
        
        // Memory usage targets (MB)
        private val MEMORY_TARGETS = mapOf(
            LiteRTBackend.NPU_QTI_HTP to 400f,
            LiteRTBackend.NPU_NNAPI to 450f,
            LiteRTBackend.GPU_OPENCL to 600f,
            LiteRTBackend.GPU_OPENGL to 550f,
            LiteRTBackend.CPU to 800f
        )
        
        // Throughput targets (analyses per second)
        private val THROUGHPUT_TARGETS = mapOf(
            LiteRTBackend.NPU_QTI_HTP to 1.8f,
            LiteRTBackend.NPU_NNAPI to 1.3f,
            LiteRTBackend.GPU_OPENCL to 0.7f,
            LiteRTBackend.GPU_OPENGL to 0.6f,
            LiteRTBackend.CPU to 0.3f
        )
    }
    
    @BeforeTest
    fun setup() {
        performanceBaseline = PerformanceBaseline()
        regressionDetector = RegressionDetector()
        benchmarkRunner = BenchmarkRunner()
        testDataFactory = LiteRTTestDataFactory()
        
        // Load historical performance baselines
        performanceBaseline.loadBaselines()
    }
    
    @AfterTest
    fun tearDown() {
        // Generate performance report
        generatePerformanceBenchmarkReport()
        
        // Check for performance regressions
        val regressions = regressionDetector.detectRegressions(benchmarkResults)
        if (regressions.isNotEmpty()) {
            println("⚠️ Performance regressions detected:")
            regressions.forEach { regression ->
                println("  - ${regression.testName}: ${regression.regressionType} (${regression.impactPercentage}% slower)")
            }
        }
    }
    
    // =====================================================
    // BACKEND PERFORMANCE BENCHMARKS
    // =====================================================
    
    @Test
    fun `test NPU HTP performance benchmarks`() = runTest {
        val mockEngine = MockLiteRTModelEngine()
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 12f,
            supportedBackends = setOf(LiteRTBackend.NPU_QTI_HTP)
        )
        
        val benchmarkSuite = BackendBenchmarkSuite(
            backend = LiteRTBackend.NPU_QTI_HTP,
            targetProcessingTime = PERFORMANCE_TARGETS[LiteRTBackend.NPU_QTI_HTP]!!,
            targetMemoryUsage = MEMORY_TARGETS[LiteRTBackend.NPU_QTI_HTP]!!,
            targetThroughput = THROUGHPUT_TARGETS[LiteRTBackend.NPU_QTI_HTP]!!
        )
        
        runBackendBenchmarks(mockEngine, benchmarkSuite)
    }
    
    @Test
    fun `test NPU NNAPI performance benchmarks`() = runTest {
        val mockEngine = MockLiteRTModelEngine()
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 8f,
            supportedBackends = setOf(LiteRTBackend.NPU_NNAPI)
        )
        
        val benchmarkSuite = BackendBenchmarkSuite(
            backend = LiteRTBackend.NPU_NNAPI,
            targetProcessingTime = PERFORMANCE_TARGETS[LiteRTBackend.NPU_NNAPI]!!,
            targetMemoryUsage = MEMORY_TARGETS[LiteRTBackend.NPU_NNAPI]!!,
            targetThroughput = THROUGHPUT_TARGETS[LiteRTBackend.NPU_NNAPI]!!
        )
        
        runBackendBenchmarks(mockEngine, benchmarkSuite)
    }
    
    @Test
    fun `test GPU OpenCL performance benchmarks`() = runTest {
        val mockEngine = MockLiteRTModelEngine()
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 6f,
            supportedBackends = setOf(LiteRTBackend.GPU_OPENCL)
        )
        
        val benchmarkSuite = BackendBenchmarkSuite(
            backend = LiteRTBackend.GPU_OPENCL,
            targetProcessingTime = PERFORMANCE_TARGETS[LiteRTBackend.GPU_OPENCL]!!,
            targetMemoryUsage = MEMORY_TARGETS[LiteRTBackend.GPU_OPENCL]!!,
            targetThroughput = THROUGHPUT_TARGETS[LiteRTBackend.GPU_OPENCL]!!
        )
        
        runBackendBenchmarks(mockEngine, benchmarkSuite)
    }
    
    @Test
    fun `test GPU OpenGL performance benchmarks`() = runTest {
        val mockEngine = MockLiteRTModelEngine()
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 4f,
            supportedBackends = setOf(LiteRTBackend.GPU_OPENGL)
        )
        
        val benchmarkSuite = BackendBenchmarkSuite(
            backend = LiteRTBackend.GPU_OPENGL,
            targetProcessingTime = PERFORMANCE_TARGETS[LiteRTBackend.GPU_OPENGL]!!,
            targetMemoryUsage = MEMORY_TARGETS[LiteRTBackend.GPU_OPENGL]!!,
            targetThroughput = THROUGHPUT_TARGETS[LiteRTBackend.GPU_OPENGL]!!
        )
        
        runBackendBenchmarks(mockEngine, benchmarkSuite)
    }
    
    @Test
    fun `test CPU fallback performance benchmarks`() = runTest {
        val mockEngine = MockLiteRTModelEngine()
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 3f,
            supportedBackends = setOf(LiteRTBackend.CPU)
        )
        
        val benchmarkSuite = BackendBenchmarkSuite(
            backend = LiteRTBackend.CPU,
            targetProcessingTime = PERFORMANCE_TARGETS[LiteRTBackend.CPU]!!,
            targetMemoryUsage = MEMORY_TARGETS[LiteRTBackend.CPU]!!,
            targetThroughput = THROUGHPUT_TARGETS[LiteRTBackend.CPU]!!
        )
        
        runBackendBenchmarks(mockEngine, benchmarkSuite)
    }
    
    // =====================================================
    // IMAGE SIZE SCALING BENCHMARKS
    // =====================================================
    
    @Test
    fun `test image size scaling performance`() = runTest {
        val mockEngine = MockLiteRTModelEngine()
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 8f,
            supportedBackends = setOf(LiteRTBackend.NPU_QTI_HTP)
        )
        
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val imageSizes = listOf(
            ImageSizeBenchmark(640, 480, "VGA", maxTimeMs = 300L),
            ImageSizeBenchmark(1280, 720, "HD", maxTimeMs = 500L),
            ImageSizeBenchmark(1920, 1080, "FHD", maxTimeMs = 600L),
            ImageSizeBenchmark(2560, 1440, "QHD", maxTimeMs = 900L),
            ImageSizeBenchmark(3840, 2160, "4K", maxTimeMs = 1500L),
            ImageSizeBenchmark(7680, 4320, "8K", maxTimeMs = 4000L)
        )
        
        imageSizes.forEach { sizeTest ->
            val imageData = testDataFactory.createTestImage(sizeTest.width, sizeTest.height)
            
            val (result, duration) = TestUtils.measureExecutionTime {
                mockEngine.generateSafetyAnalysis(
                    imageData = imageData,
                    workType = WorkType.GENERAL_CONSTRUCTION
                )
            }
            
            assertTrue(result.isSuccess, "${sizeTest.name}: Analysis should succeed")
            
            val processingTime = duration.inWholeMilliseconds
            assertTrue(
                processingTime <= sizeTest.maxTimeMs,
                "${sizeTest.name}: Processing time ${processingTime}ms should be <= ${sizeTest.maxTimeMs}ms"
            )
            
            // Record memory usage
            val metrics = mockEngine.getPerformanceMetrics()
            val memoryEfficiency = calculateMemoryEfficiency(
                imageSizeBytes = imageData.size.toLong(),
                memoryUsedMB = metrics.averageMemoryUsageMB
            )
            
            val benchmarkResult = BenchmarkResult(
                testName = "Image Size Scaling - ${sizeTest.name}",
                backend = LiteRTBackend.NPU_QTI_HTP,
                processingTimeMs = processingTime,
                memoryUsageMB = metrics.averageMemoryUsageMB,
                throughputAnalysesPerSecond = 1000f / processingTime,
                imageSize = sizeTest.width * sizeTest.height,
                success = true,
                memoryEfficiency = memoryEfficiency
            )
            
            benchmarkResults.add(benchmarkResult)
            
            println("${sizeTest.name} (${sizeTest.width}x${sizeTest.height}): ${processingTime}ms, ${metrics.averageMemoryUsageMB}MB")
        }
    }
    
    // =====================================================
    // CONCURRENT PROCESSING BENCHMARKS
    // =====================================================
    
    @Test
    fun `test concurrent analysis performance`() = runTest {
        val mockEngine = MockLiteRTModelEngine()
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 12f,
            supportedBackends = setOf(LiteRTBackend.NPU_QTI_HTP)
        )
        
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        val concurrencyLevels = listOf(1, 2, 4, 8, 16)
        
        concurrencyLevels.forEach { concurrency ->
            val testImages = (1..concurrency).map { 
                testDataFactory.createStandardTestImage() to WorkType.GENERAL_CONSTRUCTION
            }
            
            val (results, duration) = TestUtils.measureExecutionTime {
                // Simulate concurrent processing
                testImages.map { (imageData, workType) ->
                    mockEngine.generateSafetyAnalysis(imageData, workType)
                }
            }
            
            val successfulAnalyses = results.count { it.isSuccess }
            val totalTime = duration.inWholeMilliseconds
            val throughput = (successfulAnalyses * 1000f) / totalTime
            
            assertTrue(
                successfulAnalyses >= concurrency - 1, // Allow one failure
                "Concurrency $concurrency: Should complete at least ${concurrency - 1} analyses"
            )
            
            val expectedMaxTime = PERFORMANCE_TARGETS[LiteRTBackend.NPU_QTI_HTP]!! * concurrency * 0.8f // 80% efficiency
            assertTrue(
                totalTime <= expectedMaxTime,
                "Concurrency $concurrency: Total time ${totalTime}ms should be <= ${expectedMaxTime}ms"
            )
            
            val metrics = mockEngine.getPerformanceMetrics()
            
            val benchmarkResult = BenchmarkResult(
                testName = "Concurrent Processing - $concurrency parallel",
                backend = LiteRTBackend.NPU_QTI_HTP,
                processingTimeMs = totalTime,
                memoryUsageMB = metrics.peakMemoryUsageMB,
                throughputAnalysesPerSecond = throughput,
                concurrencyLevel = concurrency,
                success = successfulAnalyses >= concurrency - 1
            )
            
            benchmarkResults.add(benchmarkResult)
            
            println("Concurrency $concurrency: ${totalTime}ms total, ${throughput} analyses/sec, ${metrics.peakMemoryUsageMB}MB peak")
        }
    }
    
    // =====================================================
    // SUSTAINED LOAD BENCHMARKS
    // =====================================================
    
    @Test
    fun `test sustained load performance`() = runTest {
        val mockEngine = MockLiteRTModelEngine()
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 8f,
            supportedBackends = setOf(LiteRTBackend.NPU_NNAPI)
        )
        
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.NPU_NNAPI)
        
        val loadTestDurations = listOf(
            LoadTestScenario("Short Load", 30, 60), // 30 seconds, 60 analyses
            LoadTestScenario("Medium Load", 120, 200), // 2 minutes, 200 analyses  
            LoadTestScenario("Extended Load", 300, 400) // 5 minutes, 400 analyses
        )
        
        loadTestDurations.forEach { loadTest ->
            val sustainedLoadResult = benchmarkRunner.runSustainedLoad(
                engine = mockEngine,
                testDurationSeconds = loadTest.durationSeconds,
                targetAnalysisCount = loadTest.targetAnalysisCount,
                imageDataFactory = { testDataFactory.createStandardTestImage() }
            )
            
            assertTrue(
                sustainedLoadResult.completedAnalyses >= (loadTest.targetAnalysisCount * 0.9f).toInt(),
                "${loadTest.name}: Should complete at least 90% of target analyses"
            )
            
            assertTrue(
                sustainedLoadResult.successRate >= 0.95f,
                "${loadTest.name}: Success rate ${sustainedLoadResult.successRate} should be >= 95%"
            )
            
            assertTrue(
                sustainedLoadResult.averageProcessingTime <= PERFORMANCE_TARGETS[LiteRTBackend.NPU_NNAPI]!! * 1.2f,
                "${loadTest.name}: Average processing time should stay within 120% of baseline"
            )
            
            assertFalse(
                sustainedLoadResult.thermalThrottlingDetected,
                "${loadTest.name}: Should not trigger thermal throttling"
            )
            
            assertFalse(
                sustainedLoadResult.memoryLeakDetected,
                "${loadTest.name}: Should not have memory leaks"
            )
            
            val benchmarkResult = BenchmarkResult(
                testName = loadTest.name,
                backend = LiteRTBackend.NPU_NNAPI,
                processingTimeMs = sustainedLoadResult.averageProcessingTime.toLong(),
                memoryUsageMB = sustainedLoadResult.peakMemoryUsage,
                throughputAnalysesPerSecond = sustainedLoadResult.throughput,
                success = sustainedLoadResult.successRate >= 0.95f,
                sustainedLoad = true,
                loadDurationSeconds = loadTest.durationSeconds
            )
            
            benchmarkResults.add(benchmarkResult)
        }
    }
    
    // =====================================================
    // MEMORY EFFICIENCY BENCHMARKS
    // =====================================================
    
    @Test
    fun `test memory efficiency and leak detection`() = runTest {
        val mockEngine = MockLiteRTModelEngine()
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 6f,
            supportedBackends = setOf(LiteRTBackend.GPU_OPENCL)
        )
        
        mockEngine.initialize("construction_safety_v1.litertmlm", LiteRTBackend.GPU_OPENCL)
        
        // Baseline memory usage
        val initialMetrics = mockEngine.getPerformanceMetrics()
        val baselineMemory = initialMetrics.averageMemoryUsageMB
        
        // Run repeated analyses to check for memory leaks
        val analysisCount = 50
        val memoryMeasurements = mutableListOf<Float>()
        
        repeat(analysisCount) { iteration ->
            val imageData = testDataFactory.createStandardTestImage()
            
            val result = mockEngine.generateSafetyAnalysis(
                imageData = imageData,
                workType = WorkType.GENERAL_CONSTRUCTION
            )
            
            assertTrue(result.isSuccess, "Analysis $iteration should succeed")
            
            val currentMetrics = mockEngine.getPerformanceMetrics()
            memoryMeasurements.add(currentMetrics.averageMemoryUsageMB)
            
            // Check for memory growth trend
            if (iteration >= 10) { // Start checking after warmup
                val recentMemory = memoryMeasurements.takeLast(10).average()
                val earlyMemory = memoryMeasurements.take(10).average()
                val memoryGrowth = (recentMemory - earlyMemory) / earlyMemory
                
                assertTrue(
                    memoryGrowth <= 0.05, // Allow 5% growth
                    "Memory growth ${memoryGrowth * 100}% exceeds 5% threshold at iteration $iteration"
                )
            }
        }
        
        val finalMetrics = mockEngine.getPerformanceMetrics()
        val finalMemory = finalMetrics.averageMemoryUsageMB
        val memoryEfficiencyScore = calculateMemoryEfficiencyScore(
            baselineMemory, finalMemory, analysisCount
        )
        
        assertTrue(
            memoryEfficiencyScore >= 0.8f,
            "Memory efficiency score $memoryEfficiencyScore should be >= 0.8"
        )
        
        val benchmarkResult = BenchmarkResult(
            testName = "Memory Efficiency Test",
            backend = LiteRTBackend.GPU_OPENCL,
            processingTimeMs = finalMetrics.averageProcessingTimeMs,
            memoryUsageMB = finalMetrics.averageMemoryUsageMB,
            throughputAnalysesPerSecond = 1000f / finalMetrics.averageProcessingTimeMs,
            success = memoryEfficiencyScore >= 0.8f,
            memoryEfficiency = memoryEfficiencyScore
        )
        
        benchmarkResults.add(benchmarkResult)
    }
    
    // =====================================================
    // REGRESSION DETECTION BENCHMARKS
    // =====================================================
    
    @Test
    fun `test performance regression detection`() = runTest {
        val testBackend = LiteRTBackend.NPU_QTI_HTP
        val historicalBaseline = performanceBaseline.getBaseline(testBackend)
        
        val mockEngine = MockLiteRTModelEngine()
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 8f,
            supportedBackends = setOf(testBackend)
        )
        
        mockEngine.initialize("construction_safety_v1.litertmlm", testBackend)
        
        // Run current performance test
        val imageData = testDataFactory.createStandardTestImage()
        val (result, duration) = TestUtils.measureExecutionTime {
            mockEngine.generateSafetyAnalysis(imageData, WorkType.GENERAL_CONSTRUCTION)
        }
        
        assertTrue(result.isSuccess, "Regression test analysis should succeed")
        
        val currentPerformance = PerformanceMeasurement(
            processingTimeMs = duration.inWholeMilliseconds,
            memoryUsageMB = mockEngine.getPerformanceMetrics().averageMemoryUsageMB,
            throughput = 1000f / duration.inWholeMilliseconds,
            backend = testBackend
        )
        
        // Compare with historical baseline
        val regressionAnalysis = regressionDetector.analyzeRegression(
            baseline = historicalBaseline,
            current = currentPerformance
        )
        
        // Performance should not regress more than 15%
        assertTrue(
            regressionAnalysis.processingTimeRegression <= 0.15f,
            "Processing time regression ${regressionAnalysis.processingTimeRegression * 100}% exceeds 15% threshold"
        )
        
        assertTrue(
            regressionAnalysis.memoryUsageRegression <= 0.20f,
            "Memory usage regression ${regressionAnalysis.memoryUsageRegression * 100}% exceeds 20% threshold"
        )
        
        assertTrue(
            regressionAnalysis.throughputRegression <= 0.15f,
            "Throughput regression ${regressionAnalysis.throughputRegression * 100}% exceeds 15% threshold"
        )
        
        // Update baseline with current performance
        performanceBaseline.updateBaseline(testBackend, currentPerformance)
        
        val benchmarkResult = BenchmarkResult(
            testName = "Regression Detection Test",
            backend = testBackend,
            processingTimeMs = currentPerformance.processingTimeMs,
            memoryUsageMB = currentPerformance.memoryUsageMB,
            throughputAnalysesPerSecond = currentPerformance.throughput,
            success = regressionAnalysis.processingTimeRegression <= 0.15f,
            regressionAnalysis = regressionAnalysis
        )
        
        benchmarkResults.add(benchmarkResult)
    }
    
    // =====================================================
    // HELPER METHODS
    // =====================================================
    
    private suspend fun runBackendBenchmarks(mockEngine: MockLiteRTModelEngine, suite: BackendBenchmarkSuite) {
        mockEngine.initialize("construction_safety_v1.litertmlm", suite.backend)
        
        val testScenarios = listOf(
            "Standard Construction Site" to testDataFactory.createStandardTestImage(),
            "PPE Compliance Check" to testDataFactory.createPPECompliantImage(),
            "Fall Hazard Detection" to testDataFactory.createFallHazardImage(),
            "Multi-Hazard Scenario" to testDataFactory.createMultipleHazardsImage()
        )
        
        testScenarios.forEach { (scenarioName, imageData) ->
            val (result, duration) = TestUtils.measureExecutionTime {
                mockEngine.generateSafetyAnalysis(imageData, WorkType.GENERAL_CONSTRUCTION)
            }
            
            assertTrue(result.isSuccess, "$scenarioName: Analysis should succeed")
            
            val processingTime = duration.inWholeMilliseconds
            val metrics = mockEngine.getPerformanceMetrics()
            
            // Validate performance targets
            assertTrue(
                processingTime <= suite.targetProcessingTime,
                "$scenarioName: Processing time ${processingTime}ms should be <= ${suite.targetProcessingTime}ms"
            )
            
            assertTrue(
                metrics.averageMemoryUsageMB <= suite.targetMemoryUsage,
                "$scenarioName: Memory usage ${metrics.averageMemoryUsageMB}MB should be <= ${suite.targetMemoryUsage}MB"
            )
            
            val actualThroughput = 1000f / processingTime
            assertTrue(
                actualThroughput >= suite.targetThroughput,
                "$scenarioName: Throughput ${actualThroughput} should be >= ${suite.targetThroughput} analyses/sec"
            )
            
            val benchmarkResult = BenchmarkResult(
                testName = "${suite.backend} - $scenarioName",
                backend = suite.backend,
                processingTimeMs = processingTime,
                memoryUsageMB = metrics.averageMemoryUsageMB,
                throughputAnalysesPerSecond = actualThroughput,
                success = true
            )
            
            benchmarkResults.add(benchmarkResult)
        }
    }
    
    private fun calculateMemoryEfficiency(imageSizeBytes: Long, memoryUsedMB: Float): Float {
        val imageSizeMB = imageSizeBytes / (1024f * 1024f)
        val efficiency = imageSizeMB / memoryUsedMB
        return efficiency.coerceAtMost(1.0f)
    }
    
    private fun calculateMemoryEfficiencyScore(
        baselineMemory: Float,
        finalMemory: Float,
        analysisCount: Int
    ): Float {
        val memoryGrowthPerAnalysis = (finalMemory - baselineMemory) / analysisCount
        val maxAcceptableGrowth = 0.5f // 0.5MB per analysis maximum
        
        return if (memoryGrowthPerAnalysis <= 0) {
            1.0f // No growth or reduction is perfect
        } else {
            max(0f, 1f - (memoryGrowthPerAnalysis / maxAcceptableGrowth))
        }
    }
    
    private fun generatePerformanceBenchmarkReport() {
        println("\n" + "=".repeat(60))
        println("LITERT PERFORMANCE BENCHMARK REPORT")
        println("=".repeat(60))
        
        val groupedResults = benchmarkResults.groupBy { it.backend }
        
        groupedResults.forEach { (backend, results) ->
            println("\n$backend Backend:")
            println("-".repeat(40))
            
            val avgProcessingTime = results.map { it.processingTimeMs }.average()
            val avgMemoryUsage = results.map { it.memoryUsageMB }.average()
            val avgThroughput = results.map { it.throughputAnalysesPerSecond }.average()
            val successRate = results.count { it.success }.toFloat() / results.size
            
            val target = PERFORMANCE_TARGETS[backend] ?: 0L
            val performanceScore = if (target > 0) {
                max(0f, 1f - ((avgProcessingTime - target).toFloat() / target))
            } else 0f
            
            println("  Average Processing Time: ${avgProcessingTime.toInt()}ms (target: ${target}ms)")
            println("  Average Memory Usage: ${"%.1f".format(avgMemoryUsage)}MB")
            println("  Average Throughput: ${"%.2f".format(avgThroughput)} analyses/sec")
            println("  Success Rate: ${"%.1f".format(successRate * 100)}%")
            println("  Performance Score: ${"%.1f".format(performanceScore * 100)}%")
            
            if (performanceScore >= 0.9f) {
                println("  Status: ✅ EXCELLENT")
            } else if (performanceScore >= 0.8f) {
                println("  Status: ✅ GOOD")
            } else if (performanceScore >= 0.7f) {
                println("  Status: ⚠️ ACCEPTABLE")
            } else {
                println("  Status: ❌ NEEDS IMPROVEMENT")
            }
        }
        
        println("\n" + "=".repeat(60))
        
        val overallScore = benchmarkResults.map { result ->
            val target = PERFORMANCE_TARGETS[result.backend] ?: result.processingTimeMs
            max(0f, 1f - ((result.processingTimeMs - target).toFloat() / target))
        }.average().toFloat()
        
        println("OVERALL PERFORMANCE SCORE: ${"%.1f".format(overallScore * 100)}%")
        
        if (overallScore >= 0.85f) {
            println("🚀 PRODUCTION READY - Excellent performance across all backends")
        } else if (overallScore >= 0.75f) {
            println("⚡ GOOD PERFORMANCE - Minor optimizations recommended")
        } else if (overallScore >= 0.65f) {
            println("⚠️ NEEDS OPTIMIZATION - Performance improvements required")
        } else {
            println("🔥 CRITICAL - Significant performance issues detected")
        }
        
        println("=".repeat(60))
    }
}

// =====================================================
// BENCHMARK DATA CLASSES AND UTILITIES
// =====================================================

data class BackendBenchmarkSuite(
    val backend: LiteRTBackend,
    val targetProcessingTime: Long,
    val targetMemoryUsage: Float,
    val targetThroughput: Float
)

data class ImageSizeBenchmark(
    val width: Int,
    val height: Int,
    val name: String,
    val maxTimeMs: Long
)

data class LoadTestScenario(
    val name: String,
    val durationSeconds: Int,
    val targetAnalysisCount: Int
)

data class BenchmarkResult(
    val testName: String,
    val backend: LiteRTBackend,
    val processingTimeMs: Long,
    val memoryUsageMB: Float,
    val throughputAnalysesPerSecond: Float,
    val success: Boolean,
    val imageSize: Int? = null,
    val concurrencyLevel: Int? = null,
    val sustainedLoad: Boolean = false,
    val loadDurationSeconds: Int? = null,
    val memoryEfficiency: Float? = null,
    val regressionAnalysis: RegressionAnalysis? = null
)

data class PerformanceMeasurement(
    val processingTimeMs: Long,
    val memoryUsageMB: Float,
    val throughput: Float,
    val backend: LiteRTBackend,
    val timestamp: Long = System.currentTimeMillis()
)

data class RegressionAnalysis(
    val processingTimeRegression: Float,
    val memoryUsageRegression: Float,
    val throughputRegression: Float,
    val hasSignificantRegression: Boolean
)

data class RegressionDetection(
    val testName: String,
    val regressionType: String,
    val impactPercentage: Float,
    val backend: LiteRTBackend
)

data class SustainedLoadResult(
    val completedAnalyses: Int,
    val successRate: Float,
    val averageProcessingTime: Float,
    val peakMemoryUsage: Float,
    val throughput: Float,
    val thermalThrottlingDetected: Boolean,
    val memoryLeakDetected: Boolean
)

class PerformanceBaseline {
    private val baselines = mutableMapOf<LiteRTBackend, PerformanceMeasurement>()
    
    fun loadBaselines() {
        // Load historical baselines (in real implementation, this would load from file/database)
        baselines[LiteRTBackend.NPU_QTI_HTP] = PerformanceMeasurement(550L, 380f, 1.8f, LiteRTBackend.NPU_QTI_HTP)
        baselines[LiteRTBackend.NPU_NNAPI] = PerformanceMeasurement(750L, 420f, 1.3f, LiteRTBackend.NPU_NNAPI)
        baselines[LiteRTBackend.GPU_OPENCL] = PerformanceMeasurement(1400L, 580f, 0.7f, LiteRTBackend.GPU_OPENCL)
        baselines[LiteRTBackend.GPU_OPENGL] = PerformanceMeasurement(1700L, 520f, 0.6f, LiteRTBackend.GPU_OPENGL)
        baselines[LiteRTBackend.CPU] = PerformanceMeasurement(2800L, 750f, 0.35f, LiteRTBackend.CPU)
    }
    
    fun getBaseline(backend: LiteRTBackend): PerformanceMeasurement {
        return baselines[backend] ?: throw IllegalArgumentException("No baseline for $backend")
    }
    
    fun updateBaseline(backend: LiteRTBackend, measurement: PerformanceMeasurement) {
        baselines[backend] = measurement
    }
}

class RegressionDetector {
    fun analyzeRegression(baseline: PerformanceMeasurement, current: PerformanceMeasurement): RegressionAnalysis {
        val processingTimeRegression = (current.processingTimeMs - baseline.processingTimeMs).toFloat() / baseline.processingTimeMs
        val memoryRegression = (current.memoryUsageMB - baseline.memoryUsageMB) / baseline.memoryUsageMB
        val throughputRegression = (baseline.throughput - current.throughput) / baseline.throughput
        
        val hasSignificantRegression = processingTimeRegression > 0.15f || 
                                      memoryRegression > 0.20f || 
                                      throughputRegression > 0.15f
        
        return RegressionAnalysis(
            processingTimeRegression = processingTimeRegression,
            memoryUsageRegression = memoryRegression,
            throughputRegression = throughputRegression,
            hasSignificantRegression = hasSignificantRegression
        )
    }
    
    fun detectRegressions(results: List<BenchmarkResult>): List<RegressionDetection> {
        return results.mapNotNull { result ->
            result.regressionAnalysis?.let { regression ->
                if (regression.hasSignificantRegression) {
                    val maxRegression = maxOf(
                        regression.processingTimeRegression,
                        regression.memoryUsageRegression,
                        regression.throughputRegression
                    )
                    
                    val regressionType = when (maxRegression) {
                        regression.processingTimeRegression -> "Processing Time"
                        regression.memoryUsageRegression -> "Memory Usage"
                        else -> "Throughput"
                    }
                    
                    RegressionDetection(
                        testName = result.testName,
                        regressionType = regressionType,
                        impactPercentage = maxRegression * 100,
                        backend = result.backend
                    )
                } else null
            }
        }
    }
}

class BenchmarkRunner {
    suspend fun runSustainedLoad(
        engine: MockLiteRTModelEngine,
        testDurationSeconds: Int,
        targetAnalysisCount: Int,
        imageDataFactory: () -> ByteArray
    ): SustainedLoadResult {
        
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (testDurationSeconds * 1000L)
        
        var completedAnalyses = 0
        var successfulAnalyses = 0
        val processingTimes = mutableListOf<Long>()
        var peakMemoryUsage = 0f
        var thermalThrottlingDetected = false
        
        while (System.currentTimeMillis() < endTime && completedAnalyses < targetAnalysisCount) {
            val imageData = imageDataFactory()
            
            val (result, duration) = TestUtils.measureExecutionTime {
                engine.generateSafetyAnalysis(imageData, WorkType.GENERAL_CONSTRUCTION)
            }
            
            completedAnalyses++
            
            if (result.isSuccess) {
                successfulAnalyses++
                processingTimes.add(duration.inWholeMilliseconds)
            }
            
            val metrics = engine.getPerformanceMetrics()
            peakMemoryUsage = maxOf(peakMemoryUsage, metrics.peakMemoryUsageMB)
            
            if (metrics.thermalThrottlingDetected) {
                thermalThrottlingDetected = true
            }
        }
        
        val actualDuration = (System.currentTimeMillis() - startTime) / 1000f
        val successRate = successfulAnalyses.toFloat() / completedAnalyses
        val averageProcessingTime = if (processingTimes.isNotEmpty()) {
            processingTimes.average().toFloat()
        } else 0f
        val throughput = successfulAnalyses / actualDuration
        
        // Simple memory leak detection: check if memory usage increased significantly
        val finalMetrics = engine.getPerformanceMetrics()
        val memoryLeakDetected = (finalMetrics.averageMemoryUsageMB - peakMemoryUsage) > (peakMemoryUsage * 0.1f)
        
        return SustainedLoadResult(
            completedAnalyses = completedAnalyses,
            successRate = successRate,
            averageProcessingTime = averageProcessingTime,
            peakMemoryUsage = peakMemoryUsage,
            throughput = throughput,
            thermalThrottlingDetected = thermalThrottlingDetected,
            memoryLeakDetected = memoryLeakDetected
        )
    }
}
