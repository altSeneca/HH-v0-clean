package com.hazardhawk.ui.glass

import kotlin.test.*

/**
 * Shared test logic for glass morphism effects across all platforms.
 * Contains device capability detection, performance calculations, and fallback logic.
 */
class GlassMorphismTestLogic {
    
    // Test Constants
    companion object {
        const val MIN_FRAME_RATE_FPS = 45.0
        const val MAX_MEMORY_USAGE_MB = 50L
        const val MAX_BATTERY_IMPACT_PERCENT = 15.0
        const val MAX_LOAD_TIME_MS = 500L
        const val MIN_BLUR_RADIUS = 10.0f
        const val MAX_BLUR_RADIUS = 25.0f
        const val MIN_OPACITY = 0.3f
        const val MAX_OPACITY = 0.9f
        
        // Construction Environment Constants
        const val CONSTRUCTION_MIN_CONTRAST_RATIO = 4.5 // WCAG AA
        const val GLOVE_MIN_TOUCH_SIZE_DP = 60
        const val OUTDOOR_BRIGHTNESS_LUX = 100000
        const val EMERGENCY_MODE_MAX_LATENCY_MS = 100
    }
    
    /**
     * Device capability test scenarios
     */
    data class DeviceCapability(
        val name: String,
        val ramMB: Int,
        val apiLevel: Int,
        val hasHardwareAcceleration: Boolean,
        val expectedGlassSupport: GlassSupportLevel
    )
    
    enum class GlassSupportLevel {
        FULL,      // Full glass effects with animations
        REDUCED,   // Static glass effects, no animations
        DISABLED   // Fallback to solid backgrounds
    }
    
    /**
     * Performance metrics for glass effects
     */
    data class GlassPerformanceMetrics(
        val frameRate: Double,
        val memoryUsageMB: Long,
        val batteryImpactPercent: Double,
        val loadTimeMs: Long,
        val gpuUtilizationPercent: Double,
        val thermalState: ThermalState = ThermalState.NOMINAL
    )
    
    enum class ThermalState {
        NOMINAL, LIGHT, MODERATE, SEVERE, CRITICAL
    }
    
    /**
     * Glass effect configuration for different scenarios
     */
    data class GlassConfiguration(
        val blurRadius: Float,
        val opacity: Float,
        val animationsEnabled: Boolean,
        val enabledInEmergencyMode: Boolean,
        val supportLevel: GlassSupportLevel
    )
    
    /**
     * Construction environment test scenarios
     */
    data class ConstructionEnvironment(
        val name: String,
        val ambientLightLux: Int,
        val isWearingGloves: Boolean,
        val gloveThicknessMM: Int,
        val isVibrationPresent: Boolean,
        val expectedConfiguration: GlassConfiguration
    )
    
    // Test Device Configurations
    fun getTestDeviceCapabilities(): List<DeviceCapability> = listOf(
        DeviceCapability(
            name = "High-end device",
            ramMB = 8192,
            apiLevel = 31,
            hasHardwareAcceleration = true,
            expectedGlassSupport = GlassSupportLevel.FULL
        ),
        DeviceCapability(
            name = "Mid-range device",
            ramMB = 4096,
            apiLevel = 28,
            hasHardwareAcceleration = true,
            expectedGlassSupport = GlassSupportLevel.REDUCED
        ),
        DeviceCapability(
            name = "Low-end device",
            ramMB = 2048,
            apiLevel = 24,
            hasHardwareAcceleration = false,
            expectedGlassSupport = GlassSupportLevel.DISABLED
        ),
        DeviceCapability(
            name = "Construction tablet",
            ramMB = 3072,
            apiLevel = 29,
            hasHardwareAcceleration = true,
            expectedGlassSupport = GlassSupportLevel.REDUCED
        )
    )
    
    // Construction Environment Test Scenarios
    fun getConstructionEnvironments(): List<ConstructionEnvironment> = listOf(
        ConstructionEnvironment(
            name = "Bright outdoor sunlight",
            ambientLightLux = 100000,
            isWearingGloves = true,
            gloveThicknessMM = 3,
            isVibrationPresent = false,
            expectedConfiguration = GlassConfiguration(
                blurRadius = 15.0f,
                opacity = 0.8f,
                animationsEnabled = false,
                enabledInEmergencyMode = false,
                supportLevel = GlassSupportLevel.REDUCED
            )
        ),
        ConstructionEnvironment(
            name = "Indoor workshop",
            ambientLightLux = 1000,
            isWearingGloves = false,
            gloveThicknessMM = 0,
            isVibrationPresent = true,
            isVibrationPresent = true,
            expectedConfiguration = GlassConfiguration(
                blurRadius = 20.0f,
                opacity = 0.6f,
                animationsEnabled = true,
                enabledInEmergencyMode = true,
                supportLevel = GlassSupportLevel.FULL
            )
        ),
        ConstructionEnvironment(
            name = "Heavy machinery area",
            ambientLightLux = 500,
            isWearingGloves = true,
            gloveThicknessMM = 8,
            isVibrationPresent = true,
            expectedConfiguration = GlassConfiguration(
                blurRadius = 12.0f,
                opacity = 0.9f,
                animationsEnabled = false,
                enabledInEmergencyMode = false,
                supportLevel = GlassSupportLevel.DISABLED
            )
        ),
        ConstructionEnvironment(
            name = "Dawn/dusk low light",
            ambientLightLux = 50,
            isWearingGloves = true,
            gloveThicknessMM = 5,
            isVibrationPresent = false,
            expectedConfiguration = GlassConfiguration(
                blurRadius = 25.0f,
                opacity = 0.4f,
                animationsEnabled = true,
                enabledInEmergencyMode = true,
                supportLevel = GlassSupportLevel.FULL
            )
        )
    )
    
