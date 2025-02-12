package com.flesiy.Lotus.ui.components

import android.net.Uri
import android.util.Log
import android.widget.EditText
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.flesiy.Lotus.ui.components.markdown.AnimatedMarkdownContent
import com.flesiy.Lotus.ui.components.markdown.PreviewToggleButton
import com.flesiy.Lotus.viewmodel.Note
import com.flesiy.Lotus.viewmodel.NoteVersion
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.compose.foundation.layout.size

private const val TAG = "NoteEditor"

@Composable
private fun Modifier.drawScrollbar(
    state: ScrollState,
    color: Color
): Modifier = drawWithContent {
    drawContent()
    
    val scrollMaxValue = state.maxValue.toFloat()
    if (scrollMaxValue > 0) {
        val scrollValue = state.value.toFloat()
        val scrollPercent = scrollValue / scrollMaxValue
        
        val height = size.height
        val thumbHeight = 32.dp.toPx() // Фиксированная высота ползунка
        val maxOffset = height - thumbHeight
        val thumbOffset = (scrollPercent * maxOffset).coerceIn(0f, maxOffset)
        
        // Рисуем только ползунок
        drawRect(
            color = color.copy(alpha = 0.5f),
            topLeft = Offset(size.width - 4.dp.toPx(), thumbOffset),
            size = Size(4.dp.toPx(), thumbHeight)
        )
    }
}

@Composable
fun NoteEditor(
    note: Note,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onStartRecording: () -> Unit,
    onPreviewModeChange: (Boolean) -> Unit,
    isListening: Boolean = false,
    versions: List<NoteVersion> = emptyList(),
    selectedVersion: NoteVersion? = null,
    isVersionHistoryVisible: Boolean = false,
    onToggleVersionHistory: () -> Unit = {},
    onVersionSelected: (NoteVersion?) -> Unit = {},
    onApplyVersion: () -> Unit = {},
    onDeleteVersion: (NoteVersion) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf(note.content) }
    var editorRef by remember { mutableStateOf<EditText?>(null) }
    var isPreviewMode by remember(note.id) { mutableStateOf(note.content.isNotEmpty()) }
    val scrollState = rememberScrollState()
    var showMediaDialog by remember { mutableStateOf(false) }
    
    // Отслеживаем изменения в контенте
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    
    // Сброс флага при смене заметки
    LaunchedEffect(note.id) {
        content = note.content
        isPreviewMode = note.isPreviewMode
        hasUnsavedChanges = false
    }

    // Обновляем контент при изменении версии
    LaunchedEffect(selectedVersion, isPreviewMode, isVersionHistoryVisible) {
        if (isPreviewMode && selectedVersion != null && isVersionHistoryVisible) {
            content = selectedVersion.content
            // Не сбрасываем hasUnsavedChanges, так как это временный просмотр
        } else if (!isVersionHistoryVisible) {
            content = note.content
            // Не сбрасываем hasUnsavedChanges, возвращаемся к текущему состоянию
        }
    }

    // Функция для сохранения с обновлением состояния
    val handleSave = {
        onSave()
        hasUnsavedChanges = false
    }

    // Эффект для синхронизации состояния предпросмотра
    LaunchedEffect(isPreviewMode) {
        if (isPreviewMode != note.isPreviewMode) {
            onPreviewModeChange(isPreviewMode)
        }
    }

    // Запускаем выбор изображения
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Создаем копию файла в нашем приложении
            val imageFile = File(context.cacheDir, "images/${UUID.randomUUID()}.jpg")
            imageFile.parentFile?.mkdirs()
            
            context.contentResolver.openInputStream(selectedUri)?.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Создаем URI через FileProvider
            val imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                imageFile
            )

            // Вставляем markdown-ссылку на изображение в текст
            val imageMarkdown = "![](${imageUri})"
            val currentText = content
            val cursorPosition = editorRef?.selectionStart ?: currentText.length
            val newText = currentText.substring(0, cursorPosition) + 
                         imageMarkdown + 
                         currentText.substring(cursorPosition)
            
            content = newText
            onContentChange(newText)
        }
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
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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

                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${content.split(Regex("\\s+")).count()} слов",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.Light
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Text(
                        text = "Изменено: ${formatDate(note.modifiedAt)} • Создано: ${formatDate(note.createdAt)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Light
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    AnimatedVisibility(
                        visible = isVersionHistoryVisible && isPreviewMode,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        VersionHistoryPanel(
                            versions = versions,
                            selectedVersion = selectedVersion,
                            onVersionSelected = onVersionSelected,
                            onApplyVersion = onApplyVersion,
                            onDeleteVersion = onDeleteVersion
                        )
                    }
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
                        AnimatedVisibility(
                            visible = !isPreviewMode,
                            enter = fadeIn() + slideInHorizontally(),
                            exit = fadeOut() + slideOutHorizontally()
                        ) {
                            IconButton(
                                onClick = onStartRecording,
                                enabled = !isPreviewMode
                            ) {
                                val iconTint = when {
                                    !isPreviewMode && isListening -> MaterialTheme.colorScheme.error
                                    !isPreviewMode -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                                
                                val scale = animateFloatAsState(
                                    targetValue = if (isListening) 1.2f else 1f,
                                    label = "Recording scale animation"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = scale.value
                                            scaleY = scale.value
                                        }
                                        .background(
                                            color = if (isListening) 
                                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f) 
                                            else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = if (isListening) "Остановить запись" else "Начать запись",
                                        tint = iconTint
                                    )
                                }
                            }
                        }
                        
                        PreviewToggleButton(
                            isPreviewMode = isPreviewMode,
                            onToggle = { 
                                isPreviewMode = !isPreviewMode
                                onPreviewModeChange(isPreviewMode)
                            }
                        )

                        IconButton(onClick = { showMediaDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = "Добавить файл",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.rotate(45f)
                            )
                        }
                    }

                    Button(
                        onClick = handleSave,
                        enabled = hasUnsavedChanges,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasUnsavedChanges) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (hasUnsavedChanges)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Сохранить",
                            tint = if (hasUnsavedChanges)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasUnsavedChanges) "Сохранить*" else "Сохранить",
                            color = if (hasUnsavedChanges)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(end = 4.dp)
                .verticalScroll(scrollState)
                .drawScrollbar(
                    state = scrollState,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                    onContentChange = { newContent ->
                        if (newContent != note.content) {
                            hasUnsavedChanges = true
                        }
                        content = newContent
                        onContentChange(newContent)
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

    if (showMediaDialog) {
        AlertDialog(
            onDismissRequest = { showMediaDialog = false },
            title = { Text("Добавить") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Фотография") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable {
                            showMediaDialog = false
                            imagePickerLauncher.launch("image/*")
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMediaDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
} 