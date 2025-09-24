package com.hazardhawk.di

import org.koin.dsl.module

/**
 * Domain module for business logic use cases.
 * Contains use case implementations and business rules.
 */
val domainModule = module {
    
    // Photo use cases
    // factory<CapturePhotoUseCase> {
    //     CapturePhotoUseCase(
    //         photoRepository = get(),
    //         storageManager = get(),
    //         locationService = get()
    //     )
    // }
    
    // factory<GetPhotosUseCase> {
    //     GetPhotosUseCase(
    //         photoRepository = get()
    //     )
    // }
    
    // factory<DeletePhotosUseCase> {
    //     DeletePhotosUseCase(
    //         photoRepository = get(),
    //         storageManager = get()
    //     )
    // }
    
    // Analysis use cases
    // factory<AnalyzePhotoUseCase> {
    //     AnalyzePhotoUseCase(
    //         analysisRepository = get(),
    //         aiService = get(),
    //         complianceEngine = get()
    //     )
    // }
    
    // factory<GetAnalysisResultsUseCase> {
    //     GetAnalysisResultsUseCase(
    //         analysisRepository = get()
    //     )
    // }
    
    // Tag use cases
    // factory<GetRecommendedTagsUseCase> {
    //     GetRecommendedTagsUseCase(
    //         tagRepository = get(),
    //         aiService = get()
    //     )
    // }
    
    // factory<ApplyTagsUseCase> {
    //     ApplyTagsUseCase(
    //         tagRepository = get(),
    //         photoRepository = get()
    //     )
    // }
    
    // Report use cases
    // factory<GenerateReportUseCase> {
    //     GenerateReportUseCase(
    //         photoRepository = get(),
    //         analysisRepository = get(),
    //         tagRepository = get(),
    //         reportGenerator = get()
    //     )
    // }
    
    // factory<ExportReportUseCase> {
    //     ExportReportUseCase(
    //         reportRepository = get(),
    //         fileManager = get()
    //     )
    // }
    
    // Project use cases
    // factory<CreateProjectUseCase> {
    //     CreateProjectUseCase(
    //         projectRepository = get(),
    //         userRepository = get()
    //     )
    // }
    
    // factory<GetProjectsUseCase> {
    //     GetProjectsUseCase(
    //         projectRepository = get()
    //     )
    // }
}

/**
 * Domain services module for complex business operations.
 */
val domainServicesModule = module {
    
    // AI services
    // single<AIAnalysisService> {
    //     AIAnalysisServiceImpl(
    //         geminiClient = get(),
    //         onnxService = get(),
    //         fallbackService = get()
    //     )
    // }
    
    // single<HazardDetectionService> {
    //     HazardDetectionServiceImpl(
    //         yoloDetector = get(),
    //         geminiAnalyzer = get(),
    //         confidenceThreshold = 0.7f
    //     )
    // }
    
    // OSHA compliance engine
    // single<OSHAComplianceEngine> {
    //     OSHAComplianceEngineImpl(
    //         regulationsDatabase = get(),
    //         complianceRules = get()
    //     )
    // }
    
    // Report generation service
    // single<ReportGenerationService> {
    //     ReportGenerationServiceImpl(
    //         pdfGenerator = get(),
    //         templateEngine = get(),
    //         signatureService = get()
    //     )
    // }
    
    // Tag recommendation engine
    // single<TagRecommendationEngine> {
    //     TagRecommendationEngineImpl(
    //         aiService = get(),
    //         oshaEngine = get(),
    //         learningModel = get()
    //     )
    // }
    
    // File management service
    // single<FileManagementService> {
    //     FileManagementServiceImpl(
    //         storageProvider = get(),
    //         encryptionService = get(),
    //         compressionService = get()
    //     )
    // }
}
