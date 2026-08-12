package com.example.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

object LibrarySpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

@Preview(showBackground = true)
@Composable
private fun LibrarySpacingPreview() {
    Column(modifier = Modifier.padding(LibrarySpacing.md)) {
        Text("Library Spacing Tokens Preview", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(LibrarySpacing.sm))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(LibrarySpacing.lg)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
