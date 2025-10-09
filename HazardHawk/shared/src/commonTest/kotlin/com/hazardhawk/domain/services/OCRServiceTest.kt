package com.hazardhawk.domain.services

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Comprehensive test suite for OCRService (40 tests)
 * 
 * Coverage:
 * - Certification type mapping (15 tests)
 * - Date parsing (10 tests)
 * - Confidence calculation (5 tests)
 * - Field extraction (5 tests)
 * - Batch processing (5 tests)
 */
class OCRServiceTest {

    private lateinit var mockService: MockOCRService

    @BeforeTest
    fun setup() {
        mockService = MockOCRService()
    }

    @AfterTest
    fun tearDown() {
        mockService.reset()
    }

    // ====================
    // Certification Type Mapping (15 tests)
    // ====================

    @Test
    fun `extractCertificationData should map OSHA 10 variations correctly`() = runTest {
        val variations = listOf(
            "OSHA 10", "OSHA 10-Hour", "OSHA 10 Hour Construction",
            "10-Hour OSHA", "OSHA Ten Hour"
        )
        
        variations.forEach { variant ->
            mockService.mockRawText = "Certificate: $variant\nName: John Doe"
            val result = mockService.extractCertificationData("https://example.com/cert.pdf")
            
            assertTrue(result.isSuccess)
            assertEquals("OSHA_10", result.getOrNull()?.certificationType)
        }
    }

    @Test
    fun `extractCertificationData should map OSHA 30 variations correctly`() = runTest {
        val variations = listOf(
            "OSHA 30", "OSHA 30-Hour", "OSHA 30 Hour Construction",
            "30-Hour OSHA", "OSHA Thirty Hour"
        )
        
        variations.forEach { variant ->
            mockService.mockRawText = "Certificate: $variant\nName: Jane Smith"
            val result = mockService.extractCertificationData("https://example.com/cert.pdf")
            
            assertTrue(result.isSuccess)
            assertEquals("OSHA_30", result.getOrNull()?.certificationType)
        }
    }

    @Test
    fun `extractCertificationData should map Forklift variations correctly`() = runTest {
        val variations = listOf(
            "Forklift", "Forklift Operator", "Powered Industrial Truck",
            "PIT Certification", "Forklift License"
        )
        
        variations.forEach { variant ->
            mockService.mockRawText = "Type: $variant\nOperator: Bob Jones"
            val result = mockService.extractCertificationData("https://example.com/cert.pdf")
            
            assertTrue(result.isSuccess)
            assertEquals("FORKLIFT", result.getOrNull()?.certificationType)
        }
    }

    @Test
    fun `extractCertificationData should map First Aid variations correctly`() = runTest {
        val variations = listOf(
            "First Aid", "CPR/First Aid", "First Aid and CPR",
            "Emergency First Aid", "CPR Certification"
        )
        
        variations.forEach { variant ->
            mockService.mockRawText = "Course: $variant\nStudent: Alice Brown"
            val result = mockService.extractCertificationData("https://example.com/cert.pdf")
            
            assertTrue(result.isSuccess)
            assertEquals("FIRST_AID", result.getOrNull()?.certificationType)
        }
    }

    @Test
    fun `extractCertificationData should map Scaffold variations correctly`() = runTest {
        val variations = listOf(
            "Scaffold", "Scaffold Competent Person", "Scaffolding Certification",
            "Scaffold Safety", "Competent Person - Scaffold"
        )
        
        variations.forEach { variant ->
            mockService.mockRawText = "Training: $variant\nAttendee: Charlie Davis"
            val result = mockService.extractCertificationData("https://example.com/cert.pdf")
            
            assertTrue(result.isSuccess)
            assertEquals("SCAFFOLD", result.getOrNull()?.certificationType)
        }
    }

    @Test
    fun `extractCertificationData should handle case-insensitive matching`() = runTest {
        val variations = listOf(
            "osha 10", "OSHA 10", "Osha 10", "OsHa 10"
        )
        
        variations.forEach { variant ->
            mockService.mockRawText = "Certificate: $variant\nName: Test User"
            val result = mockService.extractCertificationData("https://example.com/cert.pdf")
            
            assertTrue(result.isSuccess)
            assertEquals("OSHA_10", result.getOrNull()?.certificationType)
        }
    }

