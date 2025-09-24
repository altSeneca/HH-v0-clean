package com.hazardhawk.ar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hazardhawk.ai.models.*
import com.hazardhawk.core.models.SafetyAnalysis
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for AR overlay renderer focusing on coordinate transformation and performance.
 * Tests rendering calculations, viewport transformations, and overlay positioning.
 */
@RunWith(AndroidJUnit4::class)
class AROverlayRendererTest {

    private lateinit var overlayRenderer: AROverlayRenderer
    private val testCanvasSize = Size(1080f, 1920f) // Full HD portrait

    @Before
    fun setUp() {
        overlayRenderer = AROverlayRenderer()
    }

    @Test
    fun arOverlayRenderer_transformsNormalizedCoordinates() {
        // Given
        val normalizedBox = BoundingBox(
            left = 0.25f,    // 25% from left
            top = 0.1f,      // 10% from top
            width = 0.3f,    // 30% width
            height = 0.2f    // 20% height
        )
        
        // When
        val screenBox = overlayRenderer.transformToScreenCoordinates(normalizedBox, testCanvasSize)
        
        // Then
        assertEquals(270f, screenBox.left, 0.1f)      // 25% of 1080
        assertEquals(192f, screenBox.top, 0.1f)       // 10% of 1920
        assertEquals(324f, screenBox.width, 0.1f)     // 30% of 1080
        assertEquals(384f, screenBox.height, 0.1f)    // 20% of 1920
    }

    @Test
    fun arOverlayRenderer_handlesEdgeCaseCoordinates() {
        // Given
        val edgeBox = BoundingBox(
            left = 0f,
            top = 0f,
            width = 1f,
            height = 1f
        )
        
        // When
        val screenBox = overlayRenderer.transformToScreenCoordinates(edgeBox, testCanvasSize)
        
        // Then
        assertEquals(0f, screenBox.left)
        assertEquals(0f, screenBox.top)
        assertEquals(testCanvasSize.width, screenBox.width)
        assertEquals(testCanvasSize.height, screenBox.height)
    }

    @Test
    fun arOverlayRenderer_clampsOutOfBoundsCoordinates() {
        // Given
        val outOfBoundsBox = BoundingBox(
            left = -0.1f,    // Negative left
            top = 1.5f,      // Beyond bottom
            width = 1.2f,    // Too wide
            height = 0.5f
        )
        
        // When
        val screenBox = overlayRenderer.transformToScreenCoordinates(outOfBoundsBox, testCanvasSize)
        
        // Then
        assertTrue(screenBox.left >= 0f)
        assertTrue(screenBox.top >= 0f)
        assertTrue(screenBox.left + screenBox.width <= testCanvasSize.width)
        assertTrue(screenBox.top + screenBox.height <= testCanvasSize.height)
    }

    @Test
    fun arOverlayRenderer_calculatesOptimalBadgePosition() {
        // Given
        val hazardBox = BoundingBox(0.4f, 0.3f, 0.2f, 0.15f)
        val existingBadges = listOf(
            BadgePosition(Offset(400f, 500f), Size(120f, 40f)),
            BadgePosition(Offset(600f, 300f), Size(120f, 40f))
        )
        
        // When
        val badgePosition = overlayRenderer.calculateOptimalBadgePosition(
            hazardBox, testCanvasSize, existingBadges
        )
        
        // Then
        // Badge should not overlap with existing badges
        existingBadges.forEach { existing ->
            assertFalse(overlapsWithBadge(badgePosition, existing))
        }
        
        // Badge should be positioned near the hazard
        val hazardCenterX = (hazardBox.left + hazardBox.width / 2) * testCanvasSize.width
        val hazardCenterY = (hazardBox.top + hazardBox.height / 2) * testCanvasSize.height
        val badgeCenterX = badgePosition.offset.x + badgePosition.size.width / 2
        val badgeCenterY = badgePosition.offset.y + badgePosition.size.height / 2
        
        val distance = kotlin.math.sqrt(
            (hazardCenterX - badgeCenterX) * (hazardCenterX - badgeCenterX) +
            (hazardCenterY - badgeCenterY) * (hazardCenterY - badgeCenterY)
        )
        
        assertTrue(distance < 200f) // Badge should be within 200px of hazard center
    }

