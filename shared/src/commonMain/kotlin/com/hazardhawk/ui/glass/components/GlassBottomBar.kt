package com.hazardhawk.ui.glass.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.hazardhawk.ui.glass.GlassConfiguration

/**
 * Platform-agnostic glass bottom bar component for construction apps.
 * Optimized for camera overlay use and construction environments.
 */
@Composable
expect fun GlassBottomBar(
    modifier: Modifier = Modifier,
    config: GlassConfiguration = GlassConfiguration.construction,
    containerColor: Color = Color.Black.copy(alpha = 0.6f),
    contentColor: Color = Color.White,
    cameraOverlayMode: Boolean = false,
    emergencyMode: Boolean = false,
    elevation: Dp? = null,
    content: @Composable RowScope.() -> Unit
)

/**
 * Glass bottom bar configuration for construction environments
 */
data class GlassBottomBarConfig(
    val blurRadius: Float = 20.0f,
    val opacity: Float = 0.8f,
    val height: Dp = Dp(80f), // Larger for construction gloves
    val cornerRadius: Float = 0f, // Full width bar typically has no corners
    val borderTopWidth: Float = 2.0f,
    val borderTopColor: Color = Color(0xFFFF6B35).copy(alpha = 0.8f), // Safety orange top accent
    val safetyAccentEnabled: Boolean = true,
    val adaptiveOpacity: Boolean = true, // Adapts based on camera overlay mode
    val emergencyHighContrast: Boolean = false
) {
    companion object {
        /**
         * Standard construction bottom bar
         */
        val construction = GlassBottomBarConfig(
            blurRadius = 20.0f,
            opacity = 0.8f,
            height = Dp(80f),
            cornerRadius = 0f,
            borderTopWidth = 2.0f,
            borderTopColor = Color(0xFFFF6B35).copy(alpha = 0.8f),
            safetyAccentEnabled = true,
            adaptiveOpacity = true,
            emergencyHighContrast = false
        )
        
        /**
         * Camera overlay optimized configuration
         */
        val cameraOverlay = GlassBottomBarConfig(
            blurRadius = 25.0f,
            opacity = 0.6f, // Lower opacity over camera
            height = Dp(72f),
            cornerRadius = 0f,
            borderTopWidth = 1.0f,
            borderTopColor = Color.White.copy(alpha = 0.3f),
            safetyAccentEnabled = false,
            adaptiveOpacity = true,
            emergencyHighContrast = false
        )
        
        /**
         * Emergency mode configuration
         */
        val emergency = GlassBottomBarConfig(
            blurRadius = 15.0f,
            opacity = 0.95f,
            height = Dp(88f),
            cornerRadius = 0f,
            borderTopWidth = 4.0f,
            borderTopColor = Color.Red,
            safetyAccentEnabled = false,
            adaptiveOpacity = false,
            emergencyHighContrast = true
        )
        
        /**
         * Professional/indoor configuration
         */
        val indoor = GlassBottomBarConfig(
            blurRadius = 18.0f,
            opacity = 0.75f,
            height = Dp(72f),
            cornerRadius = 0f,
            borderTopWidth = 1.5f,
            borderTopColor = Color.White.copy(alpha = 0.4f),
            safetyAccentEnabled = true,
            adaptiveOpacity = true,
            emergencyHighContrast = false
        )
    }
}