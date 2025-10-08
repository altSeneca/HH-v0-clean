package com.hazardhawk.domain.models.ptp

import kotlinx.serialization.Serializable

/**
 * Request to AI service for PTP generation
 */
@Serializable
data class PtpAIRequest(
    val questionnaire: PtpQuestionnaire,
    val photoAnalysisResults: List<PhotoAnalysisResult> = emptyList(),
    val projectHistory: List<PreviousHazard> = emptyList(),
    val includeSpanish: Boolean = false
)

/**
 * Photo analysis result from previous AI analysis
 */
@Serializable
data class PhotoAnalysisResult(
    val photoId: String,
    val photoUrl: String? = null,
    val detectedHazards: List<String> = emptyList(),
    val oshaCodes: List<String> = emptyList(),
    val severity: String? = null,
    val timestamp: Long
)

/**
 * Previous hazard from project history
 */
@Serializable
data class PreviousHazard(
    val workType: String,
    val hazardDescription: String,
    val oshaCode: String,
    val mitigationUsed: String,
    val dateOccurred: Long
)

/**
 * AI response with generated PTP content
 */
@Serializable
data class PtpAIResponse(
    val success: Boolean,
    val content: PtpContent? = null,
    val confidence: Double = 0.0, // 0.0 to 1.0
    val processingTimeMs: Long = 0,
    val warnings: List<String> = emptyList(),
    val errorMessage: String? = null
)

/**
 * AI prompt template for PTP generation
 */
