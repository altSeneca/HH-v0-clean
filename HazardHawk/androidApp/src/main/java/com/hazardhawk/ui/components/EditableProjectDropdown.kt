package com.hazardhawk.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hazardhawk.data.ProjectManager

/**
 * An editable dropdown component for project management that provides:
 * - Display of current project
 * - Dropdown menu with existing projects
 * - Add new project functionality
 * - Delete project capability
 * - Integration with ProjectManager
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableProjectDropdown(
    projectManager: ProjectManager,
    currentProject: String,
    onProjectSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }
    val projects by projectManager.allProjects.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier) {
        // Handle empty project list
        if (projects.isEmpty()) {
            // Show "Add First Project" button when no projects exist
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add First Project")
            }
        } else {
            // Main dropdown button
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = currentProject,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Project") },
                    trailingIcon = {
                        Row {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // Existing projects
                    projects.forEach { project ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = project,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (projects.size > 1) { // Don't allow deleting the last project
                                        IconButton(
                                            onClick = {
                                                showDeleteConfirmation = project
                                                expanded = false
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete project",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                if (project != currentProject) {
                                    onProjectSelected(project)
                                    projectManager.setCurrentProject(project)
                                }
                                expanded = false
                            }
                        )
                    }

                    HorizontalDivider()

                    // Add new project option
                    DropdownMenuItem(
                        text = { Text("+ Add New Project") },
                        onClick = {
                            showAddDialog = true
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    // Add new project dialog
    if (showAddDialog) {
        var newProjectName by remember { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }

        StandardDialog(
            onDismissRequest = {
                showAddDialog = false
                newProjectName = ""
                showError = false
            },
            title = {
                Text(
                    text = "Add New Project",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = {
                            newProjectName = it
                            showError = false
                        },
                        label = { Text("Project Name") },
                        placeholder = { Text("Enter project name") },
                        isError = showError,
                        supportingText = if (showError) {
                            { Text("Project name cannot be empty or already exist") }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                val trimmedName = newProjectName.trim()
                                when {
                                    trimmedName.isEmpty() -> showError = true
                                    projects.contains(trimmedName) -> showError = true
                                    else -> {
                                        projectManager.addProject(trimmedName)
                                        onProjectSelected(trimmedName)
                                        projectManager.setCurrentProject(trimmedName)
                                        showAddDialog = false
                                        newProjectName = ""
                                        showError = false
                                    }
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            buttons = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        newProjectName = ""
                        showError = false
                    }
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val trimmedName = newProjectName.trim()
                        when {
                            trimmedName.isEmpty() -> showError = true
                            projects.contains(trimmedName) -> showError = true
                            else -> {
                                projectManager.addProject(trimmedName)
                                onProjectSelected(trimmedName)
                                projectManager.setCurrentProject(trimmedName)
                                showAddDialog = false
                                newProjectName = ""
                                showError = false
                            }
                        }
                    }
                ) {
                    Text("Add Project")
                }
            }
        )
    }

    // Delete confirmation dialog
    showDeleteConfirmation?.let { projectToDelete ->
        StandardDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = {
                Text(
                    text = "Delete Project",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            content = {
                Text(
                    text = "Are you sure you want to delete the project \"$projectToDelete\"?\n\nThis action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            buttons = {
                TextButton(
                    onClick = { showDeleteConfirmation = null }
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        projectManager.removeProject(projectToDelete)
                        if (projectToDelete == currentProject) {
                            // Switch to the first available project
                            val remainingProjects = projects - projectToDelete
                            if (remainingProjects.isNotEmpty()) {
                                val newProject = remainingProjects.first()
                                onProjectSelected(newProject)
                                projectManager.setCurrentProject(newProject)
                            }
                        }
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            }
        )
    }
}