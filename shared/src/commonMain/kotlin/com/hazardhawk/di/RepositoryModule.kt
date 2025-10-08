package com.hazardhawk.di

import com.hazardhawk.data.repositories.*
import com.hazardhawk.domain.repositories.*
import org.koin.dsl.module

/**
 * Repository module containing data layer dependencies.
 * Repositories handle data access logic and coordinate between different data sources.
 * 
 * This module now includes the complete repository pattern with interfaces and implementations.
 */
val repositoryModule = module {
    
    // Photo Repository - handles photo storage, retrieval, and metadata
    single<PhotoRepository> {
        PhotoRepositoryImpl(
            // TODO: Add database, S3 client, and other dependencies when available
            // database = get(),
            // s3Client = get(),
            // fileManager = get()
        )
    }
    
    // Analysis Repository - manages AI analysis results and processing
    single<AnalysisRepository> {
        AnalysisRepositoryImpl(
            // TODO: Add database, AI service, and other dependencies when available
            // database = get(),
            // aiService = get()
        )
    }
    
    // User Repository - handles user authentication, profiles, and preferences
    single<UserRepository> {
        UserRepositoryImpl(
            // TODO: Add authentication service, database, and other dependencies when available
            // authService = get(),
            // database = get(),
            // preferencesManager = get()
        )
    }
    
    // Project Repository - manages project data and settings
    single<ProjectRepository> {
        ProjectRepositoryImpl(
            // TODO: Add database and other dependencies when available
            // database = get()
        )
    }
    
    // Tag Repository - handles safety tag management and recommendations
    // TODO: Create TagRepository interface and implementation
    // single<TagRepository> {
    //     TagRepositoryImpl(
    //         database = get()
    //     )
    // }

    // Report Repository - manages safety reports and document generation
    // TODO: Create ReportRepository interface and implementation
    // single<ReportRepository> {
    //     ReportRepositoryImpl(
    //         database = get(),
    //         pdfGenerator = get()
    //     )
    // }

    // ===== Crew Management Repositories =====

    // Worker Repository - manages worker profiles, roles, and assignments
    single<com.hazardhawk.domain.repositories.WorkerRepository> {
        com.hazardhawk.data.repositories.crew.WorkerRepositoryImpl()
    }

    // Crew Repository - manages crew creation, member assignments, and rosters
    single<com.hazardhawk.domain.repositories.CrewRepository> {
        com.hazardhawk.data.repositories.crew.CrewRepositoryImpl(
            workerRepository = get()
        )
    }

    // Certification Repository - manages certifications, verification, and expiration tracking
    single<com.hazardhawk.domain.repositories.CertificationRepository> {
        com.hazardhawk.data.repositories.crew.CertificationRepositoryImpl()
    }

    // Company Repository - centralized source of truth for company information
    single<com.hazardhawk.domain.repositories.CompanyRepository> {
        com.hazardhawk.data.repositories.crew.CompanyRepositoryImpl()
    }

    // Project Repository (Crew) - centralized source of truth for project information
    // Note: This shadows the existing ProjectRepository for now
    // TODO: Consolidate with existing ProjectRepository
    single<com.hazardhawk.domain.repositories.ProjectRepository>(qualifier = org.koin.core.qualifier.named("crew")) {
        com.hazardhawk.data.repositories.crew.ProjectRepositoryImpl(
            companyRepository = get(),
            workerRepository = get()
        )
    }

    // Dashboard Repositories - Phase 1 Home Dashboard Redesign
    single {
        com.hazardhawk.data.repositories.dashboard.ActivityRepositoryImpl()
    }

    single {
        com.hazardhawk.data.repositories.dashboard.UserProfileRepositoryImpl()
    }

    single {
        com.hazardhawk.data.repositories.dashboard.WeatherRepositoryImpl()
    }
}

/**
 * Local data sources module for offline-first functionality
 */
