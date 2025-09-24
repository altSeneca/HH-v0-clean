# Comprehensive Camera Wheel Testing Strategy

## Executive Summary

This document outlines a comprehensive testing strategy for HazardHawk's camera wheel fixes and positioning improvements. The strategy focuses on ensuring the dual vertical selectors (aspect ratio and zoom controls) work reliably across all scenarios, addressing current issues with zoom wheel visibility, jumping behavior, ratio wheel responsiveness, and state synchronization.

## Current Issues Analysis

Based on codebase analysis, the following issues have been identified:

### Zoom Wheel Issues
- **Visibility Problems**: Zoom wheel may not be visible in certain configurations
- **Jumping Behavior**: Zoom reverts from 10x to 1x unexpectedly
- **State Synchronization**: Misalignment between UI wheel position and camera controller zoom level
- **Continuous Updates**: Live zoom updates during drag gestures may cause performance issues

### Ratio Wheel Issues
- **Selection Responsiveness**: Aspect ratio changes may not be immediately reflected in the viewfinder
- **State Persistence**: Selected aspect ratio may not persist across app sessions
- **Index Synchronization**: Mismatch between selected index and displayed item

## Testing Strategy Overview

### 1. **Current Issues Testing**

#### 1.1 Zoom Wheel Visibility Tests

**Unit Tests**
```kotlin
@Test
fun `test zoom wheel visibility in all configurations`() {
    val zoomLevels = ZoomLevels.generateZoomItems(0.5f, 20f)
    val cameraState = CameraState(supportedZoomLevels = zoomLevels)
    
    assertThat(zoomLevels).isNotEmpty()
    assertThat(cameraState.supportedZoomLevels).containsExactlyElementsIn(zoomLevels)
}

@Test  
fun `test zoom wheel items generation with edge cases`() {
    // Test with very limited zoom range
    val limitedZoom = ZoomLevels.generateZoomItems(0.8f, 1.2f)
    assertThat(limitedZoom).isNotEmpty()
    
    // Test with extreme zoom range
    val extremeZoom = ZoomLevels.generateZoomItems(0.1f, 100f)
    assertThat(extremeZoom).isNotEmpty()
    
    // Test with invalid range (min > max)
    val invalidZoom = ZoomLevels.generateZoomItems(10f, 5f)
    assertThat(invalidZoom).hasSize(1) // Should fallback to basic 1x zoom
}
```

**Integration Tests**
```kotlin
@Test
fun `test zoom wheel visibility on different screen sizes`() {
    // Test on phone screens
    testZoomWheelVisibility(screenSize = ScreenSize.PHONE)
    
    // Test on tablet screens  
    testZoomWheelVisibility(screenSize = ScreenSize.TABLET)
    
    // Test on Android TV
    testZoomWheelVisibility(screenSize = ScreenSize.TV)
}

private fun testZoomWheelVisibility(screenSize: ScreenSize) {
    composeTestRule.setContent {
        TestDeviceConfiguration(screenSize) {
            DualVerticalSelectors(
                cameraStateManager = testCameraStateManager,
                onAspectRatioChange = { _, _ -> },
                onZoomChange = { },
                onZoomLive = { }
            )
        }
    }
    
    composeTestRule.onNodeWithTag("ZoomWheel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ZoomWheel").assertHasClickAction()
}
```

#### 1.2 Zoom Jumping Behavior Tests

**Unit Tests**
```kotlin
@Test
fun `test zoom state consistency prevents jumping`() {
    val stateManager = CameraStateManager(mockRepository, testScope)
    
    // Simulate zoom to 10x
    stateManager.updateZoom(10f, isLive = false)
    
    // Verify state is consistent
    assertEquals(10f, stateManager.state.value.zoom)
    
    // Simulate camera controller update
    val zoomEvent = stateManager.zoomChangeEvents.value
    assertNotNull(zoomEvent)
    assertEquals(10f, zoomEvent!!.zoom)
    assertFalse(zoomEvent.isLive)
    
    // Verify no unexpected state changes
    advanceUntilIdle()
    assertEquals(10f, stateManager.state.value.zoom)
}

@Test
fun `test zoom wheel index synchronization`() {
    val zoomLevels = ZoomLevels.generateZoomItems(0.5f, 20f)
    val stateManager = CameraStateManager(mockRepository, testScope)
    
    stateManager.updateZoomRange(0.5f, 20f)
    stateManager.updateZoom(10f, isLive = false)
    
    val state = stateManager.state.value
    val selectedZoomItem = state.supportedZoomLevels[state.selectedZoomIndex]
    val selectedValue = selectedZoomItem.value as Float
    
    // Verify zoom value matches selected index
    assertEquals(10f, selectedValue, 0.1f)
}
```

