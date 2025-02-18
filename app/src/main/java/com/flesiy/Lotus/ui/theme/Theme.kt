package com.flesiy.Lotus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
)

private val ClassicLightColorScheme = lightColorScheme(
    primary = classic_light_primary,
    onPrimary = classic_light_onPrimary,
    primaryContainer = classic_light_primaryContainer,
    onPrimaryContainer = classic_light_onPrimaryContainer,
    secondary = classic_light_secondary,
    onSecondary = classic_light_onSecondary,
    secondaryContainer = classic_light_secondaryContainer,
    onSecondaryContainer = classic_light_onSecondaryContainer,
    background = classic_light_background,
    onBackground = classic_light_onBackground,
    surface = classic_light_surface,
    onSurface = classic_light_onSurface,
    surfaceVariant = classic_light_surfaceVariant,
    onSurfaceVariant = classic_light_onSurfaceVariant,
    surfaceTint = Color.Transparent,  // Убираем тонировку поверхностей
    // Добавляем цвета для TopAppBar
    inverseSurface = classic_light_secondaryContainer,  // Белый фон для TopAppBar
    inverseOnSurface = classic_light_onSecondaryContainer,  // Черный текст для TopAppBar
    inversePrimary = classic_light_secondary  // Синие иконки на белом фоне
)

private val ClassicDarkColorScheme = darkColorScheme(
    primary = classic_dark_primary,
    onPrimary = classic_dark_onPrimary,
    primaryContainer = classic_dark_primaryContainer,
    onPrimaryContainer = classic_dark_onPrimaryContainer,
    secondary = classic_dark_secondary,
    onSecondary = classic_dark_onSecondary,
    secondaryContainer = classic_dark_secondaryContainer,
    onSecondaryContainer = classic_dark_onSecondaryContainer,
    background = classic_dark_background,
    onBackground = classic_dark_onBackground,
    surface = classic_dark_surface,
    onSurface = classic_dark_onSurface,
    surfaceVariant = classic_dark_surfaceVariant,
    onSurfaceVariant = classic_dark_onSurfaceVariant,
    surfaceTint = Color.Transparent,  // Убираем тонировку поверхностей
    // Добавляем цвета для TopAppBar
    inverseSurface = classic_dark_secondaryContainer,  // Черный фон для TopAppBar
    inverseOnSurface = classic_dark_onSecondaryContainer,  // Белый текст для TopAppBar
    inversePrimary = classic_dark_secondary  // Синие иконки на черном фоне
)

@Composable
fun LotusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    useClassicTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useClassicTheme -> if (darkTheme) ClassicDarkColorScheme else ClassicLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (useClassicTheme) {
                if (darkTheme) classic_dark_statusBarColor.toArgb() else classic_light_statusBarColor.toArgb()
            } else {
                colorScheme.background.toArgb()
            }
            window.navigationBarColor = if (useClassicTheme) {
                if (darkTheme) classic_dark_bottomBarColor.toArgb() else classic_light_bottomBarColor.toArgb()
            } else {
                colorScheme.background.toArgb()
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = if (useClassicTheme) {
                    !darkTheme  // В светлой теме черные иконки на белом фоне
                } else {
                    !darkTheme
                }
                isAppearanceLightNavigationBars = isAppearanceLightStatusBars
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 