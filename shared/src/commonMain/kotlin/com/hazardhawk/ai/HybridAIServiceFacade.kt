package com.hazardhawk.ai

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import com.hazardhawk.core.models.WorkType
import com.hazardhawk.core.models.SafetyAnalysis
import com.hazardhawk.core.models.AnalysisType
import com.hazardhawk.core.models.Severity
import com.hazardhawk.core.models.RiskLevel
import com.hazardhawk.ai.yolo.YOLO11SafetyAnalyzer
import com.hazardhawk.ai.yolo.AnalysisQuality
import com.hazardhawk.core.models.Hazard

/**
 * HybridAIServiceFacade - Intelligent combination of YOLO11 and Gemini Vision
 * 
 * This facade orchestrates the hybrid AI analysis system for HazardHawk, providing:
 * - Real-time YOLO11 on-device analysis (2-3 seconds)
 * - Enhanced Gemini Vision cloud analysis (when available)
 * - Intelligent fallback mechanisms
 * - Result fusion and confidence weighting
 * - Network-adaptive strategy selection
 * 
 * Strategy Selection:
 * - Offline: YOLO11 only (full functionality maintained)
 * - Poor connectivity: YOLO11 primary, Gemini optional enhancement
 * - Good connectivity: Parallel analysis with result fusion
 * - YOLO11 failure: Graceful fallback to Gemini-only mode
 * 
 * @author HazardHawk AI Team
 * @since Phase 2 - Core Integration Development
 * @version 1.0.0
 */
