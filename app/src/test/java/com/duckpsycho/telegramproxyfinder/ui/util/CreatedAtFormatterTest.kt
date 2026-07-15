package com.duckpsycho.telegramproxyfinder.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class CreatedAtFormatterTest {
    @Test
    fun format_parsesIsoToLocalDateTime() {
        val iso = "2026-07-15T12:30:45Z"
        val expected =
            DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
                .format(
                    java.time.Instant
                        .parse(iso)
                        .atZone(ZoneId.systemDefault()),
                )

        assertEquals(expected, CreatedAtFormatter.format(iso))
    }

    @Test
    fun format_invalidIso_returnsOriginal() {
        assertEquals("not-a-date", CreatedAtFormatter.format("not-a-date"))
    }
}
