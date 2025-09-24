package com.hazardhawk.camera

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*
import kotlin.random.Random

/**
 * Advanced Burnin Prevention System
 * 
 * Implements comprehensive screen burnin prevention mechanisms for camera overlays
 * in construction environments where the app may run for extended periods.
 * 
 * Key Features:
 * - Dynamic positioning of static overlay elements
 * - Intelligent timeout management
 * - Transparency gradients instead of solid colors
 * - Subtle animation system to prevent static pixel patterns
 * - Safe area constraints for all overlay elements
 * - Adaptive behavior based on usage patterns
 */

/**
 * Burnin prevention configuration
 */
data class BurninPreventionConfig(
    // Positioning
    val enableDynamicPositioning: Boolean = true,
    val positionUpdateIntervalMs: Long = 30_000L, // 30 seconds
    val maxPositionOffset: Float = 16f, // pixels
    val positionVariationEnabled: Boolean = true,
    
    // Transparency and visual
    val useTransparencyGradients: Boolean = true,
    val minimumAlpha: Float = 0.3f,
    val maximumAlpha: Float = 0.8f,
    val enableSubtleAnimations: Boolean = true,
    
    // Timeout management
    val enableTimeoutManagement: Boolean = true,
    val staticElementTimeoutMs: Long = 300_000L, // 5 minutes
    val fadeOutDurationMs: Long = 2_000L, // 2 seconds
    val fadeInDurationMs: Long = 1_000L, // 1 second
    
    // Safe area constraints
    val safeAreaMarginMultiplier: Float = 1.5f, // Extra margin beyond viewfinder safe area
    val respectSystemBars: Boolean = true,
    val avoidHotspots: Boolean = true // Avoid commonly touched areas
)

/**
 * Position variation state for dynamic positioning
 */
data class DynamicPosition(
    val baseOffset: Offset,
    val currentOffset: Offset,
    val targetOffset: Offset,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val animationProgress: Float = 0f
)

/**
 * Element timing state for timeout management
 */
data class ElementTiming(
    val creationTime: Long,
    val lastInteractionTime: Long,
    val isVisible: Boolean,
    val fadeState: FadeState = FadeState.VISIBLE
)

enum class FadeState {
    VISIBLE,
    FADING_OUT,
    HIDDEN,
    FADING_IN
}

/**
 * Main burnin prevention system class
 */
