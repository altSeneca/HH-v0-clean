package com.hazardhawk

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.hazardhawk.models.AnalysisOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import coil3.compose.AsyncImage
import com.hazardhawk.ui.components.StandardDialog
import com.hazardhawk.ui.components.*
import com.hazardhawk.ui.gallery.AIConfidenceBadge
import com.hazardhawk.ui.theme.ConstructionColors
import com.hazardhawk.models.*
import kotlinx.coroutines.launch
import java.io.File

// Type alias for compatibility
typealias RiskLevel = Severity

// Safety assessment types
enum class SafetyAssessmentType {
    GOOD_PRACTICE,
    NEEDS_IMPROVEMENT,
    UNDECIDED
}

// Dialog step tracking
enum class AssessmentStep {
    QUALITY_CHECK,
    CATEGORIZATION,
    COMPLETION
}

// Data classes for safety items
data class SafetyItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val severity: SafetySeverity = SafetySeverity.LOW,
    val oshaCode: String? = null
)

enum class SafetySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

// Main safety assessment dialog
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SafetyPhotoAssessmentDialog(
    photoPath: String?,
    onDismiss: () -> Unit,
    onComplete: (SafetyAssessmentType, List<SafetyItem>, String) -> Unit,
    apiKey: String? = null,
    aiSuggestions: List<SafetyItem> = emptyList(),
    aiConfidence: Float = 0f,
    aiAnalysisComplete: Boolean = false
) {
    var currentStep by remember { mutableStateOf(AssessmentStep.QUALITY_CHECK) }
    var assessmentType by remember { mutableStateOf(SafetyAssessmentType.UNDECIDED) }
    var selectedItems by remember { mutableStateOf(setOf<SafetyItem>()) }
    var notes by remember { mutableStateOf("") }
    
    // AI Analysis state
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<SafetyAnalysis?>(null) }
    var analysisError by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Perform AI analysis when dialog opens
    LaunchedEffect(photoPath) {
        if (!photoPath.isNullOrBlank() && analysisResult == null && !isAnalyzing) {
            isAnalyzing = true
            analysisError = null
            
            coroutineScope.launch {
                try {
                    Log.d("SafetyAssessment", "Starting AI analysis for: $photoPath")
                    val analysis = SafetyAnalysis.fromPhotoPath(
                        photoPath = photoPath,
                        apiKey = apiKey ?: "default-key",
                        analysisOptions = com.hazardhawk.models.AnalysisOptions(
                            workType = com.hazardhawk.domain.entities.WorkType.GENERAL_CONSTRUCTION,
                            includeOSHACompliance = true,
                            confidenceThreshold = 0.7f
                        )
                    )
                    analysisResult = analysis
                    Log.d("SafetyAssessment", "AI analysis completed: ${analysis.hazards.size} hazards found")
                } catch (e: Exception) {
                    Log.e("SafetyAssessment", "AI analysis failed: ${e.message}", e)
                    analysisError = e.message
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }
    
    StandardDialog(
        onDismissRequest = onDismiss,
        content = {
            Crossfade(
                targetState = currentStep,
                label = "assessment_step"
            ) { step ->
                when (step) {
                    AssessmentStep.QUALITY_CHECK -> {
                        PhotoQualityCheckContent(
                            photoPath = photoPath,
                            onGoodPractice = {
                                Log.d("SafetyAssessment", "Good Practice button clicked!")
                                assessmentType = SafetyAssessmentType.GOOD_PRACTICE
                                currentStep = AssessmentStep.CATEGORIZATION
                            },
                            onNeedsImprovement = {
                                Log.d("SafetyAssessment", "Needs Improvement button clicked!")
                                assessmentType = SafetyAssessmentType.NEEDS_IMPROVEMENT
                                currentStep = AssessmentStep.CATEGORIZATION
                            },
                            onCancel = onDismiss,
                            aiAnalysisComplete = !isAnalyzing && analysisResult != null,
                            aiConfidence = analysisResult?.aiConfidence ?: 0f,
                            isAnalyzing = isAnalyzing,
                            analysisResult = analysisResult,
                            analysisError = analysisError
                        )
                    }
                    AssessmentStep.CATEGORIZATION -> {
                        when (assessmentType) {
                            SafetyAssessmentType.GOOD_PRACTICE -> {
                                val goodPracticeAISuggestions = analysisResult?.let { analysis ->
                                    // Convert low-severity hazards to safety items for good practices
                                    analysis.hazards.filter { 
                                        it.severity == Severity.LOW 
                                    }.map { hazard ->
                                        SafetyItem(
                                            id = hazard.id,
                                            label = hazard.description,
                                            icon = mapHazardToIcon(hazard.type.toCategory()),
                                            severity = SafetySeverity.LOW,
                                            oshaCode = null
                                        )
                                    }
                                } ?: aiSuggestions.filter { it.severity == SafetySeverity.LOW }
                                
                                GoodPracticeSelectionContent(
                                    photoPath = photoPath,
                                    selectedItems = selectedItems,
                                    onItemToggle = { item ->
                                        selectedItems = if (selectedItems.contains(item)) {
                                            selectedItems - item
                                        } else {
                                            selectedItems + item
                                        }
                                    },
                                    onBack = { currentStep = AssessmentStep.QUALITY_CHECK },
                                    onNext = { 
                                        currentStep = AssessmentStep.COMPLETION 
                                    },
                                    aiSuggestions = goodPracticeAISuggestions,
                                    aiConfidence = analysisResult?.aiConfidence ?: aiConfidence
                                )
                            }
                            SafetyAssessmentType.NEEDS_IMPROVEMENT -> {
                                val violationAISuggestions = analysisResult?.let { analysis ->
                                    // Convert medium/high/critical hazards to safety violations
                                    analysis.hazards.filter { 
                                        it.severity in setOf(Severity.MEDIUM, Severity.HIGH, Severity.CRITICAL)
                                    }.map { hazard ->
                                        val oshaViolation = analysis.oshaCodes.firstOrNull { 
                                            it.description.contains(hazard.description, ignoreCase = true)
                                        }
                                        SafetyItem(
                                            id = hazard.id,
                                            label = hazard.description,
                                            icon = mapHazardToIcon(hazard.type.toCategory()),
                                            severity = when (hazard.severity) {
                                                Severity.MEDIUM -> SafetySeverity.MEDIUM
                                                Severity.HIGH -> SafetySeverity.HIGH
                                                Severity.CRITICAL -> SafetySeverity.CRITICAL
                                                else -> SafetySeverity.LOW
                                            },
                                            oshaCode = oshaViolation?.code
                                        )
                                    }
                                } ?: aiSuggestions.filter { 
                                    it.severity in setOf(SafetySeverity.MEDIUM, SafetySeverity.HIGH, SafetySeverity.CRITICAL)
                                }
                                
                                SafetyViolationSelectionContent(
                                    photoPath = photoPath,
                                    selectedItems = selectedItems,
                                    onItemToggle = { item ->
                                        selectedItems = if (selectedItems.contains(item)) {
                                            selectedItems - item
                                        } else {
                                            selectedItems + item
                                        }
                                    },
                                    onBack = { currentStep = AssessmentStep.QUALITY_CHECK },
                                    onNext = { 
                                        currentStep = AssessmentStep.COMPLETION 
                                    },
                                    aiSuggestions = violationAISuggestions,
                                    aiConfidence = analysisResult?.aiConfidence ?: aiConfidence
                                )
                            }
                            SafetyAssessmentType.UNDECIDED -> {
                                // Should not happen, but handle gracefully
                                LaunchedEffect(Unit) {
                                    currentStep = AssessmentStep.QUALITY_CHECK
                                }
                            }
                        }
                    }
                    AssessmentStep.COMPLETION -> {
                        CompletionContent(
                            photoPath = photoPath,
                            assessmentType = assessmentType,
                            selectedItems = selectedItems,
                            notes = notes,
                            onNotesChange = { notes = it },
                            onBack = { currentStep = AssessmentStep.CATEGORIZATION },
                            onComplete = {
                                onComplete(assessmentType, selectedItems.toList(), notes)
                            }
                        )
                    }
                }
            }
        },
        buttons = {
            // No buttons at the main dialog level since each step manages its own actions
        }
    )
}

// Step 1: Photo Quality Check
@Composable
fun PhotoQualityCheckContent(
    photoPath: String?,
    onGoodPractice: () -> Unit,
    onNeedsImprovement: () -> Unit,
    onCancel: () -> Unit,
    aiAnalysisComplete: Boolean = false,
    aiConfidence: Float = 0f,
    isAnalyzing: Boolean = false,
    analysisResult: SafetyAnalysis? = null,
    analysisError: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            // Header with AI Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Safety Assessment",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ConstructionColors.SafetyOrange
                        )
                    )
                    
                    // AI Status indicator - Real-time analysis status
                    when {
                        isAnalyzing -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = ConstructionColors.SafetyOrange,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "AI Analyzing...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ConstructionColors.SafetyOrange
                                )
                            }
                        }
                        analysisError != null -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Analysis Error",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Red
                                )
                                Text(
                                    text = "Analysis Offline",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Red
                                )
                            }
                        }
                        aiAnalysisComplete && aiConfidence > 0f -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Analysis Complete",
                                    modifier = Modifier.size(16.dp),
                                    tint = ConstructionColors.SafetyOrange
                                )
                                AIConfidenceBadge(confidence = aiConfidence)
                                analysisResult?.let { analysis ->
                                    Text(
                                        text = "${analysis.hazards.size} items detected",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Photo preview
            photoPath?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    AsyncImage(
                        model = File(it),
                        contentDescription = "Captured photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Question
            Text(
                text = "What does this photo show?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Help us categorize this safety observation",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Choice buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Good Practice Button
                AssessmentChoiceButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF4CAF50),
                    title = "Good Practice",
                    subtitle = "Safe work being performed",
                    onClick = onGoodPractice
                )
                
                // Needs Improvement Button
                AssessmentChoiceButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Warning,
                    iconColor = Color(0xFFFF9800),
                    title = "Needs\nImprovement",
                    subtitle = "Hazards or\nviolations present",
                    onClick = onNeedsImprovement
                )
            }
    }
}

