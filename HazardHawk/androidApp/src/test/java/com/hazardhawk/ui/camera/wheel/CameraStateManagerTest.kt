package com.hazardhawk.ui.camera.wheel

import com.hazardhawk.camera.CameraState
import com.hazardhawk.camera.CameraStateManager
import com.hazardhawk.data.repositories.CameraSettingsRepository
import com.hazardhawk.data.models.CameraSettings
import com.hazardhawk.ui.components.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * Tests for CameraStateManager focusing on zoom jumping prevention
 * and state synchronization issues
 */
@ExperimentalCoroutinesApi
class CameraStateManagerTest {

    @Mock
    private lateinit var mockRepository: CameraSettingsRepository

    private lateinit var testScope: TestScope
    private lateinit var stateManager: CameraStateManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testScope = TestScope()
        
        // Setup mock repository defaults
        whenever(mockRepository.loadSettings()).thenReturn(
            CameraSettings(
                selectedAspectRatio = "full",
                selectedAspectRatioIndex = 0,
                selectedZoom = 1f,
                selectedZoomIndex = 1,
                minZoom = 0.5f,
                maxZoom = 20f
            )
        )
        
        whenever(mockRepository.getSettingsFlow()).thenReturn(
            flowOf(CameraSettings())
        )
        
        stateManager = CameraStateManager(mockRepository, testScope)
    }

    @After
    fun teardown() {
        testScope.cancel()
    }

    @Test
    fun `test zoom state consistency prevents jumping to 1x`() = testScope.runTest {
        // Start with default state (should be 1x)
        var currentState = stateManager.state.value
        assertEquals("Initial zoom should be 1x", 1f, currentState.zoom)

        // Update to 10x zoom
        stateManager.updateZoom(10f, isLive = false)
        
        currentState = stateManager.state.value
        assertEquals("Zoom should update to 10x", 10f, currentState.zoom)
        
        // Verify zoom change event is emitted
        val zoomEvent = stateManager.zoomChangeEvents.first()
        assertNotNull("Zoom event should be emitted", zoomEvent)
        assertEquals("Zoom event should contain correct value", 10f, zoomEvent!!.zoom)
        assertFalse("Zoom event should not be live", zoomEvent.isLive)

        // Simulate some other state changes that might cause jumping
        stateManager.updateAspectRatio(AspectRatios.RATIO_16_9, 1)
        advanceUntilIdle()

        // Verify zoom hasn't jumped back to 1x
        currentState = stateManager.state.value
        assertEquals("Zoom should remain at 10x after aspect ratio change", 
            10f, currentState.zoom)
    }

    @Test
    fun `test zoom index synchronization`() = testScope.runTest {
        // Update zoom range to ensure we have specific zoom levels
        stateManager.updateZoomRange(0.5f, 20f)
        
        val state = stateManager.state.value
        val zoomLevels = state.supportedZoomLevels
        
        // Find 5x zoom in the levels
        val targetZoomIndex = zoomLevels.indexOfFirst { (it.value as Float) == 5f }
        assertTrue("5x zoom should exist in zoom levels", targetZoomIndex >= 0)
        
        // Update zoom to 5x
        stateManager.updateZoom(5f, isLive = false)
        
        val updatedState = stateManager.state.value
        assertEquals("Zoom value should be 5x", 5f, updatedState.zoom)
        
        // Verify the selected index matches
        val selectedZoomItem = updatedState.supportedZoomLevels[updatedState.selectedZoomIndex]
        val selectedValue = selectedZoomItem.value as Float
        assertEquals("Selected index should correspond to 5x zoom", 5f, selectedValue, 0.1f)
    }

    @Test
    fun `test concurrent zoom and aspect ratio updates`() = testScope.runTest {
        // Perform concurrent updates
        stateManager.updateZoom(7f, isLive = false)
        stateManager.updateAspectRatio(AspectRatios.RATIO_4_3, 2)
        
        advanceUntilIdle()
        
        val finalState = stateManager.state.value
        
        // Both changes should be applied without interference
        assertEquals("Zoom should be applied", 7f, finalState.zoom)
        assertEquals("Aspect ratio should be applied", AspectRatios.RATIO_4_3, finalState.aspectRatio)
        assertEquals("Aspect ratio index should be applied", 2, finalState.selectedAspectRatioIndex)
    }

    @Test
    fun `test live zoom updates do not persist immediately`() = testScope.runTest {
        // Perform live zoom update (like during pinch-to-zoom)
        stateManager.updateZoom(3f, isLive = true)
        
        val state = stateManager.state.value
        assertEquals("Live zoom should update state", 3f, state.zoom)
        assertTrue("State should indicate live zoom", state.isZoomLive)
        
        advanceUntilIdle()
        
        // Verify repository was not called for live updates (to avoid excessive writes)
        verify(mockRepository, never()).updateZoom(eq(3f), any())
        
        // Now perform non-live update
        stateManager.updateZoom(3f, isLive = false)
        advanceUntilIdle()
        
        // This should persist to repository
        verify(mockRepository).updateZoom(eq(3f), any())
    }

    @Test
    fun `test zoom range updates adjust current zoom appropriately`() = testScope.runTest {
        // Set zoom to 15x
        stateManager.updateZoom(15f, isLive = false)
        assertEquals("Zoom should be 15x", 15f, stateManager.state.value.zoom)
        
        // Update range to exclude 15x (new max is 10x)
        stateManager.updateZoomRange(0.5f, 10f)
        
        val state = stateManager.state.value
        assertEquals("Zoom should be clamped to new max", 10f, state.zoom)
        assertEquals("Min zoom should be updated", 0.5f, state.minZoom)
        assertEquals("Max zoom should be updated", 10f, state.maxZoom)
        
        // Verify zoom levels were regenerated
        val maxZoomItem = state.supportedZoomLevels.maxByOrNull { it.value as Float }
        assertNotNull("Should have zoom levels", maxZoomItem)
        assertTrue("Max zoom level should not exceed new range", 
            (maxZoomItem!!.value as Float) <= 10f)
    }

    @Test
    fun `test aspect ratio change events are properly managed`() = testScope.runTest {
        val initialAspectRatio = stateManager.state.value.aspectRatio
        
        // Change aspect ratio
        stateManager.updateAspectRatio(AspectRatios.RATIO_16_9, 1)
        
        // Verify change event is emitted
        val changeEvent = stateManager.aspectRatioChangeEvents.first()
        assertNotNull("Aspect ratio change event should be emitted", changeEvent)
        assertEquals("Change event should contain correct ratio item", 
            AspectRatios.RATIO_16_9, changeEvent!!.ratioItem)
        
        // Clear the event
        stateManager.clearAspectRatioEvent()
        
        // Verify event is cleared
        assertNull("Aspect ratio event should be cleared", 
            stateManager.aspectRatioChangeEvents.value)
    }

    @Test
    fun `test settings persistence on aspect ratio changes`() = testScope.runTest {
        stateManager.updateAspectRatio(AspectRatios.RATIO_1_1, 3)
        
        advanceUntilIdle()
        
        // Verify repository was called with correct parameters
        verify(mockRepository).updateAspectRatio("1:1", 3)
    }

    @Test
    fun `test state loading from persistent settings`() = testScope.runTest {
        // Setup mock to return specific saved settings
        val savedSettings = CameraSettings(
            selectedAspectRatio = "16:9",
            selectedAspectRatioIndex = 1,
            selectedZoom = 5f,
            selectedZoomIndex = 3,
            minZoom = 0.5f,
            maxZoom = 15f
        )
        
        whenever(mockRepository.loadSettings()).thenReturn(savedSettings)
        whenever(mockRepository.getSettingsFlow()).thenReturn(flowOf(savedSettings))
        
        // Create new state manager to trigger loading
        val newStateManager = CameraStateManager(mockRepository, testScope)
        
        advanceUntilIdle()
        
        val state = newStateManager.state.value
        
        // Verify state was loaded from saved settings
        assertEquals("Should load saved zoom", 5f, state.zoom)
        assertEquals("Should load saved aspect ratio index", 1, state.selectedAspectRatioIndex)
        assertEquals("Should load saved zoom range min", 0.5f, state.minZoom)
        assertEquals("Should load saved zoom range max", 15f, state.maxZoom)
    }

    @Test
    fun `test zoom persistence after live gestures end`() = testScope.runTest {
        // Perform live zoom updates
        stateManager.updateZoom(4f, isLive = true)
        stateManager.updateZoom(6f, isLive = true)
        stateManager.updateZoom(8f, isLive = true)
        
        // End gesture and persist
        stateManager.persistCurrentZoom()
        
        advanceUntilIdle()
        
        // Should persist final zoom value
        verify(mockRepository).updateZoom(eq(8f), any())
    }

    @Test
    fun `test supported aspect ratios update handling`() = testScope.runTest {
        val originalRatios = AspectRatios.getSupported()
        val newRatios = listOf(AspectRatios.RATIO_16_9, AspectRatios.RATIO_1_1)
        
        // Set current ratio to something that will be removed
        stateManager.updateAspectRatio(AspectRatios.RATIO_4_3, 2)
        
        // Update supported ratios (removing 4:3)
        stateManager.updateSupportedAspectRatios(newRatios)
        
        val state = stateManager.state.value
        assertEquals("Should update supported ratios", newRatios, state.supportedAspectRatios)
        
        // Should fall back to first available ratio since current was removed
        assertEquals("Should fallback to first available ratio", 
            newRatios[0], state.aspectRatio)
        assertEquals("Should update index accordingly", 0, state.selectedAspectRatioIndex)
    }

    @Test
    fun `test rapid zoom updates are handled gracefully`() = testScope.runTest {
        val zoomUpdates = mutableListOf<Float>()
        
        // Monitor zoom change events
        backgroundScope.launch {
            stateManager.zoomChangeEvents.collect { event ->
                event?.let { zoomUpdates.add(it.zoom) }
            }
        }
        
        // Perform rapid zoom changes
        repeat(10) { index ->
            val zoom = 1f + (index * 0.5f)
            stateManager.updateZoom(zoom, isLive = true)
        }
        
        advanceUntilIdle()
        
        // Should handle all updates without crashes or data corruption
        assertTrue("Should handle rapid updates", zoomUpdates.isNotEmpty())
        assertEquals("Final zoom should be correct", 5.5f, stateManager.state.value.zoom)
    }

    @Test
    fun `test zoom bounds enforcement`() = testScope.runTest {
        stateManager.updateZoomRange(1f, 10f)
        
        // Try to set zoom below minimum
        stateManager.updateZoom(0.5f, isLive = false)
        assertEquals("Zoom should be clamped to minimum", 1f, stateManager.state.value.zoom)
        
        // Try to set zoom above maximum
        stateManager.updateZoom(15f, isLive = false)
        assertEquals("Zoom should be clamped to maximum", 10f, stateManager.state.value.zoom)
        
        // Valid zoom should be accepted
        stateManager.updateZoom(5f, isLive = false)
        assertEquals("Valid zoom should be accepted", 5f, stateManager.state.value.zoom)
    }
}
