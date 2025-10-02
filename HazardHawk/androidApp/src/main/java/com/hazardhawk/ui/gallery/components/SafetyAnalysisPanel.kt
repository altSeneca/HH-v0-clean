package com.hazardhawk.ui.gallery.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazardhawk.ai.PhotoAnalysisWithTags
import com.hazardhawk.domain.entities.Photo
import com.hazardhawk.models.OSHAAnalysisResult
import com.hazardhawk.ui.gallery.state.*
import com.hazardhawk.tags.MobileTagManager
import com.hazardhawk.camera.MetadataSettingsManager
import com.hazardhawk.security.SecureKeyManager
import com.hazardhawk.performance.PhotoViewerPerformanceTracker
import org.koin.compose.koinInject

// Import for helper components
import com.hazardhawk.ui.gallery.components.SettingsPromptCard

// Construction-safe colors optimized for outdoor visibility
private val SafetyOrange = Color(0xFFFF6B35)
private val SafetyGreen = Color(0xFF10B981)
private val DangerRed = Color(0xFFEF4444)
private val CriticalRed = Color(0xFFB71C1C)

/**
 * Unified Safety Analysis Panel
 * Combines manual tagging, AI analysis, and OSHA compliance in one workflow
 *
 * Workflow: Manual Tags → AI Analysis → OSHA Mapping
 */
@Composable
fun SafetyAnalysisPanel(
    photo: Photo,
    analysisState: SafetyAnalysisState,
    oshaAnalysis: OSHAAnalysisResult?,
    isAnalyzingOSHA: Boolean,
    onAction: (SafetyAnalysisAction) -> Unit,
    onTagsUpdated: (String, List<String>) -> Unit,
    performanceTracker: PhotoViewerPerformanceTracker,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Get settings managers
    val metadataSettings = remember { MetadataSettingsManager(context) }
    val appSettings by metadataSettings.appSettings.collectAsStateWithLifecycle()
    val isAIAnalysisEnabled = appSettings.cameraSettings.aiAnalysisEnabled

    // Get secure key manager
    val secureKeyManager = remember { SecureKeyManager.getInstance(context) }
    val hasValidApiKey = remember { secureKeyManager.hasValidApiKey() }

    // Determine if analysis can be performed
    val canAnalyze = isAIAnalysisEnabled && hasValidApiKey && !analysisState.isAnalyzing
    val disabledReason = when {
        !isAIAnalysisEnabled -> "AI Analysis is disabled in settings"
        !hasValidApiKey -> "Gemini API key not configured"
        analysisState.isAnalyzing -> "Analysis in progress..."
        else -> null
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Section 1: Manual Tags
            ManualTagsSection(
                photo = photo,
                manualTags = analysisState.manualTags,
                onTagsUpdated = onTagsUpdated,
                onAddManualTag = { tag -> onAction(SafetyAnalysisAction.AddManualTag(tag)) },
                onRemoveManualTag = { tagId -> onAction(SafetyAnalysisAction.RemoveManualTag(tagId)) }
            )
        }

        item {
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }

        item {
            // Section 2: AI Analysis
            AIAnalysisSection(
                photo = photo,
                analysisState = analysisState,
                canAnalyze = canAnalyze,
                disabledReason = disabledReason,
                isAIAnalysisEnabled = isAIAnalysisEnabled,
                hasValidApiKey = hasValidApiKey,
                onStartAnalysis = { onAction(SafetyAnalysisAction.StartAIAnalysis) },
                onTagClick = { tag ->
                    val updatedTags = photo.tags + tag
                    onTagsUpdated(photo.id, updatedTags)
                },
                onToggleBoundingBoxes = { visible ->
                    onAction(SafetyAnalysisAction.SetBoundingBoxesVisible(visible))
                },
                performanceTracker = performanceTracker
            )
        }

        // Section 3: OSHA Compliance (only show if AI analysis completed)
        if (analysisState.aiAnalysis != null) {
            item {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }

            item {
                OSHAComplianceSection(
                    oshaAnalysis = oshaAnalysis,
                    isAnalyzing = isAnalyzingOSHA,
                    displayState = analysisState.oshaDisplay,
                    onStartOSHAAnalysis = { onAction(SafetyAnalysisAction.StartOSHAAnalysis) },
                    onToggleVisibility = { visible ->
                        onAction(SafetyAnalysisAction.SetOSHAVisible(visible))
                    },
                    onExpandStandard = { standardId, expanded ->
                        onAction(SafetyAnalysisAction.ExpandOSHAStandard(standardId, expanded))
                    }
                )
            }
        }
    }
}

