package com.hazardhawk.di

import com.hazardhawk.data.repositories.*
import com.hazardhawk.domain.repositories.*
import org.koin.dsl.module

/**
 * Repository module for data layer dependencies.
 * Contains repository implementations and data sources.
 */
val repositoryModule = module {
    
    // Basic repository implementations
    single<PhotoRepository> { PhotoRepositoryImpl(get()) }
    single<AnalysisRepository> { AnalysisRepositoryImpl(get()) }
    single<TagRepository> { TagRepositoryImpl(get()) }
    single<SyncRepository> { SyncRepositoryImpl(get()) }
}

/**
 * Local data module for caching and offline storage.
 */
val localDataModule = module {
    // Placeholder for local data dependencies
    // Will be implemented as needed
}
