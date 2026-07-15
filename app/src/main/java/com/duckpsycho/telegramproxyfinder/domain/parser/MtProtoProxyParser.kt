package com.duckpsycho.telegramproxyfinder.domain.parser

import com.duckpsycho.telegramproxyfinder.domain.model.MtProtoProxy
import java.net.URLDecoder

object MtProtoProxyParser {
    private const val MIN_PORT = 1
    private const val MAX_PORT = 65535

    private val PROXY_URL_REGEX =
        Regex(
            """(?:tg://proxy|https://t\.me/proxy)\?[^\s<>"']+""",
            setOf(RegexOption.IGNORE_CASE),
        )

    private val TRAILING_PUNCTUATION = charArrayOf(')', ']', '}', '.', ',', ';')

    fun parseAll(rawText: String): List<MtProtoProxy> = PROXY_URL_REGEX
        .findAll(rawText)
        .mapNotNull { match -> parseLink(cleanMatch(match.value)) }
        .toList()

    fun parse(rawLine: String): MtProtoProxy? = parseAll(rawLine).firstOrNull()

    private fun cleanMatch(raw: String): String = raw.trimEnd(*TRAILING_PUNCTUATION)

    private fun parseLink(link: String): MtProtoProxy? {
        val normalized = normalize(link)
        val queryStart = normalized.indexOf('?')
        if (queryStart < 0) return null

        val params = parseQuery(normalized.substring(queryStart + 1))
        val server = params["server"]?.trim().orEmpty()
        val port =
            params["port"]
                ?.trim()
                ?.toIntOrNull()
                ?.takeIf { it in MIN_PORT..MAX_PORT }
                ?: return null
        val secret = params["secret"]?.trim().orEmpty()
        return buildProxy(server, port, secret)
    }

    private fun parseQuery(query: String): Map<String, String> = query
        .split('&')
        .mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq <= 0) return@mapNotNull null
            val key = part.substring(0, eq).lowercase()
            val value = URLDecoder.decode(part.substring(eq + 1), Charsets.UTF_8.name())
            key to value
        }.toMap()

    private fun normalize(link: String): String {
        val decoded = decodeQuerySeparators(link)
        return if (decoded.startsWith("https://", ignoreCase = true)) {
            decoded.replaceFirst("https://t.me/proxy", "tg://proxy", ignoreCase = true)
        } else {
            decoded
        }
    }

    private fun decodeQuerySeparators(text: String): String = text
        .replace("\\u0026", "&", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)

    private fun buildProxy(
        server: String,
        port: Int,
        secret: String,
    ): MtProtoProxy? {
        if (server.isEmpty() || secret.isEmpty()) return null
        return MtProtoProxy(server = server, port = port, secret = secret.trim())
    }
}
