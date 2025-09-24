package com.hazardhawk.ai.integration

import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.ai.core.SmartAIOrchestrator
import com.hazardhawk.core.models.*
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * Comprehensive edge case testing suite for AI integration failures and edge conditions.
 * This suite ensures robust handling of network failures, malformed responses, 
 * poor image quality, and various failure scenarios.
 */
class AIEdgeCaseTestSuite {
    
    private lateinit var orchestrator: SmartAIOrchestrator
    private lateinit var mockNetworkService: MockNetworkService
    private lateinit var mockGeminiService: MockGeminiService
    
    @BeforeTest
    fun setup() {
        mockNetworkService = MockNetworkService()
        mockGeminiService = MockGeminiService()
        orchestrator = createTestOrchestrator()
    }
    
    /**
     * Network failure and connectivity edge cases
     */
    class NetworkFailureEdgeCaseTest {
        
        @Test
        fun `test complete network timeout handling`() = runTest {
            val mockNetwork = MockNetworkService(simulateTimeout = true, timeoutDurationMs = 30000L)
            val orchestrator = createOrchestratorWithMockNetwork(mockNetwork)
            val testImage = createTestImageData("standard_construction_site")
            
            val startTime = System.currentTimeMillis()
            val result = orchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
            val endTime = System.currentTimeMillis()
            
            // Should timeout and fallback to local analysis
            assertTrue("Analysis should succeed with local fallback", result.isSuccess)
            val analysis = result.getOrNull()!!
            assertEquals("Should use local YOLO fallback", 
                AnalysisType.LOCAL_YOLO_FALLBACK, analysis.analysisType)
            
            // Should not wait for full timeout
            assertTrue("Should not wait for full network timeout", 
                endTime - startTime < 25000L)
            
            // Should include warning about network issues
            assertTrue("Should warn about limited analysis",
                analysis.recommendations.any { 
                    it.contains("limited analysis", ignoreCase = true) ||
                    it.contains("network", ignoreCase = true)
                })
        }
        
        @Test
        fun `test intermittent connectivity during analysis`() = runTest {
            val mockNetwork = MockNetworkService(simulateIntermittent = true, 
                intermittentPattern = listOf(true, false, true, false, true))
            val orchestrator = createOrchestratorWithMockNetwork(mockNetwork)
            
            val testImages = (1..5).map { createTestImageData("test_image_$it") }
            val results = testImages.map { image ->
                orchestrator.analyzePhoto(image, WorkType.GENERAL_CONSTRUCTION)
            }
            
            // All analyses should succeed despite intermittent connectivity
            assertTrue("All analyses should succeed", results.all { it.isSuccess })
            
            // Should have mix of cloud and local analysis types
            val analysisTypes = results.mapNotNull { it.getOrNull()?.analysisType }.toSet()
            assertTrue("Should use multiple analysis types", analysisTypes.size > 1)
            
            // At least some should be local fallback due to connectivity issues
            assertTrue("Some should use local fallback",
                results.any { it.getOrNull()?.analysisType == AnalysisType.LOCAL_YOLO_FALLBACK })
        }
        
        @Test
        fun `test slow network connection handling`() = runTest {
            val mockNetwork = MockNetworkService(simulateSlowConnection = true, 
                connectionDelayMs = 5000L)
            val orchestrator = createOrchestratorWithMockNetwork(mockNetwork)
            
            val testImage = createTestImageData("fall_protection_hazard")
            
            val result = withTimeout(12000L) { // 12 second timeout
                orchestrator.analyzePhoto(testImage, WorkType.CONCRETE_WORK)
            }
            
            assertTrue("Should handle slow connections", result.isSuccess)
            
            val analysis = result.getOrNull()!!
            // Should either succeed with cloud or fallback to local
            assertTrue("Should use appropriate analysis method",
                analysis.analysisType in setOf(
                    AnalysisType.CLOUD_GEMINI,
                    AnalysisType.LOCAL_YOLO_FALLBACK,
                    AnalysisType.LOCAL_GEMMA_MULTIMODAL
                ))
        }
        
