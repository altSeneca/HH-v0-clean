package com.hazardhawk

import com.hazardhawk.ai.core.NetworkConnectivityService
import com.hazardhawk.ai.core.ConnectionQuality
import com.hazardhawk.ai.models.*
import com.hazardhawk.ai.core.AIPhotoAnalyzer
import kotlinx.coroutines.delay
import kotlinx.uuid.uuid4
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Test utilities and mock implementations for HazardHawk testing framework.
 */
object TestUtils {
    
    /**
     * Measures execution time of a suspend function.
     */
    suspend fun <T> measureExecutionTime(block: suspend () -> T): Pair<T, Duration> {
        val result: T
        val duration = measureTime {
            result = block()
        }
        return result to duration
    }
    
    /**
     * Asserts that execution time is within acceptable bounds.
     */
    fun assertPerformanceWithin(
        actualMs: Long,
        expectedMs: Long,
        tolerancePercent: Double = 20.0,
        scenario: String = ""
    ) {
        val tolerance = (expectedMs * tolerancePercent / 100).toLong()
        val minAcceptable = expectedMs - tolerance
        val maxAcceptable = expectedMs + tolerance
        
        if (actualMs !in minAcceptable..maxAcceptable) {
            throw AssertionError(
                "Performance assertion failed for $scenario: " +
                "Expected ${expectedMs}ms ±${tolerancePercent}% " +
                "(${minAcceptable}-${maxAcceptable}ms), but got ${actualMs}ms"
            )
        }
    }
    
    /**
     * Simulates realistic AI processing delays based on analysis type.
     */
    suspend fun simulateAIProcessing(analysisType: AnalysisType, imageSize: Int = 1920 * 1080) {
        val baseDelay = when (analysisType) {
            AnalysisType.LOCAL_GEMMA_MULTIMODAL -> 2000L
            AnalysisType.CLOUD_GEMINI -> 3000L 
            AnalysisType.LOCAL_YOLO_FALLBACK -> 800L
            AnalysisType.HYBRID_ANALYSIS -> 2500L
        }
        
        // Scale delay based on image size
        val scaleFactor = imageSize.toDouble() / (1920 * 1080)
        val adjustedDelay = (baseDelay * scaleFactor).toLong()
        
        delay(adjustedDelay)
    }
    
    /**
     * Validates that a SafetyAnalysis object contains all required fields.
     */
    fun validateSafetyAnalysis(analysis: SafetyAnalysis): List<String> {
        val errors = mutableListOf<String>()
        
        if (analysis.id.isEmpty()) errors.add("ID cannot be empty")
        if (analysis.timestamp <= 0) errors.add("Timestamp must be positive")
        if (analysis.confidence < 0 || analysis.confidence > 1) {
            errors.add("Confidence must be between 0 and 1, got ${analysis.confidence}")
        }
        if (analysis.processingTimeMs < 0) errors.add("Processing time cannot be negative")
        
        // Validate hazards
        analysis.hazards.forEachIndexed { index, hazard ->
            if (hazard.id.isEmpty()) errors.add("Hazard $index: ID cannot be empty")
            if (hazard.confidence < 0 || hazard.confidence > 1) {
                errors.add("Hazard $index: Confidence must be between 0 and 1")
            }
            if (hazard.description.isEmpty()) errors.add("Hazard $index: Description cannot be empty")
        }
        
        // Validate bounding boxes
        analysis.hazards.forEach { hazard ->
            hazard.boundingBox?.let { box ->
                if (box.left < 0 || box.left > 1) errors.add("BoundingBox left must be 0-1")
                if (box.top < 0 || box.top > 1) errors.add("BoundingBox top must be 0-1")  
                if (box.width <= 0 || box.width > 1) errors.add("BoundingBox width must be 0-1")
                if (box.height <= 0 || box.height > 1) errors.add("BoundingBox height must be 0-1")
                if (box.right > 1) errors.add("BoundingBox extends beyond right edge")
                if (box.bottom > 1) errors.add("BoundingBox extends beyond bottom edge")
            }
        }
        
        return errors
    }
    
