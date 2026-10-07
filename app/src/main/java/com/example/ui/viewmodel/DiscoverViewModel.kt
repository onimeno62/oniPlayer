package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.OniDatabase
import com.example.data.entity.SongEntity
import com.example.data.online.DefaultMusicProviders
import com.example.data.online.OnlineMusicRepository
import com.example.data.online.ProviderResult
import com.example.data.online.RemoteMusicItem
import com.example.data.online.SearchFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DiscoverLoadState {
    data object Idle : DiscoverLoadState
    data object Loading : DiscoverLoadState
    data class Success(val items: List<RemoteMusicItem>, val hasPartialFailures: Boolean = false) : DiscoverLoadState
    data class Error(val message: String?) : DiscoverLoadState
}

data class DiscoverUiState(
    val query: String = "",
    val searchFilter: SearchFilter = SearchFilter.All,
    val trending: DiscoverLoadState = DiscoverLoadState.Idle,
    val newReleases: DiscoverLoadState = DiscoverLoadState.Idle,
    val search: DiscoverLoadState = DiscoverLoadState.Idle
)

class DiscoverViewModel(application: Application) : AndroidViewModel(application) {
    private val localRepository = com.example.data.repository.MusicRepository(
        application,
        OniDatabase.getDatabase(application).songDao()
    )
    private val repository = OnlineMusicRepository(DefaultMusicProviders.create())

    private val _uiState = MutableStateFlow(DiscoverUiState())
    private var searchJob: Job? = null
    private val _localSongs = MutableStateFlow<List<SongEntity>>(emptyList())

    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val rankedSongs: StateFlow<List<SongEntity>> = _localSongs
        .map { songs ->
            songs.sortedWith(
                compareByDescending<SongEntity> { it.playCount }
                    .thenByDescending { it.lastPlayedTimestamp }
                    .thenBy { it.displayTitle.lowercase() }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyPlayed: StateFlow<List<SongEntity>> = _localSongs
        .map { songs ->
            songs.filter { it.lastPlayedTimestamp > 0 }
                .sortedByDescending { it.lastPlayedTimestamp }
                .take(15)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<SongEntity>> = _localSongs
        .map { songs ->
            songs.filter { it.isFavorite }
                .sortedByDescending { it.lastPlayedTimestamp }
                .take(15)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mostPlayed: StateFlow<List<SongEntity>> = rankedSongs
        .map { songs -> songs.filter { it.playCount > 0 }.take(15) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyAdded: StateFlow<List<SongEntity>> = _localSongs
        .map { songs -> songs.sortedByDescending { it.dateAdded }.take(15) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val madeForYou: StateFlow<List<SongEntity>> = _localSongs
        .map { songs ->
            val preferredGenres = songs
                .filter { it.playCount > 0 || it.isFavorite }
                .groupingBy { it.displayGenre.trim() }
                .eachCount()
                .filterKeys { it.isNotBlank() && !it.equals("Unknown Genre", true) && !it.equals("Local Audio", true) }
                .entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key }
                .toSet()

            songs.filter { song ->
                song.playCount == 0 &&
                    !song.isFavorite &&
                    (preferredGenres.isEmpty() || song.displayGenre in preferredGenres)
            }.sortedByDescending { it.dateAdded }.take(15)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val genres: StateFlow<List<String>> = _localSongs
        .map { songs ->
            songs.map { it.displayGenre.trim() }
                .filter { it.isNotBlank() && !it.equals("Unknown Genre", true) && !it.equals("Local Audio", true) }
                .distinct()
                .sorted()
                .take(20)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            localRepository.allSongs.collect { _localSongs.value = it }
        }
        refreshTrending()
        refreshNewReleases()
    }

    fun refreshNewReleases() {
        viewModelScope.launch {
            _uiState.update { it.copy(newReleases = DiscoverLoadState.Loading) }
            val results = repository.newReleases()
            val items = results.flatMap { result ->
                when (val value = result.result) {
                    is ProviderResult.Success -> value.value.map { album ->
                        RemoteMusicItem(
                            identity = album.identity,
                            title = album.title,
                            artistName = album.artistName,
                            albumName = album.title,
                            artworkUrl = album.artworkUrl,
                            musicIdentity = album.musicIdentity,
                            metadata = mapOf("release_date" to (album.releaseDate ?: ""))
                        )
                    }
                    is ProviderResult.Failure -> emptyList()
                }
            }
            val failures = results.any { it.result is ProviderResult.Failure }
            _uiState.update { it.copy(newReleases = when {
                items.isNotEmpty() -> DiscoverLoadState.Success(items.distinctBy { item -> item.identity.itemId }, failures)
                failures -> DiscoverLoadState.Error("New releases are currently unavailable.")
                else -> DiscoverLoadState.Success(emptyList())
            }) }
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun setSearchFilter(filter: SearchFilter) {
        _uiState.update { it.copy(searchFilter = filter) }
        if (_uiState.value.query.isNotBlank()) search()
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(search = DiscoverLoadState.Idle) }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(search = DiscoverLoadState.Loading) }
            val results = repository.search(query, _uiState.value.searchFilter)
            val successful = results.flatMap { result ->
                when (val value = result.result) {
                    is ProviderResult.Success<*> -> value.value as? List<RemoteMusicItem> ?: emptyList()
                    is ProviderResult.Failure -> emptyList()
                }
            }
            val failures = results.any { it.result is ProviderResult.Failure }
            _uiState.update {
                it.copy(
                    search = when {
                        successful.isNotEmpty() -> DiscoverLoadState.Success(
                            successful.distinctBy { item ->
                                item.identity.providerId + ":" + item.identity.type + ":" + item.identity.itemId
                            },
                            failures
                        )
                        failures -> DiscoverLoadState.Error("Online search is currently unavailable.")
                        else -> DiscoverLoadState.Success(emptyList())
                    }
                )
            }
        }
    }

    fun refreshTrending() {
        viewModelScope.launch {
            _uiState.update { it.copy(trending = DiscoverLoadState.Loading) }
            val results = repository.trending()
            val items = results.flatMap { result ->
                when (val value = result.result) {
                    is ProviderResult.Success -> value.value
                    is ProviderResult.Failure -> emptyList()
                }
            }
            val failures = results.any { it.result is ProviderResult.Failure }
            _uiState.update {
                it.copy(
                    trending = when {
                        items.isNotEmpty() -> DiscoverLoadState.Success(
                            items.distinctBy { item ->
                                item.identity.providerId + ":" + item.identity.type + ":" + item.identity.itemId
                            },
                            failures
                        )
                        failures -> DiscoverLoadState.Error("Online trending is currently unavailable.")
                        else -> DiscoverLoadState.Success(emptyList())
                    }
                )
            }
        }
    }
}
