package com.hazardhawk.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.models.dashboard.*
import com.hazardhawk.ui.theme.ConstructionColors
import com.hazardhawk.ui.theme.HazardColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity feed item renderer that displays different layouts based on item type.
 *
 * Handles all ActivityFeedItem sealed class types:
 * - PTPActivity: Pre-Task Plans
 * - HazardActivity: Detected hazards
 * - ToolboxTalkActivity: Safety talks
 * - PhotoActivity: Photo captures
 * - SystemAlert: System notifications
 *
 * @param item The activity feed item to render
 * @param onClick Callback when the item is clicked
 * @param modifier Optional modifier
 */
@Composable
fun ActivityFeedItem(
    item: ActivityFeedItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon indicator
            ActivityIcon(item = item)

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActivityTitle(item = item)
                ActivitySubtitle(item = item)
                ActivityTimestamp(timestamp = item.timestamp)
            }

            // Status indicator or action
            ActivityIndicator(item = item)
        }
    }
}

/**
 * Icon component for different activity types.
 */
@Composable
private fun ActivityIcon(item: ActivityFeedItem) {
    val (icon, backgroundColor, iconColor) = when (item) {
        is ActivityFeedItem.PTPActivity -> Triple(
            Icons.Default.Assignment,
            getPTPStatusColor(item.status).copy(alpha = 0.2f),
            getPTPStatusColor(item.status)
        )
        is ActivityFeedItem.HazardActivity -> Triple(
            Icons.Default.Warning,
            getHazardSeverityColor(item.severity).copy(alpha = 0.2f),
            getHazardSeverityColor(item.severity)
        )
        is ActivityFeedItem.ToolboxTalkActivity -> Triple(
            Icons.Default.Groups,
            ConstructionColors.SafetyGreen.copy(alpha = 0.2f),
            ConstructionColors.SafetyGreen
        )
        is ActivityFeedItem.PhotoActivity -> Triple(
            Icons.Default.PhotoCamera,
            ConstructionColors.WorkZoneBlue.copy(alpha = 0.2f),
            ConstructionColors.WorkZoneBlue
        )
        is ActivityFeedItem.SystemAlert -> Triple(
            getAlertIcon(item.alertType),
            getAlertColor(item.priority).copy(alpha = 0.2f),
            getAlertColor(item.priority)
        )
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Title component for different activity types.
 */
@Composable
private fun ActivityTitle(item: ActivityFeedItem) {
    val title = when (item) {
        is ActivityFeedItem.PTPActivity -> item.ptpTitle
        is ActivityFeedItem.HazardActivity -> item.hazardType
        is ActivityFeedItem.ToolboxTalkActivity -> item.talkTitle
        is ActivityFeedItem.PhotoActivity -> "Photo Captured"
        is ActivityFeedItem.SystemAlert -> item.message
    }

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = ConstructionColors.OnSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Subtitle component for different activity types.
 */
@Composable
private fun ActivitySubtitle(item: ActivityFeedItem) {
    val subtitle = when (item) {
        is ActivityFeedItem.PTPActivity -> "${item.projectName} • ${item.status.name.replace('_', ' ')}"
        is ActivityFeedItem.HazardActivity -> buildString {
            append(item.hazardDescription)
            if (item.oshaCode != null) {
                append(" • ${item.oshaCode}")
            }
            if (item.location != null) {
                append(" • ${item.location}")
            }
        }
        is ActivityFeedItem.ToolboxTalkActivity -> "${item.topic} • ${item.attendeeCount} attendees"
        is ActivityFeedItem.PhotoActivity -> buildString {
            if (item.location != null) append("${item.location} • ")
            if (item.hazardCount > 0) {
                append("${item.hazardCount} hazard${if (item.hazardCount > 1) "s" else ""} detected")
            } else if (item.analyzed) {
                append("No hazards detected")
            } else {
                append("Awaiting analysis")
            }
        }
        is ActivityFeedItem.SystemAlert -> item.alertType.name.replace('_', ' ')
    }

    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        fontSize = 13.sp,
        color = ConstructionColors.OnSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Timestamp component showing how long ago the activity occurred.
 */
@Composable
private fun ActivityTimestamp(timestamp: Long) {
    val timeAgo = getTimeAgo(timestamp)

    Text(
        text = timeAgo,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 11.sp,
        color = ConstructionColors.OnSurfaceVariant.copy(alpha = 0.7f)
    )
}

/**
 * Indicator component for status badges or action icons.
 */
@Composable
private fun ActivityIndicator(item: ActivityFeedItem) {
    when (item) {
        is ActivityFeedItem.PTPActivity -> {
            StatusBadge(
                text = item.status.name.take(3),
                color = getPTPStatusColor(item.status)
            )
        }
        is ActivityFeedItem.HazardActivity -> {
            if (item.resolved) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Resolved",
                    tint = ConstructionColors.SafetyGreen,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                StatusBadge(
                    text = item.severity.name.take(3),
                    color = getHazardSeverityColor(item.severity)
                )
            }
        }
        is ActivityFeedItem.ToolboxTalkActivity -> {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = ConstructionColors.SafetyGreen,
                modifier = Modifier.size(24.dp)
            )
        }
        is ActivityFeedItem.PhotoActivity -> {
            if (item.hazardCount > 0) {
                Badge(
                    containerColor = ConstructionColors.CautionRed,
                    contentColor = Color.White
                ) {
                    Text(
                        text = item.hazardCount.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (item.analyzed) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Analyzed",
                    tint = ConstructionColors.SafetyGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        is ActivityFeedItem.SystemAlert -> {
            if (!item.dismissed && item.actionRequired) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Action Required",
                    tint = getAlertColor(item.priority),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Status badge component for compact status display.
 */
@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.2f),
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// Helper functions

private fun getPTPStatusColor(status: PTPStatus): Color {
    return when (status) {
        PTPStatus.DRAFT -> ConstructionColors.ConcreteGray
        PTPStatus.PENDING_REVIEW -> ConstructionColors.Warning
        PTPStatus.APPROVED -> ConstructionColors.SafetyGreen
        PTPStatus.ACTIVE -> ConstructionColors.SafetyOrange
        PTPStatus.COMPLETED -> ConstructionColors.WorkZoneBlue
        PTPStatus.ARCHIVED -> ConstructionColors.OnSurfaceVariant
    }
}

private fun getHazardSeverityColor(severity: HazardSeverity): Color {
    return when (severity) {
        HazardSeverity.CRITICAL -> ConstructionColors.CautionRed
        HazardSeverity.HIGH -> ConstructionColors.SafetyOrange
        HazardSeverity.MEDIUM -> ConstructionColors.SafetyYellow
        HazardSeverity.LOW -> ConstructionColors.SafetyGreen
    }
}

private fun getAlertIcon(alertType: AlertType): ImageVector {
    return when (alertType) {
        AlertType.OSHA_UPDATE -> Icons.Default.Update
        AlertType.SAFETY_REMINDER -> Icons.Default.NotificationImportant
        AlertType.SYSTEM_UPDATE -> Icons.Default.SystemUpdate
        AlertType.COMPLIANCE -> Icons.Default.Verified
        AlertType.INCIDENT -> Icons.Default.ReportProblem
        AlertType.WEATHER -> Icons.Default.Cloud
    }
}

private fun getAlertColor(priority: AlertPriority): Color {
    return when (priority) {
        AlertPriority.URGENT -> ConstructionColors.CautionRed
        AlertPriority.HIGH -> ConstructionColors.SafetyOrange
        AlertPriority.MEDIUM -> ConstructionColors.WorkZoneBlue
        AlertPriority.LOW -> ConstructionColors.OnSurfaceVariant
    }
}

private fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
