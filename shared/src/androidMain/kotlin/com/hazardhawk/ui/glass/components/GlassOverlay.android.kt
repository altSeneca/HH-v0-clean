package com.hazardhawk.ui.glass.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeChild
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.hazardhawk.ui.glass.GlassConfiguration
import com.hazardhawk.ui.glass.GlassState
import kotlinx.coroutines.delay

/**
 * Android implementation of GlassOverlay with Haze library integration.
 * Provides adaptive blur based on lighting conditions and device performance.
 */
@Composable
actual fun GlassOverlay(
    modifier: Modifier,
    config: GlassConfiguration,
    containerColor: Color,
    contentColor: Color,
    shape: Shape?,
    maskingEnabled: Boolean,
    adaptiveBlur: Boolean,
    emergencyMode: Boolean,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val glassState = remember { GlassState.getInstance(context) }
    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    
    // Determine effective configuration
    val effectiveConfig = remember(config, glassState.supportLevel, emergencyMode) {
        when {
            emergencyMode -> GlassOverlayConfig.emergency
            glassState.supportLevel == com.hazardhawk.ui.glass.GlassSupportLevel.DISABLED ->
                GlassOverlayConfig.modal.copy(
                    blurRadius = 0f,
                    emergencyHighContrast = true
                )
            glassState.supportLevel == com.hazardhawk.ui.glass.GlassSupportLevel.REDUCED ->
                GlassOverlayConfig.modal.copy(
                    adaptiveBlurEnabled = false,
                    transitionAnimationEnabled = false
                )
            else -> GlassOverlayConfig.modal
        }
    }
    
    // Adaptive blur calculation
    var currentBlurRadius by remember { mutableStateOf(effectiveConfig.blurRadius) }
    val animatedBlurRadius by animateFloatAsState(
        targetValue = currentBlurRadius,
        animationSpec = tween(durationMillis = 300),
        label = "BlurRadiusAnimation"
    )
    
    // Simulate adaptive blur based on environment (placeholder implementation)
    LaunchedEffect(adaptiveBlur, effectiveConfig.adaptiveBlurEnabled) {
        if (adaptiveBlur && effectiveConfig.adaptiveBlurEnabled && !emergencyMode) {
            while (true) {
                // Simulate environment detection
                val lightLevel = kotlin.random.Random.nextFloat()
                val performanceScore = glassState.performanceMetrics?.frameRate ?: 60.0
                
                currentBlurRadius = when {
                    lightLevel > 0.8f -> effectiveConfig.blurRadius * 0.7f // Bright - reduce blur
                    lightLevel < 0.2f -> effectiveConfig.blurRadius * 1.3f // Dark - increase blur
                    performanceScore < 30.0 -> effectiveConfig.blurRadius * 0.5f // Poor performance - reduce
                    else -> effectiveConfig.blurRadius
                }.coerceIn(5.0f, 30.0f)
                
                delay(2000) // Update every 2 seconds
            }
        }
    }
    
    val overlayShape = shape ?: RoundedCornerShape(effectiveConfig.cornerRadius.dp)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = if (emergencyMode) "Emergency overlay"
                else if (maskingEnabled) "Content overlay with blur"
                else "Content overlay"
            }
    ) {
        // Background haze effect
        if (glassState.isEnabled && animatedBlurRadius > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(
                        hazeState,
                        backgroundColor = containerColor,
                        blurRadius = with(density) { animatedBlurRadius.dp }
                    )
            )
        }
        
        // Overlay container
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = overlayShape,
            color = if (glassState.isEnabled && !emergencyMode) {
                containerColor.copy(alpha = effectiveConfig.opacity)
            } else {
                // Fallback to solid background
                if (emergencyMode) Color.White.copy(alpha = 0.95f)
                else Color.Black.copy(alpha = 0.7f)
            },
            contentColor = if (emergencyMode) Color.Black else contentColor,
            shadowElevation = 12.dp,
            border = BorderStroke(
                width = effectiveConfig.borderWidth.dp,
                color = if (emergencyMode) Color.Red else effectiveConfig.borderColor
            )
        ) {
            // Glass effect child for backdrop blur
            if (glassState.isEnabled && animatedBlurRadius > 0f) {
                HazeChild(
                    state = hazeState,
                    shape = overlayShape,
                    modifier = Modifier.fillMaxSize()
                ) {
                    OverlayContent(
                        config = effectiveConfig,
                        emergencyMode = emergencyMode,
                        maskingEnabled = maskingEnabled,
                        content = content
                    )
                }
            } else {
                // Fallback without glass effect
                OverlayContent(
                    config = effectiveConfig,
                    emergencyMode = emergencyMode,
                    maskingEnabled = maskingEnabled,
                    content = content
                )
            }
        }
        
        // Optional masking overlay for viewfinder-style effects
        if (maskingEnabled && !emergencyMode && !effectiveConfig.emergencyHighContrast) {
            ViewfinderMask(
                config = effectiveConfig,
                shape = overlayShape
            )
        }
    }
    
    // Record frame for performance monitoring
    LaunchedEffect(Unit) {
        glassState.recordFrame()
    }
}

