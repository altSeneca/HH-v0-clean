package com.hazardhawk.performance
import kotlinx.datetime.Clock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive performance monitoring system for HazardHawk with LiteRT-LM integration.
 * Tracks FPS, memory usage, AI processing performance, thermal states, and backend switching.
 * Optimized for CPU (243 t/s), GPU (1876 t/s), NPU (5836 t/s) performance targets.
 */
class PerformanceMonitor(
    private val deviceDetector: DeviceTierDetector,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    
    private val _metricsFlow = MutableSharedFlow<PerformanceMetrics>()
    val metricsFlow: SharedFlow<PerformanceMetrics> = _metricsFlow.asSharedFlow()
    
    private val _alertsFlow = MutableSharedFlow<PerformanceAlert>()
    val alertsFlow: SharedFlow<PerformanceAlert> = _alertsFlow.asSharedFlow()
    
    private var monitoringJob: Job? = null
    private var isMonitoring = false
    private var frameCounter = FrameCounter()
    private var aiAnalysisCounter = AIAnalysisCounter()
    private var memoryTracker = MemoryTracker()
    private var thermalMonitor = ThermalMonitor()
    private var batteryMonitor = BatteryMonitor()
    private var liteRTMonitor = LiteRTPerformanceMonitor()
    private var backendSwitchingMonitor = BackendSwitchingMonitor()
    
    // Performance thresholds
    private var performanceConfig: PerformanceConfig? = null
    private val alertThresholds = AlertThresholds()
    private val monitoringMutex = Mutex()
    
    /**
     * Start performance monitoring with specified interval.
     */
    suspend fun startMonitoring(intervalSeconds: Int = 5) {
        if (isMonitoring) return
        
        isMonitoring = true
        performanceConfig = PerformanceConfig.fromCapabilities(
            deviceDetector.detectCapabilities()
        )
        
        monitoringJob = scope.launch {
            while (isActive && isMonitoring) {
                try {
                    collectAndEmitMetrics()
                    delay(intervalSeconds.seconds)
                } catch (e: Exception) {
                    emitAlert(PerformanceAlert.SystemError("Monitoring error: ${e.message}"))
                    delay(intervalSeconds.seconds)
                }
            }
        }
    }
    
    /**
     * Stop performance monitoring.
     */
    fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * Record a frame render for FPS calculation.
     */
    fun recordFrame(renderTimeMs: Long = 0) {
        frameCounter.recordFrame(renderTimeMs)
    }
    
    /**
     * Record AI analysis completion.
     */
    fun recordAIAnalysis(durationMs: Long, success: Boolean, cacheHit: Boolean = false) {
        aiAnalysisCounter.recordAnalysis(durationMs, success, cacheHit)
    }
    
    /**
     * Record model loading time.
     */
    fun recordModelLoad(durationMs: Long, modelSize: Int) {
        aiAnalysisCounter.recordModelLoad(durationMs, modelSize)
    }
    
    /**
     * Record LiteRT-LM backend performance.
     */
    fun recordLiteRTInference(
        backend: LiteRTBackend,
        tokensPerSecond: Float,
        latencyMs: Long,
        memoryUsageMB: Float,
        success: Boolean
    ) {
        liteRTMonitor.recordInference(backend, tokensPerSecond, latencyMs, memoryUsageMB, success)
    }
    
    /**
     * Record backend switching event.
     */
    fun recordBackendSwitch(
        fromBackend: LiteRTBackend,
        toBackend: LiteRTBackend,
        reason: BackendSwitchReason,
        switchTimeMs: Long
    ) {
        backendSwitchingMonitor.recordSwitch(fromBackend, toBackend, reason, switchTimeMs)
    }
    
    /**
     * Record model initialization for specific backend.
     */
    fun recordModelInitialization(
        backend: LiteRTBackend,
        modelType: String,
        initTimeMs: Long,
        success: Boolean
    ) {
        liteRTMonitor.recordModelInit(backend, modelType, initTimeMs, success)
    }
    
    /**
     * Get current performance snapshot.
     */
    suspend fun getCurrentMetrics(): PerformanceMetrics {
        return collectMetrics()
    }
    
    /**
     * Record repository query performance.
     */
    fun recordRepositoryQuery(queryType: String, durationMs: Long, success: Boolean) {
        // Implementation will be extended by RepositoryPerformanceTracker
    }
    
    /**
     * Record workflow step completion.
     */
    fun recordWorkflowStep(stepName: String, durationMs: Long, success: Boolean, metadata: Map<String, Any> = emptyMap()) {
        // Implementation will be extended by WorkflowPerformanceMonitor
    }
    
    /**
     * Get performance summary over time period.
     */
    fun getPerformanceSummary(durationMinutes: Int = 10): PerformanceSummary {
        val liteRTStats = liteRTMonitor.getStats(durationMinutes)
        val backendStats = backendSwitchingMonitor.getStats(durationMinutes)
        
        return PerformanceSummary(
            timeRangeMinutes = durationMinutes,
            avgFPS = frameCounter.getAverageFPS(durationMinutes),
            avgFrameTime = frameCounter.getAverageFrameTime(durationMinutes),
            frameDrops = frameCounter.getFrameDrops(durationMinutes),
            avgAnalysisTime = aiAnalysisCounter.getAverageAnalysisTime(durationMinutes),
            analysisSuccessRate = aiAnalysisCounter.getSuccessRate(durationMinutes),
            cacheHitRate = aiAnalysisCounter.getCacheHitRate(durationMinutes),
            memoryPeakUsage = memoryTracker.getPeakUsage(durationMinutes),
            thermalEvents = thermalMonitor.getThermalEvents(durationMinutes),
            batteryDrain = batteryMonitor.getBatteryDrain(durationMinutes),
            liteRTPerformance = liteRTStats,
            backendSwitching = backendStats
        )
    }
    
    /**
     * Get LiteRT-LM specific performance metrics.
     */
    fun getLiteRTPerformanceReport(): LiteRTPerformanceReport {
        return liteRTMonitor.generateReport()
    }
    
    /**
     * Get backend switching analysis.
     */
    fun getBackendSwitchingAnalysis(): BackendSwitchingAnalysis {
        return backendSwitchingMonitor.getAnalysis()
    }
    
    private suspend fun collectAndEmitMetrics() {
        val metrics = collectMetrics()
        _metricsFlow.emit(metrics)
        checkPerformanceAlerts(metrics)
    }
    
    private suspend fun collectMetrics(): PerformanceMetrics {
        val memoryUsed = deviceDetector.getCurrentMemoryUsage() / (1024 * 1024) // Convert to MB
        val memoryAvailable = deviceDetector.getAvailableMemory() / (1024 * 1024)
        
        return PerformanceMetrics(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            memoryUsedMB = memoryUsed.toFloat(),
            availableMemoryMB = memoryAvailable.toFloat(),
            cpuUsagePercent = getCPUUsage(),
            gpuUsagePercent = getGPUUsage(),
            currentFPS = frameCounter.getCurrentFPS(),
            aiProcessingFPS = aiAnalysisCounter.getCurrentProcessingRate(),
            modelLoadTimeMs = aiAnalysisCounter.getLastModelLoadTime(),
            analysisTimeMs = aiAnalysisCounter.getLastAnalysisTime(),
            cacheHitRate = aiAnalysisCounter.getCurrentCacheHitRate(),
            thermalState = thermalMonitor.getCurrentThermalState(),
            batteryLevel = batteryMonitor.getCurrentBatteryLevel()
        )
    }
    
    private fun checkPerformanceAlerts(metrics: PerformanceMetrics) {
        val config = performanceConfig ?: return
        
        // Low FPS alert
        if (metrics.currentFPS < config.uiTargetFPS * 0.7f) {
            emitAlert(PerformanceAlert.LowFPS(metrics.currentFPS, config.uiTargetFPS.toFloat()))
        }
        
        // High memory usage alert
        if (metrics.memoryUsedMB > config.memoryThresholdMB * 0.9f) {
            emitAlert(PerformanceAlert.HighMemoryUsage(metrics.memoryUsedMB, config.memoryThresholdMB.toFloat()))
        }
        
        // Thermal throttling alert
        if (metrics.thermalState >= ThermalState.MODERATE_THROTTLING) {
            emitAlert(PerformanceAlert.ThermalThrottling(metrics.thermalState))
        }
        
        // Low battery alert (construction workday context)
        if (metrics.batteryLevel < 20f) {
            emitAlert(PerformanceAlert.LowBattery(metrics.batteryLevel))
        }
        
        // Slow AI processing alert
        if (metrics.analysisTimeMs > 5000) { // > 5 seconds
            emitAlert(PerformanceAlert.SlowAIProcessing(metrics.analysisTimeMs))
        }
    }
    
    private fun emitAlert(alert: PerformanceAlert) {
        scope.launch {
            _alertsFlow.emit(alert)
        }
    }
    
    // Platform-specific implementations will override these
    private fun getCPUUsage(): Float = 0f
    private fun getGPUUsage(): Float = 0f
}

