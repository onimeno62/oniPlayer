package com.example.data.online

/**
 * Central registry for enabled providers.
 *
 * Providers are injected explicitly so optional integrations can be omitted without
 * changing the rest of the application. No provider is special-cased here.
 */
class MusicProviderRegistry(
    providers: List<MusicProvider>
) {
    private val providersById = providers.associateBy { it.id }

    fun all(): List<MusicProvider> = providersById.values.toList()

    fun get(id: String): MusicProvider? = providersById[id]

    fun supporting(capability: ProviderCapability): List<MusicProvider> =
        providersById.values.filter { it.capabilities.supports(capability) }
}
