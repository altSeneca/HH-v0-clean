package com.hazardhawk.data.repositories

import com.hazardhawk.FeatureFlags
import com.hazardhawk.data.mocks.MockApiClient
import com.hazardhawk.data.network.ApiClient
import com.hazardhawk.data.network.ApiResponse
import com.hazardhawk.data.network.AssignmentResponse
import com.hazardhawk.data.network.QRCodeResponse
import com.hazardhawk.data.network.SuccessResponse
import com.hazardhawk.models.crew.Crew
import com.hazardhawk.models.crew.CrewStatus
import com.hazardhawk.models.crew.CrewType
import com.hazardhawk.models.crew.CreateCrewRequest
import com.hazardhawk.models.crew.TrackAttendanceRequest
import com.hazardhawk.models.crew.AttendanceType
import com.hazardhawk.models.crew.AttendanceRecord
import com.hazardhawk.models.crew.CrewAvailability
import com.hazardhawk.models.crew.ScheduleConflict
import com.hazardhawk.models.crew.ConflictSeverity
import com.hazardhawk.models.crew.ConflictingProject
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for Crew Service
 *
 * Tests complete workflows:
 * - Crew creation → QR generation → Assignment
 * - Multi-project assignments
 * - Validation rules
 * - End-to-end crew member management
 */
class CrewIntegrationTest {

    private lateinit var mockApiClient: MockApiClient
    private lateinit var repository: CrewApiRepository

    @BeforeTest
    fun setup() {
        FeatureFlags.API_CREW_ENABLED = true
        FeatureFlags.API_CACHE_ENABLED = false  // Disable cache for integration tests
        mockApiClient = MockApiClient()
    }

    // ========== Integration Test 1: Full Crew Creation → Assignment Flow ==========
    @Test
    fun `test complete crew creation to assignment workflow`() = runTest {
        // Arrange
        val createRequest = CreateCrewRequest(
            name = "Integration Test Crew",
            projectId = null,
            crewType = CrewType.GENERAL,
            trade = "General Construction",
            foremanId = "foreman-001",
            location = "Site A"
        )

        val createdCrew = Crew(
            id = "crew-integration-001",
            companyId = "company-001",
            projectId = null,
            name = "Integration Test Crew",
            crewType = CrewType.GENERAL,
            trade = "General Construction",
            foremanId = "foreman-001",
            location = "Site A",
            status = CrewStatus.ACTIVE,
            createdAt = "2025-01-01T10:00:00Z",
            updatedAt = "2025-01-01T10:00:00Z"
        )

        val qrResponse = QRCodeResponse(
            qrCodeUrl = "https://cdn.hazardhawk.com/qr/crew-integration-001.png",
            qrCodeData = "CREW:crew-integration-001",
            expiresAt = "2026-01-01T10:00:00Z"
        )

        val assignmentResponse = AssignmentResponse(
            assignmentId = "assignment-001",
            crewId = "crew-integration-001",
            projectId = "project-001",
            assignedAt = "2025-01-15T10:00:00Z",
            assignedBy = "admin-001"
        )

        val config = MockApiClient.MockApiConfig(
            customResponses = mapOf(
                "/api/crews" to ApiResponse(createdCrew),
                "/api/crews/crew-integration-001/qr-code" to qrResponse,
                "/api/projects/project-001/assign-crew" to assignmentResponse
            )
        )
        mockApiClient = MockApiClient(config)
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act - Step 1: Create crew
        val createResult = repository.createCrew(createRequest)
        assertTrue(createResult.isSuccess)
        val crew = createResult.getOrNull()
        assertNotNull(crew)
        assertEquals("crew-integration-001", crew.id)

        // Act - Step 2: Generate QR code
        val qrResult = repository.generateCrewQRCode(crew.id)
        assertTrue(qrResult.isSuccess)
        val qrCode = qrResult.getOrNull()
        assertNotNull(qrCode)
        assertEquals("CREW:crew-integration-001", qrCode.qrCodeData)

        // Act - Step 3: Assign to project
        val assignResult = repository.assignCrewToProject("project-001", crew.id)
        assertTrue(assignResult.isSuccess)
        val assignment = assignResult.getOrNull()
        assertNotNull(assignment)
        assertEquals("project-001", assignment.projectId)
        assertEquals(crew.id, assignment.crewId)

        // Assert - Verify call sequence
        assertTrue(mockApiClient.verifyCalled("POST", "/api/crews"))
        assertTrue(mockApiClient.verifyCalled("POST", "/api/crews/crew-integration-001/qr-code"))
        assertTrue(mockApiClient.verifyCalled("POST", "/api/projects/project-001/assign-crew"))
    }

