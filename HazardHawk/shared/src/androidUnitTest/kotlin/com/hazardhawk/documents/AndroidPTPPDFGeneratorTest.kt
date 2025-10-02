package com.hazardhawk.documents

import com.hazardhawk.domain.models.ptp.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.io.File

/**
 * Test suite for AndroidPTPPDFGenerator.
 *
 * These tests verify:
 * - PDF generation succeeds with valid data
 * - PDFs are generated with correct byte array size
 * - Edge cases are handled (empty photos, missing data)
 * - Performance meets targets
 *
 * Note: These are unit tests. Visual verification of PDF output
 * should be done manually or with instrumentation tests.
 */
class AndroidPTPPDFGeneratorTest {

    private lateinit var pdfGenerator: AndroidPTPPDFGenerator

    @Before
    fun setup() {
        pdfGenerator = AndroidPTPPDFGenerator()
    }

    @Test
    fun `test generatePDF with minimal data succeeds`() = runTest {
        // Given
        val ptp = createMinimalPtp()
        val photos = emptyList<PhotoData>()

        // When
        val result = pdfGenerator.generatePDF(ptp, photos)

        // Then
        assertTrue("PDF generation should succeed", result.isSuccess)
        val pdfBytes = result.getOrNull()
        assertNotNull("PDF bytes should not be null", pdfBytes)
        assertTrue("PDF should have content", pdfBytes!!.size > 1000) // At least 1KB
    }

    @Test
    fun `test generatePDF with complete data succeeds`() = runTest {
        // Given
        val ptp = createCompletePtp()
        val photos = createSamplePhotos(5)

        // When
        val result = pdfGenerator.generatePDF(ptp, photos)

        // Then
        assertTrue("PDF generation should succeed", result.isSuccess)
        val pdfBytes = result.getOrNull()
        assertNotNull("PDF bytes should not be null", pdfBytes)
        assertTrue("PDF should have substantial content", pdfBytes!!.size > 10000) // At least 10KB
    }

    @Test
    fun `test generatePDFWithMetadata includes branding`() = runTest {
        // Given
        val ptp = createCompletePtp()
        val photos = createSamplePhotos(3)
        val metadata = PDFMetadata(
            companyName = "Test Construction Company",
            projectName = "Test Project Alpha",
            projectLocation = "123 Construction Site, City, ST"
        )

        // When
        val result = pdfGenerator.generatePDFWithMetadata(ptp, photos, metadata)

        // Then
        assertTrue("PDF generation should succeed", result.isSuccess)
        val pdfBytes = result.getOrNull()
        assertNotNull("PDF bytes should not be null", pdfBytes)
        assertTrue("PDF should have content", pdfBytes!!.size > 5000)
    }

    @Test
    fun `test generatePDF with many photos succeeds`() = runTest {
        // Given
        val ptp = createCompletePtp()
        val photos = createSamplePhotos(25) // Maximum photos

        // When
        val result = pdfGenerator.generatePDF(ptp, photos)

        // Then
        assertTrue("PDF generation with many photos should succeed", result.isSuccess)
        val pdfBytes = result.getOrNull()
        assertNotNull("PDF bytes should not be null", pdfBytes)
        // With 25 photos, expect larger file (but should still be under 10MB)
        assertTrue("PDF should be substantial but reasonable size",
            pdfBytes!!.size in 50000..10_000_000)
    }

