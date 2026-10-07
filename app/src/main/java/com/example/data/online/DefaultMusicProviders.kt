package com.example.data.online

/**
 * Composition root for currently enabled public providers.
 *
 * YouTube is intentionally absent. It can be added later without changing
 * consumers of OnlineMusicRepository.
 */
object DefaultMusicProviders {
    fun create(): List<MusicProvider> = listOf(
        AudiusMusicProvider(),
        MusicBrainzProvider()
    )
}
