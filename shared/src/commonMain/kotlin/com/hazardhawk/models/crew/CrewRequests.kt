package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

// ========== Request/Response Models ==========

@Serializable
data class CreateWorkerRequest(
    val employeeNumber: String,
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val phone: String? = null,
    val dateOfBirth: LocalDate? = null,
    val role: WorkerRole,
    val hireDate: LocalDate,
    val hourlyRate: Double? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class UpdateWorkerRequest(
    val employeeNumber: String? = null,
    val role: WorkerRole? = null,
    val status: WorkerStatus? = null,
    val hourlyRate: Double? = null,
    val permissions: List<String>? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
data class WorkerFilters(
    val status: WorkerStatus? = null,
    val role: WorkerRole? = null,
    val crewId: String? = null,
    val search: String? = null
)

@Serializable
data class CreateCrewRequest(
    val name: String,
    val projectId: String? = null,
    val crewType: CrewType,
    val trade: String? = null,
    val foremanId: String? = null,
    val location: String? = null
)

@Serializable
data class UpdateCrewRequest(
    val name: String? = null,
    val foremanId: String? = null,
    val location: String? = null,
    val status: CrewStatus? = null
)

@Serializable
data class CreateCertificationRequest(
    val certificationTypeId: String,
    val issueDate: LocalDate,
    val expirationDate: LocalDate? = null,
    val issuingAuthority: String? = null,
    val certificationNumber: String? = null,
    val documentUrl: String
)

@Serializable
data class UpdateProjectRequest(
    val name: String? = null,
    val projectNumber: String? = null,
    val status: ProjectStatus? = null,
    val clientName: String? = null,
    val streetAddress: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val generalContractor: String? = null
)
