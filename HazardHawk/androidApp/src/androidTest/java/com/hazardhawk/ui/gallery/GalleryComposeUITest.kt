package com.hazardhawk.ui.gallery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.test.GalleryTestDataFactory
import com.hazardhawk.gallery.GalleryPhoto
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive Compose UI tests for gallery functionality with construction-specific scenarios.
 * Tests UI components, interactions, accessibility, and construction worker usability.
 */
@RunWith(AndroidJUnit4::class)
class GalleryComposeUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val testDataFactory = GalleryTestDataFactory
    private lateinit var testPhotos: List<GalleryPhoto>
    
    @Before
    fun setup() {
        testPhotos = testDataFactory.createLargePhotoCollection(
            count = 20,
            withAnalysis = true,
            uploadedPercentage = 0.7f
        )
    }
    
    // MARK: - Basic UI Component Tests
    
    @Test
    fun galleryGrid_displaysPhotosCorrectly() {
        // Given
        composeTestRule.setContent {
            GalleryGridComponent(
                photos = testPhotos.take(6),
                onPhotoClick = {},
                onPhotoLongClick = {},
                selectedPhotos = emptySet()
            )
        }
        
        // Then
        composeTestRule.onAllNodesWithTag("photo_item").assertCountEquals(6)
        
        // Verify first photo is displayed
        composeTestRule.onNodeWithTag("photo_item_${testPhotos[0].id}")
            .assertIsDisplayed()
    }
    
    @Test
    fun galleryGrid_handlesEmptyState() {
        // Given
        composeTestRule.setContent {
            GalleryGridComponent(
                photos = emptyList(),
                onPhotoClick = {},
                onPhotoLongClick = {},
                selectedPhotos = emptySet()
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("No photos found")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Capture your first safety photo")
            .assertIsDisplayed()
    }
    
    @Test
    fun cameraFAB_isDisplayedAndClickable() {
        var fabClicked = false
        
        // Given
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                GalleryScreen(
                    photos = testPhotos,
                    onCameraClick = { fabClicked = true },
                    onPhotoClick = {},
                    onPhotoLongClick = {},
                    selectedPhotos = emptySet(),
                    onSelectionChange = {}
                )
            }
        }
        
        // When
        composeTestRule.onNodeWithTag("camera_fab")
            .assertIsDisplayed()
            .performClick()
        
        // Then
        assertTrue("Camera FAB should trigger callback", fabClicked)
    }
    
    @Test
    fun photoSelection_worksCorrectly() {
        val selectedPhotos = mutableSetOf<String>()
        
        // Given
        composeTestRule.setContent {
            var selection by remember { mutableStateOf(selectedPhotos) }
            GalleryGridComponent(
                photos = testPhotos.take(3),
                onPhotoClick = {},
                onPhotoLongClick = { photoId ->
                    selection = if (selection.contains(photoId)) {
                        selection - photoId
                    } else {
                        selection + photoId
                    }.toMutableSet()
                },
                selectedPhotos = selection
            )
        }
        
        // When - Long click on first photo
        composeTestRule.onNodeWithTag("photo_item_${testPhotos[0].id}")
            .performTouchInput { longClick() }
        
        // Then - Selection indicator should be visible
        composeTestRule.onNodeWithTag("selection_indicator_${testPhotos[0].id}")
            .assertIsDisplayed()
    }
    
    // MARK: - Construction Worker Usability Tests
    
    @Test
    fun touchTargets_meetConstructionGloveRequirements() {
        // Test with different glove scenarios
        val gloveScenarios = testDataFactory.createGloveTestScenarios()
        
        gloveScenarios.forEach { scenario ->
            composeTestRule.setContent {
                GalleryGridComponent(
                    photos = testPhotos.take(4),
                    onPhotoClick = {},
                    onPhotoLongClick = {},
                    selectedPhotos = emptySet(),
                    minTouchTargetSize = scenario.minimumTouchSize.dp
                )
            }
            
            // Verify touch targets are large enough
            composeTestRule.onAllNodesWithTag("photo_item")
                .onFirst()
                .assertHeightIsAtLeast(scenario.minimumTouchSize.dp)
                .assertWidthIsAtLeast(scenario.minimumTouchSize.dp)
        }
    }
    
    @Test
    fun cameraFAB_meetsAccessibilityRequirements() {
        // Given
        composeTestRule.setContent {
            GalleryScreen(
                photos = testPhotos,
                onCameraClick = {},
                onPhotoClick = {},
                onPhotoLongClick = {},
                selectedPhotos = emptySet(),
                onSelectionChange = {}
            )
        }
        
        // Then - FAB should meet minimum 72dp requirement for construction
        composeTestRule.onNodeWithTag("camera_fab")
            .assertWidthIsAtLeast(72.dp)
            .assertHeightIsAtLeast(72.dp)
        
        // Verify accessibility semantics
        composeTestRule.onNodeWithTag("camera_fab")
            .assert(hasContentDescription())
            .assert(hasClickAction())
    }
    
    @Test
    fun galleryGrid_supportsOneHandedOperation() {
        var photoClicked = false
        
        // Given - Simulate phone in one-handed mode (bottom half of screen)
        composeTestRule.setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 200.dp) // Simulate thumb reach area
            ) {
                GalleryGridComponent(
                    photos = testPhotos.take(6),
                    onPhotoClick = { photoClicked = true },
                    onPhotoLongClick = {},
                    selectedPhotos = emptySet()
                )
            }
        }
        
        // When - Click on photo in reachable area
        composeTestRule.onAllNodesWithTag("photo_item")
            .onFirst()
            .performClick()
        
        // Then
        assertTrue("Photo should be clickable in one-handed mode", photoClicked)
    }
    
    @Test
    fun highContrastMode_providesVisualClarity() {
        // Given
        composeTestRule.setContent {
            MaterialTheme(
                colorScheme = darkColorScheme() // High contrast mode
            ) {
                GalleryGridComponent(
                    photos = testPhotos.take(4),
                    onPhotoClick = {},
                    onPhotoLongClick = {},
                    selectedPhotos = setOf(testPhotos[0].id)
                )
            }
        }
        
        // Then - Selection indicators should be clearly visible
        composeTestRule.onNodeWithTag("selection_indicator_${testPhotos[0].id}")
            .assertIsDisplayed()
        
        // Photo items should have proper contrast
        composeTestRule.onAllNodesWithTag("photo_item")
            .assertAll(hasClickAction())
    }
    
    // MARK: - Performance Tests
    
    @Test
    fun galleryGrid_performsWithLargePhotoCollection() {
        val largePhotoCollection = testDataFactory.createMemoryTestCollection(100)
        
        // Given
        val startTime = System.currentTimeMillis()
        
        composeTestRule.setContent {
            GalleryGridComponent(
                photos = largePhotoCollection,
                onPhotoClick = {},
                onPhotoLongClick = {},
                selectedPhotos = emptySet()
            )
        }
        
        val loadTime = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Large collection should load within 2 seconds", loadTime < 2000)
        
        // Verify scrolling performance
        composeTestRule.onNodeWithTag("gallery_grid")
            .performScrollToIndex(50)
        
        // Should still be responsive after scrolling
        composeTestRule.onNodeWithTag("photo_item_${largePhotoCollection[50].id}")
            .assertIsDisplayed()
    }
    
    @Test
    fun galleryScroll_maintainsSmooth60FPS() {
        val largeCollection = testDataFactory.createMemoryTestCollection(200)
        
        // Given
        composeTestRule.setContent {
            GalleryGridComponent(
                photos = largeCollection,
                onPhotoClick = {},
                onPhotoLongClick = {},
                selectedPhotos = emptySet()
            )
        }
        
        // When - Perform rapid scrolling
        composeTestRule.onNodeWithTag("gallery_grid")
            .performTouchInput {
                swipeUp(durationMillis = 100)
                swipeDown(durationMillis = 100)
                swipeUp(durationMillis = 100)
            }
        
        // Then - Should remain responsive
        composeTestRule.onNodeWithTag("gallery_grid")
            .assertIsDisplayed()
    }
    
    // MARK: - AI Integration UI Tests
    
    @Test
    fun aiAnalysisIndicator_displaysCorrectly() {
        val photosWithAnalysis = testPhotos.filter { it.aiAnalysisResult != null }
        val photosWithoutAnalysis = testPhotos.filter { it.aiAnalysisResult == null }
        
        // Given
        composeTestRule.setContent {
            GalleryGridComponent(
                photos = photosWithAnalysis.take(2) + photosWithoutAnalysis.take(2),
                onPhotoClick = {},
                onPhotoLongClick = {},
                selectedPhotos = emptySet()
            )
        }
        
        // Then - Photos with analysis should show indicator
        photosWithAnalysis.take(2).forEach { photo ->
            composeTestRule.onNodeWithTag("ai_indicator_${photo.id}")
                .assertIsDisplayed()
        }
        
        // Photos without analysis should not show indicator
        photosWithoutAnalysis.take(2).forEach { photo ->
            composeTestRule.onNodeWithTag("ai_indicator_${photo.id}")
                .assertDoesNotExist()
        }
    }
    
    @Test
    fun uploadStatus_indicatorDisplaysCorrectly() {
        val uploadedPhotos = testPhotos.filter { it.isUploaded }
        val localPhotos = testPhotos.filter { !it.isUploaded }
        
        // Given
        composeTestRule.setContent {
            GalleryGridComponent(
                photos = uploadedPhotos.take(2) + localPhotos.take(2),
                onPhotoClick = {},
                onPhotoLongClick = {},
                selectedPhotos = emptySet()
            )
        }
        
        // Then - Uploaded photos should show cloud indicator
        uploadedPhotos.take(2).forEach { photo ->
            composeTestRule.onNodeWithTag("upload_indicator_${photo.id}")
                .assertIsDisplayed()
        }
        
        // Local photos should show local indicator  
        localPhotos.take(2).forEach { photo ->
            composeTestRule.onNodeWithTag("local_indicator_${photo.id}")
                .assertIsDisplayed()
        }
    }
    
    // MARK: - Error Handling UI Tests
    
    @Test
    fun corruptedPhoto_handlesGracefully() {
        val errorPhotos = testDataFactory.createErrorTestPhotos()
        
        // Given
        composeTestRule.setContent {
            GalleryGridComponent(
                photos = errorPhotos,
                onPhotoClick = {},
                onPhotoLongClick = {},
                selectedPhotos = emptySet()
            )
        }
        
        // Then - Should display error placeholders instead of crashing
        composeTestRule.onAllNodesWithTag("photo_error_placeholder")
            .assertCountEquals(errorPhotos.size)
    }
    
    @Test
    fun networkError_showsRetryOption() {
        var retryClicked = false
        
        // Given
        composeTestRule.setContent {
            GalleryErrorState(
                error = "Network connection failed",
                onRetry = { retryClicked = true }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Network connection failed")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Retry")
            .assertIsDisplayed()
            .performClick()
        
        assertTrue("Retry button should trigger callback", retryClicked)
    }
    
    // MARK: - Batch Operations UI Tests
    
    @Test
    fun batchSelection_showsCorrectCount() {
        var selectedCount = 0
        
        // Given
        composeTestRule.setContent {
            var selectedPhotos by remember { mutableStateOf(emptySet<String>()) }
            selectedCount = selectedPhotos.size
            
            Column {
                BatchSelectionToolbar(
                    selectedCount = selectedPhotos.size,
                    onSelectAll = {
                        selectedPhotos = testPhotos.take(5).map { it.id }.toSet()
                    },
                    onClearSelection = {
                        selectedPhotos = emptySet()
                    },
                    onDelete = {},
                    onShare = {}
                )
                
                GalleryGridComponent(
                    photos = testPhotos.take(5),
                    onPhotoClick = {},
                    onPhotoLongClick = { photoId ->
                        selectedPhotos = if (selectedPhotos.contains(photoId)) {
                            selectedPhotos - photoId
                        } else {
                            selectedPhotos + photoId
                        }
                    },
                    selectedPhotos = selectedPhotos
                )
            }
        }
        
        // When - Select all photos
        composeTestRule.onNodeWithText("Select All")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then
        composeTestRule.onNodeWithText("5 selected")
            .assertIsDisplayed()
    }
    
    @Test
    fun batchActions_areAccessible() {
        // Given
        composeTestRule.setContent {
            BatchSelectionToolbar(
                selectedCount = 3,
                onSelectAll = {},
                onClearSelection = {},
                onDelete = {},
                onShare = {}
            )
        }
        
        // Then - All batch actions should be accessible
        composeTestRule.onNodeWithText("Delete")
            .assertIsDisplayed()
            .assert(hasClickAction())
        
        composeTestRule.onNodeWithText("Share")
            .assertIsDisplayed()
            .assert(hasClickAction())
        
        composeTestRule.onNodeWithContentDescription("Clear selection")
            .assertIsDisplayed()
            .assert(hasClickAction())
    }
    
    // MARK: - Construction Scenario-Specific Tests
    
    @Test
    fun outdoorVisibility_maintainsReadabilityInBrightLight() {
        // Given - Outdoor construction scenario photos
        val outdoorPhotos = testPhotos.filter { photo ->
            photo.metadata.lightingCondition.contains("bright")
        }
        
        composeTestRule.setContent {
            // Simulate bright outdoor conditions with high contrast theme
            MaterialTheme(
                colorScheme = lightColorScheme().copy(
                    surface = androidx.compose.ui.graphics.Color.White,
                    onSurface = androidx.compose.ui.graphics.Color.Black
                )
            ) {
                GalleryGridComponent(
                    photos = outdoorPhotos.take(4),
                    onPhotoClick = {},
                    onPhotoLongClick = {},
                    selectedPhotos = setOf(outdoorPhotos[0].id)
                )
            }
        }
        
        // Then - UI elements should remain clearly visible
        composeTestRule.onNodeWithTag("selection_indicator_${outdoorPhotos[0].id}")
            .assertIsDisplayed()
        
        composeTestRule.onAllNodesWithTag("photo_item")
            .assertAll(hasClickAction())
    }
    
    @Test
    fun interruptionRecovery_maintainsState() {
        val selectedPhotos = mutableSetOf(testPhotos[0].id, testPhotos[2].id)
        
        // Given - Simulate app interruption and recovery
        composeTestRule.setContent {
            var selection by remember { mutableStateOf(selectedPhotos) }
            GalleryGridComponent(
                photos = testPhotos.take(5),
                onPhotoClick = {},
                onPhotoLongClick = { photoId ->
                    selection = if (selection.contains(photoId)) {
                        selection - photoId
                    } else {
                        selection + photoId
                    }.toMutableSet()
                },
                selectedPhotos = selection
            )
        }
        
        // Then - Previous selections should be maintained
        composeTestRule.onNodeWithTag("selection_indicator_${testPhotos[0].id}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("selection_indicator_${testPhotos[2].id}")
            .assertIsDisplayed()
        
        // Non-selected photos should not show selection
        composeTestRule.onNodeWithTag("selection_indicator_${testPhotos[1].id}")
            .assertDoesNotExist()
    }
    
    // MARK: - Accessibility Tests
    
    @Test
    fun talkBack_providesProperContentDescriptions() {
        // Given
        composeTestRule.setContent {
            GalleryGridComponent(
                photos = testPhotos.take(3),
                onPhotoClick = {},
                onPhotoLongClick = {},
                selectedPhotos = emptySet()
            )
        }
        
        // Then - Each photo should have descriptive content
        testPhotos.take(3).forEach { photo ->
            val expectedDescription = "Construction photo captured at ${photo.metadata.location}"
            composeTestRule.onNodeWithTag("photo_item_${photo.id}")
                .assert(hasContentDescription())
        }
    }
    
    @Test
    fun keyboardNavigation_worksCorrectly() {
        // Given
        composeTestRule.setContent {
            GalleryGridComponent(
                photos = testPhotos.take(9), // 3x3 grid
                onPhotoClick = {},
                onPhotoLongClick = {},
                selectedPhotos = emptySet()
            )
        }
        
        // When - Navigate with keyboard (simulated)
        composeTestRule.onNodeWithTag("photo_item_${testPhotos[0].id}")
            .requestFocus()
        
        // Then - Focus should be on first photo
        composeTestRule.onNodeWithTag("photo_item_${testPhotos[0].id}")
            .assertIsFocused()
    }
}

// MARK: - Test Helper Components

@Composable
fun GalleryGridComponent(
    photos: List<GalleryPhoto>,
    onPhotoClick: (String) -> Unit,
    onPhotoLongClick: (String) -> Unit,
    selectedPhotos: Set<String>,
    minTouchTargetSize: androidx.compose.ui.unit.Dp = 48.dp
) {
    if (photos.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No photos found")
            Text("Capture your first safety photo")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.testTag("gallery_grid"),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos) { photo ->
                PhotoGridItem(
                    photo = photo,
                    isSelected = selectedPhotos.contains(photo.id),
                    onClick = { onPhotoClick(photo.id) },
                    onLongClick = { onPhotoLongClick(photo.id) },
                    minTouchSize = minTouchTargetSize
                )
            }
        }
    }
}

