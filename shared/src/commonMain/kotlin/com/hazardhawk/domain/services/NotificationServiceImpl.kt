package com.hazardhawk.domain.services

import com.hazardhawk.models.crew.WorkerCertification
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Production implementation of NotificationService with multi-channel delivery.
 *
 * Features:
 * - Multi-channel delivery (Email, SMS, Push)
 * - Template system with 5 urgency levels
 * - Retry logic (3 attempts per channel)
 * - Graceful degradation (partial failures tolerated)
 * - Color-coded HTML emails
 * - Adaptive channel selection based on urgency
 *
 * TODO: Replace stubbed API calls with actual integrations:
 * - Email: SendGrid, AWS SES, or Mailgun
 * - SMS: Twilio, AWS SNS, or MessageBird
 * - Push: Firebase Cloud Messaging, AWS SNS, or OneSignal
 */
class NotificationServiceImpl : NotificationService {

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L

        // Urgency thresholds (days)
        private const val THRESHOLD_INFORMATION = 90  // 90+ days
        private const val THRESHOLD_ACTION = 30       // 30 days
        private const val THRESHOLD_URGENT = 7        // 7 days
        private const val THRESHOLD_EXPIRED = 0       // 0 days (expired)
    }

    /**
     * Urgency levels with associated colors and channel priorities.
     */
    private enum class UrgencyLevel(
        val color: String,
        val label: String,
        val useEmail: Boolean,
        val useSMS: Boolean,
        val usePush: Boolean
    ) {
        INFORMATION("#3B82F6", "Information", useEmail = true, useSMS = false, usePush = false),
        ACTION("#F59E0B", "Action Required", useEmail = true, useSMS = true, usePush = false),
        URGENT("#F97316", "URGENT", useEmail = true, useSMS = true, usePush = true),
        EXPIRED("#EF4444", "EXPIRED", useEmail = true, useSMS = true, usePush = true),
        CRITICAL("#DC2626", "CRITICAL", useEmail = true, useSMS = true, usePush = true)
    }

    override suspend fun sendCertificationExpirationAlert(
        workerId: String,
        certification: WorkerCertification,
        daysUntilExpiration: Int
    ): Result<Unit> = runCatching {
        val urgency = determineUrgency(daysUntilExpiration)
        val certName = certification.certificationType?.name ?: "Certification"

        // Track channel results
        val results = mutableListOf<Result<Unit>>()

        // Send via configured channels with retry logic
        if (urgency.useEmail) {
            results.add(
                sendEmailWithRetry(
                    to = "worker-${workerId}@example.com", // TODO: Lookup actual email from worker profile
                    subject = buildEmailSubject(certName, urgency, daysUntilExpiration),
                    body = buildHtmlEmailBody(certification, urgency, daysUntilExpiration)
                )
            )
        }

        if (urgency.useSMS) {
            results.add(
                sendSMSWithRetry(
                    to = "+1234567890", // TODO: Lookup actual phone from worker profile
                    message = buildSMSMessage(certName, daysUntilExpiration)
                )
            )
        }

        if (urgency.usePush) {
            results.add(
                sendPushWithRetry(
                    userId = workerId,
                    title = buildPushTitle(urgency),
                    body = buildPushBody(certName, daysUntilExpiration)
                )
            )
        }

        // Graceful degradation: Succeed if at least one channel succeeded
        val anySuccess = results.any { it.isSuccess }
        if (!anySuccess && results.isNotEmpty()) {
            throw Exception("All notification channels failed: ${results.mapNotNull { it.exceptionOrNull()?.message }.joinToString()}")
        }
    }

    override suspend fun sendEmail(
        to: String,
        subject: String,
        body: String
    ): Result<Unit> = runCatching {
        // TODO: Replace with actual SendGrid/SES/Mailgun API call
        println("📧 [EMAIL] To: $to | Subject: $subject")
        println("Body:\n$body")

        // Simulate API call
        delay(100)

        // Simulated failure for testing (10% failure rate)
        if (kotlin.random.Random.nextDouble() < 0.1) {
            throw Exception("Email service temporarily unavailable")
        }
    }

    override suspend fun sendSMS(
        to: String,
        message: String
    ): Result<Unit> = runCatching {
        // TODO: Replace with actual Twilio/SNS API call
        println("📱 [SMS] To: $to | Message: $message")

        // Simulate API call
        delay(100)

        // Simulated failure for testing (10% failure rate)
        if (kotlin.random.Random.nextDouble() < 0.1) {
            throw Exception("SMS service temporarily unavailable")
        }
    }

    override suspend fun sendPushNotification(
        userId: String,
        title: String,
        body: String
    ): Result<Unit> = runCatching {
        // TODO: Replace with actual FCM/SNS/OneSignal API call
        println("🔔 [PUSH] User: $userId | Title: $title | Body: $body")

        // Simulate API call
        delay(100)

        // Simulated failure for testing (10% failure rate)
        if (kotlin.random.Random.nextDouble() < 0.1) {
            throw Exception("Push notification service temporarily unavailable")
        }
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    /**
     * Determines urgency level based on days until expiration.
     */
    private fun determineUrgency(daysUntilExpiration: Int): UrgencyLevel {
        return when {
            daysUntilExpiration < 0 -> UrgencyLevel.EXPIRED
            daysUntilExpiration == 0 -> UrgencyLevel.CRITICAL
            daysUntilExpiration <= THRESHOLD_URGENT -> UrgencyLevel.URGENT
            daysUntilExpiration <= THRESHOLD_ACTION -> UrgencyLevel.ACTION
            else -> UrgencyLevel.INFORMATION
        }
    }

    /**
     * Retry wrapper for email sending.
     */
    private suspend fun sendEmailWithRetry(
        to: String,
        subject: String,
        body: String
    ): Result<Unit> {
        return retryWithBackoff(MAX_RETRIES) {
            sendEmail(to, subject, body).getOrThrow()
        }
    }

    /**
     * Retry wrapper for SMS sending.
     */
    private suspend fun sendSMSWithRetry(
        to: String,
        message: String
    ): Result<Unit> {
        return retryWithBackoff(MAX_RETRIES) {
            sendSMS(to, message).getOrThrow()
        }
    }

    /**
     * Retry wrapper for push notification sending.
     */
    private suspend fun sendPushWithRetry(
        userId: String,
        title: String,
        body: String
    ): Result<Unit> {
        return retryWithBackoff(MAX_RETRIES) {
            sendPushNotification(userId, title, body).getOrThrow()
        }
    }

    /**
     * Generic retry logic with exponential backoff.
     */
    private suspend fun retryWithBackoff(
        maxRetries: Int,
        block: suspend () -> Unit
    ): Result<Unit> = runCatching {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                block()
                return@runCatching // Success
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    // Exponential backoff: 1s, 2s, 4s
                    delay(RETRY_DELAY_MS * (1 shl attempt))
                }
            }
        }

        throw lastException ?: Exception("Retry failed with unknown error")
    }

    // ============================================================================
    // EMAIL TEMPLATE BUILDERS
    // ============================================================================

    private fun buildEmailSubject(
        certName: String,
        urgency: UrgencyLevel,
        daysUntilExpiration: Int
    ): String {
        return when {
            daysUntilExpiration < 0 -> "🚨 EXPIRED: $certName Certification"
            daysUntilExpiration == 0 -> "🚨 EXPIRES TODAY: $certName Certification"
            daysUntilExpiration <= 7 -> "⚠️ URGENT: $certName Expires in $daysUntilExpiration Days"
            daysUntilExpiration <= 30 -> "⏰ Action Required: $certName Expires in $daysUntilExpiration Days"
            else -> "📋 Reminder: $certName Expires in $daysUntilExpiration Days"
        }
    }

    private fun buildHtmlEmailBody(
        certification: WorkerCertification,
        urgency: UrgencyLevel,
        daysUntilExpiration: Int
    ): String {
        val certName = certification.certificationType?.name ?: "Certification"
        val certCode = certification.certificationType?.code ?: "N/A"
        val expirationDate = certification.expirationDate?.toString() ?: "Unknown"
        val certNumber = certification.certificationNumber ?: "N/A"

        val statusText = when {
            daysUntilExpiration < 0 -> "EXPIRED ${-daysUntilExpiration} days ago"
            daysUntilExpiration == 0 -> "EXPIRES TODAY"
            else -> "Expires in $daysUntilExpiration days"
        }

        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .header {
            background: ${urgency.color};
            color: white;
            padding: 20px;
            border-radius: 8px 8px 0 0;
            text-align: center;
        }
        .header h1 {
            margin: 0;
            font-size: 24px;
        }
        .urgency-badge {
            display: inline-block;
            background: rgba(255, 255, 255, 0.3);
            padding: 4px 12px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: bold;
            margin-top: 8px;
        }
        .content {
            background: #f9fafb;
            padding: 30px 20px;
            border-radius: 0 0 8px 8px;
        }
        .cert-details {
            background: white;
            padding: 20px;
            border-radius: 8px;
            margin: 20px 0;
            border-left: 4px solid ${urgency.color};
        }
        .cert-row {
            display: flex;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid #e5e7eb;
        }
        .cert-row:last-child {
            border-bottom: none;
        }
        .cert-label {
            font-weight: 600;
            color: #6b7280;
        }
        .cert-value {
            color: #111827;
            text-align: right;
        }
        .action-required {
            background: ${urgency.color};
            color: white;
            padding: 15px;
            border-radius: 8px;
            margin: 20px 0;
            text-align: center;
        }
        .footer {
            text-align: center;
            margin-top: 30px;
            padding-top: 20px;
            border-top: 1px solid #e5e7eb;
            color: #6b7280;
            font-size: 14px;
        }
        .cta-button {
            display: inline-block;
            background: white;
            color: ${urgency.color};
            padding: 12px 24px;
            border-radius: 6px;
            text-decoration: none;
            font-weight: bold;
            margin-top: 10px;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>🔒 Certification Expiration Alert</h1>
        <div class="urgency-badge">${urgency.label}</div>
    </div>

    <div class="content">
        <h2 style="color: #111827; margin-top: 0;">$statusText</h2>

        <div class="cert-details">
            <div class="cert-row">
                <span class="cert-label">Certification</span>
                <span class="cert-value">$certName</span>
            </div>
            <div class="cert-row">
                <span class="cert-label">Certification Code</span>
                <span class="cert-value">$certCode</span>
            </div>
            <div class="cert-row">
                <span class="cert-label">Certificate Number</span>
                <span class="cert-value">$certNumber</span>
            </div>
            <div class="cert-row">
                <span class="cert-label">Expiration Date</span>
                <span class="cert-value">$expirationDate</span>
            </div>
            <div class="cert-row">
                <span class="cert-label">Status</span>
                <span class="cert-value" style="color: ${urgency.color}; font-weight: bold;">$statusText</span>
            </div>
        </div>

        <div class="action-required">
            <strong>Action Required:</strong><br>
            ${getActionMessage(daysUntilExpiration)}<br>
            <a href="#" class="cta-button">Renew Certification</a>
        </div>

        <p style="color: #6b7280; font-size: 14px;">
            <strong>Important:</strong> ${getImportanceMessage(daysUntilExpiration)}
        </p>
    </div>

    <div class="footer">
        <p>HazardHawk Safety Management System<br>
        This is an automated notification. Please do not reply to this email.</p>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun getActionMessage(daysUntilExpiration: Int): String {
        return when {
            daysUntilExpiration < 0 -> "Your certification has expired. You are not authorized to work until renewed."
            daysUntilExpiration == 0 -> "Your certification expires today. Renew immediately to avoid work interruption."
            daysUntilExpiration <= 7 -> "Renew your certification within the next $daysUntilExpiration days to avoid work stoppage."
            daysUntilExpiration <= 30 -> "Please schedule your certification renewal within the next $daysUntilExpiration days."
            else -> "Your certification renewal is approaching. Please plan accordingly."
        }
    }

    private fun getImportanceMessage(daysUntilExpiration: Int): String {
        return when {
            daysUntilExpiration < 0 -> "Working with an expired certification violates OSHA regulations and company policy."
            daysUntilExpiration <= 7 -> "Failure to renew may result in immediate work suspension and project delays."
            else -> "Maintaining current certifications is required for continued employment on this project."
        }
    }

    // ============================================================================
    // SMS TEMPLATE BUILDER
    // ============================================================================

    private fun buildSMSMessage(
        certName: String,
        daysUntilExpiration: Int
    ): String {
        return when {
            daysUntilExpiration < 0 -> "🚨 URGENT: Your $certName certification EXPIRED ${-daysUntilExpiration} days ago. Contact safety lead immediately."
            daysUntilExpiration == 0 -> "🚨 URGENT: Your $certName certification EXPIRES TODAY. Renew immediately."
            daysUntilExpiration <= 7 -> "⚠️ URGENT: Your $certName expires in $daysUntilExpiration days. Renew ASAP to avoid work stoppage."
            else -> "⏰ Reminder: Your $certName expires in $daysUntilExpiration days. Please schedule renewal."
        }
    }

    // ============================================================================
    // PUSH NOTIFICATION TEMPLATE BUILDERS
    // ============================================================================

    private fun buildPushTitle(urgency: UrgencyLevel): String {
        return when (urgency) {
            UrgencyLevel.EXPIRED -> "🚨 Certification EXPIRED"
            UrgencyLevel.CRITICAL -> "🚨 Certification Expires TODAY"
            UrgencyLevel.URGENT -> "⚠️ Urgent: Certification Expiring Soon"
            UrgencyLevel.ACTION -> "⏰ Action Required: Certification Renewal"
            UrgencyLevel.INFORMATION -> "📋 Certification Renewal Reminder"
        }
    }

    private fun buildPushBody(
        certName: String,
        daysUntilExpiration: Int
    ): String {
        return when {
            daysUntilExpiration < 0 -> "$certName expired ${-daysUntilExpiration} days ago. Contact safety lead immediately."
            daysUntilExpiration == 0 -> "$certName expires today. Renew immediately."
            else -> "$certName expires in $daysUntilExpiration days. Tap to renew."
        }
    }
}
