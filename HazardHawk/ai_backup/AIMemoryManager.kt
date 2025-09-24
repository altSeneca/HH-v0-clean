package com.hazardhawk.ai

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive memory management system for AI operations.
 * 
 * Optimizes memory usage, prevents OOM errors, and provides intelligent
 * memory allocation strategies for HazardHawk AI processing.
 */
class AIMemoryManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    
    private val _memoryStatus = MutableStateFlow(MemoryStatus.initial())
    val memoryStatus: StateFlow<MemoryStatus> = _memoryStatus.asStateFlow()
    
    private var memoryStrategy: MemoryStrategy = MemoryStrategy.BALANCED
    private var maxMemoryThresholdMB = 2048
    private var criticalMemoryThresholdMB = 1800
    
    // Memory tracking
    private var peakMemoryUsageMB = 0
    private var lastGarbageCollectionTime = 0L
    private var memoryLeakDetection = mutableMapOf<String, Int>()
    
    /**
     * Initialize memory management with device-specific settings.
     */
    fun initialize(
        totalMemoryMB: Int,
        availableMemoryMB: Int,
        strategy: MemoryStrategy = MemoryStrategy.BALANCED
    ) {
        memoryStrategy = strategy
        
        // Set thresholds based on available memory and strategy
        when (strategy) {
            MemoryStrategy.CONSERVATIVE -> {
                maxMemoryThresholdMB = (availableMemoryMB * 0.6).toInt()
                criticalMemoryThresholdMB = (availableMemoryMB * 0.5).toInt()
            }
            MemoryStrategy.BALANCED -> {
                maxMemoryThresholdMB = (availableMemoryMB * 0.75).toInt()
                criticalMemoryThresholdMB = (availableMemoryMB * 0.65).toInt()
            }
            MemoryStrategy.AGGRESSIVE -> {
                maxMemoryThresholdMB = (availableMemoryMB * 0.85).toInt()
                criticalMemoryThresholdMB = (availableMemoryMB * 0.75).toInt()
            }
            MemoryStrategy.MAXIMUM_PERFORMANCE -> {
                maxMemoryThresholdMB = (availableMemoryMB * 0.9).toInt()
                criticalMemoryThresholdMB = (availableMemoryMB * 0.8).toInt()
            }
        }
        
        startMemoryMonitoring()
    }
    
    /**
     * Check if memory operation is safe to perform.
     */
    fun isMemoryOperationSafe(
        operationType: MemoryOperationType,
        estimatedMemoryMB: Int
    ): MemoryOperationResult {
        val currentMemoryMB = getCurrentMemoryUsageMB()
        val projectedMemoryMB = currentMemoryMB + estimatedMemoryMB
        
        return when {
            projectedMemoryMB > maxMemoryThresholdMB -> {
                MemoryOperationResult(
                    canProceed = false,
                    recommendation = MemoryRecommendation.FORCE_GARBAGE_COLLECTION,
                    reason = "Projected memory usage ($projectedMemoryMB MB) exceeds threshold ($maxMemoryThresholdMB MB)",
                    alternativeStrategy = getAlternativeStrategy(operationType)
                )
            }
            projectedMemoryMB > criticalMemoryThresholdMB -> {
                MemoryOperationResult(
                    canProceed = true,
                    recommendation = MemoryRecommendation.REDUCE_QUALITY,
                    reason = "Memory usage approaching critical threshold",
                    alternativeStrategy = getAlternativeStrategy(operationType)
                )
            }
            else -> {
                MemoryOperationResult(
                    canProceed = true,
                    recommendation = MemoryRecommendation.PROCEED_NORMALLY,
                    reason = "Memory usage within safe limits",
                    alternativeStrategy = null
                )
            }
        }
    }
    
    /**
     * Generate comprehensive memory report.
     */
    fun generateMemoryReport(): MemoryReport {
        val currentStatus = _memoryStatus.value
        
        return MemoryReport(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            currentMemoryUsageMB = currentStatus.currentMemoryUsageMB,
            peakMemoryUsageMB = peakMemoryUsageMB,
            allocatedMemoryMB = currentStatus.allocatedMemoryMB,
            availableMemoryMB = maxMemoryThresholdMB - currentStatus.currentMemoryUsageMB,
            memoryEfficiency = calculateMemoryEfficiency(),
            memoryStrategy = memoryStrategy,
            gcFrequency = currentStatus.garbageCollectionCount,
            memoryLeaksDetected = detectMemoryLeaks().size,
            thresholds = MemoryThresholds(
                maxMemoryMB = maxMemoryThresholdMB,
                criticalMemoryMB = criticalMemoryThresholdMB,
                warningMemoryMB = (criticalMemoryThresholdMB * 0.9).toInt()
            )
        )
    }
    
    private fun startMemoryMonitoring() {
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(2.seconds)
                updateMemoryStatus()
            }
        }
    }
    
    private fun updateMemoryStatus() {
        val currentMemory = getCurrentMemoryUsageMB()
        
        if (currentMemory > peakMemoryUsageMB) {
            peakMemoryUsageMB = currentMemory
        }
        
        val updated = _memoryStatus.value.copy(
            currentMemoryUsageMB = currentMemory,
            peakMemoryUsageMB = peakMemoryUsageMB,
            lastUpdateTime = Clock.System.now().toEpochMilliseconds()
        )
        
        _memoryStatus.value = updated
    }
    
    private fun detectMemoryLeaks(): Map<String, Int> {
        return memoryLeakDetection.filter { it.value > 100 } // Operations using > 100MB
    }
    
    private fun getAlternativeStrategy(operationType: MemoryOperationType): AlternativeStrategy? {
        return when (operationType) {
            MemoryOperationType.MODEL_LOADING -> AlternativeStrategy.USE_QUANTIZED_MODEL
            MemoryOperationType.IMAGE_PREPROCESSING -> AlternativeStrategy.REDUCE_RESOLUTION
            MemoryOperationType.INFERENCE -> AlternativeStrategy.BATCH_PROCESSING
            MemoryOperationType.CACHE_ALLOCATION -> AlternativeStrategy.STREAMING_MODE
        }
    }
    
    private fun calculateMemoryEfficiency(): Float {
        val totalAllocated = _memoryStatus.value.allocatedMemoryMB
        val peakUsage = peakMemoryUsageMB
        
        return if (peakUsage > 0) {
            (totalAllocated.toFloat() / peakUsage).coerceAtMost(1f)
        } else {
            1f
        }
    }
    
    // Platform-specific implementations would override these
    protected open fun getCurrentMemoryUsageMB(): Int = 512
}