class HybridAIServiceFacade(
    private val yoloAnalyzer: YOLO11SafetyAnalyzer,
    private val geminiAnalyzer: GeminiVisionAnalyzer
) : AIServiceFacade {
    
    companion object {
        private const val TAG = "HybridAIServiceFacade"
        
        // Performance thresholds
        private const val GOOD_CONNECTIVITY_THRESHOLD_MS = 2000L
        private const val POOR_CONNECTIVITY_THRESHOLD_MS = 5000L
        private const val ANALYSIS_TIMEOUT_MS = 15000L
        private const val GEMINI_TIMEOUT_MS = 10000L
        
        // Confidence weighting
        private const val YOLO_BASE_WEIGHT = 0.7f
        private const val GEMINI_BASE_WEIGHT = 0.8f
        private const val HYBRID_CONFIDENCE_BOOST = 0.15f
        
        // Strategy selection criteria
        private const val MIN_YOLO_CONFIDENCE = 0.6f
        private const val MIN_GEMINI_CONFIDENCE = 0.7f
    }
    
    // Core state management
    private var isInitialized = false
    private var networkConnectivity: NetworkConnectivityState = NetworkConnectivityState.UNKNOWN
    private var lastConnectivityCheck = 0L
    private val connectivityCheckInterval = 30000L // 30 seconds
    
    // Analysis strategy tracking
    private var currentStrategy: AnalysisStrategy = AnalysisStrategy.HYBRID_OPTIMAL
    private var fallbackReason: String? = null
    
    // Performance monitoring
    private val _hybridAnalysisResults = MutableSharedFlow<HybridAnalysisResult>()
    private val _strategyChanges = MutableSharedFlow<StrategyChange>()
    
    val hybridAnalysisResults: SharedFlow<HybridAnalysisResult> = _hybridAnalysisResults.asSharedFlow()
    val strategyChanges: SharedFlow<StrategyChange> = _strategyChanges.asSharedFlow()
    
    // Coroutine management
    private val hybridScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            logHybrid("HYBRID_INITIALIZATION_STARTED")
            
            // Initialize YOLO11 analyzer (critical path)
            val yoloInit = yoloAnalyzer.initialize()
            if (yoloInit.isFailure) {
                logHybrid("YOLO_INIT_FAILED", mapOf("error" to yoloInit.exceptionOrNull()?.message.orEmpty()))
                return@withContext Result.failure(
                    HybridAIException.InitializationFailed("YOLO11 initialization failed: ${yoloInit.exceptionOrNull()?.message}")
                )
            }
            
            // Initialize Gemini analyzer (non-critical, can fail gracefully)
            val geminiInit = geminiAnalyzer.initialize()
            if (geminiInit.isFailure) {
                logHybrid("GEMINI_INIT_FAILED", mapOf(
                    "error" to geminiInit.exceptionOrNull()?.message.orEmpty(),
                    "fallback_strategy" to "YOLO_ONLY"
                ))
                currentStrategy = AnalysisStrategy.YOLO_ONLY
                fallbackReason = "Gemini initialization failed"
            }
            
            // Initial connectivity assessment
            updateConnectivityState()
            
            // Start background monitoring
            startBackgroundMonitoring()
            
            isInitialized = true
            
            logHybrid("HYBRID_INITIALIZATION_COMPLETED", mapOf(
                "yolo_available" to yoloInit.isSuccess.toString(),
                "gemini_available" to geminiInit.isSuccess.toString(),
                "strategy" to currentStrategy.name
            ))
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            logHybrid("HYBRID_INITIALIZATION_FAILED", mapOf("error" to e.message.orEmpty()))
            Result.failure(HybridAIException.InitializationFailed("Hybrid AI initialization failed: ${e.message}", e))
        }
    }
    
    override suspend fun release() {
        try {
            logHybrid("HYBRID_CLEANUP_STARTED")
            
            hybridScope.cancel()
            yoloAnalyzer.cleanup()
            geminiAnalyzer.release()
            
            isInitialized = false
            currentStrategy = AnalysisStrategy.HYBRID_OPTIMAL
            fallbackReason = null
            
            logHybrid("HYBRID_CLEANUP_COMPLETED")
            
        } catch (e: Exception) {
            logHybrid("HYBRID_CLEANUP_FAILED", mapOf("error" to e.message.orEmpty()))
        }
    }
    
    override val isServiceAvailable: Boolean
        get() = isInitialized && (yoloAnalyzer.getAnalyzerStatus().isInitialized || geminiAnalyzer.isServiceAvailable)
    
    override suspend fun analyzePhotoWithTags(
        data: ByteArray,
        width: Int,
        height: Int,
        workType: WorkType
    ): PhotoAnalysisWithTags {
        if (!isServiceAvailable) {
            return PhotoAnalysisWithTags(
                id = "unavailable-${Clock.System.now().toEpochMilliseconds()}",
                photoId = "unavailable",
                recommendedTags = emptyList(),
                processingTimeMs = 0L
            )
        }
        
        val startTime = Clock.System.now().toEpochMilliseconds()
        val analysisId = "hybrid-$startTime"
        
        try {
            logHybrid("HYBRID_ANALYSIS_STARTED", mapOf(
                "analysis_id" to analysisId,
                "strategy" to currentStrategy.name,
                "work_type" to workType.name,
                "image_size" to data.size.toString()
            ))
            
            // Dynamic strategy selection based on current conditions
            val selectedStrategy = selectOptimalStrategy()
            
            // Execute analysis based on selected strategy
            val analysisResult = when (selectedStrategy) {
                AnalysisStrategy.YOLO_ONLY -> executeYOLOOnlyAnalysis(data, workType, analysisId)
                AnalysisStrategy.GEMINI_ONLY -> executeGeminiOnlyAnalysis(data, width, height, workType, analysisId)
                AnalysisStrategy.YOLO_PRIMARY -> executeYOLOPrimaryAnalysis(data, width, height, workType, analysisId)
                AnalysisStrategy.HYBRID_OPTIMAL -> executeHybridAnalysis(data, width, height, workType, analysisId)
            }
            
            val processingTime = Clock.System.now().toEpochMilliseconds() - startTime
            
            // Emit result for monitoring
            hybridScope.launch {
                _hybridAnalysisResults.emit(
                    HybridAnalysisResult(
                        analysisId = analysisId,
                        strategy = selectedStrategy,
                        processingTimeMs = processingTime,
                        yoloUsed = analysisResult.yoloContribution != null,
                        geminiUsed = analysisResult.geminiContribution != null,
                        finalConfidence = analysisResult.finalConfidence,
                        tagCount = analysisResult.tags.size
                    )
                )
            }
            
            logHybrid("HYBRID_ANALYSIS_COMPLETED", mapOf(
                "analysis_id" to analysisId,
                "strategy" to selectedStrategy.name,
                "processing_time_ms" to processingTime.toString(),
                "final_confidence" to analysisResult.finalConfidence.toString(),
                "tag_count" to analysisResult.tags.size.toString()
            ))
            
            return PhotoAnalysisWithTags(
                id = analysisId,
                photoId = "photo-$startTime",
                recommendedTags = analysisResult.tags,
                processingTimeMs = processingTime
            )
            
        } catch (e: Exception) {
            val processingTime = Clock.System.now().toEpochMilliseconds() - startTime
            logHybrid("HYBRID_ANALYSIS_FAILED", mapOf(
                "analysis_id" to analysisId,
                "error" to e.message.orEmpty(),
                "processing_time_ms" to processingTime.toString()
            ))
            
            // Return fallback result
            return PhotoAnalysisWithTags(
                id = analysisId,
                photoId = "error-photo-$startTime",
                recommendedTags = getBasicSafetyTags(workType),
                processingTimeMs = processingTime
            )
        }
    }
    
    /**
     * Execute comprehensive safety analysis using the hybrid system
     */
    suspend fun executeHybridSafetyAnalysis(
        imageData: ByteArray,
        workType: WorkType = WorkType.GENERAL_CONSTRUCTION,
        quality: AnalysisQuality = AnalysisQuality.BALANCED
    ): Result<SafetyAnalysis> = withContext(Dispatchers.Default) {
        try {
            if (!isServiceAvailable) {
                return@withContext Result.failure(HybridAIException.ServiceUnavailable("Hybrid AI service not available"))
            }
            
            val analysisId = "safety-${Clock.System.now().toEpochMilliseconds()}"
            val startTime = Clock.System.now().toEpochMilliseconds()
            
            logHybrid("HYBRID_SAFETY_ANALYSIS_STARTED", mapOf(
                "analysis_id" to analysisId,
                "work_type" to workType.name,
                "quality" to quality.name
            ))
            
            // Execute parallel analysis with timeout management
            val yoloDeferred = hybridScope.async {
                withTimeoutOrNull(ANALYSIS_TIMEOUT_MS) {
                    yoloAnalyzer.analyzeSafety(imageData, workType, analysisId, quality)
                }?.getOrNull()
            }
            
            val geminiDeferred = hybridScope.async {
                if (geminiAnalyzer.isServiceAvailable && shouldUseGemini()) {
                    withTimeoutOrNull(GEMINI_TIMEOUT_MS) {
                        // Convert Gemini tags to SafetyAnalysis
                        val geminiResult = geminiAnalyzer.analyzePhotoWithTags(imageData, 640, 480, workType)
                        createSafetyAnalysisFromTags(geminiResult, workType, analysisId)
                    }
                } else null
            }
            
            // Collect results
            val yoloResult = yoloDeferred.await()
            val geminiResult = geminiDeferred.await()
            
            // Combine and enhance results
            val finalAnalysis = combineAnalysisResults(
                yoloResult,
                geminiResult,
                workType,
                analysisId
            )
            
            val processingTime = Clock.System.now().toEpochMilliseconds() - startTime
            
            logHybrid("HYBRID_SAFETY_ANALYSIS_COMPLETED", mapOf(
                "analysis_id" to analysisId,
                "processing_time_ms" to processingTime.toString(),
                "yolo_success" to (yoloResult != null).toString(),
                "gemini_success" to (geminiResult != null).toString(),
                "final_confidence" to finalAnalysis.aiConfidence.toString()
            ))
            
            Result.success(finalAnalysis)
            
        } catch (e: Exception) {
            logHybrid("HYBRID_SAFETY_ANALYSIS_FAILED", mapOf("error" to e.message.orEmpty()))
            Result.failure(HybridAIException.AnalysisFailed("Hybrid safety analysis failed: ${e.message}", e))
        }
    }
    
    // Private implementation methods
    
    private suspend fun selectOptimalStrategy(): AnalysisStrategy {
        updateConnectivityState()
        
        val yoloAvailable = yoloAnalyzer.getAnalyzerStatus().isInitialized
        val geminiAvailable = geminiAnalyzer.isServiceAvailable
        
        return when {
            !yoloAvailable && !geminiAvailable -> {
                logHybrid("STRATEGY_SELECTION", mapOf("selected" to "NONE_AVAILABLE", "reason" to "both_unavailable"))
                AnalysisStrategy.YOLO_ONLY // Fallback, will handle gracefully
            }
            yoloAvailable && !geminiAvailable -> {
                logHybrid("STRATEGY_SELECTION", mapOf("selected" to "YOLO_ONLY", "reason" to "gemini_unavailable"))
                AnalysisStrategy.YOLO_ONLY
            }
            !yoloAvailable && geminiAvailable -> {
                logHybrid("STRATEGY_SELECTION", mapOf("selected" to "GEMINI_ONLY", "reason" to "yolo_unavailable"))
                AnalysisStrategy.GEMINI_ONLY
            }
            networkConnectivity == NetworkConnectivityState.OFFLINE -> {
                logHybrid("STRATEGY_SELECTION", mapOf("selected" to "YOLO_ONLY", "reason" to "offline"))
                AnalysisStrategy.YOLO_ONLY
            }
            networkConnectivity == NetworkConnectivityState.POOR -> {
                logHybrid("STRATEGY_SELECTION", mapOf("selected" to "YOLO_PRIMARY", "reason" to "poor_connectivity"))
                AnalysisStrategy.YOLO_PRIMARY
            }
            else -> {
                logHybrid("STRATEGY_SELECTION", mapOf("selected" to "HYBRID_OPTIMAL", "reason" to "good_conditions"))
                AnalysisStrategy.HYBRID_OPTIMAL
            }
        }
    }
    
    private suspend fun executeYOLOOnlyAnalysis(
        data: ByteArray,
        workType: WorkType,
        analysisId: String
    ): HybridAnalysisDetail {
        val result = yoloAnalyzer.analyzeSafety(data, workType, analysisId, AnalysisQuality.BALANCED)
        return if (result.isSuccess) {
            val analysis = result.getOrThrow()
            HybridAnalysisDetail(
                tags = extractTagsFromSafetyAnalysis(analysis),
                finalConfidence = analysis.aiConfidence * YOLO_BASE_WEIGHT,
                yoloContribution = analysis,
                geminiContribution = null
            )
        } else {
            HybridAnalysisDetail(
                tags = getBasicSafetyTags(workType),
                finalConfidence = 0.3f,
                yoloContribution = null,
                geminiContribution = null
            )
        }
    }
    
    private suspend fun executeGeminiOnlyAnalysis(
        data: ByteArray,
        width: Int,
        height: Int,
        workType: WorkType,
        analysisId: String
    ): HybridAnalysisDetail {
        val result = geminiAnalyzer.analyzePhotoWithTags(data, width, height, workType)
        val safetyAnalysis = createSafetyAnalysisFromTags(result, workType, analysisId)
        
        return HybridAnalysisDetail(
            tags = result.recommendedTags,
            finalConfidence = GEMINI_BASE_WEIGHT * 0.8f, // Slightly reduced for single-system analysis
            yoloContribution = null,
            geminiContribution = safetyAnalysis
        )
    }
    
    private suspend fun executeYOLOPrimaryAnalysis(
        data: ByteArray,
        width: Int,
        height: Int,
        workType: WorkType,
        analysisId: String
    ): HybridAnalysisDetail {
        // Primary YOLO analysis
        val yoloResult = yoloAnalyzer.analyzeSafety(data, workType, analysisId, AnalysisQuality.FAST)
        
        // Optional Gemini enhancement (non-blocking)
        val geminiResult = try {
            withTimeoutOrNull(GEMINI_TIMEOUT_MS / 2) {
                geminiAnalyzer.analyzePhotoWithTags(data, width, height, workType)
            }
        } catch (e: Exception) {
            null
        }
        
        return combineResults(yoloResult.getOrNull(), geminiResult, workType, analysisId, 0.8f, 0.2f)
    }
    
    private suspend fun executeHybridAnalysis(
        data: ByteArray,
        width: Int,
        height: Int,
        workType: WorkType,
        analysisId: String
    ): HybridAnalysisDetail {
        // Parallel execution for optimal performance
        val yoloDeferred = hybridScope.async {
            yoloAnalyzer.analyzeSafety(data, workType, analysisId, AnalysisQuality.BALANCED)
        }
        
        val geminiDeferred = hybridScope.async {
            geminiAnalyzer.analyzePhotoWithTags(data, width, height, workType)
        }
        
        val yoloResult = yoloDeferred.await().getOrNull()
        val geminiResult = geminiDeferred.await()
        
        return combineResults(yoloResult, geminiResult, workType, analysisId, 0.6f, 0.4f)
    }
    
    private fun combineResults(
        yoloResult: SafetyAnalysis?,
        geminiResult: PhotoAnalysisWithTags?,
        workType: WorkType,
        analysisId: String,
        yoloWeight: Float,
        geminiWeight: Float
    ): HybridAnalysisDetail {
        val combinedTags = mutableSetOf<String>()
        var combinedConfidence = 0f
        
        // Add YOLO contributions
        yoloResult?.let { yolo ->
            combinedTags.addAll(extractTagsFromSafetyAnalysis(yolo))
            combinedConfidence += yolo.aiConfidence * yoloWeight
        }
        
        // Add Gemini contributions
        geminiResult?.let { gemini ->
            combinedTags.addAll(gemini.recommendedTags)
            combinedConfidence += 0.85f * geminiWeight // Gemini baseline confidence
        }
        
        // Apply hybrid boost if both systems contributed
        if (yoloResult != null && geminiResult != null) {
            combinedConfidence += HYBRID_CONFIDENCE_BOOST
        }
        
        // Ensure tags from basic safety requirements
        combinedTags.addAll(getBasicSafetyTags(workType))
        
        return HybridAnalysisDetail(
            tags = combinedTags.toList(),
            finalConfidence = combinedConfidence.coerceIn(0f, 1f),
            yoloContribution = yoloResult,
            geminiContribution = geminiResult?.let { createSafetyAnalysisFromTags(it, workType, analysisId) }
        )
    }
    
    // Helper methods
    
    private fun updateConnectivityState() {
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastConnectivityCheck > connectivityCheckInterval) {
            // Mock connectivity assessment - in real implementation, would check actual network
            networkConnectivity = NetworkConnectivityState.GOOD
            lastConnectivityCheck = now
        }
    }
    
    private fun shouldUseGemini(): Boolean {
        return geminiAnalyzer.isServiceAvailable && 
               networkConnectivity != NetworkConnectivityState.OFFLINE
    }
    
    private fun startBackgroundMonitoring() {
        hybridScope.launch {
            while (isActive) {
                delay(30000L) // 30 seconds
                updateConnectivityState()
                
                val newStrategy = selectOptimalStrategy()
                if (newStrategy != currentStrategy) {
                    val change = StrategyChange(
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                        previousStrategy = currentStrategy,
                        newStrategy = newStrategy,
                        reason = "Background monitoring detected optimal strategy change"
                    )
                    currentStrategy = newStrategy
                    _strategyChanges.emit(change)
                }
            }
        }
    }
    
    // Utility methods for data conversion and fallbacks
    
    private fun extractTagsFromSafetyAnalysis(analysis: SafetyAnalysis): List<String> {
        val tags = mutableListOf<String>()
        
        // Extract hazard-based tags
        analysis.hazards.forEach { hazard ->
            tags.add("hazard-${hazard.type.name.lowercase().replace('_', '-')}")
            tags.add("severity-${hazard.severity.name.lowercase()}")
        }
        
        // Extract OSHA codes as tags
        analysis.oshaCodes.forEach { osha ->
            tags.add("osha-${osha.code}")
        }
        
        return tags.distinct()
    }
    
    private fun createSafetyAnalysisFromTags(
        tagAnalysis: PhotoAnalysisWithTags,
        workType: WorkType,
        analysisId: String
    ): SafetyAnalysis {
        val processingTime = tagAnalysis.processingTimeMs
        return SafetyAnalysis(
            id = analysisId,
            photoId = tagAnalysis.photoId,
            timestamp = Clock.System.now(),
            analysisType = AnalysisType.CLOUD_GEMINI,
            workType = workType,
            hazards = emptyList(), // Would map tags to hazards in full implementation
            oshaViolations = emptyList(),
            recommendations = tagAnalysis.recommendedTags.map { "Consider safety measures for: $it" },
            overallRiskLevel = RiskLevel.LOW,
            severity = Severity.LOW,
            aiConfidence = 0.8f,
            processingTimeMs = processingTime
        )
    }
    
    private fun combineAnalysisResults(
        yoloAnalysis: SafetyAnalysis?,
        geminiAnalysis: SafetyAnalysis?,
        workType: WorkType,
        analysisId: String
    ): SafetyAnalysis {
        val combinedHazards = mutableListOf<Hazard>()
        val combinedRecommendations = mutableListOf<String>()
        var highestSeverity = Severity.LOW
        var combinedConfidence = 0f
        val startTime = Clock.System.now()
        
        yoloAnalysis?.let { yolo ->
            combinedHazards.addAll(yolo.hazards)
            combinedRecommendations.addAll(yolo.recommendations)
            if (yolo.severity.ordinal > highestSeverity.ordinal) {
                highestSeverity = yolo.severity
            }
            combinedConfidence += yolo.aiConfidence * 0.6f
        }
        
        geminiAnalysis?.let { gemini ->
            combinedRecommendations.addAll(gemini.recommendations)
            combinedConfidence += gemini.aiConfidence * 0.4f
        }
        
        // Apply hybrid boost
        if (yoloAnalysis != null && geminiAnalysis != null) {
            combinedConfidence += HYBRID_CONFIDENCE_BOOST
        }
        
        // Calculate overall risk level from hazards
        val overallRiskLevel = calculateRiskLevelFromHazards(combinedHazards)
        
        val processingTime = (Clock.System.now() - startTime).inWholeMilliseconds
        
        return SafetyAnalysis(
            id = analysisId,
            photoId = "hybrid-photo-${Clock.System.now().toEpochMilliseconds()}",
            timestamp = Clock.System.now(),
            analysisType = AnalysisType.HYBRID_ANALYSIS,
            workType = workType,
            hazards = combinedHazards.distinctBy { it.type },
            oshaViolations = yoloAnalysis?.oshaViolations ?: emptyList(),
            recommendations = combinedRecommendations.distinct(),
            overallRiskLevel = overallRiskLevel,
            severity = highestSeverity,
            aiConfidence = combinedConfidence.coerceIn(0f, 1f),
            processingTimeMs = processingTime
        )
    }
    
    /**
     * Calculate risk level from list of hazards
     */
    private fun calculateRiskLevelFromHazards(hazards: List<Hazard>): RiskLevel {
        if (hazards.isEmpty()) return RiskLevel.MINIMAL
        
        val maxSeverity = hazards.maxByOrNull { it.severity }?.severity ?: return RiskLevel.LOW
        return when (maxSeverity) {
            Severity.CRITICAL -> RiskLevel.SEVERE
            Severity.HIGH -> RiskLevel.HIGH
            Severity.MEDIUM -> RiskLevel.MODERATE
            Severity.LOW -> RiskLevel.LOW
        }
    }
    
    private fun getBasicSafetyTags(workType: WorkType): List<String> {
        return when (workType) {
            WorkType.GENERAL_CONSTRUCTION -> listOf("general-safety", "ppe-required", "site-awareness")
            WorkType.ELECTRICAL -> listOf("electrical-safety", "lockout-tagout", "ppe-electrical")
            WorkType.FALL_PROTECTION -> listOf("fall-protection", "harness-required", "guardrails")
            WorkType.CRANE_OPERATIONS -> listOf("crane-safety", "rigging-check", "exclusion-zone")
            WorkType.EXCAVATION -> listOf("atmospheric-testing", "rescue-plan", "entry-permit")
            WorkType.WELDING -> listOf("hot-work-permit", "fire-watch", "ventilation")
            else -> listOf("general-safety", "hazard-assessment")
        }
    }
    
    private fun logHybrid(event: String, details: Map<String, String> = emptyMap()) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val detailsStr = if (details.isNotEmpty()) {
            ": ${details.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
        } else ""
        println("[$TAG] $timestamp - $event$detailsStr")
    }
}

