package com.example.data.following

import com.example.data.database.SongDao
import com.example.data.entity.FollowedArtistEntity
import com.example.data.entity.FollowedArtistReleaseEntity
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
    suspend fun saveReleases(releases: List<FollowedArtistRelease>) {
        if (releases.isEmpty()) return
        dao.insertFollowedArtistReleases(
            releases.map {
                FollowedArtistReleaseEntity(
                    canonicalArtistId = it.canonicalArtistId,
                    releaseId = it.release.identity.itemId,
                    providerId = it.release.identity.providerId,
                    title = it.release.title,
                    artistName = it.release.artistName,
                    artworkUrl = it.release.artworkUrl,
                    releaseDate = it.release.releaseDate,
                    firstSeenAt = System.currentTimeMillis()
                )
            }
        )
    }
}
