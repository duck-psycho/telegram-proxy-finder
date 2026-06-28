package com.duckpsycho.telegramproxyfinder.ui.theme

import androidx.compose.ui.graphics.Color

val TelegramBlue = Color(0xFF3390EC)
val TelegramBlueDark = Color(0xFF2B7CD6)
val TelegramLightBg = Color(0xFFF4F7FA)
val TelegramLightSurface = Color(0xFFFFFFFF)
val TelegramLightSecondaryText = Color(0xFF707579)
val TelegramLightOutline = Color(0xFFDCE8F1)
val TelegramSubtitle = Color(0xFF8393A7)

val TelegramNightDeep = Color(0xFF0E1621)
val TelegramNightSurface = Color(0xFF17212B)
val TelegramNightBlue = Color(0xFF5288C1)
val TelegramNightTextSecondary = Color(0xFF8393A7)
val TelegramNightOutline = Color(0xFF2B5278)

val PingGood = Color(0xFF43A047)
val PingMedium = TelegramBlue
val PingPoor = Color(0xFFE53935)

fun pingIndicatorColor(pingMs: Long): Color = when {
    pingMs <= 200 -> PingGood
    pingMs <= 300 -> PingMedium
    else -> PingPoor
}