@Composable
fun AssessmentChoiceButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    // Use OutlinedButton with custom content for better touch handling
    OutlinedButton(
        onClick = { 
            Log.d("SafetyAssessment", "AssessmentChoiceButton clicked: $title")
            onClick() 
        },
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = iconColor.copy(alpha = 0.1f),
            contentColor = Color.Black
        ),
        border = BorderStroke(2.dp, iconColor.copy(alpha = 0.7f)),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = iconColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Gray
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Step 2A: Good Practice Selection
@Composable
fun GoodPracticeSelectionContent(
    photoPath: String?,
    selectedItems: Set<SafetyItem>,
    onItemToggle: (SafetyItem) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    aiSuggestions: List<SafetyItem> = emptyList(),
    aiConfidence: Float = 0f
) {
    val goodPractices = remember {
        listOf(
            SafetyItem("gp1", "PPE Properly Worn", Icons.Default.Security, SafetySeverity.LOW),
            SafetyItem("gp2", "Following Safety Procedures", Icons.Default.CheckCircle, SafetySeverity.LOW),
            SafetyItem("gp3", "Proper Tool Usage", Icons.Default.Build, SafetySeverity.LOW),
            SafetyItem("gp4", "Clean & Organized Workspace", Icons.Default.CleaningServices, SafetySeverity.LOW),
            SafetyItem("gp5", "Safety Barriers in Place", Icons.Default.Warning, SafetySeverity.LOW),
            SafetyItem("gp6", "Proper Lifting Technique", Icons.Default.FitnessCenter, SafetySeverity.LOW),
            SafetyItem("gp7", "Equipment Inspection Completed", Icons.Default.Search, SafetySeverity.LOW),
            SafetyItem("gp8", "Lockout/Tagout Procedure", Icons.Default.Lock, SafetySeverity.LOW),
            SafetyItem("gp9", "Fall Protection Used", Icons.Default.Height, SafetySeverity.LOW),
            SafetyItem("gp10", "Emergency Equipment Accessible", Icons.Default.LocalHospital, SafetySeverity.LOW)
        )
    }
    
    Column {
        // AI Suggestions Banner
        if (aiSuggestions.isNotEmpty() && aiConfidence > 0.6f) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ConstructionColors.SafetyOrange.copy(alpha = 0.1f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = ConstructionColors.SafetyOrange.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Suggestions",
                            modifier = Modifier.size(20.dp),
                            tint = ConstructionColors.SafetyOrange
                        )
                        
                        Text(
                            text = "AI Suggestions",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = ConstructionColors.SafetyOrange
                        )
                        
                        AIConfidenceBadge(confidence = aiConfidence)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "AI detected ${aiSuggestions.size} potential good practices. Tap to auto-select:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(aiSuggestions) { suggestion ->
                            Surface(
                                onClick = { onItemToggle(suggestion) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (suggestion in selectedItems) 
                                    ConstructionColors.SafetyGreen.copy(alpha = 0.2f)
                                else 
                                    ConstructionColors.SafetyOrange.copy(alpha = 0.1f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (suggestion in selectedItems)
                                        ConstructionColors.SafetyGreen
                                    else
                                        ConstructionColors.SafetyOrange.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = suggestion.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (suggestion in selectedItems)
                                            ConstructionColors.SafetyGreen
                                        else
                                            ConstructionColors.SafetyOrange
                                    )
                                    
                                    Text(
                                        text = suggestion.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 12.sp
                                        ),
                                        color = if (suggestion in selectedItems)
                                            ConstructionColors.SafetyGreen
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        SafetyItemSelectionContent(
            title = "Positive Safety Observations",
            subtitle = "Select all safe practices observed",
            photoPath = photoPath,
            items = goodPractices,
            selectedItems = selectedItems,
            positiveMode = true,
            onItemToggle = onItemToggle,
            onBack = onBack,
            onNext = onNext
        )
    }
}

// Step 2B: Safety Violation Selection
@Composable
fun SafetyViolationSelectionContent(
    photoPath: String?,
    selectedItems: Set<SafetyItem>,
    onItemToggle: (SafetyItem) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    aiSuggestions: List<SafetyItem> = emptyList(),
    aiConfidence: Float = 0f
) {
    val violations = remember {
        listOf(
            SafetyItem("v1", "Missing PPE", Icons.Default.PersonOff, SafetySeverity.HIGH, "1926.95"),
            SafetyItem("v2", "Fall Hazard", Icons.Default.Height, SafetySeverity.CRITICAL, "1926.501"),
            SafetyItem("v3", "Electrical Hazard", Icons.Default.Bolt, SafetySeverity.CRITICAL, "1926.416"),
            SafetyItem("v4", "Struck-By Hazard", Icons.Default.Warning, SafetySeverity.HIGH, "1926.601"),
            SafetyItem("v5", "Caught-Between Hazard", Icons.Default.ContentCut, SafetySeverity.HIGH, "1926.600"),
            SafetyItem("v6", "Slip/Trip Hazard", Icons.Default.Report, SafetySeverity.MEDIUM, "1926.25"),
            SafetyItem("v7", "Improper Scaffolding", Icons.Default.Layers, SafetySeverity.HIGH, "1926.451"),
            SafetyItem("v8", "Missing Guards/Barriers", Icons.Default.Block, SafetySeverity.HIGH, "1926.502"),
            SafetyItem("v9", "Chemical Exposure Risk", Icons.Default.Science, SafetySeverity.HIGH, "1926.59"),
            SafetyItem("v10", "Fire Hazard", Icons.Default.LocalFireDepartment, SafetySeverity.CRITICAL, "1926.151"),
            SafetyItem("v11", "Confined Space Violation", Icons.Default.SpaceBar, SafetySeverity.CRITICAL, "1926.1200"),
            SafetyItem("v12", "Housekeeping Issue", Icons.Default.CleaningServices, SafetySeverity.LOW, "1926.25")
        )
    }
    
    SafetyItemSelectionContent(
        title = "Safety Concerns Identified",
        subtitle = "Select all hazards and violations observed",
        photoPath = photoPath,
        items = violations,
        selectedItems = selectedItems,
        positiveMode = false,
        onItemToggle = onItemToggle,
        onBack = onBack,
        onNext = onNext
    )
}

@Composable
fun SafetyItemSelectionContent(
    title: String,
    subtitle: String,
    photoPath: String?,
    items: List<SafetyItem>,
    selectedItems: Set<SafetyItem>,
    positiveMode: Boolean,
    onItemToggle: (SafetyItem) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp, max = 600.dp)
    ) {
            // Header with photo thumbnail
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Small photo thumbnail
                photoPath?.let {
                    Card(
                        modifier = Modifier.size(60.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        AsyncImage(
                            model = File(it),
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (positiveMode) Color(0xFF4CAF50) else Color(0xFFFF8C00),
                            lineHeight = 28.sp
                        ),
                        maxLines = 2
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 18.sp
                        ),
                        color = Color.Gray,
                        maxLines = 2
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Selected count indicator
            if (selectedItems.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = (if (positiveMode) Color(0xFF4CAF50) else Color(0xFFFF8C00)).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${selectedItems.size} item${if (selectedItems.size != 1) "s" else ""} selected",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (positiveMode) Color(0xFF4CAF50) else Color(0xFFFF8C00)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Items list - Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { item ->
                    SafetyItemCard(
                        item = item,
                        isSelected = selectedItems.contains(item),
                        positiveMode = positiveMode,
                        onClick = { onItemToggle(item) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back")
                }
                
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (positiveMode) Color(0xFF4CAF50) else Color(0xFFFF8C00)
                    ),
                    enabled = selectedItems.isNotEmpty()
                ) {
                    Text("Continue")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
    }
}

@Composable
fun SafetyItemCard(
    item: SafetyItem,
    isSelected: Boolean,
    positiveMode: Boolean,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected && positiveMode -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                isSelected && !positiveMode -> when (item.severity) {
                    SafetySeverity.CRITICAL -> Color.Red.copy(alpha = 0.2f)
                    SafetySeverity.HIGH -> Color(0xFFFF8C00).copy(alpha = 0.15f)
                    SafetySeverity.MEDIUM -> Color(0xFFFFC107).copy(alpha = 0.15f)
                    SafetySeverity.LOW -> Color(0xFF2196F3).copy(alpha = 0.1f)
                }
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = when {
                isSelected && positiveMode -> Color(0xFF4CAF50)
                isSelected && !positiveMode -> when (item.severity) {
                    SafetySeverity.CRITICAL -> Color.Red
                    SafetySeverity.HIGH -> Color(0xFFFF8C00)
                    SafetySeverity.MEDIUM -> Color(0xFFFFC107)
                    SafetySeverity.LOW -> Color(0xFF2196F3)
                }
                else -> Color.Gray.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (positiveMode) Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else when (item.severity) {
                            SafetySeverity.CRITICAL -> Color.Red.copy(alpha = 0.2f)
                            SafetySeverity.HIGH -> Color(0xFFFF8C00).copy(alpha = 0.2f)
                            SafetySeverity.MEDIUM -> Color(0xFFFFC107).copy(alpha = 0.2f)
                            SafetySeverity.LOW -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (positiveMode) Color(0xFF4CAF50)
                        else when (item.severity) {
                            SafetySeverity.CRITICAL -> Color.Red
                            SafetySeverity.HIGH -> Color(0xFFFF8C00)
                            SafetySeverity.MEDIUM -> Color(0xFFFFC107)
                            SafetySeverity.LOW -> Color(0xFF2196F3)
                        }
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = 22.sp
                    ),
                    maxLines = 2
                )
                item.oshaCode?.let { code ->
                    Text(
                        text = "OSHA $code",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            
            // Severity badge for violations
            if (!positiveMode && item.severity != SafetySeverity.LOW) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (item.severity) {
                            SafetySeverity.CRITICAL -> Color.Red
                            SafetySeverity.HIGH -> Color(0xFFFF8C00)
                            SafetySeverity.MEDIUM -> Color(0xFFFFC107)
                            SafetySeverity.LOW -> Color(0xFF2196F3)
                        }
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.severity.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = if (positiveMode) Color(0xFF4CAF50) else Color(0xFFFF8C00),
                    uncheckedColor = Color.Gray
                )
            )
        }
    }
}

// Step 3: Completion and Notes
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CompletionContent(
    photoPath: String?,
    assessmentType: SafetyAssessmentType,
    selectedItems: Set<SafetyItem>,
    notes: String,
    onNotesChange: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val notesFocusRequester = remember { FocusRequester() }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp, max = 600.dp)
            .verticalScroll(rememberScrollState())
    ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (assessmentType == SafetyAssessmentType.GOOD_PRACTICE) 
                        Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (assessmentType == SafetyAssessmentType.GOOD_PRACTICE) 
                        Color(0xFF4CAF50) else Color(0xFFFF8C00)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Review & Submit",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (assessmentType == SafetyAssessmentType.GOOD_PRACTICE)
                            "Good Safety Practices Observed"
                        else "Safety Improvements Needed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Summary card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Assessment Summary",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Photo thumbnail
                    photoPath?.let {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            AsyncImage(
                                model = File(it),
                                contentDescription = "Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Selected items summary
                    Text(
                        text = "${selectedItems.size} item${if (selectedItems.size != 1) "s" else ""} identified:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    selectedItems.take(3).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (assessmentType == SafetyAssessmentType.GOOD_PRACTICE)
                                    Color(0xFF4CAF50) else Color(0xFFFF8C00)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    if (selectedItems.size > 3) {
                        Text(
                            text = "...and ${selectedItems.size - 3} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Notes section
            Text(
                text = "Additional Notes (Optional)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .focusRequester(notesFocusRequester),
                placeholder = { 
                    Text(
                        "Add any additional observations or context...",
                        color = Color.Gray
                    )
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF8C00),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back")
                }
                
                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF8C00)
                    )
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Assessment")
                }
            }
    }
}

/**
 * Helper function to map hazard categories to appropriate Material icons
 */
private fun mapHazardToIcon(category: HazardCategory): ImageVector {
    return when (category) {
        HazardCategory.PPE -> Icons.Default.Security
        HazardCategory.FALL_PROTECTION -> Icons.Default.Height
        HazardCategory.ELECTRICAL -> Icons.Default.Bolt
        HazardCategory.CHEMICAL -> Icons.Default.Science
        HazardCategory.FIRE -> Icons.Default.LocalFireDepartment
        HazardCategory.MACHINERY -> Icons.Default.Build
        HazardCategory.HOUSEKEEPING -> Icons.Default.CleaningServices
        HazardCategory.GENERAL -> Icons.Default.Warning
    }
}