    /**
     * Device capability detection logic
     */
    fun detectDeviceCapability(
        ramMB: Int,
        apiLevel: Int,
        hasHardwareAcceleration: Boolean,
        thermalState: ThermalState = ThermalState.NOMINAL
    ): GlassSupportLevel {
        // Emergency thermal throttling
        if (thermalState >= ThermalState.SEVERE) {
            return GlassSupportLevel.DISABLED
        }
        
        // API level requirements
        if (apiLevel < 24) {
            return GlassSupportLevel.DISABLED
        }
        
        // Hardware acceleration requirement for blur effects
        if (!hasHardwareAcceleration) {
            return GlassSupportLevel.DISABLED
        }
        
        // RAM-based capability detection
        return when {
            ramMB >= 6144 && apiLevel >= 30 -> GlassSupportLevel.FULL
            ramMB >= 3072 && apiLevel >= 26 -> GlassSupportLevel.REDUCED
            ramMB >= 2048 -> GlassSupportLevel.DISABLED
            else -> GlassSupportLevel.DISABLED
        }
    }
    
    /**
     * Performance validation logic
     */
    fun validatePerformance(metrics: GlassPerformanceMetrics): List<String> {
        val violations = mutableListOf<String>()
        
        if (metrics.frameRate < MIN_FRAME_RATE_FPS) {
            violations.add("Frame rate ${metrics.frameRate} below minimum $MIN_FRAME_RATE_FPS FPS")
        }
        
        if (metrics.memoryUsageMB > MAX_MEMORY_USAGE_MB) {
            violations.add("Memory usage ${metrics.memoryUsageMB}MB exceeds maximum ${MAX_MEMORY_USAGE_MB}MB")
        }
        
        if (metrics.batteryImpactPercent > MAX_BATTERY_IMPACT_PERCENT) {
            violations.add("Battery impact ${metrics.batteryImpactPercent}% exceeds maximum $MAX_BATTERY_IMPACT_PERCENT%")
        }
        
        if (metrics.loadTimeMs > MAX_LOAD_TIME_MS) {
            violations.add("Load time ${metrics.loadTimeMs}ms exceeds maximum ${MAX_LOAD_TIME_MS}ms")
        }
        
        if (metrics.gpuUtilizationPercent > 80.0) {
            violations.add("GPU utilization ${metrics.gpuUtilizationPercent}% too high")
        }
        
        return violations
    }
    
    /**
     * Glass configuration validation
     */
    fun validateGlassConfiguration(config: GlassConfiguration): List<String> {
        val violations = mutableListOf<String>()
        
        if (config.blurRadius < MIN_BLUR_RADIUS || config.blurRadius > MAX_BLUR_RADIUS) {
            violations.add("Blur radius ${config.blurRadius} outside valid range $MIN_BLUR_RADIUS-$MAX_BLUR_RADIUS")
        }
        
        if (config.opacity < MIN_OPACITY || config.opacity > MAX_OPACITY) {
            violations.add("Opacity ${config.opacity} outside valid range $MIN_OPACITY-$MAX_OPACITY")
        }
        
        // Emergency mode should disable expensive effects
        if (config.enabledInEmergencyMode && config.supportLevel == GlassSupportLevel.FULL) {
            if (config.animationsEnabled) {
                violations.add("Animations should be disabled in emergency mode")
            }
            if (config.blurRadius > 15.0f) {
                violations.add("Blur radius should be limited in emergency mode")
            }
        }
        
        return violations
    }
    
