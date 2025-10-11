package com.hazardhawk.data.repositories

import com.hazardhawk.core.models.*
import com.hazardhawk.models.*
// Explicit imports to resolve enum ambiguity - using com.hazardhawk.models package
import com.hazardhawk.models.OSHASeverity
import com.hazardhawk.models.OSHAViolationType
import com.hazardhawk.security.storage.SecureStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.days

/**
 * Repository for managing OSHA regulation data from ecfr.gov API
 * Handles fetching, caching, and searching OSHA codes
 */
interface OSHARegulationRepository {
    /**
     * Get sync status and configuration
     */
    val syncStatus: StateFlow<OSHAUpdateStatus>

    /**
     * Check for and download latest OSHA regulations
     */
    suspend fun syncOSHARegulations(forceUpdate: Boolean = false): Result<OSHAUpdateStatus>

    /**
     * Search for OSHA regulations by various criteria
     */
    suspend fun searchRegulations(
        query: String,
        searchType: OSHASearchType = OSHASearchType.KEYWORD
    ): Result<OSHARegulationLookup>

    /**
     * Get specific OSHA regulation by ID (e.g., "1926.95")
     */
    suspend fun getRegulationById(regulationId: String): Result<OSHARegulationEntity?>

    /**
     * Get all regulations for a specific part (e.g., Part 1926)
     */
    suspend fun getRegulationsByPart(partNumber: String): Result<List<OSHARegulationEntity>>

    /**
     * Get regulation content with full text
     */
    suspend fun getRegulationContent(regulationId: String): Result<OSHARegulationContent?>

    /**
     * Configure sync settings
     */
    suspend fun updateSyncConfig(config: OSHASyncConfig): Result<Unit>

    /**
     * Get current sync configuration
     */
    suspend fun getSyncConfig(): Result<OSHASyncConfig>

    /**
     * Check if an update is needed
     */
    suspend fun isUpdateNeeded(): Boolean
}

/**
 * Implementation of OSHARegulationRepository
 */
