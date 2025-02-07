package com.flesiy.Lotus.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.core.content.FileProvider
import com.flesiy.Lotus.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagementScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exportDirectory by viewModel.exportDirectory.collectAsState(initial = null as File?)
    val lastViewedFile by viewModel.lastViewedNoteFile.collectAsState(initial = null as File?)
    var hasStoragePermission by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDirectoryPicker by remember { mutableStateOf(false) }

    // Запрос разрешений для Android < 13
    val requestOldStoragePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasStoragePermission = permissions.values.all { it }
    }

    // Запрос разрешений для Android 13+
    val requestNewStoragePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
    }

    // Выбор директории для экспорта
    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val directory = File(uri.path!!)
            viewModel.onExportDirectorySelect(directory)
        }
    }

    // Выбор файла для импорта
    val pickFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // TODO: Реализовать импорт файла
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Загрузка и отправка") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Секция разрешений
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                requestNewStoragePermission.launch(
                                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                                )
                            } else {
                                requestOldStoragePermission.launch(
                                    arrayOf(
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    )
                                )
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Разрешения",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PermissionItem(
                            title = "Хранилище",
                            description = "Доступ к внешнему хранилищу для импорта/экспорта",
                            isGranted = hasStoragePermission
                        )
                    }
                }
            }

            // Секция действий
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Действия",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Экспорт
                        ListItem(
                            headlineContent = { Text("Экспортировать все заметки") },
                            supportingContent = { 
                                Text("Сохранить копию всех заметок во внешнее хранилище")
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Upload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable(enabled = hasStoragePermission) {
                                showExportDialog = true
                            }
                        )
                        
                        // Импорт
                        ListItem(
                            headlineContent = { Text("Импортировать заметки") },
                            supportingContent = { 
                                Text("Загрузить заметки из внешнего хранилища")
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable(enabled = hasStoragePermission) {
                                showImportDialog = true
                            }
                        )
                    }
                }
            }

            // Секция файлов для отправки
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Файлы для отправки",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = {
                                val currentLastViewedFile = lastViewedFile
                                currentLastViewedFile?.let { file ->
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        type = "text/markdown"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Отправить заметку"))
                                }
                            }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Отправить",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val currentLastViewedFile = lastViewedFile
                        if (currentLastViewedFile != null) {
                            ListItem(
                                headlineContent = { Text(currentLastViewedFile.name) },
                                supportingContent = { Text("Последняя просмотренная заметка") },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        } else {
                            Text(
                                text = "Нет файлов для отправки",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Секция настроек экспорта
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Настройки экспорта",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ListItem(
                            headlineContent = { Text("Директория экспорта") },
                            supportingContent = { 
                                Text(exportDirectory?.absolutePath ?: "Не выбрана")
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { showDirectoryPicker = true }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Изменить"
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Справка
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Как это работает",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "• Кнопка 'Хранилище' в меню открывает папку с экспортированными файлами",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Кнопка 'Отправка' предлагает отправить текущую открытую заметку",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• В настройках экспорта можно выбрать папку для сохранения файлов",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Экспортированные файлы можно отправить в любое приложение",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Диалог экспорта
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Экспорт заметок") },
                text = { Text("Все заметки будут экспортированы во внешнее хранилище. Продолжить?") },
                confirmButton = {
                    Button(onClick = {
                        // TODO: Реализовать экспорт
                        showExportDialog = false
                    }) {
                        Text("Экспортировать")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }

        // Диалог импорта
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Импорт заметок") },
                text = { Text("Выберите файл для импорта") },
                confirmButton = {
                    Button(onClick = {
                        pickFile.launch("*/*")
                        showImportDialog = false
                    }) {
                        Text("Выбрать файл")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            if (isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = if (isGranted) "Разрешено" else "Не разрешено",
            tint = if (isGranted) MaterialTheme.colorScheme.primary 
                  else MaterialTheme.colorScheme.error
        )
    }
} 