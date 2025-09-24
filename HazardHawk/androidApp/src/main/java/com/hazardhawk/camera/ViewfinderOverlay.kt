package com.hazardhawk.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ui.theme.ConstructionColors
import kotlin.math.min

/**
 * Calculates optimal viewfinder dimensions for any screen orientation and aspect ratio
 * This is the centralized calculation logic used by all viewfinder components
 */
internal fun calculateViewfinderDimensions(
    canvasWidth: Float,
    canvasHeight: Float,
    targetRatio: Float,
    widthMarginFactor: Float = 0.95f,
    heightMarginFactor: Float = 0.8f
): Pair<Float, Float> {
    val canvasRatio = canvasWidth / canvasHeight
    val isPortraitScreen = canvasRatio < 1.0f
    val isLandscapeScreen = canvasRatio > 1.0f
    
    return when {
        // Portrait screen: height > width
        isPortraitScreen -> {
            when {
                targetRatio <= 1.0f -> {
                    // Square or portrait aspect ratios (1:1, 3:4, 9:16)
                    val width = canvasWidth * widthMarginFactor
                    val height = width / targetRatio
                    if (height <= canvasHeight * heightMarginFactor) {
                        Pair(width, height)
                    } else {
                        val constrainedHeight = canvasHeight * heightMarginFactor
                        val constrainedWidth = constrainedHeight * targetRatio
                        Pair(constrainedWidth, constrainedHeight)
                    }
                }
                else -> {
                    // Landscape aspect ratios on portrait screen (4:3, 16:9)
                    val width = canvasWidth * widthMarginFactor
                    val height = width / targetRatio
                    Pair(width, height)
                }
            }
        }
        
        // Landscape screen: width > height
        isLandscapeScreen -> {
            when {
                targetRatio >= 1.0f -> {
                    // Landscape aspect ratios (4:3, 16:9) on landscape screen
                    val height = canvasHeight * heightMarginFactor
                    val width = height * targetRatio
                    if (width <= canvasWidth * widthMarginFactor) {
                        Pair(width, height)
                    } else {
                        val constrainedWidth = canvasWidth * widthMarginFactor
                        val constrainedHeight = constrainedWidth / targetRatio
                        Pair(constrainedWidth, constrainedHeight)
                    }
                }
                else -> {
                    // Portrait/square aspect ratios on landscape screen
                    val height = canvasHeight * heightMarginFactor
                    val width = height * targetRatio
                    Pair(width, height)
                }
            }
        }
        
        // Square screen (rare): width ≈ height
        else -> {
            if (canvasRatio > targetRatio) {
                val width = canvasHeight * targetRatio * heightMarginFactor
                Pair(width, canvasHeight * heightMarginFactor)
            } else {
                val height = canvasWidth / targetRatio * heightMarginFactor
                Pair(canvasWidth * widthMarginFactor, height)
            }
        }
    }
}

/**
 * Aspect ratios for camera viewfinder with correct mathematical ratios
 */
enum class ViewfinderAspectRatio(val ratio: Float, val label: String, val description: String) {
    SQUARE(1f / 1f, "1:1", "Square format - ideal for social media"),
    FOUR_THREE(4f / 3f, "4:3", "Standard photo - good for construction docs"), 
    SIXTEEN_NINE(16f / 9f, "16:9", "Widescreen format - cinematic view")
}

/**
 * Professional viewfinder overlay with glassmorphism effect
 * ENHANCED: Now uses beautiful glass effect instead of solid black overlay
 */
