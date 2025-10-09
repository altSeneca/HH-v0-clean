package com.hazardhawk.ui.components

/**
 * Data models for WheelSelector component
 */
data class WheelItem(
    val id: String,
    val label: String,
    val icon: String? = null,
    val value: Any? = null
)

enum class WheelSide {
    LEFT, RIGHT
}

/**
 * Configuration for WheelSelector behavior
 */
data class WheelSelectorConfig(
    val itemHeight: Float = 52f, // dp
    val visibleCount: Int = 5,
    val snapEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val side: WheelSide = WheelSide.LEFT,
    val animationDurationMs: Int = 200
)

/**
 * State for individual items in the wheel
 */
data class WheelItemState(
    val item: WheelItem,
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val isSelected: Boolean = false
)

/**
 * Core wheel selector state and animations logic
 * This handles the invisible wheel math for scaling/fading/curving
 */
class WheelSelectorLogic {
    
    /**
     * Calculate item states based on scroll position
     */
    fun calculateItemStates(
        items: List<WheelItem>,
        centerIndex: Int,
        scrollOffset: Float,
        config: WheelSelectorConfig
    ): List<WheelItemState> {
        return items.mapIndexed { index, item ->
            val distanceFromCenter = (index - centerIndex).toFloat() + scrollOffset
            val absDistance = kotlin.math.abs(distanceFromCenter)
            
            // Calculate scale with invisible wheel illusion
            val scale = when {
                absDistance < 0.5f -> 1.1f // Center item: 110% scale
                absDistance < 1.5f -> {
                    // Neighbors: interpolate between 110% and 85%
                    val t = (absDistance - 0.5f) / 1f
                    1.1f - (t * 0.25f) // 1.1f to 0.85f
                }
                else -> 0.7f // Far items: 70% scale
            }
            
            // Calculate alpha with smooth fade
            val alpha = when {
                absDistance < 0.5f -> 1f // Center item: 100% opacity
                absDistance < 1.5f -> {
                    // Neighbors: interpolate between 100% and 70%
                    val t = (absDistance - 0.5f) / 1f
                    1f - (t * 0.3f) // 1f to 0.7f
                }
                else -> 0.5f // Far items: 50% opacity
            }
            
            val isSelected = absDistance < 0.5f
            
            WheelItemState(
                item = item,
                scale = scale,
                alpha = alpha,
                isSelected = isSelected
            )
        }
    }
    
    /**
     * Calculate snap target index based on current position
     */
    fun calculateSnapTarget(
        currentIndex: Int,
        scrollVelocity: Float,
        itemCount: Int
    ): Int {
        val velocityThreshold = 50f
        val targetIndex = when {
            scrollVelocity > velocityThreshold -> (currentIndex + 1).coerceAtMost(itemCount - 1)
            scrollVelocity < -velocityThreshold -> (currentIndex - 1).coerceAtLeast(0)
            else -> currentIndex
        }
        return targetIndex.coerceIn(0, itemCount - 1)
    }
    
    /**
     * Calculate continuous value for live updates (used for zoom)
     */
    fun calculateContinuousValue(
        items: List<WheelItem>,
        centerIndex: Int,
        scrollOffset: Float
    ): Float {
        if (items.isEmpty()) return 0f
        
        val clampedIndex = (centerIndex + scrollOffset).coerceIn(0f, (items.size - 1).toFloat())
        
        // Linear interpolation between item values
        val lowerIndex = clampedIndex.toInt().coerceIn(0, items.size - 1)
        val upperIndex = (lowerIndex + 1).coerceIn(0, items.size - 1)
        
        if (lowerIndex == upperIndex) {
            return items[lowerIndex].value as? Float ?: 0f
        }
        
        val fraction = clampedIndex - lowerIndex
        val lowerValue = items[lowerIndex].value as? Float ?: 0f
        val upperValue = items[upperIndex].value as? Float ?: 0f
        
        return lowerValue + (fraction * (upperValue - lowerValue))
    }
}

/**
 * Aspect ratio definitions for camera
 */
object AspectRatios {
    val FULL = WheelItem("full", "Full", value = 0f) // No crop
    val RATIO_16_9 = WheelItem("16:9", "16:9", value = 16f/9f)
    val RATIO_3_2 = WheelItem("3:2", "3:2", value = 3f/2f)
    val RATIO_4_3 = WheelItem("4:3", "4:3", value = 4f/3f)
    val RATIO_1_1 = WheelItem("1:1", "1:1", value = 1f)
    
    fun getSupported(): List<WheelItem> = listOf(
        FULL, RATIO_16_9, RATIO_4_3, RATIO_1_1
    )
}

/**
 * Zoom level definitions
 */
object ZoomLevels {
    fun generateZoomItems(minZoom: Float = 0.5f, maxZoom: Float = 10f): List<WheelItem> {
        val items = mutableListOf<WheelItem>()
        
        // Define clean zoom levels for better user experience: 1x, 2x, 5x, 10x, etc.
        val commonZoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f, 15.0f, 20.0f, 30.0f)
        
        // Filter to supported range and add items
        commonZoomLevels.filter { it >= minZoom && it <= maxZoom }.forEach { zoom ->
            val label = when {
                zoom < 1f -> "${(zoom * 10).toInt() / 10f}×"
                zoom == zoom.toInt().toFloat() -> "${zoom.toInt()}×"
                else -> "${(zoom * 10).toInt() / 10f}×"
            }
            
            items.add(WheelItem(
                id = "zoom_$zoom",
                label = label,
                value = zoom
            ))
        }
        
        // Ensure we have at least basic zoom levels if range is very limited
        if (items.isEmpty()) {
            items.add(WheelItem(id = "zoom_1.0", label = "1×", value = 1.0f))
        }
        
        return items
    }
}