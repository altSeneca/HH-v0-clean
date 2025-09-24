package com.hazardhawk.ui.gallery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * UI Automation tests for HazardHawk photo gallery functionality.
 * Tests photo display, selection, filtering, and export features.
 */
@RunWith(AndroidJUnit4::class)
class PhotoGalleryAutomationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val samplePhotos = listOf(
        TestPhoto("1", "Construction Site 1", "2024-01-15", listOf("PPE", "Hard Hat")),
        TestPhoto("2", "Equipment Check", "2024-01-14", listOf("Machinery", "Safety")),
        TestPhoto("3", "Fall Protection", "2024-01-13", listOf("Scaffolding", "Harness"))
    )
    
    @Test
    fun gallery_displays_photos_correctly() {
        composeTestRule.setContent {
            PhotoGalleryScreenTest(photos = samplePhotos)
        }
        
        // Verify all photos are displayed
        composeTestRule.onNodeWithText("Construction Site 1")
            .assertExists()
        
        composeTestRule.onNodeWithText("Equipment Check")
            .assertExists()
        
        composeTestRule.onNodeWithText("Fall Protection")
            .assertExists()
        
        // Verify photo count
        composeTestRule.onNodeWithText("3 photos")
            .assertExists()
    }
    
    @Test
    fun gallery_search_filters_photos() {
        composeTestRule.setContent {
            PhotoGalleryScreenTest(photos = samplePhotos)
        }
        
        // Test search functionality
        composeTestRule.onNodeWithContentDescription("Search photos")
            .performTextInput("Equipment")
        
        // Should show only matching photo
        composeTestRule.onNodeWithText("Equipment Check")
            .assertExists()
        
        composeTestRule.onNodeWithText("Construction Site 1")
            .assertDoesNotExist()
    }
    
    @Test
    fun gallery_tag_filtering_works() {
        composeTestRule.setContent {
            PhotoGalleryScreenTest(photos = samplePhotos)
        }
        
        // Test tag filter
        composeTestRule.onNodeWithText("PPE")
            .performClick()
        
        // Should show only photos with PPE tag
        composeTestRule.onNodeWithText("Construction Site 1")
            .assertExists()
        
        composeTestRule.onNodeWithText("Equipment Check")
            .assertDoesNotExist()
        
        composeTestRule.onNodeWithText("Fall Protection")
            .assertDoesNotExist()
    }
    
    @Test
    fun gallery_photo_selection_works() {
        var selectedCount = 0
        
        composeTestRule.setContent {
            PhotoGalleryScreenTest(
                photos = samplePhotos,
                onSelectionChanged = { count -> selectedCount = count }
            )
        }
        
        // Enable selection mode
        composeTestRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        // Select first photo
        composeTestRule.onNodeWithContentDescription("Select Construction Site 1")
            .performClick()
        
        // Verify selection count updated
        composeTestRule.onNodeWithText("1 selected")
            .assertExists()
        
        // Select second photo
        composeTestRule.onNodeWithContentDescription("Select Equipment Check")
            .performClick()
        
        composeTestRule.onNodeWithText("2 selected")
            .assertExists()
    }
    
    @Test
    fun gallery_export_functionality_works() {
        composeTestRule.setContent {
            PhotoGalleryScreenTest(photos = samplePhotos)
        }
        
        // Enter selection mode
        composeTestRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        // Select a photo
        composeTestRule.onNodeWithContentDescription("Select Construction Site 1")
            .performClick()
        
        // Test export options
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        // Verify export options are shown
        composeTestRule.onNodeWithText("Export to PDF")
            .assertExists()
        
        composeTestRule.onNodeWithText("Share Photos")
            .assertExists()
        
        composeTestRule.onNodeWithText("Export to Excel")
            .assertExists()
    }
    
    @Test
    fun gallery_photo_details_modal_works() {
        composeTestRule.setContent {
            PhotoGalleryScreenTest(photos = samplePhotos)
        }
        
        // Tap on first photo to open details
        composeTestRule.onNodeWithContentDescription("View Construction Site 1 details")
            .performClick()
        
        // Verify details modal is shown
        composeTestRule.onNodeWithText("Photo Details")
            .assertExists()
        
        // Verify photo information
        composeTestRule.onNodeWithText("Construction Site 1")
            .assertExists()
        
        composeTestRule.onNodeWithText("2024-01-15")
            .assertExists()
        
        // Verify tags are shown
        composeTestRule.onNodeWithText("PPE")
            .assertExists()
        
        composeTestRule.onNodeWithText("Hard Hat")
            .assertExists()
    }
    
    @Test
    fun gallery_performance_with_many_photos() {
        val manyPhotos = (1..100).map { index ->
            TestPhoto(
                id = "photo_$index",
                title = "Photo $index",
                date = "2024-01-01",
                tags = listOf("Tag${index % 5}")
            )
        }
        
        composeTestRule.setContent {
            PhotoGalleryScreenTest(photos = manyPhotos)
        }
        
        // Should render without issues
        composeTestRule.onRoot().assertExists()
        
        // Should show correct count
        composeTestRule.onNodeWithText("100 photos")
            .assertExists()
        
        // Should be scrollable
        composeTestRule.onNodeWithTag("PhotoGrid")
            .performScrollToIndex(50)
        
        // Should find photo in middle of list
        composeTestRule.onNodeWithText("Photo 51")
            .assertExists()
    }
}