    @Test
    fun arOverlayRenderer_prioritizesCriticalHazards() {
        // Given
        val hazards = listOf(
            createHazard(Severity.MEDIUM, BoundingBox(0.1f, 0.1f, 0.2f, 0.2f)),
            createHazard(Severity.CRITICAL, BoundingBox(0.3f, 0.3f, 0.2f, 0.2f)),
            createHazard(Severity.HIGH, BoundingBox(0.5f, 0.5f, 0.2f, 0.2f))
        )
        
        // When
        val renderOrder = overlayRenderer.calculateRenderOrder(hazards)
        
        // Then
        assertEquals(Severity.CRITICAL, renderOrder[0].severity) // Critical first
        assertEquals(Severity.HIGH, renderOrder[1].severity)     // High second
        assertEquals(Severity.MEDIUM, renderOrder[2].severity)   // Medium last
    }

    @Test
    fun arOverlayRenderer_handlesOverlappingHazards() = runTest {
        // Given
        val overlappingHazards = listOf(
            createHazard(Severity.HIGH, BoundingBox(0.2f, 0.2f, 0.3f, 0.3f)),
            createHazard(Severity.MEDIUM, BoundingBox(0.25f, 0.25f, 0.2f, 0.2f)), // Overlaps
            createHazard(Severity.CRITICAL, BoundingBox(0.3f, 0.3f, 0.15f, 0.15f)) // Overlaps both
        )
        
        // When
        val renderInstructions = overlayRenderer.generateRenderInstructions(
            overlappingHazards, testCanvasSize
        )
        
        // Then
        // Critical hazard should be rendered last (on top)
        val lastInstruction = renderInstructions.last()
        assertEquals(Severity.CRITICAL, lastInstruction.hazard.severity)
        
        // All hazards should have non-overlapping badge positions
        val badgePositions = renderInstructions.map { it.badgePosition }
        for (i in badgePositions.indices) {
            for (j in i + 1 until badgePositions.size) {
                assertFalse(overlapsWithBadge(badgePositions[i], badgePositions[j]))
            }
        }
    }

    @Test
    fun arOverlayRenderer_optimizesForPerformance() = runTest {
        // Given
        val manyHazards = (1..50).map { index ->
            createHazard(
                Severity.values()[index % Severity.values().size],
                BoundingBox(
                    (index % 10) * 0.1f,
                    (index / 10) * 0.1f,
                    0.08f,
                    0.08f
                )
            )
        }
        
        // When
        val startTime = System.currentTimeMillis()
        val renderInstructions = overlayRenderer.generateRenderInstructions(manyHazards, testCanvasSize)
        val processingTime = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue(processingTime < 16) // Should complete within one frame (16ms for 60fps)
        assertTrue(renderInstructions.size <= 20) // Should limit visible overlays for performance
        
        // Critical hazards should still be included
        val criticalCount = renderInstructions.count { it.hazard.severity == Severity.CRITICAL }
        val originalCriticalCount = manyHazards.count { it.severity == Severity.CRITICAL }
        assertEquals(originalCriticalCount, criticalCount)
    }

