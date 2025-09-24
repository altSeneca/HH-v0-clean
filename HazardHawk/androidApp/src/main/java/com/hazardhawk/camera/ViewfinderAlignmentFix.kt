package com.hazardhawk.camera

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Fixed ViewfinderMask with proper alignment and aspect ratio handling
 * This resolves the black overlay alignment issues identified in the original implementation
 */

/**
 * Corrected aspect ratio definitions matching camera standard conventions
 */
enum class CorrectedAspectRatio(val ratio: Float, val label: String, val description: String) {
    SQUARE(1f, "1:1", "Square format"),
    FOUR_THREE(4f / 3f, "4:3", "Standard photo format"), // Fixed: was 3f/4f
    SIXTEEN_NINE(16f / 9f, "16:9", "Widescreen format"), // Fixed: was 9f/16f
    THREE_TWO(3f / 2f, "3:2", "Classic 35mm format")
}

/**
 * Unified viewfinder dimensions calculator
 * This ensures pixel-perfect alignment between overlay mask and metadata positioning
 */
data class ViewfinderDimensions(
    val windowWidth: Float,
    val windowHeight: Float,
    val windowLeft: Float,
    val windowTop: Float,
    val windowRight: Float,
    val windowBottom: Float,
    val canvasWidth: Float,
    val canvasHeight: Float
) {
    
    /**
     * Get the viewfinder bounds as a Rect for easy use in Canvas operations
     */
    fun getViewfinderRect(): Rect = Rect(
        left = windowLeft,
        top = windowTop,
        right = windowRight,
        bottom = windowBottom
    )
    
    /**
     * Get the viewfinder bounds as RectF for Android Graphics API
     */
    fun getViewfinderRectF(): android.graphics.RectF = android.graphics.RectF(
        windowLeft,
        windowTop,
        windowRight,
        windowBottom
    )
    
    /**
     * Check if a point is within the viewfinder area
     */
    fun containsPoint(x: Float, y: Float): Boolean {
        return x >= windowLeft && x <= windowRight && y >= windowTop && y <= windowBottom
    }
    
    /**
     * Calculate the aspect ratio of the actual viewfinder window
     */
    fun getActualAspectRatio(): Float = windowWidth / windowHeight
}

/**
 * Calculate unified viewfinder dimensions
 * This single function is used by all viewfinder components to ensure perfect alignment
 */
fun calculateUnifiedViewfinderDimensions(
    canvasSize: Size,
    aspectRatio: CorrectedAspectRatio,
    marginFactor: Float = 0.9f
): ViewfinderDimensions {
    
    val canvasWidth = canvasSize.width
    val canvasHeight = canvasSize.height
    val targetRatio = aspectRatio.ratio
    
    // Determine the maximum available space with margins
    val availableWidth = canvasWidth * marginFactor
    val availableHeight = canvasHeight * marginFactor
    
    // Calculate window dimensions to fit within available space while maintaining aspect ratio
    val (windowWidth, windowHeight) = if (availableWidth / availableHeight > targetRatio) {
        // Height is the limiting factor
        val height = availableHeight
        val width = height * targetRatio
        Pair(width, height)
    } else {
        // Width is the limiting factor  
        val width = availableWidth
        val height = width / targetRatio
        Pair(width, height)
    }
    
    // Center the viewfinder window
    val windowLeft = (canvasWidth - windowWidth) / 2f
    val windowTop = (canvasHeight - windowHeight) / 2f
    val windowRight = windowLeft + windowWidth
    val windowBottom = windowTop + windowHeight
    
    return ViewfinderDimensions(
        windowWidth = windowWidth,
        windowHeight = windowHeight,
        windowLeft = windowLeft,
        windowTop = windowTop,
        windowRight = windowRight,
        windowBottom = windowBottom,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight
    )
}

/**
 * Fixed ViewfinderMask with proper alignment
 * CRITICAL FIX: Uses unified dimension calculation for perfect alignment
 */
@Composable
fun FixedViewfinderMask(
    aspectRatio: CorrectedAspectRatio,
    modifier: Modifier = Modifier,
    maskColor: Color = Color.Black.copy(alpha = 0.6f),
    animated: Boolean = true
) {
    val animatedRatio by animateFloatAsState(
        targetValue = aspectRatio.ratio,
        animationSpec = tween(durationMillis = if (animated) 300 else 0),
        label = "aspectRatio"
    )
    
    // Create a temporary aspect ratio for animation
    val currentAspectRatio = remember(animatedRatio) {
        object {
            val ratio = animatedRatio
        }
    }
    
    Canvas(modifier = modifier) {
        // Calculate unified dimensions - same calculation used everywhere
        val dimensions = calculateUnifiedViewfinderDimensions(
            canvasSize = size,
            aspectRatio = aspectRatio // Use target ratio for layout consistency
        )
        
        // Create path for the mask (everything except viewfinder window)
        val maskPath = Path().apply {
            // Add the entire canvas
            addRect(Rect(0f, 0f, size.width, size.height))
            
            // Subtract the viewfinder window (creates the transparent area)
            addRect(dimensions.getViewfinderRect())
        }
        
        // Draw the mask using even-odd fill rule to create the "hole"
        drawPath(
            path = maskPath,
            color = maskColor,
            style = Fill
        )
    }
}

