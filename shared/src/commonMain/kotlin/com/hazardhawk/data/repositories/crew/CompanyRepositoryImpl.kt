package com.hazardhawk.data.repositories.crew

import com.hazardhawk.domain.repositories.CompanyRepository
import com.hazardhawk.models.crew.Company
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Implementation of CompanyRepository with in-memory storage.
 * TODO: Replace with actual API client and database integration
 */
class CompanyRepositoryImpl : CompanyRepository {

    // In-memory storage
    private val companies = mutableMapOf<String, Company>()
    private val companiesFlow = MutableStateFlow<List<Company>>(emptyList())
    
    // Separate storage for settings and metadata not in Company model
    private val companySettings = mutableMapOf<String, MutableMap<String, String>>()
    private val companyMaxWorkers = mutableMapOf<String, Int>()

    // ===== Core CRUD Operations =====

    override suspend fun getCompany(companyId: String): Company? {
        return companies[companyId]
    }

    suspend fun getCompanyBySubdomain(subdomain: String): Company? {
        return companies.values.find { it.subdomain == subdomain }
    }

    suspend fun updateCompany(
        companyId: String,
        name: String?,
        address: String?,
        city: String?,
        state: String?,
        zip: String?,
        phone: String?,
        settings: Map<String, String>?
    ): Result<Company> {
        return try {
            val company = companies[companyId]
                ?: return Result.failure(IllegalArgumentException("Company not found: $companyId"))

            val updated = company.copy(
                name = name ?: company.name,
                address = address ?: company.address,
                city = city ?: company.city,
                state = state ?: company.state,
                zip = zip ?: company.zip,
                phone = phone ?: company.phone
            )

            // Store settings separately
            settings?.let {
                companySettings[companyId] = it.toMutableMap()
            }

            companies[companyId] = updated
            emitCompaniesUpdate()

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadCompanyLogo(
        companyId: String,
        logoData: ByteArray,
        fileName: String
    ): Result<String> {
        // TODO: Implement S3 upload
        return Result.failure(NotImplementedError("Logo upload not yet implemented"))
    }

    suspend fun deleteCompanyLogo(companyId: String): Result<Unit> {
        return try {
            val company = companies[companyId]
                ?: return Result.failure(IllegalArgumentException("Company not found"))

            companies[companyId] = company.copy(
                logoUrl = null
            )
            emitCompaniesUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Settings Management =====

    suspend fun getCompanySettings(companyId: String): Map<String, String> {
        return companySettings[companyId] ?: emptyMap()
    }

    suspend fun updateCompanySettings(
        companyId: String,
        settings: Map<String, String>
    ): Result<Unit> {
        return try {
            val company = companies[companyId]
                ?: return Result.failure(IllegalArgumentException("Company not found"))

            companySettings[companyId] = settings.toMutableMap()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSettingValue(
        companyId: String,
        key: String
    ): String? {
        return companySettings[companyId]?.get(key)
    }

    suspend fun updateSetting(
        companyId: String,
        key: String,
        value: String
    ): Result<Unit> {
        return try {
            val company = companies[companyId]
                ?: return Result.failure(IllegalArgumentException("Company not found"))

            val settings = companySettings.getOrPut(companyId) { mutableMapOf() }
            settings[key] = value

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Tier Management =====

    suspend fun getCompanyTier(companyId: String): String? {
        return companies[companyId]?.tier
    }

    suspend fun updateCompanyTier(
        companyId: String,
        tier: String
    ): Result<Unit> {
        return try {
            val company = companies[companyId]
                ?: return Result.failure(IllegalArgumentException("Company not found"))

            companies[companyId] = company.copy(
                tier = tier
            )
            emitCompaniesUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMaxWorkers(companyId: String): Int {
        return companyMaxWorkers[companyId] ?: 100
    }

    suspend fun updateMaxWorkers(
        companyId: String,
        maxWorkers: Int
    ): Result<Unit> {
        return try {
            val company = companies[companyId]
                ?: return Result.failure(IllegalArgumentException("Company not found"))

            companyMaxWorkers[companyId] = maxWorkers

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Reactive Queries =====

    fun observeCompany(companyId: String): Flow<Company?> {
        return companiesFlow.map { allCompanies ->
            allCompanies.find { it.id == companyId }
        }
    }

    // ===== Helper Methods =====

    private fun emitCompaniesUpdate() {
        companiesFlow.value = companies.values.toList()
    }
}
