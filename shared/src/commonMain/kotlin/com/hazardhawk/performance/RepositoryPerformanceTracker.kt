package com.hazardhawk.performance

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Repository performance tracking system for database and query optimization.
 * Monitors query response times, cache hit rates, and database operation performance.
 */
class RepositoryPerformanceTracker {
    
    private val mutex = Mutex()
    private val queryHistory = mutableListOf<QueryRecord>()
    private val cacheMetrics = mutableMapOf<String, CacheMetrics>()
    
    private val _performanceAlertsFlow = MutableSharedFlow<RepositoryPerformanceAlert>()
    val performanceAlertsFlow: SharedFlow<RepositoryPerformanceAlert> = _performanceAlertsFlow.asSharedFlow()
    
    data class QueryRecord(
        val queryType: String,
        val tableName: String?,
        val durationMs: Long,
        val success: Boolean,
        val resultCount: Int,
        val cacheHit: Boolean,
        val timestamp: Long,
        val errorMessage: String?
    )
    
    data class CacheMetrics(
        val cacheKey: String,
        var hitCount: Long = 0,
        var missCount: Long = 0,
        var lastHitTime: Long = 0,
        var avgResponseTimeMs: Long = 0
    ) {
        val hitRate: Float get() = if (hitCount + missCount > 0) hitCount.toFloat() / (hitCount + missCount) else 0f
        val totalRequests: Long get() = hitCount + missCount
    }
    
    data class RepositoryPerformanceMetrics(
        val timestamp: Long,
        val avgQueryTimeMs: Long,
        val slowQueryCount: Int,
        val querySuccessRate: Float,
        val overallCacheHitRate: Float,
        val topSlowQueries: List<QueryRecord>,
        val cachePerformance: Map<String, CacheMetrics>,
        val recommendedOptimizations: List<String>
    )
    
    sealed class RepositoryPerformanceAlert {
        data class SlowQuery(val queryType: String, val durationMs: Long, val threshold: Long) : RepositoryPerformanceAlert()
        data class HighFailureRate(val queryType: String, val failureRate: Float) : RepositoryPerformanceAlert()
        data class LowCacheHitRate(val cacheKey: String, val hitRate: Float) : RepositoryPerformanceAlert()
        data class DatabaseConnectionIssue(val message: String) : RepositoryPerformanceAlert()
    }
    
    /**
     * Record a database query execution.
     */
    suspend fun recordQuery(
        queryType: String,
        tableName: String? = null,
        durationMs: Long,
        success: Boolean,
        resultCount: Int = 0,
        cacheHit: Boolean = false,
        errorMessage: String? = null
    ) {
        mutex.withLock {
            val record = QueryRecord(
                queryType = queryType,
                tableName = tableName,
                durationMs = durationMs,
                success = success,
                resultCount = resultCount,
                cacheHit = cacheHit,
                timestamp = System.currentTimeMillis(),
                errorMessage = errorMessage
            )
            
            queryHistory.add(record)
            
            // Keep only last 1000 queries
            if (queryHistory.size > 1000) {
                queryHistory.removeAt(0)
            }
            
            // Update cache metrics
            if (cacheHit || queryType.contains("cache", ignoreCase = true)) {
                updateCacheMetrics(queryType, cacheHit, durationMs)
            }
            
            // Check for performance alerts
            checkPerformanceAlerts(record)
        }
    }
    
