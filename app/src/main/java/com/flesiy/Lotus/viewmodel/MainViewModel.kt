package com.flesiy.Lotus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flesiy.Lotus.data.UserPreferences
import com.flesiy.Lotus.data.TrashManager
import com.flesiy.Lotus.data.TrashNote
import com.flesiy.Lotus.utils.FileUtils
import com.flesiy.Lotus.utils.MarkdownUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class Note(
    val id: Long,
    val title: String,
    val preview: String,
    val content: String,
    val modifiedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val isPreviewMode: Boolean = false,
    val isPinned: Boolean = false,
    val order: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    private val _currentNote = MutableStateFlow(Note(0L, "", "", "", 0L, 0L))
    val currentNote: StateFlow<Note> = _currentNote

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

    init {
        loadNotes()
        loadLastViewedNote()
        loadTrashInfo()
        loadRetentionPeriod()
        startMemoryMonitoring()
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
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveNotePreviewMode(getApplication(), currentNote.id, isPreviewMode)
        }
    }

    fun saveNote() {
        viewModelScope.launch(Dispatchers.IO) {
            val note = _currentNote.value
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
                    val isPreviewMode = FileUtils.readNotePreviewMode(getApplication(), noteId)
                    val isPinned = FileUtils.readNotePinned(getApplication(), noteId)
                    val order = FileUtils.readNoteOrder(getApplication(), noteId)
                    _currentNote.value = Note(
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
                }
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
            
            // Обновляем порядок для закрепленных заметок
            if (note.isPinned) {
                val pinnedNotes = notes.filter { it.isPinned }
                pinnedNotes.forEachIndexed { index, pinnedNote ->
                    val updatedNote = pinnedNote.copy(order = pinnedNotes.size - index)
                    val noteIndex = notes.indexOfFirst { it.id == pinnedNote.id }
                    if (noteIndex != -1) {
                        notes[noteIndex] = updatedNote
                        FileUtils.saveNoteOrder(getApplication(), updatedNote.id, updatedNote.order)
                    }
                }
            }
            
            _notes.value = notes
        }
    }
} 