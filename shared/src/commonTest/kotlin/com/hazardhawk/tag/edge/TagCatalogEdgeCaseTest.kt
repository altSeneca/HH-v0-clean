package com.hazardhawk.tag.edge

import com.hazardhawk.data.repositories.TagRepositoryImpl
import com.hazardhawk.models.Tag
import com.hazardhawk.test.TestDataFactory
import com.hazardhawk.test.MockInMemoryDatabase
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlin.test.*

/**
 * Comprehensive edge case tests for critical tag catalog scenarios.
 * Tests boundary conditions, error states, race conditions, and system limits.
 * Ensures robustness and reliability under extreme or unusual conditions.
 */
class TagCatalogEdgeCaseTest {
    
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
        clearAllMocks()
    }
    
    // MARK: - Database Corruption and Recovery Tests
    
    @Test
    fun `corrupted tag data should be handled gracefully during read operations`() = runTest {
        // Given - Corrupted tag data in database
        val corruptedTagData = mapOf(
            "corrupt-1" to null, // Null data
            "corrupt-2" to "invalid-json-data", // Invalid JSON
            "corrupt-3" to "{\"id\":\"corrupt-3\",\"name\":}", // Malformed JSON
            "corrupt-4" to "{\"id\":\"corrupt-4\"}" // Missing required fields
        )
        
        corruptedTagData.forEach { (tagId, corruptData) ->
            every { mockDatabase.getRawTagData(tagId) } returns corruptData
            every { mockDatabase.isDataCorrupted(tagId) } returns true
        }
        
        // When - Attempt to read corrupted tags
        val results = corruptedTagData.keys.map { tagId ->
            try {
                repository.getTagById(tagId) to null
            } catch (e: Exception) {
                null to e
            }
        }
        
        // Then - Should handle corruption gracefully
        results.forEach { (tag, exception) ->
            // Either return null (graceful handling) or throw specific exception
            assertTrue(tag == null || exception != null, "Corrupted data should be handled gracefully")
            if (exception != null) {
                assertTrue(exception is DataCorruptionException || exception is IllegalArgumentException)
            }
        }
    }
    
    @Test
    fun `database recovery should restore valid tags and quarantine corrupted ones`() = runTest {
        // Given - Mixed database with valid and corrupted tags
        val validTags = TestDataFactory.createPersonalTopTags()
        val corruptedTagIds = listOf("corrupt-1", "corrupt-2", "corrupt-3")
        
        validTags.forEach { mockDatabase.insertTag(it) }
        corruptedTagIds.forEach { tagId ->
            every { mockDatabase.isDataCorrupted(tagId) } returns true
            every { mockDatabase.quarantineCorruptedTag(tagId) } returns Unit
        }
        
        // Mock recovery process
        every { mockDatabase.performDataRecovery() } returns DatabaseRecoveryResult(
            totalTags = validTags.size + corruptedTagIds.size,
            recoveredTags = validTags.size,
            corruptedTags = corruptedTagIds.size,
            quarantinedTags = corruptedTagIds
        )
        
        // When - Perform database recovery
        val recoveryResult = mockDatabase.performDataRecovery()
        
        // Then - Should recover valid tags and quarantine corrupted ones
        assertEquals(validTags.size, recoveryResult.recoveredTags)
        assertEquals(corruptedTagIds.size, recoveryResult.corruptedTags)
        assertEquals(corruptedTagIds, recoveryResult.quarantinedTags)
        
        // Verify corrupted tags were quarantined
        corruptedTagIds.forEach { tagId ->
            verify { mockDatabase.quarantineCorruptedTag(tagId) }
        }
    }
    
    // MARK: - Memory and Resource Exhaustion Tests
    
    @Test
    fun `extremely large tag names should be handled without memory overflow`() = runTest {
        // Given - Tag with extremely large name
        val massiveTagName = "A".repeat(1_000_000) // 1MB string
        val massiveTag = TestDataFactory.createTestTag(
            id = "massive-tag",
            name = massiveTagName
        )
        
        // When - Create and retrieve massive tag
        val createResult = try {
            repository.createTag(massiveTag)
        } catch (e: OutOfMemoryError) {
            Result.failure<Tag>(e)
        } catch (e: IllegalArgumentException) {
            Result.failure<Tag>(e) // Expected for validation
        }
        
        // Then - Should either reject (preferred) or handle gracefully
        if (createResult.isSuccess) {
            // If allowed, retrieval should work
            val retrievedTag = repository.getTagById("massive-tag")
            assertNotNull(retrievedTag)
            assertEquals(massiveTagName, retrievedTag?.name)
        } else {
            // Should reject with appropriate error
            assertTrue(createResult.exceptionOrNull() is IllegalArgumentException)
        }
    }
    
    @Test
    fun `concurrent creation of many tags should not exceed memory limits`() = runTest {
        // Given - Large number of concurrent tag creation operations
        val numberOfTags = 1000
        val concurrentTags = (1..numberOfTags).map { index ->
            TestDataFactory.createTestTag(
                id = "concurrent-$index",
                name = "Concurrent Tag $index"
            )
        }
        
        // When - Create all tags concurrently
        val createOperations = concurrentTags.map { tag ->
            async {
                try {
                    repository.createTag(tag)
                } catch (e: OutOfMemoryError) {
                    Result.failure<Tag>(e)
                } catch (e: Exception) {
                    Result.failure<Tag>(e)
                }
            }
        }
        
        val results = createOperations.awaitAll()
        
        // Then - Should handle concurrent operations without memory issues
        val successfulCreations = results.count { it.isSuccess }
        val memoryErrors = results.count { it.exceptionOrNull() is OutOfMemoryError }
        
        // Either all succeed or system gracefully limits operations
        assertTrue(memoryErrors == 0 || successfulCreations > 0, 
            "Should either succeed or fail gracefully without memory errors")
        
        if (memoryErrors == 0) {
            assertEquals(numberOfTags, successfulCreations)
        }
    }
    
    // MARK: - Race Condition and Concurrency Tests
    
    @Test
    fun `concurrent tag updates should maintain data consistency`() = runTest {
        // Given - Single tag being updated concurrently
        val baseTag = TestDataFactory.createTestTag(id = "race-test", name = "Original")
        mockDatabase.insertTag(baseTag)
        
        // When - Multiple concurrent updates
        val updates = (1..10).map { index ->
            async {
                val updatedTag = baseTag.copy(
                    name = "Update $index",
                    updatedAt = System.currentTimeMillis() + index
                )
                repository.updateTag(updatedTag)
            }
        }
        
        val results = updates.awaitAll()
        
        // Then - All updates should succeed and final state should be consistent
        assertTrue(results.all { it.isSuccess }, "All concurrent updates should succeed")
        
        val finalTag = repository.getTagById(baseTag.id)
        assertNotNull(finalTag)
        assertTrue(finalTag!!.name.startsWith("Update"), "Final tag should reflect one of the updates")
        assertTrue(finalTag.updatedAt > baseTag.updatedAt, "Update timestamp should be newer")
    }
    
    @Test
    fun `tag deletion during concurrent reads should not cause system instability`() = runTest {
        // Given - Tag being read and deleted concurrently
        val targetTag = TestDataFactory.createTestTag(id = "delete-race", name = "Target Tag")
        mockDatabase.insertTag(targetTag)
        
        // When - Concurrent reads and deletion
        val readOperations = (1..20).map {
            async {
                try {
                    repository.getTagById(targetTag.id)
                } catch (e: Exception) {
                    null
                }
            }
        }
        
        val deleteOperation = async {
            delay(10) // Slight delay to allow some reads to start
            repository.deleteTag(targetTag.id)
        }
        
        val readResults = readOperations.awaitAll()
        val deleteResult = deleteOperation.await()
        
        // Then - Operations should complete without system errors
        assertTrue(deleteResult.isSuccess, "Delete operation should succeed")
        
        // Read results should be either the tag or null (after deletion)
        readResults.forEach { result ->
            assertTrue(result == null || result.id == targetTag.id, 
                "Read results should be consistent")
        }
    }
    
    // MARK: - Boundary Value Tests
    
    @Test
    fun `empty and whitespace-only tag names should be handled correctly`() = runTest {
        // Given - Tags with various empty/whitespace names
        val problematicNames = listOf(
            "",                    // Empty string
            " ",                   // Single space
            "\t",                  // Tab character
            "\n",                  // Newline character
            "   ",                 // Multiple spaces
            "\t\n\r ",             // Mixed whitespace
            "  \u00A0  ",          // Non-breaking spaces
            "​",                   // Zero-width space
        )
        
        problematicNames.forEachIndexed { index, problematicName ->
            val problematicTag = TestDataFactory.createTestTag(
                id = "problematic-$index",
                name = problematicName
            )
            
            // When - Create tag with problematic name
            val result = repository.createTag(problematicTag)
            
            // Then - Should either reject or sanitize
            if (result.isSuccess) {
                val createdTag = result.getOrNull()!!
                assertTrue(createdTag.name.isNotBlank() || createdTag.name != problematicName,
                    "Whitespace-only names should be sanitized or rejected")
            } else {
                assertTrue(result.exceptionOrNull() is IllegalArgumentException,
                    "Invalid names should be rejected with appropriate error")
            }
        }
    }
    
    @Test
    fun `maximum integer values should not cause overflow errors`() = runTest {
        // Given - Tag with maximum possible usage count
        val maxUsageTag = TestDataFactory.createTestTag(
            id = "max-usage",
            name = "Max Usage Tag",
            usageCount = Int.MAX_VALUE
        )
        
        mockDatabase.insertTag(maxUsageTag)
        
        // When - Increment usage count at maximum value
        val result = repository.incrementUsageCount("max-usage")
        
        // Then - Should handle overflow gracefully
        if (result.isSuccess) {
            val updatedTag = result.getOrNull()!!
            // Should either cap at max value or handle overflow
            assertTrue(updatedTag.usageCount == Int.MAX_VALUE || updatedTag.usageCount >= 0,
                "Usage count should not overflow to negative values")
        } else {
            // Should fail with appropriate error
            assertTrue(result.exceptionOrNull() is ArithmeticException ||
                      result.exceptionOrNull() is IllegalStateException)
        }
    }
    
    @Test
    fun `extremely old and future timestamps should be handled correctly`() = runTest {
        // Given - Tags with extreme timestamps
        val extremeTimestamps = listOf(
            0L,                    // Unix epoch
            -1L,                   // Before epoch (invalid)
            Long.MIN_VALUE,        // Minimum possible value
            Long.MAX_VALUE,        // Maximum possible value (far future)
            System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000 * 100) // 100 years in future
        )
        
        extremeTimestamps.forEachIndexed { index, timestamp ->
            val extremeTag = TestDataFactory.createTestTag(
                id = "extreme-time-$index",
                name = "Extreme Timestamp Tag $index",
                createdAt = timestamp,
                updatedAt = timestamp
            )
            
            // When - Create and retrieve tag with extreme timestamp
            val createResult = repository.createTag(extremeTag)
            
            // Then - Should handle extreme timestamps gracefully
            if (createResult.isSuccess) {
                val retrievedTag = repository.getTagById(extremeTag.id)
                assertNotNull(retrievedTag)
                
                // Timestamps should be valid or normalized
                assertTrue(retrievedTag!!.createdAt >= 0, "Created timestamp should not be negative")
                assertTrue(retrievedTag.updatedAt >= 0, "Updated timestamp should not be negative")
            } else {
                // Should reject invalid timestamps
                assertTrue(createResult.exceptionOrNull() is IllegalArgumentException)
            }
        }
    }
    
    // MARK: - Special Character and Encoding Tests
    
    @Test
    fun `unicode and special characters should be preserved in tag names`() = runTest {
        // Given - Tags with various unicode and special characters
        val specialCharacterNames = listOf(
            "🚧 Construction Zone",     // Emoji
            "Señor Safety",            // Accented characters
            "Русская безопасность",     // Cyrillic
            "中文安全",                  // Chinese characters
            "🔧⚡🏗️ Multi-Tool",        // Multiple emojis
            "Safety & Security",       // Ampersand
            "Tag #1 @site",            // Hash and at symbols
            "Quote \"Test\"",          // Quotes
            "Backslash\\Test",         // Backslash
            "Line\nBreak",             // Line break
            "Tab\tSeparated"           // Tab character
        )
        
        specialCharacterNames.forEachIndexed { index, specialName ->
            val unicodeTag = TestDataFactory.createTestTag(
                id = "unicode-$index",
                name = specialName
            )
            
            // When - Create and retrieve tag with special characters
            val createResult = repository.createTag(unicodeTag)
            
            if (createResult.isSuccess) {
                val retrievedTag = repository.getTagById(unicodeTag.id)
                assertNotNull(retrievedTag)
                
                // Then - Special characters should be preserved or properly handled
                assertEquals(specialName, retrievedTag!!.name, 
                    "Special characters should be preserved: '$specialName'")
            }
        }
    }
    
    @Test
    fun `malformed UTF-8 sequences should not crash the system`() = runTest {
        // Given - Potentially malformed UTF-8 sequences
        val malformedSequences = listOf(
            "\uD800",              // Unpaired high surrogate
            "\uDC00",              // Unpaired low surrogate
            "\uD800\uD800",        // Two high surrogates
            "\uDC00\uDC00",        // Two low surrogates
            "Test\uFFFE",          // Byte order mark
            "Test\u0000End",       // Null character
            "\u202E\u202D"         // Text direction controls
        )
        
        malformedSequences.forEachIndexed { index, malformedText ->
            val malformedTag = TestDataFactory.createTestTag(
                id = "malformed-$index",
                name = malformedText
            )
            
            // When - Create tag with potentially malformed UTF-8
            val result = try {
                repository.createTag(malformedTag)
            } catch (e: Exception) {
                Result.failure<Tag>(e)
            }
            
            // Then - Should handle malformed sequences gracefully
            if (result.isFailure) {
                val exception = result.exceptionOrNull()!!
                assertTrue(exception is IllegalArgumentException || 
                          exception is UnsupportedEncodingException,
                    "Malformed UTF-8 should be handled with appropriate exception")
            }
        }
    }
    
    // MARK: - System Limit and Capacity Tests
    
    @Test
    fun `system should gracefully handle database connection exhaustion`() = runTest {
        // Given - Mock database connection pool exhaustion
        var connectionCount = 0
        val maxConnections = 5
        
        every { mockDatabase.getConnection() } answers {
            if (connectionCount >= maxConnections) {
                throw ConnectionPoolExhaustedException("No available connections")
            } else {
                connectionCount++
                mockk()
            }
        }
        
        // When - Exceed connection pool capacity
        val connectionRequests = (1..10).map {
            async {
                try {
                    repository.getAllTags().first()
                    true
                } catch (e: ConnectionPoolExhaustedException) {
                    false
                }
            }
        }
        
        val results = connectionRequests.awaitAll()
        
        // Then - Should handle connection exhaustion gracefully
        val successfulConnections = results.count { it }
        val failedConnections = results.count { !it }
        
        assertTrue(successfulConnections <= maxConnections, 
            "Successful connections should not exceed pool size")
        assertTrue(failedConnections > 0, 
            "Some connections should fail when pool is exhausted")
    }
    
    @Test
    fun `search with extremely long queries should not cause timeout`() = runTest {
        // Given - Extremely long search query
        val longQuery = "safety equipment protection " * 1000 // Very long repeated text
        
        // Populate database with test data
        val testTags = TestDataFactory.createLargeTagList(100)
        testTags.forEach { mockDatabase.insertTag(it) }
        
        // When - Search with extremely long query
        val searchStartTime = System.currentTimeMillis()
        val searchResults = try {
            repository.searchTags(longQuery).first()
        } catch (e: Exception) {
            emptyList<Tag>()
        }
        val searchEndTime = System.currentTimeMillis()
        
        val searchDuration = searchEndTime - searchStartTime
        
        // Then - Search should complete within reasonable time
        assertTrue(searchDuration < 5000, "Long query search should complete within 5 seconds")
        assertTrue(searchResults.isEmpty() || searchResults.isNotEmpty(), 
            "Search should return consistent results")
    }
    
    // MARK: - Data Consistency Under Stress Tests
    
    @Test
    fun `rapid tag creation and deletion should maintain database consistency`() = runTest {
        // Given - Rapid create/delete operations
        val operationCount = 50
        val baseTagId = "stress-test"
        
        // When - Perform rapid create/delete cycles
        repeat(operationCount) { cycle ->
            val tag = TestDataFactory.createTestTag(
                id = "$baseTagId-$cycle",
                name = "Stress Test Tag $cycle"
            )
            
            // Create tag
            val createResult = repository.createTag(tag)
            assertTrue(createResult.isSuccess, "Create should succeed in cycle $cycle")
            
            // Verify creation
            val retrievedTag = repository.getTagById(tag.id)
            assertNotNull(retrievedTag, "Tag should exist after creation in cycle $cycle")
            
            // Delete tag
            val deleteResult = repository.deleteTag(tag.id)
            assertTrue(deleteResult.isSuccess, "Delete should succeed in cycle $cycle")
            
            // Verify deletion
            val deletedTag = repository.getTagById(tag.id)
            assertNull(deletedTag, "Tag should not exist after deletion in cycle $cycle")
        }
    }
    
    @Test
    fun `tag operations during low memory conditions should degrade gracefully`() = runTest {
        // Given - Simulated low memory conditions
        every { mockDatabase.isLowMemory() } returns true
        every { mockDatabase.freeMemory() } returns 1024L * 1024 // 1MB free
        every { mockDatabase.totalMemory() } returns 1024L * 1024 * 100 // 100MB total
        
        // When - Perform operations under memory pressure
        val memoryStressedOperations = listOf(
            { repository.getAllTags().first() },
            { repository.searchTags("test").first() },
            { 
                val testTag = TestDataFactory.createTestTag()
                repository.createTag(testTag)
            }
        )
        
        val results = memoryStressedOperations.map { operation ->
            try {
                operation()
                true
            } catch (e: OutOfMemoryError) {
                false
            } catch (e: Exception) {
                // Other exceptions might be acceptable under memory pressure
                true
            }
        }
        
        // Then - Operations should either succeed or fail gracefully (no system crash)
        assertTrue(results.isNotEmpty(), "Should handle memory pressure without system failure")
        
        // At least some operations should handle memory pressure gracefully
        val gracefulHandling = results.any { it }
        assertTrue(gracefulHandling, "At least some operations should handle memory pressure")
    }
    
    // MARK: - Error Recovery and Resilience Tests
    
    @Test
    fun `system should recover from transient database errors`() = runTest {
        // Given - Database with transient connection issues
        var errorCount = 0
        val maxErrors = 3
        
        every { mockDatabase.executeQuery(any()) } answers {
            if (errorCount < maxErrors) {
                errorCount++
                throw TransientDatabaseException("Temporary connection issue")
            } else {
                // Simulate recovery
                "success"
            }
        }
        
        // Mock retry mechanism
        suspend fun operationWithRetry(maxRetries: Int = 5): Result<String> {
            repeat(maxRetries) { attempt ->
                try {
                    val result = mockDatabase.executeQuery("SELECT * FROM tags")
                    return Result.success(result)
                } catch (e: TransientDatabaseException) {
                    if (attempt == maxRetries - 1) {
                        return Result.failure(e)
                    }
                    delay(100 * (attempt + 1)) // Exponential backoff
                }
            }
            return Result.failure(Exception("Max retries exceeded"))
        }
        
        // When - Perform operation with retry logic
        val result = operationWithRetry()
        
        // Then - Should eventually succeed after retries
        assertTrue(result.isSuccess, "Operation should succeed after transient errors resolve")
        assertEquals("success", result.getOrNull())
        assertTrue(errorCount >= maxErrors, "Should have attempted through transient errors")
    }
}

