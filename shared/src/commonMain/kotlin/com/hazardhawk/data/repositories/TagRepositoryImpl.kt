package com.hazardhawk.data.repositories

import com.hazardhawk.database.HazardHawkDatabase
import com.hazardhawk.domain.entities.Tag
import com.hazardhawk.domain.repositories.TagRepository

class TagRepositoryImpl(
    private val database: HazardHawkDatabase
) : TagRepository {
    
    override suspend fun saveTag(tag: Tag): Result<Tag> {
        return Result.success(tag)
    }
    
    override suspend fun getTag(tagId: String): Tag? {
        return null
    }
    
    override suspend fun getAllTags(): List<Tag> {
        return emptyList()
    }
    
    override suspend fun deleteTag(tagId: String): Result<Unit> {
        return Result.success(Unit)
    }
}