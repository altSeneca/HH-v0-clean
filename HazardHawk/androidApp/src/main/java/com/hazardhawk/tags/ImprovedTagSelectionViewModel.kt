package com.hazardhawk.tags

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.models.TagCategory
import com.hazardhawk.models.RecommendationReason
import com.hazardhawk.tags.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Improved ViewModel that properly manages tag selection state,
 * handles multi-selection correctly, and provides proper navigation.
 */
class ImprovedTagSelectionViewModel : ViewModel() {
    
    private val _state = MutableStateFlow(ImprovedTagSelectionState())
    val state: StateFlow<ImprovedTagSelectionState> = _state.asStateFlow()
    
    // Navigation callback for when Continue is pressed with valid selection
    var onNavigateToNextStep: ((List<String>) -> Unit)? = null
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            // Simulate loading delay
            delay(500)
            
            val allTags = getAllAvailableTags()
            
            _state.value = _state.value.copy(
                quickTags = allTags.take(8),
                recommendedTags = generateRecommendations(allTags),
                tagsBySeverity = groupTagsBySeverity(allTags),
                recentTags = getRecentTags(allTags),
                availableTagsCount = allTags.size,
                isLoading = false,
                canProceed = false // Initially no tags selected
            )
        }
    }
    
    fun toggleComplianceStatus(isCompliant: Boolean) {
        _state.value = _state.value.copy(isCompliant = isCompliant)
    }
    
    fun toggleTagSelection(tagId: String) {
        val currentState = _state.value
        val newSelectedTags = if (tagId in currentState.selectedTags) {
            currentState.selectedTags - tagId
        } else {
            currentState.selectedTags + tagId
        }
        
        // Update state with proper validation
        _state.value = currentState.copy(
            selectedTags = newSelectedTags,
            canProceed = newSelectedTags.isNotEmpty(),
            showNoSelectionWarning = false // Clear warning when selection changes
        )
    }
    
    fun selectAll() {
        val currentState = _state.value
        val allTagIds = when {
            currentState.searchResults.isNotEmpty() -> currentState.searchResults.map { it.id }
            currentState.tagsBySeverity.isNotEmpty() -> 
                currentState.tagsBySeverity.values.flatten().map { it.id }
            else -> currentState.quickTags.map { it.id }
        }.toSet()
        
        _state.value = currentState.copy(
            selectedTags = allTagIds,
            canProceed = true,
            showNoSelectionWarning = false
        )
    }
    
    fun clearAll() {
        _state.value = _state.value.copy(
            selectedTags = emptySet(),
            canProceed = false,
            showNoSelectionWarning = false
        )
    }
    
    fun searchTags(query: String) {
        val allTags = getAllAvailableTags()
        val filteredTags = if (query.isBlank()) {
            emptyList()
        } else {
            allTags.filter { 
                it.name.contains(query, ignoreCase = true) ||
                it.description?.contains(query, ignoreCase = true) == true ||
                it.oshaReferences.any { ref -> ref.contains(query, ignoreCase = true) }
            }
        }
        
        _state.value = _state.value.copy(
            searchQuery = query,
            searchResults = filteredTags
        )
    }
    
    fun createCustomTag(name: String, category: TagCategory) {
        viewModelScope.launch {
            val newTag = UITag(
                id = "custom_${System.currentTimeMillis()}",
                name = name,
                category = category,
                displayColor = getCategoryColor(category),
                isCustom = true,
                description = "Custom tag created by user"
            )
            
            // Add to quick tags and select it
            val updatedQuickTags = _state.value.quickTags + newTag
            val updatedSelectedTags = _state.value.selectedTags + newTag.id
            
            _state.value = _state.value.copy(
                quickTags = updatedQuickTags,
                selectedTags = updatedSelectedTags,
                canProceed = true,
                searchQuery = "", // Clear search
                searchResults = emptyList()
            )
            
            // In a real app, save to database
            saveCustomTagToDatabase(newTag)
        }
    }
    
    fun confirmSelection() {
        val currentState = _state.value
        
        if (currentState.selectedTags.isEmpty()) {
            // Show warning if no tags selected
            _state.value = currentState.copy(
                showNoSelectionWarning = true
            )
            return
        }
        
        // Process the selected tags
        val selectedTagsList = currentState.selectedTags.toList()
        
        viewModelScope.launch {
            // Save the selection to database
            saveTagSelection(selectedTagsList, currentState.isCompliant)
            
            // Navigate to next step
            onNavigateToNextStep?.invoke(selectedTagsList)
            
            // Reset state for next use
            resetState()
        }
    }
    
    fun dismiss() {
        resetState()
    }
    
    private fun resetState() {
        _state.value = ImprovedTagSelectionState()
        loadInitialData() // Reload for next use
    }
    
    private fun getAllAvailableTags(): List<UITag> {
        // In a real app, load from repository
        return listOf(
            // PPE Tags
            UITag(
                id = "ppe_hard_hat",
                name = "Missing Hard Hat",
                category = TagCategory.PPE,
                displayColor = Color(0xFFD32F2F),
                oshaReferences = listOf("1926.95"),
                description = "Worker without required head protection"
            ),
            UITag(
                id = "ppe_safety_vest",
                name = "No Safety Vest",
                category = TagCategory.PPE,
                displayColor = Color(0xFFFF6B00),
                oshaReferences = listOf("1926.95", "1926.201"),
                description = "Missing high-visibility safety vest"
            ),
            UITag(
                id = "ppe_gloves",
                name = "No Safety Gloves",
                category = TagCategory.PPE,
                displayColor = Color(0xFFFFC107),
                oshaReferences = listOf("1926.95"),
                description = "Missing hand protection"
            ),
            
            // Safety Tags
            UITag(
                id = "safety_fall_protection",
                name = "Fall Hazard",
                category = TagCategory.GENERAL_SAFETY,
                displayColor = Color(0xFFD32F2F),
                oshaReferences = listOf("1926.501", "1926.502"),
                description = "Unprotected edge or opening"
            ),
            UITag(
                id = "safety_electrical",
                name = "Electrical Hazard",
                category = TagCategory.GENERAL_SAFETY,
                displayColor = Color(0xFFD32F2F),
                oshaReferences = listOf("1926.416", "1926.417"),
                description = "Exposed electrical components"
            ),
            UITag(
                id = "safety_housekeeping",
                name = "Poor Housekeeping",
                category = TagCategory.GENERAL_SAFETY,
                displayColor = Color(0xFFFFC107),
                oshaReferences = listOf("1926.25"),
                description = "Cluttered or disorganized work area"
            ),
            UITag(
                id = "safety_scaffolding",
                name = "Scaffold Issues",
                category = TagCategory.GENERAL_SAFETY,
                displayColor = Color(0xFFFF6B00),
                oshaReferences = listOf("1926.451"),
                description = "Improper scaffold setup or use"
            ),
            
            // Equipment Tags
            UITag(
                id = "equipment_crane",
                name = "Crane Safety",
                category = TagCategory.EQUIPMENT_SAFETY,
                displayColor = Color(0xFFFF6B00),
                oshaReferences = listOf("1926.1400"),
                description = "Crane operation safety issue"
            ),
            UITag(
                id = "equipment_ladder",
                name = "Ladder Safety",
                category = TagCategory.EQUIPMENT_SAFETY,
                displayColor = Color(0xFFFFC107),
                oshaReferences = listOf("1926.1053"),
                description = "Improper ladder use or setup"
            ),
            UITag(
                id = "equipment_tools",
                name = "Tool Safety",
                category = TagCategory.EQUIPMENT_SAFETY,
                displayColor = Color(0xFF4CAF50),
                oshaReferences = listOf("1926.301"),
                description = "Power tool safety concerns"
            ),
            
            // Environmental Tags
            UITag(
                id = "env_spill",
                name = "Chemical Spill",
                category = TagCategory.ENVIRONMENTAL,
                displayColor = Color(0xFFD32F2F),
                oshaReferences = listOf("1926.65"),
                description = "Hazardous material spill"
            ),
            UITag(
                id = "env_dust",
                name = "Dust Control",
                category = TagCategory.ENVIRONMENTAL,
                displayColor = Color(0xFFFFC107),
                oshaReferences = listOf("1926.55"),
                description = "Excessive dust or silica exposure"
            ),
            
            // Compliance Tags
            UITag(
                id = "compliance_permit",
                name = "Permit Required",
                category = TagCategory.GENERAL_SAFETY,
                displayColor = Color(0xFFFF6B00),
                oshaReferences = listOf("1926.1200"),
                description = "Work requires special permit"
            ),
            UITag(
                id = "compliance_training",
                name = "Training Required",
                category = TagCategory.GENERAL_SAFETY,
                displayColor = Color(0xFF4CAF50),
                oshaReferences = listOf("1926.21"),
                description = "Worker needs safety training"
            )
        )
    }
    
    private fun groupTagsBySeverity(tags: List<UITag>): Map<HazardSeverity, List<UITag>> {
        return mapOf(
            HazardSeverity.CRITICAL to tags.filter { 
                it.name.contains("electrical", true) || 
                it.name.contains("fall", true) ||
                it.name.contains("spill", true)
            },
            HazardSeverity.HIGH to tags.filter { 
                it.name.contains("crane", true) || 
                it.name.contains("scaffold", true) ||
                it.name.contains("missing", true) ||
                it.name.contains("no safety", true)
            },
            HazardSeverity.MEDIUM to tags.filter { 
                it.name.contains("housekeeping", true) || 
                it.name.contains("ladder", true) ||
                it.name.contains("dust", true) ||
                it.name.contains("gloves", true)
            },
            HazardSeverity.LOW to tags.filter { 
                it.name.contains("tool", true) || 
                it.name.contains("training", true)
            }
        ).filterValues { it.isNotEmpty() }
    }
    
    private fun generateRecommendations(tags: List<UITag>): List<UITagRecommendation> {
        // In a real app, use AI or analytics to generate recommendations
        return tags.take(6).mapIndexed { index, tag ->
            UITagRecommendation(
                tag = tag.copy(usageCount = (5..20).random()),
                score = (0.5f + index * 0.08f).coerceAtMost(0.9f),
                reason = when (index % 4) {
                    0 -> RecommendationReason.AI_DETECTED
                    1 -> RecommendationReason.SIMILAR_PHOTOS
                    2 -> RecommendationReason.PROJECT_REQUIREMENTS
                    else -> RecommendationReason.BEST_PRACTICE
                },
                usageContext = when (index % 4) {
                    0 -> "AI detected this hazard"
                    1 -> "Common in similar photos"
                    2 -> "Frequent on this project"
                    else -> "Industry best practice"
                }
            )
        }
    }
    
    private fun getRecentTags(tags: List<UITag>): List<UITag> {
        // In a real app, get from usage history
        return tags.takeLast(5).map { tag ->
            tag.copy(
                usageCount = (10..50).random(),
                lastUsed = System.currentTimeMillis() - (1..7).random() * 24 * 60 * 60 * 1000,
                isRecentlyUsed = true
            )
        }
    }
    
    private fun getCategoryColor(category: TagCategory): Color {
        return when (category) {
            TagCategory.PPE -> Color(0xFF2196F3)
            TagCategory.FALL_PROTECTION -> Color(0xFFFF5722)
            TagCategory.ELECTRICAL_SAFETY -> Color(0xFFFFEB3B)
            TagCategory.CHEMICAL_SAFETY -> Color(0xFF9C27B0)
            TagCategory.FIRE_SAFETY -> Color(0xFFFF5722)
            TagCategory.EQUIPMENT_SAFETY -> Color(0xFF9C27B0)
            TagCategory.HOUSEKEEPING -> Color(0xFF4CAF50)
            TagCategory.HOT_WORK -> Color(0xFFE91E63)
            TagCategory.CRANE_LIFTING -> Color(0xFF9C27B0)
            TagCategory.CONFINED_SPACE -> Color(0xFF795548)
            TagCategory.ERGONOMICS -> Color(0xFF009688)
            TagCategory.ENVIRONMENTAL -> Color(0xFF4CAF50)
            TagCategory.GENERAL_SAFETY -> Color(0xFFFFC107)
            TagCategory.EMERGENCY_PROCEDURES -> Color(0xFFFF5722)
            TagCategory.TRAINING_COMMUNICATION -> Color(0xFF607D8B)
        }
    }
    
    private suspend fun saveTagSelection(tags: List<String>, isCompliant: Boolean) {
        // In a real app, save to database
        delay(100)
        println("Saved ${tags.size} tags with compliance: $isCompliant")
    }
    
    private suspend fun saveCustomTagToDatabase(tag: UITag) {
        // In a real app, save to database
        delay(100)
        println("Saved custom tag: ${tag.name}")
    }
}