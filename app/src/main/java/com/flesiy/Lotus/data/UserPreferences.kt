package com.flesiy.Lotus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {
    private val SKIP_DELETE_CONFIRMATION = booleanPreferencesKey("skip_delete_confirmation")
    private val TRASH_RETENTION_PERIOD = intPreferencesKey("trash_retention_period")
    private val NOTE_DELETION_TIME_PREFIX = "note_deletion_time_"

    val skipDeleteConfirmation: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SKIP_DELETE_CONFIRMATION] ?: false
        }

    suspend fun setSkipDeleteConfirmation(skip: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SKIP_DELETE_CONFIRMATION] = skip
        }
    }

    suspend fun getTrashRetentionPeriod(): TrashManager.RetentionPeriod {
        val days = context.dataStore.data
            .map { preferences ->
                preferences[TRASH_RETENTION_PERIOD] ?: TrashManager.RetentionPeriod.ONE_WEEK.days
            }
            .first()
        
        return TrashManager.RetentionPeriod.values().find { it.days == days }
            ?: TrashManager.RetentionPeriod.ONE_WEEK
    }

    suspend fun setTrashRetentionPeriod(period: TrashManager.RetentionPeriod) {
        context.dataStore.edit { preferences ->
            preferences[TRASH_RETENTION_PERIOD] = period.days
        }
    }

    suspend fun setNoteDeletionTime(noteId: Long, timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[longPreferencesKey("${NOTE_DELETION_TIME_PREFIX}$noteId")] = timestamp
        }
    }

    suspend fun getNoteDeletionTime(noteId: Long): Long? {
        return context.dataStore.data
            .map { preferences ->
                preferences[longPreferencesKey("${NOTE_DELETION_TIME_PREFIX}$noteId")]
            }
            .first()
    }

    suspend fun removeNoteDeletionTime(noteId: Long) {
        context.dataStore.edit { preferences ->
            preferences.remove(longPreferencesKey("${NOTE_DELETION_TIME_PREFIX}$noteId"))
        }
    }
} 