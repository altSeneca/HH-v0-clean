package com.hazardhawk.ar

import com.hazardhawk.core.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Cross-platform tests for OSHA compliance engine used in AR safety monitoring.
 * Tests compliance validation, violation detection, and regulatory requirements.
 */
class OSHAComplianceEngineTest {

    private lateinit var oshaComplianceEngine: OSHAComplianceEngine

    @BeforeTest
    fun setUp() {
        oshaComplianceEngine = OSHAComplianceEngine()
    }

    @Test
    fun oshaComplianceEngine_initializesWithRegulations() {
        // Given
        val engine = OSHAComplianceEngine()
        
        // When
        val isInitialized = engine.isInitialized()
        val regulationCount = engine.getLoadedRegulationCount()
        
        // Then
        assertTrue(isInitialized, "Engine should initialize successfully")
        assertTrue(regulationCount > 0, "Should load OSHA regulations")
    }

    @Test
    fun oshaComplianceEngine_validatesFallProtectionCompliance() = runTest {
        // Given
        val fallProtectionHazard = createFallProtectionHazard()
        val workSite = createConstructionWorkSite(hasActiveFallProtection = false)
        
        // When
        val complianceResult = oshaComplianceEngine.evaluateCompliance(
            hazards = listOf(fallProtectionHazard),
            workSite = workSite,
            workType = WorkType.GENERAL_CONSTRUCTION
        )
        
        // Then
        assertFalse(complianceResult.isCompliant, "Should detect fall protection violation")
        
        val violations = complianceResult.violations
        assertTrue(violations.isNotEmpty(), "Should identify specific violations")
        
        val fallViolation = violations.find { it.regulationCode == "1926.501(b)(1)" }
        assertNotNull(fallViolation, "Should identify specific fall protection violation")
        assertEquals(ViolationSeverity.CRITICAL, fallViolation.severity)
    }

    @Test
    fun oshaComplianceEngine_validatesPPECompliance() = runTest {
        // Given
        val ppeStatus = createNonCompliantPPEStatus()
        val workSite = createConstructionWorkSite()
        
        // When
        val complianceResult = oshaComplianceEngine.evaluatePPECompliance(
            ppeStatus = ppeStatus,
            workSite = workSite,
            workType = WorkType.GENERAL_CONSTRUCTION
        )
        
        // Then
        assertFalse(complianceResult.isCompliant, "Should detect PPE violations")
        
        val ppeViolations = complianceResult.violations.filter { 
            it.regulationCode.startsWith("1926.95") 
        }
        assertTrue(ppeViolations.isNotEmpty(), "Should identify PPE violations")
        
        // Missing hard hat should be high severity
        val hardHatViolation = ppeViolations.find { 
            it.description.contains("hard hat", ignoreCase = true) 
        }
        assertNotNull(hardHatViolation, "Should detect missing hard hat")
    }

    @Test
    fun oshaComplianceEngine_validatesElectricalSafety() = runTest {
        // Given
        val electricalHazards = listOf(
            createElectricalHazard(type = "exposed_wiring"),
            createElectricalHazard(type = "missing_lockout")
        )
        val workSite = createElectricalWorkSite()
        
        // When
        val complianceResult = oshaComplianceEngine.evaluateCompliance(
            hazards = electricalHazards,
            workSite = workSite,
            workType = WorkType.ELECTRICAL_WORK
        )
        
        // Then
        assertFalse(complianceResult.isCompliant, "Should detect electrical violations")
        
        val electricalViolations = complianceResult.violations.filter {
            it.regulationCode.startsWith("1926.4")
        }
        assertTrue(electricalViolations.size >= 2, "Should detect multiple electrical violations")
        
        // Lockout/tagout violations should be critical
        val lockoutViolation = electricalViolations.find {
            it.description.contains("lockout", ignoreCase = true)
        }
        assertNotNull(lockoutViolation, "Should detect lockout/tagout violation")
        assertEquals(ViolationSeverity.CRITICAL, lockoutViolation.severity)
    }

