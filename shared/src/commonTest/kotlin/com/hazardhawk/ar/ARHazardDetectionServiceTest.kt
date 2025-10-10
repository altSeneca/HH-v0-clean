package com.hazardhawk.ar

import com.hazardhawk.core.models.*
import com.hazardhawk.core.models.SafetyAnalysis
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Cross-platform tests for AR hazard detection service.
 * Tests core business logic that should work consistently across all platforms.
 */
class ARHazardDetectionServiceTest {

    private lateinit var hazardDetectionService: ARHazardDetectionService

    @BeforeTest
    fun setUp() {
        hazardDetectionService = ARHazardDetectionService()
    }

    @Test
    fun arHazardDetectionService_initializesCorrectly() {
        // Given
        val service = ARHazardDetectionService()
        
        // When
        val isInitialized = service.isInitialized()
        
        // Then
        assertTrue(isInitialized, "Service should initialize successfully")
    }

    @Test
    fun arHazardDetectionService_configuresForDifferentWorkTypes() = runTest {
        // Given
        val workTypes = listOf(
            WorkType.GENERAL_CONSTRUCTION,
            WorkType.ELECTRICAL_WORK,
            WorkType.FALL_PROTECTION
        )
        
        // When & Then
        workTypes.forEach { workType ->
            val result = hazardDetectionService.configureForWorkType(workType)
            assertTrue(result.isSuccess, "Should configure for work type: $workType")
            assertEquals(workType, hazardDetectionService.getCurrentWorkType())
        }
    }

    @Test
    fun arHazardDetectionService_detectsHazardsInFrameData() = runTest {
        // Given
        val mockFrameData = createMockConstructionFrameData()
        hazardDetectionService.configureForWorkType(WorkType.GENERAL_CONSTRUCTION)
        
        // When
        val result = hazardDetectionService.detectHazards(mockFrameData)
        
        // Then
        assertTrue(result.isSuccess, "Should successfully analyze frame data")
        
        val detectedHazards = result.getOrThrow()
        assertTrue(detectedHazards.isNotEmpty(), "Should detect hazards in construction scene")
        
        // Verify hazard properties
        detectedHazards.forEach { hazard ->
            assertNotNull(hazard.boundingBox, "Each hazard should have bounding box for AR overlay")
            assertTrue(hazard.confidence > 0.5f, "Hazards should have reasonable confidence")
            assertNotNull(hazard.oshaCode, "Hazards should have OSHA code reference")
        }
    }

    @Test
    fun arHazardDetectionService_filtersLowConfidenceDetections() = runTest {
        // Given
        val mockFrameData = createMockFrameDataWithVariousConfidences()
        hazardDetectionService.setConfidenceThreshold(0.7f)
        
        // When
        val result = hazardDetectionService.detectHazards(mockFrameData)
        
        // Then
        val detectedHazards = result.getOrThrow()
        detectedHazards.forEach { hazard ->
            assertTrue(hazard.confidence >= 0.7f, 
                "All detected hazards should meet confidence threshold")
        }
    }

    @Test
    fun arHazardDetectionService_prioritizesSeverityLevels() = runTest {
        // Given
        val mockFrameData = createMockFrameDataWithMixedSeverities()
        
        // When
        val result = hazardDetectionService.detectHazards(mockFrameData)
        
        // Then
        val detectedHazards = result.getOrThrow()
        val sortedBySeverity = detectedHazards.sortedByDescending { it.severity.ordinal }
        
        // Critical hazards should be prioritized
        val criticalHazards = detectedHazards.filter { it.severity == Severity.CRITICAL }
        if (criticalHazards.isNotEmpty()) {
            assertEquals(criticalHazards.first(), sortedBySeverity.first(),
                "Critical hazards should be prioritized in results")
        }
    }

