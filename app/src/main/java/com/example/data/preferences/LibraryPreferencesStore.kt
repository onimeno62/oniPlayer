package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
 * Persists library UI preferences in a dedicated DataStore so they survive app restarts.
 *
 * Layout style is intentionally NOT per screen. There are exactly two shared styles that
 * apply everywhere in the library:
 *  - CATEGORY layout: Folders, Albums, Artists, Genres, Playlists, Smart Playlists, dashboard
 *  - SONG layout: every song list (All Songs, Favorites, Most Played, Recently Added, High Rated,
 *    Never Played, and songs inside a folder/album/artist/genre/playlist)
 *
 * Legacy per-category keys are read as a one-time fallback so existing users keep their choice.
 */
private val Context.oniLibraryPrefsStore: DataStore<Preferences> by preferencesDataStore(name = "oni_library_prefs")

object LibraryPreferencesStore {

    const val STYLE_GRID = "grid"
    const val STYLE_LIST = "list"

    // Shared, persistent layout styles
    val LAYOUT_CATEGORY_STYLE = stringPreferencesKey("layout_category_style")
    val LAYOUT_SONG_STYLE = stringPreferencesKey("layout_song_style")

    // Legacy per-category keys (read-only fallback for migration)
    val LAYOUT_ALL_SONGS = stringPreferencesKey("layout_all_songs")
    val LAYOUT_ALBUMS = stringPreferencesKey("layout_albums")

    // Global sort preferences
    val SORT_BY = stringPreferencesKey("sort_by")
    val SORT_ASCENDING = stringPreferencesKey("sort_ascending")

    // Onboarding
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

    // Dashboard "Customize home": ordered "id:visible" list, see ui/library/model/DashboardSections.kt
    val DASHBOARD_SECTIONS = stringPreferencesKey("dashboard_sections")

    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun store(context: Context): DataStore<Preferences> = context.applicationContext.oniLibraryPrefsStore

    private fun Flow<Preferences>.safe(): Flow<Preferences> =
        catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    /** Category indices (see LibraryScreen categoryList) that list songs. */
    private val SONG_CATEGORY_INDICES = setOf(0, 5, 6, 7, 12, 13)

    fun isSongCategory(categoryIndex: Int?): Boolean = categoryIndex in SONG_CATEGORY_INDICES

    /** Shared layout style for category lists. Defaults to grid. */
    fun categoryLayoutStyle(context: Context): Flow<String> =
        store(context).data.safe()
            .map { it[LAYOUT_CATEGORY_STYLE] ?: it[LAYOUT_ALBUMS] ?: STYLE_GRID }
            .distinctUntilChanged()

    /** Shared layout style for song lists. Defaults to list. */
    fun songLayoutStyle(context: Context): Flow<String> =
        store(context).data.safe()
            .map { it[LAYOUT_SONG_STYLE] ?: it[LAYOUT_ALL_SONGS] ?: STYLE_LIST }
            .distinctUntilChanged()

    fun setCategoryLayoutStyle(context: Context, style: String) {
        val app = context.applicationContext
        writeScope.launch { runCatching { store(app).edit { it[LAYOUT_CATEGORY_STYLE] = style } } }
    }

    fun setSongLayoutStyle(context: Context, style: String) {
        val app = context.applicationContext
        writeScope.launch { runCatching { store(app).edit { it[LAYOUT_SONG_STYLE] = style } } }
    }

    /** Backwards compatible: routes any category index to one of the two shared styles. */
    fun layoutMode(context: Context, categoryIndex: Int?): Flow<String> =
        if (isSongCategory(categoryIndex)) songLayoutStyle(context) else categoryLayoutStyle(context)

    /** Backwards compatible: routes any category index to one of the two shared styles. */
    fun setLayoutMode(context: Context, categoryIndex: Int?, mode: String) {
        if (isSongCategory(categoryIndex)) setSongLayoutStyle(context, mode) else setCategoryLayoutStyle(context, mode)
    }

    /** Observe the global sort-by preference. Defaults to "title". */
    fun sortBy(context: Context): Flow<String> =
        store(context).data.safe().map { it[SORT_BY] ?: "title" }.distinctUntilChanged()

    /** Observe sort direction. Defaults to "true" (ascending). */
    fun sortAscending(context: Context): Flow<String> =
        store(context).data.safe().map { it[SORT_ASCENDING] ?: "true" }.distinctUntilChanged()

    fun setSortBy(context: Context, value: String) {
        val app = context.applicationContext
        writeScope.launch { runCatching { store(app).edit { it[SORT_BY] = value } } }
    }

    fun setSortAscending(context: Context, ascending: Boolean) {
        val app = context.applicationContext
        writeScope.launch { runCatching { store(app).edit { it[SORT_ASCENDING] = ascending.toString() } } }
    }

    /** Raw dashboard section order/visibility. Empty string means "defaults". */
    fun dashboardSections(context: Context): Flow<String> =
        store(context).data.safe().map { it[DASHBOARD_SECTIONS] ?: "" }.distinctUntilChanged()

    fun setDashboardSections(context: Context, value: String) {
        val app = context.applicationContext
        writeScope.launch { runCatching { store(app).edit { it[DASHBOARD_SECTIONS] = value } } }
    }

    /** True once the user finished or skipped the welcome slider. */
    fun onboardingDone(context: Context): Flow<Boolean> =
        store(context).data.safe().map { it[ONBOARDING_DONE] ?: false }.distinctUntilChanged()

    fun setOnboardingDone(context: Context, done: Boolean = true) {
        val app = context.applicationContext
        writeScope.launch { runCatching { store(app).edit { it[ONBOARDING_DONE] = done } } }
    }

    suspend fun resetAll(context: Context) {
        store(context).edit { prefs ->
            val onboarding = prefs[ONBOARDING_DONE]
            prefs.clear()
            if (onboarding != null) prefs[ONBOARDING_DONE] = onboarding
        }
    }
}