    @Test
    fun arOverlayRenderer_handlesViewportChanges() {
        // Given
        val hazard = createHazard(Severity.HIGH, BoundingBox(0.5f, 0.5f, 0.2f, 0.2f))
        val originalSize = Size(1080f, 1920f)
        val newSize = Size(1920f, 1080f) // Landscape rotation
        
        // When
        val originalPosition = overlayRenderer.transformToScreenCoordinates(hazard.boundingBox!!, originalSize)
        val newPosition = overlayRenderer.transformToScreenCoordinates(hazard.boundingBox!!, newSize)
        
        // Then
        // Relative position should be maintained
        val originalRelativeX = originalPosition.left / originalSize.width
        val newRelativeX = newPosition.left / newSize.width
        assertEquals(originalRelativeX, newRelativeX, 0.01f)
        
        val originalRelativeY = originalPosition.top / originalSize.height
        val newRelativeY = newPosition.top / newSize.height
        assertEquals(originalRelativeY, newRelativeY, 0.01f)
    }

    @Test
    fun arOverlayRenderer_calculatesVisibilityCorrectly() {
        // Given
        val viewportBounds = ViewportBounds(
            left = 0f,
            top = 0f,
            right = testCanvasSize.width,
            bottom = testCanvasSize.height
        )
        
        val visibleHazard = createHazard(Severity.HIGH, BoundingBox(0.3f, 0.3f, 0.2f, 0.2f))
        val partiallyVisibleHazard = createHazard(Severity.MEDIUM, BoundingBox(0.9f, 0.9f, 0.2f, 0.2f))
        val invisibleHazard = createHazard(Severity.LOW, BoundingBox(1.5f, 1.5f, 0.2f, 0.2f))
        
        // When
        val visibleResult = overlayRenderer.isHazardVisible(visibleHazard, viewportBounds, testCanvasSize)
        val partialResult = overlayRenderer.isHazardVisible(partiallyVisibleHazard, viewportBounds, testCanvasSize)
        val invisibleResult = overlayRenderer.isHazardVisible(invisibleHazard, viewportBounds, testCanvasSize)
        
        // Then
        assertTrue(visibleResult.isVisible)
        assertEquals(1.0f, visibleResult.visibilityRatio, 0.01f)
        
        assertTrue(partialResult.isVisible)
        assertTrue(partialResult.visibilityRatio < 1.0f)
        assertTrue(partialResult.visibilityRatio > 0.0f)
        
        assertFalse(invisibleResult.isVisible)
        assertEquals(0.0f, invisibleResult.visibilityRatio)
    }

    @Test
    fun arOverlayRenderer_appliesDistanceFading() {
        // Given
        val closeHazard = createHazard(Severity.HIGH, BoundingBox(0.3f, 0.3f, 0.4f, 0.4f)) // Large = close
        val farHazard = createHazard(Severity.HIGH, BoundingBox(0.3f, 0.3f, 0.05f, 0.05f)) // Small = far
        
        // When
        val closeAlpha = overlayRenderer.calculateDistanceAlpha(closeHazard, testCanvasSize)
        val farAlpha = overlayRenderer.calculateDistanceAlpha(farHazard, testCanvasSize)
        
        // Then
        assertTrue(closeAlpha > farAlpha) // Closer objects should be more opaque
        assertTrue(closeAlpha >= 0.8f)   // Close objects should be mostly opaque
        assertTrue(farAlpha >= 0.3f)     // Far objects should still be visible
    }

    @Test
    fun arOverlayRenderer_generatesAnimationInstructions() {
        // Given
        val criticalHazard = createHazard(Severity.CRITICAL, BoundingBox(0.3f, 0.3f, 0.2f, 0.2f))
        
        // When
        val animationInstructions = overlayRenderer.generateAnimationInstructions(criticalHazard)
        
        // Then
        assertTrue(animationInstructions.shouldPulse)
        assertEquals(1000L, animationInstructions.pulseDuration)
        assertTrue(animationInstructions.shouldHighlight)
        assertEquals(0.2f, animationInstructions.highlightIntensity, 0.01f)
    }

    // Helper methods
    private fun createHazard(severity: Severity, boundingBox: BoundingBox): Hazard {
        return Hazard(
            id = "test-hazard-${severity.name}",
            type = HazardType.FALL_PROTECTION,
            severity = severity,
            description = "Test hazard",
            oshaCode = "TEST.001",
            boundingBox = boundingBox,
            confidence = 0.85f,
            recommendations = listOf("Test recommendation")
        )
    }

