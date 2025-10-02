package com.hazardhawk.ui.camera.clear.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hazardhawk.camera.UnifiedViewfinderCalculator
import com.hazardhawk.ui.camera.clear.theme.ClearDesignTokens

/**
 * Viewfinder Aspect Ratio Frame - Jony Ive Design Philosophy
 *
 * Displays a subtle, refined frame indicating the selected aspect ratio
 * within the camera viewfinder. The frame helps users understand exactly
 * what will be captured.
 *
 * Design Principles:
 * - Ultra-thin stroke (hairline)
 * - Subtle color (translucent white)
 * - Corner indicators (L-shaped brackets)
 * - Responsive to aspect ratio changes
 *
 * Visual Language: Precision, clarity, intention
 */
@Composable
fun BoxScope.ViewfinderAspectRatioFrame(
    aspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Calculate frame dimensions based on aspect ratio
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Calculate the frame rectangle
    val frameRect = remember(aspectRatio, screenWidth, screenHeight) {
        calculateFrameRect(aspectRatio, screenWidth, screenHeight)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 2.dp.toPx()
        val cornerLength = 24.dp.toPx()
        val frameColor = ClearDesignTokens.Colors.TranslucentWhite50

        // Draw corner brackets (L-shaped indicators)
        // Top-left corner
        drawLine(
            color = frameColor,
            start = Offset(frameRect.left, frameRect.top),
            end = Offset(frameRect.left + cornerLength, frameRect.top),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = frameColor,
            start = Offset(frameRect.left, frameRect.top),
            end = Offset(frameRect.left, frameRect.top + cornerLength),
            strokeWidth = strokeWidth
        )

        // Top-right corner
        drawLine(
            color = frameColor,
            start = Offset(frameRect.right, frameRect.top),
            end = Offset(frameRect.right - cornerLength, frameRect.top),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = frameColor,
            start = Offset(frameRect.right, frameRect.top),
            end = Offset(frameRect.right, frameRect.top + cornerLength),
            strokeWidth = strokeWidth
        )

        // Bottom-left corner
        drawLine(
            color = frameColor,
            start = Offset(frameRect.left, frameRect.bottom),
            end = Offset(frameRect.left + cornerLength, frameRect.bottom),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = frameColor,
            start = Offset(frameRect.left, frameRect.bottom),
            end = Offset(frameRect.left, frameRect.bottom - cornerLength),
            strokeWidth = strokeWidth
        )

        // Bottom-right corner
        drawLine(
            color = frameColor,
            start = Offset(frameRect.right, frameRect.bottom),
            end = Offset(frameRect.right - cornerLength, frameRect.bottom),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = frameColor,
            start = Offset(frameRect.right, frameRect.bottom),
            end = Offset(frameRect.right, frameRect.bottom - cornerLength),
            strokeWidth = strokeWidth
        )

        // Optional: Draw full frame (very subtle)
        drawRect(
            color = frameColor.copy(alpha = 0.2f),
            topLeft = Offset(frameRect.left, frameRect.top),
            size = Size(frameRect.width, frameRect.height),
            style = Stroke(width = strokeWidth / 2)
        )
    }
}

/**
 * Calculate frame rectangle based on aspect ratio and screen dimensions
 *
 * IMPORTANT: Aspect ratios are portrait-oriented (height:width) for phone cameras
 * - 4:3 means 4 tall, 3 wide (portrait) = 3:4 landscape
 * - 16:9 means 16 tall, 9 wide (portrait) = 9:16 landscape
 */
private fun calculateFrameRect(
    aspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    screenWidth: Float,
    screenHeight: Float
): Rect {
    // Calculate target height:width ratio for portrait orientation
    val targetRatio = when (aspectRatio) {
        UnifiedViewfinderCalculator.ViewfinderAspectRatio.SQUARE -> 1.0f // 1:1
        UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE -> 4.0f / 3.0f // 4:3 (portrait)
        UnifiedViewfinderCalculator.ViewfinderAspectRatio.SIXTEEN_NINE -> 16.0f / 9.0f // 16:9 (portrait)
        UnifiedViewfinderCalculator.ViewfinderAspectRatio.THREE_TWO -> 3.0f / 2.0f // 3:2 (portrait)
    }

    // Calculate frame dimensions to fit within screen while maintaining aspect ratio
    val frameWidth: Float
    val frameHeight: Float
    val currentRatio = screenHeight / screenWidth // Note: height/width for portrait

    if (currentRatio > targetRatio) {
        // Screen is taller than target ratio - constrain by width
        frameWidth = screenWidth * 0.95f // Leave some margin
        frameHeight = frameWidth * targetRatio
    } else {
        // Screen is wider than target ratio - constrain by height
        frameHeight = screenHeight * 0.95f // Leave some margin
        frameWidth = frameHeight / targetRatio
    }

    // Center the frame
    val left = (screenWidth - frameWidth) / 2
    val top = (screenHeight - frameHeight) / 2

    return Rect(
        left = left,
        top = top,
        right = left + frameWidth,
        bottom = top + frameHeight
    )
}
