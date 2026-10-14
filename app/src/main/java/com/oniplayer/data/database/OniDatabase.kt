package com.oniplayer.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.oniplayer.data.entity.SongEntity
import com.oniplayer.data.entity.EqualizerPresetEntity
import com.oniplayer.data.entity.PlaylistEntity
import com.oniplayer.data.entity.ArtistSummaryEntity
import com.oniplayer.data.entity.FollowedArtistEntity
import com.oniplayer.data.entity.FollowedArtistReleaseEntity
import com.oniplayer.data.entity.DownloadTaskEntity

@Database(
    entities = [SongEntity::class, EqualizerPresetEntity::class, PlaylistEntity::class, ArtistSummaryEntity::class, FollowedArtistEntity::class, FollowedArtistReleaseEntity::class, DownloadTaskEntity::class],
    version = 8,
    exportSchema = true
)
abstract class OniDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS followed_artists (
                        canonicalArtistId TEXT NOT NULL PRIMARY KEY,
                        artistName TEXT NOT NULL,
                        artworkUrl TEXT,
                        followedAt INTEGER NOT NULL,
                        lastReleaseCheckAt INTEGER NOT NULL,
                        lastSeenReleaseId TEXT
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS followed_artist_releases (
                        canonicalArtistId TEXT NOT NULL,
                        releaseId TEXT NOT NULL,
                        providerId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        artistName TEXT,
                        artworkUrl TEXT,
                        releaseDate TEXT,
                        firstSeenAt INTEGER NOT NULL,
                        PRIMARY KEY(canonicalArtistId, releaseId)
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS download_tasks (
                        id TEXT NOT NULL PRIMARY KEY,
                        providerId TEXT NOT NULL,
                        remoteId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        artistName TEXT,
                        albumName TEXT,
                        artworkUrl TEXT,
                        sourceUrl TEXT NOT NULL,
                        localUri TEXT,
                        status TEXT NOT NULL,
                        progressPercent INTEGER NOT NULL,
                        bytesDownloaded INTEGER NOT NULL,
                        totalBytes INTEGER NOT NULL,
                        errorMessage TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        completedAt INTEGER
                    )
                """.trimIndent())
            }
        }

        @Volatile
        private var INSTANCE: OniDatabase? = null

        fun getDatabase(context: Context): OniDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OniDatabase::class.java,
                    "oni_player_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                // Only the legacy versions that never had migrations may be wiped.
                // Any future schema bump without a migration now fails loudly
                // instead of silently deleting playlists, favorites and play counts.
                .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
