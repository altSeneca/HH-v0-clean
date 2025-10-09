package com.hazardhawk.ai.litert
import kotlinx.datetime.Clock

import com.hazardhawk.performance.DeviceTierDetector
import com.hazardhawk.performance.DeviceTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Intelligent device optimization for LiteRT backend selection.
 * Analyzes device capabilities and selects optimal processing backend
 * for maximum performance while respecting thermal and memory constraints.
 * 
 * Performance Targets:
 * - NPU: 5,836 tokens/sec (24x CPU improvement)
 * - GPU: 1,876 tokens/sec (7.7x CPU improvement)
 * - CPU: 243 tokens/sec (baseline)
 */
class LiteRTDeviceOptimizer(
    private val deviceTierDetector: DeviceTierDetector,
    private val modelEngine: LiteRTModelEngine
) {
    
    private var cachedOptimalBackend: LiteRTBackend? = null
    private var lastOptimizationTime = 0L
    private val optimizationCacheMs = 30000L // Cache for 30 seconds
    
    /**
     * Select the optimal LiteRT backend for current device conditions.
     * Considers device tier, thermal state, memory availability, and power constraints.
     * 
     * @param forceRecalculate Skip cache and recalculate optimal backend
     * @return Recommended LiteRT backend for best performance
     */
    suspend fun selectOptimalBackend(forceRecalculate: Boolean = false): LiteRTBackend = withContext(Dispatchers.Default) {
        
        val currentTime = Clock.System.now().toEpochMilliseconds()
        
        // Use cached result if recent and not forced
        if (!forceRecalculate && 
            cachedOptimalBackend != null && 
            (currentTime - lastOptimizationTime) < optimizationCacheMs) {
            return@withContext cachedOptimalBackend!!
        }
        
        val deviceCapabilities = analyzeDeviceCapabilities()
        val selectedBackend = determineOptimalBackend(deviceCapabilities)
        
        // Cache the result
        cachedOptimalBackend = selectedBackend
        lastOptimizationTime = currentTime
        
        selectedBackend
    }
    
    /**
     * Analyze comprehensive device capabilities for backend selection.
     */
    private suspend fun analyzeDeviceCapabilities(): DeviceCapabilities {
        val deviceTier = deviceTierDetector.detectDeviceTier()
        val thermalState = deviceTierDetector.getCurrentThermalState()
        val memoryInfo = deviceTierDetector.getMemoryInfo()
        val supportedBackends = modelEngine.supportedBackends
        
        return DeviceCapabilities(
            deviceTier = deviceTier,
            thermalState = thermalState,
            totalMemoryGB = memoryInfo.totalMemoryMB / 1024f,
            availableMemoryGB = memoryInfo.availableMemoryMB / 1024f,
            supportedBackends = supportedBackends,
            batteryLevel = getBatteryLevel(),
            powerSaveMode = isPowerSaveModeEnabled(),
            cpuCoreCount = getCpuCoreCount(),
            gpuVendor = detectGpuVendor(),
            hasNPU = supportedBackends.any { it.isNPU() },
            hasHighPerformanceGPU = hasHighPerformanceGpu()
        )
    }
    
    /**
     * Determine optimal backend based on device capabilities analysis.
     */
    private fun determineOptimalBackend(capabilities: DeviceCapabilities): LiteRTBackend {
        
        // Priority 1: Thermal throttling protection
        if (capabilities.thermalState.isCritical()) {
            return selectThermalSafeBackend(capabilities)
        }
        
        // Priority 2: Power save mode consideration
        if (capabilities.powerSaveMode || capabilities.batteryLevel < 15) {
            return selectPowerEfficientBackend(capabilities)
        }
        
        // Priority 3: Memory constraints
        if (capabilities.availableMemoryGB < 1.5f) {
            return selectMemoryEfficientBackend(capabilities)
        }
        
        // Priority 4: Performance optimization for capable devices
        return selectHighPerformanceBackend(capabilities)
    }
    
    /**
     * Select backend that won't trigger thermal throttling.
     */
    private fun selectThermalSafeBackend(capabilities: DeviceCapabilities): LiteRTBackend {
        return when {
            // NPU is most power efficient for thermal conditions
            capabilities.hasNPU && LiteRTBackend.NPU_NNAPI in capabilities.supportedBackends -> {
                LiteRTBackend.NPU_NNAPI
            }
            
            // CPU as thermal-safe fallback
            else -> LiteRTBackend.CPU
        }
    }
    
    /**
     * Select most power-efficient backend for battery preservation.
     */
    private fun selectPowerEfficientBackend(capabilities: DeviceCapabilities): LiteRTBackend {
        return when {
            // NPU offers best performance per watt
            capabilities.hasNPU && LiteRTBackend.NPU_NNAPI in capabilities.supportedBackends -> {
                LiteRTBackend.NPU_NNAPI
            }
            
            // Qualcomm HTP if available
            LiteRTBackend.NPU_QTI_HTP in capabilities.supportedBackends -> {
                LiteRTBackend.NPU_QTI_HTP
            }
            
            // CPU for power savings
            else -> LiteRTBackend.CPU
        }
    }
    
    /**
     * Select backend that uses minimal memory.
     */
    private fun selectMemoryEfficientBackend(capabilities: DeviceCapabilities): LiteRTBackend {
        return when {
            // NPU typically uses less memory
            capabilities.hasNPU -> {
                capabilities.supportedBackends.firstOrNull { it.isNPU() } ?: LiteRTBackend.CPU
            }
            
            // CPU for memory-constrained devices
            else -> LiteRTBackend.CPU
        }
    }
    
    /**
     * Select highest performance backend for capable devices.
     */
    private fun selectHighPerformanceBackend(capabilities: DeviceCapabilities): LiteRTBackend {
        return when (capabilities.deviceTier) {
            DeviceTier.HIGH_END -> {
                when {
                    // NPU provides best performance (24x CPU)
                    capabilities.hasNPU && LiteRTBackend.NPU_NNAPI in capabilities.supportedBackends -> {
                        LiteRTBackend.NPU_NNAPI
                    }
                    
                    // Qualcomm NPU alternative
                    LiteRTBackend.NPU_QTI_HTP in capabilities.supportedBackends -> {
                        LiteRTBackend.NPU_QTI_HTP
                    }
                    
                    // High-performance GPU (7.7x CPU)
                    capabilities.hasHighPerformanceGPU -> {
                        selectBestGpuBackend(capabilities)
                    }
                    
                    // CPU fallback
                    else -> LiteRTBackend.CPU
                }
            }
            
            DeviceTier.MID_RANGE -> {
                when {
                    // NPU if available
                    capabilities.hasNPU -> {
                        capabilities.supportedBackends.firstOrNull { it.isNPU() } ?: LiteRTBackend.CPU
                    }
                    
                    // GPU acceleration for mid-range
                    LiteRTBackend.GPU_OPENGL in capabilities.supportedBackends -> {
                        LiteRTBackend.GPU_OPENGL
                    }
                    
                    // CPU for older mid-range
                    else -> LiteRTBackend.CPU
                }
            }
            
            DeviceTier.LOW_END -> {
                // Always use CPU for low-end devices to avoid crashes
                LiteRTBackend.CPU
            }
        }
    }
    
    /**
     * Select best available GPU backend.
     */
    private fun selectBestGpuBackend(capabilities: DeviceCapabilities): LiteRTBackend {
        return when {
            LiteRTBackend.GPU_OPENCL in capabilities.supportedBackends -> LiteRTBackend.GPU_OPENCL
            LiteRTBackend.GPU_OPENGL in capabilities.supportedBackends -> LiteRTBackend.GPU_OPENGL
            else -> LiteRTBackend.CPU
        }
    }
    
    /**
     * Get device performance recommendations for UI display.
     */
    fun getPerformanceRecommendations(): List<PerformanceRecommendation> {
        val recommendations = mutableListOf<PerformanceRecommendation>()
        
        val capabilities = runCatching { analyzeDeviceCapabilities() }.getOrNull()
            ?: return listOf(
                PerformanceRecommendation(
                    title = "Device Analysis Failed",
                    description = "Unable to analyze device capabilities",
                    priority = Priority.HIGH
                )
            )
        
        // NPU availability recommendation
        if (capabilities.hasNPU) {
            recommendations.add(
                PerformanceRecommendation(
                    title = "NPU Acceleration Available",
                    description = "Your device supports Neural Processing Unit acceleration for 24x faster AI analysis",
                    priority = Priority.HIGH
                )
            )
        }
        
        // Memory recommendation
        if (capabilities.availableMemoryGB < 2.0f) {
            recommendations.add(
                PerformanceRecommendation(
                    title = "Low Memory Detected",
                    description = "Close other apps to improve AI processing performance",
                    priority = Priority.MEDIUM
                )
            )
        }
        
        // Thermal recommendation
        if (capabilities.thermalState.isCritical()) {
            recommendations.add(
                PerformanceRecommendation(
                    title = "Device Overheating",
                    description = "Allow device to cool for optimal AI performance",
                    priority = Priority.HIGH
                )
            )
        }
        
        // Battery recommendation
        if (capabilities.batteryLevel < 20) {
            recommendations.add(
                PerformanceRecommendation(
                    title = "Low Battery",
                    description = "Connect charger for full performance AI analysis",
                    priority = Priority.MEDIUM
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Validate that selected backend works on current device.
     */
    suspend fun validateBackend(backend: LiteRTBackend): BackendValidationResult = withContext(Dispatchers.Default) {
        try {
            val testResult = modelEngine.initialize("test_model_validation", backend)
            
            if (testResult.isSuccess) {
                val metrics = modelEngine.getPerformanceMetrics()
                BackendValidationResult(
                    isSupported = true,
                    expectedPerformance = backend.expectedTokensPerSecond,
                    actualPerformance = metrics.tokensPerSecond,
                    memoryUsage = metrics.averageMemoryUsageMB,
                    errorMessage = null
                )
            } else {
                BackendValidationResult(
                    isSupported = false,
                    expectedPerformance = 0f,
                    actualPerformance = 0f,
                    memoryUsage = 0f,
                    errorMessage = testResult.exceptionOrNull()?.message
                )
            }
        } catch (e: Exception) {
            BackendValidationResult(
                isSupported = false,
                expectedPerformance = 0f,
                actualPerformance = 0f,
                memoryUsage = 0f,
                errorMessage = e.message
            )
        }
    }
    
    // Platform-specific implementations (expect/actual pattern)
    private expect fun getBatteryLevel(): Int
    private expect fun isPowerSaveModeEnabled(): Boolean
    private expect fun getCpuCoreCount(): Int
    private expect fun detectGpuVendor(): GpuVendor
    private expect fun hasHighPerformanceGpu(): Boolean
}

/**
 * Device capabilities analysis result.
 */
data class DeviceCapabilities(
    val deviceTier: DeviceTier,
    val thermalState: ThermalState,
    val totalMemoryGB: Float,
    val availableMemoryGB: Float,
    val supportedBackends: Set<LiteRTBackend>,
    val batteryLevel: Int,
    val powerSaveMode: Boolean,
    val cpuCoreCount: Int,
    val gpuVendor: GpuVendor,
    val hasNPU: Boolean,
    val hasHighPerformanceGPU: Boolean
)

enum class GpuVendor {
    QUALCOMM_ADRENO,
    ARM_MALI,
    NVIDIA_TEGRA,
    INTEL_HD,
    UNKNOWN
}

enum class ThermalState {
    NORMAL,
    LIGHT_THROTTLING,
    MODERATE_THROTTLING,
    SEVERE_THROTTLING,
    CRITICAL_THROTTLING,
    EMERGENCY_SHUTDOWN;
    
    fun isCritical(): Boolean = this in listOf(SEVERE_THROTTLING, CRITICAL_THROTTLING, EMERGENCY_SHUTDOWN)
}

enum class Priority {
    LOW,
    MEDIUM, 
    HIGH,
    CRITICAL
}

/**
 * Performance recommendation for user guidance.
 */
data class PerformanceRecommendation(
    val title: String,
    val description: String,
    val priority: Priority,
    val actionable: Boolean = true
)

/**
 * Backend validation test result.
 */
data class BackendValidationResult(
    val isSupported: Boolean,
    val expectedPerformance: Float,
    val actualPerformance: Float,
    val memoryUsage: Float,
    val errorMessage: String?
)

/**
 * Extension functions for backend classification.
 */
fun LiteRTBackend.isNPU(): Boolean = when (this) {
    LiteRTBackend.NPU_NNAPI, LiteRTBackend.NPU_QTI_HTP -> true
    else -> false
}

fun LiteRTBackend.isGPU(): Boolean = when (this) {
    LiteRTBackend.GPU_OPENCL, LiteRTBackend.GPU_OPENGL -> true
    else -> false
}

fun LiteRTBackend.isCPU(): Boolean = this == LiteRTBackend.CPU