**Integration Tests**  
```kotlin
@Test
fun `test zoom jumping prevention in real UI interactions`() {
    var capturedZoomValues = mutableListOf<Float>()
    
    composeTestRule.setContent {
        DualVerticalSelectors(
            cameraStateManager = testCameraStateManager,
            onAspectRatioChange = { _, _ -> },
            onZoomChange = { zoom -> capturedZoomValues.add(zoom) },
            onZoomLive = { }
        )
    }
    
    // Simulate scroll to 10x zoom
    composeTestRule.onNodeWithTag("ZoomWheel")
        .performScrollToIndex(findZoomIndex(10f))
    
    composeTestRule.waitForIdle()
    
    // Verify zoom doesn't jump back to 1x
    assertThat(capturedZoomValues.last()).isEqualTo(10f)
    
    // Simulate additional interactions
    composeTestRule.onNodeWithTag("ZoomWheel")
        .performClick()
    
    composeTestRule.waitForIdle()
    
    // Verify zoom remains stable
    assertThat(capturedZoomValues.last()).isEqualTo(10f)
}
```

#### 1.3 Ratio Wheel Selection Tests

**Unit Tests**
```kotlin
@Test
fun `test aspect ratio responsiveness`() {
    val stateManager = CameraStateManager(mockRepository, testScope)
    val aspectRatios = AspectRatios.getSupported()
    val targetRatio = AspectRatios.RATIO_16_9
    val targetIndex = aspectRatios.indexOf(targetRatio)
    
    stateManager.updateAspectRatio(targetRatio, targetIndex)
    
    // Verify immediate state update
    assertEquals(targetRatio, stateManager.state.value.aspectRatio)
    assertEquals(targetIndex, stateManager.state.value.selectedAspectRatioIndex)
    
    // Verify change event is emitted
    val changeEvent = stateManager.aspectRatioChangeEvents.value
    assertNotNull(changeEvent)
    assertEquals(targetRatio, changeEvent!!.ratioItem)
}

@Test
fun `test aspect ratio persistence`() {
    val mockRepo = mock<CameraSettingsRepository>()
    val stateManager = CameraStateManager(mockRepo, testScope)
    
    stateManager.updateAspectRatio(AspectRatios.RATIO_4_3, 2)
    
    advanceUntilIdle()
    
    // Verify settings are persisted
    verify(mockRepo).updateAspectRatio("4:3", 2)
}
```

#### 1.4 State Synchronization Tests

**Unit Tests**
```kotlin
@Test
fun `test wheel UI and camera controller synchronization`() {
    val cameraController = mock<LifecycleCameraController>()
    val cameraInfo = mock<CameraInfo>()
    val zoomState = mock<LiveData<ZoomState>>()
    val zoomStateValue = mock<androidx.camera.core.ZoomState>()
    
    whenever(cameraController.cameraInfo).thenReturn(cameraInfo)
    whenever(cameraInfo.zoomState).thenReturn(zoomState)
    whenever(zoomState.value).thenReturn(zoomStateValue)
    whenever(zoomStateValue.zoomRatio).thenReturn(5f)
    whenever(zoomStateValue.minZoomRatio).thenReturn(0.5f)
    whenever(zoomStateValue.maxZoomRatio).thenReturn(20f)
    
    var appliedZoom = 0f
    val onZoomChange: (Float) -> Unit = { zoom ->
        appliedZoom = zoom
        whenever(cameraController.setZoomRatio(zoom)).thenReturn(mock())
    }
    
    composeTestRule.setContent {
        DualVerticalSelectorsWithCameraController(
            cameraStateManager = testCameraStateManager,
            cameraController = cameraController,
            onAspectRatioChange = { _, _ -> }
        )
    }
    
    // Simulate wheel interaction
    composeTestRule.onNodeWithTag("ZoomWheel")
        .performScrollToIndex(findZoomIndex(5f))
    
    composeTestRule.waitForIdle()
    
    // Verify camera controller receives zoom update
    verify(cameraController).setZoomRatio(5f)
    assertEquals(5f, appliedZoom)
}
```

### 2. **Wheel Positioning Tests**

#### 2.1 Screen Size Adaptation Tests

