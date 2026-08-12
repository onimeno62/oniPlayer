package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equalizer_presets")
data class EqualizerPresetEntity(
    @PrimaryKey val name: String,
    val isCustom: Boolean = true,
    val band60Hz: Float = 0f, // in dB (-15 to +15)
    val band230Hz: Float = 0f,
    val band910Hz: Float = 0f,
    val band4kHz: Float = 0f,
    val band14kHz: Float = 0f,
    val bassBoost: Float = 0f, // percentage (0 to 100)
    val virtualizer: Float = 0f // percentage (0 to 100)
)
