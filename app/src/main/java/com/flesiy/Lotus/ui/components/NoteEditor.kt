package com.flesiy.Lotus.ui.components

import android.net.Uri
import android.util.Log
import android.widget.EditText
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.flesiy.Lotus.R
import com.flesiy.Lotus.ui.components.markdown.AnimatedMarkdownContent
import com.flesiy.Lotus.ui.components.markdown.PreviewToggleButton
import com.flesiy.Lotus.viewmodel.Note
import com.flesiy.Lotus.viewmodel.NoteVersion
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeMarkDialog(
    onDismiss: () -> Unit,
    onConfirm: (timestamp: LocalDateTime, description: String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val currentDateTime = remember { LocalDateTime.now() }
    var selectedDateTime by remember { mutableStateOf(currentDateTime) }
    
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить временную метку") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Выбор даты и времени метки
                Column {
                    Text(
                        text = "Дата и время метки",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true }
                        ) {
                            Text(selectedDateTime.format(dateFormatter))
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true }
                        ) {
                            Text(selectedDateTime.format(timeFormatter))
                        }
                    }
                }

                // Описание
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedDateTime, description)
                    onDismiss()
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )

    // Диалоги выбора даты и времени
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(millis),
                                ZoneId.systemDefault()
                            )
                            selectedDateTime = selectedDateTime
                                .withYear(newDate.year)
                                .withMonth(newDate.monthValue)
                                .withDayOfMonth(newDate.dayOfMonth)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedDateTime.hour,
            initialMinute = selectedDateTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Выберите время") },
            text = {
                TimePicker(
                    state = timePickerState,
                    layoutType = TimePickerLayoutType.Vertical
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateTime = selectedDateTime
                            .withHour(timePickerState.hour)
                            .withMinute(timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun NoteEditor(
    note: Note,
    onContentChange: (String) -> Unit,
    onPreviewModeChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onStartRecording: () -> Unit,
    isListening: Boolean = false,
    selectedVersion: NoteVersion? = null,
    isVersionHistoryVisible: Boolean = false,
    onToggleVersionHistory: () -> Unit = {},
    onVersionSelected: (NoteVersion?) -> Unit = {},
    onApplyVersion: () -> Unit = {},
    onDeleteVersion: (NoteVersion) -> Unit = {},
    modifier: Modifier = Modifier,
    fontSize: Float = 16f,
    versions: List<NoteVersion> = emptyList()
) {
    val context = LocalContext.current
    var editorRef by remember { mutableStateOf<EditText?>(null) }
    var isPreviewMode by remember(note.id) { mutableStateOf(note.isPreviewMode) }
    val scrollState = rememberScrollState()
    var showMediaDialog by remember { mutableStateOf(false) }
    var showTimeMarkDialog by remember { mutableStateOf(false) }
    
    // Отслеживаем изменения в контенте
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    
    // Сброс флага при смене заметки
    LaunchedEffect(note.id) {
        isPreviewMode = note.isPreviewMode
        hasUnsavedChanges = false
    }

    // Обновляем контент при изменении версии
    LaunchedEffect(selectedVersion, isPreviewMode, isVersionHistoryVisible, note.id) {
        if (isPreviewMode && selectedVersion != null && isVersionHistoryVisible) {
            // Проверяем, что версия принадлежит текущей заметке
            if (selectedVersion.noteId == note.id) {
                onContentChange(selectedVersion.content)
            } else {
                // Если версия от другой заметки, сбрасываем выбор
                onVersionSelected(null)
            }
        } else if (!isVersionHistoryVisible) {
            onContentChange(note.content)
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
            val currentText = note.content
            val cursorPosition = editorRef?.selectionStart ?: currentText.length
            val newText = currentText.substring(0, cursorPosition) + 
                         imageMarkdown + 
                         currentText.substring(cursorPosition)
            
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
                                text = "${note.content.split(Regex("\\s+")).count()} слов",
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
                            onDeleteVersion = onDeleteVersion,
                            noteId = note.id
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
                            enter = fadeIn(),
                            exit = fadeOut()
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

                        AnimatedVisibility(
                            visible = !isPreviewMode,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(onClick = { showMediaDialog = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.attach_file_add_24px),
                                    contentDescription = "Добавить файл",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = !isPreviewMode,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = { showTimeMarkDialog = true }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.calendar_add_on_24px),
                                    contentDescription = "Добавить временную метку",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
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
                    content = note.content,
                    onContentChange = { newContent ->
                        if (newContent != note.content) {
                            hasUnsavedChanges = true
                            onContentChange(newContent)
                        }
                    },
                    isPreviewMode = isPreviewMode,
                    modifier = Modifier.fillMaxWidth(),
                    hint = "Соберитесь с мыслями...",
                    onEditorCreated = { editor ->
                        editorRef = editor
                    },
                    fontSize = fontSize
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

    if (showTimeMarkDialog) {
        TimeMarkDialog(
            onDismiss = { showTimeMarkDialog = false },
            onConfirm = { timestamp, description ->
                val timeMarkText = buildString {
                    // Форматируем дату
                    append("*")
                    append(timestamp.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                    append("*")
                    
                    // Добавляем описание, если оно есть
                    if (description.isNotEmpty()) {
                        append("   ***")
                        append(description)
                        append("***")
                    }
                }
                
                val currentText = note.content
                val cursorPosition = editorRef?.selectionStart ?: currentText.length
                val newText = currentText.substring(0, cursorPosition) + 
                             timeMarkText + 
                             currentText.substring(cursorPosition)
                
                onContentChange(newText)
            }
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
} 