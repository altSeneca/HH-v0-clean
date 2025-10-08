package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable

@Serializable
enum class CertificationStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    EXPIRED,
    REJECTED
}
