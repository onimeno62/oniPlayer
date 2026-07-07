package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val songIdsJson: String = "[]", // JSON array of song IDs (e.g. ["id1", "id2"])
    val dateCreated: Long = System.currentTimeMillis()
)
