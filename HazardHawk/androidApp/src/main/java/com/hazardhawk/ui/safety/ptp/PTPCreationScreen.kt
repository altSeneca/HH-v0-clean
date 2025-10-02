package com.hazardhawk.ui.safety.ptp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

/**
 * PTP Creation Screen - Questionnaire-based interface for creating new PTPs.
 *
 * Features:
 * - Work type selection (10 preset types)
 * - 5-question progressive disclosure questionnaire
 * - Voice dictation integration (placeholder for future)
 * - Real-time validation
 * - Large touch targets (56dp minimum)
 * - Loading overlay during AI generation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PTPCreationScreen(
    viewModel: PTPViewModel = koinViewModel(),
    onNavigateToEditor: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val questionnaireState by viewModel.questionnaireState.collectAsState()
    val generationState by viewModel.generationState.collectAsState()
    val validationErrors by viewModel.validationErrors.collectAsState()

    // Work type options
    val workTypes = remember {
        listOf(
            "Roofing", "Electrical", "Plumbing", "Excavation",
            "Concrete", "Framing", "HVAC", "Demolition",
            "Scaffolding", "Other"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Pre-Task Plan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress indicator
                LinearProgressIndicator(
                    progress = { calculateProgress(questionnaireState) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Answer these questions to generate your Pre-Task Plan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Question 1: Work Type
                WorkTypeDropdown(
                    selectedType = questionnaireState.workType,
                    options = workTypes,
                    onSelect = { viewModel.updateWorkType(it) }
                )

                // Question 2: Task Description
                VoiceDictationTextField(
                    value = questionnaireState.taskDescription,
                    onValueChange = { viewModel.updateTaskDescription(it) },
                    label = "What task are you performing?",
                    placeholder = "Describe the work to be done",
                    minLines = 3
                )

                // Question 3: Tools & Equipment
                OutlinedTextField(
                    value = questionnaireState.toolsEquipment,
                    onValueChange = { viewModel.updateToolsEquipment(it) },
                    label = { Text("Tools & Equipment") },
                    placeholder = { Text("Power drill, ladder, safety harness...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    leadingIcon = { Icon(Icons.Default.Build, null) }
                )

                // Question 4: Working at Height
                WorkingAtHeightQuestion(
                    workingAtHeight = questionnaireState.workingAtHeight,
                    maximumHeight = questionnaireState.maximumHeight,
                    onWorkingAtHeightChange = { viewModel.updateWorkingAtHeight(it) },
                    onHeightChange = { viewModel.updateMaximumHeight(it) }
                )

                // Question 5: Crew Size
                OutlinedTextField(
                    value = questionnaireState.crewSize?.toString() ?: "",
                    onValueChange = { viewModel.updateCrewSize(it.toIntOrNull()) },
                    label = { Text("How many workers?") },
                    placeholder = { Text("Number of crew members") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Group, null) }
                )

                // Additional Questions (Collapsible)
                AdditionalQuestionsSection(
                    nearPowerLines = questionnaireState.nearPowerLines,
                    confinedSpace = questionnaireState.confinedSpace,
                    hazardousMaterials = questionnaireState.hazardousMaterials,
                    onNearPowerLinesChange = { viewModel.updateNearPowerLines(it) },
                    onConfinedSpaceChange = { viewModel.updateConfinedSpace(it) },
                    onHazardousMaterialsChange = { viewModel.updateHazardousMaterials(it) }
                )

                // Validation Errors
                if (validationErrors.isNotEmpty()) {
                    ValidationErrorCard(errors = validationErrors)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Generate Button
                Button(
                    onClick = {
                        viewModel.generatePTP(
                            onSuccess = { ptpId -> onNavigateToEditor(ptpId) },
                            onError = { /* Error handled in state */ }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = validationErrors.isEmpty() && generationState !is GenerationState.Generating
                ) {
                    when (generationState) {
                        is GenerationState.Generating -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating PTP...")
                        }
                        else -> {
                            Icon(Icons.Default.AutoAwesome, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate PTP with AI")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Bottom padding
            }

            // Loading Overlay
            if (generationState is GenerationState.Generating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Analyzing hazards with AI...",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "This may take 10-30 seconds",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Error Dialog
        if (generationState is GenerationState.Error) {
            AlertDialog(
                onDismissRequest = { viewModel.clearGenerationError() },
                title = { Text("Generation Failed") },
                text = { Text((generationState as GenerationState.Error).message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearGenerationError() }) {
                        Text("OK")
                    }
                },
                icon = { Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

// ===== Helper Composables =====

@Composable
fun VoiceDictationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    minLines: Int = 1
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder) },
                modifier = Modifier.weight(1f),
                minLines = minLines
            )

            // Voice dictation button (placeholder for future implementation)
            FilledTonalIconButton(
                onClick = { /* TODO: Implement voice dictation */ },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Mic, "Voice input", modifier = Modifier.size(28.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkTypeDropdown(
    selectedType: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Work Type") },
            placeholder = { Text("Select type of work") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = { Icon(Icons.Default.Work, null) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun WorkingAtHeightQuestion(
    workingAtHeight: Boolean,
    maximumHeight: Int?,
    onWorkingAtHeightChange: (Boolean) -> Unit,
    onHeightChange: (Int?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (workingAtHeight) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Icons.Default.Warning,
                        null,
                        tint = if (workingAtHeight) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Working at height?",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = workingAtHeight,
                    onCheckedChange = onWorkingAtHeightChange
                )
            }

            AnimatedVisibility(visible = workingAtHeight) {
                OutlinedTextField(
                    value = maximumHeight?.toString() ?: "",
                    onValueChange = { onHeightChange(it.toIntOrNull()) },
                    label = { Text("Maximum height (feet)") },
                    placeholder = { Text("Height in feet") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Height, null) },
                    supportingText = {
                        Text("Fall protection required above 6 feet", style = MaterialTheme.typography.bodySmall)
                    }
                )
            }
        }
    }
}

@Composable
fun AdditionalQuestionsSection(
    nearPowerLines: Boolean,
    confinedSpace: Boolean,
    hazardousMaterials: Boolean,
    onNearPowerLinesChange: (Boolean) -> Unit,
    onConfinedSpaceChange: (Boolean) -> Unit,
    onHazardousMaterialsChange: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Additional Safety Questions")
        }

        AnimatedVisibility(visible = expanded) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Near Power Lines
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Working near power lines?")
                        Switch(
                            checked = nearPowerLines,
                            onCheckedChange = onNearPowerLinesChange
                        )
                    }

                    Divider()

                    // Confined Space
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Confined space entry?")
                        Switch(
                            checked = confinedSpace,
                            onCheckedChange = onConfinedSpaceChange
                        )
                    }

                    Divider()

                    // Hazardous Materials
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hazardous materials involved?")
                        Switch(
                            checked = hazardousMaterials,
                            onCheckedChange = onHazardousMaterialsChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ValidationErrorCard(errors: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
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
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "Please fix the following:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            errors.forEach { error ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("•", color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

private fun calculateProgress(state: QuestionnaireState): Float {
    var completed = 0
    val total = 5

    if (state.workType.isNotBlank()) completed++
    if (state.taskDescription.isNotBlank()) completed++
    if (state.toolsEquipment.isNotBlank()) completed++
    if (state.crewSize != null && state.crewSize > 0) completed++
    completed++ // Height question is optional but counts as completed

    return completed.toFloat() / total
}
