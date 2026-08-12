package com.example.ui.library.model

data class AlbumUiModel(
    val albumKey: String,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val songCount: Int,
    val totalDurationMs: Long
)

data class ArtistUiModel(
    val artistKey: String,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val artworkUri: String?
)

data class FolderUiModel(
    val folderPath: String,
    val displayName: String,
    val songCount: Int
)

data class GenreUiModel(
    val genre: String,
    val songCount: Int
)

data class LibrarySortState(
    val field: LibrarySortField,
    val ascending: Boolean
)

enum class LibrarySortField {
    TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION, PLAY_COUNT
}

enum class LibraryLayoutMode {
    LIST, GRID
}

data class LibraryStatisticsUiModel(
    val totalSongs: Int,
    val totalArtists: Int,
    val totalAlbums: Int,
    val totalGenres: Int,
    val totalPlaytimeMs: Long,
    val librarySizeBytes: Long
)
