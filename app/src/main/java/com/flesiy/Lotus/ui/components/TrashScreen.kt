package com.flesiy.Lotus.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flesiy.Lotus.data.TrashManager
import com.flesiy.Lotus.data.TrashNote
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    notes: List<TrashNote>,
    currentRetentionPeriod: TrashManager.RetentionPeriod,
    trashSize: Long,
    isOverLimit: Boolean,
    cacheStats: TrashManager.CacheStats,
    onRetentionPeriodChange: (TrashManager.RetentionPeriod) -> Unit,
    onRestoreNote: (Long) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onClearTrash: () -> Unit,
    onClearCache: () -> Unit,
    onClearImagesCache: () -> Unit,
    onClearFilesCache: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showClearCacheConfirmation by remember { mutableStateOf(false) }
    var showClearImagesCacheConfirmation by remember { mutableStateOf(false) }
    var showClearFilesCacheConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Корзина") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (notes.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirmation = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Очистить корзину",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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
            // Информация о корзине
            item {
                TrashInfo(
                    trashSize = trashSize,
                    isOverLimit = isOverLimit,
                    currentRetentionPeriod = currentRetentionPeriod,
                    onRetentionPeriodChange = onRetentionPeriodChange
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Секция кеша
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
                            text = "Кеш",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Общий размер кеша
                        ListItem(
                            headlineContent = { Text("Общий размер кеша") },
                            supportingContent = { Text(formatSize(cacheStats.totalSize)) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                TextButton(
                                    onClick = { showClearCacheConfirmation = true },
                                    enabled = cacheStats.totalSize > 0
                                ) {
                                    Text("Очистить")
                                }
                            }
                        )

                        // Кеш изображений
                        ListItem(
                            headlineContent = { Text("Кеш изображений") },
                            supportingContent = { 
                                Column {
                                    Text(formatSize(cacheStats.imagesSize))
                                    if (cacheStats.noteImagesCount.isNotEmpty()) {
                                        Text(
                                            "Временные файлы изображений в ${cacheStats.noteImagesCount.size} заметках",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                TextButton(
                                    onClick = { showClearImagesCacheConfirmation = true },
                                    enabled = cacheStats.imagesSize > 0
                                ) {
                                    Text("Очистить кеш")
                                }
                            }
                        )

                        // Временные файлы
                        ListItem(
                            headlineContent = { Text("Временные файлы") },
                            supportingContent = { 
                                Text(
                                    "Файлы предпросмотра и временные файлы импорта/экспорта (${formatSize(cacheStats.filesSize)})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                TextButton(
                                    onClick = { showClearFilesCacheConfirmation = true },
                                    enabled = cacheStats.filesSize > 0
                                ) {
                                    Text("Очистить")
                                }
                            }
                        )
                    }
                }
            }

            if (notes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Корзина пуста",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Список удаленных заметок
                items(notes) { note ->
                    TrashNoteItem(
                        note = note,
                        retentionPeriod = currentRetentionPeriod,
                        onRestore = { onRestoreNote(note.id) },
                        onDelete = { onDeleteNote(note.id) }
                    )
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Очистить корзину?") },
            text = { Text("Все заметки в корзине будут удалены безвозвратно. Это действие нельзя отменить.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearTrash()
                        showClearConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалоги подтверждения очистки кеша
    if (showClearCacheConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirmation = false },
            title = { Text("Очистить весь кеш?") },
            text = { 
                Text(
                    "Будут удалены все кешированные данные, включая изображения и файлы. " +
                    "Это действие нельзя отменить."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearCache()
                        showClearCacheConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showClearImagesCacheConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearImagesCacheConfirmation = false },
            title = { Text("Очистить кеш изображений?") },
            text = { 
                Text(
                    "Будут удалены все кешированные изображения. " +
                    "Это действие нельзя отменить."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearImagesCache()
                        showClearImagesCacheConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearImagesCacheConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showClearFilesCacheConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearFilesCacheConfirmation = false },
            title = { Text("Очистить кеш файлов?") },
            text = { 
                Text(
                    "Будут удалены все кешированные файлы. " +
                    "Это действие нельзя отменить."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearFilesCache()
                        showClearFilesCacheConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearFilesCacheConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun TrashInfo(
    trashSize: Long,
    isOverLimit: Boolean,
    currentRetentionPeriod: TrashManager.RetentionPeriod,
    onRetentionPeriodChange: (TrashManager.RetentionPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Размер корзины
        Column {
            Text(
                text = "Занято места:",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatSize(trashSize),
                style = MaterialTheme.typography.titleMedium,
                color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }

        // Период хранения
        Button(
            onClick = {
                val periods = TrashManager.RetentionPeriod.values()
                val currentIndex = periods.indexOf(currentRetentionPeriod)
                val nextIndex = (currentIndex + 1) % periods.size
                onRetentionPeriodChange(periods[nextIndex])
            }
        ) {
            Text(currentRetentionPeriod.displayName)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashNoteItem(
    note: TrashNote,
    retentionPeriod: TrashManager.RetentionPeriod,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showFullContent by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = formatDeletionDate(note.deletionTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (retentionPeriod != TrashManager.RetentionPeriod.NEVER) {
                        Text(
                            text = "Будет удалено через ${getRemainingDays(note.deletionTime, retentionPeriod)} дн.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Свернуть" else "Развернуть"
                        )
                    }
                    IconButton(onClick = onRestore) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = "Восстановить",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Text(
                        text = if (showFullContent) note.content else note.content.take(500),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (note.content.length > 500 && !showFullContent) {
                        TextButton(onClick = { showFullContent = true }) {
                            Text("Показать полностью")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Удалить заметку?") },
            text = { Text("Заметка \"${note.title}\" будет удалена безвозвратно. Это действие нельзя отменить.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

private fun formatDeletionDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return "Удалено: ${sdf.format(Date(timestamp))}"
}

private fun getRemainingDays(deletionTime: Long, retentionPeriod: TrashManager.RetentionPeriod): Int {
    if (retentionPeriod == TrashManager.RetentionPeriod.NEVER) return -1
    val currentTime = System.currentTimeMillis()
    val retentionMillis = retentionPeriod.days * 24 * 60 * 60 * 1000L
    val remainingMillis = (deletionTime + retentionMillis) - currentTime
    return (remainingMillis / (24 * 60 * 60 * 1000L)).toInt()
} 