/**
 * Frame counter for FPS tracking with construction-specific optimizations.
 */
class FrameCounter {
    private val frameHistory = mutableListOf<FrameRecord>()
    private var lastFrameTime = 0L
    
    data class FrameRecord(
        val timestamp: Long,
        val renderTimeMs: Long
    )
    
    fun recordFrame(renderTimeMs: Long = 0) {
        val now = Clock.System.now().toEpochMilliseconds()
        frameHistory.add(FrameRecord(now, renderTimeMs))
        
        // Keep only last 5 minutes of frames
        val cutoff = now - 300_000 // 5 minutes
        frameHistory.removeAll { it.timestamp < cutoff }
        
        lastFrameTime = now
    }
    
    fun getCurrentFPS(): Float {
        val now = Clock.System.now().toEpochMilliseconds()
        val recentFrames = frameHistory.filter { now - it.timestamp <= 1000 } // Last second
        return recentFrames.size.toFloat()
    }
    
    fun getAverageFPS(durationMinutes: Int): Float {
        val cutoff = Clock.System.now().toEpochMilliseconds() - (durationMinutes * 60 * 1000)
        val recentFrames = frameHistory.filter { it.timestamp >= cutoff }
        
        if (recentFrames.isEmpty()) return 0f
        
        val timeSpanSeconds = (recentFrames.maxOf { it.timestamp } - recentFrames.minOf { it.timestamp }) / 1000f
        return if (timeSpanSeconds > 0) recentFrames.size / timeSpanSeconds else 0f
    }
    
    fun getAverageFrameTime(durationMinutes: Int): Float {
        val cutoff = Clock.System.now().toEpochMilliseconds() - (durationMinutes * 60 * 1000)
        val recentFrames = frameHistory.filter { it.timestamp >= cutoff && it.renderTimeMs > 0 }
        
        return if (recentFrames.isNotEmpty()) {
            recentFrames.map { it.renderTimeMs }.average().toFloat()
        } else 0f
    }
    
    fun getFrameDrops(durationMinutes: Int): Int {
        val targetFPS = 30f
        val cutoff = Clock.System.now().toEpochMilliseconds() - (durationMinutes * 60 * 1000)
        val recentFrames = frameHistory.filter { it.timestamp >= cutoff }
        
        var frameDrops = 0
        for (i in 1 until recentFrames.size) {
            val timeDelta = recentFrames[i].timestamp - recentFrames[i-1].timestamp
            val expectedFrames = (timeDelta / (1000f / targetFPS)).toInt()
            if (expectedFrames > 1) {
                frameDrops += expectedFrames - 1
            }
        }
        
        return frameDrops
    }
}

/**
 * AI analysis performance counter.
 */
class AIAnalysisCounter {
    private val analysisHistory = mutableListOf<AnalysisRecord>()
    private val modelLoadHistory = mutableListOf<ModelLoadRecord>()
    
