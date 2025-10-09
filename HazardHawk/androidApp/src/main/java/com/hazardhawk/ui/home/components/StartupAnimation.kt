package com.hazardhawk.ui.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ui.theme.ConstructionColors
import kotlinx.coroutines.delay

/**
 * 4-second startup animation sequence for HazardHawk dashboard.
 *
 * Animation sequence:
 * 1. Logo fade-in (0-1s): App branding appears with scale animation
 * 2. Hero section (1-2s): Welcome message and stats slide in
 * 3. Command center (2-3s): Main navigation cards appear
 * 4. Activity feed (3-4s): Recent activity items fade in
 *
 * @param onAnimationComplete Callback invoked when animation completes
 * @param content The dashboard content to display after animation
 */
@Composable
fun StartupAnimation(
    onAnimationComplete: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var animationPhase by remember { mutableStateOf(AnimationPhase.LOGO) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Phase 1: Logo (0-1s)
        delay(1000)
        animationPhase = AnimationPhase.HERO

        // Phase 2: Hero (1-2s)
        delay(1000)
        animationPhase = AnimationPhase.COMMAND_CENTER

        // Phase 3: Command Center (2-3s)
        delay(1000)
        animationPhase = AnimationPhase.FEED

        // Phase 4: Feed (3-4s)
        delay(1000)
        animationPhase = AnimationPhase.COMPLETE
        showContent = true
        onAnimationComplete()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showContent) {
            // Show actual content with fade-in
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500))
            ) {
                content()
            }
        } else {
            // Show animation sequence
            AnimationSequence(animationPhase)
        }
    }
}

/**
 * Animation sequence component that renders each phase.
 */
@Composable
private fun AnimationSequence(phase: AnimationPhase) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ConstructionColors.Surface,
                        ConstructionColors.SurfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Phase 1: Logo
            LogoPhase(visible = phase >= AnimationPhase.LOGO)

            Spacer(modifier = Modifier.height(16.dp))

            // Phase 2: Hero Section
            HeroPhase(visible = phase >= AnimationPhase.HERO)

            Spacer(modifier = Modifier.height(24.dp))

            // Phase 3: Command Center Preview
            CommandCenterPhase(visible = phase >= AnimationPhase.COMMAND_CENTER)

            Spacer(modifier = Modifier.height(24.dp))

            // Phase 4: Activity Feed Preview
            FeedPhase(visible = phase >= AnimationPhase.FEED)
        }
    }
}

/**
 * Phase 1: Logo animation with scale and fade.
 */
@Composable
private fun LogoPhase(visible: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "logo_alpha"
    )

    Card(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .alpha(alpha),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.SafetyOrange
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "HazardHawk Logo",
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
        }
    }
}

/**
 * Phase 2: Hero section with app title and tagline.
 */
@Composable
private fun HeroPhase(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(animationSpec = tween(500))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "HazardHawk",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = ConstructionColors.OnSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "AI-Powered Construction Safety",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ConstructionColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Quick stats preview
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StatPreview(icon = Icons.Default.PhotoCamera, value = "24")
                StatPreview(icon = Icons.Default.Warning, value = "3")
                StatPreview(icon = Icons.Default.CheckCircle, value = "92%")
            }
        }
    }
}

/**
 * Phase 3: Command center preview showing main features.
 */
@Composable
private fun CommandCenterPhase(visible: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                initialOffsetX = { -300 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn()
        ) {
            FeaturePreview(
                title = "Smart Camera",
                icon = Icons.Default.CameraAlt,
                color = ConstructionColors.SafetyOrange
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                initialOffsetX = { 300 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn()
        ) {
            FeaturePreview(
                title = "Photo Gallery",
                icon = Icons.Default.PhotoLibrary,
                color = ConstructionColors.WorkZoneBlue
            )
        }
    }
}

/**
 * Phase 4: Activity feed preview showing recent items.
 */
@Composable
private fun FeedPhase(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(800)) + expandVertically()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Recent Activity",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ConstructionColors.OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Preview items
            FeedItemPreview(
                icon = Icons.Default.Assignment,
                title = "PTP Created",
                color = ConstructionColors.SafetyGreen
            )
            FeedItemPreview(
                icon = Icons.Default.Warning,
                title = "Hazard Detected",
                color = ConstructionColors.SafetyOrange
            )
        }
    }
}

/**
 * Stat preview component for hero phase.
 */
@Composable
private fun StatPreview(icon: ImageVector, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ConstructionColors.SafetyOrange,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ConstructionColors.OnSurface
        )
    }
}

/**
 * Feature preview card for command center phase.
 */
@Composable
private fun FeaturePreview(title: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Feed item preview for activity feed phase.
 */
@Composable
private fun FeedItemPreview(title: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ConstructionColors.OnSurface
            )
        }
    }
}

/**
 * Animation phase enum to track progress.
 */
private enum class AnimationPhase {
    LOGO,
    HERO,
    COMMAND_CENTER,
    FEED,
    COMPLETE
}
