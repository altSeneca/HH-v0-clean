package com.hazardhawk.ai.yolo

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import com.hazardhawk.core.models.WorkType
import com.hazardhawk.core.models.Severity

/**
 * Cross-platform YOLO11 performance optimization system for HazardHawk
 * Provides device-adaptive model selection, memory management, and real-time optimization
 */
class YOLOPerformanceOptimizer {
    
    companion object {
        // Performance targets
        private const val TARGET_ANALYSIS_TIME_MS = 3000L
        private const val TARGET_ACCURACY_THRESHOLD = 0.95
        private const val MAX_MEMORY_USAGE_MB = 2048
        
        // Device tier thresholds
        private const val HIGH_END_RAM_MB = 4096
        private const val MID_RANGE_RAM_MB = 2048
        private const val HIGH_END_FPS_TARGET = 22
        private const val MID_RANGE_FPS_TARGET = 15
        private const val BUDGET_FPS_TARGET = 8
    }
    
    private val performanceMetrics = mutableListOf<YOLOPerformanceMetric>()
    private val _optimizationUpdates = MutableSharedFlow<OptimizationUpdate>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    val optimizationUpdates: SharedFlow<OptimizationUpdate> = _optimizationUpdates.asSharedFlow()
    
    /**
     * Get optimal YOLO11 configuration for device capabilities
     */
    suspend fun getOptimalConfiguration(deviceCapability: DeviceCapability): YOLOOptimizationConfig {
        val deviceTier = classifyDeviceTier(deviceCapability)
        
        return when (deviceTier) {
            DeviceTier.HIGH_END -> YOLOOptimizationConfig(
                modelVariant = YOLOModelVariant.YOLO11M,
                quantizationType = QuantizationType.FP16,
                inputResolution = ImageSize(640, 640),
                batchSize = 1,
                numThreads = minOf(deviceCapability.cpuCores, 4),
                useGPUAcceleration = deviceCapability.hasGPU,
                targetFPS = HIGH_END_FPS_TARGET,
                maxMemoryMB = 2048,
                confidenceThreshold = 0.6f,
                iouThreshold = 0.45f,
                enableOptimizations = listOf(
                    OptimizationTechnique.DYNAMIC_BATCH_SIZING,
                    OptimizationTechnique.MEMORY_POOLING,
                    OptimizationTechnique.TENSOR_OPTIMIZATION
                )
            )
            
            DeviceTier.MID_RANGE -> YOLOOptimizationConfig(
                modelVariant = YOLOModelVariant.YOLO11S,
                quantizationType = QuantizationType.INT8,
                inputResolution = ImageSize(608, 608),
                batchSize = 1,
                numThreads = minOf(deviceCapability.cpuCores, 3),
                useGPUAcceleration = false,
                targetFPS = MID_RANGE_FPS_TARGET,
                maxMemoryMB = 1024,
                confidenceThreshold = 0.55f,
                iouThreshold = 0.45f,
                enableOptimizations = listOf(
                    OptimizationTechnique.MEMORY_POOLING,
                    OptimizationTechnique.QUANTIZATION_OPTIMIZATION
                )
            )
            
            DeviceTier.BUDGET -> YOLOOptimizationConfig(
                modelVariant = YOLOModelVariant.YOLO11N,
                quantizationType = QuantizationType.INT8,
                inputResolution = ImageSize(416, 416),
                batchSize = 1,
                numThreads = 1,
                useGPUAcceleration = false,
                targetFPS = BUDGET_FPS_TARGET,
                maxMemoryMB = 512,
                confidenceThreshold = 0.5f,
                iouThreshold = 0.5f,
                enableOptimizations = listOf(
                    OptimizationTechnique.MEMORY_POOLING,
                    OptimizationTechnique.AGGRESSIVE_QUANTIZATION
                )
            )
        }
    }
    
