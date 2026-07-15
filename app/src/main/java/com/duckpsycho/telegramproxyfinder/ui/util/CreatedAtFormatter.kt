package com.duckpsycho.telegramproxyfinder.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object CreatedAtFormatter {
    fun format(iso: String): String = runCatching {
        val instant = Instant.parse(iso)
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .format(instant.atZone(ZoneId.systemDefault()))
    }.getOrElse { iso }
}