@Composable
private fun ManualTagsSection(
    photo: Photo,
    manualTags: List<ManualHazardTag>,
    onTagsUpdated: (String, List<String>) -> Unit,
    onAddManualTag: (ManualHazardTag) -> Unit,
    onRemoveManualTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTagManager by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = SafetyGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Manual Safety Tags",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (manualTags.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SafetyGreen.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, SafetyGreen)
                        ) {
                            Text(
                                text = "${manualTags.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SafetyGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = { showTagManager = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Tags")
                }
            }

            // Display existing manual tags
            if (manualTags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = manualTags,
                        key = { tag -> tag.id }
                    ) { tag ->
                        ManualTagChip(
                            tag = tag,
                            onRemove = { onRemoveManualTag(tag.id) }
                        )
                    }
                }
            } else {
                Text(
                    text = "Add manual safety tags to identify hazards you observe",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Tag management dialog
    if (showTagManager) {
        MobileTagManager(
            photoId = photo.id,
            existingTags = photo.tags.toSet(),
            onTagsUpdated = { tagSet ->
                onTagsUpdated(photo.id, tagSet.toList())
                showTagManager = false
            },
            onDismiss = { showTagManager = false }
        )
    }
}

@Composable
private fun AIAnalysisSection(
    photo: Photo,
    analysisState: SafetyAnalysisState,
    canAnalyze: Boolean,
    disabledReason: String?,
    isAIAnalysisEnabled: Boolean,
    hasValidApiKey: Boolean,
    onStartAnalysis: () -> Unit,
    onTagClick: (String) -> Unit,
    onToggleBoundingBoxes: (Boolean) -> Unit,
    performanceTracker: PhotoViewerPerformanceTracker,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = SafetyOrange,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "AI Safety Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Status indicator
                if (analysisState.aiAnalysis != null) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Analysis complete",
                        tint = SafetyGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Settings prompts for disabled states
            if (!isAIAnalysisEnabled) {
                SettingsPromptCard(
                    title = "AI Analysis Disabled",
                    message = "Enable AI Analysis in camera settings to detect safety hazards automatically",
                    actionText = "Open Settings",
                    onAction = { /* TODO: Navigate to settings */ },
                    backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    actionColor = DangerRed
                )
            } else if (!hasValidApiKey) {
                SettingsPromptCard(
                    title = "API Key Required",
                    message = "Configure your Gemini API key to enable AI-powered hazard detection",
                    actionText = "Add API Key",
                    onAction = { /* TODO: Show API key dialog */ },
                    backgroundColor = SafetyOrange.copy(alpha = 0.1f),
                    actionColor = SafetyOrange
                )
            }

            // Analysis status message
            AIAnalysisStatusMessage(
                isAnalyzing = analysisState.isAnalyzingAI,
                analysisError = analysisState.aiError,
                analysisResult = analysisState.aiAnalysis
            )

            // Analyze button
            Button(
                onClick = onStartAnalysis,
                enabled = canAnalyze,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafetyOrange,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // Construction-grade height
            ) {
                if (analysisState.isAnalyzingAI) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        analysisState.isAnalyzingAI -> "Analyzing..."
                        disabledReason != null -> disabledReason
                        else -> "Analyze Safety"
                    },
                    fontSize = 16.sp // Larger text for outdoor readability
                )
            }

            // Display analysis results
            analysisState.aiAnalysis?.let { result ->
                Divider()

                AIAnalysisResults(
                    result = result,
                    photo = photo,
                    showBoundingBoxes = analysisState.showBoundingBoxes,
                    onTagClick = onTagClick,
                    onToggleBoundingBoxes = onToggleBoundingBoxes
                )
            }
        }
    }
}

