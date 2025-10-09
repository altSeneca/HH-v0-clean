package com.hazardhawk.domain.services.ptp

import com.hazardhawk.domain.models.ptp.CrewRosterEntry
import com.hazardhawk.domain.models.ptp.PreTaskPlan
import com.hazardhawk.domain.repositories.CompanyRepository
import com.hazardhawk.domain.repositories.CrewRepository
import com.hazardhawk.domain.repositories.ProjectRepository

/**
 * Service for integrating crew management with PTP generation.
 * Auto-populates PTPs with centralized company, project, and crew data.
 *
 * Implements the integration pattern from crew-management-implementation-plan.md
 */
class PTPCrewIntegrationService(
    private val companyRepository: CompanyRepository,
    private val projectRepository: ProjectRepository,
    private val crewRepository: CrewRepository
) {

    /**
     * Populate PTP with centralized company, project, and crew data.
     *
     * @param basePtp The base PTP to populate (with AI-generated content)
     * @param crewId The crew assigned to this PTP
     * @param selectedForemanId Optional foreman selection (if different from crew's default foreman)
     * @return PTP populated with all centralized data, or error if data cannot be loaded
     */
    suspend fun populatePTPWithCrewData(
        basePtp: PreTaskPlan,
        crewId: String,
        selectedForemanId: String? = null
    ): Result<PreTaskPlan> {
        return try {
            // Fetch crew with members
            val crew = crewRepository.getCrew(
                crewId = crewId,
                includeMembers = true,
                includeForeman = true,
                includeProject = false
            ) ?: return Result.failure(IllegalArgumentException("Crew not found: $crewId"))

            // Fetch project info
            val project = if (crew.projectId != null) {
                projectRepository.getProject(
                    projectId = crew.projectId,
                    includeCompany = false,
                    includeManagers = true
                )
            } else {
                null
            }

            // Fetch company info
            val company = companyRepository.getCompany(crew.companyId)
                ?: return Result.failure(IllegalArgumentException("Company not found: ${crew.companyId}"))

            // Determine foreman (selected or default)
            val foremanId = selectedForemanId ?: crew.foremanId
            val selectedForeman = if (foremanId != null) {
                crew.members.find { it.companyWorkerId == foremanId }?.worker
                    ?: crew.foreman
            } else {
                crew.foreman
            }

            // Validate selected foreman is a crew member
            if (selectedForemanId != null && !crew.members.any { it.companyWorkerId == selectedForemanId }) {
                return Result.failure(
                    IllegalArgumentException("Selected foreman must be a crew member: $selectedForemanId")
                )
            }

            // Generate crew roster sign-in sheet
            val crewRoster = crew.members.map { member ->
                CrewRosterEntry(
                    employeeNumber = member.worker?.employeeNumber ?: "",
                    name = member.worker?.workerProfile?.fullName ?: "",
                    role = member.worker?.role?.displayName ?: "",
                    certifications = member.worker?.certifications?.filter { it.isValid }
                        ?.mapNotNull { it.certificationType?.code } ?: emptyList(),
                    signature = null, // To be filled on-site
                    signedAt = null
                )
            }

            // Build full project address
            val projectAddress = if (project != null) {
                buildString {
                    project.streetAddress?.let { append(it) }
                    if (project.city != null || project.state != null || project.zip != null) {
                        if (isNotEmpty()) append(", ")
                        project.city?.let { append(it) }
                        if (project.state != null) {
                            if (project.city != null) append(", ")
                            append(project.state)
                        }
                        project.zip?.let { append(" $it") }
                    }
                }.takeIf { it.isNotBlank() }
            } else {
                null
            }

            // Populate PTP with all centralized data
            val populatedPtp = basePtp.copy(
                // Crew info
                crewId = crew.id,
                crewName = crew.name,
                foremanId = foremanId,
                foremanName = selectedForeman?.workerProfile?.fullName,
                crewSize = crew.memberCount,

                // Company info (no re-entry needed)
                companyId = company.id,
                companyName = company.name,
                companyAddress = buildCompanyAddress(company),
                companyPhone = company.phone,
                companyLogoUrl = company.logoUrl,

                // Project info (no re-entry needed)
                projectName = project?.name,
                projectNumber = project?.projectNumber,
                projectAddress = projectAddress,
                clientName = project?.clientName,
                generalContractor = project?.generalContractor,
                superintendentName = project?.superintendent?.workerProfile?.fullName,

                // Crew roster sign-in sheet
                crewRoster = crewRoster
            )

            Result.success(populatedPtp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Build full company address from company model
     */
    private fun buildCompanyAddress(company: com.hazardhawk.models.crew.Company): String? {
        return buildString {
            company.address?.let { append(it) }
            if (company.city != null || company.state != null || company.zip != null) {
                if (isNotEmpty()) append(", ")
                company.city?.let { append(it) }
                if (company.state != null) {
                    if (company.city != null) append(", ")
                    append(company.state)
                }
                company.zip?.let { append(" $it") }
            }
        }.takeIf { it.isNotBlank() }
    }

    /**
     * Get available foremen for a crew (all crew members who can act as foreman)
     * This allows foreman selection different from the crew's default foreman.
     */
    suspend fun getAvailableForemen(crewId: String): List<ForemenOption> {
        val crew = crewRepository.getCrew(
            crewId = crewId,
            includeMembers = true,
            includeForeman = false
        ) ?: return emptyList()

        return crew.members.mapNotNull { member ->
            member.worker?.let { worker ->
                ForemenOption(
                    workerId = worker.id,
                    name = worker.workerProfile?.fullName ?: "Unknown",
                    role = worker.role.displayName,
                    isDefaultForeman = worker.id == crew.foremanId
                )
            }
        }
    }
}

/**
 * Available foreman option for PTP creation
 */
data class ForemenOption(
    val workerId: String,
    val name: String,
    val role: String,
    val isDefaultForeman: Boolean
)
