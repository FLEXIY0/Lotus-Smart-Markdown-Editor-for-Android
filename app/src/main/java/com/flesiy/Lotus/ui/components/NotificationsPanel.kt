package com.flesiy.Lotus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flesiy.Lotus.R
import com.flesiy.Lotus.viewmodel.NoteNotification
import com.flesiy.Lotus.data.RepeatInterval
import java.time.format.DateTimeFormatter

@Composable
fun NotificationsPanel(
    notifications: List<NoteNotification>,
    onEditNotification: (NoteNotification) -> Unit,
    onDeleteNotification: (NoteNotification) -> Unit,
    onToggleNotification: (NoteNotification) -> Unit,
    onAddNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Нет уведомлений",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onEdit = { onEditNotification(notification) },
                            onDelete = { onDeleteNotification(notification) },
                            onToggle = { onToggleNotification(notification) }
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Button(
                onClick = onAddNotification,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notification),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Добавить уведомление")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationItem(
    notification: NoteNotification,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        notification.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (notification.description.isNotEmpty()) {
                        Text(
                            notification.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                Switch(
                    checked = notification.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }

            // Время и повторение
            Text(
                buildString {
                    append(notification.triggerTime.format(
                        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    ))
                    notification.repeatInterval?.let { interval ->
                        if (interval != RepeatInterval.NONE) {
                            append("\n")
                            append(when(interval) {
                                RepeatInterval.DAILY -> "Ежедневно"
                                RepeatInterval.WEEKLY -> "Еженедельно"
                                RepeatInterval.MONTHLY -> "Ежемесячно"
                                RepeatInterval.YEARLY -> "Ежегодно"
                                RepeatInterval.SPECIFIC_DAYS -> {
                                    val days = notification.selectedDays.sorted().map { dayNumber ->
                                        when (dayNumber) {
                                            1 -> "Пн"
                                            2 -> "Вт"
                                            3 -> "Ср"
                                            4 -> "Чт"
                                            5 -> "Пт"
                                            6 -> "Сб"
                                            7 -> "Вс"
                                            else -> ""
                                        }
                                    }.joinToString(", ")
                                    "По дням: $days"
                                }
                                RepeatInterval.NONE -> ""
                            })
                        }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Кнопки действий
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Изменить")
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Удалить")
                }
            }
        }
    }
} 