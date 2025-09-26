package com.hazardhawk.ui.gallery.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.delay

/**
 * Performance Optimizations for PhotoViewer Components
 * 
 * Key optimizations:
 * - Lazy loading of heavy components
 * - Efficient recomposition patterns
 * - Memory-conscious rendering
 */

/**
 * Lazy Loading Wrapper
 * Delays loading of heavy components until needed
 */
@Composable
fun LazyLoadingWrapper(
    isVisible: Boolean,
    loadingDelay: Long = 100L,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var shouldLoad by remember(isVisible) { mutableStateOf(false) }
    
    LaunchedEffect(isVisible) {
        if (isVisible && !shouldLoad) {
            delay(loadingDelay)
            shouldLoad = true
        }
    }
    
    Box(modifier = modifier) {
        if (shouldLoad) {
            content()
        }
    }
}

/**
 * Stable Recomposition Wrapper
 * Prevents unnecessary recompositions by providing stable state
 */
@Composable
fun <T> StableWrapper(
    data: T,
    content: @Composable (T) -> Unit
) {
    val stableData by remember(data) { mutableStateOf(data) }
    content(stableData)
}

/**
 * Memory-Efficient List Renderer
 * Only renders visible items to reduce memory usage
 */
@Composable
fun MemoryEfficientList(
    itemCount: Int,
    visibleItems: Int = 5,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int) -> Unit
) {
    val startIndex = maxOf(0, currentIndex - visibleItems / 2)
    val endIndex = minOf(itemCount - 1, currentIndex + visibleItems / 2)
    
    SubcomposeLayout(modifier = modifier) { constraints ->
        val placeables = (startIndex..endIndex).map { index ->
            subcompose(index) {
                itemContent(index)
            }.map { it.measure(constraints) }
        }.flatten()
        
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.fastForEach { placeable ->
                placeable.placeRelative(0, 0)
            }
        }
    }
}

/**
 * Fade Transition Wrapper
 * Smooth fade transitions for UI elements
 */
@Composable
fun FadeTransition(
    visible: Boolean,
    animationDuration: Long = 300L,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var targetAlpha by remember { mutableStateOf(if (visible) 1f else 0f) }
    var currentAlpha by remember { mutableFloatStateOf(targetAlpha) }
    
    LaunchedEffect(visible) {
        targetAlpha = if (visible) 1f else 0f
        
        // Simple fade animation
        val steps = 20
        val stepDuration = animationDuration / steps
        val alphaStep = (targetAlpha - currentAlpha) / steps
        
        repeat(steps) {
            currentAlpha += alphaStep
            delay(stepDuration)
        }
        currentAlpha = targetAlpha
    }
    
    Box(
        modifier = modifier
            .graphicsLayer { alpha = currentAlpha }
            .fillMaxSize()
    ) {
        if (currentAlpha > 0f) {
            content()
        }
    }
}

/**
 * Touch Performance Wrapper
 * Optimizes touch interactions for construction gloves
 */
@Composable
fun TouchOptimizedWrapper(
    enabled: Boolean = true,
    hapticFeedback: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                // Increase touch target size for construction gloves
                if (enabled) {
                    scaleX = 1.1f
                    scaleY = 1.1f
                }
            }
    ) {
        content()
    }
}

/**
 * Debounced State Manager
 * Prevents rapid state updates that cause performance issues
 */
class DebouncedStateManager<T>(
    private val debounceTimeMs: Long = 300L
) {
    private var lastUpdateTime = 0L
    private val pendingUpdates = mutableMapOf<String, T>()
    
    fun updateWithDebounce(key: String, value: T, onUpdate: (T) -> Unit) {
        val currentTime = System.currentTimeMillis()
        pendingUpdates[key] = value
        
        if (currentTime - lastUpdateTime > debounceTimeMs) {
            lastUpdateTime = currentTime
            pendingUpdates[key]?.let { onUpdate(it) }
            pendingUpdates.remove(key)
        }
    }
}