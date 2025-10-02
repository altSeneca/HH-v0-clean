package com.hazardhawk.ui.camera.clear.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.tween

/**
 * ClearDesignTokens - Minimalist Design System
 *
 * Inspired by principles of clarity, simplicity, and focus.
 * Every element serves a purpose. Technology recedes, content emerges.
 */
object ClearDesignTokens {

    // ══════════════════════════════════════════════════════════════════
    // Color Palette - True Black, Translucent Overlays, Subtle Accents
    // ══════════════════════════════════════════════════════════════════

    object Colors {
        // Background Colors
        val TrueBlack = Color(0xFF000000)
        val DeepCharcoal = Color(0xFF1C1C1E)
        val MidnightGray = Color(0xFF2C2C2E)

        // Translucent Overlays
        val TranslucentDark40 = Color.Black.copy(alpha = 0.4f)
        val TranslucentDark60 = Color.Black.copy(alpha = 0.6f)
        val TranslucentDark70 = Color.Black.copy(alpha = 0.7f)
        val TranslucentDark85 = Color.Black.copy(alpha = 0.85f)

        // White Variants
        val PureWhite = Color(0xFFFFFFFF)
        val TranslucentWhite15 = Color.White.copy(alpha = 0.15f)
        val TranslucentWhite30 = Color.White.copy(alpha = 0.30f)
        val TranslucentWhite50 = Color.White.copy(alpha = 0.50f)
        val TranslucentWhite70 = Color.White.copy(alpha = 0.70f)
        val TranslucentWhite90 = Color.White.copy(alpha = 0.90f)

        // Accent Colors
        val SafetyOrange = Color(0xFFFF6B35)
        val SafetyOrangeGlow = Color(0xFFFF6B35).copy(alpha = 0.3f)
        val CriticalRed = Color(0xFFFF3B30)
        val SuccessGreen = Color(0xFF34C759)

        // Status Colors
        val WarningAmber = Color(0xFFFFCC00)
        val InfoBlue = Color(0xFF007AFF)
    }

    // ══════════════════════════════════════════════════════════════════
    // Typography - San Francisco Inspired, Precise, Legible
    // ══════════════════════════════════════════════════════════════════

    object Typography {
        // Font Sizes
        val MicroText = 9.sp
        val TinyText = 11.sp
        val SmallText = 12.sp
        val BodyText = 14.sp
        val MediumText = 16.sp
        val LargeText = 18.sp
        val TitleText = 20.sp
        val HeaderText = 24.sp

        // Line Heights (as multipliers)
        val TightLineHeight = 1.2f
        val NormalLineHeight = 1.3f
        val RelaxedLineHeight = 1.5f
    }

    // ══════════════════════════════════════════════════════════════════
    // Spacing - Consistent, Rhythmic, 8dp Grid System
    // ══════════════════════════════════════════════════════════════════

    object Spacing {
        val XXSmall = 2.dp
        val XSmall = 4.dp
        val Small = 8.dp
        val Medium = 12.dp
        val Large = 16.dp
        val XLarge = 24.dp
        val XXLarge = 32.dp
        val Huge = 48.dp
    }

    // ══════════════════════════════════════════════════════════════════
    // Sizing - Touch Targets, Icons, Controls
    // ══════════════════════════════════════════════════════════════════

    object Sizing {
        // Touch Targets (minimum 48.dp for accessibility)
        val MinTouchTarget = 48.dp
        val LargeTouchTarget = 56.dp
        val CaptureButton = 80.dp

        // Icon Sizes
        val TinyIcon = 14.dp
        val SmallIcon = 18.dp
        val MediumIcon = 24.dp
        val LargeIcon = 32.dp

        // Control Sizes
        val ZoomPanelWidth = 80.dp
        val BottomBarHeight = 120.dp
        val TopBarHeight = 64.dp
    }

    // ══════════════════════════════════════════════════════════════════
    // Corner Radii - Smooth, Refined Curves
    // ══════════════════════════════════════════════════════════════════

    object CornerRadius {
        val Small = 8.dp
        val Medium = 12.dp
        val Large = 16.dp
        val XLarge = 20.dp
        val Pill = 999.dp  // Fully rounded
    }

    // ══════════════════════════════════════════════════════════════════
    // Blur Effects - Glassmorphism, Depth
    // ══════════════════════════════════════════════════════════════════

    object Blur {
        val Subtle = 8.dp
        val Medium = 12.dp
        val Strong = 24.dp
    }

    // ══════════════════════════════════════════════════════════════════
    // Animation Durations - Deliberate, Smooth Timing
    // ══════════════════════════════════════════════════════════════════

    object Animation {
        // Durations (milliseconds)
        const val Instant = 100
        const val Quick = 200
        const val Normal = 300
        const val Smooth = 400
        const val Slow = 500
        const val Deliberate = 800

        // Timing Functions
        val QuickTween = tween<Float>(durationMillis = Quick)
        val NormalTween = tween<Float>(durationMillis = Normal)
        val SmoothTween = tween<Float>(durationMillis = Smooth)
        val SlowTween = tween<Float>(durationMillis = Slow)
    }

    // ══════════════════════════════════════════════════════════════════
    // Auto-Hide Timing
    // ══════════════════════════════════════════════════════════════════

    object Timing {
        const val BottomBarAutoHideMs = 8000L      // 8 seconds
        const val ZoomPanelAutoHideMs = 3000L       // 3 seconds
        const val AIBannerDismissMs = 5000L         // 5 seconds
        const val TapToFocusHoldMs = 1000L          // 1 second
    }

    // ══════════════════════════════════════════════════════════════════
    // Elevation & Shadows
    // ══════════════════════════════════════════════════════════════════

    object Elevation {
        val None = 0.dp
        val Subtle = 2.dp
        val Medium = 4.dp
        val High = 8.dp
    }

    // ══════════════════════════════════════════════════════════════════
    // Border Widths
    // ══════════════════════════════════════════════════════════════════

    object Border {
        val Hairline = 0.5.dp
        val Thin = 1.dp
        val Medium = 2.dp
        val Thick = 3.dp
    }
}