**Integration Tests**
```kotlin
@Test
fun `test wheel positioning on different screen sizes`() {
    val screenSizes = listOf(
        ScreenConfiguration(width = 360.dp, height = 640.dp, density = 2f), // Small phone
        ScreenConfiguration(width = 411.dp, height = 731.dp, density = 2.6f), // Medium phone  
        ScreenConfiguration(width = 768.dp, height = 1024.dp, density = 2f), // Tablet portrait
        ScreenConfiguration(width = 1024.dp, height = 768.dp, density = 2f), // Tablet landscape
    )
    
    screenSizes.forEach { config ->
        testWheelPositioning(config)
    }
}

private fun testWheelPositioning(config: ScreenConfiguration) {
    composeTestRule.setContent {
        TestScreenConfiguration(config) {
            DualVerticalSelectors(
                cameraStateManager = testCameraStateManager,
                onAspectRatioChange = { _, _ -> },
                onZoomChange = { },
                onZoomLive = { }
            )
        }
    }
    
    // Test left wheel positioning (aspect ratio)
    composeTestRule.onNodeWithTag("AspectRatioWheel")
        .assertLeftPositionInRootIsEqualTo(56.dp) // padding start
        .assertWidthIsEqualTo(80.dp)
        .assertHeightIsEqualTo(240.dp)
    
    // Test right wheel positioning (zoom)
    composeTestRule.onNodeWithTag("ZoomWheel")
        .assertPositionInRootIsEqualTo(
            expectedLeft = config.width - 56.dp - 80.dp, // padding end + width
            expectedTop = (config.height - 240.dp) / 2 // centered vertically
        )
        .assertWidthIsEqualTo(80.dp)
        .assertHeightIsEqualTo(240.dp)
}
```

#### 2.2 Equal Spacing Tests

**UI Tests**
```kotlin
@Test 
fun `test equal spacing and padding for both wheels`() {
    composeTestRule.setContent {
        DualVerticalSelectors(
            cameraStateManager = testCameraStateManager,
            onAspectRatioChange = { _, _ -> },
            onZoomChange = { },
            onZoomLive = { }
        )
    }
    
    val aspectRatioWheel = composeTestRule.onNodeWithTag("AspectRatioWheel")
    val zoomWheel = composeTestRule.onNodeWithTag("ZoomWheel")
    
    // Both wheels should have same dimensions
    aspectRatioWheel.assertWidthIsEqualTo(80.dp)
    zoomWheel.assertWidthIsEqualTo(80.dp)
    
    aspectRatioWheel.assertHeightIsEqualTo(240.dp)
    zoomWheel.assertHeightIsEqualTo(240.dp)
    
    // Both wheels should have same padding from edges
    aspectRatioWheel.assertLeftPositionInRootIsEqualTo(56.dp)
    zoomWheel.assertRightPositionInRootIsEqualTo(56.dp)
    
    // Both wheels should be vertically centered
    val aspectRatioCenter = aspectRatioWheel.getBoundsInRoot().center.y
    val zoomCenter = zoomWheel.getBoundsInRoot().center.y
    assertEquals(aspectRatioCenter, zoomCenter, 1f) // 1dp tolerance
}
```

#### 2.3 Touch Target Accessibility Tests

**Accessibility Tests**
```kotlin
@Test
fun `test touch targets meet accessibility guidelines`() {
    composeTestRule.setContent {
        DualVerticalSelectors(
            cameraStateManager = testCameraStateManager,
            onAspectRatioChange = { _, _ -> },
            onZoomChange = { },
            onZoomLive = { }
        )
    }
    
    // Verify minimum touch target size (48dp x 48dp per Android guidelines)
    composeTestRule.onNodeWithTag("AspectRatioWheel")
        .assertTouchWidthIsEqualTo(80.dp) // Exceeds minimum
        
    composeTestRule.onNodeWithTag("ZoomWheel")
        .assertTouchHeightIsEqualTo(240.dp) // Exceeds minimum
    
    // Test with simulated gloved hands (larger touch areas)
    composeTestRule.onNodeWithTag("AspectRatioWheel")
        .performTouchInput {
            down(Offset(40.dp.toPx(), 120.dp.toPx()))
            moveBy(Offset(0f, 20.dp.toPx()))
            up()
        }
    
    composeTestRule.waitForIdle()
    
    // Verify interaction was registered
    // Implementation depends on your interaction tracking
}

@Test
fun `test wheel accessibility semantics`() {
    composeTestRule.setContent {
        DualVerticalSelectors(
            cameraStateManager = testCameraStateManager,
            onAspectRatioChange = { _, _ -> },
            onZoomChange = { },
            onZoomLive = { }
        )
    }
    
    // Test content descriptions
    composeTestRule.onNodeWithContentDescription("Aspect ratio selector")
        .assertIsDisplayed()
        
    composeTestRule.onNodeWithContentDescription("Zoom control")
        .assertIsDisplayed()
    
    // Test individual item descriptions
    composeTestRule.onNodeWithContentDescription(matches(".*selected.*"))
        .assertExists()
}
```

### 3. **State Management Testing**

#### 3.1 Zoom State Synchronization Tests

