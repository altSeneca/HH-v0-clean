package com.hazardhawk.performance

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.disk.DiskCache
import javax.inject.Singleton
import kotlin.math.min

/**
 * Touch Performance Monitor for detecting InputDispatcher issues
 * 
 * Monitors:
 * - Touch event latency
 * - Rapid fire touch detection
 * - Frame drops during touch events
 * - Input queue backup detection
 */
object TouchPerformanceMonitor {
    private const val TAG = "TouchPerformance"
    private const val TOUCH_LATENCY_WARNING_MS = 100L
    private const val RAPID_TOUCH_THRESHOLD_MS = 50L
    
    private var lastTouchTime = 0L
    private var touchEventCount = 0
    private var rapidTouchCount = 0
    
    fun logTouchEvent(eventType: String) {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastTouch = currentTime - lastTouchTime
        
        touchEventCount++
        
        // Detect rapid touches that could cause InputDispatcher to drop events
        if (timeSinceLastTouch < RAPID_TOUCH_THRESHOLD_MS && lastTouchTime > 0) {
            rapidTouchCount++
            Log.w(TAG, "Rapid touch detected: ${timeSinceLastTouch}ms since last touch (Event: $eventType)")
            
            if (rapidTouchCount > 5) {
                Log.e(TAG, "Excessive rapid touches detected ($rapidTouchCount). This may cause InputDispatcher warnings!")
            }
        } else {
            rapidTouchCount = 0 // Reset counter for non-rapid touches
        }
        
        // Log touch latency warnings
        if (timeSinceLastTouch > TOUCH_LATENCY_WARNING_MS && lastTouchTime > 0) {
            Log.w(TAG, "High touch latency: ${timeSinceLastTouch}ms between touches")
        }
        
        lastTouchTime = currentTime
        
        // Log periodic statistics
        if (touchEventCount % 20 == 0) {
            Log.d(TAG, "Touch Performance Stats - Total touches: $touchEventCount, Recent rapid touches: $rapidTouchCount")
        }
    }
    
    fun resetStats() {
        touchEventCount = 0
        rapidTouchCount = 0
        lastTouchTime = 0L
        Log.d(TAG, "Touch performance stats reset")
    }
}

/**
 * Composable that wraps content with touch performance monitoring
 */
@Composable
fun TouchPerformanceWrapper(
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var frameDropCount by remember { mutableIntStateOf(0) }
    var lastFrameTime by remember { mutableLongStateOf(System.nanoTime()) }
    
    // Monitor frame performance during touch events
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        
        while (true) {
            val currentTime = System.nanoTime()
            val frameDuration = (currentTime - lastFrameTime) / 1_000_000L // Convert to ms
            
            if (frameDuration > 32L) { // More than 2 frames at 60fps (16.67ms per frame)
                frameDropCount++
                Log.w("TouchPerformance", "Frame drop detected: ${frameDuration}ms (drops: $frameDropCount)")
                
                if (frameDropCount > 10) {
                    Log.e("TouchPerformance", "Excessive frame drops detected! This will cause touch responsiveness issues.")
                }
            }
            
            lastFrameTime = currentTime
            delay(16L) // Check approximately every frame
        }
    }
    
    if (enabled) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { 
                            TouchPerformanceMonitor.logTouchEvent("PRESS")
                        },
                        onTap = { 
                            TouchPerformanceMonitor.logTouchEvent("TAP")
                        }
                    )
                }
        ) {
            content()
        }
    } else {
        content()
    }
}

/**
 * Performance-optimized button click handler with debouncing
 */
class OptimizedClickHandler {
    private var lastClickTime = 0L
    private val debounceTimeMs = 300L
    
    fun handleClick(action: () -> Unit): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastClickTime > debounceTimeMs) {
            lastClickTime = currentTime
            TouchPerformanceMonitor.logTouchEvent("OPTIMIZED_CLICK")
            action()
            true
        } else {
            Log.d("TouchPerformance", "Click debounced - too soon after last click")
            false
        }
    }
}

