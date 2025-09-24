package com.hazardhawk.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hazardhawk.security.RobustSecureStorageService
import com.hazardhawk.security.storage.*
import com.hazardhawk.ui.theme.ConstructionTheme
import kotlinx.coroutines.launch

/**
 * Settings screen for storage management and API key configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Storage service state
    var storageService by remember { 
        mutableStateOf<RobustSecureStorageService?>(null)
    }
    var healthStatus by remember {
        mutableStateOf<StorageHealthStatus?>(null)
    }
    var storageProviders by remember {
        mutableStateOf<List<StorageProviderInfo>>(emptyList())
    }
    var manualEntryRequest by remember {
        mutableStateOf<ManualEntryRequest?>(null)
    }
    
    // UI state
    var isLoading by remember { mutableStateOf(true) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }
    
    // Initialize storage service
    LaunchedEffect(Unit) {
        val service = RobustSecureStorageService(context)
        storageService = service
        
        // Initialize and get status
        service.initialize()
        healthStatus = service.performHealthCheck()
        storageProviders = service.getStorageProviderInfo()
        
        // Check if API key exists
        apiKey = service.getString("gemini_api_key") ?: ""
        
        isLoading = false
    }
    
    // Handle manual entry requests
    LaunchedEffect(storageService) {
        storageService?.let { service ->
            service.manualEntryRequired.collect { request ->
                manualEntryRequest = request
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage & Security") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                storageService?.let { service ->
                                    healthStatus = service.performHealthCheck()
                                    storageProviders = service.getStorageProviderInfo()
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Storage Health Status
                item {
                    healthStatus?.let { status ->
                        StorageHealthCard(
                            healthStatus = status,
                            onRetry = {
                                scope.launch {
                                    storageService?.forceFailover()
                                    healthStatus = storageService?.performHealthCheck()
                                }
                            },
                            onViewDetails = {
                                // Show detailed storage info
                            }
                        )
                    }
                }
                
                // API Key Management
                item {
                    ApiKeyManagementCard(
                        hasApiKey = apiKey.isNotBlank(),
                        currentSecurityLevel = healthStatus?.currentLevel ?: StorageSecurityLevel.NONE,
                        onSetApiKey = { showApiKeyDialog = true },
                        onTestApiKey = {
                            scope.launch {
                                // Test the API key
                            }
                        },
                        onClearApiKey = {
                            scope.launch {
                                storageService?.remove("gemini_api_key")
                                apiKey = ""
                            }
                        }
                    )
                }
                
                // Storage Provider Status
                item {
                    Text(
                        text = "Storage Providers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    StorageProviderStatusList(
                        providers = storageProviders
                    )
                }
                
                // Emergency Actions
                item {
                    EmergencyActionsCard(
                        onResetStorage = {
                            scope.launch {
                                // Reset all storage
                                val service = RobustSecureStorageService(context)
                                service.initialize()
                                storageService = service
                                healthStatus = service.performHealthCheck()
                            }
                        },
                        onManualEntry = {
                            showApiKeyDialog = true
                        }
                    )
                }
            }
        }
    }
    
    // Manual API Key Entry Dialog
    if (showApiKeyDialog) {
        ApiKeyEntryDialog(
            currentValue = apiKey,
            onValueSet = { newKey ->
                scope.launch {
                    if (storageService?.setString("gemini_api_key", newKey) == true) {
                        apiKey = newKey
                    }
                }
                showApiKeyDialog = false
            },
            onDismiss = {
                showApiKeyDialog = false
            }
        )
    }
    
    // Handle automatic manual entry requests
    manualEntryRequest?.let { request ->
        ManualApiKeyEntryDialog(
            request = request,
            onValueEntered = { value ->
                scope.launch {
                    storageService?.completeManualEntry(request.id, value)
                    manualEntryRequest = null
                }
            },
            onSkip = {
                scope.launch {
                    storageService?.skipManualEntry(request.id)
                    manualEntryRequest = null
                }
            },
            onDismiss = {
                manualEntryRequest = null
            }
        )
    }
}

/**
 * API Key Management Card
 */
@Composable
fun ApiKeyManagementCard(
    hasApiKey: Boolean,
    currentSecurityLevel: StorageSecurityLevel,
    onSetApiKey: () -> Unit,
    onTestApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
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
                Text(
                    text = "API Key Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                StorageSecurityIndicator(
                    securityLevel = currentSecurityLevel
                )
            }
            
            Text(
                text = if (hasApiKey) {
                    "API key is configured and stored securely"
                } else {
                    "No API key configured. AI analysis features will be unavailable."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSetApiKey,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (hasApiKey) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (hasApiKey) "Update" else "Set Key")
                }
                
                if (hasApiKey) {
                    OutlinedButton(
                        onClick = onTestApiKey,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test")
                    }
                    
                    OutlinedButton(
                        onClick = onClearApiKey,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }
        }
    }
}

/**
 * Emergency Actions Card
 */
@Composable
fun EmergencyActionsCard(
    onResetStorage: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Emergency Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "Use these actions if all storage systems fail or need to be reset.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetStorage,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Storage")
                }
                
                OutlinedButton(
                    onClick = onManualEntry,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Input,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual Entry")
                }
            }
        }
    }
}

/**
 * Simple API Key Entry Dialog
 */
@Composable
fun ApiKeyEntryDialog(
    currentValue: String,
    onValueSet: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKey by remember { mutableStateOf(currentValue) }
    var showError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Enter API Key")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter your Google Gemini API key for AI photo analysis.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { 
                        apiKey = it
                        showError = false
                    },
                    label = { Text("API Key") },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Please enter a valid API key") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (apiKey.isBlank()) {
                        showError = true
                    } else {
                        onValueSet(apiKey)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

@Preview
@Composable
fun StorageSettingsScreenPreview() {
    ConstructionTheme {
        StorageSettingsScreen(
            onBack = {}
        )
    }
}