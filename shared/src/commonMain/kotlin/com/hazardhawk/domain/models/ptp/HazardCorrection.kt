package com.hazardhawk.domain.models.ptp

import kotlinx.serialization.Serializable

/**
 * Hazard correction tracking - before/after photo pairs
 */
@Serializable
data class HazardCorrection(
    val id: String,
    val originalPhotoId: String,
    val correctionPhotoId: String? = null,
    val hazardOshaCode: String,
    val hazardDescription: String,
    val dateIdentified: Long,
    val dateCorrected: Long? = null,
    val verifiedBy: String? = null,
    val verificationNotes: String? = null,
    val status: CorrectionStatus = CorrectionStatus.OUTSTANDING,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
enum class CorrectionStatus {
    OUTSTANDING,
    IN_PROGRESS,
    MITIGATED,
    VERIFIED
}

/**
 * Hazard correction with photo details
 */
@Serializable
data class HazardCorrectionWithPhotos(
    val correction: HazardCorrection,
    val originalPhotoPath: String,
    val correctionPhotoPath: String? = null,
    val originalPhotoTimestamp: Long,
    val correctionPhotoTimestamp: Long? = null
)

/**
 * Statistics for hazard corrections
 */
@Serializable
data class HazardCorrectionStats(
    val totalHazards: Int,
    val outstandingCount: Int,
    val inProgressCount: Int,
    val mitigatedCount: Int,
    val verifiedCount: Int,
    val byOshaCode: Map<String, OshaCodeStats>
)

@Serializable
data class OshaCodeStats(
    val oshaCode: String,
    val totalCount: Int,
    val mitigatedCount: Int,
    val outstandingCount: Int
)
