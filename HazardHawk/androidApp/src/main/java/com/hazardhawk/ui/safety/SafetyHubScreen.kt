package com.hazardhawk.ui.safety

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazardhawk.models.dashboard.SafetyAction

/**
 * Safety Hub Screen - Central hub for all safety features
 *
 * Displays feature cards for:
 * - Pre-Task Plans (PTPs) - Functional
 * - Toolbox Talks - Coming Soon
 * - Incident Reports - Coming Soon
 * - Pre-Shift Meetings - Coming Soon
 *
 * Only PTPs are currently functional. Other features show "Coming Soon" badge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyHubScreen(
    onNavigateToPTPs: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safetyFeatures = remember {
        listOf(
            SafetyFeatureItem(
                title = "Pre-Task Plans",
                description = "Plan work tasks and identify hazards before starting work",
                icon = Icons.Default.Assignment,
                statusCount = 0, // Can be connected to actual count from database
                comingSoon = false,
                enabled = true,
                action = SafetyAction.CREATE_PTP
            ),
            SafetyFeatureItem(
                title = "Toolbox Talks",
                description = "Generate and conduct weekly safety discussions with your crew",
                icon = Icons.Default.Construction,
                statusCount = 0,
                comingSoon = true,
                enabled = false,
                action = SafetyAction.CREATE_TOOLBOX_TALK
            ),
            SafetyFeatureItem(
                title = "Incident Reports",
                description = "Document and track safety incidents for OSHA compliance",
                icon = Icons.Default.Warning,
                statusCount = 0,
                comingSoon = true,
                enabled = false,
                action = SafetyAction.REPORT_INCIDENT
            ),
            SafetyFeatureItem(
                title = "Pre-Shift Meetings",
                description = "Start daily safety briefings with your team",
                icon = Icons.Default.Schedule,
                statusCount = 0,
                comingSoon = true,
                enabled = false,
                action = SafetyAction.START_PRE_SHIFT
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Safety Hub",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage your safety documentation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header section
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Safety Features",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select a feature to manage your safety documentation and compliance requirements",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Feature cards
            items(safetyFeatures) { feature ->
                SafetyFeatureCard(
                    title = feature.title,
                    description = feature.description,
                    icon = feature.icon,
                    statusCount = feature.statusCount,
                    comingSoon = feature.comingSoon,
                    enabled = feature.enabled,
                    onClick = {
                        when (feature.action) {
                            SafetyAction.CREATE_PTP -> onNavigateToPTPs()
                            else -> {
                                // Other features are not yet implemented
                            }
                        }
                    }
                )
            }

            // Footer info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Coming Soon Features",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Additional safety features are under development and will be released in upcoming updates. Pre-Task Plans are fully functional now.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data class representing a safety feature in the hub
 */
private data class SafetyFeatureItem(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val statusCount: Int,
    val comingSoon: Boolean,
    val enabled: Boolean,
    val action: SafetyAction
)
