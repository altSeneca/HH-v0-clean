package com.hazardhawk.platform

import com.hazardhawk.ai.litert.GpuVendor
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

/**
 * iOS-specific implementation of device information.
 * Provides access to device capabilities using iOS APIs.
 */
class IOSDeviceInfo : IDeviceInfo {

    override fun getBatteryLevel(): Int {
        UIDevice.currentDevice.batteryMonitoringEnabled = true
        val level = UIDevice.currentDevice.batteryLevel
        return if (level >= 0) {
            (level * 100).toInt()
        } else {
            -1 // Return -1 if battery info unavailable
        }
    }

    override fun isPowerSaveModeEnabled(): Boolean {
        return NSProcessInfo.processInfo.lowPowerModeEnabled
    }

    override fun getCpuCoreCount(): Int {
        return NSProcessInfo.processInfo.activeProcessorCount.toInt()
    }

    override fun detectGpuVendor(): GpuVendor {
        // All iOS devices use Apple-designed GPUs
        // Older devices used PowerVR (IMG), newer use Apple GPU
        val device = UIDevice.currentDevice.model

        // For iOS, GPU is always integrated and high-quality
        // We can return a default that makes sense for iOS
        return when {
            device.contains("iPad", ignoreCase = true) -> GpuVendor.UNKNOWN // Apple GPU (high-end)
            device.contains("iPhone", ignoreCase = true) -> GpuVendor.UNKNOWN // Apple GPU (varies)
            else -> GpuVendor.UNKNOWN
        }
    }

    override fun hasHighPerformanceGpu(): Boolean {
        // iOS device GPU performance detection based on model/cores
        val cores = getCpuCoreCount()
        val device = UIDevice.currentDevice.model

        return when {
            // iPads generally have high-performance GPUs
            device.contains("iPad", ignoreCase = true) -> true
            // iPhones with 6+ cores are typically Pro models with better GPUs
            device.contains("iPhone", ignoreCase = true) && cores >= 6 -> true
            // Default to false for older/budget devices
            else -> false
        }
    }
}

/**
 * Factory function to create iOS device info implementation.
 */
actual fun createPlatformDeviceInfo(): IDeviceInfo = IOSDeviceInfo()
