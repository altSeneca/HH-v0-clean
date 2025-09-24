package com.hazardhawk.ai

import kotlinx.serialization.Serializable

/**
 * Gemma text generation interface for multimodal construction safety analysis.
 * 
 * Provides text generation capabilities that combine vision features with language
 * understanding for comprehensive safety analysis. This expect/actual pattern allows
 * for platform-specific implementations using ONNX Runtime.
 */
expect class GemmaTextGenerator {
    /**
     * Initialize the text generator with ONNX model and tokenizer.
     * 
     * @param modelPath Path to the decoder ONNX model
     * @param tokenizer Initialized tokenizer for text processing
     * @return True if initialization successful
     */
    suspend fun initialize(modelPath: String, tokenizer: GemmaTokenizer): Boolean
    
    /**
     * Generate construction safety analysis text from vision features and prompt.
     * 
     * @param visionFeatures Encoded features from the vision model
     * @param prompt Text prompt for the analysis
     * @param config Generation configuration parameters
     * @return Generated text analysis
     */
    suspend fun generateSafetyAnalysis(
        visionFeatures: FloatArray,
        prompt: String,
        config: TextGenerationConfig = TextGenerationConfig.forSafetyAnalysis()
    ): TextGenerationResult
    
    /**
     * Generate text using greedy decoding for consistent results.
     * 
     * @param visionFeatures Vision features from encoder
     * @param promptTokens Pre-tokenized prompt
     * @param maxNewTokens Maximum tokens to generate
     * @return Generated token sequence
     */
    suspend fun generateGreedy(
        visionFeatures: FloatArray,
        promptTokens: IntArray,
        maxNewTokens: Int = 256
    ): IntArray
    
    /**
     * Generate text using sampling for diverse outputs.
     * 
     * @param visionFeatures Vision features from encoder
     * @param promptTokens Pre-tokenized prompt
     * @param config Sampling configuration
     * @return Generated token sequence
     */
    suspend fun generateWithSampling(
        visionFeatures: FloatArray,
        promptTokens: IntArray,
        config: SamplingConfig
    ): IntArray
    
    /**
     * Check if the generator is ready for inference.
     */
    fun isInitialized(): Boolean
    
    /**
     * Get model information and capabilities.
     */
    fun getModelInfo(): TextModelInfo
    
    /**
     * Release model resources.
     */
    suspend fun release()
}

/**
 * Result of text generation operation.
 */
@Serializable
data class TextGenerationResult(
    val generatedText: String,
    val tokenIds: IntArray,
    val confidence: Float,
    val processingTimeMs: Long,
    val metadata: GenerationMetadata
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextGenerationResult) return false
        
        if (generatedText != other.generatedText) return false
        if (!tokenIds.contentEquals(other.tokenIds)) return false
        if (confidence != other.confidence) return false
        if (processingTimeMs != other.processingTimeMs) return false
        if (metadata != other.metadata) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = generatedText.hashCode()
        result = 31 * result + tokenIds.contentHashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + processingTimeMs.hashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }
}

/**
 * Sampling configuration for text generation.
 */
@Serializable
data class SamplingConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 50,
    val repetitionPenalty: Float = 1.1f,
    val lengthPenalty: Float = 1.0f
) {
    companion object {
        fun conservative() = SamplingConfig(
            temperature = 0.3f,
            topP = 0.8f,
            topK = 40,
            repetitionPenalty = 1.15f
        )
        
        fun creative() = SamplingConfig(
            temperature = 0.8f,
            topP = 0.95f,
            topK = 60,
            repetitionPenalty = 1.05f
        )
    }
}

/**
 * Text generation metadata for analysis and debugging.
 */
@Serializable
data class GenerationMetadata(
    val promptLength: Int,
    val generatedLength: Int,
    val totalInferenceSteps: Int,
    val avgTokenProbability: Float,
    val stoppedBy: String, // "max_length", "eos_token", "early_stopping"
    val visionFeatureSize: Int,
    val memoryUsageMb: Int = 0
)

