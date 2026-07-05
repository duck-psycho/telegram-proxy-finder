package com.duckpsycho.telegramproxyfinder.data.source

import java.net.URLEncoder

enum class SourceType {
    Web,
    Telegram,
}

data class ProxySource(
    val type: SourceType,
    val value: String,
) {
    fun resolveUrl(): String = when (type) {
        SourceType.Web -> value
        SourceType.Telegram -> {
            val username = value.trim().removePrefix("@")
            val encoded = URLEncoder.encode(username, "UTF-8")
            "$TELEGRAM_API_BASE?username=$encoded"
        }
    }

    private companion object {
        private const val TELEGRAM_API_BASE = "https://finder.duckpsycho.dev/api/v1/telegram"
    }
}
