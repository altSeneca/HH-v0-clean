package com.hazardhawk.performance
import kotlinx.datetime.Clock

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Memory regression detection system for comparing before/after refactoring performance.
 * Tracks memory usage patterns, leak detection, and performance regression analysis.
 */
class MemoryRegressionDetector(
    private val deviceDetector: DeviceTierDetector,
    private val memoryManager: MemoryManager
) {
    
    private val mutex = Mutex()
    private val memorySnapshots = mutableListOf<MemorySnapshot>()
    private val baselineSnapshots = mutableListOf<MemorySnapshot>()
    private var baselineEstablished = false
    
    private val _regressionAlertsFlow = MutableSharedFlow<MemoryRegressionAlert>()
    val regressionAlertsFlow: SharedFlow<MemoryRegressionAlert> = _regressionAlertsFlow.asSharedFlow()
    
    data class MemorySnapshot(
        val timestamp: Long,
        val totalMemoryMB: Float,
        val usedMemoryMB: Float,
        val availableMemoryMB: Float,
        val managedMemoryMB: Float,
        val modelMemoryMB: Float,
        val cacheMemoryMB: Float,
        val gcCount: Int,
        val pressureLevel: MemoryPressure,
        val operationContext: String?,
        val buildVersion: String,
        val metadata: Map<String, Any> = emptyMap()
    ) {
        val memoryUsagePercent: Float get() = (usedMemoryMB / totalMemoryMB) * 100f
        val managedMemoryPercent: Float get() = (managedMemoryMB / totalMemoryMB) * 100f
    }
    
    sealed class MemoryRegressionAlert {
        data class MemoryLeak(val component: String, val growthRate: Float, val severity: RegressionSeverity) : MemoryRegressionAlert()
        data class PerformanceRegression(val metric: String, val degradation: Float, val severity: RegressionSeverity) : MemoryRegressionAlert()
        data class BaselineExceeded(val metric: String, val currentValue: Float, val baselineValue: Float) : MemoryRegressionAlert()
        data class UnusualPattern(val description: String, val confidence: Float) : MemoryRegressionAlert()
    }
    
    enum class RegressionSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    data class RegressionAnalysisReport(
        val timestamp: Long,
        val buildVersion: String,
        val baselineComparison: BaselineComparison,
        val memoryLeakAnalysis: MemoryLeakAnalysis,
        val performanceRegression: PerformanceRegressionAnalysis,
        val recommendations: List<String>,
        val overallRegressionScore: Float, // 0-100, lower is worse
        val passesRegressionThreshold: Boolean
    )
    
    data class BaselineComparison(
        val hasBaseline: Boolean,
        val baselineSnapshots: Int,
        val currentSnapshots: Int,
        val memoryUsageChange: Float, // Percentage change
        val managedMemoryChange: Float,
        val gcFrequencyChange: Float,
        val significantChanges: List<String>
    )
    
    data class MemoryLeakAnalysis(
        val potentialLeaks: List<MemoryLeakIndicator>,
        val growthPatterns: List<GrowthPattern>,
        val leakProbability: Float, // 0-1
        val affectedComponents: List<String>
    )
    
    data class MemoryLeakIndicator(
        val component: String,
        val description: String,
        val growthRate: Float, // MB per operation
        val confidence: Float,
        val severity: RegressionSeverity
    )
    
    data class GrowthPattern(
        val pattern: String,
        val description: String,
        val trendStrength: Float,
        val projectedMemoryAt100Ops: Float
    )
    
    data class PerformanceRegressionAnalysis(
        val regressions: List<PerformanceRegression>,
        val improvements: List<PerformanceImprovement>,
        val overallPerformanceChange: Float
    )
    
    data class PerformanceRegression(
        val metric: String,
        val baselineValue: Float,
        val currentValue: Float,
        val degradationPercent: Float,
        val severity: RegressionSeverity
    )
    
    data class PerformanceImprovement(
        val metric: String,
        val baselineValue: Float,
        val currentValue: Float,
        val improvementPercent: Float
    )
    
    /**
     * Establish baseline memory performance metrics.
     */
    suspend fun establishBaseline(buildVersion: String) {
        mutex.withLock {
            baselineSnapshots.clear()
            baselineEstablished = false
            
            // Take multiple baseline snapshots during typical operations
            repeat(10) {
                val snapshot = captureMemorySnapshot("baseline_$it", buildVersion)
                baselineSnapshots.add(snapshot)
                
                // Simulate some typical operations
                kotlinx.coroutines.delay(1000)
            }
            
            baselineEstablished = true
        }
    }
    
    /**
     * Capture a memory snapshot for analysis.
     */
    suspend fun captureSnapshot(
        operationContext: String? = null,
        buildVersion: String,
        metadata: Map<String, Any> = emptyMap()
    ): MemorySnapshot {
        return mutex.withLock {
            val snapshot = captureMemorySnapshot(operationContext, buildVersion, metadata)
            memorySnapshots.add(snapshot)
            
            // Keep only last 200 snapshots
            if (memorySnapshots.size > 200) {
                memorySnapshots.removeAt(0)
            }
            
            // Check for immediate regression alerts
            checkForImmediateRegressions(snapshot)
            
            snapshot
        }
    }
    
    private suspend fun captureMemorySnapshot(
        operationContext: String?,
        buildVersion: String,
        metadata: Map<String, Any> = emptyMap()
    ): MemorySnapshot {
        val deviceCapabilities = deviceDetector.detectCapabilities()
        val memoryStats = memoryManager.getMemoryStats()
        val currentMemoryUsage = deviceDetector.getCurrentMemoryUsage() / (1024 * 1024)
        val availableMemory = deviceDetector.getAvailableMemory() / (1024 * 1024)
        
        return MemorySnapshot(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            totalMemoryMB = deviceCapabilities.totalMemoryMB.toFloat(),
            usedMemoryMB = currentMemoryUsage.toFloat(),
            availableMemoryMB = availableMemory.toFloat(),
            managedMemoryMB = memoryStats.totalManagedMemoryMB.toFloat(),
            modelMemoryMB = memoryStats.totalModelMemoryMB.toFloat(),
            cacheMemoryMB = (memoryStats.totalImageCacheMB + memoryStats.totalAnalysisCacheMB).toFloat(),
            gcCount = 0, // Would be provided by platform-specific implementation
            pressureLevel = deviceCapabilities.memoryPressure,
            operationContext = operationContext,
            buildVersion = buildVersion,
            metadata = metadata
        )
    }
    
    /**
     * Analyze memory regression against baseline.
     */
    suspend fun analyzeRegression(buildVersion: String): RegressionAnalysisReport {
        return mutex.withLock {
            val currentSnapshots = memorySnapshots.filter { it.buildVersion == buildVersion }
            
            val baselineComparison = if (baselineEstablished) {
                analyzeBaselineComparison(currentSnapshots)
            } else {
                BaselineComparison(
                    hasBaseline = false,
                    baselineSnapshots = 0,
                    currentSnapshots = currentSnapshots.size,
                    memoryUsageChange = 0f,
                    managedMemoryChange = 0f,
                    gcFrequencyChange = 0f,
                    significantChanges = listOf("No baseline established")
                )
            }
            
            val memoryLeakAnalysis = analyzeMemoryLeaks(currentSnapshots)
            val performanceRegression = analyzePerformanceRegression(currentSnapshots)
            
            val overallScore = calculateRegressionScore(
                baselineComparison, memoryLeakAnalysis, performanceRegression
            )
            
            val recommendations = generateRegressionRecommendations(
                baselineComparison, memoryLeakAnalysis, performanceRegression
            )
            
            RegressionAnalysisReport(
                timestamp = Clock.System.now().toEpochMilliseconds(),
                buildVersion = buildVersion,
                baselineComparison = baselineComparison,
                memoryLeakAnalysis = memoryLeakAnalysis,
                performanceRegression = performanceRegression,
                recommendations = recommendations,
                overallRegressionScore = overallScore,
                passesRegressionThreshold = overallScore >= 70f // 70% threshold
            )
        }
    }
    
    private fun analyzeBaselineComparison(currentSnapshots: List<MemorySnapshot>): BaselineComparison {
        if (baselineSnapshots.isEmpty() || currentSnapshots.isEmpty()) {
            return BaselineComparison(false, 0, 0, 0f, 0f, 0f, emptyList())
        }
        
        val baselineAvgMemory = baselineSnapshots.map { it.memoryUsagePercent }.average().toFloat()
        val currentAvgMemory = currentSnapshots.map { it.memoryUsagePercent }.average().toFloat()
        val memoryUsageChange = ((currentAvgMemory - baselineAvgMemory) / baselineAvgMemory) * 100f
        
        val baselineAvgManaged = baselineSnapshots.map { it.managedMemoryPercent }.average().toFloat()
        val currentAvgManaged = currentSnapshots.map { it.managedMemoryPercent }.average().toFloat()
        val managedMemoryChange = ((currentAvgManaged - baselineAvgManaged) / baselineAvgManaged) * 100f
        
        val significantChanges = mutableListOf<String>()
        
        if (kotlin.math.abs(memoryUsageChange) > 10f) {
            val direction = if (memoryUsageChange > 0) "increased" else "decreased"
            significantChanges.add("Memory usage $direction by ${kotlin.math.abs(memoryUsageChange).toInt()}%")
        }
        
        if (kotlin.math.abs(managedMemoryChange) > 15f) {
            val direction = if (managedMemoryChange > 0) "increased" else "decreased"
            significantChanges.add("Managed memory $direction by ${kotlin.math.abs(managedMemoryChange).toInt()}%")
        }
        
        return BaselineComparison(
            hasBaseline = true,
            baselineSnapshots = baselineSnapshots.size,
            currentSnapshots = currentSnapshots.size,
            memoryUsageChange = memoryUsageChange,
            managedMemoryChange = managedMemoryChange,
            gcFrequencyChange = 0f, // Would calculate from actual GC data
            significantChanges = significantChanges
        )
    }
    
    private fun analyzeMemoryLeaks(snapshots: List<MemorySnapshot>): MemoryLeakAnalysis {
        if (snapshots.size < 10) {
            return MemoryLeakAnalysis(emptyList(), emptyList(), 0f, emptyList())
        }
        
        val potentialLeaks = mutableListOf<MemoryLeakIndicator>()
        val growthPatterns = mutableListOf<GrowthPattern>()
        
        // Analyze memory growth over time
        val sortedSnapshots = snapshots.sortedBy { it.timestamp }
        
        // Check for consistent memory growth (potential leak)
        val memoryUsageGrowth = analyzeGrowthTrend(sortedSnapshots.map { it.usedMemoryMB })
        if (memoryUsageGrowth.isSignificant) {
            potentialLeaks.add(
                MemoryLeakIndicator(
                    component = "Total Memory",
                    description = "Consistent memory growth detected",
                    growthRate = memoryUsageGrowth.growthRate,
                    confidence = memoryUsageGrowth.confidence,
                    severity = when {
                        memoryUsageGrowth.growthRate > 10f -> RegressionSeverity.CRITICAL
                        memoryUsageGrowth.growthRate > 5f -> RegressionSeverity.HIGH
                        memoryUsageGrowth.growthRate > 2f -> RegressionSeverity.MEDIUM
                        else -> RegressionSeverity.LOW
                    }
                )
            )
        }
        
        // Check managed memory growth
        val managedMemoryGrowth = analyzeGrowthTrend(sortedSnapshots.map { it.managedMemoryMB })
        if (managedMemoryGrowth.isSignificant) {
            potentialLeaks.add(
                MemoryLeakIndicator(
                    component = "Managed Memory",
                    description = "Managed memory growing without cleanup",
                    growthRate = managedMemoryGrowth.growthRate,
                    confidence = managedMemoryGrowth.confidence,
                    severity = when {
                        managedMemoryGrowth.growthRate > 5f -> RegressionSeverity.HIGH
                        managedMemoryGrowth.growthRate > 2f -> RegressionSeverity.MEDIUM
                        else -> RegressionSeverity.LOW
                    }
                )
            )
        }
        
        // Check cache memory growth
        val cacheMemoryGrowth = analyzeGrowthTrend(sortedSnapshots.map { it.cacheMemoryMB })
        if (cacheMemoryGrowth.isSignificant) {
            potentialLeaks.add(
                MemoryLeakIndicator(
                    component = "Cache Memory",
                    description = "Cache not being properly evicted",
                    growthRate = cacheMemoryGrowth.growthRate,
                    confidence = cacheMemoryGrowth.confidence,
                    severity = when {
                        cacheMemoryGrowth.growthRate > 3f -> RegressionSeverity.MEDIUM
                        else -> RegressionSeverity.LOW
                    }
                )
            )
        }
        
        val leakProbability = when {
            potentialLeaks.any { it.severity == RegressionSeverity.CRITICAL } -> 0.9f
            potentialLeaks.any { it.severity == RegressionSeverity.HIGH } -> 0.7f
            potentialLeaks.any { it.severity == RegressionSeverity.MEDIUM } -> 0.5f
            potentialLeaks.any { it.severity == RegressionSeverity.LOW } -> 0.3f
            else -> 0.1f
        }
        
        return MemoryLeakAnalysis(
            potentialLeaks = potentialLeaks,
            growthPatterns = growthPatterns,
            leakProbability = leakProbability,
            affectedComponents = potentialLeaks.map { it.component }
        )
    }
    
    private fun analyzeGrowthTrend(values: List<Float>): GrowthTrendAnalysis {
        if (values.size < 5) return GrowthTrendAnalysis(false, 0f, 0f)
        
        // Simple linear regression to detect growth trend
        val n = values.size
        val x = (0 until n).map { it.toFloat() }
        val y = values
        
        val xMean = x.average().toFloat()
        val yMean = y.average().toFloat()
        
        var numerator = 0f
        var denominator = 0f
        
        for (i in 0 until n) {
            val xDiff = x[i] - xMean
            val yDiff = y[i] - yMean
            numerator += xDiff * yDiff
            denominator += xDiff * xDiff
        }
        
        val slope = if (denominator != 0f) numerator / denominator else 0f
        
        // Calculate correlation coefficient for confidence
        val correlation = calculateCorrelation(x, y)
        
        return GrowthTrendAnalysis(
            isSignificant = slope > 0.1f && correlation > 0.6f,
            growthRate = slope,
            confidence = kotlin.math.abs(correlation)
        )
    }
    
    private fun calculateCorrelation(x: List<Float>, y: List<Float>): Float {
        val n = x.size
        if (n != y.size || n == 0) return 0f
        
        val xMean = x.average().toFloat()
        val yMean = y.average().toFloat()
        
        var numerator = 0f
        var xVariance = 0f
        var yVariance = 0f
        
        for (i in 0 until n) {
            val xDiff = x[i] - xMean
            val yDiff = y[i] - yMean
            numerator += xDiff * yDiff
            xVariance += xDiff * xDiff
            yVariance += yDiff * yDiff
        }
        
        val denominator = kotlin.math.sqrt(xVariance * yVariance)
        return if (denominator != 0f) numerator / denominator else 0f
    }
    
    private fun analyzePerformanceRegression(snapshots: List<MemorySnapshot>): PerformanceRegressionAnalysis {
        if (!baselineEstablished || snapshots.isEmpty()) {
            return PerformanceRegressionAnalysis(emptyList(), emptyList(), 0f)
        }
        
        val regressions = mutableListOf<PerformanceRegression>()
        val improvements = mutableListOf<PerformanceImprovement>()
        
        // Compare key metrics
        val baselineAvgMemory = baselineSnapshots.map { it.usedMemoryMB }.average().toFloat()
        val currentAvgMemory = snapshots.map { it.usedMemoryMB }.average().toFloat()
        val memoryChange = ((currentAvgMemory - baselineAvgMemory) / baselineAvgMemory) * 100f
        
        if (memoryChange > 5f) {
            regressions.add(
                PerformanceRegression(
                    metric = "Average Memory Usage",
                    baselineValue = baselineAvgMemory,
                    currentValue = currentAvgMemory,
                    degradationPercent = memoryChange,
                    severity = when {
                        memoryChange > 25f -> RegressionSeverity.CRITICAL
                        memoryChange > 15f -> RegressionSeverity.HIGH
                        memoryChange > 10f -> RegressionSeverity.MEDIUM
                        else -> RegressionSeverity.LOW
                    }
                )
            )
        } else if (memoryChange < -5f) {
            improvements.add(
                PerformanceImprovement(
                    metric = "Average Memory Usage",
                    baselineValue = baselineAvgMemory,
                    currentValue = currentAvgMemory,
                    improvementPercent = kotlin.math.abs(memoryChange)
                )
            )
        }
        
        val overallChange = if (regressions.isNotEmpty()) {
            regressions.map { it.degradationPercent }.average().toFloat() * -1f
        } else if (improvements.isNotEmpty()) {
            improvements.map { it.improvementPercent }.average().toFloat()
        } else {
            0f
        }
        
        return PerformanceRegressionAnalysis(regressions, improvements, overallChange)
    }
    
    private fun calculateRegressionScore(
        baselineComparison: BaselineComparison,
        memoryLeakAnalysis: MemoryLeakAnalysis,
        performanceRegression: PerformanceRegressionAnalysis
    ): Float {
        var score = 100f
        
        // Deduct for baseline regressions
        if (baselineComparison.hasBaseline) {
            if (kotlin.math.abs(baselineComparison.memoryUsageChange) > 20f) {
                score -= 30f
            } else if (kotlin.math.abs(baselineComparison.memoryUsageChange) > 10f) {
                score -= 15f
            }
        }
        
        // Deduct for memory leaks
        score -= memoryLeakAnalysis.leakProbability * 40f // Up to 40 points
        
        // Deduct for performance regressions
        performanceRegression.regressions.forEach { regression ->
            val deduction = when (regression.severity) {
                RegressionSeverity.CRITICAL -> 25f
                RegressionSeverity.HIGH -> 15f
                RegressionSeverity.MEDIUM -> 10f
                RegressionSeverity.LOW -> 5f
            }
            score -= deduction
        }
        
        // Add back for improvements
        performanceRegression.improvements.forEach { improvement ->
            score += (improvement.improvementPercent * 0.5f).coerceAtMost(10f)
        }
        
        return score.coerceIn(0f, 100f)
    }
    
    private fun generateRegressionRecommendations(
        baselineComparison: BaselineComparison,
        memoryLeakAnalysis: MemoryLeakAnalysis,
        performanceRegression: PerformanceRegressionAnalysis
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Baseline-based recommendations
        if (baselineComparison.hasBaseline) {
            baselineComparison.significantChanges.forEach { change ->
                recommendations.add("Address baseline change: $change")
            }
        } else {
            recommendations.add("Establish performance baseline for future regression detection")
        }
        
        // Memory leak recommendations
        memoryLeakAnalysis.potentialLeaks.forEach { leak ->
            when (leak.severity) {
                RegressionSeverity.CRITICAL -> {
                    recommendations.add("URGENT: Fix critical memory leak in ${leak.component}")
                }
                RegressionSeverity.HIGH -> {
                    recommendations.add("HIGH PRIORITY: Address memory leak in ${leak.component}")
                }
                RegressionSeverity.MEDIUM -> {
                    recommendations.add("Investigate potential memory leak in ${leak.component}")
                }
                RegressionSeverity.LOW -> {
                    recommendations.add("Monitor memory usage in ${leak.component}")
                }
            }
        }
        
        // Performance regression recommendations
        performanceRegression.regressions.forEach { regression ->
            recommendations.add("Optimize ${regression.metric} - degraded by ${regression.degradationPercent.toInt()}%")
        }
        
        if (memoryLeakAnalysis.leakProbability > 0.6f) {
            recommendations.add("Enable aggressive memory monitoring and profiling")
            recommendations.add("Consider implementing automatic memory pressure handling")
        }
        
        return recommendations
    }
    
    private suspend fun checkForImmediateRegressions(snapshot: MemorySnapshot) {
        // Check for immediate memory pressure increases
        if (snapshot.pressureLevel == MemoryPressure.CRITICAL) {
            _regressionAlertsFlow.emit(
                MemoryRegressionAlert.UnusualPattern(
                    "Critical memory pressure detected",
                    0.9f
                )
            )
        }
        
        // Check for excessive memory usage
        if (snapshot.memoryUsagePercent > 90f) {
            _regressionAlertsFlow.emit(
                MemoryRegressionAlert.BaselineExceeded(
                    "Memory Usage",
                    snapshot.memoryUsagePercent,
                    85f // Assumed baseline threshold
                )
            )
        }
    }
    
    /**
     * Compare memory performance between two builds.
     */
    suspend fun compareBuildPerformance(oldBuild: String, newBuild: String): BuildComparisonReport {
        return mutex.withLock {
            val oldSnapshots = memorySnapshots.filter { it.buildVersion == oldBuild }
            val newSnapshots = memorySnapshots.filter { it.buildVersion == newBuild }
            
            if (oldSnapshots.isEmpty() || newSnapshots.isEmpty()) {
                return@withLock BuildComparisonReport(
                    oldBuild, newBuild, false, emptyList(), emptyList(), 0f
                )
            }
            
            val comparisons = mutableListOf<MetricComparison>()
            val recommendations = mutableListOf<String>()
            
            // Compare average memory usage
            val oldAvgMemory = oldSnapshots.map { it.usedMemoryMB }.average().toFloat()
            val newAvgMemory = newSnapshots.map { it.usedMemoryMB }.average().toFloat()
            val memoryChange = ((newAvgMemory - oldAvgMemory) / oldAvgMemory) * 100f
            
            comparisons.add(
                MetricComparison("Average Memory Usage", oldAvgMemory, newAvgMemory, memoryChange)
            )
            
            if (kotlin.math.abs(memoryChange) > 10f) {
                val direction = if (memoryChange > 0) "increased" else "decreased"
                recommendations.add("Memory usage $direction by ${kotlin.math.abs(memoryChange).toInt()}%")
            }
            
            val overallChange = comparisons.map { kotlin.math.abs(it.changePercent) }.average().toFloat()
            
            BuildComparisonReport(
                oldBuild = oldBuild,
                newBuild = newBuild,
                hasData = true,
                comparisons = comparisons,
                recommendations = recommendations,
                overallChangePercent = overallChange
            )
        }
    }
}

data class GrowthTrendAnalysis(
    val isSignificant: Boolean,
    val growthRate: Float,
    val confidence: Float
)

data class BuildComparisonReport(
    val oldBuild: String,
    val newBuild: String,
    val hasData: Boolean,
    val comparisons: List<MetricComparison>,
    val recommendations: List<String>,
    val overallChangePercent: Float
)

data class MetricComparison(
    val metricName: String,
    val oldValue: Float,
    val newValue: Float,
    val changePercent: Float
)