class BurninPreventionSystem(
    private val config: BurninPreventionConfig = BurninPreventionConfig()
) {
    
    private val _dynamicPositions = MutableStateFlow<Map<String, DynamicPosition>>(emptyMap())
    val dynamicPositions: StateFlow<Map<String, DynamicPosition>> = _dynamicPositions.asStateFlow()
    
    private val _elementTimings = MutableStateFlow<Map<String, ElementTiming>>(emptyMap())
    val elementTimings: StateFlow<Map<String, ElementTiming>> = _elementTimings.asStateFlow()
    
    private val _safeAreaHotspots = MutableStateFlow<Set<Offset>>(emptySet())
    val safeAreaHotspots: StateFlow<Set<Offset>> = _safeAreaHotspots.asStateFlow()
    
    /**
     * Register an overlay element for burnin prevention
     */
    fun registerElement(
        elementId: String,
        basePosition: Offset,
        size: Size,
        bounds: UnifiedViewfinderCalculator.ViewfinderBounds
    ) {
        val currentTime = System.currentTimeMillis()
        
        // Calculate safe positioning within bounds
        val safePosition = calculateSafePosition(basePosition, size, bounds)
        
        // Register dynamic position
        val dynamicPosition = DynamicPosition(
            baseOffset = basePosition,
            currentOffset = safePosition,
            targetOffset = safePosition
        )
        
        _dynamicPositions.value = _dynamicPositions.value + (elementId to dynamicPosition)
        
        // Register element timing
        val elementTiming = ElementTiming(
            creationTime = currentTime,
            lastInteractionTime = currentTime,
            isVisible = true
        )
        
        _elementTimings.value = _elementTimings.value + (elementId to elementTiming)
    }
    
    /**
     * Update element position with burnin prevention
     */
    fun updateElementPosition(
        elementId: String,
        bounds: UnifiedViewfinderCalculator.ViewfinderBounds,
        forceUpdate: Boolean = false
    ): Offset? {
        val currentPosition = _dynamicPositions.value[elementId] ?: return null
        val currentTime = System.currentTimeMillis()
        
        // Check if position update is needed
        val timeSinceUpdate = currentTime - currentPosition.lastUpdateTime
        val needsUpdate = forceUpdate || 
            (config.enableDynamicPositioning && timeSinceUpdate >= config.positionUpdateIntervalMs)
        
        if (!needsUpdate) {
            return currentPosition.currentOffset
        }
        
        // Generate new safe target position
        val newTargetPosition = if (config.positionVariationEnabled) {
            generateVariedPosition(currentPosition.baseOffset, bounds)
        } else {
            currentPosition.baseOffset
        }
        
        val updatedPosition = currentPosition.copy(
            targetOffset = newTargetPosition,
            lastUpdateTime = currentTime,
            animationProgress = 0f
        )
        
        _dynamicPositions.value = _dynamicPositions.value + (elementId to updatedPosition)
        
        return updatedPosition.targetOffset
    }
    
    /**
     * Get animated position with smooth transitions
     */
    @Composable
    fun getAnimatedPosition(elementId: String): Offset? {
        val position = dynamicPositions.collectAsState().value[elementId] ?: return null
        
        val animatedX by animateFloatAsState(
            targetValue = position.targetOffset.x,
            animationSpec = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            ),
            label = "positionX"
        )
        
        val animatedY by animateFloatAsState(
            targetValue = position.targetOffset.y,
            animationSpec = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            ),
            label = "positionY"
        )
        
        return Offset(animatedX, animatedY)
    }
    
    /**
     * Get dynamic alpha value to prevent pixel persistence
     */
    @Composable
    fun getDynamicAlpha(elementId: String): Float {
        val timing = elementTimings.collectAsState().value[elementId]
        
        if (!config.useTransparencyGradients) {
            return config.maximumAlpha
        }
        
        // Create subtle alpha variation to prevent burnin
        val time = remember { System.currentTimeMillis() }
        val alphaVariation by rememberInfiniteTransition(label = "alphaVariation").animateFloat(
            initialValue = config.minimumAlpha,
            targetValue = config.maximumAlpha,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 4000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        
        return when (timing?.fadeState) {
            FadeState.FADING_OUT -> {
                val fadeProgress by animateFloatAsState(
                    targetValue = 0f,
                    animationSpec = tween(config.fadeOutDurationMs.toInt()),
                    label = "fadeOut"
                )
                alphaVariation * (1f - fadeProgress)
            }
            FadeState.HIDDEN -> 0f
            FadeState.FADING_IN -> {
                val fadeProgress by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(config.fadeInDurationMs.toInt()),
                    label = "fadeIn"
                )
                alphaVariation * fadeProgress
            }
            else -> alphaVariation
        }
    }
    
    /**
     * Get dynamic color with subtle variations
     */
    @Composable
    fun getDynamicColor(baseColor: Color, elementId: String): Color {
        val alpha = getDynamicAlpha(elementId)
        
        if (!config.enableSubtleAnimations) {
            return baseColor.copy(alpha = alpha)
        }
        
        // Subtle color variation to prevent static patterns
        val hueShift by rememberInfiniteTransition(label = "colorVariation").animateFloat(
            initialValue = -0.02f,
            targetValue = 0.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 8000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "hue"
        )
        
        return baseColor.copy(alpha = alpha).adjustHue(hueShift)
    }
    
    /**
     * Check element timeout and manage visibility
     */
    suspend fun manageElementTimeouts() {
        if (!config.enableTimeoutManagement) return
        
        while (true) {
            delay(10_000L) // Check every 10 seconds
            
            val currentTime = System.currentTimeMillis()
            val updatedTimings = _elementTimings.value.mapValues { (_, timing) ->
                val timeSinceInteraction = currentTime - timing.lastInteractionTime
                
                when {
                    timeSinceInteraction > config.staticElementTimeoutMs && timing.isVisible -> {
                        // Start fade out
                        timing.copy(fadeState = FadeState.FADING_OUT)
                    }
                    timing.fadeState == FadeState.FADING_OUT -> {
                        // Check if fade out complete
                        timing.copy(
                            isVisible = false,
                            fadeState = FadeState.HIDDEN
                        )
                    }
                    !timing.isVisible && timeSinceInteraction < config.staticElementTimeoutMs -> {
                        // User interaction detected, fade back in
                        timing.copy(fadeState = FadeState.FADING_IN)
                    }
                    timing.fadeState == FadeState.FADING_IN -> {
                        // Fade in complete
                        timing.copy(
                            isVisible = true,
                            fadeState = FadeState.VISIBLE
                        )
                    }
                    else -> timing
                }
            }
            
            _elementTimings.value = updatedTimings
        }
    }
    
    /**
     * Record user interaction to prevent unnecessary timeouts
     */
    fun recordUserInteraction(elementId: String) {
        val currentTime = System.currentTimeMillis()
        val currentTiming = _elementTimings.value[elementId] ?: return
        
        val updatedTiming = currentTiming.copy(
            lastInteractionTime = currentTime,
            fadeState = if (currentTiming.fadeState == FadeState.HIDDEN || 
                         currentTiming.fadeState == FadeState.FADING_OUT) {
                FadeState.FADING_IN
            } else {
                FadeState.VISIBLE
            }
        )
        
        _elementTimings.value = _elementTimings.value + (elementId to updatedTiming)
    }
    
    /**
     * Add hotspot to avoid (commonly touched areas)
     */
    fun addHotspot(position: Offset) {
        if (config.avoidHotspots) {
            _safeAreaHotspots.value = _safeAreaHotspots.value + position
        }
    }
    
    /**
     * Calculate safe position within bounds and away from hotspots
     */
    private fun calculateSafePosition(
        desiredPosition: Offset,
        elementSize: Size,
        bounds: UnifiedViewfinderCalculator.ViewfinderBounds
    ): Offset {
        val safeMargin = config.safeAreaMarginMultiplier * 16f
        
        val minX = bounds.safeAreaLeft + safeMargin
        val maxX = bounds.safeAreaRight - safeMargin - elementSize.width
        val minY = bounds.safeAreaTop + safeMargin
        val maxY = bounds.safeAreaBottom - safeMargin - elementSize.height
        
        var safeX = desiredPosition.x.coerceIn(minX, maxX)
        var safeY = desiredPosition.y.coerceIn(minY, maxY)
        
        // Avoid hotspots if enabled
        if (config.avoidHotspots) {
            val hotspots = _safeAreaHotspots.value
            val minHotspotDistance = 60f // Minimum distance from hotspots
            
            for (hotspot in hotspots) {
                val distance = sqrt((safeX - hotspot.x).pow(2) + (safeY - hotspot.y).pow(2))
                if (distance < minHotspotDistance) {
                    // Move away from hotspot
                    val angle = atan2(safeY - hotspot.y, safeX - hotspot.x)
                    safeX = hotspot.x + cos(angle) * minHotspotDistance
                    safeY = hotspot.y + sin(angle) * minHotspotDistance
                    
                    // Ensure still within bounds
                    safeX = safeX.coerceIn(minX, maxX)
                    safeY = safeY.coerceIn(minY, maxY)
                }
            }
        }
        
        return Offset(safeX, safeY)
    }
    
    /**
     * Generate varied position for dynamic positioning
     */
    private fun generateVariedPosition(
        basePosition: Offset,
        bounds: UnifiedViewfinderCalculator.ViewfinderBounds
    ): Offset {
        val maxOffset = config.maxPositionOffset
        
        val offsetX = Random.nextFloat() * 2 * maxOffset - maxOffset
        val offsetY = Random.nextFloat() * 2 * maxOffset - maxOffset
        
        val newPosition = Offset(
            basePosition.x + offsetX,
            basePosition.y + offsetY
        )
        
        // Ensure within safe area
        val safeMargin = config.safeAreaMarginMultiplier * 16f
        return Offset(
            newPosition.x.coerceIn(
                bounds.safeAreaLeft + safeMargin,
                bounds.safeAreaRight - safeMargin
            ),
            newPosition.y.coerceIn(
                bounds.safeAreaTop + safeMargin,
                bounds.safeAreaBottom - safeMargin
            )
        )
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        _dynamicPositions.value = emptyMap()
        _elementTimings.value = emptyMap()
        _safeAreaHotspots.value = emptySet()
    }
}

/**
 * Helper function to adjust color hue
 */
private fun Color.adjustHue(shift: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[0] = (hsv[0] + shift * 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * Composable function for easy burnin prevention integration
 */
@Composable
fun rememberBurninPreventionSystem(
    config: BurninPreventionConfig = BurninPreventionConfig()
): BurninPreventionSystem {
    val system = remember { BurninPreventionSystem(config) }
    
    // Automatically manage timeouts
    LaunchedEffect(system) {
        system.manageElementTimeouts()
    }
    
    DisposableEffect(system) {
        onDispose {
            system.cleanup()
        }
    }
    
    return system
}