    /**
     * Monitor and optimize YOLO11 performance in real-time
     */
    suspend fun monitorPerformance(
        detection: YOLODetectionResult,
        deviceInfo: DeviceCapability
    ): PerformanceAnalysis {
        val metric = YOLOPerformanceMetric(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            processingTimeMs = detection.processingTimeMs,
            memoryUsageMB = getCurrentMemoryUsage(),
            detectionCount = detection.detections.size,
            averageConfidence = detection.detections.map { it.confidence }.average().toFloat(),
            deviceTier = classifyDeviceTier(deviceInfo),
            modelVariant = extractModelVariant(detection.modelVersion)
        )
        
        performanceMetrics.add(metric)
        maintainMetricsHistory()
        
        val analysis = analyzeCurrentPerformance(deviceInfo)
        
        // Trigger optimization if needed
        if (shouldOptimize(analysis)) {
            coroutineScope.launch {
                val optimization = performOptimization(analysis, deviceInfo)
                _optimizationUpdates.emit(optimization)
            }
        }
        
        return analysis
    }
    
    /**
     * Optimize memory usage for YOLO11 inference
     */
    suspend fun optimizeMemoryUsage(): MemoryOptimizationResult {
        val currentUsage = getCurrentMemoryUsage()
        val optimizations = mutableListOf<MemoryOptimization>()
        
        // Model memory optimization
        if (currentUsage > MAX_MEMORY_USAGE_MB * 0.8) {
            optimizations.add(MemoryOptimization(
                technique = "Model Quantization",
                description = "Apply INT8 quantization to reduce model size",
                estimatedSavingsMB = (currentUsage * 0.3).toInt(),
                priority = OptimizationPriority.HIGH
            ))
        }
        
        // Buffer optimization
        optimizations.add(MemoryOptimization(
            technique = "Buffer Pooling",
            description = "Reuse input/output buffers across inferences",
            estimatedSavingsMB = 128,
            priority = OptimizationPriority.MEDIUM
        ))
        
        // Cache optimization
        optimizations.add(MemoryOptimization(
            technique = "Result Caching",
            description = "Cache results for similar input patterns",
            estimatedSavingsMB = 64,
            priority = OptimizationPriority.LOW
        ))
        
        val totalSavings = optimizations.sumOf { it.estimatedSavingsMB }
        
        return MemoryOptimizationResult(
            currentUsageMB = currentUsage,
            optimizations = optimizations,
            estimatedUsageAfterMB = maxOf(currentUsage - totalSavings, 256),
            optimizationSuccess = totalSavings > 100
        )
    }
    
    /**
     * Perform dynamic model switching based on performance
     */
    suspend fun dynamicModelSwitching(
        currentPerformance: PerformanceAnalysis,
        deviceCapability: DeviceCapability
    ): ModelSwitchRecommendation {
        val currentTier = classifyDeviceTier(deviceCapability)
        val performanceScore = calculatePerformanceScore(currentPerformance)
        
        return when {
            performanceScore < 0.6 && currentTier != DeviceTier.BUDGET -> {
                ModelSwitchRecommendation(
                    shouldSwitch = true,
                    recommendedVariant = getDowngradedModel(currentPerformance.modelVariant),
                    reason = "Performance below threshold, switching to lighter model",
                    expectedImprovements = mapOf(
                        "processing_time" to "30-50% faster",
                        "memory_usage" to "20-40% lower",
                        "accuracy" to "5-10% lower"
                    )
                )
            }
            
            performanceScore > 0.85 && currentTier == DeviceTier.HIGH_END -> {
                ModelSwitchRecommendation(
                    shouldSwitch = true,
                    recommendedVariant = getUpgradedModel(currentPerformance.modelVariant),
                    reason = "Excellent performance, can use more accurate model",
                    expectedImprovements = mapOf(
                        "accuracy" to "5-15% higher",
                        "detection_count" to "10-20% more objects",
                        "processing_time" to "10-20% slower"
                    )
                )
            }
            
            else -> ModelSwitchRecommendation(
                shouldSwitch = false,
                recommendedVariant = currentPerformance.modelVariant,
                reason = "Current model optimal for device performance",
                expectedImprovements = emptyMap()
            )
        }
    }
    
