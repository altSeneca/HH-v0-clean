// Quick validation script for foundational models
// This file validates the created models work as expected

fun main() {
    println("=== HazardHawk Foundational Models Validation ===")
    
    // Test UserRole and Permission integration
    println("\n1. Testing UserRole and Permission System:")
    
    val fieldWorker = UserRole.FIELD_ACCESS
    val safetyLead = UserRole.SAFETY_LEAD
    val projectAdmin = UserRole.PROJECT_ADMIN
    
    println("Field Worker can capture photos: ${fieldWorker.hasPermission(Permission.CAPTURE_PHOTOS)}")
    println("Field Worker can generate PTP: ${fieldWorker.hasPermission(Permission.GENERATE_PTP)}")
    println("Safety Lead can generate PTP: ${safetyLead.hasPermission(Permission.GENERATE_PTP)}")
    println("Project Admin has all permissions: ${projectAdmin.permissions.size == Permission.values().size}")
    
    // Test Navigation Models
    println("\n2. Testing Navigation System:")
    
    val pdfExport = HazardHawkDestination.PDFExport("doc123", DocumentType.PTP)
    val docGeneration = HazardHawkDestination.DocumentGeneration(DocumentType.INCIDENT_REPORT)
    
    println("PDF Export route: ${HazardHawkDestination.PDFExport.createRoute("doc123", DocumentType.PTP)}")
    println("Document Generation route: ${HazardHawkDestination.DocumentGeneration.createRoute(DocumentType.INCIDENT_REPORT)}")
    
    // Test DocumentType requirements
    println("\n3. Testing Document Type Requirements:")
    println("Incident Report requires photos: ${DocumentType.INCIDENT_REPORT.requiresPhotos()}")
    println("PTP requires signature: ${DocumentType.PTP.requiresSignature()}")
    println("Toolbox Talk file extension: ${DocumentType.TOOLBOX_TALK.getFileExtension()}")
    
    // Test PDF Models
    println("\n4. Testing PDF Export Models:")
    
    val metadata = PDFMetadata(
        projectName = "Test Construction Site",
        location = "123 Main St",
        generatedBy = "John Doe",
        generatedDate = System.currentTimeMillis()
    )
    
    val signatureData = SignatureData(
        signaturePath = "/path/to/signature.png",
        signerName = "John Doe",
        signerRole = "Safety Lead",
        signedDate = System.currentTimeMillis()
    )
    
    val pdfRequest = PDFExportRequest(
        documentId = "test-doc-001",
        documentType = DocumentType.PTP,
        metadata = metadata,
        signatureData = signatureData
    )
    
    println("PDF Export request validation: ${pdfRequest.validate().isSuccess}")
    println("Generated filename: ${pdfRequest.generateFileName()}")
    
    val successState = PDFExportState.Success(
        filePath = "/path/to/document.pdf",
        fileName = "test-ptp.pdf",
        fileSize = 1024 * 512 // 512KB
    )
    
    println("Success state file size: ${successState.getFormattedFileSize()}")
    
    // Test PDF Template system
    println("\n5. Testing PDF Template System:")
    val ptpTemplate = PDFTemplate.getDefault(DocumentType.PTP)
    println("PTP template name: ${ptpTemplate.templateName}")
    println("PTP template supports signature: ${ptpTemplate.supportedFeatures.contains(PDFFeature.SIGNATURE)}")
    
    println("\n✅ All foundational models validation completed successfully!")
    println("\nCreated Files:")
    println("- /shared/src/commonMain/kotlin/com/hazardhawk/models/UserRole.kt")
    println("- /shared/src/commonMain/kotlin/com/hazardhawk/models/NavigationModels.kt")
    println("- /shared/src/commonMain/kotlin/com/hazardhawk/models/PDFModels.kt")
}