# HazardHawk Comprehensive AI Integration Testing Strategy

## Executive Summary

This document outlines a comprehensive testing strategy for real AI integration in HazardHawk, ensuring safety-critical AI functionality meets construction industry requirements. The strategy covers Google Gemini Vision API integration, local LiteRT models, and safety-critical validation with OSHA compliance requirements.

## 1. AI Integration Testing Framework

### 1.1 Test Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     AI Testing Framework                    │
├─────────────────────────────────────────────────────────────┤
│  Unit Tests          │  Integration Tests  │  E2E Tests     │
│  - AI Services       │  - Orchestrator     │  - User Flows  │
│  - Response Parsing  │  - API Integration  │  - Performance │
│  - Error Handling    │  - Fallback Logic   │  - Safety      │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Test Data Management

**Test Image Repository Structure:**
```
test-data/
├── construction-hazards/
│   ├── fall-protection/
│   │   ├── high-risk/ (known critical hazards)
│   │   ├── medium-risk/ (moderate hazards)
│   │   └── compliant/ (safe scenarios)
│   ├── electrical/
│   ├── struck-by/
│   ├── caught-in-between/
│   └── ppe-violations/
├── edge-cases/
│   ├── poor-lighting/
│   ├── blurry-images/
│   ├── partial-obstructions/
│   └── no-hazards/
└── performance/
    ├── high-resolution/
    ├── multiple-hazards/
    └── batch-processing/
```

### 1.3 Testing Frameworks and Tools

- **Unit Testing**: Kotlin Test Framework
- **Integration Testing**: Ktor Test Engine (for API calls)
- **Performance Testing**: Kotlinx.benchmark
- **Mock Services**: Custom MockAIComponents
- **CI/CD**: GitHub Actions with test automation
- **Test Data**: Structured JSON with expected outcomes

## 2. AI Integration Testing Scenarios

### 2.1 Google Gemini Vision API Integration Tests

#### Test Suite: GeminiVisionIntegrationTest

```kotlin
class GeminiVisionIntegrationTest {
    
    @Test
    suspend fun `test successful API authentication`() {
        // Verify API key validation and authentication
    }
    
    @Test 
    suspend fun `test image upload and analysis request`() {
        // Test complete API request/response cycle
    }
    
    @Test
    suspend fun `test response parsing for valid hazard detection`() {
        // Verify JSON response parsing to SafetyAnalysis objects
    }
    
    @Test
    suspend fun `test API rate limiting and retry logic`() {
        // Test rate limiting handling and exponential backoff
    }
    
    @Test
    suspend fun `test large image handling and compression`() {
        // Test image size limits and automatic compression
    }
}
```

#### Mock Data for Gemini API Testing

```kotlin
object GeminiTestData {
    val validAPIResponse = """
    {
        "hazards": [
            {
                "type": "FALL_PROTECTION",
                "description": "Worker near unguarded edge",
                "confidence": 0.89,
                "severity": "HIGH",
                "oshaCode": "1926.501",
                "boundingBox": {"x": 0.2, "y": 0.3, "width": 0.4, "height": 0.5}
            }
        ],
        "ppe_status": {
            "hard_hat": {"present": true, "confidence": 0.95},
            "safety_vest": {"present": false, "confidence": 0.88}
        },
        "osha_violations": ["1926.501 - Fall Protection"],
        "overall_risk": "HIGH",
        "recommendations": ["Install guardrails", "Use safety harness"]
    }
    """
    
    val malformedResponse = """{"invalid": "json structure"}"""
    val emptyResponse = """{}"""
    val timeoutResponse = null // Simulates timeout
}
```

### 2.2 AI Response Parsing and Error Handling Tests

#### Test Suite: AIResponseParsingTest

```kotlin
class AIResponseParsingTest {
    
    @Test
    fun `test valid Gemini response parsing`() {
        // Test parsing valid JSON to SafetyAnalysis
        val result = GeminiResponseParser.parse(GeminiTestData.validAPIResponse)
        assertTrue(result.isSuccess)
        assertEquals(HazardType.FALL_PROTECTION, result.getOrNull()?.hazards?.first()?.type)
    }
    
    @Test
    fun `test malformed JSON handling`() {
        // Test graceful handling of invalid JSON
        val result = GeminiResponseParser.parse(GeminiTestData.malformedResponse)
        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message ?: "", "Invalid JSON")
    }
    
    @Test
    fun `test empty response fallback`() {
        // Test handling when AI returns no hazards
        val result = GeminiResponseParser.parse(GeminiTestData.emptyResponse)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.hazards?.isEmpty() == true)
    }
    
    @Test
    fun `test confidence threshold enforcement`() {
        // Test filtering low-confidence detections
        val lowConfidenceData = // ... test data with confidence < 0.5
        val result = GeminiResponseParser.parse(lowConfidenceData, minConfidence = 0.7f)
        assertTrue(result.getOrNull()?.hazards?.all { it.confidence >= 0.7f } == true)
    }
}
```

