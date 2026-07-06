package com.duckpsycho.telegramproxyfinder.domain

import com.duckpsycho.telegramproxyfinder.domain.model.MtProtoProxy

interface ProxyTester {
    suspend fun prepareSearch()

    suspend fun test(proxy: MtProtoProxy): Result<Long>

    suspend fun finishSearch()
}
