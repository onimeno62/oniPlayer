package com.example.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

/**
 * Always-visible search entry on the dashboard. It is a button, not a text field:
 * tapping it opens the global Search tab so search is never hidden behind an icon.
 */
@Composable
fun DashboardSearchPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search songs, artists, albums"
) {
    OniSurface(
        modifier = modifier
            .height(48.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Search library" },
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.full,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OniSkin.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = OniSkin.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = placeholder,
                style = OniSkin.typography.bodyMedium,
                color = OniSkin.colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Compact 48dp circular action used next to the search pill (Shuffle, Play All).
 * [emphasized] = filled with the accent; only one action on screen should use it.
 */
@Composable
fun DashboardActionIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    enabled: Boolean = true
) {
    val container = if (emphasized) OniSkin.colors.primary else OniSkin.colors.surfaceVariant
    val content = if (emphasized) OniSkin.colors.onPrimary else OniSkin.colors.textPrimary
    OniSurface(
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = label },
        variant = if (emphasized) OniSurfaceVariant.Flat else OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.full,
        containerColor = container,
        contentColor = content,
        onClick = onClick,
        enabled = enabled
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) content else content.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
