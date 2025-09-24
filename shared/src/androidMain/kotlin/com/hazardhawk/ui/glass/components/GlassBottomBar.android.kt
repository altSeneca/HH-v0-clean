package com.hazardhawk.ui.glass.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeChild
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.hazardhawk.ui.glass.GlassConfiguration
import com.hazardhawk.ui.glass.GlassState

/**
 * Android implementation of GlassBottomBar with Haze library integration.
 * Optimized for camera overlay use and construction environments.
 */
@Composable
actual fun GlassBottomBar(
    modifier: Modifier,
    config: GlassConfiguration,
    containerColor: Color,
    contentColor: Color,
    cameraOverlayMode: Boolean,
    emergencyMode: Boolean,
    elevation: Dp?,
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    val glassState = remember { GlassState.getInstance(context) }
    val hazeState = remember { HazeState() }
    
    // Determine effective configuration based on mode and glass state
    val effectiveConfig = remember(config, glassState.supportLevel, emergencyMode, cameraOverlayMode) {
        when {
            emergencyMode -> GlassBottomBarConfig.emergency
            cameraOverlayMode -> GlassBottomBarConfig.cameraOverlay
            glassState.supportLevel == com.hazardhawk.ui.glass.GlassSupportLevel.DISABLED ->
                GlassBottomBarConfig.construction.copy(
                    blurRadius = 0f,
                    emergencyHighContrast = true
                )
            glassState.supportLevel == com.hazardhawk.ui.glass.GlassSupportLevel.REDUCED ->
                GlassBottomBarConfig.construction.copy(
                    adaptiveOpacity = false
                )
            else -> GlassBottomBarConfig.construction
        }
    }
    
    // Calculate adaptive opacity based on mode
    val adaptiveOpacity = remember(effectiveConfig.adaptiveOpacity, cameraOverlayMode) {
        if (effectiveConfig.adaptiveOpacity && cameraOverlayMode) {
            effectiveConfig.opacity * 0.7f // Reduce opacity over camera
        } else {
            effectiveConfig.opacity
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(effectiveConfig.height)
            .semantics {
                contentDescription = if (emergencyMode) "Emergency navigation bar" 
                else if (cameraOverlayMode) "Camera overlay navigation bar"
                else "Navigation bar"
            }
    ) {
        // Background haze effect
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
        
        // Bottom bar container
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (glassState.isEnabled && !emergencyMode) {
                containerColor.copy(alpha = adaptiveOpacity)
            } else {
                // Fallback to solid background
                if (emergencyMode) Color.Red.copy(alpha = 0.9f)
                else Color.Black.copy(alpha = 0.85f)
            },
            contentColor = if (emergencyMode) Color.White else contentColor,
            shadowElevation = elevation ?: 8.dp
        ) {
            // Glass effect child for backdrop blur
            if (glassState.isEnabled && effectiveConfig.blurRadius > 0f) {
                HazeChild(
                    state = hazeState,
                    shape = RoundedCornerShape(
                        topStart = effectiveConfig.cornerRadius.dp,
                        topEnd = effectiveConfig.cornerRadius.dp
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    BottomBarContent(
                        config = effectiveConfig,
                        emergencyMode = emergencyMode,
                        cameraOverlayMode = cameraOverlayMode,
                        content = content
                    )
                }
            } else {
                // Fallback without glass effect
                BottomBarContent(
                    config = effectiveConfig,
                    emergencyMode = emergencyMode,
                    cameraOverlayMode = cameraOverlayMode,
                    content = content
                )
            }
        }
        
        // Safety accent line (top border)
        if (effectiveConfig.safetyAccentEnabled && !emergencyMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(effectiveConfig.borderTopWidth.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        if (cameraOverlayMode) {
                            // Gradient for camera overlay
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    effectiveConfig.borderTopColor,
                                    Color.Transparent
                                )
                            )
                        } else {
                            Brush.linearGradient(listOf(effectiveConfig.borderTopColor))
                        }
                    )
            )
        } else if (emergencyMode) {
            // Emergency red border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(effectiveConfig.borderTopWidth.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.Red)
            )
        }
    }
    
    // Record frame for performance monitoring
    LaunchedEffect(Unit) {
        glassState.recordFrame()
    }
}

/**
 * Bottom bar content wrapper with construction optimizations
 */
