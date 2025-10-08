package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class CrewMembership(
    val crewId: String,
    val crewName: String,
    val role: CrewMemberRole,
    val startDate: LocalDate,
    val projectName: String? = null
)
