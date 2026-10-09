package com.example.ui.viewmodel

import com.example.data.entity.SongEntity
import com.example.data.following.ArtistFollowRepository
import com.example.data.online.MusicIdentity
import com.example.data.online.ProviderIdentity
import com.example.data.online.RemoteMusicItem
import com.example.data.online.RemoteMusicType
import com.example.data.online.SearchFilter
import com.example.data.recommendation.RecommendationReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Independent per-section load state.
 *
 * Every Discover section (local and remote) exposes one of these so a single failing or
 * slow section never blanks the rest of the screen (see .ai/docs/oniplayer-online-discovery.md).
 */
sealed interface DiscoverLoadState<out T> {
    data object Idle : DiscoverLoadState<Nothing>
    data object Loading : DiscoverLoadState<Nothing>
    data object Empty : DiscoverLoadState<Nothing>
    data class Success<T>(
        val items: List<T>,
        val hasPartialFailures: Boolean = false
    ) : DiscoverLoadState<T>
    data class Error(val message: String?) : DiscoverLoadState<Nothing>
}

internal fun <T> List<T>.toLoadState(): DiscoverLoadState<T> =
    if (isEmpty()) DiscoverLoadState.Empty else DiscoverLoadState.Success(this)

/** Provider-neutral identity key: provider ids are not globally unique, so all three parts are required. */
fun RemoteMusicItem.dedupeKey(): String =
    "${identity.providerId}:${identity.type.name}:${identity.itemId}"

/**
 * UI model for a remote item. Action flags are derived from provider capabilities only,
 * never from provider ids/names.
 */
data class DiscoverRemoteCard(
    val item: RemoteMusicItem,
    val providerName: String,
    /** True when the owning provider declares ProviderCapability.STREAM and the item is a track. */
    val canStream: Boolean,
    val canFollowArtist: Boolean
) {
    val key: String get() = item.dedupeKey()

    /** Distinguishes streamable online media from metadata-only catalog entries. */
    val sourceLabel: String get() = if (canStream) "Online" else "Catalog"

    val releaseDate: String? get() = item.metadata["release_date"]?.takeIf { it.isNotBlank() }
}

/** UI model for a local song. [reason] explains why a recommendation was made. */
data class DiscoverLocalCard(
    val song: SongEntity,
    val reason: String? = null
)

data class DiscoverLocalSections(
    val libraryIsEmpty: Boolean = false,
    val madeForYou: DiscoverLoadState<DiscoverLocalCard> = DiscoverLoadState.Loading,
    val continueListening: DiscoverLoadState<DiscoverLocalCard> = DiscoverLoadState.Loading,
    val mostPlayed: DiscoverLoadState<DiscoverLocalCard> = DiscoverLoadState.Loading,
    val recentlyAdded: DiscoverLoadState<DiscoverLocalCard> = DiscoverLoadState.Loading,
    val favorites: DiscoverLoadState<DiscoverLocalCard> = DiscoverLoadState.Loading,
    val genres: DiscoverLoadState<String> = DiscoverLoadState.Loading
) {
    companion object {
        val Loading = DiscoverLocalSections()

        val EmptyLibrary = DiscoverLocalSections(
            libraryIsEmpty = true,
            madeForYou = DiscoverLoadState.Empty,
            continueListening = DiscoverLoadState.Empty,
            mostPlayed = DiscoverLoadState.Empty,
            recentlyAdded = DiscoverLoadState.Empty,
            favorites = DiscoverLoadState.Empty,
            genres = DiscoverLoadState.Empty
        )

        fun error(message: String): DiscoverLocalSections {
            val error = DiscoverLoadState.Error(message)
            return DiscoverLocalSections(
                libraryIsEmpty = false,
                madeForYou = error,
                continueListening = error,
                mostPlayed = error,
                recentlyAdded = error,
                favorites = error,
                genres = error
            )
        }
    }
}

data class DiscoverUiState(
    val query: String = "",
    /** The query that produced the current [search] state (may differ from the text being typed). */
    val submittedQuery: String = "",
    val searchFilter: SearchFilter = SearchFilter.All,
    val trending: DiscoverLoadState<DiscoverRemoteCard> = DiscoverLoadState.Idle,
    val newReleases: DiscoverLoadState<DiscoverRemoteCard> = DiscoverLoadState.Idle,
    val search: DiscoverLoadState<DiscoverRemoteCard> = DiscoverLoadState.Idle
)

/** Seam between Discover and artist-following persistence so the ViewModel is unit-testable. */
interface DiscoverFollowGateway {
    fun followedArtistIds(): Flow<Set<String>>
    fun followedReleases(): Flow<List<RemoteMusicItem>>
    suspend fun follow(canonicalArtistId: String, artistName: String, artworkUrl: String?)
    suspend fun unfollow(canonicalArtistId: String)
}

class ArtistFollowGateway(
    private val repository: ArtistFollowRepository
) : DiscoverFollowGateway {
    override fun followedArtistIds(): Flow<Set<String>> =
        repository.followedArtists().map { artists -> artists.map { it.canonicalArtistId }.toSet() }

    override fun followedReleases(): Flow<List<RemoteMusicItem>> =
        repository.followedArtists()
            .map { artists -> artists.map { it.canonicalArtistId }.toSet() }
            .flatMapLatest { artistIds ->
                repository.followedReleasesForArtists(artistIds.toList())
            }
            .map { releases ->
                releases.map { release ->
                    RemoteMusicItem(
                        identity = ProviderIdentity(release.providerId, release.releaseId, RemoteMusicType.Album),
                        title = release.title,
                        artistName = release.artistName,
                        artworkUrl = release.artworkUrl,
                        musicIdentity = MusicIdentity(canonicalArtistId = release.canonicalArtistId),
                        metadata = mapOf(
                            "release_date" to (release.releaseDate ?: ""),
                            "source" to "followed_artist"
                        )
                    )
                }
            }

    override suspend fun follow(canonicalArtistId: String, artistName: String, artworkUrl: String?) {
        repository.follow(canonicalArtistId, artistName, artworkUrl)
    }

    override suspend fun unfollow(canonicalArtistId: String) {
        repository.unfollow(canonicalArtistId)
    }
}

internal fun RecommendationReason.label(): String = when (this) {
    RecommendationReason.BecauseYouPlayed -> "From an artist you play"
    RecommendationReason.SimilarTo -> "Similar to your music"
    RecommendationReason.NewRelease -> "New release"
    RecommendationReason.Trending -> "Trending"
    RecommendationReason.YouHaventPlayedInAWhile -> "Not played in a while"
    RecommendationReason.FavoriteAffinity -> "One of your favorites"
    RecommendationReason.ColdStart -> "Fresh pick"
}
