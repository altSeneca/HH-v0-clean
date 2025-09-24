package com.hazardhawk.ai.litert

import com.hazardhawk.core.models.*
import kotlin.random.Random

/**
 * Test data factory for LiteRT testing scenarios.
 * Generates mock image data and expected results for comprehensive testing.
 */
class LiteRTTestDataFactory {
    
    companion object {
        private const val TEST_IMAGE_WIDTH = 1920
        private const val TEST_IMAGE_HEIGHT = 1080
        private const val RGB_CHANNELS = 3
        
        // Test image type markers
        private const val PPE_COMPLIANT_MARKER = 0xAA.toByte()
        private const val MISSING_HARDHAT_MARKER = 0xBB.toByte()
        private const val FALL_HAZARD_MARKER = 0xCC.toByte()
        private const val MULTI_HAZARD_MARKER = 0xDD.toByte()
        private const val LOW_CONFIDENCE_MARKER = 0xEE.toByte()
        private const val CORRUPTED_MARKER = 0x00.toByte()
    }
    
    /**
     * Creates a standard test image with realistic size and RGB data.
     */
    fun createStandardTestImage(): ByteArray {
        return createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT)
    }
    
    /**
     * Creates a test image with specified dimensions.
     */
    fun createTestImage(width: Int = TEST_IMAGE_WIDTH, height: Int = TEST_IMAGE_HEIGHT): ByteArray {
        val size = width * height * RGB_CHANNELS
        val imageData = ByteArray(size)
        
        // Generate realistic RGB values
        val random = Random(42) // Fixed seed for reproducible tests
        for (i in imageData.indices) {
            imageData[i] = (128 + random.nextInt(-64, 64)).toByte()
        }
        
        return imageData
    }
    
    /**
     * Creates an image representing a construction site with full PPE compliance.
     */
    fun createPPECompliantImage(): ByteArray {
        val imageData = createTestImage()
        
        // Add marker to identify this as PPE compliant
        if (imageData.isNotEmpty()) {
            imageData[0] = PPE_COMPLIANT_MARKER
            imageData[1] = PPE_COMPLIANT_MARKER
            imageData[2] = PPE_COMPLIANT_MARKER
        }
        
        return imageData
    }
    
    /**
     * Creates an image with a worker missing a hard hat.
     */
    fun createMissingHardHatImage(): ByteArray {
        val imageData = createTestImage()
        
        // Add marker to identify missing hard hat scenario
        if (imageData.isNotEmpty()) {
            imageData[0] = MISSING_HARDHAT_MARKER
            imageData[1] = MISSING_HARDHAT_MARKER
        }
        
        return imageData
    }
    
    /**
     * Creates an image with fall hazards (unguarded edges, heights).
     */
    fun createFallHazardImage(): ByteArray {
        val imageData = createTestImage()
        
        // Add marker for fall hazard scenario
        if (imageData.isNotEmpty()) {
            imageData[0] = FALL_HAZARD_MARKER
        }
        
        return imageData
    }
    
    /**
     * Creates an image with multiple hazards for bounding box testing.
     */
    fun createMultipleHazardsImage(): ByteArray {
        val imageData = createTestImage()
        
        // Add marker for multiple hazards
        if (imageData.isNotEmpty()) {
            imageData[0] = MULTI_HAZARD_MARKER
            imageData[1] = MULTI_HAZARD_MARKER
            imageData[2] = MULTI_HAZARD_MARKER
            imageData[3] = MULTI_HAZARD_MARKER
        }
        
        return imageData
    }
    
    /**
     * Creates an image that should result in low confidence detections.
     */
    fun createLowConfidenceHazardsImage(): ByteArray {
        val imageData = createTestImage(800, 600) // Smaller, lower quality image
        
        // Add noise and low confidence marker
        val random = Random(123)
        for (i in 0 until minOf(100, imageData.size)) {
            imageData[i] = random.nextInt(0, 256).toByte()
        }
        
        if (imageData.isNotEmpty()) {
            imageData[0] = LOW_CONFIDENCE_MARKER
        }
        
        return imageData
    }
    
    /**
     * Creates corrupted image data for error testing.
     */
    fun createCorruptedImageData(): ByteArray {
        return ByteArray(1000) { CORRUPTED_MARKER }
    }
    
    /**
     * Creates large image data for memory testing.
     */
    fun createLargeImageData(width: Int = 8192, height: Int = 8192): ByteArray {
        val size = width * height * RGB_CHANNELS
        val imageData = ByteArray(size)
        
        // Fill with pattern to simulate real image data
        for (i in imageData.indices) {
            imageData[i] = (i % 256).toByte()
        }
        
        return imageData
    }
    
    /**
     * Creates work type specific image data.
     */
    fun createWorkTypeSpecificImage(workType: WorkType): ByteArray {
        val imageData = createTestImage()
        
        // Encode work type in first few bytes
        val workTypeMarker = when (workType) {
            WorkType.ELECTRICAL -> 0x10.toByte()
            WorkType.EXCAVATION -> 0x20.toByte()
            WorkType.ROOFING -> 0x30.toByte()
            WorkType.WELDING -> 0x40.toByte()
            WorkType.CONCRETE -> 0x50.toByte()
            else -> 0x60.toByte()
        }
        
        if (imageData.size >= 10) {
            for (i in 0..9) {
                imageData[i] = workTypeMarker
            }
        }
        
        return imageData
    }
    
    /**
     * Creates an image representing a real construction site photo.
     */
    fun createConstructionSiteImage(): ByteArray {
        val imageData = createTestImage()
        
        // Add realistic construction site patterns
        val random = Random(789)
        
        // Simulate typical construction colors (grays, browns, yellows for equipment)
        for (i in 0 until imageData.size step 3) {
            if (i + 2 < imageData.size) {
                when (random.nextInt(0, 10)) {
                    in 0..3 -> {
                        // Gray concrete/steel
                        imageData[i] = (100 + random.nextInt(0, 50)).toByte()     // R
                        imageData[i + 1] = (100 + random.nextInt(0, 50)).toByte() // G
                        imageData[i + 2] = (100 + random.nextInt(0, 50)).toByte() // B
                    }
                    in 4..6 -> {
                        // Brown dirt/wood
                        imageData[i] = (139 + random.nextInt(-20, 20)).toByte()     // R
                        imageData[i + 1] = (69 + random.nextInt(-20, 20)).toByte()  // G
                        imageData[i + 2] = (19 + random.nextInt(-10, 20)).toByte()  // B
                    }
                    in 7..8 -> {
                        // Yellow equipment/safety gear
                        imageData[i] = (255).toByte()                               // R
                        imageData[i + 1] = (215 + random.nextInt(-20, 20)).toByte() // G
                        imageData[i + 2] = (0 + random.nextInt(0, 30)).toByte()     // B
                    }
                    else -> {
                        // Blue sky or other
                        imageData[i] = (135 + random.nextInt(-30, 30)).toByte()     // R
                        imageData[i + 1] = (206 + random.nextInt(-30, 30)).toByte() // G
                        imageData[i + 2] = (235 + random.nextInt(-20, 20)).toByte() // B
                    }
                }
            }
        }
        
        return imageData
    }
    
    /**
     * Creates expected analysis results for testing validation.
     */
    fun createExpectedAnalysisResult(
        scenario: String,
        processingTime: Long = 1000L,
        backend: LiteRTBackend = LiteRTBackend.CPU
    ): LiteRTAnalysisResult {
        
        return when (scenario) {
            "PPE_COMPLIANT" -> createPPECompliantResult(processingTime, backend)
            "MISSING_HARDHAT" -> createMissingHardHatResult(processingTime, backend)
            "FALL_HAZARD" -> createFallHazardResult(processingTime, backend)
            "MULTIPLE_HAZARDS" -> createMultipleHazardsResult(processingTime, backend)
            else -> createDefaultResult(processingTime, backend)
        }
    }
    
    private fun createPPECompliantResult(
        processingTime: Long,
        backend: LiteRTBackend
    ): LiteRTAnalysisResult {
        return LiteRTAnalysisResult(
            hazards = emptyList(),
            ppeStatus = mapOf(
                PPEType.HARD_HAT to PPEDetection(detected = true, confidence = 0.95f),
                PPEType.SAFETY_VEST to PPEDetection(detected = true, confidence = 0.92f),
                PPEType.SAFETY_BOOTS to PPEDetection(detected = true, confidence = 0.88f)
            ),
            oshaViolations = emptyList(),
            overallRiskAssessment = RiskAssessment(
                overallRisk = Severity.LOW,
                riskScore = 15,
                mitigationPriority = Severity.LOW,
                recommendations = listOf("Continue maintaining excellent safety standards")
            ),
            confidence = 0.94f,
            processingTimeMs = processingTime,
            backendUsed = backend
        )
    }
    
    private fun createMissingHardHatResult(
        processingTime: Long,
        backend: LiteRTBackend
    ): LiteRTAnalysisResult {
        return LiteRTAnalysisResult(
            hazards = listOf(
                DetectedHazard(
                    type = HazardType.PPE_VIOLATION,
                    description = "Worker without required hard hat",
                    severity = Severity.HIGH,
                    confidence = 0.96f,
                    boundingBox = BoundingBox(0.3f, 0.2f, 0.2f, 0.3f, 0.96f),
                    oshaCode = "1926.95",
                    recommendations = listOf("Ensure all workers wear hard hats in construction areas")
                )
            ),
            ppeStatus = mapOf(
                PPEType.HARD_HAT to PPEDetection(detected = false, confidence = 0.96f),
                PPEType.SAFETY_VEST to PPEDetection(detected = true, confidence = 0.89f)
            ),
            oshaViolations = listOf(
                OSHAViolation(
                    regulationCode = "1926.95",
                    description = "Personal protective equipment violation - missing hard hat",
                    severity = Severity.HIGH,
                    recommendation = "Ensure all workers wear appropriate head protection"
                )
            ),
            overallRiskAssessment = RiskAssessment(
                overallRisk = Severity.HIGH,
                riskScore = 85,
                mitigationPriority = Severity.HIGH,
                recommendations = listOf(
                    "Address PPE violation immediately",
                    "Conduct safety briefing on head protection requirements"
                )
            ),
            confidence = 0.89f,
            processingTimeMs = processingTime,
            backendUsed = backend
        )
    }
    
    private fun createFallHazardResult(
        processingTime: Long,
        backend: LiteRTBackend
    ): LiteRTAnalysisResult {
        return LiteRTAnalysisResult(
            hazards = listOf(
                DetectedHazard(
                    type = HazardType.FALL,
                    description = "Unguarded edge presents fall hazard",
                    severity = Severity.HIGH,
                    confidence = 0.93f,
                    boundingBox = BoundingBox(0.1f, 0.6f, 0.8f, 0.3f, 0.93f),
                    oshaCode = "1926.501",
                    recommendations = listOf(
                        "Install guardrail systems",
                        "Use personal fall arrest systems",
                        "Implement safety nets where applicable"
                    )
                )
            ),
            ppeStatus = mapOf(
                PPEType.HARD_HAT to PPEDetection(detected = true, confidence = 0.91f),
                PPEType.SAFETY_VEST to PPEDetection(detected = true, confidence = 0.87f),
                PPEType.SAFETY_HARNESS to PPEDetection(detected = false, confidence = 0.89f)
            ),
            oshaViolations = listOf(
                OSHAViolation(
                    regulationCode = "1926.501",
                    description = "Fall protection violation - unguarded edge above 6 feet",
                    severity = Severity.HIGH,
                    recommendation = "Install compliant fall protection systems"
                )
            ),
            overallRiskAssessment = RiskAssessment(
                overallRisk = Severity.HIGH,
                riskScore = 92,
                mitigationPriority = Severity.HIGH,
                recommendations = listOf(
                    "Immediately address fall hazard",
                    "Restrict access until protection is installed",
                    "Review fall protection program"
                )
            ),
            confidence = 0.91f,
            processingTimeMs = processingTime,
            backendUsed = backend
        )
    }
    
    private fun createMultipleHazardsResult(
        processingTime: Long,
        backend: LiteRTBackend
    ): LiteRTAnalysisResult {
        return LiteRTAnalysisResult(
            hazards = listOf(
                DetectedHazard(
                    type = HazardType.STRUCK_BY,
                    description = "Heavy equipment operating in work area",
                    severity = Severity.MEDIUM,
                    confidence = 0.85f,
                    boundingBox = BoundingBox(0.0f, 0.3f, 0.4f, 0.5f, 0.85f),
                    oshaCode = "1926.95",
                    recommendations = listOf("Establish equipment exclusion zones")
                ),
                DetectedHazard(
                    type = HazardType.ELECTRICAL,
                    description = "Exposed electrical equipment",
                    severity = Severity.HIGH,
                    confidence = 0.78f,
                    boundingBox = BoundingBox(0.6f, 0.1f, 0.3f, 0.2f, 0.78f),
                    oshaCode = "1926.416",
                    recommendations = listOf("De-energize and properly guard electrical equipment")
                )
            ),
            ppeStatus = mapOf(
                PPEType.HARD_HAT to PPEDetection(detected = true, confidence = 0.88f),
                PPEType.SAFETY_VEST to PPEDetection(detected = true, confidence = 0.92f),
                PPEType.SAFETY_BOOTS to PPEDetection(detected = true, confidence = 0.86f)
            ),
            oshaViolations = listOf(
                OSHAViolation(
                    regulationCode = "1926.416",
                    description = "Electrical safety violation - exposed live parts",
                    severity = Severity.HIGH,
                    recommendation = "Guard or insulate electrical equipment"
                )
            ),
            overallRiskAssessment = RiskAssessment(
                overallRisk = Severity.HIGH,
                riskScore = 88,
                mitigationPriority = Severity.HIGH,
                recommendations = listOf(
                    "Address electrical hazard immediately",
                    "Review equipment operation procedures",
                    "Conduct comprehensive safety inspection"
                )
            ),
            confidence = 0.82f,
            processingTimeMs = processingTime,
            backendUsed = backend
        )
    }
    
    private fun createDefaultResult(
        processingTime: Long,
        backend: LiteRTBackend
    ): LiteRTAnalysisResult {
        return LiteRTAnalysisResult(
            hazards = listOf(
                DetectedHazard(
                    type = HazardType.STRUCK_BY,
                    description = "General construction hazard detected",
                    severity = Severity.MEDIUM,
                    confidence = 0.75f,
                    boundingBox = BoundingBox(0.25f, 0.25f, 0.5f, 0.5f, 0.75f)
                )
            ),
            ppeStatus = mapOf(
                PPEType.HARD_HAT to PPEDetection(detected = true, confidence = 0.80f),
                PPEType.SAFETY_VEST to PPEDetection(detected = true, confidence = 0.82f)
            ),
            oshaViolations = emptyList(),
            overallRiskAssessment = RiskAssessment(
                overallRisk = Severity.MEDIUM,
                riskScore = 45,
                mitigationPriority = Severity.MEDIUM,
                recommendations = listOf("Monitor work area for safety compliance")
            ),
            confidence = 0.78f,
            processingTimeMs = processingTime,
            backendUsed = backend
        )
    }
    
    /**
     * Performance test scenarios with different characteristics.
     */
    data class PerformanceTestScenario(
        val name: String,
        val imageSize: Pair<Int, Int>,
        val workType: WorkType,
        val expectedMaxTimeMs: Long
    )
    
    fun getPerformanceTestScenarios(): List<PerformanceTestScenario> {
        return listOf(
            PerformanceTestScenario(
                name = "Small Image - Fast Processing",
                imageSize = 640 to 480,
                workType = WorkType.GENERAL_CONSTRUCTION,
                expectedMaxTimeMs = 500L
            ),
            PerformanceTestScenario(
                name = "Standard HD - Normal Processing", 
                imageSize = 1920 to 1080,
                workType = WorkType.GENERAL_CONSTRUCTION,
                expectedMaxTimeMs = 2000L
            ),
            PerformanceTestScenario(
                name = "High Resolution - Extended Processing",
                imageSize = 3840 to 2160,
                workType = WorkType.ELECTRICAL,
                expectedMaxTimeMs = 5000L
            ),
            PerformanceTestScenario(
                name = "Ultra High Resolution - Maximum Processing",
                imageSize = 7680 to 4320,
                workType = WorkType.ROOFING,
                expectedMaxTimeMs = 10000L
            )
        )
    }
    
    /**
     * Device capability test cases for compatibility testing.
     */
    data class DeviceCapabilityTestCase(
        val deviceName: String,
        val androidVersion: Int,
        val totalMemoryGB: Float,
        val boardInfo: String,
        val expectedOptimalBackend: LiteRTBackend,
        val expectedSupportedBackends: Set<LiteRTBackend>
    )
    
    fun getDeviceCapabilityTestCases(): List<DeviceCapabilityTestCase> {
        return listOf(
            DeviceCapabilityTestCase(
                deviceName = "High-end Flagship (2024)",
                androidVersion = 34,
                totalMemoryGB = 12f,
                boardInfo = "qcom_sm8550",
                expectedOptimalBackend = LiteRTBackend.NPU_QTI_HTP,
                expectedSupportedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL, 
                    LiteRTBackend.NPU_NNAPI,
                    LiteRTBackend.NPU_QTI_HTP
                )
            ),
            DeviceCapabilityTestCase(
                deviceName = "Mid-range Device (2023)",
                androidVersion = 33,
                totalMemoryGB = 6f,
                boardInfo = "qcom_sm7325",
                expectedOptimalBackend = LiteRTBackend.NPU_NNAPI,
                expectedSupportedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI,
                    LiteRTBackend.NPU_QTI_HTP
                )
            ),
            DeviceCapabilityTestCase(
                deviceName = "Budget Device (2022)",
                androidVersion = 31,
                totalMemoryGB = 4f,
                boardInfo = "qcom_sm6225",
                expectedOptimalBackend = LiteRTBackend.GPU_OPENCL,
                expectedSupportedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI,
                    LiteRTBackend.NPU_QTI_HTP
                )
            ),
            DeviceCapabilityTestCase(
                deviceName = "Entry-level Device (2021)",
                androidVersion = 30,
                totalMemoryGB = 2f,
                boardInfo = "qcom_sm4250",
                expectedOptimalBackend = LiteRTBackend.CPU,
                expectedSupportedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI,
                    LiteRTBackend.NPU_QTI_HTP
                )
            ),
            DeviceCapabilityTestCase(
                deviceName = "Samsung Exynos Device",
                androidVersion = 33,
                totalMemoryGB = 8f,
                boardInfo = "exynos2200",
                expectedOptimalBackend = LiteRTBackend.NPU_NNAPI,
                expectedSupportedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI
                    // No QTI HTP support for non-Qualcomm
                )
            ),
            DeviceCapabilityTestCase(
                deviceName = "Legacy Device (Android 7)",
                androidVersion = 24,
                totalMemoryGB = 3f,
                boardInfo = "qcom_msm8998",
                expectedOptimalBackend = LiteRTBackend.GPU_OPENGL,
                expectedSupportedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL
                    // No NNAPI support before Android 8.1
                )
            )
        )
    }
}
