package com.flesiy.Lotus.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
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
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.flesiy.Lotus.viewmodel.MainViewModel
import java.io.File
import android.util.Log
import android.provider.DocumentsContract

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
    val exportOnlyNew by viewModel.exportOnlyNew.collectAsState()
    var hasStoragePermission by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDirectoryPicker by remember { mutableStateOf(false) }
    val exportProgress by viewModel.exportProgress.collectAsState()

    // Проверяем разрешения при запуске
    LaunchedEffect(Unit) {
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    // Запрос разрешений для Android < 11
    val requestOldStoragePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasStoragePermission = permissions.values.all { it }
    }

    // Выбор директории для экспорта
    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Получаем разрешение на доступ к выбранной директории
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            
            // Получаем DocumentFile для выбранной директории
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            documentFile?.let { docFile ->
                // Логируем информацию о выбранной директории
                Log.d("FileManagement", "Выбрана директория: ${docFile.uri}")
                Log.d("FileManagement", "Название директории: ${docFile.name}")
                Log.d("FileManagement", "Тип: ${docFile.type}")
                Log.d("FileManagement", "Может записывать: ${docFile.canWrite()}")
                
                try {
                    // Преобразуем URI в путь к реальной директории
                    val path = uri.path?.replace("/tree/primary:", "/storage/emulated/0/")
                        ?.replace("/document/primary:", "/storage/emulated/0/")
                    
                    path?.let { realPath ->
                        Log.d("FileManagement", "Преобразованный путь: $realPath")
                        val exportDir = File(realPath)
                        if (!exportDir.exists()) {
                            val created = exportDir.mkdirs()
                            Log.d("FileManagement", "Создание выбранной директории: $created")
                        }
                        Log.d("FileManagement", "Путь к директории экспорта: ${exportDir.absolutePath}")
                        viewModel.setExportDirectory(exportDir)
                    } ?: run {
                        Log.e("FileManagement", "Не удалось преобразовать URI в путь")
                        // Используем резервный вариант
                        val backupDir = File(context.getExternalFilesDir(null), "Lotus")
                        if (!backupDir.exists()) {
                            backupDir.mkdirs()
                        }
                        Log.d("FileManagement", "Использование резервной директории: ${backupDir.absolutePath}")
                        viewModel.setExportDirectory(backupDir)
                    }
                } catch (e: Exception) {
                    Log.e("FileManagement", "Ошибка при создании директории", e)
                    // Используем резервный вариант
                    val backupDir = File(context.getExternalFilesDir(null), "Lotus")
                    if (!backupDir.exists()) {
                        backupDir.mkdirs()
                    }
                    Log.d("FileManagement", "Использование резервной директории: ${backupDir.absolutePath}")
                    viewModel.setExportDirectory(backupDir)
                }
            }
        }
    }

    // Выбор файла для импорта
    val pickFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                val content = inputStream.bufferedReader().readText()
                // Создаем новую заметку с импортированным содержимым
                viewModel.createNewNoteWithContent(content)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Управление файлами") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Карточка с разрешениями
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        context.startActivity(intent)
                    } else {
                        requestOldStoragePermission.launch(
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        )
                    }
                }
            ) {
                ListItem(
                    headlineContent = { Text("Разрешения") },
                    supportingContent = {
                        Text(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                                "Доступ ко всем файлам для импорта/экспорта"
                            else
                                "Доступ к внешнему хранилищу для импорта/экспорта"
                        )
                    },
                    leadingContent = {
                        Icon(
                            if (hasStoragePermission) 
                                Icons.Default.CheckCircle 
                            else 
                                Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasStoragePermission)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                )
            }

            // Основные действия
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        Log.d("FileManagement", "Нажата кнопка экспорта")
                        showExportDialog = true 
                    },
                    enabled = hasStoragePermission
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "Экспорт",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Сохранить все заметки",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    onClick = { showImportDialog = true },
                    enabled = hasStoragePermission
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "Импорт",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Загрузить заметки",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Директория экспорта
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Директория экспорта",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            exportDirectory?.path ?: "Не выбрана",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { directoryPicker.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Изменить")
                        }
                        
                        Button(
                            onClick = {
                                exportDirectory?.let { dir ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW)
                                        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A${dir.name}")
                                        intent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Log.e("FileManagement", "Ошибка при открытии директории", e)
                                    }
                                }
                            },
                            enabled = exportDirectory != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Открыть")
                        }
                    }
                }
            }
        }

        // Диалог экспорта
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showExportDialog = false 
                    viewModel.resetExportProgress()
                },
                icon = { Icon(Icons.Default.Upload, contentDescription = null) },
                title = { Text("Экспорт заметок") },
                text = { 
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Все заметки будут экспортированы в директорию:")
                        exportDirectory?.let { dir ->
                            Text(
                                dir.absolutePath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } ?: Text(
                            "Директория не выбрана",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )

                        // Добавляем переключатель для экспорта только новых заметок
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Экспортировать только новые",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Экспорт только заметок, измененных после последнего экспорта",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = exportOnlyNew,
                                onCheckedChange = { viewModel.setExportOnlyNew(it) }
                            )
                        }

                        when (val progress = exportProgress) {
                            is MainViewModel.ExportProgress.InProgress -> {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LinearProgressIndicator(
                                        progress = progress.current.toFloat() / progress.total,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        "Экспортировано ${progress.current} из ${progress.total}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            is MainViewModel.ExportProgress.Error -> {
                                Text(
                                    progress.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {}
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            exportDirectory?.let { dir ->
                                Log.d("FileManagement", "Начало экспорта в директорию: ${dir.absolutePath}")
                                viewModel.exportNotes(dir)
                            }
                        },
                        enabled = exportDirectory != null && exportProgress !is MainViewModel.ExportProgress.InProgress
                    ) {
                        Text("Экспортировать")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showExportDialog = false
                            viewModel.resetExportProgress()
                        },
                        enabled = exportProgress !is MainViewModel.ExportProgress.InProgress
                    ) {
                        Text("Отмена")
                    }
                }
            )
        }

        // Показываем уведомление об успешном экспорте
        LaunchedEffect(exportProgress) {
            when (exportProgress) {
                is MainViewModel.ExportProgress.Success -> {
                    if (showExportDialog) {  // Проверяем, что диалог открыт
                        showExportDialog = false
                        snackbarHostState.showSnackbar(
                            message = "Экспорт успешно завершен",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                is MainViewModel.ExportProgress.Error -> {
                    snackbarHostState.showSnackbar(
                        message = (exportProgress as MainViewModel.ExportProgress.Error).message,
                        duration = SnackbarDuration.Long
                    )
                }
                else -> {}
            }
        }

        // Диалог импорта
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                icon = { Icon(Icons.Default.Download, contentDescription = null) },
                title = { Text("Импорт заметок") },
                text = { Text("Выберите файл для импорта") },
                confirmButton = {
                    Button(
                        onClick = {
                            pickFile.launch("*/*")
                            showImportDialog = false
                        }
                    ) {
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