    @Test
    fun oshaComplianceEngine_adaptsToWorkTypeRequirements() = runTest {
        // Given
        val genericHazard = createGenericHazard()
        val constructionSite = createConstructionWorkSite()
        val electricalSite = createElectricalWorkSite()
        
        // When - Evaluate same hazard for different work types
        val constructionResult = oshaComplianceEngine.evaluateCompliance(
            hazards = listOf(genericHazard),
            workSite = constructionSite,
            workType = WorkType.GENERAL_CONSTRUCTION
        )
        
        val electricalResult = oshaComplianceEngine.evaluateCompliance(
            hazards = listOf(genericHazard),
            workSite = electricalSite,
            workType = WorkType.ELECTRICAL_WORK
        )
        
        // Then - Should apply different standards
        assertNotEquals(constructionResult.requiredStandards, electricalResult.requiredStandards,
            "Should apply different OSHA standards based on work type")
    }

    @Test
    fun oshaComplianceEngine_calculatesSeverityCorrectly() = runTest {
        // Given
        val mixedHazards = listOf(
            createFallProtectionHazard(height = 10f), // High fall = critical
            createPPEViolationHazard(missing = "safety_glasses"), // Lower severity
            createElectricalHazard(voltage = "480V") // High voltage = critical
        )
        val workSite = createConstructionWorkSite()
        
        // When
        val complianceResult = oshaComplianceEngine.evaluateCompliance(
            hazards = mixedHazards,
            workSite = workSite,
            workType = WorkType.GENERAL_CONSTRUCTION
        )
        
        // Then
        val criticalViolations = complianceResult.violations.filter { 
            it.severity == ViolationSeverity.CRITICAL 
        }
        val minorViolations = complianceResult.violations.filter { 
            it.severity == ViolationSeverity.MINOR 
        }
        
        assertTrue(criticalViolations.size >= 2, "Should identify critical violations")
        assertTrue(minorViolations.isNotEmpty(), "Should identify minor violations")
        
        assertEquals(ViolationSeverity.CRITICAL, complianceResult.overallSeverity,
            "Overall severity should match highest individual violation")
    }

    @Test
    fun oshaComplianceEngine_providesCorrectiveActions() = runTest {
        // Given
        val scaffoldingHazard = createScaffoldingHazard()
        val workSite = createConstructionWorkSite()
        
        // When
        val complianceResult = oshaComplianceEngine.evaluateCompliance(
            hazards = listOf(scaffoldingHazard),
            workSite = workSite,
            workType = WorkType.GENERAL_CONSTRUCTION
        )
        
        // Then
        val scaffoldingViolation = complianceResult.violations.first()
        assertTrue(scaffoldingViolation.correctiveActions.isNotEmpty(),
            "Should provide corrective actions")
        
        val immediateActions = scaffoldingViolation.correctiveActions.filter { 
            it.priority == ActionPriority.IMMEDIATE 
        }
        assertTrue(immediateActions.isNotEmpty(), "Should have immediate actions for violations")
        
        // Should include specific scaffolding requirements
        val scaffoldingAction = scaffoldingViolation.correctiveActions.find {
            it.description.contains("scaffolding", ignoreCase = true)
        }
        assertNotNull(scaffoldingAction, "Should include scaffolding-specific actions")
    }

    @Test
    fun oshaComplianceEngine_tracksComplianceHistory() = runTest {
        // Given
        val hazard = createFallProtectionHazard()
        val workSite = createConstructionWorkSite()
        
        // When - Multiple evaluations over time
        repeat(3) { iteration ->
            oshaComplianceEngine.evaluateCompliance(
                hazards = listOf(hazard),
                workSite = workSite,
                workType = WorkType.GENERAL_CONSTRUCTION,
                timestamp = System.currentTimeMillis() + iteration * 1000L
            )
        }
        
        val complianceHistory = oshaComplianceEngine.getComplianceHistory(workSite.id)
        
        // Then
        assertEquals(3, complianceHistory.size, "Should track compliance history")
        
        val latestEvaluation = complianceHistory.maxByOrNull { it.timestamp }
        assertNotNull(latestEvaluation, "Should have latest evaluation")
        assertFalse(latestEvaluation.isCompliant, "Should track compliance status over time")
    }

