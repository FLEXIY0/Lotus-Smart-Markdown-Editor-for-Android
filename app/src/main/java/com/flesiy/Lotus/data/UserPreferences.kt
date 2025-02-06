package com.flesiy.Lotus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {
    private val SKIP_DELETE_CONFIRMATION = booleanPreferencesKey("skip_delete_confirmation")

    val skipDeleteConfirmation: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SKIP_DELETE_CONFIRMATION] ?: false
        }

    suspend fun setSkipDeleteConfirmation(skip: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SKIP_DELETE_CONFIRMATION] = skip
        }
    }
} 