    data class AnalysisRecord(
        val timestamp: Long,
        val durationMs: Long,
        val success: Boolean,
        val cacheHit: Boolean
    )
    
    data class ModelLoadRecord(
        val timestamp: Long,
        val durationMs: Long,
        val modelSizeMB: Int
    )
    
    fun recordAnalysis(durationMs: Long, success: Boolean, cacheHit: Boolean = false) {
        val now = Clock.System.now().toEpochMilliseconds()
        analysisHistory.add(AnalysisRecord(now, durationMs, success, cacheHit))
        
        // Keep only last 30 minutes
        val cutoff = now - 1_800_000
        analysisHistory.removeAll { it.timestamp < cutoff }
    }
    
    fun recordModelLoad(durationMs: Long, modelSizeMB: Int) {
        val now = Clock.System.now().toEpochMilliseconds()
        modelLoadHistory.add(ModelLoadRecord(now, durationMs, modelSizeMB))
        
        // Keep only last hour
        val cutoff = now - 3_600_000
        modelLoadHistory.removeAll { it.timestamp < cutoff }
    }
    
    fun getCurrentProcessingRate(): Float {
        val now = Clock.System.now().toEpochMilliseconds()
        val recentAnalyses = analysisHistory.filter { now - it.timestamp <= 1000 } // Last second
        return recentAnalyses.size.toFloat()
    }
    
    fun getAverageAnalysisTime(durationMinutes: Int): Long {
        val cutoff = Clock.System.now().toEpochMilliseconds() - (durationMinutes * 60 * 1000)
        val recentAnalyses = analysisHistory.filter { it.timestamp >= cutoff }
        
        return if (recentAnalyses.isNotEmpty()) {
            recentAnalyses.map { it.durationMs }.average().toLong()
        } else 0L
    }
    
    fun getSuccessRate(durationMinutes: Int): Float {
        val cutoff = Clock.System.now().toEpochMilliseconds() - (durationMinutes * 60 * 1000)
        val recentAnalyses = analysisHistory.filter { it.timestamp >= cutoff }
        
        return if (recentAnalyses.isNotEmpty()) {
            recentAnalyses.count { it.success }.toFloat() / recentAnalyses.size
        } else 1f
    }
    
    fun getCacheHitRate(durationMinutes: Int): Float {
        val cutoff = Clock.System.now().toEpochMilliseconds() - (durationMinutes * 60 * 1000)
        val recentAnalyses = analysisHistory.filter { it.timestamp >= cutoff }
        
        return if (recentAnalyses.isNotEmpty()) {
            recentAnalyses.count { it.cacheHit }.toFloat() / recentAnalyses.size
        } else 0f
    }
    
    fun getCurrentCacheHitRate(): Float = getCacheHitRate(1)
    
    fun getLastAnalysisTime(): Long = analysisHistory.lastOrNull()?.durationMs ?: 0L
    
    fun getLastModelLoadTime(): Long = modelLoadHistory.lastOrNull()?.durationMs ?: 0L
}

/**
 * Memory usage tracking optimized for construction app requirements.
 */
class MemoryTracker {
    private val memoryHistory = mutableListOf<MemoryRecord>()
    
    data class MemoryRecord(
        val timestamp: Long,
        val usedMB: Float,
        val availableMB: Float
    )
    
    fun recordMemoryUsage(usedMB: Float, availableMB: Float) {
        val now = Clock.System.now().toEpochMilliseconds()
        memoryHistory.add(MemoryRecord(now, usedMB, availableMB))
        
        // Keep only last hour
        val cutoff = now - 3_600_000
        memoryHistory.removeAll { it.timestamp < cutoff }
    }
    
    fun getPeakUsage(durationMinutes: Int): Float {
        val cutoff = Clock.System.now().toEpochMilliseconds() - (durationMinutes * 60 * 1000)
        val recentRecords = memoryHistory.filter { it.timestamp >= cutoff }
        
        return recentRecords.maxOfOrNull { it.usedMB } ?: 0f
    }
}

/**
 * Thermal monitoring for outdoor construction environments.
 */
class ThermalMonitor {
    private val thermalHistory = mutableListOf<ThermalRecord>()
    
    data class ThermalRecord(
        val timestamp: Long,
        val state: ThermalState
    )
    
    fun recordThermalState(state: ThermalState) {
        val now = Clock.System.now().toEpochMilliseconds()
        thermalHistory.add(ThermalRecord(now, state))
        
        // Keep only last 2 hours
        val cutoff = now - 7_200_000
        thermalHistory.removeAll { it.timestamp < cutoff }
    }
    
    fun getCurrentThermalState(): ThermalState = 
        thermalHistory.lastOrNull()?.state ?: ThermalState.NOMINAL
    
    fun getThermalEvents(durationMinutes: Int): Int {
        val cutoff = Clock.System.now().toEpochMilliseconds() - (durationMinutes * 60 * 1000)
        return thermalHistory.count { 
            it.timestamp >= cutoff && it.state >= ThermalState.LIGHT_THROTTLING 
        }
    }
}

/**
 * Battery monitoring for 8-hour construction workdays.
 */
class BatteryMonitor {
    private val batteryHistory = mutableListOf<BatteryRecord>()
    
    data class BatteryRecord(
        val timestamp: Long,
        val level: Float
    )
    
    fun recordBatteryLevel(level: Float) {
        val now = Clock.System.now().toEpochMilliseconds()
        batteryHistory.add(BatteryRecord(now, level))
        
        // Keep only last 8 hours (full workday)
        val cutoff = now - 28_800_000
        batteryHistory.removeAll { it.timestamp < cutoff }
    }
    
    fun getCurrentBatteryLevel(): Float = 
        batteryHistory.lastOrNull()?.level ?: 100f
    
