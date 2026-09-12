package com.example.ui.library.model

import com.example.data.entity.SongEntity
import com.example.data.entity.ArtistSummaryEntity
import com.example.data.entity.PlaylistEntity
import org.json.JSONArray
import java.io.File

fun List<SongEntity>.toAlbumUiModels(): List<AlbumUiModel> {
    return groupBy { song ->
        val album = song.displayAlbum.ifBlank { "Unknown Album" }
        "$album|${song.displayAlbumArtist}"
    }.map { (key, songsInGroup) ->
        val firstSong = songsInGroup.first()
        val title = firstSong.displayAlbum.ifBlank { "Unknown Album" }
        val albumArtist = firstSong.displayAlbumArtist
        val artist = if (albumArtist.isNotBlank()) {
            albumArtist
        } else {
            firstSong.displayArtist.ifBlank { "Unknown Artist" }
        }
        val artworkUri = songsInGroup.firstNotNullOfOrNull { it.albumArtUri }
        val totalDurationMs = songsInGroup.sumOf { it.duration }
        
        AlbumUiModel(
            albumKey = key,
            title = title,
            artist = artist,
            artworkUri = artworkUri,
            songCount = songsInGroup.size,
            totalDurationMs = totalDurationMs
        )
    }.sortedBy { it.title.lowercase() }
}

fun List<SongEntity>.toArtistUiModels(summaries: List<ArtistSummaryEntity> = emptyList()): List<ArtistUiModel> {
    val summaryMap = summaries.associateBy { it.artistName }
    return groupBy { song ->
        song.displayArtist.ifBlank { "Unknown Artist" }
    }.map { (artistKey, songsInGroup) ->
        val albumCount = songsInGroup.map { song ->
            val album = song.displayAlbum.ifBlank { "Unknown Album" }
            "$album|${song.displayAlbumArtist}"
        }.distinct().size

        ArtistUiModel(
            artistKey = artistKey,
            name = artistKey,
            albumCount = albumCount,
            songCount = songsInGroup.size,
            artworkUri = summaryMap[artistKey]?.artworkUri ?: songsInGroup.firstOrNull { !it.albumArtUri.isNullOrBlank() }?.albumArtUri
        )
    }.sortedBy { it.name.lowercase() }
}

fun List<SongEntity>.toFolderUiModels(): List<FolderUiModel> {
    return groupBy { song ->
        try {
            File(song.filePath).parentFile?.name ?: "Internal"
        } catch (e: Exception) {
            "Internal"
        }
    }.map { (folderName, songsInGroup) ->
        val artworkUri = songsInGroup.firstNotNullOfOrNull { it.albumArtUri?.takeIf { uri -> uri.isNotBlank() } }
        FolderUiModel(
            folderPath = folderName,
            displayName = folderName,
            songCount = songsInGroup.size,
            artworkUri = artworkUri
        )
    }.sortedBy { it.displayName.lowercase() }
}

fun List<SongEntity>.toGenreUiModels(): List<GenreUiModel> {
    return groupBy { song ->
        song.genre.ifEmpty { "General" }
    }.map { (genre, songsInGroup) ->
        val artworkUri = songsInGroup.firstNotNullOfOrNull { it.albumArtUri?.takeIf { uri -> uri.isNotBlank() } }
        GenreUiModel(
            genre = genre,
            songCount = songsInGroup.size,
            artworkUri = artworkUri
        )
    }.sortedBy { it.genre.lowercase() }
}

fun List<PlaylistEntity>.toPlaylistUiModels(songs: List<SongEntity>): List<PlaylistUiModel> {
    val songMap = songs.associateBy { it.id }
    return map { playlist ->
        val songIds = try {
            val array = JSONArray(playlist.songIdsJson)
            List(array.length()) { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
        val matchedSongs = songIds.mapNotNull { songMap[it] }
        val artworkUri = matchedSongs.firstNotNullOfOrNull { it.albumArtUri?.takeIf { uri -> uri.isNotBlank() } }
        val totalDurationMs = matchedSongs.sumOf { it.duration }

        PlaylistUiModel(
            id = playlist.id,
            name = playlist.name,
            songCount = songIds.size,
            songIds = songIds,
            totalDurationMs = totalDurationMs,
            artworkUri = artworkUri
        )
    }.sortedBy { it.name.lowercase() }
}

fun List<SongEntity>.toLibraryStatistics(): LibraryStatisticsUiModel {
    val totalSongs = size
    val totalArtists = map { it.displayArtist.ifBlank { "Unknown Artist" } }.distinct().size
    val totalAlbums = map { song ->
        val album = song.displayAlbum.ifBlank { "Unknown Album" }
        "$album|${song.displayAlbumArtist}"
    }.distinct().size
    val totalGenres = map { it.displayGenre.ifBlank { "Unknown" } }.distinct().size
    val totalPlaytimeMs = sumOf { it.duration }
    val librarySizeBytes = sumOf { song ->
        try {
            val file = File(song.filePath)
            if (file.exists() && file.isFile) file.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }
    return LibraryStatisticsUiModel(
        totalSongs = totalSongs,
        totalArtists = totalArtists,
        totalAlbums = totalAlbums,
        totalGenres = totalGenres,
        totalPlaytimeMs = totalPlaytimeMs,
        librarySizeBytes = librarySizeBytes
    )
}
