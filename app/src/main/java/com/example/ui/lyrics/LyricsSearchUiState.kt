package com.example.ui.lyrics

/**
 * Single structured state for Online Lyrics Search results.
 * Replaces the previous isSearching / hasSearched / results trio of independent vars.
 */
sealed interface LyricsSearchUiState {
    data object Idle : LyricsSearchUiState
    data object Searching : LyricsSearchUiState
    data class Results(val items: List<OnlineLyricsResult>) : LyricsSearchUiState
    data object Empty : LyricsSearchUiState
}
