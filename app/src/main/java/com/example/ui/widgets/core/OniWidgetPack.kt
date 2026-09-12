package com.example.ui.widgets.core

/**
 * Metadata descriptor for a cohesive collection of widgets (e.g. Default Widget Pack).
 */
interface OniWidgetPack {
    val id: String
    val name: String
    val description: String
    val version: String
    val author: String
    val widgets: List<OniWidgetPlugin>
}
