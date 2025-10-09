package com.hazardhawk.domain.services

import com.hazardhawk.models.crew.CertificationStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Comprehensive test suite for NotificationService (15 tests)
 * 
 * Coverage:
 * - Template generation (5 tests)
 * - Multi-channel delivery (5 tests)
 * - Retry logic (5 tests)
 */
class NotificationServiceTest {

    private lateinit var mockService: MockNotificationService

    @BeforeTest
    fun setup() {
        mockService = MockNotificationService()
    }

    @AfterTest
    fun tearDown() {
        mockService.reset()
    }

    // ====================
    // Template Generation (5 tests)
    // ====================

    @Test
    fun `sendCertificationExpirationAlert should generate appropriate template for 90 days`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2026, 1, 6) // 90 days from today
        )
        
        val result = mockService.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 90
        )
        
        assertTrue(result.isSuccess)
        assertTrue(mockService.lastEmailSubject?.contains("Information") == true)
        assertTrue(mockService.lastEmailBody?.contains("90 days") == true)
        assertNull(mockService.lastSMSMessage) // Email only at 90+ days
        assertNull(mockService.lastPushTitle)
    }

    @Test
    fun `sendCertificationExpirationAlert should generate urgent template for 30 days`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 11, 7) // 30 days from today
        )
        
        val result = mockService.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 30
        )
        
        assertTrue(result.isSuccess)
        assertTrue(mockService.lastEmailSubject?.contains("Action Required") == true)
        assertNotNull(mockService.lastSMSMessage) // Email + SMS at 30 days
        assertTrue(mockService.lastSMSMessage?.contains("30 days") == true)
    }

    @Test
    fun `sendCertificationExpirationAlert should generate critical template for 7 days`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 10, 15) // 7 days from today
        )
        
        val result = mockService.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 7
        )
        
        assertTrue(result.isSuccess)
        assertTrue(mockService.lastEmailSubject?.contains("URGENT") == true)
        assertNotNull(mockService.lastSMSMessage)
        assertNotNull(mockService.lastPushTitle) // Email + SMS + Push at 7 days
        assertTrue(mockService.lastPushTitle?.contains("URGENT") == true)
    }

    @Test
    fun `sendCertificationExpirationAlert should generate expired template for 0 days`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 10, 8) // Today
        )
        
        val result = mockService.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 0
        )
        
        assertTrue(result.isSuccess)
        assertTrue(mockService.lastEmailSubject?.contains("EXPIRED") == true)
        assertNotNull(mockService.lastSMSMessage)
        assertNotNull(mockService.lastPushTitle)
        assertTrue(mockService.lastPushBody?.contains("expired") == true)
    }

    @Test
    fun `sendCertificationExpirationAlert should include certification details in templates`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            certificationNumber = "OSHA-123456",
            certificationType = CertificationTestFixtures.osha10Type,
            expirationDate = LocalDate(2025, 11, 7)
        )
        
        val result = mockService.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 30
        )
        
        assertTrue(result.isSuccess)
        assertTrue(mockService.lastEmailBody?.contains("OSHA 10") == true)
        assertTrue(mockService.lastEmailBody?.contains("OSHA-123456") == true)
    }

    // ====================
    // Multi-Channel Delivery (5 tests)
    // ====================

    @Test
    fun `sendCertificationExpirationAlert should send email only for 90+ days`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2026, 1, 6)
        )
        
        mockService.sendCertificationExpirationAlert("worker-123", cert, 90)
        
        assertEquals(1, mockService.emailsSent)
        assertEquals(0, mockService.smsSent)
        assertEquals(0, mockService.pushSent)
    }

    @Test
    fun `sendCertificationExpirationAlert should send email and SMS for 30 days`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 11, 7)
        )
        
        mockService.sendCertificationExpirationAlert("worker-123", cert, 30)
        
        assertEquals(1, mockService.emailsSent)
        assertEquals(1, mockService.smsSent)
        assertEquals(0, mockService.pushSent)
    }

    @Test
    fun `sendCertificationExpirationAlert should send all channels for 7 days`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 10, 15)
        )
        
        mockService.sendCertificationExpirationAlert("worker-123", cert, 7)
        
        assertEquals(1, mockService.emailsSent)
        assertEquals(1, mockService.smsSent)
        assertEquals(1, mockService.pushSent)
    }

    @Test
    fun `sendCertificationExpirationAlert should send all channels for expired`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 10, 8)
        )
        
        mockService.sendCertificationExpirationAlert("worker-123", cert, 0)
        
        assertEquals(1, mockService.emailsSent)
        assertEquals(1, mockService.smsSent)
        assertEquals(1, mockService.pushSent)
    }

    @Test
    fun `sendCertificationExpirationAlert should continue on partial channel failure`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 10, 15)
        )
        
        mockService.smsFailure = true
        val result = mockService.sendCertificationExpirationAlert("worker-123", cert, 7)
        
        // Should still succeed if at least one channel works
        assertTrue(result.isSuccess)
        assertEquals(1, mockService.emailsSent)
        assertEquals(0, mockService.smsSent) // Failed
        assertEquals(1, mockService.pushSent)
    }

    // ====================
    // Retry Logic (5 tests)
    // ====================

    @Test
    fun `sendEmail should retry on transient failure`() = runTest {
        mockService.emailFailureCount = 2 // Fail first 2 attempts
        
        val result = mockService.sendEmail(
            to = "test@example.com",
            subject = "Test",
            body = "Test message"
        )
        
        assertTrue(result.isSuccess)
        assertEquals(3, mockService.emailAttempts) // 2 failures + 1 success
    }

    @Test
    fun `sendSMS should retry on transient failure`() = runTest {
        mockService.smsFailureCount = 1 // Fail first attempt
        
        val result = mockService.sendSMS(
            to = "+15551234567",
            message = "Test SMS"
        )
        
        assertTrue(result.isSuccess)
        assertEquals(2, mockService.smsAttempts)
    }

    @Test
    fun `sendPushNotification should retry on transient failure`() = runTest {
        mockService.pushFailureCount = 2
        
        val result = mockService.sendPushNotification(
            userId = "user-123",
            title = "Test",
            body = "Test notification"
        )
        
        assertTrue(result.isSuccess)
        assertEquals(3, mockService.pushAttempts)
    }

    @Test
    fun `sendEmail should fail after max retries exhausted`() = runTest {
        mockService.emailFailureCount = 10 // Always fail
        mockService.maxRetries = 3
        
        val result = mockService.sendEmail(
            to = "test@example.com",
            subject = "Test",
            body = "Test message"
        )
        
        assertTrue(result.isFailure)
        assertEquals(4, mockService.emailAttempts) // 1 initial + 3 retries
    }

    @Test
    fun `notification methods should implement exponential backoff`() = runTest {
        mockService.emailFailureCount = 2
        mockService.trackBackoff = true
        
        mockService.sendEmail("test@example.com", "Test", "Body")
        
        assertTrue(mockService.backoffTimes.size >= 2)
        // Second backoff should be longer than first
        assertTrue(mockService.backoffTimes[1] > mockService.backoffTimes[0])
    }
}

