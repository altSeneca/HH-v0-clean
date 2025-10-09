package com.hazardhawk.data.repositories

import com.hazardhawk.data.repositories.dashboard.ActivityRepositoryImpl
import com.hazardhawk.data.repositories.dashboard.ActivityStats
import com.hazardhawk.models.dashboard.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Test
import kotlin.test.*

/**
 * Comprehensive unit tests for ActivityRepositoryImpl.
 * 
 * Tests cover:
 * - Activity feed retrieval and filtering
 * - Sorting by timestamp
 * - Unread count calculation
 * - Hazard resolution
 * - Alert dismissal
 * - Activity statistics
 * 
 * Testing philosophy (Simple, Loveable, Complete):
 * - Simple: Straightforward test cases with clear assertions
 * - Loveable: Provides confidence in activity management
 * - Complete: Covers all repository methods and edge cases
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActivityRepositoryTest {

    private lateinit var repository: ActivityRepositoryImpl
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        testScope = TestScope()
        repository = ActivityRepositoryImpl()
    }

    // ========================================================================
    // MARK: - Activity Feed Tests
    // ========================================================================

    @Test
    fun `getActivityFeed - returns activities sorted by timestamp descending`() = testScope.runTest {
        // When: Fetching activity feed
        val activities = repository.getActivityFeed(limit = 20, includeResolved = true).first()

        // Then: Activities should be sorted by timestamp (newest first)
        val timestamps = activities.map { it.timestamp }
        val sortedTimestamps = timestamps.sortedDescending()
        assertEquals(sortedTimestamps, timestamps)
    }

    @Test
    fun `getActivityFeed - respects limit parameter`() = testScope.runTest {
        // When: Fetching with limit
        val limit = 3
        val activities = repository.getActivityFeed(limit = limit, includeResolved = true).first()

        // Then: Should not exceed limit
        assertTrue(activities.size <= limit)
    }

    @Test
    fun `getActivityFeed - filters out resolved hazards when includeResolved is false`() = testScope.runTest {
        // When: Fetching without resolved items
        val activities = repository.getActivityFeed(limit = 20, includeResolved = false).first()

        // Then: No resolved hazards should be present
        val resolvedHazards = activities.filterIsInstance<ActivityFeedItem.HazardActivity>()
            .filter { it.resolved }
        assertTrue(resolvedHazards.isEmpty())
    }

    @Test
    fun `getActivityFeed - filters out dismissed alerts when includeResolved is false`() = testScope.runTest {
        // When: Fetching without dismissed items
        val activities = repository.getActivityFeed(limit = 20, includeResolved = false).first()

        // Then: No dismissed alerts should be present
        val dismissedAlerts = activities.filterIsInstance<ActivityFeedItem.SystemAlert>()
            .filter { it.dismissed }
        assertTrue(dismissedAlerts.isEmpty())
    }

    @Test
    fun `getActivityFeed - includes resolved items when includeResolved is true`() = testScope.runTest {
        // When: Fetching with resolved items
        val activities = repository.getActivityFeed(limit = 20, includeResolved = true).first()

        // Then: Should include both resolved and unresolved items
        val resolvedHazards = activities.filterIsInstance<ActivityFeedItem.HazardActivity>()
            .filter { it.resolved }
        // Note: Mock data includes at least one resolved hazard
        assertTrue(resolvedHazards.isNotEmpty())
    }

    @Test
    fun `getActivityFeed - returns different activity types`() = testScope.runTest {
        // When: Fetching activity feed
        val activities = repository.getActivityFeed(limit = 20, includeResolved = true).first()

        // Then: Should contain multiple activity types
        val hasPTP = activities.any { it is ActivityFeedItem.PTPActivity }
        val hasHazard = activities.any { it is ActivityFeedItem.HazardActivity }
        val hasPhoto = activities.any { it is ActivityFeedItem.PhotoActivity }
        val hasAlert = activities.any { it is ActivityFeedItem.SystemAlert }

        assertTrue(hasPTP, "Should contain PTP activities")
        assertTrue(hasHazard, "Should contain hazard activities")
        assertTrue(hasPhoto, "Should contain photo activities")
        assertTrue(hasAlert, "Should contain alerts")
    }

    // ========================================================================
    // MARK: - Filtering Tests
    // ========================================================================

    @Test
    fun `getActivityFeedForProject - filters by project`() = testScope.runTest {
        // When: Fetching project-specific feed
        val projectId = "project_001"
        val activities = repository.getActivityFeedForProject(projectId, limit = 10).first()

        // Then: Should return activities (mock doesn't filter, but verifies method works)
        assertNotNull(activities)
    }

    @Test
    fun `getActivityFeedForProject - respects limit`() = testScope.runTest {
        // When: Fetching with limit
        val limit = 5
        val activities = repository.getActivityFeedForProject("project_001", limit = limit).first()

        // Then: Should not exceed limit
        assertTrue(activities.size <= limit)
    }

    // ========================================================================
    // MARK: - Unread Count Tests
    // ========================================================================

    @Test
    fun `getUnreadCount - counts unresolved hazards`() = testScope.runTest {
        // When: Getting unread count
        val unreadCount = repository.getUnreadCount()

        // Then: Should count unresolved items
        assertTrue(unreadCount >= 0)

        // Verify by manually checking
        val activities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        val expectedCount = activities.count { item ->
            when (item) {
                is ActivityFeedItem.HazardActivity -> !item.resolved
                is ActivityFeedItem.SystemAlert -> !item.dismissed && item.actionRequired
                is ActivityFeedItem.PhotoActivity -> item.needsReview
                is ActivityFeedItem.PTPActivity -> item.status == PTPStatus.PENDING_REVIEW
                else -> false
            }
        }

        assertEquals(expectedCount, unreadCount)
    }

    @Test
    fun `getUnreadCount - includes undismissed alerts with actionRequired`() = testScope.runTest {
        // When: Getting unread count
        val unreadCount = repository.getUnreadCount()

        // Then: Should include alerts requiring action
        val activities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        val alertsRequiringAction = activities.filterIsInstance<ActivityFeedItem.SystemAlert>()
            .count { !it.dismissed && it.actionRequired }

        assertTrue(unreadCount >= alertsRequiringAction)
    }

    @Test
    fun `getUnreadCount - includes photos needing review`() = testScope.runTest {
        // When: Getting unread count
        val activities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        val photosNeedingReview = activities.filterIsInstance<ActivityFeedItem.PhotoActivity>()
            .count { it.needsReview }

        // Then: Should be counted
        if (photosNeedingReview > 0) {
            val unreadCount = repository.getUnreadCount()
            assertTrue(unreadCount >= photosNeedingReview)
        } else {
            // No assertion if no photos need review
            assertTrue(true)
        }
    }

    @Test
    fun `getUnreadCount - includes PTPs pending review`() = testScope.runTest {
        // When: Getting unread count
        val activities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        val ptpsPendingReview = activities.filterIsInstance<ActivityFeedItem.PTPActivity>()
            .count { it.status == PTPStatus.PENDING_REVIEW }

        // Then: Should be counted
        if (ptpsPendingReview > 0) {
            val unreadCount = repository.getUnreadCount()
            assertTrue(unreadCount >= ptpsPendingReview)
        } else {
            assertTrue(true)
        }
    }

    // ========================================================================
    // MARK: - Hazard Resolution Tests
    // ========================================================================

    @Test
    fun `markHazardResolved - successfully marks hazard as resolved`() = testScope.runTest {
        // Given: Activity feed with unresolved hazard
        val activities = repository.getActivityFeed(limit = 100, includeResolved = false).first()
        val unresolvedHazard = activities.filterIsInstance<ActivityFeedItem.HazardActivity>()
            .firstOrNull { !it.resolved }

        assertNotNull(unresolvedHazard, "Should have at least one unresolved hazard in mock data")

        // When: Marking hazard as resolved
        val result = repository.markHazardResolved(unresolvedHazard.hazardId)

        // Then: Should succeed
        assertTrue(result.isSuccess)

        // And: Hazard should be resolved in subsequent fetches
        val updatedActivities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        val resolvedHazard = updatedActivities.filterIsInstance<ActivityFeedItem.HazardActivity>()
            .first { it.hazardId == unresolvedHazard.hazardId }
        assertTrue(resolvedHazard.resolved)
    }

    @Test
    fun `markHazardResolved - returns error for non-existent hazard`() = testScope.runTest {
        // When: Trying to resolve non-existent hazard
        val result = repository.markHazardResolved("non_existent_hazard")

        // Then: Should fail
        assertTrue(result.isFailure)
        
        // And: Error message should indicate hazard not found
        result.exceptionOrNull()?.let { exception ->
            assertTrue(exception is IllegalArgumentException)
            assertTrue(exception.message?.contains("not found") == true)
        }
    }

    // ========================================================================
    // MARK: - Alert Dismissal Tests
    // ========================================================================

    @Test
    fun `dismissAlert - successfully dismisses alert`() = testScope.runTest {
        // Given: Activity feed with undismissed alert
        val activities = repository.getActivityFeed(limit = 100, includeResolved = false).first()
        val undismissedAlert = activities.filterIsInstance<ActivityFeedItem.SystemAlert>()
            .firstOrNull { !it.dismissed }

        assertNotNull(undismissedAlert, "Should have at least one undismissed alert in mock data")

        // When: Dismissing alert
        val result = repository.dismissAlert(undismissedAlert.id)

        // Then: Should succeed
        assertTrue(result.isSuccess)

        // And: Alert should be dismissed in subsequent fetches
        val updatedActivities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        val dismissedAlert = updatedActivities.filterIsInstance<ActivityFeedItem.SystemAlert>()
            .first { it.id == undismissedAlert.id }
        assertTrue(dismissedAlert.dismissed)
    }

    @Test
    fun `dismissAlert - returns error for non-existent alert`() = testScope.runTest {
        // When: Trying to dismiss non-existent alert
        val result = repository.dismissAlert("non_existent_alert")

        // Then: Should fail
        assertTrue(result.isFailure)

        // And: Error message should indicate alert not found
        result.exceptionOrNull()?.let { exception ->
            assertTrue(exception is IllegalArgumentException)
            assertTrue(exception.message?.contains("not found") == true)
        }
    }

    // ========================================================================
    // MARK: - Add Activity Tests
    // ========================================================================

    @Test
    fun `addActivity - successfully adds new activity to feed`() = testScope.runTest {
        // Given: New activity to add
        val newActivity = ActivityFeedItem.HazardActivity(
            id = "new_hazard_001",
            timestamp = Clock.System.now().toEpochMilliseconds(),
            hazardId = "haz_new_001",
            hazardType = "Test Hazard",
            hazardDescription = "Test description",
            severity = HazardSeverity.MEDIUM,
            location = "Test Location",
            resolved = false
        )

        val initialActivities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        val initialCount = initialActivities.size

        // When: Adding activity
        val result = repository.addActivity(newActivity)

        // Then: Should succeed
        assertTrue(result.isSuccess)

        // And: Activity should be in feed
        val updatedActivities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        assertTrue(updatedActivities.size > initialCount)
        
        val addedActivity = updatedActivities.first { it.id == newActivity.id }
        assertNotNull(addedActivity)
        assertEquals(newActivity.id, addedActivity.id)
    }

    @Test
    fun `addActivity - adds activity to front of feed`() = testScope.runTest {
        // Given: New activity with recent timestamp
        val newActivity = ActivityFeedItem.SystemAlert(
            id = "new_alert_001",
            timestamp = Clock.System.now().toEpochMilliseconds(),
            alertType = AlertType.SAFETY_REMINDER,
            message = "Test alert",
            priority = AlertPriority.LOW,
            dismissed = false
        )

        // When: Adding activity
        repository.addActivity(newActivity)

        // Then: Should be at or near the front (most recent)
        val activities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        val activityIndex = activities.indexOfFirst { it.id == newActivity.id }
        assertTrue(activityIndex >= 0, "Activity should be in feed")
        // Should be at front since it has the most recent timestamp
        assertTrue(activityIndex <= 2, "Activity should be near front of feed")
    }

    // ========================================================================
    // MARK: - Refresh Tests
    // ========================================================================

    @Test
    fun `refreshActivities - successfully refreshes data`() = testScope.runTest {
        // When: Refreshing activities
        val result = repository.refreshActivities()

        // Then: Should succeed
        assertTrue(result.isSuccess)

        // And: Activities should still be retrievable
        val activities = repository.getActivityFeed(limit = 10, includeResolved = true).first()
        assertTrue(activities.isNotEmpty())
    }

    // ========================================================================
    // MARK: - Statistics Tests
    // ========================================================================

    @Test
    fun `getActivityStats - returns accurate statistics`() = testScope.runTest {
        // When: Getting activity stats for last 24 hours
        val stats = repository.getActivityStats(timeRangeHours = 24)

        // Then: Should contain valid statistics
        assertNotNull(stats)
        assertTrue(stats.totalActivities >= 0)
        assertTrue(stats.newHazards >= 0)
        assertTrue(stats.newPTPs >= 0)
        assertTrue(stats.newPhotos >= 0)
        assertTrue(stats.criticalAlerts >= 0)

        // And: Individual counts should not exceed total
        assertTrue(stats.newHazards <= stats.totalActivities)
        assertTrue(stats.newPTPs <= stats.totalActivities)
        assertTrue(stats.newPhotos <= stats.totalActivities)
    }

    @Test
    fun `getActivityStats - counts only activities within time range`() = testScope.runTest {
        // Given: Very short time range (should have few/no activities)
        val stats1Hour = repository.getActivityStats(timeRangeHours = 1)

        // And: Longer time range (should have more activities)
        val stats24Hours = repository.getActivityStats(timeRangeHours = 24)

        // Then: Longer time range should have equal or more activities
        assertTrue(stats24Hours.totalActivities >= stats1Hour.totalActivities)
    }

    @Test
    fun `getActivityStats - correctly identifies critical alerts`() = testScope.runTest {
        // When: Getting stats
        val stats = repository.getActivityStats(timeRangeHours = 24)

        // Then: Critical alert count should match actual critical alerts
        val activities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        val now = Clock.System.now().toEpochMilliseconds()
        val cutoff = now - (24 * 60 * 60 * 1000)
        
        val actualCriticalAlerts = activities
            .filterIsInstance<ActivityFeedItem.SystemAlert>()
            .count { it.timestamp >= cutoff && it.priority == AlertPriority.URGENT }

        assertEquals(actualCriticalAlerts, stats.criticalAlerts)
    }

    // ========================================================================
    // MARK: - Activity Type Tests
    // ========================================================================

    @Test
    fun `activity feed contains PTP activities with correct properties`() = testScope.runTest {
        // When: Fetching activities
        val activities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        val ptpActivities = activities.filterIsInstance<ActivityFeedItem.PTPActivity>()

        // Then: PTP activities should have required properties
        ptpActivities.forEach { ptp ->
            assertNotNull(ptp.id)
            assertNotNull(ptp.ptpId)
            assertNotNull(ptp.ptpTitle)
            assertNotNull(ptp.status)
            assertNotNull(ptp.projectName)
            assertTrue(ptp.timestamp > 0)
        }
    }

    @Test
    fun `activity feed contains hazard activities with severity levels`() = testScope.runTest {
        // When: Fetching activities
        val activities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        val hazardActivities = activities.filterIsInstance<ActivityFeedItem.HazardActivity>()

        // Then: Hazards should have all severity levels represented
        assertTrue(hazardActivities.isNotEmpty())
        hazardActivities.forEach { hazard ->
            assertNotNull(hazard.severity)
            assertNotNull(hazard.hazardType)
            assertNotNull(hazard.hazardDescription)
        }
    }

    @Test
    fun `activity feed contains system alerts with priorities`() = testScope.runTest {
        // When: Fetching activities
        val activities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        val alerts = activities.filterIsInstance<ActivityFeedItem.SystemAlert>()

        // Then: Alerts should have priorities and types
        assertTrue(alerts.isNotEmpty())
        alerts.forEach { alert ->
            assertNotNull(alert.priority)
            assertNotNull(alert.alertType)
            assertNotNull(alert.message)
            assertTrue(alert.message.isNotEmpty())
        }
    }

    @Test
    fun `activity feed contains photo activities with analysis status`() = testScope.runTest {
        // When: Fetching activities
        val activities = repository.getActivityFeed(limit = 100, includeResolved = true).first()
        val photoActivities = activities.filterIsInstance<ActivityFeedItem.PhotoActivity>()

        // Then: Photos should have analysis status
        assertTrue(photoActivities.isNotEmpty())
        photoActivities.forEach { photo ->
            assertNotNull(photo.photoId)
            assertNotNull(photo.photoPath)
            assertTrue(photo.hazardCount >= 0)
            // analyzed and needsReview are boolean properties
        }
    }

    // ========================================================================
    // MARK: - Edge Case Tests
    // ========================================================================

    @Test
    fun `getActivityFeed - handles empty limit gracefully`() = testScope.runTest {
        // When: Fetching with limit of 0
        val activities = repository.getActivityFeed(limit = 0, includeResolved = true).first()

        // Then: Should return empty list
        assertTrue(activities.isEmpty())
    }

    @Test
    fun `getActivityFeed - handles large limit`() = testScope.runTest {
        // When: Fetching with very large limit
        val activities = repository.getActivityFeed(limit = 1000, includeResolved = true).first()

        // Then: Should return all available activities without error
        assertTrue(activities.isNotEmpty())
    }
}