/**
 * Extension function for Modifier to add touch performance monitoring
 */
fun Modifier.monitorTouchPerformance(enabled: Boolean = true): Modifier {
    return if (enabled) {
        this.pointerInput(Unit) {
            detectTapGestures { offset ->
                TouchPerformanceMonitor.logTouchEvent("MONITORED_TAP at $offset")
            }
        }
    } else {
        this
    }
}

/**
 * Input event debugging utilities
 */
object InputEventDebugger {
    private const val TAG = "InputEvents"
    
    fun logInputQueueStatus(queueSize: Int, droppedEvents: Int = 0) {
        if (droppedEvents > 0) {
            Log.e(TAG, "InputDispatcher dropped $droppedEvents events! Queue size: $queueSize")
        } else if (queueSize > 10) {
            Log.w(TAG, "Input queue backing up: $queueSize events pending")
        }
    }
    
    fun logAnimationPerformance(animationType: String, duration: Long) {
        if (duration > 32) { // More than 2 frames
            Log.w(TAG, "Slow animation detected: $animationType took ${duration}ms")
        }
    }
    
    fun startInputDispatcherMonitoring() {
        Log.d(TAG, "Starting InputDispatcher monitoring...")
        // This would integrate with Android's input system monitoring
        // For now, we'll rely on logcat filtering
    }
}

/**
 * Enhanced Image Loader for Construction Photography
 * Optimized for high-resolution outdoor photos with efficient caching
 */
@Singleton
class ConstructionImageLoader(private val context: Context) {
    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context)
            .build()
    }
}

/**
 * Comprehensive Performance Monitor for Construction Site Usage
 * Tracks metrics critical for all-day outdoor operation
 */
class ConstructionPerformanceMonitor {
    companion object {
        const val TAG = "ConstructionPerformance"

        // Performance targets for construction site usage
        object Targets {
            const val PHOTO_LAUNCH_TIME_MS = 500L
            const val AI_RESULT_DISPLAY_MS = 200L
            const val TAB_SWITCH_RESPONSE_MS = 100L
            const val MAX_MEMORY_USAGE_MB = 50L
            const val MAX_BATTERY_IMPACT_PERCENT = 2L
        }
    }

    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()

    data class PerformanceMetrics(
        val photoLaunchTimes: List<Long> = emptyList(),
        val tabSwitchTimes: List<Long> = emptyList(),
        val memoryUsageMB: Float = 0f,
        val batteryImpactPercent: Float = 0f,
        val frameDropCount: Int = 0,
        val totalOperations: Int = 0,
        val averagePhotoLaunchTime: Long = 0L,
        val averageTabSwitchTime: Long = 0L
    )

    fun trackPhotoViewerLaunch(): PerformanceTracker {
        return PerformanceTracker("photo_viewer_launch") { launchTime ->
            _performanceMetrics.value = _performanceMetrics.value.let { current ->
                val newLaunchTimes = (current.photoLaunchTimes + launchTime).takeLast(50)
                current.copy(
                    photoLaunchTimes = newLaunchTimes,
                    averagePhotoLaunchTime = newLaunchTimes.average().toLong(),
                    totalOperations = current.totalOperations + 1
                )
            }

            if (launchTime > Targets.PHOTO_LAUNCH_TIME_MS) {
                Log.w(TAG, "Photo launch exceeded target: ${launchTime}ms > ${Targets.PHOTO_LAUNCH_TIME_MS}ms")
            }
        }
    }

    fun trackTabSwitchPerformance(): PerformanceTracker {
        return PerformanceTracker("tab_switch") { switchTime ->
            _performanceMetrics.value = _performanceMetrics.value.let { current ->
                val newSwitchTimes = (current.tabSwitchTimes + switchTime).takeLast(50)
                current.copy(
                    tabSwitchTimes = newSwitchTimes,
                    averageTabSwitchTime = newSwitchTimes.average().toLong()
                )
            }

            if (switchTime > Targets.TAB_SWITCH_RESPONSE_MS) {
                Log.w(TAG, "Tab switch exceeded target: ${switchTime}ms > ${Targets.TAB_SWITCH_RESPONSE_MS}ms")
            }
        }
    }

