package com.duckpsycho.telegramproxyfinder.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MtProtoProxyParserTest {
    @Test
    fun parseAll_extractsStandardHttpsLink() {
        val proxies =
            MtProtoProxyParser.parseAll(
                "https://t.me/proxy?server=example.com&port=443&secret=27ebe852539fb8ec5f327c73262bb721",
            )

        assertEquals(1, proxies.size)
        assertEquals("example.com", proxies[0].server)
        assertEquals(443, proxies[0].port)
        assertEquals("27ebe852539fb8ec5f327c73262bb721", proxies[0].secret)
    }

    @Test
    fun parseAll_extractsTgLinkWithReorderedParams() {
        val proxies =
            MtProtoProxyParser.parseAll(
                "Check tg://proxy?port=8443&secret=abc123&server=example.com for use",
            )

        assertEquals(1, proxies.size)
        assertEquals("example.com", proxies[0].server)
        assertEquals(8443, proxies[0].port)
        assertEquals("abc123", proxies[0].secret)
    }

    @Test
    fun parseAll_extractsMultipleLinksFromSingleLine() {
        val proxies =
            MtProtoProxyParser.parseAll(
                """
                tg://proxy?server=example.com&port=443&secret=aaa
                and also https://t.me/proxy?server=example.com&port=8443&secret=bbb inline
                """.trimIndent(),
            )

        assertEquals(2, proxies.size)
        assertEquals("example.com", proxies[0].server)
        assertEquals("example.com", proxies[1].server)
    }

    @Test
    fun parseAll_ignoresCommentWithoutLink() {
        val proxies = MtProtoProxyParser.parseAll("# MTProto RU (100)\n# Updated: 2026-07-15")

        assertTrue(proxies.isEmpty())
    }

    @Test
    fun parseAll_ignoresPlaceholderFormatComment() {
        val proxies =
            MtProtoProxyParser.parseAll(
                "# Format: https://t.me/proxy?server=SERVER&port=PORT&secret=SECRET",
            )

        assertTrue(proxies.isEmpty())
    }

    @Test
    fun parseAll_returnsEmptyForBlankText() {
        assertTrue(MtProtoProxyParser.parseAll("").isEmpty())
        assertTrue(MtProtoProxyParser.parseAll("   \n  ").isEmpty())
    }

    @Test
    fun parseAll_trimsTrailingParenthesisFromEmbeddedLink() {
        val proxies =
            MtProtoProxyParser.parseAll(
                "Link (https://t.me/proxy?server=example.com&port=443&secret=yyy)",
            )

        assertEquals(1, proxies.size)
        assertEquals("example.com", proxies[0].server)
    }

    @Test
    fun parse_returnsFirstMatch() {
        val proxy =
            MtProtoProxyParser.parse(
                "tg://proxy?server=example.com&port=443&secret=secret123",
            )

        assertEquals("example.com", proxy?.server)
        assertEquals(443, proxy?.port)
        assertEquals("secret123", proxy?.secret)
    }

    @Test
    fun parse_returnsNullForInvalidLine() {
        assertNull(MtProtoProxyParser.parse("# just a comment"))
        assertNull(MtProtoProxyParser.parse(""))
    }
}
