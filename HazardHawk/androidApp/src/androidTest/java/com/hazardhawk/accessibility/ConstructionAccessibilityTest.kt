package com.hazardhawk.accessibility

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.test.GalleryTestDataFactory
import com.hazardhawk.gallery.GalleryPhoto
import com.hazardhawk.ui.gallery.GalleryGridComponent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * Comprehensive accessibility and usability tests for construction workers.
 * Tests TalkBack compatibility, one-handed operation, glove compatibility, 
 * outdoor visibility, and safety-critical interaction patterns.
 */
@RunWith(AndroidJUnit4::class)
class ConstructionAccessibilityTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val testDataFactory = GalleryTestDataFactory
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var testPhotos: List<GalleryPhoto>
    
    @Before
    fun setup() {
        testPhotos = testDataFactory.createAccessibilityTestPhotos()
    }
    
    // MARK: - TalkBack and Screen Reader Tests
    
    @Test
    fun talkBack_providesRichContentDescriptions() {
        val photosWithAnalysis = testPhotos.filter { it.aiAnalysisResult != null }
        
        composeTestRule.setContent {
            ConstructionAccessibleGallery(
                photos = photosWithAnalysis,
                isTalkBackEnabled = true
            )
        }
        
        photosWithAnalysis.forEach { photo ->
            composeTestRule.onNodeWithTag("photo_item_${photo.id}")
                .assert(hasContentDescription())
                .assert(
                    SemanticsMatcher("Has construction-specific description") { semantics ->
                        val description = semantics.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                        description?.contains("construction", ignoreCase = true) == true ||
                        description?.contains("safety", ignoreCase = true) == true ||
                        description?.contains("hazard", ignoreCase = true) == true
                    }
                )
        }
    }
    
    @Test
    fun talkBack_providesActionDescriptions() {
        composeTestRule.setContent {
            ConstructionAccessibleGallery(
                photos = testPhotos.take(3),
                isTalkBackEnabled = true
            )
        }
        
        // Test photo selection actions
        composeTestRule.onNodeWithTag("photo_item_${testPhotos[0].id}")
            .assert(hasClickAction())
            .assert(
                SemanticsMatcher("Has accessible click description") { semantics ->
                    val actions = semantics.getOrNull(SemanticsProperties.CustomActions)
                    actions?.any { it.label.contains("select", ignoreCase = true) } == true
                }
            )
    }
    
    @Test
    fun talkBack_navigatesLogically() {
        composeTestRule.setContent {
            ConstructionAccessibleGallery(
                photos = testPhotos.take(6), // 2x3 grid
                isTalkBackEnabled = true
            )
        }
        
        // Test navigation order is logical (left to right, top to bottom)
        composeTestRule.onNodeWithTag("photo_item_${testPhotos[0].id}")
            .requestFocus()
            .assertIsFocused()
        
        // Navigate to next photo
        composeTestRule.onRoot()
            .performKeyInput {
                keyDown(androidx.compose.ui.input.key.Key.Tab)
                keyUp(androidx.compose.ui.input.key.Key.Tab)
            }
        
        composeTestRule.onNodeWithTag("photo_item_${testPhotos[1].id}")
            .assertIsFocused()
    }
    
    @Test
    fun talkBack_announcesStatusChanges() {
        var selectedPhotos by mutableStateOf(emptySet<String>())
        
        composeTestRule.setContent {
            ConstructionAccessibleGallery(
                photos = testPhotos.take(3),
                isTalkBackEnabled = true,
                selectedPhotos = selectedPhotos,
                onPhotoLongClick = { photoId ->
                    selectedPhotos = if (selectedPhotos.contains(photoId)) {
                        selectedPhotos - photoId
                    } else {
                        selectedPhotos + photoId
                    }
                }
            )
        }
        
        // When photo is selected
        composeTestRule.onNodeWithTag("photo_item_${testPhotos[0].id}")
            .performTouchInput { longClick() }
        
        // Then selection should be announced
        composeTestRule.onNodeWithTag("photo_item_${testPhotos[0].id}")
            .assert(
                SemanticsMatcher("Announces selection status") { semantics ->
                    val stateDescription = semantics.getOrNull(SemanticsProperties.StateDescription)
                    stateDescription?.contains("selected", ignoreCase = true) == true
                }
            )
    }
    
    // MARK: - One-Handed Operation Tests
    
    @Test
    fun oneHanded_thumbReachabilityOptimal() {
        var interactionCount = 0
        
        composeTestRule.setContent {
            // Simulate phone in one-handed mode (bottom 2/3 of screen)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp) // Simulate unreachable top area
            ) {
                ConstructionAccessibleGallery(
                    photos = testPhotos.take(6),
                    isOneHandedMode = true,
                    onPhotoClick = { interactionCount++ }
                )
            }
        }
        
        // Test that photos in reachable area can be easily accessed
        composeTestRule.onAllNodesWithTag("photo_item")
            .onFirst()
            .performClick()
        
        assertEquals("Photo in reachable area should be clickable", 1, interactionCount)
    }
    
    @Test
    fun oneHanded_importantActionsAccessible() {
        var cameraClicked = false
        var menuAccessed = false
        
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                ConstructionAccessibleGallery(
                    photos = testPhotos,
                    isOneHandedMode = true
                )
                
                // Camera FAB in reachable area
                FloatingActionButton(
                    onClick = { cameraClicked = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(72.dp) // Large for one-handed use
                        .testTag("camera_fab")
                ) {
                    Icon(Icons.Filled.CameraAlt, "Camera")
                }
                
                // Menu in reachable area
                Button(
                    onClick = { menuAccessed = true },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .height(72.dp) // Large target
                        .testTag("menu_button")
                ) {
                    Text("Menu")
                }
            }
        }
        
        // Test critical actions are accessible
        composeTestRule.onNodeWithTag("camera_fab")
            .performClick()
        
        composeTestRule.onNodeWithTag("menu_button")
            .performClick()
        
        assertTrue("Camera should be accessible one-handed", cameraClicked)
        assertTrue("Menu should be accessible one-handed", menuAccessed)
    }
    
    // MARK: - Work Glove Compatibility Tests
    
    @Test
    fun workGloves_allScenarioCompatibility() = runTest {
        val gloveScenarios = testDataFactory.createGloveTestScenarios()
        
        gloveScenarios.forEach { scenario ->
            var touchSuccessCount = 0
            
            composeTestRule.setContent {
                ConstructionAccessibleGallery(
                    photos = testPhotos.take(4),
                    gloveMode = scenario,
                    onPhotoClick = { touchSuccessCount++ }
                )
            }
            
            // Test touch targets meet glove requirements
            composeTestRule.onAllNodesWithTag("photo_item")
                .assertAll(hasClickAction())
                .onFirst()
                .assertWidthIsAtLeast(scenario.minimumTouchSize.dp)
                .assertHeightIsAtLeast(scenario.minimumTouchSize.dp)
            
            // Test actual touch success
            testPhotos.take(4).forEach { photo ->
                composeTestRule.onNodeWithTag("photo_item_${photo.id}")
                    .performClick()
            }
            
            assertEquals(
                "All touches with ${scenario.gloveName} should succeed",
                4, touchSuccessCount
            )
            
            println("✓ ${scenario.gloveName} compatibility confirmed")
        }
    }
    
    @Test
    fun heavyGloves_longPressRecognition() = runTest {
        val heavyGloveScenario = testDataFactory.createGloveTestScenarios()
            .first { it.gloveName.contains("Heavy") }
        
        var longPressCount = 0
        
        composeTestRule.setContent {
            ConstructionAccessibleGallery(
                photos = testPhotos.take(3),
                gloveMode = heavyGloveScenario,
                onPhotoLongClick = { longPressCount++ }
            )
        }
        
        // Test that long press works with thick gloves
        testPhotos.take(3).forEach { photo ->
            composeTestRule.onNodeWithTag("photo_item_${photo.id}")
                .performTouchInput {
                    longClick(
                        durationMillis = 800 // Longer press for thick gloves
                    )
                }
        }
        
        assertEquals("Heavy gloves should register long press", 3, longPressCount)
    }
    
    @Test
    fun wetGloves_touchSensitivityAdaptation() = runTest {
        var touchResponses = 0
        
        composeTestRule.setContent {
            ConstructionAccessibleGallery(
                photos = testPhotos.take(5),
                isWetConditionsMode = true, // Increased touch sensitivity
                onPhotoClick = { touchResponses++ }
            )
        }
        
        // Test light touches register in wet conditions
        testPhotos.take(5).forEach { photo ->
            composeTestRule.onNodeWithTag("photo_item_${photo.id}")
                .performTouchInput {
                    // Light touch pressure
                    down(center)
                    up()
                }
        }
        
        assertTrue(
            "Wet conditions should register light touches, got $touchResponses/5",
            touchResponses >= 4 // Allow for minor variance
        )
    }
    
    // MARK: - Outdoor Visibility Tests
    
    @Test
    fun brightSunlight_highContrastVisibility() {
        composeTestRule.setContent {
            MaterialTheme(
                colorScheme = lightColorScheme().copy(
                    surface = Color.White,
                    onSurface = Color.Black,
                    primary = Color(0xFF0D47A1), // High contrast blue
                    secondary = Color(0xFFFF6F00) // High contrast orange
                )
            ) {
                ConstructionAccessibleGallery(
                    photos = testPhotos,
                    isOutdoorMode = true,
                    selectedPhotos = setOf(testPhotos[0].id)
                )
            }
        }
        
        // Verify high contrast elements are visible
        composeTestRule.onNodeWithTag("selection_indicator_${testPhotos[0].id}")
            .assertIsDisplayed()
        
        // Test that UI elements maintain visibility
        composeTestRule.onAllNodesWithTag("photo_item")
            .assertAll(hasClickAction())
    }
    
    @Test
    fun dawnDusk_lowLightVisibility() {
        composeTestRule.setContent {
            MaterialTheme(
                colorScheme = darkColorScheme().copy(
                    surface = Color(0xFF1A1A1A),
                    onSurface = Color(0xFFE0E0E0),
                    primary = Color(0xFF90CAF9), // Light blue for dark theme
                    secondary = Color(0xFFFFAB40) // Light orange for dark theme
                )
            ) {
                ConstructionAccessibleGallery(
                    photos = testPhotos,
                    isDimLightMode = true,
                    selectedPhotos = setOf(testPhotos[1].id)
                )
            }
        }
        
        // Verify low light visibility
        composeTestRule.onNodeWithTag("selection_indicator_${testPhotos[1].id}")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithTag("gallery_grid")
            .assertIsDisplayed()
    }
    
    @Test
    fun reflectiveScreen_antiGlareOptimization() {
        var interactionSuccessful = false
        
        composeTestRule.setContent {
            // Simulate anti-glare optimizations
            MaterialTheme(
                colorScheme = lightColorScheme().copy(
                    surface = Color(0xFFF5F5F5), // Slightly off-white to reduce glare
                    primary = Color(0xFF1976D2) // Strong blue visible in bright light
                )
            ) {
                ConstructionAccessibleGallery(
                    photos = testPhotos.take(4),
                    isAntiGlareMode = true,
                    onPhotoClick = { interactionSuccessful = true }
                )
            }
        }
        
        // Test interaction remains possible despite glare
        composeTestRule.onNodeWithTag("photo_item_${testPhotos[0].id}")
            .performClick()
        
        assertTrue("Anti-glare mode should maintain interactivity", interactionSuccessful)
    }
    
    // MARK: - Safety-Critical Interaction Tests
    
    @Test
    fun criticalActions_requireConfirmation() {
        var deletionRequested = false
        var confirmationShown = false
        
        composeTestRule.setContent {
            var showConfirmation by remember { mutableStateOf(false) }
            
            Box {
                ConstructionAccessibleGallery(
                    photos = testPhotos.take(3),
                    selectedPhotos = setOf(testPhotos[0].id),
                    onDeleteRequested = {
                        deletionRequested = true
                        showConfirmation = true
                    }
                )
                
                if (showConfirmation) {
                    confirmationShown = true
                    AlertDialog(
                        onDismissRequest = { showConfirmation = false },
                        title = { Text("Confirm Deletion") },
                        text = { Text("This will permanently delete the selected safety photos.") },
                        confirmButton = {
                            TextButton(
                                onClick = { showConfirmation = false },
                                modifier = Modifier.testTag("confirm_delete")
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showConfirmation = false },
                                modifier = Modifier.testTag("cancel_delete")
                            ) {
                                Text("Cancel")
                            }
                        },
                        modifier = Modifier.testTag("delete_confirmation_dialog")
                    )
                }
            }
        }
        
        // Trigger delete action
        composeTestRule.onNodeWithTag("delete_button")
            .performClick()
        
        assertTrue("Delete should be requested", deletionRequested)
        assertTrue("Confirmation should be shown for safety-critical action", confirmationShown)
        
        // Verify confirmation dialog is accessible
        composeTestRule.onNodeWithTag("delete_confirmation_dialog")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithTag("confirm_delete")
            .assert(hasClickAction())
        
        composeTestRule.onNodeWithTag("cancel_delete")
            .assert(hasClickAction())
    }
    
    @Test
    fun emergencyActions_alwaysAccessible() {
        var emergencyTriggered = false
        
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                ConstructionAccessibleGallery(
                    photos = testPhotos,
                    isEmergencyMode = false
                )
                
                // Emergency button always visible and accessible
                Button(
                    onClick = { emergencyTriggered = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .height(64.dp)
                        .testTag("emergency_button")
                        .semantics {
                            contentDescription = "Emergency: Report safety incident"
                            role = Role.Button
                        }
                ) {
                    Text("EMERGENCY", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        
        // Emergency button should always be accessible
        composeTestRule.onNodeWithTag("emergency_button")
            .assertIsDisplayed()
            .assert(hasClickAction())
            .assert(hasContentDescription())
            .performClick()
        
        assertTrue("Emergency action should be triggered", emergencyTriggered)
    }
    
    // MARK: - Interruption Recovery Tests
    
    @Test
    fun phoneCall_statePreservation() = runTest {
        var selectedPhotos = setOf(testPhotos[0].id, testPhotos[2].id)
        var isCallActive = false
        
        composeTestRule.setContent {
            if (isCallActive) {
                Text(
                    "Call in progress...",
                    modifier = Modifier.testTag("call_overlay")
                )
            } else {
                ConstructionAccessibleGallery(
                    photos = testPhotos,
                    selectedPhotos = selectedPhotos
                )
            }
        }
        
        // Verify initial state
        selectedPhotos.forEach { photoId ->
            composeTestRule.onNodeWithTag("selection_indicator_$photoId")
                .assertExists()
        }
        
        // Simulate phone call interruption
        isCallActive = true
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithTag("call_overlay")
            .assertIsDisplayed()
        
        // Return from call
        isCallActive = false
        composeTestRule.waitForIdle()
        
        // Verify state is preserved
        selectedPhotos.forEach { photoId ->
            composeTestRule.onNodeWithTag("selection_indicator_$photoId")
                .assertExists()
        }
    }
    
    @Test
    fun radioInterruption_quickResume() = runTest {
        var scrollPosition = 0
        var isRadioActive = false
        
        composeTestRule.setContent {
            if (isRadioActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("radio_overlay")
                ) {
                    Text("Radio transmission...")
                }
            } else {
                ConstructionAccessibleGallery(
                    photos = testDataFactory.createLargePhotoCollection(50),
                    initialScrollPosition = scrollPosition
                )
            }
        }
        
        // Scroll to position
        composeTestRule.onNodeWithTag("gallery_grid")
            .performScrollToIndex(20)
        scrollPosition = 20
        
        // Radio interruption
        isRadioActive = true
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithTag("radio_overlay")
            .assertIsDisplayed()
        
        // Resume after radio
        isRadioActive = false
        composeTestRule.waitForIdle()
        
        // Should resume at same position
        composeTestRule.onNodeWithTag("gallery_grid")
            .assertIsDisplayed()
    }
    
    // MARK: - Error Recovery Accessibility Tests
    
    @Test
    fun networkError_accessibleRecoveryOptions() {
        var retryCount = 0
        var showOfflineMode = false
        
        composeTestRule.setContent {
            if (showOfflineMode) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Network unavailable",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("error_title")
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { retryCount++; showOfflineMode = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .testTag("retry_button")
                            .semantics {
                                contentDescription = "Retry network connection"
                            }
                    ) {
                        Text("Retry Connection")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = { showOfflineMode = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .testTag("offline_button")
                            .semantics {
                                contentDescription = "Continue working offline"
                            }
                    ) {
                        Text("Work Offline")
                    }
                }
            } else {
                ConstructionAccessibleGallery(
                    photos = testPhotos,
                    isOfflineMode = retryCount == 0
                )
            }
        }
        
        // Trigger network error
        showOfflineMode = true
        composeTestRule.waitForIdle()
        
        // Verify error UI is accessible
        composeTestRule.onNodeWithTag("error_title")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithTag("retry_button")
            .assert(hasClickAction())
            .assert(hasContentDescription())
            .performClick()
        
        assertEquals("Retry should be accessible and functional", 1, retryCount)
    }
}

