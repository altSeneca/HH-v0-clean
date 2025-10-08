package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable

/**
 * Project Status enumeration
 */
@Serializable
enum class ProjectStatus {
    ACTIVE,
    COMPLETED,
    ON_HOLD,
    CANCELLED;

    val displayName: String
        get() = when (this) {
            ACTIVE -> "Active"
            COMPLETED -> "Completed"
            ON_HOLD -> "On Hold"
            CANCELLED -> "Cancelled"
        }

    val isActive: Boolean
        get() = this == ACTIVE

    companion object {
        fun fromString(status: String): ProjectStatus {
            return values().find {
                it.name.equals(status, ignoreCase = true) ||
                it.displayName.equals(status, ignoreCase = true)
            } ?: ACTIVE
        }
    }
}