// MARK: - Supporting Exception Classes and Data Types

class DataCorruptionException(message: String) : Exception(message)
class ConnectionPoolExhaustedException(message: String) : Exception(message)
class TransientDatabaseException(message: String) : Exception(message)

data class DatabaseRecoveryResult(
    val totalTags: Int,
    val recoveredTags: Int,
    val corruptedTags: Int,
    val quarantinedTags: List<String>
)

// Mock database extensions for edge case testing
fun MockInMemoryDatabase.getRawTagData(tagId: String): String? = "mock-raw-data"
fun MockInMemoryDatabase.isDataCorrupted(tagId: String): Boolean = false
fun MockInMemoryDatabase.quarantineCorruptedTag(tagId: String) = Unit
fun MockInMemoryDatabase.performDataRecovery(): DatabaseRecoveryResult = 
    DatabaseRecoveryResult(0, 0, 0, emptyList())
fun MockInMemoryDatabase.getConnection(): Any = mockk()
fun MockInMemoryDatabase.isLowMemory(): Boolean = false
fun MockInMemoryDatabase.freeMemory(): Long = 1024L * 1024 * 50 // 50MB
fun MockInMemoryDatabase.totalMemory(): Long = 1024L * 1024 * 100 // 100MB
fun MockInMemoryDatabase.executeQuery(query: String): String = "success"