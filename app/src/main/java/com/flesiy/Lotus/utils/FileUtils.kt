package com.flesiy.Lotus.utils

import android.content.Context
import java.io.File

object FileUtils {
    private const val NOTES_DIRECTORY = "notes"
    private const val PREVIEW_MODE_DIRECTORY = "preview_mode"
    private const val PINNED_NOTES_DIRECTORY = "pinned_notes"
    private const val NOTE_ORDER_DIRECTORY = "note_order"
    private const val LAST_VIEWED_NOTE_FILE = "last_viewed_note"

    fun getNotesDirectory(context: Context): File {
        val directory = File(context.filesDir, NOTES_DIRECTORY)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun getPreviewModeDirectory(context: Context): File {
        val directory = File(context.filesDir, PREVIEW_MODE_DIRECTORY)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun getPinnedNotesDirectory(context: Context): File {
        val directory = File(context.filesDir, PINNED_NOTES_DIRECTORY)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun getNoteOrderDirectory(context: Context): File {
        val directory = File(context.filesDir, NOTE_ORDER_DIRECTORY)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    fun createNoteFile(context: Context): File {
        val directory = getNotesDirectory(context)
        val timestamp = System.currentTimeMillis()
        return File(directory, "$timestamp.md")
    }

    fun getNoteFiles(context: Context): List<File> {
        val directory = getNotesDirectory(context)
        return directory.listFiles()?.filter { it.extension == "md" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteNote(context: Context, noteId: Long) {
        val directory = getNotesDirectory(context)
        val file = File(directory, "$noteId.md")
        if (file.exists()) {
            file.delete()
        }
    }

    fun saveNote(context: Context, noteId: Long, content: String) {
        val directory = getNotesDirectory(context)
        val file = File(directory, "$noteId.md")
        file.writeText(content)
    }

    fun readNoteContent(context: Context, noteId: Long): String {
        val directory = getNotesDirectory(context)
        val file = File(directory, "$noteId.md")
        return if (file.exists()) file.readText() else ""
    }

    fun saveNotePreviewMode(context: Context, noteId: Long, isPreviewMode: Boolean) {
        val directory = getPreviewModeDirectory(context)
        val file = File(directory, "$noteId.preview")
        file.writeText(isPreviewMode.toString())
    }

    fun readNotePreviewMode(context: Context, noteId: Long): Boolean {
        val directory = getPreviewModeDirectory(context)
        val file = File(directory, "$noteId.preview")
        return if (file.exists()) {
            file.readText().toBoolean()
        } else {
            false
        }
    }

    fun deleteNotePreviewMode(context: Context, noteId: Long) {
        val directory = getPreviewModeDirectory(context)
        val file = File(directory, "$noteId.preview")
        if (file.exists()) {
            file.delete()
        }
    }

    fun saveLastViewedNoteId(context: Context, noteId: Long) {
        val file = File(context.filesDir, LAST_VIEWED_NOTE_FILE)
        file.writeText(noteId.toString())
    }

    fun getLastViewedNoteId(context: Context): Long? {
        val file = File(context.filesDir, LAST_VIEWED_NOTE_FILE)
        return if (file.exists()) {
            try {
                file.readText().toLong()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun saveNotePinned(context: Context, noteId: Long, isPinned: Boolean) {
        val directory = getPinnedNotesDirectory(context)
        val file = File(directory, "$noteId.pinned")
        if (isPinned) {
            file.writeText(isPinned.toString())
        } else {
            file.delete()
        }
    }

    fun readNotePinned(context: Context, noteId: Long): Boolean {
        val directory = getPinnedNotesDirectory(context)
        val file = File(directory, "$noteId.pinned")
        return file.exists() && file.readText().toBoolean()
    }

    fun saveNoteOrder(context: Context, noteId: Long, order: Int) {
        val directory = getNoteOrderDirectory(context)
        val file = File(directory, "$noteId.order")
        file.writeText(order.toString())
    }

    fun readNoteOrder(context: Context, noteId: Long): Int {
        val directory = getNoteOrderDirectory(context)
        val file = File(directory, "$noteId.order")
        return if (file.exists()) {
            try {
                file.readText().toInt()
            } catch (e: Exception) {
                0
            }
        } else {
            0
        }
    }
} 