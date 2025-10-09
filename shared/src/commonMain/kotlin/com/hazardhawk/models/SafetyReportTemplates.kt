package com.hazardhawk.models

import com.hazardhawk.models.ReportTemplate
import com.hazardhawk.models.ReportType
import com.hazardhawk.models.ReportSection

/**
 * Simplified safety report templates factory
 * Provides pre-configured OSHA-compliant templates
 */
object SafetyReportTemplates {
    
    /**
     * Get all available report templates
     */
    fun getAllTemplates(): List<ReportTemplate> = listOf(
        createDailyInspectionTemplate(),
        createIncidentReportTemplate(),
        createPreTaskPlanTemplate(),
        createHazardIdentificationTemplate(),
        createToolboxTalkTemplate()
    )
    
    /**
     * Get template by ID
     */
    fun getTemplateById(id: String): ReportTemplate? =
        getAllTemplates().find { it.id == id }
    
    private fun createDailyInspectionTemplate() = ReportTemplate(
        id = "daily_inspection",
        name = "Daily Safety Inspection",
        type = ReportType.DAILY_INSPECTION,
        description = "Daily site safety inspection with photo documentation and OSHA compliance",
        minimumPhotos = 3,
        oshaCompliant = true,
        oshaStandards = listOf("1926.95", "1926.451", "1926.501"),
        requiredSignatures = listOf("Safety Inspector", "Site Supervisor"),
        sections = listOf(
            ReportSection(
                id = "site_overview",
                title = "Site Overview",
                required = true,
                order = 1
            ),
            ReportSection(
                id = "hazard_assessment",
                title = "Hazard Assessment",
                required = true,
                order = 2
            ),
            ReportSection(
                id = "recommendations",
                title = "Safety Recommendations",
                required = true,
                order = 3
            )
        ),
        requiredFields = listOf("siteName", "contractorName", "reporterName")
    )
    
    private fun createIncidentReportTemplate() = ReportTemplate(
        id = "incident_report",
        name = "Incident Report",
        type = ReportType.INCIDENT_REPORT,
        description = "Comprehensive incident documentation for OSHA compliance",
        minimumPhotos = 2,
        oshaCompliant = true,
        oshaStandards = listOf("1904.4", "1904.5", "1904.7"),
        requiredSignatures = listOf("Incident Witness", "Safety Officer", "Site Manager"),
        sections = listOf(
            ReportSection(
                id = "incident_details",
                title = "Incident Details",
                required = true,
                order = 1
            ),
            ReportSection(
                id = "injuries_damages",
                title = "Injuries and Damages",
                required = true,
                order = 2
            ),
            ReportSection(
                id = "corrective_actions",
                title = "Corrective Actions",
                required = true,
                order = 3
            )
        ),
        requiredFields = listOf("siteName", "incidentDate", "reporterName", "witnessNames")
    )
    
    private fun createPreTaskPlanTemplate() = ReportTemplate(
        id = "pre_task_plan",
        name = "Pre-Task Planning (PTP)",
        type = ReportType.PRE_TASK_PLAN,
        description = "Job hazard analysis and safety planning before work begins",
        minimumPhotos = 1,
        oshaCompliant = true,
        oshaStandards = listOf("1926.95", "1926.1200"),
        requiredSignatures = listOf("Crew Leader", "Safety Representative"),
        sections = listOf(
            ReportSection(
                id = "work_description",
                title = "Work Description",
                required = true,
                order = 1
            ),
            ReportSection(
                id = "hazard_analysis",
                title = "Hazard Analysis",
                required = true,
                order = 2
            ),
            ReportSection(
                id = "safety_measures",
                title = "Safety Measures",
                required = true,
                order = 3
            )
        ),
        requiredFields = listOf("workDescription", "crewMembers", "expectedDuration")
    )
    
    private fun createHazardIdentificationTemplate() = ReportTemplate(
        id = "hazard_identification",
        name = "Hazard Identification",
        type = ReportType.HAZARD_IDENTIFICATION,
        description = "Systematic identification and assessment of workplace hazards",
        minimumPhotos = 2,
        oshaCompliant = true,
        oshaStandards = listOf("1926.95", "1926.65", "1926.501"),
        requiredSignatures = listOf("Safety Inspector"),
        sections = listOf(
            ReportSection(
                id = "hazard_details",
                title = "Hazard Details",
                required = true,
                order = 1
            ),
            ReportSection(
                id = "risk_assessment",
                title = "Risk Assessment",
                required = true,
                order = 2
            ),
            ReportSection(
                id = "mitigation_plan",
                title = "Mitigation Plan",
                required = true,
                order = 3
            )
        ),
        requiredFields = listOf("hazardType", "severity", "likelihood")
    )
    
    private fun createToolboxTalkTemplate() = ReportTemplate(
        id = "toolbox_talk",
        name = "Toolbox Talk",
        type = ReportType.TOOLBOX_TALK,
        description = "Weekly safety meeting documentation with crew acknowledgment",
        minimumPhotos = 1,
        oshaCompliant = true,
        oshaStandards = listOf("1926.95"),
        requiredSignatures = listOf("Meeting Leader"),
        sections = listOf(
            ReportSection(
                id = "meeting_topic",
                title = "Meeting Topic",
                required = true,
                order = 1
            ),
            ReportSection(
                id = "discussion_points",
                title = "Discussion Points",
                required = true,
                order = 2
            ),
            ReportSection(
                id = "attendee_acknowledgment",
                title = "Attendee Acknowledgment",
                required = true,
                order = 3
            )
        ),
        requiredFields = listOf("meetingTopic", "attendees", "duration")
    )
}
