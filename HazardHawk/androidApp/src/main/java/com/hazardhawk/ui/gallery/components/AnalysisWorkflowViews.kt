package com.hazardhawk.ui.gallery.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazardhawk.domain.entities.Photo
import com.hazardhawk.ui.gallery.state.*

// Construction-safe colors
private val SafetyOrange = Color(0xFFFF6B35)
private val SafetyGreen = Color(0xFF10B981)
private val DangerRed = Color(0xFFEF4444)

/**
 * Pre-Analysis View - Manual hazard tagging interface
 * Displayed when analysisPhase == PRE_ANALYSIS
 */
@Composable
fun PreAnalysisView(
    photo: Photo,
    manualTags: List<ManualHazardTag>,
    availableCategories: List<HazardTagCategory>,
    isAddingTag: Boolean,
    onAddTag: (String, HazardTagCategory, String?) -> Unit,
    onRemoveTag: (String) -> Unit,
    onStartAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PreAnalysisHeader(
                manualTagCount = manualTags.size,
                onStartAnalysis = onStartAnalysis,
                canStartAnalysis = manualTags.isNotEmpty()
            )
        }
        
        item {
            ManualTaggingInterface(
                manualTags = manualTags,
                availableCategories = availableCategories,
                isAddingTag = isAddingTag,
                onAddTag = onAddTag,
                onRemoveTag = onRemoveTag
            )
        }
        
        item {
            HazardCategorySelector(
                categories = availableCategories,
                selectedCategory = HazardTagCategory.GENERAL_SAFETY, // Default selection
                onCategorySelected = { /* Handle category selection for quick tagging */ }
            )
        }
    }
}

/**
 * Post-Analysis View - AI results, OSHA compliance, and validation
 * Displayed when analysisPhase == POST_ANALYSIS
 */
@Composable
fun PostAnalysisView(
    photo: Photo,
    safetyAnalysisState: SafetyAnalysisState,
    onAnalysisAction: (SafetyAnalysisAction) -> Unit,
    onTagsUpdated: (String, List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PostAnalysisHeader(
                aiAnalysis = safetyAnalysisState.aiAnalysis,
                manualTagCount = safetyAnalysisState.manualTags.size,
                isValidated = safetyAnalysisState.hasValidatedAIResults,
                onBackToManual = {
                    onAnalysisAction(SafetyAnalysisAction.TransitionToPreAnalysis)
                }
            )
        }
        
        item {
            AIAnalysisResultsCard(
                photo = photo,
                analysisResult = safetyAnalysisState.aiAnalysis,
                isAnalyzing = safetyAnalysisState.isAnalyzingAI,
                analysisError = safetyAnalysisState.aiError,
                showBoundingBoxes = safetyAnalysisState.showBoundingBoxes,
                onAnalyze = {
                    onAnalysisAction(SafetyAnalysisAction.StartAIAnalysis)
                },
                onBoundingBoxToggle = { visible ->
                    onAnalysisAction(SafetyAnalysisAction.SetBoundingBoxesVisible(visible))
                },
                onTagClick = { tag ->
                    val updatedTags = photo.tags + tag
                    onTagsUpdated(photo.id, updatedTags)
                }
            )
        }
        
        item {
            OSHAComplianceCard(
                oshaAnalysis = safetyAnalysisState.oshaAnalysis,
                isLoadingOSHA = safetyAnalysisState.isAnalyzingOSHA,
                displayState = safetyAnalysisState.oshaDisplay,
                onAnalyze = {
                    onAnalysisAction(SafetyAnalysisAction.StartOSHAAnalysis)
                },
                onToggleVisibility = { visible ->
                    onAnalysisAction(SafetyAnalysisAction.SetOSHAVisible(visible))
                },
                onExpandStandard = { standardId, expanded ->
                    onAnalysisAction(SafetyAnalysisAction.ExpandOSHAStandard(standardId, expanded))
                }
            )
        }
        
        if (safetyAnalysisState.aiAnalysis != null) {
            item {
                AIValidationCard(
                    aiValidation = safetyAnalysisState.aiValidation,
                    onValidate = {
                        onAnalysisAction(SafetyAnalysisAction.ValidateAIResults("current_user"))
                    },
                    onOverrideFinding = { findingId, override ->
                        onAnalysisAction(SafetyAnalysisAction.OverrideAIFinding(findingId, override))
                    }
                )
            }
        }
        
        if (safetyAnalysisState.manualTags.isNotEmpty() && safetyAnalysisState.aiAnalysis != null) {
            item {
                AnalysisComparisonCard(
                    manualTags = safetyAnalysisState.manualTags,
                    aiFindings = safetyAnalysisState.aiAnalysis?.recommendedTags ?: emptyList(),
                    showComparison = safetyAnalysisState.showAnalysisComparison,
                    onToggleComparison = { visible ->
                        onAnalysisAction(SafetyAnalysisAction.SetAnalysisComparisonVisible(visible))
                    }
                )
            }
        }
    }
}

/**
 * Header for Pre-Analysis phase
 */
@Composable
private fun PreAnalysisHeader(
    manualTagCount: Int,
    onStartAnalysis: () -> Unit,
    canStartAnalysis: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                Column {
                    Text(
                        text = "Manual Safety Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tag hazards manually before AI analysis",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Manual tagging",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manual Tags: $manualTagCount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Button(
                    onClick = onStartAnalysis,
                    enabled = canStartAnalysis,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafetyOrange
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze Safety")
                }
            }
        }
    }
}

