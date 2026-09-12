package com.example.ui.widgets.defaultpack

import com.example.ui.widgets.core.OniWidgetPack
import com.example.ui.widgets.core.OniWidgetPlugin
import com.example.ui.widgets.defaultpack.compactplayer.CompactPlayerWidgetPlugin
import com.example.ui.widgets.defaultpack.lyrics.LyricsWidgetPlugin
import com.example.ui.widgets.defaultpack.nowplaying.NowPlayingWidgetPlugin

/**
 * Built-in default widget pack for oniPlayer.
 * Contains the three flagship widgets: Now Playing, Compact Player, and Lyrics & Visualizer.
 */
object DefaultWidgetPack : OniWidgetPack {
    override val id: String = "default.pack"
    override val name: String = "Default Widget Pack"
    override val description: String = "Official oniPlayer default widgets with responsive 4x1, 4x2, and 4x4 layouts adapting automatically to your active skin."
    override val version: String = "1.0.0"
    override val author: String = "oniPlayer Team"

    override val widgets: List<OniWidgetPlugin> = listOf(
        NowPlayingWidgetPlugin(),
        CompactPlayerWidgetPlugin(),
        LyricsWidgetPlugin()
    )
}
