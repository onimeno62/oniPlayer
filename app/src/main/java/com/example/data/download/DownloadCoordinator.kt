package com.example.data.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.database.OniDatabase
import com.example.data.online.MusicProvider
import com.example.data.online.ProviderCapability
import com.example.data.online.RemoteTrack
import java.util.concurrent.TimeUnit

class DownloadCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val dao = OniDatabase.getDatabase(appContext).songDao()
    private val workManager = WorkManager.getInstance(appContext)

    suspend fun enqueue(provider: MusicProvider, track: RemoteTrack): String? {
        if (!provider.capabilities.supports(ProviderCapability.DOWNLOAD)) return null
        val task = DownloadManager.createTask(track, provider.id) ?: return null
        dao.upsertDownloadTask(task)
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(Data.Builder().putString(DownloadWorker.KEY_TASK_ID, task.id).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("download:" + task.id)
            .build()
        workManager.enqueueUniqueWork(task.id, ExistingWorkPolicy.KEEP, request)
        return task.id
    }

    suspend fun retry(taskId: String) {
        val task = dao.getDownloadTask(taskId) ?: return
        if (task.status != DownloadManager.FAILED) return
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(Data.Builder().putString(DownloadWorker.KEY_TASK_ID, task.id).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("download:" + task.id)
            .build()
        workManager.enqueueUniqueWork(task.id, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(taskId: String) { workManager.cancelUniqueWork(taskId) }
}