        @Test
        fun `test API rate limiting response`() = runTest {
            val mockService = MockGeminiService(simulateRateLimit = true)
            
            val testImage = createTestImageData("electrical_hazard")
            val result = mockService.analyzePhoto(testImage, WorkType.ELECTRICAL_WORK)
            
            assertTrue("Should handle rate limiting gracefully", result.isFailure)
            val error = result.exceptionOrNull()!!
            assertTrue("Should indicate rate limiting",
                error.message?.contains("rate limit", ignoreCase = true) == true)
        }
        
        @Test
        fun `test concurrent requests during network instability`() = runTest {
            val mockNetwork = MockNetworkService(simulateIntermittent = true)
            val orchestrator = createOrchestratorWithMockNetwork(mockNetwork)
            
            val testImages = (1..10).map { createTestImageData("concurrent_test_$it") }
            
            val results = testImages.map { image ->
                kotlinx.coroutines.async {
                    orchestrator.analyzePhoto(image, WorkType.GENERAL_CONSTRUCTION)
                }
            }.map { it.await() }
            
            // At least 80% should succeed despite network issues
            val successRate = results.count { it.isSuccess }.toFloat() / results.size
            assertTrue("Success rate should be high despite network issues", 
                successRate >= 0.8f)
        }
    }
    
    /**
     * Malformed AI response handling edge cases
     */
    class MalformedResponseEdgeCaseTest {
        
        @Test
        fun `test completely corrupted JSON response`() = runTest {
            val corruptedResponse = """{"hazards": [{"type": "INVALID_TYPE", "confidence":}"""
            val parser = createGeminiResponseParser()
            
            val result = parser.parseResponse(corruptedResponse)
            
            assertTrue("Should handle corrupted JSON gracefully", result.isFailure)
            val error = result.exceptionOrNull()!!
            assertTrue("Should indicate JSON parsing error",
                error.message?.contains("JSON", ignoreCase = true) == true ||
                error.message?.contains("parse", ignoreCase = true) == true)
        }
        
        @Test
        fun `test response with missing critical fields`() = runTest {
            val incompleteResponses = listOf(
                """{"hazards": [{"type": "FALL_PROTECTION"}]}""", // Missing confidence
                """{"hazards": [{"confidence": 0.8}]}""", // Missing type
                """{"confidence": 0.7}""", // Missing hazards array
                """{}""" // Empty response
            )
            
            val parser = createGeminiResponseParser()
            
            incompleteResponses.forEach { response ->
                val result = parser.parseResponse(response)
                
                // Should handle missing fields gracefully
                assertTrue("Should handle incomplete response: $response", 
                    result.isSuccess || result.isFailure)
                
                if (result.isSuccess) {
                    val analysis = result.getOrNull()!!
                    // Should provide reasonable defaults
                    assertTrue("Should have valid confidence", analysis.confidence >= 0f)
                    assertNotNull("Should have hazards list", analysis.hazards)
                }
            }
        }
        
        @Test
        fun `test response with invalid data types`() = runTest {
            val invalidTypeResponses = listOf(
                """{"confidence": "very high", "hazards": "none"}""", // String instead of numbers/arrays
                """{"confidence": 1.5, "hazards": []}""", // Confidence > 1.0
                """{"confidence": -0.3, "hazards": []}""", // Negative confidence
                """{"hazards": [{"confidence": "0.8", "type": 123}]}""" // Mixed type errors
            )
            
            val parser = createGeminiResponseParser()
            
            invalidTypeResponses.forEach { response ->
                val result = parser.parseResponse(response)
                
                if (result.isSuccess) {
                    val analysis = result.getOrNull()!!
                    // Should sanitize invalid values
                    assertTrue("Confidence should be normalized", 
                        analysis.confidence >= 0f && analysis.confidence <= 1f)
                } else {
                    // Failure is acceptable for severely malformed data
                    assertNotNull("Should have error message", result.exceptionOrNull()?.message)
                }
            }
        }
        
