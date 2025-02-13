package com.flesiy.Lotus.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FontSizeDialog(
    currentSize: Float,
    onSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tempSize by remember { mutableStateOf(currentSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Размер шрифта") },
        text = {
            FontSizeSlider(
                value = tempSize,
                onValueChange = { tempSize = it }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSizeChange(tempSize)
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