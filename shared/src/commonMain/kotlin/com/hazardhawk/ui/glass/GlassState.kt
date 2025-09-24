package com.hazardhawk.ui.glass

/**
 * Shared state management for glass morphism effects.
 * Handles device capability detection and environment adaptation.
 */
expect class GlassState {
    /**
     * Current glass configuration
     */
    val configuration: GlassConfiguration
    
    /**
     * Whether glass effects are currently enabled
     */
    val isEnabled: Boolean
    
    /**
     * Device capability level
     */
    val supportLevel: GlassSupportLevel
    
    /**
     * Current performance metrics
     */
    val performanceMetrics: GlassPerformanceMetrics?
    
    /**
     * Update glass configuration
     */
    fun updateConfiguration(config: GlassConfiguration)
    
    /**
     * Adapt configuration for current environment
     */
    fun adaptForEnvironment(environment: ConstructionEnvironment)
    
    /**
     * Enable/disable glass effects
     */
    fun setEnabled(enabled: Boolean)
    
    /**
     * Start performance monitoring
     */
    fun startPerformanceMonitoring()
    
    /**
     * Stop performance monitoring and return metrics
     */
    fun stopPerformanceMonitoring(): GlassPerformanceMetrics?
    
    companion object {
        /**
         * Get singleton glass state instance
         */
        fun getInstance(): GlassState
    }
}

/**
 * Device capability detector for glass effects
 */
expect class GlassCapabilityDetector {
    /**
     * Detect device capability for glass effects
     */
    fun detectCapability(): GlassSupportLevel
    
    /**
     * Get device RAM in MB
     */
    fun getDeviceRamMB(): Int
    
    /**
     * Get API level
     */
    fun getApiLevel(): Int
    
    /**
     * Check if hardware acceleration is available
     */
    fun hasHardwareAcceleration(): Boolean
    
    /**
     * Get current thermal state
     */
    fun getCurrentThermalState(): ThermalState
    
    /**
     * Check if device is in power saving mode
     */
    fun isPowerSavingEnabled(): Boolean
}

/**
 * Environment adapter for construction contexts
 */
class ConstructionEnvironmentAdapter {
    
    /**
     * Adapt glass configuration for construction environment
     */
    fun adaptConfiguration(
        baseConfig: GlassConfiguration,
        environment: ConstructionEnvironment
    ): GlassConfiguration {
        var adaptedConfig = baseConfig
        
        // Bright sunlight adaptations - increase opacity for visibility
        if (environment.ambientLightLux > 50000) {
            adaptedConfig = adaptedConfig.copy(
                opacity = adaptedConfig.opacity.coerceAtLeast(0.8f),
                animationsEnabled = false,
                supportLevel = GlassSupportLevel.REDUCED
            )
        }
        
        // Low light adaptations - increase blur for better effect visibility
        if (environment.ambientLightLux < 100) {
            adaptedConfig = adaptedConfig.copy(
                blurRadius = adaptedConfig.blurRadius.coerceAtLeast(20.0f),
                opacity = adaptedConfig.opacity.coerceAtMost(0.5f)
            )
        }
        
        // Heavy glove adaptations - disable glass for better touch accuracy
        if (environment.gloveThicknessMM > 5) {
            adaptedConfig = adaptedConfig.copy(
                supportLevel = GlassSupportLevel.DISABLED
            )
        }
        
        // Vibration present adaptations - disable animations
        if (environment.isVibrationPresent) {
            adaptedConfig = adaptedConfig.copy(
                animationsEnabled = false
            )
        }
        
        // Battery level adaptations
        if (environment.batteryLevel < 0.2f) {
            adaptedConfig = adaptedConfig.copy(
                supportLevel = GlassSupportLevel.DISABLED,
                animationsEnabled = false
            )
        } else if (environment.batteryLevel < 0.5f) {
            adaptedConfig = adaptedConfig.copy(
                supportLevel = GlassSupportLevel.REDUCED,
                animationsEnabled = false
            )
        }
        
        // Thermal throttling adaptations
        if (environment.thermalState >= ThermalState.SEVERE) {
            adaptedConfig = adaptedConfig.copy(
                supportLevel = GlassSupportLevel.DISABLED
            )
        } else if (environment.thermalState >= ThermalState.MODERATE) {
            adaptedConfig = adaptedConfig.copy(
                supportLevel = GlassSupportLevel.REDUCED,
                animationsEnabled = false
            )
        }
        
        return adaptedConfig
    }
    
    /**
     * Validate accessibility compliance for environment
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
            violations.add("Touch targets must be minimum ${GlassConfiguration.GLOVE_MIN_TOUCH_SIZE_DP}dp for glove compatibility")
        }
        
        // Emergency mode requirements
        if (!config.enabledInEmergencyMode && config.supportLevel != GlassSupportLevel.DISABLED) {
            violations.add("Glass effects should be disabled or minimal in emergency mode")
        }
        
        return violations
    }
}

/**
 * Fallback behavior validator
 */
class GlassFallbackValidator {
    
    /**
     * Validate fallback configuration against primary
     */
    fun validateFallback(
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