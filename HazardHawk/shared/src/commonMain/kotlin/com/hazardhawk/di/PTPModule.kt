package com.hazardhawk.di

import com.hazardhawk.data.repositories.ptp.PTPRepository
import com.hazardhawk.data.repositories.ptp.SQLDelightPTPRepository
import com.hazardhawk.documents.PTPPDFGenerator
import com.hazardhawk.domain.services.ptp.GeminiPTPAIService
import com.hazardhawk.domain.services.ptp.PTPAIService
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Pre-Task Plan (PTP) dependency injection module.
 * Provides AI service and repository for PTP feature.
 *
 * Note: This module is in the shared module to access HttpClient from networkModule.
 * ViewModels will be registered in the Android-specific ViewModelModule.
 */
fun ptpModule(apiKeyProvider: () -> String) = module {

    // PTP AI Service - Gemini implementation
    single<PTPAIService> {
        GeminiPTPAIService(
            httpClient = get<HttpClient>(),
            apiKey = apiKeyProvider(),
            json = get<Json>()
        )
    }

    // PTP Repository - SQLDelight implementation
    single<PTPRepository> {
        SQLDelightPTPRepository(
            ptpAIService = get<PTPAIService>(),
            json = get<Json>()
        )
    }

    // PTP PDF Generator - Platform-specific implementation
    single<PTPPDFGenerator> {
        getPlatformPTPPDFGenerator()
    }
}

/**
 * Expect function to get platform-specific PDF generator.
 * Each platform must provide an actual implementation.
 */
expect fun getPlatformPTPPDFGenerator(): PTPPDFGenerator
