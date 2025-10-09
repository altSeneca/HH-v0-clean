package com.hazardhawk.domain.repositories

import com.hazardhawk.models.crew.Company

/**
 * Repository for company data access
 */
interface CompanyRepository {
    /**
     * Get company by ID
     */
    suspend fun getCompany(companyId: String): Company?
}
