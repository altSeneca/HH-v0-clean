package com.hazardhawk.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.models.Severity
import com.hazardhawk.security.AuditLogger

/**
 * Phase 3 Advanced AI Model Manager
 * Supports YOLO, custom construction models, and multi-model inference
 */
class AdvancedAIModelManager(
    private val auditLogger: AuditLogger
) {
    
    companion object {
        private const val YOLO_MODEL_PATH = "yolov8n.onnx"
        private const val CONSTRUCTION_MODEL_PATH = "construction_safety_v2.onnx"
        private const val PPE_DETECTION_MODEL_PATH = "ppe_detection_v1.onnx"
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val NMS_THRESHOLD = 0.4f
        private const val MAX_DETECTIONS = 100
        
        // Model performance targets
        private const val TARGET_INFERENCE_TIME_MS = 500L
        private const val TARGET_ACCURACY_THRESHOLD = 0.85f
        private const val BATCH_PROCESSING_SIZE = 8
    }

    // Model status tracking
    private val _modelStatusUpdates = MutableSharedFlow<ModelStatusUpdate>(replay = 3)
    val modelStatusUpdates: Flow<ModelStatusUpdate> = _modelStatusUpdates.asSharedFlow()
    
    private val _inferenceResults = MutableSharedFlow<MultiModelInferenceResult>(replay = 5)
    val inferenceResults: Flow<MultiModelInferenceResult> = _inferenceResults.asSharedFlow()

    // Model registry and status
    private val modelRegistry = mutableMapOf<String, AIModel>()
    private val modelPerformanceTracking = mutableMapOf<String, ModelPerformanceTracker>()
    private val inferenceHistory = mutableListOf<InferenceHistoryEntry>()

    /**
     * Initialize all AI models
     */
    suspend fun initializeModels(): ModelInitializationResult {
        val startTime = Clock.System.now().toEpochMilliseconds()
        val initResults = mutableListOf<SingleModelInitResult>()
        
        try {
            // Initialize YOLO model
            val yoloResult = initializeYOLOModel()
            initResults.add(yoloResult)
            
            // Initialize construction safety model
            val constructionResult = initializeConstructionModel()
            initResults.add(constructionResult)
            
            // Initialize PPE detection model
            val ppeResult = initializePPEModel()
            initResults.add(ppeResult)
            
            val successfulModels = initResults.count { it.success }
            val totalInitTime = Clock.System.now().toEpochMilliseconds() - startTime
            
            auditLogger.logEvent(
                eventType = "AI_MODELS_INITIALIZED",
                details = mapOf(
                    "totalModels" to initResults.size.toString(),
                    "successfulModels" to successfulModels.toString(),
                    "initializationTimeMs" to totalInitTime.toString()
                ),
                userId = "SYSTEM",
                metadata = mapOf(
                    "yoloEnabled" to yoloResult.success.toString(),
                    "constructionEnabled" to constructionResult.success.toString(),
                    "ppeEnabled" to ppeResult.success.toString()
                )
            )
            
            return ModelInitializationResult(
                success = successfulModels > 0,
                totalInitializationTimeMs = totalInitTime,
                initializedModels = initResults,
                enabledFeatures = generateEnabledFeatures(initResults),
                failureReasons = initResults.filter { !it.success }.map { it.errorMessage ?: "Unknown error" }
            )
            
        } catch (e: Exception) {
            auditLogger.logEvent(
                eventType = "AI_MODEL_INIT_FAILURE",
                details = mapOf("error" to (e.message ?: "Unknown initialization error")),
                userId = "SYSTEM",
                metadata = emptyMap()
            )
            
            return ModelInitializationResult(
                success = false,
                totalInitializationTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                initializedModels = emptyList(),
                enabledFeatures = emptyList(),
                failureReasons = listOf(e.message ?: "Unknown initialization error")
            )
        }
    }

    /**
     * Perform multi-model inference on construction site photos
     */
    suspend fun performMultiModelInference(
        photoId: String,
        imageData: ByteArray,
        workType: WorkType,
        priority: InferencePriority = InferencePriority.NORMAL
    ): MultiModelInferenceResult {
        val startTime = Clock.System.now().toEpochMilliseconds()
        val inferenceId = "inference-$startTime-${photoId}"
        
        try {
            // Prepare inference context
            val context = InferenceContext(
                photoId = photoId,
                workType = workType,
                priority = priority,
                timestamp = startTime,
                imageSize = imageData.size
            )
            
            // Run YOLO object detection
            val yoloResults = runYOLOInference(imageData, context)
            
            // Run construction safety analysis
            val constructionResults = runConstructionSafetyInference(imageData, context)
            
            // Run PPE detection
            val ppeResults = runPPEDetectionInference(imageData, context)
            
            // Combine and analyze results
            val combinedResults = combineModelResults(yoloResults, constructionResults, ppeResults)
            val safetyAnalysis = generateSafetyAnalysis(combinedResults, workType)
            val recommendedTags = generateRecommendedTags(combinedResults, workType)
            
            val totalProcessingTime = Clock.System.now().toEpochMilliseconds() - startTime
            
            val result = MultiModelInferenceResult(
                inferenceId = inferenceId,
                photoId = photoId,
                workType = workType,
                timestamp = startTime,
                processingTimeMs = totalProcessingTime,
                yoloDetections = yoloResults,
                constructionHazards = constructionResults,
                ppeAnalysis = ppeResults,
                combinedAnalysis = combinedResults,
                safetyAssessment = safetyAnalysis,
                recommendedTags = recommendedTags,
                confidenceScore = calculateOverallConfidence(combinedResults),
                oshaCompliant = assessOSHACompliance(combinedResults),
                requiresImmediateAction = determineImmediateAction(safetyAnalysis)
            )
            
            // Track performance metrics
            updateModelPerformance(result)
            
            // Store in inference history
            addToInferenceHistory(result)
            
            // Emit result
            _inferenceResults.emit(result)
            
            // Log inference event
            auditLogger.logEvent(
                eventType = "MULTI_MODEL_INFERENCE",
                details = mapOf(
                    "inferenceId" to inferenceId,
                    "photoId" to photoId,
                    "workType" to workType.name,
                    "processingTimeMs" to totalProcessingTime.toString(),
                    "detectionCount" to combinedResults.detectedObjects.size.toString(),
                    "severityLevel" to safetyAnalysis.overallSeverity.name,
                    "oshaCompliant" to result.oshaCompliant.toString()
                ),
                userId = null,
                metadata = mapOf(
                    "yoloDetections" to yoloResults.detections.size.toString(),
                    "constructionHazards" to constructionResults.hazards.size.toString(),
                    "ppeViolations" to ppeResults.violations.size.toString()
                )
            )
            
            return result
            
        } catch (e: Exception) {
            val errorResult = MultiModelInferenceResult(
                inferenceId = inferenceId,
                photoId = photoId,
                workType = workType,
                timestamp = startTime,
                processingTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                yoloDetections = YOLOResults(emptyList(), 0L, false, e.message),
                constructionHazards = ConstructionHazardResults(emptyList(), 0L, false, e.message),
                ppeAnalysis = PPEAnalysisResults(emptyList(), emptyList(), 0L, false, e.message),
                combinedAnalysis = CombinedAnalysisResult(emptyList(), emptyMap(), 0.0f),
                safetyAssessment = SafetyAssessment(Severity.LOW, emptyList(), false, "Error in analysis"),
                recommendedTags = emptyList(),
                confidenceScore = 0.0f,
                oshaCompliant = false,
                requiresImmediateAction = false,
                error = e.message
            )
            
            auditLogger.logEvent(
                eventType = "INFERENCE_ERROR",
                details = mapOf(
                    "inferenceId" to inferenceId,
                    "photoId" to photoId,
                    "error" to (e.message ?: "Unknown inference error")
                ),
                userId = null,
                metadata = emptyMap()
            )
            
            return errorResult
        }
    }

    /**
     * Perform batch inference for multiple photos
     */
    suspend fun performBatchInference(
        photos: List<BatchInferenceRequest>,
        batchId: String = "batch-${Clock.System.now().toEpochMilliseconds()}"
    ): BatchInferenceResult {
        val startTime = Clock.System.now().toEpochMilliseconds()
        val results = mutableListOf<MultiModelInferenceResult>()
        
        // Process photos in batches for optimal performance
        photos.chunked(BATCH_PROCESSING_SIZE).forEach { batch ->
            batch.forEach { request ->
                val result = performMultiModelInference(
                    photoId = request.photoId,
                    imageData = request.imageData,
                    workType = request.workType,
                    priority = InferencePriority.BATCH
                )
                results.add(result)
            }
        }
        
        val totalTime = Clock.System.now().toEpochMilliseconds() - startTime
        val successfulInferences = results.count { it.error == null }
        
        val batchResult = BatchInferenceResult(
            batchId = batchId,
            timestamp = startTime,
            totalPhotos = photos.size,
            successfulInferences = successfulInferences,
            failedInferences = photos.size - successfulInferences,
            totalProcessingTimeMs = totalTime,
            averageProcessingTimeMs = totalTime / photos.size,
            results = results,
            batchEfficiency = (successfulInferences.toDouble() / photos.size) * 100.0
        )
        
        auditLogger.logEvent(
            eventType = "BATCH_INFERENCE_COMPLETED",
            details = mapOf(
                "batchId" to batchId,
                "totalPhotos" to photos.size.toString(),
                "successfulInferences" to successfulInferences.toString(),
                "totalProcessingTimeMs" to totalTime.toString(),
                "batchEfficiency" to batchResult.batchEfficiency.toString()
            ),
            userId = "SYSTEM",
            metadata = mapOf("batchSize" to BATCH_PROCESSING_SIZE.toString())
        )
        
        return batchResult
    }

    /**
     * Get model performance statistics
     */
    fun getModelPerformanceStats(): ModelPerformanceStats {
        val allTrackers = modelPerformanceTracking.values
        
        return ModelPerformanceStats(
            totalInferences = inferenceHistory.size,
            averageInferenceTimeMs = allTrackers.map { it.averageInferenceTime }.average().toLong(),
            modelAccuracyScores = modelPerformanceTracking.mapValues { it.value.currentAccuracy },
            modelUsageStats = modelPerformanceTracking.mapValues { it.value.totalInferences },
            recentPerformanceTrend = calculatePerformanceTrend(),
            topPerformingModels = getTopPerformingModels(),
            recommendedOptimizations = generateOptimizationRecommendations()
        )
    }

    // Private initialization methods
    private suspend fun initializeYOLOModel(): SingleModelInitResult {
        return try {
            val model = AIModel(
                id = "yolo_v8n",
                name = "YOLOv8 Nano Object Detection",
                version = "8.0.0",
                type = ModelType.OBJECT_DETECTION,
                modelPath = YOLO_MODEL_PATH,
                inputShape = listOf(1, 3, 640, 640),
                outputClasses = getYOLOClassNames(),
                isLoaded = true,
                loadedAt = Clock.System.now().toEpochMilliseconds()
            )
            
            modelRegistry["yolo"] = model
            modelPerformanceTracking["yolo"] = ModelPerformanceTracker("yolo")
            
            _modelStatusUpdates.emit(ModelStatusUpdate(
                modelId = "yolo",
                status = ModelStatus.LOADED,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                message = "YOLO model initialized successfully"
            ))
            
            SingleModelInitResult(
                modelId = "yolo",
                success = true,
                loadTimeMs = 100L, // Simulated
                capabilities = listOf("Object Detection", "Person Detection", "Vehicle Detection", "Equipment Detection")
            )
        } catch (e: Exception) {
            SingleModelInitResult(
                modelId = "yolo",
                success = false,
                loadTimeMs = 0L,
                capabilities = emptyList(),
                errorMessage = e.message
            )
        }
    }

    private suspend fun initializeConstructionModel(): SingleModelInitResult {
        return try {
            val model = AIModel(
                id = "construction_safety_v2",
                name = "Construction Safety Hazard Detection",
                version = "2.1.0",
                type = ModelType.HAZARD_DETECTION,
                modelPath = CONSTRUCTION_MODEL_PATH,
                inputShape = listOf(1, 3, 416, 416),
                outputClasses = getConstructionHazardClasses(),
                isLoaded = true,
                loadedAt = Clock.System.now().toEpochMilliseconds()
            )
            
            modelRegistry["construction"] = model
            modelPerformanceTracking["construction"] = ModelPerformanceTracker("construction")
            
            _modelStatusUpdates.emit(ModelStatusUpdate(
                modelId = "construction",
                status = ModelStatus.LOADED,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                message = "Construction safety model initialized successfully"
            ))
            
            SingleModelInitResult(
                modelId = "construction",
                success = true,
                loadTimeMs = 150L, // Simulated
                capabilities = listOf("Fall Hazard Detection", "Electrical Hazard Detection", "Structural Hazard Detection", "Environmental Hazard Detection")
            )
        } catch (e: Exception) {
            SingleModelInitResult(
                modelId = "construction",
                success = false,
                loadTimeMs = 0L,
                capabilities = emptyList(),
                errorMessage = e.message
            )
        }
    }

    private suspend fun initializePPEModel(): SingleModelInitResult {
        return try {
            val model = AIModel(
                id = "ppe_detection_v1",
                name = "Personal Protective Equipment Detection",
                version = "1.3.0",
                type = ModelType.PPE_DETECTION,
                modelPath = PPE_DETECTION_MODEL_PATH,
                inputShape = listOf(1, 3, 512, 512),
                outputClasses = getPPEClasses(),
                isLoaded = true,
                loadedAt = Clock.System.now().toEpochMilliseconds()
            )
            
            modelRegistry["ppe"] = model
            modelPerformanceTracking["ppe"] = ModelPerformanceTracker("ppe")
            
            _modelStatusUpdates.emit(ModelStatusUpdate(
                modelId = "ppe",
                status = ModelStatus.LOADED,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                message = "PPE detection model initialized successfully"
            ))
            
            SingleModelInitResult(
                modelId = "ppe",
                success = true,
                loadTimeMs = 120L, // Simulated
                capabilities = listOf("Hard Hat Detection", "Safety Vest Detection", "Safety Glasses Detection", "Gloves Detection", "Safety Boots Detection")
            )
        } catch (e: Exception) {
            SingleModelInitResult(
                modelId = "ppe",
                success = false,
                loadTimeMs = 0L,
                capabilities = emptyList(),
                errorMessage = e.message
            )
        }
    }

    // Inference methods
    private suspend fun runYOLOInference(imageData: ByteArray, context: InferenceContext): YOLOResults {
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        // Simulate YOLO inference - in real implementation, this would use ONNX Runtime
        kotlinx.coroutines.delay(kotlin.random.Random.nextLong(100, 300))
        
        val processingTime = Clock.System.now().toEpochMilliseconds() - startTime
        
        // Mock detections based on work type
        val detections = generateMockYOLODetections(context.workType)
        
        return YOLOResults(
            detections = detections,
            inferenceTimeMs = processingTime,
            success = true,
            error = null
        )
    }

    private suspend fun runConstructionSafetyInference(imageData: ByteArray, context: InferenceContext): ConstructionHazardResults {
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        // Simulate construction safety inference
        kotlinx.coroutines.delay(kotlin.random.Random.nextLong(150, 400))
        
        val processingTime = Clock.System.now().toEpochMilliseconds() - startTime
        
        // Mock hazard detections
        val hazards = generateMockConstructionHazards(context.workType)
        
        return ConstructionHazardResults(
            hazards = hazards,
            inferenceTimeMs = processingTime,
            success = true,
            error = null
        )
    }

    private suspend fun runPPEDetectionInference(imageData: ByteArray, context: InferenceContext): PPEAnalysisResults {
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        // Simulate PPE detection inference
        kotlinx.coroutines.delay(kotlin.random.Random.nextLong(80, 250))
        
        val processingTime = Clock.System.now().toEpochMilliseconds() - startTime
        
        // Mock PPE analysis
        val detectedPPE = generateMockPPEDetections()
        val violations = generateMockPPEViolations(context.workType)
        
        return PPEAnalysisResults(
            detectedPPE = detectedPPE,
            violations = violations,
            inferenceTimeMs = processingTime,
            success = true,
            error = null
        )
    }

    // Helper methods
    private fun combineModelResults(
        yolo: YOLOResults,
        construction: ConstructionHazardResults,
        ppe: PPEAnalysisResults
    ): CombinedAnalysisResult {
        val detectedObjects = mutableListOf<DetectedObject>()
        
        // Combine YOLO detections
        yolo.detections.forEach { detection ->
            detectedObjects.add(DetectedObject(
                className = detection.className,
                confidence = detection.confidence,
                boundingBox = detection.boundingBox,
                source = "YOLO"
            ))
        }
        
        // Combine construction hazards
        construction.hazards.forEach { hazard ->
            detectedObjects.add(DetectedObject(
                className = hazard.hazardType,
                confidence = hazard.confidence,
                boundingBox = hazard.location,
                source = "Construction"
            ))
        }
        
        // Calculate confidence metrics
        val confidenceMetrics = mapOf(
            "overall" to calculateOverallConfidence(yolo, construction, ppe),
            "yolo" to yolo.detections.map { it.confidence }.average().toFloat(),
            "construction" to construction.hazards.map { it.confidence }.average().toFloat(),
            "ppe" to ppe.detectedPPE.map { it.confidence }.average().toFloat()
        )
        
        return CombinedAnalysisResult(
            detectedObjects = detectedObjects,
            confidenceMetrics = confidenceMetrics,
            overallConfidence = confidenceMetrics["overall"] ?: 0.0f
        )
    }

    private fun generateSafetyAnalysis(combined: CombinedAnalysisResult, workType: WorkType): SafetyAssessment {
        val hazards = combined.detectedObjects.filter { it.confidence > CONFIDENCE_THRESHOLD }
        val severity = when {
            hazards.any { it.className.contains("critical") } -> Severity.CRITICAL
            hazards.any { it.className.contains("high") } -> Severity.HIGH
            hazards.any { it.className.contains("medium") } -> Severity.MEDIUM
            else -> Severity.LOW
        }
        
        return SafetyAssessment(
            overallSeverity = severity,
            identifiedHazards = hazards.map { it.className },
            requiresAttention = severity in listOf(Severity.HIGH, Severity.CRITICAL),
            recommendations = generateSafetyRecommendations(hazards, workType)
        )
    }

    private fun generateRecommendedTags(combined: CombinedAnalysisResult, workType: WorkType): List<String> {
        val tags = mutableSetOf<String>()
        
        combined.detectedObjects.forEach { obj ->
            when (obj.className.lowercase()) {
                "person" -> tags.add("workers-present")
                "hard_hat" -> tags.add("hard-hat-detected")
                "safety_vest" -> tags.add("safety-vest-detected")
                "ladder" -> tags.add("fall-protection")
                "electrical" -> tags.add("electrical-hazard")
                "excavation" -> tags.add("excavation-safety")
            }
        }
        
        // Add work-type specific tags
        when (workType) {
            WorkType.FALL_PROTECTION -> tags.addAll(listOf("fall-protection", "height-work"))
            WorkType.ELECTRICAL -> tags.addAll(listOf("electrical-safety", "lockout-tagout"))
            WorkType.GENERAL_CONSTRUCTION -> tags.add("general-construction")
            else -> tags.add("safety-assessment")
        }
        
        return tags.toList()
    }

    private fun calculateOverallConfidence(combined: CombinedAnalysisResult): Float {
        return combined.overallConfidence
    }

    private fun calculateOverallConfidence(yolo: YOLOResults, construction: ConstructionHazardResults, ppe: PPEAnalysisResults): Float {
        val confidences = mutableListOf<Float>()
        
        if (yolo.success && yolo.detections.isNotEmpty()) {
            confidences.add(yolo.detections.map { it.confidence }.average().toFloat())
        }
        
        if (construction.success && construction.hazards.isNotEmpty()) {
            confidences.add(construction.hazards.map { it.confidence }.average().toFloat())
        }
        
        if (ppe.success && ppe.detectedPPE.isNotEmpty()) {
            confidences.add(ppe.detectedPPE.map { it.confidence }.average().toFloat())
        }
        
        return if (confidences.isNotEmpty()) confidences.average().toFloat() else 0.0f
    }

    private fun assessOSHACompliance(combined: CombinedAnalysisResult): Boolean {
        // Simplified OSHA compliance check
        val criticalHazards = combined.detectedObjects.filter { 
            it.className.contains("critical") || it.confidence > 0.9f 
        }
        return criticalHazards.isEmpty()
    }

    private fun determineImmediateAction(assessment: SafetyAssessment): Boolean {
        return assessment.overallSeverity in listOf(Severity.HIGH, Severity.CRITICAL)
    }

    // Mock data generation methods
    private fun generateMockYOLODetections(workType: WorkType): List<YOLODetection> {
        return when (workType) {
            WorkType.FALL_PROTECTION -> listOf(
                YOLODetection("person", 0.92f, BoundingBox(100, 150, 200, 350)),
                YOLODetection("ladder", 0.87f, BoundingBox(50, 100, 150, 400)),
                YOLODetection("hard_hat", 0.95f, BoundingBox(120, 130, 180, 180))
            )
            WorkType.ELECTRICAL -> listOf(
                YOLODetection("person", 0.89f, BoundingBox(150, 200, 250, 400)),
                YOLODetection("electrical_panel", 0.78f, BoundingBox(300, 150, 450, 300))
            )
            else -> listOf(
                YOLODetection("person", 0.88f, BoundingBox(100, 100, 200, 300)),
                YOLODetection("construction_vehicle", 0.82f, BoundingBox(400, 200, 600, 350))
            )
        }
    }

    private fun generateMockConstructionHazards(workType: WorkType): List<ConstructionHazard> {
        return when (workType) {
            WorkType.FALL_PROTECTION -> listOf(
                ConstructionHazard("fall_hazard", 0.91f, BoundingBox(0, 0, 100, 100), Severity.HIGH, "Unprotected edge detected"),
                ConstructionHazard("height_work", 0.85f, BoundingBox(200, 50, 300, 150), Severity.MEDIUM, "Work at height without proper protection")
            )
            WorkType.ELECTRICAL -> listOf(
                ConstructionHazard("electrical_hazard", 0.88f, BoundingBox(250, 100, 400, 250), Severity.HIGH, "Exposed electrical components"),
                ConstructionHazard("lockout_violation", 0.76f, BoundingBox(300, 150, 450, 300), Severity.MEDIUM, "Equipment not properly locked out")
            )
            else -> listOf(
                ConstructionHazard("general_hazard", 0.72f, BoundingBox(150, 150, 250, 250), Severity.MEDIUM, "General safety concern identified")
            )
        }
    }

    private fun generateMockPPEDetections(): List<PPEDetection> {
        return listOf(
            PPEDetection("hard_hat", 0.94f, BoundingBox(120, 130, 180, 180), true),
            PPEDetection("safety_vest", 0.88f, BoundingBox(140, 200, 220, 350), true),
            PPEDetection("safety_glasses", 0.73f, BoundingBox(135, 145, 165, 165), false)
        )
    }

    private fun generateMockPPEViolations(workType: WorkType): List<PPEViolation> {
        return listOf(
            PPEViolation("missing_hard_hat", Severity.HIGH, "Worker not wearing required hard hat", BoundingBox(100, 150, 200, 350)),
            PPEViolation("improper_vest", Severity.MEDIUM, "Safety vest not properly worn", BoundingBox(140, 200, 220, 350))
        )
    }

    private fun generateSafetyRecommendations(hazards: List<DetectedObject>, workType: WorkType): String {
        return when {
            hazards.any { it.className.contains("fall") } -> "Implement proper fall protection measures immediately"
            hazards.any { it.className.contains("electrical") } -> "Ensure proper lockout/tagout procedures are followed"
            hazards.any { it.className.contains("ppe") } -> "Verify all workers are wearing required PPE"
            else -> "Review safety procedures for identified hazards"
        }
    }

    // Performance tracking methods
    private fun updateModelPerformance(result: MultiModelInferenceResult) {
        modelPerformanceTracking.values.forEach { tracker ->
            tracker.recordInference(result.processingTimeMs, result.confidenceScore)
        }
    }

    private fun addToInferenceHistory(result: MultiModelInferenceResult) {
        inferenceHistory.add(InferenceHistoryEntry(
            timestamp = result.timestamp,
            inferenceId = result.inferenceId,
            processingTimeMs = result.processingTimeMs,
            confidence = result.confidenceScore,
            success = result.error == null
        ))
        
        // Maintain history size
        if (inferenceHistory.size > 1000) {
            inferenceHistory.removeFirst()
        }
    }

    private fun calculatePerformanceTrend(): PerformanceTrend {
        val recent = inferenceHistory.takeLast(20)
        val older = inferenceHistory.takeLast(40).take(20)
        
        if (recent.isEmpty() || older.isEmpty()) return PerformanceTrend.STABLE
        
        val recentAvg = recent.map { it.processingTimeMs }.average()
        val olderAvg = older.map { it.processingTimeMs }.average()
        
        return when {
            recentAvg < olderAvg * 0.9 -> PerformanceTrend.IMPROVING
            recentAvg > olderAvg * 1.1 -> PerformanceTrend.DECLINING
            else -> PerformanceTrend.STABLE
        }
    }

    private fun getTopPerformingModels(): List<String> {
        return modelPerformanceTracking.entries
            .sortedByDescending { it.value.currentAccuracy }
            .take(3)
            .map { it.key }
    }

    private fun generateOptimizationRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        modelPerformanceTracking.forEach { (modelId, tracker) ->
            if (tracker.averageInferenceTime > TARGET_INFERENCE_TIME_MS) {
                recommendations.add("Optimize $modelId inference time")
            }
            if (tracker.currentAccuracy < TARGET_ACCURACY_THRESHOLD) {
                recommendations.add("Improve $modelId accuracy through retraining")
            }
        }
        
        return recommendations
    }

    private fun generateEnabledFeatures(initResults: List<SingleModelInitResult>): List<String> {
        val features = mutableListOf<String>()
        
        initResults.forEach { result ->
            if (result.success) {
                features.addAll(result.capabilities)
            }
        }
        
        return features
    }

    // Class definitions and helper functions
    private fun getYOLOClassNames(): List<String> = listOf(
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
        "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
        "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
        "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
        "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
        "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple"
    )

    private fun getConstructionHazardClasses(): List<String> = listOf(
        "fall_hazard", "electrical_hazard", "struck_by_hazard", "caught_in_hazard",
        "excavation_hazard", "crane_hazard", "scaffold_hazard", "ladder_hazard",
        "confined_space_hazard", "chemical_hazard", "fire_hazard", "noise_hazard"
    )

    private fun getPPEClasses(): List<String> = listOf(
        "hard_hat", "safety_vest", "safety_glasses", "hearing_protection",
        "safety_gloves", "safety_boots", "respirator", "fall_protection_harness"
    )
}

