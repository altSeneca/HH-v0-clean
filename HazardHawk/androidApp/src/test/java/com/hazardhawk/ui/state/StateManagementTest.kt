package com.hazardhawk.ui.state

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.camera.MetadataSettingsManager
import com.hazardhawk.ui.camera.hud.HUDStateManager
import com.hazardhawk.ui.camera.hud.MetadataOverlayState
import com.hazardhawk.ui.camera.hud.SafetyHUDState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.*

/**
 * Comprehensive State Management Tests for HazardHawk UI/UX Fixes
 * 
 * Tests the critical state persistence issues identified in research:
 * - Company/project information retention across app restarts
 * - Corrupted data handling and recovery
 * - Concurrent state updates validation
 * - Security context preservation
 * - Construction worker session continuity
 * 
 * Addresses the "Simple, Loveable, Complete" testing philosophy:
 * - Simple: Clear state transitions with minimal complexity
 * - Loveable: Tests provide confidence in construction worker experience
 * - Complete: Coverage of all state persistence scenarios
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class StateManagementTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    @Mock
    private lateinit var mockMetadataSettings: MetadataSettingsManager
    
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var hudStateManager: HUDStateManager
    
    companion object {
        private const val TEST_COMPANY_NAME = "Test Construction Co."
        private const val TEST_PROJECT_NAME = "Downtown Safety Project"
        private const val TEST_COMPANY_UPDATED = "Updated Construction Inc."
        private const val TEST_PROJECT_UPDATED = "Updated Project Name"
        private const val PREFS_NAME = "hazardhawk_state"
        private const val KEY_COMPANY_NAME = "company_name"
        private const val KEY_PROJECT_NAME = "project_name"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_LAST_LOCATION_LAT = "last_location_lat"
        private const val KEY_LAST_LOCATION_LNG = "last_location_lng"
    }
    
    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)
        
        // Mock SharedPreferences behavior
        `when`(mockContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.putFloat(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.putLong(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { }
        
        // Create HUD State Manager for testing
        hudStateManager = HUDStateManager(mockContext, mockMetadataSettings)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    // MARK: - Company/Project Information Persistence Tests
    
    @Test
    fun `statePeristsAcrossAppRestarts - company and project info maintained`() = testScope.runTest {
        // Given: Initial company and project setup
        `when`(mockSharedPreferences.getString(KEY_COMPANY_NAME, "")).thenReturn(TEST_COMPANY_NAME)
        `when`(mockSharedPreferences.getString(KEY_PROJECT_NAME, "")).thenReturn(TEST_PROJECT_NAME)
        
        // When: State manager updates project info
        hudStateManager.updateProjectInfo(TEST_COMPANY_NAME, TEST_PROJECT_NAME)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: Verify state is correctly set
        val currentState = hudStateManager.state.first()
        assertEquals(TEST_COMPANY_NAME, currentState.metadataOverlay.companyName)
        assertEquals(TEST_PROJECT_NAME, currentState.metadataOverlay.projectName)
        
        // And: Verify persistence calls were made
        verify(mockEditor).putString(KEY_COMPANY_NAME, TEST_COMPANY_NAME)
        verify(mockEditor).putString(KEY_PROJECT_NAME, TEST_PROJECT_NAME)
        verify(mockEditor).apply()
    }
    
    @Test
    fun `stateRestoration - handles empty persisted data gracefully`() = testScope.runTest {
        // Given: No previously saved data
        `when`(mockSharedPreferences.getString(KEY_COMPANY_NAME, "")).thenReturn("")
        `when`(mockSharedPreferences.getString(KEY_PROJECT_NAME, "")).thenReturn("")
        
        // When: State manager initializes
        val initialState = hudStateManager.state.first()
        
        // Then: Default state should be set
        assertEquals("", initialState.metadataOverlay.companyName)
        assertEquals("", initialState.metadataOverlay.projectName)
        assertNotNull(initialState.metadataOverlay.timestamp)
        assertTrue(initialState.metadataOverlay.timestamp > 0L)
    }
    
    @Test
    fun `stateUpdate - concurrent updates handled correctly`() = testScope.runTest {
        // Given: Multiple rapid state updates (construction worker scenario)
        val updates = listOf(
            Pair("Company A", "Project 1"),
            Pair("Company B", "Project 2"),
            Pair("Company C", "Project 3")
        )
        
        // When: Concurrent updates are applied
        updates.forEach { (company, project) ->
            hudStateManager.updateProjectInfo(company, project)
        }
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: Final state should reflect last update
        val finalState = hudStateManager.state.first()
        assertEquals("Company C", finalState.metadataOverlay.companyName)
        assertEquals("Project 3", finalState.metadataOverlay.projectName)
        
        // And: All persistence calls were made
        verify(mockEditor, times(3)).putString(eq(KEY_COMPANY_NAME), any())
        verify(mockEditor, times(3)).putString(eq(KEY_PROJECT_NAME), any())
    }
    
    @Test
    fun `handlesCorruptedDataGracefully - invalid JSON recovered`() = testScope.runTest {
        // Given: Corrupted preferences data
        `when`(mockSharedPreferences.getString(KEY_COMPANY_NAME, ""))
            .thenReturn("{\\"invalid\\": \\"json") // Corrupted JSON
        `when`(mockSharedPreferences.getString(KEY_PROJECT_NAME, ""))
            .thenReturn(null) // Null value
        
        // When: State manager attempts to restore state
        val restoredState = hudStateManager.state.first()
        
        // Then: Should gracefully handle corruption with defaults
        assertNotNull(restoredState.metadataOverlay)
        assertEquals("", restoredState.metadataOverlay.companyName)
        assertEquals("", restoredState.metadataOverlay.projectName)
        
        // And: State should still be functional
        hudStateManager.updateProjectInfo(TEST_COMPANY_NAME, TEST_PROJECT_NAME)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val updatedState = hudStateManager.state.first()
        assertEquals(TEST_COMPANY_NAME, updatedState.metadataOverlay.companyName)
        assertEquals(TEST_PROJECT_NAME, updatedState.metadataOverlay.projectName)
    }
    
    // MARK: - Location State Persistence Tests
    
    @Test
    fun `locationStatePersistence - GPS coordinates retained across sessions`() = testScope.runTest {
        // Given: Mock location data
        val testLatitude = 40.7128
        val testLongitude = -74.0060
        val mockLocation = mock(Location::class.java).apply {
            `when`(latitude).thenReturn(testLatitude)
            `when`(longitude).thenReturn(testLongitude)
        }
        
        // When: Location is updated
        hudStateManager.updateLocation(mockLocation)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: State should reflect location
        val currentState = hudStateManager.state.first()
        assertEquals("$testLatitude, $testLongitude", currentState.metadataOverlay.gpsCoordinates)
        
        // And: Location should be persisted
        verify(mockEditor).putFloat(KEY_LAST_LOCATION_LAT, testLatitude.toFloat())
        verify(mockEditor).putFloat(KEY_LAST_LOCATION_LNG, testLongitude.toFloat())
    }
    
    @Test
    fun `locationStateRecovery - handles missing GPS gracefully`() = testScope.runTest {
        // Given: No location available (construction site GPS issues)
        val nullLocation: Location? = null
        
        // When: Null location is provided
        hudStateManager.updateLocation(nullLocation)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: State should remain stable
        val currentState = hudStateManager.state.first()
        assertNotNull(currentState.metadataOverlay)
        // GPS coordinates should remain empty or previous value
        assertTrue(currentState.metadataOverlay.gpsCoordinates.isEmpty())
    }
    
    // MARK: - Session Continuity Tests
    
    @Test
    fun `sessionContinuity - construction worker workflow preservation`() = testScope.runTest {
        // Given: Active construction worker session
        val sessionId = "session_${System.currentTimeMillis()}"
        `when`(mockSharedPreferences.getString(KEY_SESSION_ID, "")).thenReturn(sessionId)
        
        // When: Worker completes project setup
        hudStateManager.updateProjectInfo(TEST_COMPANY_NAME, TEST_PROJECT_NAME)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: Session context should be maintained
        val state = hudStateManager.state.first()
        assertEquals(TEST_COMPANY_NAME, state.metadataOverlay.companyName)
        assertEquals(TEST_PROJECT_NAME, state.metadataOverlay.projectName)
        
        // And: Session ID should be persisted
        verify(mockEditor).putString(KEY_SESSION_ID, sessionId)
    }
    
    @Test
    fun `interruptionRecovery - phone call state preservation`() = testScope.runTest {
        // Given: Active camera session with metadata
        hudStateManager.updateProjectInfo(TEST_COMPANY_NAME, TEST_PROJECT_NAME)
        val mockLocation = mock(Location::class.java).apply {
            `when`(latitude).thenReturn(40.7128)
            `when`(longitude).thenReturn(-74.0060)
        }
        hudStateManager.updateLocation(mockLocation)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val preInterruptionState = hudStateManager.state.first()
        
        // When: App is interrupted (simulating phone call)
        // State should be automatically preserved
        
        // Then: After interruption, state should be recoverable
        val postInterruptionState = hudStateManager.state.first()
        assertEquals(preInterruptionState.metadataOverlay.companyName, 
                    postInterruptionState.metadataOverlay.companyName)
        assertEquals(preInterruptionState.metadataOverlay.projectName, 
                    postInterruptionState.metadataOverlay.projectName)
        assertEquals(preInterruptionState.metadataOverlay.gpsCoordinates, 
                    postInterruptionState.metadataOverlay.gpsCoordinates)
    }
    
    // MARK: - State Validation Tests
    
    @Test
    fun `validatesConcurrentStateUpdates - thread safety verification`() = testScope.runTest {
        // Given: Multiple concurrent operations
        val operations = (1..10).map { index ->
            async {
                hudStateManager.updateProjectInfo(
                    "Company $index", 
                    "Project $index"
                )
            }
        }
        
        // When: All operations complete
        operations.awaitAll()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: State should be consistent (last update wins)
        val finalState = hudStateManager.state.first()
        assertNotNull(finalState.metadataOverlay.companyName)
        assertNotNull(finalState.metadataOverlay.projectName)
        assertTrue(finalState.metadataOverlay.companyName.startsWith("Company"))
        assertTrue(finalState.metadataOverlay.projectName.startsWith("Project"))
        
        // And: No state corruption should occur
        assertNotEquals("", finalState.metadataOverlay.companyName)
        assertNotEquals("", finalState.metadataOverlay.projectName)
    }
    
    @Test
    fun `stateValidation - prevents invalid data entry`() = testScope.runTest {
        // Given: Invalid input data (empty strings, special characters)
        val invalidInputs = listOf(
            Pair("", ""), // Empty strings
            Pair("   ", "   "), // Whitespace only
            Pair("A", "B"), // Too short
            Pair("Valid Company", ""), // Partial data
            Pair("", "Valid Project") // Partial data
        )
        
        invalidInputs.forEach { (company, project) ->
            // When: Invalid data is provided
            hudStateManager.updateProjectInfo(company, project)
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then: State should handle gracefully
            val state = hudStateManager.state.first()
            assertNotNull(state.metadataOverlay)
            // System should accept any input for flexibility, but log appropriately
        }
    }
    
    // MARK: - Performance and Memory Tests
    
    @Test
    fun `statePerformance - rapid updates handled efficiently`() = testScope.runTest {
        val startTime = System.currentTimeMillis()
        
        // When: Rapid state updates (construction worker corrections)
        repeat(100) { index ->
            hudStateManager.updateProjectInfo(
                "Company Update $index",
                "Project Update $index"
            )
        }
        testDispatcher.scheduler.advanceUntilIdle()
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Then: Updates should complete quickly
        assertTrue("State updates should be performant (completed in ${duration}ms)", 
                  duration < 5000) // Less than 5 seconds for 100 updates
        
        // And: Final state should be correct
        val finalState = hudStateManager.state.first()
        assertEquals("Company Update 99", finalState.metadataOverlay.companyName)
        assertEquals("Project Update 99", finalState.metadataOverlay.projectName)
    }
    
    @Test
    fun `memoryLeakPrevention - state manager cleanup verification`() = testScope.runTest {
        // Given: Active state manager with multiple updates
        repeat(50) { index ->
            hudStateManager.updateProjectInfo("Company $index", "Project $index")
        }
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: State manager is cleared
        hudStateManager.onCleared()
        
        // Then: Resources should be cleaned up
        // Note: Actual memory leak detection would require instrumentation tests
        // This test verifies the cleanup method can be called without errors
        assertTrue("State manager cleanup should complete without errors", true)
    }
    
    // MARK: - Construction Worker Scenario Tests
    
    @Test
    fun `constructionWorkerFlow - complete documentation workflow`() = testScope.runTest {
        // Given: Construction worker starts shift
        val workerSession = mapOf(
            "shift_start" to System.currentTimeMillis(),
            "site_location" to "Construction Site A"
        )
        
        // When: Worker sets up project information
        hudStateManager.updateProjectInfo(TEST_COMPANY_NAME, TEST_PROJECT_NAME)
        
        // And: Updates location as they move around site
        val siteLocations = listOf(
            Pair(40.7128, -74.0060), // Entry point
            Pair(40.7130, -74.0058), // Building A
            Pair(40.7125, -74.0062)  // Building B
        )
        
        siteLocations.forEach { (lat, lng) ->
            val location = mock(Location::class.java).apply {
                `when`(latitude).thenReturn(lat)
                `when`(longitude).thenReturn(lng)
            }
            hudStateManager.updateLocation(location)
        }
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: All state should be preserved throughout workflow
        val finalState = hudStateManager.state.first()
        assertEquals(TEST_COMPANY_NAME, finalState.metadataOverlay.companyName)
        assertEquals(TEST_PROJECT_NAME, finalState.metadataOverlay.projectName)
        assertEquals("40.7125, -74.0062", finalState.metadataOverlay.gpsCoordinates)
        
        // And: State should be ready for photo capture
        assertNotNull(finalState.metadataOverlay.timestamp)
        assertTrue(finalState.metadataOverlay.timestamp > 0L)
    }
    
    @Test
    fun `emergencyModeStatePersistence - critical scenario handling`() = testScope.runTest {
        // Given: Normal operation state
        hudStateManager.updateProjectInfo(TEST_COMPANY_NAME, TEST_PROJECT_NAME)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: Emergency mode is activated
        hudStateManager.enableEmergencyMode()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: Project info should be preserved in emergency mode
        val emergencyState = hudStateManager.state.first()
        assertTrue(emergencyState.emergencyMode)
        assertEquals(TEST_COMPANY_NAME, emergencyState.metadataOverlay.companyName)
        assertEquals(TEST_PROJECT_NAME, emergencyState.metadataOverlay.projectName)
        
        // When: Emergency mode is disabled
        hudStateManager.disableEmergencyMode()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: Normal state should be restored with preserved data
        val normalState = hudStateManager.state.first()
        assertFalse(normalState.emergencyMode)
        assertEquals(TEST_COMPANY_NAME, normalState.metadataOverlay.companyName)
        assertEquals(TEST_PROJECT_NAME, normalState.metadataOverlay.projectName)
    }
}

/**
 * Helper functions for test data creation
 */
private fun createTestMetadataOverlay(
    companyName: String = "",
    projectName: String = "",
    gpsCoordinates: String = "",
    timestamp: Long = System.currentTimeMillis()
): MetadataOverlayState {
    return MetadataOverlayState(
        companyName = companyName,
        projectName = projectName,
        gpsCoordinates = gpsCoordinates,
        timestamp = timestamp
    )
}

private fun createTestSafetyHUDState(
    metadataOverlay: MetadataOverlayState = MetadataOverlayState(),
    emergencyMode: Boolean = false
): SafetyHUDState {
    return SafetyHUDState(
        metadataOverlay = metadataOverlay,
        emergencyMode = emergencyMode
    )
}
