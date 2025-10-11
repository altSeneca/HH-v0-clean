package com.hazardhawk.documents.templates

import com.hazardhawk.core.models.WorkType
import com.hazardhawk.documents.models.PTPDocument

/**
 * Template engine for Pre-Task Plan document generation.
 * Provides industry-standard templates and customization options.
 */
class PTPTemplateEngine {
    
    private val templates = mapOf(
        WorkType.GENERAL_CONSTRUCTION to "standard_construction_ptp",
        WorkType.ELECTRICAL to "electrical_work_ptp",
        WorkType.FALL_PROTECTION to "fall_protection_ptp",
        WorkType.EXCAVATION to "excavation_ptp",
        WorkType.WELDING to "hot_work_ptp",
        WorkType.ROOFING to "roofing_ptp",
        WorkType.SCAFFOLDING to "scaffolding_ptp",
        WorkType.CRANE_OPERATIONS to "crane_operations_ptp",
        WorkType.CONCRETE to "concrete_work_ptp",
        WorkType.DEMOLITION to "demolition_ptp"
    )
    
    /**
     * Get the appropriate template for a work type.
     */
    fun getTemplate(workType: WorkType): PTPTemplate {
        val templateName = templates[workType] ?: "standard_construction_ptp"
        return loadTemplate(templateName)
    }
    
    /**
     * Apply template to PTP document for formatting.
     */
    fun applyTemplate(document: PTPDocument, template: PTPTemplate): FormattedPTPDocument {
        return FormattedPTPDocument(
            document = document,
            template = template,
            htmlContent = generateHTML(document, template),
            pdfMetadata = generatePDFMetadata(document, template)
        )
    }
    
    /**
     * Generate HTML content for web display or PDF generation.
     */
    private fun generateHTML(document: PTPDocument, template: PTPTemplate): String {
        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"en\">")
            appendLine("<head>")
            appendLine("    <meta charset=\"UTF-8\">")
            appendLine("    <title>${document.title}</title>")
            appendLine("    <style>${template.cssStyles}</style>")
            appendLine("</head>")
            appendLine("<body>")
            
            // Header section
            appendLine("    <header class=\"ptp-header\">")
            appendLine("        <h1>${document.title}</h1>")
            appendLine("        <div class=\"project-info\">")
            appendLine("            <p><strong>Project:</strong> ${document.projectInfo.projectName}</p>")
            appendLine("            <p><strong>Location:</strong> ${document.projectInfo.location}</p>")
            appendLine("            <p><strong>Date:</strong> ${document.projectInfo.workDate}</p>")
            appendLine("        </div>")
            appendLine("    </header>")
            
            // Job description section
            appendLine("    <section class=\"job-description\">")
            appendLine("        <h2>Job Description</h2>")
            appendLine("        <p><strong>Work Type:</strong> ${document.jobDescription.workType}</p>")
            appendLine("        <p><strong>Description:</strong> ${document.jobDescription.taskDescription}</p>")
            appendLine("        <p><strong>Workers:</strong> ${document.jobDescription.numberOfWorkers}</p>")
            appendLine("        <p><strong>Duration:</strong> ${document.jobDescription.workHours}</p>")
            appendLine("    </section>")
            
            // Hazard analysis section
            appendLine("    <section class=\"hazard-analysis\">")
            appendLine("        <h2>Hazard Analysis</h2>")
            appendLine("        <table class=\"hazards-table\">")
            appendLine("            <thead>")
            appendLine("                <tr>")
            appendLine("                    <th>Hazard</th>")
            appendLine("                    <th>Risk Level</th>")
            appendLine("                    <th>OSHA Reference</th>")
            appendLine("                    <th>Control Measures</th>")
            appendLine("                </tr>")
            appendLine("            </thead>")
            appendLine("            <tbody>")
            
            document.hazardAnalysis.identifiedHazards.forEach { hazard ->
                appendLine("                <tr>")
                appendLine("                    <td>${hazard.description}</td>")
                appendLine("                    <td class=\"risk-${hazard.severity.lowercase()}\">${hazard.riskRating}</td>")
                appendLine("                    <td>${hazard.oshaReference ?: "N/A"}</td>")
                appendLine("                    <td>")
                
                val controlMeasures = document.hazardAnalysis.controlMeasures
                    .filter { it.hazardId == hazard.hazardId }
                controlMeasures.forEach { control ->
                    appendLine("                        • ${control.description}<br>")
                }
                
                appendLine("                    </td>")
                appendLine("                </tr>")
            }
            
