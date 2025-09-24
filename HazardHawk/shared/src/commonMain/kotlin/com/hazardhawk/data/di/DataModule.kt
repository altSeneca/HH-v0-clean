package com.hazardhawk.data.di

import com.hazardhawk.data.repositories.*
import com.hazardhawk.domain.repositories.*
import org.koin.dsl.module

val dataModule = module {
    // Repositories
    single<PhotoRepository> { PhotoRepositoryImpl(get()) }
    single<TagRepository> { TagRepositoryImpl(get()) }
    single<AnalysisRepository> { AnalysisRepositoryImpl(get()) }
    single<SyncRepository> { SyncRepositoryImpl(get()) }
}
