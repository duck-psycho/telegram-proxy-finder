package com.duckpsycho.telegramproxyfinder.domain.source

import com.duckpsycho.telegramproxyfinder.data.source.ProxySourceEntry
import com.duckpsycho.telegramproxyfinder.data.source.SourceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceEntryNormalizerTest {
    @Test
    fun isDuplicate_telegramSameUsernameDifferentFormat_returnsTrue() {
        val entries =
            listOf(
                ProxySourceEntry(
                    type = SourceType.Telegram,
                    value = "example",
                    createdAt = "2026-07-15T12:00:00Z",
                ),
            )

        assertTrue(SourceEntryNormalizer.isDuplicate(entries, SourceType.Telegram, "@example"))
        assertTrue(SourceEntryNormalizer.isDuplicate(entries, SourceType.Telegram, "t.me/example"))
    }

    @Test
    fun isDuplicate_webSameUrl_returnsTrue() {
        val entries =
            listOf(
                ProxySourceEntry(
                    type = SourceType.Web,
                    value = "https://example.com/list.txt",
                    createdAt = "2026-07-15T12:00:00Z",
                ),
            )

        assertTrue(
            SourceEntryNormalizer.isDuplicate(
                entries,
                SourceType.Web,
                "https://example.com/list.txt",
            ),
        )
    }

    @Test
    fun isDuplicate_differentType_returnsFalse() {
        val entries =
            listOf(
                ProxySourceEntry(
                    type = SourceType.Telegram,
                    value = "example",
                    createdAt = "2026-07-15T12:00:00Z",
                ),
            )

        assertFalse(
            SourceEntryNormalizer.isDuplicate(
                entries,
                SourceType.Web,
                "https://example.com/example",
            ),
        )
    }
}
