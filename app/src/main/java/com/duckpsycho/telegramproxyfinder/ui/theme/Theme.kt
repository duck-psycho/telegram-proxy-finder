package com.duckpsycho.telegramproxyfinder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TelegramOnPrimary = Color(0xFFFFFFFF)

private val TelegramLightScheme = lightColorScheme(
    primary = TelegramBlue,
    onPrimary = TelegramOnPrimary,
    primaryContainer = Color(0xFFE8F4FF),
    onPrimaryContainer = TelegramBlueDark,
    secondary = TelegramLightSecondaryText,
    onSecondary = TelegramOnPrimary,
    tertiary = TelegramSubtitle,
    background = TelegramLightBg,
    onBackground = Color(0xFF000000),
    surface = TelegramLightSurface,
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFEFF4F9),
    onSurfaceVariant = TelegramSubtitle,
    outline = TelegramLightOutline,
    surfaceTint = TelegramBlue,
)

private val TelegramDarkScheme = darkColorScheme(
    primary = TelegramNightBlue,
    onPrimary = TelegramOnPrimary,
    primaryContainer = Color(0xFF2B5278),
    onPrimaryContainer = Color(0xFFC6E8FF),
    secondary = TelegramNightTextSecondary,
    onSecondary = TelegramOnPrimary,
    tertiary = TelegramNightTextSecondary.copy(alpha = 0.85f),
    background = TelegramNightDeep,
    onBackground = TelegramOnPrimary,
    surface = TelegramNightSurface,
    onSurface = TelegramOnPrimary,
    surfaceVariant = Color(0xFF232E3C),
    onSurfaceVariant = TelegramNightTextSecondary,
    outline = TelegramNightOutline,
    surfaceTint = TelegramNightBlue,
)

@Composable
fun MTProtoProxyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) TelegramDarkScheme else TelegramLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
