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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DiscoverLoadState {
    data object Idle : DiscoverLoadState
    data object Loading : DiscoverLoadState
    data class Success(val items: List<RemoteMusicItem>) : DiscoverLoadState
    data class Error(val message: String?) : DiscoverLoadState
}

data class DiscoverUiState(
    val query: String = "",
    val trending: DiscoverLoadState = DiscoverLoadState.Idle,
    val search: DiscoverLoadState = DiscoverLoadState.Idle
)

class DiscoverViewModel(application: Application) : AndroidViewModel(application) {
    private val localRepository = com.example.data.repository.MusicRepository(
        application,
        OniDatabase.getDatabase(application).songDao()
    )
    private val repository = OnlineMusicRepository(DefaultMusicProviders.create())

    private val _uiState = MutableStateFlow(DiscoverUiState())
    private val _localSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val recentlyPlayed = _localSongs
        .map { songs -> songs.filter { it.lastPlayedTimestamp > 0 }.sortedByDescending { it.lastPlayedTimestamp }.take(15) }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites = _localSongs
        .map { songs -> songs.filter { it.isFavorite }.take(15) }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            localRepository.allSongs.collect { _localSongs.value = it }
        }
        refreshTrending()
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(search = DiscoverLoadState.Idle) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(search = DiscoverLoadState.Loading) }
            val results = repository.search(query)
            val successful = results.flatMap { result ->
                when (val value = result.result) {
                    is ProviderResult.Success -> value.value
                    is ProviderResult.Failure -> emptyList()
                }
            }
            val failures = results.any { it.result is ProviderResult.Failure }
            _uiState.update {
                it.copy(
                    search = when {
                        successful.isNotEmpty() -> DiscoverLoadState.Success(successful)
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
            _uiState.update {
                it.copy(
                    trending = if (items.isNotEmpty()) {
                        DiscoverLoadState.Success(items)
                    } else {
                        DiscoverLoadState.Error("No online discovery results are available right now.")
                    }
                )
            }
        }
    }
}
