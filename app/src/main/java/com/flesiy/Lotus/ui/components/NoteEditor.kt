package com.flesiy.Lotus.ui.components

import android.util.Log
import android.widget.EditText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flesiy.Lotus.viewmodel.Note
import java.text.SimpleDateFormat
import java.util.*
import com.flesiy.Lotus.ui.components.markdown.AnimatedMarkdownContent
import com.flesiy.Lotus.ui.components.markdown.PreviewToggleButton

private const val TAG = "NoteEditor"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditor(
    note: Note,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onStartRecording: () -> Unit,
    onPreviewModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var content by remember { mutableStateOf(note.content) }
    var editorRef by remember { mutableStateOf<EditText?>(null) }
    val focusManager = LocalFocusManager.current
    var isPreviewMode by remember(note.id) { mutableStateOf(note.isPreviewMode) }

    // Эффект для синхронизации состояния предпросмотра
    LaunchedEffect(isPreviewMode) {
        if (isPreviewMode != note.isPreviewMode) {
            onPreviewModeChange(isPreviewMode)
        }
    }

    // Эффект для обновления состояния при смене заметки
    LaunchedEffect(note.id, note.content) {
        content = note.content
        isPreviewMode = note.isPreviewMode
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (note.title.isNotEmpty()) note.title else "Без названия",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Light
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Text(
                            text = "${content.split(Regex("\\s+")).count()} слов",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Light
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Text(
                        text = "Изменено: ${formatDate(note.modifiedAt)} • Создано: ${formatDate(note.createdAt)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Light
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onStartRecording) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Запись голоса",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        PreviewToggleButton(
                            isPreviewMode = isPreviewMode,
                            onToggle = { 
                                isPreviewMode = !isPreviewMode
                                onPreviewModeChange(isPreviewMode)
                            }
                        )
                    }

                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Сохранить"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранить")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 0.dp,
                top = paddingValues.calculateTopPadding(),
                end = 0.dp,
                bottom = paddingValues.calculateBottomPadding()
            )
        ) {
            // Редактор
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 400.dp)
                        .padding(horizontal = 16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isPreviewMode
                        ) {
                            Log.d(TAG, "Editor area clicked")
                            editorRef?.requestFocus()
                        }
                ) {
                    AnimatedMarkdownContent(
                        content = content,
                        onContentChange = { newValue ->
                            Log.d(TAG, "Content changed: $newValue")
                            content = newValue
                            onContentChange(newValue)
                        },
                        isPreviewMode = isPreviewMode,
                        modifier = Modifier.fillMaxWidth(),
                        hint = "Соберитесь с мыслями...",
                        onEditorCreated = { editor ->
                            editorRef = editor
                        }
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
} 