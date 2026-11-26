package com.austin.spendtap.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.austin.spendtap.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Settings are one JSON string. Defaults on every field of AppSettings mean an older
 * saved value with missing keys still loads after an app update.
 */
class SettingsStore(private val context: Context) {
    private val key = stringPreferencesKey("settings_json")
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true }

    val flow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString<AppSettings>(it) }.getOrNull() } ?: AppSettings()
    }

    suspend fun save(settings: AppSettings) {
        context.dataStore.edit { it[key] = json.encodeToString(AppSettings.serializer(), settings) }
    }
}
