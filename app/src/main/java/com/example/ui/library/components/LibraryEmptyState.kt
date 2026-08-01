package com.example.ui.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAccentColor

@Composable
fun LibraryEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.LibraryMusic,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(LibrarySpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LocalAccentColor.current.copy(alpha = 0.7f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(LibrarySpacing.lg))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LibrarySpacing.sm))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(LibrarySpacing.xl))

            Button(
                onClick = onActionClick,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryEmptyStatePreview() {
    LibraryEmptyState(
        title = "No Songs Found",
        message = "Scan your local storage to import music into your library.",
        actionLabel = "Scan Storage",
        onActionClick = {}
    )
}
