package com.hazardhawk.ai.litert

import com.hazardhawk.TestUtils
import com.hazardhawk.core.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Comprehensive unit test suite for LiteRT Model Engine.
 * Tests backend selection, device capability detection, error handling,
 * and construction safety analysis workflows.
 * 
 * Coverage Areas:
 * - Backend Selection Logic (AUTO, CPU, GPU, NPU)
 * - Device Capability Detection 
 * - Memory Management & Thermal Protection
 * - Error Handling & Fallback Chains
 * - Construction Safety Analysis Accuracy
 * - Performance Validation
 * - OSHA Compliance Testing
 */
class LiteRTModelEngineTest {
    
    private lateinit var mockEngine: MockLiteRTModelEngine
    private lateinit var testDataFactory: LiteRTTestDataFactory
    
    @BeforeTest
    fun setup() {
        mockEngine = MockLiteRTModelEngine()
        testDataFactory = LiteRTTestDataFactory()
    }
    
    @AfterTest
    fun tearDown() {
        mockEngine.cleanup()
    }
    
    // =====================================================
    // BACKEND SELECTION TESTS
    // =====================================================
    
    @Test
    fun `test backend selection with AUTO chooses optimal backend`() = runTest {
        // High-end device with NPU support
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 8f,
            supportedBackends = setOf(
                LiteRTBackend.CPU,
                LiteRTBackend.GPU_OPENCL,
                LiteRTBackend.NPU_QTI_HTP
            )
        )
        
        val result = mockEngine.initialize("test_model.litertmlm", LiteRTBackend.AUTO)
        
