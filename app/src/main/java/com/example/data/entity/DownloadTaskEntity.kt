package com.example.data.entity

import androidx.room.Entity

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @androidx.room.PrimaryKey val id: String,
    val providerId: String,
    val remoteId: String,
    val title: String,
    val artistName: String?,
    val albumName: String?,
    val artworkUrl: String?,
    val sourceUrl: String,
    val localUri: String? = null,
    val status: String = "Queued",
    val progressPercent: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
