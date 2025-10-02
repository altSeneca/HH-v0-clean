package com.hazardhawk.ui.camera.clear.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.camera.UnifiedViewfinderCalculator
import com.hazardhawk.ui.camera.clear.theme.ClearDesignTokens

/**
 * Elegant Zoom Control Chips - Jony Ive Design Philosophy
 *
 * Discrete zoom levels presented as refined, minimalist chips:
 * - 1x (Wide)
 * - 2x (Standard)
 * - 5x (Telephoto)
 * - 10x (Super Telephoto)
 * - MAX (Maximum hardware capability)
 *
 * Design: Ultra-thin, translucent, focused
 */
@Composable
fun ElegantZoomChips(
    currentZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    // Define standard zoom levels plus dynamic max
    val zoomLevels = buildList {
        add(1.0f to "1x")
        add(2.0f to "2x")
        if (maxZoom >= 5.0f) add(5.0f to "5x")
        if (maxZoom >= 10.0f) add(10.0f to "10x")
        if (maxZoom > 10.0f) add(maxZoom to "${maxZoom.toInt()}x")
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = ClearDesignTokens.Colors.TranslucentDark70
        ),
        shape = RoundedCornerShape(ClearDesignTokens.CornerRadius.Pill)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            zoomLevels.forEach { (zoomLevel, label) ->
                val isCurrentZoom = kotlin.math.abs(currentZoom - zoomLevel) < 0.1f

                // Subtle scale animation for active chip
                val scale by animateFloatAsState(
                    targetValue = if (isCurrentZoom) 1.05f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "zoom_chip_scale"
                )

                FilterChip(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onZoomChange(zoomLevel)
                    },
                    label = {
                        Text(
                            text = label,
                            fontWeight = if (isCurrentZoom) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    },
                    selected = isCurrentZoom,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ClearDesignTokens.Colors.SafetyOrange,
                        selectedLabelColor = Color.White,
                        containerColor = Color.Transparent,
                        labelColor = ClearDesignTokens.Colors.TranslucentWhite90
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isCurrentZoom,
                        borderColor = if (isCurrentZoom)
                            ClearDesignTokens.Colors.SafetyOrange
                        else
                            ClearDesignTokens.Colors.TranslucentWhite30,
                        selectedBorderColor = ClearDesignTokens.Colors.SafetyOrange,
                        borderWidth = if (isCurrentZoom) 2.dp else 1.dp,
                        selectedBorderWidth = 2.dp
                    ),
                    modifier = Modifier.heightIn(min = 32.dp)
                )
            }
        }
    }
}

/**
 * Elegant Aspect Ratio Chips - Jony Ive Design Philosophy
 *
 * Four standard aspect ratios presented as refined chips:
 * - Square (1:1) - Instagram-style
 * - 4:3 - Classic photography
 * - 16:9 - Widescreen
 * - 3:2 - Full-frame DSLR
 *
 * Design: Minimal, focused, intentional
 */
@Composable
fun ElegantAspectRatioChips(
    currentAspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    onAspectRatioChange: (UnifiedViewfinderCalculator.ViewfinderAspectRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = ClearDesignTokens.Colors.TranslucentDark70
        ),
        shape = RoundedCornerShape(ClearDesignTokens.CornerRadius.Pill)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.values()
                .filter { it.isStandard }
                .forEach { aspectRatio ->
                    val isCurrentRatio = currentAspectRatio == aspectRatio

                    // Subtle scale animation for active chip
                    val scale by animateFloatAsState(
                        targetValue = if (isCurrentRatio) 1.05f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "aspect_chip_scale"
                    )

                    FilterChip(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAspectRatioChange(aspectRatio)
                        },
                        label = {
                            Text(
                                text = aspectRatio.label,
                                fontWeight = if (isCurrentRatio) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        selected = isCurrentRatio,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ClearDesignTokens.Colors.SafetyOrange,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Transparent,
                            labelColor = ClearDesignTokens.Colors.TranslucentWhite90
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isCurrentRatio,
                            borderColor = if (isCurrentRatio)
                                ClearDesignTokens.Colors.SafetyOrange
                            else
                                ClearDesignTokens.Colors.TranslucentWhite30,
                            selectedBorderColor = ClearDesignTokens.Colors.SafetyOrange,
                            borderWidth = if (isCurrentRatio) 2.dp else 1.dp,
                            selectedBorderWidth = 2.dp
                        ),
                        modifier = Modifier.heightIn(min = 32.dp)
                    )
                }
        }
    }
}

/**
 * Combined Zoom + Aspect Ratio Controls
 *
 * Stacked vertically above bottom bar for optimal ergonomics
 * Design: Minimalist, unobtrusive, essential
 */
@Composable
fun ElegantCameraControls(
    currentZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
    currentAspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    onAspectRatioChange: (UnifiedViewfinderCalculator.ViewfinderAspectRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Zoom Controls
        ElegantZoomChips(
            currentZoom = currentZoom,
            maxZoom = maxZoom,
            onZoomChange = onZoomChange
        )

        // Aspect Ratio Controls
        ElegantAspectRatioChips(
            currentAspectRatio = currentAspectRatio,
            onAspectRatioChange = onAspectRatioChange
        )
    }
}
