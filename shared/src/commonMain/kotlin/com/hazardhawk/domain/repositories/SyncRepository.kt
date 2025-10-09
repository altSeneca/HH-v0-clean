package com.hazardhawk.domain.repositories

interface SyncRepository {
    suspend fun syncData(): Result<Unit>
    suspend fun hasPendingSync(): Boolean
}