package com.hazardhawk.ui.glass.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
// Semantics import removed to fix compilation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
// Removed deprecated rememberRipple import
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
// GlassConfiguration is now defined in GlassExtensions.kt
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * Construction-optimized glass modal dialogs with glass morphism effects.
 * 
 * Features:
 * - Glass morphism confirmation dialogs with backdrop blur
 * - Bulk operation dialogs for photo management
 * - Safety-critical confirmation workflows
 * - Large touch targets for construction environments
 * - Emergency mode support with high contrast
 * - OSHA compliance workflow dialogs
 */

/**
 * Glass confirmation dialog for safety-critical operations
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "CONFIRM",
    cancelText: String = "CANCEL",
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    icon: ImageVector = Icons.Default.Warning,
    isDestructive: Boolean = false,
    requiresDoubleConfirm: Boolean = false,
    onDismiss: () -> Unit = onCancel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var firstConfirmClick by remember { mutableStateOf(false) }
    
    // Destructive action pulse animation
    val destructivePulse by rememberInfiniteTransition(label = "destructive_pulse").animateFloat(
        initialValue = if (isDestructive) 0.7f else 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "destructive_alpha"
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !requiresDoubleConfirm,
            dismissOnClickOutside = !requiresDoubleConfirm,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (configuration.supportLevel.name == "DISABLED") {
                    Color.Black.copy(alpha = 0.95f)
                } else Color.Transparent
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = if (isDestructive) 3.dp else 2.dp,
                color = if (isDestructive) {
                    ConstructionColors.CautionRed.copy(alpha = destructivePulse)
                } else {
                    configuration.borderColor.copy(alpha = 0.8f)
                }
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 16.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .then(
                        if (configuration.supportLevel.name != "DISABLED") {
                            Modifier.hazeChild(
                                state = hazeState,
                                style = HazeMaterials.thick(
                                    backgroundColor = if (isDestructive) {
                                        ConstructionColors.CautionRed.copy(alpha = 0.1f)
                                    } else configuration.tintColor
                                )
                            )
                        } else Modifier
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Icon with pulsing animation for destructive actions
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDestructive) {
                            ConstructionColors.CautionRed.copy(alpha = 0.2f)
                        } else ConstructionColors.SafetyOrange.copy(alpha = 0.2f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        color = if (isDestructive) {
                            ConstructionColors.CautionRed.copy(alpha = destructivePulse)
                        } else ConstructionColors.SafetyOrange
                    ),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isDestructive) {
                                ConstructionColors.CautionRed.copy(alpha = destructivePulse)
                            } else ConstructionColors.SafetyOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Title
                Text(
                    text = title,
                    color = if (configuration.isHighContrastMode) configuration.borderColor else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                // Message
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                
                // Double confirmation warning
                if (requiresDoubleConfirm && !firstConfirmClick) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = ConstructionColors.SafetyOrange.copy(alpha = 0.2f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = ConstructionColors.SafetyOrange.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "This action requires double confirmation for safety",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCancel()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(configuration.minTouchTargetSize.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = cancelText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Confirm button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (requiresDoubleConfirm && !firstConfirmClick) {
                                firstConfirmClick = true
                            } else {
                                onConfirm()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(configuration.minTouchTargetSize.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDestructive) {
                                ConstructionColors.CautionRed
                            } else ConstructionColors.SafetyOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (requiresDoubleConfirm && !firstConfirmClick) {
                                "CLICK AGAIN TO $confirmText"
                            } else confirmText,
                            fontSize = if (requiresDoubleConfirm && !firstConfirmClick) 12.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Glass bulk operation dialog for photo management
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassBulkOperationDialog(
    title: String,
    selectedCount: Int,
    totalCount: Int,
    operations: List<BulkOperation>,
    onOperationSelected: (BulkOperation) -> Unit,
    onDismiss: () -> Unit,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (configuration.supportLevel.name == "DISABLED") {
                    Color.Black.copy(alpha = 0.95f)
                } else Color.Transparent
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = configuration.borderColor.copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 16.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .then(
                        if (configuration.supportLevel.name != "DISABLED") {
                            Modifier.hazeChild(
                                state = hazeState,
                                style = HazeMaterials.thick(
                                    backgroundColor = configuration.tintColor
                                )
                            )
                        } else Modifier
                    )
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            color = if (configuration.isHighContrastMode) configuration.borderColor else Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "$selectedCount of $totalCount photos selected",
                            color = ConstructionColors.SafetyOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier.size(configuration.minTouchTargetSize.dp * 0.7f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Operations list
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(operations) { operation ->
                        GlassBulkOperationItem(
                            operation = operation,
                            configuration = configuration,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOperationSelected(operation)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual bulk operation item
 */
