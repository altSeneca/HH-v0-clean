package com.hazardhawk.ui.glass.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Semantics import removed to fix compilation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import com.hazardhawk.camera.LocationData
// GlassConfiguration and EmergencyMode are now defined in GlassExtensions.kt
import com.hazardhawk.ui.theme.ConstructionColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Glass metadata overlay with adaptive transparency and construction-specific information display.
 * 
 * This component displays project metadata, location information, and timestamps with glass
 * morphism effects that adapt to environmental conditions and construction requirements.
 * 
 * Features:
 * - Adaptive glass transparency based on lighting conditions
 * - Construction project metadata display
 * - GPS coordinates and location information
 * - Real-time timestamp updates
 * - Emergency mode high-contrast display
 * - Performance-optimized rendering
 */
@Composable
fun GlassMetadataOverlay(
    locationData: LocationData?,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    companyName: String? = null,
    projectName: String? = null,
    modifier: Modifier = Modifier
) {
    // Real-time timestamp updates
    var currentTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTimestamp = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000) // Update every second
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Top-left: Company and project information
        AnimatedVisibility(
            visible = companyName != null || projectName != null,
            enter = fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300)
            ),
            exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300)
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            GlassProjectInfoCard(
                companyName = companyName,
                projectName = projectName,
                configuration = configuration,
                hazeState = hazeState
            )
        }
        
        // Top-right: Environmental and device status
        GlassEnvironmentalStatus(
            configuration = configuration,
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
        )
        
        // Bottom-left: Location and GPS information
        AnimatedVisibility(
            visible = locationData != null,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            locationData?.let { location ->
                GlassLocationCard(
                    locationData = location,
                    configuration = configuration,
                    hazeState = hazeState
                )
            }
        }
        
        // Bottom-right: Timestamp information
        GlassTimestampCard(
            timestamp = currentTimestamp,
            configuration = configuration,
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
        
        // Center-bottom: Construction safety reminder (when applicable)
        if (configuration.isOutdoorMode && configuration.emergencyMode == EmergencyMode.NORMAL) {
            GlassSafetyReminder(
                configuration = configuration,
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            )
        }
    }
}

@Composable
private fun GlassProjectInfoCard(
    companyName: String?,
    projectName: String?,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .hazeEffect(
                state = hazeState
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.8f)
            } else Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (configuration.safetyBorderGlow) {
            androidx.compose.foundation.BorderStroke(1.dp, configuration.borderColor.copy(alpha = 0.6f))
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with construction icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = "Project Info",
                    tint = if (configuration.isHighContrastMode) configuration.borderColor else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "PROJECT INFO",
                    color = if (configuration.isHighContrastMode) configuration.borderColor else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Company name
            companyName?.let { company ->
                GlassMetadataItem(
                    label = "Company",
                    value = company,
                    icon = Icons.Default.Business,
                    configuration = configuration
                )
            }
            
            // Project name
            projectName?.let { project ->
                GlassMetadataItem(
                    label = "Project",
                    value = project,
                    icon = Icons.Default.Engineering,
                    configuration = configuration
                )
            }
            
            // Construction phase (could be dynamic)
            GlassMetadataItem(
                label = "Phase",
                value = "Documentation",
                icon = Icons.Default.Assignment,
                configuration = configuration
            )
        }
    }
}

@Composable
private fun GlassLocationCard(
    locationData: LocationData,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .hazeEffect(
                state = hazeState
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.8f)
            } else Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (configuration.safetyBorderGlow) {
            androidx.compose.foundation.BorderStroke(1.dp, ConstructionColors.HighVisYellow.copy(alpha = 0.6f))
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with location icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Info",
                    tint = ConstructionColors.HighVisYellow,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "LOCATION",
                    color = if (configuration.isHighContrastMode) ConstructionColors.HighVisYellow else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // GPS coordinates
            GlassMetadataItem(
                label = "GPS",
                value = "${String.format("%.6f", locationData.latitude)}, ${String.format("%.6f", locationData.longitude)}",
                icon = Icons.Default.GpsFixed,
                configuration = configuration
            )
            
            // Accuracy
            GlassMetadataItem(
                label = "Accuracy",
                value = "${String.format("%.1f", locationData.accuracy)}m",
                icon = Icons.Default.MyLocation,
                configuration = configuration
            )
            
            // Address (if available)
            locationData.address?.let { address ->
                GlassMetadataItem(
                    label = "Address",
                    value = address.take(30) + if (address.length > 30) "..." else "",
                    icon = Icons.Default.Place,
                    configuration = configuration
                )
            }
        }
    }
}

