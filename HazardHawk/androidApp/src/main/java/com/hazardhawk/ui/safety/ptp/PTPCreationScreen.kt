package com.hazardhawk.ui.safety.ptp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
            "Scaffolding", "Ironworking", "Other"
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

                // Project Details Section (OSHA-required)
                ProjectDetailsSection(
                    projectName = questionnaireState.projectName,
                    projectLocation = questionnaireState.projectLocation,
                    competentPersonName = questionnaireState.competentPersonName,
                    onProjectNameChange = { viewModel.updateProjectName(it) },
                    onProjectLocationChange = { viewModel.updateProjectLocation(it) },
                    onCompetentPersonChange = { viewModel.updateCompetentPersonName(it) }
                )

                // Question 1: Work Type (Hierarchical Selector)
                WorkTypeCategorySelector(
                    selectedWorkType = questionnaireState.workType,
                    onWorkTypeSelect = { viewModel.updateWorkType(it) }
                )

                // Question 2: Task Description (Guided with Smart Prompts)
                GuidedTaskDescription(
                    value = questionnaireState.taskDescription,
                    selectedWorkType = questionnaireState.workType,
                    onValueChange = { viewModel.updateTaskDescription(it) }
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

                // Token Usage Estimate (pre-generation)
                if (generationState !is GenerationState.Success && questionnaireState.taskDescription.isNotBlank()) {
                    com.hazardhawk.ui.safety.ptp.components.TokenCostEstimateCard(
                        taskDescription = questionnaireState.taskDescription
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Token Usage Receipt (post-generation)
                if (generationState is GenerationState.Success) {
                    val successState = generationState as GenerationState.Success
                    successState.tokenUsage?.let { tokenUsage ->
                        com.hazardhawk.ui.safety.ptp.components.TokenUsageReceiptCard(
                            tokenUsage = tokenUsage,
                            processingTimeMs = successState.processingTimeMs
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

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

/**
 * Guided task description with smart prompts
 */
@Composable
fun GuidedTaskDescription(
    value: String,
    selectedWorkType: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val taskPrompts = remember(selectedWorkType) { getTaskPrompts(selectedWorkType) }
    var showPrompts by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Header
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
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Task Description",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Character counter
                Text(
                    "${value.length} chars",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        value.length < 50 -> MaterialTheme.colorScheme.error
                        value.length > 500 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            Text(
                "Recommended: 50-500 characters for optimal AI analysis",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Divider()

            // Task Description TextField
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe the specific work to be performed...") },
                minLines = 4,
                maxLines = 8,
                supportingText = {
                    when {
                        value.length < 50 -> Text(
                            "Too short - add more details for better AI analysis",
                            color = MaterialTheme.colorScheme.error
                        )
                        value.length > 500 -> Text(
                            "Too long - try to be more concise",
                            color = MaterialTheme.colorScheme.error
                        )
                        else -> Text(
                            "Good length for AI analysis ✓",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // Smart Prompts Section
            if (selectedWorkType.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Smart Prompts:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { showPrompts = !showPrompts },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (showPrompts) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showPrompts) "Hide prompts" else "Show prompts",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                AnimatedVisibility(visible = showPrompts) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Tap a suggestion to use it as a starting point",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            taskPrompts.forEach { prompt ->
                                AssistChip(
                                    onClick = {
                                        // Append or replace based on current content
                                        if (value.isBlank()) {
                                            onValueChange(prompt)
                                        } else {
                                            onValueChange("$value\n$prompt")
                                        }
                                    },
                                    label = { Text(prompt) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Select a work type above to see smart prompts tailored to your specific task",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hierarchical work type selector with categories
 */
@Composable
fun WorkTypeCategorySelector(
    selectedWorkType: String,
    onWorkTypeSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<WorkCategory?>(null) }
    val categories = remember { getWorkCategories() }

    // Auto-select category if work type is already selected
    LaunchedEffect(selectedWorkType) {
        if (selectedWorkType.isNotBlank()) {
            selectedCategory = categories.find { category ->
                category.workTypes.contains(selectedWorkType)
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    "Work Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Text(
                "Select the category and specific work type",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )

            Divider(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f))

            // Category Selection (FilterChips)
            Text(
                "Category:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(category.name)
                            }
                        },
                        leadingIcon = if (selectedCategory == category) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            // Show OSHA reference for selected category
            selectedCategory?.let { category ->
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            category.oshaReference,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Specific Work Type Selection (RadioButtons)
                Text(
                    "Specific Work:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    category.workTypes.forEach { workType ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onWorkTypeSelect(workType) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedWorkType == workType,
                                onClick = { onWorkTypeSelect(workType) }
                            )
                            Text(
                                workType,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            } ?: run {
                // Prompt to select a category
                Text(
                    "👆 Select a category above to see specific work types",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

/**
 * Project details section - OSHA-required information
 */
@Composable
fun ProjectDetailsSection(
    projectName: String,
    projectLocation: String,
    competentPersonName: String,
    onProjectNameChange: (String) -> Unit,
    onProjectLocationChange: (String) -> Unit,
    onCompetentPersonChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "Project Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                "Required for OSHA compliance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )

            Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))

            // Project Name
            OutlinedTextField(
                value = projectName,
                onValueChange = onProjectNameChange,
                label = { Text("Project Name") },
                placeholder = { Text("e.g., Downtown Office Building") },
                leadingIcon = {
                    Icon(Icons.Default.Business, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                )
            )

            // Project Location
            OutlinedTextField(
                value = projectLocation,
                onValueChange = onProjectLocationChange,
                label = { Text("Project Location") },
                placeholder = { Text("e.g., 123 Main St, City, State") },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                )
            )

            // Competent Person
            OutlinedTextField(
                value = competentPersonName,
                onValueChange = onCompetentPersonChange,
                label = { Text("Competent Person / Site Supervisor") },
                placeholder = { Text("Full name of on-site supervisor") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                )
            )

            // Info card
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "A competent person is someone capable of identifying existing and predictable hazards and authorized to take prompt corrective measures (29 CFR 1926.32(f))",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
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
    val total = 8 // 3 project details + 5 questionnaire fields

    // Project details
    if (state.projectName.isNotBlank()) completed++
    if (state.projectLocation.isNotBlank()) completed++
    if (state.competentPersonName.isNotBlank()) completed++

    // Questionnaire
    if (state.workType.isNotBlank()) completed++
    if (state.taskDescription.isNotBlank()) completed++
    if (state.toolsEquipment.isNotBlank()) completed++
    if (state.crewSize != null && state.crewSize > 0) completed++
    completed++ // Height question is optional but counts as completed

    return completed.toFloat() / total
}

// ===== Work Type Hierarchical Structure =====

/**
 * Category of construction work with OSHA reference
 */
data class WorkCategory(
    val name: String,
    val oshaReference: String,
    val workTypes: List<String>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * Get all work type categories with OSHA references
 */
fun getWorkCategories(): List<WorkCategory> {
    return listOf(
        WorkCategory(
            name = "High-Risk Work",
            oshaReference = "OSHA 1926 Subpart M & R",
            workTypes = listOf(
                "Scaffolding Erection/Dismantling",
                "Roofing (Steep Slope >4:12)",
                "Steel Erection/Ironworking",
                "Excavation (>5 ft depth)",
                "Confined Space Entry",
                "Demolition"
            ),
            icon = Icons.Default.Warning
        ),
        WorkCategory(
            name = "Structural & Building",
            oshaReference = "OSHA 1926 Subpart Q",
            workTypes = listOf(
                "Concrete Forming/Pouring",
                "Framing (Wood/Metal)",
                "Masonry/Bricklaying",
                "Roofing (Low Slope ≤4:12)",
                "Drywall Installation",
                "Insulation Installation"
            ),
            icon = Icons.Default.Build
        ),
        WorkCategory(
            name = "Site & Earth Work",
            oshaReference = "OSHA 1926 Subpart P",
            workTypes = listOf(
                "Excavation (≤5 ft depth)",
                "Trenching",
                "Grading/Earthmoving",
                "Utility Installation",
                "Paving/Asphalt Work",
                "Landscaping"
            ),
            icon = Icons.Default.Terrain
        ),
        WorkCategory(
            name = "Systems & Trades",
            oshaReference = "OSHA 1926 Subpart K & V",
            workTypes = listOf(
                "Electrical Work",
                "Plumbing",
                "HVAC Installation",
                "Fire Protection Systems",
                "Communications/Data",
                "Painting/Finishing"
            ),
            icon = Icons.Default.Settings
        )
    )
}

/**
 * Get smart task prompts based on selected work type
 */
fun getTaskPrompts(workType: String): List<String> {
    return when {
        workType.contains("Scaffolding", ignoreCase = true) -> listOf(
            "Erecting system scaffold on [building/structure name]",
            "Installing guardrails and toeboards at [height] feet",
            "Inspecting scaffold components before assembly",
            "Dismantling scaffold from [location]"
        )
        workType.contains("Roofing", ignoreCase = true) -> listOf(
            "Installing [shingles/membrane/metal] roofing on [slope]",
            "Setting up fall protection anchor points",
            "Removing existing roof materials from [area]",
            "Installing roof underlayment and flashing"
        )
        workType.contains("Steel", ignoreCase = true) || workType.contains("Ironworking", ignoreCase = true) -> listOf(
            "Erecting structural steel beams at [height] feet",
            "Installing steel decking on [floor/level]",
            "Connecting steel members with [bolts/welds]",
            "Installing safety cables and netting"
        )
        workType.contains("Excavation", ignoreCase = true) || workType.contains("Trenching", ignoreCase = true) -> listOf(
            "Digging trench [length] x [width] x [depth]",
            "Installing protective systems (shoring/sloping/shielding)",
            "Locating underground utilities before digging",
            "Backfilling and compacting excavated area"
        )
        workType.contains("Confined Space", ignoreCase = true) -> listOf(
            "Entering [tank/vessel/vault] for [purpose]",
            "Testing atmosphere for oxygen, LEL, and toxics",
            "Setting up continuous ventilation and monitoring",
            "Posting attendant and rescue equipment"
        )
        workType.contains("Demolition", ignoreCase = true) -> listOf(
            "Demolishing [structure type] at [location]",
            "Conducting hazardous materials survey (asbestos/lead)",
            "Implementing dust control and debris removal",
            "Securing utilities and establishing exclusion zone"
        )
        workType.contains("Concrete", ignoreCase = true) -> listOf(
            "Pouring [slab/column/wall] concrete at [location]",
            "Setting up and bracing formwork",
            "Installing rebar and embedded items",
            "Finishing and curing concrete surfaces"
        )
        workType.contains("Framing", ignoreCase = true) -> listOf(
            "Framing [walls/floor/roof] on [level/floor]",
            "Installing [wood/metal] studs and headers",
            "Setting trusses or rafters",
            "Installing sheathing and temporary bracing"
        )
        workType.contains("Electrical", ignoreCase = true) -> listOf(
            "Installing [conduit/cable/panel] in [location]",
            "Implementing lockout/tagout procedures",
            "Testing circuits with voltage detector",
            "Connecting [fixtures/equipment] to power"
        )
        workType.contains("Plumbing", ignoreCase = true) -> listOf(
            "Installing [water/drain/gas] piping in [location]",
            "Connecting fixtures in [room/area]",
            "Pressure testing [system] lines",
            "Trenching for underground utilities"
        )
        workType.contains("HVAC", ignoreCase = true) -> listOf(
            "Installing ductwork in [location]",
            "Setting [unit/equipment] on [roof/pad]",
            "Running refrigerant lines and controls",
            "Testing and balancing air distribution"
        )
        workType.contains("Painting", ignoreCase = true) || workType.contains("Finishing", ignoreCase = true) -> listOf(
            "Painting [interior/exterior] surfaces at [location]",
            "Sanding and preparing surfaces for finish",
            "Applying [primer/paint/stain/sealant]",
            "Installing scaffolding or lifts for access"
        )
        else -> listOf(
            "Describe the specific task you will be performing",
            "Include location, materials, and methods",
            "List any special equipment or procedures needed"
        )
    }
}