    /**
     * Construction environment adaptation logic
     */
    fun adaptConfigurationForEnvironment(
        baseConfig: GlassConfiguration,
        environment: ConstructionEnvironment
    ): GlassConfiguration {
        var adaptedConfig = baseConfig
        
        // Bright sunlight adaptations
        if (environment.ambientLightLux > 50000) {
            adaptedConfig = adaptedConfig.copy(
                opacity = adaptedConfig.opacity.coerceAtLeast(0.8f),
                animationsEnabled = false
            )
        }
        
        // Low light adaptations
        if (environment.ambientLightLux < 100) {
            adaptedConfig = adaptedConfig.copy(
                blurRadius = adaptedConfig.blurRadius.coerceAtLeast(20.0f),
                opacity = adaptedConfig.opacity.coerceAtMost(0.5f)
            )
        }
        
        // Heavy glove adaptations
        if (environment.gloveThicknessMM > 5) {
            adaptedConfig = adaptedConfig.copy(
                supportLevel = GlassSupportLevel.DISABLED
            )
        }
        
        // Vibration present adaptations
        if (environment.isVibrationPresent) {
            adaptedConfig = adaptedConfig.copy(
                animationsEnabled = false
            )
        }
        
        return adaptedConfig
    }
    
    /**
     * Accessibility compliance validation
     */
    fun validateAccessibilityCompliance(
        config: GlassConfiguration,
        environment: ConstructionEnvironment
    ): List<String> {
        val violations = mutableListOf<String>()
        
        // WCAG contrast requirements
        val effectiveOpacity = config.opacity
        if (effectiveOpacity < 0.7f && environment.ambientLightLux > 10000) {
            violations.add("Glass opacity too low for outdoor visibility")
        }
        
        // Touch target size for gloves
        if (environment.isWearingGloves) {
            // This would be validated in the UI layer
            violations.add("Touch targets must be minimum ${GLOVE_MIN_TOUCH_SIZE_DP}dp for glove compatibility")
        }
        
        // Emergency mode requirements
        if (!config.enabledInEmergencyMode && config.supportLevel != GlassSupportLevel.DISABLED) {
            violations.add("Glass effects should be disabled or minimal in emergency mode")
        }
        
        return violations
    }
    
    /**
     * Fallback system validation
     */
    fun validateFallbackBehavior(
        primaryConfig: GlassConfiguration,
        fallbackConfig: GlassConfiguration
    ): List<String> {
        val violations = mutableListOf<String>()
        
        // Fallback should be simpler
        if (fallbackConfig.blurRadius > primaryConfig.blurRadius) {
            violations.add("Fallback blur radius should not exceed primary")
        }
        
        if (fallbackConfig.animationsEnabled && !primaryConfig.animationsEnabled) {
            violations.add("Fallback should not enable animations if primary disabled them")
        }
        
        // Fallback should maintain visibility
        if (fallbackConfig.opacity < primaryConfig.opacity) {
            violations.add("Fallback should maintain or improve visibility")
        }
        
        return violations
    }
}

/**
 * Mock performance monitoring for testing
 */
class MockGlassPerformanceMonitor {
    
    private val metrics = mutableListOf<GlassMorphismTestLogic.GlassPerformanceMetrics>()
    
    fun startMonitoring() {
        metrics.clear()
    }
    
    fun recordMetrics(
        frameRate: Double,
        memoryUsageMB: Long,
        batteryImpactPercent: Double,
        loadTimeMs: Long,
        gpuUtilizationPercent: Double,
        thermalState: GlassMorphismTestLogic.ThermalState = GlassMorphismTestLogic.ThermalState.NOMINAL
    ) {
        metrics.add(
            GlassMorphismTestLogic.GlassPerformanceMetrics(
                frameRate = frameRate,
                memoryUsageMB = memoryUsageMB,
                batteryImpactPercent = batteryImpactPercent,
                loadTimeMs = loadTimeMs,
                gpuUtilizationPercent = gpuUtilizationPercent,
                thermalState = thermalState
            )
        )
    }
    
    fun getAverageMetrics(): GlassMorphismTestLogic.GlassPerformanceMetrics? {
        if (metrics.isEmpty()) return null
        
        return GlassMorphismTestLogic.GlassPerformanceMetrics(
            frameRate = metrics.map { it.frameRate }.average(),
            memoryUsageMB = (metrics.map { it.memoryUsageMB }.average()).toLong(),
            batteryImpactPercent = metrics.map { it.batteryImpactPercent }.average(),
            loadTimeMs = (metrics.map { it.loadTimeMs }.average()).toLong(),
            gpuUtilizationPercent = metrics.map { it.gpuUtilizationPercent }.average(),
            thermalState = metrics.map { it.thermalState }.maxOrNull() ?: GlassMorphismTestLogic.ThermalState.NOMINAL
        )
    }
    
    fun getAllMetrics(): List<GlassMorphismTestLogic.GlassPerformanceMetrics> = metrics.toList()
    
    fun stopMonitoring(): GlassMorphismTestLogic.GlassPerformanceMetrics? {
        return getAverageMetrics()
    }
}
