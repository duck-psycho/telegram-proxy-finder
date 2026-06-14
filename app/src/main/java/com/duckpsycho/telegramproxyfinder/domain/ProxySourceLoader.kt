package com.duckpsycho.telegramproxyfinder.domain

interface ProxySourceLoader {
    suspend fun loadUrl(url: String): Set<String>
}
