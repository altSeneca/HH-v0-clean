package com.hazardhawk

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.camera.MetadataSettingsManager
import com.hazardhawk.camera.UserProfile
import com.hazardhawk.camera.ProjectInfo
import com.hazardhawk.ui.components.ConstructionDialog
import com.hazardhawk.ui.components.EditableProjectDropdown
import com.hazardhawk.data.ProjectManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstLaunchSetupDialog(
    onSetupComplete: (companyName: String, projectName: String) -> Unit,
    metadataSettingsManager: MetadataSettingsManager,
    projectManager: ProjectManager
) {
    var companyName by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("") }
    var selectedProject by remember { mutableStateOf<String?>(null) }
    var showProjectInput by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val companyNameFocusRequester = remember { FocusRequester() }
    val projectNameFocusRequester = remember { FocusRequester() }
    
    // Predefined project suggestions
    val projectSuggestions = remember {
        listOf(
            "Site Safety Inspection",
            "Daily Walkthrough",
            "New Construction",
            "Renovation Project",
            "Maintenance Work"
        )
    }
    
    // Update projectName when selectedProject changes
    LaunchedEffect(selectedProject) {
        selectedProject?.let { project ->
            if (project != projectName) {
                projectName = project
            }
        }
    }
    
    // Request focus for project input when it becomes visible
    LaunchedEffect(showProjectInput) {
        if (showProjectInput) {
            kotlinx.coroutines.delay(100) // Small delay for animation
            projectNameFocusRequester.requestFocus()
        }
    }
    
    ConstructionDialog(
        onDismissRequest = null, // Non-dismissible on first launch
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = Color(0xFFFF8C00),
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Welcome to HazardHawk",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8C00)
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Let's set up your safety documentation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        content = {
            // Company Name Input
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Name") },
                placeholder = { Text("Enter your company name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = "Company",
                        tint = Color(0xFFFF8C00)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { 
                        if (showProjectInput) {
                            projectNameFocusRequester.requestFocus()
                        } else {
                            focusManager.clearFocus()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF8C00),
                    focusedLabelColor = Color(0xFFFF8C00)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(companyNameFocusRequester)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Project Selection Section
            Text(
                text = "Select or Enter Project",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            // Quick Project Selection Chips
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                projectSuggestions.chunked(2).forEach { rowProjects ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowProjects.forEach { project ->
                            FilterChip(
                                selected = selectedProject == project && !showProjectInput,
                                onClick = {
                                    selectedProject = project
                                    projectName = project
                                    showProjectInput = false
                                    focusManager.clearFocus()
                                },
                                label = { 
                                    Text(
                                        text = project,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF8C00).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFFFF8C00)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Add empty space if odd number of items
                        if (rowProjects.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Custom Project Input Toggle
            TextButton(
                onClick = { 
                    showProjectInput = !showProjectInput
                    if (showProjectInput) {
                        selectedProject = null
                        projectName = ""
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFFF8C00)
                )
            ) {
                Icon(
                    imageVector = if (showProjectInput) Icons.Default.ExpandLess else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (showProjectInput) "Hide Custom Project" else "Enter Custom Project",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            // Custom Project Name Input with Crossfade animation
            Crossfade(
                targetState = showProjectInput,
                label = "project_input"
            ) { isVisible ->
                if (isVisible) {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { 
                            projectName = it
                            selectedProject = null
                        },
                        label = { Text("Project Name") },
                        placeholder = { Text("Enter project name") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Engineering,
                                contentDescription = "Project",
                                tint = Color(0xFFFF8C00)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF8C00),
                            focusedLabelColor = Color(0xFFFF8C00)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .focusRequester(projectNameFocusRequester)
                    )
                } else {
                    // Invisible placeholder to maintain consistent layout
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info Text
            Text(
                text = "You can change these settings anytime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        },
        actions = {
            Button(
                onClick = {
                    if (companyName.isNotBlank() && projectName.isNotBlank()) {
                        // Save to ProjectManager
                        if (!projectManager.projectExists(projectName)) {
                            projectManager.addProject(projectName)
                        }
                        projectManager.setCurrentProject(projectName)
                        
                        // Save to MetadataSettingsManager
                        val userProfile = metadataSettingsManager.userProfile.value.copy(
                            company = companyName
                        )
                        val projectInfo = ProjectInfo(
                            projectId = System.currentTimeMillis().toString(),
                            projectName = projectName
                        )
                        
                        metadataSettingsManager.updateUserProfile(userProfile)
                        metadataSettingsManager.updateCurrentProject(projectInfo)
                        
                        // Mark first launch as complete
                        val sharedPrefs = metadataSettingsManager.javaClass
                            .getDeclaredField("sharedPrefs")
                            .apply { isAccessible = true }
                            .get(metadataSettingsManager) as android.content.SharedPreferences
                        
                        sharedPrefs.edit()
                            .putBoolean("first_launch_complete", true)
                            .apply()
                        
                        onSetupComplete(companyName, projectName)
                    }
                },
                enabled = companyName.isNotBlank() && projectName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF8C00),
                    disabledContainerColor = Color(0xFFFF8C00).copy(alpha = 0.3f)
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Capturing",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    )
}