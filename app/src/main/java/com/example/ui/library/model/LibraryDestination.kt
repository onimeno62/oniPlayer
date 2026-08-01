package com.example.ui.library.model

sealed interface LibraryDestination {
    data object Dashboard : LibraryDestination
    data object Categories : LibraryDestination
    data class Category(val type: LibraryCategoryType) : LibraryDestination
    data class AlbumDetails(val albumKey: String) : LibraryDestination
    data class ArtistDetails(val artistKey: String) : LibraryDestination
    data class FolderDetails(val folderPath: String) : LibraryDestination
    data class GenreDetails(val genre: String) : LibraryDestination
    data class PlaylistDetails(val playlistId: String) : LibraryDestination
    data class SmartPlaylistDetails(val type: String) : LibraryDestination
    data object Search : LibraryDestination
    data object Statistics : LibraryDestination
    data object Queue : LibraryDestination
}

enum class LibraryCategoryType {
    SONGS, ALBUMS, ARTISTS, GENRES, FOLDERS, PLAYLISTS
}
