package com.hazardhawk.ai.litert

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.getSystemService

/**
 * Android-specific implementation of LiteRT device optimization.
 * Provides platform-specific device capability detection and performance optimization.
 */

/**
 * Get current battery level percentage.
 */
actual fun LiteRTDeviceOptimizer.getBatteryLevel(): Int {
    return try {
        val context = getApplicationContext()
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100f).toInt()
        } else {
            100 // Default to full battery if detection fails
        }
    } catch (e: Exception) {
        Log.w("LiteRTDeviceOptimizer", "Failed to get battery level", e)
        100
    }
}

/**
 * Check if device is in power save mode.
 */
actual fun LiteRTDeviceOptimizer.isPowerSaveModeEnabled(): Boolean {
    return try {
        val context = getApplicationContext()
        val powerManager = context.getSystemService<PowerManager>()
        
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                powerManager?.isPowerSaveMode == true
            }
            else -> false
        }
    } catch (e: Exception) {
        Log.w("LiteRTDeviceOptimizer", "Failed to check power save mode", e)
        false
    }
}

/**
 * Get number of CPU cores available.
 */
actual fun LiteRTDeviceOptimizer.getCpuCoreCount(): Int {
    return try {
        Runtime.getRuntime().availableProcessors()
    } catch (e: Exception) {
        Log.w("LiteRTDeviceOptimizer", "Failed to get CPU core count", e)
        4 // Default to 4 cores
    }
}

/**
 * Detect GPU vendor for optimization decisions.
 */
actual fun LiteRTDeviceOptimizer.detectGpuVendor(): GpuVendor {
    return try {
        val renderer = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER)?.lowercase()
        
        when {
            renderer?.contains("adreno") == true -> GpuVendor.QUALCOMM_ADRENO
            renderer?.contains("mali") == true -> GpuVendor.ARM_MALI
            renderer?.contains("tegra") == true -> GpuVendor.NVIDIA_TEGRA
            renderer?.contains("intel") == true -> GpuVendor.INTEL_HD
            else -> GpuVendor.UNKNOWN
        }
    } catch (e: Exception) {
        Log.w("LiteRTDeviceOptimizer", "Failed to detect GPU vendor", e)
        GpuVendor.UNKNOWN
    }
}

/**
 * Determine if device has high-performance GPU.
 */
actual fun LiteRTDeviceOptimizer.hasHighPerformanceGpu(): Boolean {
    return try {
        val context = getApplicationContext()
        val activityManager = context.getSystemService<ActivityManager>()
        val configInfo = activityManager?.deviceConfigurationInfo
        
        // Check for OpenGL ES 3.0+ support as indicator of high-performance GPU
        val hasOpenGLES3 = configInfo?.reqGlEsVersion?.let { version ->
            ((version and 0xffff0000.toInt()) shr 16) >= 3
        } ?: false
        
        // Additional checks based on device characteristics
        val totalMemoryGB = getTotalDeviceMemoryGB()
        val gpuVendor = detectGpuVendor()
        
        // High-performance criteria
        hasOpenGLES3 && totalMemoryGB >= 4.0f && when (gpuVendor) {
            GpuVendor.QUALCOMM_ADRENO,
            GpuVendor.ARM_MALI,
            GpuVendor.NVIDIA_TEGRA -> true
            else -> false
        }
        
    } catch (e: Exception) {
        Log.w("LiteRTDeviceOptimizer", "Failed to detect high-performance GPU", e)
        false
    }
}

/**
 * Get total device memory in GB.
 */
private fun getTotalDeviceMemoryGB(): Float {
    return try {
        val context = getApplicationContext()
        val activityManager = context.getSystemService<ActivityManager>()
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        
        val totalMemoryMB = (memInfo.totalMem / (1024 * 1024)).toFloat()
        totalMemoryMB / 1024f
    } catch (e: Exception) {
        Log.w("LiteRTDeviceOptimizer", "Failed to get total memory", e)
        4.0f // Default to 4GB
    }
}

/**
 * Context holder for dependency injection.
 */
private var applicationContext: Context? = null

/**
 * Set Android context for device capability detection.
 * This should be called by the dependency injection framework.
 */
fun LiteRTDeviceOptimizer.setAndroidContext(context: Context) {
    applicationContext = context
}

/**
 * Get application context (injected via dependency injection).
 */
private fun getApplicationContext(): Context {
    return applicationContext ?: throw IllegalStateException(
        "Android Context not injected. Ensure LiteRTDeviceOptimizer is created through dependency injection."
    )
}

/**
 * Enhanced device capabilities analysis for Android.
 */
class AndroidDeviceAnalyzer(private val context: Context) {
    
    /**
     * Perform comprehensive device capability analysis.
     */
    fun analyzeDeviceCapabilities(): AndroidDeviceCapabilities {
        return AndroidDeviceCapabilities(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.SDK_INT,
            chipset = getChipsetInfo(),
            totalMemoryGB = getTotalMemoryGB(),
            availableMemoryGB = getAvailableMemoryGB(),
            cpuCoreCount = Runtime.getRuntime().availableProcessors(),
            hasNPU = hasNeuralProcessingUnit(),
            gpuInfo = getGpuInfo(),
            thermalCapabilities = getThermalCapabilities(),
            batteryCapacity = getBatteryCapacity(),
            supportedAbiList = Build.SUPPORTED_ABIS.toList()
        )
    }
    