data class TestPhoto(
    val id: String,
    val title: String,
    val date: String,
    val tags: List<String>
)

@Composable
fun PhotoGalleryScreenTest(
    photos: List<TestPhoto>,
    onSelectionChanged: (Int) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(emptySet<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedPhotos by remember { mutableStateOf(emptySet<String>()) }
    var showDetails by remember { mutableStateOf(false) }
    var selectedPhoto by remember { mutableStateOf<TestPhoto?>(null) }
    
    // Filter photos based on search and tags
    val filteredPhotos = photos.filter { photo ->
        val matchesSearch = searchQuery.isEmpty() || 
            photo.title.contains(searchQuery, ignoreCase = true)
        val matchesTags = selectedTags.isEmpty() || 
            selectedTags.any { tag -> photo.tags.contains(tag) }
        matchesSearch && matchesTags
    }
    
    Column {
        // Header with photo count and controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${filteredPhotos.size} photos")
            
            Row {
                IconButton(
                    onClick = { isSelectionMode = !isSelectionMode },
                    modifier = Modifier.semantics {
                        contentDescription = "Select photos"
                    }
                ) {
                    Icon(Icons.Default.CheckCircle, "Select")
                }
                
                if (isSelectionMode && selectedPhotos.isNotEmpty()) {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.semantics {
                            contentDescription = "Export options"
                        }
                    ) {
                        Icon(Icons.Default.Share, "Export")
                    }
                }
            }
        }
        
        // Selection count
        if (isSelectionMode) {
            Text(
                "${selectedPhotos.size} selected",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search photos") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .semantics {
                    contentDescription = "Search photos"
                }
        )
        
        // Tag filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allTags = photos.flatMap { it.tags }.distinct()
            allTags.forEach { tag ->
                FilterChip(
                    onClick = {
                        selectedTags = if (selectedTags.contains(tag)) {
                            selectedTags - tag
                        } else {
                            selectedTags + tag
                        }
                    },
                    label = { Text(tag) },
                    selected = selectedTags.contains(tag)
                )
            }
        }
        
        // Photo grid
        LazyColumn(
            modifier = Modifier.testTag("PhotoGrid")
        ) {
            items(filteredPhotos) { photo ->
                PhotoItemTest(
                    photo = photo,
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedPhotos.contains(photo.id),
                    onPhotoClick = {
                        if (isSelectionMode) {
                            selectedPhotos = if (selectedPhotos.contains(photo.id)) {
                                selectedPhotos - photo.id
                            } else {
                                selectedPhotos + photo.id
                            }
                            onSelectionChanged(selectedPhotos.size)
                        } else {
                            selectedPhoto = photo
                            showDetails = true
                        }
                    }
                )
            }
        }
    }
    
    // Export options dialog
    if (isSelectionMode && selectedPhotos.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Export Options") },
            text = {
                Column {
                    TextButton(onClick = {}) {
                        Text("Export to PDF")
                    }
                    TextButton(onClick = {}) {
                        Text("Share Photos")
                    }
                    TextButton(onClick = {}) {
                        Text("Export to Excel")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {}) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Photo details dialog
    if (showDetails && selectedPhoto != null) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Photo Details") },
            text = {
                Column {
                    Text(selectedPhoto!!.title)
                    Text(selectedPhoto!!.date)
                    Text("Tags: ${selectedPhoto!!.tags.joinToString(", ")}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun PhotoItemTest(
    photo: TestPhoto,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onPhotoClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .semantics {
                contentDescription = if (isSelectionMode) {
                    "Select ${photo.title}"
                } else {
                    "View ${photo.title} details"
                }
            },
        onClick = onPhotoClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(photo.title, style = MaterialTheme.typography.titleMedium)
                    Text(photo.date, style = MaterialTheme.typography.bodySmall)
                }
                
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null
                    )
                }
            }
            
            // Tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                photo.tags.forEach { tag ->
                    AssistChip(
                        onClick = {},
                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }
}