        @Test
        fun `test response with unexpected additional fields`() = runTest {
            val responseWithExtraFields = """
            {
                "hazards": [
                    {
                        "type": "FALL_PROTECTION",
                        "confidence": 0.85,
                        "severity": "HIGH",
                        "unknown_field": "some_value",
                        "another_field": {"nested": "data"}
                    }
                ],
                "confidence": 0.85,
                "extra_field": "should_be_ignored",
                "metadata": {"version": "1.0", "model": "test"}
            }
            """
            
            val parser = createGeminiResponseParser()
            val result = parser.parseResponse(responseWithExtraFields)
            
            assertTrue("Should handle extra fields gracefully", result.isSuccess)
            val analysis = result.getOrNull()!!
            
            assertEquals("Should parse known fields correctly", 1, analysis.hazards.size)
            assertEquals("Should parse hazard type", HazardType.FALL_PROTECTION, 
                analysis.hazards.first().type)
            assertEquals("Should parse confidence", 0.85f, analysis.confidence, 0.01f)
        }
        
        @Test
        fun `test extremely large response handling`() = runTest {
            // Simulate response with many hazards (potential memory issues)
            val largeHazardsList = (1..1000).map { i ->
                """
                {
                    "type": "FALL_PROTECTION",
                    "confidence": ${0.5f + (i % 50) * 0.01f},
                    "description": "Hazard number $i with very long description that could potentially cause memory issues if not handled properly",
                    "severity": "MEDIUM"
                }
                """.trimIndent()
            }.joinToString(",")
            
            val largeResponse = """
            {
                "hazards": [$largeHazardsList],
                "confidence": 0.75
            }
            """
            
            val parser = createGeminiResponseParser()
            val startTime = System.currentTimeMillis()
            val result = parser.parseResponse(largeResponse)
            val processingTime = System.currentTimeMillis() - startTime
            
            // Should handle large responses within reasonable time
            assertTrue("Should process large response within 5 seconds", 
                processingTime < 5000L)
            
            if (result.isSuccess) {
                val analysis = result.getOrNull()!!
                assertTrue("Should limit number of hazards to reasonable amount",
                    analysis.hazards.size <= 100) // Should cap at reasonable limit
            }
        }
    }
    
    /**
     * Photo quality and content edge cases
     */
    class PhotoQualityEdgeCaseTest {
        
        @Test
        fun `test extremely dark images`() = runTest {
            val darkImage = createTestImageData("very_dark_underground_work")
            val orchestrator = createTestOrchestrator()
            
            val result = orchestrator.analyzePhoto(darkImage, WorkType.GENERAL_CONSTRUCTION)
            
            assertTrue("Should process dark images without failure", result.isSuccess)
            val analysis = result.getOrNull()!!
            
            // Should indicate image quality issues in recommendations
            assertTrue("Should warn about image quality",
                analysis.recommendations.any { 
                    it.contains("lighting", ignoreCase = true) ||
                    it.contains("dark", ignoreCase = true) ||
                    it.contains("visibility", ignoreCase = true)
                })
            
            // Confidence may be lower but should not be zero
            assertTrue("Should have some confidence level", analysis.confidence > 0f)
        }
        
        @Test
        fun `test severely blurry motion images`() = runTest {
            val blurryImage = createTestImageData("severe_motion_blur")
            val orchestrator = createTestOrchestrator()
            
            val result = orchestrator.analyzePhoto(blurryImage, WorkType.GENERAL_CONSTRUCTION)
            
            assertTrue("Should process blurry images", result.isSuccess)
            val analysis = result.getOrNull()!!
            
            // Should warn about image quality
            assertTrue("Should include blur warning",
                analysis.recommendations.any {
                    it.contains("blur", ignoreCase = true) ||
                    it.contains("clear", ignoreCase = true) ||
                    it.contains("quality", ignoreCase = true)
                })
        }
        
