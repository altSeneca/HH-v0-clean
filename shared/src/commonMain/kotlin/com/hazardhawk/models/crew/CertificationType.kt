package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable

@Serializable
data class CertificationType(
    val id: String,
    val code: String, // OSHA_10, OSHA_30, FORKLIFT, etc.
    val name: String,
    val category: String,
    val region: String,
    val typicalDurationMonths: Int? = null,
    val renewalRequired: Boolean = true,
    val description: String? = null,
    // Additional fields for compatibility
    val validityPeriodMonths: Int? = typicalDurationMonths,
    val required: Boolean = false,
    val oshaRequired: Boolean = false
)
