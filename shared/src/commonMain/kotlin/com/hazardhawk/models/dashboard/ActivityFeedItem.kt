package com.hazardhawk.models.dashboard

import com.hazardhawk.documents.models.PTPDocument
import kotlinx.serialization.Serializable

/**
 * Sealed class representing different types of items in the activity feed.
 * Each type has specific properties and actions relevant to that activity.
 */
sealed class ActivityFeedItem {
    abstract val id: String
    abstract val timestamp: Long

    /**
     * Pre-Task Plan activity - PTPs created, edited, or approved
     */
    data class PTPActivity(
        override val id: String,
        override val timestamp: Long,
        val ptpId: String,
        val ptpTitle: String,
        val status: PTPStatus,
        val projectName: String,
        val createdBy: String? = null
    ) : ActivityFeedItem()

    /**
     * Hazard detection from AI analysis
     */
    data class HazardActivity(
        override val id: String,
        override val timestamp: Long,
        val hazardId: String,
        val hazardType: String,
        val hazardDescription: String,
        val severity: HazardSeverity,
        val location: String? = null,
        val oshaCode: String? = null,
        val photoId: String? = null,
        val resolved: Boolean = false
    ) : ActivityFeedItem()

    /**
     * Toolbox Talk completion (to be implemented)
     */
    data class ToolboxTalkActivity(
        override val id: String,
        override val timestamp: Long,
        val talkId: String,
        val talkTitle: String,
        val topic: String,
        val attendeeCount: Int,
        val conductedBy: String
    ) : ActivityFeedItem()

    /**
     * Photos captured awaiting review or analysis
     */
    data class PhotoActivity(
        override val id: String,
        override val timestamp: Long,
        val photoId: String,
        val photoPath: String,
        val location: String? = null,
        val needsReview: Boolean = true,
        val analyzed: Boolean = false,
        val hazardCount: Int = 0
    ) : ActivityFeedItem()

    /**
     * System alerts and notifications
     */
    data class SystemAlert(
        override val id: String,
        override val timestamp: Long,
        val alertType: AlertType,
        val message: String,
        val priority: AlertPriority,
        val actionRequired: Boolean = false,
        val dismissed: Boolean = false
    ) : ActivityFeedItem()
}

/**
 * Status of a Pre-Task Plan
 */
enum class PTPStatus {
    DRAFT,          // Created but not finalized
    PENDING_REVIEW, // Submitted for approval
    APPROVED,       // Approved by safety lead
    ACTIVE,         // Currently in use on site
    COMPLETED,      // Task completed
    ARCHIVED        // Historical record
}

/**
 * Hazard severity levels matching OSHA classification
 */
enum class HazardSeverity {
    CRITICAL,   // Immediate danger - red
    HIGH,       // Serious hazard - orange
    MEDIUM,     // Moderate risk - yellow
    LOW         // Minor concern - green
}

/**
 * Types of system alerts
 */
enum class AlertType {
    OSHA_UPDATE,        // New OSHA regulations or updates
    SAFETY_REMINDER,    // Periodic safety reminders
    SYSTEM_UPDATE,      // App updates or features
    COMPLIANCE,         // Compliance deadlines or requirements
    INCIDENT,           // Incident reports or follow-ups
    WEATHER             // Weather-related safety alerts
}

/**
 * Priority level for alerts
 */
enum class AlertPriority {
    URGENT,     // Requires immediate attention
    HIGH,       // Important but not time-critical
    MEDIUM,     // Standard notification
    LOW         // Informational only
}

/**
 * Action types for activity feed items
 */
enum class FeedActionType {
    VIEW,           // View details
    EDIT,           // Edit item
    SHARE,          // Share with team
    EXPORT,         // Export as PDF
    ANALYZE,        // Run AI analysis
    ASSIGN,         // Assign to team member
    RESOLVE,        // Mark as resolved
    DELETE,         // Delete item
    DISMISS         // Dismiss notification
}
