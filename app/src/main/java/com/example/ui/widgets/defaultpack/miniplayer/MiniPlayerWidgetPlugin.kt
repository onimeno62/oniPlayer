package com.example.ui.widgets.defaultpack.miniplayer

import com.example.ui.widgets.core.OniWidgetPlugin
import com.example.ui.widgets.core.OniWidgetRenderer
import com.example.ui.widgets.core.WidgetSize

/** Transport-first widget. See [MiniPlayerWidgetRenderer]. */
class MiniPlayerWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.miniplayer"
    override val packId = "default.pack"
    override val name = "Mini Player"
    override val description = "Transport-first control strip with an integrated playback capsule."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = MiniPlayerWidgetRenderer()
}
