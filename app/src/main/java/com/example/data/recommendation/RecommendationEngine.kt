package com.example.data.recommendation

import com.example.data.entity.SongEntity
import com.example.data.online.ProviderIdentity
import com.example.data.online.RemoteMusicItem
import kotlin.math.max
import kotlin.math.min

enum class RecommendationReason {
    BecauseYouPlayed,
    SimilarTo,
    NewRelease,
    Trending,
    YouHaventPlayedInAWhile,
    FavoriteAffinity,
    ColdStart
}

data class Recommendation(
    val localSong: SongEntity? = null,
    val remoteItem: RemoteMusicItem? = null,
    val score: Double,
    val reason: RecommendationReason
)

data class RecommendationSnapshot(
    val localSongs: List<SongEntity>,
    val externalCandidates: List<RemoteMusicItem> = emptyList(),
    val recentExternalIds: Set<ProviderIdentity> = emptySet(),
    val seed: Long = 0L
)

class RecommendationEngine {
    fun recommend(
        snapshot: RecommendationSnapshot,
        limit: Int = 15
    ): List<Recommendation> {
        require(limit >= 0)

        val local = rankLocal(snapshot).take(limit)
        val remaining = (limit - local.size).coerceAtLeast(0)
        if (remaining == 0) return local

        return local + rankRemote(snapshot, remaining)
    }

    private fun rankLocal(snapshot: RecommendationSnapshot): List<Recommendation> {
        val songs = snapshot.localSongs
        if (songs.isEmpty()) return emptyList()

        val now = songs.maxOfOrNull { it.lastPlayedTimestamp } ?: 0L
        val played = songs.filter { it.playCount > 0 || it.isFavorite || it.lastPlayedTimestamp > 0 }
        val genreAffinity = affinity(played.map { it.displayGenre })
        val artistAffinity = affinity(played.map { it.displayArtist })
        val albumAffinity = affinity(played.map { it.displayAlbum })

        val candidates = songs
            .filter { it.filePath.isNotBlank() }
            .map { song ->
                val personal = normalizedAffinity(
                    artistAffinity[song.displayArtist.trim().lowercase()],
                    albumAffinity[song.displayAlbum.trim().lowercase()],
                    genreAffinity[song.displayGenre.trim().lowercase()]
                )
                val favorite = if (song.isFavorite) 0.25 else 0.0
                val playSignal = min(song.playCount / 10.0, 1.0) * 0.15
                val freshness = if (song.lastPlayedTimestamp <= 0L || now <= 0L) {
                    0.20
                } else {
                    val ageDays = ((now - song.lastPlayedTimestamp).coerceAtLeast(0L) / 86_400_000.0)
                    min(ageDays / 30.0, 1.0) * 0.25
                }
                val novelty = if (song.playCount == 0) 0.15 else 0.0
                val repetitionPenalty = if (song.lastPlayedTimestamp > 0L && freshness < 0.05) 0.20 else 0.0
                val score = personal * 0.45 + favorite + playSignal + freshness + novelty - repetitionPenalty

                val reason = when {
                    song.isFavorite -> RecommendationReason.FavoriteAffinity
                    artistAffinity[song.displayArtist] ?: 0.0 > 0.0 -> RecommendationReason.BecauseYouPlayed
                    freshness >= 0.18 -> RecommendationReason.YouHaventPlayedInAWhile
                    else -> RecommendationReason.ColdStart
                }
                Recommendation(song, null, score, reason)
            }

        return diversifyLocal(candidates.sortedWith(compareByDescending<Recommendation> { it.score }
            .thenBy { it.localSong!!.displayArtist.lowercase() }
            .thenBy { it.localSong?.displayTitle.orEmpty().lowercase() }))
    }

    private fun rankRemote(snapshot: RecommendationSnapshot, limit: Int): List<Recommendation> {
        if (snapshot.externalCandidates.isEmpty()) return emptyList()

        val local = snapshot.localSongs
        val preferredArtists = affinity(local.filter { it.playCount > 0 || it.isFavorite }.map { it.displayArtist })
        val preferredGenres = affinity(local.filter { it.playCount > 0 || it.isFavorite }.map { it.displayGenre })

        val ranked = snapshot.externalCandidates
            .distinctBy { it.identity.providerId + ":" + it.identity.type + ":" + it.identity.itemId }
            .filter { it.identity !in snapshot.recentExternalIds }
            .map { item ->
                val artistAffinity = preferredArtists[item.artistName.orEmpty().trim().lowercase()].orZero()
                val genreAffinity = preferredGenres[item.metadata["genre"].orEmpty().trim().lowercase()].orZero()
                val freshness = if (item.metadata["release_date"].isNullOrBlank()) 0.0 else 0.20
                val trending = if (item.metadata["source"] == "trending") 0.25 else 0.0
                val score = artistAffinity * 0.55 + genreAffinity * 0.20 + freshness + trending

                val reason = when {
                    trending > 0.0 -> RecommendationReason.Trending
                    freshness > 0.0 -> RecommendationReason.NewRelease
                    artistAffinity > 0.0 -> RecommendationReason.SimilarTo
                    else -> RecommendationReason.ColdStart
                }
                Recommendation(null, item, score, reason)
            }
            .sortedWith(compareByDescending<Recommendation> { it.score }
                .thenBy { it.remoteItem?.artistName.orEmpty().lowercase() }
                .thenBy { it.remoteItem?.title.orEmpty().lowercase() })

        return diversifyRemote(ranked, limit)
    }

    private fun diversifyLocal(candidates: List<Recommendation>): List<Recommendation> {
        val selected = mutableListOf<Recommendation>()
        val artistCounts = mutableMapOf<String, Int>()
        for (candidate in candidates) {
            val artist = candidate.localSong?.displayArtist?.lowercase() ?: continue
            val count = artistCounts[artist] ?: 0
            if (count >= 3 && candidates.size > 3) continue
            selected += candidate
            artistCounts[artist] = count + 1
        }
        return selected
    }

    private fun diversifyRemote(candidates: List<Recommendation>, limit: Int): List<Recommendation> {
        val selected = mutableListOf<Recommendation>()
        val providerCounts = mutableMapOf<String, Int>()
        for (candidate in candidates) {
            if (selected.size >= limit) break
            val provider = candidate.remoteItem?.identity?.providerId ?: continue
            val count = providerCounts[provider] ?: 0
            if (count >= 5 && candidates.size > 5) continue
            selected += candidate
            providerCounts[provider] = count + 1
        }
        return selected
    }

    private fun affinity(values: List<String>): Map<String, Double> {
        val counts = values.map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
        val maxCount = counts.values.maxOrNull()?.toDouble() ?: 1.0
        return counts.mapValues { it.value / maxCount }
    }

    private fun normalizedAffinity(
        artist: Double?,
        album: Double?,
        genre: Double?
    ): Double = (artist.orZero() * 0.55 + album.orZero() * 0.20 + genre.orZero() * 0.25).coerceIn(0.0, 1.0)

    private fun Double?.orZero(): Double = this ?: 0.0
}
