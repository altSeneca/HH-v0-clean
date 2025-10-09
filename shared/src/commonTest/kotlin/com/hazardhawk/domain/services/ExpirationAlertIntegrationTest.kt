package com.hazardhawk.domain.services

import com.hazardhawk.domain.fixtures.CertificationTestFixtures
import com.hazardhawk.models.crew.CertificationStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.*

/**
 * Integration tests for certification expiration alert workflow.
 * Tests alert delivery at each threshold and multi-worker scenarios.
 *
 * Total: 10 tests
 */
class ExpirationAlertIntegrationTest {
    
    private lateinit var notificationService: NotificationServiceImpl
    private lateinit var mockHttpClient: HttpClient
    private var emailsSent = 0
    private var smsSent = 0
    private var pushSent = 0
    
    @BeforeTest
    fun setup() {
        emailsSent = 0
        smsSent = 0
        pushSent = 0
        
        mockHttpClient = createTrackingMockHttpClient()
        notificationService = NotificationServiceImpl(
            httpClient = mockHttpClient,
            sendGridApiKey = "test-key",
            twilioAccountSid = "test-sid",
            twilioAuthToken = "test-token",
            pushNotificationEndpoint = "https://push.test.com/send"
        )
    }
    
    // ===== Alert Delivery at Each Threshold (7 tests) =====
    
    @Test
    fun `should send alerts for certifications expiring in 90 days`() = runTest {
        // Given
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val expirationDate = LocalDate(today.year, today.monthNumber, today.dayOfMonth).let {
            LocalDate(it.year, it.monthNumber, it.dayOfMonth + 90)
        }
        
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = expirationDate,
            status = CertificationStatus.VERIFIED
        )
        
        // When
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 90
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, emailsSent)  // Email only at 90 days
        assertEquals(0, smsSent)
        assertEquals(0, pushSent)
    }
    
    @Test
    fun `should send alerts for certifications expiring in 60 days`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification()
        
        // When
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 60
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, emailsSent)
        assertEquals(0, smsSent)  // No SMS at 60 days (threshold is 30)
    }
    
    @Test
    fun `should send email and SMS for certifications expiring in 30 days`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification()
        
        // When
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 30
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, emailsSent)
        assertEquals(1, smsSent)  // SMS sent at 30-day threshold
        assertEquals(0, pushSent)
    }
    
    @Test
    fun `should send email and SMS for certifications expiring in 14 days`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification()
        
        // When
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 14
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, emailsSent)
        assertEquals(1, smsSent)
        assertEquals(0, pushSent)  // No push at 14 days (threshold is 7)
    }
    
    @Test
    fun `should send all channels for certifications expiring in 7 days`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification()
        
        // When
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 7
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, emailsSent)
        assertEquals(1, smsSent)
        assertEquals(1, pushSent)  // All channels at 7-day threshold
    }
    
    @Test
    fun `should send all channels for certifications expiring in 3 days`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification()
        
        // When
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 3
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, emailsSent)
        assertEquals(1, smsSent)
        assertEquals(1, pushSent)
    }
    
    @Test
    fun `should send urgent alerts for expired certifications`() = runTest {
        // Given
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2024, 1, 1),
            status = CertificationStatus.EXPIRED
        )
        
        // When
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = -5  // 5 days past expiration
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, emailsSent)
        assertEquals(1, smsSent)
        assertEquals(1, pushSent)  // All channels for expired certs
    }
    
    // ===== Multi-Worker Scenarios (3 tests) =====
    
    @Test
    fun `should send alerts to multiple workers with expiring certifications`() = runTest {
        // Given
        val workers = listOf(
            "worker-1" to 90,
            "worker-2" to 30,
            "worker-3" to 7
        )
        
        // When
        val results = workers.map { (workerId, daysUntilExpiration) ->
            val cert = CertificationTestFixtures.createWorkerCertification()
            notificationService.sendCertificationExpirationAlert(
                workerId = workerId,
                certification = cert,
                daysUntilExpiration = daysUntilExpiration
            )
        }
        
        // Then
        assertTrue(results.all { it.isSuccess })
        assertEquals(3, emailsSent)  // All workers get email
        assertEquals(2, smsSent)     // Workers at 30 and 7 days
        assertEquals(1, pushSent)    // Worker at 7 days
    }
    
    @Test
    fun `should handle workers with multiple expiring certifications`() = runTest {
        // Given
        val workerId = "worker-123"
        val certifications = listOf(
            CertificationTestFixtures.createWorkerCertification(
                id = "cert-1",
                certificationType = CertificationTestFixtures.createCertificationType(
                    code = CertificationTypeCodes.OSHA_10
                )
            ),
            CertificationTestFixtures.createWorkerCertification(
                id = "cert-2",
                certificationType = CertificationTestFixtures.createCertificationType(
                    code = CertificationTypeCodes.FORKLIFT
                )
            ),
            CertificationTestFixtures.createWorkerCertification(
                id = "cert-3",
                certificationType = CertificationTestFixtures.createCertificationType(
                    code = CertificationTypeCodes.FIRST_AID
                )
            )
        )
        
        val daysUntilExpiration = listOf(30, 14, 7)
        
        // When
        val results = certifications.zip(daysUntilExpiration).map { (cert, days) ->
            notificationService.sendCertificationExpirationAlert(
                workerId = workerId,
                certification = cert,
                daysUntilExpiration = days
            )
        }
        
        // Then
        assertTrue(results.all { it.isSuccess })
        assertEquals(3, emailsSent)  // One email per certification
    }
    
    @Test
    fun `should batch process expiration checks for entire company`() = runTest {
        // Given
        val companyWorkers = (1..10).map { workerId ->
            Triple(
                "worker-$workerId",
                CertificationTestFixtures.createWorkerCertification(
                    id = "cert-$workerId"
                ),
                when (workerId % 4) {
                    0 -> 90
                    1 -> 30
                    2 -> 7
                    else -> 3
                }
            )
        }
        
        // When
        val results = companyWorkers.map { (workerId, cert, days) ->
            notificationService.sendCertificationExpirationAlert(
                workerId = workerId,
                certification = cert,
                daysUntilExpiration = days
            )
        }
        
        // Then
        assertTrue(results.all { it.isSuccess })
        assertEquals(10, emailsSent)  // All workers get email
        // SMS sent for 30, 7, and 3 day thresholds: 7-8 workers
        assertTrue(smsSent >= 7)
        // Push sent for 7 and 3 day thresholds: 5 workers
        assertTrue(pushSent >= 5)
    }
    
    // ===== Helper Methods =====
    
    private fun createTrackingMockHttpClient(): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val url = request.url.toString()
                    when {
                        url.contains("sendgrid") -> emailsSent++
                        url.contains("twilio") -> smsSent++
                        url.contains("push") -> pushSent++
                    }
                    
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
