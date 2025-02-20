package com.flesiy.Lotus.ui.components

import android.content.Intent
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
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
import com.flesiy.Lotus.viewmodel.NotificationViewModel
import com.flesiy.Lotus.viewmodel.NoteNotification
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.flesiy.Lotus.utils.FileUtils
import com.flesiy.Lotus.viewmodel.MainViewModel
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import android.widget.Toast
import androidx.compose.material3.TextField
import kotlinx.coroutines.delay
import androidx.compose.material3.Divider
import androidx.compose.foundation.shape.RoundedCornerShape

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

@OptIn(ExperimentalMaterial3Api::class)
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
    versions: List<NoteVersion> = emptyList(),
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var editorRef by remember { mutableStateOf<EditText?>(null) }
    var isPreviewMode by remember(note.id) { mutableStateOf(note.isPreviewMode) }
    val scrollState = rememberScrollState()
    var showMediaDialog by remember { mutableStateOf(false) }
    var showTimeMarkDialog by remember { mutableStateOf(false) }
    var showNotificationsPanel by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var selectedNotification by remember { mutableStateOf<NoteNotification?>(null) }
    var showExtendedMetadata by remember { mutableStateOf(false) }
    
    val notificationViewModel: NotificationViewModel = viewModel()
    val notifications by notificationViewModel.notifications.collectAsState()
    
    // Отслеживаем изменения в контенте
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    
    // Автоматическая прокрутка к курсору при изменении текста
    LaunchedEffect(note.content) {
        // Пропускаем автоскролл если это операция с чекбоксом
        if (note.content.endsWith("- [ ] ") || note.content.contains("\n- [ ] $")) {
            return@LaunchedEffect
        }
        
        editorRef?.let { editor ->
            val layout = editor.layout
            if (layout != null) {
                val lineCount = layout.lineCount
                if (lineCount > 20) { // Проверяем, что у нас больше 20 строк
                    val currentLine = layout.getLineForOffset(editor.selectionStart)
                    // Проверяем, что курсор находится в одной из последних строк
                    if (currentLine >= lineCount - 20) {
                        val y = layout.getLineTop(currentLine)
                        scrollState.animateScrollTo(y)
                    }
                }
            }
        }
    }
    
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

    val windowInsets = WindowInsets.ime
    val imeHeight = with(LocalDensity.current) { windowInsets.getBottom(LocalDensity.current).toDp() }
    val bottomPadding = if (imeHeight > 0.dp) imeHeight else 0.dp

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { showExtendedMetadata = !showExtendedMetadata },
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${note.content.lines().size} стр.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.Light
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${
                                    note.content
                                        .replace(Regex("!\\[.*?\\]\\(.*?\\)"), "") // Удаляем изображения
                                        .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1") // Оставляем только текст ссылок
                                        .replace(Regex("[*_~`#>\\[\\]\\-\\d.]"), "") // Удаляем markdown символы
                                        .replace(Regex("\\s+"), " ") // Заменяем все пробельные символы на один пробел
                                        .trim()
                                        .split(" ")
                                        .filter { it.isNotEmpty() }
                                        .size
                                } сл.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.Light
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${note.content.length} сим.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.Light
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showExtendedMetadata,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Основные метаданные
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Изменено: ${formatDate(note.modifiedAt)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.Light
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "Создано: ${formatDate(note.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.Light
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }

                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            )

                            // Статистика заметки
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Абзацев: ${note.content.split("\n\n").size}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "Чекбоксов: ${note.content.split("\n").count { it.trim().startsWith("- [ ]") || it.trim().startsWith("- [x]") }}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Заголовков: ${note.content.split("\n").count { it.trim().startsWith("#") }}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "Ссылок: ${note.content.split("\n").sumOf { line -> 
                                            Regex("\\[.*?\\]\\(.*?\\)").findAll(line).count()
                                        }}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            )

                            // Системная информация
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "ID: ${note.id}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "Версий: ${versions.count { it.noteId == note.id }}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (note.isPinned) "Закреплено" else "Не закреплено",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "Уведомлений: ${notifications.count { it.noteId == note.id }}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

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
                shadowElevation = 3.dp,
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 28.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!isPreviewMode) {
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
                                        painter = if (isListening) 
                                            painterResource(id = R.drawable.stop_24) 
                                        else 
                                            painterResource(id = R.drawable.mic_24),
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

                        if (!isPreviewMode) {
                            IconButton(onClick = { showMediaDialog = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.attach_file_add_24px),
                                    contentDescription = "Добавить файл",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (!isPreviewMode) {
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

                        if (!isPreviewMode) {
                            IconButton(
                                onClick = {
                                    Log.d(TAG, "🔲 Нажата кнопка добавления чекбокса")
                                    editorRef?.let { editor ->
                                        try {
                                            val currentText = note.content
                                            Log.d(TAG, "📝 Текущий текст: '$currentText'")
                                            
                                            val cursorPosition = editor.selectionStart
                                            Log.d(TAG, "📍 Исходная позиция курсора: $cursorPosition")
                                            
                                            val safePosition = cursorPosition.coerceIn(0, currentText.length)
                                            Log.d(TAG, "✔️ Безопасная позиция курсора: $safePosition")
                                            
                                            val checkboxText = "- [ ] "
                                            
                                            // Определяем, нужно ли добавить перенос строки перед чекбоксом
                                            val needsNewLine = safePosition > 0 && 
                                                             !currentText.substring(0, safePosition).endsWith("\n")
                                            Log.d(TAG, "↩️ Нужен перенос строки: $needsNewLine")
                                            
                                            val newText = buildString {
                                                append(currentText.substring(0, safePosition))
                                                if (needsNewLine) {
                                                    append("\n")
                                                    Log.d(TAG, "➕ Добавлен перенос строки")
                                                }
                                                append(checkboxText)
                                                append(currentText.substring(safePosition))
                                            }
                                            
                                            Log.d(TAG, "📄 Новый текст: '$newText'")
                                            
                                            // Вычисляем новую позицию курсора
                                            val newCursorPosition = safePosition + checkboxText.length + (if (needsNewLine) 1 else 0)
                                            Log.d(TAG, "📍 Новая позиция курсора: $newCursorPosition")
                                            
                                            // Устанавливаем текст напрямую в EditText
                                            editor.setText(newText)
                                            
                                            // Сразу устанавливаем позицию курсора
                                            val finalPosition = newCursorPosition.coerceIn(0, editor.length())
                                            editor.setSelection(finalPosition)
                                            
                                            // Только после этого уведомляем об изменении контента
                                            onContentChange(newText)
                                            
                                            Log.d(TAG, "✅ Курсор успешно установлен в позицию $finalPosition")
                                        } catch (e: Exception) {
                                            Log.e(TAG, "❌ Ошибка при добавлении чекбокса: ${e.message}")
                                        }
                                    } ?: run {
                                        Log.e(TAG, "❌ editorRef is null")
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.check_box_24px),
                                    contentDescription = "Добавить чекбокс",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (isPreviewMode) {
                            IconButton(
                                onClick = { showNotificationsPanel = true }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.bell_24),
                                    contentDescription = "Уведомления",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (isPreviewMode) {
                            val context = LocalContext.current
                            IconButton(
                                onClick = {
                                    // Функция для очистки текста от markdown-разметки
                                    val cleanText = note.content
                                        .replace(Regex("!\\[.*?\\]\\(.*?\\)"), "") // Удаляем изображения
                                        .replace(Regex("\\[(.*?)\\]\\(.*?\\)")) { matchResult -> 
                                            matchResult.groupValues[1] // Оставляем только текст ссылки
                                        } // Заменяем ссылки на текст
                                        .replace(Regex("[*_~`\\[\\]]+"), "") // Удаляем символы форматирования и квадратные скобки
                                        .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "") // Удаляем заголовки
                                        .replace(Regex("^[*+]\\s+", RegexOption.MULTILINE), "") // Удаляем маркеры списков, кроме дефиса
                                        .replace(Regex("^\\d+\\.\\s+", RegexOption.MULTILINE), "") // Удаляем нумерацию списков
                                        .replace(Regex("^>\\s*", RegexOption.MULTILINE), "") // Удаляем цитаты
                                        .replace(Regex("\\n{3,}"), "\n\n") // Заменяем множественные переносы строк на двойной
                                        .trim()

                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Текст заметки", cleanText)
                                    clipboard.setPrimaryClip(clip)
                                    
                                    Toast.makeText(context, "Текст скопирован", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.content_copy_24px),
                                    contentDescription = "Копировать текст без разметки",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (isPreviewMode) {
                            val context = LocalContext.current
                            IconButton(
                                onClick = {
                                    val noteId = note.id
                                    val shareFile = viewModel.prepareNoteForSharing(noteId)
                                    if (shareFile != null) {
                                        val intent = Intent(Intent.ACTION_SEND)
                                        intent.type = "text/markdown"
                                        intent.putExtra(
                                            Intent.EXTRA_STREAM,
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                shareFile
                                            )
                                        )
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        context.startActivity(Intent.createChooser(intent, "Отправить заметку"))
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.share_24px),
                                    contentDescription = "Поделиться",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = handleSave,
                        enabled = hasUnsavedChanges,
                        modifier = Modifier.height(36.dp),
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
                        Text(
                            text = if (hasUnsavedChanges) "Сохранить" else "Сохранить",
                            color = if (hasUnsavedChanges)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = bottomPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 4.dp)
                    .verticalScroll(scrollState)
                    .drawScrollbar(
                        state = scrollState,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isPreviewMode
                    ) {
                        Log.d(TAG, "Editor area clicked")
                        editorRef?.let { editor ->
                            editor.requestFocus()
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 400.dp)

                        .padding(bottom = 10.dp)
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
                    // Добавляем перенос строки, если текст не начинается с него
                    if (!note.content.endsWith("\n")) {
                        append("\n")
                    }
                    // Добавляем символ метки и отступ
                    append("⌚ ")
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
                    // Добавляем перенос строки в конце
                    append("\n")
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

    if (showNotificationsPanel) {
        AlertDialog(
            onDismissRequest = { showNotificationsPanel = false },
            modifier = Modifier.fillMaxWidth(0.9f),
            content = {
                NotificationsPanel(
                    notifications = notifications.filter { it.noteId == note.id },
                    onEditNotification = { notification ->
                        selectedNotification = notification
                        showNotificationDialog = true
                    },
                    onDeleteNotification = { notification ->
                        notificationViewModel.deleteNotification(notification)
                    },
                    onToggleNotification = { notification ->
                        notificationViewModel.toggleNotification(notification)
                    },
                    onAddNotification = {
                        selectedNotification = null
                        showNotificationDialog = true
                    }
                )
            }
        )
    }

    if (showNotificationDialog) {
        NotificationDialog(
            initialNotification = selectedNotification,
            onDismiss = { showNotificationDialog = false },
            onConfirm = { notification ->
                notificationViewModel.addNotification(notification)
            },
            noteId = note.id
        )
    }

    // Автоматическое скрытие расширенных метаданных через 2 секунды
    LaunchedEffect(showExtendedMetadata) {
        if (showExtendedMetadata) {
            delay(10000)
            showExtendedMetadata = false
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
} 