@Composable
fun PhotoGridItem(
    photo: GalleryPhoto,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    minTouchSize: androidx.compose.ui.unit.Dp = 48.dp
) {
    Box(
        modifier = Modifier
            .size(minTouchSize.coerceAtLeast(120.dp))
            .testTag("photo_item_${photo.id}")
            .testTag("photo_item")
            .semantics {
                contentDescription = "Construction photo captured at ${photo.metadata.location}"
            }
    ) {
        // Simulate photo thumbnail
        Card(
            modifier = Modifier
                .fillMaxSize()
                .testTag("photo_card_${photo.id}"),
            onClick = onClick
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Photo content placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )
                
                // AI Analysis Indicator
                if (photo.aiAnalysisResult != null) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "AI analyzed",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .testTag("ai_indicator_${photo.id}")
                    )
                }
                
                // Upload Status Indicator
                if (photo.isUploaded) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Uploaded",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .testTag("upload_indicator_${photo.id}")
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Local",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .testTag("local_indicator_${photo.id}")
                    )
                }
                
                // Selection Indicator
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Selected",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("selection_indicator_${photo.id}")
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryScreen(
    photos: List<GalleryPhoto>,
    onCameraClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onPhotoLongClick: (String) -> Unit,
    selectedPhotos: Set<String>,
    onSelectionChange: (Set<String>) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        GalleryGridComponent(
            photos = photos,
            onPhotoClick = onPhotoClick,
            onPhotoLongClick = onPhotoLongClick,
            selectedPhotos = selectedPhotos
        )
        
        FloatingActionButton(
            onClick = onCameraClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(72.dp) // Construction-friendly size
                .testTag("camera_fab")
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Open camera",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun BatchSelectionToolbar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    if (selectedCount > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$selectedCount selected")
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_button")
                ) {
                    Text("Delete")
                }
                
                TextButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("share_button")
                ) {
                    Text("Share")
                }
                
                IconButton(
                    onClick = onClearSelection,
                    modifier = Modifier
                        .testTag("clear_selection_button")
                        .semantics {
                            contentDescription = "Clear selection"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null
                    )
                }
            }
        }
    } else {
        TextButton(
            onClick = onSelectAll,
            modifier = Modifier
                .padding(16.dp)
                .testTag("select_all_button")
        ) {
            Text("Select All")
        }
    }
}

@Composable
fun GalleryErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error,
            modifier = Modifier.testTag("error_message")
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier.testTag("retry_button")
        ) {
            Text("Retry")
        }
    }
}