    @Test
    fun oshaComplianceEngine_handlesPriorityMatrix() = runTest {
        // Given
        val multipleHazards = listOf(
            createFallProtectionHazard(height = 15f), // High priority
            createPPEViolationHazard(missing = "hard_hat"), // Medium priority
            createHousekeepingHazard(), // Low priority
            createElectricalHazard(voltage = "277V") // High priority
        )
        val workSite = createConstructionWorkSite()
        
        // When
        val complianceResult = oshaComplianceEngine.evaluateCompliance(
            hazards = multipleHazards,
            workSite = workSite,
            workType = WorkType.GENERAL_CONSTRUCTION
        )
        
        // Then
        val prioritizedViolations = complianceResult.violations.sortedByDescending { 
            it.severity.ordinal 
        }
        
        // Critical violations should be first
        val firstViolation = prioritizedViolations.first()
        assertEquals(ViolationSeverity.CRITICAL, firstViolation.severity,
            "Critical violations should be prioritized")
        
        // Should provide risk-based recommendations
        val riskMatrix = oshaComplianceEngine.generateRiskMatrix(complianceResult.violations)
        assertTrue(riskMatrix.highRiskItems.isNotEmpty(), "Should identify high-risk items")
    }

    @Test
    fun oshaComplianceEngine_validatesTemporaryStructures() = runTest {
        // Given
        val temporaryStructures = listOf(
            createTemporaryStructure("scaffolding", isSecure = false),
            createTemporaryStructure("ladder", isSecure = true),
            createTemporaryStructure("temporary_bridge", isSecure = false)
        )
        val workSite = createConstructionWorkSite(temporaryStructures = temporaryStructures)
        
        // When
        val structuralCompliance = oshaComplianceEngine.evaluateStructuralCompliance(workSite)
        
        // Then
        assertFalse(structuralCompliance.isCompliant, 
            "Should detect unsafe temporary structures")
        
        val structuralViolations = structuralCompliance.violations.filter {
            it.category == ViolationCategory.STRUCTURAL_SAFETY
        }
        assertEquals(2, structuralViolations.size, 
            "Should detect unsafe scaffolding and bridge")
    }

    @Test
    fun oshaComplianceEngine_generatesComplianceReport() = runTest {
        // Given
        val comprehensiveHazards = listOf(
            createFallProtectionHazard(),
            createElectricalHazard(),
            createPPEViolationHazard(),
            createMachineryHazard(),
            createHousekeepingHazard()
        )
        val workSite = createConstructionWorkSite()
        
        // When
        val complianceResult = oshaComplianceEngine.evaluateCompliance(
            hazards = comprehensiveHazards,
            workSite = workSite,
            workType = WorkType.GENERAL_CONSTRUCTION
        )
        
        val complianceReport = oshaComplianceEngine.generateComplianceReport(complianceResult)
        
        // Then
        assertNotNull(complianceReport, "Should generate compliance report")
        assertTrue(complianceReport.violations.isNotEmpty(), "Report should include violations")
        assertTrue(complianceReport.recommendations.isNotEmpty(), "Report should include recommendations")
        assertNotNull(complianceReport.complianceScore, "Should calculate compliance score")
        assertTrue(complianceReport.complianceScore in 0.0..1.0, 
            "Compliance score should be between 0 and 1")
    }

    @Test
    fun oshaComplianceEngine_handlesRegulationUpdates() = runTest {
        // Given
        val initialRegulationCount = oshaComplianceEngine.getLoadedRegulationCount()
        
        // When
        val updateResult = oshaComplianceEngine.updateRegulations()
        
        // Then
        assertTrue(updateResult.isSuccess, "Should update regulations successfully")
        
        val updatedRegulationCount = oshaComplianceEngine.getLoadedRegulationCount()
        assertTrue(updatedRegulationCount >= initialRegulationCount,
            "Should maintain or increase regulation count after update")
    }

