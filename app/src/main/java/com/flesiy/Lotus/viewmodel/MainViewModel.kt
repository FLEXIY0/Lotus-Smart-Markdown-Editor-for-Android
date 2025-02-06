package com.flesiy.Lotus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flesiy.Lotus.data.UserPreferences
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
    val isPreviewMode: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    private val _currentNote = MutableStateFlow(Note(0L, "", "", "", 0L, 0L))
    val currentNote: StateFlow<Note> = _currentNote

    private val userPreferences = UserPreferences(application)
    val skipDeleteConfirmation = userPreferences.skipDeleteConfirmation.stateIn(
        viewModelScope,
         SharingStarted.WhileSubscribed(5000),
        false
    )

    init {
        loadNotes()
        loadLastViewedNote()
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
                            Note(
                                id = noteId,
                                title = MarkdownUtils.extractTitle(content),
                                preview = MarkdownUtils.getPreview(content),
                                content = content,
                                modifiedAt = file.lastModified(),
                                createdAt = noteId,
                                isPreviewMode = isPreviewMode
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                _notes.value = notesList
            } catch (e: Exception) {
                // Если произошла ошибка при загрузке списка, сохраняем текущий список
            }
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
            _currentNote.value = Note(noteId, "Без названия", "", "", now, now, false)
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
                    _currentNote.value = Note(
                        id = noteId,
                        title = MarkdownUtils.extractTitle(content),
                        preview = MarkdownUtils.getPreview(content),
                        content = content,
                        modifiedAt = file.lastModified(),
                        createdAt = noteId,
                        isPreviewMode = isPreviewMode
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
                    // Если заметок больше нет, создаем новую
                    createNewNote()
                }
            }
            
            loadNotes()
        }
    }

    fun setSkipDeleteConfirmation(skip: Boolean) {
        viewModelScope.launch {
            userPreferences.setSkipDeleteConfirmation(skip)
        }
    }
} 