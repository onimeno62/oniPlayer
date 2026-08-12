package com.example.ui.library.model

import com.example.data.entity.SongEntity
import com.example.data.entity.ArtistSummaryEntity
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
            File(song.filePath).parent ?: "Internal"
        } catch (e: Exception) {
            "Internal"
        }
    }.map { (folderPath, songsInGroup) ->
        val displayName = try {
            File(folderPath).name.ifBlank { "Internal" }
        } catch (e: Exception) {
            "Internal"
        }
        FolderUiModel(
            folderPath = folderPath,
            displayName = displayName,
            songCount = songsInGroup.size
        )
    }.sortedBy { it.displayName.lowercase() }
}

fun List<SongEntity>.toGenreUiModels(): List<GenreUiModel> {
    return groupBy { song ->
        song.displayGenre.ifBlank { "Unknown" }
    }.map { (genre, songsInGroup) ->
        GenreUiModel(
            genre = genre,
            songCount = songsInGroup.size
        )
    }.sortedBy { it.genre.lowercase() }
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
