package com.hazardhawk.tag.ui

import com.hazardhawk.core.models.Tag
import com.hazardhawk.test.TestDataFactory
import com.hazardhawk.ui.components.tag.TagEditState
import com.hazardhawk.ui.components.tag.TagCatalogViewModel
import com.hazardhawk.ui.components.tag.TagEditAction
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Comprehensive UI component behavior tests for tag catalog edit mode functionality.
 * Tests user interactions, state management, validation, and error handling in edit mode.
 * Covers all edit operations: create, update, delete, bulk operations, and search.
 */
class TagCatalogEditModeUITest {
    
    private lateinit var mockViewModel: TagCatalogViewModel
    private lateinit var editStateFlow: MutableStateFlow<TagEditState>
    
    @BeforeTest
    fun setup() {
        mockViewModel = mockk(relaxed = true)
        editStateFlow = MutableStateFlow(TagEditState.Idle)
        
        every { mockViewModel.editState } returns editStateFlow
        every { mockViewModel.tags } returns MutableStateFlow(TestDataFactory.createPersonalTopTags())
    }
    
    @AfterTest
    fun teardown() {
        clearAllMocks()
    }
    
    // MARK: - Edit Mode Activation Tests
    
    @Test
    fun `entering edit mode should update UI state correctly`() = runTest {
        // Given - Initial idle state
        assertEquals(TagEditState.Idle, editStateFlow.value)
        
        // When - Enter edit mode
        every { mockViewModel.enterEditMode() } answers {
            editStateFlow.value = TagEditState.EditingMultiple(emptySet())
        }
        mockViewModel.enterEditMode()
        
        // Then - Should be in editing state
        assertTrue(editStateFlow.value is TagEditState.EditingMultiple)
        verify { mockViewModel.enterEditMode() }
    }
    
    @Test
    fun `exiting edit mode should reset selection and UI state`() = runTest {
        // Given - Currently in edit mode with selections
        val selectedTags = setOf("tag-1", "tag-2")
        editStateFlow.value = TagEditState.EditingMultiple(selectedTags)
        
        // When - Exit edit mode
        every { mockViewModel.exitEditMode() } answers {
            editStateFlow.value = TagEditState.Idle
        }
        mockViewModel.exitEditMode()
        
        // Then - Should return to idle state
        assertEquals(TagEditState.Idle, editStateFlow.value)
        verify { mockViewModel.exitEditMode() }
    }
    
    @Test
    fun `edit mode should preserve tag selections during state changes`() = runTest {
        // Given - Tags selected in edit mode
        val selectedTags = setOf("tag-1", "tag-2", "tag-3")
        editStateFlow.value = TagEditState.EditingMultiple(selectedTags)
        
        // When - State transitions but stays in edit mode
        editStateFlow.value = TagEditState.EditingMultiple(selectedTags.plus("tag-4"))
        
        // Then - Selection should be updated correctly
        val currentState = editStateFlow.value as TagEditState.EditingMultiple
        assertEquals(4, currentState.selectedTagIds.size)
        assertTrue(currentState.selectedTagIds.contains("tag-4"))
    }
    
    // MARK: - Tag Selection Tests
    
    @Test
    fun `selecting tag should add to selection set`() = runTest {
        // Given - In edit mode with no selections
        editStateFlow.value = TagEditState.EditingMultiple(emptySet())
        
        val tagToSelect = "tag-1"
        
        // When - Select tag
        every { mockViewModel.toggleTagSelection(tagToSelect) } answers {
            val currentState = editStateFlow.value as TagEditState.EditingMultiple
            editStateFlow.value = currentState.copy(selectedTagIds = currentState.selectedTagIds + tagToSelect)
        }
        mockViewModel.toggleTagSelection(tagToSelect)
        
        // Then - Tag should be selected
        val newState = editStateFlow.value as TagEditState.EditingMultiple
        assertTrue(newState.selectedTagIds.contains(tagToSelect))
        assertEquals(1, newState.selectedTagIds.size)
    }
    
