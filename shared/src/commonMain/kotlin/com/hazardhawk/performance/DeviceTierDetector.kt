package com.hazardhawk.performance

import kotlin.math.roundToInt
import kotlinx.datetime.Clock

/**
 * Device tier classification for performance optimization.
 */
enum class DeviceTier(
    val displayName: String,
    val maxMemoryMB: Int,
    val recommendedConcurrency: Int,
    val maxModelSizeMB: Int,
    val targetFPS: Int,
    val aiProcessingFPS: Float
) {
    LOW_END(
        displayName = "Low-End Device",
        maxMemoryMB = 2048,
        recommendedConcurrency = 1,
        maxModelSizeMB = 50,
        targetFPS = 24,
        aiProcessingFPS = 1.0f
    ),
    
    MID_RANGE(
        displayName = "Mid-Range Device", 
        maxMemoryMB = 6144,
        recommendedConcurrency = 2,
        maxModelSizeMB = 150,
        targetFPS = 30,
        aiProcessingFPS = 1.5f
    ),
    
    HIGH_END(
        displayName = "High-End Device",
        maxMemoryMB = Int.MAX_VALUE,
        recommendedConcurrency = 3,
        maxModelSizeMB = 500,
        targetFPS = 60,
        aiProcessingFPS = 2.0f
    )
}

/**
 * Device capabilities and performance characteristics.
 */
data class DeviceCapabilities(
    val tier: DeviceTier,
    val totalMemoryMB: Int,
    val availableMemoryMB: Int,
    val cpuCores: Int,
    val hasGPU: Boolean,
    val hasNNAPI: Boolean,
    val batteryOptimized: Boolean,
    val thermalThrottled: Boolean,
    val screenDensity: Float,
    val androidVersion: Int
) {
    val memoryPressure: MemoryPressure
        get() = when {
            availableMemoryMB < totalMemoryMB * 0.1f -> MemoryPressure.CRITICAL
            availableMemoryMB < totalMemoryMB * 0.2f -> MemoryPressure.HIGH
            availableMemoryMB < totalMemoryMB * 0.4f -> MemoryPressure.MODERATE
            else -> MemoryPressure.LOW
        }
    
    val recommendedModelComplexity: ModelComplexity
        get() = when {
            tier == DeviceTier.LOW_END || memoryPressure == MemoryPressure.CRITICAL -> ModelComplexity.BASIC
            tier == DeviceTier.MID_RANGE || memoryPressure == MemoryPressure.HIGH -> ModelComplexity.STANDARD
            else -> ModelComplexity.ADVANCED
        }
}

enum class MemoryPressure {
    LOW, MODERATE, HIGH, CRITICAL
}

enum class ModelComplexity {
    BASIC,      // YOLO11 only
    STANDARD,   // YOLO11 + lightweight processing
    ADVANCED    // Full Gemma + all features
}

/**
 * Performance configuration based on device capabilities.
 */
data class PerformanceConfig(
    val deviceTier: DeviceTier,
    val maxConcurrentAnalyses: Int,
    val aiProcessingIntervalMs: Long,
    val uiTargetFPS: Int,
    val enableModelPreloading: Boolean,
    val enableResultCaching: Boolean,
    val maxCacheSize: Int,
    val memoryThresholdMB: Int,
    val enableThermalThrottling: Boolean,
    val useLowPowerMode: Boolean
) {
    companion object {
        fun fromCapabilities(capabilities: DeviceCapabilities): PerformanceConfig {
            val tier = capabilities.tier
            val intervalMs = (1000f / tier.aiProcessingFPS).roundToInt().toLong()
            
            return PerformanceConfig(
                deviceTier = tier,
                maxConcurrentAnalyses = if (capabilities.thermalThrottled) 1 else tier.recommendedConcurrency,
                aiProcessingIntervalMs = intervalMs,
                uiTargetFPS = if (capabilities.batteryOptimized) tier.targetFPS * 2/3 else tier.targetFPS,
                enableModelPreloading = tier != DeviceTier.LOW_END && !capabilities.batteryOptimized,
                enableResultCaching = true,
                maxCacheSize = when (tier) {
                    DeviceTier.LOW_END -> 10
                    DeviceTier.MID_RANGE -> 25
                    DeviceTier.HIGH_END -> 50
                },
                memoryThresholdMB = tier.maxMemoryMB / 4,
                enableThermalThrottling = true,
                useLowPowerMode = capabilities.batteryOptimized
            )
        }
    }
}