### 2.3 Photo Upload and Analysis Flow Tests

#### Test Suite: PhotoAnalysisFlowTest

```kotlin
class PhotoAnalysisFlowTest {
    
    @Test
    suspend fun `test complete photo analysis workflow`() {
        val testImage = loadTestImage("fall-protection/high-risk/worker_near_edge.jpg")
        val orchestrator = createTestOrchestrator()
        
        val result = orchestrator.analyzePhoto(testImage, WorkType.CONCRETE_WORK)
        
        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!
        assertNotNull(analysis.hazards.find { it.type == HazardType.FALL_PROTECTION })
        assertTrue(analysis.confidence > 0.5f)
        assertNotNull(analysis.metadata?.processingTimeMs)
    }
    
    @Test
    suspend fun `test batch photo processing`() {
        val testImages = listOf(
            loadTestImage("electrical/exposed_wiring.jpg") to WorkType.ELECTRICAL_WORK,
            loadTestImage("ppe/missing_hardhat.jpg") to WorkType.GENERAL_CONSTRUCTION
        )
        
        val results = orchestrator.analyzeBatch(testImages)
        assertEquals(2, results.size)
        assertTrue(results.all { it.isSuccess })
    }
}
```

## 3. AI Accuracy Testing Framework

### 3.1 Hazard Detection Accuracy Validation

#### Test Suite: HazardDetectionAccuracyTest

```kotlin
class HazardDetectionAccuracyTest {
    
    private val groundTruthData = loadGroundTruthDataset()
    private val testImages = loadTestImageDataset()
    
    @Test
    suspend fun `test fall protection hazard detection accuracy`() {
        val results = testImages.filter { it.category == "fall-protection" }
            .map { testCase ->
                val analysis = aiOrchestrator.analyzePhoto(testCase.imageData, testCase.workType)
                AccuracyResult(
                    imageId = testCase.id,
                    expectedHazards = testCase.expectedHazards,
                    detectedHazards = analysis.getOrNull()?.hazards ?: emptyList(),
                    confidence = analysis.getOrNull()?.confidence ?: 0f
                )
            }
        
        val accuracy = calculateAccuracy(results)
        assertTrue("Fall protection detection accuracy below 85%", accuracy >= 0.85f)
    }
    
    @Test
    suspend fun `test PPE detection accuracy`() {
        val ppeTestCases = testImages.filter { it.category == "ppe-detection" }
        val results = ppeTestCases.map { testCase ->
            val analysis = aiOrchestrator.analyzePhoto(testCase.imageData, testCase.workType)
            PPEAccuracyResult(
                imageId = testCase.id,
                expectedPPE = testCase.expectedPPE,
                detectedPPE = analysis.getOrNull()?.ppeStatus ?: emptyMap()
            )
        }
        
        val accuracy = calculatePPEAccuracy(results)
        assertTrue("PPE detection accuracy below 80%", accuracy >= 0.80f)
    }
    
    private fun calculateAccuracy(results: List<AccuracyResult>): Float {
        val totalPredictions = results.size
        val correctPredictions = results.count { result ->
            result.expectedHazards.all { expected ->
                result.detectedHazards.any { detected ->
                    detected.type == expected.type && 
                    detected.confidence >= 0.5f &&
                    isBoundingBoxOverlap(detected.boundingBox, expected.boundingBox)
                }
            }
        }
        return correctPredictions.toFloat() / totalPredictions
    }
}
```

### 3.2 Performance Benchmark Tests

#### Test Suite: AIPerformanceBenchmarkTest