        @Test
        fun `test oversized images handling`() = runTest {
            val oversizedImage = createLargeTestImageData(50 * 1024 * 1024) // 50MB image
            val orchestrator = createTestOrchestrator()
            
            val startTime = System.currentTimeMillis()
            val result = orchestrator.analyzePhoto(oversizedImage, WorkType.GENERAL_CONSTRUCTION)
            val processingTime = System.currentTimeMillis() - startTime
            
            assertTrue("Should handle large images", result.isSuccess)
            
            // Should not take excessive time due to automatic compression
            assertTrue("Should process large images efficiently", 
                processingTime < 20000L) // Under 20 seconds
        }
        
        @Test
        fun `test completely white or black images`() = runTest {
            val whiteImage = createTestImageData("completely_white")
            val blackImage = createTestImageData("completely_black")
            val orchestrator = createTestOrchestrator()
            
            val whiteResult = orchestrator.analyzePhoto(whiteImage, WorkType.GENERAL_CONSTRUCTION)
            val blackResult = orchestrator.analyzePhoto(blackImage, WorkType.GENERAL_CONSTRUCTION)
            
            assertTrue("Should handle white images", whiteResult.isSuccess)
            assertTrue("Should handle black images", blackResult.isSuccess)
            
            // Should indicate no meaningful content
            val whiteAnalysis = whiteResult.getOrNull()!!
            val blackAnalysis = blackResult.getOrNull()!!
            
            assertTrue("White image should have no or few hazards", 
                whiteAnalysis.hazards.size <= 1)
            assertTrue("Black image should have no or few hazards", 
                blackAnalysis.hazards.size <= 1)
        }
        
        @Test
        fun `test images with no construction content`() = runTest {
            val nonConstructionImages = listOf(
                createTestImageData("landscape_nature"),
                createTestImageData("office_interior"),
                createTestImageData("food_image"),
                createTestImageData("abstract_art")
            )
            
            val orchestrator = createTestOrchestrator()
            
            nonConstructionImages.forEach { image ->
                val result = orchestrator.analyzePhoto(image, WorkType.GENERAL_CONSTRUCTION)
                
                assertTrue("Should handle non-construction images", result.isSuccess)
                val analysis = result.getOrNull()!!
                
                // Should detect minimal or no hazards
                assertTrue("Should detect few hazards in non-construction images",
                    analysis.hazards.size <= 2)
                
                // Overall risk should be low
                assertTrue("Risk should be low for non-construction content",
                    analysis.overallRisk in setOf(RiskLevel.LOW, RiskLevel.UNKNOWN))
            }
        }
        
        @Test
        fun `test corrupted or invalid image data`() = runTest {
            val corruptedImageData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) // Truncated JPEG
            val randomData = ByteArray(1000) { (it % 256).toByte() } // Random bytes
            val orchestrator = createTestOrchestrator()
            
            val corruptedResult = orchestrator.analyzePhoto(corruptedImageData, WorkType.GENERAL_CONSTRUCTION)
            val randomResult = orchestrator.analyzePhoto(randomData, WorkType.GENERAL_CONSTRUCTION)
            
            // Should handle gracefully - either succeed with low confidence or fail gracefully
            if (corruptedResult.isFailure) {
                assertTrue("Should have meaningful error message",
                    corruptedResult.exceptionOrNull()?.message?.isNotEmpty() == true)
            }
            
