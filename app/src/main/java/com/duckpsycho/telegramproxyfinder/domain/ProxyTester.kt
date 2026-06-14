package com.duckpsycho.telegramproxyfinder.domain

import com.duckpsycho.telegramproxyfinder.domain.model.MtProtoProxy

interface ProxyTester {
    suspend fun prepareSearch(workerCount: Int)

    suspend fun test(proxy: MtProtoProxy, workerSlot: Int): Result<Long>

    suspend fun finishSearch()
}
