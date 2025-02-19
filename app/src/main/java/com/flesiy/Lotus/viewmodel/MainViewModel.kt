package com.flesiy.Lotus.viewmodel

import android.app.Activity
import android.app.Application
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flesiy.Lotus.data.UserPreferences
import com.flesiy.Lotus.data.TrashManager
import com.flesiy.Lotus.data.TrashNote
import com.flesiy.Lotus.utils.FileUtils
import com.flesiy.Lotus.utils.MarkdownUtils
import com.flesiy.Lotus.utils.SpeechRecognitionManager
import com.flesiy.Lotus.utils.TextProcessor
import com.flesiy.Lotus.utils.GroqTextProcessor
import com.flesiy.Lotus.utils.GroqModel
import com.flesiy.Lotus.utils.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import android.os.Environment
import kotlinx.coroutines.flow.first
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.IOException
import androidx.documentfile.provider.DocumentFile
import android.net.Uri
import com.flesiy.Lotus.utils.GroqRequest
import com.flesiy.Lotus.utils.Message

data class Note(
    val id: Long,
    val title: String,
    val preview: String,
    val content: String,
    val modifiedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val isPreviewMode: Boolean = true,
    val isPinned: Boolean = false,
    val order: Int = 0
)

data class NoteVersion(
    val id: Long = System.currentTimeMillis(),
    val noteId: Long,
    val content: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    private val _currentNote = MutableStateFlow(Note(0L, "", "", "", 0L, 0L))
    val currentNote: StateFlow<Note> = _currentNote

    private val _noteVersions = MutableStateFlow<List<NoteVersion>>(emptyList())
    val noteVersions: StateFlow<List<NoteVersion>> = _noteVersions

    private val _isVersionHistoryVisible = MutableStateFlow(false)
    val isVersionHistoryVisible: StateFlow<Boolean> = _isVersionHistoryVisible

    private val _selectedVersion = MutableStateFlow<NoteVersion?>(null)
    val selectedVersion: StateFlow<NoteVersion?> = _selectedVersion

    private val userPreferences = UserPreferences(application)
    private val trashManager = TrashManager(application)

    val skipDeleteConfirmation = userPreferences.skipDeleteConfirmation.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    private val _trashNotes = MutableStateFlow<List<TrashNote>>(emptyList())
    val trashNotes: StateFlow<List<TrashNote>> = _trashNotes

    private val _trashSize = MutableStateFlow(0L)
    val trashSize: StateFlow<Long> = _trashSize

    private val _isTrashOverLimit = MutableStateFlow(false)
    val isTrashOverLimit: StateFlow<Boolean> = _isTrashOverLimit

    private val _currentRetentionPeriod = MutableStateFlow(TrashManager.RetentionPeriod.ONE_WEEK)
    val currentRetentionPeriod: StateFlow<TrashManager.RetentionPeriod> = _currentRetentionPeriod

    private val _totalNotesSize = MutableStateFlow(0L)
    val totalNotesSize: StateFlow<Long> = _totalNotesSize

    private val _appMemoryUsage = MutableStateFlow(0L)
    val appMemoryUsage: StateFlow<Long> = _appMemoryUsage

    private val _cacheStats = MutableStateFlow(TrashManager.CacheStats(0L, 0L, 0L, emptyMap()))
    val cacheStats: StateFlow<TrashManager.CacheStats> = _cacheStats

    private val _exportDirectory: MutableStateFlow<File?> = MutableStateFlow(null)
    val exportDirectory: StateFlow<File?> = _exportDirectory

    private val _lastViewedNoteFile: MutableStateFlow<File?> = MutableStateFlow(null)
    val lastViewedNoteFile: StateFlow<File?> = _lastViewedNoteFile

    private lateinit var speechRecognitionManager: SpeechRecognitionManager

    val isListening: StateFlow<Boolean>
        get() = speechRecognitionManager.isListening

    val elapsedTime: StateFlow<Long>
        get() = speechRecognitionManager.elapsedTime

    private val _isTextProcessorEnabled = MutableStateFlow(false)
    val isTextProcessorEnabled = _isTextProcessorEnabled.asStateFlow()

    private val _isGroqEnabled = MutableStateFlow(true)
    val isGroqEnabled = _isGroqEnabled.asStateFlow()

    private val _selectedGroqModel = MutableStateFlow("qwen-2.5-32b")
    val selectedGroqModel = _selectedGroqModel.asStateFlow()

    private val _availableGroqModels = MutableStateFlow<List<GroqModel>>(emptyList())
    val availableGroqModels = _availableGroqModels.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels = _isLoadingModels.asStateFlow()

    private val _modelLoadError = MutableStateFlow<String?>(null)
    val modelLoadError = _modelLoadError.asStateFlow()

    private val _isFileManagementEnabled = MutableStateFlow(false)
    val isFileManagementEnabled = _isFileManagementEnabled.asStateFlow()

    private val _systemPrompt = MutableStateFlow<String?>(null)
    val systemPrompt = _systemPrompt.asStateFlow()

    private val _isTodoEnabled = MutableStateFlow(true)
    val isTodoEnabled = _isTodoEnabled.asStateFlow()

    private val _fontSize = MutableStateFlow(16f)
    val fontSize = _fontSize.asStateFlow()

    private val _exportOnlyNew = MutableStateFlow(false)
    val exportOnlyNew = _exportOnlyNew.asStateFlow()

    private val _lastExportTime = MutableStateFlow<Long?>(null)
    val lastExportTime = _lastExportTime.asStateFlow()

    private val TAG = "SPEECH_DEBUG"

    private val _exportProgress = MutableStateFlow<ExportProgress>(ExportProgress.Idle)
    val exportProgress: StateFlow<ExportProgress> = _exportProgress

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult = _testResult.asStateFlow()

    sealed class ExportProgress {
        data object Idle : ExportProgress()
        data class InProgress(val current: Int, val total: Int) : ExportProgress()
        data class Error(val message: String) : ExportProgress()
        data object Success : ExportProgress()
    }

    init {
        loadNotes()
        loadLastViewedNote()
        loadTrashInfo()
        loadRetentionPeriod()
        startMemoryMonitoring()
        initExportDirectory()
        updateCacheStats()
        loadNoteVersions()
        speechRecognitionManager = SpeechRecognitionManager(getApplication())
        loadTodoEnabled()
        loadFontSize()
        loadFileManagementEnabled()
        loadExportOnlyNew()
        loadLastExportTime()
    }

    fun setActivity(activity: Activity) {
        speechRecognitionManager.setActivity(activity)
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognitionManager.destroy()
    }

    private fun initExportDirectory() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            Log.d("FileManagement", "Инициализация директории экспорта")
            
            try {
                // Пытаемся загрузить сохраненный путь
                val savedPath = userPreferences.exportDirectory.first()
                if (savedPath != null) {
                    val savedDir = File(savedPath)
                    val savedDirDoc = DocumentFile.fromFile(savedDir)
                    if (savedDirDoc != null && savedDirDoc.exists() && savedDirDoc.canWrite()) {
                        _exportDirectory.value = savedDir
                        Log.d("FileManagement", "Загружена сохраненная директория: ${savedDir.absolutePath}")
                        return@launch
                    }
                }

                // Если сохраненный путь не найден или недоступен, используем путь по умолчанию
                val defaultDir = File(Environment.getExternalStorageDirectory(), "Lotus")
                val defaultDirDoc = DocumentFile.fromFile(defaultDir)
                
                if (defaultDirDoc == null || !defaultDirDoc.exists()) {
                    val created = defaultDir.mkdirs()
                    Log.d("FileManagement", "Создание основной директории: $created")
                    if (!created) {
                        throw SecurityException("Не удалось создать директорию")
                    }
                }
                
                // Проверяем снова после создания
                val newDefaultDirDoc = DocumentFile.fromFile(defaultDir)
                if (newDefaultDirDoc != null && newDefaultDirDoc.canWrite()) {
                    _exportDirectory.value = defaultDir
                    userPreferences.setExportDirectory(defaultDir.absolutePath)
                    Log.d("FileManagement", "Установлена основная директория")
                    return@launch
                }
                
                throw SecurityException("Нет прав на запись в директорию")
                
            } catch (e: Exception) {
                Log.e("FileManagement", "Ошибка при инициализации директории", e)
                // Используем резервный вариант - внутреннюю память приложения
                val backupDir = File(context.getExternalFilesDir(null), "Lotus")
                if (!backupDir.exists()) {
                    val created = backupDir.mkdirs()
                    Log.d("FileManagement", "Создание резервной директории: $created")
                }
                Log.d("FileManagement", "Использование резервной директории: ${backupDir.absolutePath}")
                _exportDirectory.value = backupDir
                userPreferences.setExportDirectory(backupDir.absolutePath)
            }
        }
    }

    fun setExportDirectory(directory: File) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("FileManagement", "Установка новой директории экспорта: ${directory.absolutePath}")
            try {
                // Создаем DocumentFile для директории
                val dirDocFile = DocumentFile.fromFile(directory)
                
                if (dirDocFile == null || !dirDocFile.exists()) {
                    // Пробуем создать директорию
                    if (!directory.exists()) {
                        val created = directory.mkdirs()
                        Log.d("FileManagement", "Создание новой директории: $created")
                        if (!created) {
                            throw SecurityException("Не удалось создать директорию ${directory.absolutePath}")
                        }
                    }
                    
                    // Проверяем снова после создания
                    val newDirDocFile = DocumentFile.fromFile(directory)
                    if (newDirDocFile == null || !newDirDocFile.exists() || !newDirDocFile.canWrite()) {
                        throw SecurityException("Нет доступа к директории ${directory.absolutePath}")
                    }
                }
                
                // Проверяем права на запись
                if (!dirDocFile.canWrite()) {
                    Log.e("FileManagement", "Нет прав на запись в директорию: ${directory.absolutePath}")
                    throw SecurityException("Нет прав на запись в директорию")
                }
                
                _exportDirectory.value = directory
                userPreferences.setExportDirectory(directory.absolutePath)
                Log.d("FileManagement", "Директория экспорта успешно обновлена")
            } catch (e: Exception) {
                Log.e("FileManagement", "Ошибка при установке директории экспорта", e)
                // В случае ошибки используем резервный вариант
                val context = getApplication<Application>()
                val backupDir = File(context.getExternalFilesDir(null), "Lotus")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }
                Log.d("FileManagement", "Использование резервной директории: ${backupDir.absolutePath}")
                _exportDirectory.value = backupDir
                userPreferences.setExportDirectory(backupDir.absolutePath)
            }
        }
    }

    fun updateLastViewedNoteFile() {
        viewModelScope.launch(Dispatchers.IO) {
            val noteId = currentNote.value.id
            val file = File(FileUtils.getNotesDirectory(getApplication()), "$noteId.md")
            if (file.exists()) {
                _lastViewedNoteFile.value = file
            }
        }
    }

    private fun startMemoryMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while(true) {
                _totalNotesSize.value = trashManager.getTotalNotesSize()
                _appMemoryUsage.value = trashManager.getAppMemoryUsage()
                kotlinx.coroutines.delay(5000) // Обновляем каждые 5 секунд
            }
        }
    }

    fun prepareNoteForSharing(noteId: Long): File? {
        val context = getApplication<Application>()
        val content = FileUtils.readNoteContent(context, noteId)
        if (content.isEmpty()) return null

        // Получаем заголовок из первой строки
        val title = content.lines().firstOrNull()?.trim()?.removePrefix("#")?.trim() ?: "Заметка"
        
        // Форматируем имя файла (удаляем спецсимволы и пробелы заменяем на подчеркивания)
        val fileName = title
            .replace(Regex("[\\\\/:*?\"<>|]"), "") // Удаляем запрещенные символы
            .replace(Regex("\\s+"), "_") // Заменяем пробелы на подчеркивания
            .take(50) // Ограничиваем длину
            .trim('_') // Убираем подчеркивания в начале и конце
            .plus(".md")

        // Создаем временный файл в кэше
        val tempFile = File(context.cacheDir, fileName)
        tempFile.writeText(content)
        return tempFile
    }

    private fun loadLastViewedNote() {
        viewModelScope.launch(Dispatchers.IO) {
            val lastViewedNoteId = FileUtils.getLastViewedNoteId(getApplication())
            if (lastViewedNoteId != null) {
                loadNote(lastViewedNoteId)
            } else {
                // Если нет сохраненной последней просмотренной заметки,
                // загружаем последнюю редактированную как запасной вариант
                val files = FileUtils.getNoteFiles(getApplication())
                val lastEditedFile = files.maxByOrNull { it.lastModified() }
                if (lastEditedFile != null) {
                    val noteId = MarkdownUtils.extractNoteId(lastEditedFile)
                    loadNote(noteId)
                }
            }
        }
    }

    private fun loadNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = FileUtils.getNoteFiles(getApplication())
                val notesList = files.mapNotNull { file ->
                    try {
                        val content = file.readText()
                        if (content.isNotEmpty()) {
                            val noteId = MarkdownUtils.extractNoteId(file)
                            val isPreviewMode = FileUtils.readNotePreviewMode(getApplication(), noteId)
                            val isPinned = FileUtils.readNotePinned(getApplication(), noteId)
                            val order = FileUtils.readNoteOrder(getApplication(), noteId)
                            Note(
                                id = noteId,
                                title = MarkdownUtils.extractTitle(content),
                                preview = MarkdownUtils.getPreview(content),
                                content = content,
                                modifiedAt = file.lastModified(),
                                createdAt = noteId,
                                isPreviewMode = isPreviewMode,
                                isPinned = isPinned,
                                order = order
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                _notes.value = notesList.sortedWith(compareBy({ !it.isPinned }, { -it.order }, { -it.modifiedAt }))
            } catch (e: Exception) {
                // Если произошла ошибка при загрузке списка, сохраняем текущий список
            }
        }
    }

    private fun loadTrashInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            _trashNotes.value = trashManager.getTrashNotes()
            _trashSize.value = trashManager.getTrashSize()
            _isTrashOverLimit.value = trashManager.isTrashOverLimit()
            trashManager.cleanupExpiredNotes()
        }
    }

    private fun loadRetentionPeriod() {
        viewModelScope.launch {
            _currentRetentionPeriod.value = userPreferences.getTrashRetentionPeriod()
        }
    }

    fun updateNoteContent(content: String) {
        Log.d(TAG, "📝 updateNoteContent вызван с текстом: '$content'")
        val currentNote = _currentNote.value
        Log.d(TAG, "📋 Текущая заметка до обновления: '${currentNote.content}'")
        _currentNote.value = currentNote.copy(
            content = content,
            title = MarkdownUtils.extractTitle(content),
            preview = MarkdownUtils.getPreview(content),
            modifiedAt = System.currentTimeMillis()
        )
        Log.d(TAG, "✅ Заметка обновлена, новый контент: '${_currentNote.value.content}'")
    }

    fun updatePreviewMode(isPreviewMode: Boolean) {
        val currentNote = _currentNote.value
        _currentNote.value = currentNote.copy(isPreviewMode = isPreviewMode)
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveNotePreviewMode(getApplication(), currentNote.id, isPreviewMode)
        }
    }

    fun updateNotePreviewMode(isPreviewMode: Boolean) = updatePreviewMode(isPreviewMode)

    fun saveNote() {
        Log.d(TAG, "💾 saveNote вызван")
        viewModelScope.launch(Dispatchers.IO) {
            val note = _currentNote.value
            Log.d(TAG, "📋 Сохраняем заметку с контентом: '${note.content}'")
            
            // Сохраняем текущую версию как новую
            val newVersion = NoteVersion(
                noteId = note.id,
                content = note.content,
                title = note.title
            )
            
            // Обновляем список версий, сохраняя только версии текущей заметки
            val currentNoteVersions = _noteVersions.value.filter { it.noteId == note.id }
            val otherNotesVersions = _noteVersions.value.filter { it.noteId != note.id }
            
            _noteVersions.value = otherNotesVersions + (currentNoteVersions + newVersion)
                .sortedByDescending { it.createdAt }
                .take(50)

            // Сохраняем версии на диск
            saveNoteVersions()

            FileUtils.saveNote(getApplication(), note.id, note.content)
            FileUtils.saveNotePreviewMode(getApplication(), note.id, note.isPreviewMode)
            Log.d(TAG, "✅ Заметка сохранена в файл")
            
            withContext(Dispatchers.Main) {
                loadNotes()
                Log.d(TAG, "🔄 Список заметок обновлен")
            }
        }
    }

    fun createNewNote() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = FileUtils.createNoteFile(getApplication())
            val noteId = MarkdownUtils.extractNoteId(file)
            val now = System.currentTimeMillis()
            _currentNote.value = Note(noteId, "Без названия", "", "", now, now, false, false, 0)
            // Сохраняем новую заметку как последнюю просмотренную
            FileUtils.saveLastViewedNoteId(getApplication(), noteId)
        }
    }

    fun loadNote(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = FileUtils.readNoteContent(getApplication(), noteId)
                if (content.isEmpty()) {
                    // Если содержимое пустое, попробуем найти заметку в списке
                    val existingNote = _notes.value.find { it.id == noteId }
                    if (existingNote != null) {
                        _currentNote.value = existingNote
                        // Сохраняем содержимое на диск
                        FileUtils.saveNote(getApplication(), noteId, existingNote.content)
                    }
                } else {
                    val file = File(FileUtils.getNotesDirectory(getApplication()), "$noteId.md")
                    val isPinned = FileUtils.readNotePinned(getApplication(), noteId)
                    val order = FileUtils.readNoteOrder(getApplication(), noteId)
                    _currentNote.value = Note(
                        id = noteId,
                        title = MarkdownUtils.extractTitle(content),
                        preview = MarkdownUtils.getPreview(content),
                        content = content,
                        modifiedAt = file.lastModified(),
                        createdAt = noteId,
                        isPreviewMode = true,
                        isPinned = isPinned,
                        order = order
                    )
                }
                // Сбрасываем выбранную версию при смене заметки
                _selectedVersion.value = null
                // Сохраняем ID просматриваемой заметки
                FileUtils.saveLastViewedNoteId(getApplication(), noteId)
            } catch (e: Exception) {
                // В случае ошибки, попробуем загрузить из кэша
                val existingNote = _notes.value.find { it.id == noteId }
                if (existingNote != null) {
                    _currentNote.value = existingNote
                    // Сохраняем содержимое на диск
                    FileUtils.saveNote(getApplication(), noteId, existingNote.content)
                    // Сохраняем ID просматриваемой заметки
                    FileUtils.saveLastViewedNoteId(getApplication(), noteId)
                }
            }
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = _notes.value.find { it.id == noteId }
            if (note != null) {
                // Удаляем все версии заметки
                val updatedVersions = _noteVersions.value.filter { it.noteId != noteId }
                _noteVersions.value = updatedVersions
                saveNoteVersions()

                trashManager.moveToTrash(noteId, note.content)
                trashManager.deleteImagesForNote(noteId)
                FileUtils.deleteNote(getApplication(), noteId)
                FileUtils.deleteNotePreviewMode(getApplication(), noteId)
                
                if (currentNote.value.id == noteId) {
                    val files = FileUtils.getNoteFiles(getApplication())
                    val nextNote = files.firstOrNull()
                    if (nextNote != null) {
                        val nextNoteId = MarkdownUtils.extractNoteId(nextNote)
                        loadNote(nextNoteId)
                    } else {
                        createNewNote()
                    }
                }
                
                loadNotes()
                loadTrashInfo()
                updateCacheStats()
            }
        }
    }

    fun restoreNote(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = trashManager.restoreFromTrash(noteId)
            if (content != null) {
                FileUtils.saveNote(getApplication(), noteId, content)
                loadNotes()
                loadTrashInfo()
                updateCacheStats()
            }
        }
    }

    fun setSkipDeleteConfirmation(skip: Boolean) {
        viewModelScope.launch {
            userPreferences.setSkipDeleteConfirmation(skip)
        }
    }

    fun setRetentionPeriod(period: TrashManager.RetentionPeriod) {
        viewModelScope.launch {
            userPreferences.setTrashRetentionPeriod(period)
            _currentRetentionPeriod.value = period
            loadTrashInfo() // Обновляем информацию о корзине
        }
    }

    fun deleteNoteFromTrash(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            trashManager.deleteNoteFromTrash(noteId)
            loadTrashInfo()
        }
    }

    fun clearTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            trashManager.clearTrash()
            loadTrashInfo()
        }
    }

    fun toggleNotePinned(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val notes = _notes.value.toMutableList()
            val noteIndex = notes.indexOfFirst { it.id == noteId }
            if (noteIndex != -1) {
                val note = notes[noteIndex]
                val pinnedNotes = notes.filter { it.isPinned }
                val newNote = note.copy(
                    isPinned = !note.isPinned,
                    order = if (!note.isPinned) pinnedNotes.size else 0
                )
                notes[noteIndex] = newNote
                FileUtils.saveNotePinned(getApplication(), noteId, newNote.isPinned)
                FileUtils.saveNoteOrder(getApplication(), noteId, newNote.order)
                _notes.value = notes.sortedWith(compareBy({ !it.isPinned }, { -it.order }, { -it.modifiedAt }))
            }
        }
    }

    fun moveNote(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val notes = _notes.value.toMutableList()
            val note = notes.removeAt(fromIndex)
            notes.add(toIndex, note)
            
            // Обновляем порядок для всех заметок в соответствующей секции
            if (note.isPinned) {
                // Обновляем порядок закрепленных заметок
                val pinnedNotes = notes.filter { it.isPinned }
                pinnedNotes.forEachIndexed { index, pinnedNote ->
                    val updatedNote = pinnedNote.copy(order = pinnedNotes.size - index)
                    val noteIndex = notes.indexOfFirst { it.id == pinnedNote.id }
                    if (noteIndex != -1) {
                        notes[noteIndex] = updatedNote
                        FileUtils.saveNoteOrder(getApplication(), updatedNote.id, updatedNote.order)
                    }
                }
            } else {
                // Обновляем порядок незакрепленных заметок
                val unpinnedNotes = notes.filter { !it.isPinned }
                unpinnedNotes.forEachIndexed { index, unpinnedNote ->
                    val updatedNote = unpinnedNote.copy(order = unpinnedNotes.size - index)
                    val noteIndex = notes.indexOfFirst { it.id == unpinnedNote.id }
                    if (noteIndex != -1) {
                        notes[noteIndex] = updatedNote
                        FileUtils.saveNoteOrder(getApplication(), updatedNote.id, updatedNote.order)
                    }
                }
            }
            
            _notes.value = notes
        }
    }

    fun onExportDirectorySelect(directory: File) {
        setExportDirectory(directory)
    }

    private fun updateCacheStats() {
        viewModelScope.launch(Dispatchers.IO) {
            _cacheStats.value = trashManager.getCacheStats()
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            trashManager.clearCache()
            updateCacheStats()
        }
    }

    fun clearImagesCache() {
        viewModelScope.launch(Dispatchers.IO) {
            trashManager.clearImagesCache()
            updateCacheStats()
        }
    }

    fun clearFilesCache() {
        viewModelScope.launch(Dispatchers.IO) {
            trashManager.clearFilesCache()
            updateCacheStats()
        }
    }

    fun handleSpeechResult(requestCode: Int, resultCode: Int, data: Intent?) {
        speechRecognitionManager.handleActivityResult(requestCode, resultCode, data)
    }

    fun startSpeechRecognition() {
        Log.d(TAG, "🎙️ Запуск распознавания речи")
        speechRecognitionManager.startListening { text, isFinal ->
            // Переносим всю обработку в главный поток
            viewModelScope.launch(Dispatchers.Main) {
                Log.d(TAG, "📝 Получен текст от распознавания: '$text', isFinal: $isFinal")
                if (isFinal && text.isNotEmpty()) {
                    val currentNote = _currentNote.value
                    val currentText = currentNote.content
                    Log.d(TAG, "📋 Текущий текст заметки: '$currentText'")
                    
                    Log.d(TAG, "⚙️ Настройки обработки: TextProcessor=${_isTextProcessorEnabled.value}, Groq=${_isGroqEnabled.value}")
                    if (_isGroqEnabled.value) {
                        Log.d(TAG, "🤖 Отправка текста в Groq")
                        try {
                            val processed = processTextWithGroq(text)
                            val newText = if (currentText.isEmpty()) {
                                processed.trim()
                            } else {
                                currentText + (if (currentText.endsWith("\n")) "" else "\n") + processed.trim()
                            }
                            Log.d(TAG, "✨ Подготовлен новый текст (Groq): '$newText'")
                            _currentNote.value = currentNote.copy(
                                content = newText,
                                title = MarkdownUtils.extractTitle(newText),
                                preview = MarkdownUtils.getPreview(newText),
                                modifiedAt = System.currentTimeMillis()
                            )
                            saveNote()
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Ошибка обработки Groq", e)
                            val newText = if (currentText.isEmpty()) {
                                text.trim()
                            } else {
                                currentText + (if (currentText.endsWith("\n")) "" else "\n") + text.trim()
                            }
                            Log.d(TAG, "✨ Подготовлен новый текст (после ошибки Groq): '$newText'")
                            _currentNote.value = currentNote.copy(
                                content = newText,
                                title = MarkdownUtils.extractTitle(newText),
                                preview = MarkdownUtils.getPreview(newText),
                                modifiedAt = System.currentTimeMillis()
                            )
                            saveNote()
                        }
                        return@launch
                    }
                    
                    val processedText = when {
                        _isTextProcessorEnabled.value -> {
                            Log.d(TAG, "🔧 Обработка через TextProcessor")
                            TextProcessor.process(text).trim()
                        }
                        else -> {
                            Log.d(TAG, "➡️ Текст без обработки")
                            text.trim()
                        }
                    }
                    
                    val newText = if (currentText.isEmpty()) {
                        processedText
                    } else {
                        currentText + (if (currentText.endsWith("\n")) "" else "\n") + processedText
                    }
                    
                    Log.d(TAG, "✨ Подготовлен новый текст: '$newText'")
                    _currentNote.value = currentNote.copy(
                        content = newText,
                        title = MarkdownUtils.extractTitle(newText),
                        preview = MarkdownUtils.getPreview(newText),
                        modifiedAt = System.currentTimeMillis()
                    )
                    saveNote()
                }
            }
        }
    }

    fun stopSpeechRecognition() {
        Log.d(TAG, "🛑 Остановка распознавания речи")
        speechRecognitionManager.stopListening()
    }

    fun setTextProcessorEnabled(enabled: Boolean) {
        _isTextProcessorEnabled.value = enabled
    }

    fun setGroqEnabled(enabled: Boolean) {
        _isGroqEnabled.value = enabled
    }

    private suspend fun processTextWithGroq(text: String): String {
        Log.d(TAG, "🤖 Начало обработки текста через Groq: '$text'")
        return try {
            val result = GroqTextProcessor.processText(text)
            Log.d(TAG, "📥 Получен результат от Groq")
            result.fold(
                onSuccess = { processedText -> 
                    Log.d(TAG, "✅ Успешная обработка Groq: '$processedText'")
                    processedText 
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Ошибка обработки Groq", error)
                    text
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "💥 Исключение при обработке Groq", e)
            text
        }
    }

    fun setSystemPrompt(prompt: String) {
        _systemPrompt.value = prompt
        GroqTextProcessor.setSystemPrompt(prompt)
    }

    fun setFileManagementEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setFileManagementEnabled(enabled)
            _isFileManagementEnabled.value = enabled
        }
    }

    private fun loadFileManagementEnabled() {
        viewModelScope.launch {
            userPreferences.fileManagementEnabled.collect { enabled ->
                _isFileManagementEnabled.value = enabled
            }
        }
    }

    fun setTodoEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setTodoEnabled(enabled)
            _isTodoEnabled.value = enabled
        }
    }

    private fun loadTodoEnabled() {
        viewModelScope.launch {
            userPreferences.todoEnabled.collect { enabled ->
                _isTodoEnabled.value = enabled
            }
        }
    }

    fun toggleVersionHistory() {
        _isVersionHistoryVisible.value = !_isVersionHistoryVisible.value
        if (!_isVersionHistoryVisible.value) {
            _selectedVersion.value = null
        }
    }

    fun selectVersion(version: NoteVersion?) {
        _selectedVersion.value = version
    }

    fun applySelectedVersion() {
        val selectedVer = _selectedVersion.value ?: return
        _currentNote.value = _currentNote.value.copy(
            content = selectedVer.content,
            title = selectedVer.title,
            modifiedAt = System.currentTimeMillis()
        )
        _selectedVersion.value = null
        _isVersionHistoryVisible.value = false
        saveNote()
    }

    fun deleteVersion(version: NoteVersion) {
        viewModelScope.launch {
            val currentVersions = _noteVersions.value.toMutableList()
            val index = currentVersions.indexOfFirst { it.id == version.id }
            
            if (index != -1) {
                currentVersions.removeAt(index)
                
                // Если удаляем текущую версию, откатываемся на предыдущую
                if (_selectedVersion.value?.id == version.id) {
                    // Находим предыдущую версию для той же заметки
                    val previousVersion = currentVersions
                        .filter { it.noteId == version.noteId }
                        .maxByOrNull { it.createdAt }
                    
                    _selectedVersion.value = previousVersion
                    
                    // Если это была последняя версия, применяем предыдущую автоматически
                    if (previousVersion != null) {
                        _currentNote.value = _currentNote.value.copy(
                            content = previousVersion.content,
                            title = previousVersion.title,
                            modifiedAt = System.currentTimeMillis()
                        )
                    }
                }
                
                _noteVersions.value = currentVersions
                // Сохраняем изменения на диск
                saveNoteVersions()
            }
        }
    }

    private fun loadNoteVersions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val versionsDir = File(getApplication<Application>().filesDir, "versions")
                if (!versionsDir.exists()) {
                    versionsDir.mkdirs()
                    return@launch
                }

                val versions = mutableListOf<NoteVersion>()
                versionsDir.listFiles()?.forEach { file ->
                    try {
                        val content = file.readText()
                        val lines = content.lines()
                        if (lines.size >= 4) {
                            val id = lines[0].toLong()
                            val noteId = lines[1].toLong()
                            val title = lines[2]
                            val createdAt = lines[3].toLong()
                            val versionContent = lines.drop(4).joinToString("\n")
                            
                            versions.add(NoteVersion(
                                id = id,
                                noteId = noteId,
                                content = versionContent,
                                title = title,
                                createdAt = createdAt
                            ))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при загрузке версии: ${file.name}", e)
                    }
                }
                _noteVersions.value = versions.sortedByDescending { it.createdAt }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке версий", e)
            }
        }
    }

    private fun saveNoteVersions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val versionsDir = File(getApplication<Application>().filesDir, "versions")
                if (!versionsDir.exists()) {
                    versionsDir.mkdirs()
                }

                // Очищаем директорию
                versionsDir.listFiles()?.forEach { it.delete() }

                // Сохраняем каждую версию в отдельный файл
                _noteVersions.value.forEach { version ->
                    val file = File(versionsDir, "${version.id}.txt")
                    val content = buildString {
                        appendLine(version.id)
                        appendLine(version.noteId)
                        appendLine(version.title)
                        appendLine(version.createdAt)
                        append(version.content)
                    }
                    file.writeText(content)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при сохранении версий", e)
            }
        }
    }

    private fun loadFontSize() {
        viewModelScope.launch {
            userPreferences.fontSize.collect { size ->
                _fontSize.value = size
            }
        }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch {
            userPreferences.setFontSize(size)
            _fontSize.value = size
        }
    }

    fun saveCurrentNote() {
        viewModelScope.launch(Dispatchers.IO) {
            val note = _currentNote.value
            
            // Сохраняем текущую версию как новую
            val newVersion = NoteVersion(
                noteId = note.id,
                content = note.content,
                title = note.title
            )
            
            // Обновляем список версий
            val currentNoteVersions = _noteVersions.value.filter { it.noteId == note.id }
            val otherNotesVersions = _noteVersions.value.filter { it.noteId != note.id }
            
            _noteVersions.value = otherNotesVersions + (currentNoteVersions + newVersion)
                .sortedByDescending { it.createdAt }
                .take(50)

            // Сохраняем версии на диск
            saveNoteVersions()

            FileUtils.saveNote(getApplication(), note.id, note.content)
            FileUtils.saveNotePreviewMode(getApplication(), note.id, note.isPreviewMode)
            
            withContext(Dispatchers.Main) {
                loadNotes()
            }
        }
    }

    fun setVersionHistoryVisible(isVisible: Boolean) {
        _isVersionHistoryVisible.value = isVisible
        if (!isVisible) {
            _selectedVersion.value = null
        }
    }

    fun readNoteContent(file: File): String {
        return FileUtils.readNoteContent(getApplication(), file.nameWithoutExtension.toLong())
    }

    suspend fun createNewNoteWithContent(content: String): Long {
        return withContext(Dispatchers.IO) {
            val file = FileUtils.createNoteFile(getApplication())
            val noteId = MarkdownUtils.extractNoteId(file)
            val now = System.currentTimeMillis()
            
            // Создаем новую заметку с импортированным содержимым
            val newNote = Note(
                id = noteId,
                title = MarkdownUtils.extractTitle(content),
                preview = MarkdownUtils.getPreview(content),
                content = content,
                modifiedAt = now,
                createdAt = now,
                isPreviewMode = false,
                isPinned = false,
                order = 0
            )
            
            _currentNote.value = newNote
            FileUtils.saveNote(getApplication(), noteId, content)
            FileUtils.saveLastViewedNoteId(getApplication(), noteId)
            loadNotes()
            noteId
        }
    }

    fun exportNotes(directory: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _exportProgress.value = ExportProgress.Idle
            Log.d("FileManagement", "Начало экспорта заметок в директорию: ${directory.absolutePath}")
            try {
                val context = getApplication<Application>()
                var notes = _notes.value
                
                // Фильтруем заметки если включен режим экспорта только новых
                if (_exportOnlyNew.value) {
                    val lastExport = _lastExportTime.value ?: 0L
                    notes = notes.filter { it.modifiedAt > lastExport }
                    Log.d("FileManagement", "Режим экспорта новых заметок. Найдено ${notes.size} новых заметок")
                }
                
                Log.d("FileManagement", "Количество заметок для экспорта: ${notes.size}")
                
                // Проверяем и создаем директорию
                if (!directory.exists()) {
                    val created = directory.mkdirs()
                    Log.d("FileManagement", "Создание директории: $created")
                    if (!created) {
                        throw SecurityException("Не удалось создать директорию ${directory.absolutePath}")
                    }
                }
                
                if (!directory.canWrite()) {
                    throw SecurityException("Нет доступа к директории ${directory.absolutePath}")
                }
                
                // Создаем DocumentFile для директории
                val dirDocFile = DocumentFile.fromFile(directory)
                if (dirDocFile == null || !dirDocFile.exists()) {
                    throw SecurityException("Не удалось получить доступ к директории через DocumentFile")
                }
                
                notes.forEachIndexed { index, note ->
                    _exportProgress.value = ExportProgress.InProgress(index + 1, notes.size)
                    try {
                        val title = note.title.ifEmpty { "Без названия" }
                        Log.d("FileManagement", "Экспорт заметки $index: $title")
                        
                        // Улучшенная обработка имени файла
                        val fileName = title
                            .replace(Regex("[\\\\/:*?\"<>|]"), "") // Удаляем запрещенные символы
                            .replace(Regex("\\s+"), "_") // Заменяем пробелы на подчеркивания
                            .take(50) // Ограничиваем длину
                            .trim('_') // Убираем подчеркивания в начале и конце
                            .let { if (it.isEmpty()) "note" else it }
                            .removeSuffix(".md") // Удаляем .md если оно уже есть в конце
                            
                        var finalFileName = fileName
                        var counter = 1
                        
                        // Проверяем существование файла через DocumentFile
                        while (dirDocFile.findFile("$finalFileName.md") != null) {
                            finalFileName = "${fileName}_${counter}"
                            counter++
                        }
                        
                        // Создаем новый файл через DocumentFile, не добавляя .md к имени,
                        // так как оно уже будет добавлено при создании файла
                        val newFile = dirDocFile.createFile("text/markdown", finalFileName)
                        if (newFile == null) {
                            throw IOException("Не удалось создать файл ${finalFileName}")
                        }
                        
                        // Записываем содержимое через потоки
                        context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                            outputStream.write(note.content.toByteArray())
                            outputStream.flush()
                        } ?: throw IOException("Не удалось открыть поток для записи")
                        
                        Log.d("FileManagement", "Заметка успешно экспортирована в файл: ${newFile.name}")
                        
                    } catch (e: Exception) {
                        Log.e("FileManagement", "Ошибка при экспорте заметки ${note.id}", e)
                        _exportProgress.value = ExportProgress.Error("Ошибка при экспорте заметки: ${note.title}\n${e.message}")
                        return@launch
                    }
                }
                
                // Сохраняем время последнего экспорта
                if (_exportOnlyNew.value) {
                    saveLastExportTime(System.currentTimeMillis())
                }
                
                Log.d("FileManagement", "Экспорт заметок завершен")
                _exportProgress.value = ExportProgress.Success
                
            } catch (e: Exception) {
                Log.e("FileManagement", "Ошибка при экспорте заметок", e)
                _exportProgress.value = ExportProgress.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun resetExportProgress() {
        _exportProgress.value = ExportProgress.Idle
    }

    private fun loadExportOnlyNew() {
        viewModelScope.launch {
            userPreferences.exportOnlyNew.collect { enabled ->
                _exportOnlyNew.value = enabled
            }
        }
    }

    fun setExportOnlyNew(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setExportOnlyNew(enabled)
            _exportOnlyNew.value = enabled
        }
    }

    private fun loadLastExportTime() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(getApplication<Application>().filesDir, "last_export_time.txt")
            if (file.exists()) {
                _lastExportTime.value = file.readText().toLongOrNull()
            }
        }
    }

    private fun saveLastExportTime(time: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(getApplication<Application>().filesDir, "last_export_time.txt")
            file.writeText(time.toString())
            _lastExportTime.value = time
        }
    }

    fun setSelectedGroqModel(modelId: String) {
        _selectedGroqModel.value = modelId
        GroqTextProcessor.setModel(modelId)
    }

    fun loadGroqModels() {
        viewModelScope.launch {
            _isLoadingModels.value = true
            _modelLoadError.value = null
            try {
                val response = RetrofitClient.groqService.listModels()
                if (response.isSuccessful) {
                    response.body()?.let { modelsResponse ->
                        _availableGroqModels.value = modelsResponse.data
                    } ?: run {
                        _modelLoadError.value = "Пустой ответ от сервера"
                    }
                } else {
                    _modelLoadError.value = "Ошибка загрузки моделей: ${response.code()}"
                }
            } catch (e: Exception) {
                _modelLoadError.value = "Ошибка при загрузке моделей: ${e.message}"
            } finally {
                _isLoadingModels.value = false
            }
        }
    }

    fun testGroqModel(testPrompt: String = "Привет! Это тестовое сообщение.") {
        viewModelScope.launch {
            _testResult.value = "Тестирование..."
            try {
                val response = RetrofitClient.groqService.generateResponse(
                    GroqRequest(
                        messages = listOf(Message("user", testPrompt)),
                        model = _selectedGroqModel.value,
                        temperature = 0,
                        max_completion_tokens = 1024,
                        top_p = 1,
                        stream = false,
                        stop = null
                    )
                )
                _testResult.value = when {
                    response.isSuccessful -> "✅ Успешно (${response.code()})"
                    response.code() == 401 -> "❌ Ошибка авторизации (401)"
                    response.code() == 404 -> "❌ Модель не найдена (404)"
                    response.code() == 429 -> "❌ Слишком много запросов (429)"
                    response.code() == 500 -> "❌ Внутренняя ошибка сервера (500)"
                    response.code() == 503 -> "❌ Сервис недоступен (503)"
                    else -> "❌ Ошибка: ${response.code()}"
                }
            } catch (e: Exception) {
                _testResult.value = "❌ Ошибка: ${e.message}"
            }
        }
    }
} 