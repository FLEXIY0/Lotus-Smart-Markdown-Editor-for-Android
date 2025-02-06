package com.flesiy.Lotus.data

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class TrashManager(private val context: Context) {
    private val TRASH_DIRECTORY = "trash"
    private val RETENTION_PERIOD_KEY = intPreferencesKey("trash_retention_period")
    private val userPreferences = UserPreferences(context)

    enum class RetentionPeriod(val days: Int, val displayName: String) {
        ONE_WEEK(7, "1 неделя"),
        TWO_WEEKS(14, "2 недели"),
        ONE_MONTH(30, "1 месяц"),
        THREE_MONTHS(90, "3 месяца"),
        SIX_MONTHS(180, "6 месяцев"),
        ONE_YEAR(365, "1 год"),
        NEVER(-1, "Никогда")
    }

    private fun getTrashDirectory(): File {
        val directory = File(context.filesDir, TRASH_DIRECTORY)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    fun getTrashSize(): Long {
        return getTrashDirectory().walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    fun isTrashOverLimit(): Boolean {
        val maxSize = 10L * 1024 * 1024 // 10 MB в байтах
        return getTrashSize() > maxSize
    }

    suspend fun moveToTrash(noteId: Long, content: String) {
        val trashFile = File(getTrashDirectory(), "${noteId}.md")
        trashFile.writeText(content)
        
        // Сохраняем время удаления
        userPreferences.setNoteDeletionTime(noteId, System.currentTimeMillis())
    }

    suspend fun restoreFromTrash(noteId: Long): String? {
        val trashFile = File(getTrashDirectory(), "${noteId}.md")
        if (!trashFile.exists()) return null
        
        val content = trashFile.readText()
        trashFile.delete()
        userPreferences.removeNoteDeletionTime(noteId)
        
        return content
    }

    suspend fun getTrashNotes(): List<TrashNote> {
        return getTrashDirectory().listFiles()?.mapNotNull { file ->
            try {
                val noteId = file.nameWithoutExtension.toLong()
                val content = file.readText()
                TrashNote(
                    id = noteId,
                    title = content.lines().firstOrNull()?.removePrefix("#")?.trim() ?: "Без названия",
                    content = content,
                    deletionTime = userPreferences.getNoteDeletionTime(noteId) ?: System.currentTimeMillis()
                )
            } catch (e: Exception) {
                null
            }
        }?.sortedByDescending { it.deletionTime } ?: emptyList()
    }

    suspend fun cleanupExpiredNotes() {
        val retentionPeriod = userPreferences.getTrashRetentionPeriod()
        if (retentionPeriod == RetentionPeriod.NEVER) return

        val currentTime = System.currentTimeMillis()
        val retentionMillis = retentionPeriod.days * 24 * 60 * 60 * 1000L

        getTrashNotes().forEach { note ->
            if (currentTime - note.deletionTime > retentionMillis) {
                val file = File(getTrashDirectory(), "${note.id}.md")
                file.delete()
                userPreferences.removeNoteDeletionTime(note.id)
            }
        }
    }
}

data class TrashNote(
    val id: Long,
    val title: String,
    val content: String,
    val deletionTime: Long
) 