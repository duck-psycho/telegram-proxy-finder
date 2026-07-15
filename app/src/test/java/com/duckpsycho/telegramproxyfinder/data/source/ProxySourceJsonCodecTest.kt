package com.duckpsycho.telegramproxyfinder.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProxySourceJsonCodecTest {
    @Test
    fun parse_validJson_returnsEntries() {
        val json =
            """
            [
              {
                "type": "telegram",
                "value": "example",
                "createdAt": "2026-07-15T12:00:00Z"
              },
              {
                "type": "web",
                "value": "https://example.com/proxies.txt",
                "createdAt": "2026-07-15T12:01:00Z"
              }
            ]
            """.trimIndent()

        val entries = ProxySourceJsonCodec.parse(json)

        assertEquals(2, entries.size)
        assertEquals(SourceType.Telegram, entries[0].type)
        assertEquals("example", entries[0].value)
        assertEquals("2026-07-15T12:00:00Z", entries[0].createdAt)
        assertEquals(null, entries[0].proxyCount)
        assertEquals(SourceType.Web, entries[1].type)
        assertEquals("https://example.com/proxies.txt", entries[1].value)
    }

    @Test
    fun serialize_roundTrip_preservesEntries() {
        val original =
            listOf(
                ProxySourceEntry(
                    type = SourceType.Telegram,
                    value = "example",
                    createdAt = "2026-07-15T12:00:00Z",
                ),
                ProxySourceEntry(
                    type = SourceType.Web,
                    value = "https://example.com/list.txt",
                    createdAt = "2026-07-15T12:01:00Z",
                ),
            )

        val restored = ProxySourceJsonCodec.parse(ProxySourceJsonCodec.serialize(original))

        assertEquals(original, restored)
    }

    @Test
    fun serialize_roundTrip_preservesEntriesWithProxyCount() {
        val original =
            listOf(
                ProxySourceEntry(
                    type = SourceType.Web,
                    value = "https://example.com/list.txt",
                    createdAt = "2026-07-15T12:01:00Z",
                    proxyCount = 42,
                ),
            )

        val restored = ProxySourceJsonCodec.parse(ProxySourceJsonCodec.serialize(original))

        assertEquals(original, restored)
    }

    @Test
    fun parse_emptyArray_returnsEmptyList() {
        assertEquals(emptyList<ProxySourceEntry>(), ProxySourceJsonCodec.parse("[]"))
    }

    @Test
    fun parse_unknownType_throws() {
        val json =
            """
            [
              {
                "type": "unknown",
                "value": "test",
                "createdAt": "2026-07-15T12:00:00Z"
              }
            ]
            """.trimIndent()

        assertThrows(IllegalStateException::class.java) {
            ProxySourceJsonCodec.parse(json)
        }
    }
}
