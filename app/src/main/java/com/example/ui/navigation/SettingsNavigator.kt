package com.example.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiny deep-link channel into Settings sub-screens ("lyrics", "library_metadata", ...).
 *
 * Settings keeps its own `activeSubScreen` state; other screens (Lyrics tools, Library menu)
 * post a request here and switch to the Settings tab. SettingsScreen consumes the request once.
 * [returnTab] lets back from that sub-screen go straight back to where the user came from.
 */
object SettingsNavigator {
    data class Request(val subScreen: String, val returnTab: Int? = null)

    private val _pending = MutableStateFlow<Request?>(null)
    val pending: StateFlow<Request?> = _pending.asStateFlow()

    fun open(subScreen: String, returnTab: Int? = null) {
        _pending.value = Request(subScreen, returnTab)
    }

    fun consume() {
        _pending.value = null
    }
}
