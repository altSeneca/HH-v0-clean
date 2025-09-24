package com.hazardhawk.domain.entities

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

/**
 * Photo entity model for the HazardHawk application
 * Consolidated model combining fields from both previous versions
 */
@Serializable
data class Photo(
    val id: String,
    val fileName: String,
    val filePath: String,
    val capturedAt: Instant,
    val timestamp: Long = capturedAt.toEpochMilliseconds(),
    val location: GpsCoordinates? = null,
    val tags: List<String> = emptyList(),
    val analysisId: String? = null,
    val workType: WorkType? = null,
    val projectId: String? = null,
    val userId: String? = null,
    val isAnalyzed: Boolean = false,
    val isUploaded: Boolean = false,
    val fileSize: Long = 0L,
    val metadata: String? = null,
    
    // Enhanced fields from domain.entities version
    val complianceStatus: ComplianceStatus = ComplianceStatus.Unknown,
    val syncStatus: SyncStatus = SyncStatus.Pending,
    val s3Url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val md5Hash: String? = null,
    val thumbnailPath: String? = null,
    val exifData: ExifData? = null,
    val createdAt: Long = timestamp,
    val updatedAt: Long = timestamp
) {
    /**
     * Check if photo has GPS coordinates
     */
    fun hasLocation(): Boolean = location != null
    
    /**
     * Get display name for the photo
     */
    fun getDisplayName(): String {
        return fileName.substringBeforeLast('.').takeIf { it.isNotBlank() } ?: "Untitled Photo"
    }
    
    /**
     * Get file extension
     */
    fun getFileExtension(): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }
    
    /**
     * Check if photo is an image file
     */
    fun isImageFile(): Boolean {
        return when (getFileExtension()) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> true
            else -> false
        }
    }
}

/**
 * GPS coordinates data class
 */
@Serializable
data class GpsCoordinates(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
) {
    /**
     * Format as display string
     */
    fun toDisplayString(): String {
        return "${latitude.format(6)}, ${longitude.format(6)}"
    }
    
    /**
     * Check if coordinates are valid
     */
    fun isValid(): Boolean {
        return latitude != 0.0 || longitude != 0.0
    }
}

/**
 * Hazard type enumeration
 */
@Serializable
enum class HazardType {
    FALL_PROTECTION,
    PPE_VIOLATION,
    ELECTRICAL,
    EQUIPMENT_SAFETY,
    CHEMICAL,
    CRANE_LIFT,
    CONFINED_SPACE,
    HOT_WORK,
    FIRE,
    ERGONOMIC,
    HOUSEKEEPING,
    ENVIRONMENTAL,
    STRUCTURAL,
    BIOLOGICAL,
    PSYCHOLOGICAL,
    OTHER,
    UNKNOWN
}

@Serializable
enum class HazardSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Work type enumeration
 */
@Serializable
enum class WorkType {
    GENERAL_CONSTRUCTION,
    ELECTRICAL,
    PLUMBING,
    HVAC,
    ROOFING,
    CONCRETE,
    STEEL_WORK,
    EXCAVATION,
    PAINTING,
    WELDING,
    CARPENTRY,
    DEMOLITION,
    INSPECTION,
    MAINTENANCE,
    OTHER,
    // Additional work types for OSHA compliance
    GENERAL_SAFETY,
    ELECTRICAL_SAFETY,
    FALL_PROTECTION,
    CRANE_LIFTING,
    CONFINED_SPACE,
    CHEMICAL_SAFETY,
    FIRE_SAFETY,
    EMERGENCY_PROCEDURES,
    EQUIPMENT_SAFETY,
    ERGONOMICS,
    HOUSEKEEPING,
    TRAINING_COMMUNICATION
}

