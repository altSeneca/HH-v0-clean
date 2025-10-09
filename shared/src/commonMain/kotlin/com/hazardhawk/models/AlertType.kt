package com.hazardhawk.models

import kotlinx.serialization.Serializable

/**
 * Shared AlertType enum for all monitoring and alert systems
 * Consolidated from multiple files to prevent duplicate declarations
 */
@Serializable
enum class AlertType {
    PERFORMANCE_DEGRADATION,
    HIGH_ERROR_RATE,
    SYSTEM_OVERLOAD,
    RESOURCE_EXHAUSTION,
    CONNECTIVITY_ISSUE,
    COMPONENT_FAILURE,
    INCIDENT_ESCALATION
}