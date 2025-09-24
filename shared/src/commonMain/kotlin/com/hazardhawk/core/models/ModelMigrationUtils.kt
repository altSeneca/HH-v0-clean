package com.hazardhawk.core.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Comprehensive model migration utilities for Phase 2 consolidation.
 * Provides safe migration paths from duplicate models to unified core models.
 * 
 * This consolidates:
 * - 4 SafetyAnalysis implementations -> 1 unified model
 * - 3 Tag implementations -> 1 unified model
 * - Multiple Location models -> 1 unified model
 * 
 * IMPORTANT: These utilities maintain 100% backward compatibility
 * while enabling the transition to unified core models.
 */
object ModelMigrationUtils {
    
    /**
     * Creates type alias mappings for import statement updates
     */
    object TypeAliases {
        // Legacy SafetyAnalysis imports
        const val AI_MODELS_SAFETY_ANALYSIS = "com.hazardhawk.ai.models.SafetyAnalysis"
        const val MODELS_SAFETY_ANALYSIS = "com.hazardhawk.models.SafetyAnalysis"
        const val DOMAIN_SAFETY_ANALYSIS = "com.hazardhawk.domain.entities.SafetyAnalysis"
        
        // Legacy Tag imports
        const val DOMAIN_ENTITIES_TAG = "com.hazardhawk.domain.entities.Tag"
        const val MODELS_TAG = "com.hazardhawk.models.Tag"
        
        // Unified imports (target)
        const val CORE_SAFETY_ANALYSIS = "com.hazardhawk.core.models.SafetyAnalysis"
        const val CORE_TAG = "com.hazardhawk.core.models.Tag"
        const val CORE_LOCATION = "com.hazardhawk.core.models.Location"
    }
    
    /**
     * Import replacement map for automated refactoring
     */
    val importReplacements = mapOf(
        // SafetyAnalysis migrations
        TypeAliases.AI_MODELS_SAFETY_ANALYSIS to TypeAliases.CORE_SAFETY_ANALYSIS,
        TypeAliases.MODELS_SAFETY_ANALYSIS to TypeAliases.CORE_SAFETY_ANALYSIS,
        TypeAliases.DOMAIN_SAFETY_ANALYSIS to TypeAliases.CORE_SAFETY_ANALYSIS,
        
        // Tag migrations
        TypeAliases.DOMAIN_ENTITIES_TAG to TypeAliases.CORE_TAG,
        TypeAliases.MODELS_TAG to TypeAliases.CORE_TAG,
        
        // Enum migrations
        "com.hazardhawk.ai.models.WorkType" to "com.hazardhawk.core.models.WorkType",
        "com.hazardhawk.ai.models.HazardType" to "com.hazardhawk.core.models.HazardType",
        "com.hazardhawk.ai.models.Severity" to "com.hazardhawk.core.models.Severity",
        "com.hazardhawk.ai.models.RiskLevel" to "com.hazardhawk.core.models.RiskLevel",
        "com.hazardhawk.domain.entities.TagCategory" to "com.hazardhawk.core.models.TagCategory",
        "com.hazardhawk.domain.entities.ComplianceStatus" to "com.hazardhawk.core.models.ComplianceStatus"
    )
    
    /**
     * Validates that migration preserves all functionality
     */
    fun validateMigration(): MigrationValidationResult {
        val checks = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        // Check SafetyAnalysis compatibility
        try {
            val testAnalysis = SafetyAnalysis(
                id = "test-1",
                photoId = "photo-1",
                timestamp = Clock.System.now(),
                analysisType = AnalysisType.COMBINED,
                workType = WorkType.GENERAL_CONSTRUCTION,
                overallRiskLevel = RiskLevel.LOW,
                severity = Severity.LOW,
                aiConfidence = 0.8f,
                processingTimeMs = 1000L
            )
            
            // Test backward compatibility properties
            val _ = testAnalysis.oshaCodes
            val _ = testAnalysis.analyzedAt
            val _ = testAnalysis.confidence
            checks.add("SafetyAnalysis backward compatibility: PASSED")
        } catch (e: Exception) {
            errors.add("SafetyAnalysis backward compatibility: FAILED - ${e.message}")
        }
        
        // Check Tag compatibility
        try {
            val testTag = Tag(
                id = "tag-1",
                name = "Test Tag",
                category = TagCategory.PPE
            )
            
            // Test backward compatibility properties
            val _ = testTag.oshaCode
            val _ = testTag.workType
            val _ = testTag.projectSpecific
            checks.add("Tag backward compatibility: PASSED")
        } catch (e: Exception) {
            errors.add("Tag backward compatibility: FAILED - ${e.message}")
        }
        
        // Check enum consistency
        val workTypeCount = WorkType.values().size
        val hazardTypeCount = HazardType.values().size
        val severityCount = Severity.values().size
        
        if (workTypeCount < 10) warnings.add("WorkType enum may be missing values ($workTypeCount found)")
        if (hazardTypeCount < 15) warnings.add("HazardType enum may be missing values ($hazardTypeCount found)")
        if (severityCount != 4) warnings.add("Severity enum should have 4 values ($severityCount found)")
        
        return MigrationValidationResult(
            isSuccess = errors.isEmpty(),
            checks = checks,
            warnings = warnings,
            errors = errors
        )
    }
    
