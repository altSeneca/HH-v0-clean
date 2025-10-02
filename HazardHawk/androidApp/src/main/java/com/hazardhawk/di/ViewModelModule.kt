package com.hazardhawk.di

import com.hazardhawk.gallery.GalleryViewModel
import com.hazardhawk.ui.gallery.state.AnalysisWorkflowViewModel
import com.hazardhawk.ui.safety.ptp.PTPListViewModel
import com.hazardhawk.ui.safety.ptp.PTPViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * ViewModel module for Compose UI layer dependencies.
 * ViewModels are scoped to the Compose navigation lifecycle.
 */
val viewModelModule = module {
    
    // Gallery ViewModel for photo management
    viewModel<GalleryViewModel> {
        GalleryViewModel(
            // Dependencies will be injected once repositories are implemented
            // photoRepository = get(),
            // analysisRepository = get()
        )
    }
    
    // Camera ViewModel for photo capture
    // viewModel<CameraViewModel> {
    //     CameraViewModel(
    //         capturePhotoUseCase = get(),
    //         photoStorageManager = get(),
    //         locationService = get()
    //     )
    // }
    
    // Analysis Workflow ViewModel for two-state analysis workflow
    viewModel<AnalysisWorkflowViewModel> {
        AnalysisWorkflowViewModel(
            aiService = get(),
            oshaRepository = get(),
            analysisRepository = get(),
            secureKeyManager = get(),
            metadataSettings = get()
        )
    }

    // PTP List ViewModel for Pre-Task Plan list screen
    viewModel<PTPListViewModel> {
        PTPListViewModel(
            ptpRepository = get()
        )
    }

    // PTP ViewModel for Pre-Task Plan creation, editing, and PDF export
    viewModel<PTPViewModel> {
        PTPViewModel(
            ptpRepository = get(),
            pdfGenerator = get(),
            fileStorageUtil = get()
        )
    }

    // Tag ViewModel for safety tag management
    // viewModel<TagViewModel> {
    //     TagViewModel(
    //         getRecommendedTagsUseCase = get(),
    //         applyTagsUseCase = get(),
    //         tagRepository = get()
    //     )
    // }
    
    // Report ViewModel for safety report generation
    // viewModel<ReportViewModel> {
    //     ReportViewModel(
    //         generateReportUseCase = get(),
    //         exportReportUseCase = get()
    //     )
    // }
    
    // Project ViewModel for project management
    // viewModel<ProjectViewModel> {
    //     ProjectViewModel(
    //         createProjectUseCase = get(),
    //         getProjectsUseCase = get(),
    //         projectRepository = get()
    //     )
    // }
    
    // Settings ViewModel for app configuration
    // viewModel<SettingsViewModel> {
    //     SettingsViewModel(
    //         userRepository = get(),
    //         preferencesManager = get()
    //     )
    // }

    // Main ViewModel for app-level state management
    // viewModel<MainViewModel> {
    //     MainViewModel(
    //         userRepository = get(),
    //         syncManager = get(),
    //         applicationScope = getApplicationScope()
    //     )
    // }

    // Pre-Task Plan ViewModels (registered in PTPModule)
    // viewModel<PTPViewModel> { PTPViewModel(get(), get(), get()) }
    // viewModel<PTPCreationViewModel> { PTPCreationViewModel(get(), get()) }
    // viewModel<PTPEditorViewModel> { PTPEditorViewModel(get(), get()) }
    // viewModel<HazardCorrectionViewModel> { HazardCorrectionViewModel(get(), get()) }
}

/**
 * Shared ViewModels module for cross-screen state management
 */
val sharedViewModelModule = module {
    
    // Shared state ViewModels that persist across navigation
    // These are typically singletons or scoped to the application lifecycle
    
    // Selection ViewModel for multi-photo operations
    // single<PhotoSelectionViewModel> {
    //     PhotoSelectionViewModel()
    // }
    
    // Upload ViewModel for tracking background uploads
    // single<UploadViewModel> {
    //     UploadViewModel(
    //         s3Client = get(),
    //         workManager = get(),
    //         applicationScope = getApplicationScope()
    //     )
    // }
    
    // Sync ViewModel for data synchronization status
    // single<SyncViewModel> {
    //     SyncViewModel(
    //         syncManager = get(),
    //         applicationScope = getApplicationScope()
    //     )
    // }
}
