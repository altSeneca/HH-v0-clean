package com.hazardhawk.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*

/**
 * AI Integration Example and Usage Guide
 * 
 * This file demonstrates how to integrate the AI components with camera and settings screens.
 * Shows the complete user experience flow with construction-optimized design.
 */

/**
 * Example: AI Configuration integrated into Settings Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsIntegrationExample(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // Example state management
    var aiMode by remember { mutableStateOf(AIAnalysisMode.LOCAL) }
    var apiKey by remember { mutableStateOf("") }
    var isApiKeyValid by remember { mutableStateOf(false) }
    
    // Validate API key when it changes
    LaunchedEffect(apiKey) {
        isApiKeyValid = validateAPIKey(apiKey).isEmpty()
    }
    
    AIConfigurationScreen(
        currentMode = aiMode,
        onModeChange = { aiMode = it },
        apiKey = apiKey,
        onApiKeyChange = { apiKey = it },
        isApiKeyValid = isApiKeyValid,
        onBack = onNavigateBack,
        onSave = {
            // Save configuration logic here
            // e.g., saveAIConfiguration(aiMode, apiKey)
        },
        modifier = modifier
    )
}

/**
 * Example: AI Progress integrated into Camera Screen
 */
@Composable
fun AICameraIntegrationExample(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    var analysisState by remember { mutableStateOf(AIAnalysisState.IDLE) }
    var currentStep by remember { mutableStateOf(AIAnalysisStep.PREPARING) }
    var progress by remember { mutableFloatStateOf(0f) }
    var aiMode by remember { mutableStateOf(AIAnalysisMode.LOCAL) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Camera preview would go here
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Camera Preview Area")
            }
        }
        
        // AI Analysis Progress (shown when analyzing)
        if (analysisState == AIAnalysisState.ANALYZING) {
            IntelligentAIProgress(
                currentStep = currentStep,
                progress = progress,
                analysisMode = aiMode,
                onCancel = {
                    analysisState = AIAnalysisState.IDLE
                    progress = 0f
                }
            )
        }
        
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlikkerSecondaryButton(
                onClick = { 
                    // Start analysis simulation
                    analysisState = AIAnalysisState.ANALYZING
                    scope.launch {
                        simulateAIAnalysis(
                            onStepChange = { step -> currentStep = step },
                            onProgressChange = { prog -> progress = prog },
                            onComplete = { analysisState = AIAnalysisState.COMPLETED }
                        )
                    }
                },
                text = "Analyze Photo",
                icon = Icons.Default.Psychology,
                modifier = Modifier.weight(1f)
            )
            
            FlikkerPrimaryButton(
                onClick = { /* Capture photo */ },
                text = "Capture",
                icon = Icons.Default.CameraAlt,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Example: AI Results with Confidence Indicators
 */
@Composable
fun AIResultsIntegrationExample(
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
            text = "Safety Analysis Results",
            style = MaterialTheme.typography.headlineSmall,
            color = com.hazardhawk.ui.theme.ConstructionColors.SafetyOrange
        )
        
        // Example hazards with different confidence levels
        ConfidenceIndicator(
            confidence = 0.92f,
            hazardType = "Fall Hazard - Unguarded Edge",
            oshaReference = "1926.501(b)(1)",
            analysisMode = AIAnalysisMode.CLOUD,
            showDetails = false,
            onToggleDetails = { /* Toggle details */ }
        )
        
        ConfidenceIndicator(
            confidence = 0.78f,
            hazardType = "Missing PPE - Hard Hat",
            oshaReference = "1926.95(a)",
            analysisMode = AIAnalysisMode.LOCAL,
            showDetails = false,
            onToggleDetails = { /* Toggle details */ }
        )
        
        ConfidenceIndicator(
            confidence = 0.45f,
            hazardType = "Potential Electrical Hazard",
            oshaReference = "1926.416(a)(1)",
            analysisMode = AIAnalysisMode.LOCAL,
            showDetails = false,
            onToggleDetails = { /* Toggle details */ }
        )
        
        // Action buttons
        Spacer(modifier = Modifier.height(8.dp))
        
        FlikkerPrimaryButton(
            onClick = { /* Generate report */ },
            text = "Generate Safety Report",
            icon = Icons.Default.Description,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Example: Error Handling Integration
 */
@Composable
fun AIErrorHandlingIntegrationExample(
    modifier: Modifier = Modifier
) {
    var showError by remember { mutableStateOf(true) }
    var errorType by remember { mutableStateOf("network") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AI Error Handling Examples",
            style = MaterialTheme.typography.headlineSmall,
            color = com.hazardhawk.ui.theme.ConstructionColors.SafetyOrange
        )
        
        // Error type selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("network", "api_key", "quota", "processing").forEach { type ->
                FlikkerSecondaryButton(
                    onClick = { 
                        errorType = type
                        showError = true
                    },
                    text = type.replace("_", " ").uppercase(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Show error based on selected type
        if (showError) {
            val error = when (errorType) {
                "network" -> CommonAIErrors.networkError()
                "api_key" -> CommonAIErrors.invalidAPIKey()
                "quota" -> CommonAIErrors.quotaExceeded()
                "processing" -> CommonAIErrors.processingFailed()
                else -> CommonAIErrors.networkError()
            }
            
            AIErrorDisplay(
                error = error,
                onRetry = {
                    // Retry logic
                    showError = false
                },
                onDismiss = {
                    showError = false
                },
                onSwitchToLocal = {
                    // Switch to local AI
                    showError = false
                }
            )
        }
        
        // Compact error indicator example
        if (!showError) {
            Text(
                text = "Compact Error Indicator:",
                style = MaterialTheme.typography.titleMedium
            )
            
            CompactAIErrorIndicator(
                error = CommonAIErrors.networkError(),
                onClick = { showError = true }
            )
        }
    }
}

/**
 * Complete AI Integration Preview
 */
@Composable
fun CompleteAIIntegrationPreview(
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(0) }
    val tabs = listOf("Configuration", "Camera", "Results", "Errors")
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Tab selector
        ScrollableTabRow(
            selectedTabIndex = currentTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = com.hazardhawk.ui.theme.ConstructionColors.SafetyOrange
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = currentTab == index,
                    onClick = { currentTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        // Tab content
        when (currentTab) {
            0 -> AISettingsIntegrationExample()
            1 -> AICameraIntegrationExample()
            2 -> AIResultsIntegrationExample()
            3 -> AIErrorHandlingIntegrationExample()
        }
    }
}

/**
 * AI Analysis State Management
 */
enum class AIAnalysisState {
    IDLE,
    ANALYZING,
    COMPLETED,
    ERROR
}

/**
 * Simulate AI analysis progress for demo purposes
 */
suspend fun simulateAIAnalysis(
    onStepChange: (AIAnalysisStep) -> Unit,
    onProgressChange: (Float) -> Unit,
    onComplete: () -> Unit
) {
    // This would typically be replaced with actual AI analysis logic
    // For demo purposes, we simulate the steps
    
    val steps = AIAnalysisStep.entries.toList()
    
    // Simulate step progression
    withContext(Dispatchers.Main) {
        steps.forEach { step ->
            onStepChange(step)
            
            // Simulate progress within each step
            for (i in 0..100 step 10) {
                onProgressChange(i / 100f)
                delay(100)
            }
            
            delay(500)
        }
        
        onComplete()
    }
}

/**
 * Accessibility and UX Validation Notes
 * 
 * ✅ Construction-Optimized Design:
 * - All touch targets are ≥56dp (exceeds 44dp minimum)
 * - High contrast colors for outdoor visibility
 * - Large, readable text (≥16sp for body text)
 * - Proper spacing between interactive elements
 * 
 * ✅ 2-Tap Maximum Workflow:
 * - AI Configuration: Settings → AI Config → Save (2 taps)
 * - Camera Analysis: Camera → Analyze Button (2 taps)
 * - Error Recovery: Error Display → Retry/Fix (2 taps)
 * 
 * ✅ Educational Design:
 * - Progress indicators teach safety concepts
 * - Error messages guide users to solutions
 * - Confidence indicators explain AI results
 * - Help sections provide context and guidance
 * 
 * ✅ Accessibility Features:
 * - Semantic content descriptions for screen readers
 * - Proper color contrast ratios (WCAG AA compliant)
 * - Haptic feedback for important actions
 * - Keyboard navigation support
 * - Support for large text sizes
 * 
 * ✅ Construction Environment Considerations:
 * - Works with safety glasses and gloves
 * - Readable in bright outdoor lighting
 * - Dust-resistant UI (no small interactive elements)
 * - Quick recognition and action patterns
 * - Emergency-friendly error recovery flows
 */
