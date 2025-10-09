package com.hazardhawk.ui.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.models.dashboard.*
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * Command Center Button - Individual action button with role-based visibility
 *
 * Features:
 * - 60dp+ minimum touch target for gloved hands
 * - Haptic feedback on press
 * - Spring animation on interaction
 * - Role-based access control
 * - Coming soon badge for unimplemented features
 * - Notification badge support
 * - High contrast colors
 * - Material 3 design
 */
@Composable
fun CommandCenterButton(
    action: SafetyAction,
    userTier: UserTier,
    onClick: (SafetyAction) -> Unit,
    modifier: Modifier = Modifier,
    notificationBadge: Int = 0
) {
    val buttonConfig = action.toButtonConfig()
    val isAvailable = action.isAvailableForTier(userTier)
    val isImplemented = action.isImplemented()
    val canUse = isAvailable && isImplemented

    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Spring animation for press effect
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "button_scale"
    )

    // Determine colors based on state
    val backgroundColor = when {
        !canUse -> Color.Gray.copy(alpha = 0.3f)
        else -> parseColor(buttonConfig.backgroundColor)
    }

    val contentAlpha = if (canUse) 1f else 0.5f

    Card(
        modifier = modifier
            .scale(scale)
            .aspectRatio(1f)
            .heightIn(min = 80.dp) // Ensures minimum 60dp+ touch target
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = canUse
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick(action)
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (canUse) 6.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor,
                            backgroundColor.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon with notification badge
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = getIconForAction(action),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.TopStart),
                        tint = Color.White.copy(alpha = contentAlpha)
                    )

                    // Notification badge
                    if (notificationBadge > 0 && canUse) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = ConstructionColors.CautionRed
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (notificationBadge > 9) "9+" else notificationBadge.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Coming soon badge
                    if (!isImplemented) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = ConstructionColors.SafetyYellow
                        ) {
                            Text(
                                text = "SOON",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ConstructionColors.AsphaltBlack,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Title and subtitle
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = buttonConfig.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = contentAlpha),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buttonConfig.subtitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = contentAlpha * 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Access restriction overlay
            if (!isAvailable) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = when (action.getRequiredTier()) {
                                    UserTier.SAFETY_LEAD -> "Lead"
                                    UserTier.PROJECT_ADMIN -> "Admin"
                                    else -> ""
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get Material Icon for safety action
 */
private fun getIconForAction(action: SafetyAction): ImageVector {
    return when (action) {
        SafetyAction.CREATE_PTP -> Icons.Default.Assignment
        SafetyAction.CREATE_TOOLBOX_TALK -> Icons.Default.Construction
        SafetyAction.CAPTURE_PHOTO -> Icons.Default.CameraAlt
        SafetyAction.VIEW_REPORTS -> Icons.Default.Assessment
        SafetyAction.ASSIGN_TASKS -> Icons.Default.Group
        SafetyAction.OPEN_GALLERY -> Icons.Default.PhotoLibrary
        SafetyAction.REPORT_INCIDENT -> Icons.Default.Warning
        SafetyAction.START_PRE_SHIFT -> Icons.Default.Schedule
        SafetyAction.LIVE_DETECTION -> Icons.Default.Visibility
        SafetyAction.MANAGE_CREW -> Icons.Default.People
    }
}

/**
 * Parse hex color string to Color
 */
private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        ConstructionColors.SafetyOrange
    }
}

@Preview(showBackground = true)
@Composable
fun CommandCenterButtonPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConstructionColors.Surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Field Access User",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Available to field access
                CommandCenterButton(
                    action = SafetyAction.CAPTURE_PHOTO,
                    userTier = UserTier.FIELD_ACCESS,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )

                // Not available to field access
                CommandCenterButton(
                    action = SafetyAction.CREATE_PTP,
                    userTier = UserTier.FIELD_ACCESS,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Safety Lead User",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Available and implemented
                CommandCenterButton(
                    action = SafetyAction.CREATE_PTP,
                    userTier = UserTier.SAFETY_LEAD,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )

                // Available but not implemented
                CommandCenterButton(
                    action = SafetyAction.CREATE_TOOLBOX_TALK,
                    userTier = UserTier.SAFETY_LEAD,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "With Notification Badge",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CommandCenterButton(
                    action = SafetyAction.VIEW_REPORTS,
                    userTier = UserTier.PROJECT_ADMIN,
                    onClick = {},
                    notificationBadge = 3,
                    modifier = Modifier.weight(1f)
                )

                CommandCenterButton(
                    action = SafetyAction.OPEN_GALLERY,
                    userTier = UserTier.PROJECT_ADMIN,
                    onClick = {},
                    notificationBadge = 12,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
