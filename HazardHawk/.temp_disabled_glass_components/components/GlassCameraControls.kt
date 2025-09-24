package com.hazardhawk.ui.glass.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// // Semantics import removed to fix compilation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import com.hazardhawk.camera.*
// GlassConfiguration is now defined in GlassExtensions.kt
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * Glass-enhanced camera controls overlay with Haze blur effects and construction optimizations.
 * 
 * Features construction-friendly design with large touch targets, haptic feedback,
 * and adaptive glass effects based on environmental conditions.
 */
@Composable
fun GlassCameraControls(
    uiState: CameraUIState,
    glassConfig: GlassConfiguration,
    hazeState: HazeState,
    backgroundHaze: HazeState,
    onFlashToggle: () -> Unit,
    onHDRToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onLevelToggle: () -> Unit,
    onCameraSwitch: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onModeChange: (CameraMode) -> Unit,
    onBurstCountChange: (Int) -> Unit,
    onTimerSecondsChange: (Int) -> Unit,
    onCapture: () -> Unit,
    onSettingsClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onTapToFocus: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Top controls bar with glass effect
        GlassTopControlsBar(
            uiState = uiState,
            glassConfig = glassConfig,
            backgroundHaze = backgroundHaze,
            onFlashToggle = onFlashToggle,
            onHDRToggle = onHDRToggle,
            onGridToggle = onGridToggle,
            onLevelToggle = onLevelToggle,
            onCameraSwitch = onCameraSwitch,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )
        
        // Level indicator (when enabled) with glass background
        if (uiState.showLevel) {
            GlassLevelIndicator(
                levelState = uiState.levelState,
                glassConfig = glassConfig,
                backgroundHaze = backgroundHaze,
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(1f)
            )
        }
        
        // Zoom controls with glass effect
        GlassZoomControls(
            zoomState = uiState.zoomState,
            glassConfig = glassConfig,
            backgroundHaze = backgroundHaze,
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp)
        )
        
        // Mode selector with glass effect
        GlassCameraModeSelector(
            currentMode = uiState.mode,
            glassConfig = glassConfig,
            backgroundHaze = backgroundHaze,
            onModeChange = onModeChange,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(16.dp)
        )
        
        // Bottom controls with glass effect
        GlassBottomControlsBar(
            uiState = uiState,
            glassConfig = glassConfig,
            backgroundHaze = backgroundHaze,
            onBurstCountChange = onBurstCountChange,
            onTimerSecondsChange = onTimerSecondsChange,
            onCapture = onCapture,
            onSettingsClick = onSettingsClick,
            onGalleryClick = onGalleryClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
        
        // Status and info overlay with glass effect
        GlassStatusInfoOverlay(
            uiState = uiState,
            glassConfig = glassConfig,
            backgroundHaze = backgroundHaze,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
        
        // Timer countdown overlay with enhanced glass effect
        if (uiState.timerCountdown > 0) {
            GlassTimerCountdownOverlay(
                countdown = uiState.timerCountdown,
                glassConfig = glassConfig,
                backgroundHaze = backgroundHaze,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Message overlay with glass effect
        if (uiState.message.isNotEmpty()) {
            GlassMessageOverlay(
                message = uiState.message,
                isError = uiState.isError,
                glassConfig = glassConfig,
                backgroundHaze = backgroundHaze,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
    }
}

@Composable
private fun GlassTopControlsBar(
    uiState: CameraUIState,
    glassConfig: GlassConfiguration,
    backgroundHaze: HazeState,
    onFlashToggle: () -> Unit,
    onHDRToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onLevelToggle: () -> Unit,
    onCameraSwitch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .hazeEffect(
                state = backgroundHaze
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp),
        border = if (glassConfig.safetyBorderGlow) {
            androidx.compose.foundation.BorderStroke(
                width = glassConfig.borderWidth.dp,
                color = glassConfig.borderColor
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash control with construction-friendly size
            GlassControlButton(
                icon = when (uiState.flashMode) {
                    FlashMode.AUTO -> Icons.Default.FlashOn
                    FlashMode.ON -> Icons.Default.FlashOn
                    FlashMode.OFF -> Icons.Default.FlashOff
                },
                label = uiState.flashMode.name,
                isActive = uiState.flashMode != FlashMode.OFF,
                glassConfig = glassConfig,
                onClick = onFlashToggle
            )
            
            // HDR control
            GlassControlButton(
                icon = Icons.Default.CameraEnhance,
                label = "HDR",
                isActive = uiState.isHDREnabled,
                glassConfig = glassConfig,
                onClick = onHDRToggle
            )
            
            // Grid control  
            GlassControlButton(
                icon = Icons.Default.Grid3x3,
                label = "GRID",
                isActive = uiState.showGrid,
                glassConfig = glassConfig,
                onClick = onGridToggle
            )
            
            // Level control
            GlassControlButton(
                icon = Icons.Default.Straighten,
                label = "LEVEL",
                isActive = uiState.showLevel,
                glassConfig = glassConfig,
                onClick = onLevelToggle
            )
            
            // Camera switch
            GlassControlButton(
                icon = Icons.Default.SwitchCamera,
                label = if (uiState.cameraSelector == androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA) "BACK" else "FRONT",
                isActive = false,
                glassConfig = glassConfig,
                onClick = onCameraSwitch
            )
        }
    }
}

@Composable
private fun GlassControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    glassConfig: GlassConfiguration,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Use construction-optimized touch target size
        val buttonSize = glassConfig.minTouchTargetSize.dp.coerceAtLeast(56.dp)
        
        Button(
            onClick = onClick,
            modifier = Modifier.size(buttonSize),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) {
                    if (glassConfig.isHighContrastMode) ConstructionColors.SafetyOrange
                    else ConstructionColors.SafetyOrange.copy(alpha = 0.8f)
                } else {
                    Color.White.copy(alpha = if (glassConfig.supportLevel.name == "DISABLED") 0.6f else 0.2f)
                },
                contentColor = if (isActive) Color.White else Color.White
            ),
            shape = CircleShape,
            elevation = if (glassConfig.safetyBorderGlow) {
                ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            } else {
                ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            color = if (isActive) glassConfig.borderColor else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GlassZoomControls(
    zoomState: ZoomState,
    glassConfig: GlassConfiguration,
    backgroundHaze: HazeState,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .hazeEffect(state = backgroundHaze)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Transparent)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val buttonSize = glassConfig.minTouchTargetSize.dp
        
        // Zoom in button
        Button(
            onClick = onZoomIn,
            modifier = Modifier.size(buttonSize),
            enabled = zoomState.currentZoomRatio < zoomState.maxZoomRatio,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = if (glassConfig.supportLevel.name == "DISABLED") 0.8f else 0.4f),
                contentColor = Color.White,
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            ),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = "Zoom In",
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Zoom level indicator with glass effect
        AnimatedVisibility(
            visible = zoomState.isZooming || zoomState.currentZoomRatio > 1.0f,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (glassConfig.supportLevel.name == "DISABLED") {
                        Color.Black.copy(alpha = 0.8f)
                    } else Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = if (glassConfig.supportLevel.name != "DISABLED") {
                    Modifier.hazeEffect(state = backgroundHaze)
                } else Modifier
            ) {
                Text(
                    text = "${String.format("%.1f", zoomState.currentZoomRatio)}x",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        
        // Zoom out button
        Button(
            onClick = onZoomOut,
            modifier = Modifier.size(buttonSize),
            enabled = zoomState.currentZoomRatio > zoomState.minZoomRatio,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = if (glassConfig.supportLevel.name == "DISABLED") 0.8f else 0.4f),
                contentColor = Color.White,
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            ),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.ZoomOut,
                contentDescription = "Zoom Out",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun GlassCameraModeSelector(
    currentMode: CameraMode,
    glassConfig: GlassConfiguration,
    backgroundHaze: HazeState,
    onModeChange: (CameraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .hazeEffect(state = backgroundHaze)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Transparent)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CameraMode.values().forEach { mode ->
            val isSelected = mode == currentMode
            val buttonSize = if (isSelected) {
                glassConfig.minTouchTargetSize.dp + 8.dp
            } else {
                glassConfig.minTouchTargetSize.dp
            }
            
            Button(
                onClick = { onModeChange(mode) },
                modifier = Modifier.size(buttonSize),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) {
                        if (glassConfig.isHighContrastMode) ConstructionColors.SafetyOrange
                        else ConstructionColors.SafetyOrange.copy(alpha = 0.9f)
                    } else {
                        Color.Black.copy(alpha = if (glassConfig.supportLevel.name == "DISABLED") 0.8f else 0.4f)
                    },
                    contentColor = Color.White
                ),
                shape = CircleShape,
                elevation = if (isSelected && glassConfig.safetyBorderGlow) {
                    ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                } else {
                    ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                }
            ) {
                Text(
                    text = when (mode) {
                        CameraMode.SINGLE_SHOT -> "1"
                        CameraMode.BURST_MODE -> "B"
                        CameraMode.TIMER_MODE -> "T"
                        CameraMode.HDR_MODE -> "H"
                        CameraMode.VOICE_ACTIVATED -> "V"
                    },
                    fontSize = if (isSelected) 18.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GlassBottomControlsBar(
    uiState: CameraUIState,
    glassConfig: GlassConfiguration,
    backgroundHaze: HazeState,
    onBurstCountChange: (Int) -> Unit,
    onTimerSecondsChange: (Int) -> Unit,
    onCapture: () -> Unit,
    onSettingsClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .hazeEffect(
                state = backgroundHaze
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp),
        border = if (glassConfig.safetyBorderGlow) {
            androidx.compose.foundation.BorderStroke(
                width = glassConfig.borderWidth.dp,
                color = glassConfig.borderColor
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val buttonSize = glassConfig.minTouchTargetSize.dp
            
            // Gallery button
            Button(
                onClick = onGalleryClick,
                modifier = Modifier.size(buttonSize),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = if (glassConfig.supportLevel.name == "DISABLED") 0.6f else 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = "Gallery",
                    tint = Color.White
                )
            }
            
            // Mode-specific controls with glass effect
            when (uiState.mode) {
                CameraMode.BURST_MODE -> {
                    GlassBurstModeControls(
                        burstCount = uiState.burstCount,
                        glassConfig = glassConfig,
                        backgroundHaze = backgroundHaze,
                        onBurstCountChange = onBurstCountChange
                    )
                }
                CameraMode.TIMER_MODE -> {
                    GlassTimerModeControls(
                        timerSeconds = uiState.timerSeconds,
                        glassConfig = glassConfig,
                        backgroundHaze = backgroundHaze,
                        onTimerSecondsChange = onTimerSecondsChange
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.width(80.dp))
                }
            }
            
            // Main capture button with enhanced glass effect
            Button(
                onClick = onCapture,
                modifier = Modifier.size(80.dp),
                enabled = !uiState.isCapturing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isCapturing) Color.Gray else {
                        if (glassConfig.isHighContrastMode) ConstructionColors.SafetyOrange
                        else ConstructionColors.SafetyOrange.copy(alpha = 0.9f)
                    },
                    contentColor = Color.White
                ),
                shape = CircleShape,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                if (uiState.isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = when (uiState.mode) {
                            CameraMode.TIMER_MODE -> if (uiState.timerCountdown > 0) Icons.Default.Stop else Icons.Default.Schedule
                            CameraMode.VOICE_ACTIVATED -> Icons.Default.Mic
                            else -> Icons.Default.PhotoCamera
                        },
                        contentDescription = "Capture",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            // Settings button
            Button(
                onClick = onSettingsClick,
                modifier = Modifier.size(buttonSize),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = if (glassConfig.supportLevel.name == "DISABLED") 0.6f else 0.3f)
                ),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun GlassBurstModeControls(
    burstCount: Int,
    glassConfig: GlassConfiguration,
    backgroundHaze: HazeState,
    onBurstCountChange: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .hazeEffect(state = backgroundHaze)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .padding(8.dp)
    ) {
        val buttonSize = (glassConfig.minTouchTargetSize * 0.7f).dp
        
        Button(
            onClick = { onBurstCountChange((burstCount - 1).coerceAtLeast(2)) },
            modifier = Modifier.size(buttonSize),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.2f)
            ),
            shape = CircleShape
        ) {
            Text("-", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Text(
            text = "$burstCount",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = { onBurstCountChange((burstCount + 1).coerceAtMost(10)) },
            modifier = Modifier.size(buttonSize),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.2f)
            ),
            shape = CircleShape
        ) {
            Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GlassTimerModeControls(
    timerSeconds: Int,
    glassConfig: GlassConfiguration,
    backgroundHaze: HazeState,
    onTimerSecondsChange: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .hazeEffect(state = backgroundHaze)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .padding(8.dp)
    ) {
        val timerOptions = listOf(3, 5, 10)
        
        timerOptions.forEach { seconds ->
            Button(
                onClick = { onTimerSecondsChange(seconds) },
                modifier = Modifier.width(40.dp).height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (seconds == timerSeconds) {
                        if (glassConfig.isHighContrastMode) ConstructionColors.SafetyOrange
                        else ConstructionColors.SafetyOrange.copy(alpha = 0.8f)
                    } else {
                        Color.White.copy(alpha = 0.2f)
                    }
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${seconds}s",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Additional glass components to be implemented in separate files:
// - GlassLevelIndicator
// - GlassStatusInfoOverlay  
// - GlassTimerCountdownOverlay
// - GlassMessageOverlay

@Composable
fun GlassLevelIndicator(
    levelState: LevelState,
    glassConfig: GlassConfiguration,
    backgroundHaze: HazeState,
    modifier: Modifier = Modifier
) {
    val isLevel = levelState.isLevel
    val color = if (isLevel) ConstructionColors.HighVisYellow else Color.White
    
    Card(
        modifier = modifier
            .hazeEffect(
                state = backgroundHaze
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (glassConfig.safetyBorderGlow && isLevel) {
            androidx.compose.foundation.BorderStroke(2.dp, ConstructionColors.SafetyOrange)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("PITCH:", color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "${String.format("%.1f", levelState.pitch)}°",
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("ROLL:", color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "${String.format("%.1f", levelState.roll)}°",
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (isLevel) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "LEVEL",
                    color = if (glassConfig.isHighContrastMode) Color.Black else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GlassStatusInfoOverlay(
    uiState: CameraUIState,
    glassConfig: GlassConfiguration,
    backgroundHaze: HazeState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (uiState.captureCount > 0) {
            GlassStatusCard(
                text = "${uiState.captureCount} photos",
                glassConfig = glassConfig,
                backgroundHaze = backgroundHaze
            )
        }
        
        if (uiState.availableStorageGB > 0) {
            GlassStatusCard(
                text = "${String.format("%.1f", uiState.availableStorageGB)} GB free",
                glassConfig = glassConfig,
                backgroundHaze = backgroundHaze
            )
        }
        
        if (uiState.isVoiceEnabled) {
            GlassStatusCard(
                text = "Voice Ready",
                backgroundColor = ConstructionColors.HighVisYellow,
                textColor = Color.Black,
                glassConfig = glassConfig,
                backgroundHaze = backgroundHaze
            )
        }
    }
}

@Composable
private fun GlassStatusCard(
    text: String,
    glassConfig: GlassConfiguration,
    backgroundHaze: HazeState,
    backgroundColor: Color = Color.Transparent,
    textColor: Color = Color.White
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        modifier = if (backgroundColor == Color.Transparent) {
            Modifier.hazeEffect(state = backgroundHaze)
        } else Modifier
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun GlassTimerCountdownOverlay(
    countdown: Int,
    glassConfig: GlassConfiguration,
    backgroundHaze: HazeState,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (countdown > 0) 1.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    Card(
        modifier = modifier
            .scale(scale)
            .hazeEffect(state = backgroundHaze),
        colors = CardDefaults.cardColors(
            containerColor = if (glassConfig.supportLevel.name == "DISABLED") {
                ConstructionColors.SafetyOrange
            } else Color.Transparent
        ),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(
            width = 4.dp,
            color = ConstructionColors.SafetyOrange
        )
    ) {
        Text(
            text = countdown.toString(),
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
fun GlassMessageOverlay(
    message: String,
    isError: Boolean,
    glassConfig: GlassConfiguration,
    backgroundHaze: HazeState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message.isNotEmpty(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (glassConfig.supportLevel.name == "DISABLED") {
                    if (isError) ConstructionColors.CautionRed else Color.Black.copy(alpha = 0.8f)
                } else Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = if (glassConfig.supportLevel.name != "DISABLED") {
                Modifier.hazeEffect(state = backgroundHaze)
            } else Modifier,
            border = if (isError && glassConfig.safetyBorderGlow) {
                androidx.compose.foundation.BorderStroke(2.dp, ConstructionColors.CautionRed)
            } else null
        ) {
            Text(
                text = message,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}