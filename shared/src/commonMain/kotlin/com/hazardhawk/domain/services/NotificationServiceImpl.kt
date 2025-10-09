package com.hazardhawk.domain.services

import com.hazardhawk.models.crew.WorkerCertification
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay

/**
 * Implementation of NotificationService with multi-channel support.
 * Provides retry logic and template-based messaging for certification expiration alerts.
 */
class NotificationServiceImpl(
    private val httpClient: HttpClient,
    private val sendGridApiKey: String? = null,
    private val twilioAccountSid: String? = null,
    private val twilioAuthToken: String? = null,
    private val pushNotificationEndpoint: String? = null
) : NotificationService {

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L

        // Notification templates based on days until expiration
        private const val DAYS_90_THRESHOLD = 90
        private const val DAYS_30_THRESHOLD = 30
        private const val DAYS_7_THRESHOLD = 7
    }

    override suspend fun sendCertificationExpirationAlert(
        workerId: String,
        certification: WorkerCertification,
        daysUntilExpiration: Int
    ): Result<Unit> {
        return try {
            val certTypeName = certification.certificationType?.name ?: "Certification"
            val template = getNotificationTemplate(daysUntilExpiration, certTypeName)

            // TODO: Fetch worker contact information from WorkerRepository
            // For now, using placeholder email/phone
            val workerEmail = "worker-${workerId}@example.com"
            val workerPhone = "+1234567890"

            // Send via multiple channels based on urgency
            val results = mutableListOf<Result<Unit>>()

            // Always send email
            results.add(sendEmail(
                to = workerEmail,
                subject = template.emailSubject,
                body = template.emailBody
            ))

            // Send SMS for urgent notifications (30 days or less)
            if (daysUntilExpiration <= DAYS_30_THRESHOLD) {
                results.add(sendSMS(
                    to = workerPhone,
                    message = template.smsMessage
                ))
            }

            // Send push notification for critical alerts (7 days or less)
            if (daysUntilExpiration <= DAYS_7_THRESHOLD) {
                results.add(sendPushNotification(
                    userId = workerId,
                    title = template.pushTitle,
                    body = template.pushBody
                ))
            }

            // Check if at least one channel succeeded
            val hasSuccess = results.any { it.isSuccess }
            if (hasSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send notification via any channel"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendEmail(
        to: String,
        subject: String,
        body: String
    ): Result<Unit> {
        return withRetry(MAX_RETRY_ATTEMPTS) {
            try {
                // TODO: Implement actual SendGrid integration
                // This is a stub implementation
                if (sendGridApiKey == null) {
                    println("SendGrid not configured - Email stub: to=$to, subject=$subject")
                    return@withRetry Result.success(Unit)
                }

                val response: HttpResponse = httpClient.post("https://api.sendgrid.com/v3/mail/send") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf(
                        "personalizations" to listOf(
                            mapOf("to" to listOf(mapOf("email" to to)))
                        ),
                        "from" to mapOf("email" to "notifications@hazardhawk.com"),
                        "subject" to subject,
                        "content" to listOf(
                            mapOf(
                                "type" to "text/html",
                                "value" to body
                            )
                        )
                    ))
                }

                if (response.status.isSuccess()) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("SendGrid API error: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun sendSMS(
        to: String,
        message: String
    ): Result<Unit> {
        return withRetry(MAX_RETRY_ATTEMPTS) {
            try {
                // TODO: Implement actual Twilio integration
                // This is a stub implementation
                if (twilioAccountSid == null || twilioAuthToken == null) {
                    println("Twilio not configured - SMS stub: to=$to, message=$message")
                    return@withRetry Result.success(Unit)
                }

                val response: HttpResponse = httpClient.post(
                    "https://api.twilio.com/2010-04-01/Accounts/$twilioAccountSid/Messages.json"
                ) {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf(
                        "To" to to,
                        "From" to "+1234567890", // TODO: Configure Twilio phone number
                        "Body" to message
                    ))
                }

                if (response.status.isSuccess()) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Twilio API error: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun sendPushNotification(
        userId: String,
        title: String,
        body: String
    ): Result<Unit> {
        return withRetry(MAX_RETRY_ATTEMPTS) {
            try {
                // TODO: Implement actual push notification service (FCM/APNs)
                // This is a stub implementation
                if (pushNotificationEndpoint == null) {
                    println("Push notifications not configured - Push stub: userId=$userId, title=$title, body=$body")
                    return@withRetry Result.success(Unit)
                }

                val response: HttpResponse = httpClient.post(pushNotificationEndpoint) {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf(
                        "userId" to userId,
                        "notification" to mapOf(
                            "title" to title,
                            "body" to body
                        )
                    ))
                }

                if (response.status.isSuccess()) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Push notification API error: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Executes a block with retry logic.
     * Retries up to maxAttempts times with exponential backoff.
     */
    private suspend fun withRetry(
        maxAttempts: Int,
        block: suspend () -> Result<Unit>
    ): Result<Unit> {
        var lastException: Exception? = null

        repeat(maxAttempts) { attempt ->
            val result = block()
            if (result.isSuccess) {
                return result
            }

            lastException = result.exceptionOrNull() as? Exception

            // Don't delay after the last attempt
            if (attempt < maxAttempts - 1) {
                // Exponential backoff: 1s, 2s, 4s
                delay(RETRY_DELAY_MS * (1L shl attempt))
            }
        }

        return Result.failure(
            lastException ?: Exception("Failed after $maxAttempts attempts")
        )
    }

    /**
     * Generates notification template based on days until expiration.
     */
    private fun getNotificationTemplate(
        daysUntilExpiration: Int,
        certificationName: String
    ): NotificationTemplate {
        return when {
            daysUntilExpiration <= 0 -> {
                NotificationTemplate(
                    emailSubject = "EXPIRED: $certificationName has expired",
                    emailBody = buildEmailBody(
                        urgency = "EXPIRED",
                        message = "Your $certificationName has expired and must be renewed immediately.",
                        daysRemaining = daysUntilExpiration
                    ),
                    smsMessage = "EXPIRED: Your $certificationName has expired. Please renew immediately.",
                    pushTitle = "Certification Expired",
                    pushBody = "Your $certificationName has expired"
                )
            }
            daysUntilExpiration <= DAYS_7_THRESHOLD -> {
                NotificationTemplate(
                    emailSubject = "URGENT: $certificationName expires in $daysUntilExpiration days",
                    emailBody = buildEmailBody(
                        urgency = "URGENT",
                        message = "Your $certificationName expires in $daysUntilExpiration days. Immediate action required.",
                        daysRemaining = daysUntilExpiration
                    ),
                    smsMessage = "URGENT: Your $certificationName expires in $daysUntilExpiration days. Renew now.",
                    pushTitle = "Urgent: Certification Expiring Soon",
                    pushBody = "$certificationName expires in $daysUntilExpiration days"
                )
            }
            daysUntilExpiration <= DAYS_30_THRESHOLD -> {
                NotificationTemplate(
                    emailSubject = "Action Required: $certificationName expires in $daysUntilExpiration days",
                    emailBody = buildEmailBody(
                        urgency = "Action Required",
                        message = "Your $certificationName expires in $daysUntilExpiration days. Please schedule renewal.",
                        daysRemaining = daysUntilExpiration
                    ),
                    smsMessage = "Action required: Your $certificationName expires in $daysUntilExpiration days.",
                    pushTitle = "Certification Expiring",
                    pushBody = "$certificationName expires in $daysUntilExpiration days"
                )
            }
            daysUntilExpiration <= DAYS_90_THRESHOLD -> {
                NotificationTemplate(
                    emailSubject = "Reminder: $certificationName expires in $daysUntilExpiration days",
                    emailBody = buildEmailBody(
                        urgency = "Reminder",
                        message = "Your $certificationName expires in $daysUntilExpiration days.",
                        daysRemaining = daysUntilExpiration
                    ),
                    smsMessage = "Your $certificationName expires in $daysUntilExpiration days.",
                    pushTitle = "Certification Reminder",
                    pushBody = "$certificationName expires in $daysUntilExpiration days"
                )
            }
            else -> {
                NotificationTemplate(
                    emailSubject = "Info: $certificationName expires in $daysUntilExpiration days",
                    emailBody = buildEmailBody(
                        urgency = "Information",
                        message = "Your $certificationName expires in $daysUntilExpiration days.",
                        daysRemaining = daysUntilExpiration
                    ),
                    smsMessage = "Your $certificationName expires in $daysUntilExpiration days.",
                    pushTitle = "Certification Status",
                    pushBody = "$certificationName expires in $daysUntilExpiration days"
                )
            }
        }
    }

    /**
     * Builds HTML email body with consistent formatting.
     */
    private fun buildEmailBody(
        urgency: String,
        message: String,
        daysRemaining: Int
    ): String {
        val urgencyColor = when (urgency) {
            "EXPIRED" -> "#d32f2f"
            "URGENT" -> "#f57c00"
            "Action Required" -> "#ffa000"
            else -> "#1976d2"
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .header { background-color: $urgencyColor; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; }
                    .footer { background-color: #f5f5f5; padding: 10px; text-align: center; font-size: 12px; }
                    .button {
                        display: inline-block;
                        padding: 10px 20px;
                        background-color: $urgencyColor;
                        color: white;
                        text-decoration: none;
                        border-radius: 5px;
                        margin-top: 15px;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>$urgency</h1>
                </div>
                <div class="content">
                    <h2>Certification Expiration Notice</h2>
                    <p>$message</p>
                    ${if (daysRemaining > 0) "<p><strong>Days Remaining:</strong> $daysRemaining</p>" else "<p><strong>Status:</strong> Expired</p>"}
                    <p>Please log in to HazardHawk to view details and take action.</p>
                    <a href="https://app.hazardhawk.com/certifications" class="button">View Certifications</a>
                </div>
                <div class="footer">
                    <p>This is an automated notification from HazardHawk.</p>
                    <p>If you have questions, please contact your safety administrator.</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Data class to hold notification template content for all channels.
     */
    private data class NotificationTemplate(
        val emailSubject: String,
        val emailBody: String,
        val smsMessage: String,
        val pushTitle: String,
        val pushBody: String
    )
}
