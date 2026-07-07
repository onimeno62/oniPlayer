package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String, // Can be MediaStore ID or unique path
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val duration: Long,
    val filePath: String,
    val albumArtUri: String?,
    val lyrics: String? = null,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    
    // Additional standard fields
    val albumArtist: String = "",
    val composer: String = "",
    val disc: String = "",
    val track: String = "",
    val year: String = "",
    val comment: String = "",
    val bpm: String = "",

    // Smart playlist & search filters fields
    val lastPlayedTimestamp: Long = 0,
    val rating: Int = 0, // 0 to 5 stars
    val dateAdded: Long = System.currentTimeMillis(),
    val bitrate: Int = 320, // kbps
    val format: String = "MP3", // e.g. MP3, FLAC, AAC

    // User edits override physical tags
    val customTitle: String? = null,
    val customArtist: String? = null,
    val customAlbum: String? = null,
    val customGenre: String? = null,
    val customAlbumArtist: String? = null,
    val customComposer: String? = null,
    val customDisc: String? = null,
    val customTrack: String? = null,
    val customYear: String? = null,
    val customComment: String? = null,
    val customBpm: String? = null
) {
    val displayTitle: String get() = customTitle ?: title
    val displayArtist: String get() = customArtist ?: artist
    val displayAlbum: String get() = customAlbum ?: album
    val displayGenre: String get() = customGenre ?: genre
    val displayAlbumArtist: String get() = customAlbumArtist ?: albumArtist
    val displayComposer: String get() = customComposer ?: composer
    val displayDisc: String get() = customDisc ?: disc
    val displayTrack: String get() = customTrack ?: track
    val displayYear: String get() = customYear ?: year
    val displayComment: String get() = customComment ?: comment
    val displayBpm: String get() = customBpm ?: bpm
}
