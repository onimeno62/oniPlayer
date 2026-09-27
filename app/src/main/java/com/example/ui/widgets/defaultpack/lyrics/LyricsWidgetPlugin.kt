package com.example.ui.widgets.defaultpack.lyrics

import com.example.ui.widgets.core.OniWidgetPlugin
import com.example.ui.widgets.core.OniWidgetRenderer
import com.example.ui.widgets.core.WidgetSize

/** Lyric-first widget. See [LyricsWidgetRenderer]. */
class LyricsWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.lyrics"
    override val packId = "default.pack"
    override val name = "Lyrics"
    override val description = "Lyric-first stage with the current line in focus and quiet controls."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = LyricsWidgetRenderer()
}
