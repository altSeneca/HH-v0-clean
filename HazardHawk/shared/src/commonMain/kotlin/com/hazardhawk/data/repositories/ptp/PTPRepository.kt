package com.hazardhawk.data.repositories.ptp

import com.hazardhawk.domain.models.ptp.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Repository for Pre-Task Plan data operations
 */
interface PTPRepository {
    // AI Generation
    suspend fun generatePtpWithAI(request: PtpAIRequest): Result<PtpAIResponse>

    // PTP CRUD operations
    suspend fun createPtp(ptp: PreTaskPlan): Result<String>
    suspend fun getPtpById(id: String): Result<PreTaskPlan?>
    suspend fun getPtpsByProject(projectId: String): Result<List<PreTaskPlan>>
    suspend fun getPtpsByStatus(status: PtpStatus): Result<List<PreTaskPlan>>
    suspend fun updatePtpStatus(id: String, status: PtpStatus): Result<Unit>
    suspend fun updatePtpContent(id: String, content: PtpContent): Result<Unit>
    suspend fun updatePtpSignature(id: String, signature: SignatureData): Result<Unit>
    suspend fun updatePtpPdfPaths(id: String, pdfPath: String?, cloudUrl: String?): Result<Unit>
    suspend fun deletePtp(id: String): Result<Unit>

    // PTP Photo operations
    suspend fun addPhotoToPtp(ptpId: String, photoId: String, order: Int, caption: String?): Result<Unit>
    suspend fun removePhotoFromPtp(ptpId: String, photoId: String): Result<Unit>
    suspend fun updatePhotoOrder(ptpId: String, photoId: String, newOrder: Int): Result<Unit>
    suspend fun getPhotosForPtp(ptpId: String): Result<List<PtpPhoto>>

    // Hazard Correction operations
    suspend fun createHazardCorrection(correction: HazardCorrection): Result<String>
    suspend fun getHazardCorrectionById(id: String): Result<HazardCorrection?>
    suspend fun getHazardCorrectionsForPhoto(photoId: String): Result<List<HazardCorrection>>
    suspend fun getHazardCorrectionsByStatus(status: CorrectionStatus): Result<List<HazardCorrectionWithPhotos>>
    suspend fun getOutstandingHazards(): Result<List<HazardCorrectionWithPhotos>>
    suspend fun updateHazardCorrectionStatus(id: String, status: CorrectionStatus): Result<Unit>
    suspend fun linkCorrectionPhoto(id: String, correctionPhotoId: String): Result<Unit>
    suspend fun verifyHazardCorrection(id: String, verifiedBy: String, notes: String): Result<Unit>
    suspend fun getHazardStats(): Result<HazardCorrectionStats>

    // AI Learning Feedback operations
    suspend fun recordAIFeedback(feedback: AILearningFeedback): Result<String>
    suspend fun getFeedbackByDocumentType(documentType: DocumentType, limit: Int): Result<List<AILearningFeedback>>
    suspend fun getFeedbackStats(): Result<AILearningStats>

    // Token usage operations
    suspend fun storeTokenUsage(ptpId: String, usage: TokenUsageMetadata, successful: Boolean): Result<Unit>
    suspend fun getTokenUsageForPtp(ptpId: String): Result<List<TokenUsageRecord>>
    suspend fun getDailyTokenUsage(startTimestamp: Long, endTimestamp: Long): Result<TokenUsageSummary>
    suspend fun getMonthlyTokenUsage(startTimestamp: Long, endTimestamp: Long): Result<TokenUsageSummary>
    suspend fun getTotalTokenUsage(): Result<TokenUsageSummary>

    // Reactive data flows
    fun observePtpsByProject(projectId: String): Flow<List<PreTaskPlan>>
    fun observeOutstandingHazards(): Flow<List<HazardCorrectionWithPhotos>>
}

/**
 * SQLDelight implementation of PTPRepository
 */