    private fun overlapsWithBadge(position1: BadgePosition, position2: BadgePosition): Boolean {
        val right1 = position1.offset.x + position1.size.width
        val bottom1 = position1.offset.y + position1.size.height
        val right2 = position2.offset.x + position2.size.width
        val bottom2 = position2.offset.y + position2.size.height

        return !(position1.offset.x >= right2 || position2.offset.x >= right1 ||
                position1.offset.y >= bottom2 || position2.offset.y >= bottom1)
    }
}

/**
 * Mock AR Overlay Renderer for testing
 */
class AROverlayRenderer {
    
    fun transformToScreenCoordinates(boundingBox: BoundingBox, canvasSize: Size): ScreenBoundingBox {
        val clampedBox = clampBoundingBox(boundingBox)
        
        return ScreenBoundingBox(
            left = clampedBox.left * canvasSize.width,
            top = clampedBox.top * canvasSize.height,
            width = clampedBox.width * canvasSize.width,
            height = clampedBox.height * canvasSize.height
        )
    }
    
    private fun clampBoundingBox(box: BoundingBox): BoundingBox {
        val clampedLeft = box.left.coerceIn(0f, 1f)
        val clampedTop = box.top.coerceIn(0f, 1f)
        val clampedWidth = (box.width).coerceIn(0f, 1f - clampedLeft)
        val clampedHeight = (box.height).coerceIn(0f, 1f - clampedTop)
        
        return BoundingBox(clampedLeft, clampedTop, clampedWidth, clampedHeight)
    }
    
    fun calculateOptimalBadgePosition(
        hazardBox: BoundingBox,
        canvasSize: Size,
        existingBadges: List<BadgePosition>
    ): BadgePosition {
        val badgeSize = Size(120f, 40f)
        val screenBox = transformToScreenCoordinates(hazardBox, canvasSize)
        
        // Try positions around the hazard
        val candidates = listOf(
            Offset(screenBox.left - badgeSize.width - 10f, screenBox.top),              // Left
            Offset(screenBox.left + screenBox.width + 10f, screenBox.top),             // Right
            Offset(screenBox.left, screenBox.top - badgeSize.height - 10f),            // Above
            Offset(screenBox.left, screenBox.top + screenBox.height + 10f)             // Below
        )
        
        // Find first position that doesn't overlap with existing badges
        val position = candidates.firstOrNull { candidateOffset ->
            val candidateBadge = BadgePosition(candidateOffset, badgeSize)
            existingBadges.none { existing -> overlapsWithBadge(candidateBadge, existing) } &&
            isWithinBounds(candidateBadge, canvasSize)
        } ?: candidates.first() // Fallback to first position
        
        return BadgePosition(position, badgeSize)
    }
    
    private fun isWithinBounds(badge: BadgePosition, canvasSize: Size): Boolean {
        return badge.offset.x >= 0f &&
               badge.offset.y >= 0f &&
               badge.offset.x + badge.size.width <= canvasSize.width &&
               badge.offset.y + badge.size.height <= canvasSize.height
    }
    
    fun calculateRenderOrder(hazards: List<Hazard>): List<Hazard> {
        return hazards.sortedBy { hazard ->
            when (hazard.severity) {
                Severity.CRITICAL -> 0
                Severity.HIGH -> 1
                Severity.MEDIUM -> 2
                Severity.LOW -> 3
            }
        }
    }
    