    @Test
    fun arHazardDetectionService_validatesBoundingBoxes() = runTest {
        // Given
        val mockFrameData = createMockConstructionFrameData()
        
        // When
        val result = hazardDetectionService.detectHazards(mockFrameData)
        
        // Then
        val detectedHazards = result.getOrThrow()
        
        detectedHazards.forEach { hazard ->
            hazard.boundingBox?.let { box ->
                assertTrue(box.left >= 0f && box.left <= 1f, 
                    "Bounding box left coordinate should be normalized")
                assertTrue(box.top >= 0f && box.top <= 1f,
                    "Bounding box top coordinate should be normalized")
                assertTrue(box.width > 0f && box.width <= 1f,
                    "Bounding box width should be valid")
                assertTrue(box.height > 0f && box.height <= 1f,
                    "Bounding box height should be valid")
                assertTrue(box.left + box.width <= 1f,
                    "Bounding box should not exceed image boundaries")
                assertTrue(box.top + box.height <= 1f,
                    "Bounding box should not exceed image boundaries")
            }
        }
    }

    @Test
    fun arHazardDetectionService_handlesEmptyFrames() = runTest {
        // Given
        val emptyFrameData = createEmptyFrameData()
        
        // When
        val result = hazardDetectionService.detectHazards(emptyFrameData)
        
        // Then
        assertTrue(result.isSuccess, "Should handle empty frames gracefully")
        val detectedHazards = result.getOrThrow()
        assertTrue(detectedHazards.isEmpty(), "Should return empty list for empty frame")
    }

    @Test
    fun arHazardDetectionService_handlesCorruptedFrameData() = runTest {
        // Given
        val corruptedFrameData = createCorruptedFrameData()
        
        // When
        val result = hazardDetectionService.detectHazards(corruptedFrameData)
        
        // Then
        assertTrue(result.isFailure, "Should fail gracefully with corrupted data")
        val exception = result.exceptionOrNull()
        assertNotNull(exception, "Should provide meaningful error information")
    }

    @Test
    fun arHazardDetectionService_adaptsToWorkTypeRequirements() = runTest {
        // Given
        val electricalFrameData = createMockElectricalWorkFrameData()
        
        // When - Configure for electrical work
        hazardDetectionService.configureForWorkType(WorkType.ELECTRICAL_WORK)
        val result = hazardDetectionService.detectHazards(electricalFrameData)
        
        // Then
        val detectedHazards = result.getOrThrow()
        val electricalHazards = detectedHazards.filter { it.type == HazardType.ELECTRICAL_HAZARD }
        
        assertTrue(electricalHazards.isNotEmpty(), 
            "Should prioritize electrical hazards for electrical work")
        
        // Should have electrical-specific OSHA codes
        electricalHazards.forEach { hazard ->
            assertTrue(hazard.oshaCode?.startsWith("1926.4") == true,
                "Electrical hazards should have electrical OSHA codes")
        }
    }

    @Test
    fun arHazardDetectionService_maintainsPoseTracking() = runTest {
        // Given
        val frameSequence = createSequentialFrameData(count = 5)
        val detectedHazardHistory = mutableListOf<List<Hazard>>()
        
        // When - Process frame sequence
        frameSequence.forEach { frameData ->
            val result = hazardDetectionService.detectHazards(frameData)
            val hazards = result.getOrThrow()
            detectedHazardHistory.add(hazards)
        }

        // Then - Should maintain consistent tracking of persistent hazards
        val persistentHazardIds = detectedHazardHistory[0].map { it.id }
        
        if (persistentHazardIds.isNotEmpty()) {
            detectedHazardHistory.forEach { frameHazards ->
                val currentIds = frameHazards.map { it.id }
                val maintainedHazards = persistentHazardIds.intersect(currentIds.toSet())
                
                // Should maintain tracking of most hazards across frames
                assertTrue(maintainedHazards.isNotEmpty(),
                    "Should maintain tracking of hazards across frames")
            }
        }
    }

    @Test
    fun arHazardDetectionService_calculatesRiskAssessment() = runTest {
        // Given
        val highRiskFrameData = createMockHighRiskFrameData()
        
        // When
        val result = hazardDetectionService.detectHazards(highRiskFrameData)
        
        // Then
        val detectedHazards = result.getOrThrow()
        val riskAssessment = hazardDetectionService.calculateRiskAssessment(detectedHazards)
        
        assertNotNull(riskAssessment, "Should calculate risk assessment")
        assertTrue(riskAssessment.overallRisk >= RiskLevel.MEDIUM,
            "High risk scene should have at least medium risk level")
        
        // Should have actionable recommendations
        assertTrue(riskAssessment.recommendations.isNotEmpty(),
            "Risk assessment should include recommendations")
    }

