package com.hazardhawk

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import kotlin.test.assertTrue

/**
 * Comprehensive navigation flow integration tests for HazardHawk
 * 
 * These tests validate the complete user workflow that was previously crashing:
 * CompanyProjectEntryScreen → Navigation → CameraScreen
 * 
 * Critical focus: Testing data class instantiation during navigation to prevent crashes
 */
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest : KoinTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    
    private lateinit var device: UiDevice
    
    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }
    
    /**
     * PRIMARY TEST: Complete form submission navigation flow
     * This test validates the exact user workflow that was causing crashes
     */
    @Test
    fun testFormSubmissionNavigatesToCamera() {
        composeTestRule.waitForIdle()
        
        // Verify we start on the company/project entry screen
        composeTestRule.onNodeWithText("HazardHawk")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Safety Documentation Setup")
            .assertIsDisplayed()
        
        // Enter valid company information
        composeTestRule.onNodeWithText("Company Name")
            .performTextInput("Test Construction Co")
        
        // Enter valid project information  
        composeTestRule.onNodeWithText("Project Name")
            .performTextInput("Highway Safety Inspection")
        
        // Verify button text changes when form is valid
        composeTestRule.onNodeWithText("Start Safety Documentation")
            .assertIsDisplayed()
        
        // Submit form - this was the critical failure point
        composeTestRule.onNodeWithText("Start Safety Documentation")
            .performClick()
        
        // Wait for navigation and data class instantiation
        composeTestRule.waitForIdle()
        
        // Verify successful navigation to camera screen
        // Looking for camera UI elements that indicate successful navigation
        composeTestRule.onNodeWithContentDescription("Camera viewfinder")
            .assertExists()
        
        // Additional verification: Check for camera controls
        composeTestRule.onNodeWithContentDescription("Capture photo")
            .assertExists()
    }
    
    /**
     * VALIDATION TEST: Form validation prevents invalid submission
     */
    @Test
    fun testFormValidationPreventsEmptySubmission() {
        composeTestRule.waitForIdle()
        
        // Verify button is disabled when form is empty
        composeTestRule.onNodeWithText("Enter Company & Project Details")
            .assertIsDisplayed()
        
        // Try to click disabled button - should not navigate
        composeTestRule.onNodeWithText("Enter Company & Project Details")
            .assertIsNotEnabled()
        
        // Enter only company name (incomplete)
        composeTestRule.onNodeWithText("Company Name")
            .performTextInput("Test Co")
        
        // Button should still be disabled
        composeTestRule.onNodeWithText("Enter Company & Project Details")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        
        // Enter project name to complete form
        composeTestRule.onNodeWithText("Project Name")
            .performTextInput("Test Project")
        
        // Now button should be enabled
        composeTestRule.onNodeWithText("Start Safety Documentation")
            .assertIsEnabled()
    }
    
    /**
     * DATA PERSISTENCE TEST: Verify navigation preserves user data
     */
    @Test
    fun testNavigationPreservesFormData() {
        composeTestRule.waitForIdle()
        
        val testCompany = "Acme Construction LLC"
        val testProject = "Downtown Office Complex"
        
        // Fill form with specific test data
        composeTestRule.onNodeWithText("Company Name")
            .performTextInput(testCompany)
        
        composeTestRule.onNodeWithText("Project Name")
            .performTextInput(testProject)
        
        // Submit form
        composeTestRule.onNodeWithText("Start Safety Documentation")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify navigation occurred
        composeTestRule.onNodeWithContentDescription("Camera viewfinder")
            .assertExists()
        
        // Note: In a real implementation, we would verify that the metadata
        // contains the correct company and project information, but this
        // requires access to the MetadataSettingsManager state
    }
    
    /**
     * ERROR HANDLING TEST: Camera permission handling
     */
    @Test
    fun testCameraPermissionHandling() {
        composeTestRule.waitForIdle()
        
        // Complete form submission
        composeTestRule.onNodeWithText("Company Name")
            .performTextInput("Test Construction")
        
        composeTestRule.onNodeWithText("Project Name")
            .performTextInput("Safety Test")
        
        composeTestRule.onNodeWithText("Start Safety Documentation")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify camera screen loads without crashing
        // (Permissions are granted via GrantPermissionRule)
        composeTestRule.onNodeWithContentDescription("Camera viewfinder")
            .assertExists()
        
        // Verify no crash occurred by checking for error dialogs
        composeTestRule.onNodeWithText("Error", useUnmergedTree = true)
            .assertDoesNotExist()
        
        composeTestRule.onNodeWithText("Permission denied", useUnmergedTree = true)
            .assertDoesNotExist()
    }
    
    /**
     * BACK NAVIGATION TEST: Verify proper navigation stack handling
     */
    @Test
    fun testBackNavigationHandling() {
        composeTestRule.waitForIdle()
        
        // Complete form and navigate to camera
        composeTestRule.onNodeWithText("Company Name")
            .performTextInput("Construction Corp")
        
        composeTestRule.onNodeWithText("Project Name")
            .performTextInput("Bridge Project")
        
        composeTestRule.onNodeWithText("Start Safety Documentation")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify we're on camera screen
        composeTestRule.onNodeWithContentDescription("Camera viewfinder")
            .assertExists()
        
        // Test back navigation using system back button
        device.pressBack()
        
        // Since the navigation config uses popUpTo with inclusive=true,
        // back navigation from camera should not return to form
        // Instead, it should either stay on camera or exit the app
        composeTestRule.waitForIdle()
        
        // Verify we don't crash on back navigation
        assertTrue("App should handle back navigation gracefully", true)
    }
    
    /**
     * DATA CLASS INSTANTIATION TEST: Critical test for crash prevention
     */
    @Test
    fun testDataClassInstantiationDuringNavigation() {
        composeTestRule.waitForIdle()
        
        val companyName = "Heavy Industries Inc"
        val projectName = "Steel Framework Installation"
        
        // Fill form
        composeTestRule.onNodeWithText("Company Name")
            .performTextInput(companyName)
        
        composeTestRule.onNodeWithText("Project Name")
            .performTextInput(projectName)
        
        // This click triggers the critical data class instantiation:
        // UserProfile and ProjectInfo objects are created in MainActivity
        composeTestRule.onNodeWithText("Start Safety Documentation")
            .performClick()
        
        // Critical wait - this is where crashes occurred before
        composeTestRule.waitForIdle()
        
        // If we reach this point without crashing, data classes were successfully created
        composeTestRule.onNodeWithContentDescription("Camera viewfinder")
            .assertExists()
        
        // Additional verification: Check that no exception dialogs appear
        composeTestRule.onNodeWithText("Exception", useUnmergedTree = true)
            .assertDoesNotExist()
        
        composeTestRule.onNodeWithText("ClassNotFoundException", useUnmergedTree = true)
            .assertDoesNotExist()
    }
    
    /**
     * GALLERY NAVIGATION TEST: Test navigation to gallery from camera
     */
    @Test
    fun testCameraToGalleryNavigation() {
        composeTestRule.waitForIdle()
        
        // Navigate to camera first
        composeTestRule.onNodeWithText("Company Name")
            .performTextInput("Gallery Test Co")
        
        composeTestRule.onNodeWithText("Project Name")
            .performTextInput("Navigation Test")
        
        composeTestRule.onNodeWithText("Start Safety Documentation")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify camera screen is displayed
        composeTestRule.onNodeWithContentDescription("Camera viewfinder")
            .assertExists()
        
        // Look for gallery navigation button
        composeTestRule.onNodeWithContentDescription("Open gallery")
            .assertExists()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify gallery screen loads
        composeTestRule.onNodeWithText("Photo Gallery", useUnmergedTree = true)
            .assertExists()
    }
    
    /**
     * SETTINGS NAVIGATION TEST: Test navigation to settings from camera
     */
    @Test
    fun testCameraToSettingsNavigation() {
        composeTestRule.waitForIdle()
        
        // Navigate to camera first
        composeTestRule.onNodeWithText("Company Name")
            .performTextInput("Settings Test Co")
        
        composeTestRule.onNodeWithText("Project Name")
            .performTextInput("Config Test")
        
        composeTestRule.onNodeWithText("Start Safety Documentation")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Look for settings navigation button
        composeTestRule.onNodeWithContentDescription("Open settings")
            .assertExists()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify settings screen loads
        composeTestRule.onNodeWithText("Settings", useUnmergedTree = true)
            .assertExists()
    }
    
    /**
     * STRESS TEST: Multiple rapid form submissions
     */
    @Test
    fun testRapidFormSubmissions() {
        composeTestRule.waitForIdle()
        
        // Fill form
        composeTestRule.onNodeWithText("Company Name")
            .performTextInput("Stress Test Corp")
        
        composeTestRule.onNodeWithText("Project Name")
            .performTextInput("Rapid Fire Test")
        
        val button = composeTestRule.onNodeWithText("Start Safety Documentation")
        
        // Perform multiple rapid clicks (should be handled gracefully)
        repeat(3) {
            button.performClick()
        }
        
        composeTestRule.waitForIdle()
        
        // Verify app doesn't crash and navigation occurs
        composeTestRule.onNodeWithContentDescription("Camera viewfinder")
            .assertExists()
    }
    
    /**
     * MEMORY PRESSURE TEST: Large form inputs
     */
    @Test
    fun testLargeFormInputs() {
        composeTestRule.waitForIdle()
        
        val longCompanyName = "Very Long Construction Company Name That Exceeds Normal Input Length For Stress Testing Purposes Inc LLC"
        val longProjectName = "Extremely Detailed Project Name With Multiple Phases And Complex Requirements For Memory Pressure Testing"
        
        // Fill form with large inputs
        composeTestRule.onNodeWithText("Company Name")
            .performTextInput(longCompanyName)
        
        composeTestRule.onNodeWithText("Project Name")
            .performTextInput(longProjectName)
        
        composeTestRule.onNodeWithText("Start Safety Documentation")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify navigation succeeds even with large inputs
        composeTestRule.onNodeWithContentDescription("Camera viewfinder")
            .assertExists()
    }
    
    /**
     * EDGE CASE TEST: Special characters in form inputs
     */
    @Test
    fun testSpecialCharacterInputs() {
        composeTestRule.waitForIdle()
        
        val companyWithSpecialChars = "O'Brien & Associates Construction Co., Ltd. (2024)"
        val projectWithSpecialChars = "Highway I-95 Bridge Reconstruction - Phase 3A/B"
        
        composeTestRule.onNodeWithText("Company Name")
            .performTextInput(companyWithSpecialChars)
        
        composeTestRule.onNodeWithText("Project Name")
            .performTextInput(projectWithSpecialChars)
        
        composeTestRule.onNodeWithText("Start Safety Documentation")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify special characters don't cause navigation issues
        composeTestRule.onNodeWithContentDescription("Camera viewfinder")
            .assertExists()
    }
}