    suspend fun generateRenderInstructions(
        hazards: List<Hazard>,
        canvasSize: Size
    ): List<RenderInstruction> {
        val orderedHazards = calculateRenderOrder(hazards)
        val instructions = mutableListOf<RenderInstruction>()
        val usedBadgePositions = mutableListOf<BadgePosition>()
        
        // Limit to 20 overlays for performance
        val visibleHazards = orderedHazards.take(20)
        
        visibleHazards.forEach { hazard ->
            hazard.boundingBox?.let { boundingBox ->
                val badgePosition = calculateOptimalBadgePosition(
                    boundingBox, canvasSize, usedBadgePositions
                )
                usedBadgePositions.add(badgePosition)
                
                instructions.add(
                    RenderInstruction(
                        hazard = hazard,
                        screenBoundingBox = transformToScreenCoordinates(boundingBox, canvasSize),
                        badgePosition = badgePosition,
                        alpha = calculateDistanceAlpha(hazard, canvasSize),
                        animationInstructions = generateAnimationInstructions(hazard)
                    )
                )
            }
        }
        
        return instructions
    }
    
    fun isHazardVisible(
        hazard: Hazard,
        viewportBounds: ViewportBounds,
        canvasSize: Size
    ): VisibilityResult {
        hazard.boundingBox?.let { boundingBox ->
            val screenBox = transformToScreenCoordinates(boundingBox, canvasSize)
            
            val intersectionLeft = maxOf(screenBox.left, viewportBounds.left)
            val intersectionTop = maxOf(screenBox.top, viewportBounds.top)
            val intersectionRight = minOf(screenBox.left + screenBox.width, viewportBounds.right)
            val intersectionBottom = minOf(screenBox.top + screenBox.height, viewportBounds.bottom)
            
            if (intersectionLeft < intersectionRight && intersectionTop < intersectionBottom) {
                val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
                val totalArea = screenBox.width * screenBox.height
                val visibilityRatio = if (totalArea > 0) intersectionArea / totalArea else 0f
                
                return VisibilityResult(true, visibilityRatio)
            }
        }
        
        return VisibilityResult(false, 0f)
    }
    
    fun calculateDistanceAlpha(hazard: Hazard, canvasSize: Size): Float {
        hazard.boundingBox?.let { boundingBox ->
            val area = boundingBox.width * boundingBox.height
            // Larger areas indicate closer objects
            return (0.3f + (area * 2f)).coerceIn(0.3f, 1.0f)
        }
        return 0.8f
    }
    
    fun generateAnimationInstructions(hazard: Hazard): AnimationInstructions {
        return when (hazard.severity) {
            Severity.CRITICAL -> AnimationInstructions(
                shouldPulse = true,
                pulseDuration = 1000L,
                shouldHighlight = true,
                highlightIntensity = 0.2f
            )
            Severity.HIGH -> AnimationInstructions(
                shouldPulse = false,
                shouldHighlight = true,
                highlightIntensity = 0.1f
            )
            else -> AnimationInstructions()
        }
    }
    
    private fun overlapsWithBadge(position1: BadgePosition, position2: BadgePosition): Boolean {
        val right1 = position1.offset.x + position1.size.width
        val bottom1 = position1.offset.y + position1.size.height
        val right2 = position2.offset.x + position2.size.width
        val bottom2 = position2.offset.y + position2.size.height

        return !(position1.offset.x >= right2 || position2.offset.x >= right1 ||
                position1.offset.y >= bottom2 || position2.offset.y >= bottom1)
    }
}

// Data classes for testing
data class ScreenBoundingBox(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

data class BadgePosition(
    val offset: Offset,
    val size: Size
)

data class RenderInstruction(
    val hazard: Hazard,
    val screenBoundingBox: ScreenBoundingBox,
    val badgePosition: BadgePosition,
    val alpha: Float,
    val animationInstructions: AnimationInstructions
)

data class ViewportBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class VisibilityResult(
    val isVisible: Boolean,
    val visibilityRatio: Float
)

data class AnimationInstructions(
    val shouldPulse: Boolean = false,
    val pulseDuration: Long = 0L,
    val shouldHighlight: Boolean = false,
    val highlightIntensity: Float = 0f
)