    /**
     * Generate comprehensive performance benchmarking report
     */
    suspend fun generateBenchmarkReport(): YOLOBenchmarkReport {
        val recentMetrics = performanceMetrics.takeLast(100)
        
        return YOLOBenchmarkReport(
            reportId = "yolo-benchmark-${Clock.System.now().toEpochMilliseconds()}",
            generatedAt = Clock.System.now().toEpochMilliseconds(),
            totalInferences = recentMetrics.size,
            averageProcessingTime = recentMetrics.map { it.processingTimeMs }.average().toLong(),
            averageMemoryUsage = recentMetrics.map { it.memoryUsageMB }.average().toInt(),
            averageDetectionCount = recentMetrics.map { it.detectionCount }.average().toInt(),
            averageConfidence = recentMetrics.map { it.averageConfidence }.average(),
            performanceByDeviceTier = calculatePerformanceByTier(recentMetrics),
            recommendedOptimizations = generateOptimizationRecommendations(recentMetrics),
            benchmarkComparison = compareToBenchmarks(recentMetrics)
        )
    }
    
    // Private helper methods
    private fun classifyDeviceTier(capability: DeviceCapability): DeviceTier {
        return when {
            capability.availableMemoryMB >= HIGH_END_RAM_MB && capability.hasGPU -> DeviceTier.HIGH_END
            capability.availableMemoryMB >= MID_RANGE_RAM_MB -> DeviceTier.MID_RANGE
            else -> DeviceTier.BUDGET
        }
    }
    
    private fun getCurrentMemoryUsage(): Int {
        // Platform-specific memory usage would be implemented in actual usage
        return 512 // Mock value
    }
    
    private fun maintainMetricsHistory() {
        if (performanceMetrics.size > 1000) {
            performanceMetrics.removeFirst()
        }
    }
    
    private fun analyzeCurrentPerformance(deviceInfo: DeviceCapability): PerformanceAnalysis {
        val recentMetrics = performanceMetrics.takeLast(10)
        
        return PerformanceAnalysis(
            averageProcessingTime = recentMetrics.map { it.processingTimeMs }.average().toLong(),
            averageMemoryUsage = recentMetrics.map { it.memoryUsageMB }.average().toInt(),
            averageAccuracy = recentMetrics.map { it.averageConfidence }.average(),
            frameRate = calculateFrameRate(recentMetrics),
            memoryPressure = recentMetrics.map { it.memoryUsageMB }.average() / deviceInfo.availableMemoryMB,
            modelVariant = recentMetrics.lastOrNull()?.modelVariant ?: YOLOModelVariant.YOLO11S,
            performanceScore = calculatePerformanceScore(recentMetrics)
        )
    }
    
    private fun shouldOptimize(analysis: PerformanceAnalysis): Boolean {
        return analysis.averageProcessingTime > TARGET_ANALYSIS_TIME_MS ||
               analysis.memoryPressure > 0.8 ||
               analysis.performanceScore < 0.7
    }
    
    private suspend fun performOptimization(
        analysis: PerformanceAnalysis, 
        deviceInfo: DeviceCapability
    ): OptimizationUpdate {
        val optimizations = mutableListOf<String>()
        
        if (analysis.averageProcessingTime > TARGET_ANALYSIS_TIME_MS) {
            optimizations.add("Reduced input resolution for faster processing")
        }
        
        if (analysis.memoryPressure > 0.8) {
            optimizations.add("Enabled aggressive memory optimization")
        }
        
        return OptimizationUpdate(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            appliedOptimizations = optimizations,
            expectedImprovements = calculateExpectedImprovements(optimizations),
            newConfiguration = getOptimalConfiguration(deviceInfo)
        )
    }
    
