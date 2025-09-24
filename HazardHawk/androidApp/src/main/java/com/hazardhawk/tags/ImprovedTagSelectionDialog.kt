package com.hazardhawk.tags

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hazardhawk.tags.models.*
import com.hazardhawk.models.TagCategory
import com.hazardhawk.ui.theme.HazardHawkTheme
import kotlinx.coroutines.launch

/**
 * Improved post-capture tag selection dialog with fixed multi-selection,
 * better visual feedback, and enhanced user experience for construction workers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedTagSelectionDialog(
    state: ImprovedTagSelectionState,
    onComplianceToggle: (Boolean) -> Unit,
    onTagToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onSearch: (String) -> Unit,
    onCreateCustomTag: (String, TagCategory) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false // Prevent accidental dismissal
        )
    ) {
        HazardHawkTheme {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.90f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                ImprovedTagSelectionContent(
                    state = state,
                    onComplianceToggle = onComplianceToggle,
                    onTagToggle = { tagId ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTagToggle(tagId)
                    },
                    onSelectAll = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectAll()
                    },
                    onClearAll = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClearAll()
                    },
                    onSearch = onSearch,
                    onCreateCustomTag = onCreateCustomTag,
                    onConfirm = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onConfirm()
                    },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImprovedTagSelectionContent(
    state: ImprovedTagSelectionState,
    onComplianceToggle: (Boolean) -> Unit,
    onTagToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onSearch: (String) -> Unit,
    onCreateCustomTag: (String, TagCategory) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Progress Indicator
        WorkflowProgressIndicator(
            currentStep = state.currentStep,
            totalSteps = state.totalSteps
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Header with improved counter
        ImprovedTagSelectionHeader(
            selectedTagsCount = state.selectedTags.size,
            totalTagsCount = state.availableTagsCount,
            onDismiss = onDismiss
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Compliance status toggle
        ComplianceStatusCard(
            isCompliant = state.isCompliant,
            onToggle = onComplianceToggle
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Bulk action buttons
        BulkActionButtons(
            hasSelection = state.selectedTags.isNotEmpty(),
            onSelectAll = onSelectAll,
            onClearAll = onClearAll
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Tab navigation
        val tabs = listOf("By Severity", "Quick Tags", "Recommended", "Search")
        
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { 
                        selectedTabIndex = index
                        keyboardController?.hide()
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tab content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTabIndex) {
                0 -> SeverityGroupedTagsTab(
                    tagsBySeverity = state.tagsBySeverity,
                    selectedTags = state.selectedTags,
                    onTagToggle = onTagToggle
                )
                1 -> ImprovedQuickTagsTab(
                    tags = state.quickTags,
                    selectedTags = state.selectedTags,
                    onTagToggle = onTagToggle
                )
                2 -> RecommendedTagsTab(
                    recommendations = state.recommendedTags,
                    selectedTags = state.selectedTags,
                    onTagToggle = onTagToggle
                )
                3 -> ImprovedSearchTab(
                    searchQuery = state.searchQuery,
                    searchResults = state.searchResults,
                    selectedTags = state.selectedTags,
                    onSearch = onSearch,
                    onTagToggle = onTagToggle,
                    onCreateCustomTag = onCreateCustomTag
                )
            }
            
            // Loading overlay
            if (state.isLoading) {
                LoadingOverlay()
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons with validation
        ImprovedTagSelectionActions(
            selectedCount = state.selectedTags.size,
            canProceed = state.canProceed,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
        
        // Error/Warning messages
        state.error?.let { error ->
            ErrorMessage(error = error)
        }
        
        // Toast for no selection
        if (state.showNoSelectionWarning) {
            NoSelectionWarning()
        }
    }
}

@Composable
private fun ComplianceStatusCard(
    isCompliant: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val backgroundColor = animateColorAsState(
        targetValue = if (isCompliant) {
            ComplianceStatus.COMPLIANT.color.copy(alpha = 0.15f)
        } else {
            ComplianceStatus.NEEDS_IMPROVEMENT.color.copy(alpha = 0.15f)
        },
        animationSpec = tween(300),
        label = "compliance_background"
    )
    
    val iconColor = if (isCompliant) {
        ComplianceStatus.COMPLIANT.color
    } else {
        ComplianceStatus.NEEDS_IMPROVEMENT.color
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isCompliant) },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor.value
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 2.dp,
            color = iconColor.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCompliant) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Safety Compliance",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isCompliant) "Compliant" else "Needs Improvement",
                        style = MaterialTheme.typography.bodyMedium,
                        color = iconColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Switch(
                checked = isCompliant,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ComplianceStatus.COMPLIANT.color,
                    checkedTrackColor = ComplianceStatus.COMPLIANT.color.copy(alpha = 0.5f),
                    uncheckedThumbColor = ComplianceStatus.NEEDS_IMPROVEMENT.color,
                    uncheckedTrackColor = ComplianceStatus.NEEDS_IMPROVEMENT.color.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun WorkflowProgressIndicator(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { step ->
            val isCompleted = step < currentStep
            val isCurrent = step == currentStep
            
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 12.dp else 8.dp)
                    .background(
                        color = when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape
                    )
            )
            
            if (step < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(
                            color = if (isCompleted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = "Step ${currentStep + 1} of $totalSteps: Select Hazards",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ImprovedTagSelectionHeader(
    selectedTagsCount: Int,
    totalTagsCount: Int,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Tag This Photo",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Enhanced selection counter
            AnimatedContent(
                targetState = selectedTagsCount,
                transitionSpec = {
                    (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
                },
                label = "counter"
            ) { count ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            count == 0 -> "No hazards selected"
                            count == 1 -> "1 hazard selected"
                            else -> "$count hazards selected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        
        IconButton(
            onClick = onDismiss,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun BulkActionButtons(
    hasSelection: Boolean,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onSelectAll,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.CheckBox,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Select All",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        OutlinedButton(
            onClick = onClearAll,
            modifier = Modifier.weight(1f),
            enabled = hasSelection,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (hasSelection) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                } else {
                    Color.Transparent
                }
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (hasSelection) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            )
        ) {
            Icon(
                imageVector = Icons.Default.CheckBoxOutlineBlank,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Clear All",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SeverityGroupedTagsTab(
    tagsBySeverity: Map<HazardSeverity, List<UITag>>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        tagsBySeverity.forEach { (severity, tags) ->
            if (tags.isNotEmpty()) {
                item {
                    SeveritySection(
                        severity = severity,
                        tags = tags,
                        selectedTags = selectedTags,
                        onTagToggle = onTagToggle
                    )
                }
            }
        }
    }
}

@Composable
private fun SeveritySection(
    severity: HazardSeverity,
    tags: List<UITag>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit
) {
    Column {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 24.dp)
                    .background(
                        color = severity.color,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = severity.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = severity.color
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "(${tags.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Tags grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(((tags.size / 2 + tags.size % 2) * 100).dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(tags) { tag ->
                ImprovedQuickTagCard(
                    tag = tag,
                    isSelected = tag.id in selectedTags,
                    severity = severity,
                    onClick = { onTagToggle(tag.id) }
                )
            }
        }
    }
}

@Composable
private fun ImprovedQuickTagsTab(
    tags: List<UITag>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tags) { tag ->
            ImprovedQuickTagCard(
                tag = tag,
                isSelected = tag.id in selectedTags,
                severity = null,
                onClick = { onTagToggle(tag.id) }
            )
        }
    }
}

@Composable
private fun ImprovedQuickTagCard(
    tag: UITag,
    isSelected: Boolean,
    severity: HazardSeverity?,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .scale(scale)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            color = borderColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Category icon with improved visibility
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            } else {
                                tag.displayColor.copy(alpha = 0.8f)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tag.name.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp
                )
                
                if (tag.oshaReferences.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "OSHA ${tag.oshaReferences.first()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
            
            // Large checkbox for better touch targets
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImprovedSearchTab(
    searchQuery: String,
    searchResults: List<UITag>,
    selectedTags: Set<String>,
    onSearch: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onCreateCustomTag: (String, TagCategory) -> Unit
) {
    Column {
        // Enhanced search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            placeholder = {
                Text(
                    text = "Search hazards or create custom...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearch("") }
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
            keyboardActions = KeyboardActions(
                onSearch = { /* Already handled by onValueChange */ }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Search results with improved layout
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searchResults) { tag ->
                ImprovedSearchResultCard(
                    tag = tag,
                    isSelected = tag.id in selectedTags,
                    onClick = { onTagToggle(tag.id) }
                )
            }
            
            // Create custom tag option
            if (searchQuery.isNotEmpty() && searchResults.none { it.name.equals(searchQuery, ignoreCase = true) }) {
                item {
                    CreateCustomTagCard(
                        searchQuery = searchQuery,
                        onCreate = onCreateCustomTag
                    )
                }
            }
        }
    }
}

