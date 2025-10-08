package com.hazardhawk.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hazardhawk.ui.theme.ConstructionColors
import org.koin.androidx.compose.koinViewModel
import java.io.ByteArrayOutputStream

/**
 * Company Settings Screen - Centralized company information management.
 *
 * Features:
 * - Edit company name, address, city, state, zip, phone
 * - Upload/delete company logo
 * - Form validation with error messages
 * - Material 3 design with construction theme
 * - Save/cancel with unsaved changes warning
 * - Real-time validation feedback
 *
 * This is the single source of truth for company info used across all safety documents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: CompanySettingsViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect state
    val company by viewModel.company.collectAsStateWithLifecycle()
    val companyName by viewModel.companyName.collectAsStateWithLifecycle()
    val address by viewModel.address.collectAsStateWithLifecycle()
    val city by viewModel.city.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val zip by viewModel.zip.collectAsStateWithLifecycle()
    val phone by viewModel.phone.collectAsStateWithLifecycle()
    val logoUrl by viewModel.logoUrl.collectAsStateWithLifecycle()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()
    val isFormValid by viewModel.isFormValid.collectAsStateWithLifecycle()

    val nameError by viewModel.nameError.collectAsStateWithLifecycle()
    val phoneError by viewModel.phoneError.collectAsStateWithLifecycle()
    val zipError by viewModel.zipError.collectAsStateWithLifecycle()

    // Show unsaved changes dialog
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                bytes?.let { logoData ->
                    val fileName = "company_logo_${System.currentTimeMillis()}.jpg"
                    viewModel.uploadLogo(logoData, fileName)
                }
            } catch (e: Exception) {
                // Handle error - will be shown via errorMessage StateFlow
            }
        }
    }

    // Show success/error messages
    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }

    // Unsaved changes dialog
    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ConstructionColors.SafetyOrange
                )
            },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Do you want to save them before leaving?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCompany()
                        showUnsavedChangesDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ConstructionColors.SafetyOrange
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showUnsavedChangesDialog = false }) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            showUnsavedChangesDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Company Settings",
                        fontWeight = FontWeight.Bold,
                        color = ConstructionColors.OnSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasChanges) {
                                showUnsavedChangesDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ConstructionColors.OnSurface
                        )
                    }
                },
                actions = {
                    // Save button in app bar
                    AnimatedVisibility(
                        visible = hasChanges && !isSaving,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.saveCompany()
                            },
                            enabled = isFormValid
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Changes",
                                tint = if (isFormValid) ConstructionColors.OnSurface else Color.Gray
                            )
                        }
                    }

                    // Loading indicator
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 12.dp),
                            color = ConstructionColors.OnSurface,
                            strokeWidth = 2.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConstructionColors.SafetyOrange
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ConstructionColors.Surface,
                            ConstructionColors.SurfaceVariant
                        )
                    )
                )
        ) {
            if (isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ConstructionColors.SafetyOrange)
                }
            } else if (company == null) {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Failed to load company",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                // Main content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Logo Section
                    CompanyLogoSection(
                        logoUrl = logoUrl,
                        onUploadClick = { imagePickerLauncher.launch("image/*") },
                        onDeleteClick = { viewModel.deleteLogo() },
                        isLoading = isSaving
                    )

                    // Basic Information Section
                    SettingsSection(title = "Basic Information") {
                        // Company Name
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = viewModel::updateCompanyName,
                            label = { Text("Company Name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null
                                )
                            },
                            isError = nameError != null,
                            supportingText = nameError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ConstructionColors.SafetyOrange,
                                focusedLabelColor = ConstructionColors.SafetyOrange,
                                focusedLeadingIconColor = ConstructionColors.SafetyOrange,
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error
                            ),
                            singleLine = true
                        )

                        // Phone Number
                        OutlinedTextField(
                            value = phone,
                            onValueChange = viewModel::updatePhone,
                            label = { Text("Phone Number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null
                                )
                            },
                            isError = phoneError != null,
                            supportingText = phoneError?.let { { Text(it) } },
                            placeholder = { Text("(555) 123-4567") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ConstructionColors.SafetyOrange,
                                focusedLabelColor = ConstructionColors.SafetyOrange,
                                focusedLeadingIconColor = ConstructionColors.SafetyOrange
                            ),
                            singleLine = true
                        )
                    }

                    // Address Section
                    SettingsSection(title = "Address") {
                        // Street Address
                        OutlinedTextField(
                            value = address,
                            onValueChange = viewModel::updateAddress,
                            label = { Text("Street Address") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null
                                )
                            },
                            placeholder = { Text("123 Construction Ave") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ConstructionColors.SafetyOrange,
                                focusedLabelColor = ConstructionColors.SafetyOrange,
                                focusedLeadingIconColor = ConstructionColors.SafetyOrange
                            ),
                            singleLine = true
                        )

                        // City
                        OutlinedTextField(
                            value = city,
                            onValueChange = viewModel::updateCity,
                            label = { Text("City") },
                            placeholder = { Text("San Francisco") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ConstructionColors.SafetyOrange,
                                focusedLabelColor = ConstructionColors.SafetyOrange
                            ),
                            singleLine = true
                        )

                        // State and Zip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // State
                            OutlinedTextField(
                                value = state,
                                onValueChange = viewModel::updateState,
                                label = { Text("State") },
                                placeholder = { Text("CA") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ConstructionColors.SafetyOrange,
                                    focusedLabelColor = ConstructionColors.SafetyOrange
                                ),
                                singleLine = true
                            )

                            // Zip
                            OutlinedTextField(
                                value = zip,
                                onValueChange = viewModel::updateZip,
                                label = { Text("Zip Code") },
                                placeholder = { Text("94105") },
                                isError = zipError != null,
                                supportingText = zipError?.let { { Text(it) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ConstructionColors.SafetyOrange,
                                    focusedLabelColor = ConstructionColors.SafetyOrange
                                ),
                                singleLine = true
                            )
                        }
                    }

                    // Info Card
                    InfoCard(
                        text = "This information will be automatically included in all safety documents (PTPs, reports, etc.)",
                        icon = Icons.Default.Info
                    )

                    // Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel/Reset button
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.resetForm()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = hasChanges && !isSaving
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset")
                        }

                        // Save button
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.saveCompany()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = hasChanges && isFormValid && !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ConstructionColors.SafetyOrange,
                                disabledContainerColor = Color.Gray
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Changes")
                        }
                    }

                    // Bottom spacing
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun CompanyLogoSection(
    logoUrl: String?,
    onUploadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Company Logo",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ConstructionColors.SafetyOrange
            )

            // Logo display
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(ConstructionColors.SurfaceVariant)
                    .border(2.dp, ConstructionColors.SafetyOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = "Company Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = ConstructionColors.OnSurfaceVariant
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onUploadClick,
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (logoUrl != null) "Change" else "Upload")
                }

                if (logoUrl != null) {
                    OutlinedButton(
                        onClick = onDeleteClick,
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ConstructionColors.SafetyOrange,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ConstructionColors.Surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun InfoCard(
    text: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.SafetyOrange.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ConstructionColors.SafetyOrange,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                fontSize = 12.sp,
                color = ConstructionColors.OnSurface,
                lineHeight = 16.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompanySettingsScreenPreview() {
    MaterialTheme {
        // Preview requires mock ViewModel
        // CompanySettingsScreen(onNavigateBack = {})
    }
}