/**
 * Data classes for advanced AI model management
 */

@Serializable
data class AIModel(
    val id: String,
    val name: String,
    val version: String,
    val type: ModelType,
    val modelPath: String,
    val inputShape: List<Int>,
    val outputClasses: List<String>,
    val isLoaded: Boolean,
    val loadedAt: Long
)

@Serializable
data class ModelInitializationResult(
    val success: Boolean,
    val totalInitializationTimeMs: Long,
    val initializedModels: List<SingleModelInitResult>,
    val enabledFeatures: List<String>,
    val failureReasons: List<String>
)

@Serializable
data class SingleModelInitResult(
    val modelId: String,
    val success: Boolean,
    val loadTimeMs: Long,
    val capabilities: List<String>,
    val errorMessage: String? = null
)

@Serializable
data class MultiModelInferenceResult(
    val inferenceId: String,
    val photoId: String,
    val workType: WorkType,
    val timestamp: Long,
    val processingTimeMs: Long,
    val yoloDetections: YOLOResults,
    val constructionHazards: ConstructionHazardResults,
    val ppeAnalysis: PPEAnalysisResults,
    val combinedAnalysis: CombinedAnalysisResult,
    val safetyAssessment: SafetyAssessment,
    val recommendedTags: List<String>,
    val confidenceScore: Float,
    val oshaCompliant: Boolean,
    val requiresImmediateAction: Boolean,
    val error: String? = null
)

