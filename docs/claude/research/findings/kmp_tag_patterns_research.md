# Kotlin Multiplatform (KMP) Tag Management System - Best Practices Research

## Executive Summary

This document provides comprehensive research findings on implementing a tag management system in Kotlin Multiplatform (KMP), based on analysis of the HazardHawk codebase and industry best practices for 2025. The research covers data layer architecture, state management, platform-specific implementations, performance considerations, and provides practical code examples.

## Table of Contents

1. [Data Layer Architecture](#data-layer-architecture)
2. [State Management](#state-management)
3. [Platform-Specific Implementations](#platform-specific-implementations)
4. [Performance Considerations](#performance-considerations)
5. [Code Examples](#code-examples)
6. [Analysis of Current Implementation](#analysis-of-current-implementation)
7. [Industry Best Practices 2025](#industry-best-practices-2025)
8. [Recommendations](#recommendations)

## Data Layer Architecture

### Tag Data Models in commonMain

Based on analysis of the existing HazardHawk implementation and industry patterns, the optimal tag data structure should include:

```kotlin
@Serializable
data class Tag(
    val id: String,
    val name: String,
    val category: TagCategory,
    val usageCount: Int = 0,
    val lastUsed: Long? = null,
    val projectSpecific: Boolean = false,
    val isCustom: Boolean = false,
    val oshaReferences: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Performance enhancement fields
    val searchKeywords: List<String> = emptyList(), // For improved search
    val priority: Int = 0, // For custom ordering
    val metadata: Map<String, String> = emptyMap() // Extensible attributes
)
```

**Key Design Decisions:**

1. **Immutable Data Classes**: Using immutable data classes with `val` properties ensures thread safety and predictable state management across platforms.

2. **Serialization Support**: `@Serializable` annotation enables seamless network and cache serialization across platforms.

3. **Usage Tracking**: `usageCount` and `lastUsed` fields support intelligent recommendation algorithms.

4. **Extensibility**: `metadata` map allows platform-specific extensions without schema changes.

### Repository Pattern Implementation

The repository pattern provides a single source of truth and abstracts data source complexity:

```kotlin
interface TagRepository {
    suspend fun saveTag(tag: Tag): Result<Tag>
    suspend fun getTag(id: String): Tag?
    suspend fun getAllTags(): Flow<List<Tag>>
    suspend fun getTagsByCategory(category: TagCategory): List<Tag>
    suspend fun searchTags(query: String): List<Tag>
    suspend fun getRecommendedTags(userId: String, projectId: String?, limit: Int = 8): List<Tag>
    suspend fun incrementTagUsage(tagId: String, userId: String): Result<Unit>
    suspend fun createCustomTag(name: String, category: TagCategory, userId: String, projectId: String?): Result<Tag>
    
    // Performance-focused methods
    suspend fun getFrequentlyUsedTags(userId: String, limit: Int): Flow<List<Tag>>
    suspend fun getTagsInBatch(tagIds: List<String>): List<Tag>
    suspend fun preloadTagsForCategory(category: TagCategory): Result<Unit>
}
```

### SQLDelight Schema Design Best Practices

Based on the existing HazardHawk implementation and 2025 best practices:

```sql
-- Optimized tags table with performance indexes
CREATE TABLE tags (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    usage_count INTEGER DEFAULT 0,
    last_used INTEGER,
    project_specific INTEGER DEFAULT 0,
    is_custom INTEGER DEFAULT 0,
    osha_references TEXT, -- JSON array as TEXT
    search_keywords TEXT, -- Space-separated keywords for FTS
    priority INTEGER DEFAULT 0,
    metadata TEXT, -- JSON object as TEXT
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Performance optimization indexes
CREATE INDEX idx_tags_category ON tags(category);
CREATE INDEX idx_tags_usage_count ON tags(usage_count DESC);
CREATE INDEX idx_tags_last_used ON tags(last_used DESC);
CREATE INDEX idx_tags_name_search ON tags(name);
CREATE INDEX idx_tags_priority ON tags(priority DESC, usage_count DESC);

-- Full-text search support
CREATE VIRTUAL TABLE tags_fts USING fts5(
    id UNINDEXED,
    name,
    search_keywords,
    content='tags'
);

-- Triggers to maintain FTS index
CREATE TRIGGER tags_fts_insert AFTER INSERT ON tags BEGIN
    INSERT INTO tags_fts(id, name, search_keywords) 
    VALUES (new.id, new.name, new.search_keywords);
END;
```

### Caching Strategies for Tags

**Multi-level Caching Architecture:**

1. **Memory Cache (L1)**: Frequently accessed tags in application memory
2. **Local Database (L2)**: SQLDelight persistent storage
3. **Network Cache (L3)**: Server-side caching with TTL

```kotlin
class TagCacheManager(
    private val memoryCache: MemoryCache<String, Tag>,
    private val localDatabase: HazardHawkDatabase,
    private val networkApi: TagNetworkApi
) {
    private val cachePolicy = CachePolicy(
        ttl = 24.hours,
        maxMemorySize = 1000, // Max tags in memory
        evictionPolicy = LRU
    )
    
    suspend fun getTag(id: String): Tag? {
        // L1: Check memory cache
        memoryCache.get(id)?.let { return it }
        
        // L2: Check local database
        localDatabase.getTag(id)?.let { tag ->
            memoryCache.put(id, tag)
            return tag
        }
        
        // L3: Fetch from network
        return try {
            val tag = networkApi.getTag(id)
            localDatabase.saveTag(tag)
            memoryCache.put(id, tag)
            tag
        } catch (e: Exception) {
            null
        }
    }
}
```

## State Management

### StateFlow/Flow for Reactive Tag Updates

**Reactive Repository Pattern:**

```kotlin
class TagRepositoryImpl(
    private val database: HazardHawkDatabase,
    private val cacheManager: TagCacheManager
) : TagRepository {
    
    private val _tagsFlow = MutableStateFlow<List<Tag>>(emptyList())
    val tagsFlow: StateFlow<List<Tag>> = _tagsFlow.asStateFlow()
    
    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()
    
    override suspend fun getAllTags(): Flow<List<Tag>> {
        return database.tagQueries.getAllTags()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { mapRowToTag(it) } }
            .onEach { tags ->
                _tagsFlow.value = tags
                // Update memory cache
                tags.forEach { tag -> cacheManager.updateMemoryCache(tag) }
            }
    }
    
    override suspend fun incrementTagUsage(tagId: String, userId: String): Result<Unit> {
        return try {
            // Update database
            database.tagQueries.updateTagUsage(
                lastUsed = Clock.System.now().toEpochMilliseconds(),
                tagId = tagId
            )
            
            // Update reactive state
            val updatedTags = _tagsFlow.value.map { tag ->
                if (tag.id == tagId) {
                    tag.copy(
                        usageCount = tag.usageCount + 1,
                        lastUsed = Clock.System.now().toEpochMilliseconds()
                    )
                } else tag
            }
            _tagsFlow.value = updatedTags
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Managing Tag Selection State

**Centralized Selection State Management:**

```kotlin
class TagSelectionStateManager {
    private val _selectedTagIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagIds: StateFlow<Set<String>> = _selectedTagIds.asStateFlow()
    
    private val _selectionMode = MutableStateFlow(SelectionMode.SINGLE)
    val selectionMode: StateFlow<SelectionMode> = _selectionMode.asStateFlow()
    
    fun selectTag(tagId: String) {
        when (_selectionMode.value) {
            SelectionMode.SINGLE -> {
                _selectedTagIds.value = setOf(tagId)
            }
            SelectionMode.MULTIPLE -> {
                _selectedTagIds.value = _selectedTagIds.value + tagId
            }
        }
    }
    
    fun deselectTag(tagId: String) {
        _selectedTagIds.value = _selectedTagIds.value - tagId
    }
    
    fun clearSelection() {
        _selectedTagIds.value = emptySet()
    }
    
    fun setSelectionMode(mode: SelectionMode) {
        _selectionMode.value = mode
        if (mode == SelectionMode.SINGLE && _selectedTagIds.value.size > 1) {
            // Keep only the first selected tag
            _selectedTagIds.value = _selectedTagIds.value.take(1).toSet()
        }
    }
}

enum class SelectionMode {
    SINGLE, MULTIPLE
}
```

### Usage Counter and Learning Algorithm Implementation

**Smart Tag Recommendation Engine:**

```kotlin
class TagRecommendationEngine(
    private val tagRepository: TagRepository
) {
    companion object {
        private const val PERSONAL_WEIGHT = 0.4
        private const val PROJECT_WEIGHT = 0.3
        private const val INDUSTRY_WEIGHT = 0.3
        private const val RECENCY_BOOST = 0.2
        private val RECENCY_WINDOW = 7.days
    }
    
    suspend fun getRecommendedTags(
        userId: String, 
        projectId: String,
        context: TagContext = TagContext.PHOTO_TAGGING
    ): List<Tag> {
        val personalTags = tagRepository.getPersonalTopTags(userId, 10)
        val projectTags = tagRepository.getProjectTopTags(projectId, 10)
        val industryTags = tagRepository.getIndustryStandardTags()
        val recentTags = tagRepository.getRecentlyUsedTags(userId, 5)
        
        // Apply machine learning algorithm
        return weightedRecommendationAlgorithm(
            personalTags = personalTags to PERSONAL_WEIGHT,
            projectTags = projectTags to PROJECT_WEIGHT,
            industryTags = industryTags to INDUSTRY_WEIGHT,
            recentTags = recentTags,
            context = context
        )
    }
    
    private fun weightedRecommendationAlgorithm(
        personalTags: Pair<List<Tag>, Double>,
        projectTags: Pair<List<Tag>, Double>,
        industryTags: Pair<List<Tag>, Double>,
        recentTags: List<Tag>,
        context: TagContext
    ): List<Tag> {
        val tagScores = mutableMapOf<String, Double>()
        val recentTagIds = recentTags.map { it.id }.toSet()
        
        // Score personal tags
        personalTags.first.forEachIndexed { index, tag ->
            val positionScore = (personalTags.first.size - index).toDouble() / personalTags.first.size
            var score = positionScore * personalTags.second
            
            // Apply recency boost
            if (tag.id in recentTagIds) {
                score += RECENCY_BOOST
            }
            
            // Context-based boost
            score += getContextScore(tag, context)
            
            tagScores[tag.id] = score
        }
        
        // Similar logic for project and industry tags...
        
        return tagScores.entries
            .sortedByDescending { it.value }
            .take(8)
            .mapNotNull { entry -> 
                (personalTags.first + projectTags.first + industryTags.first)
                    .find { it.id == entry.key }
            }
    }
    
    private fun getContextScore(tag: Tag, context: TagContext): Double {
        return when (context) {
            TagContext.PHOTO_TAGGING -> {
                when (tag.category) {
                    TagCategory.PPE -> 0.1
                    TagCategory.FALL_PROTECTION -> 0.15
                    TagCategory.ELECTRICAL -> 0.1
                    else -> 0.0
                }
            }
            TagContext.INCIDENT_REPORT -> {
                when (tag.category) {
                    TagCategory.EQUIPMENT -> 0.2
                    TagCategory.HOT_WORK -> 0.15
                    else -> 0.0
                }
            }
        }
    }
}

enum class TagContext {
    PHOTO_TAGGING,
    INCIDENT_REPORT,
    PRE_TASK_PLANNING,
    TOOLBOX_TALK
}
```

## Platform-Specific Implementations

### Android: SQLDelight Implementation

```kotlin
// androidMain
class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = HazardHawkDatabase.Schema,
            context = context,
            name = "hazardhawk.db",
            callback = object : AndroidSqliteDriver.Callback(HazardHawkDatabase.Schema) {
                override fun onConfigure(connection: SupportSQLiteDatabase) {
                    super.onConfigure(connection)
                    connection.setForeignKeyConstraintsEnabled(true)
                    // Enable Write-Ahead Logging for better performance
                    connection.enableWriteAheadLogging()
                }
            }
        )
    }
}

class AndroidTagStorageManager(
    private val context: Context
) : TagStorageManager {
    override suspend fun exportTags(tags: List<Tag>): File {
        val exportFile = File(context.cacheDir, "tags_export_${System.currentTimeMillis()}.json")
        exportFile.writeText(Json.encodeToString(tags))
        return exportFile
    }
    
    override suspend fun importTags(file: File): List<Tag> {
        return Json.decodeFromString<List<Tag>>(file.readText())
    }
}
```

### iOS: CoreData Integration Considerations

```kotlin
// iosMain
class IOSDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = HazardHawkDatabase.Schema,
            name = "hazardhawk.db",
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = DatabaseConfiguration.Extended(
                        foreignKeyConstraints = true,
                        journalMode = JournalMode.WAL // Write-Ahead Logging
                    )
                )
            }
        )
    }
}

