package com.hazardhawk.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Safety Navigation Bar - Bottom navigation for HazardHawk
 *
 * Provides 5-item navigation:
 * - Home: Dashboard overview
 * - Capture: Camera interface
 * - Safety: Safety Hub (PTPs, Toolbox Talks, etc.)
 * - Gallery: Photo gallery
 * - Profile: Settings and user profile
 *
 * Features:
 * - Badge support for notifications
 * - Pill-shaped selected indicator
 * - Material3 NavigationBar (64dp height)
 * - Construction-friendly design with high contrast
 */
@Composable
fun SafetyNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    notifications: NavigationNotifications = NavigationNotifications()
) {
    NavigationBar(
        modifier = modifier.height(64.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationItem.entries.forEach { item ->
            val isSelected = currentRoute.startsWith(item.route)
            val badgeCount = when (item) {
                NavigationItem.HOME -> notifications.homeCount
                NavigationItem.CAPTURE -> 0
                NavigationItem.SAFETY -> notifications.safetyCount
                NavigationItem.GALLERY -> notifications.galleryCount
                NavigationItem.PROFILE -> notifications.profileCount
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    if (badgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(
                                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Navigation items with routes and icons
 */
enum class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME(
        route = "home",
        label = "Home",
        icon = Icons.Default.Home
    ),
    CAPTURE(
        route = "clear_camera",
        label = "Capture",
        icon = Icons.Default.CameraAlt
    ),
    SAFETY(
        route = "safety",
        label = "Safety",
        icon = Icons.Default.Shield
    ),
    GALLERY(
        route = "gallery",
        label = "Gallery",
        icon = Icons.Default.PhotoLibrary
    ),
    PROFILE(
        route = "settings",
        label = "Profile",
        icon = Icons.Default.Person
    )
}

/**
 * Notification badge counts for navigation items
 */
data class NavigationNotifications(
    val homeCount: Int = 0,
    val safetyCount: Int = 0,
    val galleryCount: Int = 0,
    val profileCount: Int = 0
)
