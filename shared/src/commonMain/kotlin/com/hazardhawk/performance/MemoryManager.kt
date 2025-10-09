package com.hazardhawk.performance
import kotlinx.datetime.Clock

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.math.min

/**
 * Intelligent memory management system for AI models and image processing.
 * Optimized for construction environments with varying device capabilities.
 */
class MemoryManager(
    private val deviceDetector: DeviceTierDetector,
    private val performanceMonitor: PerformanceMonitor
) {
    private val mutex = Mutex()
    private val loadedModels = mutableMapOf<String, LoadedModel>()
    private val imageCache = mutableMapOf<String, CachedImage>()
    private val analysisResultCache = mutableMapOf<String, CachedAnalysis>()
    
    private var deviceCapabilities: DeviceCapabilities? = null
    private var performanceConfig: PerformanceConfig? = null
    
    data class LoadedModel(
        val modelId: String,
        val sizeBytes: Long,
        val loadTimestamp: Long,
        val lastAccessTimestamp: Long,
        val accessCount: Int,
        val complexity: ModelComplexity,
        val modelData: Any? = null // Platform-specific model data
    )
    
    data class CachedImage(
        val imageId: String,
        val sizeBytes: Long,
        val timestamp: Long,
        val accessCount: Int,
        val imageData: ByteArray
    )
    
    data class CachedAnalysis(
        val analysisId: String,
        val sizeBytes: Long,
        val timestamp: Long,
        val lastAccessTimestamp: Long,
        val analysisData: Any // SafetyAnalysis or similar
    )
    
    suspend fun initialize() {
        mutex.withLock {
            deviceCapabilities = deviceDetector.detectCapabilities()
            performanceConfig = PerformanceConfig.fromCapabilities(deviceCapabilities!!)
        }
    }
    
    /**
     * Smart model loading with memory pressure awareness.
     */
    suspend fun loadModel(
        modelId: String,
        modelSizeBytes: Long,
        complexity: ModelComplexity,
        loader: suspend () -> Any?
    ): Result<Any?> {
        return mutex.withLock {
            try {
                val config = performanceConfig ?: return@withLock Result.failure(
                    Exception("MemoryManager not initialized")
                )
                
                // Check if model is already loaded
                loadedModels[modelId]?.let { existingModel ->
                    loadedModels[modelId] = existingModel.copy(
                        lastAccessTimestamp = Clock.System.now().toEpochMilliseconds(),
                        accessCount = existingModel.accessCount + 1
                    )
                    return@withLock Result.success(existingModel.modelData)
                }
                
                // Check memory availability
                val availableMemory = deviceDetector.getAvailableMemory()
                val requiredMemory = modelSizeBytes + (modelSizeBytes * 0.2f).toLong() // 20% buffer
                
                if (availableMemory < requiredMemory) {
                    // Attempt to free memory
                    val freedMemory = freeMemoryForModel(requiredMemory)
                    if (freedMemory < requiredMemory) {
                        return@withLock Result.failure(
                            Exception("Insufficient memory: need ${requiredMemory / (1024*1024)}MB, available ${availableMemory / (1024*1024)}MB")
                        )
                    }
                }
                
                // Validate model complexity for device tier
                val deviceCapabilities = this.deviceCapabilities!!
                if (complexity > deviceCapabilities.recommendedModelComplexity && 
                    deviceCapabilities.memoryPressure >= MemoryPressure.HIGH) {
                    return@withLock Result.failure(
                        Exception("Model complexity too high for device tier: ${deviceCapabilities.tier}")
                    )
                }
                
                // Load the model
                val startTime = Clock.System.now().toEpochMilliseconds()
                val modelData = loader()
                val loadTime = Clock.System.now().toEpochMilliseconds() - startTime
                
                // Track loading performance
                performanceMonitor.recordModelLoad(loadTime, (modelSizeBytes / (1024 * 1024)).toInt())
                
                // Store loaded model
                loadedModels[modelId] = LoadedModel(
                    modelId = modelId,
                    sizeBytes = modelSizeBytes,
                    loadTimestamp = Clock.System.now().toEpochMilliseconds(),
                    lastAccessTimestamp = Clock.System.now().toEpochMilliseconds(),
                    accessCount = 1,
                    complexity = complexity,
                    modelData = modelData
                )
                
                Result.success(modelData)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Unload model from memory.
     */
    suspend fun unloadModel(modelId: String): Boolean {
        return mutex.withLock {
            loadedModels.remove(modelId) != null
        }
    }
    
    /**
     * Cache processed images for reuse.
     */
    suspend fun cacheImage(imageId: String, imageData: ByteArray): Boolean {
        return mutex.withLock {
            val config = performanceConfig ?: return@withLock false
            
            if (!config.enableResultCaching) return@withLock false
            
            val sizeBytes = imageData.size.toLong()
            val availableMemory = deviceDetector.getAvailableMemory()
            val maxCacheMemory = availableMemory * 0.1f // Use max 10% for image cache
            
            if (getCurrentImageCacheSize() + sizeBytes > maxCacheMemory) {
                evictOldestImages(sizeBytes)
            }
            
            imageCache[imageId] = CachedImage(
                imageId = imageId,
                sizeBytes = sizeBytes,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                accessCount = 1,
                imageData = imageData
            )
            
            true
        }
    }
    
    /**
     * Retrieve cached image.
     */
    suspend fun getCachedImage(imageId: String): ByteArray? {
        return mutex.withLock {
            imageCache[imageId]?.let { cached ->
                imageCache[imageId] = cached.copy(accessCount = cached.accessCount + 1)
                cached.imageData
            }
        }
    }
    
    /**
     * Cache analysis results.
     */
    suspend fun cacheAnalysisResult(analysisId: String, analysisData: Any): Boolean {
        return mutex.withLock {
            val config = performanceConfig ?: return@withLock false
            
            if (!config.enableResultCaching) return@withLock false
            
            // Estimate size (simplified)
            val sizeBytes = 1024L // Approximate size of analysis result
            
            if (analysisResultCache.size >= config.maxCacheSize) {
                evictOldestAnalysis()
            }
            
            analysisResultCache[analysisId] = CachedAnalysis(
                analysisId = analysisId,
                sizeBytes = sizeBytes,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                lastAccessTimestamp = Clock.System.now().toEpochMilliseconds(),
                analysisData = analysisData
            )
            
            true
        }
    }
    
    /**
     * Retrieve cached analysis result.
     */
    suspend fun getCachedAnalysisResult(analysisId: String): Any? {
        return mutex.withLock {
            analysisResultCache[analysisId]?.let { cached ->
                analysisResultCache[analysisId] = cached.copy(
                    lastAccessTimestamp = Clock.System.now().toEpochMilliseconds()
                )
                cached.analysisData
            }
        }
    }
    
    /**
     * Handle memory pressure situations.
     */
    suspend fun handleMemoryPressure(pressureLevel: MemoryPressure) {
        mutex.withLock {
            when (pressureLevel) {
                MemoryPressure.LOW -> {
                    // Nothing to do
                }
                MemoryPressure.MODERATE -> {
                    // Clear some caches
                    evictOldestImages(0, 0.3f) // Remove 30% of oldest images
                    evictOldestAnalysis(0.3f) // Remove 30% of oldest analysis
                }
                MemoryPressure.HIGH -> {
                    // Aggressive cache clearing
                    evictOldestImages(0, 0.6f) // Remove 60% of oldest images  
                    evictOldestAnalysis(0.6f) // Remove 60% of oldest analysis
                    // Consider unloading least used models
                    unloadLeastUsedModels(1)
                }
                MemoryPressure.CRITICAL -> {
                    // Emergency memory freeing
                    imageCache.clear()
                    analysisResultCache.clear()
                    unloadLeastUsedModels(2) // Keep only most critical models
                }
            }
        }
    }
    
    /**
     * Get memory usage statistics.
     */
    suspend fun getMemoryStats(): MemoryStats {
        return mutex.withLock {
            val totalModelMemory = loadedModels.values.sumOf { it.sizeBytes }
            val totalImageCache = getCurrentImageCacheSize()
            val totalAnalysisCache = analysisResultCache.values.sumOf { it.sizeBytes }
            
            MemoryStats(
                loadedModels = loadedModels.size,
                totalModelMemoryMB = (totalModelMemory / (1024 * 1024)).toInt(),
                cachedImages = imageCache.size,
                totalImageCacheMB = (totalImageCache / (1024 * 1024)).toInt(),
                cachedAnalyses = analysisResultCache.size,
                totalAnalysisCacheMB = (totalAnalysisCache / (1024 * 1024)).toInt(),
                totalManagedMemoryMB = ((totalModelMemory + totalImageCache + totalAnalysisCache) / (1024 * 1024)).toInt()
            )
        }
    }
    
    private suspend fun freeMemoryForModel(requiredMemory: Long): Long {
        var freedMemory = 0L
        
        // First, try clearing analysis cache
        freedMemory += clearOldAnalysisCache()
        if (freedMemory >= requiredMemory) return freedMemory
        
        // Then, clear image cache
        freedMemory += clearOldImageCache()
        if (freedMemory >= requiredMemory) return freedMemory
        
        // Finally, unload least used models
        freedMemory += unloadLeastUsedModels(1)
        
        return freedMemory
    }
    
    private fun clearOldAnalysisCache(): Long {
        val cutoff = Clock.System.now().toEpochMilliseconds() - 600_000 // 10 minutes
        val toRemove = analysisResultCache.filter { it.value.timestamp < cutoff }
        val freedBytes = toRemove.values.sumOf { it.sizeBytes }
        
        toRemove.keys.forEach { analysisResultCache.remove(it) }
        
        return freedBytes
    }
    
    private fun clearOldImageCache(): Long {
        val cutoff = Clock.System.now().toEpochMilliseconds() - 300_000 // 5 minutes
        val toRemove = imageCache.filter { it.value.timestamp < cutoff }
        val freedBytes = toRemove.values.sumOf { it.sizeBytes }
        
        toRemove.keys.forEach { imageCache.remove(it) }
        
        return freedBytes
    }
    
    private fun unloadLeastUsedModels(keepCount: Int): Long {
        if (loadedModels.size <= keepCount) return 0L
        
        val sortedModels = loadedModels.values.sortedBy { 
            it.accessCount.toFloat() / (Clock.System.now().toEpochMilliseconds() - it.loadTimestamp)
        }
        
        val toUnload = sortedModels.dropLast(keepCount)
        val freedBytes = toUnload.sumOf { it.sizeBytes }
        
        toUnload.forEach { loadedModels.remove(it.modelId) }
        
        return freedBytes
    }
    
    private fun evictOldestImages(requiredBytes: Long, fraction: Float = 1.0f) {
        if (imageCache.isEmpty()) return
        
        val sortedImages = imageCache.values.sortedBy { it.timestamp }
        val evictCount = if (requiredBytes > 0) {
            var freedBytes = 0L
            var count = 0
            for (image in sortedImages) {
                freedBytes += image.sizeBytes
                count++
                if (freedBytes >= requiredBytes) break
            }
            count
        } else {
            (sortedImages.size * fraction).toInt()
        }
        
        sortedImages.take(evictCount).forEach { image ->
            imageCache.remove(image.imageId)
        }
    }
    
    private fun evictOldestAnalysis(fraction: Float = 1.0f) {
        if (analysisResultCache.isEmpty()) return
        
        val sortedAnalyses = analysisResultCache.values.sortedBy { it.timestamp }
        val evictCount = (sortedAnalyses.size * fraction).toInt()
        
        sortedAnalyses.take(evictCount).forEach { analysis ->
            analysisResultCache.remove(analysis.analysisId)
        }
    }
    
    private fun getCurrentImageCacheSize(): Long {
        return imageCache.values.sumOf { it.sizeBytes }
    }
    
    /**
     * Generate cache key for image analysis.
     */
    fun generateCacheKey(imageData: ByteArray, workType: String): String {
        return "${imageData.contentHashCode()}_${workType}_${imageData.size}"
    }
    
    /**
     * Check if model should be preloaded based on usage patterns.
     */
    suspend fun shouldPreloadModel(modelId: String, complexity: ModelComplexity): Boolean {
        val config = performanceConfig ?: return false
        if (!config.enableModelPreloading) return false
        
        val capabilities = deviceCapabilities ?: return false
        
        // Don't preload if memory pressure is high
        if (capabilities.memoryPressure >= MemoryPressure.HIGH) return false
        
        // Don't preload if model complexity is too high for device
        if (complexity > capabilities.recommendedModelComplexity) return false
        
        // Check if we have enough memory
        val availableMemory = deviceDetector.getAvailableMemory()
        val estimatedModelSize = when (complexity) {
            ModelComplexity.BASIC -> 50 * 1024 * 1024 // 50MB
            ModelComplexity.STANDARD -> 150 * 1024 * 1024 // 150MB
            ModelComplexity.ADVANCED -> 500 * 1024 * 1024 // 500MB
        }
        
        return availableMemory > estimatedModelSize * 2 // Need 2x buffer
    }
}

/**
 * Memory usage statistics.
 */
data class MemoryStats(
    val loadedModels: Int,
    val totalModelMemoryMB: Int,
    val cachedImages: Int,
    val totalImageCacheMB: Int,
    val cachedAnalyses: Int,
    val totalAnalysisCacheMB: Int,
    val totalManagedMemoryMB: Int
)

/**
 * Smart garbage collection optimizer.
 */
class SmartGarbageCollector {
    private var lastGCTime = 0L
    private val gcHistory = mutableListOf<GCEvent>()
    
    data class GCEvent(
        val timestamp: Long,
        val memoryBeforeMB: Float,
        val memoryAfterMB: Float,
        val durationMs: Long
    )
    
    /**
     * Suggest garbage collection based on memory pressure and patterns.
     */
    suspend fun suggestGarbageCollection(
        currentMemoryMB: Float,
        availableMemoryMB: Float,
        memoryPressure: MemoryPressure
    ): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        
        // Don't GC too frequently (min 30 seconds apart)
        if (now - lastGCTime < 30_000) return false
        
        // Force GC on high memory pressure
        if (memoryPressure >= MemoryPressure.HIGH) return true
        
        // GC if memory usage is above threshold
        val memoryUsagePercent = currentMemoryMB / (currentMemoryMB + availableMemoryMB)
        if (memoryUsagePercent > 0.8f) return true
        
        // GC based on memory growth pattern
        val recentGCs = gcHistory.filter { now - it.timestamp < 300_000 } // Last 5 minutes
        if (recentGCs.isNotEmpty()) {
            val avgMemoryAfterGC = recentGCs.map { it.memoryAfterMB }.average().toFloat()
            if (currentMemoryMB > avgMemoryAfterGC * 1.5f) return true
        }
        
        return false
    }
    
    /**
     * Record garbage collection event.
     */
    fun recordGarbageCollection(
        memoryBeforeMB: Float,
        memoryAfterMB: Float,
        durationMs: Long
    ) {
        lastGCTime = Clock.System.now().toEpochMilliseconds()
        gcHistory.add(
            GCEvent(
                timestamp = lastGCTime,
                memoryBeforeMB = memoryBeforeMB,
                memoryAfterMB = memoryAfterMB,
                durationMs = durationMs
            )
        )
        
        // Keep only last 20 events
        if (gcHistory.size > 20) {
            gcHistory.removeAt(0)
        }
    }
    
    /**
     * Get garbage collection statistics.
     */
    fun getGCStats(): GCStats {
        val recentGCs = gcHistory.filter { 
            Clock.System.now().toEpochMilliseconds() - it.timestamp < 3600_000 // Last hour
        }
        
        if (recentGCs.isEmpty()) {
            return GCStats(0, 0f, 0L, 0f)
        }
        
        return GCStats(
            eventCount = recentGCs.size,
            avgMemoryFreedMB = recentGCs.map { it.memoryBeforeMB - it.memoryAfterMB }.average().toFloat(),
            avgDurationMs = recentGCs.map { it.durationMs }.average().toLong(),
            efficiency = recentGCs.map { 
                (it.memoryBeforeMB - it.memoryAfterMB) / it.memoryBeforeMB 
            }.average().toFloat()
        )
    }
}

data class GCStats(
    val eventCount: Int,
    val avgMemoryFreedMB: Float,
    val avgDurationMs: Long,
    val efficiency: Float // 0.0 to 1.0
)