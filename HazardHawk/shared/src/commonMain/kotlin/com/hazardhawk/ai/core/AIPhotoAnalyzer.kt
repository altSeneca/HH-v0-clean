package com.hazardhawk.ai.core

import com.hazardhawk.domain.entities.WorkType

/**
 * Interface for AI photo analysis services in HazardHawk.
 * Provides contract for analyzing construction site photos for safety hazards.
 */
interface AIPhotoAnalyzer {
    /**
     * Analyze a photo for safety hazards and compliance issues.
     *
     * @param imageData The image data as a byte array
     * @param workType The type of construction work being performed
     * @return Result containing safety analysis or error
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

    /**
     * Check if the analyzer is available and ready to use.
     */
    val isAvailable: Boolean
}

/**
 * Safety analysis result from AI photo analyzer.
 */
data class SafetyAnalysis(
    val analysisId: String,
    val hazards: List<SafetyHazard> = emptyList(),
    val complianceScore: Float = 0.0f,
    val confidence: Float = 0.0f,
    val recommendations: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Individual safety hazard detected in photo.
 */
data class SafetyHazard(
    val id: String,
    val type: HazardType,
    val description: String,
    val severity: HazardSeverity,
    val boundingBox: BoundingBox? = null,
    val confidence: Float,
    val oshaViolation: String? = null,
    val recommendations: List<String> = emptyList()
)

/**
 * Types of safety hazards that can be detected.
 */
enum class HazardType {
    PERSON_NO_HARD_HAT,
    PERSON_NO_SAFETY_VEST,
    FALL_HAZARD,
    UNPROTECTED_EDGE,
    ELECTRICAL_HAZARD,
    HEAVY_MACHINERY,
    CRANE,
    EXCAVATOR,
    TRUCK,
    GENERAL_SAFETY_ISSUE
}

/**
 * Severity levels for hazards.
 */
enum class HazardSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO
}

/**
 * Bounding box coordinates for hazard location in image.
 */
data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)