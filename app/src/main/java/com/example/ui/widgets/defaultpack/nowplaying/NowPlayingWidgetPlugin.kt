package com.example.ui.widgets.defaultpack.nowplaying

import com.example.ui.widgets.core.OniWidgetPlugin
import com.example.ui.widgets.core.OniWidgetRenderer
import com.example.ui.widgets.core.WidgetSize

/** Flagship playback widget. See [NowPlayingWidgetRenderer]. */
class NowPlayingWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.nowplaying"
    override val packId = "default.pack"
    override val name = "Now Playing"
    override val description = "Flagship player with artwork, live progress, and full transport."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = NowPlayingWidgetRenderer()
}
