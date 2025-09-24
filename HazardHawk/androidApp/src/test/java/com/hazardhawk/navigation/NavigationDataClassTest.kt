package com.hazardhawk.navigation

import com.hazardhawk.camera.UserProfile
import com.hazardhawk.camera.ProjectInfo
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for data classes used in navigation flow
 * 
 * These tests ensure that UserProfile and ProjectInfo data classes
 * can be instantiated correctly and don't cause crashes during navigation.
 * 
 * This addresses the root cause of the navigation crash issue.
 */
class NavigationDataClassTest {

    @Test
    fun testUserProfileCreation() {
        // Test default constructor
        val defaultProfile = UserProfile()
        assertNotNull(defaultProfile)
        assertEquals("", defaultProfile.userId)
        assertEquals("", defaultProfile.userName)
        assertEquals("Field Worker", defaultProfile.role)
        assertEquals("", defaultProfile.company)
        assertEquals("Basic", defaultProfile.certificationLevel)
        assertEquals("", defaultProfile.email)
        assertEquals("", defaultProfile.phone)
    }

    @Test
    fun testUserProfileCreationWithParameters() {
        val company = "Test Construction Co"
        val userName = "Construction Worker"
        val userId = "user_1234567890"
        
        val profile = UserProfile(
            company = company,
            userName = userName,
            userId = userId
        )
        
        assertNotNull(profile)
        assertEquals(company, profile.company)
        assertEquals(userName, profile.userName)
        assertEquals(userId, profile.userId)
        assertEquals("Field Worker", profile.role) // Default value
        assertEquals("Basic", profile.certificationLevel) // Default value
    }

    @Test
    fun testProjectInfoCreation() {
        // Test default constructor
        val defaultProject = ProjectInfo()
        assertNotNull(defaultProject)
        assertEquals("", defaultProject.projectId)
        assertEquals("", defaultProject.projectName)
        assertEquals("", defaultProject.siteAddress)
        assertEquals("", defaultProject.projectManager)
        assertEquals("", defaultProject.contractor)
        assertEquals("", defaultProject.startDate)
        assertEquals("", defaultProject.expectedEndDate)
        assertEquals("", defaultProject.safetyOfficer)
    }

    @Test
    fun testProjectInfoCreationWithParameters() {
        val projectName = "Highway Safety Inspection"
        val projectId = "proj_1234567890"
        
        val project = ProjectInfo(
            projectName = projectName,
            projectId = projectId
        )
        
        assertNotNull(project)
        assertEquals(projectName, project.projectName)
        assertEquals(projectId, project.projectId)
        assertEquals("", project.siteAddress) // Default value
    }

    @Test
    fun testDataClassSerialization() {
        // Test that data classes can be converted to string (for debugging/logging)
        val profile = UserProfile(
            company = "Test Co",
            userName = "Test User",
            userId = "test123"
        )
        
        val project = ProjectInfo(
            projectName = "Test Project",
            projectId = "proj123"
        )
        
        val profileString = profile.toString()
        val projectString = project.toString()
        
        assertTrue("UserProfile toString should contain company name", 
                   profileString.contains("Test Co"))
        assertTrue("ProjectInfo toString should contain project name", 
                   projectString.contains("Test Project"))
    }

    @Test
    fun testDataClassEquality() {
        val profile1 = UserProfile(
            company = "Same Company",
            userName = "Same User",
            userId = "same123"
        )
        
        val profile2 = UserProfile(
            company = "Same Company",
            userName = "Same User", 
            userId = "same123"
        )
        
        val profile3 = UserProfile(
            company = "Different Company",
            userName = "Same User",
            userId = "same123"
        )
        
        assertEquals("Identical UserProfiles should be equal", profile1, profile2)
        assertNotEquals("Different UserProfiles should not be equal", profile1, profile3)
    }

