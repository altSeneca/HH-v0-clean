package com.hazardhawk.ui.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hazardhawk.models.crew.CompanyWorker
import com.hazardhawk.models.crew.WorkerRole
import com.hazardhawk.models.crew.WorkerStatus
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * Worker List Screen
 *
 * Displays all workers in the company with:
 * - Search and filter functionality
 * - Status badges (Active/Inactive)
 * - Quick actions (view, edit, delete)
 * - Floating action button to add new worker
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddWorker: () -> Unit,
    onNavigateToWorkerDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkerViewModel = viewModel()
) {
    val workers by viewModel.workers.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingWorkers.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf<WorkerStatus?>(null) }
    var filterRole by remember { mutableStateOf<WorkerRole?>(null) }

    // Load workers on first composition
    LaunchedEffect(Unit) {
        viewModel.loadWorkers("current_company_id") // TODO: Get from context
    }

    // Filter workers based on search and filters
    val filteredWorkers = remember(workers, searchQuery, filterStatus, filterRole) {
        workers.filter { worker ->
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                worker.workerProfile?.fullName?.contains(searchQuery, ignoreCase = true) == true ||
                worker.employeeNumber.contains(searchQuery, ignoreCase = true)
            }

            val matchesStatus = filterStatus?.let { worker.status == it } ?: true
            val matchesRole = filterRole?.let { worker.role == it } ?: true

            matchesSearch && matchesStatus && matchesRole
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Crew Management")
                        Text(
                            "${filteredWorkers.size} workers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Filter button
                    IconButton(onClick = { /* Show filter dialog */ }) {
                        Icon(Icons.Default.FilterList, "Filter")
                    }
                    // Refresh button
                    IconButton(onClick = { viewModel.refreshWorkers() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConstructionColors.WorkZoneBlue,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddWorker,
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("Add Worker") },
                containerColor = ConstructionColors.SafetyOrange
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by name or employee number...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status filter
                FilterChip(
                    selected = filterStatus == WorkerStatus.ACTIVE,
                    onClick = {
                        filterStatus = if (filterStatus == WorkerStatus.ACTIVE) null else WorkerStatus.ACTIVE
                    },
                    label = { Text("Active Only") },
                    leadingIcon = if (filterStatus == WorkerStatus.ACTIVE) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                    } else null
                )

                // Role filter chip (simplified - can expand to dropdown)
                FilterChip(
                    selected = filterRole != null,
                    onClick = {
                        // Toggle through common roles or show dialog
                        filterRole = when (filterRole) {
                            null -> WorkerRole.LABORER
                            WorkerRole.LABORER -> WorkerRole.FOREMAN
                            WorkerRole.FOREMAN -> WorkerRole.OPERATOR
                            else -> null
                        }
                    },
                    label = { Text(filterRole?.displayName ?: "All Roles") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Worker List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredWorkers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (searchQuery.isNotEmpty()) "No workers found" else "No workers yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isEmpty()) {
                            Button(onClick = onNavigateToAddWorker) {
                                Icon(Icons.Default.Add, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Your First Worker")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredWorkers,
                        key = { it.id }
                    ) { worker ->
                        WorkerListItem(
                            worker = worker,
                            onClick = { onNavigateToWorkerDetails(worker.id) }
                        )
                    }

                    // Bottom padding for FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Error Snackbar
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Show snackbar
                viewModel.clearError()
            }
        }
    }
}

/**
 * Individual Worker List Item Card
 */
@Composable
fun WorkerListItem(
    worker: CompanyWorker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Worker Photo or Initials
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        when (worker.status) {
                            WorkerStatus.ACTIVE -> ConstructionColors.SafetyGreen.copy(alpha = 0.2f)
                            WorkerStatus.INACTIVE -> ConstructionColors.ConcreteGray.copy(alpha = 0.2f)
                            WorkerStatus.TERMINATED -> ConstructionColors.Error.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // TODO: Load photo from worker.workerProfile?.photoUrl
                Text(
                    text = worker.workerProfile?.let {
                        "${it.firstName.firstOrNull()?.uppercase()}${it.lastName.firstOrNull()?.uppercase()}"
                    } ?: "??",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (worker.status) {
                        WorkerStatus.ACTIVE -> ConstructionColors.SafetyGreen
                        WorkerStatus.INACTIVE -> ConstructionColors.ConcreteGray
                        WorkerStatus.TERMINATED -> ConstructionColors.Error
                    }
                )
            }

            // Worker Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = worker.workerProfile?.fullName ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = worker.role.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = worker.employeeNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (worker.status) {
                        WorkerStatus.ACTIVE -> ConstructionColors.SafetyGreen.copy(alpha = 0.1f)
                        WorkerStatus.INACTIVE -> ConstructionColors.Warning.copy(alpha = 0.1f)
                        WorkerStatus.TERMINATED -> ConstructionColors.Error.copy(alpha = 0.1f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (worker.status) {
                                        WorkerStatus.ACTIVE -> ConstructionColors.SafetyGreen
                                        WorkerStatus.INACTIVE -> ConstructionColors.Warning
                                        WorkerStatus.TERMINATED -> ConstructionColors.Error
                                    }
                                )
                        )
                        Text(
                            text = worker.status.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (worker.status) {
                                WorkerStatus.ACTIVE -> ConstructionColors.SafetyGreen
                                WorkerStatus.INACTIVE -> ConstructionColors.Warning
                                WorkerStatus.TERMINATED -> ConstructionColors.Error
                            }
                        )
                    }
                }
            }

            // Arrow Icon
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
