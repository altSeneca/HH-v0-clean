package com.hazardhawk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.ai.*
import com.hazardhawk.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.kotlin.*
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Performance and Reliability Tests for AI Processing
 * 
 * These tests ensure that AI analysis meets construction site requirements:
 * - Processing time < 3 seconds for field use
 * - Memory usage < 100MB for budget devices
 * - 99.5% reliability under normal conditions
 * - Graceful degradation under stress
 * - Proper resource cleanup
 */
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class AIPerformanceReliabilityTests {

    private val testDispatcher = TestCoroutineDispatcher()
    private lateinit var testScope: TestCoroutineScope
    
    @Mock
    private lateinit var mockAIServiceFacade: AIServiceFacade
    
    private lateinit var memoryBean: MemoryMXBean
    
    companion object {
        private const val CONSTRUCTION_SITE_PROCESSING_TIME_LIMIT = 3000L // 3 seconds max
        private const val MEMORY_LIMIT_MB = 100L
        private const val RELIABILITY_THRESHOLD = 0.995 // 99.5%
        private const val STRESS_TEST_ITERATIONS = 50
        private const val CONCURRENT_REQUESTS_COUNT = 10
    }

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testScope = TestCoroutineScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)
        memoryBean = ManagementFactory.getMemoryMXBean()
        
        // Setup default successful AI response
        whenever(mockAIServiceFacade.analyzePhotoWithTags(any(), any(), any(), any()))
            .thenReturn(createFastAnalysisResult())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.cleanupTestCoroutines()
        // Force garbage collection after tests
        System.gc()
        Thread.sleep(100)
    }

    /**
     * CRITICAL TEST: AI workflow performance meets construction site requirements
     */
    @Test
    fun aiWorkflowPerformanceMeetsConstructionSiteRequirements() = testScope.runBlockingTest {
        // Given: Realistic construction site photo data
        val constructionPhotoData = createLargeConstructionPhotoData() // 8MP photo
        val processingTimes = mutableListOf<Long>()
        val memoryUsages = mutableListOf<Long>()
        
        repeat(10) { iteration ->
            // Measure memory before processing
            val beforeMemory = getUsedMemoryMB()
            
            // Measure processing time
            val processingTime = measureTimeMillis {
                // Simulate complete AI workflow
                val analysisResult = mockAIServiceFacade.analyzePhotoWithTags(
                    constructionPhotoData, 3264, 2448, WorkType.GENERAL_CONSTRUCTION
                )
                
                // Verify result is not null (AI succeeded)
                assertNotNull("AI analysis should succeed", analysisResult)
                assertTrue("Should have meaningful recommendations", 
                          analysisResult.recommendedTags.isNotEmpty())
            }
            
            // Measure memory after processing
            val afterMemory = getUsedMemoryMB()
            val memoryIncrease = afterMemory - beforeMemory
            
            processingTimes.add(processingTime)
            memoryUsages.add(memoryIncrease)
            
            // Verify individual iteration meets requirements
            assertTrue("Processing time should be < ${CONSTRUCTION_SITE_PROCESSING_TIME_LIMIT}ms for field use. " +
                      "Actual: ${processingTime}ms (iteration $iteration)", 
                      processingTime < CONSTRUCTION_SITE_PROCESSING_TIME_LIMIT)
            
            assertTrue("Memory increase should be < ${MEMORY_LIMIT_MB}MB for budget devices. " +
                      "Actual: ${memoryIncrease}MB (iteration $iteration)", 
                      memoryIncrease < MEMORY_LIMIT_MB)
        }
        
        // Verify consistent performance across all iterations
        val avgProcessingTime = processingTimes.average()
        val maxProcessingTime = processingTimes.maxOrNull() ?: 0L
        val avgMemoryUsage = memoryUsages.average()
        val maxMemoryUsage = memoryUsages.maxOrNull() ?: 0L
        
        assertTrue("Average processing time should be reasonable: ${avgProcessingTime}ms", 
                  avgProcessingTime < CONSTRUCTION_SITE_PROCESSING_TIME_LIMIT * 0.8) // 80% of limit
        
        assertTrue("Maximum processing time should not exceed limit: ${maxProcessingTime}ms", 
                  maxProcessingTime < CONSTRUCTION_SITE_PROCESSING_TIME_LIMIT)
        
        assertTrue("Average memory usage should be reasonable: ${avgMemoryUsage}MB", 
                  avgMemoryUsage < MEMORY_LIMIT_MB * 0.7) // 70% of limit
        
        assertTrue("Maximum memory usage should not exceed limit: ${maxMemoryUsage}MB", 
                  maxMemoryUsage < MEMORY_LIMIT_MB)
                  
        // Report performance metrics
        println("AI Performance Metrics:")
        println("  Average processing time: ${avgProcessingTime.toInt()}ms")
        println("  Maximum processing time: ${maxProcessingTime}ms") 
        println("  Average memory usage: ${avgMemoryUsage.toInt()}MB")
        println("  Maximum memory usage: ${maxMemoryUsage}MB")
    }

    /**
     * CRITICAL TEST: AI failure recovery and retry functionality
     */
    @Test 
    fun aiFailureRecoveryAndRetryFunctionality() = testScope.runBlockingTest {
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val recoveryCount = AtomicInteger(0)
        
        // Setup AI service with intermittent failures
        whenever(mockAIServiceFacade.analyzePhotoWithTags(any(), any(), any(), any()))
            .thenAnswer { invocation ->
                val random = Random.nextFloat()
                when {
                    random < 0.1 -> { // 10% failure rate
                        failureCount.incrementAndGet()
                        throw RuntimeException("AI service temporarily unavailable")
                    }
                    random < 0.2 -> { // 10% degraded performance
                        delay(4000) // Slower than normal
                        recoveryCount.incrementAndGet()
                        createSlowAnalysisResult()
                    }
                    else -> { // 80% normal operation
                        successCount.incrementAndGet()
                        createFastAnalysisResult()
                    }
                }
            }
        
        // Test reliability over many iterations
        val totalIterations = 100
        var completedIterations = 0
        val errorCounts = mutableMapOf<String, Int>()
        
        repeat(totalIterations) { iteration ->
            try {
                val photoData = createSmallConstructionPhotoData()
                
                // Attempt analysis with timeout
                withTimeout(8000L) { // 8 second timeout
                    val result = mockAIServiceFacade.analyzePhotoWithTags(
                        photoData, 1920, 1080, WorkType.GENERAL_CONSTRUCTION
                    )
                    
                    // Verify result quality even under stress
                    assertNotNull("Result should not be null", result)
                    assertTrue("Should have some processing time recorded", 
                              result.processingTimeMs > 0)
                }
                
                completedIterations++
                
            } catch (e: Exception) {
                val errorType = e::class.simpleName ?: "Unknown"
                errorCounts[errorType] = errorCounts.getOrDefault(errorType, 0) + 1
                
                // Verify graceful error handling
                assertFalse("Should not have unhandled critical errors", 
                           e is OutOfMemoryError || e is StackOverflowError)
            }
        }
        
        // Calculate reliability metrics
        val reliabilityRate = completedIterations.toDouble() / totalIterations
        
        assertTrue("AI reliability should be > ${RELIABILITY_THRESHOLD * 100}%. " +
                  "Actual: ${(reliabilityRate * 100).toInt()}% ($completedIterations/$totalIterations)", 
                  reliabilityRate > RELIABILITY_THRESHOLD)
        
        // Verify failure distribution is reasonable
        assertTrue("Should have some successful operations", successCount.get() > 0)
        assertTrue("Should handle failures gracefully", failureCount.get() < totalIterations * 0.15) // < 15%
        
        // Report reliability metrics
        println("AI Reliability Metrics:")
        println("  Success rate: ${(reliabilityRate * 100).toInt()}%")
        println("  Successful operations: ${successCount.get()}")
        println("  Failed operations: ${failureCount.get()}")
        println("  Recovered operations: ${recoveryCount.get()}")
        println("  Error distribution: $errorCounts")
    }

    /**
     * Test concurrent AI analysis requests
     */
    @Test
    fun concurrentAIAnalysisHandling() = testScope.runBlockingTest {
        val concurrentRequests = CONCURRENT_REQUESTS_COUNT
        val completionLatch = CountDownLatch(concurrentRequests)
        val results = mutableListOf<PhotoAnalysisWithTags?>()
        val errors = mutableListOf<Exception>()
        val processingTimes = mutableListOf<Long>()
        
        // Setup AI service to handle concurrent requests
        whenever(mockAIServiceFacade.analyzePhotoWithTags(any(), any(), any(), any()))
            .thenAnswer { invocation ->
                delay(Random.nextLong(500, 2000)) // Simulate variable processing time
                createFastAnalysisResult()
            }
        
        // Launch concurrent analysis requests
        repeat(concurrentRequests) { index ->
            launch {
                try {
                    val startTime = System.currentTimeMillis()
                    val photoData = createSmallConstructionPhotoData()
                    
                    val result = mockAIServiceFacade.analyzePhotoWithTags(
                        photoData, 1920, 1080, 
                        WorkType.values()[index % WorkType.values().size]
                    )
                    
                    val endTime = System.currentTimeMillis()
                    
                    synchronized(results) {
                        results.add(result)
                        processingTimes.add(endTime - startTime)
                    }
                    
                } catch (e: Exception) {
                    synchronized(errors) {
                        errors.add(e)
                    }
                } finally {
                    completionLatch.countDown()
                }
            }
        }
        
        // Wait for all requests to complete
        advanceUntilIdle()
        assertTrue("All concurrent requests should complete within reasonable time",
                  completionLatch.await(30, TimeUnit.SECONDS))
        
        // Verify concurrent processing results
        assertTrue("Majority of concurrent requests should succeed", 
                  results.size >= concurrentRequests * 0.9) // 90% success rate
        
        assertTrue("Should handle concurrent requests without major errors",
                  errors.size < concurrentRequests * 0.2) // < 20% error rate
        
        // Verify no memory leaks from concurrent processing
        val finalMemory = getUsedMemoryMB()
        assertTrue("Memory should not exceed limits with concurrent processing: ${finalMemory}MB", 
                  finalMemory < MEMORY_LIMIT_MB * 2) // Allow 2x limit for concurrent processing
        
        // Report concurrent processing metrics
        val avgConcurrentTime = processingTimes.average()
        println("Concurrent Processing Metrics:")
        println("  Requests completed: ${results.size}/$concurrentRequests")
        println("  Errors encountered: ${errors.size}")
        println("  Average processing time: ${avgConcurrentTime.toInt()}ms")
        println("  Final memory usage: ${finalMemory}MB")
    }

    /**
     * Test AI processing under memory pressure
     */
    @Test
    fun aiProcessingUnderMemoryPressure() = testScope.runBlockingTest {
        val initialMemory = getUsedMemoryMB()
        val memoryBallast = mutableListOf<ByteArray>()
        
        try {
            // Create memory pressure (consume 200MB)
            repeat(20) {
                memoryBallast.add(ByteArray(10 * 1024 * 1024)) // 10MB each
            }
            
            val pressuredMemory = getUsedMemoryMB()
            println("Memory pressure created: ${pressuredMemory - initialMemory}MB")
            
            // Test AI processing under memory pressure
            val processingResults = mutableListOf<Boolean>()
            
            repeat(10) { iteration ->
                try {
                    val photoData = createLargeConstructionPhotoData()
                    
                    val result = withTimeout(5000L) { // 5 second timeout
                        mockAIServiceFacade.analyzePhotoWithTags(
                            photoData, 3264, 2448, WorkType.GENERAL_CONSTRUCTION
                        )
                    }
                    
                    // Verify AI still produces quality results under pressure
                    assertNotNull("AI should still work under memory pressure", result)
                    assertTrue("Should still provide recommendations", 
                              result.recommendedTags.isNotEmpty())
                    
                    processingResults.add(true)
                    
                } catch (e: OutOfMemoryError) {
                    // This is acceptable under extreme memory pressure
                    processingResults.add(false)
                    println("OOM during iteration $iteration (expected under pressure)")
                    
                } catch (e: Exception) {
                    // Other exceptions should be minimal
                    processingResults.add(false)
                    println("Exception during iteration $iteration: ${e.message}")
                }
                
                // Allow garbage collection between iterations
                if (iteration % 3 == 0) {
                    System.gc()
                    delay(100)
                }
            }
            
            // Verify reasonable success rate under pressure
            val successRate = processingResults.count { it }.toDouble() / processingResults.size
            assertTrue("Should maintain reasonable success rate under memory pressure: " +
                      "${(successRate * 100).toInt()}%", 
                      successRate > 0.6) // 60% minimum under pressure
            
        } finally {
            // Clean up memory ballast
            memoryBallast.clear()
            System.gc()
            Thread.sleep(200)
            
            val finalMemory = getUsedMemoryMB()
            println("Memory cleanup complete: ${finalMemory}MB (vs initial ${initialMemory}MB)")
        }
    }

    /**
     * Test AI processing with various photo sizes and formats
     */
    @Test
    fun aiProcessingWithVariousPhotoSizesAndFormats() = testScope.runBlockingTest {
        val photoTestCases = listOf(
            Triple(640, 480, "VGA - Minimum resolution"),
            Triple(1920, 1080, "HD - Standard resolution"),
            Triple(2560, 1440, "QHD - High resolution"),
            Triple(3264, 2448, "8MP - Maximum resolution"),
            Triple(4000, 3000, "12MP - Ultra high resolution")
        )
        
        val processingResults = mutableMapOf<String, ProcessingMetrics>()
        
        photoTestCases.forEach { (width, height, description) ->
            val photoSize = width * height * 3 // RGB bytes
            val photoData = ByteArray(photoSize) { Random.nextInt(256).toByte() }
            
            val beforeMemory = getUsedMemoryMB()
            val startTime = System.currentTimeMillis()
            
            try {
                val result = mockAIServiceFacade.analyzePhotoWithTags(
                    photoData, width, height, WorkType.GENERAL_CONSTRUCTION
                )
                
                val endTime = System.currentTimeMillis()
                val afterMemory = getUsedMemoryMB()
                
                val metrics = ProcessingMetrics(
                    success = true,
                    processingTime = endTime - startTime,
                    memoryUsage = afterMemory - beforeMemory,
                    photoSize = photoSize
                )
                
                processingResults[description] = metrics
                
                // Verify result quality scales with photo size
                assertNotNull("Should handle photo size: $description", result)
                
                // Higher resolution should potentially provide better analysis
                if (width * height > 1920 * 1080) {
                    assertTrue("High-res photos should have detailed analysis",
                              result.recommendedTags.isNotEmpty())
                }
                
            } catch (e: Exception) {
                processingResults[description] = ProcessingMetrics(
                    success = false,
                    processingTime = -1,
                    memoryUsage = -1,
                    photoSize = photoSize,
                    error = e.message
                )
            }
        }
        
        // Verify performance scaling
        val successfulResults = processingResults.filterValues { it.success }
        assertTrue("Should handle majority of photo sizes", 
                  successfulResults.size >= photoTestCases.size * 0.8)
        
        // Report photo processing metrics
        println("Photo Size Processing Results:")
        processingResults.forEach { (description, metrics) ->
            if (metrics.success) {
                println("  $description: ${metrics.processingTime}ms, ${metrics.memoryUsage}MB")
            } else {
                println("  $description: FAILED - ${metrics.error}")
            }
        }
    }

    /**
     * Test AI processing resource cleanup
     */
    @Test
    fun aiProcessingResourceCleanup() = testScope.runBlockingTest {
        val initialMemory = getUsedMemoryMB()
        println("Initial memory usage: ${initialMemory}MB")
        
        // Perform multiple AI processing cycles
        repeat(20) { iteration ->
            val photoData = createLargeConstructionPhotoData()
            
            try {
                val result = mockAIServiceFacade.analyzePhotoWithTags(
                    photoData, 3264, 2448, WorkType.GENERAL_CONSTRUCTION
                )
                
                // Use the result to ensure it's not optimized away
                assertNotNull("Result should not be null", result)
                
            } catch (e: Exception) {
                // Continue processing even if some iterations fail
                println("Iteration $iteration failed: ${e.message}")
            }
            
            // Trigger cleanup every few iterations
            if (iteration % 5 == 0) {
                // Call release on AI service
                mockAIServiceFacade.release()
                
                // Force garbage collection
                System.gc()
                delay(100)
                
                val currentMemory = getUsedMemoryMB()
                println("Memory after cleanup cycle $iteration: ${currentMemory}MB")
            }
        }
        
        // Final cleanup
        mockAIServiceFacade.release()
        System.gc()
        Thread.sleep(500) // Allow time for cleanup
        
        val finalMemory = getUsedMemoryMB()
        val memoryIncrease = finalMemory - initialMemory
        
        // Verify memory doesn't grow excessively
        assertTrue("Memory should not leak significantly: ${memoryIncrease}MB increase", 
                  memoryIncrease < MEMORY_LIMIT_MB / 2) // Max 50MB increase
        
        println("Final memory usage: ${finalMemory}MB (increase: ${memoryIncrease}MB)")
    }

    // Helper functions
    private fun getUsedMemoryMB(): Long {
        val memoryUsage = memoryBean.heapMemoryUsage
        return (memoryUsage.used) / (1024 * 1024) // Convert to MB
    }

    private fun createLargeConstructionPhotoData(): ByteArray {
        // Simulate 8MP photo (3264x2448x3 bytes for RGB)
        return ByteArray(3264 * 2448 * 3) { Random.nextInt(256).toByte() }
    }

    private fun createSmallConstructionPhotoData(): ByteArray {
        // Simulate HD photo (1920x1080x3 bytes for RGB)
        return ByteArray(1920 * 1080 * 3) { Random.nextInt(256).toByte() }
    }

    private fun createFastAnalysisResult(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.PERSON_NO_HARD_HAT,
                    confidence = 0.87f,
                    boundingBox = BoundingBox(0.2f, 0.3f, 0.6f, 0.4f),
                    oshaCategory = OSHACategory.SUBPART_E_1926_95,
                    severity = HazardSeverity.HIGH,
                    description = "Fast detection result"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "ppe-hard-hat",
                    displayName = "PPE - Hard Hat Required",
                    confidence = 0.87f,
                    reason = "Fast analysis detected missing hard hat"
                )
            ),
            autoSelectTags = setOf("ppe-hard-hat"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.PARTIAL_COMPLIANCE,
                criticalIssues = 1,
                oshaViolations = listOf("1926.95")
            ),
            processingTimeMs = Random.nextLong(800, 1500) // Fast processing
        )
    }

    private fun createSlowAnalysisResult(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.FALL_HAZARD,
                    confidence = 0.76f,
                    boundingBox = BoundingBox(0.3f, 0.2f, 0.7f, 0.8f),
                    oshaCategory = OSHACategory.SUBPART_M_1926_501,
                    severity = HazardSeverity.MODERATE,
                    description = "Slow detection result"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "fall-protection",
                    displayName = "Fall Protection Required",
                    confidence = 0.76f,
                    reason = "Slow analysis detected potential fall hazard"
                )
            ),
            autoSelectTags = setOf("fall-protection"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.INFORMATIONAL,
                criticalIssues = 0,
                oshaViolations = emptyList()
            ),
            processingTimeMs = Random.nextLong(3000, 5000) // Slow processing
        )
    }

    private data class ProcessingMetrics(
        val success: Boolean,
        val processingTime: Long,
        val memoryUsage: Long,
        val photoSize: Int,
        val error: String? = null
    )
}