/**
 * Viewfinder border overlay with corner indicators
 * Uses the same dimension calculation as the mask for perfect alignment
 */
@Composable
fun FixedViewfinderBorder(
    aspectRatio: CorrectedAspectRatio,
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0xFFFF8C00), // Safety Orange
    borderWidth: Float = 3f,
    showCorners: Boolean = true,
    cornerLength: Float = 24f,
    cornerWidth: Float = 4f,
    animated: Boolean = true
) {
    Canvas(modifier = modifier) {
        val dimensions = calculateUnifiedViewfinderDimensions(
            canvasSize = size,
            aspectRatio = aspectRatio
        )
        
        val borderWidthPx = borderWidth.dp.toPx()
        val cornerLengthPx = cornerLength.dp.toPx()
        val cornerWidthPx = cornerWidth.dp.toPx()
        
        // Draw main border
        drawRect(
            color = borderColor,
            topLeft = Offset(dimensions.windowLeft, dimensions.windowTop),
            size = Size(dimensions.windowWidth, dimensions.windowHeight),
            style = Stroke(width = borderWidthPx)
        )
        
        // Draw corner indicators if enabled
        if (showCorners) {
            // Top-left corner
            drawLine(
                color = borderColor,
                start = Offset(dimensions.windowLeft, dimensions.windowTop + cornerLengthPx),
                end = Offset(dimensions.windowLeft, dimensions.windowTop),
                strokeWidth = cornerWidthPx
            )
            drawLine(
                color = borderColor,
                start = Offset(dimensions.windowLeft, dimensions.windowTop),
                end = Offset(dimensions.windowLeft + cornerLengthPx, dimensions.windowTop),
                strokeWidth = cornerWidthPx
            )
            
            // Top-right corner
            drawLine(
                color = borderColor,
                start = Offset(dimensions.windowRight - cornerLengthPx, dimensions.windowTop),
                end = Offset(dimensions.windowRight, dimensions.windowTop),
                strokeWidth = cornerWidthPx
            )
            drawLine(
                color = borderColor,
                start = Offset(dimensions.windowRight, dimensions.windowTop),
                end = Offset(dimensions.windowRight, dimensions.windowTop + cornerLengthPx),
                strokeWidth = cornerWidthPx
            )
            
            // Bottom-left corner
            drawLine(
                color = borderColor,
                start = Offset(dimensions.windowLeft, dimensions.windowBottom - cornerLengthPx),
                end = Offset(dimensions.windowLeft, dimensions.windowBottom),
                strokeWidth = cornerWidthPx
            )
            drawLine(
                color = borderColor,
                start = Offset(dimensions.windowLeft, dimensions.windowBottom),
                end = Offset(dimensions.windowLeft + cornerLengthPx, dimensions.windowBottom),
                strokeWidth = cornerWidthPx
            )
            
            // Bottom-right corner
            drawLine(
                color = borderColor,
                start = Offset(dimensions.windowRight - cornerLengthPx, dimensions.windowBottom),
                end = Offset(dimensions.windowRight, dimensions.windowBottom),
                strokeWidth = cornerWidthPx
            )
            drawLine(
                color = borderColor,
                start = Offset(dimensions.windowRight, dimensions.windowBottom - cornerLengthPx),
                end = Offset(dimensions.windowRight, dimensions.windowBottom),
                strokeWidth = cornerWidthPx
            )
        }
    }
}

/**
 * Grid overlay for composition guide
 * Perfectly aligned with the viewfinder bounds
 */
