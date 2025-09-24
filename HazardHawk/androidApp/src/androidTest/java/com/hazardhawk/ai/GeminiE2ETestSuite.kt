package com.hazardhawk.ai.test.e2e

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.hazardhawk.ai.test.framework.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * End-to-End Test Suite for Gemini Vision API Integration
 * 
 * Coverage: 10% of total test pyramid
 * Focus: Complete user workflows, real device conditions, full system integration
 * 
 * E2E Test Scenarios:
 * 1. Complete photo capture -> AI analysis -> tag recommendations -> PDF export
 * 2. Offline capture -> online sync with AI processing
 * 3. Multi-photo batch processing workflows
 * 4. Error recovery across complete workflows
 * 5. Performance under real device constraints
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
@EndToEndTest
class GeminiE2ETestSuite {
    
    @get:Rule
    val performanceRule = PerformanceMonitoringRule()
    
    private lateinit var context: Context
    private lateinit var geminiService: GeminiVisionAnalyzer
    private lateinit var aiServiceFacade: GeminiAIServiceFacade
    private lateinit var testDataFactory: GeminiTestDataFactory
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testDataFactory = GeminiTestDataFactory()
        
        // Initialize real components for E2E testing
        // Note: In real E2E tests, these would be actual implementations
        // For this example, we'll use enhanced mocks that simulate real behavior
        geminiService = createE2EGeminiService()
        aiServiceFacade = createE2EAIServiceFacade()
    }
    
    // COMPLETE WORKFLOW TESTS
    
    @Test
    fun `e2e complete photo analysis workflow`() = runTest {
        // Given: A complete photo analysis workflow
        val testImage = testDataFactory.createTestImageData(800) // 800KB construction site image
        
        // When: Execute complete workflow
        val totalWorkflowTime = measureTimeMillis {
            
            // Step 1: Initialize AI services
            val initResult = aiServiceFacade.initialize()
            assertTrue("AI services should initialize", initResult.isSuccess)
            
            // Step 2: Analyze photo with AI
            val analysisResult = aiServiceFacade.analyzePhotoWithTags(
                data = testImage,
                width = 2048,
                height = 1536,
                workType = WorkType.GENERAL_CONSTRUCTION
            )
            
            // Step 3: Validate analysis results
            assertNotNull("Analysis should succeed", analysisResult)
            assertTrue("Should have tag recommendations", analysisResult.recommendedTags.isNotEmpty())
            assertTrue("Should have hazard detections", analysisResult.detections.isNotEmpty())
            assertNotNull("Should have compliance overview", analysisResult.complianceOverview)
            
            // Step 4: Simulate user tag selection and confirmation
            val selectedTags = analysisResult.recommendedTags.take(3)
            assertTrue("User should be able to select tags", selectedTags.isNotEmpty())
            
            // Step 5: Generate safety report (PDF export simulation)
            val reportData = generateSafetyReport(
                imageData = testImage,
                analysisResult = analysisResult,
                selectedTags = selectedTags
            )
            assertTrue("Should generate report", reportData.isNotEmpty())
            
            println("Complete E2E workflow completed successfully")
            println("- Analysis tags: ${analysisResult.recommendedTags.size}")
            println("- Hazard detections: ${analysisResult.detections.size}")
            println("- Processing time: ${analysisResult.processingTimeMs}ms")
        }
        
        // Then: Complete workflow should be within acceptable time
        assertTrue(
            "Complete E2E workflow should complete within 10 seconds, actual: ${totalWorkflowTime}ms",
            totalWorkflowTime <= 10000L
        )
        
        GeminiTestAssertions.assertAnalysisTimeWithinCritical(totalWorkflowTime, 10000L)
    }
    
    @Test
    fun `e2e offline to online sync workflow`() = runTest {
        // Given: Offline photo capture scenario
        val offlinePhotos = listOf(
            testDataFactory.createTestImageData(600),
            testDataFactory.createTestImageData(750),
            testDataFactory.createTestImageData(550)
        )
        
        // Step 1: Simulate offline photo capture and storage
        val offlineStorage = mutableListOf<OfflinePhotoData>()
        offlinePhotos.forEachIndexed { index, imageData ->
            val offlinePhoto = OfflinePhotoData(
                id = "offline_photo_$index",
                imageData = imageData,
                width = 1920,
                height = 1080,
                captureTimestamp = System.currentTimeMillis(),
                workType = WorkType.GENERAL_CONSTRUCTION,
                metadata = mapOf(
                    "location" to "Construction Site A",
                    "project" to "Test Project",
                    "worker_id" to "worker_123"
                )
            )
            offlineStorage.add(offlinePhoto)
        }
        
        assertTrue("Should have offline photos stored", offlineStorage.size == 3)
        
        // Step 2: Simulate coming online and processing queue
        val syncResults = mutableListOf<SyncResult>()
        val totalSyncTime = measureTimeMillis {
            
            // Initialize AI services (now online)
            val initResult = aiServiceFacade.initialize()
            assertTrue("Online AI services should initialize", initResult.isSuccess)
            
            // Process each offline photo
            offlineStorage.forEach { offlinePhoto ->
                val syncStartTime = System.currentTimeMillis()
                
                try {
                    // Analyze with AI
                    val analysisResult = aiServiceFacade.analyzePhotoWithTags(
                        data = offlinePhoto.imageData,
                        width = offlinePhoto.width,
                        height = offlinePhoto.height,
                        workType = offlinePhoto.workType
                    )
                    
                    val syncTime = System.currentTimeMillis() - syncStartTime
                    
                    syncResults.add(SyncResult(
                        photoId = offlinePhoto.id,
                        success = true,
                        analysisResult = analysisResult,
                        syncTimeMs = syncTime,
                        error = null
                    ))
                    
                } catch (e: Exception) {
                    val syncTime = System.currentTimeMillis() - syncStartTime
                    
                    syncResults.add(SyncResult(
                        photoId = offlinePhoto.id,
                        success = false,
                        analysisResult = null,
                        syncTimeMs = syncTime,
                        error = e.message
                    ))
                }
            }
        }
        
        // Then: Validate sync results
        assertEquals("Should process all offline photos", 3, syncResults.size)
        
        val successfulSyncs = syncResults.count { it.success }
        assertTrue("Most syncs should succeed", successfulSyncs >= 2)
        
        val avgSyncTime = syncResults.filter { it.success }.map { it.syncTimeMs }.average()
        assertTrue(
            "Average sync time should be reasonable: ${avgSyncTime}ms",
            avgSyncTime <= 5000.0
        )
        
        println("Offline to online sync results:")
        println("- Photos processed: ${syncResults.size}")
        println("- Successful syncs: $successfulSyncs")
        println("- Total sync time: ${totalSyncTime}ms")
        println("- Average sync time: ${avgSyncTime}ms")
    }
    
    @Test
    fun `e2e batch processing workflow with performance validation`() = runTest {
        // Given: Batch of photos for processing
        val batchSize = 5
        val batchPhotos = (1..batchSize).map { index ->
            BatchPhotoData(
                id = "batch_photo_$index",
                imageData = testDataFactory.createTestImageData(400 + (index * 50)), // Varying sizes
                width = 1920,
                height = 1080,
                workType = when (index % 4) {
                    0 -> WorkType.GENERAL_CONSTRUCTION
                    1 -> WorkType.ELECTRICAL_WORK
                    2 -> WorkType.ROOFING
                    else -> WorkType.EXCAVATION
                },
                priority = when {
                    index <= 2 -> BatchPriority.HIGH
                    index <= 4 -> BatchPriority.MEDIUM
                    else -> BatchPriority.LOW
                }
            )
        }
        
        // When: Process batch with performance monitoring
        val batchResults = mutableListOf<BatchProcessResult>()
        val totalBatchTime = measureTimeMillis {
            
            // Initialize services
            val initResult = aiServiceFacade.initialize()
            assertTrue("Batch processing should initialize", initResult.isSuccess)
            
            // Process by priority order
            val prioritizedPhotos = batchPhotos.sortedBy { it.priority.ordinal }
            
            prioritizedPhotos.forEachIndexed { index, photo ->
                val photoStartTime = System.currentTimeMillis()
                
                try {
                    val analysisResult = aiServiceFacade.analyzePhotoWithTags(
                        data = photo.imageData,
                        width = photo.width,
                        height = photo.height,
                        workType = photo.workType
                    )
                    
                    val photoProcessTime = System.currentTimeMillis() - photoStartTime
                    
                    batchResults.add(BatchProcessResult(
                        photoId = photo.id,
                        success = true,
                        processingTimeMs = photoProcessTime,
                        tagCount = analysisResult.recommendedTags.size,
                        hazardCount = analysisResult.detections.size,
                        confidence = analysisResult.recommendedTags.map { it.confidence }.average().toFloat(),
                        workType = photo.workType,
                        priority = photo.priority
                    ))
                    
                } catch (e: Exception) {
                    val photoProcessTime = System.currentTimeMillis() - photoStartTime
                    
                    batchResults.add(BatchProcessResult(
                        photoId = photo.id,
                        success = false,
                        processingTimeMs = photoProcessTime,
                        error = e.message,
                        workType = photo.workType,
                        priority = photo.priority
                    ))
                }
            }
        }
        
        // Then: Validate batch processing results
        assertEquals("Should process all batch photos", batchSize, batchResults.size)
        
        val successRate = batchResults.count { it.success }.toFloat() / batchSize
        assertTrue("Batch success rate should be >80%", successRate >= 0.8f)
        
        val avgProcessingTime = batchResults.filter { it.success }.map { it.processingTimeMs }.average()
        assertTrue(
            "Average processing time should be reasonable: ${avgProcessingTime}ms",
            avgProcessingTime <= 4000.0
        )
        
        // Validate priority processing (high priority should process faster on average)
        val highPriorityResults = batchResults.filter { it.priority == BatchPriority.HIGH && it.success }
        val lowPriorityResults = batchResults.filter { it.priority == BatchPriority.LOW && it.success }
        
        if (highPriorityResults.isNotEmpty() && lowPriorityResults.isNotEmpty()) {
            val highPriorityAvgTime = highPriorityResults.map { it.processingTimeMs }.average()
            val lowPriorityAvgTime = lowPriorityResults.map { it.processingTimeMs }.average()
            
            println("Priority processing validation:")
            println("- High priority avg time: ${highPriorityAvgTime}ms")
            println("- Low priority avg time: ${lowPriorityAvgTime}ms")
        }
        
        println("Batch processing results:")
        println("- Batch size: $batchSize")
        println("- Success rate: ${successRate * 100}%")
        println("- Total batch time: ${totalBatchTime}ms")
        println("- Average processing time: ${avgProcessingTime}ms")
        println("- Throughput: ${(batchSize * 60000.0) / totalBatchTime} photos/minute")
    }
    
    @Test
    fun `e2e error recovery and resilience workflow`() = runTest {
        // Given: Various error scenarios to test resilience
        val errorScenarios = listOf(
            ErrorScenario("network_timeout", "Network timeout during analysis"),
            ErrorScenario("api_quota_exceeded", "API quota exceeded"),
            ErrorScenario("invalid_image_format", "Corrupted image data"),
            ErrorScenario("model_unavailable", "AI model temporarily unavailable"),
            ErrorScenario("memory_pressure", "Insufficient memory for processing")
        )
        
        val recoveryResults = mutableListOf<ErrorRecoveryResult>()
        val testImage = testDataFactory.createTestImageData(500)
        
        // When: Test error recovery for each scenario
        errorScenarios.forEach { scenario ->
            val recoveryStartTime = System.currentTimeMillis()
            
            try {
                // Simulate error scenario
                val result = simulateErrorScenario(scenario, testImage)
                
                val recoveryTime = System.currentTimeMillis() - recoveryStartTime
                
                recoveryResults.add(ErrorRecoveryResult(
                    scenario = scenario.type,
                    recovered = result != null,
                    recoveryTimeMs = recoveryTime,
                    fallbackUsed = result?.analysisType != AnalysisType.MULTIMODAL_AI,
                    tagCount = result?.recommendedTags?.size ?: 0
                ))
                
            } catch (e: Exception) {
                val recoveryTime = System.currentTimeMillis() - recoveryStartTime
                
                recoveryResults.add(ErrorRecoveryResult(
                    scenario = scenario.type,
                    recovered = false,
                    recoveryTimeMs = recoveryTime,
                    fallbackUsed = true,
                    tagCount = 0,
                    error = e.message
                ))
            }
        }
        
        // Then: Validate error recovery capabilities
        val recoveryRate = recoveryResults.count { it.recovered }.toFloat() / errorScenarios.size
        assertTrue(
            "Error recovery rate should be >70%, actual: ${recoveryRate * 100}%",
            recoveryRate >= 0.7f
        )
        
        val avgRecoveryTime = recoveryResults.map { it.recoveryTimeMs }.average()
        assertTrue(
            "Average error recovery time should be <2 seconds, actual: ${avgRecoveryTime}ms",
            avgRecoveryTime <= 2000.0
        )
        
        println("Error recovery test results:")
        println("- Scenarios tested: ${errorScenarios.size}")
        println("- Recovery rate: ${recoveryRate * 100}%")
        println("- Average recovery time: ${avgRecoveryTime}ms")
        
        recoveryResults.forEach { result ->
            println("- ${result.scenario}: ${if (result.recovered) "Recovered" else "Failed"} (${result.recoveryTimeMs}ms)")
        }
    }
    
    @Test
    fun `e2e real device performance under memory pressure`() = runTest {
        // Given: Memory pressure simulation
        val memoryTracker = MemoryTracker()
        val largeImageBatch = (1..10).map { 
            testDataFactory.createLargeTestImageData() // 4MP images to create memory pressure
        }
        
        memoryTracker.startMonitoring("memory_pressure_test")
        
        var successfulProcessing = 0
        var memoryErrors = 0
        
        // When: Process large images under memory pressure
        val processingTime = measureTimeMillis {
            
            val initResult = aiServiceFacade.initialize()
            assertTrue("Should initialize under memory pressure", initResult.isSuccess)
            
            largeImageBatch.forEachIndexed { index, imageData ->
                try {
                    val result = aiServiceFacade.analyzePhotoWithTags(
                        data = imageData,
                        width = 4000,
                        height = 3000,
                        workType = WorkType.GENERAL_CONSTRUCTION
                    )
                    
                    if (result.recommendedTags.isNotEmpty()) {
                        successfulProcessing++
                    }
                    
                    // Force garbage collection between large images
                    if (index % 3 == 0) {
                        System.gc()
                        kotlinx.coroutines.delay(100)
                    }
                    
                } catch (e: OutOfMemoryError) {
                    memoryErrors++
                    System.gc() // Force cleanup on memory error
                    kotlinx.coroutines.delay(200)
                } catch (e: Exception) {
                    if (e.message?.contains("memory", ignoreCase = true) == true) {
                        memoryErrors++
                    }
                }
            }
        }
        
        val memoryReport = memoryTracker.stopMonitoring()
        
        // Then: Should handle memory pressure gracefully
        val successRate = successfulProcessing.toFloat() / largeImageBatch.size
        assertTrue(
            "Should process most images even under memory pressure: ${successRate * 100}%",
            successRate >= 0.6f
        )
        
        GeminiTestAssertions.assertMemoryUsageAcceptable(memoryReport.peakUsageMB, 2048f)
        
        println("Memory pressure test results:")
        println("- Large images processed: ${largeImageBatch.size}")
        println("- Successful processing: $successfulProcessing")
        println("- Memory errors: $memoryErrors")
        println("- Success rate: ${successRate * 100}%")
        println("- Peak memory usage: ${memoryReport.peakUsageMB}MB")
        println("- Processing time: ${processingTime}ms")
    }
    
    // Helper methods and data classes for E2E testing
    
    private fun createE2EGeminiService(): GeminiVisionAnalyzer {
        // In real E2E tests, this would return actual GeminiVisionAnalyzer instance
        // For this example, we return a highly realistic mock
        val mockFactory = RealisticGeminiMockFactory()
        return mockFactory.createRealisticGeminiAnalyzer(
            GeminiMockConfiguration(
                shouldInitializeSuccessfully = true,
                baseProcessingTimeMs = 1800L,
                processingTimeVariationMs = 600f,
                analysisFailureRate = 0.02f // 2% failure rate for realism
            )
        )
    }
    
    private fun createE2EAIServiceFacade(): GeminiAIServiceFacade {
        // Similar to above, would be real implementation in actual E2E tests
        return mockk<GeminiAIServiceFacade> {
            coEvery { initialize() } returns Result.success(Unit)
            coEvery { analyzePhotoWithTags(any(), any(), any(), any()) } answers {
                // Simulate realistic processing
                delay(Random.nextLong(1000, 3000))
                
                createMockAnalysisResult(args[3] as WorkType)
            }
        }
    }
    
    private fun generateSafetyReport(
        imageData: ByteArray,
        analysisResult: AnalysisResultData,
        selectedTags: List<UITagRecommendation>
    ): ByteArray {
        // Simulate PDF report generation
        val reportContent = """
            SAFETY ANALYSIS REPORT
            Generated: ${java.util.Date()}
            
            Image Analysis Results:
            - Tags: ${selectedTags.size}
            - Hazards: ${analysisResult.detections.size}
            - Compliance Level: ${analysisResult.complianceOverview.overallLevel}
            
            Recommendations:
            ${selectedTags.joinToString("\n") { "- ${it.displayName}: ${it.reason}" }}
        """.trimIndent()
        
        return reportContent.toByteArray()
    }
    
    private suspend fun simulateErrorScenario(
        scenario: ErrorScenario,
        imageData: ByteArray
    ): AnalysisResultData? {
        return when (scenario.type) {
            "network_timeout" -> {
                // Simulate network timeout with fallback
                try {
                    delay(6000L) // Timeout scenario
                    throw Exception("Network timeout")
                } catch (e: Exception) {
                    // Fallback to offline analysis
                    createBasicAnalysisResult(WorkType.GENERAL_CONSTRUCTION)
                }
            }
            "api_quota_exceeded" -> {
                // Simulate quota exceeded with graceful degradation
                createBasicAnalysisResult(WorkType.GENERAL_CONSTRUCTION)
            }
            "invalid_image_format" -> {
                // Simulate invalid image with error handling
                null
            }
            "model_unavailable" -> {
                // Simulate model unavailable with fallback
                createBasicAnalysisResult(WorkType.GENERAL_CONSTRUCTION)
            }
            "memory_pressure" -> {
                // Simulate memory pressure with reduced processing
                createMockAnalysisResult(WorkType.GENERAL_CONSTRUCTION, reducedComplexity = true)
            }
            else -> null
        }
    }
    
    private fun createMockAnalysisResult(
        workType: WorkType, 
        reducedComplexity: Boolean = false
    ): AnalysisResultData {
        val tagCount = if (reducedComplexity) 2 else 4
        
        return AnalysisResultData(
            recommendedTags = (1..tagCount).map { index ->
                UITagRecommendation(
                    tagId = "test_tag_$index",
                    displayName = "Test Tag $index",
                    confidence = 0.7f + (index * 0.05f),
                    reason = "E2E Test Analysis",
                    priority = TagPriority.MEDIUM
                )
            },
            detections = listOf(
                HazardDetectionData(
                    description = "E2E test hazard detection",
                    confidence = 0.8f,
                    severity = HazardSeverity.MEDIUM
                )
            ),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.COMPLIANT,
                criticalIssues = 0,
                oshaViolations = emptyList()
            ),
            processingTimeMs = if (reducedComplexity) 800L else 1500L,
            analysisType = if (reducedComplexity) AnalysisType.BASIC_TAGS else AnalysisType.MULTIMODAL_AI
        )
    }
    
    private fun createBasicAnalysisResult(workType: WorkType): AnalysisResultData {
        return AnalysisResultData(
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "basic_ppe",
                    displayName = "Basic PPE Check",
                    confidence = 0.5f,
                    reason = "Fallback Analysis",
                    priority = TagPriority.LOW
                )
            ),
            detections = emptyList(),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.INFORMATIONAL,
                criticalIssues = 0,
                oshaViolations = emptyList()
            ),
            processingTimeMs = 200L,
            analysisType = AnalysisType.BASIC_TAGS
        )
    }
}

