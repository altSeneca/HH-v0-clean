package com.hazardhawk.ui.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.models.dashboard.*
import com.hazardhawk.ui.theme.ConstructionColors
import kotlinx.coroutines.delay

/**
 * Command Center Grid - 2x3 grid of action buttons
 *
 * Features:
 * - Responsive 2x3 grid layout (2 columns, 3 rows)
 * - Role-based button filtering
 * - Staggered entrance animations
 * - Adaptive spacing for different screen sizes
 * - Automatic button arrangement based on priority
 * - Coming soon indicators
 * - Construction-optimized touch targets
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CommandCenterGrid(
    userTier: UserTier,
    onActionClick: (SafetyAction) -> Unit,
    modifier: Modifier = Modifier,
    actions: List<SafetyAction> = getDefaultCommandCenterButtons(),
    notificationBadges: Map<SafetyAction, Int> = emptyMap()
) {
    var animationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        animationStarted = true
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = null,
                        tint = ConstructionColors.WorkZoneBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Command Center",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConstructionColors.OnSurface
                    )
                }

                // User tier badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (userTier) {
                        UserTier.PROJECT_ADMIN -> ConstructionColors.SafetyGreen
                        UserTier.SAFETY_LEAD -> ConstructionColors.WorkZoneBlue
                        UserTier.FIELD_ACCESS -> ConstructionColors.ConcreteGray
                    }.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = when (userTier) {
                            UserTier.PROJECT_ADMIN -> "Admin Access"
                            UserTier.SAFETY_LEAD -> "Lead Access"
                            UserTier.FIELD_ACCESS -> "Field Access"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (userTier) {
                            UserTier.PROJECT_ADMIN -> ConstructionColors.SafetyGreen
                            UserTier.SAFETY_LEAD -> ConstructionColors.WorkZoneBlue
                            UserTier.FIELD_ACCESS -> ConstructionColors.ConcreteGray
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(color = ConstructionColors.SurfaceVariant)

            // 2x3 Grid of action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Chunk actions into rows of 2
                actions.chunked(2).forEachIndexed { rowIndex, rowActions ->
                    AnimatedVisibility(
                        visible = animationStarted,
                        enter = slideInVertically(
                            initialOffsetY = { 100 + (rowIndex * 50) },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeIn(
                            animationSpec = tween(
                                durationMillis = 300,
                                delayMillis = rowIndex * 100
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowActions.forEach { action ->
                                CommandCenterButton(
                                    action = action,
                                    userTier = userTier,
                                    onClick = onActionClick,
                                    notificationBadge = notificationBadges[action] ?: 0,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Add empty spacer if odd number of items in row
                            if (rowActions.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Status message
            AnimatedVisibility(
                visible = animationStarted,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 400,
                        delayMillis = 600
                    )
                )
            ) {
                StatusMessage(
                    userTier = userTier,
                    actions = actions
                )
            }
        }
    }
}

/**
 * Status message showing available actions and coming soon features
 */
@Composable
private fun StatusMessage(
    userTier: UserTier,
    actions: List<SafetyAction>
) {
    val availableCount = actions.count { it.isAvailableForTier(userTier) && it.isImplemented() }
    val comingSoonCount = actions.count { !it.isImplemented() }
    val lockedCount = actions.count { !it.isAvailableForTier(userTier) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = ConstructionColors.WorkZoneBlue.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = ConstructionColors.WorkZoneBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Quick Status",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConstructionColors.OnSurface
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "• $availableCount actions ready to use",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = ConstructionColors.OnSurfaceVariant
                )
                if (comingSoonCount > 0) {
                    Text(
                        text = "• $comingSoonCount features coming soon",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = ConstructionColors.OnSurfaceVariant
                    )
                }
                if (lockedCount > 0) {
                    Text(
                        text = "• $lockedCount actions require higher access level",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = ConstructionColors.OnSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Grid arrangement helper for custom action sets
 */
@Composable
fun CommandCenterCustomGrid(
    userTier: UserTier,
    onActionClick: (SafetyAction) -> Unit,
    columns: Int = 2,
    modifier: Modifier = Modifier,
    actions: List<SafetyAction>,
    notificationBadges: Map<SafetyAction, Int> = emptyMap()
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.chunked(columns).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowActions.forEach { action ->
                    CommandCenterButton(
                        action = action,
                        userTier = userTier,
                        onClick = onActionClick,
                        notificationBadge = notificationBadges[action] ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Fill remaining columns with spacers
                repeat(columns - rowActions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CommandCenterGridPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConstructionColors.SurfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Field Access user
            Text(
                text = "Field Access View",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            CommandCenterGrid(
                userTier = UserTier.FIELD_ACCESS,
                onActionClick = {},
                notificationBadges = mapOf(
                    SafetyAction.VIEW_REPORTS to 3,
                    SafetyAction.OPEN_GALLERY to 7
                )
            )

            // Safety Lead user
            Text(
                text = "Safety Lead View",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            CommandCenterGrid(
                userTier = UserTier.SAFETY_LEAD,
                onActionClick = {},
                notificationBadges = mapOf(
                    SafetyAction.CREATE_PTP to 2,
                    SafetyAction.ASSIGN_TASKS to 5
                )
            )

            // Project Admin user
            Text(
                text = "Project Admin View",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            CommandCenterGrid(
                userTier = UserTier.PROJECT_ADMIN,
                onActionClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CommandCenterCustomGridPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConstructionColors.SurfaceVariant)
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = ConstructionColors.Surface
                )
            ) {
                CommandCenterCustomGrid(
                    userTier = UserTier.PROJECT_ADMIN,
                    onActionClick = {},
                    columns = 3,
                    actions = listOf(
                        SafetyAction.CREATE_PTP,
                        SafetyAction.CAPTURE_PHOTO,
                        SafetyAction.VIEW_REPORTS,
                        SafetyAction.OPEN_GALLERY,
                        SafetyAction.LIVE_DETECTION
                    ),
                    notificationBadges = mapOf(
                        SafetyAction.VIEW_REPORTS to 5
                    )
                )
            }
        }
    }
}
