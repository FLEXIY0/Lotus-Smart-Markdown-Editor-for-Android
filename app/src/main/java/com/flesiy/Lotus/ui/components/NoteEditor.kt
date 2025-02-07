package com.flesiy.Lotus.ui.components

import android.util.Log
import android.widget.EditText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.foundation.ScrollState
import com.flesiy.Lotus.ui.components.markdown.MarkdownPreviewScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.ui.draw.rotate
import androidx.core.content.FileProvider
import java.io.File
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
fun NoteEditor(
    note: Note,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onStartRecording: () -> Unit,
    onPreviewModeChange: (Boolean) -> Unit,
    onMediaManage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf(note.content) }
    var editorRef by remember { mutableStateOf<EditText?>(null) }
    val focusManager = LocalFocusManager.current
    var isPreviewMode by remember(note.id) { mutableStateOf(note.isPreviewMode) }
    val scrollState = rememberScrollState()
    var showMediaDialog by remember { mutableStateOf(false) }

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
                        modifier = Modifier.fillMaxWidth(),
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