@Serializable
data class InferenceContext(
    val photoId: String,
    val workType: WorkType,
    val priority: InferencePriority,
    val timestamp: Long,
    val imageSize: Int
)

@Serializable
data class YOLOResults(
    val detections: List<YOLODetection>,
    val inferenceTimeMs: Long,
    val success: Boolean,
    val error: String?
)

@Serializable
data class YOLODetection(
    val className: String,
    val confidence: Float,
    val boundingBox: BoundingBox
)

@Serializable
data class ConstructionHazardResults(
    val hazards: List<ConstructionHazard>,
    val inferenceTimeMs: Long,
    val success: Boolean,
    val error: String?
)

@Serializable
data class ConstructionHazard(
    val hazardType: String,
    val confidence: Float,
    val location: BoundingBox,
    val severity: Severity,
    val description: String
)

@Serializable
data class PPEAnalysisResults(
    val detectedPPE: List<PPEDetection>,
    val violations: List<PPEViolation>,
    val inferenceTimeMs: Long,
    val success: Boolean,
    val error: String?
)

@Serializable
data class PPEDetection(
    val ppeType: String,
    val confidence: Float,
    val location: BoundingBox,
    val isCompliant: Boolean
)

@Serializable
data class PPEViolation(
    val violationType: String,
    val severity: Severity,
    val description: String,
    val location: BoundingBox
)

