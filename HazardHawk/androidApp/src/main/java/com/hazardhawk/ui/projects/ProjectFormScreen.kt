package com.hazardhawk.ui.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazardhawk.models.crew.CompanyWorker
import com.hazardhawk.ui.theme.ConstructionColors
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel

/**
 * Project Form Screen - Create or edit a project
 *
 * Features:
 * - Comprehensive form with all project fields
 * - Form validation with inline error messages
 * - Date pickers for start/end dates
 * - Worker selection dropdowns for PM and Superintendent
 * - Auto-populate company info from centralized source
 * - Material 3 design with construction-friendly 60dp+ touch targets
 * - Scroll support for small screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFormScreen(
    projectId: String? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = koinViewModel()
) {
    // Collect state
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val validationErrors by viewModel.validationErrors.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val availableWorkers by viewModel.availableWorkers.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // Load project if editing
    LaunchedEffect(projectId) {
        if (projectId != null) {
            viewModel.loadProject(projectId)
        } else {
            viewModel.initializeNewProject()
        }
    }

    // Success snackbar
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

    // Date picker states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (projectId != null) "Edit Project" else "New Project",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConstructionColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
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
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Project Information Section
                        item {
                            SectionHeader(
                                title = "Project Information",
                                icon = Icons.Default.Business
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = formState.name,
                                onValueChange = { viewModel.updateFormField("name", it) },
                                label = { Text("Project Name *") },
                                isError = validationErrors.containsKey("name"),
                                supportingText = {
                                    validationErrors["name"]?.let { error ->
                                        Text(error, color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp),
                                singleLine = false,
                                maxLines = 2
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = formState.projectNumber,
                                onValueChange = { viewModel.updateFormField("projectNumber", it) },
                                label = { Text("Project Number") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp),
                                singleLine = true
                            )
                        }

                        // Dates Section
                        item {
                            SectionHeader(
                                title = "Project Schedule",
                                icon = Icons.Default.CalendarToday
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Start Date
                                OutlinedButton(
                                    onClick = { showStartDatePicker = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 60.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (validationErrors.containsKey("startDate"))
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                        else Color.Transparent
                                    )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Start Date *",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (validationErrors.containsKey("startDate"))
                                                MaterialTheme.colorScheme.error
                                            else ConstructionColors.OnSurfaceVariant
                                        )
                                        Text(
                                            formState.startDate?.toString() ?: "Select date",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                // End Date
                                OutlinedButton(
                                    onClick = { showEndDatePicker = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 60.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "End Date",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ConstructionColors.OnSurfaceVariant
                                        )
                                        Text(
                                            formState.endDate?.toString() ?: "Select date",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                            validationErrors["startDate"]?.let { error ->
                                Text(
                                    error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }

                        // Client Information Section
                        item {
                            SectionHeader(
                                title = "Client Information",
                                icon = Icons.Default.Person
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = formState.clientName,
                                onValueChange = { viewModel.updateFormField("clientName", it) },
                                label = { Text("Client Name") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = formState.clientContact,
                                onValueChange = { viewModel.updateFormField("clientContact", it) },
                                label = { Text("Client Contact Person") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp),
                                singleLine = true
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = formState.clientPhone,
                                    onValueChange = { viewModel.updateFormField("clientPhone", it) },
                                    label = { Text("Phone") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 60.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = formState.clientEmail,
                                    onValueChange = { viewModel.updateFormField("clientEmail", it) },
                                    label = { Text("Email") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 60.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    isError = validationErrors.containsKey("clientEmail"),
                                    supportingText = {
                                        validationErrors["clientEmail"]?.let { error ->
                                            Text(error, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                )
                            }
                        }

                        // Location Section
                        item {
                            SectionHeader(
                                title = "Project Location",
                                icon = Icons.Default.LocationOn
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = formState.streetAddress,
                                onValueChange = { viewModel.updateFormField("streetAddress", it) },
                                label = { Text("Street Address") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp),
                                singleLine = false,
                                maxLines = 2
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = formState.city,
                                    onValueChange = { viewModel.updateFormField("city", it) },
                                    label = { Text("City") },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .heightIn(min = 60.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = formState.state,
                                    onValueChange = { viewModel.updateFormField("state", it) },
                                    label = { Text("State") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 60.dp),
                                    singleLine = true
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = formState.zip,
                                onValueChange = { viewModel.updateFormField("zip", it) },
                                label = { Text("ZIP Code") },
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .heightIn(min = 60.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = validationErrors.containsKey("zip"),
                                supportingText = {
                                    validationErrors["zip"]?.let { error ->
                                        Text(error, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                        }

                        // General Contractor Section
                        item {
                            SectionHeader(
                                title = "Project Team",
                                icon = Icons.Default.Engineering
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = formState.generalContractor,
                                onValueChange = { viewModel.updateFormField("generalContractor", it) },
                                label = { Text("General Contractor") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp),
                                singleLine = true
                            )
                        }

                        // Project Manager Dropdown
                        item {
                            var expanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = formState.projectManagerName ?: "Select Project Manager",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Project Manager") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 60.dp)
                                        .menuAnchor(),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    // None option
                                    DropdownMenuItem(
                                        text = { Text("None") },
                                        onClick = {
                                            viewModel.updateFormField("projectManagerId", null)
                                            expanded = false
                                        }
                                    )

                                    // Worker options
                                    availableWorkers.forEach { worker ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(worker.workerProfile?.fullName ?: "Unknown")
                                                    Text(
                                                        worker.role.displayName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = ConstructionColors.OnSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.updateFormField("projectManagerId", worker.id)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Superintendent Dropdown
                        item {
                            var expanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = formState.superintendentName ?: "Select Superintendent",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Superintendent") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 60.dp)
                                        .menuAnchor(),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    // None option
                                    DropdownMenuItem(
                                        text = { Text("None") },
                                        onClick = {
                                            viewModel.updateFormField("superintendentId", null)
                                            expanded = false
                                        }
                                    )

                                    // Worker options
                                    availableWorkers.forEach { worker ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(worker.workerProfile?.fullName ?: "Unknown")
                                                    Text(
                                                        worker.role.displayName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = ConstructionColors.OnSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.updateFormField("superintendentId", worker.id)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Save Button
                        item {
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    viewModel.saveProject(
                                        onSuccess = { onNavigateBack() }
                                    )
                                },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ConstructionColors.Primary,
                                    contentColor = Color.White
                                )
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (projectId != null) "Update Project" else "Create Project",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Required fields note
                        item {
                            Text(
                                "* Required fields",
                                style = MaterialTheme.typography.bodySmall,
                                color = ConstructionColors.OnSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Date pickers (placeholders - would use actual date picker library)
    // For now, these are simplified
    if (showStartDatePicker) {
        // TODO: Implement actual date picker dialog
        AlertDialog(
            onDismissRequest = { showStartDatePicker = false },
            title = { Text("Select Start Date") },
            text = { Text("Date picker implementation needed") },
            confirmButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showEndDatePicker) {
        // TODO: Implement actual date picker dialog
        AlertDialog(
            onDismissRequest = { showEndDatePicker = false },
            title = { Text("Select End Date") },
            text = { Text("Date picker implementation needed") },
            confirmButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Section Header - Visual separator for form sections
 */
@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ConstructionColors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ConstructionColors.Primary
        )
    }
    Divider(
        color = ConstructionColors.Primary.copy(alpha = 0.3f),
        thickness = 2.dp
    )
}
