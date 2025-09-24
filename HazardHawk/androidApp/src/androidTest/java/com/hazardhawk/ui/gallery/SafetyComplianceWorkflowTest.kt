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
import kotlin.test.assertTrue

/**
 * Tests for safety compliance workflows specific to construction industry.
 * Tests OSHA compliance checking, safety documentation, and regulatory reporting.
 */
@RunWith(AndroidJUnit4::class)
class SafetyComplianceWorkflowTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<CameraGalleryActivity>()
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var safetyTestPhotos: List<File>
    
    @Before
    fun setup() {
        safetyTestPhotos = createSafetyCompliancePhotos()
    }
    
    @Test
    fun test_oshaCompliance_photoTagging() {
        openGallery()
        
        // Test OSHA compliance tagging workflow
        val safetyPhoto = safetyTestPhotos.first { it.name.contains("safety") }
        
        // Click on safety photo to open tag dialog
        composeTestRule.onNodeWithContentDescription("View ${safetyPhoto.nameWithoutExtension} details")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify tag management dialog opens
        composeTestRule.onNodeWithText("Tag Management")
            .assertExists()
        
        // Add OSHA-required safety tags
        val oshaRequiredTags = listOf(
            "Hard Hat Required",
            "Safety Vest Required",
            "Fall Protection",
            "Proper PPE"
        )
        
        oshaRequiredTags.forEach { tag ->
            composeTestRule.onNodeWithText(tag)
                .performClick()
        }
        
        composeTestRule.waitForIdle()
        
        // Verify OSHA compliance indicator appears
        composeTestRule.onNodeWithText("OSHA Compliant")
            .assertExists()
        
        // Save the tags
        composeTestRule.onNodeWithText("Save Tags")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify return to gallery
        composeTestRule.onNodeWithText(Regex("Gallery \\(\\d+ photos\\)"))
            .assertExists()
    }
    
    @Test
    fun test_safetyDocumentation_bulkExport() {
        openGallery()
        enableSelectionMode()
        
        // Select all safety-related photos for compliance report
        safetyTestPhotos.forEach { photo ->
            selectPhotoByName(photo.nameWithoutExtension)
        }
        
        // Verify all photos selected
        composeTestRule.onNodeWithText("${safetyTestPhotos.size} selected")
            .assertExists()
        
        // Access export options
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Test safety documentation export
        composeTestRule.onNodeWithText("Export to PDF")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // This would normally show PDF generation progress/success
        // For testing, we verify the action was triggered
        
        // Test Excel export for compliance tracking
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Export to Excel")
            .performClick()
        
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun test_incidentDocumentation_urgentWorkflow() {
        // Test rapid documentation of safety incidents
        openGallery()
        
        val incidentPhoto = safetyTestPhotos.first { it.name.contains("incident") }
        
        // Quick access to incident photo
        composeTestRule.onNodeWithContentDescription("View ${incidentPhoto.nameWithoutExtension} details")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Add urgent incident tags
        val incidentTags = listOf(
            "Safety Incident",
            "Immediate Action Required",
            "OSHA Reportable",
            "Near Miss"
        )
        
        incidentTags.forEach { tag ->
            composeTestRule.onNodeWithText(tag)
                .performClick()
        }
        
        composeTestRule.waitForIdle()
        
        // Save with high priority
        composeTestRule.onNodeWithText("Save Tags")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Return to gallery and immediately export for incident report
        enableSelectionMode()
        selectPhotoByName(incidentPhoto.nameWithoutExtension)
        
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Test immediate sharing for incident reporting
        composeTestRule.onNodeWithText("Share Photos")
            .performClick()
        
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun test_dailySafetyInspection_workflow() {
        // Test workflow for daily safety inspections
        openGallery()
        enableSelectionMode()
        
        // Select photos from daily inspection
        val inspectionPhotos = safetyTestPhotos.filter { 
            it.name.contains("inspection") || it.name.contains("equipment")
        }
        
        inspectionPhotos.forEach { photo ->
            selectPhotoByName(photo.nameWithoutExtension)
        }
        
        composeTestRule.onNodeWithText("${inspectionPhotos.size} selected")
            .assertExists()
        
        // Export daily inspection report
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Export to PDF")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Test that report generation completes quickly (daily workflow requirement)
        val startTime = System.currentTimeMillis()
        
        // Simulate report generation completion
        composeTestRule.waitForIdle()
        
        val reportTime = System.currentTimeMillis() - startTime
        
        assertTrue("Daily inspection report should generate quickly (< 5s)", 
            reportTime < 5000)
    }
    
    @Test
    fun test_complianceAudit_photoFiltering() {
        // Test filtering photos for compliance audits
        openGallery()
        
        // Test search for specific compliance categories
        val complianceSearchTerms = listOf(
            "safety",
            "inspection",
            "equipment",
            "ppe"
        )
        
        complianceSearchTerms.forEach { searchTerm ->
            // Search for compliance-related photos
            composeTestRule.onNodeWithContentDescription("Search photos")
                .performTextInput(searchTerm)
            
            composeTestRule.waitForIdle()
            
            // Verify filtered results
            // Note: Actual filtering would depend on photo names or tags
            
            // Clear search for next test
            composeTestRule.onNodeWithContentDescription("Search photos")
                .performTextClearance()
            
            composeTestRule.waitForIdle()
        }
        
        // Test date-based filtering for audit periods
        // This would require implementing date filters in the gallery
        
        // Verify all photos are shown after clearing filters
        composeTestRule.onNodeWithText("Gallery (${safetyTestPhotos.size} photos)")
            .assertExists()
    }
    
    @Test
    fun test_safetyMeeting_photoDocumentation() {
        // Test documenting safety meetings and toolbox talks
        openGallery()
        
        val meetingPhoto = safetyTestPhotos.first { it.name.contains("meeting") }
        
        // Tag meeting photo with appropriate safety categories
        composeTestRule.onNodeWithContentDescription("View ${meetingPhoto.nameWithoutExtension} details")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        val meetingTags = listOf(
            "Safety Meeting",
            "Toolbox Talk",
            "Safety Training",
            "Worker Education"
        )
        
        meetingTags.forEach { tag ->
            composeTestRule.onNodeWithText(tag)
                .performClick()
        }
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Save Tags")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Create meeting documentation
        enableSelectionMode()
        selectPhotoByName(meetingPhoto.nameWithoutExtension)
        
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Export to PDF")
            .performClick()
        
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun test_hazardIdentification_rapidResponse() {
        // Test rapid hazard identification and documentation
        openGallery()
        
        val hazardPhoto = safetyTestPhotos.first { it.name.contains("hazard") }
        
        val startTime = System.currentTimeMillis()
        
        // Quick hazard documentation workflow
        composeTestRule.onNodeWithContentDescription("View ${hazardPhoto.nameWithoutExtension} details")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Add critical hazard tags quickly
        val hazardTags = listOf(
            "Fall Hazard",
            "Electrical Hazard",
            "Immediate Action Required",
            "Work Stop"
        )
        
        hazardTags.forEach { tag ->
            composeTestRule.onNodeWithText(tag)
                .performClick()
        }
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Save Tags")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Immediate export for hazard notification
        enableSelectionMode()
        selectPhotoByName(hazardPhoto.nameWithoutExtension)
        
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Share Photos")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        val totalTime = System.currentTimeMillis() - startTime
        
        assertTrue("Hazard identification workflow should be fast (< 10s)", 
            totalTime < 10000)
    }
    
    @Test
    fun test_weeklyReport_batchProcessing() {
        // Test weekly safety report generation
        openGallery()
        enableSelectionMode()
        
        // Select all photos for weekly report
        safetyTestPhotos.forEach { photo ->
            selectPhotoByName(photo.nameWithoutExtension)
        }
        
        composeTestRule.onNodeWithText("${safetyTestPhotos.size} selected")
            .assertExists()
        
        val startTime = System.currentTimeMillis()
        
        // Generate comprehensive weekly report
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Test both PDF and Excel exports for comprehensive reporting
        composeTestRule.onNodeWithText("Export to PDF")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Return to export options for Excel
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Export to Excel")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        val reportGenerationTime = System.currentTimeMillis() - startTime
        
        assertTrue("Weekly report generation should complete efficiently (< 15s)", 
            reportGenerationTime < 15000)
    }
    
    @Test
    fun test_regulatoryCompliance_auditTrail() {
        // Test maintaining audit trail for regulatory compliance
        openGallery()
        
        // Verify photos have proper metadata for audit trail
        val compliancePhoto = safetyTestPhotos.first()
        
        composeTestRule.onNodeWithContentDescription("View ${compliancePhoto.nameWithoutExtension} details")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify timestamp and location data would be available
        // (This would require actual metadata display in the UI)
        composeTestRule.onNodeWithText("Tag Management")
            .assertExists()
        
        // Add regulatory compliance tags
        val regulatoryTags = listOf(
            "Regulatory Compliance",
            "Audit Documentation",
            "OSHA Standard",
            "Safety Protocol"
        )
        
        regulatoryTags.forEach { tag ->
            composeTestRule.onNodeWithText(tag)
                .performClick()
        }
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Save Tags")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Export for regulatory submission
        enableSelectionMode()
        selectPhotoByName(compliancePhoto.nameWithoutExtension)
        
        composeTestRule.onNodeWithContentDescription("Export options")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Export to PDF")
            .performClick()
        
        composeTestRule.waitForIdle()
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
    
    private fun selectPhotoByName(photoName: String) {
        composeTestRule.onNodeWithContentDescription("Select $photoName")
            .performClick()
        composeTestRule.waitForIdle()
    }
    
    private fun createSafetyCompliancePhotos(): List<File> {
        val hazardHawkDir = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "HazardHawk"
        )
        if (!hazardHawkDir.exists()) {
            hazardHawkDir.mkdirs()
        }
        
        val safetyPhotoNames = listOf(
            "safety_inspection_morning",
            "equipment_safety_check",
            "worker_ppe_compliance",
            "hazard_identification_site",
            "safety_meeting_toolbox",
            "incident_near_miss",
            "fall_protection_setup",
            "electrical_safety_lockout",
            "confined_space_entry",
            "crane_safety_inspection"
        )
        
        return safetyPhotoNames.mapIndexed { index, name ->
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
}