    // Helper methods for creating test data
    private fun createFallProtectionHazard(height: Float = 8f): Hazard {
        return Hazard(
            id = "fall_protection_test",
            type = HazardType.FALL_PROTECTION,
            severity = if (height > 6f) Severity.CRITICAL else Severity.HIGH,
            description = "Worker at ${height}ft height without fall protection",
            oshaCode = "1926.501(b)(1)",
            boundingBox = BoundingBox(0.3f, 0.2f, 0.25f, 0.4f),
            confidence = 0.9f,
            recommendations = listOf("Install fall protection system"),
            metadata = mapOf("height" to height)
        )
    }

    private fun createElectricalHazard(type: String = "exposed_wiring", voltage: String = "120V"): Hazard {
        return Hazard(
            id = "electrical_test_$type",
            type = HazardType.ELECTRICAL_HAZARD,
            severity = if (voltage.contains("480") || type == "missing_lockout") Severity.CRITICAL else Severity.HIGH,
            description = "Electrical hazard: $type at $voltage",
            oshaCode = when (type) {
                "missing_lockout" -> "1926.417(a)"
                "exposed_wiring" -> "1926.416(a)(1)"
                else -> "1926.405(a)"
            },
            boundingBox = BoundingBox(0.5f, 0.5f, 0.2f, 0.15f),
            confidence = 0.85f,
            recommendations = listOf("Address electrical safety"),
            metadata = mapOf("voltage" to voltage, "hazard_type" to type)
        )
    }

    private fun createPPEViolationHazard(missing: String = "hard_hat"): Hazard {
        return Hazard(
            id = "ppe_test_$missing",
            type = HazardType.PPE_VIOLATION,
            severity = when (missing) {
                "hard_hat", "safety_harness" -> Severity.HIGH
                "safety_glasses", "gloves" -> Severity.MEDIUM
                else -> Severity.LOW
            },
            description = "Missing PPE: $missing",
            oshaCode = "1926.95(a)",
            boundingBox = BoundingBox(0.4f, 0.1f, 0.2f, 0.3f),
            confidence = 0.8f,
            recommendations = listOf("Don required PPE: $missing"),
            metadata = mapOf("missing_ppe" to missing)
        )
    }

    private fun createGenericHazard(): Hazard {
        return Hazard(
            id = "generic_test",
            type = HazardType.HOUSEKEEPING,
            severity = Severity.MEDIUM,
            description = "General safety concern",
            oshaCode = "1926.25(a)",
            boundingBox = BoundingBox(0.6f, 0.6f, 0.2f, 0.2f),
            confidence = 0.7f,
            recommendations = listOf("Address safety concern")
        )
    }

    private fun createScaffoldingHazard(): Hazard {
        return Hazard(
            id = "scaffolding_test",
            type = HazardType.STRUCTURAL_SAFETY,
            severity = Severity.CRITICAL,
            description = "Unsafe scaffolding configuration",
            oshaCode = "1926.451(f)(3)",
            boundingBox = BoundingBox(0.1f, 0.3f, 0.4f, 0.5f),
            confidence = 0.9f,
            recommendations = listOf("Secure scaffolding properly"),
            metadata = mapOf("structure_type" to "scaffolding")
        )
    }

    private fun createMachineryHazard(): Hazard {
        return Hazard(
            id = "machinery_test",
            type = HazardType.MACHINERY_HAZARD,
            severity = Severity.HIGH,
            description = "Unsafe machinery operation",
            oshaCode = "1926.1053(a)",
            boundingBox = BoundingBox(0.2f, 0.4f, 0.3f, 0.3f),
            confidence = 0.85f,
            recommendations = listOf("Implement machinery safety protocols")
        )
    }

    private fun createHousekeepingHazard(): Hazard {
        return Hazard(
            id = "housekeeping_test",
            type = HazardType.HOUSEKEEPING,
            severity = Severity.LOW,
            description = "Debris blocking walkway",
            oshaCode = "1926.25(a)",
            boundingBox = BoundingBox(0.7f, 0.8f, 0.2f, 0.1f),
            confidence = 0.75f,
            recommendations = listOf("Clear walkway debris")
        )
    }

