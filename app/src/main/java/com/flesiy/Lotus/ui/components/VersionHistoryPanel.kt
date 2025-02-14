package com.flesiy.Lotus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flesiy.Lotus.viewmodel.NoteVersion
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VersionHistoryPanel(
    versions: List<NoteVersion>,
    selectedVersion: NoteVersion?,
    onVersionSelected: (NoteVersion?) -> Unit,
    onApplyVersion: () -> Unit,
    onDeleteVersion: (NoteVersion) -> Unit = {},
    noteId: Long,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val noteVersions = remember(versions, noteId) { 
        versions.filter { it.noteId == noteId }.sortedBy { it.createdAt } 
    }

    AnimatedVisibility(
        visible = true,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Система контроля версий заметок",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (noteVersions.isEmpty()) {
                Text(
                    text = "Нет сохраненных версий",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Старые версии",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Новые версии",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                val currentValue = selectedVersion?.let { selected ->
                    noteVersions.indexOf(selected).coerceIn(0, noteVersions.size - 1).toFloat()
                } ?: (noteVersions.size - 1).toFloat()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Slider(
                        value = currentValue,
                        onValueChange = { value ->
                            val safeValue = value.coerceIn(0f, (noteVersions.size - 1).toFloat())
                            val index = safeValue.toInt()
                            if (index >= 0 && index < noteVersions.size) {
                                onVersionSelected(noteVersions[index])
                            }
                        },
                        valueRange = 0f..(noteVersions.size - 1).toFloat(),
                        steps = (noteVersions.size - 2).coerceAtLeast(0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    
                    // Показываем номер версии
                    Text(
                        text = "Версия ${(currentValue + 1).toInt()} из ${noteVersions.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                selectedVersion?.let { version ->
                    // Проверяем, что выбранная версия принадлежит текущей заметке
                    if (version.noteId == noteId) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = version.title.ifEmpty { "Без названия" },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Создано: ${dateFormat.format(Date(version.createdAt))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                IconButton(
                                    onClick = { onDeleteVersion(version) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Удалить версию",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            // Кнопки действий
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onVersionSelected(null) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Отмена")
                                }
                                Button(
                                    onClick = onApplyVersion,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Применить")
                                }
                            }
                        }
                    } else {
                        // Если версия не принадлежит текущей заметке, сбрасываем выбор
                        LaunchedEffect(Unit) {
                            onVersionSelected(null)
                        }
                    }
                }
            }
        }
    }
} 