**Unit Tests**
```kotlin
@Test
fun `test zoom state synchronization without feedback loops`() {
    val stateManager = CameraStateManager(mockRepository, testScope)
    val zoomUpdates = mutableListOf<Float>()
    
    // Monitor zoom change events
    testScope.launch {
        stateManager.zoomChangeEvents.collect { event ->
            event?.let { zoomUpdates.add(it.zoom) }
        }
    }
    
    // Update zoom multiple times rapidly
    stateManager.updateZoom(2f, isLive = false)
    stateManager.updateZoom(5f, isLive = true)  
    stateManager.updateZoom(10f, isLive = false)
    
    advanceUntilIdle()
    
    // Verify we don't get duplicate or feedback updates
    assertThat(zoomUpdates).containsExactly(2f, 5f, 10f).inOrder()
    
    // Verify final state is consistent
    assertEquals(10f, stateManager.state.value.zoom)
}

@Test
fun `test concurrent zoom and aspect ratio changes`() {
    val stateManager = CameraStateManager(mockRepository, testScope)
    
    // Simulate concurrent updates
    launch {
        stateManager.updateZoom(5f, isLive = false)
    }
    launch {
        stateManager.updateAspectRatio(AspectRatios.RATIO_16_9, 1)
    }
    
    advanceUntilIdle()
    
    // Both changes should be applied without interference
    assertEquals(5f, stateManager.state.value.zoom)
    assertEquals(AspectRatios.RATIO_16_9, stateManager.state.value.aspectRatio)
    assertEquals(1, stateManager.state.value.selectedAspectRatioIndex)
}
```

#### 3.2 Camera Controller Integration Tests

**Integration Tests**
```kotlin
@Test
fun `test camera controller integration with wheel selections`() {
    val mockController = mock<LifecycleCameraController>()
    var lastAppliedZoom = 0f
    var lastAppliedAspectRatio = 0f
    
    whenever(mockController.setZoomRatio(any())).thenAnswer { invocation ->
        lastAppliedZoom = invocation.arguments[0] as Float
        mock<ListenableFuture<Void>>()
    }
    
    composeTestRule.setContent {
        DualVerticalSelectorsWithCameraController(
            cameraStateManager = testCameraStateManager,
            cameraController = mockController,
            onAspectRatioChange = { ratio, _ -> lastAppliedAspectRatio = ratio }
        )
    }
    
    // Test zoom wheel interaction
    composeTestRule.onNodeWithTag("ZoomWheel")
        .performScrollToIndex(findZoomIndex(3f))
    
    composeTestRule.waitForIdle()
    
    verify(mockController).setZoomRatio(3f)
    assertEquals(3f, lastAppliedZoom)
    
    // Test aspect ratio wheel interaction  
    composeTestRule.onNodeWithTag("AspectRatioWheel")
        .performScrollToIndex(1) // 16:9 ratio
    
    composeTestRule.waitForIdle()
    
    assertEquals(16f/9f, lastAppliedAspectRatio, 0.001f)
}
```

### 4. **Edge Cases & Scenarios Testing**

#### 4.1 Rapid Interaction Tests

**Stress Tests**
```kotlin
@Test
fun `test rapid wheel scrolling and selection changes`() {
    val stateManager = CameraStateManager(mockRepository, testScope)
    val zoomUpdates = mutableListOf<Float>()
    
    testScope.launch {
        stateManager.zoomChangeEvents.collect { event ->
            event?.let { zoomUpdates.add(it.zoom) }
        }
    }
    
    // Simulate rapid zoom changes
    repeat(20) { index ->
        val zoom = 1f + (index * 0.5f)
        stateManager.updateZoom(zoom, isLive = true)
    }
    
    advanceUntilIdle()
    
    // Verify system handles rapid updates gracefully
    assertTrue("Should handle rapid updates", zoomUpdates.isNotEmpty())
    assertEquals(10.5f, zoomUpdates.last(), 0.1f) // Final value should be correct
}

@Test
fun `test concurrent wheel interactions`() {
    composeTestRule.setContent {
        DualVerticalSelectors(
            cameraStateManager = testCameraStateManager,
            onAspectRatioChange = { _, _ -> },
            onZoomChange = { },
            onZoomLive = { }
        )
    }
    
    // Simulate simultaneous interactions on both wheels
    composeTestRule.onNodeWithTag("AspectRatioWheel")
        .performTouchInput {
            down(center)
            moveBy(Offset(0f, 100f))
        }
    
    composeTestRule.onNodeWithTag("ZoomWheel")
        .performTouchInput {
            down(center) 
            moveBy(Offset(0f, -100f))
        }
    
    // Release both gestures
    composeTestRule.onNodeWithTag("AspectRatioWheel")
        .performTouchInput { up() }
    
    composeTestRule.onNodeWithTag("ZoomWheel")
        .performTouchInput { up() }
    
    composeTestRule.waitForIdle()
    
    // Verify both interactions were processed without conflicts
    // Implementation depends on your state tracking
}
```

#### 4.2 Device Rotation Tests