    @Test
    fun `deselecting tag should remove from selection set`() = runTest {
        // Given - In edit mode with tag selected
        val initialSelection = setOf("tag-1", "tag-2")
        editStateFlow.value = TagEditState.EditingMultiple(initialSelection)
        
        val tagToDeselect = "tag-1"
        
        // When - Deselect tag
        every { mockViewModel.toggleTagSelection(tagToDeselect) } answers {
            val currentState = editStateFlow.value as TagEditState.EditingMultiple
            editStateFlow.value = currentState.copy(selectedTagIds = currentState.selectedTagIds - tagToDeselect)
        }
        mockViewModel.toggleTagSelection(tagToDeselect)
        
        // Then - Tag should be deselected
        val newState = editStateFlow.value as TagEditState.EditingMultiple
        assertFalse(newState.selectedTagIds.contains(tagToDeselect))
        assertEquals(1, newState.selectedTagIds.size)
        assertTrue(newState.selectedTagIds.contains("tag-2"))
    }
    
    @Test
    fun `select all should select all visible tags`() = runTest {
        // Given - Tags available and in edit mode
        val availableTags = TestDataFactory.createPersonalTopTags()
        every { mockViewModel.tags.value } returns availableTags
        editStateFlow.value = TagEditState.EditingMultiple(emptySet())
        
        // When - Select all
        every { mockViewModel.selectAllTags() } answers {
            editStateFlow.value = TagEditState.EditingMultiple(availableTags.map { it.id }.toSet())
        }
        mockViewModel.selectAllTags()
        
        // Then - All tags should be selected
        val newState = editStateFlow.value as TagEditState.EditingMultiple
        assertEquals(availableTags.size, newState.selectedTagIds.size)
        availableTags.forEach { tag ->
            assertTrue(newState.selectedTagIds.contains(tag.id))
        }
    }
    
    @Test
    fun `clear selection should deselect all tags`() = runTest {
        // Given - Multiple tags selected
        val selectedTags = setOf("tag-1", "tag-2", "tag-3")
        editStateFlow.value = TagEditState.EditingMultiple(selectedTags)
        
        // When - Clear selection
        every { mockViewModel.clearSelection() } answers {
            editStateFlow.value = TagEditState.EditingMultiple(emptySet())
        }
        mockViewModel.clearSelection()
        
        // Then - No tags should be selected
        val newState = editStateFlow.value as TagEditState.EditingMultiple
        assertTrue(newState.selectedTagIds.isEmpty())
    }
    
    // MARK: - Single Tag Edit Tests
    
    @Test
    fun `editing single tag should enter single edit mode`() = runTest {
        // Given - A tag to edit
        val tagToEdit = TestDataFactory.createTestTag(id = "edit-tag", name = "Original Name")
        
        // When - Start editing single tag
        every { mockViewModel.startEditingTag(tagToEdit.id) } answers {
            editStateFlow.value = TagEditState.EditingSingle(tagToEdit)
        }
        mockViewModel.startEditingTag(tagToEdit.id)
        
        // Then - Should be in single edit mode
        val newState = editStateFlow.value as TagEditState.EditingSingle
        assertEquals(tagToEdit.id, newState.tag.id)
        assertEquals(tagToEdit.name, newState.tag.name)
    }
    
    @Test
    fun `saving single tag edit should validate input`() = runTest {
        // Given - Editing a tag
        val originalTag = TestDataFactory.createTestTag(id = "edit-tag")
        editStateFlow.value = TagEditState.EditingSingle(originalTag)
        
        // When - Save with invalid data (empty name)
        val invalidUpdate = originalTag.copy(name = "")
        every { mockViewModel.saveTagEdit(invalidUpdate) } answers {
            editStateFlow.value = TagEditState.Error("Tag name cannot be empty")
        }
        mockViewModel.saveTagEdit(invalidUpdate)
        
        // Then - Should show validation error
        val errorState = editStateFlow.value as TagEditState.Error
        assertTrue(errorState.message.contains("empty"))
    }
    
    @Test
    fun `saving valid single tag edit should update tag and exit edit mode`() = runTest {
        // Given - Editing a tag
        val originalTag = TestDataFactory.createTestTag(id = "edit-tag", name = "Original")
        editStateFlow.value = TagEditState.EditingSingle(originalTag)
        
        // When - Save with valid data
        val validUpdate = originalTag.copy(name = "Updated Name")
        every { mockViewModel.saveTagEdit(validUpdate) } answers {
            editStateFlow.value = TagEditState.Saving
            // Simulate successful save
            editStateFlow.value = TagEditState.Idle
        }
        mockViewModel.saveTagEdit(validUpdate)
        
        // Then - Should return to idle state
        assertEquals(TagEditState.Idle, editStateFlow.value)
        verify { mockViewModel.saveTagEdit(validUpdate) }
    }
    