class OSHARegulationRepositoryImpl(
    private val httpClient: HttpClient,
    private val secureStorage: SecureStorage
) : OSHARegulationRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _syncStatus = MutableStateFlow(
        OSHAUpdateStatus(
            lastCheckDate = 0L,
            lastUpdateDate = 0L,
            apiVersion = "v1",
            totalRegulations = 0,
            updateInProgress = false,
            nextScheduledUpdate = Clock.System.now().plus(30.days).toEpochMilliseconds()
        )
    )
    override val syncStatus: StateFlow<OSHAUpdateStatus> = _syncStatus.asStateFlow()

    private val baseUrl = "https://www.ecfr.gov/api/versioner/v1"
    private val regulationCache = mutableMapOf<String, OSHARegulationEntity>()
    private val contentCache = mutableMapOf<String, OSHARegulationContent>()

    override suspend fun syncOSHARegulations(forceUpdate: Boolean): Result<OSHAUpdateStatus> {
        return try {
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val config = getSyncConfig().getOrElse { OSHASyncConfig() }

            // Check if update is needed
            if (!forceUpdate && !isUpdateNeeded()) {
                return Result.success(_syncStatus.value)
            }

            _syncStatus.value = _syncStatus.value.copy(
                updateInProgress = true,
                lastError = null
            )

            // Get current date for API call
            val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val dateString = "${currentDate.year}-${currentDate.monthNumber.toString().padStart(2, '0')}-${currentDate.dayOfMonth.toString().padStart(2, '0')}"

            // Fetch Title 29 structure (OSHA regulations)
            val structureUrl = "$baseUrl/structure/$dateString/title-29.json"
            val response: HttpResponse = httpClient.get(structureUrl)

            if (!response.status.isSuccess()) {
                val error = "Failed to fetch OSHA regulations: ${response.status}"
                _syncStatus.value = _syncStatus.value.copy(
                    updateInProgress = false,
                    lastError = error
                )
                return Result.failure(Exception(error))
            }

            val regulationData: OSHARegulationData = response.body()

            // Process and cache regulations
            val processedCount = processRegulationData(regulationData)

            // Update status
            val updatedStatus = OSHAUpdateStatus(
                lastCheckDate = currentTime,
                lastUpdateDate = currentTime,
                apiVersion = regulationData.apiVersion,
                totalRegulations = processedCount,
                updateInProgress = false,
                lastError = null,
                nextScheduledUpdate = currentTime + config.syncInterval
            )

            _syncStatus.value = updatedStatus

            // Save status to storage
            secureStorage.setString("osha_update_status", json.encodeToString(OSHAUpdateStatus.serializer(), updatedStatus))

            Result.success(updatedStatus)

        } catch (e: Exception) {
            val error = "OSHA sync failed: ${e.message}"
            _syncStatus.value = _syncStatus.value.copy(
                updateInProgress = false,
                lastError = error
            )
            Result.failure(e)
        }
    }

    override suspend fun searchRegulations(
        query: String,
        searchType: OSHASearchType
    ): Result<OSHARegulationLookup> {
        return try {
            val matches = when (searchType) {
                OSHASearchType.REGULATION_ID -> searchByRegulationId(query)
                OSHASearchType.KEYWORD -> searchByKeyword(query)
                OSHASearchType.HAZARD_TYPE -> searchByHazardType(query)
                OSHASearchType.INDUSTRY_CODE -> searchByIndustryCode(query)
                OSHASearchType.PENALTY_RANGE -> searchByPenaltyRange(query)
            }

            val lookup = OSHARegulationLookup(
                query = query,
                matches = matches,
                totalResults = matches.size,
                searchType = searchType
            )

            Result.success(lookup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRegulationById(regulationId: String): Result<OSHARegulationEntity?> {
        return try {
            // Check cache first
            val cached = regulationCache[regulationId]
            if (cached != null) {
                return Result.success(cached)
            }

            // Search for regulation
            val searchResult = searchRegulations(regulationId, OSHASearchType.REGULATION_ID)
            val matches = searchResult.getOrElse { return Result.success(null) }.matches

            val regulation = matches.firstOrNull { it.exactMatch }?.regulation
            Result.success(regulation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRegulationsByPart(partNumber: String): Result<List<OSHARegulationEntity>> {
        return try {
            val regulations = regulationCache.values.filter { regulation ->
                regulation.identifier.startsWith(partNumber) &&
                regulation.type == OSHARegulationType.SECTION
            }.sortedBy { it.identifier }

            Result.success(regulations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRegulationContent(regulationId: String): Result<OSHARegulationContent?> {
        return try {
            // Check cache first
            val cached = contentCache[regulationId]
            if (cached != null) {
                return Result.success(cached)
            }

            // In a real implementation, this would fetch detailed content from the API
            // For now, return mock content based on the regulation
            val regulation = getRegulationById(regulationId).getOrNull()
            if (regulation != null) {
                val content = createMockRegulationContent(regulation)
                contentCache[regulationId] = content
                Result.success(content)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSyncConfig(config: OSHASyncConfig): Result<Unit> {
        return try {
            secureStorage.setString("osha_sync_config", json.encodeToString(OSHASyncConfig.serializer(), config))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSyncConfig(): Result<OSHASyncConfig> {
        return try {
            val configJson = secureStorage.getString("osha_sync_config")
            if (configJson != null) {
                val config = json.decodeFromString(OSHASyncConfig.serializer(), configJson)
                Result.success(config)
            } else {
                Result.success(OSHASyncConfig()) // Default config
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isUpdateNeeded(): Boolean {
        val config = getSyncConfig().getOrElse { OSHASyncConfig() }
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val lastUpdate = _syncStatus.value.lastUpdateDate

        return config.enabled && (currentTime - lastUpdate) > config.syncInterval
    }

    private suspend fun processRegulationData(data: OSHARegulationData): Int {
        var processedCount = 0

        // Process each child node recursively
        data.children.forEach { child ->
            processedCount += processRegulationNode(child, null, 1)
        }

        return processedCount
    }

    private suspend fun processRegulationNode(
        node: OSHARegulationNode,
        parentId: String?,
        level: Int
    ): Int {
        var processedCount = 0

        // Create regulation entity
        val entity = OSHARegulationEntity(
            id = node.identifier,
            identifier = node.identifier,
            label = node.label,
            type = node.type,
            level = level,
            parentId = parentId,
            fullText = node.description,
            keywords = extractKeywords(node.label, node.description),
            lastUpdated = Clock.System.now().toEpochMilliseconds(),
            isActive = !node.reserved,
            size = node.size
        )

        // Cache the entity
        regulationCache[node.identifier] = entity
        processedCount++

        // Process children
        node.children.forEach { child ->
            processedCount += processRegulationNode(child, node.identifier, level + 1)
        }

        return processedCount
    }

    private fun extractKeywords(label: String, description: String): String {
        val text = "$label $description".lowercase()
        val keywords = mutableSetOf<String>()

        // Common safety keywords
        val safetyTerms = listOf(
            "ppe", "protective", "equipment", "safety", "hazard", "fall", "electrical",
            "scaffold", "ladder", "crane", "excavation", "confined", "space", "chemical",
            "noise", "respiratory", "eye", "head", "foot", "hand", "protection",
            "guardrail", "harness", "helmet", "glove", "boot", "goggle", "mask"
        )

        safetyTerms.forEach { term ->
            if (text.contains(term)) {
                keywords.add(term)
            }
        }

        // Add regulation-specific terms
        text.split(" ").forEach { word ->
            if (word.length > 3 && !word.matches(Regex("\\d+"))) {
                keywords.add(word)
            }
        }

        return keywords.take(10).joinToString(",")
    }

    private suspend fun searchByRegulationId(query: String): List<OSHARegulationMatch> {
        val results = mutableListOf<OSHARegulationMatch>()
        val normalizedQuery = query.trim().lowercase()

        regulationCache.values.forEach { regulation ->
            val normalizedId = regulation.identifier.lowercase()

            when {
                normalizedId == normalizedQuery -> {
                    results.add(OSHARegulationMatch(
                        regulation = regulation,
                        relevanceScore = 1.0f,
                        matchedKeywords = listOf(regulation.identifier),
                        contextSnippet = regulation.label,
                        exactMatch = true
                    ))
                }
                normalizedId.contains(normalizedQuery) -> {
                    results.add(OSHARegulationMatch(
                        regulation = regulation,
                        relevanceScore = 0.8f,
                        matchedKeywords = listOf(regulation.identifier),
                        contextSnippet = regulation.label,
                        exactMatch = false
                    ))
                }
            }
        }

        return results.sortedByDescending { it.relevanceScore }
    }

    private suspend fun searchByKeyword(query: String): List<OSHARegulationMatch> {
        val results = mutableListOf<OSHARegulationMatch>()
        val keywords = query.lowercase().split(" ").filter { it.length > 2 }

        regulationCache.values.forEach { regulation ->
            val regulationText = "${regulation.label} ${regulation.fullText} ${regulation.keywords}".lowercase()
            val matchedKeywords = mutableListOf<String>()
            var score = 0.0f

            keywords.forEach { keyword ->
                if (regulationText.contains(keyword)) {
                    matchedKeywords.add(keyword)
                    score += when {
                        regulation.label.lowercase().contains(keyword) -> 0.4f
                        regulation.keywords.lowercase().contains(keyword) -> 0.3f
                        else -> 0.1f
                    }
                }
            }

            if (matchedKeywords.isNotEmpty()) {
                results.add(OSHARegulationMatch(
                    regulation = regulation,
                    relevanceScore = (score / keywords.size).coerceAtMost(1.0f),
                    matchedKeywords = matchedKeywords,
                    contextSnippet = regulation.label,
                    exactMatch = false
                ))
            }
        }

        return results.sortedByDescending { it.relevanceScore }.take(50)
    }

    private suspend fun searchByHazardType(query: String): List<OSHARegulationMatch> {
        // Map hazard types to OSHA regulation categories
        val hazardMapping = mapOf(
            "fall" to listOf("1926.501", "1926.502", "1926.503"),
            "electrical" to listOf("1926.95", "1926.137", "1926.416"),
            "ppe" to listOf("1926.95", "1926.96", "1926.137"),
            "scaffold" to listOf("1926.451", "1926.452"),
            "crane" to listOf("1926.1400", "1926.1401"),
            "excavation" to listOf("1926.650", "1926.651"),
            "confined" to listOf("1926.146", "1926.147")
        )

        val relevantIds = hazardMapping[query.lowercase()] ?: emptyList()
        val results = mutableListOf<OSHARegulationMatch>()

        relevantIds.forEach { id ->
            regulationCache.values.filter { it.identifier.startsWith(id) }.forEach { regulation ->
                results.add(OSHARegulationMatch(
                    regulation = regulation,
                    relevanceScore = 0.9f,
                    matchedKeywords = listOf(query),
                    contextSnippet = regulation.label,
                    exactMatch = false
                ))
            }
        }

        return results
    }

    private suspend fun searchByIndustryCode(query: String): List<OSHARegulationMatch> {
        // Industry-specific searches would require additional mapping
        return searchByKeyword("construction $query")
    }

    private suspend fun searchByPenaltyRange(query: String): List<OSHARegulationMatch> {
        // Penalty range searches would require penalty data
        return searchByKeyword("penalty $query")
    }

    private fun createMockRegulationContent(regulation: OSHARegulationEntity): OSHARegulationContent {
        // Create mock content based on the regulation type
        val requirements = when {
            regulation.identifier.contains("1926.95") -> listOf(
                OSHARequirement(
                    id = "${regulation.identifier}_req_1",
                    subsection = "(a)(1)",
                    requirementText = "Employees working in areas where there is a possible danger of head injury from impact, or from falling or flying objects, or from electrical shock and burns, shall be protected by protective helmets.",
                    applicableIndustries = listOf("Construction", "General Industry"),
                    keywords = listOf("hard hat", "helmet", "head protection", "impact"),
                    severity = OSHASeverity.SERIOUS
                )
            )
            regulation.identifier.contains("1926.501") -> listOf(
                OSHARequirement(
                    id = "${regulation.identifier}_req_1",
                    subsection = "(b)(1)",
                    requirementText = "Each employee on a walking/working surface (horizontal and vertical surface) with an unprotected side or edge which is 6 feet (1.8 m) or more above a lower level shall be protected from falling by the use of guardrail systems, safety net systems, or personal fall arrest systems.",
                    applicableIndustries = listOf("Construction"),
                    keywords = listOf("fall protection", "guardrail", "safety net", "harness"),
                    severity = OSHASeverity.SERIOUS
                )
            )
            else -> emptyList()
        }

        val penalties = listOf(
            OSHAPenalty(
                violationType = OSHAViolationType.SERIOUS,
                minimumPenalty = 1000.0,
                maximumPenalty = 15625.0,
                effectiveDate = "2024-01-01",
                description = "Penalty range for serious violations"
            )
        )

        return OSHARegulationContent(
            sectionId = regulation.identifier,
            fullText = regulation.fullText,
            requirements = requirements,
            penalties = penalties,
            lastModified = Clock.System.now().toString(),
            authority = "29 CFR ${regulation.identifier}",
            source = "Federal Register"
        )
    }
}