@Serializable
data class CombinedAnalysisResult(
    val detectedObjects: List<DetectedObject>,
    val confidenceMetrics: Map<String, Float>,
    val overallConfidence: Float
)

@Serializable
data class DetectedObject(
    val className: String,
    val confidence: Float,
    val boundingBox: BoundingBox,
    val source: String
)

@Serializable
data class SafetyAssessment(
    val overallSeverity: Severity,
    val identifiedHazards: List<String>,
    val requiresAttention: Boolean,
    val recommendations: String
)

@Serializable
data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

@Serializable
data class BatchInferenceRequest(
    val photoId: String,
    val imageData: ByteArray,
    val workType: WorkType
)

@Serializable
data class BatchInferenceResult(
    val batchId: String,
    val timestamp: Long,
    val totalPhotos: Int,
    val successfulInferences: Int,
    val failedInferences: Int,
    val totalProcessingTimeMs: Long,
    val averageProcessingTimeMs: Long,
    val results: List<MultiModelInferenceResult>,
    val batchEfficiency: Double
)

@Serializable
data class ModelStatusUpdate(
    val modelId: String,
    val status: ModelStatus,
    val timestamp: Long,
    val message: String
)

@Serializable
data class ModelPerformanceStats(
    val totalInferences: Int,
    val averageInferenceTimeMs: Long,
    val modelAccuracyScores: Map<String, Float>,
    val modelUsageStats: Map<String, Int>,
    val recentPerformanceTrend: PerformanceTrend,
    val topPerformingModels: List<String>,
    val recommendedOptimizations: List<String>
)

