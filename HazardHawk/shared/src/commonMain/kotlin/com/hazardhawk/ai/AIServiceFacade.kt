package com.hazardhawk.ai

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.models.TagCategory
import com.hazardhawk.ai.yolo.ConstructionHazardDetection

/**
 * Simplified AI Service Facade for build infrastructure
 * Minimal implementation to satisfy compilation requirements
 */
interface AIServiceFacade {
    suspend fun analyzePhotoWithTags(
        data: ByteArray, 
        width: Int, 
        height: Int,
        workType: WorkType = WorkType.GENERAL_CONSTRUCTION
    ): PhotoAnalysisWithTags
    
    suspend fun initialize(): Result<Unit>
    suspend fun release()
    val isServiceAvailable: Boolean
}

/**
 * Minimal data classes for compilation
 */
@Serializable
data class PhotoAnalysisWithTags(
    val id: String,
    val photoId: String,
    val recommendedTags: List<String> = emptyList(),
    val processingTimeMs: Long = 0L,
    val hazardDetections: List<ConstructionHazardDetection> = emptyList()
)

@Serializable
data class UITagRecommendation(
    val tagId: String,
    val displayName: String,
    val confidence: Float,
    val reason: String,
    val priority: TagPriority = TagPriority.MEDIUM,
    val oshaReference: String? = null
) {
    companion object {
        fun basic(tagId: String) = UITagRecommendation(
            tagId = tagId,
            displayName = tagId.replace("-", " ").replaceFirstChar { it.uppercase() },
            confidence = 0.5f,
            reason = "Basic safety recommendation"
        )
    }
}

@Serializable
enum class TagPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Minimal implementation for compilation
 */
class StubAIServiceFacade : AIServiceFacade {
    override suspend fun analyzePhotoWithTags(
        data: ByteArray,
        width: Int,
        height: Int,
        workType: WorkType
    ): PhotoAnalysisWithTags {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return PhotoAnalysisWithTags(
            id = "stub-analysis-$timestamp",
            photoId = "stub-photo-$timestamp",
            recommendedTags = listOf("general-safety-check"),
            processingTimeMs = 100L
        )
    }
    
    override suspend fun initialize(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun release() {
        // No-op for stub
    }
    
    override val isServiceAvailable: Boolean = true
}
