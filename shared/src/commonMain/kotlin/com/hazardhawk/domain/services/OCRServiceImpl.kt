package com.hazardhawk.domain.services

import kotlinx.datetime.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.max

/**
 * Implementation of OCRService using Google Document AI.
 * Handles document processing, field extraction, and intelligent certification type mapping.
 */
class OCRServiceImpl : OCRService {

    // TODO: Inject configuration for Google Document AI
    // private val projectId: String
    // private val location: String = "us" // or "eu"
    // private val processorId: String
    // private val httpClient: HttpClient

    /**
     * Extracts certification data from a document using Google Document AI.
     */
    override suspend fun extractCertificationData(
        documentUrl: String
    ): Result<ExtractedCertification> {
        return try {
            // Validate document format first
            val validation = validateDocumentFormat(documentUrl).getOrThrow()
            if (!validation.isValid) {
                return Result.failure(
                    IllegalArgumentException(validation.errorMessage ?: "Invalid document format")
                )
            }

            // TODO: Implement actual Google Document AI integration
            // 1. Download document from URL (or pass URL directly if supported)
            // 2. Call Document AI processor
            // 3. Parse response and extract fields

            val documentAIResponse = callDocumentAI(documentUrl)
            val extracted = parseDocumentAIResponse(documentAIResponse)

            Result.success(extracted)
        } catch (e: Exception) {
            Result.failure(Exception("OCR extraction failed: ${e.message}", e))
        }
    }

    /**
     * Validates document format and accessibility.
     */
    override suspend fun validateDocumentFormat(
        documentUrl: String
    ): Result<DocumentValidation> {
        return try {
            // Extract file extension
            val extension = documentUrl.substringAfterLast('.', "").lowercase()
            val supportedFormats = setOf("pdf", "png", "jpg", "jpeg")

            if (extension !in supportedFormats) {
                return Result.success(
                    DocumentValidation(
                        isValid = false,
                        format = extension.ifBlank { null },
                        sizeBytes = null,
                        errorMessage = "Unsupported format: $extension. Supported: ${supportedFormats.joinToString()}"
                    )
                )
            }

            // TODO: Implement actual document validation
            // 1. Check if URL is accessible (HEAD request)
            // 2. Verify content-type header
            // 3. Check file size (Document AI has limits)

            Result.success(
                DocumentValidation(
                    isValid = true,
                    format = extension,
                    sizeBytes = null, // TODO: Get from HEAD request
                    errorMessage = null
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Document validation failed: ${e.message}", e))
        }
    }

    /**
     * Batch processes multiple documents in parallel.
     */
    override suspend fun batchExtractCertifications(
        documentUrls: List<String>
    ): List<Result<ExtractedCertification>> {
        return coroutineScope {
            documentUrls.map { url ->
                async {
                    extractCertificationData(url)
                }
            }.awaitAll()
        }
    }

    // ===== Private Helper Methods =====

    /**
     * Calls Google Document AI API to process the document.
     * TODO: Replace with actual API integration using Ktor HttpClient.
     */
    private suspend fun callDocumentAI(documentUrl: String): DocumentAIResponse {
        // TODO: Implement actual Google Document AI API call
        // Example structure:
        /*
        val request = json.encodeToString(DocumentAIRequest(
            rawDocument = RawDocument(
                content = base64EncodedContent, // or use gcsUri for S3
                mimeType = "application/pdf"
            )
        ))

        val response = httpClient.post("https://us-documentai.googleapis.com/v1/projects/$projectId/locations/$location/processors/$processorId:process") {
            contentType(ContentType.Application.Json)
            setBody(request)
            header("Authorization", "Bearer $accessToken")
        }

        return response.body<DocumentAIResponse>()
        */

        // Stub response for now
        return DocumentAIResponse(
            text = "Sample extracted text from document",
            entities = emptyList(),
            confidence = 0.0f
        )
    }

    /**
     * Parses Document AI response and extracts certification data.
     */
    private fun parseDocumentAIResponse(response: DocumentAIResponse): ExtractedCertification {
        val extractedFields = mutableMapOf<String, String>()

        // Extract entities from Document AI response
        // Document AI returns entities like:
        // - holder_name
        // - certification_type
        // - certification_number
        // - issue_date
        // - expiration_date
        // - issuing_authority

        response.entities.forEach { entity ->
            extractedFields[entity.type] = entity.mentionText
        }

        // Parse and clean extracted data
        val holderName = extractField(extractedFields, listOf("holder_name", "name", "full_name"))
        val certTypeRaw = extractField(extractedFields, listOf("certification_type", "cert_type", "type"))
        val certNumber = extractField(extractedFields, listOf("certification_number", "cert_number", "number", "id"))
        val issueDate = extractDateField(extractedFields, listOf("issue_date", "issued_date", "date_issued"))
        val expirationDate = extractDateField(extractedFields, listOf("expiration_date", "expiration", "expires", "exp_date"))
        val issuingAuthority = extractField(extractedFields, listOf("issuing_authority", "authority", "issued_by", "organization"))

        // Map certification type to standard code
        val certificationType = mapCertificationType(certTypeRaw, response.text)

        // Calculate confidence score
        val confidence = calculateConfidence(
            holderName = holderName,
            certificationType = certificationType,
            certNumber = certNumber,
            issueDate = issueDate,
            expirationDate = expirationDate,
            issuingAuthority = issuingAuthority,
            documentAIConfidence = response.confidence
        )

        return ExtractedCertification(
            holderName = holderName,
            certificationType = certificationType,
            certificationNumber = certNumber,
            issueDate = issueDate,
            expirationDate = expirationDate,
            issuingAuthority = issuingAuthority,
            confidence = confidence,
            needsReview = confidence < 0.85f,
            rawText = response.text,
            extractedFields = extractedFields
        )
    }

    /**
     * Extracts a field value from the extracted fields map.
     * Tries multiple field name variations.
     */
    private fun extractField(fields: Map<String, String>, fieldNames: List<String>): String {
        for (fieldName in fieldNames) {
            val value = fields[fieldName]
            if (!value.isNullOrBlank()) {
                return value.trim()
            }
        }
        return ""
    }

    /**
     * Extracts and parses a date field.
     * Supports multiple date formats: MM/DD/YYYY, DD/MM/YYYY, YYYY-MM-DD, etc.
     */
    private fun extractDateField(fields: Map<String, String>, fieldNames: List<String>): LocalDate? {
        val dateString = extractField(fields, fieldNames)
        if (dateString.isBlank()) return null

        return parseDateString(dateString)
    }

    /**
     * Parses a date string with error recovery.
     * Tries multiple common date formats.
     */
    private fun parseDateString(dateString: String): LocalDate? {
        return try {
            // Clean the date string
            val cleaned = dateString.trim()
                .replace(Regex("[^0-9/\\-.]"), " ")
                .trim()

            // Try different date formats
            val formats = listOf(
                Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})"""), // MM/DD/YYYY or DD/MM/YYYY
                Regex("""(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})"""), // YYYY-MM-DD
                Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2})""")  // MM/DD/YY or DD/MM/YY
            )

