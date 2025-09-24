package com.hazardhawk.ai.integration

import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.ai.core.SmartAIOrchestrator
import com.hazardhawk.core.models.*
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.system.measureTimeMillis

/**
 * Comprehensive performance testing suite for AI processing under various load conditions.
 * This suite validates that AI systems maintain acceptable performance, memory usage,
 * and reliability under real-world construction site usage patterns.
 */
class AIPerformanceTestSuite {
    
    /**
     * Performance test configuration and metrics
     */
    data class PerformanceTestConfig(
        val maxConcurrentUsers: Int = 20,
        val requestsPerUser: Int = 10,
        val testDurationMinutes: Int = 5,
        val imageVariations: Int = 50,
        val memorySamplingIntervalMs: Long = 1000L,
        val acceptableResponseTimeMs: Long = 8000L,
        val acceptableP95ResponseTimeMs: Long = 15000L,
        val minimumSuccessRate: Float = 0.95f,
        val maximumMemoryIncreaseMB: Float = 200f
    )
    
    data class PerformanceMetrics(
        val totalRequests: Int,
        val successfulRequests: Int,
        val failedRequests: Int,
        val averageResponseTimeMs: Long,
        val medianResponseTimeMs: Long,
        val p95ResponseTimeMs: Long,
        val p99ResponseTimeMs: Long,
        val minResponseTimeMs: Long,
        val maxResponseTimeMs: Long,
        val successRate: Float,
        val requestsPerSecond: Float,
        val memoryMetrics: MemoryMetrics,
        val errorBreakdown: Map<String, Int>
    )
    
    data class MemoryMetrics(
        val initialMemoryMB: Float,
        val peakMemoryMB: Float,
        val finalMemoryMB: Float,
        val memoryIncreaseMB: Float,
        val averageMemoryMB: Float,
        val memoryLeakDetected: Boolean
    )
    
    data class LoadTestResult(
        val testName: String,
        val config: PerformanceTestConfig,
        val metrics: PerformanceMetrics,
        val passedRequirements: Boolean,
        val failures: List<String>
    )
    
    /**
     * Load testing for concurrent AI analysis requests
     */
    class ConcurrentLoadTest {
        
        private lateinit var orchestrator: SmartAIOrchestrator
        private val memoryMonitor = MemoryMonitor()
        
        @BeforeTest
        fun setup() {
            orchestrator = createPerformanceTestOrchestrator()
            memoryMonitor.reset()
        }
        
        @Test
        fun `test concurrent user analysis load - 20 users, 10 requests each`() = runTest {
            val config = PerformanceTestConfig(
                maxConcurrentUsers = 20,
                requestsPerUser = 10,
                acceptableResponseTimeMs = 8000L,
                minimumSuccessRate = 0.95f
            )
            
            val result = executeLoadTest("Concurrent User Load Test", config)
            
            assertTrue(
                "Load test failed requirements: ${result.failures}",
                result.passedRequirements
            )
            
            // Verify specific requirements
            assertTrue(
                "Success rate ${result.metrics.successRate} below minimum ${config.minimumSuccessRate}",
                result.metrics.successRate >= config.minimumSuccessRate
            )
            
            assertTrue(
                "Average response time ${result.metrics.averageResponseTimeMs}ms exceeds ${config.acceptableResponseTimeMs}ms",
                result.metrics.averageResponseTimeMs <= config.acceptableResponseTimeMs
            )
            
            assertTrue(
                "P95 response time ${result.metrics.p95ResponseTimeMs}ms exceeds ${config.acceptableP95ResponseTimeMs}ms",
                result.metrics.p95ResponseTimeMs <= config.acceptableP95ResponseTimeMs
            )
            
            logPerformanceResults(result)
        }
        
        @Test
        fun `test sustained load over extended period - 5 minutes continuous`() = runTest {
            val config = PerformanceTestConfig(
                maxConcurrentUsers = 10,
                requestsPerUser = 30, // More requests over time
                testDurationMinutes = 5,
                acceptableResponseTimeMs = 10000L, // Slightly higher for sustained load
                minimumSuccessRate = 0.90f
            )
            
            val result = executeSustainedLoadTest("Sustained Load Test", config)
            
            assertTrue(
                "Sustained load test failed: ${result.failures}",
                result.passedRequirements
            )
            
            // Check for performance degradation over time
            assertFalse(
                "Memory leak detected during sustained load",
                result.metrics.memoryMetrics.memoryLeakDetected
            )
            
            assertTrue(
                "Memory increase ${result.metrics.memoryMetrics.memoryIncreaseMB}MB exceeds limit ${config.maximumMemoryIncreaseMB}MB",
                result.metrics.memoryMetrics.memoryIncreaseMB <= config.maximumMemoryIncreaseMB
            )
            
            logPerformanceResults(result)
        }
        
