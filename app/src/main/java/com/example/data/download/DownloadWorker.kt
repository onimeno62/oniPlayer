package com.example.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.data.database.OniDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class DownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val dao = OniDatabase.getDatabase(applicationContext).songDao()
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return@withContext Result.failure()
        val task = dao.getDownloadTask(taskId) ?: return@withContext Result.failure()
        setForeground(createForegroundInfo(task.title, 0))
        try {
            dao.updateDownloadProgress(taskId, DownloadManager.DOWNLOADING, 0, 0, 0, null, System.currentTimeMillis(), null, null)
            client.newCall(Request.Builder().url(task.sourceUrl).get().build()).execute().use { response ->
                if (!response.isSuccessful) {
                    val message = "HTTP " + response.code
                    dao.updateDownloadProgress(taskId, DownloadManager.FAILED, 0, 0, 0, message, System.currentTimeMillis(), null, null)
                    return@withContext if (response.code in 500..599) Result.retry() else Result.failure()
                }
                val body = response.body ?: return@withContext Result.retry()
                val total = body.contentLength().coerceAtLeast(0L)
                val destination = MediaStoreDestination(applicationContext, task.title, task.remoteId)
                val target = destination.open()
                try {
                    body.byteStream().use { input ->
                        target.output.use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var downloaded = 0L
                            while (true) {
                                ensureActive()
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                val progress = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0
                                dao.updateDownloadProgress(taskId, DownloadManager.DOWNLOADING, progress, downloaded, total, null, System.currentTimeMillis(), null, null)
                            }
                            output.flush()
                        }
                    }
                    val localUri = destination.complete(target)
                    dao.updateDownloadProgress(taskId, DownloadManager.COMPLETED, 100, total, total, null, System.currentTimeMillis(), localUri.toString(), System.currentTimeMillis())
                    notifyCompleted(task.title)
                    Result.success()
                } catch (error: Exception) {
                    destination.abort(target)
                    throw error
                }
            }
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            dao.updateDownloadProgress(taskId, DownloadManager.FAILED, 0, 0, 0, error.message, System.currentTimeMillis(), null, null)
            Result.retry()
        }
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading")
            .setContentText(title)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun notifyCompleted(title: String) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(
            NOTIFICATION_ID + 1,
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download complete")
                .setContentText(title)
                .setAutoCancel(true)
                .build()
        )
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        private const val CHANNEL_ID = "downloads"
        private const val NOTIFICATION_ID = 7301
    }
}

private data class MediaStoreTarget(val uri: Uri, val output: java.io.OutputStream, val file: File?)

private class MediaStoreDestination(private val context: Context, private val title: String, private val remoteId: String) {
    fun open(): MediaStoreTarget {
        val fileName = sanitize(title) + "-" + remoteId.take(8) + ".mp3"
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/oniPlayer")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: error("Unable to create MediaStore item")
            MediaStoreTarget(uri, resolver.openOutputStream(uri) ?: error("Unable to open MediaStore output"), null)
        } else {
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, fileName)
            MediaStoreTarget(Uri.fromFile(file), file.outputStream(), file)
        }
    }

    fun complete(target: MediaStoreTarget): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.update(target.uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
        } else {
            MediaScannerConnection.scanFile(context, arrayOf(target.file!!.absolutePath), arrayOf("audio/mpeg"), null)
        }
        return target.uri
    }

    fun abort(target: MediaStoreTarget) {
        runCatching { target.output.close() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.contentResolver.delete(target.uri, null, null) else target.file?.delete()
    }

    private fun sanitize(value: String): String = value.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "oniPlayer-download" }
}
