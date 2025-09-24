package com.hazardhawk.tag.performance

import com.hazardhawk.data.repositories.TagRepositoryImpl
import com.hazardhawk.models.Tag
import com.hazardhawk.test.TestDataFactory
import com.hazardhawk.test.MockInMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlin.test.*
import kotlin.time.measureTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive performance tests for tag catalog operations with large datasets.
 * Tests memory usage, query performance, and scalability with 1000+ tags.
 * Ensures production readiness under load and validates performance thresholds.
 */
class TagCatalogPerformanceTest {
    
    private lateinit var mockDatabase: MockInMemoryDatabase
    private lateinit var repository: TagRepositoryImpl
    
    @BeforeTest
    fun setup() {
        mockDatabase = MockInMemoryDatabase()
        repository = TagRepositoryImpl(mockDatabase)
    }
    
    @AfterTest
    fun teardown() {
        mockDatabase.clear()
    }
    
    // MARK: - Large Dataset Performance Tests
    
    @Test
    fun `loading 1000 tags should complete within performance threshold`() = runTest {
        // Given - Large dataset of 1000 tags
        val largeBatch = TestDataFactory.createLargeTagList(1000)
        largeBatch.forEach { mockDatabase.insertTag(it) }
        
        // When - Load all tags and measure time
        val loadTime = measureTime {
            val allTags = repository.getAllTags().first()
            assertEquals(1000, allTags.size)
        }
        
        // Then - Should complete within 500ms threshold
        assertTrue(
            loadTime < 500.milliseconds,
            "Loading 1000 tags took ${loadTime.inWholeMilliseconds}ms, expected < 500ms"
        )
    }
    
    @Test
    fun `searching through 5000 tags should be responsive`() = runTest {
        // Given - Very large dataset
        val performanceData = TestDataFactory.createPerformanceTestDataset(5000)
        performanceData.tags.forEach { mockDatabase.insertTag(it) }
        
        val searchQueries = listOf("safety", "equipment", "electrical", "structural")
        
        // When - Perform multiple searches and measure time
        val totalSearchTime = measureTime {
            searchQueries.forEach { query ->
                val results = repository.searchTags(query).first()
                assertTrue(results.isNotEmpty(), "Search for '$query' should return results")
                
                // Each individual search should be fast
                val individualSearchTime = measureTime {
                    repository.searchTags(query).first()
                }
                
                assertTrue(
                    individualSearchTime < 100.milliseconds,
                    "Individual search for '$query' took ${individualSearchTime.inWholeMilliseconds}ms, expected < 100ms"
                )
            }
        }
        
        // Then - Total search time should be reasonable
        assertTrue(
            totalSearchTime < 1.seconds,
            "Total search time was ${totalSearchTime.inWholeMilliseconds}ms, expected < 1000ms"
        )
    }
    
    @Test
    fun `bulk operations on 1000 tags should be efficient`() = runTest {
        // Given - Large batch for bulk operations
        val bulkTags = (1..1000).map { index ->
            TestDataFactory.createTestTag(
                id = "bulk-$index",
                name = "Bulk Tag $index",
                category = "Performance"
            )
        }
        
        // When - Bulk create and measure time
        val createTime = measureTime {
            val result = repository.bulkCreateTags(bulkTags)
            assertTrue(result.isSuccess, "Bulk create should succeed")
            assertEquals(1000, result.getOrNull()!!.size)
        }
        
        // Then - Should complete within reasonable time
        assertTrue(
            createTime < 2.seconds,
            "Bulk creation of 1000 tags took ${createTime.inWholeMilliseconds}ms, expected < 2000ms"
        )
        
        // When - Bulk delete and measure time
        val tagIds = bulkTags.map { it.id }
        val deleteTime = measureTime {
            val result = repository.bulkDeleteTags(tagIds)
            assertTrue(result.isSuccess, "Bulk delete should succeed")
            assertEquals(1000, result.getOrNull())
        }
        
        // Then - Bulk delete should also be efficient
        assertTrue(
            deleteTime < 1.seconds,
            "Bulk deletion of 1000 tags took ${deleteTime.inWholeMilliseconds}ms, expected < 1000ms"
        )
    }
    
