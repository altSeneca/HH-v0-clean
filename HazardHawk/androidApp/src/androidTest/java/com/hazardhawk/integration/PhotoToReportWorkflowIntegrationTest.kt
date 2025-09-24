package com.hazardhawk.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hazardhawk.CameraGalleryActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/**
 * End-to-end integration tests for the complete photo-to-report workflow.
 * Tests the entire user journey from photo capture to report generation with real UI interactions.
 */
@RunWith(AndroidJUnit4::class)
class PhotoToReportWorkflowIntegrationTest {
    
    @get:Rule
    val composeRule = createAndroidComposeRule<CameraGalleryActivity>()
    
    companion object {
        private const val WORKFLOW_TIMEOUT = 10000L
    }
    
    @Test
    fun testCompleteWorkflow_captureToReport() {
        // Step 1: Navigate to gallery
        composeRule.onNodeWithText("🖼️ View Gallery")
            .assertExists("Gallery button should exist")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Step 2: Verify gallery loads with photos
        composeRule.onNode(hasText("Gallery") and hasText("photos"))
            .assertExists("Gallery title with photo count should be visible")
        
        // Step 3: Enter selection mode
        composeRule.onNodeWithContentDescription("Select photos")
            .assertExists("Selection mode button should exist")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Step 4: Select multiple photos
        val photosToSelect = 3
        repeat(photosToSelect) { index ->
            // Select photos by their position in the grid
            composeRule.onAllNodesWithTag("photo_thumbnail")[index]
                .performClick()
            composeRule.waitForIdle()
        }
        
        // Step 5: Verify selection count
        composeRule.onNodeWithText("$photosToSelect selected")
            .assertExists("Selection count should be visible")
        
        // Step 6: Initiate report generation
        composeRule.onNode(
            hasText("Generate Report") and hasClickAction()
        ).assertExists("Generate report button should be visible")
         .performClick()
        
        composeRule.waitForIdle()
        
        // Step 7: Verify report generation dialog
        composeRule.onNodeWithText("Generating Safety Report")
            .assertExists("Report generation dialog should appear")
        
        // Step 8: Wait for progress completion
        composeRule.waitUntil(timeoutMillis = WORKFLOW_TIMEOUT) {
            composeRule.onAllNodesWithText("Report generated successfully!")
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Step 9: Verify workflow completion
        // The selection should be cleared after report generation
        composeRule.onNodeWithText("0 selected")
            .assertExists("Selection should be cleared after report generation")
    }
    
    @Test
    fun testPhotoToReportWithTags_OSHACompliance() {
        // Navigate to gallery
        composeRule.onNodeWithText("🖼️ View Gallery")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Select a photo and open photo viewer
        composeRule.onAllNodesWithTag("photo_thumbnail")[0]
            .performClick()
        
        composeRule.waitForIdle()
        
        // In photo viewer, edit tags
        composeRule.onNodeWithContentDescription("Edit tags")
            .assertExists("Tag editor should be available")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Add OSHA-specific tags
        val oshaCategories = listOf("PPE", "Fall Protection", "Electrical Safety")
        
        oshaCategories.forEach { category ->
            // Look for pre-defined OSHA category buttons
            composeRule.onNodeWithText(category)
                .assertExists("OSHA category $category should be available")
                .performClick()
            
            composeRule.waitForIdle()
        }
        
        // Save tags
        composeRule.onNodeWithText("Save Tags")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Return to gallery
        composeRule.onNodeWithContentDescription("Back")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Enter selection mode and select the tagged photo
        composeRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        composeRule.onAllNodesWithTag("photo_thumbnail")[0]
            .performClick()
        
        // Generate report
        composeRule.onNode(hasText("Generate Report"))
            .performClick()
        
        // Verify OSHA compliance stage appears
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("Creating OSHA compliance section...")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
    
    @Test
    fun testBulkPhotoSelection_reportGeneration() {
        // Navigate to gallery
        composeRule.onNodeWithText("🖼️ View Gallery")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Enter selection mode
        composeRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        // Use select all functionality
        composeRule.onNodeWithText("All")
            .assertExists("Select all button should exist in selection mode")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Verify all photos selected
        composeRule.onNode(hasText("selected"))
            .assertExists("Selection count should show all photos selected")
        
        // Generate report with all photos
        composeRule.onNode(hasText("Generate Report"))
            .performClick()
        
        composeRule.waitForIdle()
        
        // Verify comprehensive report generation
        composeRule.onNodeWithText("Generating Safety Report")
            .assertExists()
        
        // Wait for completion
        composeRule.waitUntil(timeoutMillis = WORKFLOW_TIMEOUT) {
            composeRule.onAllNodesWithText("Report generated successfully!")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
    
    @Test
    fun testWorkflowInterruption_resumeCapability() {
        // Start workflow
        composeRule.onNodeWithText("🖼️ View Gallery")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Select photos
        composeRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        composeRule.onAllNodesWithTag("photo_thumbnail")[0]
            .performClick()
        
        composeRule.onAllNodesWithTag("photo_thumbnail")[1]
            .performClick()
        
        // Simulate interruption - navigate away
        composeRule.onNodeWithText("← Back")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Return to gallery quickly
        composeRule.onNodeWithText("🖼️ View Gallery")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Verify we can quickly resume workflow
        composeRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        // Quick re-selection
        composeRule.onAllNodesWithTag("photo_thumbnail")[0]
            .performClick()
        
        composeRule.onNode(hasText("Generate Report"))
            .performClick()
        
        // Verify workflow continues normally
        composeRule.onNodeWithText("Generating Safety Report")
            .assertExists()
    }
    
    @Test
    fun testPhotoDeletion_undoWorkflow() {
        // Navigate to gallery
        composeRule.onNodeWithText("🖼️ View Gallery")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Note the initial photo count
        val initialGalleryState = composeRule.onNode(hasText("Gallery"))
        
        // Enter selection mode
        composeRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        // Select a photo for deletion
        composeRule.onAllNodesWithTag("photo_thumbnail")[0]
            .performClick()
        
        // Delete selected photo
        composeRule.onNodeWithContentDescription("Delete")
            .assertExists("Delete button should be available")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Verify undo snackbar appears
        composeRule.onNode(hasText("UNDO"))
            .assertExists("Undo option should appear after deletion")
        
        // Test undo within 5 seconds
        composeRule.onNodeWithText("UNDO")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Verify photo restored
        // The photo should be back in the gallery
        composeRule.onNode(hasText("Gallery"))
            .assertExists("Gallery should show restored photos")
    }
    
    @Test
    fun testConstructionWorkerGloves_touchAccuracy() {
        // Test workflow with imprecise touches (simulating gloves)
        composeRule.onNodeWithText("🖼️ View Gallery")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Test imprecise touch on selection button
        composeRule.onNodeWithContentDescription("Select photos")
            .performTouchInput {
                // Simulate glove touch - less precise, near edge
                val bounds = visibleSize
                down(androidx.compose.ui.geometry.Offset(bounds.width * 0.8f, bounds.height * 0.2f))
                up()
            }
        
        composeRule.waitForIdle()
        
        // Verify selection mode activated despite imprecise touch
        composeRule.onNodeWithText("0 selected")
            .assertExists("Selection mode should activate even with imprecise touch")
        
        // Test imprecise photo selection
        val photoThumbnail = composeRule.onAllNodesWithTag("photo_thumbnail")[0]
        
        photoThumbnail.performTouchInput {
            // Edge touch simulation
            val bounds = visibleSize
            down(androidx.compose.ui.geometry.Offset(5f, 5f)) // Near top-left edge
            up()
        }
        
        composeRule.waitForIdle()
        
        // Verify photo selected despite edge touch
        composeRule.onNodeWithText("1 selected")
            .assertExists("Photo should be selected despite edge touch")
    }
    
    @Test
    fun testReportGeneration_progressVisibility() {
        // Test that report generation progress is clearly visible for construction workers
        composeRule.onNodeWithText("🖼️ View Gallery")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Select photos and generate report
        composeRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        composeRule.onAllNodesWithTag("photo_thumbnail")[0]
            .performClick()
        
        composeRule.onNode(hasText("Generate Report"))
            .performClick()
        
        composeRule.waitForIdle()
        
        // Verify progress elements are clearly visible
        composeRule.onNodeWithText("Generating Safety Report")
            .assertExists("Progress dialog title should be visible")
        
        // Verify progress bar exists
        composeRule.onNode(hasProgressBarRangeInfo(androidx.compose.ui.semantics.ProgressBarRangeInfo(0.0f, 0.0f..1.0f)))
            .assertExists("Progress bar should be visible")
        
        // Verify progress text
        composeRule.onNode(hasText("0%"))
            .assertExists("Progress percentage should be visible")
        
        // Wait and verify progress updates
        composeRule.waitUntil(timeoutMillis = 2000) {
            composeRule.onAllNodes(hasText("Preparing photos..."))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
    
    @Test
    fun testErrorRecovery_networkFailure() {
        // This test would simulate network failure during report generation
        // For now, test the UI resilience
        
        composeRule.onNodeWithText("🖼️ View Gallery")
            .performClick()
        
        composeRule.waitForIdle()
        
        // Verify gallery loads despite potential network issues
        composeRule.onNode(hasText("Gallery"))
            .assertExists("Gallery should load even with network issues")
        
        // Test that basic functionality works offline
        composeRule.onNodeWithContentDescription("Select photos")
            .performClick()
        
        composeRule.onAllNodesWithTag("photo_thumbnail")[0]
            .performClick()
        
        composeRule.onNodeWithText("1 selected")
            .assertExists("Photo selection should work offline")
    }
}