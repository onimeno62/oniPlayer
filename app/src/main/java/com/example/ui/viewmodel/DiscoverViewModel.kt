package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.online.DefaultMusicProviders
import com.example.data.online.OnlineMusicRepository
import com.example.data.online.ProviderResult
import com.example.data.online.RemoteMusicItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

class DiscoverViewModel : ViewModel() {
    private val repository = OnlineMusicRepository(DefaultMusicProviders.create())

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
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