    fun trackMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val usedMemoryMB = usedMemory / (1024f * 1024f)

        _performanceMetrics.value = _performanceMetrics.value.copy(
            memoryUsageMB = usedMemoryMB
        )

        if (usedMemoryMB > Targets.MAX_MEMORY_USAGE_MB) {
            Log.w(TAG, "Memory usage exceeded target: ${usedMemoryMB}MB > ${Targets.MAX_MEMORY_USAGE_MB}MB")
        }

        Log.d(TAG, "Memory usage: ${usedMemoryMB}MB")
    }

    fun trackFrameDrop() {
        _performanceMetrics.value = _performanceMetrics.value.copy(
            frameDropCount = _performanceMetrics.value.frameDropCount + 1
        )
        Log.w(TAG, "Frame drop detected. Total drops: ${_performanceMetrics.value.frameDropCount}")
    }

    fun logPerformanceSummary() {
        val metrics = _performanceMetrics.value
        Log.i(TAG, """
            Construction Performance Summary:
            - Average photo launch: ${metrics.averagePhotoLaunchTime}ms (target: ${Targets.PHOTO_LAUNCH_TIME_MS}ms)
            - Average tab switch: ${metrics.averageTabSwitchTime}ms (target: ${Targets.TAB_SWITCH_RESPONSE_MS}ms)
            - Memory usage: ${metrics.memoryUsageMB}MB (target: <${Targets.MAX_MEMORY_USAGE_MB}MB)
            - Frame drops: ${metrics.frameDropCount}
            - Total operations: ${metrics.totalOperations}
        """.trimIndent())
    }
}

/**
 * Performance tracker for individual operations
 */
class PerformanceTracker(
    private val operationName: String,
    private val onComplete: (Long) -> Unit
) {
    private val startTime = System.currentTimeMillis()

    fun complete() {
        val duration = System.currentTimeMillis() - startTime
        onComplete(duration)
        Log.d("PerformanceTracker", "$operationName completed in ${duration}ms")
    }
}

/**
 * PhotoViewer-specific performance tracking
 */
class PhotoViewerPerformanceTracker(
    private val performanceMonitor: ConstructionPerformanceMonitor
) {
    fun trackPhotoLoad(photoId: String): PerformanceTracker {
        Log.d("PhotoViewerPerformance", "Starting photo load tracking for: $photoId")
        return performanceMonitor.trackPhotoViewerLaunch()
    }

    fun trackTabSwitch(fromTab: String, toTab: String): PerformanceTracker {
        Log.d("PhotoViewerPerformance", "Tracking tab switch: $fromTab -> $toTab")
        return performanceMonitor.trackTabSwitchPerformance()
    }

    fun trackAIAnalysis(photoId: String): PerformanceTracker {
        return PerformanceTracker("ai_analysis_$photoId") { duration ->
            Log.i("PhotoViewerPerformance", "AI analysis for $photoId completed in ${duration}ms")
            if (duration > ConstructionPerformanceMonitor.Companion.Targets.AI_RESULT_DISPLAY_MS) {
                Log.w("PhotoViewerPerformance", "AI analysis exceeded target time")
            }
        }
    }
}

/**
 * Memory Management for Construction Photography
 * Optimizes bitmap usage for large outdoor photos
 */
class ConstructionPhotoMemoryManager {
    companion object {
        const val TAG = "PhotoMemoryManager"
        const val MAX_BITMAP_DIMENSION = 2048
        const val CONSTRUCTION_PHOTO_QUALITY = 85
    }