class IOSTagStorageManager : TagStorageManager {
    override suspend fun exportTags(tags: List<Tag>): URL {
        val documentsDirectory = FileManager.default.urls(
            for = .documentDirectory,
            in = .userDomainMask
        ).first
        
        let exportURL = documentsDirectory.appendingPathComponent("tags_export.json")
        let jsonData = try JSONEncoder().encode(tags)
        try jsonData.write(to: exportURL)
        
        return exportURL
    }
}
```

### Web: IndexedDB for Offline Storage

```kotlin
// jsMain
class WebDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return WebWorkerDriver(
            schema = HazardHawkDatabase.Schema,
            databaseName = "hazardhawk.db",
            worker = js("""
                new Worker('./sqljs-worker.js')
            """)
        )
    }
}

class WebTagStorageManager : TagStorageManager {
    override suspend fun exportTags(tags: List<Tag>): Blob {
        val json = Json.encodeToString(tags)
        return Blob(arrayOf(json), BlobPropertyBag(type = "application/json"))
    }
    
    override suspend fun importTags(file: File): List<Tag> {
        val text = file.text()
        return Json.decodeFromString<List<Tag>>(text)
    }
}
```

### Desktop: Local Database Options

```kotlin
// desktopMain/jvmMain
class DesktopDatabaseDriverFactory(
    private val databasePath: String
) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:$databasePath",
            properties = Properties().apply {
                put("foreign_keys", "true")
                put("journal_mode", "WAL")
                put("synchronous", "NORMAL")
            }
        )
        HazardHawkDatabase.Schema.create(driver)
        return driver
    }
}

