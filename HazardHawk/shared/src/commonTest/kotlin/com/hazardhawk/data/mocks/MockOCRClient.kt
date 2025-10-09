package com.hazardhawk.data.mocks

import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Mock OCR client for testing document AI extraction
 * 
 * Features:
 * - Simulates Document AI responses
 * - Configurable confidence levels
 * - Recognition of common certification types
 * - Simulates processing delays
 */
class MockOCRClient(
    private val config: MockOCRConfig = MockOCRConfig()
) {
    private val extractionHistory = mutableListOf<ExtractionRecord>()
    
    data class MockOCRConfig(
        val processingDelayMs: Long = 2000L,
        val baseConfidence: Double = 0.90,
        val confidenceVariation: Double = 0.10,
        val shouldFail: Boolean = false,
        val customResponse: DocumentAIResponse? = null
    )
    
    data class DocumentAIResponse(
        val text: String = "",
        val holderName: String? = null,
        val certificationType: String? = null,
        val certificationNumber: String? = null,
        val expirationDate: String? = null,
        val issueDate: String? = null,
        val issuingAuthority: String? = null,
        val confidence: Double = 0.90
    )
    
    data class ExtractedCertification(
        val holderName: String,
        val certificationType: String,
        val certificationNumber: String,
        val expirationDate: String?,
        val issueDate: String?,
        val issuingAuthority: String?,
        val confidence: Double,
        val needsReview: Boolean,
        val rawText: String
    )
    
    data class ExtractionRecord(
        val documentUrl: String,
        val result: Result<ExtractedCertification>,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Extract certification data from document
     */
    suspend fun extractCertificationData(documentUrl: String): Result<ExtractedCertification> {
        // Simulate processing delay
        delay(config.processingDelayMs)
        
        // Return custom response if configured
        if (config.customResponse != null) {
            val extracted = convertToExtracted(config.customResponse)
            val result = Result.success(extracted)
            extractionHistory.add(ExtractionRecord(documentUrl, result))
            return result
        }
        
        // Simulate configured failure
        if (config.shouldFail) {
            val result = Result.failure<ExtractedCertification>(
                MockOCRException("Simulated OCR processing failure")
            )
            extractionHistory.add(ExtractionRecord(documentUrl, result))
            return result
        }
        
        // Detect certification type from URL
        val certType = detectCertificationType(documentUrl)
        val confidence = calculateConfidence()
        
        val extracted = ExtractedCertification(
            holderName = generateRandomName(),
            certificationType = certType,
            certificationNumber = generateCertNumber(certType),
            expirationDate = generateExpirationDate(certType),
            issueDate = generateIssueDate(),
            issuingAuthority = getIssuingAuthority(certType),
            confidence = confidence,
            needsReview = confidence < 0.75,
            rawText = generateRawText(certType)
        )
        
        val result = Result.success(extracted)
        extractionHistory.add(ExtractionRecord(documentUrl, result))
        return result
    }
    
    /**
     * Batch extract multiple documents
     */
    suspend fun batchExtract(documentUrls: List<String>): Result<List<ExtractedCertification>> {
        val results = mutableListOf<ExtractedCertification>()
        
        for (url in documentUrls) {
            val result = extractCertificationData(url)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull()!!)
            }
            results.add(result.getOrThrow())
        }
        
        return Result.success(results)
    }
    
    /**
     * Convert DocumentAIResponse to ExtractedCertification
     */
    private fun convertToExtracted(response: DocumentAIResponse): ExtractedCertification {
        return ExtractedCertification(
            holderName = response.holderName ?: "Unknown",
            certificationType = response.certificationType ?: "Unknown",
            certificationNumber = response.certificationNumber ?: "N/A",
            expirationDate = response.expirationDate,
            issueDate = response.issueDate,
            issuingAuthority = response.issuingAuthority,
            confidence = response.confidence,
            needsReview = response.confidence < 0.75,
            rawText = response.text
        )
    }
    
    /**
     * Detect certification type from filename/URL
     */
    private fun detectCertificationType(url: String): String {
        return when {
            url.contains("osha10", ignoreCase = true) -> "OSHA 10"
            url.contains("osha30", ignoreCase = true) -> "OSHA 30"
            url.contains("cpr", ignoreCase = true) -> "CPR"
            url.contains("first-aid", ignoreCase = true) -> "First Aid"
            url.contains("forklift", ignoreCase = true) -> "Forklift Operator"
            url.contains("scaffold", ignoreCase = true) -> "Scaffold Competent Person"
            url.contains("fall-protection", ignoreCase = true) -> "Fall Protection"
            else -> "General Safety"
        }
    }
    
    /**
     * Calculate confidence with variation
     */
    private fun calculateConfidence(): Double {
        val variation = Random.nextDouble(-config.confidenceVariation, config.confidenceVariation)
        return (config.baseConfidence + variation).coerceIn(0.0, 1.0)
    }
    
    /**
     * Generate random holder name
     */
    private fun generateRandomName(): String {
        val firstNames = listOf("John", "Jane", "Michael", "Sarah", "David", "Emily", "Robert", "Lisa")
        val lastNames = listOf("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis")
        return "${firstNames.random()} ${lastNames.random()}"
    }
    
    /**
     * Generate certification number
     */
    private fun generateCertNumber(certType: String): String {
        val prefix = when {
            certType.contains("OSHA") -> certType.replace(" ", "")
            certType.contains("CPR") -> "CPR"
            else -> "CERT"
        }
        val year = 2025
        val number = Random.nextInt(100000, 999999)
        return "$prefix-$year-$number"
    }
    
    /**
     * Generate issue date
     */
    private fun generateIssueDate(): String {
        val month = Random.nextInt(1, 13)
        val day = Random.nextInt(1, 29)
        return "2025-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }
    
    /**
     * Generate expiration date (null for awareness training)
     */
    private fun generateExpirationDate(certType: String): String? {
        return if (certType.contains("OSHA")) {
            null // OSHA is awareness training, doesn't expire
        } else {
            val month = Random.nextInt(1, 13)
            val day = Random.nextInt(1, 29)
            "2027-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        }
    }
    
    /**
     * Get issuing authority for certification type
     */
    private fun getIssuingAuthority(certType: String): String {
        return when {
            certType.contains("OSHA") -> "OSHA Authorized Training Provider"
            certType.contains("CPR") -> "American Red Cross"
            certType.contains("First Aid") -> "American Red Cross"
            else -> "Certified Training Institute"
        }
    }
    
    /**
     * Generate realistic raw text
     */
    private fun generateRawText(certType: String): String {
        return """
            $certType Certification
            
            This certifies that the holder has successfully completed
            the required training program and demonstrated competency
            in the subject matter.
            
            Certificate Type: $certType
            Training Provider: ${getIssuingAuthority(certType)}
            
            Topics Covered:
            - Safety regulations and compliance
            - Hazard recognition and mitigation
            - Emergency response procedures
            - Best practices and standards
        """.trimIndent()
    }
    
    /**
     * Get extraction history
     */
    fun getExtractionHistory(): List<ExtractionRecord> = extractionHistory.toList()
    
    /**
     * Clear history
     */
    fun clearHistory() {
        extractionHistory.clear()
    }
    
    /**
     * Count successful extractions
     */
    fun countSuccessfulExtractions(): Int {
        return extractionHistory.count { it.result.isSuccess }
    }
}

/**
 * Mock OCR exception
 */
class MockOCRException(message: String) : Exception(message)