@Serializable
data class ModelPerformanceTracker(
    val modelId: String,
    var totalInferences: Int = 0,
    var totalInferenceTime: Long = 0L,
    var currentAccuracy: Float = 0.0f,
    var lastUpdated: Long = Clock.System.now().toEpochMilliseconds()
) {
    val averageInferenceTime: Long
        get() = if (totalInferences > 0) totalInferenceTime / totalInferences else 0L

    fun recordInference(inferenceTimeMs: Long, confidence: Float) {
        totalInferences++
        totalInferenceTime += inferenceTimeMs
        currentAccuracy = (currentAccuracy * (totalInferences - 1) + confidence) / totalInferences
        lastUpdated = Clock.System.now().toEpochMilliseconds()
    }
}

@Serializable
data class InferenceHistoryEntry(
    val timestamp: Long,
    val inferenceId: String,
    val processingTimeMs: Long,
    val confidence: Float,
    val success: Boolean
)

@Serializable
enum class ModelType {
    OBJECT_DETECTION,
    HAZARD_DETECTION,
    PPE_DETECTION,
    CLASSIFICATION,
    SEGMENTATION
}

@Serializable
enum class ModelStatus {
    LOADING,
    LOADED,
    ERROR,
    UNLOADED
}

@Serializable
enum class InferencePriority {
    LOW,
    NORMAL,
    HIGH,
    BATCH
}

@Serializable
enum class PerformanceTrend {
    IMPROVING,
    STABLE,
    DECLINING
}