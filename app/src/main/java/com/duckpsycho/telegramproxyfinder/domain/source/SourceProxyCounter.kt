package com.duckpsycho.telegramproxyfinder.domain.source

import com.duckpsycho.telegramproxyfinder.data.source.HttpProxySourceLoader
import com.duckpsycho.telegramproxyfinder.data.source.ProxySource
import com.duckpsycho.telegramproxyfinder.data.source.ProxySourceEntry
import com.duckpsycho.telegramproxyfinder.domain.model.identityKey
import com.duckpsycho.telegramproxyfinder.domain.parser.MtProtoProxyParser

object SourceProxyCounter {
    fun countProxies(body: String): Int {
        val seenKeys = mutableSetOf<String>()
        return buildList {
            for (proxy in MtProtoProxyParser.parseAll(body)) {
                if (seenKeys.add(proxy.identityKey())) {
                    add(proxy)
                }
            }
        }.size
    }

    suspend fun countFromEntry(
        httpLoader: HttpProxySourceLoader,
        entry: ProxySourceEntry,
    ): Int? {
        val body = httpLoader.fetch(ProxySource(entry.type, entry.value).resolveUrl())
        if (body.isNullOrBlank()) return null
        return countProxies(body)
    }
}
