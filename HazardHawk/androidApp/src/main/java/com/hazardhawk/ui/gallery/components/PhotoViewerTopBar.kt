package com.hazardhawk.ui.gallery.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.hazardhawk.domain.entities.Photo
import com.hazardhawk.ui.gallery.PhotoNavigationState

// Construction-safe colors
private val SafetyOrange = Color(0xFFFF6B35)
private val SafetyGreen = Color(0xFF10B981)
private val DangerRed = Color(0xFFEF4444)

/**
 * Clean Top Bar for Photo Viewer
 * Simplified navigation controls
 */
@Composable
fun PhotoViewerTopBar(
    currentPhoto: Photo,
    navigationState: PhotoNavigationState,
    isVisible: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (isVisible) 1f else 0f
            }
    ) {
        // Status bar spacing
        Spacer(modifier = Modifier.height(40.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            FloatingIconButton(
                onClick = onBack,
                backgroundColor = SafetyOrange,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to gallery",
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Navigation controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingIconButton(
                    onClick = onPrevious,
                    enabled = navigationState.canNavigatePrevious,
                    backgroundColor = if (navigationState.canNavigatePrevious) SafetyOrange else Color.Gray.copy(alpha = 0.3f),
                    contentColor = Color.White,
                    size = 48.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.NavigateBefore,
                        contentDescription = "Previous photo",
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                FloatingIconButton(
                    onClick = onNext,
                    enabled = navigationState.canNavigateNext,
                    backgroundColor = if (navigationState.canNavigateNext) SafetyOrange else Color.Gray.copy(alpha = 0.3f),
                    contentColor = Color.White,
                    size = 48.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.NavigateNext,
                        contentDescription = "Next photo",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingIconButton(
                    onClick = onShare,
                    backgroundColor = SafetyGreen,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share photo",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                FloatingIconButton(
                    onClick = onDelete,
                    backgroundColor = DangerRed,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete photo",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingIconButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    content: @Composable () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Surface(
        onClick = {
            if (enabled) {
                hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                shadowElevation = 12.0f
                shape = CircleShape
                clip = true
            },
        enabled = enabled,
        shape = CircleShape,
        color = backgroundColor.copy(alpha = if (enabled) 0.9f else 0.5f),
        shadowElevation = if (enabled) 12.dp else 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalContentColor provides contentColor
            ) {
                content()
            }
        }
    }
}