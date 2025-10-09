package com.hazardhawk.data.repositories

import com.hazardhawk.database.HazardHawkDatabase
import com.hazardhawk.domain.repositories.SyncRepository

class SyncRepositoryImpl(
    private val database: HazardHawkDatabase
) : SyncRepository {
    
    override suspend fun syncData(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun hasPendingSync(): Boolean {
        return false
    }
}