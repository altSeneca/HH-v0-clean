package com.hazardhawk.ai.core

import com.hazardhawk.models.OSHAAnalysisResult
import com.hazardhawk.domain.entities.WorkType

/**
 * Interface for OSHA-focused AI photo analysis services in HazardHawk.
 * Provides detailed safety compliance analysis with OSHA standards and regulations.
 */
interface OSHAPhotoAnalyzer {
    /**
     * Analyze a photo for OSHA compliance issues and safety violations.
     *
     * @param imageData The image data as a byte array
     * @param workType The type of construction work being performed
     * @return Result containing OSHA analysis or error
     */
    suspend fun analyzeForOSHACompliance(
        imageData: ByteArray,
        workType: WorkType
    ): Result<OSHAAnalysisResult>

    /**
     * Configure the analyzer with API keys or settings.
     *
     * @param apiKey Optional API key for cloud services
     * @return Result indicating success or failure of configuration
     */
    suspend fun configure(apiKey: String? = null): Result<Unit>

    /**
     * Check if the analyzer is available and ready to use.
     */
    val isAvailable: Boolean
}