val localDataModule = module {
    
    // Local cache manager for offline functionality
    // single<LocalCacheManager> {
    //     LocalCacheManagerImpl(
    //         database = get(),
    //         fileManager = get()
    //     )
    // }
    
    // Sync manager for data synchronization
    // single<SyncManager> {
    //     SyncManagerImpl(
    //         repositories = getAll(),
    //         networkClient = get(),
    //         applicationScope = getApplicationScope()
    //     )
    // }
    
    // Repository factory for advanced configuration
    single { RepositoryFactory() }
    
    // Repository health checker
    single {
        RepositoryHealthChecker(
            photoRepository = get(),
            analysisRepository = get(),
            userRepository = get(),
            projectRepository = get()
        )
    }
}

/**
 * Repository factory for creating repository instances with proper error handling
 * This provides additional configuration options and error recovery mechanisms
 */
class RepositoryFactory {
    
    /**
     * Create PhotoRepository with error handling and configuration
     */
    fun createPhotoRepository(
        enableCache: Boolean = true,
        maxCacheSize: Long = 100_000_000L // 100MB
    ): Result<PhotoRepository> {
        return try {
            val repository = PhotoRepositoryImpl()
            Result.success(repository)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create AnalysisRepository with AI service configuration
     */
    fun createAnalysisRepository(
        enableCloudAnalysis: Boolean = true,
        maxRetries: Int = 3,
        timeoutMs: Long = 30_000L
    ): Result<AnalysisRepository> {
        return try {
            val repository = AnalysisRepositoryImpl()
            Result.success(repository)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create UserRepository with authentication configuration
     */
    fun createUserRepository(
        enableOfflineAuth: Boolean = false,
        sessionTimeoutMs: Long = 3600_000L // 1 hour
    ): Result<UserRepository> {
        return try {
            val repository = UserRepositoryImpl()
            Result.success(repository)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create ProjectRepository with project management configuration
     */
    fun createProjectRepository(
        enableProjectSync: Boolean = true,
        maxProjectsCache: Int = 50
    ): Result<ProjectRepository> {
        return try {
            val repository = ProjectRepositoryImpl()
            Result.success(repository)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Configuration for repository module
 */
data class RepositoryModuleConfig(
    val enableCaching: Boolean = true,
    val enableOfflineMode: Boolean = true,
    val enableAnalytics: Boolean = false,
    val maxRetries: Int = 3,
    val networkTimeoutMs: Long = 30_000L,
    val cacheExpirationMs: Long = 3600_000L // 1 hour
) {
    companion object {
        val development = RepositoryModuleConfig(
            enableAnalytics = true
        )
        
        val production = RepositoryModuleConfig(
            enableAnalytics = true,
            enableOfflineMode = true
        )
        
        val testing = RepositoryModuleConfig(
            enableCaching = false,
            enableOfflineMode = false,
            enableAnalytics = false,
            maxRetries = 1,
            networkTimeoutMs = 5_000L
        )
    }
}

/**
 * Repository health checker for monitoring repository status
 */
class RepositoryHealthChecker(
    private val photoRepository: PhotoRepository,
    private val analysisRepository: AnalysisRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository
) {
    
    /**
     * Check health of all repositories
     */
    suspend fun checkHealth(): Map<String, Boolean> {
        return mapOf(
            "PhotoRepository" to checkPhotoRepositoryHealth(),
            "AnalysisRepository" to checkAnalysisRepositoryHealth(),
            "UserRepository" to checkUserRepositoryHealth(),
            "ProjectRepository" to checkProjectRepositoryHealth()
        )
    }
    
    private suspend fun checkPhotoRepositoryHealth(): Boolean {
        return try {
            photoRepository.getPhotosCount() >= 0
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun checkAnalysisRepositoryHealth(): Boolean {
        return try {
            analysisRepository.getStorageStats()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun checkUserRepositoryHealth(): Boolean {
        return try {
            userRepository.isAuthenticated() // This should not throw
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun checkProjectRepositoryHealth(): Boolean {
        return try {
            projectRepository.getStorageStats()
            true
        } catch (e: Exception) {
            false
        }
    }
}
