package com.oniplayer.ui.widgets.defaultpack

import com.oniplayer.ui.widgets.core.OniWidgetPack
import com.oniplayer.ui.widgets.core.OniWidgetPlugin
import com.oniplayer.ui.widgets.defaultpack.dynamicalbum.DynamicAlbumWidgetPlugin
import com.oniplayer.ui.widgets.defaultpack.lyrics.LyricsWidgetPlugin
import com.oniplayer.ui.widgets.defaultpack.miniplayer.MiniPlayerWidgetPlugin
import com.oniplayer.ui.widgets.defaultpack.nowplaying.NowPlayingWidgetPlugin

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