    @Test
    fun `canceling single tag edit should discard changes and exit edit mode`() = runTest {
        // Given - Editing a tag
        val originalTag = TestDataFactory.createTestTag(id = "edit-tag")
        editStateFlow.value = TagEditState.EditingSingle(originalTag)
        
        // When - Cancel edit
        every { mockViewModel.cancelTagEdit() } answers {
            editStateFlow.value = TagEditState.Idle
        }
        mockViewModel.cancelTagEdit()
        
        // Then - Should return to idle state without saving
        assertEquals(TagEditState.Idle, editStateFlow.value)
        verify { mockViewModel.cancelTagEdit() }
    }
    
    // MARK: - Bulk Operations Tests
    
    @Test
    fun `bulk delete should confirm before deletion`() = runTest {
        // Given - Multiple tags selected
        val selectedTags = setOf("tag-1", "tag-2", "tag-3")
        editStateFlow.value = TagEditState.EditingMultiple(selectedTags)
        
        // When - Initiate bulk delete
        every { mockViewModel.initiateBulkDelete() } answers {
            editStateFlow.value = TagEditState.ConfirmingBulkDelete(selectedTags)
        }
        mockViewModel.initiateBulkDelete()
        
        // Then - Should be in confirmation state
        val confirmState = editStateFlow.value as TagEditState.ConfirmingBulkDelete
        assertEquals(selectedTags, confirmState.tagIds)
    }
    
    @Test
    fun `confirming bulk delete should delete selected tags`() = runTest {
        // Given - In bulk delete confirmation
        val selectedTags = setOf("tag-1", "tag-2")
        editStateFlow.value = TagEditState.ConfirmingBulkDelete(selectedTags)
        
        // When - Confirm deletion
        every { mockViewModel.confirmBulkDelete() } answers {
            editStateFlow.value = TagEditState.DeletingMultiple(selectedTags)
            // Simulate successful deletion
            editStateFlow.value = TagEditState.Idle
        }
        mockViewModel.confirmBulkDelete()
        
        // Then - Should complete deletion and return to idle
        assertEquals(TagEditState.Idle, editStateFlow.value)
        verify { mockViewModel.confirmBulkDelete() }
    }
    
    @Test
    fun `canceling bulk delete should return to edit mode`() = runTest {
        // Given - In bulk delete confirmation
        val selectedTags = setOf("tag-1", "tag-2")
        editStateFlow.value = TagEditState.ConfirmingBulkDelete(selectedTags)
        
        // When - Cancel deletion
        every { mockViewModel.cancelBulkDelete() } answers {
            editStateFlow.value = TagEditState.EditingMultiple(selectedTags)
        }
        mockViewModel.cancelBulkDelete()
        
        // Then - Should return to editing state with selection preserved
        val editState = editStateFlow.value as TagEditState.EditingMultiple
        assertEquals(selectedTags, editState.selectedTagIds)
    }
    
    @Test
    fun `bulk category update should apply to all selected tags`() = runTest {
        // Given - Multiple tags selected
        val selectedTags = setOf("tag-1", "tag-2", "tag-3")
        editStateFlow.value = TagEditState.EditingMultiple(selectedTags)
        
        val newCategory = "Updated Category"
        
        // When - Apply bulk category update
        every { mockViewModel.bulkUpdateCategory(newCategory) } answers {
            editStateFlow.value = TagEditState.UpdatingMultiple(selectedTags)
            // Simulate successful update
            editStateFlow.value = TagEditState.Idle
        }
        mockViewModel.bulkUpdateCategory(newCategory)
        
        // Then - Should complete update and return to idle
        assertEquals(TagEditState.Idle, editStateFlow.value)
        verify { mockViewModel.bulkUpdateCategory(newCategory) }
    }
    
    // MARK: - Search and Filter in Edit Mode Tests
    