**Configuration Change Tests**
```kotlin
@Test
fun `test device rotation and orientation changes`() {
    composeTestRule.setContent {
        DualVerticalSelectors(
            cameraStateManager = testCameraStateManager,
            onAspectRatioChange = { _, _ -> },
            onZoomChange = { },
            onZoomLive = { }
        )
    }
    
    // Set initial zoom
    composeTestRule.onNodeWithTag("ZoomWheel")
        .performScrollToIndex(findZoomIndex(5f))
    
    composeTestRule.waitForIdle()
    
    // Simulate rotation
    composeTestRule.setContent {
        TestDeviceConfiguration(orientation = Orientation.LANDSCAPE) {
            DualVerticalSelectors(
                cameraStateManager = testCameraStateManager,
                onAspectRatioChange = { _, _ -> },
                onZoomChange = { },
                onZoomLive = { }
            )
        }
    }
    
    // Verify state is preserved after rotation
    assertEquals(5f, testCameraStateManager.state.value.zoom)
    
    // Verify wheels are still functional
    composeTestRule.onNodeWithTag("ZoomWheel")
        .assertIsDisplayed()
        .performClick()
}
```

#### 4.3 Camera Permission and Error Tests

**Error Scenario Tests**
```kotlin
@Test
fun `test camera permission scenarios`() {
    val mockController = mock<LifecycleCameraController>()
    
    // Simulate camera permission denied
    whenever(mockController.cameraInfo).thenReturn(null)
    
    composeTestRule.setContent {
        DualVerticalSelectorsWithCameraController(
            cameraStateManager = testCameraStateManager,
            cameraController = mockController,
            onAspectRatioChange = { _, _ -> }
        )
    }
    
    // Wheels should still be displayed and functional for UI testing
    composeTestRule.onNodeWithTag("ZoomWheel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("AspectRatioWheel").assertIsDisplayed()
    
    // Interactions should be handled gracefully
    composeTestRule.onNodeWithTag("ZoomWheel")
        .performScrollToIndex(2)
    
    composeTestRule.waitForIdle()
    
    // Should not crash or cause exceptions
    verify(mockController, never()).setZoomRatio(any())
}

@Test
fun `test camera hardware capability scenarios`() {
    val mockController = mock<LifecycleCameraController>()
    val mockCameraInfo = mock<CameraInfo>()
    val mockZoomState = mock<LiveData<ZoomState>>()
    val mockZoomStateValue = mock<androidx.camera.core.ZoomState>()
    
    // Simulate limited zoom camera (e.g., front camera)
    whenever(mockController.cameraInfo).thenReturn(mockCameraInfo)
    whenever(mockCameraInfo.zoomState).thenReturn(mockZoomState)
    whenever(mockZoomState.value).thenReturn(mockZoomStateValue)
    whenever(mockZoomStateValue.minZoomRatio).thenReturn(1f)
    whenever(mockZoomStateValue.maxZoomRatio).thenReturn(2f) // Limited zoom
    
    composeTestRule.setContent {
        DualVerticalSelectorsWithCameraController(
            cameraStateManager = testCameraStateManager,
            cameraController = mockController,
            onAspectRatioChange = { _, _ -> }
        )
    }
    
    composeTestRule.waitForIdle()
    
    // Verify zoom range is updated to match camera capabilities
    val zoomLevels = testCameraStateManager.state.value.supportedZoomLevels
    val maxZoomItem = zoomLevels.maxByOrNull { it.value as Float }
    
    assertNotNull(maxZoomItem)
    assertTrue("Max zoom should not exceed camera capability", 
        (maxZoomItem!!.value as Float) <= 2f)
}
```

### 5. **Integration Testing**

#### 5.1 Complete Camera Integration Tests

**End-to-End Tests**
```kotlin
@Test
fun `test complete camera workflow with wheel interactions`() {
    val mockController = mock<LifecycleCameraController>()
    val capturedPhotos = mutableListOf<String>()
    
    composeTestRule.setContent {
        CameraScreen(
            cameraController = mockController,
            onPhotoCaptured = { uri -> capturedPhotos.add(uri) }
        )
    }
    
    // 1. Change aspect ratio
    composeTestRule.onNodeWithTag("AspectRatioWheel")
        .performScrollToIndex(1) // 16:9
    
    // 2. Adjust zoom
    composeTestRule.onNodeWithTag("ZoomWheel")
        .performScrollToIndex(findZoomIndex(3f))
    
    // 3. Capture photo
    composeTestRule.onNodeWithTag("CaptureButton")
        .performClick()
    
    composeTestRule.waitForIdle()
    
    // Verify all interactions worked together
    verify(mockController).setZoomRatio(3f)
    // Verify aspect ratio was applied to viewfinder
    // Verify photo was captured with correct settings
}
```

#### 5.2 Performance Integration Tests

