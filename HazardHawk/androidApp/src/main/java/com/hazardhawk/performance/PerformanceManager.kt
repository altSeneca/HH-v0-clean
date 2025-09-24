package com.hazardhawk.performance

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Central performance manager that coordinates all performance optimizations
 * Integrates CameraPreloader, ImageOptimizer, ThumbnailCache, and BatteryManager
 */
class PerformanceManager private constructor(private val context: Context) : DefaultLifecycleObserver {
    
    companion object {
        private const val TAG = "PerformanceManager"
        
        @Volatile
        private var INSTANCE: PerformanceManager? = null
        
        fun getInstance(context: Context): PerformanceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PerformanceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        /**
         * Initialize performance manager - call from Application.onCreate()
         */
        fun initialize(application: Application) {
            val manager = getInstance(application)
            ProcessLifecycleOwner.get().lifecycle.addObserver(manager)
            Log.d(TAG, "Performance manager initialized")
        }
    }
    
    private val performanceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Performance components
    private val cameraPreloader by lazy { CameraPreloader.getInstance(context) }
    private val imageOptimizer by lazy { ImageOptimizer.getInstance(context) }
    private val thumbnailCache by lazy { ThumbnailCache.getInstance(context) }
    private val batteryManager by lazy { HazardHawkBatteryManager.getInstance(context) }
    
    // Expose important state flows
    val batteryState: StateFlow<HazardHawkBatteryManager.BatteryInfo>
        get() = batteryManager.batteryState
    
    val powerMode: StateFlow<HazardHawkBatteryManager.PowerMode>
        get() = batteryManager.powerMode
    
    val cacheStats: StateFlow<ThumbnailCache.CacheStats>
        get() = thumbnailCache.cacheStats
    
    init {
        startPerformanceOptimizations()
    }
    
    // Camera Performance
    
    /**
     * Start camera preloading for instant startup
     */
    fun startCameraPreloading() {
        cameraPreloader.startPreloading()
    }
    
    /**
     * Check if camera is ready for instant use
     */
    fun isCameraReady(): Boolean {
        return cameraPreloader.isCameraReady()
    }
    
