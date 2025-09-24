package com.hazardhawk.ui.glass.components

import androidx.compose.ui.graphics.Color
import com.hazardhawk.ui.glass.*

/**
 * Android-specific Glass Configuration for Compose UI
 */
data class GlassConfiguration(
    val blurRadius: Float = 15.0f,
    val opacity: Float = 0.6f,
    val animationsEnabled: Boolean = true,
    val enabledInEmergencyMode: Boolean = false,
    val supportLevel: GlassSupportLevel = GlassSupportLevel.FULL,
    // Android-specific properties
    val safetyBorderGlow: Boolean = true,
    val borderWidth: Float = 2.0f,
    val borderColor: Color = Color(0xFFFF6B35), // Safety orange
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