    private fun createConstructionWorkSite(
        hasActiveFallProtection: Boolean = true,
        temporaryStructures: List<TemporaryStructure> = emptyList()
    ): WorkSite {
        return WorkSite(
            id = "construction_site_test",
            name = "Test Construction Site",
            workType = WorkType.GENERAL_CONSTRUCTION,
            hasActiveFallProtection = hasActiveFallProtection,
            temporaryStructures = temporaryStructures,
            metadata = mapOf(
                "site_type" to "construction",
                "active_fall_protection" to hasActiveFallProtection
            )
        )
    }

    private fun createElectricalWorkSite(): WorkSite {
        return WorkSite(
            id = "electrical_site_test",
            name = "Test Electrical Work Site",
            workType = WorkType.ELECTRICAL_WORK,
            hasActiveFallProtection = true,
            temporaryStructures = emptyList(),
            metadata = mapOf(
                "site_type" to "electrical",
                "voltage_levels" to listOf("120V", "277V", "480V")
            )
        )
    }

    private fun createNonCompliantPPEStatus(): PPEStatus {
        return PPEStatus(
            hardHat = PPEItem(PPEItemStatus.MISSING, 0.0f, null, true),
            safetyVest = PPEItem(PPEItemStatus.PRESENT, 0.9f, null, true),
            safetyBoots = PPEItem(PPEItemStatus.PRESENT, 0.85f, null, true),
            safetyGlasses = PPEItem(PPEItemStatus.MISSING, 0.0f, null, false),
            fallProtection = PPEItem(PPEItemStatus.MISSING, 0.0f, null, true),
            respirator = PPEItem(PPEItemStatus.UNKNOWN, 0.5f, null, false),
            overallCompliance = 0.4f
        )
    }

    private fun createTemporaryStructure(type: String, isSecure: Boolean): TemporaryStructure {
        return TemporaryStructure(
            id = "temp_${type}_test",
            type = type,
            isSecure = isSecure,
            inspectionDate = if (isSecure) System.currentTimeMillis() else 0L,
            compliance = if (isSecure) StructuralCompliance.COMPLIANT else StructuralCompliance.NON_COMPLIANT
        )
    }
}

/**
 * Mock OSHA Compliance Engine for cross-platform testing
 */
class OSHAComplianceEngine {
    private val regulations = mutableMapOf<String, OSHARegulation>()
    private val complianceHistory = mutableMapOf<String, MutableList<ComplianceEvaluation>>()

    init {
        loadOSHARegulations()
    }

    fun isInitialized(): Boolean = regulations.isNotEmpty()

    fun getLoadedRegulationCount(): Int = regulations.size

    suspend fun evaluateCompliance(
        hazards: List<Hazard>,
        workSite: WorkSite,
        workType: WorkType,
        timestamp: Long = System.currentTimeMillis()
    ): ComplianceResult {
        val violations = mutableListOf<OSHAViolation>()
        val requiredStandards = getRequiredStandards(workType)

        // Evaluate each hazard against OSHA regulations
        hazards.forEach { hazard ->
            val regulation = regulations[hazard.oshaCode]
            if (regulation != null) {
                val violation = evaluateHazardCompliance(hazard, regulation, workSite)
                if (violation != null) {
                    violations.add(violation)
                }
            }
        }

        val overallSeverity = violations.maxByOrNull { it.severity.ordinal }?.severity 
            ?: ViolationSeverity.NONE
        val isCompliant = violations.isEmpty()
        val complianceScore = calculateComplianceScore(violations, requiredStandards.size)

        val evaluation = ComplianceEvaluation(
            timestamp = timestamp,
            isCompliant = isCompliant,
            violations = violations,
            complianceScore = complianceScore
        )

        // Store in history
        complianceHistory.getOrPut(workSite.id) { mutableListOf() }.add(evaluation)

        return ComplianceResult(
            isCompliant = isCompliant,
            violations = violations,
            overallSeverity = overallSeverity,
            requiredStandards = requiredStandards,
            complianceScore = complianceScore
        )
    }