/**
 * Text model information and capabilities.
 */
@Serializable
data class TextModelInfo(
    val modelName: String,
    val vocabularySize: Int,
    val maxSequenceLength: Int,
    val hiddenSize: Int,
    val numLayers: Int,
    val numAttentionHeads: Int,
    val supportsVisionInput: Boolean,
    val modelSizeMb: Float
)

/**
 * Vision-text integration utilities for multimodal analysis.
 */
object VisionTextIntegration {
    
    /**
     * Prepare vision features for text generation.
     * 
     * @param rawVisionFeatures Raw features from vision encoder
     * @param targetDimension Expected dimension for text decoder
     * @return Processed vision features ready for text generation
     */
    fun prepareVisionFeatures(
        rawVisionFeatures: FloatArray,
        targetDimension: Int = 768
    ): FloatArray {
        // If features are already the right size, return as-is
        if (rawVisionFeatures.size == targetDimension) {
            return rawVisionFeatures
        }
        
        // Simple linear projection/pooling to match expected dimension
        return when {
            rawVisionFeatures.size > targetDimension -> {
                // Downsample using average pooling
                val ratio = rawVisionFeatures.size.toFloat() / targetDimension
                FloatArray(targetDimension) { i ->
                    val startIdx = (i * ratio).toInt()
                    val endIdx = ((i + 1) * ratio).toInt().coerceAtMost(rawVisionFeatures.size)
                    val sum = (startIdx until endIdx).sumOf { rawVisionFeatures[it].toDouble() }
                    (sum / (endIdx - startIdx)).toFloat()
                }
            }
            rawVisionFeatures.size < targetDimension -> {
                // Upsample using interpolation
                FloatArray(targetDimension) { i ->
                    val sourceIdx = (i * rawVisionFeatures.size.toFloat() / targetDimension).toInt()
                    val clampedIdx = sourceIdx.coerceIn(0, rawVisionFeatures.size - 1)
                    rawVisionFeatures[clampedIdx]
                }
            }
            else -> rawVisionFeatures
        }
    }
    
    /**
     * Create vision-conditioned prompt for construction safety analysis.
     * 
     * @param basePrompt Base prompt template
     * @param workType Type of construction work (electrical, roofing, etc.)
     * @param visionContext Vision-derived context (optional)
     * @return Enhanced prompt with vision conditioning
     */
    fun createVisionConditionedPrompt(
        basePrompt: String,
        workType: String = "general_construction",
        visionContext: String? = null
    ): String {
        val workTypeContext = when (workType.lowercase()) {
            "electrical" -> "Focus on electrical safety: GFCI protection, lockout/tagout, insulated tools, arc flash protection."
            "roofing" -> "Focus on fall protection: guardrails, safety harnesses, roof edge protection, ladder safety."
            "excavation" -> "Focus on excavation safety: cave-in protection, soil classification, entry/exit points, atmospheric hazards."
            "steel_erection" -> "Focus on structural safety: fall protection, crane operations, steel connection safety."
            "concrete" -> "Focus on concrete operations: formwork safety, reinforcement handling, concrete placement hazards."
            else -> "General construction safety analysis with focus on Fatal Four hazards."
        }
        
        val visionSection = visionContext?.let { 
            "\nVISION CONTEXT: Based on the image analysis, I can see: $it\n"
        } ?: ""
        
        return """
WORK TYPE: $workType
$workTypeContext
$visionSection
$basePrompt

Remember to output valid JSON with specific OSHA citations and actionable recommendations.
""".trimIndent()
    }
    
