package com.example.data.playlist

import com.example.data.entity.SongEntity

enum class SmartPlaylistRule {
    Favorites,
    HighlyRated,
    MostPlayed,
    RecentlyPlayed,
    RecentlyAdded,
    NeverPlayed,
    Longest,
    Shortest
}

data class SmartPlaylistSpec(
    val rules: Set<SmartPlaylistRule>,
    val genre: String? = null,
    val artist: String? = null,
    val minimumRating: Int = 0,
    val limit: Int = 50
)

class SmartPlaylistEngine {
    fun build(spec: SmartPlaylistSpec, songs: List<SongEntity>): List<SongEntity> {
        require(spec.limit >= 0)
        val filtered = songs.asSequence()
            .filter { song -> spec.genre.isNullOrBlank() || song.displayGenre.equals(spec.genre, ignoreCase = true) }
            .filter { song -> spec.artist.isNullOrBlank() || song.displayArtist.equals(spec.artist, ignoreCase = true) }
            .filter { song -> song.rating >= spec.minimumRating }
            .filter { song -> matchesRules(spec.rules, song) }
            .sortedWith(comparator(spec.rules))
            .take(spec.limit)
            .toList()
        return filtered
    }

    private fun matchesRules(rules: Set<SmartPlaylistRule>, song: SongEntity): Boolean {
        if (rules.isEmpty()) return true
        return rules.any { rule ->
            when (rule) {
                SmartPlaylistRule.Favorites -> song.isFavorite
                SmartPlaylistRule.HighlyRated -> song.rating >= 4
                SmartPlaylistRule.MostPlayed -> song.playCount > 0
                SmartPlaylistRule.RecentlyPlayed -> song.lastPlayedTimestamp > 0
                SmartPlaylistRule.RecentlyAdded -> song.dateAdded > 0
                SmartPlaylistRule.NeverPlayed -> song.playCount == 0 && song.lastPlayedTimestamp == 0L
                SmartPlaylistRule.Longest, SmartPlaylistRule.Shortest -> true
            }
        }
    }

    private fun comparator(rules: Set<SmartPlaylistRule>): Comparator<SongEntity> {
        return when {
            SmartPlaylistRule.MostPlayed in rules -> compareByDescending<SongEntity> { it.playCount }.thenByDescending { it.lastPlayedTimestamp }
            SmartPlaylistRule.RecentlyPlayed in rules -> compareByDescending<SongEntity> { it.lastPlayedTimestamp }.thenByDescending { it.playCount }
            SmartPlaylistRule.RecentlyAdded in rules -> compareByDescending<SongEntity> { it.dateAdded }
            SmartPlaylistRule.Longest in rules -> compareByDescending<SongEntity> { it.duration }
            SmartPlaylistRule.Shortest in rules -> compareBy<SongEntity> { it.duration }
            SmartPlaylistRule.HighlyRated in rules -> compareByDescending<SongEntity> { it.rating }.thenByDescending { it.playCount }
            else -> compareByDescending<SongEntity> { it.isFavorite }.thenByDescending { it.playCount }
        }
    }
}
