package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable

@Serializable
enum class WorkerStatus {
    ACTIVE,
    INACTIVE,
    TERMINATED;

    val isActive: Boolean get() = this == ACTIVE
}
