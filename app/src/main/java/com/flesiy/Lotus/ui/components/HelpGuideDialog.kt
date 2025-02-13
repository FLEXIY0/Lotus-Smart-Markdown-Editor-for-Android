package com.flesiy.Lotus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

data class HelpItem(
    val title: String,
    val description: String,
    val icon: @Composable () -> Unit,
    val example: String? = null
)

data class HelpSection(
    val title: String,
    val items: List<HelpItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpGuideDialog(
    onDismiss: () -> Unit
) {
    val helpSections = listOf(
        HelpSection(
            title = "Основные функции",
            items = listOf(
                HelpItem(
                    title = "Создание заметки",
                    description = "Нажмите на кнопку '+' в нижнем правом углу для создания новой заметки. Заголовок заметки автоматически формируется из первой строки текста с использованием #.",
                    icon = { Icon(Icons.Default.Add, contentDescription = null) }
                ),
                HelpItem(
                    title = "Удаление заметки",
                    description = "Смахните заметку вправо в списке для удаления. Удаленные заметки перемещаются в корзину, где хранятся в соответствии с выбранным периодом хранения.",
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) }
                ),
                HelpItem(
                    title = "Закрепление заметки",
                    description = "Смахните заметку влево в списке для закрепления. Закрепленные заметки отображаются в отдельной секции вверху списка и могут быть переупорядочены.",
                    icon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                ),
                HelpItem(
                    title = "Режим предпросмотра",
                    description = "Используйте панель быстрого редактирования для переключения между режимом редактирования и предпросмотра. В режиме предпросмотра можно видеть отформатированный текст и отмечать задачи как выполненные.",
                    icon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                ),
                HelpItem(
                    title = "Быстрое удаление",
                    description = "Нажмите на кнопку 'X' в верхней панели для удаления текущей заметки и создания новой. Удаленная заметка будет перемещена в корзину.",
                    icon = { Icon(Icons.Default.Clear, contentDescription = null) }
                )
            )
        ),
        HelpSection(
            title = "Форматирование текста",
            items = listOf(
                HelpItem(
                    title = "Заголовки",
                    description = "Используйте '#' для заголовков разного уровня (от 1 до 6 '#')",
                    icon = { Icon(Icons.Default.Title, contentDescription = null) },
                    example = "# Заголовок 1\n## Заголовок 2"
                ),
                HelpItem(
                    title = "Списки задач",
                    description = "Создавайте интерактивные списки задач. В режиме предпросмотра можно отмечать задачи как выполненные.",
                    icon = { Icon(Icons.Default.CheckBox, contentDescription = null) },
                    example = "- [ ] Задача 1\n- [x] Выполненная задача"
                ),
                HelpItem(
                    title = "Маркированные списки",
                    description = "Создавайте маркированные списки, используя '-', '+' или '*' в начале строки",
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    example = "- Пункт 1\n- Пункт 2"
                ),
                HelpItem(
                    title = "Нумерованные списки",
                    description = "Создавайте нумерованные списки, используя цифры с точкой",
                    icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                    example = "1. Первый пункт\n2. Второй пункт"
                ),
                HelpItem(
                    title = "Выделение текста",
                    description = "Используйте '*' или '_' для курсива, '**' или '__' для жирного текста",
                    icon = { Icon(Icons.Default.FormatBold, contentDescription = null) },
                    example = "*курсив* или **жирный**"
                ),
                HelpItem(
                    title = "Цитаты",
                    description = "Используйте '>' в начале строки для создания цитат",
                    icon = { Icon(Icons.Default.FormatQuote, contentDescription = null) },
                    example = "> Это цитата"
                ),
                HelpItem(
                    title = "Таблицы",
                    description = "Создавайте таблицы, используя вертикальные линии и дефисы",
                    icon = { Icon(Icons.Default.TableChart, contentDescription = null) },
                    example = "| Заголовок 1 | Заголовок 2 |\n|------------|-------------|\n| Ячейка 1 | Ячейка 2 |"
                ),
                HelpItem(
                    title = "Ссылки и изображения",
                    description = "Вставляйте ссылки и изображения с помощью специального синтаксиса",
                    icon = { Icon(Icons.Default.Link, contentDescription = null) },
                    example = "[текст ссылки](URL)\n![описание изображения](путь)"
                ),
                HelpItem(
                    title = "HTML-разметка",
                    description = "Поддерживается базовая HTML-разметка для дополнительного форматирования",
                    icon = { Icon(Icons.Default.Code, contentDescription = null) },
                    example = "<u>подчёркнутый</u>\n<s>зачёркнутый</s>"
                )
            )
        ),
        HelpSection(
            title = "Дополнительные функции",
            items = listOf(
                HelpItem(
                    title = "Уведомления",
                    description = "Создавайте напоминания для заметок с различными интервалами повторения: ежедневно, еженедельно, ежемесячно, ежегодно или в определенные дни недели. Уведомления можно включать и отключать, а также редактировать их параметры.",
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) }
                ),
                HelpItem(
                    title = "Временные метки",
                    description = "При создании и редактировании заметок можно добавлять временные метки, которые создают отметку о дате и времени в тексте заметки.",
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = null) }
                ),
                HelpItem(
                    title = "Голосовой ввод",
                    description = "Используйте кнопку микрофона для голосового ввода текста. Нажмите кнопку еще раз для остановки записи.",
                    icon = { Icon(Icons.Default.Mic, contentDescription = null) }
                ),
                HelpItem(
                    title = "Размер шрифта",
                    description = "Регулируйте размер шрифта с помощью слайдера в верхней панели приложения.",
                    icon = { Icon(Icons.Default.TextFields, contentDescription = null) }
                ),
                HelpItem(
                    title = "Корзина",
                    description = "Удаленные заметки перемещаются в корзину. Период хранения можно настроить от 1 недели до бесконечности. Заметки можно восстановить или удалить окончательно.",
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) }
                ),
                HelpItem(
                    title = "История версий",
                    description = "Автоматически сохраняются версии заметок при существенных изменениях. Доступен просмотр и восстановление предыдущих версий.",
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            )
        ),
        HelpSection(
            title = "Экспериментальные функции",
            items = listOf(
                HelpItem(
                    title = "AI-обработка текста",
                    description = "Функция постобработки текста с помощью AI находится в разработке. В будущем будет доступна для улучшения форматирования и структуры заметок.",
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) }
                ),
                HelpItem(
                    title = "Управление файлами",
                    description = "Функции экспорта и импорта заметок, а также синхронизация находятся в разработке.",
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) }
                )
            )
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Справка",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(helpSections) { section ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            section.items.forEach { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                            ) {
                                                item.icon()
                                            }
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                        if (item.example != null) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            ) {
                                                Text(
                                                    text = item.example,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.padding(8.dp),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
} 