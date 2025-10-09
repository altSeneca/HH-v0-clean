package com.hazardhawk.di

import com.hazardhawk.data.storage.HttpS3Client
import com.hazardhawk.data.storage.S3Client
import com.hazardhawk.domain.services.FileUploadService
import com.hazardhawk.domain.services.FileUploadServiceImpl
import io.ktor.client.*
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin dependency injection module for storage and file upload services.
 * Provides configuration for S3 client and file upload service.
 */
fun storageModule(
    s3Bucket: String,
    cdnBaseUrl: String? = null,
    backendApiUrl: String
): Module = module {

    // Provide HTTP-based S3 client (platform-agnostic)
    single<S3Client> {
        HttpS3Client(
            httpClient = get<HttpClient>(),
            backendApiUrl = backendApiUrl
        )
    }

    // Provide file upload service
    // Note: Platform-specific implementations (AndroidFileUploadService)
    // should be provided in platform-specific modules to enable
    // Android-specific image compression
    single<FileUploadService> {
        FileUploadServiceImpl(
            s3Client = get(),
            bucket = s3Bucket,
            cdnBaseUrl = cdnBaseUrl
        )
    }
}

/**
 * Creates a storage module with default configuration.
 * Can be customized by providing different parameters.
 *
 * Example usage in app initialization:
 * ```
 * startKoin {
 *     modules(
 *         storageModule(
 *             s3Bucket = "hazardhawk-certifications",
 *             cdnBaseUrl = "https://cdn.hazardhawk.com",
 *             backendApiUrl = "https://api.hazardhawk.com"
 *         )
 *     )
 * }
 * ```
 */
object StorageConfig {
    const val DEFAULT_BUCKET = "hazardhawk-storage"
    const val DEFAULT_CDN_URL = null // Use S3 URLs directly
}
