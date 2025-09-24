package com.hazardhawk.domain.repositories

import com.hazardhawk.core.models.Tag
import com.hazardhawk.core.models.TagCategory
import kotlinx.coroutines.flow.Flow

/**
 * Comprehensive Tag Repository interface with multi-level caching support
 * Supports L1 Memory Cache, L2 Database Cache, and L3 Network Cache
 */
interface TagRepository {
    
    // ===== Core Tag Operations =====
    
    /**
     * Save a tag with automatic cache invalidation
     */
    suspend fun saveTag(tag: Tag): Result<Tag>
    
    /**
     * Get a tag by ID with multi-level caching
     */
    suspend fun getTag(id: String): Tag?
    
    /**
     * Get all tags as a reactive Flow with caching
     */
    suspend fun getAllTags(): Flow<List<Tag>>
    
    /**
     * Get tags by category with L1 cache optimization
     */
    suspend fun getTagsByCategory(category: TagCategory): List<Tag>
    
    /**
     * Delete a tag with cascade cache invalidation
     */
    suspend fun deleteTag(id: String): Result<Unit>
    
    /**
     * Update tag details with OSHA compliance validation
     */
    suspend fun updateTag(tag: Tag): Result<Tag>
    
    /**
     * Update tag usage count and last used timestamp
     */
    suspend fun updateTagUsage(tagId: String, userId: String, projectId: String? = null): Result<Unit>
    
    /**
     * Update custom tag with validation
     */
    suspend fun updateCustomTag(tag: Tag): Result<Tag>
    
    // ===== Search and Auto-complete =====
    
    /**
     * Search tags with auto-complete suggestions (< 100ms target)
     * @param query Search query string
     * @param limit Maximum number of results (default 20)
     * @param includeCustom Include custom tags in results
     * @param categoryFilter Optional category filter
     */
    suspend fun searchTags(
        query: String, 
        limit: Int = 20,
        includeCustom: Boolean = true,
        categoryFilter: TagCategory? = null
    ): List<Tag>
    
    /**
     * Get auto-complete suggestions for tag names
     * Uses L1 cache for sub-100ms responses
     */
    suspend fun getAutocompleteSuggestions(
        prefix: String, 
        limit: Int = 10,
        categoryFilter: TagCategory? = null
    ): List<String>
    
    // ===== Tag Recommendations =====
    
    /**
     * Get recommended tags with ML-powered suggestions
     */
    suspend fun getRecommendedTags(
        userId: String, 
        projectId: String?, 
        limit: Int = 8
    ): List<Tag>
    
    /**
     * Get user's most frequently used tags
     */
    suspend fun getPersonalTopTags(userId: String, limit: Int): List<Tag>
    
    /**
     * Get project's most frequently used tags
     */
    suspend fun getProjectTopTags(projectId: String, limit: Int): List<Tag>
    
    /**
     * Get industry-standard OSHA compliant tags
     */
    suspend fun getIndustryStandardTags(): List<Tag>
    
    /**
     * Get trending tags based on recent usage patterns
     */
    suspend fun getTrendingTags(
        timeWindowDays: Int = 7,
        limit: Int = 10
    ): List<Tag>
    
    /**
     * Get most used tags across all users
     */
    suspend fun getMostUsedTags(limit: Int = 20): List<Tag>
    
    /**
     * Get recently used tags
     */
    suspend fun getRecentTags(limit: Int = 10): List<Tag>
    
    // ===== Tag Analytics and Statistics =====
    
    /**
     * Get usage statistics for a specific tag
     */
    suspend fun getTagUsageStats(tagId: String): Result<TagUsageStats>
    
    /**
     * Get usage statistics for multiple tags (batch operation)
     */
    suspend fun getTagUsageStatsBatch(tagIds: List<String>): Result<Map<String, TagUsageStats>>
    
    /**
     * Get tag usage distribution by category
     */
    suspend fun getTagCategoryDistribution(
        projectId: String? = null,
        timeWindowDays: Int? = null
    ): Map<TagCategory, Int>
    
    // ===== Batch Operations =====
    
