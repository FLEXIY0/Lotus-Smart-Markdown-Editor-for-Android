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
import androidx.compose.ui.text.style.TextOverflow
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
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val sortedVersions = remember(versions) { versions.sortedBy { it.createdAt } }

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

            if (versions.isEmpty()) {
                Text(
                    text = "Нет сохраненных версий",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Вычисляем steps только если есть хотя бы 2 версии
                val stepsCount = if (sortedVersions.size > 1) {
                    sortedVersions.size - 2
                } else {
                    0
                }
                
                Slider(
                    value = selectedVersion?.let { selected ->
                        sortedVersions.indexOf(selected).toFloat()
                    } ?: (sortedVersions.size - 1).toFloat(),
                    onValueChange = { value ->
                        val index = value.toInt().coerceIn(0, sortedVersions.size - 1)
                        onVersionSelected(sortedVersions.getOrNull(index))
                    },
                    valueRange = 0f..(sortedVersions.size - 1).toFloat(),
                    steps = stepsCount,
                    modifier = Modifier.fillMaxWidth()
                )

                selectedVersion?.let { version ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
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
                }
            }
        }
    }
} 