            for (format in formats) {
                val match = format.find(cleaned)
                if (match != null) {
                    val (g1, g2, g3) = match.destructured

                    // Determine if it's YYYY-MM-DD or MM/DD/YYYY format
                    val year: Int
                    val month: Int
                    val day: Int

                    when {
                        g1.length == 4 -> {
                            // YYYY-MM-DD format
                            year = g1.toInt()
                            month = g2.toInt()
                            day = g3.toInt()
                        }
                        g3.length == 4 -> {
                            // MM/DD/YYYY format (assume US format)
                            month = g1.toInt()
                            day = g2.toInt()
                            year = g3.toInt()
                        }
                        else -> {
                            // MM/DD/YY format - assume 20xx for years < 50, 19xx otherwise
                            month = g1.toInt()
                            day = g2.toInt()
                            val yearShort = g3.toInt()
                            year = if (yearShort < 50) 2000 + yearShort else 1900 + yearShort
                        }
                    }

                    // Validate ranges
                    if (month in 1..12 && day in 1..31 && year in 1900..2100) {
                        return LocalDate(year, month, day)
                    }
                }
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Maps raw certification type text to standard certification codes.
     * Uses fuzzy matching and keyword extraction.
     */
    private fun mapCertificationType(rawType: String, fullText: String): String {
        if (rawType.isBlank() && fullText.isBlank()) {
            return CertificationTypeCodes.OTHER
        }

        // Combine raw type and full text for better matching
        val searchText = "$rawType $fullText".lowercase()

        // Try exact matches first (case-insensitive)
        for ((key, code) in CertificationTypeCodes.NAME_TO_CODE_MAPPING) {
            if (searchText.contains(key)) {
                return code
            }
        }

        // Try partial matches with keywords
        val keywords = extractKeywords(searchText)
        for (keyword in keywords) {
            for ((key, code) in CertificationTypeCodes.NAME_TO_CODE_MAPPING) {
                if (keyword.length >= 4 && key.contains(keyword)) {
                    return code
                }
            }
        }

        // Default to OTHER if no match found
        return CertificationTypeCodes.OTHER
    }

    /**
     * Extracts relevant keywords from text for certification type matching.
     */
    private fun extractKeywords(text: String): List<String> {
        // Split by whitespace and filter out common words
        val stopWords = setOf("the", "of", "and", "for", "in", "to", "a", "an", "is", "are", "was", "were")
        return text.split(Regex("\\s+"))
            .map { it.trim().lowercase() }
            .filter { it.length > 2 && it !in stopWords }
    }

    /**
     * Calculates overall confidence score based on field extraction success.
     * Confidence ranges from 0.0 to 1.0.
     */
    private fun calculateConfidence(
        holderName: String,
        certificationType: String,
        certNumber: String?,
        issueDate: LocalDate?,
        expirationDate: LocalDate?,
        issuingAuthority: String?,
        documentAIConfidence: Float
    ): Float {
        var score = 0.0f

        // Critical fields (60% weight)
        if (holderName.isNotBlank()) score += 0.25f
        if (certificationType != CertificationTypeCodes.OTHER) score += 0.25f
        if (issueDate != null || expirationDate != null) score += 0.10f

        // Important fields (25% weight)
        if (certNumber?.isNotBlank() == true) score += 0.10f
        if (issuingAuthority?.isNotBlank() == true) score += 0.15f

        // Document AI base confidence (15% weight)
        score += documentAIConfidence * 0.15f

        return score.coerceIn(0.0f, 1.0f)
    }

    // ===== Data Classes for Document AI Integration =====

    /**
     * Response from Document AI API.
     * Simplified structure - actual API returns more fields.
     */
    private data class DocumentAIResponse(
        val text: String,
        val entities: List<Entity>,
        val confidence: Float
    )

    /**
     * Entity extracted by Document AI.
     */
    private data class Entity(
        val type: String,
        val mentionText: String,
        val confidence: Float
    )
}
