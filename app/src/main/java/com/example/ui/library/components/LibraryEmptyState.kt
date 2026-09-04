package com.example.ui.library.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.components.state.OniEmptyState

@Composable
fun LibraryEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.LibraryMusic,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    OniEmptyState(
        title = title,
        message = message,
        modifier = modifier,
        icon = icon,
        actionLabel = actionLabel,
        onActionClick = onActionClick
    )
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
