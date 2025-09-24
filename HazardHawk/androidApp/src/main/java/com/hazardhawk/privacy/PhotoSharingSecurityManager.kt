package com.hazardhawk.privacy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.hazardhawk.camera.CaptureMetadata
import com.hazardhawk.camera.LocationData
import com.hazardhawk.camera.MetadataEmbedder
import com.hazardhawk.domain.entities.Photo
import com.hazardhawk.security.SecureKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Photo Sharing Security Manager implementing GDPR-compliant photo sanitization
 *
 * Features:
 * - Automatic metadata sanitization before sharing
 * - User consent for sharing sensitive data
 * - Audit trail for data protection compliance
 * - Separate sanitized photo storage
 */
class PhotoSharingSecurityManager(
    private val context: Context,
    private val secureKeyManager: SecureKeyManager
) {
    companion object {
        private const val TAG = "PhotoSharingSecurityManager"
        private const val SANITIZED_PHOTOS_DIR = "sanitized_photos"
        private const val SHARING_CONSENT_KEY = "photo_sharing_consent"
        private const val SHARING_AUDIT_PREFIX = "audit_share_"
    }

    /**
     * Photo sharing security levels
     */
    enum class SharingSecurityLevel(
        val displayName: String,
        val description: String,
        val removeGPS: Boolean,
        val removePersonalData: Boolean,
        val addWatermark: Boolean
    ) {
        FULL_PRIVACY(
            "Full Privacy Protection",
            "Removes all GPS, personal info, and device data. Adds privacy watermark.",
            removeGPS = true,
            removePersonalData = true,
            addWatermark = true
        ),
        BUSINESS_SAFE(
            "Business Safe",
            "Keeps project info but removes GPS and personal data.",
            removeGPS = true,
            removePersonalData = true,
            addWatermark = false
        ),
        INTERNAL_SHARING(
            "Internal Team Sharing",
            "Removes GPS but keeps business metadata for internal use.",
            removeGPS = true,
            removePersonalData = false,
            addWatermark = false
        ),
        UNRESTRICTED(
            "Unrestricted (Not Recommended)",
            "Shares original photo with all metadata intact.",
            removeGPS = false,
            removePersonalData = false,
            addWatermark = false
        )
    }

    /**
     * Create sanitized photo for sharing based on security level
     */
    suspend fun createSanitizedPhotoForSharing(
        originalPhoto: Photo,
        securityLevel: SharingSecurityLevel = SharingSecurityLevel.FULL_PRIVACY,
        userConsent: Boolean = false
    ): Result<SanitizedPhotoResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating sanitized photo for sharing: ${originalPhoto.id}")

            val originalFile = File(originalPhoto.filePath)
            if (!originalFile.exists()) {
                return@withContext Result.failure(Exception("Original photo file not found"))
            }

            // Create sanitized photos directory if it doesn't exist
            val sanitizedDir = File(context.filesDir, SANITIZED_PHOTOS_DIR)
            if (!sanitizedDir.exists()) {
                sanitizedDir.mkdirs()
            }

            // Generate unique filename for sanitized photo
            val sanitizedFileName = "sanitized_${originalPhoto.id}_${System.currentTimeMillis()}.jpg"
            val sanitizedFile = File(sanitizedDir, sanitizedFileName)

            // Extract original metadata
            val metadataEmbedder = MetadataEmbedder(context)
            val originalMetadata = metadataEmbedder.extractMetadataFromPhoto(originalFile)

            // Create sanitized metadata based on security level
            val sanitizedMetadata = createSanitizedMetadata(originalMetadata, securityLevel)

            // Copy and process the photo
            val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
                ?: return@withContext Result.failure(Exception("Failed to decode original photo"))

            // Apply watermark if required
            val finalBitmap = if (securityLevel.addWatermark) {
                addPrivacyWatermark(bitmap, securityLevel)
            } else {
                bitmap
            }

            // Save sanitized photo
            FileOutputStream(sanitizedFile).use { outputStream ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }

            // Embed sanitized metadata
            if (sanitizedMetadata != null) {
                metadataEmbedder.embedMetadata(sanitizedFile, sanitizedMetadata, false)
            } else {
                // If no metadata, create minimal EXIF
                createMinimalExif(sanitizedFile)
            }

            // Log sharing activity for audit trail
            logSharingActivity(
                photoId = originalPhoto.id,
                securityLevel = securityLevel,
                userConsent = userConsent,
                sanitizedFilePath = sanitizedFile.absolutePath
            )

            // Clean up bitmaps
            bitmap.recycle()
            if (finalBitmap != bitmap) {
                finalBitmap.recycle()
            }

            val result = SanitizedPhotoResult(
                sanitizedFile = sanitizedFile,
                originalPhoto = originalPhoto,
                securityLevel = securityLevel,
                sanitizationActions = getSanitizationActions(securityLevel),
                userConsentGiven = userConsent
            )

            Log.i(TAG, "Successfully created sanitized photo: ${sanitizedFile.absolutePath}")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create sanitized photo", e)
            Result.failure(e)
        }
    }

    /**
     * Create sanitized metadata based on security level
     */
    private fun createSanitizedMetadata(
        originalMetadata: CaptureMetadata?,
        securityLevel: SharingSecurityLevel
    ): CaptureMetadata? {
        if (originalMetadata == null) return null

        val sanitizedLocationData = if (securityLevel.removeGPS) {
            LocationData() // Empty location data
        } else {
            originalMetadata.locationData
        }

        val sanitizedProjectName = if (securityLevel.removePersonalData) {
            "HazardHawk Safety Project" // Generic project name
        } else {
            originalMetadata.projectName
        }

        val sanitizedUserName = if (securityLevel.removePersonalData) {
            "" // Remove user name
        } else {
            originalMetadata.userName
        }

        val sanitizedUserId = if (securityLevel.removePersonalData) {
            "" // Remove user ID
        } else {
            originalMetadata.userId
        }

        val sanitizedDeviceInfo = if (securityLevel.removePersonalData) {
            "HazardHawk Mobile App" // Generic device info
        } else {
            originalMetadata.deviceInfo
        }

        return CaptureMetadata(
            timestamp = originalMetadata.timestamp,
            locationData = sanitizedLocationData,
            projectName = sanitizedProjectName,
            projectId = if (securityLevel.removePersonalData) "" else originalMetadata.projectId,
            userName = sanitizedUserName,
            userId = sanitizedUserId,
            deviceInfo = sanitizedDeviceInfo
        )
    }

    /**
     * Add privacy watermark to indicate photo has been sanitized
     */
    private suspend fun addPrivacyWatermark(bitmap: Bitmap, securityLevel: SharingSecurityLevel): Bitmap {
        return withContext(Dispatchers.IO) {
            val watermarkedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = android.graphics.Canvas(watermarkedBitmap)

            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                alpha = 255
                textSize = (bitmap.width * 0.025f).coerceAtLeast(40f)
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                isAntiAlias = true
                setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
            }

            val backgroundPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                alpha = 100
            }

            val watermarkText = "🔒 PRIVACY PROTECTED - ${securityLevel.displayName.uppercase()}"
            val textBounds = android.graphics.Rect()
            paint.getTextBounds(watermarkText, 0, watermarkText.length, textBounds)

            val x = 20f
            val y = bitmap.height - textBounds.height() - 20f

            // Draw background
            canvas.drawRect(
                x - 10f,
                y - textBounds.height() - 10f,
                x + textBounds.width() + 10f,
                y + 10f,
                backgroundPaint
            )

            // Draw text
            canvas.drawText(watermarkText, x, y, paint)

            watermarkedBitmap
        }
    }

    /**
     * Create minimal EXIF data for sanitized photo
     */
    private fun createMinimalExif(photoFile: File) {
        try {
            val exif = ExifInterface(photoFile.absolutePath)

            val dateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())

            exif.setAttribute(ExifInterface.TAG_DATETIME, timestamp)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, timestamp)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, timestamp)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "HazardHawk Privacy Protected")
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "Sanitized Safety Documentation Photo")
            exif.setAttribute(ExifInterface.TAG_COPYRIGHT, "HazardHawk Safety © ${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}")
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "Privacy Protected - Sensitive data removed")

            exif.saveAttributes()

        } catch (e: Exception) {
            Log.w(TAG, "Failed to create minimal EXIF data", e)
        }
    }

    /**
     * Get list of sanitization actions performed
     */
    private fun getSanitizationActions(securityLevel: SharingSecurityLevel): List<String> {
        val actions = mutableListOf<String>()

        if (securityLevel.removeGPS) {
            actions.add("GPS coordinates removed")
        }

        if (securityLevel.removePersonalData) {
            actions.add("Personal information removed")
            actions.add("Device information anonymized")
            actions.add("User identification removed")
        }

        if (securityLevel.addWatermark) {
            actions.add("Privacy protection watermark added")
        }

        actions.add("Timestamp preserved for documentation")
        actions.add("Image quality maintained for safety analysis")

        return actions
    }

    /**
     * Log sharing activity for audit trail
     */
    private fun logSharingActivity(
        photoId: String,
        securityLevel: SharingSecurityLevel,
        userConsent: Boolean,
        sanitizedFilePath: String
    ) {
        try {
            val timestamp = System.currentTimeMillis()
            val auditEvent = buildString {
                append("event:PHOTO_SHARED|")
                append("photo_id:$photoId|")
                append("security_level:${securityLevel.name}|")
                append("user_consent:$userConsent|")
                append("timestamp:$timestamp|")
                append("sanitized_file:${File(sanitizedFilePath).name}|")
            }

            // Hash and store audit event
            val eventHash = hashAuditEvent(auditEvent)
            secureKeyManager.storeGenericData("$SHARING_AUDIT_PREFIX$eventHash", auditEvent)

            Log.i(TAG, "Sharing activity logged: $photoId with ${securityLevel.name}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to log sharing activity", e)
        }
    }

    /**
     * Create hash for audit event identification
     */
    private fun hashAuditEvent(event: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(event.toByteArray())
            android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP).take(12)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hash audit event", e)
            "SHARE_${System.currentTimeMillis()}"
        }
    }

    /**
     * Clean up old sanitized photos to manage storage
     */
    suspend fun cleanupOldSanitizedPhotos(maxAgeMillis: Long = 24 * 60 * 60 * 1000) = withContext(Dispatchers.IO) {
        try {
            val sanitizedDir = File(context.filesDir, SANITIZED_PHOTOS_DIR)
            if (!sanitizedDir.exists()) return@withContext

            val cutoffTime = System.currentTimeMillis() - maxAgeMillis
            var cleanedCount = 0

            sanitizedDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        cleanedCount++
                    }
                }
            }

            Log.i(TAG, "Cleaned up $cleanedCount old sanitized photos")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup sanitized photos", e)
        }
    }

    /**
     * Get sharing consent status
     */
    fun hasSharingConsent(): Boolean {
        return secureKeyManager.getGenericData(SHARING_CONSENT_KEY)?.toBoolean() ?: false
    }

    /**
     * Set sharing consent
     */
    fun setSharingConsent(consent: Boolean) {
        secureKeyManager.storeGenericData(SHARING_CONSENT_KEY, consent.toString())
        logSharingActivity(
            photoId = "SYSTEM",
            securityLevel = SharingSecurityLevel.FULL_PRIVACY,
            userConsent = consent,
            sanitizedFilePath = "CONSENT_CHANGE"
        )
    }
}

/**
 * Result of photo sanitization process
 */
data class SanitizedPhotoResult(
    val sanitizedFile: File,
    val originalPhoto: Photo,
    val securityLevel: PhotoSharingSecurityManager.SharingSecurityLevel,
    val sanitizationActions: List<String>,
    val userConsentGiven: Boolean
)

/**
 * Sharing security options for user selection
 */
data class SharingSecurityOptions(
    val availableLevels: List<PhotoSharingSecurityManager.SharingSecurityLevel>,
    val recommendedLevel: PhotoSharingSecurityManager.SharingSecurityLevel,
    val requiresConsent: Boolean
)