@Serializable
data class WorkTypeInfo(
    val type: WorkType,
    val name: String,
    val description: String,
    val commonHazards: List<HazardType> = emptyList()
) {
    companion object {
        fun getWorkTypeInfo(type: WorkType): WorkTypeInfo = when (type) {
            WorkType.GENERAL_CONSTRUCTION -> WorkTypeInfo(
                type = type,
                name = "General Construction",
                description = "General construction and building activities",
                commonHazards = listOf(HazardType.FALL_PROTECTION, HazardType.PPE_VIOLATION)
            )
            WorkType.ELECTRICAL -> WorkTypeInfo(
                type = type,
                name = "Electrical Work",
                description = "Electrical installation and maintenance",
                commonHazards = listOf(HazardType.ELECTRICAL, HazardType.PPE_VIOLATION)
            )
            WorkType.PLUMBING -> WorkTypeInfo(
                type = type,
                name = "Plumbing",
                description = "Water and drainage systems installation",
                commonHazards = listOf(HazardType.EQUIPMENT_SAFETY, HazardType.CHEMICAL)
            )
            WorkType.HVAC -> WorkTypeInfo(
                type = type,
                name = "HVAC",
                description = "Heating, ventilation, and air conditioning",
                commonHazards = listOf(HazardType.EQUIPMENT_SAFETY, HazardType.ELECTRICAL)
            )
            WorkType.ROOFING -> WorkTypeInfo(
                type = type,
                name = "Roofing",
                description = "Roof installation and repair",
                commonHazards = listOf(HazardType.FALL_PROTECTION, HazardType.EQUIPMENT_SAFETY)
            )
            WorkType.CONCRETE -> WorkTypeInfo(
                type = type,
                name = "Concrete Work",
                description = "Concrete pouring and finishing",
                commonHazards = listOf(HazardType.EQUIPMENT_SAFETY, HazardType.CHEMICAL)
            )
            WorkType.STEEL_WORK -> WorkTypeInfo(
                type = type,
                name = "Steel Erection",
                description = "Structural steel installation",
                commonHazards = listOf(HazardType.FALL_PROTECTION, HazardType.CRANE_LIFT)
            )
            WorkType.EXCAVATION -> WorkTypeInfo(
                type = type,
                name = "Excavation",
                description = "Earth moving and digging operations",
                commonHazards = listOf(HazardType.EQUIPMENT_SAFETY, HazardType.CONFINED_SPACE)
            )
            WorkType.PAINTING -> WorkTypeInfo(
                type = type,
                name = "Painting",
                description = "Surface preparation and painting",
                commonHazards = listOf(HazardType.CHEMICAL, HazardType.PPE_VIOLATION)
            )
            WorkType.WELDING -> WorkTypeInfo(
                type = type,
                name = "Hot Work",
                description = "Welding, cutting, and other hot work activities",
                commonHazards = listOf(HazardType.HOT_WORK, HazardType.PPE_VIOLATION)
            )
            WorkType.CARPENTRY -> WorkTypeInfo(
                type = type,
                name = "Carpentry",
                description = "Wood construction and finishing",
                commonHazards = listOf(HazardType.EQUIPMENT_SAFETY, HazardType.PPE_VIOLATION)
            )
            WorkType.DEMOLITION -> WorkTypeInfo(
                type = type,
                name = "Demolition",
                description = "Structure removal and cleanup",
                commonHazards = listOf(HazardType.FALL_PROTECTION, HazardType.EQUIPMENT_SAFETY)
            )
            WorkType.INSPECTION -> WorkTypeInfo(
                type = type,
                name = "Inspection",
                description = "Safety and quality inspections",
                commonHazards = listOf(HazardType.FALL_PROTECTION, HazardType.CONFINED_SPACE)
            )
            WorkType.MAINTENANCE -> WorkTypeInfo(
                type = type,
                name = "Maintenance",
                description = "Equipment and facility maintenance",
                commonHazards = listOf(HazardType.EQUIPMENT_SAFETY, HazardType.ELECTRICAL)
            )
            WorkType.OTHER -> WorkTypeInfo(
                type = type,
                name = "Other",
                description = "Other construction-related activities",
                commonHazards = emptyList()
            )
            WorkType.GENERAL_SAFETY -> WorkTypeInfo(
                type = type,
                name = "General Safety",
                description = "General safety procedures and practices",
                commonHazards = listOf(HazardType.PPE_VIOLATION)
            )
            WorkType.ELECTRICAL_SAFETY -> WorkTypeInfo(
                type = type,
                name = "Electrical Safety",
                description = "Electrical safety procedures",
                commonHazards = listOf(HazardType.ELECTRICAL)
            )
            WorkType.FALL_PROTECTION -> WorkTypeInfo(
                type = type,
                name = "Fall Protection",
                description = "Fall protection systems and procedures",
                commonHazards = listOf(HazardType.FALL_PROTECTION)
            )
            WorkType.CRANE_LIFTING -> WorkTypeInfo(
                type = type,
                name = "Crane & Lifting",
                description = "Crane and lifting operations",
                commonHazards = listOf(HazardType.CRANE_LIFT)
            )
            WorkType.CONFINED_SPACE -> WorkTypeInfo(
                type = type,
                name = "Confined Space",
                description = "Confined space entry and work",
                commonHazards = listOf(HazardType.CONFINED_SPACE)
            )
            WorkType.CHEMICAL_SAFETY -> WorkTypeInfo(
                type = type,
                name = "Chemical Safety",
                description = "Chemical handling and safety",
                commonHazards = listOf(HazardType.CHEMICAL)
            )
            WorkType.FIRE_SAFETY -> WorkTypeInfo(
                type = type,
                name = "Fire Safety",
                description = "Fire prevention and safety",
                commonHazards = listOf(HazardType.FIRE)
            )
            WorkType.EMERGENCY_PROCEDURES -> WorkTypeInfo(
                type = type,
                name = "Emergency Procedures",
                description = "Emergency response procedures",
                commonHazards = emptyList()
            )
            WorkType.EQUIPMENT_SAFETY -> WorkTypeInfo(
                type = type,
                name = "Equipment Safety",
                description = "Equipment operation and safety",
                commonHazards = listOf(HazardType.EQUIPMENT_SAFETY)
            )
            WorkType.ERGONOMICS -> WorkTypeInfo(
                type = type,
                name = "Ergonomics",
                description = "Ergonomic practices and procedures",
                commonHazards = listOf(HazardType.ERGONOMIC)
            )
            WorkType.HOUSEKEEPING -> WorkTypeInfo(
                type = type,
                name = "Housekeeping",
                description = "Workplace cleanliness and organization",
                commonHazards = listOf(HazardType.HOUSEKEEPING)
            )
            WorkType.TRAINING_COMMUNICATION -> WorkTypeInfo(
                type = type,
                name = "Training & Communication",
                description = "Safety training and communication",
                commonHazards = emptyList()
            )
        }
    }
}

