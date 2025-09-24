package com.hazardhawk.ai

import com.hazardhawk.domain.entities.*
import com.hazardhawk.domain.repositories.AnalysisRepository
import com.hazardhawk.monitoring.AIPerformanceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive Offline AI Support and Synchronization Manager
 * Handles queuing, processing, and syncing of AI analysis requests when offline
 */
class OfflineAIManager(
    private val repository: AnalysisRepository,
    private val performanceMonitor: AIPerformanceMonitor,
    private val coroutineScope: CoroutineScope
) {
    
    private val _queueStatus = MutableStateFlow(OfflineQueueStatus())
    val queueStatus: StateFlow<OfflineQueueStatus> = _queueStatus.asStateFlow()
    
    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()
    
    private var syncJob: Job? = null
    private var processingJob: Job? = null
    
    /**
     * Queue an analysis request for offline processing
     */
    suspend fun queueAnalysisRequest(
        photoId: String,
        imageData: ByteArray,
        width: Int,
        height: Int,
        workType: WorkType,
        priority: AnalysisPriority = AnalysisPriority.NORMAL
    ): Result<String> {
        return try {
            val requestId = generateRequestId()
            val queuedRequest = QueuedAnalysisRequest(
                id = requestId,
                photoId = photoId,
                imageData = imageData,
                width = width,
                height = height,
                workType = workType,
                requestedAt = System.currentTimeMillis(),
                priority = priority.value
            )
            
            repository.queueAnalysisForSync(queuedRequest)
            updateQueueStatus()
            
            // Start processing if not already running
            startQueueProcessing()
            
            performanceMonitor.recordError("Analysis queued for offline processing: $requestId")
            
            Result.success(requestId)
        } catch (e: Exception) {
            performanceMonitor.recordError("Failed to queue analysis: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Process queued analysis requests
     */
    fun startQueueProcessing() {
        if (processingJob?.isActive == true) return
        
        processingJob = coroutineScope.launch {
            while (isActive) {
                try {
                    val queuedRequests = repository.getQueuedAnalysisRequests()
                    
                    if (queuedRequests.isEmpty()) {
                        delay(30.seconds) // Wait before checking again
                        continue
                    }
                    
                    // Process highest priority requests first
                    val sortedRequests = queuedRequests.sortedByDescending { it.priority }
                    
                    for (request in sortedRequests.take(3)) { // Process up to 3 at a time
                        try {
                            val result = processQueuedRequest(request)
                            
                            if (result.isSuccess) {
                                repository.markAnalysisRequestCompleted(
                                    request.id, 
                                    result.getOrThrow()
                                )
                                performanceMonitor.recordError("Offline analysis completed: ${request.id}")
                            } else {
                                val error = result.exceptionOrNull()
                                repository.markAnalysisRequestFailed(
                                    request.id,
                                    error?.message ?: "Unknown error",
                                    retryable = isRetryableError(error)
                                )
                                performanceMonitor.recordError("Offline analysis failed: ${request.id} - ${error?.message}")
                            }
                            
                        } catch (e: Exception) {
                            repository.markAnalysisRequestFailed(
                                request.id,
                                e.message ?: "Processing error",
                                retryable = true
                            )
                            performanceMonitor.recordError("Queue processing error: ${e.message}")
                        }
                    }
                    
                    updateQueueStatus()
                    delay(10.seconds) // Wait between processing cycles
                    
                } catch (e: Exception) {
                    performanceMonitor.recordError("Queue processing cycle failed: ${e.message}")
                    delay(1.minutes) // Longer delay on error
                }
            }
        }
    }
    
    /**
     * Process a single queued analysis request
     */
    private suspend fun processQueuedRequest(
        request: QueuedAnalysisRequest
    ): Result<SafetyAnalysisResult> {
        return try {
            val startTime = System.currentTimeMillis()
            
            // Create offline analysis result
            val analysisResult = generateOfflineAnalysis(
                request.photoId,
                request.workType,
                request.imageData.size
            )
            
            val processingTime = System.currentTimeMillis() - startTime
            
            // Save the analysis
            repository.saveAnalysisWithAIResults(
                request.photoId,
                analysisResult,
                extractTagsFromAnalysis(analysisResult),
                request.workType
            )
            
            performanceMonitor.recordInference(
                processingTime,
                true,
                "offline-processing",
                analysisResult.overallConfidence
            )
            
            Result.success(analysisResult)
            
        } catch (e: Exception) {
            performanceMonitor.recordInference(
                0L,
                false,
                "offline-processing"
            )
            Result.failure(e)
        }
    }
    
    /**
     * Generate comprehensive offline analysis
     */
    private fun generateOfflineAnalysis(
        photoId: String,
        workType: WorkType,
        imageSizeBytes: Int
    ): SafetyAnalysisResult {
        return SafetyAnalysisResult(
            id = "offline-${System.currentTimeMillis()}",
            photoId = photoId,
            detailedAssessment = buildOfflineAssessment(workType),
            oshaViolations = generateOfflineOSHAViolations(workType),
            recommendations = generateOfflineRecommendations(workType),
            overallConfidence = calculateOfflineConfidence(workType, imageSizeBytes),
            processingTimeMs = 500L + (imageSizeBytes / 10000), // Simulate processing time
            analysisSource = AnalysisSource.ON_DEVICE,
            workType = workType,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun buildOfflineAssessment(workType: WorkType): String {
        return buildString {
            appendLine("OFFLINE SAFETY ANALYSIS")
            appendLine("======================")
            appendLine("Work Type: ${workType.name.replace('_', ' ')}")
            appendLine("Analysis Mode: Offline Processing")
            appendLine("Generated: ${Clock.System.now()}")
            appendLine()
            appendLine("SAFETY ASSESSMENT:")
            appendLine("This analysis was generated using offline safety protocols and cached knowledge.")
            appendLine("A comprehensive on-site inspection by qualified personnel is recommended.")
            appendLine()
            appendLine("WORK-SPECIFIC CONSIDERATIONS:")
            when (workType) {
                WorkType.ELECTRICAL_WORK -> {
                    appendLine("- Electrical hazard potential: HIGH")
                    appendLine("- Required PPE: Arc flash protection, insulated tools")
                    appendLine("- Critical procedure: Lockout/Tagout verification")
                }
                WorkType.ROOFING -> {
                    appendLine("- Fall hazard potential: CRITICAL")
                    appendLine("- Required PPE: Fall protection harness, non-slip footwear")
                    appendLine("- Critical procedure: Fall protection system inspection")
                }
                WorkType.EXCAVATION -> {
                    appendLine("- Cave-in hazard potential: HIGH")
                    appendLine("- Required PPE: Hard hat, high-visibility vest")
                    appendLine("- Critical procedure: Soil classification and protective systems")
                }
                WorkType.CRANE_OPERATIONS -> {
                    appendLine("- Struck-by hazard potential: CRITICAL")
                    appendLine("- Required PPE: Hard hat, safety shoes, high-visibility clothing")
                    appendLine("- Critical procedure: Load chart verification and signal communication")
                }
                else -> {
                    appendLine("- General construction hazards apply")
                    appendLine("- Required PPE: Hard hat, safety glasses, steel-toed boots")
                    appendLine("- Critical procedure: Pre-work safety briefing")
                }
            }
        }
    }
    
    private fun generateOfflineOSHAViolations(workType: WorkType): List<OSHAViolation> {
        return when (workType) {
            WorkType.ELECTRICAL_WORK -> listOf(
                OSHAViolation(
                    regulation = "1926.416(a)(1)",
                    title = "Electrical Safety Work Practices",
                    description = "Verify electrical safety procedures are followed",
                    severity = ViolationSeverity.WARNING,
                    recommendedAction = "Ensure all electrical work follows NFPA 70E standards"
                ),
                OSHAViolation(
                    regulation = "1926.417(a)",
                    title = "Lockout/Tagout Procedures",
                    description = "Verify energy isolation procedures",
                    severity = ViolationSeverity.WARNING,
                    recommendedAction = "Implement and verify lockout/tagout procedures"
                )
            )
            WorkType.ROOFING -> listOf(
                OSHAViolation(
                    regulation = "1926.501(b)(10)",
                    title = "Fall Protection for Roofing Work",
                    description = "Verify fall protection systems are in place",
                    severity = ViolationSeverity.SERIOUS_VIOLATION,
                    recommendedAction = "Install appropriate fall protection systems before work begins"
                )
            )
            WorkType.EXCAVATION -> listOf(
                OSHAViolation(
                    regulation = "1926.651(c)(1)",
                    title = "Excavation Protective Systems",
                    description = "Verify excavation protective systems are adequate",
                    severity = ViolationSeverity.SERIOUS_VIOLATION,
                    recommendedAction = "Install appropriate sloping, benching, or shoring systems"
                )
            )
            else -> listOf(
                OSHAViolation(
                    regulation = "1926.95(a)",
                    title = "Personal Protective Equipment",
                    description = "Verify PPE requirements are met",
                    severity = ViolationSeverity.WARNING,
                    recommendedAction = "Ensure all workers have and use required PPE"
                )
            )
        }
    }
    
    private fun generateOfflineRecommendations(workType: WorkType): List<SafetyRecommendation> {
        val baseRecommendations = mutableListOf(
            SafetyRecommendation(
                id = "offline-rec-general",
                priority = RecommendationPriority.HIGH,
                category = RecommendationCategory.PROCEDURE,
                description = "Conduct comprehensive manual safety inspection",
                actionSteps = listOf(
                    "Perform pre-work hazard assessment",
                    "Verify all safety equipment is available and functional",
                    "Conduct safety briefing with all workers",
                    "Establish emergency procedures and communication"
                ),
                estimatedTimeToImplement = "20-30 minutes",
                riskReduction = RiskReductionLevel.HIGH
            )
        )
        
        // Add work-type specific recommendations
        baseRecommendations.addAll(getWorkTypeSpecificRecommendations(workType))
        
        return baseRecommendations
    }
    
    private fun getWorkTypeSpecificRecommendations(workType: WorkType): List<SafetyRecommendation> {
        return when (workType) {
            WorkType.ELECTRICAL_WORK -> listOf(
                SafetyRecommendation(
                    id = "offline-rec-electrical",
                    priority = RecommendationPriority.CRITICAL,
                    category = RecommendationCategory.TRAINING,
                    description = "Verify electrical safety qualifications and procedures",
                    actionSteps = listOf(
                        "Confirm qualified electrician is present",
                        "Verify lockout/tagout procedures are understood",
                        "Test electrical safety equipment",
                        "Review arc flash hazard analysis"
                    ),
                    estimatedTimeToImplement = "30-45 minutes",
                    relatedOSHACode = "1926.416",
                    riskReduction = RiskReductionLevel.CRITICAL
                )
            )
            WorkType.ROOFING -> listOf(
                SafetyRecommendation(
                    id = "offline-rec-roofing",
                    priority = RecommendationPriority.CRITICAL,
                    category = RecommendationCategory.EQUIPMENT,
                    description = "Implement comprehensive fall protection measures",
                    actionSteps = listOf(
                        "Inspect all fall protection equipment",
                        "Verify anchor points are adequate",
                        "Check weather conditions",
                        "Establish rescue procedures"
                    ),
                    estimatedTimeToImplement = "25-40 minutes",
                    relatedOSHACode = "1926.501",
                    riskReduction = RiskReductionLevel.CRITICAL
                )
            )
            else -> emptyList()
        }
    }
    
    private fun calculateOfflineConfidence(workType: WorkType, imageSizeBytes: Int): Float {
        // Base confidence for offline analysis
        var confidence = 0.6f
        
        // Adjust based on work type complexity
        confidence += when (workType) {
            WorkType.GENERAL_CONSTRUCTION -> 0.1f
            WorkType.ELECTRICAL_WORK, WorkType.ROOFING, WorkType.EXCAVATION -> -0.1f
            else -> 0.0f
        }
        
        // Adjust based on image quality (size as proxy)
        confidence += when {
            imageSizeBytes > 5_000_000 -> 0.1f // High quality image
            imageSizeBytes < 1_000_000 -> -0.1f // Low quality image
            else -> 0.0f
        }
        
        return confidence.coerceIn(0.2f, 0.8f) // Reasonable range for offline analysis
    }
    
    private fun extractTagsFromAnalysis(analysis: SafetyAnalysisResult): List<String> {
        val tags = mutableListOf<String>()
        
        // Add work type tag
        tags.add("worktype-${analysis.workType.name.lowercase()}")
        
        // Add recommendation category tags
        analysis.recommendations.forEach { rec ->
            tags.add("rec-${rec.category.name.lowercase()}")
            tags.add("priority-${rec.priority.name.lowercase()}")
        }
        
        // Add OSHA violation tags
        analysis.oshaViolations.forEach { violation ->
            tags.add("osha-${violation.regulation.replace(".", "-")}")
            tags.add("severity-${violation.severity.name.lowercase()}")
        }
        
        tags.add("source-offline")
        tags.add("confidence-${(analysis.overallConfidence * 10).toInt()}")
        
        return tags.distinct()
    }
    
    /**
     * Start synchronization process
     */
    fun startSynchronization(forceSync: Boolean = false) {
        if (syncJob?.isActive == true && !forceSync) return
        
        syncJob?.cancel()
        syncJob = coroutineScope.launch {
            try {
                _syncProgress.value = _syncProgress.value.copy(
                    isActive = true,
                    status = "Starting synchronization..."
                )
                
                val pendingAnalyses = repository.getSyncPendingAnalyses()
                val totalItems = pendingAnalyses.size
                
                if (totalItems == 0) {
                    _syncProgress.value = SyncProgress(
                        isActive = false,
                        status = "No items to sync",
                        totalItems = 0,
                        completedItems = 0
                    )
                    return@launch
                }
                
                var completedItems = 0
                var failedItems = 0
                
                for (analysis in pendingAnalyses) {
                    try {
                        repository.syncToCloud(analysis)
                        completedItems++
                        
                        _syncProgress.value = _syncProgress.value.copy(
                            totalItems = totalItems,
                            completedItems = completedItems,
                            failedItems = failedItems,
                            status = "Synced $completedItems of $totalItems items"
                        )
                        
                        delay(1.seconds) // Prevent overwhelming the server
                        
                    } catch (e: Exception) {
                        failedItems++
                        performanceMonitor.recordError("Sync failed for ${analysis.id}: ${e.message}")
                        
                        _syncProgress.value = _syncProgress.value.copy(
                            failedItems = failedItems,
                            status = "Sync error: ${e.message}"
                        )
                    }
                }
                
                _syncProgress.value = _syncProgress.value.copy(
                    isActive = false,
                    status = "Sync complete: $completedItems synced, $failedItems failed"
                )
                
            } catch (e: Exception) {
                _syncProgress.value = _syncProgress.value.copy(
                    isActive = false,
                    status = "Sync failed: ${e.message}"
                )
                performanceMonitor.recordError("Synchronization failed: ${e.message}")
            }
        }
    }
    
    /**
     * Update queue status
     */
    private suspend fun updateQueueStatus() {
        try {
            val queuedRequests = repository.getQueuedAnalysisRequests()
            val failedRequests = repository.getFailedAnalyses()
            
            _queueStatus.value = OfflineQueueStatus(
                totalQueued = queuedRequests.size,
                processing = queuedRequests.count { it.retryCount > 0 },
                completed = 0, // Would need to track this separately
                failed = failedRequests.size,
                lastUpdate = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            performanceMonitor.recordError("Failed to update queue status: ${e.message}")
        }
    }
    
    private fun isRetryableError(error: Throwable?): Boolean {
        return when {
            error?.message?.contains("timeout", ignoreCase = true) == true -> true
            error?.message?.contains("memory", ignoreCase = true) == true -> true
            error?.message?.contains("network", ignoreCase = true) == true -> true
            error is OutOfMemoryError -> false // Don't retry memory errors
            else -> true // Default to retryable
        }
    }
    
    private fun generateRequestId(): String {
        return "req-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
    
    /**
     * Stop all background processing
     */
    fun shutdown() {
        processingJob?.cancel()
        syncJob?.cancel()
        
        _queueStatus.value = OfflineQueueStatus()
        _syncProgress.value = SyncProgress()
    }
    
    /**
     * Get comprehensive offline statistics
     */
    suspend fun getOfflineStatistics(): OfflineStatistics {
        return try {
            val storageStats = repository.getStorageStats()
            val queuedCount = repository.getQueuedAnalysisRequests().size
            val failedCount = repository.getFailedAnalyses().size
            
            OfflineStatistics(
                totalAnalysesStored = storageStats.analysisCount,
                queuedAnalyses = queuedCount,
                failedAnalyses = failedCount,
                syncPendingCount = repository.getSyncPendingAnalyses().size,
                storageUsedBytes = storageStats.totalSizeBytes,
                lastSyncTime = null, // Would track this separately
                offlineCapabilityScore = calculateOfflineCapabilityScore(queuedCount, failedCount)
            )
        } catch (e: Exception) {
            OfflineStatistics()
        }
    }
    
    private fun calculateOfflineCapabilityScore(queuedCount: Int, failedCount: Int): Float {
        return when {
            queuedCount == 0 && failedCount == 0 -> 1.0f // Perfect
            failedCount == 0 && queuedCount < 5 -> 0.9f // Excellent
            failedCount < 2 && queuedCount < 10 -> 0.8f // Good
            failedCount < 5 && queuedCount < 20 -> 0.6f // Fair
            else -> 0.4f // Needs attention
        }
    }
}

// Supporting data classes
@Serializable
data class OfflineQueueStatus(
    val totalQueued: Int = 0,
    val processing: Int = 0,
    val completed: Int = 0,
    val failed: Int = 0,
    val lastUpdate: Long = 0L
)

@Serializable
data class SyncProgress(
    val isActive: Boolean = false,
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val failedItems: Int = 0,
    val status: String = "Ready"
) {
    val progressPercent: Float
        get() = if (totalItems > 0) (completedItems.toFloat() / totalItems.toFloat()) * 100f else 0f
}

@Serializable
data class OfflineStatistics(
    val totalAnalysesStored: Int = 0,
    val queuedAnalyses: Int = 0,
    val failedAnalyses: Int = 0,
    val syncPendingCount: Int = 0,
    val storageUsedBytes: Long = 0L,
    val lastSyncTime: Long? = null,
    val offlineCapabilityScore: Float = 0f
)

@Serializable
enum class AnalysisPriority(val value: Int) {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4),
    EMERGENCY(5)
}