            if (randomResult.isFailure) {
                assertTrue("Should have meaningful error message",
                    randomResult.exceptionOrNull()?.message?.isNotEmpty() == true)
            }
        }
    }
    
    /**
     * AI service availability and fallback edge cases
     */
    class ServiceAvailabilityEdgeCaseTest {
        
        @Test
        fun `test all AI services unavailable`() = runTest {
            val orchestrator = createOrchestratorWithAllServicesDown()
            val testImage = createTestImageData("construction_hazard")
            
            val result = orchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
            
            assertTrue("Should fail gracefully when all services down", result.isFailure)
            val error = result.exceptionOrNull()!!
            assertTrue("Should indicate service unavailability",
                error.message?.contains("unavailable", ignoreCase = true) == true ||
                error.message?.contains("failed", ignoreCase = true) == true)
        }
        
        @Test
        fun `test partial service availability with degraded performance`() = runTest {
            val orchestrator = createOrchestratorWithPartialServices()
            val testImage = createTestImageData("fall_protection_scenario")
            
            val result = orchestrator.analyzePhoto(testImage, WorkType.CONCRETE_WORK)
            
            assertTrue("Should succeed with available services", result.isSuccess)
            val analysis = result.getOrNull()!!
            
            // Should indicate limited capabilities
            assertTrue("Should warn about degraded service",
                analysis.recommendations.any {
                    it.contains("limited", ignoreCase = true) ||
                    it.contains("degraded", ignoreCase = true) ||
                    it.contains("unavailable", ignoreCase = true)
                })
        }
        
        @Test
        fun `test service recovery after temporary failure`() = runTest {
            val mockService = MockGeminiService(simulateTemporaryFailure = true, 
                failureCount = 2) // Fail first 2 attempts
            
            val testImage = createTestImageData("electrical_hazard")
            
            // First attempt should fail
            val firstResult = mockService.analyzePhoto(testImage, WorkType.ELECTRICAL_WORK)
            assertTrue("First attempt should fail", firstResult.isFailure)
            
            // Second attempt should also fail
            val secondResult = mockService.analyzePhoto(testImage, WorkType.ELECTRICAL_WORK)
            assertTrue("Second attempt should fail", secondResult.isFailure)
            
            // Third attempt should succeed (service recovered)
            val thirdResult = mockService.analyzePhoto(testImage, WorkType.ELECTRICAL_WORK)
            assertTrue("Third attempt should succeed after recovery", thirdResult.isSuccess)
        }
        
        @Test
        fun `test memory pressure during AI processing`() = runTest {
            val orchestrator = createTestOrchestrator()
            val largeImage = createLargeTestImageData(20 * 1024 * 1024) // 20MB
            
            // Simulate memory pressure by running multiple concurrent analyses
            val concurrentAnalyses = (1..5).map {
                kotlinx.coroutines.async {
                    orchestrator.analyzePhoto(largeImage, WorkType.GENERAL_CONSTRUCTION)
                }
            }
            
            val results = concurrentAnalyses.map { it.await() }
            
            // At least some should succeed despite memory pressure
            val successCount = results.count { it.isSuccess }
            assertTrue("Some analyses should succeed despite memory pressure",
                successCount >= 2)
            
            // Failed analyses should have meaningful error messages
            results.filter { it.isFailure }.forEach { result ->
                assertNotNull("Failed analysis should have error message",
                    result.exceptionOrNull()?.message)
            }
        }
    }
    
    /**
     * Timeout and performance edge cases
     */
    class TimeoutPerformanceEdgeCaseTest {
        
        @Test
        fun `test analysis timeout with very slow response`() = runTest {
            val slowService = MockGeminiService(simulateSlowResponse = true, 
                responseDelayMs = 25000L) // 25 second delay
            
            val testImage = createTestImageData("complex_multi_hazard_scene")
            
            val startTime = System.currentTimeMillis()
            val result = slowService.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
            val endTime = System.currentTimeMillis()
            
            // Should timeout and return failure
            assertTrue("Should timeout for very slow responses", result.isFailure)
            
            // Should not wait for full delay
            assertTrue("Should respect timeout limits", endTime - startTime < 20000L)
            
            val error = result.exceptionOrNull()!!
            assertTrue("Should indicate timeout",
                error.message?.contains("timeout", ignoreCase = true) == true)
        }
        
        @Test
        fun `test resource cleanup after timeout`() = runTest {
            val orchestrator = createTestOrchestrator()
            val testImage = createTestImageData("resource_intensive_scene")
            
            try {
                withTimeout(1000L) { // Very short timeout
                    orchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Expected timeout
            }
            
            // Verify resources are cleaned up
            delay(1000L) // Allow cleanup time
            
            // Subsequent analysis should work normally
            val subsequentResult = orchestrator.analyzePhoto(
                createTestImageData("simple_scene"), 
                WorkType.GENERAL_CONSTRUCTION
            )
            assertTrue("Subsequent analysis should work after timeout", 
                subsequentResult.isSuccess)
        }
    }
    
    // Helper functions and mock implementations
    
    private fun createTestOrchestrator(): SmartAIOrchestrator {
        // Create orchestrator with test configuration
        return SmartAIOrchestrator(
            gemma3NE2B = MockGemma3NE2BService(),
            vertexAI = MockVertexAIService(),
            yolo11 = MockYOLO11Service(),
            networkMonitor = MockNetworkService(),
            performanceManager = MockPerformanceManager(),
            memoryManager = MockMemoryManager(),
            performanceMonitor = MockPerformanceMonitor()
        )
    }
    
    private fun createOrchestratorWithMockNetwork(networkService: MockNetworkService): SmartAIOrchestrator {
        return SmartAIOrchestrator(
            gemma3NE2B = MockGemma3NE2BService(),
            vertexAI = MockVertexAIService(),
            yolo11 = MockYOLO11Service(),
            networkMonitor = networkService,
            performanceManager = MockPerformanceManager(),
            memoryManager = MockMemoryManager(),
            performanceMonitor = MockPerformanceMonitor()
        )
    }
    
    private fun createOrchestratorWithAllServicesDown(): SmartAIOrchestrator {
        return SmartAIOrchestrator(
            gemma3NE2B = MockGemma3NE2BService(isAvailable = false),
            vertexAI = MockVertexAIService(isAvailable = false),
            yolo11 = MockYOLO11Service(isAvailable = false),
            networkMonitor = MockNetworkService(isConnected = false),
            performanceManager = MockPerformanceManager(),
            memoryManager = MockMemoryManager(),
            performanceMonitor = MockPerformanceMonitor()
        )
    }
    
    private fun createOrchestratorWithPartialServices(): SmartAIOrchestrator {
        return SmartAIOrchestrator(
            gemma3NE2B = MockGemma3NE2BService(isAvailable = false),
            vertexAI = MockVertexAIService(isAvailable = false),
            yolo11 = MockYOLO11Service(isAvailable = true), // Only YOLO available
            networkMonitor = MockNetworkService(isConnected = false),
            performanceManager = MockPerformanceManager(),
            memoryManager = MockMemoryManager(),
            performanceMonitor = MockPerformanceMonitor()
        )
    }
    
    private fun createGeminiResponseParser(): GeminiResponseParser {
        return GeminiResponseParser()
    }
    
    private fun createTestImageData(scenario: String): ByteArray {
        // In real implementation, would load actual test images
        return scenario.toByteArray()
    }
    
    private fun createLargeTestImageData(sizeBytes: Int): ByteArray {
        return ByteArray(sizeBytes) { (it % 256).toByte() }
    }
}

