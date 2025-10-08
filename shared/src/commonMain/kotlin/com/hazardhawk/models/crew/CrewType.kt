package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable

@Serializable
enum class CrewType {
    PERMANENT,
    PROJECT_BASED,
    TRADE_SPECIFIC
}
