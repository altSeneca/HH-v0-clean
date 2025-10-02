package com.hazardhawk.ui.camera.clear.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazardhawk.ui.camera.clear.theme.ClearDesignTokens

/**
 * MinimalTopBar - Always-Visible Top Controls
 *
 * Three essential controls:
 * - AR Mode Toggle (left)
 * - Project Selector (center)
 * - Flash Control (right)
 *
 * Design Philosophy: Minimal, translucent, recedes into background
 */
@Composable
fun MinimalTopBar(
    isARMode: Boolean,
    onARToggle: () -> Unit,
    currentProject: String,
    onProjectTap: () -> Unit,
    flashMode: FlashMode,
    onFlashToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(ClearDesignTokens.Spacing.Large),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AR Toggle (Left)
        MinimalIconButton(
            icon = Icons.Default.ViewInAr,
            isActive = isARMode,
            contentDescription = "AR Mode",
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onARToggle()
            }
        )

        // Project Selector (Center)
        ProjectSelectorPill(
            projectName = currentProject,
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onProjectTap()
            }
        )

        // Flash Toggle (Right)
        FlashControlButton(
            flashMode = flashMode,
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onFlashToggle()
            }
        )
    }
}

/**
 * Minimal Icon Button - Clean, line-art style
 * Glows orange when active
 */
@Composable
private fun MinimalIconButton(
    icon: ImageVector,
    isActive: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Subtle pulse animation when active
    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    Box(
        modifier = modifier
            .size(ClearDesignTokens.Sizing.MinTouchTarget)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isActive) ClearDesignTokens.Colors.SafetyOrange
                else ClearDesignTokens.Colors.TranslucentDark40
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) Color.White else ClearDesignTokens.Colors.TranslucentWhite90,
            modifier = Modifier.size(ClearDesignTokens.Sizing.MediumIcon)
        )
    }
}

/**
 * Project Selector Pill - Thin, elegant, tappable
 * Shows current project, tapping opens full-screen project list
 */
@Composable
private fun ProjectSelectorPill(
    projectName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(max = 40.dp)
            .widthIn(max = 240.dp)
            .clip(RoundedCornerShape(ClearDesignTokens.CornerRadius.Pill))
            .clickable(onClick = onClick),
        color = ClearDesignTokens.Colors.TranslucentDark70,
        shape = RoundedCornerShape(ClearDesignTokens.CornerRadius.Pill)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = ClearDesignTokens.Spacing.Medium,
                vertical = ClearDesignTokens.Spacing.Small
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ClearDesignTokens.Spacing.XSmall)
        ) {
            Icon(
                imageVector = Icons.Default.Engineering,
                contentDescription = null,
                tint = ClearDesignTokens.Colors.SafetyOrange,
                modifier = Modifier.size(ClearDesignTokens.Sizing.SmallIcon)
            )

            Text(
                text = "Project: $projectName",
                color = ClearDesignTokens.Colors.TranslucentWhite90,
                fontSize = ClearDesignTokens.Typography.SmallText,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select Project",
                tint = ClearDesignTokens.Colors.TranslucentWhite70,
                modifier = Modifier.size(ClearDesignTokens.Sizing.SmallIcon)
            )
        }
    }
}

/**
 * Flash Control Button - Cycles through Off, On, Auto
 * Shows current mode with subtle text label
 */
@Composable
private fun FlashControlButton(
    flashMode: FlashMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, label) = when (flashMode) {
        FlashMode.OFF -> Icons.Default.FlashOff to "OFF"
        FlashMode.ON -> Icons.Default.FlashOn to "ON"
        FlashMode.AUTO -> Icons.Default.FlashAuto to "AUTO"
    }

    val isActive = flashMode != FlashMode.OFF

    Column(
        modifier = modifier
            .size(ClearDesignTokens.Sizing.MinTouchTarget)
            .clip(CircleShape)
            .background(
                if (isActive) ClearDesignTokens.Colors.SafetyOrange
                else ClearDesignTokens.Colors.TranslucentDark40
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Flash $label",
            tint = if (isActive) Color.White else ClearDesignTokens.Colors.TranslucentWhite90,
            modifier = Modifier.size(ClearDesignTokens.Sizing.MediumIcon)
        )

        if (isActive && flashMode == FlashMode.AUTO) {
            Text(
                text = label,
                color = Color.White,
                fontSize = ClearDesignTokens.Typography.MicroText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Flash Mode States
 */
enum class FlashMode {
    OFF,
    ON,
    AUTO
}