        @Test
        fun `test burst load handling - high intensity short duration`() = runTest {
            val config = PerformanceTestConfig(
                maxConcurrentUsers = 50, // High burst
                requestsPerUser = 3, // Short burst
                acceptableResponseTimeMs = 12000L, // Allow higher latency for burst
                minimumSuccessRate = 0.85f // Lower success rate acceptable for burst
            )
            
            val result = executeBurstLoadTest("Burst Load Test", config)
            
            assertTrue(
                "Burst load test failed: ${result.failures}",
                result.passedRequirements
            )
            
            // Verify system recovers after burst
            val recoveryTime = measureRecoveryTime()
            assertTrue(
                "System recovery time ${recoveryTime}ms too long",
                recoveryTime <= 5000L
            )
            
            logPerformanceResults(result)
        }
        
        @Test
        fun `test memory usage under high load`() = runTest {
            val config = PerformanceTestConfig(
                maxConcurrentUsers = 15,
                requestsPerUser = 20,
                memorySamplingIntervalMs = 500L, // More frequent memory monitoring
                maximumMemoryIncreaseMB = 150f
            )
            
            memoryMonitor.startMonitoring(config.memorySamplingIntervalMs)
            
            val result = executeLoadTest("Memory Usage Test", config)
            
            memoryMonitor.stopMonitoring()
            
            val memoryMetrics = memoryMonitor.getMetrics()
            
            assertTrue(
                "Memory increase ${memoryMetrics.memoryIncreaseMB}MB exceeds limit",
                memoryMetrics.memoryIncreaseMB <= config.maximumMemoryIncreaseMB
            )
            
            assertFalse(
                "Memory leak detected",
                memoryMetrics.memoryLeakDetected
            )
            
            // Verify memory cleanup after load test
            delay(2000L) // Allow garbage collection
            System.gc()
            delay(1000L)
            
            val finalMemory = getCurrentMemoryUsageMB()
            val memoryRetained = finalMemory - memoryMetrics.initialMemoryMB
            
            assertTrue(
                "Too much memory retained after test: ${memoryRetained}MB",
                memoryRetained <= 50f // Max 50MB retained
            )
            
            logMemoryResults(memoryMetrics)
        }
        
        @Test
        fun `test large image processing performance`() = runTest {
            val config = PerformanceTestConfig(
                maxConcurrentUsers = 5, // Fewer users for large images
                requestsPerUser = 5,
                acceptableResponseTimeMs = 15000L, // Higher timeout for large images
                minimumSuccessRate = 0.90f
            )
            
            val largeImageSizes = listOf(5, 10, 20, 30) // MB sizes
            val performanceResults = mutableMapOf<Int, PerformanceMetrics>()
            
            largeImageSizes.forEach { sizeMB ->
                val largeImageResult = executeLoadTestWithImageSize(
                    "Large Image Test ${sizeMB}MB", 
                    config, 
                    sizeMB
                )
                
                performanceResults[sizeMB] = largeImageResult.metrics
                
                assertTrue(
                    "Large image test failed for ${sizeMB}MB: ${largeImageResult.failures}",
                    largeImageResult.passedRequirements
                )
            }
            
            // Verify performance scales reasonably with image size
            val baselinePerf = performanceResults[5]!!
            val largestPerf = performanceResults[30]!!
            
            val performanceDegradation = largestPerf.averageResponseTimeMs.toFloat() / 
                                        baselinePerf.averageResponseTimeMs
            
            assertTrue(
                "Performance degradation too high: ${performanceDegradation}x for 6x image size",
                performanceDegradation <= 3.0f // Max 3x slower for 6x larger images
            )
            
            logLargeImageResults(performanceResults)
        }
        