class DesktopTagStorageManager(
    private val userHome: String = System.getProperty("user.home")
) : TagStorageManager {
    private val appDataDir = File(userHome, ".hazardhawk")
    
    init {
        appDataDir.mkdirs()
    }
    
    override suspend fun exportTags(tags: List<Tag>): File {
        val exportFile = File(appDataDir, "tags_export_${System.currentTimeMillis()}.json")
        exportFile.writeText(Json.encodeToString(tags))
        return exportFile
    }
}
```

## Performance Considerations

### Efficient Tag Search and Filtering

**Full-Text Search Implementation:**

```kotlin
class OptimizedTagSearchEngine(
    private val database: HazardHawkDatabase
) {
    suspend fun searchTags(
        query: String,
        filters: TagSearchFilters = TagSearchFilters(),
        limit: Int = 20
    ): List<Tag> {
        return if (query.length >= 3) {
            // Use FTS for longer queries
            performFullTextSearch(query, filters, limit)
        } else {
            // Use prefix matching for shorter queries
            performPrefixSearch(query, filters, limit)
        }
    }
    
    private suspend fun performFullTextSearch(
        query: String,
        filters: TagSearchFilters,
        limit: Int
    ): List<Tag> {
        val ftsQuery = buildFTSQuery(query, filters)
        return database.tagQueries.searchTagsFTS(ftsQuery, limit.toLong())
            .executeAsList()
            .map { mapRowToTag(it) }
    }
    
    private suspend fun performPrefixSearch(
        query: String,
        filters: TagSearchFilters,
        limit: Int
    ): List<Tag> {
        return database.tagQueries.searchTagsByPrefix("$query%", limit.toLong())
            .executeAsList()
            .map { mapRowToTag(it) }
    }
    
    private fun buildFTSQuery(query: String, filters: TagSearchFilters): String {
        val terms = query.split(" ").filter { it.isNotBlank() }
        val ftsTerms = terms.joinToString(" AND ") { "\"$it\"*" }
        
        return if (filters.categories.isNotEmpty()) {
            "$ftsTerms AND category:(${filters.categories.joinToString(" OR ")})"
        } else {
            ftsTerms
        }
    }
}

