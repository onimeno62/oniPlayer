package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.api.GeminiMusicService
import com.example.data.api.OptimizedTags
import com.example.data.database.SongDao
import com.example.data.entity.SongEntity
import com.example.data.entity.EqualizerPresetEntity
import com.example.data.entity.PlaylistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(
    private val context: Context,
    private val songDao: SongDao
) {
    private val TAG = "MusicRepository"

    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<SongEntity>> = songDao.getFavoriteSongs()
    val allPresets: Flow<List<EqualizerPresetEntity>> = songDao.getAllPresets()
    val allPlaylists: Flow<List<PlaylistEntity>> = songDao.getAllPlaylists()

    suspend fun insertPlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        songDao.insertPlaylist(playlist)
    }

    suspend fun deletePlaylistById(playlistId: String) = withContext(Dispatchers.IO) {
        songDao.deletePlaylistById(playlistId)
    }

    suspend fun updateSong(song: SongEntity) = withContext(Dispatchers.IO) {
        songDao.updateSong(song)
    }

    /**
     * Scans local MediaStore and merges with Room database cache.
     * If empty, inserts beautiful streaming preview tracks so the app is immediately usable.
     */
    suspend fun scanAndSyncMusic(): Unit = withContext(Dispatchers.IO) {
        try {
            val localSongs = mutableListOf<SongEntity>()
            val contentResolver: ContentResolver = context.contentResolver
            val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )

            val cursor: Cursor? = contentResolver.query(uri, projection, selection, null, null)
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                    val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    val album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                    val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))

                    // Construct default album art Uri
                    val artworkUri = Uri.parse("content://media/external/audio/media/$id/albumart").toString()

                    localSongs.add(
                        SongEntity(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = if (album == "<unknown>") "Unknown Album" else album,
                            genre = "Local Audio",
                            duration = duration,
                            filePath = filePath,
                            albumArtUri = artworkUri
                        )
                    )
                }
                cursor.close()
            }

            // Get existing songs in Room to preserve custom lyrics or custom overrides
            val existingSongsMap = songDao.getAllSongs().first().associateBy { it.id }

            // If local storage is empty (very common in emulator/container), pre-populate with premium online preview tracks
            if (localSongs.isEmpty()) {
                Log.d(TAG, "No local music found. Adding professional streaming preview songs.")
                val previewTracks = getPreviewStreamingTracks()
                val finalSongs = previewTracks.map { preview ->
                    val existing = existingSongsMap[preview.id]
                    if (existing != null) {
                        // Preserve overrides & lyrics
                        preview.copy(
                            lyrics = existing.lyrics,
                            isFavorite = existing.isFavorite,
                            customTitle = existing.customTitle,
                            customArtist = existing.customArtist,
                            customAlbum = existing.customAlbum,
                            customGenre = existing.customGenre
                        )
                    } else {
                        preview
                    }
                }
                songDao.insertSongs(finalSongs)
            } else {
                // Merge local MediaStore songs with Room
                val mergedSongs = localSongs.map { local ->
                    val existing = existingSongsMap[local.id]
                    if (existing != null) {
                        local.copy(
                            lyrics = existing.lyrics,
                            isFavorite = existing.isFavorite,
                            customTitle = existing.customTitle,
                            customArtist = existing.customArtist,
                            customAlbum = existing.customAlbum,
                            customGenre = existing.customGenre
                        )
                    } else {
                        local
                    }
                }
                songDao.insertSongs(mergedSongs)
            }

            // Seed default equalizer presets if not exists
            seedDefaultPresets()

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning or syncing music: ${e.message}", e)
        }
    }

    /**
     * Toggles a song's favorite status.
     */
    suspend fun toggleFavorite(songId: String) = withContext(Dispatchers.IO) {
        val song = songDao.getSongById(songId)
        if (song != null) {
            songDao.updateSong(song.copy(isFavorite = !song.isFavorite))
        }
    }

    /**
     * Automatically fetches and caches lyrics online using public live lyrics services (LRCLIB, Lyrist, etc).
     */
    suspend fun fetchAndCacheLyrics(songId: String, title: String, artist: String): String? = withContext(Dispatchers.IO) {
        val song = songDao.getSongById(songId)
        if (song != null && !song.lyrics.isNullOrBlank()) {
            return@withContext song.lyrics // return cached lyrics if already exist
        }

        val fetchedLyrics = GeminiMusicService.searchLyrics(title, artist)
        if (fetchedLyrics != null) {
            songDao.updateLyrics(songId, fetchedLyrics)
        }
        return@withContext fetchedLyrics
    }

    /**
     * Overrides and clean a song's metadata tags both locally in Room and fetches suggestions using Gemini.
     */
    suspend fun optimizeAndEditTagsOnline(songId: String, currentFileName: String, currentTitle: String, currentArtist: String, currentAlbum: String): OptimizedTags? = withContext(Dispatchers.IO) {
        val optimized = GeminiMusicService.optimizeMusicTags(currentFileName, currentTitle, currentArtist, currentAlbum)
        if (optimized != null) {
            songDao.updateSongTags(
                songId = songId,
                title = optimized.title,
                artist = optimized.artist,
                album = optimized.album,
                genre = optimized.genre
            )
        }
        return@withContext optimized
    }

    /**
     * Manually updates the song tags in the local cache.
     */
    suspend fun updateSongTagsManual(songId: String, title: String, artist: String, album: String, genre: String) = withContext(Dispatchers.IO) {
        songDao.updateSongTags(songId, title, artist, album, genre)
    }

    /**
     * Deletes a song from the local database.
     */
    suspend fun deleteSong(songId: String) = withContext(Dispatchers.IO) {
        songDao.deleteSongById(songId)
    }

    /**
     * Saves or updates an equalizer preset.
     */
    suspend fun savePreset(preset: EqualizerPresetEntity) = withContext(Dispatchers.IO) {
        songDao.insertPreset(preset)
    }

    /**
     * Deletes a custom preset.
     */
    suspend fun deletePreset(preset: EqualizerPresetEntity) = withContext(Dispatchers.IO) {
        songDao.deletePreset(preset)
    }

    private suspend fun seedDefaultPresets() {
        val current = songDao.getAllPresets().first()
        if (current.isEmpty()) {
            val defaults = listOf(
                EqualizerPresetEntity("Flat", isCustom = false, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
                EqualizerPresetEntity("Classical", isCustom = false, 4f, 3f, -1f, 4f, 5f, 10f, 20f),
                EqualizerPresetEntity("Dance", isCustom = false, 6f, 0f, 2f, 4f, 1f, 40f, 30f),
                EqualizerPresetEntity("Metal", isCustom = false, 4f, 1f, 6f, 2f, 4f, 30f, 15f),
                EqualizerPresetEntity("Pop", isCustom = false, -2f, 2f, 5f, 1f, -2f, 20f, 40f),
                EqualizerPresetEntity("Rock", isCustom = false, 5f, 3f, -2f, 3f, 6f, 50f, 25f),
                EqualizerPresetEntity("Jazz", isCustom = false, 4f, 2f, 1f, 2f, -3f, 15f, 50f),
                EqualizerPresetEntity("Bass Booster", isCustom = false, 8f, 5f, 0f, 0f, -2f, 85f, 20f)
            )
            defaults.forEach { songDao.insertPreset(it) }
        }
    }

    private fun getPreviewStreamingTracks(): List<SongEntity> {
        return listOf(
            SongEntity(
                id = "preview_1",
                title = "Oni Synthwave Horizon",
                artist = "OniPlayer Ambient Ensemble",
                album = "Cyber Beats Vol. 1",
                genre = "Synthwave",
                duration = 272000,
                filePath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=500"
            ),
            SongEntity(
                id = "preview_2",
                title = "Cyberpunk Neon Tokyo",
                artist = "Retro Future Collective",
                album = "Tokyo Night Drive",
                genre = "Cyberpunk",
                duration = 373000,
                filePath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500"
            ),
            SongEntity(
                id = "preview_3",
                title = "Midnight Coffee Lo-Fi",
                artist = "Chillhop Dreams",
                album = "Cozy Cafe Sessions",
                genre = "Lo-Fi",
                duration = 302000,
                filePath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500"
            ),
            SongEntity(
                id = "preview_4",
                title = "Shibuya Sunset Groove",
                artist = "City Pop Revival",
                album = "Summer Memory",
                genre = "City Pop",
                duration = 318000,
                filePath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500"
            )
        )
    }
}
