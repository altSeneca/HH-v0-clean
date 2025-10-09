package com.hazardhawk.di

import com.hazardhawk.data.cloud.S3UploadManager
import com.hazardhawk.domain.services.NotificationService
import com.hazardhawk.domain.services.OCRService
import com.hazardhawk.security.PhotoEncryptionService
import com.hazardhawk.security.SecureStorageService
import io.ktor.client.HttpClient
import org.koin.dsl.module

/**
 * Storage and cloud services module for Phase 2.
 * Provides platform-agnostic storage, upload, OCR, and notification services.
 *
 * Platform-specific implementations should be provided via expect/actual pattern
 * or platform-specific modules (e.g., AndroidStorageModule).
 *
 * @param s3Bucket S3 bucket name for file uploads
 * @param cdnBaseUrl CDN base URL for serving uploaded files
 * @param backendApiUrl Backend API URL for notifications and OCR processing
 */
fun storageModule(
    s3Bucket: String = "",
    cdnBaseUrl: String = "",
    backendApiUrl: String = ""
) = module {

    // S3 Upload Manager with encryption and retry logic
    single<S3UploadManager> {
        S3UploadManager(
            secureStorage = get<SecureStorageService>(),
            encryptionService = get<PhotoEncryptionService>()
        )
    }

    // Platform-specific implementations will be provided by platform modules

    // OCR Service - Platform-specific implementation
    // Expected to be registered in platform-specific modules (e.g., AndroidStorageModule)
    // single<OCRService> {
    //     getPlatformOCRService(
    //         httpClient = get<HttpClient>(),
    //         apiUrl = backendApiUrl
    //     )
    // }

    // Notification Service - Platform-specific implementation
    // Expected to be registered in platform-specific modules (e.g., AndroidStorageModule)
    // single<NotificationService> {
    //     getPlatformNotificationService(
    //         httpClient = get<HttpClient>(),
    //         apiUrl = backendApiUrl
    //     )
    // }
}

/**
 * Expect functions for platform-specific implementations.
 * Each platform (Android, iOS, etc.) must provide actual implementations.
 */

/**
 * Get platform-specific OCR service implementation.
 * - Android: Uses Google ML Kit Document Scanner
 * - iOS: Uses Vision framework
 * - Desktop/Web: Uses backend API
 */
// expect fun getPlatformOCRService(
//     httpClient: HttpClient,
//     apiUrl: String
// ): OCRService

/**
 * Get platform-specific notification service implementation.
 * - Android: Uses Firebase Cloud Messaging + SMS Manager
 * - iOS: Uses APNs + MessageUI
 * - Desktop/Web: Uses backend API only
 */
// expect fun getPlatformNotificationService(
//     httpClient: HttpClient,
//     apiUrl: String
// ): NotificationService
