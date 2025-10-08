package com.hazardhawk.ui.projects

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazardhawk.models.crew.Project
import com.hazardhawk.ui.theme.ConstructionColors
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel

/**
 * Project List Screen - Centralized project management
 *
 * Features:
 * - List all company projects with status badges
 * - Search/filter projects by status
 * - Quick view of project details (client, location, dates)
 * - Create new project FAB
 * - Navigate to project edit screen
 * - Material 3 design with construction-friendly UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onNavigateToProjectForm: (projectId: String?) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = koinViewModel()
) {
    // Collect state from ViewModel
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()

    // Filter state
    var showFilterMenu by remember { mutableStateOf(false) }

    // Error snackbar
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Projects",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Filter button
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }

                    // Filter dropdown menu
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Projects") },
                            onClick = {
                                viewModel.filterByStatus(null)
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (selectedStatus == null) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Active") },
                            onClick = {
                                viewModel.filterByStatus("active")
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (selectedStatus == "active") {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Completed") },
                            onClick = {
                                viewModel.filterByStatus("completed")
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (selectedStatus == "completed") {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("On Hold") },
                            onClick = {
                                viewModel.filterByStatus("on_hold")
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (selectedStatus == "on_hold") {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }

                    // Refresh button
                    IconButton(onClick = { viewModel.refreshProjects() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConstructionColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToProjectForm(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Project") },
                containerColor = ConstructionColors.Primary,
                contentColor = Color.White
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    // Loading state
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                projects.isEmpty() -> {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = ConstructionColors.OnSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedStatus != null) "No ${selectedStatus} projects" else "No projects yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = ConstructionColors.OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create your first project to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ConstructionColors.OnSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                else -> {
                    // Project list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(projects, key = { it.id }) { project ->
                            ProjectCard(
                                project = project,
                                onClick = { onNavigateToProjectForm(project.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Project Card - Individual project item in the list
 */
@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Project name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (project.projectNumber != null) {
                        Text(
                            text = "Project #${project.projectNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ConstructionColors.OnSurfaceVariant
                        )
                    }
                </Column>

                Spacer(modifier = Modifier.width(12.dp))

                // Status badge
                ProjectStatusBadge(status = project.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Client information
            if (project.clientName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ConstructionColors.OnSurfaceVariant
                    )
                    Text(
                        text = project.clientName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ConstructionColors.OnSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Location
            if (project.city != null || project.streetAddress != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ConstructionColors.OnSurfaceVariant
                    )
                    Text(
                        text = buildString {
                            if (project.city != null) {
                                append(project.city)
                                if (project.state != null) append(", ${project.state}")
                            } else if (project.streetAddress != null) {
                                append(project.streetAddress)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ConstructionColors.OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Dates
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ConstructionColors.OnSurfaceVariant
                )
                Text(
                    text = buildString {
                        append(project.startDate.toString())
                        if (project.endDate != null) {
                            append(" - ${project.endDate}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ConstructionColors.OnSurfaceVariant
                )
            }

            // General contractor (if specified)
            if (project.generalContractor != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ConstructionColors.OnSurfaceVariant
                    )
                    Text(
                        text = "GC: ${project.generalContractor}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ConstructionColors.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Project Status Badge
 */
@Composable
private fun ProjectStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, displayText) = when (status.lowercase()) {
        "active" -> Triple(
            Color(0xFF4CAF50).copy(alpha = 0.15f),
            Color(0xFF2E7D32),
            "Active"
        )
        "completed" -> Triple(
            Color(0xFF2196F3).copy(alpha = 0.15f),
            Color(0xFF1565C0),
            "Completed"
        )
        "on_hold" -> Triple(
            Color(0xFFFFA500).copy(alpha = 0.15f),
            Color(0xFFE65100),
            "On Hold"
        )
        "cancelled" -> Triple(
            Color(0xFFF44336).copy(alpha = 0.15f),
            Color(0xFFC62828),
            "Cancelled"
        )
        else -> Triple(
            Color.Gray.copy(alpha = 0.15f),
            Color.DarkGray,
            status
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
