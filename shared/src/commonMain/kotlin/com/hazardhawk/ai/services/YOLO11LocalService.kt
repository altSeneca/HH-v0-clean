package com.hazardhawk.ai.services

import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.ai.models.*
import kotlinx.coroutines.delay
import kotlinx.uuid.uuid4

/**
 * YOLO11-based local object detection service.
 * This serves as the final fallback when both Gemma and Vertex AI are unavailable.
 * Provides basic hazard detection but limited contextual understanding.
 */
class YOLO11LocalService : AIPhotoAnalyzer {
    
    private var isModelLoaded = false
    
    override val analyzerName = "YOLO11 Local Detection"
    override val priority = 50 // Lowest priority - basic fallback only
    
    override val analysisCapabilities = setOf(
        AnalysisCapability.PPE_DETECTION,
        AnalysisCapability.HAZARD_IDENTIFICATION,
        AnalysisCapability.OFFLINE_ANALYSIS,
        AnalysisCapability.REAL_TIME_PROCESSING
    )
    
    override val isAvailable: Boolean
        get() = isModelLoaded
    
    override suspend fun configure(apiKey: String?): Result<Unit> {
        return try {
            // TODO: Load YOLO11 model
            /*
            val modelPath = "/models/yolo_hazard/hazard_detection_yolo11.onnx"
            val modelFile = File(modelPath)
            
            if (modelFile.exists()) {
                // Load YOLO11 ONNX model
                val env = OrtEnvironment.getEnvironment()
                val sessionOptions = OrtSession.SessionOptions()
                yoloSession = env.createSession(modelFile.absolutePath, sessionOptions)
                isModelLoaded = true
            }
            */
            
            // Mock model loading for development
            delay(1000) // Simulate model loading time
            isModelLoaded = true
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("YOLO11 model loading failed: ${e.message}", e))
        }
    }
    
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType
    ): Result<SafetyAnalysis> {

        if (!isAvailable) {
            return Result.failure(Exception("YOLO11 model not loaded"))
        }

        val startTime = System.currentTimeMillis()

        try {
            // Simulate fast local inference
            delay(800)

            // TODO: Implement actual YOLO11 inference
            /*
            // Preprocess image for YOLO11
            val preprocessedImage = preprocessForYOLO(imageData)

            // Run inference
            val inputMap = mapOf("images" to OnnxTensor.createTensor(env, preprocessedImage))
            val results = yoloSession.run(inputMap)

            // Parse YOLO detections
            val detections = parseYOLOResults(results)
            return createSafetyAnalysisFromDetections(detections, workType, startTime)
            */

            // Mock YOLO detections WITH bounding boxes for development
            val mockDetections = createMockYOLODetections(workType)
            val analysis = createSafetyAnalysisFromDetections(mockDetections, workType, startTime)
            Result.success(analysis)

        } catch (e: Exception) {
            Result.failure(Exception("YOLO11 analysis failed: ${e.message}", e))
        }
    }

    /**
     * YOLO Detection data class with bounding box.
     */
    data class YOLODetection(
        val classId: Int,
        val className: String,
        val confidence: Float,
        val boundingBox: BoundingBox // Normalized 0-1 coordinates
    )

    /**
     * Map YOLO object detections to construction safety hazards.
     * This converts raw object detection into actionable safety analysis.
     */
    private fun createSafetyAnalysisFromDetections(
        detections: List<YOLODetection>,
        workType: WorkType,
        startTime: Long
    ): SafetyAnalysis {
        val hazards = detections.mapNotNull { detection ->
            mapYOLODetectionToHazard(detection, workType)
        }

        // Calculate overall risk based on detected hazards
        val riskLevel = when {
            hazards.any { it.severity == Severity.CRITICAL } -> RiskLevel.SEVERE
            hazards.any { it.severity == Severity.HIGH } -> RiskLevel.HIGH
            hazards.any { it.severity == Severity.MEDIUM } -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        return SafetyAnalysis(
            id = uuid4().toString(),
            timestamp = System.currentTimeMillis(),
            analysisType = AnalysisType.LOCAL_YOLO_FALLBACK,
            workType = workType,
            hazards = hazards, // NOW WITH BOUNDING BOXES!
            ppeStatus = inferPPEStatus(detections),
            recommendations = generateYOLORecommendations(hazards, workType),
            overallRiskLevel = riskLevel,
            confidence = calculateOverallConfidence(detections),
            processingTimeMs = System.currentTimeMillis() - startTime,
            oshaViolations = generateOSHAViolations(hazards)
        )
    }

    /**
     * Map individual YOLO detection to safety hazard.
     */
    private fun mapYOLODetectionToHazard(detection: YOLODetection, workType: WorkType): Hazard? {
        return when (detection.className) {
            "person-no-hardhat", "worker-no-helmet" -> Hazard(
                id = uuid4().toString(),
                type = HazardType.PPE_VIOLATION,
                severity = Severity.HIGH,
                description = "Worker detected without required hard hat",
                boundingBox = detection.boundingBox, // PASS THROUGH BOUNDING BOX
                confidence = detection.confidence,
                oshaCode = "1926.95",
                recommendations = listOf(
                    "Ensure all workers wear ANSI-approved hard hats",
                    "Conduct PPE compliance training"
                )
            )

            "person-no-vest", "worker-no-safety-vest" -> Hazard(
                id = uuid4().toString(),
                type = HazardType.PPE_VIOLATION,
                severity = Severity.MEDIUM,
                description = "Worker not wearing high-visibility safety vest",
                boundingBox = detection.boundingBox,
                confidence = detection.confidence,
                oshaCode = "1926.95",
                recommendations = listOf(
                    "Provide Class 2 or 3 high-visibility safety vests",
                    "Enforce PPE compliance policy"
                )
            )

            "unguarded-edge", "fall-hazard", "open-edge" -> Hazard(
                id = uuid4().toString(),
                type = HazardType.FALL_PROTECTION,
                severity = Severity.CRITICAL,
                description = "Unguarded edge or fall hazard detected",
                boundingBox = detection.boundingBox,
                confidence = detection.confidence,
                oshaCode = "1926.501(b)(1)",
                recommendations = listOf(
                    "Install guardrail system immediately",
                    "Provide personal fall arrest system",
                    "Stop work until fall protection is in place"
                ),
                immediateAction = "STOP WORK - Install fall protection"
            )

            "electrical-panel-open", "exposed-wiring" -> Hazard(
                id = uuid4().toString(),
                type = HazardType.ELECTRICAL_HAZARD,
                severity = Severity.HIGH,
                description = "Electrical hazard - open panel or exposed wiring",
                boundingBox = detection.boundingBox,
                confidence = detection.confidence,
                oshaCode = "1926.405",
                recommendations = listOf(
                    "Close and secure electrical panels",
                    "Implement lockout/tagout procedures",
                    "Use qualified electricians only"
                )
            )

            "scaffold-unsafe", "scaffold-no-guardrail" -> Hazard(
                id = uuid4().toString(),
                type = HazardType.SCAFFOLDING_UNSAFE,
                severity = Severity.HIGH,
                description = "Unsafe scaffolding configuration detected",
                boundingBox = detection.boundingBox,
                confidence = detection.confidence,
                oshaCode = "1926.451",
                recommendations = listOf(
                    "Install proper guardrails and toeboards",
                    "Ensure scaffold is level and stable",
                    "Inspect daily before use"
                )
            )

            "ladder-unsafe", "ladder-improper-angle" -> Hazard(
                id = uuid4().toString(),
                type = HazardType.FALL_PROTECTION,
                severity = Severity.MEDIUM,
                description = "Ladder safety issue detected",
                boundingBox = detection.boundingBox,
                confidence = detection.confidence,
                oshaCode = "1926.1053",
                recommendations = listOf(
                    "Position ladder at correct 4:1 ratio",
                    "Secure ladder at top and bottom",
                    "Maintain 3-point contact"
                )
            )

            // Ignore non-hazard detections (e.g., "person", "hardhat", "cone")
            else -> null
        }
    }

    /**
     * Create mock YOLO detections for development/testing.
     */
    private fun createMockYOLODetections(workType: WorkType): List<YOLODetection> {
        val detections = mutableListOf<YOLODetection>()

        // Add work-type specific mock detections
        when (workType) {
            WorkType.FALL_PROTECTION, WorkType.ROOFING, WorkType.SCAFFOLDING -> {
                detections.add(YOLODetection(
                    classId = 1,
                    className = "unguarded-edge",
                    confidence = 0.87f,
                    boundingBox = BoundingBox(left = 0.1f, top = 0.3f, width = 0.6f, height = 0.4f)
                ))
            }
            WorkType.ELECTRICAL -> {
                detections.add(YOLODetection(
                    classId = 2,
                    className = "electrical-panel-open",
                    confidence = 0.82f,
                    boundingBox = BoundingBox(left = 0.4f, top = 0.2f, width = 0.3f, height = 0.35f)
                ))
            }
            else -> {
                // General construction - PPE violation
                detections.add(YOLODetection(
                    classId = 0,
                    className = "person-no-hardhat",
                    confidence = 0.79f,
                    boundingBox = BoundingBox(left = 0.25f, top = 0.15f, width = 0.2f, height = 0.5f)
                ))
            }
        }

        return detections
    }

    private fun inferPPEStatus(detections: List<YOLODetection>): PPEStatus {
        // Basic PPE inference from YOLO detections
        val hasHardhatViolation = detections.any { it.className.contains("no-hardhat") }
        val hasVestViolation = detections.any { it.className.contains("no-vest") }

        return PPEStatus(
            hardHat = PPEItem(
                status = if (hasHardhatViolation) PPEItemStatus.MISSING else PPEItemStatus.UNKNOWN,
                confidence = if (hasHardhatViolation) 0.8f else 0.5f
            ),
            safetyVest = PPEItem(
                status = if (hasVestViolation) PPEItemStatus.MISSING else PPEItemStatus.UNKNOWN,
                confidence = if (hasVestViolation) 0.75f else 0.5f
            ),
            safetyBoots = PPEItem(PPEItemStatus.UNKNOWN, 0.3f),
            safetyGlasses = PPEItem(PPEItemStatus.UNKNOWN, 0.3f),
            fallProtection = PPEItem(PPEItemStatus.UNKNOWN, 0.3f),
            respirator = PPEItem(PPEItemStatus.UNKNOWN, 0.3f),
            overallCompliance = if (hasHardhatViolation || hasVestViolation) 0.4f else 0.6f
        )
    }

    private fun generateYOLORecommendations(hazards: List<Hazard>, workType: WorkType): List<String> {
        val recommendations = mutableListOf<String>()

        if (hazards.isEmpty()) {
            recommendations.add("No major hazards detected by YOLO object detection")
            recommendations.add("Continue following safety protocols")
        } else {
            recommendations.add("${hazards.size} safety issue(s) detected - review immediately")

            if (hazards.any { it.severity == Severity.CRITICAL }) {
                recommendations.add("CRITICAL HAZARD: Stop work and address critical issues")
            }

            recommendations.add("⚠️ YOLO detection provides visual identification only")
            recommendations.add("Supplement with manual safety inspection for full compliance")
        }

        return recommendations
    }

    private fun generateOSHAViolations(hazards: List<Hazard>): List<OSHAViolation> {
        return hazards.mapNotNull { hazard ->
            hazard.oshaCode?.let { code ->
                OSHAViolation(
                    code = code,
                    title = getOSHATitle(code),
                    description = hazard.description,
                    severity = hazard.severity,
                    correctiveAction = hazard.recommendations.firstOrNull()
                        ?: "Address safety violation immediately"
                )
            }
        }
    }

    private fun getOSHATitle(code: String): String {
        return when {
            code.startsWith("1926.95") -> "Personal Protective Equipment"
            code.startsWith("1926.501") -> "Fall Protection Requirements"
            code.startsWith("1926.405") -> "Electrical Safety Requirements"
            code.startsWith("1926.451") -> "Scaffolding Requirements"
            code.startsWith("1926.1053") -> "Ladder Safety Requirements"
            else -> "OSHA Safety Requirement"
        }
    }

    private fun calculateOverallConfidence(detections: List<YOLODetection>): Float {
        return if (detections.isEmpty()) 0.5f
        else detections.map { it.confidence }.average().toFloat()
    }
    
    private fun createMockYOLOAnalysis(workType: WorkType, startTime: Long): SafetyAnalysis {
        // Basic hazard detection - limited compared to multimodal models
        val hazards = mutableListOf<Hazard>()
        
        // YOLO can detect objects but has limited contextual understanding
        hazards.add(
            Hazard(
                id = uuid4().toString(),
                type = HazardType.PPE_VIOLATION,
                severity = Severity.MEDIUM,
                description = "Person detected without visible hard hat",
                confidence = 0.73f,
                recommendations = listOf(
                    "Verify all workers are wearing proper hard hats",
                    "⚠️ Basic detection only - manual verification recommended"
                )
            )
        )
        
        // Limited detection for specific work types
        when (workType) {
            WorkType.FALL_PROTECTION, WorkType.ROOFING, WorkType.SCAFFOLDING -> {
                hazards.add(
                    Hazard(
                        id = uuid4().toString(),
                        type = HazardType.FALL_PROTECTION,
                        severity = Severity.HIGH,
                        description = "Elevated work area detected - fall protection status unclear",
                        confidence = 0.68f,
                        recommendations = listOf(
                            "Manual inspection required for fall protection compliance",
                            "Basic detection cannot assess fall protection equipment"
                        )
                    )
                )
            }
            WorkType.ELECTRICAL -> {
                hazards.add(
                    Hazard(
                        id = uuid4().toString(),
                        type = HazardType.ELECTRICAL_HAZARD,
                        severity = Severity.MEDIUM,
                        description = "Electrical equipment detected in work area",
                        confidence = 0.65f,
                        recommendations = listOf(
                            "Verify electrical safety procedures are followed",
                            "Advanced analysis needed for electrical hazard assessment"
                        )
                    )
                )
            }
            else -> {
                // Basic generic detection
            }
        }
        
        return SafetyAnalysis(
            id = uuid4().toString(),
            timestamp = System.currentTimeMillis(),
            analysisType = AnalysisType.LOCAL_YOLO_FALLBACK,
            workType = workType,
            hazards = hazards,
            ppeStatus = PPEStatus(
                hardHat = PPEItem(PPEItemStatus.UNKNOWN, 0.60f),
                safetyVest = PPEItem(PPEItemStatus.UNKNOWN, 0.55f),
                safetyBoots = PPEItem(PPEItemStatus.UNKNOWN, 0.45f),
                safetyGlasses = PPEItem(PPEItemStatus.UNKNOWN, 0.40f),
                fallProtection = PPEItem(PPEItemStatus.UNKNOWN, 0.35f),
                respirator = PPEItem(PPEItemStatus.UNKNOWN, 0.30f),
                overallCompliance = 0.45f // Lower confidence with YOLO-only
            ),
            recommendations = listOf(
                "⚠️ LIMITED ANALYSIS - Basic object detection only",
                "Advanced AI services unavailable - enhanced analysis recommended",
                "Manual safety inspection strongly advised",
                "Consider improving network connectivity for comprehensive analysis"
            ),
            overallRiskLevel = RiskLevel.MODERATE, // Conservative assessment
            confidence = 0.62f, // Lower confidence for basic detection
            processingTimeMs = System.currentTimeMillis() - startTime,
            oshaViolations = emptyList() // YOLO cannot reliably determine OSHA violations
        )
    }
    
    // TODO: Implement actual YOLO preprocessing and inference functions
    /*
    private fun preprocessForYOLO(imageData: ByteArray): FloatArray {
        // Resize to 640x640, normalize, convert to tensor format
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        val resized = Bitmap.createScaledBitmap(bitmap, 640, 640, true)
        
        val pixels = IntArray(640 * 640)
        resized.getPixels(pixels, 0, 640, 0, 0, 640, 640)
        
        val floatArray = FloatArray(3 * 640 * 640)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            floatArray[i] = ((pixel shr 16) and 0xFF) / 255f        // R
            floatArray[i + 640 * 640] = ((pixel shr 8) and 0xFF) / 255f     // G  
            floatArray[i + 2 * 640 * 640] = (pixel and 0xFF) / 255f         // B
        }
        
        return floatArray
    }
    
    private fun parseYOLOResults(results: OrtSession.Result): List<YOLODetection> {
        // Parse YOLO output format: [batch, boxes, classes + 4 coords + confidence]
        val output = results.get(0) as OnnxTensor
        val outputArray = output.floatBuffer.array()
        
        val detections = mutableListOf<YOLODetection>()
        val numBoxes = outputArray.size / 85 // 80 classes + 4 coords + 1 confidence
        
        for (i in 0 until numBoxes) {
            val baseIndex = i * 85
            val confidence = outputArray[baseIndex + 4]
            
            if (confidence > 0.5f) { // Confidence threshold
                val x = outputArray[baseIndex]
                val y = outputArray[baseIndex + 1] 
                val w = outputArray[baseIndex + 2]
                val h = outputArray[baseIndex + 3]
                
                // Find highest class probability
                var maxClassScore = 0f
                var classId = 0
                for (j in 5 until 85) {
                    if (outputArray[baseIndex + j] > maxClassScore) {
                        maxClassScore = outputArray[baseIndex + j]
                        classId = j - 5
                    }
                }
                
                detections.add(
                    YOLODetection(
                        classId = classId,
                        confidence = confidence * maxClassScore,
                        boundingBox = BoundingBox(x - w/2, y - h/2, w, h)
                    )
                )
            }
        }
        
        return detections
    }
    */
}

// TODO: Add YOLO detection data classes
/*
data class YOLODetection(
    val classId: Int,
    val confidence: Float,
    val boundingBox: BoundingBox
)
*/