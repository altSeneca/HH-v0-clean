package com.hazardhawk.ui.camera

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
// Using simple Image component
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.hazardhawk.ui.theme.ConstructionColors
import com.hazardhawk.ui.theme.HazardColors
import com.hazardhawk.tags.models.UITagRecommendation
import java.io.File

/**
 * Post-Capture Screen for HazardHawk
 * 
 * Displays captured photo with dynamic OSHA tagging and AI analysis workflow.
 * Optimized for construction workers with large touch targets and high contrast design.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PostCaptureScreen(
    capturedPhoto: File,
    aiAnalysisResult: List<UITagRecommendation>? = null,
    isAnalyzing: Boolean = false,
    onSaveWithTags: (List<String>) -> Unit,
    onRetake: () -> Unit,
    onAnalyzeWithAI: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var animationStarted by remember { mutableStateOf(false) }
    
    val haptic = LocalHapticFeedback.current
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        animationStarted = true
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.9f),
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            )
            .clickable { onDismiss() } // Tap outside to dismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo Preview Card
            AnimatedVisibility(
                visible = animationStarted,
                enter = slideInVertically(
                    initialOffsetY = { -100 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                PhotoPreviewCard(
                    photoFile = capturedPhoto,
                    onDismiss = onDismiss
                )
            }
            
            // OSHA Tagging Section
            AnimatedVisibility(
                visible = animationStarted,
                enter = slideInHorizontally(
                    initialOffsetX = { -300 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                OSHATaggingSection(
                    selectedTags = selectedTags,
                    onTagsChanged = { tags -> selectedTags = tags },
                    aiSuggestions = aiAnalysisResult
                )
            }
            
            // AI Analysis Section
            AnimatedVisibility(
                visible = animationStarted,
                enter = slideInHorizontally(
                    initialOffsetX = { 300 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                AIAnalysisSection(
                    isAnalyzing = isAnalyzing,
                    analysisResult = aiAnalysisResult,
                    onAnalyze = onAnalyzeWithAI
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action Buttons
            AnimatedVisibility(
                visible = animationStarted,
                enter = slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                PostCaptureActionButtons(
                    selectedTagsCount = selectedTags.size,
                    onRetake = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRetake()
                    },
                    onSave = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSaveWithTags(selectedTags.toList())
                    }
                )
            }
        }
    }
}

/**
 * Photo preview card with dismiss option
 */
@Composable
private fun PhotoPreviewCard(
    photoFile: File,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Photo Display - simplified placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ConstructionColors.ConcreteGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = "Captured Photo",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
            
            // Dismiss Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Photo Info Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Captured Photo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * OSHA tagging section with dynamic tag selection
 */
@Composable
fun OSHATaggingSection(
    selectedTags: Set<String>,
    onTagsChanged: (Set<String>) -> Unit,
    aiSuggestions: List<UITagRecommendation>? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Label,
                    contentDescription = null,
                    tint = ConstructionColors.SafetyOrange,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Safety Tags",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConstructionColors.OnSurface
                )
                if (selectedTags.isNotEmpty()) {
                    Badge(
                        containerColor = ConstructionColors.SafetyOrange
                    ) {
                        Text(
                            text = selectedTags.size.toString(),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // AI Suggested Tags (if available)
            if (!aiSuggestions.isNullOrEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AI Suggestions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ConstructionColors.SafetyGreen
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(aiSuggestions) { suggestion ->
                            SuggestionTagChip(
                                tag = suggestion.tag.name,
                                confidence = suggestion.confidence,
                                isSelected = suggestion.tag.name in selectedTags,
                                onToggle = { tag ->
                                    onTagsChanged(
                                        if (tag in selectedTags) {
                                            selectedTags - tag
                                        } else {
                                            selectedTags + tag
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            // Manual Tag Selection
            Text(
                text = "Common OSHA Categories",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = ConstructionColors.OnSurface
            )
            
            val commonTags = listOf(
                "PPE Required" to HazardColors.HIGH_ORANGE,
                "Fall Hazard" to HazardColors.CautionRed,
                "Equipment Issue" to HazardColors.SafetyYellow,
                "Housekeeping" to HazardColors.SafetyGreen,
                "Electrical" to HazardColors.CautionRed,
                "Chemical" to HazardColors.SafetyOrange,
                "Fire Safety" to HazardColors.CautionRed,
                "First Aid" to HazardColors.SafetyGreen
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(commonTags) { (tag, color) ->
                    OSHATagChip(
                        tag = tag,
                        color = color,
                        isSelected = tag in selectedTags,
                        onToggle = { tagName ->
                            onTagsChanged(
                                if (tagName in selectedTags) {
                                    selectedTags - tagName
                                } else {
                                    selectedTags + tagName
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * AI analysis section with progress and results
 */
@Composable
fun AIAnalysisSection(
    isAnalyzing: Boolean,
    analysisResult: List<UITagRecommendation>?,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.SurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = ConstructionColors.SafetyGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "AI Safety Analysis",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConstructionColors.OnSurface
                )
            }
            
            when {
                isAnalyzing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ConstructionColors.SafetyGreen,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Analyzing safety conditions...",
                            fontSize = 14.sp,
                            color = ConstructionColors.OnSurfaceVariant
                        )
                    }
                }
                
                !analysisResult.isNullOrEmpty() -> {
                    Text(
                        text = "Analysis complete: ${analysisResult.size} hazards detected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ConstructionColors.SafetyGreen
                    )
                }
                
                else -> {
                    OutlinedButton(
                        onClick = onAnalyze,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ConstructionColors.SafetyGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze with AI")
                    }
                }
            }
        }
    }
}

/**
 * Action buttons for retake and save
 */
@Composable
private fun PostCaptureActionButtons(
    selectedTagsCount: Int,
    onRetake: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Retake Button
        OutlinedButton(
            onClick = onRetake,
            modifier = Modifier
                .weight(1f)
                .height(56.dp), // Construction glove compatibility
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White,
                disabledContentColor = Color.Gray
            ),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Retake",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Save Button
        Button(
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .height(56.dp), // Construction glove compatibility
            colors = ButtonDefaults.buttonColors(
                containerColor = ConstructionColors.SafetyOrange
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedTagsCount > 0) "Save ($selectedTagsCount tags)" else "Save Photo",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * OSHA tag chip with color coding
 */
@Composable
private fun OSHATagChip(
    tag: String,
    color: Color,
    isSelected: Boolean,
    onToggle: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    FilterChip(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onToggle(tag)
        },
        label = {
            Text(
                text = tag,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        },
        selected = isSelected,
        enabled = true,
        modifier = Modifier.heightIn(min = 40.dp), // Large touch target
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = Color.White
        )
    )
}

/**
 * AI suggestion tag chip with confidence indicator
 */
@Composable
private fun SuggestionTagChip(
    tag: String,
    confidence: Float,
    isSelected: Boolean,
    onToggle: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val confidenceColor = when {
        confidence >= 0.8f -> ConstructionColors.SafetyGreen
        confidence >= 0.6f -> HazardColors.SafetyYellow
        else -> HazardColors.SafetyOrange
    }
    
    FilterChip(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onToggle(tag)
        },
        label = {
            Column {
                Text(
                    text = tag,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(confidence * 100).toInt()}%",
                    fontSize = 10.sp,
                    color = confidenceColor
                )
            }
        },
        selected = isSelected,
        enabled = true,
        modifier = Modifier.heightIn(min = 44.dp), // Large touch target
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = confidenceColor,
            selectedLabelColor = Color.White
        )
    )
}