```kotlin
class AIPerformanceBenchmarkTest {
    
    @Test
    suspend fun `test response time under normal conditions`() {
        val testImage = loadTestImage("standard/construction_site.jpg")
        val startTime = System.currentTimeMillis()
        
        val result = aiOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
        
        val processingTime = System.currentTimeMillis() - startTime
        assertTrue("Analysis took longer than 10 seconds", processingTime < 10000)
        assertTrue(result.isSuccess)
    }
    
    @Test
    suspend fun `test performance with high resolution images`() {
        val highResImage = loadTestImage("performance/high_resolution_8k.jpg")
        val startTime = System.currentTimeMillis()
        
        val result = aiOrchestrator.analyzePhoto(highResImage, WorkType.GENERAL_CONSTRUCTION)
        
        val processingTime = System.currentTimeMillis() - startTime
        assertTrue("High-res analysis took longer than 15 seconds", processingTime < 15000)
    }
    
    @Test
    suspend fun `test concurrent analysis performance`() {
        val testImages = (1..5).map { loadTestImage("performance/concurrent_test_$it.jpg") }
        val startTime = System.currentTimeMillis()
        
        val results = testImages.map { image ->
            async { aiOrchestrator.analyzePhoto(image, WorkType.GENERAL_CONSTRUCTION) }
        }.awaitAll()
        
        val totalTime = System.currentTimeMillis() - startTime
        assertTrue("Concurrent analysis took too long", totalTime < 20000)
        assertTrue("Some analyses failed", results.all { it.isSuccess })
    }
}
```

### 3.3 AI Confidence Threshold Testing

```kotlin
class ConfidenceThresholdTest {
    
    @Test
    suspend fun `test confidence threshold filtering`() {
        val testImage = loadTestImage("edge-cases/low_confidence_hazard.jpg")
        
        // Test with low threshold (0.3)
        val lowThresholdResult = aiOrchestrator.analyzePhoto(
            testImage, 
            WorkType.GENERAL_CONSTRUCTION,
            AnalysisOptions(confidenceThreshold = 0.3f)
        )
        
        // Test with high threshold (0.8)
        val highThresholdResult = aiOrchestrator.analyzePhoto(
            testImage,
            WorkType.GENERAL_CONSTRUCTION, 
            AnalysisOptions(confidenceThreshold = 0.8f)
        )
        
        val lowCount = lowThresholdResult.getOrNull()?.hazards?.size ?: 0
        val highCount = highThresholdResult.getOrNull()?.hazards?.size ?: 0
        
        assertTrue("Higher threshold should filter more detections", highCount <= lowCount)
    }
}
```

## 4. Edge Case Coverage Testing

### 4.1 Network Failure Scenarios

#### Test Suite: NetworkFailureTest

```kotlin
class NetworkFailureTest {
    
    @Test
    suspend fun `test network timeout handling`() {
        val mockNetworkService = MockNetworkService(simulateTimeout = true)
        val orchestrator = createOrchestratorWithMockNetwork(mockNetworkService)
        
        val result = orchestrator.analyzePhoto(loadTestImage("standard/test.jpg"), WorkType.GENERAL_CONSTRUCTION)
        
        // Should fallback to local YOLO analysis
        assertTrue(result.isSuccess)
        assertEquals(AnalysisType.LOCAL_YOLO_FALLBACK, result.getOrNull()?.analysisType)
    }
    
    @Test
    suspend fun `test intermittent connectivity`() {
        val mockNetworkService = MockNetworkService(simulateIntermittent = true)
        val orchestrator = createOrchestratorWithMockNetwork(mockNetworkService)
        
        val results = (1..10).map {
            orchestrator.analyzePhoto(loadTestImage("standard/test_$it.jpg"), WorkType.GENERAL_CONSTRUCTION)
        }
        
        // Should have mix of cloud and local analysis based on network availability
        assertTrue("All analyses should succeed despite network issues", results.all { it.isSuccess })
    }
    
    @Test
    suspend fun `test API rate limiting response`() {
        val mockAPIService = MockGeminiService(simulateRateLimit = true)
        
        val result = mockAPIService.analyzePhoto(loadTestImage("standard/test.jpg"), WorkType.GENERAL_CONSTRUCTION)
        
        assertTrue("Should handle rate limiting gracefully", result.isFailure)
        assertContains(result.exceptionOrNull()?.message ?: "", "rate limit")
    }
}
```

### 4.2 Malformed AI Response Handling

#### Test Suite: MalformedResponseTest

```kotlin
class MalformedResponseTest {
    
    @Test
    suspend fun `test corrupted JSON response`() {
        val corruptedResponse = """{"hazards": [{"type": "INVALID_TYPE", "confidence":}"""
        val parser = GeminiResponseParser()
        
        val result = parser.parseResponse(corruptedResponse)
        assertTrue("Should handle corrupted JSON", result.isFailure)
    }
    
    @Test
    suspend fun `test missing required fields`() {
        val incompleteResponse = """{"hazards": [{"type": "FALL_PROTECTION"}]}""" // Missing confidence
        val parser = GeminiResponseParser()
        
        val result = parser.parseResponse(incompleteResponse)
        assertTrue("Should handle missing fields", result.isSuccess)
        // Should provide default confidence value
        assertTrue(result.getOrNull()?.hazards?.first()?.confidence ?: 0f > 0f)
    }
    
    @Test
    suspend fun `test unexpected data types`() {
        val wrongTypeResponse = """{"confidence": "high", "hazards": "none"}"""
        val parser = GeminiResponseParser()
        
        val result = parser.parseResponse(wrongTypeResponse)
        assertTrue("Should handle type mismatches", result.isFailure)
    }
}
```

