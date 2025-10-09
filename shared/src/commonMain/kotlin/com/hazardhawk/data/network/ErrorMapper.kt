package com.hazardhawk.data.network

/**
 * Maps API exceptions to user-friendly error messages
 *
 * Provides clear, actionable error messages for end users
 * instead of technical error details.
 */
object ErrorMapper {

    /**
     * Convert exception to user-friendly message
     */
    fun toUserMessage(error: Throwable): String {
        return when (error) {
            is ApiException.BadRequest -> {
                "The information provided was invalid. Please check and try again."
            }
            is ApiException.Unauthorized -> {
                "You need to sign in to perform this action."
            }
            is ApiException.Forbidden -> {
                "You don't have permission to perform this action."
            }
            is ApiException.NotFound -> {
                "The requested item could not be found. It may have been deleted."
            }
            is ApiException.Conflict -> {
                "This action conflicts with existing data. Please review and try again."
            }
            is ApiException.ValidationError -> {
                parseValidationError(error.message ?: "Validation failed")
            }
            is ApiException.ServerError -> {
                "We're experiencing technical difficulties. Please try again later."
            }
            is ApiException.NetworkError -> {
                "Unable to connect to the server. Please check your internet connection."
            }
            else -> {
                "An unexpected error occurred. Please try again."
            }
        }
    }

    /**
     * Parse validation error message to extract user-friendly text
     */
    private fun parseValidationError(message: String): String {
        // Try to extract meaningful validation message
        return when {
            message.contains("name", ignoreCase = true) -> {
                "Please provide a valid name."
            }
            message.contains("email", ignoreCase = true) -> {
                "Please provide a valid email address."
            }
            message.contains("phone", ignoreCase = true) -> {
                "Please provide a valid phone number."
            }
            message.contains("required", ignoreCase = true) -> {
                "Some required fields are missing. Please check the form."
            }
            message.contains("duplicate", ignoreCase = true) -> {
                "This item already exists. Please use a different name."
            }
            else -> {
                "The information provided is invalid. Please review and try again."
            }
        }
    }

    /**
     * Get error message with details (for logging/debugging)
     */
    fun toDetailedMessage(error: Throwable): String {
        val userMessage = toUserMessage(error)
        val technicalMessage = error.message ?: "Unknown error"
        return "$userMessage\nDetails: $technicalMessage"
    }

    /**
     * Specific error messages for crew operations
     */
    object CrewErrors {
        const val CREW_NOT_FOUND = "The crew could not be found. It may have been deleted or archived."
        const val CREW_NAME_EMPTY = "Crew name is required."
        const val CREW_NAME_TOO_SHORT = "Crew name must be at least 3 characters."
        const val CREW_NAME_TOO_LONG = "Crew name must be less than 100 characters."
        const val CREW_ALREADY_EXISTS = "A crew with this name already exists."
        const val CREW_HAS_MEMBERS = "Cannot delete crew with active members. Remove members first."
        const val MEMBER_ALREADY_IN_CREW = "This worker is already a member of this crew."
        const val MEMBER_NOT_FOUND = "The crew member could not be found."
        const val FOREMAN_REQUIRED = "A foreman must be assigned to this crew."
        const val INVALID_CREW_TYPE = "Invalid crew type selected."
        const val PROJECT_ASSIGNMENT_FAILED = "Failed to assign crew to project. Please try again."
        const val QR_GENERATION_FAILED = "Failed to generate QR code. Please try again."
        const val ROLE_SYNC_FAILED = "Failed to synchronize roles. Please try again."
    }

    /**
     * Success messages for crew operations
     */
    object CrewSuccess {
        const val CREW_CREATED = "Crew created successfully!"
        const val CREW_UPDATED = "Crew updated successfully!"
        const val CREW_DELETED = "Crew deleted successfully!"
        const val MEMBER_ADDED = "Member added to crew successfully!"
        const val MEMBER_REMOVED = "Member removed from crew successfully!"
        const val MEMBER_ROLE_UPDATED = "Member role updated successfully!"
        const val ASSIGNED_TO_PROJECT = "Crew assigned to project successfully!"
        const val UNASSIGNED_FROM_PROJECT = "Crew unassigned from project successfully!"
        const val QR_GENERATED = "QR code generated successfully!"
        const val ROLES_SYNCED = "Crew roles synchronized successfully!"
    }

    /**
     * Helper to wrap Result with user-friendly error
     */
    fun <T> Result<T>.mapError(): Result<T> {
        return this.onFailure { error ->
            // Log detailed error for debugging
            println("API Error: ${toDetailedMessage(error)}")
        }
    }
}
