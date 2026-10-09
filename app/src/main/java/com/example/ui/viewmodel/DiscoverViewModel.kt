package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.database.OniDatabase
import com.example.data.entity.SongEntity
import com.example.data.following.ArtistFollowRepository
import com.example.data.online.DefaultMusicProviders
import com.example.data.online.OnlineMusicRepository
import com.example.data.online.ProviderCapability
import com.example.data.online.ProviderResult
import com.example.data.online.RemoteAlbum
import com.example.data.online.RemoteMusicItem
import com.example.data.online.RemoteMusicType
import com.example.data.online.SearchFilter
import com.example.data.online.getOrNull
import com.example.data.recommendation.RecommendationEngine
import com.example.data.recommendation.RecommendationSnapshot
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Discover screen state holder.
 *
 * - Local sections derive only from the local library flow and never touch the network.
 * - Remote sections go through [OnlineMusicRepository]; provider failures are isolated per section.
 * - Playback is NOT owned here: the screen forwards local play/queue actions to the
 *   authoritative player (MusicPlayerViewModel -> PlaybackController).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModel(
    private val localSongs: Flow<List<SongEntity>>,
    private val onlineRepository: OnlineMusicRepository,
    private val followGateway: DiscoverFollowGateway,
    private val recommendationEngine: RecommendationEngine = RecommendationEngine(),
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var trendingJob: Job? = null
    private var newReleasesJob: Job? = null

    /** Incremented on every search request/clear; a response is applied only if it is still current. */
    private var searchGeneration = 0L

    private val localRetry = MutableStateFlow(0)
    private val followedRetry = MutableStateFlow(0)

    val localSections: StateFlow<DiscoverLocalSections> = localRetry
        .flatMapLatest {
            localSongs
                .map { songs -> buildLocalSections(songs, recommendationEngine) }
                .onStart { emit(DiscoverLocalSections.Loading) }
                .catch { emit(DiscoverLocalSections.error(LOCAL_ERROR)) }
        }
        .flowOn(computeDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverLocalSections.Loading)

    val followedArtistIds: StateFlow<Set<String>> = followGateway.followedArtistIds()
        .catch { emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val followedReleases: StateFlow<DiscoverLoadState<DiscoverRemoteCard>> = followedRetry
        .flatMapLatest {
            followGateway.followedReleases()
                .map { items ->
                    items.distinctBy { it.dedupeKey() }
                        .take(FOLLOWED_RELEASES_LIMIT)
                        .map(::toCard)
                        .toLoadState()
                }
                .onStart { emit(DiscoverLoadState.Loading) }
                .catch { emit(DiscoverLoadState.Error(FOLLOWED_ERROR)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverLoadState.Loading)

    init {
        refreshTrending()
        refreshNewReleases()
    }

    fun retryLocal() {
        localRetry.update { it + 1 }
    }

    fun retryFollowedReleases() {
        followedRetry.update { it + 1 }
    }

    fun refreshNewReleases() {
        newReleasesJob?.cancel()
        _uiState.update { it.copy(newReleases = DiscoverLoadState.Loading) }
        newReleasesJob = viewModelScope.launch {
            val results = onlineRepository.newReleases().map { section ->
                section.result.mapValue { albums -> albums.map(::albumToItem) }
            }
            val state = aggregateRemote(results, NEW_RELEASES_ERROR)
            _uiState.update { it.copy(newReleases = state) }
        }
    }

    fun refreshTrending() {
        trendingJob?.cancel()
        _uiState.update { it.copy(trending = DiscoverLoadState.Loading) }
        trendingJob = viewModelScope.launch {
            val state = aggregateRemote(onlineRepository.trending().map { it.result }, TRENDING_ERROR)
            _uiState.update { it.copy(trending = state) }
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        if (query.isBlank()) clearSearch()
    }

    fun setSearchFilter(filter: SearchFilter) {
        _uiState.update { it.copy(searchFilter = filter) }
        if (_uiState.value.query.isNotBlank()) search()
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) {
            clearSearch()
            return
        }

        searchJob?.cancel()
        val generation = ++searchGeneration
        val filter = _uiState.value.searchFilter
        _uiState.update { it.copy(search = DiscoverLoadState.Loading, submittedQuery = query) }
        searchJob = viewModelScope.launch {
            val results = onlineRepository.search(query, filter).map { it.result }
            val state = aggregateRemote(results, SEARCH_ERROR)
            if (generation == searchGeneration) {
                _uiState.update { it.copy(search = state) }
            }
        }
    }

    fun toggleFollowArtist(item: RemoteMusicItem) {
        val artistId = item.musicIdentity.canonicalArtistId ?: return
        val artistName = item.artistName?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            // Persistence failures must never escape into the Discover UI or playback.
            runCatching {
                if (artistId in followedArtistIds.value) {
                    followGateway.unfollow(artistId)
                } else {
                    followGateway.follow(artistId, artistName, item.artworkUrl)
                }
            }
        }
    }

    private fun clearSearch() {
        searchJob?.cancel()
        searchJob = null
        searchGeneration++
        _uiState.update { it.copy(search = DiscoverLoadState.Idle, submittedQuery = "") }
    }

    private fun aggregateRemote(
        results: List<ProviderResult<List<RemoteMusicItem>>>,
        errorMessage: String
    ): DiscoverLoadState<DiscoverRemoteCard> {
        val items = results
            .flatMap { it.getOrNull().orEmpty() }
            .distinctBy { it.dedupeKey() }
        val hasFailures = results.any { it is ProviderResult.Failure }
        return when {
            items.isNotEmpty() -> DiscoverLoadState.Success(items.map(::toCard), hasFailures)
            hasFailures -> DiscoverLoadState.Error(errorMessage)
            else -> DiscoverLoadState.Empty
        }
    }

    private fun toCard(item: RemoteMusicItem): DiscoverRemoteCard {
        val provider = onlineRepository.provider(item.identity.providerId)
        val supportsStream = provider?.capabilities?.supports(ProviderCapability.STREAM) == true
        return DiscoverRemoteCard(
            item = item,
            providerName = provider?.displayName ?: item.identity.providerId,
            canStream = supportsStream && item.identity.type == RemoteMusicType.Track,
            canFollowArtist = item.musicIdentity.canonicalArtistId != null && !item.artistName.isNullOrBlank()
        )
    }

    private fun albumToItem(album: RemoteAlbum): RemoteMusicItem = RemoteMusicItem(
        identity = album.identity,
        title = album.title,
        artistName = album.artistName,
        albumName = album.title,
        artworkUrl = album.artworkUrl,
        musicIdentity = album.musicIdentity,
        metadata = mapOf("release_date" to (album.releaseDate ?: ""))
    )

    companion object {
        private const val FOLLOWED_RELEASES_LIMIT = 30
        private const val LOCAL_ERROR = "Your library couldn't be loaded."
        private const val FOLLOWED_ERROR = "Releases from your artists couldn't be loaded."
        private const val NEW_RELEASES_ERROR = "New releases are unavailable right now."
        private const val TRENDING_ERROR = "Trending is unavailable right now."
        private const val SEARCH_ERROR = "Online search is unavailable right now."

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY]) as Application
                val dao = OniDatabase.getDatabase(application).songDao()
                DiscoverViewModel(
                    localSongs = MusicRepository(application, dao).allSongs,
                    onlineRepository = OnlineMusicRepository(DefaultMusicProviders.create()),
                    followGateway = ArtistFollowGateway(ArtistFollowRepository(dao))
                )
            }
        }
    }
}

