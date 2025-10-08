package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class CompanyWorker(
    val id: String,
    val companyId: String,
    val workerProfileId: String,
    val employeeNumber: String,
    val role: WorkerRole,
    val hireDate: LocalDate,
    val status: WorkerStatus,
    val hourlyRate: Double? = null,
    val permissions: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String,

    // Embedded for convenience
    val workerProfile: WorkerProfile? = null,
    val certifications: List<WorkerCertification> = emptyList(),
    val crews: List<CrewMembership> = emptyList()
)
