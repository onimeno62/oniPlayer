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
import com.example.data.entity.ArtistSummaryEntity
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

    // Paths of files that a rename operation couldn't delete (usually a scoped-storage
    // permission problem). Every scan filters these out so a leftover physical duplicate
    // can never re-enter the library as a duplicate song, even before it's actually deleted.
    private val pendingCleanupPaths = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun markPendingCleanup(path: String) {
        pendingCleanupPaths.add(path)
    }

    fun clearPendingCleanup(path: String) {
        pendingCleanupPaths.remove(path)
    }

    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<SongEntity>> = songDao.getFavoriteSongs()
    val allPresets: Flow<List<EqualizerPresetEntity>> = songDao.getAllPresets()
    val allPlaylists: Flow<List<PlaylistEntity>> = songDao.getAllPlaylists()
    val allArtistSummaries: Flow<List<ArtistSummaryEntity>> = songDao.getAllArtistSummariesFlow()

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

            // Pull existing Room rows up-front so we know which songs already have their
            // extended tags backfilled, and can skip re-reading the file for those on repeat scans.
            val existingSongsList = songDao.getAllSongs().first()
            val existingSongsMap = existingSongsList.associateBy { it.id }
            val existingSongsByPath = existingSongsList.associateBy { it.filePath }

            val cursor: Cursor? = contentResolver.query(uri, projection, selection, null, null)
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                    val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    val album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                    val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))

                    // Known leftover from a rename that couldn't fully clean up its old file yet.
                    // Try an opportunistic delete; either way, never let it re-enter the library.
                    if (filePath != null && pendingCleanupPaths.contains(filePath)) {
                        val leftoverFile = java.io.File(filePath)
                        if (!leftoverFile.exists() || leftoverFile.delete()) {
                            clearPendingCleanup(filePath)
                        }
                        Log.d(TAG, "Skipping known pending-cleanup duplicate: $filePath")
                        continue
                    }

                    // Verify if the physical file exists on disk and is non-empty to prevent duplicates or ghost records
                    if (filePath != null && filePath.isNotEmpty()) {
                        val physicalFile = java.io.File(filePath)
                        if (!physicalFile.exists() || physicalFile.length() == 0L) {
                            Log.d(TAG, "Skipping out-of-sync MediaStore entry as physical file does not exist or is empty: $filePath")
                            try {
                                val songUri = android.content.ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toLong())
                                contentResolver.delete(songUri, null, null)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to delete stale MediaStore entry: ${e.message}")
                            }
                            continue
                        }
                    }

                    // Construct default album art Uri
                    val artworkUri = Uri.parse("content://media/external/audio/media/$id/albumart").toString()

                    // Extended tags (genre, album artist, composer, disc/track number, year, comment)
                    // aren't in the MediaStore projection above, so read them from the file directly.
                    // Skip the read if this song was already backfilled in a previous scan, so repeat
                    // scans of a large library stay fast.
                    val existingForMeta = existingSongsMap[id] ?: existingSongsByPath[filePath]
                    val alreadyBackfilled = existingForMeta != null && (
                        existingForMeta.genre != "Local Audio" ||
                        existingForMeta.albumArtist.isNotEmpty() ||
                        existingForMeta.composer.isNotEmpty() ||
                        existingForMeta.disc.isNotEmpty() ||
                        existingForMeta.track.isNotEmpty() ||
                        existingForMeta.year.isNotEmpty() ||
                        existingForMeta.comment.isNotEmpty()
                    )

                    var extGenre = "Local Audio"
                    var extAlbumArtist = ""
                    var extComposer = ""
                    var extDisc = ""
                    var extTrack = ""
                    var extYear = ""
                    var extComment = ""

                    if (alreadyBackfilled && existingForMeta != null) {
                        extGenre = existingForMeta.genre
                        extAlbumArtist = existingForMeta.albumArtist
                        extComposer = existingForMeta.composer
                        extDisc = existingForMeta.disc
                        extTrack = existingForMeta.track
                        extYear = existingForMeta.year
                        extComment = existingForMeta.comment
                    } else if (filePath != null) {
                        try {
                            val fileMeta = com.example.data.api.GeminiMusicService.readActualFileMetadata(filePath)
                            extGenre = fileMeta.genre?.takeIf { it.isNotBlank() } ?: "Unknown Genre"
                            extAlbumArtist = fileMeta.albumArtist ?: ""
                            extComposer = fileMeta.composer ?: ""
                            extDisc = fileMeta.disc ?: ""
                            extTrack = fileMeta.track ?: ""
                            extYear = fileMeta.year ?: ""
                            extComment = fileMeta.comment ?: ""
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to read extended tags for $filePath: ${e.message}")
                        }
                    }

                    localSongs.add(
                        SongEntity(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = if (album == "<unknown>") "Unknown Album" else album,
                            genre = extGenre,
                            albumArtist = extAlbumArtist,
                            composer = extComposer,
                            disc = extDisc,
                            track = extTrack,
                            year = extYear,
                            comment = extComment,
                            duration = duration,
                            filePath = filePath,
                            albumArtUri = artworkUri
                        )
                    )
                }
                cursor.close()
            }

            // If local storage is empty (very common in emulator/container), pre-populate with premium online preview tracks
            if (localSongs.isEmpty()) {
                Log.d(TAG, "No local music found. Adding professional streaming preview songs.")
                val previewTracks = getPreviewStreamingTracks()
                val finalSongs = previewTracks.map { preview ->
                    val existing = existingSongsMap[preview.id]
                    if (existing != null) {
                        // Preserve overrides & lyrics & playback metrics
                        preview.copy(
                            lyrics = existing.lyrics,
                            isFavorite = existing.isFavorite,
                            playCount = existing.playCount,
                            lastPlayedTimestamp = existing.lastPlayedTimestamp,
                            rating = existing.rating,
                            dateAdded = existing.dateAdded,
                            customTitle = existing.customTitle,
                            customArtist = existing.customArtist,
                            customAlbum = existing.customAlbum,
                            customGenre = existing.customGenre,
                            customAlbumArtist = existing.customAlbumArtist,
                            customComposer = existing.customComposer,
                            customDisc = existing.customDisc,
                            customTrack = existing.customTrack,
                            customYear = existing.customYear,
                            customComment = existing.customComment,
                            customBpm = existing.customBpm
                        )
                    } else {
                        preview
                    }
                }
                songDao.insertSongs(finalSongs)
            } else {
                // Merge local MediaStore songs with Room
                val mergedSongs = mutableListOf<SongEntity>()
                val oldIdsToDelete = mutableSetOf<String>()

                for (local in localSongs) {
                    val existing = existingSongsMap[local.id] ?: existingSongsByPath[local.filePath]
                    if (existing != null) {
                        if (existing.id != local.id) {
                            oldIdsToDelete.add(existing.id)
                        }
                        mergedSongs.add(
                            local.copy(
                                lyrics = existing.lyrics,
                                isFavorite = existing.isFavorite,
                                playCount = existing.playCount,
                                lastPlayedTimestamp = existing.lastPlayedTimestamp,
                                rating = existing.rating,
                                dateAdded = existing.dateAdded,
                                customTitle = existing.customTitle,
                                customArtist = existing.customArtist,
                                customAlbum = existing.customAlbum,
                                customGenre = existing.customGenre,
                                customAlbumArtist = existing.customAlbumArtist,
                                customComposer = existing.customComposer,
                                customDisc = existing.customDisc,
                                customTrack = existing.customTrack,
                                customYear = existing.customYear,
                                customComment = existing.customComment,
                                customBpm = existing.customBpm
                            )
                        )
                    } else {
                        mergedSongs.add(local)
                    }
                }
                songDao.insertSongs(mergedSongs)

                // Delete old duplicate records whose IDs changed (e.g. file was renamed)
                for (oldId in oldIdsToDelete) {
                    songDao.deleteSongById(oldId)
                    Log.d(TAG, "Deleted duplicate old record after rename/ID change: $oldId")
                }

                // Delete stale local songs from Room that no longer exist physically on disk or are empty
                val localIdsInMediaStore = localSongs.map { it.id }.toSet()
                for (existing in existingSongsList) {
                    if (existing.id.startsWith("preview_")) continue
                    if (!localIdsInMediaStore.contains(existing.id)) {
                        val file = java.io.File(existing.filePath)
                        if (!file.exists() || file.length() == 0L) {
                            songDao.deleteSongById(existing.id)
                            Log.d(TAG, "Deleted stale local song no longer in MediaStore/Disk or empty: ${existing.title} (${existing.id})")
                        } else {
                            Log.d(TAG, "Retained local song in Room since physical file still exists on disk: ${existing.title} (${existing.filePath})")
                        }
                    }
                }
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

    // --- Artist Summary ---
    suspend fun getArtistSummary(artistName: String): ArtistSummaryEntity? = withContext(Dispatchers.IO) {
        songDao.getArtistSummary(artistName)
    }

    fun getArtistSummaryFlow(artistName: String): Flow<ArtistSummaryEntity?> {
        return songDao.getArtistSummaryFlow(artistName)
    }

    suspend fun saveArtistSummary(artistName: String, summary: String) = withContext(Dispatchers.IO) {
        val existing = songDao.getArtistSummary(artistName)
        val entity = ArtistSummaryEntity(
            artistName = artistName,
            summary = summary,
            lastUpdated = System.currentTimeMillis(),
            artworkUri = existing?.artworkUri
        )
        songDao.insertArtistSummary(entity)
    }

    suspend fun saveArtistArtworkUri(artistName: String, artworkUri: String?) = withContext(Dispatchers.IO) {
        val existing = songDao.getArtistSummary(artistName)
        val entity = ArtistSummaryEntity(
            artistName = artistName,
            summary = existing?.summary ?: "",
            lastUpdated = System.currentTimeMillis(),
            artworkUri = artworkUri
        )
        songDao.insertArtistSummary(entity)
    }

    suspend fun saveArtistData(artistName: String, summary: String, artworkUri: String?) = withContext(Dispatchers.IO) {
        val entity = ArtistSummaryEntity(
            artistName = artistName,
            summary = summary,
            lastUpdated = System.currentTimeMillis(),
            artworkUri = artworkUri
        )
        songDao.insertArtistSummary(entity)
    }

    suspend fun deleteArtistSummary(artistName: String) = withContext(Dispatchers.IO) {
        songDao.deleteArtistSummary(artistName)
    }
}