/**
 * Overlay content wrapper with construction optimizations
 */
@Composable
private fun OverlayContent(
    config: GlassOverlayConfig,
    emergencyMode: Boolean,
    maskingEnabled: Boolean,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                if (maskingEnabled) 24.dp else 16.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        // Emergency mode high contrast background
        if (emergencyMode && config.emergencyHighContrast) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        3.dp,
                        Color.Red,
                        RoundedCornerShape(12.dp)
                    )
            )
        }
        
        content()
    }
}

/**
 * Viewfinder masking overlay for camera-style effects
 */
@Composable
private fun ViewfinderMask(
    config: GlassOverlayConfig,
    shape: Shape
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Corner indicators for viewfinder
        val cornerSize = 24.dp
        val cornerThickness = 3.dp
        val cornerColor = Color(0xFFFF6B35).copy(alpha = 0.8f) // Safety orange
        
        // Top-left corner
        Box(
            modifier = Modifier
                .size(cornerSize)
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerThickness)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .width(cornerThickness)
                    .fillMaxHeight()
                    .background(cornerColor)
            )
        }
        
        // Top-right corner
        Box(
            modifier = Modifier
                .size(cornerSize)
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerThickness)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .width(cornerThickness)
                    .fillMaxHeight()
                    .align(Alignment.TopEnd)
                    .background(cornerColor)
            )
        }
        
        // Bottom-left corner
        Box(
            modifier = Modifier
                .size(cornerSize)
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerThickness)
                    .align(Alignment.BottomStart)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .width(cornerThickness)
                    .fillMaxHeight()
                    .background(cornerColor)
            )
        }
        
        // Bottom-right corner
        Box(
            modifier = Modifier
                .size(cornerSize)
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerThickness)
                    .align(Alignment.BottomEnd)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .width(cornerThickness)
                    .fillMaxHeight()
                    .align(Alignment.BottomEnd)
                    .background(cornerColor)
            )
        }
        
        // Center crosshair (optional, for precise aiming)
        Box(
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.Center)
                    .background(cornerColor.copy(alpha = 0.6f))
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .background(cornerColor.copy(alpha = 0.6f))
            )
        }
    }
}

/**
 * Construction-optimized glass overlay with preset configurations
 */
@Composable
fun ConstructionGlassOverlay(
    modifier: Modifier = Modifier,
    overlayType: ConstructionOverlayType = ConstructionOverlayType.MODAL,
    adaptiveBlur: Boolean = true,
    emergencyMode: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val config = when (overlayType) {
        ConstructionOverlayType.MODAL -> GlassConfiguration.construction
        ConstructionOverlayType.CAMERA_VIEWFINDER -> GlassConfiguration.construction.copy(
            blurRadius = 12.0f,
            opacity = 0.3f
        )
        ConstructionOverlayType.LOADING -> GlassConfiguration.construction.copy(
            blurRadius = 25.0f,
            opacity = 0.7f
        )
        ConstructionOverlayType.EMERGENCY -> GlassConfiguration.emergency
    }
    
    val containerColor = when (overlayType) {
        ConstructionOverlayType.MODAL -> Color.Black.copy(alpha = 0.4f)
        ConstructionOverlayType.CAMERA_VIEWFINDER -> Color.Black.copy(alpha = 0.2f)
        ConstructionOverlayType.LOADING -> Color.Black.copy(alpha = 0.6f)
        ConstructionOverlayType.EMERGENCY -> Color.Red.copy(alpha = 0.2f)
    }
    
    GlassOverlay(
        modifier = modifier,
        config = config,
        containerColor = containerColor,
        contentColor = Color.White,
        maskingEnabled = overlayType == ConstructionOverlayType.CAMERA_VIEWFINDER,
        adaptiveBlur = adaptiveBlur,
        emergencyMode = emergencyMode || overlayType == ConstructionOverlayType.EMERGENCY,
        content = content
    )
}

/**
 * Overlay types for construction environments
 */
enum class ConstructionOverlayType {
    MODAL,             // General modal dialogs
    CAMERA_VIEWFINDER, // Camera viewfinder with masking
    LOADING,           // Loading/processing states
    EMERGENCY          // Emergency notifications
}

