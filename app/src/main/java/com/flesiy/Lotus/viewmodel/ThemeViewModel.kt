package com.flesiy.Lotus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flesiy.Lotus.data.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val themePreferences = ThemePreferences(application)

    val useClassicTheme: StateFlow<Boolean> = themePreferences.useClassicTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setUseClassicTheme(useClassic: Boolean) {
        viewModelScope.launch {
            themePreferences.setUseClassicTheme(useClassic)
        }
    }
} 