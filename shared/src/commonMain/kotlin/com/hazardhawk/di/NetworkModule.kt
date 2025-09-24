package com.hazardhawk.di

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Network module for HTTP client configuration and API services.
 * Uses Ktor client for cross-platform networking support.
 */
val networkModule = module {
    
    // JSON configuration for API serialization
    single {
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    
    // Main HTTP client for API communication
    single<HttpClient> {
        HttpClient(get<HttpClientEngine>()) {
            
            // Content negotiation for JSON API responses
            install(ContentNegotiation) {
                json(get<Json>())
            }
            
            // Default request configuration
            defaultRequest {
                contentType(ContentType.Application.Json)
                host = "api.hazardhawk.com" // This should come from build config
                url {
                    protocol = URLProtocol.HTTPS
                }
            }
            
            // Request/response logging for debugging
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
                filter { request ->
                    !request.url.encodedPath.contains("auth") // Don't log auth requests
                }
            }
            
            // Authentication configuration
            install(Auth) {
                bearer {
                    loadTokens {
                        // Load tokens from secure storage
                        // This will be implemented with platform-specific secure storage
                        null
                    }
                    refreshTokens {
                        // Refresh token logic
                        null
                    }
                }
            }
            
            // Request timeout configuration
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }
            
            // Retry failed requests
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 2)
                retryOnException(maxRetries = 2, retryOnTimeout = true)
                exponentialDelay()
            }
        }
    }
    
    // AI API client for Gemini Vision API
    // single<GeminiVisionClient> {
    //     GeminiVisionClient(
    //         httpClient = get(),
    //         apiKey = get(qualifier = named("GeminiApiKey"))
    //     )
    // }
    
    // AWS S3 client for photo storage
    // single<S3Client> {
    //     S3Client(
    //         httpClient = get(),
    //         config = get<S3Config>()
    //     )
    // }
    
    // Backend API service
    // single<HazardHawkApiService> {
    //     HazardHawkApiService(
    //         httpClient = get()
    //     )
    // }
}

/**
 * API configuration module for service-specific settings
 */
val apiConfigModule = module {
    
    // API endpoint configurations
    single(qualifier = org.koin.core.qualifier.named("ApiBaseUrl")) { "https://api.hazardhawk.com" }
    single(qualifier = org.koin.core.qualifier.named("ApiVersion")) { "v1" }
    
    // API timeout configurations
    single(qualifier = org.koin.core.qualifier.named("ApiTimeoutMs")) { 30_000L }
    single(qualifier = org.koin.core.qualifier.named("ApiRetryCount")) { 3 }
    
    // S3 configuration (will be provided by platform-specific modules)
    // single<S3Config> {
    //     S3Config(
    //         bucketName = get(qualifier = named("S3BucketName")),
    //         region = get(qualifier = named("S3Region")),
    //         accessKey = get(qualifier = named("S3AccessKey")),
    //         secretKey = get(qualifier = named("S3SecretKey"))
    //     )
    // }
}
