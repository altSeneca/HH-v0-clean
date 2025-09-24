package com.hazardhawk.tags

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hazardhawk.ui.theme.HazardHawkTheme
import kotlinx.coroutines.launch

/**
 * Example activity demonstrating the improved tag selection system
 * with proper multi-selection, navigation, and user feedback.
 */
class ImprovedTagManagementExample : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HazardHawkTheme {
                ImprovedTagManagementScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedTagManagementScreen() {
    val viewModel: ImprovedTagSelectionViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    var showTagDialog by remember { mutableStateOf(false) }
    var selectedTagsSummary by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentScreen by remember { mutableStateOf(Screen.PHOTO_CAPTURE) }
    
    // Set up navigation callback
    LaunchedEffect(viewModel) {
        viewModel.onNavigateToNextStep = { tags ->
            selectedTagsSummary = tags
            currentScreen = Screen.TAG_DETAILS
            showTagDialog = false
            Toast.makeText(
                context,
                "Selected ${tags.size} hazards. Proceeding to details...",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            Screen.PHOTO_CAPTURE -> "Photo Capture"
                            Screen.TAG_SELECTION -> "Tag Selection"
                            Screen.TAG_DETAILS -> "Hazard Details"
                            Screen.SUBMIT -> "Submit Report"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    if (currentScreen != Screen.PHOTO_CAPTURE) {
                        IconButton(
                            onClick = {
                                currentScreen = when (currentScreen) {
                                    Screen.TAG_DETAILS -> Screen.TAG_SELECTION
                                    Screen.SUBMIT -> Screen.TAG_DETAILS
                                    else -> Screen.PHOTO_CAPTURE
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                Screen.PHOTO_CAPTURE -> {
                    PhotoCaptureScreen(
                        onPhotoTaken = {
                            currentScreen = Screen.TAG_SELECTION
                            showTagDialog = true
                        }
                    )
                }
                
                Screen.TAG_SELECTION -> {
                    // This screen is shown when dialog is dismissed
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Tag selection in progress...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { showTagDialog = true }
                        ) {
                            Text("Resume Tag Selection")
                        }
                    }
                }
                
                Screen.TAG_DETAILS -> {
                    HazardDetailsScreen(
                        selectedTags = selectedTagsSummary,
                        onContinue = {
                            currentScreen = Screen.SUBMIT
                        }
                    )
                }
                
                Screen.SUBMIT -> {
                    SubmitReportScreen(
                        onSubmit = {
                            Toast.makeText(
                                context,
                                "Report submitted successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                            // Reset to start
                            currentScreen = Screen.PHOTO_CAPTURE
                            selectedTagsSummary = emptyList()
                        }
                    )
                }
            }
            
            // Tag Selection Dialog
            if (showTagDialog && currentScreen == Screen.TAG_SELECTION) {
                ImprovedTagSelectionDialog(
                    state = state,
                    onComplianceToggle = viewModel::toggleComplianceStatus,
                    onTagToggle = viewModel::toggleTagSelection,
                    onSelectAll = viewModel::selectAll,
                    onClearAll = viewModel::clearAll,
                    onSearch = viewModel::searchTags,
                    onCreateCustomTag = viewModel::createCustomTag,
                    onConfirm = viewModel::confirmSelection,
                    onDismiss = {
                        showTagDialog = false
                        viewModel.dismiss()
                        // Go back to photo capture if dismissed
                        currentScreen = Screen.PHOTO_CAPTURE
                    }
                )
            }
        }
    }
}

@Composable
fun PhotoCaptureScreen(
    onPhotoTaken: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Simulated camera preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f/3f),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onPhotoTaken,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Capture Photo",
                style = MaterialTheme.typography.labelLarge,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Step 1: Capture a photo of the hazard",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HazardDetailsScreen(
    selectedTags: List<String>,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Hazard Details",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${selectedTags.size} Hazards Tagged",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(selectedTags) { tagId ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "• ",
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = tagId.replace("_", " ").replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Additional details form would go here
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter hazard location") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("Additional Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("Describe the hazard...") },
            maxLines = 5
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "Step 3: Add hazard details",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Continue to Submit",
                style = MaterialTheme.typography.labelLarge,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SubmitReportScreen(
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Ready to Submit",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your hazard report is ready to be submitted",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Submit Report",
                style = MaterialTheme.typography.labelLarge,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Step 4: Submit report",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

enum class Screen {
    PHOTO_CAPTURE,
    TAG_SELECTION,
    TAG_DETAILS,
    SUBMIT
}