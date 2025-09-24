package com.hazardhawk.ui.glass.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.hazardhawk.ui.glass.GlassConfiguration

/**
 * Platform-agnostic glass overlay component for camera viewfinders and modals.
 * Provides adaptive blur based on lighting conditions and device performance.
 */
@Composable
expect fun GlassOverlay(
    modifier: Modifier = Modifier,
    config: GlassConfiguration = GlassConfiguration.construction,
    containerColor: Color = Color.Black.copy(alpha = 0.3f),
    contentColor: Color = Color.White,
    shape: Shape? = null,
    maskingEnabled: Boolean = true,
    adaptiveBlur: Boolean = true,
    emergencyMode: Boolean = false,
    content: @Composable BoxScope.() -> Unit
)

/**
 * Glass overlay configuration for different construction scenarios
 */
data class GlassOverlayConfig(
    val blurRadius: Float = 15.0f,
    val opacity: Float = 0.4f,
    val cornerRadius: Float = 16.0f,
    val borderWidth: Float = 1.0f,
    val borderColor: Color = Color.White.copy(alpha = 0.3f),
    val maskingEnabled: Boolean = true,
    val adaptiveBlurEnabled: Boolean = true,
    val transitionAnimationEnabled: Boolean = false, // Disabled for construction
    val emergencyHighContrast: Boolean = false,
    val performanceOptimized: Boolean = true
) {
    companion object {
        /**
         * Camera viewfinder overlay
         */
        val cameraViewfinder = GlassOverlayConfig(
            blurRadius = 12.0f,
            opacity = 0.3f,
            cornerRadius = 20.0f,
            borderWidth = 2.0f,
            borderColor = Color(0xFFFF6B35).copy(alpha = 0.6f), // Safety orange
            maskingEnabled = true,
            adaptiveBlurEnabled = true,
            transitionAnimationEnabled = false,
            emergencyHighContrast = false,
            performanceOptimized = true
        )
        
        /**
         * Modal dialog overlay
         */
        val modal = GlassOverlayConfig(
            blurRadius = 20.0f,
            opacity = 0.6f,
            cornerRadius = 24.0f,
            borderWidth = 1.5f,
            borderColor = Color.White.copy(alpha = 0.4f),
            maskingEnabled = true,
            adaptiveBlurEnabled = false, // Consistent for modals
            transitionAnimationEnabled = false,
            emergencyHighContrast = false,
            performanceOptimized = true
        )
        
        /**
         * Emergency overlay - high contrast
         */
        val emergency = GlassOverlayConfig(
            blurRadius = 8.0f,
            opacity = 0.9f,
            cornerRadius = 12.0f,
            borderWidth = 3.0f,
            borderColor = Color.Red,
            maskingEnabled = false, // Clear visibility in emergency
            adaptiveBlurEnabled = false,
            transitionAnimationEnabled = false,
            emergencyHighContrast = true,
            performanceOptimized = true
        )
        
        /**
         * Loading/processing overlay
         */
        val loading = GlassOverlayConfig(
            blurRadius = 25.0f,
            opacity = 0.7f,
            cornerRadius = 16.0f,
            borderWidth = 1.0f,
            borderColor = Color.White.copy(alpha = 0.2f),
            maskingEnabled = true,
            adaptiveBlurEnabled = false, // Consistent during loading
            transitionAnimationEnabled = false,
            emergencyHighContrast = false,
            performanceOptimized = true
        )
        
        /**
         * Outdoor bright light overlay
         */
        val outdoor = GlassOverlayConfig(
            blurRadius = 15.0f,
            opacity = 0.8f, // Higher opacity for better visibility
            cornerRadius = 16.0f,
            borderWidth = 2.0f,
            borderColor = Color.White.copy(alpha = 0.8f),
            maskingEnabled = true,
            adaptiveBlurEnabled = true,
            transitionAnimationEnabled = false,
            emergencyHighContrast = false,
            performanceOptimized = true
        )
    }
}