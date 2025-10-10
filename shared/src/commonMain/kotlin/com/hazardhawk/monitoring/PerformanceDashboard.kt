package com.hazardhawk.monitoring

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import com.hazardhawk.security.audit.RealTimeMonitoringSystem
import com.hazardhawk.security.audit.MonitoringDashboardData
import com.hazardhawk.ai.AIPerformanceOptimizer
import com.hazardhawk.ai.CurrentPerformanceAnalysis
import com.hazardhawk.core.models.WorkType
import com.hazardhawk.core.models.Severity
import com.hazardhawk.core.models.AlertType

/**
 * Phase 3 Real-time Performance Dashboard
 * Provides comprehensive metrics visualization and monitoring for production systems
 */
class PerformanceDashboard(
    private val monitoringSystem: RealTimeMonitoringSystem,
    private val aiPerformanceOptimizer: AIPerformanceOptimizer
) {
    
    companion object {
        private const val DASHBOARD_UPDATE_INTERVAL_MS = 5000L
        private const val ALERT_THRESHOLD_RESPONSE_TIME_MS = 4000L
        private const val ALERT_THRESHOLD_ERROR_RATE_PERCENT = 5.0
        private const val PERFORMANCE_HISTORY_SIZE = 288 // 24 hours at 5-minute intervals
        private const val METRICS_RETENTION_HOURS = 72
    }

    // Real-time dashboard data streams
    private val _dashboardUpdates = MutableSharedFlow<DashboardSnapshot>(replay = 1)
    val dashboardUpdates: Flow<DashboardSnapshot> = _dashboardUpdates.asSharedFlow()
    
    private val _performanceAlerts = MutableSharedFlow<PerformanceAlert>(replay = 5)
    val performanceAlerts: Flow<PerformanceAlert> = _performanceAlerts.asSharedFlow()
    
    private val _systemHealthStatus = MutableSharedFlow<SystemHealthSnapshot>(replay = 1)
    val systemHealthStatus: Flow<SystemHealthSnapshot> = _systemHealthStatus.asSharedFlow()

    // Performance metrics storage
    private val metricsHistory = mutableListOf<PerformanceMetricSnapshot>()
    private val errorHistory = mutableListOf<ErrorEvent>()
    private val responseTimeHistory = mutableListOf<ResponseTimeMetric>()

    /**
     * Initialize dashboard with real-time monitoring
     */
    suspend fun initializeDashboard(): DashboardInitializationResult {
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        try {
            // Load initial data
            val initialSnapshot = generateDashboardSnapshot()
            _dashboardUpdates.emit(initialSnapshot)
            
            // Initialize health monitoring
            val healthSnapshot = generateSystemHealthSnapshot()
            _systemHealthStatus.emit(healthSnapshot)
            
            return DashboardInitializationResult(
                success = true,
                initializationTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                initialSnapshot = initialSnapshot,
                enabledFeatures = listOf(
                    "Real-time safety alerts",
                    "AI performance monitoring",
                    "OSHA compliance tracking",
                    "Response time analytics",
                    "System health monitoring",
                    "Photo processing metrics"
                ),
                alertingEnabled = true
            )
        } catch (e: Exception) {
            return DashboardInitializationResult(
                success = false,
                initializationTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                initialSnapshot = null,
                enabledFeatures = emptyList(),
                alertingEnabled = false,
                error = e.message
            )
        }
    }

    /**
     * Update dashboard with real-time metrics
     */
    suspend fun updateMetrics(
        responseTime: Long,
        workType: WorkType,
        success: Boolean,
        errorMessage: String? = null,
        photoProcessingMetrics: PhotoProcessingMetrics? = null
    ) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        
        // Record performance metrics
        recordResponseTime(responseTime, workType, timestamp)
        
        // Record errors if any
        if (!success && errorMessage != null) {
            recordError(workType, errorMessage, timestamp)
        }
        
        // Record photo processing metrics
        photoProcessingMetrics?.let { metrics ->
            recordPhotoProcessingMetrics(metrics, timestamp)
        }
        
        // Update performance history
        val metricSnapshot = PerformanceMetricSnapshot(
            timestamp = timestamp,
            responseTimeMs = responseTime,
            workType = workType,
            success = success,
            errorRate = calculateCurrentErrorRate(),
            throughputPerMinute = calculateCurrentThroughput(),
            photoProcessingMetrics = photoProcessingMetrics
        )
        
        addToMetricsHistory(metricSnapshot)
        
        // Check for performance alerts
        checkPerformanceAlerts(metricSnapshot)
        
        // Generate and emit updated dashboard snapshot
        val dashboardSnapshot = generateDashboardSnapshot()
        _dashboardUpdates.emit(dashboardSnapshot)
    }

    /**
     * Generate comprehensive dashboard data
     */
    suspend fun generateDashboardSnapshot(): DashboardSnapshot {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val currentTime = timestamp
        val last24Hours = currentTime - (24 * 60 * 60 * 1000L)
        val lastHour = currentTime - (60 * 60 * 1000L)
        
        // Get monitoring data
        val monitoringData = monitoringSystem.generateDashboardData()
        
        // Calculate performance metrics
        val recentMetrics = metricsHistory.filter { it.timestamp > last24Hours }
        val recentResponseTimes = responseTimeHistory.filter { it.timestamp > lastHour }
        val recentErrors = errorHistory.filter { it.timestamp > last24Hours }
        
        return DashboardSnapshot(
            timestamp = timestamp,
            safetyMetrics = SafetyDashboardMetrics(
                totalActiveAlerts = monitoringData.totalActiveAlerts,
                criticalAlerts = monitoringData.criticalAlerts,
                alertsLast24Hours = monitoringData.alertsLast24Hours,
                complianceEventsLast24Hours = monitoringData.complianceEventsLast24Hours,
                complianceRate = monitoringData.complianceRate,
                alertsByType = monitoringData.alertsByTypeLastWeek,
                severityDistribution = monitoringData.severityDistribution
            ),
            performanceMetrics = PerformanceDashboardMetrics(
                averageResponseTimeMs = recentResponseTimes.map { it.responseTime }.average().toLong(),
                medianResponseTimeMs = calculateMedianResponseTime(recentResponseTimes),
                p95ResponseTimeMs = calculatePercentileResponseTime(recentResponseTimes, 0.95),
                p99ResponseTimeMs = calculatePercentileResponseTime(recentResponseTimes, 0.99),
                throughputPerMinute = calculateCurrentThroughput(),
                errorRate = calculateCurrentErrorRate(),
                successRate = calculateSuccessRate(recentMetrics),
                responseTimeByWorkType = calculateResponseTimeByWorkType(recentResponseTimes),
                performanceTrend = calculatePerformanceTrend()
            ),
            systemHealthMetrics = SystemHealthDashboardMetrics(
                cpuUtilization = getCurrentCpuUtilization(),
                memoryUtilization = getCurrentMemoryUtilization(),
                storageUtilization = getCurrentStorageUtilization(),
                networkLatency = getCurrentNetworkLatency(),
                systemUptime = getSystemUptime(),
                activeConnections = getActiveConnections(),
                healthStatus = determineOverallHealthStatus()
            ),
            photoProcessingMetrics = PhotoProcessingDashboardMetrics(
                photosProcessedLast24Hours = countPhotosProcessed(last24Hours),
                averageProcessingTimeMs = calculateAveragePhotoProcessingTime(recentMetrics),
                batchProcessingEfficiency = calculateBatchProcessingEfficiency(),
                compressionRatio = calculateAverageCompressionRatio(),
                uploadSuccessRate = calculateUploadSuccessRate(),
                queueDepth = getCurrentQueueDepth(),
                processingBacklog = getCurrentProcessingBacklog()
            ),
            alertSummary = AlertDashboardSummary(
                activePerformanceAlerts = getActivePerformanceAlerts(),
                recentErrors = recentErrors.takeLast(10),
                systemWarnings = getSystemWarnings(),
                maintenanceAlerts = getMaintenanceAlerts()
            ),
            chartData = generateChartData(recentMetrics, recentResponseTimes, recentErrors)
        )
    }

    /**
     * Generate system health snapshot
     */
    private suspend fun generateSystemHealthSnapshot(): SystemHealthSnapshot {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        
        return SystemHealthSnapshot(
            timestamp = timestamp,
            overallHealth = determineOverallHealthStatus(),
            componentStatuses = mapOf(
                "AI_SERVICE" to ComponentHealth(SystemHealthStatus.HEALTHY, 95.0, emptyList()),
                "DATABASE" to ComponentHealth(SystemHealthStatus.HEALTHY, 98.0, emptyList()),
                "PHOTO_STORAGE" to ComponentHealth(SystemHealthStatus.HEALTHY, 92.0, emptyList()),
                "API_GATEWAY" to ComponentHealth(SystemHealthStatus.HEALTHY, 97.0, emptyList()),
                "MONITORING" to ComponentHealth(SystemHealthStatus.HEALTHY, 99.0, emptyList())
            ),
            resourceUtilization = ResourceUtilization(
                cpu = getCurrentCpuUtilization(),
                memory = getCurrentMemoryUtilization(),
                storage = getCurrentStorageUtilization(),
                network = getCurrentNetworkLatency()
            ),
            activeSessions = getActiveUserSessions(),
            systemLoad = calculateSystemLoad()
        )
    }

    /**
     * Check for performance alerts and emit warnings
     */
    private suspend fun checkPerformanceAlerts(metric: PerformanceMetricSnapshot) {
        val alerts = mutableListOf<PerformanceAlert>()
        
        // Response time alert
        if (metric.responseTimeMs > ALERT_THRESHOLD_RESPONSE_TIME_MS) {
            alerts.add(PerformanceAlert(
                id = "response-time-${metric.timestamp}",
                type = AlertType.PERFORMANCE_DEGRADATION,
                severity = Severity.HIGH,
                title = "High Response Time Detected",
                message = "Response time (${metric.responseTimeMs}ms) exceeded threshold (${ALERT_THRESHOLD_RESPONSE_TIME_MS}ms) for ${metric.workType}",
                timestamp = metric.timestamp,
                workType = metric.workType,
                metricValue = metric.responseTimeMs.toDouble(),
                threshold = ALERT_THRESHOLD_RESPONSE_TIME_MS.toDouble()
            ))
        }
        
        // Error rate alert
        if (metric.errorRate > ALERT_THRESHOLD_ERROR_RATE_PERCENT) {
            alerts.add(PerformanceAlert(
                id = "error-rate-${metric.timestamp}",
                type = AlertType.HIGH_ERROR_RATE,
                severity = Severity.HIGH,
                title = "High Error Rate Detected",
                message = "Error rate (${kotlin.math.round(metric.errorRate * 100) / 100}%) exceeded threshold (${ALERT_THRESHOLD_ERROR_RATE_PERCENT}%)",
                timestamp = metric.timestamp,
                workType = metric.workType,
                metricValue = metric.errorRate,
                threshold = ALERT_THRESHOLD_ERROR_RATE_PERCENT
            ))
        }
        
        // Emit alerts
        alerts.forEach { alert ->
            _performanceAlerts.emit(alert)
        }
    }

    /**
     * Generate chart data for dashboard visualizations
     */
    private fun generateChartData(
        metrics: List<PerformanceMetricSnapshot>,
        responseTimes: List<ResponseTimeMetric>,
        errors: List<ErrorEvent>
    ): DashboardChartData {
        return DashboardChartData(
            responseTimeChart = TimeSeriesChartData(
                dataPoints = responseTimes.map { 
                    DataPoint(it.timestamp, it.responseTime.toDouble()) 
                },
                title = "Response Time Trend",
                yAxisLabel = "Response Time (ms)",
                color = "#4CAF50"
            ),
            throughputChart = TimeSeriesChartData(
                dataPoints = generateThroughputDataPoints(metrics),
                title = "Throughput Trend",
                yAxisLabel = "Requests/minute",
                color = "#2196F3"
            ),
            errorRateChart = TimeSeriesChartData(
                dataPoints = generateErrorRateDataPoints(errors),
                title = "Error Rate Trend",
                yAxisLabel = "Error Rate (%)",
                color = "#F44336"
            ),
            workTypeDistribution = PieChartData(
                segments = calculateWorkTypeDistribution(metrics),
                title = "Requests by Work Type",
                colors = listOf("#FF9800", "#9C27B0", "#607D8B", "#795548")
            ),
            severityDistribution = PieChartData(
                segments = calculateSeverityDistribution(metrics),
                title = "Alert Severity Distribution",
                colors = listOf("#4CAF50", "#FF9800", "#F44336", "#9E9E9E")
            )
        )
    }

    // Helper methods for metrics calculation
    private fun recordResponseTime(responseTime: Long, workType: WorkType, timestamp: Long) {
        responseTimeHistory.add(ResponseTimeMetric(
            timestamp = timestamp,
            responseTime = responseTime,
            workType = workType
        ))
        
        // Maintain history size
        if (responseTimeHistory.size > PERFORMANCE_HISTORY_SIZE) {
            responseTimeHistory.removeFirst()
        }
    }
    
    private fun recordError(workType: WorkType, errorMessage: String, timestamp: Long) {
        errorHistory.add(ErrorEvent(
            timestamp = timestamp,
            workType = workType,
            errorMessage = errorMessage,
            severity = Severity.HIGH
        ))
        
        // Maintain history size
        if (errorHistory.size > PERFORMANCE_HISTORY_SIZE) {
            errorHistory.removeFirst()
        }
    }

    private fun recordPhotoProcessingMetrics(metrics: PhotoProcessingMetrics, timestamp: Long) {
        // Implementation would store photo processing specific metrics
    }

    private fun addToMetricsHistory(snapshot: PerformanceMetricSnapshot) {
        metricsHistory.add(snapshot)
        
        // Maintain history size
        if (metricsHistory.size > PERFORMANCE_HISTORY_SIZE) {
            metricsHistory.removeFirst()
        }
    }

    private fun calculateCurrentErrorRate(): Double {
        val recent = metricsHistory.takeLast(20)
        if (recent.isEmpty()) return 0.0
        
        val errorCount = recent.count { !it.success }
        return (errorCount.toDouble() / recent.size) * 100.0
    }

    private fun calculateCurrentThroughput(): Double {
        val lastMinute = Clock.System.now().toEpochMilliseconds() - 60000L
        return metricsHistory.count { it.timestamp > lastMinute }.toDouble()
    }

    private fun calculateSuccessRate(metrics: List<PerformanceMetricSnapshot>): Double {
        if (metrics.isEmpty()) return 100.0
        val successCount = metrics.count { it.success }
        return (successCount.toDouble() / metrics.size) * 100.0
    }

    private fun calculateMedianResponseTime(times: List<ResponseTimeMetric>): Long {
        if (times.isEmpty()) return 0L
        val sorted = times.map { it.responseTime }.sorted()
        return sorted[sorted.size / 2]
    }

    private fun calculatePercentileResponseTime(times: List<ResponseTimeMetric>, percentile: Double): Long {
        if (times.isEmpty()) return 0L
        val sorted = times.map { it.responseTime }.sorted()
        val index = ((sorted.size - 1) * percentile).toInt()
        return sorted[index]
    }

    private fun calculateResponseTimeByWorkType(times: List<ResponseTimeMetric>): Map<WorkType, Long> {
        return times.groupBy { it.workType }
            .mapValues { (_, metrics) -> 
                metrics.map { it.responseTime }.average().toLong()
            }
    }

    private fun calculatePerformanceTrend(): PerformanceTrend {
        val recent = responseTimeHistory.takeLast(20)
        val older = responseTimeHistory.takeLast(40).take(20)
        
        if (recent.isEmpty() || older.isEmpty()) return PerformanceTrend.STABLE
        
        val recentAvg = recent.map { it.responseTime }.average()
        val olderAvg = older.map { it.responseTime }.average()
        
        return when {
            recentAvg < olderAvg * 0.9 -> PerformanceTrend.IMPROVING
            recentAvg > olderAvg * 1.1 -> PerformanceTrend.DECLINING
            else -> PerformanceTrend.STABLE
        }
    }

    // System metrics (would be implemented with actual system calls)
    private fun getCurrentCpuUtilization(): Double = 75.0
    private fun getCurrentMemoryUtilization(): Double = 68.0
    private fun getCurrentStorageUtilization(): Double = 82.0
    private fun getCurrentNetworkLatency(): Double = 45.0
    private fun getSystemUptime(): Long = 86400000L // 24 hours
    private fun getActiveConnections(): Int = 42
    private fun getActiveUserSessions(): Int = 156
    private fun calculateSystemLoad(): Double = 1.2

    private fun determineOverallHealthStatus(): SystemHealthStatus {
        val cpu = getCurrentCpuUtilization()
        val memory = getCurrentMemoryUtilization()
        val errorRate = calculateCurrentErrorRate()
        
        return when {
            cpu > 90 || memory > 90 || errorRate > 10 -> SystemHealthStatus.FAILING
            cpu > 80 || memory > 80 || errorRate > 5 -> SystemHealthStatus.DEGRADED
            else -> SystemHealthStatus.HEALTHY
        }
    }

    private fun countPhotosProcessed(since: Long): Int = 
        metricsHistory.count { it.timestamp > since && it.photoProcessingMetrics != null }

    private fun calculateAveragePhotoProcessingTime(metrics: List<PerformanceMetricSnapshot>): Long =
        metrics.mapNotNull { it.photoProcessingMetrics?.processingTimeMs }
               .average().takeIf { !it.isNaN() }?.toLong() ?: 0L

    private fun calculateBatchProcessingEfficiency(): Double = 85.0
    private fun calculateAverageCompressionRatio(): Double = 0.75
    private fun calculateUploadSuccessRate(): Double = 98.5
    private fun getCurrentQueueDepth(): Int = 12
    private fun getCurrentProcessingBacklog(): Int = 3

    private fun getActivePerformanceAlerts(): List<PerformanceAlert> = emptyList()
    private fun getSystemWarnings(): List<String> = listOf("High CPU usage detected")
    private fun getMaintenanceAlerts(): List<String> = emptyList()

    private fun generateThroughputDataPoints(metrics: List<PerformanceMetricSnapshot>): List<DataPoint> {
        return metrics.windowed(5, 5, true) { window ->
            DataPoint(
                timestamp = window.last().timestamp,
                value = window.size.toDouble()
            )
        }
    }

    private fun generateErrorRateDataPoints(errors: List<ErrorEvent>): List<DataPoint> {
        return errors.windowed(10, 10, true) { window ->
            DataPoint(
                timestamp = window.last().timestamp,
                value = window.size.toDouble()
            )
        }
    }

    private fun calculateWorkTypeDistribution(metrics: List<PerformanceMetricSnapshot>): List<PieSegment> {
        return metrics.groupBy { it.workType }
            .mapValues { it.value.size }
            .map { (workType, count) ->
                PieSegment(
                    label = workType.name,
                    value = count.toDouble(),
                    percentage = (count.toDouble() / metrics.size) * 100
                )
            }
    }

    private fun calculateSeverityDistribution(metrics: List<PerformanceMetricSnapshot>): List<PieSegment> {
        // Simplified severity distribution
        return listOf(
            PieSegment("Low", 60.0, 60.0),
            PieSegment("Medium", 25.0, 25.0),
            PieSegment("High", 12.0, 12.0),
            PieSegment("Critical", 3.0, 3.0)
        )
    }
}

