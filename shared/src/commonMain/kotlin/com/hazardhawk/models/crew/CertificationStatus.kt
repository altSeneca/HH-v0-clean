package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable

@Serializable
enum class CertificationStatus {
    PENDING,
    PENDING_VERIFICATION,
    VERIFIED,
    REJECTED,
    EXPIRED
}