    // ========== Integration Test 2: QR Code Generation and Scanning ==========
    @Test
    fun `test QR code generation for crew assignment`() = runTest {
        // Arrange
        val qrResponse = QRCodeResponse(
            qrCodeUrl = "https://cdn.hazardhawk.com/qr/crew-qr-001.png",
            qrCodeData = "CREW:crew-qr-001:ASSIGNMENT:project-002",
            expiresAt = "2025-12-31T23:59:59Z"
        )

        val config = MockApiClient.MockApiConfig(
            customResponses = mapOf(
                "/api/crews/crew-qr-001/qr-code" to qrResponse
            )
        )
        mockApiClient = MockApiClient(config)
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act
        val result = repository.generateCrewQRCode("crew-qr-001")

        // Assert
        assertTrue(result.isSuccess)
        val qr = result.getOrNull()
        assertNotNull(qr)
        assertEquals("CREW:crew-qr-001:ASSIGNMENT:project-002", qr.qrCodeData)
        assertNotNull(qr.expiresAt)
        assertTrue(mockApiClient.verifyCalled("POST", "/api/crews/crew-qr-001/qr-code"))
    }

    // ========== Integration Test 3: Multi-Project Assignments ==========
    @Test
    fun `test crew assigned to multiple projects`() = runTest {
        // Arrange
        val assignment1 = AssignmentResponse(
            assignmentId = "assignment-001",
            crewId = "crew-multi-001",
            projectId = "project-001",
            assignedAt = "2025-01-10T10:00:00Z",
            assignedBy = "admin-001"
        )

        val assignment2 = AssignmentResponse(
            assignmentId = "assignment-002",
            crewId = "crew-multi-001",
            projectId = "project-002",
            assignedAt = "2025-01-11T10:00:00Z",
            assignedBy = "admin-001"
        )

        val allAssignments = listOf(assignment1, assignment2)

        val config = MockApiClient.MockApiConfig(
            customResponses = mapOf(
                "/api/projects/project-001/assign-crew" to assignment1,
                "/api/projects/project-002/assign-crew" to assignment2,
                "/api/crews/crew-multi-001/assignments" to ApiResponse(allAssignments)
            )
        )
        mockApiClient = MockApiClient(config)
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act - Assign to first project
        val assign1 = repository.assignCrewToProject("project-001", "crew-multi-001")
        assertTrue(assign1.isSuccess)

        // Act - Assign to second project
        val assign2 = repository.assignCrewToProject("project-002", "crew-multi-001")
        assertTrue(assign2.isSuccess)

        // Act - Get all assignments
        val assignmentsResult = repository.getCrewAssignments("crew-multi-001")
        assertTrue(assignmentsResult.isSuccess)
        val assignments = assignmentsResult.getOrNull()
        assertNotNull(assignments)
        assertEquals(2, assignments.size)
        assertEquals("project-001", assignments[0].projectId)
        assertEquals("project-002", assignments[1].projectId)
    }

    // ========== Integration Test 4: Unassign Crew from Project ==========
    @Test
    fun `test unassign crew from project`() = runTest {
        // Arrange
        val config = MockApiClient.MockApiConfig(
            customResponses = mapOf(
                "/api/projects/project-001/crews/crew-001" to SuccessResponse(
                    success = true,
                    message = "Crew unassigned successfully"
                )
            )
        )
        mockApiClient = MockApiClient(config)
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act
        val result = repository.unassignCrewFromProject("project-001", "crew-001")

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        assertTrue(mockApiClient.verifyCalled("DELETE", "/api/projects/project-001/crews/crew-001"))
    }

    // ========== Integration Test 5: Validation Rules - Name Length ==========
    @Test
    fun `test crew name validation rejects short names`() = runTest {
        // Arrange
        val invalidRequest = CreateCrewRequest(
            name = "AB",  // Too short (< 3 characters)
            projectId = null,
            crewType = CrewType.GENERAL,
            trade = null,
            foremanId = null,
            location = null
        )

        mockApiClient = MockApiClient()
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act
        val result = repository.createCrew(invalidRequest)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Crew name must be at least 3 characters long", result.exceptionOrNull()?.message)
    }