            appendLine("            </tbody>")
            appendLine("        </table>")
            appendLine("    </section>")
            
            // Safety procedures section
            appendLine("    <section class=\"safety-procedures\">")
            appendLine("        <h2>Safety Procedures</h2>")
            
            document.safetyProcedures.forEach { procedure ->
                appendLine("        <div class=\"procedure\">")
                appendLine("            <h3>${procedure.title}</h3>")
                appendLine("            <ol>")
                
                procedure.steps.forEach { step ->
                    appendLine("                <li>")
                    appendLine("                    ${step.instruction}")
                    if (step.safetyNote != null) {
                        appendLine("                    <div class=\"safety-note\">⚠️ ${step.safetyNote}</div>")
                    }
                    if (step.requiredPPE.isNotEmpty()) {
                        appendLine("                    <div class=\"ppe-required\">PPE: ${step.requiredPPE.joinToString(", ")}</div>")
                    }
                    appendLine("                </li>")
                }
                
                appendLine("            </ol>")
                appendLine("        </div>")
            }
            
            appendLine("    </section>")
            
            // PPE requirements section
            appendLine("    <section class=\"ppe-requirements\">")
            appendLine("        <h2>Required PPE</h2>")
            appendLine("        <div class=\"ppe-grid\">")
            
            document.requiredPPE.forEach { ppe ->
                appendLine("            <div class=\"ppe-item\">")
                appendLine("                <h4>${ppe.ppeType.name.replace('_', ' ').lowercase().split(' ').joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }}</h4>")
                appendLine("                <p>${ppe.specification}</p>")
                if (ppe.oshaStandard != null) {
                    appendLine("                <p><small>OSHA: ${ppe.oshaStandard}</small></p>")
                }
                appendLine("            </div>")
            }
            
            appendLine("        </div>")
            appendLine("    </section>")
            
            // Emergency information section
            appendLine("    <section class=\"emergency-info\">")
            appendLine("        <h2>Emergency Information</h2>")
            appendLine("        <div class=\"emergency-contacts\">")
            appendLine("            <h3>Emergency Contacts</h3>")
            
            document.emergencyInformation.emergencyContacts.forEach { contact ->
                appendLine("            <p><strong>${contact.title}:</strong> ${contact.name} - ${contact.phoneNumber}</p>")
            }
            
            appendLine("        </div>")
            appendLine("        <div class=\"hospital-info\">")
            appendLine("            <h3>Nearest Hospital</h3>")
            appendLine("            <p><strong>${document.emergencyInformation.nearestHospital.name}</strong></p>")
            appendLine("            <p>${document.emergencyInformation.nearestHospital.address}</p>")
            appendLine("            <p>Phone: ${document.emergencyInformation.nearestHospital.phoneNumber}</p>")
            appendLine("            <p>Distance: ${document.emergencyInformation.nearestHospital.distance}</p>")
            appendLine("        </div>")
            appendLine("    </section>")
            
            // Approval signatures section
            appendLine("    <section class=\"approvals\">")
            appendLine("        <h2>Approvals</h2>")
            appendLine("        <div class=\"signature-grid\">")
            
            val defaultApprovals = listOf(
                "Site Supervisor" to "____________________",
                "Safety Manager" to "____________________",
                "Project Manager" to "____________________"
            )
            
            defaultApprovals.forEach { (role, signatureLine) ->
                appendLine("            <div class=\"signature-block\">")
                appendLine("                <p><strong>$role</strong></p>")
                appendLine("                <p>Signature: $signatureLine</p>")
                appendLine("                <p>Date: ____________________</p>")
                appendLine("            </div>")
            }
            
            appendLine("        </div>")
            appendLine("    </section>")
            
            // Footer
            appendLine("    <footer class=\"ptp-footer\">")
            appendLine("        <p>Generated by HazardHawk AI Safety System</p>")
            appendLine("        <p>Document ID: ${document.id}</p>")
            appendLine("        <p>Generated: ${kotlinx.datetime.Instant.fromEpochMilliseconds(document.createdAt).toString()}</p>")
            appendLine("    </footer>")
            