    /**
     * Creates a consistent test environment setup.
     */
    fun setupTestEnvironment(): TestEnvironment {
        return TestEnvironment(
            isNetworkConnected = true,
            connectionQuality = ConnectionQuality.GOOD,
            gemmaAvailable = true,
            vertexAIAvailable = true,
            yoloAvailable = true
        )
    }
}

/**
 * Test environment configuration.
 */
data class TestEnvironment(
    val isNetworkConnected: Boolean,
    val connectionQuality: ConnectionQuality,
    val gemmaAvailable: Boolean,
    val vertexAIAvailable: Boolean,
    val yoloAvailable: Boolean
)

/**
 * Mock network connectivity service for testing.
 */
class MockNetworkConnectivityService(
    override val isConnected: Boolean = true,
    override val connectionQuality: ConnectionQuality = ConnectionQuality.GOOD
) : NetworkConnectivityService

/**
 * Mock AI photo analyzer for testing.
 */
class MockAIPhotoAnalyzer(
    override val analyzerName: String = "Mock AI Analyzer",
    override val priority: Int = 100,
    override val analysisCapabilities: Set<AnalysisCapability> = setOf(
        AnalysisCapability.MULTIMODAL_VISION,
        AnalysisCapability.HAZARD_IDENTIFICATION
    ),
    override val isAvailable: Boolean = true,
    private val responseDelay: Long = 1000L,
    private val shouldSucceed: Boolean = true,
    private val customAnalysis: SafetyAnalysis? = null
) : AIPhotoAnalyzer {
    
    private var configurationResult: Result<Unit> = Result.success(Unit)
    
    override suspend fun configure(apiKey: String?): Result<Unit> {
        delay(100) // Simulate configuration time
        return configurationResult
    }
    
    override suspend fun analyzePhoto(
        imageData: ByteArray, 
        workType: WorkType
    ): Result<SafetyAnalysis> {
        delay(responseDelay)
        
        return if (shouldSucceed) {
            val analysis = customAnalysis ?: TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
                processingTime = responseDelay
            )
            Result.success(analysis)
        } else {
            Result.failure(Exception("Mock analyzer failure"))
        }
    }
    
    fun setConfigurationResult(result: Result<Unit>) {
        configurationResult = result
    }
}

/**
 * Performance test runner for AI components.
 */
class PerformanceTestRunner {
    
    data class PerformanceResult(
        val scenario: String,
        val executionTimeMs: Long,
        val fps: Double,
        val success: Boolean,
        val errorMessage: String? = null
    )
    
    suspend fun runPerformanceTest(
        analyzer: AIPhotoAnalyzer,
        scenario: TestDataFactory.PerformanceTestScenario
    ): PerformanceResult {
        val imageData = TestDataFactory.createMockImageData(
            scenario.imageSize.first, 
            scenario.imageSize.second
        )
        
        return try {
            val (result, duration) = TestUtils.measureExecutionTime {
                analyzer.analyzePhoto(imageData, WorkType.GENERAL_CONSTRUCTION)
            }
            
            val executionTime = duration.inWholeMilliseconds
            val fps = if (executionTime > 0) 1000.0 / executionTime else 0.0
            
            PerformanceResult(
                scenario = scenario.name,
                executionTimeMs = executionTime,
                fps = fps,
                success = result.isSuccess,
                errorMessage = result.exceptionOrNull()?.message
            )
        } catch (e: Exception) {
            PerformanceResult(
                scenario = scenario.name,
                executionTimeMs = -1,
                fps = 0.0,
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    suspend fun runBatchPerformanceTest(
        analyzer: AIPhotoAnalyzer,
        batchSize: Int = 10,
        concurrency: Int = 3
    ): List<PerformanceResult> {
        val results = mutableListOf<PerformanceResult>()
        val imageData = TestDataFactory.createMockImageData()
        
        repeat(batchSize) { index ->
            val (result, duration) = TestUtils.measureExecutionTime {
                analyzer.analyzePhoto(imageData, WorkType.GENERAL_CONSTRUCTION)
            }
            
            results.add(PerformanceResult(
                scenario = "Batch Test $index",
                executionTimeMs = duration.inWholeMilliseconds,
                fps = 1000.0 / duration.inWholeMilliseconds,
                success = result.isSuccess,
                errorMessage = result.exceptionOrNull()?.message
            ))
        }
        
        return results
    }
}