@Composable
private fun BottomBarContent(
    config: GlassBottomBarConfig,
    emergencyMode: Boolean,
    cameraOverlayMode: Boolean,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = if (cameraOverlayMode) 12.dp else 16.dp,
                vertical = if (emergencyMode) 16.dp else 12.dp
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

/**
 * Construction-optimized glass bottom bar with preset configurations
 */
@Composable
fun ConstructionGlassBottomBar(
    modifier: Modifier = Modifier,
    barType: ConstructionBarType = ConstructionBarType.STANDARD,
    cameraOverlayMode: Boolean = false,
    emergencyMode: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val config = when (barType) {
        ConstructionBarType.STANDARD -> GlassConfiguration.construction
        ConstructionBarType.CAMERA_OVERLAY -> GlassConfiguration.construction.copy(
            opacity = 0.6f,
            blurRadius = 25.0f
        )
        ConstructionBarType.EMERGENCY -> GlassConfiguration.emergency
        ConstructionBarType.INDOOR -> GlassConfiguration.construction.copy(
            opacity = 0.75f,
            blurRadius = 18.0f
        )
    }
    
    val containerColor = when (barType) {
        ConstructionBarType.STANDARD -> Color.Black.copy(alpha = 0.6f)
        ConstructionBarType.CAMERA_OVERLAY -> Color.Black.copy(alpha = 0.4f)
        ConstructionBarType.EMERGENCY -> Color.Red.copy(alpha = 0.3f)
        ConstructionBarType.INDOOR -> Color.Black.copy(alpha = 0.5f)
    }
    
    val contentColor = Color.White
    
    GlassBottomBar(
        modifier = modifier,
        config = config,
        containerColor = containerColor,
        contentColor = contentColor,
        cameraOverlayMode = cameraOverlayMode || barType == ConstructionBarType.CAMERA_OVERLAY,
        emergencyMode = emergencyMode || barType == ConstructionBarType.EMERGENCY,
        content = content
    )
}

/**
 * Bottom bar types for construction environments
 */
enum class ConstructionBarType {
    STANDARD,       // General navigation with safety accents
    CAMERA_OVERLAY, // Optimized for camera viewfinder overlay
    EMERGENCY,      // Emergency/critical mode
    INDOOR         // Professional/indoor environments
}

/**
 * Camera-specific glass bottom bar with specialized controls
 */
@Composable
fun CameraGlassBottomBar(
    modifier: Modifier = Modifier,
    cameraMode: CameraMode = CameraMode.PHOTO,
    emergencyMode: Boolean = false,
    adaptiveOpacity: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val cameraConfig = remember(cameraMode) {
        when (cameraMode) {
            CameraMode.PHOTO -> GlassBottomBarConfig.cameraOverlay
            CameraMode.VIDEO -> GlassBottomBarConfig.cameraOverlay.copy(
                height = 88.dp, // Taller for video controls
                blurRadius = 20.0f
            )
            CameraMode.ANALYSIS -> GlassBottomBarConfig.construction.copy(
                opacity = 0.8f, // Higher opacity for analysis info
                safetyAccentEnabled = true
            )
        }
    }
    
    val containerColor = when (cameraMode) {
        CameraMode.PHOTO -> Color.Black.copy(alpha = 0.4f)
        CameraMode.VIDEO -> Color.Red.copy(alpha = 0.2f) // Subtle red tint for recording
        CameraMode.ANALYSIS -> Color.Black.copy(alpha = 0.6f)
    }
    
    GlassBottomBar(
        modifier = modifier,
        config = GlassConfiguration(
            blurRadius = cameraConfig.blurRadius,
            opacity = cameraConfig.opacity,
            animationsEnabled = false, // Disabled for camera performance
            enabledInEmergencyMode = false,
            supportLevel = com.hazardhawk.ui.glass.GlassSupportLevel.REDUCED
        ),
        containerColor = containerColor,
        contentColor = Color.White,
        cameraOverlayMode = true,
        emergencyMode = emergencyMode,
        elevation = 4.dp,
        content = content
    )
}

/**
 * Camera modes for specialized bottom bar styling
 */
enum class CameraMode {
    PHOTO,    // Photo capture mode
    VIDEO,    // Video recording mode
    ANALYSIS  // AI analysis mode
}

/**
 * Safety-focused glass bottom bar with accessibility features
 */
@Composable
fun SafetyGlassBottomBar(
    modifier: Modifier = Modifier,
    safetyLevel: SafetyBarLevel = SafetyBarLevel.STANDARD,
    cameraOverlayMode: Boolean = false,
    emergencyMode: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val safetyConfig = remember(safetyLevel) {
        when (safetyLevel) {
            SafetyBarLevel.STANDARD -> GlassBottomBarConfig.construction
            SafetyBarLevel.WARNING -> GlassBottomBarConfig.construction.copy(
                borderTopColor = Color(0xFFFFB020), // Warning orange
                borderTopWidth = 3.0f,
                height = 84.dp
            )
            SafetyBarLevel.CRITICAL -> GlassBottomBarConfig.emergency.copy(
                height = 92.dp,
                borderTopWidth = 4.0f
            )
        }
    }
    
    val barColor = when (safetyLevel) {
        SafetyBarLevel.STANDARD -> Color.Black.copy(alpha = 0.7f)
        SafetyBarLevel.WARNING -> Color(0xFFFFB020).copy(alpha = 0.3f)
        SafetyBarLevel.CRITICAL -> Color.Red.copy(alpha = 0.4f)
    }
    
    GlassBottomBar(
        modifier = modifier,
        config = GlassConfiguration(
            blurRadius = safetyConfig.blurRadius,
            opacity = safetyConfig.opacity,
            animationsEnabled = false,
            enabledInEmergencyMode = safetyLevel == SafetyBarLevel.CRITICAL,
            supportLevel = when (safetyLevel) {
                SafetyBarLevel.CRITICAL -> com.hazardhawk.ui.glass.GlassSupportLevel.DISABLED
                else -> com.hazardhawk.ui.glass.GlassSupportLevel.REDUCED
            }
        ),
        containerColor = barColor,
        contentColor = Color.White,
        cameraOverlayMode = cameraOverlayMode,
        emergencyMode = emergencyMode || safetyLevel == SafetyBarLevel.CRITICAL,
        elevation = when (safetyLevel) {
            SafetyBarLevel.CRITICAL -> 12.dp
            SafetyBarLevel.WARNING -> 8.dp
            SafetyBarLevel.STANDARD -> 4.dp
        },
        content = content
    )
}

/**
 * Safety levels for construction bottom bar styling
 */
enum class SafetyBarLevel {
    STANDARD,  // Normal operation
    WARNING,   // Caution required  
    CRITICAL   // High risk/emergency situation
}