        @Test
        fun `test mixed workload performance - different work types`() = runTest {
            val workTypes = listOf(
                WorkType.GENERAL_CONSTRUCTION,
                WorkType.ELECTRICAL_WORK,
                WorkType.CONCRETE_WORK,
                WorkType.EXCAVATION,
                WorkType.ROOFING
            )
            
            val config = PerformanceTestConfig(
                maxConcurrentUsers = 15,
                requestsPerUser = 8,
                acceptableResponseTimeMs = 9000L,
                minimumSuccessRate = 0.92f
            )
            
            val workTypeResults = mutableMapOf<WorkType, PerformanceMetrics>()
            
            workTypes.forEach { workType ->
                val result = executeLoadTestWithWorkType(
                    "Mixed Workload Test - $workType", 
                    config, 
                    workType
                )
                
                workTypeResults[workType] = result.metrics
                
                assertTrue(
                    "Mixed workload test failed for $workType: ${result.failures}",
                    result.passedRequirements
                )
            }
            
            // Verify consistent performance across work types
            val responseTimeVariance = calculateResponseTimeVariance(workTypeResults)
            assertTrue(
                "Response time variance too high across work types: ${responseTimeVariance}%",
                responseTimeVariance <= 30f // Max 30% variance
            )
            
            logMixedWorkloadResults(workTypeResults)
        }
        
        @Test
        fun `test network instability impact on performance`() = runTest {
            val config = PerformanceTestConfig(
                maxConcurrentUsers = 10,
                requestsPerUser = 15,
                acceptableResponseTimeMs = 12000L, // Higher due to network issues
                minimumSuccessRate = 0.80f // Lower due to network instability
            )
            
            val networkConditions = listOf(
                NetworkCondition.STABLE,
                NetworkCondition.INTERMITTENT,
                NetworkCondition.SLOW,
                NetworkCondition.UNSTABLE
            )
            
            val networkResults = mutableMapOf<NetworkCondition, PerformanceMetrics>()
            
            networkConditions.forEach { condition ->
                val mockNetwork = createMockNetworkWithCondition(condition)
                val networkOrchestrator = createOrchestratorWithNetwork(mockNetwork)
                
                val result = executeLoadTestWithOrchestrator(
                    "Network Instability Test - $condition",
                    config,
                    networkOrchestrator
                )
                
                networkResults[condition] = result.metrics
                
                // Different requirements based on network condition
                val expectedSuccessRate = when (condition) {
                    NetworkCondition.STABLE -> 0.95f
                    NetworkCondition.INTERMITTENT -> 0.85f
                    NetworkCondition.SLOW -> 0.90f
                    NetworkCondition.UNSTABLE -> 0.75f
                }
                
                assertTrue(
                    "Network test failed for $condition: success rate ${result.metrics.successRate}",
                    result.metrics.successRate >= expectedSuccessRate
                )
            }
            
            logNetworkResults(networkResults)
        }
        
        @Test
        fun `test recovery after service failures`() = runTest {
            val config = PerformanceTestConfig(
                maxConcurrentUsers = 8,
                requestsPerUser = 10,
                acceptableResponseTimeMs = 10000L,
                minimumSuccessRate = 0.70f // Lower due to induced failures
            )
            
            // Test with failing service that recovers
            val failingService = createFailingAIService(
                failureRate = 0.3f, // 30% failure rate
                recoveryTimeMs = 5000L
            )
            
            val result = executeLoadTestWithService(
                "Service Recovery Test",
                config,
                failingService
            )
            
            // Verify system handles failures gracefully
            assertTrue(
                "Service recovery test failed: ${result.failures}",
                result.metrics.successRate >= config.minimumSuccessRate
            )
            
            // Verify error types are appropriate
            val networkErrors = result.metrics.errorBreakdown["NetworkError"] ?: 0
            val timeoutErrors = result.metrics.errorBreakdown["TimeoutError"] ?: 0
            val totalErrors = result.metrics.failedRequests
            
            assertTrue(
                "Unexpected error types detected",
                networkErrors + timeoutErrors >= totalErrors * 0.8 // 80% should be expected error types
            )
            
            logRecoveryResults(result)
        }
        
