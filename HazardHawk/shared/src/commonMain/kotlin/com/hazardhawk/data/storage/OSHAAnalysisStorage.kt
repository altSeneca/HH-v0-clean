package com.hazardhawk.data.storage

import com.hazardhawk.data.repositories.OSHAAnalysisData
import com.hazardhawk.data.repositories.SecureStorage
import kotlinx.serialization.json.Json

/**
 * Storage interface for OSHA analysis data
 */
interface OSHAAnalysisStorage {
    suspend fun saveAnalysis(photoId: String, analysisData: OSHAAnalysisData)
    suspend fun getAnalysis(photoId: String): OSHAAnalysisData?
    suspend fun deleteAnalysis(photoId: String)
    suspend fun getAllAnalyses(): Map<String, OSHAAnalysisData>
    suspend fun hasAnalysis(photoId: String): Boolean
}

/**
 * Implementation using secure storage
 */
class OSHAAnalysisStorageImpl(
    private val secureStorage: SecureStorage
) : OSHAAnalysisStorage {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private const val OSHA_ANALYSIS_PREFIX = "osha_analysis_"
        private const val OSHA_ANALYSIS_INDEX = "osha_analysis_index"
    }

    override suspend fun saveAnalysis(photoId: String, analysisData: OSHAAnalysisData) {
        val key = OSHA_ANALYSIS_PREFIX + photoId
        val serializedData = json.encodeToString(OSHAAnalysisData.serializer(), analysisData)

        secureStorage.putString(key, serializedData)

        // Update index
        updateAnalysisIndex(photoId, add = true)
    }

    override suspend fun getAnalysis(photoId: String): OSHAAnalysisData? {
        val key = OSHA_ANALYSIS_PREFIX + photoId
        val serializedData = secureStorage.getString(key)

        return if (serializedData != null) {
            try {
                json.decodeFromString(OSHAAnalysisData.serializer(), serializedData)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    override suspend fun deleteAnalysis(photoId: String) {
        val key = OSHA_ANALYSIS_PREFIX + photoId
        secureStorage.remove(key)

        // Update index
        updateAnalysisIndex(photoId, add = false)
    }

    override suspend fun getAllAnalyses(): Map<String, OSHAAnalysisData> {
        val index = getAnalysisIndex()
        val analyses = mutableMapOf<String, OSHAAnalysisData>()

        index.forEach { photoId ->
            val analysisData = getAnalysis(photoId)
            if (analysisData != null) {
                analyses[photoId] = analysisData
            }
        }

        return analyses
    }

    override suspend fun hasAnalysis(photoId: String): Boolean {
        val key = OSHA_ANALYSIS_PREFIX + photoId
        return secureStorage.getString(key) != null
    }

    private suspend fun getAnalysisIndex(): Set<String> {
        val indexJson = secureStorage.getString(OSHA_ANALYSIS_INDEX)
        return if (indexJson != null) {
            try {
                json.decodeFromString<Set<String>>(indexJson)
            } catch (e: Exception) {
                emptySet()
            }
        } else {
            emptySet()
        }
    }

    private suspend fun updateAnalysisIndex(photoId: String, add: Boolean) {
        val currentIndex = getAnalysisIndex().toMutableSet()

        if (add) {
            currentIndex.add(photoId)
        } else {
            currentIndex.remove(photoId)
        }

        val updatedIndexJson = json.encodeToString(kotlinx.serialization.serializer<Set<String>>(), currentIndex)
        secureStorage.putString(OSHA_ANALYSIS_INDEX, updatedIndexJson)
    }
}