package com.hazardhawk.domain.services

import com.hazardhawk.core.models.crew.CertificationStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Integration test suite for certification expiration alerts (10 tests)
 * Tests the complete alert workflow at different expiration thresholds
 * 
 * Coverage:
 * - Alert thresholds (7 tests: 90, 60, 30, 14, 7, 3, 0 days)
 * - Multi-worker scenarios (3 tests)
 */
class ExpirationAlertIntegrationTest {

    private lateinit var notificationService: MockNotificationService

    @BeforeTest
    fun setup() {
        notificationService = MockNotificationService()
    }

    @AfterTest
    fun tearDown() {
        notificationService.reset()
    }

    // ====================
    // Alert Thresholds (7 tests)
    // ====================

    @Test
    fun `alert at 90 days should send email only`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2026, 1, 6), // 90 days from today (2025-10-08)
            status = CertificationStatus.VERIFIED
        )
        
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-123",
            certification = cert,
            daysUntilExpiration = 90
        )
        
        assertTrue(result.isSuccess)
        assertEquals(1, notificationService.emailsSent)
        assertEquals(0, notificationService.smsSent)
        assertEquals(0, notificationService.pushSent)
        
        // Verify content
        assertTrue(notificationService.lastEmailSubject?.contains("Information") == true)
        assertTrue(notificationService.lastEmailBody?.contains("90 days") == true)
    }

    @Test
    fun `alert at 60 days should send email only`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 12, 7), // 60 days from today
            status = CertificationStatus.VERIFIED
        )
        
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-456",
            certification = cert,
            daysUntilExpiration = 60
        )
        
        assertTrue(result.isSuccess)
        assertEquals(1, notificationService.emailsSent)
        assertEquals(0, notificationService.smsSent)
        assertEquals(0, notificationService.pushSent)
    }

    @Test
    fun `alert at 30 days should send email and SMS`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 11, 7), // 30 days from today
            status = CertificationStatus.VERIFIED
        )
        
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-789",
            certification = cert,
            daysUntilExpiration = 30
        )
        
        assertTrue(result.isSuccess)
        assertEquals(1, notificationService.emailsSent)
        assertEquals(1, notificationService.smsSent)
        assertEquals(0, notificationService.pushSent)
        
        // Verify urgency level
        assertTrue(notificationService.lastEmailSubject?.contains("Action Required") == true)
        assertTrue(notificationService.lastSMSMessage?.contains("30 days") == true)
    }

    @Test
    fun `alert at 14 days should send email and SMS`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 10, 22), // 14 days from today
            status = CertificationStatus.VERIFIED
        )
        
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-101",
            certification = cert,
            daysUntilExpiration = 14
        )
        
        assertTrue(result.isSuccess)
        assertEquals(1, notificationService.emailsSent)
        assertEquals(1, notificationService.smsSent)
        assertEquals(0, notificationService.pushSent)
    }

    @Test
    fun `alert at 7 days should send email, SMS, and push`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 10, 15), // 7 days from today
            status = CertificationStatus.VERIFIED
        )
        
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-202",
            certification = cert,
            daysUntilExpiration = 7
        )
        
        assertTrue(result.isSuccess)
        assertEquals(1, notificationService.emailsSent)
        assertEquals(1, notificationService.smsSent)
        assertEquals(1, notificationService.pushSent)
        
        // Verify critical urgency
        assertTrue(notificationService.lastEmailSubject?.contains("URGENT") == true)
        assertTrue(notificationService.lastPushTitle?.contains("URGENT") == true)
    }

    @Test
    fun `alert at 3 days should send all channels with high urgency`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 10, 11), // 3 days from today
            status = CertificationStatus.VERIFIED
        )
        
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-303",
            certification = cert,
            daysUntilExpiration = 3
        )
        
        assertTrue(result.isSuccess)
        assertEquals(1, notificationService.emailsSent)
        assertEquals(1, notificationService.smsSent)
        assertEquals(1, notificationService.pushSent)
        
        // Should have maximum urgency messaging
        assertTrue(notificationService.lastEmailSubject?.contains("URGENT") == true)
    }

    @Test
    fun `alert at 0 days (expired) should send all channels with expired status`() = runTest {
        val cert = CertificationTestFixtures.createWorkerCertification(
            expirationDate = LocalDate(2025, 10, 8), // Today - expired
            status = CertificationStatus.VERIFIED
        )
        
        val result = notificationService.sendCertificationExpirationAlert(
            workerId = "worker-404",
            certification = cert,
            daysUntilExpiration = 0
        )
        
        assertTrue(result.isSuccess)
        assertEquals(1, notificationService.emailsSent)
        assertEquals(1, notificationService.smsSent)
        assertEquals(1, notificationService.pushSent)
        
        // Verify expired messaging
        assertTrue(notificationService.lastEmailSubject?.contains("EXPIRED") == true)
        assertTrue(notificationService.lastPushBody?.contains("expired") == true)
    }

    // ====================
    // Multi-Worker Scenarios (3 tests)
    // ====================

    @Test
    fun `should send alerts to multiple workers with different expiration dates`() = runTest {
        val workers = listOf(
            Triple("worker-1", LocalDate(2026, 1, 6), 90),   // 90 days
            Triple("worker-2", LocalDate(2025, 11, 7), 30),  // 30 days
            Triple("worker-3", LocalDate(2025, 10, 15), 7)   // 7 days
        )
        
        workers.forEach { (workerId, expirationDate, daysUntil) ->
            notificationService.reset()
            
            val cert = CertificationTestFixtures.createWorkerCertification(
                expirationDate = expirationDate,
                status = CertificationStatus.VERIFIED
            )
            
            val result = notificationService.sendCertificationExpirationAlert(
                workerId = workerId,
                certification = cert,
                daysUntilExpiration = daysUntil
            )
            
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun `should handle worker with multiple expiring certifications`() = runTest {
        val workerId = "worker-multi"
        
        val certifications = listOf(
            CertificationTestFixtures.createWorkerCertification(
                certificationType = CertificationTestFixtures.osha10Type,
                expirationDate = LocalDate(2025, 10, 15) // 7 days
            ),
            CertificationTestFixtures.createWorkerCertification(
                certificationType = CertificationTestFixtures.forkliftType,
                expirationDate = LocalDate(2025, 11, 7) // 30 days
            ),
            CertificationTestFixtures.createWorkerCertification(
                certificationType = CertificationTestFixtures.firstAidType,
                expirationDate = LocalDate(2026, 1, 6) // 90 days
            )
        )
        
        // Send alerts for each
        certifications.forEachIndexed { index, cert ->
            notificationService.reset()
            
            val daysUntil = when(index) {
                0 -> 7
                1 -> 30
                2 -> 90
                else -> 0
            }
            
            val result = notificationService.sendCertificationExpirationAlert(
                workerId = workerId,
                certification = cert,
                daysUntilExpiration = daysUntil
            )
            
            assertTrue(result.isSuccess)
            
            // Verify correct certification type in message
            assertTrue(
                notificationService.lastEmailBody?.contains(cert.certificationType?.name ?: "") == true
            )
        }
    }

    @Test
    fun `should batch process expiration checks for all workers efficiently`() = runTest {
        // Simulate daily expiration check for 50 workers
        val workers = (1..50).map { id ->
            val daysUntil = (id % 90) + 1 // Distribute across 1-90 days
            val expirationDate = LocalDate(2025, 10, 8).plus(kotlinx.datetime.DatePeriod(days = daysUntil))
            
            Triple(
                "worker-$id",
                CertificationTestFixtures.createWorkerCertification(
                    expirationDate = expirationDate,
                    status = CertificationStatus.VERIFIED
                ),
                daysUntil
            )
        }
        
        // Process all workers
        var successCount = 0
        workers.forEach { (workerId, cert, daysUntil) ->
            notificationService.reset()
            
            val result = notificationService.sendCertificationExpirationAlert(
                workerId = workerId,
                certification = cert,
                daysUntilExpiration = daysUntil
            )
            
            if (result.isSuccess) {
                successCount++
            }
        }
        
        // All should succeed
        assertEquals(50, successCount)
    }
}
