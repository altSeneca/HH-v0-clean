package com.hazardhawk.ui.glass.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeChild
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.hazardhawk.ui.glass.GlassConfiguration
import com.hazardhawk.ui.glass.GlassState

/**
 * Android implementation of GlassCard using Haze library for backdrop blur.
 * Optimized for construction environments with safety-focused styling.
 */
@Composable
actual fun GlassCard(
    modifier: Modifier,
    config: GlassConfiguration,
    containerColor: Color,
    contentColor: Color,
    shape: Shape?,
    elevation: Dp?,
    shadowColor: Color,
    emergencyMode: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val glassState = remember { GlassState.getInstance(context) }
    val hazeState = remember { HazeState() }
    
    // Determine effective configuration based on glass state
    val effectiveConfig = remember(config, glassState.supportLevel, emergencyMode) {
        when {
            emergencyMode -> GlassCardConfig.emergency
            glassState.supportLevel == com.hazardhawk.ui.glass.GlassSupportLevel.DISABLED -> 
                GlassCardConfig.construction.copy(
                    blurRadius = 0f,
                    emergencyHighContrast = true
                )
            glassState.supportLevel == com.hazardhawk.ui.glass.GlassSupportLevel.REDUCED ->
                GlassCardConfig.construction.copy(
                    animationEnabled = false
                )
            else -> GlassCardConfig.construction
        }
    }
    
    val cardShape = shape ?: RoundedCornerShape(effectiveConfig.cornerRadius.dp)
    val cardElevation = elevation ?: 8.dp
    
    Box(modifier = modifier) {
        // Background haze effect (only if glass is enabled)
        if (glassState.isEnabled && effectiveConfig.blurRadius > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(
                        hazeState,
                        backgroundColor = containerColor,
                        blurRadius = effectiveConfig.blurRadius.dp
                    )
            )
        }
        
        // Card container
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = if (glassState.isEnabled && !emergencyMode) {
                    containerColor.copy(alpha = effectiveConfig.opacity)
                } else {
                    // Fallback to solid background
                    if (emergencyMode) Color.White.copy(alpha = 0.95f)
                    else Color.Black.copy(alpha = 0.8f)
                },
                contentColor = if (emergencyMode) Color.Black else contentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
            border = BorderStroke(
                width = effectiveConfig.borderWidth.dp,
                color = if (emergencyMode) {
                    Color.Red
                } else {
                    effectiveConfig.borderColor
                }
            )
        ) {
            // Glass effect child (backdrop blur)
            if (glassState.isEnabled && effectiveConfig.blurRadius > 0f) {
                HazeChild(
                    state = hazeState,
                    shape = cardShape,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CardContent(
                        modifier = Modifier.padding(16.dp),
                        emergencyMode = emergencyMode,
                        content = content
                    )
                }
            } else {
                // Fallback without glass effect
                CardContent(
                    modifier = Modifier.padding(16.dp),
                    emergencyMode = emergencyMode,
                    content = content
                )
            }
        }
    }
    
    // Record frame for performance monitoring
    LaunchedEffect(Unit) {
        glassState.recordFrame()
    }
}

/**
 * Card content wrapper with construction environment optimizations
 */
@Composable
private fun CardContent(
    modifier: Modifier,
    emergencyMode: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.then(
            if (emergencyMode) {
                // Emergency mode: high contrast background
                Modifier.background(
                    Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(8.dp)
                )
            } else Modifier
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

/**
 * Construction-optimized glass card with preset configurations
 */
@Composable
fun ConstructionGlassCard(
    modifier: Modifier = Modifier,
    cardType: ConstructionCardType = ConstructionCardType.STANDARD,
    emergencyMode: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val config = when (cardType) {
        ConstructionCardType.STANDARD -> GlassConfiguration.construction
        ConstructionCardType.CRITICAL -> GlassConfiguration.emergency
        ConstructionCardType.OUTDOOR -> GlassConfiguration.outdoor
    }
    
    val containerColor = when (cardType) {
        ConstructionCardType.STANDARD -> Color.Black.copy(alpha = 0.6f)
        ConstructionCardType.CRITICAL -> Color.Red.copy(alpha = 0.3f)
        ConstructionCardType.OUTDOOR -> Color.White.copy(alpha = 0.2f)
    }
    
    val contentColor = when (cardType) {
        ConstructionCardType.STANDARD -> Color.White
        ConstructionCardType.CRITICAL -> Color.White
        ConstructionCardType.OUTDOOR -> Color.Black
    }
    
    GlassCard(
        modifier = modifier,
        config = config,
        containerColor = containerColor,
        contentColor = contentColor,
        emergencyMode = emergencyMode,
        content = content
    )
}

/**
 * Card types optimized for construction environments
 */
enum class ConstructionCardType {
    STANDARD,  // General purpose with safety orange accents
    CRITICAL,  // Emergency/warning information
    OUTDOOR    // High visibility for bright environments
}

/**
 * Safety-focused glass card with OSHA compliance features
 */
@Composable
fun SafetyGlassCard(
    modifier: Modifier = Modifier,
    safetyLevel: SafetyLevel = SafetyLevel.STANDARD,
    emergencyMode: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val safetyConfig = remember(safetyLevel) {
        when (safetyLevel) {
            SafetyLevel.STANDARD -> GlassCardConfig.construction
            SafetyLevel.WARNING -> GlassCardConfig.construction.copy(
                borderColor = Color(0xFFFFB020).copy(alpha = 0.8f), // Warning orange
                borderWidth = 3.0f
            )
            SafetyLevel.CRITICAL -> GlassCardConfig.emergency
            SafetyLevel.EMERGENCY -> GlassCardConfig.emergency.copy(
                blurRadius = 5.0f,
                opacity = 0.98f,
                borderWidth = 4.0f
            )
        }
    }
    
    val cardColor = when (safetyLevel) {
        SafetyLevel.STANDARD -> Color.Black.copy(alpha = 0.7f)
        SafetyLevel.WARNING -> Color(0xFFFFB020).copy(alpha = 0.2f) // Warning overlay
        SafetyLevel.CRITICAL -> Color.Red.copy(alpha = 0.3f)
        SafetyLevel.EMERGENCY -> Color.Red.copy(alpha = 0.1f)
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(safetyConfig.cornerRadius.dp))
            .background(cardColor)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxSize(),
            config = GlassConfiguration(
                blurRadius = safetyConfig.blurRadius,
                opacity = safetyConfig.opacity,
                animationsEnabled = safetyConfig.animationEnabled,
                enabledInEmergencyMode = safetyLevel == SafetyLevel.EMERGENCY,
                supportLevel = if (safetyLevel == SafetyLevel.EMERGENCY) {
                    com.hazardhawk.ui.glass.GlassSupportLevel.DISABLED
                } else {
                    com.hazardhawk.ui.glass.GlassSupportLevel.REDUCED
                }
            ),
            containerColor = Color.Transparent,
            emergencyMode = emergencyMode || safetyLevel == SafetyLevel.EMERGENCY,
            content = content
        )
    }
}

/**
 * Safety levels for construction environments
 */
enum class SafetyLevel {
    STANDARD,  // Normal operation
    WARNING,   // Caution required
    CRITICAL,  // High risk situation
    EMERGENCY  // Immediate action required
}