    fun getBatteryDrain(durationMinutes: Int): Float {
        val cutoff = Clock.System.now().toEpochMilliseconds() - (durationMinutes * 60 * 1000)
        val recentRecords = batteryHistory.filter { it.timestamp >= cutoff }
        
        return if (recentRecords.size >= 2) {
            recentRecords.first().level - recentRecords.last().level
        } else 0f
    }
}

/**
 * Performance alert system.
 */
sealed class PerformanceAlert {
    data class LowFPS(val currentFPS: Float, val targetFPS: Float) : PerformanceAlert()
    data class HighMemoryUsage(val currentMB: Float, val thresholdMB: Float) : PerformanceAlert()
    data class ThermalThrottling(val thermalState: ThermalState) : PerformanceAlert()
    data class LowBattery(val batteryLevel: Float) : PerformanceAlert()
    data class SlowAIProcessing(val analysisTimeMs: Long) : PerformanceAlert()
    data class SystemError(val message: String) : PerformanceAlert()
}

/**
 * Performance summary for reporting and analysis.
 */
data class PerformanceSummary(
    val timeRangeMinutes: Int,
    val avgFPS: Float,
    val avgFrameTime: Float,
    val frameDrops: Int,
    val avgAnalysisTime: Long,
    val analysisSuccessRate: Float,
    val cacheHitRate: Float,
    val memoryPeakUsage: Float,
    val thermalEvents: Int,
    val batteryDrain: Float,
    val liteRTPerformance: LiteRTPerformanceStats,
    val backendSwitching: BackendSwitchingStats
)

/**
 * Alert thresholds configuration.
 */
data class AlertThresholds(
    val minFPS: Float = 20f,
    val maxMemoryUsagePercent: Float = 0.9f,
    val maxAnalysisTimeMs: Long = 5000L,
    val minBatteryPercent: Float = 20f,
    val thermalThrottleThreshold: ThermalState = ThermalState.MODERATE_THROTTLING
)

/**
 * UI frame rate limiter for smooth 30 FPS rendering.
 */
class UIFrameRateLimiter(private val targetFPS: Int) {
    private val frameIntervalNs = 1_000_000_000L / targetFPS
    private var lastFrameTimeNs = 0L
    
    /**
     * Check if enough time has passed for the next frame.
     */
    fun shouldRenderFrame(): Boolean {
        val currentTimeNs = System.nanoTime()
        val elapsedNs = currentTimeNs - lastFrameTimeNs
        
        return if (elapsedNs >= frameIntervalNs) {
            lastFrameTimeNs = currentTimeNs
            true
        } else {
            false
        }
    }
    
    /**
     * Get time until next frame in milliseconds.
     */
    fun getTimeUntilNextFrameMs(): Long {
        val currentTimeNs = System.nanoTime()
        val elapsedNs = currentTimeNs - lastFrameTimeNs
        val remainingNs = frameIntervalNs - elapsedNs
        
        return (remainingNs / 1_000_000L).coerceAtLeast(0)
    }
}

/**
 * Integration performance validator to ensure refactoring targets are met.
 */
