package com.duckpsycho.telegramproxyfinder.domain.source

import com.duckpsycho.telegramproxyfinder.data.source.ProxySourceEntry
import com.duckpsycho.telegramproxyfinder.data.source.SourceType

object SourceEntryNormalizer {
    fun normalize(
        type: SourceType,
        value: String,
    ): String = when (type) {
        SourceType.Telegram -> TelegramUsernameParser.parse(value)
        SourceType.Web -> value.trim()
    }

    fun isDuplicate(
        entries: List<ProxySourceEntry>,
        type: SourceType,
        value: String,
    ): Boolean {
        val normalized = normalize(type, value)
        if (normalized.isBlank()) return false

        return entries.any { entry ->
            entry.type == type && valuesEqual(type, entry.value, normalized)
        }
    }

    private fun valuesEqual(
        type: SourceType,
        existing: String,
        normalized: String,
    ): Boolean = when (type) {
        SourceType.Telegram -> existing.equals(normalized, ignoreCase = true)
        SourceType.Web -> existing == normalized
    }
}
