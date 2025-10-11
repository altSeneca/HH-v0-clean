package com.hazardhawk.core.models

import kotlinx.serialization.Serializable

/**
 * Report template model for OSHA-compliant safety documentation
 */
@Serializable
data class ReportTemplate(
    val id: String,
    val name: String,
    val type: ReportType,
    val description: String,
    val minimumPhotos: Int = 0,
    val oshaCompliant: Boolean = true,
    val oshaStandards: List<String> = emptyList(),
    val requiredSignatures: List<String> = emptyList(),
    val sections: List<ReportSection> = emptyList(),
    val requiredFields: List<String> = emptyList()
)

/**
 * Report type enumeration
 */
@Serializable
enum class ReportType {
    DAILY_INSPECTION,
    INCIDENT_REPORT,
    PRE_TASK_PLAN,
    HAZARD_IDENTIFICATION,
    TOOLBOX_TALK,
    WEEKLY_SAFETY_REVIEW,
    MONTHLY_SAFETY_AUDIT,
    CUSTOM
}

/**
 * Report section model
 */
@Serializable
data class ReportSection(
    val id: String,
    val title: String,
    val required: Boolean = false,
    val order: Int,
    val description: String? = null,
    val fields: List<ReportField> = emptyList()
)

/**
 * Report field model
 */
@Serializable
data class ReportField(
    val id: String,
    val name: String,
    val type: FieldType,
    val required: Boolean = false,
    val placeholder: String? = null,
    val options: List<String>? = null
)

/**
 * Field type enumeration
 */
@Serializable
enum class FieldType {
    TEXT,
    TEXTAREA,
    NUMBER,
    DATE,
    TIME,
    DATETIME,
    SELECT,
    MULTISELECT,
    CHECKBOX,
    SIGNATURE,
    PHOTO,
    LOCATION
}