    /**
     * Bind preloaded camera for immediate use
     */
    suspend fun bindPreloadedCamera(
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        onSuccess: (androidx.camera.lifecycle.ProcessCameraProvider, androidx.camera.core.Preview, androidx.camera.core.ImageCapture) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            cameraPreloader.bindPreloadedCamera(lifecycleOwner, onSuccess, onError)
            Log.d(TAG, "Camera binding initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error binding preloaded camera", e)
            onError(e)
        }
    }
    
    // Image Processing Performance
    
    /**
     * Process captured image with optimal settings
     */
    fun processImageAsync(
        inputPath: String,
        priority: ImageOptimizer.ProcessingPriority = ImageOptimizer.ProcessingPriority.NORMAL,
        callback: (ImageOptimizer.ProcessingResult) -> Unit
    ) {
        val settings = getOptimalCompressionSettings()
        imageOptimizer.processImageAsync(inputPath, settings, priority, callback)
    }
    
    /**
     * Create instant preview for captured photo
     */
    suspend fun createInstantPreview(inputPath: String): String? {
        return imageOptimizer.createInstantPreview(inputPath)
    }
    
    /**
     * Get processing status for UI feedback
     */
    fun getImageProcessingStatus(): ImageOptimizer.ProcessingStatus {
        return imageOptimizer.getProcessingStatus()
    }
    
    // Gallery Performance
    
    /**
     * Load thumbnail with intelligent caching
     */
    suspend fun getThumbnail(photoId: String, photoPath: String, size: Int = 300): android.graphics.Bitmap? {
        val request = ThumbnailCache.ThumbnailRequest(
            photoId = photoId,
            photoPath = photoPath,
            size = size,
            priority = ThumbnailCache.Priority.NORMAL
        )
        return thumbnailCache.getThumbnail(request)
    }
    
    /**
     * Preload thumbnails for smooth gallery scrolling
     */
    suspend fun preloadThumbnails(photoData: List<Pair<String, String>>) {
        val requests = photoData.map { (photoId, photoPath) ->
            ThumbnailCache.RequestFactory.forGalleryItem(photoId, photoPath)
        }
        thumbnailCache.preloadThumbnails(requests)
    }
    
    /**
     * Check thumbnail loading state
     */
    fun getThumbnailLoadingState(photoId: String): ThumbnailCache.LoadingState {
        return thumbnailCache.getLoadingState(photoId)
    }
    
    // Battery Optimization
    
    /**
     * Acquire wake lock for camera operations
     */
    fun acquireCameraWakeLock() {
        batteryManager.acquireCameraWakeLock()
    }
    
    /**
     * Release camera wake lock
     */
    fun releaseCameraWakeLock() {
        batteryManager.releaseCameraWakeLock()
    }
    
    /**
     * Schedule background task with battery optimization
     */
    fun scheduleBackgroundTask(
        id: String,
        action: suspend () -> Unit,
        priority: HazardHawkBatteryManager.TaskPriority = HazardHawkBatteryManager.TaskPriority.NORMAL,
        estimatedDurationMs: Long = 5000L
    ) {
        val task = HazardHawkBatteryManager.BackgroundTask(
            id = id,
            action = action,
            priority = priority,
            estimatedDurationMs = estimatedDurationMs
        )
        batteryManager.scheduleBackgroundTask(task)
    }
    
    /**
     * Get optimal settings based on current power mode
     */
    fun shouldUseFlash(): Boolean {
        return batteryManager.shouldUseFlash()
    }
    
    fun getOptimalLocationInterval(): Long {
        return batteryManager.getOptimalLocationInterval()
    }
    
    // Integrated Performance Methods
    
    /**
     * Get optimal compression settings based on battery and performance state
     */
    fun getOptimalCompressionSettings(): ImageOptimizer.CompressionSettings {
        val powerMode = batteryManager.powerMode.value
        val imageQuality = batteryManager.getRecommendedImageQuality()
        
        return when (powerMode) {
            HazardHawkBatteryManager.PowerMode.ULTRA_SAVER -> {
                ImageOptimizer.CompressionSettings(
                    qualityMode = ImageOptimizer.QualityMode.FAST,
                    maxWidth = 1024,
                    maxHeight = 1024,
                    enableProgressiveJPEG = false,
                    preserveExif = false
                )
            }
            HazardHawkBatteryManager.PowerMode.BATTERY_SAVER -> {
                ImageOptimizer.CompressionSettings(
                    qualityMode = ImageOptimizer.QualityMode.BALANCED,
                    maxWidth = 1536,
                    maxHeight = 1536,
                    enableProgressiveJPEG = false,
                    preserveExif = true
                )
            }
            HazardHawkBatteryManager.PowerMode.CHARGING -> {
                ImageOptimizer.CompressionSettings(
                    qualityMode = ImageOptimizer.QualityMode.ULTRA_HIGH,
                    maxWidth = 2048,
                    maxHeight = 2048,
                    enableProgressiveJPEG = true,
                    preserveExif = true
                )
            }
            else -> {
                ImageOptimizer.CompressionSettings(
                    qualityMode = ImageOptimizer.QualityMode.HIGH,
                    maxWidth = 2048,
                    maxHeight = 2048,
                    enableProgressiveJPEG = true,
                    preserveExif = true
                )
            }
        }
    }
    
    /**
     * Perform comprehensive cleanup
     */
    suspend fun performCleanup() {
        Log.d(TAG, "Starting comprehensive performance cleanup")
        
        // Clean up thumbnails
        val deletedThumbnails = thumbnailCache.performCleanup()
        
        // Clean up optimized images
        val deletedImages = imageOptimizer.cleanupOptimizedFiles()
        
        Log.d(TAG, "Cleanup completed: $deletedThumbnails thumbnails, $deletedImages optimized images removed")
    }
    
    /**
     * Get comprehensive performance metrics
     */
    fun getPerformanceMetrics(): PerformanceMetrics {
        val cameraMetrics = cameraPreloader.getPreloadMetrics()
        val processingStatus = imageOptimizer.getProcessingStatus()
        val cacheStats = thumbnailCache.cacheStats.value
        val batteryState = batteryManager.batteryState.value
        
        return PerformanceMetrics(
            cameraPreloadTime = cameraMetrics.preloadTimeMs,
            cameraReady = cameraMetrics.isOptimal(),
            activeImageProcessing = processingStatus.activeOperations,
            thumbnailCacheHitRatio = cacheStats.hitRatio,
            averageThumbnailLoadTime = cacheStats.averageLoadTime,
            batteryLevel = batteryState.level,
            powerMode = batteryState.powerMode.name,
            isCharging = batteryState.isCharging
        )
    }
    
    // Lifecycle callbacks
    
    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "App started - resuming performance optimizations")
        startCameraPreloading()
    }
    
    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "App backgrounded - optimizing for background state")
        
        // Release camera resources when app is backgrounded
        releaseCameraWakeLock()
        
        // Schedule cleanup as background task
        scheduleBackgroundTask(
            id = "background_cleanup",
            action = { performCleanup() },
            priority = HazardHawkBatteryManager.TaskPriority.LOW
        )
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        cleanup()
    }
    
    // Private methods
    
    private fun startPerformanceOptimizations() {
        performanceScope.launch {
            // Start camera preloading
            startCameraPreloading()
            
            // Preload common thumbnail sizes
            // This could be expanded to preload recent photos
        }
    }
    
    private fun cleanup() {
        Log.d(TAG, "Cleaning up performance manager")
        
        batteryManager.cleanup()
        imageOptimizer.clearQueue()
        
        performanceScope.launch {
            thumbnailCache.clearCache()
        }
    }
    
    data class PerformanceMetrics(
        val cameraPreloadTime: Long,
        val cameraReady: Boolean,
        val activeImageProcessing: Int,
        val thumbnailCacheHitRatio: Float,
        val averageThumbnailLoadTime: Long,
        val batteryLevel: Int,
        val powerMode: String,
        val isCharging: Boolean
    ) {
        fun isPerformanceOptimal(): Boolean {
            return cameraReady && 
                   cameraPreloadTime < 2000 && 
                   thumbnailCacheHitRatio > 0.8f &&
                   averageThumbnailLoadTime < 100
        }
    }
}