/**
 * Cross-platform device tier detection interface.
 */
expect class DeviceTierDetector {
    suspend fun detectCapabilities(): DeviceCapabilities
    suspend fun getCurrentMemoryUsage(): Long
    suspend fun getAvailableMemory(): Long
    suspend fun getCPUCoreCount(): Int
    suspend fun hasGPUAcceleration(): Boolean
    suspend fun hasNNAPISupport(): Boolean
    suspend fun isBatteryOptimized(): Boolean
    suspend fun isThermalThrottled(): Boolean
    suspend fun getScreenDensity(): Float
    suspend fun getAndroidVersion(): Int
    
    // New methods for LiteRTDeviceOptimizer compatibility
    suspend fun detectDeviceTier(): DeviceTier
    suspend fun getCurrentThermalState(): ThermalState
    suspend fun getMemoryInfo(): MemoryInfo
}

/**
 * Performance metrics for monitoring and optimization.
 */
data class PerformanceMetrics(
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val memoryUsedMB: Float,
    val availableMemoryMB: Float,
    val cpuUsagePercent: Float,
    val gpuUsagePercent: Float,
    val currentFPS: Float,
    val aiProcessingFPS: Float,
    val modelLoadTimeMs: Long,
    val analysisTimeMs: Long,
    val cacheHitRate: Float,
    val thermalState: ThermalState,
    val batteryLevel: Float
)

enum class ThermalState {
    NOMINAL,
    LIGHT_THROTTLING,
    MODERATE_THROTTLING,
    SEVERE_THROTTLING,
    CRITICAL_THROTTLING
}

/**
 * Adaptive performance manager that adjusts based on real-time conditions.
 */
