package com.hazardhawk.data.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import com.hazardhawk.core.models.ComplianceStatus
import com.hazardhawk.core.models.TagSource
import com.hazardhawk.core.models.PhotoTag
import com.hazardhawk.core.models.GpsLocation
import com.hazardhawk.core.models.DigitalSignature

/**
 * SQLDelight query result mapping class for photo-tag associations
 * Maps database query results to PhotoTag domain objects
 */
@Serializable
data class SelectPhotoTags(
    val photo_id: String,
    val tag_id: String,
    val applied_at: Long,
    val applied_by: String,
    val source: String,
    val confidence_score: Double?,
    val review_status: String,
    val reviewed_by: String?,
    val reviewed_at: Long?,
    val notes: String?,
    val severity_override: String?,
    val region_data: String?,
    val tag_name: String,
    val tag_category: String,
    val tag_description: String?,
    val osha_references: String?,
    val tag_compliance_status: String,
    val created_at: Long,
    val updated_at: Long
) {
    /**
     * Converts the database query result to a PhotoTag domain object
     */
    fun toPhotoTag(): PhotoTag {
        return PhotoTag(
            photoId = photo_id,
            tagId = tag_id,
            appliedAt = Instant.fromEpochMilliseconds(applied_at),
            appliedBy = applied_by,
            gpsLocation = null, // GPS location would need separate query or join
            digitalSignature = null, // Digital signature would need separate query or join
            complianceStatus = ComplianceStatus.fromString(tag_compliance_status) ?: ComplianceStatus.UNDER_REVIEW,
            auditTrailId = "audit_${photo_id}_${tag_id}",
            verifiedAt = reviewed_at?.let { Instant.fromEpochMilliseconds(it) },
            verifiedBy = reviewed_by,
            notes = notes,
            confidence = confidence_score,
            source = fromTagSourceString(source) ?: TagSource.MANUAL,
            metadata = mapOf(
                "tag_name" to tag_name,
                "tag_category" to tag_category,
                "review_status" to review_status,
                "severity_override" to (severity_override ?: ""),
                "region_data" to (region_data ?: "")
            )
        )
    }
    
    companion object {
        /**
         * Helper method to convert a list of query results to PhotoTag objects
         */
        fun List<SelectPhotoTags>.toPhotoTags(): List<PhotoTag> {
            return map { it.toPhotoTag() }
        }
        
        /**
         * Creates a SelectPhotoTags instance from database row values
         * Used by SQLDelight for result mapping
         */
        fun fromDatabaseRow(
            photo_id: String,
            tag_id: String,
            applied_at: Long,
            applied_by: String,
            source: String,
            confidence_score: Double?,
            review_status: String,
            reviewed_by: String?,
            reviewed_at: Long?,
            notes: String?,
            severity_override: String?,
            region_data: String?,
            tag_name: String,
            tag_category: String,
            tag_description: String?,
            osha_references: String?,
            tag_compliance_status: String,
            created_at: Long,
            updated_at: Long
        ): SelectPhotoTags {
            return SelectPhotoTags(
                photo_id = photo_id,
                tag_id = tag_id,
                applied_at = applied_at,
                applied_by = applied_by,
                source = source,
                confidence_score = confidence_score,
                review_status = review_status,
                reviewed_by = reviewed_by,
                reviewed_at = reviewed_at,
                notes = notes,
                severity_override = severity_override,
                region_data = region_data,
                tag_name = tag_name,
                tag_category = tag_category,
                tag_description = tag_description,
                osha_references = osha_references,
                tag_compliance_status = tag_compliance_status,
                created_at = created_at,
                updated_at = updated_at
            )
        }
    }
}

/**
 * Extension functions for enum serialization
 */
private fun fromTagSourceString(value: String?): TagSource? {
    return value?.let { str ->
        TagSource.values().find { it.name.equals(str, ignoreCase = true) }
    }
}