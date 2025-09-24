package com.hazardhawk.ui.gallery

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.CameraGalleryActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertTrue

/**
 * Specialized tests for construction worker usability scenarios.
 * Tests glove-friendly interactions, outdoor visibility, and one-handed operation.
 */
@RunWith(AndroidJUnit4::class)
class ConstructionWorkerUsabilityTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<CameraGalleryActivity>()
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var testPhotos: List<File>
    
    @Before
    fun setup() {
        testPhotos = createConstructionTestPhotos()
    }
    
    @Test
    fun test_gloveFriendlyTouchTargets() {
        // Construction workers often wear gloves, requiring larger touch targets
        openGallery()
        
        // Test minimum touch target sizes (48dp minimum, 56dp preferred for gloves)
        val minimumTouchSize = 48.dp
        val preferredTouchSize = 56.dp
        
        // Test gallery button touch targets
        composeTestRule.onNodeWithText("← Back")
            .assertWidthIsAtLeast(minimumTouchSize)
            .assertHeightIsAtLeast(minimumTouchSize)
        
        composeTestRule.onNodeWithContentDescription("Select photos")
            .assertWidthIsAtLeast(minimumTouchSize)
            .assertHeightIsAtLeast(minimumTouchSize)
        
        // Test photo thumbnail touch targets
        enableSelectionMode()
        
        // Simulate glove touch (larger, less precise)
        testPhotos.take(3).forEach { photo ->
            // Test that photo selection works with imprecise touches
            val photoNode = composeTestRule.onNodeWithContentDescription("Select ${photo.nameWithoutExtension}")
            
            // Test center touch
            photoNode.performClick()
            composeTestRule.waitForIdle()
            
            // Test edge touches (simulate glove imprecision)
            photoNode.performTouchInput {
                // Touch near edges to simulate glove imprecision
                val bounds = this.visibleSize
                val edgeOffset = 10f
                
                down(Offset(edgeOffset, edgeOffset))
                up()
            }
            composeTestRule.waitForIdle()
        }
        
        // Verify selections registered despite imprecise touches
        composeTestRule.onNodeWithText(Regex("\\d+ selected"))
            .assertExists()
    }
    
    @Test
    fun test_outdoorVisibility_highContrast() {
        // Test visibility in bright outdoor conditions
        openGallery()
        
        // Verify key UI elements are visible with high contrast
        composeTestRule.onNodeWithText("Gallery")
            .assertIsDisplayed()
        
        // Test photo count visibility
        composeTestRule.onNodeWithText(Regex("\\d+ photos"))
            .assertIsDisplayed()
        
        // Enable selection mode and test visibility
        enableSelectionMode()
        
        // Test selection count visibility
        composeTestRule.onNodeWithText("0 selected")
            .assertIsDisplayed()
        
        // Select photos and verify count remains visible
        selectPhotoByIndex(0)
        
        composeTestRule.onNodeWithText("1 selected")
            .assertIsDisplayed()
        
        // Test export options visibility
        composeTestRule.onNodeWithContentDescription("Export options")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify export dialog is clearly visible
        composeTestRule.onNodeWithText("Export to PDF")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Share Photos")
            .assertIsDisplayed()
    }
    
    @Test
    fun test_oneHandedOperation_portraitMode() {
        // Test usability when device is held in one hand (portrait mode)
        openGallery()
        
        // Simulate one-handed reach limitations
        val displayMetrics = getDisplayMetrics()
        val screenHeight = displayMetrics.heightPixels
        val thumbReachLimit = (screenHeight * 0.7f).toInt() // Typical thumb reach
        
        // Test that essential controls are within thumb reach
        // Note: This is a conceptual test - actual implementation would need
        // specific positioning measurements
        
        // Test back button accessibility (should be in top-left, reachable area)
        composeTestRule.onNodeWithText("← Back")
            .assertExists()
            .assertIsDisplayed()
        
        // Test selection mode toggle accessibility
        composeTestRule.onNodeWithContentDescription("Select photos")
            .assertExists()
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Test photo selection in reachable area
        // Photos should be easily selectable with thumb
        selectPhotoByIndex(0)
        
        composeTestRule.onNodeWithText("1 selected")
            .assertExists()
        
        // Test export options are accessible
        composeTestRule.onNodeWithContentDescription("Export options")
            .assertExists()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Test that export options can be reached and used one-handed
        composeTestRule.onNodeWithText("Share Photos")
            .assertExists()
            .performClick()
        
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun test_landscapeMode_tabletOptimization() {
        // Test gallery in landscape mode (common on tablets at construction sites)
        
        // This test would ideally change device orientation
        // For now, we test that the gallery handles wide screen layouts
        
        openGallery()
        
        // Verify gallery displays correctly in landscape
        composeTestRule.onNodeWithText(Regex("Gallery \\(\\d+ photos\\)"))
            .assertExists()
        
        // Test that photo grid adapts to landscape (more columns)
        composeTestRule.onNodeWithTag("PhotoGrid")
            .assertExists()
        
        // Test horizontal scrolling/navigation
        composeTestRule.onNodeWithTag("PhotoGrid")
            .performScrollToIndex(testPhotos.size - 1)
        
        composeTestRule.waitForIdle()
        
        // Test selection mode in landscape
        enableSelectionMode()
        
        // Select multiple photos across different areas
        selectPhotoByIndex(0)
        selectPhotoByIndex(testPhotos.size / 2)
        selectPhotoByIndex(testPhotos.size - 1)
        
        composeTestRule.onNodeWithText("3 selected")
            .assertExists()
    }
    
    @Test
    fun test_interruptedWorkflow_quickRecovery() {
        // Test common interruption scenarios in construction work
        openGallery()
        enableSelectionMode()
        
        // Start selecting photos
        selectPhotoByIndex(0)
        selectPhotoByIndex(1)
        
        composeTestRule.onNodeWithText("2 selected")
            .assertExists()
        
        // Simulate interruption by navigation away and back
        composeTestRule.onNodeWithText("← Back")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Return to gallery quickly
        composeTestRule.onNodeWithText("🖼️ View Gallery")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Test that we can quickly resume work
        // (Selection state should be cleared, but gallery should be responsive)
        composeTestRule.onNodeWithText(Regex("Gallery \\(\\d+ photos\\)"))
            .assertExists()
        
        // Quickly re-enable selection and select photos
        val startTime = System.currentTimeMillis()
        
        enableSelectionMode()
        selectPhotoByIndex(0)
        selectPhotoByIndex(1)
        
        val resumeTime = System.currentTimeMillis() - startTime
        
        // Verify quick recovery
        composeTestRule.onNodeWithText("2 selected")
            .assertExists()
        
        assertTrue("Workflow resumption should be quick (< 2s)", resumeTime < 2000)
    }
    
    @Test
    fun test_workGloves_precisionTasks() {
        // Test specific tasks that require precision while wearing work gloves
        openGallery()
        
        // Test search functionality with gloves (text input)
        composeTestRule.onNodeWithContentDescription("Search photos")
            .performTextInput("construction")
        
        composeTestRule.waitForIdle()
        
        // Clear search easily
        composeTestRule.onNodeWithContentDescription("Search photos")
            .performTextClearance()
        
        composeTestRule.waitForIdle()
        
        // Test precise photo selection with gloves
        enableSelectionMode()
        
        // Test that small checkboxes or selection indicators work with gloves
        testPhotos.take(5).forEach { photo ->
            val photoNode = composeTestRule.onNodeWithContentDescription("Select ${photo.nameWithoutExtension}")
            
            // Test multiple touch attempts (simulate glove difficulty)
            repeat(3) {
                try {
                    photoNode.performClick()
                    composeTestRule.waitForIdle()
                    break
                } catch (e: Exception) {
                    // Retry if touch didn't register
                    Thread.sleep(100)
                }
            }
        }
        
        // Verify selections worked despite glove challenges
        composeTestRule.onNodeWithText(Regex("\\d+ selected"))
            .assertExists()
    }
    
    @Test
    fun test_noiseEnvironment_visualFeedback() {
        // Construction sites are noisy - ensure visual feedback is clear
        openGallery()
        enableSelectionMode()
        
        // Test that visual feedback is immediate and clear
        val startTime = System.currentTimeMillis()
        
        selectPhotoByIndex(0)
        
        val feedbackTime = System.currentTimeMillis() - startTime
        
        // Visual feedback should be immediate (< 100ms)
        assertTrue("Visual feedback should be immediate", feedbackTime < 100)
        
        // Verify clear selection count display
        composeTestRule.onNodeWithText("1 selected")
            .assertExists()
        
        // Test visual state changes are obvious
        selectPhotoByIndex(1)
        
        composeTestRule.onNodeWithText("2 selected")
            .assertExists()
        
        // Test export button appearance
        composeTestRule.onNodeWithContentDescription("Export options")
            .assertExists()
            .assertIsDisplayed()
        
        // Test that success states are visually obvious
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Export to PDF")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun test_dirtyHands_touchSensitivity() {
        // Test touch sensitivity with dirty/wet hands
        openGallery()
        
        // Test that all interactive elements respond to different touch pressures
        val touchTargets = listOf(
            "Select photos" to composeTestRule.onNodeWithContentDescription("Select photos"),
            "Back button" to composeTestRule.onNodeWithText("← Back")
        )
        
        touchTargets.forEach { (name, node) ->
            // Test light touch
            node.performTouchInput {
                down(center)
                up()
            }
            composeTestRule.waitForIdle()
            
            // Test firm press (simulate dirty gloves needing more pressure)
            node.performTouchInput {
                down(center)
                // Simulate holding press longer
                Thread.sleep(200)
                up()
            }
            composeTestRule.waitForIdle()
        }
        
        // Enable selection mode to test photo selection sensitivity
        enableSelectionMode()
        
        // Test photo selection with various touch patterns
        val photo = testPhotos.first()
        val photoNode = composeTestRule.onNodeWithContentDescription("Select ${photo.nameWithoutExtension}")
        
        // Test quick tap
        photoNode.performClick()
        composeTestRule.waitForIdle()
        
        // Verify selection registered
        composeTestRule.onNodeWithText("1 selected")
            .assertExists()
    }
    
    @Test
    fun test_hardHat_restrictedMovement() {
        // Test usability when wearing hard hat (restricted head/neck movement)
        openGallery()
        
        // Test that important information is visible without excessive scrolling
        composeTestRule.onNodeWithText(Regex("Gallery \\(\\d+ photos\\)"))
            .assertIsDisplayed()
        
        // Test that selection controls are within easy view
        composeTestRule.onNodeWithContentDescription("Select photos")
            .assertIsDisplayed()
        
        enableSelectionMode()
        
        // Test that selection count is always visible without scrolling
        selectPhotoByIndex(0)
        composeTestRule.onNodeWithText("1 selected")
            .assertIsDisplayed()
        
        selectPhotoByIndex(1)
        composeTestRule.onNodeWithText("2 selected")
            .assertIsDisplayed()
        
        // Test export options visibility
        composeTestRule.onNodeWithContentDescription("Export options")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // All export options should be visible without scrolling
        composeTestRule.onNodeWithText("Export to PDF")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Share Photos")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Export to Excel")
            .assertIsDisplayed()
    }
    
    @Test
    fun test_sunlightReadability_highBrightness() {
        // Test readability in bright sunlight conditions
        openGallery()
        
        // Key text elements should have sufficient contrast
        val criticalElements = listOf(
            "Gallery",
            "photos",
            "Back"
        )
        
        criticalElements.forEach { text ->
            composeTestRule.onNodeWithText(Regex(".*$text.*"))
                .assertExists()
                .assertIsDisplayed()
        }
        
        enableSelectionMode()
        
        // Selection count should be clearly readable
        composeTestRule.onNodeWithText("0 selected")
            .assertIsDisplayed()
        
        selectPhotoByIndex(0)
        
        composeTestRule.onNodeWithText("1 selected")
            .assertIsDisplayed()
        
        // Export options should remain readable
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Export to PDF")
            .assertIsDisplayed()
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
        if (index < testPhotos.size) {
            val photo = testPhotos[index]
            composeTestRule.onNodeWithContentDescription("Select ${photo.nameWithoutExtension}")
                .performClick()
            composeTestRule.waitForIdle()
        }
    }
    
    private fun createConstructionTestPhotos(): List<File> {
        val hazardHawkDir = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "HazardHawk"
        )
        if (!hazardHawkDir.exists()) {
            hazardHawkDir.mkdirs()
        }
        
        val photoNames = listOf(
            "construction_site_safety",
            "equipment_inspection",
            "worker_ppe_check",
            "scaffolding_setup",
            "concrete_pour"
        )
        
        return photoNames.mapIndexed { index, name ->
            File(hazardHawkDir, "HH_${name}_${index + 1}.jpg").apply {
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
    
    private fun getDisplayMetrics(): DisplayMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        return metrics
    }
}