class AdaptivePerformanceManager(
    private val detector: DeviceTierDetector,
    private val initialConfig: PerformanceConfig
) {
    private var currentConfig = initialConfig
    private val metricsHistory = mutableListOf<PerformanceMetrics>()
    private var lastAdaptation = 0L
    
    suspend fun getCurrentConfig(): PerformanceConfig {
        // Adapt configuration every 30 seconds at most
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastAdaptation > 30_000) {
            adaptConfiguration()
            lastAdaptation = now
        }
        return currentConfig
    }
    
    private suspend fun adaptConfiguration() {
        val capabilities = detector.detectCapabilities()
        val baseConfig = PerformanceConfig.fromCapabilities(capabilities)
        
        // Analyze recent performance metrics
        val recentMetrics = metricsHistory.takeLast(10)
        if (recentMetrics.isEmpty()) {
            currentConfig = baseConfig
            return
        }
        
        val avgFPS = recentMetrics.map { it.currentFPS }.average().toFloat()
        val avgMemoryUsage = recentMetrics.map { it.memoryUsedMB }.average().toFloat()
        val maxThermalState = recentMetrics.maxOfOrNull { it.thermalState } ?: ThermalState.NOMINAL
        
        // Adaptive adjustments based on actual performance
        currentConfig = baseConfig.copy(
            maxConcurrentAnalyses = when {
                avgFPS < baseConfig.uiTargetFPS * 0.8f -> 1
                maxThermalState >= ThermalState.MODERATE_THROTTLING -> 1
                else -> baseConfig.maxConcurrentAnalyses
            },
            
            aiProcessingIntervalMs = when {
                avgFPS < baseConfig.uiTargetFPS * 0.7f -> baseConfig.aiProcessingIntervalMs * 2
                maxThermalState >= ThermalState.LIGHT_THROTTLING -> baseConfig.aiProcessingIntervalMs * 1.5f.toLong()
                else -> baseConfig.aiProcessingIntervalMs
            },
            
            enableModelPreloading = baseConfig.enableModelPreloading && 
                avgMemoryUsage < capabilities.totalMemoryMB * 0.6f &&
                maxThermalState < ThermalState.MODERATE_THROTTLING,
                
            useLowPowerMode = baseConfig.useLowPowerMode ||
                maxThermalState >= ThermalState.MODERATE_THROTTLING ||
                recentMetrics.lastOrNull()?.batteryLevel ?: 100f < 20f
        )
    }
    
    fun recordMetrics(metrics: PerformanceMetrics) {
        metricsHistory.add(metrics)
        // Keep only last 100 metrics
        if (metricsHistory.size > 100) {
            metricsHistory.removeAt(0)
        }
    }
    
    fun getPerformanceInsights(): PerformanceInsights {
        val recentMetrics = metricsHistory.takeLast(20)
        if (recentMetrics.isEmpty()) {
            return PerformanceInsights()
        }
        
        return PerformanceInsights(
            avgFPS = recentMetrics.map { it.currentFPS }.average().toFloat(),
            avgMemoryUsage = recentMetrics.map { it.memoryUsedMB }.average().toFloat(),
            avgAnalysisTime = recentMetrics.map { it.analysisTimeMs }.average().toLong(),
            cacheEffectiveness = recentMetrics.map { it.cacheHitRate }.average().toFloat(),
            thermalEvents = recentMetrics.count { it.thermalState >= ThermalState.LIGHT_THROTTLING },
            recommendations = generateRecommendations(recentMetrics)
        )
    }
    
    private fun generateRecommendations(metrics: List<PerformanceMetrics>): List<String> {
        val recommendations = mutableListOf<String>()
        
        val avgFPS = metrics.map { it.currentFPS }.average().toFloat()
        val avgMemoryUsage = metrics.map { it.memoryUsedMB }.average().toFloat()
        val thermalEvents = metrics.count { it.thermalState >= ThermalState.LIGHT_THROTTLING }
        
        if (avgFPS < currentConfig.uiTargetFPS * 0.8f) {
            recommendations.add("UI performance below target - consider reducing concurrent operations")
        }
        
        if (avgMemoryUsage > currentConfig.memoryThresholdMB) {
            recommendations.add("High memory usage detected - consider clearing cache or reducing model complexity")
        }
        
        if (thermalEvents > metrics.size * 0.3f) {
            recommendations.add("Frequent thermal throttling - enable low power mode")
        }
        
        return recommendations
    }
}

data class PerformanceInsights(
    val avgFPS: Float = 0f,
    val avgMemoryUsage: Float = 0f,
    val avgAnalysisTime: Long = 0L,
    val cacheEffectiveness: Float = 0f,
    val thermalEvents: Int = 0,
    val recommendations: List<String> = emptyList()
)
/**
 * Memory information for device optimization.
 */
data class MemoryInfo(
    val totalMemoryMB: Float,
    val availableMemoryMB: Float
)

/**
 * Extension function to check if thermal state is critical.
 */
fun ThermalState.isCritical(): Boolean {
    return this >= ThermalState.MODERATE_THROTTLING
}

/**
 * LiteRT-specific device capabilities for backend selection.
 * This is separate from the performance DeviceCapabilities to avoid conflicts.
 */
data class LiteRTDeviceCapabilities(
    val deviceTier: DeviceTier,
    val thermalState: ThermalState,
    val totalMemoryGB: Float,
    val availableMemoryGB: Float,
    val supportedBackends: List<com.hazardhawk.ai.litert.LiteRTBackend>,
    val batteryLevel: Int,
    val powerSaveMode: Boolean,
    val cpuCoreCount: Int,
    val gpuVendor: String,
    val hasNPU: Boolean,
    val hasHighPerformanceGPU: Boolean
)
