package com.flesiy.Lotus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flesiy.Lotus.viewmodel.NoteNotification
import com.flesiy.Lotus.data.RepeatInterval
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationDialog(
    initialNotification: NoteNotification? = null,
    onDismiss: () -> Unit,
    onConfirm: (NoteNotification) -> Unit,
    noteId: Long
) {
    var title by remember { mutableStateOf(initialNotification?.title ?: "") }
    var description by remember { mutableStateOf(initialNotification?.description ?: "") }
    var selectedDateTime by remember { 
        mutableStateOf(initialNotification?.triggerTime ?: LocalDateTime.now().plusMinutes(5)) 
    }
    var repeatInterval by remember { 
        mutableStateOf(initialNotification?.repeatInterval ?: RepeatInterval.NONE) 
    }
    var selectedDays by remember {
        mutableStateOf(initialNotification?.selectedDays ?: emptyList())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDaysPicker by remember { mutableStateOf(false) }
    
    // Проверяем, можно ли создать уведомление с текущими параметрами
    val isValidDateTime = remember(selectedDateTime, repeatInterval, selectedDays) {
        val now = LocalDateTime.now()
        when (repeatInterval) {
            RepeatInterval.NONE -> selectedDateTime.isAfter(now)
            RepeatInterval.SPECIFIC_DAYS -> {
                if (selectedDays.isEmpty()) false
                else {
                    val currentDayOfWeek = now.dayOfWeek.value
                    val selectedTime = selectedDateTime.toLocalTime()
                    val currentTime = now.toLocalTime()
                    
                    // Если выбран текущий день, проверяем время
                    if (selectedDays.contains(currentDayOfWeek)) {
                        selectedTime.isAfter(currentTime)
                    } else {
                        // Если выбраны другие дни, всегда разрешаем
                        true
                    }
                }
            }
            else -> true // Для других интервалов всегда разрешаем
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialNotification == null) "Новое уведомление" else "Редактировать уведомление") },
        modifier = Modifier.fillMaxWidth(0.95f),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Заголовок") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Время уведомления",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(selectedDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(selectedDateTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Повторять",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RepeatInterval.values().forEach { interval ->
                            FilterChip(
                                selected = repeatInterval == interval,
                                onClick = { 
                                    repeatInterval = interval
                                    if (interval == RepeatInterval.SPECIFIC_DAYS) {
                                        showDaysPicker = true
                                    }
                                },
                                label = { 
                                    Text(
                                        when(interval) {
                                            RepeatInterval.DAILY -> "Ежедневно"
                                            RepeatInterval.WEEKLY -> "Еженедельно"
                                            RepeatInterval.MONTHLY -> "Ежемесячно"
                                            RepeatInterval.YEARLY -> "Ежегодно"
                                            RepeatInterval.SPECIFIC_DAYS -> "Определенные дни"
                                            RepeatInterval.NONE -> "Не повторять"
                                        }
                                    )
                                }
                            )
                        }
                    }

                    // Показываем выбранные дни, если выбран режим SPECIFIC_DAYS
                    if (repeatInterval == RepeatInterval.SPECIFIC_DAYS && selectedDays.isNotEmpty()) {
                        Text(
                            text = "Выбранные дни: " + selectedDays.sorted().map { dayNumber ->
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
                            }.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isValidDateTime) {
                    Text(
                        text = when {
                            repeatInterval == RepeatInterval.SPECIFIC_DAYS && selectedDays.isEmpty() ->
                                "Выберите хотя бы один день недели"
                            else -> "Выбранное время уже прошло"
                        },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val notification = NoteNotification(
                        id = initialNotification?.id ?: System.currentTimeMillis(),
                        noteId = noteId,
                        title = title,
                        description = description,
                        triggerTime = selectedDateTime,
                        repeatInterval = repeatInterval,
                        selectedDays = if (repeatInterval == RepeatInterval.SPECIFIC_DAYS) selectedDays else emptyList()
                    )
                    onConfirm(notification)
                    onDismiss()
                },
                enabled = title.isNotBlank() && isValidDateTime
            ) {
                Text(if (initialNotification == null) "Создать" else "Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(millis),
                                ZoneId.systemDefault()
                            )
                            // Проверяем, что выбранная дата не в прошлом
                            if (!newDate.toLocalDate().isBefore(LocalDate.now())) {
                                selectedDateTime = selectedDateTime
                                    .withYear(newDate.year)
                                    .withMonth(newDate.monthValue)
                                    .withDayOfMonth(newDate.dayOfMonth)
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedDateTime.hour,
            initialMinute = selectedDateTime.minute,
            is24Hour = true
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Выберите время") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        layoutType = TimePickerLayoutType.Vertical
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newDateTime = selectedDateTime
                            .withHour(timePickerState.hour)
                            .withMinute(timePickerState.minute)
                        
                        // Проверяем, что новое время не в прошлом
                        if (newDateTime.isAfter(LocalDateTime.now()) || repeatInterval != RepeatInterval.NONE) {
                            selectedDateTime = newDateTime
                        }
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showDaysPicker) {
        AlertDialog(
            onDismissRequest = { showDaysPicker = false },
            title = { Text("Выберите дни недели") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        1 to "Понедельник",
                        2 to "Вторник",
                        3 to "Среда",
                        4 to "Четверг",
                        5 to "Пятница",
                        6 to "Суббота",
                        7 to "Воскресенье"
                    ).forEach { (dayNumber, dayName) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dayName)
                            Checkbox(
                                checked = selectedDays.contains(dayNumber),
                                onCheckedChange = { checked ->
                                    selectedDays = if (checked) {
                                        selectedDays + dayNumber
                                    } else {
                                        selectedDays - dayNumber
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDaysPicker = false },
                    enabled = selectedDays.isNotEmpty()
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (selectedDays.isEmpty()) {
                        repeatInterval = RepeatInterval.NONE
                    }
                    showDaysPicker = false
                }) {
                    Text("Отмена")
                }
            }
        )
    }
} 