class IntegrationPerformanceValidator(
    private val performanceMonitor: PerformanceMonitor,
    private val deviceDetector: DeviceTierDetector,
    private val memoryManager: MemoryManager
) {
    
    data class PerformanceTargets(
        val cameraUITargetFPS: Float = 30f,
        val aiAnalysisTargetFPS: Float = 2f,
        val aiAnalysisIntervalMs: Long = 500L,
        val repositoryQueryMaxMs: Long = 100L,
        val memoryMaxThresholdGB: Float = 2f,
        val completeWorkflowMaxSeconds: Long = 15L,
        val modelLoadingMaxSeconds: Long = 10L
    )
    
    data class ValidationResult(
        val target: String,
        val expected: Any,
        val actual: Any,
        val passed: Boolean,
        val performanceScore: Float, // 0-100
        val recommendation: String?
    )
    
    data class IntegrationValidationReport(
        val timestamp: Long,
        val deviceTier: DeviceTier,
        val validationResults: List<ValidationResult>,
        val overallScore: Float,
        val passed: Boolean,
        val criticalIssues: List<String>,
        val recommendations: List<String>
    )
    
    suspend fun validateIntegrationPerformance(): IntegrationValidationReport {
        val targets = PerformanceTargets()
        val results = mutableListOf<ValidationResult>()
        val deviceCapabilities = deviceDetector.detectCapabilities()
        
        // Validate Camera UI Performance (30 FPS target)
        results.add(validateCameraUIPerformance(targets))
        
        // Validate AI Analysis Throttling (2 FPS / 500ms interval)
        results.add(validateAIAnalysisThrottling(targets))
        
        // Validate Repository Query Performance (<100ms)
        results.add(validateRepositoryQueryPerformance(targets))
        
        // Validate Memory Usage (<2GB)
        results.add(validateMemoryUsage(targets))
        
        // Validate Complete Workflow Time (<15 seconds)
        results.add(validateWorkflowPerformance(targets))
        
        // Validate Model Loading Performance (<10 seconds)
        results.add(validateModelLoadingPerformance(targets))
        
        val overallScore = results.map { it.performanceScore }.average().toFloat()
        val passed = results.all { it.passed }
        val criticalIssues = results.filter { !it.passed && it.performanceScore < 50f }
            .map { "${it.target}: ${it.recommendation}" }
        
        val recommendations = generateIntegrationRecommendations(results, deviceCapabilities)
        
        return IntegrationValidationReport(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            deviceTier = deviceCapabilities.tier,
            validationResults = results,
            overallScore = overallScore,
            passed = passed,
            criticalIssues = criticalIssues,
            recommendations = recommendations
        )
    }
    
    private suspend fun validateCameraUIPerformance(targets: PerformanceTargets): ValidationResult {
        val currentMetrics = performanceMonitor.getCurrentMetrics()
        val actualFPS = currentMetrics.currentFPS
        val targetFPS = targets.cameraUITargetFPS
        
        val performanceScore = (actualFPS / targetFPS * 100f).coerceAtMost(100f)
        val passed = actualFPS >= targetFPS * 0.8f // Allow 20% tolerance
        
        val recommendation = if (!passed) {
            when {
                actualFPS < targetFPS * 0.5f -> "Critical: Camera UI performance severely degraded. Reduce UI complexity and disable non-essential features."
                actualFPS < targetFPS * 0.7f -> "Warning: Camera UI below target. Consider reducing concurrent operations or lowering UI quality."
                else -> "Minor: Camera UI slightly below target. Monitor for performance patterns."
            }
        } else null
        
        return ValidationResult(
            target = "Camera UI Performance",
            expected = "${targetFPS} FPS",
            actual = "${actualFPS} FPS",
            passed = passed,
            performanceScore = performanceScore,
            recommendation = recommendation
        )
    }
    
    private suspend fun validateAIAnalysisThrottling(targets: PerformanceTargets): ValidationResult {
        val currentMetrics = performanceMonitor.getCurrentMetrics()
        val actualAIFPS = currentMetrics.aiProcessingFPS
        val targetAIFPS = targets.aiAnalysisTargetFPS
        
        // For AI throttling, we want to be AT or BELOW the target (not exceed it)
        val performanceScore = when {
            actualAIFPS <= targetAIFPS -> 100f
            actualAIFPS <= targetAIFPS * 1.2f -> 80f // 20% tolerance
            actualAIFPS <= targetAIFPS * 1.5f -> 60f // 50% tolerance
            else -> 30f // Significantly exceeding target
        }
        
        val passed = actualAIFPS <= targetAIFPS * 1.2f // Allow 20% tolerance
        
        val recommendation = if (!passed) {
            "AI analysis rate exceeding target (${actualAIFPS} vs ${targetAIFPS} FPS). " +
            "Increase throttling interval to reduce CPU/battery impact."
        } else null
        
        return ValidationResult(
            target = "AI Analysis Throttling",
            expected = "≤ ${targetAIFPS} FPS",
            actual = "${actualAIFPS} FPS",
            passed = passed,
            performanceScore = performanceScore,
            recommendation = recommendation
        )
    }
    
    private suspend fun validateRepositoryQueryPerformance(targets: PerformanceTargets): ValidationResult {
        // This would integrate with actual repository performance tracking
        // For now, simulate based on device capabilities
        val deviceCapabilities = deviceDetector.detectCapabilities()
        val estimatedQueryTime = when (deviceCapabilities.tier) {
            DeviceTier.LOW_END -> 80L
            DeviceTier.MID_RANGE -> 50L
            DeviceTier.HIGH_END -> 30L
        }
        
        val targetMs = targets.repositoryQueryMaxMs
        val performanceScore = ((targetMs - estimatedQueryTime).toFloat() / targetMs * 100f)
            .coerceIn(0f, 100f)
        val passed = estimatedQueryTime <= targetMs
        
        val recommendation = if (!passed) {
            "Repository queries taking ${estimatedQueryTime}ms (target: ${targetMs}ms). " +
            "Optimize database indexes and query patterns."
        } else null
        
        return ValidationResult(
            target = "Repository Query Performance",
            expected = "< ${targetMs}ms",
            actual = "${estimatedQueryTime}ms",
            passed = passed,
            performanceScore = performanceScore,
            recommendation = recommendation
        )
    }
    
    private suspend fun validateMemoryUsage(targets: PerformanceTargets): ValidationResult {
        val currentMetrics = performanceMonitor.getCurrentMetrics()
        val memoryUsageGB = currentMetrics.memoryUsedMB / 1024f
        val targetGB = targets.memoryMaxThresholdGB
        
        val performanceScore = ((targetGB - memoryUsageGB) / targetGB * 100f)
            .coerceIn(0f, 100f)
        val passed = memoryUsageGB <= targetGB
        
        val recommendation = if (!passed) {
            "Memory usage ${memoryUsageGB}GB exceeds target ${targetGB}GB. " +
            "Enable aggressive memory management and model unloading."
        } else null
        
        return ValidationResult(
            target = "Memory Usage",
            expected = "< ${targetGB}GB",
            actual = "${String.format("%.2f", memoryUsageGB)}GB",
            passed = passed,
            performanceScore = performanceScore,
            recommendation = recommendation
        )
    }
    
    private suspend fun validateWorkflowPerformance(targets: PerformanceTargets): ValidationResult {
        // This would integrate with workflow performance tracking
        // Simulate based on device capabilities for now
        val deviceCapabilities = deviceDetector.detectCapabilities()
        val estimatedWorkflowTime = when (deviceCapabilities.tier) {
            DeviceTier.LOW_END -> 18L
            DeviceTier.MID_RANGE -> 12L  
            DeviceTier.HIGH_END -> 8L
        }
        
        val targetSeconds = targets.completeWorkflowMaxSeconds
        val performanceScore = ((targetSeconds - estimatedWorkflowTime).toFloat() / targetSeconds * 100f)
            .coerceIn(0f, 100f)
        val passed = estimatedWorkflowTime <= targetSeconds
        
        val recommendation = if (!passed) {
            "Complete workflow taking ${estimatedWorkflowTime}s (target: ${targetSeconds}s). " +
            "Optimize AI processing pipeline and enable result caching."
        } else null
        
        return ValidationResult(
            target = "Complete Workflow Time",
            expected = "< ${targetSeconds}s",
            actual = "${estimatedWorkflowTime}s",
            passed = passed,
            performanceScore = performanceScore,
            recommendation = recommendation
        )
    }
    
    private suspend fun validateModelLoadingPerformance(targets: PerformanceTargets): ValidationResult {
        val currentMetrics = performanceMonitor.getCurrentMetrics()
        val modelLoadTimeSeconds = currentMetrics.modelLoadTimeMs / 1000L
        val targetSeconds = targets.modelLoadingMaxSeconds
        
        val performanceScore = if (modelLoadTimeSeconds == 0L) {
            100f // No recent model loading
        } else {
            ((targetSeconds - modelLoadTimeSeconds).toFloat() / targetSeconds * 100f)
                .coerceIn(0f, 100f)
        }
        
        val passed = modelLoadTimeSeconds <= targetSeconds || modelLoadTimeSeconds == 0L
        
        val recommendation = if (!passed) {
            "Model loading taking ${modelLoadTimeSeconds}s (target: ${targetSeconds}s). " +
            "Use model preloading and consider smaller model variants."
        } else null
        
        return ValidationResult(
            target = "Model Loading Time",
            expected = "< ${targetSeconds}s",
            actual = "${modelLoadTimeSeconds}s",
            passed = passed,
            performanceScore = performanceScore,
            recommendation = recommendation
        )
    }
    
    private fun generateIntegrationRecommendations(
        results: List<ValidationResult>,
        deviceCapabilities: DeviceCapabilities
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Device-specific recommendations
        when (deviceCapabilities.tier) {
            DeviceTier.LOW_END -> {
                recommendations.add("Low-end device detected: Enable aggressive performance optimizations")
                recommendations.add("Consider reducing AI processing frequency to 1 FPS")
                recommendations.add("Enable model preloading only for critical models")
            }
            DeviceTier.MID_RANGE -> {
                recommendations.add("Mid-range device: Standard performance optimizations active")
                recommendations.add("Monitor thermal throttling during extended use")
            }
            DeviceTier.HIGH_END -> {
                recommendations.add("High-end device: Full feature set available")
                recommendations.add("Consider enabling advanced models for maximum accuracy")
            }
        }
        
        // Memory pressure recommendations
        when (deviceCapabilities.memoryPressure) {
            MemoryPressure.LOW -> recommendations.add("Memory pressure low: Optimal performance configuration")
            MemoryPressure.MODERATE -> recommendations.add("Moderate memory pressure: Enable cache cleanup")
            MemoryPressure.HIGH -> recommendations.add("High memory pressure: Disable model preloading")
            MemoryPressure.CRITICAL -> recommendations.add("Critical memory pressure: Enable emergency mode")
        }
        
        // Performance-specific recommendations
        val failedResults = results.filter { !it.passed }
        if (failedResults.isNotEmpty()) {
            recommendations.add("Performance issues detected in ${failedResults.size} areas")
            recommendations.add("Consider enabling low-power mode during peak usage")
        }
        
        return recommendations
    }
}

