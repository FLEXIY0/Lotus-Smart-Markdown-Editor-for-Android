package com.flesiy.Lotus.ui.theme

import androidx.compose.ui.graphics.Color

// iOS системные цвета
private val ios_blue = Color(0xFF007AFF)      // Цвет иконок и интерактивных элементов
private val ios_green = Color(0xFF34C759)     // Цвет переключателей
private val ios_gray = Color(0xFF8E8E93)      // Для второстепенного текста
private val ios_nav_bg = Color(0xFFF2F2F7)    // Фон бокового меню
private val ios_content_bg = Color(0xFFEFEFF4) // Фон контента
private val ios_white = Color(0xFFFFFFFF)     // Белый цвет
private val ios_black = Color(0xFF000000)     // Черный цвет

// Светлая тема (iOS Light)
val classic_light_background = ios_content_bg      // Основной фон контента EFEFF4
val classic_light_surface = ios_white              // Поверхности (плитки, карточки)
val classic_light_primary = ios_blue               // Акцентный цвет (иконки, кнопки) 007AFF
val classic_light_onPrimary = ios_white            // Текст на акцентном цвете
val classic_light_secondary = ios_blue             // Цвет иконок в барах
val classic_light_onSecondary = ios_black          // Текст на синем
val classic_light_onBackground = ios_black         // Основной текст
val classic_light_onSurface = ios_black           // Текст на плитках
val classic_light_primaryContainer = ios_nav_bg    // Фон бокового меню F2F2F7
val classic_light_onPrimaryContainer = ios_blue    // Акценты на контейнерах
val classic_light_secondaryContainer = ios_white   // Фон баров (белый)
val classic_light_onSecondaryContainer = ios_black // Текст на белом фоне
val classic_light_surfaceVariant = ios_white      // Фон карточек
val classic_light_onSurfaceVariant = ios_black    // Текст на карточках

// Специальные цвета для iOS элементов
val classic_light_statusBarColor = ios_white       // Цвет статус бара (белый)
val classic_light_navigationBarColor = ios_white   // Цвет навигационного бара (белый)
val classic_light_bottomBarColor = ios_white       // Цвет нижнего бара (белый)
val classic_light_switchColor = ios_green          // Цвет переключателей

// Темная тема (iOS Dark)
val classic_dark_background = ios_black           // Основной фон
val classic_dark_surface = Color(0xFF1C1C1E)     // Поверхности
val classic_dark_primary = ios_blue              // Акцентный цвет
val classic_dark_onPrimary = ios_white
val classic_dark_secondary = ios_blue            // Цвет иконок в барах
val classic_dark_onSecondary = ios_white
val classic_dark_onBackground = ios_white
val classic_dark_onSurface = ios_white
val classic_dark_primaryContainer = Color(0xFF1C1C1E)
val classic_dark_onPrimaryContainer = ios_blue
val classic_dark_secondaryContainer = ios_black  // Фон баров
val classic_dark_onSecondaryContainer = ios_white
val classic_dark_surfaceVariant = Color(0xFF1C1C1E)
val classic_dark_onSurfaceVariant = ios_white

// Специальные цвета для iOS элементов (темная тема)
val classic_dark_statusBarColor = ios_black
val classic_dark_navigationBarColor = ios_black
val classic_dark_bottomBarColor = ios_black
val classic_dark_switchColor = ios_green 