object PtpAIPrompt {
    fun buildPrompt(request: PtpAIRequest): String {
        val questionnaire = request.questionnaire

        return """
You are an OSHA-certified construction safety expert with 20+ years of field experience. Generate a comprehensive Pre-Task Plan (Job Hazard Analysis) based on the following information:

WORK DETAILS:
- Work Type: ${questionnaire.workType}
- Specific Tasks: ${questionnaire.specificTasks.joinToString(", ")}
- Tools & Equipment: ${questionnaire.toolsEquipment.joinToString(", ")}
- Mechanical Equipment: ${questionnaire.mechanicalEquipment.joinToString(", ")}
- Environmental Conditions: ${questionnaire.environmentalConditions.joinToString(", ")}
- Materials: ${questionnaire.materialsInvolved.joinToString(", ")}
- Crew Size: ${questionnaire.crewSize}
- Working at Height: ${if (questionnaire.workingAtHeight) "Yes (max ${questionnaire.maximumHeight ?: "unknown"} feet)" else "No"}
- Fall Protection: ${questionnaire.fallProtection.joinToString(", ")}
- Near Power Lines: ${if (questionnaire.nearPowerLines) "Yes" else "No"}
- Confined Space: ${if (questionnaire.confinedSpace) "Yes" else "No"}
- Hazardous Materials: ${if (questionnaire.hazardousMaterials) "Yes" else "No"}

${if (request.photoAnalysisResults.isNotEmpty()) """
PHOTO ANALYSIS RESULTS:
${request.photoAnalysisResults.joinToString("\n") { photo ->
    "- Photo ${photo.photoId}: Detected hazards: ${photo.detectedHazards.joinToString(", ")}, OSHA Codes: ${photo.oshaCodes.joinToString(", ")}, Severity: ${photo.severity}"
}}
""" else ""}

${if (request.projectHistory.isNotEmpty()) """
PROJECT HISTORY (Similar Work):
${request.projectHistory.joinToString("\n") { hazard ->
    "- ${hazard.workType}: ${hazard.hazardDescription} (${hazard.oshaCode}) - Mitigation: ${hazard.mitigationUsed}"
}}
""" else ""}

${if (questionnaire.additionalNotes != null) """
ADDITIONAL NOTES:
${questionnaire.additionalNotes}
""" else ""}

Generate a comprehensive PTP with the following requirements:

1. **All potential hazards** with accurate OSHA 1926 code references
   - Use specific subpart codes (e.g., 1926.501(b)(1) for unprotected sides/edges)
   - Include severity classification (MINOR, MAJOR, CRITICAL)
   - Critical = immediate danger of death or serious injury
   - Major = potential for serious injury
   - Minor = potential for minor injury

2. **Specific control measures and mitigations** for each hazard
   - Engineering controls (first priority)
   - Administrative controls
   - PPE requirements (last resort)
   - Be specific and actionable

3. **Job steps breakdown** with hazards per step
   - Sequential order of work
   - Hazards specific to each step
   - Controls applicable to each step
   - Required PPE for each step

4. **Emergency procedures** relevant to this work
   - Immediate actions for common emergencies
   - Evacuation procedures
   - First aid considerations

5. **Required training and certifications**
   - OSHA required training
   - Competent person requirements
   - Equipment operator certifications

CRITICAL REQUIREMENTS:
- Use only OSHA 1926 construction standards (not general industry 1910)
- Be construction field-specific, not generic
- Consider real-world construction site conditions
- Use clear, simple language (6th-8th grade reading level)
- Be specific to the work type and conditions provided
- Include height-specific requirements if working above 6 feet
- Address electrical safety if near power lines
- Include confined space protocols if applicable

Format your response as JSON following this exact schema:

{
  "hazards": [
    {
      "oshaCode": "1926.501(b)(1)",
      "description": "Fall hazard from unprotected sides, edges, and leading edges at heights greater than 6 feet",
      "severity": "CRITICAL",
      "controls": [
        "Install OSHA-compliant guardrail systems on all open sides and edges",
        "Use personal fall arrest systems (PFAS) with proper anchorage when guardrails not feasible",
        "Inspect all fall protection equipment daily before use",
        "Ensure workers are trained in fall protection use and limitations"
      ],
      "requiredPpe": [
        "Full-body harness with shock-absorbing lanyard",
        "Hard hat with chin strap",
        "Steel-toed boots with slip-resistant soles",
        "High-visibility vest"
      ],
      "photoReferences": []
    }
  ],
  "jobSteps": [
    {
      "stepNumber": 1,
      "description": "Set up and inspect scaffolding or aerial lift",
      "hazards": [
        "Fall from height during setup",
        "Scaffold collapse from improper assembly",
        "Struck by falling tools or materials"
      ],
      "controls": [
        "Competent person to supervise scaffold erection",
        "Follow manufacturer assembly instructions",
        "Install toe boards and guardrails before use",
        "Secure all tools with lanyards"
      ],
      "ppe": [
        "Hard hat",
        "Safety harness during erection",
        "Steel-toed boots",
        "Work gloves"
      ]
    }
  ],
  "emergencyProcedures": [
    "Fall incident: Do not move injured worker. Call 911 immediately. Activate site emergency response plan.",
    "Electrical contact: De-energize circuit if safe to do so. Call 911. Begin CPR if trained and necessary.",
    "Fire/explosion: Evacuate immediately to assembly point. Call 911. Account for all crew members."
  ],
  "requiredTraining": [
    "OSHA 10-hour or 30-hour Construction Safety certification",
    "Fall protection competent person training",
    "Scaffold erection and inspection training",
    "First aid and CPR certification (at least one crew member)"
  ]
}

${if (request.includeSpanish) """

ADDITIONAL REQUIREMENT:
After generating the English version, also provide Spanish translations for:
- Hazard descriptions
- Control measures
- Emergency procedures
- Required PPE

Add a "translations" field to the JSON with Spanish versions.
""" else ""}

Generate the PTP now. Be thorough, accurate, and construction field-specific.
        """.trimIndent()
    }

    /**
     * Build a prompt for regenerating PTP with user feedback
     */
    fun buildRegenerationPrompt(
        originalRequest: PtpAIRequest,
        userFeedback: String,
        previousContent: PtpContent
    ): String {
        val basePrompt = buildPrompt(originalRequest)

        return """
$basePrompt

PREVIOUS GENERATION:
The AI previously generated the following content, but the user provided feedback for improvement.

USER FEEDBACK:
$userFeedback

INSTRUCTIONS:
Regenerate the PTP incorporating the user's feedback while maintaining OSHA compliance and accuracy.
Keep any parts the user was satisfied with, and improve areas mentioned in the feedback.
        """.trimIndent()
    }
}