    @Test
    fun `search in edit mode should filter tags while preserving selection`() = runTest {
        // Given - In edit mode with some tags selected
        val selectedTags = setOf("tag-1", "tag-2")
        editStateFlow.value = TagEditState.EditingMultiple(selectedTags)
        
        val searchQuery = "safety"
        val filteredTags = TestDataFactory.createPersonalTopTags().filter { 
            it.name.contains(searchQuery, ignoreCase = true)
        }
        
        // When - Search while in edit mode
        every { mockViewModel.searchTags(searchQuery) } answers {
            every { mockViewModel.tags.value } returns filteredTags
            // Selection should be preserved
            editStateFlow.value = TagEditState.EditingMultiple(selectedTags)
        }
        mockViewModel.searchTags(searchQuery)
        
        // Then - Selection should be preserved even with filtered results
        val editState = editStateFlow.value as TagEditState.EditingMultiple
        assertEquals(selectedTags, editState.selectedTagIds)
        verify { mockViewModel.searchTags(searchQuery) }
    }
    
    @Test
    fun `clearing search in edit mode should restore full tag list`() = runTest {
        // Given - In edit mode with search applied
        val selectedTags = setOf("tag-1")
        editStateFlow.value = TagEditState.EditingMultiple(selectedTags)
        
        val fullTagList = TestDataFactory.createPersonalTopTags()
        
        // When - Clear search
        every { mockViewModel.clearSearch() } answers {
            every { mockViewModel.tags.value } returns fullTagList
            editStateFlow.value = TagEditState.EditingMultiple(selectedTags)
        }
        mockViewModel.clearSearch()
        
        // Then - Full list should be restored with selection intact
        assertEquals(fullTagList.size, mockViewModel.tags.value.size)
        val editState = editStateFlow.value as TagEditState.EditingMultiple
        assertEquals(selectedTags, editState.selectedTagIds)
    }
    
    // MARK: - Error Handling Tests
    
    @Test
    fun `network error during save should show error state`() = runTest {
        // Given - Saving a tag edit
        val tag = TestDataFactory.createTestTag(id = "save-error-test")
        editStateFlow.value = TagEditState.Saving
        
        // When - Network error occurs
        val errorMessage = "Network connection failed"
        every { mockViewModel.handleSaveError(any()) } answers {
            editStateFlow.value = TagEditState.Error(errorMessage)
        }
        mockViewModel.handleSaveError(RuntimeException(errorMessage))
        
        // Then - Should display error state
        val errorState = editStateFlow.value as TagEditState.Error
        assertEquals(errorMessage, errorState.message)
    }
    
    @Test
    fun `retrying failed operation should restore previous state`() = runTest {
        // Given - In error state from failed save
        val originalTag = TestDataFactory.createTestTag()
        editStateFlow.value = TagEditState.Error("Save failed")
        
        // When - Retry operation
        every { mockViewModel.retryLastOperation() } answers {
            editStateFlow.value = TagEditState.EditingSingle(originalTag)
        }
        mockViewModel.retryLastOperation()
        
        // Then - Should return to editing state
        val editState = editStateFlow.value as TagEditState.EditingSingle
        assertEquals(originalTag.id, editState.tag.id)
    }
    
    @Test
    fun `dismissing error should return to appropriate state`() = runTest {
        // Given - In error state
        editStateFlow.value = TagEditState.Error("Some error occurred")
        
        // When - Dismiss error
        every { mockViewModel.dismissError() } answers {
            editStateFlow.value = TagEditState.Idle
        }
        mockViewModel.dismissError()
        
        // Then - Should return to idle state
        assertEquals(TagEditState.Idle, editStateFlow.value)
    }
    
    // MARK: - Loading State Tests
    
    @Test
    fun `long-running operations should show loading state`() = runTest {
        // Given - Starting a bulk operation
        val selectedTags = setOf("tag-1", "tag-2", "tag-3")
        editStateFlow.value = TagEditState.EditingMultiple(selectedTags)
        
        // When - Start bulk delete (long operation)
        every { mockViewModel.performBulkDelete() } answers {
            editStateFlow.value = TagEditState.DeletingMultiple(selectedTags)
        }
        mockViewModel.performBulkDelete()
        
        // Then - Should show deleting state
        val deletingState = editStateFlow.value as TagEditState.DeletingMultiple
        assertEquals(selectedTags, deletingState.tagIds)
    }
    
    @Test
    fun `loading states should prevent additional user interactions`() = runTest {
        // Given - In loading state
        editStateFlow.value = TagEditState.Saving
        
        // When - Try to perform another action
        every { mockViewModel.isOperationInProgress() } returns true
        
        // Then - Should indicate operation in progress
        assertTrue(mockViewModel.isOperationInProgress())
        
        // Additional operations should be blocked
        every { mockViewModel.canPerformAction() } returns false
        assertFalse(mockViewModel.canPerformAction())
    }
    
