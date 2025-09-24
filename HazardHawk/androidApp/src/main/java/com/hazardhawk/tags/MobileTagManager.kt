package com.hazardhawk.tags

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.hazardhawk.ui.components.*
import com.hazardhawk.ui.theme.*
import kotlinx.coroutines.launch
import java.util.*

// Temporary definitions for missing types from shared module
enum class AIStatus { IDLE, ANALYZING, COMPLETE, ERROR }
data class FieldConditions(
    val brightnessLevel: BrightnessLevel,
    val isWearingGloves: Boolean,
    val isEmergencyMode: Boolean,
    val noiseLevel: Float,
    val batteryLevel: Float
)
enum class BrightnessLevel { VERY_DARK, DARK, NORMAL, BRIGHT, VERY_BRIGHT }

/**
 * Mobile-friendly tag management dialog for construction safety photo tagging.
 * 
 * This component provides a comprehensive interface for construction workers to quickly
 * and efficiently tag safety hazards in photos. Built with mobile-first design principles,
 * it ensures usability even with work gloves in bright outdoor conditions.
 * 
 * Key Features:
 * - Mobile-optimized touch targets (44dp minimum)
 * - Safety orange theme for high visibility
 * - Real-time search functionality
 * - Custom tag creation with color coding
 * - Persistent storage with offline support
 * - Usage-based tag ordering for efficiency
 * 
 * Architecture:
 * - Uses Jetpack Compose with Material3 design system
 * - Integrates with TagStorage for data persistence
 * - Follows clean architecture with UI/Business/Data separation
 * 
 * @param photoId Unique identifier for the photo being tagged
 * @param existingTags Set of currently selected tag IDs (for editing existing photos)
 * @param onTagsUpdated Callback invoked when user saves tag changes
 * @param onDismiss Callback invoked when user cancels or dismisses dialog
 * @param modifier Optional Compose modifier for customization
 * 
 * @see TagStorage For persistence layer implementation
 * @see UITag For tag data model
 * 
 * @author HazardHawk Development Team
 * @since 1.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileTagManager(
    photoId: String,
    existingTags: Set<String> = emptySet(),
    aiRecommendations: List<UITagRecommendation> = emptyList(),
    autoSelectTags: Set<String> = emptySet(),
    aiStatus: AIStatus = AIStatus.IDLE,
    onTagsUpdated: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    onAnalyzePhoto: () -> Unit = {},
    fieldConditions: FieldConditions = FieldConditions(
        brightnessLevel = BrightnessLevel.NORMAL,
        isWearingGloves = false,
        isEmergencyMode = false,
        noiseLevel = 0.3f,
        batteryLevel = 0.8f
    ),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tagStorage = remember { context.createTagStorage() }
    val coroutineScope = rememberCoroutineScope()
    
    var selectedTags by remember { mutableStateOf(existingTags) }
    var searchQuery by remember { mutableStateOf("") }
    var availableTags by remember { mutableStateOf(getDefaultQuickTags()) }
    var filteredTags by remember { mutableStateOf(availableTags) }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Safety orange theme colors
    val safetyOrange = Color(0xFFFF6B35)
    val safetyOrangeLight = Color(0xFFFFF3F0)
    
    // Load available tags from storage on component initialization
    // This runs once when the composable is first created and loads:
    // 1. Custom tags created by the user
    // 2. Usage statistics for tag ordering
    // 3. Merges with default safety tags
    LaunchedEffect(Unit) {
        try {
            val customTags = tagStorage.loadCustomTags()
            val usageCounts = tagStorage.loadTagUsageCounts()
            
            // Merge default tags with custom tags and update usage counts
            val allTags = (getDefaultQuickTags() + customTags).map { tag ->
                tag.copy(usageCount = usageCounts[tag.id] ?: tag.usageCount)
            }.sortedByDescending { it.usageCount } // Sort by usage frequency
            
            availableTags = allTags
            isLoading = false
            
        } catch (e: Exception) {
            e.printStackTrace() 
            isLoading = false
        }
    }
    
    // Filter tags based on user search input
    // This runs whenever the search query or available tags change
    // Provides real-time search results for better user experience
    LaunchedEffect(searchQuery, availableTags) {
        filteredTags = if (searchQuery.isBlank()) {
            availableTags
        } else {
            availableTags.filter { tag ->
                tag.name.contains(searchQuery, ignoreCase = true) ||
                tag.getCategoryDisplayName().contains(searchQuery, ignoreCase = true)
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
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with title and close button
                TagManagerHeader(
                    selectedTagsCount = selectedTags.size,
                    onClose = onDismiss
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Search bar
                TagSearchBar(
                    searchQuery = searchQuery,
                    onSearchChanged = { searchQuery = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tag list
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF6B35),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading tags...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    items(filteredTags) { tag ->
                        TagItem(
                            tag = tag,
                            isSelected = tag.id in selectedTags,
                            onToggle = { tagId ->
                                selectedTags = if (tagId in selectedTags) {
                                    selectedTags - tagId
                                } else {
                                    selectedTags + tagId
                                }
                            }
                        )
                    }
                    
                    // Add new tag option if search doesn't match any existing tag
                    if (searchQuery.isNotBlank() && 
                        !filteredTags.any { it.name.equals(searchQuery, ignoreCase = true) }) {
                        item {
                            AddNewTagItem(
                                searchQuery = searchQuery,
                                onClick = { showNewTagDialog = true }
                            )
                        }
                    }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Action buttons
                TagManagerActions(
                    selectedCount = selectedTags.size,
                    onSave = {
                        onTagsUpdated(selectedTags)
                        onDismiss()
                    },
                    onCancel = onDismiss
                )
            }
        }
    }

    // New tag creation dialog
    if (showNewTagDialog) {
        CreateNewTagDialog(
            initialName = searchQuery,
            onTagCreated = { name, category, color ->
                // Create new tag and save it to storage
                coroutineScope.launch {
                    val newTag = UITag(
                        id = "custom_${UUID.randomUUID()}",
                        name = name,
                        category = category,
                        displayColor = color,
                        isCustom = true
                    )
                    
                    // Save to persistent storage
                    tagStorage.saveCustomTag(newTag)
                    
                    // Update UI
                    availableTags = availableTags + newTag
                    selectedTags = selectedTags + newTag.id
                    showNewTagDialog = false
                }
            },
            onDismiss = { showNewTagDialog = false }
        )
    }
}

@Composable
private fun TagManagerHeader(
    selectedTagsCount: Int,
    onClose: () -> Unit
) {
    val safetyOrange = Color(0xFFFF6B35)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = safetyOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Manage Tags",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            AnimatedVisibility(visible = selectedTagsCount > 0) {
                Text(
                    text = "$selectedTagsCount tag${if (selectedTagsCount == 1) "" else "s"} selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = safetyOrange,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        IconButton(
            onClick = onClose,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.size(44.dp) // Minimum touch target
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagSearchBar(
    searchQuery: String,
    onSearchChanged: (String) -> Unit
) {
    val safetyOrange = Color(0xFFFF6B35)
    
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChanged,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp), // Minimum touch target height
        placeholder = {
            Text(
                text = "Search tags...",
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
            focusedBorderColor = safetyOrange,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun TagItem(
    tag: UITag,
    isSelected: Boolean,
    onToggle: (String) -> Unit
) {
    val safetyOrange = Color(0xFFFF6B35)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp) // Minimum touch target height
            .clickable { onToggle(tag.id) },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            tag.displayColor.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = if (isSelected) {
            BorderStroke(2.dp, tag.displayColor)
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
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle(tag.id) },
                colors = CheckboxDefaults.colors(
                    checkedColor = safetyOrange,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Color indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        tag.displayColor,
                        CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Tag info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
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
                }
            }
        }
    }
}

@Composable
internal fun AddNewTagItem(
    searchQuery: String,
    onClick: () -> Unit
) {
    val safetyOrange = Color(0xFFFF6B35)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = safetyOrange.copy(alpha = 0.1f),
        border = BorderStroke(2.dp, safetyOrange.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = safetyOrange,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Add New Tag",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = safetyOrange
                )
                Text(
                    text = "Create \"$searchQuery\" as custom tag",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TagManagerActions(
    selectedCount: Int,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val safetyOrange = Color(0xFFFF6B35)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .height(56.dp), // Minimum touch target
            shape = RoundedCornerShape(12.dp),
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
                containerColor = safetyOrange,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "Save Changes",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateNewTagDialog(
    initialName: String,
    onTagCreated: (String, TagCategory, Color) -> Unit,
    onDismiss: () -> Unit
) {
    var tagName by remember { mutableStateOf(initialName) }
    var selectedCategory by remember { mutableStateOf(TagCategory.GENERAL_SAFETY) }
    var selectedColor by remember { mutableStateOf(Color(0xFFFF6B35)) }
    
    val tagColors = listOf(
        Color(0xFFEF4444), // Red
        Color(0xFFF59E0B), // Yellow
        Color(0xFF3B82F6), // Blue  
        Color(0xFF10B981), // Green
        Color(0xFF8B5CF6), // Purple
        Color(0xFFFF6B35), // Safety Orange
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create New Tag",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tag name input
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Tag Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Color picker
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tagColors) { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (selectedColor == color) 3.dp else 1.dp,
                                    color = if (selectedColor == color) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tagName.isNotBlank()) {
                        onTagCreated(tagName.trim(), selectedCategory, selectedColor)
                    }
                },
                enabled = tagName.isNotBlank()
            ) {
                Text("Save Tag")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}