/**
 * Data classes for dashboard functionality
 */

@Serializable
data class DashboardSnapshot(
    val timestamp: Long,
    val safetyMetrics: SafetyDashboardMetrics,
    val performanceMetrics: PerformanceDashboardMetrics,
    val systemHealthMetrics: SystemHealthDashboardMetrics,
    val photoProcessingMetrics: PhotoProcessingDashboardMetrics,
    val alertSummary: AlertDashboardSummary,
    val chartData: DashboardChartData
)

@Serializable
data class SafetyDashboardMetrics(
    val totalActiveAlerts: Int,
    val criticalAlerts: Int,
    val alertsLast24Hours: Int,
    val complianceEventsLast24Hours: Int,
    val complianceRate: Double,
    val alertsByType: Map<String, Int>,
    val severityDistribution: Map<String, Int>
)

@Serializable
data class PerformanceDashboardMetrics(
    val averageResponseTimeMs: Long,
    val medianResponseTimeMs: Long,
    val p95ResponseTimeMs: Long,
    val p99ResponseTimeMs: Long,
    val throughputPerMinute: Double,
    val errorRate: Double,
    val successRate: Double,
    val responseTimeByWorkType: Map<WorkType, Long>,
    val performanceTrend: PerformanceTrend
)

@Serializable
data class SystemHealthDashboardMetrics(
    val cpuUtilization: Double,
    val memoryUtilization: Double,
    val storageUtilization: Double,
    val networkLatency: Double,
    val systemUptime: Long,
    val activeConnections: Int,
    val healthStatus: SystemHealthStatus
)