class SQLDelightPTPRepository(
    private val database: com.hazardhawk.database.HazardHawkDatabase,
    private val ptpAIService: com.hazardhawk.domain.services.ptp.PTPAIService,
    private val json: Json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }
) : PTPRepository {

    override suspend fun generatePtpWithAI(request: PtpAIRequest): Result<PtpAIResponse> {
        val response = ptpAIService.generatePtp(request)

        // Store token usage if available and request has project ID
        response.getOrNull()?.let { aiResponse ->
            aiResponse.tokenUsage?.let { usage ->
                // Store token usage record
                storeTokenUsage(
                    ptpId = request.questionnaire.projectName, // Use project name as temporary ID
                    usage = usage,
                    successful = aiResponse.success
                )
            }
        }

        return response
    }

    override suspend fun createPtp(ptp: PreTaskPlan): Result<String> {
        return try {
            println("PTPRepository: Creating PTP with ID: ${ptp.id}")
            println("PTPRepository: PTP has ${ptp.aiGeneratedContent?.hazards?.size ?: 0} hazards")

            database.preTaskPlansQueries.insertPreTaskPlan(
                id = ptp.id,
                project_id = ptp.projectId,
                created_by = ptp.createdBy,
                created_at = ptp.createdAt,
                updated_at = ptp.updatedAt,
                work_type = ptp.workType,
                work_scope = ptp.workScope,
                crew_size = ptp.crewSize?.toLong(),
                status = ptp.status.name.lowercase(),
                ai_generated_content = ptp.aiGeneratedContent?.let { json.encodeToString(it) },
                user_modified_content = ptp.userModifiedContent?.let { json.encodeToString(it) },
                pdf_path = ptp.pdfPath,
                cloud_storage_url = ptp.cloudStorageUrl,
                signature_supervisor_blob = ptp.signatureSupervisor?.signatureBlob,
                signature_supervisor_name = ptp.signatureSupervisor?.supervisorName,
                signature_date = ptp.signatureSupervisor?.signatureDate,
                tools_equipment = json.encodeToString(ptp.toolsEquipment),
                mechanical_equipment = null, // Not in PreTaskPlan model
                environmental_conditions = null, // Not in PreTaskPlan model
                materials_involved = null, // Not in PreTaskPlan model
                specific_tasks = null, // Not in PreTaskPlan model
                emergency_contacts = json.encodeToString(ptp.emergencyContacts),
                nearest_hospital = ptp.nearestHospital,
                evacuation_routes = ptp.evacuationRoutes
            )

            println("PTPRepository: PTP '${ptp.id}' saved successfully")
            Result.success(ptp.id)
        } catch (e: Exception) {
            println("PTPRepository: Error creating PTP: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getPtpById(id: String): Result<PreTaskPlan?> {
        return try {
            val result = database.preTaskPlansQueries.selectPreTaskPlanById(id).executeAsOneOrNull()
            val ptp = result?.let { mapToDomain(it) }

            println("PTPRepository: ${if (ptp != null) "Found" else "Not found"} PTP with ID: $id")
            Result.success(ptp)
        } catch (e: Exception) {
            println("PTPRepository: Error getting PTP by ID: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getPtpsByProject(projectId: String): Result<List<PreTaskPlan>> {
        return try {
            val results = database.preTaskPlansQueries
                .selectPreTaskPlansByProject(projectId)
                .executeAsList()
                .map { mapToDomain(it) }

            println("PTPRepository: Found ${results.size} PTPs for project: $projectId")
            Result.success(results)
        } catch (e: Exception) {
            println("PTPRepository: Error getting PTPs by project: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getPtpsByStatus(status: PtpStatus): Result<List<PreTaskPlan>> {
        return try {
            val results = database.preTaskPlansQueries
                .selectPreTaskPlansByStatus(status.name.lowercase())
                .executeAsList()
                .map { mapToDomain(it) }

            println("PTPRepository: Found ${results.size} PTPs with status: $status")
            Result.success(results)
        } catch (e: Exception) {
            println("PTPRepository: Error getting PTPs by status: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun mapToDomain(dbPtp: com.hazardhawk.database.Pre_task_plans): PreTaskPlan {
        return PreTaskPlan(
            id = dbPtp.id,
            projectId = dbPtp.project_id,
            createdBy = dbPtp.created_by,
            createdAt = dbPtp.created_at,
            updatedAt = dbPtp.updated_at,
            workType = dbPtp.work_type,
            workScope = dbPtp.work_scope,
            crewSize = dbPtp.crew_size?.toInt(),
            status = PtpStatus.valueOf(dbPtp.status?.uppercase() ?: "DRAFT"),
            aiGeneratedContent = dbPtp.ai_generated_content?.let {
                json.decodeFromString<PtpContent>(it)
            },
            userModifiedContent = dbPtp.user_modified_content?.let {
                json.decodeFromString<PtpContent>(it)
            },
            toolsEquipment = dbPtp.tools_equipment?.let {
                json.decodeFromString<List<String>>(it)
            } ?: emptyList(),
            emergencyContacts = dbPtp.emergency_contacts?.let {
                json.decodeFromString<List<EmergencyContact>>(it)
            } ?: emptyList(),
            nearestHospital = dbPtp.nearest_hospital,
            evacuationRoutes = dbPtp.evacuation_routes,
            pdfPath = dbPtp.pdf_path,
            cloudStorageUrl = dbPtp.cloud_storage_url,
            signatureSupervisor = if (dbPtp.signature_supervisor_blob != null) {
                SignatureData(
                    signatureBlob = dbPtp.signature_supervisor_blob,
                    supervisorName = dbPtp.signature_supervisor_name ?: "",
                    signatureDate = dbPtp.signature_date ?: 0L
                )
            } else null
        )
    }

    override suspend fun updatePtpStatus(id: String, status: PtpStatus): Result<Unit> {
        return try {
            // database.preTaskPlansQueries.updatePreTaskPlanStatus(
            //     status = status.name.lowercase(),
            //     updated_at = Clock.System.now().toEpochMilliseconds(),
            //     id = id
            // )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePtpContent(id: String, content: PtpContent): Result<Unit> {
        return try {
            // database.preTaskPlansQueries.updatePreTaskPlanContent(
            //     user_modified_content = json.encodeToString(content),
            //     updated_at = Clock.System.now().toEpochMilliseconds(),
            //     id = id
            // )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePtpSignature(id: String, signature: SignatureData): Result<Unit> {
        return try {
            // database.preTaskPlansQueries.updatePreTaskPlanSignature(
            //     signature_supervisor_blob = signature.signatureBlob,
            //     signature_supervisor_name = signature.supervisorName,
            //     signature_date = signature.signatureDate,
            //     updated_at = Clock.System.now().toEpochMilliseconds(),
            //     id = id
            // )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePtpPdfPaths(id: String, pdfPath: String?, cloudUrl: String?): Result<Unit> {
        return try {
            // database.preTaskPlansQueries.updatePreTaskPlanPdfPaths(
            //     pdf_path = pdfPath,
            //     cloud_storage_url = cloudUrl,
            //     updated_at = Clock.System.now().toEpochMilliseconds(),
            //     id = id
            // )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePtp(id: String): Result<Unit> {
        return try {
            // database.preTaskPlansQueries.deletePreTaskPlan(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addPhotoToPtp(ptpId: String, photoId: String, order: Int, caption: String?): Result<Unit> {
        return try {
            // database.preTaskPlansQueries.insertPtpPhoto(
            //     ptp_id = ptpId,
            //     photo_id = photoId,
            //     display_order = order.toLong(),
            //     photo_caption = caption,
            //     added_at = Clock.System.now().toEpochMilliseconds()
            // )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removePhotoFromPtp(ptpId: String, photoId: String): Result<Unit> {
        return try {
            // database.preTaskPlansQueries.deletePtpPhoto(ptp_id = ptpId, photo_id = photoId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePhotoOrder(ptpId: String, photoId: String, newOrder: Int): Result<Unit> {
        return try {
            // database.preTaskPlansQueries.updatePtpPhotoOrder(
            //     display_order = newOrder.toLong(),
            //     ptp_id = ptpId,
            //     photo_id = photoId
            // )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPhotosForPtp(ptpId: String): Result<List<PtpPhoto>> {
        return try {
            // val results = database.preTaskPlansQueries.selectPhotosByPtpId(ptpId).executeAsList()
            Result.success(emptyList()) // Placeholder
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createHazardCorrection(correction: HazardCorrection): Result<String> {
        return try {
            // database.preTaskPlansQueries.insertHazardCorrection(...)
            Result.success(correction.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHazardCorrectionById(id: String): Result<HazardCorrection?> {
        return try {
            // val result = database.preTaskPlansQueries.selectHazardCorrectionById(id).executeAsOneOrNull()
            Result.success(null) // Placeholder
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHazardCorrectionsForPhoto(photoId: String): Result<List<HazardCorrection>> {
        return try {
            // val results = database.preTaskPlansQueries.selectHazardCorrectionsByOriginalPhoto(photoId).executeAsList()
            Result.success(emptyList()) // Placeholder
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHazardCorrectionsByStatus(status: CorrectionStatus): Result<List<HazardCorrectionWithPhotos>> {
        return try {
            // val results = database.preTaskPlansQueries.selectHazardCorrectionsByStatus(status.name.lowercase()).executeAsList()
            Result.success(emptyList()) // Placeholder
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOutstandingHazards(): Result<List<HazardCorrectionWithPhotos>> {
        return try {
            // val results = database.preTaskPlansQueries.selectOutstandingHazards().executeAsList()
            Result.success(emptyList()) // Placeholder
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateHazardCorrectionStatus(id: String, status: CorrectionStatus): Result<Unit> {
        return try {
            // database.preTaskPlansQueries.updateHazardCorrectionStatus(...)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun linkCorrectionPhoto(id: String, correctionPhotoId: String): Result<Unit> {
        return try {
            // database.preTaskPlansQueries.updateHazardCorrectionPhoto(...)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyHazardCorrection(id: String, verifiedBy: String, notes: String): Result<Unit> {
        return try {
            // database.preTaskPlansQueries.verifyHazardCorrection(...)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHazardStats(): Result<HazardCorrectionStats> {
        return try {
            // Query database and aggregate statistics
            Result.success(
                HazardCorrectionStats(
                    totalHazards = 0,
                    outstandingCount = 0,
                    inProgressCount = 0,
                    mitigatedCount = 0,
                    verifiedCount = 0,
                    byOshaCode = emptyMap()
                )
            ) // Placeholder
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordAIFeedback(feedback: AILearningFeedback): Result<String> {
        return try {
            // database.preTaskPlansQueries.insertAiLearningFeedback(...)
            Result.success(feedback.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFeedbackByDocumentType(documentType: DocumentType, limit: Int): Result<List<AILearningFeedback>> {
        return try {
            // val results = database.preTaskPlansQueries.selectFeedbackByDocumentType(documentType.name.lowercase(), limit.toLong()).executeAsList()
            Result.success(emptyList()) // Placeholder
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFeedbackStats(): Result<AILearningStats> {
        return try {
            // Query and aggregate feedback statistics
            Result.success(
                AILearningStats(
                    totalFeedback = 0,
                    acceptanceRate = 0.0,
                    editRate = 0.0,
                    rejectionRate = 0.0,
                    byDocumentType = emptyMap(),
                    byWorkType = emptyMap()
                )
            ) // Placeholder
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun storeTokenUsage(ptpId: String, usage: TokenUsageMetadata, successful: Boolean): Result<Unit> {
        return try {
            val id = generateUUID()
            database.tokenUsageQueries.insertTokenUsage(
                id = id,
                ptp_id = ptpId,
                prompt_tokens = usage.promptTokenCount.toLong(),
                completion_tokens = usage.candidatesTokenCount.toLong(),
                total_tokens = usage.totalTokenCount.toLong(),
                estimated_cost = usage.estimatedCost,
                model_name = usage.modelVersion,
                timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                successful = if (successful) 1L else 0L
            )
            println("PTPRepository: Stored token usage for PTP $ptpId - ${usage.totalTokenCount} tokens, cost: $${String.format("%.4f", usage.estimatedCost)}")
            Result.success(Unit)
        } catch (e: Exception) {
            println("PTPRepository: Error storing token usage: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getTokenUsageForPtp(ptpId: String): Result<List<TokenUsageRecord>> {
        return try {
            val results = database.tokenUsageQueries.selectByPtpId(ptpId)
                .executeAsList()
                .map { dbRecord ->
                    TokenUsageRecord(
                        id = dbRecord.id,
                        ptpId = dbRecord.ptp_id,
                        promptTokens = dbRecord.prompt_tokens.toInt(),
                        completionTokens = dbRecord.completion_tokens.toInt(),
                        totalTokens = dbRecord.total_tokens.toInt(),
                        estimatedCost = dbRecord.estimated_cost,
                        modelName = dbRecord.model_name,
                        timestamp = dbRecord.timestamp,
                        successful = dbRecord.successful == 1L
                    )
                }
            Result.success(results)
        } catch (e: Exception) {
            println("PTPRepository: Error getting token usage: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getDailyTokenUsage(startTimestamp: Long, endTimestamp: Long): Result<TokenUsageSummary> {
        return try {
            val result = database.tokenUsageQueries.selectDailyUsage(startTimestamp, endTimestamp)
                .executeAsOneOrNull()

            val summary = TokenUsageSummary(
                totalTokens = result?.total_tokens ?: 0L,
                totalCost = result?.total_cost ?: 0.0,
                requestCount = result?.request_count?.toInt() ?: 0
            )
            Result.success(summary)
        } catch (e: Exception) {
            println("PTPRepository: Error getting daily token usage: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getMonthlyTokenUsage(startTimestamp: Long, endTimestamp: Long): Result<TokenUsageSummary> {
        return try {
            val result = database.tokenUsageQueries.selectMonthlyUsage(startTimestamp, endTimestamp)
                .executeAsOneOrNull()

            val summary = TokenUsageSummary(
                totalTokens = result?.total_tokens ?: 0L,
                totalCost = result?.total_cost ?: 0.0,
                requestCount = result?.request_count?.toInt() ?: 0
            )
            Result.success(summary)
        } catch (e: Exception) {
            println("PTPRepository: Error getting monthly token usage: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getTotalTokenUsage(): Result<TokenUsageSummary> {
        return try {
            val result = database.tokenUsageQueries.selectTotalUsage()
                .executeAsOneOrNull()

            val summary = TokenUsageSummary(
                totalTokens = result?.total_tokens ?: 0L,
                totalCost = result?.total_cost ?: 0.0,
                requestCount = result?.request_count?.toInt() ?: 0
            )
            Result.success(summary)
        } catch (e: Exception) {
            println("PTPRepository: Error getting total token usage: ${e.message}")
            Result.failure(e)
        }
    }

    override fun observePtpsByProject(projectId: String): Flow<List<PreTaskPlan>> {
        TODO("Implement Flow using SQLDelight asFlow()")
    }

    override fun observeOutstandingHazards(): Flow<List<HazardCorrectionWithPhotos>> {
        TODO("Implement Flow using SQLDelight asFlow()")
    }

    /**
     * Generate a UUID for new records
     */
    private fun generateUUID(): String {
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString() + (0..999).random()
    }
}

/**
 * Helper functions for mapping between database and domain models
 */
object PTPMapper {
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    fun encodeStringList(list: List<String>): String {
        return json.encodeToString(list)
    }

    fun decodeStringList(jsonString: String?): List<String> {
        return if (jsonString.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun encodeEmergencyContacts(contacts: List<EmergencyContact>): String {
        return json.encodeToString(contacts)
    }

    fun decodeEmergencyContacts(jsonString: String?): List<EmergencyContact> {
        return if (jsonString.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun encodePtpContent(content: PtpContent?): String? {
        return content?.let { json.encodeToString(it) }
    }

    fun decodePtpContent(jsonString: String?): PtpContent? {
        return if (jsonString.isNullOrBlank()) {
            null
        } else {
            try {
                json.decodeFromString(jsonString)
            } catch (e: Exception) {
                null
            }
        }
    }
}
