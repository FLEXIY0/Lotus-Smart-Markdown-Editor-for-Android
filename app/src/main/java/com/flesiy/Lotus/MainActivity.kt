package com.flesiy.Lotus

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalView
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
import com.flesiy.Lotus.ui.components.FontSizeDialog
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
        
        // Запрашиваем разрешения для уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // Проверяем разрешение на точные уведомления
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent().also { intent ->
                    intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    startActivity(intent)
                }
            }
        }
        
        onBackPressedDispatcher.addCallback(this) {
            if (isNavigating) return@addCallback
            
            isNavigating = true
            val navController = currentNavController
            when {
                navController == null -> {
                    handleDefaultBack()
                }
                navController.previousBackStackEntry != null -> {
                    // Если есть предыдущий экран в стеке, возвращаемся к нему
                    navController.popBackStack()
                }
                else -> {
                    // Если мы на главном экране, показываем сообщение о двойном нажатии
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
    var showFontSizeDialog by remember { mutableStateOf(false) }
    val fontSize by viewModel.fontSize.collectAsState()
    
    // Функция для безопасной навигации с анимацией
    fun navigateSafely(route: String) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute != route) {
            navController.navigate(route) {
                // Сохраняем стек навигации для корректной работы кнопки назад
                popUpTo("editor") {
                    saveState = true
                }
                // Предотвращаем создание дубликатов
                launchSingleTop = true
                // Восстанавливаем состояние при возврате
                restoreState = true
            }
        }
    }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
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
        } else if (drawerState.currentValue == DrawerValue.Open) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            view.findFocus()?.let { focusedView ->
                imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
            }
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
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
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
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
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
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
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
            modifier = Modifier.fillMaxSize()
        ) {
            composable(
                route = "editor",
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Lotus") },
                            navigationIcon = {
                                IconButton(onClick = {
                                    scope.launch {
                                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                        view.findFocus()?.let { focusedView ->
                                            imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
                                        }
                                        drawerState.open()
                                    }
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Меню")
                                }
                            },
                            actions = {
                                IconButton(onClick = { showFontSizeDialog = true }) {
                                    Icon(Icons.Default.FormatSize, contentDescription = "Размер шрифта")
                                }
                                if (currentNote.isPreviewMode) {
                                    IconButton(
                                        onClick = { viewModel.toggleVersionHistory() },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = "История версий",
                                            tint = if (viewModel.isVersionHistoryVisible.collectAsState().value)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val currentNoteId = currentNote.id
                                        viewModel.deleteNote(currentNoteId)
                                        viewModel.createNewNote()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Удалить заметку",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.createNewNote()
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Новая заметка")
                                }
                            }
                        )
                    }
                ) { padding ->
                    val currentNote by viewModel.currentNote.collectAsState()
                    val selectedVersion by viewModel.selectedVersion.collectAsState()
                    val isVersionHistoryVisible by viewModel.isVersionHistoryVisible.collectAsState()
                    val fontSize by viewModel.fontSize.collectAsState()
                    val isListening by viewModel.isListening.collectAsState()
                    val versions by viewModel.noteVersions.collectAsState()

                    NoteEditor(
                        note = currentNote,
                        onContentChange = { content ->
                            viewModel.updateNoteContent(content)
                        },
                        onPreviewModeChange = { isPreviewMode ->
                            viewModel.updateNotePreviewMode(isPreviewMode)
                        },
                        onSave = {
                            viewModel.saveCurrentNote()
                        },
                        onStartRecording = {
                            if (isListening) {
                                viewModel.stopSpeechRecognition()
                            } else {
                                viewModel.startSpeechRecognition()
                            }
                        },
                        isListening = isListening,
                        selectedVersion = selectedVersion,
                        onVersionSelected = { version ->
                            viewModel.selectVersion(version)
                        },
                        isVersionHistoryVisible = isVersionHistoryVisible,
                        onToggleVersionHistory = {
                            viewModel.toggleVersionHistory()
                        },
                        onApplyVersion = {
                            viewModel.applySelectedVersion()
                        },
                        onDeleteVersion = { version ->
                            viewModel.deleteVersion(version)
                        },
                        modifier = Modifier.padding(padding),
                        fontSize = fontSize,
                        versions = versions
                    )
                }
            }
            
            composable(
                route = "trash",
                enterTransition = { 
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = { 
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = { 
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = { 
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            ) {
                BackHandler {
                    navController.popBackStack()
                }
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
                enterTransition = { 
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = { 
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = { 
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = { 
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            ) {
                BackHandler {
                    navController.popBackStack()
                }
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
                enterTransition = { 
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = { 
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = { 
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = { 
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            ) {
                BackHandler {
                    navController.popBackStack()
                }
                DeveloperRoom(
                    onBack = {
                        navController.popBackStack()
                    },
                    viewModel = viewModel
                )
            }
        }
    }

    if (showFontSizeDialog) {
        FontSizeDialog(
            currentSize = fontSize,
            onSizeChange = { viewModel.setFontSize(it) },
            onDismiss = { showFontSizeDialog = false }
        )
    }
}