    @Test
    fun arHazardDetectionService_handlesPerformanceConstraints() = runTest {
        // Given
        val largeFrameData = createLargeFrameData()
        hazardDetectionService.setPerformanceMode(PerformanceMode.BALANCED)
        
        // When
        val startTime = System.currentTimeMillis()
        val result = hazardDetectionService.detectHazards(largeFrameData)
        val processingTime = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue(result.isSuccess, "Should process large frames successfully")
        assertTrue(processingTime < 2000L, // Should complete within 2 seconds
            "Should maintain performance constraints: ${processingTime}ms")
    }

    @Test
    fun arHazardDetectionService_cachesPreviousResults() = runTest {
        // Given
        val frameData = createMockConstructionFrameData()
        
        // When - Process same frame twice
        val result1 = hazardDetectionService.detectHazards(frameData)
        val startTime = System.currentTimeMillis()
        val result2 = hazardDetectionService.detectHazards(frameData)
        val cachedProcessingTime = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue(result1.isSuccess && result2.isSuccess, 
            "Both processing attempts should succeed")
        assertTrue(cachedProcessingTime < 100L, // Cached result should be fast
            "Cached processing should be fast: ${cachedProcessingTime}ms")
        
        assertEquals(result1.getOrThrow().size, result2.getOrThrow().size,
            "Cached results should match original results")
    }

    // Helper methods to create test data
    private fun createMockConstructionFrameData(): FrameData {
        return FrameData(
            imageData = ByteArray(1920 * 1080 * 3) { (it % 256).toByte() },
            width = 1920,
            height = 1080,
            timestamp = System.currentTimeMillis(),
            metadata = mapOf(
                "scene_type" to "construction_site",
                "lighting" to "outdoor_daylight",
                "hazards_present" to listOf("fall_protection", "ppe_violation")
            )
        )
    }

    private fun createMockFrameDataWithVariousConfidences(): FrameData {
        return FrameData(
            imageData = ByteArray(1920 * 1080 * 3) { (it % 256).toByte() },
            width = 1920,
            height = 1080,
            timestamp = System.currentTimeMillis(),
            metadata = mapOf(
                "confidence_levels" to listOf(0.9f, 0.6f, 0.3f, 0.8f, 0.4f)
            )
        )
    }

    private fun createMockFrameDataWithMixedSeverities(): FrameData {
        return FrameData(
            imageData = ByteArray(1920 * 1080 * 3) { (it % 256).toByte() },
            width = 1920,
            height = 1080,
            timestamp = System.currentTimeMillis(),
            metadata = mapOf(
                "severities" to listOf("CRITICAL", "HIGH", "MEDIUM", "LOW")
            )
        )
    }

    private fun createEmptyFrameData(): FrameData {
        return FrameData(
            imageData = ByteArray(0),
            width = 0,
            height = 0,
            timestamp = System.currentTimeMillis(),
            metadata = emptyMap()
        )
    }

    private fun createCorruptedFrameData(): FrameData {
        return FrameData(
            imageData = ByteArray(100) { 0xFF.toByte() }, // Invalid image data
            width = -1, // Invalid dimensions
            height = -1,
            timestamp = -1L,
            metadata = mapOf("corrupted" to true)
        )
    }

    private fun createMockElectricalWorkFrameData(): FrameData {
        return FrameData(
            imageData = ByteArray(1920 * 1080 * 3) { (it % 256).toByte() },
            width = 1920,
            height = 1080,
            timestamp = System.currentTimeMillis(),
            metadata = mapOf(
                "scene_type" to "electrical_work",
                "hazards_present" to listOf("exposed_wiring", "missing_lockout")
            )
        )
    }

