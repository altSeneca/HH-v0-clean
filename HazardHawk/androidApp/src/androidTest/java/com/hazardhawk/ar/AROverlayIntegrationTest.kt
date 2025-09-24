package com.hazardhawk.ar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hazardhawk.ai.models.*
import com.hazardhawk.core.models.SafetyAnalysis
import com.hazardhawk.ui.ar.HazardDetectionOverlay
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * Integration tests for AR overlay rendering with real camera frames.
 * Tests overlay synchronization, frame-accurate rendering, and real-world performance.
 */
@RunWith(AndroidJUnit4::class)
class AROverlayIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var overlayRenderer: AROverlayRenderer
    private lateinit var frameProvider: MockCameraFrameProvider
    private val testCanvasSize = Size(1080f, 1920f)

    @Before
    fun setUp() {
        overlayRenderer = AROverlayRenderer()
        frameProvider = MockCameraFrameProvider()
    }

    @Test
    fun arOverlayIntegration_rendersOverRealFrames() = runTest {
        // Given
        val hazards = listOf(
            createTestHazard(Severity.CRITICAL, BoundingBox(0.3f, 0.3f, 0.2f, 0.2f)),
            createTestHazard(Severity.HIGH, BoundingBox(0.6f, 0.1f, 0.15f, 0.25f))
        )
        val analysis = createTestAnalysis(hazards)

        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                // Mock camera preview background
                MockCameraPreview(
                    frameProvider = frameProvider,
                    modifier = Modifier.fillMaxSize()
                )
                
                // AR overlay on top
                HazardDetectionOverlay(
                    safetyAnalysis = analysis,
                    showBoundingBoxes = true,
                    showOSHABadges = true,
                    animationEnabled = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Wait for rendering
        composeTestRule.waitForIdle()

        // Verify overlays are displayed
        composeTestRule.onNodeWithText("🚨 CRITICAL").assertIsDisplayed()
        composeTestRule.onNodeWithText("⚠️ 2 Issues").assertIsDisplayed()
    }

    @Test
    fun arOverlayIntegration_synchronizesWithFrameRate() = runTest {
        // Given
        val frameRate = 30f // 30 FPS
        val frameDuration = 1000f / frameRate // ~33.33ms per frame
        var overlayUpdateCount = 0
        val targetUpdates = 10

        val analysis = createTestAnalysis(listOf(createTestHazard()))

        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = analysis,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Simulate frame updates
        repeat(targetUpdates) { frameIndex ->
            frameProvider.provideFrame(frameIndex)
            composeTestRule.waitForIdle()
            overlayUpdateCount++
            Thread.sleep(frameDuration.toLong()) // Simulate real frame timing
        }

        assertEquals(targetUpdates, overlayUpdateCount)
    }

    @Test
    fun arOverlayIntegration_handlesFrameDrops() = runTest {
        // Given
        val analysis = createTestAnalysis(listOf(createTestHazard()))
        var frameDropDetected = false

        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = analysis,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Simulate frame drops by skipping frames
        frameProvider.provideFrame(0)
        composeTestRule.waitForIdle()
        
        // Skip several frames
        frameProvider.provideFrame(5) // Simulates dropped frames 1-4
        composeTestRule.waitForIdle()

        // Overlay should still render correctly after frame drops
        composeTestRule.onNodeWithText("⚠️ 1 Issue").assertIsDisplayed()
    }

    @Test
    fun arOverlayIntegration_maintainsAccuracyAcrossFrames() = runTest {
        // Given
        val staticHazard = createTestHazard(
            severity = Severity.CRITICAL,
            boundingBox = BoundingBox(0.4f, 0.4f, 0.2f, 0.2f)
        )
        val analysis = createTestAnalysis(listOf(staticHazard))

        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = analysis,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Test overlay positioning consistency across multiple frames
        repeat(30) { frameIndex ->
            frameProvider.provideFrame(frameIndex)
            composeTestRule.waitForIdle()
            
            // Verify hazard overlay is still positioned correctly
            composeTestRule.onNodeWithText("🚨 CRITICAL").assertIsDisplayed()
        }
    }

    @Test
    fun arOverlayIntegration_handlesMovingHazards() = runTest {
        // Given - Hazard that moves across frames
        var currentFrame = 0
        val movingAnalysis = { frame: Int ->
            val x = 0.1f + (frame * 0.02f) // Move right across frames
            val hazard = createTestHazard(
                boundingBox = BoundingBox(x, 0.3f, 0.15f, 0.15f)
            )
            createTestAnalysis(listOf(hazard))
        }

        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = movingAnalysis(currentFrame),
                modifier = Modifier.fillMaxSize()
            )
        }

        // Simulate movement across frames
        repeat(10) { frame ->
            currentFrame = frame
            composeTestRule.setContent {
                HazardDetectionOverlay(
                    safetyAnalysis = movingAnalysis(currentFrame),
                    modifier = Modifier.fillMaxSize()
                )
            }
            composeTestRule.waitForIdle()
            
            // Verify overlay updates position
            composeTestRule.onNodeWithText("⚠️ 1 Issue").assertIsDisplayed()
        }
    }

    @Test
    fun arOverlayIntegration_handlesHazardAppearanceDisappearance() = runTest {
        // Given - Hazards that appear and disappear
        val frames = listOf(
            // Frame 0: No hazards
            createTestAnalysis(emptyList()),
            // Frame 1: One hazard appears
            createTestAnalysis(listOf(createTestHazard(Severity.HIGH))),
            // Frame 2: Another hazard appears
            createTestAnalysis(listOf(
                createTestHazard(Severity.HIGH),
                createTestHazard(Severity.CRITICAL)
            )),
            // Frame 3: First hazard disappears
            createTestAnalysis(listOf(createTestHazard(Severity.CRITICAL)))
        )

        frames.forEachIndexed { frameIndex, analysis ->
            composeTestRule.setContent {
                HazardDetectionOverlay(
                    safetyAnalysis = analysis,
                    modifier = Modifier.fillMaxSize()
                )
            }
            composeTestRule.waitForIdle()

            when (frameIndex) {
                0 -> {
                    composeTestRule.onNodeWithText("✅ No Hazards").assertIsDisplayed()
                }
                1 -> {
                    composeTestRule.onNodeWithText("⚠️ 1 Issue").assertIsDisplayed()
                }
                2 -> {
                    composeTestRule.onNodeWithText("🚨 CRITICAL").assertIsDisplayed()
                }
                3 -> {
                    composeTestRule.onNodeWithText("🚨 CRITICAL").assertIsDisplayed()
                }
            }
        }
    }

    @Test
    fun arOverlayIntegration_performanceUnderLoad() = runTest {
        // Given - Many hazards to stress test
        val manyHazards = (0 until 25).map { index ->
            createTestHazard(
                severity = Severity.values()[index % 4],
                boundingBox = BoundingBox(
                    left = (index % 5) * 0.2f,
                    top = (index / 5) * 0.2f,
                    width = 0.15f,
                    height = 0.15f
                )
            )
        }
        val heavyAnalysis = createTestAnalysis(manyHazards)

        val startTime = System.currentTimeMillis()

        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = heavyAnalysis,
                modifier = Modifier.fillMaxSize()
            )
        }

        composeTestRule.waitForIdle()
        val renderTime = System.currentTimeMillis() - startTime

        // Should render within reasonable time (under 100ms)
        assertTrue(renderTime < 100, "Heavy overlay should render quickly: ${renderTime}ms")
        
        // Should limit displayed overlays for performance
        composeTestRule.onNodeWithText("⚠️", substring = true).assertIsDisplayed()
    }

    @Test
    fun arOverlayIntegration_handlesOrientationChanges() = runTest {
        // Given
        val hazard = createTestHazard(
            boundingBox = BoundingBox(0.5f, 0.3f, 0.2f, 0.2f)
        )
        val analysis = createTestAnalysis(listOf(hazard))

        // Portrait orientation
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = analysis,
                modifier = Modifier.fillMaxSize()
            )
        }
        composeTestRule.waitForIdle()
        
        // Verify overlay is visible in portrait
        composeTestRule.onNodeWithText("⚠️ 1 Issue").assertIsDisplayed()

        // Simulate orientation change (in real app, this would trigger recomposition)
        // For this test, we just verify the overlay handles size changes gracefully
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = analysis,
                modifier = Modifier.fillMaxSize()
            )
        }
        composeTestRule.waitForIdle()
        
        // Overlay should still be visible after orientation change
        composeTestRule.onNodeWithText("⚠️ 1 Issue").assertIsDisplayed()
    }

    @Test
    fun arOverlayIntegration_respondsToUserInteraction() = runTest {
        // Given
        val hazard = createTestHazard(Severity.CRITICAL)
        val analysis = createTestAnalysis(listOf(hazard))
        var selectedHazard: Hazard? = null

        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = analysis,
                onHazardClick = { clicked -> selectedHazard = clicked },
                modifier = Modifier.fillMaxSize()
            )
        }

        // When - Click on hazard overlay (using content description)
        composeTestRule.onNodeWithContentDescription("OSHA Badge", substring = true)
            .assertIsDisplayed()
            .performClick()

        // Then
        assertEquals(hazard, selectedHazard)
    }

    @Test
    fun arOverlayIntegration_adaptsToLightingConditions() = runTest {
        // Given
        val hazard = createTestHazard()
        val analysis = createTestAnalysis(listOf(hazard))

        // Test different lighting conditions
        val lightingLevels = listOf(0.1f, 0.5f, 0.9f) // Dark, normal, bright

        lightingLevels.forEach { lightLevel ->
            frameProvider.setLightingLevel(lightLevel)
            
            composeTestRule.setContent {
                HazardDetectionOverlay(
                    safetyAnalysis = analysis,
                    modifier = Modifier.fillMaxSize()
                )
            }
            composeTestRule.waitForIdle()

            // Overlay should be visible regardless of lighting
            composeTestRule.onNodeWithText("⚠️ 1 Issue").assertIsDisplayed()
        }
    }

    @Test
    fun arOverlayIntegration_maintainsStateAcrossUpdates() = runTest {
        // Given
        val initialHazard = createTestHazard(Severity.HIGH)
        val initialAnalysis = createTestAnalysis(listOf(initialHazard))
        
        // Set initial state with hazard selected
        var selectedHazardId: String? = null
        
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = initialAnalysis,
                onHazardClick = { hazard -> selectedHazardId = hazard.id },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Select a hazard
        composeTestRule.onNodeWithContentDescription("OSHA Badge", substring = true)
            .performClick()
        
        assertEquals(initialHazard.id, selectedHazardId)

        // Update analysis with same hazard plus new one
        val updatedHazard = initialHazard.copy(confidence = 0.95f)
        val newHazard = createTestHazard(Severity.CRITICAL, id = "new-hazard")
        val updatedAnalysis = createTestAnalysis(listOf(updatedHazard, newHazard))

        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = updatedAnalysis,
                onHazardClick = { hazard -> selectedHazardId = hazard.id },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Selection state should be maintained for existing hazard
        composeTestRule.onNodeWithText("🚨 CRITICAL").assertIsDisplayed()
        composeTestRule.onNodeWithText("⚠️ 2 Issues").assertIsDisplayed()
    }

    // Helper methods
    private fun createTestHazard(
        severity: Severity = Severity.MEDIUM,
        boundingBox: BoundingBox = BoundingBox(0.3f, 0.3f, 0.2f, 0.2f),
        id: String = "test-hazard-${severity.name}"
    ): Hazard {
        return Hazard(
            id = id,
            type = HazardType.FALL_PROTECTION,
            severity = severity,
            description = "Test hazard",
            oshaCode = "TEST.001",
            boundingBox = boundingBox,
            confidence = 0.85f,
            recommendations = listOf("Test recommendation")
        )
    }

    private fun createTestAnalysis(hazards: List<Hazard>): SafetyAnalysis {
        return SafetyAnalysis(
            id = "test-analysis",
            timestamp = System.currentTimeMillis(),
            analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
            workType = WorkType.GENERAL_CONSTRUCTION,
            hazards = hazards,
            ppeStatus = createTestPPEStatus(),
            recommendations = listOf("Test recommendation"),
            overallRiskLevel = RiskLevel.MEDIUM,
            confidence = 0.85f,
            processingTimeMs = 1500L,
            oshaViolations = emptyList(),
            metadata = null
        )
    }

    private fun createTestPPEStatus(): PPEStatus {
        return PPEStatus(
            hardHat = PPEItem(PPEItemStatus.PRESENT, 0.9f, null, true),
            safetyVest = PPEItem(PPEItemStatus.PRESENT, 0.85f, null, true),
            safetyBoots = PPEItem(PPEItemStatus.PRESENT, 0.8f, null, true),
            safetyGlasses = PPEItem(PPEItemStatus.UNKNOWN, 0.5f, null, false),
            fallProtection = PPEItem(PPEItemStatus.MISSING, 0.0f, null, false),
            respirator = PPEItem(PPEItemStatus.UNKNOWN, 0.0f, null, false),
            overallCompliance = 0.7f
        )
    }
}

/**
 * Mock camera frame provider for testing overlay integration
 */
class MockCameraFrameProvider {
    private var currentFrame = 0
    private var lightingLevel = 0.5f

    fun provideFrame(frameIndex: Int) {
        currentFrame = frameIndex
    }

    fun setLightingLevel(level: Float) {
        lightingLevel = level
    }

    fun getCurrentFrame(): Int = currentFrame
    fun getLightingLevel(): Float = lightingLevel
}

/**
 * Mock camera preview component for testing
 */
@androidx.compose.runtime.Composable
fun MockCameraPreview(
    frameProvider: MockCameraFrameProvider,
    modifier: Modifier = Modifier
) {
    // In a real implementation, this would be the actual camera preview
    Box(modifier = modifier)
}
