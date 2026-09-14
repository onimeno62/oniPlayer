package com.example.ui.widgets.defaultpack

import com.example.ui.widgets.core.OniWidgetPack
import com.example.ui.widgets.core.OniWidgetPlugin
import com.example.ui.widgets.defaultpack.compactplayer.CompactPlayerWidgetPlugin
import com.example.ui.widgets.defaultpack.dynamicalbum.DynamicAlbumWidgetPlugin
import com.example.ui.widgets.defaultpack.lyrics.LyricsWidgetPlugin
import com.example.ui.widgets.defaultpack.nowplaying.NowPlayingWidgetPlugin

/**
 * Built-in widget pack. Each widget is a distinct product with intentional
 * compositions rather than a shared player layout resized for each use case.
 */
object DefaultWidgetPack : OniWidgetPack {
    override val id: String = "default.pack"
    override val name: String = "Default Widget Pack"
    override val description: String = "Distinct Mini Player, Now Playing, Dynamic Album, and Lyrics widgets."
    override val version: String = "2.0.0"
    override val author: String = "oniPlayer Team"

    override val widgets: List<OniWidgetPlugin> = listOf(
        CompactPlayerWidgetPlugin(),
        NowPlayingWidgetPlugin(),
        DynamicAlbumWidgetPlugin(),
        LyricsWidgetPlugin()
    )
}
