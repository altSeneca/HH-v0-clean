package com.hazardhawk.ui.crew

import androidx.compose.foundation.background
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
import com.hazardhawk.models.crew.WorkerCertification
import com.hazardhawk.models.crew.WorkerStatus
import com.hazardhawk.ui.theme.ConstructionColors
import kotlinx.datetime.LocalDate

/**
 * Worker Detail Screen
 *
 * Displays full worker information including:
 * - Worker profile (photo, name, contact info)
 * - Employment details (role, hire date, employee number)
 * - Certifications (with status badges)
 * - Crew assignments
 * - Action buttons (edit, deactivate, etc.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDetailScreen(
    workerId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkerViewModel = viewModel()
) {
    val worker by viewModel.selectedWorker.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeactivateDialog by remember { mutableStateOf(false) }

    // Load worker details on first composition
    LaunchedEffect(workerId) {
        viewModel.loadWorkerDetails(workerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Worker Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Edit Button
                    IconButton(onClick = { onNavigateToEdit(workerId) }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    // More Options Menu
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Deactivate Worker") },
                            onClick = {
                                showOptionsMenu = false
                                showDeactivateDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Block, null, tint = ConstructionColors.Warning)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Worker") },
                            onClick = {
                                showOptionsMenu = false
                                // TODO: Show delete confirmation
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = ConstructionColors.Error)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConstructionColors.WorkZoneBlue,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (worker == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = ConstructionColors.Error
                    )
                    Text(
                        uiState.error ?: "Worker not found",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            WorkerDetailContent(
                worker = worker!!,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

    // Deactivate Confirmation Dialog
    if (showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text("Deactivate Worker?") },
            text = {
                Text("This will prevent ${worker?.workerProfile?.fullName} from being assigned to new tasks. Their past work history will be preserved.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: Call viewModel.deactivateWorker(workerId)
                        showDeactivateDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ConstructionColors.Warning
                    )
                ) {
                    Text("Deactivate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateDialog = false }) {
                    Text("Cancel")
                }
            },
            icon = {
                Icon(Icons.Default.Warning, null, tint = ConstructionColors.Warning)
            }
        )
    }
}

@Composable
fun WorkerDetailContent(
    worker: CompanyWorker,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card with Photo and Basic Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (worker.status) {
                        WorkerStatus.ACTIVE -> ConstructionColors.SafetyGreen.copy(alpha = 0.1f)
                        WorkerStatus.INACTIVE -> ConstructionColors.Warning.copy(alpha = 0.1f)
                        WorkerStatus.TERMINATED -> ConstructionColors.Error.copy(alpha = 0.1f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Worker Photo
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        // TODO: Load photo from worker.workerProfile?.photoUrl
                        Text(
                            text = worker.workerProfile?.let {
                                "${it.firstName.firstOrNull()?.uppercase()}${it.lastName.firstOrNull()?.uppercase()}"
                            } ?: "??",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (worker.status) {
                                WorkerStatus.ACTIVE -> ConstructionColors.SafetyGreen
                                WorkerStatus.INACTIVE -> ConstructionColors.Warning
                                WorkerStatus.TERMINATED -> ConstructionColors.Error
                            }
                        )
                    }

                    // Name
                    Text(
                        text = worker.workerProfile?.fullName ?: "Unknown",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Role
                    Text(
                        text = worker.role.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Status Badge
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = when (worker.status) {
                            WorkerStatus.ACTIVE -> ConstructionColors.SafetyGreen
                            WorkerStatus.INACTIVE -> ConstructionColors.Warning
                            WorkerStatus.TERMINATED -> ConstructionColors.Error
                        }
                    ) {
                        Text(
                            text = worker.status.name,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Contact Information
        item {
            SectionCard(title = "Contact Information") {
                if (worker.workerProfile?.phone != null) {
                    InfoRow(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = worker.workerProfile.phone
                    )
                }
                if (worker.workerProfile?.email != null) {
                    InfoRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = worker.workerProfile.email
                    )
                }
            }
        }

        // Employment Details
        item {
            SectionCard(title = "Employment Details") {
                InfoRow(
                    icon = Icons.Default.Badge,
                    label = "Employee Number",
                    value = worker.employeeNumber
                )
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Hire Date",
                    value = worker.hireDate.toString()
                )
                worker.hourlyRate?.let { rate ->
                    InfoRow(
                        icon = Icons.Default.AttachMoney,
                        label = "Hourly Rate",
                        value = "$${"%.2f".format(rate)}"
                    )
                }
            }
        }

        // Certifications Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Certifications",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { /* Navigate to add certification */ }) {
                    Icon(Icons.Default.Add, "Add Certification")
                }
            }
        }

        // Certifications List
        val certifications = worker.certifications
        if (certifications.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ConstructionColors.SurfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            "No certifications on file",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { /* Navigate to add certification */ }) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Certification")
                        }
                    }
                }
            }
        } else {
            items(certifications) { certification ->
                CertificationCard(certification = certification)
            }
        }

        // Crew Assignments Section
        item {
            Text(
                "Crew Assignments",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        val crewMemberships = worker.crews
        if (crewMemberships.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ConstructionColors.SurfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Group,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            "Not assigned to any crew",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // TODO: Show crew assignments
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Reusable Section Card
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Info Row Component
 */
@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = ConstructionColors.WorkZoneBlue,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Certification Card
 */
@Composable
fun CertificationCard(
    certification: WorkerCertification,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                certification.isExpired -> ConstructionColors.Error.copy(alpha = 0.1f)
                certification.isExpiringSoon -> ConstructionColors.Warning.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Icon(
                when {
                    certification.isExpired -> Icons.Default.Cancel
                    certification.isExpiringSoon -> Icons.Default.Warning
                    certification.isValid -> Icons.Default.CheckCircle
                    else -> Icons.Default.HourglassEmpty
                },
                contentDescription = null,
                tint = when {
                    certification.isExpired -> ConstructionColors.Error
                    certification.isExpiringSoon -> ConstructionColors.Warning
                    certification.isValid -> ConstructionColors.SafetyGreen
                    else -> ConstructionColors.ConcreteGray
                },
                modifier = Modifier.size(40.dp)
            )

            // Certification Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = certification.certificationType?.name ?: "Unknown Certification",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                certification.certificationNumber?.let { number ->
                    Text(
                        text = "# $number",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                certification.expirationDate?.let { expDate ->
                    Text(
                        text = "Expires: $expDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            certification.isExpired -> ConstructionColors.Error
                            certification.isExpiringSoon -> ConstructionColors.Warning
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // View Document Button
            IconButton(onClick = { /* View document */ }) {
                Icon(Icons.Default.Visibility, "View Document")
            }
        }
    }
}
