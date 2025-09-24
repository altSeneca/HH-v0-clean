package com.hazardhawk.production

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Production monitoring system for HazardHawk LiteRT integration
 * Provides comprehensive monitoring, alerting, and observability
 */
class ProductionMonitoringSystem(
    private val alertingService: AlertingService,
    private val metricsCollector: MetricsCollector,
    private val crashReporter: CrashReporter,
    private val performanceAnalyzer: PerformanceAnalyzer
) {
    private val scope = CoroutineScope(SupervisorJob())
    
    private val _systemHealth = MutableStateFlow(SystemHealth.HEALTHY)
    val systemHealth: StateFlow<SystemHealth> = _systemHealth.asStateFlow()
    
    private val _liteRTMetrics = MutableStateFlow(LiteRTMetrics())
    val liteRTMetrics: StateFlow<LiteRTMetrics> = _liteRTMetrics.asStateFlow()
    
    private val _userSatisfactionMetrics = MutableStateFlow(UserSatisfactionMetrics())
    val userSatisfactionMetrics: StateFlow<UserSatisfactionMetrics> = _userSatisfactionMetrics.asStateFlow()
    
    private val alertThresholds = AlertThresholds()
    private val performanceBaselines = PerformanceBaselines()
    
    companion object {
        private const val MONITORING_INTERVAL_MS = 30_000L // 30 seconds
        private const val ALERT_COOLDOWN_MS = 300_000L // 5 minutes
        private const val PERFORMANCE_HISTORY_SIZE = 100
    }
    
    init {
        startContinuousMonitoring()
    }

    /**
     * Start continuous monitoring of all system components
     */
    private fun startContinuousMonitoring() {
        scope.launch {
            kotlinx.coroutines.delay(5000) // Initial delay
            
            while (true) {
                try {
                    collectSystemMetrics()
                    analyzePerformanceTrends()
                    checkAlertConditions()
                    updateHealthStatus()
                    
                } catch (e: Exception) {
                    crashReporter.reportNonFatalError("MonitoringSystemError", e)
                }
                
                kotlinx.coroutines.delay(MONITORING_INTERVAL_MS)
            }
        }
    }

    /**
     * Report LiteRT inference performance
     */
    fun reportLiteRTInference(
        backend: LiteRTBackend,
        tokensPerSecond: Float,
        latencyMs: Long,
        memoryUsageMB: Float,
        success: Boolean,
        errorType: String? = null
    ) {
        val currentMetrics = _liteRTMetrics.value
        val backendStats = currentMetrics.backendPerformance.toMutableMap()
        
        val existingStats = backendStats[backend] ?: LiteRTBackendStats()
        val updatedStats = existingStats.copy(
            totalInferences = existingStats.totalInferences + 1,
            successfulInferences = if (success) existingStats.successfulInferences + 1 else existingStats.successfulInferences,
            avgTokensPerSecond = calculateMovingAverage(existingStats.avgTokensPerSecond, tokensPerSecond, existingStats.totalInferences),
            avgLatencyMs = calculateMovingAverage(existingStats.avgLatencyMs.toFloat(), latencyMs.toFloat(), existingStats.totalInferences).toLong(),
            avgMemoryUsageMB = calculateMovingAverage(existingStats.avgMemoryUsageMB, memoryUsageMB, existingStats.totalInferences),
            lastInferenceTime = Clock.System.now(),
            recentErrors = if (!success && errorType != null) {
                (existingStats.recentErrors + LiteRTError(errorType, Clock.System.now())).takeLast(10)
            } else existingStats.recentErrors
        )
        
        backendStats[backend] = updatedStats
        
        _liteRTMetrics.value = currentMetrics.copy(
            backendPerformance = backendStats,
            totalInferences = currentMetrics.totalInferences + 1,
            lastUpdateTime = Clock.System.now()
        )
        
        // Check for performance anomalies
        checkPerformanceAnomaly(backend, updatedStats)
        
        // Report to metrics collector
        metricsCollector.recordLiteRTInference(backend, tokensPerSecond, latencyMs, memoryUsageMB, success)
    }

    /**
     * Report backend switching event
     */
    fun reportBackendSwitch(
        fromBackend: LiteRTBackend,
        toBackend: LiteRTBackend,
        reason: BackendSwitchReason,
        switchTimeMs: Long,
        success: Boolean
    ) {
        val currentMetrics = _liteRTMetrics.value
        val switchEvent = BackendSwitchEvent(
            fromBackend = fromBackend,
            toBackend = toBackend,
            reason = reason,
            switchTimeMs = switchTimeMs,
            success = success,
            timestamp = Clock.System.now()
        )
        
        _liteRTMetrics.value = currentMetrics.copy(
            backendSwitches = (currentMetrics.backendSwitches + switchEvent).takeLast(50),
            totalBackendSwitches = currentMetrics.totalBackendSwitches + 1
        )
        
        // Alert if switching is too frequent
        checkBackendSwitchingFrequency()
        
        metricsCollector.recordBackendSwitch(fromBackend, toBackend, reason, switchTimeMs, success)
    }

    /**
     * Report user satisfaction metrics
     */
    fun reportUserSatisfaction(
        sessionDurationMs: Long,
        taskCompletionSuccess: Boolean,
        userRating: Float? = null,
        responseTimePerception: ResponseTimePerception? = null
    ) {
        val currentMetrics = _userSatisfactionMetrics.value
        
        _userSatisfactionMetrics.value = currentMetrics.copy(
            totalSessions = currentMetrics.totalSessions + 1,
            avgSessionDuration = calculateMovingAverage(
                currentMetrics.avgSessionDuration.toFloat(),
                sessionDurationMs.toFloat(),
                currentMetrics.totalSessions
            ).toLong(),
            taskSuccessRate = calculateSuccessRate(
                currentMetrics.taskSuccessRate,
                taskCompletionSuccess,
                currentMetrics.totalSessions
            ),
            avgUserRating = userRating?.let {
                calculateMovingAverage(currentMetrics.avgUserRating, it, currentMetrics.totalSessions.coerceAtMost(100))
            } ?: currentMetrics.avgUserRating,
            responseTimePerception = responseTimePerception ?: currentMetrics.responseTimePerception,
            lastUpdateTime = Clock.System.now()
        )
        
        metricsCollector.recordUserSatisfaction(sessionDurationMs, taskCompletionSuccess, userRating)
    }

    /**
     * Report system crash or critical error
     */
    fun reportCriticalError(
        component: String,
        errorType: String,
        errorMessage: String,
        stackTrace: String? = null,
        context: Map<String, String> = mapOf()
    ) {
        val errorReport = CriticalErrorReport(
            component = component,
            errorType = errorType,
            errorMessage = errorMessage,
            stackTrace = stackTrace,
            context = context,
            timestamp = Clock.System.now()
        )
        
        // Report to crash reporter
        crashReporter.reportCriticalError(errorReport)
        
        // Immediate alert for critical errors
        scope.launch {
            alertingService.sendAlert(
                AlertLevel.CRITICAL,
                "Critical Error in $component",
                "Error: $errorType - $errorMessage",
                mapOf(
                    "component" to component,
                    "error_type" to errorType,
                    "timestamp" to errorReport.timestamp.toString()
                )
            )
        }
        
        // Update system health
        updateSystemHealthForError(component, errorType)
    }

    /**
     * Get comprehensive system status
     */
    fun getSystemStatus(): SystemStatus {
        val liteRTHealth = assessLiteRTHealth()
        val performanceHealth = assessPerformanceHealth()
        val userSatisfactionHealth = assessUserSatisfactionHealth()
        
        return SystemStatus(
            overallHealth = _systemHealth.value,
            liteRTHealth = liteRTHealth,
            performanceHealth = performanceHealth,
            userSatisfactionHealth = userSatisfactionHealth,
            lastUpdateTime = Clock.System.now(),
            uptime = getSystemUptime(),
            activeAlerts = getActiveAlerts()
        )
    }

    /**
     * Get performance analytics report
     */
    fun getPerformanceReport(): PerformanceReport {
        val liteRTMetrics = _liteRTMetrics.value
        val userMetrics = _userSatisfactionMetrics.value
        
        return PerformanceReport(
            liteRTPerformance = liteRTMetrics,
            userSatisfaction = userMetrics,
            performanceComparison = compareWithBaselines(),
            recommendations = generatePerformanceRecommendations(),
            trendAnalysis = performanceAnalyzer.analyzeTrends(getDurationMinutes(24 * 60)) // 24 hours
        )
    }

    private fun collectSystemMetrics() {
        // Collect various system metrics
        val memoryUsage = metricsCollector.getMemoryUsage()
        val cpuUsage = metricsCollector.getCPUUsage()
        val networkLatency = metricsCollector.getNetworkLatency()
        
        metricsCollector.recordSystemMetrics(memoryUsage, cpuUsage, networkLatency)
    }

    private fun analyzePerformanceTrends() {
        val liteRTMetrics = _liteRTMetrics.value
        
        // Analyze performance trends for each backend
        liteRTMetrics.backendPerformance.forEach { (backend, stats) ->
            val trend = performanceAnalyzer.analyzeBackendTrend(backend, stats)
            
            if (trend.performanceTrend == PerformanceTrend.DECREASING && trend.significanceLevel > 0.8) {
                scope.launch {
                    alertingService.sendAlert(
                        AlertLevel.WARNING,
                        "Performance Degradation Detected",
                        "Backend $backend showing declining performance trend",
                        mapOf(
                            "backend" to backend.name,
                            "current_performance" to "${stats.avgTokensPerSecond}",
                            "trend" to trend.toString()
                        )
                    )
                }
            }
        }
    }

    private fun checkAlertConditions() {
        checkMemoryAlerts()
        checkLatencyAlerts()
        checkErrorRateAlerts()
        checkBackendHealthAlerts()
    }

    private fun checkMemoryAlerts() {
        val currentMemoryMB = metricsCollector.getMemoryUsage()
        if (currentMemoryMB > alertThresholds.memoryThresholdMB) {
            scope.launch {
                alertingService.sendAlert(
                    AlertLevel.WARNING,
                    "High Memory Usage",
                    "Memory usage: ${currentMemoryMB}MB (threshold: ${alertThresholds.memoryThresholdMB}MB)",
                    mapOf("memory_mb" to currentMemoryMB.toString())
                )
            }
        }
    }

    private fun checkLatencyAlerts() {
        val liteRTMetrics = _liteRTMetrics.value
        liteRTMetrics.backendPerformance.forEach { (backend, stats) ->
            if (stats.avgLatencyMs > alertThresholds.getLatencyThreshold(backend)) {
                scope.launch {
                    alertingService.sendAlert(
                        AlertLevel.WARNING,
                        "High Latency Alert",
                        "Backend $backend latency: ${stats.avgLatencyMs}ms",
                        mapOf(
                            "backend" to backend.name,
                            "latency_ms" to stats.avgLatencyMs.toString()
                        )
                    )
                }
            }
        }
    }

    private fun checkErrorRateAlerts() {
        val liteRTMetrics = _liteRTMetrics.value
        liteRTMetrics.backendPerformance.forEach { (backend, stats) ->
            val errorRate = if (stats.totalInferences > 0) {
                (stats.totalInferences - stats.successfulInferences).toFloat() / stats.totalInferences
            } else 0f
            
            if (errorRate > alertThresholds.errorRateThreshold) {
                scope.launch {
                    alertingService.sendAlert(
                        AlertLevel.CRITICAL,
                        "High Error Rate Alert",
                        "Backend $backend error rate: ${errorRate * 100}%",
                        mapOf(
                            "backend" to backend.name,
                            "error_rate" to (errorRate * 100).toString()
                        )
                    )
                }
            }
        }
    }

    private fun checkBackendHealthAlerts() {
        val liteRTMetrics = _liteRTMetrics.value
        liteRTMetrics.backendPerformance.forEach { (backend, stats) ->
            val timeSinceLastInference = Clock.System.now() - stats.lastInferenceTime
            
            if (timeSinceLastInference > alertThresholds.backendTimeoutDuration && stats.totalInferences > 0) {
                scope.launch {
                    alertingService.sendAlert(
                        AlertLevel.WARNING,
                        "Backend Unresponsive",
                        "Backend $backend has been inactive for ${timeSinceLastInference.inWholeMinutes} minutes",
                        mapOf(
                            "backend" to backend.name,
                            "inactive_minutes" to timeSinceLastInference.inWholeMinutes.toString()
                        )
                    )
                }
            }
        }
    }

    private fun checkPerformanceAnomaly(backend: LiteRTBackend, stats: LiteRTBackendStats) {
        val baseline = performanceBaselines.getBaseline(backend)
        val performanceRatio = stats.avgTokensPerSecond / baseline.expectedTokensPerSecond
        
        if (performanceRatio < 0.5) { // Performance is less than 50% of expected
            scope.launch {
                alertingService.sendAlert(
                    AlertLevel.WARNING,
                    "Performance Anomaly Detected",
                    "Backend $backend performance significantly below baseline",
                    mapOf(
                        "backend" to backend.name,
                        "current_performance" to "${stats.avgTokensPerSecond}",
                        "expected_performance" to "${baseline.expectedTokensPerSecond}",
                        "performance_ratio" to "${performanceRatio * 100}%"
                    )
                )
            }
        }
    }

    private fun checkBackendSwitchingFrequency() {
        val currentMetrics = _liteRTMetrics.value
        val recentSwitches = currentMetrics.backendSwitches.filter {
            Clock.System.now() - it.timestamp < 10.minutes
        }
        
        if (recentSwitches.size > 5) { // More than 5 switches in 10 minutes
            scope.launch {
                alertingService.sendAlert(
                    AlertLevel.WARNING,
                    "Frequent Backend Switching",
                    "Too many backend switches: ${recentSwitches.size} in 10 minutes",
                    mapOf("recent_switches" to recentSwitches.size.toString())
                )
            }
        }
    }

    private fun updateHealthStatus() {
        val liteRTHealth = assessLiteRTHealth()
        val performanceHealth = assessPerformanceHealth()
        val userSatisfactionHealth = assessUserSatisfactionHealth()
        
        val overallHealth = when {
            liteRTHealth == SystemHealth.CRITICAL || 
            performanceHealth == SystemHealth.CRITICAL ||
            userSatisfactionHealth == SystemHealth.CRITICAL -> SystemHealth.CRITICAL
            
            liteRTHealth == SystemHealth.DEGRADED ||
            performanceHealth == SystemHealth.DEGRADED ||
            userSatisfactionHealth == SystemHealth.DEGRADED -> SystemHealth.DEGRADED
            
            else -> SystemHealth.HEALTHY
        }
        
        if (_systemHealth.value != overallHealth) {
            _systemHealth.value = overallHealth
            
            scope.launch {
                alertingService.sendAlert(
                    when (overallHealth) {
                        SystemHealth.CRITICAL -> AlertLevel.CRITICAL
                        SystemHealth.DEGRADED -> AlertLevel.WARNING
                        SystemHealth.HEALTHY -> AlertLevel.INFO
                    },
                    "System Health Status Changed",
                    "System health changed to: $overallHealth",
                    mapOf("health_status" to overallHealth.name)
                )
            }
        }
    }

    private fun assessLiteRTHealth(): SystemHealth {
        val metrics = _liteRTMetrics.value
        
        if (metrics.backendPerformance.isEmpty()) return SystemHealth.HEALTHY
        
        val totalErrorRate = metrics.backendPerformance.values.map { stats ->
            if (stats.totalInferences > 0) {
                (stats.totalInferences - stats.successfulInferences).toFloat() / stats.totalInferences
            } else 0f
        }.average()
        
        return when {
            totalErrorRate > 0.2 -> SystemHealth.CRITICAL // > 20% error rate
            totalErrorRate > 0.1 -> SystemHealth.DEGRADED // > 10% error rate
            else -> SystemHealth.HEALTHY
        }
    }

    private fun assessPerformanceHealth(): SystemHealth {
        val memoryUsage = metricsCollector.getMemoryUsage()
        val cpuUsage = metricsCollector.getCPUUsage()
        
        return when {
            memoryUsage > alertThresholds.memoryThresholdMB || cpuUsage > 90 -> SystemHealth.CRITICAL
            memoryUsage > alertThresholds.memoryThresholdMB * 0.8 || cpuUsage > 80 -> SystemHealth.DEGRADED
            else -> SystemHealth.HEALTHY
        }
    }

    private fun assessUserSatisfactionHealth(): SystemHealth {
        val metrics = _userSatisfactionMetrics.value
        
        return when {
            metrics.taskSuccessRate < 0.7 -> SystemHealth.CRITICAL // < 70% success rate
            metrics.taskSuccessRate < 0.85 -> SystemHealth.DEGRADED // < 85% success rate
            else -> SystemHealth.HEALTHY
        }
    }

    // Helper functions
    private fun calculateMovingAverage(current: Float, newValue: Float, count: Int): Float {
        return if (count == 1) newValue else (current * (count - 1) + newValue) / count
    }

    private fun calculateSuccessRate(current: Float, success: Boolean, count: Int): Float {
        val successCount = (current * (count - 1)) + if (success) 1f else 0f
        return successCount / count
    }

    private fun updateSystemHealthForError(component: String, errorType: String) {
        when (component) {
            "LiteRT" -> _systemHealth.value = SystemHealth.DEGRADED
            "Memory", "GPU", "NPU" -> _systemHealth.value = SystemHealth.CRITICAL
        }
    }

    private fun getSystemUptime(): Duration {
        // Placeholder - implement actual uptime tracking
        return 1.minutes
    }

    private fun getActiveAlerts(): List<ActiveAlert> {
        // Placeholder - implement active alerts tracking
        return emptyList()
    }

    private fun compareWithBaselines(): PerformanceComparison {
        // Placeholder - implement baseline comparison
        return PerformanceComparison(
            cpuPerformanceRatio = 1.0f,
            gpuPerformanceRatio = 1.0f,
            npuPerformanceRatio = 1.0f,
            memoryEfficiencyRatio = 1.0f
        )
    }

    private fun generatePerformanceRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val metrics = _liteRTMetrics.value
        
        // Analyze performance and generate recommendations
        if (metricsCollector.getMemoryUsage() > alertThresholds.memoryThresholdMB * 0.8) {
            recommendations.add("Consider enabling memory optimization features")
        }
        
        val frequentSwitches = metrics.backendSwitches.filter {
            Clock.System.now() - it.timestamp < 30.minutes
        }
        
        if (frequentSwitches.size > 10) {
            recommendations.add("Backend switching is too frequent - review switching thresholds")
        }
        
        return recommendations
    }

    private fun getDurationMinutes(minutes: Int): Duration = minutes.minutes
}

