package com.example.data.following

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.OniDatabase
import com.example.data.online.DefaultMusicProviders
import com.example.data.online.OnlineMusicRepository

class ArtistReleaseSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val dao = OniDatabase.getDatabase(applicationContext).songDao()
        val sync = ArtistReleaseSync(
            followRepository = ArtistFollowRepository(dao),
            onlineRepository = OnlineMusicRepository(DefaultMusicProviders.create())
        )
        val result = sync.sync()
        return if (result.failedArtistIds.isNotEmpty() && result.releases.isEmpty()) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
