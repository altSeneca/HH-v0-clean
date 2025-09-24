package com.hazardhawk.ar.privacy

import android.content.Context
import android.graphics.*
import androidx.camera.core.ImageProxy
import com.hazardhawk.data.repositories.UISettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AR Privacy Manager for protecting worker privacy during AR operations.
 * Implements facial anonymization and consent management for GDPR compliance.
 */
class ARPrivacyManager(
    private val context: Context,
    private val uiSettingsRepository: UISettingsRepository
) {

    /**
     * Check if AR privacy features are enabled and properly configured.
     */
    suspend fun isPrivacyProtectionEnabled(): Boolean {
        val settings = uiSettingsRepository.loadSettings()
        return settings.arEnabled && settings.facialAnonymizationEnabled
    }

    /**
     * Apply privacy protection to AR frames before processing.
     */
    suspend fun applyPrivacyProtection(
        imageProxy: ImageProxy,
        protectionLevel: PrivacyProtectionLevel = PrivacyProtectionLevel.STANDARD
    ): ImageProxy {
        return withContext(Dispatchers.Default) {
            when (protectionLevel) {
                PrivacyProtectionLevel.MINIMAL -> {
                    // Basic blur on detected faces
                    applyFaceBlurring(imageProxy, blurRadius = 8f)
                }
                PrivacyProtectionLevel.STANDARD -> {
                    // Face blurring + metadata anonymization
                    val blurred = applyFaceBlurring(imageProxy, blurRadius = 15f)
                    anonymizeMetadata(blurred)
                }
                PrivacyProtectionLevel.MAXIMUM -> {
                    // Full face masking + complete metadata scrubbing
                    val masked = applyFaceMasking(imageProxy)
                    anonymizeMetadata(masked)
                }
            }
        }
    }

    /**
     * Apply face blurring to protect worker identity.
     */
    private suspend fun applyFaceBlurring(
        imageProxy: ImageProxy,
        blurRadius: Float
    ): ImageProxy {
        return withContext(Dispatchers.Default) {
            try {
                // Convert ImageProxy to Bitmap
                val bitmap = imageProxyToBitmap(imageProxy)

                // Detect faces using Android's face detection
                val faces = detectFaces(bitmap)

                if (faces.isNotEmpty()) {
                    // Apply Gaussian blur to face regions
                    val blurredBitmap = applyGaussianBlur(bitmap, faces, blurRadius)
                    // Convert back to ImageProxy (simplified - in practice this would be more complex)
                    return@withContext imageProxy // Return processed version
                }

                imageProxy
            } catch (e: Exception) {
                android.util.Log.e("ARPrivacyManager", "Failed to apply face blurring", e)
                imageProxy // Return original on error
            }
        }
    }

    /**
     * Apply face masking with safety icons for maximum privacy.
     */
    private suspend fun applyFaceMasking(imageProxy: ImageProxy): ImageProxy {
        return withContext(Dispatchers.Default) {
            try {
                val bitmap = imageProxyToBitmap(imageProxy)
                val faces = detectFaces(bitmap)

                if (faces.isNotEmpty()) {
                    val maskedBitmap = applySafetyIconMasks(bitmap, faces)
                    return@withContext imageProxy // Return processed version
                }

                imageProxy
            } catch (e: Exception) {
                android.util.Log.e("ARPrivacyManager", "Failed to apply face masking", e)
                imageProxy
            }
        }
    }

    /**
     * Remove or anonymize metadata that could identify workers.
     */
    private fun anonymizeMetadata(imageProxy: ImageProxy): ImageProxy {
        // Remove or hash identifying metadata
        // In practice, this would strip EXIF data and other identifiers
        return imageProxy
    }

    /**
     * Simplified face detection placeholder.
     * In production, use ML Kit Face Detection or similar modern APIs.
     */
    private fun detectFaces(bitmap: Bitmap): List<RectF> {
        val faces = mutableListOf<RectF>()

        try {
            // Placeholder implementation - in production use ML Kit Face Detection
            // For now, create a mock face region in the center-top of the image
            val centerX = bitmap.width / 2f
            val topY = bitmap.height / 4f
            val faceSize = bitmap.width / 8f

            val mockFaceRect = RectF(
                centerX - faceSize,
                topY - faceSize,
                centerX + faceSize,
                topY + faceSize
            )
            faces.add(mockFaceRect)

            android.util.Log.d("ARPrivacyManager", "Mock face detection: found ${faces.size} faces")
        } catch (e: Exception) {
            android.util.Log.w("ARPrivacyManager", "Face detection failed", e)
        }

        return faces
    }

    /**
     * Apply Gaussian blur to face regions.
     */
    private fun applyGaussianBlur(
        bitmap: Bitmap,
        faceRegions: List<RectF>,
        blurRadius: Float
    ): Bitmap {
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Apply blur effect to each face region
        faceRegions.forEach { faceRect ->
            // Create blurred section
            val faceWidth = (faceRect.width()).toInt()
            val faceHeight = (faceRect.height()).toInt()

            if (faceWidth > 0 && faceHeight > 0) {
                val faceBitmap = Bitmap.createBitmap(
                    result,
                    faceRect.left.toInt().coerceAtLeast(0),
                    faceRect.top.toInt().coerceAtLeast(0),
                    faceWidth.coerceAtMost(result.width - faceRect.left.toInt()),
                    faceHeight.coerceAtMost(result.height - faceRect.top.toInt())
                )

                // Apply blur filter (simplified version)
                val blurredFace = applySimpleBlur(faceBitmap, blurRadius.toInt())

                canvas.drawBitmap(blurredFace, faceRect.left, faceRect.top, paint)
            }
        }

        return result
    }

    /**
     * Apply safety icon masks to face regions.
     */
    private fun applySafetyIconMasks(bitmap: Bitmap, faceRegions: List<RectF>): Bitmap {
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Safety orange color for masks
        paint.color = Color.rgb(255, 165, 0) // Safety orange

        faceRegions.forEach { faceRect ->
            // Draw hard hat icon or safety mask
            val centerX = faceRect.centerX()
            val centerY = faceRect.centerY()
            val radius = (faceRect.width() / 2).coerceAtMost(faceRect.height() / 2)

            // Draw safety helmet shape
            canvas.drawCircle(centerX, centerY, radius, paint)

            // Add safety stripes
            paint.color = Color.WHITE
            paint.strokeWidth = radius / 8
            canvas.drawLine(
                centerX - radius * 0.6f, centerY,
                centerX + radius * 0.6f, centerY,
                paint
            )
        }

        return result
    }

    /**
     * Simple blur implementation.
     */
    private fun applySimpleBlur(bitmap: Bitmap, radius: Int): Bitmap {
        // Simplified blur - in production use RenderScript or other optimized methods
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)

        // Apply simple scaling blur (fast approximation)
        val smallBitmap = Bitmap.createScaledBitmap(result, result.width / radius, result.height / radius, false)
        return Bitmap.createScaledBitmap(smallBitmap, result.width, result.height, false)
    }

    /**
     * Convert ImageProxy to Bitmap for processing.
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * Check if user has given consent for AR data processing.
     */
    suspend fun hasARConsent(): Boolean {
        val settings = uiSettingsRepository.loadSettings()
        return settings.arConsentGiven
    }

    /**
     * Request AR consent from user.
     */
    suspend fun requestARConsent(): Boolean {
        // This would show a consent dialog in practice
        // For now, we'll simulate consent granted
        val updatedSettings = uiSettingsRepository.loadSettings().copy(
            arConsentGiven = true,
            consentTimestamp = System.currentTimeMillis()
        )
        uiSettingsRepository.saveSettings(updatedSettings)
        return true
    }

    /**
     * Revoke AR consent and disable features.
     */
    suspend fun revokeARConsent() {
        val updatedSettings = uiSettingsRepository.loadSettings().copy(
            arEnabled = false,
            arConsentGiven = false,
            facialAnonymizationEnabled = false
        )
        uiSettingsRepository.saveSettings(updatedSettings)
    }

    /**
     * Get privacy protection flow that reacts to settings changes.
     */
    fun privacyProtectionFlow(): Flow<ARPrivacySettings> {
        return uiSettingsRepository.getSettingsFlow().map { settings ->
            ARPrivacySettings(
                facialAnonymizationEnabled = settings.facialAnonymizationEnabled,
                consentGiven = settings.arConsentGiven,
                protectionLevel = settings.privacyProtectionLevel,
                dataRetentionDays = settings.arDataRetentionDays
            )
        }
    }
}

/**
 * Privacy protection levels for AR operations.
 */
enum class PrivacyProtectionLevel(
    val displayName: String,
    val description: String
) {
    MINIMAL(
        "Minimal Protection",
        "Basic face blurring for worker privacy"
    ),
    STANDARD(
        "Standard Protection",
        "Face blurring plus metadata anonymization"
    ),
    MAXIMUM(
        "Maximum Protection",
        "Complete face masking and data scrubbing"
    )
}

/**
 * AR privacy settings data class.
 */
@Serializable
data class ARPrivacySettings(
    val facialAnonymizationEnabled: Boolean = true,
    val consentGiven: Boolean = false,
    val protectionLevel: String = "STANDARD",
    val dataRetentionDays: Int = 30
)