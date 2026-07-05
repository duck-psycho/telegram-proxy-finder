package com.duckpsycho.telegramproxyfinder.data.source

import org.json.JSONObject

object TelegramProxySourceParser {

    fun parse(body: String): Set<String> = runCatching {
        val records = JSONObject(body).getJSONArray("records")
        buildSet(records.length()) {
            for (index in 0 until records.length()) {
                val line = records.optString(index).trim()
                if (line.isNotEmpty()) add(line)
            }
        }
    }.getOrDefault(emptySet())
}
