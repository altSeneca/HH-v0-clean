package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable

@Serializable
enum class CrewMemberRole {
    CREW_LEAD,
    FOREMAN,
    MEMBER
}
