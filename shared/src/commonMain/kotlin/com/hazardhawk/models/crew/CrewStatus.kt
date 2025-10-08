package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable

@Serializable
enum class CrewStatus {
    ACTIVE,
    INACTIVE,
    DISBANDED
}
