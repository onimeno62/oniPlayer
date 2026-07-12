package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.EqualizerPresetEntity
import com.example.ui.viewmodel.MusicPlayerViewModel
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalSecondaryColor
import com.example.ui.theme.LocalAccentGlowColor

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

    val accentColor = LocalAccentColor.current
    val secondaryColor = LocalSecondaryColor.current
    val glowColor = LocalAccentGlowColor.current

    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameToSave by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {
        // Advanced Premium EQ Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Acoustic Tuning",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = Color.White
                )
                Text(
                    text = "Studio master studio parametric custom filter",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            // Save Preset Button with elegant circle outline
            IconButton(
                onClick = { showSaveDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    .background(Color.White.copy(alpha = 0.03f), CircleShape)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save Custom Preset", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Horizontal Presets Row Styled with Glass Capsule elements
        Text(
            text = "PRESET SOUNDSTAGES",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.8.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(presets) { preset ->
                val isActive = activePresetName == preset.name
                val borderCol = if (isActive) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
                val bgCol = if (isActive) accentColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.03f)
                val textCol = if (isActive) accentColor else Color.White.copy(alpha = 0.7f)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(bgCol)
                        .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                        .clickable { viewModel.selectPreset(preset) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = preset.name,
                            color = textCol,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        if (preset.isCustom) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete custom preset",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable {
                                        viewModel.deletePreset(preset)
                                        Toast.makeText(context, "Preset deleted", Toast.LENGTH_SHORT).show()
                                    },
                                tint = if (isActive) accentColor else Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Frequency Bands Card
        Text(
            text = "FREQUENCY RESPONSE BANDS (dB)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.8.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Band 1: 60Hz
                EqualizerSliderRow(label = "60 Hz (Sub Bass)", value = band60Hz, accentColor = accentColor, onValueChange = { viewModel.updateBand(0, it) })
                Spacer(modifier = Modifier.height(14.dp))

                // Band 2: 230Hz
                EqualizerSliderRow(label = "230 Hz (Punchy Bass)", value = band230Hz, accentColor = accentColor, onValueChange = { viewModel.updateBand(1, it) })
                Spacer(modifier = Modifier.height(14.dp))

                // Band 3: 910Hz
                EqualizerSliderRow(label = "910 Hz (Acoustic Vocals)", value = band910Hz, accentColor = accentColor, onValueChange = { viewModel.updateBand(2, it) })
                Spacer(modifier = Modifier.height(14.dp))

                // Band 4: 4kHz
                EqualizerSliderRow(label = "4 kHz (Detail / Presence)", value = band4kHz, accentColor = accentColor, onValueChange = { viewModel.updateBand(3, it) })
                Spacer(modifier = Modifier.height(14.dp))

                // Band 5: 14kHz
                EqualizerSliderRow(label = "14 kHz (Air / Sparkle)", value = band14kHz, accentColor = accentColor, onValueChange = { viewModel.updateBand(4, it) })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Audio Effects (Spatial audio / Sub-bass boost)
        Text(
            text = "SPATIAL AUDIO EXPANSION",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.8.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bass Boost Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Hearing, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sub-Bass", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = bassBoost,
                        onValueChange = { viewModel.updateBassBoost(it) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    Text(
                        text = "${bassBoost.toInt()}% boost",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            // Virtualizer / Spatial Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.SurroundSound, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Spatializer", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = virtualizer,
                        onValueChange = { viewModel.updateVirtualizer(it) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = secondaryColor,
                            activeTrackColor = secondaryColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    Text(
                        text = "${virtualizer.toInt()}% width",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondaryColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp)) // generous spacing at bottom
    }

    // Save Custom Preset Dialog with beautiful glass overlays
    if (showSaveDialog) {
        Dialog(onDismissRequest = { showSaveDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Save Personal Preset", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Save current frequency filter bands configuration to custom preset library.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = presetNameToSave,
                        onValueChange = { presetNameToSave = it },
                        placeholder = { Text("My Preset Name", color = Color.White.copy(alpha = 0.3f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showSaveDialog = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
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
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
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
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label, 
                fontSize = 13.sp, 
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = "${if (value >= 0) "+" else ""}${String.format("%.1f", value)} dB",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -15f..15f,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
