package com.hazardhawk.models.dashboard

import kotlinx.serialization.Serializable

/**
 * Notification badge counts for bottom navigation bar items.
 * Each navigation item can display a badge with a count or boolean indicator.
 */
@Serializable
data class NavigationNotifications(
    val homeAlerts: Int = 0,            // Critical alerts on home screen
    val captureReminders: Int = 0,      // Pending capture tasks
    val safetyPending: Int = 0,         // Pending PTPs, toolbox talks, incidents
    val galleryUnreviewed: Int = 0,     // Photos needing review or analysis
    val profileAttention: Boolean = false // Settings requiring attention
) {
    /**
     * Total notification count across all navigation items
     */
    val totalCount: Int
        get() = homeAlerts + captureReminders + safetyPending + galleryUnreviewed + (if (profileAttention) 1 else 0)

    /**
     * Check if there are any notifications
     */
    val hasNotifications: Boolean
        get() = totalCount > 0

    /**
     * Check if there are critical notifications (home alerts)
     */
    val hasCriticalNotifications: Boolean
        get() = homeAlerts > 0
}

/**
 * Detailed breakdown of safety-related notifications
 */
@Serializable
data class SafetyNotificationBreakdown(
    val pendingPTPs: Int = 0,           // PTPs awaiting approval or completion
    val draftPTPs: Int = 0,             // PTPs in draft state
    val activePTPs: Int = 0,            // PTPs currently active on site
    val pendingIncidents: Int = 0,      // Open incident reports
    val pendingToolboxTalks: Int = 0,   // Upcoming toolbox talks
    val complianceDeadlines: Int = 0    // Approaching compliance deadlines
) {
    /**
     * Total safety notification count
     */
    val total: Int
        get() = pendingPTPs + draftPTPs + activePTPs + pendingIncidents + pendingToolboxTalks + complianceDeadlines
}

/**
 * Detailed breakdown of gallery notifications
 */
@Serializable
data class GalleryNotificationBreakdown(
    val unanalyzedPhotos: Int = 0,      // Photos not yet analyzed by AI
    val photosWithHazards: Int = 0,     // Photos with detected hazards
    val unresolvedHazards: Int = 0,     // Hazards not yet resolved
    val photosPendingTag: Int = 0       // Photos without proper tags
) {
    /**
     * Total gallery notification count
     */
    val total: Int
        get() = unanalyzedPhotos + photosWithHazards + unresolvedHazards + photosPendingTag
}

/**
 * Types of profile attention indicators
 */
enum class ProfileAttentionType {
    UPDATE_REQUIRED,        // App update available
    PROFILE_INCOMPLETE,     // User profile needs completion
    CERTIFICATION_EXPIRING, // Safety certifications expiring
    PERMISSIONS_NEEDED,     // App permissions required
    SETTINGS_REVIEW         // Settings need review
}

/**
 * Profile attention details
 */
@Serializable
data class ProfileAttention(
    val type: ProfileAttentionType,
    val message: String,
    val actionRequired: Boolean = false,
    val deadline: Long? = null
)

/**
 * Extension functions for notification management
 */
fun NavigationNotifications.getSafetyBreakdown(): SafetyNotificationBreakdown {
    // This would typically be fetched from repository
    // Placeholder implementation
    return SafetyNotificationBreakdown(
        pendingPTPs = safetyPending
    )
}

fun NavigationNotifications.getGalleryBreakdown(): GalleryNotificationBreakdown {
    // This would typically be fetched from repository
    // Placeholder implementation
    return GalleryNotificationBreakdown(
        unanalyzedPhotos = galleryUnreviewed
    )
}

/**
 * Helper function to format badge text
 */
fun Int.toBadgeText(): String {
    return when {
        this == 0 -> ""
        this > 99 -> "99+"
        else -> this.toString()
    }
}
