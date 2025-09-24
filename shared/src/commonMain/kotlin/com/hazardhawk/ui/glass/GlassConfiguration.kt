package com.hazardhawk.ui.glass

/**
 * Configuration for glass morphism effects across all platforms.
 * Provides device capability detection and performance optimization.
 */
data class GlassConfiguration(
    val blurRadius: Float = 15.0f,
    val opacity: Float = 0.6f,
    val animationsEnabled: Boolean = true,
    val enabledInEmergencyMode: Boolean = false,
    val supportLevel: GlassSupportLevel = GlassSupportLevel.FULL,
    // Additional properties for Android glass components
    val safetyBorderGlow: Boolean = true,
    val borderWidth: Float = 2.0f,
    val borderColorValue: Long = 0xFFFF6B35L, // Safety orange as Long
    val minTouchTargetSize: Int = 60, // Construction-friendly touch target size in dp
    val isHighContrastMode: Boolean = false,
    val isOutdoorMode: Boolean = false,
    val lightingCondition: LightingCondition = LightingCondition.NORMAL,
    val emergencyMode: EmergencyMode = EmergencyMode.NORMAL
) {
    
    companion object {
        // Construction environment constants
        const val MIN_BLUR_RADIUS = 10.0f
        const val MAX_BLUR_RADIUS = 25.0f
        const val MIN_OPACITY = 0.3f
        const val MAX_OPACITY = 0.9f
        const val GLOVE_MIN_TOUCH_SIZE_DP = 60
        const val CONSTRUCTION_MIN_CONTRAST_RATIO = 4.5 // WCAG AA
        
        /**
         * Default configuration for construction environments
         */
        val construction = GlassConfiguration(
            blurRadius = 15.0f,
            opacity = 0.8f,
            animationsEnabled = false,
            enabledInEmergencyMode = false,
            supportLevel = GlassSupportLevel.REDUCED
        )
        
        /**
         * Emergency mode configuration - high contrast, minimal effects
         */
        val emergency = GlassConfiguration(
            blurRadius = 12.0f,
            opacity = 0.9f,
            animationsEnabled = false,
            enabledInEmergencyMode = true,
            supportLevel = GlassSupportLevel.DISABLED
        )
        
        /**
         * Outdoor bright light configuration
         */
        val outdoor = GlassConfiguration(
            blurRadius = 15.0f,
            opacity = 0.85f,
            animationsEnabled = false,
            enabledInEmergencyMode = false,
            supportLevel = GlassSupportLevel.REDUCED
        )
    }
    
    /**
     * Validates configuration against construction environment requirements
     */
    fun validate(): List<String> {
        val violations = mutableListOf<String>()
        
        if (blurRadius < MIN_BLUR_RADIUS || blurRadius > MAX_BLUR_RADIUS) {
            violations.add("Blur radius $blurRadius outside valid range $MIN_BLUR_RADIUS-$MAX_BLUR_RADIUS")
        }
        
        if (opacity < MIN_OPACITY || opacity > MAX_OPACITY) {
            violations.add("Opacity $opacity outside valid range $MIN_OPACITY-$MAX_OPACITY")
        }
        
        // Emergency mode should disable expensive effects
        if (enabledInEmergencyMode && supportLevel == GlassSupportLevel.FULL) {
            if (animationsEnabled) {
                violations.add("Animations should be disabled in emergency mode")
            }
            if (blurRadius > 15.0f) {
                violations.add("Blur radius should be limited in emergency mode")
            }
        }
        
        return violations
    }
}

/**
 * Glass effect support levels based on device capability
 */
enum class GlassSupportLevel {
    FULL,      // Full glass effects with animations
    REDUCED,   // Static glass effects, no animations  
    DISABLED   // Fallback to solid backgrounds
}

/**
 * Lighting conditions that affect glass configuration
 */
enum class LightingCondition {
    NORMAL, LOW_LIGHT, OUTDOOR_BRIGHT, OUTDOOR_NORMAL, NIGHT
}

/**
 * Emergency mode states
 */
enum class EmergencyMode {
    NORMAL, EMERGENCY
}

/**
 * Construction environment contexts that affect glass configuration
 */
data class ConstructionEnvironment(
    val name: String,
    val ambientLightLux: Int = 1000,
    val isWearingGloves: Boolean = false,
    val gloveThicknessMM: Int = 0,
    val isVibrationPresent: Boolean = false,
    val batteryLevel: Float = 1.0f,
    val thermalState: ThermalState = ThermalState.NOMINAL
)

/**
 * Device thermal states affecting performance
 */
enum class ThermalState {
    NOMINAL, LIGHT, MODERATE, SEVERE, CRITICAL
}

/**
 * Performance metrics for glass effect monitoring
 */
data class GlassPerformanceMetrics(
    val frameRate: Double,
    val memoryUsageMB: Long,
    val batteryImpactPercent: Double,
    val loadTimeMs: Long,
    val gpuUtilizationPercent: Double,
    val thermalState: ThermalState = ThermalState.NOMINAL
) {
    companion object {
        const val MIN_FRAME_RATE_FPS = 45.0
        const val MAX_MEMORY_USAGE_MB = 50L
        const val MAX_BATTERY_IMPACT_PERCENT = 15.0
        const val MAX_LOAD_TIME_MS = 500L
        const val MAX_GPU_UTILIZATION_PERCENT = 80.0
    }
    
    /**
     * Validates performance against construction app requirements
     */
    fun validate(): List<String> {
        val violations = mutableListOf<String>()
        
        if (frameRate < MIN_FRAME_RATE_FPS) {
            violations.add("Frame rate $frameRate below minimum $MIN_FRAME_RATE_FPS FPS")
        }
        
        if (memoryUsageMB > MAX_MEMORY_USAGE_MB) {
            violations.add("Memory usage ${memoryUsageMB}MB exceeds maximum ${MAX_MEMORY_USAGE_MB}MB")
        }
        
        if (batteryImpactPercent > MAX_BATTERY_IMPACT_PERCENT) {
            violations.add("Battery impact $batteryImpactPercent% exceeds maximum $MAX_BATTERY_IMPACT_PERCENT%")
        }
        
        if (loadTimeMs > MAX_LOAD_TIME_MS) {
            violations.add("Load time ${loadTimeMs}ms exceeds maximum ${MAX_LOAD_TIME_MS}ms")
        }
        
        if (gpuUtilizationPercent > MAX_GPU_UTILIZATION_PERCENT) {
            violations.add("GPU utilization $gpuUtilizationPercent% exceeds maximum $MAX_GPU_UTILIZATION_PERCENT%")
        }
        
        return violations
    }
}