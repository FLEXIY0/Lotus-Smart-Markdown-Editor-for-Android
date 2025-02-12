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

    private val _isFileManagementEnabled = MutableStateFlow(false)
    val isFileManagementEnabled = _isFileManagementEnabled.asStateFlow()

    private val _systemPrompt = MutableStateFlow<String?>(null)
    val systemPrompt = _systemPrompt.asStateFlow()

    private val _isTodoEnabled = MutableStateFlow(true)
    val isTodoEnabled = _isTodoEnabled.asStateFlow()

    private val TAG = "SPEECH_DEBUG"

    init {
        loadNotes()
        loadLastViewedNote()
        loadTrashInfo()
        loadRetentionPeriod()
        startMemoryMonitoring()
        initExportDirectory()
        updateCacheStats()
        speechRecognitionManager = SpeechRecognitionManager(getApplication())
        loadTodoEnabled()
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
            val defaultDir = File(context.getExternalFilesDir(null), "Lotus")
            if (!defaultDir.exists()) {
                defaultDir.mkdirs()
            }
            _exportDirectory.value = defaultDir
        }
    }

    fun setExportDirectory(directory: File) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!directory.exists()) {
                directory.mkdirs()
            }
            _exportDirectory.value = directory
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
        val currentNote = _currentNote.value
        _currentNote.value = currentNote.copy(
            content = content,
            title = MarkdownUtils.extractTitle(content),
            preview = MarkdownUtils.getPreview(content),
            modifiedAt = System.currentTimeMillis()
        )
    }

    fun updatePreviewMode(isPreviewMode: Boolean) {
        val currentNote = _currentNote.value
        _currentNote.value = currentNote.copy(isPreviewMode = isPreviewMode)
    }

    fun saveNote() {
        viewModelScope.launch(Dispatchers.IO) {
            val note = _currentNote.value
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
                .take(50) // Ограничиваем количество версий для текущей заметки

            FileUtils.saveNote(getApplication(), note.id, note.content)
            FileUtils.saveNotePreviewMode(getApplication(), note.id, note.isPreviewMode)
            loadNotes()
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
                trashManager.moveToTrash(noteId, note.content)
                trashManager.deleteImagesForNote(noteId) // Удаляем изображения
                FileUtils.deleteNote(getApplication(), noteId)
                FileUtils.deleteNotePreviewMode(getApplication(), noteId)
                
                // Если удаляем текущую заметку, загружаем другую
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
            Log.d(TAG, "📝 Получен текст от распознавания: '$text', isFinal: $isFinal")
            viewModelScope.launch {
                val currentNote = _currentNote.value
                val currentText = currentNote.content
                Log.d(TAG, "📋 Текущий текст заметки: '$currentText'")
                
                Log.d(TAG, "⚙️ Настройки обработки: TextProcessor=${_isTextProcessorEnabled.value}, Groq=${_isGroqEnabled.value}")
                val processedText = when {
                    _isGroqEnabled.value -> {
                        Log.d(TAG, "🤖 Отправка текста в Groq")
                        processTextWithGroq(text)
                    }
                    _isTextProcessorEnabled.value -> {
                        Log.d(TAG, "🔧 Обработка через TextProcessor")
                        TextProcessor.process(text)
                    }
                    else -> {
                        Log.d(TAG, "➡️ Текст без обработки")
                        text
                    }
                }
                
                val newText = if (currentText.isEmpty()) {
                    Log.d(TAG, "📝 Создание новой заметки с текстом")
                    processedText
                } else {
                    Log.d(TAG, "📝 Добавление текста к существующей заметке")
                    "$currentText\n$processedText"
                }
                
                Log.d(TAG, "💾 Обновление содержимого заметки")
                updateNoteContent(newText)
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
        _isFileManagementEnabled.value = enabled
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
            }
        }
    }
} 