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

// ========== Attendance Tracking Models ==========

@Serializable
data class TrackAttendanceRequest(
    val crewId: String,
    val type: AttendanceType,
    val timestamp: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null
)

@Serializable
enum class AttendanceType {
    CHECK_IN,
    CHECK_OUT,
    BREAK_START,
    BREAK_END
}

@Serializable
data class AttendanceRecord(
    val id: String,
    val crewId: String,
    val type: AttendanceType,
    val timestamp: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null,
    val createdAt: String
)

// ========== Analytics Models ==========

@Serializable
data class CrewAnalytics(
    val totalCrews: Int,
    val activeCrews: Int,
    val inactiveCrews: Int,
    val onLeaveCrews: Int,
    val averageCrewSize: Double,
    val totalMembers: Int,
    val utilizationRate: Double,
    val timestamp: String
)

@Serializable
data class CrewPerformanceMetrics(
    val crewId: String,
    val crewName: String,
    val projectCompletionRate: Double,
    val attendanceScore: Double,
    val averageProjectDuration: Double,
    val totalProjectsCompleted: Int,
    val onTimeDeliveryRate: Double,
    val safetyIncidentCount: Int,
    val periodStart: String,
    val periodEnd: String
)

// ========== Multi-Project Support Models ==========

@Serializable
data class AssignMultipleProjectsRequest(
    val crewId: String,
    val projectIds: List<String>
)

@Serializable
data class CrewAvailability(
    val crewId: String,
    val crewName: String,
    val isAvailable: Boolean,
    val currentProjects: List<String>,
    val scheduledDates: List<ScheduledDate>,
    val conflicts: List<ScheduleConflict>
)

@Serializable
data class ScheduledDate(
    val date: String,
    val projectId: String,
    val projectName: String,
    val hours: Double
)

@Serializable
data class ScheduleConflict(
    val date: String,
    val crewId: String,
    val crewName: String,
    val conflictingProjects: List<ConflictingProject>,
    val totalHours: Double,
    val severity: ConflictSeverity
)

@Serializable
data class ConflictingProject(
    val projectId: String,
    val projectName: String,
    val scheduledHours: Double
)

@Serializable
enum class ConflictSeverity {
    LOW,      // < 10 hours total
    MEDIUM,   // 10-12 hours
    HIGH,     // > 12 hours (double-booked)
    CRITICAL  // Multiple conflicts on same day
}
