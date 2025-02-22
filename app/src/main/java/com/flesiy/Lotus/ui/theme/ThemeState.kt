package com.flesiy.Lotus.ui.theme

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.flesiy.Lotus.data.UserPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

object ThemeState {
    private lateinit var userPreferences: UserPreferences

    var isDarkTheme by mutableStateOf(false)
        private set

    fun init(context: Context) {
        userPreferences = UserPreferences(context)
        isDarkTheme = userPreferences.isDarkTheme
    }

    fun toggleTheme(isDark: Boolean) {
        Log.d("ThemeState", "Изменение темы: $isDark")
        isDarkTheme = isDark
        userPreferences.isDarkTheme = isDark
    }
}

val LocalThemeState = staticCompositionLocalOf { ThemeState } 