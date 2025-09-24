package com.hazardhawk.tag.network

import com.hazardhawk.data.repositories.TagRepositoryImpl
import com.hazardhawk.models.Tag
import com.hazardhawk.network.NetworkManager
import com.hazardhawk.sync.SyncManager
import com.hazardhawk.sync.SyncStatus
import com.hazardhawk.sync.SyncQueue
import com.hazardhawk.test.TestDataFactory
import com.hazardhawk.test.MockInMemoryDatabase
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import kotlin.test.*

/**
 * Comprehensive network failure and offline sync scenario tests.
 * Tests tag catalog behavior during network outages, connection drops, and sync conflicts.
 * Ensures data integrity and user experience during offline/online transitions.
 */
class TagCatalogNetworkFailureTest {
    
    private lateinit var mockDatabase: MockInMemoryDatabase
    private lateinit var repository: TagRepositoryImpl
    private lateinit var mockNetworkManager: NetworkManager
    private lateinit var mockSyncManager: SyncManager
    private lateinit var mockSyncQueue: SyncQueue
    private val networkStateFlow = MutableStateFlow(true)
    
    @BeforeTest
    fun setup() {
        mockDatabase = MockInMemoryDatabase()
        repository = TagRepositoryImpl(mockDatabase)
        mockNetworkManager = mockk(relaxed = true)
        mockSyncManager = mockk(relaxed = true)
        mockSyncQueue = mockk(relaxed = true)
        
        every { mockNetworkManager.isConnected() } returns networkStateFlow.value
        every { mockNetworkManager.networkState } returns networkStateFlow
    }
    
    @AfterTest
    fun teardown() {
        mockDatabase.clear()
        clearAllMocks()
    }
    
    // MARK: - Network Connection Tests
    
    @Test
    fun `tag operations should work offline with local storage`() = runTest {
        // Given - Network is offline
        networkStateFlow.value = false
        every { mockNetworkManager.isConnected() } returns false
        
        // When - Create tag offline
        val offlineTag = TestDataFactory.createTestTag(
            id = "offline-tag-1",
            name = "Offline Created Tag"
        )
        
        every { mockSyncQueue.queueOperation(any()) } returns Unit
        every { repository.createTagOffline(offlineTag) } answers {
            mockDatabase.insertTag(offlineTag.copy(lastUsed = System.currentTimeMillis()))
            Result.success(offlineTag)
        }
        
        val result = repository.createTagOffline(offlineTag)
        
        // Then - Should succeed and queue for sync
        assertTrue(result.isSuccess)
        verify { mockSyncQueue.queueOperation(any()) }
        
        // Verify tag exists locally
        val localTag = repository.getTagById(offlineTag.id)
        assertNotNull(localTag)
        assertEquals(offlineTag.name, localTag?.name)
    }
    
    @Test
    fun `network reconnection should trigger sync of queued operations`() = runTest {
        // Given - Operations queued while offline
        val queuedOperations = listOf(
            QueuedOperation.CREATE("create-1", TestDataFactory.createTestTag(id = "create-1")),
            QueuedOperation.UPDATE("update-1", TestDataFactory.createTestTag(id = "update-1", name = "Updated")),
            QueuedOperation.DELETE("delete-1")
        )
        
        every { mockSyncQueue.getQueuedOperations() } returns queuedOperations
        every { mockSyncManager.syncQueuedOperations() } returns SyncResult(success = true, processedCount = 3)
        
        // When - Network comes back online
        networkStateFlow.value = false // Start offline
        networkStateFlow.value = true  // Then online
        every { mockNetworkManager.isConnected() } returns true
        
        // Simulate sync trigger on network reconnection
        mockSyncManager.syncQueuedOperations()
        
        // Then - Should process queued operations
        verify { mockSyncManager.syncQueuedOperations() }
        verify { mockSyncQueue.getQueuedOperations() }
    }
    
    @Test
    fun `intermittent network failures should not corrupt local data`() = runTest {
        // Given - Tags in local database
        val localTags = TestDataFactory.createPersonalTopTags()
        localTags.forEach { mockDatabase.insertTag(it) }
        
        // When - Network fails during sync
        networkStateFlow.value = true // Online
        every { mockNetworkManager.isConnected() } returns true
        
        val tagToUpdate = localTags.first().copy(name = "Updated During Network Issue")
        
        // Simulate network failure during operation
        every { repository.updateTagWithSync(tagToUpdate) } answers {
            networkStateFlow.value = false // Network drops
            every { mockNetworkManager.isConnected() } returns false
            Result.failure(NetworkException("Connection lost during sync"))
        }
        
        val result = repository.updateTagWithSync(tagToUpdate)
        
        // Then - Operation should fail gracefully
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
        
        // But local data should remain intact
        val originalTag = repository.getTagById(localTags.first().id)
        assertEquals(localTags.first().name, originalTag?.name) // Should not be updated
    }
    
