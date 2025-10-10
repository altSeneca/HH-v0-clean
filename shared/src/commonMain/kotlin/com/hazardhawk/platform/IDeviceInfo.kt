package com.hazardhawk.platform

import com.hazardhawk.ai.litert.GpuVendor

/**
 * Platform-specific device information interface.
 * Provides device capabilities detection across all platforms.
 */
interface IDeviceInfo {
    /**
     * Get current battery level percentage.
     * @return Battery level from 0-100, or -1 if unavailable
     */
    fun getBatteryLevel(): Int

    /**
     * Check if power save mode is currently enabled.
     * @return true if power save mode is active
     */
    fun isPowerSaveModeEnabled(): Boolean

    /**
     * Get number of available CPU cores.
     * @return Number of CPU cores available to the app
     */
    fun getCpuCoreCount(): Int

    /**
     * Detect the GPU vendor for the device.
     * @return GPU vendor enum, or UNKNOWN if detection fails
     */
    fun detectGpuVendor(): GpuVendor

    /**
     * Determine if device has a high-performance GPU.
     * @return true if GPU is high-performance tier
     */
    fun hasHighPerformanceGpu(): Boolean
}

/**
 * Factory function to create platform-specific device info implementation.
 * Implemented in androidMain, iosMain, etc.
 */
expect fun createPlatformDeviceInfo(): IDeviceInfo
