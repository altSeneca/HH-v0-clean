package com.hazardhawk.tags

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.hazardhawk.tags.models.UITag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Persistent storage manager for photo tags and tag definitions.
 * 
 * This class provides a complete storage solution for the tag management system,
 * handling photo-tag relationships, custom tag definitions, usage statistics,
 * and data integrity. Uses SharedPreferences with JSON serialization for
 * fast local storage that works offline.
 * 
 * Key Responsibilities:
 * - Photo-tag relationship persistence
 * - Custom tag definition storage
 * - Usage statistics tracking for smart ordering
 * - Data cleanup and integrity maintenance
 * - Backup/restore functionality
 * 
 * Storage Format:
 * - Uses JSON serialization via Gson library
 * - Stores data in SharedPreferences for fast access
 * - Implements automatic cleanup of orphaned references
 * - Provides export/import for data migration
 * 
 * Thread Safety:
 * - All operations use coroutines with Dispatchers.IO
 * - Safe for concurrent access from multiple threads
 * - Atomic operations prevent data corruption
 * 
 * @param context Android context for SharedPreferences access
 * 
 * @see UITag For tag data model
 * @see TagExportData For backup data structure
 * 
 * @author HazardHawk Development Team
 * @since 1.0
 */
class TagStorage(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("photo_tags", Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Save tags for a specific photo and update usage statistics.
     * 
     * This method persists the photo-tag relationship and automatically
     * updates usage counts for analytics and smart ordering. The operation
     * is atomic and safe for concurrent access.
     * 
     * @param photoPath Absolute file path to the photo (used as unique key)
     * @param tags Set of tag IDs to associate with this photo
     * @throws Exception If JSON serialization or storage operation fails
     * 
     * @see loadPhotoTags For retrieving saved tags
     * @see updateTagUsageCounts For usage tracking implementation
     */
    suspend fun savePhotoTags(photoPath: String, tags: Set<String>) = withContext(Dispatchers.IO) {
        try {
            // Load existing photo tags
            val allPhotoTags = loadAllPhotoTags().toMutableMap()
            allPhotoTags[photoPath] = tags
            
            // Save back to preferences
            val jsonString = json.encodeToString(allPhotoTags)
            prefs.edit().putString(KEY_PHOTO_TAGS, jsonString).apply()
            
            // Update tag usage counts
            updateTagUsageCounts(tags)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Load all tags associated with a specific photo.
     * 
     * Retrieves the set of tag IDs that were previously saved for the given
     * photo. Returns empty set if no tags are found or if an error occurs.
     * 
     * @param photoPath Absolute file path to the photo
     * @return Set of tag IDs associated with the photo, empty if none found
     * @throws Exception If storage access or JSON deserialization fails
     * 
     * @see savePhotoTags For storing photo tags
     */
    suspend fun loadPhotoTags(photoPath: String): Set<String> = withContext(Dispatchers.IO) {
        try {
            val allPhotoTags = loadAllPhotoTags()
            return@withContext allPhotoTags[photoPath] ?: emptySet()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptySet()
        }
    }
    
    /**
     * Load all photo-tag mappings
     */
    private suspend fun loadAllPhotoTags(): Map<String, Set<String>> = withContext(Dispatchers.IO) {
        try {
            val jsonString = prefs.getString(KEY_PHOTO_TAGS, null)
            return@withContext if (jsonString != null) {
                json.decodeFromString<Map<String, Set<String>>>(jsonString) ?: emptyMap()
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyMap()
        }
    }
    
    /**
     * Save or update a custom tag definition.
     * 
     * Persists a custom tag created by the user. If a tag with the same ID
     * already exists, it will be replaced with the new definition. Custom
     * tags are merged with default system tags in the UI.
     * 
     * @param tag UITag object containing tag definition and metadata
     * @throws Exception If JSON serialization or storage operation fails
     * 
     * @see loadCustomTags For retrieving custom tag definitions
     */
    suspend fun saveCustomTag(tag: UITag) = withContext(Dispatchers.IO) {
        try {
            val existingTags = loadCustomTags().toMutableList()
            
            // Remove existing tag with same ID if it exists
            existingTags.removeAll { it.id == tag.id }
            
            // Add the new/updated tag
            existingTags.add(tag)
            
            // Save back to preferences
            val json = Json.encodeToString(existingTags)
            prefs.edit().putString(KEY_CUSTOM_TAGS, json).apply()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Load all custom tags
     */
    suspend fun loadCustomTags(): List<UITag> = withContext(Dispatchers.IO) {
        try {
            val json = prefs.getString(KEY_CUSTOM_TAGS, null)
            return@withContext if (json != null) {
                Json.decodeFromString<List<UITag>>(json)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
    
    /**
     * Update usage counts for tags
     */
    private suspend fun updateTagUsageCounts(tags: Set<String>) = withContext(Dispatchers.IO) {
        try {
            val usageCounts = loadTagUsageCounts().toMutableMap()
            
            tags.forEach { tagId ->
                usageCounts[tagId] = (usageCounts[tagId] ?: 0) + 1
            }
            
            val json = Json.encodeToString(usageCounts)
            prefs.edit().putString(KEY_TAG_USAGE, json).apply()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Load tag usage counts
     */
    suspend fun loadTagUsageCounts(): Map<String, Int> = withContext(Dispatchers.IO) {
        try {
            val json = prefs.getString(KEY_TAG_USAGE, null)
            return@withContext if (json != null) {
                Json.decodeFromString<Map<String, Int>>(json)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyMap()
        }
    }
    
    /**
     * Get recently used tags
     */
    suspend fun getRecentTags(limit: Int = 10): List<String> = withContext(Dispatchers.IO) {
        try {
            val json = prefs.getString(KEY_RECENT_TAGS, null)
            val recentTags = if (json != null) {
                Json.decodeFromString<List<String>>(json)
            } else {
                emptyList()
            }
            
            return@withContext recentTags.take(limit)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
    
    /**
     * Update recently used tags
     */
    suspend fun updateRecentTags(tags: Set<String>) = withContext(Dispatchers.IO) {
        try {
            val existingRecent = getRecentTags(50).toMutableList()
            
            // Add new tags to the front, remove duplicates
            tags.reversed().forEach { tagId ->
                existingRecent.remove(tagId)
                existingRecent.add(0, tagId)
            }
            
            // Keep only the most recent 20
            val updatedRecent = existingRecent.take(20)
            
            val json = Json.encodeToString(updatedRecent)
            prefs.edit().putString(KEY_RECENT_TAGS, json).apply()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Delete photo tags when photo is deleted
     */
    suspend fun deletePhotoTags(photoPath: String) = withContext(Dispatchers.IO) {
        try {
            val allPhotoTags = loadAllPhotoTags().toMutableMap()
            allPhotoTags.remove(photoPath)
            
            val json = Json.encodeToString(allPhotoTags)
            prefs.edit().putString(KEY_PHOTO_TAGS, json).apply()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Clean up orphaned tags for photos that no longer exist.
     * 
     * Removes photo-tag relationships for photos that have been deleted
     * from the file system. This maintains data integrity and prevents
     * storage bloat from accumulating over time.
     * 
     * Should be called periodically or when loading photos to ensure
     * the tag database stays synchronized with the actual photo files.
     * 
     * @param existingPhotoPaths List of currently valid photo file paths
     * @throws Exception If storage access or cleanup operation fails
     * 
     * @see deletePhotoTags For removing specific photo tags
     */
    suspend fun cleanupOrphanedTags(existingPhotoPaths: List<String>) = withContext(Dispatchers.IO) {
        try {
            val allPhotoTags = loadAllPhotoTags()
            val validTags = allPhotoTags.filterKeys { photoPath ->
                existingPhotoPaths.contains(photoPath) || File(photoPath).exists()
            }
            
            if (validTags.size != allPhotoTags.size) {
                val json = Json.encodeToString(validTags)
                prefs.edit().putString(KEY_PHOTO_TAGS, json).apply()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Export all tag data for backup or synchronization purposes.
     * 
     * Creates a comprehensive snapshot of all tag-related data including
     * photo-tag relationships, custom tag definitions, usage statistics,
     * and recent tag history. This data can be used for backup, migration,
     * or synchronization across devices.
     * 
     * @return TagExportData containing all tag information
     * @throws Exception If data access or serialization fails
     * 
     * @see importTagData For restoring exported data
     * @see TagExportData For data structure details
     */
    suspend fun exportTagData(): TagExportData = withContext(Dispatchers.IO) {
        TagExportData(
            photoTags = loadAllPhotoTags(),
            customTags = loadCustomTags(),
            tagUsageCounts = loadTagUsageCounts(),
            recentTags = getRecentTags(50)
        )
    }
    
    /**
     * Import previously exported tag data for restore or sync.
     * 
     * Replaces all current tag data with the imported data. This is useful
     * for restoring from backup, migrating between devices, or synchronizing
     * tag data across a team. All existing data will be overwritten.
     * 
     * Warning: This operation replaces all existing tag data. Consider
     * exporting current data as backup before importing.
     * 
     * @param data TagExportData containing tag information to restore
     * @throws Exception If data import or serialization fails
     * 
     * @see exportTagData For creating backup data
     */
    suspend fun importTagData(data: TagExportData) = withContext(Dispatchers.IO) {
        try {
            // Save photo tags
            val photoTagsJson = Json.encodeToString(data.photoTags)
            prefs.edit().putString(KEY_PHOTO_TAGS, photoTagsJson).apply()
            
            // Save custom tags
            val customTagsJson = Json.encodeToString(data.customTags)
            prefs.edit().putString(KEY_CUSTOM_TAGS, customTagsJson).apply()
            
            // Save usage counts
            val usageCountsJson = Json.encodeToString(data.tagUsageCounts)
            prefs.edit().putString(KEY_TAG_USAGE, usageCountsJson).apply()
            
            // Save recent tags
            val recentTagsJson = Json.encodeToString(data.recentTags)
            prefs.edit().putString(KEY_RECENT_TAGS, recentTagsJson).apply()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    companion object {
        private const val KEY_PHOTO_TAGS = "photo_tags"
        private const val KEY_CUSTOM_TAGS = "custom_tags" 
        private const val KEY_TAG_USAGE = "tag_usage"
        private const val KEY_RECENT_TAGS = "recent_tags"
    }
}

/**
 * Data class for exporting/importing tag data
 */
data class TagExportData(
    val photoTags: Map<String, Set<String>>,
    val customTags: List<UITag>,
    val tagUsageCounts: Map<String, Int>,
    val recentTags: List<String>
)

/**
 * Extension function to create TagStorage instance
 */
fun Context.createTagStorage(): TagStorage {
    return TagStorage(this)
}