package com.hazardhawk.ai

import kotlinx.serialization.Serializable

/**
 * Gemma tokenizer interface for multiplatform text processing.
 * 
 * Provides tokenization and detokenization capabilities for the Gemma 3N E2B model
 * used in construction safety analysis. This expect/actual pattern allows for
 * platform-specific implementations using native tokenizers.
 */
expect class GemmaTokenizer {
    /**
     * Initialize the tokenizer with vocabulary and configuration.
     * 
     * @param vocabPath Path to the tokenizer vocabulary file
     * @param maxLength Maximum sequence length for tokenization
     * @return True if initialization successful
     */
    suspend fun initialize(vocabPath: String, maxLength: Int = 2048): Boolean
    
    /**
     * Tokenize text input for the Gemma model.
     * 
     * @param text Input text to tokenize
     * @param addSpecialTokens Whether to add BOS/EOS tokens
     * @param padding Whether to pad to max length
     * @return Tokenization result with ids and attention mask
     */
    suspend fun tokenize(
        text: String, 
        addSpecialTokens: Boolean = true,
        padding: Boolean = false
    ): TokenizationResult
    
    /**
     * Detokenize token IDs back to text.
     * 
     * @param tokenIds Array of token IDs to convert
     * @param skipSpecialTokens Whether to skip special tokens in output
     * @return Decoded text string
     */
    suspend fun detokenize(
        tokenIds: IntArray,
        skipSpecialTokens: Boolean = true
    ): String
    
    /**
     * Get vocabulary size of the tokenizer.
     */
    fun getVocabSize(): Int
    
    /**
     * Get special token IDs.
     */
    fun getSpecialTokens(): SpecialTokens
    
    /**
     * Clean up tokenizer resources.
     */
    suspend fun release()
    
    companion object {
        // Gemma special tokens
        const val BOS_TOKEN = "<bos>"
        const val EOS_TOKEN = "<eos>"
        const val PAD_TOKEN = "<pad>"
        const val UNK_TOKEN = "<unk>"
        
        // Construction safety specific tokens (if fine-tuned)
        const val SAFETY_START_TOKEN = "<safety_analysis>"
        const val HAZARD_TOKEN = "<hazard>"
        const val PPE_TOKEN = "<ppe>"
        const val OSHA_TOKEN = "<osha>"
    }
}

/**
 * Result of text tokenization.
 */
@Serializable
data class TokenizationResult(
    val inputIds: IntArray,
    val attentionMask: IntArray,
    val tokenTypeIds: IntArray? = null,
    val length: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TokenizationResult) return false
        
        if (!inputIds.contentEquals(other.inputIds)) return false
        if (!attentionMask.contentEquals(other.attentionMask)) return false
        if (tokenTypeIds != null && other.tokenTypeIds != null) {
            if (!tokenTypeIds.contentEquals(other.tokenTypeIds)) return false
        } else if (tokenTypeIds != other.tokenTypeIds) {
            return false
        }
        if (length != other.length) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = inputIds.contentHashCode()
        result = 31 * result + attentionMask.contentHashCode()
        result = 31 * result + (tokenTypeIds?.contentHashCode() ?: 0)
        result = 31 * result + length
        return result
    }
}

/**
 * Special token IDs for the Gemma model.
 */
@Serializable
data class SpecialTokens(
    val bosTokenId: Int,
    val eosTokenId: Int,
    val padTokenId: Int,
    val unkTokenId: Int,
    val safetyStartTokenId: Int? = null,
    val hazardTokenId: Int? = null,
    val ppeTokenId: Int? = null,
    val oshaTokenId: Int? = null
) {
    companion object {
        fun default() = SpecialTokens(
            bosTokenId = 1,    // Typical for Gemma models
            eosTokenId = 2,    // Typical for Gemma models  
            padTokenId = 0,    // Typical for most models
            unkTokenId = 3     // Typical for Gemma models
        )
    }
}

/**
 * Text generation configuration for construction safety analysis.
 */
@Serializable
data class TextGenerationConfig(
    val maxNewTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 50,
    val repetitionPenalty: Float = 1.1f,
    val doSample: Boolean = true,
    val earlyStopping: Boolean = true,
    val numReturnSequences: Int = 1,
    val lengthPenalty: Float = 1.0f,
    val noRepeatNgramSize: Int = 3
) {
    companion object {
        /**
         * Conservative configuration for reliable safety analysis.
         */
        fun forSafetyAnalysis() = TextGenerationConfig(
            maxNewTokens = 256,
            temperature = 0.3f,  // Lower temperature for more consistent results
            topP = 0.8f,
            topK = 40,
            repetitionPenalty = 1.2f,
            doSample = false,  // Use greedy decoding for safety-critical analysis
            earlyStopping = true
        )
        
        /**
         * Faster configuration for real-time analysis.
         */
        fun forRealTimeAnalysis() = TextGenerationConfig(
            maxNewTokens = 128,
            temperature = 0.4f,
            topP = 0.7f,
            topK = 30,
            repetitionPenalty = 1.15f,
            doSample = false,
            earlyStopping = true
        )
    }
}

/**
 * Construction safety prompt templates optimized for Gemma.
 */
object ConstructionSafetyPrompts {
    
