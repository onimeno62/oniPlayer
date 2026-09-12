package com.example.ui.lyrics

/**
 * Expected vocal pitch target for a timestamp range in a song.
 * Designed for future karaoke pitch comparison and scoring without faking current results.
 */
data class ExpectedPitchNote(
    val timestampMs: Long,
    val durationMs: Long,
    val noteName: String,
    val frequencyHz: Float,
    val lyricSyllable: String = ""
)

/**
 * Clean domain interface for future karaoke phrase scoring & evaluation.
 * Does not emit fake scores; provides contracts for pitch comparison once expected vocal data is loaded.
 */
interface KaraokeScoringEngine {
    fun evaluatePitch(detectedHz: Float, targetNote: ExpectedPitchNote): Float
    fun resetPhraseScore()
}
