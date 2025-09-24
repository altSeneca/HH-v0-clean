package com.hazardhawk.ai

import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration.Companion.seconds

/**
 * Enhanced AI Service Facade with Gemma 3N E2B integration and comprehensive error handling.
 * 
 * Provides intelligent fallback mechanisms between multimodal Gemma analysis,
 * YOLO object detection, and rule-based safety recommendations.
 */
class EnhancedAIServiceFacade(
    private val gemmaAnalyzer: GemmaVisionAnalyzer?,
    private val yoloDetector: YOLOHazardDetector,
    private val tagMapper: HazardTagMapper,
    private val errorHandler: AIErrorHandler = AIErrorHandler()
) : AIFallbackProvider {
    
    private var isGemmaAvailable = false
    private var isYoloAvailable = false
    
    suspend fun initialize(): Result<Unit> {
        return try {
            // Initialize YOLO detector
            isYoloAvailable = yoloDetector.initialize(
                modelPath = "hazard_detection_model.tflite",
                confidenceThreshold = 0.5f,
                nmsThreshold = 0.4f
            )
            
            // Initialize Gemma analyzer if available
            isGemmaAvailable = gemmaAnalyzer?.initialize(
                modelPath = "models",
                confidenceThreshold = 0.6f
            ) ?: false
            
            when {
                isGemmaAvailable -> android.util.Log.i("EnhancedAIFacade", "Multimodal AI with Gemma initialized")
                isYoloAvailable -> android.util.Log.i("EnhancedAIFacade", "YOLO hazard detection initialized")
                else -> android.util.Log.w("EnhancedAIFacade", "No AI models available, using fallback")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Perform comprehensive construction safety analysis with intelligent fallbacks.
     */
    suspend fun analyzeConstructionSafety(
        imageData: ByteArray,
        width: Int,
        height: Int,
        workType: WorkType = WorkType.GENERAL_CONSTRUCTION
    ): PhotoAnalysisWithTags = withContext(Dispatchers.Default) {
        
        val context = AIAnalysisContext(
            workType = workType,
            imageWidth = width,
            imageHeight = height,
            imageSizeBytes = imageData.size,
            hasNetworkAccess = true,
            deviceMemoryMB = getAvailableMemoryMB(),
            preferredModel = if (isGemmaAvailable) AIModelType.GEMMA_MULTIMODAL else AIModelType.YOLO_HAZARD_DETECTION
        )
        
        try {
            // Primary: Try multimodal Gemma analysis
            if (isGemmaAvailable && gemmaAnalyzer != null) {
                try {
                    val gemmaResult = analyzeWithGemma(
                        imageData, width, height, workType, context
                    )
                    return@withContext convertGemmaToPhotoAnalysis(gemmaResult, workType)
                } catch (e: Exception) {
                    android.util.Log.w("EnhancedAIFacade", "Gemma analysis failed, falling back to YOLO: ${e.message}")
                    // Continue to YOLO fallback
                }
            }
            
            // Secondary: YOLO hazard detection
            if (isYoloAvailable) {
                try {
                    val yoloResult = analyzeWithYOLO(imageData, width, height, workType)
                    return@withContext yoloResult
                } catch (e: Exception) {
                    android.util.Log.w("EnhancedAIFacade", "YOLO analysis failed, using error handler: ${e.message}")
                    // Continue to error handler
                }
            }
            
            // Tertiary: Rule-based fallback
            val errorResult = errorHandler.handleAnalysisError(
                error = IllegalStateException("No AI models available"),
                context = context,
                fallbackProvider = this@EnhancedAIServiceFacade
            )
            
            convertErrorResultToPhotoAnalysis(errorResult, workType)
            
        } catch (e: Exception) {
            // Final fallback
            val errorResult = errorHandler.handleAnalysisError(
                error = e,
                context = context,
                fallbackProvider = this@EnhancedAIServiceFacade
            )
            
            convertErrorResultToPhotoAnalysis(errorResult, workType)
        }
    }
    
    /**
     * Analyze using Gemma 3N E2B multimodal AI.
     */
    private suspend fun analyzeWithGemma(
        imageData: ByteArray,
        width: Int,
        height: Int,
        workType: WorkType,
        context: AIAnalysisContext
    ): SafetyAnalysisResult {
        return withTimeout(context.analysisTimeout) {
            gemmaAnalyzer!!.analyzeConstructionSafety(
                imageData = imageData,
                width = width,
                height = height,
                analysisPrompt = getWorkTypeSpecificPrompt(workType)
            )
        }
    }
    
    /**
     * Analyze using YOLO hazard detection.
     */
    private suspend fun analyzeWithYOLO(
        imageData: ByteArray,
        width: Int,
        height: Int,
        workType: WorkType
    ): PhotoAnalysisWithTags {
        val detections = yoloDetector.detectHazards(imageData, width, height)
        val mappingResult = tagMapper.mapToTags(detections, workType)
        
        return PhotoAnalysisWithTags(
            detections = detections,
            recommendedTags = mappingResult.recommendations,
            autoSelectTags = mappingResult.autoSelections,
            complianceOverview = mappingResult.complianceOverview,
            processingTimeMs = 0L
        )
    }
    
    /**
     * Convert Gemma analysis result to PhotoAnalysisWithTags format.
     */
    private fun convertGemmaToPhotoAnalysis(
        gemmaResult: SafetyAnalysisResult,
        workType: WorkType
    ): PhotoAnalysisWithTags {
        // Convert Gemma's hazard identifications to YOLO-style detections
        val detections = gemmaResult.hazardIdentifications.map { hazard ->
            HazardDetection(
                hazardType = mapStringToHazardType(hazard.hazardType),
                confidence = hazard.confidence,
                boundingBox = hazard.location?.boundingBox ?: BoundingBox(0.5f, 0.5f, 0.1f, 0.1f),
                oshaCategory = hazard.oshaViolation?.let { mapOSHAStringToCategory(it.regulation) },
                severity = hazard.severity,
                description = hazard.description
            )
        }
        
        // Convert Gemma's recommendations to UITagRecommendations
        val recommendations = gemmaResult.safetyRecommendations.map { rec ->
            UITagRecommendation(
                tagId = rec.category.lowercase().replace(" ", "-"),
                displayName = rec.description,
                confidence = gemmaResult.confidence,
                reason = "Gemma AI analysis: ${rec.description}",
                priority = mapRecommendationPriorityToTagPriority(rec.priority),
                oshaReference = rec.oshaReference
            )
        }
        
        // Determine auto-select tags based on confidence and severity
        val autoSelectTags = recommendations
            .filter { it.confidence > 0.8f && it.priority == TagPriority.CRITICAL }
            .map { it.tagId }
            .toSet()
        
        return PhotoAnalysisWithTags(
            detections = detections,
            recommendedTags = recommendations,
            autoSelectTags = autoSelectTags,
            complianceOverview = convertGemmaComplianceToOverview(gemmaResult.oshaCompliance),
            processingTimeMs = gemmaResult.processingTimeMs
        )
    }
    
    /**
     * Convert AI error result to PhotoAnalysisWithTags format.
     */
    private fun convertErrorResultToPhotoAnalysis(
        errorResult: AIAnalysisResult,
        workType: WorkType
    ): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = emptyList(),
            recommendedTags = errorResult.recommendations,
            autoSelectTags = emptySet(),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.UNKNOWN,
                criticalIssues = 0,
                oshaViolations = listOf(errorResult.errorMessage ?: "Analysis unavailable")
            ),
            processingTimeMs = errorResult.processingTimeMs
        )
    }
    
    /**
     * Get work type specific analysis prompt for Gemma.
     */
    private fun getWorkTypeSpecificPrompt(workType: WorkType): String {
        val basePrompt = GemmaVisionAnalyzer.DEFAULT_CONSTRUCTION_SAFETY_PROMPT
        
        val workTypeSpecific = when (workType) {
            WorkType.ELECTRICAL_WORK -> """
                Focus specifically on:
                - Electrical PPE (Class E hard hats, insulated gloves)
                - LOTO (Lockout/Tagout) procedures
                - GFCI protection
                - Electrical clearances and approach boundaries
                - Arc flash protection requirements
            """.trimIndent()
            
            WorkType.ROOFING -> """
                Focus specifically on:
                - Fall protection systems (guardrails, safety nets, personal fall arrest)
                - Roof edge protection
                - Ladder and access safety
                - Weather conditions assessment
                - Proper footwear and PPE for roofing
            """.trimIndent()
            
            WorkType.EXCAVATION -> """
                Focus specifically on:
                - Excavation protective systems (sloping, benching, shoring)
                - Competent person requirements
                - Atmospheric hazards and testing
                - Access and egress methods
                - Heavy equipment proximity
            """.trimIndent()
            
            else -> """
                Focus on general construction safety including:
                - Basic PPE compliance
                - Housekeeping and material storage
                - Equipment operation safety
                - General hazard identification
            """.trimIndent()
        }
        
        return "$basePrompt\n\n$workTypeSpecific"
    }
    
    // AIFallbackProvider implementation
    override suspend fun retryWithReducedTimeout(context: AIAnalysisContext): AIAnalysisResult {
        // Implementation for retry with reduced timeout
        return AIAnalysisResult(
            success = false,
            analysisType = AnalysisType.FALLBACK,
            recommendations = emptyList(),
            errorMessage = "Retry not implemented",
            confidence = 0f,
            processingTimeMs = 0L
        )
    }
    
    override suspend fun useAlternativeModel(context: AIAnalysisContext): AIAnalysisResult {
        // Fallback to YOLO if Gemma fails
        return try {
            val yoloResult = yoloDetector.detectHazards(
                ByteArray(0), // Placeholder - would need actual image data
                context.imageWidth,
                context.imageHeight
            )
            
            val mappingResult = tagMapper.mapToTags(yoloResult, context.workType)
            
            AIAnalysisResult(
                success = true,
                analysisType = AnalysisType.YOLO_DETECTION,
                recommendations = mappingResult.recommendations,
                confidence = 0.6f,
                processingTimeMs = 100L,
                modelUsed = "YOLO Fallback"
            )
        } catch (e: Exception) {
            AIAnalysisResult(
                success = false,
                analysisType = AnalysisType.FALLBACK,
                recommendations = emptyList(),
                errorMessage = "Alternative model failed: ${e.message}",
                confidence = 0f,
                processingTimeMs = 0L
            )
        }
    }
    
    override suspend fun generateOfflineAnalysis(context: AIAnalysisContext): AIAnalysisResult {
        // Generate basic offline recommendations based on work type
        val offlineRecommendations = when (context.workType) {
            WorkType.GENERAL_CONSTRUCTION -> listOf(
                UITagRecommendation.basic("general-safety-check"),
                UITagRecommendation.basic("ppe-inspection")
            )
            WorkType.ELECTRICAL_WORK -> listOf(
                UITagRecommendation.basic("electrical-safety-check"),
                UITagRecommendation.basic("lockout-tagout-verification")
            )
            else -> listOf(UITagRecommendation.basic("manual-safety-review"))
        }
        
        return AIAnalysisResult(
            success = true,
            analysisType = AnalysisType.OFFLINE,
            recommendations = offlineRecommendations,
            confidence = 0.4f,
            processingTimeMs = 10L,
            fallbackUsed = true,
            fallbackReason = "Offline mode - using basic safety guidelines"
        )
    }
    
    // Helper functions
    private fun getAvailableMemoryMB(): Int {
        val runtime = Runtime.getRuntime()
        return (runtime.maxMemory() / (1024 * 1024)).toInt()
    }
    
    private fun mapStringToHazardType(hazardString: String): HazardType {
        return when (hazardString.lowercase()) {
            "ppe_violation" -> HazardType.PERSON_NO_HARD_HAT
            "fall_hazard" -> HazardType.FALL_HAZARD
            "electrical_hazard" -> HazardType.ELECTRICAL_HAZARD
            else -> HazardType.PERSON_NO_HARD_HAT // Default fallback
        }
    }
    
    private fun mapOSHAStringToCategory(oshaString: String): OSHACategory? {
        return when (oshaString) {
            "1926.95" -> OSHACategory.SUBPART_E_1926_95
            "1926.501" -> OSHACategory.SUBPART_M_1926_501
            "1926.416" -> OSHACategory.SUBPART_K_1926_416
            else -> null
        }
    }
    
    private fun mapRecommendationPriorityToTagPriority(priority: RecommendationPriority): TagPriority {
        return when (priority) {
            RecommendationPriority.LOW -> TagPriority.LOW
            RecommendationPriority.MEDIUM -> TagPriority.MEDIUM
            RecommendationPriority.HIGH -> TagPriority.HIGH
            RecommendationPriority.CRITICAL -> TagPriority.CRITICAL
        }
    }
    
    private fun convertGemmaComplianceToOverview(oshaCompliance: OSHAComplianceAssessment): ComplianceOverview {
        return ComplianceOverview(
            overallLevel = oshaCompliance.complianceLevel,
            criticalIssues = oshaCompliance.criticalViolations,
            oshaViolations = oshaCompliance.violations.map { it.regulation }
        )
    }
    
    suspend fun release() {
        gemmaAnalyzer?.release()
        yoloDetector.release()
    }
    
    fun getModelStatus(): Map<String, Boolean> {
        return mapOf(
            "gemma_available" to isGemmaAvailable,
            "yolo_available" to isYoloAvailable,
            "gemma_loaded" to (gemmaAnalyzer?.isModelLoaded() ?: false),
            "yolo_loaded" to yoloDetector.isModelLoaded()
        )
    }
}
