package com.hazardhawk.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.hazardhawk.ui.theme.ConstructionColors
import com.hazardhawk.ui.home.components.*
import com.hazardhawk.ui.home.viewmodels.DashboardViewModel
import com.hazardhawk.models.dashboard.SafetyAction
import com.hazardhawk.models.dashboard.UserTier
import com.hazardhawk.data.repositories.dashboard.UserProfile

/**
 * Redesigned Home Dashboard - Safety Command Center
 *
 * Layout:
 * - StartupAnimation (4-second sequence)
 * - HeroStatusBar (personalized greeting, time-of-day gradient)
 * - CommandCenterGrid (2x3 grid of primary actions)
 * - ActivityFeedList (recent activity with pull-to-refresh)
 *
 * Features:
 * - Integrates DashboardViewModel for state management
 * - Collects StateFlows for user profile, site conditions, activities
 * - Handles action clicks with permission validation
 * - Role-based button visibility
 * - Construction-optimized design with 60dp+ touch targets
 */
@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToPTP: () -> Unit,
    onNavigateToSafety: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = koinViewModel()
) {
    // Collect state from ViewModel
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val siteConditions by viewModel.siteConditions.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // Mock activity feed (to be implemented with actual repository)
    val activities = remember { emptyList<com.hazardhawk.models.dashboard.ActivityFeedItem>() }

    // Show error snackbar if needed
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        StartupAnimation {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                ConstructionColors.Surface,
                                ConstructionColors.SurfaceVariant
                            )
                        )
                    )
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Status Bar
                item {
                    HeroStatusBar(
                        userName = userProfile?.fullName ?: "User",
                        userTier = userProfile?.userTier ?: UserTier.FIELD_ACCESS,
                        siteConditions = siteConditions
                    )
                }

                // Command Center Grid
                item {
                    CommandCenterGrid(
                        userTier = userProfile?.userTier ?: UserTier.FIELD_ACCESS,
                        onActionClick = { action ->
                            // Validate permissions in ViewModel
                            viewModel.onActionClick(action)

                            // Navigate based on action
                            when (action) {
                                SafetyAction.CAPTURE_PHOTO -> onNavigateToCamera()
                                SafetyAction.OPEN_GALLERY -> onNavigateToGallery()
                                SafetyAction.CREATE_PTP -> onNavigateToPTP()
                                SafetyAction.VIEW_REPORTS -> onNavigateToSafety()
                                SafetyAction.CREATE_TOOLBOX_TALK,
                                SafetyAction.ASSIGN_TASKS,
                                SafetyAction.REPORT_INCIDENT,
                                SafetyAction.START_PRE_SHIFT,
                                SafetyAction.MANAGE_CREW -> {
                                    // Coming soon features - handled by ViewModel
                                }
                                SafetyAction.LIVE_DETECTION -> onNavigateToCamera()
                            }
                        }
                    )
                }

                // Activity Feed Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            color = ConstructionColors.OnSurface
                        )

                        if (activities.isNotEmpty()) {
                            TextButton(onClick = { /* Navigate to full activity view */ }) {
                                Text("View All")
                            }
                        }
                    }
                }

                // Activity Feed List
                item {
                    ActivityFeedList(
                        items = activities,
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshData() },
                        onItemClick = { item ->
                            // Handle activity item clicks
                            // TODO: Navigate to appropriate screen based on item type
                        },
                        modifier = Modifier.heightIn(min = 200.dp, max = 500.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            onNavigateToCamera = {},
            onNavigateToGallery = {},
            onNavigateToPTP = {},
            onNavigateToSafety = {},
            onNavigateToSettings = {}
        )
    }
}