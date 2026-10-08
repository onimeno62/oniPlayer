package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single shared DataStore instance for the "oni_settings" preferences file.
 *
 * Android DataStore requires maintaining a single active DataStore instance per file to avoid
 * IllegalStateException ("There are multiple DataStores active for the same file").
 */
val Context.oniSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "oni_settings")
