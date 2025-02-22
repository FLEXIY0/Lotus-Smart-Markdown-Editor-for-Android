package com.flesiy.Lotus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import android.content.SharedPreferences

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {
    private val SKIP_DELETE_CONFIRMATION = booleanPreferencesKey("skip_delete_confirmation")
    private val TRASH_RETENTION_PERIOD = intPreferencesKey("trash_retention_period")
    private val TODO_ENABLED = booleanPreferencesKey("todo_enabled")
    private val NOTE_DELETION_TIME_PREFIX = "note_deletion_time_"
    private val FONT_SIZE = floatPreferencesKey("font_size")
    private val FILE_MANAGEMENT_ENABLED = booleanPreferencesKey("file_management_enabled")
    private val EXPORT_DIRECTORY = stringPreferencesKey("export_directory")
    private val EXPORT_ONLY_NEW = booleanPreferencesKey("export_only_new")
    private val showThinkingTagsKey = booleanPreferencesKey("show_thinking_tags")
    private val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
    private val SELECTED_MODEL = stringPreferencesKey("selected_model")
    private val TEXT_ALPHA = floatPreferencesKey("text_alpha")
    private val IS_VERSION_CONTROL_ENABLED = booleanPreferencesKey("is_version_control_enabled")
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

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

    val todoEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[TODO_ENABLED] ?: true
        }

    suspend fun setTodoEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TODO_ENABLED] = enabled
        }
    }

    val fileManagementEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[FILE_MANAGEMENT_ENABLED] ?: false
        }

    suspend fun setFileManagementEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FILE_MANAGEMENT_ENABLED] = enabled
        }
    }

    val fontSize: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[FONT_SIZE] ?: 16f
        }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE] = size
        }
    }

    val exportDirectory: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[EXPORT_DIRECTORY]
        }

    suspend fun setExportDirectory(path: String?) {
        context.dataStore.edit { preferences ->
            if (path != null) {
                preferences[EXPORT_DIRECTORY] = path
            } else {
                preferences.remove(EXPORT_DIRECTORY)
            }
        }
    }

    val exportOnlyNew: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[EXPORT_ONLY_NEW] ?: false
        }

    suspend fun setExportOnlyNew(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EXPORT_ONLY_NEW] = enabled
        }
    }

    val showThinkingTags: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[showThinkingTagsKey] ?: false
        }

    suspend fun setShowThinkingTags(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[showThinkingTagsKey] = show
        }
    }

    val systemPrompt: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SYSTEM_PROMPT]
        }

    suspend fun setSystemPrompt(prompt: String?) {
        context.dataStore.edit { preferences ->
            if (prompt != null) {
                preferences[SYSTEM_PROMPT] = prompt
            } else {
                preferences.remove(SYSTEM_PROMPT)
            }
        }
    }

    val selectedModel: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_MODEL] ?: "qwen-2.5-32b"
        }

    val textAlpha: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[TEXT_ALPHA] ?: 1f
        }

    suspend fun setSelectedModel(modelId: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_MODEL] = modelId
        }
    }

    suspend fun setTextAlpha(alpha: Float) {
        context.dataStore.edit { preferences ->
            preferences[TEXT_ALPHA] = alpha
        }
    }

    var isDarkTheme: Boolean
        get() = sharedPreferences.getBoolean("is_dark_theme", false)
        set(value) {
            sharedPreferences.edit().putBoolean("is_dark_theme", value).apply()
        }

    val isVersionControlEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_VERSION_CONTROL_ENABLED] ?: true
        }

    suspend fun setVersionControlEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_VERSION_CONTROL_ENABLED] = enabled
        }
    }
} 