// Data classes for monitoring system
data class LiteRTMetrics(
    val backendPerformance: Map<LiteRTBackend, LiteRTBackendStats> = mapOf(),
    val backendSwitches: List<BackendSwitchEvent> = emptyList(),
    val totalInferences: Int = 0,
    val totalBackendSwitches: Int = 0,
    val lastUpdateTime: Instant = Clock.System.now()
)

data class LiteRTBackendStats(
    val totalInferences: Int = 0,
    val successfulInferences: Int = 0,
    val avgTokensPerSecond: Float = 0f,
    val avgLatencyMs: Long = 0L,
    val avgMemoryUsageMB: Float = 0f,
    val lastInferenceTime: Instant = Clock.System.now(),
    val recentErrors: List<LiteRTError> = emptyList()
)

data class LiteRTError(
    val errorType: String,
    val timestamp: Instant
)

data class BackendSwitchEvent(
    val fromBackend: LiteRTBackend,
    val toBackend: LiteRTBackend,
    val reason: BackendSwitchReason,
    val switchTimeMs: Long,
    val success: Boolean,
    val timestamp: Instant
)

data class UserSatisfactionMetrics(
    val totalSessions: Int = 0,
    val avgSessionDuration: Long = 0L,
    val taskSuccessRate: Float = 1f,
    val avgUserRating: Float = 5f,
    val responseTimePerception: ResponseTimePerception = ResponseTimePerception.ACCEPTABLE,
    val lastUpdateTime: Instant = Clock.System.now()
)

