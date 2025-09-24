package com.hazardhawk.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * Construction-optimized UI components for HazardHawk
 *
 * Designed specifically for construction workers with:
 * - Large touch targets (72dp+) for gloved hands
 * - High contrast colors for outdoor visibility
 * - Consistent safety-oriented design language
 * - Professional appearance building user trust
 */

/**
 * Button size enumeration for construction environments
 */
enum class ButtonSize {
    SMALL,      // 48dp - minimal use
    MEDIUM,     // 56dp - secondary actions
    LARGE,      // 72dp - primary actions (recommended)
    EXTRA_LARGE // 80dp - critical safety actions
}

/**
 * Primary action button with safety orange styling
 * Optimized for construction worker usage with large touch targets
 */
@Composable
fun ConstructionPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.LARGE,
    loading: Boolean = false
) {
    val hapticFeedback = LocalHapticFeedback.current

    val buttonHeight = when (size) {
        ButtonSize.SMALL -> 48.dp
        ButtonSize.MEDIUM -> 56.dp
        ButtonSize.LARGE -> 72.dp
        ButtonSize.EXTRA_LARGE -> 80.dp
    }

    val fontSize = when (size) {
        ButtonSize.SMALL -> 14.sp
        ButtonSize.MEDIUM -> 16.sp
        ButtonSize.LARGE -> 18.sp
        ButtonSize.EXTRA_LARGE -> 20.sp
    }

    Button(
        onClick = {
            if (!loading) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        enabled = enabled && !loading,
        modifier = modifier.height(buttonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = ConstructionColors.SafetyOrange,
            contentColor = Color.White,
            disabledContainerColor = ConstructionColors.ConcreteGray,
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Secondary action button with steel blue styling
 * For navigation and less critical actions
 */
@Composable
fun ConstructionSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.MEDIUM
) {
    val hapticFeedback = LocalHapticFeedback.current

    val buttonHeight = when (size) {
        ButtonSize.SMALL -> 48.dp
        ButtonSize.MEDIUM -> 56.dp
        ButtonSize.LARGE -> 72.dp
        ButtonSize.EXTRA_LARGE -> 80.dp
    }

    val fontSize = when (size) {
        ButtonSize.SMALL -> 14.sp
        ButtonSize.MEDIUM -> 16.sp
        ButtonSize.LARGE -> 18.sp
        ButtonSize.EXTRA_LARGE -> 20.sp
    }

    Button(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        modifier = modifier.height(buttonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = ConstructionColors.SteelBlue,
            contentColor = Color.White,
            disabledContainerColor = ConstructionColors.ConcreteGray,
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Outline button for tertiary actions
 * Maintains visibility while being less prominent
 */
@Composable
fun ConstructionOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.MEDIUM
) {
    val hapticFeedback = LocalHapticFeedback.current

    val buttonHeight = when (size) {
        ButtonSize.SMALL -> 48.dp
        ButtonSize.MEDIUM -> 56.dp
        ButtonSize.LARGE -> 72.dp
        ButtonSize.EXTRA_LARGE -> 80.dp
    }

    val fontSize = when (size) {
        ButtonSize.SMALL -> 14.sp
        ButtonSize.MEDIUM -> 16.sp
        ButtonSize.LARGE -> 18.sp
        ButtonSize.EXTRA_LARGE -> 20.sp
    }

    OutlinedButton(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        modifier = modifier.height(buttonHeight),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = ConstructionColors.SafetyOrange,
            disabledContentColor = ConstructionColors.ConcreteGray
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 2.dp
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Icon button with customizable color scheme
 * Optimized for construction worker glove usage
 */
@Composable
fun ConstructionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.LARGE,
    containerColor: Color = ConstructionColors.SteelBlue,
    contentColor: Color = Color.White,
    enabled: Boolean = true
) {
    val hapticFeedback = LocalHapticFeedback.current

    val buttonSize = when (size) {
        ButtonSize.SMALL -> 48.dp
        ButtonSize.MEDIUM -> 56.dp
        ButtonSize.LARGE -> 72.dp
        ButtonSize.EXTRA_LARGE -> 80.dp
    }

    val iconSize = when (size) {
        ButtonSize.SMALL -> 20.dp
        ButtonSize.MEDIUM -> 24.dp
        ButtonSize.LARGE -> 28.dp
        ButtonSize.EXTRA_LARGE -> 32.dp
    }

    FloatingActionButton(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.size(buttonSize),
        containerColor = if (enabled) containerColor else ConstructionColors.ConcreteGray,
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.6f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = if (enabled) contentColor else contentColor.copy(alpha = 0.6f)
        )
    }
}

/**
 * Emergency button for critical safety actions
 * High visibility red with extra large touch target
 */
@Composable
fun ConstructionEmergencyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val hapticFeedback = LocalHapticFeedback.current

    Button(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        modifier = modifier.height(80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFDC2626), // Emergency red
            contentColor = Color.White,
            disabledContainerColor = ConstructionColors.ConcreteGray,
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 32.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Toggle button for settings and configuration
 * Provides clear visual feedback for on/off states
 */
@Composable
fun ConstructionToggleButton(
    text: String,
    isToggled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.MEDIUM
) {
    val hapticFeedback = LocalHapticFeedback.current

    val buttonHeight = when (size) {
        ButtonSize.SMALL -> 48.dp
        ButtonSize.MEDIUM -> 56.dp
        ButtonSize.LARGE -> 72.dp
        ButtonSize.EXTRA_LARGE -> 80.dp
    }

    val fontSize = when (size) {
        ButtonSize.SMALL -> 14.sp
        ButtonSize.MEDIUM -> 16.sp
        ButtonSize.LARGE -> 18.sp
        ButtonSize.EXTRA_LARGE -> 20.sp
    }

    Button(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onToggle(!isToggled)
        },
        enabled = enabled,
        modifier = modifier.height(buttonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isToggled) ConstructionColors.SafetyOrange else ConstructionColors.ConcreteGray,
            contentColor = Color.White,
            disabledContainerColor = ConstructionColors.ConcreteGray.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Card component with construction design language
 * Consistent elevation and corner radius for grouped content
 */
@Composable
fun ConstructionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    if (onClick != null) {
        Card(
            modifier = modifier.clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = ConstructionColors.AsphaltBlack
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = ConstructionColors.AsphaltBlack
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                content = content
            )
        }
    }
}

/**
 * Design system object for consistent theming
 */
object ConstructionDesignSystem {
    // Consistent spacing
    val SpacingSmall = 8.dp
    val SpacingMedium = 16.dp
    val SpacingLarge = 24.dp
    val SpacingExtraLarge = 32.dp

    // Consistent corner radius
    val CornerRadiusSmall = 8.dp
    val CornerRadiusMedium = 12.dp
    val CornerRadiusLarge = 16.dp

    // Touch target sizes
    val TouchTargetMinimum = 48.dp  // Android minimum
    val TouchTargetRecommended = 56.dp  // Android recommended
    val TouchTargetConstruction = 72.dp  // Construction glove optimized
    val TouchTargetEmergency = 80.dp  // Emergency action size

    // Typography scales
    object Typography {
        val BodySmall = 14.sp
        val Body = 16.sp
        val BodyLarge = 18.sp
        val Headline = 20.sp
        val HeadlineLarge = 24.sp
    }
}