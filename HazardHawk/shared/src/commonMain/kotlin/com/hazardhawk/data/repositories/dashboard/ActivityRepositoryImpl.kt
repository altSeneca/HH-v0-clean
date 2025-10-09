package com.hazardhawk.data.repositories.dashboard

import com.hazardhawk.models.dashboard.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

/**
 * Implementation of ActivityRepository for the home dashboard.
 *
 * Phase 1: Mock implementation with sample data for UI development
 * Phase 5: TODO - Replace with real data sources
 *
 * This repository aggregates activities from multiple sources:
 * - Pre-Task Plans (PTPs)
 * - Hazard detections from AI analysis
 * - Toolbox talks
 * - Photos awaiting review
 * - System alerts and notifications
 */
class ActivityRepositoryImpl {

    // In-memory storage for Phase 1 mock data
    private val mockActivities = mutableListOf<ActivityFeedItem>()

    init {
        // Initialize with sample data for development
        generateMockData()
    }

    /**
     * Get activity feed as a Flow for reactive updates
     *
     * @param limit Maximum number of items to return
     * @param includeResolved Include resolved hazards and dismissed alerts
     * @return Flow of activity feed items sorted by timestamp (newest first)
     */
    fun getActivityFeed(
        limit: Int = 10,
        includeResolved: Boolean = false
    ): Flow<List<ActivityFeedItem>> = flow {
        // Simulate network delay
        delay(500)

        val filtered = mockActivities
            .filter { item ->
                when (item) {
                    is ActivityFeedItem.HazardActivity -> includeResolved || !item.resolved
                    is ActivityFeedItem.SystemAlert -> includeResolved || !item.dismissed
                    else -> true
                }
            }
            .sortedByDescending { it.timestamp }
            .take(limit)

        emit(filtered)

        // TODO Phase 5: Replace with real data sources
        // - Query PTP database for recent PTPs
        // - Query analysis database for recent hazards
        // - Query photo database for photos needing review
        // - Query notification service for system alerts
        // - Combine and sort by timestamp
        // - Emit updates via real-time subscription
    }

    /**
     * Get activity feed for a specific project
     *
     * @param projectId Project identifier
     * @param limit Maximum number of items to return
     * @return Flow of activity feed items for the project
     */
    fun getActivityFeedForProject(
        projectId: String,
        limit: Int = 10
    ): Flow<List<ActivityFeedItem>> = flow {
        delay(300)

        // Filter by project (mock implementation)
        val filtered = mockActivities
            .sortedByDescending { it.timestamp }
            .take(limit)

        emit(filtered)

        // TODO Phase 5: Add project-based filtering from database
    }

    /**
     * Get count of unread/pending activities
     *
     * @return Number of items requiring user attention
     */
    suspend fun getUnreadCount(): Int {
        // Count unresolved hazards and undismissed alerts
        return mockActivities.count { item ->
            when (item) {
                is ActivityFeedItem.HazardActivity -> !item.resolved
                is ActivityFeedItem.SystemAlert -> !item.dismissed && item.actionRequired
                is ActivityFeedItem.PhotoActivity -> item.needsReview
                is ActivityFeedItem.PTPActivity -> item.status == PTPStatus.PENDING_REVIEW
                else -> false
            }
        }

        // TODO Phase 5: Query real database for counts
    }

