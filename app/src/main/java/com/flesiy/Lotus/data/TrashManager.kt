package com.flesiy.Lotus.data

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class TrashManager(private val context: Context) {
    private val TRASH_DIRECTORY = "trash"
    private val CACHE_DIRECTORY = "cache" // Директория для кеша
    private val IMAGES_CACHE_DIRECTORY = "images" // Поддиректория для изображений
    private val FILES_CACHE_DIRECTORY = "files" // Поддиректория для других файлов
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

    suspend fun deleteNoteFromTrash(noteId: Long) {
        val file = File(getTrashDirectory(), "${noteId}.md")
        if (file.exists()) {
            file.delete()
            userPreferences.removeNoteDeletionTime(noteId)
        }
    }

    suspend fun clearTrash() {
        getTrashDirectory().listFiles()?.forEach { file ->
            file.delete()
            val noteId = file.nameWithoutExtension.toLong()
            userPreferences.removeNoteDeletionTime(noteId)
        }
    }

    fun getTotalNotesSize(): Long {
        val notesDir = File(context.filesDir, "notes")
        return if (notesDir.exists()) {
            notesDir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        } else {
            0L
        }
    }

    fun getAppMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    // Получение размера кеша
    fun getCacheSize(): Long {
        val cacheDir = getCacheDirectory()
        return cacheDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    // Получение размера кеша изображений
    fun getImagesCacheSize(): Long {
        val imagesDir = getImagesCacheDirectory()
        return imagesDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    // Получение размера кеша файлов
    fun getFilesCacheSize(): Long {
        val filesDir = getFilesCacheDirectory()
        return filesDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    // Очистка всего кеша
    fun clearCache() {
        val cacheDir = getCacheDirectory()
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
        getImagesCacheDirectory().mkdirs()
        getFilesCacheDirectory().mkdirs()
    }

    // Очистка кеша изображений
    fun clearImagesCache() {
        val imagesDir = getImagesCacheDirectory()
        imagesDir.deleteRecursively()
        imagesDir.mkdirs()
    }

    // Очистка кеша файлов
    fun clearFilesCache() {
        val filesDir = getFilesCacheDirectory()
        filesDir.deleteRecursively()
        filesDir.mkdirs()
    }

    // Получение директории кеша
    private fun getCacheDirectory(): File {
        val directory = File(context.filesDir, CACHE_DIRECTORY)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    // Получение директории кеша изображений
    private fun getImagesCacheDirectory(): File {
        val directory = File(getCacheDirectory(), IMAGES_CACHE_DIRECTORY)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    // Получение директории кеша файлов
    private fun getFilesCacheDirectory(): File {
        val directory = File(getCacheDirectory(), FILES_CACHE_DIRECTORY)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    // Сохранение изображения в кеш для конкретной заметки
    fun cacheImageForNote(noteId: Long, imageFile: File): File {
        val imagesDir = getImagesCacheDirectory()
        val noteImagesDir = File(imagesDir, noteId.toString())
        if (!noteImagesDir.exists()) {
            noteImagesDir.mkdirs()
        }
        val targetFile = File(noteImagesDir, imageFile.name)
        imageFile.copyTo(targetFile, overwrite = true)
        return targetFile
    }

    // Получение всех изображений для заметки
    fun getImagesForNote(noteId: Long): List<File> {
        val noteImagesDir = File(getImagesCacheDirectory(), noteId.toString())
        return if (noteImagesDir.exists()) {
            noteImagesDir.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    // Удаление всех изображений для заметки
    fun deleteImagesForNote(noteId: Long) {
        val noteImagesDir = File(getImagesCacheDirectory(), noteId.toString())
        noteImagesDir.deleteRecursively()
    }

    // Получение статистики кеша
    data class CacheStats(
        val totalSize: Long,
        val imagesSize: Long,
        val filesSize: Long,
        val noteImagesCount: Map<Long, Int> // Количество изображений по заметкам
    )

    fun getCacheStats(): CacheStats {
        val imagesDir = getImagesCacheDirectory()
        val noteImagesCount = mutableMapOf<Long, Int>()
        
        // Подсчет количества изображений для каждой заметки
        if (imagesDir.exists()) {
            imagesDir.listFiles()?.forEach { noteDir ->
                val noteId = noteDir.name.toLongOrNull()
                if (noteId != null && noteDir.isDirectory) {
                    noteImagesCount[noteId] = noteDir.listFiles()?.size ?: 0
                }
            }
        }

        return CacheStats(
            totalSize = getCacheSize(),
            imagesSize = getImagesCacheSize(),
            filesSize = getFilesCacheSize(),
            noteImagesCount = noteImagesCount
        )
    }
}

data class TrashNote(
    val id: Long,
    val title: String,
    val content: String,
    val deletionTime: Long
) 