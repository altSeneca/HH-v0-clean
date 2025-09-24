package com.hazardhawk.performance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Enhanced ThumbnailCache for smooth gallery performance with lazy loading,
 * intelligent prefetching, and memory-efficient caching strategies
 */
class ThumbnailCache private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ThumbnailCache"
        private const val MEMORY_CLASS_MULTIPLIER = 8 // Use 1/8 of available memory
        private const val MAX_BITMAP_SIZE = 512 // Maximum thumbnail dimension
        private const val PREFETCH_BATCH_SIZE = 20
        private const val CLEANUP_THRESHOLD_MB = 100
        
        @Volatile
        private var INSTANCE: ThumbnailCache? = null
        
        fun getInstance(context: Context): ThumbnailCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThumbnailCache(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    
    // Memory cache for decoded bitmaps
    private val memoryCache: LruCache<String, Bitmap>
    
    // Disk cache for thumbnail files
    private val diskCacheDir = File(context.cacheDir, "thumbnail_cache").apply { mkdirs() }
    
    // Loading state management
    private val loadingStates = ConcurrentHashMap<String, LoadingState>()
    private val _cacheStats = MutableStateFlow(CacheStats())
    val cacheStats: StateFlow<CacheStats> = _cacheStats.asStateFlow()
    
    // Performance metrics
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val diskHitCount = AtomicLong(0)
    private val loadTime = AtomicLong(0)
    private val loadCount = AtomicInteger(0)
    
    init {
        val memoryClass = context.packageManager.getSystemAvailableFeatures()
        val cacheSize = calculateCacheSize()
        
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount
            }
            
            override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
                if (evicted) {
                    Log.d(TAG, "Bitmap evicted from memory cache: $key")
                }
            }
        }
        
        // Start periodic cleanup
        startPeriodicCleanup()
    }
    
    enum class LoadingState {
        IDLE,
        LOADING,
        LOADED,
        ERROR
    }
    
    data class CacheStats(
        val memoryCacheSize: Int = 0,
        val diskCacheSize: Long = 0,
        val hitRatio: Float = 0f,
        val diskHitRatio: Float = 0f,
        val averageLoadTime: Long = 0,
        val totalLoads: Int = 0
    )
    
    data class ThumbnailRequest(
        val photoId: String,
        val photoPath: String,
        val size: Int = MAX_BITMAP_SIZE,
        val priority: Priority = Priority.NORMAL
    )
    
    enum class Priority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }
    
    /**
     * Get thumbnail bitmap with intelligent loading and caching
     */
    suspend fun getThumbnail(request: ThumbnailRequest): Bitmap? {
        val startTime = System.currentTimeMillis()
        
        return try {
            // Check memory cache first
            memoryCache.get(request.photoId)?.let { bitmap ->
                hitCount.incrementAndGet()
                updateAverageLoadTime(System.currentTimeMillis() - startTime)
                return bitmap
            }
            
            missCount.incrementAndGet()
            
            // Check if already loading
            if (loadingStates[request.photoId] == LoadingState.LOADING) {
                return waitForLoading(request.photoId)
            }
            
            loadingStates[request.photoId] = LoadingState.LOADING
            
            // Try disk cache
            val diskCachedBitmap = loadFromDiskCache(request)
            if (diskCachedBitmap != null) {
                diskHitCount.incrementAndGet()
                memoryCache.put(request.photoId, diskCachedBitmap)
                loadingStates[request.photoId] = LoadingState.LOADED
                updateAverageLoadTime(System.currentTimeMillis() - startTime)
                return diskCachedBitmap
            }
            
            // Load from original photo
            val bitmap = loadAndCacheThumbnail(request)
            if (bitmap != null) {
                memoryCache.put(request.photoId, bitmap)
                saveToDiskCache(request.photoId, bitmap)
                loadingStates[request.photoId] = LoadingState.LOADED
            } else {
                loadingStates[request.photoId] = LoadingState.ERROR
            }
            
            updateAverageLoadTime(System.currentTimeMillis() - startTime)
            updateCacheStats()
            
            bitmap
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading thumbnail for ${request.photoId}", e)
            loadingStates[request.photoId] = LoadingState.ERROR
            null
        }
    }
    
    /**
     * Batch load thumbnails for gallery scrolling performance
     */
    suspend fun preloadThumbnails(requests: List<ThumbnailRequest>) {
        withContext(Dispatchers.Default) {
            val priorityRequests = requests.sortedByDescending { it.priority.ordinal }
            
            // Process in batches to avoid overwhelming the system
            priorityRequests.chunked(PREFETCH_BATCH_SIZE).forEach { batch ->
                val jobs = batch.map { request ->
                    cacheScope.launch {
                        if (memoryCache.get(request.photoId) == null && 
                            loadingStates[request.photoId] != LoadingState.LOADED) {
                            getThumbnail(request)
                        }
                    }
                }
                
                // Wait for batch to complete before starting next batch
                jobs.forEach { it.join() }
            }
        }
    }
    
    /**
     * Get loading state for a specific photo
     */
    fun getLoadingState(photoId: String): LoadingState {
        return loadingStates[photoId] ?: LoadingState.IDLE
    }
    
    /**
     * Check if thumbnail exists in cache
     */
    fun hasThumbnail(photoId: String): Boolean {
        return memoryCache.get(photoId) != null || diskCacheExists(photoId)
    }
    
    /**
     * Clear specific thumbnail from cache
     */
    suspend fun evictThumbnail(photoId: String) {
        mutex.withLock {
            memoryCache.remove(photoId)
            deleteDiskCache(photoId)
            loadingStates.remove(photoId)
            updateCacheStats()
        }
    }
    
    /**
     * Clear all cached thumbnails
     */
    suspend fun clearCache() {
        mutex.withLock {
            memoryCache.evictAll()
            clearDiskCache()
            loadingStates.clear()
            updateCacheStats()
        }
        Log.d(TAG, "Cache cleared completely")
    }
    
    /**
     * Perform cache cleanup based on size and age
     */
    suspend fun performCleanup(maxSizeMB: Int = CLEANUP_THRESHOLD_MB): Int {
        return withContext(Dispatchers.IO) {
            var deletedCount = 0
            val maxSizeBytes = maxSizeMB * 1024 * 1024L
            
            try {
                val files = diskCacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return@withContext 0
                var currentSize = files.sumOf { it.length() }
                
                // Delete oldest files if exceeding size limit
                for (file in files) {
                    if (currentSize <= maxSizeBytes) break
                    
                    if (file.delete()) {
                        val photoId = file.nameWithoutExtension.removePrefix("thumb_")
                        memoryCache.remove(photoId)
                        loadingStates.remove(photoId)
                        currentSize -= file.length()
                        deletedCount++
                    }
                }
                
                // Clean up orphaned entries
                val validPhotoIds = files.map { it.nameWithoutExtension.removePrefix("thumb_") }.toSet()
                loadingStates.keys.retainAll(validPhotoIds)
                
                updateCacheStats()
                Log.d(TAG, "Cache cleanup: deleted $deletedCount files, current size: ${currentSize / (1024 * 1024)}MB")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during cache cleanup", e)
            }
            
            deletedCount
        }
    }
    
    // Private methods
    
    private fun calculateCacheSize(): Int {
        val memoryClass = context.packageManager.getSystemAvailableFeatures().size * 1024 * 1024
        val maxMemory = Runtime.getRuntime().maxMemory()
        val cacheSize = maxOf(memoryClass / MEMORY_CLASS_MULTIPLIER, (maxMemory / 8).toInt())
        
        Log.d(TAG, "Memory cache size: ${cacheSize / (1024 * 1024)}MB")
        return cacheSize
    }
    
    private suspend fun waitForLoading(photoId: String): Bitmap? {
        return withContext(Dispatchers.Default) {
            var attempts = 0
            val maxAttempts = 50 // 5 seconds maximum wait
            
            while (loadingStates[photoId] == LoadingState.LOADING && attempts < maxAttempts) {
                kotlinx.coroutines.delay(100)
                attempts++
            }
            
            memoryCache.get(photoId)
        }
    }
    
    private suspend fun loadFromDiskCache(request: ThumbnailRequest): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val cacheFile = File(diskCacheDir, "thumb_${request.photoId}.jpg")
                if (cacheFile.exists()) {
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                        inSampleSize = 1
                    }
                    BitmapFactory.decodeFile(cacheFile.absolutePath, options)
                } else null
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading from disk cache", e)
                null
            }
        }
    }
    
    private suspend fun loadAndCacheThumbnail(request: ThumbnailRequest): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val originalFile = File(request.photoPath)
                if (!originalFile.exists()) {
                    Log.w(TAG, "Original photo does not exist: ${request.photoPath}")
                    return@withContext null
                }
                
                val options = BitmapFactory.Options()
                
                // First pass: get dimensions
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(request.photoPath, options)
                
                // Calculate sample size
                val sampleSize = calculateInSampleSize(options, request.size, request.size)
                
                // Second pass: decode bitmap
                options.inJustDecodeBounds = false
                options.inSampleSize = sampleSize
                options.inPreferredConfig = Bitmap.Config.RGB_565
                
                val bitmap = BitmapFactory.decodeFile(request.photoPath, options)
                
                // Scale to exact thumbnail size if needed
                if (bitmap != null && (bitmap.width > request.size || bitmap.height > request.size)) {
                    val scaledBitmap = createScaledBitmap(bitmap, request.size)
                    if (scaledBitmap != bitmap) {
                        bitmap.recycle()
                    }
                    scaledBitmap
                } else {
                    bitmap
                }
                
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "OOM while creating thumbnail", e)
                // Try with more aggressive downsampling
                tryCreateThumbnailWithFallback(request)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating thumbnail", e)
                null
            }
        }
    }
    
    private fun tryCreateThumbnailWithFallback(request: ThumbnailRequest): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 8  // Very aggressive downsampling
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(request.photoPath, options)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback thumbnail creation failed", e)
            null
        }
    }
    
    private suspend fun saveToDiskCache(photoId: String, bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            try {
                val cacheFile = File(diskCacheDir, "thumb_${photoId}.jpg")
                cacheFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    out.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to disk cache", e)
            }
        }
    }
    
    private fun diskCacheExists(photoId: String): Boolean {
        return File(diskCacheDir, "thumb_${photoId}.jpg").exists()
    }
    
    private fun deleteDiskCache(photoId: String) {
        File(diskCacheDir, "thumb_${photoId}.jpg").delete()
    }
    
    private fun clearDiskCache() {
        diskCacheDir.listFiles()?.forEach { it.delete() }
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    private fun createScaledBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val scale = minOf(
            maxSize.toFloat() / width,
            maxSize.toFloat() / height
        )
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun updateAverageLoadTime(loadTime: Long) {
        this.loadTime.addAndGet(loadTime)
        loadCount.incrementAndGet()
    }
    
    private fun updateCacheStats() {
        val totalRequests = hitCount.get() + missCount.get()
        val hitRatio = if (totalRequests > 0) hitCount.get().toFloat() / totalRequests else 0f
        val diskHitRatio = if (missCount.get() > 0) diskHitCount.get().toFloat() / missCount.get() else 0f
        val avgLoadTime = if (loadCount.get() > 0) loadTime.get() / loadCount.get() else 0L
        val diskSize = diskCacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        
        _cacheStats.value = CacheStats(
            memoryCacheSize = memoryCache.size(),
            diskCacheSize = diskSize,
            hitRatio = hitRatio,
            diskHitRatio = diskHitRatio,
            averageLoadTime = avgLoadTime,
            totalLoads = loadCount.get()
        )
    }
    
    private fun startPeriodicCleanup() {
        cacheScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000) // Clean up every minute
                
                // Perform cleanup if cache is getting large
                val diskSize = diskCacheDir.listFiles()?.sumOf { it.length() } ?: 0L
                if (diskSize > CLEANUP_THRESHOLD_MB * 1024 * 1024L) {
                    performCleanup()
                }
            }
        }
    }
    
    /**
     * Factory methods for common thumbnail requests
     */
    object RequestFactory {
        fun forGalleryItem(photoId: String, photoPath: String): ThumbnailRequest {
            return ThumbnailRequest(
                photoId = photoId,
                photoPath = photoPath,
                size = 300,
                priority = Priority.NORMAL
            )
        }
        
        fun forPreview(photoId: String, photoPath: String): ThumbnailRequest {
            return ThumbnailRequest(
                photoId = photoId,
                photoPath = photoPath,
                size = 150,
                priority = Priority.HIGH
            )
        }
        
        fun forDetailView(photoId: String, photoPath: String): ThumbnailRequest {
            return ThumbnailRequest(
                photoId = photoId,
                photoPath = photoPath,
                size = 512,
                priority = Priority.CRITICAL
            )
        }
    }
}