### 4.3 Photo Quality and Edge Cases

#### Test Suite: PhotoQualityTest

```kotlin
class PhotoQualityTest {
    
    @Test
    suspend fun `test very dark images`() {
        val darkImage = loadTestImage("edge-cases/very_dark.jpg")
        val result = aiOrchestrator.analyzePhoto(darkImage, WorkType.GENERAL_CONSTRUCTION)
        
        assertTrue("Should process dark images", result.isSuccess)
        // Confidence may be lower but should not fail completely
    }
    
    @Test
    suspend fun `test blurry images`() {
        val blurryImage = loadTestImage("edge-cases/motion_blur.jpg")
        val result = aiOrchestrator.analyzePhoto(blurryImage, WorkType.GENERAL_CONSTRUCTION)
        
        assertTrue("Should process blurry images", result.isSuccess)
        // Should include warning about image quality
        assertContains(result.getOrNull()?.recommendations ?: emptyList(), "image quality")
    }
    
    @Test
    suspend fun `test oversized images`() {
        val largeImage = loadTestImage("edge-cases/oversized_20mb.jpg")
        val result = aiOrchestrator.analyzePhoto(largeImage, WorkType.GENERAL_CONSTRUCTION)
        
        assertTrue("Should handle large images", result.isSuccess)
        // Should compress before sending to API
    }
    
    @Test
    suspend fun `test no hazards present`() {
        val safeImage = loadTestImage("edge-cases/completely_safe_site.jpg")
        val result = aiOrchestrator.analyzePhoto(safeImage, WorkType.GENERAL_CONSTRUCTION)
        
        assertTrue("Should handle safe images", result.isSuccess)
        assertTrue("Should report no hazards", result.getOrNull()?.hazards?.isEmpty() == true)
        assertEquals(RiskLevel.LOW, result.getOrNull()?.overallRisk)
    }
}
```

## 5. Safety-Critical Testing Framework

### 5.1 Critical Hazard Detection Validation

#### Test Suite: CriticalHazardTest

```kotlin
class CriticalHazardTest {
    
    @Test
    suspend fun `test critical fall hazard never missed`() {
        val criticalFallImages = loadCriticalHazardImages("fall-protection")
        
        criticalFallImages.forEach { testCase ->
            val result = aiOrchestrator.analyzePhoto(testCase.imageData, testCase.workType)
            
            assertTrue("Critical hazard analysis failed", result.isSuccess)
            val analysis = result.getOrNull()!!
            
            // Critical fall hazards must be detected
            val fallHazard = analysis.hazards.find { it.type == HazardType.FALL_PROTECTION }
            assertNotNull("Critical fall hazard not detected in ${testCase.filename}", fallHazard)
            assertTrue("Fall hazard confidence too low", fallHazard.confidence >= 0.6f)
            assertTrue("Fall hazard severity not marked high", fallHazard.severity >= Severity.HIGH)
        }
    }
    
    @Test
    suspend fun `test electrical hazard detection reliability`() {
        val electricalHazards = loadCriticalHazardImages("electrical")
        
        val detectionRate = electricalHazards.map { testCase ->
            val result = aiOrchestrator.analyzePhoto(testCase.imageData, testCase.workType)
            val hasElectricalHazard = result.getOrNull()?.hazards?.any { 
                it.type == HazardType.ELECTRICAL 
            } == true
            hasElectricalHazard
        }.count { it }.toFloat() / electricalHazards.size
        
        assertTrue("Electrical hazard detection rate below 90%", detectionRate >= 0.9f)
    }
}
```

### 5.2 OSHA Compliance Validation

#### Test Suite: OSHAComplianceTest

