package com.example.data.following

import com.example.data.entity.FollowedArtistEntity
import com.example.data.online.OnlineMusicRepository
import com.example.data.online.ProviderResult
import com.example.data.online.RemoteAlbum
import kotlinx.coroutines.flow.first

data class FollowedArtistRelease(
    val canonicalArtistId: String,
    val release: RemoteAlbum
)

data class ArtistReleaseSyncResult(
    val releases: List<FollowedArtistRelease>,
    val failedArtistIds: Set<String>
)

/**
 * Synchronizes followed artists against the canonical MusicBrainz catalog.
 *
 * The first successful check establishes a baseline and intentionally emits no
 * releases, preventing a newly followed artist from flooding Discover with their
 * entire back catalog.
 */
class ArtistReleaseSync(
    private val followRepository: ArtistFollowRepository,
    private val onlineRepository: OnlineMusicRepository,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun sync(): ArtistReleaseSyncResult {
        val followedArtists = followRepository.followedArtists().first()
        if (followedArtists.isEmpty()) {
            return ArtistReleaseSyncResult(emptyList(), emptySet())
        }

        val releases = mutableListOf<FollowedArtistRelease>()
        val failures = mutableSetOf<String>()

        for (artist in followedArtists) {
            when (val result = onlineRepository.artistReleases(
                providerId = MUSICBRAINZ_PROVIDER_ID,
                providerArtistId = artist.canonicalArtistId
            )) {
                is ProviderResult.Success -> {
                    val ordered = ArtistReleaseSyncPolicy.order(result.value)
                    val newest = ordered.firstOrNull()
                    val newReleases = ArtistReleaseSyncPolicy.newReleasesSince(
                        releases = ordered,
                        lastSeenReleaseId = artist.lastSeenReleaseId
                    )

                    if (artist.lastSeenReleaseId == null) {
                        followRepository.markReleaseSync(
                            canonicalArtistId = artist.canonicalArtistId,
                            checkedAt = now(),
                            lastSeenReleaseId = newest?.identity?.itemId
                        )
                    } else {
                        releases += newReleases.map { release ->
                            FollowedArtistRelease(artist.canonicalArtistId, release)
                        }
                        followRepository.markReleaseSync(
                            canonicalArtistId = artist.canonicalArtistId,
                            checkedAt = now(),
                            lastSeenReleaseId = newest?.identity?.itemId ?: artist.lastSeenReleaseId
                        )
                    }
                }

                is ProviderResult.Failure -> failures += artist.canonicalArtistId
            }
        }

        return ArtistReleaseSyncResult(
            releases = releases.distinctBy {
                it.release.musicIdentity.canonicalReleaseGroupId
                    ?: it.release.identity.itemId
            },
            failedArtistIds = failures
        )
    }

    private companion object {
        const val MUSICBRAINZ_PROVIDER_ID = "musicbrainz"
    }
}

internal object ArtistReleaseSyncPolicy {
    fun order(releases: List<RemoteAlbum>): List<RemoteAlbum> =
        releases
            .filter { it.identity.itemId.isNotBlank() }
            .distinctBy { it.identity.itemId }
            .sortedWith(
                compareByDescending<RemoteAlbum> { it.releaseDate.orEmpty() }
                    .thenBy { it.identity.itemId }
            )

    fun newReleasesSince(
        releases: List<RemoteAlbum>,
        lastSeenReleaseId: String?
    ): List<RemoteAlbum> {
        val ordered = order(releases)
        val seenIndex = ordered.indexOfFirst { it.identity.itemId == lastSeenReleaseId }
        if (seenIndex < 0) {
            // We lost the previous cursor. Do not announce the whole catalog.
            return emptyList()
        }
        return ordered.take(seenIndex)
    }
}
