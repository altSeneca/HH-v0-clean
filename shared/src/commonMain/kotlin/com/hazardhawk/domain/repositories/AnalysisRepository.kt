package com.hazardhawk.domain.repositories

import com.hazardhawk.core.models.SafetyAnalysis

interface AnalysisRepository {
    suspend fun saveAnalysis(analysis: SafetyAnalysis): Result<SafetyAnalysis>
    suspend fun getAnalysis(photoId: String): SafetyAnalysis?
    suspend fun getAllAnalyses(): List<SafetyAnalysis>
    suspend fun deleteAnalysis(analysisId: String): Result<Unit>
}