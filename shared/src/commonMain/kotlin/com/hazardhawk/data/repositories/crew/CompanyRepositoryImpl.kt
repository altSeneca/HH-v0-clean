package com.hazardhawk.data.repositories.crew

import com.hazardhawk.domain.repositories.CompanyRepository
import com.hazardhawk.models.crew.Company
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * Implementation of CompanyRepository with in-memory storage.
 * TODO: Replace with actual API client and database integration
 */
class CompanyRepositoryImpl : CompanyRepository {

    // In-memory storage
    private val companies = mutableMapOf<String, Company>()
    private val companiesFlow = MutableStateFlow<List<Company>>(emptyList())

    // ===== Core CRUD Operations =====

    override suspend fun getCompany(companyId: String): Company? {
        return companies[companyId]
    }

    override suspend fun getCompanyBySubdomain(subdomain: String): Company? {
        return companies.values.find { it.subdomain == subdomain }
    }

    override suspend fun updateCompany(
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
                phone = phone ?: company.phone,
                settings = settings ?: company.settings,
                updatedAt = Clock.System.now().toString()
            )

            companies[companyId] = updated
            emitCompaniesUpdate()

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadCompanyLogo(
        companyId: String,
        logoData: ByteArray,
        fileName: String
    ): Result<String> {
        // TODO: Implement S3 upload
        return Result.failure(NotImplementedError("Logo upload not yet implemented"))
    }

    override suspend fun deleteCompanyLogo(companyId: String): Result<Unit> {
        return try {
            val company = companies[companyId]
                ?: return Result.failure(IllegalArgumentException("Company not found"))

            companies[companyId] = company.copy(
                logoUrl = null,
                updatedAt = Clock.System.now().toString()
            )
            emitCompaniesUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Settings Management =====

    override suspend fun getCompanySettings(companyId: String): Map<String, String> {
        return companies[companyId]?.settings ?: emptyMap()
    }

    override suspend fun updateCompanySettings(
        companyId: String,
        settings: Map<String, String>
    ): Result<Unit> {
        return try {
            val company = companies[companyId]
                ?: return Result.failure(IllegalArgumentException("Company not found"))

            companies[companyId] = company.copy(
                settings = settings,
                updatedAt = Clock.System.now().toString()
            )
            emitCompaniesUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSettingValue(
        companyId: String,
        key: String
    ): String? {
        return companies[companyId]?.settings?.get(key)
    }

    override suspend fun updateSetting(
        companyId: String,
        key: String,
        value: String
    ): Result<Unit> {
        return try {
            val company = companies[companyId]
                ?: return Result.failure(IllegalArgumentException("Company not found"))

            val updatedSettings = company.settings.toMutableMap()
            updatedSettings[key] = value

            companies[companyId] = company.copy(
                settings = updatedSettings,
                updatedAt = Clock.System.now().toString()
            )
            emitCompaniesUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Tier Management =====

    override suspend fun getCompanyTier(companyId: String): String? {
        return companies[companyId]?.tier
    }

    override suspend fun updateCompanyTier(
        companyId: String,
        tier: String
    ): Result<Unit> {
        return try {
            val company = companies[companyId]
                ?: return Result.failure(IllegalArgumentException("Company not found"))

            companies[companyId] = company.copy(
                tier = tier,
                updatedAt = Clock.System.now().toString()
            )
            emitCompaniesUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMaxWorkers(companyId: String): Int {
        return companies[companyId]?.maxWorkers ?: 100
    }

    override suspend fun updateMaxWorkers(
        companyId: String,
        maxWorkers: Int
    ): Result<Unit> {
        return try {
            val company = companies[companyId]
                ?: return Result.failure(IllegalArgumentException("Company not found"))

            companies[companyId] = company.copy(
                maxWorkers = maxWorkers,
                updatedAt = Clock.System.now().toString()
            )
            emitCompaniesUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Reactive Queries =====

    override fun observeCompany(companyId: String): Flow<Company?> {
        return companiesFlow.map { allCompanies ->
            allCompanies.find { it.id == companyId }
        }
    }

    // ===== Helper Methods =====

    private fun emitCompaniesUpdate() {
        companiesFlow.value = companies.values.toList()
    }
}
