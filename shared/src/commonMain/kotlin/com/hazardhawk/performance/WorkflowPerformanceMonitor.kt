package com.hazardhawk.performance

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.uuid.uuid4

/**
 * End-to-End workflow performance monitoring for HazardHawk safety inspection workflows.
 * Tracks complete user journeys from photo capture to report generation.
 */
class WorkflowPerformanceMonitor {
    
    private val mutex = Mutex()
    private val activeWorkflows = mutableMapOf<String, WorkflowExecution>()
    private val completedWorkflows = mutableListOf<WorkflowExecution>()
    
    private val _workflowAlertsFlow = MutableSharedFlow<WorkflowPerformanceAlert>()
    val workflowAlertsFlow: SharedFlow<WorkflowPerformanceAlert> = _workflowAlertsFlow.asSharedFlow()
    
    data class WorkflowExecution(
        val workflowId: String,
        val workflowType: WorkflowType,
        val startTime: Long,
        var endTime: Long? = null,
        val steps: MutableList<WorkflowStep> = mutableListOf(),
        val metadata: MutableMap<String, Any> = mutableMapOf(),
        var success: Boolean = false,
        var errorMessage: String? = null
    ) {
        val durationMs: Long get() = (endTime ?: System.currentTimeMillis()) - startTime
        val isComplete: Boolean get() = endTime != null
        val stepCount: Int get() = steps.size
        val successfulSteps: Int get() = steps.count { it.success }
        val failedSteps: Int get() = steps.count { !it.success }
    }
    
    data class WorkflowStep(
        val stepName: String,
        val stepType: StepType,
        val startTime: Long,
        var endTime: Long? = null,
        var success: Boolean = false,
        var errorMessage: String? = null,
        val metadata: MutableMap<String, Any> = mutableMapOf()
    ) {
        val durationMs: Long get() = (endTime ?: System.currentTimeMillis()) - startTime
    }
    
    enum class WorkflowType {
        PHOTO_CAPTURE_ANALYSIS,
        SAFETY_INSPECTION,
        INCIDENT_REPORTING,
        PTP_GENERATION,
        TOOLBOX_TALK_CREATION,
        BATCH_ANALYSIS,
        REPORT_GENERATION
    }
    
    enum class StepType {
        PHOTO_CAPTURE,
        IMAGE_PREPROCESSING,
        AI_ANALYSIS,
        RESULTS_PROCESSING,
        CACHE_OPERATIONS,
        DATABASE_OPERATIONS,
        REPORT_GENERATION,
        PDF_CREATION,
        FILE_OPERATIONS,
        UI_RENDERING,
        NETWORK_OPERATIONS
    }
    
    sealed class WorkflowPerformanceAlert {
        data class SlowWorkflow(val workflowId: String, val workflowType: WorkflowType, val durationMs: Long) : WorkflowPerformanceAlert()
        data class StepFailure(val workflowId: String, val stepName: String, val errorMessage: String) : WorkflowPerformanceAlert()
        data class HighFailureRate(val workflowType: WorkflowType, val failureRate: Float) : WorkflowPerformanceAlert()
        data class PerformanceDegradation(val workflowType: WorkflowType, val degradation: Float) : WorkflowPerformanceAlert()
    }
    
    /**
     * Start monitoring a new workflow.
     */
    suspend fun startWorkflow(workflowType: WorkflowType, metadata: Map<String, Any> = emptyMap()): String {
        return mutex.withLock {
            val workflowId = uuid4().toString()
            val workflow = WorkflowExecution(
                workflowId = workflowId,
                workflowType = workflowType,
                startTime = System.currentTimeMillis(),
                metadata = metadata.toMutableMap()
            )
            
            activeWorkflows[workflowId] = workflow
            workflowId
        }
    }
    
    /**
     * Start a workflow step.
     */
    suspend fun startWorkflowStep(
        workflowId: String,
        stepName: String,
        stepType: StepType,
        metadata: Map<String, Any> = emptyMap()
    ): String {
        mutex.withLock {
            val workflow = activeWorkflows[workflowId]
            if (workflow != null) {
                val step = WorkflowStep(
                    stepName = stepName,
                    stepType = stepType,
                    startTime = System.currentTimeMillis(),
                    metadata = metadata.toMutableMap()
                )
                workflow.steps.add(step)
            }
        }
        return "${workflowId}_${stepName}"
    }
    
