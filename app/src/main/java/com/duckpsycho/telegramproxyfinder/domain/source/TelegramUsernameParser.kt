package com.duckpsycho.telegramproxyfinder.domain.source

import java.net.URI

object TelegramUsernameParser {
    private val T_ME_HOSTS = setOf("t.me", "telegram.me", "www.t.me", "www.telegram.me")
    private val TG_DOMAIN_REGEX =
        Regex(
            """domain=([a-zA-Z0-9_]+)""",
            RegexOption.IGNORE_CASE,
        )

    fun parse(raw: String): String {
        val input = raw.trim()
        if (input.isBlank()) return ""

        parseTgUri(input)?.let { return it }
        if (containsTMeHost(input)) {
            return parseTMeUrl(input).orEmpty()
        }
        if (input.startsWith("@")) return input.removePrefix("@").trim()

        return input.trim()
    }

    private fun parseTgUri(input: String): String? {
        if (!input.startsWith("tg://", ignoreCase = true)) return null
        return TG_DOMAIN_REGEX.find(input)?.groupValues?.get(1)?.trim()
    }

    private fun containsTMeHost(input: String): Boolean = input.contains("t.me", ignoreCase = true) ||
        input.contains("telegram.me", ignoreCase = true)

    private fun parseTMeUrl(input: String): String? {
        if (!containsTMeHost(input)) return null

        return runCatching {
            val uri = URI(if (input.contains("://")) input else "https://$input")
            val host = uri.host?.lowercase() ?: return@runCatching null
            if (host !in T_ME_HOSTS) return@runCatching null

            val segments =
                uri.path
                    ?.trim('/')
                    ?.split('/')
                    ?.filter { it.isNotBlank() }
                    ?: return@runCatching null
            if (segments.isEmpty()) return@runCatching null

            val first = segments.first()
            when {
                first.equals("s", ignoreCase = true) -> {
                    val username = segments.getOrNull(1) ?: return@runCatching null
                    if (isReservedSegment(username)) return@runCatching null
                    username
                }

                isReservedSegment(first) -> null

                else -> first
            }
        }.getOrNull()
    }

    private fun isReservedSegment(segment: String): Boolean = segment.equals("joinchat", ignoreCase = true) ||
        segment.equals("s", ignoreCase = true) ||
        segment.startsWith("+")
}
