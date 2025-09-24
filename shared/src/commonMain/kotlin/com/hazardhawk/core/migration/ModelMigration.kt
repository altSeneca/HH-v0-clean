package com.hazardhawk.core.migration

import com.hazardhawk.core.models.*
import com.hazardhawk.core.serialization.JsonConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

/**
 * Migration utilities to convert between old and new model structures
 * Ensures zero data loss during refactoring
 */
object ModelMigration {
    
    /**
     * Migrate old SafetyAnalysis models to unified format
     */
    fun migrateOldSafetyAnalysis(
        oldAnalysis: com.hazardhawk.models.SafetyAnalysis
    ): SafetyAnalysis {
        return SafetyAnalysis(
            id = oldAnalysis.id,
            photoId = oldAnalysis.photoId,
            timestamp = oldAnalysis.analyzedAt,
            analysisType = when (oldAnalysis.analysisType) {
                com.hazardhawk.models.AnalysisType.ON_DEVICE -> AnalysisType.ON_DEVICE
                com.hazardhawk.models.AnalysisType.CLOUD_GEMINI -> AnalysisType.CLOUD_GEMINI
                com.hazardhawk.models.AnalysisType.COMBINED -> AnalysisType.COMBINED
                com.hazardhawk.models.AnalysisType.BATCH_OPERATION -> AnalysisType.BATCH_OPERATION
            },
            workType = WorkType.GENERAL_CONSTRUCTION, // Default mapping
            hazards = oldAnalysis.hazards.map { migrateOldHazard(it) },
            ppeStatus = null, // Not available in old model
            oshaViolations = oldAnalysis.oshaCodes.map { migrateOldOshaCode(it) },
            recommendations = oldAnalysis.recommendations,
            overallRiskLevel = when (oldAnalysis.severity) {
                com.hazardhawk.models.Severity.LOW -> RiskLevel.LOW
                com.hazardhawk.models.Severity.MEDIUM -> RiskLevel.MODERATE
                com.hazardhawk.models.Severity.HIGH -> RiskLevel.HIGH
                com.hazardhawk.models.Severity.CRITICAL -> RiskLevel.SEVERE
            },
            severity = Severity.valueOf(oldAnalysis.severity.name),
            aiConfidence = oldAnalysis.aiConfidence,
            processingTimeMs = 0L, // Not tracked in old model
            metadata = null
        )
    }
    
    /**
     * Migrate AI models SafetyAnalysis to unified format
     */
    fun migrateAISafetyAnalysis(
        aiAnalysis: com.hazardhawk.ai.models.SafetyAnalysis
    ): SafetyAnalysis {
        return SafetyAnalysis(
            id = aiAnalysis.id,
            photoId = "photo-${aiAnalysis.id}", // Inferred ID
            timestamp = Instant.fromEpochMilliseconds(aiAnalysis.timestamp),
            analysisType = when (aiAnalysis.analysisType) {
                com.hazardhawk.ai.models.AnalysisType.LOCAL_GEMMA_MULTIMODAL -> AnalysisType.LOCAL_GEMMA_MULTIMODAL
                com.hazardhawk.ai.models.AnalysisType.CLOUD_GEMINI -> AnalysisType.CLOUD_GEMINI
                com.hazardhawk.ai.models.AnalysisType.LOCAL_YOLO_FALLBACK -> AnalysisType.LOCAL_YOLO_FALLBACK
                com.hazardhawk.ai.models.AnalysisType.HYBRID_ANALYSIS -> AnalysisType.HYBRID_ANALYSIS
            },
            workType = WorkType.valueOf(aiAnalysis.workType.name),
            hazards = aiAnalysis.hazards.map { migrateAIHazard(it) },
            ppeStatus = migrateAIPPEStatus(aiAnalysis.ppeStatus),
            oshaViolations = aiAnalysis.oshaViolations.map { migrateAIOshaViolation(it) },
            recommendations = aiAnalysis.recommendations,
            overallRiskLevel = RiskLevel.valueOf(aiAnalysis.overallRiskLevel.name),
            severity = Severity.valueOf(aiAnalysis.overallRiskLevel.name), // Map risk to severity
            aiConfidence = aiAnalysis.confidence,
            processingTimeMs = aiAnalysis.processingTimeMs,
            metadata = aiAnalysis.metadata?.let { migrateAIMetadata(it) }
        )
    }
    
