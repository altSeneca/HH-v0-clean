package com.hazardhawk.camera

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ui.theme.ConstructionColors
import kotlin.math.min

/**
 * Grid types for different composition needs in construction photography
 */
enum class GridType {
    RULE_OF_THIRDS,     // Standard 3x3 grid
    GOLDEN_RATIO,       // Golden ratio grid (more aesthetic)
    CENTER_LINES,       // Simple center cross
    DIAGONAL,           // Diagonal guide lines
    SAFETY_ZONES        // Construction-specific safety zone grid
}

/**
 * Professional grid overlay for composition assistance
 * Designed for construction documentation with multiple grid types
 */
@Composable
fun GridOverlay(
    isVisible: Boolean,
    gridType: GridType = GridType.RULE_OF_THIRDS,
    opacity: Float = 0.5f,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main grid canvas
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawGrid(
                    gridType = gridType,
                    opacity = opacity
                )
            }
            
            // Grid type indicator
            GridTypeIndicator(
                gridType = gridType,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Grid overlay with type selector for professional use
 */
@Composable
fun AdvancedGridOverlay(
    isVisible: Boolean,
    gridType: GridType,
    onGridTypeChange: (GridType) -> Unit,
    opacity: Float = 0.5f,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main grid canvas
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawGrid(
                    gridType = gridType,
                    opacity = opacity
                )
            }
            
            // Grid selector panel
            GridSelectorPanel(
                currentGridType = gridType,
                onGridTypeChange = onGridTypeChange,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Draw different types of composition grids
 */
private fun DrawScope.drawGrid(
    gridType: GridType,
    opacity: Float
) {
    val strokeWidth = 2.dp.toPx()
    val color = Color.White.copy(alpha = opacity)
    val strokeStyle = Stroke(
        width = strokeWidth,
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
    )
    
    when (gridType) {
        GridType.RULE_OF_THIRDS -> drawRuleOfThirdsGrid(color, strokeStyle)
        GridType.GOLDEN_RATIO -> drawGoldenRatioGrid(color, strokeStyle)
        GridType.CENTER_LINES -> drawCenterLinesGrid(color, strokeStyle)
        GridType.DIAGONAL -> drawDiagonalGrid(color, strokeStyle)
        GridType.SAFETY_ZONES -> drawSafetyZonesGrid(color, strokeStyle)
    }
}

/**
 * Classic rule of thirds grid (3x3)
 */
private fun DrawScope.drawRuleOfThirdsGrid(
    color: Color,
    strokeStyle: Stroke
) {
    val width = size.width
    val height = size.height
    
    // Vertical lines
    val verticalLine1 = width / 3f
    val verticalLine2 = width * 2f / 3f
    
    drawLine(
        color = color,
        start = Offset(verticalLine1, 0f),
        end = Offset(verticalLine1, height),
        strokeWidth = strokeStyle.width,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
    
    drawLine(
        color = color,
        start = Offset(verticalLine2, 0f),
        end = Offset(verticalLine2, height),
        strokeWidth = strokeStyle.width,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
    
    // Horizontal lines
    val horizontalLine1 = height / 3f
    val horizontalLine2 = height * 2f / 3f
    
    drawLine(
        color = color,
        start = Offset(0f, horizontalLine1),
        end = Offset(width, horizontalLine1),
        strokeWidth = strokeStyle.width,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
    
    drawLine(
        color = color,
        start = Offset(0f, horizontalLine2),
        end = Offset(width, horizontalLine2),
        strokeWidth = strokeStyle.width,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
    
    // Draw intersection points (sweet spots)
    val pointRadius = 8.dp.toPx()
    val intersections = listOf(
        Offset(verticalLine1, horizontalLine1),
        Offset(verticalLine2, horizontalLine1),
        Offset(verticalLine1, horizontalLine2),
        Offset(verticalLine2, horizontalLine2)
    )
    
    intersections.forEach { point ->
        drawCircle(
            color = ConstructionColors.SafetyOrange.copy(alpha = 0.8f),
            radius = pointRadius,
            center = point,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

/**
 * Golden ratio grid for more aesthetic composition
 */
private fun DrawScope.drawGoldenRatioGrid(
    color: Color,
    strokeStyle: Stroke
) {
    val width = size.width
    val height = size.height
    val goldenRatio = 1.618f
    
    // Vertical golden ratio lines
    val verticalLine1 = width / goldenRatio
    val verticalLine2 = width - (width / goldenRatio)
    
    drawLine(
        color = color,
        start = Offset(verticalLine1, 0f),
        end = Offset(verticalLine1, height),
        strokeWidth = strokeStyle.width,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
    
    drawLine(
        color = color,
        start = Offset(verticalLine2, 0f),
        end = Offset(verticalLine2, height),
        strokeWidth = strokeStyle.width,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
    
    // Horizontal golden ratio lines
    val horizontalLine1 = height / goldenRatio
    val horizontalLine2 = height - (height / goldenRatio)
    
    drawLine(
        color = color,
        start = Offset(0f, horizontalLine1),
        end = Offset(width, horizontalLine1),
        strokeWidth = strokeStyle.width,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
    
    drawLine(
        color = color,
        start = Offset(0f, horizontalLine2),
        end = Offset(width, horizontalLine2),
        strokeWidth = strokeStyle.width,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
}

/**
 * Simple center cross lines
 */
private fun DrawScope.drawCenterLinesGrid(
    color: Color,
    strokeStyle: Stroke
) {
    val width = size.width
    val height = size.height
    val centerX = width / 2f
    val centerY = height / 2f
    
    // Vertical center line
    drawLine(
        color = color,
        start = Offset(centerX, 0f),
        end = Offset(centerX, height),
        strokeWidth = strokeStyle.width,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
    
    // Horizontal center line
    drawLine(
        color = color,
        start = Offset(0f, centerY),
        end = Offset(width, centerY),
        strokeWidth = strokeStyle.width,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
    
    // Center point
    drawCircle(
        color = ConstructionColors.SafetyOrange.copy(alpha = 0.8f),
        radius = 6.dp.toPx(),
        center = Offset(centerX, centerY),
        style = Stroke(width = 3.dp.toPx())
    )
}

/**
 * Diagonal composition guides
 */
private fun DrawScope.drawDiagonalGrid(
    color: Color,
    strokeStyle: Stroke
) {
    val width = size.width
    val height = size.height
    
    // Main diagonals
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(width, height),
        strokeWidth = strokeStyle.width,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
    
    drawLine(
        color = color,
        start = Offset(width, 0f),
        end = Offset(0f, height),
        strokeWidth = strokeStyle.width,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
    
    // Additional diagonal guides
    val quarterWidth = width / 4f
    val quarterHeight = height / 4f
    
    // Offset diagonals for dynamic composition
    drawLine(
        color = color.copy(alpha = color.alpha * 0.6f),
        start = Offset(quarterWidth, 0f),
        end = Offset(width, height - quarterHeight),
        strokeWidth = strokeStyle.width * 0.7f,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
    
    drawLine(
        color = color.copy(alpha = color.alpha * 0.6f),
        start = Offset(width - quarterWidth, 0f),
        end = Offset(0f, height - quarterHeight),
        strokeWidth = strokeStyle.width * 0.7f,
        cap = strokeStyle.cap,
        pathEffect = strokeStyle.pathEffect
    )
}

/**
 * Construction safety zones grid for documenting work areas
 */
private fun DrawScope.drawSafetyZonesGrid(
    color: Color,
    strokeStyle: Stroke
) {
    val width = size.width
    val height = size.height
    
    // Safety zone boundaries (typically 6-foot boundaries in construction)
    val zoneWidth = width / 4f
    val zoneHeight = height / 4f
    
    val safetyColor = ConstructionColors.HighVisYellow.copy(alpha = 0.6f)
    val dangerColor = ConstructionColors.CautionRed.copy(alpha = 0.6f)
    
    // Draw grid lines
    for (i in 1..3) {
        // Vertical lines
        drawLine(
            color = color,
            start = Offset(zoneWidth * i, 0f),
            end = Offset(zoneWidth * i, height),
            strokeWidth = strokeStyle.width,
            cap = strokeStyle.cap
        )
        
        // Horizontal lines
        drawLine(
            color = color,
            start = Offset(0f, zoneHeight * i),
            end = Offset(width, zoneHeight * i),
            strokeWidth = strokeStyle.width,
            cap = strokeStyle.cap
        )
    }
    
    // Mark corner zones as caution areas
    val cornerSize = min(zoneWidth, zoneHeight) * 0.3f
    val corners = listOf(
        Offset(cornerSize, cornerSize),
        Offset(width - cornerSize, cornerSize),
        Offset(cornerSize, height - cornerSize),
        Offset(width - cornerSize, height - cornerSize)
    )
    
    corners.forEach { corner ->
        drawCircle(
            color = dangerColor,
            radius = cornerSize,
            center = corner,
            style = Stroke(width = 4.dp.toPx())
        )
    }
    
    // Mark center as safe zone
    drawCircle(
        color = safetyColor,
        radius = min(zoneWidth, zoneHeight) * 0.4f,
        center = Offset(width / 2f, height / 2f),
        style = Stroke(width = 4.dp.toPx())
    )
}

/**
 * Grid type indicator showing current grid name
 */
@Composable
private fun GridTypeIndicator(
    gridType: GridType,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = when (gridType) {
                GridType.RULE_OF_THIRDS -> "Rule of Thirds"
                GridType.GOLDEN_RATIO -> "Golden Ratio"
                GridType.CENTER_LINES -> "Center Lines"
                GridType.DIAGONAL -> "Diagonal"
                GridType.SAFETY_ZONES -> "Safety Zones"
            },
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Grid selector panel for switching between grid types
 */
@Composable
private fun GridSelectorPanel(
    currentGridType: GridType,
    onGridTypeChange: (GridType) -> Unit,
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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "GRID TYPE",
                color = ConstructionColors.SafetyOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            
            GridType.values().forEach { gridType ->
                val isSelected = gridType == currentGridType
                
                Button(
                    onClick = { onGridTypeChange(gridType) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) ConstructionColors.SafetyOrange else Color.Transparent,
                        contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (gridType) {
                            GridType.RULE_OF_THIRDS -> "Rule of Thirds"
                            GridType.GOLDEN_RATIO -> "Golden Ratio"
                            GridType.CENTER_LINES -> "Center Lines"
                            GridType.DIAGONAL -> "Diagonal"
                            GridType.SAFETY_ZONES -> "Safety Zones"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Construction-specific grid overlay with safety zone highlighting
 */
@Composable
fun ConstructionGridOverlay(
    isVisible: Boolean,
    showSafetyZones: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Base rule of thirds grid
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRuleOfThirdsGrid(
                    color = Color.White.copy(alpha = 0.4f),
                    strokeStyle = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                    )
                )
                
                if (showSafetyZones) {
                    drawSafetyZonesGrid(
                        color = ConstructionColors.HighVisYellow.copy(alpha = 0.6f),
                        strokeStyle = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
            
            // Safety zone legend
            if (showSafetyZones) {
                SafetyZoneLegend(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * Legend explaining safety zone colors
 */
@Composable
private fun SafetyZoneLegend(
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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "SAFETY ZONES",
                color = ConstructionColors.SafetyOrange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(ConstructionColors.HighVisYellow)
                )
                Text(
                    text = "Safe Zone",
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(ConstructionColors.CautionRed)
                )
                Text(
                    text = "Caution Area",
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }
    }
}