@Composable
private fun GlassBulkOperationItem(
    operation: BulkOperation,
    configuration: GlassConfiguration,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Use null indication for glass morphism effect
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (operation.isDestructive) {
                ConstructionColors.CautionRed.copy(alpha = 0.1f)
            } else Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (operation.isDestructive) {
                ConstructionColors.CautionRed.copy(alpha = 0.6f)
            } else Color.White.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = configuration.minTouchTargetSize.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (operation.isDestructive) {
                        ConstructionColors.CautionRed.copy(alpha = 0.2f)
                    } else ConstructionColors.SafetyOrange.copy(alpha = 0.2f)
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = operation.icon,
                        contentDescription = null,
                        tint = if (operation.isDestructive) ConstructionColors.CautionRed else ConstructionColors.SafetyOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = operation.title,
                    color = if (operation.isDestructive) ConstructionColors.CautionRed else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = operation.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                
                if (operation.requiresConfirmation) {
                    Text(
                        text = "Requires confirmation",
                        color = ConstructionColors.SafetyOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Arrow indicator
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Progress dialog for long-running operations
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassProgressDialog(
    title: String,
    message: String,
    progress: Float,
    canCancel: Boolean = false,
    onCancel: () -> Unit = {},
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = { if (canCancel) onCancel() },
        properties = DialogProperties(
            dismissOnBackPress = canCancel,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (configuration.supportLevel.name == "DISABLED") {
                    Color.Black.copy(alpha = 0.9f)
                } else Color.Transparent
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = configuration.borderColor.copy(alpha = 0.8f)
            )
        ) {
            Column(
                modifier = Modifier
                    .then(
                        if (configuration.supportLevel.name != "DISABLED") {
                            Modifier.hazeChild(
                                state = hazeState,
                                style = HazeMaterials.regular(
                                    backgroundColor = configuration.tintColor
                                )
                            )
                        } else Modifier
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = title,
                    color = if (configuration.isHighContrastMode) configuration.borderColor else Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                // Progress indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Progress bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ConstructionColors.SafetyOrange,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    
                    // Progress percentage
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = ConstructionColors.SafetyOrange,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Message
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                
                // Cancel button (if allowed)
                if (canCancel) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(configuration.minTouchTargetSize.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "CANCEL",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Data class representing a bulk operation
 */
data class BulkOperation(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isDestructive: Boolean = false,
    val requiresConfirmation: Boolean = false
) {
    companion object {
        val DELETE_PHOTOS = BulkOperation(
            id = "delete",
            title = "Delete Photos",
            description = "Permanently remove selected photos from device",
            icon = Icons.Default.Delete,
            isDestructive = true,
            requiresConfirmation = true
        )
        
        val EXPORT_PHOTOS = BulkOperation(
            id = "export",
            title = "Export Photos",
            description = "Save selected photos to external storage",
            icon = Icons.Default.FileDownload,
            requiresConfirmation = false
        )
        
        val GENERATE_REPORT = BulkOperation(
            id = "report",
            title = "Generate Safety Report",
            description = "Create OSHA compliance report from selected photos",
            icon = Icons.Default.Assignment,
            requiresConfirmation = false
        )
        
        val SHARE_PHOTOS = BulkOperation(
            id = "share",
            title = "Share Photos",
            description = "Share selected photos with team members",
            icon = Icons.Default.Share,
            requiresConfirmation = false
        )
        
        val TAG_PHOTOS = BulkOperation(
            id = "tag",
            title = "Add Tags",
            description = "Apply safety tags to selected photos",
            icon = Icons.Default.Label,
            requiresConfirmation = false
        )
        
        val ANALYZE_PHOTOS = BulkOperation(
            id = "analyze",
            title = "AI Analysis",
            description = "Run safety analysis on selected photos",
            icon = Icons.Default.Psychology,
            requiresConfirmation = false
        )
    }
}