    // ========== Integration Test 6: Validation Rules - Name Too Long ==========
    @Test
    fun `test crew name validation rejects long names`() = runTest {
        // Arrange
        val longName = "A".repeat(101)  // 101 characters, over limit
        val invalidRequest = CreateCrewRequest(
            name = longName,
            projectId = null,
            crewType = CrewType.GENERAL,
            trade = null,
            foremanId = null,
            location = null
        )

        mockApiClient = MockApiClient()
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act
        val result = repository.createCrew(invalidRequest)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Crew name must be less than 100 characters", result.exceptionOrNull()?.message)
    }

    // ========== Integration Test 7: Role Synchronization ==========
    @Test
    fun `test sync crew roles updates permissions`() = runTest {
        // Arrange
        val config = MockApiClient.MockApiConfig(
            customResponses = mapOf(
                "/api/crews/crew-sync-001/sync-roles" to SuccessResponse(
                    success = true,
                    message = "Roles synchronized successfully"
                )
            )
        )
        mockApiClient = MockApiClient(config)
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act
        val result = repository.syncCrewRoles("crew-sync-001")

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        assertTrue(mockApiClient.verifyCalled("POST", "/api/crews/crew-sync-001/sync-roles"))
    }

    // ========== Integration Test 8: Complete Member Management Flow ==========
    @Test
    fun `test complete crew member lifecycle - add, update role, remove`() = runTest {
        // Arrange
        val memberId = "member-lifecycle-001"
        val crewId = "crew-lifecycle-001"

        val config = MockApiClient.MockApiConfig(
            customResponses = mapOf(
                "/api/crews/$crewId/members" to ApiResponse(
                    com.hazardhawk.models.crew.CrewMember(
                        id = memberId,
                        crewId = crewId,
                        companyWorkerId = "worker-001",
                        role = com.hazardhawk.models.crew.CrewMemberRole.MEMBER,
                        startDate = kotlinx.datetime.LocalDate(2025, 1, 15),
                        endDate = null,
                        status = "active",
                        worker = null
                    )
                ),
                "/api/crews/$crewId/members/$memberId" to ApiResponse(
                    com.hazardhawk.models.crew.CrewMember(
                        id = memberId,
                        crewId = crewId,
                        companyWorkerId = "worker-001",
                        role = com.hazardhawk.models.crew.CrewMemberRole.LEAD,
                        startDate = kotlinx.datetime.LocalDate(2025, 1, 15),
                        endDate = null,
                        status = "active",
                        worker = null
                    )
                ),
                "/api/crews/$crewId/members/$memberId" to SuccessResponse(success = true)
            )
        )
        mockApiClient = MockApiClient(config)
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act - Step 1: Add member
        val addResult = repository.addCrewMember(crewId, "worker-001", "member")
        assertTrue(addResult.isSuccess)
        val member = addResult.getOrNull()
        assertNotNull(member)
        assertEquals(memberId, member.id)

        // Act - Step 2: Update member role
        val updateResult = repository.updateCrewMemberRole(crewId, memberId, "lead")
        assertTrue(updateResult.isSuccess)
        val updatedMember = updateResult.getOrNull()
        assertNotNull(updatedMember)
        assertEquals(com.hazardhawk.models.crew.CrewMemberRole.LEAD, updatedMember.role)

        // Act - Step 3: Remove member
        val removeResult = repository.removeCrewMember(crewId, memberId)
        assertTrue(removeResult.isSuccess)

        // Assert - Verify all operations were called
        assertTrue(mockApiClient.verifyCalled("POST", "/api/crews/$crewId/members"))
        assertTrue(mockApiClient.verifyCalled("PATCH", "/api/crews/$crewId/members/$memberId"))
        assertTrue(mockApiClient.verifyCalled("DELETE", "/api/crews/$crewId/members/$memberId"))
    }

    // ========== Week 3 Integration Tests ==========