@Composable
private fun OSHAComplianceSection(
    oshaAnalysis: OSHAAnalysisResult?,
    isAnalyzing: Boolean,
    displayState: OSHADisplayState,
    onStartOSHAAnalysis: () -> Unit,
    onToggleVisibility: (Boolean) -> Unit,
    onExpandStandard: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "OSHA Compliance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (oshaAnalysis == null && !isAnalyzing) {
                    Button(
                        onClick = onStartOSHAAnalysis,
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check Compliance")
                    }
                }
            }

            when {
                isAnalyzing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = DangerRed
                        )
                        Text(
                            text = "Checking OSHA compliance...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                oshaAnalysis != null -> {
                    OSHAAnalysisResults(
                        analysis = oshaAnalysis,
                        displayState = displayState,
                        onExpandStandard = onExpandStandard
                    )
                }

                else -> {
                    Text(
                        text = "Run OSHA compliance check to identify potential violations and penalties",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Helper components used by the main sections
@Composable
private fun ManualTagChip(
    tag: ManualHazardTag,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (tag.category) {
        HazardTagCategory.FALL_HAZARD,
        HazardTagCategory.ELECTRICAL,
        HazardTagCategory.STRUCTURAL -> CriticalRed
        HazardTagCategory.MACHINERY,
        HazardTagCategory.CHEMICAL -> SafetyOrange
        else -> SafetyGreen
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = categoryColor.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, categoryColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = categoryColor
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove tag",
                    modifier = Modifier.size(12.dp),
                    tint = categoryColor
                )
            }
        }
    }
}

@Composable
private fun AIAnalysisStatusMessage(
    isAnalyzing: Boolean,
    analysisError: String?,
    analysisResult: PhotoAnalysisWithTags?
) {
    when {
        isAnalyzing -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = SafetyOrange
                )
                Text(
                    text = "Analyzing photo with Gemini Vision...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        analysisError != null -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Analysis failed: $analysisError",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DangerRed
                )
            }
        }

        analysisResult == null -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Ready to analyze photo for safety hazards",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        else -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SafetyGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Analysis completed in ${analysisResult.processingTimeMs}ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SafetyGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AIAnalysisResults(
    result: PhotoAnalysisWithTags,
    photo: Photo,
    showBoundingBoxes: Boolean,
    onTagClick: (String) -> Unit,
    onToggleBoundingBoxes: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "AI Analysis Results:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // Bounding box toggle
        if (result.hazardDetections.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = showBoundingBoxes,
                    onCheckedChange = onToggleBoundingBoxes,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SafetyGreen,
                        checkedTrackColor = SafetyGreen.copy(alpha = 0.5f)
                    )
                )
                Text(
                    text = "Show Hazards on Photo",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Recommended tags
        if (result.recommendedTags.isNotEmpty()) {
            Text(
                text = "Recommended Safety Tags:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = result.recommendedTags,
                    key = { tag -> "ai_recommended_tag_$tag" }
                ) { tag ->
                    RecommendedTagChip(
                        tag = tag,
                        isAlreadyAdded = photo.tags.contains(tag),
                        onTagClick = { if (!photo.tags.contains(tag)) onTagClick(tag) }
                    )
                }
            }
        }

        // Hazard detections
        if (result.hazardDetections.isNotEmpty()) {
            Text(
                text = "Detected Hazards (${result.hazardDetections.size}):",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            result.hazardDetections.forEach { hazard ->
                HazardDetectionCard(hazard = hazard)
            }
        }
    }
}

@Composable
private fun RecommendedTagChip(
    tag: String,
    isAlreadyAdded: Boolean,
    onTagClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isAlreadyAdded) SafetyGreen.copy(alpha = 0.2f) else SafetyOrange.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, if (isAlreadyAdded) SafetyGreen else SafetyOrange),
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable { if (!isAlreadyAdded) onTagClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAlreadyAdded) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Already added",
                    tint = SafetyGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                color = if (isAlreadyAdded) SafetyGreen else SafetyOrange
            )
        }
    }
}

@Composable
private fun HazardDetectionCard(
    hazard: com.hazardhawk.ai.yolo.ConstructionHazardDetection,
    modifier: Modifier = Modifier
) {
    val severityColor = when (hazard.severity.name) {
        "CRITICAL" -> CriticalRed
        "HIGH" -> DangerRed
        "MEDIUM" -> SafetyOrange
        else -> SafetyGreen
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, severityColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hazard.severity.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = severityColor
                )
                Text(
                    text = "${(hazard.boundingBox.confidence * 100).toInt()}% confident",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = hazard.description?.ifEmpty {
                    hazard.hazardType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                } ?: hazard.hazardType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (!hazard.oshaReference.isNullOrEmpty()) {
                Text(
                    text = "OSHA: ${hazard.oshaReference}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}