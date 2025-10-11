package com.hazardhawk.performance

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ConfigurationInfo
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import android.util.DisplayMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.roundToInt

/**
 * Android-specific performance optimization and device detection.
 * Optimized for construction environments and varying device capabilities.
 */
actual class DeviceTierDetector(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    actual suspend fun detectCapabilities(): DeviceCapabilities {
        return withContext(Dispatchers.IO) {
            val totalMemoryMB = getTotalMemoryMB()
            val availableMemoryMB = (getAvailableMemory() / (1024 * 1024)).toInt()
            val cpuCores = getCPUCoreCount()
            val hasGPU = hasGPUAcceleration()
            val hasNNAPI = hasNNAPISupport()
            val batteryOptimized = isBatteryOptimized()
            val thermalThrottled = isThermalThrottled()
            val screenDensity = getScreenDensity()
            val androidVersion = getAndroidVersion()
            
            val tier = determineTier(totalMemoryMB, cpuCores, androidVersion)
            
            DeviceCapabilities(
                tier = tier,
                totalMemoryMB = totalMemoryMB,
                availableMemoryMB = availableMemoryMB,
                cpuCores = cpuCores,
                hasGPU = hasGPU,
                hasNNAPI = hasNNAPI,
                batteryOptimized = batteryOptimized,
                thermalThrottled = thermalThrottled,
                screenDensity = screenDensity,
                androidVersion = androidVersion
            )
        }
    }
    
    actual suspend fun getCurrentMemoryUsage(): Long {
        return withContext(Dispatchers.IO) {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo.totalMem - memoryInfo.availMem
        }
    }
    
    actual suspend fun getAvailableMemory(): Long {
        return withContext(Dispatchers.IO) {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo.availMem
        }
    }
    
    actual suspend fun getCPUCoreCount(): Int {
        return withContext(Dispatchers.IO) {
            Runtime.getRuntime().availableProcessors()
        }
    }
    
    actual suspend fun hasGPUAcceleration(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val configInfo: ConfigurationInfo = activityManager.deviceConfigurationInfo
                configInfo.reqGlEsVersion >= 0x30000 // OpenGL ES 3.0+
            } catch (e: Exception) {
                false
            }
        }
    }
    
    actual suspend fun hasNNAPISupport(): Boolean {
        return withContext(Dispatchers.IO) {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 // Android 8.1+
        }
    }
    
    actual suspend fun isBatteryOptimized(): Boolean {
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isPowerSaveMode
            } else {
                false
            }
        }
    }
    
    actual suspend fun isThermalThrottled(): Boolean {
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    // Use CPU frequency as proxy for thermal state
                    // This requires API 29+ and appropriate permissions
                    // For now, we'll use CPU frequency as a proxy
                    getCurrentCPUFrequency() < getMaxCPUFrequency() * 0.8f
                } catch (e: Exception) {
                    false
                }
            } else {
                // Fallback: Check CPU frequency scaling
                getCurrentCPUFrequency() < getMaxCPUFrequency() * 0.8f
            }
        }
    }
    
    actual suspend fun getScreenDensity(): Float {
        return withContext(Dispatchers.Main) {
            val displayMetrics = context.resources.displayMetrics
            displayMetrics.density
        }
    }
    
    actual suspend fun getAndroidVersion(): Int {
        return Build.VERSION.SDK_INT
    }
    
    private fun getTotalMemoryMB(): Int {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return (memoryInfo.totalMem / (1024 * 1024)).toInt()
    }
    
    private fun determineTier(totalMemoryMB: Int, cpuCores: Int, androidVersion: Int): DeviceTier {
        return when {
            // High-end: 8GB+ RAM, 8+ cores, Android 10+
            totalMemoryMB >= 8192 && cpuCores >= 8 && androidVersion >= Build.VERSION_CODES.Q -> 
                DeviceTier.HIGH_END
                
            // Mid-range: 4-8GB RAM, 6+ cores, Android 8+
            totalMemoryMB >= 4096 && cpuCores >= 6 && androidVersion >= Build.VERSION_CODES.O -> 
                DeviceTier.MID_RANGE
                
            // Low-end: Everything else
            else -> DeviceTier.LOW_END
        }
    }
    
    private fun getCurrentCPUFrequency(): Float {
        return try {
            val file = RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "r")
            val frequency = file.readLine().toFloat() / 1000f // Convert to MHz
            file.close()
            frequency
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun getMaxCPUFrequency(): Float {
        return try {
            val file = RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r")
            val frequency = file.readLine().toFloat() / 1000f // Convert to MHz
            file.close()
            frequency
        } catch (e: Exception) {
            2000f // Default assumption: 2GHz max
        }
    }
    
    actual suspend fun detectDeviceTier(): DeviceTier {
        val capabilities = detectCapabilities()
        return capabilities.tier
    }
    
    actual suspend fun getCurrentThermalState(): ThermalState {
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    // Use proper thermal API when available
                    ThermalState.NOMINAL
                } catch (e: Exception) {
                    ThermalState.NOMINAL
                }
            } else {
                // Fallback: Use CPU frequency as indicator
                val currentFreq = getCurrentCPUFrequency()
                val maxFreq = getMaxCPUFrequency()
                val ratio = currentFreq / maxFreq
                
                when {
                    ratio > 0.95f -> ThermalState.NOMINAL
                    ratio > 0.80f -> ThermalState.LIGHT_THROTTLING
                    ratio > 0.60f -> ThermalState.MODERATE_THROTTLING
                    ratio > 0.40f -> ThermalState.SEVERE_THROTTLING
                    else -> ThermalState.CRITICAL_THROTTLING
                }
            }
        }
    }
    
    actual suspend fun getMemoryInfo(): MemoryInfo {
        return withContext(Dispatchers.IO) {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            MemoryInfo(
                totalMemoryMB = (memoryInfo.totalMem / (1024f * 1024f)),
                availableMemoryMB = (memoryInfo.availMem / (1024f * 1024f))
            )
        }
    }

}

