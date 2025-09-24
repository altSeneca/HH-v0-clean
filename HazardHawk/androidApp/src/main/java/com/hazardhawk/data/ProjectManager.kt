package com.hazardhawk.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * ProjectManager handles project persistence and management using SharedPreferences.
 * Provides reactive project lists and current project tracking for the HazardHawk app.
 */
class ProjectManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // Internal mutable state for projects
    private val _allProjects = MutableStateFlow<List<String>>(emptyList())
    private val _currentProject = MutableStateFlow("")
    
    // Public read-only state flows
    val allProjects: StateFlow<List<String>> = _allProjects.asStateFlow()
    val currentProject: StateFlow<String> = _currentProject.asStateFlow()
    
    init {
        loadProjects()
        loadCurrentProject()
    }
    
    /**
     * Load projects from SharedPreferences
     */
    private fun loadProjects() {
        try {
            val projectsJson = sharedPreferences.getString(KEY_PROJECTS, null)
            val projects = if (projectsJson != null) {
                json.decodeFromString<List<String>>(projectsJson)
            } else {
                emptyList()
            }
            _allProjects.value = projects
        } catch (e: Exception) {
            // Handle errors gracefully with empty list
            _allProjects.value = emptyList()
            saveProjects()
        }
    }
    
    /**
     * Load current project from SharedPreferences
     */
    private fun loadCurrentProject() {
        val savedProject = sharedPreferences.getString(KEY_CURRENT_PROJECT, "")
            ?: ""
        
        // Ensure the saved project exists in the projects list
        val currentProjects = _allProjects.value
        _currentProject.value = if (currentProjects.isNotEmpty() && currentProjects.contains(savedProject)) {
            savedProject
        } else {
            currentProjects.firstOrNull() ?: ""
        }
        
        // Save the validated current project
        saveCurrentProject()
    }
    
    /**
     * Save projects to SharedPreferences
     */
    private fun saveProjects() {
        try {
            val projectsJson = json.encodeToString(_allProjects.value)
            sharedPreferences.edit()
                .putString(KEY_PROJECTS, projectsJson)
                .apply()
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }
    
    /**
     * Save current project to SharedPreferences
     */
    private fun saveCurrentProject() {
        sharedPreferences.edit()
            .putString(KEY_CURRENT_PROJECT, _currentProject.value)
            .apply()
    }
    
    /**
     * Add a new project to the list
     * @param projectName The name of the project to add
     * @return true if added successfully, false if it already exists or is invalid
     */
    fun addProject(projectName: String): Boolean {
        val trimmedName = projectName.trim()
        if (trimmedName.isEmpty() || _allProjects.value.contains(trimmedName)) {
            return false
        }
        
        val updatedProjects = _allProjects.value.toMutableList()
        updatedProjects.add(trimmedName)
        _allProjects.value = updatedProjects.sorted() // Keep projects sorted
        saveProjects()
        return true
    }
    
    /**
     * Remove a project from the list
     * @param projectName The name of the project to remove
     * @return true if removed successfully, false if it doesn't exist or is the last project
     */
    fun removeProject(projectName: String): Boolean {
        val currentProjects = _allProjects.value
        
        // Don't allow removing the last project
        if (currentProjects.size <= 1 || !currentProjects.contains(projectName)) {
            return false
        }
        
        val updatedProjects = currentProjects.toMutableList()
        updatedProjects.remove(projectName)
        _allProjects.value = updatedProjects
        saveProjects()
        
        // If we removed the current project, switch to the first available one
        if (_currentProject.value == projectName) {
            setCurrentProject(updatedProjects.firstOrNull() ?: "")
        }
        
        return true
    }
    
    /**
     * Set the current active project
     * @param projectName The name of the project to set as current
     * @return true if set successfully, false if the project doesn't exist
     */
    fun setCurrentProject(projectName: String): Boolean {
        if (!_allProjects.value.contains(projectName)) {
            return false
        }
        
        _currentProject.value = projectName
        saveCurrentProject()
        return true
    }
    
    /**
     * Get all projects as a StateFlow (same as allProjects property)
     */
    fun getAllProjectsFlow(): StateFlow<List<String>> = allProjects
    
    /**
     * Get the current project as a StateFlow (same as currentProject property)
     */
    fun getCurrentProjectFlow(): StateFlow<String> = currentProject
    
    /**
     * Get the current project name synchronously
     */
    fun getCurrentProjectName(): String = _currentProject.value
    
    /**
     * Check if a project exists
     */
    fun projectExists(projectName: String): Boolean {
        return _allProjects.value.contains(projectName.trim())
    }
    
    /**
     * Clear all projects (useful for troubleshooting)
     */
    fun clearAllProjects() {
        _allProjects.value = emptyList()
        _currentProject.value = ""
        saveProjects()
        saveCurrentProject()
    }
    
    companion object {
        private const val PREFS_NAME = "hazard_hawk_projects"
        private const val KEY_PROJECTS = "projects_list"
        private const val KEY_CURRENT_PROJECT = "current_project"
    }
}