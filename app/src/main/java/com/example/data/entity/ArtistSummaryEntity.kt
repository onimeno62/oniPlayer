package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artist_summaries")
data class ArtistSummaryEntity(
    @PrimaryKey val artistName: String,
    val summary: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val artworkUri: String? = null
)