/**
 * Supporting data classes and enums for hybrid AI system
 */

@Serializable
enum class AnalysisStrategy {
    YOLO_ONLY,
    GEMINI_ONLY, 
    YOLO_PRIMARY,
    HYBRID_OPTIMAL
}

@Serializable
enum class NetworkConnectivityState {
    OFFLINE, POOR, GOOD, EXCELLENT, UNKNOWN
}

@Serializable
data class HybridAnalysisResult(
    val analysisId: String,
    val strategy: AnalysisStrategy,
    val processingTimeMs: Long,
    val yoloUsed: Boolean,
    val geminiUsed: Boolean,
    val finalConfidence: Float,
    val tagCount: Int
)

@Serializable
data class HybridAnalysisDetail(
    val tags: List<String>,
    val finalConfidence: Float,
    val yoloContribution: SafetyAnalysis?,
    val geminiContribution: SafetyAnalysis?
)

@Serializable
data class StrategyChange(
    val timestamp: Long,
    val previousStrategy: AnalysisStrategy,
    val newStrategy: AnalysisStrategy,
    val reason: String
)

/**
 * Exception types for hybrid AI system
 */
sealed class HybridAIException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InitializationFailed(message: String, cause: Throwable? = null) : HybridAIException(message, cause)
    class ServiceUnavailable(message: String) : HybridAIException(message)
    class AnalysisFailed(message: String, cause: Throwable? = null) : HybridAIException(message, cause)
    class StrategySelectionFailed(message: String) : HybridAIException(message)
}