data class TagSearchFilters(
    val categories: List<TagCategory> = emptyList(),
    val customOnly: Boolean = false,
    val projectSpecific: Boolean? = null,
    val minUsageCount: Int = 0
)
```

### Batch Operations for Multiple Photo Tagging

**Optimized Batch Processing:**

```kotlin
class BatchTagOperations(
    private val database: HazardHawkDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun applyTagsToPhotos(
        photoIds: List<String>,
        tagIds: List<String>,
        userId: String
    ): Result<Unit> = withContext(dispatcher) {
        try {
            database.transaction {
                val timestamp = Clock.System.now().toEpochMilliseconds()
                
                // Batch insert photo-tag relationships
                photoIds.forEach { photoId ->
                    tagIds.forEach { tagId ->
                        database.tagQueries.insertPhotoTag(
                            photoId = photoId,
                            tagId = tagId,
                            appliedAt = timestamp,
                            appliedBy = userId
                        )
                    }
                }
                
                // Batch update tag usage counts
                tagIds.forEach { tagId ->
                    database.tagQueries.incrementTagUsage(
                        tagId = tagId,
                        incrementBy = photoIds.size.toLong(),
                        lastUsed = timestamp
                    )
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeTagsFromPhotos(
        photoIds: List<String>,
        tagIds: List<String>
    ): Result<Unit> = withContext(dispatcher) {
        try {
            database.transaction {
                photoIds.forEach { photoId ->
                    tagIds.forEach { tagId ->
                        database.tagQueries.deletePhotoTag(
                            photoId = photoId,
                            tagId = tagId
                        )
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Memory Management for Large Tag Sets

**Pagination and Virtual Loading:**

```kotlin
class PaginatedTagLoader(
    private val tagRepository: TagRepository
) {
    companion object {
        private const val PAGE_SIZE = 50
        private const val CACHE_SIZE = 200
    }
    
    private val tagCache = LRUCache<Int, List<Tag>>(CACHE_SIZE / PAGE_SIZE)
    
    suspend fun loadPage(
        pageIndex: Int,
        category: TagCategory? = null,
        searchQuery: String? = null
    ): List<Tag> {
        val cacheKey = generateCacheKey(pageIndex, category, searchQuery)
        
        tagCache.get(cacheKey)?.let { return it }
        
        val tags = when {
            !searchQuery.isNullOrBlank() -> {
                tagRepository.searchTagsPaginated(
                    query = searchQuery,
                    offset = pageIndex * PAGE_SIZE,
                    limit = PAGE_SIZE
                )
            }
            category != null -> {
                tagRepository.getTagsByCategoryPaginated(
                    category = category,
                    offset = pageIndex * PAGE_SIZE,
                    limit = PAGE_SIZE
                )
            }
            else -> {
                tagRepository.getAllTagsPaginated(
                    offset = pageIndex * PAGE_SIZE,
                    limit = PAGE_SIZE
                )
            }
        }
        
        tagCache.put(cacheKey, tags)
        return tags
    }
    
    private fun generateCacheKey(
        pageIndex: Int,
        category: TagCategory?,
        searchQuery: String?
    ): Int {
        return "$pageIndex|${category?.name ?: ""}|${searchQuery ?: ""}".hashCode()
    }
}
```

## Code Examples

### Sample KMP Data Class Structure

```kotlin
// commonMain/src/kotlin/com/hazardhawk/domain/entities/Tag.kt
@Serializable
data class Tag(
    val id: String,
    val name: String,
    val category: TagCategory,
    val usageCount: Int = 0,
    val lastUsed: Long? = null,
    val projectSpecific: Boolean = false,
    val isCustom: Boolean = false,
    val oshaReferences: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isRecentlyUsed(withinDays: Int = 7): Boolean {
        return lastUsed?.let { lastUsedTime ->
            val daysSinceUsed = (Clock.System.now().toEpochMilliseconds() - lastUsedTime) / 86400000
            daysSinceUsed <= withinDays
        } ?: false
    }
    
    fun calculateRelevanceScore(
        personalUsageWeight: Double = 0.4,
        projectUsageWeight: Double = 0.3,
        industryWeight: Double = 0.3
    ): Double {
        // Implementation of weighted scoring algorithm
        val personalScore = if (usageCount > 0) usageCount.toDouble() / 100.0 else 0.0
        val recentBoost = if (isRecentlyUsed()) 0.2 else 0.0
        val customBoost = if (isCustom) 0.1 else 0.0
        
        return (personalScore * personalUsageWeight) + recentBoost + customBoost
    }
}

@Serializable
enum class TagCategory {
    PPE,
    FALL_PROTECTION,
    ELECTRICAL,
    HOUSEKEEPING,
    EQUIPMENT,
    HOT_WORK,
    CRANE_LIFT,
    CUSTOM;
    
    fun getDisplayName(): String = when (this) {
        PPE -> "Personal Protective Equipment"
        FALL_PROTECTION -> "Fall Protection"
        ELECTRICAL -> "Electrical Safety"
        HOUSEKEEPING -> "Housekeeping"
        EQUIPMENT -> "Equipment Safety"
        HOT_WORK -> "Hot Work"
        CRANE_LIFT -> "Crane & Lifting"
        CUSTOM -> "Custom Tags"
    }
    
    fun getOSHAStandardTags(): List<String> = when (this) {
        PPE -> listOf("Hard Hat", "Safety Glasses", "Steel Toes", "High-Vis Vest")
        FALL_PROTECTION -> listOf("Harness", "Guardrail", "Safety Net", "Ladder")
        ELECTRICAL -> listOf("LOTO", "Arc Flash", "Ground Fault", "Electrical Panel")
        // ... etc
        else -> emptyList()
    }
}
```

### Repository Interface Example

```kotlin
// commonMain/src/kotlin/com/hazardhawk/domain/repositories/TagRepository.kt
interface TagRepository {
    // Basic CRUD operations
    suspend fun saveTag(tag: Tag): Result<Tag>
    suspend fun getTag(id: String): Tag?
    suspend fun deleteTag(id: String): Result<Unit>
    
    // Reactive queries
    fun getAllTags(): Flow<List<Tag>>
    fun getTagsByCategory(category: TagCategory): Flow<List<Tag>>
    fun getSelectedTags(): StateFlow<Set<String>>
    
    // Search operations
    suspend fun searchTags(
        query: String,
        filters: TagSearchFilters = TagSearchFilters()
    ): List<Tag>
    suspend fun searchTagsFTS(query: String, limit: Int = 20): List<Tag>
    
    // Recommendation system
    suspend fun getRecommendedTags(
        userId: String,
        projectId: String?,
        context: TagContext = TagContext.PHOTO_TAGGING,
        limit: Int = 8
    ): List<Tag>
    
    suspend fun getPersonalTopTags(userId: String, limit: Int): List<Tag>
    suspend fun getProjectTopTags(projectId: String, limit: Int): List<Tag>
    suspend fun getIndustryStandardTags(): List<Tag>
    suspend fun getRecentlyUsedTags(userId: String, limit: Int): List<Tag>
    
    // Usage tracking
    suspend fun incrementTagUsage(tagId: String, userId: String): Result<Unit>
    suspend fun batchIncrementUsage(tagIds: List<String>, userId: String): Result<Unit>
    suspend fun getTagUsageAnalytics(userId: String, timeRange: TimeRange): TagUsageAnalytics
    
    // Custom tag operations
    suspend fun createCustomTag(
        name: String,
        category: TagCategory,
        userId: String,
        projectId: String?
    ): Result<Tag>
    
    // Batch operations
    suspend fun applyTagsToPhotos(
        photoIds: List<String>,
        tagIds: List<String>,
        userId: String
    ): Result<Unit>
    
    suspend fun removeTagsFromPhotos(
        photoIds: List<String>,
        tagIds: List<String>
    ): Result<Unit>
    
    // Selection state management
    suspend fun selectTag(tagId: String)
    suspend fun deselectTag(tagId: String)
    suspend fun clearSelection()
    suspend fun setSelectionMode(mode: SelectionMode)
    
    // Cache management
    suspend fun preloadTagsForCategory(category: TagCategory): Result<Unit>
    suspend fun clearTagCache(): Result<Unit>
    suspend fun syncTagsWithServer(): Result<Unit>
}
```

### ViewModel/UseCase Patterns

```kotlin
// commonMain/src/kotlin/com/hazardhawk/domain/usecases/TagUseCases.kt

class ApplyTagsUseCase(
    private val tagRepository: TagRepository,
    private val analyticsRepository: AnalyticsRepository
) {
    suspend operator fun invoke(
        photoIds: List<String>,
        tagIds: List<String>,
        userId: String
    ): Result<Unit> {
        return try {
            // Apply tags to photos
            tagRepository.applyTagsToPhotos(photoIds, tagIds, userId).fold(
                onSuccess = {
                    // Track analytics
                    analyticsRepository.trackTagApplication(
                        TagApplicationEvent(
                            userId = userId,
                            photoCount = photoIds.size,
                            tagCount = tagIds.size,
                            timestamp = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                    
                    Result.success(Unit)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetRecommendedTagsUseCase(
    private val tagRepository: TagRepository,
    private val recommendationEngine: TagRecommendationEngine
) {
    suspend operator fun invoke(
        userId: String,
        projectId: String,
        context: TagContext = TagContext.PHOTO_TAGGING
    ): List<Tag> {
        return recommendationEngine.getRecommendedTagsWithRecency(
            userId = userId,
            projectId = projectId,
            context = context
        )
    }
}

// Platform-specific ViewModel (Android example)
class TagSelectionViewModel(
    private val applyTagsUseCase: ApplyTagsUseCase,
    private val getRecommendedTagsUseCase: GetRecommendedTagsUseCase,
    private val tagRepository: TagRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TagSelectionUiState())
    val uiState: StateFlow<TagSelectionUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            combine(
                tagRepository.getAllTags(),
                tagRepository.getSelectedTags(),
                userRepository.getCurrentUser()
            ) { allTags, selectedTags, user ->
                _uiState.value.copy(
                    allTags = allTags,
                    selectedTagIds = selectedTags,
                    recommendedTags = if (user != null) {
                        getRecommendedTagsUseCase(
                            userId = user.id,
                            projectId = user.currentProjectId
                        )
                    } else emptyList()
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
    
    fun searchTags(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val searchResults = tagRepository.searchTags(query)
                _uiState.value = _uiState.value.copy(
                    searchResults = searchResults,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun selectTag(tagId: String) {
        viewModelScope.launch {
            tagRepository.selectTag(tagId)
        }
    }
    
    fun applySelectedTags(photoIds: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true)
            
            val currentUser = userRepository.getCurrentUser()
            if (currentUser != null) {
                applyTagsUseCase(
                    photoIds = photoIds,
                    tagIds = _uiState.value.selectedTagIds.toList(),
                    userId = currentUser.id
                ).fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isApplying = false,
                            successMessage = "Tags applied successfully"
                        )
                        tagRepository.clearSelection()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isApplying = false,
                            error = error.message
                        )
                    }
                )
            }
        }
    }
}

data class TagSelectionUiState(
    val allTags: List<Tag> = emptyList(),
    val recommendedTags: List<Tag> = emptyList(),
    val searchResults: List<Tag> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isApplying: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
```

## Analysis of Current Implementation

Based on the analysis of the existing HazardHawk codebase, here are the key findings:

### Strengths of Current Implementation

1. **Well-structured Data Models**: The current `Tag` entity in `/shared/src/commonMain/kotlin/com/hazardhawk/domain/entities/Tag.kt` follows best practices with:
   - Proper serialization support
   - Comprehensive metadata fields
   - Usage tracking capabilities
   - OSHA reference integration

2. **Clean Repository Pattern**: The `TagRepository` interface is well-designed with:
   - Comprehensive method signatures
   - Proper separation of concerns
   - Support for recommendation algorithms

3. **Optimized Database Schema**: The SQLDelight schema in `/shared/src/commonMain/sqldelight/com/hazardhawk/database/HazardHawkDatabase.sq` includes:
   - Proper indexing for performance
   - Foreign key constraints
   - Junction table for photo-tag relationships

4. **Smart Recommendation Engine**: The existing implementation in `/shared/src/commonMain/kotlin/com/hazardhawk/domain/engine/TagRecommendationEngine.kt` provides:
   - Weighted scoring algorithm
   - Recency-based boosting
   - Context-aware recommendations

### Areas for Improvement

1. **Missing State Management**: The current implementation lacks reactive state management using StateFlow/Flow patterns.

2. **Limited Caching Strategy**: No multi-level caching implementation for improved performance.

3. **Platform-Specific Optimizations**: Missing platform-specific storage managers and optimizations.

4. **Batch Operations**: Limited support for efficient batch tagging operations.

5. **Search Performance**: Could benefit from full-text search implementation using SQLite FTS.

## Industry Best Practices 2025

### Key Trends and Patterns

1. **SQLDelight vs Room for KMP**: 
   - SQLDelight remains the preferred choice for true multiplatform projects
   - Room's KMP support (available since 2024) is gaining traction but still platform-limited
   - SQLDelight provides better type safety and compile-time verification

2. **State Management Evolution**:
   - StateFlow/SharedFlow patterns are becoming standard
   - Unidirectional data flow with MVI architecture
   - Reactive programming with Kotlin Coroutines and Flow

3. **Performance Optimization**:
   - Multi-level caching strategies
   - Lazy loading and pagination for large datasets
   - Background synchronization with exponential backoff
   - Memory-efficient data structures

4. **Cross-Platform Storage Patterns**:
   - expect/actual pattern for platform-specific implementations
   - Unified API with platform-specific optimizations
   - Consistent error handling across platforms

### 2025 Technology Stack Recommendations

```kotlin
dependencies {
    // Core KMP
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // Database
    implementation("app.cash.sqldelight:coroutines-extensions:2.1.0")
    
    // Networking
    implementation("io.ktor:ktor-client-core:2.3.8")
    
    // Dependency Injection
    implementation("io.insert-koin:koin-core:3.5.3")
    
    // Date/Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    
    // Caching
    implementation("com.github.reactivecircus.cache4k:cache4k:0.11.0") // For memory caching
}
```

## Recommendations

Based on the research findings and analysis, here are the key recommendations for implementing a robust tag management system in Kotlin Multiplatform:

### 1. Data Layer Architecture

- **Use SQLDelight** for cross-platform database management with full-text search capabilities
- **Implement multi-level caching** with memory, disk, and network layers
- **Design immutable data classes** with comprehensive metadata support
- **Use proper indexing strategies** for optimal query performance

### 2. State Management

- **Adopt StateFlow/Flow patterns** for reactive state management
- **Implement centralized state management** for tag selection and UI state
- **Use unidirectional data flow** with MVI or similar architectures
- **Leverage Kotlin Coroutines** for asynchronous operations

### 3. Performance Optimization

- **Implement intelligent caching** with LRU eviction policies
- **Use pagination and virtual loading** for large tag sets
- **Optimize search with FTS** for complex queries
- **Implement batch operations** for multiple photo tagging

### 4. Platform-Specific Features

- **Create platform-specific storage managers** for optimal file handling
- **Implement platform-optimized search** (Core Spotlight on iOS, SearchManager on Android)
- **Use platform-native sharing** mechanisms for tag export/import
- **Optimize memory management** per platform constraints

### 5. Smart Features

- **Implement machine learning algorithms** for tag recommendations
- **Add context-aware suggestions** based on user workflow
- **Support custom tag creation** with validation and suggestion
- **Track usage analytics** for continuous improvement

### 6. Development Best Practices

- **Follow Clean Architecture principles** with proper separation of concerns
- **Implement comprehensive testing** across all platforms
- **Use proper error handling** with Result types and sealed classes
- **Document APIs thoroughly** for maintainability

### 7. Future-Proofing

- **Design extensible schemas** for future enhancements
- **Implement version migration strategies** for database updates
- **Support offline-first architecture** with synchronization
- **Plan for internationalization** and accessibility features

This comprehensive approach will ensure a robust, performant, and maintainable tag management system that leverages the full power of Kotlin Multiplatform while following industry best practices for 2025.