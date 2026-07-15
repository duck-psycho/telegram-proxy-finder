package com.duckpsycho.telegramproxyfinder.data.source

data class ProxySourceEntry(
    val type: SourceType,
    val value: String,
    val createdAt: String,
    val proxyCount: Int? = null,
)

fun ProxySourceEntry.stableKey(): String = "$type:$value:$createdAt"