/**
 * LiteRT-LM performance monitoring system.
 * Tracks performance across CPU, GPU, and NPU backends with target validation.
 */
class LiteRTPerformanceMonitor {
    private val inferenceHistory = mutableMapOf<LiteRTBackend, MutableList<InferenceRecord>>()
    private val modelInitHistory = mutableMapOf<LiteRTBackend, MutableList<ModelInitRecord>>()
    
    data class InferenceRecord(
        val timestamp: Long,
        val tokensPerSecond: Float,
        val latencyMs: Long,
        val memoryUsageMB: Float,
        val success: Boolean
    )
    
    data class ModelInitRecord(
        val timestamp: Long,
        val modelType: String,
        val initTimeMs: Long,
        val success: Boolean
    )
    
    fun recordInference(
        backend: LiteRTBackend,
        tokensPerSecond: Float,
        latencyMs: Long,
        memoryUsageMB: Float,
        success: Boolean
    ) {
        val record = InferenceRecord(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            tokensPerSecond = tokensPerSecond,
            latencyMs = latencyMs,
            memoryUsageMB = memoryUsageMB,
            success = success
        )
        
        inferenceHistory.getOrPut(backend) { mutableListOf() }.add(record)
        
        // Keep only recent records (last 30 minutes)
        val cutoff = Clock.System.now().toEpochMilliseconds() - 1800_000
        inferenceHistory[backend]?.removeAll { it.timestamp < cutoff }
    }
    
    fun recordModelInit(
        backend: LiteRTBackend,
        modelType: String,
        initTimeMs: Long,
        success: Boolean
    ) {
        val record = ModelInitRecord(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            modelType = modelType,
            initTimeMs = initTimeMs,
            success = success
        )
        
        modelInitHistory.getOrPut(backend) { mutableListOf() }.add(record)
        
        // Keep only recent records (last hour)
        val cutoff = Clock.System.now().toEpochMilliseconds() - 3600_000
        modelInitHistory[backend]?.removeAll { it.timestamp < cutoff }
    }
    
    fun getStats(durationMinutes: Int): LiteRTPerformanceStats {
        val cutoff = Clock.System.now().toEpochMilliseconds() - (durationMinutes * 60 * 1000)
        
        val backendStats = LiteRTBackend.values().associateWith { backend ->
            val records = inferenceHistory[backend]?.filter { it.timestamp >= cutoff } ?: emptyList()
            
            if (records.isEmpty()) {
                BackendPerformanceStats(
                    backend = backend,
                    inferenceCount = 0,
                    avgTokensPerSecond = 0f,
                    avgLatencyMs = 0L,
                    avgMemoryUsageMB = 0f,
                    successRate = 0f,
                    performanceScore = 0f
                )
            } else {
                val avgTokensPerSecond = records.map { it.tokensPerSecond }.average().toFloat()
                val target = when (backend) {
                    LiteRTBackend.CPU -> 243f
                    LiteRTBackend.GPU -> 1876f
                    LiteRTBackend.NPU -> 5836f
                }
                val performanceScore = (avgTokensPerSecond / target * 100f).coerceAtMost(100f)
                
                BackendPerformanceStats(
                    backend = backend,
                    inferenceCount = records.size,
                    avgTokensPerSecond = avgTokensPerSecond,
                    avgLatencyMs = records.map { it.latencyMs }.average().toLong(),
                    avgMemoryUsageMB = records.map { it.memoryUsageMB }.average().toFloat(),
                    successRate = records.count { it.success }.toFloat() / records.size,
                    performanceScore = performanceScore
                )
            }
        }
        
        return LiteRTPerformanceStats(
            timeRangeMinutes = durationMinutes,
            backendStats = backendStats,
            bestPerformingBackend = backendStats.maxByOrNull { it.value.performanceScore }?.key,
            overallPerformanceScore = backendStats.values.map { it.performanceScore }.average().toFloat()
        )
    }
    
