package com.hazardhawk.data.repositories

import com.hazardhawk.database.HazardHawkDatabase
import com.hazardhawk.domain.entities.SafetyAnalysis
import com.hazardhawk.domain.repositories.AnalysisRepository

class AnalysisRepositoryImpl(
    private val database: HazardHawkDatabase
) : AnalysisRepository {
    
    override suspend fun saveAnalysis(analysis: SafetyAnalysis): Result<SafetyAnalysis> {
        return Result.success(analysis)
    }
    
    override suspend fun getAnalysis(photoId: String): SafetyAnalysis? {
        return null
    }
    
    override suspend fun getAllAnalyses(): List<SafetyAnalysis> {
        return emptyList()
    }
    
    override suspend fun deleteAnalysis(analysisId: String): Result<Unit> {
        return Result.success(Unit)
    }
}