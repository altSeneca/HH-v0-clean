package com.hazardhawk.ui.camera.hud

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import coil.compose.AsyncImage  // TODO: Add Coil dependency or use alternative
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * Post-Capture Overlay for AI-Powered Tagging
 * 
 * High-touch, rapid tagging interface optimized for construction workers
 * Integrates AI analysis with manual tag selection
 */
@Composable
fun PostCaptureOverlay(
    capturedPhoto: HUDPhotoAnalysis?,
    onTagSelected: (AITag) -> Unit,
    onConfirmCapture: () -> Unit,
    onRetakePhoto: () -> Unit,
    emergencyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var showAIAnalysis by remember { mutableStateOf(false) }
    
    if (capturedPhoto == null) return
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
    ) {
        // Captured Photo Preview (Frozen in place)
        // TODO: Replace with actual photo display when Coil dependency is added
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ConstructionColors.AsphaltBlack),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Photo Preview\n${capturedPhoto.photoPath}",
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
        
        // Overlay Gradient for better text contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        ),
                        startY = 300f
                    )
                )
        )
        
        // AI Analysis Indicator
        AIAnalysisIndicator(
            analysisInProgress = capturedPhoto.aiTags.isEmpty(),
            hasAnalysis = capturedPhoto.aiTags.isNotEmpty(),
            onToggleAnalysis = {
                showAIAnalysis = !showAIAnalysis
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
        
        // Quick Tag Selection Area
        QuickTagSelector(
            aiTags = capturedPhoto.aiTags,
            selectedTags = selectedTags,
            emergencyMode = emergencyMode,
            onTagSelected = { tag ->
                selectedTags = if (selectedTags.contains(tag.id)) {
                    selectedTags - tag.id
                } else {
                    selectedTags + tag.id
                }
                onTagSelected(tag)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        )
        
        // Action Buttons Row
        PostCaptureActions(
            selectedTagCount = selectedTags.size,
            emergencyMode = emergencyMode,
            onConfirm = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onConfirmCapture()
            },
            onRetake = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onRetakePhoto()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
        
        // Detailed AI Analysis (Expandable)
        AnimatedVisibility(
            visible = showAIAnalysis,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            AIAnalysisPanel(
                photo = capturedPhoto,
                emergencyMode = emergencyMode,
                onDismiss = { showAIAnalysis = false }
            )
        }
    }
}

/**
 * AI Analysis Indicator with pulsating animation
 * Shows AI processing state and confidence
 */
@Composable
private fun AIAnalysisIndicator(
    analysisInProgress: Boolean,
    hasAnalysis: Boolean,
    onToggleAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Surface(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(enabled = hasAnalysis) { onToggleAnalysis() },
        color = when {
            analysisInProgress -> ConstructionColors.PendingOrange.copy(alpha = pulseAlpha)
            hasAnalysis -> ConstructionColors.SafetyGreen
            else -> ConstructionColors.ConcreteGray
        },
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                analysisInProgress -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
                hasAnalysis -> {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Analysis Complete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "AI Analysis Failed",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Quick Tag Selection Interface
 * Large, touch-friendly tags for rapid selection
 */
@Composable
private fun QuickTagSelector(
    aiTags: List<AITag>,
    selectedTags: Set<String>,
    emergencyMode: Boolean,
    onTagSelected: (AITag) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (aiTags.isNotEmpty()) {
            Text(
                text = "Tap to add tags:",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(aiTags.take(6)) { tag -> // Limit to 6 most confident tags
                    QuickTagButton(
                        tag = tag,
                        isSelected = selectedTags.contains(tag.id),
                        emergencyMode = emergencyMode,
                        onSelected = { onTagSelected(tag) }
                    )
                }
            }
        } else {
            // Show manual tag options while AI processes
            Text(
                text = "Add tags manually:",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(getDefaultConstructionTags()) { tag ->
                    QuickTagButton(
                        tag = tag,
                        isSelected = selectedTags.contains(tag.id),
                        emergencyMode = emergencyMode,
                        onSelected = { onTagSelected(tag) }
                    )
                }
            }
        }
    }
}

/**
 * Individual Quick Tag Button
 * High-contrast, construction-optimized design
 */
@Composable
private fun QuickTagButton(
    tag: AITag,
    isSelected: Boolean,
    emergencyMode: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected && emergencyMode -> ConstructionColors.CautionRed
        isSelected -> ConstructionColors.SafetyOrange
        tag.isHazard -> ConstructionColors.Warning.copy(alpha = 0.8f)
        else -> ConstructionColors.SteelBlue.copy(alpha = 0.8f)
    }
    
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onSelected() },
        color = backgroundColor,
        shadowElevation = if (isSelected) 6.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hazard indicator
            if (tag.isHazard) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Hazard",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // Tag text
            Text(
                text = tag.text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Confidence indicator
            if (tag.confidence > 0f) {
                Text(
                    text = "${(tag.confidence * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

/**
 * Post-Capture Action Buttons
 * Confirm, Retake, and additional options
 */
@Composable
private fun PostCaptureActions(
    selectedTagCount: Int,
    emergencyMode: Boolean,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Retake Button
        Surface(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .clickable { onRetake() },
            color = ConstructionColors.ConcreteGray.copy(alpha = 0.9f),
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retake Photo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Confirm Button with tag count
        Surface(
            modifier = Modifier
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .clickable { onConfirm() },
            color = if (emergencyMode) {
                ConstructionColors.CautionRed
            } else {
                ConstructionColors.SafetyGreen
            },
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Confirm",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = if (selectedTagCount > 0) {
                        "Save ($selectedTagCount tags)"
                    } else {
                        "Save Photo"
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Detailed AI Analysis Panel
 * Expandable side panel with full analysis results
 */
@Composable
private fun AIAnalysisPanel(
    photo: HUDPhotoAnalysis,
    emergencyMode: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .padding(16.dp),
        color = if (emergencyMode) {
            Color.Black.copy(alpha = 0.95f)
        } else {
            ConstructionColors.AsphaltBlack.copy(alpha = 0.95f)
        },
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Analysis",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confidence Score
            Text(
                text = "Confidence: ${(photo.confidenceScore * 100).toInt()}%",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Detailed tag list
            if (photo.aiTags.isNotEmpty()) {
                Text(
                    text = "Detected Items:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                photo.aiTags.forEach { tag ->
                    AITagDetailItem(
                        tag = tag,
                        emergencyMode = emergencyMode
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Hazard warning if detected
            if (photo.hazardDetected) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = ConstructionColors.CautionRed,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Hazard Warning",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Text(
                            text = "Safety hazard detected",
                            color = Color.White,
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
 * Individual AI Tag Detail Item
 */
@Composable
private fun AITagDetailItem(
    tag: AITag,
    emergencyMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (tag.isHazard) {
            ConstructionColors.Warning.copy(alpha = 0.3f)
        } else {
            ConstructionColors.SteelBlue.copy(alpha = 0.3f)
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tag.text,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${(tag.confidence * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            
            if (tag.oshaCode != null) {
                Text(
                    text = "OSHA: ${tag.oshaCode}",
                    color = ConstructionColors.SafetyOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Default construction tags for manual selection
 */
private fun getDefaultConstructionTags(): List<AITag> {
    return listOf(
        AITag("ppe_hardhat", "Hard Hat", 1f, TagCategory.PPE),
        AITag("ppe_safety_vest", "Safety Vest", 1f, TagCategory.PPE),
        AITag("ppe_gloves", "Gloves", 1f, TagCategory.PPE),
        AITag("equipment_ladder", "Ladder", 1f, TagCategory.EQUIPMENT),
        AITag("hazard_height", "Height Risk", 1f, TagCategory.HAZARD, isHazard = true),
        AITag("structural_concrete", "Concrete", 1f, TagCategory.STRUCTURAL)
    )
}