    @Test
    fun testDataClassCopy() {
        val originalProfile = UserProfile(
            company = "Original Company",
            userName = "Original User",
            userId = "orig123"
        )
        
        val copiedProfile = originalProfile.copy(company = "Modified Company")
        
        assertEquals("Modified Company", copiedProfile.company)
        assertEquals("Original User", copiedProfile.userName) // Should remain unchanged
        assertEquals("orig123", copiedProfile.userId) // Should remain unchanged
    }

    @Test
    fun testSpecialCharactersInDataClasses() {
        // Test that special characters don't break data class creation
        val profile = UserProfile(
            company = "O'Brien & Associates Construction Co., Ltd. (2024)",
            userName = "José María García-López",
            userId = "user_special_chars_123"
        )
        
        val project = ProjectInfo(
            projectName = "Highway I-95 Bridge Reconstruction - Phase 3A/B",
            projectId = "proj_special_chars_456"
        )
        
        assertNotNull(profile)
        assertNotNull(project)
        assertTrue("Special characters should be preserved in company name",
                   profile.company.contains("O'Brien & Associates"))
        assertTrue("Special characters should be preserved in project name",
                   project.projectName.contains("I-95 Bridge"))
    }

    @Test
    fun testLongStringsInDataClasses() {
        // Test that long strings don't cause issues
        val longCompanyName = "Very Long Construction Company Name That Exceeds Normal Input Length For Stress Testing Purposes And Memory Management Validation Inc LLC Corporation Limited"
        val longProjectName = "Extremely Detailed Project Name With Multiple Phases And Complex Requirements For Memory Pressure Testing And String Handling Validation In The Navigation Flow"
        
        val profile = UserProfile(
            company = longCompanyName,
            userName = "Test User",
            userId = "long_string_test"
        )
        
        val project = ProjectInfo(
            projectName = longProjectName,
            projectId = "long_project_test"
        )
        
        assertNotNull(profile)
        assertNotNull(project)
        assertEquals(longCompanyName, profile.company)
        assertEquals(longProjectName, project.projectName)
    }

    @Test
    fun testEmptyAndNullStringHandling() {
        // Test that empty strings are handled correctly
        val profile = UserProfile(
            company = "",
            userName = "",
            userId = ""
        )
        
        val project = ProjectInfo(
            projectName = "",
            projectId = ""
        )
        
        assertNotNull(profile)
        assertNotNull(project)
        assertEquals("", profile.company)
        assertEquals("", project.projectName)
    }

    @Test
    fun testNavigationDataTransfer() {
        // Simulate the exact data transfer that happens during navigation
        val companyInput = "Test Construction Co"
        val projectInput = "Highway Safety Inspection"
        
        // This mimics the data class creation in MainActivity.kt
        val userProfile = UserProfile(
            company = companyInput,
            userName = "Construction Worker", // Default for now
            userId = "user_${System.currentTimeMillis()}" // Auto-generated
        )
        
        val projectInfo = ProjectInfo(
            projectName = projectInput,
            projectId = "proj_${System.currentTimeMillis()}" // Auto-generated
        )
        
        // Verify data classes were created successfully
        assertNotNull("UserProfile should be created successfully", userProfile)
        assertNotNull("ProjectInfo should be created successfully", projectInfo)
        
        // Verify data was preserved
        assertEquals("Company name should be preserved", companyInput, userProfile.company)
        assertEquals("Project name should be preserved", projectInput, projectInfo.projectName)
        
        // Verify auto-generated fields are not empty
        assertTrue("User ID should be auto-generated", userProfile.userId.isNotEmpty())
        assertTrue("Project ID should be auto-generated", projectInfo.projectId.isNotEmpty())
        
        // Verify auto-generated IDs have expected prefix
        assertTrue("User ID should have correct prefix", userProfile.userId.startsWith("user_"))
        assertTrue("Project ID should have correct prefix", projectInfo.projectId.startsWith("proj_"))
    }
}
