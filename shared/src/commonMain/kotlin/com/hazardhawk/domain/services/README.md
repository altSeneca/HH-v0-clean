# File Upload Service

## Overview

The FileUploadService provides a robust solution for uploading certification documents and images to S3-compatible cloud storage with automatic retry logic, progress tracking, and image compression.

## Architecture

### Components

1. **FileUploadService** (Interface)
   - Main service interface for file uploads
   - Location: `domain/services/FileUploadService.kt`

2. **FileUploadServiceImpl** (Base Implementation)
   - Platform-agnostic implementation with retry logic
   - Location: `domain/services/FileUploadServiceImpl.kt`

3. **AndroidFileUploadService** (Android-specific)
   - Extends base implementation with Android native image compression
   - Location: `androidMain/kotlin/.../services/AndroidFileUploadService.kt`

4. **S3Client** (Interface)
   - Abstraction for S3 operations
   - Location: `data/storage/S3Client.kt`

5. **HttpS3Client** (HTTP Implementation)
   - Uses Ktor for presigned URL-based uploads (recommended)
   - Location: `data/storage/HttpS3Client.kt`

6. **AndroidS3Client** (AWS SDK Implementation)
   - Direct AWS SDK integration (placeholder)
   - Location: `androidMain/kotlin/.../storage/AndroidS3Client.kt`

## Features

### 1. Automatic Retry Logic
- Retries failed uploads up to 3 times
- Exponential backoff (1s, 2s, 4s delays)
- Maximum delay capped at 8 seconds

### 2. Progress Tracking
- Real-time upload progress callbacks
- Supports chunked uploads for large files
- Progress ranges from 0.0 to 1.0

### 3. Image Compression
- Android: Native bitmap compression with EXIF orientation correction
- Target size: 500KB (configurable)
- Automatic quality adjustment
- Fallback to scaling if quality reduction insufficient

