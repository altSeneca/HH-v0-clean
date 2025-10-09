package com.hazardhawk.domain.services

import com.hazardhawk.models.crew.WorkerCertification

/**
 * Service for sending multi-channel notifications.
 * Supports email, SMS, and push notifications with retry logic.
 */
interface NotificationService {
    /**
     * Sends a certification expiration alert to the worker.
     * Automatically selects appropriate template based on days until expiration.
     *
     * @param workerId The ID of the worker to notify
     * @param certification The certification that is expiring
     * @param daysUntilExpiration Number of days until expiration (can be negative if expired)
     * @return Result indicating success or failure
     */
    suspend fun sendCertificationExpirationAlert(
        workerId: String,
        certification: WorkerCertification,
        daysUntilExpiration: Int
    ): Result<Unit>

    /**
     * Sends an email notification.
     * Includes automatic retry logic (3 attempts) for failed deliveries.
     *
     * @param to Recipient email address
     * @param subject Email subject line
     * @param body Email body content (supports HTML)
     * @return Result indicating success or failure
     */
    suspend fun sendEmail(
        to: String,
        subject: String,
        body: String
    ): Result<Unit>

    /**
     * Sends an SMS notification.
     * Includes automatic retry logic (3 attempts) for failed deliveries.
     *
     * @param to Recipient phone number (E.164 format)
     * @param message SMS message content
     * @return Result indicating success or failure
     */
    suspend fun sendSMS(
        to: String,
        message: String
    ): Result<Unit>

    /**
     * Sends a push notification.
     * Includes automatic retry logic (3 attempts) for failed deliveries.
     *
     * @param userId The user ID to send the notification to
     * @param title Notification title
     * @param body Notification body text
     * @return Result indicating success or failure
     */
    suspend fun sendPushNotification(
        userId: String,
        title: String,
        body: String
    ): Result<Unit>
}