        assertTrue(result.isSuccess, "Initialization should succeed")
        assertEquals(LiteRTBackend.NPU_QTI_HTP, mockEngine.currentBackend)
    }
    
    @Test
    fun `test backend selection fallback chain for mid-range devices`() = runTest {
        // Mid-range device without NPU
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 4f,
            supportedBackends = setOf(
                LiteRTBackend.CPU,
                LiteRTBackend.GPU_OPENCL
            )
        )
        
        val result = mockEngine.initialize("test_model.litertmlm", LiteRTBackend.AUTO)
        
        assertTrue(result.isSuccess)
        assertEquals(LiteRTBackend.GPU_OPENCL, mockEngine.currentBackend)
    }
    
    @Test
    fun `test CPU fallback for low-end devices`() = runTest {
        // Low-end device with CPU only
        mockEngine.setDeviceCapabilities(
            totalMemoryGB = 2f,
            supportedBackends = setOf(LiteRTBackend.CPU)
        )
        
        val result = mockEngine.initialize("test_model.litertmlm", LiteRTBackend.AUTO)
        
        assertTrue(result.isSuccess)
        assertEquals(LiteRTBackend.CPU, mockEngine.currentBackend)
    }
    
    @Test
    fun `test unsupported backend throws appropriate exception`() = runTest {
        mockEngine.setDeviceCapabilities(
            supportedBackends = setOf(LiteRTBackend.CPU)
        )
        
        val result = mockEngine.initialize("test_model.litertmlm", LiteRTBackend.NPU_QTI_HTP)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is LiteRTException.UnsupportedBackendException)
    }
    
    // =====================================================
    // DEVICE CAPABILITY DETECTION TESTS
    // =====================================================
    
    @Test
    fun `test device capability detection for various Android versions`() {
        val testCases = listOf(
            DeviceTestCase(
                androidVersion = 24, // Android 7.0
                expectedBackends = setOf(LiteRTBackend.CPU, LiteRTBackend.GPU_OPENGL)
            ),
            DeviceTestCase(
                androidVersion = 27, // Android 8.1 - NNAPI introduced
                expectedBackends = setOf(
                    LiteRTBackend.CPU, 
                    LiteRTBackend.GPU_OPENGL, 
                    LiteRTBackend.NPU_NNAPI
                )
            ),
            DeviceTestCase(
                androidVersion = 34, // Android 14
                expectedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI,
                    LiteRTBackend.NPU_QTI_HTP
                )
            )
        )
        
        testCases.forEach { testCase ->
            mockEngine.setAndroidVersion(testCase.androidVersion)
            val supportedBackends = mockEngine.supportedBackends
            
            testCase.expectedBackends.forEach { expectedBackend ->
                assertTrue(
                    supportedBackends.contains(expectedBackend),
                    "Android ${testCase.androidVersion} should support $expectedBackend"
                )
            }
        }
    }
    
    @Test
    fun `test Qualcomm chipset detection`() {
        mockEngine.setBoardInfo("qcom_msm8998") // Qualcomm board
        
        val supportedBackends = mockEngine.supportedBackends
        
        assertTrue(
            supportedBackends.contains(LiteRTBackend.NPU_QTI_HTP),
            "Qualcomm devices should support HTP backend"
        )
    }
    
    @Test
    fun `test non-Qualcomm chipset detection`() {
        mockEngine.setBoardInfo("exynos9820") // Samsung Exynos
        
        val supportedBackends = mockEngine.supportedBackends
        
        assertFalse(
            supportedBackends.contains(LiteRTBackend.NPU_QTI_HTP),
            "Non-Qualcomm devices should not support HTP backend"
        )
    }
    
    // =====================================================
    // MEMORY MANAGEMENT TESTS
    // =====================================================
    
    @Test
    fun `test memory requirement validation`() = runTest {
        val testCases = listOf(
            MemoryTestCase(
                backend = LiteRTBackend.CPU,
                availableMemoryMB = 300f,
                shouldSucceed = false // CPU requires ~512MB minimum
            ),
            MemoryTestCase(
                backend = LiteRTBackend.GPU_OPENCL,
                availableMemoryMB = 400f,
                shouldSucceed = false // GPU requires ~512-1GB
            ),
            MemoryTestCase(
                backend = LiteRTBackend.NPU_NNAPI,
                availableMemoryMB = 600f,
                shouldSucceed = true // NPU is more memory efficient
            )
        )
        
        testCases.forEach { testCase ->
            mockEngine.setAvailableMemory(testCase.availableMemoryMB)
            
            val result = mockEngine.initialize("test_model.litertmlm", testCase.backend)
            
            assertEquals(
                testCase.shouldSucceed,
                result.isSuccess,
                "Backend ${testCase.backend} with ${testCase.availableMemoryMB}MB should ${if (testCase.shouldSucceed) "succeed" else "fail"}"
            )
            
            if (!result.isSuccess && !testCase.shouldSucceed) {
                assertTrue(
                    result.exceptionOrNull() is LiteRTException.OutOfMemoryException,
                    "Should throw OutOfMemoryException for insufficient memory"
                )
            }
        }
    }
    
    @Test
    fun `test thermal throttling protection`() = runTest {
        // Set high device temperature
        mockEngine.setDeviceTemperature(85f) // Above THERMAL_STATE_SEVERE threshold
        
        val imageData = testDataFactory.createConstructionSiteImage()
        val result = mockEngine.generateSafetyAnalysis(
            imageData = imageData,
            workType = WorkType.GENERAL_CONSTRUCTION
        )
        
        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull() is LiteRTException.ThermalThrottlingException,
            "Should throw ThermalThrottlingException when device is too hot"
        )
    }
    
    // =====================================================
    // ERROR HANDLING TESTS
    // =====================================================
    
    @Test
    fun `test initialization failure scenarios`() = runTest {
        val testCases = listOf(
            ErrorTestCase(
                scenario = "Model file not found",
                setupAction = { mockEngine.setModelFileExists(false) },
                expectedExceptionType = LiteRTException.ModelLoadException::class
            ),
            ErrorTestCase(
                scenario = "Corrupted model file",
                setupAction = { mockEngine.setModelCorrupted(true) },
                expectedExceptionType = LiteRTException.ModelLoadException::class
            ),
            ErrorTestCase(
                scenario = "Insufficient permissions",
                setupAction = { mockEngine.setPermissionDenied(true) },
                expectedExceptionType = LiteRTException.InitializationException::class
            )
        )
        
        testCases.forEach { testCase ->
            testCase.setupAction()
            
            val result = mockEngine.initialize("test_model.litertmlm", LiteRTBackend.CPU)
            
            assertTrue(result.isFailure, "Should fail for scenario: ${testCase.scenario}")
            assertTrue(
                testCase.expectedExceptionType.isInstance(result.exceptionOrNull()),
                "Should throw ${testCase.expectedExceptionType.simpleName} for ${testCase.scenario}"
            )
            
            // Reset for next test
            mockEngine.reset()
        }
    }
    
    @Test
    fun `test analysis failure scenarios`() = runTest {
        mockEngine.initialize("test_model.litertmlm", LiteRTBackend.CPU)
        
        val testCases = listOf(
            AnalysisErrorTestCase(
                scenario = "Invalid image data",
                imageData = ByteArray(0), // Empty array
                expectedExceptionType = LiteRTException.InferenceException::class
            ),
            AnalysisErrorTestCase(
                scenario = "Corrupted image data",
                imageData = testDataFactory.createCorruptedImageData(),
                expectedExceptionType = LiteRTException.InferenceException::class
            ),
            AnalysisErrorTestCase(
                scenario = "Out of memory during inference",
                imageData = testDataFactory.createLargeImageData(8192, 8192),
                setupAction = { mockEngine.setAvailableMemory(100f) },
                expectedExceptionType = LiteRTException.OutOfMemoryException::class
            )
        )
        
        testCases.forEach { testCase ->
            testCase.setupAction?.invoke()
            
            val result = mockEngine.generateSafetyAnalysis(
                imageData = testCase.imageData,
                workType = WorkType.GENERAL_CONSTRUCTION
            )
            
            assertTrue(result.isFailure, "Should fail for scenario: ${testCase.scenario}")
            assertTrue(
                testCase.expectedExceptionType.isInstance(result.exceptionOrNull()),
                "Should throw correct exception type for ${testCase.scenario}"
            )
            
            mockEngine.reset()
        }
    }
    
    // =====================================================
    // CONSTRUCTION SAFETY ANALYSIS TESTS
    // =====================================================
    
    @Test
    fun `test PPE detection analysis`() = runTest {
        mockEngine.initialize("test_model.litertmlm", LiteRTBackend.CPU)
        
        val testCases = listOf(
            ConstructionTestCase(
                scenario = "Complete PPE compliance",
                imageData = testDataFactory.createPPECompliantImage(),
                expectedPPEDetections = mapOf(
                    PPEType.HARD_HAT to PPEDetection(detected = true, confidence = 0.95f),
                    PPEType.SAFETY_VEST to PPEDetection(detected = true, confidence = 0.92f),
                    PPEType.SAFETY_BOOTS to PPEDetection(detected = true, confidence = 0.88f)
                )
            ),
            ConstructionTestCase(
                scenario = "Missing hard hat violation",
                imageData = testDataFactory.createMissingHardHatImage(),
                expectedPPEDetections = mapOf(
                    PPEType.HARD_HAT to PPEDetection(detected = false, confidence = 0.96f),
                    PPEType.SAFETY_VEST to PPEDetection(detected = true, confidence = 0.89f)
                )
            )
        )
        
        testCases.forEach { testCase ->
            val result = mockEngine.generateSafetyAnalysis(
                imageData = testCase.imageData,
                workType = WorkType.GENERAL_CONSTRUCTION,
                includeOSHACodes = true
            )
            
            assertTrue(result.isSuccess, "Analysis should succeed for ${testCase.scenario}")
            
            val analysis = result.getOrNull()!!
            testCase.expectedPPEDetections.forEach { (ppeType, expectedDetection) ->
                val actualDetection = analysis.ppeStatus[ppeType]
                assertNotNull(actualDetection, "Should detect $ppeType")
                assertEquals(
                    expectedDetection.detected,
                    actualDetection.detected,
                    "PPE detection status for $ppeType in ${testCase.scenario}"
                )
                assertTrue(
                    actualDetection.confidence >= 0.8f,
                    "PPE detection confidence should be >= 0.8 for $ppeType"
                )
            }
        }
    }
    
    @Test
    fun `test OSHA violation detection`() = runTest {
        mockEngine.initialize("test_model.litertmlm", LiteRTBackend.CPU)
        
        val imageData = testDataFactory.createFallHazardImage()
        val result = mockEngine.generateSafetyAnalysis(
            imageData = imageData,
            workType = WorkType.GENERAL_CONSTRUCTION,
            includeOSHACodes = true
        )
        
        assertTrue(result.isSuccess)
        
        val analysis = result.getOrNull()!!
        assertTrue(analysis.oshaViolations.isNotEmpty(), "Should detect OSHA violations")
        
        // Check for fall protection violation
        val fallProtectionViolation = analysis.oshaViolations.find { 
            it.regulationCode.contains("1926.501") 
        }
        assertNotNull(fallProtectionViolation, "Should detect fall protection violation (1926.501)")
        assertEquals(Severity.HIGH, fallProtectionViolation.severity)
        assertTrue(
            fallProtectionViolation.description.contains("fall protection", ignoreCase = true),
            "Violation description should mention fall protection"
        )
    }
    
    @Test
    fun `test hazard detection with bounding boxes`() = runTest {
        mockEngine.initialize("test_model.litertmlm", LiteRTBackend.GPU_OPENCL)
        
        val imageData = testDataFactory.createMultipleHazardsImage()
        val result = mockEngine.generateSafetyAnalysis(
            imageData = imageData,
            workType = WorkType.GENERAL_CONSTRUCTION,
            confidenceThreshold = 0.7f
        )
        
        assertTrue(result.isSuccess)
        
        val analysis = result.getOrNull()!!
        assertTrue(analysis.hazards.size >= 2, "Should detect multiple hazards")
        
        analysis.hazards.forEach { hazard ->
            assertNotNull(hazard.boundingBox, "Each hazard should have a bounding box")
            
            val box = hazard.boundingBox!!
            assertTrue(box.x >= 0 && box.x <= 1, "Bounding box X should be normalized")
            assertTrue(box.y >= 0 && box.y <= 1, "Bounding box Y should be normalized")
            assertTrue(box.width > 0 && box.width <= 1, "Bounding box width should be valid")
            assertTrue(box.height > 0 && box.height <= 1, "Bounding box height should be valid")
            assertTrue(box.confidence >= 0.7f, "Bounding box confidence should meet threshold")
        }
    }
    
    // =====================================================
    // PERFORMANCE VALIDATION TESTS
    // =====================================================
    
    @Test
    fun `test backend performance characteristics`() = runTest {
        val imageData = testDataFactory.createStandardTestImage()
        val performanceTargets = mapOf(
            LiteRTBackend.CPU to 3000L,           // 3 seconds max for CPU
            LiteRTBackend.GPU_OPENCL to 1500L,    // 1.5 seconds max for GPU
            LiteRTBackend.NPU_NNAPI to 800L       // 800ms max for NPU
        )
        
        performanceTargets.forEach { (backend, targetTimeMs) ->
            mockEngine.reset()
            mockEngine.setDeviceCapabilities(supportedBackends = setOf(backend))
            
            val initResult = mockEngine.initialize("test_model.litertmlm", backend)
            assertTrue(initResult.isSuccess, "Should initialize $backend successfully")
            
            val (analysisResult, duration) = TestUtils.measureExecutionTime {
                mockEngine.generateSafetyAnalysis(
                    imageData = imageData,
                    workType = WorkType.GENERAL_CONSTRUCTION
                )
            }
            
            assertTrue(analysisResult.isSuccess, "Analysis should succeed for $backend")
            
            val actualTimeMs = duration.inWholeMilliseconds
            TestUtils.assertPerformanceWithin(
                actualMs = actualTimeMs,
                expectedMs = targetTimeMs,
                tolerancePercent = 25.0,
                scenario = "Analysis with $backend backend"
            )
            
            // Verify reported processing time matches measured time
            val reportedTimeMs = analysisResult.getOrNull()!!.processingTimeMs
            val timeDifference = kotlin.math.abs(actualTimeMs - reportedTimeMs)
            assertTrue(
                timeDifference <= 100,
                "Reported processing time ($reportedTimeMs ms) should match measured time ($actualTimeMs ms)"
            )
        }
    }
    
    @Test
    fun `test memory usage tracking`() = runTest {
        mockEngine.initialize("test_model.litertmlm", LiteRTBackend.GPU_OPENCL)
        
        val initialMetrics = mockEngine.getPerformanceMetrics()
        assertEquals(0L, initialMetrics.analysisCount)
        assertEquals(0f, initialMetrics.peakMemoryUsageMB)
        
        // Perform several analyses
        repeat(5) { iteration ->
            val imageData = testDataFactory.createTestImage(1920 + (iteration * 100), 1080)
            val result = mockEngine.generateSafetyAnalysis(
                imageData = imageData,
                workType = WorkType.GENERAL_CONSTRUCTION
            )
            assertTrue(result.isSuccess, "Analysis $iteration should succeed")
        }
        
        val finalMetrics = mockEngine.getPerformanceMetrics()
        assertEquals(5L, finalMetrics.analysisCount)
        assertTrue(finalMetrics.peakMemoryUsageMB > 0, "Should track peak memory usage")
        assertTrue(finalMetrics.averageMemoryUsageMB > 0, "Should track average memory usage")
        assertTrue(
            finalMetrics.peakMemoryUsageMB >= finalMetrics.averageMemoryUsageMB,
            "Peak memory should be >= average memory"
        )
        assertEquals(1.0f, finalMetrics.successRate, 0.001f)
    }
    
    @Test
    fun `test confidence threshold filtering`() = runTest {
        mockEngine.initialize("test_model.litertmlm", LiteRTBackend.CPU)
        
        val imageData = testDataFactory.createLowConfidenceHazardsImage()
        
        // Test with high confidence threshold
        val highThresholdResult = mockEngine.generateSafetyAnalysis(
            imageData = imageData,
            workType = WorkType.GENERAL_CONSTRUCTION,
            confidenceThreshold = 0.9f
        )
        
        // Test with low confidence threshold
        val lowThresholdResult = mockEngine.generateSafetyAnalysis(
            imageData = imageData,
            workType = WorkType.GENERAL_CONSTRUCTION,
            confidenceThreshold = 0.3f
        )
        
        assertTrue(highThresholdResult.isSuccess && lowThresholdResult.isSuccess)
        
        val highThresholdHazards = highThresholdResult.getOrNull()!!.hazards
        val lowThresholdHazards = lowThresholdResult.getOrNull()!!.hazards
        
        assertTrue(
            highThresholdHazards.size <= lowThresholdHazards.size,
            "High threshold should filter out more hazards"
        )
        
        // Verify all returned hazards meet the confidence threshold
        highThresholdHazards.forEach { hazard ->
            assertTrue(
                hazard.confidence >= 0.9f,
                "All hazards should meet high confidence threshold"
            )
        }
    }
    
    // =====================================================
    // WORK TYPE SPECIFIC TESTS
    // =====================================================
    
    @Test
    fun `test work type specific analysis`() = runTest {
        mockEngine.initialize("test_model.litertmlm", LiteRTBackend.CPU)
        
        val testCases = listOf(
            WorkTypeTestCase(
                workType = WorkType.ELECTRICAL,
                expectedHazardTypes = setOf(HazardType.ELECTRICAL, HazardType.FALL),
                expectedOSHACodes = setOf("1926.416", "1926.501")
            ),
            WorkTypeTestCase(
                workType = WorkType.EXCAVATION,
                expectedHazardTypes = setOf(HazardType.CAUGHT_IN, HazardType.STRUCK_BY),
                expectedOSHACodes = setOf("1926.651", "1926.95")
            ),
            WorkTypeTestCase(
                workType = WorkType.ROOFING,
                expectedHazardTypes = setOf(HazardType.FALL),
                expectedOSHACodes = setOf("1926.501", "1926.502")
            )
        )
        
        testCases.forEach { testCase ->
            val imageData = testDataFactory.createWorkTypeSpecificImage(testCase.workType)
            
            val result = mockEngine.generateSafetyAnalysis(
                imageData = imageData,
                workType = testCase.workType,
                includeOSHACodes = true
            )
            
            assertTrue(result.isSuccess, "Analysis should succeed for ${testCase.workType}")
            
            val analysis = result.getOrNull()!!
            
            // Check that expected hazard types are detected
            val detectedHazardTypes = analysis.hazards.map { it.type }.toSet()
            val hasExpectedHazards = testCase.expectedHazardTypes.any { it in detectedHazardTypes }
            assertTrue(
                hasExpectedHazards,
                "Should detect expected hazard types for ${testCase.workType}"
            )
            
            // Check that expected OSHA codes are present
            val detectedOSHACodes = analysis.oshaViolations.map { it.regulationCode }.toSet()
            val hasExpectedCodes = testCase.expectedOSHACodes.any { expectedCode ->
                detectedOSHACodes.any { detectedCode -> detectedCode.contains(expectedCode) }
            }
            assertTrue(
                hasExpectedCodes,
                "Should detect expected OSHA codes for ${testCase.workType}"
            )
        }
    }
}

// =====================================================
// TEST DATA CLASSES AND HELPERS
// =====================================================

data class DeviceTestCase(
    val androidVersion: Int,
    val expectedBackends: Set<LiteRTBackend>
)

data class MemoryTestCase(
    val backend: LiteRTBackend,
    val availableMemoryMB: Float,
    val shouldSucceed: Boolean
)

data class ErrorTestCase(
    val scenario: String,
    val setupAction: () -> Unit,
    val expectedExceptionType: kotlin.reflect.KClass<out LiteRTException>
)

data class AnalysisErrorTestCase(
    val scenario: String,
    val imageData: ByteArray,
    val setupAction: (() -> Unit)? = null,
    val expectedExceptionType: kotlin.reflect.KClass<out LiteRTException>
)

data class ConstructionTestCase(
    val scenario: String,
    val imageData: ByteArray,
    val expectedPPEDetections: Map<PPEType, PPEDetection>
)

data class WorkTypeTestCase(
    val workType: WorkType,
    val expectedHazardTypes: Set<HazardType>,
    val expectedOSHACodes: Set<String>
)