    // Integration Test 9: Multi-Project Assignment Workflow
    @Test
    fun `test crew assigned to multiple projects simultaneously`() = runTest {
        // Arrange
        val crewId = "crew-multi-001"
        val projectIds = listOf("project-001", "project-002", "project-003")

        val assignments = listOf(
            AssignmentResponse(
                assignmentId = "assign-multi-001",
                crewId = crewId,
                projectId = "project-001",
                assignedAt = "2025-10-09T10:00:00Z",
                assignedBy = "admin-001"
            ),
            AssignmentResponse(
                assignmentId = "assign-multi-002",
                crewId = crewId,
                projectId = "project-002",
                assignedAt = "2025-10-09T10:00:00Z",
                assignedBy = "admin-001"
            ),
            AssignmentResponse(
                assignmentId = "assign-multi-003",
                crewId = crewId,
                projectId = "project-003",
                assignedAt = "2025-10-09T10:00:00Z",
                assignedBy = "admin-001"
            )
        )

        val config = MockApiClient.MockApiConfig(
            customResponses = mapOf(
                "/api/crews/$crewId/projects/assign-multiple" to ApiResponse(assignments)
            )
        )
        mockApiClient = MockApiClient(config)
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act
        val result = repository.assignCrewToMultipleProjects(crewId, projectIds)

        // Assert
        assertTrue(result.isSuccess)
        val assignmentList = result.getOrNull()
        assertNotNull(assignmentList)
        assertEquals(3, assignmentList.size)
        assertEquals("project-001", assignmentList[0].projectId)
        assertEquals("project-002", assignmentList[1].projectId)
        assertEquals("project-003", assignmentList[2].projectId)
        assertTrue(mockApiClient.verifyCalled("POST", "/api/crews/$crewId/projects/assign-multiple"))
    }

    // Integration Test 10: Multi-Project Assignment with Conflict Detection
    @Test
    fun `test multi-project assignment detects schedule conflicts`() = runTest {
        // Arrange - First assign crew to multiple projects
        val crewId = "crew-conflict-001"
        val projectIds = listOf("project-001", "project-002")

        val assignments = listOf(
            AssignmentResponse(
                assignmentId = "assign-001",
                crewId = crewId,
                projectId = "project-001",
                assignedAt = "2025-10-09T10:00:00Z",
                assignedBy = "admin-001"
            ),
            AssignmentResponse(
                assignmentId = "assign-002",
                crewId = crewId,
                projectId = "project-002",
                assignedAt = "2025-10-09T10:00:00Z",
                assignedBy = "admin-001"
            )
        )

        // Then detect conflicts
        val conflicts = listOf(
            ScheduleConflict(
                date = "2025-10-10",
                crewId = crewId,
                crewName = "Conflict Test Crew",
                conflictingProjects = listOf(
                    ConflictingProject("project-001", "Site A", 8.0),
                    ConflictingProject("project-002", "Site B", 8.0)
                ),
                totalHours = 16.0,
                severity = ConflictSeverity.HIGH
            )
        )

        val config = MockApiClient.MockApiConfig(
            customResponses = mapOf(
                "/api/crews/$crewId/projects/assign-multiple" to ApiResponse(assignments),
                "/api/crews/detect-conflicts" to ApiResponse(conflicts)
            )
        )
        mockApiClient = MockApiClient(config)
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act - Step 1: Assign to multiple projects
        val assignResult = repository.assignCrewToMultipleProjects(crewId, projectIds)
        assertTrue(assignResult.isSuccess)

        // Act - Step 2: Detect conflicts
        val conflictResult = repository.detectScheduleConflicts(
            startDate = "2025-10-09",
            endDate = "2025-10-15",
            crewIds = listOf(crewId)
        )

        // Assert
        assertTrue(conflictResult.isSuccess)
        val conflictList = conflictResult.getOrNull()
        assertNotNull(conflictList)
        assertEquals(1, conflictList.size)
        assertEquals(ConflictSeverity.HIGH, conflictList[0].severity)
        assertEquals(16.0, conflictList[0].totalHours)
        assertTrue(mockApiClient.verifyCalled("POST", "/api/crews/detect-conflicts"))
    }

    // Integration Test 11: Multi-Project Unassignment
    @Test
    fun `test crew can be unassigned from multiple projects`() = runTest {
        // Arrange - Setup crew with multiple assignments
        val crewId = "crew-unassign-001"

        val config = MockApiClient.MockApiConfig(
            customResponses = mapOf(
                "/api/projects/project-001/crews/$crewId" to SuccessResponse(success = true),
                "/api/projects/project-002/crews/$crewId" to SuccessResponse(success = true)
            )
        )
        mockApiClient = MockApiClient(config)
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act - Unassign from project 1
        val unassign1 = repository.unassignCrewFromProject("project-001", crewId)
        assertTrue(unassign1.isSuccess)

        // Act - Unassign from project 2
        val unassign2 = repository.unassignCrewFromProject("project-002", crewId)
        assertTrue(unassign2.isSuccess)

        // Assert
        assertTrue(mockApiClient.verifyCalled("DELETE", "/api/projects/project-001/crews/$crewId"))
        assertTrue(mockApiClient.verifyCalled("DELETE", "/api/projects/project-002/crews/$crewId"))
    }