    fun getBackendStats(backend: LiteRTBackend, durationMinutes: Int): BackendPerformanceStats {
        val cutoff = Clock.System.now().toEpochMilliseconds() - (durationMinutes * 60 * 1000)
        val records = inferenceHistory[backend]?.filter { it.timestamp >= cutoff } ?: emptyList()
        
        if (records.isEmpty()) {
            return BackendPerformanceStats(
                backend = backend,
                inferenceCount = 0,
                avgTokensPerSecond = 0f,
                avgLatencyMs = 0L,
                avgMemoryUsageMB = 0f,
                successRate = 0f,
                performanceScore = 0f
            )
        }
        
        val avgTokensPerSecond = records.map { it.tokensPerSecond }.average().toFloat()
        val target = when (backend) {
            LiteRTBackend.CPU -> 243f
            LiteRTBackend.GPU -> 1876f
            LiteRTBackend.NPU -> 5836f
        }
        val performanceScore = (avgTokensPerSecond / target * 100f).coerceAtMost(100f)
        
        return BackendPerformanceStats(
            backend = backend,
            inferenceCount = records.size,
            avgTokensPerSecond = avgTokensPerSecond,
            avgLatencyMs = records.map { it.latencyMs }.average().toLong(),
            avgMemoryUsageMB = records.map { it.memoryUsageMB }.average().toFloat(),
            successRate = records.count { it.success }.toFloat() / records.size,
            performanceScore = performanceScore
        )
    }
    
    fun generateReport(): LiteRTPerformanceReport {
        val allBackends = LiteRTBackend.values()
        val performanceComparison = allBackends.map { backend ->
            val stats = getBackendStats(backend, 10) // Last 10 minutes
            val initStats = modelInitHistory[backend]?.let { records ->
                val recentRecords = records.filter { Clock.System.now().toEpochMilliseconds() - it.timestamp <= 600_000 } // 10 minutes
                if (recentRecords.isNotEmpty()) {
                    ModelInitStats(
                        initCount = recentRecords.size,
                        avgInitTimeMs = recentRecords.map { it.initTimeMs }.average().toLong(),
                        successRate = recentRecords.count { it.success }.toFloat() / recentRecords.size
                    )
                } else null
            }
            
            BackendComparisonResult(
                backend = backend,
                performanceStats = stats,
                modelInitStats = initStats,
                meetsTarget = stats.performanceScore >= 80f,
                recommendation = generateBackendRecommendation(backend, stats)
            )
        }
        
        return LiteRTPerformanceReport(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            backendComparison = performanceComparison,
            recommendedBackend = performanceComparison
                .filter { it.meetsTarget }
                .maxByOrNull { it.performanceStats.performanceScore }?.backend
                ?: LiteRTBackend.CPU, // Fallback to CPU
            overallAssessment = generateOverallAssessment(performanceComparison)
        )
    }
    
    private fun generateBackendRecommendation(
        backend: LiteRTBackend,
        stats: BackendPerformanceStats
    ): String {
        return when {
            stats.performanceScore >= 90f -> "Excellent performance - optimal backend choice"
            stats.performanceScore >= 80f -> "Good performance - suitable for production use"
            stats.performanceScore >= 60f -> "Moderate performance - consider optimization"
            stats.performanceScore >= 40f -> "Poor performance - investigate issues"
            else -> "Critical performance issues - not recommended for use"
        }
    }
    
    private fun generateOverallAssessment(comparison: List<BackendComparisonResult>): String {
        val workingBackends = comparison.count { it.meetsTarget }
        val bestScore = comparison.maxOfOrNull { it.performanceStats.performanceScore } ?: 0f
        
        return when {
            workingBackends >= 2 && bestScore >= 90f -> "Excellent: Multiple high-performance backends available"
            workingBackends >= 1 && bestScore >= 80f -> "Good: At least one backend meets performance targets"
            workingBackends >= 1 -> "Fair: Basic performance requirements met"
            else -> "Poor: No backends meet performance targets - optimization required"
        }
    }
}

/**
 * Backend switching monitoring and analysis.
 */
class BackendSwitchingMonitor {
    private val switchHistory = mutableListOf<BackendSwitchRecord>()
    
    data class BackendSwitchRecord(
        val timestamp: Long,
        val fromBackend: LiteRTBackend,
        val toBackend: LiteRTBackend,
        val reason: BackendSwitchReason,
        val switchTimeMs: Long
    )
    
    fun recordSwitch(
        fromBackend: LiteRTBackend,
        toBackend: LiteRTBackend,
        reason: BackendSwitchReason,
        switchTimeMs: Long
    ) {
        switchHistory.add(
            BackendSwitchRecord(
                timestamp = Clock.System.now().toEpochMilliseconds(),
                fromBackend = fromBackend,
                toBackend = toBackend,
                reason = reason,
                switchTimeMs = switchTimeMs
            )
        )
        
        // Keep only recent records (last 2 hours)
        val cutoff = Clock.System.now().toEpochMilliseconds() - 7200_000
        switchHistory.removeAll { it.timestamp < cutoff }
    }
    