```kotlin
class OSHAComplianceTest {
    
    @Test
    suspend fun `test OSHA code assignment accuracy`() {
        val complianceTestCases = loadOSHATestCases()
        
        complianceTestCases.forEach { testCase ->
            val result = aiOrchestrator.analyzePhoto(testCase.imageData, testCase.workType)
            val analysis = result.getOrNull()!!
            
            // Verify OSHA codes are assigned
            analysis.hazards.forEach { hazard ->
                assertNotNull("OSHA code missing for ${hazard.type}", hazard.oshaCode)
                assertTrue("Invalid OSHA code format", isValidOSHACode(hazard.oshaCode))
            }
            
            // Verify expected OSHA violations are detected
            testCase.expectedOSHACodes.forEach { expectedCode ->
                assertTrue(
                    "Expected OSHA code $expectedCode not found",
                    analysis.hazards.any { it.oshaCode == expectedCode }
                )
            }
        }
    }
    
    @Test
    suspend fun `test safety recommendation quality`() {
        val testImage = loadTestImage("fall-protection/unguarded_edge.jpg")
        val result = aiOrchestrator.analyzePhoto(testImage, WorkType.CONCRETE_WORK)
        
        val recommendations = result.getOrNull()?.recommendations ?: emptyList()
        
        assertTrue("No recommendations provided", recommendations.isNotEmpty())
        assertTrue("Recommendations should mention guardrails", 
            recommendations.any { it.contains("guardrail", ignoreCase = true) })
        assertTrue("Recommendations should reference OSHA standards",
            recommendations.any { it.contains("OSHA") || it.contains("1926") })
    }
    
    private fun isValidOSHACode(code: String?): Boolean {
        return code?.matches(Regex("\\d{4}\\.\\d+")) == true
    }
}
```

### 5.3 False Positive/Negative Analysis

#### Test Suite: FalseDetectionTest

```kotlin
class FalseDetectionTest {
    
    @Test
    suspend fun `test false positive rate for safe conditions`() {
        val safeImages = loadTestImages("safe-conditions")
        val falsePositives = mutableListOf<String>()
        
        safeImages.forEach { testCase ->
            val result = aiOrchestrator.analyzePhoto(testCase.imageData, testCase.workType)
            val analysis = result.getOrNull()!!
            
            if (analysis.hazards.any { it.severity >= Severity.HIGH }) {
                falsePositives.add(testCase.filename)
            }
        }
        
        val falsePositiveRate = falsePositives.size.toFloat() / safeImages.size
        assertTrue("False positive rate too high: ${falsePositiveRate * 100}%", 
            falsePositiveRate <= 0.05f) // Max 5% false positive rate
    }
    
    @Test
    suspend fun `test false negative rate for known hazards`() {
        val hazardImages = loadTestImages("known-hazards")
        val falseNegatives = mutableListOf<String>()
        
        hazardImages.forEach { testCase ->
            val result = aiOrchestrator.analyzePhoto(testCase.imageData, testCase.workType)
            val analysis = result.getOrNull()!!
            
            val expectedHazardDetected = testCase.expectedHazards.all { expected ->
                analysis.hazards.any { detected ->
                    detected.type == expected.type && detected.confidence >= 0.5f
                }
            }
            
            if (!expectedHazardDetected) {
                falseNegatives.add(testCase.filename)
            }
        }
        
        val falseNegativeRate = falseNegatives.size.toFloat() / hazardImages.size
        assertTrue("False negative rate too high: ${falseNegativeRate * 100}%",
            falseNegativeRate <= 0.10f) // Max 10% false negative rate
    }
}
```

## 6. Performance Testing Suite

### 6.1 Load Testing for Concurrent AI Requests

#### Test Suite: AILoadTest

```kotlin
class AILoadTest {
    
    @Test
    suspend fun `test concurrent user analysis requests`() {
        val concurrentUsers = 10
        val requestsPerUser = 5
        val testImage = loadTestImage("standard/construction_site.jpg")
        
        val startTime = System.currentTimeMillis()
        
        val results = (1..concurrentUsers).map { userId ->
            async {
                (1..requestsPerUser).map {
                    aiOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
                }
            }
        }.awaitAll().flatten()
        
        val totalTime = System.currentTimeMillis() - startTime
        val successfulRequests = results.count { it.isSuccess }
        
        assertTrue("Load test failed requests", successfulRequests >= results.size * 0.95) // 95% success rate
        assertTrue("Load test took too long", totalTime < 60000) // Under 1 minute
        
        logPerformanceMetrics("Load Test", totalTime, successfulRequests, results.size)
    }
    
    @Test
    suspend fun `test memory usage under sustained load`() {
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val testImage = loadTestImage("standard/construction_site.jpg")
        
        repeat(50) { iteration ->
            val result = aiOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
            assertTrue("Analysis $iteration failed", result.isSuccess)
            
            if (iteration % 10 == 0) {
                System.gc() // Suggest garbage collection
                delay(100) // Allow GC to run
            }
        }
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        assertTrue("Memory leak detected: ${memoryIncrease / 1024 / 1024}MB increase",
            memoryIncrease < 100 * 1024 * 1024) // Less than 100MB increase
    }
}
```