    /**
     * Complete a workflow step.
     */
    suspend fun completeWorkflowStep(
        workflowId: String,
        stepName: String,
        success: Boolean,
        errorMessage: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        mutex.withLock {
            val workflow = activeWorkflows[workflowId]
            val step = workflow?.steps?.findLast { it.stepName == stepName && it.endTime == null }
            
            if (step != null) {
                step.endTime = System.currentTimeMillis()
                step.success = success
                step.errorMessage = errorMessage
                step.metadata.putAll(metadata)
                
                // Check for step performance alerts
                checkStepPerformanceAlerts(workflowId, step)
            }
        }
    }
    
    /**
     * Complete an entire workflow.
     */
    suspend fun completeWorkflow(
        workflowId: String,
        success: Boolean,
        errorMessage: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        mutex.withLock {
            val workflow = activeWorkflows.remove(workflowId)
            if (workflow != null) {
                workflow.endTime = System.currentTimeMillis()
                workflow.success = success
                workflow.errorMessage = errorMessage
                workflow.metadata.putAll(metadata)
                
                completedWorkflows.add(workflow)
                
                // Keep only last 500 completed workflows
                if (completedWorkflows.size > 500) {
                    completedWorkflows.removeAt(0)
                }
                
                // Check for workflow performance alerts
                checkWorkflowPerformanceAlerts(workflow)
            }
        }
    }
    