/**
 * Camera-specific glass overlay for viewfinder masking
 */
@Composable
fun CameraGlassOverlay(
    modifier: Modifier = Modifier,
    cameraMode: CameraOverlayMode = CameraOverlayMode.PHOTO,
    showViewfinderMask: Boolean = true,
    adaptiveBlur: Boolean = true,
    emergencyMode: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val cameraConfig = remember(cameraMode) {
        when (cameraMode) {
            CameraOverlayMode.PHOTO -> GlassOverlayConfig.cameraViewfinder
            CameraOverlayMode.VIDEO -> GlassOverlayConfig.cameraViewfinder.copy(
                blurRadius = 10.0f,
                opacity = 0.2f
            )
            CameraOverlayMode.ANALYSIS -> GlassOverlayConfig.modal.copy(
                blurRadius = 15.0f,
                opacity = 0.4f,
                borderColor = Color(0xFFFF6B35).copy(alpha = 0.8f) // Safety orange
            )
        }
    }
    
    val overlayColor = when (cameraMode) {
        CameraOverlayMode.PHOTO -> Color.Black.copy(alpha = 0.2f)
        CameraOverlayMode.VIDEO -> Color.Red.copy(alpha = 0.1f)
        CameraOverlayMode.ANALYSIS -> Color.Black.copy(alpha = 0.3f)
    }
    
    GlassOverlay(
        modifier = modifier,
        config = GlassConfiguration(
            blurRadius = cameraConfig.blurRadius,
            opacity = cameraConfig.opacity,
            animationsEnabled = false, // Disabled for camera performance
            enabledInEmergencyMode = false,
            supportLevel = com.hazardhawk.ui.glass.GlassSupportLevel.REDUCED
        ),
        containerColor = overlayColor,
        contentColor = Color.White,
        shape = RoundedCornerShape(cameraConfig.cornerRadius.dp),
        maskingEnabled = showViewfinderMask,
        adaptiveBlur = adaptiveBlur,
        emergencyMode = emergencyMode,
        content = content
    )
}

/**
 * Camera overlay modes for specialized styling
 */
enum class CameraOverlayMode {
    PHOTO,    // Photo capture overlay
    VIDEO,    // Video recording overlay
    ANALYSIS  // AI analysis overlay
}

/**
 * Safety-focused glass overlay with accessibility features
 */
@Composable
fun SafetyGlassOverlay(
    modifier: Modifier = Modifier,
    safetyLevel: SafetyOverlayLevel = SafetyOverlayLevel.STANDARD,
    adaptiveBlur: Boolean = true,
    emergencyMode: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val safetyConfig = remember(safetyLevel) {
        when (safetyLevel) {
            SafetyOverlayLevel.STANDARD -> GlassOverlayConfig.modal
            SafetyOverlayLevel.WARNING -> GlassOverlayConfig.modal.copy(
                borderColor = Color(0xFFFFB020), // Warning orange
                borderWidth = 2.0f,
                opacity = 0.6f
            )
            SafetyOverlayLevel.CRITICAL -> GlassOverlayConfig.emergency.copy(
                borderWidth = 3.0f,
                opacity = 0.8f
            )
        }
    }
    
    val overlayColor = when (safetyLevel) {
        SafetyOverlayLevel.STANDARD -> Color.Black.copy(alpha = 0.4f)
        SafetyOverlayLevel.WARNING -> Color(0xFFFFB020).copy(alpha = 0.2f)
        SafetyOverlayLevel.CRITICAL -> Color.Red.copy(alpha = 0.3f)
    }
    
    GlassOverlay(
        modifier = modifier,
        config = GlassConfiguration(
            blurRadius = safetyConfig.blurRadius,
            opacity = safetyConfig.opacity,
            animationsEnabled = false,
            enabledInEmergencyMode = safetyLevel == SafetyOverlayLevel.CRITICAL,
            supportLevel = when (safetyLevel) {
                SafetyOverlayLevel.CRITICAL -> com.hazardhawk.ui.glass.GlassSupportLevel.DISABLED
                else -> com.hazardhawk.ui.glass.GlassSupportLevel.REDUCED
            }
        ),
        containerColor = overlayColor,
        contentColor = Color.White,
        maskingEnabled = false,
        adaptiveBlur = adaptiveBlur && safetyLevel != SafetyOverlayLevel.CRITICAL,
        emergencyMode = emergencyMode || safetyLevel == SafetyOverlayLevel.CRITICAL,
        content = content
    )
}

/**
 * Safety levels for construction overlay styling
 */
enum class SafetyOverlayLevel {
    STANDARD,  // Normal operation
    WARNING,   // Caution required
    CRITICAL   // High risk/emergency situation
}