### 6.2 Battery Life Impact Testing (Mobile)

#### Test Suite: BatteryImpactTest

```kotlin
class BatteryImpactTest {
    
    @Test
    suspend fun `test battery consumption during AI processing`() {
        val batteryMonitor = MockBatteryMonitor()
        val initialBatteryLevel = batteryMonitor.getBatteryLevel()
        val testImage = loadTestImage("standard/construction_site.jpg")
        
        val startTime = System.currentTimeMillis()
        
        // Simulate 1 hour of typical usage (1 analysis every 2 minutes)
        repeat(30) {
            val result = aiOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
            assertTrue("Analysis failed", result.isSuccess)
            delay(2000) // 2 second intervals for test speed
        }
        
        val finalBatteryLevel = batteryMonitor.getBatteryLevel()
        val batteryDrain = initialBatteryLevel - finalBatteryLevel
        
        // Should not drain more than 10% battery in simulated hour
        assertTrue("Excessive battery drain: ${batteryDrain}%", batteryDrain <= 10f)
    }
}
```

### 6.3 Offline Behavior Testing

#### Test Suite: OfflineBehaviorTest

```kotlin
class OfflineBehaviorTest {
    
    @Test
    suspend fun `test offline AI analysis fallback`() {
        val offlineOrchestrator = createOfflineOrchestrator()
        val testImage = loadTestImage("standard/construction_site.jpg")
        
        val result = offlineOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
        
        assertTrue("Offline analysis should succeed", result.isSuccess)
        assertEquals("Should use local YOLO fallback", 
            AnalysisType.LOCAL_YOLO_FALLBACK, result.getOrNull()?.analysisType)
        
        val analysis = result.getOrNull()!!
        assertTrue("Should include offline warning",
            analysis.recommendations.any { it.contains("offline", ignoreCase = true) })
    }
    
    @Test
    suspend fun `test analysis queue when offline`() {
        val queueManager = AnalysisQueueManager()
        val testImages = (1..5).map { loadTestImage("queue/test_$it.jpg") }
        
        // Queue analyses while offline
        testImages.forEach { image ->
            queueManager.queueAnalysis(image, WorkType.GENERAL_CONSTRUCTION)
        }
        
        assertEquals("Should queue all analyses", 5, queueManager.getQueueSize())
        
        // Simulate going online
        queueManager.setOnlineStatus(true)
        delay(5000) // Wait for queue processing
        
        assertEquals("Should process all queued analyses", 0, queueManager.getQueueSize())
    }
}
```

## 7. User Experience Testing Framework

### 7.1 AI Results Presentation Testing

#### Test Suite: ResultsPresentationTest

```kotlin
class ResultsPresentationTest {
    
    @Test
    suspend fun `test hazard visualization clarity`() {
        val testImage = loadTestImage("fall-protection/multiple_hazards.jpg")
        val result = aiOrchestrator.analyzePhoto(testImage, WorkType.CONCRETE_WORK)
        val analysis = result.getOrNull()!!
        
        // Verify bounding boxes are reasonable
        analysis.hazards.forEach { hazard ->
            assertNotNull("Bounding box missing for ${hazard.type}", hazard.boundingBox)
            val bbox = hazard.boundingBox!!
            assertTrue("Invalid bounding box coordinates", 
                bbox.x >= 0f && bbox.y >= 0f && 
                bbox.x + bbox.width <= 1f && bbox.y + bbox.height <= 1f)
        }
        
        // Verify severity color coding
        val highSeverityHazards = analysis.hazards.filter { it.severity == Severity.HIGH }
        assertTrue("High severity hazards should be present", highSeverityHazards.isNotEmpty())
    }
    
    @Test
    suspend fun `test confidence score interpretation`() {
        val testImage = loadTestImage("ppe/partial_hard_hat.jpg")
        val result = aiOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
        val analysis = result.getOrNull()!!
        
        // Confidence should be displayed appropriately
        analysis.hazards.forEach { hazard ->
            assertTrue("Confidence should be between 0 and 1", 
                hazard.confidence >= 0f && hazard.confidence <= 1f)
        }
        
        // Overall confidence should reflect individual hazard confidences
        val avgConfidence = analysis.hazards.map { it.confidence }.average().toFloat()
        assertTrue("Overall confidence should align with average",
            kotlin.math.abs(analysis.confidence - avgConfidence) < 0.2f)
    }
}
```

### 7.2 Accessibility Testing for Safety-Critical Features

#### Test Suite: AccessibilityTest

