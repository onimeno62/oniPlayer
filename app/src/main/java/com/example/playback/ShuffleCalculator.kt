package com.example.playback

import com.example.data.entity.SongEntity
import kotlin.random.Random

/**
 * Pure helper for calculating weighted shuffle transformations on a song queue.
 *
 * This object is independent of ExoPlayer, MediaSession, Android lifecycle, and database access.
 */
object ShuffleCalculator {

    /**
     * Shuffles [queue] using the weighting strategy specified by [shuffleMode], keeping the song
     * at [currentIndex] (or the first song if [currentIndex] is invalid) at the beginning of the result.
     *
     * @param queue The list of songs to shuffle.
     * @param currentIndex The index of the currently active song that should remain at index 0.
     * @param shuffleMode The weighting mode ([ShuffleMode.RANDOM], [ShuffleMode.DISCOVER], or [ShuffleMode.FAVORITES_BOOST]).
     * @param random The random instance to use for rolling weights (defaults to standard Random).
     */
    fun shuffleQueue(
        queue: List<SongEntity>,
        currentIndex: Int,
        shuffleMode: ShuffleMode,
        random: Random = Random.Default
    ): List<SongEntity> {
        if (queue.size <= 1) return queue
        val current = queue.getOrNull(currentIndex) ?: queue.first()
        val rest = queue.filterNot { it.id == current.id }.toMutableList()
        val result = mutableListOf(current)

        while (rest.isNotEmpty()) {
            val weights = rest.map { song ->
                when (shuffleMode) {
                    ShuffleMode.RANDOM -> 1.0
                    ShuffleMode.DISCOVER -> 1.0 / (1.0 + song.playCount)
                    ShuffleMode.FAVORITES_BOOST -> if (song.isFavorite) 4.0 else 1.0
                }
            }
            val total = weights.sum()
            var roll = random.nextDouble() * total
            val i = weights.indexOfFirst {
                roll -= it
                roll <= 0
            }.let { if (it < 0) rest.lastIndex else it }
            result += rest.removeAt(i)
        }
        return result
    }

    /**
     * Calculates the permutation of 0-based indices representing the shuffle order of [queue]
     * with [currentId] retained as the first item.
     *
     * @return An [IntArray] containing the indices corresponding to positions in [queue].
     */
    fun calculateShuffleIndices(
        queue: List<SongEntity>,
        currentId: String?,
        shuffleMode: ShuffleMode,
        random: Random = Random.Default
    ): IntArray {
        if (queue.isEmpty()) return IntArray(0)
        val currentIndex = queue.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        val target = shuffleQueue(queue, currentIndex, shuffleMode, random)
        return target.mapNotNull { song ->
            queue.indexOfFirst { it.id == song.id }.takeIf { it >= 0 }
        }.toIntArray()
    }
}