**Performance Tests**
```kotlin
@Test
fun `test wheel performance under memory pressure`() {
    // Simulate low memory conditions
    simulateMemoryPressure()
    
    composeTestRule.setContent {
        DualVerticalSelectors(
            cameraStateManager = testCameraStateManager,
            onAspectRatioChange = { _, _ -> },
            onZoomChange = { },
            onZoomLive = { }
        )
    }
    
    // Perform intensive wheel operations
    repeat(100) {
        composeTestRule.onNodeWithTag("ZoomWheel")
            .performScrollToIndex(it % 5)
        
        composeTestRule.waitForIdle()
    }
    
    // Verify performance remains acceptable
    val fpsCounter = measureFrameRate {
        composeTestRule.onNodeWithTag("ZoomWheel")
            .performScrollToIndex(3)
    }
    
    assertTrue("Frame rate should remain above 30fps under memory pressure", 
        fpsCounter > 30)
}
```

### 6. **User Experience Testing**

#### 6.1 Responsiveness Tests

**UI Responsiveness Tests**
```kotlin
@Test
fun `test wheel responsiveness and smooth scrolling`() {
    val frameRates = mutableListOf<Double>()
    
    composeTestRule.setContent {
        DualVerticalSelectors(
            cameraStateManager = testCameraStateManager,
            onAspectRatioChange = { _, _ -> },
            onZoomChange = { },
            onZoomLive = { }
        )
    }
    
    // Measure frame rate during smooth scrolling
    val frameRate = measureFrameRate {
        composeTestRule.onNodeWithTag("ZoomWheel")
            .performTouchInput {
                down(center)
                repeat(20) { step ->
                    moveBy(Offset(0f, 10f))
                }
                up()
            }
    }
    
    assertTrue("Scrolling should maintain 60fps", frameRate >= 58.0)
}

@Test
fun `test haptic feedback timing and strength`() {
    var hapticEvents = mutableListOf<Long>()
    
    // Mock haptic feedback to capture timing
    val mockHaptic = mock<HapticFeedback>()
    whenever(mockHaptic.performHapticFeedback(any())).then {
        hapticEvents.add(System.currentTimeMillis())
    }
    
    composeTestRule.setContent {
        CompositionLocalProvider(LocalHapticFeedback provides mockHaptic) {
            DualVerticalSelectors(
                cameraStateManager = testCameraStateManager,
                onAspectRatioChange = { _, _ -> },
                onZoomChange = { },
                onZoomLive = { }
            )
        }
    }
    
    // Perform multiple wheel interactions
    repeat(5) {
        composeTestRule.onNodeWithTag("ZoomWheel")
            .performScrollToIndex(it)
        composeTestRule.waitForIdle()
    }
    
    // Verify haptic feedback occurred for each selection
    assertEquals(5, hapticEvents.size)
    
    // Verify timing between haptic events is reasonable (not too rapid)
    if (hapticEvents.size > 1) {
        val intervals = hapticEvents.zipWithNext { a, b -> b - a }
        assertTrue("Haptic intervals should be at least 100ms apart",
            intervals.all { it >= 100 })
    }
}
```

#### 6.2 Visual Feedback Tests

**Visual State Tests**
```kotlin
@Test
fun `test visual feedback and selection highlighting`() {
    composeTestRule.setContent {
        DualVerticalSelectors(
            cameraStateManager = testCameraStateManager,
            onAspectRatioChange = { _, _ -> },
            onZoomChange = { },
            onZoomLive = { }
        )
    }
    
    // Test initial selection is visually highlighted
    composeTestRule.onNodeWithTag("ZoomWheel")
        .onChildren()
        .filterToOne(hasContentDescription(matches(".*selected.*")))
        .assertExists()
    
    // Change selection
    composeTestRule.onNodeWithTag("ZoomWheel")
        .performScrollToIndex(2)
    
    composeTestRule.waitForIdle()
    
    // Verify new selection is highlighted
    composeTestRule.onNodeWithTag("ZoomWheel")
        .onChildren()
        .filterToOne(hasContentDescription(matches(".*2x.*selected.*")))
        .assertExists()
    
    // Verify scaling animation
    composeTestRule.onNodeWithTag("ZoomWheel")
        .onChildren()
        .filterToOne(hasContentDescription(matches(".*selected.*")))
        .assert(hasTestTag("ScaledItem")) // Assuming you tag scaled items
}

@Test
fun `test accessibility features and contrast`() {
    composeTestRule.setContent {
        TestHighContrastMode {
            DualVerticalSelectors(
                cameraStateManager = testCameraStateManager,
                onAspectRatioChange = { _, _ -> },
                onZoomChange = { },
                onZoomLive = { }
            )
        }
    }
    
    // Verify wheels remain visible in high contrast mode
    composeTestRule.onNodeWithTag("ZoomWheel")
        .assertIsDisplayed()
        .captureToImage()
        .assertColorContrast(minimumRatio = 4.5f) // WCAG AA standard
    
    composeTestRule.onNodeWithTag("AspectRatioWheel")
        .assertIsDisplayed()
        .captureToImage()
        .assertColorContrast(minimumRatio = 4.5f)
}
```

