package com.hazardhawk.ui.camera.clear.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.hazardhawk.ui.camera.clear.theme.ClearDesignTokens

/**
 * ElegantBottomBar - Auto-Hiding Bottom Controls
 *
 * Three primary actions:
 * - Gallery (left)
 * - Capture (center) - The iconic capture button
 * - Settings (right)
 *
 * Auto-hides after 8 seconds of inactivity.
 * Tap anywhere on screen to restore.
 */
@Composable
fun ElegantBottomBar(
    visible: Boolean,
    isCapturing: Boolean,
    onGalleryClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(ClearDesignTokens.Animation.Normal)) +
                slideInVertically(
                    animationSpec = tween(ClearDesignTokens.Animation.Normal),
                    initialOffsetY = { it / 2 }
                ),
        exit = fadeOut(animationSpec = tween(ClearDesignTokens.Animation.Slow)) +
               slideOutVertically(
                   animationSpec = tween(ClearDesignTokens.Animation.Slow),
                   targetOffsetY = { it / 2 }
               ),
        modifier = modifier
    ) {
        // No background - pure minimalism, controls float over camera view
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ClearDesignTokens.Spacing.XLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Button (Left)
            BottomBarIconButton(
                icon = Icons.Default.PhotoLibrary,
                contentDescription = "Gallery",
                onClick = onGalleryClick
            )

            // Capture Button (Center) - The Star of the Show
            CaptureButton(
                isCapturing = isCapturing,
                onClick = onCaptureClick
            )

            // Settings Button (Right)
            BottomBarIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                onClick = onSettingsClick
            )
        }
    }
}

/**
 * Bottom Bar Icon Button - Outline style, subtle
 */
@Composable
private fun BottomBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(ClearDesignTokens.Sizing.LargeTouchTarget)
            .clip(CircleShape)
            .background(ClearDesignTokens.Colors.TranslucentDark40)
            .clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = ClearDesignTokens.Colors.TranslucentWhite90,
            modifier = Modifier.size(ClearDesignTokens.Sizing.MediumIcon)
        )
    }
}

/**
 * CaptureButton - The Iconic iOS-Style Capture Button
 *
 * White circle with orange ring.
 * Ring pulses subtly when ready.
 * Shows progress indicator when capturing.
 */
@Composable
private fun CaptureButton(
    isCapturing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    // Subtle pulse animation when ready to capture
    val infiniteTransition = rememberInfiniteTransition(label = "capture_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (!isCapturing) 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    Box(
        modifier = modifier
            .size(ClearDesignTokens.Sizing.CaptureButton)
            .clip(CircleShape)
            .clickable(enabled = !isCapturing) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer Ring (Orange) - Pulses when ready
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(ringScale)
                .background(
                    color = if (isCapturing)
                        ClearDesignTokens.Colors.SafetyOrange.copy(alpha = 0.5f)
                    else
                        ClearDesignTokens.Colors.SafetyOrange,
                    shape = CircleShape
                )
        )

        // Inner Circle (White) - Solid background
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(
                    color = ClearDesignTokens.Colors.PureWhite,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCapturing) {
                // Progress Indicator when capturing
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = ClearDesignTokens.Colors.SafetyOrange,
                    strokeWidth = 3.dp
                )
            } else {
                // Inner Orange Dot when ready
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = ClearDesignTokens.Colors.SafetyOrange,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