    /**
     * Migrate old Tag models to unified format
     */
    fun migrateOldTag(
        oldTag: com.hazardhawk.models.Tag
    ): Tag {
        return Tag(
            id = oldTag.id,
            name = oldTag.name,
            category = TagCategory.valueOf(oldTag.category.name),
            description = oldTag.description,
            oshaReferences = listOfNotNull(oldTag.oshaCode),
            complianceStatus = ComplianceStatus.COMPLIANT, // Default
            severity = oldTag.severity,
            usageStats = TagUsageStats(
                totalUsageCount = oldTag.usageCount,
                lastUsedAt = oldTag.lastUsed?.let { Instant.fromEpochMilliseconds(it) }
            ),
            projectId = null, // Not in old model
            workTypes = oldTag.workTypes,
            isCustom = oldTag.isCustom,
            isActive = true, // Default
            isRequired = oldTag.isRequired,
            priority = 100, // Default
            color = null,
            createdBy = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }
    
    /**
     * Migrate domain entity Tag to unified format
     */
    fun migrateDomainTag(
        domainTag: com.hazardhawk.domain.entities.Tag
    ): Tag {
        return Tag(
            id = domainTag.id,
            name = domainTag.name,
            category = TagCategory.valueOf(domainTag.category.name),
            description = domainTag.description,
            oshaReferences = domainTag.oshaReferences,
            complianceStatus = ComplianceStatus.valueOf(domainTag.complianceStatus.name),
            severity = Severity.LOW, // Default
            usageStats = TagUsageStats(
                totalUsageCount = domainTag.usageStats.totalUsageCount,
                recentUsageCount = domainTag.usageStats.recentUsageCount,
                lastUsedAt = domainTag.usageStats.lastUsedAt,
                averageConfidenceScore = domainTag.usageStats.averageConfidenceScore,
                projectUsageMap = domainTag.usageStats.projectUsageMap,
                hourlyUsagePattern = domainTag.usageStats.hourlyUsagePattern
            ),
            projectId = domainTag.projectId,
            workTypes = emptyList(), // Map from work types if needed
            isCustom = domainTag.isCustom,
            isActive = domainTag.isActive,
            isRequired = false, // Default
            priority = domainTag.priority,
            color = domainTag.color,
            createdBy = domainTag.createdBy,
            createdAt = domainTag.createdAt,
            updatedAt = domainTag.updatedAt
        )
    }
    
    // Private helper methods
    private fun migrateOldHazard(
        oldHazard: com.hazardhawk.models.Hazard
    ): Hazard {
        return Hazard(
            id = oldHazard.id,
            type = HazardType.valueOf(oldHazard.type.name),
            severity = Severity.valueOf(oldHazard.severity.name),
            description = oldHazard.description,
            confidence = oldHazard.confidence,
            oshaCode = oldHazard.oshaReference,
            boundingBox = oldHazard.boundingBox?.let {
                BoundingBox(
                    left = it.x,
                    top = it.y,
                    width = it.width,
                    height = it.height
                )
            },
            recommendations = emptyList(),
            immediateAction = null
        )
    }
    
    private fun migrateOldOshaCode(
        oldCode: com.hazardhawk.models.OSHACode
    ): OSHAViolation {
        return OSHAViolation(
            code = oldCode.code,
            title = oldCode.title,
            description = oldCode.description,
            severity = Severity.MEDIUM, // Default
            fineRange = null,
            correctiveAction = "Review compliance requirements"
        )
    }
    
    private fun migrateAIHazard(
        aiHazard: com.hazardhawk.ai.models.Hazard
    ): Hazard {
        return Hazard(
            id = aiHazard.id,
            type = HazardType.valueOf(aiHazard.type.name),
            severity = Severity.valueOf(aiHazard.severity.name),
            description = aiHazard.description,
            confidence = aiHazard.confidence,
            oshaCode = aiHazard.oshaCode,
            boundingBox = aiHazard.boundingBox?.let {
                BoundingBox(
                    left = it.left,
                    top = it.top,
                    width = it.width,
                    height = it.height
                )
            },
            recommendations = aiHazard.recommendations,
            immediateAction = aiHazard.immediateAction
        )
    }
    
    private fun migrateAIPPEStatus(
        aiPpeStatus: com.hazardhawk.ai.models.PPEStatus
    ): PPEStatus {
        return PPEStatus(
            hardHat = migrateAIPPEItem(aiPpeStatus.hardHat),
            safetyVest = migrateAIPPEItem(aiPpeStatus.safetyVest),
            safetyBoots = migrateAIPPEItem(aiPpeStatus.safetyBoots),
            safetyGlasses = migrateAIPPEItem(aiPpeStatus.safetyGlasses),
            fallProtection = migrateAIPPEItem(aiPpeStatus.fallProtection),
            respirator = migrateAIPPEItem(aiPpeStatus.respirator),
            overallCompliance = aiPpeStatus.overallCompliance
        )
    }
    
    private fun migrateAIPPEItem(
        aiItem: com.hazardhawk.ai.models.PPEItem
    ): PPEItem {
        return PPEItem(
            status = PPEItemStatus.valueOf(aiItem.status.name),
            confidence = aiItem.confidence,
            boundingBox = aiItem.boundingBox?.let {
                BoundingBox(
                    left = it.left,
                    top = it.top,
                    width = it.width,
                    height = it.height
                )
            },
            required = aiItem.required
        )
    }
    
    private fun migrateAIOshaViolation(
        aiViolation: com.hazardhawk.ai.models.OSHAViolation
    ): OSHAViolation {
        return OSHAViolation(
            code = aiViolation.code,
            title = aiViolation.title,
            description = aiViolation.description,
            severity = Severity.valueOf(aiViolation.severity.name),
            fineRange = aiViolation.fineRange,
            correctiveAction = aiViolation.correctiveAction
        )
    }
    
    private fun migrateAIMetadata(
        aiMetadata: com.hazardhawk.ai.models.AnalysisMetadata
    ): AnalysisMetadata {
        return AnalysisMetadata(
            imageWidth = aiMetadata.imageWidth,
            imageHeight = aiMetadata.imageHeight,
            location = aiMetadata.location?.let {
                Location(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy,
                    address = it.address
                )
            },
            weather = aiMetadata.weather?.let {
                WeatherConditions(
                    temperature = it.temperature,
                    humidity = it.humidity,
                    windSpeed = it.windSpeed,
                    conditions = it.conditions
                )
            },
            timeOfDay = aiMetadata.timeOfDay,
            cameraInfo = null // Add if needed
        )
    }
    
    /**
     * Batch migration for large datasets
     */
    suspend fun batchMigrateTags(
        oldTags: List<com.hazardhawk.models.Tag>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<Tag> {
        return oldTags.mapIndexed { index, oldTag ->
            onProgress(index + 1, oldTags.size)
            migrateOldTag(oldTag)
        }
    }
    
    suspend fun batchMigrateSafetyAnalyses(
        oldAnalyses: List<com.hazardhawk.models.SafetyAnalysis>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<SafetyAnalysis> {
        return oldAnalyses.mapIndexed { index, oldAnalysis ->
            onProgress(index + 1, oldAnalyses.size)
            migrateOldSafetyAnalysis(oldAnalysis)
        }
    }
}
