package com.flesiy.Lotus

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flesiy.Lotus.ui.components.DeveloperRoom
import com.flesiy.Lotus.ui.components.FileManagementScreen
import com.flesiy.Lotus.ui.components.NoteEditor
import com.flesiy.Lotus.ui.components.NotesList
import com.flesiy.Lotus.ui.components.TrashScreen
import com.flesiy.Lotus.ui.theme.LotusTheme
import com.flesiy.Lotus.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var viewModelInstance: MainViewModel? = null
    private var backPressedTime = 0L
    private val doubleBackPressedInterval = 2000L
    private var currentNavController: NavController? = null
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        onBackPressedDispatcher.addCallback(this) {
            if (isNavigating) return@addCallback
            
            isNavigating = true
            val navController = currentNavController
            when {
                navController == null -> {
                    handleDefaultBack()
                }
                navController.currentBackStackEntry?.destination?.route != "editor" -> {
                    navController.popBackStack(
                        route = "editor",
                        inclusive = false
                    )
                }
                       else -> {
                    handleDefaultBack()
                }
            }
            isNavigating = false
        }

        setContent {
            LotusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel()
                    viewModelInstance = viewModel
                    viewModel.setActivity(this)
                    LotusApp(
                        viewModel = viewModel,
                        onNavControllerCreated = { navController ->
                            currentNavController = navController
                        }
                    )
                }
            }
        }
    }

    private fun handleDefaultBack() {
        if (backPressedTime + doubleBackPressedInterval > System.currentTimeMillis()) {
            finish()
        } else {
            Toast.makeText(this, "Нажмите ещё раз для выхода", Toast.LENGTH_SHORT).show()
            backPressedTime = System.currentTimeMillis()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModelInstance?.handleSpeechResult(requestCode, resultCode, data)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotusApp(
    viewModel: MainViewModel,
    onNavControllerCreated: (NavController) -> Unit
) {
    val navController = rememberNavController()
    
    // Функция для безопасной навигации
    fun navigateSafely(route: String) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute != route) {
            navController.navigate(route) {
                // Очищаем весь стек до стартовой точки
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
                // Предотвращаем создание дубликатов
                launchSingleTop = true
            }
        }
    }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val notes by viewModel.notes.collectAsState()
    val currentNote by viewModel.currentNote.collectAsState()
    val trashNotes by viewModel.trashNotes.collectAsState()
    val trashSize by viewModel.trashSize.collectAsState()
    val isTrashOverLimit by viewModel.isTrashOverLimit.collectAsState()
    val currentRetentionPeriod by viewModel.currentRetentionPeriod.collectAsState()
    
    // Сохраняем ссылку на NavController
    LaunchedEffect(navController) {
        onNavControllerCreated(navController)
    }
    
    // Добавляем ключ для перезапуска списка
    var notesListKey by remember { mutableStateOf(0) }
    
    // Отслеживаем состояние drawer'а
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Closed) {
            notesListKey++
        }
    }
    
    // Обработчик кнопки "назад" для Compose
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
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
                            // Очищаем бэкстек и переходим сразу к редактору
                            navController.navigate("editor") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
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
                        if (viewModel.isFileManagementEnabled.collectAsState().value) {
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
                                    navigateSafely("file_management")
                                }
                            )
                        }
                        
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
                                navigateSafely("trash")
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
                                    Icons.Default.Memory,
                                    contentDescription = "Использование памяти",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.clickable {
                                scope.launch {
                                    drawerState.close()
                                }
                                navigateSafely("developer_room")
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
                route = "editor",
                popEnterTransition = { fadeIn() },
                popExitTransition = { fadeOut() }
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
                        },
                        onSave = {
                            viewModel.saveNote()
                        },
                        onStartRecording = {
                            if (viewModel.isListening.value) {
                                viewModel.stopSpeechRecognition()
                            } else {
                                viewModel.startSpeechRecognition()
                            }
                        },
                        onPreviewModeChange = { isPreview ->
                            viewModel.updatePreviewMode(isPreview)
                        },
                        isListening = viewModel.isListening.collectAsState().value,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            
            composable(
                route = "trash",
                enterTransition = { slideInHorizontally { it } },
                exitTransition = { slideOutHorizontally { -it } },
                popEnterTransition = { slideInHorizontally { -it } },
                popExitTransition = { slideOutHorizontally { it } }
            ) {
                TrashScreen(
                    notes = trashNotes,
                    currentRetentionPeriod = currentRetentionPeriod,
                    trashSize = trashSize,
                    isOverLimit = isTrashOverLimit,
                    cacheStats = viewModel.cacheStats.collectAsState().value,
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
                    onClearCache = {
                        viewModel.clearCache()
                    },
                    onClearImagesCache = {
                        viewModel.clearImagesCache()
                    },
                    onClearFilesCache = {
                        viewModel.clearFilesCache()
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "file_management",
                enterTransition = { slideInHorizontally { it } },
                exitTransition = { slideOutHorizontally { -it } },
                popEnterTransition = { slideInHorizontally { -it } },
                popExitTransition = { slideOutHorizontally { it } }
            ) {
                FileManagementScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    modifier = Modifier
                )
            }

            composable(
                route = "developer_room",
                enterTransition = { slideInHorizontally { it } },
                exitTransition = { slideOutHorizontally { -it } },
                popEnterTransition = { slideInHorizontally { -it } },
                popExitTransition = { slideOutHorizontally { it } }
            ) {
                DeveloperRoom(
                    onBack = {
                        navController.popBackStack()
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}