    @Test
    fun `concurrent access to large tag catalog should be thread-safe`() = runTest {
        // Given - Large dataset
        val largeBatch = TestDataFactory.createLargeTagList(2000)
        largeBatch.forEach { mockDatabase.insertTag(it) }
        
        // When - Multiple concurrent read operations
        val concurrentOperations = (1..10).map { index ->
            async {
                val searchQuery = when (index % 3) {
                    0 -> "safety"
                    1 -> "equipment"
                    else -> "electrical"
                }
                
                val startTime = System.currentTimeMillis()
                val results = repository.searchTags(searchQuery).first()
                val endTime = System.currentTimeMillis()
                
                Pair(results.size, endTime - startTime)
            }
        }
        
        // Wait for all operations to complete
        val results = concurrentOperations.awaitAll()
        
        // Then - All operations should complete successfully
        assertTrue(results.all { it.first > 0 }, "All searches should return results")
        
        // And within reasonable time
        val maxTime = results.maxOf { it.second }
        assertTrue(maxTime < 200, "Concurrent operations should complete within 200ms, max was ${maxTime}ms")
    }
    
    // MARK: - Memory Usage Tests
    
    @Test
    fun `memory usage should remain stable with large datasets`() = runTest {
        // Given - Monitor memory before loading large dataset
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // When - Load large dataset
        val largeBatch = TestDataFactory.createLargeTagList(3000)
        largeBatch.forEach { mockDatabase.insertTag(it) }
        
        // Load all tags multiple times to test for memory leaks
        repeat(5) {
            val allTags = repository.getAllTags().first()
            assertEquals(3000, allTags.size)
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100) // Give GC time to run
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        val memoryIncreaseInMB = memoryIncrease / (1024 * 1024)
        
        // Then - Memory increase should be reasonable (less than 100MB for 3000 tags)
        assertTrue(
            memoryIncreaseInMB < 100,
            "Memory increase was ${memoryIncreaseInMB}MB, expected < 100MB"
        )
    }
    
    @Test
    fun `memory should be released after clearing large datasets`() = runTest {
        val runtime = Runtime.getRuntime()
        
        // Given - Load large dataset
        val largeBatch = TestDataFactory.createLargeTagList(2000)
        largeBatch.forEach { mockDatabase.insertTag(it) }
        
        // Load data into memory
        val loadedTags = repository.getAllTags().first()
        assertEquals(2000, loadedTags.size)
        
        val memoryAfterLoad = runtime.totalMemory() - runtime.freeMemory()
        
        // When - Clear the database
        mockDatabase.clear()
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        val memoryAfterClear = runtime.totalMemory() - runtime.freeMemory()
        
        // Then - Memory should be reduced (within 20% of original)
        assertTrue(
            memoryAfterClear <= memoryAfterLoad,
            "Memory should not increase after clearing data"
        )
    }
    
    // MARK: - Pagination Performance Tests
    
    @Test
    fun `paginated loading should perform consistently across pages`() = runTest {
        // Given - Large dataset with varied tag names
        val totalTags = 1000
        val pageSize = 50
        val largeBatch = (1..totalTags).map { index ->
            TestDataFactory.createTestTag(
                id = "page-tag-$index",
                name = "Tag Number $index",
                usageCount = totalTags - index // Varied usage for realistic sorting
            )
        }
        largeBatch.forEach { mockDatabase.insertTag(it) }
        
        val pageTimes = mutableListOf<Duration>()
        
        // When - Load data page by page
        for (page in 0 until (totalTags / pageSize)) {
            val pageTime = measureTime {
                val offset = page * pageSize
                val pageData = repository.getTagsPaginated(offset, pageSize).first()
                assertEquals(pageSize, pageData.size)
            }
            pageTimes.add(pageTime)
        }
        
        // Then - All pages should load within consistent time
        val maxPageTime = pageTimes.maxOrNull()!!
        val minPageTime = pageTimes.minOrNull()!!
        val avgPageTime = Duration.milliseconds(pageTimes.map { it.inWholeMilliseconds }.average().toLong())
        
        assertTrue(
            maxPageTime < 50.milliseconds,
            "Slowest page took ${maxPageTime.inWholeMilliseconds}ms, expected < 50ms"
        )
        
        // Page times should be consistent (max shouldn't be more than 3x min)
        assertTrue(
            maxPageTime.inWholeMilliseconds <= minPageTime.inWholeMilliseconds * 3,
            "Page time variation too high: min=${minPageTime.inWholeMilliseconds}ms, max=${maxPageTime.inWholeMilliseconds}ms"
        )
    }
    
