package com.example.data.following

import com.example.data.database.SongDao
import com.example.data.entity.FollowedArtistEntity
import kotlinx.coroutines.flow.Flow

class ArtistFollowRepository(
    private val dao: SongDao
) {
    fun followedArtists(): Flow<List<FollowedArtistEntity>> = dao.getFollowedArtists()

    suspend fun follow(
        canonicalArtistId: String,
        artistName: String,
        artworkUrl: String?
    ) {
        require(canonicalArtistId.isNotBlank())
        dao.insertFollowedArtist(
            FollowedArtistEntity(
                canonicalArtistId = canonicalArtistId,
                artistName = artistName,
                artworkUrl = artworkUrl
            )
        )
    }

    suspend fun unfollow(canonicalArtistId: String) {
        if (canonicalArtistId.isNotBlank()) {
            dao.deleteFollowedArtist(canonicalArtistId)
        }
    }

    suspend fun markReleaseSync(
        canonicalArtistId: String,
        checkedAt: Long,
        lastSeenReleaseId: String?
    ) {
        if (canonicalArtistId.isNotBlank()) {
            dao.updateFollowedArtistReleaseSync(
                canonicalArtistId = canonicalArtistId,
                checkedAt = checkedAt,
                lastSeenReleaseId = lastSeenReleaseId
            )
        }
    }
}
