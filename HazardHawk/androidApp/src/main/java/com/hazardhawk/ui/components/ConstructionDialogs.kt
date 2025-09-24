package com.hazardhawk.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * Construction-optimized dialog components with:
 * - 44dp minimum touch targets for safety compliance
 * - High contrast colors for outdoor visibility
 * - Large text for readability with safety glasses
 * - Proper button sizing to prevent text truncation
 * - Thumb-friendly button positioning
 */

@Composable
fun ConstructionDialog(
    onDismissRequest: (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismissRequest ?: {},
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress && onDismissRequest != null,
            dismissOnClickOutside = dismissOnClickOutside && onDismissRequest != null
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f) // Leave margin for thumb reach
                .heightIn(min = 280.dp, max = 640.dp)
                .imePadding()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(20.dp), // Larger radius for construction friendliness
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Title section with proper prominence
                title?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp, 28.dp, 24.dp, 16.dp)
                    ) {
                        ProvideTextStyle(
                            value = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 32.sp
                            )
                        ) {
                            it()
                        }
                    }
                }
                
                // Content section (scrollable with proper spacing)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    content()
                }
                
                // Actions section with construction-friendly spacing
                if (actions != {}) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp, 16.dp, 24.dp, 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions()
                    }
                }
            }
        }
    }
}

// ConstructionPrimaryButton moved to ConstructionButtons.kt to avoid duplication

/**
 * Secondary action button for construction use
 */
@Composable
fun ConstructionSecondaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 2.dp // Thicker border for visibility
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(
            horizontal = 24.dp,
            vertical = 14.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Compact secondary button for when space is limited
 */
@Composable
fun RowScope.ConstructionCompactButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 2.dp
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * Extended primary button for main actions
 */
@Composable
fun RowScope.ConstructionExtendedButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = ConstructionColors.SafetyOrange
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1.5f) // Takes more space than compact buttons
            .heightIn(min = 48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White,
            disabledContainerColor = containerColor.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        contentPadding = PaddingValues(
            horizontal = 20.dp,
            vertical = 12.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * Assessment Summary Dialog optimized for construction use
 */
@Composable
fun AssessmentSummaryDialog(
    title: String,
    photoPath: String?,
    items: List<String>,
    notes: String,
    onNotesChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    isPositiveAssessment: Boolean = true
) {
    ConstructionDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = if (isPositiveAssessment) ConstructionColors.SafetyGreen else ConstructionColors.SafetyOrange,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        content = {
            // Photo preview if available
            photoPath?.let { path ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    // AsyncImage would go here
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Photo Preview",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // Items summary
            Text(
                text = "${items.size} item${if (items.size != 1) "s" else ""} identified:",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Items list with proper spacing
            items.take(3).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPositiveAssessment) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isPositiveAssessment) ConstructionColors.SafetyGreen else ConstructionColors.SafetyOrange
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp
                        )
                    )
                }
            }
            
            if (items.size > 3) {
                Text(
                    text = "...and ${items.size - 3} more",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 32.dp, top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Notes section
            Text(
                text = "Additional Notes (Optional)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // FIXED: Using FlikkerTextField instead of OutlinedTextField
            FlikkerTextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = "Add any additional observations or context...",
                maxLines = 5,
                minLines = 3
            )
        },
        actions = {
            ConstructionCompactButton(
                onClick = onBack,
                text = "Back",
                icon = Icons.Default.ArrowBack
            )
            
            ConstructionExtendedButton(
                onClick = onSubmit,
                text = "Submit Assessment",
                icon = Icons.Default.Upload,
                containerColor = ConstructionColors.SafetyOrange
            )
        }
    )
}