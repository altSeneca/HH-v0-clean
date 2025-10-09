package com.hazardhawk.data.mocks

import kotlinx.coroutines.delay

/**
 * Mock notification client for testing email, SMS, and push notifications
 * 
 * Features:
 * - Simulates SendGrid email delivery
 * - Simulates Twilio SMS delivery
 * - Simulates FCM push notifications
 * - Tracks sent notifications
 */
class MockNotificationClient(
    private val config: MockNotificationConfig = MockNotificationConfig()
) {
    private val sentNotifications = mutableListOf<SentNotification>()
    
    var emailsSentCount = 0
        private set
    var smsSentCount = 0
        private set
    var pushSentCount = 0
        private set
    
    data class MockNotificationConfig(
        val emailDelayMs: Long = 100L,
        val smsDelayMs: Long = 50L,
        val pushDelayMs: Long = 30L,
        val shouldFailEmail: Boolean = false,
        val shouldFailSMS: Boolean = false,
        val shouldFailPush: Boolean = false,
        val failureRate: Double = 0.0
    )
    
    sealed class NotificationChannel {
        object Email : NotificationChannel()
        object SMS : NotificationChannel()
        object Push : NotificationChannel()
    }
    
    data class SentNotification(
        val channel: NotificationChannel,
        val recipient: String,
        val subject: String? = null,
        val message: String,
        val metadata: Map<String, String> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis(),
        val success: Boolean
    )
    
    /**
     * Send email notification
     */
    suspend fun sendEmail(
        to: String,
        subject: String,
        body: String,
        from: String = "noreply@hazardhawk.com",
        metadata: Map<String, String> = emptyMap()
    ): Result<Unit> {
        delay(config.emailDelayMs)
        
        if (config.shouldFailEmail) {
            val notification = SentNotification(
                channel = NotificationChannel.Email,
                recipient = to,
                subject = subject,
                message = body,
                metadata = metadata,
                success = false
            )
            sentNotifications.add(notification)
            return Result.failure(MockNotificationException("Email delivery failed (simulated)"))
        }
        
        emailsSentCount++
        val notification = SentNotification(
            channel = NotificationChannel.Email,
            recipient = to,
            subject = subject,
            message = body,
            metadata = metadata,
            success = true
        )
        sentNotifications.add(notification)
        
        return Result.success(Unit)
    }
    
    /**
     * Send SMS notification
     */
    suspend fun sendSMS(
        to: String,
        message: String,
        from: String = "+15551234567",
        metadata: Map<String, String> = emptyMap()
    ): Result<Unit> {
        delay(config.smsDelayMs)
        
        if (config.shouldFailSMS) {
            val notification = SentNotification(
                channel = NotificationChannel.SMS,
                recipient = to,
                message = message,
                metadata = metadata,
                success = false
            )
            sentNotifications.add(notification)
            return Result.failure(MockNotificationException("SMS delivery failed (simulated)"))
        }
        
        smsSentCount++
        val notification = SentNotification(
            channel = NotificationChannel.SMS,
            recipient = to,
            message = message,
            metadata = metadata,
            success = true
        )
        sentNotifications.add(notification)
        
        return Result.success(Unit)
    }
    
    /**
     * Send push notification
     */
    suspend fun sendPushNotification(
        userId: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Result<Unit> {
        delay(config.pushDelayMs)
        
        if (config.shouldFailPush) {
            val notification = SentNotification(
                channel = NotificationChannel.Push,
                recipient = userId,
                subject = title,
                message = body,
                metadata = data,
                success = false
            )
            sentNotifications.add(notification)
            return Result.failure(MockNotificationException("Push notification failed (simulated)"))
        }
        
        pushSentCount++
        val notification = SentNotification(
            channel = NotificationChannel.Push,
            recipient = userId,
            subject = title,
            message = body,
            metadata = data,
            success = true
        )
        sentNotifications.add(notification)
        
        return Result.success(Unit)
    }
    
    /**
     * Send certification expiration alert (multi-channel)
     */
    suspend fun sendCertificationExpirationAlert(
        workerId: String,
        workerEmail: String,
        workerPhone: String,
        certificationName: String,
        daysUntilExpiration: Int
    ): Result<Unit> {
        // Email for all alerts
        val emailResult = sendEmail(
            to = workerEmail,
            subject = "Certification Expiring Soon",
            body = "Your $certificationName expires in $daysUntilExpiration days."
        )
        
        if (emailResult.isFailure) {
            return emailResult
        }
        
        // SMS only for urgent alerts (7 days or less)
        if (daysUntilExpiration <= 7) {
            val smsResult = sendSMS(
                to = workerPhone,
                message = "Your $certificationName expires in $daysUntilExpiration days. Renew now."
            )
            
            if (smsResult.isFailure) {
                return smsResult
            }
        }
        
        // Push notification for all alerts
        val pushResult = sendPushNotification(
            userId = workerId,
            title = "Certification Expiring",
            body = "$certificationName expires in $daysUntilExpiration days",
            data = mapOf(
                "certificationType" to certificationName,
                "daysRemaining" to daysUntilExpiration.toString()
            )
        )
        
        return pushResult
    }
    
    /**
     * Get all sent notifications
     */
    fun getSentNotifications(): List<SentNotification> = sentNotifications.toList()
    
    /**
     * Get notifications by channel
     */
    fun getNotificationsByChannel(channel: NotificationChannel): List<SentNotification> {
        return sentNotifications.filter { it.channel == channel }
    }
    
    /**
     * Get notifications to a specific recipient
     */
    fun getNotificationsTo(recipient: String): List<SentNotification> {
        return sentNotifications.filter { it.recipient == recipient }
    }
    
    /**
     * Clear all history
     */
    fun clear() {
        sentNotifications.clear()
        emailsSentCount = 0
        smsSentCount = 0
        pushSentCount = 0
    }
    
    /**
     * Get delivery rate (successful / total)
     */
    fun getDeliveryRate(): Double {
        if (sentNotifications.isEmpty()) return 0.0
        val successful = sentNotifications.count { it.success }
        return successful.toDouble() / sentNotifications.size
    }
}

/**
 * Mock notification exception
 */
class MockNotificationException(message: String) : Exception(message)
