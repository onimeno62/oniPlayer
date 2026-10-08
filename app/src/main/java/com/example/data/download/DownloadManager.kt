package com.example.data.download

import com.example.data.entity.DownloadTaskEntity
import com.example.data.online.RemoteTrack
import java.security.MessageDigest

object DownloadManager {
    const val QUEUED = "Queued"
    const val DOWNLOADING = "Downloading"
    const val PAUSED = "Paused"
    const val COMPLETED = "Completed"
    const val FAILED = "Failed"
    const val CANCELLED = "Cancelled"

    fun taskId(providerId: String, remoteId: String): String =
        sha256("$providerId:$remoteId")

    fun canDownload(provider: com.example.data.online.MusicProvider): Boolean =
        provider.capabilities.supports(com.example.data.online.ProviderCapability.DOWNLOAD)

    fun createTask(
        track: RemoteTrack,
        providerId: String
    ): DownloadTaskEntity? {
        val url = track.downloadUrl ?: return null
        return DownloadTaskEntity(
            id = taskId(providerId, track.item.identity.itemId),
            providerId = providerId,
            remoteId = track.item.identity.itemId,
            title = track.item.title,
            artistName = track.item.artistName,
            albumName = track.item.albumName,
            artworkUrl = track.item.artworkUrl,
            sourceUrl = url
        )
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