## Manual Testing Procedures

### 1. **Device Testing Matrix**

| Device Category | Screen Size | Resolution | Density | Test Priority |
|----------------|-------------|------------|---------|---------------|
| Small Phone | 5.0" - 5.5" | 720p+ | ~320dpi | High |
| Medium Phone | 5.5" - 6.1" | 1080p+ | ~400dpi | High |
| Large Phone | 6.1" - 6.7" | 1080p+ | ~400dpi | Medium |
| Small Tablet | 7" - 8" | 1200p+ | ~160dpi | Medium |
| Large Tablet | 9" - 12" | 1400p+ | ~160dpi | Low |
| Rugged Device | Varies | 720p+ | ~300dpi | High |

### 2. **Manual Test Scenarios**

#### Scenario 1: Basic Wheel Functionality
1. **Setup**: Launch camera screen
2. **Steps**:
   - Locate both wheels (aspect ratio left, zoom right)
   - Verify equal spacing from screen edges (56dp padding)
   - Verify both wheels are same size (80dp x 240dp)
   - Test scrolling both wheels independently
   - Verify haptic feedback on selection changes
   - Verify visual selection highlighting
3. **Expected**: Smooth, responsive wheel interactions with proper feedback

#### Scenario 2: Zoom Stability Test  
1. **Setup**: Camera screen with zoom wheel visible
2. **Steps**:
   - Scroll zoom wheel to 10x
   - Verify zoom level shows "10×" 
   - Take a photo
   - Return to camera view
   - Verify zoom remains at 10x (no jumping to 1x)
   - Switch to front camera and back
   - Verify zoom setting is preserved
3. **Expected**: Zoom level remains stable throughout interactions

#### Scenario 3: Gloved Hand Testing
1. **Setup**: Wear construction gloves, launch camera
2. **Steps**:
   - Attempt to scroll both wheels with gloved hands
   - Test tap interactions on wheel items
   - Test drag gestures on wheels
   - Verify all interactions register properly
3. **Expected**: All wheel interactions work reliably with gloves

#### Scenario 4: Rapid Interaction Test
1. **Setup**: Camera screen ready
2. **Steps**:
   - Rapidly scroll zoom wheel up and down 10 times
   - Immediately switch to aspect ratio wheel
   - Rapidly change aspect ratio selections 5 times
   - Return to zoom wheel and make final selection
   - Verify final states are correct
3. **Expected**: System handles rapid inputs without crashes or incorrect states

#### Scenario 5: Memory Pressure Test
1. **Setup**: Open multiple heavy apps to consume memory
2. **Steps**:
   - Launch HazardHawk camera
   - Interact with both wheels extensively
   - Monitor for lag, frame drops, or crashes
   - Take photos while interacting with wheels
3. **Expected**: Wheel performance remains acceptable under memory pressure

### 3. **Construction Site Testing Protocol**

#### Environmental Conditions
- **Temperature**: Test in cold (32°F/0°C) and hot (100°F/38°C) conditions
- **Lighting**: Test in bright sunlight and low-light conditions  
- **Humidity**: Test in high humidity/moisture conditions
- **Dust**: Test with dusty/dirty screen conditions

#### Real-World Usage Patterns
1. **Single-handed operation** while holding materials
2. **Quick adjustments** during active work
3. **Gloved hand usage** with different glove types
4. **Dirty screen conditions** with reduced visibility

## Test Automation Framework

### 1. **Automated Test Execution**

```bash
#!/bin/bash
# Camera Wheel Test Runner

echo "Running Camera Wheel Test Suite..."

# Unit Tests
./gradlew :shared:testDebugUnitTest \
  --tests "*WheelSelector*" \
  --tests "*CameraState*" \
  --tests "*ZoomControl*"

# Integration Tests  
./gradlew :androidApp:testDebugUnitTest \
  --tests "*DualVerticalSelectors*" \
  --tests "*CameraController*"

# UI Tests
./gradlew :androidApp:connectedDebugAndroidTest \
  --tests "*WheelPositioning*" \
  --tests "*WheelInteraction*" \
  --tests "*AccessibilityTest*"

# Performance Tests
./gradlew :androidApp:connectedDebugAndroidTest \
  --tests "*PerformanceTest*"

echo "Test execution complete. Results in build/reports/"
```

### 2. **Continuous Integration Pipeline**