```kotlin
class AccessibilityTest {
    
    @Test
    suspend fun `test high contrast hazard indicators`() {
        val testImage = loadTestImage("accessibility/low_vision_test.jpg")
        val result = aiOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
        val analysis = result.getOrNull()!!
        
        // Verify hazard descriptions are clear and detailed
        analysis.hazards.forEach { hazard ->
            assertTrue("Hazard description too short", hazard.description.length >= 10)
            assertFalse("Hazard description should avoid technical jargon",
                hazard.description.contains(Regex("\\b[A-Z]{3,}\\b"))) // Avoid abbreviations
        }
    }
    
    @Test
    suspend fun `test audio description support`() {
        val testImage = loadTestImage("accessibility/visual_impairment_test.jpg")
        val result = aiOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
        val analysis = result.getOrNull()!!
        
        // Generate audio-friendly descriptions
        val audioDescription = generateAudioDescription(analysis)
        
        assertTrue("Audio description should be comprehensive", audioDescription.length >= 50)
        assertTrue("Should include severity information",
            audioDescription.contains("high risk", ignoreCase = true) ||
            audioDescription.contains("medium risk", ignoreCase = true))
    }
}
```

### 7.3 Construction Worker Workflow Testing

#### Test Suite: WorkerWorkflowTest

```kotlin
class WorkerWorkflowTest {
    
    @Test
    suspend fun `test quick hazard assessment workflow`() {
        val workflowTimer = WorkflowTimer()
        
        // Simulate worker taking photo and getting analysis
        workflowTimer.start("photo_capture")
        val testImage = loadTestImage("workflow/quick_assessment.jpg")
        workflowTimer.end("photo_capture")
        
        workflowTimer.start("ai_analysis")
        val result = aiOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
        workflowTimer.end("ai_analysis")
        
        workflowTimer.start("results_review")
        val analysis = result.getOrNull()!!
        val actionableRecommendations = analysis.recommendations.filter { 
            it.contains("install", ignoreCase = true) || 
            it.contains("use", ignoreCase = true) ||
            it.contains("ensure", ignoreCase = true)
        }
        workflowTimer.end("results_review")
        
        // Workflow should be completed in under 30 seconds
        val totalTime = workflowTimer.getTotalTime()
        assertTrue("Workflow too slow for field use", totalTime < 30000)
        
        // Results should be actionable
        assertTrue("Should provide actionable recommendations", 
            actionableRecommendations.isNotEmpty())
    }
    
    @Test
    suspend fun `test multi-language safety recommendations`() {
        val testImage = loadTestImage("workflow/multi_language_test.jpg")
        
        // Test English (default)
        val englishResult = aiOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION,
            AnalysisOptions(language = "en"))
        
        // Test Spanish
        val spanishResult = aiOrchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION,
            AnalysisOptions(language = "es"))
        
        val englishAnalysis = englishResult.getOrNull()!!
        val spanishAnalysis = spanishResult.getOrNull()!!
        
        // Should detect same hazards regardless of language
        assertEquals("Same hazards should be detected", 
            englishAnalysis.hazards.size, spanishAnalysis.hazards.size)
        
        // Recommendations should be in appropriate language
        assertTrue("Spanish recommendations should contain Spanish text",
            spanishAnalysis.recommendations.any { it.contains("usar") || it.contains("instalar") })
    }
}
```

## 8. Testing Implementation Guidelines

### 8.1 Test Data Requirements

**Critical Test Image Categories:**
1. **Fall Protection** (50+ images)
   - Unguarded edges, missing guardrails
   - Improper ladder use, unstable scaffolding
   - Various heights and scenarios

2. **PPE Violations** (40+ images)
   - Missing hard hats, safety vests, eye protection
   - Improper PPE usage
   - Partial visibility scenarios

3. **Electrical Hazards** (30+ images)
   - Exposed wiring, improper grounding
   - Power line proximity
   - Wet conditions with electrical equipment

4. **Struck-by Hazards** (30+ images)
   - Heavy machinery operation
   - Falling objects
   - Vehicle traffic areas

5. **Edge Cases** (25+ images)
   - Poor lighting conditions
   - Weather impacts (rain, snow, fog)
   - Crowded work areas

### 8.2 Mock Service Implementation