    /**
     * Chain-of-thought prompt for comprehensive safety analysis.
     */
    const val CHAIN_OF_THOUGHT_ANALYSIS = """
You are an OSHA-certified construction safety inspector with 20+ years of experience. 

ANALYSIS FRAMEWORK:
1. OBSERVE: Describe what you see in this construction site image
2. IDENTIFY: List all visible safety hazards using the Fatal Four framework
3. ASSESS: Rate severity and OSHA compliance for each hazard
4. RECOMMEND: Provide specific corrective actions with OSHA citations

FATAL FOUR FOCUS:
- Falls (29 CFR 1926.501-502)
- Struck-by objects (29 CFR 1926.95)
- Electrocutions (29 CFR 1926.416)
- Caught-in/between (29 CFR 1926.651)

STEP 1 - OBSERVATION:
Describe the construction activity, workers, equipment, and environment visible in the image.

STEP 2 - HAZARD IDENTIFICATION:
For each hazard found, specify:
- Hazard type (fall, struck-by, electrical, caught-in, PPE violation)
- Location in image
- Severity level (LOW/MEDIUM/HIGH/CRITICAL)
- Workers at risk

STEP 3 - COMPLIANCE ASSESSMENT:
Check PPE compliance:
- Hard hats (required per 29 CFR 1926.95)
- Safety vests/high-visibility clothing
- Fall protection harnesses (if >6 feet)
- Eye and hearing protection
- Proper footwear

STEP 4 - RECOMMENDATIONS:
Provide immediate and long-term actions with specific OSHA regulation references.

OUTPUT FORMAT: Respond in valid JSON with this structure:
"""

    /**
     * Few-shot learning examples for better accuracy.
     */
    const val FEW_SHOT_EXAMPLES = """
EXAMPLE 1:
IMAGE: Worker on scaffolding without guardrails at 15 feet height
ANALYSIS: {
  "hazards": [{
    "type": "fall_protection_violation",
    "description": "Unprotected edge >6 feet without guardrail system",
    "severity": "CRITICAL",
    "osha_reference": "29 CFR 1926.501(b)(1)",
    "immediate_action": "Stop work, install guardrail system"
  }]
}

EXAMPLE 2:
IMAGE: Worker using angle grinder without eye protection
ANALYSIS: {
  "hazards": [{
    "type": "ppe_violation", 
    "description": "No eye protection during grinding operation",
    "severity": "HIGH",
    "osha_reference": "29 CFR 1926.95(a)",
    "immediate_action": "Provide safety goggles immediately"
  }]
}

EXAMPLE 3:
IMAGE: Construction site with proper PPE and safety measures
ANALYSIS: {
  "hazards": [],
  "ppe_compliance": {
    "hard_hat": "COMPLIANT",
    "safety_vest": "COMPLIANT", 
    "overall_score": 1.0
  },
  "safety_commendation": "Excellent safety practices observed"
}
"""

    /**
     * Structured output template for parsing.
     */
    const val JSON_OUTPUT_TEMPLATE = """
{
  "analysis_metadata": {
    "timestamp": "ISO_TIMESTAMP",
    "confidence": 0.0-1.0,
    "processing_time_ms": 0
  },
  "safety_assessment": {
    "overall_risk_level": "LOW|MEDIUM|HIGH|CRITICAL",
    "hazards_detected": [
      {
        "type": "hazard_type",
        "description": "detailed_description", 
        "severity": "LOW|MEDIUM|HIGH|CRITICAL",
        "confidence": 0.0-1.0,
        "location": "location_in_image",
        "osha_reference": "29 CFR 1926.xxx",
        "immediate_action": "specific_corrective_action",
        "workers_affected": "number_or_description"
      }
    ],
    "ppe_compliance": {
      "hard_hat": {"status": "COMPLIANT|NON_COMPLIANT|NOT_VISIBLE", "confidence": 0.0-1.0},
      "safety_vest": {"status": "COMPLIANT|NON_COMPLIANT|NOT_VISIBLE", "confidence": 0.0-1.0},
      "fall_protection": {"status": "COMPLIANT|NON_COMPLIANT|NOT_APPLICABLE", "confidence": 0.0-1.0},
      "eye_protection": {"status": "COMPLIANT|NON_COMPLIANT|NOT_APPLICABLE", "confidence": 0.0-1.0},
      "overall_score": 0.0-1.0
    },
    "osha_violations": [
      {
        "regulation": "29 CFR 1926.xxx",
        "description": "violation_description",
        "severity": "MINOR|SERIOUS|WILLFUL|IMMINENT_DANGER",
        "recommended_action": "corrective_action",
        "citation_likely": true|false
      }
    ],
    "recommendations": [
      {
        "category": "ppe|fall_protection|electrical|housekeeping|training",
        "priority": "LOW|MEDIUM|HIGH|CRITICAL",
        "description": "specific_recommendation",
        "osha_reference": "29 CFR 1926.xxx",
        "timeline": "immediate|daily|weekly|ongoing"
      }
    ]
  }
}
"""

    /**
     * Build a complete prompt for construction safety analysis.
     */
    fun buildCompleteSafetyPrompt(
        workType: String = "general_construction",
        includeExamples: Boolean = true,
        focusAreas: List<String> = emptyList()
    ): String {
        val focusSection = if (focusAreas.isNotEmpty()) {
            "\nSPECIAL FOCUS AREAS:\n${focusAreas.joinToString("\n") { "- $it" }}\n"
        } else ""
        
        val examplesSection = if (includeExamples) "\n$FEW_SHOT_EXAMPLES\n" else ""
        
        return """
$CHAIN_OF_THOUGHT_ANALYSIS
$focusSection$examplesSection
NOW ANALYZE THIS CONSTRUCTION SITE IMAGE:

Expected JSON output structure:
$JSON_OUTPUT_TEMPLATE

Begin your analysis:
""".trimIndent()
    }
}