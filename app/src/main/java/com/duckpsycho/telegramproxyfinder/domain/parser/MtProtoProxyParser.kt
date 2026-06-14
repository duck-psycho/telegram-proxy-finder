package com.duckpsycho.telegramproxyfinder.domain.parser

import android.net.Uri
import com.duckpsycho.telegramproxyfinder.domain.model.MtProtoProxy

object MtProtoProxyParser {

    fun parse(rawLine: String): MtProtoProxy? {
        val line = rawLine.trim()
        if (line.isEmpty()) return null

        if (!line.startsWith("tg://proxy", ignoreCase = true) &&
            !line.startsWith("https://t.me/proxy", ignoreCase = true)
        ) {
            return null
        }

        return parseLink(line)
    }

    private fun parseLink(link: String): MtProtoProxy? {
        val uri = runCatching { Uri.parse(normalize(link)) }.getOrNull() ?: return null
        val server = uri.getQueryParameter("server")?.trim().orEmpty()
        val port = uri.getQueryParameter("port")?.trim()?.toIntOrNull() ?: return null
        val secret = uri.getQueryParameter("secret")?.trim().orEmpty()
        return buildProxy(server, port, secret)
    }

    private fun normalize(link: String): String =
        if (link.startsWith("https://", ignoreCase = true)) {
            link.replaceFirst("https://t.me/proxy", "tg://proxy")
        } else {
            link
        }

    private fun buildProxy(server: String, port: Int, secret: String): MtProtoProxy? {
        if (server.isEmpty() || secret.isEmpty()) return null
        return MtProtoProxy(server = server, port = port, secret = secret.trim())
    }
}
