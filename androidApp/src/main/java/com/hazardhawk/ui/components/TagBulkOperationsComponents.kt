/*
 * Copyright (c) 2025 HazardHawk Safety Platform
 *
 * Bulk tag operations components for construction environments.
 * Handles multi-photo tagging, batch operations, and bulk management.
 */
package com.hazardhawk.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hazardhawk.domain.entities.*
import com.hazardhawk.shared.ui.components.*
import com.hazardhawk.ui.theme.*

/**
 * Bulk Tag Operations Panel
 * 
 * Features:
 * - Multi-photo selection
 * - Bulk tag application/removal
 * - Progress tracking
 * - Construction-friendly controls
 * - Undo/redo support
 */
@Composable
fun BulkTagOperationsPanel(
    selectedPhotos: List<String>,
    availableTags: List<Tag>,
    onBulkApplyTags: (List<String>, Set<String>) -> Unit,
    onBulkRemoveTags: (List<String>, Set<String>) -> Unit,
    onClearPhotoSelection: () -> Unit,
    isProcessing: Boolean = false,
    processingProgress: Float = 0f,
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
    
    var selectedTagsForBulk by remember { mutableStateOf(setOf<String>()) }
    var showBulkDialog by remember { mutableStateOf(false) }
    var bulkOperation by remember { mutableStateOf(BulkOperation.APPLY) }
    
    AnimatedVisibility(
        visible = selectedPhotos.isNotEmpty(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(Dimensions.Spacing.MEDIUM.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(ColorPalette.Primary.getColor(colorScheme)).copy(alpha = 0.08f)
            ),
            border = BorderStroke(
                width = Dimensions.Border.STANDARD.dp,
                color = Color(ColorPalette.Primary.getColor(colorScheme)).copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = Dimensions.Elevation.LEVEL2.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Spacing.MEDIUM.dp)
            ) {
                // Header with selection count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color(ColorPalette.Primary.getColor(colorScheme)),
                            modifier = Modifier.size(Dimensions.IconSize.STANDARD.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(Dimensions.Spacing.SMALL.dp))
                        
                        Column {
                            Text(
                                text = "Bulk Tag Operations",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(ColorPalette.OnSurface.getColor(colorScheme))
                            )
                            
                            Text(
                                text = "${selectedPhotos.size} photos selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(ColorPalette.OnSurface.getColor(colorScheme)).copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClearPhotoSelection()
                        },
                        modifier = Modifier.size(Dimensions.TouchTargets.COMFORTABLE.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear selection",
                            tint = Color(ColorPalette.OnSurface.getColor(colorScheme)).copy(alpha = 0.6f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Dimensions.Spacing.MEDIUM.dp))
                
                // Action buttons
                if (!isProcessing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.Spacing.SMALL.dp)
                    ) {
                        HazardHawkPrimaryButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                bulkOperation = BulkOperation.APPLY
                                showBulkDialog = true
                            },
                            text = "Add Tags",
                            icon = Icons.Default.Add,
                            fieldConditions = fieldConditions,
                            modifier = Modifier.weight(1f)
                        )
                        
                        HazardHawkSecondaryButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                bulkOperation = BulkOperation.REMOVE
                                showBulkDialog = true
                            },
                            text = "Remove Tags",
                            icon = Icons.Default.Remove,
                            fieldConditions = fieldConditions,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bulk operation types
 */
enum class BulkOperation {
    APPLY, REMOVE
}