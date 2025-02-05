package com.flesiy.Lotus.utils

import java.io.File

object MarkdownUtils {
    fun extractTitle(content: String): String {
        val lines = content.lines()
        return lines.firstOrNull { line ->
            line.trim().startsWith("#")
        }?.trim()?.removePrefix("#")?.trim() ?: "Без названия"
    }

    fun getPreview(content: String): String {
        val lines = content.lines()
        val firstNonEmptyLine = lines.firstOrNull { it.isNotBlank() } ?: ""
        return if (firstNonEmptyLine.startsWith("#")) {
            lines.drop(1).firstOrNull { it.isNotBlank() } ?: ""
        } else {
            firstNonEmptyLine
        }
    }

    fun extractNoteId(file: File): Long {
        return file.nameWithoutExtension.toLongOrNull() ?: 0L
    }
} 