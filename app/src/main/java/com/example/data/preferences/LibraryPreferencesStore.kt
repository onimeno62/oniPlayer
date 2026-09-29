package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Persists library UI preferences (layout style per category, sort order, etc.)
 * in a dedicated DataStore so they survive app restarts.
 *
 * Layout style is stored per-category so the user can have grid for Albums
 * and list for Songs independently, matching Poweramp/WAVORA behavior.
 */
private val Context.oniLibraryPrefsStore: DataStore<Preferences> by preferencesDataStore(name = "oni_library_prefs")

object LibraryPreferencesStore {

    // Per-category layout keys: "grid" or "list"
    val LAYOUT_ALL_SONGS = stringPreferencesKey("layout_all_songs")
    val LAYOUT_FOLDERS = stringPreferencesKey("layout_folders")
    val LAYOUT_ALBUMS = stringPreferencesKey("layout_albums")
    val LAYOUT_ARTISTS = stringPreferencesKey("layout_artists")
    val LAYOUT_GENRES = stringPreferencesKey("layout_genres")
    val LAYOUT_PLAYLISTS = stringPreferencesKey("layout_playlists")
    val LAYOUT_DASHBOARD = stringPreferencesKey("layout_dashboard")

    // Global sort preferences
    val SORT_BY = stringPreferencesKey("sort_by")
    val SORT_ASCENDING = stringPreferencesKey("sort_ascending")

    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun store(context: Context): DataStore<Preferences> = context.applicationContext.oniLibraryPrefsStore

    /**
     * Returns the layout key for a given category index.
     * Category indices match LibraryScreen's categoryList order.
     */
    fun layoutKeyForCategory(categoryIndex: Int?): Preferences.Key<String> {
        return when (categoryIndex) {
            0 -> LAYOUT_ALL_SONGS
            1 -> LAYOUT_FOLDERS
            2 -> LAYOUT_ALBUMS
            3 -> LAYOUT_ARTISTS
            4 -> LAYOUT_GENRES
            8 -> LAYOUT_PLAYLISTS
            else -> LAYOUT_DASHBOARD
        }
    }

    /** Observe layout mode for a specific category. Defaults to "grid". */
    fun layoutMode(context: Context, categoryIndex: Int?): Flow<String> {
        val key = layoutKeyForCategory(categoryIndex)
        return store(context).data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[key] ?: "grid" }
            .distinctUntilChanged()
    }

    /** Observe the global sort-by preference. Defaults to "title". */
    fun sortBy(context: Context): Flow<String> {
        return store(context).data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[SORT_BY] ?: "title" }
            .distinctUntilChanged()
    }

    /** Observe sort direction. Defaults to "true" (ascending). */
    fun sortAscending(context: Context): Flow<String> {
        return store(context).data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[SORT_ASCENDING] ?: "true" }
            .distinctUntilChanged()
    }

    /** Fire-and-forget write for layout mode per category. */
    fun setLayoutMode(context: Context, categoryIndex: Int?, mode: String) {
        val app = context.applicationContext
        val key = layoutKeyForCategory(categoryIndex)
        writeScope.launch { runCatching { store(app).edit { it[key] = mode } } }
    }

    /** Fire-and-forget write for sort preference. */
    fun setSortBy(context: Context, value: String) {
        val app = context.applicationContext
        writeScope.launch { runCatching { store(app).edit { it[SORT_BY] = value } } }
    }

    /** Fire-and-forget write for sort direction. */
    fun setSortAscending(context: Context, ascending: Boolean) {
        val app = context.applicationContext
        writeScope.launch { runCatching { store(app).edit { it[SORT_ASCENDING] = ascending.toString() } } }
    }

    suspend fun resetAll(context: Context) {
        store(context).edit { it.clear() }
    }
}