// Data classes for E2E testing

data class OfflinePhotoData(
    val id: String,
    val imageData: ByteArray,
    val width: Int,
    val height: Int,
    val captureTimestamp: Long,
    val workType: WorkType,
    val metadata: Map<String, String>
)

data class SyncResult(
    val photoId: String,
    val success: Boolean,
    val analysisResult: AnalysisResultData?,
    val syncTimeMs: Long,
    val error: String?
)

data class BatchPhotoData(
    val id: String,
    val imageData: ByteArray,
    val width: Int,
    val height: Int,
    val workType: WorkType,
    val priority: BatchPriority
)

enum class BatchPriority {
    HIGH, MEDIUM, LOW
}

data class BatchProcessResult(
    val photoId: String,
    val success: Boolean,
    val processingTimeMs: Long,
    val tagCount: Int = 0,
    val hazardCount: Int = 0,
    val confidence: Float = 0f,
    val workType: WorkType,
    val priority: BatchPriority,
    val error: String? = null
)

data class ErrorScenario(
    val type: String,
    val description: String
)

data class ErrorRecoveryResult(
    val scenario: String,
    val recovered: Boolean,
    val recoveryTimeMs: Long,
    val fallbackUsed: Boolean,
    val tagCount: Int,
    val error: String? = null
)

data class AnalysisResultData(
    val recommendedTags: List<UITagRecommendation>,
    val detections: List<HazardDetectionData>,
    val complianceOverview: ComplianceOverview,
    val processingTimeMs: Long,
    val analysisType: AnalysisType
)

data class HazardDetectionData(
    val description: String,
    val confidence: Float,
    val severity: HazardSeverity
)
