package com.hazardhawk.ai.yolo

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android-specific YOLO optimizer for device capability detection and performance tuning.
 * Provides intelligent model selection and execution optimization based on device hardware.
 */
class AndroidYOLOOptimizer {
    
    private val tag = "AndroidYOLOOptimizer"
    
    // Device capability cache
    private var cachedCapability: DeviceCapability? = null
    
    /**
     * Assess device capability for optimal YOLO model selection
     */
    suspend fun assessDeviceCapability(): DeviceCapability {
        return withContext(Dispatchers.IO) {
            cachedCapability?.let { return@withContext it }
            
            val deviceType = detectDeviceType()
            val availableMemoryMB = getAvailableMemoryMB()
            val cpuCores = getCPUCoreCount()
            val hasGPU = detectGPUSupport()
            val performanceLevel = calculatePerformanceLevel(
                availableMemoryMB, cpuCores, hasGPU
            )
            
            Log.d(tag, "Device capability assessment:")
            Log.d(tag, "  Device Type: $deviceType")
            Log.d(tag, "  Available Memory: ${availableMemoryMB}MB")
            Log.d(tag, "  CPU Cores: $cpuCores")
            Log.d(tag, "  GPU Support: $hasGPU")
            Log.d(tag, "  Performance Level: $performanceLevel")
            
            val capability = DeviceCapability(
                deviceType = deviceType,
                availableMemoryMB = availableMemoryMB,
                cpuCores = cpuCores,
                hasGPU = hasGPU,
                performanceLevel = performanceLevel
            )
            
            cachedCapability = capability
            capability
        }
    }
    
    /**
     * Get optimal thread count for ONNX inference
     */
    fun getOptimalThreadCount(): Int {
        val cpuCores = getCPUCoreCount()
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        
        // Use 75% of available cores, minimum 2, maximum 8
        val optimalThreads = ((cpuCores * 0.75).toInt()).coerceIn(2, 8)
        
        Log.d(tag, "Optimal thread count: $optimalThreads (CPU cores: $cpuCores, available: $availableProcessors)")
        return optimalThreads
    }
    
