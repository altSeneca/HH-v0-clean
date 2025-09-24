package com.hazardhawk.database

data class SyncStatistics(
    val totalPhotos: Int = 0,
    val uploadedPhotos: Int = 0,
    val pendingPhotos: Int = 0,
    val failedPhotos: Int = 0,
    val lastSyncTime: Long = 0L,
    val syncDuration: Long = 0L,
    val bytesTransferred: Long = 0L
)

data class SelectTagsForPhoto(
    val tagId: String,
    val tagName: String,
    val category: String,
    val color: String
)