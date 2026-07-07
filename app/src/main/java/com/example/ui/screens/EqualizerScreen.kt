package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.EqualizerPresetEntity
import com.example.ui.viewmodel.MusicPlayerViewModel

@Composable
fun EqualizerScreen(viewModel: MusicPlayerViewModel) {
    val presets by viewModel.allPresets.collectAsState()
    val activePresetName by viewModel.currentPresetName.collectAsState()

    val band60Hz by viewModel.eqBand60Hz.collectAsState()
    val band230Hz by viewModel.eqBand230Hz.collectAsState()
    val band910Hz by viewModel.eqBand910Hz.collectAsState()
    val band4kHz by viewModel.eqBand4kHz.collectAsState()
    val band14kHz by viewModel.eqBand14kHz.collectAsState()

    val bassBoost by viewModel.eqBassBoost.collectAsState()
    val virtualizer by viewModel.eqVirtualizer.collectAsState()

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameToSave by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Advanced Equalizer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Acoustic personalization & spatial audio",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Save Preset Button
            IconButton(
                onClick = { showSaveDialog = true },
                modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save Custom Preset", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Presets Horizontal Row
        Text(
            text = "PRESETS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(presets) { preset ->
                val isActive = activePresetName == preset.name
                val containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                val contentColor = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface

                Card(
                    modifier = Modifier
                        .clickable { viewModel.selectPreset(preset) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = preset.name,
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (preset.isCustom) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete custom preset",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        viewModel.deletePreset(preset)
                                        Toast.makeText(context, "Preset deleted", Toast.LENGTH_SHORT).show()
                                    },
                                tint = if (isActive) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Sliders Block (5 Bands)
        Text(
            text = "FREQUENCY BANDS (dB)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
            border = borderStrokeDefault()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Band 1: 60Hz
                EqualizerSliderRow(label = "60 Hz (Bass)", value = band60Hz, onValueChange = { viewModel.updateBand(0, it) })
                Spacer(modifier = Modifier.height(12.dp))

                // Band 2: 230Hz
                EqualizerSliderRow(label = "230 Hz (Mid-Bass)", value = band230Hz, onValueChange = { viewModel.updateBand(1, it) })
                Spacer(modifier = Modifier.height(12.dp))

                // Band 3: 910Hz
                EqualizerSliderRow(label = "910 Hz (Mids)", value = band910Hz, onValueChange = { viewModel.updateBand(2, it) })
                Spacer(modifier = Modifier.height(12.dp))

                // Band 4: 4kHz
                EqualizerSliderRow(label = "4 kHz (Upper Mids)", value = band4kHz, onValueChange = { viewModel.updateBand(3, it) })
                Spacer(modifier = Modifier.height(12.dp))

                // Band 5: 14kHz
                EqualizerSliderRow(label = "14 kHz (Treble)", value = band14kHz, onValueChange = { viewModel.updateBand(4, it) })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Audio Effects (Bass Boost & Spatial Virtualizer)
        Text(
            text = "SPATIAL AUDIO EFFECTS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bass Boost Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                border = borderStrokeDefault()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Hearing, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Bass Boost", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Slider(
                        value = bassBoost,
                        onValueChange = { viewModel.updateBassBoost(it) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                    Text("${bassBoost.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Virtualizer / Spatial Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                border = borderStrokeDefault()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.SurroundSound, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Virtualizer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Slider(
                        value = virtualizer,
                        onValueChange = { viewModel.updateVirtualizer(it) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.secondary)
                    )
                    Text("${virtualizer.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // space for container bottom padding
    }

    // Save Custom Preset Dialog
    if (showSaveDialog) {
        Dialog(onDismissRequest = { showSaveDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Save Custom Preset", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = presetNameToSave,
                        onValueChange = { presetNameToSave = it },
                        label = { Text("Preset Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showSaveDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (presetNameToSave.isNotBlank()) {
                                    viewModel.saveCustomPreset(presetNameToSave.trim())
                                    Toast.makeText(context, "Preset saved!", Toast.LENGTH_SHORT).show()
                                    showSaveDialog = false
                                    presetNameToSave = ""
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EqualizerSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "${if (value >= 0) "+" else ""}${String.format("%.1f", value)} dB",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -15f..15f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
