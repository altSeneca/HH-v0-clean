package com.hazardhawk.di

import com.hazardhawk.data.storage.AndroidS3Client
import com.hazardhawk.data.storage.S3Client
import com.hazardhawk.domain.services.AndroidFileUploadService
import com.hazardhawk.domain.services.FileUploadConfig
import com.hazardhawk.domain.services.FileUploadService
import org.koin.dsl.module

/**
 * Android-specific storage module with native Android implementations.
 * Provides AndroidFileUploadService with platform-specific image compression.
 *
 * Use this module instead of the common storageModule when you want to
 * leverage Android-specific optimizations.
 */
fun androidStorageModule(
    s3Bucket: String,
    cdnBaseUrl: String? = null,
    awsRegion: String = "us-east-1",
    awsAccessKeyId: String? = null,
    awsSecretAccessKey: String? = null,
    useDirectS3: Boolean = false
): org.koin.core.module.Module = module {

    // Provide Android-specific S3 client (if using direct AWS SDK)
    if (useDirectS3) {
        single<S3Client> {
            AndroidS3Client(
                region = awsRegion,
                accessKeyId = awsAccessKeyId,
                secretAccessKey = awsSecretAccessKey
            )
        }
    }

    // Provide Android-specific file upload service with native image compression
    single<FileUploadService> {
        AndroidFileUploadService(
            s3Client = get(),
            config = FileUploadConfig(
                bucket = s3Bucket,
                uploadPath = "uploads"
            )
        )
    }
}

/**
 * Android storage configuration defaults.
 */
object AndroidStorageConfig {
    const val DEFAULT_AWS_REGION = "us-east-1"
    const val DEFAULT_USE_DIRECT_S3 = false // Use HTTP client by default (more secure)
}
