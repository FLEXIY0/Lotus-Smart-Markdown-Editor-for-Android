package com.flesiy.Lotus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

class ThemePreferences(private val context: Context) {
    private val useClassicThemeKey = booleanPreferencesKey("use_classic_theme")

    val useClassicTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[useClassicThemeKey] ?: false
        }

    suspend fun setUseClassicTheme(useClassic: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[useClassicThemeKey] = useClassic
        }
    }
} 