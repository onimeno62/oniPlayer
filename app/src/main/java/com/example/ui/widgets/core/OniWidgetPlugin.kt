package com.example.ui.widgets.core

/**
 * Metadata and plugin definition for any oniPlayer widget.
 * Decouples widget specification from the core player and skin engine.
 */
interface OniWidgetPlugin {
    val id: String
    val packId: String
    val name: String
    val description: String
    val supportedSizes: Set<WidgetSize>
    val isBuiltIn: Boolean get() = true

    fun createRenderer(): OniWidgetRenderer
}
