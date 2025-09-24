package com.hazardhawk.ui.gallery

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.domain.entities.Photo

/**
 * OSHA-Compliant Tag Editor
 * Construction safety tags organized by OSHA categories
 */

// OSHA-compliant construction safety colors
private val SafetyOrange = Color(0xFFFF6B35)
private val SafetyGreen = Color(0xFF10B981)
private val DangerRed = Color(0xFFEF4444)
private val WarningYellow = Color(0xFFF59E0B)
private val InfoBlue = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditingBottomSheet(
    photo: Photo,
    onDismiss: () -> Unit,
    onTagsUpdated: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTags by remember { mutableStateOf(photo.tags.toSet()) }
    var customTagInput by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.fillMaxHeight(0.95f), // Almost full screen for comprehensive tagging
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            TagEditorHeader(
                photoId = photo.id,
                selectedCount = selectedTags.size,
                onSave = {
                    onTagsUpdated(selectedTags.toList())
                    onDismiss()
                },
                onCancel = onDismiss
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tag categories in scrollable content
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Fall Protection - Critical safety category
                item {
                    OSHATagCategory(
                        title = "Fall Protection (OSHA 1926.95)",
                        subtitle = "Required for work above 6 feet",
                        icon = Icons.Default.Security,
                        color = DangerRed,
                        tags = OSHAStandardTags.FALL_PROTECTION,
                        selectedTags = selectedTags,
                        onTagToggle = { tag ->
                            selectedTags = if (selectedTags.contains(tag)) {
                                selectedTags - tag
                            } else {
                                selectedTags + tag
                            }
                        }
                    )
                }
                
                // PPE Requirements
                item {
                    OSHATagCategory(
                        title = "PPE Requirements (OSHA 1926.95)",
                        subtitle = "Personal protective equipment",
                        icon = Icons.Default.Security,
                        color = SafetyOrange,
                        tags = OSHAStandardTags.PPE_REQUIREMENTS,
                        selectedTags = selectedTags,
                        onTagToggle = { tag ->
                            selectedTags = if (selectedTags.contains(tag)) {
                                selectedTags - tag
                            } else {
                                selectedTags + tag
                            }
                        }
                    )
                }
                
                // Electrical Safety
                item {
                    OSHATagCategory(
                        title = "Electrical Safety (OSHA 1926.416)",
                        subtitle = "Electrical hazards and lockout/tagout",
                        icon = Icons.Default.ElectricBolt,
                        color = WarningYellow,
                        tags = OSHAStandardTags.ELECTRICAL_SAFETY,
                        selectedTags = selectedTags,
                        onTagToggle = { tag ->
                            selectedTags = if (selectedTags.contains(tag)) {
                                selectedTags - tag
                            } else {
                                selectedTags + tag
                            }
                        }
                    )
                }
                
                // Excavation & Trenching
                item {
                    OSHATagCategory(
                        title = "Excavation & Trenching (OSHA 1926.650)",
                        subtitle = "Cave-in protection and soil analysis",
                        icon = Icons.Default.Construction,
                        color = InfoBlue,
                        tags = OSHAStandardTags.EXCAVATION_TRENCHING,
                        selectedTags = selectedTags,
                        onTagToggle = { tag ->
                            selectedTags = if (selectedTags.contains(tag)) {
                                selectedTags - tag
                            } else {
                                selectedTags + tag
                            }
                        }
                    )
                }
                
                // Hazard Communication
                item {
                    OSHATagCategory(
                        title = "Hazard Communication (OSHA 1926.59)",
                        subtitle = "Chemical hazards and SDS requirements",
                        icon = Icons.Default.Warning,
                        color = DangerRed,
                        tags = OSHAStandardTags.HAZARD_COMMUNICATION,
                        selectedTags = selectedTags,
                        onTagToggle = { tag ->
                            selectedTags = if (selectedTags.contains(tag)) {
                                selectedTags - tag
                            } else {
                                selectedTags + tag
                            }
                        }
                    )
                }
                
                // Custom tags input
                item {
                    CustomTagsSection(
                        customTagInput = customTagInput,
                        onCustomTagInputChange = { customTagInput = it },
                        onAddCustomTag = { tag ->
                            if (tag.isNotBlank() && !selectedTags.contains(tag)) {
                                selectedTags = selectedTags + tag
                                customTagInput = ""
                            }
                        },
                        selectedCustomTags = selectedTags.filter { tag ->
                            OSHAStandardTags.ALL_TAGS.none { oshaTag -> oshaTag.equals(tag, ignoreCase = true) }
                        },
                        onRemoveCustomTag = { tag ->
                            selectedTags = selectedTags - tag
                        }
                    )
                }
                
                // Bottom padding for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun TagEditorHeader(
    photoId: String,
    selectedCount: Int,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onCancel) {
            Text(
                text = "Cancel",
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Safety Tags",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SafetyOrange
            )
            Text(
                text = "$selectedCount tags selected",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        
        TextButton(
            onClick = onSave,
            colors = ButtonDefaults.textButtonColors(
                contentColor = SafetyOrange
            )
        ) {
            Text(
                text = "Save",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun OSHATagCategory(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    tags: List<String>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Category header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Tag chips in a flow layout
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    OSHATagChip(
                        tag = tag,
                        isSelected = selectedTags.contains(tag),
                        color = color,
                        onToggle = { onTagToggle(tag) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OSHATagChip(
    tag: String,
    isSelected: Boolean,
    color: Color,
    onToggle: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onToggle,
        label = {
            Text(
                text = tag,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (isSelected) color else Color.Transparent,
            labelColor = if (isSelected) Color.White else color,
            iconColor = if (isSelected) Color.White else color,
            selectedContainerColor = color,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = color.copy(alpha = 0.5f),
            selectedBorderColor = color,
            borderWidth = 2.dp
        ),
        modifier = Modifier.height(48.dp) // Large touch target for gloves
    )
}

@Composable
private fun CustomTagsSection(
    customTagInput: String,
    onCustomTagInputChange: (String) -> Unit,
    onAddCustomTag: (String) -> Unit,
    selectedCustomTags: List<String>,
    onRemoveCustomTag: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Gray.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Custom Safety Tags",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Gray.copy(alpha = 0.8f)
            )
            
            Text(
                text = "Add project-specific or unique hazard tags",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Custom tag input
            OutlinedTextField(
                value = customTagInput,
                onValueChange = onCustomTagInputChange,
                label = { Text("Add custom tag") },
                placeholder = { Text("e.g., Crane Operation, Hot Work, Confined Space") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (customTagInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onAddCustomTag(customTagInput.trim())
                                keyboardController?.hide()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add tag",
                                tint = SafetyGreen
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (customTagInput.isNotBlank()) {
                            onAddCustomTag(customTagInput.trim())
                            keyboardController?.hide()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SafetyGreen,
                    cursorColor = SafetyGreen
                )
            )
            
            // Show custom tags if any
            if (selectedCustomTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedCustomTags.forEach { tag ->
                        CustomTagChip(
                            tag = tag,
                            onRemove = { onRemoveCustomTag(tag) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomTagChip(
    tag: String,
    onRemove: () -> Unit
) {
    Surface(
        color = Color.Gray.copy(alpha = 0.8f),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.height(48.dp),
        onClick = { /* Do nothing - click handled by remove button */ }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = tag,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove tag",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * OSHA Standard Construction Safety Tags
 * Based on 29 CFR 1926 Construction Standards
 */
object OSHAStandardTags {
    
    val FALL_PROTECTION = listOf(
        "Fall Hazard - 6+ Feet",
        "Guardrails Required",
        "Safety Net Installed", 
        "Personal Fall Arrest System",
        "Harness & Lanyard Required",
        "Hole Covers Installed",
        "Ladder Safety",
        "Scaffold Safety"
    )
    
    val PPE_REQUIREMENTS = listOf(
        "Hard Hat Required",
        "Safety Glasses Required",
        "High-Vis Vest Required",
        "Steel-Toed Boots Required",
        "Hearing Protection Required",
        "Respirator Required",
        "Cut-Resistant Gloves",
        "Chemical-Resistant Gloves"
    )
    
    val ELECTRICAL_SAFETY = listOf(
        "Energized Equipment",
        "Lockout/Tagout Required",
        "Qualified Person Only",
        "Arc Flash Hazard",
        "Ground Fault Protection",
        "Overhead Power Lines",
        "Electrical Panel Work",
        "Temporary Wiring"
    )
    
    val EXCAVATION_TRENCHING = listOf(
        "Cave-In Hazard",
        "Trench Box Required",
        "Sloping Required",
        "Competent Person Present",
        "Atmospheric Testing",
        "Egress Ladder Required",
        "Spoil Pile Setback",
        "Utility Location"
    )
    
    val HAZARD_COMMUNICATION = listOf(
        "Chemical Hazard",
        "SDS Available",
        "Proper Labeling Required",
        "Ventilation Required",
        "Chemical Storage",
        "Spill Kit Available",
        "Eye Wash Station",
        "Emergency Shower"
    )
    
    val ALL_TAGS = FALL_PROTECTION + PPE_REQUIREMENTS + ELECTRICAL_SAFETY + 
                   EXCAVATION_TRENCHING + HAZARD_COMMUNICATION
}

// Flow layout for tag chips (simplified implementation)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Simplified flow layout - in production would use Google's FlowRow
    // For now, just use a simple Column
    Column(modifier = modifier) {
        content()
    }
}