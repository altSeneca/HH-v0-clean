package com.hazardhawk.ai.core

import com.hazardhawk.ai.models.SafetyAnalysis
import com.hazardhawk.ai.models.WorkType
import com.hazardhawk.ai.models.AnalysisCapability

/**
 * Enhanced interface for AI photo analysis services in HazardHawk.
 * Provides contract for analyzing construction site photos for safety hazards.
 *
 * Updated to match orchestrator implementations (SimplifiedAIOrchestrator, SmartAIOrchestrator)
 * with additional metadata properties for service management and monitoring.
 */
interface AIPhotoAnalyzer {
    /**
     * Human-readable name of the analyzer for logging and UI display.
     */
    val analyzerName: String

    /**
     * Priority level for service selection (higher = preferred).
     * Used by factory to determine which analyzer to use when multiple are available.
     */
    val priority: Int

    /**
     * Set of analysis capabilities supported by this analyzer.
     * Used for feature detection and capability matching.
     */
    val analysisCapabilities: Set<AnalysisCapability>

    /**
     * Check if the analyzer is available and ready to use.
     */
    val isAvailable: Boolean

    /**
     * Analyze a photo for safety hazards and compliance issues.
     *
     * @param imageData The image data as a byte array
     * @param workType The type of construction work being performed
     * @return Result containing detailed safety analysis or error
     */
    suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType
    ): Result<SafetyAnalysis>

    /**
     * Configure the analyzer with API keys or settings.
     *
     * @param apiKey Optional API key for cloud services
     * @return Result indicating success or failure of configuration
     */
    suspend fun configure(apiKey: String? = null): Result<Unit>
}