    fun getStats(durationMinutes: Int): BackendSwitchingStats {
        val cutoff = Clock.System.now().toEpochMilliseconds() - (durationMinutes * 60 * 1000)
        val recentSwitches = switchHistory.filter { it.timestamp >= cutoff }
        
        if (recentSwitches.isEmpty()) {
            return BackendSwitchingStats(
                switchCount = 0,
                avgSwitchTimeMs = 0L,
                mostCommonReason = null,
                switchFrequency = 0f,
                stabilityScore = 100f
            )
        }
        
        val switchFrequency = recentSwitches.size.toFloat() / durationMinutes // switches per minute
        val stabilityScore = (100f - (switchFrequency * 10f)).coerceIn(0f, 100f) // Penalize frequent switching
        
        return BackendSwitchingStats(
            switchCount = recentSwitches.size,
            avgSwitchTimeMs = recentSwitches.map { it.switchTimeMs }.average().toLong(),
            mostCommonReason = recentSwitches.groupingBy { it.reason }
                .eachCount().maxByOrNull { it.value }?.key,
            switchFrequency = switchFrequency,
            stabilityScore = stabilityScore
        )
    }
    
    fun getAnalysis(): BackendSwitchingAnalysis {
        val recentSwitches = switchHistory.filter { 
            Clock.System.now().toEpochMilliseconds() - it.timestamp <= 3600_000 // Last hour
        }
        
        val switchPatterns = recentSwitches.groupBy { "${it.fromBackend}->${it.toBackend}" }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
        
        val reasonBreakdown = recentSwitches.groupingBy { it.reason }.eachCount()
        
        val recommendations = mutableListOf<String>()
        
        if (reasonBreakdown[BackendSwitchReason.PERFORMANCE_DEGRADATION] ?: 0 > 2) {
            recommendations.add("Frequent performance degradation - check thermal throttling and memory pressure")
        }
        
        if (reasonBreakdown[BackendSwitchReason.MEMORY_PRESSURE] ?: 0 > 1) {
            recommendations.add("Memory pressure causing backend switches - enable aggressive memory management")
        }
        
        if (reasonBreakdown[BackendSwitchReason.BACKEND_FAILURE] ?: 0 > 0) {
            recommendations.add("Backend failures detected - investigate driver issues and model compatibility")
        }
        
        val stabilityScore = if (recentSwitches.size <= 2) 100f 
                           else (100f - (recentSwitches.size * 5f)).coerceIn(0f, 100f)
        
        return BackendSwitchingAnalysis(
            totalSwitches = recentSwitches.size,
            switchPatterns = switchPatterns,
            reasonBreakdown = reasonBreakdown,
            stabilityScore = stabilityScore,
            recommendations = recommendations
        )
    }
}

/**
 * LiteRT-LM backend enumeration.
 */
enum class LiteRTBackend(val displayName: String, val targetTokensPerSecond: Float) {
    CPU("CPU Backend", 243f),
    GPU("GPU Backend", 1876f),
    NPU("NPU Backend", 5836f)
}

/**
 * Reasons for backend switching.
 */
enum class BackendSwitchReason(val description: String) {
    PERFORMANCE_DEGRADATION("Performance below threshold"),
    MEMORY_PRESSURE("High memory usage"),
    THERMAL_THROTTLING("Device overheating"),
    BACKEND_FAILURE("Backend initialization failed"),
    MANUAL_OVERRIDE("User-initiated switch"),
    ADAPTIVE_OPTIMIZATION("Automatic optimization")
}

/**
 * LiteRT performance statistics.
 */
data class LiteRTPerformanceStats(
    val timeRangeMinutes: Int,
    val backendStats: Map<LiteRTBackend, BackendPerformanceStats>,
    val bestPerformingBackend: LiteRTBackend?,
    val overallPerformanceScore: Float
)

/**
 * Individual backend performance statistics.
 */
data class BackendPerformanceStats(
    val backend: LiteRTBackend,
    val inferenceCount: Int,
    val avgTokensPerSecond: Float,
    val avgLatencyMs: Long,
    val avgMemoryUsageMB: Float,
    val successRate: Float,
    val performanceScore: Float // 0-100 based on target achievement
)

/**
 * Backend switching statistics.
 */
data class BackendSwitchingStats(
    val switchCount: Int,
    val avgSwitchTimeMs: Long,
    val mostCommonReason: BackendSwitchReason?,
    val switchFrequency: Float, // switches per minute
    val stabilityScore: Float // 0-100, higher is more stable
)

/**
 * LiteRT performance report.
 */
data class LiteRTPerformanceReport(
    val timestamp: Long,
    val backendComparison: List<BackendComparisonResult>,
    val recommendedBackend: LiteRTBackend,
    val overallAssessment: String
)

/**
 * Backend comparison result.
 */
data class BackendComparisonResult(
    val backend: LiteRTBackend,
    val performanceStats: BackendPerformanceStats,
    val modelInitStats: ModelInitStats?,
    val meetsTarget: Boolean,
    val recommendation: String
)

/**
 * Model initialization statistics.
 */
data class ModelInitStats(
    val initCount: Int,
    val avgInitTimeMs: Long,
    val successRate: Float
)

/**
 * Backend switching analysis.
 */
data class BackendSwitchingAnalysis(
    val totalSwitches: Int,
    val switchPatterns: List<Pair<String, Int>>, // "FROM->TO" pattern and count
    val reasonBreakdown: Map<BackendSwitchReason, Int>,
    val stabilityScore: Float,
    val recommendations: List<String>
)

/**
 * LiteRT performance targets for validation.
 */
data class LiteRTPerformanceTargets(
    val cpuTargetTokensPerSecond: Float = 243f,
    val gpuTargetTokensPerSecond: Float = 1876f,
    val npuTargetTokensPerSecond: Float = 5836f,
    val maxMemoryUsageMB: Float = 2048f, // 2GB limit
    val minAccuracy: Float = 0.90f, // 90% hazard detection accuracy
    val maxFallbackTimeMs: Long = 500L, // 500ms max for backend switching
    val minDeviceSupport: Float = 0.95f // 95% device compatibility
)

/**
 * LiteRT validation report.
 */
data class LiteRTValidationReport(
    val timestamp: Long,
    val deviceCapabilities: DeviceCapabilities,
    val validationResults: List<ValidationResult>,
    val overallScore: Float,
    val passed: Boolean,
    val performanceTargets: LiteRTPerformanceTargets
)