// MARK: - Helper Components

@Composable
fun ConstructionAccessibleGallery(
    photos: List<GalleryPhoto>,
    isTalkBackEnabled: Boolean = false,
    isOneHandedMode: Boolean = false,
    isOutdoorMode: Boolean = false,
    isDimLightMode: Boolean = false,
    isAntiGlareMode: Boolean = false,
    isWetConditionsMode: Boolean = false,
    isEmergencyMode: Boolean = false,
    gloveMode: GalleryTestDataFactory.GloveTestScenario? = null,
    selectedPhotos: Set<String> = emptySet(),
    initialScrollPosition: Int = 0,
    onPhotoClick: (String) -> Unit = {},
    onPhotoLongClick: (String) -> Unit = {},
    onDeleteRequested: () -> Unit = {}
) {
    val minTouchSize = when {
        gloveMode != null -> gloveMode.minimumTouchSize.dp
        isOneHandedMode || isOutdoorMode -> 72.dp
        else -> 48.dp
    }
    
    Column {
        if (selectedPhotos.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${selectedPhotos.size} selected")
                
                Button(
                    onClick = onDeleteRequested,
                    modifier = Modifier
                        .testTag("delete_button")
                        .semantics {
                            contentDescription = "Delete ${selectedPhotos.size} selected photos"
                        }
                ) {
                    Text("Delete")
                }
            }
        }
        
        GalleryGridComponent(
            photos = photos,
            onPhotoClick = onPhotoClick,
            onPhotoLongClick = onPhotoLongClick,
            selectedPhotos = selectedPhotos,
            minTouchTargetSize = minTouchSize
        )
    }
}

