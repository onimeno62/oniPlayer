package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "followed_artists")
data class FollowedArtistEntity(
    @PrimaryKey val canonicalArtistId: String,
    val artistName: String,
    val artworkUrl: String? = null,
    val followedAt: Long = System.currentTimeMillis(),
    val lastReleaseCheckAt: Long = 0L,
    val lastSeenReleaseId: String? = null
)
