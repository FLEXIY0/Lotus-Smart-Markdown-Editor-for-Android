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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.flesiy.Lotus.R

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
            title = stringResource(
                R.string.help_guide_section_main_features),
            items = listOf(
                HelpItem(
                    title = stringResource(R.string.help_create_note_title),
                    description = stringResource(R.string.help_create_note_description),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) }
                ),
                HelpItem(
                    title = stringResource(R.string.help_delete_note_title),
                    description = stringResource(R.string.help_delete_note_description),
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) }
                ),
                HelpItem(
                    title = stringResource(R.string.help_pin_note_title),
                    description = stringResource(R.string.help_pin_note_description),
                    icon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                ),
                HelpItem(
                    title = stringResource(R.string.help_preview_mode_title),
                    description = stringResource(R.string.help_preview_mode_description),
                    icon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                ),
                HelpItem(
                    title = stringResource(R.string.help_quick_delete_title),
                    description = stringResource(R.string.help_quick_delete_description),
                    icon = { Icon(Icons.Default.Clear, contentDescription = null) }
                )
            )
        ),
        HelpSection(
            title = stringResource(R.string.help_guide_section_text_formatting),
            items = listOf(
                HelpItem(
                    title = stringResource(R.string.help_headers_title),
                    description = stringResource(R.string.help_headers_description),
                    icon = { Icon(Icons.Default.Title, contentDescription = null) },
                    example = "# Заголовок 1\n## Заголовок 2"
                ),
                HelpItem(
                    title = stringResource(R.string.help_task_lists_title),
                    description = stringResource(R.string.help_task_lists_description),
                    icon = { Icon(Icons.Default.CheckBox, contentDescription = null) },
                    example = "- [ ] Задача 1\n- [x] Выполненная задача"
                ),
                HelpItem(
                    title = stringResource(R.string.help_bullet_lists_title),
                    description = stringResource(R.string.help_bullet_lists_description),
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    example = "- Пункт 1\n- Пункт 2"
                ),
                HelpItem(
                    title = stringResource(R.string.help_numbered_lists_title),
                    description = stringResource(R.string.help_numbered_lists_description),
                    icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                    example = "1. Первый пункт\n2. Второй пункт"
                ),
                HelpItem(
                    title = stringResource(R.string.help_text_formatting_title),
                    description = stringResource(R.string.help_text_formatting_description),
                    icon = { Icon(Icons.Default.FormatBold, contentDescription = null) },
                    example = "*курсив* или **жирный**"
                ),
                HelpItem(
                    title = stringResource(R.string.help_quotes_title),
                    description = stringResource(R.string.help_quotes_description),
                    icon = { Icon(Icons.Default.FormatQuote, contentDescription = null) },
                    example = "> Это цитата"
                ),
                HelpItem(
                    title = stringResource(R.string.help_tables_title),
                    description = stringResource(R.string.help_tables_description),
                    icon = { Icon(Icons.Default.TableChart, contentDescription = null) },
                    example = "| Заголовок 1 | Заголовок 2 |\n|------------|-------------|\n| Ячейка 1 | Ячейка 2 |"
                ),
                HelpItem(
                    title = stringResource(R.string.help_links_title),
                    description = stringResource(R.string.help_links_description),
                    icon = { Icon(Icons.Default.Link, contentDescription = null) },
                    example = "[текст ссылки](URL)\n![описание изображения](путь)"
                ),
                HelpItem(
                    title = stringResource(R.string.help_html_title),
                    description = stringResource(R.string.help_html_description),
                    icon = { Icon(Icons.Default.Code, contentDescription = null) },
                    example = "<u>подчёркнутый</u>\n<s>зачёркнутый</s>"
                )
            )
        ),
        HelpSection(
            title = stringResource(R.string.help_guide_section_additional_features),
            items = listOf(
                HelpItem(
                    title = stringResource(R.string.help_notifications_title),
                    description = stringResource(R.string.help_notifications_description),
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) }
                ),
                HelpItem(
                    title = stringResource(R.string.help_timestamps_title),
                    description = stringResource(R.string.help_timestamps_description),
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = null) }
                ),
                HelpItem(
                    title = stringResource(R.string.help_voice_input_title),
                    description = stringResource(R.string.help_voice_input_description),
                    icon = { Icon(Icons.Default.Mic, contentDescription = null) }
                ),
                HelpItem(
                    title = stringResource(R.string.help_font_size_title),
                    description = stringResource(R.string.help_font_size_description),
                    icon = { Icon(Icons.Default.TextFields, contentDescription = null) }
                ),
                HelpItem(
                    title = stringResource(R.string.help_trash_title),
                    description = stringResource(R.string.help_trash_description),
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) }
                ),
                HelpItem(
                    title = stringResource(R.string.help_version_history_title),
                    description = stringResource(R.string.help_version_history_description),
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            )
        ),
        HelpSection(
            title = stringResource(R.string.help_guide_section_experimental_features),
            items = listOf(
                HelpItem(
                    title = stringResource(R.string.help_ai_processing_title),
                    description = stringResource(R.string.help_ai_processing_description),
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) }
                ),
                HelpItem(
                    title = stringResource(R.string.help_file_management_title),
                    description = stringResource(R.string.help_file_management_description),
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) }
                )
            )
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.help_guide_title),
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