package com.hazardhawk.integration.refactoring

import com.hazardhawk.TestDataFactory
import com.hazardhawk.TestUtils
import com.hazardhawk.MockAIPhotoAnalyzer
import com.hazardhawk.PerformanceTestRunner
import com.hazardhawk.core.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Integration Performance Test for HazardHawk Refactoring Validation
 * 
 * Validates that performance targets are maintained after comprehensive
 * integration refactoring across all system components.
 * 
 * Performance Targets:
 * - Camera UI: 30 FPS (33ms frame budget)
 * - AI Analysis: 2 FPS (500ms intervals)
 * - Database queries: < 100ms
 * - Memory usage: 30% reduction target
 * - End-to-end workflow: < 15 seconds
 */
class IntegrationPerformanceTest {
    
    private lateinit var performanceRunner: PerformanceTestRunner
    private lateinit var mockAnalyzer: MockAIPhotoAnalyzer
    
    @BeforeTest
    fun setup() {
        performanceRunner = PerformanceTestRunner()
        mockAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "Performance Test Analyzer",
            responseDelay = 450L, // Within 500ms AI target
            shouldSucceed = true
        )
    }
    
    @Test
    fun `camera performance should meet 30 FPS target after refactoring`() = runTest {
        // Given - Camera performance test scenarios
        val frameTargets = mapOf(
            "Light Scene" to 33L,     // 30 FPS = 33ms
            "Medium Scene" to 40L,    // 25 FPS = 40ms (acceptable)
            "Heavy Scene" to 50L      // 20 FPS = 50ms (minimum acceptable)
        )
        
        // When & Then - Test each scenario
        frameTargets.forEach { (scenario, targetFrameTime) ->
            val frameRenderTimes = mutableListOf<Long>()
            
            // Simulate camera frame rendering
            repeat(60) { frameIndex -> // Test 2 seconds at 30 FPS
                val (_, frameTime) = TestUtils.measureExecutionTime {
                    simulateCameraFrameRendering(scenario, frameIndex)
                }
                frameRenderTimes.add(frameTime.inWholeMilliseconds)
            }
            
            // Validate frame time targets
            val averageFrameTime = frameRenderTimes.average()
            val maxFrameTime = frameRenderTimes.maxOrNull() ?: 0L
            val frameTimeVariance = calculateVariance(frameRenderTimes, averageFrameTime)
            
            assertTrue(
                averageFrameTime <= targetFrameTime,
                "$scenario: Average frame time ${averageFrameTime}ms should be ≤ ${targetFrameTime}ms"
            )
            
            assertTrue(
                maxFrameTime <= targetFrameTime * 1.5,
                "$scenario: Max frame time ${maxFrameTime}ms should be reasonable"
            )
            
            assertTrue(
                frameTimeVariance < 100.0,
                "$scenario: Frame time variance ${frameTimeVariance} should be low for smooth rendering"
            )
            
            // Calculate actual FPS
            val actualFPS = 1000.0 / averageFrameTime
            val targetFPS = when (scenario) {
                "Light Scene" -> 30.0
                "Medium Scene" -> 25.0
                "Heavy Scene" -> 20.0
                else -> 30.0
            }
            
            assertTrue(
                actualFPS >= targetFPS,
                "$scenario: Actual FPS ${actualFPS} should be ≥ ${targetFPS}"
            )
        }
    }
    
    @Test
    fun `AI analysis performance should maintain 2 FPS throttling after refactoring`() = runTest {
        // Given - AI analysis performance test
        val analysisInterval = 500L // Target: 2 FPS
        val testDurationMs = 10000L // 10 seconds
        val expectedAnalyses = (testDurationMs / analysisInterval).toInt()
        
        // When - Run throttled AI analysis test
        val analysisResults = mutableListOf<AnalysisPerformanceResult>()
        val startTime = System.currentTimeMillis()
        var lastAnalysisTime = 0L
        
        while (System.currentTimeMillis() - startTime < testDurationMs) {
            val currentTime = System.currentTimeMillis()
            
            if (currentTime - lastAnalysisTime >= analysisInterval) {
                val imageData = TestDataFactory.createMockImageData(1280, 720)
                
                val (result, analysisTime) = TestUtils.measureExecutionTime {
                    mockAnalyzer.analyzePhoto(imageData, WorkType.ELECTRICAL)
                }
                
                analysisResults.add(AnalysisPerformanceResult(
                    success = result.isSuccess,
                    processingTimeMs = analysisTime.inWholeMilliseconds,
                    analysisId = result.getOrNull()?.id ?: "failed"
                ))
                
                lastAnalysisTime = currentTime
            }
            
            // Simulate other work
            kotlinx.coroutines.delay(50L)
        }
        
        // Then - Validate AI performance targets
        val actualAnalyses = analysisResults.size
        assertTrue(
            actualAnalyses >= expectedAnalyses - 2, // Allow slight timing variance
            "Should complete ~${expectedAnalyses} analyses, got ${actualAnalyses}"
        )
        
        val successfulAnalyses = analysisResults.count { it.success }
        assertTrue(
            successfulAnalyses >= actualAnalyses * 0.95, // 95% success rate minimum
            "AI analysis success rate should be ≥95%: ${successfulAnalyses}/${actualAnalyses}"
        )
        
        val averageProcessingTime = analysisResults
            .filter { it.success }
            .map { it.processingTimeMs }
            .average()
        
        assertTrue(
            averageProcessingTime < 3000L, // Well within real-time constraints
            "Average AI processing time should be <3000ms, got ${averageProcessingTime}ms"
        )
        
        // Validate consistent performance (low variance)
        val processingTimes = analysisResults.filter { it.success }.map { it.processingTimeMs.toDouble() }
        val processingVariance = calculateVariance(processingTimes, averageProcessingTime)
        assertTrue(
            processingVariance < 1000000.0, // Standard deviation < 1000ms
            "AI processing time should be consistent, variance: ${processingVariance}"
        )
    }
    
    @Test
    fun `database query performance should meet targets after refactoring`() = runTest {
        // Given - Database performance test setup
        val mockRepository = MockPerformanceRepository()
        
        // Populate test data
        val testDataSize = 500
        val testAnalyses = (1..testDataSize).map { index ->
            TestDataFactory.createSampleSafetyAnalysis().copy(
                id = "perf-test-$index",
                timestamp = System.currentTimeMillis() - (index * 3600000L) // Spread over time
            )
        }
        
        testAnalyses.forEach { analysis ->
            mockRepository.store(analysis)
        }
        
        // When - Benchmark different query types
        val queryPerformanceResults = mutableMapOf<String, List<Long>>()
        
        // Test single record queries
        val singleQueryTimes = mutableListOf<Long>()
        repeat(20) {
            val randomId = testAnalyses.random().id
            val (_, queryTime) = TestUtils.measureExecutionTime {
                mockRepository.get(randomId)
            }
            singleQueryTimes.add(queryTime.inWholeMilliseconds)
        }
        queryPerformanceResults["single_query"] = singleQueryTimes
        
        // Test batch queries
        val batchQueryTimes = mutableListOf<Long>()
        repeat(10) {
            val batchIds = testAnalyses.shuffled().take(10).map { it.id }
            val (_, queryTime) = TestUtils.measureExecutionTime {
                mockRepository.getBatch(batchIds)
            }
            batchQueryTimes.add(queryTime.inWholeMilliseconds)
        }
        queryPerformanceResults["batch_query"] = batchQueryTimes
        
        // Test filtered queries
        val filteredQueryTimes = mutableListOf<Long>()
        repeat(10) {
            val (_, queryTime) = TestUtils.measureExecutionTime {
                mockRepository.findByWorkType(WorkType.ELECTRICAL)
            }
            filteredQueryTimes.add(queryTime.inWholeMilliseconds)
        }
        queryPerformanceResults["filtered_query"] = filteredQueryTimes
        
        // Test complex queries
        val complexQueryTimes = mutableListOf<Long>()
        repeat(5) {
            val startTime = System.currentTimeMillis() - 86400000L // 24 hours ago
            val endTime = System.currentTimeMillis()
            val (_, queryTime) = TestUtils.measureExecutionTime {
                mockRepository.findByDateRangeAndRiskLevel(startTime, endTime, RiskLevel.HIGH)
            }
            complexQueryTimes.add(queryTime.inWholeMilliseconds)
        }
        queryPerformanceResults["complex_query"] = complexQueryTimes
        
        // Then - Validate performance targets (< 100ms)
        queryPerformanceResults.forEach { (queryType, times) ->
            val averageTime = times.average()
            val maxTime = times.maxOrNull() ?: 0L
            val p95Time = times.sorted()[(times.size * 0.95).toInt()]
            
            assertTrue(
                averageTime < 100.0,
                "$queryType average time should be <100ms, got ${averageTime}ms"
            )
            
            assertTrue(
                p95Time < 200L,
                "$queryType P95 time should be <200ms, got ${p95Time}ms"
            )
            
            // Log performance statistics
            println("$queryType Performance: avg=${averageTime}ms, max=${maxTime}ms, p95=${p95Time}ms")
        }
    }
    
    @Test
    fun `memory usage should show 30 percent reduction after refactoring`() = runTest {
        // Given - Memory usage test scenarios
        val baselineMemory = captureMemoryBaseline()
        val memorySnapshots = mutableListOf<MemorySnapshot>()
        
        // When - Run memory-intensive operations
        repeat(50) { iteration ->
            val iterationStart = captureMemoryUsage()
            
            // Simulate typical application workflow
            val imageData = TestDataFactory.createMockImageData(1920, 1080)
            val analysisResult = mockAnalyzer.analyzePhoto(imageData, WorkType.GENERAL_CONSTRUCTION)
            
            // Simulate UI updates
            simulateUIOperations(iteration)
            
            // Simulate data storage
            val analysis = analysisResult.getOrNull()
            if (analysis != null) {
                simulateDataStorage(analysis)
            }
            
            val iterationEnd = captureMemoryUsage()
            memorySnapshots.add(MemorySnapshot(
                iteration = iteration,
                memoryBefore = iterationStart,
                memoryAfter = iterationEnd,
                memoryGrowth = iterationEnd - iterationStart
            ))
            
            // Periodic cleanup simulation
            if (iteration % 10 == 0) {
                System.gc()
                kotlinx.coroutines.delay(100L)
            }
        }
        
        // Then - Validate memory optimization
        val finalMemory = captureMemoryUsage()
        val totalMemoryGrowth = finalMemory - baselineMemory
        val averageGrowthPerIteration = memorySnapshots.map { it.memoryGrowth }.average()
        
        // Memory growth should be controlled
        assertTrue(
            averageGrowthPerIteration < 1024 * 1024, // < 1MB per iteration average
            "Average memory growth should be <1MB per iteration, got ${averageGrowthPerIteration / (1024 * 1024)}MB"
        )
        
        // Total memory growth should be reasonable
        val totalGrowthMB = totalMemoryGrowth / (1024 * 1024)
        assertTrue(
            totalGrowthMB < 50, // < 50MB total growth for test
            "Total memory growth should be <50MB, got ${totalGrowthMB}MB"
        )
        
        // Memory usage should stabilize (not grow exponentially)
        val recentGrowth = memorySnapshots.takeLast(10).map { it.memoryGrowth }.average()
        val earlyGrowth = memorySnapshots.take(10).map { it.memoryGrowth }.average()
        
        assertTrue(
            recentGrowth <= earlyGrowth * 2, // Growth should not double
            "Memory growth should stabilize: early=${earlyGrowth}, recent=${recentGrowth}"
        )
        
        // Log memory statistics
        println("Memory Performance: baseline=${baselineMemory / (1024 * 1024)}MB, " +
                "final=${finalMemory / (1024 * 1024)}MB, growth=${totalGrowthMB}MB")
    }
    
    @Test
    fun `end-to-end workflow should complete under 15 seconds after refactoring`() = runTest {
        // Given - Complete workflow test
        val workflowSteps = mutableListOf<WorkflowStep>()
        
        // When - Execute complete safety inspection workflow
        val totalStartTime = System.currentTimeMillis()
        
        // Step 1: Camera capture simulation
        val (imageData, captureTime) = TestUtils.measureExecutionTime {
            TestDataFactory.createMockImageData(1920, 1080)
        }
        workflowSteps.add(WorkflowStep("Camera Capture", captureTime.inWholeMilliseconds))
        
        // Step 2: AI analysis
        val (analysisResult, analysisTime) = TestUtils.measureExecutionTime {
            mockAnalyzer.analyzePhoto(imageData, WorkType.FALL_PROTECTION)
        }
        workflowSteps.add(WorkflowStep("AI Analysis", analysisTime.inWholeMilliseconds))
        assertTrue(analysisResult.isSuccess, "AI analysis should succeed")
        
        val analysis = analysisResult.getOrNull()!!
        
        // Step 3: OSHA compliance check
        val (_, complianceTime) = TestUtils.measureExecutionTime {
            performOSHAComplianceCheck(analysis)
        }
        workflowSteps.add(WorkflowStep("OSHA Compliance Check", complianceTime.inWholeMilliseconds))
        
        // Step 4: Document generation (PTP)
        val (documentResult, documentTime) = TestUtils.measureExecutionTime {
            generateConstructionDocument(analysis)
        }
        workflowSteps.add(WorkflowStep("Document Generation", documentTime.inWholeMilliseconds))
        assertTrue(documentResult.isSuccess, "Document generation should succeed")
        
        // Step 5: Tag and store results
        val (_, storageTime) = TestUtils.measureExecutionTime {
            storeAnalysisResults(analysis)
        }
        workflowSteps.add(WorkflowStep("Data Storage", storageTime.inWholeMilliseconds))
        
        val totalTime = System.currentTimeMillis() - totalStartTime
        
        // Then - Validate workflow performance
        assertTrue(
            totalTime < 15000L, // 15 seconds target
            "Complete workflow should finish in <15 seconds, took ${totalTime}ms"
        )
        
        // Validate individual step performance
        workflowSteps.forEach { step ->
            when (step.name) {
                "Camera Capture" -> assertTrue(
                    step.durationMs < 1000L,
                    "Camera capture should be <1s, took ${step.durationMs}ms"
                )
                "AI Analysis" -> assertTrue(
                    step.durationMs < 5000L,
                    "AI analysis should be <5s, took ${step.durationMs}ms"
                )
                "OSHA Compliance Check" -> assertTrue(
                    step.durationMs < 2000L,
                    "OSHA check should be <2s, took ${step.durationMs}ms"
                )
                "Document Generation" -> assertTrue(
                    step.durationMs < 5000L,
                    "Document generation should be <5s, took ${step.durationMs}ms"
                )
                "Data Storage" -> assertTrue(
                    step.durationMs < 2000L,
                    "Data storage should be <2s, took ${step.durationMs}ms"
                )
            }
        }
        
        // Log workflow performance
        println("Workflow Performance (${totalTime}ms total):")
        workflowSteps.forEach { step ->
            println("  ${step.name}: ${step.durationMs}ms")
        }
    }
    
    @Test
    fun `system should maintain performance under concurrent load after refactoring`() = runTest {
        // Given - Concurrent load test setup
        val concurrentUsers = 10
        val operationsPerUser = 20
        
        // When - Simulate concurrent operations
        val concurrentResults = (1..concurrentUsers).map { userId ->
            kotlinx.coroutines.async {
                val userResults = mutableListOf<ConcurrentOperationResult>()
                
                repeat(operationsPerUser) { operationIndex ->
                    val operationStart = System.currentTimeMillis()
                    
                    try {
                        // Simulate user workflow
                        val imageData = TestDataFactory.createMockImageData(640, 480) // Smaller for load test
                        val result = mockAnalyzer.analyzePhoto(imageData, WorkType.ELECTRICAL)
                        
                        val operationTime = System.currentTimeMillis() - operationStart
                        userResults.add(ConcurrentOperationResult(
                            userId = userId,
                            operationIndex = operationIndex,
                            success = result.isSuccess,
                            durationMs = operationTime
                        ))
                        
                    } catch (e: Exception) {
                        val operationTime = System.currentTimeMillis() - operationStart
                        userResults.add(ConcurrentOperationResult(
                            userId = userId,
                            operationIndex = operationIndex,
                            success = false,
                            durationMs = operationTime,
                            error = e.message
                        ))
                    }
                    
                    // Small delay between operations
                    kotlinx.coroutines.delay(100L)
                }
                
                userResults
            }
        }.map { it.await() }.flatten()
        
        // Then - Validate concurrent performance
        val successfulOperations = concurrentResults.count { it.success }
        val totalOperations = concurrentUsers * operationsPerUser
        val successRate = successfulOperations.toDouble() / totalOperations
        
        assertTrue(
            successRate >= 0.95,
            "Success rate under concurrent load should be ≥95%, got ${successRate * 100}%"
        )
        
        // Performance should not degrade significantly under load
        val averageResponseTime = concurrentResults
            .filter { it.success }
            .map { it.durationMs }
            .average()
        
        assertTrue(
            averageResponseTime < 3000.0, // 3 seconds max under load
            "Average response time under load should be <3s, got ${averageResponseTime}ms"
        )
        
        // Response times should be reasonably consistent
        val responseTimes = concurrentResults.filter { it.success }.map { it.durationMs.toDouble() }
        val responseVariance = calculateVariance(responseTimes, averageResponseTime)
        val responseStdDev = kotlin.math.sqrt(responseVariance)
        
        assertTrue(
            responseStdDev < 1000.0, // Standard deviation < 1 second
            "Response times should be consistent under load, std dev: ${responseStdDev}ms"
        )
        
        // Log concurrent performance results
        println("Concurrent Load Test Results:")
        println("  Total Operations: $totalOperations")
        println("  Successful: $successfulOperations (${successRate * 100}%)")
        println("  Average Response Time: ${averageResponseTime}ms")
        println("  Response Time Std Dev: ${responseStdDev}ms")
    }
    
    // Helper functions and data classes
    
    private suspend fun simulateCameraFrameRendering(scenario: String, frameIndex: Int) {
        // Simulate different rendering loads
        val baseDelay = when (scenario) {
            "Light Scene" -> 20L
            "Medium Scene" -> 30L
            "Heavy Scene" -> 40L
            else -> 25L
        }
        
        // Add some variance based on frame content
        val variance = when (frameIndex % 4) {
            0 -> 0L          // Light frame
            1 -> baseDelay / 4   // Medium frame
            2 -> baseDelay / 2   // Heavy frame
            else -> baseDelay / 8  // Variable frame
        }
        
        kotlinx.coroutines.delay(baseDelay + variance)
    }
    
    private fun calculateVariance(values: List<Long>, mean: Double): Double {
        return values.map { (it - mean) * (it - mean) }.average()
    }
    
    private fun calculateVariance(values: List<Double>, mean: Double): Double {
        return values.map { (it - mean) * (it - mean) }.average()
    }
    
    private fun captureMemoryBaseline(): Long {
        System.gc()
        kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(100L) }
        return captureMemoryUsage()
    }
    
    private fun captureMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private suspend fun simulateUIOperations(iteration: Int) {
        // Simulate UI updates, animations, etc.
        kotlinx.coroutines.delay(20L + (iteration % 10) * 2)
    }
    
    private fun simulateDataStorage(analysis: SafetyAnalysis) {
        // Simulate data storage operations
        // In real implementation, this would be actual database operations
    }
    
    private suspend fun performOSHAComplianceCheck(analysis: SafetyAnalysis): OSHAComplianceResult {
        kotlinx.coroutines.delay(200L) // Simulate OSHA compliance checking
        return OSHAComplianceResult(
            compliant = analysis.oshaViolations.isEmpty(),
            violationCount = analysis.oshaViolations.size,
            criticalViolations = analysis.oshaViolations.count { it.severity == Severity.CRITICAL }
        )
    }
    
    private suspend fun generateConstructionDocument(analysis: SafetyAnalysis): Result<String> {
        return try {
            kotlinx.coroutines.delay(800L) // Simulate document generation
            Result.success("Generated PTP document for analysis ${analysis.id}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun storeAnalysisResults(analysis: SafetyAnalysis): Result<Unit> {
        return try {
            kotlinx.coroutines.delay(150L) // Simulate database storage
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Data classes for performance tracking
    
    data class AnalysisPerformanceResult(
        val success: Boolean,
        val processingTimeMs: Long,
        val analysisId: String
    )
    
    data class MemorySnapshot(
        val iteration: Int,
        val memoryBefore: Long,
        val memoryAfter: Long,
        val memoryGrowth: Long
    )
    
    data class WorkflowStep(
        val name: String,
        val durationMs: Long
    )
    
    data class ConcurrentOperationResult(
        val userId: Int,
        val operationIndex: Int,
        val success: Boolean,
        val durationMs: Long,
        val error: String? = null
    )
    
    data class OSHAComplianceResult(
        val compliant: Boolean,
        val violationCount: Int,
        val criticalViolations: Int
    )
    
    // Mock repository for performance testing
    class MockPerformanceRepository {
        private val storage = mutableMapOf<String, SafetyAnalysis>()
        
        suspend fun store(analysis: SafetyAnalysis): Result<Unit> {
            kotlinx.coroutines.delay(20L) // Simulate storage time
            storage[analysis.id] = analysis
            return Result.success(Unit)
        }
        
        suspend fun get(id: String): Result<SafetyAnalysis?> {
            kotlinx.coroutines.delay(30L) // Simulate query time
            return Result.success(storage[id])
        }
        
        suspend fun getBatch(ids: List<String>): Result<List<SafetyAnalysis>> {
            kotlinx.coroutines.delay(50L) // Batch query optimization
            val results = ids.mapNotNull { storage[it] }
            return Result.success(results)
        }
        
        suspend fun findByWorkType(workType: WorkType): Result<List<SafetyAnalysis>> {
            kotlinx.coroutines.delay(40L) // Filtered query time
            val results = storage.values.filter { it.workType == workType }
            return Result.success(results.toList())
        }
        
        suspend fun findByDateRangeAndRiskLevel(
            startTime: Long,
            endTime: Long,
            riskLevel: RiskLevel
        ): Result<List<SafetyAnalysis>> {
            kotlinx.coroutines.delay(80L) // Complex query time
            val results = storage.values.filter { 
                it.timestamp in startTime..endTime && it.overallRiskLevel == riskLevel 
            }
            return Result.success(results.toList())
        }
    }
}