    /**
     * Get current repository performance metrics.
     */
    suspend fun getCurrentMetrics(): RepositoryPerformanceMetrics {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            val recentQueries = queryHistory.filter { now - it.timestamp <= 600_000 } // Last 10 minutes
            
            if (recentQueries.isEmpty()) {
                return@withLock RepositoryPerformanceMetrics(
                    timestamp = now,
                    avgQueryTimeMs = 0,
                    slowQueryCount = 0,
                    querySuccessRate = 1.0f,
                    overallCacheHitRate = 0f,
                    topSlowQueries = emptyList(),
                    cachePerformance = emptyMap(),
                    recommendedOptimizations = emptyList()
                )
            }
            
            val avgQueryTime = recentQueries.map { it.durationMs }.average().toLong()
            val slowQueryThreshold = 100L // 100ms threshold
            val slowQueries = recentQueries.filter { it.durationMs > slowQueryThreshold }
            val successRate = recentQueries.count { it.success }.toFloat() / recentQueries.size
            val cacheHitRate = if (recentQueries.any { it.cacheHit }) {
                recentQueries.count { it.cacheHit }.toFloat() / recentQueries.size
            } else 0f
            
            val topSlowQueries = recentQueries
                .sortedByDescending { it.durationMs }
                .take(5)
            
            val optimizations = generateOptimizationRecommendations(recentQueries, slowQueries)
            
            RepositoryPerformanceMetrics(
                timestamp = now,
                avgQueryTimeMs = avgQueryTime,
                slowQueryCount = slowQueries.size,
                querySuccessRate = successRate,
                overallCacheHitRate = cacheHitRate,
                topSlowQueries = topSlowQueries,
                cachePerformance = cacheMetrics.toMap(),
                recommendedOptimizations = optimizations
            )
        }
    }
    
    /**
     * Get detailed query performance by type.
     */
    suspend fun getQueryPerformanceByType(): Map<String, QueryTypeMetrics> {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            val recentQueries = queryHistory.filter { now - it.timestamp <= 3600_000 } // Last hour
            
            recentQueries.groupBy { it.queryType }.mapValues { (_, queries) ->
                QueryTypeMetrics(
                    queryType = queries.first().queryType,
                    totalQueries = queries.size,
                    avgDurationMs = queries.map { it.durationMs }.average().toLong(),
                    maxDurationMs = queries.maxOfOrNull { it.durationMs } ?: 0L,
                    minDurationMs = queries.minOfOrNull { it.durationMs } ?: 0L,
                    successRate = queries.count { it.success }.toFloat() / queries.size,
                    cacheHitRate = if (queries.any { it.cacheHit }) {
                        queries.count { it.cacheHit }.toFloat() / queries.size
                    } else 0f,
                    avgResultCount = queries.map { it.resultCount }.average().toInt()
                )
            }
        }
    }
    
    private fun updateCacheMetrics(queryType: String, cacheHit: Boolean, durationMs: Long) {
        val metrics = cacheMetrics.getOrPut(queryType) { CacheMetrics(queryType) }
        
        if (cacheHit) {
            metrics.hitCount++
            metrics.lastHitTime = System.currentTimeMillis()
            // Update rolling average response time
            metrics.avgResponseTimeMs = ((metrics.avgResponseTimeMs * (metrics.hitCount - 1)) + durationMs) / metrics.hitCount
        } else {
            metrics.missCount++
        }
    }
    
    private suspend fun checkPerformanceAlerts(record: QueryRecord) {
        // Slow query alert
        val slowQueryThreshold = 100L
        if (record.durationMs > slowQueryThreshold) {
            _performanceAlertsFlow.emit(
                RepositoryPerformanceAlert.SlowQuery(record.queryType, record.durationMs, slowQueryThreshold)
            )
        }
        
        // High failure rate alert
        if (!record.success) {
            val recentSameTypeQueries = queryHistory.filter { 
                it.queryType == record.queryType && 
                System.currentTimeMillis() - it.timestamp <= 300_000 // Last 5 minutes
            }
            
            if (recentSameTypeQueries.size >= 5) {
                val failureRate = recentSameTypeQueries.count { !it.success }.toFloat() / recentSameTypeQueries.size
                if (failureRate > 0.3f) { // More than 30% failure rate
                    _performanceAlertsFlow.emit(
                        RepositoryPerformanceAlert.HighFailureRate(record.queryType, failureRate)
                    )
                }
            }
        }
        
        // Low cache hit rate alert
        val cacheMetrics = cacheMetrics[record.queryType]
        if (cacheMetrics != null && cacheMetrics.totalRequests >= 10) {
            if (cacheMetrics.hitRate < 0.3f) { // Less than 30% hit rate
                _performanceAlertsFlow.emit(
                    RepositoryPerformanceAlert.LowCacheHitRate(record.queryType, cacheMetrics.hitRate)
                )
            }
        }
    }
    
    private fun generateOptimizationRecommendations(
        allQueries: List<QueryRecord>,
        slowQueries: List<QueryRecord>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Analyze slow queries
        if (slowQueries.isNotEmpty()) {
            val slowQueryTypes = slowQueries.groupBy { it.queryType }
            slowQueryTypes.forEach { (queryType, queries) ->
                val avgTime = queries.map { it.durationMs }.average()
                recommendations.add("Optimize $queryType queries (avg: ${avgTime.toLong()}ms)")
                
                // Specific recommendations based on query type
                when {
                    queryType.contains("SELECT", ignoreCase = true) -> {
                        recommendations.add("Consider adding indexes for $queryType operations")
                    }
                    queryType.contains("JOIN", ignoreCase = true) -> {
                        recommendations.add("Review JOIN conditions and consider denormalization for $queryType")
                    }
                    queryType.contains("COUNT", ignoreCase = true) -> {
                        recommendations.add("Consider caching COUNT results for $queryType")
                    }
                }
            }
        }
        
        // Cache optimization recommendations
        val queriesWithLowCacheHitRate = cacheMetrics.filter { it.value.hitRate < 0.5f && it.value.totalRequests > 5 }
        queriesWithLowCacheHitRate.forEach { (queryType, metrics) ->
            recommendations.add("Improve caching strategy for $queryType (hit rate: ${(metrics.hitRate * 100).toInt()}%)")
        }
        
        // Result set size recommendations
        val largeResultQueries = allQueries.filter { it.resultCount > 1000 }
        if (largeResultQueries.isNotEmpty()) {
            recommendations.add("Consider pagination for queries returning large result sets (${largeResultQueries.size} queries)")
        }
        
        return recommendations
    }
    
    /**
     * Generate comprehensive performance report.
     */
    suspend fun generatePerformanceReport(): RepositoryPerformanceReport {
        val currentMetrics = getCurrentMetrics()
        val queryTypeMetrics = getQueryPerformanceByType()
        
        return RepositoryPerformanceReport(
            timestamp = System.currentTimeMillis(),
            overallMetrics = currentMetrics,
            queryTypeBreakdown = queryTypeMetrics,
            performanceGrade = calculatePerformanceGrade(currentMetrics),
            criticalIssues = identifyCriticalIssues(currentMetrics, queryTypeMetrics),
            optimizationPriority = prioritizeOptimizations(currentMetrics, queryTypeMetrics)
        )
    }
    
    private fun calculatePerformanceGrade(metrics: RepositoryPerformanceMetrics): String {
        var score = 100
        
        // Deduct points for slow average query time
        when {
            metrics.avgQueryTimeMs > 200 -> score -= 30
            metrics.avgQueryTimeMs > 100 -> score -= 15
            metrics.avgQueryTimeMs > 50 -> score -= 5
        }
        
        // Deduct points for slow queries
        score -= (metrics.slowQueryCount * 2).coerceAtMost(20)
        
        // Deduct points for low success rate
        if (metrics.querySuccessRate < 0.95f) {
            score -= ((1 - metrics.querySuccessRate) * 100).toInt()
        }
        
        // Deduct points for low cache hit rate
        if (metrics.overallCacheHitRate < 0.5f) {
            score -= 10
        }
        
        return when {
            score >= 90 -> "A (Excellent)"
            score >= 80 -> "B (Good)"
            score >= 70 -> "C (Fair)"
            score >= 60 -> "D (Poor)"
            else -> "F (Critical)"
        }
    }
    
    private fun identifyCriticalIssues(
        metrics: RepositoryPerformanceMetrics,
        queryTypeMetrics: Map<String, QueryTypeMetrics>
    ): List<String> {
        val issues = mutableListOf<String>()
        
        if (metrics.avgQueryTimeMs > 200) {
            issues.add("CRITICAL: Average query time exceeds 200ms")
        }
        
        if (metrics.querySuccessRate < 0.9f) {
            issues.add("CRITICAL: Query success rate below 90%")
        }
        
        queryTypeMetrics.forEach { (queryType, typeMetrics) ->
            if (typeMetrics.avgDurationMs > 500) {
                issues.add("CRITICAL: $queryType queries averaging ${typeMetrics.avgDurationMs}ms")
            }
            if (typeMetrics.successRate < 0.8f) {
                issues.add("CRITICAL: $queryType has ${(typeMetrics.successRate * 100).toInt()}% success rate")
            }
        }
        
        return issues
    }
    
    private fun prioritizeOptimizations(
        metrics: RepositoryPerformanceMetrics,
        queryTypeMetrics: Map<String, QueryTypeMetrics>
    ): List<OptimizationPriority> {
        val priorities = mutableListOf<OptimizationPriority>()
        
        // Priority 1: Fix critical query performance issues
        queryTypeMetrics.forEach { (queryType, typeMetrics) ->
            if (typeMetrics.avgDurationMs > 200) {
                priorities.add(
                    OptimizationPriority(
                        priority = 1,
                        area = "Query Performance",
                        description = "Optimize $queryType (${typeMetrics.avgDurationMs}ms avg)",
                        estimatedImpact = "High",
                        effort = "Medium"
                    )
                )
            }
        }
        
        // Priority 2: Improve cache hit rates
        val lowCacheHitQueries = queryTypeMetrics.filter { it.value.cacheHitRate < 0.3f }
        if (lowCacheHitQueries.isNotEmpty()) {
            priorities.add(
                OptimizationPriority(
                    priority = 2,
                    area = "Caching",
                    description = "Improve caching for ${lowCacheHitQueries.keys.joinToString(", ")}",
                    estimatedImpact = "Medium",
                    effort = "Low"
                )
            )
        }
        
        // Priority 3: General performance improvements
        if (metrics.slowQueryCount > 5) {
            priorities.add(
                OptimizationPriority(
                    priority = 3,
                    area = "General Performance", 
                    description = "Address ${metrics.slowQueryCount} slow queries",
                    estimatedImpact = "Medium",
                    effort = "High"
                )
            )
        }
        
        return priorities.sortedBy { it.priority }
    }
}

