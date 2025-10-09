package com.hazardhawk.domain.models.ptp

import kotlinx.serialization.Serializable

/**
 * AI learning feedback for improving suggestion accuracy
 */
@Serializable
data class AILearningFeedback(
    val id: String,
    val documentType: DocumentType,
    val workType: String?,
    val aiSuggestion: String,
    val userModification: String? = null,
    val feedbackType: FeedbackType,
    val context: String? = null,
    val timestamp: Long,
    val userId: String? = null
)

@Serializable
enum class DocumentType {
    PTP,
    JHA,
    TOOLBOX_TALK,
    INCIDENT_REPORT
}

@Serializable
enum class FeedbackType {
    ACCEPT, // User accepted AI suggestion as-is
    EDIT,   // User modified AI suggestion
    REJECT, // User rejected AI suggestion
    ADD     // User added something AI didn't suggest
}

/**
 * Statistics for AI learning
 */
@Serializable
data class AILearningStats(
    val totalFeedback: Int,
    val acceptanceRate: Double, // Percentage of ACCEPT feedback
    val editRate: Double,       // Percentage of EDIT feedback
    val rejectionRate: Double,  // Percentage of REJECT feedback
    val byDocumentType: Map<DocumentType, DocumentTypeStats>,
    val byWorkType: Map<String, WorkTypeStats>
)

@Serializable
data class DocumentTypeStats(
    val documentType: DocumentType,
    val totalCount: Int,
    val acceptCount: Int,
    val editCount: Int,
    val rejectCount: Int,
    val addCount: Int
)

@Serializable
data class WorkTypeStats(
    val workType: String,
    val totalCount: Int,
    val acceptCount: Int,
    val editCount: Int,
    val rejectCount: Int,
    val addCount: Int
)