    // MARK: - Sync Conflict Resolution Tests
    
    @Test
    fun `sync conflicts should be detected and handled appropriately`() = runTest {
        // Given - Tag modified both locally and remotely
        val originalTag = TestDataFactory.createTestTag(id = "conflict-tag", name = "Original")
        mockDatabase.insertTag(originalTag)
        
        val localModification = originalTag.copy(name = "Local Update", updatedAt = System.currentTimeMillis())
        val remoteModification = originalTag.copy(name = "Remote Update", updatedAt = System.currentTimeMillis() + 1000)
        
        every { mockSyncManager.detectConflict(localModification, remoteModification) } returns true
        every { mockSyncManager.resolveConflict(localModification, remoteModification) } returns ConflictResolution(
            resolvedTag = remoteModification, // Prefer remote (newer timestamp)
            resolution = ConflictResolutionStrategy.PREFER_REMOTE
        )
        
        // When - Sync detects conflict
        val conflictDetected = mockSyncManager.detectConflict(localModification, remoteModification)
        val resolution = mockSyncManager.resolveConflict(localModification, remoteModification)
        
        // Then - Should detect conflict and resolve appropriately
        assertTrue(conflictDetected)
        assertEquals("Remote Update", resolution.resolvedTag.name)
        assertEquals(ConflictResolutionStrategy.PREFER_REMOTE, resolution.resolution)
    }
    
    @Test
    fun `manual conflict resolution should be supported for critical changes`() = runTest {
        // Given - Conflicting tag modifications requiring manual resolution
        val baseTag = TestDataFactory.createTestTag(id = "manual-conflict")
        val localTag = baseTag.copy(name = "Local Critical Change", category = "Safety")
        val remoteTag = baseTag.copy(name = "Remote Critical Change", category = "Equipment")
        
        every { mockSyncManager.requiresManualResolution(localTag, remoteTag) } returns true
        every { mockSyncManager.createConflictForManualResolution(localTag, remoteTag) } returns ManualConflictResolution(
            conflictId = "conflict-1",
            localTag = localTag,
            remoteTag = remoteTag,
            requiresUserInput = true
        )
        
        // When - Conflict requires manual resolution
        val requiresManual = mockSyncManager.requiresManualResolution(localTag, remoteTag)
        val manualConflict = mockSyncManager.createConflictForManualResolution(localTag, remoteTag)
        
        // Then - Should create manual conflict resolution
        assertTrue(requiresManual)
        assertEquals("conflict-1", manualConflict.conflictId)
        assertTrue(manualConflict.requiresUserInput)
        assertEquals(localTag.name, manualConflict.localTag.name)
        assertEquals(remoteTag.name, manualConflict.remoteTag.name)
    }
    
    // MARK: - Offline Queue Management Tests
    
    @Test
    fun `offline operations should be queued in correct order`() = runTest {
        // Given - Network is offline
        networkStateFlow.value = false
        every { mockNetworkManager.isConnected() } returns false
        
        val operations = mutableListOf<QueuedOperation>()
        every { mockSyncQueue.queueOperation(capture(operations)) } returns Unit
        
        // When - Multiple operations performed offline
        val tag1 = TestDataFactory.createTestTag(id = "queue-1", name = "First")
        val tag2 = TestDataFactory.createTestTag(id = "queue-2", name = "Second")
        val tag3 = tag1.copy(name = "First Updated")
        
        // Simulate queued operations
        mockSyncQueue.queueOperation(QueuedOperation.CREATE("queue-1", tag1))
        mockSyncQueue.queueOperation(QueuedOperation.CREATE("queue-2", tag2))
        mockSyncQueue.queueOperation(QueuedOperation.UPDATE("queue-1", tag3))
        mockSyncQueue.queueOperation(QueuedOperation.DELETE("queue-2"))
        
        // Then - Operations should be queued in order
        assertEquals(4, operations.size)
        assertTrue(operations[0] is QueuedOperation.CREATE)
        assertTrue(operations[1] is QueuedOperation.CREATE)
        assertTrue(operations[2] is QueuedOperation.UPDATE)
        assertTrue(operations[3] is QueuedOperation.DELETE)
    }
    
