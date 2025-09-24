package com.hazardhawk.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazardhawk.camera.MetadataSettingsManager
import com.hazardhawk.ui.theme.ConstructionColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Company/Project Entry Screen for HazardHawk
 * 
 * Professional entry point that captures company and project information
 * before accessing camera functionality. Optimized for construction workers
 * with large touch targets and high contrast design.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CompanyProjectEntryScreen(
    onNavigateToCamera: (company: String, project: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val projectManager = remember { com.hazardhawk.data.ProjectManager(context) }
    val metadataSettingsManager = remember { MetadataSettingsManager(context, projectManager) }

    // Load existing data from persistent storage
    val userProfile by metadataSettingsManager.userProfile.collectAsStateWithLifecycle()
    val currentProject by metadataSettingsManager.currentProject.collectAsStateWithLifecycle()

    var companyName by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("") }
    var animationStarted by remember { mutableStateOf(false) }
    var dataLoaded by remember { mutableStateOf(false) }

    // Load existing data when available
    LaunchedEffect(userProfile, currentProject) {
        if (!dataLoaded) {
            companyName = userProfile.company
            projectName = currentProject.projectName
            dataLoaded = true
        }
    }

    // Input validation
    val isCompanyValid = companyName.trim().length >= 2
    val isProjectValid = currentProject.projectName.trim().length >= 2 || projectName.trim().length >= 2
    val canProceed = isCompanyValid && isProjectValid

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        delay(200)
        animationStarted = true
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ConstructionColors.Surface,
                        ConstructionColors.SurfaceVariant
                    )
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Professional Branding Header
        AnimatedVisibility(
            visible = animationStarted,
            enter = slideInVertically(
                initialOffsetY = { -100 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        ) {
            CompanyProjectBrandingCard()
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Company Information Section
        AnimatedVisibility(
            visible = animationStarted,
            enter = slideInHorizontally(
                initialOffsetX = { -300 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                )
            )
        ) {
            CompanyInputCard(
                companyName = companyName,
                onCompanyNameChange = { companyName = it },
                isValid = isCompanyValid
            )
        }
        
        // Project Information Section with Slim Project Bar
        AnimatedVisibility(
            visible = animationStarted,
            enter = slideInHorizontally(
                initialOffsetX = { 300 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                )
            )
        ) {
            ProjectSelectionCard(
                currentProject = currentProject,
                onProjectSelected = { project ->
                    projectName = project.projectName
                    metadataSettingsManager.updateCurrentProject(project)
                },
                isValid = isProjectValid
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Security Notice
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                )
            )
        ) {
            SecurityNoticeCard()
        }
        
        // Start Documentation Button
        AnimatedVisibility(
            visible = animationStarted,
            enter = slideInVertically(
                initialOffsetY = { 100 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                )
            )
        ) {
            StartDocumentationButton(
                enabled = canProceed,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    // Save data to persistent storage
                    val userProfile = com.hazardhawk.camera.UserProfile(
                        company = companyName.trim(),
                        userName = "Construction Worker",
                        userId = "user_${System.currentTimeMillis()}"
                    )
                    val projectInfo = com.hazardhawk.camera.ProjectInfo(
                        projectName = projectName.trim(),
                        projectId = "proj_${System.currentTimeMillis()}"
                    )

                    metadataSettingsManager.updateUserProfile(userProfile)
                    metadataSettingsManager.updateCurrentProject(projectInfo)

                    onNavigateToCamera(companyName.trim(), projectName.trim())
                }
            )
        }
    }
}

/**
 * Professional branding card with HazardHawk logo and current date
 */
@Composable
private fun CompanyProjectBrandingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.SafetyOrange
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "HazardHawk",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            
            Text(
                text = "Safety Documentation Setup",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Text(
                text = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(Date()),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Company name input card with validation
 */
@Composable
private fun CompanyInputCard(
    companyName: String,
    onCompanyNameChange: (String) -> Unit,
    isValid: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = ConstructionColors.SafetyOrange,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Company Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConstructionColors.OnSurface
                )
            }
            
            OutlinedTextField(
                value = companyName,
                onValueChange = onCompanyNameChange,
                label = { Text("Company Name") },
                placeholder = { Text("e.g., Acme Construction") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp), // Construction glove compatibility
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                isError = companyName.isNotEmpty() && !isValid,
                supportingText = if (companyName.isNotEmpty() && !isValid) {
                    { Text("Company name must be at least 2 characters") }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ConstructionColors.SafetyOrange,
                    focusedLabelColor = ConstructionColors.SafetyOrange
                )
            )
        }
    }
}

/**
 * Project selection card with slim project bar functionality
 */