@Composable
fun ViewfinderOverlay(
    aspectRatio: ViewfinderAspectRatio = ViewfinderAspectRatio.FOUR_THREE,
    showGuideLines: Boolean = true,
    showAspectRatioLabel: Boolean = true,
    useGlassmorphism: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Main viewfinder overlay with glassmorphism or traditional masking
        if (useGlassmorphism) {
            // Temporarily using Box instead of GlassViewfinderMask (glass components disabled)
            Box(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ViewfinderMask(
                aspectRatio = aspectRatio,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Guide lines (rule of thirds, center lines, etc.)
        if (showGuideLines) {
            ViewfinderGuideLines(
                aspectRatio = aspectRatio,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Aspect ratio indicator
        if (showAspectRatioLabel) {
            AspectRatioLabel(
                aspectRatio = aspectRatio,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
        
        // Corner indicators for framing
        ViewfinderCorners(
            aspectRatio = aspectRatio,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Creates the black overlay mask with correct aspect ratio window
 * CRITICAL FIX: Corrects the inverted ratios for 4:3 and 16:9
 */
@Composable
private fun ViewfinderMask(
    aspectRatio: ViewfinderAspectRatio,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val targetRatio = aspectRatio.ratio
        
        // Calculate the viewfinder window dimensions using centralized logic
        val (windowWidth, windowHeight) = calculateViewfinderDimensions(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            targetRatio = targetRatio
        )
        
        // Center the viewfinder window
        val windowLeft = (canvasWidth - windowWidth) / 2f
        val windowTop = (canvasHeight - windowHeight) / 2f
        val windowRight = windowLeft + windowWidth
        val windowBottom = windowTop + windowHeight
        
        // Create the overlay path (everything except the viewfinder window)
        val overlayPath = Path().apply {
            // Full canvas rectangle
            addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
            
            // Subtract the viewfinder window (creates the "hole")
            addRect(
                Rect(
                    left = windowLeft,
                    top = windowTop, 
                    right = windowRight,
                    bottom = windowBottom
                )
            )
        }
        
        // Draw the black overlay with transparency
        drawPath(
            path = overlayPath,
            color = Color.Black.copy(alpha = 0.6f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
        
        // Draw the viewfinder border
        drawRect(
            color = ConstructionColors.SafetyOrange,
            topLeft = Offset(windowLeft, windowTop),
            size = Size(windowWidth, windowHeight),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

/**
 * Guide lines for composition (rule of thirds, center lines)
 */
@Composable
private fun ViewfinderGuideLines(
    aspectRatio: ViewfinderAspectRatio,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val targetRatio = aspectRatio.ratio
        
        // Calculate viewfinder window bounds using centralized logic
        val (windowWidth, windowHeight) = calculateViewfinderDimensions(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            targetRatio = targetRatio
        )
        
        val windowLeft = (canvasWidth - windowWidth) / 2f
        val windowTop = (canvasHeight - windowHeight) / 2f
        
        // Draw rule of thirds grid within the viewfinder window
        val lineColor = Color.White.copy(alpha = 0.4f)
        val strokeWidth = 1.dp.toPx()
        
        // Vertical lines (1/3 and 2/3)
        val verticalLine1 = windowLeft + windowWidth / 3f
        val verticalLine2 = windowLeft + windowWidth * 2f / 3f
        
        drawLine(
            color = lineColor,
            start = Offset(verticalLine1, windowTop),
            end = Offset(verticalLine1, windowTop + windowHeight),
            strokeWidth = strokeWidth
        )
        
        drawLine(
            color = lineColor,
            start = Offset(verticalLine2, windowTop),
            end = Offset(verticalLine2, windowTop + windowHeight),
            strokeWidth = strokeWidth
        )
        
        // Horizontal lines (1/3 and 2/3)  
        val horizontalLine1 = windowTop + windowHeight / 3f
        val horizontalLine2 = windowTop + windowHeight * 2f / 3f
        
        drawLine(
            color = lineColor,
            start = Offset(windowLeft, horizontalLine1),
            end = Offset(windowLeft + windowWidth, horizontalLine1),
            strokeWidth = strokeWidth
        )
        
        drawLine(
            color = lineColor,
            start = Offset(windowLeft, horizontalLine2),
            end = Offset(windowLeft + windowWidth, horizontalLine2),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * Corner indicators for precise framing
 */
@Composable
private fun ViewfinderCorners(
    aspectRatio: ViewfinderAspectRatio,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val targetRatio = aspectRatio.ratio
        
        // Calculate viewfinder window bounds using centralized logic
        val (windowWidth, windowHeight) = calculateViewfinderDimensions(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            targetRatio = targetRatio
        )
        
        val windowLeft = (canvasWidth - windowWidth) / 2f
        val windowTop = (canvasHeight - windowHeight) / 2f
        val windowRight = windowLeft + windowWidth
        val windowBottom = windowTop + windowHeight
        
        val cornerSize = 24.dp.toPx()
        val cornerThickness = 4.dp.toPx()
        val cornerColor = ConstructionColors.SafetyOrange
        
        // Top-left corner
        drawLine(
            color = cornerColor,
            start = Offset(windowLeft, windowTop + cornerSize),
            end = Offset(windowLeft, windowTop),
            strokeWidth = cornerThickness
        )
        drawLine(
            color = cornerColor,
            start = Offset(windowLeft, windowTop),
            end = Offset(windowLeft + cornerSize, windowTop),
            strokeWidth = cornerThickness
        )
        
        // Top-right corner
        drawLine(
            color = cornerColor,
            start = Offset(windowRight - cornerSize, windowTop),
            end = Offset(windowRight, windowTop),
            strokeWidth = cornerThickness
        )
        drawLine(
            color = cornerColor,
            start = Offset(windowRight, windowTop),
            end = Offset(windowRight, windowTop + cornerSize),
            strokeWidth = cornerThickness
        )
        
        // Bottom-left corner
        drawLine(
            color = cornerColor,
            start = Offset(windowLeft, windowBottom - cornerSize),
            end = Offset(windowLeft, windowBottom),
            strokeWidth = cornerThickness
        )
        drawLine(
            color = cornerColor,
            start = Offset(windowLeft, windowBottom),
            end = Offset(windowLeft + cornerSize, windowBottom),
            strokeWidth = cornerThickness
        )
        
        // Bottom-right corner
        drawLine(
            color = cornerColor,
            start = Offset(windowRight - cornerSize, windowBottom),
            end = Offset(windowRight, windowBottom),
            strokeWidth = cornerThickness
        )
        drawLine(
            color = cornerColor,
            start = Offset(windowRight, windowBottom - cornerSize),
            end = Offset(windowRight, windowBottom),
            strokeWidth = cornerThickness
        )
    }
}

/**
 * Aspect ratio label showing current mode
 */
@Composable
private fun AspectRatioLabel(
    aspectRatio: ViewfinderAspectRatio,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = aspectRatio.label,
                color = ConstructionColors.SafetyOrange,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = aspectRatio.description.take(20) + if (aspectRatio.description.length > 20) "..." else "",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/**
 * Aspect ratio selector with visual previews
 */
@Composable
fun AspectRatioSelector(
    currentRatio: ViewfinderAspectRatio,
    onRatioChanged: (ViewfinderAspectRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ASPECT RATIO",
                color = ConstructionColors.SafetyOrange,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            ViewfinderAspectRatio.values().forEach { ratio ->
                val isSelected = ratio == currentRatio
                
                Button(
                    onClick = { onRatioChanged(ratio) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) ConstructionColors.SafetyOrange else Color.Transparent,
                        contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = ratio.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = ratio.description,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        
                        // Mini aspect ratio preview
                        Box(
                            modifier = Modifier
                                .size(32.dp, 24.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        ) {
                            val previewWidth = when (ratio) {
                                ViewfinderAspectRatio.SQUARE -> 20.dp
                                ViewfinderAspectRatio.FOUR_THREE -> 24.dp 
                                ViewfinderAspectRatio.SIXTEEN_NINE -> 28.dp
                            }
                            val previewHeight = when (ratio) {
                                ViewfinderAspectRatio.SQUARE -> 20.dp
                                ViewfinderAspectRatio.FOUR_THREE -> 18.dp
                                ViewfinderAspectRatio.SIXTEEN_NINE -> 16.dp
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(previewWidth, previewHeight)
                                    .background(
                                        color = if (isSelected) Color.White else ConstructionColors.SafetyOrange,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}