data class CriticalErrorReport(
    val component: String,
    val errorType: String,
    val errorMessage: String,
    val stackTrace: String?,
    val context: Map<String, String>,
    val timestamp: Instant
)

data class SystemStatus(
    val overallHealth: SystemHealth,
    val liteRTHealth: SystemHealth,
    val performanceHealth: SystemHealth,
    val userSatisfactionHealth: SystemHealth,
    val lastUpdateTime: Instant,
    val uptime: Duration,
    val activeAlerts: List<ActiveAlert>
)

data class PerformanceReport(
    val liteRTPerformance: LiteRTMetrics,
    val userSatisfaction: UserSatisfactionMetrics,
    val performanceComparison: PerformanceComparison,
    val recommendations: List<String>,
    val trendAnalysis: TrendAnalysis
)

data class PerformanceComparison(
    val cpuPerformanceRatio: Float,
    val gpuPerformanceRatio: Float,
    val npuPerformanceRatio: Float,
    val memoryEfficiencyRatio: Float
)

data class ActiveAlert(
    val level: AlertLevel,
    val title: String,
    val message: String,
    val timestamp: Instant,
    val acknowledged: Boolean = false
)

data class TrendAnalysis(
    val performanceTrend: PerformanceTrend = PerformanceTrend.STABLE,
    val significanceLevel: Double = 0.0,
    val projectedImpact: String = "No significant change expected"
)