    // Integration Test 12: Crew Availability Check for Date Range
    @Test
    fun `test crew availability shows current assignments and conflicts`() = runTest {
        // Arrange
        val crewId = "crew-avail-001"
        val availability = CrewAvailability(
            crewId = crewId,
            crewName = "Availability Test Crew",
            isAvailable = false,
            currentProjects = listOf("project-001", "project-002"),
            scheduledDates = emptyList(),
            conflicts = listOf(
                ScheduleConflict(
                    date = "2025-10-12",
                    crewId = crewId,
                    crewName = "Availability Test Crew",
                    conflictingProjects = listOf(
                        ConflictingProject("project-001", "Site A", 8.0),
                        ConflictingProject("project-002", "Site B", 6.0)
                    ),
                    totalHours = 14.0,
                    severity = ConflictSeverity.HIGH
                )
            )
        )

        val config = MockApiClient.MockApiConfig(
            customResponses = mapOf(
                "/api/crews/$crewId/availability" to ApiResponse(availability)
            )
        )
        mockApiClient = MockApiClient(config)
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act
        val result = repository.getCrewAvailability(
            crewId = crewId,
            startDate = "2025-10-09",
            endDate = "2025-10-15"
        )

        // Assert
        assertTrue(result.isSuccess)
        val availData = result.getOrNull()
        assertNotNull(availData)
        assertEquals(false, availData.isAvailable)
        assertEquals(2, availData.currentProjects.size)
        assertEquals(1, availData.conflicts.size)
        assertEquals(ConflictSeverity.HIGH, availData.conflicts[0].severity)
        assertTrue(mockApiClient.verifyCalled("GET", "/api/crews/$crewId/availability"))
    }

    // Integration Test 13: Attendance Tracking Throughout the Day
    @Test
    fun `test crew attendance tracking from check-in to check-out`() = runTest {
        // Arrange
        val crewId = "crew-attendance-001"

        val checkInRecord = AttendanceRecord(
            id = "attendance-001",
            crewId = crewId,
            type = AttendanceType.CHECK_IN,
            timestamp = "2025-10-09T08:00:00Z",
            latitude = 37.7749,
            longitude = -122.4194,
            notes = "Morning check-in",
            createdAt = "2025-10-09T08:00:00Z"
        )

        val breakStartRecord = AttendanceRecord(
            id = "attendance-002",
            crewId = crewId,
            type = AttendanceType.BREAK_START,
            timestamp = "2025-10-09T12:00:00Z",
            latitude = 37.7749,
            longitude = -122.4194,
            notes = "Lunch break",
            createdAt = "2025-10-09T12:00:00Z"
        )

        val breakEndRecord = AttendanceRecord(
            id = "attendance-003",
            crewId = crewId,
            type = AttendanceType.BREAK_END,
            timestamp = "2025-10-09T13:00:00Z",
            latitude = 37.7749,
            longitude = -122.4194,
            notes = "Back from lunch",
            createdAt = "2025-10-09T13:00:00Z"
        )

        val checkOutRecord = AttendanceRecord(
            id = "attendance-004",
            crewId = crewId,
            type = AttendanceType.CHECK_OUT,
            timestamp = "2025-10-09T17:00:00Z",
            latitude = 37.7749,
            longitude = -122.4194,
            notes = "End of day",
            createdAt = "2025-10-09T17:00:00Z"
        )

        val config = MockApiClient.MockApiConfig(
            customResponses = mapOf(
                "/api/crews/$crewId/attendance" to checkInRecord
            )
        )
        mockApiClient = MockApiClient(config)
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act - Check in
        val checkInResult = repository.trackAttendance(
            crewId,
            TrackAttendanceRequest(
                crewId = crewId,
                type = AttendanceType.CHECK_IN,
                timestamp = "2025-10-09T08:00:00Z",
                latitude = 37.7749,
                longitude = -122.4194,
                notes = "Morning check-in"
            )
        )
        assertTrue(checkInResult.isSuccess)

        // Act - Break start
        mockApiClient = MockApiClient(MockApiClient.MockApiConfig(
            customResponses = mapOf("/api/crews/$crewId/attendance" to breakStartRecord)
        ))
        repository = CrewApiRepository(createMockApiClient(mockApiClient))
        val breakStartResult = repository.trackAttendance(
            crewId,
            TrackAttendanceRequest(
                crewId = crewId,
                type = AttendanceType.BREAK_START,
                timestamp = "2025-10-09T12:00:00Z",
                latitude = 37.7749,
                longitude = -122.4194,
                notes = "Lunch break"
            )
        )
        assertTrue(breakStartResult.isSuccess)

        // Act - Break end
        mockApiClient = MockApiClient(MockApiClient.MockApiConfig(
            customResponses = mapOf("/api/crews/$crewId/attendance" to breakEndRecord)
        ))
        repository = CrewApiRepository(createMockApiClient(mockApiClient))
        val breakEndResult = repository.trackAttendance(
            crewId,
            TrackAttendanceRequest(
                crewId = crewId,
                type = AttendanceType.BREAK_END,
                timestamp = "2025-10-09T13:00:00Z",
                latitude = 37.7749,
                longitude = -122.4194,
                notes = "Back from lunch"
            )
        )
        assertTrue(breakEndResult.isSuccess)

        // Act - Check out
        mockApiClient = MockApiClient(MockApiClient.MockApiConfig(
            customResponses = mapOf("/api/crews/$crewId/attendance" to checkOutRecord)
        ))
        repository = CrewApiRepository(createMockApiClient(mockApiClient))
        val checkOutResult = repository.trackAttendance(
            crewId,
            TrackAttendanceRequest(
                crewId = crewId,
                type = AttendanceType.CHECK_OUT,
                timestamp = "2025-10-09T17:00:00Z",
                latitude = 37.7749,
                longitude = -122.4194,
                notes = "End of day"
            )
        )
        assertTrue(checkOutResult.isSuccess)

        // Assert - All attendance events tracked successfully
        val checkOut = checkOutResult.getOrNull()
        assertNotNull(checkOut)
        assertEquals(AttendanceType.CHECK_OUT, checkOut.type)
    }

