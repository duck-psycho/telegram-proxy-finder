package com.duckpsycho.telegramproxyfinder.data.source

import org.json.JSONArray
import org.json.JSONObject

object ProxySourceJsonCodec {
    private const val KEY_TYPE = "type"
    private const val KEY_VALUE = "value"
    private const val KEY_CREATED_AT = "createdAt"
    private const val KEY_PROXY_COUNT = "proxyCount"

    fun parse(json: String): List<ProxySourceEntry> {
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                add(parseEntry(array.getJSONObject(index)))
            }
        }
    }

    fun serialize(entries: List<ProxySourceEntry>): String {
        val array = JSONArray()
        for (entry in entries) {
            array.put(serializeEntry(entry))
        }
        return array.toString(2)
    }

    private fun parseEntry(json: JSONObject): ProxySourceEntry {
        val type =
            when (json.getString(KEY_TYPE).lowercase()) {
                "telegram" -> SourceType.Telegram
                "web" -> SourceType.Web
                else -> error("Unknown source type: ${json.getString(KEY_TYPE)}")
            }
        return ProxySourceEntry(
            type = type,
            value = json.getString(KEY_VALUE),
            createdAt = json.getString(KEY_CREATED_AT),
            proxyCount =
            if (json.has(KEY_PROXY_COUNT) && !json.isNull(KEY_PROXY_COUNT)) {
                json.getInt(KEY_PROXY_COUNT)
            } else {
                null
            },
        )
    }

    private fun serializeEntry(entry: ProxySourceEntry): JSONObject {
        val json =
            JSONObject()
                .put(KEY_TYPE, entry.type.toJsonValue())
                .put(KEY_VALUE, entry.value)
                .put(KEY_CREATED_AT, entry.createdAt)
        if (entry.proxyCount != null) {
            json.put(KEY_PROXY_COUNT, entry.proxyCount)
        }
        return json
    }

    private fun SourceType.toJsonValue(): String = when (this) {
        SourceType.Telegram -> "telegram"
        SourceType.Web -> "web"
    }
}
