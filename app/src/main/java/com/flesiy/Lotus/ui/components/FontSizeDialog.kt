package com.flesiy.Lotus.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FontSizeDialog(
    currentSize: Float,
    isDarkTheme: Boolean,
    onSizeChange: (Float) -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tempSize by remember { mutableStateOf(currentSize) }
    var tempIsDarkTheme by remember { mutableStateOf(isDarkTheme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Размер шрифта")
                FontSizeSlider(
                    value = tempSize,
                    onValueChange = { tempSize = it }
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Тёмная тема")
                    Switch(
                        checked = tempIsDarkTheme,
                        onCheckedChange = { isChecked -> 
                            tempIsDarkTheme = isChecked
                            Log.d("FontSizeDialog", "Переключение темы: $isChecked")
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSizeChange(tempSize)
                    onThemeChange(tempIsDarkTheme)
                    onDismiss()
                }
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
} 