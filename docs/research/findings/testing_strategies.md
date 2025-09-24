# Testing Strategies for Tag Management Systems in Kotlin Multiplatform

## Executive Summary

This document provides comprehensive testing strategies for tag management systems in Kotlin Multiplatform (KMP), based on analysis of the HazardHawk codebase, industry best practices for 2025, and extensive research into modern KMP testing approaches. The strategies cover unit testing, integration testing, UI testing, and performance testing with specific focus on tag repositories, recommendation algorithms, data model validation, and SQLDelight mocking strategies.

## Table of Contents

1. [Unit Testing](#unit-testing)
2. [Integration Testing](#integration-testing)
3. [UI Testing](#ui-testing)
4. [Performance Testing](#performance-testing)
5. [Platform-Specific Testing](#platform-specific-testing)
6. [Testing Infrastructure](#testing-infrastructure)
7. [Best Practices and Recommendations](#best-practices-and-recommendations)

## Unit Testing

### 1. Testing Tag Repositories

Tag repositories are the core data access layer and require comprehensive testing to ensure reliable data operations across all platforms.

#### Repository Interface Testing

```kotlin
// commonTest
class TagRepositoryTest {
    private lateinit var repository: TagRepository
    private lateinit var mockDatabase: HazardHawkDatabase
    private lateinit var mockCacheManager: TagCacheManager
    
    @BeforeEach
    fun setUp() {
        mockDatabase = mockk(relaxed = true)
        mockCacheManager = mockk(relaxed = true)
        repository = TagRepositoryImpl(mockDatabase, mockCacheManager)
    }
    
    @Test
    fun `saveTag should persist tag to database and update cache`() = runTest {
        // Given
        val tag = TestDataFactory.createTestTag()
        every { mockDatabase.tagQueries.insertTag(any(), any(), any()) } just Runs
        
        // When
        val result = repository.saveTag(tag)
        
        // Then
        assertTrue(result.isSuccess)
        verify { mockDatabase.tagQueries.insertTag(any(), any(), any()) }
        verify { mockCacheManager.updateCache(tag) }
    }
    
    @Test
    fun `getAllTags should return flow of tags from database`() = runTest {
        // Given
        val mockTags = TestDataFactory.createPersonalTopTags()
        val mockFlow = flowOf(mockTags)
        every { mockDatabase.tagQueries.getAllTags().asFlow() } returns mockFlow
        
        // When
        val result = repository.getAllTags().first()
        
        // Then
        assertEquals(mockTags, result)
    }
    
    @Test
    fun `searchTags should use FTS for queries longer than 2 characters`() = runTest {
        // Given
        val query = "fall protection"
        val expectedResults = listOf(TestDataFactory.createTestTag(name = "Fall Protection"))
        every { 
            mockDatabase.tagQueries.searchTagsFTS(any(), any()) 
        } returns expectedResults.asQueryResult()
        
        // When
        val results = repository.searchTags(query)
        
        // Then
        assertEquals(expectedResults, results)
        verify { mockDatabase.tagQueries.searchTagsFTS(any(), any()) }
    }
    
    @Test
    fun `searchTags should use prefix search for short queries`() = runTest {
        // Given
        val query = "fa"
        val expectedResults = listOf(TestDataFactory.createTestTag(name = "Fall"))
        every { 
            mockDatabase.tagQueries.searchTagsByPrefix(any(), any()) 
        } returns expectedResults.asQueryResult()
        
        // When
        val results = repository.searchTags(query)
        
        // Then
        assertEquals(expectedResults, results)
        verify { mockDatabase.tagQueries.searchTagsByPrefix("$query%", any()) }
    }
}
```

#### Repository Error Handling Tests

```kotlin
@Test
fun `saveTag should handle database constraint violations gracefully`() = runTest {
    // Given
    val duplicateTag = TestDataFactory.createTestTag(id = "existing-id")
    every { 
        mockDatabase.tagQueries.insertTag(any(), any(), any()) 
    } throws SQLException("UNIQUE constraint failed")
    
    // When
    val result = repository.saveTag(duplicateTag)
    
    // Then
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is DatabaseConstraintException)
}

@Test
fun `getAllTags should handle database connection failures`() = runTest {
    // Given
    every { mockDatabase.tagQueries.getAllTags() } throws SQLiteException("database is locked")
    
    // When & Then
    assertFailsWith<DatabaseConnectionException> {
        repository.getAllTags().first()
    }
}
```

### 2. Algorithm Testing for Recommendations

The tag recommendation engine requires extensive testing to validate the weighted algorithm, recency boost, and context-aware suggestions.

#### Weighted Algorithm Validation

```kotlin
class TagRecommendationEngineTest {
    private lateinit var engine: TagRecommendationEngine
    private lateinit var mockRepository: MockTagRepository
    
    @BeforeEach
    fun setUp() {
        mockRepository = MockTagRepository()
        engine = TagRecommendationEngine(mockRepository)
    }
    
    @Test
    fun `getRecommendedTags should apply 40-30-30 weight distribution`() = runTest {
        // Given
        val personalTags = TestDataFactory.createPersonalTopTags()
        val projectTags = TestDataFactory.createProjectTopTags()
        val industryTags = TestDataFactory.createIndustryStandardTags()
        
        // Mock repository responses
        coEvery { mockRepository.getPersonalTopTags(any(), any()) } returns personalTags
        coEvery { mockRepository.getProjectTopTags(any(), any()) } returns projectTags
        coEvery { mockRepository.getIndustryStandardTags() } returns industryTags
        
        // When
        val recommendations = engine.getRecommendedTags("user-1", "project-1")
        
        // Then
        assertEquals(8, recommendations.size) // Maximum recommendations
        
        // Verify weighted distribution is applied
        val personalInResults = recommendations.any { tag ->
            personalTags.any { it.id == tag.id }
        }
        val projectInResults = recommendations.any { tag ->
            projectTags.any { it.id == tag.id }
        }
        val industryInResults = recommendations.any { tag ->
            industryTags.any { it.id == tag.id }
        }
        
        assertTrue(personalInResults, "Personal tags should be included (40% weight)")
        assertTrue(projectInResults, "Project tags should be included (30% weight)")
        assertTrue(industryInResults, "Industry tags should be included (30% weight)")
    }
    
    @Test
    fun `getRecommendedTagsWithRecency should boost recently used tags`() = runTest {
        // Given
        val recentTag = TestDataFactory.createTestTag(
            id = "recent-tag",
            name = "Recently Used Tag",
            usageCount = 1 // Low usage count
        )
        val highUsageTag = TestDataFactory.createTestTag(
            id = "high-usage-tag", 
            name = "High Usage Tag",
            usageCount = 50 // High usage count
        )
        
        coEvery { mockRepository.getRecentlyUsedTags(any(), any()) } returns listOf(recentTag)
        coEvery { mockRepository.getPersonalTopTags(any(), any()) } returns listOf(highUsageTag)
        
        // When
        val recommendations = engine.getRecommendedTagsWithRecency("user-1", "project-1")
        
        // Then
        // Recent tag should appear before high usage tag due to recency boost
        val recentTagIndex = recommendations.indexOfFirst { it.id == recentTag.id }
        val highUsageTagIndex = recommendations.indexOfFirst { it.id == highUsageTag.id }
        
        assertTrue(recentTagIndex != -1, "Recent tag should be in recommendations")
        assertTrue(recentTagIndex < highUsageTagIndex, "Recent tag should rank higher than high usage tag")
    }
    
    @Test
    fun `recommendation algorithm should handle empty tag categories gracefully`() = runTest {
        // Given
        coEvery { mockRepository.getPersonalTopTags(any(), any()) } returns emptyList()
        coEvery { mockRepository.getProjectTopTags(any(), any()) } returns emptyList()
        coEvery { mockRepository.getIndustryStandardTags() } returns emptyList()
        
        // When
        val recommendations = engine.getRecommendedTags("user-1", "project-1")
        
        // Then
        assertEquals(0, recommendations.size, "Should handle empty repositories gracefully")
    }
    
    @Test
    fun `searchTagsWithContext should boost relevant tags based on context`() = runTest {
        // Given
        val ppeTag = TestDataFactory.createTestTag(name = "Hard Hat", category = "PPE")
        val fallProtectionTag = TestDataFactory.createTestTag(name = "Harness", category = "FALL_PROTECTION")
        val allTags = listOf(ppeTag, fallProtectionTag)
        
        coEvery { mockRepository.searchTagsByName(any(), any()) } returns allTags
        
        // When - Search in photo tagging context
        val photoTaggingResults = engine.searchTagsWithContext(
            query = "safety",
            userId = "user-1",
            projectId = "project-1",
            context = TagContext.PHOTO_TAGGING
        )
        
        // Then - Fall protection should rank higher in photo tagging context
        val fallProtectionIndex = photoTaggingResults.indexOfFirst { it.id == fallProtectionTag.id }
        val ppeIndex = photoTaggingResults.indexOfFirst { it.id == ppeTag.id }
        
        assertTrue(fallProtectionIndex < ppeIndex, "Fall protection should rank higher in photo context")
    }
}
```

#### Performance Testing for Recommendation Algorithms

```kotlin
@Test
fun `recommendation algorithm should complete within 100ms for normal datasets`() = runTest {
    // Given
    val largePersonalTags = TestDataFactory.createLargeTagList(50)
    val largeProjectTags = TestDataFactory.createLargeTagList(30)
    val largeIndustryTags = TestDataFactory.createLargeTagList(100)
    
    coEvery { mockRepository.getPersonalTopTags(any(), any()) } returns largePersonalTags
    coEvery { mockRepository.getProjectTopTags(any(), any()) } returns largeProjectTags
    coEvery { mockRepository.getIndustryStandardTags() } returns largeIndustryTags
    
    // When
    val startTime = System.currentTimeMillis()
    val recommendations = engine.getRecommendedTags("user-1", "project-1")
    val duration = System.currentTimeMillis() - startTime
    
    // Then
    assertTrue(duration < 100, "Recommendation algorithm should complete within 100ms, took ${duration}ms")
    assertEquals(8, recommendations.size)
}
```

### 3. Data Model Validation

Data models require validation testing to ensure proper serialization, deserialization, and business rule enforcement.

#### Tag Model Validation

```kotlin
class TagModelTest {
    
    @Test
    fun `Tag serialization should preserve all fields`() {
        // Given
        val originalTag = Tag(
            id = "tag-1",
            name = "Fall Protection",
            category = TagCategory.FALL_PROTECTION,
            usageCount = 10,
            lastUsed = Clock.System.now().toEpochMilliseconds(),
            projectSpecific = true,
            isCustom = false,
            oshaReferences = listOf("1926.501", "1926.502"),
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
        
        // When
        val json = Json.encodeToString(originalTag)
        val deserializedTag = Json.decodeFromString<Tag>(json)
        
        // Then
        assertEquals(originalTag, deserializedTag)
    }
    
    @Test
    fun `Tag isRecentlyUsed should correctly calculate recency window`() {
        // Given
        val now = Clock.System.now()
        val recentTag = TestDataFactory.createTestTag().copy(
            lastUsed = now.minus(3.days).toEpochMilliseconds()
        )
        val oldTag = TestDataFactory.createTestTag().copy(
            lastUsed = now.minus(10.days).toEpochMilliseconds()
        )
        
        // When & Then
        assertTrue(recentTag.isRecentlyUsed(withinDays = 7), "Tag used 3 days ago should be recent")
        assertFalse(oldTag.isRecentlyUsed(withinDays = 7), "Tag used 10 days ago should not be recent")
    }
    
    @Test
    fun `Tag calculateRelevanceScore should apply weighted scoring correctly`() {
        // Given
        val tag = TestDataFactory.createTestTag().copy(
            usageCount = 20,
            lastUsed = Clock.System.now().minus(2.days).toEpochMilliseconds(),
            isCustom = true
        )
        
        // When
        val score = tag.calculateRelevanceScore()
        
        // Then
        assertTrue(score > 0.0, "Score should be positive")
        
        // Score should include personal usage (20/100 * 0.4) + recency boost (0.2) + custom boost (0.1)
        val expectedMinimumScore = (20.0/100.0 * 0.4) + 0.2 + 0.1 // 0.38
        assertTrue(score >= expectedMinimumScore, "Score should include all boosts: $score vs $expectedMinimumScore")
    }
    
    @Test
    fun `TagCategory should provide correct OSHA standard tags`() {
        // Given & When
        val ppeStandardTags = TagCategory.PPE.getOSHAStandardTags()
        val fallProtectionTags = TagCategory.FALL_PROTECTION.getOSHAStandardTags()
        
        // Then
        assertTrue(ppeStandardTags.contains("Hard Hat"), "PPE should include Hard Hat")
        assertTrue(ppeStandardTags.contains("Safety Glasses"), "PPE should include Safety Glasses")
        assertTrue(fallProtectionTags.contains("Harness"), "Fall Protection should include Harness")
        assertTrue(fallProtectionTags.contains("Guardrail"), "Fall Protection should include Guardrail")
    }
}
```

### 4. Mock Strategies for SQLDelight

Effective testing of SQLDelight requires proper mocking strategies that maintain type safety while enabling controlled test scenarios.

#### In-Memory Database Testing

```kotlin
// Test database driver factory for consistent in-memory testing
class TestDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            .also { HazardHawkDatabase.Schema.create(it) }
    }
}

// Test base class for database tests
abstract class DatabaseTestBase {
    protected lateinit var database: HazardHawkDatabase
    protected lateinit var driver: SqlDriver
    
    @BeforeEach
    fun setUpDatabase() {
        driver = TestDatabaseDriverFactory().createDriver()
        database = HazardHawkDatabase(driver)
        insertTestData()
    }
    
    @AfterEach
    fun tearDownDatabase() {
        driver.close()
    }
    
    private fun insertTestData() {
        // Insert standard test data for consistent testing
        TestDataFactory.createIndustryStandardTags().forEach { tag ->
            database.tagQueries.insertTag(
                id = tag.id,
                name = tag.name,
                category = tag.category.name,
                usage_count = tag.usageCount.toLong(),
                is_custom = if (tag.isCustom) 1L else 0L,
                created_at = tag.createdAt,
                updated_at = tag.updatedAt
            )
        }
    }
}
```

#### Repository Integration Testing

```kotlin
class TagRepositoryIntegrationTest : DatabaseTestBase() {
    
    private lateinit var repository: TagRepositoryImpl
    
    @BeforeEach
    fun setUpRepository() {
        repository = TagRepositoryImpl(database, mockk(relaxed = true))
    }
    
    @Test
    fun `repository should correctly save and retrieve tags`() = runTest {
        // Given
        val newTag = TestDataFactory.createTestTag(
            id = "integration-test-tag",
            name = "Integration Test Tag"
        )
        
        // When
        val saveResult = repository.saveTag(newTag)
        val retrievedTag = repository.getTag(newTag.id)
        
        // Then
        assertTrue(saveResult.isSuccess)
        assertNotNull(retrievedTag)
        assertEquals(newTag.name, retrievedTag?.name)
        assertEquals(newTag.category, retrievedTag?.category)
    }
    
    @Test
    fun `repository should handle concurrent tag usage updates`() = runTest {
        // Given
        val tag = database.tagQueries.getAllTags().executeAsList().first()
        val initialUsageCount = tag.usage_count
        val concurrentUpdates = 10
        
        // When - Simulate concurrent usage updates
        val jobs = (1..concurrentUpdates).map {
            async {
                repository.incrementTagUsage(tag.id, "user-$it")
            }
        }
        jobs.awaitAll()
        
        // Then
        val updatedTag = database.tagQueries.getTagById(tag.id).executeAsOne()
        assertEquals(
            initialUsageCount + concurrentUpdates,
            updatedTag.usage_count,
            "All concurrent updates should be applied"
        )
    }
}
```

#### Mock Query Result Extensions

```kotlin
// Extension functions to simplify mock query results
fun <T> List<T>.asQueryResult(): Query<T> = mockk {
    every { executeAsList() } returns this@asQueryResult
    every { executeAsOne() } returns this@asQueryResult.first()
    every { executeAsOneOrNull() } returns this@asQueryResult.firstOrNull()
}

fun <T> List<T>.asFlow(): Flow<Query<T>> = flowOf(this.asQueryResult())
```

## Integration Testing

### 1. Database Operation Testing

Integration tests verify that multiple components work together correctly, particularly database operations with business logic.

#### Tag Synchronization Testing

```kotlin
class TagSyncIntegrationTest : DatabaseTestBase() {
    
    private lateinit var syncService: SyncService
    private lateinit var tagRepository: TagRepository
    private lateinit var mockNetworkApi: TagNetworkApi
    
    @BeforeEach
    fun setUpIntegration() {
        mockNetworkApi = mockk()
        tagRepository = TagRepositoryImpl(database, mockk(relaxed = true))
        syncService = SyncService(tagRepository, mockNetworkApi)
    }
    
    @Test
    fun `sync should merge local and remote tags correctly`() = runTest {
        // Given
        val localTag = TestDataFactory.createTestTag(
            id = "local-tag",
            name = "Local Tag",
            usageCount = 10,
            updatedAt = Clock.System.now().minus(1.hours).toEpochMilliseconds()
        )
        val remoteTag = TestDataFactory.createTestTag(
            id = "local-tag", // Same ID, different data
            name = "Updated Remote Tag",
            usageCount = 15,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
        
        tagRepository.saveTag(localTag) // Save local version
        coEvery { mockNetworkApi.getAllTags() } returns listOf(remoteTag)
        
        // When
        val syncResult = syncService.syncTags()
        
        // Then
        assertTrue(syncResult.isSuccess)
        val syncedTag = tagRepository.getTag("local-tag")
        assertNotNull(syncedTag)
        assertEquals("Updated Remote Tag", syncedTag?.name) // Remote should win due to newer timestamp
        assertEquals(15, syncedTag?.usageCount)
    }
    
    @Test
    fun `sync should handle network failures gracefully`() = runTest {
        // Given
        coEvery { mockNetworkApi.getAllTags() } throws NetworkException("Connection timeout")
        
        // When
        val syncResult = syncService.syncTags()
        
        // Then
        assertTrue(syncResult.isFailure)
        assertTrue(syncResult.exceptionOrNull() is NetworkException)
        
        // Local data should remain unchanged
        val localTags = tagRepository.getAllTags().first()
        assertTrue(localTags.isNotEmpty(), "Local tags should be preserved during sync failures")
    }
    
    @Test
    fun `sync should resolve conflicts using timestamp precedence`() = runTest {
        // Given
        val baseTime = Clock.System.now()
        val newerLocalTag = TestDataFactory.createTestTag(
            id = "conflict-tag",
            name = "Newer Local",
            updatedAt = baseTime.toEpochMilliseconds()
        )
        val olderRemoteTag = TestDataFactory.createTestTag(
            id = "conflict-tag",
            name = "Older Remote",
            updatedAt = baseTime.minus(1.hours).toEpochMilliseconds()
        )
        
        tagRepository.saveTag(newerLocalTag)
        coEvery { mockNetworkApi.getAllTags() } returns listOf(olderRemoteTag)
        
        // When
        val syncResult = syncService.syncTags()
        
        // Then
        assertTrue(syncResult.isSuccess)
        val resolvedTag = tagRepository.getTag("conflict-tag")
        assertEquals("Newer Local", resolvedTag?.name, "Newer local tag should win conflict")
    }
}
```

### 2. Sync Mechanism Testing

Synchronization mechanisms require testing to ensure data consistency across devices and platforms.

#### Offline Queue Testing

```kotlin
class OfflineQueueIntegrationTest : DatabaseTestBase() {
    
    private lateinit var offlineQueue: OfflineQueueManager
    private lateinit var tagRepository: TagRepository
    private lateinit var mockNetworkClient: NetworkClient
    
    @BeforeEach
    fun setUpOfflineQueue() {
        mockNetworkClient = mockk()
        tagRepository = TagRepositoryImpl(database, mockk(relaxed = true))
        offlineQueue = OfflineQueueManager(database, mockNetworkClient)
    }
    
    @Test
    fun `offline queue should persist tag operations when network unavailable`() = runTest {
        // Given
        val tag = TestDataFactory.createTestTag()
        coEvery { mockNetworkClient.isNetworkAvailable() } returns false
        
        // When
        val result = offlineQueue.queueTagUpdate(tag)
        
        // Then
        assertTrue(result.isSuccess)
        
        val queuedOperations = database.offlineQueueQueries
            .getPendingOperations()
            .executeAsList()
        
        assertEquals(1, queuedOperations.size)
        assertEquals("TAG_UPDATE", queuedOperations.first().operation_type)
    }
    
    @Test
    fun `offline queue should process queued operations when network restored`() = runTest {
        // Given - Queue operations while offline
        val tagsToQueue = TestDataFactory.createPersonalTopTags()
        coEvery { mockNetworkClient.isNetworkAvailable() } returns false
        
        tagsToQueue.forEach { tag ->
            offlineQueue.queueTagUpdate(tag)
        }
        
        // Network becomes available
        coEvery { mockNetworkClient.isNetworkAvailable() } returns true
        coEvery { mockNetworkClient.updateTag(any()) } returns Result.success(Unit)
        
        // When
        val processResult = offlineQueue.processQueue()
        
        // Then
        assertTrue(processResult.isSuccess)
        
        val remainingOperations = database.offlineQueueQueries
            .getPendingOperations()
            .executeAsList()
        
        assertEquals(0, remainingOperations.size, "All operations should be processed")
        
        // Verify all tags were sent to network
        verify(exactly = tagsToQueue.size) { 
            runBlocking { mockNetworkClient.updateTag(any()) }
        }
    }
    
    @Test
    fun `offline queue should implement exponential backoff for failed operations`() = runTest {
        // Given
        val failingTag = TestDataFactory.createTestTag()
        coEvery { mockNetworkClient.isNetworkAvailable() } returns true
        coEvery { mockNetworkClient.updateTag(any()) } returns Result.failure(NetworkException("Server error"))
        
        offlineQueue.queueTagUpdate(failingTag)
        
        // When - Process queue multiple times to trigger retry logic
        repeat(3) {
            offlineQueue.processQueue()
            delay(100) // Allow backoff delay
        }
        
        // Then
        val failedOperation = database.offlineQueueQueries
            .getPendingOperations()
            .executeAsList()
            .first()
        
        assertTrue(failedOperation.retry_count > 0, "Retry count should increase")
        assertTrue(failedOperation.next_retry_at > Clock.System.now().toEpochMilliseconds(), 
                  "Next retry should be scheduled in future")
    }
}
```

### 3. API Integration Tests

API integration tests verify that the application correctly communicates with external services.

#### Network Tag API Testing

```kotlin
class TagNetworkApiIntegrationTest {
    
    private lateinit var apiClient: TagNetworkApi
    private lateinit var mockEngine: MockEngine
    
    @BeforeEach
    fun setUpApiClient() {
        mockEngine = MockEngine { request ->
            when (request.url.pathSegments.last()) {
                "tags" -> {
                    if (request.method == HttpMethod.Get) {
                        respond(
                            content = Json.encodeToString(TestDataFactory.createIndustryStandardTags()),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    } else {
                        respond("", HttpStatusCode.MethodNotAllowed)
                    }
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        
        apiClient = TagNetworkApiImpl(
            HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json()
                }
            }
        )
    }
    
    @Test
    fun `getAllTags should deserialize API response correctly`() = runTest {
        // When
        val result = apiClient.getAllTags()
        
        // Then
        assertTrue(result.isSuccess)
        val tags = result.getOrThrow()
        assertTrue(tags.isNotEmpty())
        
        // Verify structure of deserialized tags
        val firstTag = tags.first()
        assertNotNull(firstTag.id)
        assertNotNull(firstTag.name)
        assertNotNull(firstTag.category)
        assertTrue(firstTag.usageCount >= 0)
    }
    
    @Test
    fun `createTag should send correct request format`() = runTest {
        // Given
        val newTag = TestDataFactory.createTestTag()
        
        mockEngine.config.addHandler { request ->
            when {
                request.method == HttpMethod.Post && request.url.pathSegments.last() == "tags" -> {
                    // Verify request body
                    val requestBody = request.body.toByteArray().decodeToString()
                    val deserializedTag = Json.decodeFromString<Tag>(requestBody)
                    assertEquals(newTag.name, deserializedTag.name)
                    
                    respond(
                        content = Json.encodeToString(newTag.copy(id = "server-generated-id")),
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        
        // When
        val result = apiClient.createTag(newTag)
        
        // Then
        assertTrue(result.isSuccess)
        val createdTag = result.getOrThrow()
        assertEquals("server-generated-id", createdTag.id)
        assertEquals(newTag.name, createdTag.name)
    }
    
    @Test
    fun `API should handle rate limiting gracefully`() = runTest {
        // Given
        mockEngine.config.addHandler { request ->
            respond(
                content = """{"error": "Rate limit exceeded", "retry_after": 60}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf("application/json"),
                    "Retry-After" to listOf("60")
                )
            )
        }
        
        // When
        val result = apiClient.getAllTags()
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is RateLimitException)
        assertEquals(60, (exception as RateLimitException).retryAfterSeconds)
    }
}
```

### 4. Platform-Specific Tests

Platform-specific integration tests ensure that expect/actual implementations work correctly across different platforms.

#### Database Driver Integration

```kotlin
// androidUnitTest
class AndroidDatabaseDriverIntegrationTest {
    
    @Test
    fun `Android SQLite driver should handle large transactions`() = runTest {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val driver = AndroidDatabaseDriverFactory(context).createDriver()
        val database = HazardHawkDatabase(driver)
        
        val largeBatch = TestDataFactory.createLargeTagList(1000)
        
        // When
        val startTime = System.currentTimeMillis()
        database.transaction {
            largeBatch.forEach { tag ->
                database.tagQueries.insertTag(
                    id = tag.id,
                    name = tag.name,
                    category = tag.category,
                    usage_count = tag.usageCount.toLong(),
                    is_custom = if (tag.isCustom) 1L else 0L,
                    created_at = tag.createdAt,
                    updated_at = tag.updatedAt
                )
            }
        }
        val duration = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue(duration < 5000, "Large transaction should complete within 5 seconds")
        
        val insertedCount = database.tagQueries.countTags().executeAsOne()
        assertEquals(1000L, insertedCount, "All tags should be inserted")
        
        driver.close()
    }
}

// iosTest (similar structure for iOS)
class IOSDatabaseDriverIntegrationTest {
    
    @Test
    fun `iOS SQLite driver should handle concurrent access`() = runTest {
        // Given
        val driver = IOSDatabaseDriverFactory().createDriver()
        val database = HazardHawkDatabase(driver)
        
        // When - Simulate concurrent database access
        val jobs = (1..10).map { index ->
            async {
                val tag = TestDataFactory.createTestTag(id = "concurrent-$index")
                database.tagQueries.insertTag(
                    id = tag.id,
                    name = tag.name,
                    category = tag.category.name,
                    usage_count = tag.usageCount.toLong(),
                    is_custom = if (tag.isCustom) 1L else 0L,
                    created_at = tag.createdAt,
                    updated_at = tag.updatedAt
                )
            }
        }
        
        // Then
        assertDoesNotThrow {
            jobs.awaitAll()
        }
        
        val finalCount = database.tagQueries.countTags().executeAsOne()
        assertEquals(10L, finalCount, "All concurrent inserts should succeed")
        
        driver.close()
    }
}
```

## UI Testing

### 1. Compose Testing for Tag UI

Compose UI testing ensures that tag selection interfaces work correctly across different user interactions and states.

#### Tag Selection Dialog Testing

Based on the existing `TagSelectionFlowTest.kt`, here are additional comprehensive UI testing strategies:

```kotlin
class EnhancedTagSelectionUITest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    
    private lateinit var mockViewModel: TagSelectionViewModel
    
    @BeforeEach
    fun setUp() {
        mockViewModel = mockk(relaxed = true)
    }
    
    @Test
    fun `tag selection should handle rapid consecutive taps without duplication`() {
        // Given
        val testTag = TestDataFactory.createTestTag(name = "Rapid Tap Test")
        every { mockViewModel.recommendedTags } returns listOf(testTag)
        every { mockViewModel.selectedTags } returns mutableListOf()
        every { mockViewModel.selectTag(any()) } just Runs
        
        // When
        composeTestRule.setContent {
            TagSelectionDialog(
                isVisible = true,
                viewModel = mockViewModel,
                onDismiss = {},
                onConfirm = {}
            )
        }
        
        // Rapidly tap the tag multiple times
        repeat(5) {
            composeTestRule.onNodeWithText("Rapid Tap Test")
                .performClick()
        }
        
        // Then
        verify(exactly = 1) { mockViewModel.selectTag(testTag) }
    }
    
    @Test
    fun `tag selection should show loading state during search`() {
        // Given
        every { mockViewModel.isSearching } returns true
        every { mockViewModel.searchResults } returns emptyList()
        
        // When
        composeTestRule.setContent {
            TagSelectionDialog(
                isVisible = true,
                viewModel = mockViewModel,
                onDismiss = {},
                onConfirm = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Searching...")
            .assertIsDisplayed()
        
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }
    
    @Test
    fun `tag selection should handle keyboard navigation`() {
        // Given
        val tags = TestDataFactory.createPersonalTopTags().take(3)
        every { mockViewModel.recommendedTags } returns tags
        
        // When
        composeTestRule.setContent {
            TagSelectionDialog(
                isVisible = true,
                viewModel = mockViewModel,
                onDismiss = {},
                onConfirm = {}
            )
        }
        
        // Navigate using keyboard
        composeTestRule.onNodeWithText(tags[0].name)
            .requestFocus()
        
        composeTestRule.onNodeWithText(tags[0].name)
            .performKeyInput {
                pressKey(Key.Tab) // Move to next tag
            }
        
        // Then
        composeTestRule.onNodeWithText(tags[1].name)
            .assertIsFocused()
    }
    
    @Test
    fun `tag selection should maintain scroll position during updates`() {
        // Given
        val manyTags = TestDataFactory.createLargeTagList(50)
        every { mockViewModel.recommendedTags } returns manyTags
        
        // When
        composeTestRule.setContent {
            TagSelectionDialog(
                isVisible = true,
                viewModel = mockViewModel,
                onDismiss = {},
                onConfirm = {}
            )
        }
        
        // Scroll to bottom
        composeTestRule.onNodeWithTag("tag-list")
            .performScrollToIndex(40)
        
        // Update tags (simulate data refresh)
        val updatedTags = manyTags.map { it.copy(usageCount = it.usageCount + 1) }
        every { mockViewModel.recommendedTags } returns updatedTags
        
        // Force recomposition
        composeTestRule.runOnIdle {
            // Trigger recomposition
        }
        
        // Then - Should maintain scroll position
        composeTestRule.onNodeWithText(manyTags[40].name)
            .assertIsDisplayed()
    }
}
```

#### Performance-Focused UI Testing

```kotlin
class TagSelectionPerformanceTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    
    @Test
    fun `tag selection should render large lists within acceptable time`() {
        // Given
        val largeTagList = TestDataFactory.createLargeTagList(1000)
        val mockViewModel = mockk<TagSelectionViewModel>(relaxed = true) {
            every { recommendedTags } returns largeTagList
        }
        
        // When
        val renderStartTime = System.currentTimeMillis()
        composeTestRule.setContent {
            TagSelectionDialog(
                isVisible = true,
                viewModel = mockViewModel,
                onDismiss = {},
                onConfirm = {}
            )
        }
        
        // Wait for initial composition
        composeTestRule.waitForIdle()
        val renderDuration = System.currentTimeMillis() - renderStartTime
        
        // Then
        assertTrue(renderDuration < 1000, "Large list should render within 1 second, took ${renderDuration}ms")
        
        // Verify first few items are visible (lazy loading)
        composeTestRule.onNodeWithText(largeTagList[0].name)
            .assertIsDisplayed()
    }
    
    @Test
    fun `tag search should provide real-time feedback`() {
        // Given
        val mockViewModel = mockk<TagSelectionViewModel>(relaxed = true)
        val searchQuery = "safety"
        var searchCallbackTime = 0L
        
        every { mockViewModel.searchTags(any()) } answers {
            searchCallbackTime = System.currentTimeMillis()
        }
        
        // When
        composeTestRule.setContent {
            TagSelectionDialog(
                isVisible = true,
                viewModel = mockViewModel,
                onDismiss = {},
                onConfirm = {}
            )
        }
        
        val inputStartTime = System.currentTimeMillis()
        composeTestRule.onNodeWithText("Search tags...")
            .performTextInput(searchQuery)
        
        composeTestRule.waitForIdle()
        val responseTime = searchCallbackTime - inputStartTime
        
        // Then
        assertTrue(responseTime < 200, "Search should respond within 200ms, took ${responseTime}ms")
    }
}
```

### 2. SwiftUI Testing Patterns

For iOS SwiftUI testing, we need to ensure compatibility with the shared KMP ViewModels.

```swift
// iosApp/HazardHawkTests/TagSelectionViewTests.swift
import XCTest
import SwiftUI
@testable import HazardHawk

class TagSelectionViewTests: XCTestCase {
    
    var mockViewModel: MockTagSelectionViewModel!
    
    override func setUp() {
        super.setUp()
        mockViewModel = MockTagSelectionViewModel()
    }
    
    func testTagSelectionViewDisplaysRecommendedTags() {
        // Given
        let testTags = TestDataFactory().createPersonalTopTags()
        mockViewModel.recommendedTags = testTags
        
        // When
        let view = TagSelectionView(viewModel: mockViewModel)
        let hostingController = UIHostingController(rootView: view)
        
        // Then
        let expectation = XCTestExpectation(description: "Tags should be displayed")
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            let mirror = Mirror(reflecting: hostingController.rootView)
            // Verify tag presence in view hierarchy
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 1.0)
    }
    
    func testTagSelectionViewHandlesTapGestures() {
        // Given
        let testTag = TestDataFactory().createTestTag()
        mockViewModel.recommendedTags = [testTag]
        
        // When
        let view = TagSelectionView(viewModel: mockViewModel)
        let hostingController = UIHostingController(rootView: view)
        
        // Simulate tap gesture
        let tapGesture = UITapGestureRecognizer()
        hostingController.view.addGestureRecognizer(tapGesture)
        
        // Then
        XCTAssertEqual(mockViewModel.selectTagCallCount, 0)
        
        // Simulate tap
        tapGesture.state = .ended
        
        XCTAssertEqual(mockViewModel.selectTagCallCount, 1)
    }
}
```

### 3. Accessibility Testing

Accessibility testing ensures that tag management interfaces work correctly with assistive technologies.

#### Android Accessibility Testing

```kotlin
class TagSelectionAccessibilityTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    
    @Test
    fun `tag chips should have proper accessibility labels`() {
        // Given
        val testTag = TestDataFactory.createTestTag(name = "Fall Protection", usageCount = 15)
        val mockViewModel = mockk<TagSelectionViewModel>(relaxed = true) {
            every { recommendedTags } returns listOf(testTag)
            every { selectedTags } returns mutableListOf()
        }
        
        // When
        composeTestRule.setContent {
            TagSelectionDialog(
                isVisible = true,
                viewModel = mockViewModel,
                onDismiss = {},
                onConfirm = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Fall Protection")
            .assertHasClickAction()
            .assertIsNotSelected()
        
        composeTestRule.onNode(
            hasContentDescription("Select Fall Protection tag, used 15 times")
        ).assertExists()
    }
    
    @Test
    fun `selected tags should announce selection state`() {
        // Given
        val testTag = TestDataFactory.createTestTag(name = "Hard Hat Required")
        val mockViewModel = mockk<TagSelectionViewModel>(relaxed = true) {
            every { recommendedTags } returns listOf(testTag)
            every { selectedTags } returns mutableListOf(testTag)
        }
        
        // When
        composeTestRule.setContent {
            TagSelectionDialog(
                isVisible = true,
                viewModel = mockViewModel,
                onDismiss = {},
                onConfirm = {}
            )
        }
        
        // Then
        composeTestRule.onNode(
            hasContentDescription("Hard Hat Required tag is selected")
        ).assertExists()
    }
    
    @Test
    fun `search field should have proper accessibility hints`() {
        // Given
        val mockViewModel = mockk<TagSelectionViewModel>(relaxed = true)
        
        // When
        composeTestRule.setContent {
            TagSelectionDialog(
                isVisible = true,
                viewModel = mockViewModel,
                onDismiss = {},
                onConfirm = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Search tags...")
            .assertHasClickAction()
            .assertTextContains("Search tags...")
        
        composeTestRule.onNode(
            hasContentDescription("Search for safety tags by name or category")
        ).assertExists()
    }
}
```

#### iOS Accessibility Testing

```swift
class TagSelectionAccessibilityTests: XCTestCase {
    
    func testTagChipsHaveVoiceOverLabels() {
        // Given
        let testTag = TestDataFactory().createTestTag(name: "Safety Vest", usageCount: 10)
        let mockViewModel = MockTagSelectionViewModel()
        mockViewModel.recommendedTags = [testTag]
        
        // When
        let view = TagSelectionView(viewModel: mockViewModel)
        let hostingController = UIHostingController(rootView: view)
        
        // Then
        let tagButton = hostingController.view.subviews.first { view in
            view.accessibilityLabel == "Safety Vest tag, used 10 times"
        }
        
        XCTAssertNotNil(tagButton)
        XCTAssertEqual(tagButton?.accessibilityTraits, .button)
        XCTAssertEqual(tagButton?.accessibilityHint, "Double tap to select tag")
    }
    
    func testSelectedTagsAnnounceSelectionState() {
        // Given
        let testTag = TestDataFactory().createTestTag(name: "Hard Hat")
        let mockViewModel = MockTagSelectionViewModel()
        mockViewModel.selectedTags = [testTag]
        
        // When
        let view = TagSelectionView(viewModel: mockViewModel)
        let hostingController = UIHostingController(rootView: view)
        
        // Then
        let selectedTagButton = hostingController.view.subviews.first { view in
            view.accessibilityLabel?.contains("Hard Hat") == true &&
            view.accessibilityValue == "Selected"
        }
        
        XCTAssertNotNil(selectedTagButton)
    }
}
```

### 4. Gesture Testing

Testing gesture interactions ensures that tag selection works correctly with touch, mouse, and keyboard inputs.

#### Multi-Touch Gesture Testing

```kotlin
class TagSelectionGestureTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    
    @Test
    fun `tag selection should handle long press for context menu`() {
        // Given
        val testTag = TestDataFactory.createTestTag(name = "Context Menu Test")
        val mockViewModel = mockk<TagSelectionViewModel>(relaxed = true) {
            every { recommendedTags } returns listOf(testTag)
            every { showTagContextMenu(any()) } just Runs
        }
        
        // When
        composeTestRule.setContent {
            TagSelectionDialog(
                isVisible = true,
                viewModel = mockViewModel,
                onDismiss = {},
                onConfirm = {}
            )
        }
        
        composeTestRule.onNodeWithText("Context Menu Test")
            .performGesture {
                longClick()
            }
        
        // Then
        verify { mockViewModel.showTagContextMenu(testTag) }
        
        composeTestRule.onNodeWithText("Edit Tag")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove from Favorites")
            .assertIsDisplayed()
    }
    
    @Test
    fun `tag list should handle swipe to reveal actions`() {
        // Given
        val testTag = TestDataFactory.createTestTag(name = "Swipe Test")
        val mockViewModel = mockk<TagSelectionViewModel>(relaxed = true) {
            every { recommendedTags } returns listOf(testTag)
            every { addToFavorites(any()) } just Runs
        }
        
        // When
        composeTestRule.setContent {
            TagSelectionDialog(
                isVisible = true,
                viewModel = mockViewModel,
                onDismiss = {},
                onConfirm = {}
            )
        }
        
        composeTestRule.onNodeWithText("Swipe Test")
            .performGesture {
                swipeLeft()
            }
        
        // Then
        composeTestRule.onNode(hasContentDescription("Add to favorites"))
            .assertIsDisplayed()
            .performClick()
        
        verify { mockViewModel.addToFavorites(testTag) }
    }
    
    @Test
    fun `tag selection should support drag and drop reordering`() {
        // Given
        val tags = TestDataFactory.createPersonalTopTags().take(3)
        val mockViewModel = mockk<TagSelectionViewModel>(relaxed = true) {
            every { recommendedTags } returns tags
            every { reorderTags(any(), any()) } just Runs
        }
        
        // When
        composeTestRule.setContent {
            TagSelectionDialog(
                isVisible = true,
                viewModel = mockViewModel,
                onDismiss = {},
                onConfirm = {},
                allowReordering = true
            )
        }
        
        // Drag first tag to third position
        composeTestRule.onNodeWithText(tags[0].name)
            .performGesture {
                dragAndDrop(
                    start = center,
                    end = composeTestRule.onNodeWithText(tags[2].name).fetchSemanticsNode().boundsInRoot.center
                )
            }
        
        // Then
        verify { mockViewModel.reorderTags(0, 2) }
    }
}
```

## Performance Testing

### 1. Load Testing with Large Tag Sets

Performance testing ensures that the tag management system performs well under realistic load conditions.

#### Large Dataset Testing

```kotlin
class TagPerformanceTest {
    
    private lateinit var repository: TagRepository
    private lateinit var database: HazardHawkDatabase
    private lateinit var engine: TagRecommendationEngine
    
    @BeforeEach
    fun setUpPerformance() {
        val driver = TestDatabaseDriverFactory().createDriver()
        database = HazardHawkDatabase(driver)
        repository = TagRepositoryImpl(database, mockk(relaxed = true))
        engine = TagRecommendationEngine(repository)
        
        // Pre-populate with large dataset
        insertLargeDataset()
    }
    
    private fun insertLargeDataset() = runBlocking {
        val largeTags = TestDataFactory.createLargeTagList(10000)
        database.transaction {
            largeTags.forEach { tag ->
                database.tagQueries.insertTag(
                    id = tag.id,
                    name = tag.name,
                    category = tag.category,
                    usage_count = tag.usageCount.toLong(),
                    is_custom = if (tag.isCustom) 1L else 0L,
                    created_at = tag.createdAt,
                    updated_at = tag.updatedAt
                )
            }
        }
    }
    
    @Test
    fun `tag search should complete within 100ms for large datasets`() = runTest {
        // Given
        val searchQuery = "safety"
        
        // When
        val startTime = System.currentTimeMillis()
        val results = repository.searchTags(searchQuery, limit = 20)
        val duration = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue(duration < 100, "Search should complete within 100ms, took ${duration}ms")
        assertTrue(results.size <= 20, "Results should respect limit")
        assertTrue(results.all { it.name.contains(searchQuery, ignoreCase = true) })
    }
    
    @Test
    fun `tag recommendations should complete within 50ms for large datasets`() = runTest {
        // Given
        val userId = "performance-user"
        val projectId = "performance-project"
        
        // When
        val startTime = System.currentTimeMillis()
        val recommendations = engine.getRecommendedTags(userId, projectId)
        val duration = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue(duration < 50, "Recommendations should complete within 50ms, took ${duration}ms")
        assertEquals(8, recommendations.size, "Should return exactly 8 recommendations")
    }
    
    @Test
    fun `batch tag operations should scale linearly`() = runTest {
        // Test batch sizes: 10, 100, 1000
        val batchSizes = listOf(10, 100, 1000)
        val durations = mutableListOf<Long>()
        
        batchSizes.forEach { batchSize ->
            val photoIds = (1..batchSize).map { "photo-$it" }
            val tagIds = listOf("batch-tag-1", "batch-tag-2")
            
            val startTime = System.currentTimeMillis()
            repository.applyTagsToPhotos(photoIds, tagIds, "batch-user")
            val duration = System.currentTimeMillis() - startTime
            
            durations.add(duration)
        }
        
        // Verify linear scaling (within reasonable bounds)
        val ratio1 = durations[1].toDouble() / durations[0]
        val ratio2 = durations[2].toDouble() / durations[1]
        
        assertTrue(ratio1 < 20, "10x batch size should not take more than 20x time")
        assertTrue(ratio2 < 15, "10x batch size should not take more than 15x time")
    }
}
```

### 2. Stress Testing Sync Operations

Stress testing ensures that synchronization operations remain stable under high load.

#### Concurrent Operations Testing

```kotlin
class TagSyncStressTest {
    
    private lateinit var syncService: SyncService
    private lateinit var database: HazardHawkDatabase
    
    @BeforeEach
    fun setUpStressTest() {
        val driver = TestDatabaseDriverFactory().createDriver()
        database = HazardHawkDatabase(driver)
        val repository = TagRepositoryImpl(database, mockk(relaxed = true))
        val mockNetworkApi = mockk<TagNetworkApi>(relaxed = true)
        syncService = SyncService(repository, mockNetworkApi)
    }
    
    @Test
    fun `sync should handle concurrent tag updates without data corruption`() = runTest {
        // Given
        val concurrentUsers = 20
        val tagsPerUser = 50
        
        coEvery { 
            any<TagNetworkApi>().getAllTags() 
        } returns TestDataFactory.createLargeTagList(1000)
        
        // When - Simulate concurrent sync operations
        val jobs = (1..concurrentUsers).map { userId ->
            async {
                repeat(tagsPerUser) { tagIndex ->
                    val tag = TestDataFactory.createTestTag(
                        id = "user-$userId-tag-$tagIndex",
                        name = "User $userId Tag $tagIndex"
                    )
                    database.tagQueries.insertTag(
                        id = tag.id,
                        name = tag.name,
                        category = tag.category,
                        usage_count = tag.usageCount.toLong(),
                        is_custom = if (tag.isCustom) 1L else 0L,
                        created_at = tag.createdAt,
                        updated_at = tag.updatedAt
                    )
                }
                syncService.syncTags()
            }
        }
        
        val results = jobs.awaitAll()
        
        // Then
        assertTrue(results.all { it.isSuccess }, "All concurrent syncs should succeed")
        
        val finalTagCount = database.tagQueries.countTags().executeAsOne()
        assertTrue(finalTagCount >= (concurrentUsers * tagsPerUser), 
                  "All tags should be preserved during concurrent operations")
    }
    
    @Test
    fun `sync should maintain consistency under network interruptions`() = runTest {
        // Given
        val networkFailureCount = AtomicInteger(0)
        val maxFailures = 3
        
        coEvery { 
            any<TagNetworkApi>().getAllTags() 
        } answers {
            if (networkFailureCount.getAndIncrement() < maxFailures) {
                throw NetworkException("Simulated network failure")
            } else {
                TestDataFactory.createIndustryStandardTags()
            }
        }
        
        // When - Attempt sync multiple times with retries
        var lastResult: Result<Unit>? = null
        repeat(5) { attempt ->
            lastResult = syncService.syncTags()
            if (lastResult?.isSuccess == true) return@repeat
            delay(100) // Brief delay between retries
        }
        
        // Then
        assertNotNull(lastResult)
        assertTrue(lastResult!!.isSuccess, "Sync should eventually succeed after network recovery")
        
        val syncedTags = database.tagQueries.getAllTags().executeAsList()
        assertTrue(syncedTags.isNotEmpty(), "Tags should be synced after network recovery")
    }
}
```

### 3. Memory Leak Detection

Memory leak detection ensures that tag management operations don't cause memory accumulation over time.

#### Memory Usage Monitoring

```kotlin
class TagMemoryLeakTest {
    
    @Test
    fun `tag operations should not leak memory during extended use`() = runTest {
        // Given
        val driver = TestDatabaseDriverFactory().createDriver()
        val database = HazardHawkDatabase(driver)
        val repository = TagRepositoryImpl(database, mockk(relaxed = true))
        
        // Measure initial memory
        System.gc()
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // When - Perform many tag operations
        repeat(10000) { iteration ->
            val tag = TestDataFactory.createTestTag(id = "memory-test-$iteration")
            repository.saveTag(tag)
            repository.getTag(tag.id)
            repository.deleteTag(tag.id)
            
            // Occasional cleanup
            if (iteration % 1000 == 0) {
                System.gc()
                delay(10)
            }
        }
        
        // Force garbage collection
        System.gc()
        delay(100)
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Then
        val memoryIncrease = finalMemory - initialMemory
        val memoryIncreaseKB = memoryIncrease / 1024
        
        assertTrue(memoryIncreaseKB < 1024, 
                  "Memory increase should be less than 1MB, actual: ${memoryIncreaseKB}KB")
        
        driver.close()
    }
    
    @Test
    fun `tag cache should evict old entries to prevent memory bloat`() = runTest {
        // Given
        val cacheSize = 100
        val cache = LRUCache<String, Tag>(cacheSize)
        val totalTags = 1000
        
        // When - Add more tags than cache capacity
        repeat(totalTags) { index ->
            val tag = TestDataFactory.createTestTag(id = "cache-test-$index")
            cache.put(tag.id, tag)
        }
        
        // Then
        assertTrue(cache.size() <= cacheSize, "Cache should not exceed maximum size")
        
        // Verify oldest entries are evicted
        val oldestId = "cache-test-0"
        assertNull(cache.get(oldestId), "Oldest entry should be evicted")
        
        // Verify newest entries are retained
        val newestId = "cache-test-${totalTags - 1}"
        assertNotNull(cache.get(newestId), "Newest entry should be retained")
    }
}
```

### 4. Battery Usage Profiling

Battery usage profiling ensures that tag management operations are energy-efficient, particularly important for mobile applications.

#### Energy Efficiency Testing

```kotlin
// Android-specific battery testing
class TagBatteryUsageTest {
    
    @Test
    fun `tag sync should minimize wake locks and CPU usage`() = runTest {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TagTest:SyncWakeLock"
        )
        
        val repository = TagRepositoryImpl(
            HazardHawkDatabase(TestDatabaseDriverFactory().createDriver()),
            mockk(relaxed = true)
        )
        
        // When
        wakeLock.acquire(10000) // Max 10 seconds
        val startTime = SystemClock.elapsedRealtime()
        val startCpuTime = Process.getElapsedCpuTime()
        
        try {
            // Perform sync operations
            repeat(100) {
                val tag = TestDataFactory.createTestTag()
                repository.saveTag(tag)
                repository.incrementTagUsage(tag.id, "user-1")
            }
        } finally {
            wakeLock.release()
        }
        
        val endTime = SystemClock.elapsedRealtime()
        val endCpuTime = Process.getElapsedCpuTime()
        
        // Then
        val wallClockTime = endTime - startTime
        val cpuTime = endCpuTime - startCpuTime
        val cpuUsagePercent = (cpuTime.toDouble() / wallClockTime) * 100
        
        assertTrue(cpuUsagePercent < 20, 
                  "CPU usage should be less than 20%, actual: $cpuUsagePercent%")
        assertTrue(wallClockTime < 5000, 
                  "Operations should complete within 5 seconds, took: ${wallClockTime}ms")
    }
}
```

## Platform-Specific Testing

### 1. Android Testing Specifics

Android testing requires special consideration for activities, fragments, and Android-specific components.

#### Android ViewModel Testing

```kotlin
class AndroidTagViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var viewModel: TagSelectionViewModel
    private lateinit var mockRepository: TagRepository
    
    @BeforeEach
    fun setUp() {
        mockRepository = mockk(relaxed = true)
        viewModel = TagSelectionViewModel(mockRepository, mockk(), mockk())
    }
    
    @Test
    fun `viewModel should handle Android lifecycle correctly`() = runTest {
        // Given
        val testTags = TestDataFactory.createPersonalTopTags()
        coEvery { mockRepository.getAllTags() } returns flowOf(testTags)
        
        // When - Simulate lifecycle events
        viewModel.onStart()
        
        // Then - ViewModel should load data
        assertEquals(testTags, viewModel.uiState.value.allTags)
        
        // When - Simulate app backgrounding
        viewModel.onStop()
        
        // Then - ViewModel should handle background state
        assertFalse(viewModel.uiState.value.isLoading)
    }
    
    @Test
    fun `viewModel should persist state during configuration changes`() = runTest {
        // Given
        val selectedTags = TestDataFactory.createPersonalTopTags().take(2)
        selectedTags.forEach { viewModel.selectTag(it.id) }
        
        // When - Simulate configuration change
        val savedState = viewModel.saveState()
        val newViewModel = TagSelectionViewModel(mockRepository, mockk(), mockk())
        newViewModel.restoreState(savedState)
        
        // Then
        assertEquals(selectedTags.map { it.id }.toSet(), 
                    newViewModel.uiState.value.selectedTagIds)
    }
}
```

### 2. iOS Testing Specifics

iOS testing focuses on Swift interoperability and platform-specific behaviors.

```swift
// iosApp/HazardHawkTests/TagRepositoryIOSTest.swift
import XCTest
@testable import shared

class TagRepositoryIOSTest: XCTestCase {
    
    var repository: TagRepository!
    var database: HazardHawkDatabase!
    
    override func setUp() {
        super.setUp()
        let driver = IOSDatabaseDriverFactory().createDriver()
        database = HazardHawkDatabase(driver: driver)
        repository = TagRepositoryImpl(database: database, cacheManager: MockTagCacheManager())
    }
    
    func testRepositoryShouldHandleIOSBackgroundTransitions() async throws {
        // Given
        let testTag = TestDataFactory().createTestTag()
        
        // When - App enters background
        NotificationCenter.default.post(name: UIApplication.didEnterBackgroundNotification, object: nil)
        
        let result = try await repository.saveTag(tag: testTag)
        
        // Then
        XCTAssertTrue(result.isSuccess)
        
        // When - App returns to foreground
        NotificationCenter.default.post(name: UIApplication.willEnterForegroundNotification, object: nil)
        
        let retrievedTag = try await repository.getTag(id: testTag.id)
        XCTAssertNotNil(retrievedTag)
        XCTAssertEqual(retrievedTag?.name, testTag.name)
    }
    
    func testRepositoryShouldHandleMemoryWarnings() async throws {
        // Given
        let largeBatch = TestDataFactory().createLargeTagList(count: 1000)
        
        // When - Simulate memory warning
        NotificationCenter.default.post(name: UIApplication.didReceiveMemoryWarningNotification, object: nil)
        
        // Save tags after memory warning
        for tag in largeBatch {
            let result = try await repository.saveTag(tag: tag)
            XCTAssertTrue(result.isSuccess)
        }
        
        // Then - All tags should be saved successfully
        let allTags = try await repository.getAllTags().collect()
        XCTAssertGreaterThanOrEqual(allTags.count, largeBatch.count)
    }
}
```

### 3. Desktop Testing Specifics

Desktop testing covers JVM-specific concerns and desktop UI patterns.

```kotlin
// desktopTest
class DesktopTagRepositoryTest {
    
    @Test
    fun `desktop repository should handle file system permissions`() = runTest {
        // Given
        val tempDir = Files.createTempDirectory("tag-test")
        val databasePath = tempDir.resolve("test.db").toString()
        
        val driver = DesktopDatabaseDriverFactory(databasePath).createDriver()
        val database = HazardHawkDatabase(driver)
        val repository = TagRepositoryImpl(database, mockk(relaxed = true))
        
        // When - Test with various file permissions
        val testTag = TestDataFactory.createTestTag()
        val result = repository.saveTag(testTag)
        
        // Then
        assertTrue(result.isSuccess)
        
        // Verify file was created with correct permissions
        val dbFile = File(databasePath)
        assertTrue(dbFile.exists())
        assertTrue(dbFile.canRead())
        assertTrue(dbFile.canWrite())
        
        // Cleanup
        driver.close()
        Files.deleteIfExists(tempDir.resolve("test.db"))
        Files.deleteIfExists(tempDir)
    }
    
    @Test
    fun `desktop app should handle concurrent database access from multiple windows`() = runTest {
        // Given
        val tempDir = Files.createTempDirectory("concurrent-test")
        val databasePath = tempDir.resolve("concurrent.db").toString()
        
        val numWindows = 3
        val repositories = (1..numWindows).map {
            val driver = DesktopDatabaseDriverFactory(databasePath).createDriver()
            val database = HazardHawkDatabase(driver)
            TagRepositoryImpl(database, mockk(relaxed = true))
        }
        
        // When - Each "window" saves tags concurrently
        val jobs = repositories.mapIndexed { index, repo ->
            async {
                val tag = TestDataFactory.createTestTag(id = "window-$index-tag")
                repo.saveTag(tag)
            }
        }
        
        val results = jobs.awaitAll()
        
        // Then
        assertTrue(results.all { it.isSuccess })
        
        // Verify all tags were saved
        val firstRepo = repositories.first()
        val allTags = firstRepo.getAllTags().first()
        assertEquals(numWindows, allTags.size)
        
        // Cleanup
        repositories.forEach { repo ->
            (repo as TagRepositoryImpl).database.driver.close()
        }
        Files.deleteIfExists(tempDir.resolve("concurrent.db"))
        Files.deleteIfExists(tempDir)
    }
}
```

### 4. Web Testing Specifics

Web testing focuses on browser-specific behaviors and JavaScript interop.

```kotlin
// jsTest
class WebTagRepositoryTest {
    
    @Test
    fun testWebRepositoryShouldHandleBrowserStorageQuotas() = runTest {
        // Given
        val mockStorage = MockWebStorage(quotaBytes = 5 * 1024 * 1024) // 5MB quota
        val repository = WebTagRepository(mockStorage)
        
        // When - Fill storage near quota
        val largeTags = TestDataFactory.createLargeTagList(10000)
        val results = largeTags.map { tag ->
            repository.saveTag(tag)
        }
        
        // Then - Repository should handle quota exceeded gracefully
        val successCount = results.count { it.isSuccess }
        val failureCount = results.count { it.isFailure }
        
        assertTrue(successCount > 0, "Some tags should be saved successfully")
        
        if (failureCount > 0) {
            // Verify failures are due to quota, not other errors
            val quotaErrors = results.filter { it.isFailure }.map { it.exceptionOrNull() }
            assertTrue(quotaErrors.all { it is QuotaExceededException })
        }
    }
    
    @Test
    fun testWebRepositoryShouldHandleOfflineMode() = runTest {
        // Given
        val mockNetwork = MockNetworkDetector(isOnline = false)
        val repository = WebTagRepository(mockNetwork = mockNetwork)
        
        // When
        val testTag = TestDataFactory.createTestTag()
        val result = repository.saveTag(testTag)
        
        // Then - Should queue for later sync
        assertTrue(result.isSuccess)
        
        val queuedOperations = repository.getQueuedOperations()
        assertEquals(1, queuedOperations.size)
        assertEquals("SAVE_TAG", queuedOperations.first().type)
        
        // When - Network comes back online
        mockNetwork.isOnline = true
        val syncResult = repository.syncQueuedOperations()
        
        // Then
        assertTrue(syncResult.isSuccess)
        assertEquals(0, repository.getQueuedOperations().size)
    }
}
```

## Testing Infrastructure

### 1. Test Data Management

Effective test data management ensures consistent, maintainable tests across all platforms.

#### Enhanced TestDataFactory

```kotlin
// Enhanced version of existing TestDataFactory with more comprehensive data generation
object EnhancedTestDataFactory : TestDataFactory() {
    
    // Hierarchical data generation
    fun createTagHierarchy(): TagHierarchy {
        return TagHierarchy(
            categories = TagCategory.values().toList(),
            industryStandards = createIndustryStandardTags(),
            projectSpecific = createProjectTopTags(),
            userPersonal = createPersonalTopTags(),
            customTags = createCustomPromotableTags()
        )
    }
    
    // Context-specific data
    fun createContextualTags(context: TagContext, count: Int = 10): List<Tag> {
        return when (context) {
            TagContext.PHOTO_TAGGING -> {
                listOf(
                    createTestTag(name = "Fall Protection", category = "FALL_PROTECTION"),
                    createTestTag(name = "PPE Required", category = "PPE"),
                    createTestTag(name = "Guardrail Missing", category = "FALL_PROTECTION"),
                    createTestTag(name = "Hard Hat Zone", category = "PPE")
                )
            }
            TagContext.INCIDENT_REPORT -> {
                listOf(
                    createTestTag(name = "Equipment Failure", category = "EQUIPMENT"),
                    createTestTag(name = "Near Miss", category = "SAFETY"),
                    createTestTag(name = "Injury", category = "MEDICAL"),
                    createTestTag(name = "Property Damage", category = "EQUIPMENT")
                )
            }
            TagContext.PRE_TASK_PLANNING -> {
                listOf(
                    createTestTag(name = "Hot Work Permit", category = "HOT_WORK"),
                    createTestTag(name = "Confined Space", category = "CONFINED_SPACE"),
                    createTestTag(name = "Crane Lift Plan", category = "CRANE_LIFT"),
                    createTestTag(name = "LOTO Required", category = "ELECTRICAL")
                )
            }
        }
    }
    
    // Performance test data with controlled characteristics
    fun createPerformanceTagSet(
        totalCount: Int,
        searchablePercentage: Double = 0.3,
        recentPercentage: Double = 0.1
    ): TagTestSet {
        val allTags = createLargeTagList(totalCount)
        val searchableCount = (totalCount * searchablePercentage).toInt()
        val recentCount = (totalCount * recentPercentage).toInt()
        
        return TagTestSet(
            allTags = allTags,
            searchableTags = allTags.take(searchableCount),
            recentTags = allTags.takeLast(recentCount),
            expectedSearchResults = generateExpectedSearchResults(allTags)
        )
    }
    
    // Stress test scenarios
    fun createStressTestScenario(name: String): StressTestScenario {
        return when (name) {
            "high_volume_sync" -> StressTestScenario(
                tags = createLargeTagList(10000),
                concurrentUsers = 50,
                operationsPerUser = 100,
                expectedDuration = 30.seconds
            )
            "rapid_selection" -> StressTestScenario(
                tags = createLargeTagList(1000),
                concurrentUsers = 10,
                operationsPerUser = 1000,
                expectedDuration = 10.seconds
            )
            else -> throw IllegalArgumentException("Unknown stress test scenario: $name")
        }
    }
    
    private fun generateExpectedSearchResults(allTags: List<Tag>): Map<String, List<Tag>> {
        return mapOf(
            "safety" to allTags.filter { it.name.contains("safety", ignoreCase = true) },
            "fall" to allTags.filter { it.name.contains("fall", ignoreCase = true) },
            "ppe" to allTags.filter { it.category.contains("PPE") },
            "hard hat" to allTags.filter { it.name.contains("hard hat", ignoreCase = true) }
        )
    }
}

data class TagHierarchy(
    val categories: List<TagCategory>,
    val industryStandards: List<Tag>,
    val projectSpecific: List<Tag>,
    val userPersonal: List<Tag>,
    val customTags: List<Tag>
)

data class TagTestSet(
    val allTags: List<Tag>,
    val searchableTags: List<Tag>,
    val recentTags: List<Tag>,
    val expectedSearchResults: Map<String, List<Tag>>
)

data class StressTestScenario(
    val tags: List<Tag>,
    val concurrentUsers: Int,
    val operationsPerUser: Int,
    val expectedDuration: Duration
)
```

#### Test Database Management

```kotlin
// Centralized test database management
class TestDatabaseManager {
    
    companion object {
        private val testDatabases = mutableMapOf<String, HazardHawkDatabase>()
        
        fun getOrCreateDatabase(testName: String, prePopulated: Boolean = true): HazardHawkDatabase {
            return testDatabases.getOrPut(testName) {
                val driver = TestDatabaseDriverFactory().createDriver()
                val database = HazardHawkDatabase(driver)
                
                if (prePopulated) {
                    populateTestData(database)
                }
                
                database
            }
        }
        
        fun cleanupDatabase(testName: String) {
            testDatabases[testName]?.let { database ->
                database.driver.close()
                testDatabases.remove(testName)
            }
        }
        
        fun cleanupAllDatabases() {
            testDatabases.values.forEach { it.driver.close() }
            testDatabases.clear()
        }
        
        private fun populateTestData(database: HazardHawkDatabase) {
            val testTags = EnhancedTestDataFactory.createTagHierarchy()
            
            database.transaction {
                // Insert industry standard tags
                testTags.industryStandards.forEach { tag ->
                    database.tagQueries.insertTag(
                        id = tag.id,
                        name = tag.name,
                        category = tag.category,
                        usage_count = tag.usageCount.toLong(),
                        is_custom = if (tag.isCustom) 1L else 0L,
                        created_at = tag.createdAt,
                        updated_at = tag.updatedAt
                    )
                }
                
                // Insert project and personal tags
                (testTags.projectSpecific + testTags.userPersonal + testTags.customTags).forEach { tag ->
                    database.tagQueries.insertTag(
                        id = tag.id,
                        name = tag.name,
                        category = tag.category,
                        usage_count = tag.usageCount.toLong(),
                        is_custom = if (tag.isCustom) 1L else 0L,
                        created_at = tag.createdAt,
                        updated_at = tag.updatedAt
                    )
                }
            }
        }
    }
}
```

### 2. Mock Factories and Builders

Comprehensive mocking infrastructure reduces test setup complexity and improves maintainability.

#### Advanced Mock Repository

```kotlin
class AdvancedMockTagRepository : TagRepository {
    
    private val tags = mutableMapOf<String, Tag>()
    private val usageCounts = mutableMapOf<String, Int>()
    private val userPreferences = mutableMapOf<String, List<String>>()
    
    // Configurable behavior
    var simulateNetworkDelay: Duration = Duration.ZERO
    var simulateErrors: Boolean = false
    var errorProbability: Double = 0.0
    
    init {
        // Pre-populate with test data
        EnhancedTestDataFactory.createTagHierarchy().let { hierarchy ->
            (hierarchy.industryStandards + hierarchy.projectSpecific + 
             hierarchy.userPersonal + hierarchy.customTags).forEach { tag ->
                tags[tag.id] = tag
                usageCounts[tag.id] = tag.usageCount
            }
        }
    }
    
    override suspend fun saveTag(tag: Tag): Result<Tag> {
        if (simulateErrors && Random.nextDouble() < errorProbability) {
            return Result.failure(Exception("Simulated repository error"))
        }
        
        delay(simulateNetworkDelay)
        tags[tag.id] = tag
        return Result.success(tag)
    }
    
    override suspend fun getAllTags(): Flow<List<Tag>> {
        delay(simulateNetworkDelay)
        return flowOf(tags.values.toList().sortedByDescending { it.usageCount })
    }
    
    override suspend fun searchTags(
        query: String,
        filters: TagSearchFilters
    ): List<Tag> {
        delay(simulateNetworkDelay)
        
        return tags.values
            .filter { tag ->
                tag.name.contains(query, ignoreCase = true) &&
                (filters.categories.isEmpty() || tag.category in filters.categories) &&
                (!filters.customOnly || tag.isCustom) &&
                (filters.projectSpecific == null || tag.projectSpecific == filters.projectSpecific) &&
                tag.usageCount >= filters.minUsageCount
            }
            .sortedByDescending { it.usageCount }
    }
    
    override suspend fun getRecommendedTags(
        userId: String,
        projectId: String?,
        context: TagContext,
        limit: Int
    ): List<Tag> {
        delay(simulateNetworkDelay)
        
        val userTags = userPreferences[userId] ?: emptyList()
        val contextTags = EnhancedTestDataFactory.createContextualTags(context)
        
        return (tags.values.filter { it.id in userTags } + contextTags)
            .distinctBy { it.id }
            .sortedByDescending { it.usageCount }
            .take(limit)
    }
    
    override suspend fun incrementTagUsage(tagId: String, userId: String): Result<Unit> {
        delay(simulateNetworkDelay)
        
        usageCounts[tagId] = (usageCounts[tagId] ?: 0) + 1
        tags[tagId]?.let { tag ->
            tags[tagId] = tag.copy(
                usageCount = usageCounts[tagId] ?: tag.usageCount,
                lastUsed = Clock.System.now().toEpochMilliseconds()
            )
        }
        
        return Result.success(Unit)
    }
    
    // Test configuration methods
    fun setUserPreferences(userId: String, preferredTagIds: List<String>) {
        userPreferences[userId] = preferredTagIds
    }
    
    fun simulateHighLatency() {
        simulateNetworkDelay = 2.seconds
    }
    
    fun simulateIntermittentErrors(probability: Double = 0.1) {
        simulateErrors = true
        errorProbability = probability
    }
    
    fun resetToDefaults() {
        simulateNetworkDelay = Duration.ZERO
        simulateErrors = false
        errorProbability = 0.0
    }
}
```

### 3. Test Configuration Management

Centralized test configuration ensures consistent test environments across platforms.

#### Test Configuration System

```kotlin
object TestConfiguration {
    
    data class TestSettings(
        val databaseType: DatabaseType = DatabaseType.IN_MEMORY,
        val networkSimulation: NetworkSimulation = NetworkSimulation.NONE,
        val performanceThresholds: PerformanceThresholds = PerformanceThresholds.DEFAULT,
        val logLevel: LogLevel = LogLevel.ERROR
    )
    
    enum class DatabaseType {
        IN_MEMORY,
        TEMPORARY_FILE,
        PERSISTENT_TEST_DB
    }
    
    enum class NetworkSimulation {
        NONE,
        SLOW_NETWORK,
        INTERMITTENT_FAILURES,
        OFFLINE_MODE
    }
    
    data class PerformanceThresholds(
        val maxSearchTime: Duration,
        val maxRecommendationTime: Duration,
        val maxSyncTime: Duration,
        val maxMemoryIncrease: Long // in bytes
    ) {
        companion object {
            val DEFAULT = PerformanceThresholds(
                maxSearchTime = 100.milliseconds,
                maxRecommendationTime = 50.milliseconds,
                maxSyncTime = 5.seconds,
                maxMemoryIncrease = 1024 * 1024 // 1MB
            )
            
            val STRICT = PerformanceThresholds(
                maxSearchTime = 50.milliseconds,
                maxRecommendationTime = 25.milliseconds,
                maxSyncTime = 2.seconds,
                maxMemoryIncrease = 512 * 1024 // 512KB
            )
        }
    }
    
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }
    
    // Environment-based configuration
    fun getSettingsForEnvironment(environment: String): TestSettings {
        return when (environment) {
            "unit" -> TestSettings(
                databaseType = DatabaseType.IN_MEMORY,
                networkSimulation = NetworkSimulation.NONE,
                performanceThresholds = PerformanceThresholds.STRICT
            )
            "integration" -> TestSettings(
                databaseType = DatabaseType.TEMPORARY_FILE,
                networkSimulation = NetworkSimulation.SLOW_NETWORK,
                performanceThresholds = PerformanceThresholds.DEFAULT
            )
            "stress" -> TestSettings(
                databaseType = DatabaseType.PERSISTENT_TEST_DB,
                networkSimulation = NetworkSimulation.INTERMITTENT_FAILURES,
                performanceThresholds = PerformanceThresholds.DEFAULT.copy(
                    maxSyncTime = 30.seconds
                )
            )
            else -> TestSettings()
        }
    }
}

// Test configuration annotation
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestConfig(
    val environment: String = "default",
    val timeout: Long = 10000,
    val repeatCount: Int = 1
)
```

### 4. Continuous Integration Setup

CI/CD configuration ensures tests run reliably across all platforms and environments.

#### GitHub Actions Workflow Configuration

```yaml
# .github/workflows/test.yml
name: Comprehensive Testing

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          
      - name: Setup Gradle
        uses: gradle/gradle-build-action@v2
        
      - name: Run Common Tests
        run: ./gradlew :shared:commonTest
        
      - name: Run Android Unit Tests
        run: ./gradlew :shared:androidUnitTest
        
      - name: Run Desktop Tests
        run: ./gradlew :shared:desktopTest

  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:13
        env:
          POSTGRES_PASSWORD: test_password
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          
      - name: Run Integration Tests
        run: ./gradlew :shared:integrationTest
        env:
          TEST_DATABASE_URL: jdbc:postgresql://localhost:5432/test_db
          
  android-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          
      - name: Setup Android SDK
        uses: android-actions/setup-android@v2
        
      - name: AVD cache
        uses: actions/cache@v3
        id: avd-cache
        with:
          path: |
            ~/.android/avd/*
            ~/.android/adb*
          key: avd-api-30
          
      - name: Create AVD and generate snapshot for caching
        if: steps.avd-cache.outputs.cache-hit != 'true'
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 30
          force-avd-creation: false
          emulator-options: -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -camera-back none
          disable-animations: false
          script: echo "Generated AVD snapshot for caching."
          
      - name: Run Android Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 30
          script: ./gradlew :androidApp:connectedDebugAndroidTest

  ios-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Xcode
        uses: maxim-lobanov/setup-xcode@v1
        with:
          xcode-version: '15.0'
          
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          
      - name: Run iOS Tests
        run: |
          cd iosApp
          xcodebuild test -project HazardHawk.xcodeproj -scheme HazardHawk -destination 'platform=iOS Simulator,name=iPhone 15,OS=17.0'

  performance-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          
      - name: Run Performance Tests
        run: ./gradlew :shared:performanceTest
        
      - name: Upload Performance Results
        uses: actions/upload-artifact@v3
        with:
          name: performance-results
          path: build/reports/performance/

  coverage:
    runs-on: ubuntu-latest
    needs: [unit-tests, integration-tests]
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          
      - name: Generate Coverage Report
        run: ./gradlew koverXmlReport
        
      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: build/reports/kover/xml/report.xml
          flags: unittests
          name: kotlin-multiplatform-coverage
```

## Best Practices and Recommendations

### 1. Testing Strategy Guidelines

#### Test Pyramid Implementation

```kotlin
/**
 * Test Pyramid for Tag Management System
 * 
 * Unit Tests (70%):
 * - Repository implementations
 * - Business logic (recommendation algorithms)
 * - Data model validation
 * - Utility functions
 * 
 * Integration Tests (20%):
 * - Database operations with real SQLDelight
 * - API integration with mock servers
 * - Cross-component workflows
 * 
 * UI Tests (10%):
 * - Critical user journeys
 * - Platform-specific UI components
 * - Accessibility compliance
 */

abstract class BaseTestStrategy {
    
    protected fun executeTestPyramid(
        unitTestSuite: () -> Unit,
        integrationTestSuite: () -> Unit,
        uiTestSuite: () -> Unit
    ) {
        // Execute in order of increasing cost and decreasing frequency
        try {
            unitTestSuite()
        } catch (e: Exception) {
            throw AssertionError("Unit tests failed - stopping test execution", e)
        }
        
        integrationTestSuite()
        uiTestSuite()
    }
}
```

#### Test Naming and Organization

```kotlin
/**
 * Test naming convention:
 * [Method/Feature]_should_[ExpectedBehavior]_when_[Condition]
 */
class TagRecommendationEngineTest {
    
    @Nested
    @DisplayName("Weighted Algorithm Tests")
    inner class WeightedAlgorithmTests {
        
        @Test
        fun `getRecommendedTags should return 8 tags maximum when sufficient data available`() {
            // Test implementation
        }
        
        @Test
        fun `getRecommendedTags should apply 40-30-30 weight distribution when all categories have data`() {
            // Test implementation
        }
        
        @Test
        fun `getRecommendedTags should handle empty categories gracefully when some categories are empty`() {
            // Test implementation
        }
    }
    
    @Nested
    @DisplayName("Recency Boost Tests")
    inner class RecencyBoostTests {
        
        @Test
        fun `getRecommendedTagsWithRecency should boost tags used within 7 days when recent usage exists`() {
            // Test implementation
        }
    }
}
```

### 2. Mock Strategy Best Practices

#### Smart Mock Implementation

```kotlin
/**
 * Mock Strategy Guidelines:
 * 1. Use real implementations for value objects (data classes)
 * 2. Mock external dependencies (network, file system)
 * 3. Use in-memory implementations for databases in unit tests
 * 4. Create configurable mocks for different test scenarios
 */

interface MockConfiguration {
    val simulateLatency: Boolean
    val failureRate: Double
    val dataSize: Int
}

class ConfigurableMockTagRepository(
    private val config: MockConfiguration
) : TagRepository {
    
    override suspend fun getAllTags(): Flow<List<Tag>> {
        if (config.simulateLatency) {
            delay(Random.nextLong(50, 200))
        }
        
        if (Random.nextDouble() < config.failureRate) {
            throw NetworkException("Simulated network failure")
        }
        
        val tags = EnhancedTestDataFactory.createLargeTagList(config.dataSize)
        return flowOf(tags)
    }
    
    // Other repository methods...
}
```

### 3. Performance Testing Guidelines

#### Performance Assertion Framework

```kotlin
class PerformanceAssertion {
    
    companion object {
        inline fun <T> assertPerformance(
            operation: String,
            maxDuration: Duration,
            maxMemoryIncrease: Long = 0L,
            block: () -> T
        ): T {
            val initialMemory = if (maxMemoryIncrease > 0) getCurrentMemoryUsage() else 0L
            
            val startTime = System.currentTimeMillis()
            val result = block()
            val actualDuration = (System.currentTimeMillis() - startTime).milliseconds
            
            assertTrue(
                actualDuration <= maxDuration,
                "$operation took ${actualDuration.inWholeMilliseconds}ms, expected <= ${maxDuration.inWholeMilliseconds}ms"
            )
            
            if (maxMemoryIncrease > 0) {
                System.gc()
                delay(100) // Allow GC to complete
                val finalMemory = getCurrentMemoryUsage()
                val memoryIncrease = finalMemory - initialMemory
                
                assertTrue(
                    memoryIncrease <= maxMemoryIncrease,
                    "$operation increased memory by ${memoryIncrease}KB, expected <= ${maxMemoryIncrease}KB"
                )
            }
            
            return result
        }
        
        private fun getCurrentMemoryUsage(): Long {
            val runtime = Runtime.getRuntime()
            return (runtime.totalMemory() - runtime.freeMemory()) / 1024 // Convert to KB
        }
    }
}

// Usage example
@Test
fun `tag search should meet performance requirements`() = runTest {
    val repository = TagRepositoryImpl(database, cacheManager)
    
    val result = PerformanceAssertion.assertPerformance(
        operation = "Tag search",
        maxDuration = 100.milliseconds,
        maxMemoryIncrease = 100 * 1024 // 100KB
    ) {
        repository.searchTags("safety equipment", limit = 20)
    }
    
    assertTrue(result.isNotEmpty())
}
```

### 4. Cross-Platform Testing Consistency

#### Platform Test Synchronization

```kotlin
/**
 * Ensures consistent test behavior across platforms
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CrossPlatformTest(
    val platforms: Array<Platform> = [Platform.ANDROID, Platform.IOS, Platform.DESKTOP, Platform.WEB]
)

enum class Platform {
    ANDROID, IOS, DESKTOP, WEB
}

abstract class CrossPlatformTestBase {
    
    protected fun runOnAllPlatforms(testBlock: suspend () -> Unit) = runTest {
        // This would be implemented differently for each platform target
        // Common test logic goes here
        testBlock()
    }
    
    protected fun skipOnPlatform(platform: Platform, reason: String = "Platform limitation") {
        val currentPlatform = getCurrentPlatform()
        assumeTrue(currentPlatform != platform, "Skipped on $platform: $reason")
    }
    
    private fun getCurrentPlatform(): Platform {
        return when {
            isAndroid() -> Platform.ANDROID
            isIOS() -> Platform.IOS
            isDesktop() -> Platform.DESKTOP
            isWeb() -> Platform.WEB
            else -> throw IllegalStateException("Unknown platform")
        }
    }
    
    // Platform detection methods would be implemented using expect/actual
    private fun isAndroid(): Boolean = TODO("Platform-specific implementation")
    private fun isIOS(): Boolean = TODO("Platform-specific implementation")  
    private fun isDesktop(): Boolean = TODO("Platform-specific implementation")
    private fun isWeb(): Boolean = TODO("Platform-specific implementation")
}
```

### 5. Test Maintenance and Evolution

#### Test Health Monitoring

```kotlin
/**
 * Test health monitoring and maintenance utilities
 */
object TestHealthMonitor {
    
    data class TestMetrics(
        val executionTime: Duration,
        val memoryUsage: Long,
        val flakiness: Double, // Percentage of failures in recent runs
        val lastUpdated: Long,
        val coveragePercentage: Double
    )
    
    private val testMetrics = mutableMapOf<String, TestMetrics>()
    
    fun recordTestExecution(
        testName: String,
        executionTime: Duration,
        memoryUsage: Long,
        failed: Boolean
    ) {
        val existing = testMetrics[testName]
        val newFlakiness = if (existing != null) {
            // Simple exponential moving average
            existing.flakiness * 0.9 + (if (failed) 1.0 else 0.0) * 0.1
        } else {
            if (failed) 1.0 else 0.0
        }
        
        testMetrics[testName] = TestMetrics(
            executionTime = executionTime,
            memoryUsage = memoryUsage,
            flakiness = newFlakiness,
            lastUpdated = System.currentTimeMillis(),
            coveragePercentage = existing?.coveragePercentage ?: 0.0
        )
    }
    
    fun getTestHealthReport(): TestHealthReport {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000)
        
        val staleLTests = testMetrics.filter { (_, metrics) ->
            metrics.lastUpdated < oneWeekAgo
        }
        
        val flakyTests = testMetrics.filter { (_, metrics) ->
            metrics.flakiness > 0.1 // More than 10% failure rate
        }
        
        val slowTests = testMetrics.filter { (_, metrics) ->
            metrics.executionTime > 5.seconds
        }
        
        return TestHealthReport(
            totalTests = testMetrics.size,
            staleTests = staleLTests.keys.toList(),
            flakyTests = flakyTests.keys.toList(),
            slowTests = slowTests.keys.toList(),
            averageCoverage = testMetrics.values.map { it.coveragePercentage }.average()
        )
    }
}

data class TestHealthReport(
    val totalTests: Int,
    val staleTests: List<String>,
    val flakyTests: List<String>,
    val slowTests: List<String>,
    val averageCoverage: Double
)
```

## Conclusion

This comprehensive testing strategy document provides a robust framework for testing tag management systems in Kotlin Multiplatform applications. The strategies outlined ensure:

1. **Comprehensive Coverage**: From unit tests to integration tests to UI tests
2. **Platform Consistency**: Consistent behavior across Android, iOS, desktop, and web
3. **Performance Assurance**: Load testing, stress testing, and memory management
4. **Maintainability**: Well-organized test structure with reusable components
5. **Scalability**: Infrastructure that supports growing test suites

Key implementation priorities:

### Immediate Actions
1. Implement the enhanced `TestDataFactory` and `MockRepositories`
2. Set up the cross-platform test infrastructure with SQLDelight in-memory testing
3. Create comprehensive unit tests for the tag recommendation algorithm
4. Establish performance benchmarks and automated performance testing

### Medium-term Goals
1. Implement comprehensive UI testing across all platforms
2. Set up continuous integration with automated test execution
3. Create stress testing scenarios for high-volume operations
4. Implement test health monitoring and maintenance tools

### Long-term Vision
1. Advanced AI-powered test generation for edge cases
2. Real-world performance monitoring integration
3. Automated test evolution as the system grows
4. Cross-platform test result analytics and insights

The testing infrastructure outlined in this document provides a solid foundation for maintaining high-quality, reliable tag management functionality across all supported platforms while ensuring optimal performance and user experience.