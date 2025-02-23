package com.flesiy.Lotus.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flesiy.Lotus.R

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
        title = { Text(stringResource(R.string.font_size_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.font_size_setting))
                FontSizeSlider(
                    value = tempSize,
                    onValueChange = { tempSize = it }
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (tempIsDarkTheme) stringResource(R.string.dark_theme) else stringResource(R.string.light_theme))
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
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
} 