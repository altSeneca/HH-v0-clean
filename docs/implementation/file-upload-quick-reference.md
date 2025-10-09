# FileUploadService - Quick Reference

## Setup (Koin)

```kotlin
// In your app initialization
startKoin {
    modules(
        storageModule(
            s3Bucket = "your-bucket-name",
            cdnBaseUrl = "https://cdn.example.com", // Optional
            backendApiUrl = "https://api.example.com"
        )
    )
}
```

## Basic Usage

```kotlin
class YourViewModel(
    private val fileUploadService: FileUploadService
) : ViewModel() {

    fun uploadFile(fileBytes: ByteArray, fileName: String) {
        viewModelScope.launch {
            val result = fileUploadService.uploadFile(
                file = fileBytes,
                fileName = fileName,
                contentType = "image/jpeg",
                onProgress = { progress ->
                    println("Progress: ${(progress * 100).toInt()}%")
                }
            )

            result.onSuccess { uploadResult ->
                println("URL: ${uploadResult.url}")
                println("Size: ${uploadResult.sizeBytes}")
            }

            result.onFailure { error ->
                println("Error: ${error.message}")
            }
        }
    }
}
```

## Compress Before Upload

```kotlin
val compressed = fileUploadService.compressImage(
    imageData = originalBytes,
    maxSizeKB = 500
).getOrNull()
```

## Required Backend Endpoints

### 1. Generate Presigned URL
```
POST /api/storage/presigned-url
Body: { bucket, key, contentType, expirationSeconds, operation }
Response: "https://s3.amazonaws.com/..."
```

### 2. Delete File
```
DELETE /api/storage/files
Body: { bucket, key }
Response: 200 OK
```

### 3. Check Existence
```
HEAD /api/storage/files?bucket=X&key=Y
Response: 200 (exists) or 404 (not found)
```

## File Structure in S3

```
bucket/
├── certifications/timestamp-filename.ext
└── thumbnails/timestamp-filename.ext
```

## Features

- ✅ **Retry Logic**: 3 attempts with exponential backoff
- ✅ **Progress Tracking**: Real-time callbacks (0.0 to 1.0)
- ✅ **Image Compression**: Android native (500KB target)
- ✅ **Thumbnails**: Auto-generated (100KB target)
- ✅ **Security**: Uses presigned URLs from backend

## Platform Support

| Platform | Status | Implementation |
|----------|--------|----------------|
| Android  | ✅ Complete | Native bitmap compression |
| iOS      | 🚧 Planned | UIImage compression needed |
| Web      | 🚧 Planned | Canvas API compression needed |

## Error Handling

```kotlin
result.onFailure { error ->
    when (error) {
        is FileUploadException -> handleUploadError(error)
        is java.net.UnknownHostException -> showNoInternet()
        else -> showGenericError(error)
    }
}
```

## Files Location

- **Interface**: `domain/services/FileUploadService.kt`
- **Implementation**: `domain/services/FileUploadServiceImpl.kt`
- **Android Impl**: `androidMain/.../AndroidFileUploadService.kt`
- **S3 Client**: `data/storage/S3Client.kt`
- **HTTP Client**: `data/storage/HttpS3Client.kt`
- **Compression**: `androidMain/.../ImageCompression.kt`
- **DI Module**: `di/StorageModule.kt`

## Dependencies

All required dependencies are already in `shared/build.gradle.kts`:
- Ktor Client
- kotlinx.coroutines
- kotlinx.serialization
- AWS SDK (androidMain)
- ExifInterface (androidMain)

## Integration Checklist

- [ ] Add Koin module to app initialization
- [ ] Implement backend presigned URL endpoint
- [ ] Configure S3 bucket with CORS
- [ ] Create file picker UI
- [ ] Display upload progress
- [ ] Handle success/failure states
- [ ] Save URLs to database
- [ ] Write unit tests
- [ ] Write integration tests

## Support

See full documentation: `domain/services/README.md`
