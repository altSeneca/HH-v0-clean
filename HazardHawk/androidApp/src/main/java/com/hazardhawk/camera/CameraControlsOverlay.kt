package com.hazardhawk.camera

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hazardhawk.ui.theme.ConstructionColors
import kotlin.math.abs

/**
 * Professional camera controls overlay designed for construction workers with gloves
 * Features large touch targets, clear visual feedback, and construction-friendly design
 */
@Composable
fun CameraControlsOverlay(
    uiState: CameraUIState,
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
    onTapToFocus: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Box(modifier = modifier.fillMaxSize()) {
        // Focus ring animation
        FocusRingOverlay(
            focusPoint = null, // Will be connected to ViewModel focusPoint StateFlow
            modifier = Modifier.fillMaxSize()
        )
        
        // Top controls bar
        TopControlsBar(
            uiState = uiState,
            onFlashToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onFlashToggle()
            },
            onHDRToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onHDRToggle()
            },
            onGridToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onGridToggle()
            },
            onLevelToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLevelToggle()
            },
            onCameraSwitch = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCameraSwitch()
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Level indicator (when enabled)
        if (uiState.showLevel) {
            DigitalLevelIndicator(
                levelState = uiState.levelState,
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(1f)
            )
        }
        
        // Zoom controls
        ZoomControls(
            zoomState = uiState.zoomState,
            onZoomIn = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onZoomIn()
            },
            onZoomOut = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onZoomOut()
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp)
        )
        
        // Mode selector
        CameraModeSelector(
            currentMode = uiState.mode,
            onModeChange = { mode ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onModeChange(mode)
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(16.dp)
        )
        
        // Bottom controls
        BottomControlsBar(
            uiState = uiState,
            onBurstCountChange = onBurstCountChange,
            onTimerSecondsChange = onTimerSecondsChange,
            onCapture = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCapture()
            },
            onSettingsClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSettingsClick()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        // Status and info overlay
        StatusInfoOverlay(
            uiState = uiState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
        
        // Timer countdown overlay
        if (uiState.timerCountdown > 0) {
            TimerCountdownOverlay(
                countdown = uiState.timerCountdown,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Message overlay
        if (uiState.message.isNotEmpty()) {
            MessageOverlay(
                message = uiState.message,
                isError = uiState.isError,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
    }
}

@Composable
private fun TopControlsBar(
    uiState: CameraUIState,
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
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash control
            ControlButton(
                icon = when (uiState.flashMode) {
                    FlashMode.AUTO -> Icons.Default.FlashOn
                    FlashMode.ON -> Icons.Default.FlashOn
                    FlashMode.OFF -> Icons.Default.FlashOff
                },
                label = uiState.flashMode.name,
                isActive = uiState.flashMode != FlashMode.OFF,
                onClick = onFlashToggle
            )
            
            // HDR control
            ControlButton(
                icon = Icons.Default.CameraEnhance,
                label = "HDR",
                isActive = uiState.isHDREnabled,
                onClick = onHDRToggle
            )
            
            // Grid control  
            ControlButton(
                icon = Icons.Default.Grid3x3,
                label = "GRID",
                isActive = uiState.showGrid,
                onClick = onGridToggle
            )
            
            // Level control
            ControlButton(
                icon = Icons.Default.Straighten, // Using straighten as level icon
                label = "LEVEL",
                isActive = uiState.showLevel,
                onClick = onLevelToggle
            )
            
            // Camera switch
            ControlButton(
                icon = Icons.Default.SwitchCamera,
                label = if (uiState.cameraSelector == androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA) "BACK" else "FRONT",
                isActive = false,
                onClick = onCameraSwitch
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(56.dp), // Large touch target
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) ConstructionColors.SafetyOrange else Color.White.copy(alpha = 0.2f),
                contentColor = if (isActive) Color.White else Color.White
            ),
            shape = CircleShape
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
            color = if (isActive) ConstructionColors.SafetyOrange else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ZoomControls(
    zoomState: ZoomState,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Zoom in button
        Button(
            onClick = onZoomIn,
            modifier = Modifier.size(60.dp),
            enabled = zoomState.currentZoomRatio < zoomState.maxZoomRatio,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White
            ),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = "Zoom In",
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Zoom level indicator
        AnimatedVisibility(
            visible = zoomState.isZooming || zoomState.currentZoomRatio > 1.0f,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(16.dp)
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
            modifier = Modifier.size(60.dp),
            enabled = zoomState.currentZoomRatio > zoomState.minZoomRatio,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White
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
private fun CameraModeSelector(
    currentMode: CameraMode,
    onModeChange: (CameraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CameraMode.values().forEach { mode ->
            val isSelected = mode == currentMode
            
            Button(
                onClick = { onModeChange(mode) },
                modifier = Modifier.size(if (isSelected) 64.dp else 56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) ConstructionColors.SafetyOrange else Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ),
                shape = CircleShape
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
private fun BottomControlsBar(
    uiState: CameraUIState,
    onBurstCountChange: (Int) -> Unit,
    onTimerSecondsChange: (Int) -> Unit,
    onCapture: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Last photo thumbnail placeholder
            Button(
                onClick = { /* TODO: Open gallery */ },
                modifier = Modifier.size(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = "Gallery",
                    tint = Color.White
                )
            }
            
            // Mode-specific controls
            when (uiState.mode) {
                CameraMode.BURST_MODE -> {
                    BurstModeControls(
                        burstCount = uiState.burstCount,
                        onBurstCountChange = onBurstCountChange
                    )
                }
                CameraMode.TIMER_MODE -> {
                    TimerModeControls(
                        timerSeconds = uiState.timerSeconds,
                        onTimerSecondsChange = onTimerSecondsChange
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.width(80.dp))
                }
            }
            
            // Main capture button
            Button(
                onClick = onCapture,
                modifier = Modifier.size(80.dp),
                enabled = !uiState.isCapturing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isCapturing) Color.Gray else ConstructionColors.SafetyOrange,
                    contentColor = Color.White
                ),
                shape = CircleShape
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
                modifier = Modifier.size(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
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
private fun BurstModeControls(
    burstCount: Int,
    onBurstCountChange: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = { onBurstCountChange((burstCount - 1).coerceAtLeast(2)) },
            modifier = Modifier.size(40.dp),
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
            modifier = Modifier.size(40.dp),
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
private fun TimerModeControls(
    timerSeconds: Int,
    onTimerSecondsChange: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val timerOptions = listOf(3, 5, 10)
        
        timerOptions.forEach { seconds ->
            Button(
                onClick = { onTimerSecondsChange(seconds) },
                modifier = Modifier.width(40.dp).height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (seconds == timerSeconds) ConstructionColors.SafetyOrange else Color.White.copy(alpha = 0.2f)
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

@Composable
private fun StatusInfoOverlay(
    uiState: CameraUIState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Photo counter
        if (uiState.captureCount > 0) {
            StatusCard("${uiState.captureCount} photos")
        }
        
        // Storage info
        if (uiState.availableStorageGB > 0) {
            StatusCard("${String.format("%.1f", uiState.availableStorageGB)} GB free")
        }
        
        // Voice activation status
        if (uiState.isVoiceEnabled) {
            StatusCard(
                text = "Voice Ready",
                backgroundColor = ConstructionColors.HighVisYellow,
                textColor = Color.Black
            )
        }
    }
}

@Composable
private fun StatusCard(
    text: String,
    backgroundColor: Color = Color.Black.copy(alpha = 0.6f),
    textColor: Color = Color.White
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
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
private fun FocusRingOverlay(
    focusPoint: Pair<Float, Float>?,
    modifier: Modifier = Modifier
) {
    // TODO: Implement focus ring animation at tap coordinates
    // This will show a animated ring at the tap location for focus feedback
}

@Composable
private fun TimerCountdownOverlay(
    countdown: Int,
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
        modifier = modifier.scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.SafetyOrange
        ),
        shape = CircleShape
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
private fun MessageOverlay(
    message: String,
    isError: Boolean,
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
                containerColor = if (isError) ConstructionColors.CautionRed else Color.Black.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(12.dp)
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

/**
 * Digital level indicator showing pitch and roll for straight shots
 */
@Composable
fun DigitalLevelIndicator(
    levelState: LevelState,
    modifier: Modifier = Modifier
) {
    val isLevel = levelState.isLevel
    val color = if (isLevel) ConstructionColors.HighVisYellow else Color.White
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isLevel) Color.Green.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pitch indicator (up/down tilt)
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
            
            // Roll indicator (left/right tilt)
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
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}