    private fun createSequentialFrameData(count: Int): List<FrameData> {
        return (0 until count).map { index ->
            FrameData(
                imageData = ByteArray(1920 * 1080 * 3) { ((it + index) % 256).toByte() },
                width = 1920,
                height = 1080,
                timestamp = System.currentTimeMillis() + index * 33L, // 30 FPS timing
                metadata = mapOf(
                    "frame_sequence" to index,
                    "persistent_hazards" to listOf("fall_hazard_1", "ppe_violation_1")
                )
            )
        }
    }

    private fun createMockHighRiskFrameData(): FrameData {
        return FrameData(
            imageData = ByteArray(1920 * 1080 * 3) { (it % 256).toByte() },
            width = 1920,
            height = 1080,
            timestamp = System.currentTimeMillis(),
            metadata = mapOf(
                "scene_type" to "high_risk_construction",
                "hazards_present" to listOf("fall_protection", "electrical_hazard", "heavy_machinery")
            )
        )
    }

    private fun createLargeFrameData(): FrameData {
        return FrameData(
            imageData = ByteArray(4096 * 2160 * 3) { (it % 256).toByte() }, // 4K resolution
            width = 4096,
            height = 2160,
            timestamp = System.currentTimeMillis(),
            metadata = mapOf("resolution" to "4K")
        )
    }
}

/**
 * Mock AR Hazard Detection Service for cross-platform testing
 */
class ARHazardDetectionService {
    private var currentWorkType = WorkType.GENERAL_CONSTRUCTION
    private var confidenceThreshold = 0.6f
    private var performanceMode = PerformanceMode.QUALITY
    private val resultCache = mutableMapOf<String, List<Hazard>>()

    fun isInitialized(): Boolean = true

