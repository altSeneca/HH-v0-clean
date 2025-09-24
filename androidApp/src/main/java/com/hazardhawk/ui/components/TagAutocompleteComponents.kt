/*
 * Copyright (c) 2025 HazardHawk Safety Platform
 *
 * Tag autocomplete and suggestion components.
 * Provides intelligent tag suggestions based on AI analysis and usage patterns.
 */
package com.hazardhawk.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.hazardhawk.domain.entities.*
import com.hazardhawk.shared.ui.components.*
import com.hazardhawk.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Data classes for tag suggestions
 */
data class TagSuggestion(
    val tag: Tag,
    val confidence: Double? = null,
    val source: SuggestionSource,
    val reason: String? = null
)

enum class SuggestionSource {
    AI,
    FREQUENT,
    RECENT,
    PROJECT_BASED,
    OSHA_RECOMMENDED,
    SEARCH;
    
    val displayName: String
        get() = when (this) {
            AI -> "AI Suggested"
            FREQUENT -> "Frequently Used"
            RECENT -> "Recently Used"
            PROJECT_BASED -> "Project Based"
            OSHA_RECOMMENDED -> "OSHA Recommended"
            SEARCH -> "Search Result"
        }
}

/**
 * Smart Tag Autocomplete Field
 * 
 * Features:
 * - Real-time suggestions as user types
 * - AI-powered recommendations
 * - OSHA compliance prioritization
 * - Construction-friendly input
 * - Multi-tag input with chips
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTagAutocompleteField(
    query: String,
    onQueryChanged: (String) -> Unit,
    suggestions: List<TagSuggestion>,
    onSuggestionSelected: (TagSuggestion) -> Unit,
    selectedTags: List<Tag>,
    onTagRemoved: (Tag) -> Unit,
    placeholder: String = "Type to search safety tags...",
    maxSuggestions: Int = 8,
    showAISuggestions: Boolean = true,
    showOSHAPriority: Boolean = true,
    fieldConditions: FieldConditions = FieldConditions(
        brightnessLevel = BrightnessLevel.NORMAL,
        isWearingGloves = false,
        isEmergencyMode = false,
        noiseLevel = 0.3f,
        batteryLevel = 0.8f
    ),
    modifier: Modifier = Modifier
) {
    val colorScheme = fieldConditions.getColorScheme()
    val haptics = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    
    var isExpanded by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    
    // Show suggestions when there's a query or AI suggestions are enabled
    val shouldShowSuggestions = (query.isNotEmpty() || showAISuggestions) && 
                               suggestions.isNotEmpty() && 
                               (isExpanded || hasFocus)
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Selected tags display
        if (selectedTags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.Spacing.SMALL.dp),
                contentPadding = PaddingValues(
                    vertical = Dimensions.Spacing.SMALL.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(selectedTags, key = { it.id }) { tag ->
                    SelectedTagChip(
                        tag = tag,
                        onRemove = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTagRemoved(tag)
                        },
                        fieldConditions = fieldConditions
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Dimensions.Spacing.SMALL.dp))
        }
        
        // Input field
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(ColorPalette.OnSurface.getColor(colorScheme)).copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search tags",
                    tint = Color(ColorPalette.OnSurface.getColor(colorScheme)),
                    modifier = Modifier.size(Dimensions.IconSize.STANDARD.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onQueryChanged("")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = Color(ColorPalette.OnSurface.getColor(colorScheme))
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    hasFocus = focusState.hasFocus
                    isExpanded = focusState.hasFocus && suggestions.isNotEmpty()
                }
        )
        
        // Suggestions list
        AnimatedVisibility(
            visible = shouldShowSuggestions,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(suggestions.take(maxSuggestions), key = { it.tag.id }) { suggestion ->
                    TagSuggestionItem(
                        suggestion = suggestion,
                        onSelected = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSuggestionSelected(suggestion)
                            isExpanded = false
                        },
                        showOSHAPriority = showOSHAPriority,
                        fieldConditions = fieldConditions
                    )
                }
            }
        }
    }
}

/**
 * Individual suggestion item
 */
@Composable
fun TagSuggestionItem(
    suggestion: TagSuggestion,
    onSelected: () -> Unit,
    showOSHAPriority: Boolean,
    fieldConditions: FieldConditions,
    modifier: Modifier = Modifier
) {
    val colorScheme = fieldConditions.getColorScheme()
    val tag = suggestion.tag
    
    Surface(
        onClick = onSelected,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Spacing.MEDIUM.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                category = tag.category,
                size = Dimensions.IconSize.STANDARD.dp,
                tint = Color(ColorPalette.OnSurface.getColor(colorScheme))
            )
            
            Spacer(modifier = Modifier.width(Dimensions.Spacing.SMALL.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(ColorPalette.OnSurface.getColor(colorScheme))
                )
                
                Text(
                    text = "${tag.category.displayName} • ${suggestion.source.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(ColorPalette.OnSurface.getColor(colorScheme)).copy(alpha = 0.7f)
                )
            }
            
            if (showOSHAPriority && tag.hasComplianceImplications) {
                OSHAComplianceIndicator(
                    complianceStatus = tag.complianceStatus,
                    size = Dimensions.IconSize.SMALL.dp,
                    fieldConditions = fieldConditions
                )
            }
        }
    }
}

/**
 * Selected tag chip with remove option
 */
@Composable
fun SelectedTagChip(
    tag: Tag,
    onRemove: () -> Unit,
    fieldConditions: FieldConditions,
    modifier: Modifier = Modifier
) {
    val colorScheme = fieldConditions.getColorScheme()
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(ColorPalette.Primary.getColor(colorScheme)).copy(alpha = 0.12f),
        border = BorderStroke(
            width = 1.dp,
            color = Color(ColorPalette.Primary.getColor(colorScheme)).copy(alpha = 0.3f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = 8.dp,
                top = 4.dp,
                bottom = 4.dp,
                end = 4.dp
            )
        ) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelMedium,
                color = Color(ColorPalette.OnSurface.getColor(colorScheme)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove ${tag.name}",
                    modifier = Modifier.size(16.dp),
                    tint = Color(ColorPalette.OnSurface.getColor(colorScheme)).copy(alpha = 0.7f)
                )
            }
        }
    }
}