```yaml
# .github/workflows/camera-wheel-tests.yml
name: Camera Wheel Tests

on:
  pull_request:
    paths:
      - '**/camera/**'
      - '**/ui/camera/**'
      - '**/ui/components/Wheel*'

jobs:
  wheel-tests:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
    
    - name: Run Unit Tests
      run: ./gradlew testDebugUnitTest --tests "*Wheel*"
    
    - name: Run Integration Tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 29
        script: ./gradlew connectedDebugAndroidTest --tests "*Wheel*"
    
    - name: Performance Validation
      run: ./scripts/validate-wheel-performance.sh
    
    - name: Upload Test Reports
      uses: actions/upload-artifact@v3
      with:
        name: wheel-test-reports
        path: |
          **/build/reports/tests/
          **/build/reports/androidTests/
```

### 3. **Test Data and Utilities**

```kotlin
// Test utilities for camera wheel testing
object CameraWheelTestUtils {
    
    fun findZoomIndex(targetZoom: Float): Int {
        val zoomLevels = ZoomLevels.generateZoomItems()
        return zoomLevels.indexOfFirst { 
            abs((it.value as Float) - targetZoom) < 0.1f 
        }.coerceAtLeast(0)
    }
    
    fun createTestCameraStateManager(): CameraStateManager {
        val mockRepo = mock<CameraSettingsRepository>()
        val testScope = TestScope()
        return CameraStateManager(mockRepo, testScope)
    }
    
    fun simulateMemoryPressure() {
        // Allocate large amounts of memory to simulate pressure
        val memoryConsumer = mutableListOf<ByteArray>()
        repeat(100) {
            memoryConsumer.add(ByteArray(1024 * 1024)) // 1MB chunks
        }
    }
    
    @Composable
    fun TestDeviceConfiguration(
        screenSize: ScreenSize = ScreenSize.PHONE,
        orientation: Orientation = Orientation.PORTRAIT,
        content: @Composable () -> Unit
    ) {
        val configuration = when (screenSize) {
            ScreenSize.PHONE -> LocalConfiguration.current.copy(
                screenWidthDp = 360,
                screenHeightDp = 640,
                orientation = orientation.value
            )
            ScreenSize.TABLET -> LocalConfiguration.current.copy(
                screenWidthDp = 768,
                screenHeightDp = 1024,
                orientation = orientation.value
            )
            ScreenSize.TV -> LocalConfiguration.current.copy(
                screenWidthDp = 1920,
                screenHeightDp = 1080,
                orientation = orientation.value
            )
        }
        
        CompositionLocalProvider(LocalConfiguration provides configuration) {
            content()
        }
    }
}

enum class ScreenSize { PHONE, TABLET, TV }
enum class Orientation(val value: Int) {
    PORTRAIT(Configuration.ORIENTATION_PORTRAIT),
    LANDSCAPE(Configuration.ORIENTATION_LANDSCAPE)
}
```

## Success Criteria

### 1. **Functional Requirements**
- [ ] All zoom wheel visibility issues resolved
- [ ] Zoom jumping behavior (1x to 10x reversion) eliminated  
- [ ] Aspect ratio wheel selection is immediately responsive
- [ ] State synchronization between UI and camera controller is perfect
- [ ] Both wheels maintain equal positioning and spacing

### 2. **Performance Requirements**
- [ ] Wheel scrolling maintains 60fps on target devices
- [ ] Haptic feedback timing is consistent and appropriate
- [ ] Memory usage remains stable during extended wheel interactions
- [ ] No frame drops during concurrent wheel operations

### 3. **Accessibility Requirements**
- [ ] Touch targets meet 48dp minimum size guidelines
- [ ] Gloved hand interaction success rate > 95%
- [ ] Content descriptions are accurate and helpful
- [ ] High contrast mode maintains visibility
- [ ] Screen reader compatibility verified

### 4. **Reliability Requirements**
- [ ] Zero crashes during wheel interactions
- [ ] State persistence across app lifecycle events
- [ ] Graceful handling of camera permission changes
- [ ] Stable performance under memory pressure
- [ ] Consistent behavior across device rotation

### 5. **User Experience Requirements**
- [ ] Intuitive wheel positioning and visual feedback
- [ ] Smooth transitions and animations
- [ ] Appropriate haptic feedback strength and timing
- [ ] Clear visual indication of selected items
- [ ] Responsive interaction with immediate feedback

## Test Reporting

### 1. **Automated Test Reports**
- Unit test coverage reports with line and branch coverage
- Integration test results with performance metrics
- UI test screenshots and interaction recordings
- Accessibility compliance reports
- Performance profiling data

### 2. **Manual Test Documentation**
- Device-specific test results matrix
- Environmental condition test results
- Construction site usage feedback
- User acceptance testing results
- Edge case scenario outcomes

### 3. **Defect Tracking**
- Critical: Issues that prevent core wheel functionality
- High: Performance issues or major UX problems
- Medium: Minor visual or interaction issues
- Low: Enhancement opportunities

This comprehensive testing strategy ensures that the camera wheel fixes and positioning improvements are thoroughly validated across all scenarios, providing construction workers with reliable and intuitive camera controls that work consistently in demanding field conditions.
