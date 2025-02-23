package com.flesiy.Lotus.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.flesiy.Lotus.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SearchNote(
    val id: Long,
    val content: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDialog(
    onDismiss: () -> Unit,
    onNoteSelected: (Long) -> Unit,
    notes: List<SearchNote>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(100) // Небольшая задержка для анимации
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(0.95f),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Поле поиска
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.search_notes)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // Результаты поиска
                if (searchQuery.isNotEmpty()) {
                    val filteredNotes = notes.filter { note ->
                        note.content.contains(searchQuery, ignoreCase = true)
                    }.sortedByDescending { note ->
                        // Приоритет по количеству совпадений
                        note.content.split(searchQuery, ignoreCase = true).size - 1
                    }

                    if (filteredNotes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Ничего не найдено",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(filteredNotes) { note ->
                                SearchResultItem(
                                    note = note,
                                    searchQuery = searchQuery,
                                    onClick = {
                                        scope.launch {
                                            onNoteSelected(note.id)
                                            onDismiss()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultItem(
    note: SearchNote,
    searchQuery: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = {
            val title = note.content.lines().firstOrNull() ?: ""
            Text(
                highlightText(title, searchQuery),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            val context = findMatchContext(note.content, searchQuery)
            Text(
                highlightText(context, searchQuery),
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

private fun findMatchContext(text: String, query: String): String {
    if (query.isEmpty()) return text.take(200)

    val pattern = query.toRegex(RegexOption.IGNORE_CASE)
    val match = pattern.find(text) ?: return text.take(200)
    
    // Определяем границы контекста (150 символов до и после совпадения)
    val contextSize = 150
    val matchStart = match.range.first
    val matchEnd = match.range.last + 1
    
    val start = (matchStart - contextSize).coerceAtLeast(0)
    val end = (matchEnd + contextSize).coerceAtMost(text.length)
    
    return buildString {
        // Добавляем многоточие в начале, если начали не с начала текста
        if (start > 0) append("... ")
        
        // Получаем контекст
        val context = text.substring(start, end)
        
        // Разбиваем длинный текст на части примерно по 80 символов для читаемости
        val chunks = context.chunked(80)
        append(chunks.joinToString("\n"))
        
        // Добавляем многоточие в конце, если есть ещё текст
        if (end < text.length) append(" ...")
    }
}

private fun highlightText(text: String, query: String): AnnotatedString {
    if (query.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        var lastIndex = 0
        val pattern = query.toRegex(RegexOption.IGNORE_CASE)
        
        pattern.findAll(text).forEach { result ->
            // Добавляем текст до совпадения
            append(text.substring(lastIndex, result.range.first))
            
            // Добавляем подсвеченное совпадение
            withStyle(SpanStyle(
                background = Color(0xFFFFEB3B).copy(alpha = 0.3f),
                color = Color(0xFF000000)
            )) {
                append(result.value)
            }
            
            lastIndex = result.range.last + 1
        }
        
        // Добавляем оставшийся текст
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
} 