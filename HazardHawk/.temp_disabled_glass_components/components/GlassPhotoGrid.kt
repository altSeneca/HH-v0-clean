package com.hazardhawk.ui.glass.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
// Semantics import removed to fix compilation
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
// GlassConfiguration is now defined in GlassExtensions.kt
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * Construction-optimized photo grid with glass morphism effects and safety metadata display.
 * 
 * Features:
 * - Glass morphism photo cards with backdrop blur
 * - Multi-selection with translucent overlays
 * - Construction safety metadata display
 * - Large touch targets for gloved hands
 * - OSHA compliance visual indicators
 * - Parallax scrolling effects
 */

/**
 * Data class representing a photo with construction safety metadata
 */
data class GlassPhotoItem(
    val id: String,
    val uri: String,
    val thumbnailUri: String? = null,
    val timestamp: Long,
    val location: String? = null,
    val hazardLevel: HazardLevel = HazardLevel.NONE,
    val oshaCompliance: Boolean = true,
    val tags: List<String> = emptyList(),
    val analysisComplete: Boolean = false,
    val reportGenerated: Boolean = false,
    val isSelected: Boolean = false
)

enum class HazardLevel(val color: Color, val displayName: String) {
    NONE(ConstructionColors.SafetyGreen, "Safe"),
    LOW(ConstructionColors.SafetyOrange.copy(alpha = 0.7f), "Low Risk"),
    MEDIUM(ConstructionColors.SafetyOrange, "Medium Risk"),
    HIGH(ConstructionColors.CautionRed, "High Risk"),
    CRITICAL(ConstructionColors.CautionRed.copy(red = 0.9f), "Critical")
}

/**
 * Main glass photo grid component with construction optimizations
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassPhotoGrid(
    photos: List<GlassPhotoItem>,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    isSelectionMode: Boolean = false,
    onPhotoClick: (GlassPhotoItem) -> Unit,
    onPhotoLongPress: (GlassPhotoItem) -> Unit,
    onSelectionToggle: (GlassPhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    // Parallax scroll state for glass effects
    val scrollState = rememberLazyGridState()
    val parallaxOffset by remember {
        derivedStateOf {
            scrollState.firstVisibleItemScrollOffset * 0.3f
        }
    }
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        state = scrollState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 88.dp // Account for bottom navigation
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(photos, key = { it.id }) { photo ->
            GlassPhotoCard(
                photo = photo,
                configuration = configuration,
                hazeState = hazeState,
                isSelectionMode = isSelectionMode,
                parallaxOffset = parallaxOffset,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isSelectionMode) {
                        onSelectionToggle(photo)
                    } else {
                        onPhotoClick(photo)
                    }
                },
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPhotoLongPress(photo)
                },
                onSelectionToggle = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelectionToggle(photo)
                }
            )
        }
    }
}

/**
 * Individual glass photo card with construction safety indicators
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun GlassPhotoCard(
    photo: GlassPhotoItem,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    isSelectionMode: Boolean,
    parallaxOffset: Float,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onSelectionToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Selection animation
    val selectionScale by animateFloatAsState(
        targetValue = if (photo.isSelected) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "selection_scale"
    )
    
    // Hazard level pulse animation for high-risk photos
    val hazardPulse by rememberInfiniteTransition(label = "hazard_pulse").animateFloat(
        initialValue = if (photo.hazardLevel >= HazardLevel.MEDIUM) 0.7f else 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hazard_pulse"
    )
    
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = selectionScale
                scaleY = selectionScale
                translationY = -parallaxOffset * 0.1f
            }
            .pointerInput(photo.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.7f)
            } else Color.Transparent
        ),
        border = if (photo.hazardLevel >= HazardLevel.MEDIUM) {
            androidx.compose.foundation.BorderStroke(
                width = 3.dp,
                color = photo.hazardLevel.color.copy(alpha = hazardPulse)
            )
        } else if (photo.isSelected) {
            androidx.compose.foundation.BorderStroke(
                width = 3.dp,
                color = ConstructionColors.SafetyOrange
            )
        } else if (configuration.safetyBorderGlow) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = configuration.borderColor.copy(alpha = 0.4f)
            )
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (photo.isSelected) 8.dp else 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (configuration.supportLevel.name != "DISABLED") {
                        Modifier.hazeChild(
                            state = hazeState,
                            style = HazeMaterials.regular(
                                backgroundColor = photo.hazardLevel.color.copy(alpha = 0.05f)
                            )
                        )
                    } else Modifier
                )
        ) {
            // Photo image
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(photo.thumbnailUri ?: photo.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Safety Photo ${photo.id}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )
            
            // Gradient overlay for better text visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0.4f,
                            endY = 1.0f
                        )
                    )
            )
            
            // Selection mode overlay
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (photo.isSelected) {
                                ConstructionColors.SafetyOrange.copy(alpha = 0.3f)
                            } else {
                                Color.Black.copy(alpha = 0.3f)
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSelectionToggle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Selection checkbox
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (photo.isSelected) {
                                ConstructionColors.SafetyOrange
                            } else {
                                Color.White.copy(alpha = 0.8f)
                            }
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = ConstructionColors.SafetyOrange
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (photo.isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Top-left status indicators
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Hazard level indicator
                if (photo.hazardLevel != HazardLevel.NONE) {
                    GlassStatusBadge(
                        icon = when (photo.hazardLevel) {
                            HazardLevel.LOW -> Icons.Default.Warning
                            HazardLevel.MEDIUM -> Icons.Default.Warning
                            HazardLevel.HIGH -> Icons.Default.Dangerous
                            HazardLevel.CRITICAL -> Icons.Default.Emergency
                            else -> Icons.Default.Info
                        },
                        color = photo.hazardLevel.color,
                        size = 20.dp,
                        configuration = configuration
                    )
                }
                
                // OSHA compliance indicator
                if (!photo.oshaCompliance) {
                    GlassStatusBadge(
                        icon = Icons.Default.Error,
                        color = ConstructionColors.CautionRed,
                        size = 20.dp,
                        configuration = configuration
                    )
                }
                
                // Analysis status indicator
                if (!photo.analysisComplete) {
                    GlassStatusBadge(
                        icon = Icons.Default.Schedule,
                        color = ConstructionColors.SafetyOrange,
                        size = 20.dp,
                        configuration = configuration,
                        isAnimated = true
                    )
                }
            }
            
            // Bottom metadata overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tags
                if (photo.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        photo.tags.take(2).forEach { tag ->
                            GlassTagChip(
                                text = tag,
                                configuration = configuration,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        if (photo.tags.size > 2) {
                            GlassTagChip(
                                text = "+${photo.tags.size - 2}",
                                configuration = configuration
                            )
                        }
                    }
                }
                
                // Location and timestamp
                photo.location?.let { location ->
                    Text(
                        text = location,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Action buttons overlay (visible on hover/tap)
            var showActions by remember { mutableStateOf(false) }
            
            AnimatedVisibility(
                visible = showActions,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (photo.analysisComplete) {
                        GlassActionButton(
                            icon = Icons.Default.Visibility,
                            contentDescription = "View Analysis",
                            configuration = configuration,
                            onClick = { /* TODO: View analysis */ }
                        )
                    }
                    
                    if (photo.reportGenerated) {
                        GlassActionButton(
                            icon = Icons.Default.Assignment,
                            contentDescription = "View Report",
                            configuration = configuration,
                            onClick = { /* TODO: View report */ }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Glass status badge with construction safety styling
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun GlassStatusBadge(
    icon: ImageVector,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    configuration: GlassConfiguration,
    isAnimated: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animatedAlpha by rememberInfiniteTransition(label = "status_pulse").animateFloat(
        initialValue = if (isAnimated) 0.6f else 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_alpha"
    )
    
    Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                color.copy(alpha = 0.8f)
            } else Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = if (isAnimated) animatedAlpha else 0.8f)
        ),
        modifier = modifier.size(size + 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (configuration.supportLevel.name != "DISABLED") {
                        Modifier.background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.3f),
                                    color.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            ),
                            radius = size.value * 2
                        )
                    } else Modifier
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size)
            )
        }
    }
}

