package com.hazardhawk.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

data class MetadataDisplaySettings(
    val showTimestamp: Boolean = true,
    val showLocation: Boolean = true,
    val showProjectInfo: Boolean = true,
    val showUserInfo: Boolean = true,
    val showCoordinates: Boolean = false, // Technical coordinates - off by default
    val overlayOpacity: Float = 0.85f,
    val position: OverlayPosition = OverlayPosition.TOP_LEFT
)

enum class OverlayPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

data class CaptureMetadata(
    val timestamp: Long = System.currentTimeMillis(),
    val locationData: LocationData = LocationData(),
    val projectName: String = "",
    val projectId: String = "",
    val userName: String = "",
    val userId: String = "",
    val deviceInfo: String = ""
)

// Construction Safety Color Scheme
object ConstructionColors {
    val SafetyOrange = Color(0xFFFF6B35)
    val HighVisYellow = Color(0xFFFFDD00)
    val ReflectiveWhite = Color(0xFFFAFAFA)
    val CautionRed = Color(0xFFE53E3E)
    val WorkZoneBlue = Color(0xFF2B6CB0)
    val OverlayBackground = Color.Black.copy(alpha = 0.75f)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFE2E8F0)
    val AccentBorder = SafetyOrange.copy(alpha = 0.8f)
}

@Composable
fun MetadataOverlay(
    metadata: CaptureMetadata,
    settings: MetadataDisplaySettings,
    modifier: Modifier = Modifier
) {
    val formattedTimestamp = remember(metadata.timestamp) {
        SimpleDateFormat("MMM dd, yyyy\nhh:mm:ss a", Locale.getDefault()).format(Date(metadata.timestamp))
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = when (settings.position) {
            OverlayPosition.TOP_LEFT -> Alignment.TopStart
            OverlayPosition.TOP_RIGHT -> Alignment.TopEnd
            OverlayPosition.BOTTOM_LEFT -> Alignment.BottomStart
            OverlayPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
        }
    ) {
        Card(
            modifier = Modifier
                .padding(12.dp)
                .widthIn(max = 280.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = ConstructionColors.OverlayBackground.copy(alpha = settings.overlayOpacity)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header with HazardHawk branding
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🥺 HazardHawk",
                        color = ConstructionColors.SafetyOrange,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (metadata.locationData.isAvailable) ConstructionColors.HighVisYellow
                                else ConstructionColors.CautionRed
                            )
                    )
                }
                
                Divider(
                    color = ConstructionColors.AccentBorder,
                    thickness = 1.dp
                )
                
                // Timestamp
                if (settings.showTimestamp) {
                    MetadataItem(
                        icon = "🕒",
                        label = "Captured",
                        value = formattedTimestamp
                    )
                }
                
                // Location Information
                if (settings.showLocation && metadata.locationData.isAvailable) {
                    if (settings.showCoordinates) {
                        // User prefers raw GPS coordinates
                        MetadataItem(
                            icon = "",
                            label = "Location",
                            value = "${String.format("%.6f", metadata.locationData.latitude)}, ${String.format("%.6f", metadata.locationData.longitude)}",
                            maxLines = 1
                        )
                    } else {
                        // User prefers human-readable address
                        MetadataItem(
                            icon = "📍",
                            label = "Location",
                            value = metadata.locationData.address.ifBlank {
                                "${String.format("%.6f", metadata.locationData.latitude)}, ${String.format("%.6f", metadata.locationData.longitude)}"
                            },
                            maxLines = 2
                        )
                    }
                    
                    // Location accuracy
                    if (metadata.locationData.accuracy > 0) {
                        MetadataItem(
                            icon = "",
                            label = "Accuracy",
                            value = "±${String.format("%.1f", metadata.locationData.accuracy)}m",
                            textSize = 11.sp
                        )
                    }
                }
                
                // Project Information
                if (settings.showProjectInfo && metadata.projectName.isNotBlank()) {
                    MetadataItem(
                        icon = "🏢",
                        label = "Project",
                        value = if (metadata.projectId.isNotBlank()) {
                            "${metadata.projectName}\n#${metadata.projectId}"
                        } else {
                            metadata.projectName
                        },
                        maxLines = 2
                    )
                }
                
                // User Information
                if (settings.showUserInfo && metadata.userName.isNotBlank()) {
                    MetadataItem(
                        icon = "👷",
                        label = "Inspector",
                        value = if (metadata.userId.isNotBlank()) {
                            "${metadata.userName}\nID: ${metadata.userId}"
                        } else {
                            metadata.userName
                        },
                        maxLines = 2
                    )
                }
                
                // Device Info (if available)
                if (metadata.deviceInfo.isNotBlank()) {
                    MetadataItem(
                        icon = "📱",
                        label = "Device",
                        value = metadata.deviceInfo,
                        textSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataItem(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    textSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    if (value.isBlank()) return
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 1.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = ConstructionColors.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = value,
                color = ConstructionColors.TextPrimary,
                fontSize = textSize,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                lineHeight = (textSize.value + 2).sp
            )
        }
    }
}

@Composable
fun LiveTimestampOverlay(
    modifier: Modifier = Modifier,
    format: String = "MMM dd, yyyy • hh:mm:ss a"
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val formatter = remember { SimpleDateFormat(format, Locale.getDefault()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000) // Update every second
        }
    }
    
    Box(
        modifier = modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(ConstructionColors.OverlayBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = formatter.format(Date(currentTime)),
            color = ConstructionColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CompactLocationOverlay(
    locationData: LocationData,
    modifier: Modifier = Modifier
) {
    if (!locationData.isAvailable) return
    
    Box(
        modifier = modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(ConstructionColors.OverlayBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📍",
                fontSize = 12.sp
            )
            
            Column {
                Text(
                    text = locationData.address,
                    color = ConstructionColors.TextPrimary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (locationData.accuracy > 0) {
                    Text(
                        text = "±${String.format("%.0f", locationData.accuracy)}m accuracy",
                        color = ConstructionColors.TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}