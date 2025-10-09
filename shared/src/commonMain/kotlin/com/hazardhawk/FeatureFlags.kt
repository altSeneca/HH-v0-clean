package com.hazardhawk

/**
 * Feature flags for Phase 2 backend integration
 *
 * Controls which features use real API vs mock implementations
 */
object FeatureFlags {
    /**
     * Enable crew service API integration
     * When true: Uses CrewApiRepository with real backend
     * When false: Uses mock implementation
     */
    var API_CREW_ENABLED: Boolean = false

    /**
     * Enable project service API integration
     * When true: Uses ProjectApiRepository with real backend
     * When false: Uses mock implementation
     */
    var API_PROJECT_ENABLED: Boolean = false

    /**
     * Enable company service API integration
     * When true: Uses CompanyApiRepository with real backend
     * When false: Uses mock implementation
     */
    var API_COMPANY_ENABLED: Boolean = false

    /**
     * Enable certification service API integration
     * When true: Uses CertificationApiRepository with real backend
     * When false: Uses mock implementation
     */
    var API_CERTIFICATION_ENABLED: Boolean = false

    /**
     * Enable dashboard service API integration
     * When true: Uses DashboardApiRepository with real backend
     * When false: Uses mock implementation
     */
    var API_DASHBOARD_ENABLED: Boolean = false

    /**
     * Base URL for backend API
     */
    var API_BASE_URL: String = "https://dev-api.hazardhawk.com"

    /**
     * API timeout in milliseconds
     */
    var API_TIMEOUT_MS: Long = 30000L

    /**
     * Enable request/response logging
     */
    var API_LOGGING_ENABLED: Boolean = true

    /**
     * Enable caching for GET requests
     */
    var API_CACHE_ENABLED: Boolean = true

    /**
     * Cache TTL in seconds
     */
    var API_CACHE_TTL_SECONDS: Long = 300L
    
    /**
     * Use real API repositories (not used yet in consolidation)
     */
    var USE_API_REPOSITORIES: Boolean = false
}
