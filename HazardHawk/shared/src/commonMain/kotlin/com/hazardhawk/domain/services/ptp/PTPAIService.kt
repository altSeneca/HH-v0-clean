package com.hazardhawk.domain.services.ptp

import com.hazardhawk.domain.models.ptp.*
import com.hazardhawk.platform.currentTimeMillis
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * AI service for Pre-Task Plan generation using Google Gemini
 */
interface PTPAIService {
    /**
     * Generate a PTP using AI based on questionnaire and optional photo analysis
     */
    suspend fun generatePtp(request: PtpAIRequest): Result<PtpAIResponse>

    /**
     * Regenerate PTP with user feedback
     */
    suspend fun regeneratePtp(
        originalRequest: PtpAIRequest,
        userFeedback: String,
        previousContent: PtpContent
    ): Result<PtpAIResponse>

    /**
     * Validate OSHA code format
     */
    fun validateOshaCode(code: String): Boolean

    /**
     * Check if service is available (API key configured, network available)
     */
    suspend fun isAvailable(): Boolean
}

/**
 * Implementation of PTPAIService using Google Gemini API
 */
class GeminiPTPAIService(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val json: Json = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
    }
) : PTPAIService {

    companion object {
        private const val GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta"
        private const val MODEL_NAME = "gemini-2.5-flash" // Using Gemini 2.5 Flash for faster, cost-effective generation
        private const val REQUEST_TIMEOUT_MILLIS = 120000L // 2 minutes for AI generation

        // OSHA code regex pattern: 1926.XXX or 1926.XXX(x)(x)
        internal val OSHA_CODE_PATTERN = Regex("^1926\\.[0-9]+(\\([a-z]\\)(\\([0-9]+\\))?)?$")
    }

    override suspend fun generatePtp(request: PtpAIRequest): Result<PtpAIResponse> {
        return try {
            println("PTPAIService: Starting PTP generation")
            println("PTPAIService: API Key configured: ${apiKey.isNotBlank()}")

            val startTime = currentTimeMillis()

            // Build the AI prompt
            println("PTPAIService: Building AI prompt")
            val prompt = PtpAIPrompt.buildPrompt(request)
            println("PTPAIService: Prompt length: ${prompt.length} characters")

            // Call Gemini API
            println("PTPAIService: Calling Gemini API")
            val geminiResponse = callGeminiAPI(prompt)
            println("PTPAIService: Received Gemini response, length: ${geminiResponse.textContent.length}")

            // Parse the response
            println("PTPAIService: Parsing Gemini response")
            val content = parseGeminiResponse(geminiResponse.textContent)
            println("PTPAIService: Successfully parsed content with ${content.hazards.size} hazards")

            val processingTime = currentTimeMillis() - startTime

            // Log token usage if available
            geminiResponse.tokenUsage?.let { usage ->
                println("PTPAIService: Token usage - Input: ${usage.promptTokenCount}, Output: ${usage.candidatesTokenCount}, Total: ${usage.totalTokenCount}, Cost: $${String.format("%.4f", usage.estimatedCost)}")
            }

            Result.success(
                PtpAIResponse(
                    success = true,
                    content = content,
                    confidence = calculateConfidence(content),
                    processingTimeMs = processingTime,
                    warnings = validateContent(content),
                    tokenUsage = geminiResponse.tokenUsage
                )
            )
        } catch (e: Exception) {
            println("PTPAIService: ERROR - ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun regeneratePtp(
        originalRequest: PtpAIRequest,
        userFeedback: String,
        previousContent: PtpContent
    ): Result<PtpAIResponse> {
        return try {
            val startTime = currentTimeMillis()

            // Build regeneration prompt with user feedback
            val prompt = PtpAIPrompt.buildRegenerationPrompt(
                originalRequest,
                userFeedback,
                previousContent
            )

            // Call Gemini API
            val geminiResponse = callGeminiAPI(prompt)

            // Parse the response
            val content = parseGeminiResponse(geminiResponse.textContent)

            val processingTime = currentTimeMillis() - startTime

            Result.success(
                PtpAIResponse(
                    success = true,
                    content = content,
                    confidence = calculateConfidence(content),
                    processingTimeMs = processingTime,
                    warnings = validateContent(content),
                    tokenUsage = geminiResponse.tokenUsage
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun validateOshaCode(code: String): Boolean {
        return OSHA_CODE_PATTERN.matches(code)
    }

    override suspend fun isAvailable(): Boolean {
        return apiKey.isNotBlank()
    }

    /**
     * Data class to hold Gemini API response including token usage
     */
    private data class GeminiAPIResponse(
        val textContent: String,
        val tokenUsage: TokenUsageMetadata?
    )

    /**
     * Call Gemini API with the constructed prompt
     */
    private suspend fun callGeminiAPI(prompt: String): GeminiAPIResponse {
        val endpoint = "$GEMINI_API_BASE/models/$MODEL_NAME:generateContent"
        println("PTPAIService: Endpoint: $endpoint")

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", prompt)
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.2) // Lower temperature for more consistent OSHA compliance
                put("topK", 40)
                put("topP", 0.95)
                put("maxOutputTokens", 8192)
                putJsonArray("stopSequences") {
                    // No stop sequences - we want complete JSON
                }
                put("responseMimeType", "application/json") // Request JSON response
            }
            putJsonArray("safetySettings") {
                // Set all safety categories to BLOCK_NONE since we're generating safety documentation
                addJsonObject {
                    put("category", "HARM_CATEGORY_HARASSMENT")
                    put("threshold", "BLOCK_NONE")
                }
                addJsonObject {
                    put("category", "HARM_CATEGORY_HATE_SPEECH")
                    put("threshold", "BLOCK_NONE")
                }
                addJsonObject {
                    put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT")
                    put("threshold", "BLOCK_NONE")
                }
                addJsonObject {
                    put("category", "HARM_CATEGORY_DANGEROUS_CONTENT")
                    put("threshold", "BLOCK_NONE")
                }
            }
        }

        println("PTPAIService: Sending POST request to Gemini")
        val response = httpClient.post(endpoint) {
            parameter("key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
            // Override default timeout for AI generation requests
            timeout {
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                connectTimeoutMillis = 30000L
                socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            }
        }

        println("PTPAIService: Received response with status: ${response.status}")
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            println("PTPAIService: ERROR Response Body: $errorBody")
            throw Exception("Gemini API error: ${response.status} - $errorBody")
        }

        val responseBody = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseBody).jsonObject

        // Extract token usage metadata
        val usageMetadata = responseJson["usageMetadata"]?.jsonObject?.let { metadata ->
            TokenUsageMetadata(
                promptTokenCount = metadata["promptTokenCount"]?.jsonPrimitive?.int ?: 0,
                candidatesTokenCount = metadata["candidatesTokenCount"]?.jsonPrimitive?.int ?: 0,
                totalTokenCount = metadata["totalTokenCount"]?.jsonPrimitive?.int ?: 0,
                modelVersion = MODEL_NAME
            )
        }

        // Extract the generated text from Gemini response
        val candidates = responseJson["candidates"]?.jsonArray
        if (candidates.isNullOrEmpty()) {
            throw Exception("No candidates in Gemini response")
        }

        val firstCandidate = candidates[0].jsonObject
        val content = firstCandidate["content"]?.jsonObject
        val parts = content?.get("parts")?.jsonArray

        if (parts.isNullOrEmpty()) {
            throw Exception("No parts in Gemini response")
        }

        val text = parts[0].jsonObject["text"]?.jsonPrimitive?.content
            ?: throw Exception("No text in Gemini response")

        return GeminiAPIResponse(
            textContent = text,
            tokenUsage = usageMetadata
        )
    }

    /**
     * Parse Gemini JSON response into PtpContent
     */
    private fun parseGeminiResponse(responseText: String): PtpContent {
        try {
            println("PTPAIService: Raw response text length: ${responseText.length}")
            println("PTPAIService: First 200 chars: ${responseText.take(200)}")
            println("PTPAIService: Last 200 chars: ${responseText.takeLast(200)}")

            // The response should be pure JSON since we set responseMimeType
            val cleaned = responseText.trim().let {
                // Remove markdown code blocks if present
                if (it.startsWith("```json")) {
                    it.removePrefix("```json").removeSuffix("```").trim()
                } else if (it.startsWith("```")) {
                    it.removePrefix("```").removeSuffix("```").trim()
                } else {
                    it
                }
            }.let {
                // Fix Gemini's occasional trailing commas in arrays/objects
                // Replace ",]" with "]" and ",}" with "}"
                it.replace(Regex(",\\s*\\]"), "]")
                  .replace(Regex(",\\s*\\}"), "}")
            }

            println("PTPAIService: Cleaned JSON length: ${cleaned.length}")
            println("PTPAIService: Attempting to parse JSON...")

            val jsonElement = json.parseToJsonElement(cleaned).jsonObject

            // Parse hazards
            val hazards = jsonElement["hazards"]?.jsonArray?.map { hazardJson ->
                val hazardObj = hazardJson.jsonObject
                PtpHazard(
                    oshaCode = hazardObj["oshaCode"]?.jsonPrimitive?.content ?: "",
                    description = hazardObj["description"]?.jsonPrimitive?.content ?: "",
                    severity = HazardSeverity.valueOf(
                        hazardObj["severity"]?.jsonPrimitive?.content?.uppercase() ?: "MINOR"
                    ),
                    controls = hazardObj["controls"]?.jsonArray?.map {
                        it.jsonPrimitive.content
                    } ?: emptyList(),
                    requiredPpe = hazardObj["requiredPPE"]?.jsonArray?.map {
                        it.jsonPrimitive.content
                    } ?: emptyList(),
                    photoReferences = hazardObj["photoReferences"]?.jsonArray?.map {
                        it.jsonPrimitive.content
                    } ?: emptyList()
                )
            } ?: emptyList()

            // Parse job steps
            val jobSteps = jsonElement["jobSteps"]?.jsonArray?.map { stepJson ->
                val stepObj = stepJson.jsonObject
                JobStep(
                    stepNumber = stepObj["step"]?.jsonPrimitive?.int ?: 0,
                    description = stepObj["description"]?.jsonPrimitive?.content ?: "",
                    hazards = stepObj["hazards"]?.jsonArray?.map {
                        it.jsonPrimitive.content
                    } ?: emptyList(),
                    controls = stepObj["controls"]?.jsonArray?.map {
                        it.jsonPrimitive.content
                    } ?: emptyList(),
                    ppe = stepObj["ppe"]?.jsonArray?.map {
                        it.jsonPrimitive.content
                    } ?: emptyList()
                )
            } ?: emptyList()

            // Parse emergency procedures
            val emergencyProcedures = jsonElement["emergencyProcedures"]?.jsonArray?.map {
                it.jsonPrimitive.content
            } ?: emptyList()

            // Parse required training
            val requiredTraining = jsonElement["requiredTraining"]?.jsonArray?.map {
                it.jsonPrimitive.content
            } ?: emptyList()

            return PtpContent(
                hazards = hazards,
                jobSteps = jobSteps,
                emergencyProcedures = emergencyProcedures,
                requiredTraining = requiredTraining
            )
        } catch (e: Exception) {
            println("PTPAIService: Parse error at offset: ${e.message}")
            println("PTPAIService: Full response text:")
            println(responseText)
            throw Exception("Failed to parse Gemini response: ${e.message}", e)
        }
    }

    /**
     * Calculate confidence score based on content completeness and OSHA code validity
     */
    private fun calculateConfidence(content: PtpContent): Double {
        var score = 0.0
        var maxScore = 0.0

        // Hazards present and valid
        maxScore += 30.0
        if (content.hazards.isNotEmpty()) {
            score += 15.0
            val validOshaCodes = content.hazards.count { validateOshaCode(it.oshaCode) }
            score += (validOshaCodes.toDouble() / content.hazards.size) * 15.0
        }

        // Job steps present
        maxScore += 25.0
        if (content.jobSteps.isNotEmpty()) {
            score += 25.0
        }

        // Emergency procedures present
        maxScore += 20.0
        if (content.emergencyProcedures.isNotEmpty()) {
            score += 20.0
        }

        // Required training present
        maxScore += 15.0
        if (content.requiredTraining.isNotEmpty()) {
            score += 15.0
        }

        // Controls specified for hazards
        maxScore += 10.0
        val hazardsWithControls = content.hazards.count { it.controls.isNotEmpty() }
        if (content.hazards.isNotEmpty()) {
            score += (hazardsWithControls.toDouble() / content.hazards.size) * 10.0
        }

        return if (maxScore > 0) score / maxScore else 0.0
    }

    /**
     * Validate content and return warnings
     */
    private fun validateContent(content: PtpContent): List<String> {
        val warnings = mutableListOf<String>()

        // Check for invalid OSHA codes
        content.hazards.forEach { hazard ->
            if (!validateOshaCode(hazard.oshaCode)) {
                warnings.add("Invalid OSHA code format: ${hazard.oshaCode}")
            }
        }

        // Check for hazards without controls
        val hazardsWithoutControls = content.hazards.filter { it.controls.isEmpty() }
        if (hazardsWithoutControls.isNotEmpty()) {
            warnings.add("${hazardsWithoutControls.size} hazard(s) missing control measures")
        }

        // Check for critical hazards without PPE
        val criticalWithoutPpe = content.hazards.filter {
            it.severity == HazardSeverity.CRITICAL && it.requiredPpe.isEmpty()
        }
        if (criticalWithoutPpe.isNotEmpty()) {
            warnings.add("${criticalWithoutPpe.size} critical hazard(s) missing PPE requirements")
        }

        // Check for empty sections
        if (content.hazards.isEmpty()) {
            warnings.add("No hazards identified - review questionnaire inputs")
        }
        if (content.jobSteps.isEmpty()) {
            warnings.add("No job steps defined - consider adding work breakdown")
        }
        if (content.emergencyProcedures.isEmpty()) {
            warnings.add("No emergency procedures specified")
        }

        return warnings
    }
}

/**
 * Mock implementation for testing
 */
class MockPTPAIService : PTPAIService {
    override suspend fun generatePtp(request: PtpAIRequest): Result<PtpAIResponse> {
        // Simulate processing delay
        kotlinx.coroutines.delay(2000)

        val mockContent = PtpContent(
            hazards = listOf(
                PtpHazard(
                    oshaCode = "1926.501(b)(1)",
                    description = "Fall hazard from unprotected sides and edges at heights greater than 6 feet",
                    severity = HazardSeverity.CRITICAL,
                    controls = listOf(
                        "Install OSHA-compliant guardrail systems",
                        "Use personal fall arrest systems with proper anchorage"
                    ),
                    requiredPpe = listOf("Full-body harness", "Hard hat", "Steel-toed boots")
                )
            ),
            jobSteps = listOf(
                JobStep(
                    stepNumber = 1,
                    description = "Set up work area and safety equipment",
                    hazards = listOf("Fall from height", "Struck by objects"),
                    controls = listOf("Inspect equipment", "Secure tools"),
                    ppe = listOf("Hard hat", "Safety harness", "Steel-toed boots")
                )
            ),
            emergencyProcedures = listOf(
                "Fall incident: Call 911, do not move injured worker",
                "Evacuate to assembly point if unsafe conditions"
            ),
            requiredTraining = listOf(
                "OSHA 10-hour Construction Safety",
                "Fall protection training"
            )
        )

        return Result.success(
            PtpAIResponse(
                success = true,
                content = mockContent,
                confidence = 0.95,
                processingTimeMs = 2000,
                warnings = emptyList()
            )
        )
    }

    override suspend fun regeneratePtp(
        originalRequest: PtpAIRequest,
        userFeedback: String,
        previousContent: PtpContent
    ): Result<PtpAIResponse> {
        return generatePtp(originalRequest)
    }

    override fun validateOshaCode(code: String): Boolean {
        return GeminiPTPAIService.OSHA_CODE_PATTERN.matches(code)
    }

    override suspend fun isAvailable(): Boolean = true
}
