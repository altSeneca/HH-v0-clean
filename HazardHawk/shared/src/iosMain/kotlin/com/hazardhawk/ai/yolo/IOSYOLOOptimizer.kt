@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.hazardhawk.ai.yolo

import platform.Foundation.*
import platform.UIKit.*
import platform.CoreML.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS-specific YOLO optimizer for device capability detection and performance tuning.
 * Provides intelligent model selection and execution optimization based on iOS device hardware,
 * including Neural Engine detection and thermal management.
 */
class IOSYOLOOptimizer {
    
    companion object {
        private const val TAG = "IOSYOLOOptimizer"
    }
    
    // Device capability cache
    private var cachedCapability: DeviceCapability? = null
    
    /**
     * Assess device capability for optimal YOLO model selection
     */
    suspend fun assessDeviceCapability(): DeviceCapability {
        return withContext(Dispatchers.Default) {
            cachedCapability?.let { return@withContext it }
            
            val deviceType = detectDeviceType()
            val availableMemoryMB = getAvailableMemoryMB()
            val cpuCores = getCPUCoreCount()
            val hasGPU = detectGPUSupport()
            val performanceLevel = calculatePerformanceLevel(
                availableMemoryMB, cpuCores, hasGPU
            )
            
            NSLog("$TAG: Device capability assessment:")
            NSLog("$TAG:   Device Type: $deviceType")
            NSLog("$TAG:   Device Model: ${UIDevice.currentDevice.model}")
            NSLog("$TAG:   System Version: ${UIDevice.currentDevice.systemVersion}")
            NSLog("$TAG:   Available Memory: ${availableMemoryMB}MB")
            NSLog("$TAG:   CPU Cores: $cpuCores")
            NSLog("$TAG:   GPU Support: $hasGPU")
            NSLog("$TAG:   Neural Engine: ${hasNeuralEngine()}")
            NSLog("$TAG:   Performance Level: $performanceLevel")
            
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
     * Check if device has Neural Engine (A12 Bionic or newer)
     */
    fun hasNeuralEngine(): Boolean {
        val deviceModel = getDeviceModel()
        
        // Neural Engine is available on:
        // A12: iPhone XS, XS Max, XR (2018)
        // A13: iPhone 11, 11 Pro, 11 Pro Max (2019)
        // A14: iPhone 12 series, iPad Air 4 (2020)
        // A15: iPhone 13 series, iPad mini 6 (2021)
        // A16: iPhone 14 Pro series (2022)
        // A17: iPhone 15 Pro series (2023)
        // M1, M2, M3: iPad Pro, MacBook Air/Pro
        
        return when {
            deviceModel.contains("iPhone") -> {
                val modelNumbers = extractModelNumbers(deviceModel)
                when {
                    // iPhone 15 series (A16/A17)
                    modelNumbers.any { it >= 15 } -> true
                    // iPhone 14 series (A15/A16)
                    modelNumbers.any { it >= 14 } -> true
                    // iPhone 13 series (A15)
                    modelNumbers.any { it >= 13 } -> true
                    // iPhone 12 series (A14)
                    modelNumbers.any { it >= 12 } -> true
                    // iPhone 11 series (A13)
                    modelNumbers.any { it >= 11 } -> true
                    // iPhone XS, XS Max, XR (A12)
                    deviceModel.contains("iPhone11") || deviceModel.contains("iPhone12") -> true
                    else -> false
                }
            }
            deviceModel.contains("iPad") -> {
                when {
                    // iPad with M-series chips
                    deviceModel.contains("iPad13") || deviceModel.contains("iPad14") -> true
                    // iPad Air 4+ (A14+)
                    deviceModel.contains("iPad13") -> true
                    // iPad Pro with A12X/A12Z or newer
                    deviceModel.contains("iPad8") || deviceModel.contains("iPad11") -> true
                    else -> false
                }
            }
            else -> false
        }
    }
    
    /**
     * Get optimal compute units based on device capability
     */
    fun getOptimalComputeUnits(): MLComputeUnits {
        return when {
            hasNeuralEngine() -> MLComputeUnitsNeuralEngine
            detectGPUSupport() -> MLComputeUnitsGPU
            else -> MLComputeUnitsCPUOnly
        }
    }
    
    /**
     * Check if GPU acceleration is available and recommended
     */
    fun isGPUAccelerationAvailable(): Boolean {
        return try {
            // Check iOS version (Metal is available on iOS 8+)
            val majorVersion = NSProcessInfo.processInfo.operatingSystemVersion.majorVersion
            if (majorVersion < 8) {
                NSLog("$TAG: GPU acceleration not available: iOS version too old")
                return false
            }
            
            // Check for Metal support
            val hasGPU = detectGPUSupport()
            if (!hasGPU) {
                NSLog("$TAG: GPU acceleration not available: No Metal GPU detected")
                return false
            }
            
            // Check for sufficient memory (GPU acceleration requires more memory)
            val availableMemory = getAvailableMemoryMB()
            if (availableMemory < 1024) {
                NSLog("$TAG: GPU acceleration not recommended: Low memory (${availableMemory}MB)")
                return false
            }
            
            // Check thermal state
            if (isThermalThrottling()) {
                NSLog("$TAG: GPU acceleration not recommended: Device is thermally throttling")
                return false
            }
            
            NSLog("$TAG: GPU acceleration available and recommended")
            true
        } catch (e: Exception) {
            NSLog("$TAG: Error checking GPU acceleration availability: ${e.message}")
            false
        }
    }
    
    /**
     * Get memory pressure status using iOS-specific APIs
     */
    fun getMemoryPressure(): MemoryPressure {
        return try {
            // Use mach task info to get memory statistics
            val info = mach_task_basic_info_data_t()
            var count = MACH_TASK_BASIC_INFO_COUNT
            val result = task_info(
                mach_task_self(),
                MACH_TASK_BASIC_INFO.toInt(),
                info.ptr,
                count.ptr
            )
            
            if (result == KERN_SUCCESS) {
                val residentSize = info.resident_size
                val virtualSize = info.virtual_size
                
                // Get total device memory
                val totalMemory = NSProcessInfo.processInfo.physicalMemory
                val memoryUsageRatio = residentSize.toDouble() / totalMemory.toDouble()
                
                when {
                    memoryUsageRatio > 0.9 -> MemoryPressure.CRITICAL
                    memoryUsageRatio > 0.7 -> MemoryPressure.HIGH
                    memoryUsageRatio > 0.5 -> MemoryPressure.MEDIUM
                    else -> MemoryPressure.LOW
                }
            } else {
                NSLog("$TAG: Failed to get task info, defaulting to medium memory pressure")
                MemoryPressure.MEDIUM
            }
        } catch (e: Exception) {
            NSLog("$TAG: Error getting memory pressure: ${e.message}")
            MemoryPressure.MEDIUM
        }
    }
    
    /**
     * Check if device is experiencing thermal throttling
     */
    fun isThermalThrottling(): Boolean {
        return try {
            // Use NSProcessInfo thermal state (iOS 11+)
            val majorVersion = NSProcessInfo.processInfo.operatingSystemVersion.majorVersion
            if (majorVersion >= 11) {
                val thermalState = NSProcessInfo.processInfo.thermalState
                when (thermalState) {
                    NSProcessInfoThermalStateNominal -> false
                    NSProcessInfoThermalStateFair -> false
                    NSProcessInfoThermalStateSerious -> true
                    NSProcessInfoThermalStateCritical -> true
                    else -> false
                }
            } else {
                // Fallback: assume no throttling for older iOS versions
                false
            }
        } catch (e: Exception) {
            NSLog("$TAG: Error checking thermal throttling: ${e.message}")
            false
        }
    }
    
    /**
     * Get battery optimization recommendations
     */
    fun getBatteryOptimizedSettings(): BatteryOptimizedSettings {
        val batteryLevel = UIDevice.currentDevice.batteryLevel
        val batteryState = UIDevice.currentDevice.batteryState
        val isLowPowerMode = NSProcessInfo.processInfo.isLowPowerModeEnabled
        
        return when {
            isLowPowerMode || batteryLevel < 0.2f -> BatteryOptimizedSettings(
                useNeuralEngine = false,
                useGPU = false,
                maxInferenceThreads = 2,
                reduceModelSize = true
            )
            batteryLevel < 0.5f -> BatteryOptimizedSettings(
                useNeuralEngine = true,
                useGPU = false,
                maxInferenceThreads = 4,
                reduceModelSize = true
            )
            else -> BatteryOptimizedSettings(
                useNeuralEngine = hasNeuralEngine(),
                useGPU = detectGPUSupport(),
                maxInferenceThreads = getCPUCoreCount(),
                reduceModelSize = false
            )
        }
    }
    
    // Private helper methods
    
    private fun detectDeviceType(): DeviceType {
        val idiom = UIDevice.currentDevice.userInterfaceIdiom
        return when (idiom) {
            UIUserInterfaceIdiomPad -> DeviceType.TABLET
            UIUserInterfaceIdiomTV -> DeviceType.TV
            UIUserInterfaceIdiomPhone -> DeviceType.MOBILE_PHONE
            else -> DeviceType.MOBILE_PHONE
        }
    }
    
    private fun getAvailableMemoryMB(): Long {
        return try {
            // Get physical memory from NSProcessInfo
            val physicalMemory = NSProcessInfo.processInfo.physicalMemory
            physicalMemory.toLong() / 1024 / 1024 // Convert to MB
        } catch (e: Exception) {
            NSLog("$TAG: Error getting available memory: ${e.message}")
            2048L // Default to 2GB
        }
    }
    
    private fun getCPUCoreCount(): Int {
        return try {
            // Get active processor count
            val activeProcessors = NSProcessInfo.processInfo.activeProcessorCount
            activeProcessors.toInt()
        } catch (e: Exception) {
            NSLog("$TAG: Error getting CPU core count: ${e.message}")
            4 // Default to 4 cores
        }
    }
    
    private fun detectGPUSupport(): Boolean {
        return try {
            // Check if Metal device is available
            val defaultDevice = MTLCreateSystemDefaultDevice()
            defaultDevice != null
        } catch (e: Exception) {
            NSLog("$TAG: Error detecting GPU support: ${e.message}")
            // Assume modern iOS devices have GPU support
            NSProcessInfo.processInfo.operatingSystemVersion.majorVersion >= 8
        }
    }
    
    private fun getDeviceModel(): String {
        var size: size_t = 0u
        sysctlbyname("hw.machine", null, size.ptr, null, 0u)
        
        val machine = ByteArray(size.toInt())
        sysctlbyname("hw.machine", machine.refTo(0), size.ptr, null, 0u)
        
        return machine.decodeToString().trim('\u0000')
    }
    
    private fun extractModelNumbers(deviceModel: String): List<Int> {
        val regex = Regex("\\d+")
        return regex.findAll(deviceModel)
            .map { it.value.toIntOrNull() ?: 0 }
            .toList()
    }
    
    private fun calculatePerformanceLevel(
        availableMemoryMB: Long,
        cpuCores: Int,
        hasGPU: Boolean
    ): PerformanceLevel {
        var score = 0
        
        // Memory score (0-3 points)
        score += when {
            availableMemoryMB >= 8192 -> 3 // 8GB+ (iPad Pro, newer iPhones)
            availableMemoryMB >= 6144 -> 2 // 6GB+ (iPhone Pro models)
            availableMemoryMB >= 4096 -> 1 // 4GB+ (Standard iPhones)
            else -> 0 // <4GB (older devices)
        }
        
        // CPU score (0-3 points)
        score += when {
            cpuCores >= 8 -> 3 // High-end (A15+, M1+)
            cpuCores >= 6 -> 2 // Mid-high (A12-A14)
            cpuCores >= 4 -> 1 // Standard (A10-A11)
            else -> 0 // Low-end
        }
        
        // GPU score (0-2 points)
        if (hasGPU) score += 2
        
        // Neural Engine bonus (0-2 points)
        if (hasNeuralEngine()) score += 2
        
        // iOS version score (0-2 points)
        val majorVersion = NSProcessInfo.processInfo.operatingSystemVersion.majorVersion
        score += when {
            majorVersion >= 16 -> 2 // iOS 16+ (latest optimizations)
            majorVersion >= 14 -> 1 // iOS 14+ (Core ML 4+)
            else -> 0
        }
        
        return when {
            score >= 10 -> PerformanceLevel.HIGH
            score >= 6 -> PerformanceLevel.MEDIUM
            else -> PerformanceLevel.LOW
        }
    }
    
    /**
     * Get device-specific optimization recommendations
     */
    fun getDeviceOptimizations(): DeviceOptimizations {
        val deviceModel = getDeviceModel()
        val hasNeuralEngine = hasNeuralEngine()
        val thermalState = NSProcessInfo.processInfo.thermalState
        
        return DeviceOptimizations(
            preferredComputeUnits = when {
                hasNeuralEngine && thermalState <= NSProcessInfoThermalStateFair -> 
                    MLComputeUnitsNeuralEngine
                detectGPUSupport() && thermalState <= NSProcessInfoThermalStateSerious -> 
                    MLComputeUnitsGPU
                else -> MLComputeUnitsCPUOnly
            },
            maxConcurrentInferences = when {
                thermalState >= NSProcessInfoThermalStateSerious -> 1
                hasNeuralEngine -> 2
                else -> 1
            },
            shouldUseQuantization = when {
                getAvailableMemoryMB() < 3072 -> true // <3GB RAM
                thermalState >= NSProcessInfoThermalStateSerious -> true
                else -> false
            },
            recommendedModelSize = when {
                getAvailableMemoryMB() >= 6144 && hasNeuralEngine -> "large"
                getAvailableMemoryMB() >= 3072 -> "medium"
                else -> "small"
            }
        )
    }
}

/**
 * Memory pressure levels for iOS optimization
 */
enum class MemoryPressure {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Battery optimized settings for iOS devices
 */
data class BatteryOptimizedSettings(
    val useNeuralEngine: Boolean,
    val useGPU: Boolean,
    val maxInferenceThreads: Int,
    val reduceModelSize: Boolean
)

/**
 * Device-specific optimization recommendations
 */
data class DeviceOptimizations(
    val preferredComputeUnits: MLComputeUnits,
    val maxConcurrentInferences: Int,
    val shouldUseQuantization: Boolean,
    val recommendedModelSize: String
)