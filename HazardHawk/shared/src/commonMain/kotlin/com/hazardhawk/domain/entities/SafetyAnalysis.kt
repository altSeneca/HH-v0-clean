package com.hazardhawk.domain.entities

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class SafetyAnalysis(
    val id: String,
    val photoId: String,
    val severity: String,
    val aiConfidence: Float,
    val analyzedAt: LocalDateTime,
    val analysisSource: String,
    val hazards: List<String> = emptyList(),
    val oshaCodes: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)