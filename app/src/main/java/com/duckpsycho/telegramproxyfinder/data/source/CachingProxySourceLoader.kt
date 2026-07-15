package com.duckpsycho.telegramproxyfinder.data.source

import com.duckpsycho.telegramproxyfinder.domain.ProxySourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CachingProxySourceLoader(
    private val httpLoader: HttpProxySourceLoader,
    private val cache: FileProxySourceCache,
) : ProxySourceLoader {
    override suspend fun load(source: ProxySource): Set<String> = withContext(Dispatchers.IO) {
        val url = source.resolveUrl()
        val body =
            httpLoader.fetch(url)?.also { cache.write(url, it) }
                ?: cache.read(url)
        body?.let { parseBody(it, source.type) } ?: emptySet()
    }

    private fun parseBody(
        body: String,
        type: SourceType,
    ): Set<String> = when (type) {
        SourceType.Web -> parseLines(body)
        SourceType.Telegram -> TelegramProxySourceParser.parse(body)
    }

    private fun parseLines(body: String): Set<String> = body
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
}
