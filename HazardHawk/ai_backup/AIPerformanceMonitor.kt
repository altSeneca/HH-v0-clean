package com.hazardhawk.ai

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive AI performance monitoring and optimization system.
 * 
 * Tracks real-time performance metrics, device compatibility, and provides
 * optimization recommendations for the HazardHawk AI integration.
 */
class AIPerformanceMonitor(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    
    private val _performanceMetrics = MutableStateFlow(AIPerformanceMetrics.initial())
    val performanceMetrics: StateFlow<AIPerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    private val _deviceCompatibility = MutableStateFlow(DeviceCompatibilityInfo.unknown())
    val deviceCompatibility: StateFlow<DeviceCompatibilityInfo> = _deviceCompatibility.asStateFlow()
    
    private val analysisHistory = mutableListOf<AnalysisRecord>()
    private var totalAnalyses = 0
    private var successfulAnalyses = 0
    private var totalAnalysisTime = 0L
    private var batteryStart: Float = 100f
    private var memoryPeakMB = 0
    
    // Performance targets from implementation plan
    private val performanceTargets = PerformanceTargets(
        maxAnalysisTimeMs = 3000L,
        maxMemoryUsageMB = 2048,
        minSuccessRate = 0.95f,
        maxBatteryImpactPercent = 3f,
        maxErrorRate = 0.05f
    )
    
    private val alertThresholds = AlertThresholds(
        highMemoryUsageMB = 1500,
        slowAnalysisTimeMs = 5000L,
        lowSuccessRate = 0.80f,
        highBatteryUsagePercent = 5f,
        highTemperatureC = 45f
    )
    
    /**
     * Initialize performance monitoring with device capability detection.
     */
    suspend fun initialize() {
        detectDeviceCapabilities()
        startPerformanceTracking()
    }
    
    /**
     * Record start of AI analysis for performance tracking.
     */
    fun recordAnalysisStart(
        imageSize: Int,
        modelType: String = "Gemma3N-E2B",
        workType: WorkType = WorkType.GENERAL_CONSTRUCTION
    ): AnalysisSession {
        val sessionId = generateSessionId()
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        return AnalysisSession(
            sessionId = sessionId,
            startTime = startTime,
            imageSize = imageSize,
            modelType = modelType,
            workType = workType,
            initialMemoryMB = getCurrentMemoryUsageMB()
        )
    }
    
    /**
     * Record completion of AI analysis with performance metrics.
     */
    fun recordAnalysisComplete(
        session: AnalysisSession,
        success: Boolean,
        errorMessage: String? = null,
        analysisResult: SafetyAnalysisResult? = null
    ) {
        val endTime = Clock.System.now().toEpochMilliseconds()
        val analysisTime = endTime - session.startTime
        val currentMemoryMB = getCurrentMemoryUsageMB()
        
        totalAnalyses++
        if (success) successfulAnalyses++
        totalAnalysisTime += analysisTime
        
        if (currentMemoryMB > memoryPeakMB) {
            memoryPeakMB = currentMemoryMB
        }
        
        val record = AnalysisRecord(
            sessionId = session.sessionId,
            timestamp = endTime,
            analysisTimeMs = analysisTime,
            success = success,
            memoryUsageMB = currentMemoryMB,
            peakMemoryMB = session.initialMemoryMB.coerceAtLeast(currentMemoryMB),
            imageSize = session.imageSize,
            modelType = session.modelType,
            workType = session.workType,
            errorMessage = errorMessage,
            confidence = analysisResult?.confidence ?: 0f,
            hazardCount = analysisResult?.hazardIdentifications?.size ?: 0
        )
        
        analysisHistory.add(record)
        
        // Keep only recent records to prevent memory bloat
        if (analysisHistory.size > 1000) {
            analysisHistory.removeAt(0)
        }
        
        updatePerformanceMetrics()
        checkPerformanceAlerts(record)
    }
    
    /**
     * Record memory usage spike for monitoring.
     */
    fun recordMemorySpike(memoryMB: Int, context: String) {
        if (memoryMB > memoryPeakMB) {
            memoryPeakMB = memoryMB
            
            if (memoryMB > alertThresholds.highMemoryUsageMB) {
                scope.launch {
                    _alerts.emit(
                        PerformanceAlert(
                            type = AlertType.HIGH_MEMORY_USAGE,
                            message = "High memory usage: ${memoryMB}MB in $context",
                            severity = if (memoryMB > performanceTargets.maxMemoryUsageMB) 
                                AlertSeverity.CRITICAL else AlertSeverity.WARNING,
                            timestamp = Clock.System.now().toEpochMilliseconds(),
                            context = context,
                            value = memoryMB.toFloat()
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Get performance recommendations based on current metrics.
     */
    fun getPerformanceRecommendations(): List<PerformanceRecommendation> {
        val current = _performanceMetrics.value
        val recommendations = mutableListOf<PerformanceRecommendation>()
        
        // Analysis time recommendations
        if (current.averageAnalysisTime > performanceTargets.maxAnalysisTimeMs) {
            recommendations.add(
                PerformanceRecommendation(
                    category = RecommendationCategory.PERFORMANCE,
                    title = "Optimize Analysis Speed",
                    description = "Average analysis time (${current.averageAnalysisTime}ms) exceeds target (${performanceTargets.maxAnalysisTimeMs}ms)",
                    priority = RecommendationPriority.HIGH,
                    actions = listOf(
                        "Enable GPU acceleration if available",
                        "Reduce input image resolution",
                        "Enable model quantization",
                        "Limit concurrent analyses"
                    ),
                    impact = "Reduce analysis time by 30-50%"
                )
            )
        }
        
        // Memory usage recommendations
        if (current.peakMemoryMB > performanceTargets.maxMemoryUsageMB) {
            recommendations.add(
                PerformanceRecommendation(
                    category = RecommendationCategory.MEMORY,
                    title = "Optimize Memory Usage",
                    description = "Peak memory usage (${current.peakMemoryMB}MB) exceeds target (${performanceTargets.maxMemoryUsageMB}MB)",
                    priority = RecommendationPriority.CRITICAL,
                    actions = listOf(
                        "Enable model quantization",
                        "Implement memory mapping",
                        "Reduce batch sizes",
                        "Add explicit garbage collection",
                        "Consider model pruning"
                    ),
                    impact = "Reduce memory usage by 20-40%"
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Generate comprehensive performance report.
     */
    fun generatePerformanceReport(): PerformanceReport {
        val current = _performanceMetrics.value
        val recent = analysisHistory.takeLast(100)
        
        return PerformanceReport(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            overallStatus = current.meetsPerformanceTargets(),
            metrics = current,
            deviceInfo = _deviceCompatibility.value,
            recommendations = getPerformanceRecommendations(),
            recentAnalyses = recent.size,
            averageAnalysisTime = recent.map { it.analysisTimeMs }.average(),
            successRate = recent.count { it.success }.toFloat() / recent.size,
            memoryTrend = calculateMemoryTrend(),
            performanceTrend = calculatePerformanceTrend(),
            alertsCount = calculateRecentAlerts()
        )
    }
    
    // Performance alerts flow
    private val _alerts = MutableSharedFlow<PerformanceAlert>(replay = 10)
    val alerts: SharedFlow<PerformanceAlert> = _alerts.asSharedFlow()
    
    private fun detectDeviceCapabilities() {
        // This would be implemented differently per platform
        val compatibility = DeviceCompatibilityInfo.unknown()
        _deviceCompatibility.value = compatibility
    }
    
    private fun startPerformanceTracking() {
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(5.seconds)
                updatePerformanceMetrics()
            }
        }
    }
    
    private fun updatePerformanceMetrics() {
        val successRate = if (totalAnalyses > 0) successfulAnalyses.toFloat() / totalAnalyses else 0f
        val errorRate = 1f - successRate
        val averageTime = if (totalAnalyses > 0) totalAnalysisTime.toFloat() / totalAnalyses else 0f
        
        val updated = AIPerformanceMetrics(
            analysisTimeMs = if (analysisHistory.isNotEmpty()) analysisHistory.last().analysisTimeMs else 0L,
            memoryUsageMB = getCurrentMemoryUsageMB(),
            peakMemoryMB = memoryPeakMB,
            successRate = successRate,
            errorRate = errorRate,
            batteryImpactPercent = calculateBatteryImpact(),
            totalAnalysisCount = totalAnalyses,
            failedAnalysisCount = totalAnalyses - successfulAnalyses,
            averageAnalysisTime = averageTime,
            deviceTemperatureC = getDeviceTemperature(),
            lastUpdated = Clock.System.now().toEpochMilliseconds()
        )
        
        _performanceMetrics.value = updated
    }
    
    private fun checkPerformanceAlerts(record: AnalysisRecord) {
        scope.launch {
            if (record.analysisTimeMs > alertThresholds.slowAnalysisTimeMs) {
                _alerts.emit(
                    PerformanceAlert(
                        type = AlertType.SLOW_ANALYSIS,
                        message = "Slow analysis detected: ${record.analysisTimeMs}ms",
                        severity = AlertSeverity.WARNING,
                        timestamp = record.timestamp,
                        context = "Analysis ID: ${record.sessionId}",
                        value = record.analysisTimeMs.toFloat()
                    )
                )
            }
        }
    }
    
    // Platform-specific implementations would override these
    protected open fun getCurrentMemoryUsageMB(): Int = 512
    protected open fun getDeviceTemperature(): Float = 25f
    protected open fun calculateBatteryImpact(): Float = 1f
    
    private fun calculateMemoryTrend(): PerformanceTrend = PerformanceTrend.STABLE
    private fun calculatePerformanceTrend(): PerformanceTrend = PerformanceTrend.STABLE
    private fun calculateRecentAlerts(): Int = 0
    
    private fun generateSessionId(): String = 
        "analysis_${Clock.System.now().toEpochMilliseconds()}_${kotlin.random.Random.nextInt(1000)}"
}

/**
 * Performance tracking session for individual analyses.
 */
data class AnalysisSession(
    val sessionId: String,
    val startTime: Long,
    val imageSize: Int,
    val modelType: String,
    val workType: WorkType,
    val initialMemoryMB: Int
)

/**
 * Individual analysis record for performance tracking.
 */
@Serializable
data class AnalysisRecord(
    val sessionId: String,
    val timestamp: Long,
    val analysisTimeMs: Long,
    val success: Boolean,
    val memoryUsageMB: Int,
    val peakMemoryMB: Int,
    val imageSize: Int,
    val modelType: String,
    val workType: WorkType,
    val errorMessage: String? = null,
    val confidence: Float = 0f,
    val hazardCount: Int = 0
)

/**
 * Performance targets for optimization.
 */
@Serializable
data class PerformanceTargets(
    val maxAnalysisTimeMs: Long,
    val maxMemoryUsageMB: Int,
    val minSuccessRate: Float,
    val maxBatteryImpactPercent: Float,
    val maxErrorRate: Float
)

/**
 * Alert thresholds for performance monitoring.
 */
@Serializable
data class AlertThresholds(
    val highMemoryUsageMB: Int,
    val slowAnalysisTimeMs: Long,
    val lowSuccessRate: Float,
    val highBatteryUsagePercent: Float,
    val highTemperatureC: Float
)

/**
 * Performance alert for monitoring system.
 */
@Serializable
data class PerformanceAlert(
    val type: AlertType,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Long,
    val context: String,
    val value: Float
)

/**
 * Performance recommendation for optimization.
 */
@Serializable
data class PerformanceRecommendation(
    val category: RecommendationCategory,
    val title: String,
    val description: String,
    val priority: RecommendationPriority,
    val actions: List<String>,
    val impact: String
)

/**
 * Comprehensive performance report.
 */
@Serializable
data class PerformanceReport(
    val timestamp: Long,
    val overallStatus: PerformanceStatus,
    val metrics: AIPerformanceMetrics,
    val deviceInfo: DeviceCompatibilityInfo,
    val recommendations: List<PerformanceRecommendation>,
    val recentAnalyses: Int,
    val averageAnalysisTime: Double,
    val successRate: Float,
    val memoryTrend: PerformanceTrend,
    val performanceTrend: PerformanceTrend,
    val alertsCount: Int
)

// Enums for performance monitoring
@Serializable
enum class AlertType {
    HIGH_MEMORY_USAGE,
    SLOW_ANALYSIS,
    ANALYSIS_FAILURE,
    HIGH_BATTERY_USAGE,
    HIGH_TEMPERATURE,
    MODEL_LOAD_FAILURE,
    LOW_SUCCESS_RATE
}

@Serializable
enum class AlertSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

@Serializable
enum class RecommendationCategory {
    PERFORMANCE,
    MEMORY,
    RELIABILITY,
    COMPATIBILITY,
    BATTERY,
    THERMAL
}

@Serializable
enum class PerformanceTrend {
    IMPROVING,
    STABLE,
    DEGRADING
}

// Extension functions for RecommendedAISettings
fun RecommendedAISettings.Companion.balanced() = RecommendedAISettings(
    enableGPUAcceleration = true,
    enableNNAPI = true,
    maxConcurrentAnalyses = 1,
    recommendedInputSize = 224,
    enableModelQuantization = true,
    enableMemoryMapping = true,
    backgroundProcessingLimit = 2,
    maxModelCacheSize = 1024,
    enableThermalThrottling = true
)