    // Integration Test 14: Conflict Detection Across Multiple Crews
    @Test
    fun `test detect conflicts for multiple crews simultaneously`() = runTest {
        // Arrange
        val conflicts = listOf(
            ScheduleConflict(
                date = "2025-10-10",
                crewId = "crew-001",
                crewName = "Alpha Team",
                conflictingProjects = listOf(
                    ConflictingProject("project-001", "Site A", 8.0),
                    ConflictingProject("project-002", "Site B", 8.0)
                ),
                totalHours = 16.0,
                severity = ConflictSeverity.HIGH
            ),
            ScheduleConflict(
                date = "2025-10-11",
                crewId = "crew-002",
                crewName = "Beta Team",
                conflictingProjects = listOf(
                    ConflictingProject("project-003", "Site C", 10.0),
                    ConflictingProject("project-004", "Site D", 4.0)
                ),
                totalHours = 14.0,
                severity = ConflictSeverity.HIGH
            ),
            ScheduleConflict(
                date = "2025-10-12",
                crewId = "crew-003",
                crewName = "Gamma Team",
                conflictingProjects = listOf(
                    ConflictingProject("project-005", "Site E", 9.0),
                    ConflictingProject("project-006", "Site F", 9.0)
                ),
                totalHours = 18.0,
                severity = ConflictSeverity.CRITICAL
            )
        )

        val config = MockApiClient.MockApiConfig(
            customResponses = mapOf(
                "/api/crews/detect-conflicts" to ApiResponse(conflicts)
            )
        )
        mockApiClient = MockApiClient(config)
        val apiClient = createMockApiClient(mockApiClient)
        repository = CrewApiRepository(apiClient)

        // Act
        val result = repository.detectScheduleConflicts(
            startDate = "2025-10-09",
            endDate = "2025-10-15",
            crewIds = listOf("crew-001", "crew-002", "crew-003")
        )

        // Assert
        assertTrue(result.isSuccess)
        val conflictList = result.getOrNull()
        assertNotNull(conflictList)
        assertEquals(3, conflictList.size)

        // Verify conflict severity levels
        assertEquals(ConflictSeverity.HIGH, conflictList[0].severity)
        assertEquals(ConflictSeverity.HIGH, conflictList[1].severity)
        assertEquals(ConflictSeverity.CRITICAL, conflictList[2].severity)

        // Verify total hours calculation
        assertEquals(16.0, conflictList[0].totalHours)
        assertEquals(14.0, conflictList[1].totalHours)
        assertEquals(18.0, conflictList[2].totalHours)

        assertTrue(mockApiClient.verifyCalled("POST", "/api/crews/detect-conflicts"))
    }

    // ========== Helper: Create Mock ApiClient Wrapper ==========
    private fun createMockApiClient(mockClient: MockApiClient): ApiClient {
        return ApiClient(baseUrl = "http://mock-api.test")
    }
}