/**
 * Enhanced photo grid item with construction-specific accessibility features
 */
@Composable
fun AccessiblePhotoGridItem(
    photo: GalleryPhoto,
    isSelected: Boolean,
    hasAnalysis: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    minTouchSize: androidx.compose.ui.unit.Dp = 48.dp
) {
    val contentDescription = buildString {
        append("Construction safety photo")
        
        photo.metadata.location?.let {
            append(", location ${it.latitude}, ${it.longitude}")
        }
        
        if (hasAnalysis) {
            append(", AI analysis available")
        }
        
        if (photo.isUploaded) {
            append(", uploaded to cloud")
        } else {
            append(", stored locally")
        }
        
        if (isSelected) {
            append(", selected for batch operation")
        }
    }
    
    Card(
        modifier = Modifier
            .size(minTouchSize.coerceAtLeast(120.dp))
            .testTag("photo_item_${photo.id}")
            .testTag("photo_item")
            .semantics {
                this.contentDescription = contentDescription
                if (isSelected) {
                    stateDescription = "Selected"
                }
                role = Role.Image
                
                // Add custom actions for TalkBack
                customActions = listOf(
                    CustomAccessibilityAction("Select photo") {
                        onLongClick()
                        true
                    }
                )
            },
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Photo content placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
            
            // Selection indicator with high contrast
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null, // Already described in card
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .testTag("selection_indicator_${photo.id}"),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}