// Enums and configuration classes
enum class SystemHealth {
    HEALTHY,
    DEGRADED,
    CRITICAL
}

enum class LiteRTBackend {
    CPU,
    GPU,
    NPU
}

enum class BackendSwitchReason {
    PERFORMANCE_DEGRADATION,
    MEMORY_PRESSURE,
    THERMAL_THROTTLING,
    BACKEND_FAILURE,
    MANUAL_OVERRIDE,
    ADAPTIVE_OPTIMIZATION
}

enum class ResponseTimePerception {
    VERY_FAST,
    FAST,
    ACCEPTABLE,
    SLOW,
    VERY_SLOW
}

enum class AlertLevel {
    INFO,
    WARNING,
    CRITICAL
}

enum class PerformanceTrend {
    IMPROVING,
    STABLE,
    DECREASING
}

// Configuration classes
data class AlertThresholds(
    val memoryThresholdMB: Float = 1800f, // 1.8GB threshold
    val errorRateThreshold: Float = 0.05f, // 5% error rate
    val backendTimeoutDuration: Duration = 5.minutes,
    val cpuBackendLatencyMs: Long = 5000L,
    val gpuBackendLatencyMs: Long = 1000L,
    val npuBackendLatencyMs: Long = 500L
) {
    fun getLatencyThreshold(backend: LiteRTBackend): Long = when (backend) {
        LiteRTBackend.CPU -> cpuBackendLatencyMs
        LiteRTBackend.GPU -> gpuBackendLatencyMs
        LiteRTBackend.NPU -> npuBackendLatencyMs
    }
}