@Serializable
data class PhotoProcessingDashboardMetrics(
    val photosProcessedLast24Hours: Int,
    val averageProcessingTimeMs: Long,
    val batchProcessingEfficiency: Double,
    val compressionRatio: Double,
    val uploadSuccessRate: Double,
    val queueDepth: Int,
    val processingBacklog: Int
)

@Serializable
data class AlertDashboardSummary(
    val activePerformanceAlerts: List<PerformanceAlert>,
    val recentErrors: List<ErrorEvent>,
    val systemWarnings: List<String>,
    val maintenanceAlerts: List<String>
)

@Serializable
data class DashboardChartData(
    val responseTimeChart: TimeSeriesChartData,
    val throughputChart: TimeSeriesChartData,
    val errorRateChart: TimeSeriesChartData,
    val workTypeDistribution: PieChartData,
    val severityDistribution: PieChartData
)

@Serializable
data class TimeSeriesChartData(
    val dataPoints: List<DataPoint>,
    val title: String,
    val yAxisLabel: String,
    val color: String
)

@Serializable
data class PieChartData(
    val segments: List<PieSegment>,
    val title: String,
    val colors: List<String>
)

@Serializable
data class DataPoint(
    val timestamp: Long,
    val value: Double
)

@Serializable
data class PieSegment(
    val label: String,
    val value: Double,
    val percentage: Double
)