    /**
     * Parse generated safety analysis text to extract structured data.
     * 
     * @param generatedText Raw text output from model
     * @return Parsed safety analysis or error information
     */
    fun parseSafetyAnalysis(generatedText: String): SafetyAnalysisParseResult {
        return try {
            // Try to extract JSON from the generated text
            val jsonStart = generatedText.indexOf('{')
            val jsonEnd = generatedText.lastIndexOf('}') + 1
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonText = generatedText.substring(jsonStart, jsonEnd)
                
                // Basic validation that it looks like our expected structure
                val hasExpectedFields = jsonText.contains("hazards") || 
                                      jsonText.contains("ppe_compliance") ||
                                      jsonText.contains("safety_assessment")
                
                if (hasExpectedFields) {
                    SafetyAnalysisParseResult.Success(jsonText)
                } else {
                    SafetyAnalysisParseResult.InvalidFormat(
                        "Generated JSON does not contain expected safety analysis fields",
                        generatedText
                    )
                }
            } else {
                // No JSON found, create structured fallback
                val fallbackAnalysis = createFallbackAnalysis(generatedText)
                SafetyAnalysisParseResult.Success(fallbackAnalysis)
            }
        } catch (e: Exception) {
            SafetyAnalysisParseResult.ParseError(e.message ?: "Unknown parsing error", generatedText)
        }
    }
    
    /**
     * Create fallback structured analysis from free-form text.
     */
    private fun createFallbackAnalysis(text: String): String {
        val hasHazardKeywords = text.lowercase().contains(Regex("hazard|danger|unsafe|violation|risk"))
        val hasPPEKeywords = text.lowercase().contains(Regex("hard hat|helmet|safety vest|harness|gloves|protection"))
        val hasOSHAKeywords = text.lowercase().contains(Regex("osha|1926|regulation|compliance|citation"))
        
        val riskLevel = when {
            text.lowercase().contains(Regex("critical|imminent|danger|stop work|emergency")) -> "CRITICAL"
            text.lowercase().contains(Regex("high|serious|major|significant")) -> "HIGH"
            text.lowercase().contains(Regex("medium|moderate")) -> "MEDIUM"
            else -> "LOW"
        }
        
        return """
{
    "analysis_metadata": {
        "timestamp": "${System.currentTimeMillis()}",
        "confidence": 0.6,
        "processing_time_ms": 0,
        "fallback_parsing": true
    },
    "safety_assessment": {
        "overall_risk_level": "$riskLevel",
        "hazards_detected": ${if (hasHazardKeywords) """[
            {
                "type": "unspecified_hazard",
                "description": "Potential safety concerns identified in analysis",
                "severity": "$riskLevel",
                "confidence": 0.5,
                "location": "general_site_area",
                "immediate_action": "Review site conditions and implement safety measures"
            }
        ]""" else "[]"},
        "ppe_compliance": {
            "hard_hat": {"status": "${if (hasPPEKeywords) "COMPLIANT" else "NOT_VISIBLE"}", "confidence": 0.5},
            "safety_vest": {"status": "NOT_VISIBLE", "confidence": 0.0},
            "fall_protection": {"status": "NOT_APPLICABLE", "confidence": 0.0},
            "overall_score": 0.5
        },
        "osha_violations": ${if (hasOSHAKeywords) """[
            {
                "regulation": "General OSHA requirements",
                "description": "Review for compliance with applicable standards",
                "severity": "SERIOUS",
                "recommended_action": "Conduct comprehensive safety assessment"
            }
        ]""" else "[]"},
        "recommendations": [
            {
                "category": "general_safety",
                "priority": "MEDIUM",
                "description": "Conduct thorough safety review based on analysis: ${text.take(100)}...",
                "timeline": "immediate"
            }
        ],
        "raw_analysis_text": "${text.replace("\"", "\\\"")}"
    }
}
""".trimIndent()
    }
}

/**
 * Result of parsing generated safety analysis text.
 */
sealed class SafetyAnalysisParseResult {
    data class Success(val jsonText: String) : SafetyAnalysisParseResult()
    data class InvalidFormat(val error: String, val rawText: String) : SafetyAnalysisParseResult()
    data class ParseError(val error: String, val rawText: String) : SafetyAnalysisParseResult()
}