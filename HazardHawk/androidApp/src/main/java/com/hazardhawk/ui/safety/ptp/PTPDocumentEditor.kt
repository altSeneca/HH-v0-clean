package com.hazardhawk.ui.safety.ptp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hazardhawk.domain.models.ptp.*
import com.hazardhawk.ui.safety.components.SignatureCaptureComponent
import org.koin.androidx.compose.koinViewModel

/**
 * PTP Document Editor Screen
 *
 * Features:
 * - Displays AI-generated content with quality indicators
 * - Editable hazards table with OSHA codes
 * - Job steps with reordering capability
 * - Collapsible sections for better UX
 * - Signature capture integration
 * - Export to PDF functionality
 * - Construction-friendly design (high contrast, large touch targets)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PTPDocumentEditor(
    ptpId: String,
    viewModel: PTPViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onExportComplete: (String) -> Unit
) {
    val documentState by viewModel.documentState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showSignatureDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Load PTP when screen opens
    LaunchedEffect(ptpId) {
        viewModel.loadPTP(ptpId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Pre-Task Plan") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (documentState?.hasUnsavedChanges == true) {
                            // TODO: Show unsaved changes dialog
                        }
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Save button
                    if (documentState?.hasUnsavedChanges == true) {
                        IconButton(
                            onClick = { viewModel.savePTP() },
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Save, "Save")
                            }
                        }
                    }

                    // Export button
                    IconButton(
                        onClick = { showExportDialog = true },
                        enabled = !uiState.isExporting
                    ) {
                        Icon(Icons.Default.PictureAsPdf, "Export PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            // Sign & Complete FAB
            if (documentState?.hasSignature != true) {
                ExtendedFloatingActionButton(
                    onClick = { showSignatureDialog = true },
                    icon = { Icon(Icons.Default.Draw, null) },
                    text = { Text("Sign & Complete") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            } else if (documentState != null) {
                PTPEditorContent(
                    documentState = documentState!!,
                    onHazardUpdate = { index, hazard ->
                        viewModel.updateHazard(index, hazard)
                    },
                    onHazardDelete = { index ->
                        viewModel.deleteHazard(index)
                    },
                    onJobStepUpdate = { index, step ->
                        viewModel.updateJobStep(index, step)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } else if (uiState.error != null) {
                ErrorDisplay(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadPTP(ptpId) },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )
            }
        }
    }

    // Signature Capture Dialog
    if (showSignatureDialog) {
        AlertDialog(
            onDismissRequest = { showSignatureDialog = false },
            title = { Text("Supervisor Signature") },
            text = {
                SignatureCaptureComponent(
                    onSignatureCaptured = { signatureData ->
                        viewModel.saveSignature(signatureData)
                        showSignatureDialog = false
                    },
                    onCancel = { showSignatureDialog = false }
                )
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportPDFDialog(
            isExporting = uiState.isExporting,
            onConfirm = { companyName, projectName, location ->
                viewModel.exportPDF(
                    companyName = companyName,
                    projectName = projectName,
                    projectLocation = location,
                    onComplete = { path ->
                        showExportDialog = false
                        onExportComplete(path)
                    },
                    onError = { error ->
                        // Error shown in UI state
                    }
                )
            },
            onDismiss = { showExportDialog = false }
        )
    }

    // Error Snackbar
    if (uiState.error != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(uiState.error!!)
        }
    }
}

@Composable
private fun PTPEditorContent(
    documentState: DocumentState,
    onHazardUpdate: (Int, PtpHazard) -> Unit,
    onHazardDelete: (Int) -> Unit,
    onJobStepUpdate: (Int, JobStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val content = documentState.userModifiedContent ?: documentState.aiGeneratedContent

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Quality Card
        if (documentState.aiGeneratedContent != null) {
            item {
                AIQualityCard(
                    confidence = documentState.aiConfidence,
                    warnings = documentState.aiWarnings,
                    hasUserModifications = documentState.userModifiedContent != null
                )
            }
        }

        // Document Header
        item {
            PTDocumentHeader(ptp = documentState.ptp)
        }

        // Hazards Section
        if (!content?.hazards.isNullOrEmpty()) {
            item {
                Text(
                    "Identified Hazards",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            itemsIndexed(content!!.hazards) { index, hazard ->
                HazardCard(
                    hazard = hazard,
                    onUpdate = { updatedHazard ->
                        onHazardUpdate(index, updatedHazard)
                    },
                    onDelete = {
                        onHazardDelete(index)
                    }
                )
            }
        }

        // Job Steps Section
        if (!content?.jobSteps.isNullOrEmpty()) {
            item {
                Text(
                    "Job Steps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            itemsIndexed(content!!.jobSteps) { index, step ->
                JobStepCard(
                    step = step,
                    onUpdate = { updatedStep ->
                        onJobStepUpdate(index, updatedStep)
                    }
                )
            }
        }

        // Emergency Procedures Section
        if (!content?.emergencyProcedures.isNullOrEmpty()) {
            item {
                EmergencyProceduresSection(procedures = content!!.emergencyProcedures)
            }
        }

        // Required Training Section
        if (!content?.requiredTraining.isNullOrEmpty()) {
            item {
                RequiredTrainingSection(training = content!!.requiredTraining)
            }
        }

        // Signature Section
        if (documentState.hasSignature) {
            item {
                SignatureSection(signature = documentState.ptp.signatureSupervisor!!)
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AIQualityCard(
    confidence: Double,
    warnings: List<String>,
    hasUserModifications: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                confidence >= 0.8 -> MaterialTheme.colorScheme.primaryContainer
                confidence >= 0.6 -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "AI-Generated Content",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (hasUserModifications) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Modified") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                }
            }

            if (confidence > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Confidence:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { confidence.toFloat() },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                    )
                    Text(
                        "${(confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (warnings.isNotEmpty()) {
                Divider()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Review Required:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    warnings.forEach { warning ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                warning,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PTDocumentHeader(ptp: PreTaskPlan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Pre-Task Plan Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Divider()

            InfoRow("Work Type", ptp.workType)
            InfoRow("Work Scope", ptp.workScope)
            if (ptp.crewSize != null) {
                InfoRow("Crew Size", ptp.crewSize.toString())
            }
            InfoRow("Status", ptp.status.name)
            InfoRow(
                "Created",
                java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US)
                    .format(java.util.Date(ptp.createdAt))
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HazardCard(
    hazard: PtpHazard,
    onUpdate: (PtpHazard) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (hazard.severity) {
                HazardSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                HazardSeverity.MAJOR -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                HazardSeverity.MINOR -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            }
        ),
        border = BorderStroke(
            width = 2.dp,
            color = when (hazard.severity) {
                HazardSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                HazardSeverity.MAJOR -> Color(0xFFFF9800)
                HazardSeverity.MINOR -> MaterialTheme.colorScheme.secondary
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    hazard.severity.name,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = when (hazard.severity) {
                                    HazardSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                                    HazardSeverity.MAJOR -> Color(0xFFFF9800)
                                    HazardSeverity.MINOR -> MaterialTheme.colorScheme.secondary
                                }
                            )
                        )

                        Text(
                            hazard.oshaCode,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Delete hazard",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    IconButton(
                        onClick = { isEditing = !isEditing },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            "Edit hazard"
                        )
                    }

                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            "Expand"
                        )
                    }
                }
            }

            if (isEditing) {
                var editedDescription by remember { mutableStateOf(hazard.description) }

                OutlinedTextField(
                    value = editedDescription,
                    onValueChange = { editedDescription = it },
                    label = { Text("Hazard Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Button(
                    onClick = {
                        onUpdate(hazard.copy(description = editedDescription))
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            } else {
                Text(
                    hazard.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Divider()

                    if (hazard.controls.isNotEmpty()) {
                        Text(
                            "Controls:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        hazard.controls.forEach { control ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•")
                                Text(control, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (hazard.requiredPpe.isNotEmpty()) {
                        Text(
                            "Required PPE:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            hazard.requiredPpe.forEach { ppe ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(ppe, style = MaterialTheme.typography.bodySmall) },
                                    leadingIcon = { Icon(Icons.Default.Shield, null) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Delete Hazard?")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Are you sure you want to delete this hazard?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "OSHA Code: ${hazard.oshaCode}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        hazard.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
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
private fun JobStepCard(
    step: JobStep,
    onUpdate: (JobStep) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                step.stepNumber.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        "Expand"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Divider()

                    if (step.hazards.isNotEmpty()) {
                        Text(
                            "Hazards:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        step.hazards.forEach { hazard ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.Warning,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(hazard, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (step.controls.isNotEmpty()) {
                        Text(
                            "Controls:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        step.controls.forEach { control ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•")
                                Text(control, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (step.ppe.isNotEmpty()) {
                        Text(
                            "PPE Required:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            step.ppe.forEach { ppe ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(ppe, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyProceduresSection(procedures: List<String>) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocalHospital,
                        null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Emergency Procedures",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        "Expand"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    procedures.forEachIndexed { index, procedure ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                modifier = Modifier.size(24.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.error
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        (index + 1).toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onError,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                procedure,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequiredTrainingSection(training: List<String>) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.School, null)
                    Text(
                        "Required Training & Certifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        "Expand"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    training.forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(item, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignatureSection(signature: SignatureData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Signed & Approved",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider()

            InfoRow("Supervisor", signature.supervisorName)
            InfoRow(
                "Date",
                java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US)
                    .format(java.util.Date(signature.signatureDate))
            )

            if (signature.signatureBlob != null) {
                // TODO: Display signature image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.medium
                        )
                        .background(Color.White, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Signature Image",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportPDFDialog(
    isExporting: Boolean,
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var companyName by remember { mutableStateOf("HazardHawk") }
    var projectName by remember { mutableStateOf("") }
    var projectLocation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = { Text("Export to PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExporting
                )

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExporting
                )

                OutlinedTextField(
                    value = projectLocation,
                    onValueChange = { projectLocation = it },
                    label = { Text("Project Location") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExporting
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(companyName, projectName, projectLocation)
                },
                enabled = !isExporting && companyName.isNotBlank()
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isExporting) "Exporting..." else "Export")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ErrorDisplay(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
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
            "Error Loading PTP",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}