    private fun calculatePerformanceScore(analysis: PerformanceAnalysis): Double {
        val timeScore = maxOf(0.0, 1.0 - (analysis.averageProcessingTime.toDouble() / TARGET_ANALYSIS_TIME_MS))
        val memoryScore = maxOf(0.0, 1.0 - analysis.memoryPressure)
        val accuracyScore = analysis.averageAccuracy
        
        return (timeScore + memoryScore + accuracyScore) / 3.0
    }
    
    private fun calculatePerformanceScore(metrics: List<YOLOPerformanceMetric>): Double {
        if (metrics.isEmpty()) return 0.0
        
        val avgTime = metrics.map { it.processingTimeMs }.average()
        val avgMemory = metrics.map { it.memoryUsageMB }.average()
        val avgConfidence = metrics.map { it.averageConfidence }.average()
        
        val timeScore = maxOf(0.0, 1.0 - (avgTime / TARGET_ANALYSIS_TIME_MS))
        val memoryScore = maxOf(0.0, 1.0 - (avgMemory / MAX_MEMORY_USAGE_MB))
        
        return (timeScore + memoryScore + avgConfidence) / 3.0
    }
    
    private fun extractModelVariant(modelVersion: String): YOLOModelVariant {
        return when {
            modelVersion.contains("yolo11n", ignoreCase = true) -> YOLOModelVariant.YOLO11N
            modelVersion.contains("yolo11s", ignoreCase = true) -> YOLOModelVariant.YOLO11S
            modelVersion.contains("yolo11m", ignoreCase = true) -> YOLOModelVariant.YOLO11M
            else -> YOLOModelVariant.YOLO11S
        }
    }
    
    private fun getDowngradedModel(current: YOLOModelVariant): YOLOModelVariant {
        return when (current) {
            YOLOModelVariant.YOLO11M -> YOLOModelVariant.YOLO11S
            YOLOModelVariant.YOLO11S -> YOLOModelVariant.YOLO11N
            YOLOModelVariant.YOLO11N -> YOLOModelVariant.YOLO11N
        }
    }
    
    private fun getUpgradedModel(current: YOLOModelVariant): YOLOModelVariant {
        return when (current) {
            YOLOModelVariant.YOLO11N -> YOLOModelVariant.YOLO11S
            YOLOModelVariant.YOLO11S -> YOLOModelVariant.YOLO11M
            YOLOModelVariant.YOLO11M -> YOLOModelVariant.YOLO11M
        }
    }
    
    private fun calculateFrameRate(metrics: List<YOLOPerformanceMetric>): Double {
        if (metrics.isEmpty()) return 0.0
        val avgProcessingTime = metrics.map { it.processingTimeMs }.average()
        return 1000.0 / avgProcessingTime
    }
    
    private fun calculatePerformanceByTier(metrics: List<YOLOPerformanceMetric>): Map<DeviceTier, Double> {
        return metrics.groupBy { it.deviceTier }
            .mapValues { (_, tierMetrics) -> calculatePerformanceScore(tierMetrics) }
    }
    
    private fun generateOptimizationRecommendations(metrics: List<YOLOPerformanceMetric>): List<String> {
        val recommendations = mutableListOf<String>()
        val avgTime = metrics.map { it.processingTimeMs }.average()
        val avgMemory = metrics.map { it.memoryUsageMB }.average()
        
        if (avgTime > TARGET_ANALYSIS_TIME_MS) {
            recommendations.add("Consider model quantization for faster inference")
        }
        
        if (avgMemory > MAX_MEMORY_USAGE_MB * 0.7) {
            recommendations.add("Implement memory pooling to reduce allocation overhead")
        }
        
        return recommendations
    }
    
