package com.hazardhawk.privacy

import android.content.Context
import android.util.Log
import com.hazardhawk.camera.LocationData
import com.hazardhawk.security.SecureKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import kotlin.math.round
import kotlin.math.pow
import kotlin.random.Random

/**
 * GPS Privacy Manager implementing GDPR-compliant location data handling
 *
 * Features:
 * - Explicit consent collection for location data
 * - Coordinate fuzzing for privacy protection
 * - Data minimization and retention controls
 * - Audit trail for compliance
 */
class GPSPrivacyManager(
    private val context: Context,
    private val secureKeyManager: SecureKeyManager
) {
    companion object {
        private const val TAG = "GPSPrivacyManager"
        private const val GPS_CONSENT_KEY = "gps_consent_given"
        private const val GPS_CONSENT_TIMESTAMP_KEY = "gps_consent_timestamp"
        private const val GPS_PRECISION_KEY = "gps_precision_level"
        private const val GPS_RETENTION_DAYS_KEY = "gps_retention_days"

        // Default retention: 30 years for OSHA compliance when consent given
        private const val DEFAULT_RETENTION_DAYS = 30 * 365
        private const val HIGH_PRECISION_METERS = 1.0 // ~1 meter accuracy
        private const val MEDIUM_PRECISION_METERS = 100.0 // ~100 meter accuracy
        private const val LOW_PRECISION_METERS = 1000.0 // ~1 km accuracy
    }

    private val _privacySettings = MutableStateFlow(GPSPrivacySettings())
    val privacySettings: StateFlow<GPSPrivacySettings> = _privacySettings.asStateFlow()

    /**
     * GPS Privacy precision levels
     */
    enum class PrecisionLevel(
        val displayName: String,
        val accuracyMeters: Double,
        val description: String
    ) {
        EXACT("Exact Location", HIGH_PRECISION_METERS, "Full GPS precision for critical safety documentation"),
        APPROXIMATE("Approximate Area", MEDIUM_PRECISION_METERS, "Fuzzes location to ~100m area for general safety records"),
        GENERAL_AREA("General Area", LOW_PRECISION_METERS, "Fuzzes location to ~1km area for basic incident tracking"),
        DISABLED("No Location", 0.0, "Location data disabled - coordinates not collected")
    }

    init {
        loadPrivacySettings()
    }

    /**
     * Check if user has given explicit consent for GPS data collection
     */
    fun hasGPSConsent(): Boolean {
        val consentGiven = secureKeyManager.getGenericData(GPS_CONSENT_KEY)?.toBoolean() ?: false
        val consentTimestamp = secureKeyManager.getGenericData(GPS_CONSENT_TIMESTAMP_KEY)?.toLongOrNull() ?: 0L

        // Check if consent is still valid (not older than 1 year per GDPR best practices)
        val oneYearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
        val consentValid = consentTimestamp > oneYearAgo

        return consentGiven && consentValid
    }

    /**
     * Request GPS consent with clear privacy information
     */
    fun requestGPSConsent(
        precisionLevel: PrecisionLevel = PrecisionLevel.APPROXIMATE,
        retentionDays: Int = DEFAULT_RETENTION_DAYS
    ): GPSConsentRequest {
        return GPSConsentRequest(
            precisionLevel = precisionLevel,
            retentionDays = retentionDays,
            onConsentGranted = { level, retention ->
                grantGPSConsent(level, retention)
            },
            onConsentDenied = {
                denyGPSConsent()
            }
        )
    }

    /**
     * Grant GPS consent with specified privacy settings
     */
    private fun grantGPSConsent(
        precisionLevel: PrecisionLevel,
        retentionDays: Int
    ) {
        try {
            val timestamp = System.currentTimeMillis()

            secureKeyManager.storeGenericData(GPS_CONSENT_KEY, "true")
            secureKeyManager.storeGenericData(GPS_CONSENT_TIMESTAMP_KEY, timestamp.toString())
            secureKeyManager.storeGenericData(GPS_PRECISION_KEY, precisionLevel.name)
            secureKeyManager.storeGenericData(GPS_RETENTION_DAYS_KEY, retentionDays.toString())

            updatePrivacySettings()
            logPrivacyEvent("GPS_CONSENT_GRANTED", mapOf(
                "precision_level" to precisionLevel.name,
                "retention_days" to retentionDays,
                "timestamp" to timestamp
            ))

            Log.i(TAG, "GPS consent granted with precision: ${precisionLevel.displayName}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to store GPS consent", e)
            throw SecurityException("Failed to securely store GPS consent", e)
        }
    }

    /**
     * Deny GPS consent and disable location features
     */
    private fun denyGPSConsent() {
        try {
            secureKeyManager.storeGenericData(GPS_CONSENT_KEY, "false")
            secureKeyManager.storeGenericData(GPS_PRECISION_KEY, PrecisionLevel.DISABLED.name)

            updatePrivacySettings()
            logPrivacyEvent("GPS_CONSENT_DENIED", mapOf(
                "timestamp" to System.currentTimeMillis()
            ))

            Log.i(TAG, "GPS consent denied - location features disabled")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to store GPS consent denial", e)
        }
    }

    /**
     * Withdraw GPS consent (GDPR Article 7.3)
     */
    fun withdrawGPSConsent() {
        try {
            secureKeyManager.removeGenericData(GPS_CONSENT_KEY)
            secureKeyManager.removeGenericData(GPS_CONSENT_TIMESTAMP_KEY)
            secureKeyManager.removeGenericData(GPS_PRECISION_KEY)
            secureKeyManager.removeGenericData(GPS_RETENTION_DAYS_KEY)

            updatePrivacySettings()
            logPrivacyEvent("GPS_CONSENT_WITHDRAWN", mapOf(
                "timestamp" to System.currentTimeMillis()
            ))

            Log.i(TAG, "GPS consent withdrawn - location features disabled")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to withdraw GPS consent", e)
        }
    }

    /**
     * Process location data according to user privacy preferences
     */
    fun processLocationData(originalLocation: LocationData): LocationData? {
        if (!hasGPSConsent()) {
            Log.d(TAG, "GPS consent not given - returning null location")
            return null
        }

        val precisionLevel = getCurrentPrecisionLevel()
        if (precisionLevel == PrecisionLevel.DISABLED) {
            return null
        }

        return when (precisionLevel) {
            PrecisionLevel.EXACT -> originalLocation
            PrecisionLevel.APPROXIMATE -> fuzzyLocation(originalLocation, MEDIUM_PRECISION_METERS)
            PrecisionLevel.GENERAL_AREA -> fuzzyLocation(originalLocation, LOW_PRECISION_METERS)
            PrecisionLevel.DISABLED -> null
        }
    }

    /**
     * Create fuzzy location data for privacy protection
     */
    private fun fuzzyLocation(original: LocationData, accuracyMeters: Double): LocationData {
        // Convert meters to approximate degrees (rough conversion)
        val latitudeDegreeOffset = accuracyMeters / 111000.0 // ~111km per degree
        val longitudeDegreeOffset = accuracyMeters / (111000.0 * kotlin.math.cos(Math.toRadians(original.latitude)))

        // Add random offset within the specified accuracy range
        val random = Random.Default
        val latOffset = (random.nextDouble() - 0.5) * 2 * latitudeDegreeOffset
        val lngOffset = (random.nextDouble() - 0.5) * 2 * longitudeDegreeOffset

        val fuzzedLat = original.latitude + latOffset
        val fuzzedLng = original.longitude + lngOffset

        // Round to appropriate precision based on accuracy
        val precision = when {
            accuracyMeters >= 1000 -> 2 // ~1km precision
            accuracyMeters >= 100 -> 3  // ~100m precision
            else -> 4 // ~10m precision
        }

        val roundedLat = round(fuzzedLat * 10.0.pow(precision.toDouble())) / 10.0.pow(precision.toDouble())
        val roundedLng = round(fuzzedLng * 10.0.pow(precision.toDouble())) / 10.0.pow(precision.toDouble())

        return original.copy(
            latitude = roundedLat,
            longitude = roundedLng,
            accuracy = accuracyMeters.toFloat(),
            address = "Approximate Location (Privacy Protected)"
        )
    }

    /**
     * Get current precision level setting
     */
    private fun getCurrentPrecisionLevel(): PrecisionLevel {
        val levelName = secureKeyManager.getGenericData(GPS_PRECISION_KEY) ?: PrecisionLevel.DISABLED.name
        return try {
            PrecisionLevel.valueOf(levelName)
        } catch (e: Exception) {
            Log.w(TAG, "Invalid precision level stored: $levelName")
            PrecisionLevel.DISABLED
        }
    }

    /**
     * Get GPS consent information for transparency
     */
    fun getGPSConsentInfo(): GPSConsentInfo {
        val consentTimestamp = secureKeyManager.getGenericData(GPS_CONSENT_TIMESTAMP_KEY)?.toLongOrNull() ?: 0L
        val retentionDays = secureKeyManager.getGenericData(GPS_RETENTION_DAYS_KEY)?.toIntOrNull() ?: DEFAULT_RETENTION_DAYS
        val precisionLevel = getCurrentPrecisionLevel()

        return GPSConsentInfo(
            hasConsent = hasGPSConsent(),
            consentTimestamp = consentTimestamp,
            precisionLevel = precisionLevel,
            retentionDays = retentionDays,
            expirationTimestamp = consentTimestamp + (365L * 24 * 60 * 60 * 1000) // 1 year
        )
    }

    /**
     * Update privacy settings state
     */
    private fun updatePrivacySettings() {
        val consentInfo = getGPSConsentInfo()
        _privacySettings.value = GPSPrivacySettings(
            gpsConsentGiven = consentInfo.hasConsent,
            precisionLevel = consentInfo.precisionLevel,
            retentionDays = consentInfo.retentionDays,
            consentTimestamp = consentInfo.consentTimestamp
        )
    }

    /**
     * Load privacy settings from storage
     */
    private fun loadPrivacySettings() {
        updatePrivacySettings()
    }

    /**
     * Log privacy events for audit trail
     */
    private fun logPrivacyEvent(event: String, metadata: Map<String, Any>) {
        Log.i(TAG, "Privacy Event: $event, Metadata: $metadata")

        // Store in audit log for GDPR compliance
        try {
            val auditEvent = buildString {
                append("event:$event|")
                append("timestamp:${System.currentTimeMillis()}|")
                metadata.forEach { (key, value) ->
                    append("$key:$value|")
                }
            }

            // Hash and store audit event securely
            val eventHash = hashAuditEvent(auditEvent)
            secureKeyManager.storeGenericData("audit_gps_$eventHash", auditEvent)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to log privacy event", e)
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
            "AUDIT_${System.currentTimeMillis()}"
        }
    }
}

/**
 * GPS Privacy Settings
 */
data class GPSPrivacySettings(
    val gpsConsentGiven: Boolean = false,
    val precisionLevel: GPSPrivacyManager.PrecisionLevel = GPSPrivacyManager.PrecisionLevel.DISABLED,
    val retentionDays: Int = 0,
    val consentTimestamp: Long = 0L
)

/**
 * GPS Consent Request
 */
data class GPSConsentRequest(
    val precisionLevel: GPSPrivacyManager.PrecisionLevel,
    val retentionDays: Int,
    val onConsentGranted: (GPSPrivacyManager.PrecisionLevel, Int) -> Unit,
    val onConsentDenied: () -> Unit
)

/**
 * GPS Consent Information for transparency
 */
data class GPSConsentInfo(
    val hasConsent: Boolean,
    val consentTimestamp: Long,
    val precisionLevel: GPSPrivacyManager.PrecisionLevel,
    val retentionDays: Int,
    val expirationTimestamp: Long
)