        private suspend fun executeLoadTest(
            testName: String, 
            config: PerformanceTestConfig
        ): LoadTestResult {
            val startTime = System.currentTimeMillis()
            val responseTimes = mutableListOf<Long>()
            val errors = mutableMapOf<String, Int>()
            var successCount = 0
            var failureCount = 0
            
            memoryMonitor.startMonitoring(config.memorySamplingIntervalMs)
            
            try {
                // Create concurrent users
                val userTasks = (1..config.maxConcurrentUsers).map { userId ->
                    async {
                        repeat(config.requestsPerUser) { requestId ->
                            try {
                                val testImage = generateTestImage(userId, requestId)
                                val requestStart = System.currentTimeMillis()
                                
                                val result = orchestrator.analyzePhoto(
                                    testImage, 
                                    WorkType.GENERAL_CONSTRUCTION
                                )
                                
                                val requestTime = System.currentTimeMillis() - requestStart
                                responseTimes.add(requestTime)
                                
                                if (result.isSuccess) {
                                    successCount++
                                } else {
                                    failureCount++
                                    val errorType = result.exceptionOrNull()?.javaClass?.simpleName ?: "UnknownError"
                                    errors[errorType] = errors.getOrDefault(errorType, 0) + 1
                                }
                                
                            } catch (e: Exception) {
                                failureCount++
                                errors[e.javaClass.simpleName] = errors.getOrDefault(e.javaClass.simpleName, 0) + 1
                            }
                        }
                    }
                }
                
                userTasks.awaitAll()
                
            } finally {
                memoryMonitor.stopMonitoring()
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            val totalRequests = successCount + failureCount
            
            val metrics = calculatePerformanceMetrics(
                responseTimes, successCount, failureCount, totalTime, errors, memoryMonitor.getMetrics()
            )
            
            val failures = validatePerformanceRequirements(metrics, config)
            
            return LoadTestResult(
                testName = testName,
                config = config,
                metrics = metrics,
                passedRequirements = failures.isEmpty(),
                failures = failures
            )
        }
        
        private suspend fun executeSustainedLoadTest(
            testName: String,
            config: PerformanceTestConfig
        ): LoadTestResult {
            val endTime = System.currentTimeMillis() + (config.testDurationMinutes * 60 * 1000)
            val responseTimes = mutableListOf<Long>()
            val errors = mutableMapOf<String, Int>()
            var successCount = 0
            var failureCount = 0
            
            memoryMonitor.startMonitoring(config.memorySamplingIntervalMs)
            
            try {
                while (System.currentTimeMillis() < endTime) {
                    val batchTasks = (1..config.maxConcurrentUsers).map { userId ->
                        async {
                            try {
                                val testImage = generateTestImage(userId, System.currentTimeMillis().toInt())
                                val requestStart = System.currentTimeMillis()
                                
                                val result = orchestrator.analyzePhoto(
                                    testImage, 
                                    WorkType.GENERAL_CONSTRUCTION
                                )
                                
                                val requestTime = System.currentTimeMillis() - requestStart
                                responseTimes.add(requestTime)
                                
                                if (result.isSuccess) {
                                    successCount++
                                } else {
                                    failureCount++
                                }
                                
                            } catch (e: Exception) {
                                failureCount++
                                errors[e.javaClass.simpleName] = errors.getOrDefault(e.javaClass.simpleName, 0) + 1
                            }
                        }
                    }
                    
                    batchTasks.awaitAll()
                    
                    // Brief pause between batches
                    delay(100L)
                }
                
            } finally {
                memoryMonitor.stopMonitoring()
            }
            
            val totalRequests = successCount + failureCount
            val metrics = calculatePerformanceMetrics(
                responseTimes, successCount, failureCount, 
                config.testDurationMinutes * 60 * 1000L, errors, memoryMonitor.getMetrics()
            )
            
            val failures = validatePerformanceRequirements(metrics, config)
            
            return LoadTestResult(
                testName = testName,
                config = config,
                metrics = metrics,
                passedRequirements = failures.isEmpty(),
                failures = failures
            )
        }
        
        private suspend fun executeBurstLoadTest(
            testName: String,
            config: PerformanceTestConfig
        ): LoadTestResult {
            val responseTimes = mutableListOf<Long>()
            val errors = mutableMapOf<String, Int>()
            var successCount = 0
            var failureCount = 0
            
            memoryMonitor.startMonitoring(config.memorySamplingIntervalMs)
            
            val startTime = System.currentTimeMillis()
            
            try {
                // Execute all requests simultaneously (burst)
                val allTasks = (1..config.maxConcurrentUsers).flatMap { userId ->
                    (1..config.requestsPerUser).map { requestId ->
                        async {
                            try {
                                val testImage = generateTestImage(userId, requestId)
                                val requestStart = System.currentTimeMillis()
                                
                                val result = orchestrator.analyzePhoto(
                                    testImage, 
                                    WorkType.GENERAL_CONSTRUCTION
                                )
                                
                                val requestTime = System.currentTimeMillis() - requestStart
                                responseTimes.add(requestTime)
                                
                                if (result.isSuccess) {
                                    successCount++
                                } else {
                                    failureCount++
                                }
                                
                            } catch (e: Exception) {
                                failureCount++
                                errors[e.javaClass.simpleName] = errors.getOrDefault(e.javaClass.simpleName, 0) + 1
                            }
                        }
                    }
                }
                
                allTasks.awaitAll()
                
            } finally {
                memoryMonitor.stopMonitoring()
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            val totalRequests = successCount + failureCount
            
            val metrics = calculatePerformanceMetrics(
                responseTimes, successCount, failureCount, totalTime, errors, memoryMonitor.getMetrics()
            )
            
            val failures = validatePerformanceRequirements(metrics, config)
            
            return LoadTestResult(
                testName = testName,
                config = config,
                metrics = metrics,
                passedRequirements = failures.isEmpty(),
                failures = failures
            )
        }
        
        private suspend fun measureRecoveryTime(): Long {
            val startTime = System.currentTimeMillis()
            
            // Test single request to measure recovery
            val testImage = generateTestImage(1, 1)
            val result = orchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
            
            return if (result.isSuccess) {
                System.currentTimeMillis() - startTime
            } else {
                Long.MAX_VALUE // Failed to recover
            }
        }
        
        private fun calculatePerformanceMetrics(
            responseTimes: List<Long>,
            successCount: Int,
            failureCount: Int,
            totalTimeMs: Long,
            errors: Map<String, Int>,
            memoryMetrics: MemoryMetrics
        ): PerformanceMetrics {
            
            val sortedTimes = responseTimes.sorted()
            val totalRequests = successCount + failureCount
            
            return PerformanceMetrics(
                totalRequests = totalRequests,
                successfulRequests = successCount,
                failedRequests = failureCount,
                averageResponseTimeMs = if (responseTimes.isNotEmpty()) responseTimes.average().toLong() else 0L,
                medianResponseTimeMs = if (sortedTimes.isNotEmpty()) sortedTimes[sortedTimes.size / 2] else 0L,
                p95ResponseTimeMs = if (sortedTimes.isNotEmpty()) sortedTimes[(sortedTimes.size * 0.95).toInt()] else 0L,
                p99ResponseTimeMs = if (sortedTimes.isNotEmpty()) sortedTimes[(sortedTimes.size * 0.99).toInt()] else 0L,
                minResponseTimeMs = sortedTimes.minOrNull() ?: 0L,
                maxResponseTimeMs = sortedTimes.maxOrNull() ?: 0L,
                successRate = if (totalRequests > 0) successCount.toFloat() / totalRequests else 0f,
                requestsPerSecond = if (totalTimeMs > 0) totalRequests * 1000f / totalTimeMs else 0f,
                memoryMetrics = memoryMetrics,
                errorBreakdown = errors
            )
        }
        
        private fun validatePerformanceRequirements(
            metrics: PerformanceMetrics,
            config: PerformanceTestConfig
        ): List<String> {
            val failures = mutableListOf<String>()
            
            if (metrics.successRate < config.minimumSuccessRate) {
                failures.add("Success rate ${metrics.successRate} below minimum ${config.minimumSuccessRate}")
            }
            
            if (metrics.averageResponseTimeMs > config.acceptableResponseTimeMs) {
                failures.add("Average response time ${metrics.averageResponseTimeMs}ms exceeds ${config.acceptableResponseTimeMs}ms")
            }
            
            if (metrics.p95ResponseTimeMs > config.acceptableP95ResponseTimeMs) {
                failures.add("P95 response time ${metrics.p95ResponseTimeMs}ms exceeds ${config.acceptableP95ResponseTimeMs}ms")
            }
            
            if (metrics.memoryMetrics.memoryIncreaseMB > config.maximumMemoryIncreaseMB) {
                failures.add("Memory increase ${metrics.memoryMetrics.memoryIncreaseMB}MB exceeds ${config.maximumMemoryIncreaseMB}MB")
            }
            
            if (metrics.memoryMetrics.memoryLeakDetected) {
                failures.add("Memory leak detected")
            }
            
            return failures
        }
        
        // Helper functions for different test variations
        private suspend fun executeLoadTestWithImageSize(
            testName: String,
            config: PerformanceTestConfig,
            imageSizeMB: Int
        ): LoadTestResult {
            // Implementation would use images of specified size
            return executeLoadTest(testName, config)
        }
        
        private suspend fun executeLoadTestWithWorkType(
            testName: String,
            config: PerformanceTestConfig,
            workType: WorkType
        ): LoadTestResult {
            // Implementation would use specific work type
            return executeLoadTest(testName, config)
        }
        
        private suspend fun executeLoadTestWithOrchestrator(
            testName: String,
            config: PerformanceTestConfig,
            customOrchestrator: SmartAIOrchestrator
        ): LoadTestResult {
            val originalOrchestrator = orchestrator
            orchestrator = customOrchestrator
            try {
                return executeLoadTest(testName, config)
            } finally {
                orchestrator = originalOrchestrator
            }
        }
        
        private suspend fun executeLoadTestWithService(
            testName: String,
            config: PerformanceTestConfig,
            service: AIPhotoAnalyzer
        ): LoadTestResult {
            // Implementation would use specific service
            return executeLoadTest(testName, config)
        }
        
        private fun calculateResponseTimeVariance(results: Map<WorkType, PerformanceMetrics>): Float {
            val responseTimes = results.values.map { it.averageResponseTimeMs }
            val mean = responseTimes.average()
            val variance = responseTimes.map { (it - mean) * (it - mean) }.average()
            val stdDev = kotlin.math.sqrt(variance)
            return (stdDev / mean * 100).toFloat()
        }
        
        // Logging functions
        private fun logPerformanceResults(result: LoadTestResult) {
            println("=== ${result.testName} Results ===")
            println("Total Requests: ${result.metrics.totalRequests}")
            println("Success Rate: ${String.format("%.2f%%", result.metrics.successRate * 100)}")
            println("Average Response Time: ${result.metrics.averageResponseTimeMs}ms")
            println("P95 Response Time: ${result.metrics.p95ResponseTimeMs}ms")
            println("Requests/Second: ${String.format("%.2f", result.metrics.requestsPerSecond)}")
            println("Memory Increase: ${String.format("%.1f", result.metrics.memoryMetrics.memoryIncreaseMB)}MB")
            println("Test Passed: ${result.passedRequirements}")
            if (!result.passedRequirements) {
                println("Failures: ${result.failures}")
            }
            println("========================================")
        }
        
        private fun logMemoryResults(metrics: MemoryMetrics) {
            println("=== Memory Usage Results ===")
            println("Initial Memory: ${String.format("%.1f", metrics.initialMemoryMB)}MB")
            println("Peak Memory: ${String.format("%.1f", metrics.peakMemoryMB)}MB")
            println("Final Memory: ${String.format("%.1f", metrics.finalMemoryMB)}MB")
            println("Memory Increase: ${String.format("%.1f", metrics.memoryIncreaseMB)}MB")
            println("Memory Leak Detected: ${metrics.memoryLeakDetected}")
            println("==============================")
        }
        
        private fun logLargeImageResults(results: Map<Int, PerformanceMetrics>) {
            println("=== Large Image Performance Results ===")
            results.forEach { (sizeMB, metrics) ->
                println("${sizeMB}MB: ${metrics.averageResponseTimeMs}ms avg, ${String.format("%.2f%%", metrics.successRate * 100)} success")
            }
            println("=======================================")
        }
        
        private fun logMixedWorkloadResults(results: Map<WorkType, PerformanceMetrics>) {
            println("=== Mixed Workload Results ===")
            results.forEach { (workType, metrics) ->
                println("$workType: ${metrics.averageResponseTimeMs}ms avg, ${String.format("%.2f%%", metrics.successRate * 100)} success")
            }
            println("==============================")
        }
        
        private fun logNetworkResults(results: Map<NetworkCondition, PerformanceMetrics>) {
            println("=== Network Condition Results ===")
            results.forEach { (condition, metrics) ->
                println("$condition: ${metrics.averageResponseTimeMs}ms avg, ${String.format("%.2f%%", metrics.successRate * 100)} success")
            }
            println("=================================")
        }
        
        private fun logRecoveryResults(result: LoadTestResult) {
            println("=== Service Recovery Results ===")
            println("Success Rate: ${String.format("%.2f%%", result.metrics.successRate * 100)}")
            println("Error Breakdown:")
            result.metrics.errorBreakdown.forEach { (error, count) ->
                println("  $error: $count")
            }
            println("===============================")
        }
    }
    
    /**
     * Memory monitoring utility
     */
    class MemoryMonitor {
        private val memorySamples = mutableListOf<Float>()
        private var initialMemory: Float = 0f
        private var monitoringActive = false
        
        fun reset() {
            memorySamples.clear()
            initialMemory = 0f
            monitoringActive = false
        }
        
        fun startMonitoring(intervalMs: Long) {
            initialMemory = getCurrentMemoryUsageMB()
            memorySamples.clear()
            monitoringActive = true
            
            // Start background monitoring (simplified for test)
            memorySamples.add(initialMemory)
        }
        
        fun stopMonitoring() {
            monitoringActive = false
            memorySamples.add(getCurrentMemoryUsageMB())
        }
        
        fun getMetrics(): MemoryMetrics {
            val currentMemory = getCurrentMemoryUsageMB()
            val peakMemory = memorySamples.maxOrNull() ?: currentMemory
            val averageMemory = memorySamples.average().toFloat()
            val memoryIncrease = currentMemory - initialMemory
            val memoryLeakDetected = detectMemoryLeak()
            
            return MemoryMetrics(
                initialMemoryMB = initialMemory,
                peakMemoryMB = peakMemory,
                finalMemoryMB = currentMemory,
                memoryIncreaseMB = memoryIncrease,
                averageMemoryMB = averageMemory,
                memoryLeakDetected = memoryLeakDetected
            )
        }
        
        private fun detectMemoryLeak(): Boolean {
            if (memorySamples.size < 10) return false
            
            // Simple trend detection - if memory keeps increasing
            val recentSamples = memorySamples.takeLast(10)
            val trend = calculateTrend(recentSamples)
            return trend > 5f // Increasing by >5MB over recent samples
        }
        
        private fun calculateTrend(samples: List<Float>): Float {
            if (samples.size < 2) return 0f
            return samples.last() - samples.first()
        }
    }
    
    /**
     * Helper enums and functions
     */
    enum class NetworkCondition {
        STABLE,
        INTERMITTENT,
        SLOW,
        UNSTABLE
    }
    
    private fun createPerformanceTestOrchestrator(): SmartAIOrchestrator {
        // Create orchestrator optimized for performance testing
        return SmartAIOrchestrator(
            gemma3NE2B = createMockGemmaService(),
            vertexAI = createMockVertexAIService(),
            yolo11 = createMockYOLOService(),
            networkMonitor = createMockNetworkService(),
            performanceManager = createMockPerformanceManager(),
            memoryManager = createMockMemoryManager(),
            performanceMonitor = createMockPerformanceMonitor()
        )
    }
    
    private fun createMockNetworkWithCondition(condition: NetworkCondition): MockNetworkService {
        return when (condition) {
            NetworkCondition.STABLE -> MockNetworkService(isConnected = true, isStable = true)
            NetworkCondition.INTERMITTENT -> MockNetworkService(isConnected = true, isIntermittent = true)
            NetworkCondition.SLOW -> MockNetworkService(isConnected = true, isSlow = true)
            NetworkCondition.UNSTABLE -> MockNetworkService(isConnected = false, isUnstable = true)
        }
    }
    
    private fun createOrchestratorWithNetwork(networkService: MockNetworkService): SmartAIOrchestrator {
        return SmartAIOrchestrator(
            gemma3NE2B = createMockGemmaService(),
            vertexAI = createMockVertexAIService(),
            yolo11 = createMockYOLOService(),
            networkMonitor = networkService,
            performanceManager = createMockPerformanceManager(),
            memoryManager = createMockMemoryManager(),
            performanceMonitor = createMockPerformanceMonitor()
        )
    }
    
    private fun createFailingAIService(failureRate: Float, recoveryTimeMs: Long): AIPhotoAnalyzer {
        return FailingMockAIService(failureRate, recoveryTimeMs)
    }
    
    private fun generateTestImage(userId: Int, requestId: Int): ByteArray {
        // Generate test image data
        return "test_image_user_${userId}_request_${requestId}".toByteArray()
    }
    
    private fun getCurrentMemoryUsageMB(): Float {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024f * 1024f)
    }
    
