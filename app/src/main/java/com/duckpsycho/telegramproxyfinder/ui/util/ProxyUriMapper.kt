package com.duckpsycho.telegramproxyfinder.ui.util

import android.net.Uri
import com.duckpsycho.telegramproxyfinder.domain.model.WorkingMtProtoProxy

fun WorkingMtProtoProxy.toTelegramProxyUri(): Uri =
    Uri.Builder()
        .scheme("tg")
        .authority("proxy")
        .appendQueryParameter("server", server.trim())
        .appendQueryParameter("port", port.toString())
        .appendQueryParameter("secret", secret.trim())
        .build()

fun secretPreview(secret: String): String {
    val trimmed = secret.trim()
    if (trimmed.length <= 40) return trimmed
    return trimmed.take(32) + "…"
}
