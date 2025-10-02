package com.hazardhawk.ai

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import com.hazardhawk.security.SecureStorageService
import kotlin.math.roundToInt

/**
 * Manages dynamic downloading of AI models for offline usage
 * Downloads models on demand to reduce APK size
 */
class ModelDownloadManager(
    private val secureStorage: SecureStorageService,
    private val httpClient: HttpClient
) {
    companion object {
        private const val MODEL_BASE_URL = "https://storage.googleapis.com/hazardhawk-models"
        private const val MODEL_MANIFEST_KEY = "model_manifest"
        private const val DOWNLOAD_CHUNK_SIZE = 8192L
    }

    @Serializable
    data class ModelManifest(
        val models: List<ModelInfo>
    )

    @Serializable
    data class ModelInfo(
        val name: String,
        val version: String,
        val sizeBytes: Long,
        val downloadUrl: String,
        val checksum: String,
        val platform: String, // "android", "ios", "all"
        val priority: Int // 1=critical, 2=recommended, 3=optional
    )

    sealed class DownloadProgress {
        object Starting : DownloadProgress()
        data class InProgress(val bytesDownloaded: Long, val totalBytes: Long, val percentage: Int) : DownloadProgress()
        data class Completed(val modelName: String, val filePath: String) : DownloadProgress()
        data class Failed(val error: String) : DownloadProgress()
    }

    private val downloadJobs = mutableMapOf<String, Job>()
    
    /**
     * Check if essential models are downloaded, download if missing
     */
    suspend fun ensureEssentialModels(
        platform: String = "android",
        onProgress: (DownloadProgress) -> Unit = {}
    ): Result<List<String>> {
        return try {
            val manifest = getModelManifest()
            val essentialModels = manifest.models.filter { 
                it.priority == 1 && (it.platform == platform || it.platform == "all")
            }
            
            val downloadedPaths = mutableListOf<String>()
            
            for (model in essentialModels) {
                if (!isModelDownloaded(model.name)) {
                    val result = downloadModel(model, onProgress)
                    if (result.isSuccess) {
                        downloadedPaths.add(result.getOrThrow())
                    } else {
                        return Result.failure(Exception("Failed to download ${model.name}"))
                    }
                } else {
                    downloadedPaths.add(getModelPath(model.name))
                }
            }
            
            Result.success(downloadedPaths)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Download specific model
     * Note: Uses Dispatchers.Default for commonMain compatibility
     */
    suspend fun downloadModel(
        model: ModelInfo,
        onProgress: (DownloadProgress) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.Default) {
        try {
            onProgress(DownloadProgress.Starting)
            
            val response = httpClient.get(model.downloadUrl)
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(Exception("Download failed: ${response.status}"))
            }
            
            val totalBytes = response.contentLength() ?: 0L
            var downloadedBytes = 0L
            
            val filePath = getModelPath(model.name)
            val tempFilePath = "$filePath.tmp"
            
            // Create platform-specific file handling
            val fileBytes = ByteArray(totalBytes.toInt())
            var offset = 0
            
            response.bodyAsChannel().let { channel ->
                val buffer = ByteArray(DOWNLOAD_CHUNK_SIZE.toInt())
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        buffer.copyInto(fileBytes, offset, 0, bytesRead)
                        offset += bytesRead
                        downloadedBytes += bytesRead
                        
                        val percentage = ((downloadedBytes * 100) / totalBytes).toInt()
                        onProgress(DownloadProgress.InProgress(downloadedBytes, totalBytes, percentage))
                    }
                }
            }
            
            // Verify checksum if provided
            if (model.checksum.isNotEmpty()) {
                val downloadedChecksum = calculateChecksum(fileBytes)
                if (downloadedChecksum != model.checksum) {
                    return@withContext Result.failure(Exception("Checksum mismatch"))
                }
            }
            
            // Save to secure storage (platform-specific implementation needed)
            saveModelFile(model.name, fileBytes)
            
            onProgress(DownloadProgress.Completed(model.name, filePath))
            Result.success(filePath)
            
        } catch (e: Exception) {
            onProgress(DownloadProgress.Failed(e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }
    
    /**
     * Get available models from remote manifest
     */
    private suspend fun getModelManifest(): ModelManifest {
        // Try cached manifest first
        val cachedManifest = secureStorage.getString(MODEL_MANIFEST_KEY)
        if (cachedManifest != null) {
            try {
                return Json.decodeFromString<ModelManifest>(cachedManifest)
            } catch (e: Exception) {
                // Continue to fetch fresh manifest
            }
        }
        
        // Fetch from remote
        val response = httpClient.get("$MODEL_BASE_URL/manifest.json")
        val manifestJson = response.bodyAsText()
        val manifest = Json.decodeFromString<ModelManifest>(manifestJson)
        
        // Cache for future use
        secureStorage.setString(MODEL_MANIFEST_KEY, manifestJson)
        
        return manifest
    }
    
    /**
     * Check if model is already downloaded
     */
    private fun isModelDownloaded(modelName: String): Boolean {
        // Platform-specific implementation needed
        return false // Placeholder
    }
    
    /**
     * Get local path for model file
     */
    private fun getModelPath(modelName: String): String {
        // Platform-specific implementation needed
        return "/models/$modelName.tflite" // Placeholder
    }
    
    /**
     * Save model file to platform storage
     */
    private suspend fun saveModelFile(modelName: String, data: ByteArray) {
        // Platform-specific implementation needed
        // Android: Internal storage
        // iOS: Documents directory
        // Desktop: User data directory
    }
    
    /**
     * Calculate file checksum for verification
     */
    private fun calculateChecksum(data: ByteArray): String {
        // Simple hash for now - platform-specific crypto implementation needed
        return data.contentHashCode().toString()
    }
    
    /**
     * Cancel all downloads
     */
    fun cancelAllDownloads() {
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
    }
    
    /**
     * Get downloaded model size info
     */
    suspend fun getDownloadedModelsSize(): Long {
        // Platform-specific implementation to calculate total size
        return 0L
    }
    
    /**
     * Clean up old/unused models
     */
    suspend fun cleanupOldModels(): Result<Long> {
        // Implementation to remove unused models and return freed space
        return Result.success(0L)
    }
}