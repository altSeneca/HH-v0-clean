package com.hazardhawk.domain.models.ptp

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock

/**
 * Token usage metadata from Gemini API response
 * Tracks actual token consumption and calculates cost
 */
@Serializable
data class TokenUsageMetadata(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int,
    val modelVersion: String = "gemini-2.5-flash"
) {
    /**
     * Calculate estimated cost based on Gemini 2.5 Flash pricing
     */
    val estimatedCost: Double
        get() = calculateCost(promptTokenCount, candidatesTokenCount)

    companion object {
        // Gemini 2.5 Flash pricing (as of January 2025)
        const val INPUT_COST_PER_MILLION = 0.30
        const val OUTPUT_COST_PER_MILLION = 1.20

        /**
         * Calculate cost based on input and output token counts
         */
        fun calculateCost(inputTokens: Int, outputTokens: Int): Double {
            return (inputTokens / 1_000_000.0 * INPUT_COST_PER_MILLION) +
                   (outputTokens / 1_000_000.0 * OUTPUT_COST_PER_MILLION)
        }

        /**
         * Estimate token count from text length
         * Heuristic: 1 token ≈ 4 characters
         * Includes system prompt overhead
         */
        fun estimateTokens(text: String): Int {
            // Base token count from text (rough heuristic)
            val baseTokens = (text.length / 4.0).toInt()

            // System prompt tokens (from PtpAIPrompt template)
            val systemPromptTokens = 450

            // OSHA reference and schema tokens
            val oshaReferenceTokens = 200

            return baseTokens + systemPromptTokens + oshaReferenceTokens
        }

        /**
         * Estimate cost range for pre-generation display
         * Returns (low, high) estimates with 30% variance
         */
        fun estimateCostRange(text: String): Pair<Double, Double> {
            val estimatedInput = estimateTokens(text)
            val estimatedOutput = 2000 // Typical PTP output size

            val baseCost = calculateCost(estimatedInput, estimatedOutput)

            // ±30% variance for estimate
            val lowCost = baseCost * 0.7
            val highCost = baseCost * 1.3

            return Pair(lowCost, highCost)
        }
    }
}

/**
 * Token usage record for database storage
 * Tracks historical usage for analytics
 */
@Serializable
data class TokenUsageRecord(
    val id: String,
    val ptpId: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val estimatedCost: Double,
    val modelName: String,
    val timestamp: Long,
    val successful: Boolean = true
)

/**
 * Aggregated token usage summary for analytics
 */
@Serializable
data class TokenUsageSummary(
    val totalTokens: Long,
    val totalCost: Double,
    val requestCount: Int
)
