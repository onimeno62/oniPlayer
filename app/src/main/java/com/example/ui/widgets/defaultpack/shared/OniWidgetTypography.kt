package com.example.ui.widgets.defaultpack.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.ui.theme.OniSkinTokens
import androidx.compose.ui.text.TextStyle as SkinTextStyle
import androidx.compose.ui.text.font.FontFamily as SkinFontFamily

/** Semantic widget text roles. Each maps to a skin typography token. */
enum class OniWidgetTextRole {
    /** Dynamic Album 4x4 title. */
    Display,
    /** Flagship / hero titles. */
    Hero,
    /** Standard widget song title. */
    Title,
    /** 4x1 song title. */
    CompactTitle,
    /** Artist line. */
    Body,
    /** Album / secondary metadata. */
    Meta,
    /** Eyebrow / section labels. */
    Label,
    /** Playback time. */
    Time,
    /** 4x4 current lyric. */
    LyricHero,
    /** 4x2 current lyric. */
    LyricCurrent,
    /** 4x1 current lyric. */
    LyricCompact,
    /** Previous / next lyric lines. */
    LyricContext
}

/**
 * Converts skin typography into Glance [TextStyle]s.
 *
 * Sizes are scaled from the skin tokens (not hardcoded) so a skin that
 * changes its type scale or family changes the widgets too. Emphasis roles
 * are bumped one weight step because widgets are read at a glance.
 */
object OniWidgetType {

    fun style(
        skin: OniSkinTokens,
        role: OniWidgetTextRole,
        color: Color,
        align: TextAlign? = null
    ): TextStyle {
        val t = skin.typography
        val spec = when (role) {
            OniWidgetTextRole.Display -> Spec(t.displayMedium, 0.92f, 24f, bump = true)
            OniWidgetTextRole.Hero -> Spec(t.titleLarge, 1.0f, 22f, bump = true)
            OniWidgetTextRole.Title -> Spec(t.titleMedium, 1.0f, 18f, bump = true)
            OniWidgetTextRole.CompactTitle -> Spec(t.titleSmall, 0.94f, 15f, bump = true)
            OniWidgetTextRole.Body -> Spec(t.bodySmall, 1.0f, 13f)
            OniWidgetTextRole.Meta -> Spec(t.caption, 0.92f, 11f)
            OniWidgetTextRole.Label -> Spec(t.labelMedium, 0.84f, 10f, bump = true)
            OniWidgetTextRole.Time -> Spec(t.caption, 0.84f, 10f)
            OniWidgetTextRole.LyricHero -> Spec(t.displayMedium, 0.86f, 22f, bump = true)
            OniWidgetTextRole.LyricCurrent -> Spec(t.titleMedium, 1.0f, 18f, bump = true)
            OniWidgetTextRole.LyricCompact -> Spec(t.titleSmall, 0.94f, 15f, bump = true)
            OniWidgetTextRole.LyricContext -> Spec(t.bodyMedium, 1.0f, 14f)
        }
        return TextStyle(
            color = ColorProvider(color),
            fontSize = spec.sizeSp().sp,
            fontWeight = spec.weight(),
            textAlign = align,
            fontFamily = family(spec.token)
        )
    }

    private class Spec(
        val token: SkinTextStyle,
        val scale: Float,
        val fallbackSp: Float,
        val bump: Boolean = false
    ) {
        fun sizeSp(): Float {
            val size = token.fontSize
            val base = if (size.isSpecified) size.value else fallbackSp
            return (base * scale).coerceIn(9f, 30f)
        }

        fun weight(): FontWeight {
            val w = (token.fontWeight?.weight ?: 400) + if (bump) 100 else 0
            return when {
                w >= 600 -> FontWeight.Bold
                w >= 500 -> FontWeight.Medium
                else -> FontWeight.Normal
            }
        }
    }

    private fun family(token: SkinTextStyle): FontFamily? = when (token.fontFamily) {
        SkinFontFamily.Serif -> FontFamily.Serif
        SkinFontFamily.Monospace -> FontFamily.Monospace
        SkinFontFamily.Cursive -> FontFamily.Cursive
        SkinFontFamily.SansSerif -> FontFamily.SansSerif
        else -> null
    }
}
