package com.hazardhawk.ui.glass

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.view.Choreographer
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlin.math.roundToLong

/**
 * Android implementation of glass morphism performance monitoring.
 * Tracks frame rates, memory usage, battery impact, and thermal state.
 */
class GlassPerformanceMonitor(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    private var isMonitoring = false
    private var monitoringJob: Job? = null
    private var frameCallback: Choreographer.FrameCallback? = null
    
    // Performance tracking
    private var frameCount = 0
    private var lastFrameTime = 0L
    private var monitoringStartTime = 0L
    private var frameRates = mutableListOf<Double>()
    private var memoryUsages = mutableListOf<Long>()
    
    // Current metrics
    private var _currentMetrics = mutableStateOf<GlassPerformanceMetrics?>(null)
    val currentMetrics: State<GlassPerformanceMetrics?> = _currentMetrics
    
    /**
     * Start performance monitoring with frame rate tracking
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        monitoringStartTime = SystemClock.elapsedRealtime()
        frameCount = 0
        lastFrameTime = monitoringStartTime
        frameRates.clear()
        memoryUsages.clear()
        
        // Start frame rate monitoring
        startFrameRateMonitoring()
        
        // Start periodic system metrics monitoring
        monitoringJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isMonitoring) {
                updateMetrics()
                delay(1000) // Update every second
            }
        }
    }
    
    /**
     * Stop performance monitoring and return final metrics
     */
    fun stopMonitoring(): GlassPerformanceMetrics? {
        isMonitoring = false
        monitoringJob?.cancel()
        stopFrameRateMonitoring()
        
        return _currentMetrics.value
    }
    
    /**
     * Get current device capability level
     */
    fun getDeviceCapability(): GlassSupportLevel {
        val detector = GlassCapabilityDetector(context)
        return detector.detectCapability()
    }
    
    /**
     * Check if current performance meets requirements
     */
    fun validateCurrentPerformance(): List<String> {
        val metrics = _currentMetrics.value ?: return listOf("No performance data available")
        return metrics.validate()
    }
    
    /**
     * Get performance recommendation based on current metrics
     */
    fun getPerformanceRecommendation(): PerformanceRecommendation {
        val metrics = _currentMetrics.value ?: return PerformanceRecommendation.UNKNOWN
        val violations = metrics.validate()
        
        return when {
            violations.isEmpty() -> PerformanceRecommendation.OPTIMAL
            violations.size <= 2 && metrics.frameRate >= GlassPerformanceMetrics.MIN_FRAME_RATE_FPS * 0.8 -> 
                PerformanceRecommendation.REDUCE_EFFECTS
            violations.size > 2 || metrics.frameRate < GlassPerformanceMetrics.MIN_FRAME_RATE_FPS * 0.6 -> 
                PerformanceRecommendation.DISABLE_GLASS
            else -> PerformanceRecommendation.MONITOR_CLOSELY
        }
    }
    
    private fun startFrameRateMonitoring() {
        frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isMonitoring) return
                
                frameCount++
                val currentTime = SystemClock.elapsedRealtime()
                
                // Calculate instantaneous frame rate every 60 frames
                if (frameCount % 60 == 0) {
                    val elapsedTime = currentTime - lastFrameTime
                    if (elapsedTime > 0) {
                        val currentFps = (60 * 1000.0) / elapsedTime
                        frameRates.add(currentFps)
                        
                        // Keep only recent frame rates (last 10 measurements)
                        if (frameRates.size > 10) {
                            frameRates.removeFirst()
                        }
                    }
                    lastFrameTime = currentTime
                }
                
                // Schedule next frame callback
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        
        Choreographer.getInstance().postFrameCallback(frameCallback!!)
    }
    
    private fun stopFrameRateMonitoring() {
        frameCallback?.let { callback ->
            Choreographer.getInstance().removeFrameCallback(callback)
        }
        frameCallback = null
    }
    
    private suspend fun updateMetrics() {
        val currentTime = SystemClock.elapsedRealtime()
        val elapsedTime = currentTime - monitoringStartTime
        
        if (elapsedTime <= 0) return
        
        // Calculate average frame rate
        val avgFrameRate = if (frameRates.isNotEmpty()) {
            frameRates.average()
        } else {
            // Fallback calculation
            (frameCount * 1000.0) / elapsedTime
        }
        
        // Get current memory usage
        val memoryUsage = getCurrentMemoryUsageMB()
        memoryUsages.add(memoryUsage)
        if (memoryUsages.size > 10) {
            memoryUsages.removeFirst()
        }
        
        // Estimate battery impact
        val batteryImpact = estimateBatteryImpact(avgFrameRate)
        
        // Calculate load time (time to first frame or 0 if already running)
        val loadTime = if (frameCount > 0) 0L else elapsedTime
        
        // Estimate GPU utilization
        val gpuUtilization = estimateGpuUtilization()
        
        // Get thermal state
        val thermalState = getCurrentThermalState()
        
        _currentMetrics.value = GlassPerformanceMetrics(
            frameRate = avgFrameRate,
            memoryUsageMB = memoryUsages.average().roundToLong(),
            batteryImpactPercent = batteryImpact,
            loadTimeMs = loadTime,
            gpuUtilizationPercent = gpuUtilization,
            thermalState = thermalState
        )
    }
    
    private fun getCurrentMemoryUsageMB(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        // Get app memory usage
        val myPid = android.os.Process.myPid()
        val processMemInfo = activityManager.getProcessMemoryInfo(intArrayOf(myPid))
        
        return if (processMemInfo.isNotEmpty()) {
            // Convert from KB to MB
            (processMemInfo[0].totalPss / 1024).toLong()
        } else {
            // Fallback to system memory calculation
            val usedMemory = memInfo.totalMem - memInfo.availMem
            (usedMemory / (1024 * 1024))
        }
    }
    
    private fun estimateBatteryImpact(frameRate: Double): Double {
        // Estimate based on frame rate and glass effects usage
        val baseImpact = when {
            frameRate >= 60.0 -> 5.0  // Minimal impact at 60fps
            frameRate >= 45.0 -> 8.0  // Low impact at 45fps
            frameRate >= 30.0 -> 12.0 // Medium impact at 30fps
            else -> 20.0              // High impact below 30fps
        }
        
        // Adjust for thermal state
        val thermalMultiplier = when (getCurrentThermalState()) {
            ThermalState.NOMINAL -> 1.0
            ThermalState.LIGHT -> 1.2
            ThermalState.MODERATE -> 1.5
            ThermalState.SEVERE -> 2.0
            ThermalState.CRITICAL -> 3.0
        }
        
        // Adjust for power saving mode
        val powerSaveMultiplier = if (isPowerSavingEnabled()) 1.5 else 1.0
        
        return (baseImpact * thermalMultiplier * powerSaveMultiplier).coerceAtMost(50.0)
    }
    
    private fun estimateGpuUtilization(): Double {
        // Estimate based on device capability and current frame rate
        val deviceCapability = getDeviceCapability()
        val currentFps = frameRates.lastOrNull() ?: 60.0
        
        val baseUtilization = when (deviceCapability) {
            GlassSupportLevel.FULL -> 60.0
            GlassSupportLevel.REDUCED -> 40.0
            GlassSupportLevel.DISABLED -> 10.0
        }
        
        // Adjust based on actual frame rate vs target
        val targetFps = 60.0
        val fpsRatio = currentFps / targetFps
        
        return (baseUtilization * (2.0 - fpsRatio)).coerceIn(0.0, 100.0)
    }
    
    private fun getCurrentThermalState(): ThermalState {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> ThermalState.NOMINAL
                PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.LIGHT
                PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE
                PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.CRITICAL
                else -> ThermalState.NOMINAL
            }
        } else {
            ThermalState.NOMINAL
        }
    }
    
    private fun isPowerSavingEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager.isPowerSaveMode
        } else {
            false
        }
    }
}

