package com.hazardhawk.ui.glass.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.hazardhawk.ui.glass.GlassConfiguration

/**
 * Platform-agnostic glass card component for construction environments.
 * Provides backdrop blur effects with safety-focused styling.
 */
@Composable
expect fun GlassCard(
    modifier: Modifier = Modifier,
    config: GlassConfiguration = GlassConfiguration.construction,
    containerColor: Color = Color.Transparent,
    contentColor: Color = Color.White,
    shape: Shape? = null,
    elevation: Dp? = null,
    shadowColor: Color = Color.Black.copy(alpha = 0.3f),
    emergencyMode: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
)

/**
 * Glass card data class for configuration
 */
data class GlassCardConfig(
    val blurRadius: Float = 15.0f,
    val opacity: Float = 0.8f,
    val cornerRadius: Float = 16.0f,
    val borderWidth: Float = 1.0f,
    val borderColor: Color = Color.White.copy(alpha = 0.2f),
    val shadowEnabled: Boolean = true,
    val animationEnabled: Boolean = false, // Disabled for construction environments
    val emergencyHighContrast: Boolean = false
) {
    companion object {
        /**
         * Construction-optimized configuration
         */
        val construction = GlassCardConfig(
            blurRadius = 15.0f,
            opacity = 0.8f,
            cornerRadius = 12.0f,
            borderWidth = 2.0f,
            borderColor = Color(0xFFFF6B35).copy(alpha = 0.6f), // Safety orange
            shadowEnabled = true,
            animationEnabled = false,
            emergencyHighContrast = false
        )
        
        /**
         * Emergency mode configuration - high contrast
         */
        val emergency = GlassCardConfig(
            blurRadius = 10.0f,
            opacity = 0.95f,
            cornerRadius = 8.0f,
            borderWidth = 3.0f,
            borderColor = Color.Red,
            shadowEnabled = true,
            animationEnabled = false,
            emergencyHighContrast = true
        )
        
        /**
         * Outdoor bright light configuration
         */
        val outdoor = GlassCardConfig(
            blurRadius = 12.0f,
            opacity = 0.9f,
            cornerRadius = 12.0f,
            borderWidth = 2.0f,
            borderColor = Color.White.copy(alpha = 0.8f),
            shadowEnabled = true,
            animationEnabled = false,
            emergencyHighContrast = false
        )
    }
}