/**
 * Current memory status information.
 */
@Serializable
data class MemoryStatus(
    val currentMemoryUsageMB: Int,
    val peakMemoryUsageMB: Int,
    val allocatedMemoryMB: Int,
    val garbageCollectionCount: Int,
    val allocationCount: Int,
    val lastAllocationTime: Long,
    val lastDeallocationTime: Long,
    val lastUpdateTime: Long
) {
    companion object {
        fun initial() = MemoryStatus(
            currentMemoryUsageMB = 0,
            peakMemoryUsageMB = 0,
            allocatedMemoryMB = 0,
            garbageCollectionCount = 0,
            allocationCount = 0,
            lastAllocationTime = 0L,
            lastDeallocationTime = 0L,
            lastUpdateTime = Clock.System.now().toEpochMilliseconds()
        )
    }
}

/**
 * Memory operation result with recommendations.
 */
@Serializable
data class MemoryOperationResult(
    val canProceed: Boolean,
    val recommendation: MemoryRecommendation,
    val reason: String,
    val alternativeStrategy: AlternativeStrategy?
)

/**
 * Comprehensive memory report.
 */
@Serializable
data class MemoryReport(
    val timestamp: Long,
    val currentMemoryUsageMB: Int,
    val peakMemoryUsageMB: Int,
    val allocatedMemoryMB: Int,
    val availableMemoryMB: Int,
    val memoryEfficiency: Float,
    val memoryStrategy: MemoryStrategy,
    val gcFrequency: Int,
    val memoryLeaksDetected: Int,
    val thresholds: MemoryThresholds
)

/**
 * Memory thresholds for monitoring.
 */
@Serializable
data class MemoryThresholds(
    val maxMemoryMB: Int,
    val criticalMemoryMB: Int,
    val warningMemoryMB: Int
)

// Enums for memory management
@Serializable
enum class MemoryOperationType {
    MODEL_LOADING,
    IMAGE_PREPROCESSING,
    INFERENCE,
    CACHE_ALLOCATION
}

@Serializable
enum class MemoryRecommendation {
    PROCEED_NORMALLY,
    REDUCE_QUALITY,
    FORCE_GARBAGE_COLLECTION,
    DEFER_OPERATION,
    USE_ALTERNATIVE
}

@Serializable
enum class AlternativeStrategy {
    USE_QUANTIZED_MODEL,
    REDUCE_RESOLUTION,
    BATCH_PROCESSING,
    STREAMING_MODE
}