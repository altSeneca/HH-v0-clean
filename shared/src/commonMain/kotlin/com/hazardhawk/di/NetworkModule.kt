package com.hazardhawk.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Network module for HTTP client and API communication.
 * Contains networking dependencies and configuration.
 */
val networkModule = module {
    
    // JSON serializer for HTTP client
    single<Json> {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = false
        }
    }
    
    // HTTP Client (platform-specific engine will be provided)
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(get<Json>())
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 30000
            }
            install(DefaultRequest) {
                headers.append("Content-Type", "application/json")
                headers.append("Accept", "application/json")
            }
        }
    }
    
    // API client services
    // single<GeminiApiClient> {
    //     GeminiApiClient(
    //         httpClient = get(),
    //         apiKey = get<ApiConfiguration>().geminiApiKey,
    //         baseUrl = "https://generativelanguage.googleapis.com/"
    //     )
    // }
    
    // single<S3Client> {
    //     S3Client(
    //         httpClient = get(),
    //         configuration = get<S3Configuration>()
    //     )
    // }
    
    // single<HazardHawkApiClient> {
    //     HazardHawkApiClient(
    //         httpClient = get(),
    //         baseUrl = get<ApiConfiguration>().baseUrl,
    //         authTokenProvider = get()
    //     )
    // }
    
    // WebSocket client for real-time updates
    // single<WebSocketClient> {
    //     WebSocketClient(
    //         httpClient = get(),
    //         baseUrl = get<ApiConfiguration>().wsBaseUrl
    //     )
    // }
    
    // Network connectivity monitor
    // single<NetworkConnectivityMonitor> {
    //     NetworkConnectivityMonitorImpl(
    //         scope = get<CoroutineScope>()
    //     )
    // }
}

/**
 * API configuration module for endpoint and service settings.
 */
val apiConfigModule = module {
    
    // API configuration
    // single<ApiConfiguration> {
    //     ApiConfiguration(
    //         baseUrl = "https://api.hazardhawk.com",
    //         wsBaseUrl = "wss://ws.hazardhawk.com",
    //         geminiApiKey = get<SecureStorage>().getApiKey("gemini"),
    //         timeout = 30000L,
    //         retryAttempts = 3
    //     )
    // }
    
    // S3 configuration for photo storage
    // single<S3Configuration> {
    //     S3Configuration(
    //         bucketName = "hazardhawk-photos",
    //         region = "us-east-1",
    //         accessKeyId = get<SecureStorage>().getApiKey("aws_access_key"),
    //         secretAccessKey = get<SecureStorage>().getApiKey("aws_secret_key")
    //     )
    // }
    
    // Rate limiting configuration
    // single<RateLimitConfiguration> {
    //     RateLimitConfiguration(
    //         maxRequestsPerMinute = 60,
    //         maxConcurrentRequests = 10,
    //         backoffMultiplier = 2.0
    //     )
    // }
    
    // Request retry configuration
    // single<RetryConfiguration> {
    //     RetryConfiguration(
    //         maxRetries = 3,
    //         initialDelayMs = 1000L,
    //         maxDelayMs = 30000L,
    //         backoffMultiplier = 2.0
    //     )
    // }
}