@Composable
private fun ImprovedSearchResultCard(
    tag: UITag,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large checkbox for construction workers
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tag.getCategoryDisplayName(),
                        style = MaterialTheme.typography.labelMedium,
                        color = tag.displayColor
                    )
                    
                    if (tag.usageCount > 0) {
                        Text(
                            text = " • ${tag.getUsageText()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImprovedTagSelectionActions(
    selectedCount: Int,
    canProceed: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (canProceed) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline
            ),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.labelLarge,
                fontSize = 16.sp
            )
        }
        
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .weight(1f)
                .scale(animatedScale),
            enabled = canProceed,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (canProceed) 4.dp else 0.dp
            ),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = when {
                    selectedCount == 0 -> "Select Hazards"
                    selectedCount == 1 -> "Continue (1)"
                    else -> "Continue ($selectedCount)"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading hazards...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorMessage(error: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun NoSelectionWarning() {
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Please select at least one hazard to continue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}


@Composable
private fun RecommendedTagsTab(
    recommendations: List<UITagRecommendation>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(recommendations) { recommendation ->
            RecommendedTagCard(
                recommendation = recommendation,
                isSelected = recommendation.tag.id in selectedTags,
                onClick = { onTagToggle(recommendation.tag.id) }
            )
        }
    }
}

@Composable
private fun RecommendedTagCard(
    recommendation: UITagRecommendation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tag = recommendation.tag
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                tag.displayColor.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, tag.displayColor)
        } else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        tag.displayColor.copy(alpha = 0.8f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tag.name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (recommendation.isHighPriority) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "High priority",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Text(
                    text = recommendation.getReasonText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                recommendation.usageContext?.let { context ->
                    Text(
                        text = context,
                        style = MaterialTheme.typography.labelSmall,
                        color = tag.displayColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = tag.displayColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CreateCustomTagCard(
    searchQuery: String,
    onCreate: (String, TagCategory) -> Unit
) {
    var showCategorySelector by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "Create \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Add as custom tag",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (showCategorySelector) {
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TagCategory.entries.toTypedArray()) { category ->
                        FilterChip(
                            onClick = {
                                onCreate(searchQuery, category)
                                showCategorySelector = false
                            },
                            label = {
                                Text(
                                    text = category.name.lowercase().replaceFirstChar { char -> char.uppercaseChar() },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            selected = false,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { showCategorySelector = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Choose Category",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

// Compliance status for tagging
enum class ComplianceStatus(val displayName: String, val color: Color) {
    COMPLIANT("Compliant", Color(0xFF4CAF50)),
    NEEDS_IMPROVEMENT("Needs Improvement", Color(0xFFFF9800))
}

// Extension for HazardSeverity
enum class HazardSeverity(val displayName: String, val color: Color) {
    CRITICAL("Critical - Immediate Danger", Color(0xFFD32F2F)),
    HIGH("High - Serious Risk", Color(0xFFFF6B00)),
    MEDIUM("Medium - Moderate Risk", Color(0xFFFFC107)),
    LOW("Low - Minor Risk", Color(0xFF4CAF50))
}

// Improved state model
data class ImprovedTagSelectionState(
    val isCompliant: Boolean = true,
    val selectedTags: Set<String> = emptySet(),
    val quickTags: List<UITag> = emptyList(),
    val recommendedTags: List<UITagRecommendation> = emptyList(),
    val tagsBySeverity: Map<HazardSeverity, List<UITag>> = emptyMap(),
    val searchQuery: String = "",
    val searchResults: List<UITag> = emptyList(),
    val recentTags: List<UITag> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentStep: Int = 1,
    val totalSteps: Int = 4,
    val availableTagsCount: Int = 0,
    val canProceed: Boolean = false,
    val showNoSelectionWarning: Boolean = false
)