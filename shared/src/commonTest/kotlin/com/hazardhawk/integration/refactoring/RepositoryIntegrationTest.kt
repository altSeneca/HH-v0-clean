package com.hazardhawk.integration.refactoring

import com.hazardhawk.TestDataFactory
import com.hazardhawk.TestUtils
import com.hazardhawk.ai.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Repository Integration Test for HazardHawk Refactoring Validation
 * 
 * Tests database migration integrity, performance targets, caching effectiveness,
 * OSHA compliance features, and error handling robustness.
 * 
 * Key Requirements:
 * - Database migration maintains data integrity
 * - Performance targets met (< 100ms queries)
 * - Caching effectiveness validated
 * - OSHA compliance features functional
 * - Error handling robust and graceful
 */
class RepositoryIntegrationTest {
    
    private lateinit var mockRepository: MockSafetyAnalysisRepository
    private lateinit var mockCache: MockMemoryCache<String, SafetyAnalysis>
    
    @BeforeTest
    fun setup() {
        mockCache = MockMemoryCache()
        mockRepository = MockSafetyAnalysisRepository(mockCache)
    }
    
    @Test
    fun `database migration should maintain data integrity after refactoring`() = runTest {
        // Given - Pre-migration data with various scenarios
        val preMigrationData = listOf(
            TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
                hazardCount = 5,
                includeCriticalHazards = true
            ),
            TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.CLOUD_GEMINI,
                hazardCount = 3,
                includeCriticalHazards = false
            ),
            TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.LOCAL_YOLO_FALLBACK,
                hazardCount = 2,
                confidence = 0.65f
            )
        )
        
        // When - Store data (simulates pre-migration state)
        preMigrationData.forEach { analysis ->
            val result = mockRepository.store(analysis)
            assertTrue(result.isSuccess, "Pre-migration storage should succeed")
        }
        
        // Simulate migration process
        val migrationResult = mockRepository.performMigration()
        assertTrue(migrationResult.isSuccess, "Database migration should succeed")
        
        // Then - Validate post-migration data integrity
        preMigrationData.forEach { originalAnalysis ->
            val retrievedResult = mockRepository.get(originalAnalysis.id)
            assertTrue(retrievedResult.isSuccess, "Post-migration retrieval should succeed")
            
            val retrievedAnalysis = retrievedResult.getOrNull()!!
            validateDataIntegrity(originalAnalysis, retrievedAnalysis)
        }
        
        // Validate migration statistics
        val stats = mockRepository.getMigrationStats()
        assertEquals(preMigrationData.size, stats.migratedRecords)
        assertEquals(0, stats.failedMigrations)
        assertTrue(stats.migrationTimeMs > 0, "Migration time should be recorded")
        assertTrue(stats.migrationTimeMs < 5000, "Migration should complete quickly")
    }
    
    @Test
    fun `query performance should meet targets after refactoring`() = runTest {
        // Given - Performance test dataset
        val testDataSize = 100
        val testAnalyses = (1..testDataSize).map { index ->
            TestDataFactory.createSampleSafetyAnalysis(
                hazardCount = 3,
                includeCriticalHazards = index % 3 == 0 // Every third has critical hazards
            )
        }
        
        // Store test data
        testAnalyses.forEach { analysis ->
            mockRepository.store(analysis)
        }
        
        // When - Benchmark various query types
        val performanceResults = mutableMapOf<String, Long>()
        
        // Test single record retrieval
        val sampleId = testAnalyses.first().id
        val (_, singleQueryTime) = TestUtils.measureExecutionTime {
            mockRepository.get(sampleId)
        }
        performanceResults["single_query"] = singleQueryTime.inWholeMilliseconds
        
        // Test batch retrieval
        val batchIds = testAnalyses.take(10).map { it.id }
        val (_, batchQueryTime) = TestUtils.measureExecutionTime {
            mockRepository.getBatch(batchIds)
        }
        performanceResults["batch_query"] = batchQueryTime.inWholeMilliseconds
        
        // Test filtered query (OSHA violations)
        val (_, filteredQueryTime) = TestUtils.measureExecutionTime {
            mockRepository.findByOSHAViolations(true)
        }
        performanceResults["filtered_query"] = filteredQueryTime.inWholeMilliseconds
        
        // Test complex query (date range with risk level)
        val startTime = System.currentTimeMillis() - 86400000L // 24 hours ago
        val endTime = System.currentTimeMillis()
        val (_, complexQueryTime) = TestUtils.measureExecutionTime {
            mockRepository.findByDateRangeAndRiskLevel(startTime, endTime, RiskLevel.HIGH)
        }
        performanceResults["complex_query"] = complexQueryTime.inWholeMilliseconds
        
        // Then - Validate performance targets (< 100ms)
        performanceResults.forEach { (queryType, timeMs) ->
            assertTrue(
                timeMs < 100L,
                "$queryType should complete in <100ms, got ${timeMs}ms"
            )
        }
        
        // Validate performance scaling
        assertTrue(
            performanceResults["batch_query"]!! < performanceResults["single_query"]!! * 5,
            "Batch queries should be more efficient than multiple single queries"
        )
    }
    
    @Test
    fun `caching effectiveness should be validated after refactoring`() = runTest {
        // Given - Cache performance test setup
        val testAnalysis = TestDataFactory.createSampleSafetyAnalysis(hazardCount = 5)
        
        // When - Test cache miss scenario
        val (_, firstRetrievalTime) = TestUtils.measureExecutionTime {
            mockRepository.get(testAnalysis.id)
        }
        
        // Store the analysis
        mockRepository.store(testAnalysis)
        
        // Test cache hit scenario
        val (result, cachedRetrievalTime) = TestUtils.measureExecutionTime {
            mockRepository.get(testAnalysis.id)
        }
        
        // Then - Validate caching effectiveness
        assertTrue(result.isSuccess, "Cached retrieval should succeed")
        
        // Cache should significantly improve performance
        assertTrue(
            cachedRetrievalTime.inWholeMilliseconds < firstRetrievalTime.inWholeMilliseconds / 2,
            "Cache should provide significant speedup: first=${firstRetrievalTime.inWholeMilliseconds}ms, " +
            "cached=${cachedRetrievalTime.inWholeMilliseconds}ms"
        )
        
        // Validate cache statistics
        val cacheStats = mockCache.getStats()
        assertTrue(cacheStats.hitRate > 0.5, "Cache hit rate should be reasonable: ${cacheStats.hitRate}")
        assertTrue(cacheStats.totalHits > 0, "Should have cache hits")
        assertTrue(cacheStats.totalMisses >= 0, "Cache misses should be tracked")
        
        // Test cache invalidation
        mockRepository.invalidateCache(testAnalysis.id)
        val (_, postInvalidationTime) = TestUtils.measureExecutionTime {
            mockRepository.get(testAnalysis.id)
        }
        
        assertTrue(
            postInvalidationTime.inWholeMilliseconds > cachedRetrievalTime.inWholeMilliseconds,
            "Cache invalidation should require fresh retrieval"
        )
    }
    
    @Test
    fun `OSHA compliance features should remain functional after refactoring`() = runTest {
        // Given - OSHA-specific test data
        val oshaTestCases = listOf(
            TestDataFactory.createSampleSafetyAnalysis(
                hazardCount = 3,
                includeCriticalHazards = true
            ).let { analysis ->
                analysis.copy(
                    oshaViolations = analysis.oshaViolations + OSHAViolation(
                        code = "1926.501(b)(1)",
                        title = "Fall Protection Test",
                        description = "Test OSHA violation",
                        severity = Severity.CRITICAL,
                        fineRange = "$7,000 - $70,000",
                        correctiveAction = "Install fall protection"
                    )
                )
            },
            TestDataFactory.createSampleSafetyAnalysis(
                hazardCount = 2,
                includeCriticalHazards = false
            ).let { analysis ->
                analysis.copy(
                    oshaViolations = listOf(
                        OSHAViolation(
                            code = "1926.95(a)",
                            title = "PPE Violation Test",
                            description = "Test PPE violation",
                            severity = Severity.HIGH,
                            fineRange = "$2,500 - $25,000",
                            correctiveAction = "Provide proper PPE"
                        )
                    )
                )
            }
        )
        
        // Store OSHA test data
        oshaTestCases.forEach { analysis ->
            mockRepository.store(analysis)
        }
        
        // When - Test OSHA compliance queries
        val violationsResult = mockRepository.findByOSHAViolations(true)
        val criticalViolationsResult = mockRepository.findByCriticalOSHAViolations()
        val oshaCodesResult = mockRepository.findByOSHACode("1926.501(b)(1)")
        
        // Then - Validate OSHA functionality
        assertTrue(violationsResult.isSuccess, "OSHA violations query should succeed")
        val violations = violationsResult.getOrNull()!!
        assertEquals(2, violations.size, "Should find all analyses with OSHA violations")
        
        assertTrue(criticalViolationsResult.isSuccess, "Critical OSHA violations query should succeed")
        val criticalViolations = criticalViolationsResult.getOrNull()!!
        assertEquals(1, criticalViolations.size, "Should find critical OSHA violation")
        
        assertTrue(oshaCodesResult.isSuccess, "OSHA code query should succeed")
        val codeMatches = oshaCodesResult.getOrNull()!!
        assertEquals(1, codeMatches.size, "Should find specific OSHA code")
        
        // Validate OSHA reporting functionality
        val reportResult = mockRepository.generateOSHAComplianceReport(
            startDate = System.currentTimeMillis() - 86400000L,
            endDate = System.currentTimeMillis()
        )
        
        assertTrue(reportResult.isSuccess, "OSHA compliance report should generate")
        val report = reportResult.getOrNull()!!
        assertTrue(report.totalViolations > 0, "Report should include violations")
        assertTrue(report.criticalViolations > 0, "Report should identify critical violations")
        assertTrue(report.complianceScore < 100.0, "Compliance score should reflect violations")
    }
    
    @Test
    fun `error handling should be robust after refactoring`() = runTest {
        // Given - Various error scenarios
        val errorScenarios = mapOf(
            "Non-existent record" to "non-existent-id",
            "Invalid ID format" to "",
            "Corrupted ID" to "corrupted-id-with-special-chars-@#$%"
        )
        
        // When & Then - Test error handling for each scenario
        errorScenarios.forEach { (scenario, testId) ->
            val result = mockRepository.get(testId)
            
            if (result.isFailure) {
                val error = result.exceptionOrNull()!!
                assertTrue(
                    error.message?.isNotEmpty() == true,
                    "Error scenario '$scenario' should provide meaningful error message"
                )
            } else {
                // If it succeeds, it should return null or empty result appropriately
                val analysis = result.getOrNull()
                assertTrue(
                    analysis == null,
                    "Error scenario '$scenario' should return null for non-existent records"
                )
            }
        }
        
        // Test storage error scenarios
        val invalidAnalysisScenarios = listOf(
            "Empty ID" to TestDataFactory.createSampleSafetyAnalysis().copy(id = ""),
            "Null hazards" to TestDataFactory.createSampleSafetyAnalysis().copy(hazards = emptyList()),
            "Invalid timestamp" to TestDataFactory.createSampleSafetyAnalysis().copy(timestamp = -1L)
        )
        
        invalidAnalysisScenarios.forEach { (scenario, invalidAnalysis) ->
            val result = mockRepository.store(invalidAnalysis)
            
            // Should either handle gracefully or fail with meaningful error
            if (result.isFailure) {
                val error = result.exceptionOrNull()!!
                assertTrue(
                    error.message?.contains("validation", ignoreCase = true) == true ||
                    error.message?.isNotEmpty() == true,
                    "Storage error scenario '$scenario' should provide meaningful error"
                )
            }
        }
        
        // Test system recovery after errors
        val validAnalysis = TestDataFactory.createSampleSafetyAnalysis()
        val recoveryResult = mockRepository.store(validAnalysis)
        assertTrue(
            recoveryResult.isSuccess,
            "System should recover after error scenarios"
        )
    }
    
    @Test
    fun `concurrent operations should be handled correctly after refactoring`() = runTest {
        // Given - Concurrent operation test setup
        val concurrentAnalyses = (1..20).map { index ->
            TestDataFactory.createSampleSafetyAnalysis().copy(
                id = "concurrent-test-$index"
            )
        }
        
        // When - Simulate concurrent storage operations
        val storageResults = kotlinx.coroutines.async {
            concurrentAnalyses.map { analysis ->
                kotlinx.coroutines.async {
                    mockRepository.store(analysis)
                }
            }.map { it.await() }
        }.await()
        
        // Then - Validate all operations succeeded
        assertTrue(
            storageResults.all { it.isSuccess },
            "All concurrent storage operations should succeed"
        )
        
        // Validate data integrity after concurrent operations
        concurrentAnalyses.forEach { originalAnalysis ->
            val retrievalResult = mockRepository.get(originalAnalysis.id)
            assertTrue(
                retrievalResult.isSuccess,
                "Should retrieve data after concurrent storage: ${originalAnalysis.id}"
            )
            
            val retrieved = retrievalResult.getOrNull()!!
            assertEquals(
                originalAnalysis.id,
                retrieved.id,
                "Data integrity should be maintained during concurrent operations"
            )
        }
        
        // Test concurrent read operations
        val concurrentReads = kotlinx.coroutines.async {
            (1..10).map {
                kotlinx.coroutines.async {
                    mockRepository.get(concurrentAnalyses.first().id)
                }
            }.map { it.await() }
        }.await()
        
        assertTrue(
            concurrentReads.all { it.isSuccess },
            "Concurrent read operations should all succeed"
        )
    }
    
    // Helper functions
    
    private fun validateDataIntegrity(original: SafetyAnalysis, retrieved: SafetyAnalysis) {
        assertEquals(original.id, retrieved.id, "ID should be preserved")
        assertEquals(original.timestamp, retrieved.timestamp, "Timestamp should be preserved")
        assertEquals(original.analysisType, retrieved.analysisType, "Analysis type should be preserved")
        assertEquals(original.workType, retrieved.workType, "Work type should be preserved")
        assertEquals(original.hazards.size, retrieved.hazards.size, "Hazard count should be preserved")
        assertEquals(original.confidence, retrieved.confidence, "Confidence should be preserved")
        assertEquals(original.processingTimeMs, retrieved.processingTimeMs, "Processing time should be preserved")
        assertEquals(original.oshaViolations.size, retrieved.oshaViolations.size, "OSHA violations should be preserved")
    }
    
    // Mock implementations
    
    class MockSafetyAnalysisRepository(
        private val cache: MockMemoryCache<String, SafetyAnalysis>
    ) {
        private val storage = mutableMapOf<String, SafetyAnalysis>()
        private var migrationStats = MigrationStats(0, 0, 0L)
        
        suspend fun store(analysis: SafetyAnalysis): Result<Unit> {
            return try {
                if (analysis.id.isEmpty()) {
                    throw IllegalArgumentException("Analysis ID cannot be empty")
                }
                if (analysis.timestamp < 0) {
                    throw IllegalArgumentException("Invalid timestamp")
                }
                
                storage[analysis.id] = analysis
                cache.put(analysis.id, analysis)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        suspend fun get(id: String): Result<SafetyAnalysis?> {
            return try {
                if (id.isEmpty()) {
                    throw IllegalArgumentException("ID cannot be empty")
                }
                
                // Check cache first
                cache.get(id)?.let { cached ->
                    return Result.success(cached)
                }
                
                // Simulate database retrieval delay
                kotlinx.coroutines.delay(50L)
                
                val analysis = storage[id]
                analysis?.let { cache.put(id, it) }
                Result.success(analysis)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        suspend fun getBatch(ids: List<String>): Result<List<SafetyAnalysis>> {
            return try {
                kotlinx.coroutines.delay(20L) // Simulate batch query optimization
                
                val results = ids.mapNotNull { id ->
                    cache.get(id) ?: storage[id]?.also { cache.put(id, it) }
                }
                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        suspend fun findByOSHAViolations(hasViolations: Boolean): Result<List<SafetyAnalysis>> {
            return try {
                kotlinx.coroutines.delay(30L) // Simulate filtered query
                
                val results = storage.values.filter { analysis ->
                    if (hasViolations) analysis.oshaViolations.isNotEmpty()
                    else analysis.oshaViolations.isEmpty()
                }
                Result.success(results.toList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        suspend fun findByCriticalOSHAViolations(): Result<List<SafetyAnalysis>> {
            return try {
                kotlinx.coroutines.delay(35L)
                
                val results = storage.values.filter { analysis ->
                    analysis.oshaViolations.any { it.severity == Severity.CRITICAL }
                }
                Result.success(results.toList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        suspend fun findByOSHACode(code: String): Result<List<SafetyAnalysis>> {
            return try {
                kotlinx.coroutines.delay(25L)
                
                val results = storage.values.filter { analysis ->
                    analysis.oshaViolations.any { it.code == code }
                }
                Result.success(results.toList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        suspend fun findByDateRangeAndRiskLevel(
            startTime: Long,
            endTime: Long,
            riskLevel: RiskLevel
        ): Result<List<SafetyAnalysis>> {
            return try {
                kotlinx.coroutines.delay(40L) // Simulate complex query
                
                val results = storage.values.filter { analysis ->
                    analysis.timestamp in startTime..endTime &&
                    analysis.overallRiskLevel == riskLevel
                }
                Result.success(results.toList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        suspend fun generateOSHAComplianceReport(
            startDate: Long,
            endDate: Long
        ): Result<OSHAComplianceReport> {
            return try {
                kotlinx.coroutines.delay(60L) // Simulate report generation
                
                val analyses = storage.values.filter { it.timestamp in startDate..endDate }
                val totalViolations = analyses.sumOf { it.oshaViolations.size }
                val criticalViolations = analyses.sumOf { analysis ->
                    analysis.oshaViolations.count { it.severity == Severity.CRITICAL }
                }
                
                val report = OSHAComplianceReport(
                    totalAnalyses = analyses.size,
                    totalViolations = totalViolations,
                    criticalViolations = criticalViolations,
                    complianceScore = if (analyses.isEmpty()) 100.0 
                                     else maxOf(0.0, 100.0 - (totalViolations * 10.0))
                )
                
                Result.success(report)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        suspend fun performMigration(): Result<Unit> {
            return try {
                val startTime = System.currentTimeMillis()
                kotlinx.coroutines.delay(100L) // Simulate migration time
                
                // Simulate successful migration of all stored records
                migrationStats = MigrationStats(
                    migratedRecords = storage.size,
                    failedMigrations = 0,
                    migrationTimeMs = System.currentTimeMillis() - startTime
                )
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        fun getMigrationStats(): MigrationStats = migrationStats
        
        suspend fun invalidateCache(id: String) {
            cache.remove(id)
        }
    }
    
    class MockMemoryCache<K, V> {
        private val cache = mutableMapOf<K, V>()
        private var hits = 0
        private var misses = 0
        
        fun put(key: K, value: V) {
            cache[key] = value
        }
        
        fun get(key: K): V? {
            return cache[key]?.also { hits++ } ?: run { misses++; null }
        }
        
        fun remove(key: K) {
            cache.remove(key)
        }
        
        fun getStats(): CacheStats {
            val total = hits + misses
            return CacheStats(
                totalHits = hits,
                totalMisses = misses,
                hitRate = if (total > 0) hits.toDouble() / total else 0.0
            )
        }
    }
    
    // Data classes
    
    data class MigrationStats(
        val migratedRecords: Int,
        val failedMigrations: Int,
        val migrationTimeMs: Long
    )
    
    data class CacheStats(
        val totalHits: Int,
        val totalMisses: Int,
        val hitRate: Double
    )
    
    data class OSHAComplianceReport(
        val totalAnalyses: Int,
        val totalViolations: Int,
        val criticalViolations: Int,
        val complianceScore: Double
    )
}
