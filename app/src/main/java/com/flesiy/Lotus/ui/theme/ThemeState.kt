package com.flesiy.Lotus.ui.theme

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

object ThemeState {
    var isDarkTheme by mutableStateOf(false)
        private set

    fun toggleTheme(isDark: Boolean) {
        Log.d("ThemeState", "Изменение темы: $isDark")
        isDarkTheme = isDark
    }
}

val LocalThemeState = staticCompositionLocalOf { ThemeState } 