            appendLine("</body>")
            appendLine("</html>")
        }
    }
    
    /**
     * Generate PDF metadata for document properties.
     */
    private fun generatePDFMetadata(document: PTPDocument, template: PTPTemplate): PDFMetadata {
        return PDFMetadata(
            title = document.title,
            author = "HazardHawk AI Safety System",
            subject = "Pre-Task Plan for ${document.jobDescription.workType}",
            keywords = listOf(
                "construction",
                "safety",
                "PTP",
                "OSHA",
                document.jobDescription.workType.name.lowercase()
            ),
            creator = "HazardHawk",
            producer = "HazardHawk PDF Generator",
            creationDate = document.createdAt
        )
    }
    
    /**
     * Load template configuration.
     */
    private fun loadTemplate(templateName: String): PTPTemplate {
        // This would typically load from a template repository
        // For now, return a default template with construction-focused styling
        return PTPTemplate(
            name = templateName,
            description = "Standard construction PTP template",
            cssStyles = getConstructionCSS(),
            headerTemplate = getDefaultHeaderTemplate(),
            footerTemplate = getDefaultFooterTemplate(),
            sectionOrder = listOf(
                "header",
                "job_description", 
                "hazard_analysis",
                "safety_procedures",
                "ppe_requirements",
                "emergency_info",
                "approvals"
            )
        )
    }
    
    /**
     * Get construction-focused CSS styles.
     */
    private fun getConstructionCSS(): String {
        return """
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            margin: 40px;
            color: #333;
        }
        
        .ptp-header {
            background-color: #1a472a;
            color: white;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 30px;
        }
        
        .ptp-header h1 {
            margin: 0;
            font-size: 24px;
        }
        
        .project-info {
            margin-top: 15px;
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 15px;
        }
        
        .project-info p {
            margin: 5px 0;
        }
        
        section {
            margin-bottom: 30px;
            page-break-inside: avoid;
        }
        
        h2 {
            color: #1a472a;
            border-bottom: 2px solid #e74c3c;
            padding-bottom: 5px;
        }
        
        .hazards-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 15px;
        }
        
        .hazards-table th,
        .hazards-table td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }
        
        .hazards-table th {
            background-color: #1a472a;
            color: white;
        }
        
        .risk-critical { color: #e53e3e; font-weight: bold; }
        .risk-high { color: #ff8c00; font-weight: bold; }
        .risk-moderate { color: #ffa500; font-weight: bold; }
        .risk-low { color: #38a169; font-weight: bold; }
        
        .procedure {
            background-color: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 20px;
        }
        
        .safety-note {
            background-color: #fff3cd;
            border-left: 4px solid #ffc107;
            padding: 10px;
            margin: 10px 0;
            font-weight: bold;
        }
        
        .ppe-required {
            background-color: #d1ecf1;
            border-left: 4px solid #2b6cb0;
            padding: 8px;
            margin: 8px 0;
            font-style: italic;
        }
        
        .ppe-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-top: 15px;
        }
        
        .ppe-item {
            background-color: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 15px;
        }
        
        .emergency-info {
            background-color: #f8d7da;
            border: 1px solid #f5c6cb;
            border-radius: 8px;
            padding: 20px;
        }
        
        .signature-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 30px;
            margin-top: 20px;
        }
        
        .signature-block {
            border: 1px solid #dee2e6;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
        }
        
        .ptp-footer {
            margin-top: 50px;
            padding-top: 20px;
            border-top: 1px solid #dee2e6;
            text-align: center;
            color: #6c757d;
            font-size: 12px;
        }
        
        @media print {
            body { margin: 20px; }
            .ptp-header { background-color: #1a472a !important; }
        }
        """.trimIndent()
    }
    
    private fun getDefaultHeaderTemplate(): String = ""
    private fun getDefaultFooterTemplate(): String = ""
}

/**
 * PTP template configuration.
 */
data class PTPTemplate(
    val name: String,
    val description: String,
    val cssStyles: String,
    val headerTemplate: String,
    val footerTemplate: String,
    val sectionOrder: List<String>
)

/**
 * Formatted PTP document ready for rendering.
 */
data class FormattedPTPDocument(
    val document: PTPDocument,
    val template: PTPTemplate,
    val htmlContent: String,
    val pdfMetadata: PDFMetadata
)

/**
 * PDF generation metadata.
 */
data class PDFMetadata(
    val title: String,
    val author: String,
    val subject: String,
    val keywords: List<String>,
    val creator: String,
    val producer: String,
    val creationDate: Long
)