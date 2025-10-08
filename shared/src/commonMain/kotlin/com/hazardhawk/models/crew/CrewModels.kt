package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

// ========== Worker Models ==========

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
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: String,
    val updatedAt: String,

    // Embedded for convenience
    val workerProfile: WorkerProfile? = null,
    val certifications: List<WorkerCertification> = emptyList(),
    val crews: List<CrewMembership> = emptyList()
)

@Serializable
enum class WorkerStatus {
    ACTIVE,
    INACTIVE,
    TERMINATED;

    val isActive: Boolean get() = this == ACTIVE
}

// ========== Certification Models ==========

@Serializable
data class WorkerCertification(
    val id: String,
    val workerProfileId: String,
    val companyId: String? = null,
    val certificationTypeId: String,
    val certificationNumber: String? = null,
    val issueDate: LocalDate,
    val expirationDate: LocalDate? = null,
    val issuingAuthority: String? = null,
    val documentUrl: String,
    val thumbnailUrl: String? = null,
    val status: CertificationStatus,
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val rejectionReason: String? = null,
    val ocrConfidence: Double? = null,
    val ocrMetadata: Map<String, String> = emptyMap(),
    val createdAt: String,
    val updatedAt: String,

    // Embedded for convenience
    val certificationType: CertificationType? = null
) {
    val isValid: Boolean
        get() = status == CertificationStatus.VERIFIED && !isExpired

    val isExpired: Boolean
        get() = expirationDate?.let {
            it < kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        } ?: false

    val isExpiringSoon: Boolean
        get() {
            val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
            return expirationDate?.let {
                val daysUntilExpiration = it.toEpochDays() - today.toEpochDays()
                daysUntilExpiration in 1..30
            } ?: false
        }
}

@Serializable
enum class CertificationStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    EXPIRED,
    REJECTED
}

@Serializable
data class CertificationType(
    val id: String,
    val code: String, // OSHA_10, OSHA_30, FORKLIFT, etc.
    val name: String,
    val category: String,
    val region: String,
    val typicalDurationMonths: Int? = null,
    val renewalRequired: Boolean = true,
    val description: String? = null,
    val issuingBodies: List<String> = emptyList()
)

// ========== Crew Models ==========

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
    val foreman: CompanyWorker? = null,
    val project: Project? = null
) {
    val memberCount: Int get() = members.size
    val isActive: Boolean get() = status == CrewStatus.ACTIVE
}

@Serializable
enum class CrewType {
    PERMANENT,
    PROJECT_BASED,
    TRADE_SPECIFIC
}

@Serializable
enum class CrewStatus {
    ACTIVE,
    INACTIVE,
    DISBANDED
}

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

@Serializable
enum class CrewMemberRole {
    CREW_LEAD,
    FOREMAN,
    MEMBER
}

@Serializable
data class CrewMembership(
    val crewId: String,
    val crewName: String,
    val role: CrewMemberRole,
    val startDate: LocalDate,
    val projectName: String? = null
)

// ========== Company Models ==========

@Serializable
data class Company(
    val id: String,
    val name: String,
    val subdomain: String,
    val tier: String,
    val maxWorkers: Int = 100,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val phone: String? = null,
    val logoUrl: String? = null,
    val settings: Map<String, String> = emptyMap(),
    val createdAt: String,
    val updatedAt: String
)

// ========== Project Models ==========

@Serializable
data class Project(
    val id: String,
    val companyId: String,
    val name: String,
    val projectNumber: String? = null,
    val location: String? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val status: ProjectStatus,
    val projectManagerId: String? = null,
    val superintendentId: String? = null,

    // Centralized project info
    val clientName: String? = null,
    val clientContact: String? = null,
    val clientPhone: String? = null,
    val clientEmail: String? = null,
    val streetAddress: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val generalContractor: String? = null,

    val metadata: Map<String, String> = emptyMap(),
    val createdAt: String,
    val updatedAt: String,

    // Embedded for convenience
    val company: Company? = null,
    val projectManager: CompanyWorker? = null,
    val superintendent: CompanyWorker? = null
)

@Serializable
enum class ProjectStatus {
    ACTIVE,
    COMPLETED,
    ON_HOLD
}

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

// ========== Pagination Models ==========

@Serializable
data class PaginationRequest(
    val cursor: String? = null,
    val pageSize: Int = 20,
    val sortBy: String? = null,
    val sortDirection: SortDirection = SortDirection.ASC
)

@Serializable
enum class SortDirection {
    ASC, DESC
}

@Serializable
data class PaginatedResult<T>(
    val data: List<T>,
    val pagination: PaginationInfo
)

@Serializable
data class PaginationInfo(
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val totalCount: Int = 0
)