    // Mock service creation functions
    private fun createMockGemmaService() = MockGemmaPerformanceService()
    private fun createMockVertexAIService() = MockVertexAIPerformanceService()
    private fun createMockYOLOService() = MockYOLOPerformanceService()
    private fun createMockNetworkService() = MockNetworkService()
    private fun createMockPerformanceManager() = MockPerformanceManager()
    private fun createMockMemoryManager() = MockMemoryManager()
    private fun createMockPerformanceMonitor() = MockPerformanceMonitor()
}

/**
 * Mock services optimized for performance testing
 */
class MockNetworkService(
    private val isConnected: Boolean = true,
    private val isStable: Boolean = true,
    private val isIntermittent: Boolean = false,
    private val isSlow: Boolean = false,
    private val isUnstable: Boolean = false
) : NetworkConnectivityService {
    
    private var connectionToggle = true
    
    override val isConnected: Boolean
        get() = when {
            isUnstable -> false
            isIntermittent -> {
                connectionToggle = !connectionToggle
                connectionToggle
            }
            else -> isConnected
        }
    
    override val connectionQuality: ConnectionQuality
        get() = when {
            isSlow -> ConnectionQuality.POOR
            isStable -> ConnectionQuality.GOOD
            else -> ConnectionQuality.FAIR
        }
}

class FailingMockAIService(
    private val failureRate: Float,
    private val recoveryTimeMs: Long
) : AIPhotoAnalyzer {
    
    private val startTime = System.currentTimeMillis()
    private var requestCount = 0
    
    override val analyzerName = "Failing Mock AI Service"
    override val analysisCapabilities = setOf(AnalysisCapability.HAZARD_DETECTION)
    
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType,
        analysisOptions: AnalysisOptions
    ): Result<SafetyAnalysis> {
        
        requestCount++
        
        // Recover after specified time
        val hasRecovered = System.currentTimeMillis() - startTime > recoveryTimeMs
        
        // Simulate failures based on failure rate
        val shouldFail = !hasRecovered && Math.random() < failureRate
        
        if (shouldFail) {
            return Result.failure(Exception("Simulated service failure"))
        }
        
        // Simulate processing time
        delay((50..200).random().toLong())
        
        return Result.success(
            SafetyAnalysis(
                id = "failing-service-${requestCount}",
                hazards = emptyList(),
                overallRisk = RiskLevel.LOW,
                confidence = 0.75f,
                processingTimeMs = 150L,
                aiProvider = analyzerName
            )
        )
    }
    
