package com.duckpsycho.telegramproxyfinder.domain.source

import org.junit.Assert.assertEquals
import org.junit.Test

class TelegramUsernameParserTest {
    @Test
    fun parse_plainUsername_returnsAsIs() {
        assertEquals("example", TelegramUsernameParser.parse("example"))
    }

    @Test
    fun parse_atUsername_stripsAt() {
        assertEquals("example", TelegramUsernameParser.parse("@example"))
    }

    @Test
    fun parse_tMePath_extractsUsername() {
        assertEquals("example", TelegramUsernameParser.parse("t.me/example"))
        assertEquals("example", TelegramUsernameParser.parse("https://t.me/example"))
        assertEquals("example", TelegramUsernameParser.parse("http://www.t.me/example/"))
    }

    @Test
    fun parse_tgResolve_extractsDomain() {
        assertEquals(
            "example",
            TelegramUsernameParser.parse("tg://resolve?domain=example"),
        )
        assertEquals(
            "example",
            TelegramUsernameParser.parse("tg://resolve?domain=example&post=1"),
        )
    }

    @Test
    fun parse_blank_returnsEmpty() {
        assertEquals("", TelegramUsernameParser.parse("   "))
    }

    @Test
    fun parse_tMePreview_extractsChannelUsername() {
        assertEquals("example", TelegramUsernameParser.parse("t.me/s/example"))
        assertEquals("example", TelegramUsernameParser.parse("https://t.me/s/example"))
    }

    @Test
    fun parse_joinchatLink_returnsEmpty() {
        assertEquals("", TelegramUsernameParser.parse("https://t.me/joinchat/ABC123"))
        assertEquals("", TelegramUsernameParser.parse("t.me/joinchat/ABC123"))
    }

    @Test
    fun parse_plusInviteLink_returnsEmpty() {
        assertEquals("", TelegramUsernameParser.parse("https://t.me/+ABC123"))
    }
}