    // MARK: - Tag Creation Tests
    
    @Test
    fun `creating new tag should enter creation mode`() = runTest {
        // Given - In idle state
        editStateFlow.value = TagEditState.Idle
        
        // When - Start creating new tag
        every { mockViewModel.startCreatingTag() } answers {
            val newTag = TestDataFactory.createTestTag(id = "", name = "")
            editStateFlow.value = TagEditState.CreatingNew(newTag)
        }
        mockViewModel.startCreatingTag()
        
        // Then - Should be in creation mode
        assertTrue(editStateFlow.value is TagEditState.CreatingNew)
        val creationState = editStateFlow.value as TagEditState.CreatingNew
        assertTrue(creationState.tag.id.isEmpty())
        assertTrue(creationState.tag.name.isEmpty())
    }
    
    @Test
    fun `saving new tag should validate required fields`() = runTest {
        // Given - Creating new tag
        val newTag = TestDataFactory.createTestTag(id = "", name = "")
        editStateFlow.value = TagEditState.CreatingNew(newTag)
        
        // When - Try to save without name
        val invalidTag = newTag.copy(name = "")
        every { mockViewModel.saveNewTag(invalidTag) } answers {
            editStateFlow.value = TagEditState.Error("Tag name is required")
        }
        mockViewModel.saveNewTag(invalidTag)
        
        // Then - Should show validation error
        val errorState = editStateFlow.value as TagEditState.Error
        assertTrue(errorState.message.contains("required"))
    }
    
    @Test
    fun `successfully creating new tag should add to catalog and exit creation mode`() = runTest {
        // Given - Creating new tag with valid data
        val newTag = TestDataFactory.createTestTag(id = "", name = "New Safety Tag")
        editStateFlow.value = TagEditState.CreatingNew(newTag)
        
        // When - Save valid new tag
        val validTag = newTag.copy(name = "New Safety Tag", category = "Safety")
        every { mockViewModel.saveNewTag(validTag) } answers {
            editStateFlow.value = TagEditState.Saving
            // Simulate successful creation
            editStateFlow.value = TagEditState.Idle
        }
        mockViewModel.saveNewTag(validTag)
        
        // Then - Should return to idle after successful creation
        assertEquals(TagEditState.Idle, editStateFlow.value)
        verify { mockViewModel.saveNewTag(validTag) }
    }
}

/**
 * Extended tag edit states for comprehensive testing
 */
sealed class TagEditState {
    object Idle : TagEditState()
    object Saving : TagEditState()
    data class EditingSingle(val tag: Tag) : TagEditState()
    data class EditingMultiple(val selectedTagIds: Set<String>) : TagEditState()
    data class CreatingNew(val tag: Tag) : TagEditState()
    data class ConfirmingBulkDelete(val tagIds: Set<String>) : TagEditState()
    data class DeletingMultiple(val tagIds: Set<String>) : TagEditState()
    data class UpdatingMultiple(val tagIds: Set<String>) : TagEditState()
    data class Error(val message: String) : TagEditState()
}

/**
 * Mock tag catalog view model for testing
 */
abstract class TagCatalogViewModel {
    abstract val editState: StateFlow<TagEditState>
    abstract val tags: StateFlow<List<Tag>>
    
    abstract fun enterEditMode()
    abstract fun exitEditMode()
    abstract fun toggleTagSelection(tagId: String)
    abstract fun selectAllTags()
    abstract fun clearSelection()
    abstract fun startEditingTag(tagId: String)
    abstract fun saveTagEdit(tag: Tag)
    abstract fun cancelTagEdit()
    abstract fun initiateBulkDelete()
    abstract fun confirmBulkDelete()
    abstract fun cancelBulkDelete()
    abstract fun performBulkDelete()
    abstract fun bulkUpdateCategory(category: String)
    abstract fun searchTags(query: String)
    abstract fun clearSearch()
    abstract fun handleSaveError(error: Throwable)
    abstract fun retryLastOperation()
    abstract fun dismissError()
    abstract fun isOperationInProgress(): Boolean
    abstract fun canPerformAction(): Boolean
    abstract fun startCreatingTag()
    abstract fun saveNewTag(tag: Tag)
}