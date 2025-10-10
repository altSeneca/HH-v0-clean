package com.hazardhawk.domain.repositories

import com.hazardhawk.core.models.Photo
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    suspend fun savePhoto(photo: Photo): Result<Photo>
    suspend fun getPhoto(photoId: String): Photo?
    suspend fun getPhotos(): Flow<List<Photo>>
    suspend fun getAllPhotos(): List<Photo> // Keep for backward compatibility
    suspend fun deletePhoto(photoId: String): Result<Unit>
    suspend fun updatePhotoTags(photoId: String, tags: List<String>): Result<Photo>
}