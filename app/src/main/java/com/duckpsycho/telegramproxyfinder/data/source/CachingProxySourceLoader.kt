package com.duckpsycho.telegramproxyfinder.data.source

import com.duckpsycho.telegramproxyfinder.domain.ProxySourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CachingProxySourceLoader(
    private val httpLoader: HttpProxySourceLoader,
    private val cache: FileProxySourceCache,
) : ProxySourceLoader {
    override suspend fun load(source: ProxySource): String? = withContext(Dispatchers.IO) {
        val url = source.resolveUrl()
        httpLoader.fetch(url)?.also { cache.write(url, it) }
            ?: cache.read(url)
    }
}