    suspend fun evaluatePPECompliance(
        ppeStatus: PPEStatus,
        workSite: WorkSite,
        workType: WorkType
    ): ComplianceResult {
        val violations = mutableListOf<OSHAViolation>()
        val requiredPPE = getRequiredPPE(workType)

        // Check each required PPE item
        requiredPPE.forEach { ppeType ->
            val ppeItem = getPPEItem(ppeStatus, ppeType)
            if (ppeItem?.status == PPEItemStatus.MISSING && ppeItem.required) {
                violations.add(
                    OSHAViolation(
                        regulationCode = "1926.95(a)",
                        description = "Missing required PPE: $ppeType",
                        severity = getPPESeverity(ppeType),
                        category = ViolationCategory.PPE,
                        correctiveActions = listOf(
                            CorrectiveAction(
                                description = "Don required $ppeType",
                                priority = ActionPriority.IMMEDIATE,
                                timeframe = "Immediately"
                            )
                        )
                    )
                )
            }
        }

        return ComplianceResult(
            isCompliant = violations.isEmpty(),
            violations = violations,
            overallSeverity = violations.maxByOrNull { it.severity.ordinal }?.severity ?: ViolationSeverity.NONE,
            requiredStandards = getRequiredStandards(workType),
            complianceScore = calculatePPEComplianceScore(ppeStatus)
        )
    }

    suspend fun evaluateStructuralCompliance(workSite: WorkSite): ComplianceResult {
        val violations = mutableListOf<OSHAViolation>()

        workSite.temporaryStructures.forEach { structure ->
            if (structure.compliance == StructuralCompliance.NON_COMPLIANT) {
                violations.add(
                    OSHAViolation(
                        regulationCode = getStructuralRegulationCode(structure.type),
                        description = "Unsafe ${structure.type} configuration",
                        severity = ViolationSeverity.CRITICAL,
                        category = ViolationCategory.STRUCTURAL_SAFETY,
                        correctiveActions = listOf(
                            CorrectiveAction(
                                description = "Secure ${structure.type} properly",
                                priority = ActionPriority.IMMEDIATE,
                                timeframe = "Immediately"
                            )
                        )
                    )
                )
            }
        }

        return ComplianceResult(
            isCompliant = violations.isEmpty(),
            violations = violations,
            overallSeverity = violations.maxByOrNull { it.severity.ordinal }?.severity ?: ViolationSeverity.NONE,
            requiredStandards = getStructuralStandards(),
            complianceScore = if (violations.isEmpty()) 1.0 else 0.5
        )
    }

    fun getComplianceHistory(workSiteId: String): List<ComplianceEvaluation> {
        return complianceHistory[workSiteId] ?: emptyList()
    }

    fun generateRiskMatrix(violations: List<OSHAViolation>): RiskMatrix {
        val highRiskItems = violations.filter { it.severity == ViolationSeverity.CRITICAL }
        val mediumRiskItems = violations.filter { it.severity == ViolationSeverity.MAJOR }
        val lowRiskItems = violations.filter { it.severity == ViolationSeverity.MINOR }

        return RiskMatrix(
            highRiskItems = highRiskItems.map { it.description },
            mediumRiskItems = mediumRiskItems.map { it.description },
            lowRiskItems = lowRiskItems.map { it.description }
        )
    }

