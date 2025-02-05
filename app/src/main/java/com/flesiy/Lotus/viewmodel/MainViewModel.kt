package com.flesiy.Lotus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flesiy.Lotus.utils.FileUtils
import com.flesiy.Lotus.utils.MarkdownUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val createdAt: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    private val _currentNote = MutableStateFlow(Note(0L, "", "", "", 0L, 0L))
    val currentNote: StateFlow<Note> = _currentNote

    init {
        loadNotes()
        loadLastEditedNote()
    }

    private fun loadLastEditedNote() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = FileUtils.getNoteFiles(getApplication())
            val lastEditedFile = files.maxByOrNull { it.lastModified() }
            if (lastEditedFile != null) {
                val content = lastEditedFile.readText()
                val noteId = MarkdownUtils.extractNoteId(lastEditedFile)
                _currentNote.value = Note(
                    id = noteId,
                    title = MarkdownUtils.extractTitle(content),
                    preview = MarkdownUtils.getPreview(content),
                    content = content,
                    modifiedAt = lastEditedFile.lastModified(),
                    createdAt = noteId // используем id как timestamp создания
                )
            }
        }
    }

    private fun loadNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = FileUtils.getNoteFiles(getApplication())
            val notesList = files.map { file ->
                val content = file.readText()
                Note(
                    id = MarkdownUtils.extractNoteId(file),
                    title = MarkdownUtils.extractTitle(content),
                    preview = MarkdownUtils.getPreview(content),
                    content = content,
                    modifiedAt = file.lastModified(),
                    createdAt = MarkdownUtils.extractNoteId(file)
                )
            }
            _notes.value = notesList
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

    fun saveNote() {
        viewModelScope.launch(Dispatchers.IO) {
            val note = _currentNote.value
            FileUtils.saveNote(getApplication(), note.id, note.content)
            loadNotes()
        }
    }

    fun createNewNote() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = FileUtils.createNoteFile(getApplication())
            val noteId = MarkdownUtils.extractNoteId(file)
            val now = System.currentTimeMillis()
            _currentNote.value = Note(noteId, "Без названия", "", "", now, now)
        }
    }

    fun loadNote(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = FileUtils.readNoteContent(getApplication(), noteId)
            val file = File(FileUtils.getNotesDirectory(getApplication()), "$noteId.md")
            _currentNote.value = Note(
                id = noteId,
                title = MarkdownUtils.extractTitle(content),
                preview = MarkdownUtils.getPreview(content),
                content = content,
                modifiedAt = file.lastModified(),
                createdAt = noteId
            )
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.deleteNote(getApplication(), noteId)
            loadNotes()
        }
    }
} 