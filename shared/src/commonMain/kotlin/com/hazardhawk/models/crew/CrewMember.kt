package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class CrewMember(
    val id: String,
    val crewId: String,
    val companyWorkerId: String,
    val role: CrewMemberRole,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val status: String,

    // Embedded for convenience
    val worker: CompanyWorker? = null
)
