package com.hazardhawk.tags

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ui.gallery.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hazardhawk.tags.models.*
import com.hazardhawk.models.TagCategory
import com.hazardhawk.ui.gallery.AIAnalysisResults
import com.hazardhawk.ui.gallery.AIResultsHeroCard
import com.hazardhawk.ui.gallery.AIRecommendationToggle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

/**
 * Enhanced tag management dialog with AI-powered suggestions and construction-optimized UX.
 * 
 * This "loveable" version of the tag manager focuses on:
 * - Simple: Quick 2-tap tagging workflow 
 * - Loveable: Smooth animations, haptic feedback, smart suggestions
 * - Complete: Full OSHA compliance integration and offline support
 * 
 * Key Enhanced Features:
 * - AI-powered tag suggestions based on photo analysis
 * - One-tap compliance toggle (Compliant/Needs Work)
 * - Smart voice input for hands-free operation
 * - Large touch targets optimized for work gloves
 * - High contrast mode for bright outdoor conditions
 * - Offline-first with intelligent caching
 * 
 * @param photoId Unique identifier for the photo being tagged
 * @param photoPath File path for AI analysis context
 * @param existingTags Set of currently selected tag IDs
 * @param onTagsUpdated Callback with updated tags and compliance status
 * @param onDismiss Callback when dialog is dismissed
 * @param aiAnalysisResult Optional AI analysis for smart suggestions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoveableTagDialog(
    photoId: String,
    photoPath: String = "",
    existingTags: Set<String> = emptySet(),
    onTagsUpdated: (Set<String>, Boolean) -> Unit,
    onDismiss: () -> Unit,
    aiAnalysisResult: AIAnalysisResults? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tagStorage = remember { context.createTagStorage() }
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Dialog State
    var selectedTags by remember { mutableStateOf(existingTags) }
    var isCompliant by remember { mutableStateOf(aiAnalysisResult?.hazardsFound == 0) }
    var searchQuery by remember { mutableStateOf("") }
    var showVoiceInput by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showAdvancedMode by remember { mutableStateOf(false) }
    var showingAIRecommendations by remember { mutableStateOf(aiAnalysisResult != null && aiAnalysisResult.recommendedTags.isNotEmpty()) }
    
    // Tag Data
    var availableTags by remember { mutableStateOf(getDefaultQuickTags()) }
    var aiSuggestions by remember { mutableStateOf<List<UITag>>(emptyList()) }
    var quickTags by remember { mutableStateOf<List<UITag>>(emptyList()) }
    var filteredTags by remember { mutableStateOf<List<UITag>>(emptyList()) }
    
    // Safety-first color scheme
    val safetyOrange = Color(0xFFFF6B35)
    val safetyRed = Color(0xFFDC2626) 
    val safetyGreen = Color(0xFF059669)
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    // Load tag data and generate AI suggestions
    LaunchedEffect(Unit) {
        try {
            val customTags = tagStorage.loadCustomTags()
            val usageCounts = tagStorage.loadTagUsageCounts()
            val recentTags = tagStorage.getRecentTags()
            
            // Merge all tags with usage stats
            val allTags = (getDefaultQuickTags() + customTags).map { tag ->
                tag.copy(
                    usageCount = usageCounts[tag.id] ?: tag.usageCount,
                    isRecentlyUsed = recentTags.contains(tag.id)
                )
            }.sortedByDescending { it.usageCount }
            
            availableTags = allTags
            
            // Generate AI suggestions if analysis is available
            if (aiAnalysisResult != null) {
                aiSuggestions = generateAISuggestionsFromResults(aiAnalysisResult, allTags)
            }
            
            // Create quick tags (most used + AI suggestions)
            quickTags = (aiSuggestions.take(3) + 
                       allTags.filter { it.isRecentlyUsed }.take(3) +
                       allTags.sortedByDescending { it.usageCount }.take(6))
                       .distinctBy { it.id }
                       .take(8)
            
            isLoading = false
            
        } catch (e: Exception) {
            e.printStackTrace()
            isLoading = false
        }
    }
    
    // Filter tags based on search
    LaunchedEffect(searchQuery, availableTags) {
        filteredTags = if (searchQuery.isBlank()) {
            availableTags
        } else {
            availableTags.filter { tag ->
                tag.name.contains(searchQuery, ignoreCase = true) ||
                tag.getCategoryDisplayName().contains(searchQuery, ignoreCase = true) ||
                tag.description?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            tonalElevation = 16.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with compliance toggle - Fixed at top
                LoveableTagHeader(
                    selectedTagsCount = selectedTags.size,
                    isCompliant = isCompliant,
                    onComplianceToggle = { 
                        isCompliant = it
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onClose = onDismiss,
                    showVoiceInput = showVoiceInput,
                    onVoiceToggle = { 
                        showVoiceInput = it
                        if (it) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Scrollable content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                
                // AI Results Hero Card - Show AI value proposition
                if (aiAnalysisResult != null) {
                    AIResultsHeroCard(
                        hazardsFound = aiAnalysisResult.hazardsFound,
                        processingTime = "${aiAnalysisResult.processingTimeMs}ms",
                        topRecommendation = aiAnalysisResult.topRecommendation,
                        confidence = aiAnalysisResult.confidence,
                        results = aiAnalysisResult.results,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // AI/Generic toggle if we have AI suggestions
                    if (aiSuggestions.isNotEmpty()) {
                        AIRecommendationToggle(
                            aiRecommendationsCount = aiSuggestions.size,
                            genericCount = availableTags.size - aiSuggestions.size,
                            showingAI = showingAIRecommendations,
                            onToggle = { showAI ->
                                showingAIRecommendations = showAI
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                if (isLoading) {
                    LoadingState()
                } else {
                    // Quick suggestions section
                    if (!showAdvancedMode && quickTags.isNotEmpty()) {
                        val displayTags = if (showingAIRecommendations && aiSuggestions.isNotEmpty()) {
                            aiSuggestions
                        } else {
                            quickTags
                        }
                        
                        SmartSuggestionSection(
                            quickTags = displayTags,
                            aiSuggestions = aiSuggestions,
                            selectedTags = selectedTags,
                            showingAIOnly = showingAIRecommendations,
                            onTagToggle = { tagId ->
                                selectedTags = if (tagId in selectedTags) {
                                    selectedTags - tagId
                                } else {
                                    selectedTags + tagId
                                }
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Toggle to advanced mode
                        TextButton(
                            onClick = { showAdvancedMode = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("More Options")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        // Advanced search mode
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All Tags",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (showAdvancedMode) {
                                    TextButton(
                                        onClick = { showAdvancedMode = false }
                                    ) {
                                        Icon(Icons.Default.ExpandLess, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Quick Mode")
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Search bar
                            EnhancedSearchBar(
                                searchQuery = searchQuery,
                                onSearchChanged = { searchQuery = it },
                                showVoiceInput = showVoiceInput
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // All tags list
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredTags, key = { it.id }) { tag ->
                                    EnhancedTagItem(
                                        tag = tag,
                                        isSelected = tag.id in selectedTags,
                                        isAISuggestion = tag in aiSuggestions,
                                        onToggle = { tagId ->
                                            selectedTags = if (tagId in selectedTags) {
                                                selectedTags - tagId
                                            } else {
                                                selectedTags + tagId
                                            }
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )
                                }
                                
                                // Add new tag option
                                if (searchQuery.isNotBlank() && 
                                    !filteredTags.any { it.name.equals(searchQuery, ignoreCase = true) }) {
                                    item {
                                        AddNewTagItem(
                                            searchQuery = searchQuery,
                                            onClick = {
                                                // Quick tag creation without dialog
                                                coroutineScope.launch {
                                                    val newTag = UITag(
                                                        id = "custom_${UUID.randomUUID()}",
                                                        name = searchQuery.trim(),
                                                        category = TagCategory.GENERAL_SAFETY,
                                                        displayColor = safetyOrange,
                                                        isCustom = true
                                                    )
                                                    
                                                    tagStorage.saveCustomTag(newTag)
                                                    availableTags = availableTags + newTag
                                                    selectedTags = selectedTags + newTag.id
                                                    searchQuery = ""
                                                    
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                } // End scrollable content
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Action buttons
                LoveableActionButtons(
                    selectedCount = selectedTags.size,
                    isCompliant = isCompliant,
                    onSave = {
                        coroutineScope.launch {
                            // Save tags and update usage stats
                            tagStorage.savePhotoTags(photoPath, selectedTags)
                            tagStorage.updateRecentTags(selectedTags)
                            
                            // Callback with results
                            onTagsUpdated(selectedTags, isCompliant)
                            onDismiss()
                        }
                    },
                    onCancel = onDismiss
                )
            }
        }
    }
}

@Composable
private fun LoveableTagHeader(
    selectedTagsCount: Int,
    isCompliant: Boolean,
    onComplianceToggle: (Boolean) -> Unit,
    onClose: () -> Unit,
    showVoiceInput: Boolean,
    onVoiceToggle: (Boolean) -> Unit
) {
    val safetyGreen = Color(0xFF059669)
    val safetyRed = Color(0xFFDC2626)
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Safety Tagging",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                AnimatedVisibility(visible = selectedTagsCount > 0) {
                    Text(
                        text = "$selectedTagsCount tag${if (selectedTagsCount == 1) "" else "s"} selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Voice input toggle
                IconButton(
                    onClick = { onVoiceToggle(!showVoiceInput) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (showVoiceInput) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (showVoiceInput) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = "Voice Input",
                        tint = if (showVoiceInput) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                // Close button
                IconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Compliance Toggle - Large and prominent
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ComplianceToggleButton(
                text = "Compliant",
                isSelected = isCompliant,
                color = safetyGreen,
                modifier = Modifier.weight(1f),
                onClick = { onComplianceToggle(true) }
            )
            
            ComplianceToggleButton(
                text = "Needs Work",
                isSelected = !isCompliant,
                color = safetyRed,
                modifier = Modifier.weight(1f),
                onClick = { onComplianceToggle(false) }
            )
        }
    }
}

@Composable
private fun ComplianceToggleButton(
    text: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Surface(
        modifier = modifier
            .height(56.dp)
            .scale(animatedScale)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.5.dp,
            color = if (isSelected) color else color.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SmartSuggestionSection(
    quickTags: List<UITag>,
    aiSuggestions: List<UITag>,
    selectedTags: Set<String>,
    showingAIOnly: Boolean = false,
    onTagToggle: (String) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (showingAIOnly) "AI Recommendations" else "Smart Suggestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (aiSuggestions.isNotEmpty() && showingAIOnly) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "AI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Grid layout for quick tags
        val chunkedTags = quickTags.chunked(2)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chunkedTags.forEach { rowTags ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTags.forEach { tag ->
                        QuickTagChip(
                            tag = tag,
                            isSelected = tag.id in selectedTags,
                            isAISuggestion = tag in aiSuggestions,
                            onToggle = { onTagToggle(tag.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Fill empty space in last row if odd number of tags
                    if (rowTags.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTagChip(
    tag: UITag,
    isSelected: Boolean,
    isAISuggestion: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Surface(
        modifier = modifier
            .height(64.dp)
            .scale(animatedScale)
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            tag.displayColor.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        },
        border = if (isSelected) {
            BorderStroke(2.dp, tag.displayColor)
        } else if (isAISuggestion) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        },
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAISuggestion) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(tag.displayColor, CircleShape)
                )
                
                if (isSelected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = tag.displayColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedSearchBar(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    showVoiceInput: Boolean
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChanged,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = {
            Text(
                text = if (showVoiceInput) "Say tag name..." else "Search tags...",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = if (showVoiceInput) Icons.Default.Mic else Icons.Default.Search,
                contentDescription = if (showVoiceInput) "Voice Search" else "Search",
                tint = if (showVoiceInput) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchChanged("") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun EnhancedTagItem(
    tag: UITag,
    isSelected: Boolean,
    isAISuggestion: Boolean,
    onToggle: (String) -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .scale(animatedScale)
            .clickable { onToggle(tag.id) },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            tag.displayColor.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = if (isSelected) {
            BorderStroke(2.dp, tag.displayColor)
        } else if (isAISuggestion) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = tag.displayColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            if (!isSelected) {
                Spacer(modifier = Modifier.width(36.dp)) // Keep consistent alignment
            }
            
            // Color indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(tag.displayColor, CircleShape)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Tag info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (isAISuggestion) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "AI",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tag.getCategoryDisplayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (tag.usageCount > 0) {
                        Text(
                            text = " • Used ${tag.usageCount} times",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (tag.isRecentlyUsed) {
                        Text(
                            text = " • Recent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading smart suggestions...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
    }
}

@Composable
private fun LoveableActionButtons(
    selectedCount: Int,
    isCompliant: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val safetyGreen = Color(0xFF059669)
    val safetyRed = Color(0xFFDC2626)
    val saveButtonColor = if (isCompliant) safetyGreen else safetyRed
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        Button(
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = saveButtonColor,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCompliant) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedCount > 0) {
                        "Save ($selectedCount)"
                    } else {
                        "Save Tags"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Generate AI-powered tag suggestions based on photo analysis
 * This is a mock implementation - in production, this would integrate with actual AI analysis
 */