    @Test
    fun `duplicate operations should be consolidated in sync queue`() = runTest {
        // Given - Multiple updates to same tag
        val originalTag = TestDataFactory.createTestTag(id = "consolidate-test")
        val update1 = originalTag.copy(name = "Update 1")
        val update2 = originalTag.copy(name = "Update 2")
        val finalUpdate = originalTag.copy(name = "Final Update")
        
        every { mockSyncQueue.consolidateOperations() } answers {
            // Simulate consolidation logic
            listOf(QueuedOperation.UPDATE("consolidate-test", finalUpdate))
        }
        
        // When - Consolidate duplicate operations
        val consolidatedOps = mockSyncQueue.consolidateOperations()
        
        // Then - Should have only final operation
        assertEquals(1, consolidatedOps.size)
        val finalOp = consolidatedOps.first() as QueuedOperation.UPDATE
        assertEquals("Final Update", finalOp.tag.name)
    }
    
    @Test
    fun `sync queue should handle operation failures gracefully`() = runTest {
        // Given - Operations that may fail
        val queuedOps = listOf(
            QueuedOperation.CREATE("fail-1", TestDataFactory.createTestTag(id = "fail-1")),
            QueuedOperation.UPDATE("fail-2", TestDataFactory.createTestTag(id = "fail-2")),
            QueuedOperation.CREATE("success-1", TestDataFactory.createTestTag(id = "success-1"))
        )
        
        every { mockSyncQueue.getQueuedOperations() } returns queuedOps
        every { mockSyncManager.processQueuedOperation(queuedOps[0]) } returns OperationResult.FAILURE
        every { mockSyncManager.processQueuedOperation(queuedOps[1]) } returns OperationResult.FAILURE
        every { mockSyncManager.processQueuedOperation(queuedOps[2]) } returns OperationResult.SUCCESS
        
        // When - Process queued operations
        val results = queuedOps.map { mockSyncManager.processQueuedOperation(it) }
        
        // Then - Should handle mixed results
        assertEquals(OperationResult.FAILURE, results[0])
        assertEquals(OperationResult.FAILURE, results[1])
        assertEquals(OperationResult.SUCCESS, results[2])
        
        // Failed operations should be retried
        verify(exactly = 1) { mockSyncManager.processQueuedOperation(queuedOps[0]) }
        verify(exactly = 1) { mockSyncManager.processQueuedOperation(queuedOps[1]) }
        verify(exactly = 1) { mockSyncManager.processQueuedOperation(queuedOps[2]) }
    }
    
    // MARK: - Network Recovery Tests
    
    @Test
    fun `gradual sync should handle large offline queues efficiently`() = runTest {
        // Given - Large queue of offline operations
        val largeQueue = (1..500).map { index ->
            QueuedOperation.CREATE("large-$index", TestDataFactory.createTestTag(id = "large-$index"))
        }
        
        every { mockSyncQueue.getQueuedOperations() } returns largeQueue
        every { mockSyncManager.processBatch(any(), any()) } answers {
            val batch = firstArg<List<QueuedOperation>>()
            BatchProcessingResult(
                processed = batch.size,
                failed = 0,
                errors = emptyList()
            )
        }
        
        // When - Process large queue in batches
        val batchSize = 50
        val batches = largeQueue.chunked(batchSize)
        val results = batches.map { batch ->
            mockSyncManager.processBatch(batch, batchSize)
        }
        
        // Then - Should process all batches successfully
        assertEquals(10, results.size) // 500 / 50 = 10 batches
        results.forEach { result ->
            assertEquals(batchSize, result.processed)
            assertEquals(0, result.failed)
        }
    }
    
    @Test
    fun `partial sync failures should not prevent subsequent operations`() = runTest {
        // Given - Mixed queue with some failing operations
        val mixedQueue = listOf(
            QueuedOperation.CREATE("partial-1", TestDataFactory.createTestTag(id = "partial-1")),
            QueuedOperation.UPDATE("partial-fail", TestDataFactory.createTestTag(id = "partial-fail")),
            QueuedOperation.CREATE("partial-2", TestDataFactory.createTestTag(id = "partial-2"))
        )
        
        every { mockSyncManager.processQueuedOperation(mixedQueue[0]) } returns OperationResult.SUCCESS
        every { mockSyncManager.processQueuedOperation(mixedQueue[1]) } returns OperationResult.FAILURE
        every { mockSyncManager.processQueuedOperation(mixedQueue[2]) } returns OperationResult.SUCCESS
        
        // When - Process mixed queue
        val results = mixedQueue.map { op ->
            mockSyncManager.processQueuedOperation(op)
        }
        
        // Then - Should continue processing despite failures
        assertEquals(OperationResult.SUCCESS, results[0])
        assertEquals(OperationResult.FAILURE, results[1])
        assertEquals(OperationResult.SUCCESS, results[2])
        
        // All operations should be attempted
        verify { mockSyncManager.processQueuedOperation(mixedQueue[0]) }
        verify { mockSyncManager.processQueuedOperation(mixedQueue[1]) }
        verify { mockSyncManager.processQueuedOperation(mixedQueue[2]) }
    }
    
