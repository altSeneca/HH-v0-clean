package com.hazardhawk.ui.crew

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hazardhawk.models.crew.WorkerRole
import com.hazardhawk.ui.theme.ConstructionColors
import kotlinx.datetime.LocalDate

/**
 * Add Worker Screen - Multi-step form
 *
 * Step 0: Basic Information (name, email, phone, role, hire date, employee number)
 * Step 1: Photo Upload (camera or gallery)
 * Step 2: Certifications Upload (multiple files, optional)
 *
 * Features:
 * - Form validation with real-time feedback
 * - Progress indicator
 * - Large touch targets (56dp minimum)
 * - Construction-friendly design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkerScreen(
    onNavigateBack: () -> Unit,
    onWorkerCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkerViewModel = viewModel()
) {
    val formState by viewModel.addWorkerState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val validationErrors by viewModel.validationErrors.collectAsStateWithLifecycle()

    val canProceed = viewModel.canProceedToNextStep()

    // Reset form when navigating back
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetForm()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Add New Worker")
                        Text(
                            "Step ${formState.currentStep + 1} of 3",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (formState.currentStep > 0) {
                            viewModel.previousStep()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConstructionColors.WorkZoneBlue,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress Indicator
            LinearProgressIndicator(
                progress = { (formState.currentStep + 1) / 3f },
                modifier = Modifier.fillMaxWidth(),
                color = ConstructionColors.SafetyOrange
            )

            // Form Content
            AnimatedContent(
                targetState = formState.currentStep,
                modifier = Modifier.weight(1f),
                label = "step_content"
            ) { step ->
                when (step) {
                    0 -> BasicInfoStep(
                        formState = formState,
                        viewModel = viewModel,
                        validationErrors = validationErrors
                    )
                    1 -> PhotoUploadStep(
                        formState = formState,
                        viewModel = viewModel
                    )
                    2 -> CertificationsUploadStep(
                        formState = formState,
                        viewModel = viewModel
                    )
                }
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Previous/Cancel Button
                if (formState.currentStep > 0) {
                    OutlinedButton(
                        onClick = { viewModel.previousStep() },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Previous")
                    }
                }

                // Next/Create Button
                Button(
                    onClick = {
                        if (formState.currentStep < 2) {
                            viewModel.nextStep()
                        } else {
                            // Final step - create worker
                            viewModel.createWorker(
                                companyId = "current_company_id", // TODO: Get from context
                                onSuccess = onWorkerCreated,
                                onError = { /* Error handled in UI state */ }
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = when (formState.currentStep) {
                        0 -> canProceed && !uiState.isSaving
                        1, 2 -> !uiState.isSaving
                        else -> false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ConstructionColors.SafetyOrange
                    )
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            when (formState.currentStep) {
                                2 -> "Create Worker"
                                else -> "Next"
                            }
                        )
                        if (formState.currentStep < 2) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, null)
                        }
                    }
                }
            }
        }

        // Error Dialog
        uiState.error?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                },
                icon = { Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

/**
 * Step 0: Basic Information
 */
@Composable
fun BasicInfoStep(
    formState: AddWorkerFormState,
    viewModel: WorkerViewModel,
    validationErrors: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Basic Information",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Enter the worker's basic details. All fields marked with * are required.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Validation Errors
        if (validationErrors.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = ConstructionColors.Error.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            null,
                            tint = ConstructionColors.Error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Please fix the following:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionColors.Error
                        )
                    }
                    validationErrors.forEach { error ->
                        Text(
                            "• $error",
                            style = MaterialTheme.typography.bodySmall,
                            color = ConstructionColors.Error
                        )
                    }
                }
            }
        }

        // Employee Number
        OutlinedTextField(
            value = formState.employeeNumber,
            onValueChange = { viewModel.updateEmployeeNumber(it) },
            label = { Text("Employee Number *") },
            placeholder = { Text("E-001") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Badge, null) },
            singleLine = true
        )

        // First Name
        OutlinedTextField(
            value = formState.firstName,
            onValueChange = { viewModel.updateFirstName(it) },
            label = { Text("First Name *") },
            placeholder = { Text("John") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, null) },
            singleLine = true
        )

        // Last Name
        OutlinedTextField(
            value = formState.lastName,
            onValueChange = { viewModel.updateLastName(it) },
            label = { Text("Last Name *") },
            placeholder = { Text("Doe") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, null) },
            singleLine = true
        )

        // Phone Number
        OutlinedTextField(
            value = formState.phone,
            onValueChange = { viewModel.updatePhone(it) },
            label = { Text("Phone Number *") },
            placeholder = { Text("+1-555-0100") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Phone, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            supportingText = { Text("Format: +1-XXX-XXXX") }
        )

        // Email (Optional)
        OutlinedTextField(
            value = formState.email,
            onValueChange = { viewModel.updateEmail(it) },
            label = { Text("Email (Optional)") },
            placeholder = { Text("john.doe@example.com") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Email, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        // Role Dropdown
        RoleDropdown(
            selectedRole = formState.role,
            onRoleSelect = { viewModel.updateRole(it) }
        )

        // Hire Date Picker
        // TODO: Implement date picker
        OutlinedTextField(
            value = formState.hireDate.toString(),
            onValueChange = { },
            label = { Text("Hire Date") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
            readOnly = true,
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
        )

        // Hourly Rate (Optional)
        OutlinedTextField(
            value = formState.hourlyRate,
            onValueChange = { viewModel.updateHourlyRate(it) },
            label = { Text("Hourly Rate (Optional)") },
            placeholder = { Text("25.00") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            prefix = { Text("$") }
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

/**
 * Step 1: Photo Upload
 */
@Composable
fun PhotoUploadStep(
    formState: AddWorkerFormState,
    viewModel: WorkerViewModel,
    modifier: Modifier = Modifier
) {
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Photo saved to URI, already set in formState
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updatePhotoUri(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Worker Photo",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Add a photo to help identify this worker. This is optional but recommended for safety documentation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Photo Preview or Upload Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            colors = CardDefaults.cardColors(
                containerColor = ConstructionColors.SurfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (formState.photoUri != null) {
                    // TODO: Display photo from URI
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .border(4.dp, ConstructionColors.SafetyGreen, CircleShape)
                                .background(ConstructionColors.Surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = ConstructionColors.SafetyGreen
                            )
                        }
                        Text(
                            "Photo Selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = ConstructionColors.SafetyGreen
                        )
                        TextButton(onClick = { viewModel.updatePhotoUri(null) }) {
                            Text("Remove Photo")
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            "No photo selected",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Upload Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    // TODO: Create temp URI for camera
                    // cameraLauncher.launch(tempUri)
                },
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Camera")
            }

            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gallery")
            }
        }

        Surface(
            color = ConstructionColors.Info.copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint = ConstructionColors.Info,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "This photo will be used for worker identification in safety documents, PTPs, and incident reports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Step 2: Certifications Upload
 */
@Composable
fun CertificationsUploadStep(
    formState: AddWorkerFormState,
    viewModel: WorkerViewModel,
    modifier: Modifier = Modifier
) {
    // Multiple file picker
    val certificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            viewModel.addCertificationUri(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Certifications",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Upload certification documents (OSHA 10/30, CPR, Forklift, etc.). You can add these later if needed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Upload Button
        OutlinedButton(
            onClick = { certificationLauncher.launch("image/*,application/pdf") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Upload, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload Certifications")
        }

        // Uploaded Certifications List
        if (formState.certificationUris.isNotEmpty()) {
            Text(
                "${formState.certificationUris.size} certification(s) uploaded",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            formState.certificationUris.forEach { uri ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Description,
                            null,
                            tint = ConstructionColors.WorkZoneBlue
                        )
                        Text(
                            uri.lastPathSegment ?: "Document",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(onClick = { viewModel.removeCertificationUri(uri) }) {
                            Icon(Icons.Default.Close, "Remove", tint = ConstructionColors.Error)
                        }
                    }
                }
            }
        } else {
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
                        Icons.Default.Upload,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        "No certifications uploaded yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Surface(
            color = ConstructionColors.Warning.copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint = ConstructionColors.Warning,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Certifications can be added or updated later from the worker details page. OSHA requires valid certifications for certain job tasks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Role Dropdown Selector
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleDropdown(
    selectedRole: WorkerRole,
    onRoleSelect: (WorkerRole) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedRole.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Role *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = { Icon(Icons.Default.Work, null) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            WorkerRole.entries.forEach { role ->
                DropdownMenuItem(
                    text = { Text(role.displayName) },
                    onClick = {
                        onRoleSelect(role)
                        expanded = false
                    }
                )
            }
        }
    }
}