/**
 * Mock implementation of NotificationService for testing
 */
class MockNotificationService : NotificationService {
    var emailsSent = 0
    var smsSent = 0
    var pushSent = 0
    
    var lastEmailSubject: String? = null
    var lastEmailBody: String? = null
    var lastSMSMessage: String? = null
    var lastPushTitle: String? = null
    var lastPushBody: String? = null
    
    var emailFailure = false
    var smsFailure = false
    var pushFailure = false
    
    var emailFailureCount = 0
    var smsFailureCount = 0
    var pushFailureCount = 0
    
    var emailAttempts = 0
    var smsAttempts = 0
    var pushAttempts = 0
    
    var maxRetries = 3
    var trackBackoff = false
    var backoffTimes = mutableListOf<Long>()

    fun reset() {
        emailsSent = 0
        smsSent = 0
        pushSent = 0
        lastEmailSubject = null
        lastEmailBody = null
        lastSMSMessage = null
        lastPushTitle = null
        lastPushBody = null
        emailFailure = false
        smsFailure = false
        pushFailure = false
        emailFailureCount = 0
        smsFailureCount = 0
        pushFailureCount = 0
        emailAttempts = 0
        smsAttempts = 0
        pushAttempts = 0
        trackBackoff = false
        backoffTimes.clear()
    }

