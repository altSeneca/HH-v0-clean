package com.hazardhawk.data.mocks

/**
 * Sample API responses for testing
 * Contains realistic mock data for all Phase 2 services
 */
object MockApiResponses {
    
    /**
     * Sample certification data
     */
    val sampleOsha10Certification = mapOf(
        "id" to "cert-001",
        "workerId" to "worker-123",
        "workerName" to "John Doe",
        "certificationType" to "OSHA 10",
        "certificationNumber" to "OSHA10-2025-456789",
        "issueDate" to "2025-01-15",
        "expirationDate" to null,
        "issuingAuthority" to "OSHA Authorized Training Provider",
        "documentUrl" to "https://cdn.hazardhawk.com/certifications/cert-001.pdf",
        "status" to "active",
        "verificationStatus" to "approved"
    )
    
    val sampleOsha30Certification = mapOf(
        "id" to "cert-002",
        "workerId" to "worker-456",
        "workerName" to "Jane Smith",
        "certificationType" to "OSHA 30",
        "certificationNumber" to "OSHA30-2025-789012",
        "issueDate" to "2025-02-01",
        "expirationDate" to null,
        "issuingAuthority" to "Construction Safety Institute"
    )
    
    val sampleCprCertification = mapOf(
        "id" to "cert-003",
        "workerId" to "worker-789",
        "workerName" to "Bob Johnson",
        "certificationType" to "CPR",
        "certificationNumber" to "CPR-2025-345678",
        "issueDate" to "2025-03-10",
        "expirationDate" to "2027-03-10",
        "issuingAuthority" to "American Red Cross"
    )
    
    /**
     * Sample worker data
     */
    val sampleWorker = mapOf(
        "id" to "worker-123",
        "firstName" to "John",
        "lastName" to "Doe",
        "email" to "john.doe@example.com",
        "phone" to "+15551234567",
        "role" to "general_laborer",
        "status" to "active",
        "certifications" to listOf("cert-001"),
        "assignedProjects" to listOf("project-001", "project-002")
    )
    
    val sampleSupervisor = mapOf(
        "id" to "worker-456",
        "firstName" to "Jane",
        "lastName" to "Smith",
        "email" to "jane.smith@example.com",
        "phone" to "+15552345678",
        "role" to "site_supervisor",
        "status" to "active",
        "certifications" to listOf("cert-002")
    )
    
    /**
     * Sample dashboard metrics
     */
    val sampleSafetyMetrics = mapOf(
        "period" to "last_30_days",
        "incidentCount" to 2,
        "nearMissCount" to 8,
        "complianceScore" to 92.5,
        "activeCertifications" to 156,
        "expiringCertifications" to 12,
        "expiredCertifications" to 3
    )
    
    val sampleComplianceSummary = mapOf(
        "totalWorkers" to 120,
        "workersWithAllCerts" to 108,
        "compliancePercentage" to 90.0,
        "requiredCertifications" to listOf("OSHA 10", "Fall Protection", "First Aid/CPR")
    )
    
    /**
     * Sample presigned URL response
     */
    val samplePresignedUrl = mapOf(
        "uploadUrl" to "https://s3.amazonaws.com/hazardhawk-certifications/temp/cert-123.pdf?presigned=true",
        "cdnUrl" to "https://cdn.hazardhawk.com/certifications/cert-123.pdf",
        "expiresIn" to 900,
        "bucket" to "hazardhawk-certifications",
        "key" to "certifications/cert-123.pdf"
    )
    
    /**
     * Sample OCR extraction result
     */
    val sampleOcrExtractionSuccess = mapOf(
        "documentUrl" to "https://cdn.hazardhawk.com/certifications/cert-001.pdf",
        "extracted" to mapOf(
            "holderName" to "John Doe",
            "certificationType" to "OSHA 10",
            "certificationNumber" to "OSHA10-2025-456789",
            "issueDate" to "2025-01-15",
            "expirationDate" to null,
            "issuingAuthority" to "OSHA Authorized Training Provider"
        ),
        "confidence" to 0.92,
        "needsReview" to false,
        "processingTime" to 2.5
    )
    
    val sampleOcrExtractionLowConfidence = mapOf(
        "documentUrl" to "https://cdn.hazardhawk.com/certifications/cert-blurry.pdf",
        "extracted" to mapOf(
            "holderName" to "J0hn D0e",
            "certificationType" to "Unknown",
            "certificationNumber" to null
        ),
        "confidence" to 0.42,
        "needsReview" to true,
        "processingTime" to 3.1
    )
    
    /**
     * Sample notification responses
     */
    val sampleEmailSent = mapOf(
        "messageId" to "msg-001",
        "status" to "sent",
        "recipient" to "john.doe@example.com",
        "subject" to "Certification Expiring Soon"
    )
    
    val sampleSmsSent = mapOf(
        "messageId" to "sms-001",
        "status" to "sent",
        "recipient" to "+15551234567"
    )
    
    /**
     * Sample error responses
     */
    val errorUnauthorized = mapOf(
        "error" to "unauthorized",
        "message" to "Authentication token is invalid or expired",
        "code" to 401
    )
    
    val errorNotFound = mapOf(
        "error" to "not_found",
        "message" to "Resource not found",
        "code" to 404
    )
    
    val errorValidation = mapOf(
        "error" to "validation_error",
        "message" to "Invalid request parameters",
        "code" to 400,
        "details" to listOf(
            mapOf(
                "field" to "certificationType",
                "message" to "Must be one of: OSHA 10, OSHA 30, CPR, First Aid"
            )
        )
    )
    
    val errorServerError = mapOf(
        "error" to "internal_server_error",
        "message" to "An unexpected error occurred",
        "code" to 500,
        "requestId" to "req-123456"
    )
}
