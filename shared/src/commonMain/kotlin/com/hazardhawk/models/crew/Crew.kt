package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable

@Serializable
data class Crew(
    val id: String,
    val companyId: String,
    val projectId: String? = null,
    val name: String,
    val crewType: CrewType,
    val trade: String? = null,
    val foremanId: String? = null,
    val location: String? = null,
    val status: CrewStatus,
    val createdAt: String,
    val updatedAt: String,

    // Embedded for convenience
    val members: List<CrewMember> = emptyList(),
    val foreman: CompanyWorker? = null
) {
    val memberCount: Int get() = members.size
    val isActive: Boolean get() = status == CrewStatus.ACTIVE
}
