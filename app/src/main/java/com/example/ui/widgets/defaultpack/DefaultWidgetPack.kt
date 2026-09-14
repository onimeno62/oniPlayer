package com.example.ui.widgets.defaultpack

import com.example.ui.widgets.core.OniWidgetPack
import com.example.ui.widgets.core.OniWidgetPlugin
import com.example.ui.widgets.defaultpack.dynamicalbum.DynamicAlbumWidgetPlugin
import com.example.ui.widgets.defaultpack.lyrics.LyricsWidgetPlugin
import com.example.ui.widgets.defaultpack.miniplayer.MiniPlayerWidgetPlugin
import com.example.ui.widgets.defaultpack.nowplaying.NowPlayingWidgetPlugin

/** Built-in widget pack containing intentionally different widget products. */
object DefaultWidgetPack : OniWidgetPack {
    override val id: String = "default.pack"
    override val name: String = "Default Widget Pack"
    override val description: String = "Mini Player, Now Playing, Dynamic Album, and Lyrics widgets."
    override val version: String = "2.0.0"
    override val author: String = "oniPlayer Team"

    override val widgets: List<OniWidgetPlugin> = listOf(
        MiniPlayerWidgetPlugin(),
        NowPlayingWidgetPlugin(),
        DynamicAlbumWidgetPlugin(),
        LyricsWidgetPlugin()
    )
}
