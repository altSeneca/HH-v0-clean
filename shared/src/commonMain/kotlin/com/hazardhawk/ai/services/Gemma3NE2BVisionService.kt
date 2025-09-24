package com.hazardhawk.ai.services

import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.ai.loaders.GemmaModelLoader
import com.hazardhawk.ai.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.uuid.uuid4

/**
 * Gemma 3N E2B multimodal vision service for local, privacy-first AI analysis.
 * Uses existing ONNX models in /models/gemma3n_e2b_onnx/
 */
class Gemma3NE2BVisionService(
    private val modelLoader: GemmaModelLoader
) : AIPhotoAnalyzer {
    
    private var isInitialized = false
    
    override val analyzerName = "Gemma 3N E2B Multimodal"
    override val priority = 100 // Highest priority - local and private
    
    override val analysisCapabilities = setOf(
        AnalysisCapability.MULTIMODAL_VISION,
        AnalysisCapability.PPE_DETECTION,
        AnalysisCapability.HAZARD_IDENTIFICATION,
        AnalysisCapability.OSHA_COMPLIANCE,
        AnalysisCapability.OFFLINE_ANALYSIS,
        AnalysisCapability.DOCUMENT_GENERATION
    )
    
    override val isAvailable: Boolean
        get() = isInitialized && modelLoader.isAvailable
    
    override suspend fun configure(apiKey: String?): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val modelsLoaded = modelLoader.loadModels(
                visionEncoderPath = "/models/gemma3n_e2b_onnx/vision_encoder.onnx",
                textDecoderPath = "/models/gemma3n_e2b_onnx/decoder_model_merged_q4.onnx",
                tokenizerPath = "/models/gemma3n_e2b_onnx/tokenizer.json",
                configPath = "/models/gemma3n_e2b_onnx/config.json"
            )
            
            if (modelsLoaded) {
                isInitialized = true
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to load Gemma 3N E2B models"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gemma 3N E2B configuration failed: ${e.message}", e))
        }
    }
    
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType
    ): Result<SafetyAnalysis> = withContext(Dispatchers.Default) {
        
        if (!isAvailable) {
            return@withContext Result.failure(Exception("Gemma 3N E2B models not available"))
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Phase 1: Vision encoding
            val imageFeatures = modelLoader.encodeImage(imageData)
                ?: return@withContext Result.failure(Exception("Failed to encode image"))
            
            // Phase 2: Generate construction safety prompt
            val prompt = buildConstructionSafetyPrompt(workType, imageFeatures)
            
            // Phase 3: Multimodal text generation
            val analysisText = modelLoader.generateText(
                prompt = prompt,
                imageContext = imageFeatures,
                maxTokens = 750,
                temperature = 0.3f // Lower temperature for more consistent safety analysis
            ) ?: return@withContext Result.failure(Exception("Failed to generate analysis"))
            
            // Phase 4: Parse structured safety analysis
            val analysis = parseGemmaSafetyAnalysis(
                analysisText = analysisText,
                workType = workType,
                processingTime = System.currentTimeMillis() - startTime
            )
            
            Result.success(analysis)
            
        } catch (e: Exception) {
            Result.failure(Exception("Gemma 3N E2B analysis failed: ${e.message}", e))
        }
    }
    
    private fun buildConstructionSafetyPrompt(
        workType: WorkType,
        imageFeatures: FloatArray
    ): String {
        return """
        <image>
        You are analyzing a construction site photo for safety hazards. The image features are encoded.
        
        Work Type: ${workType.name}
        Task: Identify all safety hazards, PPE compliance, and OSHA violations.
        
        Provide your analysis in the following JSON format:
        {
            "hazards": [
                {
                    "type": "FALL_PROTECTION|PPE_VIOLATION|ELECTRICAL_HAZARD|MECHANICAL_HAZARD|etc",
                    "severity": "LOW|MEDIUM|HIGH|CRITICAL",
                    "description": "Detailed description of the hazard",
                    "oshaCode": "1926.501|1926.95|etc (if applicable)",
                    "confidence": 0.85,
                    "recommendations": ["Specific corrective action"],
                    "immediateAction": "Immediate action required (if critical)"
                }
            ],
            "ppeStatus": {
                "hardHat": {"status": "PRESENT|MISSING|UNKNOWN", "confidence": 0.90},
                "safetyVest": {"status": "PRESENT|MISSING|UNKNOWN", "confidence": 0.85},
                "safetyBoots": {"status": "PRESENT|MISSING|UNKNOWN", "confidence": 0.80},
                "safetyGlasses": {"status": "PRESENT|MISSING|UNKNOWN", "confidence": 0.75},
                "fallProtection": {"status": "PRESENT|MISSING|UNKNOWN", "confidence": 0.95},
                "respirator": {"status": "PRESENT|MISSING|UNKNOWN", "confidence": 0.70}
            },
            "recommendations": [
                "Prioritized list of safety improvements"
            ],
            "overallRiskLevel": "MINIMAL|LOW|MODERATE|HIGH|SEVERE",
            "confidence": 0.88
        }
        
        Focus specifically on construction safety for ${workType.name}.
        Be thorough but concise. Prioritize worker safety.
        </image>
        """.trimIndent()
    }
    
    private fun parseGemmaSafetyAnalysis(
        analysisText: String,
        workType: WorkType,
        processingTime: Long
    ): SafetyAnalysis {
        return try {
            // Extract JSON from the response (Gemma might include explanatory text)
            val jsonStart = analysisText.indexOf("{")
            val jsonEnd = analysisText.lastIndexOf("}") + 1
            
            if (jsonStart == -1 || jsonEnd == 0) {
                // Fallback to basic analysis if JSON parsing fails
                return createFallbackAnalysis(analysisText, workType, processingTime)
            }
            
            val jsonText = analysisText.substring(jsonStart, jsonEnd)
            val json = Json { ignoreUnknownKeys = true }
            
            // Parse the JSON response into our safety analysis structure
            val response = json.decodeFromString<GemmaAnalysisResponse>(jsonText)
            
            SafetyAnalysis(
                id = uuid4().toString(),
                timestamp = System.currentTimeMillis(),
                analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
                workType = workType,
                hazards = response.hazards.map { hazard ->
                    Hazard(
                        id = uuid4().toString(),
                        type = parseHazardType(hazard.type),
                        severity = parseSeverity(hazard.severity),
                        description = hazard.description,
                        oshaCode = hazard.oshaCode,
                        confidence = hazard.confidence,
                        recommendations = hazard.recommendations,
                        immediateAction = hazard.immediateAction
                    )
                },
                ppeStatus = PPEStatus(
                    hardHat = PPEItem(parsePPEStatus(response.ppeStatus.hardHat.status), response.ppeStatus.hardHat.confidence),
                    safetyVest = PPEItem(parsePPEStatus(response.ppeStatus.safetyVest.status), response.ppeStatus.safetyVest.confidence),
                    safetyBoots = PPEItem(parsePPEStatus(response.ppeStatus.safetyBoots.status), response.ppeStatus.safetyBoots.confidence),
                    safetyGlasses = PPEItem(parsePPEStatus(response.ppeStatus.safetyGlasses.status), response.ppeStatus.safetyGlasses.confidence),
                    fallProtection = PPEItem(parsePPEStatus(response.ppeStatus.fallProtection.status), response.ppeStatus.fallProtection.confidence),
                    respirator = PPEItem(parsePPEStatus(response.ppeStatus.respirator.status), response.ppeStatus.respirator.confidence),
                    overallCompliance = calculatePPECompliance(response.ppeStatus)
                ),
                recommendations = response.recommendations,
                overallRiskLevel = parseRiskLevel(response.overallRiskLevel),
                confidence = response.confidence,
                processingTimeMs = processingTime,
                oshaViolations = generateOSHAViolations(response.hazards)
            )
            
        } catch (e: Exception) {
            // Fallback to text analysis if JSON parsing fails
            createFallbackAnalysis(analysisText, workType, processingTime)
        }
    }
    
    private fun createFallbackAnalysis(
        analysisText: String,
        workType: WorkType,
        processingTime: Long
    ): SafetyAnalysis {
        // Basic text analysis fallback when JSON parsing fails
        val hazards = mutableListOf<Hazard>()
        
        // Look for key safety terms in the response
        if (analysisText.contains("fall", ignoreCase = true) || 
            analysisText.contains("height", ignoreCase = true)) {
            hazards.add(
                Hazard(
                    id = uuid4().toString(),
                    type = HazardType.FALL_PROTECTION,
                    severity = Severity.HIGH,
                    description = "Potential fall hazard identified",
                    confidence = 0.7f,
                    recommendations = listOf("Verify fall protection equipment and procedures")
                )
            )
        }
        
        if (analysisText.contains("ppe", ignoreCase = true) || 
            analysisText.contains("helmet", ignoreCase = true) ||
            analysisText.contains("hard hat", ignoreCase = true)) {
            hazards.add(
                Hazard(
                    id = uuid4().toString(),
                    type = HazardType.PPE_VIOLATION,
                    severity = Severity.MEDIUM,
                    description = "PPE compliance issue detected",
                    confidence = 0.6f,
                    recommendations = listOf("Review PPE requirements and compliance")
                )
            )
        }
        
        return SafetyAnalysis(
            id = uuid4().toString(),
            timestamp = System.currentTimeMillis(),
            analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
            workType = workType,
            hazards = hazards,
            ppeStatus = createDefaultPPEStatus(),
            recommendations = listOf("Manual review recommended - automated parsing incomplete"),
            overallRiskLevel = if (hazards.isNotEmpty()) RiskLevel.MODERATE else RiskLevel.LOW,
            confidence = 0.5f,
            processingTimeMs = processingTime
        )
    }
    
    // Helper parsing functions
    private fun parseHazardType(type: String): HazardType {
        return when (type.uppercase()) {
            "FALL_PROTECTION" -> HazardType.FALL_PROTECTION
            "PPE_VIOLATION" -> HazardType.PPE_VIOLATION
            "ELECTRICAL_HAZARD" -> HazardType.ELECTRICAL_HAZARD
            "MECHANICAL_HAZARD" -> HazardType.MECHANICAL_HAZARD
            else -> HazardType.GENERAL_CONSTRUCTION
        }
    }
    
    private fun parseSeverity(severity: String): Severity {
        return when (severity.uppercase()) {
            "CRITICAL" -> Severity.CRITICAL
            "HIGH" -> Severity.HIGH
            "MEDIUM" -> Severity.MEDIUM
            "LOW" -> Severity.LOW
            else -> Severity.MEDIUM
        }
    }
    
    private fun parsePPEStatus(status: String): PPEItemStatus {
        return when (status.uppercase()) {
            "PRESENT" -> PPEItemStatus.PRESENT
            "MISSING" -> PPEItemStatus.MISSING
            "INCORRECT" -> PPEItemStatus.INCORRECT
            else -> PPEItemStatus.UNKNOWN
        }
    }
    
    private fun parseRiskLevel(level: String): RiskLevel {
        return when (level.uppercase()) {
            "SEVERE" -> RiskLevel.SEVERE
            "HIGH" -> RiskLevel.HIGH
            "MODERATE" -> RiskLevel.MODERATE
            "LOW" -> RiskLevel.LOW
            "MINIMAL" -> RiskLevel.MINIMAL
            else -> RiskLevel.MODERATE
        }
    }
    
    private fun calculatePPECompliance(ppeStatus: GemmaPPEStatus): Float {
        val items = listOf(
            ppeStatus.hardHat,
            ppeStatus.safetyVest,
            ppeStatus.safetyBoots,
            ppeStatus.safetyGlasses,
            ppeStatus.fallProtection
        )
        
        val compliantCount = items.count { 
            parsePPEStatus(it.status) == PPEItemStatus.PRESENT 
        }
        
        return compliantCount.toFloat() / items.size
    }
    
    private fun createDefaultPPEStatus(): PPEStatus {
        return PPEStatus(
            hardHat = PPEItem(PPEItemStatus.UNKNOWN, 0.0f),
            safetyVest = PPEItem(PPEItemStatus.UNKNOWN, 0.0f),
            safetyBoots = PPEItem(PPEItemStatus.UNKNOWN, 0.0f),
            safetyGlasses = PPEItem(PPEItemStatus.UNKNOWN, 0.0f),
            fallProtection = PPEItem(PPEItemStatus.UNKNOWN, 0.0f),
            respirator = PPEItem(PPEItemStatus.UNKNOWN, 0.0f),
            overallCompliance = 0.0f
        )
    }
    
    private fun generateOSHAViolations(hazards: List<GemmaHazard>): List<OSHAViolation> {
        return hazards.mapNotNull { hazard ->
            hazard.oshaCode?.let { code ->
                OSHAViolation(
                    code = code,
                    title = getOSHATitle(code),
                    description = hazard.description,
                    severity = parseSeverity(hazard.severity),
                    correctiveAction = hazard.recommendations.firstOrNull() ?: "Review OSHA requirements"
                )
            }
        }
    }
    
    private fun getOSHATitle(code: String): String {
        return when (code) {
            "1926.501" -> "Fall Protection Requirements"
            "1926.95" -> "Personal Protective Equipment"
            "1926.405" -> "Electrical Safety Requirements"
            "1926.452" -> "Scaffolding Requirements"
            else -> "OSHA Safety Requirement"
        }
    }
}

// Data classes for parsing Gemma responses
@kotlinx.serialization.Serializable
private data class GemmaAnalysisResponse(
    val hazards: List<GemmaHazard>,
    val ppeStatus: GemmaPPEStatus,
    val recommendations: List<String>,
    val overallRiskLevel: String,
    val confidence: Float
)

@kotlinx.serialization.Serializable
private data class GemmaHazard(
    val type: String,
    val severity: String,
    val description: String,
    val oshaCode: String? = null,
    val confidence: Float,
    val recommendations: List<String> = emptyList(),
    val immediateAction: String? = null
)

@kotlinx.serialization.Serializable
private data class GemmaPPEStatus(
    val hardHat: GemmaPPEItem,
    val safetyVest: GemmaPPEItem,
    val safetyBoots: GemmaPPEItem,
    val safetyGlasses: GemmaPPEItem,
    val fallProtection: GemmaPPEItem,
    val respirator: GemmaPPEItem
)

@kotlinx.serialization.Serializable
private data class GemmaPPEItem(
    val status: String,
    val confidence: Float
)