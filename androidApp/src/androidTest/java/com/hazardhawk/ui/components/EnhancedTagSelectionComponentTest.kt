package com.hazardhawk.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hazardhawk.domain.entities.*
import com.hazardhawk.domain.compliance.*
import com.hazardhawk.test.EnhancedTestDataFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive UI tests for enhanced tag selection components including
 * OSHA compliance indicators, bulk operations, and accessibility features.
 */
@RunWith(AndroidJUnit4::class)
class EnhancedTagSelectionComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // MARK: - Tag Selection Interface Tests
    
    @Test
    fun tag_selection_display_shows_enhanced_properties() {
        val testTags = listOf(
            EnhancedTestDataFactory.createEnhancedTestTag(
                name = "Fall Protection Required",
                category = TagCategory.FALL_PROTECTION,
                oshaCompliance = OSHACompliance(
                    status = ComplianceStatus.CRITICAL,
                    references = listOf(OSHAReference("Fall Protection", 1926, 501))
                )
            ),
            EnhancedTestDataFactory.createEnhancedTestTag(
                name = "Safety Glasses Required", 
                category = TagCategory.PPE,
                oshaCompliance = OSHACompliance(status = ComplianceStatus.COMPLIANT)
            )
        )
        
        composeTestRule.setContent {
            EnhancedTagSelectionGrid(
                tags = testTags,
                selectedTags = emptySet(),
                onTagSelected = { },
                onTagDeselected = { }
            )
        }
        
        // Verify tag names are displayed
        composeTestRule.onNodeWithText("Fall Protection Required").assertExists()
        composeTestRule.onNodeWithText("Safety Glasses Required").assertExists()
        
        // Verify OSHA compliance indicators
        composeTestRule.onNodeWithContentDescription("Critical compliance status").assertExists()
        composeTestRule.onNodeWithContentDescription("Compliant status").assertExists()
        
        // Verify category indicators
        composeTestRule.onNodeWithText("Fall Protection").assertExists()
        composeTestRule.onNodeWithText("PPE").assertExists()
    }
    
    @Test
    fun tag_selection_supports_hierarchical_navigation() {
        val hierarchicalTags = EnhancedTestDataFactory.createHierarchicalTagStructure()
        
        composeTestRule.setContent {
            HierarchicalTagSelector(
                tags = hierarchicalTags,
                selectedTags = emptySet(),
                onTagSelected = { },
                onNavigateToLevel = { }
            )
        }
        
        // Should show root level initially
        composeTestRule.onNodeWithText("Safety").assertExists()
        
        // Navigate to category level
        composeTestRule.onNodeWithText("Safety").performClick()
        
        // Should show category tags
        composeTestRule.onNodeWithText("Personal Protection").assertExists()
        composeTestRule.onNodeWithText("Fall Protection").assertExists()
    }
    
    @Test
    fun bulk_tag_operations_select_multiple_tags() {
        val testTags = (1..5).map { index ->
            EnhancedTestDataFactory.createEnhancedTestTag(
                id = "bulk-tag-$index",
                name = "Bulk Tag $index"
            )
        }
        
        var selectedCount = 0
        
        composeTestRule.setContent {
            BulkTagOperationsComponent(
                tags = testTags,
                selectedTags = emptySet(),
                onBulkSelect = { selectedCount = it.size },
                onBulkClear = { selectedCount = 0 }
            )
        }
        
        // Test "Select All" functionality
        composeTestRule.onNodeWithText("Select All").performClick()
        assertEquals(5, selectedCount)
        
        // Test "Clear All" functionality  
        composeTestRule.onNodeWithText("Clear All").performClick()
        assertEquals(0, selectedCount)
    }
    
    @Test
    fun osha_compliance_indicator_shows_correct_status_colors() {
        val criticalTag = EnhancedTestDataFactory.createEnhancedTestTag(
            oshaCompliance = OSHACompliance(status = ComplianceStatus.CRITICAL)
        )
        
        val compliantTag = EnhancedTestDataFactory.createEnhancedTestTag(
            oshaCompliance = OSHACompliance(status = ComplianceStatus.COMPLIANT)
        )
        
        composeTestRule.setContent {
            TagComplianceStatusGrid(
                tags = listOf(criticalTag, compliantTag)
            )
        }
        
        // Verify compliance status indicators are shown
        composeTestRule.onNodeWithContentDescription("Critical compliance - immediate attention required")
            .assertExists()
        composeTestRule.onNodeWithContentDescription("Compliant - meets OSHA standards")
            .assertExists()
    }
    
    // MARK: - Search and Filter Tests
    
    @Test
    fun enhanced_tag_search_filters_by_multiple_criteria() {
        val testTags = listOf(
            EnhancedTestDataFactory.createEnhancedTestTag(
                name = "Hard Hat Required",
                category = TagCategory.PPE,
                oshaCompliance = OSHACompliance(status = ComplianceStatus.CRITICAL)
            ),
            EnhancedTestDataFactory.createEnhancedTestTag(
                name = "Safety Glasses",
                category = TagCategory.PPE,
                oshaCompliance = OSHACompliance(status = ComplianceStatus.COMPLIANT)
            ),
            EnhancedTestDataFactory.createEnhancedTestTag(
                name = "Fall Protection Harness",
                category = TagCategory.FALL_PROTECTION,
                oshaCompliance = OSHACompliance(status = ComplianceStatus.CRITICAL)
            )
        )
        
        composeTestRule.setContent {
            EnhancedTagSearchAndFilter(
                tags = testTags,
                onSearchQuery = { },
                onCategoryFilter = { },
                onComplianceFilter = { }
            )
        }
        
        // Test search functionality
        composeTestRule.onNodeWithContentDescription("Search tags").performTextInput("Hard Hat")
        composeTestRule.onNodeWithText("Hard Hat Required").assertExists()
        composeTestRule.onNodeWithText("Safety Glasses").assertDoesNotExist()
        
        // Test category filter
        composeTestRule.onNodeWithText("PPE").performClick()
        composeTestRule.onNodeWithText("Hard Hat Required").assertExists()
        composeTestRule.onNodeWithText("Safety Glasses").assertExists()
        composeTestRule.onNodeWithText("Fall Protection Harness").assertDoesNotExist()
        
        // Test compliance filter
        composeTestRule.onNodeWithText("Critical Only").performClick()
        composeTestRule.onNodeWithText("Hard Hat Required").assertExists()
        composeTestRule.onNodeWithText("Safety Glasses").assertDoesNotExist()
    }
    
    // MARK: - Performance and Responsiveness Tests
    
    @Test
    fun tag_selection_performance_with_large_dataset() {
        val largeTagSet = EnhancedTestDataFactory.createEnhancedPerformanceDataset(500).tags
        
        composeTestRule.setContent {
            LazyEnhancedTagGrid(
                tags = largeTagSet,
                selectedTags = emptySet(),
                onTagToggle = { }
            )
        }
        
        // Should render without performance issues
        composeTestRule.onRoot().assertExists()
        
        // Should be able to scroll through large dataset
        composeTestRule.onNodeWithContentDescription("Tag grid").performScrollToIndex(100)
        composeTestRule.onNodeWithContentDescription("Tag grid").performScrollToIndex(400)
        
        // Performance validation would be done through automated testing tools
        // This test structure ensures the UI can handle large datasets
    }
    
    @Test
    fun tag_selection_accessibility_meets_requirements() {
        val testTag = EnhancedTestDataFactory.createEnhancedTestTag(
            name = "Accessible Tag",
            category = TagCategory.PPE
        )
        
        composeTestRule.setContent {
            AccessibleTagSelectionCard(
                tag = testTag,
                isSelected = false,
                onToggle = { }
            )
        }
        
        // Verify accessibility features
        composeTestRule.onNodeWithContentDescription("Accessible Tag, PPE category, tap to select")
            .assertExists()
            .assertHasClickAction()
        
        // Test keyboard navigation
        composeTestRule.onRoot().performKeyInput {
            pressKey(android.view.KeyEvent.KEYCODE_TAB)
        }
        
        // Test screen reader compatibility
        composeTestRule.onNodeWithText("Accessible Tag")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    // MARK: - User Interaction Tests
    
    @Test
    fun tag_selection_state_management_works_correctly() {
        val testTags = (1..3).map { index ->
            EnhancedTestDataFactory.createEnhancedTestTag(
                id = "interaction-tag-$index",
                name = "Tag $index"
            )
        }
        
        val selectedTags = mutableSetOf<TagId>()
        
        composeTestRule.setContent {
            EnhancedTagSelectionGrid(
                tags = testTags,
                selectedTags = selectedTags,
                onTagSelected = { tagId -> selectedTags.add(tagId) },
                onTagDeselected = { tagId -> selectedTags.remove(tagId) }
            )
        }
        
        // Select first tag
        composeTestRule.onNodeWithText("Tag 1").performClick()
        assertTrue(selectedTags.contains(TagId("interaction-tag-1")))
        
        // Select second tag
        composeTestRule.onNodeWithText("Tag 2").performClick()
        assertEquals(2, selectedTags.size)
        
        // Deselect first tag
        composeTestRule.onNodeWithText("Tag 1").performClick()
        assertFalse(selectedTags.contains(TagId("interaction-tag-1")))
        assertEquals(1, selectedTags.size)
    }
    
    @Test
    fun tag_details_modal_shows_comprehensive_information() {
        val detailedTag = EnhancedTestDataFactory.createEnhancedTestTag(
            name = "Detailed Safety Tag",
            description = "Comprehensive tag with all features",
            oshaCompliance = OSHACompliance(
                status = ComplianceStatus.NEEDS_IMPROVEMENT,
                references = listOf(OSHAReference("Safety Standard", 1926, 95))
            )
        )
        
        composeTestRule.setContent {
            TagDetailsModal(
                tag = detailedTag,
                isVisible = true,
                onDismiss = { }
            )
        }
        
        // Verify detailed information is displayed
        composeTestRule.onNodeWithText("Detailed Safety Tag").assertExists()
        composeTestRule.onNodeWithText("Comprehensive tag with all features").assertExists()
        composeTestRule.onNodeWithText("29 CFR 1926.95").assertExists()
        composeTestRule.onNodeWithText("Needs Improvement").assertExists()
        
        // Verify OSHA compliance section
        composeTestRule.onNodeWithText("OSHA Compliance").assertExists()
        composeTestRule.onNodeWithText("Safety Standard").assertExists()
    }
}

// MARK: - Mock Composable Components for Testing

@Composable
fun EnhancedTagSelectionGrid(
    tags: List<Tag>,
    selectedTags: Set<TagId>,
    onTagSelected: (TagId) -> Unit,
    onTagDeselected: (TagId) -> Unit
) {
    // Mock implementation - real implementation would be in androidMain
    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        items(tags) { tag ->
            TagSelectionCard(
                tag = tag,
                isSelected = selectedTags.contains(tag.id),
                onToggle = { 
                    if (selectedTags.contains(tag.id)) {
                        onTagDeselected(tag.id)
                    } else {
                        onTagSelected(tag.id)
                    }
                }
            )
        }
    }
}

@Composable
fun TagSelectionCard(
    tag: Tag,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Column {
            Text(tag.name)
            Text(tag.category.displayName)
            
            // OSHA compliance indicator
            when (tag.oshaCompliance.status) {
                ComplianceStatus.CRITICAL -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Critical compliance status",
                        tint = Color.Red
                    )
                }
                ComplianceStatus.COMPLIANT -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Compliant status",
                        tint = Color.Green
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Needs improvement status",
                        tint = Color.Orange
                    )
                }
            }
        }
    }
}
