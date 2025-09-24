package com.hazardhawk.models

/**
 * User role definitions with integrated permission system
 * Defines the three-tier access model for construction site safety management
 */
enum class UserRole(val displayName: String, val permissions: Set<Permission>) {
    FIELD_ACCESS(
        displayName = "Field Worker",
        permissions = setOf(
            Permission.CAPTURE_PHOTOS,
            Permission.VIEW_ANALYSIS,
            Permission.VIEW_DOCUMENTS
        )
    ),
    SAFETY_LEAD(
        displayName = "Safety Lead", 
        permissions = setOf(
            Permission.CAPTURE_PHOTOS,
            Permission.VIEW_ANALYSIS,
            Permission.VIEW_DOCUMENTS,
            Permission.GENERATE_PTP,
            Permission.GENERATE_TOOLBOX_TALK,
            Permission.GENERATE_INCIDENT_REPORT,
            Permission.EXPORT_PDF
        )
    ),
    PROJECT_ADMIN(
        displayName = "Project Administrator",
        permissions = setOf(*Permission.values()) // All permissions
    );
    
    /**
     * Check if this role has the specified permission
     */
    fun hasPermission(permission: Permission): Boolean {
        return permissions.contains(permission)
    }
    
    /**
     * Check if this role has all the specified permissions
     */
    fun hasAllPermissions(vararg permissions: Permission): Boolean {
        return this.permissions.containsAll(permissions.toList())
    }
    
    /**
     * Check if this role has any of the specified permissions
     */
    fun hasAnyPermission(vararg permissions: Permission): Boolean {
        return permissions.any { this.permissions.contains(it) }
    }
}

/**
 * Permission system for granular access control
 * Maps to specific features and capabilities within HazardHawk
 */
enum class Permission {
    CAPTURE_PHOTOS,
    VIEW_ANALYSIS,
    VIEW_DOCUMENTS,
    GENERATE_PTP,
    GENERATE_TOOLBOX_TALK,
    GENERATE_INCIDENT_REPORT,
    EXPORT_PDF,
    MANAGE_USERS,
    VIEW_ANALYTICS
}