    /**
     * Get chipset information for device optimization.
     */
    private fun getChipsetInfo(): ChipsetInfo {
        val board = Build.BOARD.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        
        return when {
            board.contains("qcom") || hardware.contains("qcom") -> {
                ChipsetInfo(
                    vendor = ChipsetVendor.QUALCOMM,
                    series = detectQualcommSeries(),
                    hasNPU = true,
                    optimizedBackends = setOf(
                        LiteRTBackend.NPU_QTI_HTP,
                        LiteRTBackend.GPU_OPENCL,
                        LiteRTBackend.NPU_NNAPI
                    )
                )
            }
            board.contains("exynos") -> {
                ChipsetInfo(
                    vendor = ChipsetVendor.SAMSUNG_EXYNOS,
                    series = "Exynos",
                    hasNPU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                    optimizedBackends = setOf(
                        LiteRTBackend.NPU_NNAPI,
                        LiteRTBackend.GPU_OPENGL
                    )
                )
            }
            hardware.contains("mt") || board.contains("mt") -> {
                ChipsetInfo(
                    vendor = ChipsetVendor.MEDIATEK,
                    series = "MediaTek",
                    hasNPU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                    optimizedBackends = setOf(
                        LiteRTBackend.NPU_NNAPI,
                        LiteRTBackend.GPU_OPENGL
                    )
                )
            }
            else -> {
                ChipsetInfo(
                    vendor = ChipsetVendor.UNKNOWN,
                    series = "Unknown",
                    hasNPU = false,
                    optimizedBackends = setOf(LiteRTBackend.CPU)
                )
            }
        }
    }
    
    /**
     * Detect Qualcomm chipset series for specific optimizations.
     */
    private fun detectQualcommSeries(): String {
        return when {
            Build.MODEL.contains("888") -> "Snapdragon 8 Gen 1"
            Build.MODEL.contains("8 Gen") -> "Snapdragon 8 Gen Series"
            Build.MODEL.contains("855") -> "Snapdragon 855"
            Build.MODEL.contains("845") -> "Snapdragon 845"
            else -> "Snapdragon"
        }
    }
    
    /**
     * Check if device has dedicated Neural Processing Unit.
     */
    private fun hasNeuralProcessingUnit(): Boolean {
        return when {
            // Android 10+ with NNAPI 1.2+ generally indicates NPU support
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val chipset = getChipsetInfo()
                chipset.hasNPU && chipset.vendor in listOf(
                    ChipsetVendor.QUALCOMM,
                    ChipsetVendor.SAMSUNG_EXYNOS,
                    ChipsetVendor.MEDIATEK
                )
            }
            else -> false
        }
    }
    
    /**
     * Get detailed GPU information.
     */
    private fun getGpuInfo(): GpuInfo {
        return try {
            val renderer = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER) ?: ""
            val version = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_VERSION) ?: ""
            
            GpuInfo(
                renderer = renderer,
                version = version,
                vendor = when {
                    renderer.contains("adreno", ignoreCase = true) -> GpuVendor.QUALCOMM_ADRENO
                    renderer.contains("mali", ignoreCase = true) -> GpuVendor.ARM_MALI
                    renderer.contains("tegra", ignoreCase = true) -> GpuVendor.NVIDIA_TEGRA
                    else -> GpuVendor.UNKNOWN
                },
                supportsOpenCL = hasOpenCLSupport(),
                supportsVulkan = hasVulkanSupport()
            )
        } catch (e: Exception) {
            GpuInfo(
                renderer = "Unknown",
                version = "Unknown", 
                vendor = GpuVendor.UNKNOWN,
                supportsOpenCL = false,
                supportsVulkan = false
            )
        }
    }
    
    /**
     * Get thermal management capabilities.
     */
    private fun getThermalCapabilities(): ThermalCapabilities {
        return ThermalCapabilities(
            hasThermalAPI = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            maxOperatingTemp = 70f, // Conservative estimate
            throttlingTemp = 60f,
            shutdownTemp = 80f
        )
    }
    
    /**
     * Get battery capacity information.
     */
    private fun getBatteryCapacity(): Int {
        return try {
            val batteryManager = context.getSystemService<BatteryManager>()
            val capacity = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            capacity ?: 100
        } catch (e: Exception) {
            100 // Default
        }
    }
    
    private fun getTotalMemoryGB(): Float {
        return try {
            val activityManager = context.getSystemService<ActivityManager>()
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            (memInfo.totalMem / (1024 * 1024 * 1024)).toFloat()
        } catch (e: Exception) {
            4.0f
        }
    }
    
    private fun getAvailableMemoryGB(): Float {
        return try {
            val activityManager = context.getSystemService<ActivityManager>()
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            (memInfo.availMem / (1024 * 1024 * 1024)).toFloat()
        } catch (e: Exception) {
            2.0f
        }
    }
    
    private fun hasOpenCLSupport(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    private fun hasVulkanSupport(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
}

/**
 * Android-specific device capabilities.
 */
data class AndroidDeviceCapabilities(
    val manufacturer: String,
    val model: String,
    val androidVersion: Int,
    val chipset: ChipsetInfo,
    val totalMemoryGB: Float,
    val availableMemoryGB: Float,
    val cpuCoreCount: Int,
    val hasNPU: Boolean,
    val gpuInfo: GpuInfo,
    val thermalCapabilities: ThermalCapabilities,
    val batteryCapacity: Int,
    val supportedAbiList: List<String>
)

data class ChipsetInfo(
    val vendor: ChipsetVendor,
    val series: String,
    val hasNPU: Boolean,
    val optimizedBackends: Set<LiteRTBackend>
)

enum class ChipsetVendor {
    QUALCOMM,
    SAMSUNG_EXYNOS,
    MEDIATEK,
    UNKNOWN
}

data class GpuInfo(
    val renderer: String,
    val version: String,
    val vendor: GpuVendor,
    val supportsOpenCL: Boolean,
    val supportsVulkan: Boolean
)

data class ThermalCapabilities(
    val hasThermalAPI: Boolean,
    val maxOperatingTemp: Float,
    val throttlingTemp: Float,
    val shutdownTemp: Float
)