    /**
     * Mark a hazard as resolved
     *
     * @param hazardId Hazard identifier
     * @return Success/failure result
     */
    suspend fun markHazardResolved(hazardId: String): Result<Unit> {
        return try {
            val index = mockActivities.indexOfFirst {
                it is ActivityFeedItem.HazardActivity && it.hazardId == hazardId
            }

            if (index != -1) {
                val hazard = mockActivities[index] as ActivityFeedItem.HazardActivity
                mockActivities[index] = hazard.copy(resolved = true)
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Hazard not found: $hazardId"))
            }

            // TODO Phase 5: Update hazard status in database
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dismiss a system alert
     *
     * @param alertId Alert identifier
     * @return Success/failure result
     */
    suspend fun dismissAlert(alertId: String): Result<Unit> {
        return try {
            val index = mockActivities.indexOfFirst {
                it is ActivityFeedItem.SystemAlert && it.id == alertId
            }

            if (index != -1) {
                val alert = mockActivities[index] as ActivityFeedItem.SystemAlert
                mockActivities[index] = alert.copy(dismissed = true)
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Alert not found: $alertId"))
            }

            // TODO Phase 5: Update alert status in database
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add a new activity to the feed
     * Used when new PTPs are created, photos analyzed, etc.
     *
     * @param activity Activity feed item to add
     * @return Success/failure result
     */
    suspend fun addActivity(activity: ActivityFeedItem): Result<Unit> {
        return try {
            mockActivities.add(0, activity) // Add to front
            Result.success(Unit)

            // TODO Phase 5: Insert into database and trigger real-time update
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh activity feed from all sources
     * Triggers a re-fetch from backend services
     *
     * @return Success/failure result
     */
    suspend fun refreshActivities(): Result<Unit> {
        return try {
            delay(800) // Simulate network refresh
            // In Phase 1, just re-sort existing data
            mockActivities.sortByDescending { it.timestamp }
            Result.success(Unit)

            // TODO Phase 5:
            // - Fetch latest PTPs from PTP service
            // - Fetch latest hazards from analysis service
            // - Fetch latest photos from photo service
            // - Fetch latest alerts from notification service
            // - Merge and deduplicate
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get activity statistics for dashboard summary
     *
     * @param timeRangeHours Number of hours to look back
     * @return Summary statistics
     */
    suspend fun getActivityStats(timeRangeHours: Int = 24): ActivityStats {
        val cutoffTime = Clock.System.now().toEpochMilliseconds() - (timeRangeHours * 60 * 60 * 1000)
        val recentActivities = mockActivities.filter { it.timestamp >= cutoffTime }

        return ActivityStats(
            totalActivities = recentActivities.size,
            newHazards = recentActivities.count { it is ActivityFeedItem.HazardActivity },
            newPTPs = recentActivities.count { it is ActivityFeedItem.PTPActivity },
            newPhotos = recentActivities.count { it is ActivityFeedItem.PhotoActivity },
            criticalAlerts = recentActivities.count {
                it is ActivityFeedItem.SystemAlert && it.priority == AlertPriority.URGENT
            }
        )

        // TODO Phase 5: Query database for statistics
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================

    /**
     * Generate mock data for Phase 1 development
     * This will be removed in Phase 5
     */
    private fun generateMockData() {
        val now = Clock.System.now().toEpochMilliseconds()

        // Add sample PTPs
        mockActivities.add(
            ActivityFeedItem.PTPActivity(
                id = "ptp_001",
                timestamp = now - (2 * 60 * 60 * 1000), // 2 hours ago
                ptpId = "ptp_abc123",
                ptpTitle = "Excavation Work - North Site",
                status = PTPStatus.APPROVED,
                projectName = "Downtown Construction",
                createdBy = "John Smith"
            )
        )

        mockActivities.add(
            ActivityFeedItem.PTPActivity(
                id = "ptp_002",
                timestamp = now - (5 * 60 * 60 * 1000), // 5 hours ago
                ptpId = "ptp_xyz789",
                ptpTitle = "Roofing Installation - Building A",
                status = PTPStatus.PENDING_REVIEW,
                projectName = "Downtown Construction",
                createdBy = "Sarah Johnson"
            )
        )

        // Add sample hazards
        mockActivities.add(
            ActivityFeedItem.HazardActivity(
                id = "hazard_001",
                timestamp = now - (1 * 60 * 60 * 1000), // 1 hour ago
                hazardId = "haz_456",
                hazardType = "Fall Hazard",
                hazardDescription = "Unguarded edge detected at elevated work area",
                severity = HazardSeverity.HIGH,
                location = "Building A, Level 3",
                oshaCode = "1926.501(b)(1)",
                photoId = "photo_789",
                resolved = false
            )
        )

        mockActivities.add(
            ActivityFeedItem.HazardActivity(
                id = "hazard_002",
                timestamp = now - (3 * 60 * 60 * 1000), // 3 hours ago
                hazardId = "haz_789",
                hazardType = "Electrical Hazard",
                hazardDescription = "Exposed wiring near work area",
                severity = HazardSeverity.CRITICAL,
                location = "Main Electrical Room",
                oshaCode = "1926.416",
                photoId = "photo_456",
                resolved = true
            )
        )

        // Add sample photos
        mockActivities.add(
            ActivityFeedItem.PhotoActivity(
                id = "photo_001",
                timestamp = now - (30 * 60 * 1000), // 30 minutes ago
                photoId = "photo_123",
                photoPath = "/storage/photos/IMG_20240925_143022.jpg",
                location = "South Wing, Ground Floor",
                needsReview = true,
                analyzed = false,
                hazardCount = 0
            )
        )

        mockActivities.add(
            ActivityFeedItem.PhotoActivity(
                id = "photo_002",
                timestamp = now - (4 * 60 * 60 * 1000), // 4 hours ago
                photoId = "photo_234",
                photoPath = "/storage/photos/IMG_20240925_123045.jpg",
                location = "North Site",
                needsReview = false,
                analyzed = true,
                hazardCount = 2
            )
        )

        // Add sample alerts
        mockActivities.add(
            ActivityFeedItem.SystemAlert(
                id = "alert_001",
                timestamp = now - (6 * 60 * 60 * 1000), // 6 hours ago
                alertType = AlertType.WEATHER,
                message = "Heat advisory in effect: Temperature expected to reach 102°F today. Ensure adequate hydration breaks.",
                priority = AlertPriority.HIGH,
                actionRequired = true,
                dismissed = false
            )
        )

        mockActivities.add(
            ActivityFeedItem.SystemAlert(
                id = "alert_002",
                timestamp = now - (24 * 60 * 60 * 1000), // 1 day ago
                alertType = AlertType.OSHA_UPDATE,
                message = "New OSHA guidance on fall protection updated. Review requirements for elevated work.",
                priority = AlertPriority.MEDIUM,
                actionRequired = false,
                dismissed = false
            )
        )

        // Add sample toolbox talk
        mockActivities.add(
            ActivityFeedItem.ToolboxTalkActivity(
                id = "talk_001",
                timestamp = now - (7 * 60 * 60 * 1000), // 7 hours ago
                talkId = "talk_001",
                talkTitle = "Ladder Safety and Proper Usage",
                topic = "Fall Prevention",
                attendeeCount = 15,
                conductedBy = "Mike Wilson"
            )
        )
    }
}

/**
 * Activity statistics summary
 */
data class ActivityStats(
    val totalActivities: Int,
    val newHazards: Int,
    val newPTPs: Int,
    val newPhotos: Int,
    val criticalAlerts: Int
)