@Composable
private fun ProjectSelectionCard(
    currentProject: com.hazardhawk.camera.ProjectInfo,
    onProjectSelected: (com.hazardhawk.camera.ProjectInfo) -> Unit,
    isValid: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Engineering,
                    contentDescription = null,
                    tint = ConstructionColors.SafetyOrange,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Project Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConstructionColors.OnSurface
                )
            }

            // Slim Project Bar for setup screen
            SetupSlimProjectBar(
                currentProject = currentProject,
                onProjectSelected = onProjectSelected
            )

            if (currentProject.projectName.isNotEmpty() && !isValid) {
                Text(
                    text = "Project name must be at least 2 characters",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

/**
 * Setup version of Slim Project Bar - with direct text input for better UX
 */
@Composable
private fun SetupSlimProjectBar(
    currentProject: com.hazardhawk.camera.ProjectInfo,
    onProjectSelected: (com.hazardhawk.camera.ProjectInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var projectText by remember { mutableStateOf(currentProject.projectName) }

    // Update projectText when currentProject changes
    LaunchedEffect(currentProject) {
        projectText = currentProject.projectName
    }

    // Project state management with sample projects
    var projectList by remember {
        mutableStateOf(
            listOf(
                com.hazardhawk.camera.ProjectInfo(
                    projectId = "DEMO",
                    projectName = "Demo Project",
                    siteAddress = "Sample Site"
                ),
                com.hazardhawk.camera.ProjectInfo(
                    projectId = "OFFICE",
                    projectName = "Office Tower",
                    siteAddress = "Downtown"
                ),
                com.hazardhawk.camera.ProjectInfo(
                    projectId = "BRIDGE",
                    projectName = "Bridge Repair",
                    siteAddress = "Highway 95"
                ),
                com.hazardhawk.camera.ProjectInfo(
                    projectId = "WAREHOUSE",
                    projectName = "Warehouse Complex",
                    siteAddress = "Industrial District"
                ),
                com.hazardhawk.camera.ProjectInfo(
                    projectId = "SCHOOL",
                    projectName = "Elementary School",
                    siteAddress = "Education District"
                )
            )
        )
    }

    Column(modifier = modifier) {
        // Direct input field with dropdown toggle
        OutlinedTextField(
            value = projectText,
            onValueChange = { newText ->
                projectText = newText
                // Create new project when user types
                if (newText.isNotBlank()) {
                    val newProject = com.hazardhawk.camera.ProjectInfo(
                        projectId = "CUSTOM_${System.currentTimeMillis()}",
                        projectName = newText,
                        siteAddress = ""
                    )
                    onProjectSelected(newProject)
                }
            },
            label = { Text("Project Name") },
            placeholder = { Text("Enter project name or select from list") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { expanded = false }
            ),
            trailingIcon = {
                IconButton(
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Hide suggestions" else "Show suggestions",
                        tint = ConstructionColors.OnSurface
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ConstructionColors.SafetyOrange,
                focusedLabelColor = ConstructionColors.SafetyOrange,
                focusedTextColor = ConstructionColors.OnSurface,
                unfocusedTextColor = ConstructionColors.OnSurface
            )
        )

        // Suggestions dropdown - only show when expanded and with reasonable height
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp) // Reasonable max height
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ConstructionColors.Surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Filter projects based on current text
                    val filteredProjects = projectList.filter {
                        it.projectName.contains(projectText, ignoreCase = true) ||
                        projectText.isBlank()
                    }

                    items(filteredProjects.size) { index ->
                        val project = filteredProjects[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (project.projectName == projectText)
                                        ConstructionColors.SafetyOrange.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    projectText = project.projectName
                                    onProjectSelected(project)
                                    expanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.projectName,
                                    color = ConstructionColors.OnSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (project.siteAddress.isNotBlank()) {
                                    Text(
                                        text = project.siteAddress,
                                        color = ConstructionColors.OnSurface.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (project.projectName == projectText) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = ConstructionColors.SafetyOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Show message if no projects match
                    if (filteredProjects.isEmpty() && projectText.isNotBlank()) {
                        item {
                            Text(
                                text = "Creating new project: \"$projectText\"",
                                color = ConstructionColors.SafetyOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Security and compliance notice
 */
@Composable
private fun SecurityNoticeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.SurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = ConstructionColors.SafetyGreen,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Your data is secured and OSHA compliant. All safety documentation meets industry standards.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ConstructionColors.OnSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Large start button optimized for construction gloves
 */
@Composable
private fun StartDocumentationButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp), // Large touch target for gloves
        colors = ButtonDefaults.buttonColors(
            containerColor = ConstructionColors.SafetyOrange,
            disabledContainerColor = ConstructionColors.SurfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = if (enabled) "Start" else "Enter Details",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompanyProjectEntryScreenPreview() {
    MaterialTheme {
        CompanyProjectEntryScreen(
            onNavigateToCamera = { _, _ -> }
        )
    }
}