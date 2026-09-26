package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiMusicService
import com.example.ui.lyrics.LyricsSearchUiState
import com.example.ui.lyrics.OnlineLyricsResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns Online Lyrics Search logic that previously ran inside the dialog composable.
 *
 * The search itself is unchanged: it calls [GeminiMusicService.searchRealLyricsOnline], which
 * queries LRCLIB / Lyrist / Lyrics.ovh, classifies each result's language internally and
 * deduplicates. The user no longer supplies desired languages, so no language filter is passed.
 *
 * Applying a result still goes through MusicPlayerViewModel.updateLyrics (single write path).
 */
class LyricsSearchViewModel : ViewModel() {

    private val _state = MutableStateFlow<LyricsSearchUiState>(LyricsSearchUiState.Idle)
    val state: StateFlow<LyricsSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun search(title: String, artist: String, sourceSelection: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        searchJob?.cancel()
        _state.value = LyricsSearchUiState.Searching
        searchJob = viewModelScope.launch {
            val results: List<OnlineLyricsResult> = try {
                withContext(Dispatchers.IO) {
                    GeminiMusicService.searchRealLyricsOnline(
                        title = cleanTitle,
                        artist = artist.trim(),
                        sourceSelection = sourceSelection
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Online lyrics search failed: ${e.message}")
                emptyList()
            }
            _state.value = if (results.isEmpty()) {
                LyricsSearchUiState.Empty
            } else {
                LyricsSearchUiState.Results(results)
            }
        }
    }

    fun reset() {
        searchJob?.cancel()
        searchJob = null
        _state.value = LyricsSearchUiState.Idle
    }

    private companion object {
        const val TAG = "LyricsSearchViewModel"
    }
}
