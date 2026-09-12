package com.example.ui.widgets.core

import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry for all built-in and future installed widget plugins.
 */
object OniWidgetRegistry {
    private val plugins = ConcurrentHashMap<String, OniWidgetPlugin>()
    private val packs = ConcurrentHashMap<String, OniWidgetPack>()

    fun registerPack(pack: OniWidgetPack) {
        packs[pack.id] = pack
        pack.widgets.forEach { registerPlugin(it) }
    }

    fun registerPlugin(plugin: OniWidgetPlugin) {
        plugins[plugin.id] = plugin
    }

    fun getPlugin(id: String): OniWidgetPlugin? = plugins[id]

    fun getAllPlugins(): List<OniWidgetPlugin> = plugins.values.toList()

    fun getAllPacks(): List<OniWidgetPack> = packs.values.toList()

    fun clear() {
        plugins.clear()
        packs.clear()
    }
}
