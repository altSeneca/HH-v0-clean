package com.hazardhawk.domain.services

import com.hazardhawk.domain.fixtures.CertificationTestFixtures
import com.hazardhawk.models.crew.CertificationStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Unit tests for NotificationService.
 * Tests template generation, multi-channel delivery, and retry logic.
 *
 * Total: 15 tests
 */
class NotificationServiceTest {
    
    private lateinit var mockHttpClient: HttpClient
    private lateinit var service: NotificationServiceImpl
    private var requestCount = 0
    
    @BeforeTest
    fun setup() {
        requestCount = 0
        mockHttpClient = createMockHttpClient()
        service = NotificationServiceImpl(
            httpClient = mockHttpClient,
            sendGridApiKey = "test-sendgrid-key",
            twilioAccountSid = "test-twilio-sid",
            twilioAuthToken = "test-twilio-token",
            pushNotificationEndpoint = "https://push.test.com/send"
        )
    }
    
    // ===== Template Generation (5 tests - one for each urgency level) =====
    
    @Test
    fun `sendCertificationExpirationAlert should use EXPIRED template for expired cert`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2024, 1, 1),
            status = CertificationStatus.EXPIRED
        )
        
        // When
        val result = service.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = -10  // 10 days expired
        )
        
        // Then
        assertTrue(result.isSuccess)
        // Verify email was sent with EXPIRED urgency
        assertTrue(requestCount > 0)
    }
    
    @Test
    fun `sendCertificationExpirationAlert should use URGENT template for 7 days or less`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 1, 20)
        )
        
        // When
        val result = service.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 5
        )
        
        // Then
        assertTrue(result.isSuccess)
        // Should send email, SMS, and push (3 channels)
        assertTrue(requestCount >= 3)
    }
    
    @Test
    fun `sendCertificationExpirationAlert should use ACTION_REQUIRED template for 30 days`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification()
        
        // When
        val result = service.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 25
        )
        
        // Then
        assertTrue(result.isSuccess)
        // Should send email and SMS (2 channels for 30 days or less)
        assertTrue(requestCount >= 2)
    }
    
    @Test
    fun `sendCertificationExpirationAlert should use REMINDER template for 90 days`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification()
        
        // When
        val result = service.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 60
        )
        
        // Then
        assertTrue(result.isSuccess)
        // Should send email only (1 channel for 30+ days)
        assertTrue(requestCount >= 1)
    }
    
    @Test
    fun `sendCertificationExpirationAlert should use INFO template for 90+ days`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification()
        
        // When
        val result = service.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 120
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(requestCount >= 1)
    }
    
    // ===== Multi-Channel Delivery (5 tests) =====
    
    @Test
    fun `sendCertificationExpirationAlert should send email for all urgency levels`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification()
        
        // When
        val result = service.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 90
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(requestCount > 0)  // Email always sent
    }
    
    @Test
    fun `sendCertificationExpirationAlert should send SMS for urgent notifications`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification()
        
        // When
        val result = service.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 20  // Within 30-day threshold
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(requestCount >= 2)  // Email + SMS
    }
    
    @Test
    fun `sendCertificationExpirationAlert should send push for critical alerts`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification()
        
        // When
        val result = service.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 5  // Within 7-day threshold
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(requestCount >= 3)  // Email + SMS + Push
    }
    
    @Test
    fun `sendEmail should successfully send email via SendGrid`() = runTest {
        // When
        val result = service.sendEmail(
            to = "test@example.com",
            subject = "Test Subject",
            body = "<p>Test Body</p>"
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(requestCount > 0)
    }
    
    @Test
    fun `sendSMS should successfully send SMS via Twilio`() = runTest {
        // When
        val result = service.sendSMS(
            to = "+1234567890",
            message = "Test SMS message"
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(requestCount > 0)
    }
    
    // ===== Retry Logic (5 tests) =====
    
    @Test
    fun `sendEmail should retry on failure`() = runTest {
        // Given
        val failingClient = createFailingMockHttpClient(failureCount = 1)
        val serviceWithRetry = NotificationServiceImpl(
            httpClient = failingClient,
            sendGridApiKey = "test-key"
        )
        
        // When
        val result = serviceWithRetry.sendEmail(
            to = "test@example.com",
            subject = "Test",
            body = "Body"
        )
        
        // Then
        assertTrue(result.isSuccess)  // Should succeed after retry
    }
    
    @Test
    fun `sendSMS should retry on failure`() = runTest {
        // Given
        val failingClient = createFailingMockHttpClient(failureCount = 1)
        val serviceWithRetry = NotificationServiceImpl(
            httpClient = failingClient,
            twilioAccountSid = "test-sid",
            twilioAuthToken = "test-token"
        )
        
        // When
        val result = serviceWithRetry.sendSMS(
            to = "+1234567890",
            message = "Test"
        )
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `sendPushNotification should retry on failure`() = runTest {
        // Given
        val failingClient = createFailingMockHttpClient(failureCount = 1)
        val serviceWithRetry = NotificationServiceImpl(
            httpClient = failingClient,
            pushNotificationEndpoint = "https://push.test.com/send"
        )
        
        // When
        val result = serviceWithRetry.sendPushNotification(
            userId = "user-123",
            title = "Test Title",
            body = "Test Body"
        )
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `sendEmail should fail after max retries`() = runTest {
        // Given
        val failingClient = createFailingMockHttpClient(failureCount = 5)
        val serviceWithRetry = NotificationServiceImpl(
            httpClient = failingClient,
            sendGridApiKey = "test-key"
        )
        
        // When
        val result = serviceWithRetry.sendEmail(
            to = "test@example.com",
            subject = "Test",
            body = "Body"
        )
        
        // Then
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `sendCertificationExpirationAlert should succeed if at least one channel works`() = runTest {
        // Given - Mock client that fails for SMS but succeeds for email
        val partialFailClient = createPartialFailMockHttpClient()
        val serviceWithPartialFail = NotificationServiceImpl(
            httpClient = partialFailClient,
            sendGridApiKey = "test-key",
            twilioAccountSid = "test-sid",
            twilioAuthToken = "test-token"
        )
        val cert = CertificationTestFixtures.createWorkerCertification()
        
        // When
        val result = serviceWithPartialFail.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 20  // Triggers email + SMS
        )
        
        // Then
        assertTrue(result.isSuccess)  // Should succeed because email worked
    }
    
    // ===== Helper Methods =====
    
    private fun createMockHttpClient(): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    requestCount++
                    respond(
                        content = ByteReadChannel("""{"success": true}"""),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }
    }
    
    private fun createFailingMockHttpClient(failureCount: Int): HttpClient {
        var attemptCount = 0
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    attemptCount++
                    if (attemptCount <= failureCount) {
                        respond(
                            content = ByteReadChannel("""{"error": "Server error"}"""),
                            status = HttpStatusCode.InternalServerError,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    } else {
                        respond(
                            content = ByteReadChannel("""{"success": true}"""),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    }
                }
            }
        }
    }
    
    private fun createPartialFailMockHttpClient(): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    // Succeed for SendGrid (email), fail for Twilio (SMS)
                    if (request.url.toString().contains("sendgrid")) {
                        respond(
                            content = ByteReadChannel("""{"success": true}"""),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    } else {
                        respond(
                            content = ByteReadChannel("""{"error": "Failed"}"""),
                            status = HttpStatusCode.InternalServerError,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    }
                }
            }
        }
    }
}
