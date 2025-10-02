package com.hazardhawk.ui.safety.ptp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hazardhawk.domain.models.ptp.PreTaskPlan
import com.hazardhawk.domain.models.ptp.PtpStatus
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * PTP List Screen showing all Pre-Task Plans with filtering and status indicators.
 * Provides quick access to create new PTPs or view/edit existing ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PTPListScreen(
    viewModel: PTPListViewModel = koinViewModel(),
    onNavigateToCreate: () -> Unit,
    onNavigateToPTP: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val ptps by viewModel.ptps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pre-Task Plans") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create PTP")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    ErrorView(
                        error = error!!,
                        onRetry = { viewModel.loadPTPs() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                ptps.isEmpty() -> {
                    EmptyStateView(
                        onCreateFirst = onNavigateToCreate,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    PTPListContent(
                        ptps = ptps,
                        onPTPClick = onNavigateToPTP,
                        onDeletePTP = { ptpId -> viewModel.deletePTP(ptpId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PTPListContent(
    ptps: List<PreTaskPlan>,
    onPTPClick: (String) -> Unit,
    onDeletePTP: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(ptps, key = { it.id }) { ptp ->
            PTPListItem(
                ptp = ptp,
                onClick = { onPTPClick(ptp.id) },
                onDelete = { onDeletePTP(ptp.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PTPListItem(
    ptp: PreTaskPlan,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with work type and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ptp.workType,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = ptp.status)
            }

            // Work scope description
            Text(
                text = ptp.workScope,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Crew size
                    ptp.crewSize?.let { crewSize ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$crewSize workers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Date
                    Text(
                        text = formatTimestamp(ptp.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Delete button (only for drafts)
                if (ptp.status == PtpStatus.DRAFT) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Hazard count indicator if content available
            ptp.aiGeneratedContent?.hazards?.let { hazards ->
                if (hazards.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "${hazards.size} hazards identified",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete PTP?") },
            text = { Text("Are you sure you want to delete this Pre-Task Plan? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusBadge(status: PtpStatus) {
    val (color, label, icon) = when (status) {
        PtpStatus.DRAFT -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "Draft",
            Icons.Default.Edit
        )
        PtpStatus.APPROVED -> Triple(
            MaterialTheme.colorScheme.primary,
            "Approved",
            Icons.Default.CheckCircle
        )
        PtpStatus.SUBMITTED -> Triple(
            MaterialTheme.colorScheme.tertiary,
            "Submitted",
            Icons.Default.Send
        )
        PtpStatus.ARCHIVED -> Triple(
            MaterialTheme.colorScheme.outline,
            "Archived",
            Icons.Default.Archive
        )
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun EmptyStateView(
    onCreateFirst: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "No Pre-Task Plans yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Create your first PTP to document safety procedures and hazards for construction work",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onCreateFirst,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create First PTP")
        }
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            "Error loading PTPs",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val timestampCal = Calendar.getInstance().apply { time = date }

    return when {
        // Today
        now.get(Calendar.YEAR) == timestampCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == timestampCal.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("'Today at' h:mm a", Locale.US).format(date)
        }
        // This year
        now.get(Calendar.YEAR) == timestampCal.get(Calendar.YEAR) -> {
            SimpleDateFormat("MMM dd 'at' h:mm a", Locale.US).format(date)
        }
        // Previous years
        else -> {
            SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date)
        }
    }
}