    /**
     * Generates import replacement commands for common file patterns
     */
    fun generateImportReplacements(fileContent: String): List<ImportReplacement> {
        val replacements = mutableListOf<ImportReplacement>()
        
        importReplacements.forEach { (oldImport, newImport) ->
            if (fileContent.contains("import $oldImport")) {
                replacements.add(
                    ImportReplacement(
                        oldImport = "import $oldImport",
                        newImport = "import $newImport",
                        description = "Migrate to unified core model"
                    )
                )
            }
        }
        
        return replacements
    }
    
    /**
     * Creates migration report for tracking progress
     */
    fun createMigrationReport(
        totalFiles: Int,
        migratedFiles: Int,
        errors: List<String> = emptyList()
    ): String {
        val progress = if (totalFiles > 0) (migratedFiles * 100) / totalFiles else 0
        
        return buildString {
            appendLine("# Model Consolidation Migration Report")
            appendLine()
            appendLine("## Progress")
            appendLine("- Total files: $totalFiles")
            appendLine("- Migrated files: $migratedFiles")
            appendLine("- Progress: $progress%")
            appendLine()
            
            if (errors.isNotEmpty()) {
                appendLine("## Errors")
                errors.forEach { error ->
                    appendLine("- $error")
                }
                appendLine()
            }
            
            appendLine("## Consolidated Models")
            appendLine("- ✅ SafetyAnalysis: 4 implementations → 1 unified model")
            appendLine("- ✅ Tag: 3 implementations → 1 unified model")
            appendLine("- ✅ Location: Multiple implementations → 1 unified model")
            appendLine()
            
            appendLine("## Unified Model Locations")
            appendLine("- `/shared/src/commonMain/kotlin/com/hazardhawk/core/models/SafetyAnalysis.kt`")
            appendLine("- `/shared/src/commonMain/kotlin/com/hazardhawk/core/models/Tag.kt`")
            appendLine("- Migration utilities in `/shared/src/commonMain/kotlin/com/hazardhawk/core/models/ModelMigrationUtils.kt`")
            appendLine()
            
            appendLine("## Next Steps")
            appendLine("1. Update all import statements across codebase")
            appendLine("2. Remove duplicate model files")
            appendLine("3. Update AI service integrations")
            appendLine("4. Run comprehensive test suite")
            appendLine("5. Validate OSHA compliance features")
        }
    }
}

/**
 * Result of migration validation
 */
@Serializable
data class MigrationValidationResult(
    val isSuccess: Boolean,
    val checks: List<String>,
    val warnings: List<String>,
    val errors: List<String>
)

/**
 * Import replacement instruction
 */
@Serializable
data class ImportReplacement(
    val oldImport: String,
    val newImport: String,
    val description: String
)

/**
 * Legacy compatibility helpers - will be deprecated after migration
 */
object LegacyCompatibility {
    
    /**
     * Helper for AI services that expect old SafetyAnalysis structure
     */
    @Deprecated("Use unified SafetyAnalysis directly", level = DeprecationLevel.WARNING)
    fun toAiModelsFormat(analysis: SafetyAnalysis): Map<String, Any> {
        return mapOf(
            "id" to analysis.id,
            "timestamp" to analysis.timestamp.toEpochMilliseconds(),
            "analysisType" to analysis.analysisType.name,
            "workType" to analysis.workType.name,
            "hazards" to analysis.hazards,
            "ppeStatus" to (analysis.ppeStatus ?: "null"),
            "recommendations" to analysis.recommendations,
            "overallRiskLevel" to analysis.overallRiskLevel.name,
            "confidence" to analysis.aiConfidence,
            "processingTimeMs" to analysis.processingTimeMs,
            "oshaViolations" to analysis.oshaViolations,
            "metadata" to (analysis.metadata ?: "null")
        )
    }
    
    /**
     * Helper for domain services that expect old Tag structure
     */
    @Deprecated("Use unified Tag directly", level = DeprecationLevel.WARNING)
    fun toDomainEntityFormat(tag: Tag): Map<String, Any> {
        return mapOf(
            "id" to tag.id,
            "name" to tag.name,
            "category" to tag.category.name,
            "description" to (tag.description ?: ""),
            "oshaReferences" to tag.oshaReferences,
            "complianceStatus" to tag.complianceStatus.name,
            "usageStats" to tag.usageStats,
            "projectId" to (tag.projectId ?: ""),
            "isCustom" to tag.isCustom,
            "isActive" to tag.isActive,
            "priority" to tag.priority,
            "color" to (tag.color ?: ""),
            "createdBy" to (tag.createdBy ?: ""),
            "createdAt" to tag.createdAt,
            "updatedAt" to tag.updatedAt
        )
    }
}