data class QueryTypeMetrics(
    val queryType: String,
    val totalQueries: Int,
    val avgDurationMs: Long,
    val maxDurationMs: Long,
    val minDurationMs: Long,
    val successRate: Float,
    val cacheHitRate: Float,
    val avgResultCount: Int
)

data class RepositoryPerformanceReport(
    val timestamp: Long,
    val overallMetrics: RepositoryPerformanceMetrics,
    val queryTypeBreakdown: Map<String, QueryTypeMetrics>,
    val performanceGrade: String,
    val criticalIssues: List<String>,
    val optimizationPriority: List<OptimizationPriority>
)

data class OptimizationPriority(
    val priority: Int,
    val area: String,
    val description: String,
    val estimatedImpact: String,
    val effort: String
)

/**
 * Repository performance benchmarking utilities.
 */
class RepositoryPerformanceBenchmark(
    private val tracker: RepositoryPerformanceTracker
) {
    
    /**
     * Run comprehensive repository performance benchmark.
     */
    suspend fun runBenchmark(): RepositoryBenchmarkResults {
        val startTime = System.currentTimeMillis()
        val results = mutableMapOf<String, BenchmarkResult>()
        
        // Benchmark different query types
        val queryTypes = listOf(
            "SELECT_SIMPLE",
            "SELECT_WITH_JOIN",
            "SELECT_WITH_AGGREGATION",
            "INSERT_BATCH",
            "UPDATE_SINGLE",
            "DELETE_CONDITIONAL",
            "COUNT_QUERY",
            "FULL_TEXT_SEARCH"
        )
        
        queryTypes.forEach { queryType ->
            results[queryType] = benchmarkQueryType(queryType)
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        
        return RepositoryBenchmarkResults(
            timestamp = System.currentTimeMillis(),
            totalBenchmarkTimeMs = totalTime,
            queryBenchmarks = results,
            overallScore = results.values.map { it.score }.average().toFloat(),
            recommendations = generateBenchmarkRecommendations(results)
        )
    }
    
    private suspend fun benchmarkQueryType(queryType: String): BenchmarkResult {
        val iterations = 10
        val durations = mutableListOf<Long>()
        var successCount = 0
        
        repeat(iterations) {
            val startTime = System.currentTimeMillis()
            
            // Simulate query execution based on type
            val (duration, success) = simulateQuery(queryType)
            durations.add(duration)
            
            if (success) successCount++
            
            // Record for tracking
            tracker.recordQuery(
                queryType = queryType,
                tableName = "benchmark_table",
                durationMs = duration,
                success = success,
                resultCount = when (queryType) {
                    "SELECT_SIMPLE" -> 1
                    "SELECT_WITH_JOIN" -> 5
                    "SELECT_WITH_AGGREGATION" -> 1
                    "COUNT_QUERY" -> 1
                    "FULL_TEXT_SEARCH" -> 15
                    else -> 0
                },
                cacheHit = false
            )
        }
        
        val avgDuration = durations.average().toLong()
        val maxDuration = durations.maxOrNull() ?: 0L
        val minDuration = durations.minOrNull() ?: 0L
        val successRate = successCount.toFloat() / iterations
        
        // Calculate score (lower duration = higher score)
        val score = when {
            avgDuration < 10 -> 100f
            avgDuration < 25 -> 90f
            avgDuration < 50 -> 80f
            avgDuration < 100 -> 70f
            avgDuration < 200 -> 50f
            else -> 25f
        } * successRate // Multiply by success rate
        
        return BenchmarkResult(
            queryType = queryType,
            avgDurationMs = avgDuration,
            maxDurationMs = maxDuration,
            minDurationMs = minDuration,
            successRate = successRate,
            score = score
        )
    }
    
    private suspend fun simulateQuery(queryType: String): Pair<Long, Boolean> {
        // Simulate different query complexities and success rates
        val duration = when (queryType) {
            "SELECT_SIMPLE" -> (10..30).random().toLong()
            "SELECT_WITH_JOIN" -> (25..75).random().toLong()
            "SELECT_WITH_AGGREGATION" -> (40..120).random().toLong()
            "INSERT_BATCH" -> (30..90).random().toLong()
            "UPDATE_SINGLE" -> (15..45).random().toLong()
            "DELETE_CONDITIONAL" -> (20..60).random().toLong()
            "COUNT_QUERY" -> (35..100).random().toLong()
            "FULL_TEXT_SEARCH" -> (50..200).random().toLong()
            else -> (25..75).random().toLong()
        }
        
        // Simulate occasional failures
        val success = (1..100).random() > 2 // 98% success rate
        
        return duration to success
    }
    
    private fun generateBenchmarkRecommendations(results: Map<String, BenchmarkResult>): List<String> {
        val recommendations = mutableListOf<String>()
        
        results.forEach { (queryType, result) ->
            when {
                result.score < 50f -> {
                    recommendations.add("URGENT: Optimize $queryType (score: ${result.score.toInt()})")
                }
                result.score < 70f -> {
                    recommendations.add("Consider optimizing $queryType (score: ${result.score.toInt()})")
                }
                result.avgDurationMs > 100 -> {
                    recommendations.add("$queryType averaging ${result.avgDurationMs}ms - consider indexing")
                }
            }
        }
        
        val avgScore = results.values.map { it.score }.average()
        when {
            avgScore < 60 -> recommendations.add("Overall repository performance needs significant improvement")
            avgScore < 80 -> recommendations.add("Repository performance is acceptable but could be optimized")
            else -> recommendations.add("Repository performance is good")
        }
        
        return recommendations
    }
}

data class BenchmarkResult(
    val queryType: String,
    val avgDurationMs: Long,
    val maxDurationMs: Long,
    val minDurationMs: Long,
    val successRate: Float,
    val score: Float
)

data class RepositoryBenchmarkResults(
    val timestamp: Long,
    val totalBenchmarkTimeMs: Long,
    val queryBenchmarks: Map<String, BenchmarkResult>,
    val overallScore: Float,
    val recommendations: List<String>
)