data class PerformanceBaselines(
    val cpuExpectedTokensPerSecond: Float = 243f,
    val gpuExpectedTokensPerSecond: Float = 1876f,
    val npuExpectedTokensPerSecond: Float = 5836f
) {
    fun getBaseline(backend: LiteRTBackend) = when (backend) {
        LiteRTBackend.CPU -> PerformanceBaseline(cpuExpectedTokensPerSecond)
        LiteRTBackend.GPU -> PerformanceBaseline(gpuExpectedTokensPerSecond)
        LiteRTBackend.NPU -> PerformanceBaseline(npuExpectedTokensPerSecond)
    }
}

data class PerformanceBaseline(
    val expectedTokensPerSecond: Float
)

// Service interfaces
interface AlertingService {
    suspend fun sendAlert(level: AlertLevel, title: String, message: String, metadata: Map<String, String>)
}

interface MetricsCollector {
    fun recordLiteRTInference(backend: LiteRTBackend, tokensPerSecond: Float, latencyMs: Long, memoryUsageMB: Float, success: Boolean)
    fun recordBackendSwitch(fromBackend: LiteRTBackend, toBackend: LiteRTBackend, reason: BackendSwitchReason, switchTimeMs: Long, success: Boolean)
    fun recordUserSatisfaction(sessionDurationMs: Long, taskCompletionSuccess: Boolean, userRating: Float?)
    fun recordSystemMetrics(memoryUsage: Float, cpuUsage: Float, networkLatency: Long)
    fun getMemoryUsage(): Float
    fun getCPUUsage(): Float
    fun getNetworkLatency(): Long
}

interface CrashReporter {
    fun reportCriticalError(errorReport: CriticalErrorReport)
    fun reportNonFatalError(tag: String, exception: Exception)
}

interface PerformanceAnalyzer {
    fun analyzeBackendTrend(backend: LiteRTBackend, stats: LiteRTBackendStats): TrendAnalysis
    fun analyzeTrends(duration: Duration): TrendAnalysis
}