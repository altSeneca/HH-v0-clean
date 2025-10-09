package com.hazardhawk.ui.home.viewmodels

import com.hazardhawk.data.repositories.dashboard.*
import com.hazardhawk.models.dashboard.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*
import kotlin.time.Duration.Companion.hours

/**
 * Comprehensive unit tests for DashboardViewModel.
 * 
 * Tests cover:
 * - State initialization
 * - Data refresh functionality
 * - Permission validation
 * - Error handling
 * - User tier management
 * - Notification handling
 * 
 * Testing philosophy (Simple, Loveable, Complete):
 * - Simple: Clear test names and straightforward assertions
 * - Loveable: Provides confidence in core dashboard functionality
 * - Complete: Covers all public methods and edge cases
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    // Mock dependencies
    private lateinit var mockActivityRepository: ActivityRepositoryImpl
    private lateinit var mockUserProfileRepository: UserProfileRepositoryImpl
    private lateinit var mockWeatherRepository: WeatherRepositoryImpl

    // Test subject
    private lateinit var viewModel: DashboardViewModel

    // Test dispatcher and scope
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    // Test data
    private val testUserProfile = createTestUserProfile(UserTier.SAFETY_LEAD)
    private val testWeather = createTestWeather()
    private val testSiteConditions = createTestSiteConditions()

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)

        // Initialize mocks
        mockActivityRepository = mockk(relaxed = true)
        mockUserProfileRepository = mockk(relaxed = true)
        mockWeatherRepository = mockk(relaxed = true)

        // Setup default mock behaviors
        coEvery { mockUserProfileRepository.getCurrentUser() } returns testUserProfile
        every { mockUserProfileRepository.getCurrentUserFlow() } returns flowOf(testUserProfile)
        every { mockWeatherRepository.currentWeather } returns MutableStateFlow(testWeather)
        coEvery { mockWeatherRepository.getSiteConditions(any(), any()) } returns Result.success(testSiteConditions)
        every { mockActivityRepository.getActivityFeed(any(), any()) } returns flowOf(emptyList())
        coEvery { mockActivityRepository.refreshActivities() } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // ========================================================================
    // MARK: - Initialization Tests
    // ========================================================================

    @Test
    fun `initialization - loads dashboard data successfully`() = testScope.runTest {
        // When: ViewModel is initialized
        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: User profile should be loaded
        val userProfile = viewModel.userProfile.value
        assertNotNull(userProfile)
        assertEquals(testUserProfile.userId, userProfile.userId)
        assertEquals(testUserProfile.userTier, userProfile.userTier)

        // And: Site conditions should be loaded
        val siteConditions = viewModel.siteConditions.value
        assertNotNull(siteConditions)

        // And: Command center buttons should be initialized
        val buttons = viewModel.commandCenterButtons.value
        assertTrue(buttons.isNotEmpty())

        // And: Loading state should be false
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `initialization - handles error gracefully`() = testScope.runTest {
        // Given: Repository throws exception
        coEvery { mockUserProfileRepository.getCurrentUser() } throws Exception("Network error")

        // When: ViewModel is initialized
        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error message should be set
        val errorMessage = viewModel.errorMessage.value
        assertNotNull(errorMessage)
        assertTrue(errorMessage.contains("Failed to load dashboard"))

        // And: User profile should be null
        assertNull(viewModel.userProfile.value)
    }

    @Test
    fun `initialization - observes user profile changes reactively`() = testScope.runTest {
        // Given: User profile flow that emits updates
        val updatedProfile = testUserProfile.copy(userTier = UserTier.PROJECT_ADMIN)
        val profileFlow = MutableStateFlow(testUserProfile)
        every { mockUserProfileRepository.getCurrentUserFlow() } returns profileFlow

        // When: ViewModel is initialized
        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // And: Profile is updated
        profileFlow.value = updatedProfile
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: ViewModel should reflect updated profile
        val userProfile = viewModel.userProfile.value
        assertEquals(UserTier.PROJECT_ADMIN, userProfile?.userTier)

        // And: Command center buttons should be updated for new tier
        val buttons = viewModel.commandCenterButtons.value
        assertTrue(buttons.isNotEmpty())
    }

    // ========================================================================
    // MARK: - Refresh Data Tests
    // ========================================================================

    @Test
    fun `refreshData - successfully refreshes all data`() = testScope.runTest {
        // Given: Initialized ViewModel
        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Refresh is triggered
        viewModel.refreshData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Loading state should be true during refresh
        // (Advanced until idle, so loading should be false after completion)
        assertFalse(viewModel.isRefreshing.value)

        // And: All repositories should be called
        coVerify { mockActivityRepository.refreshActivities() }
        coVerify { mockUserProfileRepository.getCurrentUser() }
        coVerify { mockWeatherRepository.getSiteConditions(any(), any()) }

        // And: No error should be present
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `refreshData - handles error during refresh`() = testScope.runTest {
        // Given: Initialized ViewModel
        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // And: Repository throws exception
        coEvery { mockActivityRepository.refreshActivities() } throws Exception("Refresh failed")

        // When: Refresh is triggered
        viewModel.refreshData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Loading should be complete
        assertFalse(viewModel.isRefreshing.value)

        // And: Error message should be set
        val errorMessage = viewModel.errorMessage.value
        assertNotNull(errorMessage)
        assertTrue(errorMessage.contains("Failed to refresh dashboard"))
    }

    @Test
    fun `refreshData - clears previous error message`() = testScope.runTest {
        // Given: ViewModel with existing error
        coEvery { mockUserProfileRepository.getCurrentUser() } throws Exception("Initial error")
        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)

        // And: Setup successful refresh
        coEvery { mockUserProfileRepository.getCurrentUser() } returns testUserProfile

        // When: Refresh is triggered
        viewModel.refreshData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error should be cleared
        assertNull(viewModel.errorMessage.value)
    }

    // ========================================================================
    // MARK: - Permission Validation Tests
    // ========================================================================

    @Test
    fun `onActionClick - allows action for authorized user tier`() = testScope.runTest {
        // Given: Safety Lead user
        val safetyLeadProfile = testUserProfile.copy(userTier = UserTier.SAFETY_LEAD)
        coEvery { mockUserProfileRepository.getCurrentUser() } returns safetyLeadProfile
        every { mockUserProfileRepository.getCurrentUserFlow() } returns flowOf(safetyLeadProfile)

        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User clicks on CREATE_PTP action (requires SAFETY_LEAD)
        viewModel.onActionClick(SafetyAction.CREATE_PTP)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: No error should be set (action is allowed)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `onActionClick - denies action for unauthorized user tier`() = testScope.runTest {
        // Given: Field Access user
        val fieldAccessProfile = testUserProfile.copy(userTier = UserTier.FIELD_ACCESS)
        coEvery { mockUserProfileRepository.getCurrentUser() } returns fieldAccessProfile
        every { mockUserProfileRepository.getCurrentUserFlow() } returns flowOf(fieldAccessProfile)

        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User clicks on CREATE_PTP action (requires SAFETY_LEAD)
        viewModel.onActionClick(SafetyAction.CREATE_PTP)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error message should indicate insufficient permissions
        val errorMessage = viewModel.errorMessage.value
        assertNotNull(errorMessage)
        assertTrue(errorMessage.contains("don't have permission"))
    }

    @Test
    fun `onActionClick - shows coming soon message for unimplemented features`() = testScope.runTest {
        // Given: Project Admin user (has all permissions)
        val adminProfile = testUserProfile.copy(userTier = UserTier.PROJECT_ADMIN)
        coEvery { mockUserProfileRepository.getCurrentUser() } returns adminProfile
        every { mockUserProfileRepository.getCurrentUserFlow() } returns flowOf(adminProfile)

        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User clicks on unimplemented action
        viewModel.onActionClick(SafetyAction.CREATE_TOOLBOX_TALK)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error message should indicate feature is coming soon
        val errorMessage = viewModel.errorMessage.value
        assertNotNull(errorMessage)
        assertTrue(errorMessage.contains("coming soon"))
    }

    @Test
    fun `onActionClick - PROJECT_ADMIN has access to all actions`() = testScope.runTest {
        // Given: Project Admin user
        val adminProfile = testUserProfile.copy(userTier = UserTier.PROJECT_ADMIN)
        coEvery { mockUserProfileRepository.getCurrentUser() } returns adminProfile
        every { mockUserProfileRepository.getCurrentUserFlow() } returns flowOf(adminProfile)

        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Admin clicks on various implemented actions
        val implementedActions = listOf(
            SafetyAction.CREATE_PTP,
            SafetyAction.CAPTURE_PHOTO,
            SafetyAction.VIEW_REPORTS,
            SafetyAction.OPEN_GALLERY,
            SafetyAction.LIVE_DETECTION
        )

        implementedActions.forEach { action ->
            viewModel.clearError()
            viewModel.onActionClick(action)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then: No permission errors for implemented actions
            val errorMessage = viewModel.errorMessage.value
            if (errorMessage != null) {
                assertFalse(errorMessage.contains("don't have permission"))
            }
        }
    }

    // ========================================================================
    // MARK: - Notification Handling Tests
    // ========================================================================

    @Test
    fun `dismissNotification - successfully dismisses alert`() = testScope.runTest {
        // Given: Activity feed with alerts
        val testAlert = ActivityFeedItem.SystemAlert(
            id = "alert_001",
            timestamp = Clock.System.now().toEpochMilliseconds(),
            alertType = AlertType.WEATHER,
            message = "Heat advisory",
            priority = AlertPriority.HIGH,
            actionRequired = true,
            dismissed = false
        )
        every { mockActivityRepository.getActivityFeed(any(), any()) } returns flowOf(listOf(testAlert))
        coEvery { mockActivityRepository.dismissAlert("alert_001") } returns Result.success(Unit)

        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Notification is dismissed
        viewModel.dismissNotification("alert_001")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Repository should be called
        coVerify { mockActivityRepository.dismissAlert("alert_001") }

        // And: No error should be set
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `dismissNotification - handles error gracefully`() = testScope.runTest {
        // Given: Repository fails to dismiss alert
        coEvery { mockActivityRepository.dismissAlert(any()) } returns Result.failure(Exception("Dismiss failed"))

        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Notification is dismissed
        viewModel.dismissNotification("alert_001")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error message should be set
        val errorMessage = viewModel.errorMessage.value
        assertNotNull(errorMessage)
        assertTrue(errorMessage.contains("Failed to dismiss notification"))
    }

    @Test
    fun `unreadNotificationCount - updates when notifications change`() = testScope.runTest {
        // Given: Activity feed with multiple alerts
        val alerts = listOf(
            createTestAlert("alert_001", dismissed = false),
            createTestAlert("alert_002", dismissed = false),
            createTestAlert("alert_003", dismissed = true)
        )
        every { mockActivityRepository.getActivityFeed(any(), any()) } returns flowOf(alerts)

        // When: ViewModel is initialized
        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Unread count should reflect undismissed alerts
        val unreadCount = viewModel.unreadNotificationCount.value
        assertEquals(2, unreadCount)
    }

    // ========================================================================
    // MARK: - Error Handling Tests
    // ========================================================================

    @Test
    fun `clearError - clears error message`() = testScope.runTest {
        // Given: ViewModel with error
        coEvery { mockUserProfileRepository.getCurrentUser() } throws Exception("Test error")
        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)

        // When: Error is cleared
        viewModel.clearError()

        // Then: Error message should be null
        assertNull(viewModel.errorMessage.value)
    }

    // ========================================================================
    // MARK: - Weather and Site Conditions Tests
    // ========================================================================

    @Test
    fun `isWeatherSafeForWork - returns true for safe conditions`() = testScope.runTest {
        // Given: Safe weather conditions
        val safeWeather = createTestWeather(temperature = 75, condition = WeatherType.CLEAR)
        val safeSiteConditions = testSiteConditions.copy(weather = safeWeather)
        coEvery { mockWeatherRepository.getSiteConditions(any(), any()) } returns Result.success(safeSiteConditions)

        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Checking if weather is safe
        val isSafe = viewModel.isWeatherSafeForWork()

        // Then: Should return true
        assertTrue(isSafe)
    }

    @Test
    fun `isWeatherSafeForWork - returns false for dangerous conditions`() = testScope.runTest {
        // Given: Dangerous weather conditions
        val dangerousWeather = createTestWeather(temperature = 105, condition = WeatherType.THUNDERSTORM)
        val dangerousSiteConditions = testSiteConditions.copy(weather = dangerousWeather)
        coEvery { mockWeatherRepository.getSiteConditions(any(), any()) } returns Result.success(dangerousSiteConditions)

        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Checking if weather is safe
        val isSafe = viewModel.isWeatherSafeForWork()

        // Then: Should return false
        assertFalse(isSafe)
    }

    @Test
    fun `setCurrentProject - switches project and refreshes data`() = testScope.runTest {
        // Given: Initialized ViewModel
        coEvery { mockUserProfileRepository.setCurrentProject(any()) } returns Result.success(Unit)
        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Project is changed
        viewModel.setCurrentProject("project_new_001")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Repository should be called
        coVerify { mockUserProfileRepository.setCurrentProject("project_new_001") }

        // And: Data should be refreshed
        coVerify(atLeast = 2) { mockActivityRepository.refreshActivities() }
    }

    @Test
    fun `setCurrentProject - handles error when switching fails`() = testScope.runTest {
        // Given: Initialized ViewModel
        coEvery { mockUserProfileRepository.setCurrentProject(any()) } returns 
            Result.failure(Exception("Switch failed"))

        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Project switch is attempted
        viewModel.setCurrentProject("project_new_001")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error message should be set
        val errorMessage = viewModel.errorMessage.value
        assertNotNull(errorMessage)
        assertTrue(errorMessage.contains("Failed to switch project"))
    }

    // ========================================================================
    // MARK: - Command Center Button Tests
    // ========================================================================

    @Test
    fun `commandCenterButtons - filtered by FIELD_ACCESS tier`() = testScope.runTest {
        // Given: Field Access user
        val fieldAccessProfile = testUserProfile.copy(userTier = UserTier.FIELD_ACCESS)
        coEvery { mockUserProfileRepository.getCurrentUser() } returns fieldAccessProfile
        every { mockUserProfileRepository.getCurrentUserFlow() } returns flowOf(fieldAccessProfile)

        // When: ViewModel is initialized
        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Only field access buttons should be available
        val buttons = viewModel.commandCenterButtons.value
        assertTrue(buttons.isNotEmpty())
        
        // Field access users should only see actions available to their tier
        val fieldAccessActions = buttons.map { it.action }
        assertTrue(fieldAccessActions.all { action ->
            action.isAvailableForTier(UserTier.FIELD_ACCESS)
        })
    }

    @Test
    fun `commandCenterButtons - includes all appropriate actions for SAFETY_LEAD`() = testScope.runTest {
        // Given: Safety Lead user
        val safetyLeadProfile = testUserProfile.copy(userTier = UserTier.SAFETY_LEAD)
        coEvery { mockUserProfileRepository.getCurrentUser() } returns safetyLeadProfile
        every { mockUserProfileRepository.getCurrentUserFlow() } returns flowOf(safetyLeadProfile)

        // When: ViewModel is initialized
        viewModel = DashboardViewModel(
            mockActivityRepository,
            mockUserProfileRepository,
            mockWeatherRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Safety Lead buttons should include elevated permissions
        val buttons = viewModel.commandCenterButtons.value
        val actions = buttons.map { it.action }
        
        // Should include CREATE_PTP which requires SAFETY_LEAD
        assertTrue(actions.contains(SafetyAction.CREATE_PTP))
    }

    // ========================================================================
    // MARK: - Helper Functions
    // ========================================================================

    private fun createTestUserProfile(tier: UserTier): UserProfile {
        val now = Clock.System.now()
        return UserProfile(
            userId = "test_user_001",
            email = "test@hazardhawk.com",
            fullName = "Test User",
            role = "Test Safety Lead",
            userTier = tier,
            companyId = "test_company_001",
            companyName = "Test Construction Co.",
            currentProjectId = "test_project_001",
            avatarUrl = null,
            phoneNumber = "+1234567890",
            createdAt = now.minus(30.hours),
            lastLogin = now,
            lastUpdated = now,
            preferences = UserPreferences()
        )
    }

    private fun createTestWeather(
        temperature: Int = 75,
        condition: WeatherType = WeatherType.CLEAR
    ): WeatherConditions {
        return WeatherConditions(
            temperature = temperature,
            temperatureUnit = "°F",
            condition = condition,
            description = "Test weather",
            humidity = 50,
            windSpeed = 10,
            windDirection = "N",
            precipitation = 0,
            uvIndex = 5,
            visibility = 10.0,
            alerts = emptyList()
        )
    }

    private fun createTestSiteConditions(): SiteConditions {
        return SiteConditions(
            weather = createTestWeather(),
            crewInfo = CrewInfo(
                totalCrew = 10,
                presentCount = 10,
                supervisors = 2,
                specialtyTrades = listOf("Test Trade")
            ),
            shiftInfo = ShiftInfo(
                shiftType = ShiftType.DAY_SHIFT,
                startTime = "08:00",
                endTime = "16:00",
                breakTimes = listOf("12:00"),
                currentStatus = ShiftStatus.ACTIVE
            ),
            lastUpdated = Clock.System.now().toEpochMilliseconds()
        )
    }

    private fun createTestAlert(id: String, dismissed: Boolean): ActivityFeedItem.SystemAlert {
        return ActivityFeedItem.SystemAlert(
            id = id,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            alertType = AlertType.WEATHER,
            message = "Test alert",
            priority = AlertPriority.MEDIUM,
            actionRequired = false,
            dismissed = dismissed
        )
    }
}
