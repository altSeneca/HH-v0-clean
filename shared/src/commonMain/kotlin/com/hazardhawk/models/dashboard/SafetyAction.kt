package com.hazardhawk.models.dashboard

/**
 * Safety actions available from the command center.
 * Each action corresponds to a major feature of HazardHawk.
 */
enum class SafetyAction {
    /**
     * Create a new Pre-Task Plan
     * Requires: Safety Lead or Project Admin tier
     */
    CREATE_PTP,

    /**
     * Create a new Toolbox Talk
     * Requires: Safety Lead or Project Admin tier
     * Status: Coming soon - not yet implemented
     */
    CREATE_TOOLBOX_TALK,

    /**
     * Capture photos for safety analysis
     * Requires: All user tiers
     */
    CAPTURE_PHOTO,

    /**
     * View safety reports and analytics
     * Requires: All user tiers (field access = read-only)
     */
    VIEW_REPORTS,

    /**
     * Assign tasks to crew members
     * Requires: Safety Lead or Project Admin tier
     */
    ASSIGN_TASKS,

    /**
     * Open photo gallery
     * Requires: All user tiers
     */
    OPEN_GALLERY,

    /**
     * Report an incident
     * Requires: All user tiers
     * Status: Coming soon - not yet implemented
     */
    REPORT_INCIDENT,

    /**
     * Start pre-shift meeting
     * Requires: Safety Lead or Project Admin tier
     * Status: Coming soon - not yet implemented
     */
    START_PRE_SHIFT,

    /**
     * Access live detection AR camera
     * Requires: All user tiers
     */
    LIVE_DETECTION,

    /**
     * View and manage team/crew
     * Requires: Safety Lead or Project Admin tier
     */
    MANAGE_CREW
}

/**
 * User tier/role for permission checking
 */
enum class UserTier {
    /**
     * Field Access - Basic access for field workers
     * Can: Capture photos, view analysis, read-only docs
     * Cannot: Generate PTPs, toolbox talks, assign tasks
     */
    FIELD_ACCESS,

    /**
     * Safety Lead - Extended access for safety supervisors
     * Can: All Field Access + Generate PTPs, toolbox talks, assign tasks
     */
    SAFETY_LEAD,

    /**
     * Project Admin - Full access for project management
     * Can: All Safety Lead + Analytics, user management, settings
     */
    PROJECT_ADMIN
}

/**
 * Command center button configuration
 */
data class CommandCenterButton(
    val action: SafetyAction,
    val title: String,
    val subtitle: String,
    val iconName: String,           // Icon identifier
    val backgroundColor: String,    // Hex color code
    val requiredTier: UserTier,    // Minimum tier required
    val enabled: Boolean = true,    // Feature implemented and enabled
    val comingSoon: Boolean = false, // Feature not yet available
    val notificationBadge: Int = 0  // Badge count
)

/**
 * Extension functions for permission checking
 */
fun SafetyAction.getRequiredTier(): UserTier {
    return when (this) {
        SafetyAction.CREATE_PTP,
        SafetyAction.CREATE_TOOLBOX_TALK,
        SafetyAction.ASSIGN_TASKS,
        SafetyAction.REPORT_INCIDENT,
        SafetyAction.START_PRE_SHIFT,
        SafetyAction.MANAGE_CREW -> UserTier.SAFETY_LEAD

        SafetyAction.CAPTURE_PHOTO,
        SafetyAction.VIEW_REPORTS,
        SafetyAction.OPEN_GALLERY,
        SafetyAction.LIVE_DETECTION -> UserTier.FIELD_ACCESS
    }
}

fun SafetyAction.isAvailableForTier(userTier: UserTier): Boolean {
    val requiredTier = getRequiredTier()
    return when (userTier) {
        UserTier.PROJECT_ADMIN -> true // Admins have access to everything
        UserTier.SAFETY_LEAD -> requiredTier != UserTier.PROJECT_ADMIN || requiredTier == UserTier.SAFETY_LEAD || requiredTier == UserTier.FIELD_ACCESS
        UserTier.FIELD_ACCESS -> requiredTier == UserTier.FIELD_ACCESS
    }
}

fun SafetyAction.isImplemented(): Boolean {
    return when (this) {
        SafetyAction.CREATE_PTP,
        SafetyAction.CAPTURE_PHOTO,
        SafetyAction.VIEW_REPORTS,
        SafetyAction.OPEN_GALLERY,
        SafetyAction.LIVE_DETECTION -> true

        SafetyAction.CREATE_TOOLBOX_TALK,
        SafetyAction.ASSIGN_TASKS,
        SafetyAction.REPORT_INCIDENT,
        SafetyAction.START_PRE_SHIFT,
        SafetyAction.MANAGE_CREW -> false // Coming soon
    }
}

