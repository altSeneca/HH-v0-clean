package com.hazardhawk.domain.services

import com.hazardhawk.models.crew.WorkerCertification

/**
 * Multi-channel notification service for critical alerts.
 * Supports Email, SMS, and Push notifications with automatic channel selection.
 */
interface NotificationService {
    /**
     * Sends certification expiration alert using appropriate channels based on urgency.
     *
     * Channel Selection Strategy:
     * - 90+ days: Email only (Blue - Information)
     * - 30 days: Email + SMS (Amber - Action Required)
     * - 7 days: Email + SMS + Push (Orange - URGENT)
     * - 0 days: Email + SMS + Push (Red - EXPIRED)
     *
     * @param workerId Worker's unique identifier
     * @param certification Certification about to expire
     * @param daysUntilExpiration Days until expiration (negative if expired)
     * @return Result with Unit on success, or error on failure
     */
    suspend fun sendCertificationExpirationAlert(
        workerId: String,
        certification: WorkerCertification,
        daysUntilExpiration: Int
    ): Result<Unit>

    /**
     * Sends an email notification.
     *
     * @param to Recipient email address
     * @param subject Email subject line
     * @param body Email body (supports HTML)
     * @return Result with Unit on success, or error on failure
     */
    suspend fun sendEmail(
        to: String,
        subject: String,
        body: String
    ): Result<Unit>

    /**
     * Sends an SMS notification.
     *
     * @param to Recipient phone number (E.164 format recommended)
     * @param message SMS message content (max 160 chars recommended)
     * @return Result with Unit on success, or error on failure
     */
    suspend fun sendSMS(
        to: String,
        message: String
    ): Result<Unit>

    /**
     * Sends a push notification to the user's device.
     *
     * @param userId User's unique identifier
     * @param title Notification title
     * @param body Notification body
     * @return Result with Unit on success, or error on failure
     */
    suspend fun sendPushNotification(
        userId: String,
        title: String,
        body: String
    ): Result<Unit>
}