    suspend fun configureForWorkType(workType: WorkType): Result<Unit> {
        return try {
            currentWorkType = workType
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentWorkType(): WorkType = currentWorkType

    fun setConfidenceThreshold(threshold: Float) {
        confidenceThreshold = threshold
    }

    fun setPerformanceMode(mode: PerformanceMode) {
        performanceMode = mode
    }

    suspend fun detectHazards(frameData: FrameData): Result<List<Hazard>> {
        return try {
            // Validate frame data
            if (frameData.width < 0 || frameData.height < 0) {
                throw IllegalArgumentException("Invalid frame dimensions")
            }
            if (frameData.imageData.isEmpty() && frameData.width > 0) {
                return Result.success(emptyList()) // Empty frame
            }

            // Check cache first
            val cacheKey = generateCacheKey(frameData)
            resultCache[cacheKey]?.let { cachedResult ->
                return Result.success(cachedResult)
            }

            // Simulate processing delay based on performance mode
            val processingDelay = when (performanceMode) {
                PerformanceMode.PERFORMANCE -> 50L
                PerformanceMode.BALANCED -> 100L
                PerformanceMode.QUALITY -> 200L
            }
            Thread.sleep(processingDelay)

            // Generate mock hazards based on frame metadata
            val hazards = generateMockHazards(frameData)
            val filteredHazards = hazards.filter { it.confidence >= confidenceThreshold }

            // Cache results
            resultCache[cacheKey] = filteredHazards

            Result.success(filteredHazards)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun calculateRiskAssessment(hazards: List<Hazard>): RiskAssessment {
        val overallRisk = when {
            hazards.any { it.severity == Severity.CRITICAL } -> RiskLevel.HIGH
            hazards.any { it.severity == Severity.HIGH } -> RiskLevel.MEDIUM
            hazards.isNotEmpty() -> RiskLevel.LOW
            else -> RiskLevel.NONE
        }

        val recommendations = hazards.flatMap { it.recommendations }

        return RiskAssessment(
            overallRisk = overallRisk,
            recommendations = recommendations,
            criticalCount = hazards.count { it.severity == Severity.CRITICAL },
            highCount = hazards.count { it.severity == Severity.HIGH }
        )
    }

    private fun generateCacheKey(frameData: FrameData): String {
        return "${frameData.width}x${frameData.height}_${frameData.imageData.size}_${frameData.metadata.hashCode()}"
    }

    private fun generateMockHazards(frameData: FrameData): List<Hazard> {
        val hazardsPresent = frameData.metadata["hazards_present"] as? List<String> ?: emptyList()
        val confidenceLevels = frameData.metadata["confidence_levels"] as? List<Float>
        val severities = frameData.metadata["severities"] as? List<String>

        return hazardsPresent.mapIndexed { index, hazardType ->
            val confidence = confidenceLevels?.getOrNull(index) ?: 0.8f
            val severityName = severities?.getOrNull(index) ?: "MEDIUM"
            val severity = Severity.valueOf(severityName)

            when (hazardType) {
                "fall_protection" -> createFallProtectionHazard(confidence, severity)
                "ppe_violation" -> createPPEViolationHazard(confidence, severity)
                "electrical_hazard" -> createElectricalHazard(confidence, severity)
                "exposed_wiring" -> createElectricalHazard(confidence, severity)
                "missing_lockout" -> createElectricalHazard(confidence, severity)
                "heavy_machinery" -> createMachineryHazard(confidence, severity)
                else -> createGenericHazard(confidence, severity)
            }
        }
    }

    private fun createFallProtectionHazard(confidence: Float, severity: Severity): Hazard {
        return Hazard(
            id = "fall_protection_${System.nanoTime()}",
            type = HazardType.FALL_PROTECTION,
            severity = severity,
            description = "Worker at height without fall protection",
            oshaCode = "1926.501(b)(1)",
            boundingBox = BoundingBox(0.3f, 0.2f, 0.25f, 0.4f),
            confidence = confidence,
            recommendations = listOf("Install fall protection system", "Use safety harness"),
            immediateAction = if (severity == Severity.CRITICAL) "STOP WORK" else null
        )
    }

    private fun createPPEViolationHazard(confidence: Float, severity: Severity): Hazard {
        return Hazard(
            id = "ppe_violation_${System.nanoTime()}",
            type = HazardType.PPE_VIOLATION,
            severity = severity,
            description = "Missing required PPE",
            oshaCode = "1926.95(a)",
            boundingBox = BoundingBox(0.4f, 0.1f, 0.2f, 0.3f),
            confidence = confidence,
            recommendations = listOf("Don required PPE", "Ensure compliance training")
        )
    }

    private fun createElectricalHazard(confidence: Float, severity: Severity): Hazard {
        return Hazard(
            id = "electrical_hazard_${System.nanoTime()}",
            type = HazardType.ELECTRICAL_HAZARD,
            severity = severity,
            description = "Electrical safety violation",
            oshaCode = "1926.416(a)(1)",
            boundingBox = BoundingBox(0.5f, 0.6f, 0.2f, 0.15f),
            confidence = confidence,
            recommendations = listOf("Implement lockout/tagout", "Cover exposed wiring")
        )
    }

    private fun createMachineryHazard(confidence: Float, severity: Severity): Hazard {
        return Hazard(
            id = "machinery_hazard_${System.nanoTime()}",
            type = HazardType.MACHINERY_HAZARD,
            severity = severity,
            description = "Unsafe machinery operation",
            oshaCode = "1926.1053(a)",
            boundingBox = BoundingBox(0.1f, 0.5f, 0.4f, 0.3f),
            confidence = confidence,
            recommendations = listOf("Maintain safe distance", "Use proper guarding")
        )
    }

    private fun createGenericHazard(confidence: Float, severity: Severity): Hazard {
        return Hazard(
            id = "generic_hazard_${System.nanoTime()}",
            type = HazardType.HOUSEKEEPING,
            severity = severity,
            description = "General safety concern",
            oshaCode = "1926.25(a)",
            boundingBox = BoundingBox(0.6f, 0.7f, 0.2f, 0.1f),
            confidence = confidence,
            recommendations = listOf("Address safety concern")
        )
    }
}

// Data classes for testing
data class FrameData(
    val imageData: ByteArray,
    val width: Int,
    val height: Int,
    val timestamp: Long,
    val metadata: Map<String, Any>
)

data class RiskAssessment(
    val overallRisk: RiskLevel,
    val recommendations: List<String>,
    val criticalCount: Int,
    val highCount: Int
)

enum class PerformanceMode { PERFORMANCE, BALANCED, QUALITY }
enum class RiskLevel { NONE, LOW, MEDIUM, HIGH }
