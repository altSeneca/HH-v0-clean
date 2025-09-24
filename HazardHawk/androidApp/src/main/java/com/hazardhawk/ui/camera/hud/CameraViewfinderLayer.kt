package com.hazardhawk.ui.camera.hud

import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hazardhawk.ui.theme.ConstructionColors
import com.hazardhawk.ui.camera.hud.MetadataOverlayState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Camera Viewfinder Layer for Safety HUD
 * 
 * Full-screen camera feed with permanent metadata overlay
 * Designed for construction environments with high visibility
 */
@Composable
fun CameraViewfinderLayer(
    controller: LifecycleCameraController,
    metadataOverlay: MetadataOverlayState,
    emergencyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    Box(modifier = modifier.fillMaxSize()) {
        // Camera Preview - Full screen real estate
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.controller = controller
                    controller.bindToLifecycle(lifecycleOwner)
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Safety HUD Overlay - MOVED TO UnifiedCameraOverlay for proper viewport positioning
        // Keeping this comment for context - metadata now rendered by UnifiedCameraOverlay
        
        // Emergency mode indicators
        if (emergencyMode) {
            EmergencyModeIndicators(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Permanent Safety HUD overlay
 * Expandable metadata bar with critical information
 * Always visible and burned into final image
 */
@Composable
private fun SafetyHUDOverlay(
    metadataOverlay: MetadataOverlayState,
    emergencyMode: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val dateFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }
    
    val formattedTimestamp = remember(metadataOverlay.timestamp) {
        dateFormatter.format(Date(metadataOverlay.timestamp))
    }
    
    // Basic metadata line
    val basicMetadataText = buildString {
        if (metadataOverlay.companyName.isNotBlank()) {
            append(metadataOverlay.companyName)
        } else {
            append("HazardHawk")
        }
        
        append(" | ")
        
        if (metadataOverlay.projectName.isNotBlank()) {
            append(metadataOverlay.projectName)
        } else {
            append("Safety Documentation")
        }
        
        append(" | ")
        append(formattedTimestamp)
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { isExpanded = !isExpanded },
        color = if (emergencyMode) {
            ConstructionColors.CautionRed.copy(alpha = 0.9f)
        } else {
            Color.Black.copy(alpha = 0.75f)
        },
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Basic metadata line (always visible)
            Text(
                text = basicMetadataText,
                color = Color.White,
                fontSize = if (emergencyMode) 16.sp else 14.sp,
                fontWeight = if (emergencyMode) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Expandable GPS/Address information
            if (isExpanded && (metadataOverlay.gpsCoordinates.isNotBlank() || metadataOverlay.address.isNotBlank())) {
                Spacer(modifier = Modifier.height(8.dp))
                
                if (metadataOverlay.gpsCoordinates.isNotBlank()) {
                    Text(
                        text = "GPS: ${metadataOverlay.gpsCoordinates}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (metadataOverlay.address.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Address: ${metadataOverlay.address}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Tap indicator if expandable content available
            if (metadataOverlay.gpsCoordinates.isNotBlank() || metadataOverlay.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isExpanded) "▲ Tap to collapse" else "▼ Tap for location",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Emergency Mode Visual Indicators
 * High-contrast borders and warnings for critical situations
 */
@Composable
private fun EmergencyModeIndicators(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Emergency border frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            // Top border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(ConstructionColors.CautionRed)
                    .align(Alignment.TopCenter)
            )
            
            // Bottom border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(ConstructionColors.CautionRed)
                    .align(Alignment.BottomCenter)
            )
            
            // Left border
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(ConstructionColors.CautionRed)
                    .align(Alignment.CenterStart)
            )
            
            // Right border
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(ConstructionColors.CautionRed)
                    .align(Alignment.CenterEnd)
            )
        }
        
        // Emergency mode text indicator
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            color = ConstructionColors.CautionRed,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp
        ) {
            Text(
                text = "🚨 EMERGENCY MODE",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Grid Lines Overlay for photo composition
 * Rule of thirds grid for professional documentation
 */
@Composable
fun GridLinesOverlay(
    visible: Boolean,
    emergencyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    
    val gridColor = if (emergencyMode) {
        Color.White.copy(alpha = 0.8f)
    } else {
        Color.White.copy(alpha = 0.3f)
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Vertical lines
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(gridColor)
                .align(Alignment.Center)
                .offset(x = (-100).dp) // 1/3 from left
        )
        
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(gridColor)
                .align(Alignment.Center)
                .offset(x = 100.dp) // 1/3 from right
        )
        
        // Horizontal lines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(gridColor)
                .align(Alignment.Center)
                .offset(y = (-80).dp) // 1/3 from top
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(gridColor)
                .align(Alignment.Center)
                .offset(y = 80.dp) // 1/3 from bottom
        )
    }
}

/**
 * Level Indicator for construction accuracy
 * Shows device tilt for properly aligned photos
 */
@Composable
fun LevelIndicator(
    visible: Boolean,
    tiltAngle: Float = 0f, // Device tilt in degrees
    modifier: Modifier = Modifier
) {
    if (!visible) return
    
    Surface(
        modifier = modifier
            .size(80.dp, 24.dp),
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Level bubble
            // Smooth out the level indicator to reduce sensitivity
            val smoothedAngle = kotlin.math.abs(tiltAngle).let { absAngle ->
                when {
                    absAngle < 1f -> 0f // Dead zone for very small tilts
                    absAngle < 5f -> absAngle * 0.5f // Reduce sensitivity for small tilts
                    else -> absAngle * 0.8f // Slightly reduce for larger tilts
                } * if (tiltAngle < 0) -1f else 1f
            }
            val bubbleOffset = (smoothedAngle * 1.5f).coerceIn(-20f, 20f) // Max 20dp offset with reduced multiplier
            
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .offset(x = bubbleOffset.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        if (kotlin.math.abs(tiltAngle) < 2f) {
                            ConstructionColors.SafetyGreen
                        } else {
                            ConstructionColors.SafetyOrange
                        }
                    )
            )
            
            // Center target lines
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(Color.White.copy(alpha = 0.8f))
            )
        }
    }
}