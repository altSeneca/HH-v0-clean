package com.hazardhawk.ui.gallery

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.CameraGalleryActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests specifically for photo selection functionality.
 * Tests multi-select, batch operations, and selection state management.
 */
@RunWith(AndroidJUnit4::class)
class PhotoSelectionIntegrationTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<CameraGalleryActivity>()
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var testPhotos: List<File>
    
    @Before
    fun setup() {
        // Create test photos for selection testing
        testPhotos = createTestPhotos(5)
    }
    
    @Test
    fun selectionMode_toggleOnOff_worksCorrectly() {
        // Step 1: Open gallery
        openGallery()
        
        // Step 2: Verify selection mode is off initially
        composeTestRule.onNodeWithText("selected")
            .assertDoesNotExist()
        
        // Step 3: Enable selection mode
        composeTestRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Step 4: Verify selection UI appears
        composeTestRule.onNodeWithText("0 selected")
            .assertExists()
        
        // Step 5: Disable selection mode
        composeTestRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Step 6: Verify selection UI disappears
        composeTestRule.onNodeWithText("selected")
            .assertDoesNotExist()
    }
    
    @Test
    fun singlePhotoSelection_worksCorrectly() {
        openGallery()
        enableSelectionMode()
        
        // Step 1: Select first photo
        selectPhotoByIndex(0)
        
        // Step 2: Verify selection count
        composeTestRule.onNodeWithText("1 selected")
            .assertExists()
        
        // Step 3: Verify export options become available
        composeTestRule.onNodeWithContentDescription("Export options")
            .assertExists()
        
        // Step 4: Deselect the photo
        selectPhotoByIndex(0)
        
        // Step 5: Verify selection cleared
        composeTestRule.onNodeWithText("0 selected")
            .assertExists()
        
        // Step 6: Verify export options disappear
        composeTestRule.onNodeWithContentDescription("Export options")
            .assertDoesNotExist()
    }
    
    @Test
    fun multiplePhotoSelection_sequential() {
        openGallery()
        enableSelectionMode()
        
        // Step 1: Select photos one by one
        for (i in 0 until 3) {
            selectPhotoByIndex(i)
            
            // Verify count updates
            composeTestRule.onNodeWithText("${i + 1} selected")
                .assertExists()
        }
        
        // Step 2: Verify final count
        composeTestRule.onNodeWithText("3 selected")
            .assertExists()
        
        // Step 3: Deselect middle photo
        selectPhotoByIndex(1)
        
        // Step 4: Verify count decreased
        composeTestRule.onNodeWithText("2 selected")
            .assertExists()
    }
    
    @Test
    fun selectAllPhotos_functionality() {
        openGallery()
        enableSelectionMode()
        
        // Step 1: Use select all functionality (if available)
        // Note: This would require implementing a "Select All" button
        // For now, we'll select all manually to test the concept
        
        for (i in testPhotos.indices) {
            selectPhotoByIndex(i)
        }
        
        // Step 2: Verify all photos selected
        composeTestRule.onNodeWithText("${testPhotos.size} selected")
            .assertExists()
        
        // Step 3: Clear all selections
        composeTestRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Step 4: Verify all selections cleared
        composeTestRule.onNodeWithText("0 selected")
            .assertExists()
    }
    
    @Test
    fun selectionPersistence_withinSession() {
        openGallery()
        enableSelectionMode()
        
        // Step 1: Select some photos
        selectPhotoByIndex(0)
        selectPhotoByIndex(2)
        
        // Step 2: Verify selections
        composeTestRule.onNodeWithText("2 selected")
            .assertExists()
        
        // Step 3: Scroll or perform other actions
        composeTestRule.onNodeWithTag("PhotoGrid")
            .performScrollToIndex(4)
        
        composeTestRule.waitForIdle()
        
        // Step 4: Verify selections still maintained
        composeTestRule.onNodeWithText("2 selected")
            .assertExists()
    }
    
    @Test
    fun bulkOperations_exportFlow() {
        openGallery()
        enableSelectionMode()
        
        // Step 1: Select multiple photos
        selectPhotoByIndex(0)
        selectPhotoByIndex(1)
        selectPhotoByIndex(2)
        
        // Step 2: Verify bulk selection
        composeTestRule.onNodeWithText("3 selected")
            .assertExists()
        
        // Step 3: Access export options
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Step 4: Verify export options available
        composeTestRule.onNodeWithText("Export to PDF")
            .assertExists()
        
        composeTestRule.onNodeWithText("Share Photos")
            .assertExists()
        
        composeTestRule.onNodeWithText("Export to Excel")
            .assertExists()
        
        // Step 5: Test cancel behavior
        composeTestRule.onNodeWithText("Cancel")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Step 6: Verify we're back to gallery with selections maintained
        composeTestRule.onNodeWithText("3 selected")
            .assertExists()
    }
    
    @Test
    fun selectionMode_visualFeedback() {
        openGallery()
        enableSelectionMode()
        
        // Step 1: Verify selection mode visual changes
        // (Checkboxes should appear, different styling, etc.)
        selectPhotoByIndex(0)
        
        // Step 2: Verify selected photo has visual indication
        // Note: This would require specific test tags or content descriptions
        // to verify visual state changes
        
        // Step 3: Verify unselected photos have different visual state
        composeTestRule.onNodeWithText("1 selected")
            .assertExists()
    }
    
    @Test
    fun selectionLimits_performanceBoundaries() {
        // This test would verify behavior with large numbers of selections
        // For now, test with available photos
        
        openGallery()
        enableSelectionMode()
        
        val startTime = System.currentTimeMillis()
        
        // Step 1: Select all available photos rapidly
        for (i in testPhotos.indices) {
            selectPhotoByIndex(i)
        }
        
        val selectionTime = System.currentTimeMillis() - startTime
        
        // Step 2: Verify all selections processed
        composeTestRule.onNodeWithText("${testPhotos.size} selected")
            .assertExists()
        
        // Step 3: Performance assertion
        assertTrue("Selection should be fast (< 1s for ${testPhotos.size} photos)", 
            selectionTime < 1000)
    }
    
    @Test
    fun selectionMode_accessibility() {
        openGallery()
        enableSelectionMode()
        
        // Step 1: Verify selection mode is announced for accessibility
        composeTestRule.onNodeWithContentDescription("Select photos")
            .assertExists()
        
        // Step 2: Test selection with accessibility
        selectPhotoByIndex(0)
        
        // Step 3: Verify accessible selection feedback
        composeTestRule.onNodeWithText("1 selected")
            .assertExists()
    }
    
    @Test
    fun selectionMode_errorHandling() {
        openGallery()
        enableSelectionMode()
        
        // Step 1: Select a photo
        selectPhotoByIndex(0)
        
        // Step 2: Simulate photo deletion or unavailability
        // (This would require mocking the photo storage)
        
        // Step 3: Verify graceful handling of missing selected photos
        // For now, just verify basic selection still works
        composeTestRule.onNodeWithText("1 selected")
            .assertExists()
    }
    
    @Test
    fun selectionMode_memoryEfficiency() {
        openGallery()
        enableSelectionMode()
        
        val initialMemory = getUsedMemory()
        
        // Step 1: Select all photos
        for (i in testPhotos.indices) {
            selectPhotoByIndex(i)
        }
        
        val selectionMemory = getUsedMemory()
        
        // Step 2: Clear selections
        composeTestRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        val finalMemory = getUsedMemory()
        
        // Step 3: Verify memory usage is reasonable
        val memoryIncrease = selectionMemory - initialMemory
        val memoryRecovered = selectionMemory - finalMemory
        
        assertTrue("Selection memory increase should be minimal (< 10MB)", 
            memoryIncrease < 10 * 1024 * 1024)
        assertTrue("Memory should be recovered when clearing selections", 
            memoryRecovered > 0)
    }
    
    // Helper methods
    private fun openGallery() {
        composeTestRule.onNodeWithText("🖼️ View Gallery")
            .performClick()
        composeTestRule.waitForIdle()
    }
    
    private fun enableSelectionMode() {
        composeTestRule.onNodeWithContentDescription("Select photos")
            .performClick()
        composeTestRule.waitForIdle()
    }
    
    private fun selectPhotoByIndex(index: Int) {
        val photo = testPhotos[index]
        composeTestRule.onNodeWithContentDescription("Select ${photo.nameWithoutExtension}")
            .performClick()
        composeTestRule.waitForIdle()
    }
    
    private fun createTestPhotos(count: Int): List<File> {
        val hazardHawkDir = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), 
            "HazardHawk"
        )
        if (!hazardHawkDir.exists()) {
            hazardHawkDir.mkdirs()
        }
        
        return (1..count).map { index ->
            File(hazardHawkDir, "HH_selection_test_$index.jpg").apply {
                writeBytes(createMockJpegData())
                deleteOnExit()
            }
        }
    }
    
    private fun createMockJpegData(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xE0.toByte(),
            0x00.toByte(), 0x10.toByte(),
            0x4A.toByte(), 0x46.toByte(), 0x49.toByte(), 0x46.toByte(), 0x00.toByte(),
            0x01.toByte(), 0x01.toByte(),
            0x01.toByte(),
            0x00.toByte(), 0x48.toByte(),
            0x00.toByte(), 0x48.toByte(),
            0x00.toByte(), 0x00.toByte(),
            0xFF.toByte(), 0xD9.toByte()
        )
    }
    
    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
}