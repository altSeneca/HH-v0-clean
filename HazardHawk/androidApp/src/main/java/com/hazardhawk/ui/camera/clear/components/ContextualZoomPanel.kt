package com.hazardhawk.ui.camera.clear.components

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.camera.UnifiedViewfinderCalculator

/**
 * ContextualZoomPanel - Minimalist Right-Edge Slide-Out Panel
 *
 * "Less, but Better" - Ultra-minimal text-based control panel
 *
 * Design Principles:
 * - Translucent blurred background (Android 12+) or dark fallback
 * - Text labels only - no opaque buttons
 * - Safety orange accent for active state (text color only)
 * - Subtle spacing and dividers
 * - Haptic feedback on interaction
 * - Smooth slide animations from off-screen (250-300ms)
 * - Fixed width for visual stability
 */
@Composable
fun ContextualZoomPanel(
    visible: Boolean,
    currentZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
    currentAspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    onAspectRatioChange: (UnifiedViewfinderCalculator.ViewfinderAspectRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    val safetyOrange = Color(0xFFFFA500)

    // Simple fade in/out animation - no slide, no background
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(150, easing = LinearOutSlowInEasing)),
        modifier = modifier
            .wrapContentHeight()
            .padding(end = 12.dp, top = 80.dp, bottom = 160.dp)
    ) {
        // Just the text labels - no background, no container
        Column(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom Levels Section
            val zoomLevels = buildList {
                add(1.0f to "1x")
                add(2.0f to "2x")
                if (maxZoom >= 5.0f) add(5.0f to "5x")
                if (maxZoom >= 10.0f) add(10.0f to "10x")
                if (maxZoom > 10.0f) add(maxZoom to "MAX")
            }

            zoomLevels.forEach { (zoomLevel, label) ->
                TextControlItem(
                    label = label,
                    isActive = kotlin.math.abs(currentZoom - zoomLevel) < 0.1f,
                    activeColor = safetyOrange,
                    onClick = { onZoomChange(zoomLevel) }
                )
            }

            // Subtle divider between zoom and aspect ratio
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Aspect Ratio Section
            val aspectRatios = listOf(
                UnifiedViewfinderCalculator.ViewfinderAspectRatio.SQUARE,
                UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE,
                UnifiedViewfinderCalculator.ViewfinderAspectRatio.SIXTEEN_NINE,
                UnifiedViewfinderCalculator.ViewfinderAspectRatio.THREE_TWO
            )

            aspectRatios.forEach { aspectRatio ->
                TextControlItem(
                    label = aspectRatio.label,
                    isActive = currentAspectRatio == aspectRatio,
                    activeColor = safetyOrange,
                    onClick = { onAspectRatioChange(aspectRatio) }
                )
            }
        }
    }
}

/**
 * TextControlItem - Minimalist Text-Only Control
 *
 * Design Principles:
 * - Text label only - no background shape
 * - Active state: Safety orange text color
 * - Inactive state: White text with transparency
 * - Generous tap target padding
 * - Haptic feedback on tap
 */
@Composable
fun TextControlItem(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    Text(
        text = label,
        color = if (isActive) activeColor else Color.White,
        fontSize = 14.sp,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1,
        style = TextStyle(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.75f),
                offset = Offset(0f, 2f),
                blurRadius = 4f
            )
        ),
        modifier = modifier
            .clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    )
}