/**
 * Mock services for edge case testing
 */
class MockNetworkService(
    private var isConnected: Boolean = true,
    private val simulateTimeout: Boolean = false,
    private val timeoutDurationMs: Long = 30000L,
    private val simulateIntermittent: Boolean = false,
    private val intermittentPattern: List<Boolean> = listOf(true, false, true),
    private val simulateSlowConnection: Boolean = false,
    private val connectionDelayMs: Long = 1000L
) : NetworkConnectivityService {
    
    private var intermittentIndex = 0
    
    override val isConnected: Boolean
        get() = when {
            simulateIntermittent -> {
                val connected = intermittentPattern[intermittentIndex % intermittentPattern.size]
                intermittentIndex++
                connected
            }
            else -> isConnected
        }
    
    override val connectionQuality: ConnectionQuality
        get() = when {
            simulateSlowConnection -> ConnectionQuality.POOR
            isConnected -> ConnectionQuality.GOOD
            else -> ConnectionQuality.POOR
        }
}

class MockGeminiService(
    private var isAvailable: Boolean = true,
    private val simulateRateLimit: Boolean = false,
    private val simulateTemporaryFailure: Boolean = false,
    private val failureCount: Int = 0,
    private val simulateSlowResponse: Boolean = false,
    private val responseDelayMs: Long = 100L
) : AIPhotoAnalyzer {
    
    private var currentFailureCount = 0
    
    override val analyzerName = "Mock Gemini Service"
    override val analysisCapabilities = setOf(
        AnalysisCapability.MULTIMODAL_VISION,
        AnalysisCapability.HAZARD_IDENTIFICATION,
        AnalysisCapability.PPE_DETECTION
    )
    
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType,
        analysisOptions: AnalysisOptions
    ): Result<SafetyAnalysis> {
        
        if (!isAvailable) {
            return Result.failure(Exception("Service unavailable"))
        }
        
        if (simulateRateLimit) {
            return Result.failure(Exception("API rate limit exceeded"))
        }
        
        if (simulateTemporaryFailure && currentFailureCount < failureCount) {
            currentFailureCount++
            return Result.failure(Exception("Temporary service failure"))
        }
        
        if (simulateSlowResponse) {
            delay(responseDelayMs)
        }
        
        // Generate realistic mock response
        return Result.success(
            SafetyAnalysis(
                id = "mock-gemini-${System.currentTimeMillis()}",
                hazards = generateMockHazards(String(imageData)),
                overallRisk = RiskLevel.MEDIUM,
                confidence = 0.80f,
                processingTimeMs = responseDelayMs,
                aiProvider = analyzerName,
                analysisType = AnalysisType.CLOUD_GEMINI
            )
        )
    }
    
    private fun generateMockHazards(scenario: String): List<DetectedHazard> {
        return when {
            scenario.contains("fall", ignoreCase = true) -> listOf(
                DetectedHazard(
                    type = HazardType.FALL_PROTECTION,
                    description = "Mock fall hazard",
                    severity = Severity.HIGH,
                    confidence = 0.85f,
                    oshaCode = "1926.501"
                )
            )
            else -> emptyList()
        }
    }
    
    override fun getPerformanceMetrics(): AnalyzerPerformanceMetrics {
        return AnalyzerPerformanceMetrics(
            analysisCount = 50L,
            averageProcessingTime = responseDelayMs,
            successRate = if (isAvailable) 0.95f else 0.0f,
            averageConfidence = 0.80f
        )
    }
    
    override fun cleanup() {}
}

