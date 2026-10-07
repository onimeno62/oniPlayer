package com.example.data.recommendation

import com.example.data.entity.SongEntity

/**
 * Deterministic local recommendation engine.
 *
 * It only ranks songs already present in the local library. No network calls,
 * no playback control, and no mutation of library state.
 */
class RecommendationEngine {

    fun becauseYouPlayed(songs: List<SongEntity>, limit: Int = 15): List<SongEntity> {
        val anchors = songs
            .filter { it.playCount > 0 || it.isFavorite }
            .sortedWith(
                compareByDescending<SongEntity> { it.playCount }
                    .thenByDescending { it.lastPlayedTimestamp }
            )
            .take(8)

        if (anchors.isEmpty()) return emptyList()

        val artists = anchors.map { normalize(it.displayArtist) }.filter(String::isNotBlank).toSet()
        val genres = anchors.map { normalize(it.displayGenre) }.filter(::isUsefulGenre).toSet()
        val albums = anchors.map { normalize(it.displayAlbum) }.filter(String::isNotBlank).toSet()

        return songs
            .asSequence()
            .filter { it.playCount == 0 && !it.isFavorite }
            .map { song ->
                var score = 0
                var hasAffinity = false

                if (normalize(song.displayArtist) in artists) {
                    score += 100
                    hasAffinity = true
                }
                if (isUsefulGenre(song.displayGenre) && normalize(song.displayGenre) in genres) {
                    score += 45
                    hasAffinity = true
                }
                if (normalize(song.displayAlbum) in albums) {
                    score += 15
                    hasAffinity = true
                }
                score += minOf(song.rating.coerceAtLeast(0), 5) * 3

                if (hasAffinity) {
                    score += recencyBonus(song.dateAdded)
                }

                Ranked(song, score)
            }
            .filter { it.score > 0 }
            .sortedWith(compareByDescending<Ranked> { it.score }.thenByDescending { it.song.dateAdded })
            .take(limit)
            .map { it.song }
            .toList()
    }

    fun similarToFavorites(songs: List<SongEntity>, limit: Int = 15): List<SongEntity> {
        val seeds = songs
            .filter { it.isFavorite || it.rating >= 4 || it.playCount >= 3 }
            .sortedWith(
                compareByDescending<SongEntity> { it.rating }
                    .thenByDescending { it.playCount }
                    .thenByDescending { it.lastPlayedTimestamp }
            )
            .take(10)

        if (seeds.isEmpty()) return emptyList()

        val artists = seeds.map { normalize(it.displayArtist) }.filter(String::isNotBlank).toSet()
        val genres = seeds.map { normalize(it.displayGenre) }.filter(::isUsefulGenre).toSet()
        val albums = seeds.map { normalize(it.displayAlbum) }.filter(String::isNotBlank).toSet()

        return songs
            .asSequence()
            .filter { candidate ->
                candidate !in seeds &&
                    normalize(candidate.displayArtist) !in artists
            }
            .map { song ->
                var score = 0
                var hasAffinity = false

                if (isUsefulGenre(song.displayGenre) && normalize(song.displayGenre) in genres) {
                    score += 70
                    hasAffinity = true
                }
                if (normalize(song.displayAlbum) in albums) {
                    score += 20
                    hasAffinity = true
                }
                if (song.rating >= 3) score += song.rating * 2
                if (song.playCount > 0) score += minOf(song.playCount, 10)

                if (hasAffinity) {
                    score += recencyBonus(song.dateAdded)
                }

                Ranked(song, score)
            }
            .filter { it.score > 0 }
            .sortedWith(compareByDescending<Ranked> { it.score }.thenBy { it.song.displayTitle.lowercase() })
            .take(limit)
            .map { it.song }
            .toList()
    }

    private fun normalize(value: String): String = value.trim().lowercase()

    private fun isUsefulGenre(value: String): Boolean {
        val normalized = normalize(value)
        return normalized.isNotBlank() &&
            normalized != "unknown genre" &&
            normalized != "local audio"
    }

    private fun recencyBonus(timestamp: Long): Int {
        if (timestamp <= 0L) return 0
        val ageDays = ((System.currentTimeMillis() - timestamp).coerceAtLeast(0L) / DAY_MS).toInt()
        return when {
            ageDays <= 7 -> 8
            ageDays <= 30 -> 5
            ageDays <= 90 -> 2
            else -> 0
        }
    }

    private data class Ranked(val song: SongEntity, val score: Int)

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1_000L
    }
}