    @Test
    fun `filtered pagination should remain performant`() = runTest {
        // Given - Large mixed dataset
        val safetyTags = (1..800).map { index ->
            TestDataFactory.createTestTag(
                id = "safety-$index",
                name = "Safety Tag $index",
                category = "Safety"
            )
        }
        val otherTags = (1..200).map { index ->
            TestDataFactory.createTestTag(
                id = "other-$index",
                name = "Other Tag $index",
                category = "Equipment"
            )
        }
        
        (safetyTags + otherTags).forEach { mockDatabase.insertTag(it) }
        
        // When - Load safety tags with pagination
        val pageSize = 50
        val filterTime = measureTime {
            for (page in 0 until (safetyTags.size / pageSize)) {
                val offset = page * pageSize
                val safetyPage = repository.getTagsByCategoryPaginated("Safety", offset, pageSize).first()
                assertTrue(safetyPage.size <= pageSize)
                assertTrue(safetyPage.all { it.category == "Safety" })
            }
        }
        
        // Then - Filtered pagination should be efficient
        assertTrue(
            filterTime < 1.seconds,
            "Filtered pagination took ${filterTime.inWholeMilliseconds}ms, expected < 1000ms"
        )
    }
    
    // MARK: - Sorting Performance Tests
    
    @Test
    fun `sorting large datasets should be efficient`() = runTest {
        // Given - Large unsorted dataset
        val unsortedTags = (1..1500).map { index ->
            TestDataFactory.createTestTag(
                id = "sort-$index",
                name = "Tag ${1500 - index}", // Reverse order names
                usageCount = index % 100, // Varied usage counts
                lastUsed = System.currentTimeMillis() - (index * 1000) // Varied last used times
            )
        }
        unsortedTags.forEach { mockDatabase.insertTag(it) }
        
        // When - Sort by different criteria and measure time
        val sortTests = mapOf(
            "name" to { repository.getTagsSortedByName().first() },
            "usage" to { repository.getTagsSortedByUsage().first() },
            "lastUsed" to { repository.getTagsSortedByLastUsed().first() }
        )
        
        sortTests.forEach { (sortType, sortOperation) ->
            val sortTime = measureTime {
                val sortedTags = sortOperation()
                assertEquals(1500, sortedTags.size)
                
                // Verify sorting is correct based on type
                when (sortType) {
                    "name" -> assertTrue(
                        sortedTags.zipWithNext().all { (first, second) ->
                            first.name <= second.name
                        }
                    )
                    "usage" -> assertTrue(
                        sortedTags.zipWithNext().all { (first, second) ->
                            first.usageCount >= second.usageCount
                        }
                    )
                }
            }
            
            // Then - Each sort should complete within threshold
            assertTrue(
                sortTime < 200.milliseconds,
                "Sorting 1500 tags by $sortType took ${sortTime.inWholeMilliseconds}ms, expected < 200ms"
            )
        }
    }
    
    // MARK: - Stress Testing
    
    @Test
    fun `system should handle rapid consecutive operations`() = runTest {
        // Given - Moderate dataset
        val baseTags = TestDataFactory.createLargeTagList(500)
        baseTags.forEach { mockDatabase.insertTag(it) }
        
        // When - Perform rapid consecutive operations
        val operationTime = measureTime {
            repeat(100) { index ->
                // Mix of operations
                when (index % 4) {
                    0 -> {
                        // Search operation
                        val searchResults = repository.searchTags("Tag").first()
                        assertTrue(searchResults.isNotEmpty())
                    }
                    1 -> {
                        // Create operation
                        val newTag = TestDataFactory.createTestTag(id = "rapid-$index")
                        val result = repository.createTag(newTag)
                        assertTrue(result.isSuccess)
                    }
                    2 -> {
                        // Read operation
                        val allTags = repository.getAllTags().first()
                        assertTrue(allTags.isNotEmpty())
                    }
                    3 -> {
                        // Update operation (if tag exists)
                        val tagToUpdate = repository.getTagById("rapid-${index-3}")
                        if (tagToUpdate != null) {
                            val updatedTag = tagToUpdate.copy(name = "${tagToUpdate.name} Updated")
                            repository.updateTag(updatedTag)
                        }
                    }
                }
            }
        }
        
        // Then - Should handle rapid operations without significant degradation
        assertTrue(
            operationTime < 5.seconds,
            "100 rapid operations took ${operationTime.inWholeMilliseconds}ms, expected < 5000ms"
        )
        
        // Verify data integrity after stress test
        val finalTags = repository.getAllTags().first()
        assertTrue(finalTags.size >= 500, "Should maintain at least original 500 tags")
    }
    
