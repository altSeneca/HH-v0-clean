package com.hazardhawk.ui.gallery.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazardhawk.ai.PhotoAnalysisWithTags
import com.hazardhawk.ai.yolo.ConstructionHazardDetection
import com.hazardhawk.domain.entities.Photo

// Construction-safe colors optimized for outdoor visibility
private val SafetyOrange = Color(0xFFFF6B35)
private val SafetyGreen = Color(0xFF10B981)
private val DangerRed = Color(0xFFEF4444)

/**
 * Reusable AI Analysis Results Card Component
 * Simplified and focused on displaying analysis results
 */
@Composable
fun AIAnalysisResultsCard(
    photo: Photo,
    analysisResult: PhotoAnalysisWithTags?,
    isAnalyzing: Boolean,
    analysisError: String?,
    showBoundingBoxes: Boolean,
    onAnalyze: () -> Unit,
    onBoundingBoxToggle: (Boolean) -> Unit,
    onTagClick: (String) -> Unit,
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
            AIAnalysisHeader(
                analysisResult = analysisResult,
                isAnalyzing = isAnalyzing,
                analysisError = analysisError
            )
            
            // Controls
            AIAnalysisControls(
                isAnalyzing = isAnalyzing,
                analysisResult = analysisResult,
                showBoundingBoxes = showBoundingBoxes,
                onAnalyze = onAnalyze,
                onBoundingBoxToggle = onBoundingBoxToggle
            )
            
            // Results Display
            analysisResult?.let { result ->
                AIAnalysisResults(
                    result = result,
                    photo = photo,
                    onTagClick = onTagClick
                )
            }
        }
    }
}

@Composable
private fun AIAnalysisHeader(
    analysisResult: PhotoAnalysisWithTags?,
    isAnalyzing: Boolean,
    analysisError: String?
) {
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
        if (analysisResult != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SafetyGreen)
            )
        }
    }
    
    // Status message
    AIAnalysisStatusMessage(
        isAnalyzing = isAnalyzing,
        analysisError = analysisError,
        analysisResult = analysisResult
    )
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        analysisError != null -> {
            Text(
                text = "Analysis failed: $analysisError",
                style = MaterialTheme.typography.bodyLarge,
                color = DangerRed
            )
        }
        
        analysisResult == null -> {
            Text(
                text = "Ready to analyze photo for safety hazards",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        else -> {
            Text(
                text = "Analysis completed in ${analysisResult.processingTimeMs}ms",
                style = MaterialTheme.typography.bodyMedium,
                color = SafetyGreen,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AIAnalysisControls(
    isAnalyzing: Boolean,
    analysisResult: PhotoAnalysisWithTags?,
    showBoundingBoxes: Boolean,
    onAnalyze: () -> Unit,
    onBoundingBoxToggle: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Analyze button
        Button(
            onClick = onAnalyze,
            enabled = !isAnalyzing,
            colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isAnalyzing) "Analyzing..." else "Analyze Safety")
        }
        
        // Bounding box toggle
        if (analysisResult != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = showBoundingBoxes,
                    onCheckedChange = onBoundingBoxToggle,
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
    }
}

@Composable
private fun AIAnalysisResults(
    result: PhotoAnalysisWithTags,
    photo: Photo,
    onTagClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Divider()
        
        Text(
            text = "AI Analysis Results:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Recommended tags
        if (result.recommendedTags.isNotEmpty()) {
            RecommendedTagsSection(
                tags = result.recommendedTags,
                existingTags = photo.tags,
                onTagClick = onTagClick
            )
        }
        
        // Hazard detections
        if (result.hazardDetections.isNotEmpty()) {
            HazardDetectionsSection(hazards = result.hazardDetections)
        }
    }
}

@Composable
private fun RecommendedTagsSection(
    tags: List<String>,
    existingTags: List<String>,
    onTagClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                items = tags,
                key = { tag -> "ai_recommended_tag_$tag" }
            ) { tag ->
                RecommendedTagChip(
                    tag = tag,
                    isAlreadyAdded = existingTags.contains(tag),
                    onTagClick = { if (!existingTags.contains(tag)) onTagClick(tag) }
                )
            }
        }
    }
}

@Composable
private fun HazardDetectionsSection(
    hazards: List<ConstructionHazardDetection>
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Detected Hazards (${hazards.size}):",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        hazards.forEach { hazard ->
            HazardDetectionItem(hazard = hazard)
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
private fun HazardDetectionItem(
    hazard: ConstructionHazardDetection,
    modifier: Modifier = Modifier
) {
    val severityColor = when (hazard.severity.name) {
        "CRITICAL" -> Color(0xFFB71C1C)
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
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = severityColor
                )
                Text(
                    text = "${(hazard.boundingBox.confidence * 100).toInt()}% confidence",
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