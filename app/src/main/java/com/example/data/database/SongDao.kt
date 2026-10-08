package com.example.data.database

import androidx.room.*
import com.example.data.entity.SongEntity
import com.example.data.entity.EqualizerPresetEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.ArtistSummaryEntity
import com.example.data.entity.FollowedArtistEntity
import com.example.data.entity.FollowedArtistReleaseEntity
import com.example.data.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM songs WHERE isFavorite = 1")
    suspend fun getFavoriteSongsCount(): Int

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getAllPlaylistsCount(): Int

    @Query("SELECT * FROM songs WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 15")
    suspend fun getRecentlyPlayedSongs(): List<SongEntity>

    @Query("SELECT * FROM songs LIMIT 15")
    suspend fun getFallbackSongs(): List<SongEntity>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsSuspend(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: String): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:songIds)")
    suspend fun getSongsByIds(songIds: List<String>): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET lyrics = :lyrics WHERE id = :songId")
    suspend fun updateLyrics(songId: String, lyrics: String?)

    @Query("UPDATE songs SET customTitle = :title, customArtist = :artist, customAlbum = :album, customGenre = :genre WHERE id = :songId")
    suspend fun updateSongTags(songId: String, title: String?, artist: String?, album: String?, genre: String?)

    @Query("SELECT * FROM equalizer_presets")
    fun getAllPresets(): Flow<List<EqualizerPresetEntity>>

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSongById(songId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqualizerPresetEntity)

    @Delete
    suspend fun deletePreset(preset: EqualizerPresetEntity)

    // --- Playlist CRUD Queries ---
    @Query("SELECT * FROM playlists ORDER BY dateCreated DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: String)

    // --- Artist Summary Queries ---
    @Query("SELECT * FROM artist_summaries")
    fun getAllArtistSummariesFlow(): Flow<List<ArtistSummaryEntity>>

    @Query("SELECT * FROM artist_summaries WHERE artistName = :artistName LIMIT 1")
    suspend fun getArtistSummary(artistName: String): ArtistSummaryEntity?

    @Query("SELECT * FROM artist_summaries WHERE artistName = :artistName LIMIT 1")
    fun getArtistSummaryFlow(artistName: String): Flow<ArtistSummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtistSummary(artistSummary: ArtistSummaryEntity)

    @Query("DELETE FROM artist_summaries WHERE artistName = :artistName")
    suspend fun deleteArtistSummary(artistName: String)

    // --- Followed Artist Queries ---
    @Query("SELECT * FROM followed_artists ORDER BY followedAt DESC")
    fun getFollowedArtists(): Flow<List<FollowedArtistEntity>>

    @Query("SELECT * FROM followed_artists WHERE canonicalArtistId = :canonicalArtistId LIMIT 1")
    suspend fun getFollowedArtist(canonicalArtistId: String): FollowedArtistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowedArtist(artist: FollowedArtistEntity)

    @Query("DELETE FROM followed_artists WHERE canonicalArtistId = :canonicalArtistId")
    suspend fun deleteFollowedArtist(canonicalArtistId: String)

    @Query("""
        UPDATE followed_artists
        SET lastReleaseCheckAt = :checkedAt,
            lastSeenReleaseId = :lastSeenReleaseId
        WHERE canonicalArtistId = :canonicalArtistId
    """)
    suspend fun updateFollowedArtistReleaseSync(
        canonicalArtistId: String,
        checkedAt: Long,
        lastSeenReleaseId: String?
    )
    // --- Followed Artist Release Queries ---
    @Query("SELECT * FROM followed_artist_releases ORDER BY releaseDate DESC, firstSeenAt DESC")
    fun getFollowedArtistReleases(): Flow<List<FollowedArtistReleaseEntity>>

    @Query("SELECT * FROM followed_artist_releases WHERE canonicalArtistId IN (:artistIds) ORDER BY releaseDate DESC, firstSeenAt DESC")
    fun getFollowedArtistReleasesForArtists(artistIds: List<String>): Flow<List<FollowedArtistReleaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowedArtistReleases(releases: List<FollowedArtistReleaseEntity>)
    // --- Download Tasks ---
    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun getDownloadTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE id = :id LIMIT 1")
    suspend fun getDownloadTask(id: String): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownloadTask(task: DownloadTaskEntity)

    @Query("UPDATE download_tasks SET status = :status, progressPercent = :progressPercent, bytesDownloaded = :bytesDownloaded, totalBytes = :totalBytes, errorMessage = :errorMessage, updatedAt = :updatedAt, localUri = :localUri, completedAt = :completedAt WHERE id = :id")
    suspend fun updateDownloadProgress(id: String, status: String, progressPercent: Int, bytesDownloaded: Long, totalBytes: Long, errorMessage: String?, updatedAt: Long, localUri: String?, completedAt: Long?)

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteDownloadTask(id: String)
}