    /**
     * Save multiple tags in a single transaction
     */
    suspend fun saveTags(tags: List<Tag>): Result<List<Tag>>
    
    /**
     * Apply multiple tags to a photo efficiently
     */
    suspend fun applyTagsToPhoto(
        photoId: String, 
        tagIds: List<String>, 
        userId: String
    ): Result<Unit>
    
    /**
     * Remove multiple tags from a photo
     */
    suspend fun removeTagsFromPhoto(
        photoId: String, 
        tagIds: List<String>
    ): Result<Unit>
    
    /**
     * Increment usage count for multiple tags atomically
     */
    suspend fun incrementTagUsageBatch(
        tagUsageMap: Map<String, String> // tagId to userId
    ): Result<Unit>
    
    // ===== Custom Tag Management =====
    
    /**
     * Create a custom tag with validation
     */
    suspend fun createCustomTag(
        name: String, 
        category: TagCategory, 
        userId: String, 
        projectId: String?
    ): Result<Tag>
    
    /**
     * Get all custom tags for a user
     */
    suspend fun getUserCustomTags(
        userId: String, 
        projectId: String? = null
    ): List<Tag>
    
    /**
     * Get all custom tags for a project
     */
    suspend fun getProjectCustomTags(projectId: String): List<Tag>
    
    /**
     * Get all custom tags
     */
    suspend fun getCustomTags(): List<Tag>
    
    // ===== OSHA Compliance =====
    
    /**
     * Get OSHA compliant tags only
     */
    suspend fun getOSHACompliantTags(category: TagCategory? = null): List<Tag>
    
    /**
     * Validate tag against OSHA standards
     */
    suspend fun validateTagOSHACompliance(tag: Tag): Result<Boolean>
    
    /**
     * Get OSHA reference information for a tag
     */
    suspend fun getOSHAReferences(tagId: String): List<String>
    
    // ===== Cache Management =====
    
    /**
     * Preload frequently used tags into L1 cache
     */
    suspend fun preloadFrequentTags(userId: String, projectId: String?)
    
    /**
     * Clear all cache levels (for testing/debugging)
     */
    suspend fun clearAllCaches()
    
    /**
     * Clear specific cache level
     */
    suspend fun clearCache(level: CacheLevel)
    
    /**
     * Get cache statistics for monitoring
     */
    suspend fun getCacheStats(): CacheStatistics
    
    // ===== Sync and Offline Support =====
    
    /**
     * Sync tags with remote server
     */
    suspend fun syncTags(): Result<SyncResult>
    
    /**
     * Get tags pending sync to server
     */
    suspend fun getPendingSyncTags(): List<Tag>
    
    /**
     * Mark tags as synced
     */
    suspend fun markTagsAsSynced(tagIds: List<String>): Result<Unit>
    
    // ===== Legacy Methods (for backward compatibility) =====
    
    @Deprecated("Use incrementTagUsageBatch for better performance")
    suspend fun incrementTagUsage(tagId: String, userId: String): Result<Unit>
}

/**
 * Tag usage statistics data class
 */
data class TagUsageStats(
    val tagId: String,
    val totalUsage: Int,
    val usageThisWeek: Int,
    val usageThisMonth: Int,
    val averageUsagePerDay: Double,
    val lastUsedTimestamp: Long?,
    val topUsers: List<String>, // User IDs who use this tag most
    val topProjects: List<String> // Project IDs where this tag is used most
)

/**
 * Cache levels for selective cache management
 */
enum class CacheLevel {
    L1_MEMORY,
    L2_DATABASE,
    L3_NETWORK
}

/**
 * Cache performance statistics
 */
data class CacheStatistics(
    val l1MemoryCacheSize: Int,
    val l1HitRate: Double,
    val l1MissRate: Double,
    val l2DatabaseQueryCount: Long,
    val l2HitRate: Double,
    val l3NetworkRequestCount: Long,
    val l3HitRate: Double,
    val averageResponseTime: Long, // milliseconds
    val cacheEvictionCount: Long
)

/**
 * Sync operation result
 */
data class SyncResult(
    val syncedTags: Int,
    val conflictsResolved: Int,
    val errors: List<String>,
    val syncTimestamp: Long
)