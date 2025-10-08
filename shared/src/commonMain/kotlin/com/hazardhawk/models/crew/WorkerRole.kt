package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable

@Serializable
enum class WorkerRole {
    LABORER,
    SKILLED_WORKER,
    OPERATOR,
    CREW_LEAD,
    FOREMAN,
    SUPERINTENDENT,
    PROJECT_MANAGER,
    SAFETY_MANAGER;

    val displayName: String get() = name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
}