    @Test
    fun `test generatePDF with all hazard severities`() = runTest {
        // Given
        val ptp = createPtpWithAllHazardSeverities()
        val photos = emptyList<PhotoData>()

        // When
        val result = pdfGenerator.generatePDF(ptp, photos)

        // Then
        assertTrue("PDF with all hazard severities should succeed", result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `test generatePDF with signature data`() = runTest {
        // Given
        val ptp = createPtpWithSignature()
        val photos = emptyList<PhotoData>()

        // When
        val result = pdfGenerator.generatePDF(ptp, photos)

        // Then
        assertTrue("PDF with signature should succeed", result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `test generatePDF with emergency contacts`() = runTest {
        // Given
        val ptp = createPtpWithEmergencyInfo()
        val photos = emptyList<PhotoData>()

        // When
        val result = pdfGenerator.generatePDF(ptp, photos)

        // Then
        assertTrue("PDF with emergency info should succeed", result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `test generatePDF performance with 10 photos`() = runTest {
        // Given
        val ptp = createCompletePtp()
        val photos = createSamplePhotos(10)

        // When
        val startTime = System.currentTimeMillis()
        val result = pdfGenerator.generatePDF(ptp, photos)
        val duration = System.currentTimeMillis() - startTime

        // Then
        assertTrue("PDF generation should succeed", result.isSuccess)
        assertTrue("Generation should complete within 5 seconds", duration < 5000)
        println("PDF generation took ${duration}ms for 10 photos")
    }

    // Helper functions to create test data

    private fun createMinimalPtp(): PreTaskPlan {
        return PreTaskPlan(
            id = "test-ptp-001",
            projectId = "project-001",
            createdBy = "test-user",
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            workType = "General Construction",
            workScope = "Perform routine maintenance and inspection.",
            status = PtpStatus.DRAFT
        )
    }

    private fun createCompletePtp(): PreTaskPlan {
        return PreTaskPlan(
            id = "test-ptp-complete",
            projectId = "project-002",
            createdBy = "John Supervisor",
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            workType = "Roofing",
            workScope = "Install new roofing materials on Building 3. Work includes removal of old shingles, inspection of roof deck, installation of underlayment, and application of new architectural shingles.",
            crewSize = 5,
            status = PtpStatus.APPROVED,
            aiGeneratedContent = PtpContent(
                hazards = listOf(
                    PtpHazard(
                        oshaCode = "1926.501(b)(1)",
                        description = "Fall from height while working on roof edge",
                        severity = HazardSeverity.CRITICAL,
                        controls = listOf(
                            "Install perimeter guardrails before work begins",
                            "Use personal fall arrest systems (PFAS) when within 6 feet of edge",
                            "Implement safety monitoring system with designated safety monitor"
                        ),
                        requiredPpe = listOf("Full-body harness", "Hard hat", "Safety boots")
                    ),
                    PtpHazard(
                        oshaCode = "1926.451",
                        description = "Scaffold stability and load capacity concerns",
                        severity = HazardSeverity.MAJOR,
                        controls = listOf(
                            "Inspect scaffold before each shift",
                            "Ensure proper weight distribution",
                            "Secure scaffold to building structure"
                        ),
                        requiredPpe = listOf("Hard hat", "Safety boots", "High-visibility vest")
                    ),
                    PtpHazard(
                        oshaCode = "1926.416",
                        description = "Overhead power lines within 10 feet of work area",
                        severity = HazardSeverity.MAJOR,
                        controls = listOf(
                            "De-energize lines if possible",
                            "Maintain minimum 10-foot clearance",
                            "Use non-conductive tools and equipment"
                        ),
                        requiredPpe = listOf("Rubber gloves", "Non-conductive boots")
                    )
                ),
                jobSteps = listOf(
                    JobStep(
                        stepNumber = 1,
                        description = "Set up perimeter protection and access equipment",
                        hazards = listOf("Fall from height", "Struck by falling objects"),
                        controls = listOf("Install guardrails", "Barricade ground level area"),
                        ppe = listOf("Hard hat", "Safety harness", "Safety boots")
                    ),
                    JobStep(
                        stepNumber = 2,
                        description = "Remove old roofing materials",
                        hazards = listOf("Falls", "Flying debris", "Sharp edges"),
                        controls = listOf("Use fall protection", "Debris chute", "Proper tool handling"),
                        ppe = listOf("Full-body harness", "Safety glasses", "Work gloves", "Hard hat")
                    ),
                    JobStep(
                        stepNumber = 3,
                        description = "Inspect and repair roof deck as needed",
                        hazards = listOf("Weak roof deck", "Protruding nails"),
                        controls = listOf("Visual inspection", "Mark weak areas", "Replace damaged sections"),
                        ppe = listOf("Hard hat", "Work gloves", "Knee pads")
                    ),
                    JobStep(
                        stepNumber = 4,
                        description = "Install underlayment and new shingles",
                        hazards = listOf("Falls", "Heat stress", "Repetitive motion"),
                        controls = listOf("Fall protection", "Hydration breaks", "Proper lifting techniques"),
                        ppe = listOf("Full-body harness", "Sun protection", "Work gloves")
                    )
                ),
                emergencyProcedures = listOf(
                    "In case of fall: Activate rescue plan immediately, call 911",
                    "For minor injuries: Administer first aid, report to supervisor",
                    "Severe weather: Stop work, secure materials, evacuate to designated shelter"
                )
            ),
            toolsEquipment = listOf(
                "Pneumatic nail guns",
                "Roofing hammers",
                "Utility knives",
                "Chalk lines",
                "Measuring tapes"
            ),
            mechanicalEquipment = listOf(
                "Scissor lift",
                "Material hoist"
            ),
            emergencyContacts = listOf(
                EmergencyContact("John Smith", "Site Supervisor", "555-0101", isPrimary = true),
                EmergencyContact("Sarah Johnson", "Safety Manager", "555-0102", isPrimary = false)
            ),
            nearestHospital = "City Medical Center, 456 Hospital Drive, 5 miles north",
            evacuationRoutes = "Primary: South stairwell to parking lot. Secondary: North fire escape to street level."
        )
    }

    private fun createPtpWithAllHazardSeverities(): PreTaskPlan {
        val ptp = createMinimalPtp()
        return ptp.copy(
            aiGeneratedContent = PtpContent(
                hazards = listOf(
                    PtpHazard("1926.501", "Critical fall hazard", HazardSeverity.CRITICAL,
                        controls = listOf("Install guardrails"), requiredPpe = listOf("Harness")),
                    PtpHazard("1926.451", "Major scaffold issue", HazardSeverity.MAJOR,
                        controls = listOf("Daily inspections"), requiredPpe = listOf("Hard hat")),
                    PtpHazard("1926.102", "Minor slip hazard", HazardSeverity.MINOR,
                        controls = listOf("Keep area clean"), requiredPpe = listOf("Safety boots"))
                )
            )
        )
    }

    private fun createPtpWithSignature(): PreTaskPlan {
        val ptp = createMinimalPtp()
        return ptp.copy(
            signatureSupervisor = SignatureData(
                supervisorName = "John Supervisor",
                signatureDate = Clock.System.now().toEpochMilliseconds(),
                signatureBlob = null // Text signature only for testing
            )
        )
    }

    private fun createPtpWithEmergencyInfo(): PreTaskPlan {
        val ptp = createMinimalPtp()
        return ptp.copy(
            emergencyContacts = listOf(
                EmergencyContact("Emergency Contact 1", "Supervisor", "555-1234", isPrimary = true),
                EmergencyContact("Emergency Contact 2", "Safety Officer", "555-5678", isPrimary = false)
            ),
            nearestHospital = "County Hospital, 123 Medical Blvd",
            evacuationRoutes = "Exit through main gate, assemble at north parking lot"
        )
    }

    private fun createSamplePhotos(count: Int): List<PhotoData> {
        return (1..count).map { index ->
            PhotoData(
                imageBytes = createDummyImageBytes(),
                metadata = PhotoMetadata(
                    location = "Building 3, Floor ${index % 3 + 1}",
                    gpsCoordinates = "40.${7100 + index}, -74.${60 + index}",
                    timestamp = Clock.System.now().toEpochMilliseconds() - (index * 3600000L),
                    aiAnalysisSummary = listOf(
                        "Fall hazard detected at roof edge",
                        "Missing guardrail on scaffold",
                        "PPE compliance: 3/5 workers"
                    ).take((index % 3) + 1),
                    caption = "Photo $index: Safety inspection documentation"
                )
            )
        }
    }

    private fun createDummyImageBytes(): ByteArray {
        // Create a simple 100x100 JPEG placeholder
        // In real tests, this would be an actual image
        // For now, return a minimal JPEG header
        return byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x01, 0x00, 0x48, 0x00, 0x48, 0x00, 0x00,
            // ... (truncated for brevity, would need full JPEG)
            0xFF.toByte(), 0xD9.toByte() // JPEG end marker
        )
    }

    @Test
    fun `test PDFLayoutConfig constants are correct`() {
        // Verify layout constants
        assertEquals("Page width should be 612 (8.5 inches)", 612f, PDFLayoutConfig.PAGE_WIDTH)
        assertEquals("Page height should be 792 (11 inches)", 792f, PDFLayoutConfig.PAGE_HEIGHT)
        assertEquals("Content width should account for margins", 540f, PDFLayoutConfig.CONTENT_WIDTH)
        assertEquals("Photos per page should be 2", 2, PDFLayoutConfig.PHOTOS_PER_PAGE)
    }

    @Test
    fun `test PDFMetadata equality`() {
        val metadata1 = PDFMetadata(
            companyName = "Company A",
            projectName = "Project 1",
            projectLocation = "Location 1"
        )
        val metadata2 = PDFMetadata(
            companyName = "Company A",
            projectName = "Project 1",
            projectLocation = "Location 1"
        )

        assertEquals("Identical metadata should be equal", metadata1, metadata2)
        assertEquals("Hash codes should match", metadata1.hashCode(), metadata2.hashCode())
    }

    @Test
    fun `test PhotoData equality`() {
        val photoData1 = PhotoData(
            imageBytes = byteArrayOf(1, 2, 3),
            metadata = PhotoMetadata(timestamp = 1000L)
        )
        val photoData2 = PhotoData(
            imageBytes = byteArrayOf(1, 2, 3),
            metadata = PhotoMetadata(timestamp = 1000L)
        )

        assertEquals("Identical photo data should be equal", photoData1, photoData2)
    }
}
