/*
 * Copyright (c) 2025 HazardHawk Safety Platform
 *
 * Comprehensive tag management UI components for Android.
 * Designed for construction environments with accessibility and OSHA compliance.
 */
package com.hazardhawk.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hazardhawk.domain.entities.*
import com.hazardhawk.shared.ui.components.*
import com.hazardhawk.ui.theme.*
import kotlinx.coroutines.flow.collectAsState

/**
 * Hierarchical Tag Selection Component
 * 
 * Features:
 * - Category-based browsing with OSHA compliance indicators
 * - Search and filter functionality
 * - Large touch targets for construction use
 * - Multi-selection support
 * - Accessibility compliant
 * - Emergency mode support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSelectionComponent(
    availableTags: List<Tag>,
    selectedTagIds: Set<String>,
    onTagSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    onSearchQueryChanged: (String) -> Unit = {},
    selectedCategory: TagCategory? = null,
    onCategoryChanged: (TagCategory?) -> Unit = {},
    maxSelections: Int = Int.MAX_VALUE,
    showOSHAIndicators: Boolean = true,
    fieldConditions: FieldConditions = FieldConditions(
        brightnessLevel = BrightnessLevel.NORMAL,
        isWearingGloves = false,
        isEmergencyMode = false,
        noiseLevel = 0.3f,
        batteryLevel = 0.8f
    ),
    onCreateCustomTag: ((String, TagCategory) -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val colorScheme = fieldConditions.getColorScheme()
    
    // Filter tags based on search and category
    val filteredTags = remember(availableTags, searchQuery, selectedCategory) {
        availableTags.filter { tag ->
            val matchesSearch = searchQuery.isEmpty() || 
                tag.name.contains(searchQuery, ignoreCase = true) ||
                tag.description?.contains(searchQuery, ignoreCase = true) == true ||
                tag.oshaReferences.any { it.contains(searchQuery, ignoreCase = true) }
            
            val matchesCategory = selectedCategory == null || tag.category == selectedCategory
            
            matchesSearch && matchesCategory && tag.isActive
        }.sortedWith(
            compareByDescending<Tag> { it.isFrequentlyUsed }
                .thenByDescending { it.complianceStatus == ComplianceStatus.CRITICAL }
                .thenBy { it.displayPriorityScore }
                .thenBy { it.name }
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Tag selection interface with ${availableTags.size} available tags"
                role = Role.TabList
            }
    ) {
        // Search Bar
        TagSearchBar(
            searchQuery = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged,
            placeholder = "Search safety tags...",
            fieldConditions = fieldConditions,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.Spacing.MEDIUM.dp, vertical = Dimensions.Spacing.SMALL.dp)
        )
        
        // Category Filter Chips
        CategoryFilterChips(
            selectedCategory = selectedCategory,
            onCategoryChanged = onCategoryChanged,
            fieldConditions = fieldConditions,
            modifier = Modifier.padding(horizontal = Dimensions.Spacing.MEDIUM.dp)
        )
        
        // Selection Summary
        if (selectedTagIds.isNotEmpty()) {
            TagSelectionSummary(
                selectedCount = selectedTagIds.size,
                maxSelections = maxSelections,
                onClearAll = { 
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTagSelectionChanged(emptySet()) 
                },
                fieldConditions = fieldConditions,
                modifier = Modifier.padding(
                    horizontal = Dimensions.Spacing.MEDIUM.dp,
                    vertical = Dimensions.Spacing.SMALL.dp
                )
            )
        }
        
        // Tags Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            contentPadding = PaddingValues(
                horizontal = Dimensions.Spacing.MEDIUM.dp,
                vertical = Dimensions.Spacing.SMALL.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Spacing.SMALL.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.Spacing.SMALL.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredTags, key = { it.id }) { tag ->
                TagSelectionCard(
                    tag = tag,
                    isSelected = selectedTagIds.contains(tag.id),
                    onSelectionChanged = { isSelected ->
                        haptics.performHapticFeedback(
                            if (fieldConditions.isWearingGloves) HapticFeedbackType.LongPress
                            else HapticFeedbackType.TextHandleMove
                        )
                        
                        val newSelection = if (isSelected) {
                            if (selectedTagIds.size < maxSelections) {
                                selectedTagIds + tag.id
                            } else {
                                selectedTagIds // Don't add if at max
                            }
                        } else {
                            selectedTagIds - tag.id
                        }
                        onTagSelectionChanged(newSelection)
                    },
                    showOSHAIndicator = showOSHAIndicators,
                    fieldConditions = fieldConditions,
                    canSelect = selectedTagIds.size < maxSelections || selectedTagIds.contains(tag.id)
                )
            }
            
            // Custom tag creation option
            if (onCreateCustomTag != null && searchQuery.isNotBlank() && 
                filteredTags.none { it.name.equals(searchQuery, ignoreCase = true) }) {
                item {
                    CreateCustomTagCard(
                        tagName = searchQuery,
                        selectedCategory = selectedCategory ?: TagCategory.CUSTOM,
                        onCreateTag = onCreateCustomTag,
                        fieldConditions = fieldConditions
                    )
                }
            }
        }
        
        // Empty state
        if (filteredTags.isEmpty() && searchQuery.isNotEmpty()) {
            EmptyTagSearchResults(
                searchQuery = searchQuery,
                onCreateCustomTag = if (onCreateCustomTag != null) {
                    { onCreateCustomTag(searchQuery, selectedCategory ?: TagCategory.CUSTOM) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Spacing.LARGE.dp)
            )
        }
    }
}

/**
 * Search bar component optimized for construction environments
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSearchBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    placeholder: String,
    fieldConditions: FieldConditions,
    modifier: Modifier = Modifier
) {
    val colorScheme = fieldConditions.getColorScheme()
    val focusRequester = remember { FocusRequester() }
    
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
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
                contentDescription = "Search",
                tint = Color(ColorPalette.OnSurface.getColor(colorScheme)),
                modifier = Modifier.size(Dimensions.IconSize.STANDARD.dp)
            )
        },
        trailingIcon = if (searchQuery.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onSearchQueryChanged("") },
                    modifier = Modifier.size(Dimensions.TouchTargets.COMFORTABLE.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = Color(ColorPalette.OnSurface.getColor(colorScheme))
                    )
                }
            }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
            keyboardType = KeyboardType.Text
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(ColorPalette.Primary.getColor(colorScheme)),
            unfocusedBorderColor = Color(ColorPalette.OnSurface.getColor(colorScheme)).copy(alpha = 0.3f),
            focusedTextColor = Color(ColorPalette.OnSurface.getColor(colorScheme)),
            unfocusedTextColor = Color(ColorPalette.OnSurface.getColor(colorScheme))
        ),
        shape = RoundedCornerShape(Dimensions.CornerRadius.LARGE.dp),
        modifier = modifier
            .focusRequester(focusRequester)
            .semantics {
                contentDescription = "Search for safety tags"
                role = Role.Button
            }
    )
}

/**
 * Category filter chips with OSHA compliance indicators
 */