@Composable
private fun GlassTimestampCard(
    timestamp: Long,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val date = Date(timestamp)
    
    Card(
        modifier = modifier
            .hazeEffect(
                state = hazeState
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.8f)
            } else Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (configuration.safetyBorderGlow) {
            androidx.compose.foundation.BorderStroke(1.dp, configuration.borderColor.copy(alpha = 0.6f))
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Header with time icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Timestamp",
                    tint = if (configuration.isHighContrastMode) configuration.borderColor else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "TIMESTAMP",
                    color = if (configuration.isHighContrastMode) configuration.borderColor else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Date
            Text(
                text = dateFormat.format(date),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            // Time with pulsing animation for seconds
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val timeString = timeFormat.format(date)
                val timeParts = timeString.split(":")
                
                Text(
                    text = "${timeParts[0]}:${timeParts[1]}:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Pulsing seconds
                val pulseAnimation by rememberInfiniteTransition().animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                
                Text(
                    text = timeParts[2],
                    color = if (configuration.isHighContrastMode) {
                        configuration.borderColor.copy(alpha = pulseAnimation)
                    } else {
                        Color.White.copy(alpha = pulseAnimation)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GlassEnvironmentalStatus(
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Light level indicator
        GlassStatusIndicator(
            icon = when (configuration.lightingCondition.name) {
                "OUTDOOR_BRIGHT" -> Icons.Default.WbSunny
                "OUTDOOR_NORMAL" -> Icons.Default.WbCloudy
                "LOW_LIGHT" -> Icons.Default.WbTwilight
                "NIGHT" -> Icons.Default.Bedtime
                else -> Icons.Default.Lightbulb
            },
            label = configuration.lightingCondition.name.replace("_", " "),
            color = when (configuration.lightingCondition.name) {
                "OUTDOOR_BRIGHT" -> ConstructionColors.HighVisYellow
                "OUTDOOR_NORMAL" -> Color.White
                "LOW_LIGHT" -> ConstructionColors.SafetyOrange
                "NIGHT" -> Color.Blue
                else -> Color.White
            },
            configuration = configuration,
            hazeState = hazeState
        )
        
        // Glass performance indicator
        if (configuration.supportLevel.name != "FULL") {
            GlassStatusIndicator(
                icon = when (configuration.supportLevel.name) {
                    "REDUCED" -> Icons.Default.Speed
                    "DISABLED" -> Icons.Default.Block
                    else -> Icons.Default.CheckCircle
                },
                label = "Glass ${configuration.supportLevel.name}",
                color = when (configuration.supportLevel.name) {
                    "REDUCED" -> ConstructionColors.SafetyOrange
                    "DISABLED" -> ConstructionColors.CautionRed
                    else -> ConstructionColors.SafetyGreen
                },
                configuration = configuration,
                hazeState = hazeState
            )
        }
        
        // Emergency mode indicator
        if (configuration.isHighContrastMode || configuration.emergencyMode.name == "EMERGENCY") {
            GlassStatusIndicator(
                icon = Icons.Default.Warning,
                label = "EMERGENCY MODE",
                color = ConstructionColors.CautionRed,
                configuration = configuration,
                hazeState = hazeState,
                isPulsing = true
            )
        }
    }
}

@Composable
private fun GlassStatusIndicator(
    icon: ImageVector,
    label: String,
    color: Color,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    isPulsing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pulseAnimation by rememberInfiniteTransition().animateFloat(
        initialValue = if (isPulsing) 0.7f else 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Card(
        modifier = modifier
            .hazeEffect(
                state = hazeState
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.7f)
            } else Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color.copy(alpha = if (isPulsing) pulseAnimation else 1.0f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = if (isPulsing) pulseAnimation else 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GlassSafetyReminder(
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    // Rotate through safety reminders
    val safetyMessages = listOf(
        "PPE Required",
        "Hard Hat Zone",
        "Safety First",
        "Watch for Hazards",
        "Stay Alert"
    )
    
    var currentMessageIndex by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000) // Change message every 5 seconds
            currentMessageIndex = (currentMessageIndex + 1) % safetyMessages.size
        }
    }
    
    AnimatedContent(
        targetState = currentMessageIndex,
        transitionSpec = {
            if (configuration.animationsEnabled) {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        modifier = modifier
    ) { index ->
        Card(
            modifier = Modifier
                .hazeEffect(
                    state = hazeState
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (configuration.supportLevel.name == "DISABLED") {
                    ConstructionColors.SafetyOrange.copy(alpha = 0.8f)
                } else Color.Transparent
            ),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = ConstructionColors.SafetyOrange
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Safety",
                    tint = ConstructionColors.HighVisYellow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = safetyMessages[index],
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GlassMetadataItem(
    label: String,
    value: String,
    icon: ImageVector,
    configuration: GlassConfiguration,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (configuration.isHighContrastMode) {
                configuration.borderColor.copy(alpha = 0.8f)
            } else {
                Color.White.copy(alpha = 0.7f)
            },
            modifier = Modifier.size(14.dp)
        )
        
        Column {
            Text(
                text = label,
                color = if (configuration.isHighContrastMode) {
                    configuration.borderColor.copy(alpha = 0.9f)
                } else {
                    Color.White.copy(alpha = 0.8f)
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}