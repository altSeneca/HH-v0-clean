package com.hazardhawk.ai.core
import kotlinx.datetime.Clock

import com.hazardhawk.core.models.SafetyAnalysis
import com.hazardhawk.core.models.AnalysisType
import com.hazardhawk.core.models.AnalysisCapability
import com.hazardhawk.core.models.WorkType
import com.hazardhawk.ai.services.Gemma3NE2BVisionService
import com.hazardhawk.ai.services.VertexAIGeminiService
import com.hazardhawk.ai.services.YOLO11LocalService
import com.hazardhawk.performance.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Smart orchestrator that manages multiple AI services with intelligent fallback logic.
 * Prioritizes local processing for privacy while maintaining reliability.
 * 
 * Priority Order:
 * 1. Gemma 3N E2B (Local multimodal) - Privacy-first, comprehensive analysis
 * 2. Vertex AI Gemini (Cloud) - Enhanced analysis when network available  
 * 3. YOLO11 (Local fallback) - Basic hazard detection only
 */
class SmartAIOrchestrator(
    private val gemma3NE2B: Gemma3NE2BVisionService,
    private val vertexAI: VertexAIGeminiService, 
    private val yolo11: YOLO11LocalService,
    private val networkMonitor: NetworkConnectivityService,
    private val performanceManager: AdaptivePerformanceManager,
    private val memoryManager: MemoryManager,
    private val performanceMonitor: PerformanceMonitor
) : AIPhotoAnalyzer {
    
    override val analyzerName = "Smart AI Orchestrator"
    override val priority = 200 // Highest priority as the main entry point
    
    override val analysisCapabilities = setOf(
        AnalysisCapability.MULTIMODAL_VISION,
        AnalysisCapability.PPE_DETECTION,
        AnalysisCapability.HAZARD_IDENTIFICATION, 
        AnalysisCapability.OSHA_COMPLIANCE,
        AnalysisCapability.OFFLINE_ANALYSIS,
        AnalysisCapability.REAL_TIME_PROCESSING,
        AnalysisCapability.DOCUMENT_GENERATION
    )
    
    override val isAvailable: Boolean
        get() = gemma3NE2B.isAvailable || vertexAI.isAvailable || yolo11.isAvailable
    
    private var orchestratorStats = OrchestratorStats()
    private var lastAnalysisTime = 0L
    private val aiFrameLimiter = AIFrameLimiter(2.0f) // 2 FPS AI processing
    
    override suspend fun configure(apiKey: String?): Result<Unit> {
        return try {
            // Configure all available services
            val results = listOf(
                gemma3NE2B.configure(),
                vertexAI.configure(apiKey),
                yolo11.configure()
            )
            
            val hasAnySuccess = results.any { it.isSuccess }
            
            if (hasAnySuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("No AI services could be configured"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Orchestrator configuration failed: ${e.message}", e))
        }
    }
    
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType
    ): Result<SafetyAnalysis> {
        
        val startTime = Clock.System.now().toEpochMilliseconds()
        var lastError: Exception? = null
        
        // Apply AI processing throttling (2 FPS)
        if (!aiFrameLimiter.shouldProcess()) {
            val waitTime = aiFrameLimiter.getTimeUntilNextProcessMs()
            if (waitTime > 0) {
                delay(waitTime)
            }
        }
        
        // Check for cached result first
        val cacheKey = memoryManager.generateCacheKey(imageData, workType.toString())
        memoryManager.getCachedAnalysisResult(cacheKey)?.let { cachedResult ->
            performanceMonitor.recordAIAnalysis(0L, true, cacheHit = true)
            return Result.success(cachedResult as SafetyAnalysis)
        }
        
        // Get current performance configuration
        val performanceConfig = performanceManager.getCurrentConfig()
        
        // Determine best analysis strategy based on device capabilities and performance
        val shouldUseAdvanced = performanceConfig.deviceTier != DeviceTier.LOW_END && 
                               !performanceConfig.useLowPowerMode
        
        // Strategy 1: Gemma 3N E2B (Local multimodal - PRIORITY)
        if (gemma3NE2B.isAvailable && shouldUseAdvanced) {
            try {
                // Dynamic timeout based on device tier
                val timeout = when (performanceConfig.deviceTier) {
                    DeviceTier.LOW_END -> 15000L
                    DeviceTier.MID_RANGE -> 10000L
                    DeviceTier.HIGH_END -> 8000L
                }
                
                val result = withTimeoutOrNull(timeout) {
                    gemma3NE2B.analyzePhoto(imageData, workType)
                }
                
                if (result?.isSuccess == true) {
                    val analysisTime = Clock.System.now().toEpochMilliseconds() - startTime
                    orchestratorStats.recordSuccess(AnalysisType.LOCAL_GEMMA_MULTIMODAL)
                    performanceMonitor.recordAIAnalysis(analysisTime, true)
                    
                    // Cache the result
                    result.getOrNull()?.let { analysis ->
                        memoryManager.cacheAnalysisResult(cacheKey, analysis)
                    }
                    
                    return result.map { analysis ->
                        analysis.copy(
                            processingTimeMs = analysisTime
                        )
                    }
                } else {
                    val analysisTime = Clock.System.now().toEpochMilliseconds() - startTime
                    lastError = result?.exceptionOrNull() as? Exception 
                        ?: Exception("Gemma analysis timeout")
                    orchestratorStats.recordFailure(AnalysisType.LOCAL_GEMMA_MULTIMODAL)
                    performanceMonitor.recordAIAnalysis(analysisTime, false)
                }
            } catch (e: Exception) {
                val analysisTime = Clock.System.now().toEpochMilliseconds() - startTime
                lastError = e
                orchestratorStats.recordFailure(AnalysisType.LOCAL_GEMMA_MULTIMODAL)
                performanceMonitor.recordAIAnalysis(analysisTime, false)
            }
        }
        
        // Strategy 2: Vertex AI Gemini (Cloud fallback)
        if (networkMonitor.isConnected && vertexAI.isAvailable) {
            try {
                val result = withTimeoutOrNull(15000) { // 15 second timeout for cloud
                    vertexAI.analyzePhoto(imageData, workType)
                }
                
                if (result?.isSuccess == true) {
                    orchestratorStats.recordSuccess(AnalysisType.CLOUD_GEMINI)
                    return result.map { analysis ->
                        analysis.copy(
                            analysisType = AnalysisType.CLOUD_GEMINI,
                            recommendations = analysis.recommendations + 
                                "Analysis completed via cloud service (Gemma local unavailable)"
                        )
                    }
                } else {
                    lastError = result?.exceptionOrNull() as? Exception 
                        ?: Exception("Vertex AI analysis timeout") 
                    orchestratorStats.recordFailure(AnalysisType.CLOUD_GEMINI)
                }
            } catch (e: Exception) {
                lastError = e
                orchestratorStats.recordFailure(AnalysisType.CLOUD_GEMINI)
            }
        }
        
        // Strategy 3: YOLO11 (Local fallback - basic detection only)
        if (yolo11.isAvailable) {
            try {
                val result = withTimeoutOrNull(8000) { // 8 second timeout for local YOLO
                    yolo11.analyzePhoto(imageData, workType)
                }
                
                if (result?.isSuccess == true) {
                    orchestratorStats.recordSuccess(AnalysisType.LOCAL_YOLO_FALLBACK)
                    return result.map { analysis ->
                        analysis.copy(
                            analysisType = AnalysisType.LOCAL_YOLO_FALLBACK,
                            recommendations = analysis.recommendations + listOf(
                                "⚠️ Limited analysis - advanced AI services unavailable",
                                "Manual safety review recommended for comprehensive assessment"
                            ),
                            aiConfidence = analysis.aiConfidence * 0.7f // Reduce confidence for basic fallback
                        )
                    }
                } else {
                    lastError = result?.exceptionOrNull() as? Exception 
                        ?: Exception("YOLO analysis timeout")
                    orchestratorStats.recordFailure(AnalysisType.LOCAL_YOLO_FALLBACK)
                }
            } catch (e: Exception) {
                lastError = e
                orchestratorStats.recordFailure(AnalysisType.LOCAL_YOLO_FALLBACK)
            }
        }
        
        // All strategies failed - return error with diagnostic info
        val errorMessage = buildString {
            append("All AI analysis methods failed. ")
            if (!gemma3NE2B.isAvailable) append("Gemma unavailable. ")
            if (!networkMonitor.isConnected) append("Network offline. ")
            if (!vertexAI.isAvailable) append("Vertex AI unavailable. ")
            if (!yolo11.isAvailable) append("YOLO fallback unavailable. ")
            append("Last error: ${lastError?.message}")
        }
        
        orchestratorStats.recordTotalFailure()
        
        return Result.failure(Exception(errorMessage, lastError))
    }
    
    /**
     * Analyze multiple photos concurrently for batch processing.
     */
    suspend fun analyzeBatch(
        images: List<Pair<ByteArray, WorkType>>,
        maxConcurrency: Int = 3
    ): List<Result<SafetyAnalysis>> = coroutineScope {
        
        images.chunked(maxConcurrency).flatMap { batch ->
            batch.map { (imageData, workType) ->
                async {
                    analyzePhoto(imageData, workType)
                }
            }.map { it.await() }
        }
    }
    
    /**
     * Get performance statistics for monitoring and optimization.
     */
    fun getStats(): OrchestratorStats = orchestratorStats.copy()
    
    /**
     * Reset performance statistics.
     */
    fun resetStats() {
        orchestratorStats = OrchestratorStats()
    }
    
    /**
     * Get the best available analyzer for current conditions.
     */
    fun getBestAnalyzer(): AIPhotoAnalyzer {
        return when {
            gemma3NE2B.isAvailable -> gemma3NE2B
            networkMonitor.isConnected && vertexAI.isAvailable -> vertexAI
            yolo11.isAvailable -> yolo11
            else -> throw IllegalStateException("No AI analyzers available")
        }
    }
    
    /**
     * Test connectivity and performance of all analyzers.
     */
    suspend fun performHealthCheck(): HealthCheckResult {
        val results = mutableMapOf<String, Boolean>()
        val timings = mutableMapOf<String, Long>()
        
        // Test Gemma 3N E2B
        val gemmaStart = Clock.System.now().toEpochMilliseconds()
        try {
            results["Gemma3NE2B"] = gemma3NE2B.isAvailable
            timings["Gemma3NE2B"] = Clock.System.now().toEpochMilliseconds() - gemmaStart
        } catch (e: Exception) {
            results["Gemma3NE2B"] = false
        }
        
        // Test Vertex AI
        val vertexStart = Clock.System.now().toEpochMilliseconds() 
        try {
            results["VertexAI"] = networkMonitor.isConnected && vertexAI.isAvailable
            timings["VertexAI"] = Clock.System.now().toEpochMilliseconds() - vertexStart
        } catch (e: Exception) {
            results["VertexAI"] = false
        }
        
        // Test YOLO11
        val yoloStart = Clock.System.now().toEpochMilliseconds()
        try {
            results["YOLO11"] = yolo11.isAvailable  
            timings["YOLO11"] = Clock.System.now().toEpochMilliseconds() - yoloStart
        } catch (e: Exception) {
            results["YOLO11"] = false
        }
        
        return HealthCheckResult(
            analyzersAvailable = results,
            responseTimes = timings,
            networkConnected = networkMonitor.isConnected,
            overallHealth = results.values.any { it }
        )
    }
}

/**
 * Network connectivity monitoring interface.
 */
interface NetworkConnectivityService {
    val isConnected: Boolean
    val connectionQuality: ConnectionQuality
}

enum class ConnectionQuality {
    POOR,
    FAIR, 
    GOOD,
    EXCELLENT
}

/**
 * Performance tracking for the orchestrator.
 */
data class OrchestratorStats(
    private val successCounts: MutableMap<AnalysisType, Int> = mutableMapOf(),
    private val failureCounts: MutableMap<AnalysisType, Int> = mutableMapOf(),
    private val totalAnalyses: Int = 0,
    private val totalFailures: Int = 0
) {
    fun recordSuccess(type: AnalysisType) {
        successCounts[type] = successCounts.getOrDefault(type, 0) + 1
    }
    
    fun recordFailure(type: AnalysisType) {
        failureCounts[type] = failureCounts.getOrDefault(type, 0) + 1
    }
    
    fun recordTotalFailure() {
        // Recorded when all strategies fail
    }
    
    val successRate: Float
        get() = if (totalAnalyses > 0) {
            (totalAnalyses - totalFailures).toFloat() / totalAnalyses
        } else 0f
        
    val preferredAnalyzer: AnalysisType?
        get() = successCounts.maxByOrNull { it.value }?.key
}

/**
 * Health check results for monitoring.
 */
data class HealthCheckResult(
    val analyzersAvailable: Map<String, Boolean>,
    val responseTimes: Map<String, Long>,
    val networkConnected: Boolean,
    val overallHealth: Boolean
)

/**
 * AI processing frame rate limiter to maintain 2 FPS processing rate.
 */
class AIFrameLimiter(private val targetFPS: Float) {
    private val frameIntervalMs = (1000f / targetFPS).toLong()
    private var lastProcessTime = 0L
    
    fun shouldProcess(): Boolean {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val elapsed = currentTime - lastProcessTime
        
        return if (elapsed >= frameIntervalMs) {
            lastProcessTime = currentTime
            true
        } else {
            false
        }
    }
    
    fun getTimeUntilNextProcessMs(): Long {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val elapsed = currentTime - lastProcessTime
        val remaining = frameIntervalMs - elapsed
        return remaining.coerceAtLeast(0)
    }
}