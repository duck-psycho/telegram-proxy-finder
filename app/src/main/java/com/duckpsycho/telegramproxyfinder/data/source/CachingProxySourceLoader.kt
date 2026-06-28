package com.duckpsycho.telegramproxyfinder.data.source

import com.duckpsycho.telegramproxyfinder.domain.ProxySourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CachingProxySourceLoader(
    private val httpLoader: HttpProxySourceLoader,
    private val cache: FileProxySourceCache,
) : ProxySourceLoader {

    override suspend fun loadUrl(url: String): Set<String> = withContext(Dispatchers.IO) {
        val body = httpLoader.fetch(url)?.also { cache.write(url, it) }
            ?: cache.read(url)
        body?.let(::parseLines) ?: emptySet()
    }

    private fun parseLines(body: String): Set<String> =
        body.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
}