@Serializable
data class PerformanceMetricSnapshot(
    val timestamp: Long,
    val responseTimeMs: Long,
    val workType: WorkType,
    val success: Boolean,
    val errorRate: Double,
    val throughputPerMinute: Double,
    val photoProcessingMetrics: PhotoProcessingMetrics?
)

@Serializable
data class PhotoProcessingMetrics(
    val processingTimeMs: Long,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val uploadTimeMs: Long,
    val batchSize: Int,
    val queuePosition: Int
)

@Serializable
data class ResponseTimeMetric(
    val timestamp: Long,
    val responseTime: Long,
    val workType: WorkType
)

@Serializable
data class ErrorEvent(
    val timestamp: Long,
    val workType: WorkType,
    val errorMessage: String,
    val severity: Severity
)

@Serializable
data class PerformanceAlert(
    val id: String,
    val type: AlertType,
    val severity: Severity,
    val title: String,
    val message: String,
    val timestamp: Long,
    val workType: WorkType,
    val metricValue: Double,
    val threshold: Double
)

@Serializable
data class SystemHealthSnapshot(
    val timestamp: Long,
    val overallHealth: SystemHealthStatus,
    val componentStatuses: Map<String, ComponentHealth>,
    val resourceUtilization: ResourceUtilization,
    val activeSessions: Int,
    val systemLoad: Double
)

@Serializable
data class ComponentHealth(
    val status: SystemHealthStatus,
    val healthScore: Double,
    val issues: List<String>
)

@Serializable
data class ResourceUtilization(
    val cpu: Double,
    val memory: Double,
    val storage: Double,
    val network: Double
)

@Serializable
data class DashboardInitializationResult(
    val success: Boolean,
    val initializationTimeMs: Long,
    val initialSnapshot: DashboardSnapshot?,
    val enabledFeatures: List<String>,
    val alertingEnabled: Boolean,
    val error: String? = null
)

@Serializable
enum class SystemHealthStatus {
    HEALTHY,
    DEGRADED,
    FAILING,
    DOWN
}


@Serializable
enum class PerformanceTrend {
    IMPROVING,
    STABLE,
    DECLINING
}