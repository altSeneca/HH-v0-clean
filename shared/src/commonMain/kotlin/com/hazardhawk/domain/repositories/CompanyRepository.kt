package com.hazardhawk.domain.repositories

import com.hazardhawk.models.crew.Company
import kotlinx.coroutines.flow.Flow

/**
 * Repository for company management - centralized source of truth for company information.
 * Company info is reused across all safety documents (PTPs, reports, etc.).
 */
interface CompanyRepository {

    // ===== Core CRUD Operations =====

    /**
     * Get company by ID
     */
    suspend fun getCompany(companyId: String): Company?

    /**
     * Get company by subdomain
     */
    suspend fun getCompanyBySubdomain(subdomain: String): Company?

    /**
     * Update company information
     */
    suspend fun updateCompany(
        companyId: String,
        name: String? = null,
        address: String? = null,
        city: String? = null,
        state: String? = null,
        zip: String? = null,
        phone: String? = null,
        settings: Map<String, String>? = null
    ): Result<Company>

    /**
     * Upload company logo
     */
    suspend fun uploadCompanyLogo(
        companyId: String,
        logoData: ByteArray,
        fileName: String
    ): Result<String> // Returns logo URL

    /**
     * Delete company logo
     */
    suspend fun deleteCompanyLogo(companyId: String): Result<Unit>

    // ===== Settings Management =====

    /**
     * Get company settings
     */
    suspend fun getCompanySettings(companyId: String): Map<String, String>

    /**
     * Update company settings
     */
    suspend fun updateCompanySettings(
        companyId: String,
        settings: Map<String, String>
    ): Result<Unit>

    /**
     * Get specific setting value
     */
    suspend fun getSettingValue(
        companyId: String,
        key: String
    ): String?

    /**
     * Update specific setting
     */
    suspend fun updateSetting(
        companyId: String,
        key: String,
        value: String
    ): Result<Unit>

    // ===== Tier Management =====

    /**
     * Get company tier
     */
    suspend fun getCompanyTier(companyId: String): String?

    /**
     * Update company tier
     */
    suspend fun updateCompanyTier(
        companyId: String,
        tier: String
    ): Result<Unit>

    /**
     * Get max workers for company
     */
    suspend fun getMaxWorkers(companyId: String): Int

    /**
     * Update max workers limit
     */
    suspend fun updateMaxWorkers(
        companyId: String,
        maxWorkers: Int
    ): Result<Unit>

    // ===== Reactive Queries =====

    /**
     * Observe company changes
     */
    fun observeCompany(companyId: String): Flow<Company?>
}
