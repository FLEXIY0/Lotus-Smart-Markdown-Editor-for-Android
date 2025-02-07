package com.flesiy.Lotus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flesiy.Lotus.ui.components.NoteEditor
import com.flesiy.Lotus.ui.components.NotesList
import com.flesiy.Lotus.ui.components.TrashScreen
import com.flesiy.Lotus.ui.components.FileManagementScreen
import com.flesiy.Lotus.ui.theme.LotusTheme
import com.flesiy.Lotus.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LotusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LotusApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotusApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val notes by viewModel.notes.collectAsState()
    val currentNote by viewModel.currentNote.collectAsState()
    val trashNotes by viewModel.trashNotes.collectAsState()
    val trashSize by viewModel.trashSize.collectAsState()
    val isTrashOverLimit by viewModel.isTrashOverLimit.collectAsState()
    val currentRetentionPeriod by viewModel.currentRetentionPeriod.collectAsState()
    
    // Добавляем ключ для перезапуска списка
    var notesListKey by remember { mutableStateOf(0) }
    
    // Отслеживаем состояние drawer'а
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Closed) {
            notesListKey++ // Увеличиваем ключ при закрытии меню
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Список заметок в скроллируемом контейнере
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Добавляем ключ к NotesList
                        key(notesListKey) {
                            NotesList(
                                notes = notes,
                                onNoteClick = { noteId ->
                                    viewModel.loadNote(noteId)
                                    viewModel.updateLastViewedNoteFile()
                                    scope.launch {
                                        drawerState.close()
                                    }
                                    navController.navigate("editor")
                                },
                                onNoteDelete = { noteId ->
                                    viewModel.deleteNote(noteId)
                                },
                                onNotePinned = { noteId ->
                                    viewModel.toggleNotePinned(noteId)
                                },
                                onNoteMove = { fromIndex, toIndex ->
                                    viewModel.moveNote(fromIndex, toIndex)
                                },
                                skipDeleteConfirmation = viewModel.skipDeleteConfirmation.collectAsState().value,
                                onSkipDeleteConfirmationChange = { skip ->
                                    viewModel.setSkipDeleteConfirmation(skip)
                                }
                            )
                        }
                    }
                    
                    // Фиксированная нижняя часть
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Divider()
                        
                        // Пункт импорта/экспорта
                        ListItem(
                            headlineContent = { Text("Загрузка и отправка") },
                            supportingContent = {
                                Column {
                                    Text(
                                        text = "Экспорт, импорт и отправка заметок",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val context = LocalContext.current
                                        val exportDirectory by viewModel.exportDirectory.collectAsState(initial = null)
                                        val lastViewedFile by viewModel.lastViewedNoteFile.collectAsState(initial = null)

                                        AssistChip(
                                            onClick = { 
                                                exportDirectory?.let { dir ->
                                                    val intent = Intent(Intent.ACTION_VIEW)
                                                    intent.setDataAndType(
                                                        FileProvider.getUriForFile(
                                                            context,
                                                            "${context.packageName}.provider",
                                                            dir
                                                        ),
                                                        "*/*"
                                                    )
                                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    context.startActivity(intent)
                                                }
                                            },
                                            label = { Text("Хранилище") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.FolderOpen,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            enabled = exportDirectory != null
                                        )
                                        AssistChip(
                                            onClick = { 
                                                lastViewedFile?.let { file ->
                                                    val intent = Intent(Intent.ACTION_SEND)
                                                    intent.type = "text/plain"
                                                    intent.putExtra(
                                                        Intent.EXTRA_STREAM,
                                                        FileProvider.getUriForFile(
                                                            context,
                                                            "${context.packageName}.provider",
                                                            file
                                                        )
                                                    )
                                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    context.startActivity(Intent.createChooser(intent, "Отправить заметку"))
                                                }
                                            },
                                            label = { Text("Отправка") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Share,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            enabled = lastViewedFile != null
                                        )
                                    }
                                }
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = "Загрузка и отправка",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                scope.launch {
                                    drawerState.close()
                                }
                                navController.navigate("file_management")
                            }
                        )
                        
                        Divider()
                        
                        // Кнопка корзины
                        ListItem(
                            headlineContent = { Text("Корзина") },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Корзина",
                                    tint = if (isTrashOverLimit) MaterialTheme.colorScheme.error 
                                          else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingContent = if (trashSize > 0) {
                                {
                                    Text(
                                        text = when {
                                            trashSize < 1024 -> "$trashSize B"
                                            trashSize < 1024 * 1024 -> String.format("%.1f KB", trashSize / 1024.0)
                                            else -> String.format("%.1f MB", trashSize / (1024.0 * 1024.0))
                                        },
                                        color = if (isTrashOverLimit) MaterialTheme.colorScheme.error 
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else null,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    drawerState.close()
                                }
                                navController.navigate("trash")
                            }
                        )
                        
                        Divider()
                        
                        // Информация о ресурсах
                        ListItem(
                            headlineContent = { 
                                Text(
                                    "Использование памяти",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            supportingContent = {
                                Column {
                                    val notesSize = viewModel.totalNotesSize.collectAsState().value
                                    val memoryUsage = viewModel.appMemoryUsage.collectAsState().value
                                    Text(
                                        text = "Заметки: " + when {
                                            notesSize < 1024 -> "$notesSize B"
                                            notesSize < 1024 * 1024 -> String.format("%.1f KB", notesSize / 1024.0)
                                            else -> String.format("%.1f MB", notesSize / (1024.0 * 1024.0))
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "ОЗУ: " + when {
                                            memoryUsage < 1024 -> "$memoryUsage B"
                                            memoryUsage < 1024 * 1024 -> String.format("%.1f KB", memoryUsage / 1024.0)
                                            else -> String.format("%.1f MB", memoryUsage / (1024.0 * 1024.0))
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = "Использование памяти"
                                )
                            }
                        )
                    }
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "editor",
            modifier = Modifier
        ) {
            composable(
                route = "editor"
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Lotus") },
                            navigationIcon = {
                                IconButton(onClick = {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Меню")
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    viewModel.createNewNote()
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Новая заметка")
                                }
                            }
                        )
                    }
                ) { padding ->
                    NoteEditor(
                        note = currentNote,
                        onContentChange = { content ->
                            viewModel.updateNoteContent(content)
                            viewModel.saveNote()
                        },
                        onSave = {
                            viewModel.saveNote()
                        },
                        onStartRecording = {
                            // TODO: Implement speech recognition
                        },
                        onPreviewModeChange = { isPreview ->
                            viewModel.updatePreviewMode(isPreview)
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            
            composable(
                route = "trash"
            ) {
                TrashScreen(
                    notes = trashNotes,
                    currentRetentionPeriod = currentRetentionPeriod,
                    trashSize = trashSize,
                    isOverLimit = isTrashOverLimit,
                    onRetentionPeriodChange = { period ->
                        viewModel.setRetentionPeriod(period)
                    },
                    onRestoreNote = { noteId ->
                        viewModel.restoreNote(noteId)
                    },
                    onDeleteNote = { noteId ->
                        viewModel.deleteNoteFromTrash(noteId)
                    },
                    onClearTrash = {
                        viewModel.clearTrash()
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "file_management"
            ) {
                FileManagementScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    modifier = Modifier
                )
            }
        }
    }
}