private fun generateAISuggestions(analysisResult: String, availableTags: List<UITag>): List<UITag> {
    // Mock AI analysis - in production this would process actual AI results
    val mockSuggestions = listOf("safety", "ppe", "electrical", "fall", "housekeeping", "equipment")
    
    return availableTags.filter { tag ->
        mockSuggestions.any { suggestion ->
            tag.name.contains(suggestion, ignoreCase = true) ||
            tag.description?.contains(suggestion, ignoreCase = true) == true
        }
    }.take(3)
}

/**
 * Generate AI-powered tag suggestions from structured AI analysis results
 */
private fun generateAISuggestionsFromResults(
    aiResults: AIAnalysisResults,
    availableTags: List<UITag>
): List<UITag> {
    val recommendedTagNames = aiResults.recommendedTags
    val oshaKeywords = aiResults.oshaReferences.flatMap { ref ->
        ref.split(" ", ".", "-").filter { it.length > 2 }
    }
    
    // Find matching tags by name, description, or OSHA keywords
    return availableTags.filter { tag ->
        // Direct match with recommended tags
        recommendedTagNames.any { recommended ->
            tag.name.contains(recommended, ignoreCase = true) ||
            tag.description?.contains(recommended, ignoreCase = true) == true
        } ||
        // Match with OSHA keywords
        oshaKeywords.any { keyword ->
            tag.name.contains(keyword, ignoreCase = true) ||
            tag.description?.contains(keyword, ignoreCase = true) == true
        }
    }.distinctBy { it.id }.take(6) // Limit to 6 AI suggestions
}