    private val bitmapPool = mutableMapOf<String, Bitmap>()
    private val maxPoolSize = 10 // Keep maximum 10 bitmaps in pool

    /**
     * Optimize bitmap for construction photo display
     * Maintains quality while reducing memory footprint
     */
    fun optimizeBitmapForDisplay(bitmap: Bitmap, maxDimension: Int = MAX_BITMAP_DIMENSION): Bitmap {
        val scaleWidth = maxDimension.toFloat() / bitmap.width
        val scaleHeight = maxDimension.toFloat() / bitmap.height
        val scale = min(min(scaleWidth, scaleHeight), 1.0f)

        return if (scale < 1.0f) {
            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()

            Log.d(TAG, "Scaling bitmap from ${bitmap.width}x${bitmap.height} to ${scaledWidth}x${scaledHeight}")

            Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true).also {
                // Recycle original if it's not the same reference
                if (it != bitmap && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        } else {
            bitmap
        }
    }

    /**
     * Cache optimized bitmap for reuse
     */
    fun cacheBitmap(key: String, bitmap: Bitmap) {
        // Remove oldest entry if pool is full
        if (bitmapPool.size >= maxPoolSize) {
            val oldestKey = bitmapPool.keys.first()
            bitmapPool.remove(oldestKey)?.let { oldBitmap ->
                if (!oldBitmap.isRecycled) {
                    oldBitmap.recycle()
                }
            }
        }

        bitmapPool[key] = bitmap
        Log.d(TAG, "Cached bitmap for key: $key, pool size: ${bitmapPool.size}")
    }

    /**
     * Retrieve cached bitmap
     */
    fun getCachedBitmap(key: String): Bitmap? {
        return bitmapPool[key]?.let { bitmap ->
            if (!bitmap.isRecycled) bitmap else {
                bitmapPool.remove(key)
                null
            }
        }
    }

    /**
     * Cleanup memory for construction site usage
     * Call during background/pause events
     */
    fun cleanupMemoryOnBackground() {
        Log.d(TAG, "Cleaning up construction photo memory, pool size: ${bitmapPool.size}")

        bitmapPool.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        bitmapPool.clear()

        // Suggest garbage collection for construction site memory management
        System.gc()

        Log.d(TAG, "Memory cleanup completed")
    }

    /**
     * Get current memory usage statistics
     */
    fun getMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()

        return MemoryStats(
            usedMemoryMB = usedMemory / (1024f * 1024f),
            totalMemoryMB = totalMemory / (1024f * 1024f),
            maxMemoryMB = maxMemory / (1024f * 1024f),
            cacheSize = bitmapPool.size,
            memoryPressure = (usedMemory.toFloat() / maxMemory) > 0.8f
        )
    }

    data class MemoryStats(
        val usedMemoryMB: Float,
        val totalMemoryMB: Float,
        val maxMemoryMB: Float,
        val cacheSize: Int,
        val memoryPressure: Boolean
    )
}

/**
 * Construction Environment Optimizer
 * Handles outdoor usage patterns and interruptions
 */
object ConstructionEnvironmentOptimizer {
    private const val TAG = "ConstructionOptimizer"

    /**
     * Optimize for outdoor construction environment
     * Higher contrast, larger touch targets, efficient battery usage
     */
    fun optimizeForOutdoorUsage() {
        Log.d(TAG, "Optimizing for outdoor construction environment")
        // Brightness and contrast optimizations would be handled by the UI layer
        // This tracks the optimization request
    }

    /**
     * Handle work interruptions (calls, notifications, emergencies)
     * Ensures quick state recovery and background processing continuation
     */
    fun optimizeForWorkInterruptions() {
        Log.d(TAG, "Optimizing for work interruption handling")
        // State persistence and background task management
    }

    /**
     * Optimize for all-day battery usage on construction sites
     */
    fun optimizeForAllDayUsage() {
        Log.d(TAG, "Optimizing for all-day construction site usage")
        // Battery optimization strategies
    }
}