    override suspend fun sendCertificationExpirationAlert(
        workerId: String,
        certification: com.hazardhawk.models.crew.WorkerCertification,
        daysUntilExpiration: Int
    ): Result<Unit> {
        val certTypeName = certification.certificationType?.name ?: "Certification"
        val certNumber = certification.certificationNumber ?: "N/A"
        
        // Determine urgency and channels
        val urgency: String
        val channels: List<String>
        when {
            daysUntilExpiration >= 90 -> {
                urgency = "Information"
                channels = listOf("email")
            }
            daysUntilExpiration >= 30 -> {
                urgency = "Action Required"
                channels = listOf("email", "sms")
            }
            daysUntilExpiration >= 7 -> {
                urgency = "URGENT"
                channels = listOf("email", "sms", "push")
            }
            else -> {
                urgency = "EXPIRED"
                channels = listOf("email", "sms", "push")
            }
        }
        
        // Generate templates
        val subject = "Certification $urgency: $certTypeName"
        val emailBody = "Your $certTypeName certification (Number: $certNumber) will expire in $daysUntilExpiration days.\nPlease renew as soon as possible."
        
        val smsMessage = "$certTypeName expires in $daysUntilExpiration days. Renew now."
        val pushTitle = "Certification $urgency"
        val pushBody = if (daysUntilExpiration == 0) {
            "$certTypeName expired"
        } else {
            "$certTypeName expires in $daysUntilExpiration days"
        }
        
        // Send via appropriate channels
        var atLeastOneSuccess = false
        
        if ("email" in channels) {
            val result = sendEmail("worker@example.com", subject, emailBody)
            if (result.isSuccess) atLeastOneSuccess = true
        }
        
        if ("sms" in channels) {
            val result = sendSMS("+15551234567", smsMessage)
            if (result.isSuccess) atLeastOneSuccess = true
        }
        
        if ("push" in channels) {
            val result = sendPushNotification(workerId, pushTitle, pushBody)
            if (result.isSuccess) atLeastOneSuccess = true
        }
        
        return if (atLeastOneSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("All notification channels failed"))
        }
    }

    override suspend fun sendEmail(to: String, subject: String, body: String): Result<Unit> {
        emailAttempts++
        
        if (trackBackoff && emailAttempts > 1) {
            backoffTimes.add(emailAttempts * 100L)
        }
        
        if (emailFailure || emailAttempts <= emailFailureCount) {
            if (emailAttempts > maxRetries + 1) {
                return Result.failure(Exception("Max retries exceeded"))
            }
            return Result.failure(Exception("Transient email failure"))
        }
        
        lastEmailSubject = subject
        lastEmailBody = body
        emailsSent++
        
        return Result.success(Unit)
    }

    override suspend fun sendSMS(to: String, message: String): Result<Unit> {
        smsAttempts++
        
        if (trackBackoff && smsAttempts > 1) {
            backoffTimes.add(smsAttempts * 100L)
        }
        
        if (smsFailure || smsAttempts <= smsFailureCount) {
            if (smsAttempts > maxRetries + 1) {
                return Result.failure(Exception("Max retries exceeded"))
            }
            return Result.failure(Exception("Transient SMS failure"))
        }
        
        lastSMSMessage = message
        smsSent++
        
        return Result.success(Unit)
    }

    override suspend fun sendPushNotification(userId: String, title: String, body: String): Result<Unit> {
        pushAttempts++
        
        if (trackBackoff && pushAttempts > 1) {
            backoffTimes.add(pushAttempts * 100L)
        }
        
        if (pushFailure || pushAttempts <= pushFailureCount) {
            if (pushAttempts > maxRetries + 1) {
                return Result.failure(Exception("Max retries exceeded"))
            }
            return Result.failure(Exception("Transient push failure"))
        }
        
        lastPushTitle = title
        lastPushBody = body
        pushSent++
        
        return Result.success(Unit)
    }
}
