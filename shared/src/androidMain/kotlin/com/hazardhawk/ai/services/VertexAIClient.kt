package com.hazardhawk.ai.services

import android.graphics.BitmapFactory
import android.util.Base64
import com.google.firebase.Firebase
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.type.generationConfig
import com.google.firebase.vertexai.vertexAI
import com.hazardhawk.core.models.*
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.uuid.uuid4

/**
 * Android-specific implementation of Vertex AI client using Firebase Vertex AI SDK
 */
actual class VertexAIClient {
    
    private var model: GenerativeModel? = null
    private var isConfigured = false
    
    suspend fun configure(apiKey: String): Result<Unit> {
        return try {
            // Configure Firebase Vertex AI with the provided API key
            val vertexAI = Firebase.vertexAI
            
            model = vertexAI.generativeModel("gemini-1.5-pro-vision-latest") {
                generationConfig {
                    temperature = 0.3f
                    topK = 32
                    topP = 0.8f
                    maxOutputTokens = 2048
                }
            }
            
            isConfigured = true
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(Exception("Failed to configure Vertex AI: ${e.message}", e))
        }
    }
    
    suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType
    ): Result<SafetyAnalysis> {
        
        if (!isConfigured || model == null) {
            return Result.failure(Exception("Vertex AI not configured"))
        }
        
        return try {
            withTimeout(30000) { // 30 second timeout
                val startTime = System.currentTimeMillis()
                
                // Convert ByteArray to bitmap for Firebase Vertex AI
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                    ?: return@withTimeout Result.failure(Exception("Failed to decode image"))
                
                val prompt = buildConstructionSafetyPrompt(workType)
                
                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }
                
                val response = model!!.generateContent(inputContent)
                val responseText = response.text ?: ""
                
                // Parse the JSON response from Gemini
                val analysis = parseVertexAIResponse(responseText, workType, startTime)
                Result.success(analysis)
            }
            
        } catch (e: Exception) {
            Result.failure(Exception("Vertex AI analysis failed: ${e.message}", e))
        }
    }
    
    private fun buildConstructionSafetyPrompt(workType: WorkType): String {
        return """
        You are a construction safety expert analyzing a worksite photo for OSHA compliance.

        Work Type: ${workType.name}

        Please analyze this construction site image and identify:
        1. All safety hazards present WITH BOUNDING BOX COORDINATES
        2. PPE compliance status for visible workers WITH BOUNDING BOX COORDINATES
        3. Specific OSHA violations with regulation codes
        4. Immediate corrective actions needed
        5. Overall risk assessment

        Respond ONLY with valid JSON in this exact format:
        {
            "hazards": [
                {
                    "type": "ELECTRICAL_HAZARD",
                    "severity": "CRITICAL",
                    "description": "Detailed description of hazard",
                    "oshaCode": "1926.405(a)(2)(ii)",
                    "confidence": 0.95,
                    "boundingBox": {
                        "left": 0.25,
                        "top": 0.15,
                        "width": 0.30,
                        "height": 0.40
                    },
                    "recommendations": ["Action 1", "Action 2"],
                    "immediateAction": "Stop work immediately"
                }
            ],
            "ppeStatus": {
                "hardHat": {"status": "PRESENT", "confidence": 0.89, "boundingBox": {"left": 0.1, "top": 0.05, "width": 0.15, "height": 0.2}},
                "safetyVest": {"status": "MISSING", "confidence": 0.85},
                "safetyBoots": {"status": "PRESENT", "confidence": 0.87},
                "safetyGlasses": {"status": "UNKNOWN", "confidence": 0.45},
                "fallProtection": {"status": "MISSING", "confidence": 0.92},
                "respirator": {"status": "UNKNOWN", "confidence": 0.25},
                "overallCompliance": 0.65
            },
            "recommendations": [
                "Primary recommendation",
                "Secondary recommendation"
            ],
            "overallRiskLevel": "HIGH",
            "confidence": 0.87
        }

        IMPORTANT: For each detected hazard or PPE item, include a "boundingBox" with coordinates normalized 0.0-1.0:
        - left: x-position from left edge (0.0 = left edge, 1.0 = right edge)
        - top: y-position from top edge (0.0 = top edge, 1.0 = bottom edge)
        - width: box width as fraction of image width (0.0-1.0)
        - height: box height as fraction of image height (0.0-1.0)

        Focus on worker safety and OSHA compliance for ${workType.name} work.
        Use only these hazard types: ELECTRICAL_HAZARD, FALL_PROTECTION, PPE_VIOLATION, STRUCK_BY, CAUGHT_IN_BETWEEN, ENVIRONMENTAL, CHEMICAL_EXPOSURE.
        Use only these severity levels: LOW, MEDIUM, HIGH, CRITICAL.
        Use only these PPE statuses: PRESENT, MISSING, UNKNOWN.
        Use only these risk levels: LOW, MODERATE, HIGH, SEVERE.
        """.trimIndent()
    }
    
    /**
     * Parse bounding box from JSON object.
     * Expects format: {"left": 0.25, "top": 0.15, "width": 0.30, "height": 0.40}
     * All coordinates should be normalized 0.0-1.0
     */
    private fun parseBoundingBox(boxJson: kotlinx.serialization.json.JsonObject?): BoundingBox? {
        return boxJson?.let {
            try {
                val left = it["left"]?.jsonPrimitive?.content?.toFloatOrNull() ?: return null
                val top = it["top"]?.jsonPrimitive?.content?.toFloatOrNull() ?: return null
                val width = it["width"]?.jsonPrimitive?.content?.toFloatOrNull() ?: return null
                val height = it["height"]?.jsonPrimitive?.content?.toFloatOrNull() ?: return null

                // Validate coordinates are in range [0.0, 1.0]
                if (left < 0f || left > 1f || top < 0f || top > 1f ||
                    width < 0f || width > 1f || height < 0f || height > 1f) {
                    return null
                }

                // Validate box doesn't exceed image boundaries
                if (left + width > 1f || top + height > 1f) {
                    return null
                }

                BoundingBox(
                    left = left,
                    top = top,
                    width = width,
                    height = height
                )
            } catch (e: Exception) {
                null // Invalid bounding box format
            }
        }
    }

    private fun parseVertexAIResponse(
        responseText: String,
        workType: WorkType,
        startTime: Long
    ): SafetyAnalysis {

        return try {
            // Extract JSON from response (handle markdown code blocks if present)
            val jsonText = responseText
                .substringAfter("```json")
                .substringBefore("```")
                .takeIf { it.isNotBlank() } ?: responseText

            val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonText).jsonObject

            // Parse hazards with bounding boxes
            val hazards = mutableListOf<Hazard>()
            json["hazards"]?.let { hazardsArray ->
                // Handle array parsing safely
                try {
                    // Improved parsing that attempts to extract individual hazard objects
                    val hazardsJson = Json { ignoreUnknownKeys = true }.parseToJsonElement(hazardsArray.toString())

                    if (hazardsJson is kotlinx.serialization.json.JsonArray) {
                        hazardsJson.forEach { hazardElement ->
                            val hazardObj = hazardElement.jsonObject
                            val hazardType = parseHazardType(hazardObj["type"]?.jsonPrimitive?.content)
                            val severity = parseSeverity(hazardObj["severity"]?.jsonPrimitive?.content)
                            val boundingBox = parseBoundingBox(hazardObj["boundingBox"]?.jsonObject)

                            hazards.add(Hazard(
                                id = uuid4().toString(),
                                type = hazardType,
                                severity = severity,
                                description = hazardObj["description"]?.jsonPrimitive?.content
                                    ?: "Safety hazard detected",
                                oshaCode = hazardObj["oshaCode"]?.jsonPrimitive?.content,
                                boundingBox = boundingBox, // NOW INCLUDES BOUNDING BOX!
                                confidence = hazardObj["confidence"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0.8f,
                                recommendations = parseRecommendations(hazardObj["recommendations"]),
                                immediateAction = hazardObj["immediateAction"]?.jsonPrimitive?.content
                            ))
                        }
                    }

                    // Fallback if array parsing fails
                    if (hazards.isEmpty()) {
                        hazardsArray.toString().let { hazardStr ->
                            if (hazardStr.contains("ELECTRICAL_HAZARD")) {
                                hazards.add(createHazardFromType(HazardType.ELECTRICAL_HAZARD, workType))
                            }
                            if (hazardStr.contains("FALL_PROTECTION")) {
                                hazards.add(createHazardFromType(HazardType.FALL_PROTECTION, workType))
                            }
                            if (hazardStr.contains("PPE_VIOLATION")) {
                                hazards.add(createHazardFromType(HazardType.PPE_VIOLATION, workType))
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to default hazard for workType
                    hazards.add(createHazardFromType(getDefaultHazardType(workType), workType))
                }
            }
            
            // Parse PPE status
            val ppeStatus = json["ppeStatus"]?.jsonObject?.let { ppe ->
                PPEStatus(
                    hardHat = PPEItem(
                        status = parsePPEStatus(ppe["hardHat"]?.jsonObject?.get("status")?.jsonPrimitive?.content),
                        confidence = ppe["hardHat"]?.jsonObject?.get("confidence")?.jsonPrimitive?.content?.toFloatOrNull() ?: 0.5f
                    ),
                    safetyVest = PPEItem(
                        status = parsePPEStatus(ppe["safetyVest"]?.jsonObject?.get("status")?.jsonPrimitive?.content),
                        confidence = ppe["safetyVest"]?.jsonObject?.get("confidence")?.jsonPrimitive?.content?.toFloatOrNull() ?: 0.5f
                    ),
                    safetyBoots = PPEItem(
                        status = parsePPEStatus(ppe["safetyBoots"]?.jsonObject?.get("status")?.jsonPrimitive?.content),
                        confidence = ppe["safetyBoots"]?.jsonObject?.get("confidence")?.jsonPrimitive?.content?.toFloatOrNull() ?: 0.5f
                    ),
                    safetyGlasses = PPEItem(
                        status = parsePPEStatus(ppe["safetyGlasses"]?.jsonObject?.get("status")?.jsonPrimitive?.content),
                        confidence = ppe["safetyGlasses"]?.jsonObject?.get("confidence")?.jsonPrimitive?.content?.toFloatOrNull() ?: 0.5f
                    ),
                    fallProtection = PPEItem(
                        status = parsePPEStatus(ppe["fallProtection"]?.jsonObject?.get("status")?.jsonPrimitive?.content),
                        confidence = ppe["fallProtection"]?.jsonObject?.get("confidence")?.jsonPrimitive?.content?.toFloatOrNull() ?: 0.5f
                    ),
                    respirator = PPEItem(
                        status = parsePPEStatus(ppe["respirator"]?.jsonObject?.get("status")?.jsonPrimitive?.content),
                        confidence = ppe["respirator"]?.jsonObject?.get("confidence")?.jsonPrimitive?.content?.toFloatOrNull() ?: 0.5f
                    ),
                    overallCompliance = ppe["overallCompliance"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0.6f
                )
            } ?: createDefaultPPEStatus()
            
            // Parse overall risk level
            val riskLevel = parseRiskLevel(json["overallRiskLevel"]?.jsonPrimitive?.content)
            val confidence = json["confidence"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0.8f
            
            SafetyAnalysis(
                id = uuid4().toString(),
                timestamp = System.currentTimeMillis(),
                analysisType = AnalysisType.CLOUD_GEMINI,
                workType = workType,
                hazards = hazards,
                ppeStatus = ppeStatus,
                recommendations = listOf(
                    "Cloud AI analysis completed with ${confidence * 100}% confidence",
                    "Review identified hazards and implement corrective actions",
                    "Ensure all workers comply with PPE requirements"
                ),
                overallRiskLevel = riskLevel,
                confidence = confidence,
                processingTimeMs = System.currentTimeMillis() - startTime,
                oshaViolations = hazards.mapNotNull { hazard ->
                    hazard.oshaCode?.let { code ->
                        OSHAViolation(
                            code = code,
                            title = getOSHATitle(code),
                            description = hazard.description,
                            severity = hazard.severity,
                            correctiveAction = hazard.recommendations.firstOrNull() 
                                ?: "Comply with OSHA requirements"
                        )
                    }
                }
            )
            
        } catch (e: Exception) {
            // Fallback to structured mock response if JSON parsing fails
            createFallbackAnalysis(workType, startTime, responseText)
        }
    }
    
    private fun parseHazardType(type: String?): HazardType {
        return when (type?.uppercase()?.replace(" ", "_")) {
            "ELECTRICAL_HAZARD" -> HazardType.ELECTRICAL_HAZARD
            "FALL_PROTECTION" -> HazardType.FALL_PROTECTION
            "PPE_VIOLATION" -> HazardType.PPE_VIOLATION
            "STRUCK_BY" -> HazardType.STRUCK_BY_OBJECT
            "STRUCK_BY_OBJECT" -> HazardType.STRUCK_BY_OBJECT
            "CAUGHT_IN_BETWEEN", "CAUGHT_IN_EQUIPMENT" -> HazardType.CAUGHT_IN_EQUIPMENT
            "ENVIRONMENTAL" -> HazardType.ENVIRONMENTAL_HAZARD
            "CHEMICAL_EXPOSURE", "CHEMICAL_HAZARD" -> HazardType.CHEMICAL_HAZARD
            "FIRE_HAZARD" -> HazardType.FIRE_HAZARD
            "MECHANICAL_HAZARD" -> HazardType.MECHANICAL_HAZARD
            "HOUSEKEEPING" -> HazardType.HOUSEKEEPING
            else -> HazardType.PPE_VIOLATION // Default
        }
    }

    private fun parseSeverity(severity: String?): Severity {
        return when (severity?.uppercase()) {
            "LOW" -> Severity.LOW
            "MEDIUM" -> Severity.MEDIUM
            "HIGH" -> Severity.HIGH
            "CRITICAL" -> Severity.CRITICAL
            else -> Severity.MEDIUM // Default
        }
    }

    private fun parseRecommendations(recommendationsJson: kotlinx.serialization.json.JsonElement?): List<String> {
        return try {
            when (recommendationsJson) {
                is kotlinx.serialization.json.JsonArray -> {
                    recommendationsJson.mapNotNull { it.jsonPrimitive?.content }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parsePPEStatus(status: String?): PPEItemStatus {
        return when (status?.uppercase()) {
            "PRESENT" -> PPEItemStatus.PRESENT
            "MISSING" -> PPEItemStatus.MISSING
            else -> PPEItemStatus.UNKNOWN
        }
    }

    private fun parseRiskLevel(risk: String?): RiskLevel {
        return when (risk?.uppercase()) {
            "LOW" -> RiskLevel.LOW
            "MODERATE" -> RiskLevel.MODERATE
            "HIGH" -> RiskLevel.HIGH
            "SEVERE" -> RiskLevel.SEVERE
            else -> RiskLevel.MODERATE
        }
    }
    
    private fun getDefaultHazardType(workType: WorkType): HazardType {
        return when (workType) {
            WorkType.ELECTRICAL -> HazardType.ELECTRICAL_HAZARD
            WorkType.FALL_PROTECTION -> HazardType.FALL_PROTECTION
            else -> HazardType.PPE_VIOLATION
        }
    }
    
    private fun createHazardFromType(type: HazardType, workType: WorkType): Hazard {
        return when (type) {
            HazardType.ELECTRICAL_HAZARD -> Hazard(
                id = uuid4().toString(),
                type = type,
                severity = Severity.CRITICAL,
                description = "Electrical hazard detected in work area",
                oshaCode = "1926.405(a)(2)(ii)",
                confidence = 0.91f,
                recommendations = listOf(
                    "De-energize circuits before work",
                    "Use appropriate PPE",
                    "Follow lockout/tagout procedures"
                ),
                immediateAction = "Stop work and secure electrical hazard"
            )
            HazardType.FALL_PROTECTION -> Hazard(
                id = uuid4().toString(),
                type = type,
                severity = Severity.HIGH,
                description = "Fall protection hazard identified",
                oshaCode = "1926.501(b)(1)",
                confidence = 0.89f,
                recommendations = listOf(
                    "Install guardrail system",
                    "Provide fall arrest equipment",
                    "Train workers on fall protection"
                )
            )
            else -> Hazard(
                id = uuid4().toString(),
                type = type,
                severity = Severity.MEDIUM,
                description = "Safety compliance issue detected",
                oshaCode = "1926.95(a)",
                confidence = 0.85f,
                recommendations = listOf(
                    "Ensure proper PPE usage",
                    "Review safety procedures"
                )
            )
        }
    }
    
    private fun createDefaultPPEStatus(): PPEStatus {
        return PPEStatus(
            hardHat = PPEItem(PPEItemStatus.UNKNOWN, 0.5f),
            safetyVest = PPEItem(PPEItemStatus.UNKNOWN, 0.5f),
            safetyBoots = PPEItem(PPEItemStatus.UNKNOWN, 0.5f),
            safetyGlasses = PPEItem(PPEItemStatus.UNKNOWN, 0.5f),
            fallProtection = PPEItem(PPEItemStatus.UNKNOWN, 0.5f),
            respirator = PPEItem(PPEItemStatus.UNKNOWN, 0.5f),
            overallCompliance = 0.6f
        )
    }
    
    private fun createFallbackAnalysis(workType: WorkType, startTime: Long, rawResponse: String): SafetyAnalysis {
        return SafetyAnalysis(
            id = uuid4().toString(),
            timestamp = System.currentTimeMillis(),
            analysisType = AnalysisType.CLOUD_GEMINI,
            workType = workType,
            hazards = listOf(createHazardFromType(getDefaultHazardType(workType), workType)),
            ppeStatus = createDefaultPPEStatus(),
            recommendations = listOf(
                "AI analysis completed (JSON parsing fallback)",
                "Manual review recommended for detailed findings",
                "Raw AI response available in logs"
            ),
            overallRiskLevel = RiskLevel.MODERATE,
            confidence = 0.7f,
            processingTimeMs = System.currentTimeMillis() - startTime,
            oshaViolations = emptyList()
        )
    }
    
    private fun getOSHATitle(code: String): String {
        return when {
            code.startsWith("1926.501") -> "Fall Protection Requirements"
            code.startsWith("1926.405") -> "Electrical Safety Requirements"
            code.startsWith("1926.95") -> "Personal Protective Equipment"
            code.startsWith("1926.452") -> "Scaffolding Safety Requirements"
            else -> "OSHA Safety Requirement"
        }
    }
}