@Serializable
enum class ComplianceStatus {
    Unknown, Compliant, NonCompliant, ReviewRequired
}

@Serializable
enum class SyncStatus {
    Pending, InProgress, Completed, Failed
}

@Serializable
data class PhotoMetadata(
    val location: String? = null,
    val projectId: String? = null,
    val userId: String? = null
)

@Serializable
data class PhotoStorageInfo(
    val totalPhotos: Int,
    val totalStorageBytes: Long,
    val thumbnailStorageBytes: Long,
    val duplicatePhotos: Int,
    val oldestPhotoTimestamp: Long?,
    val newestPhotoTimestamp: Long?,
    val averageFileSize: Long,
    val storageByMonth: Map<String, Long>
)

@Serializable
data class DuplicatePhotoGroup(
    val photos: List<Photo>
)

@Serializable
data class ExifData(
    val data: Map<String, String>
)

@Serializable
data class StorageStats(
    val totalPhotos: Int,
    val totalStorageBytes: Long
)

/**
 * Simple double formatter for KMP compatibility
 */
private fun Double.format(digits: Int): String {
    return toString().let { str ->
        val dotIndex = str.indexOf('.')
        if (dotIndex == -1) str else str.substring(0, minOf(str.length, dotIndex + digits + 1))
    }
}