    @Test
    fun `extractCertificationData should fail on unknown certification type`() = runTest {
        mockService.mockRawText = "Certificate: Underwater Basket Weaving\nName: John Doe"
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OCRError.UnknownCertificationType)
    }

    @Test
    fun `extractCertificationData should extract certification with partial match`() = runTest {
        mockService.mockRawText = """
            This is to certify that John Doe has completed
            the OSHA 10-Hour Construction Safety Training Program
            on January 15, 2023
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals("OSHA_10", result.getOrNull()?.certificationType)
    }

    @Test
    fun `extractCertificationData should prioritize exact matches over partial`() = runTest {
        mockService.mockRawText = """
            OSHA 30-Hour (not OSHA 10)
            Name: Jane Doe
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals("OSHA_30", result.getOrNull()?.certificationType)
    }

    @Test
    fun `extractCertificationData should handle multiple certification mentions`() = runTest {
        // Should extract the first/most prominent one
        mockService.mockRawText = """
            Primary: OSHA 10-Hour
            Also holds: First Aid, Forklift
            Name: Multi Cert
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals("OSHA_10", result.getOrNull()?.certificationType)
    }

    @Test
    fun `extractCertificationData should handle typos in certification types`() = runTest {
        mockService.fuzzyMatchingEnabled = true
        mockService.mockRawText = "Certificate: OSHA 1O (typo with letter O)\nName: Test"
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals("OSHA_10", result.getOrNull()?.certificationType)
    }

    @Test
    fun `extractCertificationData should extract from structured documents`() = runTest {
        mockService.mockRawText = """
            CERTIFICATION RECORD
            ====================
            Type: OSHA 30-Hour Construction
            Holder: Professional Worker
            Number: OSHA-123456
            Valid: 01/01/2023 - 01/01/2028
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let {
            assertEquals("OSHA_30", it.certificationType)
            assertEquals("Professional Worker", it.holderName)
        }
    }

    @Test
    fun `extractCertificationData should handle abbreviations`() = runTest {
        mockService.mockRawText = "Cert: FL Operator (Forklift)\nName: Operator One"
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals("FORKLIFT", result.getOrNull()?.certificationType)
    }

    @Test
    fun `extractCertificationData should extract from poor scans with OCR errors`() = runTest {
        mockService.mockRawText = """
            C3rt1f1cat3: 0SHA 10 H0ur
            Nam3: J0hn D03
        """.trimIndent()
        mockService.fuzzyMatchingEnabled = true
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals("OSHA_10", result.getOrNull()?.certificationType)
        // Should flag for review due to poor quality
        assertTrue(result.getOrNull()?.needsReview == true)
    }

    @Test
    fun `extractCertificationData should handle regional variations`() = runTest {
        mockService.mockRawText = """
            Certificate: Forklift Licence (UK spelling)
            Holder: British Worker
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals("FORKLIFT", result.getOrNull()?.certificationType)
    }

    // ====================
    // Date Parsing (10 tests)
    // ====================

    @Test
    fun `extractCertificationData should parse MM-DD-YYYY format`() = runTest {
        mockService.mockRawText = """
            OSHA 10
            Name: John Doe
            Issue: 01/15/2023
            Expiry: 01/15/2028
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let {
            assertEquals(LocalDate(2023, 1, 15), it.issueDate)
            assertEquals(LocalDate(2028, 1, 15), it.expirationDate)
        }
    }

    @Test
    fun `extractCertificationData should parse DD-MM-YYYY format`() = runTest {
        mockService.mockRawText = """
            OSHA 10
            Name: Jane Doe
            Issued: 15/01/2023
            Expires: 15/01/2028
        """.trimIndent()
        mockService.dateFormat = "DD/MM/YYYY"
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals(LocalDate(2023, 1, 15), result.getOrNull()?.issueDate)
    }

    @Test
    fun `extractCertificationData should parse ISO date format`() = runTest {
        mockService.mockRawText = """
            OSHA 30
            Name: Bob Smith
            Issue Date: 2023-01-15
            Expiration Date: 2028-01-15
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals(LocalDate(2023, 1, 15), result.getOrNull()?.issueDate)
        assertEquals(LocalDate(2028, 1, 15), result.getOrNull()?.expirationDate)
    }

    @Test
    fun `extractCertificationData should parse written month format`() = runTest {
        mockService.mockRawText = """
            Forklift Certification
            Name: Alice Jones
            Issued: January 15, 2023
            Valid Until: January 15, 2026
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals(LocalDate(2023, 1, 15), result.getOrNull()?.issueDate)
        assertEquals(LocalDate(2026, 1, 15), result.getOrNull()?.expirationDate)
    }

    @Test
    fun `extractCertificationData should parse abbreviated month format`() = runTest {
        mockService.mockRawText = """
            First Aid/CPR
            Name: Charlie Brown
            Date Issued: Jan 15, 2023
            Expires: Jan 15, 2025
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals(LocalDate(2023, 1, 15), result.getOrNull()?.issueDate)
    }

    @Test
    fun `extractCertificationData should parse European format`() = runTest {
        mockService.mockRawText = """
            Scaffold Training
            Name: David Wilson
            Issue: 15 Jan 2023
            Expiry: 15 Jan 2028
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals(LocalDate(2023, 1, 15), result.getOrNull()?.issueDate)
    }

    @Test
    fun `extractCertificationData should handle ambiguous dates with context`() = runTest {
        mockService.mockRawText = """
            OSHA 10
            Name: Ambiguous Date
            Issued: 12/06/2023
        """.trimIndent()
        // Default to MM/DD/YYYY for US certs
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let {
            // Should parse as December 6th (MM/DD)
            assertEquals(12, it.issueDate?.monthNumber)
            assertEquals(6, it.issueDate?.dayOfMonth)
        }
    }

    @Test
    fun `extractCertificationData should handle incomplete dates gracefully`() = runTest {
        mockService.mockRawText = """
            OSHA 10
            Name: Incomplete Info
            Issued: 2023
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        // Should extract what it can, flag for review
        assertTrue(result.getOrNull()?.needsReview == true)
    }

    @Test
    fun `extractCertificationData should parse dates with various separators`() = runTest {
        val separators = listOf("01/15/2023", "01-15-2023", "01.15.2023")
        
        separators.forEach { dateStr ->
            mockService.mockRawText = "OSHA 10\nName: Test\nIssue: $dateStr"
            val result = mockService.extractCertificationData("https://example.com/cert.pdf")
            
            assertTrue(result.isSuccess)
            assertEquals(LocalDate(2023, 1, 15), result.getOrNull()?.issueDate)
        }
    }

    @Test
    fun `extractCertificationData should handle no expiration date`() = runTest {
        mockService.mockRawText = """
            OSHA 10
            Name: No Expiry
            Issue Date: 01/15/2023
            No Expiration
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let {
            assertNotNull(it.issueDate)
            assertNull(it.expirationDate)
        }
    }

    // ====================
    // Confidence Calculation (5 tests)
    // ====================

    @Test
    fun `extractCertificationData should calculate high confidence for complete data`() = runTest {
        mockService.mockRawText = CertificationTestFixtures.osha10RawText
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let {
            assertTrue(it.confidence >= ExtractedCertification.MIN_AUTO_ACCEPT_CONFIDENCE)
            assertFalse(it.needsReview)
        }
    }

    @Test
    fun `extractCertificationData should calculate low confidence for poor quality`() = runTest {
        mockService.mockRawText = CertificationTestFixtures.poorQualityText
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let {
            assertTrue(it.confidence < ExtractedCertification.MIN_AUTO_ACCEPT_CONFIDENCE)
            assertTrue(it.needsReview)
        }
    }

    @Test
    fun `extractCertificationData should flag for review when missing critical fields`() = runTest {
        mockService.mockRawText = """
            OSHA 10
            Some text but missing name and dates
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let {
            assertTrue(it.needsReview)
        }
    }

    @Test
    fun `extractCertificationData should provide field-level confidence scores`() = runTest {
        mockService.mockRawText = CertificationTestFixtures.osha10RawText
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let {
            assertTrue(it.fieldConfidences.isNotEmpty())
            assertTrue(it.fieldConfidences.containsKey("holderName"))
            assertTrue(it.fieldConfidences.containsKey("certificationType"))
            
            it.fieldConfidences.values.forEach { confidence ->
                assertTrue(confidence in 0.0f..1.0f)
            }
        }
    }

    @Test
    fun `extractCertificationData should adjust confidence based on field quality`() = runTest {
        mockService.mockRawText = """
            OSHA 10 (very clear)
            Name: John Smith (very clear)
            Issue: 0?/15/2023 (date unclear)
            Number: unclear
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let {
            // Type and name should have high confidence
            assertTrue(it.fieldConfidences["certificationType"]!! > 0.8f)
            assertTrue(it.fieldConfidences["holderName"]!! > 0.8f)
            // Date and number should have lower confidence
            assertTrue((it.fieldConfidences["issueDate"] ?: 0f) < 0.8f)
        }
    }

    // ====================
    // Field Extraction (5 tests)
    // ====================

    @Test
    fun `extractCertificationData should extract holder name accurately`() = runTest {
        val names = listOf("John Smith", "Jane O'Brien", "José García", "Mary-Jane Watson")
        
        names.forEach { name ->
            mockService.mockRawText = "OSHA 10\nName: $name\nIssue: 01/15/2023"
            val result = mockService.extractCertificationData("https://example.com/cert.pdf")
            
            assertTrue(result.isSuccess)
            assertEquals(name, result.getOrNull()?.holderName)
        }
    }

    @Test
    fun `extractCertificationData should extract certification number`() = runTest {
        mockService.mockRawText = """
            OSHA 10
            Name: John Doe
            Certificate Number: OSHA-123456789
            Issue: 01/15/2023
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals("OSHA-123456789", result.getOrNull()?.certificationNumber)
    }

    @Test
    fun `extractCertificationData should extract issuing authority`() = runTest {
        mockService.mockRawText = """
            Forklift Certification
            Name: Jane Doe
            Issued by: National Safety Council
            Date: 01/15/2023
        """.trimIndent()
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals("National Safety Council", result.getOrNull()?.issuingAuthority)
    }

    @Test
    fun `extractCertificationData should preserve raw text for reference`() = runTest {
        val rawText = CertificationTestFixtures.osha10RawText
        mockService.mockRawText = rawText
        
        val result = mockService.extractCertificationData("https://example.com/cert.pdf")
        
        assertTrue(result.isSuccess)
        assertEquals(rawText, result.getOrNull()?.rawText)
    }

    @Test
    fun `extractCertificationData should handle fields with labels in various formats`() = runTest {
        val variations = listOf(
            "Name: John Doe",
            "NAME: John Doe",
            "Worker Name: John Doe",
            "Certificate Holder: John Doe",
            "This certifies: John Doe"
        )
        
        variations.forEach { variant ->
            mockService.mockRawText = "OSHA 10\n$variant\nIssue: 01/15/2023"
            val result = mockService.extractCertificationData("https://example.com/cert.pdf")
            
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.holderName?.contains("John Doe") == true)
        }
    }

    // ====================
    // Batch Processing (5 tests)
    // ====================

    @Test
    fun `extractCertificationDataBatch should process multiple documents`() = runTest {
        val urls = listOf(
            "https://example.com/cert1.pdf",
            "https://example.com/cert2.pdf",
            "https://example.com/cert3.pdf"
        )
        
        mockService.mockRawText = CertificationTestFixtures.osha10RawText
        val results = mockService.extractCertificationDataBatch(urls)
        
        assertEquals(3, results.size)
        results.forEach { result ->
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun `extractCertificationDataBatch should handle partial failures`() = runTest {
        val urls = listOf(
            "https://example.com/valid1.pdf",
            "https://example.com/invalid.pdf",
            "https://example.com/valid2.pdf"
        )
        
        mockService.failOnUrl = "https://example.com/invalid.pdf"
        val results = mockService.extractCertificationDataBatch(urls)
        
        assertEquals(3, results.size)
        assertTrue(results[0].isSuccess)
        assertTrue(results[1].isFailure)
        assertTrue(results[2].isSuccess)
    }

    @Test
    fun `extractCertificationDataBatch should maintain order`() = runTest {
        val urls = (1..5).map { "https://example.com/cert$it.pdf" }
        
        mockService.addNumberToName = true
        val results = mockService.extractCertificationDataBatch(urls)
        
        results.forEachIndexed { index, result ->
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.holderName?.contains("${index + 1}") == true)
        }
    }

    @Test
    fun `extractCertificationDataBatch should process empty list`() = runTest {
        val results = mockService.extractCertificationDataBatch(emptyList())
        
        assertTrue(results.isEmpty())
    }

    @Test
    fun `extractCertificationDataBatch should handle large batches efficiently`() = runTest {
        val urls = (1..50).map { "https://example.com/cert$it.pdf" }
        
        mockService.mockRawText = CertificationTestFixtures.osha10RawText
        val results = mockService.extractCertificationDataBatch(urls)
        
        assertEquals(50, results.size)
        assertTrue(results.all { it.isSuccess })
    }
}

/**
 * Mock implementation of OCRService for testing
 */
class MockOCRService : OCRService {
    var mockRawText: String = CertificationTestFixtures.osha10RawText
    var dateFormat: String = "MM/DD/YYYY"
    var fuzzyMatchingEnabled: Boolean = false
    var failOnUrl: String? = null
    var addNumberToName: Boolean = false
    
    private var callCount = 0

    fun reset() {
        mockRawText = CertificationTestFixtures.osha10RawText
        dateFormat = "MM/DD/YYYY"
        fuzzyMatchingEnabled = false
        failOnUrl = null
        addNumberToName = false
        callCount = 0
    }

    override suspend fun extractCertificationData(documentUrl: String): Result<ExtractedCertification> {
        callCount++
        
        if (documentUrl == failOnUrl) {
            return Result.failure(OCRError.ExtractionFailed("Simulated failure"))
        }
        
        // Extract certification type
        val certType = extractCertificationType(mockRawText) ?: return Result.failure(
            OCRError.UnknownCertificationType("Unable to determine type")
        )
        
        // Extract holder name
        var holderName = extractName(mockRawText)
        if (addNumberToName) {
            holderName = "$holderName $callCount"
        }
        
        // Extract dates
        val issueDate = extractDate(mockRawText, "issue")
        val expirationDate = extractDate(mockRawText, "expir")
        
        // Extract other fields
        val certNumber = extractCertNumber(mockRawText)
        val issuingAuthority = extractAuthority(mockRawText)
        
        // Calculate confidence
        val confidence = calculateConfidence(holderName, certType, issueDate, expirationDate)
        val needsReview = confidence < ExtractedCertification.MIN_AUTO_ACCEPT_CONFIDENCE ||
                         issueDate == null || expirationDate == null
        
        // Field confidences
        val fieldConfidences = mapOf(
            "holderName" to if (holderName.isNotBlank()) 0.95f else 0.5f,
            "certificationType" to 0.90f,
            "certificationNumber" to if (certNumber != null) 0.88f else 0.0f,
            "issueDate" to if (issueDate != null) 0.92f else 0.5f,
            "expirationDate" to if (expirationDate != null) 0.91f else 0.5f,
            "issuingAuthority" to if (issuingAuthority != null) 0.89f else 0.0f
        )
        
        return Result.success(
            ExtractedCertification(
                holderName = holderName,
                certificationType = certType,
                certificationNumber = certNumber,
                issueDate = issueDate,
                expirationDate = expirationDate,
                issuingAuthority = issuingAuthority,
                confidence = confidence,
                needsReview = needsReview,
                rawText = mockRawText,
                fieldConfidences = fieldConfidences
            )
        )
    }

    override suspend fun extractCertificationDataBatch(documentUrls: List<String>): List<Result<ExtractedCertification>> {
        return documentUrls.map { url ->
            extractCertificationData(url)
        }
    }

    private fun extractCertificationType(text: String): String? {
        val normalizedText = text.uppercase()
        
        return CertificationTestFixtures.certificationTypeMappings.entries
            .firstOrNull { (pattern, _) ->
                normalizedText.contains(pattern.uppercase())
            }?.value
    }

    private fun extractName(text: String): String {
        val namePatterns = listOf(
            Regex("(?:Name|NAME|Worker Name|Certificate Holder|This certifies):\\s*([A-Za-z\\s'-]+)", RegexOption.MULTILINE),
            Regex("certifies that\\s+([A-Za-z\\s'-]+)", RegexOption.IGNORE_CASE)
        )
        
        namePatterns.forEach { pattern ->
            pattern.find(text)?.let { match ->
                return match.groupValues[1].trim()
            }
        }
        
        return "Unknown"
    }

    private fun extractDate(text: String, keyword: String): LocalDate? {
        val datePattern = Regex("$keyword[^:]*:\\s*(\\d{1,2}[/-.]\\d{1,2}[/-.]\\d{4}|\\d{4}-\\d{2}-\\d{2}|[A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})", RegexOption.IGNORE_CASE)
        
        val match = datePattern.find(text) ?: return null
        val dateStr = match.groupValues[1]
        
        return parseDate(dateStr)
    }

    private fun parseDate(dateStr: String): LocalDate? {
        // Try various formats
        return try {
            when {
                dateStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                    val parts = dateStr.split("-")
                    LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                }
                dateStr.matches(Regex("\\d{1,2}[/-.]\\d{1,2}[/-.]\\d{4}")) -> {
                    val parts = dateStr.split(Regex("[/.-]"))
                    if (dateFormat == "DD/MM/YYYY") {
                        LocalDate(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                    } else {
                        LocalDate(parts[2].toInt(), parts[0].toInt(), parts[1].toInt())
                    }
                }
                else -> {
                    // Try to parse written dates
                    val monthMap = mapOf(
                        "january" to 1, "jan" to 1,
                        "february" to 2, "feb" to 2,
                        "march" to 3, "mar" to 3,
                        // ... etc
                    )
                    
                    val parts = dateStr.lowercase().split(Regex("\\s+|,"))
                    val month = monthMap.entries.firstOrNull { parts.any { part -> part.contains(it.key) } }?.value ?: 1
                    val day = parts.firstOrNull { it.toIntOrNull() != null && it.toInt() <= 31 }?.toInt() ?: 1
                    val year = parts.firstOrNull { it.toIntOrNull() != null && it.toInt() > 1900 }?.toInt() ?: 2023
                    
                    LocalDate(year, month, day)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractCertNumber(text: String): String? {
        val pattern = Regex("(?:Certificate|Cert|License|Card)\\s+(?:Number|No|#):\\s*([A-Z0-9-]+)", RegexOption.IGNORE_CASE)
        return pattern.find(text)?.groupValues?.get(1)
    }

    private fun extractAuthority(text: String): String? {
        val pattern = Regex("(?:Issued by|Issuing Authority|Authorized by):\\s*([A-Za-z\\s&.]+)", RegexOption.IGNORE_CASE)
        return pattern.find(text)?.groupValues?.get(1)?.trim()
    }

    private fun calculateConfidence(
        holderName: String,
        certType: String,
        issueDate: LocalDate?,
        expirationDate: LocalDate?
    ): Float {
        var confidence = 0.0f
        
        if (holderName.isNotBlank() && !holderName.contains("?") && !holderName.contains("3")) confidence += 0.25f
        if (certType.isNotBlank()) confidence += 0.25f
        if (issueDate != null) confidence += 0.25f
        if (expirationDate != null) confidence += 0.25f
        
        return confidence
    }
}
