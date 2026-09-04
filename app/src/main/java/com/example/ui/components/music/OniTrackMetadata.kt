package com.example.ui.components.music

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Reusable metadata layout primitive for track/album/artist titles and descriptions.
 * Supports primary title, secondary subtitle (artist/album), and tertiary details.
 * Consumes [OniSkin.typography] and [OniSkin.colors] tokens.
 */
@Composable
fun OniTrackMetadata(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    tertiaryText: String? = null,
    isCurrent: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    textAlign: TextAlign = TextAlign.Start,
    titleStyle: TextStyle = OniSkin.typography.bodyLarge,
    subtitleStyle: TextStyle = OniSkin.typography.bodySmall,
    tertiaryStyle: TextStyle = OniSkin.typography.caption,
    titleColor: Color = if (isCurrent) OniSkin.colors.primary else OniSkin.colors.textPrimary,
    subtitleColor: Color = if (isCurrent) OniSkin.colors.primary.copy(alpha = 0.85f) else OniSkin.colors.textSecondary,
    tertiaryColor: Color = OniSkin.colors.textTertiary,
    maxLinesTitle: Int = 1,
    maxLinesSubtitle: Int = 1
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = title,
            style = titleStyle,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
            color = titleColor,
            textAlign = textAlign,
            maxLines = maxLinesTitle,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtitle,
            style = subtitleStyle,
            fontWeight = FontWeight.Normal,
            color = subtitleColor,
            textAlign = textAlign,
            maxLines = maxLinesSubtitle,
            overflow = TextOverflow.Ellipsis
        )

        if (!tertiaryText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tertiaryText,
                style = tertiaryStyle,
                color = tertiaryColor,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