    /**
     * Get current workflow performance metrics.
     */
    suspend fun getCurrentMetrics(): WorkflowPerformanceMetrics {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            val recentWorkflows = completedWorkflows.filter { now - it.startTime <= 3600_000 } // Last hour
            
            if (recentWorkflows.isEmpty()) {
                return@withLock WorkflowPerformanceMetrics(
                    timestamp = now,
                    activeWorkflows = activeWorkflows.size,
                    completedWorkflows = 0,
                    avgWorkflowDurationMs = 0L,
                    workflowSuccessRate = 1.0f,
                    stepPerformance = emptyMap(),
                    workflowTypePerformance = emptyMap(),
                    bottlenecks = emptyList(),
                    recommendations = emptyList()
                )
            }
            
            val avgDuration = recentWorkflows.map { it.durationMs }.average().toLong()
            val successRate = recentWorkflows.count { it.success }.toFloat() / recentWorkflows.size
            
            val stepPerformance = analyzeStepPerformance(recentWorkflows)
            val workflowTypePerformance = analyzeWorkflowTypePerformance(recentWorkflows)
            val bottlenecks = identifyBottlenecks(recentWorkflows)
            val recommendations = generateWorkflowRecommendations(recentWorkflows, bottlenecks)
            
            WorkflowPerformanceMetrics(
                timestamp = now,
                activeWorkflows = activeWorkflows.size,
                completedWorkflows = recentWorkflows.size,
                avgWorkflowDurationMs = avgDuration,
                workflowSuccessRate = successRate,
                stepPerformance = stepPerformance,
                workflowTypePerformance = workflowTypePerformance,
                bottlenecks = bottlenecks,
                recommendations = recommendations
            )
        }
    }
    
    /**
     * Get detailed workflow analytics.
     */
    suspend fun getWorkflowAnalytics(workflowType: WorkflowType? = null, durationHours: Int = 24): WorkflowAnalytics {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            val cutoff = now - (durationHours * 3600_000L)
            
            val workflows = completedWorkflows.filter { 
                it.startTime >= cutoff && (workflowType == null || it.workflowType == workflowType)
            }
            
            if (workflows.isEmpty()) {
                return@withLock WorkflowAnalytics(
                    workflowType = workflowType,
                    timeRangeHours = durationHours,
                    totalWorkflows = 0,
                    successfulWorkflows = 0,
                    failedWorkflows = 0,
                    avgDurationMs = 0L,
                    p50DurationMs = 0L,
                    p90DurationMs = 0L,
                    p99DurationMs = 0L,
                    stepAnalytics = emptyMap(),
                    performanceTrends = emptyList(),
                    errorPatterns = emptyMap()
                )
            }
            
            val durations = workflows.map { it.durationMs }.sorted()
            val stepAnalytics = analyzeStepPerformanceDetailed(workflows)
            val performanceTrends = analyzePerformanceTrends(workflows)
            val errorPatterns = analyzeErrorPatterns(workflows)
            
            WorkflowAnalytics(
                workflowType = workflowType,
                timeRangeHours = durationHours,
                totalWorkflows = workflows.size,
                successfulWorkflows = workflows.count { it.success },
                failedWorkflows = workflows.count { !it.success },
                avgDurationMs = durations.average().toLong(),
                p50DurationMs = durations[durations.size / 2],
                p90DurationMs = durations[(durations.size * 0.9).toInt()],
                p99DurationMs = durations[(durations.size * 0.99).toInt()],
                stepAnalytics = stepAnalytics,
                performanceTrends = performanceTrends,
                errorPatterns = errorPatterns
            )
        }
    }
    
    private fun analyzeStepPerformance(workflows: List<WorkflowExecution>): Map<StepType, StepPerformanceMetrics> {
        val allSteps = workflows.flatMap { it.steps }
        
        return allSteps.groupBy { it.stepType }.mapValues { (stepType, steps) ->
            val durations = steps.map { it.durationMs }
            StepPerformanceMetrics(
                stepType = stepType,
                totalExecutions = steps.size,
                successRate = steps.count { it.success }.toFloat() / steps.size,
                avgDurationMs = durations.average().toLong(),
                maxDurationMs = durations.maxOrNull() ?: 0L,
                minDurationMs = durations.minOrNull() ?: 0L
            )
        }
    }
    
    private fun analyzeWorkflowTypePerformance(workflows: List<WorkflowExecution>): Map<WorkflowType, WorkflowTypeMetrics> {
        return workflows.groupBy { it.workflowType }.mapValues { (workflowType, typeWorkflows) ->
            val durations = typeWorkflows.map { it.durationMs }
            WorkflowTypeMetrics(
                workflowType = workflowType,
                totalExecutions = typeWorkflows.size,
                successRate = typeWorkflows.count { it.success }.toFloat() / typeWorkflows.size,
                avgDurationMs = durations.average().toLong(),
                avgStepsCount = typeWorkflows.map { it.stepCount }.average().toInt()
            )
        }
    }
    
    private fun identifyBottlenecks(workflows: List<WorkflowExecution>): List<PerformanceBottleneck> {
        val bottlenecks = mutableListOf<PerformanceBottleneck>()
        
        // Analyze step duration bottlenecks
        val allSteps = workflows.flatMap { it.steps }
        val stepDurations = allSteps.groupBy { it.stepType }.mapValues { (_, steps) ->
            steps.map { it.durationMs }.average()
        }
        
        stepDurations.forEach { (stepType, avgDuration) ->
            if (avgDuration > 5000) { // Steps taking more than 5 seconds
                bottlenecks.add(
                    PerformanceBottleneck(
                        type = BottleneckType.SLOW_STEP,
                        description = "$stepType steps averaging ${avgDuration.toLong()}ms",
                        impact = when {
                            avgDuration > 10000 -> BottleneckImpact.HIGH
                            avgDuration > 7500 -> BottleneckImpact.MEDIUM
                            else -> BottleneckImpact.LOW
                        },
                        affectedWorkflows = workflows.count { workflow ->
                            workflow.steps.any { it.stepType == stepType && it.durationMs > avgDuration }
                        }
                    )
                )
            }
        }
        
        // Analyze workflow type bottlenecks
        val workflowDurations = workflows.groupBy { it.workflowType }.mapValues { (_, typeWorkflows) ->
            typeWorkflows.map { it.durationMs }.average()
        }
        
        workflowDurations.forEach { (workflowType, avgDuration) ->
            if (avgDuration > 15000) { // Workflows taking more than 15 seconds
                bottlenecks.add(
                    PerformanceBottleneck(
                        type = BottleneckType.SLOW_WORKFLOW,
                        description = "$workflowType workflows averaging ${avgDuration.toLong()}ms",
                        impact = when {
                            avgDuration > 30000 -> BottleneckImpact.HIGH
                            avgDuration > 20000 -> BottleneckImpact.MEDIUM
                            else -> BottleneckImpact.LOW
                        },
                        affectedWorkflows = workflows.count { it.workflowType == workflowType && it.durationMs > avgDuration }
                    )
                )
            }
        }
        
        return bottlenecks.sortedByDescending { 
            when (it.impact) {
                BottleneckImpact.HIGH -> 3
                BottleneckImpact.MEDIUM -> 2
                BottleneckImpact.LOW -> 1
            }
        }
    }
    
    private fun generateWorkflowRecommendations(
        workflows: List<WorkflowExecution>,
        bottlenecks: List<PerformanceBottleneck>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Recommendations based on bottlenecks
        bottlenecks.forEach { bottleneck ->
            when (bottleneck.type) {
                BottleneckType.SLOW_STEP -> {
                    recommendations.add("Optimize ${bottleneck.description}")
                    if (bottleneck.description.contains("AI_ANALYSIS")) {
                        recommendations.add("Consider enabling model preloading and result caching for AI analysis")
                    }
                    if (bottleneck.description.contains("DATABASE_OPERATIONS")) {
                        recommendations.add("Review database queries and consider adding indexes")
                    }
                }
                BottleneckType.SLOW_WORKFLOW -> {
                    recommendations.add("Investigate ${bottleneck.description}")
                }
                BottleneckType.HIGH_FAILURE_RATE -> {
                    recommendations.add("Address reliability issues in ${bottleneck.description}")
                }
            }
        }
        
        // General recommendations
        val avgWorkflowDuration = workflows.map { it.durationMs }.average()
        if (avgWorkflowDuration > 15000) {
            recommendations.add("Overall workflow performance exceeds target - enable performance optimizations")
        }
        
        val successRate = workflows.count { it.success }.toFloat() / workflows.size
        if (successRate < 0.9f) {
            recommendations.add("Workflow success rate below 90% - investigate error patterns")
        }
        
        return recommendations
    }
    
    private suspend fun checkStepPerformanceAlerts(workflowId: String, step: WorkflowStep) {
        // Alert for slow steps
        val slowStepThresholds = mapOf(
            StepType.PHOTO_CAPTURE to 3000L,
            StepType.AI_ANALYSIS to 8000L,
            StepType.DATABASE_OPERATIONS to 1000L,
            StepType.PDF_CREATION to 5000L,
            StepType.UI_RENDERING to 500L
        )
        
        val threshold = slowStepThresholds[step.stepType] ?: 5000L
        if (step.durationMs > threshold) {
            // Would emit alert in real implementation
        }
        
        // Alert for step failures
        if (!step.success) {
            _workflowAlertsFlow.emit(
                WorkflowPerformanceAlert.StepFailure(
                    workflowId, step.stepName, step.errorMessage ?: "Unknown error"
                )
            )
        }
    }
    
    private suspend fun checkWorkflowPerformanceAlerts(workflow: WorkflowExecution) {
        // Alert for slow workflows
        val workflowThresholds = mapOf(
            WorkflowType.PHOTO_CAPTURE_ANALYSIS to 15000L,
            WorkflowType.SAFETY_INSPECTION to 20000L,
            WorkflowType.INCIDENT_REPORTING to 10000L,
            WorkflowType.BATCH_ANALYSIS to 60000L
        )
        
        val threshold = workflowThresholds[workflow.workflowType] ?: 15000L
        if (workflow.durationMs > threshold) {
            _workflowAlertsFlow.emit(
                WorkflowPerformanceAlert.SlowWorkflow(
                    workflow.workflowId, workflow.workflowType, workflow.durationMs
                )
            )
        }
        
        // Check for high failure rate
        val recentSimilarWorkflows = completedWorkflows.filter { 
            it.workflowType == workflow.workflowType && 
            System.currentTimeMillis() - it.startTime <= 3600_000 // Last hour
        }
        
        if (recentSimilarWorkflows.size >= 5) {
            val failureRate = recentSimilarWorkflows.count { !it.success }.toFloat() / recentSimilarWorkflows.size
            if (failureRate > 0.2f) { // More than 20% failure rate
                _workflowAlertsFlow.emit(
                    WorkflowPerformanceAlert.HighFailureRate(workflow.workflowType, failureRate)
                )
            }
        }
    }
    
    private fun analyzeStepPerformanceDetailed(workflows: List<WorkflowExecution>): Map<StepType, DetailedStepAnalytics> {
        val allSteps = workflows.flatMap { it.steps }
        
        return allSteps.groupBy { it.stepType }.mapValues { (stepType, steps) ->
            val durations = steps.map { it.durationMs }.sorted()
            DetailedStepAnalytics(
                stepType = stepType,
                totalExecutions = steps.size,
                successfulExecutions = steps.count { it.success },
                failedExecutions = steps.count { !it.success },
                avgDurationMs = durations.average().toLong(),
                p50DurationMs = if (durations.isNotEmpty()) durations[durations.size / 2] else 0L,
                p90DurationMs = if (durations.isNotEmpty()) durations[(durations.size * 0.9).toInt()] else 0L,
                p99DurationMs = if (durations.isNotEmpty()) durations[(durations.size * 0.99).toInt()] else 0L,
                commonErrors = steps.filter { !it.success }.groupBy { it.errorMessage ?: "Unknown" }
                    .mapValues { it.value.size }.toList().sortedByDescending { it.second }.take(5)
            )
        }
    }
    
    private fun analyzePerformanceTrends(workflows: List<WorkflowExecution>): List<PerformanceTrend> {
        if (workflows.size < 10) return emptyList()
        
        val trends = mutableListOf<PerformanceTrend>()
        
        // Analyze duration trends
        val sortedWorkflows = workflows.sortedBy { it.startTime }
        val firstHalf = sortedWorkflows.take(workflows.size / 2)
        val secondHalf = sortedWorkflows.drop(workflows.size / 2)
        
        val firstHalfAvgDuration = firstHalf.map { it.durationMs }.average()
        val secondHalfAvgDuration = secondHalf.map { it.durationMs }.average()
        
        val durationChange = (secondHalfAvgDuration - firstHalfAvgDuration) / firstHalfAvgDuration
        
        if (kotlin.math.abs(durationChange) > 0.1) { // 10% change
            trends.add(
                PerformanceTrend(
                    metric = "Average Duration",
                    direction = if (durationChange > 0) TrendDirection.INCREASING else TrendDirection.DECREASING,
                    changePercent = (durationChange * 100).toFloat(),
                    significance = when {
                        kotlin.math.abs(durationChange) > 0.3 -> TrendSignificance.HIGH
                        kotlin.math.abs(durationChange) > 0.2 -> TrendSignificance.MEDIUM
                        else -> TrendSignificance.LOW
                    }
                )
            )
        }
        
        return trends
    }
    
    private fun analyzeErrorPatterns(workflows: List<WorkflowExecution>): Map<String, Int> {
        val allErrors = workflows.filter { !it.success }.mapNotNull { it.errorMessage } +
                       workflows.flatMap { it.steps }.filter { !it.success }.mapNotNull { it.errorMessage }
        
        return allErrors.groupBy { it }.mapValues { it.value.size }
            .toList().sortedByDescending { it.second }.take(10).toMap()
    }
}