    /**
     * Check if GPU acceleration is available and recommended
     */
    fun isGPUAccelerationAvailable(): Boolean {
        return try {
            // Check for NNAPI support (Android 8.1+)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
                Log.d(tag, "GPU acceleration not available: Android version too old")
                return false
            }
            
            // Check for GPU driver availability
            val hasGPU = detectGPUSupport()
            if (!hasGPU) {
                Log.d(tag, "GPU acceleration not available: No GPU detected")
                return false
            }
            
            // Check for sufficient memory (GPU acceleration requires more memory)
            val availableMemory = getAvailableMemoryMB()
            if (availableMemory < 2048) {
                Log.d(tag, "GPU acceleration not recommended: Low memory (${availableMemory}MB)")
                return false
            }
            
            Log.d(tag, "GPU acceleration available and recommended")
            true
        } catch (e: Exception) {
            Log.w(tag, "Error checking GPU acceleration availability", e)
            false
        }
    }
    
    /**
     * Get memory pressure status
     */
    fun getMemoryPressure(): MemoryPressure {
        return try {
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            
            val usedMemory = totalMemory - freeMemory
            val memoryUsageRatio = usedMemory.toFloat() / maxMemory.toFloat()
            
            when {
                memoryUsageRatio > 0.9 -> MemoryPressure.CRITICAL
                memoryUsageRatio > 0.7 -> MemoryPressure.HIGH
                memoryUsageRatio > 0.5 -> MemoryPressure.MEDIUM
                else -> MemoryPressure.LOW
            }
        } catch (e: Exception) {
            Log.w(tag, "Error getting memory pressure", e)
            MemoryPressure.MEDIUM
        }
    }
    
    /**
     * Check if device is experiencing thermal throttling
     */
    fun isThermalThrottling(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use thermal service on API 29+
                val thermalFile = File("/sys/class/thermal/thermal_zone0/temp")
                if (thermalFile.exists()) {
                    val temp = RandomAccessFile(thermalFile, "r").use { it.readLine() }
                    val tempCelsius = temp.toIntOrNull()?.div(1000) ?: 0
                    
                    // Consider throttling if temperature > 70°C
                    return tempCelsius > 70
                }
            }
            
            // Fallback: check CPU frequency scaling
            val cpuFreqFile = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
            if (cpuFreqFile.exists()) {
                val currentFreq = RandomAccessFile(cpuFreqFile, "r").use { it.readLine() }
                val maxFreqFile = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq")
                if (maxFreqFile.exists()) {
                    val maxFreq = RandomAccessFile(maxFreqFile, "r").use { it.readLine() }
                    
                    val current = currentFreq.toLongOrNull() ?: 0L
                    val max = maxFreq.toLongOrNull() ?: 1L
                    
                    // Consider throttling if frequency < 80% of max
                    return current < (max * 0.8)
                }
            }
            
            false
        } catch (e: Exception) {
            Log.w(tag, "Error checking thermal throttling", e)
            false
        }
    }
    
    // Private helper methods
    
    private fun detectDeviceType(): DeviceType {
        return when {
            isTablet() -> DeviceType.TABLET
            isTV() -> DeviceType.TV
            else -> DeviceType.MOBILE_PHONE
        }
    }
    
    private fun isTablet(): Boolean {
        // Simple heuristic based on screen size and density
        return try {
            val displayMetrics = android.content.res.Resources.getSystem().displayMetrics
            val widthDp = displayMetrics.widthPixels / displayMetrics.density
            val heightDp = displayMetrics.heightPixels / displayMetrics.density
            val minDp = kotlin.math.min(widthDp, heightDp)
            minDp >= 600 // Tablets typically have at least 600dp in smallest dimension
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isTV(): Boolean {
        return try {
            // Check for Android TV features
            Build.MODEL.contains("TV", ignoreCase = true) ||
            Build.PRODUCT.contains("TV", ignoreCase = true) ||
            Build.DEVICE.contains("TV", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getAvailableMemoryMB(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            maxMemory / 1024 / 1024 // Convert to MB
        } catch (e: Exception) {
            Log.w(tag, "Error getting available memory", e)
            1024L // Default to 1GB
        }
    }
    
    private fun getCPUCoreCount(): Int {
        return try {
            // Method 1: Use /proc/cpuinfo
            val cpuInfoFile = File("/proc/cpuinfo")
            if (cpuInfoFile.exists()) {
                val cpuInfo = cpuInfoFile.readText()
                val processorPattern = Pattern.compile("processor\\s*:\\s*\\d+")
                val matcher = processorPattern.matcher(cpuInfo)
                var count = 0
                while (matcher.find()) {
                    count++
                }
                if (count > 0) return count
            }
            
            // Method 2: Use Runtime
            Runtime.getRuntime().availableProcessors()
        } catch (e: Exception) {
            Log.w(tag, "Error getting CPU core count", e)
            4 // Default to 4 cores
        }
    }
    
    private fun detectGPUSupport(): Boolean {
        return try {
            // Check for GPU renderer information
            val glRenderer = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER)
            val glVendor = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_VENDOR)
            
            // Look for known GPU vendors
            val hasKnownGPU = glRenderer?.let { renderer ->
                renderer.contains("Adreno", ignoreCase = true) ||
                renderer.contains("Mali", ignoreCase = true) ||
                renderer.contains("PowerVR", ignoreCase = true) ||
                renderer.contains("Tegra", ignoreCase = true) ||
                renderer.contains("Immortalis", ignoreCase = true)
            } ?: false
            
            val hasKnownVendor = glVendor?.let { vendor ->
                vendor.contains("Qualcomm", ignoreCase = true) ||
                vendor.contains("ARM", ignoreCase = true) ||
                vendor.contains("Imagination", ignoreCase = true) ||
                vendor.contains("NVIDIA", ignoreCase = true)
            } ?: false
            
            hasKnownGPU || hasKnownVendor
        } catch (e: Exception) {
            Log.w(tag, "Error detecting GPU support", e)
            // Assume modern devices have GPU support
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        }
    }
    
    private fun calculatePerformanceLevel(
        availableMemoryMB: Long,
        cpuCores: Int,
        hasGPU: Boolean
    ): PerformanceLevel {
        var score = 0
        
        // Memory score (0-3 points)
        score += when {
            availableMemoryMB >= 6144 -> 3 // 6GB+
            availableMemoryMB >= 4096 -> 2 // 4GB+
            availableMemoryMB >= 3072 -> 1 // 3GB+
            else -> 0 // <3GB
        }
        
        // CPU score (0-3 points)
        score += when {
            cpuCores >= 8 -> 3
            cpuCores >= 6 -> 2
            cpuCores >= 4 -> 1
            else -> 0
        }
        
        // GPU score (0-2 points)
        if (hasGPU) score += 2
        
        // Android version score (0-2 points)
        score += when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> 2 // Android 11+
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> 1 // Android 10+
            else -> 0
        }
        
        return when {
            score >= 8 -> PerformanceLevel.HIGH
            score >= 5 -> PerformanceLevel.MEDIUM
            else -> PerformanceLevel.LOW
        }
    }
}

/**
 * Memory pressure levels for adaptive optimization
 */
enum class MemoryPressure {
    LOW, MEDIUM, HIGH, CRITICAL
}