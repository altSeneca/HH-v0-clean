package com.hazardhawk.ai.services
import kotlinx.datetime.Clock

import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.ai.models.SafetyAnalysis
import com.hazardhawk.ai.litert.LiteRTModelEngine
import com.hazardhawk.ai.litert.LiteRTDeviceOptimizer
import com.hazardhawk.ai.litert.LiteRTBackend
import com.hazardhawk.ai.litert.LiteRTException
import com.hazardhawk.core.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.uuid.uuid4

/**
 * Google LiteRT-LM powered vision service for real construction safety analysis.
 * Replaces mock Gemma3NE2BVisionService with genuine on-device AI processing.
 * 
 * Key Features:
 * - Real hazard detection vs mock JSON generation
 * - GPU/NPU acceleration (3-8x performance boost)
 * - Maintains existing SafetyAnalysis contracts
 * - OSHA-compliant construction focus
 * - Zero breaking changes to UI integration
 * 
 * Performance Targets:
 * - NPU: <0.8s analysis time (8x improvement)
 * - GPU: <1.5s analysis time (3x improvement) 
 * - CPU: <3.0s analysis time (baseline)
 */
class LiteRTVisionService(
    private val modelEngine: LiteRTModelEngine,
    private val deviceOptimizer: LiteRTDeviceOptimizer
) : AIPhotoAnalyzer {
    
    override val analyzerName = "LiteRT Construction Vision"
    override val priority = 150 // Same as previous Gemma service for drop-in replacement
    
    override val analysisCapabilities = setOf(
        AnalysisCapability.MULTIMODAL_VISION,
        AnalysisCapability.PPE_DETECTION,
        AnalysisCapability.HAZARD_IDENTIFICATION,
        AnalysisCapability.OSHA_COMPLIANCE,
        AnalysisCapability.OFFLINE_ANALYSIS,
        AnalysisCapability.REAL_TIME_PROCESSING,
        AnalysisCapability.DOCUMENT_GENERATION
    )
    
    private var isInitialized = false
    private var currentBackend: LiteRTBackend? = null
    private var initializationError: Exception? = null
    
    override val isAvailable: Boolean
        get() = isInitialized && modelEngine.isAvailable && initializationError == null
    
    override suspend fun configure(apiKey: String?): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // Select optimal backend for device
            val optimalBackend = deviceOptimizer.selectOptimalBackend()
            
            // Initialize LiteRT engine with construction safety model
            val modelPath = selectModelForDevice(optimalBackend)
            val initResult = modelEngine.initialize(modelPath, optimalBackend)
            
            if (initResult.isSuccess) {
                isInitialized = true
                currentBackend = optimalBackend
                initializationError = null
                
                Result.success(Unit)
            } else {
                val error = initResult.exceptionOrNull() ?: Exception("Unknown initialization error")
                initializationError = error
                
                // Attempt CPU fallback if optimal backend failed
                if (optimalBackend != LiteRTBackend.CPU) {
                    val fallbackResult = modelEngine.initialize(modelPath, LiteRTBackend.CPU)
                    if (fallbackResult.isSuccess) {
                        isInitialized = true
                        currentBackend = LiteRTBackend.CPU
                        initializationError = null
                        return@withContext Result.success(Unit)
                    }
                }
                
                Result.failure(Exception("LiteRT initialization failed: ${error.message}", error))
            }
        } catch (e: Exception) {
            initializationError = e
            Result.failure(Exception("LiteRT configuration failed: ${e.message}", e))
        }
    }
    
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType,
        progressCallback: ((String, Float) -> Unit)? = null
    ): Result<SafetyAnalysis> = withContext(Dispatchers.Default) {
        
        if (!isAvailable) {
            return@withContext Result.failure(
                Exception("LiteRT service not available: ${initializationError?.message ?: "Not initialized"}")
            )
        }
        
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        try {
            // Get timeout based on current backend performance characteristics
            val timeout = getTimeoutForBackend(currentBackend ?: LiteRTBackend.CPU)
            
            val analysisResult = withTimeoutOrNull(timeout) {
                performLiteRTAnalysis(imageData, workType, startTime, progressCallback)
            }
            
            if (analysisResult == null) {
                return@withContext Result.failure(
                    Exception("LiteRT analysis timeout after ${timeout}ms on ${currentBackend}")
                )
            }
            
            analysisResult
            
        } catch (e: LiteRTException.ThermalThrottlingException) {
            // Handle thermal throttling gracefully
            Result.failure(Exception(
                "Device too hot for AI processing. Please allow cooling before retrying.", 
                e
            ))
        } catch (e: LiteRTException.OutOfMemoryException) {
            // Handle memory issues
            Result.failure(Exception(
                "Insufficient device memory for AI analysis. Close other apps and retry.", 
                e
            ))
        } catch (e: Exception) {
            Result.failure(Exception("LiteRT analysis failed: ${e.message}", e))
        }
    }
    
    /**
     * Perform the actual LiteRT model inference and convert to SafetyAnalysis.
     */
    private suspend fun performLiteRTAnalysis(
        imageData: ByteArray,
        workType: WorkType,
        startTime: Long,
        progressCallback: ((String, Float) -> Unit)? = null
    ): Result<SafetyAnalysis> {
        
        // Execute real AI inference with progress tracking
        val liteRTResult = modelEngine.generateSafetyAnalysis(
            imageData = imageData,
            workType = workType,
            includeOSHACodes = true,
            confidenceThreshold = 0.6f,
            progressCallback = { progress ->
                // Convert LiteRT progress to simple callback format
                progressCallback?.invoke(progress.message, progress.progress)
            }
        )
        
        return liteRTResult.fold(
            onSuccess = { result ->
                val analysisTime = Clock.System.now().toEpochMilliseconds() - startTime
                
                // Convert LiteRT results to HazardHawk SafetyAnalysis format
                val safetyAnalysis = convertToSafetyAnalysis(
                    liteRTResult = result,
                    workType = workType,
                    processingTime = analysisTime
                )
                
                Result.success(safetyAnalysis)
            },
            onFailure = { exception ->
                Result.failure(Exception("LiteRT inference failed: ${exception.message}", exception))
            }
        )
    }
    
    /**
     * Convert LiteRT analysis result to HazardHawk SafetyAnalysis format.
     * Maintains full compatibility with existing UI and report generation.
     */
    private fun convertToSafetyAnalysis(
        liteRTResult: com.hazardhawk.ai.litert.LiteRTAnalysisResult,
        workType: WorkType,
        processingTime: Long
    ): SafetyAnalysis {
        
        // Convert hazards from LiteRT format
        val hazards = liteRTResult.hazards.map { detectedHazard ->
            Hazard(
                id = uuid4().toString(),
                type = detectedHazard.type,
                severity = detectedHazard.severity,
                description = detectedHazard.description,
                location = detectedHazard.boundingBox?.let { box ->
                    HazardLocation(
                        x = box.x,
                        y = box.y,
                        width = box.width,
                        height = box.height
                    )
                },
                oshaReference = detectedHazard.oshaCode,
                recommendations = detectedHazard.recommendations,
                confidence = detectedHazard.confidence
            )
        }
        
        // Convert OSHA violations
        val oshaViolations = liteRTResult.oshaViolations.map { violation ->
            OSHAViolation(
                code = violation.code,
                description = violation.description,
                severity = violation.severity,
                citation = violation.citation
            )
        }
        
        // Generate comprehensive recommendations based on real analysis
        val recommendations = generateConstructionRecommendations(
            hazards = hazards,
            ppeStatus = liteRTResult.ppeStatus,
            workType = workType,
            riskAssessment = liteRTResult.overallRiskAssessment
        )
        
        // Create metadata with LiteRT processing information
        val metadata = SafetyAnalysisMetadata(
            analysisId = uuid4().toString(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            processingTimeMs = processingTime,
            modelVersion = "LiteRT-Construction-Safety-v1.0",
            confidenceScore = liteRTResult.confidence,
            analysisType = AnalysisType.LOCAL_LITERT_VISION,
            deviceInfo = mapOf(
                "backend" to (liteRTResult.backendUsed.displayName),
                "tokens_per_second" to liteRTResult.backendUsed.expectedTokensPerSecond.toString(),
                "memory_usage_mb" to modelEngine.getPerformanceMetrics().averageMemoryUsageMB.toString()
            )
        )
        
        return SafetyAnalysis(
            id = metadata.analysisId,
            hazards = hazards,
            overallRiskLevel = mapRiskAssessment(liteRTResult.overallRiskAssessment),
            recommendations = recommendations,
            ppeCompliance = calculatePPECompliance(liteRTResult.ppeStatus),
            oshaViolations = oshaViolations,
            confidence = liteRTResult.confidence,
            analysisType = AnalysisType.LOCAL_LITERT_VISION,
            workType = workType,
            metadata = metadata
        )
    }
    
    /**
     * Generate contextual safety recommendations based on real AI analysis.
     */
    private fun generateConstructionRecommendations(
        hazards: List<Hazard>,
        ppeStatus: Map<PPEType, PPEDetection>,
        workType: WorkType,
        riskAssessment: RiskAssessment
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Real hazard-based recommendations (not mock data)
        hazards.groupBy { it.type }.forEach { (hazardType, typeHazards) ->
            when (hazardType) {
                HazardType.FALL -> {
                    recommendations.add("⚠️ Fall protection required: Install guardrails or safety nets")
                    recommendations.add("✓ Ensure all workers use safety harnesses when working above 6 feet")
                }
                HazardType.ELECTRICAL -> {
                    recommendations.add("⚡ Electrical hazard identified: Implement lockout/tagout procedures")
                    recommendations.add("✓ Verify all electrical work meets OSHA 1926.405 requirements")
                }
                HazardType.STRUCK_BY -> {
                    recommendations.add("🚧 Struck-by hazard: Establish safe work zones around heavy equipment")
                    recommendations.add("✓ Ensure all workers wear high-visibility safety vests")
                }
                HazardType.CAUGHT_IN -> {
                    recommendations.add("⚙️ Caught-in hazard: Install machine guards and safety devices")
                    recommendations.add("✓ Provide confined space training if applicable")
                }
                else -> {
                    recommendations.add("🔍 ${hazardType} identified: Follow work-specific safety protocols")
                }
            }
        }
        
        // PPE-based recommendations
        ppeStatus.forEach { (ppeType, detection) ->
            if (detection.isRequired && !detection.isPresent) {
                recommendations.add("🦺 Missing ${ppeType.displayName}: Required for this work type")
            }
        }
        
        // Work type specific recommendations
        when (workType) {
            WorkType.HIGH_RISE_CONSTRUCTION -> {
                recommendations.add("🏗️ High-rise work: Implement comprehensive fall protection plan")
            }
            WorkType.EXCAVATION -> {
                recommendations.add("⛏️ Excavation work: Ensure proper soil classification and shoring")
            }
            WorkType.ROOFING -> {
                recommendations.add("🏠 Roofing work: Use warning lines and safety monitor systems")
            }
            else -> {
                recommendations.add("📋 Follow standard safety protocols for ${workType.displayName}")
            }
        }
        
        // Risk-based recommendations
        when (riskAssessment.overallLevel) {
            RiskLevel.CRITICAL -> {
                recommendations.add("🚨 CRITICAL RISK: Stop work immediately and address all hazards")
            }
            RiskLevel.HIGH -> {
                recommendations.add("⚠️ HIGH RISK: Implement additional safety measures before proceeding")
            }
            RiskLevel.MEDIUM -> {
                recommendations.add("⚡ MEDIUM RISK: Monitor conditions and maintain safety protocols")
            }
            RiskLevel.LOW -> {
                recommendations.add("✅ LOW RISK: Continue with standard safety precautions")
            }
        }
        
        return recommendations.take(8) // Limit to 8 most important recommendations
    }
    
    /**
     * Select appropriate model size based on device backend capabilities.
     */
    private fun selectModelForDevice(backend: LiteRTBackend): String {
        return when (backend) {
            LiteRTBackend.NPU_NNAPI, LiteRTBackend.NPU_QTI_HTP -> {
                // Full model for NPU - can handle larger models efficiently
                "/models/litert/construction_safety_full.litertmlm"
            }
            LiteRTBackend.GPU_OPENCL, LiteRTBackend.GPU_OPENGL -> {
                // Optimized model for GPU processing
                "/models/litert/construction_safety_gpu.litertmlm"
            }
            else -> {
                // Lightweight model for CPU fallback
                "/models/litert/construction_safety_lite.litertmlm"
            }
        }
    }
    
    /**
     * Get analysis timeout based on backend performance characteristics.
     */
    private fun getTimeoutForBackend(backend: LiteRTBackend): Long {
        return when (backend) {
            LiteRTBackend.NPU_NNAPI, LiteRTBackend.NPU_QTI_HTP -> 3000L // 3 seconds for NPU
            LiteRTBackend.GPU_OPENCL, LiteRTBackend.GPU_OPENGL -> 5000L // 5 seconds for GPU  
            else -> 8000L // 8 seconds for CPU
        }
    }
    
    /**
     * Map LiteRT risk assessment to HazardHawk risk level.
     */
    private fun mapRiskAssessment(riskAssessment: RiskAssessment): RiskLevel {
        return when (riskAssessment.overallLevel) {
            RiskLevel.CRITICAL -> RiskLevel.CRITICAL
            RiskLevel.HIGH -> RiskLevel.HIGH
            RiskLevel.MEDIUM -> RiskLevel.MEDIUM
            RiskLevel.LOW -> RiskLevel.LOW
        }
    }
    
    /**
     * Calculate overall PPE compliance score.
     */
    private fun calculatePPECompliance(ppeStatus: Map<PPEType, PPEDetection>): PPEComplianceScore {
        val requiredPPE = ppeStatus.filter { it.value.isRequired }
        val compliantPPE = requiredPPE.filter { it.value.isPresent }
        
        val compliancePercentage = if (requiredPPE.isNotEmpty()) {
            (compliantPPE.size.toFloat() / requiredPPE.size.toFloat()) * 100f
        } else {
            100f
        }
        
        return PPEComplianceScore(
            overallScore = compliancePercentage,
            requiredItems = requiredPPE.keys.toList(),
            compliantItems = compliantPPE.keys.toList(),
            missingItems = requiredPPE.keys.filter { !ppeStatus[it]?.isPresent ?: false }
        )
    }
    
    /**
     * Get current performance metrics from LiteRT engine.
     */
    fun getPerformanceMetrics() = modelEngine.getPerformanceMetrics()
    
    /**
     * Get device optimization recommendations.
     */
    fun getDeviceRecommendations() = deviceOptimizer.getPerformanceRecommendations()
    
    /**
     * Cleanup LiteRT resources.
     */
    fun cleanup() {
        modelEngine.cleanup()
        isInitialized = false
        currentBackend = null
    }
}

/**
 * PPE compliance scoring for construction safety.
 */
data class PPEComplianceScore(
    val overallScore: Float,
    val requiredItems: List<PPEType>,
    val compliantItems: List<PPEType>, 
    val missingItems: List<PPEType>
)