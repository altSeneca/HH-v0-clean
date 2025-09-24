package com.hazardhawk.ai.mock

import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.ai.core.AIServiceFactory
import com.hazardhawk.ai.litert.*
import com.hazardhawk.ai.services.LiteRTVisionService
import com.hazardhawk.performance.*
import com.hazardhawk.core.models.*

/**
 * Mock implementations of AI components for unit testing.
 * These mocks provide predictable responses without requiring actual AI processing.
 */

/**
 * Mock LiteRT Model Engine that simulates successful operations.
 */
class MockLiteRTModelEngine : LiteRTModelEngine() {
    override val isAvailable: Boolean = true
    override val supportedBackends: Set<LiteRTBackend> = setOf(
        LiteRTBackend.CPU,
        LiteRTBackend.GPU_OPENGL,
        LiteRTBackend.NPU_NNAPI
    )
    override val currentBackend: LiteRTBackend? = LiteRTBackend.CPU
    
    override suspend fun initialize(
        modelPath: String,
        backend: LiteRTBackend
    ): Result<Unit> = Result.success(Unit)
    
    override suspend fun generateSafetyAnalysis(
        imageData: ByteArray,
        workType: WorkType,
        includeOSHACodes: Boolean,
        confidenceThreshold: Float
    ): Result<LiteRTAnalysisResult> = Result.success(
        LiteRTAnalysisResult(
            hazards = listOf(
                DetectedHazard(
                    type = HazardType.FALL_PROTECTION,
                    description = "Mock fall hazard detected",
                    severity = Severity.HIGH,
                    confidence = 0.85f,
                    boundingBox = BoundingBox(0.1f, 0.2f, 0.3f, 0.4f, 0.85f),
                    oshaCode = "1926.501",
                    recommendations = listOf("Install guardrails")
                )
            ),
            ppeStatus = mapOf(
                PPEType.HARD_HAT to PPEDetection(
                    isPresent = true,
                    confidence = 0.9f,
                    boundingBox = BoundingBox(0.45f, 0.1f, 0.1f, 0.15f, 0.9f)
                )
            ),
            oshaViolations = emptyList(),
            overallRiskAssessment = RiskAssessment(
                level = RiskLevel.MEDIUM,
                score = 65,
                factors = listOf("Fall protection required")
            ),
            confidence = 0.85f,
            processingTimeMs = 150L,
            backendUsed = LiteRTBackend.CPU,
            debugInfo = LiteRTDebugInfo(
                modelVersion = "mock-v1.0",
                inputPreprocessingTime = 10L,
                inferenceTime = 100L,
                postProcessingTime = 40L,
                memoryPeakMB = 256f,
                deviceTemperature = 35f
            )
        )
    )
    
    override fun getPerformanceMetrics(): LiteRTPerformanceMetrics = LiteRTPerformanceMetrics(
        analysisCount = 10L,
        averageProcessingTimeMs = 150L,
        tokensPerSecond = 243f,
        peakMemoryUsageMB = 256f,
        averageMemoryUsageMB = 200f,
        successRate = 1.0f,
        preferredBackend = LiteRTBackend.CPU,
        thermalThrottlingDetected = false
    )
    
    override fun cleanup() {
        // Mock cleanup - no actual resources to release
    }
}

/**
 * Mock LiteRT Device Optimizer that provides predictable device analysis.
 */
class MockLiteRTDeviceOptimizer(deviceTierDetector: DeviceTierDetector, modelEngine: LiteRTModelEngine) : 
    LiteRTDeviceOptimizer(deviceTierDetector, modelEngine) {
    
    override suspend fun selectOptimalBackend(forceRecalculate: Boolean): LiteRTBackend = 
        LiteRTBackend.CPU
    
    override fun getPerformanceRecommendations(): List<PerformanceRecommendation> = listOf(
        PerformanceRecommendation(
            title = "Mock Recommendation",
            description = "This is a mock performance recommendation for testing",
            priority = Priority.MEDIUM,
            actionable = true
        )
    )
    
    override suspend fun validateBackend(backend: LiteRTBackend): BackendValidationResult = 
        BackendValidationResult(
            isSupported = true,
            expectedPerformance = 243f,
            actualPerformance = 243f,
            memoryUsage = 200f,
            errorMessage = null
        )
}

/**
 * Mock LiteRT Vision Service for testing.
 */
