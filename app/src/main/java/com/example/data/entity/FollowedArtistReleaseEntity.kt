package com.example.data.entity

import androidx.room.Entity

@Entity(
    tableName = "followed_artist_releases",
    primaryKeys = ["canonicalArtistId", "releaseId"]
)
data class FollowedArtistReleaseEntity(
    val canonicalArtistId: String,
    val releaseId: String,
    val providerId: String,
    val title: String,
    val artistName: String?,
    val artworkUrl: String?,
    val releaseDate: String?,
    val firstSeenAt: Long
)
