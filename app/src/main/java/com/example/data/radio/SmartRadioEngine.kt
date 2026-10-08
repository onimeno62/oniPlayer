package com.example.data.radio

import com.example.data.entity.SongEntity
import com.example.data.online.RemoteMusicItem
import com.example.data.online.ProviderIdentity
import com.example.data.recommendation.RecommendationEngine
import com.example.data.recommendation.RecommendationSnapshot

data class RadioSeed(
    val songId: String? = null,
    val artist: String? = null,
    val genre: String? = null,
    val providerItem: ProviderIdentity? = null
)

class SmartRadioEngine(
    private val recommendationEngine: RecommendationEngine = RecommendationEngine()
) {
    fun buildQueue(
        seed: RadioSeed,
        localSongs: List<SongEntity>,
        remoteCandidates: List<RemoteMusicItem> = emptyList(),
        recentIds: Set<String> = emptySet(),
        limit: Int = 30
    ): List<RadioItem> {
        require(limit >= 0)
        val snapshot = RecommendationSnapshot(
            localSongs = localSongs,
            externalCandidates = remoteCandidates,
            recentExternalIds = remoteCandidates.map { it.identity }.filter { it.itemId in recentIds }.toSet()
        )
        return recommendationEngine.recommend(snapshot, limit * 2)
            .asSequence()
            .filter { recommendation ->
                val localId = recommendation.localSong?.id
                localId == null || localId != seed.songId
            }
            .map { recommendation ->
                when {
                    recommendation.localSong != null -> RadioItem.Local(recommendation.localSong)
                    recommendation.remoteItem != null -> RadioItem.Remote(recommendation.remoteItem)
                    else -> null
                }
            }
            .filterNotNull()
            .distinctBy {
                when (it) {
                    is RadioItem.Local -> "local:" + it.song.id
                    is RadioItem.Remote -> "remote:" + it.item.identity.providerId + ":" + it.item.identity.itemId
                }
            }
            .take(limit)
            .toList()
    }
}

sealed interface RadioItem {
    data class Local(val song: SongEntity) : RadioItem
    data class Remote(val item: RemoteMusicItem) : RadioItem
}