private const val SECTION_LIMIT = 15
private const val GENRE_LIMIT = 20

/** Pure, deterministic local section builder. Runs on the compute dispatcher. */
internal fun buildLocalSections(
    songs: List<SongEntity>,
    engine: RecommendationEngine
): DiscoverLocalSections {
    if (songs.isEmpty()) return DiscoverLocalSections.EmptyLibrary

    val madeForYou = engine
        .recommend(RecommendationSnapshot(localSongs = songs), limit = SECTION_LIMIT)
        .mapNotNull { recommendation ->
            recommendation.localSong?.let { DiscoverLocalCard(it, recommendation.reason.label()) }
        }

    val continueListening = songs
        .filter { it.lastPlayedTimestamp > 0 }
        .sortedByDescending { it.lastPlayedTimestamp }
        .take(SECTION_LIMIT)
        .map { DiscoverLocalCard(it) }

    val mostPlayed = songs
        .filter { it.playCount > 0 }
        .sortedWith(
            compareByDescending<SongEntity> { it.playCount }
                .thenByDescending { it.lastPlayedTimestamp }
                .thenBy { it.displayTitle.lowercase() }
        )
        .take(SECTION_LIMIT)
        .map { DiscoverLocalCard(it) }

    val recentlyAdded = songs
        .sortedByDescending { it.dateAdded }
        .take(SECTION_LIMIT)
        .map { DiscoverLocalCard(it) }

    val favorites = songs
        .filter { it.isFavorite }
        .sortedByDescending { it.lastPlayedTimestamp }
        .take(SECTION_LIMIT)
        .map { DiscoverLocalCard(it) }

    val genres = songs
        .asSequence()
        .map { it.displayGenre.trim() }
        .filter { it.isNotBlank() && !it.equals("Unknown Genre", true) && !it.equals("Local Audio", true) }
        .distinct()
        .sorted()
        .take(GENRE_LIMIT)
        .toList()

    return DiscoverLocalSections(
        libraryIsEmpty = false,
        madeForYou = madeForYou.toLoadState(),
        continueListening = continueListening.toLoadState(),
        mostPlayed = mostPlayed.toLoadState(),
        recentlyAdded = recentlyAdded.toLoadState(),
        favorites = favorites.toLoadState(),
        genres = genres.toLoadState()
    )
}

private inline fun <T, R> ProviderResult<T>.mapValue(transform: (T) -> R): ProviderResult<R> = when (this) {
    is ProviderResult.Success -> ProviderResult.Success(transform(value))
    is ProviderResult.Failure -> this
}