// Data classes for workflow performance metrics
data class WorkflowPerformanceMetrics(
    val timestamp: Long,
    val activeWorkflows: Int,
    val completedWorkflows: Int,
    val avgWorkflowDurationMs: Long,
    val workflowSuccessRate: Float,
    val stepPerformance: Map<StepType, StepPerformanceMetrics>,
    val workflowTypePerformance: Map<WorkflowType, WorkflowTypeMetrics>,
    val bottlenecks: List<PerformanceBottleneck>,
    val recommendations: List<String>
)

data class StepPerformanceMetrics(
    val stepType: StepType,
    val totalExecutions: Int,
    val successRate: Float,
    val avgDurationMs: Long,
    val maxDurationMs: Long,
    val minDurationMs: Long
)

data class WorkflowTypeMetrics(
    val workflowType: WorkflowType,
    val totalExecutions: Int,
    val successRate: Float,
    val avgDurationMs: Long,
    val avgStepsCount: Int
)

data class PerformanceBottleneck(
    val type: BottleneckType,
    val description: String,
    val impact: BottleneckImpact,
    val affectedWorkflows: Int
)

enum class BottleneckType {
    SLOW_STEP,
    SLOW_WORKFLOW,
    HIGH_FAILURE_RATE
}

enum class BottleneckImpact {
    LOW, MEDIUM, HIGH
}

data class WorkflowAnalytics(
    val workflowType: WorkflowType?,
    val timeRangeHours: Int,
    val totalWorkflows: Int,
    val successfulWorkflows: Int,
    val failedWorkflows: Int,
    val avgDurationMs: Long,
    val p50DurationMs: Long,
    val p90DurationMs: Long,
    val p99DurationMs: Long,
    val stepAnalytics: Map<StepType, DetailedStepAnalytics>,
    val performanceTrends: List<PerformanceTrend>,
    val errorPatterns: Map<String, Int>
)

data class DetailedStepAnalytics(
    val stepType: StepType,
    val totalExecutions: Int,
    val successfulExecutions: Int,
    val failedExecutions: Int,
    val avgDurationMs: Long,
    val p50DurationMs: Long,
    val p90DurationMs: Long,
    val p99DurationMs: Long,
    val commonErrors: List<Pair<String, Int>>
)

data class PerformanceTrend(
    val metric: String,
    val direction: TrendDirection,
    val changePercent: Float,
    val significance: TrendSignificance
)

enum class TrendDirection {
    INCREASING, DECREASING, STABLE
}

enum class TrendSignificance {
    LOW, MEDIUM, HIGH
}