```kotlin
class MockGeminiService : AIPhotoAnalyzer {
    private var simulateFailure = false
    private var simulateTimeout = false
    private var simulateRateLimit = false
    
    override suspend fun analyzePhoto(
        imageData: ByteArray, 
        workType: WorkType
    ): Result<SafetyAnalysis> {
        when {
            simulateTimeout -> {
                delay(20000) // Simulate timeout
                return Result.failure(Exception("Request timeout"))
            }
            simulateRateLimit -> {
                return Result.failure(Exception("API rate limit exceeded"))
            }
            simulateFailure -> {
                return Result.failure(Exception("Service temporarily unavailable"))
            }
            else -> {
                // Return realistic mock analysis based on image content
                return Result.success(generateMockAnalysis(imageData, workType))
            }
        }
    }
    
    private fun generateMockAnalysis(imageData: ByteArray, workType: WorkType): SafetyAnalysis {
        // Analyze image filename or content to provide realistic mock responses
        val hazards = when {
            imageContainsKeyword(imageData, "fall") -> listOf(
                DetectedHazard(
                    type = HazardType.FALL_PROTECTION,
                    description = "Worker near unguarded edge",
                    severity = Severity.HIGH,
                    confidence = 0.87f,
                    oshaCode = "1926.501"
                )
            )
            imageContainsKeyword(imageData, "electrical") -> listOf(
                DetectedHazard(
                    type = HazardType.ELECTRICAL,
                    description = "Exposed electrical wiring",
                    severity = Severity.HIGH,
                    confidence = 0.91f,
                    oshaCode = "1926.95"
                )
            )
            else -> emptyList()
        }
        
        return SafetyAnalysis(
            id = "mock-${System.currentTimeMillis()}",
            hazards = hazards,
            overallRisk = if (hazards.any { it.severity == Severity.HIGH }) RiskLevel.HIGH else RiskLevel.LOW,
            confidence = hazards.map { it.confidence }.average().toFloat(),
            processingTimeMs = Random.nextLong(100, 2000),
            aiProvider = "MockGemini"
        )
    }
}
```

### 8.3 Continuous Integration Testing

**GitHub Actions Workflow:**
```yaml
name: AI Integration Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  ai-integration-tests:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Download test data
      run: |
        mkdir -p test-data
        # Download sanitized test images (no sensitive content)
        
    - name: Run AI unit tests
      run: ./gradlew shared:testDebugUnitTest --tests "*AI*Test"
      
    - name: Run AI integration tests
      run: ./gradlew shared:testDebugUnitTest --tests "*Integration*Test"
      env:
        GEMINI_API_KEY: ${{ secrets.GEMINI_TEST_API_KEY }}
        
    - name: Run performance benchmarks
      run: ./gradlew shared:benchmark
      
    - name: Upload test results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: ai-test-results
        path: |
          shared/build/test-results/
          shared/build/reports/
```

### 8.4 Quality Gates and Success Criteria

**Minimum Requirements for AI Integration:**

1. **Accuracy Thresholds:**
   - Fall protection detection: ≥85% accuracy
   - PPE detection: ≥80% accuracy
   - Critical hazard detection: ≥90% accuracy
   - False positive rate: ≤5%
   - False negative rate: ≤10%

2. **Performance Requirements:**
   - Average response time: ≤8 seconds
   - 95th percentile response time: ≤15 seconds
   - Concurrent user support: ≥20 users
   - Memory usage: ≤512MB per analysis

3. **Reliability Requirements:**
   - Service availability: ≥99.5%
   - Graceful degradation: 100% (must fallback to local analysis)
   - Error recovery: ≤3 retry attempts
   - Offline functionality: 100% available

4. **Safety Requirements:**
   - Critical hazard detection: 100% (no false negatives for life-threatening hazards)
   - OSHA compliance: 100% accurate code assignment
   - Safety recommendations: Present for all detected hazards

## 9. Reporting and Monitoring

### 9.1 Test Result Dashboard

Create automated test reporting that tracks:
- AI accuracy metrics over time
- Performance regression detection
- False positive/negative trends
- Service availability statistics
- User workflow completion rates

### 9.2 Production Monitoring

Implement monitoring for:
- Real-time AI analysis success rates
- Response time percentiles
- Error patterns and categorization
- User satisfaction metrics
- Safety incident correlation with AI recommendations

## 10. Conclusion

This comprehensive testing strategy ensures that HazardHawk's AI integration meets the highest standards for construction safety applications. By implementing rigorous testing across accuracy, performance, reliability, and safety dimensions, we can confidently deploy AI-powered hazard detection that construction workers can trust with their safety.

The testing framework emphasizes:
- **Safety-first approach** with zero tolerance for missed critical hazards
- **Real-world validation** using diverse construction site scenarios
- **Performance optimization** for mobile device constraints
- **Accessibility compliance** for diverse user needs
- **Continuous monitoring** for ongoing quality assurance

Regular execution of this testing suite will ensure that AI improvements enhance rather than compromise the safety-critical functionality that construction professionals depend on.