class MockLiteRTVisionService(liteRTEngine: LiteRTModelEngine, deviceOptimizer: LiteRTDeviceOptimizer) : 
    LiteRTVisionService(liteRTEngine, deviceOptimizer) {
    
    override suspend fun analyzePhoto(imageData: ByteArray, workType: WorkType): Result<SafetyAnalysis> =
        Result.success(
            SafetyAnalysis(
                id = "mock-analysis-123",
                hazards = listOf(
                    DetectedHazard(
                        type = HazardType.ELECTRICAL,
                        description = "Mock electrical hazard",
                        severity = Severity.HIGH,
                        confidence = 0.88f,
                        boundingBox = null,
                        oshaCode = "1926.95",
                        recommendations = listOf("Use proper electrical PPE")
                    )
                ),
                overallRisk = RiskLevel.HIGH,
                confidence = 0.88f,
                processingTimeMs = 200L,
                aiProvider = "MockLiteRT"
            )
        )
}

/**
 * Mock AI Service Factory for testing.
 */
class MockAIServiceFactory : AIServiceFactory() {
    
    override fun createOrchestrator(
        networkMonitor: NetworkConnectivityService,
        performanceManager: AdaptivePerformanceManager,
        memoryManager: MemoryManager,
        performanceMonitor: PerformanceMonitor,
        deviceTierDetector: DeviceTierDetector
    ): AIPhotoAnalyzer = MockAIPhotoAnalyzer()
}

/**
 * Mock AI Photo Analyzer for testing.
 */
class MockAIPhotoAnalyzer : AIPhotoAnalyzer {
    override val analyzerName: String = "MockAIAnalyzer"
    override val analysisCapabilities: Set<AnalysisCapability> = setOf(
        AnalysisCapability.HAZARD_DETECTION,
        AnalysisCapability.PPE_DETECTION,
        AnalysisCapability.OSHA_COMPLIANCE
    )
    
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType,
        analysisOptions: AnalysisOptions
    ): Result<SafetyAnalysis> = Result.success(
        SafetyAnalysis(
            id = "mock-analysis-456",
            hazards = listOf(
                DetectedHazard(
                    type = HazardType.STRUCK_BY,
                    description = "Mock struck-by hazard",
                    severity = Severity.MEDIUM,
                    confidence = 0.75f,
                    boundingBox = null,
                    recommendations = listOf("Maintain safe distance from equipment")
                )
            ),
            overallRisk = RiskLevel.MEDIUM,
            confidence = 0.75f,
            processingTimeMs = 180L,
            aiProvider = "MockAI"
        )
    )
    
    override fun getPerformanceMetrics(): AnalyzerPerformanceMetrics = AnalyzerPerformanceMetrics(
        analysisCount = 5L,
        averageProcessingTime = 180L,
        successRate = 1.0f,
        averageConfidence = 0.8f
    )
    
    override fun cleanup() {
        // Mock cleanup
    }
}

// Mock Performance Components
class MockNetworkConnectivityService : NetworkConnectivityService {
    override suspend fun isConnected(): Boolean = true
    override suspend fun getConnectionQuality(): ConnectionQuality = ConnectionQuality.HIGH
}

class MockAdaptivePerformanceManager : AdaptivePerformanceManager {
    override fun getCurrentPerformanceLevel(): PerformanceLevel = PerformanceLevel.HIGH
    override suspend fun optimizeForTask(task: PerformanceTask): PerformanceOptimization =
        PerformanceOptimization(recommended = true, changes = emptyList())
}

class MockMemoryManager : MemoryManager {
    override fun getAvailableMemoryMB(): Float = 2048f
    override fun getTotalMemoryMB(): Float = 4096f
    override suspend fun requestMemoryCleanup(): Boolean = true
}

class MockPerformanceMonitor : PerformanceMonitor {
    override fun startMonitoring(taskId: String) {}
    override fun stopMonitoring(taskId: String): PerformanceReport = PerformanceReport(
        taskId = taskId,
        duration = 150L,
        memoryUsage = 256f,
        cpuUsage = 25f
    )
}

class MockDeviceTierDetector : DeviceTierDetector {
    override fun detectDeviceTier(): DeviceTier = DeviceTier.MID_RANGE
    override fun getMemoryInfo(): MemoryInfo = MemoryInfo(
        totalMemoryMB = 4096f,
        availableMemoryMB = 2048f
    )
    override fun getCurrentThermalState(): ThermalState = ThermalState.NORMAL
}
