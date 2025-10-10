package com.hazardhawk.platform

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.hazardhawk.ai.litert.GpuVendor

/**
 * Android-specific implementation of device information.
 * Provides access to device capabilities using Android APIs.
 */
class AndroidDeviceInfo(private val context: Context) : IDeviceInfo {

    override fun getBatteryLevel(): Int {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        } catch (e: Exception) {
            -1 // Return -1 if battery info unavailable
        }
    }

    override fun isPowerSaveModeEnabled(): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isPowerSaveMode ?: false
        } catch (e: Exception) {
            false
        }
    }

    override fun getCpuCoreCount(): Int {
        return Runtime.getRuntime().availableProcessors()
    }

    override fun detectGpuVendor(): GpuVendor {
        return try {
            val renderer = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER)?.lowercase() ?: ""

            when {
                renderer.contains("adreno") || renderer.contains("qualcomm") -> GpuVendor.QUALCOMM_ADRENO
                renderer.contains("mali") || renderer.contains("arm") -> GpuVendor.ARM_MALI
                renderer.contains("tegra") || renderer.contains("nvidia") -> GpuVendor.NVIDIA_TEGRA
                renderer.contains("intel") -> GpuVendor.INTEL_HD
                else -> GpuVendor.UNKNOWN
            }
        } catch (e: Exception) {
            // Fallback detection based on device manufacturer
            when (Build.MANUFACTURER.lowercase()) {
                "qualcomm", "samsung", "google", "oneplus", "xiaomi" -> GpuVendor.QUALCOMM_ADRENO
                "mediatek", "huawei" -> GpuVendor.ARM_MALI
                "nvidia" -> GpuVendor.NVIDIA_TEGRA
                else -> GpuVendor.UNKNOWN
            }
        }
    }

    override fun hasHighPerformanceGpu(): Boolean {
        val gpuVendor = detectGpuVendor()
        val cores = getCpuCoreCount()

        // High-performance GPU detection heuristics
        return when (gpuVendor) {
            GpuVendor.QUALCOMM_ADRENO -> {
                // Adreno 6xx+ series are high-performance
                // Also check core count as proxy for flagship devices
                cores >= 8
            }
            GpuVendor.ARM_MALI -> {
                // Mali G7x+ series are high-performance
                cores >= 8
            }
            GpuVendor.NVIDIA_TEGRA -> true // All Tegra GPUs are high-performance
            else -> false
        }
    }
}

/**
 * Factory function to create Android device info implementation.
 *
 * Note: This requires a Context. In production, you should inject this
 * from your Application class or via dependency injection.
 */
actual fun createPlatformDeviceInfo(): IDeviceInfo {
    // This is a temporary implementation that will need context injection
    // In production, use: AndroidDeviceInfo(applicationContext)
    throw IllegalStateException(
        "AndroidDeviceInfo requires Context. " +
        "Please pass platformDeviceInfo explicitly in constructor: " +
        "LiteRTDeviceOptimizer(deviceTierDetector, modelEngine, AndroidDeviceInfo(context))"
    )
}