@Composable
fun CategoryFilterChips(
    selectedCategory: TagCategory?,
    onCategoryChanged: (TagCategory?) -> Unit,
    fieldConditions: FieldConditions,
    modifier: Modifier = Modifier
) {
    val colorScheme = fieldConditions.getColorScheme()
    val haptics = LocalHapticFeedback.current
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Spacing.SMALL.dp),
        contentPadding = PaddingValues(vertical = Dimensions.Spacing.SMALL.dp),
        modifier = modifier.semantics {
            contentDescription = "Category filter options"
            role = Role.TabList
        }
    ) {
        // "All" chip
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCategoryChanged(null)
                },
                label = {
                    Text(
                        text = "All Categories",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.IconSize.SMALL.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(ColorPalette.Primary.getColor(colorScheme)),
                    selectedLabelColor = Color(ColorPalette.OnPrimary.getColor(colorScheme))
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Show all tag categories"
                    role = Role.Tab
                }
            )
        }
        
        // Category chips
        items(TagCategory.orderedByPriority) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCategoryChanged(if (selectedCategory == category) null else category)
                },
                label = {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    CategoryIcon(
                        category = category,
                        size = Dimensions.IconSize.SMALL.dp,
                        tint = if (selectedCategory == category) {
                            Color(ColorPalette.OnPrimary.getColor(colorScheme))
                        } else {
                            Color(ColorPalette.OnSurface.getColor(colorScheme))
                        }
                    )
                },
                trailingIcon = if (category.typicallyHighPriority) {
                    {
                        Icon(
                            imageVector = Icons.Default.Priority,
                            contentDescription = "High priority category",
                            tint = Color(ColorPalette.Warning.getColor(colorScheme)),
                            modifier = Modifier.size(Dimensions.IconSize.SMALL.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(ColorPalette.Primary.getColor(colorScheme)),
                    selectedLabelColor = Color(ColorPalette.OnPrimary.getColor(colorScheme)),
                    unselectedLabelColor = Color(ColorPalette.OnSurface.getColor(colorScheme))
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Filter by ${category.displayName} category"
                    role = Role.Tab
                    if (category.typicallyHighPriority) {
                        stateDescription = "High priority OSHA category"
                    }
                }
            )
        }
    }
}
