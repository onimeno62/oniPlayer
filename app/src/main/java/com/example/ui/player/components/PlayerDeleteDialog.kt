package com.example.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.SongEntity
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

/**
 * Confirmation dialog for track deletion in the Player screen.
 *
 * Supports deleting from library database or permanent physical file deletion.
 */
@Composable
fun PlayerDeleteDialog(
    song: SongEntity,
    onConfirmDelete: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var deletePhysicalFile by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        OniSurface(
            variant = OniSurfaceVariant.Elevated,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("player_delete_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = OniSkin.colors.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Delete Track",
                        style = OniSkin.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Are you sure you want to remove \"${song.title}\" from your library?",
                    style = OniSkin.typography.bodyMedium,
                    color = OniSkin.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deletePhysicalFile = !deletePhysicalFile }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = deletePhysicalFile,
                        onCheckedChange = { deletePhysicalFile = it },
                        colors = CheckboxDefaults.colors(checkedColor = OniSkin.colors.error)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Also delete file permanently from storage",
                        style = OniSkin.typography.bodySmall,
                        color = if (deletePhysicalFile) OniSkin.colors.error else OniSkin.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = OniSkin.colors.textSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onConfirmDelete(deletePhysicalFile) },
                        colors = ButtonDefaults.buttonColors(containerColor = OniSkin.colors.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
