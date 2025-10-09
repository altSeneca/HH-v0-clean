package com.hazardhawk.domain.models.ptp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for TokenUsageMetadata and related functionality
 */
class TokenUsageTest {

    @Test
    fun `calculateCost returns accurate cost for known token counts`() {
        // Given known token counts
        val inputTokens = 1000
        val outputTokens = 2000

        // When calculating cost
        val cost = TokenUsageMetadata.calculateCost(inputTokens, outputTokens)

        // Then cost should match expected formula
        val expectedCost = (1000 / 1_000_000.0 * 0.30) + (2000 / 1_000_000.0 * 1.20)
        assertEquals(expectedCost, cost, 0.0001)
    }

    @Test
    fun `calculateCost handles zero tokens correctly`() {
        // Given zero tokens
        val cost = TokenUsageMetadata.calculateCost(0, 0)

        // Then cost should be zero
        assertEquals(0.0, cost, 0.0001)
    }

    @Test
    fun `estimateTokens provides reasonable estimates for varying text lengths`() {
        // Given different text lengths
        val shortText = "Install scaffolding"
        val mediumText = "Install scaffolding with proper fall protection on the east side of building 3"
        val longText = "Install scaffolding with proper fall protection on the east side of building 3. " +
                "Ensure all workers wear hard hats and harnesses. Verify lockout/tagout procedures before beginning work."

        // When estimating tokens
        val shortTokens = TokenUsageMetadata.estimateTokens(shortText)
        val mediumTokens = TokenUsageMetadata.estimateTokens(mediumText)
        val longTokens = TokenUsageMetadata.estimateTokens(longText)

        // Then estimates should increase with text length
        assertTrue(shortTokens < mediumTokens, "Short text should have fewer tokens than medium")
        assertTrue(mediumTokens < longTokens, "Medium text should have fewer tokens than long")

        // Short text should include base overhead (system prompt + OSHA reference)
        assertTrue(shortTokens in 650..750, "Short text estimate should be in range with overhead")
    }

    @Test
    fun `estimateTokens includes system prompt overhead`() {
        // Given minimal text
        val minimalText = "Test"

        // When estimating tokens
        val tokens = TokenUsageMetadata.estimateTokens(minimalText)

        // Then should include base overhead (450 + 200 = 650 minimum)
        assertTrue(tokens >= 650, "Should include system prompt and OSHA reference overhead")
    }

    @Test
    fun `estimateCostRange provides 30 percent variance`() {
        // Given sample text
        val text = "Install electrical conduit with proper grounding and lockout procedures"

        // When estimating cost range
        val (lowCost, highCost) = TokenUsageMetadata.estimateCostRange(text)

        // Then range should have ~30% variance
        val baseCost = (lowCost + highCost) / 2
        val variance = (highCost - lowCost) / baseCost

        assertTrue(variance >= 0.55 && variance <= 0.65, "Variance should be approximately 60% (±30%)")
        assertTrue(lowCost < highCost, "Low cost should be less than high cost")
    }

    @Test
    fun `TokenUsageMetadata estimatedCost property matches calculateCost`() {
        // Given token usage metadata
        val usage = TokenUsageMetadata(
            promptTokenCount = 850,
            candidatesTokenCount = 2100,
            totalTokenCount = 2950,
            modelVersion = "gemini-2.5-flash"
        )

        // When accessing estimatedCost property
        val propertyCost = usage.estimatedCost

        // Then it should match manual calculation
        val calculatedCost = TokenUsageMetadata.calculateCost(850, 2100)
        assertEquals(calculatedCost, propertyCost, 0.0001)
    }

    @Test
    fun `TokenUsageMetadata pricing constants are correct for Gemini 2-5 Flash`() {
        // Verify pricing constants
        assertEquals(0.30, TokenUsageMetadata.INPUT_COST_PER_MILLION, 0.0001)
        assertEquals(1.20, TokenUsageMetadata.OUTPUT_COST_PER_MILLION, 0.0001)
    }

    @Test
    fun `TokenUsageRecord stores all required fields`() {
        // Given token usage record data
        val record = TokenUsageRecord(
            id = "test-id-123",
            ptpId = "ptp-456",
            promptTokens = 800,
            completionTokens = 2000,
            totalTokens = 2800,
            estimatedCost = 0.00264,
            modelName = "gemini-2.5-flash",
            timestamp = 1728400000000L,
            successful = true
        )

        // Then all fields should be accessible and correct
        assertEquals("test-id-123", record.id)
        assertEquals("ptp-456", record.ptpId)
        assertEquals(800, record.promptTokens)
        assertEquals(2000, record.completionTokens)
        assertEquals(2800, record.totalTokens)
        assertEquals(0.00264, record.estimatedCost, 0.00001)
        assertEquals("gemini-2.5-flash", record.modelName)
        assertEquals(1728400000000L, record.timestamp)
        assertTrue(record.successful)
    }

    @Test
    fun `TokenUsageSummary aggregates correctly`() {
        // Given summary data
        val summary = TokenUsageSummary(
            totalTokens = 10000L,
            totalCost = 0.05,
            requestCount = 5
        )

        // Then fields should be accessible
        assertEquals(10000L, summary.totalTokens)
        assertEquals(0.05, summary.totalCost, 0.0001)
        assertEquals(5, summary.requestCount)
    }

    @Test
    fun `cost calculation matches real-world example`() {
        // Given realistic PTP generation scenario
        // Input: ~850 tokens (questionnaire + system prompt)
        // Output: ~2100 tokens (comprehensive PTP)
        val inputTokens = 850
        val outputTokens = 2100

        // When calculating cost
        val cost = TokenUsageMetadata.calculateCost(inputTokens, outputTokens)

        // Then cost should be reasonable (less than $0.01)
        assertTrue(cost < 0.01, "Typical PTP generation should cost less than $0.01")
        assertTrue(cost > 0.001, "Cost should be non-trivial")

        // Specific calculation:
        // (850/1M * 0.30) + (2100/1M * 1.20) = 0.000255 + 0.00252 = 0.002775
        assertEquals(0.002775, cost, 0.000001)
    }

    @Test
    fun `estimate is within 30 percent of actual for typical PTP`() {
        // Given typical questionnaire text
        val questionnaireText = """
            Project: Office Building Renovation
            Work Type: Electrical Installation
            Task: Install new electrical panel and conduit on 3rd floor
            Crew Size: 4 workers
            Working at height: Yes, up to 12 feet
            Near power lines: Yes
            Tools: Ladders, conduit benders, multimeters, hand tools
        """.trimIndent()

        // When estimating tokens
        val estimatedInput = TokenUsageMetadata.estimateTokens(questionnaireText)

        // Then estimate should be reasonable for this input
        // Actual would be around 700-900 tokens including overhead
        assertTrue(estimatedInput in 700..900, "Estimate should be in realistic range")

        // Verify cost estimate range
        val (lowCost, highCost) = TokenUsageMetadata.estimateCostRange(questionnaireText)
        assertTrue(lowCost > 0.0, "Low cost should be positive")
        assertTrue(highCost < 0.01, "High cost should be reasonable for typical PTP")
    }
}