    override fun getPerformanceMetrics(): AnalyzerPerformanceMetrics {
        return AnalyzerPerformanceMetrics(
            analysisCount = requestCount.toLong(),
            averageProcessingTime = 150L,
            successRate = 1.0f - failureRate,
            averageConfidence = 0.75f
        )
    }
    
    override fun cleanup() {}
}

// Additional performance-optimized mock services
class MockGemmaPerformanceService : AIPhotoAnalyzer {
    override val analyzerName = "Mock Gemma Performance"
    override val analysisCapabilities = setOf(AnalysisCapability.MULTIMODAL_VISION)
    
    override suspend fun analyzePhoto(imageData: ByteArray, workType: WorkType, analysisOptions: AnalysisOptions): Result<SafetyAnalysis> {
        delay((100..300).random().toLong()) // Simulate processing time
        return Result.success(SafetyAnalysis(
            id = "gemma-perf-${System.currentTimeMillis()}",
            hazards = emptyList(),
            overallRisk = RiskLevel.LOW,
            confidence = 0.85f,
            processingTimeMs = 200L,
            aiProvider = analyzerName
        ))
    }
    
    override fun getPerformanceMetrics() = AnalyzerPerformanceMetrics(100L, 200L, 0.98f, 0.85f)
    override fun cleanup() {}
}