/**
 * Glass tag chip for photo metadata
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun GlassTagChip(
    text: String,
    configuration: GlassConfiguration,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                ConstructionColors.SafetyOrange.copy(alpha = 0.8f)
            } else Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = ConstructionColors.SafetyOrange.copy(alpha = 0.6f)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (configuration.supportLevel.name != "DISABLED") {
                        Modifier.background(
                            color = ConstructionColors.SafetyOrange.copy(alpha = 0.2f)
                        )
                    } else Modifier
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Glass action button for quick photo actions
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun GlassActionButton(
    icon: ImageVector,
    contentDescription: String,
    configuration: GlassConfiguration,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                ConstructionColors.SafetyOrange.copy(alpha = 0.9f)
            } else Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = ConstructionColors.SafetyOrange.copy(alpha = 0.8f)
        ),
        modifier = modifier
            .size(32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(
                    bounded = false,
                    radius = 16.dp,
                    color = ConstructionColors.SafetyOrange
                ),
                onClick = onClick
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (configuration.supportLevel.name != "DISABLED") {
                        Modifier.background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ConstructionColors.SafetyOrange.copy(alpha = 0.3f),
                                    ConstructionColors.SafetyOrange.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                    } else Modifier
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Glass loading state for photo grid
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassPhotoGridLoading(
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(12) { // Show 12 skeleton items
            Card(
                modifier = Modifier.aspectRatio(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (configuration.supportLevel.name == "DISABLED") {
                        Color.Gray.copy(alpha = 0.3f)
                    } else Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (configuration.supportLevel.name != "DISABLED") {
                                Modifier.hazeChild(
                                    state = hazeState,
                                    style = HazeMaterials.regular(
                                        backgroundColor = Color.White.copy(alpha = 0.1f),
                                        blurRadius = configuration.effectiveBlurRadius.dp * 0.5f
                                    )
                                )
                            } else Modifier
                        )
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.1f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Loading",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}