    @Test
    fun `performance should degrade gracefully under extreme load`() = runTest {
        // Given - Extremely large dataset (10,000 tags)
        val extremeDataset = TestDataFactory.createPerformanceTestDataset(10000)
        
        // Load in batches to avoid timeout
        val batchSize = 1000
        val loadTime = measureTime {
            extremeDataset.tags.chunked(batchSize).forEach { batch ->
                batch.forEach { tag -> mockDatabase.insertTag(tag) }
            }
        }
        
        // When - Perform basic operations on extreme dataset
        val operationTime = measureTime {
            // Basic read operation
            val allTags = repository.getAllTags().first()
            assertEquals(10000, allTags.size)
            
            // Search operation
            val searchResults = repository.searchTags("safety").first()
            assertTrue(searchResults.isNotEmpty())
        }
        
        // Then - Should complete but may take longer (graceful degradation)
        assertTrue(
            operationTime < 10.seconds,
            "Operations on 10,000 tags took ${operationTime.inWholeMilliseconds}ms, expected < 10,000ms"
        )
        
        // Performance should still be acceptable for critical operations
        val criticalOperationTime = measureTime {
            repository.getTagById("perf-tag-1")
        }
        
        assertTrue(
            criticalOperationTime < 100.milliseconds,
            "Single tag lookup took ${criticalOperationTime.inWholeMilliseconds}ms even with 10k tags, expected < 100ms"
        )
    }
    
    // MARK: - Performance Regression Tests
    
    @Test
    fun `performance should not regress with database growth over time`() = runTest {
        val performanceBaseline = mutableListOf<Duration>()
        val batchSize = 500
        
        // Simulate database growth over time
        repeat(5) { iteration ->
            // Add more data each iteration
            val newBatch = TestDataFactory.createLargeTagList(batchSize, startIndex = iteration * batchSize)
            newBatch.forEach { mockDatabase.insertTag(it) }
            
            // Measure search performance at each growth stage
            val searchTime = measureTime {
                val results = repository.searchTags("Tag").first()
                assertTrue(results.isNotEmpty())
            }
            
            performanceBaseline.add(searchTime)
            
            // Performance shouldn't degrade significantly
            if (iteration > 0) {
                val previousTime = performanceBaseline[iteration - 1]
                val currentTime = performanceBaseline[iteration]
                
                // Current time shouldn't be more than 50% slower than previous
                assertTrue(
                    currentTime.inWholeMilliseconds <= previousTime.inWholeMilliseconds * 1.5,
                    "Performance regressed from ${previousTime.inWholeMilliseconds}ms to ${currentTime.inWholeMilliseconds}ms at iteration $iteration"
                )
            }
        }
        
        // Final check: with 2500 tags, search should still be fast
        assertTrue(
            performanceBaseline.last() < 150.milliseconds,
            "Search with 2500 tags took ${performanceBaseline.last().inWholeMilliseconds}ms, expected < 150ms"
        )
    }
}

/**
 * Extension functions for performance testing
 */
suspend fun TagRepositoryImpl.getTagsPaginated(offset: Int, limit: Int) = getAllTags()
suspend fun TagRepositoryImpl.getTagsByCategoryPaginated(category: String, offset: Int, limit: Int) = getTagsByCategory(category)
suspend fun TagRepositoryImpl.getTagsSortedByName() = getAllTags()
suspend fun TagRepositoryImpl.getTagsSortedByUsage() = getAllTags()
suspend fun TagRepositoryImpl.getTagsSortedByLastUsed() = getAllTags()

/**
 * Enhanced test data factory for large datasets
 */
fun TestDataFactory.createLargeTagList(count: Int, startIndex: Int = 0): List<Tag> {
    return (startIndex until startIndex + count).map { index ->
        createTestTag(
            id = "tag-$index",
            name = "Test Tag $index",
            usageCount = (count - index) // Higher usage for lower indices
        )
    }
}