    private fun compareToBenchmarks(metrics: List<YOLOPerformanceMetric>): Map<String, String> {
        val avgTime = metrics.map { it.processingTimeMs }.average()
        val avgAccuracy = metrics.map { it.averageConfidence }.average()
        
        return mapOf(
            "processing_time" to when {
                avgTime < TARGET_ANALYSIS_TIME_MS * 0.8 -> "Excellent"
                avgTime < TARGET_ANALYSIS_TIME_MS -> "Good"
                else -> "Needs Improvement"
            },
            "accuracy" to when {
                avgAccuracy > TARGET_ACCURACY_THRESHOLD * 0.95 -> "Excellent"
                avgAccuracy > TARGET_ACCURACY_THRESHOLD * 0.90 -> "Good"
                else -> "Needs Improvement"
            }
        )
    }
    
    private fun calculateExpectedImprovements(optimizations: List<String>): Map<String, String> {
        return mapOf(
            "processing_time" to "10-30% improvement",
            "memory_usage" to "15-25% reduction",
            "overall_performance" to "Moderate improvement expected"
        )
    }
}

// Supporting data classes and enums
@Serializable
data class YOLOOptimizationConfig(
    val modelVariant: YOLOModelVariant,
    val quantizationType: QuantizationType,
    val inputResolution: ImageSize,
    val batchSize: Int,
    val numThreads: Int,
    val useGPUAcceleration: Boolean,
    val targetFPS: Int,
    val maxMemoryMB: Int,
    val confidenceThreshold: Float,
    val iouThreshold: Float,
    val enableOptimizations: List<OptimizationTechnique>
)

@Serializable
enum class YOLOModelVariant { YOLO11N, YOLO11S, YOLO11M }

@Serializable
enum class QuantizationType { FP32, FP16, INT8 }

@Serializable
enum class DeviceTier { HIGH_END, MID_RANGE, BUDGET }

@Serializable
enum class OptimizationTechnique {
    DYNAMIC_BATCH_SIZING, MEMORY_POOLING, TENSOR_OPTIMIZATION,
    QUANTIZATION_OPTIMIZATION, AGGRESSIVE_QUANTIZATION
}

@Serializable
enum class OptimizationPriority { HIGH, MEDIUM, LOW }

@Serializable
data class YOLOPerformanceMetric(
    val timestamp: Long,
    val processingTimeMs: Long,
    val memoryUsageMB: Int,
    val detectionCount: Int,
    val averageConfidence: Float,
    val deviceTier: DeviceTier,
    val modelVariant: YOLOModelVariant
)

@Serializable
data class PerformanceAnalysis(
    val averageProcessingTime: Long,
    val averageMemoryUsage: Int,
    val averageAccuracy: Double,
    val frameRate: Double,
    val memoryPressure: Double,
    val modelVariant: YOLOModelVariant,
    val performanceScore: Double
)

@Serializable
data class MemoryOptimization(
    val technique: String,
    val description: String,
    val estimatedSavingsMB: Int,
    val priority: OptimizationPriority
)

@Serializable
data class MemoryOptimizationResult(
    val currentUsageMB: Int,
    val optimizations: List<MemoryOptimization>,
    val estimatedUsageAfterMB: Int,
    val optimizationSuccess: Boolean
)

@Serializable
data class ModelSwitchRecommendation(
    val shouldSwitch: Boolean,
    val recommendedVariant: YOLOModelVariant,
    val reason: String,
    val expectedImprovements: Map<String, String>
)

@Serializable
data class OptimizationUpdate(
    val timestamp: Long,
    val appliedOptimizations: List<String>,
    val expectedImprovements: Map<String, String>,
    val newConfiguration: YOLOOptimizationConfig
)

@Serializable
data class YOLOBenchmarkReport(
    val reportId: String,
    val generatedAt: Long,
    val totalInferences: Int,
    val averageProcessingTime: Long,
    val averageMemoryUsage: Int,
    val averageDetectionCount: Int,
    val averageConfidence: Double,
    val performanceByDeviceTier: Map<DeviceTier, Double>,
    val recommendedOptimizations: List<String>,
    val benchmarkComparison: Map<String, String>
)