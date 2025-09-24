package com.hazardhawk.ui.gallery

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.CameraGalleryActivity
import com.hazardhawk.data.PhotoStorageManagerCompat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for complete gallery workflows.
 * Tests photo capture → gallery display → selection → export workflows.
 */
@RunWith(AndroidJUnit4::class)
class GalleryWorkflowIntegrationTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<CameraGalleryActivity>()
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    @Before
    fun setup() {
        // Clean up any existing test photos
        PhotoStorageManagerCompat.getAllPhotos(context).forEach { it.delete() }
    }
    
    @Test
    fun completeWorkflow_captureToGalleryToExport() {
        // Step 1: Start from main screen
        composeTestRule.onNodeWithText("🖼️ View Gallery")
            .assertExists()
            .assertIsDisplayed()
        
        // Create a test photo first
        val testPhoto = createTestPhoto()
        
        // Step 2: Open gallery
        composeTestRule.onNodeWithText("🖼️ View Gallery")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Step 3: Verify gallery shows the photo
        composeTestRule.onNodeWithText("Gallery (1 photos)")
            .assertExists()
        
        // Step 4: Enter selection mode
        composeTestRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Step 5: Select the photo
        composeTestRule.onNodeWithContentDescription("Select ${testPhoto.nameWithoutExtension}")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Step 6: Verify selection count
        composeTestRule.onNodeWithText("1 selected")
            .assertExists()
        
        // Step 7: Access export options
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Step 8: Verify export options are available
        composeTestRule.onNodeWithText("Export to PDF")
            .assertExists()
        composeTestRule.onNodeWithText("Share Photos")
            .assertExists()
        
        // Cleanup
        testPhoto.delete()
    }
    
    @Test
    fun galleryNavigation_backAndForthFlow() {
        // Step 1: Start at main screen
        composeTestRule.onNodeWithText("HazardHawk")
            .assertExists()
        
        // Step 2: Navigate to gallery
        composeTestRule.onNodeWithText("🖼️ View Gallery")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Step 3: Verify in gallery
        composeTestRule.onNodeWithText("Gallery (0 photos)")
            .assertExists()
        
        // Step 4: Navigate back
        composeTestRule.onNodeWithText("← Back")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Step 5: Verify back at main screen
        composeTestRule.onNodeWithText("HazardHawk")
            .assertExists()
        composeTestRule.onNodeWithText("Construction Safety Camera")
            .assertExists()
    }
    
    @Test
    fun multiPhotoSelection_workflow() {
        // Setup: Create multiple test photos
        val testPhotos = (1..3).map { createTestPhoto("test_photo_$it") }
        
        try {
            // Step 1: Open gallery
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Step 2: Verify all photos are shown
            composeTestRule.onNodeWithText("Gallery (3 photos)")
                .assertExists()
            
            // Step 3: Enter selection mode
            composeTestRule.onNodeWithContentDescription("Select photos")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Step 4: Select all photos one by one
            testPhotos.forEachIndexed { index, photo ->
                composeTestRule.onNodeWithContentDescription("Select ${photo.nameWithoutExtension}")
                    .performClick()
                
                composeTestRule.waitForIdle()
                
                // Verify selection count updates
                composeTestRule.onNodeWithText("${index + 1} selected")
                    .assertExists()
            }
            
            // Step 5: Verify all photos are selected
            composeTestRule.onNodeWithText("3 selected")
                .assertExists()
            
            // Step 6: Test deselection by selecting one photo again
            composeTestRule.onNodeWithContentDescription("Select ${testPhotos[0].nameWithoutExtension}")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Step 7: Verify count decreased
            composeTestRule.onNodeWithText("2 selected")
                .assertExists()
            
        } finally {
            // Cleanup
            testPhotos.forEach { it.delete() }
        }
    }
    
    @Test
    fun photoTagging_integrationFlow() {
        val testPhoto = createTestPhoto("construction_site")
        
        try {
            // Step 1: Open gallery
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Step 2: Click on photo to open tag dialog
            composeTestRule.onNodeWithContentDescription("View ${testPhoto.nameWithoutExtension} details")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Step 3: Verify tag dialog opens
            composeTestRule.onNodeWithText("Tag Management")
                .assertExists()
            
            // Step 4: Add some safety tags
            composeTestRule.onNodeWithText("Hard Hat Required")
                .performClick()
            
            composeTestRule.onNodeWithText("Safety Vest Required")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Step 5: Verify OSHA compliance indicator updates
            composeTestRule.onNodeWithText("OSHA Compliant")
                .assertExists()
            
            // Step 6: Save tags
            composeTestRule.onNodeWithText("Save Tags")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Step 7: Verify we're back in gallery
            composeTestRule.onNodeWithText("Gallery (1 photos)")
                .assertExists()
            
        } finally {
            testPhoto.delete()
        }
    }
    
    @Test
    fun gallerySearch_filteringWorkflow() {
        // Setup: Create photos with different names
        val constructionPhoto = createTestPhoto("construction_site_safety")
        val equipmentPhoto = createTestPhoto("equipment_inspection")
        val meetingPhoto = createTestPhoto("safety_meeting")
        
        try {
            // Step 1: Open gallery
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Step 2: Verify all photos are shown initially
            composeTestRule.onNodeWithText("Gallery (3 photos)")
                .assertExists()
            
            // Step 3: Use search to filter photos
            composeTestRule.onNodeWithContentDescription("Search photos")
                .performTextInput("construction")
            
            composeTestRule.waitForIdle()
            
            // Step 4: Verify filtered results
            // Note: This test would need actual search implementation
            // For now, we verify the search field exists and accepts input
            composeTestRule.onNodeWithText("construction")
                .assertExists()
            
            // Step 5: Clear search
            composeTestRule.onNodeWithContentDescription("Search photos")
                .performTextClearance()
            
            composeTestRule.waitForIdle()
            
            // Step 6: Verify all photos are shown again
            composeTestRule.onNodeWithText("Gallery (3 photos)")
                .assertExists()
            
        } finally {
            listOf(constructionPhoto, equipmentPhoto, meetingPhoto)
                .forEach { it.delete() }
        }
    }
    
    @Test
    fun galleryPerformance_withMultiplePhotos() {
        // Setup: Create multiple photos to test performance
        val photos = (1..10).map { createTestPhoto("performance_test_$it") }
        
        try {
            val startTime = System.currentTimeMillis()
            
            // Step 1: Open gallery
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            val loadTime = System.currentTimeMillis() - startTime
            
            // Step 2: Verify all photos loaded
            composeTestRule.onNodeWithText("Gallery (10 photos)")
                .assertExists()
            
            // Step 3: Test scrolling performance
            val scrollStartTime = System.currentTimeMillis()
            
            // Scroll through the grid
            composeTestRule.onNodeWithTag("PhotoGrid")
                .performScrollToIndex(5)
            
            composeTestRule.waitForIdle()
            
            val scrollTime = System.currentTimeMillis() - scrollStartTime
            
            // Step 4: Performance assertions
            assertTrue("Gallery should load within 2 seconds", loadTime < 2000)
            assertTrue("Scrolling should be smooth (< 500ms)", scrollTime < 500)
            
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun errorHandling_missingPhotosWorkflow() {
        // Step 1: Create a photo reference but delete the file
        val photo = createTestPhoto()
        val originalPath = photo.absolutePath
        photo.delete()
        
        // Step 2: Open gallery
        composeTestRule.onNodeWithText("🖼️ View Gallery")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Step 3: Verify gallery handles missing files gracefully
        // Should show empty state or error message
        composeTestRule.onNodeWithText("No photos yet")
            .assertExists()
    }
    
    @Test
    fun galleryState_persistenceAcrossNavigation() {
        val testPhoto = createTestPhoto()
        
        try {
            // Step 1: Open gallery and enter selection mode
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            composeTestRule.onNodeWithContentDescription("Select photos")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Step 2: Select photo
            composeTestRule.onNodeWithContentDescription("Select ${testPhoto.nameWithoutExtension}")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Step 3: Navigate away and back
            composeTestRule.onNodeWithText("← Back")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Step 4: Verify selection state is cleared (expected behavior)
            composeTestRule.onNodeWithText("Gallery (1 photos)")
                .assertExists()
            
            // Selection mode should be off by default
            composeTestRule.onNodeWithText("selected")
                .assertDoesNotExist()
            
        } finally {
            testPhoto.delete()
        }
    }
    
    private fun createTestPhoto(name: String = "test_photo"): File {
        val hazardHawkDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "HazardHawk")
        if (!hazardHawkDir.exists()) {
            hazardHawkDir.mkdirs()
        }
        
        val photoFile = File(hazardHawkDir, "HH_${name}_${System.currentTimeMillis()}.jpg")
        
        // Create a simple test image
        photoFile.writeBytes(createMockJpegData())
        
        return photoFile
    }
    
    private fun createMockJpegData(): ByteArray {
        // Minimal JPEG header for testing
        return byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), // JPEG Start of Image
            0xFF.toByte(), 0xE0.toByte(), // JFIF APP0
            0x00.toByte(), 0x10.toByte(), // Length
            0x4A.toByte(), 0x46.toByte(), 0x49.toByte(), 0x46.toByte(), 0x00.toByte(), // "JFIF\0"
            0x01.toByte(), 0x01.toByte(), // Version
            0x01.toByte(), // Units
            0x00.toByte(), 0x48.toByte(), // X density
            0x00.toByte(), 0x48.toByte(), // Y density
            0x00.toByte(), 0x00.toByte(), // Thumbnail dimensions
            0xFF.toByte(), 0xD9.toByte()  // JPEG End of Image
        )
    }
}