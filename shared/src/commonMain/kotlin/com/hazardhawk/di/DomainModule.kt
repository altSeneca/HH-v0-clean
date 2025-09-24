package com.hazardhawk.di

import org.koin.dsl.module

/**
 * Domain module containing business logic use cases.
 * Use cases encapsulate specific business operations and coordinate between repositories.
 */
val domainModule = module {
    
    // Photo Management Use Cases
    // factory<CapturePhotoUseCase> {
    //     CapturePhotoUseCase(
    //         photoRepository = get(),
    //         locationService = get()
    //     )
    // }
    
    // factory<GetPhotosUseCase> {
    //     GetPhotosUseCase(
    //         photoRepository = get()
    //     )
    // }
    
    // factory<DeletePhotoUseCase> {
    //     DeletePhotoUseCase(
    //         photoRepository = get()
    //     )
    // }
    
    // AI Analysis Use Cases
    // factory<AnalyzePhotoUseCase> {
    //     AnalyzePhotoUseCase(
    //         analysisRepository = get(),
    //         photoRepository = get()
    //     )
    // }
    
    // factory<GetAnalysisResultsUseCase> {
    //     GetAnalysisResultsUseCase(
    //         analysisRepository = get()
    //     )
    // }
    
    // Tag Management Use Cases
    // factory<GetRecommendedTagsUseCase> {
    //     GetRecommendedTagsUseCase(
    //         tagRepository = get(),
    //         analysisRepository = get()
    //     )
    // }
    
    // factory<ApplyTagsUseCase> {
    //     ApplyTagsUseCase(
    //         tagRepository = get(),
    //         photoRepository = get()
    //     )
    // }
    
    // Safety Report Use Cases
    // factory<GenerateReportUseCase> {
    //     GenerateReportUseCase(
    //         reportRepository = get(),
    //         photoRepository = get(),
    //         analysisRepository = get()
    //     )
    // }
    
    // factory<ExportReportUseCase> {
    //     ExportReportUseCase(
    //         reportRepository = get()
    //     )
    // }
    
    // Project Management Use Cases
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
    
    // OSHA Compliance Use Cases
    // factory<ValidateComplianceUseCase> {
    //     ValidateComplianceUseCase(
    //         analysisRepository = get(),
    //         tagRepository = get()
    //     )
    // }
    
    // factory<GenerateComplianceReportUseCase> {
    //     GenerateComplianceReportUseCase(
    //         reportRepository = get(),
    //         analysisRepository = get()
    //     )
    // }
}

/**
 * Domain services module for complex business logic
 */
val domainServicesModule = module {
    
    // AI Service Facade for managing different AI providers
    // single<AIServiceFacade> {
    //     AIServiceFacade(
    //         geminiService = get(),
    //         localAIService = get(),
    //         applicationScope = getApplicationScope()
    //     )
    // }
    
    // Safety recommendation engine
    // single<SafetyRecommendationEngine> {
    //     SafetyRecommendationEngine(
    //         tagRepository = get(),
    //         analysisRepository = get()
    //     )
    // }
    
    // Hazard detection and classification service
    // single<HazardDetectionService> {
    //     HazardDetectionService(
    //         aiService = get(),
    //         oshaStandards = get()
    //     )
    // }
}
