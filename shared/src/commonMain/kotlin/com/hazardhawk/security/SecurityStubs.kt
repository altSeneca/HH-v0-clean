package com.hazardhawk.security

import kotlinx.datetime.Clock

/**
 * Stub implementations for Phase 2 development
 * These provide working implementations of the full security interfaces for testing.
 * They implement the full interfaces defined in separate files:
 * - AuditLogger.kt
 * - SecureStorageService.kt
 * - PhotoEncryptionService.kt
 *
 * Note: Simplified APIs (getString/setString/encryptData/logEvent) are provided
 * via extension methods in SecurityExtensions.kt
 */

/**
 * Stub implementation of AuditLogger that does nothing
 */
class StubAuditLogger : AuditLogger {
    override suspend fun logSafetyAction(action: SafetyAction): Result<Unit> {
        // Stub implementation - logs would be stored in actual implementation
        return Result.success(Unit)
    }

    override suspend fun logComplianceEvent(event: ComplianceEvent): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun logSecurityEvent(event: SecurityEvent): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun logDataAccessEvent(event: DataAccessEvent): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun generateAuditReport(
        dateRange: DateRange,
        reportType: AuditReportType
    ): Result<AuditReport> {
        // Return minimal stub report
        val now = Clock.System.now()
        return Result.success(
            AuditReport(
                id = "stub-report",
                reportType = reportType,
                dateRange = dateRange,
                generatedAt = now,
                generatedBy = "stub",
                safetyActionCount = 0,
                complianceEventCount = 0,
                securityEventCount = 0,
                dataAccessEventCount = 0,
                sections = emptyList(),
                summary = "Stub audit report",
                complianceScore = 1.0,
                criticalIssues = 0,
                recommendations = emptyList()
            )
        )
    }

    override suspend fun queryAuditLogs(
        criteria: AuditQueryCriteria,
        limit: Int,
        offset: Int
    ): Result<AuditLogQueryResult> {
        return Result.success(
            AuditLogQueryResult(
                safetyActions = emptyList(),
                complianceEvents = emptyList(),
                securityEvents = emptyList(),
                dataAccessEvents = emptyList(),
                totalCount = 0,
                hasMore = false
            )
        )
    }

    override suspend fun verifyLogIntegrity(dateRange: DateRange?): Result<LogIntegrityResult> {
        val now = Clock.System.now()
        return Result.success(
            LogIntegrityResult(
                isIntact = true,
                totalLogsChecked = 0,
                corruptedLogs = 0,
                missingLogs = 0,
                lastVerifiedTimestamp = now,
                verificationMethod = "stub"
            )
        )
    }

    override suspend fun archiveOldLogs(cutoffDate: kotlinx.datetime.Instant): Result<ArchivalResult> {
        val now = Clock.System.now()
        return Result.success(
            ArchivalResult(
                archivedLogCount = 0,
                archivedDataSize = 0L,
                oldestArchivedLog = now,
                newestArchivedLog = now,
                archiveLocation = "stub",
                compressionUsed = false
            )
        )
    }

    override suspend fun exportAuditLogs(
        dateRange: DateRange,
        format: ExportFormat
    ): Result<ExportedAuditData> {
        val now = Clock.System.now()
        return Result.success(
            ExportedAuditData(
                format = format,
                data = "{}",
                recordCount = 0,
                exportedAt = now,
                checksum = "stub",
                compressedSize = 0L,
                originalSize = 0L
            )
        )
    }

    override suspend fun getAuditStatistics(): Result<AuditStatistics> {
        val now = Clock.System.now()
        return Result.success(
            AuditStatistics(
                totalSafetyActions = 0L,
                totalComplianceEvents = 0L,
                totalSecurityEvents = 0L,
                totalDataAccessEvents = 0L,
                logsPerDay = 0.0,
                storageUsed = 0L,
                oldestLogDate = now,
                newestLogDate = now,
                integrityLastChecked = now,
                complianceScore = 1.0
            )
        )
    }
}

/**
 * Stub implementation of SecureStorageService that uses in-memory storage
 */
class StubSecureStorageService : SecureStorageService {
    private val storage = mutableMapOf<String, String>()

    override suspend fun storeApiKey(
        key: String,
        value: String,
        metadata: CredentialMetadata?
    ): Result<Unit> {
        storage[key] = value
        return Result.success(Unit)
    }

    override suspend fun getApiKey(key: String): Result<String?> {
        return Result.success(storage[key])
    }

