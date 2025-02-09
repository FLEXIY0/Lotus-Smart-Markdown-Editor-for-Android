package com.flesiy.Lotus.ui.components

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flesiy.Lotus.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperRoom(
    onBack: () -> Unit,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isTextProcessorEnabled by viewModel.isTextProcessorEnabled.collectAsState()
    val isGroqEnabled by viewModel.isGroqEnabled.collectAsState()
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Комната разработчика") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "Экспериментальный режим",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = "Данный раздел содержит экспериментальный функционал, который находится в разработке. Использование этих функций может привести к непредсказуемым результатам.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Настройки распознавания речи",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Постобработка текста",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Автоматическое исправление пунктуации и форматирования",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isTextProcessorEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && isGroqEnabled) {
                                    viewModel.setGroqEnabled(false)
                                }
                                viewModel.setTextProcessorEnabled(enabled)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "AI постобработка (Groq)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (isGroqEnabled) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = "Активно",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Использование ИИ для исправления текста",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isGroqEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (context.checkSelfPermission(android.Manifest.permission.INTERNET) 
                                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        showErrorDialog = true
                                        errorMessage = "Для работы AI требуется разрешение на доступ в интернет"
                                        return@Switch
                                    }
                                    viewModel.setTextProcessorEnabled(false)
                                }
                                viewModel.setGroqEnabled(enabled)
                            }
                        )
                    }
                }
            }
            
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Функции в разработке:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "• Расширенная аналитика использования памяти\n" +
                              "• Инструменты отладки\n" +
                              "• Экспериментальные функции редактора\n" +
                              "• AI постобработка текста",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Ошибка") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

private fun checkInternetPermission(context: Context): Boolean {
    return context.checkSelfPermission(android.Manifest.permission.INTERNET) == 
        android.content.pm.PackageManager.PERMISSION_GRANTED
} 