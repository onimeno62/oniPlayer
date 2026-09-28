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

    /**
     * Poweramp-style shuffle order for [queue]. The current song always plays first.
     *
     * - [ShuffleType.SONGS] / [ShuffleType.ALL]: every song in random (weighted) order.
     * - [ShuffleType.CATEGORIES]: the current album first (from the current song, in list order),
     *   then the remaining albums in random order, each played in list order.
     * - [ShuffleType.SONGS_AND_CATEGORIES]: albums in random order, songs shuffled inside each album.
     *
     * The result is always a complete permutation of `0 until queue.size`.
     */
    fun calculateOrder(
        queue: List<SongEntity>,
        currentId: String?,
        shuffleMode: ShuffleMode,
        shuffleType: ShuffleType,
        random: Random = Random.Default
    ): IntArray {
        if (queue.isEmpty()) return IntArray(0)
        val raw = when (shuffleType) {
            ShuffleType.SONGS, ShuffleType.ALL -> calculateShuffleIndices(queue, currentId, shuffleMode, random)
            ShuffleType.CATEGORIES -> categoryOrder(queue, currentId, shuffleMode, random, shuffleSongs = false)
            ShuffleType.SONGS_AND_CATEGORIES -> categoryOrder(queue, currentId, shuffleMode, random, shuffleSongs = true)
        }
        return completePermutation(raw, queue.size)
    }

    /** Category (album) grouping key. Falls back to the containing folder when the album tag is empty. */
    fun categoryKey(song: SongEntity): String {
        val album = song.displayAlbum.trim()
        if (album.isNotEmpty() && !album.equals("<unknown>", ignoreCase = true)) return album.lowercase()
        return "folder:" + song.filePath.substringBeforeLast('/', "")
    }

    private fun categoryOrder(
        queue: List<SongEntity>,
        currentId: String?,
        shuffleMode: ShuffleMode,
        random: Random,
        shuffleSongs: Boolean
    ): IntArray {
        val groups = LinkedHashMap<String, MutableList<Int>>()
        queue.forEachIndexed { index, song ->
            groups.getOrPut(categoryKey(song)) { mutableListOf() }.add(index)
        }
        val currentIndex = queue.indexOfFirst { it.id == currentId }.let { if (it < 0) 0 else it }
        val currentKey = categoryKey(queue[currentIndex])
        val otherKeys = groups.keys.filter { it != currentKey }.shuffled(random)

        val result = ArrayList<Int>(queue.size)
        result += orderGroup(queue, groups.getValue(currentKey), currentIndex, shuffleMode, random, shuffleSongs)
        otherKeys.forEach { key ->
            result += orderGroup(queue, groups.getValue(key), null, shuffleMode, random, shuffleSongs)
        }
        return result.toIntArray()
    }

    private fun orderGroup(
        queue: List<SongEntity>,
        indices: List<Int>,
        startIndex: Int?,
        shuffleMode: ShuffleMode,
        random: Random,
        shuffleSongs: Boolean
    ): List<Int> {
        if (indices.size <= 1) return indices
        if (shuffleSongs) {
            // Shuffle positions (not ids) so duplicate ids can never drop an index.
            val remaining = indices.toMutableList()
            val result = ArrayList<Int>(indices.size)
            val first = if (startIndex != null && startIndex in remaining) startIndex else remaining[random.nextInt(remaining.size)]
            remaining.remove(first)
            result += first
            while (remaining.isNotEmpty()) {
                val weights = remaining.map { index ->
                    val song = queue[index]
                    when (shuffleMode) {
                        ShuffleMode.RANDOM -> 1.0
                        ShuffleMode.DISCOVER -> 1.0 / (1.0 + song.playCount)
                        ShuffleMode.FAVORITES_BOOST -> if (song.isFavorite) 4.0 else 1.0
                    }
                }
                var roll = random.nextDouble() * weights.sum()
                val pick = weights.indexOfFirst {
                    roll -= it
                    roll <= 0
                }.let { if (it < 0) remaining.lastIndex else it }
                result += remaining.removeAt(pick)
            }
            return result
        }
        // Album order, rotated so the current song plays first; earlier songs follow at the end.
        if (startIndex == null) return indices
        val pos = indices.indexOf(startIndex)
        return if (pos <= 0) indices else indices.drop(pos) + indices.take(pos)
    }

    /** Guarantees a valid permutation for ExoPlayer's ShuffleOrder (distinct, in range, complete). */
    internal fun completePermutation(order: IntArray, size: Int): IntArray {
        val seen = BooleanArray(size)
        val result = ArrayList<Int>(size)
        for (i in order) {
            if (i in 0 until size && !seen[i]) {
                seen[i] = true
                result += i
            }
        }
        for (i in 0 until size) if (!seen[i]) result += i
        return result.toIntArray()
    }
}