    override suspend fun removeApiKey(key: String): Result<Unit> {
        storage.remove(key)
        return Result.success(Unit)
    }

    override suspend fun clearAllCredentials(): Result<Unit> {
        storage.clear()
        return Result.success(Unit)
    }

    override suspend fun listCredentialKeys(): Result<List<String>> {
        return Result.success(storage.keys.toList())
    }

    override suspend fun isAvailable(): Boolean = true

    override suspend fun getCredentialMetadata(key: String): Result<CredentialMetadata?> {
        return Result.success(null)
    }

    override suspend fun updateCredentialMetadata(
        key: String,
        metadata: CredentialMetadata
    ): Result<Unit> {
        return Result.success(Unit)
    }
}

/**
 * Stub implementation of PhotoEncryptionService that does no actual encryption
 */
class StubPhotoEncryptionService : PhotoEncryptionService {
    override suspend fun encryptPhoto(
        photo: ByteArray,
        photoId: String,
        compressionLevel: Int
    ): Result<EncryptedPhoto> {
        val now = Clock.System.now()
        return Result.success(
            EncryptedPhoto(
                photoId = photoId,
                encryptedData = photo, // No actual encryption in stub
                initializationVector = ByteArray(12),
                authenticationTag = ByteArray(16),
                keyId = "stub_key",
                encryptionAlgorithm = "AES-256-GCM",
                compressionUsed = compressionLevel > 0,
                originalSize = photo.size.toLong(),
                encryptedAt = now,
                integrity = IntegrityMetadata(
                    checksum = "",
                    algorithm = "SHA-256",
                    createdAt = now
                )
            )
        )
    }

    override suspend fun decryptPhoto(encrypted: EncryptedPhoto): Result<ByteArray> {
        return Result.success(encrypted.encryptedData) // No actual decryption in stub
    }

    override fun generateEncryptionKey(keyPurpose: KeyPurpose): ByteArray {
        return ByteArray(32) // 256-bit key
    }

    override suspend fun encryptThumbnail(
        thumbnail: ByteArray,
        photoId: String
    ): Result<EncryptedThumbnail> {
        val now = Clock.System.now()
        return Result.success(
            EncryptedThumbnail(
                photoId = photoId,
                encryptedData = thumbnail,
                initializationVector = ByteArray(12),
                authenticationTag = ByteArray(16),
                keyId = "stub_key",
                originalSize = thumbnail.size.toLong(),
                encryptedAt = now
            )
        )
    }

    override suspend fun decryptThumbnail(encrypted: EncryptedThumbnail): Result<ByteArray> {
        return Result.success(encrypted.encryptedData)
    }

    override suspend fun encryptPhotoBatch(
        photos: List<PhotoToEncrypt>,
        progress: ((current: Int, total: Int) -> Unit)?
    ): Result<List<EncryptedPhoto>> {
        val results = photos.mapIndexed { index, photo ->
            progress?.invoke(index + 1, photos.size)
            encryptPhoto(photo.data, photo.photoId, photo.compressionLevel).getOrThrow()
        }
        return Result.success(results)
    }

    override suspend fun decryptPhotoBatch(
        encryptedPhotos: List<EncryptedPhoto>,
        progress: ((current: Int, total: Int) -> Unit)?
    ): Result<List<ByteArray>> {
        val results = encryptedPhotos.mapIndexed { index, encrypted ->
            progress?.invoke(index + 1, encryptedPhotos.size)
            decryptPhoto(encrypted).getOrThrow()
        }
        return Result.success(results)
    }

    override suspend fun verifyPhotoIntegrity(encrypted: EncryptedPhoto): Boolean {
        return true // Stub always returns valid
    }

    override suspend fun getEncryptionMetrics(): EncryptionMetrics {
        val now = Clock.System.now()
        return EncryptionMetrics(
            totalPhotosEncrypted = 0L,
            totalPhotosDecrypted = 0L,
            averageEncryptionTime = 0L,
            averageDecryptionTime = 0L,
            totalDataEncrypted = 0L,
            hardwareBackedEncryption = false,
            encryptionFailures = 0L,
            integrityFailures = 0L,
            lastKeyRotation = now,
            activeKeyCount = 1
        )
    }

    override suspend fun rotateEncryptionKey(oldKeyId: String): Result<KeyRotationResult> {
        val now = Clock.System.now()
        return Result.success(
            KeyRotationResult(
                newKeyId = "new_stub_key",
                oldKeyId = oldKeyId,
                rotatedAt = now,
                photosToReencrypt = 0
            )
        )
    }
}