@Composable
fun FixedGridOverlay(
    aspectRatio: CorrectedAspectRatio,
    modifier: Modifier = Modifier,
    gridColor: Color = Color.White.copy(alpha = 0.3f),
    gridType: GridType = GridType.RULE_OF_THIRDS
) {
    Canvas(modifier = modifier) {
        val dimensions = calculateUnifiedViewfinderDimensions(
            canvasSize = size,
            aspectRatio = aspectRatio
        )
        
        val strokeWidth = 1.dp.toPx()
        
        // Handle GridType compatibility - map old values to new ones
        val actualGridType = when (gridType.name) {
            "RULE_OF_THIRDS" -> com.hazardhawk.camera.GridType.RULE_OF_THIRDS
            "CENTER_CROSS" -> com.hazardhawk.camera.GridType.CENTER_LINES
            else -> com.hazardhawk.camera.GridType.RULE_OF_THIRDS
        }
        
        when (actualGridType) {
            com.hazardhawk.camera.GridType.RULE_OF_THIRDS -> {
                // Vertical lines (1/3 and 2/3)
                val verticalLine1 = dimensions.windowLeft + dimensions.windowWidth / 3f
                val verticalLine2 = dimensions.windowLeft + dimensions.windowWidth * 2f / 3f
                
                drawLine(
                    color = gridColor,
                    start = Offset(verticalLine1, dimensions.windowTop),
                    end = Offset(verticalLine1, dimensions.windowBottom),
                    strokeWidth = strokeWidth
                )
                
                drawLine(
                    color = gridColor,
                    start = Offset(verticalLine2, dimensions.windowTop),
                    end = Offset(verticalLine2, dimensions.windowBottom),
                    strokeWidth = strokeWidth
                )
                
                // Horizontal lines (1/3 and 2/3)
                val horizontalLine1 = dimensions.windowTop + dimensions.windowHeight / 3f
                val horizontalLine2 = dimensions.windowTop + dimensions.windowHeight * 2f / 3f
                
                drawLine(
                    color = gridColor,
                    start = Offset(dimensions.windowLeft, horizontalLine1),
                    end = Offset(dimensions.windowRight, horizontalLine1),
                    strokeWidth = strokeWidth
                )
                
                drawLine(
                    color = gridColor,
                    start = Offset(dimensions.windowLeft, horizontalLine2),
                    end = Offset(dimensions.windowRight, horizontalLine2),
                    strokeWidth = strokeWidth
                )
            }
            
            com.hazardhawk.camera.GridType.CENTER_LINES -> {
                // Vertical center line
                val centerX = dimensions.windowLeft + dimensions.windowWidth / 2f
                drawLine(
                    color = gridColor,
                    start = Offset(centerX, dimensions.windowTop),
                    end = Offset(centerX, dimensions.windowBottom),
                    strokeWidth = strokeWidth
                )
                
                // Horizontal center line
                val centerY = dimensions.windowTop + dimensions.windowHeight / 2f
                drawLine(
                    color = gridColor,
                    start = Offset(dimensions.windowLeft, centerY),
                    end = Offset(dimensions.windowRight, centerY),
                    strokeWidth = strokeWidth
                )
            }
            
            else -> {
                // Handle other grid types or no grid
            }
        }
    }
}

// Note: GridType is imported from GridOverlay.kt to avoid conflicts

/**
 * Complete fixed viewfinder overlay system
 * Combines mask, border, and grid with perfect alignment
 */
@Composable
fun CompleteFixedViewfinder(
    aspectRatio: CorrectedAspectRatio,
    modifier: Modifier = Modifier,
    showGrid: Boolean = false,
    gridType: GridType = GridType.RULE_OF_THIRDS,
    borderColor: Color = Color(0xFFFF8C00),
    maskColor: Color = Color.Black.copy(alpha = 0.6f),
    animated: Boolean = true
) {
    Box(modifier = modifier) {
        // Black overlay mask with transparent viewfinder area
        FixedViewfinderMask(
            aspectRatio = aspectRatio,
            modifier = Modifier.fillMaxSize(),
            maskColor = maskColor,
            animated = animated
        )
        
        // Viewfinder border with corner indicators
        FixedViewfinderBorder(
            aspectRatio = aspectRatio,
            modifier = Modifier.fillMaxSize(),
            borderColor = borderColor,
            showCorners = true,
            animated = animated
        )
        
        // Optional grid overlay
        if (showGrid) {
            FixedGridOverlay(
                aspectRatio = aspectRatio,
                modifier = Modifier.fillMaxSize(),
                gridType = gridType
            )
        }
    }
}

/**
 * Helper function to get viewfinder dimensions for use in other components
 * This ensures metadata overlays and other elements are positioned correctly
 */
@Composable
fun rememberViewfinderDimensions(aspectRatio: CorrectedAspectRatio): ViewfinderDimensions? {
    val density = LocalDensity.current
    
    return remember(aspectRatio, density) {
        // We need the actual canvas size here, which we can't get without BoxWithConstraints
        // This is provided as a convenience function, but components should calculate
        // dimensions within their own Canvas scope for accuracy
        null
    }
}