/**
 * Header for Post-Analysis phase
 */
@Composable
private fun PostAnalysisHeader(
    aiAnalysis: com.hazardhawk.ai.PhotoAnalysisWithTags?,
    manualTagCount: Int,
    isValidated: Boolean,
    onBackToManual: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                Column {
                    Text(
                        text = "AI Safety Analysis Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Review and validate AI findings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "AI analysis",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Manual: $manualTagCount | AI: ${aiAnalysis?.recommendedTags?.size ?: 0}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (isValidated) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Validated",
                                tint = SafetyGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Validated",
                                style = MaterialTheme.typography.bodySmall,
                                color = SafetyGreen
                            )
                        }
                    }
                }
                
                OutlinedButton(
                    onClick = onBackToManual
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back to Manual")
                }
            }
        }
    }
}

/**
 * Manual tagging interface with category-based tag selection
 */
@Composable
private fun ManualTaggingInterface(
    manualTags: List<ManualHazardTag>,
    availableCategories: List<HazardTagCategory>,
    isAddingTag: Boolean,
    onAddTag: (String, HazardTagCategory, String?) -> Unit,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(HazardTagCategory.GENERAL_SAFETY) }
    var customTagName by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Manual Hazard Tags",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Category selector
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableCategories) { category ->
                    FilterChip(
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        selected = category == selectedCategory,
                        leadingIcon = {
                            Icon(
                                imageVector = getCategoryIcon(category),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
            
            // Predefined tags for selected category
            val predefinedTags = remember(selectedCategory) {
                getPredefinedTagsForCategory(selectedCategory)
            }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(predefinedTags) { tagName ->
                    val isAlreadyAdded = manualTags.any { it.name == tagName }
                    FilterChip(
                        onClick = {
                            if (!isAlreadyAdded) {
                                onAddTag(tagName, selectedCategory, null)
                            }
                        },
                        label = { Text(tagName, style = MaterialTheme.typography.bodySmall) },
                        selected = isAlreadyAdded,
                        enabled = !isAlreadyAdded
                    )
                }
                
                item {
                    FilterChip(
                        onClick = { showCustomInput = !showCustomInput },
                        label = { Text("Custom", style = MaterialTheme.typography.bodySmall) },
                        selected = showCustomInput,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
            
            // Custom tag input
            if (showCustomInput) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customTagName,
                        onValueChange = { customTagName = it },
                        label = { Text("Custom hazard tag") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    Button(
                        onClick = {
                            if (customTagName.isNotBlank()) {
                                onAddTag(customTagName, selectedCategory, null)
                                customTagName = ""
                                showCustomInput = false
                            }
                        },
                        enabled = customTagName.isNotBlank()
                    ) {
                        Text("Add")
                    }
                }
            }
            
            // Current manual tags
            if (manualTags.isNotEmpty()) {
                Text(
                    text = "Current Tags (${manualTags.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(manualTags) { tag ->
                        FilterChip(
                            onClick = { onRemoveTag(tag.id) },
                            label = { Text(tag.name, style = MaterialTheme.typography.bodySmall) },
                            selected = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove tag",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * AI validation card for user validation of AI results
 */
@Composable
private fun AIValidationCard(
    aiValidation: AIValidationState,
    onValidate: () -> Unit,
    onOverrideFinding: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (aiValidation.isValidated) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
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
                Text(
                    text = "AI Results Validation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                if (aiValidation.isValidated) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Validated",
                            tint = SafetyGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Validated",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SafetyGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onValidate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafetyGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Validate Results")
                    }
                }
            }
            
            if (!aiValidation.isValidated) {
                Text(
                    text = "Review AI findings and validate their accuracy before proceeding.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                aiValidation.validatedAt?.let { validatedAt ->
                    Text(
                        text = "Validated on ${validatedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Comparison card showing manual vs AI analysis results
 */
@Composable
private fun AnalysisComparisonCard(
    manualTags: List<ManualHazardTag>,
    aiFindings: List<String>,
    showComparison: Boolean,
    onToggleComparison: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                    text = "Analysis Comparison",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = { onToggleComparison(!showComparison) }
                ) {
                    Icon(
                        imageVector = if (showComparison) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showComparison) "Hide comparison" else "Show comparison"
                    )
                }
            }
            
            if (showComparison) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Manual (${manualTags.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        manualTags.take(3).forEach { tag ->
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (manualTags.size > 3) {
                            Text(
                                text = "+${manualTags.size - 3} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "AI (${aiFindings.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        aiFindings.take(3).forEach { finding ->
                            Text(
                                text = finding,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (aiFindings.size > 3) {
                            Text(
                                text = "+${aiFindings.size - 3} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get Material Design icon for hazard category
 */
private fun getCategoryIcon(category: HazardTagCategory) = when (category) {
    HazardTagCategory.PPE_VIOLATION -> Icons.Default.Security
    HazardTagCategory.FALL_HAZARD -> Icons.Default.Height
    HazardTagCategory.ELECTRICAL -> Icons.Default.Bolt
    HazardTagCategory.MACHINERY -> Icons.Default.Settings
    HazardTagCategory.CHEMICAL -> Icons.Default.Science
    HazardTagCategory.STRUCTURAL -> Icons.Default.Foundation
    HazardTagCategory.ENVIRONMENTAL -> Icons.Default.Cloud
    HazardTagCategory.GENERAL_SAFETY -> Icons.Default.Shield
    HazardTagCategory.CUSTOM -> Icons.Default.Add
}