/**
 * Get button configuration for a safety action
 */
fun SafetyAction.toButtonConfig(): CommandCenterButton {
    return when (this) {
        SafetyAction.CREATE_PTP -> CommandCenterButton(
            action = this,
            title = "Create Pre-Task Plan",
            subtitle = "Plan work tasks and identify hazards",
            iconName = "assignment",
            backgroundColor = "#FF6B35", // Safety Orange
            requiredTier = UserTier.SAFETY_LEAD,
            enabled = true
        )

        SafetyAction.CREATE_TOOLBOX_TALK -> CommandCenterButton(
            action = this,
            title = "Create Toolbox Talk",
            subtitle = "Generate safety meeting content",
            iconName = "construction",
            backgroundColor = "#4CAF50", // Safety Green
            requiredTier = UserTier.SAFETY_LEAD,
            enabled = false,
            comingSoon = true
        )

        SafetyAction.CAPTURE_PHOTO -> CommandCenterButton(
            action = this,
            title = "Capture Photos",
            subtitle = "Document site conditions and hazards",
            iconName = "camera_alt",
            backgroundColor = "#0066CC", // OSHA Blue
            requiredTier = UserTier.FIELD_ACCESS,
            enabled = true
        )

        SafetyAction.VIEW_REPORTS -> CommandCenterButton(
            action = this,
            title = "View Reports",
            subtitle = "Access safety reports and analytics",
            iconName = "assessment",
            backgroundColor = "#9C27B0", // Material Purple
            requiredTier = UserTier.FIELD_ACCESS,
            enabled = true
        )

        SafetyAction.ASSIGN_TASKS -> CommandCenterButton(
            action = this,
            title = "Assign Tasks/Crews",
            subtitle = "Manage crew assignments and tasks",
            iconName = "group",
            backgroundColor = "#FFA500", // Amber
            requiredTier = UserTier.SAFETY_LEAD,
            enabled = false,
            comingSoon = true
        )

        SafetyAction.OPEN_GALLERY -> CommandCenterButton(
            action = this,
            title = "Photo Gallery",
            subtitle = "Browse and manage safety photos",
            iconName = "photo_library",
            backgroundColor = "#607D8B", // Material Grey
            requiredTier = UserTier.FIELD_ACCESS,
            enabled = true
        )

        SafetyAction.REPORT_INCIDENT -> CommandCenterButton(
            action = this,
            title = "Report Incident",
            subtitle = "Document safety incidents",
            iconName = "warning",
            backgroundColor = "#F44336", // Red
            requiredTier = UserTier.FIELD_ACCESS,
            enabled = false,
            comingSoon = true
        )

        SafetyAction.START_PRE_SHIFT -> CommandCenterButton(
            action = this,
            title = "Pre-Shift Meeting",
            subtitle = "Start daily safety briefing",
            iconName = "schedule",
            backgroundColor = "#FF9800", // Deep Orange
            requiredTier = UserTier.SAFETY_LEAD,
            enabled = false,
            comingSoon = true
        )

        SafetyAction.LIVE_DETECTION -> CommandCenterButton(
            action = this,
            title = "Live Detection",
            subtitle = "Real-time AR hazard detection",
            iconName = "visibility",
            backgroundColor = "#00BCD4", // Cyan
            requiredTier = UserTier.FIELD_ACCESS,
            enabled = true
        )

        SafetyAction.MANAGE_CREW -> CommandCenterButton(
            action = this,
            title = "Manage Crew",
            subtitle = "View and manage team members",
            iconName = "people",
            backgroundColor = "#795548", // Brown
            requiredTier = UserTier.SAFETY_LEAD,
            enabled = false,
            comingSoon = true
        )
    }
}

/**
 * Get default command center button set for 2x3 grid
 */
fun getDefaultCommandCenterButtons(): List<SafetyAction> {
    return listOf(
        SafetyAction.CREATE_PTP,
        SafetyAction.CREATE_TOOLBOX_TALK,
        SafetyAction.CAPTURE_PHOTO,
        SafetyAction.VIEW_REPORTS,
        SafetyAction.ASSIGN_TASKS,
        SafetyAction.OPEN_GALLERY
    )
}

/**
 * Filter buttons based on user tier
 */
fun List<SafetyAction>.filterByTier(userTier: UserTier): List<SafetyAction> {
    return this.filter { it.isAvailableForTier(userTier) }
}