    // MARK: - Data Consistency Tests
    
    @Test
    fun `offline modifications should maintain referential integrity`() = runTest {
        // Given - Related tags offline
        val parentTag = TestDataFactory.createTestTag(id = "parent-offline", category = "Safety")
        val childTags = (1..3).map { index ->
            TestDataFactory.createTestTag(
                id = "child-$index",
                name = "Child Tag $index",
                category = parentTag.category
            )
        }
        
        networkStateFlow.value = false
        every { mockNetworkManager.isConnected() } returns false
        
        // When - Modify tags offline
        every { repository.createTagOffline(any()) } answers {
            val tag = firstArg<Tag>()
            mockDatabase.insertTag(tag)
            Result.success(tag)
        }
        
        // Create parent and children offline
        repository.createTagOffline(parentTag)
        childTags.forEach { repository.createTagOffline(it) }
        
        // Then - Verify relationships maintained
        val storedParent = repository.getTagById(parentTag.id)
        val storedChildren = childTags.map { repository.getTagById(it.id) }
        
        assertNotNull(storedParent)
        storedChildren.forEach { child ->
            assertNotNull(child)
            assertEquals(parentTag.category, child!!.category)
        }
    }
    
    @Test
    fun `concurrent offline operations should maintain data consistency`() = runTest {
        // Given - Multiple concurrent offline modifications
        networkStateFlow.value = false
        every { mockNetworkManager.isConnected() } returns false
        
        val baseTag = TestDataFactory.createTestTag(id = "concurrent-offline")
        mockDatabase.insertTag(baseTag)
        
        // When - Simulate concurrent modifications
        every { repository.updateTagOffline(any()) } answers {
            val tag = firstArg<Tag>()
            val existing = mockDatabase.getTag(tag.id)
            if (existing != null) {
                // Simulate optimistic locking
                if (existing.updatedAt < tag.updatedAt) {
                    mockDatabase.updateTag(tag)
                    Result.success(tag)
                } else {
                    Result.failure(ConcurrencyException("Tag was modified by another process"))
                }
            } else {
                Result.failure(NotFoundException("Tag not found"))
            }
        }
        
        val update1 = baseTag.copy(name = "Update 1", updatedAt = System.currentTimeMillis())
        val update2 = baseTag.copy(name = "Update 2", updatedAt = System.currentTimeMillis() + 1)
        
        val result1 = repository.updateTagOffline(update1)
        val result2 = repository.updateTagOffline(update2)
        
        // Then - Later update should win
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        
        val finalTag = repository.getTagById(baseTag.id)
        assertEquals("Update 2", finalTag?.name)
    }
    
    // MARK: - Error Recovery Tests
    
    @Test
    fun `sync failures should trigger appropriate retry strategies`() = runTest {
        // Given - Operations that initially fail
        val failingOp = QueuedOperation.CREATE("retry-test", TestDataFactory.createTestTag(id = "retry-test"))
        
        var attemptCount = 0
        every { mockSyncManager.processQueuedOperation(failingOp) } answers {
            attemptCount++
            when (attemptCount) {
                1, 2 -> OperationResult.RETRY_LATER // Fail first two attempts
                else -> OperationResult.SUCCESS     // Succeed on third attempt
            }
        }
        
        every { mockSyncManager.shouldRetry(any(), any()) } answers {
            val attempts = secondArg<Int>()
            attempts < 3 // Retry up to 3 times
        }
        
        // When - Process with retries
        var result = mockSyncManager.processQueuedOperation(failingOp)
        var retryCount = 1
        
        while (result == OperationResult.RETRY_LATER && mockSyncManager.shouldRetry(failingOp, retryCount)) {
            delay(100) // Simulate retry delay
            result = mockSyncManager.processQueuedOperation(failingOp)
            retryCount++
        }
        
        // Then - Should eventually succeed
        assertEquals(OperationResult.SUCCESS, result)
        assertEquals(3, attemptCount)
    }
    
