package com.hazardhawk.data.repositories

import com.hazardhawk.core.models.*
import com.hazardhawk.data.storage.OSHAAnalysisStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/**
 * Repository for managing OSHA analysis results persistence
 * Handles saving, retrieving, and deleting OSHA analysis data linked to photos
 */
interface OSHAAnalysisRepository {
    /**
     * Save OSHA analysis result for a specific photo
     */
    suspend fun saveAnalysis(photoId: String, analysis: OSHAAnalysisResult): Result<Unit>

    /**
     * Get saved OSHA analysis for a photo
     */
    suspend fun getAnalysis(photoId: String): Result<OSHAAnalysisResult?>

    /**
     * Delete OSHA analysis when photo is deleted
     */
    suspend fun deleteAnalysis(photoId: String): Result<Unit>

    /**
     * Get all OSHA analyses for report generation
     */
    suspend fun getAllAnalyses(): Result<List<Pair<String, OSHAAnalysisResult>>>

    /**
     * Get analyses by date range for reports
     */
    suspend fun getAnalysesByDateRange(startDate: Long, endDate: Long): Result<List<Pair<String, OSHAAnalysisResult>>>

    /**
     * Check if analysis exists for a photo
     */
    suspend fun hasAnalysis(photoId: String): Boolean

    /**
     * Get analysis summary for reporting
     */
    suspend fun getAnalysisSummary(): Result<OSHAAnalysisSummary>
}

/**
 * Implementation of OSHAAnalysisRepository using secure storage
 */
class OSHAAnalysisRepositoryImpl(
    private val storage: OSHAAnalysisStorage
) : OSHAAnalysisRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun saveAnalysis(photoId: String, analysis: OSHAAnalysisResult): Result<Unit> {
        return try {
            val analysisData = OSHAAnalysisData(
                photoId = photoId,
                analysisResult = analysis,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                version = 1
            )

            storage.saveAnalysis(photoId, analysisData)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAnalysis(photoId: String): Result<OSHAAnalysisResult?> {
        return try {
            val analysisData = storage.getAnalysis(photoId)
            Result.success(analysisData?.analysisResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAnalysis(photoId: String): Result<Unit> {
        return try {
            storage.deleteAnalysis(photoId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllAnalyses(): Result<List<Pair<String, OSHAAnalysisResult>>> {
        return try {
            val allData = storage.getAllAnalyses()
            val analyses = allData.map { (photoId, data) ->
                photoId to data.analysisResult
            }
            Result.success(analyses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAnalysesByDateRange(startDate: Long, endDate: Long): Result<List<Pair<String, OSHAAnalysisResult>>> {
        return try {
            val allData = storage.getAllAnalyses()
            val filteredAnalyses = allData.filter { (_, data) ->
                data.timestamp in startDate..endDate
            }.map { (photoId, data) ->
                photoId to data.analysisResult
            }
            Result.success(filteredAnalyses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasAnalysis(photoId: String): Boolean {
        return try {
            storage.getAnalysis(photoId) != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAnalysisSummary(): Result<OSHAAnalysisSummary> {
        return try {
            val allData = storage.getAllAnalyses()
            val analyses = allData.values.map { it.analysisResult }

            val totalAnalyses = analyses.size
            val totalViolations = analyses.sumOf { it.oshaViolations.size }
            val averageComplianceScore = if (analyses.isNotEmpty()) {
                analyses.map { it.complianceScore }.average()
            } else 0.0

            val violationsBySeverity = mapOf<OSHASeverity, Int>()

            val violationsByStandard = analyses.flatMap { it.oshaViolations }
                .groupBy { it.oshaStandard }
                .mapValues { it.value.size }

            val summary = OSHAAnalysisSummary(
                totalAnalyses = totalAnalyses,
                totalViolations = totalViolations,
                averageComplianceScore = averageComplianceScore,
                violationsBySeverity = violationsBySeverity,
                violationsByStandard = violationsByStandard,
                lastUpdated = Clock.System.now().toEpochMilliseconds()
            )

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Data model for storing OSHA analysis with metadata
 */
@kotlinx.serialization.Serializable
data class OSHAAnalysisData(
    val photoId: String,
    val analysisResult: OSHAAnalysisResult,
    val timestamp: Long,
    val version: Int = 1
)

/**
 * Summary statistics for OSHA analyses
 */
@kotlinx.serialization.Serializable
data class OSHAAnalysisSummary(
    val totalAnalyses: Int,
    val totalViolations: Int,
    val averageComplianceScore: Double,
    val violationsBySeverity: Map<OSHASeverity, Int>,
    val violationsByStandard: Map<String, Int>,
    val lastUpdated: Long
)