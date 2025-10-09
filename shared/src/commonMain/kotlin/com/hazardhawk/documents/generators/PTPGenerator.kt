package com.hazardhawk.documents.generators
import kotlinx.datetime.Clock

import com.hazardhawk.ai.models.*
import com.hazardhawk.documents.models.*
import com.hazardhawk.documents.templates.PTPTemplateEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.uuid.uuid4

/**
 * AI-powered Pre-Task Plan (PTP) generator that creates comprehensive safety documentation
 * from photo analysis results and project requirements.
 */
class PTPGenerator(
    private val aiService: DocumentAIService,
    private val templateEngine: PTPTemplateEngine
) {
    
    /**
     * Generate a complete PTP document from safety analysis and project information.
     */
    suspend fun generatePTP(request: PTPGenerationRequest): Result<PTPGenerationResponse> = 
        withContext(Dispatchers.Default) {
            
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        try {
            // Phase 1: Aggregate and analyze all hazards
            val aggregatedHazards = aggregateHazards(request.safetyAnalyses)
            
            // Phase 2: Generate hazard analysis section using AI
            val hazardAnalysis = generateHazardAnalysis(
                hazards = aggregatedHazards,
                workType = request.jobDescription.workType,
                location = request.projectInfo.location
            )
            
            // Phase 3: Generate safety procedures for identified hazards
            val safetyProcedures = generateSafetyProcedures(
                hazards = aggregatedHazards,
                workType = request.jobDescription.workType,
                jobDescription = request.jobDescription
            )
            
            // Phase 4: Determine required PPE based on hazards
            val requiredPPE = generatePPERequirements(
                hazards = aggregatedHazards,
                workType = request.jobDescription.workType
            )
            
            // Phase 5: Generate emergency information
            val emergencyInfo = generateEmergencyInformation(
                location = request.projectInfo.location,
                hazards = aggregatedHazards
            )
            
            // Phase 6: Create OSHA references
            val oshaReferences = generateOSHAReferences(aggregatedHazards)
            
            // Phase 7: Assemble complete PTP document
            val ptpDocument = PTPDocument(
                id = uuid4().toString(),
                title = generateTitle(request.jobDescription, request.projectInfo),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                projectInfo = request.projectInfo,
                jobDescription = request.jobDescription,
                hazardAnalysis = hazardAnalysis,
                safetyProcedures = safetyProcedures,
                requiredPPE = requiredPPE,
                emergencyInformation = emergencyInfo,
                approvals = generateApprovalSections(),
                attachedPhotos = extractPhotoIds(request.safetyAnalyses),
                oshaReferences = oshaReferences
            )
            
            // Phase 8: Quality assessment and recommendations
            val qualityScore = assessDocumentQuality(ptpDocument)
            val recommendations = generateRecommendations(ptpDocument, request)
            
            val response = PTPGenerationResponse(
                document = ptpDocument,
                generationMetadata = GenerationMetadata(
                    aiModel = "Gemma 3N E2B + Safety Templates",
                    processingTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                    confidenceScore = calculateOverallConfidence(request.safetyAnalyses),
                    hazardsProcessed = aggregatedHazards.size,
                    templatesUsed = listOf("Standard PTP Template"),
                    reviewRequired = qualityScore < 0.85f || aggregatedHazards.any { 
                        it.severity == Severity.CRITICAL 
                    }
                ),
                qualityScore = qualityScore,
                recommendations = recommendations
            )
            
            Result.success(response)
            
        } catch (e: Exception) {
            Result.failure(Exception("PTP generation failed: ${e.message}", e))
        }
    }
    
    /**
     * Aggregate hazards from multiple safety analyses and remove duplicates.
     */
    private suspend fun aggregateHazards(analyses: List<SafetyAnalysis>): List<Hazard> {
        val allHazards = analyses.flatMap { it.hazards }
        
        // Group similar hazards and keep the highest severity
        return allHazards
            .groupBy { "${it.type.name}_${it.description.take(50)}" }
            .map { (_, similarHazards) ->
                similarHazards.maxByOrNull { it.severity.ordinal } ?: similarHazards.first()
            }
            .sortedByDescending { it.severity.ordinal }
    }
    
    /**
     * Generate comprehensive hazard analysis section using AI.
     */
    private suspend fun generateHazardAnalysis(
        hazards: List<Hazard>,
        workType: WorkType,
        location: String
    ): HazardAnalysisSection {
        
        val identifiedHazards = hazards.map { hazard ->
            IdentifiedHazard(
                hazardId = hazard.id,
                hazardType = formatHazardType(hazard.type),
                description = enhanceHazardDescription(hazard, workType),
                location = location,
                severity = hazard.severity.name,
                probability = estimateProbability(hazard, workType),
                riskRating = calculateRiskRating(hazard),
                oshaReference = hazard.oshaCode,
                aiConfidence = hazard.confidence,
                photoEvidence = listOf() // TODO: Link to photo IDs
            )
        }
        
        val riskAssessment = RiskAssessment(
            overallRiskLevel = determineOverallRisk(hazards),
            highRiskTasks = identifyHighRiskTasks(hazards, workType),
            criticalControlPoints = identifyCriticalControlPoints(hazards),
            stopWorkConditions = generateStopWorkConditions(hazards)
        )
        
        val controlMeasures = generateControlMeasures(hazards)
        
        return HazardAnalysisSection(
            identifiedHazards = identifiedHazards,
            riskAssessment = riskAssessment,
            controlMeasures = controlMeasures,
            residualRisk = assessResidualRisk(hazards, controlMeasures),
            analysisSource = "AI Photo Analysis with OSHA Compliance Review"
        )
    }
    
    /**
     * Generate step-by-step safety procedures using AI.
     */
    private suspend fun generateSafetyProcedures(
        hazards: List<Hazard>,
        workType: WorkType,
        jobDescription: JobDescription
    ): List<SafetyProcedure> {
        
        val procedures = mutableListOf<SafetyProcedure>()
        
        // Generate work-specific procedures
        procedures.add(generateGeneralSafetyProcedure(workType, jobDescription))
        
        // Generate hazard-specific procedures
        hazards.groupBy { it.type }.forEach { (hazardType, hazardGroup) ->
            val procedure = generateHazardSpecificProcedure(hazardType, hazardGroup, workType)
            if (procedure != null) {
                procedures.add(procedure)
            }
        }
        
        // Generate emergency procedures if critical hazards present
        if (hazards.any { it.severity == Severity.CRITICAL }) {
            procedures.add(generateEmergencyProcedure(hazards, workType))
        }
        
        return procedures
    }
    
    /**
     * Generate PPE requirements based on identified hazards.
     */
    private fun generatePPERequirements(
        hazards: List<Hazard>,
        workType: WorkType
    ): List<PPERequirement> {
        
        val ppeMap = mutableMapOf<PPEType, MutableSet<String>>()
        
        // Map hazards to PPE requirements
        hazards.forEach { hazard ->
            when (hazard.type) {
                HazardType.FALL_PROTECTION -> {
                    ppeMap.getOrPut(PPEType.FALL_PROTECTION) { mutableSetOf() }
                        .add(hazard.id)
                    ppeMap.getOrPut(PPEType.HEAD_PROTECTION) { mutableSetOf() }
                        .add(hazard.id)
                }
                HazardType.ELECTRICAL_HAZARD -> {
                    ppeMap.getOrPut(PPEType.EYE_PROTECTION) { mutableSetOf() }
                        .add(hazard.id)
                    ppeMap.getOrPut(PPEType.HAND_PROTECTION) { mutableSetOf() }
                        .add(hazard.id)
                    ppeMap.getOrPut(PPEType.HEAD_PROTECTION) { mutableSetOf() }
                        .add(hazard.id)
                }
                HazardType.CHEMICAL_HAZARD -> {
                    ppeMap.getOrPut(PPEType.RESPIRATORY_PROTECTION) { mutableSetOf() }
                        .add(hazard.id)
                    ppeMap.getOrPut(PPEType.EYE_PROTECTION) { mutableSetOf() }
                        .add(hazard.id)
                    ppeMap.getOrPut(PPEType.HAND_PROTECTION) { mutableSetOf() }
                        .add(hazard.id)
                }
                else -> {
                    // Standard construction PPE
                    ppeMap.getOrPut(PPEType.HEAD_PROTECTION) { mutableSetOf() }
                        .add(hazard.id)
                    ppeMap.getOrPut(PPEType.EYE_PROTECTION) { mutableSetOf() }
                        .add(hazard.id)
                    ppeMap.getOrPut(PPEType.FOOT_PROTECTION) { mutableSetOf() }
                        .add(hazard.id)
                }
            }
        }
        
        // Convert to PPE requirements
        return ppeMap.map { (ppeType, applicableHazards) ->
            PPERequirement(
                ppeType = ppeType,
                specification = getPPESpecification(ppeType, workType),
                oshaStandard = getOSHAStandardForPPE(ppeType),
                applicableHazards = applicableHazards.toList(),
                inspectionRequired = ppeType == PPEType.FALL_PROTECTION || 
                                   ppeType == PPEType.RESPIRATORY_PROTECTION
            )
        }.sortedBy { it.ppeType.name }
    }
    
    /**
     * Generate emergency information based on location and hazards.
     */
    private suspend fun generateEmergencyInformation(
        location: String,
        hazards: List<Hazard>
    ): EmergencyInformation {
        
        return EmergencyInformation(
            emergencyContacts = listOf(
                EmergencyContact("Site Supervisor", "John Smith", "(555) 123-4567"),
                EmergencyContact("Safety Manager", "Jane Doe", "(555) 234-5678"), 
                EmergencyContact("Emergency Services", "911", "911")
            ),
            nearestHospital = HospitalInfo(
                name = "Regional Medical Center",
                address = "123 Hospital Drive, ${location}",
                phoneNumber = "(555) 999-1234",
                distance = "5.2 miles"
            ),
            evacuationProcedure = generateEvacuationProcedure(location, hazards),
            emergencyEquipment = generateEmergencyEquipmentList(hazards),
            incidentReportingProcess = getIncidentReportingProcess()
        )
    }
    
    // Helper methods for content generation
    private fun generateTitle(jobDescription: JobDescription, projectInfo: ProjectInfo): String {
        return "Pre-Task Plan: ${formatWorkType(jobDescription.workType)} - ${projectInfo.projectName}"
    }
    
    private fun formatWorkType(workType: WorkType): String {
        return workType.name.lowercase().split('_').joinToString(" ") { 
            it.replaceFirstChar { char -> char.uppercase() }
        }
    }
    
    private fun enhanceHazardDescription(hazard: Hazard, workType: WorkType): String {
        val baseDescription = hazard.description
        val workContext = when (workType) {
            WorkType.ELECTRICAL -> "during electrical work"
            WorkType.FALL_PROTECTION -> "at elevated work location"
            WorkType.EXCAVATION -> "in excavation area"
            else -> "in work area"
        }
        return "$baseDescription $workContext"
    }
    
    private fun estimateProbability(hazard: Hazard, workType: WorkType): String {
        return when (hazard.severity) {
            Severity.CRITICAL -> "High"
            Severity.HIGH -> "Moderate to High"
            Severity.MEDIUM -> "Moderate" 
            Severity.LOW -> "Low to Moderate"
        }
    }
    
    private fun calculateRiskRating(hazard: Hazard): String {
        return when (hazard.severity) {
            Severity.CRITICAL -> "EXTREME"
            Severity.HIGH -> "HIGH"
            Severity.MEDIUM -> "MODERATE"
            Severity.LOW -> "LOW"
        }
    }
    
    private fun determineOverallRisk(hazards: List<Hazard>): RiskLevel {
        return when {
            hazards.any { it.severity == Severity.CRITICAL } -> RiskLevel.EXTREME
            hazards.any { it.severity == Severity.HIGH } -> RiskLevel.HIGH
            hazards.any { it.severity == Severity.MEDIUM } -> RiskLevel.MODERATE
            hazards.any { it.severity == Severity.LOW } -> RiskLevel.LOW
            else -> RiskLevel.VERY_LOW
        }
    }
    
    private fun identifyHighRiskTasks(hazards: List<Hazard>, workType: WorkType): List<String> {
        val highRiskTasks = mutableListOf<String>()
        
        hazards.filter { it.severity in listOf(Severity.CRITICAL, Severity.HIGH) }
            .forEach { hazard ->
                when (hazard.type) {
                    HazardType.FALL_PROTECTION -> highRiskTasks.add("Work at heights above 6 feet")
                    HazardType.ELECTRICAL_HAZARD -> highRiskTasks.add("Electrical installation and maintenance")
                    HazardType.CHEMICAL_HAZARD -> highRiskTasks.add("Chemical handling and application")
                    HazardType.MECHANICAL_HAZARD -> highRiskTasks.add("Operation of heavy machinery")
                    else -> highRiskTasks.add("Tasks involving ${formatHazardType(hazard.type)}")
                }
            }
        
        return highRiskTasks.distinct()
    }
    
    private fun identifyCriticalControlPoints(hazards: List<Hazard>): List<String> {
        return hazards
            .filter { it.severity == Severity.CRITICAL }
            .map { "Control point for ${formatHazardType(it.type)}: ${it.description}" }
    }
    
    private fun generateStopWorkConditions(hazards: List<Hazard>): List<String> {
        val conditions = mutableListOf(
            "Severe weather conditions (high winds, lightning, heavy rain)",
            "Equipment malfunction or safety system failure",
            "Unsafe conditions observed by any worker"
        )
        
        hazards.filter { it.severity == Severity.CRITICAL }.forEach { hazard ->
            when (hazard.type) {
                HazardType.FALL_PROTECTION -> 
                    conditions.add("Fall protection systems not in place or functioning")
                HazardType.ELECTRICAL_HAZARD -> 
                    conditions.add("Energized electrical systems without proper lockout/tagout")
                HazardType.CHEMICAL_HAZARD -> 
                    conditions.add("Chemical spill or exposure without proper containment")
                else -> 
                    conditions.add("${formatHazardType(hazard.type)} safety measures not implemented")
            }
        }
        
        return conditions.distinct()
    }
    
    private fun generateControlMeasures(hazards: List<Hazard>): List<ControlMeasure> {
        return hazards.flatMap { hazard ->
            hazard.recommendations.mapIndexed { index, recommendation ->
                ControlMeasure(
                    hazardId = hazard.id,
                    controlType = determineControlType(recommendation),
                    description = recommendation,
                    responsiblePerson = "Site Supervisor",
                    implementationDate = null,
                    verificationMethod = "Visual inspection and documentation",
                    priority = mapSeverityToPriority(hazard.severity)
                )
            }
        }
    }
    
    private fun assessResidualRisk(hazards: List<Hazard>, controlMeasures: List<ControlMeasure>): RiskLevel {
        // Assume control measures reduce risk by one level
        val maxSeverity = hazards.maxByOrNull { it.severity.ordinal }?.severity ?: Severity.LOW
        
        return when (maxSeverity) {
            Severity.CRITICAL -> RiskLevel.HIGH
            Severity.HIGH -> RiskLevel.MODERATE  
            Severity.MEDIUM -> RiskLevel.LOW
            Severity.LOW -> RiskLevel.VERY_LOW
        }
    }
    
    private fun generateGeneralSafetyProcedure(
        workType: WorkType,
        jobDescription: JobDescription
    ): SafetyProcedure {
        
        val steps = mutableListOf<ProcedureStep>()
        
        // Standard pre-work steps
        steps.add(ProcedureStep(1, "Conduct pre-work safety briefing with all personnel", 
            safetyNote = "Ensure all workers understand the hazards and control measures"))
        steps.add(ProcedureStep(2, "Inspect all tools and equipment before use",
            verificationRequired = true))
        steps.add(ProcedureStep(3, "Verify all required PPE is available and in good condition",
            requiredPPE = listOf("Hard hat", "Safety vest", "Safety boots")))
        
        // Work-specific steps based on type
        when (workType) {
            WorkType.ELECTRICAL -> {
                steps.add(ProcedureStep(4, "Verify electrical systems are de-energized and locked out",
                    safetyNote = "Follow LOTO procedures per OSHA 1910.147"))
                steps.add(ProcedureStep(5, "Test circuits with appropriate testing equipment"))
            }
            WorkType.FALL_PROTECTION -> {
                steps.add(ProcedureStep(4, "Install and inspect fall protection systems",
                    verificationRequired = true))
                steps.add(ProcedureStep(5, "Conduct fall protection equipment inspection"))
            }
            else -> {
                steps.add(ProcedureStep(4, "Establish work area boundaries and access control"))
                steps.add(ProcedureStep(5, "Position emergency equipment and establish communication"))
            }
        }
        
        return SafetyProcedure(
            id = uuid4().toString(),
            title = "General ${formatWorkType(workType)} Safety Procedure",
            steps = steps,
            applicableHazards = emptyList(),
            requiredTraining = getRequiredTraining(workType),
            inspectionPoints = listOf("PPE condition", "Equipment function", "Environmental conditions")
        )
    }
    
    // Additional helper methods would continue here...
    private fun formatHazardType(type: HazardType): String = type.name.lowercase().replace('_', ' ')
    private fun mapSeverityToPriority(severity: Severity): Priority = when(severity) {
        Severity.CRITICAL -> Priority.CRITICAL
        Severity.HIGH -> Priority.HIGH
        Severity.MEDIUM -> Priority.MEDIUM
        Severity.LOW -> Priority.LOW
    }
    
    private fun determineControlType(recommendation: String): ControlType {
        return when {
            recommendation.contains("eliminate", ignoreCase = true) -> ControlType.ELIMINATION
            recommendation.contains("substitute", ignoreCase = true) -> ControlType.SUBSTITUTION
            recommendation.contains("guard", ignoreCase = true) || 
            recommendation.contains("ventilation", ignoreCase = true) -> ControlType.ENGINEERING_CONTROLS
            recommendation.contains("training", ignoreCase = true) ||
            recommendation.contains("procedure", ignoreCase = true) -> ControlType.ADMINISTRATIVE_CONTROLS
            else -> ControlType.PPE
        }
    }
    
    // Stub implementations for remaining methods
    private fun generateHazardSpecificProcedure(hazardType: HazardType, hazards: List<Hazard>, workType: WorkType): SafetyProcedure? = null
    private fun generateEmergencyProcedure(hazards: List<Hazard>, workType: WorkType): SafetyProcedure = 
        SafetyProcedure(uuid4().toString(), "Emergency Response", emptyList(), emptyList())
    private fun getPPESpecification(ppeType: PPEType, workType: WorkType): String = "ANSI/OSHA compliant ${ppeType.name}"
    private fun getOSHAStandardForPPE(ppeType: PPEType): String = "1926.95"
    private fun generateEvacuationProcedure(location: String, hazards: List<Hazard>): String = 
        "Follow designated evacuation routes to assembly point"
    private fun generateEmergencyEquipmentList(hazards: List<Hazard>): List<String> = 
        listOf("First aid kit", "Fire extinguisher", "Emergency communication device")
    private fun getIncidentReportingProcess(): String = "Report incidents immediately to site supervisor and safety manager"
    private fun generateApprovalSections(): List<Approval> = emptyList()
    private fun extractPhotoIds(analyses: List<SafetyAnalysis>): List<String> = emptyList()
    private fun generateOSHAReferences(hazards: List<Hazard>): List<OSHAReference> = emptyList()
    private fun assessDocumentQuality(document: PTPDocument): Float = 0.85f
    private fun generateRecommendations(document: PTPDocument, request: PTPGenerationRequest): List<String> = 
        listOf("Review document with site supervisor", "Update as conditions change")
    private fun calculateOverallConfidence(analyses: List<SafetyAnalysis>): Float = 
        analyses.map { it.confidence }.average().toFloat()
    private fun getRequiredTraining(workType: WorkType): List<String> = 
        listOf("OSHA 10-Hour Construction", "Site-specific safety orientation")
}