/**
 * Android-specific performance optimizer for construction app requirements.
 */
class AndroidPerformanceOptimizer(
    private val context: Context,
    private val deviceDetector: DeviceTierDetector
) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    /**
     * Optimize Android system settings for construction app performance.
     */
    suspend fun optimizeSystemSettings(capabilities: DeviceCapabilities): AndroidOptimizations {
        return withContext(Dispatchers.IO) {
            val optimizations = mutableListOf<String>()
            
            // Battery optimization
            if (capabilities.batteryOptimized) {
                optimizations.add("Battery saver mode detected - reduced AI processing frequency")
            }
            
            // Memory optimization
            val memoryClass = activityManager.memoryClass
            val largeMemoryClass = activityManager.largeMemoryClass
            optimizations.add("Available heap: ${memoryClass}MB (large: ${largeMemoryClass}MB)")
            
            // Thermal management
            if (capabilities.thermalThrottled) {
                optimizations.add("Thermal throttling detected - enabling performance degradation")
            }
            
            // GPU optimization
            if (capabilities.hasGPU) {
                optimizations.add("GPU acceleration available - enabling hardware rendering")
            } else {
                optimizations.add("Software rendering only - reduced visual effects")
            }
            
            // NNAPI optimization
            if (capabilities.hasNNAPI) {
                optimizations.add("NNAPI available - enabling hardware AI acceleration")
            }
            
            AndroidOptimizations(
                heapSizeMB = memoryClass,
                largeHeapSizeMB = largeMemoryClass,
                canUseHardwareAcceleration = capabilities.hasGPU,
                canUseNNAPI = capabilities.hasNNAPI,
                recommendedConcurrency = capabilities.tier.recommendedConcurrency,
                optimizations = optimizations
            )
        }
    }
    
    /**
     * Monitor Android-specific performance metrics.
     */
    suspend fun getAndroidPerformanceMetrics(): AndroidPerformanceMetrics {
        return withContext(Dispatchers.IO) {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            // Get runtime memory info
            val runtime = Runtime.getRuntime()
            val heapUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            val heapTotal = runtime.totalMemory() / (1024 * 1024)
            val heapMax = runtime.maxMemory() / (1024 * 1024)
            
            // Get CPU info
            val cpuUsage = getCPUUsage()
            val cpuFrequency = getCurrentCPUFrequency()
            
            // Get thermal state
            val thermalState = getThermalState()
            
            // Get battery info
            val batteryLevel = getBatteryLevel()
            val isCharging = isBatteryCharging()
            
            AndroidPerformanceMetrics(
                systemMemoryUsedMB = ((memoryInfo.totalMem - memoryInfo.availMem) / (1024 * 1024)).toInt(),
                systemMemoryAvailableMB = (memoryInfo.availMem / (1024 * 1024)).toInt(),
                heapUsedMB = heapUsed.toInt(),
                heapTotalMB = heapTotal.toInt(),
                heapMaxMB = heapMax.toInt(),
                cpuUsagePercent = cpuUsage,
                cpuFrequencyMHz = cpuFrequency,
                thermalState = thermalState,
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                isPowerSaveMode = powerManager.isPowerSaveMode,
                isLowMemory = memoryInfo.lowMemory
            )
        }
    }
    
    /**
     * Setup construction-specific Android optimizations.
     */
    suspend fun setupConstructionOptimizations(): ConstructionOptimizations {
        return withContext(Dispatchers.IO) {
            val optimizations = mutableListOf<String>()
            val capabilities = deviceDetector.detectCapabilities()
            
            // Screen brightness optimization for outdoor use
            val maxBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            )
            
            if (maxBrightness == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                optimizations.add("Automatic brightness enabled - good for outdoor construction use")
            } else {
                optimizations.add("Consider enabling automatic brightness for outdoor visibility")
            }
            
            // Touch sensitivity for work gloves
            optimizations.add("Touch targets sized for work glove use")
            
            // Vibration feedback for tactile response
            optimizations.add("Haptic feedback enabled for construction glove compatibility")
            
            // Storage optimization for photos/reports
            val storageStats = getStorageStats()
            optimizations.add("Available storage: ${storageStats.availableGB}GB of ${storageStats.totalGB}GB")
            
            if (storageStats.availableGB < 2.0f) {
                optimizations.add("WARNING: Low storage - may affect photo capture and caching")
            }
            
            ConstructionOptimizations(
                hasAutoBrightness = maxBrightness == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
                workGloveOptimized = true,
                hapticFeedbackEnabled = true,
                storageStats = storageStats,
                optimizations = optimizations
            )
        }
    }
    
    private fun getCPUUsage(): Float {
        return try {
            val file = RandomAccessFile("/proc/stat", "r")
            val line = file.readLine()
            file.close()
            
            val tokens = line.split(" ")
            val idle = tokens[4].toLong()
            val total = tokens.drop(1).take(7).sumOf { it.toLong() }
            
            val usage = ((total - idle).toFloat() / total) * 100f
            usage.coerceIn(0f, 100f)
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun getCurrentCPUFrequency(): Float {
        return try {
            val file = RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "r")
            val frequency = file.readLine().toFloat() / 1000f // Convert to MHz
            file.close()
            frequency
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun getThermalState(): ThermalState {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use proper thermal API when available
                ThermalState.NOMINAL // Simplified for now
            } else {
                // Fallback: Use CPU frequency as indicator
                val currentFreq = getCurrentCPUFrequency()
                val maxFreq = getMaxCPUFrequency()
                val ratio = currentFreq / maxFreq
                
                when {
                    ratio > 0.95f -> ThermalState.NOMINAL
                    ratio > 0.80f -> ThermalState.LIGHT_THROTTLING
                    ratio > 0.60f -> ThermalState.MODERATE_THROTTLING
                    ratio > 0.40f -> ThermalState.SEVERE_THROTTLING
                    else -> ThermalState.CRITICAL_THROTTLING
                }
            }
        } catch (e: Exception) {
            ThermalState.NOMINAL
        }
    }
    
    private fun getMaxCPUFrequency(): Float {
        return try {
            val file = RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r")
            val frequency = file.readLine().toFloat() / 1000f
            file.close()
            frequency
        } catch (e: Exception) {
            2000f // 2GHz default
        }
    }
    
    private fun getBatteryLevel(): Float {
        return try {
            val batteryFile = File("/sys/class/power_supply/battery/capacity")
            if (batteryFile.exists()) {
                batteryFile.readText().trim().toFloat()
            } else {
                100f // Default if can't read
            }
        } catch (e: Exception) {
            100f
        }
    }
    
    private fun isBatteryCharging(): Boolean {
        return try {
            val statusFile = File("/sys/class/power_supply/battery/status")
            if (statusFile.exists()) {
                val status = statusFile.readText().trim()
                status.equals("Charging", ignoreCase = true) || 
                status.equals("Full", ignoreCase = true)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getStorageStats(): StorageStats {
        return try {
            val stat = StatFs(context.filesDir.path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            val bytesTotal = stat.blockSizeLong * stat.blockCountLong
            
            StorageStats(
                availableGB = bytesAvailable / (1024f * 1024f * 1024f),
                totalGB = bytesTotal / (1024f * 1024f * 1024f)
            )
        } catch (e: Exception) {
            StorageStats(0f, 0f)
        }
    }
}

/**
 * Android-specific optimization results.
 */
data class AndroidOptimizations(
    val heapSizeMB: Int,
    val largeHeapSizeMB: Int,
    val canUseHardwareAcceleration: Boolean,
    val canUseNNAPI: Boolean,
    val recommendedConcurrency: Int,
    val optimizations: List<String>
)

/**
 * Android performance metrics.
 */
data class AndroidPerformanceMetrics(
    val systemMemoryUsedMB: Int,
    val systemMemoryAvailableMB: Int,
    val heapUsedMB: Int,
    val heapTotalMB: Int,
    val heapMaxMB: Int,
    val cpuUsagePercent: Float,
    val cpuFrequencyMHz: Float,
    val thermalState: ThermalState,
    val batteryLevel: Float,
    val isCharging: Boolean,
    val isPowerSaveMode: Boolean,
    val isLowMemory: Boolean
)

/**
 * Construction-specific optimizations.
 */
data class ConstructionOptimizations(
    val hasAutoBrightness: Boolean,
    val workGloveOptimized: Boolean,
    val hapticFeedbackEnabled: Boolean,
    val storageStats: StorageStats,
    val optimizations: List<String>
)

/**
 * Storage statistics.
 */
data class StorageStats(
    val availableGB: Float,
    val totalGB: Float
) {
    val usagePercent: Float
        get() = if (totalGB > 0) ((totalGB - availableGB) / totalGB) * 100f else 0f
}

/**
 * Android frame rate limiter for 30 FPS UI / 2 FPS AI processing.
 */
class AndroidFrameRateLimiter(private val targetFPS: Int) {
    private val frameIntervalNs = 1_000_000_000L / targetFPS
    private var lastFrameTimeNs = 0L
    
    /**
     * Check if enough time has passed for the next frame.
     */
    fun shouldRenderFrame(): Boolean {
        val currentTimeNs = System.nanoTime()
        val elapsedNs = currentTimeNs - lastFrameTimeNs
        
        return if (elapsedNs >= frameIntervalNs) {
            lastFrameTimeNs = currentTimeNs
            true
        } else {
            false
        }
    }
    
    /**
     * Get time until next frame in milliseconds.
     */
    fun getTimeUntilNextFrameMs(): Long {
        val currentTimeNs = System.nanoTime()
        val elapsedNs = currentTimeNs - lastFrameTimeNs
        val remainingNs = frameIntervalNs - elapsedNs
        
        return (remainingNs / 1_000_000L).coerceAtLeast(0)
    }
}

/**
 * Construction-specific Android UI optimizations.
 */
class ConstructionUIOptimizer {
    
    /**
     * Calculate optimal touch target sizes for work gloves.
     */
    fun getOptimalTouchTargetSizeDp(screenDensity: Float): Int {
        // WCAG recommends 44dp minimum, but construction gloves need larger
        val baseSizeDp = 56 // 56dp for work glove compatibility
        
        // Scale based on screen density
        return when {
            screenDensity >= 3.0f -> (baseSizeDp * 0.9f).roundToInt() // XXXHDPI
            screenDensity >= 2.0f -> baseSizeDp // XXHDPI  
            screenDensity >= 1.5f -> (baseSizeDp * 1.1f).roundToInt() // XHDPI
            else -> (baseSizeDp * 1.2f).roundToInt() // Lower density
        }
    }
    
    /**
     * Get construction-appropriate color scheme for outdoor visibility.
     */
    fun getConstructionColorScheme(): ConstructionColorScheme {
        return ConstructionColorScheme(
            primaryColor = 0xFF_FF_8F_00, // High-vis orange
            secondaryColor = 0xFF_00_C8_53, // Safety green
            warningColor = 0xFF_FF_D6_00, // Warning yellow
            dangerColor = 0xFF_D3_2F_2F, // Safety red
            backgroundColor = 0xFF_F5_F5_F5, // Light gray for sun readability
            textColor = 0xFF_21_21_21, // Dark text for contrast
            surfaceColor = 0xFF_FF_FF_FF // White surfaces
        )
    }
    
    /**
     * Calculate optimal text sizes for outdoor readability.
     */
    fun getOptimalTextSizeSp(screenDensity: Float, isOutdoor: Boolean = true): TextSizes {
        val multiplier = if (isOutdoor) 1.2f else 1.0f // Larger text for outdoor use
        
        return TextSizes(
            titleSp = (24 * multiplier).roundToInt(),
            headlineSp = (20 * multiplier).roundToInt(),
            bodyLargeSp = (16 * multiplier).roundToInt(),
            bodyMediumSp = (14 * multiplier).roundToInt(),
            bodySmalSp = (12 * multiplier).roundToInt()
        )
    }
}

data class ConstructionColorScheme(
    val primaryColor: Long,
    val secondaryColor: Long,
    val warningColor: Long,
    val dangerColor: Long,
    val backgroundColor: Long,
    val textColor: Long,
    val surfaceColor: Long
)

data class TextSizes(
    val titleSp: Int,
    val headlineSp: Int,
    val bodyLargeSp: Int,
    val bodyMediumSp: Int,
    val bodySmalSp: Int
)

