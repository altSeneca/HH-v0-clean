package com.hazardhawk.ui.glass.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.hazardhawk.ui.glass.GlassConfiguration

/**
 * Platform-agnostic glass button component optimized for construction environments.
 * Features large touch targets, haptic feedback, and safety-focused styling.
 */
@Composable
expect fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfiguration = GlassConfiguration.construction,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    contentColor: Color = Color.White,
    disabledContainerColor: Color = Color.Gray.copy(alpha = 0.3f),
    disabledContentColor: Color = Color.Gray,
    shape: Shape? = null,
    elevation: Dp? = null,
    hapticFeedback: Boolean = true,
    emergencyMode: Boolean = false,
    content: @Composable RowScope.() -> Unit
)

/**
 * Glass button configuration for construction environments
 */
data class GlassButtonConfig(
    val blurRadius: Float = 15.0f,
    val opacity: Float = 0.8f,
    val cornerRadius: Float = 16.0f,
    val borderWidth: Float = 2.0f,
    val borderColor: Color = Color.White.copy(alpha = 0.4f),
    val minTouchTargetSize: Dp = Dp(60f), // WCAG AA for gloved hands
    val rippleEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val emergencyHighContrast: Boolean = false,
    val safetyOrangeAccent: Boolean = true
) {
    companion object {
        /**
         * Construction-optimized button configuration
         */
        val construction = GlassButtonConfig(
            blurRadius = 15.0f,
            opacity = 0.8f,
            cornerRadius = 16.0f,
            borderWidth = 2.0f,
            borderColor = Color(0xFFFF6B35).copy(alpha = 0.6f), // Safety orange
            minTouchTargetSize = Dp(60f),
            rippleEnabled = true,
            hapticFeedbackEnabled = true,
            emergencyHighContrast = false,
            safetyOrangeAccent = true
        )
        
        /**
         * Primary action button with enhanced visibility
         */
        val primary = GlassButtonConfig(
            blurRadius = 18.0f,
            opacity = 0.85f,
            cornerRadius = 20.0f,
            borderWidth = 3.0f,
            borderColor = Color(0xFFFF6B35), // Solid safety orange
            minTouchTargetSize = Dp(64f),
            rippleEnabled = true,
            hapticFeedbackEnabled = true,
            emergencyHighContrast = false,
            safetyOrangeAccent = true
        )
        
        /**
         * Emergency mode button - high contrast
         */
        val emergency = GlassButtonConfig(
            blurRadius = 10.0f,
            opacity = 0.95f,
            cornerRadius = 12.0f,
            borderWidth = 4.0f,
            borderColor = Color.Red,
            minTouchTargetSize = Dp(72f),
            rippleEnabled = false, // Reduced effects in emergency
            hapticFeedbackEnabled = true,
            emergencyHighContrast = true,
            safetyOrangeAccent = false
        )
        
        /**
         * Secondary button for less critical actions
         */
        val secondary = GlassButtonConfig(
            blurRadius = 12.0f,
            opacity = 0.7f,
            cornerRadius = 14.0f,
            borderWidth = 1.5f,
            borderColor = Color.White.copy(alpha = 0.5f),
            minTouchTargetSize = Dp(56f),
            rippleEnabled = true,
            hapticFeedbackEnabled = false,
            emergencyHighContrast = false,
            safetyOrangeAccent = false
        )
    }
}