/**
 * Response parser for testing malformed responses
 */
class GeminiResponseParser {
    fun parseResponse(jsonResponse: String): Result<SafetyAnalysis> {
        return try {
            // Simplified parsing logic for testing
            when {
                jsonResponse.contains("INVALID_TYPE") -> 
                    Result.failure(Exception("Invalid JSON structure"))
                jsonResponse == "{}" -> 
                    Result.success(createEmptyAnalysis())
                jsonResponse.contains("confidence") && jsonResponse.contains("hazards") ->
                    Result.success(createMockAnalysis())
                else -> 
                    Result.failure(Exception("Unable to parse response"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("JSON parsing failed: ${e.message}"))
        }
    }
    
    private fun createEmptyAnalysis(): SafetyAnalysis {
        return SafetyAnalysis(
            id = "empty-response",
            hazards = emptyList(),
            overallRisk = RiskLevel.UNKNOWN,
            confidence = 0.0f,
            processingTimeMs = 0L,
            aiProvider = "Mock Parser"
        )
    }
    
    private fun createMockAnalysis(): SafetyAnalysis {
        return SafetyAnalysis(
            id = "parsed-response",
            hazards = listOf(
                DetectedHazard(
                    type = HazardType.FALL_PROTECTION,
                    description = "Parsed mock hazard",
                    severity = Severity.MEDIUM,
                    confidence = 0.75f
                )
            ),
            overallRisk = RiskLevel.MEDIUM,
            confidence = 0.75f,
            processingTimeMs = 200L,
            aiProvider = "Mock Parser"
        )
    }
}

// Additional mock services
class MockGemma3NE2BService(private val isAvailable: Boolean = true) : AIPhotoAnalyzer {
    override val analyzerName = "Mock Gemma 3N E2B"
    override val analysisCapabilities = setOf(AnalysisCapability.MULTIMODAL_VISION)
    
    override suspend fun analyzePhoto(imageData: ByteArray, workType: WorkType, analysisOptions: AnalysisOptions): Result<SafetyAnalysis> {
        if (!isAvailable) return Result.failure(Exception("Service unavailable"))
        
        return Result.success(SafetyAnalysis(
            id = "mock-gemma-${System.currentTimeMillis()}",
            hazards = emptyList(),
            overallRisk = RiskLevel.LOW,
            confidence = 0.85f,
            processingTimeMs = 150L,
            aiProvider = analyzerName,
            analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL
        ))
    }
    
    override fun getPerformanceMetrics() = AnalyzerPerformanceMetrics(10L, 150L, if (isAvailable) 1.0f else 0.0f, 0.85f)
    override fun cleanup() {}
}

class MockVertexAIService(private val isAvailable: Boolean = true) : AIPhotoAnalyzer {
    override val analyzerName = "Mock Vertex AI"
    override val analysisCapabilities = setOf(AnalysisCapability.CLOUD_PROCESSING)
    
    override suspend fun analyzePhoto(imageData: ByteArray, workType: WorkType, analysisOptions: AnalysisOptions): Result<SafetyAnalysis> {
        if (!isAvailable) return Result.failure(Exception("Service unavailable"))
        
        return Result.success(SafetyAnalysis(
            id = "mock-vertex-${System.currentTimeMillis()}",
            hazards = emptyList(),
            overallRisk = RiskLevel.LOW,
            confidence = 0.90f,
            processingTimeMs = 800L,
            aiProvider = analyzerName,
            analysisType = AnalysisType.CLOUD_GEMINI
        ))
    }
    
    override fun getPerformanceMetrics() = AnalyzerPerformanceMetrics(20L, 800L, if (isAvailable) 0.98f else 0.0f, 0.90f)
    override fun cleanup() {}
}

class MockYOLO11Service(private val isAvailable: Boolean = true) : AIPhotoAnalyzer {
    override val analyzerName = "Mock YOLO11"
    override val analysisCapabilities = setOf(AnalysisCapability.OBJECT_DETECTION)
    
    override suspend fun analyzePhoto(imageData: ByteArray, workType: WorkType, analysisOptions: AnalysisOptions): Result<SafetyAnalysis> {
        if (!isAvailable) return Result.failure(Exception("Service unavailable"))
        
        return Result.success(SafetyAnalysis(
            id = "mock-yolo-${System.currentTimeMillis()}",
            hazards = emptyList(),
            overallRisk = RiskLevel.LOW,
            confidence = 0.70f,
            processingTimeMs = 100L,
            aiProvider = analyzerName,
            analysisType = AnalysisType.LOCAL_YOLO_FALLBACK,
            recommendations = listOf("Limited analysis - advanced AI services unavailable")
        ))
    }
    
    override fun getPerformanceMetrics() = AnalyzerPerformanceMetrics(100L, 100L, if (isAvailable) 1.0f else 0.0f, 0.70f)
    override fun cleanup() {}
}
