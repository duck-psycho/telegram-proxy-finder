package com.duckpsycho.telegramproxyfinder.domain

import com.duckpsycho.telegramproxyfinder.data.source.ProxySource

interface ProxySourceLoader {
    suspend fun load(source: ProxySource): String?
}