### 4. Thumbnail Generation
- Automatic thumbnail creation for images
- Target size: 100KB
- Maintains aspect ratio
- Non-blocking (failures don't affect main upload)

### 5. Security
- Uses presigned URLs from backend (recommended approach)
- Credentials never exposed to client
- Time-limited upload URLs (default: 1 hour)

## Usage

### Basic Setup (Koin)

```kotlin
// In your app initialization
startKoin {
    modules(
        // For platform-agnostic implementation (recommended)
        storageModule(
            s3Bucket = "hazardhawk-certifications",
            cdnBaseUrl = "https://cdn.hazardhawk.com",
            backendApiUrl = "https://api.hazardhawk.com"
        ),

        // OR for Android-specific with native compression
        androidStorageModule(
            s3Bucket = "hazardhawk-certifications",
            cdnBaseUrl = "https://cdn.hazardhawk.com"
        )
    )
}
```

### Upload a File

```kotlin
class CertificationUploadViewModel(
    private val fileUploadService: FileUploadService
) : ViewModel() {

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress

    private val _uploadResult = MutableStateFlow<Result<UploadResult>?>(null)
    val uploadResult: StateFlow<Result<UploadResult>?> = _uploadResult

    fun uploadCertification(fileUri: Uri) {
        viewModelScope.launch {
            try {
                // Read file bytes (platform-specific)
                val fileBytes = readFileBytes(fileUri)
                val fileName = getFileName(fileUri)
                val contentType = getContentType(fileUri)

                // Upload with progress tracking
                val result = fileUploadService.uploadFile(
                    file = fileBytes,
                    fileName = fileName,
                    contentType = contentType,
                    onProgress = { progress ->
                        _uploadProgress.value = progress
                    }
                )

                _uploadResult.value = result

                result.onSuccess { uploadResult ->
                    println("Upload successful!")
                    println("URL: ${uploadResult.url}")
                    println("Thumbnail: ${uploadResult.thumbnailUrl}")
                    println("Size: ${uploadResult.sizeBytes} bytes")
                }

                result.onFailure { error ->
                    println("Upload failed: ${error.message}")
                }

            } catch (e: Exception) {
                _uploadResult.value = Result.failure(e)
            }
        }
    }
}
```

### Compress Image Before Upload

```kotlin
// Compress image manually before uploading
val compressedImage = fileUploadService.compressImage(
    imageData = originalImageBytes,
    maxSizeKB = 500
).getOrNull()

if (compressedImage != null) {
    // Upload compressed image
    fileUploadService.uploadFile(
        file = compressedImage,
        fileName = "certificate.jpg",
        contentType = "image/jpeg"
    )
}
```

### UI Integration (Jetpack Compose)

```kotlin
@Composable
fun CertificationUploadScreen(
    viewModel: CertificationUploadViewModel
) {
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val uploadResult by viewModel.uploadResult.collectAsState()

    Column {
        Button(onClick = { /* Open file picker */ }) {
            Text("Select Certificate")
        }

        if (uploadProgress > 0f) {
            LinearProgressIndicator(
                progress = uploadProgress,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Uploading: ${(uploadProgress * 100).toInt()}%")
        }

        uploadResult?.let { result ->
            result.onSuccess { upload ->
                Text("Upload successful!")
                Text("Size: ${upload.sizeBytes / 1024}KB")

                // Display thumbnail if available
                upload.thumbnailUrl?.let { thumbnailUrl ->
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = "Certificate thumbnail"
                    )
                }
            }

            result.onFailure { error ->
                Text(
                    text = "Upload failed: ${error.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
```

## Backend API Requirements

The backend API must provide the following endpoints:

### 1. Generate Presigned Upload URL

```
POST /api/storage/presigned-url
Content-Type: application/json

{
  "bucket": "hazardhawk-certifications",
  "key": "certifications/1234567890-certificate.pdf",
  "contentType": "application/pdf",
  "expirationSeconds": 3600,
  "operation": "PUT"
}

Response: "https://s3.amazonaws.com/bucket/key?..."
```

### 2. Delete File

```
DELETE /api/storage/files
Content-Type: application/json

{
  "bucket": "hazardhawk-certifications",
  "key": "certifications/1234567890-certificate.pdf"
}

Response: 200 OK
```

### 3. Check File Existence

```
HEAD /api/storage/files?bucket=hazardhawk-certifications&key=certifications/...

Response: 200 OK (exists) or 404 Not Found
```

## Storage Structure

Files are organized in S3 with the following structure:

```
hazardhawk-certifications/
├── certifications/
│   ├── 1696800000000-osha-30.pdf
│   ├── 1696800001000-forklift-cert.jpg
│   └── ...
└── thumbnails/
    ├── 1696800001000-forklift-cert.jpg
    └── ...
```

## Error Handling

The service uses Kotlin's `Result` type for error handling:

```kotlin
result.onSuccess { uploadResult ->
    // Handle success
}

result.onFailure { error ->
    when (error) {
        is FileUploadException -> {
            // Handle upload-specific errors
        }
        is java.net.UnknownHostException -> {
            // Handle network errors
        }
        else -> {
            // Handle other errors
        }
    }
}
```

## Configuration

### Storage Module Configuration

```kotlin
storageModule(
    s3Bucket = "hazardhawk-certifications",
    cdnBaseUrl = "https://cdn.hazardhawk.com", // Optional
    backendApiUrl = "https://api.hazardhawk.com"
)
```

### Android-Specific Configuration

```kotlin
androidStorageModule(
    s3Bucket = "hazardhawk-certifications",
    cdnBaseUrl = "https://cdn.hazardhawk.com",
    awsRegion = "us-east-1",
    useDirectS3 = false // Use HTTP client for security
)
```

## Platform-Specific Notes

### Android
- Uses `ImageCompression` utility for native bitmap processing
- Handles EXIF orientation automatically
- Supports JPEG compression with quality adjustment
- Falls back to scaling if quality reduction insufficient

### iOS (Future)
- Will use `UIImage` for compression
- Should implement similar EXIF handling
- Can use `CGImage` for efficient processing

### Web (Future)
- Use Canvas API for image compression
- Consider WebAssembly for performance
- May need different approach for large files

## Testing

See `androidApp/src/test/java/com/hazardhawk/data/` for test examples.

## Dependencies

- Ktor Client (HTTP operations)
- kotlinx.coroutines (Async operations)
- kotlinx.serialization (Data serialization)
- AWS SDK for Kotlin (Optional, Android direct S3)
- ExifInterface (Android EXIF handling)

## Integration Points

### For UI Developers (android-developer agent)
- Use `FileUploadService` via Koin injection
- Implement file picker integration
- Display upload progress with `onProgress` callback
- Handle `Result<UploadResult>` in UI state

### For Backend Developers
- Implement presigned URL generation endpoint
- Configure S3 bucket with CORS
- Set up CloudFront CDN (optional)
- Implement file deletion endpoint

### For Test Developers (test-guardian agent)
- Mock `FileUploadService` for unit tests
- Test retry logic with simulated failures
- Test progress tracking callbacks
- Test compression with various image sizes