    fun generateComplianceReport(complianceResult: ComplianceResult): ComplianceReport {
        return ComplianceReport(
            violations = complianceResult.violations,
            recommendations = complianceResult.violations.flatMap { it.correctiveActions.map { action -> action.description } },
            complianceScore = complianceResult.complianceScore,
            overallStatus = if (complianceResult.isCompliant) "COMPLIANT" else "NON_COMPLIANT",
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun updateRegulations(): Result<Unit> {
        return try {
            // Simulate regulation update
            loadOSHARegulations()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadOSHARegulations() {
        // Load common OSHA regulations for testing
        regulations["1926.501(b)(1)"] = OSHARegulation(
            code = "1926.501(b)(1)",
            title = "Fall Protection - General Requirements",
            description = "Fall protection required at 6 feet or higher",
            severity = ViolationSeverity.CRITICAL
        )
        
        regulations["1926.95(a)"] = OSHARegulation(
            code = "1926.95(a)",
            title = "Personal Protective Equipment",
            description = "Employees shall be required to wear protective equipment",
            severity = ViolationSeverity.MAJOR
        )
        
        regulations["1926.416(a)(1)"] = OSHARegulation(
            code = "1926.416(a)(1)",
            title = "General Requirements for Electrical Equipment",
            description = "Protection of live parts of electric equipment",
            severity = ViolationSeverity.CRITICAL
        )
        
        regulations["1926.417(a)"] = OSHARegulation(
            code = "1926.417(a)",
            title = "Lockout and Tagging of Circuits",
            description = "Controls that are deactivated during work must be tagged",
            severity = ViolationSeverity.CRITICAL
        )
        
        regulations["1926.451(f)(3)"] = OSHARegulation(
            code = "1926.451(f)(3)",
            title = "Scaffolding General Requirements",
            description = "Scaffolds must be secured to prevent displacement",
            severity = ViolationSeverity.CRITICAL
        )
        
        regulations["1926.25(a)"] = OSHARegulation(
            code = "1926.25(a)",
            title = "Housekeeping",
            description = "Construction areas must be kept clean and orderly",
            severity = ViolationSeverity.MINOR
        )
    }

    private fun evaluateHazardCompliance(
        hazard: Hazard,
        regulation: OSHARegulation,
        workSite: WorkSite
    ): OSHAViolation? {
        // Evaluate if hazard constitutes a violation
        val isViolation = when (hazard.type) {
            HazardType.FALL_PROTECTION -> !workSite.hasActiveFallProtection
            HazardType.PPE_VIOLATION -> true // PPE violations are always violations
            HazardType.ELECTRICAL_HAZARD -> true // Electrical hazards indicate violations
            else -> hazard.severity >= Severity.MEDIUM
        }

        return if (isViolation) {
            OSHAViolation(
                regulationCode = regulation.code,
                description = hazard.description,
                severity = mapSeverity(hazard.severity),
                category = mapCategory(hazard.type),
                correctiveActions = generateCorrectiveActions(hazard)
            )
        } else null
    }

    private fun getRequiredStandards(workType: WorkType): List<String> {
        return when (workType) {
            WorkType.GENERAL_CONSTRUCTION -> listOf(
                "1926.501", "1926.95", "1926.25"
            )
            WorkType.ELECTRICAL_WORK -> listOf(
                "1926.416", "1926.417", "1926.95"
            )
            WorkType.FALL_PROTECTION -> listOf(
                "1926.501", "1926.502", "1926.95"
            )
            else -> listOf("1926.95", "1926.25")
        }
    }

    private fun getRequiredPPE(workType: WorkType): List<String> {
        return when (workType) {
            WorkType.ELECTRICAL_WORK -> listOf("hard_hat", "safety_glasses", "insulated_gloves")
            WorkType.FALL_PROTECTION -> listOf("hard_hat", "safety_harness", "safety_boots")
            else -> listOf("hard_hat", "safety_vest", "safety_boots")
        }
    }

    private fun getPPEItem(ppeStatus: PPEStatus, ppeType: String): PPEItem? {
        return when (ppeType) {
            "hard_hat" -> ppeStatus.hardHat
            "safety_vest" -> ppeStatus.safetyVest
            "safety_boots" -> ppeStatus.safetyBoots
            "safety_glasses" -> ppeStatus.safetyGlasses
            "safety_harness", "fall_protection" -> ppeStatus.fallProtection
            "respirator" -> ppeStatus.respirator
            else -> null
        }
    }

    private fun getPPESeverity(ppeType: String): ViolationSeverity {
        return when (ppeType) {
            "hard_hat", "safety_harness" -> ViolationSeverity.MAJOR
            "safety_glasses", "safety_boots" -> ViolationSeverity.MINOR
            else -> ViolationSeverity.MINOR
        }
    }

    private fun getStructuralRegulationCode(structureType: String): String {
        return when (structureType) {
            "scaffolding" -> "1926.451(f)(3)"
            "ladder" -> "1926.1053(a)"
            "temporary_bridge" -> "1926.502(b)"
            else -> "1926.25(a)"
        }
    }

    private fun getStructuralStandards(): List<String> {
        return listOf("1926.451", "1926.502", "1926.1053")
    }

    private fun calculateComplianceScore(violations: List<OSHAViolation>, totalStandards: Int): Double {
        if (totalStandards == 0) return 1.0
        
        val violationWeight = violations.sumOf { violation ->
            when (violation.severity) {
                ViolationSeverity.CRITICAL -> 1.0
                ViolationSeverity.MAJOR -> 0.7
                ViolationSeverity.MINOR -> 0.3
                ViolationSeverity.NONE -> 0.0
            }
        }
        
        return maxOf(0.0, 1.0 - (violationWeight / totalStandards))
    }

    private fun calculatePPEComplianceScore(ppeStatus: PPEStatus): Double {
        return ppeStatus.overallCompliance.toDouble()
    }

    private fun mapSeverity(hazardSeverity: Severity): ViolationSeverity {
        return when (hazardSeverity) {
            Severity.CRITICAL -> ViolationSeverity.CRITICAL
            Severity.HIGH -> ViolationSeverity.MAJOR
            Severity.MEDIUM -> ViolationSeverity.MINOR
            Severity.LOW -> ViolationSeverity.MINOR
        }
    }

    private fun mapCategory(hazardType: HazardType): ViolationCategory {
        return when (hazardType) {
            HazardType.FALL_PROTECTION -> ViolationCategory.FALL_PROTECTION
            HazardType.PPE_VIOLATION -> ViolationCategory.PPE
            HazardType.ELECTRICAL_HAZARD -> ViolationCategory.ELECTRICAL
            HazardType.STRUCTURAL_SAFETY -> ViolationCategory.STRUCTURAL_SAFETY
            HazardType.MACHINERY_HAZARD -> ViolationCategory.MACHINERY
            else -> ViolationCategory.GENERAL
        }
    }

    private fun generateCorrectiveActions(hazard: Hazard): List<CorrectiveAction> {
        return hazard.recommendations.map { recommendation ->
            CorrectiveAction(
                description = recommendation,
                priority = if (hazard.severity >= Severity.HIGH) ActionPriority.IMMEDIATE else ActionPriority.ROUTINE,
                timeframe = if (hazard.severity == Severity.CRITICAL) "Immediately" else "Within 24 hours"
            )
        }
    }
}

// Data classes for testing
data class OSHARegulation(
    val code: String,
    val title: String,
    val description: String,
    val severity: ViolationSeverity
)

data class OSHAViolation(
    val regulationCode: String,
    val description: String,
    val severity: ViolationSeverity,
    val category: ViolationCategory,
    val correctiveActions: List<CorrectiveAction>
)

data class CorrectiveAction(
    val description: String,
    val priority: ActionPriority,
    val timeframe: String
)

data class ComplianceResult(
    val isCompliant: Boolean,
    val violations: List<OSHAViolation>,
    val overallSeverity: ViolationSeverity,
    val requiredStandards: List<String>,
    val complianceScore: Double
)

data class ComplianceEvaluation(
    val timestamp: Long,
    val isCompliant: Boolean,
    val violations: List<OSHAViolation>,
    val complianceScore: Double
)

data class ComplianceReport(
    val violations: List<OSHAViolation>,
    val recommendations: List<String>,
    val complianceScore: Double,
    val overallStatus: String,
    val timestamp: Long
)

data class WorkSite(
    val id: String,
    val name: String,
    val workType: WorkType,
    val hasActiveFallProtection: Boolean,
    val temporaryStructures: List<TemporaryStructure>,
    val metadata: Map<String, Any>
)

data class TemporaryStructure(
    val id: String,
    val type: String,
    val isSecure: Boolean,
    val inspectionDate: Long,
    val compliance: StructuralCompliance
)

data class RiskMatrix(
    val highRiskItems: List<String>,
    val mediumRiskItems: List<String>,
    val lowRiskItems: List<String>
)

enum class ViolationSeverity { NONE, MINOR, MAJOR, CRITICAL }
enum class ViolationCategory { 
    FALL_PROTECTION, PPE, ELECTRICAL, STRUCTURAL_SAFETY, MACHINERY, GENERAL 
}
enum class ActionPriority { ROUTINE, URGENT, IMMEDIATE }
enum class StructuralCompliance { COMPLIANT, NON_COMPLIANT, NEEDS_INSPECTION }
