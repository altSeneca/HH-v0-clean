package com.hazardhawk.ui.components

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.hazardhawk.ui.components.WheelItem
import com.hazardhawk.ui.components.WheelSide
import com.hazardhawk.ui.components.WheelSelectorConfig
import com.hazardhawk.ui.components.WheelSelectorLogic
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Android implementation of WheelSelector using Jetpack Compose
 * Provides invisible wheel illusion with scaling/fading but no visible wheel graphics
 */
@Composable
fun WheelSelector(
    items: List<WheelItem>,
    selectedIndex: Int,
    onChange: (item: WheelItem, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    onChangeLive: ((progressValue: Float) -> Unit)? = null,
    config: WheelSelectorConfig = WheelSelectorConfig(),
    formatItem: @Composable (WheelItem, WheelItemState) -> Unit = { item, state ->
        DefaultWheelItem(item, state, config)
    }
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    
    // State management - Fixed to ensure all items can be selected including the first ones
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0)
    )
    
    val logic = remember { WheelSelectorLogic() }
    var lastSelectedIndex by remember { mutableIntStateOf(selectedIndex) }
    var isDragging by remember { mutableStateOf(false) }
    var scrollVelocity by remember { mutableFloatStateOf(0f) }
    
    // Calculate current center index based on scroll position
    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportSize.height / 2
            
            layoutInfo.visibleItemsInfo.minByOrNull { itemInfo ->
                abs((itemInfo.offset + itemInfo.size / 2) - viewportCenter)
            }?.index ?: selectedIndex
        }
    }
    
    // Calculate scroll offset for smooth animations
    val scrollOffset by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val centerItemInfo = layoutInfo.visibleItemsInfo.find { it.index == centerIndex }
            
            if (centerItemInfo != null) {
                val viewportCenter = layoutInfo.viewportSize.height / 2
                val itemCenter = centerItemInfo.offset + centerItemInfo.size / 2
                val offsetPixels = itemCenter - viewportCenter
                
                // Convert to item units
                offsetPixels.toFloat() / centerItemInfo.size.toFloat()
            } else {
                0f
            }
        }
    }
    
    // Handle live updates for zoom
    LaunchedEffect(centerIndex, scrollOffset) {
        if (onChangeLive != null && isDragging) {
            val continuousValue = logic.calculateContinuousValue(items, centerIndex, scrollOffset)
            onChangeLive(continuousValue)
        }
    }
    
    // Handle snapping and onChange
    LaunchedEffect(listState.isScrollInProgress, centerIndex) {
        if (!listState.isScrollInProgress && config.snapEnabled) {
            // Delay to allow scroll to settle
            delay(50)
            
            val targetIndex = logic.calculateSnapTarget(centerIndex, scrollVelocity, items.size)
            
            if (targetIndex != lastSelectedIndex) {
                // Trigger haptic feedback
                if (config.hapticEnabled) {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (e: Exception) {
                        // Fallback to vibrator
                        try {
                            val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
                            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        } catch (ex: Exception) {
                            // Ignore vibration errors
                        }
                    }
                }
                
                lastSelectedIndex = targetIndex
                onChange(items[targetIndex], targetIndex)
            }
            
            // Snap to center - Fixed offset calculation to ensure proper centering
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = targetIndex,
                    scrollOffset = 0 // Let content padding handle the centering
                )
            }
        }
    }
    
    // Calculate item states for rendering
    val itemStates = logic.calculateItemStates(items, centerIndex, scrollOffset, config)
    
    // Optional center alignment marker
    Box(modifier = modifier) {
        // Invisible wheel selector with LazyColumn
        LazyColumn(
            state = listState,
            modifier = Modifier
                .height(with(density) { (config.itemHeight * config.visibleCount).dp })
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { 
                            isDragging = false
                            scrollVelocity = 0f
                        }
                    ) { _, dragAmount ->
                        scrollVelocity = dragAmount.y
                    }
                }
                .semantics {
                    contentDescription = when (config.side) {
                        WheelSide.LEFT -> "Aspect ratio selector"
                        WheelSide.RIGHT -> "Zoom control"
                    }
                },
            horizontalAlignment = when (config.side) {
                WheelSide.LEFT -> Alignment.Start
                WheelSide.RIGHT -> Alignment.End
            },
            contentPadding = PaddingValues(
                top = with(density) { (config.itemHeight * (config.visibleCount / 2)).dp },
                bottom = with(density) { (config.itemHeight * (config.visibleCount / 2)).dp }
            )
        ) {
            itemsIndexed(items) { index, item ->
                val itemState = itemStates.getOrNull(index) ?: return@itemsIndexed
                
                Box(
                    modifier = Modifier
                        .height(with(density) { config.itemHeight.dp })
                        .fillMaxWidth()
                        .alpha(if (itemState.isSelected) 1f else 0.6f) // Simple alpha instead of graphicsLayer
                        .clearAndSetSemantics {
                            contentDescription = "${item.label}${if (itemState.isSelected) " selected" else ""}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    formatItem(item, itemState)
                }
            }
        }
        
        // Optional faint center alignment marker
        Divider(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(1.dp)
                .align(Alignment.Center)
                .alpha(0.3f),
            color = Color.White
        )
    }
}

/**
 * Default item renderer for WheelSelector
 */
@Composable
private fun DefaultWheelItem(
    item: WheelItem,
    state: WheelItemState,
    config: WheelSelectorConfig
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (state.isSelected) {
                    Color.White.copy(alpha = 0.2f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.label,
            color = Color.White,
            fontSize = if (state.isSelected) 16.sp else 14.sp,
            fontWeight = if (state.isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Aspect Ratio Selector - Left Side
 */
@Composable
fun AspectRatioSelector(
    items: List<WheelItem>,
    selectedIndex: Int,
    onChange: (item: WheelItem, index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    WheelSelector(
        items = items,
        selectedIndex = selectedIndex,
        onChange = onChange,
        modifier = modifier,
        config = WheelSelectorConfig(
            side = WheelSide.LEFT,
            snapEnabled = true,
            hapticEnabled = true
        )
    )
}

/**
 * Zoom Control Selector - Right Side  
 */
@Composable
fun ZoomControlSelector(
    items: List<WheelItem>,
    selectedIndex: Int,
    onChange: (item: WheelItem, index: Int) -> Unit,
    onChangeLive: ((progressValue: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    WheelSelector(
        items = items,
        selectedIndex = selectedIndex,
        onChange = onChange,
        onChangeLive = onChangeLive,
        modifier = modifier,
        config = WheelSelectorConfig(
            side = WheelSide.RIGHT,
            snapEnabled = true,
            hapticEnabled = true
        )
    ) { item, state ->
        // Custom zoom formatter
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    color = if (state.isSelected) {
                        Color.White.copy(alpha = 0.2f)
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.label,
                color = Color.White,
                fontSize = if (state.isSelected) 16.sp else 14.sp,
                fontWeight = if (state.isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}