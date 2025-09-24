package com.hazardhawk.ai.core

import com.hazardhawk.core.models.SafetyAnalysis
import com.hazardhawk.core.models.WorkType
import com.hazardhawk.core.models.AnalysisCapability

/**
 * Core interface for AI-powered construction safety photo analysis.
 * Implementations include Gemma 3N E2B (local), Vertex AI (cloud), and YOLO11 (fallback).
 */
interface AIPhotoAnalyzer {
    /**
     * Analyze a construction photo for safety hazards and compliance.
     * 
     * @param imageData Raw image bytes from camera or gallery
     * @param workType Type of construction work being performed
     * @return Result containing safety analysis or error
     */
    suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType = WorkType.GENERAL_CONSTRUCTION
    ): Result<SafetyAnalysis>
    
    /**
     * Configure the analyzer with necessary credentials or settings.
     * 
     * @param apiKey Optional API key for cloud services
     * @return Result indicating success or failure
     */
    suspend fun configure(apiKey: String? = null): Result<Unit>
    
    /**
     * Check if this analyzer is available and ready to use.
     */
    val isAvailable: Boolean
    
    /**
     * Get the analysis capabilities supported by this implementation.
     */
    val analysisCapabilities: Set<AnalysisCapability>
    
    /**
     * Get a human-readable name for this analyzer.
     */
    val analyzerName: String
    
    /**
     * Get the priority of this analyzer (higher = preferred).
     */
    val priority: Int
}