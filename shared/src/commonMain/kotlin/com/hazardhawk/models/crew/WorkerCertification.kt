package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayAt

@Serializable
data class WorkerCertification(
    val id: String,
    val workerProfileId: String,
    val companyId: String? = null,
    val certificationTypeId: String,
    val certificationNumber: String? = null,
    val issueDate: LocalDate,
    val expirationDate: LocalDate? = null,
    val issuingAuthority: String? = null,
    val documentUrl: String,
    val thumbnailUrl: String? = null,
    val status: CertificationStatus,
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val rejectionReason: String? = null,
    val ocrConfidence: Double? = null,
    val createdAt: String,
    val updatedAt: String,

    // Embedded for convenience
    val certificationType: CertificationType? = null
) {
    val isValid: Boolean get() = status == CertificationStatus.VERIFIED && !isExpired
    val isExpired: Boolean get() = expirationDate?.let { it < Clock.System.todayAt(TimeZone.currentSystemDefault()) } ?: false
    val isExpiringSoon: Boolean get() {
        val today = Clock.System.todayAt(TimeZone.currentSystemDefault())
        return expirationDate?.let {
            val daysUntilExpiration = it.toEpochDays() - today.toEpochDays()
            daysUntilExpiration in 1..30
        } ?: false
    }
}