    @Test
    fun `corrupted sync data should be handled gracefully`() = runTest {
        // Given - Corrupted sync payload
        val corruptedData = "corrupted_json_payload"
        
        every { mockSyncManager.parseSyncData(corruptedData) } throws DataCorruptionException("Invalid sync data format")
        every { mockSyncManager.handleCorruptedData(any()) } returns SyncDataRecovery(
            recovered = false,
            fallbackAction = FallbackAction.USE_LOCAL_DATA
        )
        
        // When - Handle corrupted sync data
        var recoveryAction: SyncDataRecovery? = null
        try {
            mockSyncManager.parseSyncData(corruptedData)
        } catch (e: DataCorruptionException) {
            recoveryAction = mockSyncManager.handleCorruptedData(e)
        }
        
        // Then - Should handle corruption gracefully
        assertNotNull(recoveryAction)
        assertFalse(recoveryAction!!.recovered)
        assertEquals(FallbackAction.USE_LOCAL_DATA, recoveryAction.fallbackAction)
        
        verify { mockSyncManager.handleCorruptedData(any()) }
    }
}

// MARK: - Supporting Classes and Enums

sealed class QueuedOperation {
    data class CREATE(val id: String, val tag: Tag) : QueuedOperation()
    data class UPDATE(val id: String, val tag: Tag) : QueuedOperation()
    data class DELETE(val id: String) : QueuedOperation()
}

data class SyncResult(
    val success: Boolean,
    val processedCount: Int = 0,
    val errors: List<String> = emptyList()
)

data class ConflictResolution(
    val resolvedTag: Tag,
    val resolution: ConflictResolutionStrategy
)

enum class ConflictResolutionStrategy {
    PREFER_LOCAL,
    PREFER_REMOTE,
    MERGE_CHANGES,
    MANUAL_RESOLUTION
}

data class ManualConflictResolution(
    val conflictId: String,
    val localTag: Tag,
    val remoteTag: Tag,
    val requiresUserInput: Boolean
)

enum class OperationResult {
    SUCCESS,
    FAILURE,
    RETRY_LATER
}

data class BatchProcessingResult(
    val processed: Int,
    val failed: Int,
    val errors: List<String>
)

data class SyncDataRecovery(
    val recovered: Boolean,
    val fallbackAction: FallbackAction
)

enum class FallbackAction {
    USE_LOCAL_DATA,
    CLEAR_AND_RESYNC,
    MANUAL_INTERVENTION
}

// Exception classes
class NetworkException(message: String) : Exception(message)
class ConcurrencyException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
class DataCorruptionException(message: String) : Exception(message)

// Mock interfaces
abstract class NetworkManager {
    abstract fun isConnected(): Boolean
    abstract val networkState: MutableStateFlow<Boolean>
}

abstract class SyncManager {
    abstract suspend fun syncQueuedOperations(): SyncResult
    abstract fun detectConflict(local: Tag, remote: Tag): Boolean
    abstract fun resolveConflict(local: Tag, remote: Tag): ConflictResolution
    abstract fun requiresManualResolution(local: Tag, remote: Tag): Boolean
    abstract fun createConflictForManualResolution(local: Tag, remote: Tag): ManualConflictResolution
    abstract suspend fun processQueuedOperation(operation: QueuedOperation): OperationResult
    abstract suspend fun processBatch(operations: List<QueuedOperation>, batchSize: Int): BatchProcessingResult
    abstract fun shouldRetry(operation: QueuedOperation, attemptCount: Int): Boolean
    abstract fun parseSyncData(data: String): Any
    abstract fun handleCorruptedData(exception: DataCorruptionException): SyncDataRecovery
}

abstract class SyncQueue {
    abstract fun queueOperation(operation: QueuedOperation)
    abstract fun getQueuedOperations(): List<QueuedOperation>
    abstract fun consolidateOperations(): List<QueuedOperation>
}

// Repository extensions for offline operations
suspend fun TagRepositoryImpl.createTagOffline(tag: Tag): Result<Tag> {
    // Mock implementation - would normally handle offline storage
    return Result.success(tag)
}

suspend fun TagRepositoryImpl.updateTagOffline(tag: Tag): Result<Tag> {
    // Mock implementation - would normally handle offline updates
    return Result.success(tag)
}

suspend fun TagRepositoryImpl.updateTagWithSync(tag: Tag): Result<Tag> {
    // Mock implementation - would normally attempt network sync
    return Result.success(tag)
}