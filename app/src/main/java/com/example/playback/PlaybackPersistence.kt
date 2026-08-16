package com.example.playback

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * Immutable representation of persisted playback information.
 */
data class PersistedPlaybackState(
    val currentSongId: String?,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val shuffleMode: ShuffleMode = ShuffleMode.RANDOM,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val queueIds: List<String> = emptyList()
)

/**
 * Handles low-level saving and loading of playback state to/from [SharedPreferences].
 *
 * This class has no dependency on ExoPlayer, MediaSession, or UI components.
 */
class PlaybackPersistence(context: Context) {

    companion object {
        const val PREFS_NAME = "playback_persistence"
        const val KEY_CURRENT_SONG_ID = "current_song_id"
        const val KEY_POSITION = "position"
        const val KEY_IS_PLAYING = "is_playing"
        const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"
        const val KEY_SHUFFLE_MODE = "shuffle_mode"
        const val KEY_REPEAT_MODE = "repeat_mode"
        const val KEY_QUEUE_IDS = "queue_ids"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Persists the given [PersistedPlaybackState].
     */
    fun save(state: PersistedPlaybackState) {
        try {
            val queueIdsJson = serializeQueueIds(state.queueIds)
            prefs.edit()
                .putString(KEY_CURRENT_SONG_ID, state.currentSongId ?: "")
                .putLong(KEY_POSITION, state.positionMs.coerceAtLeast(0L))
                .putBoolean(KEY_IS_PLAYING, state.isPlaying)
                .putBoolean(KEY_SHUFFLE_ENABLED, state.shuffleEnabled)
                .putInt(KEY_SHUFFLE_MODE, state.shuffleMode.ordinal)
                .putInt(KEY_REPEAT_MODE, state.repeatMode.ordinal)
                .putString(KEY_QUEUE_IDS, queueIdsJson)
                .apply()
        } catch (_: Exception) {
            // Failures in persistence must never crash playback
        }
    }

    /**
     * Updates only the playback position timestamp.
     */
    fun savePosition(positionMs: Long) {
        try {
            prefs.edit()
                .putLong(KEY_POSITION, positionMs.coerceAtLeast(0L))
                .apply()
        } catch (_: Exception) {}
    }

    /**
     * Loads the persisted playback state, or returns null if no valid state was saved.
     */
    fun load(): PersistedPlaybackState? {
        return try {
            val currentSongId = prefs.getString(KEY_CURRENT_SONG_ID, null)?.takeIf { it.isNotEmpty() }
            val queueIdsStr = prefs.getString(KEY_QUEUE_IDS, null)
            val queueIds = deserializeQueueIds(queueIdsStr)

            if (currentSongId == null && queueIds.isEmpty()) {
                return null
            }

            val position = prefs.getLong(KEY_POSITION, 0L).coerceAtLeast(0L)
            val isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)
            val shuffleEnabled = prefs.getBoolean(KEY_SHUFFLE_ENABLED, false)
            val rawShuffleMode = prefs.getInt(KEY_SHUFFLE_MODE, ShuffleMode.RANDOM.ordinal)
            val shuffleMode = ShuffleMode.entries.getOrElse(rawShuffleMode) { ShuffleMode.RANDOM }
            val rawRepeatMode = prefs.getInt(KEY_REPEAT_MODE, RepeatMode.ALL.ordinal)
            val repeatMode = RepeatMode.entries.getOrElse(rawRepeatMode) { RepeatMode.ALL }

            PersistedPlaybackState(
                currentSongId = currentSongId,
                positionMs = position,
                isPlaying = isPlaying,
                shuffleEnabled = shuffleEnabled,
                shuffleMode = shuffleMode,
                repeatMode = repeatMode,
                queueIds = queueIds
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Clears all persisted playback data.
     */
    fun clear() {
        try {
            prefs.edit().clear().apply()
        } catch (_: Exception) {}
    }

    /**
     * Serializes a list of song IDs into a JSON string.
     */
    fun serializeQueueIds(queueIds: List<String>): String {
        val array = JSONArray()
        for (id in queueIds) {
            array.put(id)
        }
        return array.toString()
    }

    /**
     * Deserializes a JSON string into a list of song IDs.
     */
    fun deserializeQueueIds(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val id = array.optString(i)
                if (!id.isNullOrEmpty()) {
                    list.add(id)
                }
            }
        } catch (_: Exception) {}
        return list
    }
}
