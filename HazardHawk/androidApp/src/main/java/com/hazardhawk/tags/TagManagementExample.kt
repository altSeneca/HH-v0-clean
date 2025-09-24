package com.hazardhawk.tags

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.models.TagCategory
import com.hazardhawk.tags.models.*
import com.hazardhawk.ui.theme.HazardHawkTheme
import com.hazardhawk.models.RecommendationReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Example usage of the smart tag management system.
 * This demonstrates how to integrate the TagSelectionDialog with a ViewModel.
 */

@Composable
fun TagManagementExample() {
    val viewModel = remember { TagManagementExampleViewModel() }
    val tagSelectionState by viewModel.tagSelectionState.collectAsState()
    var showTagDialog by remember { mutableStateOf(false) }
    
    HazardHawkTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "HazardHawk Smart Tag Management",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Text(
                    text = "Tap the button to test the tag selection dialog",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Button(
                    onClick = {
                        viewModel.startTagSelection()
                        showTagDialog = true
                    }
                ) {
                    Text("Open Tag Selection")
                }
                
                // Show current state info
                Card(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Current State:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "Selected tags: ${tagSelectionState.selectedTags.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Compliant: ${tagSelectionState.isCompliant}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Quick tags available: ${tagSelectionState.quickTags.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Recommendations: ${tagSelectionState.recommendedTags.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Tag Selection Dialog
            if (showTagDialog) {
                TagSelectionDialog(
                    state = tagSelectionState,
                    onComplianceToggle = viewModel::updateComplianceStatus,
                    onTagToggle = viewModel::toggleTagSelection,
                    onSearch = viewModel::searchTags,
                    onCreateCustomTag = viewModel::createCustomTag,
                    onConfirm = {
                        viewModel.applyTags()
                        showTagDialog = false
                    },
                    onDismiss = {
                        showTagDialog = false
                    }
                )
            }
        }
    }
}

/**
 * Example ViewModel demonstrating integration with the tag management system
 */
class TagManagementExampleViewModel : ViewModel() {
    
    private val _tagSelectionState = MutableStateFlow(TagSelectionState())
    val tagSelectionState = _tagSelectionState
    
    init {
        // Initialize with mock data
        updateMockState()
    }
    
    fun startTagSelection() {
        // In a real app, this would call the repository
        updateMockState()
    }
    
    fun updateComplianceStatus(isCompliant: Boolean) {
        _tagSelectionState.value = _tagSelectionState.value.copy(
            isCompliant = isCompliant
        )
    }
    
    fun toggleTagSelection(tagId: String) {
        val currentState = _tagSelectionState.value
        val newSelectedTags = if (tagId in currentState.selectedTags) {
            currentState.selectedTags - tagId
        } else {
            currentState.selectedTags + tagId
        }
        
        _tagSelectionState.value = currentState.copy(
            selectedTags = newSelectedTags
        )
    }
    
    fun searchTags(query: String) {
        // Mock search implementation
        val allTags = getDefaultQuickTags()
        val filteredTags = if (query.isBlank()) {
            emptyList()
        } else {
            allTags.filter { it.name.contains(query, ignoreCase = true) }
        }
        
        _tagSelectionState.value = _tagSelectionState.value.copy(
            searchQuery = query,
            searchResults = filteredTags
        )
    }
    
    fun createCustomTag(name: String, category: TagCategory) {
        // Mock custom tag creation
        println("Creating custom tag: $name in category: $category")
    }
    
    fun applyTags() {
        val state = _tagSelectionState.value
        println("Applying ${state.selectedTags.size} tags, compliant: ${state.isCompliant}")
        
        // Reset state after applying
        _tagSelectionState.value = TagSelectionState()
    }
    
    private fun updateMockState() {
        _tagSelectionState.value = TagSelectionState(
            quickTags = getDefaultQuickTags().take(8),
            recommendedTags = getMockRecommendations(),
            recentTags = getDefaultQuickTags().takeLast(5),
            isLoading = false
        )
    }
    
    private fun getMockRecommendations(): List<UITagRecommendation> {
        return getDefaultQuickTags().take(6).mapIndexed { index, tag ->
            UITagRecommendation(
                tag = tag,
                score = (0.5f + index * 0.05f).coerceAtMost(0.9f),
                reason = when (index % 4) {
                    0 -> RecommendationReason.USER_HISTORY
                    1 -> RecommendationReason.PROJECT_REQUIREMENTS
                    2 -> RecommendationReason.BEST_PRACTICE
                    else -> RecommendationReason.SIMILAR_PHOTOS
                },
                usageContext = when (index % 4) {
                    0 -> "You've used this ${5 + index} times"
                    1 -> "Popular in this project"
                    2 -> "Industry standard"
                    else -> "Recently used"
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TagManagementExamplePreview() {
    TagManagementExample()
}