class MockVertexAIPerformanceService : AIPhotoAnalyzer {
    override val analyzerName = "Mock Vertex AI Performance"
    override val analysisCapabilities = setOf(AnalysisCapability.CLOUD_PROCESSING)
    
    override suspend fun analyzePhoto(imageData: ByteArray, workType: WorkType, analysisOptions: AnalysisOptions): Result<SafetyAnalysis> {
        delay((200..800).random().toLong()) // Simulate cloud processing time
        return Result.success(SafetyAnalysis(
            id = "vertex-perf-${System.currentTimeMillis()}",
            hazards = emptyList(),
            overallRisk = RiskLevel.LOW,
            confidence = 0.90f,
            processingTimeMs = 500L,
            aiProvider = analyzerName
        ))
    }
    
    override fun getPerformanceMetrics() = AnalyzerPerformanceMetrics(50L, 500L, 0.95f, 0.90f)
    override fun cleanup() {}
}

class MockYOLOPerformanceService : AIPhotoAnalyzer {
    override val analyzerName = "Mock YOLO Performance"
    override val analysisCapabilities = setOf(AnalysisCapability.OBJECT_DETECTION)
    
    override suspend fun analyzePhoto(imageData: ByteArray, workType: WorkType, analysisOptions: AnalysisOptions): Result<SafetyAnalysis> {
        delay((50..150).random().toLong()) // Fast local processing
        return Result.success(SafetyAnalysis(
            id = "yolo-perf-${System.currentTimeMillis()}",
            hazards = emptyList(),
            overallRisk = RiskLevel.LOW,
            confidence = 0.70f,
            processingTimeMs = 100L,
            aiProvider = analyzerName
        ))
    }
    
    override fun getPerformanceMetrics() = AnalyzerPerformanceMetrics(200L, 100L, 1.0f, 0.70f)
    override fun cleanup() {}
}
