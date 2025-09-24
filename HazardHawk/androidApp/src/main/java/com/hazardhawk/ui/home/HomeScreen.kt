package com.hazardhawk.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ui.theme.ConstructionColors
import com.hazardhawk.ui.theme.HazardColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Professional home screen for HazardHawk with construction-optimized navigation.
 * 
 * Features:
 * - Large touch targets (60dp+) for gloved hands
 * - High contrast colors for outdoor visibility
 * - Professional appearance suitable for safety documentation
 * - Three main navigation areas: Camera, Gallery, Glass
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToGlassCamera: () -> Unit = {},
    onNavigateToGlassGallery: () -> Unit = {},
    onNavigateToGlassSettings: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var animationStarted by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        animationStarted = true
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ConstructionColors.Surface,
                        ConstructionColors.SurfaceVariant
                    )
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header with Branding
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
            WelcomeBanner()
        }
        
        // Quick Stats Card  
        AnimatedVisibility(
            visible = animationStarted,
            enter = slideInVertically(
                initialOffsetY = { -50 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                )
            )
        ) {
            QuickStatsCard()
        }
        
        // Main Navigation Cards
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Camera Interface Card
            AnimatedVisibility(
                visible = animationStarted,
                enter = slideInHorizontally(
                    initialOffsetX = { -300 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    )
                )
            ) {
                NavigationCard(
                    title = "Smart Camera",
                    subtitle = "Capture & Analyze Safety Hazards",
                    icon = Icons.Default.CameraAlt,
                    backgroundColor = ConstructionColors.SafetyOrange,
                    onClick = onNavigateToCamera
                )
            }
            
            // Gallery Interface Card
            AnimatedVisibility(
                visible = animationStarted,
                enter = slideInHorizontally(
                    initialOffsetX = { 300 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    )
                )
            ) {
                NavigationCard(
                    title = "Photo Gallery",
                    subtitle = "Review & Manage Safety Photos",
                    icon = Icons.Default.PhotoLibrary,
                    backgroundColor = HazardColors.OSHA_BLUE,
                    onClick = onNavigateToGallery
                )
            }
            
            // Glass Interface Showcase Card
            AnimatedVisibility(
                visible = animationStarted,
                enter = slideInHorizontally(
                    initialOffsetX = { -300 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    )
                )
            ) {
                GlassInterfaceCard(
                    onNavigateToGlassCamera = onNavigateToGlassCamera,
                    onNavigateToGlassGallery = onNavigateToGlassGallery,
                    onNavigateToGlassSettings = onNavigateToGlassSettings
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Settings Access
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                )
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "v3.1.0 Production Ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = ConstructionColors.OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ConstructionColors.SurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = ConstructionColors.OnSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Professional welcome banner with branding and safety status.
 */
@Composable
private fun WelcomeBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.SafetyOrange
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "HazardHawk",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "AI-Powered Construction Safety",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            // Current time for professional appearance
            Text(
                text = SimpleDateFormat("EEEE, MMM dd, yyyy • HH:mm", Locale.getDefault())
                    .format(Date()),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Quick stats showing daily activity and safety metrics.
 */
@Composable
private fun QuickStatsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickStat(
                value = "24",
                label = "Photos Today",
                color = ConstructionColors.SafetyOrange
            )
            QuickStat(
                value = "3",
                label = "Hazards Found",
                color = HazardColors.HIGH_ORANGE
            )
            QuickStat(
                value = "92%",
                label = "Safety Score",
                color = ConstructionColors.SafetyGreen
            )
        }
    }
}

@Composable
private fun QuickStat(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = ConstructionColors.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Large navigation card optimized for gloved hands (60dp+ touch targets).
 */
@Composable
fun NavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "card_scale"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp) // Ensures 60dp+ touch target
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Large icon for easy recognition
            Card(
                modifier = Modifier.size(64.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            }
            
            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 16.sp
                )
            }
            
            // Navigation arrow
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Navigate",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Glass interface showcase card with access to all glass morphism screens.
 */
@Composable
private fun GlassInterfaceCard(
    onNavigateToGlassCamera: () -> Unit,
    onNavigateToGlassGallery: () -> Unit,
    onNavigateToGlassSettings: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.SafetyGreen
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main glass interface card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.size(64.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Glass Interface",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Premium Glass Morphism UI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Expandable glass interface options
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider(
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    GlassOption(
                        title = "Glass Camera",
                        subtitle = "Camera with glass morphism effects",
                        icon = Icons.Default.CameraAlt,
                        onClick = onNavigateToGlassCamera
                    )
                    
                    GlassOption(
                        title = "Glass Gallery", 
                        subtitle = "Photo gallery with advanced visuals",
                        icon = Icons.Default.PhotoLibrary,
                        onClick = onNavigateToGlassGallery
                    )
                    
                    GlassOption(
                        title = "Glass Settings",
                        subtitle = "Settings with construction optimizations",
                        icon = Icons.Default.Tune,
                        onClick = onNavigateToGlassSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(20.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Navigate",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            onNavigateToCamera = {},
            onNavigateToGallery = {},
            onNavigateToGlassCamera = {},
            onNavigateToGlassGallery = {},
            onNavigateToGlassSettings = {},
            onNavigateToSettings = {}
        )
    }
}