/**
 * Performance recommendations based on current metrics
 */
enum class PerformanceRecommendation {
    OPTIMAL,         // Performance is good, continue with full effects
    REDUCE_EFFECTS,  // Reduce glass effects intensity
    DISABLE_GLASS,   // Disable glass effects completely
    MONITOR_CLOSELY, // Watch performance closely
    UNKNOWN          // No data available
}

/**
 * Composable function for monitoring glass performance
 */
@Composable
fun rememberGlassPerformanceMonitor(context: Context): GlassPerformanceMonitor {
    return remember { GlassPerformanceMonitor(context) }
}

/**
 * Performance metrics display composable for debugging
 */
@Composable
fun GlassPerformanceDisplay(
    monitor: GlassPerformanceMonitor,
    visible: Boolean = false
) {
    val metrics by monitor.currentMetrics
    
    if (visible && metrics != null) {
        androidx.compose.material3.Card(
            modifier = androidx.compose.ui.Modifier
                .androidx.compose.foundation.layout.fillMaxWidth()
                .androidx.compose.foundation.layout.padding(8.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)
            )
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier.androidx.compose.foundation.layout.padding(12.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "Glass Performance Metrics",
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = androidx.compose.ui.graphics.Color.White
                )
                
                androidx.compose.foundation.layout.Spacer(
                    modifier = androidx.compose.ui.Modifier.androidx.compose.foundation.layout.height(8.dp)
                )
                
                with(metrics!!) {
                    androidx.compose.material3.Text(
                        text = "FPS: ${frameRate.roundToLong()}",
                        color = if (frameRate >= 45.0) androidx.compose.ui.graphics.Color.Green 
                               else androidx.compose.ui.graphics.Color.Red,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                    
                    androidx.compose.material3.Text(
                        text = "Memory: ${memoryUsageMB}MB",
                        color = if (memoryUsageMB <= 50) androidx.compose.ui.graphics.Color.Green 
                               else androidx.compose.ui.graphics.Color.Red,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                    
                    androidx.compose.material3.Text(
                        text = "Battery: ${batteryImpactPercent.roundToLong()}%",
                        color = if (batteryImpactPercent <= 15.0) androidx.compose.ui.graphics.Color.Green 
                               else androidx.compose.ui.graphics.Color.Red,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                    
                    androidx.compose.material3.Text(
                        text = "Thermal: $thermalState",
                        color = when (thermalState) {
                            ThermalState.NOMINAL, ThermalState.LIGHT -> androidx.compose.ui.graphics.Color.Green
                            ThermalState.MODERATE -> androidx.compose.ui.graphics.Color.Yellow
                            ThermalState.SEVERE, ThermalState.CRITICAL -> androidx.compose.ui.graphics.Color.Red
                        },
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
                
                val recommendation = monitor.getPerformanceRecommendation()
                androidx.compose.material3.Text(
                    text = "Recommendation: $recommendation",
                    color = when (recommendation) {
                        PerformanceRecommendation.OPTIMAL -> androidx.compose.ui.graphics.Color.Green
                        PerformanceRecommendation.REDUCE_EFFECTS, 
                        PerformanceRecommendation.MONITOR_CLOSELY -> androidx.compose.ui.graphics.Color.Yellow
                        PerformanceRecommendation.DISABLE_GLASS -> androidx.compose.ui.graphics.Color.Red
                        PerformanceRecommendation.UNKNOWN -> androidx.compose.ui.graphics.Color.Gray
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}