package com.hazardhawk.domain.repositories

import com.hazardhawk.core.models.Tag

interface TagRepository {
    suspend fun saveTag(tag: Tag): Result<Tag>
    suspend fun getTag(tagId: String): Tag?
    suspend fun getAllTags(): List<Tag>
    suspend fun deleteTag(tagId: String): Result<Unit>
}